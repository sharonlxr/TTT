package com.freescale.stateStamper.model.logic;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.*;
import java.util.*;

import org.slf4j.Logger;

import oracle.jdbc.driver.OracleDriver;

import com.freescale.serverUtil.ThreadSafeSimpleDateFormat;
import com.freescale.stateStamper.model.entity.EquipmentType;

/**
 * This class creates fiscal dates (shift, day, week and month) form a physical date and allows to translate from one to another
 */
public class FiscalDate implements Cloneable
	{
	public String period, fDate, shift, startTime, endTime;
	private static ThreadSafeSimpleDateFormat formatter = new ThreadSafeSimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private static int[] daysOfWeek = {Calendar.SUNDAY,Calendar.MONDAY,Calendar.TUESDAY,Calendar.WEDNESDAY,Calendar.THURSDAY,Calendar.FRIDAY,Calendar.SATURDAY};
	private static TimeZone timeZone = new SimpleTimeZone(0,"NoDayLightSavings");
	private static String[] shifts;
	private static Map<String,String> workWeeks = new HashMap<String,String>();
	private static String[][] crews;
	private static final long MINUTE_MSECS = 60 * 1000;
	private static final long HOUR_MSECS = 60 * MINUTE_MSECS;
	private static final long DAY_MSECS = 24 * HOUR_MSECS;
	private static final long MONTH_MSECS = 30 * DAY_MSECS;
	public static final String RAW = "Raw";
	public static final String SHIFT = "Shift";
	public static final String DAY = "Day";
	public static final String WEEK = "Week";
	public static final String MONTH = "Month";
	public static final String[] PERIODS = {SHIFT,DAY,WEEK,MONTH};
	private static Connection conn;
	static String dbServer, port, sid,user,password;
	private static String shiftStart;
	private static Map<String,Map<String,String>> weeks = new TreeMap<String,Map<String,String>>();	
	private static Map<String,Map<String,String>> months = new TreeMap<String,Map<String,String>>();
	static boolean debug = false;
	private static String START = "startTime",END = "endTime", FISCAL_DATE = "fiscalDate";
	private static Logger logger;
	
	/**
	 * Initializes all fiscal dates
	 * @param calendarShifts A list of shift definitions. Each shift contains: shift ID, start time, end time (HH24:MI:SS format) and the days of the week when the shift runs. 
	 * This is a string with either a Y (shift runs) or an N (shift does not run) for each day of the week starting with Sunday. Example: 1,08:00:00,20:00:00,YYYYYYY;2,20:00:00,08:00:00,YYYYYYY
	 */
	public static void setShifts(String calendarShifts, String calendarCrews)
		{
		// Use a new TimeZone with no day light savings to avoid problems when adding 1 day to a date.
		formatter.setTimeZone(timeZone);
		shifts = calendarShifts.split(";");
		String firstShift = shifts[0];
		shiftStart = firstShift.split(",")[1];
		if (calendarCrews != null && !calendarCrews.trim().equals(""))
			{
			String[] aux2 = calendarCrews.split(";");
			crews = new String[aux2.length][];
			for (int i=0; i<aux2.length; i++) crews[i] = aux2[i].split(",");
			}
		// Use a new TimeZone with no day light savings to avoid problems when adding 1 day to a date.
		formatter.setTimeZone(timeZone);
		Calendar now=Calendar.getInstance(timeZone);
		try
			{
			initWeeks(now.get(Calendar.YEAR));
			initMonths(now.get(Calendar.YEAR));
			}
		catch (SQLException e)
			{
			e.printStackTrace();
			System.err.println("Error while querying the DB");
			}
		}
	
	
	public static void setLogger(Logger log)
		{
		logger = log;
		}
	
	public static void loadCalendarConnection(Properties properties)
		{
		dbServer = properties.getProperty("fiscalCalendarSORHost"    );
		port     = properties.getProperty("fiscalCalendarSORPort"    );
		sid      = properties.getProperty("fiscalCalendarSORSID"     );
		user     = properties.getProperty("fiscalCalendarSORUser"    );
		password = properties.getProperty("fiscalCalendarSORPassword");
		}
	
	public static void reloadConnection()
		{
		try	{if(conn != null && !conn.isClosed()){conn.close();	}}		catch (SQLException e){	e.printStackTrace();	}
		initConnection();
		}
	
	private static void initConnection()
		{
		try
			{
			if(dbServer == null || dbServer.equals("") || port == null || port.equals("") || 
			   sid == null || sid.equals("") || user == null || user.equals("") || password == null || password.equals("") ){
			throw new IllegalArgumentException("Not enough information to create connection dbServer:" + dbServer + " port: " + port+ " sid: " + sid + " user:" + user + " password null? " + (password ==null)); 
			}
			String connection = "jdbc:oracle:thin:@" + dbServer + ":" + port + ":" + sid;
			if(logger!=null)logger.info("Creating Fiscal Calendar SOR connection " + connection + ":" + user);
			DriverManager.registerDriver(new OracleDriver());
			conn = DriverManager.getConnection(connection, user, password);
			if(logger!=null)logger.info("Fiscal Calendar SOR connection created");
			}
		catch (SQLException e)
			{
			if(logger!=null)logger.error(e.getMessage());
			}
		}
	
	private static void initWeeks(int year) throws SQLException
		{
		try
			{
			initDates(year, weeks,WEEK);
			}
		catch (SQLException e)
			{
			e.printStackTrace();
			}
		String secondWeekId = String.valueOf(year) + "-02";
		Map<String,String> secondWeek = weeks.get(secondWeekId);
		workWeeks.put(String.valueOf(year), secondWeek.get(START));
		}
	
	private static void initMonths(int year)
		{
		try
			{
			initDates(year, months,MONTH);
			}
		catch (SQLException e)
			{
			e.printStackTrace();
			}
		}
	
	private static void initDates(int year,Map<String,Map<String,String>> dates,String period) throws SQLException
		{
		if (conn == null || conn.isClosed())
			{
			initConnection();
			}
		String prefix = null;
		String lastPrevYear = null;
		if (period.equals(WEEK))
			{
			prefix = "WK";
			lastPrevYear = "52";
			}
		else if (period.equals(MONTH))
			{
			prefix = "MTH";
			lastPrevYear = "12";
			}		
		
		String previousYear = String.valueOf(year - 1);
		String nextYear = String.valueOf(year + 1);

		String sql = "SELECT FISCAL_YR_NUM, FISCAL_"+prefix+"_NUM, TO_CHAR(FISC_"+prefix+"_BEG_DTME,'YYYY-MM-DD HH24:MI:SS') startTime, TO_CHAR(FISC_"+prefix+"_END_DTME,'YYYY-MM-DD HH24:MI:SS') endTime "
				+ "FROM EDSMGR.FISCAL_CALENDAR_"+prefix
				+ " WHERE FISCAL_YR_NUM = " + year	
				+ " OR (FISCAL_YR_NUM = "  + previousYear + "  AND FISCAL_"+prefix+"_NUM = "+ lastPrevYear + ") "
				+ " OR (FISCAL_YR_NUM = "  + nextYear     + "  AND FISCAL_"+prefix+"_NUM = 1 ) " + 
				"ORDER BY FISCAL_YR_NUM, FISCAL_"+prefix+"_NUM";
		Statement stmt = null;
		ResultSet rs = null;
		try
			{
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
			Map<String, String> periodEntry;
			while (rs.next())
				{
				periodEntry = new HashMap<String, String>();
				String yearNum = rs.getString("FISCAL_YR_NUM");
				String periodNum = rs.getString("FISCAL_"+prefix+"_NUM");
				String startTime = rs.getString(START);
				String endTime = rs.getString(END);

				String shiftStartTime = startTime.substring(0, 11) + shiftStart;

				Calendar endShift = Calendar.getInstance();
				endShift.setTime(formatter.parse(endTime));
				endShift.add(Calendar.DAY_OF_MONTH, 1);
				String tmpEndTime = formatter.format(endShift.getTime());
				String shiftEndTime = tmpEndTime.substring(0, 11) + shiftStart;

				if (periodNum.length() < 2)
					{
					periodNum = "0" + periodNum;
					}

				String fiscalDate = yearNum + "-" + periodNum;

				periodEntry.put("year", yearNum);
				periodEntry.put(period, periodNum);
				// periodEntry.put("startTime", startTime);
				periodEntry.put(START, shiftStartTime);
				// periodEntry.put("endTime", endTime);
				periodEntry.put(END, shiftEndTime);
				periodEntry.put(FISCAL_DATE, fiscalDate);
				dates.put(fiscalDate, periodEntry);
				}
			}
		catch (SQLException e)
			{
			e.printStackTrace();
			}
		catch (ParseException e)
			{
			e.printStackTrace();
			}
		finally
			{
			rs.close();
			stmt.close();
			}
		}
	
	private static Map<String,String> getFiscalDateInformation(String period,String time)
		{
		Map<String,String> periodInfo = null;
		boolean found = false;
		Map<String,Map<String,String>> dates = null;
		if(period.equals(WEEK))        { dates = weeks; }
		else if(period.equals(MONTH))  { dates = months;}
		
		for(Iterator<String> i = dates.keySet().iterator(); i.hasNext() && !found;)
			{
			Map<String,String> date = dates.get(i.next());
			String startTime = date.get(START);
			String endTime = date.get(END);
			if(startTime.compareTo(time) <= 0 && endTime.compareTo(time)  >= 0)
				{
				periodInfo = date;
				found = true;
				}
			}
		return periodInfo;
		}
	
	/** 
	 * Calculate the difference in seconds between two dates
	 * @param time2 Last date in format yyyy-MM-dd HH:mm:ss
	 * @param time1 Firstt date in format yyyy-MM-dd HH:mm:ss
	 * @return The tiem difference is seconds
	 */
	public static long diff(String time2, String time1)
		{	
		if (time2.regionMatches(false,0,time1,0,10))
			{
			int h2,m2,s2,h1,m1,s1;
			h2=Integer.parseInt(time2.substring(11,13));
			m2=Integer.parseInt(time2.substring(14,16));
			s2=Integer.parseInt(time2.substring(17,19));

			h1=Integer.parseInt(time1.substring(11,13));
			m1=Integer.parseInt(time1.substring(14,16));
			s1=Integer.parseInt(time1.substring(17,19));
			
			return ((h2-h1)*3600+(m2-m1)*60+(s2-s1));
			}
		else
			{
      		return ((formatter.parse(time2,new ParsePosition(0)).getTime() - formatter.parse(time1,new ParsePosition(0)).getTime())/1000);		
			}
    	}

	/**
	 * @param utcTime a Time in the format as YYYY-MM-DDT24H:MM:SSZ
	 * @return the local time in the format YYYY-MM-DDT24H:MM:SSZ
	 * */
	public static String getLocalTimeFromUTC(String utcTime) throws Exception
		{   
	    String utcStr = utcTime.replace("T"," ").replace("Z","");
	    Date utcDate = formatter.parse(utcStr);
	    Calendar local = Calendar.getInstance();
	    int diff = local.get(Calendar.ZONE_OFFSET) + local.get(Calendar.DST_OFFSET);
	    utcDate.setTime(utcDate.getTime()+diff);
	    return formatter.format(utcDate);
	    }
	public static String getTimeElapsedinDays(String dateStr)
		{
		SimpleDateFormat simpledateformat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date date = null;
		try{
		date = simpledateformat.parse(dateStr);
		}
		catch (Throwable exception){
			exception.printStackTrace();	
			return "0";
		}
		long oldtime = date.getTime();
		long currenttime = System.currentTimeMillis();
		long difference = currenttime - oldtime;
		double days = difference / DAY_MSECS*10.0;
		days = Math.floor(days);
		days = days / 10.0;
		return String.valueOf(days);		
		}
	
	public static String getElapsedTime(String from,String to)throws Exception
		{	
		long difference = getTimeDifference(from, to); 
		return getElapsedTime(difference);
		}
	
	private static long getTimeDifference(String from,String to) throws Exception
		{
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		long fromTime = simpleDateFormat.parse(from).getTime();
		long toTime = simpleDateFormat.parse(to).getTime();
		long difference = toTime - fromTime;
		return difference;		
		}
	
	public static String getElapsedTime(long duration)throws Exception
	{
		long difference = duration;
		String elapsed = null;
		long month = difference / MONTH_MSECS;
		difference = difference - (MONTH_MSECS * month);
		long days = difference / DAY_MSECS;
		difference = difference - (DAY_MSECS * days);
		long hours = difference / HOUR_MSECS;
		difference = difference - (HOUR_MSECS * hours);
		long minutes = difference / MINUTE_MSECS;
		if(month >0){elapsed = month + "m "+ days+"d";}
		else if(days > 0){elapsed = days +"d " +hours +"h";}
		else {elapsed= hours +"h " + minutes +"m";}
		return elapsed;
	}
	
	public static String getElapsedTimeinDays(long duration)throws Exception
	{
		String elapsed = "0";
		long days = duration / DAY_MSECS;				
		if(days > 0){elapsed = String.valueOf(days);}	
		return elapsed;
	}
	
	public static String getElapsedTimeinHours(long duration)throws Exception
	{
		String elapsed = "0";
		long hours = duration / HOUR_MSECS;				
		if(hours > 0){elapsed = String.valueOf(hours);}	
		return elapsed;
	}
	
	public static String getTimeBasedThresholdValueWithElapse(EquipmentType eType, String name, String value)
		{
		if (eType.isTimeRelatedAttribute(name))
			{
			try
				{
				return FiscalDate.getElapsedTime(Long.parseLong(value));

				}
			catch (Exception e)
				{
				return "";
				}
			}
		else
			{
			return value;
			}
		}
		
	public static String getElapsedTime(String from)throws Exception
		{
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String to =  simpleDateFormat.format(new Date()) ;
		return getElapsedTime(from, to);
		}

	/**
	 * Return crew for given shift if crew information was specified
	 * @param shift A fiscal date object representing a shift
	 * @return The crew for the shift or empty string if no crew information was specified
	 */
	public static String getCrew(FiscalDate shift)
		{
		if (crews == null) return "";  // Return empty string if no crew information was specified
		int index = -1;
		// Get crew info for this shift
		for (int i=0; i<crews.length; i++) if (crews[i][1].equals(shift.shift)) index = i;  
		if (index >= 0) // If crew info found
			{
			int days = (int)(diff(shift.fDate+" 00:00:00",crews[index][0]+" 00:00:00")/86400);
			int period = crews[index].length-2; // crew(index] is day,shift,crews: 2010-01-01,1,A,A,A,A,C,C,C,C,B,B,B,B   
			return crews[index][2+days%period]; 
			}
		else return "";
		}
	
	/**
	 * Return number of shifts per day
	 * @return Number of shifts per day
	 */
	public static int getNumberShifts() {return shifts.length;}
	
	/**
	 * Creates a new fiscal date
	 * @param period The period for this fiscal date: Shift, Day, Week or Month
	 * @param fDate The fiscal time representation of this date: 2008-09-01 for Shift and Day, 2008-37 for Week and 2008-09 for Month 
	 * @param shift The shift if it is a shift fiscal date
	 * @throws Exception
	 */
	public FiscalDate(String period, String fDate, String shift) throws Exception
		{
		// Use a new TimeZone with no day light savings to avoid problems when adding 1 day to a date.
		formatter.setTimeZone(timeZone);

		this.period=period;
		this.fDate=fDate;
		this.shift=shift;

		String fiscalYear = fDate.substring(0,4);
		if (shifts == null ) throw new Exception("There is no shift information");
		String date = workWeeks.get(fiscalYear);
		if (date==null)
			{
			initWeeks(Integer.parseInt(fiscalYear));
			date  = workWeeks.get(fiscalYear);
			}
		Calendar WW2=Calendar.getInstance(timeZone);
		WW2.setTime(formatter.parse(date));

		if (period.equals(SHIFT))
			{
			String id=null, shiftStartTime=null, shiftEndTime=null;
			for (int i=0;i<shifts.length;i++)
				{
				String[] shiftItems=shifts[i].split(",");
				id=shiftItems[0].trim();
				shiftStartTime=shiftItems[1].trim();
				shiftEndTime=shiftItems[2].trim();				
				if (id.equals(shift)) {break;}
				}
			if (id==null || shiftStartTime==null || shiftEndTime==null) {throw new Exception("Shift "+fDate+" "+shift+ "is not found in the fiscal calendar");}

			FiscalDate day=new FiscalDate(DAY,fDate,"*");
			if (day.startTime.substring(11).compareTo(shiftStartTime)<=0) {startTime=day.startTime.substring(0,10)+" "+shiftStartTime;}
			else {startTime=day.endTime.substring(0,10)+" "+shiftStartTime;}
			if (shiftEndTime.compareTo(day.endTime.substring(11))<=0) {endTime=day.endTime.substring(0,10)+" "+shiftEndTime;}
			else {endTime=day.startTime.substring(0,10)+" "+shiftEndTime;}
			}
		if (period.equals(DAY))
			{
			Calendar now=Calendar.getInstance(timeZone);
			now.setTime(formatter.parse(fDate+" 11:59:59",new ParsePosition(0)));
			// Divide by the number of milliseconds in a day. Then round it to the number of days (-1.0 -0.0 -> -1, 0.0 1.0 -> 0, 1.0 2.0 -> 1)
			int diff=(int)Math.floor((now.getTime().getTime() - WW2.getTime().getTime())/(DAY_MSECS*1.0));
			
			WW2.add(Calendar.DATE,diff);
			startTime=formatter.format(WW2.getTime());
	
			WW2.add(Calendar.DATE,1);
			endTime=formatter.format(WW2.getTime());
			}
		if (period.equals(WEEK))
			{
			Map<String,String> week = weeks.get(fDate);
			startTime = week.get(START);
			endTime = week.get(END);
			fDate = week.get(FISCAL_DATE);
			}
		if (period.equals(MONTH))
			{
			Map<String,String> month = months.get(fDate);
			if(month == null)
				{
				initMonths(Integer.parseInt(fiscalYear));
				month = months.get(fDate);
				}
			startTime = month.get(START);
			endTime = month.get(END);
			fDate = month.get(FISCAL_DATE);
			}
		}

	/**
	 * Creates a new fiscal date
	 * @param period The period for this fiscal date: Shift, Day, Week or Month
	 * @param time A time included in this fiscal date in format yyyy-MM-dd HH:mm:ss
	 * @throws Exception
	 */
	public FiscalDate(String period, String time) throws Exception
		{
		// Use a new TimeZone with no day light savings to avoid problems when adding 1 day to a date.
		formatter.setTimeZone(timeZone);
		this.period=period;
		Calendar now=Calendar.getInstance(timeZone);
		now.setTime(formatter.parse(time));

		int fiscalYear=Integer.parseInt(time.substring(0,4));

		if (time.compareTo((new FiscalDate(DAY,fiscalYear+"-01-01","*")).startTime)<0) {fiscalYear--;}
		if ((new FiscalDate(DAY,fiscalYear+"-12-31","*")).endTime.compareTo(time)<=0) {fiscalYear++;}

		if (shifts == null) throw new Exception("There is no shift information");
		String date = workWeeks.get(""+fiscalYear);
		if ( date == null )
			{
			initWeeks(fiscalYear);
			date  = workWeeks.get(""+fiscalYear);
			}
		Calendar WW2=Calendar.getInstance(timeZone);
		WW2.setTime(formatter.parse(date));

		if (period.equals(SHIFT))
			{
			// Get the shift by looking at the day of the week and the beginning and start times
			// It may happen that the shift starts and ends on different days
			Calendar previousDay=(Calendar)now.clone(); previousDay.add(Calendar.DATE,-1);
			Calendar nextDay=(Calendar)now.clone(); nextDay.add(Calendar.DATE,1);

			String day1=time, day2=time;
			String hourMinSec=time.substring(11);
			String id=null, shiftStartTime=null, shiftEndTime=null;

			for (int i=0;i<shifts.length;i++)
				{
				String[] shift=shifts[i].split(",");
				id=shift[0].trim();
				shiftStartTime=shift[1].trim();
				shiftEndTime=shift[2].trim();
				String days=shift[3].trim();
				if (shiftStartTime.compareTo(shiftEndTime)<0)
					{
					if (shiftStartTime.compareTo(hourMinSec)<=0 && hourMinSec.compareTo(shiftEndTime)<0 && sameWeekDay(now,days)) {break;}
					}
				else
					{
					if (shiftStartTime.compareTo(hourMinSec)<=0 && sameWeekDay(now,days))
						{
						day2=formatter.format(nextDay.getTime());
						break;
						}
					if (hourMinSec.compareTo(shiftEndTime)<0 && sameWeekDay(previousDay,days))
						{
						day1=formatter.format(previousDay.getTime());
						break;
						}
					}
				}
			if (id==null || shiftStartTime==null || shiftEndTime==null) {throw new Exception("Timestamp "+time+" is not contained in any shift");}

			startTime=day1.substring(0,10)+" "+shiftStartTime;
			endTime=day2.substring(0,10)+" "+shiftEndTime;

			fDate=(new FiscalDate(DAY,time)).fDate;
			shift=id;
			}
		if (period.equals(DAY))
			{
			// Divide by the number of milliseconds in a day. Then round it to the number of days (-1.0 -0.0 -> -1, 0.0 1.0 -> 0, 1.0 2.0 -> 1)
			int diff=(int)Math.floor((now.getTime().getTime() - WW2.getTime().getTime())/(DAY_MSECS*1.0));
			
			WW2.add(Calendar.DATE,diff);
			startTime=formatter.format(WW2.getTime());
	
			WW2.add(Calendar.DATE,1);
			endTime=formatter.format(WW2.getTime());

			if (startTime.substring(11).compareTo("12:00:00")<0) {fDate=startTime.substring(0,10);}
			else {fDate=endTime.substring(0,10);}
			}
		if (period.equals(WEEK))
			{
				Map<String,String> week = getFiscalDateInformation(WEEK,time);
				startTime = week.get(START);
				endTime = week.get(END);
				fDate = week.get(FISCAL_DATE);
			}
		if (period.equals(MONTH))
			{
			Map<String,String> month = getFiscalDateInformation(MONTH,time);
			if(month == null)
				{
				initMonths(fiscalYear);
				month = getFiscalDateInformation(MONTH,time);
				}
			startTime = month.get(START);
			endTime = month.get(END);
			fDate = month.get(FISCAL_DATE);
			}
		}

	/**
	 * Returns a new fiscal date by increasing this fiscal date by the number of days
	 * @param delta The number of days to increase
	 * @return A new fiscal date
	 * @throws Exception
	 */
	public FiscalDate add(int delta) throws Exception
		{
		if (period.equals(SHIFT) || period.equals(DAY))
			{
			Calendar now=Calendar.getInstance(timeZone);
			now.setTime(formatter.parse(startTime,new ParsePosition(0)));
			now.add(Calendar.DATE,delta);

			FiscalDate aux=new FiscalDate(period,formatter.format(now.getTime()));
			fDate=aux.fDate;
			startTime=aux.startTime;
			endTime=aux.endTime;
			}
		if (period.equals(WEEK))
			{
			int total=Integer.parseInt(fDate.substring(0,4))*52+Integer.parseInt(fDate.substring(5))-1+delta;
			int fiscalYear=total/52;
			int fiscalWeek=total%52+1;
		
			if (fiscalWeek<10) {fDate=fiscalYear+"-0"+fiscalWeek;}
			else {fDate=fiscalYear+"-"+fiscalWeek;}
			FiscalDate aux=new FiscalDate(WEEK,fDate,"*");
			startTime=aux.startTime;
			endTime=aux.endTime;
			}
		if (period.equals(MONTH))
			{
			int total=Integer.parseInt(fDate.substring(0,4))*12+Integer.parseInt(fDate.substring(5))-1+delta;
			int fiscalYear=total/12;
			int fiscalMonth=total%12+1;

			if (fiscalMonth<10) {fDate=fiscalYear+"-0"+fiscalMonth;}
			else {fDate=fiscalYear+"-"+fiscalMonth;}

			FiscalDate aux=new FiscalDate(MONTH,fDate,"*");
			startTime=aux.startTime;
			endTime=aux.endTime;
			}
		return this;
		}
	
	/** Clones this object */
	public Object clone() throws CloneNotSupportedException {return super.clone();}

	/**
	 * Return true if the calendar object represents a day when the shift for this fiscal date runs
	 * @param calendar A calendar object
	 * @param days A string 0f 7 chars indicating if this shift fiscal date runs on that day of teh week. i.e. YYYYYYY
	 * @return
	 */
	private boolean sameWeekDay(Calendar calendar, String days)
		{
		int day=calendar.get(Calendar.DAY_OF_WEEK);
		for (int i=0; i<7; i++)	if (daysOfWeek[i]==day) return days.substring(i,i+1).equals("Y");
		return false;
		}
	
	public static String trim(String input, int length)
		{
		if (input.length() > length)
			{
			String subString = input.substring(0, length - 1);
			subString = subString + "...";
			return subString;
			}
		else
			{
			return input;
			}
		}
	/**
	 * @return the passed string with double quotes " and single quotes ' replaced by the html entity &quot; 
	 * */
	public static String escape(String str)
		{
		if(str.contains("\"") ){str = str.replace("\"","&quot;");}
		if(str.contains("\'") ){str = str.replace("\'","&quot;");}
		return str;
		}
	

	/**
	 * @param day a Time in the format as YYYY-MM-DDT24H:MM:SSZ
	 * @param x number of Hours.
	 * @return a Time in the format as YYYY-MM-DDT24H:MM:SSZ incremented by number of minutes. 
	 * */
	
	public static String addDatebyMinutes(String day, int x) {
		Date date = null;
		try {
			date = formatter.parse(day);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		if (date == null)
			return "";
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		cal.add(Calendar.MINUTE, x);
		date = cal.getTime();
		cal = null;
		return formatter.format(date);
	}
	
	public String toString()
		{
		if(FiscalDate.SHIFT.equals(period))
			{
			return fDate + "-"+ shift;
			}
		else
			return fDate;
		}
	
    public static final Comparator<String> SHIFT_STRING_COMPARATOR    = new ShiftStringComparator();
	
	private static class ShiftStringComparator implements Comparator<String>
		{
		private static final String SEPARATOR = "-";
		public int compare(String s1, String s2)
			{
			String f1 = getFiscalDate(s1);
			String f2 = getFiscalDate(s2);
			int comp = f1.compareTo(f2);
			//if the date (YYYY-MM-DD) are the same then compare the shifts ID  
			if(comp == 0)
				{
				try
					{
					//the order is determined by the order of the shifts id in the shift definition 
					Integer idxId1 = getShiftIdIndex(getShiftId(s1));
					Integer idxId2 = getShiftIdIndex(getShiftId(s2));
					comp = idxId1.compareTo(idxId2);
					}
				catch (Exception e)
					{
					e.printStackTrace();
					}
				}
			comp =  comp * -1;
			return comp;
			}
		
		private String getFiscalDate(String shiftStr)
			{
			return shiftStr.substring(0, shiftStr.lastIndexOf(SEPARATOR));
			}
		
		private String getShiftId(String shiftStr)
			{
			return shiftStr.substring(shiftStr.lastIndexOf(SEPARATOR)+1);
			}
		
		private Integer getShiftIdIndex(String shiftId)
			{
			Integer index = null;
			try
				{
				for (int i = 0; i <= shifts.length; i++)
					{
					String[] items = shifts[i].split(",");
					String id = items[0];
					if (id.equals(shiftId))
						{
						index = Integer.valueOf(i);
						break;
						}
					}
				}
			catch (Exception e)
				{
				index = Integer.valueOf(-1);
				}
			return index;
			}

		}
	
	}