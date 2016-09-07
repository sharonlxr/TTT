package com.freescale.stateStamper.model.entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import junit.framework.TestCase;

public class ReportTestCase extends TestCase {
    
	Report report;
	protected void setUp() throws Exception {
		report = new Report();
		super.setUp();
	}

	/**
	 * Test when Report has no Machines  defined 
	 * */
	public void testGetMachinesAsString_null() {
		assertNull(report.getMachinesAsString());		
	}
	/**
	 * Test when the Report has a simple machine list
	 * */
	public void testGetMAchinesAsString(){
		Map macs= new HashMap();
		Machine mac1= new Machine();
		mac1.mESName ="Mac1";
		macs.put(mac1.mESName, mac1);
		report.machines = macs;
		assertEquals("Mac1",report.getMachinesAsString()); 
		Machine mac2 = new Machine();
		mac2.mESName = "Mac2";
		macs.put(mac2.mESName, mac1);
		report.machines = macs;
		assertEquals("Mac1,Mac2",report.getMachinesAsString());
	}
	
	/**
	 * Test when the Report has a machine list and exclusion list
	 * */
	public void testGetMachinesAsString_Exclusion(){
		Map macs= new HashMap();

		Machine mac1= new Machine();
		mac1.mESName ="Mac1";
		macs.put(mac1.mESName, mac1);
		
		Machine mac2 = new Machine();
		mac2.mESName = "Mac2";
		macs.put(mac2.mESName, mac1);
		
		Machine mac3 = new Machine();
		mac3.mESName = "Mac3";
		macs.put(mac3.mESName, mac1);

		report.machines = macs;
		
		Vector v = new Vector();
		Machine mac4 = new Machine();
		mac4.mESName ="Mac2";
		v.add(mac4);
		report.exclusions = v;
		assertEquals("Mac1,Mac3",report.getMachinesAsString());
	}
	
	public void testGetDatesAsString(){
		String [] dates={"1999-12","2000-1"};
		report.dates = dates;
	    assertEquals("1999-12,2000-1", report.getDatesAsString());
	    List l= new ArrayList();
	    l.add("1");
	    l.add("2");
	    String [] in=null;
	    in = (String[])l.toArray(new String[0]);
	    
	}

}
