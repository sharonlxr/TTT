package com.freescale.stateStamper.view;

import com.freescale.stateStamper.model.entity.Substate;
import com.freescale.clientUtil.*;
import java.awt.*;
import java.util.*;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;

public class Colors extends Table
	{
	private Integer equipmenttypePK;
	private Map<String,Color> stateColors;
	
	public Colors(Integer equipmenttypePK) throws Throwable
		{
		type = "States";
		init(true);
		stateColors = new HashMap();
		table.setDefaultRenderer(Object.class,new CellRenderer());
		decimal = true;
		this.equipmenttypePK = equipmenttypePK;
		search();		
		}
	
	public void search() throws Throwable
		{
		setPreferredSize(new Dimension(Util.WIDTH/2,Util.HEIGHT));
		records = (Vector)Util.request(new Substate(this.equipmenttypePK),"search.do",false);
		loadColors();
		model.fireTableRowsInserted(0, records.size() - 1);
		}
	
	public void save() throws Throwable
		{
		Util.request(records,"store.do");
		cancel();
		Util.createWindow("Success",new Form("Success","",this),this);
		}
	
	public void save(Form form)throws Throwable{
		Substate sub =(Substate)form.getObject();
		if(sub.color ==null)sub.color="FFFFFF";
		sub.equipmenttypePK = this.equipmenttypePK;
		super.save(form);
	}
	
	
	public void changeColor() throws Throwable
		{
		if (table.getSelectedRows().length == 0) throw new Throwable("Select one or more states");
		Color color = getSubStateColorByRow(table.getSelectedRows()[0]);
		if (color == null) color = Color.WHITE;
		color = Util.getColor(this,color);
		if (color == null) return;
		for (int i=0; i<table.getSelectedRows().length; i++)
			{
			Substate state = (Substate)records.get(table.getSelectedRows()[i]);
			String rgb ="";
			String tmp = Integer.toString(color.getRed(),16).toUpperCase();
			if(tmp.length()==1){tmp = "0"+tmp;}
			rgb +=  tmp;
			tmp = Integer.toString(color.getGreen(),16).toUpperCase();
			if(tmp.length()==1){tmp = "0"+tmp;}
			rgb +=  tmp;
			tmp = Integer.toString(color.getBlue(),16).toUpperCase();
			if(tmp.length()==1){tmp = "0"+tmp;}
			rgb +=  tmp;
			state.color = rgb;
			stateColors.put(state.e10+"-"+state.substate, color);
			}
		table.repaint();
		}
	
	private Color getSubStateColorByRow(int row)
		{
		Color color = null;
		Substate sub = (Substate)records.get(row);
		color = stateColors.get(sub.e10+"-"+sub.substate);
		return color;
		}
	
	private void loadColors() throws Throwable
		{
		for (int i=0; i<records.size(); i++)
			{
			Substate state = (Substate)records.elementAt(i);
			if (state.color == null) state.color = "FFFFFF";
			stateColors.put(state.e10+"-"+state.substate, new Color(Integer.parseInt(state.color,16)));
			}
		}
	
	private class CellRenderer extends DefaultTableCellRenderer
		{
		public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean hasFocus, int row, int column)
			{
			if (column == 0)
				{
				JLabel label = new JLabel();
				label.setOpaque(true);
				label.setBackground(getSubStateColorByRow(row));
				return label;
				}
			else
				{			
				JComponent component = (JComponent) super.getTableCellRendererComponent(table, value, selected, hasFocus, row, column);
				component.setOpaque(selected);
				return component;
				}
			}
		}
	}
