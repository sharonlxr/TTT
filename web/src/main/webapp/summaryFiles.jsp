<%@ include file ="header.jsp" %>

<%@ page import="java.util.List" %>
<jsp:useBean id="result" scope="request" type="java.util.List" />

<h1>Summary Files</h1>
<table><tr>
<%
String[] labels = new String[] {"Raw","Shift","Day","Week","Month"};
for (int i=0; i<result.size(); i++)
	{
	Vector category = (Vector)result.get(i);
	out.println("<td valign=top ><table><tr><th colspan=2>"+labels[i]+" </th></tr><tr><th>File</th></tr>");
	for (int j=0;j<category.size();  j++)
		{
		String line = (String)category.elementAt(j);
		out.println("<tr><td><a href='getFile.do?file="+labels[i]+"/"+line+".txt' target='_blank'>"+line+".txt</a></td></tr>");
		}
	out.println("</table></td>");
	}
%>
</tr></table>

<%@ include file ="footer.html" %>