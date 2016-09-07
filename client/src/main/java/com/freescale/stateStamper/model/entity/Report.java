
package com.freescale.stateStamper.model.entity;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

public class Report implements Serializable
	{
    private static final long serialVersionUID = -9102496372353250946L;
    
	public Integer reportPK;
	public Map machines;
	public String[] types, locations,filterlist;
	public Object[] dates;
	public Date startDate, endDate;
	public Vector exclusions , emails ;
	public String period, reportType, grouping, chartType,totals, condition;
	public Double numberPeriods;
	public Boolean isHTML ;
	public Boolean includeAll;
	private Integer equipmentTypePk;
	public EquipmentType equipmentType;
	public Area area;
	private String orderBy, displayLocation;
	public Boolean transitionOnly = false;
	public Boolean alternativeDisplay = false;
	private String[] columns;
	public Boolean shiftHours;
	
	
	public Boolean isTransitionOnly()
		{
		return transitionOnly;
		}

	public void setTransitionOnly(Boolean transitionOnly)
		{
		this.transitionOnly = transitionOnly;
		}

	public Boolean isAternativeDisplay()
		{
		return alternativeDisplay;
		}

	public void setAternativeDisplay(Boolean aternativeDisplay)
		{
		this.alternativeDisplay = aternativeDisplay;
		}

	public String getOrderBy()
		{
		return orderBy;
		}

	public void setOrderBy(String orderBy)
		{
		this.orderBy = orderBy;
		}

	private Map filters;
	public Map getFilters()
		{
		return filters;
		}

	public void setFilters(Map filters)
		{
		this.filters = filters;
		}

	public String toString() {return period+" "+reportType+" "+grouping;}
	
	public String toExtString(){return "period "+ period + " type " + reportType + " grouping " + grouping + " dates " + getDatesAsString() + " machines " + getMachinesAsString(); }
	
	/**
	 * @return a string with the list of machines names separated by commas
	 * */
	public String getMachinesAsString(){
		if(machines==null){return null;}
		StringBuffer str= new StringBuffer();
		Set keySet = machines.keySet();
		for(Object key:keySet){
			str.append(key);
			str.append(",");
		}
		//remove the machines in the exclusion list
		if(exclusions!=null && exclusions.size() > 0){
			Machine mac;
			int pos;
			for(int i=0;i<exclusions.size();i++){
				mac= (Machine)exclusions.get(i);
				pos = str.indexOf(mac.mESName+","); 
				if(pos > -1 ){
					str.delete(pos,pos + mac.mESName.length()+1);
				}
			}
		}
		return str.substring(0, str.lastIndexOf(","));
	}
	
	/**
	 * Returns a string with a list separated by commas
	 * @param arr the array to convert to string
	 * @return a string with elements separated by commas
	 * */
	public String getDatesAsString(){
		StringBuilder str = new StringBuilder();
		if(dates !=null){
			for(int i=0; i< dates.length; i++){
				str.append(dates[i].toString()+",");
			}
			int idx = str.lastIndexOf(",");
			if(idx >-1)str.deleteCharAt(idx);
		}
		return str.toString();
	}
	
	public EquipmentType getEquipmentType()
		{
		return equipmentType;
		}
	
	public void setEquipmentType(EquipmentType type)
		{
		equipmentType=type;
		equipmentTypePk = equipmentType.primaryKey;
		}

	public Integer getEquipmentTypePk()
		{
		return equipmentTypePk;
		}

	public void setEquipmentTypePk(Integer equipmentTypePK)
		{
		equipmentTypePk = equipmentTypePK;
		if(equipmentType != null && !equipmentType.primaryKey.equals(equipmentTypePk)){equipmentTypePk=null;}
		}

	public String[] getColumns()
		{
		return columns;
		}

	public void setColumns(String[] columns)
		{
		this.columns = columns;
		}
	
	/**
	 * @return is the report is for testers.
	 * by default a report is for tester; if there is no equipment type defined or 
	 * if the equipment type defined does not have name or name is empty
	 * */
	public boolean isTesterReport()
		{
		return getEquipmentType() == null || getEquipmentType().name == null || 
		       getEquipmentType().name == ""  || getEquipmentType().name.equalsIgnoreCase("Tester");	
		}

	public Area getArea()
		{
		return area;
		}

	public void setArea(Area area)
		{
		this.area = area;
		}
	
	public boolean hasEmailRecipients()
		{
		return emails != null && emails.size() > 0;
		}
	
	/**
	 * @return a vector of strings with the To e-mails 
	 * */
	public Vector<String> getToEmails()
	{
	return getEmailsByType(Email.TO);
	}
	
	/**
	 * @return a vector of strings with the Cc e-mails 
	 * */
	public Vector<String> getCcEmails()
	{
	return getEmailsByType(Email.CC);
	}
	
	
	private Vector<String> getEmailsByType(String type)
		{
		Vector<String> filteredMails = new Vector<String>();
		if (emails != null)
			{
			for (Object obj : emails)
				{
				Email email = (Email) obj;
				if (email.type.equals(type) ||  ( type.equals(Email.TO) && email.type == null ) )
					{
					filteredMails.add(email.email);
					}
				}
			}
		return filteredMails;
		}
	
	}
