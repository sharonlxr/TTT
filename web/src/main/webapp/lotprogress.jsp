<%@ include file ="header.jsp" %>
<jsp:useBean id="result" scope="request" class="java.util.Vector" />
<h1>Lot Progress (Percentage >  <%=((Float)result.get(0))*100 + "%" %> )</h1>
<table><tr><th>Machine</th><th>Location</th><th>Type</th><th></th></tr>
<%
for (int i=1; i<result.size(); i++)
	{
	String[] line = (String[])result.elementAt(i);
	out.println("<tr><td>"+line[0]+"</td><td>"+line[1]+"</td><td>"+line[2]+"<td><img src=images/bar.png height=16 width="+((int)Float.parseFloat(line[3])*3)+"> "+line[3]+"%</td></tr>");
	}
%>
</table>

<%@ include file ="footer.html" %>