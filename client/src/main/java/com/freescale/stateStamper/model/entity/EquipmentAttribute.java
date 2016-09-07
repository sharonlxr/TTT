package com.freescale.stateStamper.model.entity;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

public class EquipmentAttribute implements Serializable
	{

	private static final long serialVersionUID = 1L;
	public Integer primaryKey;
	public String name;
	public String type;
	public String valRule;
	public String required;
	public String isstatic;// static
	public Integer typeKey;
	public String belongs;
	private String attributeAlias;
	private Map<String, String> configOptions;
	// types of attribute
	public static final String COUNTER = "Counter";
	public static final String TEXT = "Text";
	public static final String THRESHOLDCOUNTER = "ThresholdCounter";
	public static final String ATTACHMENT = "Attachment";
	// values for belongs
	public static final String EQUIPMENT = "EQUIPMENT";
	public static final String STATUS = "STATUS";
	public static final String SEPARATOR = "$";

	public EquipmentAttribute()
		{}

	public EquipmentAttribute(EquipmentAttribute copy)
		{
		primaryKey = copy.primaryKey;
		name = copy.name;
		type = copy.type;
		valRule = copy.valRule;
		required = copy.required;
		isstatic = copy.isstatic;
		typeKey = copy.typeKey;
		belongs = copy.belongs;
		attributeAlias = copy.attributeAlias;
		configOptions = copy.configOptions;
		}

	public String toString()
		{
		return name + ":" + type;
		}

	public String getConfiguration()
		{
		StringBuffer config = new StringBuffer("");
		if (configOptions != null)
			{
			config.append(configOptions.toString());
			config.deleteCharAt(0);
			config.deleteCharAt(config.length() - 1);
			}
		return config.toString();
		}

	public void setConfiguration(String config)
		{
		configOptions = new HashMap<String, String>();
		if (StringUtils.isNotEmpty(config)) 
			{
			String[] elements = config.split(",");
			for (int i = 0; elements != null && i < elements.length; i++)
				{
				String[] tmp = elements[i].split("=");
				if (tmp != null && tmp.length == 2)
					{
					configOptions.put(tmp[0].trim(), tmp[1].trim());
					}
				}
			}
		}

	public Map<String, String> getConfigOptions()
		{
		return configOptions;
		}

	/**
	 * 
	 * */
	public void setConfigOptions(Map<String, String> config)
		{
		configOptions = config;
		}

	public Integer getPrimaryKey()
		{
		return primaryKey;
		}

	public void setPrimaryKey(Integer primaryKey)
		{
		this.primaryKey = primaryKey;
		}

	public String getName()
		{
		return name;
		}

	public void setName(String name)
		{
		this.name = name;
		}

	public String getType()
		{
		return type;
		}

	public void setType(String type)
		{
		this.type = type;
		}

	public String getValRule()
		{
		return valRule;
		}

	public void setValRule(String valRule)
		{
		this.valRule = valRule;
		}

	public String getRequired()
		{
		return required;
		}

	public void setRequired(String required)
		{
		this.required = required;
		}

	public Integer getTypeKey()
		{
		return typeKey;
		}

	public void setTypeKey(Integer typeKey)
		{
		this.typeKey = typeKey;
		}

	public String getBelongs()
		{
		return belongs;
		}

	public void setBelongs(String belongs)
		{
		this.belongs = belongs;
		}

	public boolean isEquipmentAttribute()
		{
		return EquipmentAttribute.EQUIPMENT.equals(belongs);
		}

	public boolean isStatusAttribute()
		{
		return EquipmentAttribute.STATUS.equals(belongs);
		}

	public boolean isCounter()
		{
		return EquipmentAttribute.COUNTER.equals(type);
		}

	public boolean isText()
		{
		return EquipmentAttribute.TEXT.equals(type);
		};

	public boolean isTimeBasedAttribute()
		{
		Map<String, String> configurations = this.getConfigOptions();
		if (configurations != null)
			{
			String units = configurations.get("units");
			if (units != null && units.equalsIgnoreCase("time"))
				{
				return true;
				}
			}
		return false;
		}

	public boolean isThresholdAttribute()
		{
		return name.contains("$") || getType().equalsIgnoreCase("ThresholdCounter");
		}

	public boolean isAttachment()
		{
		return getType().equalsIgnoreCase(this.ATTACHMENT);
		}

	public String getAttributeAlias()
		{
		return attributeAlias;
		}

	public void setAttributeAlias(String alias)
		{
		attributeAlias = alias;
		}

	}
