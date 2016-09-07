<%@page import="com.freescale.stateStamper.model.entity.*"%>
<%@page import="java.util.*"%>
<%@page import="java.math.*"%>
<jsp:useBean id="result" scope="request" class="java.util.ArrayList" />
<script>
	setTimeout(function(){window.location.reload(1);}, 300000); 
</script>
<html>
<head> <title>Durables by Tester </title> 
<link href="http://swo.freescale.net/css/fsl.css" rel="stylesheet" type="text/css">
<link rel="stylesheet" type="text/css" href="css/tttmain.css"/>
<link rel="icon" href="images/ttt.ico"  type="image/x-icon"/>
</head>

<body>
<h2>Durables by Tester </h2>
	<%
	String tester = request.getParameter("hostname"); 
		if ( tester == null)
			{
	%>
	<form id="form" name="form" method="get" action="results_tester_attached_durable.do">
		<table width=100%>
			<tr>
				<td width=50%>Tester ID</td>
				<td width=50%><input type="text" value="" name="hostname" autofocus="autofocus"></td>
			</tr>
			<tr>
				<td width=50%></td>
				<td width=50%><input type="submit" name="next" value="next">
				</td>
			</tr>
		</table>
	</form>

	<%
		}
		else
			{
	       if(result.isEmpty() ){
	       out.println("<h3 align=\"center\" >"+tester + " not found</h3>");
	       }
	       else if(result.size() == 1){
	       out.println("<h3 align=\"center\" >No durables attached to " + tester +"</h3>");
	       }else{
	       
	%>
	<table width="100%">
		<tr>
			<th width="100%">Tester :<%=tester%></th>
		<tr>
		<tbody>
			<%
				for (int i = 1; i < result.size(); i++)
				   {
				   String value = (String)result.get(i);
				   String textAttributes="";
				   if(value.contains("%"))
					   {
					   String percent = value.substring(0,value.indexOf("%"));
					   if(percent.contains(".")){percent = percent.substring(0,percent.indexOf("."));}
					   if(Integer.valueOf(percent) >= 80){value = "<h1><font color=\"FF0000\" size=\"7\"  >"+value+"</font></h1>";}
					   else                              {value = "<h1><font  size=\"7\"  >"+value+"</font></h1>";}
					   }
						
			%>
			<tr>
				<td width="100%" align="center"><%=value%></td>
			</tr>
			<%
				  }
			%>
		</tbody>
	</table>
	<%
		}
		}
	%>

</body>
</html>