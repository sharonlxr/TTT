<%@ include file ="header.jsp" %>
<%@page import="java.io.UnsupportedEncodingException"%>
<jsp:useBean id="result" scope="request" class="java.util.Vector"/>
<%@page import="com.freescale.stateStamper.model.entity.Machine "%>
<%@page import="com.freescale.stateStamper.model.entity.DurableStatus"%>
<%@page import="com.freescale.stateStamper.model.logic.FiscalDate"%>
<%@page import="java.net.URLEncoder"%>
<h1>Durable Management - Inventory  </h1>
<a href="durable_main.do">Back to Durable Main page</a>
<br/>
<%
List equipments      = (List)result.get(0);
Map summaryTables    = (Map)result.get(1);
List headers		 = (List)result.get(2);
Integer rowsperPage	 = (Integer)result.get(3);
Integer total = (Integer)((List)summaryTables.get("equipmentType")).get(0); 

List summaries =  new Vector(summaryTables.keySet());


String range    = request.getParameter("range");

Map parameters = request.getParameterMap();
request.setAttribute("params",parameters);
String currentQuery = request.getQueryString();
if(currentQuery==null){currentQuery="";}
else{
   List rem = new Vector();
   rem.add("range");
   currentQuery = getFilteredQuery(parameters,rem);
   }
Integer rangeStart= Integer.valueOf(0);
if(range==null){range="1-"+rowsperPage;}
if(range.contains("-")){rangeStart = Integer.valueOf(range.split("-")[0]);}

String linkToRestore="";
boolean filtered =false;
List paramsToRemove; 

%>
<%!

private String getFilteredQuery(Map paramMap,List names)throws UnsupportedEncodingException
	{
	StringBuffer query=new StringBuffer();
	Iterator it = paramMap.entrySet().iterator();
	while(it.hasNext())
		{
		Map.Entry param = (Map.Entry)it.next();
		String key = (String)param.getKey();
		if(names.contains(key)){continue;}
		String value = ((String[])param.getValue())[0];
		query.append(URLEncoder.encode(key,"UTF-8"));
		query.append("=");
		query.append(URLEncoder.encode(value,"UTF-8"));
		query.append("&");
		}
	if(query.length()>0  && query.lastIndexOf("&") == query.length()-1 ){query.deleteCharAt(query.length()-1);}
	return query.toString();
	}

private HashMap getFilteredParameters(Map paramMap,List names) throws UnsupportedEncodingException
	{
	StringBuffer query=new StringBuffer();
	Iterator it = paramMap.entrySet().iterator();
	HashMap returnhashtable = new HashMap();
	while(it.hasNext())
		{
		Map.Entry param = (Map.Entry)it.next();
		String key = (String)param.getKey();
		if(names.contains(key)){continue;}
		String value = ((String[])param.getValue())[0];
		returnhashtable.put(key,value);
		}
	return returnhashtable;
	}
%>

<table >
	<tr>
		<!-- Summary tables -->
		<%
		for(int i=0;summaries!=null && i< summaries.size();i++)
			{
			String category = (String)summaries.get(i);
			List results = (List)summaryTables.get(category);
			request.setAttribute("items",results);
			filtered = parameters != null && parameters.get(category)!=null;
			paramsToRemove = new Vector();
			paramsToRemove.add("range");
			paramsToRemove.add(category);
			linkToRestore =     getFilteredQuery(parameters,paramsToRemove);
			boolean displayPercentage = category.equals("state") || category.equals("substate");  
		%>
		<td valign="top" >
		<jsp:include page="durable_summary_table.jsp">
		 <jsp:param name="field" value="<%=category%>" />
		 <jsp:param name="currentQuery" value="<%=currentQuery%>" />
		 <jsp:param name="restoreLink" value="<%=linkToRestore%>" />
		 <jsp:param name="filtered" value="<%=filtered%>" />
		 <jsp:param name="displayPercentage" value="<%=displayPercentage%>" />
		</jsp:include>
		</td>
	 <%     } %>
	</tr>

</table>
<table >
<tr>
<td>
<!-- list to select the page -->
<% 
paramsToRemove = new Vector();
paramsToRemove.add("range");
HashMap hiddens =   getFilteredParameters(parameters,paramsToRemove);
%>
<form name="rangeForm" action="durable_inventory.do?<%=linkToRestore%>" >
<select name="range"  onchange="document.forms['rangeForm'].submit()">
<%
 String pageNumber="";
 String selected="";
 for(int i=0;i<total;i+=rowsperPage)
	 {
	 int start = i+1;
	 int end = start + rowsperPage > total?total:start + rowsperPage-1;
	 pageNumber = start+"-"+end ;	 
	 if(range.equals(pageNumber)){selected="selected=\"selected\"";}
	 else{selected="";}
%>
     <option <%=selected%> value="<%=pageNumber%>"><%=pageNumber%></option>
<%
	 }
%>	 
</select>
<% 
	Set keySet = hiddens.keySet();
	Iterator iterator = keySet.iterator();
	while (iterator.hasNext()){
		Object key = iterator.next();
		Object value = hiddens.get(key);	
%>
	<input type="hidden" id="<%=key%>" name="<%=key%>" value="<%=value%>"> 
	<% } %>
</form>
</td>
<td>
<a href="durable_inventory_excel.do?<%=currentQuery%>" ><img alt="excel" src="images/ExcelReport.gif"> </a>
</td>
</tr>
</table>
<!-- list of durables -->
<table>
<tr>
<th>Index</th>
<% 
Iterator headerIterator = headers.iterator();
while (headerIterator.hasNext()) {
    String header = (String)headerIterator.next();
%>
<th><%=header%></th>  

<% }%>
</tr>
<% Iterator it = equipments.iterator(); 
 int index = Integer.parseInt(range.split("-")[0])-1;
 while (it.hasNext()){
	 Machine durable= (Machine)it.next();	 
	 index++;
%>

<tr> 
<td><%=index%></td>
    <%  for(int i =0 ; i<headers.size() ; i++) 
    	{
    	if ("DURABLE_ID".equalsIgnoreCase((String)headers.get(i)))
    		{
    	%>
    	<td><a href="durable_details.do?durable_id=<%=URLEncoder.encode((String)durable.get("DURABLE_ID"),"UTF-8")%>"><%=(String)durable.get("DURABLE_ID")%></a></td>
		<%}else {
			String tmpvalue =(String) durable.get((String)headers.get(i));
			if (tmpvalue.length()<=20){
			//when the size is under 20
		%>
		<td><%=tmpvalue%></td>		
		<% 	
			}
			else{
		%>
		<td onmouseover="return overlib('<%=FiscalDate.escape(tmpvalue)%>');" onmouseout="return nd();"><%=FiscalDate.trim(tmpvalue,20)%></td>
<%}}}%>
</tr>
<% }%>
</table>

<%@ include file ="footer.html" %>