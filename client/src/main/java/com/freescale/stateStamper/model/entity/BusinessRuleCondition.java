package com.freescale.stateStamper.model.entity;

import java.io.Serializable;
import java.math.BigDecimal;

public class BusinessRuleCondition implements Serializable
	{
	private static final long serialVersionUID = 1L;
	private Integer primaryKey;
	private String attribute;
	private String condition;
	private String condValue;
	private Integer businessRulePk;
	public Integer getPrimaryKey()
		{
		return primaryKey;
		}
	public void setPrimaryKey(Integer primaryKey)
		{
		this.primaryKey = primaryKey;
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
	/**
	 * @returns the percentage string in the condition value
	 *  e.g. condValue = 90%Value it returns the string 90%
	 * */
	public String getPercentageStr()
		{
		String perStr=null;
		if(condValue!=null && condValue.indexOf('%')!=-1){perStr = condValue.substring(0,condValue.indexOf('%')+1 );}
		return perStr;
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
		
	public String getCondValue()
		{
		return condValue;
		}	
	public void setCondValue(String condValue)
		{
		this.condValue = condValue;
		}
	public Integer getBusinessRulePk()
		{
		return businessRulePk;
		}
	public void setBusinessRulePk(Integer businessRulePk)
		{
		this.businessRulePk = businessRulePk;
		}
	
	
	private boolean isNumericComparison()
		{
		condition = condition.trim();
		return condition.equals(">")||
		       condition.equals("<")||
		       condition.equals(">=")||
		       condition.equals("<=");
		}
	
	private void preValidate()throws Throwable
		{
		if (condition==null||condition.equals("") ||			
		    condValue==null||condValue.equals("") ||				
		    attribute==null||attribute.equals("")) throw new Throwable("Condition is not defined properly");
		}
	
	private boolean inList(String value)
		{
		String items[] = condValue.split(",");
		boolean found=false;
		for (int i = 0;items!=null && i < items.length&&!found; i++)
			{
			found=items[i].equalsIgnoreCase(value);
			}
		return found;
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
	
	public boolean isMet(IStatus status)
		{
		String value = null;
		String value2=null;	
		Object tmp=null;
		if(status==null || !status.has(attribute) )return false;
		tmp = status.get(attribute);		
		if(tmp==null){return false;}
		value = tmp.toString();
		value2 = (String)(status.get(getCondValuewithoutPercentange()));		
		if(value2==null)value2=condValue;
		Double percentage=getPercentage();
		if(percentage!=null)
			{
			 percentage = percentage.doubleValue() * Double.valueOf( value2);
			value2 = String.valueOf( percentage.longValue()); 
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
			else if(condition.equals("in")){conditionMet = inList(value);}
			else if(condition.equals("contains")){conditionMet=contains(value,value2);}
			}
		catch(Throwable t){}
		return conditionMet;
		}
	
	/**
	 * @return a extended string version of this condition consisting of attribute + condition + condValue 
	 * */
	public String toExtString()
		{
		return attribute + " " + condition  + " " + condValue;		
		}
	
	
	public String toExtStringWithAlias(EquipmentType eqType)
		{
		String attributeDisplay = attribute,condValueDisplay = condValue,condValueField;
		if(eqType.hasAttribute(attributeDisplay))
			{
			attributeDisplay = eqType.getAttributeAlias(attributeDisplay);
			}
		boolean percentage = condValue.indexOf("%") != -1 ;
		condValueField = condValueDisplay.substring(condValueDisplay.indexOf("%")+1);
		if(eqType.hasAttribute(condValueField))
			{
			condValueDisplay = eqType.getAttributeAlias(condValueField);
			}
		if(percentage){condValueDisplay = condValue.substring(0,condValue.indexOf("%")+1) + condValueDisplay; }
		return attributeDisplay + " " + condition  + " " + condValueDisplay;
		}

	
	}
