<%@ include file ="header.jsp" %>
<%@page import="com.freescale.stateStamper.model.entity.*"%>
<%@page import="java.util.*"%>
<%@page import="java.net.URLEncoder"%>
<%@page import="com.freescale.stateStamper.model.logic.StateTransition"%>
<%@page import="com.freescale.stateStamper.model.logic.FiscalDate"%>
<jsp:useBean id="result" scope="request" class="java.util.Vector"/>
<html>
<%	  
    List headers = (ArrayList)result.get(0);
	List resultList = (ArrayList)result.get(1);	
	Hashtable filters =  (Hashtable)result.get(2);	
	Hashtable selectedfilter = (Hashtable)result.get(3);
	Report report = (Report)result.get(4);
	List displayheaders = (ArrayList)result.get(6);
	String transitionOnly = (String)result.get(7);
	String startdate = (String)result.get(8);
	String enddate = (String)result.get(9);
%>
<h1><%=report.reportType%> <%= site %></h1>
<p>
<a href="durable_main.do">Back to Durable Main page</a>
<style type="text/css"><!--td {white-space: normal;}--></style>
<table class="simplereport">
<tbody>
<form id="searchForm" name="searchForm" method="get" action="durable_realtime_simple_history_report.do">
<tr>
<td class="simplereport">Index</td>
<% 
Iterator headerIterator = headers.iterator();
Iterator displayheaderIterator = displayheaders.iterator();
String addanotherheader = "";
while (headerIterator.hasNext()) {   
    String header = "";
    String headerDisplay = "";
    if (addanotherheader.equalsIgnoreCase("")){
		header = (String)headerIterator.next();
		headerDisplay = ((String)displayheaderIterator.next()).toUpperCase();
    }else{
    	header = addanotherheader;
    	headerDisplay = addanotherheader;
    	addanotherheader = "";
    }
    //for historical report to set formstate and tostate   
    if (header.equalsIgnoreCase("STATE-SUBSTATE")&&(report.reportType.equalsIgnoreCase("History")||report.reportType.equalsIgnoreCase("History_Details"))){
    	header = "FROMSTATE";
    	headerDisplay =  "FROMSTATE";
    	addanotherheader = "TOSTATE";
    }
    if (header.equalsIgnoreCase("STATUS_PK")){
    	continue;
    }
%>
<td class="simplereport"><%=headerDisplay%>
<br>
<% 
ArrayList filterValueList = (ArrayList)filters.get(header); 
if(filters.get(header)!=null) { %>
<select name="<%=header%>" >
<option  value=""></option>
<%  
	Iterator filterValueIterator = filterValueList.iterator();	
	String selectedValue = "";
	while (filterValueIterator.hasNext()){ 				
		selectedValue = "";	
		String value = (String)filterValueIterator.next();
		if (selectedfilter.get(header).equals(value)) 
			{
			selectedValue = "selected";
			}
%>			
<option  value="<%=(String)value%>" <%=selectedValue%>><%=(String)value%></option>			
<% }%>
</select>
<% }if (header.equalsIgnoreCase("TIME")){ %>
	start date<input type="text" name="startdate" id="startdate" value="<%=startdate%>"/><a href="javascript:NewCal('startdate','yyyymmdd')"><img src="images/cal.gif" width="16" height="16" border="0" alt="Pick a date"></a>
	<br>
	end date<input type="text" name="enddate" id="enddate" value="<%=enddate%>"/><a href="javascript:NewCal('enddate','yyyymmdd')"><img src="images/cal.gif" width="16" height="16" border="0" alt="Pick a date"></a>
	<br>
	<input type="Submit" id="submit" value="Submit" /></td>
<%}%>
</td>
<% }%>
</tr>
<input type="hidden" name="reportType" value="<%=report.reportType %>"/>
<input type="hidden" name="grouping" value="<%=report.grouping %>"/>
<input type="hidden" name="transitionOnly" value="<%=transitionOnly%>"/>
</form>
<% Iterator it = resultList.iterator();
	int index = 0;
 while (it.hasNext()){
	 DurableStatus durable= (DurableStatus)it.next();
	 index ++;
%>
<tr> 
<td class="simplereport"><%=index%></td>
    <%  for(int i =0 ; i<headers.size() ; i++) 
    	{    	
        if (((String)headers.get(i)).equalsIgnoreCase("STATUS_PK")){
    	continue;
    	}
    	if ("DURABLE_ID".equalsIgnoreCase((String)headers.get(i)))
    		{
    	%>
    	<td class="simplereport"><a href="durable_details.do?durable_id=<%=(String)durable.get("DURABLE_ID")%>&status_pk=<%=durable.get("STATUS_PK")%>"><%=(String)durable.get("DURABLE_ID")%></a></td>

<%}else if (((String)headers.get(i)).equalsIgnoreCase("STATE-SUBSTATE")&&(report.reportType.equalsIgnoreCase("History")||report.reportType.equalsIgnoreCase("History_Details"))){%>		
<td class="simplereport"><%=(String)durable.get("FROMSTATE")%></td>
<td class="simplereport"><%=(String)durable.get("TOSTATE")%></td>
<%}
else if (((String)headers.get(i)).equalsIgnoreCase("COMMENTS")){	 	
		Set tmpSet = ((Map)(durable.get((String)headers.get(i)))).keySet();
	 	Vector tmpList = new Vector(tmpSet);
	 	Collections.sort(tmpList);	
	 	Iterator commentIt = tmpList.iterator();
	 	if (tmpSet.size()>1) {
	 		String tmpvalue = "";
	 		String fullcomment = "";
	 		int count = 0;
	 		while (commentIt.hasNext()){
	 			List comment = (List)(((Map)durable.get((String)headers.get(i))).get(commentIt.next()));
	 			if (count ==0) {	 				
		 			tmpvalue =(String)(comment.get(2));
	 			}
	 			count++;
	 			fullcomment = fullcomment +  " - " + comment.get(2) + "<br>";
	 			
	 		}
	 		%>
			<td class="simplereport" onmouseover="return overlib('<%=FiscalDate.escape(fullcomment)%>',CAPTION, '<%=headers.get(i)%>');" onmouseout="return nd();"><%=FiscalDate.trim(tmpvalue,20)%></td>
			<%
	 		
	 	}
	 	else{
	 		
	 		List comment = (List)(((Map)durable.get((String)headers.get(i))).get(commentIt.next()));
	 		String tmpvalue =(String)(comment.get(2));
	 		%>
			<td class="simplereport" onmouseover="return overlib('<%=FiscalDate.escape(tmpvalue)%>',CAPTION, '<%=headers.get(i)%>');" onmouseout="return nd();"><%=FiscalDate.trim(tmpvalue,20)%></td>	
			<% 	
	 	}
	 	
	 	
}else {
		String tmpvalue = durable.get((String)headers.get(i)).toString(); 		
		if (tmpvalue.length()<=20){
			//when the size is under 20
		%>
		<td class="simplereport"><%=tmpvalue%></td>		
		<% 	
			}
			else{
		%>
		<td class="simplereport" onmouseover="return overlib('<%=FiscalDate.escape(tmpvalue)%>',CAPTION, '<%=headers.get(i)%>');" onmouseout="return nd();"><%=FiscalDate.trim(tmpvalue,20)%></td>
<%}}}%>
</tr>
<% }%>
</tbody></table>
<%
Enumeration e = request.getParameterNames();
String name,value;
StringBuilder query = new StringBuilder();
while(e.hasMoreElements())
	{
	name  = URLEncoder.encode((String)e.nextElement(), "UTF-8");
	value = URLEncoder.encode(request.getParameter(name), "UTF-8");
	if(value == null || value.equals("")){continue;}
	query.append(name+"="+value+"&");
	}
if(query.lastIndexOf("&") > -1)
	{
	query.deleteCharAt(query.lastIndexOf("&"));
	}
%>
<a href="durable_realtime_simple_report_excel.do?<%=query.toString()%>" ><img alt="excel" src="images/ExcelReport.gif"> </a>
<%@ include file ="footer.html" %>