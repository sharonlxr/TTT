package com.freescale.stateStamper.model.logic;

import genesis.api.APIExecutor;
import genesis.api.DataCollection;
import genesis.api.libs.CommonUtilities;
import genesis.api.libs.InterfaceMgrSocketConfig;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.freescale.stateStamper.model.entity.DurableStatus;
import com.freescale.stateStamper.model.entity.Machine;
import com.freescale.stateStamper.model.entity.Model;



public class GenesisIntefaceManager
	{

	private InterfaceMgrSocketConfig interfaceMfrSocketConfig = null;
	private String txnUser = null;
	private String emailList = null;
	private static Logger logger = LoggerFactory.getLogger("genesisInterface");
	private Properties properties = null;

	public GenesisIntefaceManager(Properties properties)
		{
		this.properties = properties;
		String host = properties.getProperty("genesisHost");
		int port = Integer.parseInt(properties.getProperty("genesisPort"));
		String secureId = properties.getProperty("genesisSecureID");
		String txnUser = properties.getProperty("genesisTxnUserID");
		String emailList = properties.getProperty("genesisEmailNotification");
		interfaceMfrSocketConfig = new InterfaceMgrSocketConfig();
		interfaceMfrSocketConfig.setHost(host);
		interfaceMfrSocketConfig.setPort(port);
		interfaceMfrSocketConfig.setTimeout(10000);
		interfaceMfrSocketConfig.setUser(secureId);
		this.txnUser = txnUser;
		this.emailList = emailList;
		}

	/**
	 * send machine information to genesis
	 * @param machine
	 * @param added
	 * @throws Exception
	 */
	public void saveMachine(Machine machine,boolean added) throws Exception
		{
		String method = null;
		String instance = null;
		String picAddress = "";
		if(added)
			{
			method = "addMachine";
			instance = properties.getProperty("genesis.addMachine.instance");
			}
		else
			{
			method= "updateMachineAttr";
			instance = properties.getProperty("genesis.updateMachineAttr.instance");
			}
		if(machine.getEquipmentType().getPicName()!=null)
			{
			picAddress = "PIC='"+StringUtils.trimToEmpty(machine.getEquipmentType().getPicName())+"',";
			}
		APIExecutor apiExecutor = new APIExecutor(interfaceMfrSocketConfig);
		String API = method+"(transUserId='" + txnUser + "',"
					+ "machId='" + machine.mESName + "',"
					+ "machName='" + machine.mESName + "',"
					+ "machType='" + machine.getEquipmentType().mesEquipmentType+ "'," 
					+ "department='" + machine.getAreaName() + "'," 
					+ "machPlatform='" + machine.getPlatform() +"'," 
					+ "machStatus='ACTIVE',"
					+ picAddress
					+ "machModel='" + machine.getModelName() + "')";
		logger.info("API:"+API+";instance:"+instance);
		try
			{
			apiExecutor.query(API,instance);
			if (!CommonUtilities.isEmpty(apiExecutor.getMessage()))
				{
				logErrors(apiExecutor.getMessage());
				}
			}
		catch (Exception e)
			{
			logger.error(e.getMessage(),e);
			throw e;
			}
		}
	/**
	 * update genesis machine state
	 * @param machine
	 * @throws Exception
	 */
	public void updateMachineStatus(Machine machine) throws Exception
		{
		APIExecutor apiExecutor = new APIExecutor(interfaceMfrSocketConfig);
		DurableStatus durableStatus = (DurableStatus) machine.context.get("durableStatus");
		String API = "machineStateChange(transUserId='" + txnUser + "',"
				+ "machId='" + machine.mESName + "',"
				+ "machState='" + durableStatus.STATE + "'," 
				+ "comments='" + durableStatus.SUBSTATE + "')";
		String instance = properties.getProperty("genesis.machineStateChange.instance");
		logger.info("API:"+API+";instance:"+instance);
		try
			{
			apiExecutor.query(API,instance);
			if (!CommonUtilities.isEmpty(apiExecutor.getMessage()))
				{
				logErrors(apiExecutor.getMessage());
				}
			}
		catch (Exception e)
			{
			logger.error(e.getMessage(),e);
			throw e;
			}
		}
	/**
	 * delete genesis machine
	 * @param machine
	 * @throws Exception
	 */
	public void deleteMachine(Machine machine) throws Exception
		{
		APIExecutor apiExecutor = new APIExecutor(interfaceMfrSocketConfig);
		String API = "updateMachineAttr(transUserId='" + txnUser + "',"
				+ "machId='" + machine.mESName + "',"
				+ "machStatus='RETIRED')";
		String instance = properties.getProperty("genesis.updateMachineAttr.instance");
		logger.info("API:"+API+";instance:"+instance);
		try
			{
			apiExecutor.query(API,instance);
			if (!CommonUtilities.isEmpty(apiExecutor.getMessage()))
				{
				logErrors(apiExecutor.getMessage());
				}
			}
		catch (Exception e)
			{
			logger.error(e.getMessage(),e);
			throw e;
			}
		}
	/**
	 * get machine model by machine type and platform
	 * @param machineType
	 * @param platform
	 * @return
	 * @throws Exception
	 */
	public List<String> getMachineModel(String machineType, String platform) throws Exception
		{
		String API = null;
		String instance = properties.getProperty("genesis.getMachineModel.instance");
		if (StringUtils.isNotEmpty(platform))
			{
			API = "getMachineModel(attributes='machModel',machType='" + machineType + "',machPlatform='" + platform + "')";
			}
		else
			{
			API = "getMachineModel(attributes='machModel',machType='" + machineType + "')";
			}
		logger.info("API:"+API+";instance:"+instance);
		
		APIExecutor apiExecutor = new APIExecutor(interfaceMfrSocketConfig);
		List<String> modelList = new ArrayList<String>();
		try
			{
			DataCollection returnData = apiExecutor.query(API,instance);
			if (!CommonUtilities.isEmpty(apiExecutor.getMessage()))
				{
				logErrors(apiExecutor.getMessage());
				}
			else
				{
				for (List<String> record : returnData)
					{
					String item = record.get(0);
					modelList.add(item);
					}
				}
			}
		catch (Exception e)
			{
			logger.error(e.getMessage(),e);
			throw e;
			}
		return modelList;
		}
	/**
	 * get loadboard handler and changekit configurations. table:LOADBOARD_HANDLER_CONFIG
	 * @param model
	 * @return
	 * @throws Exception
	 */
	public List<String> getLoadboardConfigurationMap(String model) throws Exception
		{
		List<String> childModelList = new ArrayList<String>();
		String API = "getLoadboardConfigurationMap(attributes='handlerType,changekitType',loadboardModel='"+model+"')";
		String instance = properties.getProperty("genesis.getLoadboardConfigurationMap.instance");
		logger.info("API:"+API+";instance:"+instance);
		APIExecutor apiExecutor = new APIExecutor(interfaceMfrSocketConfig);
		try
			{
			DataCollection returnData = apiExecutor.query(API,instance);
			if (!CommonUtilities.isEmpty(apiExecutor.getMessage()))
				{
				logErrors(apiExecutor.getMessage());
				}
			else
				{
				for (List<String> record : returnData)
					{
					String handlerType = record.get(0);
					String changekitType = record.get(1);
					String childModel = "handlerType="+handlerType+"|changekitType="+changekitType;
					childModelList.add(childModel);
					}
				}
			}
		catch (Exception e)
			{
			logger.error(e.getMessage(),e);
			throw e;
			}
		return childModelList;
		}
	/**
	 * get machine platform list for specific machine type
	 * @param machType
	 * @return
	 * @throws Exception
	 */
	public List<String> getMachinePlatform(String machType) throws Exception
		{
		String API = "getMachineModel(attributes='machPlatform',machType='" + machType + "')";
		String instance = properties.getProperty("genesis.getMachineModel.instance");
		logger.info("API:"+API+";instance:"+instance);
		List<String> platformList = new ArrayList<String>();
		APIExecutor apiExecutor = new APIExecutor(interfaceMfrSocketConfig);
		try
			{
			DataCollection returnData = apiExecutor.query(API,instance);
			if (!CommonUtilities.isEmpty(apiExecutor.getMessage()))
				{
				logErrors(apiExecutor.getMessage());
				}
			else
				{
				for (List<String> record : returnData)
					{
					String item = record.get(0);
					platformList.add(item);
					}
				}
			}
		catch (Exception e)
			{
			logger.error(e.getMessage(),e);
			throw e;
			}
		return platformList;
		}
	/**
	 * check if specific machId exists in genesis. table:machines
	 * @param machId
	 * @return
	 * @throws Exception
	 */
	public boolean isMachineExisting(String machId) throws Exception {
		String API = "getMachineAttributes(attributes='machId',machId='" + machId + "')";
		String instance = properties.getProperty("genesis.getMachineAttributes.instance");
		logger.info("API:"+API+";instance:"+instance);
		APIExecutor apiExecutor = new APIExecutor(interfaceMfrSocketConfig);
		boolean exist =  false;
		try
			{
			DataCollection returnData = apiExecutor.query(API,instance);
			if (!CommonUtilities.isEmpty(apiExecutor.getMessage()))
				{
				logErrors(apiExecutor.getMessage());
				}
			else
				{
				if(returnData!=null&&returnData.size()>0)
					{
					List<String> lt = returnData.get(0);
					if(lt!=null&&lt.size()>0)
						{
						String id = lt.get(0);
						if(id!=null&&id.equals(machId))
							{
							exist = true;
							}
						}
					}
				}
			}
		catch (Exception e)
			{
			logger.error(e.getMessage(),e);
			throw e;
			}
		return exist;
	}
	/**
	 * save model to genesis database. table: machine_model
	 * @param model
	 * @throws Exception
	 */
	public void saveModel(Model model) throws Exception
		{
		APIExecutor apiExecutor = new APIExecutor(interfaceMfrSocketConfig);
		String API = "setupMachineModel(transUserId='" + txnUser + "',"
				+ "machModel='" + model.getModelName() + "',"
				+ "machModelDesc='" + model.getModelDescription() + "',"
				+ "mfrCode='" + model.getMfrCode()+ "'," 
				+ "machPlatform='" + model.getPlatform() +"')";
		String instance = properties.getProperty("genesis.setupMachineModel.instance");
		logger.info("API:"+API+";instance:"+instance);
		try
			{
			apiExecutor.query(API,instance);
			if (!CommonUtilities.isEmpty(apiExecutor.getMessage()))
				{
				logErrors(apiExecutor.getMessage());
				}
			}
		catch (Exception e)
			{
			logger.error(e.getMessage(),e);
			throw e;
			}
		}
	/**
	 * As stated by genesis team, we cannot delete genesis model. So the method will be deprecated
	 * @param model
	 * @throws Exception
	 */
	@Deprecated
	public void deleteModel(Model model) throws Exception
		{
		APIExecutor apiExecutor = new APIExecutor(interfaceMfrSocketConfig);
		String API = "deleteMachineModel(transUserId='" + txnUser + "',"
				+ "machModel='" + model.getModelName() + "')";
		String instance = properties.getProperty("genesis.deleteMachineModel.instance");
		logger.info("API:"+API+";instance:"+instance);
		try
			{
			apiExecutor.query(API,instance);
			if (!CommonUtilities.isEmpty(apiExecutor.getMessage()))
				{
				logErrors(apiExecutor.getMessage());
				}
			}
		catch (Exception e)
			{
			logger.error(e.getMessage(),e);
			throw e;
			}
		}
	/**
	 * check if genesis has specific machine model
	 * @param machModel
	 * @param mfrCode
	 * @return
	 * @throws Exception
	 */
	public boolean isModelExisting(String machModel,String mfrCode) throws Exception
		{
		String API = "getMachineModelAttributes(attributes='machModel',machModel='" + machModel + "',mfrCode='"+mfrCode+"')";
		String instance = properties.getProperty("genesis.getMachineModelAttributes.instance");
		logger.info("API:"+API+";instance:"+instance);
		APIExecutor apiExecutor = new APIExecutor(interfaceMfrSocketConfig);
		boolean exist =  false;
		try
			{
			DataCollection returnData = apiExecutor.query(API,instance);
			if (!CommonUtilities.isEmpty(apiExecutor.getMessage()))
				{
				logErrors(apiExecutor.getMessage());
				}
			else
				{
				if(returnData!=null&&returnData.size()>0)
					{
					List<String> lt = returnData.get(0);
					if(lt!=null&&lt.size()>0)
						{
						String id = lt.get(0);
						if(id!=null&&id.equals(machModel))
							{
							exist = true;
							}
						}
					}
				}
			}
		catch (Exception e)
			{
			logger.error(e.getMessage(),e);
			throw e;
			}
		return exist;
		}
	/**
	 * check if genesis have the combination of specific machine type and machine model
	 * @param machType
	 * @param machModel
	 * @return
	 * @throws Exception
	 */
	public boolean isMachTypeModelCombExisting(String machType,String machModel) throws Exception
		{
		String API = "getMachTypeModelCombinations(attributes='machModel',machModel='" + machModel + "',machType='"+machType+"')";
		String instance = properties.getProperty("genesis.getMachTypeModelCombinations.instance");
		logger.info("API:"+API+";instance:"+instance);
		APIExecutor apiExecutor = new APIExecutor(interfaceMfrSocketConfig);
		boolean exist =  false;
		try
			{
			DataCollection returnData = apiExecutor.query(API,instance);
			if (!CommonUtilities.isEmpty(apiExecutor.getMessage()))
				{
				logErrors(apiExecutor.getMessage());
				}
			else
				{
				if(returnData!=null&&returnData.size()>0)
					{
					List<String> lt = returnData.get(0);
					if(lt!=null&&lt.size()>0)
						{
						String id = lt.get(0);
						if(id!=null&&id.equals(machModel))
							{
							exist = true;
							}
						}
					}
				}
			}
		catch (Exception e)
			{
			logger.error(e.getMessage(),e);
			throw e;
			}
		return exist;
		}
	/**
	 * add genesis machine type and model name combination. genesis table name: MACH_TYPE_MODEL_COMBINATIONS
	 * @param machType
	 * @param machModel
	 * @return
	 * @throws Exception
	 */
	public boolean addMachTypeModelComb(String machType,String machModel) throws Exception
		{
		String API = "addMachTypeModelComb(transUserId='"+txnUser+"',machType='"+machType+"',machModel='" + machModel + "')";
		String instance = properties.getProperty("genesis.addMachTypeModelComb.instance");
		logger.info("API:"+API+";instance:"+instance);
		APIExecutor apiExecutor = new APIExecutor(interfaceMfrSocketConfig);
		boolean exist =  false;
		try
			{
			apiExecutor.query(API,instance);
			if (!CommonUtilities.isEmpty(apiExecutor.getMessage()))
				{
				logErrors(apiExecutor.getMessage());
				}
			}
		catch (Exception e)
			{
			logger.error(e.getMessage(),e);
			throw e;
			}
		return exist;
		}
	/**
	 * delete machine type and model combination
	 * @param machType
	 * @param machModel
	 * @return
	 * @throws Exception
	 */
	public boolean deleteMachTypeModelComb(String machType,String machModel) throws Exception
		{
		String API = "deleteMachTypeModelComb(transUserId='"+txnUser+"',machType='"+machType+"',machModel='" + machModel + "')";
		String instance = properties.getProperty("genesis.deleteMachTypeModelComb.instance");
		logger.info("API:"+API+";instance:"+instance);
		APIExecutor apiExecutor = new APIExecutor(interfaceMfrSocketConfig);
		boolean exist =  false;
		try
			{
			apiExecutor.query(API,instance);
			if (!CommonUtilities.isEmpty(apiExecutor.getMessage()))
				{
				logErrors(apiExecutor.getMessage());
				}
			}
		catch (Exception e)
			{
			logger.error(e.getMessage(),e);
			throw e;
			}
		return exist;
		}
		
	public void help(String function)
		{
		String API = "help('" + function + "')";
		try
			{
			APIExecutor apiExecutor = new APIExecutor(interfaceMfrSocketConfig);
			DataCollection returnData = apiExecutor.query(API);
			if (!CommonUtilities.isEmpty(apiExecutor.getMessage()))
				{
				logErrors(apiExecutor.getMessage());
				}
			else
				{
				for (List<String> record : returnData)
					{
					System.out.println(StringUtils.join(record, ","));
					}
				}
			}
		catch (Exception e)
			{
			logger.error(e.getMessage(),e);
			}
		}

	public void logErrors(String message) throws Exception
		{
		logger.error(message);
		sendEmailNotification(message);
		throw new Exception(message);
		}
	

	public void sendEmailNotification(String message)
		{
		DurableEmail email = new DurableEmail();
		email.setTo(emailList);
		email.setSubject("TTT:Genesis API unavailable");
		email.setType("html");
		email.setContent(message);
		new Thread(email).start();
		}

	}
