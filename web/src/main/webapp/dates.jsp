<%@ page import="java.util.*" %>
<jsp:useBean id="result" scope="request" class="java.util.Hashtable" />

<head>
<title>Dates</title>
<style type="text/css">
body
	{
	background: url(images/background.png);
	margin: 4px;
	}
body, select, input, td
	{
	font-family: Tahoma,"Times New Roman",Times,serif;
	font-size: 11px;
	}
select {width: 100%;}
.tab
	{
	float: left;
	padding: 4px;
	margin: 0px;
	border-style: solid;
	border-color: #7f9db9;
	border-width: 2px;
	}
.content
	{
	position: absolute;
	left: 4px;
	top: 30px;
	border-style: solid;
	border-width: 1px;
	}
</style>
<script>
var period;

function show(id)
	{
	period = id;
	var periods = new Array('Raw','Shift','Day','Week','Month');
	for (i=0; i<periods.length; i++)
		{
		document.getElementById('tab'+periods[i]).style.background=periods[i]==id?'#ffd69a':'url(images/toolbar.png)';
		document.getElementById(periods[i]).style.visibility=periods[i]==id?'':'hidden';
		}
	}
	
function setDates()
	{
	var startDate = '';
	var endDate = '';
	var dates = new Array();
	if (period=='Raw')
		{
		startDate = document.getElementById('startDate').value;
		endDate = document.getElementById('endDate').value;
		var newEndDate = new Date(parseInt(endDate.substring(0,4)),parseInt(endDate.substring(5,2))-1,parseInt(endDate.substring(8,2)));
		newEndDate.setDate(newEndDate.getDate()+1);
		newEndDate = newEndDate.getFullYear()+'-'+(newEndDate.getMonth()+1)+'-'+newEndDate.getDate();
		for (i=0; i<raw.length; i++)
			{
			if (raw[i] != null && raw[i].substring(0,10)>=startDate.substring(0,10) && raw[i].substring(0,10)<=newEndDate.substring(0,10)) dates.push(raw[i]);
			}
		}
	else
		{
    	var select = document.getElementById(period);
    	for (i=0; i<select.options.length; i++)
    		if (select.options[i].selected)
    			dates.push(select.options[i].value);
		}
	eval('opener.period="'+period+'"');			
	eval('opener.dates="'+dates+'"');
	eval('opener.startDate="'+startDate+'"');
	eval('opener.endDate="'+endDate+'"');
	window.close();
	}
</script>
</head>
<body>
<%
String[] PERIODS = {"Raw","Shift","Day","Week","Month"};
for (int i=0; i<PERIODS.length; i++) out.println("<div class='tab' id='tab"+PERIODS[i]+"' onClick=\"show('"+PERIODS[i]+"');\"> "+PERIODS[i]+" </div>");
for (int i=0; i<PERIODS.length; i++)
	{
	if (PERIODS[i].equals("Raw"))
		{
		out.println("<form class='content' style='z-index:10000;' id='"+PERIODS[i]+"'><table border=0><tr><td>Start Date:</td><td><input type='text' name='startDate'></td><td><input type='button' value='Calendar' onClick=''></td></tr><tr><td>End Date:</td><td><input type='text' name='endDate'></td><td><input type='button' value='Calendar' onClick=''></td></tr></table></form>");
	 	Vector dates = (Vector)result.get(PERIODS[i]);
    	out.print("<script>var raw=[");
    	for (int j=0; j<dates.size(); j++) out.print("'"+dates.elementAt(j)+"',");
    	out.println("]</script>");
 		}
	else
		{
    	Vector dates = (Vector)result.get(PERIODS[i]);
    	out.println("<form class='content'><select id='"+PERIODS[i]+"' multiple size='12'>");
    	for (int j=0; j<dates.size(); j++) out.println("<option value='"+dates.elementAt(j)+"'>"+dates.elementAt(j)+"</option>");
    	out.println("</select></form>");
		}
	}
%>
<br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br>
<form style='text-align:right;'>
<input type='button' value='  OK  ' onClick='setDates();'>
<input type='button' value=' Cancel ' onClick='window.close();'>
</form>	
<script>show('Raw');</script>
</body>
