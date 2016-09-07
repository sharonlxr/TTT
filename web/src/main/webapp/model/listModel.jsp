	<%@ include file ="/model/model_header.jsp" %>
	<div style="position: relative;left:160px">
		<h1>Model Management - list</h1>
		<a href="<%=request.getContextPath() %>/durable_main.do">Back to Durable Main page</a>
	</div>
<form id="searchForm" name="searchForm" method="post" action="<%=request.getContextPath() %>/listModel.action">
	<s:if test="errorMessage!=null">
		<div style="color:red;position: relative;left:160px"><s:property value="errorMessage" /></div>
	</s:if>
	<table width=100% style="position: relative;left:160px" id="searchFilterTable">
		<tr>
			<td width="50%">Model Name</td>
			<td width="50%"><s:textfield theme="simple" name="modelVO.searchValues['modelName']" id="modelName" /></td>
		</tr>
		<tr>
			<td width="50%">Platform</td>
			<td width="50%"><s:select theme="simple" name="modelVO.searchValues['platform']" id="platform" list="modelVO.platformSet" headerKey="" headerValue=""></s:select></td>
		</tr>
		<tr>
			<td width="50%">Engineer Contact</td>
			<td width="50%"><s:select theme="simple" name="modelVO.searchValues['engineerContact']" id="engineerContact" list="modelVO.engineerContactSet" headerKey="" headerValue=""></s:select></td>
		</tr>
		<tr>
			<td width="50%">Status</td>
			<td width="50%"><s:select theme="simple" name="modelVO.searchValues['status']" id="status" list="modelVO.statusSet" headerKey="" headerValue=""></s:select></td>
		</tr>
		<tr>
			<td width="50%">Created By</td>
			<td width="50%"><s:select theme="simple" name="modelVO.searchValues['creator']" id="creator" list="modelVO.creatorSet" headerKey="" headerValue=""></s:select></td>
		</tr>		
		<s:iterator  value="modelVO.searchAttributes" id="item" status="st">
			<tr>
				<td width="50%"><s:property value="item" /></td>
				<td width="50%"><s:select theme="simple" id="%{#item}" name="modelVO.searchValues['%{#item}']"  list="modelVO.searchDropDownValues[#item]" headerKey="" headerValue="" /></td>
			</tr>
		</s:iterator>
		<tr>
			<td width="50%"></td>
			<td width="50%"><input type="button" name="search" value="search" onclick="searchModel();" />
			</td>
		</tr>
	</table>
	<table style="position: relative;left:160px">
		<tr>	
			<th>Index</th>
			<th>Model Name</th>
			<th>Model Description</th>
			<th>Area</th>
			<th>Equipment Type</th>
			<th>Platform</th>
			<th>Engineer Contact</th>
			<th>Status</th>
			<th>Creator</th>
		</tr>
		<s:iterator value="modelList" id="item" status="st">
		<tr>
			<td><a href="<%=request.getContextPath() %>/showModel.action?modelVO.primaryKey=<s:property value='#item.primaryKey' />"><s:property value="%{#st.index+1}" /></a></td>
			<td><s:property value='#item.modelName' /></td>
			<td><s:property value='#item.modelDescription' /></td>
			<td><s:property value='#item.area.name' /></td>
			<td><s:property value='#item.equipmentType.name' /></td>
			<td><s:property value='#item.platform' /></td>
			<td><s:property value='#item.engineerContact' /></td>
			<td><s:property value='#item.status' /></td>
			<td><s:property value='#item.creator' /></td>
		</tr>
		</s:iterator>
	</table>
	<div style="position: relative;left:160px">
		<s:if test="modelVO.creatorRoleList.contains(modelVO.userRole)">
			<input type="button" name="" formaction="" value="Create New" onclick="location.href='<%=request.getContextPath() %>/createModel.action'" />
		</s:if>
	    <br><br><a href="index.jsp">Home page</a>
	</div>
	<input type="hidden" name="modelVO.searchAttributesJSON" id="searchAttributesJSON"/>
</form>
	<script language="JavaScript">
		    POPI = 'FIUO';
		    WriteFooter();
	</script>
</body>
</html>