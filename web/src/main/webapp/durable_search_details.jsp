<%@ include file ="header.jsp" %>
<%@page import="com.freescale.stateStamper.model.entity.*"%>
<%@page import="java.util.*"%>
<jsp:useBean id="result" scope="request" type="java.util.Map"/>
<h1>Search <%=equipmentType.name%> Details</h1>
<a href="durable_main.do">Back to Durable Main page</a>
<form id="searchForm" name="searchForm" method="get" action="durable_search_list.do">  
<%
  	Map  search = (Map) result.get("searchFieldsCatalog");
  	List types =  (List)search.get("type");
  	List substates = (List) search.get("SUBSTATE");
  	List<IStatus> machines = (List) result.get("durables");
  	List displayFields = (List)result.get("searchFields");
  	List<String> stateColors = (List)result.get("stateColors");
  	int resultsSize = machines.size();
  	String durableNameParam = request.getParameter("durable_name");
  	if(durableNameParam == null){durableNameParam = "";}
  	String typeParam = request.getParameter("type");
  	if(typeParam == null){typeParam = "";}
  	String substateParam = request.getParameter("SUBSTATE");
  	if(substateParam == null){substateParam = "";}
  	if(stateColors == null){stateColors = new ArrayList();}
%>
<table width=100%>
<tr>
<td width=50%><%=equipmentType.name%>  ID<br></td>
<td width=50%>
<input type="text" value="<%=durableNameParam%>" name="durable_name"> 
</td>
</tr>
<tr>
<td width=50%>
<%=equipmentType.name%> Type 
</td>
<td width=50%>
<select name="type">
	<option  value=""></option>
	<%	for (int i = 0; i < types.size(); i++)
			{
			Object item = types.get(i);
			if (item != null)
				{
				String selected = item.toString().equals(typeParam)?"selected":"";
	%>
	<option  value="<%=item.toString()%>" <%=selected%> ><%=item.toString()%></option>
	<%
		        }
			}
	%>
</select>
</td>
</tr>
<tr>
<td width=50%>
Status
</td>
<td width=50%>
<select name="SUBSTATE">
<option  value=""></option>
<%	for (int i = 0; i < substates.size(); i++)
		{
		Object item = substates.get(i);
		if (item != null)
			{
			String selected = item.toString().equals(substateParam)?"selected":"";
%>
<option  value="<%=item.toString()%>" <%=selected%> ><%=item.toString()%></option>
<%
	        }
		}
%>
</select>
</td>
</tr>
<tr>
<td width=50%>
</td>
<td width=50%>
<input type="submit" name="search" value="search">
</td>
</tr>
</table>
</form>

<%=machines.size()+ " " + equipmentType.name +"s found"  %> 
<table>
<tbody>
   <tr>
      <th><%=equipmentType.name%> Id</th>
      <%for(int i=1;i < displayFields.size();i++)
    	  {%>
     <th><%=displayFields.get(i)%></th>	  
    	<%}%>  
   </tr>
<%
for (int i = 0; machines != null && i < machines.size();i++)
	{
	IStatus status = machines.get(i);
	String stateColor = "";
	if(i < stateColors.size()){stateColor = stateColors.get(i);}
	
%>
	<tr>
		<td><a href="durable_details.do?durable_id=<%=status.get("mESName")%>"><%=status.get("mESName")%></a></td>
		<%for(int j=1;j < displayFields.size();j++)
    	  {
    	  String field = displayFields.get(j).toString();
    	  String cellColor ="";
    	  if(field.equals("SUBSTATE") && !stateColor.equals("")){cellColor = "bgcolor=\""+stateColor+"\"";}
    	  %>
		<td <%=cellColor%> ><%=status.get(field) %> </td>
		<%}%>
	</tr>
<%
	}
%>
</tbody></table>
<%@ include file ="footer.html" %>