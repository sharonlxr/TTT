package com.freescale.stateStamper.model.entity;

import java.util.Vector;


import junit.framework.TestCase;

public class EquipmentTypeTestCase extends TestCase
	{
	EquipmentType type;
	EquipmentAttribute statusAttr,equipmentAttr;
	String statName="Status_Attribute";
	String eqName="Equipment_Attribute";
	protected void setUp() throws Exception
		{
		type= new EquipmentType();
		statusAttr = new EquipmentAttribute();
		statusAttr.name=statName;
		statusAttr.belongs=EquipmentAttribute.STATUS;
		equipmentAttr = new EquipmentAttribute();
		equipmentAttr.name=eqName;
		equipmentAttr.belongs=EquipmentAttribute.EQUIPMENT;
		Vector attributes = new Vector();
		attributes.add(statusAttr);
		attributes.add(equipmentAttr);
		type.setAttributes(attributes);
		super.setUp();
		}
	
	public void testGetBusinessRulesBy()
		{
		BusinessRule rule1 = new BusinessRule();
		rule1.setEventName(BusinessRule.EPR_EVENT);
		
		BusinessRule rule2 = new BusinessRule();
		rule2.setEventName(BusinessRule.MONITOR);
		
		Vector rules = new Vector();
		rules.add(rule1);
		rules.add(rule2);
		
		type.setBusinessRules(rules);
		
		Vector res=type.getBusinessRulesBy(BusinessRule.EPR_EVENT);
		assertNotNull(res);
		assertEquals(1,res.size());
		assertEquals(BusinessRule.EPR_EVENT,((BusinessRule)res.get(0)).getEventName());
		
		res=type.getBusinessRulesBy(BusinessRule.MONITOR);
		assertNotNull(res);
		assertEquals(1,res.size());
		assertEquals(BusinessRule.MONITOR,((BusinessRule)res.get(0)).getEventName());
		
		}
	
	public void testGetAttributes()
		{
		assertNotNull(type.getAttributes());
		assertEquals(2,type.getAttributes().size());
		}
	
	public void testGetStatusAttributes()
		{
		assertNotNull(type.getStatusAttributes());
		assertEquals(1, type.getStatusAttributes().size());
		}
	
	public void testGetEquipmentAttributes()
		{
		assertNotNull(type.getEquipmentAttributes());
		assertEquals(1, type.getEquipmentAttributes().size());
		}
	
	public void testGetAttribute()
		{
		assertNotNull(type.getAttribute(statName));
		assertEquals(statusAttr, type.getAttribute(statName));
		}
	
	public void testGetAttributesNames()
		{
		assertNotNull(type.getAttributesNames());
		Vector names = type.getAttributesNames();
		assertEquals(2, names.size());
		assertTrue(names.contains(statName));
		assertTrue(names.contains(eqName));
		}
	
	}
