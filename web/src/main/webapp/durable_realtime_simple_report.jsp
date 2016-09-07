<%@ include file ="header.jsp" %>
<%@page import="com.freescale.stateStamper.model.entity.*"%>
<%@page import="java.util.*"%>
<%@page import="com.freescale.stateStamper.model.logic.StateTransition"%>
<%@page import="com.freescale.stateStamper.model.logic.FiscalDate"%>
<%@page import="com.freescale.stateStamper.model.logic.DurableManager"%>
<jsp:useBean id="result" scope="request" class="java.util.Vector"/>

<%	  
    List headers = (ArrayList)result.get(0);
	List resultList = (ArrayList)result.get(1);	
	Hashtable filters =  (Hashtable)result.get(2);	
	Hashtable selectedfilter = (Hashtable)result.get(3);
	Report report = (Report)result.get(4);
	List displayheaders = (ArrayList)result.get(6);
	
%>
<h1><%=report.reportType%> </h1>
<a href="durable_main.do">Back to Durable Main page</a>

<table class="simplereport">
<tbody>
<form id="searchForm" name="searchForm" method="get" action="durable_realtime_simple_report.do">
<tr>
<td class="simplereport">INDEX</td>
<% 
Iterator headerIterator = headers.iterator();
Iterator displayheaderIterator = displayheaders.iterator();
while (headerIterator.hasNext()) {
    String header = (String)headerIterator.next();    
    String headerDisplay = ((String)displayheaderIterator.next()).toUpperCase();
%>
<td class="simplereport"><%=headerDisplay%>
<br>
<% 

ArrayList filterValueList = (ArrayList)filters.get(header); 
if(filters.get(header)!=null) { %>
<select name="<%=header%>" onChange='document.forms["searchForm"].submit()'>
<option  value=""></option>
<%  
	Iterator filterValueIterator = filterValueList.iterator();	
	String selectedValue = "";
	while (filterValueIterator.hasNext()){ 				
		selectedValue = "";	
		String value = (String)filterValueIterator.next();
		if (selectedfilter.get(header).equals(value)) 
			{
			selectedValue = "selected";
			}
%>			
<option  value="<%=(String)value%>" <%=selectedValue%>><%=(String)value%></option>			
<% }%>
</select>
<% }%>
</td>
<% }%>
</tr>
<input type="hidden" name="reportType" value="<%=report.reportType %>"/>
<input type="hidden" name="grouping" value="<%=report.grouping %>"/>
</form>



<% Iterator it = resultList.iterator();
	int index = 0;
 while (it.hasNext()){
	 Machine durable= (Machine)it.next();
	 index ++;
%>
<tr> 
<td class="simplereport"><%=index%></td>
    <%  for(int i =0 ; i<headers.size() ; i++) 
    	{
    	if ("DURABLE_ID".equalsIgnoreCase((String)headers.get(i)))
    		{
    	%>
    	<td class="simplereport"><a href="durable_details.do?durable_id=<%=(String)durable.get("DURABLE_ID")%>"><%=(String)durable.get("DURABLE_ID")%></a></td>

		<%
    	}   else if ("children".equalsIgnoreCase((String)headers.get(i))){
		 	String childrenlist= ""; 
			Map children =(Map) durable.get((String)headers.get(i)); 
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
		<% }  else if (((String)headers.get(i)).equalsIgnoreCase("COMMENTS")){	 	
    			Set tmpSet = ((Map)(durable.get((String)headers.get(i)))).keySet();
    		 	List tmpList = new ArrayList(tmpSet);
    		 	Collections.sort(tmpList);	
    		 	Iterator commentIt = tmpList.iterator();
    		 	if (tmpSet.size()>1) {
    		 		String tmpvalue = "";
    		 		String fullcomment = "";
    		 		int count = 0;
    		 		while (commentIt.hasNext()){
    		 			List comment = (List)(((Map)durable.get((String)headers.get(i))).get(commentIt.next()));
    		 			if (count ==0) {	 				
    			 			tmpvalue =(String)(comment.get(2));
    		 			}
    		 			count++;
    		 			fullcomment = fullcomment +  " - " + comment.get(2) + "<br>";
    		 			
    		 		}
    		 		%>
    				<td class="simplereport" onmouseover="return overlib('<%=FiscalDate.escape(fullcomment)%>',CAPTION, '<%=headers.get(i)%>');" onmouseout="return nd();"><%=FiscalDate.trim(tmpvalue,20)%></td>
    				<%
    		 		
    		 	}
    		 	else{
    		 		
    		 		List comment = (List)(((Map)durable.get((String)headers.get(i))).get(commentIt.next()));
    		 		String tmpvalue =(String)(comment.get(2));
    		 		%>
    				<td class="simplereport" onmouseover="return overlib('<%=FiscalDate.escape(tmpvalue)%>',CAPTION, '<%=headers.get(i)%>');" onmouseout="return nd();"><%=FiscalDate.trim(tmpvalue,20)%></td>	
    				<% 	
    		 	}

    		}
    	else if(((String)(headers.get(i))).startsWith("COUNTERBAR")) {
    		String color = DurableManager.getCounterbarColor(durable, equipmentType, (String)headers.get(i));
    		String tmpvalue = (String)(durable.get((String)headers.get(i)));
    		if (((String)(headers.get(i))).startsWith("COUNTERBAR_TIME")) {
    			tmpvalue = DurableManager.getTimebasedCounterbarValue((String)durable.get((String)headers.get(i)));
    		}
    		else if (((String)(headers.get(i))).startsWith("COUNTERBAR_WW")){
    			tmpvalue = DurableManager.getWorkWeekbasedCounterbarValue((String)durable.get((String)headers.get(i)));
    		}
    	
    		%>
    		<td class="simplereport"><font color="<%=color%>"><%=tmpvalue%></font></td>		
    		<% 
    	}
    	else {
		String tmpvalue =durable.get((String)headers.get(i)).toString();
			if (tmpvalue.length()<=20){
			//when the size is under 20
		%>
		<td class="simplereport"><%=tmpvalue%></td>		
		<% 	
			}	
			else{
		%>
		<td class="simplereport" onmouseover="return overlib('<%=FiscalDate.escape(tmpvalue)%>');" onmouseout="return nd();"><%=FiscalDate.trim(tmpvalue,20)%></td>
<%}}}%>
</tr>
<% }%>
</tbody></table>
<%@ include file ="footer.html" %>