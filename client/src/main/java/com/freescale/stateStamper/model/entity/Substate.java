package com.freescale.stateStamper.model.entity;

import java.io.*;
import java.util.Comparator;

public class Substate implements Serializable
	{
    private static final long serialVersionUID = -9102496372353250946L;
    
	public Integer primaryKey;
	public String substate, group, color,e10;
	public Integer limit, equipmenttypePK;
	public static final String UNK = "Unknown";
	
	public Substate(){};
	
	public Substate(Integer equipmenttypePK){
		this.equipmenttypePK = equipmenttypePK;
	}
	
	public Substate(Integer equipmenttypePK,String subState)
		{
		if(subState == null || subState.trim().equals("") || ! subState.contains("-"))
			{throw new IllegalArgumentException("substate does not have the proper format e10-substate");}
		String states [] = subState.split("-"); 
		this.equipmenttypePK = equipmenttypePK;;
		e10 = states[0];
		substate = states[1];
		}

	public String toString(){
		return "Substate " + substate + " group: " + group + " color " + color + " e10 " + e10 + " equipmenttypePK " + equipmenttypePK;
	}
	
	public boolean belongsto(String group){
		return this.group.equalsIgnoreCase(group);
	}
	
	public boolean equals(String substate){
		return this.substate.equalsIgnoreCase(substate);
	}

	public boolean equals(Object obj)
		{
		if (this == obj)return true;
		if (obj == null)return false;
		if (getClass() != obj.getClass())return false;
		Substate other = (Substate) obj;
		if (e10 == null)
			{
			if (other.e10 != null)
				return false;
			} else if (!e10.equals(other.e10))
			return false;
		if (substate == null)
			{
			if (other.substate != null)
				return false;
			} else if (!substate.equals(other.substate))
			return false;
		return true;
		}
	
	public int hashCode()
		{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((e10 == null) ? 0 : e10.hashCode());
		result = prime * result
				+ ((substate == null) ? 0 : substate.hashCode());
		return result;
		}

	public class CompBySubstate implements Comparator
		{
		public int compare(Object o1, Object o2)
			{
			return ((Substate) o1).substate.compareToIgnoreCase(((Substate) o2).substate);
			}
		}
	
    }