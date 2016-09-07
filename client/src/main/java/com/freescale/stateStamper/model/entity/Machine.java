package com.freescale.stateStamper.model.entity;

import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;

import org.apache.commons.lang.StringUtils;

public class Machine extends IStatus implements Serializable, Cloneable
	{
	private static final long serialVersionUID = -9102496372353250946L;

	public Integer primaryKey;
	public String eIName, mESName, type, location, state;
	public Integer equipmentType_pk, area_pk;
	public String serial_number, engineer_contact, vendor, workstation;
	public Date stateStart, eventStart;
	public Map context = new HashMap(), summing = new HashMap(), performance = new HashMap();
	public float productiveTime;
	public boolean mesEvent;
	private EquipmentType equipmentType;
	public String pic_Host, pic_Port, dmhbox;
	public String platform, deviceGroup;
	public Map<String, String> dynamicAttributes;
	public Map<String, List<String>> children;
	private Vector attachments;
	private Area area;
	private transient Status currentStatus;
	private String modelName;
	private Model model;
	private String childModel;
	private Integer modelPk;
	private String modelDescription;

	public Vector getAttachments()
		{
		return attachments;
		}

	public void setAttachments(Vector attachments)
		{
		this.attachments = attachments;
		}

	public void addAttachment(Attachment attachment)
		{
		attachments.add(attachment);
		}

	public void removeAttachment(Attachment attachment)
		{
		for (int i = 0; i < attachments.size(); i++)
			{
			Attachment tmp = (Attachment) attachments.get(i);
			if (tmp.getAttachmentPk().equals((attachment.getAttachmentPk())))
				{
				attachments.remove(i);
				return;
				}
			}
		}

	public List getAttachmentsbyName(String attribute)
		{
		List result = new ArrayList();
		for (int i = 0; i < attachments.size(); i++)
			{
			Attachment tmp = (Attachment) attachments.get(i);
			if (tmp.getType().equalsIgnoreCase(attribute))
				{
				result.add(tmp);
				}
			}
		Collections.sort(result, new Attachment());
		return result;
		}

	public Attachment getAttachmentLatestAttachmentByName(String attribute)
		{
		Attachment result = null;
		for (int i = 0; i < attachments.size(); i++)
			{
			Attachment tmp = (Attachment) attachments.get(i);
			if (tmp.getType().equalsIgnoreCase(attribute))
				{
				if (result == null)
					{
					result = tmp;
					}
				else if (result.getAttachmentPk() < tmp.getAttachmentPk())
					{
					result = tmp;
					}
				}
			}
		return result;
		}

	public boolean hasAttachment()
		{
		return (attachments != null && !attachments.isEmpty());
		}

	public Attachment getAttachmentBy(String filename)
		{

		for (int i = 0; attachments != null && i < attachments.size(); i++)
			{
			Attachment attachment = (Attachment) attachments.get(i);
			if (attachment.getDisplayFilename().equals(filename))
				{
				return attachment;
				}
			}
		return null;
		}

	private static Map properties;
	public static final String[] commonAttributes = { "TIME", "STATE", "SUBSTATE", "OPERATOR_ID", "COMMENTS",
			"LOCATION", "WORKSTATION", "DURATION", "deviceGroup", "platform", "type" };
	static
		{
		Field[] fields = Machine.class.getDeclaredFields();
		properties = new HashMap(fields.length);
		for (int i = 0; i < fields.length; i++)
			{
			try
				{
				String name = fields[i].getName();
				PropertyDescriptor property = new PropertyDescriptor(name, Machine.class);
				properties.put(name, property);
				}
			catch (Exception ioe)
				{}
			}
		}

	public Object clone() throws CloneNotSupportedException
		{
		Machine clone = (Machine) super.clone();
		if (context != null)
			{
			clone.context = new HashMap();
			clone.context.putAll(context);
			DurableStatus ds = (DurableStatus) context.get("durableStatus");
			if (ds != null)
				{
				clone.context.put("durableStatus", ds.clone());
				}
			}
		if (summing != null)
			{
			clone.summing = new HashMap();
			clone.summing.putAll(summing);
			}
		if (performance != null)
			{
			clone.performance = new HashMap();
			clone.performance.putAll(performance);
			}
		if (dynamicAttributes != null)
			{
			clone.dynamicAttributes = new HashMap<String, String>();
			clone.dynamicAttributes.putAll(dynamicAttributes);
			}
		if (children != null)
			{
			clone.children = new HashMap<String, List<String>>();
			for(Entry<String, List<String>> entry:children.entrySet())
				{
				List<String> list = entry.getValue();
				List<String> newArray = new ArrayList<String>();
				newArray.addAll(list);
				clone.children.put(entry.getKey(), newArray);
				}
			}
		return clone;
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

	public boolean hasChild(String childName)
		{
		boolean found = false;
		if (children == null || children.isEmpty())
			{
			return found;
			}
		Set<String> keyset = children.keySet();
		for (Iterator<String> it = keyset.iterator(); it.hasNext() && !found;)
			{
			List<String> childrenByType = children.get(it.next());
			for (int i = 0; childrenByType != null && i < childrenByType.size() && !found; i++)
				{
				found = childName.equals(childrenByType.get(i));
				}
			}
		return found;

		}

	public Map<String, List<String>> getChildren()
		{
		return children;
		}

	/**
	 * @return a list of child attached to this machine that are of the
	 *         equipment type received as parameter
	 * */
	public List<String> getChildrenByEquipmentType(String equipmentType)
		{
		List<String> childrenFiltered = new ArrayList<String>();
		if (children == null)
			{
			return childrenFiltered;
			}
		List<String> tmp = children.get(equipmentType);
		if (tmp != null)
			{
			childrenFiltered.addAll(tmp);
			}
		return childrenFiltered;
		}
	
	/**
	 * if the input equipment type is null, it will return all children of all equipment types.
	 * @param equipmentType
	 * @return a list of child attached to this machine that are of the equipment type received as parameter
	 */
	public List<String> getChildrenByEquipmentTypeIgnoreCase(String equipmentType)
		{
		List<String> childrenFiltered = new ArrayList<String>();
		if (children == null)
			{
			return childrenFiltered;
			}
		for(Entry<String, List<String>> entry: children.entrySet())
			{
			String eqType = entry.getKey();
			if(eqType==null)
				{
				continue;
				}
			else if(equipmentType==null)
				{
				childrenFiltered.addAll(entry.getValue());
				}
			else if(eqType.toUpperCase().contains(equipmentType.toUpperCase()))
				{
				childrenFiltered.addAll(entry.getValue());
				}
			}
		return childrenFiltered;
		}

	public void setChildren(Map<String, List<String>> children)
		{
		this.children = children;
		}

	public EquipmentType getEquipmentType()
		{
		return equipmentType;
		}

	public void setEquipmentType(EquipmentType equipmentType)
		{
		this.equipmentType = equipmentType;
		if (equipmentType == null)
			{
			equipmentType_pk = null;
			}
		else
			{
			equipmentType_pk = equipmentType.primaryKey;
			}
		}

	public Machine()
		{}

	public Machine(Integer equipmentType_pk)
		{
		this.equipmentType_pk = equipmentType_pk;
		}

	public Machine(String eIName, String mESName, String type, String location)
		{
		this.eIName = eIName;
		this.mESName = mESName;
		this.type = type;
		this.location = location;
		}

	public String toString()
		{
		return eIName;
		}

	public Integer getEquipmentType_pk()
		{
		return equipmentType_pk;
		}

	public void setEquipmentType_pk(Integer equipmentTypePk)
		{
		equipmentType_pk = equipmentTypePk;
		}

	public String getPlatform()
		{
		return platform;
		}

	public void setPlatform(String platform)
		{
		this.platform = platform;
		}

	public String getEIName()
		{
		return eIName;
		}

	public void setEIName(String eIName)
		{
		this.eIName = eIName;
		}

	public String getType()
		{
		return type;
		}

	public void setType(String type)
		{
		this.type = type;
		}

	public String getState()
		{
		return state;
		}

	public void setState(String state)
		{
		this.state = state;
		}

	public Integer getPrimaryKey()
		{
		return primaryKey;
		}

	public void setPrimaryKey(Integer primaryKey)
		{
		this.primaryKey = primaryKey;
		}

	public String getMESName()
		{
		return mESName;
		}

	public void setMESName(String mESName)
		{
		this.mESName = mESName;
		}

	public String getLocation()
		{
		return location;
		}

	public void setLocation(String location)
		{
		this.location = location;
		}

	public Integer getArea_pk()
		{
		return area_pk;
		}

	public void setArea_pk(Integer areaPk)
		{
		area_pk = areaPk;
		}

	public String getSerial_number()
		{
		return serial_number;
		}

	public void setSerial_number(String serialNumber)
		{
		serial_number = serialNumber;
		}

	public String getEngineer_contact()
		{
		return engineer_contact;
		}

	public void setEngineer_contact(String engineerContact)
		{
		engineer_contact = engineerContact;
		}

	public String getVendor()
		{
		return vendor;
		}

	public void setVendor(String vendor)
		{
		this.vendor = vendor;
		}

	public String getWorkstation()
		{
		return workstation;
		}

	public void setWorkstation(String workstation)
		{
		this.workstation = workstation;
		}

	public Date getStateStart()
		{
		return stateStart;
		}

	public void setStateStart(Date stateStart)
		{
		this.stateStart = stateStart;
		}

	public Date getEventStart()
		{
		return eventStart;
		}

	public void setEventStart(Date eventStart)
		{
		this.eventStart = eventStart;
		}

	public Map getContext()
		{
		return context;
		}

	public void setContext(Map context)
		{
		this.context = context;
		}

	public Map getSumming()
		{
		return summing;
		}

	public void setSumming(Map summing)
		{
		this.summing = summing;
		}

	public Map getPerformance()
		{
		return performance;
		}

	public void setPerformance(Map performance)
		{
		this.performance = performance;
		}

	public float getProductiveTime()
		{
		return productiveTime;
		}

	public void setProductiveTime(float productiveTime)
		{
		this.productiveTime = productiveTime;
		}

	public boolean isMesEvent()
		{
		return mesEvent;
		}

	public void setMesEvent(boolean mesEvent)
		{
		this.mesEvent = mesEvent;
		}

	public String getPic_Host()
		{
		return pic_Host;
		}

	public void setPic_Host(String picHost)
		{
		pic_Host = picHost;
		}

	public String getPic_Port()
		{
		return pic_Port;
		}

	public void setPic_Port(String picPort)
		{
		pic_Port = picPort;
		}

	public String getDmhbox()
		{
		return dmhbox;
		}

	public void setDmhbox(String dmhbox)
		{
		this.dmhbox = dmhbox;
		}

	public String getDeviceGroup()
		{
		return deviceGroup;
		}

	public void setDeviceGroup(String devGroup)
		{
		deviceGroup = devGroup;
		}

	public Map<String, String> getDynamicAttributes()
		{
		return dynamicAttributes;
		}

	public void setDynamicAttributes(Map<String, String> dynamicAttributes)
		{
		this.dynamicAttributes = dynamicAttributes;
		}

	public boolean has(String name)
		{
		name = getRealname(name);
		boolean has = false;
		has = hasProperty(name) || hasDynamicAttribute(name) || hasStatusAttribute(name)
				|| hasContextAttribute(name);
		return has;
		}

	private boolean hasProperty(String name)
		{
		return properties.containsKey(name);
		}

	private boolean hasDynamicAttribute(String name)
		{
		boolean has = false;
		if (isEquipmentTypeDefined())
			{
			has = equipmentType.getAttribute(name) != null;
			}
		return has;
		}

	private boolean hasContextAttribute(String name)
		{
		boolean has = false;
		if (context != null) has = context.containsKey(name);
		return has;
		}

	private boolean hasStatusAttribute(String name)
		{
		boolean has = false;
		DurableStatus durableStatus = null;
		if (context != null && (durableStatus = (DurableStatus) context.get("durableStatus")) != null)
			{
			has = durableStatus.has(name);
			}
		return has;
		}

	private boolean isEquipmentTypeDefined()
		{
		return equipmentType != null;
		}

	public Object get(String field)
		{
		Object value = null;
		field = getRealname(field);
		if (hasProperty(field))
			{
			value = getPropertyValue(field);
			}
		else if (hasDynamicAttribute(field))
			{
			value = getDynamicAttribute(field);
			}
		else if (hasStatusAttribute(field))
			{
			value = getStatusAttribute(field);
			}
		else
			{
			value = context.get(field);
			}
		return value;
		}

	private Object getPropertyValue(String propertyName)
		{
		Object value = null;
		try
			{
			PropertyDescriptor property = (PropertyDescriptor) properties.get(propertyName);
			Method get = property.getReadMethod();
			Object[] args = null;
			value = get.invoke(this, args);
			}
		catch (Exception ioe)
			{}

		return value;
		}

	private Object getDynamicAttribute(String name)
		{
		Object value = null;
		EquipmentAttribute attribute = equipmentType.getAttribute(name);
		if (attribute.isEquipmentAttribute())
			{
			value = dynamicAttributes.get(name);
			}
		else if (attribute.isStatusAttribute())
			{
			DurableStatus durableStatus = (DurableStatus) context.get("durableStatus");
			value = durableStatus.get(name);
			}
		return value;
		}

	public String getRealname(String name)
		{
		EquipmentType eType = getEquipmentType();
		if (eType == null) return name;
		EquipmentAttribute attribute = eType.getAttributebyAlia(name);
		if (attribute == null) return name;
		return attribute.getName();
		}

	private Object getStatusAttribute(String name)
		{
		DurableStatus durableStatus = (DurableStatus) context.get("durableStatus");
		return durableStatus.get(name);
		}

	public void set(String name, Object value)
		{
		name = getRealname(name);
		if (hasProperty(name))
			{
			setPropertyValue(name, value);
			}
		else if (hasDynamicAttribute(name))
			{
			setDynamicAttribute(name, value);
			}
		else if (hasStatusAttribute(name))
			{
			setStatusAttribute(name, value);
			}
		else
			{
			context.put(name, value);
			}
		}

	private void setPropertyValue(String name, Object value)
		{
		try
			{
			PropertyDescriptor property = (PropertyDescriptor) properties.get(name);
			Method set = property.getWriteMethod();
			Object[] args = new Object[] { value };
			set.invoke(this, args);
			}
		catch (Exception e)
			{}
		}

	private void setDynamicAttribute(String name, Object value)
		{
		EquipmentAttribute attribute = equipmentType.getAttribute(name);
		Map<String, String> attributes;
		if (attribute.isEquipmentAttribute())
			{
			if (dynamicAttributes == null)
				{
				dynamicAttributes = new HashMap<String, String>();
				}
			dynamicAttributes.put(name, value.toString());
			}
		else if (attribute.isStatusAttribute())
			{
			DurableStatus dStatus = (DurableStatus) context.get("durableStatus");
			if (dStatus == null)
				{
				dStatus = new DurableStatus();
				dStatus.ATTRIBUTES = new HashMap<String, String>();
				context.put("durableStatus", dStatus);
				}
			attributes = dStatus.getDynamicAttributes();
			if (attributes == null)
				{
				dStatus.ATTRIBUTES = new HashMap<String, String>();
				attributes = dStatus.getDynamicAttributes();
				}
			attributes.put(name, String.valueOf(value));
			}
		}

	public boolean isAttached()
		{
		return workstation != null && !workstation.equals("");
		}

	private void setStatusAttribute(String name, Object value)
		{
		DurableStatus durableStatus = (DurableStatus) context.get("durableStatus");
		durableStatus.set(name, value);
		}

	// to maintain the order of message going through this machine, we make it
	// synchronized;
	public synchronized void sendStatus(IStateTransition stateTransition, IEquipmentEvent equipmentEvent, String newState, String oldState, String oldSubState, String newSubState) throws Throwable
		{
		stateTransition.sendEvent(equipmentEvent, newState, oldState, oldSubState, newSubState);
		}

	// to maintain the order of message going through this machine, we make it
	// synchronized;
	public synchronized void executeEPRrules(IStateTransition stateTransition, String latestEvent) throws Throwable
		{
		stateTransition.executeBusinessRules(this, latestEvent);
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

	public Status getCurrentStatus()
		{
			return currentStatus;
		}

	public void setCurrentStatus(Status currentStatus)
		{
			this.currentStatus = currentStatus;
		}

	public String getModelName()
		{
		return modelName;
		}

	public void setModelName(String modelName)
		{
		this.modelName = modelName;
		}

	public Model getModel()
		{
		return model;
		}

	public void setModel(Model model)
		{
		this.model = model;
		}

	public String getChildModel()
		{
		return childModel;
		}

	public void setChildModel(String childModel)
		{
		this.childModel = childModel;
		}

	public Integer getModelPk()
		{
		return modelPk;
		}

	public void setModelPk(Integer modelPk)
		{
		this.modelPk = modelPk;
		}

	public String getModelDescription()
		{
		if(model!=null&&modelDescription==null)
			{
			modelDescription = model.getModelDescription();
			}
		return modelDescription;
		}

	public void setModelDescription(String modelDescription)
		{
		this.modelDescription = modelDescription;
		}

	}
