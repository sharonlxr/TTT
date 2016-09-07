package com.freescale.stateStamper.service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import org.apache.commons.lang3.StringUtils;

import com.freescale.serverUtil.Persistence;
import com.freescale.stateStamper.model.entity.Administrator;
import com.freescale.stateStamper.model.entity.Area;
import com.freescale.stateStamper.model.entity.BusinessRule;
import com.freescale.stateStamper.model.entity.BusinessRuleAction;
import com.freescale.stateStamper.model.entity.DurableStatus;
import com.freescale.stateStamper.model.entity.EquipmentType;
import com.freescale.stateStamper.model.entity.Machine;
import com.freescale.stateStamper.model.entity.Model;
import com.freescale.stateStamper.model.entity.ModelStatus;
import com.freescale.stateStamper.model.entity.Role;
import com.freescale.stateStamper.model.logic.FormatReport;
import com.freescale.stateStamper.model.logic.GenesisServices;
import com.freescale.stateStamper.model.logic.StateTransition;
import com.freescale.stateStamper.util.ModelUtil;

public class ModelService
	{
	private static ModelService modelService = new ModelService();
	private static StateTransition stateTransition;
	private static GenesisServices genesisServices;
	private ModelService()
		{
		
		}
	
	public static void init(StateTransition stateTransition,GenesisServices genesisServices)
		{
		ModelService.stateTransition = stateTransition;
		ModelService.genesisServices = genesisServices;
		}
	
	public static ModelService getInstance()
		{
		return modelService;
		}
	
	@SuppressWarnings("unchecked")
	public List<Model> getModelList(Model searchModel) throws Throwable
		{
		List<Model> results = new ArrayList<Model>();
		List<Model> list = Persistence.search(searchModel);
		results.addAll(list);
		return results;
		}
	
	public Model getModelByPk(Integer primaryKey) throws Throwable
		{
		Model result = null;
		if(primaryKey==null)
			{
			return null;
			}
		Model searchModel = new Model();
		searchModel.setPrimaryKey(primaryKey);
		List<Model> list = Persistence.search(searchModel);
		if(list!=null&&list.size()>0)
			{
			result = list.get(0);
			}
		return result;
		}
	public void saveModel(Model model) throws Throwable
		{
		try 
			{
			if(ModelStatus.ACTIVE.toString().equals(model.getStatus()))
				{
				genesisServices.saveModel(model);
				}
			Persistence.store(model);
			}
		catch(Exception e)
			{
			throw e;
			}
		}
	
	public boolean hasAssociatedEquipments(Model model) throws Throwable
		{
		Machine machine = new Machine();
		machine.setModelPk(model.getPrimaryKey());
		List list = Persistence.search(machine);
		if(list!=null&&list.size()>0)
			{
			return true;
			}
		else
			{
			return false;
			}
		}
	public void deleteModel(Model model) throws Throwable
		{
		Persistence.delete(model);
		}

	public void set3DigitsModelName(Model model,boolean filterCharacters) throws Throwable
		{
		if(StringUtils.isNotEmpty(model.getModelName()))
			{
			return;
			}
		boolean executed = true;
		while(executed)
			{
			String sequence = null;
			if(filterCharacters)
				{
				sequence = ModelUtil.generateFilterModelToken();
				}
			else
				{
				sequence = ModelUtil.generateGenericModelToken();
				}
			model.setModelName(sequence);
			try
				{
				validateModelName(model);
				executed = false;
				}
			catch(Throwable throwable)
				{
				throwable.printStackTrace();
				}
			}
		}
	public void validateModelName(Model model) throws Throwable
		{
		BusinessRule br = model.getEquipmentType().getBusinessRuleByEvent(BusinessRule.MODEL_CONFIGURATION);
		BusinessRuleAction autoNameRule = br.getBusinessRuleActionsByAction(BusinessRuleAction.AUTO_NAME_GENERATION);
		if(StringUtils.isEmpty(model.getModelName()))
			{
			if(autoNameRule==null)
				{
				throw new Throwable("Model name cannot be null!");
				}
			else
				{
				return;
				}
			}
		Model searchModel = new Model();
		searchModel.setModelName(model.getModelName());
		searchModel.setEquipmentTypePk(model.getEquipmentTypePk());
		searchModel.setAreaPk(model.getAreaPk());
		List list = Persistence.search(searchModel);
		if(list!=null&&list.size()>0)
			{
			searchModel = (Model) list.get(0);
			if(!searchModel.getPrimaryKey().equals(model.getPrimaryKey()))
				{
				throw new Throwable("Duplicate model name!");
				}
			}
		}
	
	public void validateModelDescription(Model model) throws Throwable
		{
		if(StringUtils.isEmpty(model.getModelDescription()))
			{
			throw new Throwable("Model description cannot be null");
			}
		Model searchModel = new Model();
		searchModel.setModelDescription(model.getModelDescription());
		searchModel.setEquipmentTypePk(model.getEquipmentTypePk());
		searchModel.setAreaPk(model.getAreaPk());
		List list = Persistence.search(searchModel);
		if(list!=null&&list.size()>0)
			{
			searchModel = (Model) list.get(0);
			if(!searchModel.getPrimaryKey().equals(model.getPrimaryKey()))
				{
				throw new Throwable("Duplicate model description!");
				}
			}
		
		}

	public Set<String> getUserEmailList(EquipmentType et,Area area,Set<String> roleList) throws Throwable
		{
		Set<String> results = new HashSet<String>();
		Administrator search = new Administrator();
		List<Administrator> list = Persistence.search(search);
		if(list!=null&&list.size()>0)
			{
			for(Administrator admin:list)
				{
				Role role = admin.getRole();
				Integer areaPk = admin.getAreaPK();
				String rolePk = String.valueOf(admin.getRolePK());
				if(role.equipmenttypePK.equals(0)||role.equipmenttypePK.equals(et.primaryKey))
					{
					if(Area.AREA_ALL_PK.equals(areaPk)||area.getPrimaryKey().equals(areaPk))
						{
						if(roleList.contains(rolePk))
							{
							results.add(admin.coreId+FormatReport.getDomain());
							}
						}
					}
				}
			}
		return results;
		}
	
	public Model getModel(Model searchModel) throws Throwable
		{
		Model result = null;
		List<Model> list = Persistence.search(searchModel);
		if(list!=null&&list.size()>0)
			{
			result = list.get(0);
			}
		return result;
		}
	
	public void setModelByModelName(Machine machine) throws Throwable
		{
		Model searchModel = new Model();
		searchModel.setAreaPk(machine.getArea_pk());
		searchModel.setEquipmentTypePk(machine.getEquipmentType_pk());
		searchModel.setModelName(machine.getModelName());
		Model modelObj = getModel(searchModel);
		machine.setModelPk(modelObj.getPrimaryKey());
		machine.setModelName(modelObj.getModelName());
		machine.setModel(modelObj);
		}
	
	public Model getModelByDescription(EquipmentType et,Area area,String description) throws Throwable
		{
		Model searchModel = new Model();
		searchModel.setEquipmentTypePk(et.primaryKey);
		searchModel.setAreaPk(area.getPrimaryKey());
		searchModel.setModelDescription(description);
		return getModel(searchModel);
		}
	
	public List<String> getMachinePlatform(EquipmentType et,Area area) throws Throwable
		{
		List<String> result = new ArrayList<String>();
		Set<String> platformSet = new TreeSet<String>();
		Model searchModel = new Model();
		searchModel.setEquipmentTypePk(et.primaryKey);
		searchModel.setAreaPk(area.getPrimaryKey());
		searchModel.setStatus(ModelStatus.ACTIVE.toString());
		List<Model> modelList = getModelList(searchModel);
		for(Model model:modelList)
			{
			platformSet.add(model.getPlatform());
			}
		result.addAll(platformSet);
		return result;
		}
	
	public List<String> getMachineModel(EquipmentType et,Area area,String platform) throws Throwable
		{
		List<String> result = new ArrayList<String>();
		Set<String> modelSet = new TreeSet<String>();
		Model searchModel = new Model();
		searchModel.setEquipmentTypePk(et.primaryKey);
		searchModel.setAreaPk(area.getPrimaryKey());
		searchModel.setStatus(ModelStatus.ACTIVE.toString());
		if(StringUtils.isNotEmpty(platform))
			{
			searchModel.setPlatform(platform);
			}
		List<Model> modelList = getModelList(searchModel);
		for(Model model:modelList)
			{
			modelSet.add(model.getModelDescription());
			}
		result.addAll(modelSet);
		return result;
		}
	
	public Machine getMachine(String mid)
		{
		return stateTransition.getMachine(mid);
		}
	
	public void storeMachine(Machine durable) throws Throwable
		{
		Persistence.store(durable);
		stateTransition.setMachine(durable);
		}
	
	public void insertDurableStatus(Machine durable, String coreId) throws Throwable
		{
		DurableStatus status = stateTransition.getDurableStatus(durable);	
		DurableStatus oldDStatus = status.clone();
		status.STATUS_PK = null;
		status.COMMENTS = null;
		status.OPERATOR_ID = coreId;
		status.putNewComments(status.OPERATOR_ID , status.TIME, "Overwrite Durable Attributes by Model Update");		
		Date now = new Date();
		status.TIME =  new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(now);
		Persistence.store(status);
		durable.context.put("durableStatus", status);
		stateTransition.updateDurationForDurableStatus(durable, oldDStatus,new HashMap(),null);
		}
	
	public EquipmentType getEquipmentType(int equipmentTypePk) throws Throwable
		{
		EquipmentType result = null;
		EquipmentType et = new EquipmentType();
		et.primaryKey = equipmentTypePk;
		Vector vector = Persistence.search(et);
		if(vector!=null&&vector.size()>0)
			{
			result = (EquipmentType) vector.get(0);
			}
		return result;
		}
	}
