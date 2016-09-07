<%@ include file ="header.jsp" %>
<%@page import="com.freescale.stateStamper.model.entity.*"%>
<%@page import="com.freescale.stateStamper.model.logic.FiscalDate"%>
<%@page import="java.util.*"%>
<jsp:useBean id="result" scope="request" class="java.util.Vector"/>
<%
	CurrentDurableStatus status = (CurrentDurableStatus) (result.get(0));
%>

<br>
<a href="durable_details.do?durable_id=<%=status.DURABLE_ID%>">Back to Durable Details Page</a>
<br>
<table width=100% border="1">
<tr>
<td width=25%>
<b>State</b>
</td>
<td width=25%>
<%=status.STATE%>
</td>
<td width=25%>
<b>SubState</b>
</td>
<td width=25%>
<%=status.SUBSTATE%>
</td>
</tr>
</table>

<%@ include file ="durable_comments.jsp" %>
Please add a new Comment
<br>
<form id="addcommentForm" name="addcommentForm" method="post" action="durable_addComment.do">  
<textarea rows="20" cols="50" name="comments"></textarea>
<input type="hidden" id="status_pk" name="status_pk" value="<%=status.STATUS_PK%>"> 
<input type="hidden" id="durable_id" name="durable_id" value="<%=status.DURABLE_ID%>"> 	
<input type="submit" name="submit" value="submit" >
</form>
<%@ include file ="footer.html" %>
