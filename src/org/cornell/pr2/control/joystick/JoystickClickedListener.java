package org.cornell.pr2.control.joystick;

import android.util.Log;

public class JoystickClickedListener {
	public void OnClicked() {
		Log.i("touch listener", "on click");
;
	}
	public void OnReleased() {
		Log.i("move listener", "on release");
		
	}
}
