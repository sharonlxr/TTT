<%@ include file ="header.jsp" %>
<%@page import="com.freescale.stateStamper.model.entity.*"%>
<%@page import="com.freescale.serverUtil.ThreadSafeSimpleDateFormat"%>
<link rel="stylesheet" type="text/css" href="javascripts/anylinkmenu.css" />
<script type="text/javascript" src="javascripts/anylinkmenu.js">
/***********************************************
* AnyLink JS Drop Down Menu v2.0- © Dynamic Drive DHTML code library (www.dynamicdrive.com)
* This notice MUST stay intact for legal use
* Visit Project Page at http://www.dynamicdrive.com/dynamicindex1/dropmenuindex.htm for full source code
***********************************************/
</script>
<jsp:useBean id="result" scope="request" type="java.util.List"/>
<%
 List activities = (List)result.get(0);
 List reports    = (List)result.get(1);
 List monitorsRules   = (List)result.get(2);
 String startDate = new ThreadSafeSimpleDateFormat("yyyy-MM-dd").format(new Date());
%>
<!--Transitions menu  -->
<script type="text/javascript">
var transitionsMenu={divclass:'anylinkmenu', inlinestyle:'', linktarget:''} 
transitionsMenu.items=[
<%for(int i=0;i <activities.size();i++ )
	{
	String activity = (String)activities.get(i);
	String link = "durable_transaction_search.do?reportType=Transition&grouping="+((EquipmentType)session.getAttribute("equipmentType")).name+"&activity="+activity;
  %>                       
	['<%=activity%>','<%=link%>'],
<%  }%>
	['Transition', 'durable_transaction_search.do?reportType=Transition&grouping=<%=((EquipmentType)session.getAttribute("equipmentType")).name%>']
]

var reportsMenu={divclass:'anylinkmenu', inlinestyle:'', linktarget:''}
reportsMenu.items=[
<% for(int i=0;i<reports.size();i++)
	{
	Report report = (Report)reports.get(i);
	String reportName = report.reportType + " Report";
	String link = "durable_realtime_simple_report.do?reportType="+report.reportType+"&grouping="+report.grouping;
%>
   ['<%=reportName%>','<%=link%>'],
    <%}%>
    <%String historyLink = "durable_realtime_simple_history_report.do?transitionOnly=yes&startdate="+startDate;%>
   ['History','<%=historyLink%>'],
   ['PM Monitor','durable_monitor_threshold.do']
   <% for(int i=0;i<monitorsRules.size();i++)
	{
	BusinessRule rule = (BusinessRule)monitorsRules.get(i);
	String monitorName = rule.getName().replace("_Main"," ") + " Monitor";
	String link = "durable_monitor.do?monitor="+rule.getName();
%>
  ,['<%=monitorName%>','<%=link%>'] 
   <%}%>

   
]
anylinkmenu.init("menuanchorclass");
</script>

<h1>Welcome to Durable Management <%= site %></h1>
<br><img src="images/Splash.png">
<table>
<tr>
<th><a href='durable_transaction_search.do?reportType=Transition&grouping=<%=((EquipmentType)session.getAttribute("equipmentType")).name%>' class="menuanchorclass" rel="transitionsMenu">Transitions</a></th>
<th><a href="durable_search.do">Search</a>&nbsp;&nbsp;&nbsp;</th>
<% EquipmentType et = (EquipmentType)session.getAttribute("equipmentType"); 
   BusinessRule br = et.getBusinessRuleByEvent(BusinessRule.MODEL_CONFIGURATION);
   if(br!=null){
%>
<th><a href="listModel.action">Model Management</a></th>
<%} %>
<th><a href="#" class="menuanchorclass" rel="reportsMenu" >Reports and Tracking</a>&nbsp;&nbsp;&nbsp;</th>
<th><a href="durable_inventory.do">Inventory</a>&nbsp;&nbsp;&nbsp;</th>
<th><a href="durable_logoff.do">Log Out</a>&nbsp;&nbsp;&nbsp;</th>
</tr>
</table>
<br/>
<br/>
<br/>
<br/>
<br/>
<br/>
<br/>
<%@ include file ="footer.html" %>