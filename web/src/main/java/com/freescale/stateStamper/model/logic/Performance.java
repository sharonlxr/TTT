package com.freescale.stateStamper.model.logic;

import java.util.*;

import com.freescale.stateStamper.model.entity.Machine;

public class Performance
	{
	private int[] cntGoodArray = new int[24], devProcessedArray = new int[24], sitesInsertionsArray = new int[24];
	private int[] cntGoodMinArray = new int[12], devProcessedMinArray = new int[12], oeeMinArray = new int[12];
	private Date lastTime, substateStart;
	private String lastLot, lastSubstate;
	private int lastIndex, lastIndexMin, lotCount, totalDevProcessed;
	private boolean clear, clearMin;

	public void update(Machine machine, Date time, String record, Hashtable columnIndex)
		{
		String[] contexts = new String[]{"SUBSTATE","LOT_ID","SITES_AVAILABLE"};
		String[] summings = new String[]{"INSERTIONS","DEVICES_PROCESSED","CNT_GOOD","TEST_TIME","HND_INDEX_TIME","LOT_COUNT"};
		Map context = new HashMap(), summing = new HashMap();
		for (int i=0; i<contexts.length; i++) context.put(contexts[i],ProcessRecord.getValue(contexts[i],record,null,columnIndex));
		for (int i=0; i<summings.length; i++)
			{
			String value = ProcessRecord.getValue(summings[i],record,null,columnIndex);
			if (value.equals("")) value = "0";
			summing.put(summings[i],Integer.valueOf(value));
			}
		update(machine,time,context,summing, new HashMap());
		}
	
	public void update(Machine machine, Date time, Map context, Map summing,Map performance)
		{
		try
			{
			long diff = 0;
			if (lastTime != null) diff = time.getTime()-lastTime.getTime();
			clear = diff > 24*3600*1000; clearMin = diff > 3600*1000; 
			int hour = time.getHours(), minute = time.getMinutes()/5;
	
			String lot = (String)context.get("LOT_ID");
			if (lot == null) lot = "";
			// If new lot is not empty and different form last lot, reset total device count 
			if (lastLot == null || (!lastLot.equals(lot) && !lot.equals("")))
				{
				totalDevProcessed = 0;
				lastLot = lot;
				}
			int cntGood = getValue(summing,"CNT_GOOD"), devProcessed = getValue(summing,"DEVICES_PROCESSED");
			int testTime = getValue(summing,"TEST_TIME"), indexTime = getValue(summing,"HND_INDEX_TIME");
			totalDevProcessed += devProcessed;
			int aux = getValue(summing,"LOT_COUNT");
			if (aux > 0) lotCount = aux;
			int sitesInsertions = 0;
			try {sitesInsertions = (int)Float.parseFloat((String)context.get("SITES_AVAILABLE")) * getValue(summing,"INSERTIONS");}
			catch (Throwable t) {}
			String substate = (String)context.get("SUBSTATE");
			if (substate==null) substate = "";
			if (lastSubstate == null || !lastSubstate.equals(substate))
				{
				substateStart = new Date();
				lastSubstate = substate;
				}
			Float lotProgress = (Float)machine.performance.get("LOT_PROGRESS");
			if(performance.get("LOT_PROGRESS") != null)
				{
				try{lotProgress = Float.valueOf( (String)performance.get("LOT_PROGRESS"));}
				catch(Exception e){lotProgress = Float.valueOf(0);}
				}
			else if(lotProgress == null){lotProgress = 0.0f;}
			if(lotProgress.floatValue() > 100.0f){lotProgress = Float.valueOf(100);}
			update(cntGoodArray,hour,cntGood);
			update(devProcessedArray,hour,devProcessed);
			update(sitesInsertionsArray,hour,sitesInsertions);	
			update(cntGoodMinArray,minute,cntGood);
			update(devProcessedMinArray,minute,devProcessed);
			if (substate.equals("Test") && sitesInsertions > 0) update(oeeMinArray,minute,cntGood*(testTime+indexTime)/sitesInsertions);
			else if (substate.equals("Retest") && devProcessed > 0) update(oeeMinArray,minute,cntGood*(testTime+indexTime)/devProcessed);
			else if (!substate.equals("Unknown")) update(oeeMinArray,minute,0); 

			long cntGoodTotal = total(cntGoodArray);
			long devProcessedTotal = total(devProcessedArray);
			long sitesInsertionsTotal = total(sitesInsertionsArray);
			long cntGoodMinTotal = total(cntGoodMinArray);
			long devProcessedMinTotal = total(devProcessedMinArray);
			long oeeMinTotal = total(oeeMinArray); 
			
			if (oeeMinTotal>3600000) oeeMinTotal= 3600000;
	
			machine.performance.put("SUBSTATE_START",substateStart);
			machine.performance.put("LAST_HR_OEE",new Float(oeeMinTotal*100F/3600000));
			machine.performance.put("LOT_PROGRESS",lotProgress);
			if (devProcessed>0 && sitesInsertions>0) machine.performance.put("CURRENT_SITE_PERF",new Float(devProcessed*100F/sitesInsertions));
			machine.performance.put("24_HR_SITE_PERF",new Float(devProcessedTotal*100F/sitesInsertionsTotal));
			machine.performance.put("LAST_HR_YIELD",new Float(cntGoodMinTotal*100F/devProcessedMinTotal));
			machine.performance.put("24_HR_YIELD",new Float(cntGoodTotal*100F/devProcessedTotal));
						
			lastIndex=hour;
			lastIndexMin = minute;
			lastTime = time;
			}
		catch (Throwable t) {t.printStackTrace();}
		}
	
	private int getValue(Map summing, String variable)
		{
		Integer value = (Integer)summing.get(variable);
		if (value != null) return value.intValue();
		else return 0;
		}
	
	private void update(int[] array, int index, int value)
		{
		if ((array.length==24 && clear) || (array.length==12 && clearMin)) Arrays.fill(array,0);  
		int last = array.length==24?lastIndex:lastIndexMin;
		if (last != index) // Clear buckets from last index
			{
			if (index < last)
				{
				for (int i=last+1; i<array.length; i++) array[i]=0;
				last = -1;
				}
			for (int i=last+1; i<=index; i++) array[i]=0;
			}
		array[index] += value;
		}
	
	private long total(int[] array)
		{
		long total = 0;
		for (int i=0; i<array.length; i++) total += array[i];
		return total;
		}
	}
