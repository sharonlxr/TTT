package com.freescale.stateStamper.model.entity;

import java.io.Serializable;

public class Event implements Serializable
	{
	private static final long serialVersionUID = 1L;
	private Integer primaryKey;
	private String name;
	
	public Event clone(){
		Event event = new Event();
		event.setName( name);
		event.setPrimaryKey(primaryKey);
		return event;
	}
	
	public Integer getPrimaryKey()
		{
		return primaryKey;
		}
	
	public void setPrimaryKey(Integer primaryKey)
		{
		this.primaryKey = primaryKey;
		}

	public String getName()
		{
		return name;
		}

	public void setName(String name)
		{
		this.name = name;
		}
	
	public String toString()
		{
		return name;
		}
	
	}
