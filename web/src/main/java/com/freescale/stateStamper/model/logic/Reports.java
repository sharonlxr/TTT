package com.freescale.stateStamper.model.logic;

import com.freescale.serverUtil.EmailServer;
import com.freescale.stateStamper.model.entity.*;
import java.io.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Logger;
import jxl.*;
import jxl.write.*;
import jxl.write.Number;

/**
 * This class generates reports requested by the GUI and publishes them as well
 */

public class Reports
	{
	private static final String[] STATES = {"Unknown","B0B0B0","Productive","A8FFA8","Standby","FFF8A0","Engineering","A8FFFF","Scheduled Down","FFC078","Unscheduled Down","FF9898","Non Scheduled","E0E0E0"};
	private static final String[] PERIODS = {"Shift","Day","Week","Month"};
	private static final String EMAIL_SERVER = "remotesmtp.freescale.net", FROM_USER = "StateStamper", FROM_ADDRESS = "StateStamper@freescale.com", SUBJECT = "Utilization Report";
	private static final String CONTENT = "\n\n   Utilization report attached\n\n   The information contained in this email has been classified Freescale Confidential Proprietary";
	private static final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private static DecimalFormat decimal = new DecimalFormat("##0.00");
	private static Hashtable reportDefinition = new Hashtable();
	private String dataDirectory, reportsDirectory;
	private Vector reportTypes = new Vector();
	private Vector scheduledReports;
	private Hashtable colors = new Hashtable();
	private Report[] reports = new Report[10];
	private int index = 0;
	private Logger logger;
	
	/**
	 * Constructs a Report object that will be used to generate and publish reports
	 * @param dataDirectory The directory where the data files reside
	 * @param reportsDirectory The directory where reports will be published
	 * @param scheduledReports A vector of Report objects that are scheduled to be generated and published 
	 * @throws Throwable
	 */
	public Reports(String dataDirectory, String reportsDirectory, Vector scheduledReports) throws Throwable
		{
		this.dataDirectory = dataDirectory;
		this.reportsDirectory = reportsDirectory;
		this.scheduledReports = scheduledReports;
		logger = Logger.getLogger("StateStamper");
		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(StateStamper.class.getResourceAsStream("reports.txt")));
		String line = bufferedReader.readLine();
		while((line = bufferedReader.readLine()) != null)
			{
			String[] values = line.split("\t");
			String[] columns = new String[values.length-2];
			// First 2 items are report type and grouping, ignore them
			for (int i=2; i<values.length; i++) {columns[i-2] = values[i].trim();}
			reportDefinition.put(values[0]+"\t"+values[1],columns);
			reportTypes.addElement(values[0]+"\t"+values[1]);
			}
		bufferedReader.close();
		for (int i=0; i<STATES.length; i+=2) colors.put(STATES[i],STATES[i+1]); // Initialize colors
		}

	/**
	 * Generates a report 
	 * @param report A report object with the settings for this report
	 * @param outputStream Output stream to write the report to
	 * @param publish Whether to publish the report or not
	 * @return A hashtable with the report data
	 * @throws Throwable
	 */
	public Hashtable generate(Report report, OutputStream outputStream, boolean publish) throws Throwable
		{
		Hashtable columnIndex = new Hashtable(), data = new Hashtable(), exclusion = new Hashtable();
		for (int i=0; report.exclusions!=null && i<report.exclusions.size(); i++) exclusion.put(((Machine)report.exclusions.elementAt(i)).mESName,""); // Populate exclusion hashtable
		boolean all = report.machines == null || report.machines.size() == 0;
		boolean raw = report.period.equals("Raw");
		String startDate = null, endDate = null; // startDate and endDate are only used when period = "Raw"
		if (raw) {startDate = formatter.format(report.startDate); endDate = formatter.format(report.endDate);}
		String[] reportColumns = (String[])reportDefinition.get(report.reportType+"\t"+report.grouping);
		for (int i=0; i<report.dates.length; i++)
			{
			String date = (String)report.dates[i];
			File file = new File(dataDirectory+File.separator+report.period,date+".txt");
			if (file.exists())
				{
				BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
				String record = bufferedReader.readLine();
				String columns[] = record.split("\t");
				for (int k=0; k<columns.length; k++) columnIndex.put(columns[k],new Integer(k));
				while((record = bufferedReader.readLine()) != null)
					{
					String machine = getValue("MID",record,date,columnIndex);
					// Find out if record should be included in report
					boolean include = false;
					if (all || (report.machines.get(machine) != null && exclusion.get(machine) == null))
						{
						if (raw)
							{
							String time = getValue("TIME",record,date,columnIndex);
							if (time.compareTo(startDate) >= 0 && time.compareTo(endDate) <= 0) include = true;
							}
						else include = true;
						}
					if (include)
						{
						if (reportColumns.length == 0) processRecordST(data,report,record,date,columnIndex);
						else processRecord(data,reportColumns,record,date,columnIndex);
						}
					}
				bufferedReader.close();
				}
			}
		if (reportColumns.length == 0) postProcessST(data,report);
		else
			{
			postProcess(data,reportColumns);
			if (report.isHTML.booleanValue()) html(report,data,reportColumns[0],outputStream,publish);
			else excel(report,data,reportColumns[0],outputStream,publish);
			}
		return data;		
		}
	
	/**
	 * Returns a vector of vectors with all the report types and subtypes
	 * @return A vector of vectors: [type1,[subtype1,subtype2,...],type2,[subtype1,subtype2,...],...
	 */
	public 	Vector getReportTypes() {return reportTypes;}
	
	/**
	 * Sets the entire list of scheduled reports
	 * @param vector A vector of Report objects
	 */
	public void setReports(Vector vector) {scheduledReports = vector;}
	
	/**
	 * Store a report settings in the array of reports
	 * @param report The Report object with the settings
	 * @return An Integer with the index in the reports array
	 */
	public Integer storeReport(Report report)
		{
		Integer result = new Integer(index);
		reports[index] = report;
		index++;
		if (index >= reports.length) index = 0;
		return result;
		}
	
	/**
	 * Retrieve a report from the given index
	 * @param index The index for the stored report
	 * @return A Report object
	 */
	public Report getReport(int index) {return reports[index];}

	/**
	 * Returns a hashtable with the fiscal dates for each period
	 * @return The hashtable:  Shift=[2008-01-01-A,2008-01-01-B,...],Day=[2008-01-01,2008-01-02,...],Week=[2008-01,...],Month=[2008-01,...]
	 */
	public Hashtable getDates()
		{
		Hashtable dates = new Hashtable();
		dates.put("Raw",readFiles("Raw"));
		for (int i=0; i<PERIODS.length; i++) dates.put(PERIODS[i],readFiles(PERIODS[i]));
		return dates;
		}

	/**
	 * Generates all the reports scheduled for the given fiscal date
	 * @param date The fiscal date
	 */
	public void generate(FiscalDate date)
		{
		try
			{
			Vector dates = readFiles(date.period);
			for (int i=0; i<scheduledReports.size(); i++)
				{
				try
					{
					Report report = (Report)scheduledReports.elementAt(i);
					if (report.period.equals(date.period))
						{
						setDates(report,dates);
						if (report.dates.length > 0)
							{
							report.machines = StateTransition.getMachines(report.types,report.locations); // Get the lost of machines
							generate(report,null,true);
							}
						}
					}
				catch (Throwable t) {logger.severe(t.getMessage());}
				}
			}
		catch (Throwable t) {logger.severe(t.getMessage());}	
		}

	/**
	 * Get the value for a given variable for a record in the data files
	 * @param variable The variable
	 * @param record A record from the data files
	 * @param date The fiscal date
	 * @param variableIndex A hahstable with the position of each variable in the record
	 * @return
	 */
	private String getValue(String variable, String record, String date, Hashtable variableIndex)
		{
		String function = null;
		if (variable.equals("ALL")) return "All";
		if (variable.equals("DATE")) return date;
		int index1 = variable.indexOf("(");
		if (index1 != -1)
			{
			function = variable.substring(0,index1);
			variable = variable.substring(index1+1,variable.length()-1);
			}
		Integer index = (Integer)variableIndex.get(variable);
		if (index == null) return "";
		int index2 = index.intValue(), index3 = -1, index4;
		for (int i=0; i<index2; i++) {index3 = record.indexOf("\t",index3+1);}
		index4 = record.indexOf("\t",index3+1);
		if (index4 == -1) {index4 = record.length();}
		String value = record.substring(index3+1,index4);
		if (variable.equals("SUBSTATE") && value.equals("")) return "Unknown";
		if (function != null)
			{
			if (function.equals("TYPE")) value = (String)StateTransition.getType(value);
			else if (function.equals("OEE"))
				{
				if (!getValue("SUBSTATE",record,date,variableIndex).equals("Test")) value = "0";
				else value = ""+yieldGain(record,date,variableIndex)*100;
				}
			else if (function.equals("UOEE"))
				{
				if (!getValue("STATE",record,date,variableIndex).equals("Productive")) value = "0";
				else value = ""+testIndexTime(record,date,variableIndex)*100;
				}
			else if (function.equals("DEE"))
				{
				if (!getValue("STATE",record,date,variableIndex).equals("Productive")) value = "0";
				else value = ""+testIndexTime(record,date,variableIndex)*100;
				}
			else if (function.equals("YIELD_LOSS"))
				{
				if (!getValue("STATE",record,date,variableIndex).equals("Productive"))  value = "0";
				else value = ""+yieldLoss(record,date,variableIndex)*100;
				}
			else if (function.equals("FIRST_PASS_YIELD_LOSS"))
				{
				if (!getValue("SUBSTATE",record,date,variableIndex).equals("Test")) value = "0";
				else value = ""+yieldLoss(record,date,variableIndex)*100;
				}
			else if (function.equals("FIRST_PASS_SITE_PERF_LOSS"))
				{			
				if (!getValue("SUBSTATE",record,date,variableIndex).equals("Test")) value = "0";
				else value = ""+sitePerformanceLoss(record,date,variableIndex)*100;
				}
			else if (function.equals("RETEST_GAIN"))
				{
				if (!getValue("SUBSTATE",record,date,variableIndex).equals("Retest")) value = "0";
				else value = ""+yieldGain(record,date,variableIndex)*100;
				}
			else if (function.equals("SITE_PERFORMANCE"))
				{
				value = ""+sitePerformance(record,date,variableIndex)*100;
				}
			}
		return value;
		}
	
	private float testIndexTime(String record, String date, Hashtable variableIndex)
		{
		float testTime = Float.parseFloat(getValue("TEST_TIME",record,date,variableIndex));
		float indexTime = Float.parseFloat(getValue("HND_INDEX_TIME",record,date,variableIndex));
		return (testTime+indexTime);
		}
	
	private float yieldLoss(String record, String date, Hashtable variableIndex)
		{
		float devices = Float.parseFloat(getValue("DEVICES_PROCESSED",record,date,variableIndex));
		if (devices == 0) return 0;
		float good = Float.parseFloat(getValue("CNT_GOOD",record,date,variableIndex));
		float testTime = Float.parseFloat(getValue("TEST_TIME",record,date,variableIndex));
		float indexTime = Float.parseFloat(getValue("HND_INDEX_TIME",record,date,variableIndex));
		return (devices-good)/devices*(testTime+indexTime);
		}
	
	private float sitePerformanceLoss(String record, String date, Hashtable variableIndex)
		{
		float devices = Float.parseFloat(getValue("DEVICES_PROCESSED",record,date,variableIndex));
		if (devices == 0) return 0;
		float sitesAvailable;
		try {sitesAvailable = Float.parseFloat(getValue("SITES_AVAILABLE",record,date,variableIndex)) * Float.parseFloat(getValue("INSERTIONS",record,date,variableIndex));}
		catch (Throwable t) {sitesAvailable = 0;}
		if (sitesAvailable == 0) return 0;
		float good = Float.parseFloat(getValue("CNT_GOOD",record,date,variableIndex));
		float testTime = Float.parseFloat(getValue("TEST_TIME",record,date,variableIndex));
		float indexTime = Float.parseFloat(getValue("HND_INDEX_TIME",record,date,variableIndex));
		return ((sitesAvailable-devices)/sitesAvailable)*(testTime+indexTime)*(good/devices);
		}
	
	private float yieldGain(String record, String date, Hashtable variableIndex)
		{
		float devices = Float.parseFloat(getValue("DEVICES_PROCESSED",record,date,variableIndex));
		if (devices == 0) return 0;
		float good = Float.parseFloat(getValue("CNT_GOOD",record,date,variableIndex));
		float testTime = Float.parseFloat(getValue("TEST_TIME",record,date,variableIndex));
		float indexTime = Float.parseFloat(getValue("HND_INDEX_TIME",record,date,variableIndex));
		return good/devices*(testTime+indexTime);
		}
	
	private float sitePerformance(String record, String date, Hashtable variableIndex)
		{
		float sitesAvailable;
		try {sitesAvailable = Float.parseFloat(getValue("SITES_AVAILABLE",record,date,variableIndex)) * Float.parseFloat(getValue("INSERTIONS",record,date,variableIndex));}
		catch (Throwable t) {sitesAvailable = 0;}
		return sitesAvailable;
		}

	/**
	 * Returns a vector with all the fiscal dates in the data files for the given period
	 * @param period The period
	 * @return The vector
	 */
	private Vector readFiles(String period)
		{
		Vector result = new Vector();
		String[] files = (new File(dataDirectory,period)).list();
		if (files != null)
			{
			for (int i=0; i<files.length; i++) result.addElement(files[i].substring(0,files[i].length()-4));
			}
		if (period.equals("Raw") || period.equals("Shift")) Collections.sort(result,new CompareDates());
		else Collections.sort(result);
		return result;
		}
	
	/**
	 * Set all dates in the report object for this report settings and the available dates 
	 * @param report The report object
	 * @param dates A vector of all available dates
	 */
	private void setDates(Report report, Vector dates)
		{
		// Add dates to report
		int size = Math.min(report.numberPeriods.intValue(),dates.size());
		report.dates = new Object[size];
		for (int i=0; i<size; i++) report.dates[i] = dates.elementAt(i+(dates.size()-size));
		}
	
	/**
	 * Process a record when generating a report
	 * @param data A hashtable with the stored results for the report
	 * @param variables The variables to include in the report
	 * @param record The record
	 * @param date The dfiscal date for the report
	 * @param variableIndex A hashtable with the index of each variable in the record
	 */
	private void processRecord(Hashtable data, String[] variables, String record, String date, Hashtable variableIndex)
		{
		String categories = "";
		Vector values = new Vector();
		for (int i=1; i<variables.length; i++)
			{
			String[] aux = variables[i].split("/");
			for (int j=0; j<aux.length; j++)
				{
				String value = getValue(aux[j],record,date,variableIndex);
				if (isSumming(variables[i])) values.addElement(new Float(value.equals("")?"0":value));
				else categories += value + "\t";
				}
			}
		categories = categories.substring(0,categories.length()-1);
		Vector aux = (Vector)data.get(categories);
		if (aux == null) data.put(categories,values);
		else
			{
			for (int i=0; i<aux.size(); i++) aux.setElementAt(new Float(((Float)aux.elementAt(i)).floatValue()+((Float)values.elementAt(i)).floatValue()),i);
			}
		}
	
	/**
	 * Process a record for the state transition report report
	 * @param data A hashtable with the stored results for the report
	 * @param report A report object with the settings for this report
	 * @param record The record
	 * @param date The dfiscal date for the report
	 * @param variableIndex A hashtable with the index of each variable in the record
	 */
	private void processRecordST(Hashtable data, Report report, String record, String date, Hashtable variableIndex)
		{
		String machine = getValue("MID",record,date,variableIndex);
		String state = getValue("STATE",record,date,variableIndex);
		if (report.reportType.startsWith("Substate"))
			{
			String substate = getValue("SUBSTATE",record,date,variableIndex);
			Hashtable substates = (Hashtable)data.get("Substates");
			if (substates == null) {substates = new Hashtable(); data.put("Substates",substates);}
			substates.put(substate,state);
			state = substate;
			}
		String duration = getValue("DURATION",record,date,variableIndex);
		String time = getValue("TIME",record,date,variableIndex);
		Vector vector = (Vector)data.get(machine);
		if (vector == null) {vector = new Vector();	data.put(machine,vector);}
		addUnknown(vector,time,report.startDate);		
		// Add state, duration and time to the vector. Time will be removed when the next record is processed
		vector.addElement(state);
		vector.addElement(new Long((long)Float.parseFloat(duration)));
		vector.addElement(time);
		}
	
	/**
	 * Adds unknown time nd trims duration if needed
	 * @param vector A vector with a list of states and durations
	 * @param time The last time for the next state
	 * @param startDate The initial date for the report
	 */
	private void addUnknown(Vector vector, String time, Date startDate)
		{
		long prevDuration;
		String prevTime;
		int size = vector.size();
		if (size == 0)
			{
			prevDuration = 0;
			prevTime = formatter.format(startDate);
			}
		else
			{ // Get previous duration and time
			prevDuration = ((Long)vector.elementAt(size-2)).longValue();
			prevTime = (String)vector.elementAt(size-1);
			vector.removeElementAt(size-1); // Remove last time from the vector
			}
		long diff = FiscalDate.diff(time,prevTime);
		// If duration of previous state and time difference do not match, add unknown state for the difference
		// else if duration is greater than the time difference, trim it
		if (diff*1000-prevDuration > 1000)
			{
			vector.addElement("Unknown");
			vector.addElement(new Long(diff*1000-prevDuration));
			}
		else if (prevDuration-diff*1000 > 1000) vector.setElementAt(new Long(diff*1000),size-2);
		}
	
	/**
	 * Performs any needed divisions when generating a report
	 * @param data A hashtable with the stored results for the report
	 * @param variables The variables to include in the report
	 */
	private void postProcess(Hashtable data, String[] variables)
		{
		Vector divide = new Vector();
		int index = 0;
		for (int i=1; i<variables.length; i++)
			{
			if (variables[i].indexOf("/") != -1) {divide.addElement(new Integer(index));}			
			if (isSumming(variables[i])) index++;
			}
		if (divide.size() == 0) {return;}
	    Enumeration enumeration = data.keys();
		while(enumeration.hasMoreElements())
			{
			Vector values = (Vector)data.get(enumeration.nextElement());
			for (int i=0; i<divide.size(); i++)
				{
				index = ((Integer)divide.elementAt(i)).intValue();
				if (((Float)values.elementAt(index+1)).floatValue() == 0) {values.setElementAt(new Float(0),index);}
				else {values.setElementAt(new Float(((Float)values.elementAt(index)).floatValue()/((Float)values.elementAt(index+1)).floatValue()),index);}
				values.removeElementAt(index+1);
				}
			}
		}
	
	/**
	 * Adds any extra Unknown time needed and remove last time element from each machine
	 * @param data A hashtable with the stored results for the report
	 * @param report A report object with the settings for this report
	 */
	private void postProcessST(Hashtable data, Report report)
		{
		String time = formatter.format(report.endDate);
		Enumeration machines = data.keys();
		while (machines.hasMoreElements())
			{
			String machine = (String)machines.nextElement();
			if (!machine.equals("Substates"))
				{
				Vector vector = (Vector)data.get(machine);
				if (vector != null) addUnknown(vector,time,null);
				}
			}
		}
	
	/**
	 * Returns true if the parameter is a summing variable
	 * @param variable
	 * @return
	 */
	private boolean isSumming(String variable)
		{
		return variable.startsWith("SUM(") || variable.startsWith("SITE_PERFORMANCE(") || variable.startsWith("OEE(") || variable.startsWith("UOEE(") || variable.startsWith("FIRST_PASS_SITE_PERF_LOSS(") || variable.startsWith("FIRST_PASS_YIELD_LOSS(") || variable.startsWith("RETEST_GAIN(") || variable.startsWith("YIELD_LOSS(");
		}

	/**
	 * Generates a report in HTML format
	 * @param report The report settings
	 * @param data The report data
	 * @param type The file where save the report (if publish is true)
	 * @param outputStream The file stream where to write the report to (if publish is false)
	 * @param publish Whether to publish the report in a file or write it to the output stream
	 * @throws Throwable
	 */	
	private void html(Report report, Hashtable data, String type, OutputStream outputStream, boolean publish) throws Throwable
		{
		String content = "";
		if (type.equals("bar") || type.equals("column")) content = chartHTML(report,false,data);
		else if (type.equals("stack")) content = chartHTML(report,true,data);
		else if (type.startsWith("performance") || type.equals("utilization")) content = chartHTML(report,false,data);		

		StringBuffer buffer = new StringBuffer();
		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(StateStamper.class.getResourceAsStream("chart.html")));
		String line;
		while((line = bufferedReader.readLine()) != null)
			{
			if (line.indexOf("<!--variables-->") !=  -1) buffer.append(content);
			else buffer.append(line+"\n");
			}
		bufferedReader.close();
		
		if (publish)
			{
			File directory = new File(reportsDirectory,report.period);
			if (!directory.exists()) directory.mkdir();
			directory = new File(directory,(String)report.dates[report.dates.length-1]);
			if (!directory.exists()) directory.mkdir();
			File file = new File(directory,title(report).replaceAll(" ","_")+".html");
			PrintWriter pw = new PrintWriter(new FileWriter(file));
			pw.print(buffer.toString());
			pw.close();
			email(report,file); // Email report file to recipients
			}
		else
			{
			outputStream.write(buffer.toString().getBytes());
			outputStream.flush();
			outputStream.close();				
			}
		}
		
	/**
	 * Format report data (categories and subcategories) in HTML format
	 * @param report The report object
	 * @param data A hashtable with the report data
	 * @param buffer A  string buffer
	 * @throws Throwable
	 */
	private String chartHTML(Report report, boolean stacked, Hashtable data) throws Throwable
		{
		StringBuffer buffer = new StringBuffer();
		buffer.append("var title = '"+title(report)+"';\n");
		Hashtable categories = new Hashtable(), subcategories = new Hashtable(), totals = null;
		if (stacked) totals = getTotals(data);
		Vector vector = new Vector(data.keySet());
		for (int i=0; i<vector.size(); i++)
	    	{
	    	String aux = (String)vector.elementAt(i);
	    	String category = aux, subcategory = report.reportType;
	    	if (stacked)
	    		{
		    	int index = aux.indexOf("\t");
		    	category = aux.substring(0,index);
		    	subcategory = aux.substring(index+1,aux.length());
	    		}
		   	categories.put(category,"");
	    	subcategories.put(subcategory,"");
	    	}
		Vector catVector = new Vector(categories.keySet()), subcatVector = new Vector(subcategories.keySet());
		Collections.sort(catVector);
		Collections.sort(subcatVector);
		
		if (!stacked) buffer.append("colors['"+report.reportType+"'] = [255,0,0];\n");
		buffer.append("var isStacked = true;\nvar isBar = true;\n");		
		buffer.append("var categories = [");
		for (int i=0; i<catVector.size(); i++) buffer.append("'"+catVector.elementAt(i)+"',");
		buffer.deleteCharAt(buffer.length()-1);
		buffer.append("];\n");
		buffer.append("var subcategories = [");
		for (int i=0; i<subcatVector.size(); i++) buffer.append("'"+subcatVector.elementAt(i)+"',");
		buffer.deleteCharAt(buffer.length()-1);
		buffer.append("];\n");		
		buffer.append("var data =[");
	    for (int i=0; i<catVector.size(); i++)
	    	{
	    	String category = (String)catVector.elementAt(i);
	    	buffer.append("[");
	    	for (int j=0; j<subcatVector.size(); j++)
	 	    	{
	 	    	Vector aux;
	 	    	if (stacked) aux = (Vector)data.get(category+"\t"+subcatVector.elementAt(j));
	 	    	else aux = (Vector)data.get(category);
	 	    	String value = "0";
	 	    	if (aux != null)
	 	    		{
	 	    		if (stacked) value = decimal.format(((Float)aux.firstElement()).floatValue()*100/((Float)totals.get(category)).floatValue());
	 	    		else value = decimal.format(((Float)aux.firstElement()).floatValue());
	 	    		}
	 	    	buffer.append(value+",");
	 	    	}
	    	buffer.deleteCharAt(buffer.length()-1);
	    	buffer.append("],");
	    	}
	    buffer.deleteCharAt(buffer.length()-1);
    	buffer.append("];\n");
    	return buffer.toString();
		}
	
	/**
	 * Generates a report in Excel format
	 * @param report The report settings
	 * @param data The report data
	 * @param type The file where save the report (if publish is true)
	 * @param outputStream The file stream where to write the report to (if publish is false)
	 * @param publish Whether to publish the report in a file or write it to the output stream
	 * @throws Throwable
	 */	
	private void excel(Report report, Hashtable data, String type, OutputStream outputStream, boolean publish) throws Throwable
		{
		WritableWorkbook workbook;
		String title = title(report);
		File file = null;
		if (publish)
			{
			File directory = new File(reportsDirectory,report.period);
			if (!directory.exists()) directory.mkdir();
			directory = new File(directory,(String)report.dates[report.dates.length-1]);
			if (!directory.exists()) directory.mkdir();
			file = new File(directory,title.replaceAll(" ","_")+".xls");
			workbook = Workbook.createWorkbook(file,Workbook.getWorkbook(Reports.class.getResourceAsStream(type+".xls")));
			}
		else workbook = Workbook.createWorkbook(outputStream,Workbook.getWorkbook(Reports.class.getResourceAsStream(type+".xls")));
		Label label = new Label(0,0,title); 
		workbook.getSheet(0).addCell(label);
		if (type.equals("bar")) bar(report,data,workbook.getSheet(0));
		else if (type.equals("column")) column(data,workbook.getSheet(0));
		else if (type.equals("stack")) stack(data,workbook.getSheet(0),report.reportType.endsWith("all states"));
		else if (type.equals("raw")) raw(report,data,workbook.getSheet(0));
		else if (type.startsWith("performance") || type.equals("utilization")) report(data,workbook.getSheet(0));
		workbook.write(); 
		workbook.close();
		if (publish) email(report,file); // Email report file to recipients
		}
	
	/**
	 * Generate the title for a report
	 * @param report The Report object
	 * @return A string with the report title
	 */
	private String title(Report report)
		{
		String dates;
		if (report.period.equals("Raw")) dates = " from "+formatter.format(report.startDate)+" to "+formatter.format(report.endDate);
		else dates = " from "+report.period+" "+(report.dates.length>0?report.dates[0]:"")+" to "+(report.dates.length>0?report.dates[report.dates.length-1]:"");
		String title = report.reportType+" by "+report.grouping+dates;
		// Add machine types and or locations to title
		if (report.types != null && report.types.length > 0)
			{
			title += " types ";
			for (int i=0; i<report.types.length; i++) title += report.types[i]+",";
			}
		if (report.locations != null && report.locations.length > 0)
			{
			title += " locations ";
			for (int i=0; i<report.locations.length; i++) title += report.locations[i]+",";
			}
		if (title.endsWith(",")) title = title.substring(0,title.length()-1);
		return title;
		}
	
	/**
	 * Emails the report stored in file
	 * @param report A Report object with the report definition
	 * @param file A File object with the report
	 * @throws Throwable
	 */
	private void email(Report report, File file) throws Throwable
		{
		Vector to = new Vector(), cc = new Vector();
		for (int i=0; i<report.emails.size(); i++)
			{
			Email email = (Email)report.emails.elementAt(i);
			if (email.type == null || email.type.equals("To")) to.addElement(email.email);
			else cc.addElement(email.email);
			}
		if (to.size() > 0 || cc.size() > 0) EmailServer.send(EMAIL_SERVER,FROM_USER,FROM_ADDRESS,to,cc,SUBJECT,CONTENT,file);
		}
	
	/**
	 * Generates a report in bar format
	 * @param report The report settings
	 * @param data The report data
	 * @param sheet The Excel work sheet where the report will be written
	 * @throws Throwable
	 */
	private void bar(Report report, Hashtable data, WritableSheet sheet) throws Throwable
		{
		Label label = new Label(0,1,report.grouping); 
		sheet.addCell(label);
		label = new Label(1,1,report.reportType); 
		sheet.addCell(label);
		Vector categories = new Vector(data.keySet());
		Collections.sort(categories);
		int row = 2;
	    for (int i=0; i<categories.size(); i++)
	    	{
	    	String category = (String)categories.elementAt(i);
	    	float value = ((Float)((Vector)data.get(category)).firstElement()).floatValue();
	    	if (value != 0)
	    		{
		    	label = new Label(0,row,category); 
				sheet.addCell(label);
				Number number = new Number(1,row,value); 
				sheet.addCell(number);
				row++;
	    		}
	    	}
		}
	
	/**
	 * Generates a report in column format
	 * @param data The report data
	 * @param sheet The Excel work sheet where the report will be written
	 * @throws Throwable
	 */
	private void column(Hashtable data, WritableSheet sheet) throws Throwable
		{
		Vector categories = new Vector(data.keySet());
		Collections.sort(categories);
		int row = 2;
	    for (int i=0; i<categories.size(); i++)
	    	{
	    	String category = (String)categories.elementAt(i);
	    	Vector values = (Vector)data.get(category);
	    	if (values.size()>0)
	    		{
		    	Label label = new Label(0,row,category);
		    	sheet.addCell(label);
		    	for (int j=0; j<values.size(); j++)
		    		{
					Number number = new Number(j+1,row,((Float)values.elementAt(j)).floatValue()); 
					sheet.addCell(number);
		    		}
		    	row++;
	    		}
	    	}
		}
	
	/**
	 * Generates a report in stacked bar format
	 * @param data The report data
	 * @param sheet The Excel work sheet where the report will be written
	 * @throws Throwable
	 */
	private void stack(Hashtable data, WritableSheet sheet, boolean addPerformance) throws Throwable
		{
		Hashtable categories = new Hashtable(), subcategories = new Hashtable(), totals = getTotals(data);
		int rowCount = 2, columnCount = 1;
		Vector vector = new Vector(data.keySet());
		Collections.sort(vector);
	    for (int i=0; i<vector.size(); i++)
	    	{
	    	String aux = (String)vector.elementAt(i);
	    	int index = aux.indexOf("\t");
	    	String category = aux.substring(0,index), subcategory = aux.substring(index+1,aux.length());
	    	float value = ((Float)((Vector)data.get(aux)).firstElement()).floatValue()*100/((Float)totals.get(category)).floatValue();
	    	Integer row = (Integer)categories.get(category), column = (Integer)subcategories.get(subcategory);
	    	if (row == null)
	    		{
	    		Label label = new Label(0,rowCount,category); 
				sheet.addCell(label);
				row = new Integer(rowCount);
				categories.put(category,row);
				rowCount++;
	    		}
	    	if (column == null)
	    		{
	    		Label label = new Label(columnCount,1,subcategory); 
				sheet.addCell(label);
				label = new Label(columnCount,65535,(String)colors.get(subcategory)); 
				sheet.addCell(label);
				column = new Integer(columnCount);
				subcategories.put(subcategory,column);
				columnCount++;
	    		}
			Number number = new Number(column.intValue(),row.intValue(),value); 
			sheet.addCell(number);
	    	}
	    if (addPerformance) addPerformance(data,vector,categories,totals,columnCount,sheet);	    
	    Label label = new Label(0,65535,"Color"); 
		sheet.addCell(label);
		}
	
	/**
	 * Generates a report in raw format
	 * @param report The report settings
	 * @param data The report data
	 * @param sheet The Excel work sheet where the report will be written
	 * @throws Throwable
	 */
	private void raw(Report report, Hashtable data, WritableSheet sheet) throws Throwable
		{
		Vector categories = new Vector(data.keySet());
		Collections.sort(categories);
	    for (int i=0; i<categories.size(); i++)
	    	{
	    	String[] items = ((String)data.get(categories.elementAt(i))).split("\t");
	    	for (int j=0; j<items.length; j++) sheet.addCell(new Label(j,i+1,items[j]));
	    	}
		}
	
	/**
	 * Write performance numbers: OEE, First Pass Site Perf Loss, First Pass Yield Loss and Retest Gain 
	 * @param data The report data
	 * @param vector A vector with a list of categories and subcategories
	 * @param categories A hashtable with all teh categories
	 * @param totals A hashtable with the time totals for each category
	 * @param columnCount The maximum column count
	 * @param sheet The Excel work sheet where the report will be written
	 * @throws Throwable
	 */
	private void addPerformance(Hashtable data, Vector vector, Hashtable categories, Hashtable totals, int columnCount, WritableSheet sheet) throws Throwable
		{
		sheet.addCell(new Label(columnCount+1,1,"OEE"));
		sheet.addCell(new Label(columnCount+2,1,"First Pass Site Perf Loss"));
		sheet.addCell(new Label(columnCount+3,1,"First Pass Yield Loss"));
		sheet.addCell(new Label(columnCount+4,1,"Retest Gain"));
	    String prevCategory = null;
	    float oee = 0, firstPassSitePerfLoss = 0, firstPassYieldLoss = 0, retestGain = 0;
	    for (int i=0; i<vector.size(); i++)
	    	{
	    	String aux = (String)vector.elementAt(i);
	    	String category = aux.substring(0,aux.indexOf("\t"));	    	
	    	if (prevCategory != null && !category.equals(prevCategory))
	    		{
	    		Integer row = (Integer)categories.get(prevCategory);
	    		float total = ((Float)totals.get(category)).floatValue();
	    		sheet.addCell(new Number(columnCount+1,row.intValue(),oee/total));
	    		sheet.addCell(new Number(columnCount+2,row.intValue(),firstPassSitePerfLoss/total));
	    		sheet.addCell(new Number(columnCount+3,row.intValue(),firstPassYieldLoss/total));
	    		sheet.addCell(new Number(columnCount+4,row.intValue(),retestGain/total));
				oee = firstPassSitePerfLoss = firstPassYieldLoss = retestGain = 0;
	    		}
			prevCategory = category;
	    	Vector values = (Vector)data.get(aux);
	    	oee += ((Float)values.elementAt(1)).floatValue();
	    	firstPassSitePerfLoss += ((Float)values.elementAt(2)).floatValue();
	    	firstPassYieldLoss += ((Float)values.elementAt(3)).floatValue();
	    	retestGain += ((Float)values.elementAt(4)).floatValue();
	    	}
	    if (prevCategory == null) return;
		Integer row = (Integer)categories.get(prevCategory);
		float total = ((Float)totals.get(prevCategory)).floatValue();
		sheet.addCell(new Number(columnCount+1,row.intValue(),oee/total));
		sheet.addCell(new Number(columnCount+2,row.intValue(),firstPassSitePerfLoss/total));
		sheet.addCell(new Number(columnCount+3,row.intValue(),firstPassYieldLoss/total));
		sheet.addCell(new Number(columnCount+4,row.intValue(),retestGain/total));	
		}
	
	/**
	 * Generates a report in text format
	 * @param data The report data
	 * @param sheet The Excel work sheet where the report will be written
	 * @throws Throwable
	 */
	private void report(Hashtable data, WritableSheet sheet) throws Throwable
		{
		Vector categories = new Vector(data.keySet());
		Collections.sort(categories);
		int row = 2;
	    for (int i=0; i<categories.size(); i++)
	    	{
	    	String category = (String)categories.elementAt(i);
	    	Vector values = (Vector)data.get(category);
	    	if (values.size()>0)
	    		{
	    		String[] aux = category.split("\t",-1);
	    		for (int j=0; j<aux.length; j++)
	    			{
	    		   	Label label = new Label(j,row,aux[j]);
			    	sheet.addCell(label);
	    			}
		    	for (int j=0; j<values.size(); j++)
		    		{
					Number number = new Number(aux.length+j,row,((Float)values.elementAt(j)).floatValue()); 
					sheet.addCell(number);
		    		}
		    	row++;
	    		}
	    	}
		}
	
	/**
	 * Returns a hashtable with the accumulate duration of all events for each machine
	 * @param data The report data
	 * @return The hashtable with the durations
	 * @throws Throwable
	 */
	private Hashtable getTotals(Hashtable data) throws Throwable
		{
		Hashtable totals = new Hashtable();
		Enumeration enumeration = data.keys();
		while(enumeration.hasMoreElements())
			{
	    	String categories = (String)enumeration.nextElement();
	    	float value = ((Float)((Vector)data.get(categories)).firstElement()).floatValue();
	    	String category = categories.substring(0,categories.indexOf("\t"));
	    	Float stored = (Float)totals.get(category);
	    	if (stored == null) {totals.put(category,new Float(value));}
	    	else {totals.put(category,new Float(stored.floatValue()+value));}
	    	}
		return totals;
		}
	
	/**
	 * Class that compares two shifts with format YYYY-MM-DD-shift (i.e. 2009-06-01-2)
	 */
	private class CompareDates implements Comparator
		{
		/**
		 * Return 1 if object1 > object2 -1 if object1 < object2 and 0 is object1 == object2
		 * @param object1 A shift with format YYYY-MM-DD-shift
		 * @param object2 A shift with format YYYY-MM-DD-shift
		 */
		public int compare(Object object1, Object object2)
			{
			String time1 = (String)object1, time2 = (String)object2;
			int index1 = time1.lastIndexOf("-"), index2 = time2.lastIndexOf("-");
			String fDate1 = time1.substring(0,index1), shift1 = time1.substring(index1+1), fDate2 = time2.substring(0,index2), shift2 = time2.substring(index2+1); 
			if (fDate1.equals(fDate2))
				{
				try
					{
					FiscalDate date1 = new FiscalDate("Shift",fDate1,shift1);
					FiscalDate date2 = new FiscalDate("Shift",fDate2,shift2);
					return (int)FiscalDate.diff(date1.startTime,date2.startTime);
					}
				catch (Throwable t) {return 0;}
				}
			else return fDate1.compareTo(fDate2);
			}
		}
	}
			