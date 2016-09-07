package com.freescale.stateStamper.view;

import com.freescale.clientUtil.Util;
import com.freescale.stateStamper.model.entity.*;

import java.awt.*;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

import javax.swing.*;

public class FinalTest
	{
	private static final String[] PROD_CNTX = {"SUBSTATE","DEVICE_NAME","FLOW_ID","TEMPERATURE","TIME"}, PROD_PERF = {"LAST_HR_OEE","LOT_PROGRESS"};
	private static final String[] MAINT_CNTX = {"SUBSTATE","DEVICE_NAME","LOADBOARD_ID","TIME"}, MAINT_PERF = {"CURRENT_SITE_PERF","24_HR_SITE_PERF"};
	private static final String[] ENG_CNTX = {"TEST_PROGRAM","FLOW_ID"}, ENG_PERF = {"LAST_HR_YIELD","24_HR_YIELD"};
	private static DecimalFormat formatter = new DecimalFormat(); 
	private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private static long DAY_MSECS = 24*60*60*1000;
	private static String [] extendedStatusFields,extendedContexts,extendedSummings;
	private static int[] indexes;
	static {formatter.setMaximumFractionDigits(2);}
	
	/**
	 * Create label text for a given machine and dashboard view
	 * @param machine The machine
	 * @param dashboard The dashboard view
	 * @param value Whether to display the variable value or name
	 * @return
	 */
	public static String createText(Machine machine, int dashboard, boolean value)
		{
		switch (dashboard)
			{
			case StateStamperFrame.UTILIZATION: return createText(machine,new String[]{},new String[]{},dashboard,value);
			case StateStamperFrame.PRODUCTION: return createText(machine,PROD_CNTX,PROD_PERF,dashboard,value);
			case StateStamperFrame.MAINTENANCE: return createText(machine,MAINT_CNTX,MAINT_PERF,dashboard,value);
			case StateStamperFrame.ENGINEERING: return createText(machine,ENG_CNTX,ENG_PERF,dashboard,value);
			}
		return "";
		}
	
	/**
	 * Returns the text for the given dashboard
	 * @param dashboard The dashboard view
	 * @return
	 */
	public static String getView(int dashboard)
		{
		switch (dashboard)
			{
			case StateStamperFrame.UTILIZATION: return "Utilization";
			case StateStamperFrame.PRODUCTION: return "Production";
			case StateStamperFrame.MAINTENANCE: return "Maintenance";
			case StateStamperFrame.ENGINEERING: return "Engineering";
			}
		return "";
		}
	
	/**
	 * Return color for a given machine and dashboard view
	 * @param machine The machine
	 * @param dashboard The dashboard view
	 * @return The color for the given machine
	 */
	public static Color getColor(Machine machine, int dashboard)
		{
		switch (dashboard)
			{
			case StateStamperFrame.UTILIZATION:
				{				
				return StateStamperFrame.getColor(machine.state);
				}
			case StateStamperFrame.PRODUCTION:
				{
				Float complete = (Float)machine.performance.get("LOT_PROGRESS"); 
				if (complete != null && !complete.isInfinite())
					{
					if (complete.floatValue()>=95) return Color.RED;
					else if (complete.floatValue()>=80) return Color.ORANGE;
					else return Color.GREEN;				
					}
				else return Color.GREEN;
				}
			case StateStamperFrame.MAINTENANCE:
				{
				Float sitePerformance = (Float)machine.performance.get("CURRENT_SITE_PERF");
				if (sitePerformance != null)
					{
					if (sitePerformance.floatValue()<50 || machine.state.equals("Unscheduled Down")) return Color.RED;
					else if (sitePerformance.floatValue()<90) return Color.ORANGE;
					else return Color.GREEN;
					}
				}
			case StateStamperFrame.ENGINEERING:
				{
				Float yield = (Float)machine.performance.get("LAST_HR_YIELD");
				if (yield != null)
					{
					if (yield.floatValue()<50) return Color.RED;
					else if (yield.floatValue()<80) return Color.ORANGE;
					else return Color.GREEN;
					}
				}
			}
		return Color.LIGHT_GRAY;
		}
	
	/**
	 * Return whether the limit for the machine substate has been exceeded
	 * @param machine The machine to check if the time in the current substate has been exceeded
	 * @param dashboard The dashboard view
	 * @return Whether the limit has been exceeded
	 */
	public static boolean limitExceeded(Machine machine, int dashboard)
		{
		if (dashboard==StateStamperFrame.UTILIZATION) return false;  // Do not show limit exceeded on utilization view 
		String substate = (String)machine.context.get("SUBSTATE");
		if (substate != null)
			{
			Substate aux = StateStamperFrame.getSubstate(substate);
			if (aux != null && aux.limit != null && aux.limit.intValue() > 0)
				{
				Date substateStart = (Date)machine.performance.get("SUBSTATE_START");
				if (substateStart!=null && (new Date().getTime()-substateStart.getTime())/60000 > aux.limit.intValue()) return true;
				}
			}
		return false;
		}
	
	public static JPanel createDurableReport(Machine durable)
		{
		JPanel panel = new JPanel(new BorderLayout());
		try
			{
			List results = (List)Util.request(durable.eIName,"getDurableTransactionHistory.do",false);
			CurrentDurableStatus currStatus=null;
			if(results!=null&&results.size()>0){currStatus = (CurrentDurableStatus)results.get(0);}
			List tmp = (List) results.get(1);
			DurableStatus hStatus=null;
			long totalDuration=0;
			List values = new ArrayList();
			StringBuffer row; 
			Date dEndDate = new Date();
			Date dStartDate = new Date(dEndDate.getTime() - DAY_MSECS );
			String endDate = dateFormat.format(dEndDate);
			long duration1;
			boolean dayCompleted=false;
		    for(int i = tmp.size();!dayCompleted && i> 0;i--)
		    	{
		    	row=new StringBuffer();
		    	hStatus = (DurableStatus)tmp.get(i-1);
		    	row.append(hStatus.STATE);
		    	row.append("\t"+hStatus.SUBSTATE);
		    	duration1 = diff(endDate,hStatus.TIME);
		    	if(duration1 + totalDuration >= DAY_MSECS)
		    		{
		    		duration1 = DAY_MSECS - totalDuration;
		    		endDate = dateFormat.format( dStartDate );
		    		dayCompleted=true;
		    		}
		    	else
		    		{
		    		totalDuration+=duration1;
		    		endDate  =  hStatus.TIME;
		    		}
		    	row.append("\t"+endDate);
		    	row.append("\t"+duration1);
		    	row.append("\t"+hStatus.workstation);
		    	values.add(row.toString());
		    	row=null;
		    	}
		    Collections.reverse(values);
			Hashtable data = new Hashtable();
			Hashtable colors = StateStamperFrame.getColors();
			String[] contextItems = new String[]{"State","Substate","Workstation"};
			Vector[] contextVectors = new Vector[contextItems.length];
			for (int j=0; j<contextItems.length; j++) data.put(contextItems[j],contextVectors[j]=new Vector());
			int[] indexes = new int[]{0,1,4};
			String[] summingItems = new String[0];
			Vector[] summingVectors = new Vector[summingItems.length];
			for (int j=0; j<summingItems.length; j++) data.put(summingItems[j],summingVectors[j]=new Vector());
			
			for (int i=0; i<values.size(); i++)
				{
				String[] items = ((String)values.get(i)).split("\t");
				String time = items[2];
				String duration = items[3];
				for (int j=0; j<contextItems.length; j++) add(contextVectors[j],items[indexes[j]],time,duration,colors);
				for (int j=0; j<summingItems.length; j++)
					{
					float value = 0;
					add(summingVectors[j],(int)value+"",time,duration,null);
					}
				}
			// Add current context and performance numbers
			String[] items= new String[0]; 
			List lItems = new Vector();
			lItems.add("STATE");lItems.add("SUBSTATE");lItems.add("WORKSTATION");lItems.add("LOCATION");
			Map dynAttributes = currStatus.dynamicAttributes;
			lItems.addAll( dynAttributes.keySet());
			items = (String[])lItems.toArray(items);
			String text = "<html><table cellpadding=1 cellspacing=1><tr><td width=6></td>";
			String value;
			for (int i=0; i<items.length; i++) 
				{
				value="";
				if(items[i].equals("STATE")){
				value = currStatus.STATE;
				}else if(items[i].equals("SUBSTATE")){
				value= currStatus.SUBSTATE;
				}else if("WORKSTATION".equals(items[i])){
				value = currStatus.workstation;
				}else if ("LOCATION".equals(items[i])){
				value= currStatus.location;
				}else if ("PLATFORM".equals(items[i])){
				value = currStatus.platform;
				}else if( dynAttributes!=null&& dynAttributes.containsKey(  items[i])){
				value+= dynAttributes.get(items[i]);
				}
				
				text += "<td>"+Machines.deCapitalize(items[i])+":</td><td>"+value+" &nbsp</td>";
				if (i%5==4) text += "</tr><tr><td width=6></td>";
				}
			text += "</tr></table></html>";
			panel.add(new JLabel(text),BorderLayout.NORTH);
			panel.add(new StateTransitions(data,contextItems,summingItems,colors,dStartDate,dEndDate),BorderLayout.CENTER);
			}
		catch (Throwable t) {t.printStackTrace();}

		
		return panel;
		}
	/**
	 * returns the milliseconds difference between two dates
	 * @param time1 time in format "yyyy-MM-dd HH:mm:ss"
	 * @param time2 
	 * */
	private static long diff(String time1,String time2)
		{
		long diff=0;
		try
			{
			long d1 = dateFormat.parse(time1).getTime();
			long d2 = dateFormat.parse(time2).getTime();
			diff = d1-d2;
			}
		catch (ParseException e){e.printStackTrace();} 
		return diff;
		}
	/**
	 * Create report for given machine
	 * @param machine The machine
	 * @return A JPanel with the report
	 */
	public static JPanel createReport(Machine machine)
		{
		JPanel panel = new JPanel(new BorderLayout());
		try
			{
			Report report = getStateTransitionReport(machine);
			Map aux = (Map)Util.request(report,"getStateTransitions.do",false);
			// Convert to local time
			report.startDate.setTime(report.startDate.getTime()+report.startDate.getTimezoneOffset()*60000);
			report.endDate.setTime(report.endDate.getTime()+report.endDate.getTimezoneOffset()*60000);
			List values = (List)aux.get(machine.mESName);			
			Hashtable data = new Hashtable();
			Hashtable colors = StateStamperFrame.getColors();
			String[] contextItems =  getExtendedContexts();
			Vector[] contextVectors = new Vector[contextItems.length];
			for (int j=0; j<contextItems.length; j++) data.put(contextItems[j],contextVectors[j]=new Vector());
			int[] indexes = getIndexes(report,contextItems);
			String[] summingItems = getExtendedSummings();
			Vector[] summingVectors = new Vector[summingItems.length];
			for (int j=0; summingItems != null && j<summingItems.length; j++) data.put(summingItems[j],summingVectors[j]=new Vector());
			for (int i=0;values != null && i<values.size(); i++)
				{
				String[] items = ((String)values.get(i)).split("\t");
				String time = items[20], duration = items[21], sites = items[8], insertions = items[22], devices = items[23], good = items[24];
				for (int j=0; j<contextItems.length; j++) add(contextVectors[j],items[indexes[j]],time,duration,colors);
				for (int j=0; j<summingItems.length; j++)
					{
					float value = 0;
					try
						{
						if (summingItems[j].equals("Yield")) value = Integer.parseInt(good)*100F/Integer.parseInt(devices);
						else if (summingItems[j].equals("Site Perf")) value = Integer.parseInt(devices)*100/(Integer.parseInt(sites)*Integer.parseInt(insertions));
						else value = Integer.parseInt(items[27+(j-2)*2])*100F/Integer.parseInt(items[28+(j-2)*2]);
						if (Float.isNaN(value) || Float.isInfinite(value)) value = 0;
						}
					catch (Throwable t) {}
					add(summingVectors[j],(int)value+"",time,duration,null);
					}
				}
			// Add current context and performance numbers
			String[] items = getExtendedStatusFields(); 
			String text = "<html><table cellpadding=1 cellspacing=1><tr><td width=6></td>";
			for (int i=0; i<items.length; i++) 
				{
				String value = items[i].equals("STATE")?machine.state:extract(machine,items[i]);
				text += "<td>"+Machines.deCapitalize(items[i])+":</td><td>"+value+" &nbsp</td>";
				if (i%5==4) text += "</tr><tr><td width=6></td>";
				}
			text += "</tr></table></html>";
			panel.add(new JLabel(text),BorderLayout.NORTH);
			panel.add(new StateTransitions(data,contextItems,summingItems,colors,report.startDate,report.endDate),BorderLayout.CENTER);
			}
		catch (Throwable t) {t.printStackTrace();}
		return panel;
		}
	
	private static Report getStateTransitionReport(Machine machine)
		{
		Date date = new Date();
		Report report = new Report();
		report.reportType = "Transitions";
		report.grouping = "Test head";
		report.machines = new HashMap();
		report.machines.put(machine.mESName,machine);
		report.period = "Raw";
		report.endDate = date;
		report.startDate = new Date(date.getTime()- DAY_MSECS);
		report.dates = Dates.populateDates(report.startDate,report.endDate);
		report.setEquipmentType(machine.getEquipmentType());
		// Convert to GMT
		report.startDate.setTime(report.startDate.getTime()-report.startDate.getTimezoneOffset()*60000);
		report.endDate.setTime(report.endDate.getTime()-report.endDate.getTimezoneOffset()*60000);
		return report;
		}
	
	/**
	 * Add a data item to a vector of data items
	 * @param items A vector with all the data items
	 * @param item The data item to add
	 * @param time The time for that data item
	 * @param duration The duration for that data item
	 * @param colors A hashtable with colors for the data items 
	 */
	private static void add(Vector items, String item, String time, String duration, Hashtable colors)
		{
		items.addElement(item+"\t"+time+"\t"+duration);
		if (colors==null) return;
		Color color = (Color)colors.get(item);
		if (color == null) colors.put(item,new Color(128+(int)(Math.random()*128),128+(int)(Math.random()*128),128+(int)(Math.random()*128)));
		}
	
	/**
	 * Create label text for a given machine
	 * @param machine The machine
	 * @param context An array of context variables to display
	 * @param performance An array of performance variables to display
	 * @param value Whether to display the variable value or name
	 * @return The label text
	 */
	private static String createText(Machine machine, String[] context, String[] performance, int dashboard, boolean value)
		{
		String text;
		if (value)
			{
			text = "<html>"+machine.mESName+"<br><table cellpadding=0 cellspacing=0>";
			for (int i=0; i<context.length; i++) text += "<tr><td nowrap>"+extract(machine,context[i])+"</td></tr>";
			if (performance.length >= 2) text += "<tr><td nowrap>"+extract(machine,performance[0])+"/"+extract(machine,performance[1])+"</td></tr>";
			}
		else
			{
			text = "<html>Tester<br><table cellpadding=1 cellspacing=1>";
			for (int i=0; i<context.length; i++) text += "<tr><td nowrap colspan=2>"+Machines.deCapitalize(context[i])+"</td></tr>";
			if (performance.length >= 2) text += "<tr><td nowrap colspan=2>"+Machines.deCapitalize(performance[0])+"/"+Machines.deCapitalize(performance[1])+"</td></tr>";
			text += getColorLegend(dashboard);
			}
		text += "</table></html>";
		return text;
		}

	/**
	 * Create color legend for the given dashboard
	 * @param dashboard The dashboard
	 * @return Color legend
	 */
	private static String getColorLegend(int dashboard)
		{
		switch (dashboard)
			{
			case StateStamperFrame.UTILIZATION:
				{
				String legend = "";
				for (int i=0; i<StateStamperFrame.STATES.length; i+=3) legend += "<tr><td bgcolor="+StateStamperFrame.STATES[i+1]+">___</td><td>"+StateStamperFrame.STATES[i]+"</td></tr>";
				return legend;
				}
			case StateStamperFrame.PRODUCTION:
				{				
				return "<tr><td bgcolor=00ff00>___</td><td>Lot Progress &lt 80%</td></tr>" +
				"<tr><td bgcolor=ffc800>___</td><td>Lot Progress &gt= 80%</td></tr>" +
				"<tr><td bgcolor=ff0000>___</td><td>Lot Progress &gt= 95%</td></tr>";
				}
			case StateStamperFrame.MAINTENANCE:
				{
				return "<tr><td bgcolor=00ff00>___</td><td>Current Site Perf &gt= 90%</td></tr>" +
				"<tr><td bgcolor=ffc800>___</td><td>Current site Perf &gt= 50%</td></tr>" +
				"<tr><td bgcolor=ff0000>___</td><td>Current Site Perf &lt 50% or Unsched Down</td></tr>";
				}
			case StateStamperFrame.ENGINEERING:
				{
				return "<tr><td bgcolor=00ff00>___</td><td>Last Hr Yield &gt= 80%</td></tr>" +
				"<tr><td bgcolor=ffc800>___</td><td>Last Hr Yield &gt= 50%</td></tr>" +
				"<tr><td bgcolor=ff0000>___</td><td>Last Hr Yield &lt 50%</td></tr>";
				}
			}
		return "";
		}

	/**
	 * Returns the value for a context or performance variable for a given machine
	 * @param machine The machine
	 * @param variable The variable
	 * @param isContext Whether it is a context variable or not
	 * @return The variable value
	 */
	private static String extract(Machine machine, String variable)
	{
	Object value = machine.context.get(variable);
	if (value == null) value = machine.performance.get(variable);
	
		if (variable.equals("TIME") && value != null) {
			String dateStateStart = null;
			Object currentState = machine.state;
			String time = "0d:0h:0m";

			if (currentState != null && !currentState.equals("Unknown")) {
				Object lastStateTime = machine.context.get("LAST_STATE_TIME");
				dateStateStart = lastStateTime.toString();
				Date current = new Date();
				String lastTimeStamp = dateStateStart;
				String currentTimeStamp = dateFormat.format(current);
				long duration;
				duration = diff(currentTimeStamp, lastTimeStamp);
				long seconds = duration / 1000;
				long minutes = seconds / 60;
				long hours = minutes / 60;
				long days = hours / 24;
				time = days+"d" + ":" + hours%24+"h" + ":" + minutes%60+"m";
			}
			value = time;
		}
	
	if (value == null) return "&nbsp";
	if (value instanceof Float)
		{
		if (((Float)value).isNaN() || ((Float)value).isInfinite()) return "&nbsp";
		return formatter.format(value)+"%";
		}
	String result = value.toString();
	return result.equals("")?"&nbsp":result;
	}
	
	private static String[] getExtendedStatusFields() throws Throwable
		{
		if(extendedStatusFields == null)
			{
			String fields = (String)Util.request("detailedStatus","getConfigurationKey.do");
			extendedStatusFields = fields.split(",");
			}
		return extendedStatusFields;
		}
	
	private static String[] getExtendedContexts() throws Throwable
		{
		if (extendedContexts == null)
			{
			String fields = (String)Util.request("detailedStatusContextBar","getConfigurationKey.do");
			extendedContexts = fields.split(",");
			}
		return extendedContexts;
		}

	private static String[] getExtendedSummings() throws Throwable
	{
	if (extendedSummings == null)
		{
		String fields = (String)Util.request("detailedStatusSummingsBar","getConfigurationKey.do");
		extendedSummings = fields.split(",");
		}
	return extendedSummings;
	}

	
	private static int[] getIndexes(Report report,String[] fields) throws Throwable
		{
		if(indexes==null)
			{
			indexes = new int[fields.length];
			int idx;
			String[] reportColumns = (String[])Util.request(report,"getReportDefinition.do");
			int fieldOffset = 4;
			boolean found;
			for(int i = 0; i< fields.length;i++)
				{
				idx = -1;
				found = false;
				for(int j = fieldOffset; j < reportColumns.length && !found;j++)
					{
					found = fields[i].equals(reportColumns[j]);
					idx = j-fieldOffset;
					}
				if(found){indexes[i]=idx;}
				}
			}
		return indexes;
		}
	
	}
