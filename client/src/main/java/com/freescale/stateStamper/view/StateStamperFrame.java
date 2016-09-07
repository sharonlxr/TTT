package com.freescale.stateStamper.view;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Vector;
import java.util.prefs.Preferences;

import javax.jnlp.BasicService;
import javax.jnlp.ServiceManager;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

import org.apache.commons.lang.StringUtils;

import com.freescale.clientUtil.Form;
import com.freescale.clientUtil.MainFrame;
import com.freescale.clientUtil.Table;
import com.freescale.clientUtil.Util;
import com.freescale.stateStamper.model.entity.Administrator;
import com.freescale.stateStamper.model.entity.Area;
import com.freescale.stateStamper.model.entity.E10Substate;
import com.freescale.stateStamper.model.entity.EquipmentType;
import com.freescale.stateStamper.model.entity.Machine;
import com.freescale.stateStamper.model.entity.Report;
import com.freescale.stateStamper.model.entity.Role;
import com.freescale.stateStamper.model.entity.Substate;
import com.freescale.stateStamper.model.entity.Transitions;
/**
 * This class creates the main frame for the State Stamper GUI
 */

public class StateStamperFrame extends MainFrame
	{
	private static final long serialVersionUID = 1L;
	public static final int UTILIZATION=0, PRODUCTION=1, MAINTENANCE=2, ENGINEERING=3;
	public static final String[] STATES = {"Unknown","B0B0B0","808080","Productive","C0FFC0","00FF00","Standby","FFF8A0","FFFF80","Engineering","A8FFFF","00FFFF","Scheduled Down","FFC880","FFC000","Unscheduled Down","FFD0D0","FF0000","Non Scheduled","E0E0E0","C0C0C0"};
	private static final String[][] MENU = {{"File","Login","Exit"},{"Type"},{"View","Types","Locations"},{"Dashboard","Utilization","Production","Maintenance","Engineering","Legend"},{"Dates","Dates"},{"Report","Excel Report","HTML Report"},{"Help","Help","About"}};
	private static final String[] ADMIN_MENU_DEFAULT = {"Settings","EquipmentTypes","States","Transitions","Users","Machines", "Locations Configuration", "Scheduled Reports", "Area"};	
	private static final String CONF_KEY = "TTT_CONFIGURATION";
	public static final String MESSAGE = "Welcome to Tool Time Tracker!";
	private static String[] ADMIN_MENU;	
	private static Machines machines;
	private static StateUpdate stateUpdate;
	private static Dates dates;
	private static int dashboard = UTILIZATION;
	private static boolean displayType;
	private static String reportType, grouping;
	private static Hashtable groupings = new Hashtable(), types, locations, states, colors = new Hashtable(), brightColors = new Hashtable();
	public  EquipmentType equipmenttype;
    private static String lastEquipmentTypeName = "";
    public  Area area;
    private static String lastArea = "";
    private String siteName;
    int localOffset = 0;
	
	/**
	 * Main method for this application
	 * @param args Array of arguments
	 * @throws Throwable
	 */
	public static void main(String args[]) throws Throwable
		{		
		try {Util.codeBase = ((BasicService)ServiceManager.lookup("javax.jnlp.BasicService")).getCodeBase();}
		catch (javax.jnlp.UnavailableServiceException e) {Util.codeBase = new URL(args[0]);}
		new StateStamperFrame();
		}

	/**
	 * Creates a frame with menu and tool bars and panel with all machines
	 * @throws Throwable
	 */
	public StateStamperFrame() throws Throwable
		{
		super("Tool Time Tracker",MENU,"Reports",true);
		toolBar = Util.createToolbar(MENU,new String[]{"Utilization","Production","Maintenance","Engineering","Legend"},true,true,this);  // Recreate toolbar without the dashboard icons
		setBody(new Util.Welcome(MESSAGE,"","",Color.WHITE));
		addWindowListener(new WindowAdapter() {public void windowClosing(WindowEvent e) {exit();}});
		readConfiguration();
		siteName = (String)Util.request(null,"getSiteName.do",false);
		setTitle("Tool Time Tracker "+ siteName+ " " +FinalTest.getView(dashboard)+" View"); // Update title
		addReportTypes();
		dates = new Dates((Map)Util.request(null,"getDates.do",false));
		states = (Hashtable)Util.request(null,"getColors.do",false);
		refreshMachines();		
		Enumeration keys = states.elements();
		while (keys.hasMoreElements())
			{
			Substate substate = (Substate)keys.nextElement();
			brightColors.put(substate.substate,new Color(Integer.parseInt(substate.color,16)));
			}
		for (int i=0; i<STATES.length; i+=3) // Initialize colors and bright colors
			{
			colors.put(STATES[i],new Color(Integer.parseInt(STATES[i+1],16)));
			brightColors.put(STATES[i],new Color(Integer.parseInt(STATES[i+2],16)));
			}
		Calendar local = new GregorianCalendar();
		localOffset = local.get(Calendar.ZONE_OFFSET)  + local.get(Calendar.DST_OFFSET);
		stateUpdate = new StateUpdate();
		stateUpdate.start();			
		}
	
	public static Color getColor(String state) {return (Color)colors.get(state);}
	
	public static Hashtable getColors() {return (Hashtable)brightColors.clone();}
	
	public static Substate getSubstate(String state) {return (Substate)states.get(state);}

	/**
	 * Saves configuration and exits the application
	 */
	public void exit()
		{
		try {Preferences.userRoot().put(CONF_KEY,displayType+"\t"+dashboard+"\t"+lastEquipmentTypeName+"\t"+lastArea);}
		catch (Throwable t) {}
		System.exit(0);
		}
	
	/**
	 * Displays machines grouped by type
	 * @throws Throwable
	 */
	public void types() throws Throwable {displayType=true; displayMachines();}
	
	/**
	 * Displays machines grouped by location
	 * @throws Throwable
	 */
	public void locations() throws Throwable {displayType=false; displayMachines();}
	
	/**
	 * Displays utilization dashboard
	 * @throws Throwable
	 */
	public void utilization() throws Throwable {changeDashboard(UTILIZATION);}
	
	/**
	 * Displays production dashboard
	 * @throws Throwable
	 */
	public void production() throws Throwable {changeDashboard(PRODUCTION);}
	
	/**
	 * Displays maintenance dashboard
	 * @throws Throwable
	 */
	public void maintenance() throws Throwable {changeDashboard(MAINTENANCE);}
	
	/**
	 * Displays engineering dashboard
	 * @throws Throwable
	 */
	public void engineering() throws Throwable {changeDashboard(ENGINEERING);}
	
	/**
	 * Displays/hides dashboard legend
	 * @throws Throwable
	 */
	public void legend() throws Throwable {Util.createWindow(FinalTest.getView(dashboard)+" View",new JLabel(FinalTest.createText(null,dashboard,false)),this);}
		
	/**
	 * Displays a panel with all dates available for reports grouped by period. Dates are refreshed from server
	 */
	public void dates()
		{
		try {dates.refresh((Map)Util.request(null,"getDates.do",false));} catch (Throwable t) {}	
		Util.createWindow("Dates",dates,this);
		}
	
	/**
	 * Generates a report in Excel format with the selected report type, machines and dates
	 * @throws Throwable
	 */
	public void excelReport() throws Throwable {generateReport(false);}
	
	/**
	 * Generates a report in HTML format with the selected report type, machines and dates
	 * @throws Throwable
	 */
	public void hTMLReport() throws Throwable {generateReport(true);}
		
	/**
	 * Display the login form
	 * @throws Throwable
	 */
	public void login() throws Throwable
		{
		Form login = new Form("Login", new Administrator(), this, 0);
		login.setValues("equipmentType", getEquipmentTypeNames());
		login.setValues("areaName",getAreaNames());
		if(lastEquipmentTypeName != null && !lastEquipmentTypeName.equals("")){login.setValue("equipmentType", lastEquipmentTypeName);}
		if(lastArea != null && !lastArea.equals("")){login.setValue("areaName", lastArea);}
		Util.createWindow("Login", login, this);
		}

	/**
	 * Login the user
	 * @param form The form with the user ID and password
	 * @throws Throwable
	 */
	public void login(Form form) throws Throwable
		{
		EquipmentType equipmentType = getEquipmentTypeByName(form.getValue("equipmentType"));
		Area area = getAreaByName(form.getValue("areaName"));
		Object o = form.getObject();
		Administrator  admin =((Administrator)(o));
		admin.setEquipmentType(equipmentType.name);
		admin.setEquipmentTypePK(equipmentType.primaryKey);
		admin.setArea(area);
		admin.setAreaPK(area.getPrimaryKey());
		admin = (Administrator)Util.request(admin,"login.do");
		form.cancel();		
		//to filter out the functions that the user have privileges
		Vector functions = new Vector();

		for (int i = 1; i < ADMIN_MENU_DEFAULT.length; i++)
			{
			functions.add(ADMIN_MENU_DEFAULT[i]);
			}
		Vector allowedFunctions = (Vector)Util.request(functions,"authentication.do",false);		
		ADMIN_MENU = new String[allowedFunctions.size()+1];		
		ADMIN_MENU[0] = ADMIN_MENU_DEFAULT[0];
		for (int i = 0; i < allowedFunctions.size(); i++)
			{
			ADMIN_MENU[i + 1] = (String) allowedFunctions.get(i);
			}
		
		if (getJMenuBar().getMenu(6).getText() != ADMIN_MENU[0])  // Add admin menu in 6th position 
			{
			Util.add(getJMenuBar(),ADMIN_MENU,this,6);
			for (int i=1; i<ADMIN_MENU.length; i++)	Util.add(toolBar,ADMIN_MENU[i],this,true,11+i);
			toolBar.add(new JSeparator(SwingConstants.VERTICAL),12);
			if(admin.isAdministrator())
				{
				JMenu menuFile = getJMenuBar().getMenu(0);
				JMenuItem reload = new JMenuItem("Reload Settings");
				reload.addActionListener(this);
				menuFile.insert(reload,1);
				}
			validate();
			}

		this.equipmenttype = equipmentType;
		this.area = area;
		lastEquipmentTypeName = this.equipmenttype.name; 
		lastArea = this.area.getName();
		refreshMachines();
		addReportTypes();
		}
	
	public void refreshMachines()throws Throwable{
		Map aux = (Map)Util.request(new Object[]{equipmenttype,area} ,"getMachines.do",false);
		indexMachines(aux);
		displayMachines();
		machines.setState(aux,dashboard);	
	}
	
	
	/**
	 * Display a table with all the users
	 * @throws Throwable
	 */
	public void users() throws Throwable
	   	{
		Vector administrators = (Vector)Util.request(new Administrator(),"search.do",false);
		Collections.sort(administrators);
		Util.createWindow("Users",new Users("Users",administrators,this),this);
		}

	/**
	 * 
	 * */
	private Map<Integer,String> getExtendedRolesNames()throws Throwable
		{
		Role role  = new Role();
		Map<Integer,String> roles = new HashMap<Integer,String>();
		List res; 
		res =(List)Util.request(role,"search.do",false);
		Map types = (Map)Util.request(null,"getEquipmentTypes.do",false);
		for (int i = 0; i < res.size(); i++)
			{
			role = (Role)res.get(i);
			StringBuffer tmp = new StringBuffer();
			tmp.append(role.rolename);
			//if the role is not for Multiple equipments include the Equipment Type
			if (! role.isMultiEquipment() )
				{
				tmp.insert(0,((EquipmentType)types.get(role.equipmenttypePK)).name+"-");
				}
			roles.put(role.rolePK,tmp.toString()); 
			}
		return roles;
		}
	
	
	public void locationsConfiguration()throws Throwable
	{
	
		Util.setProperty("Locations.properties",
					"name,capacity,count");
		Util.setProperty("Locations.required", "Y,Y,Y");
		Util.setProperty("Locations.length", "16,16,16");		 
		Util.createWindow("Locations", new Locations(this), this);
	}
	
	public void area()throws Throwable
		{
		Util.createWindow("Area", new AreaTable(this), this);
		}
	
	/**
	 * Display a table with all the machines that will be collecting data
	 * @throws Throwable
	 */
	public void machines() throws Throwable
		{
		equipmenttype = (EquipmentType) Util.request(null, "getCurrentEquipmentType.do");
		Util.createWindow("Machines", new Equipments(equipmenttype,area,this), this);
		}
	
	
	
	

	/**
	 * Display a table with all states
	 * @throws Throwable
	 */
	public void states() throws Throwable {Util.createWindow("States",new Colors(this.equipmenttype.primaryKey),this);}

	/**
	 * Display a table with all the scheduled reports
	 * @throws Throwable
	 */
	public void scheduledReports() throws Throwable 
	    {
	    Report report = new Report();
	    EquipmentType eqType = (EquipmentType)Util.request(null,"getCurrentEquipmentType.do",false);
	    if(eqType == null){eqType = (EquipmentType)Util.request(null,"getDefaultEquipmentType.do",false);}
	    report.setEquipmentType(eqType);
	    Util.createWindow("Reports",new Reports((Vector)Util.request(report,"search.do",false),groupings,toString(types),toString(locations),eqType),this);
	    }
	
	/**
	 * Displays the help documents
	 * @throws Throwable
	 */
	public void help() throws Throwable {Util.openPage("usersGuide/index.html");}
	
	/**
	 * display an about dialog with the Build number (YYYYMMDD_SVNRevision) 
	 * */
	public void about()throws Throwable
	    {
		String [] message;
		StringBuffer aux = new StringBuffer();
		aux.append("TTT ");
		aux.append(Util.request(null,"getWarInfo.do"));
		aux.append("\tJava Version : " + System.getProperty("java.version") + " ( " + System.getProperty("java.vendor") +" )\t");
		Calendar cal = new GregorianCalendar();
		aux.append("\tCopyright \u00A9 "+cal.get(Calendar.YEAR) +" NXP Semiconductor");
		message=aux.toString().split("\t");
		JOptionPane.showMessageDialog(null, message,"About TTT",JOptionPane.INFORMATION_MESSAGE);
		}
	
	public void reloadSettings()throws Throwable
		{
		Util.request(null,"reloadSettings.do");
		Util.createWindow("Success", new Form("Success", "", this, 0), this);
		}

	public void equipmentTypes()throws Throwable
	    {
		Util.createWindow("EquipmentType", new EquipmentTypes((Vector)Util.request(new EquipmentType(),"search.do",false),this), this);
	    }
	
	public void e10States()throws Throwable
	    {
		Table states = new Table("E10States",(Vector)Util.request(new E10Substate(),"search.do",false),null,null,false);
		Util.createWindow("E10States",states,this);
	    }
	
	public void transitions()throws Throwable
	    {
		Transitions transition = new Transitions();
		transition.setEquipmentTypePK(equipmenttype.primaryKey);
		TransitionsView transitions = new TransitionsView("TransitionsView",this.equipmenttype,(Vector)Util.request(transition,"search.do",false),700,400,null);
		Util.createWindow("TransitionsView",transitions,this);
	    }
	
	
	private void readConfiguration()
		{
		try
			{
			String[] conf = Preferences.userRoot().get(CONF_KEY,"").split("\t");
			displayType = Boolean.parseBoolean(conf[0]);
			dashboard = Integer.parseInt(conf[1]);
			lastEquipmentTypeName = conf[2];
			lastArea = conf[3];
			}
		catch (Throwable t) {}
		
		}
	
	public static String getVersion(){return "Tool Time Tracker 3.1";}
	/**
	 * Displays machines grouped by type or location
	 * @throws Throwable
	 */
	private void displayMachines() throws Throwable
		{
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.getViewport().setOpaque(false);
		scrollPane.setPreferredSize(new Dimension(640,460));
		if (machines!=null) {machines.stop();}
		machines = new Machines(displayType?types:locations,dashboard);
		scrollPane.setViewportView(machines);		
		setBody(scrollPane);
		new Thread(machines).start();
		if (stateUpdate != null) stateUpdate.interrupt();
		}
	
	/**
	 * Indexes the vector of Machines objects by type and location
	 */
	private void indexMachines(Map machines)
		{
		types = new Hashtable();
		locations = new Hashtable();
		for (Object mid:machines.keySet())
			{
			Machine machine = (Machine)machines.get(mid);
			Vector aux1 = (Vector)types.get(machine.type);
			if (aux1 == null) {aux1 = new Vector(); types.put(machine.type,aux1);}
			aux1.addElement(machine.mESName);
			if(machine.location==null)machine.location="Unknown";
			Vector aux2 = (Vector)locations.get(machine.location);
			if (aux2 == null) {aux2 = new Vector(); locations.put(machine.location,aux2);}
			aux2.addElement(machine.mESName);
			}
		}
	
	/**
	 * Change the dashboard
	 * @param view The dashboard to change it to
	 */
	private void changeDashboard(int view)
		{
		dashboard = view;
		machines.updateLabels(dashboard,true);
		setTitle("Tool Time Tracker "+siteName + " "+FinalTest.getView(dashboard)+" View");
		}
	
	private String toString(Hashtable hashtable)
		{
		String result = "";
		Vector vector = new Vector(hashtable.keySet());
		Collections.sort(vector);
		for (int i=0; i<vector.size(); i++) result += vector.elementAt(i) + ",";
		return result;
		}
	
	/**
	 * Creates a report object from the selected report type, machines and dates
	 * @param isHTML Whether the format should be HTML or Excel
	 * @return A Report object
	 * @throws Throwable
	 */
	private Report createReport(boolean isHTML) throws Throwable
		{
		Report report = new Report();
		report.machines = machines.getSelected();
		if (report.machines.size() == 0) throw new Throwable("Select 1 or more machines");
		//getSelected returns a hashtable only with the names of the equipments as key, populate the value with the machine object
		Hashtable aux = dates.getSelected();
		if (aux.size() == 0) throw new Throwable("Select the dates for the report");
		report.period = (String)aux.get("Period");
		report.dates = (Object[])aux.get("Dates");
		if (report.period.equals("Raw"))
			{			
			report.startDate = (Date)aux.get("StartDate");
			report.endDate = (Date)aux.get("EndDate");
			// Convert to GMT
			report.startDate = new Date(report.startDate.getTime()+localOffset);
			report.endDate = new Date(report.endDate.getTime()+localOffset);
			if (report.endDate.before(report.startDate)) throw new Throwable("Start date must be before end date");
			} 
		report.reportType = reportType;
		report.grouping = grouping;
		report.isHTML = new Boolean(isHTML );
		return report;
		}

	/**
	 * Generates a report with the selected report type, machines and dates
	 * @param isHTML Whether the format should be HTML or Excel
	 * @throws Throwable
	 */
	private void generateReport(boolean isHTML) throws Throwable
		{
		Report report = createReport(isHTML);
		if (reportType.equals("Transitions")) return;
		if (reportType.equals("State Transitions") || reportType.equals("Substate Transitions"))
			{
			if (!report.period.equals("Raw")) throw new Throwable("Select start and end times from the Raw tab on the Dates window");
			Hashtable data = (Hashtable)Util.request(report,"getStateTransitions.do",false);
			// Convert to local time
			report.startDate = new Date(report.startDate.getTime()-localOffset);
			report.endDate = new Date(report.endDate.getTime()-localOffset);
			Util.createFrame("State Transitions","Reports",new StateTransitions(data,null,null,brightColors,report.startDate,report.endDate),this,null,null,null);
			}
		else
			{
			Integer index = (Integer) Util.request(report,"storeReport.do",false);
			Util.openPage("generateReport"+index+".do");
			}
		}
	
	/**
	 * Add the report types and subtypes to the bar menu and populate the hashtable groupings with them
	 * @throws Throwable
	 */
	private void addReportTypes() throws Throwable
		{
		Object type= Util.request(null,"getCurrentEquipmentType.do",false);
		Vector reports = (Vector)Util.request(type,"getReportTypes.do",false);
		String report = (String)reports.firstElement();
		reportType = report.split("\t")[0]; grouping = report.split("\t")[1];
		JMenu menuReport = getJMenuBar().getMenu(1), subMenu = null;
		menuReport.removeAll();
		String item = null, reportTypes = "";
		Vector vector = null;
		SetReportType setReportType = new SetReportType();
		for (int j=0; j<reports.size(); j++)
			{
			String[] items = ((String)reports.elementAt(j)).split("\t");
			if (!items[0].equals(item))
				{
				subMenu = new JMenu("   "+items[0]);
				menuReport.add(subMenu);
				item = items[0];
				vector = new Vector();
				reportTypes += item+",";
				groupings.put(item,vector);
				}
			vector.addElement(items[1]);
			JMenuItem menuItem = new JMenuItem(items[1],Util.getIcon("Invisible",16));
			menuItem.setActionCommand(item+"\t"+items[1]);
			menuItem.addActionListener(setReportType);
			subMenu.add(menuItem);
			}
		Properties properties = (Properties)UIManager.get("gui.properties");
		properties.setProperty("ReportSetup.reportType.values",reportTypes);
		}
	
	private Vector<String> getEquipmentTypeNames() throws Throwable
		{
		Map<Integer,EquipmentType> typelist = (Map<Integer,EquipmentType>) Util.request(null, "getEquipmentTypes.do", false);
		Vector<String> equipmentTypes = new Vector<String>();
		for (Entry<Integer,EquipmentType> entry: typelist.entrySet())
			{
			EquipmentType equipmenttype = entry.getValue();
			equipmentTypes.add(equipmenttype.name);
			}
		Collections.sort(equipmentTypes);
		return equipmentTypes;
		}

	public Vector<String> getAreaNames() throws Throwable
		{
		Vector<String> vector = new Vector<String>();
		List<Area> list = (List<Area>) Util.request(new Area(), "search.do", false);
		for(Area area:list)
			{
			vector.add(area.getName());
			}
		Collections.sort(vector);
		return vector;
		}
	
	public Vector<String> getAreaNamesWithoutAll() throws Throwable
		{
		Vector<String> vector = getAreaNames();
		vector.remove(Area.AREA_ALL_NAME);
		return vector;
		}
	
	public String getRoleValues() throws Throwable
		{
		Map<Integer,String> roles =  getExtendedRolesNames();
	   	List<String> rolesNames = new ArrayList<String>(roles.values());
	   	Collections.sort(rolesNames);
	   	//make Role.GUEST the first element so it is the default role
	   	if(rolesNames.contains(Role.GUEST)){rolesNames.remove(Role.GUEST);}
	   	rolesNames.add(0, Role.GUEST);
		return StringUtils.join(rolesNames,",");
		}
	
	public EquipmentType getEquipmentTypeByName(String name) throws Throwable
		{
		return (EquipmentType) Util.request(name,"getEquipmentTypeByName.do",false);
		}
	
	public Area getAreaByName(String name) throws Throwable
		{
		return (Area)Util.request(name,"getAreaByName.do",false);
		}

	
	/**
	 * This class is used to set the report subtype when selecting a report type in the report schedule table
	 */
	private class SetReportType implements ActionListener
		{
		public void actionPerformed(ActionEvent e)
			{
			reportType = e.getActionCommand().split("\t")[0];
			grouping = e.getActionCommand().split("\t")[1];
			}
		}
	
	private class StateUpdate extends Thread
		{
		public void run()
			{	
			while (true)
				{	
				try {machines.updateMachineStatus();} catch (Throwable t) {}
				try {sleep(20000);} catch(InterruptedException e){} catch (Throwable t) {t.printStackTrace();}				
				}
			}
		}			
	}