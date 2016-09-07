<%@ include file ="header.jsp" %>

<jsp:useBean id="result" scope="request" type="java.util.List" />

<h1>Summary Schedule</h1>
<table>
<tr><th>Period</th><th>Date</th><th>Start time</th><th>End time</th></tr>
<%
for (int i=0; i<result.size(); i++)
	{
	String[] line = (String[])result.get(i);
	out.println("<tr><td>"+line[0]+"</td><td>"+line[1]+"</td><td>"+line[2]+"</td><td>"+line[3]+"</td></tr>");
	}
%>
</table>

<%@ include file ="footer.html" %>