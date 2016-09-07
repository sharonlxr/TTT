<%@ include file ="header.jsp" %>
<%@page import="com.freescale.stateStamper.model.entity.*"%>
<%@page import="java.util.*"%>
<jsp:useBean id="result" scope="request" class="java.util.Vector"/>
<h1>select the equipment </h1>
<a href="durable_main.do">Back to Durable Main page</a>
<form id="searchForm" name="searchForm" method="get" action="searchDurable.do">  

<table width=100%>
<tr>
<td width=75%>
<input type="text" value="" name="durable_id"> 
</td>
<td width=15%>
<select name="action">
<option  value="checkin">Check in</option>
<option  value="checkout">Check out</option>
<option  value="">Repair</option>
</select>

</td>
<td width=10%>
<input type="submit" name="search" value="search">
</td>

</tr>
</table>

</form>


<table width=100%>
<tr>
<td>
Equipment_ID
</td>
</tr>
<%
 if (!result.isEmpty()){
 	String actiontype = (String)result.get(0);
	if (result.get(1)!= null){ 
	Iterator iterator = ((Vector)(result.get(1))).iterator();
 	while (iterator.hasNext()){
		CurrentDurableStatus status = (CurrentDurableStatus)iterator.next();  
%>
<tr>
<td>
<a href="getDurableStatus.do?action=<%=actiontype%>"><%=status.eIName%></a>
</td>
</tr>
<%
 }
 	}
 }
%>
</table>
<%@ include file ="footer.html" %>