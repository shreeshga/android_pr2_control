package org.cornell.pr2.control.joystick;

import org.cornell.pr2.control.MainActivity;
import org.cornell.pr2.control.PR2Control;

import android.app.Activity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public class Pr2JoystickMovedListener implements JoystickMovedListener {
	MainActivity currentActivity;

	public Pr2JoystickMovedListener(MainActivity activity) {
		currentActivity = activity;
	}

	@Override
	public void OnMoved(int pan, int tilt) {
		Log.i("PR2JoyStick", "Pan " + pan + " Tilt " + tilt);
		currentActivity.sendJoystickEvent(pan, tilt);
	}

	@Override
	public void OnReleased() {

	}

	@Override
	public void OnReturnedToCenter() {

	}

	@Override
	public boolean onTouch(View arg0, MotionEvent motionEvent) {
		// int action = motionEvent.getAction();
		// pr2Controller.updateMessage(arg0,action,motionEvent);
		Log.i("move listener", "on touch");
		return true;
	}
}
