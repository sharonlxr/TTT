<%@ include file ="header.jsp" %>
<%@page import="com.freescale.stateStamper.model.entity.*"%>
<%@page import="com.freescale.stateStamper.util.*"%>
<%@page import="com.freescale.stateStamper.model.logic.FiscalDate"%>
<%@page import="java.util.*"%>
<jsp:useBean id="result" scope="request" class="java.util.Vector"/>
		</td>
	</tr>	
</table>
<%
	CurrentDurableStatus status = (CurrentDurableStatus) (result.get(0));
	List result2 = (List) result.get(1);
	EquipmentType type = (EquipmentType) result.get(2);
	EquipmentType parentType = (EquipmentType) result.get(3);
	boolean isCurrent = (Boolean) result.get(4);
	Machine durable = (Machine) result.get(5);
	String parentName = null;
	boolean parentLink = false;
	String workStation = status.workstation != null ? status.workstation : "";
	if ((parentType != null && !"tester".equalsIgnoreCase(parentType.name)) && status.workstation != null && !status.workstation.equals(""))
		{
		workStation = "<a href=\"durable_details.do?durable_id=" + status.workstation + "\">"
		+ status.workstation + "</a>";
		}
	List attachRule = type.getBusinessRulesBy(BusinessRule.ATTACH_CHILD);
%>

<h1><%=type.name%> Details</h1> 
<!--configurable -->
 
<a href="durable_main.do">Back to Durable Main page</a>
<table class="durabledetail">
<tr>
<td class="durabledetail">
<b>State</b>
</td>
<td class="durabledetail">
<%=status.STATE%>
</td>
<td class="durabledetail">
<b>SubState</b>
</td>
<td class="durabledetail">
<%=status.SUBSTATE%>
</td>
</tr>

<tr class="durabledetail">
<td class="durabledetail">
<b><%=type.name%> ID</b>
</td>
<td class="durabledetail">
<%=status.DURABLE_ID%>
</td>
<td class="durabledetail">
<b>Type</b>
</td>
<td class="durabledetail">
<%=status.type%>
</td>
</tr>

<tr>
<td class="durabledetail">
<b>Last Transition Time</b>
</td>
<td class="durabledetail">
<%=status.TIME%>
</td>
<td class="durabledetail">
<b>Conduct By</b>
</td>
<td class="durabledetail">
<%=status.get(type,"OPERATOR_ID")%>
</td>
</tr>



<tr class="durabledetail">
<td class="durabledetail">
<b>Location</b>
</td>
<td class="durabledetail">
<%=status.location%>
</td>
<td class="durabledetail">
<b>Parent\Workstation</b>
</td>
<td class="durabledetail">
<%
	if (parentType != null && parentType.name.equalsIgnoreCase("tester"))
		{
%>
<%=workStation%>
<%
	}
	else if (parentType != null)
		{
%>
<%=workStation%>
<%
	}
%>
</td>
</tr>

<tr>
<td class="durabledetail">
<b>DEVICE GROUP</b>
</td>
<td class="durabledetail">
<%=status.deviceGroup%>
</td>
<td class="durabledetail">
<b>Vendor</b>
</td>
<td class="durabledetail">
<%
	if(status.vendor !=null){
%>
<%=status.vendor%>
<%
	}
%>
</td>
</tr>
</table>
<%!private String getStatusTable(Map attributes, EquipmentType eqType, IStatus iStatus) throws Exception
		{
		StringBuffer tableStr = new StringBuffer();
		tableStr.append("<table class=\"durabledetail\">");
		if (attributes != null)
			{
			List attrNames = new Vector(attributes.keySet());
			Collections.sort(attrNames);
			String name, label;
			EquipmentAttribute tmpAttr;
			boolean even = false;
			boolean evenline = false;
			int linecounter = 0;
			for (int i = 0; i < attrNames.size(); i++)
				{
				even = linecounter % 2 == 0;
				evenline = linecounter % 4 == 0;
				name = (String) attrNames.get(i);
				label = null;
				tmpAttr = eqType.getAttribute(name);
				if(tmpAttr == null){continue;}
				if (tmpAttr.isThresholdAttribute()||tmpAttr.isAttachment())
					{
					continue;//donot display threshold attributes or attachment
					}
				if (tmpAttr != null)
					{
					label = tmpAttr.getAttributeAlias();
					}
				if (label == null || label.equals(""))
					{
					label = name;
					}
				Object objValue = iStatus.get(eqType,name);
				String value = "";
				String extendedValue = "";
				if (objValue != null)
					{
					value = objValue.toString();
					if(value.length() > 40)
						{
						extendedValue = "onmouseover=\"return overlib('"+FiscalDate.escape(value)+"');\" onmouseout=\"return nd();\"";
						value = value.substring(0,40)+"...";
						} 
					}
				if (eqType.isTimeRelatedAttribute(name))
					{
					try
						{
						value = FiscalDate.getElapsedTime(Long.parseLong(value));
						}
					catch (NumberFormatException nbe)
						{
						value = "";
						}
					}
				if (eqType.isAttachmentAttribute(name))
				{				
					continue;									
				}
				if (even)
					{
					if (evenline)
						 {
						 tableStr.append("<tr>");	
						 }
					else {
						tableStr.append("<tr class=\"durabledetail\">");						 
						}
					}
				tableStr.append("<td class=\"durabledetail\"><b>" + label + "</b></td>");
				tableStr.append("<td class=\"durabledetail\" "+ extendedValue +" >" + value + "</td>");
				if (!even)
					{
					tableStr.append("</tr>");
					}
				linecounter++;
				}
			}
		tableStr.append("</table>");
		return tableStr.toString();
		}

	private String getAliasforThreshold(EquipmentType eqType, String name, String baseName)
		{
		EquipmentAttribute tmpAttr =eqType.getAttribute(baseName + EquipmentAttribute.SEPARATOR + name);
		String label="";
		if (tmpAttr != null)
			{
			label = tmpAttr.getAttributeAlias();
			}
		if (label == null || label.equals(""))
			{
			label = name;
			}
		return label;
		}
				
	private String getThresholdStatusTable(EquipmentType eqType, IStatus iStatus) throws Exception
		{
		List<EquipmentAttribute> thresholdGroup = new ArrayList(eqType.getAttributesByType(EquipmentAttribute.THRESHOLDCOUNTER) );
		EquipmentAttribute tmpAttr;
		String returnTableStr = "";
		
		for (int i = 0; i < thresholdGroup.size(); i++)
			{
			tmpAttr = thresholdGroup.get(i);
			String tmpName = tmpAttr.getName();
			StringBuffer tableStr = new StringBuffer();
			tableStr.append("<table class=\"durabledetail\">");
			tableStr.append("<tr>");
			tableStr.append("<td width=\"25%\" colspan=\"4\"><b>" + tmpName + "</b></td>");
			tableStr.append("</tr>");
			
			String serviceUsageLabel = getAliasforThreshold(eqType,"serviceUsage",tmpName);			
			String serviceUsageName = tmpName + EquipmentAttribute.SEPARATOR + "serviceUsage";
			String serviceUsageValue = FiscalDate.getTimeBasedThresholdValueWithElapse(eqType, serviceUsageName, String.valueOf(iStatus.get(serviceUsageName)));
			String serviceUsageLimitLabel = getAliasforThreshold(eqType,"serviceUsageLimit",tmpName);	
			String serviceUsageLimitName = tmpName + EquipmentAttribute.SEPARATOR + "serviceUsageLimit";
			String serviceUsageLimitValue = FiscalDate.getTimeBasedThresholdValueWithElapse(eqType,serviceUsageLimitName,String.valueOf(iStatus.get(serviceUsageLimitName)));
			
			String totalServicesLabel = getAliasforThreshold(eqType,"totalServices",tmpName);	
			String totalServicesName = tmpName + EquipmentAttribute.SEPARATOR + "totalServices";
			String totalServicesValue = String.valueOf(iStatus.get(totalServicesName));			
			String totalServicesLimitLabel = getAliasforThreshold(eqType,"totalServicesLimit",tmpName);
			String totalServicesLimitName = tmpName + EquipmentAttribute.SEPARATOR + "totalServicesLimit";
			String totalServicesLimitValue = String.valueOf(iStatus.get(totalServicesLimitName));
			
			String rebuildUsageLabel =  getAliasforThreshold(eqType,"rebuildUsage",tmpName);
			String rebuildUsageName = tmpName + EquipmentAttribute.SEPARATOR + "rebuildUsage";
			String rebuildUsageValue = FiscalDate.getTimeBasedThresholdValueWithElapse (eqType, rebuildUsageName, String.valueOf(iStatus.get(rebuildUsageName)));			
			String rebuildUsageLimitLabel =  getAliasforThreshold(eqType,"rebuildUsageLimit",tmpName);
			String rebuildUsageLimitName = tmpName + EquipmentAttribute.SEPARATOR + "rebuildUsageLimit";
			String rebuildUsageLimitValue = FiscalDate.getTimeBasedThresholdValueWithElapse (eqType, rebuildUsageLimitName, String.valueOf(iStatus.get(rebuildUsageLimitName)));			
			
			String totalRebuildsLabel = getAliasforThreshold(eqType,"totalRebuilds",tmpName);
			String totalRebuildsName = tmpName + EquipmentAttribute.SEPARATOR + "totalRebuilds";
			String totalRebuildsValue = String.valueOf(iStatus.get(totalRebuildsName));
			String totalRebuildsLimitLabel = getAliasforThreshold(eqType,"totalRebuildsLimit",tmpName);
			String totalRebuildsLimitName = tmpName + EquipmentAttribute.SEPARATOR + "totalRebuildsLimit";
			String totalRebuildsLimitValue = String.valueOf(iStatus.get(totalRebuildsLimitName));
						
			String lifeUsageLabel = getAliasforThreshold(eqType,"lifeUsage",tmpName);
			String lifeUsageName = tmpName + EquipmentAttribute.SEPARATOR + "lifeUsage";
			String lifeUsageValue = FiscalDate.getTimeBasedThresholdValueWithElapse (eqType,lifeUsageName, String.valueOf(iStatus.get(lifeUsageName)));
			String lifeLimitLabel = getAliasforThreshold(eqType,"lifeLimit",tmpName);
			String lifeLimitName = tmpName + EquipmentAttribute.SEPARATOR + "lifeLimit";
			String lifeLimitValue = FiscalDate.getTimeBasedThresholdValueWithElapse (eqType,lifeLimitName, String.valueOf(iStatus.get(lifeLimitName)));
			
			tableStr.append("<tr>");
			tableStr.append("<td class=\"durabledetail\"><b>" + serviceUsageLabel + "</b></td>");
			tableStr.append("<td class=\"durabledetail\">" + serviceUsageValue + "</td>");		
			tableStr.append("<td class=\"durabledetail\"><b>" + serviceUsageLimitLabel + "</b></td>");
			tableStr.append("<td class=\"durabledetail\">" + serviceUsageLimitValue + "</td>");			
			tableStr.append("</tr>");
			
			tableStr.append("<tr class=\"durabledetail\">");
			tableStr.append("<td class=\"durabledetail\"><b>" + totalServicesLabel + "</b></td>");
			tableStr.append("<td class=\"durabledetail\">" + totalServicesValue + "</td>");		
			tableStr.append("<td class=\"durabledetail\"><b>" + totalServicesLimitLabel + "</b></td>");
			tableStr.append("<td class=\"durabledetail\">" + totalServicesLimitValue + "</td>");			
			tableStr.append("</tr>");
			
			tableStr.append("<tr>");
			tableStr.append("<td class=\"durabledetail\"><b>" + rebuildUsageLabel + "</b></td>");
			tableStr.append("<td class=\"durabledetail\">" + rebuildUsageValue + "</td>");		
			tableStr.append("<td class=\"durabledetail\"><b>" + rebuildUsageLimitLabel + "</b></td>");
			tableStr.append("<td class=\"durabledetail\">" + rebuildUsageLimitValue + "</td>");			
			tableStr.append("</tr>");
			
			tableStr.append("<tr class=\"durabledetail\">");
			tableStr.append("<td class=\"durabledetail\"><b>" + totalRebuildsLabel + "</b></td>");
			tableStr.append("<td class=\"durabledetail\">" + totalRebuildsValue + "</td>");		
			tableStr.append("<td class=\"durabledetail\"><b>" + totalRebuildsLimitLabel + "</b></td>");
			tableStr.append("<td class=\"durabledetail\">" + totalRebuildsLimitValue + "</td>");			
			tableStr.append("</tr>");
			
			tableStr.append("<tr>");
			tableStr.append("<td class=\"durabledetail\"><b>" + lifeUsageLabel + "</b></td>");
			tableStr.append("<td class=\"durabledetail\">" + lifeUsageValue + "</td>");		
			tableStr.append("<td class=\"durabledetail\"><b>" + lifeLimitLabel + "</b></td>");
			tableStr.append("<td class=\"durabledetail\">" + lifeLimitValue + "</td>");			
			tableStr.append("</tr>");
			
			tableStr.append("</table>");
			
			returnTableStr = returnTableStr + tableStr;
			}		
		return returnTableStr.toString();		
		}%>

<%=getStatusTable(status.ATTRIBUTES, type, status)%>

<%=getStatusTable(status.dynamicAttributes, type, status)%>

<%=getThresholdStatusTable(type, status)%>
<br>

<%@ include file ="durable_comments.jsp" %>


<% if (isCurrent){ 

//history related
List headers = (List) result2.get(0);
List resultList = (List) result2.get(1);
Map filters = (Map) result2.get(2);
Map selectedfilter = (Map) result2.get(3);
Report report = (Report) result2.get(4);
List displayheaders = (ArrayList)result2.get(6);
%>
<table>
   <tr>
      <th><a href="durable_transaction_details.do?durable_id=<%=status.DURABLE_ID%>">Make Transition</a></th>
     	<%
      	if (parentType != null&&!"N".equalsIgnoreCase(type.parent))
      		{
      %>
       <th>
<a href="durable_association_list.do?durable_id=<%=status.DURABLE_ID%>&parent=<%=status.workstation%>">Associate</a>
	  </th>
<%
	}
%>	

<%if(attachRule != null && attachRule.size() > 0)
	{%>
<th><a href="durable_attach_children_durable_list.do?durable_id=<%=status.DURABLE_ID%>"><%=BusinessRule.ATTACH_CHILD%></a> </th>
<%  }%>
       <th>
<a href="durable_commentList.do?durable_id=<%=status.DURABLE_ID%>&status_pk=<%=status.STATUS_PK%>">Add Comments</a>
	  </th>

      <% if (type.hasAttachmentAttribute()){%>
      <th>      
			<a href="durable_attachment_management.do?durable_id=<%=status.DURABLE_ID%>">Manage Attachments</a>
	  </th>
	  <%}%>
      
   </tr>
</table>
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
<button id="<%=attachtmpName + "_attachment_button_hide"%>">Hide old</button>
<button id="<%=attachtmpName + "_attachment_button_show"%>">Show all</button>
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
		
<%= tmpstatus.get(type, "OPERATOR_ID")%>
	</td>
	<td width=10%>
		
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




<%
	if (status.hasChild())
		{
%>
<h1>List of associated durables</h1>
<table width=100%>
<tr>
<td width=20%>
EquipmentType
</td>
<td width=20%>
Durable_ID
</td>
<td width=20%>
</td>
<td width=20%>
</td>
<td width=20%>
</td>
</tr>
<%
	Map children = status.getChildren();
		Set keyset = children.keySet();
		Iterator it = keyset.iterator();
		while (it.hasNext())
			{
			String equipmentTypetmp = it.next().toString();
			List childrenVector = (List) children.get(equipmentTypetmp);
			for (int i = 0; i < childrenVector.size(); i++)
				{
				String durableid = childrenVector.get(i).toString();
%>

<tr>
<td width=20%>
<%=equipmentTypetmp%>
</td>
<td width=20%>
<a href="durable_details.do?durable_id=<%=durableid%>"><%=durableid%></a> 
</td>
<td width=20%>
</td>
<td width=20%>
</td>
<td width=20%>
</td>
</tr>

<%
	}
			}
%>
</table>
<%
	}
%>


<h1>Transition History</h1>

<table>
<tbody>
<tr>
<td>Index</td>
<%
	Iterator headerIterator = headers.iterator();
	Iterator displayheaderIterator = displayheaders.iterator();
	String addanotherheader = "";
	while (headerIterator.hasNext())
		{
		String header = "";
		String headerDisplay = "";
		   
		if (addanotherheader.equalsIgnoreCase(""))
			{
			header = (String) headerIterator.next();	
			headerDisplay = ((String)displayheaderIterator.next()).toUpperCase();
			}
		else
			{
			header = addanotherheader;
			headerDisplay = addanotherheader;
			addanotherheader = "";
			}
		if ("STATUS_PK".equalsIgnoreCase(header))
    		{
    		continue;
    		}    
		//for historical report to set formstate and tostate   
		if (header.equalsIgnoreCase("STATE-SUBSTATE") && report.reportType.equalsIgnoreCase("History"))
			{
			header = "FROMSTATE";
			headerDisplay = "FROMSTATE";
			addanotherheader = "TOSTATE";
			}
		
%>
<td><%=headerDisplay%>
</td>
<%
	}
%>
</tr>
<%
	Iterator it = resultList.iterator();
	int index = 0;
	for (int i = 0; i < 5 && it.hasNext(); i++)
		{
		DurableStatus durableStatus = (DurableStatus) it.next();
		index++;
%>
<tr> 
<td><%=index%></td>
    <%
    	for (int j = 0; j < headers.size(); j++)
    			{
    			if ("STATUS_PK".equalsIgnoreCase((String)headers.get(j)))
    	    		{
    	    		continue;
    	    		}  
    			
    			if (((String) headers.get(j)).equalsIgnoreCase("STATE-SUBSTATE")
    					&& report.reportType.equalsIgnoreCase("History"))
    				{
    %>		
		<td><%=(String) durableStatus.get("FROMSTATE")%></td>
		<td><%=(String) durableStatus.get("TOSTATE")%></td>
		<%
    				}
    			else if (((String)headers.get(j)).equalsIgnoreCase("COMMENTS")){	 	
    					Set tmpSet = ((Map)(durableStatus.get((String)headers.get(j)))).keySet();
    				 	Vector tmpList = new Vector(tmpSet);
    				 	Collections.sort(tmpList);	
    				 	Iterator commentIt = tmpList.iterator();
    				 	if (tmpSet.size()>1) {
    				 		String tmpvalue = "";
    				 		String fullcomment = "";
    				 		int count = 0;
    				 		while (commentIt.hasNext()){
    				 			List comment = comment = (List)(((Map)durableStatus.get((String)headers.get(j))).get(commentIt.next()));
    				 			if (count ==0) {	 				
    					 			tmpvalue =(String)(comment.get(2));
    				 			}
    				 			count++;
    				 			fullcomment = fullcomment +  " - " + comment.get(2) + "<br>";
    				 			
    				 		}
    				 		%>
    						<td onmouseover="return overlib('<%=FiscalDate.escape(fullcomment)%>',CAPTION, '<%=headers.get(j)%>');" onmouseout="return nd();"><%=FiscalDate.trim(tmpvalue,20)%></td>
    						<%
    				 		
    				 	}
    				 	else{
    				 		
    				 		List comment = (List)(((Map)durableStatus.get((String)headers.get(j))).get(commentIt.next()));
    				 		String tmpvalue =(String)(comment.get(2));
    				 		%>
    						<td onmouseover="return overlib('<%=FiscalDate.escape(tmpvalue)%>',CAPTION, '<%=headers.get(j)%>');" onmouseout="return nd();"><%=FiscalDate.trim(tmpvalue,20)%></td>	
    						<% 	
    				 	}
    				 	
    				 	
    			}else{
		%>
		<td><%=(String) durableStatus.get(type,(String) headers.get(j))%></td>
		<%
			}
					}
			%>
</tr>
<%
	}
%>
</tbody></table>
<table>
   <tr>
   <th><a href="durable_realtime_simple_history_report.do?DURABLE_ID=<%=status.DURABLE_ID%>&transitionOnly=yes">Transition Report</a></th>
   <th><a href="durable_realtime_simple_history_report.do?DURABLE_ID=<%=status.DURABLE_ID%>&transitionOnly=no">All status Report</a></th>
   </tr>
</table>

<%} %>

 <script type="text/javascript">
            /*Main js function. Run when page initiate*/
            $(document).ready(function(){
                pageFormatters();
                clickHandlers();               
            });
            
            function pageFormatters() {
            	$("[id$=_attachment_button_hide]").hide();
            	$("[class$=_attachment_hide]").hide();
            }
            
function clickHandlers() {
            	
            	<% Vector attachmenttypes = type.getAttributesByType(EquipmentAttribute.ATTACHMENT); 
            	
            	   for (int i = 0 ; i<attachmenttypes.size(); i++){	
            			String attachtmpName =  ((EquipmentAttribute)attachmenttypes.get(i)).getName();
            	%>
            	
            	$("#<%=attachtmpName%>_attachment_button_show").click(function() {
            		$("#<%=attachtmpName%>_attachment_button_show").hide();
            		$("#<%=attachtmpName%>_attachment_button_hide").show();
                	$(".<%=attachtmpName%>_attachment_hide").show();
                });
            			
            	$("#<%=attachtmpName%>_attachment_button_hide").click(function() {
            		$("#<%=attachtmpName%>_attachment_button_hide").hide();
            		$("#<%=attachtmpName%>_attachment_button_show").show();
                	$(".<%=attachtmpName%>_attachment_hide").hide();
                });
            	<% } %>           
  }
 </script>           

<table width="100%" cellspacing="0" cellpadding="0">
	<tr>
		<td>

<%@ include file ="footer.html" %>
