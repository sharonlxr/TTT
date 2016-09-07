package com.freescale.stateStamper.controller;

import java.io.EOFException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.*;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.freescale.serverUtil.*;
import com.freescale.stateStamper.model.entity.*;
import com.freescale.stateStamper.model.logic.*;

import javax.servlet.*;
import javax.servlet.http.*;
/**
 * Servlet that implements the controller
 */
public class Controller extends ServerController
	{
	private static final String URL = "ldaps://fsl-ids.freescale.net:636", SERVICE_USER = "EWM", SERVICE_PASSWORD = "zd31_Ewm1";
	private static final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private static final int MONTH_SECS = 30*24*60*60;
	private static StateStamper stateStamper;	
	private static Logger logger;
	
	public Controller() {super(null,null,null,null,"StateStamper",10,false);}

	/**
	 * Create the State Stamper class
	 * @param servletConfig The servlet configuration object
	 */
	public void init(ServletConfig servletConfig)
	{
	try
		{
		super.init(servletConfig);
		stateStamper = new StateStamper();
		stateStamper.init(servletConfig);
		DurableManager.setStateStamper(stateStamper);
		
		logger = LoggerFactory.getLogger("access");
		}
	catch (Throwable th)
		{
		logger.error("Controller.init " + th.getMessage(),th);
		}
	}
	
	/**
	 * Destroys the state stamper object
	 */
	public void destroy() {stateStamper.destroy(); stateStamper = null;}
	
	/**
	 * Overwrite doGet method
	 */
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
		{
		String url = request.getRequestURI();
		String action = url.substring(url.lastIndexOf("/") + 1, url.length() - 3);
		if(stateStamper.getAppUrl() == null){stateStamper.setAppUrl(request.getScheme() + "://" + request.getServerName()+":"+ request.getServerPort()+request.getContextPath());}
		// If request method if HEAD, don't do anything, there will be another request with method GET
		if (request.getMethod().equals("HEAD") && action.startsWith("generateReport"))
			{
			response.setContentType("application/vnd.ms-excel");
			return;
			}	
		Administrator administrator = (Administrator)request.getSession().getAttribute("user");
		Object output = null;
		String page = null;
		try
			{
			if (administrator == null && (action.equals("store") || action.equals("delete"))) throw new Throwable("Session expired. Please login again");
			Object input = null;								
			try {				
				if (!action.startsWith("durable"))
					//try to avoid calling request.getInputStream() and request.getParameter for a same request 
				input = Util.read(request);				
			}
			catch(EOFException eof){}
			catch (Throwable throwable1) {
				logger.error(url,throwable1);
				
			}									
			if (action.equals("login")) output = login(request,URL,SERVICE_USER,SERVICE_PASSWORD,((Administrator)input).getCoreId(),
					((Administrator)input).getPassword(),((Administrator)input).getEquipmentTypePK(),((Administrator)input).getArea().getPrimaryKey());

			else if (action.equals("logout")) request.getSession().invalidate();
			else
				{
				Object[] aux = process(action, input, request, response);
				output = aux[0];
				page = (String) aux[1];
				}
			}
		catch (Throwable throwable) {
			;output = new Throwable(throwable.toString());logger.error(url,throwable);}
		if (page != null)
			{
			request.setAttribute("result",output);
			request.getRequestDispatcher(page).forward(request,response);
			}
		else if (output != null) Util.write(response,output);
		}
	/**
	 * Processes GET requests
	 * @param request The HttpServletRequest object
	 * @param response The HttpServletResponse object
	 */
	protected Object[] process(String action, Object input, HttpServletRequest request, HttpServletResponse response) throws Throwable
		{
		Object output = null;
		String page = null;
		
		if (action.startsWith("durable")) {
			 return processDurableRequest(action, input, request,  response); 
		}
		if (action.equals("getReportTypesHTML")) {output = stateStamper.getReportTypes(null); page = "reportTypes.jsp";}
		else if (action.equals("getMachinesHTML")) { output = stateStamper.getMachines(); page = "machines.jsp";}		
		else if (action.equals("getDatesHTML")) {output = stateStamper.getDates(); page = "dates.jsp";}
		else if (action.equals("getReports")) {output = stateStamper.getReports(request.getParameter("period")); page = "reports.jsp";}
		else if (action.equals("getConnections")) {output = stateStamper.getConnections(); page = "connections.jsp";}
		else if (action.equals("getGroups")) {output = stateStamper.getGroups(request.getParameter("equipmentType")); page = "groups.jsp";}
		else if (action.equals("getStates")) {output = stateStamper.getStates(request.getParameter("type"),request.getParameter("location"),request.getParameter("equipmentType")); page = "states.jsp";}
		else if (action.equals("getUOEE")) {output = stateStamper.getUOEE(request.getParameter("type"),request.getParameter("location")); page = "uoee.jsp";}
		else if (action.equals("getUOEE2")) {output = stateStamper.getUOEE(request.getParameter("type"),request.getParameter("location")); page = "uoee2.jsp";}
		else if (action.equals("getLotProgress")){Float percentage=Float.parseFloat(request.getParameter("lotprogress"));output = stateStamper.getLotProgress(percentage);((Vector)output).insertElementAt(percentage, 0);page = "lotprogress.jsp";}
		else if (action.equals("getContextsSummings")) {output = stateStamper.getContextsSummings(request.getParameter("type"),request.getParameter("location")); page = "contextsSummings.jsp";}
		else if (action.equals("getSummarySchedule")) {output = stateStamper.getSummarySchedule(); page = "summarySchedule.jsp";}
		else if (action.equals("getSummaryFiles")) {output =  stateStamper.getSummaryFiles(); page = "summaryFiles.jsp";}
		else if (action.equals("getReportTypes")) {output = stateStamper.getReportTypes((EquipmentType)input);}
		else if (action.equals("getMachine")) output = stateStamper.getMachine((String)input);
		else if (action.equals("getMachines"))output = stateStamper.getMachines((Object[])input);
		else if (action.equals("getDates")) output = stateStamper.getDates();
		else if (action.equals("getColors")) output = stateStamper.getColors();
		else if (action.equals("getContextSumming")) output = stateStamper.getContextsSummings((Machine)input);
		else if (action.equals("getStateTransitions")){output = stateStamper.getStateTransitions((Report)input);}
		else if (action.equals("search")) output = stateStamper.search(input);
		else if (action.equals("store")) {output = ""; stateStamper.store(input);}
		else if (action.equals("delete")) {output = ""; stateStamper.delete(input);}
		else if (action.equals("storeReport")) output = stateStamper.storeReport((Report)input);
		else if (action.equals("getReport")) stateStamper.getFile(request.getParameter("file"),response,true);
		else if (action.equals("getFile")) stateStamper.getFile(request.getParameter("file"),response,false);
		else if (action.equals("generateReportHTML"))
			{
			Report report = parseInput(request);
			report.setEquipmentType(getCurrentEquipmentType(request));
			report.setArea(getCurrentArea(request));
			stateStamper.generateReport(report,response);}
		else if (action.startsWith("generateReport")){Report report = stateStamper.getReport(Integer.parseInt(action.substring(14)));stateStamper.generateReport(report,response); } // Extract report number:  generateReport12  where 12 is the report number
		else if (action.equals("authentication")) output = authenticate((Administrator)(request.getSession().getAttribute("user")),(Vector)input,getCurrentEquipmentType(request)) ;
		else if (action.equals("getCurrentUser")) output = getCurrentUser(request) ;
		else if (action.equals("getCurrentEquipmentType")) output = getCurrentEquipmentType(request);
		else if (action.equals("getEquipmentTypes"))output = stateStamper.getEquipmentTypes();
		else if (action.equals("getAreas"))output = stateStamper.getAreas();
		else if (action.equals("getEquipmentType"))output= stateStamper.getEquipmentType((Integer)input);
		else if (action.equals("getDefaultEquipmentType"))output= stateStamper.getDefaultEquipmentType() ;
		else if (action.equals("getWarInfo")){output=stateStamper.getWarInfo();}
		else if (action.equals("getSiteName")){output=stateStamper.getSiteName();}
		else if (action.equals("getParentEquipmentType")){output=stateStamper. getParentEquipmentType((EquipmentType)input);}
		else if (action.equals("getParentList")){output=stateStamper.getParentList((Machine)input);}
		else if (action.equals("getParentListFilterFields")){output=stateStamper.getParentListFilterFields((EquipmentType)input);}
		else if (action.equals("getlocationList")){output=stateStamper.getLocationList((Machine)input);}
		else if (action.equals("updateEquipmentAttributes")){output=stateStamper.updateEquipmentAttributes((Map)input);}
		else if (action.equals("getConfigurationKey")){output=stateStamper.getConfigurationKey((String)input);}
		else if (action.equals("getReportDefinition")){output=stateStamper.getReportDefinition((Report)input);}
		else if (action.equals("validateDurable")){output = DurableManager.validateDurable((String)input); }
		else if (action.equals("reloadSettings")){output = stateStamper.reloadSettings();}
		else if (action.equals("getDurableTransactionHistory")){
		List result = new Vector();
		String durableId=(String)input;
		result.add(DurableManager.getDurableDetails(durableId));
		result.add(DurableManager.getDurableTransitionHistory(durableId));	
		output= result;
		}else if (action.equals("attributeValidator")){output=AttributeValidator.validate();page="attribute_validator.jsp";}
		else if (action.equals("childValidator"))
			{
			String equipmentType = request.getParameter("equipmentType");
			Integer equipmentTypePK = null;
			if(equipmentType==null)equipmentTypePK =getCurrentEquipmentType(request).primaryKey;
			else equipmentTypePK = Integer.valueOf(equipmentType);
			output=ChildValidator.validate(equipmentTypePK);
			page="child_validator.jsp";
			}
		else if (action.equals("reloadEquipments")){output = "";stateStamper.reloadEquipments((EquipmentType)input);}
		else if (action.equals("getEquipmentTypeByName")){output = stateStamper.getEquipmentTypeByName((String)input);}
		else if (action.equals("getAreaByName")){output = stateStamper.getAreaByName((String)input);}
		else if (action.equals("getUserFullName")){ response.getWriter().write(stateStamper.getUserFullName(request.getParameter("coreid")));}
		else if (action.equals("tester_attached_durable")) {page = "tester_attached_durable.jsp";}
		else if(action.equals("results_tester_attached_durable") ){
			output = DurableManager.getAttachedDurablesMonitor(request.getParameter("hostname"));
			page = "tester_attached_durable.jsp";
		}
		else if (action.equals("getMachineModel"))
			{
			String platform = (String)input;
			Area area = getCurrentArea(request);
			EquipmentType et = getCurrentEquipmentType(request);
			output = stateStamper.getMachineModel(et,area,platform);
			}
		else if (action.equals("getChildModel"))
			{
			String model = (String)input;
			EquipmentType et = getCurrentEquipmentType(request);
			output = stateStamper.getChildModel(et, model);
			}
		else if (action.equals("getMachinePlatform"))
			{
			Area area = getCurrentArea(request);
			EquipmentType et = getCurrentEquipmentType(request);
			output = stateStamper.getMachinePlatform(et,area);
			}
		else if (action.equals("getModelByDescription"))
			{
			Area area = getCurrentArea(request);
			EquipmentType et = getCurrentEquipmentType(request);
			String description = (String)input;
			output = stateStamper.getModelByDescription(et, area, description);
			}
		else if(action.equals("updateMachineStatus"))
			{
			output = stateStamper.updateMachineStatus((List<String>)input);
			}
		else if(action.equals("getMachinesByMesNames"))
			{
			output = stateStamper.getMachinesByMesNames((List<String>)input);
			}
		return new Object[]{output,page};
		}
	
	
	private EquipmentType getCurrentEquipmentType(HttpServletRequest request)
		{
		EquipmentType type = (EquipmentType)request.getSession().getAttribute("equipmentType");
		if(type == null || type.primaryKey == null) type = stateStamper.getDefaultEquipmentType();
		return type;
		}
	
	private static Administrator getCurrentUser(HttpServletRequest request) throws Throwable{
		Administrator admin = (Administrator)(request.getSession().getAttribute("user"));
		if (admin==null) throw new Throwable("No user information available!");
		if (admin.coreId==null||admin.coreId.equals("")) admin.coreId = "Guest";
		return admin;		
	}
			
	protected Object[] processDurableRequest (String action, Object input, HttpServletRequest request, HttpServletResponse response) throws Throwable {
		Object output = null;
		String page = null;		
		if (!authenticate((Administrator)(request.getSession().getAttribute("user")),action,getCurrentEquipmentType(request),request.getParameter("automatic_request"))){
			output = new HashMap();
			((Map) output).put("equipmentTypes", stateStamper.search(new EquipmentType()));
			((Map) output).put("areas", stateStamper.getLoginAreas());
			String originPage = (String) request.getAttribute("originPage");
			if (originPage != null)
				{
				((Map) output).put("originPage", originPage);
				}
			if (action.equals("durable_main")) page = "durable_login.jsp";
			else page = "durable_errlogin.jsp";
		}				
		else if (action.equals("durable_main")) {
		   EquipmentType eqType = getCurrentEquipmentType(request);
		   Area area = getCurrentArea(request);
		    List activities = stateStamper.getActivityList(eqType);
		    List reports    = stateStamper.reports.getDurableRealTimeReport(eqType,area,"Realtime");
		    List monitors   = stateStamper.getDurableMonitorsMain(eqType);  
		    output = new Vector();
		    ((List)output).add(activities);
		    ((List)output).add(reports);
		    ((List)output).add(monitors);
			page = "durable_main.jsp";
		}						
		else if (action.equals("durable_login")) {page = durableLogin(request,response);} 
		else if (action.equals("durable_switchtype"))
			{
			try
				{
				Integer typePk = Integer.parseInt((String) request.getParameter("newEquipmentType"));
				Integer areaPK = Integer.parseInt((String) request.getParameter("newArea"));
				Area area = stateStamper.getAreas().get(areaPK);
				EquipmentType equipmentType = stateStamper.getEquipmentType(typePk);
				boolean allowGuest = false;
				Role role = getGuestRole(typePk);
				if (role!= null) 
					{
					allowGuest = true;
					}			
				Administrator user = (Administrator) request.getSession().getAttribute("user");
				String coreId = user.coreId;
				Administrator returnUser = (Administrator) authorize(coreId, typePk, areaPK);
				request.getSession().setAttribute("equipmentType",equipmentType);
				request.getSession().setAttribute("area",area);
				page = "durable_main.do";
				if (returnUser != null)
					{
					request.getSession().setAttribute("user", returnUser);
					}
				else
					{
					if (allowGuest&&user!=null)
						{
						user.setRolePK(role.rolePK);
						user.setRole(role);
						request.getSession().setAttribute("user", user);
						}
					else
						{
						// if user does not have role for the equipment Type PK
						// and guest is not allowed
						logout(request);
						page = "index.jsp";
						}
					}
				}
			catch (Throwable ex)
				{
				logger.error(ex.getMessage(),ex);
				}
			}
		else if (action.equals("durable_logoff")) {
			try{logout(request);}
			finally{page = "index.jsp";} 
		}else if (action.equals("durable_transaction_search"))
			{
			output = DurableManager.generateDurableSimpleReport(request, response, true);
			page = "durable_transaction_search.jsp";
			}
		else if (action.equals("durable_transaction_details")) {
		    String durableId = request.getParameter("durable_id");
		    String activity  = request.getParameter("activity");
		    Administrator user = (Administrator)request.getSession().getAttribute("user");
		    EquipmentType eqType = getCurrentEquipmentType(request);
		    List transitions = stateStamper.getTransitionsByActivity(eqType, activity);
		    String toSubstate = request.getParameter("toSubstate");
		    Map transitionDetails = new HashMap();
		    if(transitions.size()>0)
		    	{ 
		    	Transitions trans = ((Transitions)transitions.get(0)); 
		    	Substate to = trans.getToState();
		    	to.primaryKey = trans.toStatePk;
		    	toSubstate = String.valueOf(to.primaryKey);
		    	transitionDetails.put("activityState", to);
		    	}
		    transitionDetails.putAll(DurableManager.getTransitionDetails(durableId,user,toSubstate));
			output = transitionDetails;
			page = "durable_transaction_details.jsp";
		}		
		else if (action.equals("durable_transaction")) {
		    Administrator user = (Administrator)request.getSession().getAttribute("user");
		    Map tmp = DurableManager.getTransitionAttributes(request);
		    Map attribs = (HashMap)tmp.get("attribute");
		    Map attachments = (HashMap)tmp.get("attachment");    
		    Map res = DurableManager.transition(user, attribs,attachments);
			page = "durable_transition_result.jsp";
			output = new HashMap();
			((Map)output).putAll(res);
		}   
		else if (action.equals("durable_getAttachment")){ 
			String file = request.getParameter("file");
			String mime = getServletContext().getMimeType(file);
			DurableManager.getAttachment(file,mime,response);
		}
		
		else if (action.equals("durable_search")) {
			String[] searchFields = {"mESName","type","SUBSTATE"};
			output = new HashMap();
			EquipmentType eqType = getCurrentEquipmentType(request);
			Area area = getCurrentArea(request);
			((Map)output).putAll(DurableManager.getDurableSearchResults(eqType,area,searchFields,new HashMap()));
			page = "durable_search_details.jsp";
		}		
		else if (action.equals("durable_search_list")) {			
			String[] searchFields = {"mESName","type","SUBSTATE"};
			output = new HashMap();
			EquipmentType eqType = getCurrentEquipmentType(request);
			Area area = getCurrentArea(request);
			((Map)output).putAll(DurableManager.getDurableSearchResults(eqType,area,searchFields,request.getParameterMap()));
			page = "durable_search_details.jsp";
		}		
		else if (action.equals("durable_commentList")) {
			String status_pkStr= request.getParameter("status_pk");
			output = new Vector();
			((Vector)output).add(DurableManager.getDurableDetails(request.getParameter("durable_id"),status_pkStr));
			page = "durable_add_comments.jsp";
		}
		else if (action.equals("durable_addComment")) {			
			String durable_id = request.getParameter("durable_id");
			String status_PK= request.getParameter("status_pk");
			Administrator admin = (Administrator)(request.getSession().getAttribute("user"));
			String user= admin.coreId;
			String content= request.getParameter("comments");
            DurableManager.addcomment(durable_id, status_PK, user, content);
        	output = new Vector();
            ((Vector)output).add(DurableManager.getDurableDetails(durable_id,status_PK));
            page = "durable_add_comments.jsp";
		}		
		else if (action.equals("durable_details")) {			
			Machine durable = stateStamper.getMachine(request.getParameter("durable_id"));
			String statusPk = request.getParameter("status_pk");
			boolean isCurrent = statusPk == null || statusPk.equals("") || statusPk.equalsIgnoreCase("null");
			EquipmentType type = durable.getEquipmentType();
			EquipmentType parentType = stateStamper.getParentEquipmentType(type);
			output = new Vector();						
			if (isCurrent)
				{
				((List)output).add(DurableManager.getDurableDetails(request.getParameter("durable_id")));
				Report report = new Report();
				report.reportType = "History";
				report.grouping = durable.getEquipmentType().name;
				report.period = "Raw";
				report.setTransitionOnly(true);
				report.setAternativeDisplay(true);
				report.setEquipmentType(type);
				report.setArea(durable.getArea());
				report = DurableManager.getDefaultReport(report);

				Machine selectedDurable = stateStamper.getMachine(request.getParameter("durable_id"));
				Map hashMap = new HashMap();
				hashMap.put(request.getParameter("durable_id"), selectedDurable);
				report.machines = hashMap;
				((List) output).add(DurableManager.generateDurableSimpleReport(request, response, false, report));				
				}
			else
				{
				((List)output).add(DurableManager.getDurableDetails(request.getParameter("durable_id"),statusPk));
				((List)output).add(new Vector());			
				}				
			((List)output).add(type);
			((List)output).add(parentType);
			((List)output).add(isCurrent);
			((List)output).add(durable);
			page = "durable_details.jsp";
		}	
		else if (action.equals("durable_attachment_management")) {
			output = new Vector();	
			String durableId = request.getParameter("durable_id");
			EquipmentType type = getCurrentEquipmentType(request);
			((Vector)output).add(this.stateStamper.getMachine(durableId));
			((Vector)output).add(type.getAttributesByType(EquipmentAttribute.ATTACHMENT));
			page = "durable_attachment_management.jsp";
		}
		else if (action.equals("durable_add_attachment")) {
			output = new Vector();	
			String durableId = DurableManager.addAttachment(request);
			EquipmentType type = getCurrentEquipmentType(request);
			((Vector)output).add(this.stateStamper.getMachine(durableId));
			((Vector)output).add(type.getAttributesByType(EquipmentAttribute.ATTACHMENT));
			page = "durable_attachment_management.jsp";
		}else if (action.equals("durable_delete_attachment")) {
			output = new Vector();	
			String durableId = request.getParameter("durable_id");
			EquipmentType type = getCurrentEquipmentType(request);
			Integer attachmentPK = Integer.parseInt(request.getParameter("attachment_pk"));
			DurableManager.removeAttachment(attachmentPK, durableId);
			((Vector)output).add(this.stateStamper.getMachine(durableId));
			((Vector)output).add(type.getAttributesByType(EquipmentAttribute.ATTACHMENT));
			page = "durable_attachment_management.jsp";
		}
		else if (action.equals("durable_association_list")) {
			String selectedParent = request.getParameter("parent");
			String message = request.getParameter("message");
			if (message==null||message.equals("null")) message = "";
			if (selectedParent==null||selectedParent.equals("null")) selectedParent = "";
			String durableId = request.getParameter("durable_id");
			Administrator admin = (Administrator)(request.getSession().getAttribute("user"));
			EquipmentType type = getCurrentEquipmentType(request);
			String parentType = stateStamper.getParentEquipmentType(type).name;
			Machine machine = stateStamper.getMachine(durableId);
			List parents = stateStamper.getParentList(machine);
			output = new Vector();						
			((Vector)output).add(selectedParent);
			((Vector)output).add(message);
			((Vector)output).add(durableId);
			((Vector)output).add(parentType);
			((Vector)output).add(parents);
			page = "durable_association.jsp";
		}
		else if (action.equals("durable_association")) {
			String selectedparent = request.getParameter("parent");
			String durableId = request.getParameter("durable_id");		
			Administrator admin = (Administrator)(request.getSession().getAttribute("user"));
			EquipmentType type = getCurrentEquipmentType(request);			
			String parentType = stateStamper.getParentEquipmentType(type).name;
			output = new Vector();	
			String message = "";	
			if (DurableManager.associateDurable(durableId, selectedparent, admin.coreId)){						
				message = "Update is successful";
				((Vector)output).add(selectedparent);
			}	
			else {
				message = "Update is not successful";
				((Vector)output).add("");
			}
			((Vector)output).add(message);
			((Vector)output).add(durableId);
			((Vector)output).add(parentType);
			Machine machine = stateStamper.getMachine(durableId);
			((Vector)output).add(stateStamper.getParentList(machine));
			page = "durable_association.jsp";
		}else if (action.equals("durable_attach_children_durable_list")){
			String durableId = request.getParameter("durable_id");
			output = new HashMap();
			((Map)output).put("durableId",durableId);
			Machine durable = stateStamper.getMachine(durableId);
			((Map)output).put("durable",durable);
			List attachableChildren  = DurableManager.getAttachableChildren(durableId);
			((Map)output).put("attachableChildren",attachableChildren);
			int childrenNumber = DurableManager.getAttachableChildrenNumber(durableId);
			((Map)output).put("childrenNumber",childrenNumber);
			String childrenType = DurableManager.getChildrenTypeForAttachChildAction(durableId);
			((Map)output).put("childrenType",childrenType);
			Map<String,Machine> currentChildren = DurableManager.getCurrentChildren(durable,childrenType);
			((Map)output).put("currentChildren",currentChildren);
			String index = DurableManager.getAttachChildIndexVariable(durableId);
			if(index != null){((Map)output).put("indexVar",index);}
			page = "durable_attach_children.jsp";
		}else if (action.equals("durable_attach_children")){
			String parent = request.getParameter("parent_id");
			Map<String,String>  children =  DurableManager.getChildrenIdsToAttach(request.getParameterMap());
			Administrator admin = (Administrator)(request.getSession().getAttribute("user"));
			output = new HashMap();
			((Map)output).put("attach_result",DurableManager.attachChildrenToParent(parent,children,admin));
			Machine durable = stateStamper.getMachine(parent);
			((Map)output).put("durable",durable);
		    page = "durable_attach_children_result.jsp";
		}else if (action.equals("durable_monitor_threshold")) {
			Map tmp = new HashMap();
			EquipmentType eqType = getCurrentEquipmentType(request);
			tmp.put("rules", eqType.getBusinessRulesBy(BusinessRule.MONITOR_PAGE));
			tmp.put("results", new Vector());
			output = tmp;
			page = "durable_threshold_monitor.jsp";
		}else if (action.equals("durable_monitor_threshold_list")) {
			Map tmp = new HashMap();
			EquipmentType eqType = getCurrentEquipmentType(request);
			Area area = getCurrentArea(request);
			tmp.put("rules", eqType.getBusinessRulesBy(BusinessRule.MONITOR_PAGE));
			String[] format = { "DURABLE_ID", "deviceGroup", "conditionattribute", "limit", "usage" };
			String monitorType = request.getParameter("monitorType");
			tmp.put("results",DurableManager.getMonitorResults(eqType,area,new Integer(Integer.parseInt(request.getParameter("condition"))),
							Double.parseDouble(request.getParameter("warning")), format, 5, monitorType));
			tmp.put("warningLimit", Double.parseDouble(request.getParameter("warning")));
			tmp.put("criticalLimit", Double.parseDouble(request.getParameter("critical")));
			tmp.put("monitorType", monitorType);
			output = tmp;
			page = "durable_threshold_monitor.jsp";
		}else if(action.equals("durable_monitor"))
			{
			String monitor = request.getParameter("monitor");
			EquipmentType eqType = (EquipmentType)request.getSession().getAttribute("equipmentType");
			Area area = getCurrentArea(request);
			if(eqType == null)
				{
				String eqTypeName = request.getParameter("equipmentType");
				eqType = stateStamper.getEquipmentTypeByName(eqTypeName);
				}
			Map results = DurableManager.getMonitorResults(eqType,area,monitor);
			output = results;
			page = "durable_monitor.jsp";
		}else if (action.equals("durable_realtime_simple_report")) {output =DurableManager.generateDurableSimpleReport(request, response, true);page = "durable_realtime_simple_report.jsp";}
		else if (action.equals("durable_realtime_simple_history_report")){output = DurableManager.durableRealtimeSimpleHistoryReport(request,response);page = "durable_history_simple_report.jsp";}
		else if(action.equals("durable_realtime_simple_report_excel")){response.setContentType("application/vnd.ms-excel");DurableManager.durableRealtimeSimpleReportExcel(request,response);}
		else if(action.equals("durable_inventory")){
	 		Report report = DurableManager.getInventoryReport(request);
	 		Hashtable data = stateStamper.reports.generateDurableReport(report,response, false, false);	 		 		
	 		String range = request.getParameter("range"); 		
			output = DurableManager.getInventory(report,data,range);
			page = "durable_inventory.jsp";
		}else if(action.equals("durable_inventory_excel") ){//EXCEL inventory
			Report report = DurableManager.getInventoryReport(request);			
			Hashtable data = stateStamper.reports.generateDurableReport(report,response, false, false);	 		 		
			response.setContentType("application/vnd.ms-excel");
			DurableManager.getInventoryExcel(response.getOutputStream(),report, data);
		}
		return new Object[]{output,page};
	}
	
	private Machine getMachineFromMap(Map parameters)
		{
		Machine machine = new Machine();
		String name;
		String value;
		Iterator it = parameters.entrySet().iterator();
		while(it.hasNext())
			{
			Map.Entry entry;
			 entry = (Map.Entry)it.next();
			 name = (String)entry.getKey();
			 value = ((String[])entry.getValue())[0];
			 if("equipmentType".equals(name)){
				 EquipmentType eqType = stateStamper.getEquipmentTypeByName(value);
				 machine.setEquipmentType(eqType);
			 }else if("substate".equals(name) || "state".equals(name)){
			     name = name.toUpperCase();
				 DurableStatus durableStatus = new DurableStatus();//manually inserting durable status since it is a brand new object and does not have it in tis context
				 durableStatus.set(name, value);
				 machine.context = new HashMap();
				 machine.context.put("durableStatus", durableStatus);
			 }
			 machine.set(name, value);
			}
		return machine;
		}
	
	private void logout(HttpServletRequest request){
		HttpSession session = request.getSession(true);
		session.removeAttribute("user");
		session.removeAttribute("equipmentType");
		session.removeAttribute("equipmentTypes");
		session.removeAttribute("areas");		
		session.removeAttribute("area");
		session.invalidate();
	}
	
	/**
	 * Login
	 * @param request The HTTP request
	 * @param userClass The user object class
	 * @param url The LDAP URL
	 * @param serviceUser The service account ID
	 * @param servicePassword The service account password
	 * @param user The user core ID
	 * @param password The user password
	 * @return The user object
	 * @throws Throwable
	 */
	private Object login(HttpServletRequest request, String url, String serviceUser, String servicePassword, String user, String password, Integer equipmentTypePK,Integer areaPK) throws Throwable
		{
		Administrator loginUser = new Administrator();
		loginUser.setCoreId(user);
		EquipmentType equipmentType;
		if(equipmentTypePK != null)
			{
			equipmentType = (EquipmentType) stateStamper.getEquipmentType(equipmentTypePK);
			}
		else
			{
			equipmentType = stateStamper.getDefaultEquipmentType();
			}
		Area area = stateStamper.getAreas().get(areaPK);
		if(!Area.AREA_ALL_PK.equals(equipmentType.getArea().getPrimaryKey())
			&&!equipmentType.getArea().getPrimaryKey().equals(area.getPrimaryKey()))
			throw new Exception("The equipment type "+equipmentType.name+" doesn't belong to area "+area.getName());
		// check if allowed visitor for this type;
		Role guest = getGuestRole(equipmentTypePK);
		boolean allowGuest = guest != null;
		Object userObject = null;
		// because somehow if we give password =null oneID authentication still success
		if (user != null && password != null && !user.trim().equals("") && !password.trim().equals("") && !isAPIUser(loginUser))
			{
			Authentication.authenticate(url, serviceUser, servicePassword, user, password);
			userObject = authorize(user, equipmentTypePK, areaPK);
			}
		if (userObject != null)
			{
			loginUser = (Administrator) userObject;
			}
		//Allowed user as Guest for non-TTT user after LDAP authentication 
		else if (userObject == null && allowGuest) 
			{		
			loginUser.firstName = guest.rolename;
			loginUser.equipmentTypePK = equipmentTypePK;
			loginUser.setRolePK(guest.getRolePK());
			loginUser.setRole(guest);
			loginUser.setArea(area);
			}
		else
			{
			logger.error("CoreId" + loginUser.coreId + " "+ loginUser.firstName +" " + " User not authorized to login using the selected equipment type and area");
			throw new Throwable("User not authorized to login using the selected equipment type and area");
			}
		request.getSession().setAttribute("user", loginUser);
		request.getSession().setAttribute("equipmentType", equipmentType);
		Map types = stateStamper.getEquipmentTypes();
		List typelist = new ArrayList(types.values());
		request.getSession().setAttribute("equipmentTypes", typelist);
		Map<Integer, Area> areas = stateStamper.getAreas();
		request.getSession().setAttribute("areas", stateStamper.getLoginAreas());
		request.getSession().setAttribute("area", areas.get(areaPK));
		request.getSession().setMaxInactiveInterval(30*60);
		return loginUser;
		}
	
	private boolean isAPIUser(Administrator user)
		{
		return user !=  null && "TTTAPIUSER".equals( user.getCoreId());
		}

	/**
	 *get the VisitorRolePK for that equipment type, return null if 
	 *
	 */
	private Role getGuestRole(Integer equipmentTypePK) throws Throwable
		{
		Role role = new Role();
		role.equipmenttypePK = equipmentTypePK;
		role.rolename = Role.GUEST;
		Vector roles = Persistence.search(role);
		boolean allowGuest = (roles != null) && (!roles.isEmpty());
		Role guest=null;
		if (allowGuest)
			{
			guest = (Role) roles.get(0);
			}
		return guest;
		}
	
	/**
	 * Authorize user. Return the user entity or throw an exception if user not authorized
	 * @param userClass The entity class that holds the user's information
	 * @param user The user ID
	 * @return An object of class userClass with the user's info
	 * @throws Throwable If the database is unreachable or the user is not authorized to use this site
	 */
	private Object authorize(String user, Integer equipmentTypePK, Integer areaPK) throws Throwable
		{
		Administrator loginUser = new Administrator();
		loginUser.equipmentTypePK = equipmentTypePK;
		loginUser.setCoreId(user.toLowerCase());// check lower case first
		Administrator returnUser = authorizeUser(loginUser,areaPK);
		if (returnUser == null)
			{
			loginUser.setCoreId(user.toUpperCase());// check upper case
			returnUser = authorizeUser(loginUser,areaPK);
			}
		return returnUser;
		}	
	
	private Administrator authorizeUser(Administrator user, Integer areaPK) throws Throwable 
		{
		Integer equipmentTypePK = user.equipmentTypePK;
		Vector users = stateStamper.search(user);
		Administrator returnUser = null;
		for (int i = 0; i < users.size(); i++)
			{
			Administrator tmpUser = (Administrator) users.get(i);
			if(areaPK.equals(tmpUser.getArea().getPrimaryKey())||Area.AREA_ALL_PK.equals(tmpUser.getArea().getPrimaryKey()))
				{
				Role role = tmpUser.getRole();
				if (role.equipmenttypePK.equals(equipmentTypePK) || role.equipmenttypePK.equals(0))
					{
					returnUser = tmpUser;
					}
				}
			}
		return returnUser;
		}
	
	private String durableLogin(HttpServletRequest request, HttpServletResponse response)
		{
		String page = null;
		try
			{
			int eqTypePk = Integer.parseInt(request.getParameter("equipmentType"));
			int areaPk = Integer.parseInt(request.getParameter("area"));
			EquipmentType eqType = stateStamper.getEquipmentType(eqTypePk);
			Area area = stateStamper.getAreas().get(areaPk);
			Object user = login(request, URL, SERVICE_USER, SERVICE_PASSWORD, request.getParameter("sUserId"),
					request.getParameter("sPassword"), eqTypePk,areaPk);
			
			if (eqType != null && eqType.name != null && eqType.name != "")
				{
				Cookie eqTypeCookie = new Cookie("lastEquipmentType", eqType.name);
				eqTypeCookie.setMaxAge(MONTH_SECS);
				response.addCookie(eqTypeCookie);
				}
			if (area != null && StringUtils.isNotEmpty(area.getName()))
				{
				Cookie areaCookie = new Cookie("lastArea", area.getName());
				areaCookie.setMaxAge(MONTH_SECS);
				response.addCookie(areaCookie);
				}
			if (user != null) { page = "durable_main.do";     }
			else              { page = "durable_errlogin.jsp";}
			}
		catch (Throwable ex)
			{
			page = "durable_errlogin.jsp";
			logger.error(ex.getMessage(),ex);
			}
		return page;
		}
	
	/**
	 * Creates a report definition by parsing the request data
	 * @param request The servlet request object
	 * @return A report object
	 * @throws Throwable
	 */
	private Report parseInput(HttpServletRequest request) throws Throwable
		{
		Report report = new Report();	
		report.reportType = request.getParameter("reportType");
		report.grouping = request.getParameter("grouping");
		String[] machines = request.getParameter("machines").split(",");
		report.machines = new HashMap();
		for (int i=0; i<machines.length; i++) if (!machines[i].equals("")) report.machines.put(machines[i],"");
		report.period = request.getParameter("period");
		report.dates = request.getParameter("dates").split(",");
		if (request.getParameter("startDate") != null && !request.getParameter("startDate").equals(""))
			{
			report.startDate = formatter.parse(request.getParameter("startDate"));
			report.endDate = formatter.parse(request.getParameter("endDate"));						
			}
		report.isHTML = new Boolean(request.getParameter("isHTML")!=null);
		report.setEquipmentType( getCurrentEquipmentType(request) );
		report.setArea(getCurrentArea(request));
		return report;
		}
	/**
	 *check if the user is allowed to perform certain actions
	 *
	 */
	private static boolean authenticate(Administrator admin, String subaction, EquipmentType equipmentType, String automatic)throws Throwable
		{		
		if (subaction.equals("durable_login")||subaction.equals("durable_logoff") || subaction.equals("durable_details") || Boolean.valueOf(automatic)) {return true;}		
		if (admin==null||admin.getCoreId()==null||subaction ==null || equipmentType ==null||equipmentType.primaryKey==null){ return false;}		
		Integer role =admin.getRolePK();
		Integer equipmentTypePK = equipmentType.primaryKey;		
		//find the right permission						
		if (role==null||equipmentTypePK ==null ) {return false;}					
		Vector permissionPKVector = getPermissionPKs(subaction);
		if (permissionPKVector ==null ||permissionPKVector.isEmpty()){return false;}
		for (int i = 0; i<permissionPKVector.size();i++)
			{							
			Integer permissionPK = (Integer)permissionPKVector.get(i);			
			Permission permission = new Permission();
			permission.permissionPK = permissionPK;			
			permission = (Permission)Persistence.search(permission, "roles");
			List roles = permission.getRoles();
			Iterator it = roles.iterator();
			while (it.hasNext())
				{
				Role tmprole = (Role) it.next();
				if (tmprole.rolePK.equals(role)&& 
					(tmprole.equipmenttypePK.equals(0) || tmprole.equipmenttypePK.equals(equipmentTypePK)))
					{
					return true;
					}
				}
			}	
		return false;
		}
		
	private static Vector authenticate(Administrator admin, Vector functions, EquipmentType equipmentType) throws Throwable{
		Vector allowedfunctions = new Vector();
		for (int i=0;i< functions.size();i++)
		{
			String subaction = (String)functions.get(i);
			if (authenticate(admin,subaction,equipmentType,"")){
				allowedfunctions.add(subaction);
			}					
		}
		return allowedfunctions;
	}
	
	private static Vector getPermissionPKs(String subaction)throws Throwable{			
		Permission permission = new Permission();
		Vector permissionvector = Persistence.search(permission);
		Iterator it = permissionvector.iterator();
		Vector permissions = new Vector();
		while (it.hasNext())
		{
			Permission p = (Permission)it.next();
			//search through the subaction vector
			String[] subactions = p.subaction;	
			for (int i = 0; i< subactions.length;i++)
			{
				if (subactions[i].equalsIgnoreCase(subaction))
					permissions.add(p.permissionPK);
			}
		}
		return permissions;						
	}
	
	private static Area getCurrentArea(HttpServletRequest request)
		{
		Area area = (Area)request.getSession().getAttribute("area");
		if(area == null || area.getPrimaryKey() == null) area = stateStamper.getDefaultArea();
		return area;
		}
	}
