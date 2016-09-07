package com.freescale.stateStamper.view;

import java.awt.Window;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.UIManager;

import com.freescale.clientUtil.Form;
import com.freescale.clientUtil.Table;
import com.freescale.clientUtil.Tree;
import com.freescale.clientUtil.Util;
import com.freescale.stateStamper.model.entity.DurableStatus;
import com.freescale.stateStamper.model.entity.EquipmentAttribute;
import com.freescale.stateStamper.model.entity.EquipmentType;
import com.freescale.stateStamper.model.entity.Substate;
import com.freescale.stateStamper.model.entity.TransitionAction;
import com.freescale.stateStamper.model.entity.TransitionCondition;
import com.freescale.stateStamper.model.entity.Transitions;

public class TransitionSetup extends Tree implements ActionListener 
{
	private static final long serialVersionUID = 1L;
	private Transitions transition;
	private Table table;
	private Form form;
	private HashMap<String,Substate> states;
    private Table parent; 
    private Properties properties;
    private EquipmentType eqType;
    private static final Object[] ITEMS = new Object[]{"Conditions","TransitionActions"};
    
	public TransitionSetup(Table parentTable, HashMap<String,Substate> states,Transitions trans)throws Throwable
	    {
		super("Transition Setup",ITEMS,null,new int[]{190,520,478});
		this.parent = parentTable;
		this.states=states;
		transition = trans;
		selected=getRoot();
		properties = (Properties)UIManager.get("gui.properties");
		try{eqType=(EquipmentType)Util.request(null,"getCurrentEquipmentType.do");}
		catch(Throwable t){t.printStackTrace();}
		expand();
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
			Form form = new Form("Transition Setup",transition,this,0);
			component = form;
			}
		else
			{
			String value = selected.getUserObject().toString();
			if (value.equals("Conditions"))
			    {			
				Vector v;
				if (transition.primaryKey==null){v = new Vector();}
				else{v = transition.getConditions();}					
				properties.setProperty("Conditions.attribute.values",getConditionsAttributes());
				component = new Table("Conditions",v,null,this,true);
				((Table)component).setName("Conditions");
			    }
			else if(value.equals("TransitionActions"))
				{
				Vector actions = transition.getActions();
				String actionsStr= TransitionAction.INCREASE    + "," + TransitionAction.DECREASE + "," +
						           TransitionAction.INCREASE_BY + "," + TransitionAction.DECREASE_BY + "," +
				                   TransitionAction.RESET+ "," + TransitionAction.CLEAR +"," + TransitionAction.SET +","+TransitionAction.SEND_EPR; 
	            properties.setProperty("TransitionActions.action.values",actionsStr);
	            StringBuffer actionsParamStr= new StringBuffer();
	            Vector attr=  eqType.getAttributes();
	            for(int i=0;attr!=null&&i<attr.size();i++)
	            	{
	            	EquipmentAttribute atr = (EquipmentAttribute)attr.get(i); 
	            	actionsParamStr.append(atr.name+",");
	            	}
	            if(actionsParamStr.length()>0){actionsParamStr.deleteCharAt(actionsParamStr.length()-1);}
//	            properties.setProperty("TransitionActions.actionParameters.values",actionsParamStr.toString());
	            ActionsTable actionTable = new ActionsTable("TransitionActions",actions,null,this,true);
	            actionTable.setName("TransitionActions");
	            component = actionTable;
				}
			}					
		if (component != null)
			{
			if (component instanceof Form) form = (Form)component;
			else form = null;
			if (component instanceof Table) table = (Table)component;
			else table = null;
			add(component,false,true);
			}
		
		try{
			String title = transition.from + " to " +  transition.to;
			setWindowTitle(title);
		}catch(Throwable throwable){}			
		}
	
	private String getConditionsAttributes()
		{
		List attributes = eqType.getAttributes();
		List condAttributes = new Vector();
		for(int i = 0; i < attributes.size();i++)
			{
			condAttributes.add(((EquipmentAttribute)attributes.get(i)).getName());
			}
		EquipmentAttribute attribute;
		attributes = eqType.getAttributesByType(EquipmentAttribute.THRESHOLDCOUNTER);
		for(int i = 0; i < attributes.size();i++)
			{
			attribute = (EquipmentAttribute)attributes.get(i);
			condAttributes.add(attribute.getName()+":"+TransitionCondition.SERVICE_BAR);
			condAttributes.add(attribute.getName()+":"+TransitionCondition.REBUILD_BAR);
			condAttributes.add(attribute.getName()+":"+TransitionCondition.LIFE_BAR);
			}
		condAttributes.add("ROLE");
		condAttributes.add("COMMENTS");
		condAttributes.add("workstation");
		condAttributes.add("location");
		condAttributes.add("type");
		condAttributes.add(DurableStatus.CURRENT_STATE_DURATION);
		if(eqType.propagateStatus()){condAttributes.add(TransitionCondition.PROPAGATE);}
		Collections.sort(condAttributes);
		StringBuffer attrStr= new StringBuffer( condAttributes.toString().replace(", ",","));
		attrStr.deleteCharAt(0).deleteCharAt(attrStr.length()-1);
		return  attrStr.toString();
		}

	private void setWindowTitle (String title){
		JDialog transitionView = (JDialog)(((JComponent) parent).getTopLevelAncestor());
		Window[] windows = transitionView.getOwnedWindows();		
		JDialog transitionSetupWindow =(JDialog)(windows[windows.length-1]);
		transitionSetupWindow.setTitle(title);
		transitionSetupWindow.repaint();
	}
	
	public void add(ActionsTable table)
		{
		try{table.add();}
		catch (Throwable e){e.printStackTrace();}
		}
	
	public void edit(ActionsTable table)
		{
		try{table.edit();}
		catch (Throwable e){e.printStackTrace();}
		}
	
	public void cancel(ActionsTable table)
		{
		table.cancel();
		}

	
	
	/**
	 * Saves a form
	 * @param form The form to save
	 * @throws Throwable
	 */
	public void save(Form form) throws Throwable {save();}

	public void save(Table table) throws Throwable 
	    {
		table.finishEdit();
		String name = table.getName();
		if("Conditions".equals(name))
			{
			Vector conds = table.getRecords();
			TransitionCondition cond;
			for (Object obj : conds) 
				{
				cond = (TransitionCondition)obj;
				cond.setStateTransition_pk(transition.primaryKey);
				if(TransitionCondition.CHECK_CORE_ID.endsWith(cond.getCondition()))
					{
					EquipmentAttribute attribute = eqType.getAttribute(cond.getAttribute());
					String subType = attribute.getConfigOptions().get(TransitionCondition.SUB_TYPE);
					if(!TransitionCondition.SUB_TYPE_CORE_ID.equals(subType))
						{
						throw new Exception("For checkCoreId condition, attribute "+cond.getAttribute()+" should be an attribute of COREID, please change its configOptions to contains 'subType=COREID'");
						}
					}
				}
			}
		else if("TransitionActions".equals(name))
			{
			Vector actions = table.getRecords();
			TransitionAction action;
			for(int i=0;i<actions.size();i++)
				{
				action = (TransitionAction)actions.get(i);
				action.setTransitionPk(transition.primaryKey);
				}
			}
		save();
	    }
	
	public void delete(Table table) throws Throwable {
		if (table.getSelectedRows().length == 0 || table.getSelectedRows()[0] >= table.getRecords().size()) return;
		Util.request(table.getRecords().get(table.getSelectedRows()[0]), "delete.do");				
		TransitionCondition tranCond = new TransitionCondition();
		tranCond.setStateTransition_pk(transition.primaryKey);
		Vector vAttributes =(Vector)Util.request(tranCond, "search.do"); 
		table.setRecords(vAttributes);
		Util.createWindow("Success", new Form("Success", "", this, 0), this);		
		}

	public void delete(ActionsTable table) throws Throwable {
		if (table.getSelectedRows().length == 0 || table.getSelectedRows()[0] >= table.getRecords().size()) return;
		Util.request(table.getRecords().get(table.getSelectedRows()[0]), "delete.do");				
		TransitionAction tranAction = new TransitionAction();
		tranAction.setTransitionPk(transition.primaryKey);
		Vector vAttributes =(Vector)Util.request(tranAction, "search.do"); 
		table.setRecords(vAttributes);
		Util.createWindow("Success", new Form("Success", "", this, 0), this);
		parent.search();
		}	
	
	public void save(ActionsTable actionTable)throws Throwable
		{
		save((Table)actionTable);
		}
	
	private void save() throws Throwable
	    {
		Object o=null;
		if (form != null)o= form.getObject();
		transition.actionBlock=0;
		transition.fromStatePK=states.get(transition.from).primaryKey;
		transition.toStatePk=states.get(transition.to).primaryKey;
		transition.setEquipmentTypePK(states.get(transition.from).equipmenttypePK);
		Util.request(transition,"store.do");
		if(table!=null)
		    {
			Vector conds = table.getRecords();
			if(conds!=null && conds.size() >0)
				Util.request(table.getRecords(),"store.do");
		    }
		((Window)getTopLevelAncestor()).dispose();
		parent.search();
		Util.createWindow("Success",new Form("Success","",this,0),this);
	    }

	public void fieldChanged(Form form, String field, Object value) throws Throwable 
	    {
		if(value==null)return;
	    }

	public void cancel(Form form){form.cancel();}
	
	/**
	 * Dismiss a form
	 * @param form The form to close
	 * @throws Throwable
	 */
	public void dismiss(Form form) throws Throwable {form.cancel();}

}
