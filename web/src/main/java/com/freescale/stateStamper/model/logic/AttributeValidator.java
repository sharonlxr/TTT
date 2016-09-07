package com.freescale.stateStamper.model.logic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.freescale.serverUtil.Persistence;
import com.freescale.stateStamper.model.entity.CurrentDurableStatus;
import com.freescale.stateStamper.model.entity.EquipmentAttribute;
import com.freescale.stateStamper.model.entity.EquipmentType;

public class AttributeValidator
	{
	private static List<EquipmentType> equipmentTypes = new ArrayList<EquipmentType>();
	private static List<EquipmentAttribute> attributes = new ArrayList<EquipmentAttribute>();
	private static Map<Integer, List<String>> machineAttributesMap = new HashMap<Integer, List<String>>();
	private static Map<Integer, List<String>> statusAttributesMap = new HashMap<Integer, List<String>>();
	private static Integer testerEquipmentTypePK=null;

	public static List<String[]> validate() throws Throwable
		{
		return checkDurable();
		}

	private static void initAttributeMap() throws Throwable
		{
		equipmentTypes = Persistence.search(new EquipmentType());
		for (EquipmentType et : equipmentTypes)
			{
			machineAttributesMap.put(et.primaryKey, new ArrayList<String>());
			statusAttributesMap.put(et.primaryKey, new ArrayList<String>());
			if("Tester".equalsIgnoreCase(et.name))
				{
				testerEquipmentTypePK=et.primaryKey;
				}
			}
		attributes = Persistence.search(new EquipmentAttribute());
		List<String> list;
		for (EquipmentAttribute attr : attributes)
			{
			if (attr.isEquipmentAttribute()&& !attr.name.endsWith("eIVariable") && !attr.name.endsWith("units"))
				{
				list = machineAttributesMap.get(attr.typeKey);
				list.add(attr.name);
				}
			else if (attr.isStatusAttribute())
				{
				list = statusAttributesMap.get(attr.typeKey);
				list.add(attr.name);
				}
			}
		}

	private static void clearAll()
		{
		testerEquipmentTypePK=null;
		equipmentTypes.clear();
		attributes.clear();
		machineAttributesMap.clear();
		statusAttributesMap.clear();
		}

	private static List<String[]> checkDurable() throws Throwable
		{
		clearAll();
		initAttributeMap();
		List<String[]> results = new ArrayList<String[]>();
		List<CurrentDurableStatus> list = Persistence.search(new CurrentDurableStatus());
		List<String> missAttrs = new ArrayList<String>();
		for (CurrentDurableStatus item : list)
			{
			if (item.equipmentType_pk.intValue() == testerEquipmentTypePK.intValue()) continue;
			Map<String, String> equipAttributes = item.dynamicAttributes;
			Map<String, String> statusAttributes = item.ATTRIBUTES;
			List<String> machineAttributeList = machineAttributesMap.get(item.equipmentType_pk);
			List<String> statusAttributeList = statusAttributesMap.get(item.equipmentType_pk);
			for (String machineAttributeName : machineAttributeList)
				{
				Object machineAttributeValue = equipAttributes.get(machineAttributeName);
				if (machineAttributeValue == null)
					{
					missAttrs.add(machineAttributeName);
					}
				}
			if(missAttrs.size()>0)results.add(generateResultRecord(item.DURABLE_ID,"EQUIPMENT",item.EQUIPMENT_PK,missAttrs));
			missAttrs.clear();
			for (String statusAttributeName : statusAttributeList)
				{
				Object statusAttributeValue = statusAttributes.get(statusAttributeName);
				if (statusAttributeValue == null)
					{
					missAttrs.add(statusAttributeName);
					}
				}
			if(missAttrs.size()>0)results.add(generateResultRecord(item.DURABLE_ID,"DURABLE_STATUS",item.STATUS_PK,missAttrs));
			missAttrs.clear();
			}
		return results;
		}

	private static String[] generateResultRecord(String mid,String table, Integer primaryKey,List<String> missingAttributes)
		{
		String[] record = new String[4];
		record[0] = mid;
		record[1] = table;
		record[2] = String.valueOf(primaryKey);
		record[3] = StringUtils.join(missingAttributes.toArray(),",");
		return record;
		}

	}
