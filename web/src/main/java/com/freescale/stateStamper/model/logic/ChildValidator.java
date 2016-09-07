package com.freescale.stateStamper.model.logic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import com.freescale.serverUtil.Persistence;
import com.freescale.stateStamper.model.entity.Machine;

public class ChildValidator
	{

	private static List<Machine> equipments = new ArrayList<Machine>();
	private static Map<String, Machine> equipmentMap = new HashMap<String, Machine>();
	private static Map<String, List<String>> childrenMap = new HashMap<String, List<String>>();
	private static Map<String, String> workstationMap = new HashMap<String, String>();

	@SuppressWarnings("unchecked")
	public static List<String[]> validate(Integer equipmentType) throws Throwable
		{
		List<String[]> results = new ArrayList<String[]>();
		equipments.clear();
		equipmentMap.clear();
		childrenMap.clear();
		workstationMap.clear();
		equipments = Persistence.search(new Machine());
		prepareEquipmentData();
		List<String> equipmentsList = new ArrayList<String>(equipmentMap.keySet());
		Collections.sort(equipmentsList);
		for (String mesName : equipmentsList)
			{
			Machine machine = equipmentMap.get(mesName);
			String equipmentTypeName = machine.getEquipmentType().name;
			Map<String, String> childResults = checkChildsAssociation(mesName);
			Map<String, List<String>> parentResults = checkParentAssociation(mesName);
			if(!machine.equipmentType_pk.equals(equipmentType))continue;
			String[] formatResultItem = formatResultItem(mesName, equipmentTypeName, parentResults, childResults);
			if(formatResultItem!=null)results.add(formatResultItem);
			}

		return results;
		}

	private static Map<String, List<String>> checkParentAssociation(String mesName)
		{
		Map<String, List<String>> parentResults = new HashMap<String, List<String>>();
		String parent = workstationMap.get(mesName);
		List<String> mismatchChilds = new ArrayList<String>();
		if (StringUtils.isNotEmpty(parent))
			{
			List<String> parentChilds = childrenMap.get(parent);
			if (parentChilds == null||!parentChilds.contains(mesName))
				{
				parentResults.put(parent, parentChilds);
				}
			}
		return parentResults;
		}

	private static Map<String, String> checkChildsAssociation(String mesName)
		{
		Map<String, String> childResults = new HashMap<String, String>();
		List<String> list = childrenMap.get(mesName);
		for (String child : list)
			{
			String parentMesName = workstationMap.get(child);
			if (!StringUtils.equalsIgnoreCase(parentMesName, mesName))
				{
				childResults.put(child, parentMesName);
				}
			}
		return childResults;
		}

	private static void prepareEquipmentData()
		{
		for (Machine machine : equipments)
			{
			String mesName = machine.mESName;
			equipmentMap.put(mesName, machine);
			Map<String, List<String>> childsMap = machine.getChildren();
			List<String> childrenList = new ArrayList<String>();
			for (Entry<String, List<String>> entry : childsMap.entrySet())
				{
				List<String> tempChildrenList = entry.getValue();
				for (String childMID : tempChildrenList)
					{
					if (StringUtils.isNotEmpty(childMID)) childrenList.add(childMID);
					}
				}
			childrenMap.put(mesName, childrenList);
			workstationMap.put(mesName, machine.workstation);
			}
		}

	private static String[] formatResultItem(String mesName, String equipmentTypeName, Map<String, List<String>> parentResults, Map<String, String> childResults)
		{
		String[] resultItem = new String[4];
		resultItem[0] = mesName;
		resultItem[1] = equipmentTypeName;
		StringBuilder parentResult = new StringBuilder();
		for (Entry<String, List<String>> entry : parentResults.entrySet())
			{
			String temp = null;
			if(entry.getValue()!=null&&entry.getValue().size()!=0)temp=StringUtils.join(entry.getValue().toArray(),",");
			parentResult.append("Actual parent: "+entry.getKey() + "; Actual parent's child: "+temp);
			}
		resultItem[2] = parentResult.toString();

		StringBuilder childResult = new StringBuilder();
		for (Entry<String, String> entry : childResults.entrySet())
			{
			childResult.append("Actual children: "+entry.getKey() + "; Actual children's parent: "+entry.getValue());
			}
		resultItem[3] = childResult.toString();
		
		if(parentResult.length()==0&&childResult.length()==0)return null;
		return resultItem;
		}
	}
