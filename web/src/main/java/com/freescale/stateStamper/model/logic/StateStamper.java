package com.freescale.stateStamper.model.logic;

import com.freescale.stateStamper.model.entity.*;
import com.freescale.stateStamper.service.ModelService;
import com.freescale.sc.model.entity.event.*;
import com.freescale.serverUtil.Persistence;
import com.freescale.serverUtil.ThreadSafeSimpleDateFormat;
import com.motorola.gramms.util.net.ServerCall;
import com.freescale.serverUtil.PersistenceBrokerIterator;

import java.io.*;
import java.net.ServerSocket;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.exolab.castor.mapping.Mapping;
import org.exolab.castor.xml.XMLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
	
/**
 * Main class. Reads configuration (WEB-INF/conf.txt file) and starts running and listening for client socket connections
 */
public class StateStamper extends Thread implements TransactionMemorySyncMaster
	{
	private static ServerSocket serverSocket;
	private static Mapping mapping;
	private static StateTransition stateTransition;
	private static String site, reportsDirectory;
	private static String areaCode,plantCode,stage;

	public 	static String transferDirectory;
	public 	static String attachmentDirectory;
	private static ThreadSafeDMHConnection DMHConnection;
	private static ThreadSafeServerCall promisConnection;
	private static Summary summary;
	public static GenerateReport reports;
	private static FormatReport formatReport;
	private static Vector threads = new Vector();
	private String attrib[];
	private static Logger logger;
	private static boolean log, run, mes,eprPrec;
	private static String COUNTER= "Counter";
	private static ServletConfig servletConfig;
	private static Properties properties;
	private String baseDirectory;
	private String[] contexts;
	private String[] summings;
	private String[] performance;
	private ExternalDBSource externalDBSource;
	private int maxRaw,maxSummary;
	private String canonicalVersion;
    private GenesisServices genesisServices;
	private ThreadSafeSimpleDateFormat timeFormat = new ThreadSafeSimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
	private String instanceType;
	private Map<EquipmentType,PICConnection> picConnections;
	
	/**
	 * Initialization method
	 * @param servletConfig The servlet configuration object which contains information about this servlet
	 */
	public void init(ServletConfig servletConfig)
		{		
		logger = LoggerFactory.getLogger("stateStamper");
		baseDirectory = servletConfig.getServletContext().getRealPath("")+"/WEB-INF";
		try
			{
			StateStamper.servletConfig=servletConfig;
			loadConfiguration();
			initSettings();
			canonicalVersion = getWarInfo();
			int port = Integer.parseInt(properties.getProperty("port"));
			int idleInterval = Integer.parseInt(properties.getProperty("idleInterval"));
			int writeInterval = Integer.parseInt(properties.getProperty("writeInterval"));
			String diamondDirectory = properties.getProperty("diamondDirectory");
			String picHost = properties.getProperty("picHost");
			String picPort = properties.getProperty("picPort");
			String dmhBox = properties.getProperty("dmhBox");
			String promisPrimaryHost = properties.getProperty("promisPrimaryHost");
			String promisPrimaryPort = properties.getProperty("promisPrimaryPort");
			String promisSecondaryHost = properties.getProperty("promisSecondaryHost");
			String promisSecondaryPort = properties.getProperty("promisSecondaryPort");
			String promisUser = properties.getProperty("promisUser");
			String promisPassword = properties.getProperty("promisPassword");
			String instanceType = properties.getProperty("instanceType");
			String defaultEquipmentType = properties.getProperty("defaultEquipmentType");
			String stateChangeTesterID = properties.getProperty("stateChangeTesterID","");
			boolean isProduction = !instanceType.equalsIgnoreCase("Reporting");
			boolean schedulerMinsInterval = Boolean.parseBoolean(properties.getProperty("schedulerMinsInterval","false"));
			
			initEquipmentAttributes();
			initRetentionPolicy(properties);
			// Create thread group
			FiscalDate.setLogger(logger);
			FiscalDate.loadCalendarConnection(properties);
			FiscalDate.setShifts(properties.getProperty("shifts"),properties.getProperty("crews"));
			Vector aux = (Vector)Persistence.search(new Report());
			reports = new GenerateReport(reportsDirectory,transferDirectory,baseDirectory,aux,writeInterval);
			formatReport= new FormatReport(reportsDirectory);
			summary = new Summary(reports,contexts,summings,maxRaw,maxSummary);
			summary.setSchedulerMinsInterval(schedulerMinsInterval); 
			if (!picHost.equals(""))
				{
				DMH tmpdMHConnection = new DMH(picHost,picPort,dmhBox); // Open connection to PIC
				DMHConnection = new ThreadSafeDMHConnection(tmpdMHConnection); 
				}
			if (promisPrimaryHost !=null &&  !promisPrimaryHost.equals("")) // Open TP connection to Promis
				{
				int secondaryPort = -1;
				if (promisSecondaryPort != null) secondaryPort = Integer.parseInt(promisSecondaryPort);
				ServerCall tmpServerCall= new ServerCall(promisPrimaryHost,Integer.parseInt(promisPrimaryPort),promisSecondaryHost,secondaryPort,"TTT",30000);
				promisConnection = new ThreadSafeServerCall(tmpServerCall);
				}	
			serverSocket = new ServerSocket(port); // Create server socket
			// Load XML mapping in file mapping.xml and open server socket
			mapping = new Mapping();
			mapping.setBaseURL(baseDirectory);
			mapping.loadMapping("mapping.xml");
			XMLContext context = new XMLContext();
			context.addMapping(mapping);
			initExternalDBSource(properties);
			genesisServices = new GenesisServices(properties);
			picConnections = new HashMap<EquipmentType,PICConnection>();
			stateTransition = new StateTransition(summary,contexts,summings,performance,idleInterval,writeInterval,mes,eprPrec,baseDirectory,diamondDirectory,context,DMHConnection,promisConnection,promisUser,promisPassword, isProduction,defaultEquipmentType,stateChangeTesterID);
			stateTransition.setExternalDBSource(externalDBSource);
			stateTransition.setSitesFilterByTestProgramFlowId(properties.getProperty("sitesFilterByTestProgramFlowId",""));
			stateTransition.setServerTime(properties.getProperty("eventTime", "server"));
			stateTransition.setClientAcknowledgment(Boolean.parseBoolean(properties.getProperty("clientAcknowledgment","false")));
			ModelService.init(stateTransition,genesisServices);
			run = true;
			start();
			logger.info("State Stamper " + site+ " "+ canonicalVersion+ " started ");		
			}
		catch (Throwable throwable) {
			logger.error("Init State Stamper",throwable);}	
		}

	/** Destroy this object and all the clients */ 
	public void destroy()
			{
			logger.info("State Stamper stopping");
			run = false;
			interrupt();
			try {serverSocket.close();} catch (Throwable t) {}
			serverSocket = null;
			// Destroy all the client threads
			for (int i=0; i<threads.size(); i++)
				{
				try {((Client)threads.elementAt(i)).destroy();}
				catch (Throwable t) {}
				}
			stateTransition.destroy();
			stateTransition = null;
			try {DMHConnection.close();} catch (Throwable t) {}
			DMHConnection = null;
			try {promisConnection.closeSocket();} catch (Throwable t) {}
			promisConnection = null;
			for(Iterator<EquipmentType> it = picConnections.keySet().iterator();it.hasNext();)
				{
				PICConnection pic =  picConnections.get(it.next());
				pic.exit();
				}
			
	        Enumeration<Driver> drivers = DriverManager.getDrivers();
			while (drivers.hasMoreElements())
				{
				Driver driver = drivers.nextElement();
				try
					{
					DriverManager.deregisterDriver(driver);
					logger.info("Deregistering jdbc driver:" + driver);
					}
				catch (SQLException e)
					{
					logger.error("Error deregistering driver" + driver);
					}
				}
			logger.info("State Stamper stopped");
	        }

	
	/**
	 * Run method for this thread
	 */
	public void run()
		{
		while (run)
			{
			try {new Client(threads,serverSocket.accept(),mapping,stateTransition,log);}
			catch (Throwable t) {logger.error(t.getMessage(),t);}
			}
		}
	
	public void loadConfiguration()throws IOException
		{
		properties = new Properties();			
		properties.load(new FileInputStream(baseDirectory+"/conf.txt"));
		}
	
	private void initEquipmentAttributes()throws Throwable
		{
		// Get contexts and summings
		EquipmentAttribute attr = new EquipmentAttribute();
		attr.typeKey = new Integer(1);
		List cont = new ArrayList();
		List sum = new ArrayList();
		List attribs = new ArrayList();
		//add the default status attributes
		for(int i=0;i<Status.DEFAULT_STATUS.length;i++)
			{
			attribs.add(Status.DEFAULT_STATUS[i]);
			cont.add(Status.DEFAULT_STATUS[i]);
			}
		Vector res = Persistence.search(attr);
		//add the defined equipment attributes to att and the appropiate context or summing
		for(int i=0;res != null &&i<res.size();i++)
			{
			attr= (EquipmentAttribute)res.get(i);
			attribs.add(attr.name);
			if(attr.type.equals(COUNTER)){
				sum.add(attr.name);
			}else{
				cont.add(attr.name);
			}
			}
		attrib =  (String [])attribs.toArray(new String[0]);
		contexts = (String[])cont.toArray(new String[0]);
		summings = (String[])sum.toArray(new String[0]);
		performance = (String[])(properties.getProperty("performance","")).split(",");
		}
	
	private void initExternalDBSource(Properties properties)
		{
		String dbServer = properties.getProperty("OracleExternalDBServer","");
		String user     = properties.getProperty("OracleExternalUser","");
		String pwd      = properties.getProperty("OracleExternalPassword","");
		String port     = properties.getProperty("OracleExternalPort","");
		String SID      = properties.getProperty("OracleExternalSID","");
		try{if(externalDBSource != null){externalDBSource.close();}}
		catch (SQLException e){e.printStackTrace();}
		externalDBSource = null;
		if( dbServer.equals("") || user.equals("") || pwd.equals("") || port.equals("") || SID.equals(""))
			{
			logger.warn("Not enough information to set up the External DBSource ");
			logger.warn("dbServer:"+ dbServer+ " port:"+port +" SID:"+SID+" user:"+user+" pwd empty?"+pwd.equals(""));
			}
		else
			{
			externalDBSource = new ExternalDBSource(dbServer, SID, port, user, pwd);
			}
		}
	
	public Boolean reloadSettings()throws Throwable
		{
		loadConfiguration();
		initSettings();
		initRetentionPolicy(properties);	
		stateTransition.setPerformances(performance);
		stateTransition.setIdleInterval(Integer.valueOf(properties.getProperty("idleInterval")));
		stateTransition.setWriteInterval(Integer.valueOf(properties.getProperty("writeInterval")));
		String mesPrecedence = properties.getProperty("mesPrecedence","Y");
		boolean mes = mesPrecedence.trim().toUpperCase().equals("Y");
		stateTransition.setMesPrecedence(mes);
		String eprPrecedence = properties.getProperty("eprPrecedence","Y");
		boolean eprPrec = eprPrecedence.trim().toUpperCase().equals("Y");
		stateTransition.setEprPrecedence(eprPrec);
		stateTransition.setDiamondDirectory(properties.getProperty("diamondDirectory"));
		stateTransition.setStateChangeTesterID(properties.getProperty("stateChangeTesterID"));
		stateTransition.setSitesFilterByTestProgramFlowId(properties.getProperty("sitesFilterByTestProgramFlowId",""));
		stateTransition.setServerTime(properties.getProperty("eventTime", "server"));
		stateTransition.setClientAcknowledgment(Boolean.parseBoolean(properties.getProperty("clientAcknowledgment","false")));
		stateTransition.initColumnsLength();
		initExternalDBSource(properties);
		stateTransition.setExternalDBSource(externalDBSource);
		FiscalDate.loadCalendarConnection(properties);
		FiscalDate.reloadConnection();
		FiscalDate.setShifts(properties.getProperty("shifts"),properties.getProperty("crews"));
		summary.setMaxRaw(maxRaw);
		summary.setMaxSummary(maxSummary);
		summary.setSchedulerMinsInterval(Boolean.parseBoolean(properties.getProperty("schedulerMinsInterval","false")));
		
		reports.setReportsDirectory(reportsDirectory);
		reports.setTransferDirectory(properties.getProperty("transferDirectory"));
		reports.setWriteInterval(Integer.valueOf(properties.getProperty("writeInterval")));
		reports.initReportDefinitions();
		genesisServices = new GenesisServices(properties);
		return Boolean.TRUE;
		}
	
	private void initSettings()
		{
		site = properties.getProperty("site");
		stage = properties.getProperty("stage");
		areaCode = properties.getProperty("area_code");
		plantCode = properties.getProperty("plant_code");
		reportsDirectory = properties.getProperty("reportsDirectory");
		transferDirectory = properties.getProperty("transferDirectory");
		attachmentDirectory = properties.getProperty("attachmentDirectory");
		String logEvents = properties.getProperty("logEvents");
		String mesPrecedence = properties.getProperty("mesPrecedence");
		String eprPrecedence = properties.getProperty("eprPrecedence");
		log = logEvents != null && logEvents.trim().toUpperCase().equals("Y");
		mes = mesPrecedence != null && mesPrecedence.trim().toUpperCase().equals("Y");
		eprPrec = eprPrecedence != null && eprPrecedence.trim().toUpperCase().equals("Y");
		StateStamper.servletConfig.getServletContext().setAttribute("site",site); // Store site as application bean
		if(properties.containsKey("emailServer")){FormatReport.setEmailServer(properties.getProperty("emailServer"));}
		if(properties.containsKey("fromAddress")){FormatReport.setFromAddress(properties.getProperty("fromAddress"));}
		if(properties.containsKey("content"    )){FormatReport.setContent    (properties.getProperty("content"    ));}
		if(properties.containsKey("domain"     )){FormatReport.setDomain     (properties.getProperty("domain"     ));}
		if(properties.containsKey("fromUser"   )){FormatReport.setFromUser   (properties.getProperty("fromUser"   )+ " "+site);}
		else                                     {FormatReport.setFromUser   (FormatReport.getFromUser() + " "+site);}
		
		}
	
	/**
	 * Returns a vector of vectors with the dates and report names for a given period
	 * @param period The period
	 * @return A vector of vectors: [date1,[report1,report2,...],date2,[report1,report2,...],...
	 */
	public Vector getReports(String period) {return summary.getReports(period);}
	
	/**
	 * Reads an already generated report (isReport = true) or a data file (isReport = true) and writes it to the given output stream
	 * @param file The path to the file to read
	 * @param reponse The servlet response
	 * @param isReport Whether it is a report (true) or a data file (false)
	 * @throws Throwable
	 */
	public void getFile(String file, HttpServletResponse response, boolean isReport) throws Throwable
		{		
		if (file.endsWith(".html")) response.setContentType("text/html");
		else response.setContentType("application/vnd.ms-excel");
		OutputStream os = response.getOutputStream();
		if(isReport){
			BufferedInputStream bif = new BufferedInputStream(new FileInputStream((reportsDirectory)+"/"+file));
			try
			{
				int ch = 0;
				while((ch = bif.read())> -1) os.write(ch);
			}
			finally {bif.close(); os.close();}
			return;
		}

	    String params[]= file.split("/");
	    String period = params[0];
	    String time = params[1].substring(0,params[1].length()-4);
	    String[] equal=null,greater = null,less = null,like = null,order = null,in = null,grouping=null;
	    String outer = null;
		order = new String[]{"STATUS_PK",Boolean.toString(true)};
		//For Raw compare time with >= and <=, for other periods compare time  with =  
		if(period.equals("Raw")){
			equal = new String[]{"PERIOD",String.valueOf(period.charAt(0))};
		    int idx =time.lastIndexOf("-");
		    String day = time.substring(0,idx);
		    String shift = time.substring(idx+1,time.length());
			FiscalDate fd = new FiscalDate("Shift",day,shift);
		    greater = new String[] {"TIME", fd.startTime };
		    less = new String[] { "TIME", fd.endTime };
		}else{
			equal= new String[]{"PERIOD",String.valueOf(period.charAt(0)),"TIME",time};
		}	
		Iterator it = null;
		try{
		it=Persistence.iSearch(Status.class, attrib, equal, greater, less, like, in, grouping, order, outer);
		Object []obj;
		StringBuffer sb = new StringBuffer();
		int idx_time=-1;
		for(int i=0;i<attrib.length;i++){
			if (attrib[i].equals("TIME")&&!period.equals("Raw")){idx_time = i;continue; }//do not include the TIME column in summary
			sb.append(attrib[i]+"\t");}
		sb.deleteCharAt(sb.length()-1);
		sb.append("\n");
		os.write(sb.toString().getBytes());
		while(it!=null && it.hasNext()){
			obj=(Object[])it.next();
			sb = null;
			sb = new StringBuffer();
			for (int i = 0; i < obj.length; i++) {
				if (i==idx_time) continue; //do not include the TIME column in summary
				sb.append(obj[i]!=null?obj[i]:"");
				sb.append("\t");
			}
			sb.append("\n");
			os.write( sb.toString().getBytes() );
		}
		}

		catch (Throwable throwable) {
			logger.error("file:"+file,throwable);
		}
		finally {
			  if (it != null)((PersistenceBrokerIterator)it).destroy();
		}
		}

	
	/**
	 * Returnes a vector with statistics about each connection (String array with host, time running and number of messages) 
	 * @return Vector with the statistics
	 */
	public Vector getConnections()
		{		
		Vector result = new Vector();
		long duration = 0, messages = 0;
		for (int i=0; i<threads.size(); i++)
		{
			try
    		{
    		String[] stats = ((Client)threads.elementAt(i)).getStatistics();
    		result.addElement(stats);
    		long aux = Long.parseLong(stats[1]);
    		if (aux > duration) duration = aux;
    		messages += Long.parseLong(stats[2]);
    		} 
			catch (Throwable t) {}
    		}
		result.addElement(new String[]{"Total",""+duration,""+messages});
		return result;
	}

	/**
	 * Returns vector. First element is a vector of types, second is a vector of locations
	 * @return Vector with the data
	 */
	public List getGroups(String equipmentType) {return stateTransition.getGroups(equipmentType);}
	
	/**
	 * Returns sorted vector with String array with machine, state and time
	 * @param type If not null, return UOEE for this type only
	 * @param location If not null, return UOEE for this location only
	 * @return Vector with the data
	 */
	public List getStates(String type, String location,String equipmentType) {return stateTransition.getStates(type,location,equipmentType);}
		
	/**
	 * Returns substates, groups and colors in as a hash table
	 * @return Hash table with substates, groups and colors
	 */
	public Hashtable getColors() {return StateTransition.getColors();}
	
	/**
	 * Returns sorted vector with String array with machine and UOEE
	 * @param type If not null, return UOEE for this type only
	 * @param location If not null, return UOEE for this location only
	 * @return Vector with the data
	 */
	public Vector getUOEE(String types, String locations) {return stateTransition.getUOEE(types,locations);}
		
	/**
	 * Returns sorted vector with String array with machine id, location ,type and percentage completed 
	 * @param lotprogress, return testers with percentage completed  >= than given lotprogress
	 * @return Vector with the data
	 */
	
	public Vector getLotProgress(float lotprogress) {return stateTransition.getLotProgress(lotprogress);}
	
	
	/**
	 * Returns vector of vectors with machine data (machine, state, context and summing variables)
	 * @param machine The machine to get data from. If it is null, return all data. If MES name is not null, return for that machine only, if type is not null return for that type or if location is not null return for that location 
	 * @return Vector with the data
	 */
	public Vector getContextsSummings(Machine machine) {return stateTransition.getContextsSummings(machine);}
	
	/**
	 * Returns vector of vectors state transitions for the machines and times specified in the report
	 * @param report A report object with the machines selected and the time interval 
	 * @return Vector with the data
	 */
	public Hashtable getStateTransitions(Report report) throws Throwable
		{
		// Convert to local time
		report.startDate.setTime(report.startDate.getTime()+report.startDate.getTimezoneOffset()*60000);
		report.endDate.setTime(report.endDate.getTime()+report.endDate.getTimezoneOffset()*60000);
		report.setEquipmentType(getEquipmentTypeByName("Tester"));
		return reports.generate(report,null,false,false);
		}
	
	/**
	 * Returns vector of vectors with machine data (machine, state, context and summing variables)
	 * @param type If not null, return data for this type
	 * @param location If not null, return data for this location 
	 * @return Vector with the data
	 */
	public Vector getContextsSummings(String type, String location) {return stateTransition.getContextsSummings(new Machine(null,null,type,location));}

	/**
	 * Returns list with summary schedule: String array  with period, fiscal date, start and end time 
	 * @return list with the data
	 */
	public List getSummarySchedule() {return summary.getSummarySchedule();}
	
	/**
	 * Returns list with summary files size. Each element is a vector for Raw. Shift, Day, Week and Month files.
	 * Each element in these vectors is a String array with period and file size.
	 * @return List with the summary files size.
	 */
	public List getSummaryFiles() {return summary.getSummaryFiles();}
	
	/**
	 * Returns a vector of vectors with all the report types and subtypes
	 * @return A vector of vectors: [type1,[subtype1,subtype2,...],type2,[subtype1,subtype2,...],...
	 */
	public Vector getReportTypes(EquipmentType type) 
		{
		return reports.getReportTypes(type);
		}
	
	
	/**
	 * @param Name: the name of the item to return
	 * @return a Machine object given 
	 * */
	public Machine getMachine(String name)
		{
		return stateTransition.getMachine(name) ;
		}

	/**
	 * Returns a hahstable of Machine objects
	 * @return The hashtable
	 */
	public Map getMachines() throws Throwable
		{
		return stateTransition.getMachines();
		}
	
	/**
	 * Returns a hahstable of Machine objects
	 * @return The hashtable
	 */
	public Map getAllDurables() throws Throwable
		{
		return stateTransition.getAllDurables();
		}
	
	/**
	 * get machines under specific equipment type and specific area
	 * @param input a list of objects, the first is search equipment type, the second is search area.
	 * @return
	 */
	public Map getMachines(Object[] input) 
	    {
		if(input==null||input.length<2){
			return new HashMap();
		}
		EquipmentType et = (EquipmentType) input[0];
		Area area = (Area) input[1];
		Integer areaPK = (area!=null)?area.getPrimaryKey():Area.AREA_ALL_PK; 
		return getMachines(et,areaPK);
		}
	
	
	/**
	 * get machines under specific equipment type and specific area
	 * @param equipmentType
	 * @param areaPK
	 * @return
	 */
	public Map getMachines(EquipmentType equipmentType, Integer areaPK) 
	    {
		if(equipmentType == null)
			{
			equipmentType = getDefaultEquipmentType(); 
			}
		return stateTransition.getMachines(equipmentType,areaPK);
		}
	
	/**
	 * returns s Hash map of Key=Integer(primaryKey),Value=EquipmentTypes objects
	 * */
	public Map getEquipmentTypes()
		{
		return stateTransition.getEquipmentTypes();
		}
	
	public List<Area> getLoginAreas()
		{
		List<Area> v = new ArrayList<Area>();
		try
			{
			Vector<Area> search = Persistence.search(new Area());
			for(Area area:search)
				{
				if(!area.getPrimaryKey().equals(Area.AREA_ALL_PK))
					{
					v.add(area);
					}
				}
			}
		catch (Throwable e)
			{
			e.printStackTrace();
			}
		return v;
		}
	
	/**
	 * @return HashMap contains all area object
	 */
	public Map<Integer,Area> getAreas()
		{
		return StateTransition.getAreas();
		}
	
	/**
	 * @param equipmentTypePk the primary key of the equipmentType to retrieve
	 * @return an EquipmentType
	 * */
	public EquipmentType getEquipmentType(Integer equipmentTypePk)
		{
		return StateTransition.getEquipmentType(equipmentTypePk);
		}
	
	/**
	 * 
	 * @param the name of the equipmentType to look for
	 * @return the equipmentType that match with the name passed, null if not found
	 * */
	public EquipmentType getEquipmentTypeByName(String name)
		{
		return  stateTransition.getEquipmentTypeByName(name);
		}
	
	/**
	 * get area object by name
	 * @param name area name
	 * @return area object
	 */
	public Area getAreaByName(String name)
		{
		return  stateTransition.getAreaByName(name);
		}
	
	/**
	 * Returns a hashtable with the fiscal dates for each period
	 * @return The hashtable:  Shift=[2008-01-01-A,2008-01-01-B,...],Day=[2008-01-01,2008-01-02,...],Week=[2008-01,...],Month=[2008-01,...]
	 */
	public Map getDates() {return summary.getDates();}

	/**
	 * Store a report settings in the array of reports
	 * @param report The Report object with the settings
	 * @return An Integer with the index in the reports array
	 */
	public Integer storeReport(Report report) {return reports.storeReport(report);}

	/**
	 * Retrieve a report from the given index
	 * @param index The index for the stored report
	 * @return
	 */
	public Report getReport(int index) {return reports.getReport(index);}
	
	/**
	 * Generates a report 
	 * @param index Index to the Report array where the settings for this report are defined
	 * @param reponse The servlet response
	 * @throws Throwable
	 */
	public void generateReport(Report report, HttpServletResponse response) throws Throwable
		{
		// Convert to local time
		if (report.period.equals("Raw"))
			{
			report.startDate.setTime(report.startDate.getTime()+report.startDate.getTimezoneOffset()*60000);
			report.endDate.setTime(report.endDate.getTime()+report.endDate.getTimezoneOffset()*60000);
			}
		if (report.isHTML.booleanValue()) response.setContentType("text/html");
		else
			{
			response.setContentType("application/vnd.ms-excel");
			String name= FormatReport.title(report,!report.period.equals("Raw")).replaceAll(" ","_");
			response.addHeader("Content-Disposition","attachment;filename="+name+".xls");
			}
		if(report.getEquipmentType()== null || report.getEquipmentType().name == null ){report.setEquipmentType(getEquipmentTypeByName("Tester"));}
		reports.generate(report,response.getOutputStream(),false,false);
		}
	
	/**
	 * Returns a vector with all the objects stored in a file for that object type 
	 * @param object An object
	 * @return The vector
	 * @throws Throwable
	 */
	public Vector search(Object object) throws Throwable {
		return Persistence.search(object);
		}
	
	public List searchMachine(Machine machine,List fields)
		{
		List results= new Vector();
		List machines = new Vector(stateTransition.getMachines().values());
		if(fields==null || fields.size()==0)
			{ 
			results.addAll(machines);
			return results;
			}
		Machine other;
		boolean equal;
		for(int i=0;machines!=null&&i<machines.size();i++)
			{
			other = (Machine)machines.get(i);
			equal=true;
			for(int j=0;j<fields.size()&&equal;j++)
				{
				String field = (String)fields.get(j);
				Object src = machine.get(field);
				Object toCompare = other.get(field);
				equal = src.equals(toCompare) ;
				}
			if(equal)
				{
				results.add(other);
				}
			}
		return results;
		}

	/**
	 * Stores an object in the files system with all the objects of its type 
	 * @param object The object to store
	 * @throws Throwable
	 */
	public void store(Object object) throws Throwable
		{
		TransactionManager txManager = new TransactionManager();
		boolean isNewTester = false;
		try
			{
			txManager.beginTransaction();
			if (object instanceof Machine)
				{
				Machine machine = (Machine) object;
				EquipmentType type = getEquipmentType(machine.equipmentType_pk);
				if (!type.name.equalsIgnoreCase("Tester"))
					{
					storeDurable(machine, txManager);
					}
				else
					{
					String newLocName = ((Machine) object).location;
					String oldLocName = null;
					Machine edittingMachine = new Machine();
					isNewTester = machine.primaryKey == null || machine.primaryKey.equals(""); 
					if (isNewTester)
						{
						edittingMachine = null;
						}
					else
						{
						edittingMachine.primaryKey = machine.primaryKey;
						Vector tmp = Persistence.search(edittingMachine);
						if (tmp == null || tmp.isEmpty())
							{
							edittingMachine = null;
							}
						else
							{
							edittingMachine = (Machine) tmp.get(0);
							oldLocName = edittingMachine.location;
							}
						}
	
					txManager.store(object);
					DurableManager.updateLocationCount((Machine) object, newLocName, oldLocName, txManager);
					if (edittingMachine != null && (!edittingMachine.mESName.equals(((Machine) object).mESName)))
						{
						stateTransition.syncChildrenwithParent((Machine) object, false, txManager);
						}
	
					}
				}
			else if (object instanceof Location)
				{
				storeLocation(object, txManager);
				}
			else if (!(object instanceof Vector))
				{
				txManager.store(object);
				}
			else if (object instanceof Vector && !((Vector) object).isEmpty())
				{
				Vector v = (Vector) object;
				Object objAux = v.elementAt(0);
				if (editInTable(objAux))
					{
					for (Iterator iterator = v.iterator(); iterator.hasNext();)
						{
						objAux = (Object) iterator.next();
						txManager.store(objAux);
						}
					}
				}
			txManager.executeBatch();
			}
		catch(Exception ex)
			{
			txManager.rollback();
			logger.error("object:"+object.toString(),ex);
			throw new Exception("Batch Update Failed - " + ex.getMessage());
			}
		finally 
			{
			txManager.close();
			}
		if (object instanceof Report) reports.setReports(search(new Report()));
		else if (object instanceof Machine) 
			{
			if( isNewTester )
				{
				stateTransition.initTesterState((Machine)object, new HashMap());
				}
			Vector v = new Vector();
			v.add(object);
			stateTransition.setMachines(v);
			}
		else if (object instanceof Substate) stateTransition.setStates(search(new Substate()));
		else if (isEquipmenTypeMember(object))
			{
			refreshMachinesAfterEqTypeChange(object);
			}
		else if (object instanceof Vector)
			{
			Vector vector = (Vector) object;
			if (vector.size() >= 1)
				{
				Object first = vector.firstElement();
				if (first instanceof Machine) 
					{
					stateTransition.setMachinesByEquipmentType(((Machine)first).getEquipmentType());
					}
				else if (first instanceof Substate)
					{
					stateTransition.setStates(search(new Substate()));
					}
				else if (isEquipmenTypeMember(first))
					{
					refreshMachinesAfterEqTypeChange(first);
					}
				}
			}
	    }
	/**
	 * Refresh machines after equipment type change. 
	 * The equipment type change includes EquipmentType,EquipmentAttribute,BusinessRule,BusinessRuleAction,BusinessRuleCondition change.
	 * After those changes, the equipment types and machines data need to be refreshed by equipment type
	 * @param obj
	 * @throws Throwable
	 */
	private void refreshMachinesAfterEqTypeChange(Object obj) throws Throwable{
		stateTransition.setEquipmentTypes(search(new EquipmentType()));
		Integer equipmentTypePk=null;
		if(obj instanceof EquipmentType)
			{
			equipmentTypePk=((EquipmentType)obj).primaryKey;
			}
		else if(obj instanceof EquipmentAttribute)
			{
			equipmentTypePk=((EquipmentAttribute)obj).typeKey;
			}
		else if(obj instanceof BusinessRule)
			{
			equipmentTypePk=((BusinessRule)obj).getEquipmentTypePk();
			}
		else if(obj instanceof BusinessRuleAction||obj instanceof BusinessRuleCondition)
			{
			BusinessRule rule = new BusinessRule();
			if(obj instanceof BusinessRuleAction)
				{
				rule.setPrimaryKey(((BusinessRuleAction)obj).getBusinessRulePk());
				}
			else if(obj instanceof BusinessRuleCondition)
				{
				rule.setPrimaryKey(((BusinessRuleCondition)obj).getBusinessRulePk());
				}
			rule = (BusinessRule) Persistence.search(rule,null);
			equipmentTypePk=rule.getEquipmentTypePk();
			}
		EquipmentType et = StateTransition.getEquipmentType(equipmentTypePk);
		stateTransition.setMachinesByEquipmentType(et);
	}
	
	private boolean editInTable(Object obj)
		{
		return obj instanceof EquipmentAttribute    || obj instanceof Substate     ||
		       obj instanceof TransitionCondition   || obj instanceof TransitionAction ||
		       obj instanceof BusinessRule          || obj instanceof BusinessRuleCondition || obj instanceof BusinessRuleAction;
		}
	
	private boolean isEquipmenTypeMember(Object obj)
		{
		return obj instanceof EquipmentType || obj instanceof EquipmentAttribute    ||
		       obj instanceof BusinessRule  || obj instanceof BusinessRuleCondition || obj instanceof BusinessRuleAction;
		}
	
	/**
	 * Deletes an object from the file where all the objects of its type are stored 
	 * @param object The object to delete
	 * @throws Throwable
	 */
	public void delete(Object object) throws Throwable
		{
		TransactionManager txManager = new TransactionManager();
		try
			{
			txManager.beginTransaction();
			txManager.delete(object);
			if (object instanceof Machine)
				{
				Machine machine = (Machine) object;
				if (machine.workstation != null && !machine.workstation.equalsIgnoreCase(""))
					{
					updateDurableParent(machine, machine.workstation, null, txManager);
					}
				stateTransition.syncChildrenwithParent(machine, true, txManager);
				genesisServices.deleteMachine(machine);
				}
			txManager.executeBatch();
			}
		catch (Exception ex)
			{
			txManager.rollback();
			logger.error("object:"+object.toString(),ex);
			throw new Exception("Batch Update Failed -" + ex.getMessage() );
			}
		finally 
			{
			txManager.close();
			}
		if (object instanceof Report) reports.setReports(search(new Report()));
		else if (object instanceof Machine) stateTransition.removeMachine((Machine)object);
		else if (object instanceof Substate) stateTransition.setStates(search(new Substate()));
		else if (object instanceof EquipmentType) stateTransition.setEquipmentTypes(search(new EquipmentType()));
		else if (object instanceof BusinessRule) stateTransition.setEquipmentTypes(search(new EquipmentType()));
		else if (object instanceof BusinessRuleCondition) stateTransition.setEquipmentTypes(search(new EquipmentType()));
		else if (object instanceof BusinessRuleAction) stateTransition.setEquipmentTypes(search(new EquipmentType()));
		else if (object instanceof EquipmentAttribute) stateTransition.setEquipmentTypes(search(new EquipmentType()));
		}		

	/**
	 * @return a String with the following information from the war
	 * Implementation-Version '\t' Build-Creation-Date (YYYYMMDD)_SCM Revision
	 * */
	public String getWarInfo()
		{
		if(canonicalVersion == null)
			{
			StringBuffer result= new StringBuffer();
			String appServerHome = servletConfig.getServletContext().getRealPath("/");
			File manifestFile = new File(appServerHome, "META-INF/MANIFEST.MF");
			Manifest mf = new Manifest();
			try
				{
				mf.read(new FileInputStream(manifestFile));
				} 
			catch (IOException e){e.printStackTrace();}
			Attributes atts = mf.getMainAttributes();
			result.append(atts.getValue("Implementation-Version"));
			result.append("\t");
			result.append(atts.getValue("Build-Creation-Date"));
			result.append("_");
			result.append(atts.getValue("Implementation-Build"));
			canonicalVersion = result.toString();
			}
		return canonicalVersion;
		}
	
	public String getSiteName(){return site;}
	
public EquipmentType  getParentEquipmentType (EquipmentType equipmentType)throws Throwable
	{
	return StateTransition.getParentEquipmentType(equipmentType);
	}

public List getLocationList(Machine durable) throws Throwable 
	{
		return DurableManager.getLocationList(durable);
	}

public List getParentList(Machine durable) throws Throwable
    {
	List posibleParents = new ArrayList();
	EquipmentType equipmentType = durable.getEquipmentType();
	EquipmentType parentType =  getParentEquipmentType(equipmentType);		
	if (parentType == null)
		{
		return posibleParents;
		}
	Map parents = getMachines(parentType,durable.getArea_pk());
	parents = getAvailableParents(equipmentType,parents);
	parents = getFilteredParents(durable,parents);
	posibleParents.addAll(parents.keySet());
	Collections.sort(posibleParents);			
	return posibleParents;
	}

public List getParentListFilterFields(EquipmentType eqType)
	{
	List fields = new ArrayList();
	List rules = eqType.getBusinessRulesBy(BusinessRule.ASSOCIATE);
	if(rules!= null && rules.size() > 0)
		{
		List conditions = ((BusinessRule)rules.get(0)).getConditions() ;
		for(int i =0;conditions != null && i < conditions.size() ;i++)
			{
			fields.add(((BusinessRuleCondition)conditions.get(i)).getAttribute());
			}
		}
	return fields;
	}

private Map getAvailableParents(EquipmentType equipmentType,Map parents)
	{
	Map availables = new HashMap();
	if ("Y".equalsIgnoreCase(equipmentType.getSiblings()))
		{
		availables.putAll(parents);
		}
	else
		{
		Iterator it = parents.keySet().iterator();
		while (it.hasNext())
			{
			// filter out the occupied items
			Machine parent = (Machine) parents.get(it.next());
			Map<String, List<String>> children = parent.getChildren();
			List<String> equipments = children.get(equipmentType.name);
			if (equipments == null || equipments.isEmpty())
				{
				availables.put(parent.mESName, parent);
				}
			}
		}
	return availables; 
	}

private Map getFilteredParents(Machine durable,Map parents)
	{
	Map filtered = new HashMap();
	EquipmentType equipmentType = durable.getEquipmentType();
	List rules = equipmentType.getBusinessRulesBy(BusinessRule.ASSOCIATE);
	if(rules != null && rules.size() > 0)
		{
		Collection macs = parents.values();
		
		for(int i = 0; i < rules.size();i++)
			{
			Iterator it = macs.iterator();
			while(it.hasNext())
				{
				Machine parent = (Machine)it.next();
				if(isParentAssociable(durable,parent,(BusinessRule)rules.get(i))){filtered.put(parent.mESName,parent);}
				}
			}
		}
	else
		{
		filtered.putAll(parents);
		}
	return filtered;
	}

private boolean isParentAssociable(Machine durable,Machine parent,BusinessRule rule)
	{
	boolean associable = false;
	List conds = rule.getConditions();
	for(int i = 0; i < conds.size();i++)
		{
		BusinessRuleCondition condition = (BusinessRuleCondition)conds.get(i);
		String parentField;
		Object tmp;
		String val = condition.getCondValue();
		if(val.startsWith("parent."))
			{
			parentField = val.substring("parent.".length());
			tmp = parent.get(parentField);
			if(tmp == null){tmp = "";}
			durable.context.put(val,tmp);
			}
		}
	associable = rule.conditionsTrue(durable);
	return associable;
	}


public EquipmentType getDefaultEquipmentType(){	return stateTransition.getDefaultEquipmentType();}

public Area getDefaultArea(){return stateTransition.getDefaultArea();}

public void  updateDurableParent(Machine durable, String oldparent, String newparent,TransactionManager txManager) throws Throwable
	{
	stateTransition.updateDurableParent(durable,oldparent,newparent,txManager);	
	}

public void  setMachine(Machine auxMachine)
	{
	stateTransition.setMachine(auxMachine);
	}

public void updateDurationForDurableStatus(Machine durable, DurableStatus olddurableStatus, Map exceptionAttributes,TransactionManager txManager) throws Throwable
	{
	stateTransition.updateDurationForDurableStatus(durable, olddurableStatus,exceptionAttributes,txManager);
	}

public void storeDurable (Machine machine, TransactionManager txManager)throws Throwable
	{			
	Machine edittingMachine = new Machine();
	if (machine.primaryKey==null||machine.primaryKey.equals(""))
		{
		edittingMachine =null;
		if (getMachine(machine.eIName)!=null)
			{
			//duplicate Durable ID
			throw new Exception (DurableManager.DUPLICATE_DURABLE_ID);			
			}
		}
	else
		{
		edittingMachine.primaryKey = machine.primaryKey;
		Vector tmp = Persistence.search(edittingMachine);
		if (tmp == null || tmp.isEmpty())
			{
			edittingMachine = null;
			}
		else
			{
			edittingMachine = (Machine) tmp.get(0);
			//check if the durable Id is duplicated
			Machine tmpMachine = (Machine)getMachine(machine.eIName);
			if (tmpMachine !=null && !tmpMachine.primaryKey.equals(machine.primaryKey))
				{
				throw new Exception (DurableManager.DUPLICATE_DURABLE_ID);
				}
			}
		}
	
	String oldParent = null;
	String newParent = null;
	if (edittingMachine !=null) 
		oldParent = edittingMachine.workstation;		
	if (machine.workstation != null && !machine.workstation.trim().equals("")){
		newParent = machine.workstation;
	}else {
		machine.workstation = null;
	}
	if(machine.getModelName()!=null&&"TTT".equals(machine.getEquipmentType().getModelSource()))
		{
		ModelService.getInstance().setModelByModelName(machine);
		}
	boolean needUpdateParent = false;
	boolean needUpdateChildren = false;
	boolean isNewMachine = false;
	if(edittingMachine != null) 
		{
		if (oldParent==null || !oldParent.equalsIgnoreCase(newParent))
		    {
			needUpdateParent = true;
		    }
		if (!edittingMachine.eIName.equals(machine.eIName))
		    {
			needUpdateChildren = true;
			needUpdateParent = true;
		    }
	    }
	else
		{
		isNewMachine = true;				
		if (newParent != null && !newParent.equalsIgnoreCase(""))
			{
			needUpdateParent = true;
			}
		}	
	String coreId = (String)machine.context.get("OPERATOR_ID");
	machine.mESName = machine.eIName;	
	if (machine.children==null)
		machine.children = new HashMap();		
	DurableStatus oldStatus = stateTransition.getDurableStatus(machine);
	txManager.store(machine);		
	Date now = new Date();
	SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	if (isNewMachine)
		{		
		DurableStatus status = new DurableStatus();
		status.EQUIPMENT_PK = machine.primaryKey;
		status.DURABLE_ID = machine.eIName;
		EquipmentType tmpParentType = getParentEquipmentType(machine.getEquipmentType());
		if (tmpParentType!=null && needUpdateParent && tmpParentType.propagateStatus())
			{
			Machine newParentDurable = (Machine) getMachine(newParent).clone();
			DurableStatus parentStatus = stateTransition.getDurableStatus(newParentDurable);
			status.STATE = parentStatus.STATE;
			status.SUBSTATE = parentStatus.SUBSTATE;
			}
		else
			{
			Map states = stateTransition.getInitialState(machine);
			status.STATE = (String)states.get("STATE");
			status.SUBSTATE = (String)states.get("SUBSTATE");
			}
		status.OPERATOR_ID=  coreId;
		status.duration = "0";				
		status.TIME = formatter.format(now);	
        status.putNewComments(status.OPERATOR_ID , status.TIME, "New Durable entering.");
		status.workstation = machine.workstation;
		status.location = machine.location;
		Vector attributes = machine.getEquipmentType().getStatusAttributes();
		Map hashMap = new HashMap();
		for (int i=0; i<attributes.size(); i++) 
		    {
			EquipmentAttribute attribute = (EquipmentAttribute) attributes.get(i);
			String value = null;
			if (attribute.type.equalsIgnoreCase("Text"))
				value = "";
			else 
				value = "0";
			hashMap.put(attribute.name, value);
		    }
		status.ATTRIBUTES = hashMap;
		txManager.store(status);
	    machine.context.put("durableStatus", status);	
	    txManager.setMachine(machine,stateTransition);
        }
	else 
	    {		
		DurableStatus durableStatus = oldStatus.clone();
		durableStatus.STATUS_PK = null;
		durableStatus.location= machine.location;
		durableStatus.workstation = machine.workstation;
		durableStatus.TIME = formatter.format(now);			   
		durableStatus.DURABLE_ID = machine.eIName;
		durableStatus.OPERATOR_ID = coreId;
		durableStatus.COMMENTS = null;
		durableStatus.putNewComments(durableStatus.OPERATOR_ID , durableStatus.TIME, "Durable attributes update");
		EquipmentType tmpParentType = getParentEquipmentType(machine.getEquipmentType());
		if (tmpParentType!= null && needUpdateParent&&tmpParentType.propagateStatus())
			{
			if (!tmpParentType.name.equalsIgnoreCase("tester")&&newParent!=null) 
				{
				Machine newParentDurable = getMachine(newParent);
				DurableStatus parentStatus = stateTransition.getDurableStatus(newParentDurable);
				durableStatus.STATE = parentStatus.STATE;
				durableStatus.SUBSTATE = parentStatus.SUBSTATE;
				}
			}		
		txManager.store(durableStatus);
		machine.context.put("durableStatus", durableStatus);
		txManager.setMachine(machine,stateTransition);
		updateDurationForDurableStatus(machine, oldStatus, new Hashtable(),txManager);
		}
	if (needUpdateParent)
	    {
		stateTransition.updateDurableParent(machine, oldParent, newParent,txManager);
	    }
	if (needUpdateChildren)
	    {
		stateTransition.syncChildrenwithParent(machine,false,txManager);
	    }	
	if (edittingMachine!=null)
		{
		DurableManager.updateLocationCount(machine,machine.location,edittingMachine.location,txManager);
		}
	else 
		{
		DurableManager.updateLocationCount(machine,machine.location,null,txManager);
		}	
	genesisServices.saveMachine(machine);
	}

	public static StateTransition getStateTransition() 
		{
		return stateTransition;
		}
	
	public List getActivityList(EquipmentType equipmentType)throws Throwable
		{
		List activities = new Vector();
		if(equipmentType == null){return activities;}
		Transitions transition = new Transitions();
		transition.setEquipmentTypePK(equipmentType.primaryKey);
		List allTransitions = (List)search(transition);
		Set namedTransitions = new TreeSet();
		for(int i=0;allTransitions!=null && i< allTransitions.size();i++)
			{
			transition = (Transitions)allTransitions.get(i);
			String activity = transition.getActivity();
			if( activity != null && !activity.equals("")){namedTransitions.add(activity);}
			}
		activities.addAll(namedTransitions);
		return activities;
		}
	
	public List getDurableMonitorsMain(EquipmentType eqType)
		{
		List monitorRules = eqType.getBusinessRulesBy("Durable Monitor");
		List monitors = new ArrayList();
		for(int i = 0;i< monitorRules.size();i++)
			{
			BusinessRule rule = (BusinessRule)monitorRules.get(i);
			if(rule.getName().endsWith("_Main")){monitors.add(rule);}
			}
		return monitors;
		}
	
	
	public List getTransitionsByActivity(EquipmentType equipmentType,String activity)throws Throwable
		{
		List transitions = new Vector();
		if(activity == null || equipmentType == null){return transitions;}
		Transitions transition = new Transitions();
		transition.setEquipmentTypePK(equipmentType.primaryKey);
		transition.setActivity(activity);
		transitions = (List)search(transition);
		return transitions;
		}
	
	public Map updateEquipmentAttributes(Map updates) throws Throwable
		{
		TransactionManager txManager = new TransactionManager();
		try
			{
			txManager.beginTransaction();
			EquipmentType eqType = (EquipmentType)updates.get("equipmentType");
			if(eqType == null || eqType.name.equals("Tester")){return updates;}
			List added   = (List)updates.get("added");
			List deleted = (List)updates.get("deleted");
			Map edited  = (Map)updates.get("edited");
			Map machines = stateTransition.getMachines(eqType);
			Iterator it = machines.keySet().iterator();
			while(it.hasNext())
				{
				Machine machine = (Machine)machines.get(it.next());
				machine = (Machine) machine.clone();
				boolean updateMachine = false;
				boolean updateStatus  = false;
				synchronized (machine)
					{
					Map dynamicAttributes = machine.getDynamicAttributes();
					Map statusDynamicAttributes = null;
					DurableStatus durableStatus  = (DurableStatus)machine.getContext().get("durableStatus");
					if(durableStatus!=null){ statusDynamicAttributes = durableStatus.getDynamicAttributes();}
					for(int i=0; i< added.size();i++)
						{
						EquipmentAttribute attribute = (EquipmentAttribute)added.get(i);
						Object value = "";
						if(attribute.getType().equals( EquipmentAttribute.COUNTER) ){value ="0";}
						if(attribute.getBelongs().equals(EquipmentAttribute.EQUIPMENT))
							{
							dynamicAttributes.put(attribute.getName(),value);
							updateMachine = true;
							}
						else if(attribute.getBelongs().equals(EquipmentAttribute.STATUS))
							{
							statusDynamicAttributes.put(attribute.getName(),value);
							updateStatus=true;
							}
						}
					for(int i=0;i< deleted.size();i++)
						{
						EquipmentAttribute attribute = (EquipmentAttribute)deleted.get(i);
						if(attribute.getBelongs().equals(EquipmentAttribute.EQUIPMENT))
							{
							dynamicAttributes.remove(attribute.getName());
							updateMachine = true;
							}
						else if(attribute.getBelongs().equals(EquipmentAttribute.STATUS))
							{
							statusDynamicAttributes.remove(attribute.getName());
							updateStatus = true;
							}
						}
					Iterator itEd = edited.keySet().iterator();
					while(itEd.hasNext())
						{
						List pair = (List)edited.get( itEd.next());
						if(pair == null || pair.size() != 2){continue;}
						EquipmentAttribute before = (EquipmentAttribute)pair.get(0);
						EquipmentAttribute after  = (EquipmentAttribute)pair.get(1);
						Object beforeValue ="";
						if(before.getBelongs().equals(EquipmentAttribute.EQUIPMENT))
							{
							beforeValue = dynamicAttributes.get(before.name);
							dynamicAttributes.remove(before.name);
							updateMachine = true;
							}
						else if(before.getBelongs().equals(EquipmentAttribute.STATUS))
							{
							beforeValue = statusDynamicAttributes.get(before.name);
							statusDynamicAttributes.remove(before.name);
							updateStatus = true;
							}
						if(after.getBelongs().equals(EquipmentAttribute.EQUIPMENT))
							{
							dynamicAttributes.put(after.name, beforeValue);
							updateMachine = true;
							}
						else if(before.getBelongs().equals(EquipmentAttribute.STATUS))
							{
							statusDynamicAttributes.put(after.name, beforeValue);
							updateStatus = true;
							}
						}
					if(updateMachine){txManager.store(machine);}
					if(updateStatus && durableStatus != null){txManager.store(durableStatus);}
					machine.context.put("durableStatus", durableStatus);
					txManager.setMachine(machine,this);
					}
				}
			txManager.executeBatch();
			}
		catch(Exception ex)
			{
			txManager.rollback();
			logger.error("updates:"+updates,ex);
			throw new Throwable("Batch Update Failed - " + ex.getMessage());
			}
		finally 
			{
			txManager.close();
			}
		return updates;
		}
	
	/**
	 * returns a value for a configuration key
	 * @param key the configuration key to retrieve
	 * @return the value of the configuration key passed 
	 * */
	public String getConfigurationKey(String key)
		{
		String value=null;
		if(properties.containsKey(key)){value = (String)properties.get(key);} 
		return value;
		}
	/**
	 * retrieves the report definition of the Report object passed
	 * @param report the report definition to retrieve, the report should have at least the report type and grouping populated, 
	 * if the report does not have a equipment type defined Tester is assumed 
	 * @return the definition of the report as defined in the reports definition files 
	 * */
	public String[] getReportDefinition(Report report)
		{
		return reports.getReportDefinition(report);
		}
	
	
	public Location getLocationByName (String location)throws Throwable
		{
		return stateTransition.getLocationByName(location);
		}

	private void storeLocation(Object location, TransactionManager txManager) throws Throwable
		{
		Location newLocation = (Location) location;
		Integer location_pk = newLocation.getLocationPk();
		if (location_pk == null)
			{
			txManager.store(newLocation);
			}
		else
			{
			Location oldLocation = new Location();
			oldLocation.setLocationPk(location_pk);
			oldLocation = (Location) Persistence.search(oldLocation).get(0);
			DurableManager.updateLocationName(newLocation.getName(), oldLocation.getName(),txManager);
			txManager.store(newLocation);
			}
		}
	
	public String getAppUrl(){return stateTransition.getAppUrl();}
	public void setAppUrl(String url){stateTransition.setAppUrl(url);}
	
	private void initRetentionPolicy(Properties props)
		{
		String tmp;
		tmp = properties.getProperty("maxRaw","1");
		try{maxRaw = Integer.parseInt(tmp);}
		catch(NumberFormatException nfe){maxRaw = 1;}
		if(maxRaw < Summary.MIN_MAX_RAW){maxRaw = Summary.MIN_MAX_RAW;}
		
		tmp = properties.getProperty("maxSummary","3");
		try{maxSummary = Integer.parseInt(tmp);}
		catch(NumberFormatException nfe){maxSummary = 3;}
		if(maxSummary < Summary.MIN_MAX_SUMMARY){ maxSummary =  Summary.MIN_MAX_SUMMARY;}
		}

	/**
	 * reload all equipments of the specific type from database to memory
	 * @param et equipments of this equipment will be removed from memory  
	 * @throws Throwable
	 */
	public void reloadEquipments(EquipmentType et) throws Throwable
		{
		stateTransition.setMachinesByEquipmentType(et);
		}
	
	public String getUserFullName(String name)
		{
		return stateTransition.getUserFullName(name);
		}
	
	public List<String> getMachineModel(EquipmentType et,Area area,String platform) throws Throwable
		{
		List<String> result = new ArrayList<String>();
		if ("MES".equals(et.getModelSource()))
			{
			result = genesisServices.getMachineModel(et.mesEquipmentType, platform);
			}
		else if("TTT".equals(et.getModelSource()))
			{
			result = ModelService.getInstance().getMachineModel(et, area, platform);
			}
		return result;
		}

	public List<String> getChildModel(EquipmentType et, String model) throws Throwable
		{
		List<String> result = new ArrayList<String>();
		if ("MES".equals(et.getChildSource()))
			{
			result = genesisServices.getChildModel(et.mesEquipmentType, model);
			}
		return result;
		}

	public List<String> getMachinePlatform(EquipmentType et,Area area) throws Throwable
		{
		List<String> result = new ArrayList<String>();
		if ("MES".equals(et.getPlatformSource()))
			{
			result = genesisServices.getMachinePlatform(et.mesEquipmentType);
			}
		else if("TTT".equals(et.getModelSource()))
			{
			result = ModelService.getInstance().getMachinePlatform(et,area);
			}
		return result;
		}
	
	public List<Model> getModelList(Model searchModel) throws Throwable
		{
		return ModelService.getInstance().getModelList(searchModel);
		}
	public Model getModelByDescription(EquipmentType et,Area area,String description) throws Throwable
		{
		return ModelService.getInstance().getModelByDescription(et,area,description);
		}
	public static void setPlantCode(String plantCode){StateStamper.plantCode= plantCode;}
	public static String getPlantCode(){return plantCode;}
	public static void setAreaCode(String areaCode){StateStamper.areaCode= areaCode;}
	public static String getAreaCode(){return areaCode;}
	public static void setStage(String stage){StateStamper.stage=stage;}
	public static String getStage(){return stage;}

	
	public List<Machine> updateMachineStatus(List<String> names)
		{
		return stateTransition.updateMachineStatus(names);
		}
	
	public List<Machine> getMachinesByMesNames(List<String> names)
		{
		return stateTransition.getMachinesByMesNames(names);
		}

	public GenesisServices getGenesisServices()
		{
		return genesisServices;
        }

	
	public void sendEPRToPic(Machine machine)
		{
		PICConnection picConn = getPicConnection(machine);
		picConn.sendEvent(generateEquipmentEvent(machine));
		}
	
	private PICConnection getPicConnection(Machine machine)
		{
		EquipmentType eqType = machine.getEquipmentType();
		PICConnection picConn =  picConnections.get(eqType);
		if(picConn == null)
			{
			String host = eqType.getPicHost();
			if(StringUtils.isEmpty(host) )
				{
				throw new IllegalArgumentException(eqType.name + " picHost is null ");
				}
			int port = 0;
			try{port = Integer.valueOf(eqType.getPicPort()).intValue();}
			catch(NumberFormatException nfe){ throw new IllegalArgumentException(eqType.name + " picPort has an invalid value :" + eqType.getPicPort());}
			picConn = new PICConnection(host,port ,mapping);
			picConnections.put(eqType, picConn);
			}
		return picConn;
		}
	
	public EquipmentEvent generateEquipmentEvent(Machine machine)
		{
  		EquipmentEvent equipmentEvent = new EquipmentEvent();
  		equipmentEvent.equipment = new Equipment( trimEquipmentTypeName(machine.getEquipmentType().name),machine.mESName,machine.location,false,new Vector());
  		DurableStatus dStatus = (DurableStatus)machine.getContext().get("durableStatus");
  		equipmentEvent.state = dStatus.STATE;
  		equipmentEvent.event = new com.freescale.sc.model.entity.event.Event(StateTransition.getEventIdByState(equipmentEvent.state));
  		equipmentEvent.startTime = timeFormat.format(new Date());
  		equipmentEvent.event.time = equipmentEvent.startTime;
  		Vector<Context> contexts = new Vector<Context>();
  		contexts.add(new Context("SUBSTATE",dStatus.SUBSTATE,""));
  		contexts.add(new Context("OPERATOR_ID",dStatus.OPERATOR_ID,""));
  		//Parent ID
  		String parentEquipmentType = trimEquipmentTypeName(machine.getEquipmentType().parent);
  		parentEquipmentType = parentEquipmentType.toUpperCase()+"_ID";
  		contexts.add(new Context(parentEquipmentType,machine.workstation,""));
  		
  		equipmentEvent.contexts = contexts;
		equipmentEvent.summings = new Vector<Summing>();
		return equipmentEvent;
		}
	
	private String trimEquipmentTypeName(String eqTypeName)
		{
		String name = eqTypeName;
		if (name != null && name.contains(" "))
			{
			name = name.substring(0, name.indexOf(" "));
			}
		return name;
		}
	
}


