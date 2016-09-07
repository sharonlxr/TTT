package com.freescale.stateStamper.vo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.freescale.stateStamper.model.entity.EquipmentAttribute;
import com.freescale.stateStamper.model.entity.Machine;
import com.freescale.stateStamper.model.entity.Model;

public class ModelVO
	{
	private Integer primaryKey;
	private String modelName;
	private String modelDescription;
	private String vendor;
	private String platform;
	private boolean autoModelName;
	private boolean autoModelDescription;
	private String approvalType;
	private String userRole;
	private String engineerContact;
	private String modelAttributesJSON;
	private String dynamicAttributesJSON;
	private String mfrCode;
	private boolean propagateAttributes = false;
	private List<ModelAttributeVO> modelAttributes = new ArrayList<ModelAttributeVO>();
	private List<EquipmentAttribute> dynamicAttributes = new ArrayList<EquipmentAttribute>();
	private Map<String,String> modelAttributesValues = new HashMap<String,String>();
	private Map<String,String> dynamicAttributesValues = new HashMap<String,String>();
	private Map<String,Set<String>> dropdownValues= new HashMap<String,Set<String>>();
	
	private List<String> creatorRoleList = new ArrayList<String>();
	private List<String> approverRoleList = new ArrayList<String>();
	private List<String> releaserRoleList = new ArrayList<String>();
	
	private Set<String> platformSet = new HashSet<String>();
	private Set<String> creatorSet = new HashSet<String>();
	private Set<String> statusSet = new HashSet<String>();
	private Set<String> engineerContactSet = new HashSet<String>();
	private List<String> searchAttributes = new ArrayList<String>();
	private String searchAttributesJSON;
	private Map<String,Set<String>> searchDropDownValues = new HashMap<String,Set<String>>();
	private Map<String,String> searchValues = new HashMap<String,String>();
	private List<Machine> durableList = new ArrayList<Machine>();
	private List<String> selectChildrenList = new ArrayList<String>();
	private Map<String,Set<String>> genesisValues = new HashMap<String,Set<String>>();
	private Map<String,List<Model>> childrenModelListMap = new HashMap<String,List<Model>>();
	private Map<String,String> childrenModelMap = new HashMap<String,String>();
	private List<String> childrenModelList = new ArrayList<String>();
	private boolean modelMappingEnabled = false;
	
	public List<Machine> getDurableList() {
		return durableList;
	}
	public void setDurableList(List<Machine> durableList) {
		this.durableList = durableList;
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
	public boolean getAutoModelName()
		{
		return autoModelName;
		}
	public void setAutoModelName(boolean autoModelName)
		{
		this.autoModelName = autoModelName;
		}
	public boolean getAutoModelDescription()
		{
		return autoModelDescription;
		}
	public void setAutoModelDescription(boolean autoModelDescription)
		{
		this.autoModelDescription = autoModelDescription;
		}
	public String getApprovalType()
		{
		return approvalType;
		}
	public void setApprovalType(String approvalType)
		{
		this.approvalType = approvalType;
		}
	public String getUserRole()
		{
		return userRole;
		}
	public void setUserRole(String userRole)
		{
		this.userRole = userRole;
		}
	public String getEngineerContact()
		{
		return engineerContact;
		}
	public void setEngineerContact(String engineerContact)
		{
		this.engineerContact = engineerContact;
		}
	public String getModelAttributesJSON()
		{
		return modelAttributesJSON;
		}
	public void setModelAttributesJSON(String modelAttributesJSON)
		{
		this.modelAttributesJSON = modelAttributesJSON;
		}
	public String getDynamicAttributesJSON()
		{
		return dynamicAttributesJSON;
		}
	public void setDynamicAttributesJSON(String dynamicAttributesJSON)
		{
		this.dynamicAttributesJSON = dynamicAttributesJSON;
		}
	public List<ModelAttributeVO> getModelAttributes()
		{
		return modelAttributes;
		}
	public void setModelAttributes(List<ModelAttributeVO> modelAttributes)
		{
		this.modelAttributes = modelAttributes;
		}
	public List<EquipmentAttribute> getDynamicAttributes()
		{
		return dynamicAttributes;
		}
	public void setDynamicAttributes(List<EquipmentAttribute> dynamicAttributes)
		{
		this.dynamicAttributes = dynamicAttributes;
		}
	public Map<String, String> getModelAttributesValues()
		{
		return modelAttributesValues;
		}
	public void setModelAttributesValues(Map<String, String> modelAttributesValues)
		{
		this.modelAttributesValues = modelAttributesValues;
		}
	public Map<String, String> getDynamicAttributesValues()
		{
		return dynamicAttributesValues;
		}
	public void setDynamicAttributesValues(Map<String, String> dynamicAttributesValues)
		{
		this.dynamicAttributesValues = dynamicAttributesValues;
		}
	public Map<String, Set<String>> getDropdownValues()
		{
		return dropdownValues;
		}
	public void setDropdownValues(Map<String, Set<String>> dropdownValues)
		{
		this.dropdownValues = dropdownValues;
		}
	public List<String> getCreatorRoleList()
		{
		return creatorRoleList;
		}
	public void setCreatorRoleList(List<String> creatorRoleList)
		{
		this.creatorRoleList = creatorRoleList;
		}
	public List<String> getApproverRoleList()
		{
		return approverRoleList;
		}
	public void setApproverRoleList(List<String> approverRoleList)
		{
		this.approverRoleList = approverRoleList;
		}
	public List<String> getReleaserRoleList()
		{
		return releaserRoleList;
		}
	public void setReleaserRoleList(List<String> releaserRoleList)
		{
		this.releaserRoleList = releaserRoleList;
		}
	public Set<String> getPlatformSet()
		{
		return platformSet;
		}
	public void setPlatformSet(Set<String> platformSet)
		{
		this.platformSet = platformSet;
		}
	public Set<String> getCreatorSet()
		{
		return creatorSet;
		}
	public void setCreatorSet(Set<String> creatorSet)
		{
		this.creatorSet = creatorSet;
		}
	public Set<String> getStatusSet()
		{
		return statusSet;
		}
	public void setStatusSet(Set<String> statusSet)
		{
		this.statusSet = statusSet;
		}
	public Set<String> getEngineerContactSet()
		{
		return engineerContactSet;
		}
	public void setEngineerContactSet(Set<String> engineerContactSet)
		{
		this.engineerContactSet = engineerContactSet;
		}
	public List<String> getSearchAttributes()
		{
		return searchAttributes;
		}
	public void setSearchAttributes(List<String> searchAttributes)
		{
		this.searchAttributes = searchAttributes;
		}
	public String getSearchAttributesJSON()
		{
		return searchAttributesJSON;
		}
	public void setSearchAttributesJSON(String searchAttributesJSON)
		{
		this.searchAttributesJSON = searchAttributesJSON;
		}
	public Map<String, Set<String>> getSearchDropDownValues()
		{
		return searchDropDownValues;
		}
	public void setSearchDropDownValues(Map<String, Set<String>> searchDropDownValues)
		{
		this.searchDropDownValues = searchDropDownValues;
		}
	public Map<String, String> getSearchValues()
		{
		return searchValues;
		}
	public void setSearchValues(Map<String, String> searchValues)
		{
		this.searchValues = searchValues;
		}
	public boolean isPropagateAttributes() 
		{
		return propagateAttributes;
		}
	public void setPropagateAttributes(boolean propagateAttributes) 
		{
		this.propagateAttributes = propagateAttributes;
		}
	public List<String> getSelectChildrenList() 
		{
		return selectChildrenList;
		}
	public void setSelectChildrenList(List<String> selectChildrenList) 
		{
		this.selectChildrenList = selectChildrenList;
		}
	public Map<String, Set<String>> getGenesisValues()
		{
		return genesisValues;
		}
	public void setGenesisValues(Map<String, Set<String>> genesisValues)
		{
		this.genesisValues = genesisValues;
		}
	public String getMfrCode()
		{
		return mfrCode;
		}
	public void setMfrCode(String mfrCode)
		{
		this.mfrCode = mfrCode;
		}
	public Map<String, List<Model>> getChildrenModelListMap()
		{
		return childrenModelListMap;
		}
	public void setChildrenModelListMap(Map<String, List<Model>> childrenModelListMap)
		{
		this.childrenModelListMap = childrenModelListMap;
		}
	public Map<String, String> getChildrenModelMap()
		{
		return childrenModelMap;
		}
	public void setChildrenModelMap(Map<String, String> childrenModelMap)
		{
		this.childrenModelMap = childrenModelMap;
		}
	public List<String> getChildrenModelList()
		{
		return childrenModelList;
		}
	public void setChildrenModelList(List<String> childrenModelList)
		{
		this.childrenModelList = childrenModelList;
		}
	public boolean isModelMappingEnabled()
		{
		return modelMappingEnabled;
		}
	public void setModelMappingEnabled(boolean modelMappingEnabled)
		{
		this.modelMappingEnabled = modelMappingEnabled;
		}
	
	
	}
