package com.freescale.stateStamper.model.entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import junit.framework.TestCase;

public class MachineTestCase extends TestCase
	{
	private Machine machine;
	private static final String RACK = "RACK"; 
	private static final String TOUCH = "TOUCH_INCREMENT";

	protected void setUp() throws Exception
		{
		super.setUp();
		machine = new Machine();
		machine.setEquipmentType(setUpEquipmentType());
		machine.setEquipmentType_pk(machine.getEquipmentType().primaryKey);
		Map<String,List<String>> childrenMap = new HashMap<String,List<String>>();
		List<String> arrays = new ArrayList<String>();
		arrays.add("socket1");
		arrays.add("socket2");
		childrenMap.put("SOCKET", arrays);
		machine.children = childrenMap;
		machine.context = new HashMap();
		DurableStatus dStatus = getDurableStatus();
		machine.context.put("durableStatus", dStatus);
		}
	
	protected DurableStatus getDurableStatus()
		{
		DurableStatus durableStatus = new DurableStatus();
		Map dynamicStatusAttributes = new HashMap();
		dynamicStatusAttributes.put(TOUCH,"0");
		durableStatus.setDynamicAttributes(dynamicStatusAttributes);
		return durableStatus;
		}
	protected EquipmentType setUpEquipmentType()
		{
		EquipmentAttribute equipmentAttribute = new EquipmentAttribute();
		equipmentAttribute.name = RACK;
		equipmentAttribute.type = EquipmentAttribute.TEXT;
		equipmentAttribute.belongs = EquipmentAttribute.EQUIPMENT;
		
		EquipmentAttribute statusAttribute = new EquipmentAttribute();
		statusAttribute.name = TOUCH;
		statusAttribute.type = EquipmentAttribute.COUNTER;
		statusAttribute.belongs = EquipmentAttribute.STATUS;
		Integer equipmentTypePk = Integer.valueOf(1);
		
		EquipmentType eqType = new EquipmentType();
		eqType.name = "Dummy";
		eqType.primaryKey= equipmentTypePk;
		equipmentAttribute.primaryKey = eqType.primaryKey;
		statusAttribute.primaryKey = eqType.primaryKey;
		Vector attributes = new Vector();
		attributes.add(equipmentAttribute);
		attributes.add(statusAttribute);
		eqType.setAttributes(attributes);
		return eqType;
		}

	public void testHas()
		{
		String property = "state";
		assertTrue(machine.has(property));
		String nonProperty = "invalid";
		assertFalse(machine.has(nonProperty));
        String dynAttribute = RACK;
        assertTrue(machine.has(dynAttribute) );
        String dynStatusAttribute = TOUCH;
        assertTrue(machine.has(dynStatusAttribute) );
        String statusAttribute = "SUBSTATE";
        assertTrue(machine.has(statusAttribute) );
		}
	
	public void testGet()
		{
		String property = "state";
		String state="Unknown";
		machine.setState(state);
		assertEquals(state,machine.get(property) );
		String nonProperty = "invalid";
		assertNull(machine.get(nonProperty));
		}
	
	public void testSet()
		{
		String property="state";
		String value ="Engineering";
		machine.set(property, value);
		assertEquals(value,machine.getState());
		String nonProperty ="invalid";
		machine.set(nonProperty, value);
		}
	
	public void testGetSetDynamic()
		{
		String dynEquipAttr = RACK;
		String equipAttrVal = "Rack1";
		machine.set(dynEquipAttr, equipAttrVal);
		assertEquals(equipAttrVal, machine.get(dynEquipAttr)); 
		
		String dynStatusAttr = TOUCH;
		String touchCount = "1";
		machine.set(dynStatusAttr, touchCount);
		assertEquals(touchCount, machine.get(dynStatusAttr));
		}
	
	public void testGetSetStatus()
		{
		String substate = "Running";
		machine.set("SUBSTATE",substate);
		assertEquals(substate, machine.get("SUBSTATE"));
		}
	
	public void testClone()
		{
		Machine clone = null;
		machine.setEIName("name");
		machine.context.put("key", "value");
		DurableStatus ds = (DurableStatus)machine.get("durableStatus");
		ds.STATUS_PK = 1;
		try{clone =(Machine)machine.clone();}
		catch(CloneNotSupportedException e){fail("Clone failed");}
		assertEquals(machine.getEIName(),clone.getEIName());
		assertEquals(machine.context.get("key"),clone.context.get("key"));
		assertEquals(((DurableStatus)machine.context.get("durableStatus")).STATUS_PK,((DurableStatus)clone.context.get("durableStatus")).STATUS_PK);
		assertEquals(machine.children.get("SOCKET"),clone.getChildren().get("SOCKET"));
		assertNotSame(machine.children.get("SOCKET"),clone.getChildren().get("SOCKET"));
		assertNotSame(machine.context.get("durableStatus"),clone.context.get("durableStatus"));
		clone.context.put("key", "value2");
		assertNotSame(machine.context, clone.context);
		}
	
	}
