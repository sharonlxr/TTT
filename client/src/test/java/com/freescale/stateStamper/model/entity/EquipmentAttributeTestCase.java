package com.freescale.stateStamper.model.entity;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

public class EquipmentAttributeTestCase extends TestCase
	{

	EquipmentAttribute ea;
	protected void setUp() throws Exception
		{
		super.setUp();
		ea = new EquipmentAttribute();
		}
	public void testEquipmentAttribute()
		{
		EquipmentAttribute original = new EquipmentAttribute();
		original.name = "Original";
		EquipmentAttribute copy = new EquipmentAttribute(original);
		assertEquals(original.toString(), copy.toString());
		original.name = "Updated";
		assertNotSame(original.toString(), copy.toString());
		}
	
	public void testGetConfiguration()
		{
		Map parametersSource = new HashMap();
		String key1="STATE";
		String val1="Standby";
		String key2="SUBSTATE";
		String val2="Idle";
		parametersSource.put(key1, val1);
		parametersSource.put(key2, val2);
		ea.setConfigOptions(parametersSource);
		String configStr = ea.getConfiguration();
		assertEquals(configStr,"STATE=Standby, SUBSTATE=Idle");
		}
	
	public void testSetConfiguration()
		{
		ea.setConfiguration("STATE=Standby, SUBSTATE=Idle");
		Map<String, String> actionParameters = ea.getConfigOptions();
		assertNotNull(actionParameters);
		assertEquals(2,actionParameters.size());
		assertTrue(actionParameters.containsKey("STATE"));
		assertEquals("Standby",actionParameters.get("STATE") );
		assertTrue(actionParameters.containsKey("SUBSTATE"));
		assertEquals("Idle",actionParameters.get("SUBSTATE") );
		}
	
	public void testSetConfigurationNull()
		{
		ea.setConfiguration(null);
		Map<String, String> actionParameters = ea.getConfigOptions();
		assertNotNull(actionParameters);
		assertEquals(0,actionParameters.size());
		}

	}
