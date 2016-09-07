package com.freescale.stateStamper.model.logic;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.text.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.freescale.serverUtil.Persistence;
import com.freescale.stateStamper.model.entity.EquipmentAttribute;
import com.freescale.stateStamper.model.entity.Report;
import com.freescale.serverUtil.ThreadSafeSimpleDateFormat;
import com.freescale.stateStamper.model.entity.Status;
import com.freescale.serverUtil.PersistenceBrokerIterator;

/**
 * This class writes events to the raw data files and summarizes the events in the raw files to the shift files, shift files 
 * into day files, day into week and week into month. Old files are deleted.
 */
public class Summary
	{
	private static final ThreadSafeSimpleDateFormat formatter = new ThreadSafeSimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private Logger logger;
	private String[] contexts, summings;
	private int maxRaw,maxSummary;
	private FiscalDate next, hour, shift, day, week, month;
	private Map<String,List<String>> summaryDates;
	private GenerateReport reports;
	private String attrib[];
	public static final int MIN_MAX_RAW = 30;
	public static final int MIN_MAX_SUMMARY = 1;
	private ExecutorService executor;
	private boolean schedulerMinsInterval = false;

	/**
	 * Summary constructor. Initializes the summary and get the last dates that were summarized.
	 * @param reports GenerateReport object to generate the scheduled reports
	 * @param contexts String array of context variables
	 * @param summings String array of summing variables
	 * @param maxRaw Maximum number of days to keep for raw data
	 * @param maxSummary Maximum number of months to keep summarized data
	 */
	public Summary(GenerateReport reports, String[] contexts, String[] summings,int maxRaw,int maxSummary)
		{
		try
			{
			this.reports = reports;
			this.contexts = contexts;
			this.summings = summings;
			this.maxRaw = maxRaw;
			this.maxSummary = maxSummary;
			logger = LoggerFactory.getLogger("stateStamper");
			// Set current hour
			String aux = formatter.format(new Date());
			hour = new FiscalDate(FiscalDate.RAW,aux);
			hour.endTime = aux.substring(0,14)+"00:00";
			summaryDates = new ConcurrentHashMap<String, List<String>>();
			// Get the last dates when the summary was done for shift, day, week and month, and finally, start the summary
			initFiscalDates();
			setSummaryDates();
			attrib = new String[contexts.length+summings.length];
			for(int i=0;i< contexts.length;i++){attrib[i]=contexts[i];}
			for(int i=0;i< summings.length;i++){attrib[contexts.length+i]=summings[i];}
			executor = Executors.newSingleThreadExecutor();
			}
		catch (Throwable t) {logger.error(t.getMessage(),t);}	
		}
	
	public void setMaxRaw(int maxraw)
		{
		maxRaw = maxraw;
		}
	
	public void setMaxSummary(int maxsummary)
		{
		maxSummary = maxsummary;
		}

	public void setSchedulerMinsInterval(boolean schedulerMinsInterval) 
		{
		this.schedulerMinsInterval = schedulerMinsInterval;
		}

	/**
	 * Returns list with summary schedule: String array  with period, fiscal date, start and end time 
	 * @return List with the data
	 */
	public List getSummarySchedule()
		{
		List result = new ArrayList();
		if (next!=null && shift!=null && day!=null && week!=null && month!=null)
			{
			result.add(new String[]{FiscalDate.SHIFT,shift.toString(),shift.startTime,shift.endTime});
			result.add(new String[]{FiscalDate.DAY,day.toString(),day.startTime,day.endTime});
			result.add(new String[]{FiscalDate.WEEK,week.toString(),week.startTime,week.endTime});
			result.add(new String[]{FiscalDate.MONTH,month.toString(),month.startTime,month.endTime});
			}
		return result;
		}
	
	/**
	 * Returns list with summary files size. Each element is a vector for Raw. Shift, Day, Week and Month files.
	 * Each element in these vectors is a String array with period and file size.
	 * @return List with the summary files size.
	 */
	public List getSummaryFiles()
		{
		Map<String, List<String>> availableDates = summaryDates;
		List<String> result = new ArrayList<String>();
		result.addAll(availableDates.get(FiscalDate.RAW));
		result.addAll(availableDates.get(FiscalDate.SHIFT));
		result.addAll(availableDates.get(FiscalDate.DAY));
		result.addAll(availableDates.get(FiscalDate.WEEK));
		result.addAll(availableDates.get(FiscalDate.MONTH));
		return result;
		}
	
	/**
	 * Return the current shift
	 * @return A fiscal date representing the current shift
	 */
	public FiscalDate getShift() {return shift;}
	
	/**
	 * Run the summary
	 */
	public void summarize(Date date,Boolean isProduction)
		{
		boolean endOfShift = next.endTime.compareTo(formatter.format(date)) < 0;
		boolean startSummary;
		if (isProduction) {
			startSummary = endOfShift;
		}
		else {
			String tmpdate = FiscalDate.addDatebyMinutes(next.endTime, 15);
			startSummary = tmpdate.compareTo(formatter.format(date)) < 0;
			startSummary = startSummary&&endOfShift;
		}
		try
			{
			String dateString = formatter.format(date);
			
			FiscalDate aux = new FiscalDate(FiscalDate.RAW, dateString);
			Date dateStart = formatter.parse(dateString);
			Date dateEnd = formatter.parse(hour.endTime);
			aux.endTime = dateString.substring(0, 14) + "00:00";

			if (aux.endTime.compareTo(hour.endTime.substring(0, 14) + "00:00") > 0) {
				hour = aux;
				reports.generate(hour, endOfShift);
			} 
				if(schedulerMinsInterval){
					if (dateStart.getTime() - dateEnd.getTime() >= 30 * 60 * 1000
						|| dateStart.getTime() - dateEnd.getTime() >= 15 * 60 * 1000) {
						hour.endTime = toString(dateStart);
						reports.generate(hour, endOfShift);
					}
				}
			}
		catch (Throwable t) {logger.error(t.getMessage(),t);}
		try
			{
			if (startSummary)
				{
				Summarizer summarizer = new Summarizer();
				executor.execute(summarizer);
				}
			}
		catch (Throwable t) {logger.error(t.getMessage(),t);}
		}

	public String toString(Date date) {
		SimpleDateFormat dateFormat = new SimpleDateFormat(
				"yyyy-MM-dd HH:mm:ss");
		String timeStamp = dateFormat.format(new Date());

		return timeStamp;
	}
	
	private void summarize()
		{
		boolean endOfShift = next.endTime.compareTo(formatter.format(new Date())) < 0;
		if(!endOfShift){logger.debug("summarize !endOfShift " + next.endTime);return;}
		try
			{
			long start  = System.currentTimeMillis();
			long summaryRecords = summaryData(next.startTime,next.endTime);
			long end = System.currentTimeMillis();
			logger.info((end-start) +" ms summarizing " + summaryRecords + " records for Shift: "+ next + " ( "+ next.startTime + " - " + next.endTime + " ) Day: "+ day + " week: " + week + " month: " + month);
			if (shift.endTime.equals(next.endTime)){reports.generate(shift, endOfShift);}
			if (day.endTime.equals(next.endTime)){reports.generate(day, endOfShift);}
			if (week.endTime.equals(next.endTime)){reports.generate(week, endOfShift);}
			if (month.endTime.equals(next.endTime)){reports.generate(month, endOfShift);}
			deleteOldRecords();
			deleteOldReports();
			setSummaryDates();
			}
		catch (Throwable t) {logger.error(t.getMessage(),t);}
		try	{setNextFiscalDates();}
		catch (Throwable t) {logger.error("Error in setNextFiscalDates ",t); }
		}
	
	/**
	 * Returns a vector of vectors with the dates and report names for a given period
	 * @param period The period
	 * @return A vector of vectors: [date1,[report1,report2,...],date2,[report1,report2,...],...
	 */
	public Vector getReports(String period)
		{
		Vector results = new Vector();
		File[] files = new File(reports.getReportsDirectory(),period).listFiles();
		if (files == null) return results;
		Arrays.sort(files);
		for (int i=files.length-1; i>=0; i--)
			{
			Vector aux = new Vector();
			aux.addElement(files[i].getName());
			aux.addAll(Arrays.asList(files[i].list()));
			results.addElement(aux);
			}
		return results;
		}
	
	/**
	 * set a map with the fiscal dates for each period according to the current date and the maxSummary configuration
	 * @return The map : Shift=[2008-01-01-A,2008-01-01-B,...],Day=[2008-01-01,2008-01-02,...],Week=[2008-52,...],Month=[2008-01,...]
	 */
	private void setSummaryDates()
		{
		long start = System.currentTimeMillis();
		try
			{
			FiscalDate summaryLimit = new FiscalDate(FiscalDate.MONTH,formatter.format(new Date()));
			summaryLimit  = summaryLimit.add(-maxSummary); 
			List shifts = getSummaryDatesByPeriod(summaryLimit, FiscalDate.SHIFT);
			summaryDates.put(FiscalDate.RAW,shifts);
			summaryDates.put(FiscalDate.SHIFT,shifts);
			summaryDates.put(FiscalDate.DAY,getSummaryDatesByPeriod(summaryLimit, FiscalDate.DAY));
			summaryDates.put(FiscalDate.WEEK,getSummaryDatesByPeriod(summaryLimit, FiscalDate.WEEK));
			summaryDates.put(FiscalDate.MONTH,getSummaryDatesByPeriod(summaryLimit, FiscalDate.MONTH));
			reports.setDates(summaryDates);
			}
		catch (Exception e)
			{
			logger.error(e.getMessage(),e);
			}
		long end = System.currentTimeMillis();
		logger.info("setSummaryDates " + (end-start));
		}

	/**
	 * @param limit
	 * @param period the period of the fiscal dates to return, could be FiscalDate.SHIFT,DAY,WEEK or MONTH 
	 * @return a list with all the fiscal dates from current date to the to the limit specified for a given period   
	 * Shift=[2008-01-01-A,2008-01-01-B,...],Day=[2008-01-01,2008-01-02,...],Week=[2008-52,...],Month=[2008-01,...]
	 */
	private List getSummaryDatesByPeriod(FiscalDate limit,String period)throws Exception
		{
		long start = System.currentTimeMillis();
		List<String> dates = new ArrayList<String>();
		Date today = new Date();
		FiscalDate dateReport =  new FiscalDate(period,formatter.format(today));
		FiscalDate currentShift = new FiscalDate(FiscalDate.SHIFT,formatter.format(today));
		boolean isShift = FiscalDate.SHIFT.equals(period);
		boolean includeCurrentPeriod = FiscalDate.diff(currentShift.startTime,dateReport.startTime) > 0; 
		if(includeCurrentPeriod)
			{
			dates.add(dateReport.toString());
			}
		while (FiscalDate.diff(dateReport.startTime, limit.startTime) > 0)
			{
			if (isShift)
				{
				dateReport = new FiscalDate(FiscalDate.SHIFT,formatter.format(new Date(((Date)formatter.parse(dateReport.startTime)).getTime()-1000L)));
				}
			else
				{
				dateReport = dateReport.add(-1);
				}
			dates.add(dateReport.toString());
			}
		long end = System.currentTimeMillis();
		logger.info("getSummaryDatesByPeriod: period: " + period + " limit: "+limit + " dates: " + dates + " in " + (end-start));
		return dates;
		}

	/**
	 * Sets the initial  fiscal dates to summarize
	 */
	private void initFiscalDates() throws Throwable
		{
		shift = getNextShift();
		if(shift == null)
			{
			long start = System.currentTimeMillis();
			String firstRawRecordTime = getOldestRaw();
			long end = System.currentTimeMillis();
			logger.info("oldestRaw retrieved in " + (end-start));
			if (firstRawRecordTime == null) shift = new FiscalDate(FiscalDate.SHIFT,formatter.format(new Date()));
			else shift = new FiscalDate(FiscalDate.SHIFT,firstRawRecordTime);
			}
		day = new FiscalDate(FiscalDate.DAY,shift.startTime);
		week = new FiscalDate(FiscalDate.WEEK,shift.startTime);
		month = new FiscalDate(FiscalDate.MONTH,shift.startTime);
		getNextFiscalDate();
		}
	
	/**
	 * @return the last shift from the summary records in the DB, null if there are no summaries 
	 * */
	private FiscalDate getNextShift()
		{
		FiscalDate lastShift = null,nextShift = null;
		try
			{
			logger.debug("entering getNextShift ");
			long start = System.currentTimeMillis(); 
			String lastDay = getLastSummaryDay();
			long end = System.currentTimeMillis();
			logger.debug("getLastSummaryDay - " + (end-start));
			if(lastDay == null){ return nextShift;}
			Iterator it = null;
			String[] attribs = {FiscalDate.SHIFT};
			String[] like = {FiscalDate.SHIFT,lastDay+"%"};
			String[] grouping = {FiscalDate.SHIFT};
			List tmpShifts = new ArrayList();
			start = System.currentTimeMillis();
			try
				{
				it = Persistence.iSearch(Status.class, attribs, null, null, null, like, null, grouping, null, null);
				while (it != null && it.hasNext() )
					{
					tmpShifts.add(((Object[]) it.next())[0]);
					}
				}
			catch (Throwable t)
				{
				logger.error(t.getMessage(),t);			
				}
			finally
				{
				if (it != null) ((PersistenceBrokerIterator) it).destroy();
				}
			end = System.currentTimeMillis();
			logger.debug("shifts retrieved in " +(end-start));
			start = System.currentTimeMillis();
			Collections.sort(tmpShifts,FiscalDate.SHIFT_STRING_COMPARATOR);
			end = System.currentTimeMillis();
			logger.debug("shifts ordered in " +(end-start));
			String tmp = (String)tmpShifts.get(0);
			String fDate = tmp.substring(0,tmp.lastIndexOf("-"));
			String shiftId = tmp.substring(tmp.lastIndexOf("-")+1);
			lastShift = new FiscalDate(FiscalDate.SHIFT,fDate,shiftId);
			nextShift = new FiscalDate(FiscalDate.SHIFT,lastShift.endTime);
			}
		catch (Throwable t)
			{
			logger.error(t.getMessage(),t);
			}
		return nextShift;
		}
	
	/**
	 * Get the  oldest date of the raw records 
	 * @return The oldest date. Example: 2008-09-01 00:00:00  
	 * @throws Throwable
	 */
	private String getOldestRaw() throws Throwable
		{
		String[] attribs = { "MIN(TIME)" };
		String[] equal = {"PERIOD","R"};
		return getLimitDate(attribs, equal);
		}
	
	/**
	 * Get the last day of the summary records
	 * */
	private String getLastSummaryDay()throws Throwable 
		{
		String[] attribs = { "MAX(SUBSTR(SHIFT,1,10))" };
		return getLimitDate(attribs, null);
		}

	private String getLimitDate(String[] attribs, String[] equal) throws Throwable
		{
		long start,end;
		start = System.currentTimeMillis();
		String sdate = null;
		Iterator it = null;
		try
			{
			it = Persistence.iSearch(Status.class, attribs, equal, null, null, null, null, null, null, null);
			while (it != null && it.hasNext() && sdate == null)
				{
				sdate = (String) ((Object[]) it.next())[0];
				}
			}
		catch (Throwable t)
			{
			logger.error(t.getMessage(),t);			
			}
		finally
			{
			if (it != null) ((PersistenceBrokerIterator) it).destroy();
			}
		end = System.currentTimeMillis();
		logger.debug("getLimitDate time elapsed "+ (end-start));
		return sdate;
		}
	
	/**
	 * Get the next fiscal date to summarize (shift, day, week or month).
	 */
	private void getNextFiscalDate()
		{
		next = shift;
		}

	/**
	 * Increase the fiscal date given by next
	 * @throws Throwable
	 */
	private void setNextFiscalDates() throws Throwable
		{
		logger.debug("Summary.setNextFiscalDates()");
		logger.debug("Old FiscalDates");
		logger.debug("Shift " + shift + " start " + shift.startTime+ " end "+ shift.endTime);
		logger.debug("Day " + day);
		logger.debug("Week " + week);
		logger.debug("Month "+ month);
		
		shift = new FiscalDate(FiscalDate.SHIFT, shift.endTime);
		day = new FiscalDate(FiscalDate.DAY, shift.startTime);
		week = new FiscalDate(FiscalDate.WEEK, shift.startTime);
		month = new FiscalDate(FiscalDate.MONTH, shift.startTime);
		logger.debug("New FiscalDates");
		logger.debug("Shift " + shift + " start " + shift.startTime+ " end "+ shift.endTime);
		logger.debug("Day " + day);
		logger.debug("Week " + week);
		logger.debug("Month "+ month);

		getNextFiscalDate();
		}
	
	/**
	 * Summarizes the raw data for the given start and end times
	 * @param startTime The first fiscal date to summarize. Example: 2008-09-01 00:00:00  
	 * @param endTime The last fiscal date to summarize. Format same as startTime
	 * @throws Throwable
	 */
	
	private long summaryData(String startTime, String endTime)
		{
		long summaryRows = 0;
		try{
		    logger.info("summaryData from " +startTime + " to " + endTime);
		    startTime = formatter.format(new Date(formatter.parse(startTime).getTime()-1000L));//move startTime one second before
		    endTime   = formatter.format(new Date(formatter.parse(endTime).getTime()+1000L));//move startTime one second after 
			String[] equal  = new String[]{"PERIOD","R"};
			String[] greater= new String[]{"TIME",startTime};
			String[] less   = new String[]{"TIME", endTime};
			String[] attributes = new String[0];
			String[] attributesGrouping = new String[0];
			String[] attributesNames = new String[0];
			List<String> attributesList = new ArrayList();
			List<String> attributesNamesList = new ArrayList();
			Status status;
			Object []obj;
			Iterator it=null;
			for(String context:contexts)
				{
				if(context.equalsIgnoreCase("TIME"))continue;
				attributesList.add(context);
				}
			attributesNamesList.addAll(attributesList);
			attributesGrouping = attributesList.toArray(attributesGrouping);
			for(String summing:summings)
				{
				attributesList.add("SUM("+summing+")");
				attributesNamesList.add(summing);
				}
			attributes = attributesList.toArray(attributes);
			attributesNames = attributesNamesList.toArray(attributesNames);
			String values[] = new String[attributes.length];
			Object value;
			
			try
				{
				long start = System.currentTimeMillis();
				it = Persistence.iSearch(Status.class, attributes, equal, greater, less, null, null, attributesGrouping, null, null);
				long end = System.currentTimeMillis();
				logger.info((end-start) +" ms executing the summary query");
				start = System.currentTimeMillis();
				while (it != null && it.hasNext())
					{
					summaryRows++;
					obj = (Object[]) it.next();
					for (int i = 0; obj != null && i < obj.length; i++)
						{
						values[i] = obj[i] != null ? obj[i].toString() : null;
						}
					status = new Status();
					for(int i = 0; i< values.length;i++)
						{
						value = values[i];
						if(value == null){continue;}
						value = value.toString();
						status.set(attributesNames[i], value);
						}
					status.setShift(next.toString());
					status.setDay(day.toString());
					status.setWeek(week.toString());
					status.setMonth(month.toString());
					Persistence.store(status);
					}
				end = System.currentTimeMillis();
				logger.info((end-start)+" ms creating the summary records");
				}
			catch (Throwable t)
				{
				logger.error(t.getMessage(),t);		
				}
			finally
				{
				if (it != null) ((PersistenceBrokerIterator) it).destroy();
				}

		}catch(Throwable t){logger.error(t.getMessage(),t);}
		return summaryRows;
		}

	/**
	 * Delete old records
	 * @throws Throwable
	 */
	private void deleteOldRecords() throws Throwable
		{
		FiscalDate fDate = new FiscalDate(FiscalDate.MONTH,next.startTime);
		fDate.add(-maxSummary);
		if (month.endTime.equals(next.endTime))
			{
			logger.info("Deleting summary records older than " + fDate.fDate);
			deleteOldRecords(fDate.period, fDate);
			}
		if (day.endTime.equals(next.endTime))
			{
			FiscalDate rawDate = ((FiscalDate) next.clone()).add(-maxRaw);
			logger.info("Deleting raw records older than " + rawDate.startTime);
			deleteOldRecords(FiscalDate.RAW, rawDate);
			}
		}
	
	private void deleteOldReports()
		{
		List dates;
		String fDate = null;
		if(shift.endTime.equals(next.endTime)){dates = summaryDates.get(FiscalDate.SHIFT); fDate = (String)dates.get(dates.size()-1); deleteReports(FiscalDate.SHIFT,fDate);}
		if(day.endTime.equals(next.endTime))  {dates = summaryDates.get(FiscalDate.DAY);   fDate = (String)dates.get(dates.size()-1); deleteReports(FiscalDate.DAY  ,fDate);}
		if(week.endTime.equals(next.endTime)) {dates = summaryDates.get(FiscalDate.WEEK);  fDate = (String)dates.get(dates.size()-1); deleteReports(FiscalDate.WEEK ,fDate);}
		if(month.endTime.equals(next.endTime)){dates = summaryDates.get(FiscalDate.MONTH); fDate = (String)dates.get(dates.size()-1); deleteReports(FiscalDate.MONTH,fDate);}
		}
		
	/**
	 * Delete records for the given period that are older than the given fiscal date 
	 * @param period The period: Raw, Shift, Day, Week or Month
	 * @param fDate The fiscal date. Example: 2008-09-01-A for Raw and Shift, 2008-09-01 for Day, 2008-37 for Week and 2008-09 for Month 
	 */
	private void deleteOldRecords(String period, FiscalDate fDate)
	{
	try
		{			
		String p = period.substring(0,1);			
		if ("R".equals(p))
			{
			String[] less = { "TIME", fDate.fDate };
			String[] equal = { "PERIOD", p };
			Persistence.delete(Status.class, equal, less);
			}
		else
			{
			Persistence.delete(Status.class, fDate.period,fDate.fDate);
			}
		}
	catch (Throwable t) {logger.error(t.getMessage(),t);}
	}
	
	/**
	 * Delete reports for the given period that are older than the given fiscal date 
	 * @param period The period: Raw, Shift, Day, Week or Month
	 * @param fDate The fiscal date. Example: 2008-09-01-A for Raw and Shift, 2008-09-01 for Day, 2008-37 for Week and 2008-09 for Month 
	 */
	private void deleteReports(String period, String fDate)
		{
		try
			{
			File[] directories = new File(reports.getReportsDirectory(),period).listFiles();
			if (directories == null) return;
			for (int i=0; i<directories.length; i++)
				{
				if (directories[i].getName().compareTo(fDate) < 0)
					{
					File[] files = directories[i].listFiles();
					for (int j=0; j<files.length; j++) files[j].delete();
					directories[i].delete();
					}
				}
			}
		catch (Throwable t) {logger.error(t.getMessage(),t);}
		}
	
	public Map getDates()
		{
		return summaryDates;
		}

	
	private class Summarizer implements Runnable
		{
		public void run()
			{
			summarize();
			}
		}

	public static void main(String[] args) throws Throwable
		{
		if(args.length != 3)
			{
			System.out.println("usage : Summary <propertiesfile> <baseDirectory> <YYYY-MM-DD 24HH:MM:SS>");
			return;
			}
		String propertiesFile = args[0];
		String baseDirectory = args[1];
		String summaryDate = args[2];
		Properties properties = new Properties();
		properties.load(new FileInputStream(propertiesFile));
		String reportsDirectory = properties.getProperty("reportsDirectory");
		String transferDirectory = properties.getProperty("transferDirectory");
		int maxRaw = Integer.parseInt(properties.getProperty("maxRaw").trim());
		int maxSummary = Integer.parseInt(properties.getProperty("maxSummary").trim());
		String[] contexts;
		String[] summings;
		// Get contexts and summings
		List cont = new ArrayList();
		List sum = new ArrayList();
		cont.addAll(Arrays.asList(Status.DEFAULT_STATUS));
		EquipmentAttribute attr = new EquipmentAttribute();
		attr.typeKey = new Integer(1);
		List<EquipmentAttribute> equipAttributes = Persistence.search(attr);
		// add the defined equipment attributes to att and the appropiate context or summing
		for (EquipmentAttribute attribute : equipAttributes)
			{
			if (attribute.type.equals("Counter"))
				{
				sum.add(attribute.name);
				}
			else
				{
				cont.add(attribute.name);
				}
			}
		contexts = (String[]) cont.toArray(new String[0]);
		summings = (String[]) sum.toArray(new String[0]);
		int writeInterval = Integer.parseInt(properties.getProperty("writeInterval"));
		// Create thread group
		//properties has to have defined the Fiscal Calendar connection
		FiscalDate.loadCalendarConnection(properties);
		FiscalDate.setShifts(properties.getProperty("shifts"),properties.getProperty("crews"));
		List aux = (List) Persistence.search(new Report());
		GenerateReport reports = new GenerateReport(reportsDirectory,transferDirectory, baseDirectory, aux, writeInterval);
		StateStamper.setAreaCode(properties.getProperty("area_code"));
		StateStamper.setPlantCode(properties.getProperty("plant_code"));
		StateStamper.setStage(properties.getProperty("stage"));
		Summary summary = new Summary(reports, contexts,summings, maxRaw,maxSummary);
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date summarydate = formatter.parse(summaryDate);
		summary.summarize(summarydate, false);
		}
	
	}