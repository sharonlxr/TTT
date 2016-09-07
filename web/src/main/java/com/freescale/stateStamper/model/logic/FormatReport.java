package com.freescale.stateStamper.model.logic;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jxl.Workbook;
import jxl.WorkbookSettings;
import jxl.write.Label;
import jxl.write.Number;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;

import com.freescale.serverUtil.EmailServer;
import com.freescale.stateStamper.model.entity.Email;
import com.freescale.stateStamper.model.entity.Report;

/**
 * This class generates reports requested by the GUI and publishes them as well
 */

public class FormatReport
	{
	private static final String EMAIL_SERVER = "remotesmtp.freescale.net", FROM_USER = "ToolTimeTracker", FROM_ADDRESS = "ToolTimeTracker@freescale.com",DOMAIN="freescale.com";
	private static final String CONTENT = "Utilization report attached\n\n   The information contained in this email has been classified Freescale Confidential Proprietary";
	private static final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private static String emailServer = EMAIL_SERVER, fromUser = FROM_USER, fromAddress = FROM_ADDRESS, content = CONTENT,domain=DOMAIN;  

	public static boolean addPerformance(Report report) {return report.reportType.equals("Utilization all states") || report.reportType.equals("Losses") || (report.reportType.equals("Losses Breakdown")&&(!report.grouping.equals("Device Name")));}
	public static boolean stackedNoNPercentageCategory(String subcategory){ return subcategory.trim().equals("Good Count");}	
	private static String reportsDirectory;
	private static WorkbookSettings settings;
	private static Logger logger;
	
	public FormatReport(String reportsDirectory)
		{
		FormatReport.reportsDirectory = reportsDirectory;
		logger = LoggerFactory.getLogger("stateStamper");
		}
	
	public static WorkbookSettings getWorkbookSettings()
		{
		if (settings == null)
			{
			settings = new WorkbookSettings();
			settings.setSuppressWarnings(true);
			}
		return settings;
		}

	public static WritableWorkbook createWorkbook(OutputStream os, Workbook in) throws Throwable
		{
		return Workbook.createWorkbook(os, in, getWorkbookSettings());
		}

	public static WritableWorkbook createWorkbook(OutputStream os) throws Throwable
		{
		return Workbook.createWorkbook(os, getWorkbookSettings());
		}
		
	/**
	 * Generates a report in HTML format
	 * @param report The report settings
	 * @param data The report data
	 * @param type The file where save the report (if publish is true)
	 * @param outputStream The file stream where to write the report to (if publish is false)
	 * @param publish Whether to publish the report in a file or write it to the output stream
	 * @param endOfShift Whether it is the end of shift or not
	 * @throws Throwable
	 */	
	public static void html(Report report, String[] reportColumns, Hashtable data, OutputStream outputStream, boolean publish, boolean endOfShift, String reportsDirectory) throws Throwable
		{	
		StringBuffer buffer = new StringBuffer();	
		if (report.chartType.equals("text")) buffer = formatTextHTML(report.reportType,title(report,true),reportColumns,data);
		else
			{
			String content  = formatChartHTML(title(report,true),format(report,report.period,reportColumns[3],reportColumns[4],reportColumns[5],report.chartType.equals("stack"),data),report.totals,addPerformance(report),report.period.equals("Raw")&&publish);
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(StateStamper.class.getResourceAsStream("chart.html")));
			try
				{
				String line;
				while((line = bufferedReader.readLine()) != null)
					{
					if (line.indexOf("<!--variables-->") !=  -1) buffer.append(content);
					else buffer.append(line+"\n");
					}
				}
			finally {bufferedReader.close();}
			}	
		if (publish)
			{
			File file = publish(report,".html",reportsDirectory);
			PrintWriter pw = new PrintWriter(new FileWriter(file));			
			try {pw.print(buffer.toString());}
			finally {pw.close();}
			if (endOfShift) email(report,reportColumns,data,file); // Email report file to recipients only at end of shift
			}
		else
			{
			try {outputStream.write(buffer.toString().getBytes()); outputStream.flush();}
			finally {outputStream.close();}				
			}		
		}
	
	/**
	 * Generates a report in Excel format
	 * @param report The report settings
	 * @param data The report data
	 * @param type The file where save the report (if publish is true)
	 * @param outputStream The file stream where to write the report to (if publish is false)
	 * @param publish Whether to publish the report in a file or write it to the output stream
	 * @param endOfShift Whether it is the end of shift or not
	 * @throws Throwable
	 */	
	public static void excel(Report report, String[] reportColumns, Hashtable data, OutputStream outputStream, boolean publish, boolean endOfShift, String reportsDirectory, int exlcudedclumns) throws Throwable
		{
		WritableWorkbook workbook = null;
		try{
		File file = null;
		if (publish)
			{
			file = publish(report,".xls",reportsDirectory);
			outputStream = new FileOutputStream(file);
			}
		if (report.chartType.equals("text"))
			{
			workbook = createWorkbook(outputStream);
			workbook.createSheet("Report",0);
			}
		else workbook = createWorkbook(outputStream,Workbook.getWorkbook(FormatReport.class.getResourceAsStream("chart.xls")));	
		if (report.chartType.equals("text")) formatTextExcel(report.reportType,title(report,true),reportColumns,data,workbook.getSheet(0), exlcudedclumns);
		else formatChartExcel(title(report,true),format(report,report.period,reportColumns[3],reportColumns[4],reportColumns[5],report.chartType.equals("stack"),data),workbook.getSheet(0),report.totals,addPerformance(report)); 
		workbook.write(); 
		workbook.close();
		workbook = null;
		if (publish && endOfShift) email(report,reportColumns,data,file); // Email report file to recipients only at end of shift
		}catch (Throwable th){
			logger.error(report.reportType+report.grouping,th);
		throw th;
		}
		finally{
			if (workbook!=null)
			workbook.close();
		}
		}
	
	public static void csv(Report report, String[] reportColumns, Hashtable data, OutputStream outputStream, boolean publish, boolean endOfShift, String transferDirectory,String areaCode,String plantCode,String stage)throws Throwable
	    {
		File file = null;
		String suffix=null;
		suffix = stage + "_"+ areaCode + "_" +  plantCode;
		if (publish)
		    {
		    file = publish(report,suffix,transferDirectory);
		    outputStream = new FileOutputStream(file);
		    }
		formatCSV(report,reportColumns,data,outputStream);
		if (publish && endOfShift) email(report,reportColumns,data,file); // Email report file to recipients only at end of shift
	    }
	
	/**
	 * Generate the title for a report
	 * @param report The Report object
	 * @param addDate Whether to add the dates to teh title or not 
	 * @return A string with the report title
	 */
	public static String title(Report report, boolean addDate)
		{
		String dates = "";
		if (addDate)
			{
			if (report.period.equals("Raw")) dates = " from "+formatter.format(report.startDate)+" to "+formatter.format(report.endDate);
			else dates = " from "+report.period+" "+(report.dates.length>0?report.dates[0]:"")+" to "+(report.dates.length>0?report.dates[report.dates.length-1]:"");
			}
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
	 * Format report data
	 * @param reportType The report type
	 * @param period The report period
	 * @param reportCategory The category
	 * @param subcategory1 The first subcategory
	 * @param subcategory2 The second subcategory
	 * @param stacked Whether the data is stacked or not
	 * @param data A hashtable with the report data
	 * @return A 2D String array with the report data
	 * @throws Throwable
	 */
	private static String[][] format(Report report, String period, String reportCategory, String subcategory1, String subcategory2, boolean stacked, Hashtable data) throws Throwable
		{
		Hashtable categories = new Hashtable(), subcategories = new Hashtable(), totals = null;
		if (stacked) totals = getTotals(data,addPerformance(report));
		List<String> keys = new Vector(data.keySet());
		for (String key : keys)
			{
			if (!key.contains("null"))
				{
				int index = key.indexOf("\t");
				categories.put(key.substring(0, index), "");
				subcategories.put(key.substring(index + 1), "");
				}
			}
		List<String> categoryVector = new Vector(categories.keySet()), subcategoryVector = new Vector(subcategories.keySet());
		if (reportCategory.startsWith("DATE") && (period.equals("Raw") || period.equals("Shift"))) Collections.sort(categoryVector,new GenerateReport.CompareDates());
		else Collections.sort(categoryVector);  // If the category is not the date, then sort the values
		Collections.sort(subcategoryVector);
		String[][] result = new String[subcategoryVector.size()+1][categoryVector.size()+2];
		result[0][0] = subcategory1.equals("BLANK")?"":deCapitalize(subcategory1); 
		result[0][1] = subcategory2.equals("BLANK")?"":deCapitalize(subcategory2);
	    for (int i=0; i<categoryVector.size(); i++)
	    	{
	    	String category = categoryVector.get(i);
	    	result[0][i+2] = category;
	       	for (int j=0; j<subcategoryVector.size(); j++)
	 	    	{
	 	    	String subcategory = subcategoryVector.get(j);
	 	      	if (i==0)
	 	    		{
	 	    		String[] aux = subcategory.split("\t");
		 	    	result[j+1][0] = aux.length>0?aux[0]:"";
	 	    		result[j+1][1] = aux.length>1?aux[1]:"";
	 	    		}
	 	    	List aux = (List)data.get(category+"\t"+subcategory);
			 	float value = 0;
			 	if (aux != null)
			 		{
			 		value = ((Float)aux.get(0)).floatValue();
			 		if (stacked && value!=0F) value *= 100/((Float)totals.get(category)).floatValue();
			 		if (stackedNoNPercentageCategory(subcategory)){value = ((Float)aux.get(0)).floatValue();}
			 		}
			 	result[j+1][i+2] = ""+value;
	 	    	}
			}   
	    return result;
		}
	
	/**
	 * Format report data (categories and subcategories) in HTML format
	 * @param title The title for the report
	 * @param result A 2D array with the report rows and columns
	 * @param sheet An Excel sheet to write the data to
	 * @param subtotals A string with the subtotals that need to be calculated
	 * @param ignoreEmptyGroups Whether to ignore rows with empty groups to calculate totals
	 * @throws Throwable
	 */
	private static void formatChartExcel(String title, String[][] result, WritableSheet sheet, String subtotals, boolean ignoreEmptyGroups) throws Throwable
		{
		sheet.addCell(new Label(0,0,title)); // Write title in cell A1
		sheet.addCell(new Label(1,0,subtotals));  // Write subtotals in cell B1
		sheet.addCell(new Label(2,0,""+ignoreEmptyGroups));  // Write exclusions in cell C1
	
		StringBuilder buffer = new StringBuilder();
		writeColors(buffer,result,0,false); // Write colors for groups
    	writeColors(buffer,result,1,false); // Write colors for substates
    	sheet.addCell(new Label(3,0,buffer.toString()));  // Write colors
		    	
	    for (int i=0; i<result.length; i++)
	    	{
	    	for (int j=0; j<result[0].length; j++)
	    		{
	    		if (i==0 || j<2) sheet.addCell(new Label(j+1,i+1,result[i][j]));
	    		else sheet.addCell(new Number(j+1,i+1,Float.parseFloat(result[i][j])));
	    		}
	    	}
		}
	
	/**
	 * Generates a report in text format
	 * @param data The report data
	 * @param sheet The Excel work sheet where the report will be written
	 * @throws Throwable
	 */
	private static void formatTextExcel(String reportType, String title, String[] reportColumns, Hashtable data, WritableSheet sheet, int excludecloumns) throws Throwable
		{
		sheet.addCell(new Label(0,0,title)); // Write title in cell A1
		boolean summing = false;
		for (int i=excludecloumns; i<reportColumns.length; i++)  // Ignore first 3 columns (chart type, subtotals and condition)
			{
			if (ProcessRecord.isSumming(reportColumns[i])) summing = true;
			sheet.addCell(new Label(i-3,1,deCapitalize(reportColumns[i])));
			}
		List categories = new Vector(data.keySet());
		Collections.sort(categories);
		int row = 2;
	    for (int i=0; i<categories.size(); i++)
	    	{
	    	String category = (String)categories.get(i);
	    	List values = (List)data.get(category);
	    	if (summing)
	    		{
	    		String[] aux = category.split("\t",-1);
	    		for (int j=0; j<aux.length; j++) sheet.addCell(new Label(j,i+2,aux[j]));
		    	for (int j=0; j<values.size(); j++) sheet.addCell(new Number(aux.length+j,i+2,((Float)values.get(j)).floatValue()));
		    	}
	    	else
	    		{
	    		if (reportType.equals("Jam Alarms")) Collections.sort(values);
		      	for (int j=0; j<values.size(); j++) 
		      		{
		      		sheet.addCell(new Label(0,row,category));
					String[] aux = ((String)values.get(j)).split("\t");
					for (int k=0; k<aux.length; k++) sheet.addCell(new Label(k+1,row,aux[k]));
					row++;
		      		}
	    		}
	    	}
		}
	
	/**
	 * Generates a report in text format
	 * @param data The report data
	 * @param sheet The Excel work sheet where the report will be written
	 * @throws Throwable
	 */
	private static void formatCSV(Report report,  String[] reportColumns, Hashtable data, OutputStream ostream) throws Throwable
		{
		String grouping = report.grouping;
		int head_len =3;// first 3 columns (chart type, subtotals and condition)
		boolean summing = false;
	    OutputStreamWriter osw = new OutputStreamWriter(ostream);
	    BufferedWriter bw = new BufferedWriter(osw);
	    String comma=",";
	    Vector colNames = new Vector();
	    String totals = reportColumns[1];
	    for(int i=head_len;i<reportColumns.length;i++)
	    {
	    	colNames.add(reportColumns[i]);
	    }

	    if(! totals.equals("NONE"))
	       {
	    	String [] totalsBy;
	    	String aux;
	    	int start=totals.indexOf("(");
	    	int end =totals.lastIndexOf(")");
	    	aux = totals.substring(start+1, end);
	    	totalsBy= aux.split(",");
	    	for(int i=0;totalsBy !=null && i< totalsBy.length;i++ )
	    	   {
	    		String totalColName="TOTAL";
	    		String percentage="PERCENTAGE";
	    		String args[] = totalsBy[i].split("&&");
	    		Vector  indexs = new Vector();
	    		for (String totArg : args) {
	    			totalColName += "_"+totArg;
	    			percentage += "_"+totArg;
					for(int j=head_len;j<reportColumns.length;j++){
						if(totArg.equals(reportColumns[j])){
							indexs.add(j-head_len);
						}
					}
				}
	    		colNames.add(totalColName);
	    		colNames.add(percentage);
	    		addTotalsBy(data,indexs,true);
	    	   }
	       }
	    
	    reportColumns = new String[0];
	    reportColumns = (String[])colNames.toArray(reportColumns);
	    StringBuffer fheader = new StringBuffer();
		for (int i=0; i<reportColumns.length; i++)  
			{
			if (ProcessRecord.isSumming(reportColumns[i])) summing = true;
			fheader.append(deCapitalize(reportColumns[i])+comma);
			}
		String heads = fheader.toString();
		if(heads.endsWith(","))heads=heads.substring(0, heads.length()-1 );
		bw.write(heads);
		bw.write("\n");
		List categories = new Vector(data.keySet());
		Collections.sort(categories);
		DecimalFormat twoDec = new DecimalFormat("0.00");
		
	    for (int i=0; i<categories.size(); i++)
	    	{
	    	String category = (String)categories.get(i);
	    	List values = (List)data.get(category);
	    	if (summing)
	    		{
	    		StringBuffer row=new StringBuffer();
	    		String[] aux = category.split("\t");
	    		for (int j=0; j<aux.length; j++)
	    		    {
	    			aux[j]=aux[j].replace(',',' ');
	    			row.append(aux[j]+comma);
	    			}
		    	for (int j=0; j<values.size(); j++) 
		    	    {
		    		Float v = (Float)values.get(j);
		    		row.append(twoDec.format(v)+comma);
		    	    }
		    	//remove last ,
		    	row.deleteCharAt(row.length()-1);
		    	bw.write(row.toString());
		    	bw.write("\n");
		    	}
	    	else
	    		{
	    		String[] head = category.split("\t");
	    		StringBuffer body,header = new StringBuffer();
	    		for(int j=0;j<head.length;j++){
	    			head[j]=head[j].replace(',',' ');//replace , in value to avoid being considered a different column 
	    			header.append(head[j]+comma);
	    			}
		      	for (int j=0; j<values.size(); j++) 
		      		{
					String[] aux = ((String)values.get(j)).split("\t");
					body = new StringBuffer();
					for (int k=0; k<aux.length; k++)
					    {
			            //if value in scientific notation, convert it to something understandable
			            int idxDot = aux[k].indexOf(".") ;
			            int idxE = aux[k].indexOf("E") ;
			            if(idxDot > -1 && idxE>-1 && idxE> idxDot)
			               {
			            	try{
			            	float f= Float.parseFloat(aux[k]);
			            	aux[k] = twoDec.format(f);
			            	}catch(NumberFormatException nbe){}
			               }
						body.append(aux[k]+comma);
					    }
					//remove the last ,
					body.deleteCharAt(body.length()-1);
					bw.write(header.toString()+body.toString() );
					bw.write("\n");
		      		}
	    		}
	    	}
	       bw.flush();
	       bw.close();
		}

	/**
	 * Format report data (categories and subcategories) in HTML format
	 * @param title The title for the report
	 * @param result A 2D array with the report rows and columns
	 * @param subtotals A string with the subtotals that need to be calculated
	 * @param ignoreEmptyGroups Whether to ignore rows with empty groups to calculate totals
	 * @param reload True if the report will auto-reload
	 * @return A String with the data in HTML format
	 * @throws Throwable
	 */
	private static String formatChartHTML(String title, String[][] result, String subtotals, boolean ignoreEmptyGroups, boolean reload) throws Throwable
		{
		StringBuilder buffer = new StringBuilder();
		buffer.append("var title = '"+title+"';\n");
		buffer.append("var subtotals = '"+subtotals+"';\n");
		buffer.append("var isStacked = true;\nvar isBar = true;\nvar ignoreEmptyGroups = "+ignoreEmptyGroups+";\n");		
		buffer.append("var categories = "+toString(result,2,0,0,true)+";\n");
		buffer.append("var groups = "+toString(result,1,0,1,true)+";\n");
		buffer.append("var subcategories = "+toString(result,1,1,1,true)+";\n");
		buffer.append("var data =[");
	    for (int i=2; i<result[0].length; i++)
	    	{
	    	buffer.append(toString(result,1,i,1,false));
	    	buffer.append(",");
	    	}
	    if (result.length>1) buffer.deleteCharAt(buffer.length()-1);
    	buffer.append("];\n");
    	writeColors(buffer,result,0,true); // Write colors for groups
    	writeColors(buffer,result,1,true); // Write colors for substates
    	buffer.append("var reload = "+reload+";\n");  // Specify if the report will auto-reload
    	return buffer.toString();
		}
	
	/**
	 * Format report data
	 * @param data A hashtable with the report data
	 * @return A string with the report
	 * @throws Throwable
	 */
	private static StringBuffer formatTextHTML(String reportType, String title, String[] reportColumns, Hashtable data) throws Throwable
		{
		StringBuffer buffer = new StringBuffer();
		boolean summing = false;
		buffer.append("<style type='text/css'>"+
				"body {font-family:Tahoma,Arial,Helvetica,sans-serif;}"+
				"h1 {font-size:14px;}"+
				"div {font-size:12px;}"+
				"table {font-size:12px; border-collapse:collapse; border-width:1px; border-style:solid; border-color:#A0A0A0;}"+
				"th {border-width:1px; border-style:solid; border-color:#A0A0A0;}"+ 
				"td {border-width:1px; border-style:solid; border-color:#A0A0A0;}"+ 
				"</style>");
		buffer.append("<center><h1>"+title+"</h1>");
		buffer.append("<table border=1><tr>");
		for (int i=3; i<reportColumns.length; i++)  // Ignore first 3 columns (chart type, subtotals and condition)
			{
			if (ProcessRecord.isSumming(reportColumns[i])) summing = true;
			buffer.append("<th>"+deCapitalize(reportColumns[i])+"</th>");
			}
		buffer.append("</tr>");
		List categories = new Vector(data.keySet());
		Collections.sort(categories);
		for (int i=0; i<categories.size(); i++)
			{
	    	String category = (String)categories.get(i);
	    	List values = (List)data.get(category);
	    	if (summing)
	    		{
		    	buffer.append("<tr>");
		    	String[] aux = category.split("\t");
		    	for (int j=0; j<aux.length; j++) buffer.append("<td>"+aux[j]+"</td>");
		    	for (int j=0; j<values.size(); j++)  buffer.append("<td>"+values.get(j)+"</td>");
		    	buffer.append("</tr>");
	    		}
	    	else
	    		{
	    		if (reportType.equals("Jam Alarms")) Collections.sort(values);
		      	for (int j=0; j<values.size(); j++) 
		      		{
					buffer.append("<tr><td>"+category+"</td>");
					String[] aux = ((String)values.get(j)).split("\t");
					for (int k=0; k<aux.length; k++) buffer.append("<td>"+aux[k]+"</td>");
		    		buffer.append("</tr>");
		      		}
	    		}
	    	}
		buffer.append("</table></center>");
		return buffer;
		}
	
	/**
	 * Writes defined colors to string buffer
	 * @param buffer The string buffer
	 * @param result A 2D table with the report results
	 * @param pivot An integer indicating which column to write colors for
	 * @param isHTML Whether the output is for HTML chart or not (Excel)
	 */
	private static void writeColors(StringBuilder buffer, String[][] result, int pivot, boolean isHTML)
		{
		// Create list of all states without duplicates
		List states = new Vector();
		for (int i=1; i<result.length; i++)
			{
			String state = result[i][pivot];
			if (state.trim().length()>0 && !states.contains(state)) states.add(state);
			}
		// Output state and color
		for (int i=0; i<states.size(); i++)
			{
			String state = (String)states.get(i);
			String hex = StateTransition.getColor(state);
			if (hex != null) // hex = Integer.toHexString((int)(Math.random()*0xFFFFFF));
				{		
				if (isHTML)
					{
					long color = Integer.parseInt(hex,16);
					buffer.append("colors['"+state+"']=["+(color>>16)+","+(color>>8 & 0xFF)+","+(color%256)+"];\n");
					}
				else buffer.append(state+","+hex+",");
				}
			}
		}
	
	/**
	 * Return a decapitalized version of the input string. For instance DEVICE_ID becomes Device Id
	 * @param name The input string
	 * @return The input string decapitalized
	 */
	private static String deCapitalize(String name)
		{
		StringBuffer buffer = new StringBuffer();
		name = name.replaceAll("_"," ");
		boolean upper = true;
		for (int i = 0; i <name.length(); i++)
			{
			if (upper)
				{
				buffer.append(name.charAt(i));
				upper = false;
				}
			else buffer.append(Character.toLowerCase(name.charAt(i)));
			if (name.charAt(i) == ' ') upper=true;
			}
		return buffer.toString();
		}
	
	/**
	 * Creates a file to publish this report
	 * @param report The report
	 * @param extension File extension
	 * @return A file to publish this report
	 */
	private static File publish(Report report, String extension, String reportsDirectory)
		{
		File directory = new File(reportsDirectory,report.period);
		if (!directory.exists()) directory.mkdir();
		// Create All subdirectory for raw reports
		if (report.period.equals("Raw")) directory = new File(directory,"All");
		//create a new directory for the date when report is not csv, csv reports are stored at report level
		else if( ! report.chartType.equals("csv")) directory = new File(directory,(String)report.dates[report.dates.length-1]);
		if (!directory.exists()) directory.mkdir();
		String name;
		if(report.chartType.equals("csv"))
		   {
			directory = new File(reportsDirectory);
			StringBuffer date = new StringBuffer(); 
			for(Object obj:report.dates)
				date.append(obj.toString()+"_");
			if(date.length() > 0 && date.charAt(date.length()-1) == '_' ){date.deleteCharAt(date.length()-1);}
			name = report.reportType + "_"+ report.grouping + "_" + extension+ "_" +date.toString() + ".csv";
			name = name.replace(" ", "_");
			name = name.toLowerCase();
		   }
		else
		   name=title(report,!report.period.equals("Raw")).replaceAll(" ","_")+extension;
		return new File(directory,name);
		}
 	
	/**
	 * E-mails the report stored in file
	 * @param report A Report object with the report definition
	 * @param Hashtable A hash table with the report data 
	 * @param file A File object with the report
	 * @throws Throwable
	 */
	private static void email(Report report, String[] reportColumns, Hashtable data, File file) throws Throwable
		{
		if(!report.hasEmailRecipients())return;
		String subject = "TTT Report: " + file.getName();		
		if (subject.length() > 255)
			{
			subject.substring(0, 255);
			}

		String content = "\n\n"+FormatReport.content;
		if (report.reportType.equals("Handler Jam Alarms")) content = printSummary(report,data)+"\n\n"+FormatReport.content;
		if (report.reportType.equals("Temperature Change Over")) content = printTempChangeOver(report,data)+"\n\n"+FormatReport.content;
		if (report.reportType.equals("PNP Performance")){
			Map pnpData = printPNPSummary(report,reportColumns,data); 
			String result = (String)pnpData.get("content")+content;
			content = result;
			file = (File)pnpData.get("file");
		}
		if (report.reportType.equals("Enabled Site Performance")){
			data = (Hashtable)printSitePerformance(report,data); 
			String result = (String)data.get("content")+content;
			content = result;
		}
		EmailServer.send(emailServer,fromUser,fromAddress,report.getToEmails(),report.getCcEmails(),subject,content,file);
		}

	/**
	 * Returns a hash table with the accumulate duration of all events for each machine
	 * @param data The report data
	 * @param ignoreEmptyGroups Whether to ignore rows with empty groups to calculate totals
	 * @return The hash table with the durations
	 * @throws Throwable
	 */
	private static Hashtable getTotals(Hashtable data, boolean ignoreEmptyGroups)
		{
		Hashtable totals = new Hashtable();
		Enumeration enumeration = data.keys();
		while (enumeration.hasMoreElements())
			{
	    	String categories = (String)enumeration.nextElement();
	    	float value = ((Float)((List)data.get(categories)).get(0)).floatValue();
	    	String category = categories.split("\t")[0];
	    	// If ignoreEmptyGroups and group is empty, do not add it to the totals
	    	if (!ignoreEmptyGroups || !categories.split("\t")[1].trim().equals(""))
	    		{
		    	Float stored = (Float)totals.get(category);
		    	if (stored == null) totals.put(category,new Float(value));
		    	else totals.put(category,new Float(stored.floatValue()+value));
		    	}
	    	}
		return totals;
		}
	
	private static Hashtable getTotalsBy(Hashtable data,Vector<Integer> indexs)
	   	{
		
		Hashtable totals= new Hashtable();
		Enumeration keys = data.keys();
		String categories;//the fields that make the category separated by \t
		String criteria[];//the fields from categories to consider in the totals 
		while(keys.hasMoreElements())
		    {
			categories = (String)keys.nextElement();
			criteria=categories.split("\t");
			String category="";
			Float value = (Float)((List)data.get(categories)).get(0);
			for(int i=0;i< indexs.size();i++)
			   {
				category+=criteria[indexs.get(i)]+"\t";
			   }
			Float stored=(Float)totals.get(category);
			if(stored ==null)totals.put(category, value);
			else totals.put(category,stored + value);
		    }
		return totals;
		}
	 
	private static Hashtable addTotalsBy(Hashtable data,Vector<Integer> indexs,boolean addPercent){
		//preconditions
		if(indexs==null)return null;
		Hashtable totals = getTotalsBy(data, indexs);
		Enumeration totalK = totals.keys();
		//iterate through all the totals
		while(totalK.hasMoreElements())
		   {
			String totalCategory = (String)totalK.nextElement();
			String []totalCols = totalCategory.split("\t");
			if(totalCols.length != indexs.size() )break;
			// for each total add it to the data where the indexes match
			Enumeration dataK = data.keys();
			String categories;
			String[] criteria;
			while(dataK.hasMoreElements())
			   {
				categories = (String)dataK.nextElement();
				List values = (List)data.get(categories);
				criteria=categories.split("\t");
				boolean add=false;
				for(int i=0;i < totalCols.length;i++ )
				   {
					if(totalCols[i].equals(criteria[indexs.get(i)]))add=true;
					else {add=false;break;}
				   }
				if(add)
				   {
					float total=0.0f;
					try{
					total= (Float)totals.get(totalCategory);
					}catch(Exception e){}
					//add the new total to the vector
					values.add(total);
					if(addPercent){
						Float percent = (Float)values.get(0)/total;
						percent *=100;
						values.add(percent);
					}
					//update the vector in the data hashtable
					data.put(categories, values);
				   }
			   }
		   }
		return data;
	}
	
	private static Map printSitePerformance(Report report, Map data)
		{
		Float totalJ750DevicesProcessed = 0F;
		Float totalUflexDevicesProcessed = 0F;
		StringBuilder content = new StringBuilder();
		content.append(title(report, true)+"\n");
		List<String> keys = new ArrayList<String>(data.keySet());
		Set<String> countSet = new HashSet<String>();
		Map<String, Float[]> resultsByLocation = new TreeMap<String, Float[]>();
		Map<String, Float[]> resultsByType = new TreeMap<String, Float[]>();
		Map<String, Float[]> resultsByHandler = new TreeMap<String, Float[]>();
		Map<String, String> includedKeysByType = new HashMap<String, String>();
		Map<String, Float> deviceProcessedByLocation = new HashMap<String, Float>();
		Map<String[], String> deviceNameChangeOverByLoc = new HashMap<String[], String>();
		Map<String[], String> deviceNameChangeOverByType = new HashMap<String[], String>();
		Map<String[], String> deviceNameChangeOverByHandler = new HashMap<String[], String>();
		Map<String[], String> temperatureChangeOverByLoc = new HashMap<String[], String>();
		Map<String[], String> temperatureChangeOverByType = new HashMap<String[], String>();
		Map<String[], String> temperatureChangeOverByHandler = new HashMap<String[], String>();
		Map<String, String> includedKeysByHandler = new HashMap<String, String>();
		includedKeysByHandler.put("MX", "MX");
		for (String key : keys)
			{
			String[] keyArr = key.split("\t");
			String location = keyArr[0];
			String mid = keyArr[1];
			String className = keyArr[2];
			String handleId = keyArr[5];
			Float deviceProcessed = Float.parseFloat(keyArr[11]);
			String deviceName = keyArr[8];
			String temperature = keyArr[9];
			List<Float> values = (List<Float>) data.get(key);
			String platform = keyArr[12];
			
			if (totalJ750DevicesProcessed == 0F
					|| totalUflexDevicesProcessed == 0F) {// get total of Device
															// Processed and each Location
				for (String val : keys) {
					String[] valArr = val.split("\t");
					includedKeysByType.put(valArr[12], valArr[12]);
					if (valArr[12].equals("J750")) {
						Float j750DeviceProcessed = Float
								.parseFloat(valArr[11]);
						totalJ750DevicesProcessed = (totalJ750DevicesProcessed + j750DeviceProcessed);
						Float currentSum = deviceProcessedByLocation
								.get(valArr[0]);
						deviceProcessedByLocation.put(valArr[0],
								(currentSum == null) ? j750DeviceProcessed
										: currentSum + j750DeviceProcessed);

					} else if (valArr[12].equals("UFLEXB")) {
						Float uFlexDeviceProcessed = Float
								.parseFloat(valArr[11]);
						totalUflexDevicesProcessed = (totalUflexDevicesProcessed + uFlexDeviceProcessed);
						Float currentSum = deviceProcessedByLocation
								.get(valArr[0]);
						deviceProcessedByLocation.put(valArr[0],
								(currentSum == null) ? uFlexDeviceProcessed
										: currentSum + uFlexDeviceProcessed);
					}
				}
			}
			
			deviceNameChangeOverByLoc.put(keyArr, location);
			deviceNameChangeOverByType.put(keyArr, className);
			deviceNameChangeOverByHandler.put(keyArr, handleId);
			temperatureChangeOverByLoc.put(keyArr, location);
			temperatureChangeOverByType.put(keyArr, className);
			temperatureChangeOverByHandler.put(keyArr, handleId);
			values.add(deviceProcessed);
			
			Float totalCount=(Float)deviceProcessedByLocation.get(location);
			values.add(totalCount);
			groupDataByKeyValue(resultsByLocation,location,values,null,countSet,mid,deviceNameChangeOverByLoc,temperatureChangeOverByLoc);
			
			if(platform.equals("J750")){
				values.set(6,totalJ750DevicesProcessed);
			}else if(platform.equals("UFLEXB")){
				values.set(6,totalUflexDevicesProcessed);
			}
			groupDataByKeyValue(resultsByType,platform,values,includedKeysByType,deviceNameChangeOverByType,temperatureChangeOverByType);
			groupDataByKeyValue(resultsByHandler,handleId,values,includedKeysByHandler,deviceNameChangeOverByHandler,temperatureChangeOverByHandler);
			}
		
		String title="\nTYPE                	Site Performance (%)		1st Pass Bin8(%)\n\n";
		generateSitePerfEmailContent(content,  resultsByType, title, false, deviceNameChangeOverByType, temperatureChangeOverByType);
		
		title="\nHANDLER\n\n";
		generateSitePerfEmailContent(content,  resultsByHandler, title, false, deviceNameChangeOverByHandler, temperatureChangeOverByHandler);
		content.append("------------------------------------------------------------------------------------------------------------------------------------------------\n");
		
		title="\nLocation                   Site Performance (%)		1st Pass Bin8(%)	Temp Change		Device Change\n\n";
		generateSitePerfEmailContent(content,  resultsByLocation, title, true, deviceNameChangeOverByLoc, temperatureChangeOverByLoc);
		
		data.put("content", content.toString());
		return data;
		}
	
	private static String[] splitByNumber(String text, int number) {

        int inLength = text.length();
        int arLength = inLength / number;
        int left=inLength%number;
        if(left>0){++arLength;}
        String ar[] = new String[arLength];
            String tempText=text;
            for (int x = 0; x < arLength; ++x) {

                if(tempText.length()>number){
                ar[x]=tempText.substring(0, number);
                tempText=tempText.substring(number);
                }else{
                    ar[x]=tempText;
                }
            }
        return ar;
    }
	
	private static void generateSitePerfEmailContent(StringBuilder content, Map<String, Float[]> results, String title,boolean enableCount, Map<String[], String> deviceNameChangeOver, Map<String[], String> temperatureChangeOver)
		{
		Float percent = 0F;
		BigDecimal bin8Percent = BigDecimal.ZERO;
		List<String> keys = new ArrayList<String>(results.keySet());
		content.append(title);
		BigDecimal hundred = new BigDecimal(100);
		
		for (String key : keys)
			{
			Integer deviceCount = 0;
			Integer tempCount = 0;
			Float[] value = results.get(key);
			if(key.length()>10){
				String ar[]=splitByNumber(key,10);
				if(ar.length>0){
					key=ar[0].concat("\n").concat(ar[1]);
				}
			}

			content.append(key + ":");
			if(enableCount)content.append(value[value.length-1]);
			if(key.equals("UFLEXB")){
				content.append("	");
			}else{
				content.append("		");
			}
			if(value[0]==0)
				{
				percent = 0F;
				}
			else
				{
				Float totalPercentage = value[6];
				percent = totalPercentage*100;
				percent=Float.parseFloat(String.format("%.2f", percent));
				}
			if(value[3]==0 && value[5]==0)
				{
				bin8Percent = BigDecimal.ZERO;
				}
			else
				{
				bin8Percent = new BigDecimal(value[3]).divide(new BigDecimal(value[5]), 4, RoundingMode.HALF_UP).multiply(hundred);
				}
			
			Map<String, String> distinctDev = new HashMap<String, String>();
	        for(Map.Entry<String[], String> en: deviceNameChangeOver.entrySet()){
	        	if(en.getValue().contains(key.replace("\n", ""))){
	        		String[] result = en.getKey();
	        		distinctDev.put(result[8], key.replace("\n", ""));
	            }
	        }
	        
	        Map<String, String> distinctTemp = new HashMap<String, String>();
	        for(Map.Entry<String[], String> en: temperatureChangeOver.entrySet()){
	            if(en.getValue().contains(key.replace("\n", ""))){
	            	String[] result = en.getKey();
	            	distinctTemp.put(result[9], key.replace("\n", ""));
	            }
	        }
			
			if(title.contains("Location")){
				content.append(""+percent + "%");
				content.append("			"+bin8Percent.doubleValue() + "%");
				content.append("			"+distinctTemp.size());
				content.append("			"+distinctDev.size());
			}else {
				content.append("	"+percent + "%");
				content.append("				"+bin8Percent.doubleValue() + "%");
			}
			content.append("\n\n");
			}
		}
	
	/**
	 * retrieve records and sum them up by key
	 * @param resultMap result Map
	 * @param key the key that need to be grouped by
	 * @param values summing values, the last value will be the count of total distinct countKey
	 * @param countSet set utility to calculate the total count
	 * @param countKey count 
	 */
	private static void groupDataByKeyValue(Map<String, Float[]> resultMap, String value, List<Float> values, Map<String, String> includedKeys, Set<String> countSet, String countKey, Map<String[], String> deviceName, Map<String[], String> temperature)
		{
		String key = null;
		if (includedKeys != null)
			{
			List<String> prefixs = new ArrayList<String>(includedKeys.keySet());

			for (String prefix : prefixs)
				{
				if (value != null && value.startsWith(prefix))
					{
					key = includedKeys.get(prefix);
					continue;
					}
				}
			if (key == null) return;
			}
		else
			{
			key = value;
			}

		Float[] resultValues = null;
		if (resultMap.containsKey(key))
			{
			resultValues = resultMap.get(key);
			}
		else
			{
			resultValues = new Float[values.size() + 1];
			for (int i = 0; i < resultValues.length; i++)
				{
				resultValues[i] = Float.valueOf(0);
				}
			}
		if (countSet != null && !countSet.contains(key + "\t" + countKey)) {
			countSet.add(key + "\t" + countKey);
			resultValues[resultValues.length - 1] = resultValues[resultValues.length - 1] + 1;
		}

		Float valArr[] = values.toArray(new Float[values.size()]);
		BigDecimal sitePerformancePercentage = BigDecimal.ZERO;
		BigDecimal enableSiteCount = new BigDecimal(valArr[2]);
		sitePerformancePercentage = new BigDecimal(valArr[5]).divide(
				new BigDecimal(valArr[6]), 7, RoundingMode.HALF_UP).multiply(
				enableSiteCount);

		valArr[6] = sitePerformancePercentage.floatValue();
		values = Arrays.asList(valArr);

		for (int i = 0; i < values.size(); i++) {
			resultValues[i] = resultValues[i] + values.get(i).floatValue();
		}
		resultMap.put(key, resultValues);
		}

	/**
	 * retrieve records and sum them up by key
	 * @param resultMap result Map
	 * @param key the key that need to be grouped by
	 * @param values summing values
	 */
	private static void groupDataByKeyValue(Map<String, Float[]> resultMap, String value, List<Float> values, Map<String, String> includedKeys, Map<String[], String> deviceName, Map<String[], String> temperature)
		{
		groupDataByKeyValue(resultMap, value, values, includedKeys,null,null, deviceName,temperature);
		}
	
	private static String printTempChangeOver(Report report, Map data) {
		StringBuilder content = new StringBuilder();
		List<String> distinctVal = new ArrayList();
		Map<String, Integer> tempChangeCount = new HashMap<String, Integer>();
		Map<String, List<String>> tempListComplete = new HashMap<String, List<String>>();
		TreeMap<String, List<String>> tempListOrderByDesc = new TreeMap<String, List<String>>();
		content.append(title(report, true) + "\n");
		String result = "   " + title(report, true) + "\n\n";
		
		List keys = new Vector(data.keySet());
		for (int i = 0; i < keys.size(); i++) {
			String key = (String) keys.get(i);
			List values = (List) data.get(key);
			distinctVal = new ArrayList();
			for (int j = 0; j < values.size(); j++) {
				String actualTemp = (String) values.get(j);
				String[] aux = actualTemp.split("\t", -1);
				String prevTemp = getTemperatureType(Double
						.valueOf((String) aux[0]));
				if (j + 1 < values.size() || values.size() == j) {
					String nextTemp = (String) values.get(j + 1);
					aux = nextTemp.split("\t", -1);
					if (!actualTemp.equals(nextTemp)) {
						nextTemp = getTemperatureType(Double
								.valueOf((String) aux[0]));
						distinctVal.add(prevTemp + " - " + nextTemp);
					}
				}
			}
			tempChangeCount = new HashMap<String, Integer>();
			for (String temp : distinctVal) {
				Integer count = tempChangeCount.get(temp);
				tempChangeCount.put(temp, (count == null) ? 1 : count + 1);
			}
			List<String> tempTypeVal = new ArrayList();
			for (Iterator<String> iterator = tempChangeCount.keySet().iterator(); iterator
					.hasNext();) {
				String keyVal = iterator.next().toString();
				Integer value = tempChangeCount.get(keyVal);
				tempTypeVal.add(keyVal + " : " + value.toString());
			}
			if (!tempTypeVal.isEmpty()){tempListComplete.put(key, tempTypeVal);}
		}
		
		tempListOrderByDesc.putAll(tempListComplete);
		tempListOrderByDesc.descendingMap();
		for (Iterator<String> iterator = tempListOrderByDesc.keySet().iterator(); iterator
				.hasNext();) {
			String keyVal = iterator.next().toString();
			List vlist = tempListOrderByDesc.get(keyVal);
			for (int j = 0; j < vlist.size(); j++) {
				result += "   " + keyVal + ": " + vlist.get(j) + "\n";
			}
		}
		return result;
	}
	
	private static String getTemperatureType(Double temp){
		String tempType = "NONE";
		Double roomTemp = 25.0;
		Double hotTemp = 200.0;
		Double coldTemp = -60.0;
		if(temp > roomTemp && temp <= hotTemp){tempType="Hot";}
		if(temp >= coldTemp && temp < roomTemp){tempType="Cold";}
		if(temp.equals(roomTemp)){tempType="Room";}
		return tempType;
	}

	/**
	 * Returns a hash table with summary information
	 * @param data The report data
	 * @return The hash table with the summary
	 */
	private static String printSummary(Report report, Hashtable data)
		{
		int totalCount = 0;
		Hashtable summary = new Hashtable();
		List keys = new Vector(data.keySet());
		for (int i=0; i<keys.size(); i++)
			{
			String key = (String)keys.get(i);
			List values = (List)data.get(key);
			for (int j=0; j<values.size(); j++) 
  				{
				String[] aux = ((String)values.get(j)).split("\t");
				String location = key;
				Integer count = (Integer)summary.get(location);
				if (count == null) count = new Integer(1);
				else count = new Integer(count.intValue()+1);
				summary.put(location,count);
				String type = "Handler "+ProcessRecord.handlerType(aux[2]);
				count = (Integer)summary.get(type);
				if (count == null) count = new Integer(1);
				else count = new Integer(count.intValue()+1);
				summary.put(type,count);
				totalCount++;
  				}
			}
		String result = "   "+title(report,true)+"\n\n"+"   Total count: "+totalCount+"\n";
		keys = new Vector(summary.keySet());
		Collections.sort(keys);
		for (int i=0; i<keys.size(); i++)
			{
			String key = (String)keys.get(i);
			result += "   "+key+": "+summary.get(key)+"\n";
			}
		return result;	
		}
	
	private static Map getMetricsByHandler(String handlerType,Map data,int pnpHeadColumnIndex)
	{
	Map metrics = new HashMap();
	Map<String,Integer> summary = new Hashtable<String,Integer>();
	Map pnpAverage = new Hashtable();
	Map endOfShift = new Hashtable();
	int totalHandlerCount = 0;
	List<String> keys = new Vector(data.keySet());
	for (String key : keys)
		{
		List<String> values = (List) data.get(key);
		Integer totalPnpHead = 0;

		for (String value: values )
			{
			String[] aux = value.split("\t");
			String location = key;

			List eorList = new ArrayList();
			eorList.add(key);
			eorList.addAll(Arrays.asList(aux));
			aux = (String[]) eorList.toArray(new String[eorList.size()]);
			String handler = ProcessRecord.handlerType(aux[2]);

			Integer pnpHeadAvailable = Integer.valueOf(0);
			try{pnpHeadAvailable = Integer.parseInt(aux[pnpHeadColumnIndex]);} 
			catch(NumberFormatException ne){} 
			catch(ArrayIndexOutOfBoundsException ae){}
			
			
			totalPnpHead = totalPnpHead + pnpHeadAvailable;
			pnpAverage.put(location, totalPnpHead);
			Integer count;
			if (handler.equals(handlerType))
				{
				count = summary.get(location);
				if (count == null) count = Integer.valueOf(1);
				else count = Integer.valueOf(count.intValue() + 1);
				summary.put(location, count);
				}

			String mid = aux[1];
			endOfShift.put(mid, eorList);

			count = (Integer) pnpAverage.get(mid);
			if (count == null && handler.equals(handlerType))
				{
				totalHandlerCount++;
				}
			pnpAverage.put(mid, totalHandlerCount);
			summary.put(handler, totalHandlerCount);
			}
		}
	metrics.put("summary", summary);
	metrics.put("pnpAverage", pnpAverage);
	metrics.put("endOfShift", endOfShift);
	metrics.put("totalHandlerCount", totalHandlerCount);
	return metrics;
	}
	
	private static String getSummaryByHandle(Map summary,Map pnpAverage)//,String handleType)
	{
	String result = "";
	List<String> keys = new Vector(summary.keySet());
	Integer totalHandler = 8; // default handler is 8
	Collections.sort(keys);

	for (String key : keys)
		{
		boolean isLocation = !key.contains("Castle") && !key.contains("UTS") && !key.contains("Matrix"); 
		if (isLocation)
			{
			double pnpCount = (Integer) pnpAverage.get(key);
			double locIDcount = (Integer)summary.get(key);
			double averagePNP = ((((double) pnpCount / locIDcount) / totalHandler * 100));
			BigDecimal bd = new BigDecimal(averagePNP).setScale(2, RoundingMode.HALF_UP);
			averagePNP = bd.doubleValue();
			result +=  String.format(" %s  : %5d \t\t %6.2f \n", key,summary.get(key), averagePNP); 
			
			}
		}
	return result;
	}
	
	/**
	 * Returns a Map with PNP summary information
	 * @param data The report data
	 * @return The Map with the PNP summary
	 * @throws Throwable 
	 */
	private static Map printPNPSummary(Report report, String[] reportColumns, Map data) throws Throwable
		{
		int totalHandlerCount = 0;
		String castle = "Castle",matrix = "Matrix";
		int idxPNPAvailable = 5, idxIPNPAvailable = 14, idxOPNPAvailable = 23, idxTPNPAvailable = 32;
		Map<String,Integer> summary = new Hashtable<String,Integer>();
		Map pnpAverage, endOfShift = new Hashtable(); 
		Map content = new Hashtable();
		
		//Castle
		Map castleMetrics = getMetricsByHandler(castle, data,idxPNPAvailable);
        pnpAverage = (Map)castleMetrics.get("pnpAverage");
        summary = (Map)castleMetrics.get("summary");
        endOfShift.putAll((Map)castleMetrics.get("endOfShift"));
        totalHandlerCount = (Integer)castleMetrics.get("totalHandlerCount");
		String result = "   " + title(report, true) + "\n\n" + castle +" Handler Count: "	+ totalHandlerCount + "        Average PNP Performance(%)\n";
		result += getSummaryByHandle(summary,pnpAverage);
		//Matrix
		//IPNP
		Map matrixMetrics = getMetricsByHandler(matrix, data,idxIPNPAvailable);
        pnpAverage = (Map)matrixMetrics.get("pnpAverage");
        summary = (Map)matrixMetrics.get("summary");
        totalHandlerCount = (Integer)matrixMetrics.get("totalHandlerCount");
		result += "   " +"\n\n" + matrix + "  Handler Count: "	+ totalHandlerCount + "        Average PNP Performance(%)\n";
		result += "\n IPNP : \n"+getSummaryByHandle(summary,pnpAverage);
		//OPNP
		matrixMetrics = getMetricsByHandler(matrix, data,idxOPNPAvailable);
        pnpAverage = (Map)matrixMetrics.get("pnpAverage");
        summary = (Map)matrixMetrics.get("summary");
        totalHandlerCount = (Integer)matrixMetrics.get("totalHandlerCount");
		result += "\n OPNP : \n" + getSummaryByHandle(summary,pnpAverage);
		//TPNP
		matrixMetrics = getMetricsByHandler(matrix, data,idxTPNPAvailable);
        pnpAverage = (Map)matrixMetrics.get("pnpAverage");
        summary = (Map)matrixMetrics.get("summary");
        totalHandlerCount = (Integer)matrixMetrics.get("totalHandlerCount");
		result += "\n TPNP :\n" + getSummaryByHandle(summary,pnpAverage);
		
		File lastRec = generateFileWithLastPNPRecord(report, reportColumns, endOfShift);
		content.put("content", result);
		content.put("file", lastRec);
		return content;
		}
	
	private static File generateFileWithLastPNPRecord(Report report, String[] reportColumns, Map data) throws Throwable
		{
		WritableWorkbook workbook = null;
		OutputStream outputStream = null;
		try
			{
			File file = null;
			file = publish(report, ".xls", reportsDirectory);
			outputStream = new FileOutputStream(file);

			workbook = Workbook.createWorkbook(outputStream);
			workbook.createSheet("Report", 0);
			WritableSheet sheet = workbook.getSheet(0);
			sheet.addCell(new Label(0, 0, title(report, true)));

			for (int i = 3; i < reportColumns.length; i++)
				sheet.addCell(new Label(i - 3, 1, deCapitalize(reportColumns[i])));
			List categories = new Vector(data.keySet());
			Collections.sort(categories);
			int row = 2;
			for (Object category : categories)
				{
				List values = (List) data.get(category);
				for (int j = 0; j < values.size(); j++)
					{
					String[] aux = ((String) values.get(j)).split("\t");
					for (int k = 0; k < aux.length; k++)
						sheet.addCell(new Label(j, row, aux[k]));
					}
				row++;
				}
			workbook.write();
			workbook.close();
			workbook = null;

			return file;

			}
		catch (Throwable th)
			{
			throw th;
			}
		}
	
	
	/**
	 * Convert a dimension of a 2D array into a string like [1,2,3]
	 * @param result The 2D String array
	 * @param dimension The dimension to output (1 or 2)
	 * @param pivot The pivot for the other dimension
	 * @param isText Whether to use commas to delimite items
	 * @return
	 */
	private static String toString(String[][] result, int dimension, int pivot, int start,  boolean isText)
		{
		StringBuffer buffer = new StringBuffer();
		buffer.append("[");
		int size = dimension==1?result.length:result[0].length;
		for (int i=start; i<size; i++)
			{
			String aux = dimension==1?result[i][pivot]:result[pivot][i];
			if (isText) buffer.append("'"+aux+"',");
			else buffer.append(aux+",");
			}
		if (size>1) buffer.deleteCharAt(buffer.length()-1);
		buffer.append("]");
		return buffer.toString();
		}
	public static String getEmailServer()
		{
		return emailServer;
		}
	public static void setEmailServer(String emailServer)
		{
		FormatReport.emailServer = emailServer;
		}
	public static String getFromUser()
		{
		return fromUser;
		}
	public static void setFromUser(String fromUser)
		{
		FormatReport.fromUser = fromUser;
		}
	public static String getFromAddress()
		{
		return fromAddress;
		}
	public static void setFromAddress(String fromAddress)
		{
		FormatReport.fromAddress = fromAddress;
		}
	public static String getContent()
		{
		return content;
		}
	public static void setContent(String content)
		{
		FormatReport.content = content;
		}
	public static String getDomain()
		{
		return domain;
		}
	public static void setDomain(String domain)
		{
		FormatReport.domain = domain;
		}
	}