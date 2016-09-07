<%@ include file ="header.jsp" %>
<jsp:useBean id="result" scope="request" class="java.util.Vector" />

<h1>UOEE (current shift)</h1>
<table><tr><th>Machine</th><th></th></tr>
<%
for (int i=0; i<result.size(); i++)
	{
	String[] line = (String[])result.elementAt(i);
	out.println("<tr><td>"+line[0]+"</td><td><img src=images/bar.png height=16 width="+((int)Float.parseFloat(line[1])*3)+"> "+line[1]+"%</td></tr>");
	}
%>
</table>

<%@ include file ="footer.html" %>