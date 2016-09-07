package com.freescale.stateStamper.model.logic;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.freescale.stateStamper.model.entity.EquipmentType;
import com.freescale.stateStamper.model.entity.Machine;

import junit.framework.TestCase;

public class DurableManagerTestCase extends TestCase
	{
	DurableManager durableManager;

	protected void setUp() throws Exception
		{
		super.setUp();
		durableManager = new DurableManager();
		}
	

	public void testGetTotalsBy()
		{
		String platform1="J750",platform2="Ultra";
		Machine mac1 = new Machine(),mac2=new Machine(),mac3=new Machine(),mac4= new Machine();
		mac1.setPlatform(platform1);
		mac2.setPlatform(platform1);
		mac3.setPlatform(platform2);
		List equipments = new ArrayList();
		equipments.add(mac1);
		equipments.add(mac2);
		equipments.add(mac3);
		equipments.add(mac4);
		Map totals = DurableManager.getTotalsBy(equipments, "platform",null);
		assertNotNull(totals);
		assertEquals(3,totals.size());
		assertTrue( totals.containsKey(platform1));
		assertTrue( totals.containsKey(platform2));
		assertTrue( totals.containsKey(""));
		assertEquals(Integer.valueOf(2) , totals.get(platform1) );
		assertEquals(Integer.valueOf(1) , totals.get(platform2) );
		assertEquals(Integer.valueOf(1) , totals.get("") );
		
		List summary =  DurableManager.getSummaryfromTotals(totals);
		assertNotNull(summary);
		assertEquals(4,summary.size());
		}
	
	public void testRemoveMachinesByEquipmentType()
		{
		Machine tester,durable;
		tester = new Machine();
		EquipmentType testerType = new EquipmentType(),probecardType=new EquipmentType();
		testerType.name="Tester";
		probecardType.name="ProbeCard";
		tester.setEquipmentType(testerType);
		durable = new Machine();
		durable.setEquipmentType(probecardType);
		List equipments = new ArrayList();
		equipments.add(tester);
		equipments.add(durable);
		List durables = durableManager.removeMachinesByEquipmentType(equipments,"Tester");
		assertNotNull(durables);
		assertEquals(1, durables.size());
		}

	public void _testGetWorkWeekbasedCounterbarValue()
	   {
	    FiscalDate.setShifts("1,06:00:00,18:00:00,YYYYYYY;2,18:00:00,06:00:00,YYYYYYY","2013-01-01,1,A,A,A,A,C,C,C,C,B,B,B,B;2013-01-01,2,C,C,B,B,B,B,A,A,A,A,C,C");
		String value = DurableManager.getWorkWeekbasedCounterbarValue("2592000000\\1296000000 (20%)");		
		assertEquals(value,"2013-07");
	   }

	}
