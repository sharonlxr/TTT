<%@ include file="header.jsp"%>
<%@page import="org.apache.commons.lang.StringUtils"%>
<%@page import="com.freescale.stateStamper.model.entity.EquipmentType" %>
<%@page import="java.util.*"%>
<jsp:useBean id="result" scope="request" type="java.util.List"/>
<form id="equipmentTypeForm" name="equipmentTypeForm" method="get" action="childValidator.do">
Equipment Type&nbsp;&nbsp;	<select name="equipmentType" onChange="document.forms['equipmentTypeForm'].submit();">
<%
	String selected;
	Iterator it =  equipmentTypes.iterator();
	String equipmentTypeSelected = request.getParameter("equipmentType");
	if(equipmentTypeSelected==null)equipmentTypeSelected=String.valueOf(equipmentType.primaryKey);
	while (it.hasNext()){ 
	 EquipmentType item =(EquipmentType)it.next();
	 if (item.primaryKey.equals(Integer.valueOf(equipmentTypeSelected))) 
	 {	 			 	
	 	selected = "selected";
	 }
	 else 
	 {
		selected = "";	 
     }
	%>
<option  value="<%=item.primaryKey%>" <%=selected%>><%=item.name%></option>
<%}%>
</select>
</form>
  <tr>
    <th>DURABLE_ID</th>
    <th>EQUIPMENT_TYPE</th>
    <th>PARENT</th>
    <th>CHILDREN MATCH RESULTS</th>
  </tr>
<%
	if(result!=null){
		for(Object record:result)
			{
			String[] array = (String[])record;
			if(array==null)continue;%>
			<tr>
				<%if(!StringUtils.equals(array[1],"Tester")){%>
			    <td width="15%"><a href="durable_details.do?durable_id=<%=array[0]%>"><%=array[0]%></td>
			    <%} else {%>
			     <td width="15%"><%=array[0]%></td>
			    <%}%> 
			    <td width="15%"><%=array[1]%></td>
			    <td width="25%"><%=array[2]%></td>
			    <td width="45%"><%=array[3]%></td>
			</tr>
			<%}
	}%>

<%@ include file ="footer.html" %>