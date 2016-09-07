<%@ include file ="header.jsp" %>

<jsp:useBean id="result" scope="request" class="java.util.Vector" />

<h1>Connections</h1>
<table>
<tr><th>Host</th><th>Duration</th><th>Messages</th></tr>
<%
for (int i=0; i<result.size(); i++)
	{
	String[] line = (String[])result.elementAt(i);
	if (i<result.size()-1) out.println("<tr><td>"+line[0]+"</td><td>"+line[1]+"</td><td>"+line[2]+"</td></tr>");
	else out.println("<tr><th>"+line[0]+"</th><th>"+line[1]+"</th><th>"+line[2]+"</th></tr>");
	}
%>
</table>

<%@ include file ="footer.html" %>