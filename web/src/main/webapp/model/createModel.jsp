	<%@ include file ="/model/model_header.jsp" %>
	<div>
		<h1>Create Model</h1>
		<a href="durable_main.do">Back to Durable Main page</a>
		<br>
		<a href="listModel.action">Back to Model List page</a>
	</div>
	<s:if test="errorMessage!=null">
		<div style="color:red;position: relative;left:160px"><s:property value="errorMessage" /></div>
	</s:if>
	<form name="modelForm" id="modelForm" method="post" action="<%=request.getContextPath()%>/saveModel.action">
		<h2>Model Fields</h2>
		<table  class="durabledetail" id="modelFieldsTable">
			<tr>
				<td class="durabledetail"><b>Model Name</b><b style="color:red">*</b></td>
				<td class="durabledetail">
					<s:if test="modelVO.autoModelName==true">
						AUTO GENERATION
					</s:if>
					<s:else>
						<s:textfield required="required" theme="simple" name="modelVO.modelName" id="modelVO.modelName" style="width:260px"/>
					</s:else>
				</td>
				<td class="durabledetail"><b>Model Description</b><b style="color:red">*</b></td>
				<td class="durabledetail">
					<s:if test="modelVO.autoModelDescription==true">
						AUTO GENERATION
					</s:if>
					<s:else>
						<s:textfield required="required" theme="simple" name="modelVO.modelDescription" id="modelVO.modelDescription" style="width:260px"/>
					</s:else>
				</td>
			</tr>
			<tr>
				<td class="durabledetail"><b>Area</b><b style="color:red">*</b></td>
				<td class="durabledetail"><s:property value="#session.area.name" /></td>
				<td class="durabledetail"><b>Equipment Type</b><b style="color:red">*</b></td>
				<td class="durabledetail"><s:property value="#session.equipmentType.name" /></td>
			</tr>
			<tr>
				<td class="durabledetail"><b>Platform</b><b style="color:red">*</b></td>
				<td class="durabledetail">
					<s:if test="modelVO.dropdownValues.platform">
						<s:select id="platform" name="modelVO.platform" theme="simple" list="modelVO.dropdownValues.platform" style="width:260px"/>
					</s:if>
					<s:else>
						<s:textfield required="required" theme="simple" name="modelVO.platform" id="modelVO.platform" style="width:260px"/>
					</s:else>
				</td>
				<td class="durabledetail"><b>Vendor</b></td>
				<td class="durabledetail">
					<s:if test="modelVO.dropdownValues.vendor">
						<s:select id="vendor" name="modelVO.vendor" theme="simple" list="modelVO.dropdownValues.vendor" style="width:260px"/>
					</s:if>
					<s:else>
						<s:textfield theme="simple" name="modelVO.vendor"  id="modelVO.vendor" style="width:260px"/>
					</s:else>
				</td>
			</tr>
			<s:if test='#session.equipmentType.syncModelFlag=="Y"' >
			<tr>
				<td class="durabledetail"><b>MfrCode</b><b style="color:red">*</b></td>
				<td class="durabledetail">
					<s:if test="modelVO.genesisValues.mfrCode">
						<s:select id="mfrCode" name="modelVO.mfrCode" theme="simple" list="modelVO.genesisValues.mfrCode" style="width:260px"/>
					</s:if>
					<s:else>
						<s:textfield required="required" theme="simple" name="modelVO.mfrCode" id="modelVO.mfrCode" style="width:260px"/>
					</s:else>
				</td>
				<td class="durabledetail"><b>&nbsp;</b></td>
				<td class="durabledetail">&nbsp;</td>
			</tr>
			</s:if>
		</table>
		<h2>Model Attributes</h2>
		<table class="durabledetail" id="modelAttributesTable">
			<s:iterator value="modelVO.modelAttributes" id="item" status="st">
				<s:if test="#st.odd">
					<tr>
						<td class="durabledetail">
							<s:property value="#item.name" />
							<s:if test="#item.required">
								<b style="color:red">*</b>
							</s:if>
						</td>
						<td class="durabledetail">
							<s:if test="%{#item.valueList}">
								<s:select id="%{#item.name}" name="%{#item.name}" theme="simple" list="%{#item.valueList}" style="width:260px"/>
							</s:if>
							<s:else>
								<s:if test="#item.required">
									<s:textfield required="required" theme="simple" name="%{#item.name}" id="%{#item.name}" style="width:260px"/>
								</s:if>
								<s:else>
									<s:textfield theme="simple" name="%{#item.name}" id="%{#item.name}" style="width:260px"/>
								</s:else>
							</s:else>
						</td>
						<s:if test="#st.last">
							<td class="durabledetail">&nbsp;</td>
							<td class="durabledetail">&nbsp;</td>
						</tr>
						</s:if>
				</s:if>
				<s:else>
						<td class="durabledetail">
							<s:property value="#item.name" />
							<s:if test="#item.required">
								<b style="color:red">*</b>
							</s:if>
						</td>
						<td class="durabledetail">
							<s:if test="%{#item.valueList}">
								<s:select id="%{#item.name}" name="%{#item.name}" theme="simple" list="%{#item.valueList}" style="width:260px"/>
							</s:if>
							<s:else>
								<s:if test="#item.required">
									<s:textfield required="required" theme="simple" name="%{#item.name}" id="%{#item.name}" style="width:260px"/>
								</s:if>
								<s:else>
									<s:textfield theme="simple" name="%{#item.name}" id="%{#item.name}" style="width:260px"/>
								</s:else>
							</s:else>
						</td>
					</tr>
				</s:else>
			</s:iterator>
		</table>
		<br/>
		<s:if test="modelVO.modelMappingEnabled">
			<h2>Model Mapping</h2>
			<table class="durabledetail" id="modelMappingTable">
				<s:iterator value="modelVO.childrenModelListMap" status="st">
					<s:if test="#st.odd">
						<tr>
							<td class="durabledetail">
								<s:property value="key"/>
							</td>
							<td class="durabledetail">
								<s:select name="modelVO.childrenModelList" listKey="primaryKey" listValue="modelName" theme="simple" list="value" style="width:260px"/>
							</td>
							<s:if test="#st.last">
								<td class="durabledetail">&nbsp;</td>
								<td class="durabledetail">&nbsp;</td>
							</tr>
							</s:if>
					</s:if>
					<s:else>
							<td class="durabledetail">
								<s:property value="key"/>
							</td>
							<td class="durabledetail">
								<s:select name="modelVO.childrenModelList" listKey="primaryKey" listValue="modelName" theme="simple" list="value" style="width:260px"/>
							</td>
						</tr>
					</s:else>
				</s:iterator>
			</table>
			<br/>
		</s:if>
		<s:if test="modelVO.approvalType!=3">
			<h2>Dynamic Attributes</h2>
			<table class="durabledetail" id="dynamicAttributesTable">
				<s:iterator value="modelVO.dynamicAttributes" id="item" status="st">
					<s:if test="#st.odd">
						<tr>
							<td class="durabledetail"><s:property value="#item.name" /></td>
							<td class="durabledetail">
								<s:if test="modelVO.dropdownValues[#item.name]">
									<s:select id="%{#item.name}" name="%{#item.name}" theme="simple" list="modelVO.dropdownValues[#item.name]" style="width:260px"/>
								</s:if>
								<s:else>
									<s:if test="%{#item.type=='Counter'}">
										<s:textfield  theme="simple" step="0.01" type="number" name="%{#item.name}" id="%{#item.name}" style="width:260px"/>
									</s:if>
									<s:else>
										<s:textfield theme="simple" name="%{#item.name}" id="%{#item.name}" style="width:260px"/>
									</s:else>
								</s:else>
							</td>
							<s:if test="#st.last">
								<td class="durabledetail">&nbsp;</td>
								<td class="durabledetail">&nbsp;</td>
							</tr>
							</s:if>
					</s:if>
					<s:else>
							<td class="durabledetail"><s:property value="#item.name" /></td>
							<td class="durabledetail">
								<s:if test="modelVO.dropdownValues[#item.name]">
									<s:select id="%{#item.name}" name="%{#item.name}" theme="simple" list="modelVO.dropdownValues[#item.name]" style="width:260px"/>
								</s:if>
								<s:else>
									<s:if test="%{#item.type=='Counter'}">
										<s:textfield theme="simple" step="0.01" type="number" name="%{#item.name}" id="%{#item.name}" style="width:260px"/>
									</s:if>
									<s:else>
										<s:textfield theme="simple" name="%{#item.name}" id="%{#item.name}" style="width:260px"/>
									</s:else>
								</s:else>
							</td>
						</tr>
					</s:else>
				</s:iterator>
			</table>
		</s:if>
		<div style="float:left">
			<s:if test="modelVO.approvalType==3">
				<input type="submit" name="saveModel" value="Save" onclick="return submitModel(true,false);"/>
			</s:if>
			<s:else>
				<input type="submit" name="saveModel" value="Save" onclick="return submitModel(true,true);"/>
			</s:else>
		</div>
		<input type="hidden" name="modelVO.modelAttributesJSON" id="modelAttributesJSON"/>
		<input type="hidden" name="modelVO.dynamicAttributesJSON" id="dynamicAttributesJSON"/>
	</form>
	<br><br><a href="index.jsp">Home page</a>
	<script language="JavaScript">
		    POPI = 'FIUO';
		    WriteFooter();
	</script>
</body>
</html>