package org.cornell.pr2.control;

import org.ros.exception.RosException;
import org.ros.message.Message;
import org.ros.message.geometry_msgs.Twist;
import org.ros.message.trajectory_msgs.JointTrajectory;
import org.ros.namespace.GraphName;
import org.ros.node.Node;
import org.ros.node.NodeMain;
import org.ros.node.topic.Publisher;

import android.util.Log;

public class ROSNodeWrapper implements NodeMain {
	public static final String baseControlTopic = "/base_controller/command";
	public static final String headTiltTopic = "head_tilt_joint";
	public static final String headPanTopic = "head_pan_joint";
	public static final String headControlTopic = "head_traj_controller/command";

	private Twist touchCmdMessage;
	private JointTrajectory touchTrajMessage;
	private boolean sendMessages = true;
	private boolean nullMessage = true;
	private Publisher<Twist> twistPub;
	private Publisher<JointTrajectory> jointPub;

	private Thread pubThread;

	
	public void setSendMessage(boolean flag) {
		sendMessages = flag;
	}

	public void setNullMessage(boolean flag) {
		nullMessage = flag;
	}

	@Override
	public void onShutdown(Node arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onShutdownComplete(Node arg0) {
		// TODO Auto-generated method stub

	}

	private <T extends Message> void createPublisherThread(
			final Publisher<T> pub, final T message, final int rate) {
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
							// Log.i("JoystickView", "skipping joystick");
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

	@Override
	public GraphName getDefaultNodeName() {
		return new GraphName("pr2_control/joystick_view");
	}

	@Override
	public void onStart(Node node) {
		Log.i("JoystickView", "init twistPub");
		twistPub = node.newPublisher(baseControlTopic, "geometry_msgs/Twist");
		createPublisherThread(twistPub, touchCmdMessage, 10);
		jointPub = node.newPublisher(headControlTopic,
				"trajectory_msgs/JointTrajectory");
		createPublisherThread(jointPub, touchTrajMessage, 10);
	}

	public void startBaseControllerNode(Node node) throws RosException {
		Log.i("JoystickView", "init Publisher");
		if (twistPub != null) {
			twistPub.shutdown();
			twistPub = null;
		}

		twistPub = node.newPublisher(baseControlTopic, "geometry_msgs/Twist");
		createPublisherThread(twistPub, touchCmdMessage, 10);
	}

	public void startHeadControllerNode(Node node) throws RosException {
		if (jointPub != null) {
			jointPub.shutdown();
			jointPub = null;
		}

		jointPub = node.newPublisher(headControlTopic,
				"trajectory_msgs/JointTrajectory");
		createPublisherThread(jointPub, touchTrajMessage, 10);
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
}
