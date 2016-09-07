package com.freescale.stateStamper.view;

import com.freescale.clientUtil.*;
import com.freescale.stateStamper.model.entity.Report;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

/**
 * Creates a tree to add/edit a report
 */

public class ReportSetup extends Tree implements ActionListener 
	{
	private static final long serialVersionUID = 4922376685529009896L;
	private static final Object[] ITEMS = new Object[]{"Exclusion Setup","Emails Setup"};
	private Reports reports;
	private Table table;
	private Form form;
	private Report report;
	private Hashtable groupings;
		
	/**
	 * Constructor for this tree
	 * @param report A report object
	 * @param groupings A hashtable with the grouping for each report type
	 * @throws Throwable
	 */
	public ReportSetup(Reports reports, Report report, Hashtable groupings) throws Throwable
		{
		super("Report Setup",ITEMS,null,new int[]{190,520,478});
		this.reports = reports;
		this.report = report;
		if (report.exclusions ==null) report.exclusions = new Vector();
		if (report.emails ==null) report.emails = new Vector();
		this.groupings = groupings;
		selected = getRoot();
		expand();
		JCheckBox includeAll = (JCheckBox)form.getComponent("includeAll"); 
		includeAll.addActionListener(new includeAllListener());
		}
	
	/**
	 * Override expand method
	 * @throws Throwable
	 */
	public void expand() throws Throwable
		{
		if (form != null) form.getObject();
		if (table != null) table.finishEdit();
		JComponent component = null;
		if (selected.getParent() == null)
			{			
			Form form = new Form("Report Setup",report,this,0);
			form.setValue("reportType",report.reportType);
			form.setValue("grouping",report.grouping);
			JCheckBox includeAll = (JCheckBox)form.getComponent("includeAll");
			enableTypeLocations(form,!includeAll.isSelected());
			component = form;
			}
		else
			{			
			String value = selected.getUserObject().toString();
			if (value.equals("Exclusion Setup")) component = new Table("Exclusion Setup",report.exclusions,null,this,true);
			else if (value.equals("Emails Setup")) component = new Table("Emails Setup",report.emails,null,this,true);
			}
		if (component != null)
			{
			if (component instanceof Form) form = (Form)component;
			else form = null;
			if (component instanceof Table) table = (Table)component;
			else table = null;
			add(component,false,true);
			}
		}
	
	private void enableTypeLocations(Form form,boolean enable)
		{
		JList types =  (JList)form.getComponent("types");
		JList locations =  (JList)form.getComponent("locations");
		types.setEnabled(enable);
		locations.setEnabled(enable);
		}
	
	class includeAllListener implements ActionListener
		{
		public void actionPerformed(ActionEvent arg0)
			{
			JCheckBox includeAll = (JCheckBox)arg0.getSource();
			if(includeAll.isSelected())
				{
				JList types =  (JList)form.getComponent("types");
				JList locations =  (JList)form.getComponent("locations");
				types.clearSelection();
				locations.clearSelection();
				}
			enableTypeLocations(form,!includeAll.isSelected());
			}
		}
	
	/**
	 * Invoked when a selected in changed in a combo box
	 * @param form The form that contains the combo box
	 * @param field The combo box field
	 * @param value The new value
	 * @throws Throwable
	 */
	public void fieldChanged(Form form, String field, Object value) throws Throwable {
			if(value==null)return;
			Vector v = (Vector)groupings.get(value);
		form.setValues("grouping",v);
		}

	/**
	 * Dismiss a form
	 * @param form The form to close
	 * @throws Throwable
	 */
	public void dismiss(Form form) throws Throwable {form.cancel();}
	
	/** 
	 * Save a table (exclusions or emails)
	 * @param table The table to save
	 * @throws Throwable
	 */
	public void save(Table table) throws Throwable {table.finishEdit(); save();}

	/**
	 * Saves a form
	 * @param form The form to save
	 * @throws Throwable
	 */
	public void save(Form form) throws Throwable {save();}
			
	/**
	 * Saves the report
	 * @throws Throwable
	 */
	private void save() throws Throwable
		{
		if (form != null) form.getObject();
		Util.request(report,"store.do");
		((Window)getTopLevelAncestor()).dispose();
		reports.search();
		Util.createWindow("Success",new Form("Success","",this,0),this);
		}
	
	
	
	}