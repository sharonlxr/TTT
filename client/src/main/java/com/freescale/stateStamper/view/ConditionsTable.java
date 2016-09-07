package com.freescale.stateStamper.view;

import java.awt.Color;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Vector;

import javax.swing.JComboBox;
import javax.swing.table.TableCellEditor;

import com.freescale.clientUtil.Table;

public class ConditionsTable extends Table implements ItemListener
	{
	
	public ConditionsTable(String type, Vector records, Color[] colors, Object parent, boolean editable)
		{
		super(type, records, colors, parent, editable);
		TableCellEditor editor = table.getColumnModel().getColumn(1).getCellEditor();
		JComboBox comb = (JComboBox)editor.getTableCellEditorComponent(table, null, true, 0, 1);
		comb.addItemListener(this);
		}
	
	

	public void itemStateChanged(ItemEvent e)
		{
		}

	}
