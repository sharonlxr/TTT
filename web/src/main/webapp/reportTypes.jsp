<%@ page import="java.util.*" %>
<jsp:useBean id="result" scope="request" class="java.util.Vector" />

<%
Vector reports = result;
String prev = "";
int count = 1;
out.println("<div id='menu' class='menu'>");
for (int i=0; i<reports.size(); i++)
	{
	String[] items = ((String)reports.elementAt(i)).split("\t");
	if (!items[0].equals(prev))
		{
		if (!prev.equals("")) out.println("</div>");
		out.println("<div class='submenu' onMouseOver='showMenu(this,false,\"menu"+count+"\");'><img align='absmiddle' src='images/pad.png'> "+items[0]+" <img align='absmiddle' src='images/arrow.gif'></div>");
		out.println("<div id='menu"+count+"' class='menu'>");
		count++;
		}
	out.println("<div class='submenu' onMouseDown='selectReport(\""+items[0]+"\t"+items[1]+"\");'><img align='absmiddle' src='images/pad.png'> "+items[1]+"&nbsp</div>");
	prev = items[0];
	}
out.println("</div></div>");
%>