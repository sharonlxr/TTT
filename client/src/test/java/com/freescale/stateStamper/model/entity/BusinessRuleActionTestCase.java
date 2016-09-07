package com.freescale.stateStamper.model.entity;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

public class BusinessRuleActionTestCase extends TestCase
	{
	BusinessRuleAction action;
	protected void setUp() throws Exception
		{
		super.setUp();
		action = new BusinessRuleAction();
		}
	
	public void testGetActionParameters()
		{
		Map parametersSource = new HashMap();
		String key1="STATE";
		String val1="Standby";
		String key2="SUBSTATE";
		String val2="Idle";
		parametersSource.put(key1, val1);
		parametersSource.put(key2, val2);
		action.setActionParameters(parametersSource);
		String parameterStr = action.getActionParameterStr();
		assertEquals(parameterStr,"STATE=Standby,SUBSTATE=Idle");
		}
	
	public void testSetActionParameters()
		{
		action.setActionParameterStr("STATE=Standby,SUBSTATE=Idle");
		Map<String, String> actionParameters = action.getActionParameters();
		assertNotNull(actionParameters);
		assertEquals(2,actionParameters.size());
		assertTrue(actionParameters.containsKey("STATE"));
		assertEquals("Standby",actionParameters.get("STATE") );
		assertTrue(actionParameters.containsKey("SUBSTATE"));
		assertEquals("Idle",actionParameters.get("SUBSTATE") );
		}
	
	public void testSetActionParametersNull()
		{
		action.setActionParameterStr(null);
		Map<String, String> actionParameters = action.getActionParameters();
		assertNotNull(actionParameters);
		assertEquals(0,actionParameters.size());
		}
	
	}
