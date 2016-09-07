<%@ include file ="header.jsp" %>
<%@page import="com.freescale.stateStamper.model.entity.*"%>
<%@page import="com.freescale.stateStamper.model.logic.FiscalDate"%>
<%@page import="java.util.*"%>
<jsp:useBean id="result" scope="request" class="java.util.Vector"/>
<%
Machine durable = (Machine) (result.get(0));
Vector attributes = (Vector) (result.get(1));
DurableStatus status = (DurableStatus)durable.context.get("durableStatus");
EquipmentType type = (EquipmentType)request.getSession().getAttribute("equipmentType");
%>

<br>
<a href="durable_details.do?durable_id=<%=status.DURABLE_ID%>">Back to Durable Details Page</a>
<br>
<h1>Attachment Management</h1> 
Please add a new Comment
<br>
<form id="addattachmentForm" name="addattachmentForm" method="post" action="durable_add_attachment.do" enctype="multipart/form-data">  
select an attribute
<select  name="attribute"  size="1">
    <% 	
	 for (int j= 0 ; j<attributes.size();j++)
		 {
		 String selected="";
		 EquipmentAttribute tmp = (EquipmentAttribute)attributes.get(j);
		 if (j==0) selected="selected=\"selected\"";				
		 %>
	    <option  value="<%=tmp.getName()%>" <%=selected%>><%=tmp.getName()%></option>
	<%   }%>   
</select>  
<br>
<input type="file" name="file" size="50" />
<input type="hidden" id="durableID" name="durableID" value="<%=status.DURABLE_ID%>"> 	
<input type="submit" name="submit" value="submit" >
</form>

<% 
	if (durable.hasAttachment()){
	
%>
<h1>List of attachments</h1>

<% Vector attachmenttypes = type.getAttributesByType(EquipmentAttribute.ATTACHMENT); 
	
   for (int i = 0 ; i<attachmenttypes.size(); i++){	
		String attachtmpName =  ((EquipmentAttribute)attachmenttypes.get(i)).getName();
%>
	
<h2><%=attachtmpName%></h2>

<table width=100%>
<tr>
<td width=16%>
FileName
</td>
<td width=16%>
Time
</td>
<td width=16%>
state
</td>
<td width=16%>
substate
</td>
<td width=16%>
Operator
</td>
<td width=10%>
</td>
</tr>


<% 		

   List attachments =  durable.getAttachmentsbyName(attachtmpName);
   for (int j=0; j<attachments.size(); j++) {
	   Attachment attach = (Attachment)attachments.get(j);
	   DurableStatus tmpstatus = attach.getStatus();	 
	   String attachmentID = attachtmpName + "_attachment_hide";
	   if (j==0) {
		   attachmentID = attachtmpName + "_attachment_show";
	   }
%>	   
   
   <tr class="<%=attachmentID%>">
	<td width=16%>
	<a href='durable_getAttachment.do?file=<%=attach.getFileLocation()%>' >	<%=attach.getDisplayFilename()%></a>	
	</td>
	<td width=16%>
		<%=attach.getTime()%>
	</td>
	<td width=16%>
		<%=tmpstatus.STATE%>
	</td>
	<td width=16%>
		<%=tmpstatus.SUBSTATE%>
	</td>
	<td width=16%>
		<%=tmpstatus.get(type,"OPERATOR_ID")%>
	</td>
	<td width=10%>
		<a href='durable_delete_attachment.do?durable_id=<%=status.DURABLE_ID%>&attachment_pk=<%=attach.getAttachmentPk()%>' >delete</a>	
	</td>	
 </tr>
   
<%
   }
 %>
</table>
 <%  
  	}
	 }
%>
<%@ include file ="footer.html" %>
