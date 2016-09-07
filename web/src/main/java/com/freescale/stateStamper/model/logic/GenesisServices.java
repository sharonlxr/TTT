package com.freescale.stateStamper.model.logic;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;

import com.freescale.stateStamper.model.entity.Machine;
import com.freescale.stateStamper.model.entity.Model;

public class GenesisServices
	{
	private GenesisIntefaceManager genesisInterfaceManager;
	
	public GenesisServices(Properties properties)
		{
		String genesisHost= properties.getProperty("genesisHost");
		String genesisPort= properties.getProperty("genesisPort");
		String genesisSecureID= properties.getProperty("genesisSecureID");
		if(StringUtils.isNotEmpty(genesisHost)&&StringUtils.isNotEmpty(genesisPort)&&StringUtils.isNotEmpty(genesisSecureID))
			{
			genesisInterfaceManager = new GenesisIntefaceManager(properties);
			}
		}
	/**
	 * Send machine details to genesis
	 * @param machine
	 * @throws Exception
	 */
	public void saveMachine(Machine machine) throws Exception
		{
		if(genesisInterfaceManager!=null && machine.getEquipmentType().isMesEquipmentSyncEnabled())
			{
			boolean exist = genesisInterfaceManager.isMachineExisting(machine.getMESName());
			if(exist)
				{
				genesisInterfaceManager.saveMachine(machine,false);
				}
			else
				{
				genesisInterfaceManager.saveMachine(machine,true);
				}
			}
		}
	/**
	 * After durable state changed, TTT will call this to update genesis machine state
	 * @param machine
	 * @throws Exception
	 */
	public void updateMachineStatus(Machine machine) throws Exception
		{
		if(genesisInterfaceManager!=null && machine.getEquipmentType().isMesEquipmentSyncEnabled())
			{
			boolean exist = genesisInterfaceManager.isMachineExisting(machine.getMESName());
			if(exist)
				{
				genesisInterfaceManager.updateMachineStatus(machine);
				}
			else 
				{
				throw new IllegalArgumentException("The machine "+machine.mESName+" doesn't exist in genesis!");
				}
			}
		}
	/**
	 * Send retire machine request to genesis
	 * @param machine
	 * @throws Exception
	 */
	public void deleteMachine(Machine machine) throws Exception
		{
		if(genesisInterfaceManager!=null&& machine.getEquipmentType().isMesEquipmentSyncEnabled())
			{
			boolean exist = genesisInterfaceManager.isMachineExisting(machine.getMESName());
			if(exist)
				{
				genesisInterfaceManager.deleteMachine(machine);
				}
			}
		}
	/**
	 * Get Machine models from genesis
	 * @param machineType
	 * @param platform
	 * @return
	 * @throws Exception
	 */
	public List<String> getMachineModel(String machineType, String platform) throws Exception
		{
		List<String> result = new ArrayList<String>();
		if(genesisInterfaceManager!=null)
			{
			result.addAll(genesisInterfaceManager.getMachineModel(machineType, platform));
			}
		return result;
		}
	/**
	 * get child model from genesis
	 * @param machType
	 * @param model
	 * @return
	 * @throws Exception
	 */
	public List<String> getChildModel(String machType, String model) throws Exception
		{
		List<String> result = new ArrayList<String>();
		if(genesisInterfaceManager!=null)
			{
			if("LOADBRD".equals(machType))
				{
				result.addAll(genesisInterfaceManager.getLoadboardConfigurationMap(model));
				}
			}
		return result;
		}
	/**
	 * get machine platform API
	 * @param machType
	 * @return
	 * @throws Exception
	 */
	public List<String> getMachinePlatform(String machType) throws Exception
		{
		List<String> result = new ArrayList<String>();
		if(genesisInterfaceManager!=null)
			{
			result.addAll(genesisInterfaceManager.getMachinePlatform(machType));
			}
		return result;
		}
	/**
	 * Send model dtails to genesis
	 * @param model
	 * @throws Exception
	 */
	public void saveModel(Model model) throws Exception
		{
		if(genesisInterfaceManager!=null&&"Y".equalsIgnoreCase(model.getEquipmentType().syncModelFlag))
			{
			genesisInterfaceManager.saveModel(model);
			boolean exist = genesisInterfaceManager.isMachTypeModelCombExisting(model.getEquipmentType().mesEquipmentType,model.getModelName());
			if(!exist)
				{
				genesisInterfaceManager.addMachTypeModelComb(model.getEquipmentType().mesEquipmentType, model.getModelName());
				}
			}
		}

	/**
	 * As suggested by genesis team, we cannot delete genesis model. So the method will be deprecated
	 * @param model
	 * @throws Exception
	 */
	@Deprecated
	public boolean deleteModel(Model model) throws Exception
		{
		boolean result = false;
		if(genesisInterfaceManager!=null&&"Y".equalsIgnoreCase(model.getEquipmentType().syncModelFlag))
			{
			boolean exist = genesisInterfaceManager.isModelExisting(model.getModelName(),model.getMfrCode());
			if(exist)
				{
				boolean combExist = genesisInterfaceManager.isMachTypeModelCombExisting(model.getEquipmentType().mesEquipmentType,model.getModelName());
				if(combExist)
					{
					genesisInterfaceManager.deleteMachTypeModelComb(model.getEquipmentType().mesEquipmentType, model.getModelName());
					}
				genesisInterfaceManager.deleteModel(model);
				result = true;
				}
			}
		return result;
		}

	}
