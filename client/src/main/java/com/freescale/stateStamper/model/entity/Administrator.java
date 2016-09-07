package com.freescale.stateStamper.model.entity;

import java.io.Serializable;


public class Administrator implements Serializable,Comparable
	{
	private static final long serialVersionUID = -9102496372353250946L;
	
	public Integer tttUserPk,rolePK,areaPK,equipmentTypePK;
	private Role role;
	private Area area;
	
	public Role getRole()
		{
		 return role;
		}
	
	public void setRole(Role role)
		{
		this.role=role;
		}
	
	
	public Integer getEquipmentTypePK() {
		return equipmentTypePK;
	}

	public void setEquipmentTypePK(Integer equipmentTypePK) {
		this.equipmentTypePK = equipmentTypePK;
	}

	public String getEquipmentType() {
		return equipmentType;
	}

	public void setEquipmentType(String equipmentType) {
		this.equipmentType = equipmentType;
	}

	public String coreId, password,firstName, lastName, eMail, equipmentType, roleName;
	
	public void setCoreId(String coreid){
		coreId = coreid;
	}
	
	public String getCoreId(){
		return coreId;
	}
	
	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public int getTttUserPk() 
		{
		int pk=0;
		if(tttUserPk!=null)pk=tttUserPk.intValue();
		return pk;
		}
	
	public void setTttUserPk(int i)
		{
		tttUserPk =i;
		}
	
	public void setTttUserPk(Integer tttUserPk) {
		this.tttUserPk = tttUserPk;
	}

	public int getRolePK() {
		return rolePK.intValue();
	}

	public void setRolePK(int rolePK) {
		this.rolePK = new Integer(rolePK);
	}	
	
	public Integer getAreaPK() {
		return this.areaPK;
	}

	public void setAreaPK(Integer areaPk) {
		this.areaPK = areaPk;
	}

	public boolean isAdministrator()
		{
		return  role !=null && getRole().isAdministrator();
		}
	
	public Administrator() {}
	
	public Administrator(String coreId, String firstName, String lastName)
		{
		this.coreId = coreId;
		this.firstName = firstName;
		this.lastName = lastName;
		}

	public Area getArea()
		{
		return area;
		}

	public void setArea(Area area)
		{
		this.area = area;
		}

	public String getAreaName()
		{
		if(area!=null)
			{
			return area.getName();
			}
		else
			return "";
		}

	public void setAreaName(String areaName)
		{
		if(area==null)
			{
			area = new Area();
			}
		this.area.setName(areaName);
		}
	
	public String getDisplayName()
		{
		return coreId+"-"+lastName+" "+firstName;
		}

	public int compareTo(Object o)
		{
		Administrator b = (Administrator)o;
		return coreId.toUpperCase().compareTo(b.coreId.toUpperCase());
		}
	}
