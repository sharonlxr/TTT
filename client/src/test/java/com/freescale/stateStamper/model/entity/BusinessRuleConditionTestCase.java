package com.freescale.stateStamper.model.entity;

import java.util.Hashtable;

import junit.framework.TestCase;

public class BusinessRuleConditionTestCase extends TestCase
	{
	BusinessRuleCondition condition;
	protected void setUp() throws Exception
		{
		condition= new BusinessRuleCondition();
		super.setUp();
		}
	
	public void testIsMet()
		{
		condition.setAttribute("DURATION");
		condition.setCondition("=");
		condition.setCondValue("100");
		CurrentDurableStatus status = new CurrentDurableStatus();
		status.dynamicAttributes = new Hashtable();
		
		status.dynamicAttributes.put("DURATION","100");
		assertTrue(condition.isMet(status));
		
		status.dynamicAttributes.put("DURATION","200");
		assertFalse(condition.isMet(status));
		
		
		status.dynamicAttributes.put("DURATION","200");
		condition.setCondition(">");
		assertTrue(condition.isMet(status));

		condition.setCondition(">=");
		assertTrue(condition.isMet(status));
		
		status.dynamicAttributes.put("DURATION","10");
		condition.setCondition("<");
		assertTrue(condition.isMet(status));
		
		condition.setCondition("<=");
		assertTrue(condition.isMet(status));
		
		}
	
	public void testIsMetSecondAttribute()
		{
		condition.setAttribute("DEVICES_PROCESSED");
		condition.setCondition("=");
		condition.setCondValue("GOOD_DEVICES");
		CurrentDurableStatus status = new CurrentDurableStatus();
		status.dynamicAttributes = new Hashtable();
		status.dynamicAttributes.put("DEVICES_PROCESSED","100");
		status.dynamicAttributes.put("GOOD_DEVICES","200");
		assertFalse(condition.isMet(status));
		status.dynamicAttributes.put("GOOD_DEVICES","100");
		assertTrue(condition.isMet(status));
		
		condition.setCondValue("CNT_GOOD");
		Status status2 = new Status();
		status2.set("DEVICES_PROCESSED","100");
		status2.set("CNT_GOOD","200");
		assertFalse(condition.isMet(status2) );
		status2.set("CNT_GOOD","100");
		assertTrue(condition.isMet(status2) );
		}
	
	public void testPercentage()
		{
		condition.setAttribute("TOUCHDOWN");
		condition.setCondition(">");
		condition.setCondValue("90%TOTAL_TOUCHDOWN");
		assertEquals(new Double(.90),condition.getPercentage());
		assertEquals("TOTAL_TOUCHDOWN",condition.getCondValuewithoutPercentange());
		condition.setCondValue("80%TOTAL_MAX_TOUCHDOWN");
		assertEquals(new Double(.80),condition.getPercentage());
		assertEquals("TOTAL_MAX_TOUCHDOWN",condition.getCondValuewithoutPercentange());
		assertEquals("80%",condition.getPercentageStr());
		}
	
	public void testIsMetPercentage()
		{
		condition.setAttribute("TOUCHDOWN");
		condition.setCondition(">");
		condition.setCondValue("90%TOTAL_TOUCHDOWN");
		CurrentDurableStatus status = new CurrentDurableStatus();
		status.dynamicAttributes = new Hashtable();
		status.dynamicAttributes.put("TOTAL_TOUCHDOWN","100");
		status.dynamicAttributes.put("TOUCHDOWN","95");
		assertTrue(condition.isMet(status));
		status.dynamicAttributes.put("TOUCHDOWN","89");
		assertFalse(condition.isMet(status));
		
		condition.setCondition("=");
		status.dynamicAttributes.put("TOUCHDOWN","90");
		assertTrue(condition.isMet(status));
		}
	
	
		
	
	public void testIsIn()
		{
		condition.setAttribute("DEVICE_ID");
		condition.setCondition("in");
		condition.setCondValue("A,B,C");

		CurrentDurableStatus status = new CurrentDurableStatus();
		status.dynamicAttributes = new Hashtable();
		
		status.dynamicAttributes.put("DEVICE_ID", "B");
		assertTrue( condition.isMet(status));
		
		status.dynamicAttributes.put("DEVICE_ID", "Z");
		assertFalse(condition.isMet(status));

		}
	
	public void testDifferent()
		{
		//numeric values
		condition.setAttribute("CNT_GOOD");
		condition.setCondition("<>");
		condition.setCondValue("101");
		DurableStatus dStatus = new DurableStatus();
		dStatus.ATTRIBUTES  = new Hashtable();
		dStatus.ATTRIBUTES.put("CNT_GOOD", "99");
		assertTrue( condition.isMet(dStatus));
		dStatus.ATTRIBUTES.put("CNT_GOOD", "101");
		assertFalse( condition.isMet(dStatus));
		//alpha values
		condition.setAttribute("DEVICE_ID");
		condition.setCondition("<>");
		condition.setCondValue("ABC");
		dStatus.ATTRIBUTES  = new Hashtable();
		dStatus.ATTRIBUTES.put("DEVICE_ID", "DEF");
		assertTrue( condition.isMet(dStatus));
		dStatus.ATTRIBUTES.put("DEVICE_ID", "ABC");
		assertFalse( condition.isMet(dStatus));
		
		}
	
	public void testContains()
		{
		condition.setAttribute("AREA_FLEX");
		condition.setCondition("contains");
		condition.setCondValue("parent.platform");
		DurableStatus dStatus = new DurableStatus();
		dStatus.ATTRIBUTES  = new Hashtable();
		dStatus.ATTRIBUTES.put("AREA_FLEX", "[FLEX1,FLEX2,FLEX3]");
		dStatus.ATTRIBUTES.put("parent.platform", "FLEX");
		assertFalse( condition.isMet(dStatus));
		
		condition.setAttribute("AREA_FLEX");
		condition.setCondition("contains");
		condition.setCondValue("parent.platform");
		dStatus = new DurableStatus();
		dStatus.ATTRIBUTES  = new Hashtable();
		dStatus.ATTRIBUTES.put("AREA_FLEX", "FLEX1,FLEX2,FLEX3");
		dStatus.ATTRIBUTES.put("parent.platform", "FLEX1");
		assertTrue( condition.isMet(dStatus));
		
		condition.setAttribute("AREA_FLEX");
		condition.setCondition("contains");
		condition.setCondValue("parent.platform");
		dStatus = new DurableStatus();
		dStatus.ATTRIBUTES  = new Hashtable();
		dStatus.ATTRIBUTES.put("AREA_FLEX", "[FLEX1,FLEX2,FLEX3]");
		dStatus.ATTRIBUTES.put("parent.platform", "FLEX1");
		assertTrue( condition.isMet(dStatus));
		
		condition.setAttribute("AREA_FLEX");
		condition.setCondition("contains");
		condition.setCondValue("parent.platform");
		dStatus = new DurableStatus();
		dStatus.ATTRIBUTES  = new Hashtable();
		dStatus.ATTRIBUTES.put("AREA_FLEX", "FLEX1");
		dStatus.ATTRIBUTES.put("parent.platform", "FLEX1");
		assertTrue( condition.isMet(dStatus));
		}
	
	}
