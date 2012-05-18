package org.cornell.pr2.control.joystick;

import org.cornell.pr2.control.PR2Control;

import android.view.MotionEvent;
import android.view.View;

public class Pr2JoystickMovedListener  implements JoystickMovedListener {
	PR2Control pr2Controller;
	
	
	public Pr2JoystickMovedListener() {
		
	}
	@Override
	public void OnMoved(int pan, int tilt) {

	}

	@Override
	public void OnReleased() {


	}

	@Override
	public void OnReturnedToCenter() {


	}

	@Override
	public boolean onTouch(View arg0, MotionEvent motionEvent) {
		int action = motionEvent.getAction();
		pr2Controller.updateMessage(arg0,action,motionEvent);
		return true;
	}
}
