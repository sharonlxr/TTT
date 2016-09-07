package com.freescale.stateStamper.view;

import java.util.List;
import java.util.Vector;

import com.freescale.clientUtil.Table;
import com.freescale.clientUtil.Util;
import com.freescale.stateStamper.model.entity.EquipmentType;

public class EquipmentTypes extends Table {

	private static final long serialVersionUID = 1L;
	private StateStamperFrame stateStamperFrame;
	
	public EquipmentTypes (Vector records,StateStamperFrame stateStamperFrame){
		super("EquipmentTypes",records,null,false);
		this.stateStamperFrame = stateStamperFrame;
	}

	/**
	 * Overrides add method
	 */
	public void add() throws Throwable {Util.createWindow(type,new EquipmentTypeSetup(this,new EquipmentType(),this,stateStamperFrame),this);}
	
	/**
	 * Overrides edit method
	 */
	public void edit() throws Throwable {Util.createWindow(type,new EquipmentTypeSetup(this,((EquipmentType)records.elementAt(table.getSelectedRows()[0])),this,stateStamperFrame),this);}
	/**
	 * Overrides delete method to delete the equipment attributes of the Equipment Type
	 * */
	public void delete()throws Throwable
		{
		int idx = table.getSelectedRow();
		if(idx<0)return;
		EquipmentType type = (EquipmentType)records.get(idx);
		List attributes = type.getAttributes();
		for(int i=0;attributes!=null&&i<attributes.size();i++)
			{Util.request(attributes.get(i), "delete.do");}
		super.delete();
		}

}
