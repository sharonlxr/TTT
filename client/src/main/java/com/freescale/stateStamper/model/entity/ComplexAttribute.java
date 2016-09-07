package com.freescale.stateStamper.model.entity;

import java.util.Map;

public interface ComplexAttribute
	{
	public final static String SEPARATOR="$";
	public String getName();
	public void setName(String name);
	/**
	 * @returns this object as a map with the name of the attributes as the key 
	 * and the attribute's values a the values of the map 
	 * */
	public Map getAsMap();
	
	/**
	 * 
	 * */
	public boolean setAsMap(Map args);
	
	}
