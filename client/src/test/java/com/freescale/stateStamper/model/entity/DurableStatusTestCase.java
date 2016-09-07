package com.freescale.stateStamper.model.entity;


import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Hashtable;

import junit.framework.TestCase;

public class DurableStatusTestCase extends TestCase
	{
	DurableStatus status;
	
	protected void setUp() throws Exception
		{
		status = new DurableStatus();
		super.setUp();
		}
	
	public void testGetStaticAttribute()
		{
		//set a value in a field
		String name = "DURABLE_ID";
		Object value = "XY";
		Object valueReturned;
		status.set(name, value);
		valueReturned=status.get(name);
		assertEquals(value, valueReturned);
		Integer statusPk=new Integer(1);
		status.set("STATUS_PK",statusPk);
		assertEquals(new Integer(1), status.get("STATUS_PK"));
		}
	
	public void testGetDynamicAttribute()
		{
		//set a value in a the hash table
		String name = "DYNAMIC";
		Object value = "XX";
		Object valueReturned;
		status.ATTRIBUTES = new Hashtable();
		status.set(name, value);
		valueReturned=status.get(name);
		assertEquals(value, valueReturned);
		}
	
	public void testClone()
		{
		status.EQUIPMENT_PK = 1;
		status.DURABLE_ID = "Dummy";
		status.duration = "7000";
		status.location = "Hades";
		status.OPERATOR_ID = "Myself";
		status.STATUS_PK = 1;
		status.STATE = "Productive";
		status.SUBSTATE = "Testing";
		status.TIME = "2012-21-21 00:00:00";
		status.workstation = "HAL";
		Hashtable attribs = new Hashtable();
		attribs.put("dyna","mite");
		status.ATTRIBUTES = attribs;
		DurableStatus clone = null;
		try{clone=(DurableStatus)status.clone();}
		catch(CloneNotSupportedException cle){fail("clone must be supported");}
		assertEquals(clone.EQUIPMENT_PK,status.EQUIPMENT_PK);
		assertEquals(clone.DURABLE_ID  ,status.DURABLE_ID  );
		assertEquals(clone.COMMENTS    ,status.COMMENTS    );
		assertEquals(clone.duration    ,status.duration    );
		assertEquals(clone.location    ,status.location    );
		assertEquals(clone.OPERATOR_ID ,status.OPERATOR_ID );
		assertEquals(clone.STATUS_PK   ,status.STATUS_PK   );
		assertEquals(clone.STATE       ,status.STATE       );
		assertEquals(clone.SUBSTATE    ,status.SUBSTATE    );
		assertEquals(clone.TIME        ,status.TIME        );
		assertEquals(clone.workstation ,status.workstation );
		assertEquals(clone.ATTRIBUTES  ,status.ATTRIBUTES  );
		status.ATTRIBUTES.put("dyna", "updated");
		assertNotSame(clone.ATTRIBUTES  ,status.ATTRIBUTES );
		}
	
	
	public void testGetCurrentStateDuration()
		{
		DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		status.TIME =  format.format(new Date());
		try	{Thread.sleep(1000);}
		catch (InterruptedException e){e.printStackTrace();}
		long duration = status.getCurrentStateDuration(); 
		System.out.println(duration);
		assertTrue(duration >= 1000);  
		}
	
	public void testGenericGetCurrentStateDuration()
		{
		DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		status.TIME =  format.format(new Date());
		String currentDuration = status.get(DurableStatus.CURRENT_STATE_DURATION).toString();
		}
	
	}
