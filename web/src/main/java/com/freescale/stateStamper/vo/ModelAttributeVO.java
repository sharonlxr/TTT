package com.freescale.stateStamper.vo;

import java.util.Set;

public class ModelAttributeVO
	{
	private String name;
	private boolean required;
	private Set<String> valueList;
	public String getName()
		{
		return name;
		}
	public void setName(String name)
		{
		this.name = name;
		}
	public boolean isRequired()
		{
		return required;
		}
	public void setRequired(boolean required)
		{
		this.required = required;
		}
	public Set<String> getValueList()
		{
		return valueList;
		}
	public void setValueList(Set<String> valueList)
		{
		this.valueList = valueList;
		}
	
	}
