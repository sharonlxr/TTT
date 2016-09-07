<%@ include file ="header.jsp" %>
<%@page import="com.freescale.stateStamper.model.entity.*"%>
<%@page import="java.util.*"%>
<jsp:useBean id="result" scope="request" type="java.util.Map"/>
<h1>Durable Attach Child <%=site%></h1>
<%
    Machine durable = (Machine)result.get("durable");
    Map<String,List> attachResult = (Map)result.get("attach_result");
    List<String> attached = attachResult.get("attached");
    List<String> detached = attachResult.get("detached");
    List<String> tried    = attachResult.get("tried");
%>
<script >

</script>
<a href="durable_main.do">Back to Durable Main Page</a>
<br>
<a href="durable_details.do?durable_id=<%=durable.mESName%>">Back to <%=durable.mESName%>  Details Page</a><br/>
<h1><%=durable.mESName%></h1>
<b>Attached</b> : the following items were attached
<%
if(attached != null)
	{
	for(String childId:attached)
		{
%>
<li><a href="durable_details.do?durable_id=<%=childId%>"><%=childId%></a>
	
<%      }
	}%>
	
<%
if(detached != null && detached.size() > 0)
	{
	%>
<hr>
<br/><b>Detached</b> : the following items were detached
	<% for(String childId:detached)
		{
%>
<li><a href="durable_details.do?durable_id=<%=childId%>"><%=childId%></a>
<%      }
	}%>

<%
if(tried != null && tried.size() > 0)
	{
	%>
<hr>
<br/><b>Errors</b> : there were errors when trying to attach the following items
	<% for(String childId:tried)
		{
%>
<li><a href="durable_details.do?durable_id=<%=childId%>"><%=childId%></a>
<%      }
	}%>

<br/><br/>	
<%@ include file ="footer.html" %>