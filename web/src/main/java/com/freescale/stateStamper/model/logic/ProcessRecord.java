package com.freescale.stateStamper.model.logic;

import java.util.*;

import com.freescale.stateStamper.model.entity.Report;


/**
 * This class retrieves values from a record and applies any necessary function
 */

public class ProcessRecord
	{
	
	/**
	 * Process a record when generating a report
	 * @param data A hash table with the stored results for the report
	 * @param variables The variables to include in the report
	 * @param record The record
	 * @param date The fiscal date for the report
	 * @param variableIndex A hash table with the index of each variable in the record
	 */
	public static void process(Hashtable data, String[] variables, String record, String date, Hashtable variableIndex, long count,  int excludedcolums, boolean isText) throws Throwable
		{
		boolean summing = false;
		String categories = "";
		List values = new ArrayList();
		for (int i=excludedcolums; i<variables.length; i++)  // First elements are chart type, subtotals and condition
			{
			String[] aux;
			if(isFunctionExpandable(variables[i]))
				{
				aux = getFunctionParameters(variables[i]);
				}
			else
				{
				aux = variables[i].split("/"); // If there is a division
				}
			for (String var: aux)
				{
				String value = "";
				int index = var.indexOf("||");
				int index2 = var.indexOf("-"); 														
				if (index>0) // Concatenate two variables
					{
					String aux1 = getValue(var.substring(0,index),record,date,variableIndex);
					String aux2 = getValue(var.substring(index+2),record,date,variableIndex);
					value = aux1+" "+aux2;
					}
				else if (index2>0){
					String aux1 = getValue(var.substring(0,index2),record,date,variableIndex);
					String aux2 = getValue(var.substring(index2+1),record,date,variableIndex);
					value = aux1+"-"+aux2;
				}
				else value = getValue(var,record,date,variableIndex);
				if (isSumming(var)) {summing = true; values.add(new Float(value.equals("")?"0":value));}
				else categories += value + "\t";
				}
			}
		categories = categories.substring(0,categories.length()-1); // Remove last \t
		if (summing)
			{
			if (isText && count >= 65535)
				{
				throw new Throwable("Query exceeded maximum number of rows");
				}
			List val = (List)data.get(categories);
			if (val == null) data.put(categories,values);
			else for (int i=0; i<val.size(); i++) val.set(i,new Float(((Float)val.get(i)).floatValue()+((Float)values.get(i)).floatValue()));
			}
		else
			{
			if (count >= 65535) throw new Throwable("Query exceeded maximum number of rows");
			int index = categories.indexOf("\t");  // Extract first category
			String category = categories.substring(0,index);
			List val = (List)data.get(category);
			if (val == null) {val = new Vector(); data.put(category,val);}
			val.add(categories.substring(index+1));
			}
		}
	
	private static boolean isFunctionExpandable(String name)
		{
		return name.equals("OEE()") ||
			   name.equals("OEE_PERFORMANCE()");
		}
	
	private static String[] getFunctionParameters(String name)
		{
		String[] parameters = null; 
		if(name.equals("OEE()"))
			{
			parameters = new String[]{"SUM(DURATION)","OEE_INSERT(SITES_AVAILABLE)","SUM_OEE(CNT_GOOD)","SUM_OEE(TEST_TIME)","SUM_OEE(HND_INDEX_TIME)"};
			}
		if(name.equals("OEE_PERFORMANCE()"))
			{
			parameters = new String[]{"SUM(DURATION)","OEE_INSERT(SITES_AVAILABLE)","SUM_OEE(CNT_GOOD)","SUM_OEE(TEST_TIME)","SUM_OEE(HND_INDEX_TIME)", //OEE variables
					                  "TEST_INSERT(SITES_AVAILABLE)","SUM_TEST(DEVICES_PROCESSED)","SUM_TEST(CNT_GOOD)","SUM_TEST(TEST_TIME)","SUM_TEST(HND_INDEX_TIME)", //FIRST_PASS_SITE_PERF_LOSS variables
					                  "RETEST_INSERT(INSERTIONS)","SUM_RETEST(CNT_GOOD)","SUM_RETEST(TEST_TIME)","SUM_RETEST(HND_INDEX_TIME)", //RETEST_GAIN variables
					                  "SUM(CNT_GOOD)"};
			
			}
		return parameters;
		}

	public static boolean isPostProcessCondition(String condition)
		{
		return condition.contains("DUPLICATE(");
		}
	/**
	 * Performs any needed divisions when generating a report
	 * @param data A hash table with the stored results for the report
	 * @param Report The report object
	 */
	public static void postProcess(Map data, Report report, int excludedcolums)
		{
		String[] variables = report.getColumns();
		List<Integer> divide = new Vector();
		Map roundto100percent = new HashMap();
		int index = 0;
		if(isPostProcessCondition(report.condition))
			{
			removeDuplicates(report,data);
			}
		for (int i=excludedcolums; i<variables.length; i++) //First elements are chart type, subtotals and condition
			{
			if (variables[i].indexOf("/") != -1) divide.add(Integer.valueOf(index));		
			if (isRoundto100percent(variables[i])) roundto100percent.put(Integer.valueOf(index),Integer.valueOf(index));
			if (isSumming(variables[i])) index++;
			}
		for(Object key:data.keySet())
			{
			List values = (List)data.get(key);
			String postFunction = variables[variables.length-1]; 
			if(postFunction.equals("OEE()"))
				{
				float oee = oee(values);
				values.clear();
				values.add(oee);
				}
			else
				{
				if(divide.size() == 0 ){return;}
				for (Integer iDivide :divide)
					{
					index = iDivide.intValue();
					if (((Float)values.get(index+1)).floatValue() == 0) values.set(index,Float.valueOf(0));
					else
						{ 
						Float tmpvalue = new Float(((Float)values.get(index)).floatValue()/((Float)values.get(index+1)).floatValue());
						if (roundto100percent.containsKey(Integer.valueOf(index)) && tmpvalue > 100.0f)
							{
							values.set(index, Float.valueOf(100.0f));
							}
						else
							{
							values.set(index, tmpvalue);
							}
						}
					values.remove(index+1);
					}
				}
				
			}
		}
	
	public static float oee(List<Float> values)
		{
		float oee = 0;
		Float duration = values.get(0);
		Float insertions = values.get(1);
		Float countGood = values.get(2);
		Float testTime = values.get(3);
		Float handlerIndexTime = values.get(4);
		if(insertions == 0 || duration == 0){return 0;}
		oee = ((countGood/(insertions))*(testTime+handlerIndexTime))/duration;
		oee *=100;
		return oee;
		}
	
	public static float firstPassSitePerfLoss(List<Float> values)
		{
		float firstPassSitePerfLoss = 0;
		float insertions = values.get(0);
		float devices = values.get(1);
		float goodCount = values.get(2);
		float testTime = values.get(3);
		float indexTime = values.get(4);
		float duration = values.get(5);
		if(insertions == 0 || devices == 0){return 0;}
		firstPassSitePerfLoss=((insertions-devices)/insertions)*(testTime+indexTime)/duration*(goodCount/devices);
		firstPassSitePerfLoss *=100;
		return firstPassSitePerfLoss;
		}
	
	public static float firstPassYieldLoss(List<Float> values)
		{
		float firstPassYieldLoss = 0;
		float devicesProcessed = values.get(0); 
		float goodCount = values.get(1);
		float testTime = values.get(2);
		float indexTime = values.get(3);
		float duration = values.get(4);
		if(devicesProcessed == 0 || duration == 0){return 0;}
		firstPassYieldLoss = (devicesProcessed-goodCount)/devicesProcessed *
                             ((testTime+indexTime)/duration);
		firstPassYieldLoss *=100;
		return firstPassYieldLoss;
		}
	
	public static float retestGain(List<Float> values)
		{
		float insertions = values.get(0);
		float goodCount = values.get(1);
		float testTime = values.get(2);
		float indexTime = values.get(3);
		float duration = values.get(4);
		if(insertions == 0 || duration == 0){return 0;}
		float retestGain =(goodCount/insertions) * 
		           ((testTime+indexTime)/duration);
		retestGain *= 100;
		return retestGain;
		}
	
	/**
	 * Get the value for a given variable for a record in the data files
	 * @param variable The variable
	 * @param record A record from the data files
	 * @param date The fiscal date
	 * @param variableIndex A hahstable with the position of each variable in the record
	 * @return
	 */
	public static String getValue(String variable, String record, String date, Hashtable variableIndex)
		{
		String function = null;
		if (variable.equals("ALL")) return "All";
		if (variable.equals("BLANK")) return " ";
		if (variable.equals("DATE")) return date;
		int index1 = variable.indexOf("(");
		if (index1 != -1)
			{
			function = variable.substring(0,index1);
			variable = variable.substring(index1+1,variable.length()-1);
			}
		Integer index = (Integer)variableIndex.get(variable);
		if (index == null) 
		{
			if (variable.contains("\\")&& function!=null && function.startsWith("COUNTERBAR")){
				return function(function,variable,record,date,variableIndex);
				//for COUNTERBAR function with the format COUNTERBAR(usage\limit)	
			}
			else {
				return ""; // If variable not found in record, return empty string
			}
		}
			int index2 = index.intValue(), index3 = -1, index4;
		for (int i=0; i<index2; i++) index3 = record.indexOf("\t",index3+1);
		index4 = record.indexOf("\t",index3+1);
		if (index4 == -1) index4 = record.length();
		String value = record.substring(index3+1,index4);
		if (variable.equals("SUBSTATE") && value.equals("")) return "Unknown";
		if (function != null) value = function(function,value,record,date,variableIndex);
		return value;
		}
	
	/**
	 * Returns true if the parameter is a summing variable
	 * @param variable
	 * @return
	 */
	public static boolean isSumming(String variable)
		{
		return variable.startsWith("SUM(") || 
		       variable.startsWith("SITE_PERFORMANCE(") || 
		       variable.startsWith("OEE(") || 
		       variable.startsWith("UOEE(") ||
		       variable.startsWith("FIRST_PASS_SITE_PERF_LOSS(") || 
		       variable.startsWith("FIRST_PASS_YIELD_LOSS(") || 
		       variable.startsWith("RETEST_GAIN(") || 
		       variable.startsWith("YIELD_LOSS(")||
		       variable.startsWith("TOTAL_RETEST_QTY(")||
		       variable.startsWith("TOTAL_FIRST_PASS_QTY(")||
		       variable.startsWith("POPUP")||
		       variable.startsWith("ENABLED_SITE_COUNT(")||
		       variable.startsWith("TOTAL_SITES_AVAILABLE(") ||
		       variable.startsWith("SUM_OEE(") ||
			   variable.startsWith("SUM_TEST(")||
			   variable.startsWith("SUM_RETEST(") ||
			   variable.startsWith("SUM_PRODUCTIVE(") ||
			   variable.startsWith("OEE_INSERT(")||
			   variable.startsWith("RETEST_INSERT(") ||
			   variable.startsWith("TEST_INSERT(") ||
			   variable.startsWith("ENABLED_BIN8_COUNT(") ||
			   variable.startsWith("TOTAL_BIN8_AVAILABLE(")
			   ;
		}
	
	public static boolean isRoundto100percent(String variable)
		{
		return variable.startsWith("UOEE(");
		}

	/**
	 * Returns the handler type for a given handler ID
	 * @param handlerId The handler ID
	 * @return The handler type
	 */
	public static String handlerType(String handlerId) 
		{
		if (handlerId.startsWith("CAS")) return "Castle";
		else if (handlerId.startsWith("MX") || handlerId.toUpperCase().startsWith("MATRIX")) return "Matrix";
		else return "UTS";
		}
	
	/**
	 * Apply a function and return its value
	 * @param function The function to apply
	 * @param value An optional parameter to the function 
	 * @param record The record being processed
	 * @param date The date
	 * @param variableIndex A hashtable with the index of each variable in the record
	 * @return The function value
	 */
	private static String function(String function,String value, String record, String date, Hashtable variableIndex)
		{
		if (function.equals("TYPE")) value = StateTransition.getType(value);
		if (function.equals("LOCATION")) value = StateTransition.getLocation(value);
		if (function.equals("PLATFORM")) value = StateTransition.getPlatform(value);
		else if (function.equals("GROUP")) value = StateTransition.getGroup(value);
		else if (function.equals("NOT_NULL")) value = ""+(value.length()>0 && !value.equals("0") && !value.equals("0 0") && !value.equals("0.0"));
		else if (function.equals("OEE"))
			{
			String substate = getValue("SUBSTATE",record,date,variableIndex);  
			if (substate.equals("Test") || substate.equals("Retest")) value = ""+siteYieldPerformance(record,date,variableIndex)*100;
			else value = "0";
			}
		else if (function.equals("SUM_OEE"))
			{
			String substate = getValue("SUBSTATE",record,date,variableIndex);  
			if (!substate.equals("Test") && !substate.equals("Retest")) value = "0";
			}
		else if (function.equals("SUM_PRODUCTIVE"))
			{
			if (!getValue("STATE",record,date,variableIndex).equals("Productive"))  value = "0";
			}
		else if(function.equals("SUM_TEST"))
			{
			if (!getValue("SUBSTATE",record,date,variableIndex).equals("Test")) value = "0";
			}
		else if(function.equals("SUM_RETEST"))
			{
			if (!getValue("SUBSTATE",record,date,variableIndex).equals("Retest")) value = "0";
			}
		else if (function.equals("OEE_INSERT"))
			{
			String substate = getValue("SUBSTATE",record,date,variableIndex);  
			if (substate.equals("Test") || substate.equals("Retest")) value = String.valueOf(sitesAvailable(record,date,variableIndex));
			else value = "0";
			}
		else if (function.equals("TEST_INSERT"))
			{
			String substate = getValue("SUBSTATE",record,date,variableIndex);  
			if (substate.equals("Test") ) value = String.valueOf(sitesAvailable(record,date,variableIndex));
			else value = "0";
			}
		else if (function.equals("RETEST_INSERT"))
			{
			String substate = getValue("SUBSTATE",record,date,variableIndex);  
			if (substate.equals("Retest") ) value = String.valueOf(sitesAvailable(record,date,variableIndex));
			else value = "0";
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
			else value = ""+siteYieldPerformance(record,date,variableIndex)*100;
			}
		else if (function.equals("SITE_PERFORMANCE"))
			{
			value = ""+sitePerformance(record,date,variableIndex)/100;			
			}
		else if (function.equals("RESPONSE_TIME"))
		    {
			value = ProcessRecord.convertElapsedTime(value);
		    }
		else if (function.equals("TRUE"))
	    	{
			if (!value.equalsIgnoreCase("N")) 
				value = "true";
			else
				value = "false";	
	    	}
		else if (function.equals("FALSE"))
	    	{
			if (value.equalsIgnoreCase("N")) 
				value = "true";
			else
				value = "false";	
	    	}
		else if (function.equals("TOTAL_RETEST_QTY")){
			if(getValue("TRUE(RETEST)",record,date, variableIndex).equals("true"))
				return  getValue("DEVICES_PROCESSED",record,date,variableIndex);
			else 
				return  "0";
		}
		
	    else if (function.equals("TOTAL_FIRST_PASS_QTY")){
	    	if(getValue("TRUE(RETEST)",record,date, variableIndex).equals("false"))
				return  getValue("DEVICES_PROCESSED",record,date,variableIndex);
			else 
				return  "0";
		}		
	    else if (function.equals("POPUP")){
				return  "1";
		}else if(function.equals("TRIM")){
			value = trim(getValue("TIME",record,date,variableIndex));
		}else if(function.equals("PRODUCTIVE")){
			value = String.valueOf(getValue("STATE",record,date,variableIndex).equals("Productive")); 
		}else if(function.equals("SUBSTATE_TEST")){
			value = String.valueOf(getValue("SUBSTATE",record,date,variableIndex).equals("Test")); 
		}else if(function.equals("RETEST_YN")){
		if(StateStamper.getStage().equalsIgnoreCase("PRB"))
			{
			value = getValue("REPROBE",record,date,variableIndex);
			}
			if(value == null || value.equals("") || value.equalsIgnoreCase("N")||value.equalsIgnoreCase("0") || value.equalsIgnoreCase("Unknown"))
				value = "N";
			else
				value="Y"; 
		}else if(function.equals("ELAPSEDINDAYS")){
			value = FiscalDate.getTimeElapsedinDays(value) + " days";
		
		}else if(function.equals("EQUIPMENTTYPE")){
			value = DurableManager.getEquipmentTypebyDurableID(value).name;
		
		}else if(function.equals("TIME_IN_STATE")){
			try
				{
				value = FiscalDate.getElapsedTime(value);
				}
			catch (Exception e)
				{
				value = "";
				}
		}else if(function.equals("TIME_CONVERSION")){
		try
			{
			value = FiscalDate.getElapsedTime(Long.parseLong(value));
			}
		catch (Exception e)
			{
			value = "";
			}
		}else if(function.equals("LOCALTIME")){
		    try{value = FiscalDate.getLocalTimeFromUTC(value);}
		    catch (Exception e){e.printStackTrace();value = "";}
		}else if(function.equals("UNK")){
		    if(value == null || value.equals("")){value="UNKNOWN";}
		}else if(function.equals("NORM")){
		     if(value == null || value.equals("")){value="UNKNOWN";}
		     value = value.toUpperCase();
		}else if(function.equals("LOT_TYPE")){
		     value = String.valueOf("PROD".equalsIgnoreCase(getValue("LOT_TYPE",record,date,variableIndex)));
		}else if(function.equals("PERM")){
			value = value.replace(" ","").replace("\\","").replace("/","").toUpperCase();
			                      
		}else if(function.equals("COUNTERBAR")||function.equals("COUNTERBAR_TIME")||function.equals("COUNTERBAR_WW")){							
			value = getCounterBarLabel( value, record,  date, variableIndex);
	    }
		
	    else if(function.equals("COUNTERBAR_INVERSE")){
			value = getReverseCounterBarLabel( value, record,  date, variableIndex);		
	    }else if(function.equals("ENABLED_SITE_COUNT")){
			value = getEnabledSiteCount( value, record,  date, variableIndex);		
	    }else if(function.equals("AREA")){
	    	value = StateTransition.getAreaPK(value);
	    }else if(function.equals("GET_FULL_NAME")){
	    	value = StateTransition.getUserFullName(value);
	    }else if(function.equals("ENABLED_BIN8_COUNT")){
			value = getEnabledBin8Count( value, record,  date, variableIndex);		
	    }else if(function.equals("AREA_N")){//AREA_N returns the name of an area for a given MID
    	    value = StateTransition.getAreaNameByEquipmentName(value);
    		if(value.equals("")){value = "Unknown";}
        }else if (function.equals("PNP_AVAILABLE"))
        	{
        	String pnp  = getValue("PNP_SITES_AVAILABLE",record,date,variableIndex);
        	String ipnp = getValue("IPNP_SITES_AVAILABLE",record,date,variableIndex);
        	String opnp = getValue("OPNP_SITES_AVAILABLE",record,date,variableIndex);
        	String tpnp = getValue("TPNP_SITES_AVAILABLE",record,date,variableIndex);
        	boolean available = (pnp  != null && !pnp.equals("")  && !pnp.equals("0") ) ||
        			            (ipnp != null && !ipnp.equals("") && !ipnp.equals("0")) ||
        			            (opnp != null && !opnp.equals("") && !opnp.equals("0")) ||
        			            (tpnp != null && !tpnp.equals("") && !tpnp.equals("0"));
        	value =  Boolean.valueOf(available).toString();
        	}
		return value;
		}
		
	/**
	 * Return the label value of CounterBar or CounterBar_time 
	 * @param record value 
	 * @param record The record being processed
	 * @param date The date
	 * @param variableIndex A hashtable with the index of each variable in the record
	 * @return the label value for the counter bar
	 */
	
	private static  String getCounterBarLabel(String value, String record, String date, Hashtable variableIndex) {
		//time base value does not take the "countTimeElapse" into account at here. It will processed out side
		int slashIdx = value.indexOf("\\");
		int length = value.length();
		if( slashIdx == -1){return "";}

		String limit   = value.substring(slashIdx+1,length);
		String usage   = value.substring(0,slashIdx);						
		String usageValue = getValue(usage,record,date,variableIndex);
		String limitValue = getValue(limit,record,date,variableIndex);
		
		usageValue = usageValue.equalsIgnoreCase("")|| usageValue==null?"0":usageValue;
		limitValue = limitValue.equalsIgnoreCase("")||limitValue==null?"0":limitValue;
		
		if (limitValue.equals("0")){
			return "-";
		}
		
		String percentage = DurableManager.getPercentage(usageValue, limitValue);						
		value = usageValue +"\\"+limitValue+" ("+percentage+"%)";			
		return value;
	}
	
	
	/**
	 * Return the label value of CounterBar or CounterBar_time 
	 * @param record value 
	 * @param record The record being processed
	 * @param date The date
	 * @param variableIndex A hashtable with the index of each variable in the record
	 * @return the label value for the counter bar
	 */
	
	private static String getReverseCounterBarLabel(String value, String record, String date, Hashtable variableIndex) {
		int slashIdx = value.indexOf("\\");
		int length = value.length();
		if( slashIdx == -1){return "";}
		
		String limit   = value.substring(slashIdx+1,length);
		String usage   = value.substring(0,slashIdx);	
								
		String usageValue = getValue(usage,record,date,variableIndex);
		String limitValue = getValue(limit,record,date,variableIndex);
		
		usageValue = usageValue.equalsIgnoreCase("")|| usageValue==null?"0":usageValue;
		limitValue = limitValue.equalsIgnoreCase("")||limitValue==null?"0":limitValue;	
		
		if (limitValue.equals("0")){
			return "-";
		}
		
		String percentage = DurableManager.getPercentage(limitValue,usageValue);
		value = usageValue +"\\"+limitValue+" ("+percentage+"%)";				
		return value;
	}
	
	private static String getEnabledSiteCount(String value, String record, String date, Hashtable variableIndex)
		{
		int siteAvailable=getIntValue(value);
		int result = 0;
		for (int i = 0; i < siteAvailable; i++)
			{
			int hb1Count = getIntValue(getValue("SITE" + i + "_HB1", record, date, variableIndex));
			int hb$Count = getIntValue(getValue("SITE" + i + "_HB$", record, date, variableIndex));
			if (hb1Count==0&& hb$Count==0)
				{
				continue;
				}
			else result++;
			}
		return String.valueOf(result);
		}
	
	private static String getEnabledBin8Count(String value, String record, String date, Hashtable variableIndex)
	{
	int hardBin8AllSite=32;
	int result = 0;
	for (int i = 0; i < hardBin8AllSite; i++)
		{
		int hb1Count = getIntValue(getValue("SITE" + i + "_HB8", record, date, variableIndex));
		result = result+hb1Count;
		}
	return String.valueOf(result);
	}
	
	private static int getIntValue(String value)
		{
		int result = 0;
		try
			{
			Double doubleValue = Double.valueOf(value);
			result = doubleValue.intValue();
			}
		catch (NumberFormatException e)
			{
			result = 0;
			}
		return result;
		}
	/**
	 * Return test plus index time for given record
	 * @param record The record being processed
	 * @param date The date
	 * @param variableIndex A hash table with the index of each variable in the record
	 * @return Test plus index time
	 */
	private static float testIndexTime(String record, String date, Hashtable variableIndex)
		{
		float testTime = Float.parseFloat(getValue("TEST_TIME",record,date,variableIndex));
		float indexTime = Float.parseFloat(getValue("HND_INDEX_TIME",record,date,variableIndex));
		return (testTime+indexTime);
		}
		
	
	/**
	 * Return site yield performance for given record
	 * @param record The record being processed
	 * @param date The date
	 * @param variableIndex A hash table with the index of each variable in the record
	 * @return Site performance loss
	 */
	private static float siteYieldPerformance(String record, String date, Hashtable variableIndex)
		{
		float sitesAvailable;
		try {sitesAvailable = Float.parseFloat(getValue("SITES_AVAILABLE",record,date,variableIndex)) * Float.parseFloat(getValue("INSERTIONS",record,date,variableIndex));}
		catch (Throwable t) {sitesAvailable = 0;}
		if (sitesAvailable == 0) return 0;
		float good = Float.parseFloat(getValue("CNT_GOOD",record,date,variableIndex));
		float testTime = Float.parseFloat(getValue("TEST_TIME",record,date,variableIndex));
		float indexTime = Float.parseFloat(getValue("HND_INDEX_TIME",record,date,variableIndex));
		float oee = (good/sitesAvailable)*(testTime+indexTime);
		return oee;
		}

	/**
	 * Return site yield performance for given record
	 * @param record The record being processed
	 * @param date The date
	 * @param variableIndex A hash table with the index of each variable in the record
	 * @return Site performance loss
	 */
	private static float sitesAvailable(String record, String date, Hashtable variableIndex)
		{
		float sitesAvailable = 0;
		float insertions;
		try
			{
			sitesAvailable = Float.parseFloat(getValue("SITES_AVAILABLE", record, date, variableIndex));
			insertions = Float.parseFloat(getValue("INSERTIONS", record, date, variableIndex));
			sitesAvailable *= insertions; 
			}
		catch (Throwable t)
			{
			sitesAvailable = 0;
			}
		return sitesAvailable;
		}

	/**
	 * Return yield loss for given record
	 * @param record The record being processed
	 * @param date The date
	 * @param variableIndex A hash table with the index of each variable in the record
	 * @return Yield loss
	 */
	private static float yieldLoss(String record, String date, Hashtable variableIndex)
		{
		float devices = Float.parseFloat(getValue("DEVICES_PROCESSED",record,date,variableIndex));
		if (devices == 0) return 0;
		float good = Float.parseFloat(getValue("CNT_GOOD",record,date,variableIndex));
		float testTime = Float.parseFloat(getValue("TEST_TIME",record,date,variableIndex));
		float indexTime = Float.parseFloat(getValue("HND_INDEX_TIME",record,date,variableIndex));
		return (devices-good)/devices*(testTime+indexTime);
		}
	
	/**
	 * Return site performance loss for given record
	 * @param record The record being processed
	 * @param date The date
	 * @param variableIndex A hash table with the index of each variable in the record
	 * @return Site performance loss
	 */
	private static float sitePerformanceLoss(String record, String date, Hashtable variableIndex)
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

	
	/**
	 * Return site performance for given record
	 * @param record The record being processed
	 * @param date The date
	 * @param variableIndex A hashtable with the index of each variable in the record
	 * @return Site performance
	 */
	private static float sitePerformance(String record, String date, Hashtable variableIndex)
		{
		float sitesAvailable;
		try {sitesAvailable = Float.parseFloat(getValue("SITES_AVAILABLE",record,date,variableIndex)) * Float.parseFloat(getValue("INSERTIONS",record,date,variableIndex));}
		catch (Throwable t) {sitesAvailable = 0;}
		return sitesAvailable;
		}
	/**
	 * compute elapsed time in milliseconds to "HH:MM:SS"  
	 * @param the elapsed time in milliseconds
	 * @return the elapsed time in the format of "HH:MM:SS"
	 */
	private static String convertElapsedTime(String elapsedTimeStr)
		{
		String time = "";
		try
			{
			long elapsedTime = Long.parseLong(elapsedTimeStr);
			String format = String.format("%%0%dd", 2);
			elapsedTime = elapsedTime / 1000;
			String seconds = String.format(format, elapsedTime % 60);
			String minutes = String.format(format, (elapsedTime % (60 * 60)) / 60);
			String hours = String.format(format, elapsedTime / (60 * 60));
			time = hours + ":" + minutes + ":" + seconds;
			}
		catch(NumberFormatException nfe)
			{
			time = "--:--:--";
			}
		catch (Throwable t)
			{
			}
		return time;
		}


	private static int getColumnIndex(String[]columns,String column)
		{
		int idx = -1;
		int offset = 4; // First elements are chart type, subtotals and condition and LOCATION(MID)
		for(int i = 0; i < columns.length && idx == -1;i++)
			{
			if(column.equals(columns[i])) idx = i;
			}
		idx-= offset;
		return idx;
		}
	
	private static String getDuplicateVariable(Report report)
		{
		String var = null;
		String DUP = "DUPLICATE(";
		int start = report.condition.indexOf(DUP);
		var = report.condition.substring(start + DUP.length(), report.condition.indexOf(")", start));
		return var;
		}
	
	private static SortedMap getOrderedRecords(Report report,List<String> entries)
		{
		SortedMap orderedEntries = new TreeMap();
		String variable = getDuplicateVariable(report); 
		int idxMid  = getColumnIndex(report.getColumns(), "MID");
		int idxTime = getColumnIndex(report.getColumns(), "TIME");
		int idxVar  = getColumnIndex(report.getColumns(), variable);
		String mid, time, var, key;
		String values[];
		for (int i = 0; i < entries.size(); i++)
			{
			String value = entries.get(i);
			values = value.split("\t");
			mid = values[idxMid];
			var = values[idxVar];
			time = values[idxTime];
			key = mid + "%" + var + "%" + time;
			// store the indexes to remove them from the original list
			orderedEntries.put(key, Integer.valueOf(i));
			}
		return orderedEntries;
		}
	
	private static List filterDuplicateRecords(SortedMap orderedRecords,List records)
		{
		List filteredEntries = new Vector();
		String mid, time, var, key;
		String values[];
		int FIVE_MINUTES = 60*5;
		// remove duplicates
		String lastMid = "", lastVar = "", lastTime = "";
		for (Iterator<String> itOrd = orderedRecords.keySet().iterator(); itOrd.hasNext();)
			{
			key = itOrd.next();
			values = key.split("%");
			mid = values[0];
			var = values[1];
			time = values[2];
			if (!mid.equals(lastMid)){lastMid = mid; lastTime = time;}
			if (!var.equals(lastVar)){lastVar = var; lastTime = time;}
			if (mid.equals(lastMid) && var.equals(lastVar))
				{
				long diff = FiscalDate.diff(time, lastTime);
				boolean duplicate = diff > 0 && diff < FIVE_MINUTES;
				if (!duplicate)
					{
					lastTime = time;
					Integer idx  = (Integer)orderedRecords.get(key);
					filteredEntries.add(records.get(idx.intValue()));
					}
				}
			}
		return filteredEntries;
		}
	
	private static void removeDuplicates(Report report, Map data)
		{
		if( !report.period.equals(FiscalDate.RAW) )return;
		for (Iterator it = data.keySet().iterator(); it.hasNext();)
			{
			String dataKey = (String)it.next();
			List<String> records = (List)data.get(dataKey);
			SortedMap orderedRecords = getOrderedRecords(report,records);
			List filteredRows = filterDuplicateRecords(orderedRecords,records);
				
			data.put(dataKey, filteredRows);
			}
		}

	private static String trim(String time){
		if(time!=null) return time.replace("-","");
		else return null;
	}
	
	
	}