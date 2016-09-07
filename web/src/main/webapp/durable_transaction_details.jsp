<%@ include file ="header.jsp" %>
<%@page import="com.freescale.stateStamper.model.entity.*"%>
<%@page import="java.util.*"%>
<%@page import="com.freescale.stateStamper.model.logic.DurableManager"%>
<jsp:useBean id="result" scope="request" type="java.util.Map"/>
<<script type="text/javascript">
function checkName(item,spanid){
	var coreid = $(item).val();
	$("#"+spanid).html("");
	$.ajax({
        type: "GET",
        url: "getUserFullName.do",
        data: "coreid="+coreid,
        success: function(data){
            if(data!=null&&data!=""&&data!=coreid)
           	 {
           	 $("#"+spanid).html(data);
           	 }
            else
           	 {
           	 $("#"+spanid).html("invalid core id");
           	 }
        }
    });
}
</script>
<% 
String activity = request.getParameter("activity");
String transaction = "Transaction";
String activityState="";
Integer activityStatePk=null;
boolean isActivity = activity != null;
if(isActivity)
	{
	transaction=activity;
	Substate toState = (Substate)result.get("activityState");
	activityState = toState.e10+"-"+toState.substate;
	activityStatePk = toState.primaryKey;
	}
Map states = (Map)result.get("states");
String toSubstate = request.getParameter("toSubstate");
CurrentDurableStatus currentDurableStatus = (CurrentDurableStatus)result.get("currentStatus");
List parents = (List)result.get("parents");
List locations = (List)result.get("locations");
Map attributes = (Map)result.get("tranAttributes");
Map multiSelection = (Map)result.get("multiSelection"); 
Map singleSelection = (Map)result.get("singleSelection"); 
Map attachments = (Map)result.get("attachment"); 
%>
<h1><%=equipmentType.name%> <%=transaction%> </h1>
<a href="durable_main.do">Back to Durable Main page</a>

<form id="transaction" name="transaction" method="post" action="durable_transaction.do" enctype="multipart/form-data" onsubmit="submit.disabled = true;var sub = validateTransaction('requiredDynamicAttribute','transaction');submit.disabled = sub;return sub;">  
<a href="durable_details.do?durable_id=<%=currentDurableStatus.DURABLE_ID%>">Back to Durable Details for <%=currentDurableStatus.DURABLE_ID%></a>	

<table width=100%>
<tr>
<td width=50%><%=equipmentType.name%> ID<br></td>
<td width=50%><%=currentDurableStatus.DURABLE_ID%></td>
<td> 
<input type="hidden" id="STATUS_PK" name="STATUS_PK" value="<%=currentDurableStatus.STATUS_PK%>"> 
<input type="hidden" id="durableId" name="durableId" value="<%=currentDurableStatus.DURABLE_ID%>">
</td>
</tr>

<tr>
<td width=50%>
Current Status
</td>
<td width=50%>
<%=currentDurableStatus.SUBSTATE%>
</td>
</tr>

<tr>
<td width=50%>
To Status
</td>
<td width=50%>
<%if(!isActivity)
	{%>
	<select name="substate" id="substate" onchange="onchangestate(this.form.substate);">
	<%
	if(toSubstate == null)
		{%>
		<option id=" " value="-1" > </option>
	  <%}
	List defined = (List)states.get("defined");
	List forced = (List)states.get("forced");
	List all = new ArrayList();
	if(defined != null){all.addAll(defined);}
	if(forced!=null){all.addAll(forced);}
	insertStateForDuplicateSubstates(all);
	if(defined != null && defined.size() > 1){orderBySubstate(defined);}
	out.print(getStateOptions(defined,toSubstate,"",""));
	if(states.containsKey("forced"))
		{
		%>
		<option id="-Non-defined transitions-" value="-1" <%="-1".equals(toSubstate)?"selected=\"selected\"":"" %> style="background-color:#D3D3D3;color:black;font-style:italic;" >-Non-defined transitions-</option>
		<% 
		forced = (List)states.get("forced");
		if(forced != null && forced.size() > 1){orderBySubstate(forced);}
		out.print(getStateOptions(forced,toSubstate,"forced_","style=\"background-color:#D3D3D3;color:black;font-style:italic;\""));
		}
		%>
	</select>
<%
   }
else
	{%>
	<%=activityState%>
	<input type="hidden" name="substate" value="<%=activityStatePk%>"/>
<% 	} %>

</td>
</tr>
<%!
private String getStateOptions(List states,String selectedState,String idprefix,String style)
	{
	StringBuffer sb = new StringBuffer();
	for (int i=0; states != null && i < states.size();i++)
		{ 
	     Substate sub = (Substate)states.get(i);
	     String item = sub.substate;
	     String value = String.valueOf(sub.primaryKey);
	     String selected="";
	     if (selectedState != null && selectedState.equals(value)){selected="selected=\"selected\"";}
	     sb.append("<option id=\""+idprefix+item+"\" value=\"" +value  +"\" " + selected +  " " + style +" >"+item+"</option>\n"); 
	   }
	return sb.toString();
	}

private void orderBySubstate(List states)
	{
	Comparator comp = ((Substate)states.get(0)).new CompBySubstate();
	Collections.sort(states,comp);
	}

private void insertStateForDuplicateSubstates(List states)
	{
	Map substates = new HashMap();
	Set duplicates = new HashSet();
	for(int i=0;states != null && i <  states.size();i++)
		{
		Substate sub  = (Substate)states.get(i);
		if(substates.containsKey(sub.substate))
			{
			duplicates.add(sub);
			duplicates.add(states.get((Integer)substates.get(sub.substate)));
			}
		substates.put(sub.substate,i);
		}
	for(Iterator it = duplicates.iterator();it.hasNext();)
		{
		Substate tmp = (Substate)it.next();
		tmp.substate = tmp.substate + " ("+tmp.e10+") ";
		}
	}
%>
<%if(attributes.containsKey("workstation"))
	{
	Map workstation = (Map) attributes.remove("workstation");
	String display = (String)workstation.get(DurableManager.TRANSITION_FIELD_DISPLAY);
	String value = (String)workstation.get(DurableManager.TRANSITION_FIELD_VALUE);
	if(value != null && !value.trim().equals("")){parents.add(value);}
	String workStationId = "workstation";
	String readOnly = "";
	if(display.equals(TransitionCondition.REQUIRE))	{workStationId="required_"+workStationId;}
	else if(display.equals(TransitionCondition.DISPLAY)){readOnly="disabled=\"disabled\"";}
	%>
	<tr> 
		<td width=50%>Workstation</td>
		<td width=50%>
		<select name="workstation" title="workstation" id="<%=workStationId%>" <%=readOnly%> >
			<option  value=""></option>
			<%for (int i = 0;i < parents.size();i++)
				{
				String parent = parents.get(i).toString();
				String selected="";
				if(parent.equals(value)){selected="selected=\"selected\"";}
				%>
			    <option  value="<%=parent%>" <%=selected%> ><%=parent%></option>
			<%  }%>
		</select>
		</td>
	</tr>
<%  }%>

<% if(attributes.containsKey("location"))
	{
	Map locationMap = (Map) attributes.remove("location");
	String display = (String)locationMap.get(DurableManager.TRANSITION_FIELD_DISPLAY);
	String value = (String)locationMap.get(DurableManager.TRANSITION_FIELD_VALUE);
	String locationId = "location";
	String readOnly = "";
	if(display.equals(TransitionCondition.REQUIRE))	{locationId="required_"+locationId;}
	else if(display.equals(TransitionCondition.DISPLAY)){readOnly="disabled=\"disabled\"";}

%>
<tr> 
		<td width=50%>Location</td>
		<td width=50%>
		<select name="location" title="location" id="<%=locationId%>" <%=readOnly%> >
			<%for (int i = 0;i < locations.size();i++)
				{
				String location = locations.get(i).toString();
				String selected="";
				if(location.equals(value)){selected="selected=\"selected\"";}
				%>
			    <option  value="<%=location%>" <%=selected%> ><%=location%></option>
			<%  }%>
		</select>
		</td>
	</tr>
<%  }

if (attributes.containsKey(TransitionCondition.PROPAGATE)) 
	{
	attributes.remove(TransitionCondition.PROPAGATE);
	if(currentDurableStatus.hasChild())
		{%>
		<tr>
		<td width=50%>
		propagate status to children?
		</td>
		<td width=50%>
		<select name="propstatus">
		<option  value="Y">Y</option>
		<option  value="N">N</option>
		</select>
		</td>
		</tr>
<%      }
	}

if(equipmentType.propagateStatus())
	{%>
	<tr><td>List of Children</td></tr>
	<%
		Map childrenTypes = new HashMap();
	    childrenTypes.putAll(currentDurableStatus.getChildren()); 
		Iterator iterator = childrenTypes.keySet().iterator();
		while (iterator.hasNext())
			{
			String equipmentTypetmp = iterator.next().toString();
			List children = (List)childrenTypes.get(equipmentTypetmp);		
			for (int i= 0; i<children.size();i++) 
				{
				String durableid = children.get(i).toString();
				%>
				<tr>
					<td><%=equipmentTypetmp%></td> <td><a href="durable_details.do?durable_id=<%=durableid%>"><%=durableid%></a></td>
				</tr>
	<%          }
			}
	}
	
    Map field;
    Map comments = (Map)attributes.remove("COMMENTS");
    Iterator it = attributes.keySet().iterator();
	for (int i = 0; it.hasNext();i++)
		{
		String fieldName = (String)it.next(); 
	    field = (Map)attributes.get(fieldName);
	    String name = (String)field.get(DurableManager.TRANSITION_FIELD_NAME);
	    String label = (String)field.get(DurableManager.TRANSITION_FIELD_LABEL);
	    Object objvalue = field.get(DurableManager.TRANSITION_FIELD_VALUE);
	    String display = (String)field.get(DurableManager.TRANSITION_FIELD_DISPLAY);
        String fieldStatus = (String)field.get(DurableManager.TRANSITION_FIELD_STATUS);
        String checkName = (String)field.get(DurableManager.TRANSITION_FIELD_CHECKNAME);
	    String value=null;
	    String required=null;
	    String backgroundColor="";
	    String id ="";
		if (objvalue!=null){value = objvalue.toString();}
		else{value = "";}	
        if(fieldStatus != null)
        	{
	        if(fieldStatus.equals(DurableManager.TRANSITION_PERCENTAGE_STATUS_OK)){backgroundColor="#00FF00";}
	        else if(fieldStatus.equals(DurableManager.TRANSITION_PERCENTAGE_STATUS_WARNING)){backgroundColor="#FFFF00";}
	        else if(fieldStatus.equals(DurableManager.TRANSITION_PERCENTAGE_STATUS_CRITICAL)){backgroundColor="#FF0000";}
	 	    backgroundColor="style=\"background-color:"+backgroundColor+"\"";	
        	}
	    String readOnly="";
		if(display.equals(TransitionCondition.DISPLAY )){readOnly="readonly=\"readonly\"";}
		else if(display.equals(TransitionCondition.REQUIRE)){id = "id=\"requiredDynamicAttribute_" + i +"\"";}
			
%>
<tr>
<% 
List valuesMulti  = (List)multiSelection.get(name);
List valuesSingle = (List)singleSelection.get(name);
Object attachment = attachments.get(name);
boolean single = valuesSingle != null && !valuesSingle.isEmpty();
boolean multi =  valuesMulti  != null && !valuesMulti.isEmpty();
boolean isAttachment = attachment != null ;
boolean isList =  single || multi;
if(!isList&&display.equals(TransitionCondition.DISPLAY )){readOnly="readonly=\"readonly\"";}
else if(isList&&display.equals(TransitionCondition.DISPLAY)){readOnly="disabled=\"disabled\"";}
else {readOnly="";}
if(!isList&&!isAttachment)
	{
	%>
	<td width=50%><%=label%><br></td>
	<td width=50%>
	<%if(checkName!=null){ %>
	<input  type="text" value="<%=value%>" name="<%=name%>" size="35" onchange="checkName(this,'<%=name.replaceAll(" ","")%>verify');" title="<%=label%>" <%=id%> <%=readOnly%> <%=backgroundColor %> >
	<span id="<%=name.replaceAll(" ","")%>verify"></span>
	<% } else {%>
	<input  type="text" value="<%=value%>" name="<%=name%>" size="35" title="<%=label%>" <%=id%> <%=readOnly%> <%=backgroundColor %> >
	<%}
	} 
else if (isAttachment){
	%>
	
	<%  
	if (display.equals(TransitionCondition.DISPLAY)&&!label.equals(DurableManager.TRANSITION_FIELD_NOATTACHMENT)){
		 
	%>
		<td width=50%><%=name%><br></td>
		<td width=50%>
		<a href='durable_getAttachment.do?file=<%=objvalue%>' ><%=label%></a>
	<%	 
	 }else if (display.equals(TransitionCondition.DISPLAY)&&label.equals(DurableManager.TRANSITION_FIELD_NOATTACHMENT)) {
	%>
		<td width=50%><%=name%><br></td>
		<td width=50%>
	<%
	}	
	else {
	%>
		<td width=50%><%=name%><br></td>
		<td width=50%>
		<input type="file" name="<%=name%>" size="50" title="<%=label%>" <%=id%> <%=readOnly%> />
	<%
	 }
}
else{
	%>
	<td width=50%><%=label%><br></td>
	<td width=50%>
	<% 
    String multiple = ""; 
    List values = valuesSingle;
    int size = 1;
	if (multi)
		{
		multiple = "multiple";
		values = valuesMulti;
		size = valuesMulti.size()+1;
		}
	%>    
    <select title="<%=label%>" name="<%=name%>" <%=id%> <%=multiple%> size="<%=size%>" <%=readOnly%>>
    <option  value=""></option>
    <% 	
	 for (int j= 0 ; j<values.size();j++)
		 {%>
	    <option  value="<%=values.get(j).toString()%>"><%=values.get(j).toString()%></option>
	<%   }%>   
  </select>  
<% }%>
</td>
</tr>
<% } %>


<tr>
<td width=50%>
Comments
</td>
<td width=50%>
<%
List selections = null;
String multiple ="";
if(multiSelection.containsKey("COMMENTS"))
	{
	selections = (List)multiSelection.get("COMMENTS");
	multiple = "multiple";
	}
if(singleSelection.containsKey("COMMENTS"))
	{
	selections = (List)singleSelection.get("COMMENTS");
	}

String commentsId = "COMMENTS";
if(comments != null &&  TransitionCondition.REQUIRE.equals(comments.get(DurableManager.TRANSITION_FIELD_DISPLAY)) ){commentsId="required_"+commentsId;}
if(selections==null||selections.isEmpty()){
%>	
<textarea rows="10" cols="30" name="COMMENTS" title="COMMENTS" id="<%=commentsId%>" ></textarea>
<% }else{  %>    
	    <select name="COMMENTS" title="COMMENTS" <%=multiple%>  id="<%=commentsId%>"  size="1">
	    <% 	
		 for (int j= 0 ; j<selections.size();j++){
		%>
		<option  value="<%=selections.get(j).toString()%>"><%=selections.get(j).toString()%></option>
		<% } %>	   
	  </select>  
<% }%>	
</td>
</tr>

<tr>
<td width=50%>
</td>
<td width=50%>
<input type="submit" name="submit" value="submit" >
</td>
</tr>


</table>

</form>
<%@ include file ="footer.html" %>