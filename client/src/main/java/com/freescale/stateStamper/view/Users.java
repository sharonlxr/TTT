package com.freescale.stateStamper.view;

import java.util.Collections;
import java.util.Map;
import java.util.Vector;

import org.apache.commons.lang.StringUtils;

import com.freescale.clientUtil.Form;
import com.freescale.clientUtil.Table;
import com.freescale.clientUtil.Util;
import com.freescale.stateStamper.model.entity.Administrator;
import com.freescale.stateStamper.model.entity.Area;
import com.freescale.stateStamper.model.entity.EquipmentType;
import com.freescale.stateStamper.model.entity.Role;

public class Users extends Table{
	private StateStamperFrame stateStamperFrame;
	public Users(String type, Vector records,StateStamperFrame stateStamperFrame) throws Throwable
		{
		this.type = type;
		this.records = records;
		this.stateStamperFrame = stateStamperFrame;
		resetProperty();
		init(false);
		setAdminRoleName();
		}

	public void resetProperty() throws Throwable
		{
		Util.setProperty("Users.properties","coreId,firstName,lastName,roleName,eMail,areaName");
		Util.setProperty("Users.required", "Y,Y,Y,Y,N,Y");
		Util.setProperty("Users.length", "16,16,16,44,16,32");
		Util.setProperty("Users.areaName.values", StringUtils.join(stateStamperFrame.getAreaNames(),","));
		Util.setProperty("Users.roleName.values", stateStamperFrame.getRoleValues());
		sort = true;
		}
	
	public void save(Form form) throws Throwable
		{
		Administrator user = (Administrator) form.getObject();
		String selectedRoleName = user.roleName;
		Role role = new Role();
		Area area = stateStamperFrame.getAreaByName(user.getAreaName());
		user.setArea(area);
		user.setAreaPK(area.getPrimaryKey());

		if (selectedRoleName.equalsIgnoreCase(Role.ADMIN))
			{
			role.rolename = selectedRoleName;
			}
		else
			{
			String values[] = selectedRoleName.split("-");
			if (values.length == 2)
				{
				role.rolename = values[1];
				EquipmentType type = new EquipmentType();
				type.name = values[0];
				type = (EquipmentType) (((Vector) Util.request(type, "search.do")).get(0));
				role.equipmenttypePK = type.primaryKey;
				}
			else if (values.length == 1)
				{
				role.rolename = values[0];
				role.equipmenttypePK = 0;
				}
			}

		role = (Role) ((Vector) Util.request(role, "search.do")).get(0);
		user.rolePK = role.getRolePK();
		user.setRole(role);
		Util.request(user, "store.do");
		form.cancel();

		refresh();
		}

	private void refresh() throws Throwable
		{
		this.records = (Vector)Util.request(new Administrator(),"search.do",false);
		Collections.sort(records);
		setAdminRoleName();
		Util.createWindow("Success", new Form("Success", "", this, 0), this);
		}
	
	private void setAdminRoleName() throws Throwable
		{
		Map<Integer,EquipmentType> types = (Map<Integer,EquipmentType>)Util.request(null, "getEquipmentTypes.do", false);
		for (int i = 0; i < records.size(); i++)
			{
			Administrator admin = (Administrator) (records.get(i));
			if (admin.getRole().equipmenttypePK.equals(0))
				{
				admin.roleName = admin.getRole().rolename;
				}
			else
				{
				admin.roleName =  types.get(admin.getRole().equipmenttypePK).name + "-" + admin.getRole().rolename;
				}
			}
		}
	
	public void add() throws Throwable
		{
		Administrator admin = new Administrator();
		Util.createWindow(type, new Form(type, admin, this), this);
		}
	
	public void edit() throws Throwable
		{
		Administrator admin = (Administrator) (records.elementAt(table.getSelectedRows()[0]));
		Util.createWindow(type, new Form(type, admin, this), this);
		}

	public void delete() throws Throwable
		{
		if (table.getSelectedRows().length == 0 || table.getSelectedRows()[0] >= records.size()) return;
		Util.request(records.elementAt(table.getSelectedRows()[0]), "delete.do");
		refresh();
		}
	public void cancel(Form form) 
		{
		form.cancel();
		}
	}
