package com.freescale.stateStamper.model.entity;

import java.io.Serializable;
import java.util.List;
import java.util.Vector;
public class Role implements Serializable{
	private static final long serialVersionUID = 1L;
	public String rolename;
	public Integer equipmenttypePK,rolePK;
	public static final String ADMIN = "Administrator";
	public static final String GUEST = "Guest";
	/**ALL_EQUIPMENTS equipmentPk to indicate this role si for all equipments*/
	public static final int ALL_EQUIPMENTS=0;

	public Integer getRolePK()
		{
		return rolePK;
		}
	public void setRolePK(Integer rolePK)
		{
		this.rolePK = rolePK;
		}
	
	
	private List permissions;

	public List getPermissions()
		{
		return permissions;
		}
	public void setPermissions(List permissions)
		{
		this.permissions = permissions;
		}
	
	public String toString()
		{
		return rolename+":"+equipmenttypePK ;
		}
	
	/**
	 * @return true if this role is an Adminsitrator
	 * */
	public boolean isAdministrator()
		{
		return ADMIN.equals(rolename);
		}
	
	/**
	 * @return true if this role is for All equipments
	 * */
	public boolean isMultiEquipment()
		{
		return isAdministrator() ||  ALL_EQUIPMENTS == equipmenttypePK; 
		}

}
