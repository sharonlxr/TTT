package com.freescale.stateStamper.model.logic;

import com.freescale.stateStamper.db.Map2Json;
import com.freescale.stateStamper.model.entity.*;

import java.io.*;
import java.util.*;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;

import com.freescale.serverUtil.PersistenceBrokerIterator;
import com.freescale.serverUtil.Persistence;
import com.freescale.serverUtil.ThreadSafeSimpleDateFormat;

/**
 * This class generates reports requested by the GUI and publishes them as well
 */

public class GenerateReport
	{
	private static final ThreadSafeSimpleDateFormat formatter = new ThreadSafeSimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private static Hashtable reportDefinition= new Hashtable();
	private static Hashtable durableReportDefinition= new Hashtable();
	private static Hashtable realTimeDurableReportDefinition= new Hashtable();
	private String reportsDirectory,transferDirectory,baseDirectory;
	private Vector reportTypes = new Vector();
	private Vector durableReportTypes = new Vector();
	private Vector realTimeDurableReportTypes = new Vector();
	private List<Report> scheduledReports;
	private Report[] reports = new Report[10];
	private int index = 0, writeInterval;
	private String attrib[];
	private static int TIME_INDEX =1;
	private Logger logger;
	Map<String,List<String>> dbDates;
	
	private static final  String[] defaultAttributes = { "type", "serial_number", "platform", "eIName", "children" };
	private static final String[] defaultDurableAttributes = { "STATUS_PK", "DURABLE_ID", "TIME", "EQUIPMENT_PK", "EVENT_PK",
			"OPERATOR_ID", "COMMENTS", "workstation", "location", "STATE", "SUBSTATE", "ATTRIBUTES" };
	
	/**
	 * Constructs a Report object that will be used to generate and publish reports
	 * @param reportsDirectory The directory where reports will be published
	 * @param scheduledReports A vector of Report objects that are scheduled to be generated and published 
	 * @throws Throwable
	 */
	public GenerateReport(String reportsDirectory,String transferDirectory,String baseDir,List scheduledReports, int writeInterval) throws Throwable
		{
		this.reportsDirectory = reportsDirectory;
		this.transferDirectory = transferDirectory;
		baseDirectory = baseDir;
		this.scheduledReports = scheduledReports;
		this.writeInterval = writeInterval;
		logger = LoggerFactory.getLogger("stateStamper");
		initReportDefinitions();
		}
	
	private List readReportDefinitions(String fileName)throws IOException
		{
		List res = new ArrayList();
		Hashtable repDefinition=new Hashtable();
		List repTypes= new Vector();
		BufferedReader bufferedReader = new BufferedReader(new FileReader(baseDirectory+"/classes/"+ getClass().getPackage().getName().replace(".","/") +"/"+fileName));
		String line;
		try{
			// First line is header. Ignore it
			line = bufferedReader.readLine();
			while((line = bufferedReader.readLine()) != null)
				{
				if(line.isEmpty())
					{
					logger.warn("Empty line found in report definition in "+ fileName + " ignoring the definition");
					continue;
					}
				String[] values = line.split("\t");
				if(values.length < 2 )
					{
					logger.warn("Incorrect report definition in "+ fileName + " ignoring the definition:"+ line );
					continue;
					}
				String[] columns = new String[values.length-2];
				// First 2 items are report type and grouping, ignore them
				for (int i=2; i<values.length; i++) columns[i-2] = values[i].trim();
				repDefinition.put(values[0]+"\t"+values[1],columns);
				repTypes.add(values[0]+"\t"+values[1]);
				}
			bufferedReader.close();
			} catch (IOException e){logger.error(e.getMessage());}
			res.add(repDefinition);
			res.add(repTypes);
		return res;
		}
	
	public void initReportDefinitions()throws IOException
		{
		List definitions=  readReportDefinitions("reports.txt");
		reportDefinition = (Hashtable)definitions.get(0);
		reportTypes =  (Vector)definitions.get(1);
		
		definitions = readReportDefinitions("durable_reports.txt");
		durableReportDefinition =  (Hashtable)definitions.get(0);
		durableReportTypes = (Vector)definitions.get(1);
	
		definitions = readReportDefinitions("realtime_durable_reports.txt");
		realTimeDurableReportDefinition =  (Hashtable)definitions.get(0);
		realTimeDurableReportTypes = (Vector)definitions.get(1);	
		}
	
	/**
	 * Generates a report for durable 
	 * @param report A report object with the settings for this report
	 * @param outputStream Output stream to write the report to
	 * @param publish Whether to publish the report or not
	 * @param endOfShift Whether it is the end of shift or not
	 * @return A hashtable with the report data
	 * @throws Throwable
	 */
	public Hashtable generateDurableReport(Report report, HttpServletResponse response, boolean publish, boolean endOfShift) throws Throwable
		{
		int excludedcolumns = 3;
		boolean realtime = false;
		boolean durable = false;
		String[] equal = null, greater = null, less = null, like = null, order = null, in = null, grouping = null;
		String outer = null, period = null;
		Hashtable columnIndex = new Hashtable(), data = new Hashtable();
		boolean all = report.machines == null || report.machines.size() == 0 || Boolean.TRUE.equals(report.includeAll);
		boolean raw = report.period.equals("Raw");
		realtime = report.period.equalsIgnoreCase("Realtime");
		boolean isAternativeDisplay = report.isAternativeDisplay();
		if (report.getEquipmentType() == null || report.getEquipmentType().name == null
				|| report.getEquipmentType().name == ""
				|| "Tester".equalsIgnoreCase(report.getEquipmentType().name))
			{
			durable = false;
			}
		else
			{
			durable = true;
			}
		if (report.reportType.equalsIgnoreCase("RealtimeInventory"))
			{
			durable = true;
			}
		Hashtable repDefinition = null;
		if (durable)
			{

			if (realtime)
				{
				repDefinition = realTimeDurableReportDefinition;
				}
			else
				{
				repDefinition = durableReportDefinition;
				}
			}
		else
			{
			repDefinition = reportDefinition;
			}
		String[] reportColumns = (String[]) repDefinition.get(report.reportType + "\t" + report.grouping);
		report.chartType = reportColumns[0];
		report.totals = reportColumns[1];
		report.condition = reportColumns[2];
		report.setOrderBy(reportColumns[3]);// in a format attribute,true
		if (reportColumns[4] != null && !reportColumns[4].equals("") && !reportColumns[4].equals("NONE"))
			{
			report.filterlist = reportColumns[4].split(",");
			}

		Map filterHashTable = report.getFilters();
		String startDate = null, endDate = null; // startDate and endDate are
													// only used when period =
													// "Raw"

		long count = 0;
		if (report.getOrderBy() == null || report.getOrderBy().equals(""))
			{
			order = new String[] { "STATUS_PK", Boolean.toString(true) };
			}
		else
			{
			order = report.getOrderBy().split(",");
			}
		if (report.period != null)
			{
			period = report.period.substring(0, 1);
			}

		Class table = null;
		if (!durable)
			{
			table = Status.class;
			equal = new String[] { "PERIOD", period };
			List lAttr = report.getEquipmentType().getAttributes();
			int size = Status.DEFAULT_STATUS.length + (lAttr != null ? lAttr.size() : 0);
			attrib = new String[size];
			for (int i = 0; i < Status.DEFAULT_STATUS.length; i++)
				{
				attrib[i] = Status.DEFAULT_STATUS[i];
				}
			for (int i = 0; lAttr != null && i < lAttr.size(); i++)
				{
				attrib[i + Status.DEFAULT_STATUS.length] = ((EquipmentAttribute) lAttr.get(i)).name;
				}

			if (raw)
				{
				Date auxDate = report.startDate;
				// If the report is a state transition report, move the start
				// time to writeInterval seconds earlier to include the last
				// event before the start time
				if (report.chartType.equals("raw"))
					auxDate = new Date(report.startDate.getTime() - writeInterval * 1000L);
				startDate = formatter.format(auxDate);
				endDate = formatter.format(report.endDate);
				greater = new String[] { "TIME", startDate };
				less = new String[] { "TIME", endDate };
				}

			List inList = new ArrayList();
			if (!all)
				{
				if (durable)
					{
					inList.add("DURABLE_ID");
					inList.add(report.getMachinesAsString());
					}
				else
					{
					inList.add("MID");
					inList.add(report.getMachinesAsString());
					}
				}
			if (!(raw) && !(realtime))
				{
				inList.add("TIME");
				inList.add(report.getDatesAsString());
				}
			if (inList.size() > 0)
				{
				in = (String[]) inList.toArray(new String[0]);
				}
			}
		else
			{
			durable = true;
			
			// always put State, Substate and Attributes in the end for easy
			// processing
			Set attributes = new LinkedHashSet();
			if (realtime)
				{
				table = CurrentDurableStatus.class;
				excludedcolumns = 7;
				attributes.addAll(Arrays.asList(defaultDurableAttributes));
				attributes.addAll(Arrays.asList(defaultAttributes));
				Vector durableAtrributes = new Vector();
				for (int i = excludedcolumns; i < reportColumns.length; i++)
					{
					attributes.add(reportColumns[i]);
					}
				attrib = getFunctionColumns(attributes);
				}
			else
				{
				table = DurableStatus.class;
				excludedcolumns = 7;
				attrib = defaultDurableAttributes;
				if (report.startDate != null)
					{
					startDate = formatter.format(report.startDate);
					greater = new String[] { "TIME", startDate };
					}
				if (report.endDate != null)
					{
					endDate = formatter.format(report.endDate);
					less = new String[] { "TIME", endDate };
					}
				List inList = new ArrayList();
				if (!all)
					{
					if (durable)
						{
						inList.add("DURABLE_ID");
						inList.add(report.getMachinesAsString());
						}
					else
						{
						inList.add("MID");
						inList.add(report.getMachinesAsString());
						}
					}
				if (inList.size() > 0)
					{
					in = (String[]) inList.toArray(new String[0]);
					}
				}
			}

		Iterator it = null;
		try
			{
			if (!durable) it = Persistence.iSearch(table, attrib, equal, greater, less, like, in, grouping, order,
					outer);
			else if (realtime)
				{
				String[] orderArray = report.getOrderBy().split(",");
				if (filterHashTable == null) filterHashTable = new Hashtable();
				Vector vetorofArray = DurableManager.getDurablesForReport(report.getEquipmentType(),report.getArea(),
						filterHashTable, orderArray[0], Boolean.valueOf(orderArray[1]), attrib);
				it = vetorofArray.iterator();
				}
			else
				{
				it = Persistence.iSearch(table, attrib, equal, greater, less, like, in, grouping, order, outer);
				attrib = getCompleteStatusAttributeforDurable(report.getEquipmentType(), attrib);
				Vector vectorofArray = getDurableHistoricalReportData(it, report, attrib);
				it = vectorofArray.iterator();
				}
			int k;
			for (k = 0; k < attrib.length; k++)
				{
				columnIndex.put(attrib[k], new Integer(k));				
				EquipmentType eType = report.getEquipmentType();
				if (eType == null) continue;
				EquipmentAttribute attribute = eType.getAttribute(attrib[k]);
				if (attribute == null) continue;
				String alia = attribute.getAttributeAlias();
				if (alia!=null&& !alia.equals(""))
					{
						columnIndex.put(alia, new Integer(k));
					}
				} // Read column headers
			columnIndex.put("AREA_CODE", new Integer(k++));
			columnIndex.put("PLANT_CODE", new Integer(k++));
			columnIndex.put("STAGE", new Integer(k++));
			Object[] res = null;
			String record;
			String date = null;
			for (count = 0; it != null && it.hasNext(); count++)
				{
				res = (Object[]) it.next();
				record = "";
				for (int j = 0; res != null && j < res.length; j++)
					{
					record += res[j] != null ? res[j] + "\t" : "\t";
					if (j == TIME_INDEX)
						{// Time is processed independently of the record in
							// date field
						if (!durable)
							{
							if (!raw)
								{
								date = res[j].toString();

								}
							else
								{// raw data is grouped by shift
								FiscalDate fd = new FiscalDate("Shift", res[j].toString());
								date = fd.fDate + "-" + fd.shift;
								}
							}
						else
							{
							date = res[j].toString();
							}
						}
					}
				record += StateStamper.getAreaCode() != null ? StateStamper.getAreaCode() + "\t" : "\t";
				record += StateStamper.getPlantCode()!= null ? StateStamper.getPlantCode() + "\t" : "\t";
				record += StateStamper.getStage() != null ? StateStamper.getStage() + "\t" : "\t";
				boolean isText = report.chartType.equals("text");
				if(filterRecordsByArea(report, durable, columnIndex, record, date))
					{
						if (condition(report.condition, record, date, columnIndex))
						{
						ProcessRecord.process(data, reportColumns, record, date, columnIndex, count, excludedcolumns,isText);
						}
					}
				}
			}
		catch (Throwable t)
			{
			logger.error(t.getMessage(),t);
			}
		finally
			{
			if (it != null && it instanceof PersistenceBrokerIterator) ((PersistenceBrokerIterator) it).destroy();
			}
		if (!realtime && !isAternativeDisplay)
			{
			if (!report.chartType.equals("raw"))
				{
				ProcessRecord.postProcess(data, report, excludedcolumns);
				if (FormatReport.addPerformance(report)) addPerformance(data);
				if (report.isHTML.booleanValue()) FormatReport.html(report, reportColumns, data,
						response.getOutputStream(), publish, endOfShift, reportsDirectory);
				else if (report.chartType.equals("csv")) FormatReport.csv(report, reportColumns, data,
						response.getOutputStream(), publish, endOfShift, transferDirectory, StateStamper.getAreaCode(), StateStamper.getPlantCode(),
						StateStamper.getStage());
				else FormatReport.excel(report, reportColumns, data, response.getOutputStream(), publish,
						endOfShift, reportsDirectory, excludedcolumns);
				}
			}
		return data;
		}

	private boolean filterRecordsByArea(Report report, boolean durable, Hashtable columnIndex, String record, String date)
		{
		boolean result = true;
		String variable = durable? "AREA(DURABLE_ID)": "AREA(MID)";
		String areaPK = ProcessRecord.getValue(variable, record, date, columnIndex);
		if(report.getArea()!=null&&!Area.AREA_ALL_PK.equals(report.getArea().getPrimaryKey())
			&&!StringUtils.equals(report.getArea().getPrimaryKey().toString(), areaPK))
			{
			result = false;
			}
		return result;
		}
	
	/**
	 * @param cols a set f string with the columns of a report
	 * @return a String array containing the columns passed and the columns that are parameters of the functions used in the reports
	 * for instance COUNTERBAR_WW(TIME USAGE\PM DUE) would return TIME USAGE and PM DUE as part of the array  
	 * */
	private String[] getFunctionColumns(Set<String> cols)
		{
		String[] functionCols = new String[0];
		List <String> params = new ArrayList<String>();
		for(String s:cols)
			{
			params.addAll(getSlashFields(getFunctionArguments(s)));
			}
		functionCols = (String[]) params.toArray(functionCols);
		return functionCols;
		}
	
	/**
	 * @param function a string that may be a function that is it may contain () and arguments between the parenthesis
	 * @return the string between the ( and the ), if there are no () the passed string is returned 
	 * */
	private String getFunctionArguments(String function)
		 {
		 String arg = function;
		 int startPar = function.indexOf("(");
		 int endPar = function.indexOf(")");
		 if(startPar != -1 && endPar != -1)
			 {
			 arg = function.substring(startPar+1,endPar); 
			 }
		 return arg;
		 }
	 
	/**
	 * @param str a string that may contain slashes
	 * @return a List of strings containing the strings before and after the slash "\"
	 * for instance when called with "Time Usage\PM Due" as argument it would return a List with two elements "Time Usage" and "PM Due"
	 * if there is no slash a single item list is returned with the parameter passed 
	 * */
	private List<String> getSlashFields(String str)
		{
		List<String> fields = new ArrayList<String>();
		int slashIdx = str.indexOf("\\");
		if (slashIdx != -1)
			{
			String param1, param2;
			param1 = str.substring(0, slashIdx);
			param2 = str.substring(slashIdx + 1, str.length());
			fields.add(param1);
			fields.add(param2);
			}
		else
			{
			fields.add(str);
			}
		return fields;
		}

	public List getDurableReportHeaders(Report report) throws Throwable
		{
		List headers = new ArrayList();
		Hashtable repDefinition;
		if (report.period.equalsIgnoreCase("realtime"))
			{
			repDefinition = realTimeDurableReportDefinition;
			}
		else
			{
			repDefinition = durableReportDefinition;
			}
		String[] reportColumns = (String[]) repDefinition.get(report.reportType + "\t" + report.grouping);
		for (int i = 7; i < reportColumns.length; i++)
			{
			headers.add(reportColumns[i]);
			}
		return headers;
		}

	/**
	 * returns a list with the report definitions of the passed equipment type and the specified type 
	 * */
	public List getDurableRealTimeReport(EquipmentType equipmentType,Area area,String type)
		{
		Vector types = realTimeDurableReportTypes;
		Vector returnVector = new Vector();
		for (int i = 0; i < types.size(); i++)
			{
			String item = (String) types.get(i);
			String[] typeStr = item.split("\t");
			Report report = new Report();
			report.reportType = typeStr[0];
			report.grouping = typeStr[1];
			report.setArea(area);
			report.setEquipmentType(equipmentType);
			if(!report.reportType.startsWith(type)){continue;}
			String[] reportColumns = (String[]) realTimeDurableReportDefinition.get(report.reportType + "\t"
					+ report.grouping);
			if (equipmentType.name.equalsIgnoreCase(reportColumns[5]))
				{
				report.setColumns(reportColumns);
				returnVector.add(report);
				}
			}
		return returnVector;
		}

	/**
	 * get report filters from definition and populate the filter dropdown list.
	 * 
	 * @param report
	 *            report definition
	 * @param resultList
	 *            list of items for filtering. Set to be null for realtime
	 *            report.
	 * 
	 * 
	 */
	public Map getReportFilters(Report report, List resultList) throws Throwable
		{
		Hashtable repDefinition;
		if (report.period.equalsIgnoreCase("realtime"))
			{
			repDefinition = realTimeDurableReportDefinition;
			}
		else
			{
			repDefinition = durableReportDefinition;
			}
		String[] reportColumns = (String[]) repDefinition.get(report.reportType + "\t" + report.grouping);
		Hashtable filterHastable = new Hashtable();
		if ( !hasFilters(reportColumns)){ return filterHastable;}
		String[] filterlist = reportColumns[4].split(",");
		for (int i = 0; i < filterlist.length; i++)
			{
			Set tmp;
			if (report.period.equalsIgnoreCase("realtime"))
				{
				tmp = DurableManager.getFilterValue(report.getEquipmentType(), report.getArea(), filterlist[i]);
				}
			else
				{
				tmp = DurableManager.getFilterValuefromHistory(report.getEquipmentType(), report.getArea(), filterlist[i],resultList);
				}
			List tmpList = new ArrayList(tmp);
			List values = new ArrayList(tmpList.size()); 
			for(int j=0;tmpList !=null && j < tmpList.size();j++){values.add(tmpList.get(j).toString());}
			Collections.sort(values);
			filterHastable.put(filterlist[i], values);
			}
		return filterHastable;
		}
	
	private boolean hasFilters(String[] reportColumns)
		{
		return reportColumns != null &&
		       reportColumns.length > 3 &&
		       (! "NONE".equalsIgnoreCase(reportColumns[4]) );
		}
	
	
	public int getnumberofRecordperPage(Report report) throws Throwable
		{
		Hashtable repDefinition = realTimeDurableReportDefinition;
		String[] reportColumns = (String[]) repDefinition.get(report.reportType + "\t" + report.grouping);
		return Integer.parseInt(reportColumns[6]);
		}
	
	public Map getHistoryReportDataAsDurableStatus(Report report, Hashtable data) throws Throwable
		{
		List headers = getDurableReportHeaders(report);
		Set keyset = data.keySet();
		Iterator it = keyset.iterator();

		Hashtable hashtable = new Hashtable();
		while (it.hasNext())
			{
			List resultList = new ArrayList();
			String keyStr = (String) it.next();
			Vector recordVector = (Vector) data.get(keyStr);
			String fromstate = "";
			DurableStatus lastdurableStatus = null;
			for (int j = 0; j < recordVector.size(); j++)
				{
				String record = recordVector.get(j).toString();
				record = keyStr + "\t" + record;
				if (record.endsWith("\t"))
					{
					record = record + " ";
					}
				String[] recordArray = record.split("\t");
				DurableStatus durableStatus = new DurableStatus();
				for (int i = 0; i < headers.size(); i++)
					{
					String tmp = (String) headers.get(i);															
					if ((report.reportType.equalsIgnoreCase("History")||report.reportType.equalsIgnoreCase("History_Details"))&& tmp.equalsIgnoreCase("STATE-SUBSTATE"))
						{ // special check for historical report to generate
							// FROMSTATE and TOSTATE
						if (lastdurableStatus != null)
							{
							fromstate = recordArray[i];
							lastdurableStatus.set("FROMSTATE", fromstate);
							}
						durableStatus.set("TOSTATE", recordArray[i]);
						lastdurableStatus = durableStatus;
						}
					else if (tmp.equalsIgnoreCase("COMMENTS"))
						{
						Map2Json map2Json = new Map2Json();
						Map map = map2Json.str2Map(recordArray[i]);
						durableStatus.set("COMMENTS",map);
						}
					else
						{
						durableStatus.set(tmp, recordArray[i]);
						}
					}
				resultList.add(durableStatus);
				}
			hashtable.put(keyStr, resultList);
			}
		return hashtable;
		}

	public List getSortedRealtimeReportData(Report report, Hashtable data) throws Throwable
	{
		return getSortedRealtimeReportData(report, data, null);	
	}
	public List getSortedRealtimeReportData(Report report, Hashtable data, EquipmentType eqType) throws Throwable
		{
		String[] orderBy = report.getOrderBy().split(",");
		String order = orderBy[0];
		boolean isAscendent = Boolean.valueOf(orderBy[1]);
		List headers = getDurableReportHeaders(report);
		Set keyset = data.keySet();
		Iterator it = keyset.iterator();
		List resultList = new ArrayList();
		while (it.hasNext())
			{
			String keyStr = (String) it.next();
			Vector recordVector = (Vector) data.get(keyStr);
			String record = recordVector.get(0).toString();
			record = keyStr + "\t" + record;
			if (record.endsWith("\t"))
				{
				record = record + " ";
				}
			String[] recordArray = record.split("\t");
			Machine durable = new Machine();
			durable.setEquipmentType(eqType);
			DurableStatus status = new DurableStatus();
			durable.context.put("durableStatus", status);
			for (int i = 0; i < headers.size(); i++)
				{
				String tmp = (String) headers.get(i);
				if ("children".equals(tmp))
					{
					// convert String to Hashtable
					Map2Json hashMap2Json = new Map2Json();
					Map map = hashMap2Json.str2Map(recordArray[i]);
					durable.set(tmp, map);
					}
				else if (tmp.equalsIgnoreCase("COMMENTS")){
					Map2Json map2Json = new Map2Json();
					Map map = map2Json.str2Map(recordArray[i]);
					durable.set("COMMENTS", map);					
					}
				else {
					durable.set(tmp, recordArray[i]);
					}
				}

			resultList.add(durable);
			}
		if (!resultList.isEmpty() && !order.equalsIgnoreCase("NONE"))
			{
			Collections.sort(resultList, new SortDurable(order));
			}
		return resultList;
		}

	/**
	 * Generates a report 
	 * @param report A report object with the settings for this report
	 * @param outputStream Output stream to write the report to
	 * @param publish Whether to publish the report or not
	 * @param endOfShift Whether it is the end of shift or not
	 * @return A hashtable with the report data
	 * @throws Throwable
	 */

	public Hashtable generate(Report report, OutputStream outputStream,boolean publish,boolean endOfShift) throws Throwable 
	    {
		long start = System.currentTimeMillis();
	    int excludedcolumns = 3;
		String[] equal = null,greater = null,less = null,like = null,order = null,in = null,grouping=null;
		String outer = null;
		Hashtable columnIndex = new Hashtable(), data = new Hashtable();
		boolean all = report.machines == null || report.machines.size() == 0 || Boolean.TRUE.equals(report.includeAll);
		boolean raw = report.period.equals("Raw");
		report.setColumns(getReportDefinition(report));
		report.chartType = report.getColumns()[0];
		report.totals = report.getColumns()[1];
		report.condition = report.getColumns()[2];
		String startDate = null, endDate = null; // startDate and endDate are  only used when period = "Raw"
		long count = 0;
		order = new String[]{"STATUS_PK",Boolean.toString(true)};
		boolean durableReport = false;
		Class table =null;
		Date auxDate = null;
		if(null == report.shiftHours){report.shiftHours=false;}
		if (report.isTesterReport())
			{
			durableReport = false;
			table = Status.class;
			List statusAttributes = report.getEquipmentType().getStatusAttributes();
			List lAttr = new ArrayList(statusAttributes.size());
			for (int i = 0; i < statusAttributes.size(); i++)
				{
				EquipmentAttribute tmp = (EquipmentAttribute) statusAttributes.get(i);
				lAttr.add(tmp.getName());
				}
			int size = Status.DEFAULT_STATUS.length + lAttr.size();
			attrib = new String[size];
			for (int i = 0; i < Status.DEFAULT_STATUS.length; i++){attrib[i] = Status.DEFAULT_STATUS[i];}
			for (int i = 0; lAttr != null && i < lAttr.size(); i++){attrib[i + Status.DEFAULT_STATUS.length] = (String) lAttr.get(i);}
			if(raw)
				{
				equal = new String[] { "PERIOD", report.period.substring(0, 1)};
				}
			else
				{
				attrib[TIME_INDEX] = report.period;
				}
			}
		else
			{
			durableReport = true;
			table = DurableStatus.class;
		     String[] durableAttributes={"STATUS_PK","TIME","EQUIPMENT_PK","EVENT_PK","STATE","SUBSTATE",
		    		"OPERATOR_ID","DURABLE_ID","ATTRIBUTES","COMMENTS","WORKSTATION","LOCATION"};
		     attrib=durableAttributes;
		    }
		
		List inList = new ArrayList();
		if (raw)
			{
			if(report.shiftHours){
				String dateString = formatter.format(report.startDate);
				FiscalDate startShift = new FiscalDate(FiscalDate.SHIFT, dateString);
				auxDate = report.startDate = formatter.parse(startShift.startTime);
			}else{
				auxDate = report.startDate;
			}
			
			// If the report is a state transition report, move the start time to writeInterval seconds earlier to include the last event before the start time
			if (report.chartType.equals("raw"))
				auxDate = new Date(report.startDate.getTime() - writeInterval * 1000L);
			else
				auxDate = new Date(report.startDate.getTime()-1000L);
			//since the comparisons are greater and less we need to move the startDate one second before and endDate one second after to include the events that happen at exactly the start and end ime
			
			startDate = formatter.format(auxDate);
			endDate = formatter.format(new Date(report.endDate.getTime()+1000L));
			greater = new String[] {"TIME", startDate };
			less = new String[] { "TIME", endDate };
			}
		else
			{
			inList.add(report.period);inList.add(report.getDatesAsString());
			}
		
		if(!all)
			{
			if(report.isTesterReport() ){inList.add("MID");inList.add(report.getMachinesAsString());}
			else {inList.add("DURABLE_ID");inList.add(report.getMachinesAsString());}
			}
		if(inList.size() >0){in=(String[])inList.toArray(new String[0]);}
		Iterator it = null;
		try{
		it =Persistence.iSearch(table, attrib, equal,greater, less, like, in,grouping,order, outer);
		int k;
		for (k=0; k<attrib.length; k++) {columnIndex.put(attrib[k],new Integer(k));} // Read column headers
		columnIndex.put("AREA_CODE",new Integer(k++));
		columnIndex.put("PLANT_CODE",new Integer(k++));
		columnIndex.put("STAGE",new Integer(k++));
		columnIndex.put("TIME",TIME_INDEX);
		Object[] res = null;
		String record;
		String date =null;
		for (count = 0;it != null && it.hasNext(); count++) {
			res = (Object[]) it.next();
			record="";
			for (int j = 0; res != null && j < res.length; j++) {
				record+=res[j]!= null?res[j]+"\t":"\t";
				if (j==TIME_INDEX){//Time is processed independently of the record in date field
					if(!raw){
						date= res[j].toString();
					}else{//raw data is grouped by shift
						FiscalDate fd = new FiscalDate("Shift",res[j].toString());
						date = fd.fDate+"-"+fd.shift;
					}
				}
			}
			record+= StateStamper.getAreaCode() !=null?StateStamper.getAreaCode()+"\t":"\t";
			record+= StateStamper.getPlantCode()!=null?StateStamper.getPlantCode()+"\t":"\t";
			record+= StateStamper.getStage() !=null?StateStamper.getStage()+"\t":"\t";
			boolean isText = report.chartType.equals("text");
			if(filterRecordsByArea(report, durableReport, columnIndex, record, date))
				{
				if(condition(report.condition,record,date,columnIndex)){				
					ProcessRecord.process(data,report.getColumns(),record,date,columnIndex,count,excludedcolumns,isText);
				}
			}
		}
			long end = System.currentTimeMillis();
			logger.debug("SchedulerReport Type: " + report.reportType + " by "
					+ report.grouping + " ; number period: " + report.period
					+ " / " + report.numberPeriods + "  started from: "
					+ startDate + " to " + endDate + " in " + (end - start)
					+ "ms");
		if (!report.chartType.equals("raw"))
			{
			ProcessRecord.postProcess(data,report,excludedcolumns);
			if (FormatReport.addPerformance(report)) addPerformance(data);
			if (report.isHTML.booleanValue()) FormatReport.html(report,report.getColumns(),data,outputStream,publish,endOfShift,reportsDirectory);
			else if(report.chartType.equals("csv"))FormatReport.csv(report,report.getColumns(),data,outputStream,publish,endOfShift,transferDirectory,StateStamper.getAreaCode(),StateStamper.getPlantCode(),StateStamper.getStage());
			else FormatReport.excel(report,report.getColumns(),data,outputStream,publish,endOfShift,reportsDirectory,excludedcolumns);
			}
		}
		catch(Throwable  t) {
		        StringBuilder dates = new StringBuilder();
		        for(int i = 0;report.dates!= null && i< report.dates.length;i++){dates.append(report.dates[i]);}
		 		logger.error("reportType " + report.reportType + " Grouping "+ report.grouping +" charType " + report.chartType  + " period " + report.period + 
		 				" number of periods " + report.numberPeriods + " includeAll " + report.includeAll + " machines " + report.machines  + " dates " + dates + 
		 				" startDate " + report.startDate + " endDate " + report.endDate + " data size " + data.size());

			logger.error(report.reportType + "\t" + report.grouping,t);
		}finally{
			if (it != null)((PersistenceBrokerIterator)it).destroy();
		}
		return data;
	}

	
	/**
	 * Returns a vector of vectors with all the report types and subtypes
	 * @return A vector of vectors: [type1,[subtype1,subtype2,...],type2,[subtype1,subtype2,...],...
	 */
	public Vector getReportTypes(EquipmentType type) 
		{
		if(type==null || type.name== null || "Tester".equals(type.name)){return reportTypes;}
		else {return durableReportTypes;}
		}
	
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

	
	public void setDates(String period,List<String> dates)
		{
		dbDates.put(period, dates);
		}

	/**
	 * Generates all the reports scheduled for the given fiscal date
	 * @param date The fiscal date
	 * @param endOfShift Whether it is the end of shift or not
	 */
	public void generate(FiscalDate date, boolean endOfShift)
		{
		try
			{
			List<String> dates = dbDates.get(date.period);
			for (int i=0; i<scheduledReports.size(); i++)
				{
				try
					{
					Report report = scheduledReports.get(i);
					if (report.period.equals(date.period))
						{
						if (report.period.equals("Raw"))
							{
							report.endDate = formatter.parse(date.endTime);
							report.startDate = new Date((long) (report.endDate.getTime()-report.numberPeriods.doubleValue() * 3600000L));
							}
						setReportDates(report,dates,date);
						if (report.dates.length > 0)
							{
							report.machines = StateTransition.getMachines(report.types,report.locations); // Get the lost of machines
							report.setEquipmentType(StateTransition.getEquipmentType(report.getEquipmentTypePk()));
							generate(report,null,true,endOfShift);
							}
						}
					}
				catch (Throwable t) {
						Report report = scheduledReports.get(i);
						logger.error(report.reportType + "\t" + report.grouping + "\t" + date,t);
					}
				}
			}
		catch (Throwable t) {logger.error(date.period+" "+date.startTime+" "+date.endTime,t);}	
		}
	
	/**
	 * */
	public void setReportsDirectory(String repDir){reportsDirectory = repDir;}
	public void setTransferDirectory(String transDir){transferDirectory=transDir;}
	public void setWriteInterval(int writeInt){writeInterval=writeInt;}
	public void setDates(Map<String,List<String>> dates){dbDates = dates;}
	
	/**
	 * Set all dates in the report object for this report settings and the available dates 
	 * @param report The report object
	 * @param dates A vector of all available dates
	 * @param date the date of the report
	 */
	private void setReportDates(Report report, List dates,FiscalDate date)
		{
		// Add dates to report
		double size = report.numberPeriods.doubleValue();
		if (report.period.equals("Raw")) size=(int)Math.ceil(size/24.0*FiscalDate.getNumberShifts()); // Translate number of hours to number of shifts
		size = Math.min(size,dates.size());
		report.dates = new Object[(int) size];
		int initDate = 0;
		boolean found = false;
		if( !date.period.equals(FiscalDate.RAW) && !dates.contains(date.toString()))
			{
			dates.add(0, date.toString());
			}
		for(int i=0;i< dates.size() && !found;i++)
			{
			found = dates.get(i).equals(date.toString());
			if(found) initDate = i;
			}
		for (int i=0; i<size; i++) report.dates[i] = dates.get(initDate+i);
		}
	
	/**
	 * Returns whether given condition is true or false
	 * @param condition The condition
	 * @param record The record being processed
	 * @param date The date
	 * @param variableIndex A hashtable with the index of each variable in the record
	 * @return
	 */
	private boolean condition(String condition, String record, String date, Hashtable variableIndex)
		{
		if (condition.equals("NONE")) return true;
		else {
			//replace | with & since | has a special meaning for regex
			String condCopy= condition.replace("|","&");
			String[] conditions = condCopy.split("&&");
			boolean met=false;
			for(int i=0;i<conditions.length;i++)
				{
				if (ProcessRecord.isPostProcessCondition(conditions[i])){continue;}
				met = Boolean.parseBoolean(ProcessRecord.getValue(conditions[i], record, date, variableIndex));
				if (!met) break;
				}
			return met;
		}
			
		}
	
	/**
	 * Write performance numbers: OEE, First Pass Site Perf Loss, First Pass Yield Loss and Retest Gain 
	 * @param data The report data
	 * @throws Throwable
	 */
	private void addPerformance(Hashtable data) throws Throwable
		{
		Vector keys = new Vector(data.keySet());
		Collections.sort(keys);
	    String prevCategory = null;
	    float oee = 0, firstPassSitePerfLoss = 0, firstPassYieldLoss = 0, retestGain = 0, goodCount = 0;
	    for (int i=0; i<keys.size(); i++)
	    	{
	    	String key = (String)keys.elementAt(i);
	    	String category = key.substring(0,key.indexOf("\t"));
	    	if (prevCategory != null && !category.equals(prevCategory))
	    		{
	    		addPerformance(data,prevCategory,oee,firstPassSitePerfLoss,firstPassYieldLoss,retestGain,goodCount);
				oee = firstPassSitePerfLoss = firstPassYieldLoss = retestGain = goodCount = 0;
	    		}
			prevCategory = category;
	    	List values = (List)data.get(key);
	    	oee +=  ((Float)values.get(1)).floatValue()/100;
	    	firstPassSitePerfLoss +=  ((Float)values.get(2)).floatValue()/100;
	    	firstPassYieldLoss += ((Float)values.get(3)).floatValue()/100;
	    	retestGain += ((Float)values.get(4)).floatValue()/100;
	    	goodCount += ((Float)values.get(5)).floatValue();
	    	}
	    if (prevCategory == null) return;
	    addPerformance(data,prevCategory,oee,firstPassSitePerfLoss,firstPassYieldLoss,retestGain,goodCount);
		}

	/**
	 * Add performance to the data object for the given category
	 * @param data The data object
	 * @param category The category
	 * @param oee
	 * @param firstPassSitePerfLoss
	 * @param firstPassYieldLoss
	 * @param retestGain
	 * @param total
	 */
	private void addPerformance(Hashtable data, String category, float oee, float firstPassSitePerfLoss, float firstPassYieldLoss, float retestGain,float goodCount)
		{
		data.put(category+"\t \tOEE",new Vector(Arrays.asList(new Float[]{new Float(oee)})));
		data.put(category+"\t \tFirst Pass Site Perf Loss",new Vector(Arrays.asList(new Float[]{new Float(firstPassSitePerfLoss)})));
		data.put(category+"\t \tFirst Pass Yield Loss",new Vector(Arrays.asList(new Float[]{new Float(firstPassYieldLoss)})));
		data.put(category+"\t \tRetest Gain",new Vector(Arrays.asList(new Float[]{new Float(retestGain)})));
		data.put(category+"\t \tGood Count",new Vector(Arrays.asList(new Float[]{new Float(goodCount)})));
		}

	/**
	 * Class that compares two shifts with format YYYY-MM-DD-shift (i.e. 2009-06-01-2)
	 */
	public static class CompareDates implements Comparator
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
			String fDate1 = time1.substring(0,index1), fDate2 = time2.substring(0,index2);
			
			if (fDate1.equals(fDate2))
				{
				try
					{
					String shift1 = time1.substring(index1+1), shift2 = time2.substring(index2+1);
					index1 = shift1.indexOf(" "); if (index1>0) shift1 = shift1.substring(0,index1);
					index2 = shift2.indexOf(" "); if (index2>0) shift2 = shift2.substring(0,index2);
					FiscalDate date1 = new FiscalDate("Shift",fDate1,shift1);
					FiscalDate date2 = new FiscalDate("Shift",fDate2,shift2);
					return (int)FiscalDate.diff(date1.startTime,date2.startTime);
					}
				catch (Throwable t) {return 0;}
				}
			else return fDate1.compareTo(fDate2);
			}
		}

	

	/**
	 * get the attribute that containing the dynamic attributes together with
	 * the static attributes from
	 * 
	 * @param eType
	 *            the equipmentType
	 * @param attri
	 *            String[] of static atrributes
	 * @return String[] for new attributes
	 * @throws Throwable
	 */
	private String[] getCompleteStatusAttributeforDurable(EquipmentType eType, String[] attri)
		{
		Vector attributes = eType.getStatusAttributes();
		List statusAttributeName = new ArrayList();

		for (int i = 0; i < attributes.size(); i++)
			{
			EquipmentAttribute eAttribute = (EquipmentAttribute) attributes.get(i);			
			statusAttributeName.add(eAttribute.name);							
			}
		List newattributes = new ArrayList();
		newattributes.addAll(Arrays.asList(attri));
		newattributes.remove(newattributes.size() - 1); // remove the Attributes
		newattributes.addAll(statusAttributeName);		
		String[] newattrib = new String[0];
		newattrib = (String[]) newattributes.toArray(newattrib);
		return newattrib;
		}

	/**
	 * Process the data from DB query.
	 * 
	 * @param it
	 *            the Iterator return from DB
	 * @param isTransitiononly
	 *            whether to only return transition rows
	 * @param eType
	 *            equipmentType
	 * @param attrib
	 *            attribute variables
	 * @return A Vector of Object[]
	 * @throws Throwable
	 */
	private Vector getDurableHistoricalReportData(Iterator it, Report report, String[] attrib)
		{
		String tempState = "";
		String tempSubstate = "";
		Vector returnVector = new Vector();
		while (it.hasNext())
			{
			Object[] result = (Object[]) it.next();
			List returnResultList = new ArrayList();
			returnResultList.addAll(Arrays.asList(result));			
			String state = (String) result[result.length - (defaultDurableAttributes.length - getArrayIndex(defaultDurableAttributes,"STATE"))];
			String substate = (String) result[result.length - (defaultDurableAttributes.length - getArrayIndex(defaultDurableAttributes,"SUBSTATE"))];			
			Map statusAttributeMap = (Map) result[result.length - 1];
			returnResultList.remove(returnResultList.size() - 1);
			if (report.isTransitionOnly())
				{
				if (state.equalsIgnoreCase(tempState) && substate.equalsIgnoreCase(tempSubstate))
					{					
					Object[] lastitem =(Object[])returnVector.elementAt(returnVector.size()-1);					
					returnVector.remove(returnVector.size()-1);										
					}
				}		
			tempState = state;
			tempSubstate = substate;
			for (int j = result.length - 1; j < attrib.length; j++)
				{
				Object tmpValue = (Object) statusAttributeMap.get(attrib[j]);
				returnResultList.add(tmpValue);
				}
			Object[] tmp = new Object[0];
			tmp = (Object[]) returnResultList.toArray(tmp);
			returnVector.add(tmp);
			}
		if (it != null && it instanceof PersistenceBrokerIterator) ((PersistenceBrokerIterator) it).destroy();
		return returnVector;
		}
	
	
	public List getFilteredHistoricalReport(List resultList, Map filter)
		{
		Iterator it = resultList.iterator();
		List result = new ArrayList();
		List filterKeys = new ArrayList(filter.keySet());
		while (it.hasNext())
			{
			DurableStatus durable = (DurableStatus) it.next();
			boolean isValid = true;
			for(int i=0;i< filterKeys.size() && isValid;i++)
				{
				String key = (String) filterKeys.get(i);
				String value = filter.get(key).toString();
				if (value == null || value.equals("")){continue;}
				String statusValue = (String) durable.get(key);
				if      (statusValue == null)       { isValid = false;}
				else if (statusValue.contains(",")) { isValid = statusValue.contains(value);}
				else                                { isValid = statusValue.equals(value);}
				}
			if (isValid)
				{
				result.add(durable);
				}
			}
		return result;
		}
	
	private int getArrayIndex(String[] inputArray, String inputStirng)
		{
		for (int i = 0; i < inputArray.length; i++)
			{
			if (inputArray[i].equals(inputStirng)) return (i);
			}
		return (-1);
		}
	/**
	 * retrieves the report definition of the Report object passed
	 * @param report the report definition to retrieve, the report should have at least the report type and grouping populated, 
	 * if the report does not have a equipment type defined Tester is assumed 
	 * @return the definition of the report as defined in the reports definition files 
	 * */
	public String[] getReportDefinition(Report report)
		{
		String[] repDef=null;
		Map defs;
		if(report.isTesterReport()){defs = reportDefinition;}
		else if(report.period.equalsIgnoreCase("Realtime")){defs = realTimeDurableReportDefinition;}
		else {defs = durableReportDefinition;}
		String repKey = report.reportType + "\t" + report.grouping;
		if(defs.containsKey(repKey)){ repDef = (String[])defs.get(repKey);}
		return repDef;
		}

	public String getReportsDirectory()
		{
		return reportsDirectory;
		}
	
	}			