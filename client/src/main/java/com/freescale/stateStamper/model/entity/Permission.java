package com.freescale.stateStamper.model.entity;

import java.io.Serializable;
import java.util.List;
import java.util.Vector;

public class Permission implements Serializable{

	public Integer permissionPK; 
	public String action; 
	public Integer stateTransitionPK;
	public String[] subaction;

	public Integer getPermissionPK()
		{
		return permissionPK;
		}
	
	public void setPermissionPK(Integer permissionPK)
		{
		this.permissionPK = permissionPK;
		}
	
	
	private List roles;

	public List getRoles()
		{
		return roles;
		}

	public void setRoles(List roles)
		{
		this.roles = roles;
		}
	
	public String toString()
		{
		return action;
		}

}
