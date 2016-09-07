package com.freescale.stateStamper.view;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import javax.swing.JOptionPane;

import org.apache.commons.lang.StringUtils;

import com.freescale.clientUtil.Form;
import com.freescale.clientUtil.Table;
import com.freescale.clientUtil.Util;
import com.freescale.stateStamper.model.entity.*;


public class Equipments extends Table {
	private static final long serialVersionUID = 1L;
	private EquipmentType equipmentType;
	private Area area;
	private StateStamperFrame stateStamperFrame;
	private String fieldsObj;
	private String lenObj;
	private String requiredObj;
	public Equipments(EquipmentType equipmenttype, Area area, StateStamperFrame stateStamperFrame) throws Throwable
		{
		type = "Machines"; 	
		this.equipmentType=equipmenttype;
		this.area = area;
		this.stateStamperFrame = stateStamperFrame;
		resetProperty();
		init(true);
		search();		
		}
	
	public void search() throws Throwable
		{
		setPreferredSize(new Dimension(800,600));
		records = (Vector) Util.request(new Machine(equipmentType.primaryKey),"search.do",false);
		for(Iterator iterator = records.iterator();iterator.hasNext();)
			{
			Machine equipment = (Machine) iterator.next();
			if( !Area.AREA_ALL_NAME.equalsIgnoreCase(area.getName())  && !area.getPrimaryKey().equals(equipment.getArea_pk()))
				{
				iterator.remove();
				}
			}
		Collections.sort(records, new SortDurable("eIName"));
		model.fireTableRowsInserted(0, records.size() - 1);
		}
	
	public void save() throws Throwable
		{
		Util.request(records,"store.do");
		cancel();
		Util.createWindow("Success",new Form("Success","",this),this);
		stateStamperFrame.refreshMachines();
		}
	
	public void save(EquipmentForm form)throws Throwable{
		Machine equip = (Machine)form.getObject();
		equip.setEquipmentType(equipmentType);
		equip.equipmentType_pk = equipmentType.primaryKey;
		Area newArea = stateStamperFrame.getAreaByName(equip.getAreaName());
		equip.setArea(newArea);
		equip.setArea_pk(newArea.getPrimaryKey());
		Administrator admin  = (Administrator)Util.request(null,"getCurrentUser.do",false);
		if (!equipmentType.name.equalsIgnoreCase("Tester")){
			DurableStatus status = (DurableStatus)equip.getContext().get("durableStatus");	
			if (status!=null) {
				status.OPERATOR_ID = admin.coreId;
			}
			else {
			    if(equip.context == null){equip.context = new HashMap();}
				equip.context.put("OPERATOR_ID",admin.coreId);
			}
		}
		super.save(form);
		stateStamperFrame.refreshMachines();
	}
	
	public void delete() throws Throwable
		{
		Machine equip = (Machine) records.elementAt(table.getSelectedRows()[0]);
		int option = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete " + equip.mESName + " ?","Delete", JOptionPane.YES_NO_OPTION,JOptionPane.WARNING_MESSAGE);
		if(option != JOptionPane.OK_OPTION){return;}
		if (!equipmentType.name.equalsIgnoreCase("Tester"))
			{
			Administrator admin = (Administrator) Util.request(null, "getCurrentUser.do", false);
			DurableStatus status = (DurableStatus) equip.getContext().get("durableStatus");
			if (status != null){status.OPERATOR_ID = admin.coreId;}
			}
		super.delete();
		stateStamperFrame.refreshMachines();
		}
	
	public void add() throws Throwable
	{		
	Machine durable = new Machine();	
	durable.setEquipmentType(equipmentType);
	resetProperty();
	setDropDownValues(durable);
	if (!editable)
		{
		Vector v= equipmentType.getEquipmentAttributes();
	    for(int i=0;v!=null&& i< v.size();i++)
	    	{
	    	EquipmentAttribute att = (EquipmentAttribute)v.get(i);
	    	if(att.type.equals("ThresholdCounter") || att.name.endsWith("eIVariable") || att.name.endsWith("units")){
	    	v.remove(i--);
	    	}
	    	}
	    Util.createWindow(type, new EquipmentForm(type, durable, this,v), this);

		} 
	else
		{
		int index = records.size();
		if (getSelectedRows().length > 0 && getSelectedRows()[0] < records.size()) index = getSelectedRows()[0];
		records.insertElementAt(Class.forName(Util.getProperty(type + ".object")).newInstance(), index);
		rowsInserted(0, records.size() - 1);
		}
	}
	public void cancel(EquipmentForm form) throws Throwable{
		form.cancel();
		search();			
	}
	
	public void edit() throws Throwable
	{
	Machine durable = (Machine)(records.elementAt(table.getSelectedRows()[0]));
	resetProperty();
	setDropDownValues(durable);
	if (!editable) {
	    Vector v= equipmentType.getEquipmentAttributes();
	    for(int i=0;v!=null&& i< v.size();i++)
	    	{
	    	EquipmentAttribute att = (EquipmentAttribute)v.get(i);
	    	if(att.type.equals("ThresholdCounter") || att.name.endsWith("eIVariable") || att.name.endsWith("units")){
	    	v.remove(i--);
	    	}
	    	}
		Util.createWindow(type, new EquipmentForm(type,durable, this,v), this);
	}
	}

	private void setDropDownValues(Machine durable) throws Throwable
		{
		setWorkstation(durable);   
		setLocation(durable);
		setAreas();
		setMachineModel(durable);
		setMachinePlatform(durable);
		}
	
	private void resetProperty()throws Throwable
		{
		List<BusinessRule> rules = equipmentType.getBusinessRulesBy(BusinessRule.ASSOCIATE);
		boolean workstation = true;
		boolean childModel = false;
		boolean modelDescription = false;
		String platformLength = "16";
		String modelLength = "16";
		for (BusinessRule rule : rules)
			{
			if (rule.hasConditions())
				{
				workstation = false;
				break;
				}
			}
		
		if("MES".equals(equipmentType.getChildSource()))
			{
			childModel = true;
			}
		if("MES".equals(equipmentType.getPlatformSource())||"TTT".equals(equipmentType.getPlatformSource()))
			{
			platformLength ="32";
			}
		if("MES".equals(equipmentType.getModelSource()))
			{
			modelLength = "32";
			}
		else if("TTT".equals(equipmentType.getModelSource()))
			{
			modelLength = "16";
			modelDescription = true;
			}
		if(equipmentType.name.equals("Tester"))
			{
			fieldsObj = "eIName,mESName,type,areaName,platform,modelName,location,pic_Host,pic_Port,dmhbox";
			requiredObj = "Y,Y,Y,Y,Y,Y,N,N,N,N";
			lenObj = "16,16,16,16,16,16,32,16,16,16";
			}
		else 
			{
			fieldsObj = "eIName,type,areaName,platform,modelName"+(modelDescription?",modelDescription":"")+(childModel?",childModel":"")+",location,serial_number,engineer_contact,deviceGroup,vendor"+(workstation?",workstation":"");
			requiredObj=  "Y,Y,Y,Y,Y"+(modelDescription?",Y":"")+(childModel?",N":"")+",N,N,N,N,N"+(workstation?",N":"");
			lenObj = "16,16,16,"+platformLength+","+modelLength+(modelDescription?",48":"")+(childModel?",32":"")+",32,32,16,16,16"+(workstation?",32":"");
			}
		
		Util.setProperty(type+".properties",fieldsObj);
		Util.setProperty(type+".required", requiredObj);
		Util.setProperty(type+".length", lenObj);
		}
	
	public void setMachineModel(Machine durable) throws Throwable
		{
		if("MES".equals(equipmentType.getModelSource()))
			{
			List<String> modelList = (List<String>)Util.request(null,"getMachineModel.do",false);
			Collections.sort(modelList);
			Util.setProperty("Machines.modelName.values", ","+StringUtils.join(modelList, ","));
			if(durable.getModelName()!=null&&"MES".equals(equipmentType.getChildSource()))
				{
				List<String> childModelList = (List<String>)Util.request(durable.getModelName(),"getChildModel.do",false);
				durable.setChildModel(StringUtils.join(childModelList,","));
				}
			}
		else if("TTT".equals(equipmentType.getModelSource()))
			{
			if(durable.getModel()!=null)
				{
				durable.setModelDescription(durable.getModel().getModelDescription());
				}
			List<String> modelList = (List<String>)Util.request(null,"getMachineModel.do",false);
			Collections.sort(modelList);
			Util.setProperty("Machines.modelDescription.values", ","+StringUtils.join(modelList, ","));
			}
		}
	
	public void setMachinePlatform(Machine durable) throws Throwable
		{
		if(equipmentType.getPlatformSource()!=null&&!"MANUAL".equals(equipmentType.getPlatformSource()))
			{
			List<String> platformList = (List<String>)Util.request(null,"getMachinePlatform.do",false);
			Collections.sort(platformList);
			Util.setProperty("Machines.platform.values", ","+StringUtils.join(platformList, ","));
			}
		}
	
	private void setAreas() throws Throwable
		{
		Vector<String> areaNames = new Vector<String>();
		if(area.getPrimaryKey().equals(Area.AREA_ALL_PK))
			{
			areaNames = stateStamperFrame.getAreaNamesWithoutAll();
			}
		else
			{
			areaNames.add(area.getName());
			}
		Util.setProperty("Machines.areaName.values", StringUtils.join(areaNames.iterator(), ","));
		}
	
	public void setWorkstation(Machine durable) throws Throwable
		{
		String parents = "";
		if (durable.primaryKey == null)
			{
			List<BusinessRule> rules = durable.getEquipmentType().getBusinessRulesBy(BusinessRule.ASSOCIATE);
			for (BusinessRule rule : rules)
				{
				if (rule.hasConditions())
					{
					return;
					}
				}
			}
		if (durable.workstation != null && (!durable.workstation.trim().equals("")))
			parents = durable.workstation + ", ";
		List parentList = (List) Util.request(durable, "getParentList.do", false);
		if (parentList != null && !parentList.isEmpty())
			{
			String parentListStr = parentList.toString();
			parentListStr = parentListStr.substring(1, parentListStr.length() - 1);
			if (!parents.equals("")){parentListStr = parents  + "," + parentListStr;}
			else { parentListStr = " ,"+parentListStr;}
			parents = parentListStr;
			}
		Util.setProperty("Machines.workstation.values", parents);
		}
	
	public List getLocation(Machine durable) throws Throwable{
		return (ArrayList)Util.request(durable,"getlocationList.do",false);	
	}
	
	public void setLocation(Machine durable) throws Throwable{
		List list = getLocation(durable);
		if (list ==null||list.isEmpty()){
			list = new ArrayList();
			list.add(Location.DEFAULT_LOCATION);
		}
				
		String locationlist = list.toString();
		locationlist = locationlist.substring(1, locationlist.length()-1);			
		Util.setProperty("Machines.location.values", locationlist);
		
	}
	
}
