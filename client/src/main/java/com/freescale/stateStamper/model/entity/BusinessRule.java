package com.freescale.stateStamper.model.entity;

import java.io.Serializable;
import java.util.List;
import java.util.Vector;

public class BusinessRule implements Serializable
	{
	private static final long serialVersionUID = 1L;
	private Integer primaryKey;
	private Integer eventPk;
	private Integer equipmentTypePk;
	private String  name;
	private Vector conditions; 
	private Vector actions;
	private Event event;
	public static final String EPR_EVENT = "EPR Received";
	public static final String MONITOR = "Monitor";
    public static final String NEW_STATUS = "New Status";
    public static final String MONITOR_PAGE = "Monitor Page";
    public static final String OPEN_REPORT = "Open Report";
    public static final String NEW_EQUIPMENT = "Add Equipment";
    public static final String DM_MONITOR = "Durable Monitor";
    public static final String ASSOCIATE = "Association";
    public static final String ATTACH_CHILD = "Attach Child Equipment";
    public static final String MODEL_CONFIGURATION = "Model Configuration";
    
    
    
	public BusinessRule()
		{
		event = new Event();
		conditions = new Vector();
		actions = new Vector();
		}
	
	public Integer getEquipmentTypePk()
		{
		return equipmentTypePk;
		}
	
	public void setEquipmentTypePk(Integer equipmentTypePk)
		{
		this.equipmentTypePk = equipmentTypePk;
		}
	
	public String getName()
		{
		return name;
		}

	public void setName(String name)
		{
		this.name = name;
		}

	public Integer getPrimaryKey()
		{
		return primaryKey;
		}
	
	public void setPrimaryKey(Integer primaryKey)
		{
		this.primaryKey = primaryKey;
		}

	public Integer getEventPk()
		{
		return eventPk;
		}

	public void setEventPk(Integer eventpK)
		{
		this.eventPk = eventpK;
		}


	public Event getEvent()
		{
		return event;
		}

	public void setEvent(Event event)
		{
		this.event = event;
		}

	public Vector getConditions()
		{
		return conditions;
		}

	public void setConditions(Vector conditions)
		{
		this.conditions = conditions;
		}
	
	
	public Vector getActions()
		{
		return actions;
		}

	public void setActions(Vector actions)
		{
		this.actions = actions;
		}

	public String getEventName()
		{
		return event.getName();
		}

	public void setEventName(String eventName)
		{
		event.setName( eventName );
		}

	public boolean conditionsTrue(IStatus status)
		{
		if(conditions==null|| conditions.size()==0)return true;
		if(status==null)return false;
		boolean met=false;
		for (int i=0;i<conditions.size();i++)
			{
			BusinessRuleCondition condition;
			condition = (BusinessRuleCondition)conditions.get(i);
			met=condition.isMet(status);
			if(!met)break;
			}
		return met;
		}
	
	public List getActionsBy(String action)
		{
		List result = new Vector();
		for(int i=0;actions!=null && i< actions.size();i++)
			{
			BusinessRuleAction brAction = (BusinessRuleAction)actions.get(i);  
			if(brAction.getAction().equals(action) ){result.add(brAction);}
			}
		return result;
		}
	
	public BusinessRuleAction getBusinessRuleActionsByAction(String action)
		{
		BusinessRuleAction bra = null;
		for(Object obj:actions)
			{
			BusinessRuleAction brAction = (BusinessRuleAction)obj;  
			if(brAction.getAction().equals(action))
				{
				bra = brAction;
				break;
				}
			}
		return bra;
		}
	
	public String getConditionsAsString()
		{
		StringBuffer conds = new StringBuffer();
		for(int i = 0;conditions != null && i< conditions.size();i++){if(i>0){conds.append(" and ");}conds.append(((BusinessRuleCondition)conditions.get(i)).toExtString()+"\t");}
		return conds.toString();
		}
	
	public String getConditionsAsStringWithAlias(EquipmentType eqType)
		{
		StringBuffer conds = new StringBuffer();
		for(int i = 0;conditions != null && i< conditions.size();i++){if(i>0){conds.append(" and ");}conds.append(((BusinessRuleCondition)conditions.get(i)).toExtStringWithAlias(eqType)+"\t");}
		return conds.toString();
		}
	
	public boolean hasConditions()
		{
		return conditions != null && conditions.size() > 0;
		}

	public boolean hasActions()
		{
		return actions != null && actions.size() > 0;
		}	
	
	}
