<%@ page import="java.util.Date" %>
<%@ page import="java.text.SimpleDateFormat" %>
<jsp:useBean id="result" scope="request" class="java.util.Vector" />

<html>
<script>
	setTimeout("window.location.reload()",60*1000);  // Reload page every minute
</script>
<style type="text/css">
	body {font-family:Tahoma,Arial,Helvetica,sans-serif;}
	h1 {font-size:14px;}
	div {font-size:12px;}
	table {font-size:12px; border-collapse:collapse; border-width:0px;}
	table td {padding:0;}		
</style>
<center><h1>UOEE (current shift) <% out.println(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())); %></h1>
<table><tr>
<%
for (int i=0; i<result.size(); i++)
	{
	String[] line = (String[])result.elementAt(i);
	out.println("<td align=center valign=bottom>"+line[1]+"%<br><img src=images/bar.png width=32 height="+((int)Float.parseFloat(line[1])*3)+"></td>");
	}
out.println("</tr><tr>");
for (int i=0; i<result.size(); i++)
	{
	String[] line = (String[])result.elementAt(i);
	out.println("<td>"+line[0]+"</td>");
	}
%>
</tr></table>
</center>
</html>