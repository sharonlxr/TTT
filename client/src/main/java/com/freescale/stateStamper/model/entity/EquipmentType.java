package com.freescale.stateStamper.model.entity;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import com.freescale.stateStamper.util.Map2StringUtil;

public class EquipmentType implements Serializable{

	private static final long serialVersionUID =  -9102496372353250946L;;
	public Integer primaryKey;
	public String name;
	public String container;
	public String propStatus;
	public String parent;
	private Vector attributes;
	private Integer areaPK;
	private HashMap mapAliasAttributes;
	private Area area;
	public String mesEquipmentType;
	private String mesEquipmentSync;
    public String syncModelFlag;
	private String platformSource;
	private String modelSource;
	private String childSource;
	private String picHost,picPort;
	private String picName;
	private Map<String, String> configOptions;
	
	public HashMap getMapAliasAttributes()
		{
		return mapAliasAttributes;
		}

	public void setMapAliasAttributes(HashMap mapAliasAttributes)
		{
		this.mapAliasAttributes = mapAliasAttributes;
		}

	public String getPropStatus() {
		return propStatus;
	}

	public void setPropStatus(String propStatus) {
		if (propStatus!=null&&(propStatus.equalsIgnoreCase("Y")||propStatus.equalsIgnoreCase("N")))
			this.propStatus = propStatus;
	}
    
	public boolean propagateStatus()
		{
		return propStatus !=null && propStatus.equalsIgnoreCase("Y");
		}
	
	private Vector<BusinessRule> businessRules;
	private HashMap mapAttributes;
	private String siblings;

	
	public String getSiblings() {
		return siblings;
	}

	public void setSiblings(String siblings) {
		if (siblings!=null&&(siblings.equalsIgnoreCase("Y")||siblings.equalsIgnoreCase("N")))
			this.siblings = siblings;
	}

	public EquipmentType(){}
	
	public EquipmentType(Integer primaryKey)
		{
		this.primaryKey = primaryKey;
		}
	
	public Vector getBusinessRules()
		{
		return businessRules;
		}

	public void setBusinessRules(Vector arg)
		{
		businessRules = arg;
		}

	public Vector getAttributes()
		{
		return attributes;
		}
	public Vector getFilteredAttributes()
		{
		Vector filtered = new Vector();
		for(int i=0;attributes!=null&& i<attributes.size();i++)
			{
			if(((EquipmentAttribute)attributes.get(i)).name.indexOf(EquipmentAttribute.SEPARATOR)<0)
				filtered.add(attributes.get(i));
			}
		return filtered; 
		}
	/**
	 * @param parentAttribute the name of a complex attribute
	 * @returns a vector with the sub-attributes of the complex attribute name
	 * */
	public Vector getSubAttributes(String parentAttribute)
		{
		Vector subs = new Vector();
		boolean subAttribute = false;
		EquipmentAttribute tmp =null;
		for(int i=0;attributes != null && i < attributes.size();i++)
			{
			tmp = (EquipmentAttribute)attributes.get(i);
			subAttribute = tmp.getName().indexOf(parentAttribute+EquipmentAttribute.SEPARATOR) == 0;
			if(subAttribute){subs.add(tmp);}
			}
		return subs;
		}

	public void setAttributes(Vector arg)
		{
		attributes=arg;
		}
	
	public EquipmentAttribute getAttribute(String name)
		{
		if(attributes ==null)return null;
		EquipmentAttribute attribute= null;
		//set up attributes
		if(mapAttributes==null){initMapAttributes();}
		if(mapAttributes.containsKey(name)){attribute=(EquipmentAttribute)mapAttributes.get(name);}
		return attribute;
		}
	
	public String getAttributeAlias(String name)
		{
		String alias=null;
		if(hasAttribute(name))
			{
			EquipmentAttribute attribute = getAttribute(name);
			alias = attribute.getAttributeAlias();
			if(alias == null || alias.equals("")){alias = name;}
			}
		return alias;
		}
	
	public boolean hasAttribute(String name)
		{
		if(attributes ==null)return false;
		//set up attributes
		if(mapAttributes==null){initMapAttributes();}
		return mapAttributes.containsKey(name);
		}
	
	public EquipmentAttribute getAttributebyAlia(String name)
		{
		if (attributes == null) return null;
		EquipmentAttribute attribute = null;
		// set up attributes
		if (mapAliasAttributes == null)
			{
			initMapAliasAttributes();
			}
		if (mapAliasAttributes.containsKey(name))
			{
			attribute = (EquipmentAttribute) mapAliasAttributes.get(name);
			}
		return attribute;
		}
	
	public Map getAttributesMap()
		{
		Map atts = new HashMap();
		for(int i=0;attributes !=null && i< attributes.size();i++)
			{
			EquipmentAttribute tmp = (EquipmentAttribute)attributes.get(i);
			atts.put(tmp.primaryKey,tmp);
			}
		return atts;
		}
	
	private Map initMapAttributes()
		{
		mapAttributes = new HashMap();
		EquipmentAttribute attribute= null;
		for (Object obj: attributes)
			{
			attribute = (EquipmentAttribute)obj;
			mapAttributes.put(attribute.name,attribute);
			}
		return mapAttributes;
		}
	
	private Map initMapAliasAttributes()
		{
		mapAliasAttributes = new HashMap();
		EquipmentAttribute attribute = null;
		for (Object obj : attributes)
			{
			attribute = (EquipmentAttribute) obj;
			if (attribute.getAttributeAlias() != null
					&& !attribute.getAttributeAlias().trim().equalsIgnoreCase(""))
				{
				mapAliasAttributes.put(attribute.getAttributeAlias(), attribute);
				}
			}
		return mapAliasAttributes;
		}

	public Vector getAttributesNames()
		{
		if(mapAttributes==null){initMapAttributes();}
		Vector result=new Vector(mapAttributes.keySet());
		return result;
		}

	/**
	 * @return the attributes that belong to the equipment 
	 * */
	public Vector getEquipmentAttributes()
		{
		return getAttributesBy(EquipmentAttribute.EQUIPMENT) ;
		}
	
	/**
	 * @return the attributes that belong to the status 
	 * */
	public Vector getStatusAttributes()
		{
		return getAttributesBy(EquipmentAttribute.STATUS) ;
		}
	
	private Vector getAttributesBy(String belongsTo)
		{
		Vector results = new Vector();
		EquipmentAttribute attribute;
		for (int i=0;attributes!=null&&i<attributes.size();i++)
			{
			attribute = (EquipmentAttribute)attributes.get(i);
			if(belongsTo.equals(attribute.belongs)){results.add(attribute);}
			}
		return results;
		}
	
	public Boolean hasAttachmentAttribute(){
		if (getAttributesByType(EquipmentAttribute.ATTACHMENT).isEmpty()){
			return false;
		}
		else {
			return true;
		}
	}
	
	/**
	 * @return a list of all the attributes that match the given type
	 * */
	public Vector getAttributesByType(String type)
		{
		Vector results = new Vector();
		EquipmentAttribute attribute;
		for (int i=0;attributes!=null&&i<attributes.size();i++)
			{
			attribute = (EquipmentAttribute)attributes.get(i);
			if(type.equals(attribute.type)){results.add(attribute);}
			}
		return results;
		}
	
	
	public Vector getBusinessRulesBy(String event)
		{
		Vector result= new Vector();
		for(int i=0;businessRules !=null && i<businessRules.size();i++)
			{
			if(businessRules.get(i).getEventName().equalsIgnoreCase(event))
				result.add(businessRules.get(i));
			}
		return result;
		}
	
	public BusinessRule getBusinessRuleByName(String name)
		{
		BusinessRule rule = null;
		for(int i=0;businessRules !=null && i<businessRules.size();i++)
			{
			if(businessRules.get(i).getName().equalsIgnoreCase(name)){rule = businessRules.get(i);break;}
			}
		return rule;
		}
	
	public String toString()
		{
		String s="";
		s+=name +":" +container+":"+propStatus+":"+parent;
		return s;
		}
	
	   
	public boolean isAttachmentAttribute(String attribute) {
		EquipmentAttribute att = getAttribute(attribute);
		return EquipmentAttribute.ATTACHMENT.equalsIgnoreCase(att.type);
	}
	
	public boolean isTimeRelatedAttribute(String attribute)
		{
		 if (attribute.contains(ThresholdCounter.SEPARATOR)){
		 	String[] temp = attribute.split("\\" + ThresholdCounter.SEPARATOR);
		 	String tempStr = temp[1];
		 	if (ThresholdCounter.isLifeAttributes(tempStr)||
		 		ThresholdCounter.isRebuildAttributes(tempStr)||
		 		ThresholdCounter.isServicesAttributes(tempStr)){
		 		EquipmentAttribute equipmentAttribute = getAttribute(temp[0]);
			 	return equipmentAttribute.isTimeBasedAttribute();
		 		}
		 	else {
		 		return false;
		 	}
		 }
		 else {
		 	return false;
		 }
		}
	
	public boolean isContainer()
		{
		return "Y".equalsIgnoreCase(container); 
		}

	public Integer getAreaPK()
		{
		return areaPK;
		}

	public void setAreaPK(Integer areaPK)
		{
		this.areaPK = areaPK;
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
	public BusinessRule getBusinessRuleByEvent(String event)
		{
		BusinessRule businessRule = null;
		if(businessRules==null)
			{
			return businessRule;
			}
		for(BusinessRule br:businessRules)
			{
			if(br.getEventName().equalsIgnoreCase(event))
				{
				businessRule = br;
				break;
				}
			}
		return businessRule;
	}

	public String getPlatformSource()
		{
		return platformSource;
		}

	public void setPlatformSource(String platformSource)
		{
		this.platformSource = platformSource;
		}

	public String getModelSource()
		{
		return modelSource;
		}

	public void setModelSource(String modelSource)
		{
		this.modelSource = modelSource;
		}

	public String getChildSource()
		{
		return childSource;
		}

	public void setChildSource(String childSource)
		{
		this.childSource = childSource;
		}

	public String getMesEquipmentType()
		{
		return mesEquipmentType;
		}

	public void setMesEquipmentType(String mesEquipmentType)
		{
		this.mesEquipmentType = mesEquipmentType;
		}

	public String getMesEquipmentSync()
		{
		return mesEquipmentSync;
		}

	public void setMesEquipmentSync(String mesEquipmentSync)
		{
		this.mesEquipmentSync = mesEquipmentSync;
		}
	
	public boolean isMesEquipmentSyncEnabled()
		{
		return mesEquipmentSync != null && mesEquipmentSync.equalsIgnoreCase("Y");
		}

	public String getPicHost()
		{
		return picHost;
		}

	public void setPicHost(String picHost)
		{
		this.picHost = picHost;
		}

	public String getPicPort()
		{
		return picPort;
		}

	public void setPicPort(String picPort)
		{
		this.picPort = picPort;
		}

	public Map<String, String> getConfigOptions()
		{
		return configOptions;
		}

	public void setConfigOptions(Map<String, String> configOptions)
		{
		this.configOptions = configOptions;
		}

	public String getConfigOptionsStr()
		{
		return Map2StringUtil.map2Str(configOptions);
		}
	
	public void setConfigOptionsStr(String configOptionsStr)
		{
		this.configOptions = Map2StringUtil.str2Map(configOptionsStr);
		}

	public String getPicName()
		{
		return picName;
		}

	public void setPicName(String picName)
		{
		this.picName = picName;
		}
	
}
