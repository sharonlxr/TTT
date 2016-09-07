<%@ include file ="header.jsp" %>
<%@page import="com.freescale.stateStamper.model.entity.*"%>
<%@page import="java.util.*"%>
<jsp:useBean id="result" scope="request" type="java.util.Map"/>
<h1>Durable Attach Child <%=site%></h1>
<%
    String durableId = (String)result.get("durableId");
    Machine parent = (Machine)result.get("durable");
    List attachableChildren = (List)result.get("attachableChildren");
    Integer childrenNumber = (Integer)result.get("childrenNumber");
    if(childrenNumber == null){childrenNumber = Integer.valueOf(0);}
    String childrenType = (String)result.get("childrenType");
    String indexVar = (String)result.get("indexVar");
     Map<String,Machine>  currentChildren = (Map)result.get("currentChildren");
     List<Machine> childrens = new ArrayList(currentChildren.values());
    List<String> currentChildenIds = parent.getChildrenByEquipmentType(childrenType);
    List attachRule = equipmentType.getBusinessRulesBy(BusinessRule.ATTACH_CHILD);
    BusinessRuleAction checkboxAction = null;
    if(attachRule != null && attachRule.size() > 0){
    	BusinessRule br = (BusinessRule)attachRule.get(0);
    	if(br.getActions()!=null&&br.getActions().size()>0){
    		List bractions = br.getActionsBy(BusinessRuleAction.ADD_VERIFICATION_CHECKBOX);
    		if(bractions!=null&&bractions.size()>0){
    			checkboxAction = (BusinessRuleAction)bractions.get(0);
    		}
    	}
    }
%>
<script >
function submitDurablesList()
{
	var childrenId = [];
	var name;
	var value;
	for(i=0; i<document.childrenForm.elements.length; i++)
	   {
		name = document.childrenForm.elements[i].name;
		if(name.indexOf('child_')==0)
		   {
			value = document.childrenForm.elements[i].value;
			if(value == ''){continue;}
			var found = childrenId.indexOf(value) != -1; 
			if(found)
				{
				alert(value + ' has been selected more than 1');
				return false;
				}
			else
				{
				childrenId.push(value);
				}
		   }
		}
	return true;
}
function changeColor(i){
	var checkvalue = $("#highlight_"+i).prop("checked");
	if(checkvalue){
		$('#child_'+i).css("background","#ff0000");
	} else {
		$('#child_'+i).css("background","#ffffff");
	}
}
</script>
<a href="durable_main.do">Back to Durable Main Page</a>
<br>
<a href="durable_details.do?durable_id=<%=durableId%>">Back to Durable Details Page</a>
<h2><%=equipmentType.name%> <%=durableId%></h2>  
<br/>Select children to attach
<form id="childrenForm" name="childrenForm" method="post" action="durable_attach_children.do" onsubmit="return submitDurablesList();">
<input type="hidden" name="parent_id" value="<%=durableId%>" >    
<table width="100%">
<%for(int i=0;i<childrenNumber;i++)
	{
	String optionSelected = "";
	boolean selected  = false;
	
	%>
	<tr>
		<th  >
		<%=childrenType%> <%=i%>
		</th>
		<td >
		<select size="1" name="child_<%=i%>" id="child_<%=i%>" >
		<option> </option>
		<%
			if(attachableChildren != null)
			for(Object child :attachableChildren)
				{
				if(!selected)
					{
						if (currentChildenIds.contains(child))
							{
							if (indexVar != null)
								{
								Machine childObj = currentChildren.get(child.toString());
								Object idx = childObj.get(indexVar);
								if (idx == null) idx = "";
								if (idx.equals(String.valueOf(i)))
									{
									optionSelected = "selected";
									currentChildenIds.remove(child);
									childrens.remove(childObj);
									selected = true;
									}
								}
							else
								{
								optionSelected = "selected";
								currentChildenIds.remove(child);
								selected = true;
								}

							}
						}
		%>
		    <option value="<%=child%>" <%=optionSelected%> > <%=child%> </option>
		    <%
		    optionSelected = "";
		        }%>
		</select>
		<%if(checkboxAction!=null) {%>
		<input name="checkGroup" type="checkbox" id="highlight_<%=i%>" onclick="changeColor('<%=i%>')" />
		<%} %>
		</td>
	</tr>
 <% } %>
	<tr>
	<td colspan="2"  ><input type="submit" value="attach" > </td>
	</tr>
</table>
</form>

<%@ include file ="footer.html" %>