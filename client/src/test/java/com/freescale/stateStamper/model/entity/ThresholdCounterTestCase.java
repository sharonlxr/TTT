package com.freescale.stateStamper.model.entity;

import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

public class ThresholdCounterTestCase extends TestCase
	{
	ThresholdCounter counter;
	protected void setUp() throws Exception
		{
		super.setUp();
		counter = new ThresholdCounter("TOUCHDOWN");
		}
	
	public void testIncrementServiceUsage()
		{
		assertEquals(0, counter.getServiceUsage());
		counter.incrementServiceUsage(100);
		assertEquals(100, counter.getServiceUsage());
		counter.incrementServiceUsage(100);
		assertEquals(200, counter.getServiceUsage());
		}
	
	public void testIsServiceUsageLimitReached()
		{
		counter.setServiceUsageLimit(1000);
		counter.incrementServiceUsage(100);
		assertFalse(counter.isServiceUsageLimitReached() );
		counter.incrementServiceUsage(900);
		assertTrue(counter.isServiceUsageLimitReached());
		}
	
	public void testResetServiceUsage()
		{
		counter.incrementServiceUsage(100);
		assertEquals(100, counter.getServiceUsage());
		assertEquals(100, counter.getLifeUsage());
		counter.resetServiceUsage();
		assertEquals(0, counter.getServiceUsage());
		assertEquals(100, counter.getLifeUsage());
		}
	
	public void testResetRebuildUsage()
		{
		counter.setRebuildUsage(100);
		assertEquals(100,counter.getRebuildUsage() );
		counter.resetRebuildUsage();
		assertEquals(0, counter.getRebuildUsage());
		}
	
	public void testGetAsMap()
		{
		Map values=counter.getAsMap();
		assertTrue( values.containsKey(counter.getName()));
		assertEquals("ThresholdCounter",values.get(counter.getName()));
		Set keys =values.keySet();
		Iterator it = keys.iterator();
		while(it.hasNext())
			{
			int idx = it.next().toString().indexOf(counter.getName());
			 assertEquals(0,idx ) ;
			}
		
		counter.incrementServiceUsage(100);
		values =counter.getAsMap();
		Object su = values.get(counter.getName()+"$"+"serviceUsage");
		Object lu =  values.get(counter.getName()+"$"+"lifeUsage");
		assertEquals(new Long(100), su);
		assertEquals(new Long(100), lu);
		}
	
	public void testGetAttributesMap()
		{
		Map attributes= counter.getAttributesMap();
		EquipmentAttribute attr = (EquipmentAttribute)attributes.get("TOUCHDOWN$units");
		attr = (EquipmentAttribute)attributes.get("TOUCHDOWN$eIVariable");
		
		assertTrue( attributes.containsKey(counter.getName()));
//		assertEquals("ThresholdCounter",attributes.get(counter.getName()));
		Set keys =attributes.keySet();
		Iterator it = keys.iterator();
		while(it.hasNext())
			{
			int idx = it.next().toString().indexOf(counter.getName());
			 assertEquals(0,idx ) ;
			}
		Collection c = attributes.values();
		it=c.iterator();
		while(it.hasNext())
			{
			EquipmentAttribute attribute =(EquipmentAttribute)it.next();
			String type = attribute.type;
			assertTrue(type.equals("Text") || type.equals("Counter") || type.equals("ThresholdCounter"));
			}
		}
	
	public void _testSetAsMap()
		{
		counter.incrementServiceUsage(100);
		assertEquals(100, counter.getServiceUsage());
		counter.setName("POLISH");
		Map values = new HashMap();
		values.put("POLISH", "ThresholdCounter");
		values.put("POLISH$serviceUsage", "500");
		counter.setAsMap(values);
		Map valuesAfter = counter.getAsMap();
		Set entries =values.entrySet();
		Iterator it = entries.iterator();
		while(it.hasNext())
			{
			Map.Entry entry = (Map.Entry)it.next();
			assertTrue(valuesAfter.containsKey( entry.getKey()));
			assertEquals(entry.getValue().toString(),valuesAfter.get(entry.getKey()).toString());
			}
		assertEquals(500, counter.getServiceUsage());
		}
	
	public void testLoadFromStatus()
		{
		DurableStatus status = new DurableStatus();
		status.ATTRIBUTES = new Hashtable();
		status.set("TOUCHDOWN$serviceUsage","100");
		status.set("TOUCHDOWN$lifeUsage","1000");
		status.set("TOUCHDOWN$eIVariable","TOUCH_INCREMENT");
		status.set("TOUCHDOWN","ThresholdCounter" );
		assertEquals(0, counter.getServiceUsage());
		assertEquals(0, counter.getLifeUsage());
		counter.loadFromStatus(status);
		assertEquals(100, counter.getServiceUsage());
		assertEquals(1000, counter.getLifeUsage());
		assertEquals("TOUCH_INCREMENT",counter.getEIVariable());
		}
	
	public void testUnloadToStatus()
		{
		DurableStatus status = new DurableStatus();
		status.ATTRIBUTES = new Hashtable();
		status.set("TOUCHDOWN$serviceUsage","0");
		status.set("TOUCHDOWN$lifeUsage","0");
		status.set("TOUCHDOWN$eiName", "TOUCH");
		assertEquals("0",status.get("TOUCHDOWN$serviceUsage"));
		assertEquals("0",status.get("TOUCHDOWN$lifeUsage"));
		counter.setServiceUsage(100);
		counter.setLifeUsage(1000);
		counter.unloadToStatus(status);
		assertEquals("100",status.get("TOUCHDOWN$serviceUsage"));
		assertEquals("1000",status.get("TOUCHDOWN$lifeUsage"));
		assertEquals("TOUCH", status.get("TOUCHDOWN$eiName"));
		
		counter.setRebuildUsage(200);
		counter.unloadToStatus(status,true);
		assertEquals("200",status.get("TOUCHDOWN$rebuildUsage"));
		}
	
	}
