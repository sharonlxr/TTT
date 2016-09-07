package com.freescale.stateStamper.model.entity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.freescale.stateStamper.util.TTTUtil;


public class TransitionCondition implements Serializable
	{
	
	private static final long serialVersionUID = 5660212323080532366L;
	private Integer primaryKey;
	private Integer stateTransition_pk;
	private String order, attribute, condition, condValue;
	public static final int UI_PRE_ORDER = -2;
	public static final String DISPLAY ="Display";
	public static final String REQUEST ="Request";
	public static final String REQUIRE ="Require";
	public static final String MULTIPLE_IN = "Multiple-in";
	public static final String SINGLE_IN = "Single-in";
	public static final String SERVICE_BAR = "serviceUsage/serviceUsageLimit";
	public static final String REBUILD_BAR = "rebuildUsage/rebuildUsageLimit";
	public static final String LIFE_BAR = "lifeUsage/lifeLimit";
	public static final String CLEAR = "CLEAR";
	public static final String PROPAGATE = "Propagate Status";
	public static final String CHECK_CORE_ID = "checkCoreId";
	public static final String SUB_TYPE = "subType";
	public static final String CHECK_NAME_DISPLAY = "displayFullName";
	public static final String CHECK_NAME_POPULATE = "populateFullName";
	public static final String SUB_TYPE_CORE_ID = "COREID";
	
	public Integer getPrimaryKey()
		{
		return primaryKey;
		}
	
	public void setPrimaryKey(Integer primaryKey)
		{
		this.primaryKey = primaryKey;
		}
	
	public Integer getStateTransition_pk()
		{
		return stateTransition_pk;
		}
	
	public void setStateTransition_pk(Integer stateTransitionPk)
		{
		stateTransition_pk = stateTransitionPk;
		}
	
	public String getOrder()
		{
		return order;
		}
	
	public void setOrder(String order)
		{
		this.order = order;
		}
	
	public String getAttribute()
		{
		return attribute;
		}
	
	public void setAttribute(String attribute)
		{
		this.attribute = attribute;
		}
	
	public String getCondition()
		{
		return condition;
		}
	
	public void setCondition(String condition)
		{
		this.condition = condition;
		if(isUIPrecondition()){setOrder(String.valueOf(UI_PRE_ORDER));}
		}
	
	public String getCondValue()
		{
		if(condValue!=null)
			{
			return condValue;
			}
		else 
			{
			return "";
			}
		}
	
	public void setCondValue(String condVal)
		{
		condValue = condVal;
		}
	
	public boolean isUIPrecondition()
		{
		return condition !=null && 
		       (condition.equals(DISPLAY) || condition.equals(REQUEST) || condition.equals(REQUIRE)|| String.valueOf(UI_PRE_ORDER).equals(getOrder()));  
		}
	
	public String toString()
		{
		return getAttribute() + ":" + getCondition() + ":" + getCondValue()
				+ ":" + getStateTransition_pk();
		}
	
	private boolean isNumericComparison()
		{
		condition = condition.trim();
		return condition.equals(">")||
		       condition.equals("<")||
		       condition.equals(">=")||
		       condition.equals("<=")||
		       condition.equals("<>");
		}
	/**
	 * if condition is checkCodeId, the condValue can be emtpy, otherwise attribute and condvalue both cannot be empty
	 * @throws Throwable
	 */
	private void preValidate()throws Throwable
		{
		boolean invalid=false;
		if (attribute == null || attribute.equals(""))
			{
			invalid = true;
			}
		else if (!condition.equals(CHECK_CORE_ID) && (condValue == null || condValue.equals("")))
			{
			invalid = true;
			}
		if(invalid)throw new Throwable("Condition is not defined properly");
		}
	
	private boolean inList(String value, String value2){return inList(value,value2,false);}
	
	private boolean inList(String value,String value2, boolean isList)
		{
		String list = value2;
		if(list.startsWith("[")){list=list.substring(1);}
		if(list.endsWith("]")){list = list.substring(0,list.length()-1); }
        List items = Arrays.asList(list.split(","));
        boolean result = false;
        if(!isList){result = items.contains(value);}
        else{
        	List inner = Arrays.asList(value.split(", "));
            result = items.containsAll(inner); 
        	}
		return result;
		}
	
	/**
	 * @param list a string representing with a comma separated list of items e.g. A,B,C
	 * @param search 
	 * @return true if the second parameter is in the first parameter   
	 * */
	private boolean contains(String list, String search)
		{
		boolean result = false;
		if (list != null )
			{
			String trimmed = list.trim();
			if (trimmed.startsWith("[") && trimmed.endsWith("]"))
				{
				trimmed = trimmed.substring(1, trimmed.length() - 1);
				}
			String[] array = trimmed.split(",");
			for (String temp : array)
				{
				if (temp != null && temp.trim().equals(search))
					{
					result = true;
					break;
					}
				}
			}
		return result;
		}
	
	public String getCondValuewithoutPercentange()
	    {
		if (condValue!=null&&condValue.indexOf("%")!=-1){
			String[] tmp = condValue.split("%");
			return  tmp[1];		
		    }
		else 
		    {
			return condValue;
		    }
		}
	
	public Double getPercentage()
	    {
		if (condValue!=null&&condValue.indexOf("%")!=-1){
			String[] tmp = condValue.split("%");
			return Double.parseDouble(tmp[0])/100.0;
		}else
		    {
			return null;
		    }
		}


	public boolean isMet(IStatus status)
		{
		String value = null;
		String value2=null;
		Object tmp=null;
		if(status==null || !status.has(attribute) )return false;
		tmp = status.get(attribute);		
		if(tmp==null){return false;}
		if(tmp instanceof Map && attribute.equals("COMMENTS")){tmp= getCommentValue(status);}
		value = tmp.toString();
		String condValue = getCondValuewithoutPercentange();
		if(condValue!=null&&status.has(condValue)){tmp = status.get(condValue);}
		else{tmp = condValue;}
		value2 = tmp != null? tmp.toString():"";
		Double percentage=getPercentage();
		if(percentage!=null)
			{
			 percentage = percentage.doubleValue() * Double.valueOf( value2);
			value2 = String.valueOf( percentage.intValue()); 
			}
		return evaluate(value,value2);
		}
	
	
	private boolean evaluate(String value,String value2)
		{
		boolean conditionMet=false;
		try
			{
			preValidate();		    
		    if(value==null)return conditionMet;
			BigDecimal number=null,valNumber=null;
			Integer comparison=null;
			if(isNumericComparison())
				{
				number = new BigDecimal(value2 );
				valNumber = new BigDecimal(value);
				comparison = valNumber.compareTo(number);				
				}
			if     (condition.equals(">")) {conditionMet = comparison > 0;}
			else if(condition.equals(">=")){conditionMet = comparison >=0;}
			else if(condition.equals("<")) {conditionMet = comparison < 0;}
			else if(condition.equals("<=")){conditionMet = comparison <=0;}
			else if(condition.equals("<>")){conditionMet = !value2.equals(value);}
			else if(condition.equals("=")) {conditionMet = value2.equals(value);}
			else if(condition.equals("in") || condition.equals("Single-in")){conditionMet = inList(value, value2);}
			else if(condition.equals("Multiple-in")){conditionMet = inList(value,value2,true); }
			else if(condition.equals("contains")){conditionMet=contains(value,value2);}
			else if(condition.equals(CHECK_CORE_ID))
				{
				String fullName = TTTUtil.getUserFullName(value);
				if(fullName==null)
					{
					throw new Exception("User not found");
					}
				else
					{
					conditionMet = true;
					}
				}
			}
		catch(Throwable t){t.printStackTrace();}
		return conditionMet;
		}

	private String getCommentValue(IStatus status)
		{
		String comment=null;
		DurableStatus dStatus=null;
		if(status instanceof Machine){dStatus = (DurableStatus)((Machine)status).getContext().get("durableStatus");}
		else if(status instanceof DurableStatus){ dStatus = (DurableStatus)status;}
		if(dStatus != null){comment = dStatus.getLatestComment();}
		else {comment ="";}
		return comment;
		}
	
	}
