package org.cornell.pr2.control;

import java.net.URI;
import java.util.Currency;

import org.cornell.pr2.control.Common;
import org.cornell.pr2.control.joystick.JoystickView;
import org.cornell.pr2.control.joystick.Pr2JoystickMovedListener;
import org.ros.address.InetAddressFactory;
import org.ros.android.BitmapFromCompressedImage;
import org.ros.android.RosActivity;

import org.ros.android.views.RosImageView;
import android.os.Bundle;
import org.ros.android.BitmapFromImage;
import org.ros.exception.RosException;
import org.ros.namespace.NameResolver;
import org.ros.node.Node;
import org.ros.node.NodeMainExecutor;
import org.ros.message.sensor_msgs.CompressedImage;
import org.ros.message.sensor_msgs.Image;

import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

import android.util.Log;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.hardware.SensorManager;

import org.cornell.pr2.control.R;

/**
 * @author damonkohler@google.com (Damon Kohler)
 * @author pratkanis@willowgarage.com (Tony Pratkanis)
 */
public class MainActivity extends RosActivity {
//	public static final String imageTopic = "/wide_stereo/left/image_color/compressed";
	public static final String imageTopic = "/wide_stereo/resize_compressed";

	public static final String imageMessage = "sensor_msgs/CompressedImage";

	private RosImageView<CompressedImage> rosImageView;
	private JoystickView joystickView;
	Pr2JoystickMovedListener listener;
	PR2Control pr2Controller;
	ROSNodeWrapper rosNode;
	private ToggleButton togglePart;
	private Common.BODY_PART activebodyPart;

	public MainActivity() {
		super("Pr2Control", "PR2Control");
		activebodyPart = Common.BODY_PART.BODY;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		rosNode = new ROSNodeWrapper();
		
		pr2Controller = new PR2Control(rosNode);
		listener = new Pr2JoystickMovedListener(this);

		rosImageView = (RosImageView<CompressedImage>) findViewById(R.id.imageView);
		rosImageView.setTopicName(imageTopic);
		rosImageView.setMessageType(imageMessage);
		rosImageView
				.setMessageToBitmapCallable(new BitmapFromCompressedImage());

		joystickView = (JoystickView) findViewById(R.id.joystickView);
		joystickView.bringToFront();
		joystickView.setOnJostickMovedListener(listener);
		togglePart = (ToggleButton) findViewById(R.id.toggle_body_part);
		togglePart.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {

				Log.i("JoystickView", "Text " + togglePart.getText());
				toggleBodyPart();
			}
		});
	}

	@Override
	protected void init(NodeMainExecutor nodeMainExecutor) {
		try {
			NodeConfiguration nodeConfiguration = NodeConfiguration.newPublic(
					InetAddressFactory.newNonLoopback().getHostAddress(),
					getMasterUri());
			nodeMainExecutor.execute(rosImageView,
					nodeConfiguration.setNodeName("pr2_control/video_view"));
			
			nodeMainExecutor.execute(rosNode,
					nodeConfiguration.setNodeName("pr2_control/joystick_view"));
			// nodeMainExecutor.execute(orientationPublisher,
			// nodeConfiguration.setNodeName("pr2_control/orientation_pub"));
			// NameResolver appNamespace = getAppNamespace(super.node);
		} catch (Exception ex) {
//			Toast.makeText(MainActivity.this, "Failed: " + ex.getMessage(),
//					Toast.LENGTH_LONG).show();
		}

	}
	@Override
	protected void onStop() {
		super.onStop();
		rosNode.stop();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		rosNode.stop();
	}
	protected void toggleBodyPart() {
		activebodyPart = (activebodyPart == Common.BODY_PART.BODY) ? Common.BODY_PART.HEAD
				: Common.BODY_PART.BODY;
		pr2Controller.setActiveBodyPart(activebodyPart);
	}
	
	public void sendJoystickEvent(int pan,int tilt) {
		pr2Controller.sendMessage(pan, tilt);
	}
}
