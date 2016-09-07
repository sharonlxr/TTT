package com.freescale.stateStamper.view;

import com.freescale.clientUtil.*;
import com.freescale.stateStamper.model.entity.EquipmentType;
import com.freescale.stateStamper.model.entity.Report;
import java.util.*;
import javax.swing.UIManager;

/**
 * Creates a new table to maintain the scheduled reports
 */

public class Reports extends Table
	{
	private Hashtable groupings;
	private EquipmentType eqType;
	
	/**
	 * Creates a new table to maintain the scheduled reports
	 * @param records A vector of Report objects
	 * @param groupings A hashtable with the grouping for each report type
	 * @param types A comma separated string of machine types
	 * @param locations A comma separated string of machine locations
	 */
	public Reports(Vector records, Hashtable groupings, String types, String locations,EquipmentType equipmentType)
		{
		super("Reports",records,700,400,null);
		this.groupings = groupings;
		Properties properties = (Properties)UIManager.get("gui.properties");
		properties.setProperty("ReportSetup.types.values",types);
		properties.setProperty("ReportSetup.locations.values",locations);
		eqType = equipmentType;
		}
	
	/**
	 * Overrides add method
	 */
	public void add() throws Throwable
		{
		Report report = new Report();
		report.setEquipmentType(eqType);
		Util.createWindow(type, new ReportSetup(this, report, groupings), this);
		}
	
	/**
	 * Overrides edit method
	 */
	public void edit() throws Throwable {Util.createWindow(type,new ReportSetup(this,((Report)records.elementAt(table.getSelectedRows()[0])),groupings),this);}
	
	
	public void search() throws Throwable
		{
		Report report = new Report();
		report.setEquipmentType(eqType);
		records = (Vector) Util.request(report, "search.do");
		if (sort && records != null) {Collections.sort(records, new Util.Compare());}
		model.fireTableRowsInserted(0, records.size() - 1);
		resize();
		}

	
	}