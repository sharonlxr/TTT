package com.freescale.stateStamper.view;

import junit.framework.TestCase;
import com.freescale.stateStamper.view.StateStamperFrame;

public class StateStamperFrameTestCase extends TestCase {

	public void testStateStamperFrameVersion(){
		assertTrue(StateStamperFrame.getVersion() == "Tool Time Tracker 3.1");
	}
}
