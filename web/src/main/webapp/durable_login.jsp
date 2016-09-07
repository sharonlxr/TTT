<%@ include file ="header.jsp" %>
<%@page import="com.freescale.stateStamper.model.entity.*"%>
<%@page import="java.util.*"%>
<jsp:useBean id="result" scope="request" type="java.util.Map"/>
<h1>Login </h1>

	<% 
	List equipmentTypesList = (List)result.get("equipmentTypes");
	String originPage = (String)result.get("originPage");
	List areaList = (List)result.get("areas");
	Cookie[] cookies = request.getCookies();
	String lastEquipmentType=null;
	for(int i = 0; cookies != null && i < cookies.length;i++)
		{
		if(cookies[i].getName().equals("lastEquipmentType"))
			{
			lastEquipmentType = cookies[i].getValue();
			}
		}
	String lastArea=null;
	for(int i = 0; cookies != null && i < cookies.length;i++)
		{
		if(cookies[i].getName().equals("lastArea"))
			{
			lastArea = cookies[i].getValue();
			}
		}
	%>
<script type="text/javascript">
var equipmentTypesArray = [];
var areasArray = [];
var lastEquipmentType=null;
var lastArea=null;
var allArea = '<%=Area.AREA_ALL_NAME%>'
$(document).ready(function(){
	initData();
	loadAreas(areasArray);
	loadEquipments(equipmentTypesArray,lastArea);
});

function initData()
	{
	lastEquipmentType = '<%=lastEquipmentType%>';
	lastArea = '<%=lastArea%>';
	<% 
	for(Object tmp:equipmentTypesList){
		EquipmentType obj = (EquipmentType)tmp;
	%>
	var equipment = {};
	equipment.primaryKey='<%=obj.primaryKey%>';
	equipment.name='<%=obj.name%>';
	equipment.areaPK='<%=obj.getAreaPK()%>';
	equipment.areaName='<%=obj.getArea().getName()%>';
	equipment.selected='<%=obj.name.equals(lastEquipmentType)?"selected":"" %>';
	equipmentTypesArray.push(equipment);
	<%
	}
	%>
	<% 
	for(Object tmp:areaList)
		{
		Area obj = (Area)tmp;
		%>
		var area = {};
		area.primaryKey='<%=obj.getPrimaryKey()%>';
		area.name='<%=obj.getName()%>';
		area.selected='<%=obj.getName().equals(lastArea)?"selected":"" %>';
		areasArray.push(area);
		<%
		}
		%>
	}

function loadEquipments(equipmenttypes,selectedArea)
	{
	 var selObj = $("#equipmentType");
	 for(var index in equipmenttypes)
	 	{
		 var item = equipmenttypes[index];
		 if(selectedArea!=null&&selectedArea!= allArea &&selectedArea!="null")
		 	{
			 if(selectedArea==item.areaName||item.areaName == allArea)
			 	{
			 	selObj.append("<option value='"+item.primaryKey+"' "+item.selected+">"+item.name+"</option>");
				}
		 	} 
		 else 
		 	{
			 selObj.append("<option value='"+item.primaryKey+"' "+item.selected+">"+item.name+"</option>");
		 	}
	 	}
	}

function loadAreas(areas)
	{
	 var selObj = $("#area");
	 for(var index in areas)
	 	{
		 var item = areas[index];
		 selObj.append("<option value='"+item.primaryKey+"' "+item.selected+">"+item.name+"</option>");
		}
	}

function switchArea()
	{
	var selectedArea = $("#area").find('option:selected').text();
	if(selectedArea!="")
		{
		$("#equipmentType").empty();
		loadEquipments(equipmentTypesArray,selectedArea);
		}
	}
</script>
<a href="index.jsp">Back to TTT main page</a>
<form id="loginForm" name="loginForm" method="post" action="durable_login.do">  
<table width=100%>
<tr>
<td width=50%>Core ID<br></td>
<td width=50%>
<input name="sUserId" type="text" id="sUserId" class="input">
</td>
<tr>
<td width=50%>Password&nbsp;  
</td>
<td width=50%>
<input name="sPassword" type="password" id="sPassword" class="input">
<%if(originPage != null && !originPage.equals(""))
	{%>
	<input type="hidden" name="originPage" value="<%=originPage%>">
  <%}%>
</td>
</tr>

<tr>
<td width=50%>Area&nbsp;  
</td>
<td width=50%>
<select id="area" name="area" onchange="switchArea()">
</select>
</td>
</tr>

<tr>
<td width=50%>EquipmentType&nbsp;  
</td>
<td width=50%>
<select id="equipmentType" name="equipmentType">
</select>
</td>
</tr>

<tr>
<td width=50%>
</td>
<td width=50%>
<input type="submit" name="login" value="login">
</td>
</tr>
</table>
</form>

<%@ include file ="footer.html" %>