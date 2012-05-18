/*
 * Copyright (c) 2011, Willow Garage, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the Willow Garage, Inc. nor the names of its
 *       contributors may be used to endorse or promote products derived from
 *       this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
 
package org.cornell.pr2.control;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.widget.ImageView;
import android.util.AttributeSet;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import org.cornell.pr2.control.Common;
import org.cornell.pr2.control.Common.BODY_PART;
import org.ros.namespace.GraphName;
import org.ros.node.parameter.ParameterTree;
import java.lang.Thread;


import org.ros.actionlib.client.SimpleActionClientCallbacks;
import org.ros.actionlib.state.SimpleClientGoalState;


import org.ros.message.geometry_msgs.Twist;
import org.ros.message.trajectory_msgs.JointTrajectory;
import org.ros.message.trajectory_msgs.JointTrajectoryPoint;

import android.view.MotionEvent;
import org.ros.node.Node;
import org.ros.node.NodeMain;
import org.ros.exception.RosException;
//import org.ros.android.util.PlaneTfChangeListener;
import android.view.View.OnTouchListener;
import android.view.View;
import org.ros.node.Node;
import org.ros.node.topic.Publisher;
import org.ros.node.service.ServiceResponseListener;
import org.ros.node.topic.Subscriber;
import org.ros.exception.RosException;
import org.ros.exception.RemoteException;
import org.ros.node.service.ServiceClient;
import org.ros.internal.node.service.ServiceIdentifier;
import org.ros.message.Message;
import org.ros.message.MessageListener;
import org.ros.time.NtpTimeProvider;


/**
 * View for screen-based joystick teleop.
 * @param <T>
 */
public class PR2Control<T> extends ImageView implements OnTouchListener,NodeMain {
  private String baseControlTopic;
  private String headPanTopic;
  private String headTiltTopic;
  private String headControlTopic;
  private Twist touchCmdMessage;
  private JointTrajectory touchTrajMessage;
  
  public static final double maxHeadPan = 2.7;
  public static final double maxHeadTilt = 1.4;  
  public static final double minHeadTilt = -0.4;
  public static final double headTiltDiff = 0.5;
  public static final double headPanDiff = 1.2;
  public static final String TAG="JoyStickView"; 
  
  private Thread pubThread;
  private float motionY;
  private float motionX;
  
  private Publisher<Twist> twistPub;
  private Publisher<JointTrajectory> jointPub;
  private boolean sendMessages = true;
  private boolean nullMessage = true;
  private Common.BODY_PART  movePart;
  
  public PR2Control(Context ctx) {
    super(ctx);
    init(ctx);
  }

  public PR2Control(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    init(context);
  }

  public PR2Control(Context context, AttributeSet attrs) {
    super(context, attrs);
    init(context);
  }

  private void init(Context context) {
    baseControlTopic = "/base_controller/command";
    headTiltTopic = "head_tilt_joint"; 
    headPanTopic = "head_pan_joint";
    headControlTopic  = "head_traj_controller/command";
    touchCmdMessage = new Twist();
    touchTrajMessage = new JointTrajectory();
    movePart = Common.BODY_PART.BODY;

    this.setOnTouchListener(this);
  }

  public void setBaseControlTopic(String t) {
    baseControlTopic = t;
  }

  public void setActiveBodyPart(Common.BODY_PART var) {
	  movePart = var;
  }

  public Common.BODY_PART getActiveBodyPart() {
	  return movePart;
  }
  
  private <T extends Message> void createPublisherThread(final Publisher<T> pub, final T message,
      final int rate) {
    pubThread = new Thread(new Runnable() {

      @Override
      public void run() {
        try {
          while (true) {
            if (sendMessages) {
              Log.i("JoystickView", "send joystick message");
              pub.publish(message);
              if (nullMessage) {
                sendMessages = false;
              }
            } else {
              //Log.i("JoystickView", "skipping joystick");
            }
            Thread.sleep(1000 / rate);
          }
        } catch (InterruptedException e) {
        }
      }
    });
    Log.i("JoystickView", "started pub thread");
    pubThread.start();
  }
  
  public void start(Node node) throws RosException { 
    Log.i("JoystickView", "init Publisher");
    if(movePart == Common.BODY_PART.BODY) {
    twistPub = node.newPublisher(baseControlTopic, "geometry_msgs/Twist");
    createPublisherThread(twistPub, touchCmdMessage, 10);
    } else {
        jointPub = node.newPublisher(headControlTopic, "trajectory_msgs/JointTrajectory");
        createPublisherThread(jointPub, touchTrajMessage, 10);
    }
    
  }

  public void stop() {
    if (pubThread != null) {
      pubThread.interrupt();
      pubThread = null;
    }
    if (twistPub != null) {
      twistPub.shutdown();
      twistPub = null;
    }
    if (jointPub != null) {
        jointPub.shutdown();
        jointPub = null;
      }
  }


  @Override
  public GraphName getDefaultNodeName() {
    return new GraphName("pr2_control/joystick_view");
  }

  @Override
  public void onStart(Node node) {
	  	Log.i("JoystickView", "init twistPub");
     	twistPub = node.newPublisher(baseControlTopic, "geometry_msgs/Twist");
     	createPublisherThread(twistPub, touchCmdMessage, 10);
    	jointPub = node.newPublisher(headControlTopic, "trajectory_msgs/JointTrajectory");
        createPublisherThread(jointPub, touchTrajMessage, 10);
    }

  @Override
  public void onShutdown(Node node) {
  }

  @Override
  public void onShutdownComplete(Node node) {
  }
  
  public void updateMessage(View arg0,int action,MotionEvent motionEvent) {
	if (arg0 == this && (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE)) {
	    motionX = (motionEvent.getX() - (arg0.getWidth() / 2)) / (arg0.getWidth());
	    motionY = (motionEvent.getY() - (arg0.getHeight() / 2)) / (arg0.getHeight());    
	  if(movePart == Common.BODY_PART.BODY) {
		  
		  touchCmdMessage.linear.x = -2 * motionY;
		  touchCmdMessage.linear.y = 0;
		  touchCmdMessage.linear.z = 0;
		  touchCmdMessage.angular.x = 0;
		  touchCmdMessage.angular.y = 0;
		  touchCmdMessage.angular.z = -5 * motionX;
		  sendMessages = true;
		  nullMessage = false;
		  Log.i(TAG,"Moving Body z ="+touchCmdMessage.angular.z+" x ="+touchCmdMessage.linear.x );
	  } else {
			JointTrajectoryPoint p = new JointTrajectoryPoint();
			p.positions  = new double[2];
			p.velocities  = new double[2];
			double val = Math.max(maxHeadPan,motionX);
			p.positions[0] =  val;
			p.velocities[0] = 0.0;
			
			val = Math.min(maxHeadTilt,motionY);
			val = Math.max(minHeadTilt,val);
			p.positions[0] =  val;
			p.velocities[0] = 0.0;
			
			touchTrajMessage.joint_names = new ArrayList<String>(2);
			touchTrajMessage.joint_names.add("head_pan_joint");
			touchTrajMessage.joint_names.add("head_tilt_joint");
			
			touchTrajMessage.points = new ArrayList<JointTrajectoryPoint>(1);
			touchTrajMessage.points.add(p);
		  	Log.i(TAG,"Moving Head val="+val);

		}
	} else {
		  Log.i(TAG,"Not Moving");
		  touchCmdMessage.linear.x = 0;
		  touchCmdMessage.linear.y = 0;
		  touchCmdMessage.linear.z = 0;
		  touchCmdMessage.angular.x = 0;
		  touchCmdMessage.angular.y = 0;
		  touchCmdMessage.angular.z = 0;
		  nullMessage = true;

			JointTrajectoryPoint p = new JointTrajectoryPoint();
			p.positions  = new double[2];
			p.positions[0] =  0.0;
			p.velocities = new double[2];
			p.velocities[0] = 0.0;
			
			p.positions[0] =  0.0;
			p.velocities[0] = 0.0;
		  
			touchTrajMessage.joint_names.add("head_pan_joint");
			touchTrajMessage.joint_names.add("head_tilt_joint");
			
			touchTrajMessage.points = new ArrayList<JointTrajectoryPoint>(1);
			touchTrajMessage.points.add(p);

	}
  }
  @Override
  public boolean onTouch(View arg0, MotionEvent motionEvent) {
	int action = motionEvent.getAction();
	updateMessage(arg0,action,motionEvent);   	
	return true;
  }
}
