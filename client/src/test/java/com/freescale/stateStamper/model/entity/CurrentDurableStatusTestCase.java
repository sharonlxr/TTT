package com.freescale.stateStamper.model.entity;

import java.util.Hashtable;

import junit.framework.TestCase;

public class CurrentDurableStatusTestCase extends TestCase
	{
	CurrentDurableStatus currentStatus;
	protected void setUp() throws Exception
		{
		super.setUp();
		currentStatus = new CurrentDurableStatus();
		}
	
	public void testStaticAttribute()
		{
		//set a value in a field from parent
		String name = "DURABLE_ID";
		Object value = "XY";
		Object valueReturned;
		currentStatus.set(name, value);
		valueReturned=currentStatus.get(name);
		assertEquals(value, valueReturned);
		
		name = "eIName";
		value = "eiNameValue";
		currentStatus.set(name, value);
		valueReturned=null;
		valueReturned=currentStatus.get(name);
		assertEquals(value, valueReturned);
		
		name ="type";
		value="J750";
		currentStatus.set(name, value);
		assertEquals(value,currentStatus.get(name));
		
		name = "location";
		value="Austin";
		currentStatus.set(name,value);
		assertEquals(value,currentStatus.get(name) );
		
		}
	
	public void testDynamicAttribute()
		{
		String key1="key1";
		String value1="value1";
		currentStatus.dynamicAttributes= new Hashtable();
		currentStatus.dynamicAttributes.put(key1, value1);
		assertEquals(value1,currentStatus.get(key1));
		}
	}
