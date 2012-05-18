package org.cornell.pr2.control.joystick;

import android.view.View.OnTouchListener;

public interface JoystickMovedListener extends OnTouchListener  {
	public void OnMoved(int pan, int tilt);
	public void OnReleased();
	public void OnReturnedToCenter();
}
