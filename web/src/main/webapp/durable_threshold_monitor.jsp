<%@ include file ="header.jsp" %>
<%@page import="com.freescale.stateStamper.model.entity.*"%>
<%@page import="java.util.*"%>
<jsp:useBean id="result" scope="request" type="java.util.Map"/>
<script>
	setTimeout("window.location.reload()",300*1000);  // Reload page every minute
</script>
<h1>Durable Threshold Monitor</h1>
<a href="durable_main.do">Back to Durable Main page</a>
<form id="searchForm" name="searchForm" method="get" action="durable_monitor_threshold_list.do">  
<%
  	List rules = (List)result.get("rules");	
  	List durablesResult = (List)result.get("results");	
  	Double warning = 0.0;
  	Double critical = 0.0;
  	String monitorType = "";
  	if (result.size() > 2)
  		{
  		warning = (Double) result.get("warningLimit");
  		critical = (Double) result.get("criticalLimit");
  		monitorType = (String) result.get("monitorType");
  		}
  %>
<table width=100%>
<tr>
<td width=50%>
Attribute
</td>
<td width=50%>
<select name="condition">
	<option  value=""></option>
	<%
	 EquipmentType eType = (EquipmentType)session.getAttribute("equipmentType");
		for (Object obj: rules)
			{
			BusinessRule rule = (BusinessRule) obj;
			if (rule != null && rule.hasActions())
				{
				BusinessRuleAction businessRuleAction = (BusinessRuleAction) rule.getActions().get(0);
				Map<String, String> parameters = businessRuleAction.getActionParameters();
				String count = parameters.get("count");
				String limit = parameters.get("limit");
				EquipmentAttribute countAttribute = (EquipmentAttribute) eType.getAttribute(count);
				if (countAttribute != null && countAttribute.getAttributeAlias() != null)
					{
					count = countAttribute.getAttributeAlias();
					}

				EquipmentAttribute limitAttribute = (EquipmentAttribute) eType.getAttribute(limit);
				if (limitAttribute != null && limitAttribute.getAttributeAlias() != null)
					{
					limit = limitAttribute.getAttributeAlias();
					}
	%>
	<option value="<%=rule.getPrimaryKey()%>"><%=count + "-" + limit%></option>
			 <% }
			}%>
</select>
</td>
</tr>

<tr>
<td width=50%>
Monitor Type
</td>
<td width=50%>
<select name="monitorType">
<option selected="selected" value="maximum_rate">Maximum Limit By Usage Rate</option>
<option value="minimum_rate">Minimum Limit By Usage Rate</option>
<option value="time_left_in_days">Remaining Time in Days</option>
<option value="time_left_in_hours">Remaining Time in Hours</option>
</select>
</td>
</tr>


<tr>
<td width=50%>Warning Rate<br></td>
<td width=50%>
<%if (request.getParameter("warning")==null) { %>
<input type="text" value="0.90" name="warning"> 
<% }else { %>
<input type="text" value="<%=request.getParameter("warning")%>" name="warning"> 
<% }%>
</td>
</tr>

<tr>
<td width=50%>Critical Rate<br></td>
<td width=50%>
<%if (request.getParameter("critical")==null) { %>
<input type="text" value="1.00" name="critical"> 
<% }else {%>
<input type="text" value="<%=request.getParameter("critical")%>" name="critical"> 
<% }%>
</td>
</tr>

<tr>
<td width=50%>
</td>
<td width=50%>
<input type="submit" name="search" value="search">
</td>
</tr>
</table>
</form>

<table border="1">
<tbody><tr>
<td>Durable Id</td>
<td>Device Group</td>
<td>Used</td>
<td>Maximum Limit</td>
<td>&nbsp;</td>
<td>&nbsp;</td>
<td>&nbsp;</td>
<td>&nbsp;</td>
<td>&nbsp;</td>
<td>&nbsp;</td>
<td>&nbsp;</td>
<td>&nbsp;</td>
<td>&nbsp;</td></tr>
<% 
 for (Object obj: durablesResult){
	 String[] itemarray = (String[])obj;
	 String color = "FF7F00";
	 double usage = Double.parseDouble(itemarray[4]);
	 boolean maximunFiltered =  (monitorType.equals("maximum_rate")||monitorType.equals("minimum_rate"));
	 boolean minimunFiltered =  (monitorType.equals("time_left_in_days")||monitorType.equals("time_left_in_hours"));
	 String value = "";
	 if(usage>=critical && maximunFiltered ) 
	 	{
	 	color="FF0000";	    
	 	value = (new Double(Double.parseDouble(itemarray[4])*100)).intValue() + "%";
	 	}
	 else if(usage<critical && maximunFiltered ) 
	 	{
		color="FF7F00";
		value = (new Double(Double.parseDouble(itemarray[4])*100)).intValue() + "%";
	 	}	 
	 else if(usage<=critical && minimunFiltered ) 
	 	{
		color="FF0000";	 
		value = itemarray[4];
	 	}	 
	 else if (usage>critical && minimunFiltered )
		{
		color="FF7F00";
		value = itemarray[4];
		}
%>

<tr>

<td><a href="durable_details.do?durable_id=<%=itemarray[0]%>"><font color="<%=color%>"><%=itemarray[0]%></font></a></td>
<td><font color="<%=color%>"><%=itemarray[1]%></font></td>
<td><%=itemarray[2]%></td>
<td><%=itemarray[3]%></td>
<td>
<font color="<%=color%>"><%=value%></font>
</td>


<td>&nbsp;</td>
<td>&nbsp;</td>
<td>&nbsp;</td>
<td>&nbsp;</td>
<td>&nbsp;</td>
<td>&nbsp;</td>
<td>&nbsp;</td>
<td>&nbsp;</td></tr>
<% }%>
</tbody></table>
<%@ include file ="footer.html" %>