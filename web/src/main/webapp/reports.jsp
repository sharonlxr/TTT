<%@ include file ="header.jsp" %>

<%@ page import="java.util.Vector" %>
<%@ page import="java.net.URLEncoder"%>
<jsp:useBean id="result" scope="request" class="java.util.Vector" />

<%
String period = request.getParameter("period");
out.print("<h1>");
if (period.equals("Raw")) out.print("Hour");
else if (period.equals("Shift")) out.print("Shift"); 
else out.print(period+"ly");
out.println(" Reports</h1>");
for (int i=0; i<result.size(); i++)
	{
	Vector files = (Vector)result.elementAt(i);
	String date = (String)files.firstElement();
	out.println(date+"<ul>");
	for (int j=1; j<files.size(); j++) out.println("<li><a href='getReport.do?file="+period+"/"+date+"/"+URLEncoder.encode((String)files.elementAt(j),"UTF-8")+"' target='_blank'>"+files.elementAt(j)+"</a>");
	out.println("</ul>");
	}
%>

<%@ include file ="footer.html" %>