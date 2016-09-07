package com.freescale.stateStamper.model.logic;

import com.freescale.sc.model.entity.event.*;
import com.freescale.sc.model.entity.event.Event;
import com.freescale.serverUtil.PersistenceBrokerIterator;
import com.freescale.serverUtil.Persistence;
import com.freescale.serverUtil.ThreadSafeSimpleDateFormat;
import com.freescale.stateStamper.model.entity.*;
import com.freescale.stateStamper.util.TTTUtil;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.text.*;
import java.util.*;

import oracle.jdbc.driver.OracleDriver;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.ojb.broker.metadata.JdbcConnectionDescriptor;
import org.apache.ojb.broker.metadata.MetadataManager;
import org.exolab.castor.xml.Marshaller;
import org.exolab.castor.xml.XMLContext;

import java.util.concurrent.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * Perform the state change according to equipment event ID. Update state change information in hash table for each equipment
 */
public class StateTransition extends Thread implements IStateTransition,TransactionMemorySyncMaster
	{
	private static Map machines;
	private static Hashtable eiToMESName = new Hashtable(), states = new Hashtable(), performance = new Hashtable();
	private Map<Integer,String> pkToMESName = new HashMap<Integer,String>();
	private static Logger logger;
	private static Logger sites;
	private String[] contexts, summings, performances;
	private int idleInterval, writeInterval, counter;
	private String  baseDirectory, diamondDirectory,defaultEquipmentTypeName;
	private Summary summary;
	private XMLContext xMLContext;
	private ThreadSafeDMHConnection DMHConnection;
	private ThreadSafeServerCall promisConnection;
	private String promisUser, promisPassword;
	private DecimalFormat decimal = new DecimalFormat("##0.0");
	private ThreadSafeSimpleDateFormat formatter = new ThreadSafeSimpleDateFormat("yyyy-MM-dd HH:mm:ss"), formatterGMT = new ThreadSafeSimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
	private boolean run, mesPrecedence,eprPrecedence;
	private Map<String,DMH> dmhs;
	private static Map<Integer,EquipmentType> equipmentTypes = new ConcurrentHashMap<Integer,EquipmentType>(10,0.9f,1);
	private Map childNames = new ConcurrentHashMap<Integer,EquipmentType>(10,0.9f,1);
	private String header;	
	private Boolean isProduction;
	private String appUrl;
	private ExecutorService StatusSenderExecutor;
	private ExecutorService biznessRuleUpdateExecutor;
	private ExternalDBSource externalDBSource;
	private String stateChangeTesterID;
	private EquipmentType testerEquipmentType;
	private String sitesFilterByTestProgramFlowId;
	private Map<String, String> siteFilterMap = new HashMap<String, String>();
	private List<String> siteFilterArrList = new ArrayList<String>();
	private boolean serverTime = true;
	private boolean clientAcknowledgment = false;
    private Map<String,Integer> columnsLength = new ConcurrentHashMap<String,Integer>(100,0.9f,1);
	private static final ThreadSafeSimpleDateFormat dateFormat = new ThreadSafeSimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private static Map<Integer,Area> areas;  
	/**
	 * Creates an initializes a state transition object
	 * @param summary The summary object
	 * @param dataDirectory Directory where data files are stored
	 * @param contexts String array of context variables
	 * @param summings String array of summing variables
	 * @param idleInterval Time interval in seconds after which a machine in Productive state will go to Standby if it has not received any messages
	 * @param writeInterval Time interval in seconds after which a record will be written to the raw file if a machine has not changed state of context
	 * @param baseDirectory Directory to store temporary files
	 * @param marshaller The marshaller object to create XML files
	 * @param diamondDirectory Directory where to store files for Diamond
	 * @param DMHConnection Connection to the PIC
	 * @param promisConnection Promis connection or null if not used
	 * @param promisUser The Promis user ID
	 * @param promisPassword The Promis password
	 */

	public StateTransition(Summary summary, String[] contexts, String[] summings,String[] performances,int idleInterval, int writeInterval, boolean mesPrecedence,boolean eprPrecedence, String baseDirectory, String diamondDirectory, XMLContext xMLContext, ThreadSafeDMHConnection DMHConnection, ThreadSafeServerCall promisConnection, String promisUser, String promisPassword, Boolean isProduction,String defEquipmentType, String stateChangeTesterID) throws Throwable
		{
		this.summary = summary;
		this.contexts = contexts;
		this.summings = summings;
		this.performances = performances;
		this.idleInterval = idleInterval;
		this.writeInterval = writeInterval;
		this.mesPrecedence = mesPrecedence;
		this.eprPrecedence = eprPrecedence;
		this.baseDirectory = baseDirectory;
		this.diamondDirectory = diamondDirectory;
		this.xMLContext = xMLContext;
		this.DMHConnection = DMHConnection;
		this.promisConnection = promisConnection;
		this.promisUser = promisUser;
		this.promisPassword = promisPassword;
		this.dmhs = new HashMap();
		this.isProduction = isProduction;
		this.stateChangeTesterID = stateChangeTesterID;
		defaultEquipmentTypeName = defEquipmentType;
		header=getHeader();
		logger = LoggerFactory.getLogger("stateStamper");
		sites = LoggerFactory.getLogger("sitesFilter");
		formatterGMT.setTimeZone(TimeZone.getTimeZone("GMT"));
		setEquipmentTypes(Persistence.search(new EquipmentType()));
		testerEquipmentType = getEquipmentTypeByName("Tester");
		Vector auxMachines = Persistence.search(new Machine());
		machines= new ConcurrentHashMap(auxMachines.size(),0.9f,2);
		//non-existing machines will be removed from machines, tester status will be reloaded
		setMachines(auxMachines,true,null,true);
		setStates(Persistence.search(new Substate()));
		int coreSize = 20;
		int testerSize = getMachines(testerEquipmentType).size();
		if(testerSize < coreSize)testerSize = coreSize;
		StatusSenderExecutor = new ThreadPoolExecutor(coreSize, testerSize, 10000l, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
		biznessRuleUpdateExecutor = new ThreadPoolExecutor(coreSize, testerSize, 10000l, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
		initColumnsLength();
		update();
		run = true;
		start();
		}
	
	/**
	 * Initialize a map that has the column names of the status table as key and the length of the field in the DB as value
	 * This map is used to check the length when a event is received, if the length of a context variable exceeds the length 
	 * it is truncated to fit the length in the DB. 
	 * */
	public void initColumnsLength()
		{
		try
			{
			MetadataManager mm = MetadataManager.getInstance();
			List<JdbcConnectionDescriptor> connections = mm.connectionRepository().getAllDescriptor();
			for (JdbcConnectionDescriptor connDesc : connections)
				{
				String connectionAlias = "jdbc:oracle:thin:" + connDesc.getDbAlias();
				DriverManager.registerDriver(new OracleDriver());
				Connection connection = DriverManager.getConnection(connectionAlias, connDesc.getUserName(),connDesc.getPassWord());
				DatabaseMetaData databaseMetaData = connection.getMetaData();
				String catalog = null, schemaPattern = null, tableNamePattern = "STATUS", columnNamePattern = null;
				ResultSet rs = databaseMetaData.getColumns(catalog, schemaPattern, tableNamePattern,columnNamePattern);
				while (rs.next())
					{
					String columName = rs.getString("COLUMN_NAME");
					Integer columnSize = Integer.valueOf(rs.getInt("COLUMN_SIZE"));
					String typeName = rs.getString("TYPE_NAME");
					if(typeName.startsWith("VARCHAR"))
						{
						columnsLength.put(columName, columnSize);
						}
					}
				rs.close();
				connection.close();
				}
			}
		catch (Throwable t)
			{
			t.printStackTrace();
			}

		}
	
	public StateTransition setExternalDBSource(ExternalDBSource extDBSource)
		{
		externalDBSource = extDBSource; 
		return this;
		}

	/**
	 * set the equipmentTypes from a vector of types returned by Persistence.search
	 * */
	public void setEquipmentTypes(Vector types)
		{
		if(types ==null) return;
		equipmentTypes.clear();
		EquipmentType type;
		for (Object object : types)
			{
			type= (EquipmentType)object;
			equipmentTypes.put(type.primaryKey, type);
			}
		setChildNames(equipmentTypes);
		}
	
	private void setChildNames(Map equipmentTypes)
		{
		Iterator it= equipmentTypes.values().iterator();
		EquipmentType type;
		childNames.clear();
		while(it.hasNext())
			{
			type=(EquipmentType)it.next();
			String name = type.name;
			if(name.contains(" "))
				{
				name = name.substring(0,name.indexOf(" "));
				}
			name = name.toUpperCase()+"_ID";
			childNames.put(name,name);
			}
		}
	/**
	 * return the equipmentTypes defined
	 * */
	public Map<Integer,EquipmentType> getEquipmentTypes()
		{
		return equipmentTypes;
		}
	
	public static EquipmentType getEquipmentType(Integer equipmentTypePk)
		{
		return equipmentTypes.get(equipmentTypePk);
		}
	
	public EquipmentType getEquipmentTypeByName(String name)
		{
		EquipmentType eqType=null;
		Collection eqTypes = equipmentTypes.values();
		Iterator it = eqTypes.iterator() ;
		boolean found=false;
		while(it.hasNext()&& !found )
			{
			 eqType = (EquipmentType)it.next();
			 if(eqType.name.equalsIgnoreCase(name)){found=true;}
			}
		if(!found){eqType=null;}
		return eqType;
		}
	/**
	 * get area object by name
	 * @param name area name
	 * @return area object
	 */
	@SuppressWarnings("unchecked")
	public Area getAreaByName(String name)
		{
		Area result = null;
		List<Area> list;
		try
			{
			list = (List<Area>) Persistence.search(new Area());
			for (Area area : list)
				{
				if (StringUtils.equals(area.getName(), name))
					{
					result = area;
					break;
					}
				}
			}
		catch (Throwable e)
			{
			logger.error("Error with Area "+ name,e);
			}
		return result;
		}
	
	/**
	 * Gets the type for a machine given its name
	 * @param name The machine name
	 * @return The machine type
	 */
	public static String getType(String name)
		{
		String type = null;
		Machine machine = (Machine)machines.get(name);
		if (machine != null) type = machine.type;
		if(type == null || type.equals("") )type = "UNKNOWN";
		return type;
		}
	
	/**
	 * get equipment area_pk by equipment name
	 * @param name
	 * @return
	 */
	public static String getAreaPK(String name)
		{
		Machine machine = (Machine)machines.get(name);
		if (machine != null && machine.getArea_pk()!=null) 
			{
			return machine.getArea_pk().toString();
			}
		else return null;
		}
	/**
	 * get user full name by user core id
	 * @param name core id
	 * @return full name
	 */
	public static String getUserFullName(String name) 
		{
		String result = name;
		try
			{
			String tmp = TTTUtil.getUserFullName(name);
			if(StringUtils.isNotEmpty(tmp))
				{
				result = tmp;
				}
			}
		catch (Throwable e)
			{
			logger.error("Error with User "+ name ,e);
			}
		return result;
		}
	
	/**
	 * Gets all the machines for a list of types and locations
	 * @param types An array of types
	 * @param An array of locations
	 * @return A hash table with all the machines
	 */
	public static Map getMachines(String[] types, String[] locations)
		{
		Map result = new HashMap();
		Map typesHash = new HashMap();
		Map locationsHash = new HashMap();
		for (int i=0; i<types.length; i++) typesHash.put(types[i],"");
		for (int i=0; i<locations.length; i++) locationsHash.put(locations[i],"");
		Set<String> midSet = machines.keySet();
		for(String mid:midSet)
			{
			Machine machine = (Machine)machines.get(mid);
			if (typesHash.get(machine.type)!=null || locationsHash.get(machine.location)!=null) result.put(machine.mESName,machine);
			}
		return result;
		}
	
	/**
	 * Gets the location for a machine given its name
	 * @param name The machine location
	 * @return The machine type
	 */
	public static String getLocation(String name)
		{
		Machine machine = (Machine)machines.get(name);
		if (machine != null) return machine.location;
		else return null;
		}
	
	/**
	 * Gets the platform for a machine given its name
	 * @param name The machine platform
	 * @return The machine type
	 */
	public static String getPlatform(String name)
		{
		Machine machine = (Machine)machines.get(name);
		if (machine != null) return machine.platform;
		else return null;
		}

	/**
	 * Returns the group for the given substate
	 * @param substate The substate
	 * @return The group
	 */
	public static String getGroup(String substate)
		{
		Substate auxSubstate = (Substate)states.get(substate);
		if (auxSubstate != null) return auxSubstate.group;
		else return " ";
		}
	
	/**
	 * Returns the color for the given sub state
	 * @param substate The sub state
	 * @return The color
	 */
	public static String getColor(String substate)
		{
		Substate state = (Substate)states.get(substate);
		if (state != null) return state.color;
		else return null;
		}
	
	/**
	 * Returns sub states, groups and colors
	 * @return A hash table with sub states, groups and colors
	 */
	public static Hashtable getColors() {return states;}
	
	private boolean filterSitesByTestProgramFlowId(Machine machine,EquipmentEvent equipmentEvent) 
	   {
		boolean changed = false;
		if(sitesFilterByTestProgramFlowId == null ||   !machine.getEquipmentType().name.equalsIgnoreCase("Tester"))return changed;
		String testProgConf = null;
		Map<String, String> values = toHashtable(equipmentEvent.contexts, true);
		String mesName = machine.mESName;
		String testProg = (String) values.get("TEST_PROGRAM");
		String flow = (String) values.get("FLOW_ID");
		String newSubState = (String) values.get("SUBSTATE");
		String siteAvailable = (String) values.get("SITES_AVAILABLE");
		boolean variablesValid  = newSubState != null && (newSubState.equalsIgnoreCase("Test") || newSubState.equalsIgnoreCase("Retest")) && 
				                   testProg != null    && testProg.contains(".xls") && testProg.contains("_") && 
				                   flow != null;
		if (variablesValid)
			{
			int subValue1 = testProg.lastIndexOf("_");
			int subValue2 = testProg.lastIndexOf(".xls") + 4;
			String subString = testProg.substring(subValue1, subValue2);
			testProgConf = testProg.replace(subString, "");
			boolean testProgramFound = siteFilterArrList.contains(testProgConf);
			if (testProgramFound) 
			   { // only met test_program is allowed to proceed
				String newSitesAvailable = (String) siteFilterMap.get(testProgConf + "_" + flow);
				List<Context> contexts = equipmentEvent.contexts;
				for (int i = 0; i < contexts.size(); i++)
					{
					if (contexts.get(i).name.equals("SITES_AVAILABLE"))
						{
						changed = true;
						contexts.get(i).value = newSitesAvailable;
						sites.info("MES_NAME:" + mesName + "| TEST_PROGRAM:" + testProg + "| FLOW_ID:" + flow
								+ "| SITE_AVAILABLE:" + siteAvailable + "| SITE_AVAILABLE_CHANGE:" + newSitesAvailable);
					   }
				   }
			   }
			}
		return changed;
	 }
	
	private boolean filterStateChange(Machine machine, EquipmentEvent equipmentEvent)
		{
		boolean changed = false;
		if(stateChangeTesterID == null || stateChangeTesterID.equals("") || !machine.getEquipmentType().name.equalsIgnoreCase("Tester"))return changed;
		String[] arr = stateChangeTesterID.split(",");
		Set<String> tester = new HashSet<String>(Arrays.asList(arr));
		if (tester.contains("all") || tester.contains(machine.getMESName()))
			{
			String oldSubState = (String) machine.context.get("SUBSTATE");
			Map<String, String> values = toHashtable(equipmentEvent.contexts, true);
			String newSubState = (String) values.get("SUBSTATE");
			boolean oldSubstateToFilter = oldSubState != null && (oldSubState.equalsIgnoreCase("Test")    || oldSubState.equalsIgnoreCase("Retest")	|| 
							              oldSubState.equalsIgnoreCase("QC Test") || oldSubState.equalsIgnoreCase("EE Test"));
			boolean newSubstateToFilter = newSubState != null && (newSubState.equalsIgnoreCase("Parts are Soaking") || newSubState.equalsIgnoreCase("Alarm Recovery"));
			if (oldSubstateToFilter && newSubstateToFilter)
				{
				equipmentEvent.state = "Productive";
				List<Context> v = equipmentEvent.contexts;
				for (int i = 0; i < v.size(); i++)
					{
					if (v.get(i).name.equals("SUBSTATE"))
						{
						v.get(i).value = oldSubState;
						Context context = v.get(i);
						equipmentEvent.contexts.set(i, context);
						}
					if (contexts[i].toString().equals("SUBSTATE"))
						{
						machine.context.remove(contexts[i]);
						}
					}
				changed = true;
				}
			}
		return changed;
		}

	/**
	 * called to process event from outside of TTT
	 */
	
	public void event(EquipmentEvent equipmentEvent)
		{
		event(equipmentEvent, true);
		}
	
	/**
	 * Update java classes with new changed state
	 */
	private void event(EquipmentEvent equipmentEvent,  boolean isFromClient)
		{
		try
			{
			if (!setNameTypeLocation(equipmentEvent.equipment)) return; // Ignore events for machines not defined in State Stamper
			Date eventTime;
			if(isServerTime()){eventTime = new Date();}
			else              {eventTime = getEventTime(equipmentEvent);}
			logger.debug("StateTransition.event()\tequipmentEvent.startTime\t" + equipmentEvent.startTime +"\tequipmentEvent.event.time\t"+ equipmentEvent.event.time+"\teventTime\t"+ eventTime);
			equipmentEvent.setTime(eventTime); // Set event time to avoid problems with computers not being synchronized
			// Process event
	 		Machine machine = (Machine)machines.get(equipmentEvent.equipment.id);
	 		synchronized (machine)
		 		{
		 		String name = machine.mESName;
				String oldState = machine.state;
				Map oldContext = (Map)((HashMap)machine.context).clone();
			  	if (oldState == null) oldState = "Unknown";
				int stateDuration = (int)(eventTime.getTime()-machine.stateStart.getTime())/1000;
				boolean autoWrite = stateDuration >= writeInterval;
				boolean stateChanged = !machine.state.equals(equipmentEvent.state);
		
			  	Hashtable values = toHashtable(equipmentEvent.contexts,true);
				String newSubState = (String)values.get("SUBSTATE");
			  	String oldSubState = (String)machine.context.get("SUBSTATE"); 	
			  	
			  	// If MES events should take precedence, ignore non MES events if the last event came from MES and last state is Engineering, Scheduled Down, Unscheduled Down or Non Scheduled 
				if (mesPrecedence)
					{
					String source = (String)values.get("SOURCE");
					boolean fromMES = false;
					if (source != null && source.equals("MES")) fromMES = true;
					if (machine.mesEvent && !fromMES && (oldState.equals("Engineering") || oldState.equals("Scheduled Down") || oldState.equals("Unscheduled Down") || oldState.equals("Non Scheduled"))) return;
					machine.mesEvent = fromMES;
					}
				if (machine.stateStart == null) machine.stateStart = eventTime;
				if (machine.eventStart == null) machine.eventStart = eventTime;
				
				transition(equipmentEvent,oldState);
				
				boolean soakingAlarmStateChanged = filterStateChange(machine, equipmentEvent);
				filterSitesByTestProgramFlowId(machine, equipmentEvent);
				machine.state = equipmentEvent.state;
				
				boolean contextChanged = updateContexts(machine.context,equipmentEvent.contexts);
				boolean newStatus = stateChanged || contextChanged || autoWrite;
				boolean updateStatus = !newStatus && isFromClient;
			    String crew;
			  	Hashtable newSumming = toHashtable(equipmentEvent.summings,false);
			  	if(newStatus)
			  		{
			  		machine.stateStart = eventTime;
			  		machine.summing.clear();
			  		}
			  	machine.eventStart = eventTime;
			  	int duration = (int)(eventTime.getTime()-machine.stateStart.getTime());  // Duration is in milliseconds
				if("PIC".equals((String)values.get("SOURCE")))processPICMessage(machine, values, newSumming);
				updateSummings(machine.summing,duration,1,newSumming); // Update summing variables
				
				if (soakingAlarmStateChanged)
					{
					if (newSubState != null && (newSubState.equals("Alarm Recovery") && (oldSubState.equals("Test"))))
						{
						Integer testTime = Integer.valueOf(0);
						try{testTime = (Integer)machine.summing.get("TEST_TIME");}catch(Throwable t){logger.error("Error with "+name ,t);}
						Integer hndIndexTime =  Integer.valueOf(duration-testTime.intValue());
						if(hndIndexTime.intValue() <0){hndIndexTime = Integer.valueOf(0);}
						machine.summing.put("HND_INDEX_TIME", hndIndexTime);
						}
					}
			  	
			  	Map clientPerformance = getPerformance(performances, toHashtable(equipmentEvent.contexts, true) , newSumming);
			  	if(performance.get(machine.mESName) !=null){((Performance)performance.get(machine.mESName)).update(machine,eventTime,oldContext,newSumming,clientPerformance);} // Update performance variables
			  	if ( newStatus && isProduction)		  		
			  		{
					crew = FiscalDate.getCrew(new FiscalDate(FiscalDate.SHIFT,formatter.format(eventTime)));			
					machine.context.put("CREW", crew);
					Status currentStatus = getUpdateStatusSummings(machine.getCurrentStatus(),eventTime,newSumming);
					String startState = (String)currentStatus.get("TIME");
					Date startDate = formatter.parse(startState);
					long oldDuration = eventTime.getTime() - startDate.getTime() ;
					if(oldDuration < 0){oldDuration = 0;}
					currentStatus.set("DURATION",String.valueOf(oldDuration) );
					
					if (newSubState != null) {
						if (newSubState.equals(oldSubState)) {
							boolean exists = machine.context
									.containsKey("LAST_STATE_TIME");
							if (!exists)
								machine.context.put("LAST_STATE_TIME",
										startState);
						} else {
							String eventTimeStamp = dateFormat
									.format(eventTime);
							machine.context.put("LAST_STATE_TIME",
									eventTimeStamp);
						}
					} else {
						boolean exists = machine.context
								.containsKey("LAST_STATE_TIME");
						if (!exists)
							machine.context.put("LAST_STATE_TIME",
									startState);
					}
					
					Persistence.store(currentStatus);
					String rec = name+"\t" + formatter.format(machine.stateStart) + "\t"+machine.state+ "\t" + hashToString(machine.context,true)+"\t"+hashToString(new Hashtable(),false);
			  		write(rec);
			  		try  {
			  			IEquipmentEvent tmpequipmentEvent = toEquipmentEvent(name,equipmentEvent.event.id,formatterGMT.format(machine.stateStart),oldState,oldContext,machine.summing);
			  			StatusSendTask tmpSendTask = new StatusSendTask(machine,this,tmpequipmentEvent,equipmentEvent.state,oldState, oldSubState, newSubState);
			  			StatusSenderExecutor.execute(tmpSendTask);
			  		} 
			  		catch (Throwable throwable) {
			  			logger.error("Error with " + name + "on sendingEvent ",throwable);
			  			}
			  		machine.stateStart = eventTime;  // Set state start time
			  		logger.debug("new status saved for machine\t" + machine.mESName + "\tat\t" + new Date().getTime());
			  		}
			  	else if (updateStatus)
			  		{
			  		Status updatedSummings = getUpdateStatusSummings(machine.getCurrentStatus(),eventTime,newSumming);
			  		Persistence.store(updatedSummings);
			  		machine.setCurrentStatus(updatedSummings);
			  		logger.debug("updated status for machine\t" + machine.mESName+ "\tat\t" + new Date().getTime());
			  		}
			  	if(isFromClient)
			  		{
			  		String latestEvent = name+"\t" + formatter.format(machine.stateStart) + "\t"+machine.state+ "\t" + hashToString(machine.context,true)+"\t"+hashToString(newSumming,false);
					BussinessRuleUpdateTask tmpEPRTask = new BussinessRuleUpdateTask(machine,this,latestEvent);
		  		    biznessRuleUpdateExecutor.execute(tmpEPRTask);
			  		}
				}
			}
		catch (Throwable throwable) {logger.error("Error on Machine "+ equipmentEvent.equipment.id ,throwable);
			}
		}

	private Status getUpdateStatusSummings(Status status,Date eventTime,Map newSumming)
		{
		Integer valueAux;
		for (int i=0; i<summings.length; i++)
			{
			String tmp = (String)status.get(summings[i]);
			try{valueAux = Integer.valueOf(tmp);}
			catch(NumberFormatException e){valueAux = Integer.valueOf(0);}
			int value = valueAux==null?0:valueAux.intValue();
			if (summings[i].equals("DURATION")) {
			String startState = (String)status.get("TIME");
			Date startDate=null;
			try	{startDate = formatter.parse(startState);}
			catch (ParseException e){e.printStackTrace();}
			long oldDuration = eventTime.getTime() - startDate.getTime() ;
			status.set("DURATION",String.valueOf(oldDuration) );
			}
			else if (summings[i].equals("COUNT")) value += 1;
			else
				{
				if (summings[i].contains("$"))valueAux = getValue(newSumming,summings[i]);
				else valueAux = (Integer)newSumming.get(summings[i]);
				value += valueAux==null?0:valueAux.intValue();		
				}
			status.set(summings[i],new Integer(value));
			}
		return status;
		}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void processPICMessage(Machine machine, Hashtable values, Hashtable newSumming)
		{
		machine.context.put("SETUP_SOURCE", "PIC");
		Machine childLoadBoard = null;
		List<String> loadBoardList = machine.getChildrenByEquipmentTypeIgnoreCase("loadboard");
		for(String mid:loadBoardList)
			{
			childLoadBoard = (Machine)machines.get(mid);
			if(childLoadBoard!=null)break;
		}
		if(childLoadBoard==null)return;
		
		machine.context.put("LOADBOARD_ID", childLoadBoard.mESName);
		String siteNumber = (String)childLoadBoard.get("SITES_NUMBER");
		int siteNumberInteger = NumberUtils.isNumber(siteNumber)?Integer.parseInt(siteNumber):1;
		machine.context.put("SITES_AVAILABLE", String.valueOf(siteNumberInteger));

		Integer cntGood = (Integer)newSumming.get("CNT_GOOD");
		Integer deviceProcessed = (Integer)newSumming.get("DEVICES_PROCESSED");
		if(cntGood!=null&&deviceProcessed!=null)
			{
			int goodCount = (cntGood!=null?cntGood.intValue():deviceProcessed.intValue());
			int totalCount = (deviceProcessed!=null?deviceProcessed.intValue():cntGood.intValue());
			int[] goodArray = calculateTouchDown(goodCount,siteNumberInteger);
			int[] totalArray = calculateTouchDown(totalCount,siteNumberInteger);
			for(int i=0;i<siteNumberInteger;i++)
				{
				newSumming.put("SITE"+i+"_HB1", goodArray[i]);
				newSumming.put("SITE"+i+"_HB$", totalArray[i]-goodArray[i]);
				}
			}
		}
	
	private int[] calculateTouchDown(int total, int siteCount)
		{
		int[] array = new int[siteCount];
		int remainder = total % siteCount;
		int average = (total - remainder) / siteCount;
		for (int i = 0; i < siteCount; i++)
			{
			array[i] = average;
			if (remainder > 0)
				{
				array[i] = average + 1;
				remainder--;
				}
			}
		return array;
		}
	
	public void executeBusinessRules(Machine machine, String latestEvent) {
		try {
			Status status = getStatus(latestEvent);
			Map durables = getChildren(status);
			durables.putAll(getUpdateMultiple(machine, status));
			Iterator it = durables.values().iterator();
			while (it.hasNext()) {
				Machine durable = (Machine) it.next();
				executeEPRRules(durable, status);
			}
		} catch (Throwable th) {logger.error("executeBusinessRules\t"+machine.eIName + "\t"+latestEvent,th);}
	}
	
	
	private Map getUpdateMultiple(Machine tester,IStatus status)
		{
		Map result =  new HashMap();
		EquipmentType eqType = tester.getEquipmentType();
		List  list = eqType.getBusinessRulesBy(BusinessRule.EPR_EVENT);
		List actions = new ArrayList();
		for(int i = 0; i< list.size();i++)
			{
			BusinessRule rule = (BusinessRule)list.get(i);
			actions.addAll( rule.getActionsBy(BusinessRuleAction.UPDATE_MULTIPLE));
			}
		for(int i = 0;i< actions.size();i++)
			{
			BusinessRuleAction action = (BusinessRuleAction)actions.get(i);
			Map<String, String> param = action.getActionParameters();
			String eqTypeName = param.get("equipment_type");
			String idVar = param.get("id_variable");
			String incVar = param.get("increment_variable");
			String prefixId = ""+param.get("prefix_id");
			int limit = Integer.valueOf(param.get("limit")).intValue();
			Object obj = getEquipmentTypeByName(eqTypeName),objTmp=null;
			if(obj==null){continue;}
			String dynName,dynInc,prefixVal;
			Machine durable;
			if(tester.has(prefixId)){objTmp = tester.get(prefixId);}
			if(objTmp != null) {prefixVal = objTmp.toString();}
			else               {prefixVal = "";}
			String durableId;
			for(int j = 0;j<= limit;j++)
				{
				dynName = idVar.replace("?",j+"");
				dynInc = incVar.replace("?",j+"");
				boolean defined = tester.context.containsKey(dynName);
				durableId = prefixVal + "_" + tester.context.get(dynName);
				durable = getMachine(durableId);
				if( defined && durable != null )
					{
					durable.context.put("ID_VARIABLE",dynName);
					durable.context.put("INC_VARIABLE",dynInc);
					result.put(durable.mESName,durable);
					}
				}
			}
		return result;
		}
	
	/** Destroy this object */
	public void destroy()
		{
		run = false;
		Iterator it=null;
		try
			{
			if (machines != null && machines.values() != null) it = machines.values().iterator();
			if (it.hasNext())
				{
				removeDMHinstance((Machine) it.next());
				}
			if( externalDBSource != null){externalDBSource.close();}
			StatusSenderExecutor.shutdown();
			biznessRuleUpdateExecutor.shutdown();
			logger.info(" Status executors shutdown? " + StatusSenderExecutor.isShutdown());
			logger.info(" Status executors terminated? " + StatusSenderExecutor.isTerminated());
			logger.info(" BizRule executors shutdown? " + biznessRuleUpdateExecutor.isShutdown());
			logger.info(" BizRule executors terminated? " + biznessRuleUpdateExecutor.isTerminated());
			}
		catch (Throwable t)
			{
			logger.error(t.getMessage(),t);
			}
		finally
			{
			interrupt();
			}
		
		}
	
	/**
	 * Run method for this thread. Change state to Standby if it was in Production and did not receive an event in the last idle interval.
	 * Sends events at the end of the shift and clear UOEE. Write events to raw file and run summary if needed.
	 */
	public void run()
		{
		Date last = new Date();
		Map monitorExecution = new HashMap();
		while (run)
			{
			try
				{
				Date now = new Date();
				String crew = "";
				boolean endOfShift = summary.getShift().endTime.compareTo(formatter.format(now)) < 0;
				if (endOfShift) crew = FiscalDate.getCrew(new FiscalDate(FiscalDate.SHIFT,formatter.format(now)));  // If end of shift, get crew for next shift
				Map testers = getMachines(testerEquipmentType);
				Iterator keys = testers.keySet().iterator();
				while (keys.hasNext())
					{
					String name = (String)keys.next();
					Machine machine = (Machine)machines.get(name);
					if (machine.stateStart != null && machine.eventStart != null)
						{
						int eventDuration = (int)(now.getTime()-machine.eventStart.getTime())/1000;
						logger.debug("Machine\t" + machine.mESName + "\tstate\t"+ machine.state + "\tsubstate\t"+ machine.context.get("SUBSTATE")+ "\teventStart\t"+ machine.eventStart + "\tms\t" +machine.eventStart.getTime() + " now " + now.getTime());
						boolean productive = "Productive".equals(machine.state);
						boolean autoStandby = eventDuration >= idleInterval  && isServerTime();
						boolean autoWrite   = eventDuration >= writeInterval && isServerTime();
						// Send an idle event if it was in Production and did not receive an event in the last idle interval
						if (autoStandby && productive) 
							{
							EquipmentEvent event = toEquipmentEvent(name,"GOTO_PRODUCTION_READY","","",new Hashtable(),new Hashtable());
							event.contexts.addElement(new Context("SUBSTATE","Standby","")); // Set SUBSTATE to Standby
							if (machine.mesEvent) event.contexts.addElement(new Context("SOURCE","MES","")); // If last event wasfrom MES, add context variable to specify so
							if (endOfShift) event.contexts.addElement(new Context("CREW",crew,"")); // If end of shift, set CREW context variable
							event(event,false);
							}
						else if (endOfShift || autoWrite)
							{
							EquipmentEvent event = toEquipmentEvent(name,"EQUIP_INFO","","",new Hashtable(),new Hashtable());
							if (machine.mesEvent) event.contexts.addElement(new Context("SOURCE","MES",""));  // If last event was from MES, add context variable to specify so
							if (endOfShift) event.contexts.addElement(new Context("CREW",crew,"")); // If end of shift, set CREW context variable
							event(event,false);
							}
						if (endOfShift) machine.productiveTime = 0;
						else if (productive) machine.productiveTime += (now.getTime()-last.getTime())/1000;
						}
					else if (endOfShift)
						{
						EquipmentEvent event = toEquipmentEvent(name,"EQUIP_INFO","","",new Hashtable(),new Hashtable());
						if (machine.mesEvent) event.contexts.addElement(new Context("SOURCE","MES",""));  // If last event was from MES, add context variable to specify so
						if (endOfShift) event.contexts.addElement(new Context("CREW",crew,"")); // If end of shift, set CREW context variable
						event(event,false);
						}						
					}
				executeMonitors(monitorExecution,endOfShift);
				last = now;
				summary.summarize(now,isProduction);
				}
			catch (Throwable t) {logger.error(t.getMessage(),t);
			}
			try {
				Thread.sleep(60000);} catch (Throwable t) {}
			}
		}
	
	private Map executeMonitors(Map monitors,boolean endOfShift) throws Throwable
		{
		try{
		List<BusinessRule> monitorRules = new ArrayList();
		monitorRules.addAll(getBusinessRules(BusinessRule.MONITOR));
		monitorRules.addAll(getBusinessRules(BusinessRule.DM_MONITOR));		
		for (int i = 0; i < monitorRules.size(); i++)
			{
			BusinessRule rule = monitorRules.get(i);
			boolean equipmentTypeExist = getEquipmentTypes().get(rule.getEquipmentTypePk()) != null;
			List actions = new ArrayList();
			actions.addAll(rule.getActions());
			for (int j = 0; j < actions.size(); j++)
				{
				BusinessRuleAction action = (BusinessRuleAction) actions.get(j);
				String monitorId = rule.getPrimaryKey() + "-" + action.getPrimaryKey();
				Date lastTime = null;
				if(equipmentTypeExist){lastTime= (Date) monitors.get(monitorId);}
				else
					{
					monitors.remove(monitorId);
					continue;
					}
				if (lastTime == null)
					{
					lastTime = new Date();
					monitors.put(monitorId, lastTime);
					}
				if (BusinessRuleAction.GENERATE_FILE.equals(action.getAction()))
					{
					Date lastExecution = lastTime;
					try{if(generateFile(lastTime, rule)){lastExecution = new Date();}}
					catch(Throwable t){lastExecution = new Date();logger.error(""+lastExecution,t);}
					monitors.put(monitorId,lastExecution);
					}
				else if(BusinessRuleAction.GENERATE_FILE_TEMPLATE.equals(action.getAction()))
					{
					Date lastExecution = lastTime;
					try{if(generateFileFromTemplate(lastTime,rule)){lastExecution = new Date();}}
					catch(Throwable t){lastExecution = new Date();logger.error(""+lastExecution,t);}
					monitors.put(monitorId,lastExecution);
					}
				else if(BusinessRuleAction.EMAIL_MONITOR.equals(action.getAction()))
					{
					Date  lastExecution = lastTime;
					try{if(endOfShift){if(eMailMonitor(lastTime,rule)){lastExecution = new Date();}}}
					catch(Throwable t){logger.error(""+lastExecution,t);}
					monitors.put(monitorId,lastExecution);
					}
				else if (BusinessRuleAction.UPDATE_ATTRIBUTE_EXTERNAL.equals(action.getAction()))
					{
					Date lastExecution = lastTime;
					try { if (updateAttributeWithExternalSource(lastTime, rule)) lastExecution = new Date();  } 
					catch (Throwable t) {lastExecution = new Date();logger.error(""+lastExecution,t);}
					monitors.put(monitorId, lastExecution);
					}
				}
			
			 }
		}catch(Throwable t){logger.error(t.getMessage(),t);}
		return monitors;
		}
	
	
	private boolean updateAttributeWithExternalSource(Date last, BusinessRule rule)
		{
		BusinessRuleAction action = (BusinessRuleAction) rule.getActionsBy("Update Attribute with External Source").get(0);
		Map<String, String> parameters = action.getActionParameters();
		String tmp = parameters.get("timeout");
		if ((tmp == null) || (tmp.equals(""))) tmp = "60";
		int timeout = Integer.valueOf(tmp).intValue();
		Date now = new Date();
		if (now.getTime() - last.getTime() < timeout * 1000 * 60){return false;}
		else{return updateAttributeWithExternalSource(rule);}
		}
	
	private boolean updateAttributeWithExternalSource(BusinessRule rule)
		{
		boolean updated = true;
		BusinessRuleAction action = (BusinessRuleAction) rule.getActionsBy("Update Attribute with External Source").get(0);
		Map<String, String> parameters = action.getActionParameters();
		String query = parameters.get("query");
		String attribute = parameters.get("attribute");
		List macs = DurableManager.getMachinesMetBusinessRule(rule,new ArrayList(getMachines(getEquipmentType(rule.getEquipmentTypePk())).values()));
		if (externalDBSource != null)
			{
			int updatedDurables = 0;
			long start = System.currentTimeMillis();
			int limit = macs.size();
			Machine machine;
			Object tmp;
			Map values = new HashMap();
			Object currentValue;
			for (int i = 0; i < limit; i++)
				{
				String param = externalDBSource.getParamName(query);
				machine = (Machine) macs.get(i);
				tmp = machine.get(param);
				if(tmp == null || tmp.equals("")){continue;}
				String value = tmp.toString();
				if(!values.containsKey(value))
					{
					List results = externalDBSource.executeQuery(query, machine);
					values.put(value,results);
					}
				currentValue = machine.get(attribute);
				if(currentValue==null){currentValue = "";}
				currentValue = currentValue.toString();
				String dbValue = values.get(value).toString();
				if(!currentValue.equals(dbValue))
					{
					logger.info("updating Machine " +machine.mESName + " attribute "+attribute + " current value " + currentValue+ " new value from DB " +dbValue);
					machine.set(attribute,dbValue);
					try{
					Persistence.store(machine);
					setMachine(machine);
					updatedDurables++;
					}catch(Throwable t){logger.error(rule.getName(),t);}
					}
				}
			long end = System.currentTimeMillis();
			logger.info(updatedDurables + " updated in " + (end - start) + " msecs");
			}
		else{logger.warn("updateAttributeWithExternalSource executed but externalDBSource is null");}
		return updated;
		}

	
	private boolean eMailMonitor(Date last,BusinessRule rule)
		{
		Date now = new Date();
		long diff = now.getTime() - last.getTime();
		long hour = 60 * 60 *1000;
		if(diff < hour)return false;
		BusinessRuleAction action = (BusinessRuleAction)rule.getActionsBy(BusinessRuleAction.EMAIL_MONITOR).get(0);
		Map<String, String> actionParameters = action.getActionParameters();
		String to = actionParameters.get(TransitionAction.PARAM_TO);
		Map params = new HashMap();
		params.put("monitor",rule.getName());
        params.put("equipmentType",((EquipmentType)getEquipmentType(rule.getEquipmentTypePk())).name);
		String report = getProcessedJsp("durable_monitor.do",params);
		DurableEmail email = new DurableEmail();
		email.setTo(to);
		email.setSubject(rule.getName().replace("_Main"," Monitor"));
		email.setType("html");
		email.setContent(report);
		new Thread(email).start();
		return true;
		}
	
	public void setMachine(Machine auxMachine)
		{
		machines.put(auxMachine.mESName,auxMachine);
		}
	
	public void removeMachine(Machine auxMachine)
		{
		machines.remove(auxMachine.mESName);
		}
	
	
	private boolean generateFile(Date last, BusinessRule rule) throws Throwable
		{
		Date now = new Date();
		List actions = rule.getActionsBy(BusinessRuleAction.GENERATE_FILE);
		if (actions.size() == 0) {return false;}
		BusinessRuleAction action = (BusinessRuleAction)actions.get(0); 
		Map<String, String> parameterMap = action.getActionParameters();
		int period = Integer.parseInt(parameterMap.get("Timeout"));	
		String filename = parameterMap.get("Filename");
		String content = parameterMap.get("Content");
		if ((now.getTime() - last.getTime())< period * 1000*60||filename ==null||content==null) {return false;}  
		EquipmentType eqType= getEquipmentTypes().get(rule.getEquipmentTypePk());
		Map machines = getMachines(eqType);
		List macs = DurableManager.getMachinesMetBusinessRule(rule, new ArrayList(machines.values())); 
		StringBuilder filecontents = new StringBuilder();
		String head = parameterMap.get("Head");	
		filecontents.append(head);
        filecontents.append(System.getProperty("line.separator"));
        Iterator it = macs.iterator();
        String line;
        Machine mac;
		while (it.hasNext())
			{
			mac = (Machine)it.next();
			line = getFormattedLineFromMachine(content, "#", mac);
			filecontents.append(line);
			filecontents.append(System.getProperty("line.separator"));
			}
		File file = new File(StateStamper.transferDirectory,filename);
	    Writer output = new BufferedWriter(new FileWriter(file ));
	    try {
	      output.write(filecontents.toString());
	    }
	    finally {
	      output.close();
	    }
	    return true;
	}
	
	private String getFormattedLineFromMachine(String format,String replace,Machine mac)
		{
		String[] contents=format.split(replace);
		StringBuilder sb = new StringBuilder();
		Object val;
		for(int i=0;i<contents.length;i++)
			{
			val = mac.get(contents[i]);
			if(val==null){val="";}
			sb.append(val+",");
			}
		return sb.toString();
		}
	
	private boolean generateFileFromTemplate(Date last, BusinessRule rule) throws Throwable
		{
		long start = System.currentTimeMillis();
		Date now = new Date();
		BusinessRuleAction action = (BusinessRuleAction)rule.getActionsBy(BusinessRuleAction.GENERATE_FILE_TEMPLATE).get(0);
		Map<String, String> parameter = action.getActionParameters();
		int period = Integer.parseInt(parameter.get("timeout"));
		String fileName = parameter.get("fileName");
		String template = parameter.get("template");
		if ((now.getTime() - last.getTime())< period * 1000*60||fileName ==null) {return false;}
		EquipmentType eqType= getEquipmentTypes().get(rule.getEquipmentTypePk());
		Map machines = getMachines(eqType);
		List macs = DurableManager.getMachinesMetBusinessRule(rule, new ArrayList(machines.values()));
		Collections.sort(macs, new Comparator(){public int compare(Object o1, Object o2){return o1.toString().compareTo(o2.toString());}});
		StringBuilder fileContents = new StringBuilder();
		Properties props = new Properties();
		props.load(new FileInputStream(baseDirectory+"/"+ template));
		String header = props.getProperty("header"); 
		fileContents.append(header+"\n");
		for(int i=0;i< macs.size();i++)//content
			{
			Machine machine = (Machine)macs.get(i);
			String content = "content.";
			int j = 1;
			String contentId =content + j;
			String line;
			boolean split;
			while(props.containsKey(contentId))
				{
				line = props.getProperty(contentId,"");
				split = Boolean.valueOf(props.getProperty(contentId+".splitMultipleItems","")).booleanValue();
				String var = getVariable(line);
				String value = getVariableValue(var, machine);
				line = replaceVariable(line, machine,split);
				fileContents.append(line+"\n");
				if(props.containsKey(contentId+".detail.associateBy"))
					{
					String detailsBy = props.getProperty(contentId+".detail.associateBy","");
					Map details = null;
					if(detailsBy.equals("type")){details = new TreeMap(getMachines(new String[]{value},new String[]{}));}
					else if(detailsBy.equals("workstation")){details = new TreeMap(getAttachedEquipments(value));}
					Iterator it = details.keySet().iterator();
					while(it.hasNext())
						{
						String macDetailId = (String)it.next();
						Machine mac = (Machine)details.get(macDetailId);
						int k =1;
						String detailId = contentId+".detail."+k;
						String detailLine;
						while(props.containsKey(detailId))
							{
							detailLine = props.getProperty(detailId,"");
							detailLine = replaceVariable(detailLine,mac,false);
							fileContents.append(detailLine+"\n");
							k++;
							detailId = contentId+".detail."+k;
							}
						}
					}
				j++;
				contentId =content + j;
				}
			}
		String footer = props.getProperty("footer",""); 
		fileContents.append(footer);
		//write out the file
		File file = new File(StateStamper.transferDirectory,fileName);
	    Writer output = new BufferedWriter(new FileWriter(file ));
	    try {output.write(fileContents.toString());}
	    finally {output.close();}
	    long end = System.currentTimeMillis();
	    logger.info("generateFileFromTemplate() " + (end-start));
	    return true;
	    }
	
	private String replaceVariable(String line,IStatus status,boolean split)
		{
		String newLine=line;
		String var = getVariable(line);
		String value = getVariableValue(var, status);
		if(var != null)
			{
			if(split){newLine = splitVector(line,var,value);}
			else{newLine = line.replace(var, value);}
			}
		return newLine;
		}
	
	private String getVariableValue(String var,IStatus status)
		{
		String value="";
		Object tmp;
		if( var != null && status.has(var.substring(1)))
			{
			tmp = status.get(var.substring(1));
			value = (tmp !=null) ?tmp.toString():"";
			}
		return value;
		}
	
	private String splitVector(String line,String var,String value)
		{
		StringBuilder tmp = new StringBuilder(value);
		StringBuilder result = new StringBuilder();
		if(tmp.toString().startsWith("[")){tmp.deleteCharAt(0);}
		if(tmp.toString().endsWith("]")){tmp.deleteCharAt(tmp.length()-1);}
		String[] values = tmp.toString().split(",");
		String tmpValue;
		for(int i = 0;values !=null && i < values.length;i++)
			{
			tmpValue = values[i].trim();
			result.append(line.replace(var,tmpValue)+"\n");
			}
		if(result.toString().endsWith("\n")){result.deleteCharAt(result.length()-1);}
		return result.toString();
		}
	
	private Map getAttachedEquipments(String name)
		{
		Map result = new HashMap();
		if(name == null){return result;}
		Iterator it =machines.values().iterator();
		Machine mac=null;
		while(it.hasNext())
			{
			mac = (Machine)it.next();
			if(name.equals(mac.getWorkstation())){result.put(mac.getMESName(),mac);}
			}
		return result;
		}
	
	private String getSubstring(String str,String start,String end)
		{
		if(str == null || !str.contains(start)){return null;}
		int first = str.indexOf(start);
		int endIdx; 
		if(end !=null && str.indexOf(end, first)>-1){ endIdx = str.indexOf(end, first);}
		else {endIdx = str.length();}
		return  str.substring(first,endIdx);
		}
	
	private String getVariable(String line)
		{
		return getSubstring(line, "$"," ");
		}
	
	/**
	 * reload equipments under specific equipment type
	 * non-existing machines will be removed from machines,tester status will be reloaded
	 * loadTesterStatus
	 * @param equipmentTypePk the equipment type to be refreshed
	 * @throws Throwable 
	 */

	public synchronized void setMachinesByEquipmentType(EquipmentType equipmentType) throws Throwable
		{
		Machine machine = new Machine();
		machine.setEquipmentType_pk(equipmentType.primaryKey);
		Vector machines = Persistence.search(machine);
		clearMachines(machines);
		setMachines(machines,true,equipmentType,true);
		}

	
	/**
	 * Sets the list of machines that will be collecting data
	 * non-existing machines will not be removed from machines, tester status will not be reloaded
	 * @param machines A vector of Machine objects
	 * @throws Throwable 
	 */
	public synchronized void setMachines(Vector auxMachines) throws Throwable
		{
		setMachines(auxMachines,false,null,false);
		}

	
	/**
	 * Sets the list of machines that will be collecting data
	 * @param machines A vector of Machine objects
	 * @param loadTesterStatus to indicate if load from the DB the latest tester status
	 * @param equipmentType the equipment type to reload, null for all equipment types 
	 * @param deleteMachines if the auxMachines donot include all machines, will the redundant machines to be deleted from memory 
	 * @throws Throwable 
	 */
	private synchronized void setMachines(List auxMachines,boolean loadTesterStatus,EquipmentType equipmentType,boolean deleteMachines) throws Throwable 
		{
		Map durableStatus = null;
		String crew = "";
		try {crew = FiscalDate.getCrew(new FiscalDate(FiscalDate.SHIFT,formatter.format(new Date())));}
		catch (Throwable throwable)	{logger.error(throwable.getMessage(),throwable);}
		Set auxNames = new HashSet();
		Map testerStatus = null;
		for (int i=0; i<auxMachines.size(); i++)
			{
			Machine auxMachine = (Machine)auxMachines.get(i);	
			auxNames.add(auxMachine.mESName);
			Machine machine = (Machine)machines.get(auxMachine.mESName);
			boolean isNew = !pkToMESName.containsKey(auxMachine.primaryKey);
			boolean isTester = "Tester".equalsIgnoreCase(auxMachine.getEquipmentType().name);
			if(isNew)
				{
				machines.put(auxMachine.mESName,auxMachine);
				eiToMESName.put(auxMachine.eIName,auxMachine.mESName);
				pkToMESName.put(auxMachine.primaryKey,auxMachine.mESName );
				auxMachine.context = new HashMap();
			  	auxMachine.context.put("CREW",crew);
				if (isTester)
					{
				  	if(loadTesterStatus)
				  		{
				  		if(testerStatus == null)
				  			{
				  			testerStatus = getLatestTesterStatus();
				  			}
				  		initTesterState(auxMachine,testerStatus);
				  		}
					}
				else
					{
					if(durableStatus == null)
						{
						durableStatus=getCurrentDurableState(null);
						}
					initDurableStatus(durableStatus, auxMachine);
					}
			  	auxMachine.summing = new HashMap();
			  	auxMachine.performance = new HashMap();
			  	if(auxMachine.children == null){auxMachine.children = new HashMap<String, List<String>>();}
			  	performance.put(auxMachine.mESName,new Performance());			  
			  	addDMHinstance(auxMachine);			
			  	//set the state to Unknown for testers, for durables query the durable status
			  	if (auxMachine.state == null)
			  		{
			  		if(isTester){auxMachine.state = "Unknown";}
			  		else{auxMachine.state = getCurrentDurableState(auxMachine,durableStatus);}
			  		}			
				}
			else // Update machines
				{
				String oldMesName = pkToMESName.get(auxMachine.primaryKey);
				boolean renamedTester =  !oldMesName.equals(auxMachine.mESName) && isTester; 
				if(renamedTester)
					{
					machine = (Machine)machines.remove(oldMesName);
					pkToMESName.put(auxMachine.primaryKey,auxMachine.mESName );
					machine.mESName = auxMachine.mESName;
					}
				machine.eIName = auxMachine.eIName;
				machine.deviceGroup = auxMachine.deviceGroup;
				machine.platform = auxMachine.platform;				
				machine.type = auxMachine.type;
				machine.location = auxMachine.location;
				machine.workstation = auxMachine.workstation;
				machine.serial_number = auxMachine.serial_number;
				machine.engineer_contact = auxMachine.engineer_contact;				
				machine.vendor = auxMachine.vendor;				
				machine.pic_Host = auxMachine.pic_Host;
				machine.pic_Port = auxMachine.pic_Port;
				machine.dmhbox = auxMachine.dmhbox;
				machine.setDynamicAttributes(auxMachine.getDynamicAttributes());
				if (!isTester)
					{
					if(durableStatus == null)
						{
						durableStatus=getCurrentDurableState(null);
						}
					initDurableStatus(durableStatus, auxMachine);
					}							
				machine.setEquipmentType(getEquipmentTypes().get(machine.equipmentType_pk) );
				eiToMESName.put(machine.eIName,machine.mESName);
				machines.put(machine.mESName,machine);
			    addDMHinstance(auxMachine);
				}
			}
		// Delete machines
		if(deleteMachines)
			{
			Iterator keys;
			if(equipmentType != null)
				{
				keys = getMachines(equipmentType).keySet().iterator();
				}
			else
				{
				keys = getMachines().keySet().iterator();
				}
			while (keys.hasNext())
				{
				String name = (String)keys.next();
				if (!auxNames.contains(name))
					{
					Machine toDelete = (Machine)machines.get(name);
					machines.remove(name);
					if (toDelete != null) removeDMHinstance(toDelete);
					}
				}	
			}
		}

	private void initDurableStatus(Map durableStatus, Machine auxMachine) throws Throwable
		{
		CurrentDurableStatus currentDurableStatus = (CurrentDurableStatus) durableStatus
		.get(auxMachine.eIName);
		if (currentDurableStatus == null)
			{
			DurableStatus insertDurableStatus = insertDurableStatus(auxMachine);
			auxMachine.context.put("durableStatus",insertDurableStatus);
			CurrentDurableStatus insertedCurrDurableStatus = new CurrentDurableStatus(insertDurableStatus,auxMachine);
			durableStatus.put(insertedCurrDurableStatus.DURABLE_ID, insertedCurrDurableStatus);
			}
		else
			{
			DurableStatus dStatus = currentDurableStatus.getDurableStatus();
			auxMachine.context.put("durableStatus", dStatus);
			}
		}

	private DurableStatus insertDurableStatus(Machine auxMachine) throws Throwable
		{
		DurableStatus status = new DurableStatus();
		status.EQUIPMENT_PK = auxMachine.primaryKey;
		status.DURABLE_ID = auxMachine.eIName;
		Map states = getInitialState(auxMachine);
		status.STATE = (String) states.get("STATE");
		status.SUBSTATE = (String) states.get("SUBSTATE");
		status.duration = "0";
		status.TIME = formatter.format(new Date());
		status.putNewComments("TTT", status.TIME, "No durable status found for machine "+auxMachine.eIName+", inserting durable status.");
		status.workstation = auxMachine.workstation;
		status.location = auxMachine.location;
		List<EquipmentAttribute> attributes = auxMachine.getEquipmentType().getStatusAttributes();
		Map attributesHash = new HashMap();
		for (EquipmentAttribute attribute:attributes)
			{
			String value = null;
			if (attribute.type.equalsIgnoreCase("Text")) value = "";
			else value = "0";
			attributesHash.put(attribute.name, value);
			}
		status.ATTRIBUTES = attributesHash;
		Persistence.store(status);
		logger.error("No durable status found for machine "+auxMachine.eIName+", inserting durable status "+status.STATUS_PK);
		return status;
		}
	
	public void initTesterState(Machine machine,Map testerStatus) throws Throwable
		{
  		Status latestStatus = (Status)testerStatus.get(machine.primaryKey);
  		if(latestStatus==null)
  			{
  			latestStatus = insertTesterStatus(machine);
  			testerStatus.put(machine.primaryKey, latestStatus);
  			}
  		machine.setCurrentStatus(latestStatus);
  		String state = (String)latestStatus.get("STATE");
  		String substate = (String)latestStatus.get("SUBSTATE");
  		String time = (String)latestStatus.get("TIME");
  		Date now = new Date();
  		Date lastStateTime = null;
  		Date newStateTime = null;
  		try{lastStateTime = formatter.parse(time);}
  		catch(ParseException pe){lastStateTime = now;};
  		long diff = now.getTime() - lastStateTime.getTime();
  		if(diff > (writeInterval * 1000))
  			{
  			newStateTime = new Date(lastStateTime.getTime() + (writeInterval*1000)); 
  			}
  		else{
  		    newStateTime = now;
  		    }
  		if(state != null && state.equals("Productive") && diff > (idleInterval*1000))
  			{
  			state = "Standby";
  			substate = "Standby";
  			}
  		machine.state = state; 
  		if(machine.context == null){machine.context = new HashMap();}
  		Object tmp ;
  		for(int i=0; i < contexts.length; i++)
  			{
  			 tmp = latestStatus.get(contexts[i]);
  			 if(tmp != null){machine.context.put(contexts[i], tmp.toString());}
  			}
  		machine.context.put("STATE", state);
  		if(substate != null){machine.context.put("SUBSTATE",substate);}
  		if(machine.summing == null){machine.summing = new HashMap();}
  		for(int i=0; i < summings.length; i++)
  			{
  			 tmp = latestStatus.get(summings[i]);
  			 if(tmp != null){machine.summing.put(summings[i], Integer.valueOf(tmp.toString()) );}
  			}
  		machine.stateStart = newStateTime;
  		machine.eventStart = newStateTime;
  		logger.debug("initTesterState Machine\t" + machine.mESName+"\tstate\t"+machine.state+"\tsubstate\t"+machine.context.get("SUBSTATE")+"\tstateStart\t"+machine.stateStart);
		}
	/**
	 * insert new tester status record into database
	 * @param the tester details
	 */
	private Status insertTesterStatus(Machine machine) throws Throwable
		{
		Status status = new Status();
		status.set("MID", machine.mESName);
		status.set("STATE", Substate.UNK);
		status.set("SUBSTATE", Substate.UNK);
		status.set("EQUIPMENT_PK", machine.primaryKey);
		status.set("PERIOD", "R");
		status.set("TIME", formatter.format(new Date()));
		Persistence.store(status);
		return status;
		}
	
	/**
	 * returns the current durable status for the durable passed
	 * @param the durable to retrieve the status
	 * @return a string with the current E10 state of the durable 
	 * */
	private String getCurrentDurableState(Machine durable, Map durableStatus )
		{
		String state;
		if(durableStatus ==null)
			{
			durableStatus = getCurrentDurableState(null);
			}
		state = ((CurrentDurableStatus)durableStatus.get(durable.eIName)).STATE;
		return state!=null?state:"Unknown";
		}

	private Map getCurrentDurableState(Integer equipment_type_pk)
		{
		Map durableStatus = new HashMap();
		CurrentDurableStatus currentStatus = new CurrentDurableStatus();
		if (equipment_type_pk != null)
			{
			currentStatus.equipmentType_pk = equipment_type_pk;
			}
		Vector vDurableStatus = null;
		try
			{
			long start = System.currentTimeMillis();
			vDurableStatus = (Vector) Persistence.search(currentStatus);
			long end = System.currentTimeMillis();
			logger.debug( (end-start) + " ms querying CurrentDurableStatus in getCurrentDurableState");
			}
		catch (Throwable e)
			{
			logger.error("getCurrentDurableState-"+equipment_type_pk+"\t" , e);
			}
		for (int j = 0; vDurableStatus != null && j < vDurableStatus.size(); j++)
			{
			currentStatus = (CurrentDurableStatus) vDurableStatus.get(j);
			durableStatus.put(currentStatus.DURABLE_ID, currentStatus);
			}
		return durableStatus;
		}
	
	/**
	 * @return a map where the key is the mes name of the tester and the value is a Map that has two keys the state E10 and sub-state with the string with the 
	 * */
	private Map getLatestTesterStatus()
		{
		Map result = new Hashtable();
		CurrentStatus currentStatus = null;
		long start = System.currentTimeMillis(),end;
		try
			{
			List latestStatus = Persistence.search(new CurrentStatus());
			end = System.currentTimeMillis();
			logger.debug( (end-start) + " ms querying CurrentStatus in getLatestTesterStatus");
			for(int i = 0; latestStatus != null && i < latestStatus.size();i++)
				{
				currentStatus = (CurrentStatus)latestStatus.get(i);
				Status status = currentStatus.getStatus();
				result.put(currentStatus.get("EQUIPMENT_PK"), status);
				}
			}
		catch (Throwable e)
			{
			logger.error(e.getMessage(),e);
			}
		return result;
		}
	
	private Map getPerformance(String[] perfVars,Map contexts,Map summings)
		{
		Map performance = new HashMap();
		for(int i=0;i<perfVars.length;i++)
			{
			if(contexts.containsKey(perfVars[i])){performance.put(perfVars[i],contexts.get(perfVars[i]));}
			if(summings.containsKey(perfVars[i])){performance.put(perfVars[i],summings.get(perfVars[i]));}
			}
		return performance;
		}


	/**
	 * add a new PIC connection to the hashtable. If the there is already an existing connection
	 * reuse it. 
	 * @param Machine machine
	 */
	private void addDMHinstance(Machine machine){			
		if(noPicInfo(machine)){
			removeDMHinstance(machine);
			return;
			}
		if(dmhs.containsKey(machine.mESName)){					
		  if (dmhs.get(machine.mESName).comparePicConn(machine)){
			  return; 		   
          }
		  else{
			  removeDMHinstance(machine);
		  }
		} 
		Collection<DMH> collection = dmhs.values();
		for (DMH client:collection ){
			if (client.comparePicConn(machine)) {
				dmhs.put(machine.mESName, client);
				return;
			}
		}
		dmhs.put(machine.mESName, new DMH(machine.pic_Host,machine.pic_Port, machine.dmhbox));
	}
	
	private boolean noPicInfo(Machine machine){
		return machine.pic_Host==null||machine.pic_Host.trim().equals("")||
		       machine.pic_Port==null||machine.pic_Port.trim().equals("")||
		       machine.dmhbox==null||machine.dmhbox.trim().equals("");
	}

		
	/**
	 * remove a DMH client from our hashmap, 
	 * if there is no other machine relate to this client then close it.
	 * @param Machine machine
	 */
	private void removeDMHinstance(Machine machine){		
		DMH client = this.dmhs.remove(machine.mESName);
		if (client ==null) return;
		if (!this.dmhs.containsValue(client)) 
			{			
			client.close();
			}
		
	}
					
	/**
	 * Populate hash table states
	 * @param auxStates
	 */
	public void setStates(Vector substates)
		{
		states = new Hashtable();
		for (int i=0; i<substates.size(); i++)
			{
			Substate substate = (Substate)substates.elementAt(i);
			EquipmentType eqType = getEquipmentType(substate.equipmenttypePK); 
			if(eqType != null && "Tester".equalsIgnoreCase(eqType.name))
				{
				states.put(substate.substate,substate);
				}
			}
		}
	
	/**
	 * Returns vector. First element is a vector of types, second is a vector of locations
	 * @return Vector with the data
	 */
	public List getGroups(String equipmentType)
		{
		List types = new ArrayList(), locations = new ArrayList();
		List names;
		if(equipmentType == null || equipmentType.equals(""))
			{
			names = new ArrayList(getDefaultMachineNames());
			}
		else
			{
			names = new ArrayList(getMachines(getEquipmentTypeByName(equipmentType)).keySet());
			}
		for (int i=0; i<names.size(); i++)
			{
			Machine machine = (Machine)machines.get(names.get(i));
			if (machine.type != null && !types.contains(machine.type)) types.add(machine.type);
			if (machine.location != null && !locations.contains(machine.location)) locations.add(machine.location);
			}
		List result = new ArrayList();
		Collections.sort(types);
		result.add(types);
		Collections.sort(locations);
		result.add(locations);
		return result;
		}
	
	private Collection getDefaultMachineNames()
		{
		Collection keys = null; 
		EquipmentType eqType = getDefaultEquipmentType();
		Map macs = null;
		if(eqType != null )
			{
			macs = getMachines(eqType);
			keys = macs.keySet();
			}
		else
			{
			keys = machines.keySet();
			}
		return keys;
		}
	
	/**
	 * @return the default Equipment Type defined in the configuration
	 * */
	public EquipmentType getDefaultEquipmentType()
		{
		return getEquipmentTypeByName(defaultEquipmentTypeName);
		}
	/**
	 * Return Default Area whose area_pk is 1
	 * @return
	 */
	public Area getDefaultArea()
		{
		return getAreas().get(Area.AREA_ALL_PK);
		}
	
	/**
	 * Returns a Map of ALL Durable objects
	 * @return The Hashtable
	 */
	public Map getAllDurables() {
		Map map = new HashMap();
		Set keys = machines.keySet();
		Iterator it = keys.iterator();
		while (it.hasNext()){
			Object object = it.next();
			Machine machine = (Machine)machines.get(object);
			EquipmentType type = getEquipmentTypeByName("Tester");
			if (!machine.equipmentType_pk.equals(type.primaryKey))
				map.put(object, machine);
		}		
		return map;		
	}
	
	/**
	 * get equipments under specific equipmentType and in specific area
	 * @param equipmentType
	 * @param areaPK
	 * @return
	 */
	public Map getMachines(EquipmentType equipmentType, Integer areaPK) {
		Map map = new HashMap();
		Set keys = machines.keySet();
		Iterator it = keys.iterator();
		if(areaPK==null)areaPK=Area.AREA_ALL_PK;
		while (it.hasNext())
			{
			Object object = it.next();
			Machine machine = (Machine)machines.get(object);
			if (machine.equipmentType_pk.equals(equipmentType.primaryKey))
				{
				if(areaPK.equals(machine.getArea_pk())||Area.AREA_ALL_PK.equals(areaPK))
					{
					try 
						{
						Object clone = machine.clone();
						map.put(object, clone);
						} catch (CloneNotSupportedException e) {
							e.printStackTrace();
						}
					}
				}
			}		
		return map;		
	}
	
	/**
	 * Returns a Map of Machine objects
	 * @return The vector
	 */
	public Map getMachines(EquipmentType equipmentType) {
		Map map = new HashMap();
		Set keys = machines.keySet();
		Iterator it = keys.iterator();
		while (it.hasNext()){
			Object object = it.next();
			Machine machine = (Machine)machines.get(object);
			if (machine.equipmentType_pk.equals(equipmentType.primaryKey))
				map.put(object, machine);
		}		
		return map;		
	}
	/**returns a Map with all the machine objects
	 * 
	 * 	 * */
	public Map getMachines()
		{
		return machines;
		}
	
	/**
	 * @param Name: the name of the item to return
	 * @return a Machine object given 
	 * */
	public Machine getMachine(String name)
		{
		return (Machine)machines.get(name);
		}

	/**
	 * Returns sorted vector with String array with machine, state and time
	 * @param type If not null, return UOEE for this type only
	 * @param location If not null, return UOEE for this location only
	 * @return Vector with the data
	 */
	public List getStates(String type, String location,String equipmentType)
		{
		List result = new ArrayList();
		List names;
		if(equipmentType == null || equipmentType.equals(""))
			{
			names = new ArrayList(getDefaultMachineNames());
			}
		else
			{
			names = new ArrayList(getMachines(getEquipmentTypeByName(equipmentType)).keySet());
			}
		Collections.sort(names);
		for (int i=0; i<names.size(); i++)
			{
			Machine machine = (Machine)machines.get(names.get(i));
			String stateStart = "";
			if (machine.stateStart != null) stateStart = formatter.format(machine.stateStart);
			if ((type != null && machine.type.equals(type)) || (location != null && machine.location.equals(location))) result.add(new String[]{machine.mESName,machine.state,stateStart,machine.getEquipmentType().name});
			}
		return result;
		}
	
	/**
	 * Returns sorted vector with String array with machine and UOEE
	 * @param type If not null, return UOEE for this type only
	 * @param location If not null, return UOEE for this location only
	 * @return Vector with the data
	 */
	public Vector getUOEE(String types, String locations)
		{
		if (types != null) types += ","; // Add final delimitation
		if (locations != null) locations += ",";
		String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
		long total = 1;
		try
			{
			total = FiscalDate.diff(now, new FiscalDate(FiscalDate.SHIFT, now).startTime);
			}
		catch (Throwable t)
			{
			logger.error("types:"+types+",locations:"+locations,t);
			}
		Vector result = new Vector();
		Vector names = new Vector(getDefaultMachineNames());
		Collections.sort(names);
		for (int i = 0; i < names.size(); i++)
			{
			Machine machine = (Machine) machines.get(names.elementAt(i));
			if ((types != null && types.indexOf(machine.type + ",") >= 0)
					|| (locations != null && locations.indexOf(machine.location + ",") >= 0))
				{
				Float tmp = machine.productiveTime * 100 / total;
				if (tmp > 100.0) tmp = 100.0f;
				result.addElement(new String[] { machine.mESName, decimal.format(tmp) });
				}
			}
		return result;
		}
		
	/**
	 * Returns sorted vector with String array with machine id, location ,type and percentage completed 
	 * @param lotprogress, return testers with percentage completed  >= than given lotprogress
	 * @return Vector with the data
	 */
	public Vector getLotProgress(float lotProgressLimit)
		{		
		Vector result = new Vector();
		Vector names = new Vector(getDefaultMachineNames());
		Collections.sort(names);
		for (int i=0; i<names.size(); i++)
			{
			Machine machine = (Machine)machines.get(names.elementAt(i));
			try
			   {
				Float lotProgress = (Float)machine.performance.get("LOT_PROGRESS");
				   if ( lotProgress >= (lotProgressLimit*100.0f))
					   {						
					   result.addElement(new String[]{machine.mESName,machine.location,machine.type,String.valueOf(lotProgress)});
				       }
			   }
			catch(Exception ex){}
			}	
		return result;
		}			
	/**
	 * Returns vector of vectors with machine data (machine, state, context and summing variables)
	 * @param machine The machine to get data from. If it is null, return all data. If MES name is not null, return for that machine only, if type is not null return for that type or if location is not null return for that location 
	 * @return Vector with the data
	 */
	public Vector getContextsSummings(Machine machine)
		{
		//create a local contexts to overshadow the global contexts
		//using the global contexts cause a duplicate TIME element with incorrect information
		List tmpContext = new ArrayList(Arrays.asList(this.contexts));
		tmpContext.remove("TIME");
		String contexts[] =  (String[])tmpContext.toArray(new String[0]);
		Vector result = new Vector();
		for (int i=0; i<contexts.length; i++)
			{
			Vector row = new Vector();
			row.addElement(contexts[i]);
			result.addElement(row);
			}
		Vector row = new Vector();
		row.addElement("TIME");
		result.addElement(row);
		for (int i=0; i<summings.length; i++)
			{
			row = new Vector();
			row.addElement(summings[i]);
			result.addElement(row);
			}
		Vector names = new Vector(getDefaultMachineNames());
		Collections.sort(names);
		for (int i=0; i<names.size(); i++)
			{
			Machine aux = (Machine)machines.get(names.elementAt(i));
			if (machine == null || (machine.mESName == null && (aux.type.equals(machine.type) || aux.location.equals(machine.location))) || aux.mESName.equals(machine.mESName))
				{
				((Vector)result.elementAt(0)).addElement(aux.mESName);
				((Vector)result.elementAt(1)).addElement(aux.state);
				for (int j=2; j<contexts.length; j++)
					{
					String value = (String)aux.context.get(contexts[j]);
					if (value==null) value="";
					((Vector)result.elementAt(j)).addElement(value);
					}
				String date = aux.stateStart==null?"":formatter.format(aux.stateStart);
				((Vector)result.elementAt(contexts.length)).addElement(date);
				for (int j=0; j<summings.length; j++)
					{
					Integer value = (Integer)aux.summing.get(summings[j]);
					if (value==null) value=new Integer(0);
					((Vector)result.elementAt(j+contexts.length+1)).addElement(value.toString());
					}
				}
			}
		return result;
		}
	
	/**
	 * Initializes performance by reading data for the given raw dates
	 * @throws Throwable
	 */
	private void update() throws Throwable
		{
		Date now = new Date(), first = new Date(now.getTime()-60*60*24 *1000L);
		long start=0,end=0;
		FiscalDate fiscalDate = new FiscalDate(FiscalDate.SHIFT,formatter.format(first));
		String[] equal = null,greater = null,less = null,like = null,order = null,in = null,grouping=null;
		String outer=null;
		String[] attrib = new String[contexts.length+summings.length];
		for(int i=0;i< contexts.length;i++){attrib[i]=contexts[i];}
		for(int i=0;i< summings.length;i++){attrib[contexts.length+i]=summings[i];}
		equal = new String[]{"PERIOD","R"};
		greater= new String[]{"TIME",fiscalDate.startTime};
		order = new String[]{"STATUS_PK",Boolean.toString(true)};
		Iterator it = null;
		try{
		start = System.currentTimeMillis();
		it = 	Persistence.iSearch(Status.class, attrib, equal,greater, less, like, in,grouping, order, outer);
		end = System.currentTimeMillis();
		logger.debug((end-start)+" ms reading last 24 hours in update for performance metrics");
		start = System.currentTimeMillis();
		Hashtable columnIndex = new Hashtable();
		for (int k=0; k<attrib.length; k++) {columnIndex.put(attrib[k],new Integer(k));} // Read column headers
		Object[] res;
		String record;
		String date=null;
		while(it!=null && it.hasNext()){
			res= (Object[])it.next();
			record="";
			for (int j = 0; res != null && j < res.length; j++) {
				record+=res[j]!= null?res[j]+"\t":"\t";
				if (j==1){//Time is processed independently of the record in date field
					date= res[j].toString();
				}				
			}
			try
			{
			String mid = ProcessRecord.getValue("MID",record,date,columnIndex);
			String time = ProcessRecord.getValue("TIME",record,date,columnIndex);
			Machine machine = (Machine)machines.get(mid);
			Performance perf = (Performance)performance.get(mid);
			if(perf!=null)
			perf.update(machine,formatter.parse(time),record,columnIndex); // Update performance variables
			}
			catch (Throwable t) {logger.error("update\trecord\t"+record,t);throw t;};
		}
		}
		catch (Throwable t) {
			logger.error(t.getMessage(),t);
		}
		finally{
			 if (it != null)((PersistenceBrokerIterator)it).destroy();
		}
		end = System.currentTimeMillis();
		logger.debug( (end-start)+" ms in update ");
		}
	
	/**
	 * Set machine name, type and location for an equipment object
	 * @param Equipment equipment The equipment object
	 */
	private boolean setNameTypeLocation(Equipment equipment)
		{
		try
			{
			Machine machine = (Machine)machines.get(equipment.id);
			if (machine == null)
				{
				String name = (String)eiToMESName.get(equipment.id);
				if (name != null) machine = (Machine)machines.get(name); 
				if (machine == null) return false;
				}
			equipment.id = machine.mESName;
			equipment.type = machine.type;
			equipment.location = machine.location;
			}
		catch (Throwable throwable) {
			logger.error("setNameTypeLocation:equipment\t"+equipment+"\tequipment.id\t"+equipment.id,throwable);
		}
		return true;
		}

	/**
	 * Send equipment event to Diamond, PIC, and Promis if needed
	 * @param equipmentEvent The equipment event to send
	 * @param newState The new state
	 * @param oldState The old state
	 * @throws Throwable
	 */
	public void sendEvent(IEquipmentEvent iequipmentEvent, String newState, String oldState, String oldSubState, String newSubState) throws Throwable
		{
		EquipmentEvent equipmentEvent = (EquipmentEvent)iequipmentEvent; // this is a bad conversion to deal with legacy code implementation
		String equipmentId = equipmentEvent.equipment.id;
		if(!isValidTester(equipmentId)){return;}

		// Load to Diamond
		if (!diamondDirectory.equals(""))
			{
			String time = equipmentEvent.startTime.replaceAll(":", "");
			File file = new File(baseDirectory + "/temp/" + equipmentEvent.equipment.id + "_" + time + counter
					+ ".xml");
			counter++; // Append a counter (0 through 99) to the file name to
						// avoid duplicates
			if (counter > 99) counter = 0;
			FileWriter output = new FileWriter(file);
			Marshaller marshaller = this.xMLContext.createMarshaller();
			marshaller.setWriter(output);
			marshaller.marshal(equipmentEvent);
			output.close();
			file.renameTo(new File(diamondDirectory, file.getName()));
			}
		// Send event to PIC. Remove context and summing variables
		boolean isStateChanged = (oldState != null && newState != null) && (!oldState.equals(newState));
		boolean isSubstateChanged = (oldSubState != null && newSubState != null)
				&& (!oldSubState.equals(newSubState));
		if (isStateChanged && isSubstateChanged)
			{
			String mesname = equipmentEvent.equipment.id;
			DMH dmhclient = null;
			equipmentEvent.state = newState;
			equipmentEvent.contexts = new Vector();
			equipmentEvent.summings = new Vector();
			Context context = new Context("SUBSTATE", newSubState, "");
			equipmentEvent.contexts.add(context);
			if (mesname != null && !mesname.trim().equals("")) dmhclient = this.dmhs.get(mesname);
			if (dmhclient != null)
				{
				dmhclient.send(equipmentEvent);
				}
			else if (DMHConnection != null)
				{
				DMHConnection.send(equipmentEvent);
				}
			}

		if (promisConnection != null && !"EQUIP_INFO".equals(equipmentEvent.event.id) && isValidPromisId(equipmentId))
			send(equipmentEvent.equipment.id, newState, equipmentEvent.startTime);
		}
	
	private boolean isValidTester(String equipmentId)
		{
		boolean isValid = false;
		if(equipmentId == null){return false;}
		Machine machine = getMachine(equipmentId);
		if(machine != null && machine.getEquipmentType() != null)
			{
			isValid = machine.getEquipmentType().name != null && machine.getEquipmentType().name.equalsIgnoreCase("Tester");
			}
		return isValid;
		}
	
	private boolean isValidPromisId(String equipmentId)
		{
		int PROMIS_ID_LENGTH = 8;
		return equipmentId != null && equipmentId.length() <= PROMIS_ID_LENGTH;
		}

	
	/**
	 * Send state change for a given equipment to Promis
	 * @param equipment The equipment
	 * @param newState The new state
	 */
	private void send(String equipment, String state, String startTime)
		{
		try
			{
			String activityId = getEventIdByState(state);
 			String message = "~TPCEQPSTATUS_GENERATEACTIVITY~USERID "+promisUser+"~PWD "+promisPassword+"~EQPID "+equipment+"~ACTIVITYID "+activityId+"~ATTRIBUTELIST 1"+"~START_TIME~"+startTime+"~END~";
			String reply = promisConnection.getReply(message);
			// If reply is not successful throw exception
			if (!reply.startsWith("SUCCESS") && !reply.startsWith("~SUCCESS")) throw new IOException("Promis error: "+reply);			
			}
		catch (Throwable throwable) {logger.error("send\tequipment\t"+equipment+"\tstate\t"+state+"\tstartTime\t"+startTime,throwable);}
		}
	
	/**
	 * @param state the e10 state
	 * @returns the event id for a given E10 state 
	 * EquipmentEvent.PRODUCTIVE       : Event.EQUIP_START
	 * EquipmentEvent.STANDBY          : Event.EQUIP_STOP
	 * EquipmentEvent.UNSCHEDULED_DOWN : Event.ALARM_SET
	 * EquipmentEvent.SCHEDULED_DOWN   : Event.GOTO_SCHEDULED_DOWNTIME
	 * EquipmentEvent.ENGINEERING      : Event.GOTO_ENGINEERING
	 */
	public static String getEventIdByState(String state)
		{
		String eventId = "";
		if      (state.equals(EquipmentEvent.PRODUCTIVE))       eventId = Event.EQUIP_START;
		else if (state.equals(EquipmentEvent.STANDBY))          eventId = Event.EQUIP_STOP;
		else if (state.equals(EquipmentEvent.UNSCHEDULED_DOWN)) eventId = Event.ALARM_SET;
		else if (state.equals(EquipmentEvent.SCHEDULED_DOWN))   eventId = Event.GOTO_SCHEDULED_DOWNTIME;
		else if (state.equals(EquipmentEvent.ENGINEERING))      eventId = Event.GOTO_ENGINEERING;
		return eventId;
		}

	/**
	 * 
	 * @param the event id according to the EPR specification
	 * @return the E10 that corresponds to the eventId
	 * Event.EQUIP_START : EquipmentEvent.PRODUCTIVE
	 * Event.GOTO_PRODUCTION_READY, Event.EQUIP_STOP or Event.ALARM_CLEAR : EquipmentEvent.STANDBY
	 * Event.GOTO_ENGINEERING : EquipmentEvent.ENGINEERING
	 * Event.GOTO_SCHEDULED_DOWNTIME : EquipmentEvent.SCHEDULED_DOWN
	 * Event.GOTO_UNSCHEDULED_DOWNTIME or Event.ALARM_SET : EquipmentEvent.UNSCHEDULED_DOWN
	 * Event.GOTO_NON_SCHEDULED : EquipmentEvent.NON_SCHEDULED
	 */
	public static String getStateByEventId(String eventId)
		{
		String state = "";
		if      (eventId.equals(Event.EQUIP_START))             state = EquipmentEvent.PRODUCTIVE;
		else if (eventId.equals(Event.GOTO_PRODUCTION_READY) || 
				 eventId.equals(Event.EQUIP_STOP)            ||
				 eventId.equals(Event.ALARM_CLEAR))             state = EquipmentEvent.STANDBY;
		else if (eventId.equals(Event.GOTO_ENGINEERING))        state = EquipmentEvent.ENGINEERING;
		else if (eventId.equals(Event.GOTO_SCHEDULED_DOWNTIME)) state = EquipmentEvent.SCHEDULED_DOWN;
		else if (eventId.equals(Event.GOTO_UNSCHEDULED_DOWNTIME) ||
				 eventId.equals(Event.ALARM_SET))               state = EquipmentEvent.UNSCHEDULED_DOWN;
		else if (eventId.equals(Event.GOTO_NON_SCHEDULED))      state = EquipmentEvent.NON_SCHEDULED;
		return state;
		}

	/**
	 * Perform state transition on an equipment event
	 * @param equipmentEvent The equipment event
	 * @param oldState The previous state for the machine in equipment event
	 */
	private void transition(EquipmentEvent equipmentEvent, String oldState)
		{
		if (equipmentEvent.state != null && !equipmentEvent.state.equals("")) return;  // If the equipment specified the state (smart tool) take it. No need to look at the event
		String newState = oldState; // Stay in the same state unless event takes to another one
		String event = equipmentEvent.event.id;		
		if (event.equals(Event.EQUIP_INFO)) newState = oldState;
		else newState = getStateByEventId(event);
		// Otherwise, the event can't change the state
		equipmentEvent.state = newState;	
		}

	public void updateDurationForDurableStatus(Machine durable, DurableStatus olddurableStatus, Map exceptionAttributes, TransactionManager txManager) throws Throwable
		{
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		DurableStatus durableStatus = (DurableStatus) durable.context.get("durableStatus");
		long fromTime = simpleDateFormat.parse(olddurableStatus.TIME).getTime();
		long toTime = simpleDateFormat.parse(durableStatus.TIME).getTime();
		long duration = toTime - fromTime;
		olddurableStatus.duration = String.valueOf(duration);
		if(txManager!=null)
			{
			txManager.store(olddurableStatus);
			}
		else
			{
			Persistence.store(olddurableStatus);
			}
		if(!durable.eIName.equalsIgnoreCase(olddurableStatus.DURABLE_ID)){
			machines.put(durable.mESName,durable);
			machines.remove(olddurableStatus.DURABLE_ID);
		}
		executeTimebasedRules(durable, olddurableStatus,exceptionAttributes);
		if(txManager!=null)
			{
			txManager.store(durableStatus);
			}
		else
			{
			Persistence.store(durableStatus);
			}
		}
	
	public void updateDurationForDurableStatus(Machine durable, DurableStatus olddurableStatus, Map exceptionAttributes) throws Throwable
		{
		updateDurationForDurableStatus(durable, olddurableStatus, exceptionAttributes,null);
		}
	/**
	 * 
	 * @param exceptionAttributes do not update attributes that are in the exceptionAttributes map
	 */
	private void executeTimebasedRules (Machine durable, DurableStatus olddurableStatus, Map exceptionAttributes) throws Throwable {
		Vector<BusinessRule> eprRules=durable.getEquipmentType().getBusinessRulesBy(BusinessRule.NEW_STATUS);
		DurableStatus durableStatus = getDurableStatus(durable);
		CurrentDurableStatus previousDurableStatus = new CurrentDurableStatus(olddurableStatus,durable);
		for(int i=0;i<eprRules.size();i++)
			{
			boolean timeelapsedflag = false;
			boolean ruleconditionflag = false;
			BusinessRule rule = (BusinessRule)eprRules.get(i);
			if(rule.conditionsTrue(durableStatus)) timeelapsedflag = true;
			if(rule.conditionsTrue(previousDurableStatus))ruleconditionflag = true;//check preiousDurableStatus as 			
			Vector actions = rule.getActions();
			BusinessRuleAction action=null;			
			Vector durableAttributes=null;
			Map stateParams=null;
			String emailaddress = null;
			for (int j = 0; actions!=null && j < actions.size(); j++)
				{
				action = (BusinessRuleAction)actions.get(j);
				if(action.getAction().equals(BusinessRuleAction.UPDATE_DURABLE))
					{
					if(durableAttributes==null )durableAttributes= new Vector();
					durableAttributes.add(action.getActionParameters().get(TransitionAction.PARAM_ATTRIBUTE));
					}
				}
			for (int j=0;durableAttributes!=null&&j<durableAttributes.size();j++)
				{
				String var = (String)durableAttributes.get(j);
				if (ruleconditionflag&&!exceptionAttributes.containsKey(var)){
					attributeUpdated(durableStatus,var,olddurableStatus.duration, "time", false);			
				}
				List attributes = getThresholdCountersAttributes(durable.getEquipmentType(),var,"time");
				for (int k = 0 ; k <attributes.size() ; k ++) {
					String timeelapseflag = ((EquipmentAttribute) (attributes.get(k))).name+ThresholdCounter.SEPARATOR+"countTimeElapse"; 
					durableStatus.ATTRIBUTES.put(timeelapseflag, String.valueOf(timeelapsedflag));
				}				
				}
			}		
	}
	
	private void executeEPRRules (Machine durable,Status status) throws Throwable {
		Vector<BusinessRule> eprRules=durable.getEquipmentType().getBusinessRulesBy(BusinessRule.EPR_EVENT);
		for(int i=0;i<eprRules.size();i++)
			{
			executeEprRule(durable,eprRules.get(i),status);
			}
		Map<String, List<String>> map = durable.getChildren();
		Iterator it = map.keySet().iterator();
		while (it.hasNext()){
			List<String> childrenvector = map.get(it.next());
			if (childrenvector!=null&& !childrenvector.isEmpty()) {
				for (int j = 0; j< childrenvector.size(); j++) {
					executeEPRRules(getMachine(childrenvector.get(j).toString()),status);
				}
			}
		}
	}
	
	/**
	 * @param row a string with the record to store
	 * since 4.2
	 * */
	private void write(String row)
		{
		Status status;
		Machine mac=null;
		try
			{
			status = getStatus(row);
			 mac = getMachine((String) status.get("MID"));
			if (mac != null && "Tester".equalsIgnoreCase(mac.getEquipmentType().name))
				{
				Persistence.store(status);
				mac.setCurrentStatus(status);
				}
			}
		catch (Throwable t)
			{
			logger.error("write\tMachine " + mac.mESName+";Row:"+row,t );
			}
		}	
	
	private void executeEprRule(Machine durable, BusinessRule rule,Status status)throws Throwable
		{
		DurableStatus durableStatus = getDurableStatus(durable);
		CurrentDurableStatus currentDurableStatus = new CurrentDurableStatus(durableStatus,durable);
		if(!rule.conditionsTrue(currentDurableStatus))return;
		Vector actions = rule.getActions();
		BusinessRuleAction action=null;
		Vector durableAttributes=null;
		Map stateParams=null;
		String emailaddress = null;
		Hashtable enableLogMap = new Hashtable();
		for (int i = 0; actions!=null && i < actions.size(); i++)
			{
			action = (BusinessRuleAction)actions.get(i);
			if (action.getAction().equals(BusinessRuleAction.UPDATE_DURABLE)
					|| action.getAction().equals(BusinessRuleAction.LOG_UPDATE_DURABLE))
				{
				if (durableAttributes == null) durableAttributes = new Vector();
				Map<String, String> actionParameters = action.getActionParameters();
				String tmpattribute = actionParameters.get(TransitionAction.PARAM_ATTRIBUTE);
				durableAttributes.add(tmpattribute);
				if (action.getAction().equals(BusinessRuleAction.UPDATE_DURABLE))
					{
					enableLogMap.put(tmpattribute, Boolean.FALSE);
					}
				else
					{
					enableLogMap.put(tmpattribute, Boolean.TRUE);
					}
				}
			else if(action.getAction().equals(BusinessRuleAction.UPDATE_STATE_VIA_EPR ))
				{
				stateParams = action.getActionParameters();
				if(durableAttributes==null )durableAttributes= new Vector();
				durableAttributes.add("STATE");
				enableLogMap.put("STATE", Boolean.TRUE);
				}
			else if(action.getAction().equals(BusinessRuleAction.SEND_EMAIL ))
				{
				emailaddress = action.getActionParameters().get(TransitionAction.PARAM_TO);					
				}
			else if(action.getAction().equals(BusinessRuleAction.UPDATE_MULTIPLE))
				{
				if(durableAttributes == null){durableAttributes = new Vector();}
				String incVar = (String)durable.context.get("INC_VARIABLE");
				durableAttributes.add(incVar);
				enableLogMap.put(incVar,Boolean.FALSE);
				}
			else if(action.getAction().equals(BusinessRuleAction.UPDATE_MULTIPLE_BY_INDEX))
				{
				Map<String, String> params = action.getActionParameters();
				String field = params.get("FIELD");
				String replace = params.get("REPLACE");
				if(field == null || replace == null){continue;}
				Object tmp = durable.get(field);
				if(tmp == null){continue;}
				String value = tmp.toString();
				replace = replace.replace("?",value);
				if(durableAttributes == null){durableAttributes = new Vector();}
				durableAttributes.add(replace);
				enableLogMap.put(replace,Boolean.FALSE);
				}
			}
		if (durableAttributes != null)
			{
			updateDurableswithEPR(status, durableAttributes, stateParams, durable, enableLogMap);
			}
		if (emailaddress!=null&& !currentDurableStatus.STATE.equalsIgnoreCase((String)status.get("STATE"))){sendEmailwithEPR(currentDurableStatus,rule,emailaddress);}
		}
	
	private Map getChildren(IStatus epr)
		{
		Iterator it = childNames.keySet().iterator() ;
		Map results = new HashMap();		
		while(it.hasNext())
			{
			String value=null;
			String name = (String)it.next();
			
			try{
			if(epr.has(name))
				{
				value=(String)epr.get(name);
				if(value!=null && !value.trim().equals(""))
					{
					Machine mac = (Machine)machines.get(value);
					if(mac!=null){results.put(name,mac);}
					}
				}
			}
			catch(NullPointerException e){e.printStackTrace();}
			}
		return results;
		}
	
	public CurrentDurableStatus getCurrentDurableStatus(Machine durable)throws Throwable
		{
		CurrentDurableStatus current = new CurrentDurableStatus();
		current.EQUIPMENT_PK = durable.primaryKey;
		Vector v = (Vector) Persistence.search(current);
		if(v!=null && v.size()>0){
			current = (CurrentDurableStatus) v.get(0);
			current.setEquipmentType(durable.getEquipmentType());
			return current;
		}
		else {
			return null;
		}
		}
	
	public DurableStatus getDurableStatus(Machine durable)throws Throwable
		{
		DurableStatus durableStatus = (DurableStatus) durable.context.get("durableStatus");
		if (durableStatus != null)
			{
			return durableStatus;
			}
		else
			{
			CurrentDurableStatus current = new CurrentDurableStatus();
			current.EQUIPMENT_PK = durable.primaryKey;
			Vector v = (Vector) Persistence.search(current);
			if (v != null && v.size() > 0)
				{
				current = (CurrentDurableStatus) v.get(0);
				durableStatus = new DurableStatus();
				durableStatus.STATUS_PK = current.STATUS_PK;
				durableStatus.DURABLE_ID = current.eIName;
				durableStatus.STATE = current.STATE;
				durableStatus.SUBSTATE = current.SUBSTATE;
				durableStatus.EQUIPMENT_PK = current.EQUIPMENT_PK;
				durableStatus.EVENT_PK = current.EVENT_PK;
				durableStatus.ATTRIBUTES = current.ATTRIBUTES;
				durableStatus.TIME = current.TIME;
				durableStatus.OPERATOR_ID = current.OPERATOR_ID;
				durableStatus.COMMENTS = current.COMMENTS;
				durableStatus.location = current.location;
				durableStatus.workstation = current.workstation;
				}
			return durableStatus;
			}
		}
	

	
	private boolean attributeUpdated(DurableStatus durableStatus,String name,String value, String units, boolean enableLog)
		{
		boolean updated=false;
		if(durableStatus==null || name==null || value==null)return false;
		value=value.trim();
		Machine durable = (Machine)machines.get( durableStatus.DURABLE_ID );
		EquipmentAttribute attribute=durable.getEquipmentType().getAttribute(name);
		if(attribute==null||"ThresholdCounter".equalsIgnoreCase(attribute.type))//if not found in the attributes look for in the eiName of the threshold countesrs
			{
			List countersAtributes = getThresholdCountersAttributes(durable.getEquipmentType(),name,units);
			List counters = getThresholdCounters(countersAtributes,durableStatus);
			for(int i=0;i<counters.size();i++ )
				{
				long inc = Long.parseLong(value);
				if(inc!=0)
					{
					ThresholdCounter tCounter = (ThresholdCounter)counters.get(i);
					tCounter.incrementServiceUsage(inc);
					durableStatus=tCounter.unloadToStatus(durableStatus);
					updated=true;
					}
				}
			if (updated && enableLog)
				{
				durableStatus.STATUS_PK = null;
				durableStatus.COMMENTS = null;
				durableStatus.putNewComments(durableStatus.OPERATOR_ID , formatter.format(new Date()), "Automatic update");			
				}
			return updated;
			}			
			if ("Counter".equalsIgnoreCase(attribute.type))
				{// summing variables
				Integer inc = Integer.valueOf(value.toString());
				if (inc != 0)
					{
					Integer current = Integer.valueOf(0);
					Object tmp = durableStatus.ATTRIBUTES.get(name);
					if (tmp != null) current = Integer.valueOf(tmp.toString());
					inc += current;
					durableStatus.ATTRIBUTES.put(name, String.valueOf(inc));
					updated = true;
					}
				}
			else if ("Text".equalsIgnoreCase(attribute.type))
				{
				String current = (String) durableStatus.ATTRIBUTES.get(name);
				if (!value.equals(current))
					{
					durableStatus.ATTRIBUTES.put(name, value);
					updated = true;
					}
				}
		if (updated && enableLog)
			{
			durableStatus.STATUS_PK = null;
			durableStatus.COMMENTS = null;
			durableStatus.putNewComments(durableStatus.OPERATOR_ID , formatter.format(new Date()), "Automatic update");
			}
		return updated;
		}
	/**
	 * @param counters: a list of attributes that are ThresholdCounters
	 * @param status 
	 * */
	private List getThresholdCounters(List counters,DurableStatus status)
		{
		List tCounters = new Vector();
		for(int i=0;counters!=null&&i<counters.size();i++)
			{
			EquipmentAttribute att= (EquipmentAttribute)counters.get(i);
			ThresholdCounter tCounter= new ThresholdCounter(att.name);
			tCounter.loadFromStatus(status);
			tCounters.add(tCounter);
			}
		return tCounters;
		}
	/**
	 * @return a list with the attributes that are Threshold counters, have a eivariable mapped to the passed name and has required units 
	 * */
	private List getThresholdCountersAttributes(EquipmentType type, String eiName, String units)
		{
		List tCounters = new Vector();
		if (type == null) return tCounters;
		Vector attrs = type.getAttributes();
		EquipmentAttribute attr;
		for (int i = 0; attrs != null && i < attrs.size(); i++)
			{
			attr = (EquipmentAttribute) attrs.get(i);
			if (attr.type.equals(EquipmentAttribute.THRESHOLDCOUNTER))
				{
				Map<String, String> options = attr.getConfigOptions();
				if(options == null){continue;}
				String optionName = options.get("eIVariable");
				boolean add = optionName.equals(eiName);
				if(!add)
					{
					int idxWildCard =  optionName.indexOf("?");
					if(idxWildCard > -1)
						{
						String prefix = optionName.substring(0,idxWildCard);
						String suffix = optionName.substring(idxWildCard+1);
						add = eiName.startsWith(prefix) && eiName.endsWith(suffix);
						}
					}
				if (add && options.containsValue(units)){tCounters.add(attr);}
				}
			}
		return tCounters;
		}
	
	private void sendEmailwithEPR(CurrentDurableStatus durableStatus, BusinessRule businessRule, String emailaddress)throws Throwable
		{					  
		DurableEmail email = DurableManager.buildBusinessRuleAlarmEmail(durableStatus, emailaddress, businessRule);
		new Thread(email).start();			
		}
	
	private void updateDurableswithEPR(Status epr, Vector toUpdate,Map substates, Machine durable, Map enableLogMap)throws Throwable
		{	
		DurableStatus durableStatus = getDurableStatus(durable);	
		DurableStatus olddurableStatus = durableStatus.clone();//(DurableStatus)durableStatus.clone();
		// update the attributes according to the action parameter
		String variable;
		String value;
		boolean changed = false;
		boolean parentchanged = false;
		String newParent=null; 
		String oldParent=null;
		
		for (int i=0;toUpdate!=null&&i<toUpdate.size();i++)
			{
			boolean enableLog = false;
			variable = (String)toUpdate.get(i);
			value = (String) epr.get(variable);
			if ((Boolean)enableLogMap.get(variable))enableLog = true;
			if("STATE".equals(variable)){
			    if(! durableStatus.STATE.equalsIgnoreCase((String)epr.get("STATE")) )
			    	{
			    	if(eprPrecedence)
			    		{
			    		String state = (String)epr.get("STATE");
			    		String sub = state.toUpperCase()+"_SUBSTATE";
			    		String substate = null;
			    		if(substates.containsKey(sub))
			    			{
			    			durableStatus.SUBSTATE = substates.get(sub).toString();
			    			}
			    		else{continue;}
			    		
			    		durableStatus.STATUS_PK=null;	
			    		durableStatus.STATE = state;
			    		durable.state = durableStatus.STATE;
			    		durableStatus.COMMENTS=null;
						durableStatus.putNewComments((String)epr.get("OPERATOR_ID") , durableStatus.TIME, "Automatic update");
			    		changed=true;			
			    		}
			    	}
			}
			else if("WORKSTATION".equalsIgnoreCase(variable))
				{
				String parent = durable.getEquipmentType().parent;
	
				
				if (parent != null && !parent.trim().equals(""))
					{
					if ("Tester".equalsIgnoreCase(parent))
						{
						durableStatus.workstation = (String) epr.get("MID");
						}
					else
		                {		   
		                String parentName=parent.toUpperCase()+"_ID";
		                durableStatus.workstation = (String) epr.get(parentName);
		                }

					if (durable.workstation == null || (durableStatus.workstation != null
							&& !durable.workstation.equalsIgnoreCase(durableStatus.workstation)))
						{
						oldParent = durable.workstation;
						newParent = durableStatus.workstation;
						durable.workstation = durableStatus.workstation;
						changed = true;
						durableStatus.STATUS_PK = null;
						parentchanged = true;
						}
					}				
				}
			
			else if("LOCATION".equalsIgnoreCase(variable))
				{
				Machine tester = getMachine((String)epr.get("MID"));
				String locationstr = null;
				if (tester!=null){
					locationstr = tester.location;
				}				
				if (locationstr != null && !locationstr.trim().equals("")&& 
						!locationstr.equals(durableStatus.location))
					{
					durableStatus.location = locationstr;
					durableStatus.STATUS_PK = null;
					durable.location = locationstr;
					changed = true;
					}
				}
			else if(attributeUpdated(durableStatus,variable,value, "usage", enableLog)){changed=true;} 
			}
		if (changed)
			{
			if (durableStatus.STATUS_PK == null) {
				durableStatus.TIME = (String) epr.get("TIME");
				durableStatus.OPERATOR_ID = (String) epr.get("OPERATOR_ID");
				//not inserrt new status row if State and location and workstation not changed. 
				updateDurationForDurableStatus(durable,olddurableStatus, new HashMap()); 
			}		
			Persistence.store(durableStatus);
			durable.context.put("durableStatus",durableStatus);
			Persistence.store(durable);
			setMachine(durable);			
			}	
		if (parentchanged)
			{
			updateDurableParent(durable, oldParent, newParent);
			}
		}
	
	/**
	 * @param row String with the values for the status separated by \t  
	 * */
	private Status getStatus(String row) {
		Status status = new Status();
		String[] header = this.header.split("\t");
		String[] values = row.split("\t");
		if( values.length != header.length){throw new IllegalArgumentException("Row and Header mismatch ");}
        String col,val;  
        for(int i=0;i<header.length;i++ ){
        	col=header[i];
			val = values[i];
			status.set(col, val);
			if (col.equals("MID")){
				status.set("EQUIPMENT_PK",((Machine)machines.get(val)).primaryKey);			    
			}
		}
        status.set("PERIOD","R");
        return status;
	} 
	

	
	
	
	private String getHeader(){
		String line = "";
		for (int i=0; i<contexts.length; i++) line += contexts[i]+"\t";
		for (int i=0; i<summings.length; i++) line += summings[i]+"\t";
		return line;
	}
	
	/**
	 * Create an Equipment Event object from individual components
	 * @param machine The machine
	 * @param event The event
	 * @param time The event time
	 * @param state The state
	 * @param context An array of Strings with the context values
	 * @param summing An array of int with the summing values
	 * @return The Equipment Event
	 */
	private EquipmentEvent toEquipmentEvent(String machine, String event, String time, String state, Map context, Map summing)
		{
  		EquipmentEvent equipmentEvent = new EquipmentEvent();
  		equipmentEvent.equipment = new Equipment("",machine,"",true,new Vector());
  		equipmentEvent.state = state;
  		equipmentEvent.event = new com.freescale.sc.model.entity.event.Event(event);
  		equipmentEvent.startTime = time;
		Vector result = new Vector(); // Exclude first 2 items which are MID and STATE
		for (int i=2; i<contexts.length; i++)
			{
			String value = (String)context.get(contexts[i]);
			if (value != null) result.addElement(new Context(contexts[i],value,""));
			}
  		equipmentEvent.contexts = result;
		result = new Vector(); // Exclude first 2 which are DURATION and COUNT
		for (int i=2; i<summings.length; i++)
			{
			Integer value = (Integer)summing.get(summings[i]);
			if (value != null) result.addElement(new Summing(summings[i],value.intValue(),1,""));
			}
		equipmentEvent.summings = result;
		return equipmentEvent;
		}
	
	/**
	 * Update old context values with new context values
	 * @param context A hash table with the old context
	 * @param newContext A vector of Context objects
	 * @return True if context changed, otherwise false
	 */
	private boolean updateContexts(Map context, Vector newContext)
		{
		boolean changed = false;
		Hashtable aux = toHashtable(newContext,true);
		for (String currContext:contexts)  
		{
		   String name = currContext.toString();
		    if (name.equals("MID") || name.equals("STATE"))continue;// Exclude  MID and STATE
			String currentValue = (String)context.get(currContext);
			String msgValue  = (String)aux.get(currContext);
			if ((currentValue==null && msgValue!=null) || (currentValue!=null && msgValue!=null && !currentValue.equals(msgValue))) changed = true;
			if (msgValue==null) msgValue=currentValue==null?"":currentValue;
			try
				{
				if (columnsLength.containsKey(name))
					{
					Integer length = columnsLength.get(name);
					if (msgValue.length() > length.intValue())
						{
						logger.info("value for " + name+ " exceeds the length of the column in the DB - DB length " + length+ " value length " + msgValue.length() + " value : " + msgValue);
						msgValue = msgValue.substring(0, length);
						}
					}
				}
			catch (Throwable t)	{}
			context.put(currContext,msgValue);
			}
		return changed;
		}
	
	/**
	 * Generate a hash table with duration, count and summing values
	 * @param summing A hash table with the summing values
	 * @param duration The duration
	 * @param count The count
	 * @param summing A hashtable of Summing objects
	 */
	private void updateSummings(Map summing, int duration, int count, Map newSumming)
		{
		for (int i=0; i<summings.length; i++)
			{	
			Integer valueAux = (Integer)summing.get(summings[i]);
			int value = valueAux==null?0:valueAux.intValue();
			if (summings[i].equals("DURATION")) value += duration;
			else if (summings[i].equals("COUNT")) value += count;
			else
				{
				if (summings[i].contains("$"))valueAux = getValue(newSumming,summings[i]);
				else valueAux = (Integer)newSumming.get(summings[i]);
				value += valueAux==null?0:valueAux.intValue();		
				}
			summing.put(summings[i],new Integer(value));
			}
		}
	
	/**
	 * Return the sum of values for all variables that match a given variable name
	 * @param summing A hash table with the summing values
	 * @param variable The name of the variable with an * that will match any string
	 * @return The sum of values for all variables that match the given variable
	 */
	private Integer getValue(Map summing, String variable)
		{
		variable = variable.replace("$","");
		int value = 0;
		for (Object key:summing.keySet())
			{
			String aux = (String)key;
			if (aux.startsWith(variable)) value += ((Integer)summing.get(aux)).intValue();
			}
		return new Integer(value);
		}
	
	/**
	 * Converts a hash table of values to a tab separated string
	 * @param array The hash table
	 * @param isContext Whether these are context variable sor not (summing)
	 * @return A string
	 */
	private String hashToString(Map values, boolean isContext)
		{
		StringBuffer result = new StringBuffer();
		if (isContext)
			{
			 // Exclude first 3 which are MID, TIME and STATE
			for (int i=3; i<contexts.length; i++)
				{
				String value = (String)values.get(contexts[i]);
				if (value==null) value = "";
				result.append(value+"\t"); 
				}
			}
		else
			{
			for (int i=0; i<summings.length; i++)
				{
				Integer value = (Integer)values.get(summings[i]);
				if (value==null) value = new Integer(0);
				result.append(value+"\t");
				}
			}
		if(result.length() >0 )result.deleteCharAt(result.length()-1);
	    return result.toString();
		}

	/**
	 * Returns a hash table with pairs of names and values
	 * @param values A vector of Context or Summing elements
	 * @param isContext Whether the vector contains Context or Summing elements
	 * @return A hash table with the values
	 */
	private Hashtable toHashtable(Vector values, boolean isContext)
		{
		Hashtable hashtable = new Hashtable();
		for (int i=0; i<values.size(); i++)
			{
			if (isContext)
				{
				Context context = (Context)values.elementAt(i);
				if (context.name != null && context.value != null) hashtable.put(context.name,context.value);
				}
			else
				{
				Summing summing = (Summing)values.elementAt(i);
				hashtable.put(summing.name,new Integer(summing.value));				
				}
			}
		return hashtable;
		}
	
	public static EquipmentType getParentEquipmentType (EquipmentType type) 
	    {
		EquipmentType parent = new EquipmentType();  
		parent.name  = type.parent;
		
		if (parent.name == null || parent.name.equals("") || "N".equalsIgnoreCase(parent.name))
			{
			return null;
			}
		Vector tmp = null;
		try{tmp=Persistence.search(parent);}
		catch(Throwable t){}
		if (tmp!=null&&!tmp.isEmpty()){return (EquipmentType)tmp.get(0);}
		else {return null;}
		}
	/**
	 * 
	 * */
	public Machine getParent(Machine obj)
		{
		Machine parent = null;
		Map macs = null;
		if(obj == null || obj.workstation == null ){return parent;}
		EquipmentType tmpType= getParentEquipmentType(obj.getEquipmentType());
		if (tmpType ==null){
			return null;
		}else{
			macs = getMachines(tmpType);
		}
		if( macs.containsKey(obj.workstation)){parent = (Machine)macs.get(obj.workstation);}
		return parent;
		}
	
	
	/**
	 * update children after change done on the parent
	 * @param durable  the parent.
	 * @param isDeletion whether the operation is deletion.
	 */	
	public void syncChildrenwithParent(Machine durable, boolean isDeletion, TransactionManager txManager)throws Throwable 
		{
			Map<String, List<String>> children = durable.getChildren();
			EquipmentType type = durable.getEquipmentType();
			String coreId = "N/A";
			if (!type.name.equalsIgnoreCase("tester"))
				{
				 DurableStatus status = (DurableStatus)durable.getContext().get("durableStatus");
				 if (status!=null){coreId = status.OPERATOR_ID;}
				 }			
			if (children.isEmpty()) return;
			Set keyset = children.keySet();
			Iterator it = keyset.iterator();
			while (it.hasNext()){
				List<String> list = children.get(it.next());
				if (list!=null) {
					for (int i= 0; i< list.size();i++)
					{
						String durable_id = list.get(i).toString();
						Machine child = (Machine) getMachine(durable_id).clone();
						DurableStatus status= getDurableStatus(child);
						DurableStatus oldDStatus = status.clone();
						status.STATUS_PK = null;
						if (isDeletion){
							child.workstation="";
							status.workstation="";
						}
						else{
							if (type.name.equalsIgnoreCase("tester"))
							{
								child.workstation = durable.mESName;
								status.workstation= durable.mESName;
							}
							else
							{
								child.workstation = durable.eIName;
								status.workstation = durable.eIName;
							}
						}	
						status.COMMENTS = null;
						status.OPERATOR_ID = coreId;
						status.putNewComments(status.OPERATOR_ID , status.TIME, "update durable association");					
						Date now = new Date();	    
						status.TIME = formatter.format(now);
						txManager.store(child);
						txManager.store(status);	
						child.context.put("durableStatus", status);
						txManager.setMachine(child,this);
						updateDurationForDurableStatus(child, oldDStatus,new HashMap(),txManager);					
						
					}
				}
			}
		}
	/**
	 * update parent after child change its parent 
	 * @param durable  the parent.
	 * @param oldprarent oldparernt.
	 * @param oldprarent newparernt.
	 */	
	public void  updateDurableParent(Machine durable, String oldparent, String newparent, TransactionManager txManager) throws Throwable
		{
		if ((getParentEquipmentType(durable.getEquipmentType())==null)||oldparent!=null && oldparent.equals(newparent))
			return;			
		boolean isTester = getParentEquipmentType(durable.getEquipmentType()).name.equalsIgnoreCase("tester");
		EquipmentType type = durable.getEquipmentType();
		String coreId = "N/A";
		DurableStatus status = (DurableStatus)durable.getContext().get("durableStatus");
		if (status!=null){coreId = status.OPERATOR_ID;			}							
		if (oldparent!=null &&!oldparent.equals(""))
			{
			Machine oldparentDurable = (Machine) getMachine(oldparent);
			if (oldparentDurable!=null)
				{
				oldparentDurable = (Machine)oldparentDurable.clone();
				Map<String, List<String>> oldchildren = oldparentDurable.getChildren();
				List<String> oldsiblings = oldchildren.get(type.name);
				if (oldsiblings!=null&&!oldsiblings.isEmpty()) 
					{
					if (type.getSiblings().equalsIgnoreCase("Y"))
						oldsiblings.remove(durable.eIName);
					else 
						oldsiblings.clear();
					//update status comments
					if (!isTester)
						{
						addAssociationStatus(oldparentDurable,coreId,txManager);
						}
					if(txManager!=null)
						{
						txManager.store(oldparentDurable);
						txManager.setMachine(oldparentDurable,this);
						}
					else
						{
						Persistence.store(oldparentDurable);
						setMachine(oldparentDurable);
						}
					}									
				}
			}
		if (newparent!=null &&!newparent.equals(""))
			{	
				Machine newparentDurable = (Machine) getMachine(newparent).clone();
		    	if (newparentDurable!=null&&!newparentDurable.equals(""))
		    	{
		    		Map<String, List<String>> newchildren = newparentDurable.getChildren();
		    		List<String> newsiblings = newchildren.get(type.name);
		    		if (newsiblings!=null&&!newsiblings.isEmpty())
		    		{
		    			if (type.getSiblings().equalsIgnoreCase("N")) 
		    			{		    		
		    			    for (int i=0;i<newsiblings.size();i++)
		    			    {		    			        
		    			    	String durable_id = newsiblings.get(i).toString();
		    			    	if (durable_id.equals(durable.eIName))
		    			    		{
		    			    		continue;
		    			    		}		    		
		    			    	Machine durableMachine = (Machine) getMachine(durable_id).clone();
		    			    	durableMachine.workstation = ""; 
								addAssociationStatus(durableMachine,coreId,txManager);
								if(txManager!=null)
									{
									txManager.store(durableMachine);
									txManager.setMachine(durableMachine,this);
									}
								else
									{
									Persistence.store(durableMachine);
									setMachine(durableMachine);
									}
		    			    }
		    			    newsiblings.clear();
		    			}
		    		}else
		    		{ 
		    			newsiblings = new Vector();
		    		}		    				
		    		if (!newsiblings.contains(durable.eIName))
					{
					newsiblings.add(durable.eIName);
					}
		    		newchildren.put(type.name, newsiblings);	
		    		if (!isTester)
		    			{
		    			addAssociationStatus(newparentDurable,coreId,txManager);
		    			}
		    		if(txManager!=null)
						{
						txManager.store(newparentDurable);
						txManager.setMachine(newparentDurable,this);
						}
					else
						{
						Persistence.store(newparentDurable);
						setMachine(newparentDurable);
						}
		    	}
			}	
		}
	
	public void  updateDurableParent(Machine durable, String oldparent, String newparent) throws Throwable
		{
		updateDurableParent(durable,oldparent,newparent,null);
		}
	
	private void addAssociationStatus(Machine durable, String coreId, TransactionManager txManager) throws Throwable
		{		
		DurableStatus status = getDurableStatus(durable);	
		DurableStatus oldDStatus = status.clone();
		status.STATUS_PK = null;
		status.COMMENTS = null;
		status.OPERATOR_ID = coreId;
		status.putNewComments(status.OPERATOR_ID , status.TIME, "update durable association");		
		Date now = new Date();
		status.TIME = formatter.format(now);
		if(txManager!=null)
			{
			txManager.store(status);
			}
		else
			{
			Persistence.store(status);
			}
		durable.context.put("durableStatus", status);
		updateDurationForDurableStatus(durable, oldDStatus,new HashMap(),txManager);
		}
	
	private List<BusinessRule> getBusinessRules(String event)
		{
		List rules = new ArrayList();
		Map types = getEquipmentTypes();
		Iterator it = types.keySet().iterator();
		while (it.hasNext())
			{
			EquipmentType type = (EquipmentType) types.get(it.next());
			rules.addAll(type.getBusinessRulesBy(event));
			}
		return rules;
		}
	
	public Location getLocationByName(String location) throws Throwable {
		Location loc = new Location();
		loc.setName(location);
		Vector results = Persistence.search(loc);
		if (results!=null&&!results.isEmpty()){
			return (Location)results.get(0);
		}
		else {
		    return null;
		}
		
	}
	
	public String getProcessedJsp(String jsp,Map params)
		{
		String result ="";
		try
			{
			String charset = "UTF-8";
			String query ="automatic_request=true";
			if(params != null)
				{
				Iterator it = params.keySet().iterator();
				while(it.hasNext())
					{
					String key = (String)it.next();
					query += "&" + URLEncoder.encode(key,charset) + "=" + URLEncoder.encode((String)params.get(key),charset);  
					}
				}
			if(appUrl == null)return "";
			String connectionStr = appUrl +"/" +URLEncoder.encode(jsp,"UTF-8");
			connectionStr+="?"+query;
			URLConnection connection = new URL(connectionStr).openConnection();
			connection.setRequestProperty("Accept-Charset", charset);
			String processed;
			InputStream ins = null;
			if (((HttpURLConnection)connection).getResponseCode() == HttpURLConnection.HTTP_OK)
				{
				ins = connection.getInputStream();
				}
			else
				{
				ins = ((HttpURLConnection) connection).getErrorStream();
				}
			if(ins != null)
				{
				BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
				while((processed = in.readLine()) != null)
					{
					result+=processed;
					}
				in.close();
				}
			}
		catch(Throwable t){
			logger.error(t.getMessage(),t);
		}
		return result;
		}

	/**
	 * @param the durable to return state
	 * @return map with STATE and SUBSTATE values
	 * if there is a business rule defined for new equipments it will return the state defined in the business rule, otherwise it will return Unknown
	 * */
	public Map getInitialState(Machine durable)
		{
		Map result = new HashMap();
		result.put("STATE",Substate.UNK);
		result.put("SUBSTATE",Substate.UNK);
		EquipmentType eqType = durable.getEquipmentType();
		List rules = eqType.getBusinessRulesBy(BusinessRule.NEW_EQUIPMENT);
		if(rules != null && rules.size()>0)
			{
			BusinessRule rule = (BusinessRule)rules.get(0);
			List actions = rule.getActionsBy(BusinessRuleAction.INIT_STATE);
			if(actions != null && actions.size() >0)
				{
				BusinessRuleAction action = (BusinessRuleAction)actions.get(0);
				Map<String, String> params = action.getActionParameters();
				result.put("STATE",params.get("STATE"));
				result.put("SUBSTATE",params.get("SUBSTATE"));
				}
			}
		return result;
		}

	/**
	 * clear equipments of specific type from memory
	 * @param list the list of equipments to be cleared
	 */
	private void clearMachines(List<Machine> list)
		{
		for(Machine machine: list)
			{
			machines.remove(machine.mESName);
			pkToMESName.remove(machine.primaryKey);
			}
		}
	
	/**
	 * @return a map where the key is the Area pk and the object the Area object
	 * */
	public static Map<Integer,Area> getAreas()
		{
		if(areas == null)
			{
			areas = new HashMap<Integer,Area>();
			try
				{
				List<Area> list = (List<Area>)Persistence.search(new Area());
				for(Area area:list)
					{
					areas.put(area.getPrimaryKey(), area);
					}
				}
			catch (Throwable e)
				{
				logger.error(e.getMessage(),e);
				}
			}
		return areas;
		}
	
	/**
	 * reads the areas table to populates the area map  
	 * */
	public static boolean reloadAreas()
		{
		areas = null;
		getAreas();
		return true;
		}
	
	/**
	 * returns the area name of a given equipment
	 * @param equipmentName the name of the equipment to return the area name
	 * @return the area name that the equipment belongs to 
	 * */
	public static String getAreaNameByEquipmentName(String equipmentName)
		{
		String areaName = "";
		String areaPk = getAreaPK(equipmentName);
		if(areaPk != null)
			{
			Area area = getAreas().get(Integer.valueOf(areaPk));
			if(area != null){areaName = area.getName();}
			}
		return areaName;
		}
	
	/**
	 * Clients send the events from the equipment with the time in UTC 
	 * getEventTime returns the time of the event adjusted to the time zone of the server
	 * First it will try to retrieve the equipmentEvent.event.time, if it is null it will try to retrieve from startTime, if fails then it will return the current server date   
	 * @param equipmentEvent
	 * @return the time of the event adjusted to the
	 * */
	public Date getEventTime(EquipmentEvent equipmentEvent)
		{
		Date time = null;
		try
			{
			time = formatterGMT.parse(equipmentEvent.event.time);
			}
		catch (Exception pe)
			{
			try
				{
				time = formatterGMT.parse(equipmentEvent.startTime);
				}
			catch (Exception e)
				{
				time = new Date();
				}
			};
		return time;
		}

	
	public String[] getPerformances(){return performances;}
	public void setPerformances(String[] perf){performances = perf;}

	public String[] getContexts(){return contexts;}

	public String[] getSummings(){return summings;}

	public int getIdleInterval(){return idleInterval;}
	public void setIdleInterval(int idleInterval){this.idleInterval = idleInterval;}

	public int getWriteInterval(){return writeInterval;}
	public void setWriteInterval(int writeInterval){this.writeInterval = writeInterval;}

	public String getDiamondDirectory(){return diamondDirectory;}
	public void setDiamondDirectory(String diamondDirectory){this.diamondDirectory = diamondDirectory;}

	public ThreadSafeDMHConnection getDMHConnection(){return DMHConnection;}
	public void setDMHConnection(ThreadSafeDMHConnection dMHConnection){DMHConnection = dMHConnection;}

	public ThreadSafeServerCall getPromisConnection(){return promisConnection;}
	public void setPromisConnection(ThreadSafeServerCall promisConnection){this.promisConnection = promisConnection;}

	public String getPromisUser(){return promisUser;}
	public void setPromisUser(String promisUser){this.promisUser = promisUser;}

	public String getPromisPassword(){return promisPassword;}
	public void setPromisPassword(String promisPassword){this.promisPassword = promisPassword;}

	public boolean isMesPrecedence(){return mesPrecedence;}
	public void setMesPrecedence(boolean mesPrecedence){this.mesPrecedence = mesPrecedence;}

	public boolean isEprPrecedence(){return eprPrecedence;}
	public void setEprPrecedence(boolean eprPrecedence){this.eprPrecedence = eprPrecedence;}

	public Boolean getIsProduction(){return isProduction;}
	public void setIsProduction(Boolean isProduction){this.isProduction = isProduction;}

	public String getAppUrl(){return appUrl;}
	public void setAppUrl(String url){appUrl = url;}
	
	public String getStateChangeTesterID(){return stateChangeTesterID;}
	public void setStateChangeTesterID(String stateChangeTesterID){this.stateChangeTesterID = stateChangeTesterID;}
	
	public String getSitesFilterByTestProgramFlowId(){return sitesFilterByTestProgramFlowId;}

	public void setSitesFilterByTestProgramFlowId(String sitesFilterByTestProgramFlowId)
		{
		this.sitesFilterByTestProgramFlowId = sitesFilterByTestProgramFlowId;
		siteFilterMap = new HashMap<String, String>();
		siteFilterArrList = new ArrayList<String>();
		String testProgramflowId = null;
		if (!sitesFilterByTestProgramFlowId.equals(""))
			{
			String[] arr = sitesFilterByTestProgramFlowId.split(",");
			for (String item : arr)
				{
				String[] text = item.split("\\|");
				if (!siteFilterArrList.contains(text[0]))
					{
					siteFilterArrList.add(text[0]);
					}
				testProgramflowId = text[0] + "_" + text[1];
				siteFilterMap.put(testProgramflowId, text[2]);
				}
			}
		}

	public boolean isServerTime(){return serverTime;}
	public void setServerTime(String eventTime)
		{
		serverTime = !"client".equalsIgnoreCase(eventTime);
		}
	
	public boolean isClientAcknowledgment(){return clientAcknowledgment;}
	public void setClientAcknowledgment(boolean clientAcknowledgment) 
		{
		this.clientAcknowledgment = clientAcknowledgment;
		}
	
	/**
	 * update status of a list of machines, it will only return the required attributes for all machines dashboard.
	 * @param names list of machineIds
	 * @return
	 */
	public List<Machine> updateMachineStatus(List<String> names)
	{
	List<Machine> result = new ArrayList<Machine>();
	for(String mesName:names)
		{
		Machine machine = (Machine)machines.get(mesName);
		if(machine!=null)
			{
			Machine returnedMachine = new Machine();
			returnedMachine.mESName = machine.mESName;
			returnedMachine.context = new HashMap();
			returnedMachine.context.put("SUBSTATE", machine.context.get("SUBSTATE"));
			returnedMachine.context.put("DEVICE_NAME", machine.context.get("DEVICE_NAME"));
			returnedMachine.context.put("TEMPERATURE", machine.context.get("TEMPERATURE"));
			returnedMachine.context.put("LOADBOARD_ID", machine.context.get("LOADBOARD_ID"));
			returnedMachine.context.put("TEST_PROGRAM", machine.context.get("TEST_PROGRAM"));
			returnedMachine.context.put("FLOW_ID", machine.context.get("FLOW_ID"));
			returnedMachine.context.put("LAST_STATE_TIME", machine.context.get("LAST_STATE_TIME"));
			returnedMachine.state = machine.state;
			returnedMachine.performance = new HashMap();
			returnedMachine.performance.put("LAST_HR_OEE", machine.performance.get("LAST_HR_OEE"));
			returnedMachine.performance.put("LOT_PROGRESS", machine.performance.get("LOT_PROGRESS"));
			returnedMachine.performance.put("CURRENT_SITE_PERF", machine.performance.get("CURRENT_SITE_PERF"));
			returnedMachine.performance.put("24_HR_SITE_PERF", machine.performance.get("24_HR_SITE_PERF"));
			returnedMachine.performance.put("LAST_HR_YIELD", machine.performance.get("LAST_HR_YIELD"));
			returnedMachine.performance.put("24_HR_YIELD", machine.performance.get("24_HR_YIELD"));
			result.add(returnedMachine);
			}
		}
	return result;
	}
	/**
	 * update full status of a lit of machines
	 * @param names a list of machines mesNames
	 * @return
	 */
	public List<Machine> getMachinesByMesNames(List<String> names)
		{
		List<Machine> result = new ArrayList<Machine>();
		for(String mesName:names)
			{
			Machine machine = (Machine)machines.get(mesName);
			if(machine!=null)
				{
				try 
					{
					result.add((Machine)machine.clone());
					} 
				catch (CloneNotSupportedException e) 
					{
					e.printStackTrace();
					}
				}
			}
		return result;
		}

	}
