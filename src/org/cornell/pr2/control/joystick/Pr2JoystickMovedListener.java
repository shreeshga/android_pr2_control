package org.cornell.pr2.control.joystick;

import org.cornell.pr2.control.MainActivity;
import org.cornell.pr2.control.PR2Control;

import android.app.Activity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public class Pr2JoystickMovedListener implements JoystickMovedListener {
	MainActivity currentActivity;
	String TAG;
	
	public Pr2JoystickMovedListener(MainActivity activity,String stringID) {
		currentActivity = activity;
		TAG = stringID;
	}

	@Override
	public void OnMoved(int vSlide, int hSlide) {
//		Log.i("PR2JoyStick", "Pan " + vSlide + " Tilt " + hSlide);
		currentActivity.sendJoystickEvent(TAG,vSlide, hSlide);
	}

	@Override
	public void OnReleased() {
		Log.i("move listener", "on release");

	}

	@Override
	public void OnReturnedToCenter() {
		Log.i("move listener", "on center");

	}

	@Override
	public boolean onTouch(View arg0, MotionEvent motionEvent) {
		// int action = motionEvent.getAction();
		// pr2Controller.updateMessage(arg0,action,motionEvent);
//		Log.i("move listener", "on touch");
		return true;
	}
}
