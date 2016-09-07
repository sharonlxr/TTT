<%@ include file ="header.jsp" %>
<%@page import="com.freescale.stateStamper.model.entity.*"%>
<%@page import="java.util.*"%>
<%@page import="com.freescale.stateStamper.model.logic.StateTransition"%>
<%@page import="com.freescale.stateStamper.model.logic.FiscalDate"%>
<%@page import="com.freescale.stateStamper.model.logic.DurableManager"%>
<jsp:useBean id="result" scope="request" type="java.util.List"/>

<%	  
    List headers = (List)result.get(0);
	List resultList = (List)result.get(1);	
	Map filters =  (Map)result.get(2);	
	Map selectedfilter = (Map)result.get(3);
	Report report = (Report)result.get(4);
	String activity = (String)result.get(5);
	List displayheaders = (ArrayList)result.get(6);
	String transaction = "Transaction";
	String disabledStr ="";
	String activityParameter="";
	boolean disabled=false;		
	if(activity != null)
		{
		transaction=activity;
		disabled = true;
		activityParameter="&activity="+activity;
		}
	else{activity="";}
%>
<h1><%=equipmentType.name %> <%=transaction%> </h1>
<a href="durable_main.do">Back to Durable Main page</a>
<form id="searchForm" name="searchForm" method="get" action="durable_transaction_search.do">  

<table width=100%>
<tr>
<td width=50%><%=equipmentType.name %> ID<br></td>
<td width=50%>
<input type="text" value="" name="DURABLE_ID"> 
</td>
</tr>
<% 
for (int i = 0; i< headers.size();i++) 
	{
    String header = (String)headers.get(i);
	List filterValues = (List)filters.get(header);
	if(disabled && (header.equals("STATE") || header.equals("SUBSTATE") ) ){disabledStr = "disabled";}
	else{disabledStr = "";}
	if((!header.equalsIgnoreCase("DURABLE_ID"))&&filters.get(header)!=null) 
		{ 
		String headerDisplay = header.toUpperCase();
		%>
		
	<tr>
	<td width=50%><%=headerDisplay%><br></td>
	<td width=50%>
	<select name="<%=header%>" <%=disabledStr%> onChange='document.forms["searchForm"].submit()'>
	   <option  value=""></option>
		<%  
		String selectedValue = "";
		for (int j=0;j< filterValues.size();j++)
			{ 				
			selectedValue = "";	
			String value = (String)filterValues.get(j);
			if (selectedfilter.get(header).equals(value)) 
				{
				selectedValue = "selected";
				}
		%>			
            <option  value="<%=(String)value%>" <%=selectedValue%> ><%=(String)value%></option>			
         <%}%>
    </select>
</td>
</tr>
<%      }
   }%>
</table>
<input type="hidden" name="reportType" value="<%=report.reportType %>"/>
<input type="hidden" name="grouping" value="<%=report.grouping %>"/>
<input type="hidden" name="activity" value="<%=activity%>"/>
</form>
<table class="simplereport">
<tbody>
<tr>
<td class="simplereport">INDEX</td>
<% 
for(int i=0;i<headers.size();i++) {
    String header = (String)headers.get(i);
    String headerDisplay = ((String)displayheaders.get(i)).toUpperCase();
%>
<td class="simplereport"><%=headerDisplay%>
</td>
<% }%>
</tr>
<% 
 for (int i=0;i<resultList.size();i++){
	 Machine durable= (Machine)resultList.get(i);
%>
<tr> 
<td class="simplereport"><%=i+1%></td>
    <%  for(int j =0 ; j<headers.size() ; j++)
    	{
    	if ("DURABLE_ID".equalsIgnoreCase((String)headers.get(j)))
    		{
    	%>
    	<td class="simplereport"><a href="durable_transaction_details.do?durable_id=<%=durable.get("DURABLE_ID")+activityParameter%>"><%=(String)durable.get("DURABLE_ID")%></a></td>
		<%}else if ("children".equalsIgnoreCase((String)headers.get(j))){
		 	String childrenlist= ""; 
			Map children =(Map) durable.get((String)headers.get(j)); 
			Set keyset = children.keySet();
			Iterator iterator = keyset.iterator();
			while (iterator.hasNext()){
				String equipmentTypetmp = iterator.next().toString();	
				if (equipmentTypetmp.trim().equals("")) continue;
				childrenlist = childrenlist +  " " + equipmentTypetmp + ":"; 				
				String tmps = children.get(equipmentTypetmp).toString();
				if (tmps.contains("[")){
					tmps = tmps.substring(1);
					tmps = tmps.substring(0,tmps.length()-1);
				}
				childrenlist = childrenlist + " " +  tmps;
			}		
		%>
		<td class="simplereport"><%=childrenlist%></td>		
		<%} else if(((String)(headers.get(j))).startsWith("COUNTERBAR")) {
    		String color = DurableManager.getCounterbarColor(durable, equipmentType, (String)headers.get(j));
    		String tmpvalue = (String)(durable.get((String)headers.get(j)));
    		if (((String)(headers.get(j))).startsWith("COUNTERBAR_TIME")) {
    			tmpvalue = DurableManager.getTimebasedCounterbarValue((String)durable.get((String)headers.get(j)));
    		}
    		else if (((String)(headers.get(j))).startsWith("COUNTERBAR_WW")){
    			tmpvalue = DurableManager.getWorkWeekbasedCounterbarValue((String)durable.get((String)headers.get(j)));
    		}
    		%>
    			<td class="simplereport"><font color="<%=color%>"><%=tmpvalue%></font></td>			
    		<% 
    	} 	
	else if (((String)headers.get(j)).equalsIgnoreCase("COMMENTS")){	 	
	Set tmpSet = ((Map)(durable.get((String)headers.get(j)))).keySet();
 	Vector tmpList = new Vector(tmpSet);
 	Collections.sort(tmpList);	
 	Iterator commentIt = tmpList.iterator();
 	if (tmpSet.size()>1) {
 		String tmpvalue = "";
 		String fullcomment = "";
 		int count = 0;
 		while (commentIt.hasNext()){
 			List comment = (List)(((Map)durable.get((String)headers.get(j))).get(commentIt.next()));
 			if (count ==0) {	 				
	 			tmpvalue =(String)(comment.get(2));
 			}
 			count++;
 			fullcomment = fullcomment +  " - " + comment.get(2) + "<br>"; 			
 		}
 		%>
		<td class="simplereport" onmouseover="return overlib('<%=FiscalDate.escape(fullcomment)%>',CAPTION, '<%=headers.get(j)%>');" onmouseout="return nd();"><%=FiscalDate.trim(tmpvalue,20)%></td>
		<%	
 	}
 	else{	
 		List comment = (List)(((Map)durable.get((String)headers.get(j))).get(commentIt.next()));
 		String tmpvalue =(String)(comment.get(2));
 		%>
		<td class="simplereport" onmouseover="return overlib('<%=FiscalDate.escape(tmpvalue)%>',CAPTION, '<%=headers.get(j)%>');" onmouseout="return nd();"><%=FiscalDate.trim(tmpvalue,20)%></td>	
		<% 	
 	}
}else {
			String tmpvalue =durable.get((String)headers.get(j)).toString();
			if (tmpvalue.length()<=20){
			//when the size is under 20
			%>
			<td class="simplereport"><%=tmpvalue%></td>		
		<% 	
			}
			else{
		%>
		<td class="simplereport" onmouseover="return overlib('<%=FiscalDate.escape(tmpvalue)%>');" onmouseout="return nd();"><%=FiscalDate.trim(tmpvalue,20)%></td>
		<% }}}%>	
</tr>
<%}%>
</tbody></table>
<%@ include file ="footer.html" %>