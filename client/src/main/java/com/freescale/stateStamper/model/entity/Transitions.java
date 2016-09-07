package com.freescale.stateStamper.model.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class Transitions implements Serializable
	{
	
	private static final long serialVersionUID = 1L;
	public Integer primaryKey;

	public String from;
	public String to;
	public Boolean enabled;
	public Integer actionBlock;
	public Integer fromStatePK;
	public Integer toStatePk;
	public Vector conditions;
	private Vector actions; 
	private Integer equipmentTypePK;
	private String activity;
	
	public Integer getPrimaryKey()
		{
		return primaryKey;
		}

	public Integer getEquipmentTypePK()
		{
		return equipmentTypePK;
		}

	public void setEquipmentTypePK(Integer equipmentTypePK)
		{
		this.equipmentTypePK = equipmentTypePK;
		}

	public void setPrimaryKey(Integer primaryKey)
		{
		this.primaryKey = primaryKey;
		}

	public Vector getConditions()
		{
		return conditions;
		}
	
	public List getPreConditions()
		{
		List preConditions = new Vector();
		if(!hasConditions()){return preConditions;}
		for(int i=0;i<conditions.size();i++)
			{
			TransitionCondition condition = (TransitionCondition)conditions.get(i);
			if(condition.isUIPrecondition())
				{
				preConditions.add(condition);
				}
			}
		return preConditions;
		}
	
	public List getPostConditions()
		{
		List postConditions= null;
		if(!hasConditions()){return postConditions;}
		for(int i=0;i<conditions.size();i++)
			{
			TransitionCondition condition = (TransitionCondition)conditions.get(i);
			if(!condition.isUIPrecondition())
				{
				if (postConditions == null)
					{
					postConditions = new ArrayList();
					}
				postConditions.add(condition);
				}
			}
		return postConditions;
		}

	/**
	 * returns a list of conditions that match the comparison passed
	 * */
	public List getConditionsByConditional(String comparison)
		{
		List filtered= new Vector();
		if(!hasConditions()){return filtered;}
		for(int i=0;i<conditions.size();i++)
			{
			TransitionCondition condition = (TransitionCondition)conditions.get(i);
			if(condition.getCondition().equals(comparison)){filtered.add(condition);}
			}
		return filtered;
		}
	
	public List getConditionsForAttribute(String attribute)
		{
		List filtered= new Vector();
		if(!hasConditions()){return filtered;}
		for(int i=0;i<conditions.size();i++)
			{
			TransitionCondition condition = (TransitionCondition)conditions.get(i);   
			if(condition.getAttribute().equals(attribute)){filtered.add(condition);}
			}
		return filtered;
		}
	
	public void setConditions(Vector conditions)
		{
		this.conditions = conditions;
		}
	
	public Vector getActions(){return actions;}
	public void setActions(Vector actions){this.actions=actions;}

	public String toString()
		{
		StringBuffer sb = new StringBuffer();
		if(from != null)sb.append(from);
		sb.append(":");
		if(to != null)sb.append(to);
		sb.append(":" + enabled);
		return sb.toString();
		}
	
	
	public boolean hasConditions()
		{
		return conditions !=null &&  !conditions.isEmpty();
		}

	public String getActivity()
		{
		return activity;
		}

	public void setActivity(String activity)
		{
		this.activity = activity;
		}
	
	public Substate getFromState()
		{
		return new Substate(equipmentTypePK,from);
		}
	
	public Substate getToState()
		{
		return new Substate(equipmentTypePK,to);
		}

	public String getFrom()
		{
		return from;
		}

	public void setFrom(String from)
		{
		this.from = from;
		}

	public String getTo()
		{
		return to;
		}

	public void setTo(String to)
		{
		this.to = to;
		}

	public Boolean getEnabled()
		{
		return enabled;
		}

	public void setEnabled(Boolean enabled)
		{
		this.enabled = enabled;
		}

	public Integer getFromStatePK()
		{
		return fromStatePK;
		}

	public void setFromStatePK(Integer fromStatePK)
		{
		this.fromStatePK = fromStatePK;
		}

	public Integer getToStatePk()
		{
		return toStatePk;
		}

	public void setToStatePk(Integer toStatePk)
		{
		this.toStatePk = toStatePk;
		}

	public boolean conditionsTrue(IStatus status)
		{
		List postConditions = getPostConditions(); 
		if(postConditions==null|| postConditions.size()==0){return true;}
		if(status==null){return false;}
		boolean met=true;
		for (int i=0;i<postConditions.size() && met;i++)
			{
			TransitionCondition condition;
			condition = (TransitionCondition)postConditions.get(i);
			met=condition.isMet(status);
			if(!met){throw new RuntimeException("Transition condition not met: " + condition.getAttribute() + " " + condition.getCondition()  + " " + condition.getCondValue());}
			}
		return met;
		}

	}
