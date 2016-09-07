package com.freescale.stateStamper.view;

import java.awt.Dimension;
import java.util.List;
import java.util.Vector;

import com.freescale.clientUtil.Form;
import com.freescale.clientUtil.Table;
import com.freescale.clientUtil.Util;
import com.freescale.stateStamper.model.entity.Area;

public class AreaTable extends Table
	{

	private static final long serialVersionUID = -5430462058844830664L;

	public AreaTable(StateStamperFrame stateStamperFrame) throws Throwable
		{
		type = "Area";
		init(true);
		search();
		}

	public void search() throws Throwable
		{
		setPreferredSize(new Dimension(Util.WIDTH / 2, Util.HEIGHT));
		records = (Vector) Util.request(new Area(), "search.do", false);
		model.fireTableRowsInserted(0, records.size() - 1);
		}

	public void save() throws Throwable
		{
		Util.request(records, "store.do");
		cancel();
		Util.createWindow("Success", new Form("Success", "", this), this);
		search();
		}

	@SuppressWarnings("unchecked")
	public void save(Form form) throws Throwable
		{
		Area area = (Area) form.getObject();
		int count = 0;
		for(Area temp: (List<Area>)records)
			{
			if(temp.getName().equalsIgnoreCase(area.getName()))
				{
				count++;
				}
			}
		if(count>1)throw new Exception("Duplicate area");
		super.save(form);
		}

	public void delete() throws Throwable
		{
		int idx = table.getSelectedRow();
		if(idx<0)return;
		Area area = (Area)records.get(idx);
		if(Area.AREA_ALL_NAME.equals(area.getName()))
			throw new Exception("Area ALL cannot be deleted");
		super.delete();
		}

	public void add() throws Throwable
		{
		Area area = new Area();
		if (!editable)
			{
			Util.createWindow(type, new Form(type, area, this), this);
			}
		else
			{
			int index = records.size();
			if (getSelectedRows().length > 0 && getSelectedRows()[0] < records.size())
				index = getSelectedRows()[0];
			records.insertElementAt(Class.forName(Util.getProperty(type + ".object")).newInstance(), index);
			rowsInserted(0, records.size() - 1);
			}
		}

	public void cancel(Form form)
		{
		form.cancel();
		}

	public void edit() throws Throwable
		{
		Area area = (Area) (records.elementAt(table.getSelectedRows()[0]));
		if (!editable)
			{
			Util.createWindow(type, new Form(type, area, this), this);
			}
		}

	}
