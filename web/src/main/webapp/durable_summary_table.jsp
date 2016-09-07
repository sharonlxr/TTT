<%@page import="java.util.*"%>
<%@page import="java.io.UnsupportedEncodingException"%>
<%@page import="java.net.URLEncoder"%>
<%
String field= request.getParameter("field");
String currentQuery = request.getParameter("currentQuery");
String restoreLink = request.getParameter("restoreLink");
Boolean displayPercentage = Boolean.valueOf(request.getParameter("displayPercentage"));
Map hiddenFields = (Map)request.getAttribute("params");
Character cap = Character.toUpperCase(field.charAt(0)) ;
String header = cap + field.substring(1);
String newQuery;
if(currentQuery==null || currentQuery.trim().equals("")){newQuery=field;}
else{newQuery=currentQuery+"&"+field;}
if(restoreLink!=null && ! restoreLink.trim().equals("")){restoreLink="?"+restoreLink;}
Boolean filtered = Boolean.valueOf(request.getParameter("filtered"));
String topping;
String headerStyle="";
if(filtered)
	{
	topping="<a href=\"durable_inventory.do"+restoreLink +"\">Display all</a>";
	headerStyle="style=\"background-color: #2D8AA7;font-weight: bold;\"";
	}
else{
    topping="&nbsp;";
    }
List items = (List)request.getAttribute("items");
boolean combo =  items!=null && items.size()> 11;


%>

<%! 
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
<td align="right"   colspan="2"><%=topping%></td>
</tr>
<tr>
<th align="left"  <%=headerStyle%> ><%=header %></th> <th align="right" <%=headerStyle%>><%=items.get(0)%></th> 
<%if(displayPercentage.booleanValue()){%> <th align="right">100%</th><%} %>  
</tr>
<% if (combo){
 	if  (displayPercentage.booleanValue()) {
%>	
   <tr><td align="left" colspan="3">
 <%}else{%>
 	<tr><td align="left" colspan="2">
 <%}%>  
   <form action="durable_inventory.do" name="<%=field%>Form">
   <select name="<%=field%>" size="10" onclick='document.forms["<%=field%>Form"].submit()'>
<% }%>
<% for(int i=1;i<items.size();i++)
	{
	List row = (List)items.get(i);
		if(combo){
		%>
		<option value="<%=row.get(0)%>"><%=row.get(0)%>&nbsp;&nbsp;&nbsp;&nbsp;<%=row.get(1)%><%if(displayPercentage.booleanValue()){%>&nbsp;&nbsp;&nbsp;&nbsp;<%=row.get(2)%><%}%></option>
		<%
		}else{
		%>
		<tr>
			<td align="left"><a href="durable_inventory.do?<%=newQuery%>=<%=URLEncoder.encode((String)row.get(0),"UTF-8")%>"><%=row.get(0)%></a>  </td> 
			<td align="right"><%= row.get(1)%></td> 
			<%if(displayPercentage.booleanValue()){ %><td align="right"><%=row.get(2)%> </td><%} %> 
		</tr>
		<%} %>
<%  }
   if(combo)
	   {
	   %>
	   </select>
	   <%
	   Iterator it = hiddenFields.keySet().iterator();
	   while(it.hasNext())
		   {
		   String name = (String)it.next();
		   String value = ((String[])hiddenFields.get(name))[0];
	   %>
        <input type="hidden" name="<%=name%>" value="<%=value%>" >  
       	<% }%>
       	</form>
	   <%}%>        
        </td></tr>
</table>
