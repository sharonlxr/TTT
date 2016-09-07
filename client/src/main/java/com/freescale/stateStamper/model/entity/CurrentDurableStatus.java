package com.freescale.stateStamper.model.entity;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CurrentDurableStatus extends DurableStatus implements Serializable
	{
	private static final long serialVersionUID = 1632486196600178632L;
	public Integer primaryKey;
	public String eIName, mESName, type, location, state;
	public Integer equipmentType_pk, area_pk;
	public String serial_number, engineer_contact, vendor, workstation;
	public Date stateStart, eventStart;
	public Map context = new HashMap(), summing = new HashMap(), performance = new HashMap();;
	public float productiveTime;
	public boolean mesEvent;
	public Map<String, String> dynamicAttributes;
	private EquipmentType equipmentType;
	private Map<String, List<String>> children;
	public String platform, deviceGroup;

	public Map<String, List<String>> getChildren()
		{
		return children;
		}

	public void setChildren(Map<String, List<String>> children)
		{
		this.children = children;
		}

	public boolean hasChild()
		{
		if (children == null || children.isEmpty()) return false;
		Set<String> keyset = children.keySet();
		Iterator<String> it = keyset.iterator();
		while (it.hasNext())
			{
			List<String> childrenvector = children.get(it.next());
			if (!childrenvector.isEmpty()) return true;
			}

		return false;
		}

	public CurrentDurableStatus(DurableStatus durableStatus, Machine durable)
		{
		super.DURABLE_ID = durableStatus.DURABLE_ID;
		super.TIME = durableStatus.TIME;
		super.STATE = durableStatus.STATE;
		super.SUBSTATE = durableStatus.SUBSTATE;
		super.OPERATOR_ID = durableStatus.OPERATOR_ID;
		super.COMMENTS = durableStatus.COMMENTS;
		super.STATUS_PK = durableStatus.STATUS_PK;
		super.EQUIPMENT_PK = durableStatus.EQUIPMENT_PK;
		super.EVENT_PK = durableStatus.EVENT_PK;
		super.workstation = durableStatus.workstation;
		super.location = durableStatus.location;
		super.ATTRIBUTES = durableStatus.ATTRIBUTES;

		this.eIName = durable.eIName;
		this.mESName = durable.mESName;
		this.type = durable.type;
		this.location = durable.location;
		this.state = durable.state;
		this.equipmentType_pk = durable.equipmentType_pk;
		this.area_pk = durable.area_pk;
		this.serial_number = durable.serial_number;
		this.engineer_contact = durable.engineer_contact;
		this.vendor = durable.vendor;
		this.workstation = durable.workstation;
		this.stateStart = durable.stateStart;
		this.eventStart = durable.eventStart;
		this.context = durable.context;
		this.summing = durable.summing;
		this.performance = durable.performance;
		this.productiveTime = durable.productiveTime;
		this.mesEvent = durable.mesEvent;
		this.dynamicAttributes = durable.dynamicAttributes;
		this.equipmentType = durable.getEquipmentType();
		this.children = durable.getChildren();
		this.platform = durable.getPlatform();
		this.deviceGroup = durable.getDeviceGroup();
		initialize();
		}

	public CurrentDurableStatus()
		{
		initialize();
		}

	public DurableStatus getDurableStatus()
		{
		DurableStatus durablestatus = new DurableStatus();
		durablestatus.DURABLE_ID = this.DURABLE_ID;
		durablestatus.TIME = this.TIME;
		durablestatus.STATE = this.STATE;
		durablestatus.SUBSTATE = this.SUBSTATE;
		durablestatus.OPERATOR_ID = this.OPERATOR_ID;
		durablestatus.COMMENTS = this.COMMENTS;
		durablestatus.STATUS_PK = this.STATUS_PK;
		durablestatus.EQUIPMENT_PK = this.EQUIPMENT_PK;
		durablestatus.EVENT_PK = this.EVENT_PK;
		durablestatus.workstation = this.workstation;
		durablestatus.location = this.location;
		durablestatus.ATTRIBUTES = this.ATTRIBUTES;
		return durablestatus;
		}

	public CurrentDurableStatus(Integer equipmentType_pk)
		{
		this();
		this.equipmentType_pk = equipmentType_pk;
		}

	private void initialize()
		{
		Field[] f = getClass().getDeclaredFields();
		fields = new HashMap();
		for (int i = 0; i < f.length; i++)
			{
			fields.put(f[i].getName(), f[i]);
			}
		f = getClass().getSuperclass().getDeclaredFields();
		for (int i = 0; i < f.length; i++)
			{
			fields.put(f[i].getName(), f[i]);
			}
		}

	public Substate getSubstate()
		{
		Substate sub = new Substate();
		sub.equipmenttypePK = equipmentType_pk;
		sub.e10 = STATE;
		sub.substate = SUBSTATE;
		return sub;
		}

	public Object get(String name)
		{
		Object result = super.get(name);
		if (result == null && dynamicAttributes != null && dynamicAttributes.containsKey(name))
			{
			result = dynamicAttributes.get(name);
			}
		return result;
		}

	public boolean has(String name)
		{
		return super.has(name) || (dynamicAttributes != null && dynamicAttributes.containsKey(name));
		}

	public EquipmentType getEquipmentType()
		{
		return equipmentType;
		}

	public void setEquipmentType(EquipmentType esquipmentType)
		{
		this.equipmentType = esquipmentType;
		}

	}
