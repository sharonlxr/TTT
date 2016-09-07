<%@ include file ="header.jsp" %>
<jsp:useBean id="result" scope="request" type="java.util.List"/>
<form
<select name="newEquipmentType" onChange="formsubmit()">
<% 	 
	String selected = "";
	Iterator it =  equipmentTypes.iterator();
	while (it.hasNext()){ 
	 EquipmentType item =(EquipmentType)it.next();		
	 if (item.primaryKey.equals(equipmentType.primaryKey)) 
	 {	 			 	
	 	selected = "selected";
	 }
	 else 
	 {
		selected = "";	 
     }
%>
<option  value="<%=item.primaryKey%>" <%=selected%>><%=item.name%></option>
<% } %>
</select>
<table>
  <tr>
    <th>DURABLE_ID</th>
    <th>TABLE</th>
    <th>PRIMARY_KEY</th>
    <th style="word-wrap:break-all">MISSING ATTRIBUTE_NAME</th>
  </tr>
<%
	if(result!=null){
	for(Object record:result)
		{
		String[] array = (String[])record;
		if(array==null)continue;
%>
<tr>
    <td width="15%"><%=array[0] %></td>
    <td width="15%"><%=array[1] %></td>
    <td width="15%"><%=array[2] %></td>
    <td width="55%"><%=array[3] %></td>
  </tr>
  <%}
	}%>

<%@ include file ="footer.html" %>