<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<%@ page import="com.freescale.stateStamper.model.entity.Machine" %>
<%@ page import="java.util.*" %>
<jsp:useBean id="result" scope="request" class="java.util.Hashtable" />

<head>
<style type="text/css">
body
	{
	margin:0px;
	font-family: Tahoma,"Times New Roman",Times,serif;
	font-size: 11px;
	}
a:link, a:visited {padding: 2px; text-decoration: none; color: black;}
a:hover {padding: 2px; text-decoration: none; background: #ffd69a;}
.menu
	{
	background: #ffffff;
	position:absolute;
	visibility:hidden;
	z-index:10000;
	border-style: solid;
	border-color: #7f9db9;
	border-width: 1px;
	}
.submenu {padding:0px;}
.submenu:hover {background: #ffd69a;}
</style>
<script>
var reportType = null;
var period = null;
var dates = null;
var startDate, endDate;
var subMenu = null;

function load(url,target)
	{
  	if (window.XMLHttpRequest) req = new XMLHttpRequest();
	else if (window.ActiveXObject) req = new ActiveXObject("Microsoft.XMLHTTP");
  	if (req != undefined)
		{
	    req.onreadystatechange = function() {requestDone(url,target);};
    	req.open("GET",url,true);
	    req.send("");
  		}
	return false;
	}	  

function requestDone(url,target)
	{
	if (req.readyState == 4) // only if req is "loaded"
		{
	    if (req.status == 200) document.getElementById(target).innerHTML = req.responseText;
    	else document.getElementById(target).innerHTML = "Error: "+req.status+" "+req.statusText;
		}
    }

function hideMenusEvent() {hideMenus(); return false;}

function hideMenus() {hideMenu('menu'); hideMenu(subMenu);}

function hideMenu(id)
	{
    if (id != null) document.getElementById(id).style.visibility = 'hidden';
  	return true;
	}

function showMenu(obj,isRoot,id)
	{
	var left = obj.offsetLeft;
	var top = obj.offsetTop;
	if (isRoot) top += obj.offsetHeight+4;
	else
		{
		left += obj.offsetWidth;
		if (subMenu != null) hideMenu(subMenu);
		subMenu = id;
		}
	var style = document.getElementById(id).style;
   	style.pixelLeft = left+document.body.scrollLeft;
    style.pixelTop = top+document.body.scrollTop;
	style.visibility = 'visible';
	isMenu = true;
	return false;
	}
	
function showHide(id)
	{
	var elements = document.getElementsByName(id);
	for (i=0; i<elements.length; i++)
		{ 
		if (elements[i].style.display == '') elements[i].style.display = 'none'; else elements[i].style.display = '';
		}
	}
	
function selectReport(id)
	{
	reportType = id;
	hideMenus();
	}
	
document.onmousedown  = hideMenusEvent;
document.oncontextmenu  = hideMenusEvent;

function select(id)
	{
	var status = document.getElementById(id).style.filter;
	if (status == '') status = 'Invert';
	else status = '';
	document.getElementById(id).style.filter=status;
	}
	
function report(isHTML)
	{
	var machines = new Array();
	for (i = 0; i < document.all.length; i++)
    		{
	        id = document.all(i).id;
			if (id != ''  && document.getElementById(id).style.filter == 'Invert') machines.push(id);
    		}
	if (reportType == null) {alert ('No report type selected'); return;}
	if (period == null || dates == null) {alert ('No dates selected'); return;}			
	var url = 'generateReportHTML.do';
	url += '?reportType='+reportType.split('\t')[0];
	url += '&grouping='+reportType.split('\t')[1];
	if (isHTML) url += '&isHTML=true';
	url += '&machines='+machines+'&period='+period+'&dates='+dates+'&startDate='+startDate+'&endDate='+endDate;
	window.open(url,"reportWindow","status=0,toolbar=0,location=0,menubar=0,directories=0,resizable=1,scrollbars=1,width=600,height=400");
	}
</script>
<title>Shop Floor</title>
</head>

<body onload="load('getReportTypesHTML.do','reportMenu');">
	
<div style="position:fixed; top:0; left:0; width:100%; height:100%;"><img src="images/background.png" width="100%" height="100%"></div>
<div style="position:relative; z-index:1; padding:0px;">
<table style="background-image: url(images/toolbar.png); height: 26px; width: 100%; vertical-align:middle;">
	<tr><td>
	<a href="#" onMouseOver="showMenu(this,true,'menu');"><img src='images/Dates.gif' border='0' align='absmiddle'> Report Type </a>
	<a href="#" onclick='window.open("getDatesHTML.do","dateWindow","status=0,toolbar=0,location=0,menubar=0,directories=0,resizable=0,scrollbars=0,width=200,height=260");'><img src='images/Dates.gif' border='0' align='absmiddle'> Dates </a>
	<a href="#" onclick='report(false);'><img src='images/ExcelReport.gif' border='0' align='absmiddle'> Excel Report </a>
	<a href="#" onclick='report(true);'><img src='images/HTMLReport.gif' border='0' align='absmiddle'> HTML Report </a>
	</td></tr>
</table>

<%	
String[] states = new String[]{"Unknown","B0B0B0","Productive","A8FFA8","Standby","FFF8A0","Engineering","A8FFFF","Scheduled Down","FFC078","Unscheduled Down","FF9898","Non Scheduled","E0E0E0"};
Hashtable types = new Hashtable();
Enumeration keys = result.keys();
while (keys.hasMoreElements())
	{
	Machine machine = (Machine)result.get(keys.nextElement());
	Vector aux = (Vector)types.get(machine.type);
	if (aux == null) {aux = new Vector(); types.put(machine.type,aux);}
	aux.addElement(machine);
	}
keys = types.keys();
out.println("<table>");
while (keys.hasMoreElements())
	{
	String type = (String)keys.nextElement();
	Vector machines = (Vector)types.get(type);
	out.println("<tr><td><img src='images/OpenFolder.gif' align='absmiddle' onClick='showHide(\"group"+type+"\")'>"+type+"</td></tr><tr id='group"+type+"'>");
	for (int i=0; i<machines.size(); i++)
		{
		Machine machine = (Machine)machines.elementAt(i);
		String color = "FFFFFF";
		for (int k=0; k<states.length; k+=2) if (states[k].equals(machine.state)) color = states[k+1];
		out.println("<td align='center' bgcolor="+color+"><img src='images/Machine.gif' id ='"+machine.mESName+"' onclick='select(this.id);'><br>"+machine.mESName+"</td>");
		if (i%8 == 7) out.println("</tr><tr id='group"+type+"'>");
		}
	out.println("</tr>");
	}
out.println("</table>States<table><tr>");	
for (int k=0; k<states.length; k+=2) out.println("<td bgcolor="+states[k+1]+">"+states[k]+"</td>");
out.println("</tr></table>");
%>

<div id="reportMenu"></div>

</div>
</body>