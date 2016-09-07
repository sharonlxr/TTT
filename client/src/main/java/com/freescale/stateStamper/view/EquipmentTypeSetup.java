package com.freescale.stateStamper.view;

import java.awt.Window;
import java.awt.event.ActionListener;

import javax.swing.*;
import javax.swing.tree.*;

import org.apache.commons.lang.StringUtils;

import java.util.*;

import com.freescale.clientUtil.*;
import com.freescale.stateStamper.model.entity.*;


public class EquipmentTypeSetup extends Tree implements ActionListener{

	private static final long serialVersionUID = 1L;
	private static final String BIZ_RULE= "BusinessRules";
	private static final String ATTRIBUTES="Attributes";
	private static final String CONDITIONS="Conditions";
	private static final String ACTIONS="Actions";
	private static final Object[] ITEMS = new Object[]{ATTRIBUTES,BIZ_RULE};
	private static final String[] GrandChildrenITEMS = new String[]{CONDITIONS,ACTIONS};
	private EquipmentTypes types;
	private EquipmentType type;
	private Table table;
	private Table bizrules;
	private Form form;
	private Map<String,Event> events;
	private BusinessRule ruleSelected;
	private Object parent;
	private Properties properties;
	private List<Area> areas;
	private StateStamperFrame stateStamperFrame;
	
	public EquipmentTypeSetup(EquipmentTypes eqTypes, EquipmentType type, Object parent,StateStamperFrame stateStamperFrame ) throws Throwable{
		super("EquipmentTypes",ITEMS,GrandChildrenITEMS,new int[]{190,520,478});
		this.types = eqTypes;
		this.type=type;
		this.parent = parent;
		this.stateStamperFrame = stateStamperFrame;
		selected = getRoot();
		properties = (Properties)UIManager.get("gui.properties");
		Util.setProperty("EquipmentTypeSetup.areaName.values", StringUtils.join(stateStamperFrame.getAreaNames(),","));	
		expand();
	}
	
	public EquipmentType getEquipmentType()
		{
		return type;
		}
	
	public void setEquipmentType(EquipmentType eqType)
		{
		type = eqType;
		}
	
	
	private Map getEvents()throws Throwable
		{
		if(events ==null)
			{
			Event e = new Event();
			Vector v = (Vector)Util.request(e, "search.do");
			events = new TreeMap<String,Event>();
			for (Object object : v)
				{
				e = (Event)object;
				events.put(e.getName(),e);
				}
			}
		return events;
		}
	
	private String getEvents2Str(Map eves)
		{
		StringBuffer eventStr=new StringBuffer();
		eventStr.append(eves.keySet().toString());
		if(eventStr.toString().startsWith("["))eventStr.deleteCharAt(0);
		if(eventStr.toString().endsWith("]"))eventStr.deleteCharAt(eventStr.length()-1);
		return eventStr.toString().replace(", ", ",");
		}
	
	public void setChildren(DefaultMutableTreeNode parent, Object[] children, String[] grandChildren)
	{
		deleteChildren(parent);
		for (int i=0; i<children.length; i++)
		{
			DefaultMutableTreeNode child = new DefaultMutableTreeNode(children[i]);
			treeModel.insertNodeInto(child,parent,i);
			if (grandChildren != null&&children[i].equals(BIZ_RULE))
			{
			for (int j=0; j<grandChildren.length; j++) treeModel.insertNodeInto(new DefaultMutableTreeNode(grandChildren[j]),child,j);
			}
		}
	tree.expandPath(new TreePath(parent.getPath()));
	}
	
	public void expand()throws Throwable{
		JComponent component = null;
		if (form != null) form.getObject();
		if (table != null) table.finishEdit();
		if (selected.getParent() == null)
			{
			Form form = new Form("EquipmentTypeSetup",type,this,0);
			component = form;
			}
		else
			{			
			selected.getUserObject();
			String value = selected.getUserObject().toString();
			if (value.equals(BIZ_RULE)) 
			{//business rules
				setWindowTitle(BIZ_RULE);
				properties.setProperty(BIZ_RULE+".eventName.values",getEvents2Str(getEvents()));
				
				Vector rules =  type.getBusinessRules();
				//
				for (int i = 0 ; i < rules.size(); i++){
					BusinessRule rule = (BusinessRule)rules.get(i);				
					rule.setEvent(rule.getEvent().clone());
				}
				component = new Table(BIZ_RULE,rules,null,this,true);
				((Table)component).setName(BIZ_RULE);
				bizrules = (Table)component;
				
			}
			else if(value.equals(CONDITIONS))
			{//Conditions
				ruleSelected = new BusinessRule();
				if (bizrules != null && !(bizrules.getSelected().isEmpty()))
					{
					ruleSelected = (BusinessRule) bizrules.getSelected().get(0);
					}
				else
					{
					ruleSelected = (BusinessRule) bizrules.getRecords().get(0);
					}
				String rulename = ruleSelected.getName();
				if (rulename != null && (!rulename.equalsIgnoreCase("")))
					{
					setWindowTitle("Selected Business Rule" + " " + rulename);
					}
				else
					{
					setWindowTitle("No Business Rule selected");
					}
				Vector conditions= ruleSelected.getConditions();
			
			
				EquipmentAttribute attribute;
				Vector attrs= type.getAttributes();
				StringBuffer sb= new StringBuffer();
				for (Object object : attrs)
					{
					attribute = (EquipmentAttribute) object;
					sb.append(attribute.name + ",");
					}
				for (int i = 0; i <Machine.commonAttributes.length; i++)
					{
					sb.append(Machine.commonAttributes[i] + ",");
					}
				sb.append(DurableStatus.CURRENT_STATE_DURATION + ",");
				if(sb.toString().endsWith(","))sb.deleteCharAt(sb.length()-1);
				properties.setProperty("BusinessRuleCondition.attribute.values",sb.toString());
				component = new ConditionsTable("BusinessRuleCondition",conditions,null,this,true);
				((Table)component).setName(CONDITIONS);
			
			}else if(value.equals(ACTIONS))
			{//Actions
			
				BusinessRuleAction action = new BusinessRuleAction();
				ruleSelected = new BusinessRule();
				if(bizrules !=null&&!(bizrules.getSelected().isEmpty()) ){ruleSelected=(BusinessRule)bizrules.getSelected().get(0);}
				else {ruleSelected=(BusinessRule)bizrules.getRecords().get(0); }
				action.setBusinessRulePk(ruleSelected.getPrimaryKey());
				String rulename = ruleSelected.getName();
				if (rulename != null && (!rulename.equalsIgnoreCase("")))
					{
					setWindowTitle("Selected Business Rule" + " " + rulename);
					}
				else
					{
					setWindowTitle("No Business Rule selected");
					}
				Vector actions = ruleSelected.getActions();
				String actionsStr = BusinessRuleAction.getActionsAsString();
				properties.setProperty("BusinessRuleActions.action.values",actionsStr);
				if (actions==null) actions = new Vector();
				component = new BusinessRuleActionTable(ruleSelected,actions,this);			
				((Table)component).setName(ACTIONS);
			}else if (value.equals(ATTRIBUTES))
			{// Attributes
				setWindowTitle(ATTRIBUTES);
				Vector attributes = type.getFilteredAttributes();
				component = new EquipmentAttributes("EquipmentAttribute",attributes,null,this);
				((Table)component).setName(ATTRIBUTES);
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
	}
	

	/* EquipmentAttributes methods*/
	public void add(EquipmentAttributes eaTable)
		{
		try{eaTable.add();}
		catch(Throwable t){}
		}

	public void edit(EquipmentAttributes eaTable)
		{
		eaTable.edit();
		}

	public void delete(EquipmentAttributes eaTable)
		{
		try	{eaTable.delete();} 
		catch (Throwable e){e.printStackTrace();}
		}
	
	public void save(EquipmentAttributes eaTable) throws Throwable 
		{
		eaTable.save();
		}
	
	public void cancel(EquipmentAttributes eaTable)
		{
		eaTable.cancel();
		}

	/* Conditions table methods*/
	public void add(ConditionsTable table)
		{
		try	{table.add();}
		catch (Throwable e){e.printStackTrace();}
		}
	
	public void edit(ConditionsTable table)
		{
		try	{table.edit();}
		catch (Throwable e){e.printStackTrace();}
		}
	
	public void cancel(ConditionsTable table)
		{
		table.cancel();
		}

	public void delete(ConditionsTable table)
		{
		try {delete((Table)table);} 
		catch (Throwable e) {e.printStackTrace();}
		}
	
	public void save(ConditionsTable table)
		{
		try {save((Table) table);}
		catch (Throwable e)	{e.printStackTrace();}
		}
	
	/* BusinessRuleActionTable */
	public void add(BusinessRuleActionTable table)
		{
		try	{table.add();}
		catch (Throwable e){e.printStackTrace();}
		}
	
	public void edit(BusinessRuleActionTable table)
		{
		try	{table.edit();}
		catch (Throwable e){e.printStackTrace();}
		}
	
	public void cancel(BusinessRuleActionTable table)
		{
		table.cancel();
		}

	public void delete(BusinessRuleActionTable table)
		{
		try {delete((Table)table);} 
		catch (Throwable e) {e.printStackTrace();}
		}
	
	public void save(BusinessRuleActionTable table)
		{
		try {save((Table) table);}
		catch (Throwable e)	{e.printStackTrace();}
		}
	
	/**
	 * Saves a form
	 * @param form The form to save
	 * @throws Throwable
	 */
	public void save(Form form) throws Throwable {save();}
	
	public void reloadEquipments(Form form) throws Throwable 
		{
		if (form != null)form.getObject();
		Util.request(type,"reloadEquipments.do");
		((Window)getTopLevelAncestor()).dispose();
		Util.createWindow("Success",new Form("Success","",this,0),this);
		}

	public void save(Table table) throws Throwable {
		table.finishEdit();
		Vector tableRecords = table.getRecords();
		Iterator it = tableRecords.iterator();
		Object e;
		String name=table.getName();
		while(it.hasNext())
			{
		    e= it.next();
		    
		    if(name.equalsIgnoreCase(BIZ_RULE)){
		    BusinessRule rule = (BusinessRule)e;
		    rule.getPrimaryKey();
		    rule.setEquipmentTypePk(type.primaryKey);
		    rule.setEvent(events.get( rule.getEventName()).clone());
		    rule.setEventPk(rule.getEvent().getPrimaryKey());
		    
		    }else if (name.equalsIgnoreCase(CONDITIONS)){
		    BusinessRuleCondition cond = (BusinessRuleCondition)e;
		    cond.setBusinessRulePk(ruleSelected.getPrimaryKey() );
		    
		    }else if(name.equalsIgnoreCase(ACTIONS)){
		    BusinessRuleAction action = (BusinessRuleAction)e;
		    action.setBusinessRulePk(ruleSelected.getPrimaryKey());
		    }
		    }
		table.save();
		save();
	}
	
	
	
	public void save() throws Throwable{
		if (form != null)form.getObject();
		Area area = stateStamperFrame.getAreaByName(type.getAreaName());
		type.setArea(area);
		type.setAreaPK(area.getPrimaryKey());
		Util.request(type,"store.do");
		types.search();
		Vector v=(Vector)Util.request(type, "search.do");
		if(v!=null && !v.isEmpty())
			{
			Substate substate= new Substate();
			substate.e10 = "Unknown";
			substate.substate = "Unknown";
			substate.equipmenttypePK= ((EquipmentType)v.get(0)).primaryKey;
			v=(Vector)Util.request(substate,"search.do");
			if(v==null || v.size()==0)
				{
				substate.color = "999999";
				Util.request(substate, "store.do");
				}
			}
		
		((Window)getTopLevelAncestor()).dispose();
		Util.createWindow("Success",new Form("Success","",this,0),this);
	}
	public void delete(Table table) throws Throwable {
		if (table.getSelectedRows().length == 0 || table.getSelectedRows()[0] >= table.getRecords().size()) return;
		String name = table.getName();
		Object rec2Delete=table.getRecords().elementAt(table.getSelectedRows()[0]); 
		Util.request(rec2Delete, "delete.do");
		type=reloadEquipmentType(); 
		if (BIZ_RULE.equals(name)){
			BusinessRule rule = new BusinessRule();
			rule.setEquipmentTypePk(type.primaryKey);
			Vector rulez=(Vector)Util.request(rule, "search.do"); 
			table.setRecords(rulez);
			type.setBusinessRules(rulez);
		}
		else if (ACTIONS.equals(name) ){
			BusinessRule rule = (BusinessRule)bizrules.getSelected().get(0);
			BusinessRuleAction action = new BusinessRuleAction();
			
			action.setBusinessRulePk(rule.getPrimaryKey());
			Vector actions = (Vector)Util.request(action, "search.do");
			table.setRecords(actions);
			rule.setActions(actions);
		}
		else if (CONDITIONS.equals(name)  ){
			BusinessRuleCondition condition = new BusinessRuleCondition();
			BusinessRule rule = (BusinessRule)bizrules.getSelected().get(0);
			condition.setBusinessRulePk(rule.getPrimaryKey());
			Vector conditions = (Vector)Util.request(condition, "search.do"); 
			table.setRecords(conditions);
			rule.setConditions(conditions);
		}
		else if (ATTRIBUTES.equals(name)){
		    EquipmentAttribute attributeDeleted = (EquipmentAttribute)rec2Delete;
		    //for ThresholdCounters delete all the attributes that belong to the counter 
		    if(EquipmentAttribute.THRESHOLDCOUNTER.equals( attributeDeleted.type ))
		    	{
		    	String tcName = attributeDeleted.name + "$";
		    	Vector toDelete = type.getAttributes();
		    	EquipmentAttribute childAttribute;
		    	for(int i=0;toDelete!=null&&i<toDelete.size();i++)
		    		{
		    		childAttribute= (EquipmentAttribute)toDelete.get(i);
		    		if(childAttribute.name.startsWith(tcName) )
		    			{
		    			Util.request(childAttribute, "delete.do");
		    			}
		    		}
		    	}
		    type= reloadEquipmentType();
			table.setRecords(type.getFilteredAttributes());
		}
		types.search();
		Util.createWindow("Success", new Form("Success", "", this, 0), this);		
	   }

	public EquipmentType reloadEquipmentType()
		{
		try{
		if( type==null || type.toString().equals("null:null:null:null"))return type;
		  Vector v = (Vector)Util.request(type, "search.do");
		  if(v !=null && v.size() >0){type= (EquipmentType)v.get(0);}
		  types.search();
		}catch(Throwable t){}
		return type;
		}
	/**
	 * Dismiss a form
	 * @param form The form to close
	 * @throws Throwable
	 */
	public void dismiss(Form form) throws Throwable {form.cancel();}
	
	private void setWindowTitle (String title){
		JDialog transitionView = (JDialog)(((JComponent) parent).getTopLevelAncestor());
		Window[] windows = transitionView.getOwnedWindows();		
		JDialog transitionSetupWindow =(JDialog)(windows[windows.length-1]);
		transitionSetupWindow.setTitle(title);
		transitionSetupWindow.repaint();
	}
	
	public void sortAscending(EquipmentAttributes attributes ) throws Throwable {table.sortAscending();}
	public void sortDescending(EquipmentAttributes attributes ) throws Throwable {table.sortDescending();}
	

}
