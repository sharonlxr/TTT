<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<jsp:useBean id="user" scope="session" class="com.freescale.stateStamper.model.entity.Administrator" />
<jsp:useBean id="site" scope="application" class="java.lang.String" />
<jsp:useBean id="equipmentTypes" scope="session" class="java.util.ArrayList" />
<jsp:useBean id="areas" scope="session" class="java.util.ArrayList" />
<jsp:useBean id="equipmentType" scope="session" class="com.freescale.stateStamper.model.entity.EquipmentType" />
<jsp:useBean id="area" scope="session" class="com.freescale.stateStamper.model.entity.Area" />
<%@page import="com.freescale.stateStamper.model.entity.Role"%>
<%@page import="com.freescale.stateStamper.model.entity.EquipmentType"%>
<%@page import="com.freescale.stateStamper.model.entity.Area"%>
<%@page import="java.util.*"%>
<%! String version = "4.6"; %> 
<html>
<head>
<title>Tool Time Tracker <%=site%></title>
<link href="http://swo.freescale.net/css/fsl.css" rel="stylesheet"	type="text/css" />
<script type="text/javascript" src="http://swo.freescale.net/jscripts/FSLHeader.js" ></script>
<script type="text/javascript" src="javascripts/transition.js" 		charset="UTF-8" ></script>
<script type="text/javascript" src="javascripts/datetimepicker.js" 	charset="UTF-8" ></script>
<script type="text/javascript" src="javascripts/overlib.js"  		charset="UTF-8" ></script>
<script type="text/javascript" src="javascripts/jquery-1.9.0.min.js"charset="UTF-8" ></script>
<link rel="stylesheet" type="text/css" href="css/tttmain.css"/>
<link rel="icon" href="images/ttt.ico"  type="image/x-icon"/>
</head>
<body>
	<div id="overDiv"
		style="position: absolute; visibility: hidden; z-index: 1000;"></div>
	<script language="JavaScript">
    	FSTitle = 'Tool Time Tracker  <%=version%> <%=site%>';
		HideDropDowns = true;
		HideSearch = true;
		HideStockQuote = true;
		ShowStockB = false;
		FSHeader_localSearch = false;
		WriteMotHeader();
		function formsubmit() {
			var area = $("#newArea").val();
			var equipmenttype = $("#newEquipmentType").val();
			var equipmentArea = $("#newEquipmentType option[value='"+equipmenttype+"']").attr("area");
			if(equipmentArea!=1&&equipmentArea!=area&&area!=1)
				{
				$("#newEquipmentType").val('<%=equipmentType.primaryKey%>');
				$("#newArea").val('<%=area.getPrimaryKey()%>');
				alert("The equipment and area doesn't match");
				return;
				}
			document.forms["typeswitchForm"].submit();
		}
		
		
	</script>
	<div align="right" class="fsl_banner_applinks"
		style="position: relative; top: 20px; left: 0px">
		<% 
	if(user!=null && user.coreId != null && user.coreId !="")
		{
		Role role = user.getRole();
		out.print(user.coreId+" - ");
		if(role!=null)out.print(role.rolename+" - ");
		out.print("<a href='durable_logoff.do'>signoff&nbsp;&nbsp;</a>");
		}
	else
		{
	   out.print("Guest - <a href='durable_main.do'>sign in&nbsp;&nbsp;</a>");
		}
%>
	</div>
	
	<div align="right" style="position: relative; top: 20px; left: 0px">
		<form name="typeswitchForm" method="get" action="durable_switchtype.do">
	<% 
  	 if (equipmentTypes!=null&&!equipmentTypes.isEmpty())
    	{  
	%>
			EquipmentType 
			<select id="newEquipmentType" name="newEquipmentType">
		<% 	
	    	 for(Object temp:equipmentTypes)
		   		 {
	    		 String selected = "";
		    	 EquipmentType item = (EquipmentType)temp;
				 if (item.primaryKey.equals(equipmentType.primaryKey)) 
				 	{	 			 	
				 	selected = "selected";
					}
					else 
					{
					selected = "";	 
				    }
%>
				<option value="<%=item.primaryKey%>" area="<%=item.getAreaPK()%>" <%=selected%>><%=item.name%></option>
				<% } %>
			</select>
			<% } %>
			<% 
   if (areas!=null&&!areas.isEmpty())
    	{   
%>
		Areas <select id="newArea" name="newArea" >
		<% 	 
		for (Object temp:areas)
			{ 
			Area item = (Area)temp;
			String selected = "";
			 if (item.getPrimaryKey().equals(area.getPrimaryKey())) 
			 {	 			 	
			 	selected = "selected";
			 }
			 else 
			 {
				selected = "";	 
		     }
%>
			<option value="<%=item.getPrimaryKey()%>" <%=selected%>><%=item.getName()%></option>
			<% } %>
			</select>
			<%} %>
			<% if (equipmentTypes!=null&&!equipmentTypes.isEmpty()&&areas!=null&&!areas.isEmpty()) {%>
			<input type="button" name="switch" value="switch" onclick="formsubmit()" >
			<%} %>
		</form>
	</div>


	<style type="text/css">
<!--
td {
	white-space: nowrap;
}
-->
</style>
	<table width="100%" cellspacing="0" cellpadding="0">
		<tr>
			<td width="160"></td>
			<td>