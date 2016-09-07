package com.freescale.stateStamper.model.entity;

import java.io.Serializable;

public class Area implements Serializable
	{

	private static final long serialVersionUID = -8834430586099940011L;
	
	public static final Integer AREA_ALL_PK=1;
	public static final String AREA_ALL_NAME="ALL";
	private Integer primaryKey;
	
	private String name;
	
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
	
	}
