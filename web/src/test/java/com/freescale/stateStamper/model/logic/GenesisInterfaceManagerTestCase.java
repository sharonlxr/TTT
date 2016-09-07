package com.freescale.stateStamper.model.logic;

import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import com.freescale.stateStamper.model.entity.DurableStatus;
import com.freescale.stateStamper.model.entity.EquipmentType;
import com.freescale.stateStamper.model.entity.Machine;
import com.freescale.stateStamper.model.entity.Model;

import genesis.api.libs.InterfaceMgrSocketConfig;
import junit.framework.Assert;
import junit.framework.TestCase;

public class GenesisInterfaceManagerTestCase  extends TestCase
	{
	private GenesisIntefaceManager genesisInterfaceManager = null;
	private static String machId = "Test"+Math.round(Math.random()*100000);
	private static String modelId = "TestModel"+Math.round(Math.random()*100000);
	private Machine machine = new Machine();
	private Model model = new Model();
	protected void setUp() throws Exception
		{
		Properties properties = new Properties();
		properties.put("genesisHost","liriver");
		properties.put("genesisPort","55699");
		properties.put("genesisSecureID","abc123");
		properties.put("genesisTxnUserID","B32793");
		properties.put("genesisEmailNotification","B48475");
		properties.put("genesis.addMachine.instance","MatlMgr");
		properties.put("genesis.updateMachineAttr.instance","MatlMgr");
		properties.put("genesis.machineStateChange.instance","M3Mgr");
		properties.put("genesis.getMachineModel.instance","M3Mgr");
		properties.put("genesis.getLoadboardConfigurationMap.instance","M3Mgr");
		properties.put("genesis.getMachineAttributes.instance","M3Mgr");
		properties.put("genesis.setupMachineModel.instance","MatlMgr");
		properties.put("genesis.deleteMachineModel.instance","MatlMgr");
		properties.put("genesis.getMachineModelAttributes.instance","MatlMgr");
		properties.put("genesis.getMachTypeModelCombinations.instance","MatlMgr");
		properties.put("genesis.addMachTypeModelComb.instance","MatlMgr");
		properties.put("genesis.deleteMachTypeModelComb.instance","MatlMgr");
		genesisInterfaceManager = new GenesisIntefaceManager(properties);
		System.out.println(machId);
		machine.setMESName(machId);
		EquipmentType et = new EquipmentType();
		et.setMesEquipmentSync("Y");
		et.mesEquipmentType = "LOADBRD";
		machine.setEquipmentType(et);
		machine.setAreaName("TEST1");
		machine.setPlatform("J973");
		machine.context = new HashMap();
		DurableStatus durableStatus = new DurableStatus();
		durableStatus.STATE="Productive";
		durableStatus.SUBSTATE="Test";
		machine.setModelName("ABC");
		machine.context.put("durableStatus", durableStatus);
		model.setModelName("ABC");
		model.setModelDescription("TEST MODEL");
		model.setPlatform("J973");
		model.setMfrCode("LOADBOARD");
		model.setEquipmentType(et);
		}
	
	public void testAddModel() throws Exception
		{
		genesisInterfaceManager.saveModel(model);
		}
	
	public void testAddMachTypeModelCombination() throws Exception
		{
		boolean exist = genesisInterfaceManager.isMachTypeModelCombExisting(model.getEquipmentType().mesEquipmentType,model.getModelName());
		if(!exist)
			{
			genesisInterfaceManager.addMachTypeModelComb(model.getEquipmentType().mesEquipmentType,model.getModelName());
			}
		}
	public void testAddMachine() throws Exception
		{
		genesisInterfaceManager.saveMachine(machine,true);
		}
	public void testMahineExist() throws Exception
		{
		genesisInterfaceManager.isMachineExisting(machine.mESName);
		}
	public void testUpdateMachine() throws Exception
		{
		genesisInterfaceManager.saveMachine(machine,false);
		}
	
	public void testUpdateMachineStatus() throws Exception
		{
		genesisInterfaceManager.updateMachineStatus(machine);
		}
	
	public void testDeleteMachine() throws Exception
		{
		genesisInterfaceManager.deleteMachine(machine);
		}
	
	public void testGetModel() throws Exception
		{
		List<String> machineModel = genesisInterfaceManager.getMachineModel("LOADBRD",null);
		}
	
	public void testGetModelByPlatform() throws Exception
		{
		List<String> machineModel = genesisInterfaceManager.getMachineModel("LOADBRD","ULFEX-B");
		}
	public void testGetPlatforms() throws Exception
		{
		List<String> machinePlatform = genesisInterfaceManager.getMachinePlatform("LOADBRD");
		}
	public void testGetLoadboradConfiguration() throws Exception
		{
		List<String> childModel = genesisInterfaceManager.getLoadboardConfigurationMap("ATX7987A");
		}
	}
