<!DOCTYPE html>
<%@ taglib uri="/struts-tags" prefix="s"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@page pageEncoding="UTF-8" contentType="text/html; charset=UTF-8"%>
<%! String version = "4.6"; %> 
<html>
<head>
<meta http-equiv="X-UA-Compatible" content="IE=edge" />
<title>Tool Time Tracker</title>
<link href="http://swo.freescale.net/css/fsl.css" rel="stylesheet" type="text/css" />
<script type="text/javascript" src="http://swo.freescale.net/jscripts/FSLHeader.js"></script>
<script type="text/javascript" src="<%=request.getContextPath()%>/javascripts/jquery-1.9.0.min.js" charset="UTF-8"></script>
<script type="text/javascript" src="<%=request.getContextPath()%>/javascripts/model.js" charset="UTF-8"></script>
<link rel="stylesheet" type="text/css" href="<%=request.getContextPath()%>/css/tttmain.css" />
<link rel="icon" href="<%=request.getContextPath()%>/images/ttt.ico" type="image/x-icon" />
<script language="JavaScript">
	FSTitle = 'Tool Time Tracker <%=version%> <s:property value="#application.site" />';
	HideDropDowns = true;
	HideSearch = true;
	HideStockQuote = true;
	ShowStockB = false;
	FSHeader_localSearch = false;
	WriteMotHeader();
	function swithEtArea() {
		var area = $("#newArea").val();
		var equipmenttype = $("#newEquipmentType").val();
		var equipmentArea = $("#newEquipmentType option[value='" + equipmenttype + "']").attr("area");
		if (equipmentArea != 1 && equipmentArea != area) {
			$("#newEquipmentType").val('<s:property value="#session.equipmentType.primaryKey" />');
			$("#newArea").val('<s:property value="#session.area.primaryKey" />');
			alert("The equipment and area doesn't match");
			return;
		}
		document.forms["typeswitchForm"].submit();
	}
</script>
<style type="text/css">
	#modelFieldsTable tr:nth-child(odd){background:#eeeeee;}
	#modelAttributesTable tr:nth-child(odd){background:#eeeeee;}
	#modelMappingTable tr:nth-child(odd){background:#eeeeee;}
	#dynamicAttributesTable tr:nth-child(odd){background:#eeeeee;}
	#childDurable tr:nth-child(odd){background:#eeeeee;}
</style>
</head>
<body>
<div id="overDiv" style="position: absolute; visibility: hidden; z-index: 1000;"></div>
<div align="right" class="fsl_banner_applinks"
	style="position: relative; top: 20px; left: 0px">
	<s:if test="#session.user!=null">
		<s:property value="#session.user.coreId" />-<s:property value="#session.user.role.rolename" />
		<a href='durable_logoff.do'>signoff&nbsp;&nbsp;</a>
	</s:if>
	<s:else>
			Guest - <a href='durable_main.do'>sign in&nbsp;&nbsp;</a>
	</s:else>
</div>

<div align="right" style="position: relative;top: 20px; left: 0px;z-index: 1000;">
	<form name="typeswitchForm" method="get" action="durable_switchtype.do">
		<s:set name="currentEquipmentType" value="#session.equipmentType.primaryKey" />
		EquipmentType <select id="newEquipmentType" name="newEquipmentType">
			<s:iterator value="#session.equipmentTypes" var="et">
				<s:if test="%{#et.primaryKey==#currentEquipmentType}">
					<option value="<s:property value="#et.primaryKey" />"
						area="<s:property value="#et.area.primaryKey" />" selected><s:property
							value="#et.name" /></option>
				</s:if>
				<s:else>
					<option value="<s:property value="#et.primaryKey" />"
						area="<s:property value="#et.area.primaryKey" />"><s:property
							value="#et.name" /></option>
				</s:else>
			</s:iterator>
		</select> Areas
		<s:select id="newArea" name="newArea" list="#session.areas"
			listKey="primaryKey" listValue="name"
			value="#session.area.primaryKey">
		</s:select>
		<input type="button" name="switch" value="switch"
			onclick="swithEtArea()">
	</form>
</div>