package com.freescale.stateStamper.view;

import java.util.Collections;
import java.util.HashMap;
import java.util.Properties;
import java.util.Vector;

import javax.swing.JDialog;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;

import com.freescale.clientUtil.Table;
import com.freescale.clientUtil.Util;
import com.freescale.stateStamper.model.entity.EquipmentType;
import com.freescale.stateStamper.model.entity.Substate;
import com.freescale.stateStamper.model.entity.Transitions;

public class TransitionsView extends Table
    {
	private static final long serialVersionUID = -1531431100688927894L;
	private static Util.Compare comparator = new Util.Compare();
	EquipmentType equipmentType;
	HashMap<String,Substate> states;
	
	public TransitionsView (String type,EquipmentType equipmentType, Vector records, int width, int height, Object parent) 
	    {
		super(type,records,width,height,parent);
		Collections.sort(records,comparator);
		this.equipmentType = equipmentType;
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		Substate sub = new Substate();
		sub.equipmenttypePK = equipmentType.primaryKey;
		Vector substates=null;
		try {substates= (Vector)Util.request(sub,"search.do",false);} 
		catch (Throwable e) {e.printStackTrace();}
		states = new HashMap<String,Substate>();
		
		StringBuffer statesStr = new StringBuffer();
		for (Object obj : substates) 
		    {
			sub=(Substate)obj;
			String key=sub.e10+"-"+sub.substate;
			statesStr.append(key+",");
			states.put(key,sub);
		    }
		if(statesStr.toString().endsWith(",") )statesStr.deleteCharAt(statesStr.length()-1);
		Properties properties = (Properties)UIManager.get("gui.properties");
		properties.setProperty("TransitionSetup.from.values",statesStr.toString());
		properties.setProperty("TransitionSetup.to.values",statesStr.toString());
	    }
	

	public void add() throws Throwable 
	    {
		TransitionSetup transitionSetup = new TransitionSetup(this,states,new Transitions());
		try{
			JDialog dialog = Util.createWindow("New Transition",transitionSetup,this);
		}
		catch(Throwable t){t.printStackTrace();}
	    }
	
	/**
	 * Overrides edit method
	 */
	public void edit() throws Throwable
	    {
		Transitions tran = (Transitions) getSelected().get(0);
		String title = tran.from + " to " + tran.to;
		try{Util.createWindow(title,new TransitionSetup(this,states,tran),this);}
		catch(Throwable t){t.printStackTrace();}
	    }

	/**
	 * overrides search to search only for the transitions for the EquipmentType 
	 * */
	public void search() throws Throwable
		{
		Transitions transition = new Transitions();
		transition.setEquipmentTypePK(equipmentType.primaryKey);
		records = (Vector) Util.request(transition, "search.do");
		if (sort && records != null){ Collections.sort(records,comparator); }
		model.fireTableRowsInserted(0, records.size() - 1);
		resize();
		}
}
