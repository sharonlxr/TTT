package com.freescale.stateStamper.view;

import java.awt.Color;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Properties;
import java.util.Vector;

import javax.swing.JComboBox;
import javax.swing.UIManager;
import javax.swing.table.TableCellEditor;

import com.freescale.clientUtil.Table;
import com.freescale.clientUtil.Util;
import com.freescale.stateStamper.model.entity.BusinessRuleAction;
import com.freescale.stateStamper.model.entity.EquipmentAttribute;
import com.freescale.stateStamper.model.entity.EquipmentType;

public class ActionsTable extends Table implements ItemListener
	{
	private EquipmentType eqType;
	private Properties properties;
	
	public ActionsTable(String type, Vector records, Color[] colors, Object parent, boolean editable)
		{
		super(type, records, colors, parent, editable);
		TableCellEditor editor = table.getColumnModel().getColumn(1).getCellEditor();
		JComboBox comb = (JComboBox)editor.getTableCellEditorComponent(table, null, true, 0, 1);
		comb.addItemListener(this);
		properties = (Properties)UIManager.get("gui.properties");
		try{eqType=(EquipmentType)Util.request(null,"getCurrentEquipmentType.do");}
		catch(Throwable t){t.printStackTrace();}
		}
	
	public void itemStateChanged(ItemEvent e)
		{
		Object item =e.getItem();
		StringBuffer counters= new StringBuffer();
		if(BusinessRuleAction.INCREASE.equals(item))
			{
			Vector vCounter=eqType.getAttributes();
			EquipmentAttribute attribute;
			for(int i=0;vCounter!=null&&i < vCounter.size();i++)
				{
				attribute = (EquipmentAttribute)vCounter.get(i);
				if(EquipmentAttribute.COUNTER.equals(attribute.type) )
					{
					counters.append(attribute.name+",");
					}
				}
			if(counters.length()>0)counters.deleteCharAt(counters.length()-1);
			properties.setProperty("TransitionActions.action.values",counters.toString());
			}
		}

	}
