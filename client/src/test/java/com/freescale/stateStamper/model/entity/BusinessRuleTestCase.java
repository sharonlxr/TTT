package com.freescale.stateStamper.model.entity;

import java.util.Hashtable;
import java.util.Vector;

import junit.framework.TestCase;

public class BusinessRuleTestCase extends TestCase
	{
	BusinessRule rule;
	
	protected void setUp() throws Exception
		{
		rule = new BusinessRule();
		super.setUp();
		}
	
	public void testConditionsTrueDurable()
		{
		BusinessRuleCondition cond1= new BusinessRuleCondition();
		cond1.setAttribute("DURATION");
		cond1.setCondition("=");
		cond1.setCondValue("100");
		
		BusinessRuleCondition cond2= new BusinessRuleCondition();
		cond2.setAttribute("DEVICES_PROCESSED");
		cond2.setCondition(">");
		cond2.setCondValue("500");
		
		BusinessRuleCondition cond3= new BusinessRuleCondition();
		cond3.setAttribute("DEVICES_PROCESSED");
		cond3.setCondition(">");
		cond3.setCondValue("90%MAX_DEVICES_PROCESSED");
		assertEquals(new Double(0.90),cond3.getPercentage());
		
		Vector conds = new Vector();
		conds.add(cond1);
		conds.add(cond2);
		rule.setConditions(conds);
		
		CurrentDurableStatus status = new CurrentDurableStatus();
		status.dynamicAttributes = new Hashtable();
		status.dynamicAttributes.put("DURATION", "100");
		status.dynamicAttributes.put("DEVICES_PROCESSED","1000");
		status.ATTRIBUTES = new Hashtable();
		status.ATTRIBUTES.put("MAX_DEVICES_PROCESSED", "500");
		assertTrue(rule.conditionsTrue(status));
			
		status.dynamicAttributes.put("DEVICES_PROCESSED","100");
		
		assertFalse(rule.conditionsTrue(status));
		}
	
	
	public void testConditionsIsMet(){
		
		BusinessRuleCondition cond1= new BusinessRuleCondition();
		cond1.setAttribute("PACKAGE_TYPE");
		cond1.setCondition("=");
		cond1.setCondValue("689 TEPBGA/P2020/ATC");
		
		BusinessRuleCondition cond2= new BusinessRuleCondition();
		cond2.setAttribute("platform");
		cond2.setCondition("=");
		cond2.setCondValue("Tiger");
		
		BusinessRuleCondition cond3= new BusinessRuleCondition();
		cond3.setAttribute("deviceGroup");
		cond3.setCondition("=");
		cond3.setCondValue("NA");
		
		BusinessRuleCondition cond4= new BusinessRuleCondition();
		cond4.setAttribute("PIN_CONDITION");
		cond4.setCondition("=");
		cond4.setCondValue("GOOD");
		
		Vector conds = new Vector();
		conds.add(cond1);
		conds.add(cond2);
		conds.add(cond3);
		conds.add(cond4);
		rule.setConditions(conds);
		
		CurrentDurableStatus status = new CurrentDurableStatus();
		status.deviceGroup = "NA";
		status.platform = "Tiger";
		status.dynamicAttributes = new Hashtable();
		status.dynamicAttributes.put("PACKAGE_TYPE", "689 TEPBGA/P2020/ATC");
		
		status.ATTRIBUTES = new Hashtable();
		status.ATTRIBUTES.put("PIN_CONDITION", "GOOD");
		assertTrue(rule.conditionsTrue(status));
		
	}
	
	public void testConditionsTrueTester()
		{
		BusinessRuleCondition cond1= new BusinessRuleCondition();
		cond1.setAttribute("DURATION");
		cond1.setCondition("=");
		cond1.setCondValue("100");
		
		BusinessRuleCondition cond2= new BusinessRuleCondition();
		cond2.setAttribute("DEVICES_PROCESSED");
		cond2.setCondition(">");
		cond2.setCondValue("500");
		
		Vector conds = new Vector();
		conds.add(cond1);
		conds.add(cond2);
		rule.setConditions(conds);
		
		Status status = new Status();
		status.set("DURATION", "100");
		status.set("DEVICES_PROCESSED","1000");
		assertTrue(rule.conditionsTrue(status));
			
		status.set("DEVICES_PROCESSED","100");
		assertFalse(rule.conditionsTrue(status));
		}

	
	}
