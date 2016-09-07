<%@ include file ="header.jsp" %>

<%@ page import="java.util.Vector" %>
<jsp:useBean id="result" scope="request" type="java.util.List" />
<%
String eqType = request.getParameter("equipmentType");
String eqTypeParam = "";
if(eqType != null && !eqType.equals(""))
	{
	eqTypeParam += "&equipmentType="+eqType;
	}
boolean isTester  = eqType == null || eqType.equalsIgnoreCase("Tester");
%>

<h1>Real Time Reports</h1>
<table><tr><td valign=top><table><tr><th>State by Type</th></tr>
<%
List types = (List)result.get(0), locations = (List)result.get(1);
for (int i=0; i<types.size(); i++) out.println("<tr><td><a href='getStates.do?type="+types.get(i)+eqTypeParam+"'>"+types.get(i)+"</a></td></tr>");
out.println("</table></td><td valign=top><table><tr><th>State by Location</th></tr>");
for (int i=0; i<locations.size(); i++) out.println("<tr><td><a href='getStates.do?location="+locations.get(i)+eqTypeParam+"'>"+locations.get(i)+"</a></td></tr>");
if(isTester)
	{
	out.println("</table></td><td valign=top><table><tr><th>UOEE by Type</th></tr>");
	for (int i=0; i<types.size(); i++) out.println("<tr><td><a href='getUOEE.do?type="+types.get(i)+"'>"+types.get(i)+"</a></td></tr>");
	out.println("</table></td><td valign=top><table><tr><th>UOEE by Location</th></tr>");
	for (int i=0; i<locations.size(); i++) out.println("<tr><td><a href='getUOEE.do?location="+locations.get(i)+"'>"+locations.get(i)+"</a></td></tr>");
	out.println("</table></td><td valign=top><table><tr><th>Data by Type</th></tr>");
	for (int i=0; i<types.size(); i++) out.println("<tr><td><a href='getContextsSummings.do?type="+types.get(i)+"'>"+types.get(i)+"</a></td></tr>");
	out.println("</table></td><td valign=top><table><tr><th>Data by Location</th></tr>");
	for (int i=0; i<locations.size(); i++) out.println("<tr><td><a href='getContextsSummings.do?location="+locations.get(i)+"'>"+locations.get(i)+"</a></td></tr>");
	out.println("</table></td><td valign=top><table><tr><th>Lot Progress</th></tr>");
	out.println("<tr><td><a href='getLotProgress.do?lotprogress=0.7'>Lot Progress &#62 70%</a></td></tr>");
	}
%>
</table></td></tr></table>

<%@ include file ="footer.html" %>