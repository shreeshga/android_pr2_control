package org.cornell.pr2.control;

import java.net.URI;
import java.util.Currency;

import org.cornell.pr2.control.Common;
import org.cornell.pr2.control.panzoom.PanZoomView;
import org.ros.address.InetAddressFactory;
import org.ros.android.BitmapFromCompressedImage;
import org.ros.android.OrientationPublisher;
import org.ros.android.RosActivity;

import org.ros.android.OrientationPublisher;

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

import org.ros.android.pr2.control.R;

/**
 * @author damonkohler@google.com (Damon Kohler)
 * @author pratkanis@willowgarage.com (Tony Pratkanis)
 */
public class MainActivity extends RosActivity {
	
	
	private OrientationPublisher orientationPublisher;                                                                     
	private RosImageView<CompressedImage> rosImageView;	
	private PR2Control joystickView; 
	private PanZoomView panView;
//	private SendGoalDisplay goalDisplay;
	
	
	private String imageTopic = "/wide_stereo/left/image_color/compressed";
	private String imageMessage  = "sensor_msgs/CompressedImage";
	private ToggleButton togglePart;
	private Common.BODY_PART  activebodyPart;
	public MainActivity() {
	  super("Pr2Control", "PR2Control");
	  activebodyPart = Common.BODY_PART.BODY;
	}

	  @Override
	  public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);

	    setContentView(R.layout.main);
	    rosImageView = (RosImageView<CompressedImage>) findViewById(R.id.imageView);
	    //rosImageView.setTopicName("/wide_stereo/left/image_color/compressed");
	    rosImageView.setTopicName(imageTopic);
	    
	    rosImageView.setMessageType(imageMessage);
	    rosImageView.setMessageToBitmapCallable(new BitmapFromCompressedImage());
	  	
	    joystickView = (PR2Control) findViewById(R.id.joystick);
	    togglePart = (ToggleButton) findViewById(R.id.toggle_body_part);
	    togglePart.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Log.i("JoystickView","Text "+togglePart.getText());
				toggleBodyPart();
			}
		});
	    
	    joystickView.bringToFront();
	    //orientationPublisher = new OrientationPublisher((SensorManager) getSystemService(SENSOR_SERVICE));
	  }
	
	  @Override
	  protected void init(NodeMainExecutor nodeMainExecutor) {
		    try {
		        NodeConfiguration nodeConfiguration =
		                NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostAddress(),
		                    getMasterUri());
		        nodeMainExecutor.execute(rosImageView, nodeConfiguration.setNodeName("pr2_control/video_view"));
		        nodeMainExecutor.execute(joystickView, nodeConfiguration.setNodeName("pr2_control/joystick_view"));
		       // nodeMainExecutor.execute(orientationPublisher, nodeConfiguration.setNodeName("pr2_control/orientation_pub"));
		         //NameResolver appNamespace = getAppNamespace(super.node);
			    } catch (Exception ex) {
			      Toast.makeText(MainActivity.this, "Failed: " + ex.getMessage(), Toast.LENGTH_LONG).show();
			    }
		    
	  }
	  
	  protected void toggleBodyPart() {
		  activebodyPart = (activebodyPart == Common.BODY_PART.BODY)? Common.BODY_PART.HEAD:Common.BODY_PART.BODY;
		  joystickView.setActiveBodyPart(activebodyPart);
	  }
}
