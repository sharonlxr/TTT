package com.freescale.stateStamper.model.entity;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

public class Model implements Comparable<Model>,Serializable
	{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Integer primaryKey;
	private String modelName;
	private String modelDescription;
	private Integer equipmentTypePk;
	private Integer areaPk;
	private EquipmentType equipmentType;
	private Area area;
	private String engineerContact;
	private String vendor;
	private String platform;
	private String status;
	private Map<String, String> modelAttributes;
	private Map<String, String> dynamicAttributes;
	private String creator;
	private String createDate;
	private String approver;
	private String approveDate;
	private String releaser;
	private String releaseDate;
	private String lastUpdateBy;
	private String lastUpdateDate;
	private String mfrCode;
	private Map<String, String> modelMapping;
	protected static transient Map<String,Field> fields = new HashMap<String,Field>();
	
	static{
		Field[] f = Model.class.getDeclaredFields();
		for (int i = 0; i < f.length; i++)
			{
			Field field = f[i];
			String modifier = Modifier.toString(field.getModifiers());
			if(field.getType()!=Map.class&&!"public static final".equals(modifier))
				{
				fields.put(f[i].getName(), f[i]);
				}
			}
	}
	
	public Object get(String name)
		{
		Object value = null;
		if (fields.containsKey(name))
			{
			Field f = (Field) fields.get(name);
			try
				{
				value = f.get(this);
				}
			catch (Exception e)
				{
				e.printStackTrace();
				}
			}
		else if (modelAttributes.containsKey(name))
			{
			value = modelAttributes.get(name);
			}
		else if (dynamicAttributes.containsKey(name))
			{
			value = dynamicAttributes.get(name);
			}
		return value;
		}

	public Object set(String name, Object value)
		{
		if (fields.containsKey(name))
			{
			try
				{
				Field field = (Field) fields.get(name);
				if (field.getType() == Integer.class)
					{
					Integer val = null;
					if (value == null)
						{
						val = new Integer(0);
						}
					else
						{
						try
							{
							val = Integer.valueOf(value.toString());
							}
						catch (NumberFormatException ne)
							{
							val = new Integer(0);
							}
						}
					field.set(this, val);
					}
				else if (field.getType() != String.class)
					{
					field.set(this, value);
					}
				else
					{
					field.set(this, value.toString());
					}
				}
			catch (Exception e)
				{
				e.printStackTrace();
				}
			}
		else if (modelAttributes.containsKey(name))
			{
			modelAttributes.put(name, (String)value);
			}
		else if (dynamicAttributes.containsKey(name))
			{
			dynamicAttributes.put(name, (String)value);
			}
		return value;
		}

	public Integer getPrimaryKey()
		{
		return primaryKey;
		}

	public void setPrimaryKey(Integer primaryKey)
		{
		this.primaryKey = primaryKey;
		}

	public String getModelName()
		{
		return modelName;
		}

	public void setModelName(String modelName)
		{
		this.modelName = modelName;
		}

	public String getModelDescription()
		{
		return modelDescription;
		}

	public void setModelDescription(String modelDescription)
		{
		this.modelDescription = modelDescription;
		}

	public EquipmentType getEquipmentType()
		{
		return equipmentType;
		}

	public void setEquipmentType(EquipmentType equipmentType)
		{
		this.equipmentType = equipmentType;
		}

	public Area getArea()
		{
		return area;
		}

	public void setArea(Area area)
		{
		this.area = area;
		}

	public String getEngineerContact()
		{
		return engineerContact;
		}

	public void setEngineerContact(String engineerContact)
		{
		this.engineerContact = engineerContact;
		}

	public String getVendor()
		{
		return vendor;
		}

	public void setVendor(String vendor)
		{
		this.vendor = vendor;
		}

	public String getPlatform()
		{
		return platform;
		}

	public void setPlatform(String platform)
		{
		this.platform = platform;
		}

	public String getStatus()
		{
		return status;
		}

	public void setStatus(String status)
		{
		this.status = status;
		}

	public Map<String, String> getModelAttributes()
		{
		return modelAttributes;
		}

	public void setModelAttributes(Map<String, String> modelAttributes)
		{
		this.modelAttributes = modelAttributes;
		}

	public Map<String, String> getDynamicAttributes()
		{
		return dynamicAttributes;
		}

	public void setDynamicAttributes(Map<String, String> dynamicAttributes)
		{
		this.dynamicAttributes = dynamicAttributes;
		}

	public String getCreator()
		{
		return creator;
		}

	public void setCreator(String creator)
		{
		this.creator = creator;
		}

	public String getApprover()
		{
		return approver;
		}

	public void setApprover(String approver)
		{
		this.approver = approver;
		}

	public Integer getEquipmentTypePk()
		{
		return equipmentTypePk;
		}

	public void setEquipmentTypePk(Integer equipmentTypePk)
		{
		this.equipmentTypePk = equipmentTypePk;
		}

	public Integer getAreaPk()
		{
		return areaPk;
		}

	public void setAreaPk(Integer areaPk)
		{
		this.areaPk = areaPk;
		}

	public String getReleaser()
		{
		return releaser;
		}

	public void setReleaser(String releaser)
		{
		this.releaser = releaser;
		}

	public String getCreateDate()
		{
		return createDate;
		}

	public void setCreateDate(String createDate)
		{
		this.createDate = createDate;
		}

	public String getApproveDate()
		{
		return approveDate;
		}

	public void setApproveDate(String approveDate)
		{
		this.approveDate = approveDate;
		}

	public String getReleaseDate()
		{
		return releaseDate;
		}

	public void setReleaseDate(String releaseDate)
		{
		this.releaseDate = releaseDate;
		}

	public String getLastUpdateBy()
		{
		return lastUpdateBy;
		}

	public void setLastUpdateBy(String lastUpdateBy)
		{
		this.lastUpdateBy = lastUpdateBy;
		}

	public String getLastUpdateDate()
		{
		return lastUpdateDate;
		}

	public void setLastUpdateDate(String lastUpdateDate)
		{
		this.lastUpdateDate = lastUpdateDate;
		}

	public int compareTo(Model o)
		{
		return o.modelDescription.compareTo(modelDescription);
		}

	public String getMfrCode() 
		{
		return mfrCode;
		}

	public void setMfrCode(String mfrCode) 
		{
		this.mfrCode = mfrCode;
		}

	public Map<String, String> getModelMapping()
		{
		return modelMapping;
		}

	public void setModelMapping(Map<String, String> modelMapping)
		{
		this.modelMapping = modelMapping;
		}
	
	}
