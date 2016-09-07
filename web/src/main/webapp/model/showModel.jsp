		<%@ include file ="/model/model_header.jsp" %>
		<div>
			<h1>Model Details</h1>
			<a href="durable_main.do">Back to Durable Main page</a>
			<br>
			<a href="listModel.action">Back to Model List page</a>
		</div>
		<s:if test="errorMessage!=null">
			<div style="color:red;position: relative;left:160px"><s:property value="errorMessage" /></div>
		</s:if>
		<br/><h2>Model Fields</h2>
		<table  class="durabledetail" id="modelFieldsTable">
			<tr>
				<td class="durabledetail"><b>Model Name</b></td>
				<td class="durabledetail"><s:property value="model.modelName" /></td>
				<td class="durabledetail"><b>Model Description</b></td>
				<td class="durabledetail"><s:property value="model.modelDescription" /></td>
			</tr>
			<tr>
				<td class="durabledetail"><b>Equipment Type</b></td>
				<td class="durabledetail"><s:property value="model.equipmentType.name" /></td>
				<td class="durabledetail"><b>Area</b></td>
				<td class="durabledetail"><s:property value="model.area.name" /></td>
			</tr>
			<tr>
				<td class="durabledetail"><b>Platform</b></td>
				<td class="durabledetail"><s:property value="model.platform" /></td>
				<td class="durabledetail"><b>Engineer Contact</b></td>
				<td class="durabledetail"><s:property value="model.engineerContact" /></td>
			</tr>
			<tr>
				<td class="durabledetail"><b>Vendor</b></td>
				<td class="durabledetail"><s:property value="model.vendor" /></td>
				<td class="durabledetail"><b>Status</b></td>
				<td class="durabledetail"><s:property value="model.status" /></td>
			</tr>
			<tr>
				<td class="durabledetail"><b>Creator</b></td>
				<td class="durabledetail"><s:property value="model.creator" /></td>
				<td class="durabledetail"><b>Create Date</b></td>
				<td class="durabledetail"><s:property value="model.createDate" /></td>
			</tr>
			<s:if test="modelVO.approvalType==3||modelVO.approvalType==2">
			<tr>
				<td class="durabledetail"><b>Approver</b></td>
				<td class="durabledetail"><s:property value="model.approver" /></td>
				<td class="durabledetail"><b>Approve Date</b></td>
				<td class="durabledetail"><s:property value="model.approveDate" /></td>
			</tr>
			</s:if>
			<s:if test="modelVO.approvalType==3">
			<tr>
				<td class="durabledetail"><b>Releaser</b></td>
				<td class="durabledetail"><s:property value="model.releaser" /></td>
				<td class="durabledetail"><b>Release Date</b></td>
				<td class="durabledetail"><s:property value="model.releaseDate" /></td>
			</tr>
			</s:if>
			<tr>
				<td class="durabledetail"><b>Last Update By</b></td>
				<td class="durabledetail"><s:property value="model.lastUpdateBy" /></td>
				<td class="durabledetail"><b>Last Update Date</b></td>
				<td class="durabledetail"><s:property value="model.lastUpdateDate" /></td>
			</tr>
			<tr>
				<td class="durabledetail"><b>MfrCode</b></td>
				<td class="durabledetail"><s:property value="model.mfrCode" /></td>
				<td class="durabledetail"><b>&nbsp;</b></td>
				<td class="durabledetail">&nbsp;</td>
			</tr>
		</table>
		<br/><h2>Model Attributes</h2>
		<table  class="durabledetail" id="modelAttributesTable">
			<s:iterator value="modelVO.modelAttributes" id="item" status="st">
				<s:if test="#st.odd">
					<tr>
						<td class="durabledetail"><b><s:property value="#item.name" /></b></td>
						<td class="durabledetail"><s:property value="model.modelAttributes[#item.name]" /></td>
						<s:if test="#st.last">
							<td class="durabledetail">&nbsp;</td>
							<td class="durabledetail">&nbsp;</td>
						</tr>
						</s:if>
				</s:if>
				<s:else>
						<td class="durabledetail"><b><s:property value="#item.name" /></b></td>
						<td class="durabledetail"><s:property value="model.modelAttributes[#item.name]" /></td>
					</tr>
				</s:else>
			</s:iterator>
		</table>
		<s:if test="modelVO.modelMappingEnabled">
			<br/><h2>Model Mapping</h2>
			<table  class="durabledetail" id="modelMappingTable">
				<s:iterator value="modelVO.childrenModelMap" status="st">
					<s:if test="#st.odd">
						<tr>
							<td class="durabledetail"><b><s:property value="key" /></b></td>
							<td class="durabledetail"><s:property value="value" /></td>
							<s:if test="#st.last">
								<td class="durabledetail">&nbsp;</td>
								<td class="durabledetail">&nbsp;</td>
							</tr>
							</s:if>
					</s:if>
					<s:else>
							<td class="durabledetail"><b><s:property value="key" /></b></td>
							<td class="durabledetail"><s:property value="value" /></td>
						</tr>
					</s:else>
				</s:iterator>
			</table>
		</s:if>
		<br/><h2>Dynamic Attributes</h2>
		<table  class="durabledetail" id="dynamicAttributesTable">
			<s:iterator value="modelVO.dynamicAttributes" id="item" status="st">
				<s:if test="#st.odd">
					<tr>
						<td class="durabledetail"><b><s:property value="#item.name" /></b></td>
						<td class="durabledetail"><s:property value="model.dynamicAttributes[#item.name]" /></td>
						<s:if test="#st.last">
							<td class="durabledetail">&nbsp;</td>
							<td class="durabledetail">&nbsp;</td>
						</tr>
						</s:if>
				</s:if>
				<s:else>
						<td class="durabledetail"><b><s:property value="#item.name" /></b></td>
						<td class="durabledetail"><s:property value="model.dynamicAttributes[#item.name]" /></td>
					</tr>
				</s:else>
			</s:iterator>
		</table>
		<s:if test="model.status=='ACTIVE'">
		<br/><h2>Durable List Belong To The Model</h2>
			<s:if test="!modelVO.durableList.isEmpty()">
			<table class="durabledetail" id="childDurable">
				<s:iterator value="modelVO.durableList" id="item" status="st">
					<s:if test="#st.odd">
						<tr>
							<td class="durabledetail">
								<a target="_blank" href="durable_details.do?durable_id=<s:property value='#item.mESName' />"><s:property value="#item.mESName" /></a>
							</td>
							<s:if test="#st.last">
								<td class="durabledetail">&nbsp;</td>
							</tr>
							</s:if>
					</s:if>
					<s:else>
							<td class="durabledetail">
								<a target="_blank" href="durable_details.do?durable_id=<s:property value='#item.mESName' />"><s:property value="#item.mESName" /></a>
							</td>
						</tr>
					</s:else>
				</s:iterator>
			</table>
			</s:if>
			<s:else>
				N/A
			</s:else>
		</s:if>
		<br/>
		<div  style="float:left">
			<s:if test="modelVO.approvalType==3">
				<s:if test="model.status=='PENDING'">
					<s:if test="model.creator==#session.user.coreId||modelVO.approverRoleList.contains(modelVO.userRole)||modelVO.releaserRoleList.contains(modelVO.userRole)">
						<input type="button" value="Update" onclick="location.href='<%=request.getContextPath() %>/editModel.action?modelVO.primaryKey=<s:property value="model.primaryKey" />'"/>
						<input type="button" value="Delete" onclick="location.href='<%=request.getContextPath() %>/deleteModel.action?modelVO.primaryKey=<s:property value="model.primaryKey" />'"/>
					</s:if>
					<s:if test="modelVO.approverRoleList.contains(modelVO.userRole)">
						<input type="button" value="Approve" onclick="location.href='<%=request.getContextPath() %>/approveModel.action?modelVO.primaryKey=<s:property value="model.primaryKey" />'"/>
						<input type="button" value="Reject" onclick="location.href='<%=request.getContextPath() %>/rejectModel.action?modelVO.primaryKey=<s:property value="model.primaryKey" />'"/>
					</s:if>
				</s:if>
				<s:if test="model.status=='APPROVED'">
					<s:if test="modelVO.approverRoleList.contains(modelVO.userRole)||modelVO.releaserRoleList.contains(modelVO.userRole)">
						<input type="button" value="Update" onclick="location.href='<%=request.getContextPath() %>/editModel.action?modelVO.primaryKey=<s:property value="model.primaryKey" />'"/>
					</s:if>
					<s:if test="modelVO.releaserRoleList.contains(modelVO.userRole)">
						<input type="button" value="Trigger Alert Email" onclick="location.href='<%=request.getContextPath() %>/triggerAlertEmail.action?modelVO.primaryKey=<s:property value="model.primaryKey" />'"/>
						<input type="button" value="Release" onclick="location.href='<%=request.getContextPath() %>/releaseModel.action?modelVO.primaryKey=<s:property value="model.primaryKey" />'"/>
					</s:if>
				</s:if>
				<s:if test="model.status=='REJECTED'">
					<s:if test="model.creator==#session.user.coreId||modelVO.approverRoleList.contains(modelVO.userRole)||modelVO.releaserRoleList.contains(modelVO.userRole)">
						<input type="button" value="Update" onclick="location.href='<%=request.getContextPath() %>/editModel.action?modelVO.primaryKey=<s:property value="model.primaryKey" />'"/>
						<input type="button" value="Delete" onclick="location.href='<%=request.getContextPath() %>/deleteModel.action?modelVO.primaryKey=<s:property value="model.primaryKey" />'"/>
					</s:if>
				</s:if>
				<s:if test="model.status=='ACTIVE'">
					<s:if test="modelVO.approverRoleList.contains(modelVO.userRole)||modelVO.releaserRoleList.contains(modelVO.userRole)">
						<input type="button" value="Update" onclick="location.href='<%=request.getContextPath() %>/editModel.action?modelVO.primaryKey=<s:property value="model.primaryKey" />'"/>
						<input type="button" value="Retire" onclick="location.href='<%=request.getContextPath() %>/retireModel.action?modelVO.primaryKey=<s:property value="model.primaryKey" />'"/>
					</s:if>
				</s:if>
				<s:if test="model.status=='RETIRED'">
					<s:if test="modelVO.approverRoleList.contains(modelVO.userRole)||modelVO.releaserRoleList.contains(modelVO.userRole)">
						<input type="button" value="Update" onclick="location.href='<%=request.getContextPath() %>/editModel.action?modelVO.primaryKey=<s:property value="model.primaryKey" />'"/>
						<input type="button" value="Delete" onclick="location.href='<%=request.getContextPath() %>/deleteModel.action?modelVO.primaryKey=<s:property value="model.primaryKey" />'"/>
						<input type="button" value="Activate" onclick="location.href='<%=request.getContextPath() %>/activateModel.action?modelVO.primaryKey=<s:property value="model.primaryKey" />'"/>
					</s:if>
				</s:if>
			</s:if>
			<s:elseif test="modelVO.approvalType==2">
				<s:if test="model.status=='PENDING'">
					<s:if test="model.creator==#session.user.coreId||modelVO.approverRoleList.contains(modelVO.userRole)||modelVO.releaserRoleList.contains(modelVO.userRole)">
						<input type="button" value="Update" onclick="location.href='<%=request.getContextPath() %>/editModel.action?modelVO.primaryKey=<s:property value="model.primaryKey" />'"/>
						<input type="button" value="Delete" onclick="location.href='<%=request.getContextPath() %>/deleteModel.action?modelVO.primaryKey=<s:property value="model.primaryKey" />'"/>
					</s:if>
					<s:if test="modelVO.approverRoleList.contains(modelVO.userRole)">
						<input type="button" value="Approve" onclick="location.href='<%=request.getContextPath() %>/approveModel.action?modelVO.primaryKey=<s:property value="model.primaryKey" />'"/>
						<input type="button" value="Reject" onclick="location.href='<%=request.getContextPath() %>/rejectModel.action?modelVO.primaryKey=<s:property value="model.primaryKey" />'"/>
					</s:if>
				</s:if>
				<s:if test="model.status=='REJECTED'">
					<s:if test="model.creator==#session.user.coreId||modelVO.approverRoleList.contains(modelVO.userRole)||modelVO.releaserRoleList.contains(modelVO.userRole)">
						<input type="button" value="Update" onclick="location.href='<%=request.getContextPath() %>/editModel.action?modelVO.primaryKey=<s:property value="model.primaryKey" />'"/>
						<input type="button" value="Delete" onclick="location.href='<%=request.getContextPath() %>/deleteModel.action?modelVO.primaryKey=<s:property value="model.primaryKey" />'"/>
					</s:if>
				</s:if>
				<s:if test="model.status=='ACTIVE'">
					<s:if test="modelVO.approverRoleList.contains(modelVO.userRole)||modelVO.releaserRoleList.contains(modelVO.userRole)">
						<input type="button" value="Update" onclick="location.href='<%=request.getContextPath() %>/editModel.action?modelVO.primaryKey=<s:property value="model.primaryKey" />'"/>
						<input type="button" value="Retire" onclick="location.href='<%=request.getContextPath() %>/retireModel.action?modelVO.primaryKey=<s:property value="model.primaryKey" />'"/>
					</s:if>
				</s:if>
				<s:if test="model.status=='RETIRED'">
					<s:if test="modelVO.approverRoleList.contains(modelVO.userRole)||modelVO.releaserRoleList.contains(modelVO.userRole)">
						<input type="button" value="Update" onclick="location.href='<%=request.getContextPath() %>/editModel.action?modelVO.primaryKey=<s:property value="model.primaryKey" />'"/>
						<input type="button" value="Delete" onclick="location.href='<%=request.getContextPath() %>/deleteModel.action?modelVO.primaryKey=<s:property value="model.primaryKey" />'"/>
						<input type="button" value="Activate" onclick="location.href='<%=request.getContextPath() %>/activateModel.action?modelVO.primaryKey=<s:property value="model.primaryKey" />'"/>
					</s:if>
				</s:if>
			</s:elseif>
			<s:elseif test="modelVO.approvalType==1">
				<s:if test="model.status=='ACTIVE'">
					<s:if test="modelVO.approverRoleList.contains(modelVO.userRole)||modelVO.releaserRoleList.contains(modelVO.userRole)">
						<input type="button" value="Update" onclick="location.href='<%=request.getContextPath() %>/editModel.action?modelVO.primaryKey=<s:property value="model.primaryKey" />'"/>
						<input type="button" value="Retire" onclick="location.href='<%=request.getContextPath() %>/retireModel.action?modelVO.primaryKey=<s:property value="model.primaryKey" />'"/>
					</s:if>
				</s:if>
				<s:if test="model.status=='RETIRED'">
					<s:if test="modelVO.approverRoleList.contains(modelVO.userRole)||modelVO.releaserRoleList.contains(modelVO.userRole)">
						<input type="button" value="Update" onclick="location.href='<%=request.getContextPath() %>/editModel.action?modelVO.primaryKey=<s:property value="model.primaryKey" />'"/>
						<input type="button" value="Delete" onclick="location.href='<%=request.getContextPath() %>/deleteModel.action?modelVO.primaryKey=<s:property value="model.primaryKey" />'"/>
						<input type="button" value="Activate" onclick="location.href='<%=request.getContextPath() %>/activateModel.action?modelVO.primaryKey=<s:property value="model.primaryKey" />'"/>
					</s:if>
				</s:if>
			</s:elseif>
			<br><br><a href="index.jsp">Home page</a>
		</div>
		<script language="JavaScript">
		    POPI = 'FIUO';
		    WriteFooter();
		</script>
	</body>
</html>
