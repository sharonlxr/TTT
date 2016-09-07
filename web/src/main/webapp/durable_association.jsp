<%@ include file ="header.jsp" %>
<%@page import="com.freescale.stateStamper.model.entity.*"%>
<%@page import="java.util.*"%>
<jsp:useBean id="result" scope="request" class="java.util.Vector"/>
<h1>Durable Association<%=site%></h1>
<%
	  
    String parent = (String)result.get(0);
	String message = (String)result.get(1);
	String durable_id = (String)result.get(2);
	String parentType = (String)result.get(3);
	List parentList = (List)result.get(4);
	
%>
<a href="durable_main.do">Back to Durable Main Page</a>
<br>
<a href="durable_details.do?durable_id=<%=durable_id%>">Back to Durable Details Page</a>
<form id="searchForm" name="searchForm" method="get" action="durable_association.do">  
<table width=100%>
<tr>
<td width=50%><%=((EquipmentType)session.getAttribute("equipmentType")).name%>  ID<br></td>
<td width=50%>
<%=durable_id%> 
</td>
</tr>
<tr>
<td width=50%>
<%=parentType%>
</td>
<td width=50%>
<input type="hidden" name="durable_id" value="<%=durable_id%>">
<select name="parent">	
	<%
	if (parent==null||parent.equals("")||parent.equals("NULL"))
	{
	%>
	<option  value=""></option>
	<%}else{ %>
	<option  value="<%=parent%>"><%=parent%></option>
	<option  value=""></option>	
	<%
	}
	Iterator iterator = parentList.iterator();	
	String selectedValue = "";
	while (iterator.hasNext()){ 				
		selectedValue = "";	
		String value = (String)iterator.next();
		if (parent.equals(value)) 
			{
			selectedValue = "selected";
			}
	%>			
<option  value="<%=(String)value%>" <%=selectedValue%>><%=(String)value%></option>			
<% }%>
</select>
</td>
</tr>
<tr>
<td width=50%>
</td>
<td width=50%>
<%=message%>
</td>
</tr>
<tr>
<td width=50%>
</td>
<td width=50%>
<input type="submit" name="submit" value="submit">
</td>
</tr>
</table>
</form>

<%@ include file ="footer.html" %>