/*
 * Software License Agreement (BSD License)
 *
 * Copyright (c) 2011, Willow Garage, Inc.
 * All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above
 *    copyright notice, this list of conditions and the following
 *    disclaimer in the documentation and/or other materials provided
 *    with the distribution.
 *  * Neither the name of Willow Garage, Inc. nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package org.cornell.pr2.control.robotselector;

import android.util.Log;
import org.ros.internal.node.server.NodeIdentifier;
import org.ros.internal.node.client.ParameterClient;
import org.ros.namespace.NameResolver;
import org.ros.namespace.GraphName;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import org.cornell.pr2.control.robotselector.RobotDescription;;
/**
 * Threaded ROS-master checker. Runs a thread which checks for a valid ROS
 * master and sends back a {@link RobotDescription} (with robot name and type)
 * on success or a failure reason on failure.
 *
 * @author hersh@willowgarage.com
 */
public class MasterChecker {
  public interface RobotDescriptionReceiver {
    /** Called on success with a description of the robot that got checked. */
    void receive(RobotDescription robotDescription);
  }

  public interface FailureHandler {
    /**
     * Called on failure with a short description of why it failed, like
     * "exception" or "timeout".
     */
    void handleFailure(String reason);
  }

  private CheckerThread checkerThread;
  private RobotDescriptionReceiver foundMasterCallback;
  private FailureHandler failureCallback;

  /** Constructor. Should not take any time. */
  public MasterChecker(RobotDescriptionReceiver foundMasterCallback, FailureHandler failureCallback) {
    this.foundMasterCallback = foundMasterCallback;
    this.failureCallback = failureCallback;
  }

  /**
   * Start the checker thread with the given robotId. If the thread is
   * already running, kill it first and then start anew. Returns immediately.
   */
  public void beginChecking(RobotId robotId) {
    stopChecking();
    if (robotId.getMasterUri() == null) {
      failureCallback.handleFailure("empty master URI");
      return;
    }
    URI uri;
    try {
      uri = new URI(robotId.getMasterUri());
    } catch (URISyntaxException e) {
      failureCallback.handleFailure("invalid master URI");
      return;
    }

    checkerThread = new CheckerThread(robotId, uri);
    checkerThread.start();
  }

  /** Stop the checker thread. */
  public void stopChecking() {
    if (checkerThread != null && checkerThread.isAlive()) {
      checkerThread.interrupt();
    }
  }

  private class CheckerThread extends Thread {

    private URI masterUri;
    private RobotId robotId;

    public CheckerThread(RobotId robotId, URI masterUri) {
      this.masterUri = masterUri;
      this.robotId = robotId;

      setDaemon(true);

      // don't require callers to explicitly kill all the old checker threads.
      setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
        @Override
        public void uncaughtException(Thread thread, Throwable ex) {
          failureCallback.handleFailure("exception: " + ex.getMessage());
        }
      });
    }

    @Override
    public void run() {
      try {
        ParameterClient paramClient = new ParameterClient(
                  NodeIdentifier.newFromStrings("/master_checker", masterUri.toString()), masterUri);
        boolean hasName = ((Boolean)paramClient.hasParam(new GraphName("robot/name")).getResult()).booleanValue();
        boolean hasType = ((Boolean)paramClient.hasParam(new GraphName("robot/type")).getResult()).booleanValue();
        if (hasName && hasType) {
          String robotName = (String) paramClient.getParam(new GraphName("robot/name")).getResult();
          String robotType = (String) paramClient.getParam(new GraphName("robot/type")).getResult();
          Date timeLastSeen = new Date(); // current time.
          RobotDescription robotDescription = new RobotDescription(
                             robotId, robotName, robotType, timeLastSeen);
          foundMasterCallback.receive(robotDescription);
        } else {
          Log.e("RosAndroid", "No parameters");
          failureCallback.handleFailure("The parameters on the server are not set. Please set robot/name and robot/type.");
        }
        return;
      } catch (Throwable ex) {
        Log.e("RosAndroid", "Exception while creating node in MasterChecker for master URI "
              + masterUri, ex);
        failureCallback.handleFailure(ex.toString());
      }
    }
  }
}
