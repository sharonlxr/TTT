<%@ include file ="header.jsp" %>

<%@ page import="java.util.Vector" %>
<jsp:useBean id="result" scope="request" class="java.util.Vector" />

<h1>Contexts & Summings</h1>
<table>
<%
for (int i=0; i<result.size(); i++)
	{
	out.println("<tr>");
	Vector row = (Vector)result.elementAt(i);
	for (int j=0; j<row.size(); j++)
		{
		out.print((j==0)?"<th align=left>":"<td>");
		out.print(row.elementAt(j));
		out.print((j==0)?"</th>":"</td>");
		}
	out.println("</tr>");
	}
%>
</table>

<%@ include file ="footer.html" %>