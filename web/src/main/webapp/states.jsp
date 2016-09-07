<%@ include file ="header.jsp" %>

<jsp:useBean id="result" scope="request" type="java.util.List" />

<h1>States</h1>
<table cellspacing=2><tr>
<%
String[] states = new String[]{"Unknown","B0B0B0","Productive","A8FFA8","Standby","FFF8A0","Engineering","A8FFFF","Scheduled Down","FFC078","Unscheduled Down","FF9898","Non Scheduled","E0E0E0"};
for (int k=0; k<states.length; k+=2) out.println("<td bgcolor='#"+states[k+1]+"'>"+states[k]+"</td>");
out.println("</tr></table><h1>Machines</h1><table cellspacing=2><tr>");
for (int i=0; i<result.size(); i++)
	{
	String[] line = (String[])result.get(i);
	String color = "FFFFFF";
	String link="";
	if( "Tester".equalsIgnoreCase(line[3])){link=line[0];}
	else{ link="<a href=\"durable_details.do?durable_id="+line[0]+ "\">"+line[0]+"</a>";}
	for (int k=0; k<states.length; k+=2) if (states[k].equals(line[1])) color = states[k+1];
	out.println("<td align='center' bgcolor='#"+color+"' title='"+line[2]+"'><img src='images/Machine.gif'><br>"+link +"</td>");
	if (i%8 == 7) out.println("</tr><tr>");
	}
%>
</tr></table>
<br><br>
<%@ include file ="footer.html" %>