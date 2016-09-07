package com.freescale.stateStamper.model.logic;


import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import com.freescale.serverUtil.ThreadSafeSimpleDateFormat;

import junit.framework.TestCase;

public class FiscalDateTestCase extends TestCase
	{
	private static final long MINUTE_MSECS = 60 * 1000;
	private static final long HOUR_MSECS = 60 * MINUTE_MSECS;
	private static final long DAY_MSECS = 24 * HOUR_MSECS;
	FiscalDate fiscalDate;
	ThreadSafeSimpleDateFormat formatter = new ThreadSafeSimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	protected void setUp() throws Exception
		{
		super.setUp();
		Properties properties = new Properties();
		properties.setProperty("fiscalCalendarSORHost", "fspeds.am.freescale.net");
		properties.setProperty("fiscalCalendarSORPort", "1521");
		properties.setProperty("fiscalCalendarSORSID", "fspeds");
		properties.setProperty("fiscalCalendarSORUser", "edsread");
		properties.setProperty("fiscalCalendarSORPassword", "edsread");
		FiscalDate.loadCalendarConnection(properties);
		}

	public void testGetElapsedTime()throws Exception
		{
		//"yyyy-MM-dd HH:mm:ss"
		String time = "2011-10-10 00:00:00";
		
		String elapsedTime = FiscalDate.getElapsedTime(time);
		String time2 = "2011-09-09 00:00:00";
		elapsedTime = FiscalDate.getElapsedTime(time2,time);
		assertEquals("1m 1d",elapsedTime );
		}
	
	public void testGetElapsedTimeMonth()throws Exception
		{
		String from = "2011-09-09 00:00:00";
		String to = "2011-10-10 00:00:00";
		//Month
		String elapsedTime = FiscalDate.getElapsedTime(from, to);
		assertEquals("1m 1d",elapsedTime );
		//Week
		to ="2011-09-19 00:00:00";
		elapsedTime = FiscalDate.getElapsedTime(from, to);
		assertEquals("10d 0h",elapsedTime);
		}
	
	public void testShifts()throws Exception
		{
		String shifts = "2,18:00:00,06:00:00,YYYYYYY;1,06:00:00,18:00:00,YYYYYYY";
		String crews = "2011-01-01,1,A,A,A,A,C,C,C,C,B,B,B,B;2011-01-01,2,C,C,B,B,B,B,A,A,A,A,C,C";
		FiscalDate.setShifts(shifts, crews);
		FiscalDate fisDate = new FiscalDate(FiscalDate.SHIFT,"2014-04-01 06:00:00");
		assertEquals("2014-04-01 06:00:00",fisDate.startTime);
		assertEquals("2014-04-01 18:00:00",fisDate.endTime);
		assertEquals("2014-04-01",fisDate.fDate);
		assertEquals(FiscalDate.SHIFT, fisDate.period);
		assertEquals("1", fisDate.shift);
		
		fisDate = new FiscalDate(FiscalDate.SHIFT,"2014-04-01 17:59:59");
		assertEquals("2014-04-01 06:00:00",fisDate.startTime);
		assertEquals("2014-04-01 18:00:00",fisDate.endTime);
		assertEquals("2014-04-01",fisDate.fDate);
		assertEquals(FiscalDate.SHIFT, fisDate.period);
		assertEquals("1", fisDate.shift);

		fisDate = new FiscalDate(FiscalDate.SHIFT,"2014-04-01 18:00:00");
		assertEquals("2014-04-01 18:00:00",fisDate.startTime);
		assertEquals("2014-04-02 06:00:00",fisDate.endTime);
		assertEquals("2014-04-02",fisDate.fDate);
		assertEquals(FiscalDate.SHIFT, fisDate.period);
		assertEquals("2", fisDate.shift);
		}
	
	
	public void testShiftComparator() throws Exception
		{
		String shifts = "2,18:00:00,06:00:00,YYYYYYY;1,06:00:00,18:00:00,YYYYYYY";
		String workWeeks = "2012-01-07 18:00:00,2013-01-05 18:00:00,2014-01-11 18:00:00";
		String crews = "2011-01-01,1,A,A,A,A,C,C,C,C,B,B,B,B;2011-01-01,2,C,C,B,B,B,B,A,A,A,A,C,C";
		FiscalDate.setShifts(shifts, crews);
		FiscalDate fisDate = new FiscalDate(FiscalDate.SHIFT, "2014-04-01 06:00:00");
		assertEquals("2014-04-01 06:00:00", fisDate.startTime);
		assertEquals("2014-04-01 18:00:00", fisDate.endTime);
		assertEquals("2014-04-01", fisDate.fDate);
		assertEquals(FiscalDate.SHIFT, fisDate.period);
		assertEquals("1", fisDate.shift);

		fisDate = new FiscalDate(FiscalDate.SHIFT, "2014-04-01 17:59:59");
		assertEquals("2014-04-01 06:00:00", fisDate.startTime);
		assertEquals("2014-04-01 18:00:00", fisDate.endTime);
		assertEquals("2014-04-01", fisDate.fDate);
		assertEquals(FiscalDate.SHIFT, fisDate.period);
		assertEquals("1", fisDate.shift);

		fisDate = new FiscalDate(FiscalDate.SHIFT, "2014-04-01 18:00:00");
		assertEquals("2014-04-01 18:00:00", fisDate.startTime);
		assertEquals("2014-04-02 06:00:00", fisDate.endTime);
		assertEquals("2014-04-02", fisDate.fDate);
		assertEquals(FiscalDate.SHIFT, fisDate.period);
		assertEquals("2", fisDate.shift);
		}
	
	public void testShiftComparatorOrdered() throws Exception
		{
		//Define first the shift 1, second the shift 2
		String shiftDefinition = "1,06:00:00,18:00:00,YYYYYYY;2,18:00:00,06:00:00,YYYYYYY";
		String crews = "2016-01-01,1,C,B,B,B,B,A,A,A,A,C,C,C;2016-01-01,2,A,A,A,C,C,C,C,B,B,B,B,A";
		FiscalDate.setShifts(shiftDefinition, crews);
		FiscalDate fd1 = new FiscalDate(FiscalDate.SHIFT, "2016-01-01", "1");
		FiscalDate fd2 = new FiscalDate(FiscalDate.SHIFT, "2016-01-01", "2");
		FiscalDate fd3 = new FiscalDate(FiscalDate.SHIFT, "2016-02-01", "1");
		FiscalDate fd4 = new FiscalDate(FiscalDate.SHIFT, "2016-02-01", "2");
		
		List<String> shifts = new ArrayList<String>();
		//add from 1 to 4
		shifts.add(fd1.toString());
		shifts.add(fd2.toString());
		shifts.add(fd3.toString());
		shifts.add(fd4.toString());
		Collections.sort(shifts,FiscalDate.SHIFT_STRING_COMPARATOR);
		assertEquals("2016-02-01-2",shifts.get(0).toString());
		assertEquals("2016-02-01-1",shifts.get(1).toString());
		assertEquals("2016-01-01-2",shifts.get(2).toString());
		assertEquals("2016-01-01-1",shifts.get(3).toString());
		
		//add from 4 to 1
		shifts.clear();
		shifts.add(fd4.toString());
		shifts.add(fd3.toString());
		shifts.add(fd2.toString());
		shifts.add(fd1.toString());
		Collections.sort(shifts,FiscalDate.SHIFT_STRING_COMPARATOR);
		assertEquals("2016-02-01-2",shifts.get(0).toString());
		assertEquals("2016-02-01-1",shifts.get(1).toString());
		assertEquals("2016-01-01-2",shifts.get(2).toString());
		assertEquals("2016-01-01-1",shifts.get(3).toString());
		
		//Define first the shift 2, second the shift 1
		shiftDefinition = "2,18:00:00,06:00:00,YYYYYYY;1,06:00:00,18:00:00,YYYYYYY";
		crews = "2016-01-01,1,C,B,B,B,B,A,A,A,A,C,C,C;2016-01-01,2,A,A,A,C,C,C,C,B,B,B,B,A";
		FiscalDate.setShifts(shiftDefinition, crews);
		fd1 = new FiscalDate(FiscalDate.SHIFT, "2016-01-01", "1");
		fd2 = new FiscalDate(FiscalDate.SHIFT, "2016-01-01", "2");
		fd3 = new FiscalDate(FiscalDate.SHIFT, "2016-02-01", "1");
		fd4 = new FiscalDate(FiscalDate.SHIFT, "2016-02-01", "2");
		
		shifts = new ArrayList<String>();
		//add from 1 to 4 
		shifts.add(fd1.toString());
		shifts.add(fd2.toString());
		shifts.add(fd3.toString());
		shifts.add(fd4.toString());
		Collections.sort(shifts,FiscalDate.SHIFT_STRING_COMPARATOR);
		assertEquals("2016-02-01-1",shifts.get(0).toString());
		assertEquals("2016-02-01-2",shifts.get(1).toString());
		assertEquals("2016-01-01-1",shifts.get(2).toString());
		assertEquals("2016-01-01-2",shifts.get(3).toString());
		
		//add from 4 to 1
		shifts.clear();
		shifts.add(fd4.toString());
		shifts.add(fd3.toString());
		shifts.add(fd2.toString());
		shifts.add(fd1.toString());
		Collections.sort(shifts,FiscalDate.SHIFT_STRING_COMPARATOR);
		assertEquals("2016-02-01-1",shifts.get(0).toString());
		assertEquals("2016-02-01-2",shifts.get(1).toString());
		assertEquals("2016-01-01-1",shifts.get(2).toString());
		assertEquals("2016-01-01-2",shifts.get(3).toString());
		
		
		//Define the shifts with alpha Ids  
		shiftDefinition = "A,06:00:00,18:00:00,YYYYYYY;B,18:00:00,06:00:00,YYYYYYY";
		crews = "2016-01-01,1,C,B,B,B,B,A,A,A,A,C,C,C;2016-01-01,2,A,A,A,C,C,C,C,B,B,B,B,A";
		FiscalDate.setShifts(shiftDefinition, crews);
		fd1 = new FiscalDate(FiscalDate.SHIFT, "2016-01-01", "A");
		fd2 = new FiscalDate(FiscalDate.SHIFT, "2016-01-01", "B");
		fd3 = new FiscalDate(FiscalDate.SHIFT, "2016-02-01", "A");
		fd4 = new FiscalDate(FiscalDate.SHIFT, "2016-02-01", "B");
		
		shifts = new ArrayList<String>();
		//add from 1 to 4 
		shifts.add(fd1.toString());
		shifts.add(fd2.toString());
		shifts.add(fd3.toString());
		shifts.add(fd4.toString());
		Collections.sort(shifts,FiscalDate.SHIFT_STRING_COMPARATOR);
		assertEquals("2016-02-01-B",shifts.get(0).toString());
		assertEquals("2016-02-01-A",shifts.get(1).toString());
		assertEquals("2016-01-01-B",shifts.get(2).toString());
		assertEquals("2016-01-01-A",shifts.get(3).toString());
		
		//add from 4 to 1
		shifts.clear();
		shifts.add(fd4.toString());
		shifts.add(fd3.toString());
		shifts.add(fd2.toString());
		shifts.add(fd1.toString());
		Collections.sort(shifts,FiscalDate.SHIFT_STRING_COMPARATOR);
		assertEquals("2016-02-01-B",shifts.get(0).toString());
		assertEquals("2016-02-01-A",shifts.get(1).toString());
		assertEquals("2016-01-01-B",shifts.get(2).toString());
		assertEquals("2016-01-01-A",shifts.get(3).toString());

		
		//Define the shifts with alpha Ids  
		shiftDefinition = "B,06:00:00,18:00:00,YYYYYYY;A,18:00:00,06:00:00,YYYYYYY";
		crews = "2016-01-01,1,C,B,B,B,B,A,A,A,A,C,C,C;2016-01-01,2,A,A,A,C,C,C,C,B,B,B,B,A";
		FiscalDate.setShifts(shiftDefinition, crews);
		fd1 = new FiscalDate(FiscalDate.SHIFT, "2016-01-01", "A");
		fd2 = new FiscalDate(FiscalDate.SHIFT, "2016-01-01", "B");
		fd3 = new FiscalDate(FiscalDate.SHIFT, "2016-02-01", "A");
		fd4 = new FiscalDate(FiscalDate.SHIFT, "2016-02-01", "B");
		
		shifts = new ArrayList<String>();
		//add from 1 to 4 
		shifts.add(fd1.toString());
		shifts.add(fd2.toString());
		shifts.add(fd3.toString());
		shifts.add(fd4.toString());
		Collections.sort(shifts,FiscalDate.SHIFT_STRING_COMPARATOR);
		assertEquals("2016-02-01-A",shifts.get(0).toString());
		assertEquals("2016-02-01-B",shifts.get(1).toString());
		assertEquals("2016-01-01-A",shifts.get(2).toString());
		assertEquals("2016-01-01-B",shifts.get(3).toString());
		
		//add from 4 to 1
		shifts.clear();
		shifts.add(fd4.toString());
		shifts.add(fd3.toString());
		shifts.add(fd2.toString());
		shifts.add(fd1.toString());
		Collections.sort(shifts,FiscalDate.SHIFT_STRING_COMPARATOR);
		assertEquals("2016-02-01-A",shifts.get(0).toString());
		assertEquals("2016-02-01-B",shifts.get(1).toString());
		assertEquals("2016-01-01-A",shifts.get(2).toString());
		assertEquals("2016-01-01-B",shifts.get(3).toString());
		
		//dates with the same shifts should return equals
		fd1 = new FiscalDate(FiscalDate.SHIFT, "2016-01-01", "A");
		fd2 = new FiscalDate(FiscalDate.SHIFT, "2016-01-01", "A");
		fd3 = new FiscalDate(FiscalDate.SHIFT, "2016-02-02", "A");
		shifts.clear();
		shifts.add(fd1.toString());
		shifts.add(fd2.toString());
		shifts.add(fd3.toString());
		Collections.sort(shifts,FiscalDate.SHIFT_STRING_COMPARATOR);
		assertEquals("2016-02-02-A",shifts.get(0).toString());
		assertEquals("2016-01-01-A",shifts.get(1).toString());
		assertEquals("2016-01-01-A",shifts.get(2).toString());
		}

	}
