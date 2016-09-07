package com.freescale.stateStamper.model.entity;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ThresholdCounter implements ComplexAttribute
	{
	private String name;
	private String eIVariable;
	private String units;
	//services
	private long serviceUsageLimit;
	private long serviceUsage;
	private long totalServicesLimit;
	private long totalServices;
	//rebuild
	private long rebuildUsageLimit;
	private long rebuildUsage;
	private long totalRebuildsLimit;
	private long totalRebuilds;
	//lifetime
	private long lifeLimit;
	private long lifeUsage;
	
	//flag to use in time based attribute
	
	private boolean countTimeElapse;
	public boolean isCountTimeElapse()
		{
		return countTimeElapse;
		}


	public void setCountTimeElapse(boolean countTimeElapse)
		{
		this.countTimeElapse = countTimeElapse;
		}
	private Map values;
	private Map attributes;
	
	
	public ThresholdCounter(String name)
		{
		setName(name);
		}
	
	
	public String getName()
		{
		return name;
		}
	public void setName(String name)
		{
		this.name = name;
		init();
		}
	public String getEIVariable()
		{
		return eIVariable;
		}
	public void setEIVariable(String eiVariable)
		{
		this.eIVariable = eiVariable;
		}
	public String getUnits()
		{
		return units;
		}
	public void setUnits(String units)
		{
		this.units = units;
		}
	public long getServiceUsageLimit()
		{
		return serviceUsageLimit;
		}
	public void setServiceUsageLimit(long serviceUsageLimit)
		{
		this.serviceUsageLimit = serviceUsageLimit;
		}
	public long getServiceUsage()
		{
		return serviceUsage;
		}
	public void setServiceUsage(long serviceUsage)
		{
		this.serviceUsage = serviceUsage;
		}
	public long getTotalServicesLimit()
		{
		return totalServicesLimit;
		}
	public void setTotalServicesLimit(long totalServiceLimit)
		{
		this.totalServicesLimit = totalServiceLimit;
		}
	public long getTotalServices()
		{
		return totalServices;
		}
	public void setTotalServices(long totalService)
		{
		this.totalServices = totalService;
		}
	public long getRebuildUsageLimit()
		{
		return rebuildUsageLimit;
		}
	public void setRebuildUsageLimit(long rebuildUsageLimit)
		{
		this.rebuildUsageLimit = rebuildUsageLimit;
		}
	public long getRebuildUsage()
		{
		return rebuildUsage;
		}
	public void setRebuildUsage(long rebuildUsage)
		{
		this.rebuildUsage = rebuildUsage;
		}
	public long getTotalRebuildsLimit()
		{
		return totalRebuildsLimit;
		}
	public void setTotalRebuildsLimit(long totalRebuildLimit)
		{
		this.totalRebuildsLimit = totalRebuildLimit;
		}
	public long getTotalRebuilds()
		{
		return totalRebuilds;
		}
	public void setTotalRebuilds(long totalRebuilds)
		{
		this.totalRebuilds = totalRebuilds;
		}
	public long getLifeLimit()
		{
		return lifeLimit;
		}
	public void setLifeLimit(long lifeLimit)
		{
		this.lifeLimit = lifeLimit;
		}
	public long getLifeUsage()
		{
		return lifeUsage;
		}
	public void setLifeUsage(long lifeUsage)
		{
		this.lifeUsage = lifeUsage;
		}
	
	public long incrementServiceUsage(long increment)
		{
		serviceUsage+=increment;
		incrementRebuildUsage(increment);
		incrementLifeUsage(increment);
		return serviceUsage;
		}
	
	private long incrementRebuildUsage(long inc)
		{
		rebuildUsage+=inc;
		return rebuildUsage;
		}
	
	private long incrementLifeUsage(long increment)
		{
		lifeUsage+=increment;
		return lifeUsage;
		}
	
	public boolean isServiceUsageLimitReached()
		{
		return getServiceUsage() >= getServiceUsageLimit();
		}
	
	public boolean isTotalServiceLimitReached()
		{
		return getTotalServices() >= getTotalServicesLimit();
		}

	public boolean isRebuildUsageLimitReached()
		{
		return getRebuildUsage() >= getRebuildUsageLimit();
		}
	
	public boolean isTotalRebuildLimitReached()
		{
		return getTotalRebuilds() >= getTotalRebuildsLimit();
		}
	
	public boolean isLifeLimitReached()
		{
		return getTotalRebuilds() >= getTotalRebuildsLimit();
		}

	public long resetServiceUsage()
		{
		return serviceUsage=0;
		}
	
	public long resetRebuildUsage()
		{
		rebuildUsage=0;
		resetServiceUsage();
		return rebuildUsage;
		}
	
	public Map getAsMap()
		{
		init();
		return values;
		}
	
	public Map getAttributesMap()
		{
		if(attributes==null){init();}
		return attributes;
		}
	
	public Map getAttributesBy(String belongs)
		{
		Map eqAttr= new HashMap();
		List lAttr = new ArrayList(attributes.values());
		EquipmentAttribute attribute;
		for(int i=0;lAttr!=null&&i<lAttr.size();i++)
			{
			attribute=(EquipmentAttribute)lAttr.get(i);
			if(belongs.equals( attribute.belongs ) )
				{eqAttr.put(attribute.name, attribute);}
			}
		return eqAttr;
		}
	
	
	
	public void init()
		{
		try{
		values = new HashMap();
		attributes=new HashMap();
		String name = getName();
		String className = getClass().getSimpleName();
		values.put(name,className);
		EquipmentAttribute attribute= new EquipmentAttribute();
		attribute.name = name;
		attribute.type = EquipmentAttribute.TEXT;
		attribute.belongs=EquipmentAttribute.EQUIPMENT;
		attributes.put(attribute.name, attribute);    
		name+=SEPARATOR;  
		Field fields[]=getClass().getDeclaredFields();
		String fieldName;
		Object value;
		for(int i=0;fields!=null&&i<fields.length;i++)
			{
			fieldName=fields[i].getName();
			if("name".equals( fieldName) || "values".equals(fieldName) ||
			   "attributes".equals(fieldName) ){continue;}
			value = fields[i].get(this);
			values.put(name+fieldName, value);
			attribute = new EquipmentAttribute();
			attribute.name = name+fieldName;
			Class fieldClass=fields[i].getType();
			if(fieldClass == long.class ){attribute.type= EquipmentAttribute.COUNTER ;}
			else{attribute.type= EquipmentAttribute.TEXT ;}
			if(attribute.name.endsWith("Limit") || attribute.name.endsWith("eIVariable") ||
			   attribute.name.endsWith("units")	){attribute.belongs=EquipmentAttribute.EQUIPMENT;}
			else {attribute.belongs=EquipmentAttribute.STATUS;}
			attribute.required="N";
			attributes.put(attribute.name,attribute);
			}
		}catch(Exception e)
			{
			e.printStackTrace();
			}
		}
	
	public boolean setAsMap(Map otherMap)
		{
		if(otherMap==null)return false;
		if(!otherMap.containsKey(getName() ))return false;
		if(!getClass().getSimpleName().equals(  otherMap.get(getName())))return false;
		boolean result=false;
		Map map = getAsMap();
		Set attribs =  map.entrySet();
		Iterator it = attribs.iterator();
		otherMap.remove(getName());
		while(it.hasNext())
			{
			Map.Entry entry = (Map.Entry)it.next();
			Object value=null;
			if(otherMap.containsKey(entry.getKey()))
				{
				String propName = entry.getKey().toString();
				propName = propName.substring(propName.lastIndexOf(SEPARATOR )+1);
				try
					{
					PropertyDescriptor prop = new PropertyDescriptor(propName,getClass());
					Method writer=prop.getWriteMethod();
					value=otherMap.get(entry.getKey());
					if("Counter".equals(attributes.get(entry.getKey()))){value = Long.parseLong(value.toString());}
					writer.invoke(this,value);
					result=true;
				}
				catch(Exception e){e.printStackTrace();}
				}
			}
		return result;
		}
	/**
	 * loads the counter's values from a durable status that match with the name 
	 * of this ThresholdCounter 
	 * for instance a durableStatus with a entry in the dynamic attributes map of 
	 * TOUCH$serviceUsage=100 will be loaded if the name of this counter is TOUCH   
	 * @param status a durable status to load
	 * @return the updated ThresholdCounter
	 * @see unloadToStatus
	 * */
	public ThresholdCounter loadFromStatus(DurableStatus status)
		{
		Map attrs=status.getDynamicAttributes();
		if(attrs==null)return this;
		Set set = attrs.entrySet();
		Iterator it = set.iterator(); 
		while(it.hasNext() )
			{
			Map.Entry att= (Map.Entry)it.next();
			String attName = (String)att.getKey();
			if(attributes.containsKey(attName)&&!attName.equals(name))
				{		
				attName = attName.substring(name.length()+1);
				PropertyDescriptor desc=null;
				try	{
				desc = new PropertyDescriptor(attName,getClass());
				Object[] param = new Object[1];
				param[0]=att.getValue();
				if( desc.getPropertyType() == long.class){param[0] = getNumericValue(param[0].toString());}
				if( desc.getPropertyType() == boolean.class){param[0] = Boolean.valueOf(param[0].toString());}
				desc.getWriteMethod().invoke(this,param);
				}
				catch (Throwable t){t.printStackTrace();}
				}
			}
		return this;
		}
	
	private Long getNumericValue(String val)
		{
		Long value=Long.valueOf(0);
		try{value=Long.valueOf(val);}
		catch(NumberFormatException e){}
		return value;
		}
	/**
	 * updates the status passed as parameter with the values from the
	 * ThresholdCounter, <b> it only updates the status when the attributes are already in
	 * the status</b> 
	 * @param the status to unload the values 
	 * @return the updated status
	 * @see unloadToStatus(DurableStatus,boolean)  
	 * */
	public DurableStatus unloadToStatus(DurableStatus status)
		{
		return unloadToStatus(status, false);
		}
	/**
	 * updates the status passed as parameter with the values from the
	 * ThresholdCounter, if the status passed do not have the <b> it only updates the status when the attributes are already in
	 * the status</b> 
	 * @param the status to unload the values 
	 * @return the updated status
	 * @see #unloadToStatus(com.freescale.stateStamper.model.entity.DurableStatus)  
	 * */
	public DurableStatus unloadToStatus(DurableStatus status,boolean insert)
		{
		if(status==null)return status;
		init();
		Set sValues = values.entrySet();
		Iterator it =sValues.iterator();
		Map dynAttrs = status.getDynamicAttributes();
		while(it.hasNext())
			{
			Map.Entry attr = (Map.Entry)it.next();
			boolean has=dynAttrs.containsKey(attr.getKey());
			if( (!has && !insert) || attr.getValue()==null){continue;}//not found and do not insert then continue
			status.set(attr.getKey().toString(),attr.getValue().toString() );
			}
		return status;
		}
	public static boolean isServicesAttributes(String attribute){
		 return (attribute.equalsIgnoreCase("serviceUsageLimit")||
				 attribute.equalsIgnoreCase("serviceUsage")||
				 attribute.equalsIgnoreCase("totalServicesLimit")||
				 attribute.equalsIgnoreCase("totalServices"));
	}
	
	public static boolean isRebuildAttributes(String attribute){
	 	return (attribute.equalsIgnoreCase("rebuildUsageLimit")||
	 			attribute.equalsIgnoreCase("rebuildUsage")||
	 			attribute.equalsIgnoreCase("totalRebuildsLimit")||
	 			attribute.equalsIgnoreCase("totalRebuilds"));
	}
	
	public static boolean isLifeAttributes(String attribute){
		return (attribute.equalsIgnoreCase("lifeLimit")||
				attribute.equalsIgnoreCase("lifeUsage"));
	}
	
	public static boolean isLimitAttributes(String attribute){
	 	return (attribute.endsWith("rebuildUsageLimit")||
	 			attribute.endsWith("totalServicesLimit")||
	 			attribute.endsWith("totalRebuildsLimit")||
	 			attribute.endsWith("serviceUsageLimit")||
	 			attribute.endsWith("lifeLimit"));
	}
	
	}
