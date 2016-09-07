package com.freescale.stateStamper.view;

import com.freescale.clientUtil.*;
import com.freescale.clientUtil.Util.*;
import com.freescale.stateStamper.model.entity.Report;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.swing.*;

/**
 * Creates a panel to choose from the available dates for each period
 */
public class Dates extends TitlePanel 
	{
	private static final long serialVersionUID = 1L;
	private static final String[] PERIODS = {"Shift","Day","Week","Month"};
	private static SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
	private static List raw;
	private JTabbedPane tabbedPane;
	private Hashtable lists = new Hashtable(), selected = new Hashtable();
	private Form datesForm;
	
	/**
	 * Creates a panel to choose from the available dates for each period
	 * @param dates A hashtable with a vector of dates available for each period
	 */
	public Dates(Map dates)
		{
		tabbedPane = new JTabbedPane();
		Report report = new Report();
		report.endDate = new Date();
		datesForm = new Form("Raw",report,null);
		datesForm.setOpaque(true);
		tabbedPane.addTab("Raw",null,datesForm,null);
		raw = (List)dates.get("Raw");
		for (int i=0; i<PERIODS.length; i++) addList(PERIODS[i],(List)dates.get(PERIODS[i]));
		add(tabbedPane);
		add(new Form("Submit","",this));
		}
	
	/**
	 * Return a hashtable with 2 keys: period and dates (Object array with selected dates)
	 * @return
	 */
	public Hashtable getSelected() {return selected;}
	
	/**
	 * Add dates for raw files to selected hash table. Add 1 more day to end date to include times that fall in the next fiscal day
	 * @return An array of object with raw dates
	 */
	public static Object[] populateDates(Date startDate, Date endDate)
		{
		Vector aux = new Vector();
		String start = formatter.format(startDate), end = formatter.format(new Date(endDate.getTime()+86400000L)); // Format is 2009-01-01
		for (int i=0; i<raw.size(); i++)
			{
			String date = (String)raw.get(i), day = date.substring(0,10); // Format is 2009-01-01
			if (day.compareTo(start)>=0 && day.compareTo(end)<=0) aux.addElement(date);  // Include date for raw file if between start and end dates
			}
		return aux.toArray();	
		}
	
	/**
	 * Refresh existing dates with new ones 
	 * @param selected A hashtable with 2 keys: period and dates (Object array with selected dates)
	 */
	public void refresh(Map dates)
		{
		try{

			datesForm.setValue("endDate",new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
		}
		catch (Throwable ex){
			ex.printStackTrace();
		}		
		for (int i=0; i<PERIODS.length; i++)
			{
			DefaultListModel model = (DefaultListModel)((JList)lists.get(PERIODS[i])).getModel();  // Get existing list model for this period
			String date = (String)((Vector)dates.get(PERIODS[i])).lastElement();  // Get new last date for this period
			if (!date.equals(model.lastElement())) model.addElement(date); // If the new last date does not match the last existing date, add it to the list  
			}
		}
	
	/**
	 * Invoked when user clicks on OK button
	 * @param form The form with the button
	 */
	public void oK(Form form) throws Throwable
		{
		int index = tabbedPane.getSelectedIndex();
		String period = tabbedPane.getTitleAt(index);
		selected.put("Period",period);
		if (!period.equals("Raw")) selected.put("Dates",((JList)lists.get(period)).getSelectedValues());
		else
			{
			Report report = (Report)datesForm.getObject();
			selected.put("StartDate",report.startDate);
			selected.put("EndDate",report.endDate);
			selected.put("Dates",populateDates(report.startDate,report.endDate));			
			}
		form.cancel();
		}
	
	/**
	 * Invoked when user clicks on Cancel button
	 * @param form The form with the button
	 */
	public void cancel(Form form) {form.cancel();}
	
	/**
	 * Invoked when user clicks on Dismiss button
	 * @param form The form with the button
	 */
	public void dismiss(Form form) {form.cancel();}
	
	/**
	 * Creates a tab labeled period and adds a list populated with vector dates
	 * @param period The period
	 * @param dates A List with dates
	 */
	private void addList(String period, List dates)
		{
		DefaultListModel model = new DefaultListModel();
		for (int i=0; i<dates.size(); i++) model.addElement(dates.get(i));
		JList list = new JList(model);
		list.setVisibleRowCount(12);
		list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		lists.put(period,list);
		tabbedPane.addTab(period,null,new JScrollPane(list),null);
		}
	}