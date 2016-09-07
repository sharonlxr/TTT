package com.freescale.stateStamper.model.logic;



import java.util.Hashtable;
import java.util.List;
import java.util.Vector;



import com.freescale.stateStamper.model.entity.Report;
import com.freescale.stateStamper.model.logic.ProcessRecord;

import junit.framework.TestCase;

public class ProcessRecordTestCase extends TestCase {

	 //for cct79468
	 public void testConvertElapsedTime(){
		 String variable = "RESPONSE_TIME(SITE_DISABLED_RESPONSE_TIME)";

		 String record = "ATS-149\t2011-01-11 15:33:39\tNon Scheduled\tNon Scheduled\t\t\t\t25.0\t\t\t4\tQHD-151\t03\tb26259\tB\t\t\t\t\tQRN\t20101030_01169_B\t1.31.2\t\t\t960006\t1\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t2011-01-11\t40044\t1\t1\ttset\t21\t";	 
		 String date = "2011-01-11-1" ; 
		 Hashtable variableIndex = new Hashtable();
		 variableIndex.put("SITE_DISABLED_RESPONSE_TIME", 49);
		 assertEquals("00:00:40",ProcessRecord.getValue(variable, record, date, variableIndex) );
		
	 }
	 
	 //for cct79469
	 public void testTrueFunction(){
		 
		 String variable = "TRUE(RETEST)";

		 String record = "ATS-149\t2011-02-02 00:00:12\tStandby\tStandby\tTJMEA0WAZK00\tMC13892AJVL\tMC13892_X4_Rev_F2.xls\t25.0\tN\tN\t4\tQHD-151\t01\tEG325\tA\t\t\t\tGM32WA\tFRN\t20101030_01169_B\t1.31.2\tGenesis\tGM32WA\t\t\t\t\t\t\t30000\t1\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t";	
		 String date = "2011-02-02-2" ; 
		 Hashtable variableIndex = new Hashtable();
		 variableIndex.put("RETEST", 8);
		 assertEquals("false",ProcessRecord.getValue(variable, record, date, variableIndex));
		 
	      
	     record = "ATS-149\t2011-02-02 00:00:12\tStandby\tStandby\tTJMEA0WAZK00\tMC13892AJVL\tMC13892_X4_Rev_F2.xls\t25.0\tY\tN\t4\tQHD-151\t01\tEG325\tA\t\t\t\tGM32WA\tFRN\t20101030_01169_B\t1.31.2\tGenesis\tGM32WA\t\t\t\t\t\t\t30000\t1\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t";	
	     assertEquals("true",ProcessRecord.getValue(variable, record, date, variableIndex));
	 
	     record = "ATS-149\t2011-02-02 00:00:12\tStandby\tStandby\tTJMEA0WAZK00\tMC13892AJVL\tMC13892_X4_Rev_F2.xls\t25.0\t1\tN\t4\tQHD-151\t01\tEG325\tA\t\t\t\tGM32WA\tFRN\t20101030_01169_B\t1.31.2\tGenesis\tGM32WA\t\t\t\t\t\t\t30000\t1\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t";	
	     assertEquals("true",ProcessRecord.getValue(variable, record, date, variableIndex));
	     
	 }
	 
	 //for cct79469
	 public void testFalseFunction(){
		 
		 String variable = "FALSE(RETEST)";

		 String record = "ATS-149\t2011-02-02 00:00:12\tStandby\tStandby\tTJMEA0WAZK00\tMC13892AJVL\tMC13892_X4_Rev_F2.xls\t25.0\tN\tN\t4\tQHD-151\t01\tEG325\tA\t\t\t\tGM32WA\tFRN\t20101030_01169_B\t1.31.2\tGenesis\tGM32WA\t\t\t\t\t\t\t30000\t1\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t";	
		 String date = "2011-02-02-2" ; 
		 Hashtable variableIndex = new Hashtable();
		 variableIndex.put("RETEST", 8);
		 assertEquals("true",ProcessRecord.getValue(variable, record, date, variableIndex));
		 
	      
	     record = "ATS-149\t2011-02-02 00:00:12\tStandby\tStandby\tTJMEA0WAZK00\tMC13892AJVL\tMC13892_X4_Rev_F2.xls\t25.0\tY\tN\t4\tQHD-151\t01\tEG325\tA\t\t\t\tGM32WA\tFRN\t20101030_01169_B\t1.31.2\tGenesis\tGM32WA\t\t\t\t\t\t\t30000\t1\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t";	
	     assertEquals("false",ProcessRecord.getValue(variable, record, date, variableIndex));
	 
	     record = "ATS-149\t2011-02-02 00:00:12\tStandby\tStandby\tTJMEA0WAZK00\tMC13892AJVL\tMC13892_X4_Rev_F2.xls\t25.0\t1\tN\t4\tQHD-151\t01\tEG325\tA\t\t\t\tGM32WA\tFRN\t20101030_01169_B\t1.31.2\tGenesis\tGM32WA\t\t\t\t\t\t\t30000\t1\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t";	
	     assertEquals("false",ProcessRecord.getValue(variable, record, date, variableIndex));
	     
	 }
	 //for cct79469
	 // testTOTAL_FIRST_PASS_QTY
	 public void testTotalRetestQty(){
		 
		 String variable = "TOTAL_RETEST_QTY(DEVICES_PROCESSED)";

		 String record = "ATS-145\t2011-02-01 00:00:35\tProductive\tTest\tLJMHA1C5XZ00\tSC900652CAG\tPCU06ESP_20110107.xls\t-40.0\tY\tN\t2\tQHD-121\t6\tEM501\tA\t\t\t\tCNNS652C\tFCBx2\t20101030_01169_B\t1.31.2\tGenesis\tCNNS652C\t\t\t\t50\t\t\t301002\t1\t35\t70\t59\t255327\t49828\t30\t35\t29\t35\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t";	
		 String date = "2011-02-01-2" ; 
		 Hashtable variableIndex = new Hashtable();
		 variableIndex.put("RETEST", 8);
		 variableIndex.put("DEVICES_PROCESSED",33);
		 assertEquals("70",ProcessRecord.getValue(variable, record, date, variableIndex));
		 
	      
	     record = "ATS-145\t2011-02-01 00:00:35\tProductive\tTest\tLJMHA1C5XZ00\tSC900652CAG\tPCU06ESP_20110107.xls\t-40.0\tN\tN\t2\tQHD-121\t6\tEM501\tA\t\t\t\tCNNS652C\tFCBx2\t20101030_01169_B\t1.31.2\tGenesis\tCNNS652C\t\t\t\t50\t\t\t301002\t1\t35\t70\t59\t255327\t49828\t30\t35\t29\t35\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t";	
	     assertEquals("0",ProcessRecord.getValue(variable, record, date, variableIndex));
	 
	     record = "ATS-145\t2011-02-01 00:00:35\tProductive\tTest\tLJMHA1C5XZ00\tSC900652CAG\tPCU06ESP_20110107.xls\t-40.0\t1\tN\t2\tQHD-121\t6\tEM501\tA\t\t\t\tCNNS652C\tFCBx2\t20101030_01169_B\t1.31.2\tGenesis\tCNNS652C\t\t\t\t50\t\t\t301002\t1\t35\t70\t59\t255327\t49828\t30\t35\t29\t35\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t";	
	     assertEquals("70",ProcessRecord.getValue(variable, record, date, variableIndex));
	     
	 }
	 // testTOTAL_RETEST_QTY
	 public void testTotalFirstPassQTY(){
		 
		 String variable = "TOTAL_FIRST_PASS_QTY(DEVICES_PROCESSED)";

		 String record = "ATS-145\t2011-02-01 00:00:35\tProductive\tTest\tLJMHA1C5XZ00\tSC900652CAG\tPCU06ESP_20110107.xls\t-40.0\tY\tN\t2\tQHD-121\t6\tEM501\tA\t\t\t\tCNNS652C\tFCBx2\t20101030_01169_B\t1.31.2\tGenesis\tCNNS652C\t\t\t\t50\t\t\t301002\t1\t35\t70\t59\t255327\t49828\t30\t35\t29\t35\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t";	
		 String date = "2011-02-01-2" ; 
		 Hashtable variableIndex = new Hashtable();
		 variableIndex.put("RETEST", 8);
		 variableIndex.put("DEVICES_PROCESSED",33);
		 assertEquals("0",ProcessRecord.getValue(variable, record, date, variableIndex));
		 
	      
	     record = "ATS-145\t2011-02-01 00:00:35\tProductive\tTest\tLJMHA1C5XZ00\tSC900652CAG\tPCU06ESP_20110107.xls\t-40.0\tN\tN\t2\tQHD-121\t6\tEM501\tA\t\t\t\tCNNS652C\tFCBx2\t20101030_01169_B\t1.31.2\tGenesis\tCNNS652C\t\t\t\t50\t\t\t301002\t1\t35\t70\t59\t255327\t49828\t30\t35\t29\t35\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t";	
	    
	     assertEquals("70",ProcessRecord.getValue(variable, record, date, variableIndex));
	 
	     record = "ATS-145\t2011-02-01 00:00:35\tProductive\tTest\tLJMHA1C5XZ00\tSC900652CAG\tPCU06ESP_20110107.xls\t-40.0\t1\tN\t2\tQHD-121\t6\tEM501\tA\t\t\t\tCNNS652C\tFCBx2\t20101030_01169_B\t1.31.2\tGenesis\tCNNS652C\t\t\t\t50\t\t\t301002\t1\t35\t70\t59\t255327\t49828\t30\t35\t29\t35\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t";	
	     assertEquals("0",ProcessRecord.getValue(variable, record, date, variableIndex));
	     
	 }
	 
	public void testGetEnabledBin8Count()
		{
		String variable = "ENABLED_BIN8_COUNT(DEVICES_PROCESSED)";
		int site1 = 7;
		int site2 = 11;
		int site3 = 13;
		String record = "KKJ750-116\t2015-04-16 11:46:07\tProductive\tTest\t\t\t\t7\t"+site1 +"\t"+site2 + "\t"+site3+"\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t2\t5\t2\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t1\t1\t1\t1\t1\t1\t1\t1\t1\t0\t1\t1\t1\t1\t1\t1";
		String date = "2015-04-16-2";
		Hashtable variableIndex = new Hashtable();
		variableIndex.put("DEVICES_PROCESSED", 7);
		variableIndex.put("SITE1_HB8", 8);
		variableIndex.put("SITE2_HB8", 9);
		assertEquals(String.valueOf(site1+site2), ProcessRecord.getValue(variable, record, date, variableIndex));
		variableIndex.put("SITE3_HB8", 10);
		assertEquals(String.valueOf(site1+site2+site3), ProcessRecord.getValue(variable, record, date, variableIndex));
		}
	 
	public void testDuplicate()
		{
		String variable = "DUPLICATE(JAM_NUMBER)";
		// record 2 1 minutes difference should be removed
		String record1 = "ATS-145\t2011-02-01 00:00:00\tProductive\tTest\t3.5";
		String record2 = "ATS-145\t2011-02-01 00:01:00\tProductive\tTest\t3.5";
		// record 4 exact same time should be removed
		String record3 = "ATS-145\t2011-02-01 00:11:01\tProductive\tTest\t3.5";
		String record4 = "ATS-145\t2011-02-01 00:11:01\tProductive\tTest\t3.5";
		// second time between the 5 minutes but with a different jam number
		// should not be considered duplicate
		String record5 = "ATS-145\t2011-02-01 00:30:00\tProductive\tTest\t3.5";
		String record6 = "ATS-145\t2011-02-01 00:31:00\tProductive\tTest\t3.7";

		List records = new Vector();
		records.add(record1);
		records.add(record2);
		records.add(record3);
		records.add(record4);
		records.add(record5);
		records.add(record6);
		Hashtable data = new Hashtable();
		data.put("WW", records);

		Report report = new Report();
		report.condition = variable;
		report.period = FiscalDate.RAW;
		// first 4 columns are empty, are reporting information not needed for this tc
		String[] columns = { "", "", "", "", "MID", "TIME", "STATE", "SUBSTATE", "JAM_NUMBER" };
		report.setColumns(columns);
		ProcessRecord.postProcess(data, report, 3);
		records = (List) data.get("WW");
		assertEquals(4, records.size());
		}
	 
	 public void testTrim(){
		 String record="2001-52";
		 String date=record;
		 String variable="TRIM(TIME)";
		 Hashtable variableIndex = new Hashtable();
		 variableIndex.put("TIME", 0);
		 assertEquals("200152",ProcessRecord.getValue(variable, record, date, variableIndex));
	 }
	 
	 public void testProductive(){
		 String record ="Productive";
		 String date ="2001-52";
		 String variable="PRODUCTIVE(STATE)";
		 Hashtable variableIndex = new Hashtable();
		 variableIndex.put("STATE",0);
		 assertEquals("true", ProcessRecord.getValue(variable, record, date, variableIndex));
		 record="Standby";
		 assertEquals("false", ProcessRecord.getValue(variable, record, date, variableIndex));
	 }
	 
	public void testNotNull()
		{
		String date = "2001-52";
		String variable = "NOT_NULL(DEVICE_PROCESSED)";
		Hashtable variableIndex = new Hashtable();
		variableIndex.put("DEVICE_PROCESSED", 0);
		String record = "0.0";
		assertEquals("false", ProcessRecord.getValue(variable, record, date, variableIndex));
		record = "0";
		assertEquals("false", ProcessRecord.getValue(variable, record, date, variableIndex));
		record = "";
		assertEquals("false", ProcessRecord.getValue(variable, record, date, variableIndex));
		record = "0 0";
		assertEquals("false", ProcessRecord.getValue(variable, record, date, variableIndex));
		record = "1";
		assertEquals("true", ProcessRecord.getValue(variable, record, date, variableIndex));
		record = "A";
		assertEquals("true", ProcessRecord.getValue(variable, record, date, variableIndex));
		}
	
	public void testPnpAvailable()
		{
		String date = "2001-52";
		String variable = "PNP_AVAILABLE(PNP_SITES_AVAILABLE)";
		Hashtable variableIndex = new Hashtable();
		variableIndex.put("PNP_SITES_AVAILABLE", 0);
		variableIndex.put("IPNP_SITES_AVAILABLE", 1);
		variableIndex.put("OPNP_SITES_AVAILABLE", 2);
		variableIndex.put("TPNP_SITES_AVAILABLE", 3);
		String record = "0\t0\t0\t0";
		assertEquals("false", ProcessRecord.getValue(variable, record, date, variableIndex));
		record = "1\t0\t0\t0";
		assertEquals("true", ProcessRecord.getValue(variable, record, date, variableIndex));
		record = "0\t1\t0\t0";
		assertEquals("true", ProcessRecord.getValue(variable, record, date, variableIndex));
		record = "0\t0\t1\t0";
		assertEquals("true", ProcessRecord.getValue(variable, record, date, variableIndex));
		record = "0\t0\t0\t1";
		assertEquals("true", ProcessRecord.getValue(variable, record, date, variableIndex));
		}
	 
}
