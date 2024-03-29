
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
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URI;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;


/**
 * Threaded control checker. Checks to see if the software is running and in a valid state.
 *
 * @author pratkanis@willowgarage.com
 */
public class ControlChecker {
  public interface SuccessHandler {
    /** Called on success with a description of the robot that got checked. */
    void handleSuccess();
  }

  public interface FailureHandler {
    /**
     * Called on failure with a short description of why it failed, like
     * "exception" or "timeout".
     */
    void handleFailure(String reason);
  }
  public interface EvictionHandler {
    /** Called to prompt the user to evict another user */
    boolean doEviction(String user, String message);
  }

  public interface StartHandler {
    /** Called when starting the robot so that the user can be informed of possible delays */
    void handleStarting();
  }

  private CheckerThread checkerThread;
  private SuccessHandler robotReadyCallback;
  private FailureHandler failureCallback;
  private EvictionHandler evictionCallback;
  private StartHandler startCallback;
  private boolean doStart;

  /** Constructor. Should not take any time. Never starts or evicts. */
  public ControlChecker(SuccessHandler robotReadyCallback, FailureHandler failureCallback) {
    this.robotReadyCallback = robotReadyCallback;
    this.failureCallback = failureCallback;
    this.evictionCallback = new EvictionHandler() {
        public boolean doEviction(String user, String message) { 
          return false; 
        }};
    this.startCallback = null;
    this.doStart = false;
  }


  /** Constructor. Should not take any time. */
  public ControlChecker(SuccessHandler robotReadyCallback, FailureHandler failureCallback, EvictionHandler evictionCallback) {
    this.robotReadyCallback = robotReadyCallback;
    this.failureCallback = failureCallback;
    this.evictionCallback = evictionCallback;
    this.startCallback = null;
    this.doStart = true;
  }

  /** Constructor. Should not take any time. */
  public ControlChecker(SuccessHandler robotReadyCallback, FailureHandler failureCallback, EvictionHandler evictionCallback, StartHandler startCallback) {
    this.robotReadyCallback = robotReadyCallback;
    this.failureCallback = failureCallback;
    this.evictionCallback = evictionCallback;
    this.startCallback = startCallback;
    this.doStart = true;
  }


  /**
   * Start the checker thread with the given robotId. If the thread is
   * already running, kill it first and then start anew. Returns immediately.
   */
  public void beginChecking(RobotId robotId) {
    stopChecking();
    //If there's no wifi tag in the robot id, skip this step
    if (robotId.getControlUri() == null) {
      robotReadyCallback.handleSuccess();
      return;
    }

    checkerThread = new CheckerThread(robotId);
    checkerThread.start();
  }

  /** Stop the checker thread. */
  public void stopChecking() {
    if (checkerThread != null && checkerThread.isAlive()) {
      checkerThread.interrupt();
    }
  }
  
  private enum State {
    IN_USE, 
    OFF,
    VALID
  }
  private class RobotState {
    private final String IN_USE_TAG = "STATE_IN_USE";
    private final String VALID_TAG = "STATE_VALID";
    private final String OFF_TAG = "STATE_OFF";
    private final String USER_TAG = "USER:";
    private final String MESSAGE_TAG = "MESSAGE:";
    
    public State state;
    public String user;
    public String message;
    public boolean readError;
    
    public RobotState(String page) {
      String[] pageLines = page.split("\n");
      
      boolean stateFound = false;
      
      for (String i : pageLines) {
        if (i.trim().indexOf(IN_USE_TAG) >= 0) {
          state = State.IN_USE;
          stateFound = true;
        }
        if (i.trim().indexOf(VALID_TAG) >= 0) {
          state = State.VALID;
          stateFound = true;
        }
        if (i.trim().indexOf(OFF_TAG) >= 0) {
          state = State.OFF;
          stateFound = true;
        }
        if (i.trim().indexOf(USER_TAG) >= 0) {
          user = i.trim().substring(USER_TAG.length() + 1).trim();
        }
        if (i.trim().indexOf(MESSAGE_TAG) >= 0) {
          message = i.trim().substring(MESSAGE_TAG.length() + 1).trim();
        }
      }
      
      if (!stateFound) {
        readError = true;
      }
    }
  }
  
  
  private class CheckerThread extends Thread {

    private RobotId robotId;

    public CheckerThread(RobotId robotId) {
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

    private String getPage(String uri) {
      try {
        HttpClient client = new DefaultHttpClient();
        HttpGet request = new HttpGet();
        request.setURI(new URI(uri));
        HttpResponse response = client.execute(request);
        BufferedReader in = new BufferedReader
          (new InputStreamReader(response.getEntity().getContent()));
        StringBuffer sb = new StringBuffer("");
        String line = "";
        String NL = System.getProperty("line.separator");
        while ((line = in.readLine()) != null) {
          sb.append(line + NL);
        }
        in.close();
        String page = sb.toString();
        return page;
      } catch (java.io.IOException ex) {
        Log.e("ControlChecker", "IOError: " + uri, ex);
      } catch (java.net.URISyntaxException ex) {
        Log.e("ControlChecker", "URI Invalid: " + uri, ex);
      }
      return null;
    }

    private RobotState getRobotState() {
      String page = getPage(robotId.getControlUri() + "?action=GET_STATE");
      if (page == null) {
        return null;
      }
      RobotState state = new RobotState(page);
      if (state.readError) {
        return null;
      }
      return state;
    }

    @Override
    public void run() {
      try {
        RobotState state = getRobotState();
        if (state == null) {
          failureCallback.handleFailure("Could not connect to the control page");
          return;
        }
        Log.d("ControlChecker", "Active user: " + state.user + " (" + state.state + ")");

        if (state.state == ControlChecker.State.VALID) {
          robotReadyCallback.handleSuccess();
        } else {
          if (state.state == ControlChecker.State.IN_USE) {
            if (evictionCallback.doEviction(state.user, state.message)) { //Prompt
              Log.d("ControlChecker", "Stopping robot");
              getPage(robotId.getControlUri() + "?action=STOP_ROBOT");
            } else {
              Log.d("ControlChecker", "No eviction");
              failureCallback.handleFailure("Need to evict current user inorder to connect");
              return;
            }
          }
          if (doStart) {
            if (startCallback != null) {
              startCallback.handleStarting();
            }
            Log.d("ControlChecker", "Starting robot");
            getPage(robotId.getControlUri() + "?action=START_ROBOT");
            
            int i = 0;
            
            while (i < 30 && state.state != ControlChecker.State.VALID) {
              i++;
              Thread.sleep(1000L);
              state = getRobotState();
              if (state == null) {
                failureCallback.handleFailure("Lost connection with robot");
                return;
              }
            }
            
            if (state.state == ControlChecker.State.VALID) {
              robotReadyCallback.handleSuccess();
            } else {
              Log.d("ControlChecker", "Restarted robot, still not working");
              failureCallback.handleFailure("Re-started the robot, but it is still not working");
            }
          } else {
            //Non-started robot
            Log.d("ControlChecker", "Robot, not started working");
            failureCallback.handleFailure("Robot not started");
          }
        }
      } catch (Throwable ex) {
        Log.e("ControlChecker", "Exception while checking control URI "
              + robotId.getControlUri(), ex);
        failureCallback.handleFailure(ex.toString());
      }
    }
  }
}