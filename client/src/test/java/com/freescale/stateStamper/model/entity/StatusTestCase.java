package com.freescale.stateStamper.model.entity;


import junit.framework.TestCase;

public class StatusTestCase extends TestCase {
	Status status;

	protected void setUp() throws Exception {
		super.setUp();
		status = new Status();
	}

	public void testSet() {
	status.set("DURATION", "0.0");
	assertEquals("0.0",status.get("DURATION"));
	}
	
	public void testHas()
		{
		assertFalse( status.has("DUMMY"));
		assertTrue(status.has("MID"));
		}

}
