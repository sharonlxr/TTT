<%@ include file ="header.jsp" %>
<h1>Welcome to Tool Time Tracker <%=version%> <%= site %></h1>
<br><img src="images/Splash.png">

<table>
<tr>
<td valign=top>
Reports
<br><br><li><a href="getGroups.do">Real Time Reports</a></li>
<br><br><li><a href="getReports.do?period=Raw">Hour Reports</a></li>
<br><br><li><a href="getReports.do?period=Shift">Shift Reports</a></li>
<br><br><li><a href="getReports.do?period=Day">Daily Reports</a></li>
<br><br><li><a href="getReports.do?period=Week">Weekly Reports</a></li>
<br><br><li><a href="getReports.do?period=Month">Monthly Reports</a></li>
</td>
<td width=100></td>
<td valign=top>
Connections & Summary
<br><br><li><a href="index.jnlp">Java User Interface</a></li>
<!--  <br><br><li><a href="getMachinesHTML.do">Browser User Interface</a></li>-->	
<br><br><li><a href="getConnections.do">Connections</a></li>
<br><br><li><a href="getSummarySchedule.do">Summary Schedule</a></li>
<!--  <br><br><li><a href="getSummaryFiles.do">Summary Files</a></li> -->
<br><br><li><a href="durable_main.do">Durable Management</a></li>
<br><br><li><a href="tester_attached_durable.do" onclick="window.open(this.href, 'mywin','left=20,top=20,width=500,height=250,toolbar=1,resizable=0'); return false;" >Durable By Tester</a></li>
<br><br><li><a href="usersGuide/index.html">Help</a></li>
</td>
</tr>
</table>
<br />
<a href="global.html">TTT Sites</a>
<br />
<%@ include file ="footer.html" %>