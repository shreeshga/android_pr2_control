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
import android.util.Log;
import java.util.ArrayList;

import org.cornell.pr2.control.Common;
import org.ros.namespace.GraphName;
import java.lang.Thread;

import org.ros.message.geometry_msgs.Twist;
import org.ros.message.trajectory_msgs.JointTrajectory;
import org.ros.message.trajectory_msgs.JointTrajectoryPoint;

import android.view.MotionEvent;
import org.ros.node.Node;
import org.ros.node.NodeMain;
import org.ros.exception.RosException;

import android.view.View;
import org.ros.node.topic.Publisher;
import org.ros.message.Message;

public class PR2Control {

	private Twist touchCmdMessage;
	private JointTrajectory touchTrajMessage;

	public static final double maxHeadPan = 2.7;
	public static final double maxHeadTilt = 1.4;
	public static final double minHeadTilt = -0.4;
	public static final double headTiltDiff = 0.5;
	public static final double headPanDiff = 1.2;
	public static final String TAG = "JoyStickView";

	private float motionY;
	private float motionX;
	private Common.BODY_PART movePart;
	private ROSNodeWrapper rosNode;

	private void init(Context context) {

		touchCmdMessage = new Twist();
		touchTrajMessage = new JointTrajectory();
		movePart = Common.BODY_PART.BODY;

	}

	public void setActiveBodyPart(Common.BODY_PART var) {
		movePart = var;
	}

	public Common.BODY_PART getActiveBodyPart() {
		return movePart;
	}

	public void updateMessage(View arg0, int action, MotionEvent motionEvent) {
		if ((action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE)) {
			motionX = (motionEvent.getX() - (arg0.getWidth() / 2))
					/ (arg0.getWidth());
			motionY = (motionEvent.getY() - (arg0.getHeight() / 2))
					/ (arg0.getHeight());
			if (movePart == Common.BODY_PART.BODY) {

				touchCmdMessage.linear.x = -2 * motionY;
				touchCmdMessage.linear.y = 0;
				touchCmdMessage.linear.z = 0;
				touchCmdMessage.angular.x = 0;
				touchCmdMessage.angular.y = 0;
				touchCmdMessage.angular.z = -5 * motionX;
				rosNode.setSendMessage(true);
				rosNode.setNullMessage(false);
				Log.i(TAG, "Moving Body z =" + touchCmdMessage.angular.z
						+ " x =" + touchCmdMessage.linear.x);
			} else {
				JointTrajectoryPoint p = new JointTrajectoryPoint();
				p.positions = new double[2];
				p.velocities = new double[2];
				double val = Math.max(maxHeadPan, motionX);
				p.positions[0] = val;
				p.velocities[0] = 0.0;

				val = Math.min(maxHeadTilt, motionY);
				val = Math.max(minHeadTilt, val);
				p.positions[0] = val;
				p.velocities[0] = 0.0;

				touchTrajMessage.joint_names = new ArrayList<String>(2);
				touchTrajMessage.joint_names.add("head_pan_joint");
				touchTrajMessage.joint_names.add("head_tilt_joint");

				touchTrajMessage.points = new ArrayList<JointTrajectoryPoint>(1);
				touchTrajMessage.points.add(p);
				Log.i(TAG, "Moving Head val=" + val);

			}
		} else {
			Log.i(TAG, "Not Moving");
			touchCmdMessage.linear.x = 0;
			touchCmdMessage.linear.y = 0;
			touchCmdMessage.linear.z = 0;
			touchCmdMessage.angular.x = 0;
			touchCmdMessage.angular.y = 0;
			touchCmdMessage.angular.z = 0;
			rosNode.setNullMessage(true);

			JointTrajectoryPoint p = new JointTrajectoryPoint();
			p.positions = new double[2];
			p.positions[0] = 0.0;
			p.velocities = new double[2];
			p.velocities[0] = 0.0;

			p.positions[0] = 0.0;
			p.velocities[0] = 0.0;

			touchTrajMessage.joint_names.add("head_pan_joint");
			touchTrajMessage.joint_names.add("head_tilt_joint");

			touchTrajMessage.points = new ArrayList<JointTrajectoryPoint>(1);
			touchTrajMessage.points.add(p);

		}
	}

}
