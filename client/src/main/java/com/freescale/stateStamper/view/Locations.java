package com.freescale.stateStamper.view;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import com.freescale.clientUtil.Form;
import com.freescale.clientUtil.Table;
import com.freescale.clientUtil.Util;
import com.freescale.stateStamper.model.entity.*;

public class Locations extends Table {
	private static final long serialVersionUID = 1L;
	private StateStamperFrame stateStamperFrame;
	private String fieldsObj;
	private String lenObj;

	public Locations(StateStamperFrame stateStamperFrame) throws Throwable {
		type = "Locations";
		init(true);
		this.stateStamperFrame = stateStamperFrame;
		fieldsObj = Util.getProperty(type + ".properties");
		lenObj = Util.getProperty(type + ".length");
		search();
	}

	public void search() throws Throwable {
		setPreferredSize(new Dimension(Util.WIDTH / 2, Util.HEIGHT));
		records = (Vector) Util.request(new Location(), "search.do", false);
		model.fireTableRowsInserted(0, records.size() - 1);
	}

	public void save() throws Throwable {
		Util.request(records, "store.do");
		cancel();
		Util.createWindow("Success", new Form("Success", "", this), this);
	}

	public void save(LocationForm form) throws Throwable {
		Location location = (Location) form.getObject();
		super.save(form);
	}

	public void delete() throws Throwable {
		super.delete();
	}

	public void add() throws Throwable {
		Location location = new Location();
		resetProperty(location);
		if (!editable) {
			Util.createWindow(type, new LocationForm(type, location, this),
					this);
		} else {
			int index = records.size();
			if (getSelectedRows().length > 0
					&& getSelectedRows()[0] < records.size())
				index = getSelectedRows()[0];
			records.insertElementAt(
					Class.forName(Util.getProperty(type + ".object"))
							.newInstance(), index);
			rowsInserted(0, records.size() - 1);
		}
	}

	public void cancel(LocationForm form) throws Throwable {
		form.cancel();
		search();
	}

	public void edit() throws Throwable {
		Location location = (Location) (records.elementAt(table
				.getSelectedRows()[0]));
		resetProperty(location);
		if (!editable) {
			Util.createWindow(type, new LocationForm(type, location, this),
					this);
		}
	}

	private void resetProperty(Location location) throws Throwable {
		Util.setProperty(type + ".properties", fieldsObj);
		Util.setProperty(type + ".length", lenObj);
	}

}
