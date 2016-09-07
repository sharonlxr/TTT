<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<%@page import="com.freescale.stateStamper.model.entity.Role" %>
<%@page import="com.freescale.stateStamper.model.entity.EquipmentType" %>
<%@page import="com.freescale.stateStamper.util.TTTUtil" %>
<%@page import="java.util.*"%>
<html>
<head>	
</head>
<body>
<b>Comments<b> 
<table width="100%">
<tr>
<td width="15%">
<b>User<b>
</td>
<td width="15%">
<b>Time<b>
</td>
<td width="70%">
<b>Content<b>
</td>
</tr>

<%
	if (status.get("COMMENTS")!=null)
	{
	Set<String> tmpSet = ((Map)status.get("COMMENTS")).keySet();
	List<String> tmpList = new ArrayList<String>(tmpSet);
	Collections.sort(tmpList);
	boolean even = false;
	int linecounter = 0;
	for (String key: tmpList)
		{	
		linecounter = linecounter + 1;
		even = linecounter % 2 == 0;		
		List comment = (List)(status.COMMENTS.get(key));	
		if (even){
%><tr><%
	}
		else
	{
%>
			<tr bgcolor = #eeeeee>
			<%
				}
						
					String coreid = (String)comment.get(0);
					String temp = TTTUtil.getUserFullName(coreid);
					if(temp!=null)
						coreid = temp;
					String time = (String)comment.get(1);
					String content = (String)comment.get(2);
			%>
		<td><%=coreid%></td>
		<td><%=time%></td>
		<td style="white-space: normal"><%=content%></td>
</tr>
<% 
	}}
%>
</table>
