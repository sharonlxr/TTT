<%@ include file ="header.jsp" %>
<jsp:useBean id="result" scope="request"  type="java.util.Map"/>
<table>
<tr>
<td valign=top>
<%
 String msg;
 String img;
 String durableId = result.get("durableId").toString(); 
 if(result.containsKey("ERROR"))
	 {
	 msg = result.get("ERROR").toString();
	 img="error_icon.png";
	 }
 else 
	 {
	 msg ="Transtition is successful";
	 img="success_icon.png";
	 }
%>
<img src="images/<%=img%>">
<h3><%=msg%></h3>
</td>
</tr>
<tr>
<td>
<h3><a href="durable_transaction_search.do?reportType=Transition&grouping=<%=((EquipmentType)session.getAttribute("equipmentType")).name%>">return to transition page</a></h3>
<h3><a href="durable_main.do">return to main page</a></h3>
</td>
</tr>
<tr>
<td>
<h3> <a href="durable_details.do?durable_id=<%=durableId%>">Display <%=durableId%> Details</a></h3>
</td>
</tr>
</table>

<%@ include file ="footer.html" %>