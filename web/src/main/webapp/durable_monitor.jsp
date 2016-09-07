<%@page import="java.util.*"%>
<%@page import="com.freescale.serverUtil.ThreadSafeSimpleDateFormat"%>

<%
boolean automatic = request.getParameter("automatic_request") != null; 
if(!automatic) 
     {%>
     <jsp:include page="header.jsp" />     
<script>
	setTimeout("window.location.reload()",5*60*1000);  
</script>
<%   } %>
<link href="http://swo.freescale.net/css/fsl.css" rel="stylesheet" type="text/css" />	
<jsp:useBean id="result" scope="request" type="java.util.Map"/>
<%
List header;
String criticalConds = (String)result.get("critical_conditions");
List criticalResults = (List)result.get("critical_results");
String warningConds = (String)result.get("warning_conditions");
List warningResults = (List)result.get("warning_results");
String monitorName = request.getParameter("monitor");
ThreadSafeSimpleDateFormat formatter = new ThreadSafeSimpleDateFormat("yyyy-MM-dd HH:mm:ss");
%>
<%!
public String getStringFixedWidth(String src)
	{
	StringBuilder fixed = new StringBuilder();
	int size = 100;
	for(int acum = 0;acum < src.length();)
		{
		int end = 0;
		end = (acum + size) > src.length()? src.length():acum +size; 
		fixed.append(src.substring(acum, end)+"<br>");
		acum+=size;
		}
	return fixed.toString();
	}
%>
<h1><%=monitorName.replace("_Main","")%> Monitor</h1>
<%if(!automatic){%><a href="durable_main.do">Back to Durable Main page</a><%}%>
<br/>
<b>Critical :</b><br/>
<%=getStringFixedWidth(criticalConds)%>
<table width=100%>
<tr>
<% 
header = (List)criticalResults.get(0);
for(int i=0;i<header.size();i++)
	{
%>
<th><%=header.get(i) %></th>
<%  }%>
</tr>
<% for(int i = 1;criticalResults != null && i < criticalResults.size();i++)
	{
	List row = (List)criticalResults.get(i);
	%>
	<tr>
	<%for(int j = 0; row != null && j< row.size();j++)
		{%>
	<td><%=row.get(j) %> </td>
	<%  } %>
	</tr>
<%  }%>
</table>
<br/>
<b>Warning : </b><br/>
<%=getStringFixedWidth(warningConds)%>
<table width=100%>
<tr>
<% 
header = (List)warningResults.get(0);
for(int i=0;i<header.size();i++)
	{
%>
<th><%=header.get(i) %></th>
<%  }%>
</tr>
<% for(int i= 1;warningResults != null && i < warningResults.size();i++)
	{
	List  row = (List)warningResults.get(i);
	%>
	<tr>
	<%for(int j = 0;row != null && j < row.size();j++)
		{%>
	<td><%=row.get(j) %> </td>
	<%  } %>
	</tr>
<%  }%>
</table>

<br>generated at <%=formatter.format(new  Date()) %><br>
<%if(!automatic) 
     {%>
     <jsp:include page="footer.html" />
<%   } %>