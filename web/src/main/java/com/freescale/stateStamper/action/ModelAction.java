package com.freescale.stateStamper.action;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.struts2.interceptor.SessionAware;

import com.freescale.serverUtil.Persistence;
import com.freescale.stateStamper.model.entity.Administrator;
import com.freescale.stateStamper.model.entity.Area;
import com.freescale.stateStamper.model.entity.BusinessRule;
import com.freescale.stateStamper.model.entity.BusinessRuleAction;
import com.freescale.stateStamper.model.entity.EquipmentAttribute;
import com.freescale.stateStamper.model.entity.EquipmentType;
import com.freescale.stateStamper.model.entity.Machine;
import com.freescale.stateStamper.model.entity.Model;
import com.freescale.stateStamper.model.entity.ModelStatus;
import com.freescale.stateStamper.model.logic.DurableEmail;
import com.freescale.stateStamper.model.logic.FormatReport;
import com.freescale.stateStamper.service.ModelService;
import com.freescale.stateStamper.util.ModelUtil;
import com.freescale.stateStamper.vo.ModelAttributeVO;
import com.freescale.stateStamper.vo.ModelVO;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.opensymphony.xwork2.ActionSupport;

public class ModelAction extends ActionSupport implements SessionAware 
	{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private ModelService modelService = ModelService.getInstance();
	private Map session;
	private ModelVO modelVO = new ModelVO();
	private List<Model> modelList;
	private Model model;
	private String errorMessage;
	public void setSession(Map session)
		{
		this.session = session;
		}
	public void setModelService(ModelService modelService)
		{
		this.modelService = modelService;
		}
	public Model getModel()
		{
		return model;
		}

	public void setModel(Model model)
		{
		this.model = model;
		}

	public ModelVO getModelVO()
		{
		return modelVO;
		}
	public void setModelVO(ModelVO modelVO)
		{
		this.modelVO = modelVO;
		}
	public List<Model> getModelList()
		{
		return modelList;
		}
	public void setModelList(List<Model> modelList)
		{
		this.modelList = modelList;
		}
	public String getErrorMessage()
		{
		return errorMessage;
		}
	public void setErrorMessage(String errorMessage)
		{
		this.errorMessage = errorMessage;
		}
	public String create() throws Throwable
		{
		EquipmentType et = getEquipmentType();
		BusinessRule br = et.getBusinessRuleByEvent(BusinessRule.MODEL_CONFIGURATION);
		if(br==null)
			{
			errorMessage = "Please configure all model settings in Business Rule";
			return "list";
			}
		setupModelConfigurations();
		return "success";
		}
	public String edit() throws Throwable
		{
		EquipmentType et = getEquipmentType();
		BusinessRule br = et.getBusinessRuleByEvent(BusinessRule.MODEL_CONFIGURATION);
		Administrator admin = (Administrator)session.get("user");
		model = modelService.getModelByPk(modelVO.getPrimaryKey());
		if(model==null)
			{
			errorMessage = "The selected model is not existing";
			return "list";
			}
		if(br==null)
			{
			errorMessage = "Please configure all model settings in Business Rule";
			return "list";
			}
		modelVO.setModelName(model.getModelName());
		modelVO.setModelDescription(model.getModelDescription());
		modelVO.setPlatform(model.getPlatform());
		modelVO.setEngineerContact(model.getEngineerContact());
		modelVO.setVendor(model.getVendor());
		modelVO.setModelAttributesValues(model.getModelAttributes());
		modelVO.setDynamicAttributesValues(model.getDynamicAttributes());
		setupModelConfigurations();
		Boolean flag = BooleanUtils.toBooleanObject(getModelConfigurationValue(BusinessRuleAction.OVERWRITE_DURABLE_ATTRIBUTES,"flag"));
		if(flag!=null&&flag.booleanValue()==true&&"ACTIVE".equalsIgnoreCase(model.getStatus()))
			{
			modelVO.getDurableList().addAll(getDurablesByModel(model));
			modelVO.setPropagateAttributes(true);
			}
		if("3".equals(modelVO.getApprovalType()))
			{
			if(model.getCreator().equals(admin.coreId)&&ModelStatus.PENDING.toString().equals(model.getStatus()))
				{
				return "success_approver";
				}
			else if(modelVO.getApproverRoleList().contains(modelVO.getUserRole())&&modelVO.getReleaserRoleList().contains(modelVO.getUserRole()))
				{
				return "success";
				}
			else if(modelVO.getApproverRoleList().contains(modelVO.getUserRole()))
				{
				return "success_approver";		
				}
			else if(modelVO.getReleaserRoleList().contains(modelVO.getUserRole()))
				{
				return "success_releaser";
				}
			else
				{
				errorMessage = "You donot have privilege to update the model";
				return "list";
				}
			}
		else
			{
			return "success";
			}
		}
	public String save() throws Throwable
		{
		EquipmentType et =  getEquipmentType();
		Area area = (Area)session.get("area");
		Administrator admin = (Administrator)session.get("user");
		Gson gson = new Gson();
		List<String> creatorRoleList = getModelConfigurationRoleList(BusinessRuleAction.CREATOR_ROLE);
		if(!creatorRoleList.contains(String.valueOf(admin.rolePK)))
			{
			errorMessage = "You donot have permission to add model";
			setupModelConfigurations();
			return "input";
			}
		Model model = new Model();
		model.setEquipmentTypePk(et.primaryKey);
		model.setEquipmentType(et);
		model.setAreaPk(area.getPrimaryKey());
		model.setArea(area);
		model.setPlatform(modelVO.getPlatform());
		model.setEngineerContact(admin.coreId);
		model.setMfrCode(modelVO.getMfrCode());
		if(StringUtils.isNotEmpty(modelVO.getVendor()))
			{
			model.setVendor(modelVO.getVendor().toUpperCase());
			}
		if(StringUtils.isNotEmpty(modelVO.getModelAttributesJSON()))
			{
			Map<String,String> modelAttributesMap = gson.fromJson(modelVO.getModelAttributesJSON(),new TypeToken<HashMap<String,String>>(){}.getType());
			model.setModelAttributes(modelAttributesMap);
			}
		if(StringUtils.isNotEmpty(modelVO.getDynamicAttributesJSON()))
			{
			Map<String,String> dynamicAttributesMap = gson.fromJson(modelVO.getDynamicAttributesJSON(),new TypeToken<HashMap<String,String>>(){}.getType());
			model.setDynamicAttributes(dynamicAttributesMap);
			}
		if(modelVO.getChildrenModelList()!=null)
			{
			Map<String,String> childModelMap = new HashMap<String,String>();
			for(String childModelPk:modelVO.getChildrenModelList())
				{
				Model childModel=modelService.getModelByPk(Integer.parseInt(childModelPk));
				childModelMap.put(childModel.getEquipmentType().name, childModelPk);
				}
			model.setModelMapping(childModelMap);
			}
		try
			{
			setModelDescription(model);
			String approvalType = getModelConfigurationValue(BusinessRuleAction.APPROVAL_PROCESS,"type");
			if("1".equalsIgnoreCase(approvalType))
				{
				model.setStatus(ModelStatus.ACTIVE.toString());
				setModelName(model,true);
				}
			else
				{
				model.setStatus(ModelStatus.PENDING.toString());
				setModelName(model,false);
				}
			modelService.validateModelName(model);
			modelService.validateModelDescription(model);
			}
		catch(Throwable e)
			{
			setupModelConfigurations();
			errorMessage = e.getMessage();
			return "input";
			}
		model.setCreator(admin.coreId);
		model.setCreateDate(sdf.format(new Date()));
		model.setLastUpdateBy(admin.coreId);
		model.setLastUpdateDate(sdf.format(new Date()));
		modelService.saveModel(model);
		modelVO.setPrimaryKey(model.getPrimaryKey());
		Set<String> emailList = getEmailList(model,false,true);
		sendNotificationEmail(emailList,model,"Model Created Notification","Below model has been created by "+model.getLastUpdateBy());
		return "success";
		}

	public String update() throws Throwable
		{
		Administrator admin = (Administrator)session.get("user");
		Gson gson = new Gson();
		model = modelService.getModelByPk(modelVO.getPrimaryKey());
		if(StringUtils.isNotEmpty(modelVO.getPlatform()))
			{
			model.setPlatform(modelVO.getPlatform());
			}
		if(StringUtils.isNotEmpty(modelVO.getVendor()))
			{
			model.setVendor(modelVO.getVendor().toUpperCase());
			}
		if(StringUtils.isNotEmpty(modelVO.getMfrCode()))
			{
			model.setMfrCode(modelVO.getMfrCode());
			}
		if(StringUtils.isNotEmpty(modelVO.getModelAttributesJSON()))
			{
			Map<String,String> modelAttributesMap = gson.fromJson(modelVO.getModelAttributesJSON(),new TypeToken<HashMap<String,String>>(){}.getType());
			model.setModelAttributes(modelAttributesMap);
			}
		if(StringUtils.isNotEmpty(modelVO.getDynamicAttributesJSON()))
			{
			Map<String,String> dynamicAttributesMap = gson.fromJson(modelVO.getDynamicAttributesJSON(),new TypeToken<HashMap<String,String>>(){}.getType());
			model.setDynamicAttributes(dynamicAttributesMap);
			}
		if(model==null)
			{
			errorMessage = "The selected model is not existing";
			setupModelConfigurations();
			return "list";
			}
		try
			{
			setModelName(model,false);
			setModelDescription(model);
			modelService.validateModelName(model);
			modelService.validateModelDescription(model);
			}
		catch(Throwable e)
			{
			setupModelConfigurations();
			errorMessage = e.getMessage();
			return "input";
			}
		if(ModelStatus.REJECTED.toString().equalsIgnoreCase(model.getStatus()))
			{
			model.setStatus(ModelStatus.PENDING.toString());
			}
		model.setLastUpdateBy(admin.coreId);
		model.setLastUpdateDate(sdf.format(new Date()));
		modelService.saveModel(model);
		overwriteDurableAttributes(model);
		Set<String> emailList = getEmailList(model,true,true);
		sendNotificationEmail(emailList,model,"Model Updated Notification","Below model has been updated by "+model.getLastUpdateBy());
		return "success";
		}
	public String list() throws Throwable
		{
		Model searchModel = new Model();
		Administrator admin = (Administrator)session.get("user");
		EquipmentType et =  getEquipmentType();
		Gson gson = new Gson();
		if(modelVO.getSearchAttributesJSON()!=null)
			{
			Map<String,String> searchFilteMap = gson.fromJson(modelVO.getSearchAttributesJSON(),new TypeToken<HashMap<String,String>>(){}.getType());
			modelVO.setSearchValues(searchFilteMap);
			}
		String mandatoryAttributes = getModelConfigurationValue(BusinessRuleAction.MANDATORY_ATTRIBUTES,"attributes");
		if(StringUtils.isNotEmpty(mandatoryAttributes))
			{
			Map<String,Set<String>> searchDropDownValues = new HashMap<String,Set<String>>();
			List<String> mandatoryAttributesList = Arrays.asList(mandatoryAttributes.split(","));
			for(String attribute:mandatoryAttributesList)
				{
				searchDropDownValues.put(attribute, new TreeSet<String>());
				}
			modelVO.setSearchAttributes(mandatoryAttributesList);
			modelVO.setSearchDropDownValues(searchDropDownValues);
			}
		Area area = (Area)session.get("area");
		searchModel.setEquipmentTypePk(et.primaryKey);
		searchModel.setAreaPk(area.getPrimaryKey());
		modelList = modelService.getModelList(searchModel);
		Iterator<Model> iterator = modelList.iterator();
		while(iterator.hasNext())
			{
			Model item = iterator.next();
			for(String searchFilter:modelVO.getSearchAttributes())
				{
				String filterValue = (String)item.get(searchFilter);
				if(StringUtils.isNoneEmpty(filterValue))
					{
					modelVO.getSearchDropDownValues().get(searchFilter).add(filterValue);
					}
				}
			if(StringUtils.isNoneEmpty(item.getPlatform()))
				{
				modelVO.getPlatformSet().add(item.getPlatform());
				}
			if(StringUtils.isNoneEmpty(item.getEngineerContact()))
				{
				modelVO.getEngineerContactSet().add(item.getEngineerContact());
				}
			modelVO.getCreatorSet().add(item.getCreator());
			modelVO.getStatusSet().add(item.getStatus());
			boolean matchCondition = true;
			if(modelVO.getSearchValues()!=null)
				{
				for(Entry<String,String> entry:modelVO.getSearchValues().entrySet())
					{
					String attrValue = (String)item.get(entry.getKey());
					if(!entry.getValue().equals(attrValue))
						{
						matchCondition = false;
						break;
						}
					}
				}
			if(!matchCondition)
				{
				iterator.remove();
				continue;
				}
			}
		setupModelConfigurations();
		Collections.sort(modelList);
		return "success";
		}
	public String detail() throws Throwable
		{
		model = modelService.getModelByPk(modelVO.getPrimaryKey());
		if(model==null)
			{
			errorMessage = "The selected model is not existing";
			return "list";
			}
		setupModelConfigurations();
		modelVO.getDurableList().addAll(getDurablesByModel(model));
		if(modelVO.isModelMappingEnabled())
			{
			Map<String, String> modelMapping = model.getModelMapping();
			Map<String,String> displayedModelMapping = new HashMap<String,String>();
			for(String equipmentType:modelMapping.keySet())
				{
				String modelPk = modelMapping.get(equipmentType);
				Model childModel = modelService.getModelByPk(Integer.parseInt(modelPk));
				displayedModelMapping.put(childModel.getEquipmentType().name, childModel.getModelName());
				}
			modelVO.setChildrenModelMap(displayedModelMapping);
			}
		return "success";
		}
	
	public String delete() throws Throwable
		{
		List<String> releaserRoleList = getModelConfigurationRoleList(BusinessRuleAction.RELEASER_ROLE);
		List<String> approverRoleList = getModelConfigurationRoleList(BusinessRuleAction.APPROVER_ROLE);
		Administrator admin = (Administrator)session.get("user");
		Model model = modelService.getModelByPk(modelVO.getPrimaryKey());
		List<Machine> durables = getDurablesByModel(model);
		if(durables.size()>0)
			{
			errorMessage = "Please remove all durables belong to this model first";
			return "input";
			}
		boolean deleted = false;
		if(ModelStatus.PENDING.toString().equals(model.getStatus())||ModelStatus.REJECTED.toString().equals(model.getStatus())||ModelStatus.RETIRED.toString().equals(model.getStatus()))
			{
			if(releaserRoleList.contains(String.valueOf(admin.rolePK))||approverRoleList.contains(String.valueOf(admin.rolePK))||model.getCreator().equals(admin.coreId))
				{
				modelService.deleteModel(model);
				deleted = true;
				Set<String> emailList = getEmailList(model,true,true);
				sendNotificationEmail(emailList,model,"Model Deleted Notification","Below model has been deleted by "+model.getLastUpdateBy());
				}
			}
		if(deleted==false)
			{
			errorMessage = "The model cannot be deleted due to insufficient previlege";
			return "input";
			}
		return "success";
		}
	public String approve() throws Throwable
		{
		List<String> roleList = getModelConfigurationRoleList(BusinessRuleAction.APPROVER_ROLE);
		Model model = modelService.getModelByPk(modelVO.getPrimaryKey());
		Administrator admin = (Administrator)session.get("user");
		if(ModelStatus.PENDING.toString().equals(model.getStatus())&&roleList.contains(String.valueOf(admin.rolePK)))
			{
			model.setApprover(admin.coreId);
			model.setApproveDate(sdf.format(new Date()));
			model.setLastUpdateBy(admin.coreId);
			model.setLastUpdateDate(sdf.format(new Date()));
			String approvalType = getModelConfigurationValue(BusinessRuleAction.APPROVAL_PROCESS,"type");
			if("2".equals(approvalType))
				{
				setModelName(model,true);
				model.setStatus(ModelStatus.ACTIVE.toString());
				}
			else
				{
				model.setStatus(ModelStatus.APPROVED.toString());
				}
			modelService.saveModel(model);
			Set<String> emailList = getEmailList(model,true,true);
			sendNotificationEmail(emailList,model,"Model Approved Notification","Below model has been approved by "+model.getLastUpdateBy());
			}
		return "success";
		}
	public String reject() throws Throwable
		{
		List<String> roleList = getModelConfigurationRoleList(BusinessRuleAction.APPROVER_ROLE);
		Model model = modelService.getModelByPk(modelVO.getPrimaryKey());
		Administrator admin = (Administrator)session.get("user");
		if(ModelStatus.PENDING.toString().equals(model.getStatus())&&roleList.contains(String.valueOf(admin.rolePK)))
			{
			model.setApprover(admin.coreId);
			model.setApproveDate(sdf.format(new Date()));
			model.setLastUpdateBy(admin.coreId);
			model.setLastUpdateDate(sdf.format(new Date()));
			model.setStatus(ModelStatus.REJECTED.toString());
			modelService.saveModel(model);
			Set<String> emailList = getEmailList(model,true,true);
			sendNotificationEmail(emailList,model,"Model Rejected Notification","Below model has been rejected by "+model.getLastUpdateBy());
			}
		return "success";
		}
	public String retire() throws Throwable
		{
		List<String> releaserRoleList = getModelConfigurationRoleList(BusinessRuleAction.RELEASER_ROLE);
		List<String> approverRoleList = getModelConfigurationRoleList(BusinessRuleAction.APPROVER_ROLE);
		Model model = modelService.getModelByPk(modelVO.getPrimaryKey());
		List<Machine> durables = getDurablesByModel(model);
		if(durables.size()>0)
			{
			errorMessage = "Please remove all durables belong to this model first";
			return "input";
			}
		Administrator admin = (Administrator)session.get("user");
		if(ModelStatus.ACTIVE.toString().equals(model.getStatus()))
			{
			if(releaserRoleList.contains(String.valueOf(admin.rolePK))||approverRoleList.contains(String.valueOf(admin.rolePK))||model.getCreator().equals(admin.coreId))
				{
				model.setLastUpdateBy(admin.coreId);
				model.setLastUpdateDate(sdf.format(new Date()));
				model.setStatus(ModelStatus.RETIRED.toString());
				modelService.saveModel(model);
				Set<String> emailList = getEmailList(model,true,true);
				sendNotificationEmail(emailList,model,"Model Retired Notification","Below model has been retired by "+model.getLastUpdateBy());
				}
			}
		return "success";
		}
	public String release() throws Throwable
		{
		List<String> releaserRoleList = getModelConfigurationRoleList(BusinessRuleAction.RELEASER_ROLE);
		Model model = modelService.getModelByPk(modelVO.getPrimaryKey());
		Administrator admin = (Administrator)session.get("user");
		if(ModelStatus.APPROVED.toString().equals(model.getStatus())&&releaserRoleList.contains(String.valueOf(admin.rolePK)))
			{
			setModelName(model,true);
			model.setReleaser(admin.coreId);
			model.setReleaseDate(sdf.format(new Date()));
			model.setLastUpdateBy(admin.coreId);
			model.setLastUpdateDate(sdf.format(new Date()));
			model.setStatus(ModelStatus.ACTIVE.toString());
			modelService.saveModel(model);
			Set<String> emailList = getEmailList(model,true,true);
			sendNotificationEmail(emailList,model,"Model Released Notification","Below model has been released by "+model.getLastUpdateBy());
			}
		return "success";
		}
	
	
	public String activate() throws Throwable
		{
		List<String> releaserRoleList = getModelConfigurationRoleList(BusinessRuleAction.RELEASER_ROLE);
		List<String> approverRoleList = getModelConfigurationRoleList(BusinessRuleAction.APPROVER_ROLE);
		Model model = modelService.getModelByPk(modelVO.getPrimaryKey());
		Administrator admin = (Administrator)session.get("user");
		if(ModelStatus.RETIRED.toString().equals(model.getStatus()))
			{
			if(releaserRoleList.contains(String.valueOf(admin.rolePK))||approverRoleList.contains(String.valueOf(admin.rolePK))||model.getCreator().equals(admin.coreId))
				{
				model.setLastUpdateBy(admin.coreId);
				model.setLastUpdateDate(sdf.format(new Date()));
				model.setStatus(ModelStatus.ACTIVE.toString());
				modelService.saveModel(model);
				Set<String> emailList = getEmailList(model,true,true);
				sendNotificationEmail(emailList,model,"Model Activated Notification","Below model has been activated by "+model.getLastUpdateBy());
				}
			}
		return "success";
		}
	
	public String triggerAlertEmail() throws Throwable
		{
		Model model = modelService.getModelByPk(modelVO.getPrimaryKey());
		Set<String> emailList = getEmailList(model,true,true);
		sendNotificationEmail(emailList,model,"Model Map Missing Notification","Below model donot have associated map yet");
		return "success";
		}
	
	private List<String> getModelConfigurationRoleList(String role) throws Throwable
		{
		List<String> roleList = new ArrayList<String>();
		EquipmentType et = getEquipmentType();
		BusinessRule br = et.getBusinessRuleByEvent(BusinessRule.MODEL_CONFIGURATION);
		BusinessRuleAction brAction = br.getBusinessRuleActionsByAction(role);
		if(brAction!=null)
			{
			String tmp = brAction.getActionParameters().get("role");
			if(StringUtils.isNotEmpty(tmp))
				{
				roleList.addAll(Arrays.asList(tmp.split(",")));
				}
			}
		return roleList;
		}
	
	private BusinessRuleAction getModelBusinessRuleAction(String action) throws Throwable
		{
		EquipmentType et = getEquipmentType();
		BusinessRule br = et.getBusinessRuleByEvent(BusinessRule.MODEL_CONFIGURATION);
		BusinessRuleAction brAction = br.getBusinessRuleActionsByAction(action);
		return brAction;
		}
	private String getModelConfigurationValue(String action,String paramName) throws Throwable
		{
		String result = null;
		EquipmentType et = getEquipmentType();
		BusinessRule br = et.getBusinessRuleByEvent(BusinessRule.MODEL_CONFIGURATION);
		BusinessRuleAction brAction = br.getBusinessRuleActionsByAction(action);
		if(brAction!=null)
			{
			result = brAction.getActionParameters().get(paramName);
			}
		return result;
		}
	
	private Map<String,Set<String>> getDropDownValues(String type) throws Throwable
		{
		Map<String,Set<String>> dropDownMap = new HashMap<String,Set<String>>();
		EquipmentType et =  getEquipmentType();
		BusinessRule br = et.getBusinessRuleByEvent(BusinessRule.MODEL_CONFIGURATION);
		List<BusinessRuleAction> actions= br.getActionsBy(type);
		for(BusinessRuleAction action:actions)
			{
			Map<String, String> actionParameters = action.getActionParameters();
			for(Entry<String, String> entry:actionParameters.entrySet())
				{
				String value = entry.getValue();
				Set<String> valueList = new TreeSet<String>();
				if(value!=null)
					{
					valueList.addAll(Arrays.asList(value.split(",")));
					}
				dropDownMap.put(entry.getKey(), valueList);
				}
			}
		return dropDownMap;
		}
	
	private void setupModelConfigurations() throws Throwable
		{
		Administrator admin = (Administrator)session.get("user");
		Area area = (Area)session.get("area");
		EquipmentType et = getEquipmentType();
		modelVO.setDropdownValues(getDropDownValues(BusinessRuleAction.DROPDOWN_ATTRIBUTES));
		List<ModelAttributeVO> mavList = new ArrayList<ModelAttributeVO>();
		String mandatoryAttributes = getModelConfigurationValue(BusinessRuleAction.MANDATORY_ATTRIBUTES,"attributes");
		mavList.addAll(generateModelAttributesList(modelVO.getDropdownValues(), mandatoryAttributes,true));
		String optionalAttributes = getModelConfigurationValue(BusinessRuleAction.OPTIONAL_ATTRIBUTES,"attributes");
		mavList.addAll(generateModelAttributesList(modelVO.getDropdownValues(), optionalAttributes,false));
		modelVO.setModelAttributes(mavList);
		List<EquipmentAttribute> dynamicAttributes = new ArrayList<EquipmentAttribute>();
		dynamicAttributes.addAll(et.getEquipmentAttributes());
		modelVO.setDynamicAttributes(dynamicAttributes);
		BusinessRuleAction autoName = getModelBusinessRuleAction(BusinessRuleAction.AUTO_NAME_GENERATION);
		modelVO.setAutoModelName(autoName!=null);
		BusinessRuleAction autoDesc = getModelBusinessRuleAction(BusinessRuleAction.AUTO_DESCRIPTION_GENERATION);
		modelVO.setAutoModelDescription(autoDesc!=null);
		String approvalType = getModelConfigurationValue(BusinessRuleAction.APPROVAL_PROCESS,"type");
		modelVO.setApprovalType(approvalType);
		modelVO.setCreatorRoleList(getModelConfigurationRoleList(BusinessRuleAction.CREATOR_ROLE));
		modelVO.setApproverRoleList(getModelConfigurationRoleList(BusinessRuleAction.APPROVER_ROLE));
		modelVO.setReleaserRoleList(getModelConfigurationRoleList(BusinessRuleAction.RELEASER_ROLE));
		modelVO.setUserRole(String.valueOf(admin.getRole().getRolePK()));
		modelVO.setGenesisValues(getDropDownValues(BusinessRuleAction.GENESIS_ATTRIBUTES));
		BusinessRuleAction modelMappingAction = getModelBusinessRuleAction(BusinessRuleAction.MODEL_MAPPING_CONFIGURATION);
		if(modelMappingAction!=null)
			{
			modelVO.setModelMappingEnabled(true);
			List<String> childrenEquipmentTypeList = Arrays.asList(modelMappingAction.getActionParameters().get("children").split(","));
			for(String childPk:childrenEquipmentTypeList)
				{
				EquipmentType childEt = modelService.getEquipmentType(Integer.parseInt(childPk));
				if(childEt!=null)
					{
					Model searchChildModel = new Model();
					searchChildModel.setEquipmentTypePk(Integer.parseInt(childPk));
					searchChildModel.setStatus("ACTIVE");
					searchChildModel.setAreaPk(area.getPrimaryKey());
					List<Model> childModelList = modelService.getModelList(searchChildModel);
					modelVO.getChildrenModelListMap().put(childEt.name, childModelList);
					}
				}
			}
		}
	private List<ModelAttributeVO> generateModelAttributesList(Map<String,Set<String>> dropdownValuesMap,String modelAttributes,boolean mandatory)
		{
		List<ModelAttributeVO> mavList = new ArrayList<ModelAttributeVO>();
		if(StringUtils.isNotEmpty(modelAttributes))
			{
			String[] modelAttributesArray = modelAttributes.split(",");
			for(String item:modelAttributesArray)
				{
				ModelAttributeVO mav = new ModelAttributeVO();
				mav.setName(item);
				mav.setRequired(mandatory);
				if(dropdownValuesMap.containsKey(item))
					{
					mav.setValueList(dropdownValuesMap.get(item));
					}
				mavList.add(mav);
				}
			}
		return mavList;
		}
	
	private void sendNotificationEmail(Set<String> emailList,Model model,String subject,String message) throws Throwable
		{
		StringBuilder sb = new StringBuilder();
		sb.append(message+"</br></br>");
		String modelName = model.getModelName()!=null?model.getModelName():"";
		sb.append("Model Name:"+modelName+"</br>");
		String modelDescription = model.getModelDescription()!=null?model.getModelDescription():"";
		sb.append("Model Description:"+modelDescription+"</br>");
		sb.append("Model Equipment Type:"+model.getEquipmentType().name+"</br>");
		sb.append("Model Area:"+model.getArea().getName()+"</br>");
		sb.append("Model Platform:"+model.getPlatform()+"</br>");
		Map<String, String> modelAttributes = model.getModelAttributes();
		if(modelAttributes!=null)
			{
			for(Entry<String, String> entry:modelAttributes.entrySet())
				{
				sb.append(entry.getKey()+":"+entry.getValue()+"</br>");
				}
			}
		DurableEmail email = new DurableEmail();
		email.setTo(new Vector(emailList));
		email.setSubject(subject);
		email.setType("html");
		email.setContent(sb.toString());
		new Thread(email).start();
		}
	
	private Set<String>  getEmailList(Model model,boolean releaserNeeded,boolean ApproverNeeded) throws Throwable
		{
		Set<String> roleSet = new HashSet<String>();
		Set<String> userEmailList = new HashSet<String>();
		if(releaserNeeded)
			{
			List<String> releaserRoleList = getModelConfigurationRoleList(BusinessRuleAction.RELEASER_ROLE);
			roleSet.addAll(releaserRoleList);
			}
		if(ApproverNeeded)
			{
			List<String> approverRoleList = getModelConfigurationRoleList(BusinessRuleAction.APPROVER_ROLE);
			roleSet.addAll(approverRoleList);
			}
		userEmailList.addAll(modelService.getUserEmailList(model.getEquipmentType(), model.getArea(), roleSet));
		userEmailList.add(model.getCreator()+FormatReport.getDomain());
		return userEmailList;
		}
	
	private void setModelName(Model model,boolean requireToken) throws Throwable
		{
		BusinessRuleAction autoName = getModelBusinessRuleAction(BusinessRuleAction.AUTO_NAME_GENERATION);
		if(autoName==null)
			{
			if(StringUtils.isNotEmpty(modelVO.getModelName()))
				{
				model.setModelName(modelVO.getModelName().toUpperCase());
				}
			}
		else if(requireToken)
			{
			if(StringUtils.isEmpty(model.getModelName()))
				{
				Map<String, String> actionParameters = autoName.getActionParameters();
				String method = actionParameters.get("method");
				boolean filterCharacters = Boolean.valueOf(actionParameters.get("filterCharacters"));
				if("3-DIGITS".equalsIgnoreCase(method))
					{
					modelService.set3DigitsModelName(model,filterCharacters);
					}
				}
			}
		}
	
	private void setModelDescription(Model model) throws Throwable
		{
		BusinessRuleAction autoDescriptionAction = getModelBusinessRuleAction(BusinessRuleAction.AUTO_DESCRIPTION_GENERATION);
		if (autoDescriptionAction == null)
			{
			if(StringUtils.isNotEmpty(modelVO.getModelDescription()))
				{
				model.setModelDescription(modelVO.getModelDescription().toUpperCase());
				}
			}
		else
			{
			String namingRule = autoDescriptionAction.getActionParameters().get("value");
			String description = ModelUtil.getModelDescription(model,namingRule);
			model.setModelDescription(description);
			}
		}
	
	private EquipmentType getEquipmentType() throws Throwable
		{
		EquipmentType result = null;
		EquipmentType searchEt = new EquipmentType();
		EquipmentType et = (EquipmentType)session.get("equipmentType");
		searchEt.primaryKey = et.primaryKey;
		List<EquipmentType> vector = Persistence.search(searchEt);
		if(vector!=null&&vector.size()>0)
			{
			result = vector.get(0);
			}
		return result;
		}
	
	private void overwriteDurableAttributes(Model model) throws Throwable
		{
		Boolean flag = BooleanUtils.toBooleanObject(getModelConfigurationValue(BusinessRuleAction.OVERWRITE_DURABLE_ATTRIBUTES,"flag"));
		if(flag==null||flag.booleanValue()==false||!"ACTIVE".equalsIgnoreCase(model.getStatus()))
			{
			return;
			}
		try 
			{
			for(String mesName:modelVO.getSelectChildrenList())
				{
				Machine machine = (Machine) modelService.getMachine(mesName);
				if(model.getPlatform()!=null)machine.platform = model.getPlatform();
				if(model.getVendor()!=null)machine.vendor = model.getVendor();
				for(Entry<String,String> entry:model.getModelAttributes().entrySet())
					{
					if(entry.getValue()!=null&&!entry.getValue().equals(""))
						{
						machine.set(entry.getKey(), entry.getValue());
						}
					}
				for(Entry<String,String> entry:model.getDynamicAttributes().entrySet())
					{
					if(entry.getValue()!=null&&!entry.getValue().equals(""))
						{
						machine.set(entry.getKey(), entry.getValue());
						}
					}
				modelService.insertDurableStatus(machine,(String)session.get("coreId"));
				modelService.storeMachine(machine);
				}
			} 
		catch (Throwable e) 
			{
			e.printStackTrace();
			}
		}
	private List<Machine> getDurablesByModel(Model model) throws Throwable {
		Machine searchMachine = new Machine();
		searchMachine.equipmentType_pk = model.getEquipmentTypePk();
		searchMachine.setModelPk(model.getPrimaryKey());
		List<Machine> list = new ArrayList<Machine>();
		list.addAll(Persistence.search(searchMachine));
		return list;
	}
	}
