package com.freescale.stateStamper.model.logic;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest; 
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import jxl.Workbook;
import jxl.write.Label;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;

import com.freescale.serverUtil.Persistence;
import com.freescale.serverUtil.ThreadSafeSimpleDateFormat;
import com.freescale.stateStamper.model.entity.*;

import org.apache.commons.fileupload.*;
import org.apache.commons.fileupload.disk.*;
import org.apache.commons.fileupload.servlet.*;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DurableManager {
    private static Logger logger;
	private static ThreadSafeSimpleDateFormat formatter = new ThreadSafeSimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private static final String TRANSITION_ERR_NOT_LATEST_STATUS = "Another transition has been performed on this item, please go to the details page and try again";
	private static final String TRANSITION_ERR_TRANSITION_UNDEFINED = "Transition not defined";
	private static final String TRANSITION_ERR_TRANSITION_DISABLED = "Transition not enabled";
	private static final String TRANSITION_ERR_MISSING_FIELD = " is required";
	private static final String TRANSITION_ERR_NO_PRIVILEGES = "You don't have authority to perform this transition";
	private static final String SYSTEM_ERROR = "System error, please contact administrator";
	private static final String BUSINESSRULE_EMAIL_CONTENT_PART1 = "There are violations on attributes for below durables:";
	private static final String BUSINESSRULE_EMAIL_CONTENT_PART2 = "Please check the attributes:";
	private static final String BUSINESSRULE_EMAIL_SUBJECT = "Durable Management warning on";
	private static final String BUSINESSRULE_EMAIL_WARNING = "The information contained in this email has been classified Freescale Confidential Proprietary";
	public static final String TRANSITION_FIELD_DISPLAY = "display";
	public static final String TRANSITION_FIELD_VALUE = "value";
	public static final String TRANSITION_FIELD_STATUS = "status";
	public static final String TRANSITION_FIELD_NAME = "NAME";
	public static final String TRANSITION_FIELD_LABEL = "label";
	public static final String TRANSITION_FIELD_NOATTACHMENT = "no attachment";
	public static final String TRANSITION_FIELD_CHECKNAME = "check name";
	public static final String TRANSITION_PERCENTAGE_STATUS_OK = "OK";
	public static final String TRANSITION_PERCENTAGE_STATUS_WARNING= "Warning";
	public static final String TRANSITION_PERCENTAGE_STATUS_CRITICAL = "Critical";
			
	public static final String DUPLICATE_DURABLE_ID = "Duplicate name is not allowed";
	private static StateStamper stateStamper;
	private static BigDecimal BIG100 = BigDecimal.valueOf(100);
	
	private static ServletFileUpload upload; 
	private static String directoryMark; 
	
	
	public static StateStamper getStateStamper(){
		return stateStamper;
	}
	
	public static void setStateStamper(StateStamper stateStamper){
		if (DurableManager.stateStamper == null)
		{
			DurableManager.stateStamper = stateStamper;
			
			int maxFileSize = 5000 * 1024;
			int maxMemSize = 5000 * 1024;
			
			DiskFileItemFactory factory = new DiskFileItemFactory();
			// maximum size that will be stored in memory
			factory.setSizeThreshold(maxMemSize);
			// Location to save data that is larger than maxMemSize.
			factory.setRepository(new File(stateStamper.attachmentDirectory));
			// Create a new file upload handler
			upload = new ServletFileUpload(factory);
			// maximum file size to be uploaded.
			upload.setSizeMax(maxFileSize);
			
			String os = System.getProperty("os.name").toLowerCase();
			
			if (os.indexOf("nix") >= 0 || os.indexOf("nux") >= 0 || os.indexOf("aix") >= 0){
				directoryMark = "//";
			}
			else{
				directoryMark = "\\";
			}
			logger = LoggerFactory.getLogger("stateStamper");
		}
	}
	
	public static void getAttachment(String file, String mimeType,
			HttpServletResponse response) throws Throwable {
		// String mimeType = request..getMimeType(file);
		// getServletContext().getMimeType(filename);
		String fileName = "";
		if (directoryMark.equalsIgnoreCase("//")){
			fileName = file.substring(file.lastIndexOf("/")+ 1);
		}
		else {
			fileName = file.substring(file.lastIndexOf(directoryMark));
		}
		fileName = URLEncoder.encode(fileName, "UTF-8").replaceAll("\\+", "%20").replaceAll("%28", "\\(").replaceAll("%29", "\\)").replaceAll("%3B", ";").replaceAll("%40", "@").replaceAll("%23", "\\#").replaceAll("%26", "\\&");
	    response.setHeader("Content-disposition", "attachment;filename="
	              + fileName);
		response.setContentType(mimeType);
		OutputStream os = response.getOutputStream();
		BufferedInputStream bif = new BufferedInputStream(new FileInputStream(
				file));
		try {
			int ch = 0;
			while ((ch = bif.read()) > -1)
				os.write(ch);
		} finally {
			bif.close();
			os.close();			
		}
		return;

	}
	
	public static void removeAttachment(Integer attachment_PK, String durableID) throws Throwable {
		Attachment attachment = new Attachment();
		attachment.setAttachmentPk(attachment_PK);		
		attachment = (Attachment)(Persistence.search(attachment).get(0));
		File f = new File(attachment.getFileLocation());
		f.delete();
		Machine durable = stateStamper.getMachine(durableID);
		durable.removeAttachment(attachment);
		Persistence.delete(attachment);
	}
	
	public static String addAttachment(HttpServletRequest request){
		String durableID = null;
		String equipmentAttribute =null;
		Machine durable = null; 
		DurableStatus newStatus = null;		
		try {
			// setup the path to store the attachment
			String filePath = stateStamper.attachmentDirectory;
			EquipmentType type = (EquipmentType) (request.getSession()
					.getAttribute("equipmentType"));
			String equipmentType = type.name;
			Integer equipment_pk = 0;

			if (filePath.endsWith(directoryMark)) {
				filePath = filePath + equipmentType + directoryMark;
			} else {
				filePath = filePath + directoryMark + equipmentType + directoryMark;
			}
			
			File file;

			// Verify the content type
			String contentType = request.getContentType();
			if (contentType.indexOf("multipart/form-data") >= 0) {
				// Parse the request to get fx``ile items.
				List fileItems = upload.parseRequest(request);
				// Process the uploaded file items
				Iterator i = fileItems.iterator();
				// first iterator to find DurableID
				while (i.hasNext()) {					
					FileItem fi = (FileItem) i.next();
					if (fi.isFormField()) {
						if ("durableID".equalsIgnoreCase(fi.getFieldName())) {
							durableID = fi.getString();
							equipment_pk = ((Machine) stateStamper
									.getMachine(durableID)).primaryKey;
							filePath = filePath + fi.getString() + directoryMark;
							durable = stateStamper.getMachine(durableID);
							newStatus =(DurableStatus)durable.context.get("durableStatus");
						}
						
						if ("attribute".equalsIgnoreCase(fi.getFieldName())) {
							equipmentAttribute = fi.getString();							
						}
						
					}
				}
				i = fileItems.iterator();
				String basefilePath = filePath;
				while (i.hasNext()) {
					FileItem fi = (FileItem) i.next();
					if (!fi.isFormField()) {
						// Get the uploaded file parameters
						filePath = basefilePath;
						Attachment attachment = new Attachment();
						attachment.setEquipmentPk(equipment_pk);
						String fieldName = equipmentAttribute;
						String fileName = fi.getName();

						filePath = filePath + fieldName + directoryMark;
						File tmp = new File(filePath);
						// tmp.mkdirs();
						boolean isInMemory = fi.isInMemory();
						long sizeInBytes = fi.getSize();
						
					
						fileName = fileName.substring(fileName
											.lastIndexOf("\\") +1);						
						
						file = new File(filePath + fileName);
						
						if (file.exists()){
							String filePathtmp = file.getPath();
							//if the file exists already then we add timestamp at the end of it.
							filePathtmp = filePathtmp.substring(0,filePathtmp.indexOf(fileName));
							
							String timestamp = new ThreadSafeSimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date());
														
							if (fileName.lastIndexOf('.')>=0){
								String firstpart = fileName.substring(0,fileName.indexOf('.'));
								String secondpart = fileName.substring(fileName.indexOf('.'));
								fileName = firstpart + timestamp + secondpart;
							}
							else {
								fileName = fileName + timestamp;
							}
							file = new File(filePathtmp + fileName);
     							
						}
						attachment.setDisplayFilename(fileName);
						attachment.setFileLocation(file.getPath());
						attachment.setFileContent(fi);
						attachment.setType(fieldName);		
						attachment.setStatusPk(newStatus.STATUS_PK);
						Date time = new Date();						
			 	    	attachment.setTime(formatter.format(time));
			 	    	attachment = (Attachment)Persistence.store(attachment);
			 	    	String folder = filePath.substring(0,filePath.lastIndexOf(directoryMark));
			 	        File temp = new File(folder);
			 	        temp.mkdirs();
			 	        FileItem fileItem = attachment.getFileContent();
			 	        temp = new File(attachment.getFileLocation());
			 	        fileItem.write(temp);
			 	        attachment.setStatus(newStatus);
			 	        durable.addAttachment(attachment);
					}
				}
			}
		} catch (Throwable t) {
			logger.error(t.getMessage(),t);
		} finally {
			return  durableID;
		}
	}
	
	public static Map getTransitionAttributes(HttpServletRequest request) {
		HashMap attachments = new HashMap();
		HashMap attributes = new HashMap();
		HashMap result = new HashMap();
		String durableID;
		try {
			// setup the path to store the attachment
			String filePath = stateStamper.attachmentDirectory;
			EquipmentType type = (EquipmentType) (request.getSession()
					.getAttribute("equipmentType"));
			String equipmentType = type.name;
			Integer equipment_pk = 0;

			if (filePath.endsWith(directoryMark)) {
				filePath = filePath + equipmentType + directoryMark;
			} else {
				filePath = filePath + directoryMark + equipmentType + directoryMark;
			}
			
			File file;

			// Verify the content type
			String contentType = request.getContentType();
			if (contentType.indexOf("multipart/form-data") >= 0) {
				// Parse the request to get file items.
				List fileItems = upload.parseRequest(request);
				// Process the uploaded file items
				Iterator i = fileItems.iterator();
				// first iterator to find DurableID
				while (i.hasNext()) {
					FileItem fi = (FileItem) i.next();
					if (fi.isFormField()) {
						if ("durableID".equalsIgnoreCase(fi.getFieldName())) {
							durableID = fi.getString();
							equipment_pk = ((Machine) stateStamper
									.getMachine(durableID)).primaryKey;
							filePath = filePath + fi.getString() + directoryMark;
						}
					}
				}
				i = fileItems.iterator();
				String basefilePath = filePath;
				while (i.hasNext()) {
					filePath = basefilePath;
					FileItem fi = (FileItem) i.next();
					if (!fi.isFormField()) {
						// Get the uploaded file parameters
						Attachment attachment = new Attachment();
						attachment.setEquipmentPk(equipment_pk);
						String fieldName = fi.getFieldName();
						String fileName = fi.getName();

						filePath = filePath + fieldName + directoryMark;
					
						boolean isInMemory = fi.isInMemory();
						long sizeInBytes = fi.getSize();
						
						fileName = fileName.substring(fileName
											.lastIndexOf("\\") + 1);
						
						file = new File(filePath + fileName);
						
						if (file.exists()){
							filePath = file.getPath();
							//if the file exists already then we add timestamp at the end of it.
							filePath = filePath.substring(0,filePath.indexOf(fileName));
							
							String timestamp = new ThreadSafeSimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date());
														
							if (fileName.lastIndexOf('.')>=0){
								String firstpart = fileName.substring(0,fileName.indexOf('.'));
								String secondpart = fileName.substring(fileName.indexOf('.'));
								fileName = firstpart + timestamp + secondpart;
							}
							else {
								fileName = fileName + timestamp;
							}
							file = new File(filePath + fileName);
     							
						}
						attachment.setDisplayFilename(fileName);
						attachment.setFileLocation(file.getPath());
						attachment.setFileContent(fi);
						attachment.setType(fieldName);						
						attachments.put(fieldName, attachment);
					}else {
						if (attributes.get(fi.getFieldName())!=null){
							String tmp = (String)attributes.get(fi.getFieldName());
							tmp = tmp + "," + fi.getString();
							attributes.put(fi.getFieldName(), tmp);
						}
						else {
							String tmp = fi.getString();
							if (tmp!=null){
								tmp = DurableStatus.removeillegalcharactors(tmp);	
							}
							else{
								tmp = "";
							}
								
							attributes.put(fi.getFieldName(), tmp );
						}
						
					}	
				}
			}
		} catch (Throwable t) {
			logger.error(t.getMessage(),t);
		} finally {
			result.put("attribute",attributes);
			result.put("attachment", attachments);			
			return result;
		}
	}
	
	public static Map transition(Administrator user,Map transAttributes, Map transAttachments)
		{
		TransactionManager txManager = new TransactionManager();
		Map result = new HashMap();
		Machine durable = new Machine();
		try
			{
			txManager.beginTransaction();
			Map attribs = transAttributes;
			String substatePk = (String)attribs.get("substate");
			String statusPk = (String)attribs.get("STATUS_PK");
			String durableId = (String)attribs.get("durableId");
			String propStatus = (String)attribs.get("propstatus");
			result.put("durableId", durableId);
			durable  = 	stateStamper.getMachine(durableId);
			DurableStatus currentStatus = getLatestStatus(statusPk);
			Substate toSubstate = getSubstateByPk(Integer.valueOf(substatePk));
			Transitions transition = getTransition(durable.getEquipmentType(),currentStatus,toSubstate);
			//Map attachments = getTransitionConditions(transition, TransitionCondition.ATTACHMENT, currentStatus);
			//result.put("attachment",attachments);				
			validateTransitionPreConditions(durable.getEquipmentType(),user,transition,attribs);
			String oldLocation = durable.location;
			try{durable = (Machine)durable.clone();}
			catch(Throwable t){
				logger.error("durableId:"+durableId,t);throw new RuntimeException(SYSTEM_ERROR + ":" + t.getMessage());}
			DurableStatus newStatus = updatedStatusFromTransition(durable.getEquipmentType(),currentStatus,attribs,user,toSubstate);
			durable.location = newStatus.location;
			durable.workstation = newStatus.workstation;
			durable.getContext().put("durableStatus", newStatus);
			if(user.isAdministrator() || transition.conditionsTrue(durable))
				{
				newStatus.TIME = DurableManager.formatter.format( new Date());
				Map exceptionAttributes = executeTransitionActions(transition.getActions(),durable,newStatus);
				if(validateLocation(durable, newStatus.location,oldLocation))
					updateLocationCount( durable, newStatus.location, oldLocation,txManager);
				txManager.store(newStatus);
				txManager.store(durable);
				txManager.setMachine(durable,stateStamper);
				stateStamper.updateDurationForDurableStatus(durable, currentStatus,exceptionAttributes,txManager);
				stateStamper.updateDurableParent(durable, currentStatus.workstation, newStatus.workstation,txManager);
				boolean propagateStatus = ( propStatus == null && durable.getEquipmentType().propagateStatus() ) || (propStatus != null && propStatus.equals("Y"));
			    if (propagateStatus && durable.hasChild())
			    	{
			    	propagateStatus(durable, newStatus,user.isAdministrator(),txManager);
			    	}
			}
			//store attachments
	 	    Iterator it = transAttachments.keySet().iterator();
	 	    while (it.hasNext())
	 	    	{
	 	    	Object key = it.next();
	 	    	Attachment attachment =  (Attachment)transAttachments.get(key);
	 	    	attachment.setStatusPk(newStatus.STATUS_PK);
	 	    	attachment.setTime(newStatus.TIME);
	 	    	attachment = (Attachment)txManager.store(attachment);
	 	    	String tempslash;
	 	    	if (directoryMark.equalsIgnoreCase("//")){
	 				tempslash = "/";
	 			}
	 			else {
	 				tempslash = "\\";
	 			}
	 	    	
	 	    	String folder = (attachment.getFileLocation()).substring(0,attachment.getFileLocation()
								.lastIndexOf(tempslash));
	 	        
	 	    	File temp = new File(folder);
	 	        temp.mkdirs();
	 	        FileItem fileItem = attachment.getFileContent();
	 	        temp = new File(attachment.getFileLocation());
	 	        fileItem.write(temp);
	 	        attachment.setStatus(newStatus);
	 	        durable.addAttachment(attachment);
	 	    	}
	 	    txManager.executeBatch();
			}
		catch(Throwable t)
			{
			txManager.rollback();
			result.put("ERROR", t.getMessage());
			logger.error(t.getMessage(),t);
			}
		finally 
			{
			txManager.close();
			}
		return result;
		}
	
	
	private static Map executeTransitionActions(List actions,Machine durable,DurableStatus newStatus)
		{
		Map exceptionAttributes = updateDynamicAttributes(durable,actions,newStatus);
		durable.state = newStatus.STATE;
		durable.workstation = newStatus.workstation;
		durable.location = newStatus.location;					
		return exceptionAttributes;
		}
	
	private static Substate getSubstateByPk(Integer substatePk)
		{
		Substate search = new Substate();
		search.primaryKey = substatePk;
		List states  = null;
		try{ states = Persistence.search(search);}
		catch(Throwable t){}
		Substate result =  null;
		if(states != null && states.size() > 0){result = (Substate)states.get(0);}
		return result;
		}
	
	private static DurableStatus updatedStatusFromTransition(EquipmentType eqType,DurableStatus currentStatus,Map transAttributes,Administrator user,Substate newState)throws CloneNotSupportedException
		{
		DurableStatus updatedStatus = currentStatus.clone();
		updatedStatus.STATUS_PK = null;
		updatedStatus.OPERATOR_ID = user.getCoreId();
		updatedStatus.STATE= newState.e10;
		updatedStatus.SUBSTATE = newState.substate;
		Iterator attribs = transAttributes.keySet().iterator();
		String param;
		List attribsNames =  eqType.getAttributesNames();
		Object tmpVal=null;
		while(attribs.hasNext())
			{
			param = (String)attribs.next();
			if(updatedStatus.has(param) && attribsNames.contains(param) )
				{
				tmpVal=transAttributes.get(param);
				if(tmpVal == null){tmpVal="";} 
				updatedStatus.set(param,tmpVal);
				}
			}
		updatedStatus.COMMENTS = null;
		updatedStatus.putNewComments(user.coreId, updatedStatus.TIME,(String)transAttributes.get("COMMENTS"));
		if(transAttributes.containsKey("workstation"))
			{
			tmpVal = transAttributes.get("workstation");
			if(tmpVal==null){tmpVal="";}
			updatedStatus.workstation = tmpVal.toString();
			}
		if(transAttributes.containsKey("location"))
			{
			tmpVal = transAttributes.get("location");
			if(tmpVal == null){tmpVal="";}
			updatedStatus.location = tmpVal.toString();
			}
		return updatedStatus;
		}
	
	private static Transitions getTransition(EquipmentType eqType,DurableStatus current,Substate toState)
		{
		Substate oldState = getState(eqType.primaryKey,current.STATE,current.SUBSTATE);
		Transitions transitions = getTransition(oldState, toState);
		return transitions;
		}
	
	private static void validateTransitionPreConditions(EquipmentType eqType,Administrator user,Transitions transition,Map attributes)
		{
		if(!user.isAdministrator())
			{
			validateTransition(transition);
			validateRequiredFields(transition,attributes);
			validateRole(transition,user);
			}
		}
	
	private static void validateTransition(Transitions transition)
		{
		if(transition == null){throw new RuntimeException(TRANSITION_ERR_TRANSITION_UNDEFINED);}
		if(!transition.getEnabled()){throw new RuntimeException(TRANSITION_ERR_TRANSITION_DISABLED);}
		}
	
	private static void validateRole(Transitions transition,Administrator user)
		{
		List conds = transition.getConditionsForAttribute("ROLE");
		if(conds != null && conds.size() > 0)
			{
			TransitionCondition cond = (TransitionCondition)conds.get(0); 
			String value = cond.getCondValue(); 
			if(!value.contains(user.getRole().rolename)){throw new RuntimeException(TRANSITION_ERR_NO_PRIVILEGES);}
			else {cond.setOrder(String.valueOf( TransitionCondition.UI_PRE_ORDER)); }
			}
		}
	
	private static boolean validateRequiredFields(Transitions transition,Map attributes)
		{
		boolean filled = true;
		List preConditions = transition.getPreConditions();
		TransitionCondition condition;
		String val;
		for(int i = 0 ; preConditions != null && i < preConditions.size();i++)
			{
			condition = (TransitionCondition)preConditions.get(i);
			if(condition.getCondition().equals(TransitionCondition.REQUIRE) )
				{
				 val = (String)attributes.get(condition.getAttribute());
				 filled = val != null && !val.trim().equals("");
				}
			if(!filled){throw new RuntimeException(condition.getAttribute() + TRANSITION_ERR_MISSING_FIELD);}
			}
		return filled;
		}
	
	private static DurableStatus getLatestStatus(String statusPk)
		{
		DurableStatus currentStatus=null;
		try
			{
			CurrentDurableStatus searchStatus = new CurrentDurableStatus();
			searchStatus.STATUS_PK = Integer.parseInt(statusPk);
			Vector tmp = (Vector) Persistence.search(searchStatus);
			if (!tmp.isEmpty()){currentStatus = ((CurrentDurableStatus) tmp.get(0)).getDurableStatus() ;}
			else {throw new RuntimeException(TRANSITION_ERR_NOT_LATEST_STATUS);}
			}
		catch(RuntimeException re)
			{//catch RuntimeException so it's not caught by the catch(Throwable) and the message is not  lost
			throw re; 
			}
		catch (Throwable t)
			{
			throw new RuntimeException(SYSTEM_ERROR);
			}
		return currentStatus;
		}
	
	private static Map translateTransAttributes(Map transAttributes)
		{
		Map attributes = new HashMap();
		Set names = transAttributes.keySet();
		Iterator it = names.iterator();
		String name,value;
		while(it.hasNext())
			{
			name = (String)it.next();
			List values = Arrays.asList((String[])transAttributes.get(name));
			value = values.toString().replace("[","").replace("]","");
			attributes.put(name, value);
			}
		return attributes;
		}			
	private static Substate getState(Integer equipmentpk,String state,String substate)
		{
		Substate res=null;
		Substate sub= new Substate();
		sub.equipmenttypePK = equipmentpk;
		sub.e10= state;
		sub.substate=substate;
		try
			{
			Vector result = (Vector)Persistence.search(sub);
			if( result!=null && result.size() >0  ){res= ((Substate)result.get(0));}
			} catch (Throwable e)
			{
			}
		return res;
	}

	private static void propagateStatus(Machine durable, DurableStatus newDStatus, boolean force,TransactionManager txManager) throws Throwable
		{
		String substate = newDStatus.SUBSTATE;
		String state = newDStatus.STATE;
		String op = newDStatus.OPERATOR_ID;
		String location = newDStatus.location;
		String time = newDStatus.TIME;
		Map<String, List<String>> children = durable.getChildren();
		if (children == null || children.isEmpty()) return;
		String durableId = null;
		for (Iterator it = children.keySet().iterator();it.hasNext();)
			{
			List<String> childrenvector = children.get(it.next());
			for (int j = 0; j < childrenvector.size(); j++)
				{
				try
					{
					durableId = (String) childrenvector.get(j);
					Machine childDurable = stateStamper.getMachine(durableId);
					childDurable = (Machine) childDurable.clone();
					DurableStatus durableStatus = (DurableStatus) childDurable.getContext().get("durableStatus");
					DurableStatus oldDStatus = durableStatus.clone();
					//update fields before we validate them
					childDurable.location=location;
					durableStatus.SUBSTATE = substate;
					durableStatus.STATE = state;
					durableStatus.STATUS_PK = null;
					durableStatus.OPERATOR_ID = op;
					durableStatus.location = location;
					durableStatus.COMMENTS = null;
					durableStatus.putNewComments(durableStatus.OPERATOR_ID, durableStatus.TIME,"Automatic state update from parent equipment");
					Substate childNewState = getState(childDurable.getEquipmentType().primaryKey,newDStatus.STATE, newDStatus.SUBSTATE);
					Transitions childTransition = getTransition(childDurable.getEquipmentType(), oldDStatus,childNewState);
					if (force || (childTransition != null && childTransition.enabled && childTransition.conditionsTrue(childDurable)))
						{
						//update TIME field
						durableStatus.TIME = time;
						Map exceptionAttributes = new HashMap();
						if (childTransition != null)
							{
							exceptionAttributes.putAll(updateDynamicAttributes(childDurable,childTransition.getActions(),durableStatus));
							}
						if(validateLocation(childDurable, durableStatus.location,oldDStatus.location))
	                        updateLocationCount( childDurable, durableStatus.location, oldDStatus.location,txManager);
						txManager.store(durableStatus);
						childDurable.context.put("durableStatus", durableStatus);
						txManager.store(childDurable);
						txManager.setMachine(childDurable,stateStamper);
						stateStamper.updateDurationForDurableStatus(childDurable, oldDStatus, exceptionAttributes,txManager);
						}
					}
				catch (Throwable t)
					{
					logger.error("Error trying to update attached durable " + durable.getMESName(),t);
					}

				}
			}
		}
	/**
	 * 
	 * @return return a Map with the attributes that are not going to be updated by updateDurationForDurableStatus, these are the counters that are reset
	 */
	private static Map updateDynamicAttributes(Machine mac,List actions,DurableStatus newStatus)
		{
		TransitionAction action=null;
		Map exceptionAttributes = new HashMap();
		//this is the Time attributes get reset and will not be update in the  updateDurationForDurableStatus method.
	    for(int i =0;actions!=null&& i< actions.size();i++)
	    	{
	    	action = (TransitionAction)actions.get(i);
	    	if(!action.onDurable()){continue;}
	    	Map<String, String> actionParameters = action.getActionParameters();
			String attribute = actionParameters.get(TransitionAction.PARAM_ATTRIBUTE);
	    	EquipmentAttribute attr = mac.getEquipmentType().getAttribute(attribute);
	    	Object newValue="";
	    	Object oldValue =mac.get(attribute);
    		if(TransitionAction.RESET.equalsIgnoreCase(action.getAction()) && attr != null)
    			{
    			if(attr.isCounter()){
    				newValue = "0";
    				if (mac.getEquipmentType().isTimeRelatedAttribute(attr.getName())){
    					String[] temp = attr.name.split("\\" + ThresholdCounter.SEPARATOR);  					
    					exceptionAttributes.put(temp[0], attr);
    				}
    			}
    			else if(attr.isText()){newValue = "";}
    			}
    		else if(TransitionAction.INCREASE.equalsIgnoreCase(action.getAction()) && attr != null && attr.isCounter())
    			{
    			long lValue = 0;
    			try{lValue = Long.parseLong(oldValue.toString());}
    			catch(NumberFormatException e){e.printStackTrace();}
    			lValue++;newValue = lValue;
    			}
    		else if(TransitionAction.DECREASE.equalsIgnoreCase(action.getAction()) && attr != null && attr.isCounter())
    			{
    			long lValue = 0;
				try{lValue = Long.parseLong(oldValue.toString());}
				catch (NumberFormatException e){e.printStackTrace();}
				lValue--;newValue = lValue;
    			}
    		else if(TransitionAction.SET.equalsIgnoreCase(action.getAction()))
    			{
    			String value = action.getActionParameters().get(TransitionAction.PARAM_VALUE);
    			if(value.startsWith(TransitionAction.PARAM) && mac.has(value.substring(1))){value = String.valueOf(mac.get(value.substring(1)));}
    			newValue = value;
    			if (newValue == null)newValue = "";
    			}
    		else if(TransitionAction.INCREASE_BY.equalsIgnoreCase(action.getAction()))
    			{
    			newValue = updateBy(mac,action,true);
    			}
    		else if(TransitionAction.DECREASE_BY.equalsIgnoreCase(action.getAction()))
    			{
    			newValue = updateBy(mac,action,false);
    			}
    		else if(TransitionAction.CLEAR.equalsIgnoreCase(action.getAction()))
				{
				newValue = "";
				if(attribute!=null&&attribute.equalsIgnoreCase("location")) 
					{
					newValue = "Unknown";
					}
				}
    		else if(TransitionAction.SEND_EPR.equalsIgnoreCase(action.getAction()))
    			{
    			stateStamper.sendEPRToPic(mac);
    			}
    		if(newStatus.has(attribute))
    			{
				newStatus.set(attribute,newValue);
    			}
	    	if(mac.has(attribute))
	    		{
	    		mac.set(attribute, String.valueOf(newValue));
	    		}
	    	}
	    return exceptionAttributes;
		}
	
	private static Object updateBy(Machine mac,TransitionAction action,boolean add)
		{
		Object newValue = null;
		Map<String, String> actionParameters = action.getActionParameters();
		String attribute = actionParameters.get(TransitionAction.PARAM_ATTRIBUTE);
		String by = actionParameters.get(TransitionAction.PARAM_BY);
		Object tmpVal;
		EquipmentAttribute attr = mac.getEquipmentType().getAttribute(attribute);
		if(attr == null || !attr.isCounter()){return mac.get(attribute);}
		tmpVal = mac.get(attribute);
		if(tmpVal == null){tmpVal = "0";}
		BigDecimal  numVal = new BigDecimal(tmpVal.toString()),byVal=BigDecimal.valueOf(0); 
		EquipmentAttribute byAtt = mac.getEquipmentType().getAttribute(by);
		if(byAtt == null)
			{ 
			try{byVal = new BigDecimal(by);}
			catch(NumberFormatException e){}
			}
		else if(byAtt.isCounter())
			{
			tmpVal = mac.get(by);
			if(tmpVal==null){tmpVal = "0";}
			byVal = new BigDecimal(tmpVal.toString());
		    }
		if(add){newValue = numVal.add(byVal);}
		else   {newValue = numVal.subtract(byVal);}
		return newValue;
		}
	
	/**
	 * get the transition specified for the from and to states passed as arguments
	 * @param the source state
	 * @param the destination state
	 * @return the transition for these states, a empty transition if not defined
	 * */
	private static Transitions getTransition(Substate from,Substate to)
		{
		Transitions transition = new Transitions();
		//return if one argument is empty or they do not belong to the same equipment type
		if( (from ==null || to == null) || !from.equipmenttypePK.equals(to.equipmenttypePK ))return transition;
		transition.setEquipmentTypePK(from.equipmenttypePK);
		transition.from = from.e10+ "-" + from.substate;
		transition.to = to.e10+ "-" + to.substate;
		Vector v=null;
		try
			{
			v = Persistence.search(transition);
			if (v != null && v.size() > 0){transition = (Transitions) v.get(0);}
			}
		catch (Throwable t){
			logger.error("From:"+from+" To:"+to,t);
		}
		return transition;
		}

	public static Map getDurableSearchResults(EquipmentType equipmentType, Area area, String[] searchFields,Map params) throws Throwable
		{
		Map result = new HashMap();
		Report report = getSearchDisplayReport(equipmentType, area);
		String [] orderArray = report.getColumns()[3].split(",");
		String orderBy = orderArray[0].replace("\"","");
		if(orderBy.equals("DURABLE_ID")){orderBy="mESName";}
		Boolean orderAsc = Boolean.valueOf(orderArray[1].replace("\"",""));
		Map filters = new HashMap();
		int attribStart = 7;
		List attributes = new ArrayList(report.getColumns().length-attribStart);
		for(int i = 0;i+attribStart<report.getColumns().length;i++)
			{
			attributes.add(report.getColumns()[i+attribStart]);
			}
		for(String searchField:searchFields)
			{
			if(params.containsKey(searchField))
				{
				String [] paramValue = (String[])params.get(searchField);
				filters.put(searchField,paramValue[0]);
				}
			}
		
		List durables = DurableManager.getDurablesForReport(report.getEquipmentType(),area,filters, orderBy, orderAsc);
		if(params.containsKey("durable_name"))
			{
			String durableId = ((String[])params.get("durable_name"))[0];
			if(durableId != "")
				{
			    durables = filterDurablesByNameWithLike(durables,durableId);
				}
			}
		if(attributes != null && attributes.contains("STATE") && attributes.contains("SUBSTATE"))
			{
			result.put("stateColors",getStateColors(equipmentType,durables));
			}
		Map searchFieldsCatalog = getSearchFieldsCatalog(durables,searchFields);
		result.put("searchFieldsCatalog",searchFieldsCatalog);
		result.put("searchFields",attributes);
		if(filters.size() == 0 ){durables = new ArrayList();}
		result.put("durables",durables);
		return result;
		}
	
	
	/**
	 * @returns the list of state-colors for the list of the durable result 
	 * */
	private static List getStateColors(EquipmentType equipmentType,List<IStatus> durables)
		{
		List colors = new ArrayList();
		String state,substate,color;
		Map<String,String> cachedColors = new HashMap();
		for(IStatus durable:durables)
			{
			state = (String)durable.get("STATE");
			substate = (String)durable.get("SUBSTATE");
			String key = state + "-" + substate;
			if(!cachedColors.containsKey(key))
				{
				String dbColor = "";
				Substate dbSubstate = getState(equipmentType.primaryKey,state,substate);
				if(dbSubstate != null && dbSubstate.color != null){dbColor = dbSubstate.color;}
				cachedColors.put(key,dbColor);
				}
			color = cachedColors.get(key);
			colors.add(color);
			}
		return colors;
		}
	
	private static List filterDurablesByNameWithLike(List<Machine> durables, String name)
		{
		List filterDurables = new ArrayList();
		for(Machine durable:durables)
			{
			if(durable.mESName.contains(name))
				{
				filterDurables.add(durable);
				}
			}
		return filterDurables;
		}
	
	private static Map getSearchFieldsCatalog(List durables,String[] searchFields)
		{
		Map searchFieldsCatalog = new HashMap();
		for(int j = 1;j<searchFields.length;j++)
			{
			Set values = new HashSet();
			for(int i = 0; i < durables.size();i++)
				{
				IStatus durable = (IStatus)durables.get(i);
				Object tmp = durable.get(searchFields[j]);
			    String value  = tmp !=null?tmp.toString():"";
			    values.add(value);
				}
			List sorted = new ArrayList(values);
			Collections.sort(sorted);
			searchFieldsCatalog.put(searchFields[j],sorted);
			}
		return searchFieldsCatalog;
		}
	
	public static Report getSearchDisplayReport(EquipmentType eqType, Area area)throws Throwable
		{
		Report report = new Report();
		report.reportType = "Search";
		report.period = "Realtime";
		report.grouping = eqType.name;
		report.setEquipmentType(eqType);
		report.setArea(area);
		List reps  = StateStamper.reports.getDurableRealTimeReport(eqType,area,"Search");
		if(reps == null || reps.size() == 0)
			{
			String eqName = eqType.name;
			eqType.name = "DEFAULT";
			report.grouping = "DEFAULT";
			reps  = StateStamper.reports.getDurableRealTimeReport(eqType,area,"Search");
			eqType.name = eqName; 
			}
		if(reps.size() > 0)
			{
			report.setColumns(StateStamper.reports.getReportDefinition(report));
			}
		return report;
		}
	
	/**
	 * returns a list with the possible states that can be transitioned from the state passed 
	 * @param fromState the state from the  
	 * @return a vector with the possible sub states from the fromState specified in the argument
	 * */
	public static List getDefinedNextSubstate (Substate fromState)throws Throwable
	    {
		Vector result = new Vector();
		Transitions transitions = new Transitions();
		try
			{
			Substate state;
			state = (Substate) ((Vector) Persistence.search(fromState)).get(0);
			transitions.setEquipmentTypePK(state.equipmenttypePK);
			transitions.setFromStatePK(state.primaryKey);
			transitions.setEnabled(true);
			List tmp = Persistence.search(transitions);
			Substate tmpState;
			for (int i = 0; tmp != null && i < tmp.size(); i++)
				{
				tmpState = ((Transitions) tmp.get(i)).getToState();
				tmpState.primaryKey = ((Transitions) tmp.get(i)).toStatePk;
				result.add(tmpState);
				}
			}
		catch (Throwable ex)
			{
			logger.error("fromState:"+fromState,ex);
			throw ex;
			}
		return result;
		}   
	
	public static void store (Object input) throws Throwable {
		if(input instanceof DurableStatus){
			Persistence.store((DurableStatus)input);
		}
	}
	
	public static CurrentDurableStatus getDurableDetails(String durableId, String statusPk) throws Throwable
		{
		boolean isCurrent = statusPk == null || statusPk.equalsIgnoreCase("") || statusPk.equalsIgnoreCase("null");
		Machine machine = stateStamper.getMachine(durableId);
		DurableStatus status;
		if (isCurrent)
			{
			status = (DurableStatus)machine.get("durableStatus");
			}
		else
			{
			status = new DurableStatus();
			status.STATUS_PK = Integer.parseInt(statusPk);
			status = (DurableStatus) ((Vector) Persistence.search(status)).get(0);
			}
		return new CurrentDurableStatus(status, machine);
		}

	public static CurrentDurableStatus getDurableDetails(String durableId) throws Throwable
		{
		return getDurableDetails(durableId, "");
		}
	
	public static Vector getDurableTransitionHistory(String durable_ID) throws Throwable
		{
		DurableStatus durableStatus = new DurableStatus();
		String[] equal = { "DURABLE_ID", durable_ID };
		String[] order = { "TIME" };
		return Persistence.search(DurableStatus.class, equal, null, null, null, order, false, false);
		}
	
	/**
	 * returns a single result for each testerId - loadboardId - touchDown usage and limit 
	 * @param testerId/ host  
	 * @return a vector with testerId, loadboardId and touchDown usage/limit
	 * */
	public static List getAttachedDurablesMonitor(String testerID) throws Throwable
		{
		List results = new ArrayList();
		Machine equipment = stateStamper.getMachine(testerID);
		if(equipment == null) return results;
		results.add(testerID);
		Report report = new Report();
		report.reportType = "TesterDurables";
		report.grouping = "DEFAULT";
		report.equipmentType = new EquipmentType();
		report.equipmentType.name = "Durable";
		report.period = "RealTime";
		String[] definition = StateStamper.reports.getReportDefinition(report);
		String equipmentTypeName = definition[5];
		List<String> children = equipment.getChildrenByEquipmentType(equipmentTypeName);
		String value,label;

		for (String childID : children)
			{
			Machine durable = stateStamper.getMachine(childID);
			if (durable != null)
				{
				EquipmentType eqType = durable.getEquipmentType();
				DurableStatus durableStatus = (DurableStatus) durable.context.get("durableStatus");
				if (equipment.mESName.equals(durableStatus.workstation) || equipment.eIName.equals(durableStatus.workstation))
					{
						if(durableStatus.STATE.equals("Productive")){
							for (int i = 7; i < definition.length; i++)
							{
							String col = definition[i];
							if (col.contains("/"))
								{
								value = getCounterBarValue(durable, col);
								label = getCounterBarLabel(eqType, col);
								String percentage = value.substring(value.indexOf("(") + 1, value.indexOf(")"));
								value = value.substring(0, value.indexOf("("));
								results.add(label);
								results.add(percentage);
								results.add(value);
								}
							else
								{
								results.add(col + " : " + durable.get(col));
								}
							}
						}
					}
				}
			}
		return results;
		}
	
	public static List getMonitorResults (EquipmentType eqType,Area area,Integer rulePK, double warning, String[] returnformat, int orderby, String monitorType ) throws Throwable
	    {
		List rules = eqType.getBusinessRulesBy(BusinessRule.MONITOR_PAGE);
		boolean isTimeRelatedAttribute;
		CurrentDurableStatus currentDurableStatus = new CurrentDurableStatus();
		currentDurableStatus.equipmentType_pk = eqType.primaryKey;
		BusinessRule rule = null;
		String attribute = null;
		String conditionLimit = null;
		for (Object obj: rules)
			{
			BusinessRule brule = (BusinessRule) obj;
			if(rulePK.equals(brule.getPrimaryKey())){rule = brule;}
			}
		if (! rule.hasActions() )
			{
			return new Vector();
			}
		BusinessRuleAction businessRuleAction = (BusinessRuleAction) rule.getActions().get(0);
		Map<String, String> actionParameters = businessRuleAction.getActionParameters();
		attribute = actionParameters.get("count");
		conditionLimit = actionParameters.get("limit");
		isTimeRelatedAttribute = eqType.isTimeRelatedAttribute(attribute);
		Collection equipments = stateStamper.getMachines(eqType,area.getPrimaryKey()).values();
		List results = new ArrayList();
		for (Iterator it = equipments.iterator();it.hasNext();)
			{
			Machine equipment = (Machine) it.next();
			DurableStatus durableStatus = (DurableStatus) equipment.context.get("durableStatus");
			boolean isConditionMet = true;
			if (rule.hasConditions())
				{
				for (Object obj : rule.getConditions())
					{
					BusinessRuleCondition businessRuleCondition = (BusinessRuleCondition) obj;
					isConditionMet = businessRuleCondition.isMet(durableStatus);
					if (!isConditionMet) break;
					}
				}
			if (!isConditionMet) continue;
			CurrentDurableStatus equipmentStatus = new CurrentDurableStatus(durableStatus, equipment);
			BigDecimal attributeValue = new BigDecimal(0);
			BigDecimal limit = getLimit(conditionLimit, equipmentStatus);
			if (equipmentStatus.get(attribute) != null)
				{
				try{attributeValue = new BigDecimal((String) equipmentStatus.get(attribute));}catch (Exception e){}
				}
			if (limit == null || limit.equals(BigDecimal.ZERO))
				{
				continue;
				}
			BigDecimal usage = null;
			if (monitorType.equals("maximum_rate"))
				{
				usage = attributeValue.divide(limit, 2, BigDecimal.ROUND_CEILING);
				}
			else if (monitorType.equals("minimum_rate"))
				{
				if (attributeValue.equals(BigDecimal.ZERO))
					{
					usage = new BigDecimal(1);
					}
				else
					{
					usage = limit.divide(attributeValue, 2, BigDecimal.ROUND_CEILING);
					}
				}
			else if (monitorType.equals("time_left_in_days"))
				{
				long tmpValueLong = attributeValue.longValue();
				long tmpLimitLong = limit.longValue();
				if (tmpValueLong > tmpLimitLong)
					{
					int diff = Integer.parseInt(FiscalDate.getElapsedTimeinDays(tmpValueLong - tmpLimitLong));
					usage = new BigDecimal(0 - diff);
					}
				else
					{
					int diff = Integer.parseInt(FiscalDate.getElapsedTimeinDays(tmpLimitLong - tmpValueLong));
					usage = new BigDecimal(diff);
					}
				}

			else if (monitorType.equals("time_left_in_hours"))
				{
				long tmpValueLong = attributeValue.longValue();
				long tmpLimitLong = limit.longValue();
				if (tmpValueLong > tmpLimitLong)
					{
					int diff = Integer.parseInt(FiscalDate.getElapsedTimeinHours(tmpValueLong - tmpLimitLong));
					usage = new BigDecimal(0 - diff);
					}
				else
					{
					int diff = Integer.parseInt(FiscalDate.getElapsedTimeinHours(tmpLimitLong - tmpValueLong));
					usage = new BigDecimal(diff);
					}
				}
			boolean maximunFiltered = (monitorType.equals("maximum_rate") || monitorType.equals("minimum_rate")) && (usage.doubleValue() >= warning);
			boolean minimunFiltered = (monitorType.equals("time_left_in_days") || monitorType.equals("time_left_in_hours")) && (usage.doubleValue() <= warning);
			if (maximunFiltered || minimunFiltered)
				{
				String[] attributevaluegroup = new String[returnformat.length];
				for (int j = 0; j < returnformat.length; j++)
					{
					String tmpcase = returnformat[j];
					if (tmpcase.equalsIgnoreCase("usage"))
						{
						attributevaluegroup[j] = String.valueOf(usage);
						}
					else if (tmpcase.equalsIgnoreCase("limit"))
						{
						if (isTimeRelatedAttribute)
							{
							attributevaluegroup[j] = String.valueOf(FiscalDate.getElapsedTime(limit.longValue()));
							}
						else
							{
							attributevaluegroup[j] = String.valueOf(limit);
							}
						}
					else if (tmpcase.equalsIgnoreCase("conditionattribute"))
						{
						if (isTimeRelatedAttribute)
							{
							attributevaluegroup[j] = String.valueOf(FiscalDate.getElapsedTime(attributeValue.longValue()));
							}
						else
							{
							attributevaluegroup[j] = String.valueOf(attributeValue);
							}
						}
					else
						{
						attributevaluegroup[j] = (String) equipmentStatus.get(returnformat[j]);
						}
					}
				if (orderby > 0)
					{
					if (results.isEmpty())
						{
						results.add(attributevaluegroup);
						}
					else
						{
						for (int k = 0; k < results.size(); k++)
							{
							String[] item = (String[]) results.get(k);
							double itemvalue = Double.parseDouble(item[orderby - 1]);
							if (maximunFiltered && itemvalue <= usage.doubleValue())
								{
								results.add(k, attributevaluegroup);
								break;
								}
							else if (minimunFiltered && itemvalue <= usage.doubleValue())
								{
								results.add(k, attributevaluegroup);
								break;
								}
							if (k == results.size() - 1)
								{
								results.add(attributevaluegroup);
								break;
								}
							}
						}
					}
				else
					{
					results.add(attributevaluegroup);
					}

				}
			}
		return results;
	}
	
	private static BigDecimal getLimit(String val,CurrentDurableStatus equipmentStatus)
		{
		BigDecimal limit =null;		
		try
			{
			if (equipmentStatus.has(val))
				{
				Object objVal = equipmentStatus.get(val);
				if (objVal == null || objVal.equals(""))
					{
					limit = new BigDecimal("0");
					}
				else
					{
					limit = new BigDecimal((String) objVal);
					}
				}
			else
				{
				limit = new BigDecimal(val);
				}
			}
		catch (java.lang.NumberFormatException ex) {
			ex.printStackTrace();
		}
		finally{
			if (limit ==null) {
				limit = new BigDecimal("1");
			}
		}
		return limit;
		}
	
	/**
	 * @return a list of lists with the inventory by the following attribtues
	 * @platform Equipment, platform, type, state 
	 * */
	public static List getInventory(Report report, Hashtable data, String range)throws Throwable
		{
		List durables = stateStamper.reports.getSortedRealtimeReportData(report,data);	 					 			 
		Map summaryTables = new LinkedHashMap(); 
		String field = "EQUIPMENTTYPE(DURABLE_ID)";
		Map totals = getTotalsBy(durables,field,null);		
		List byEquipmentType= getSummaryfromTotals(totals);
		summaryTables.put("equipmentType",byEquipmentType);
		Iterator it = report.getFilters().entrySet().iterator();
		while(it.hasNext())
			{
			Map.Entry cat = (Map.Entry)it.next();
			String category = (String)cat.getKey();
			String categoryValue = (String)cat.getValue();
			if(categoryValue.equals("")){categoryValue=null;}
			totals = getTotalsBy(durables,category,categoryValue);
			List summary = getSummaryfromTotals(totals);
			summaryTables.put(category.toLowerCase(),summary);
			}
        //only return a subset of the results to display
		Integer total = (Integer)byEquipmentType.get(0);
		int rowsPerPage = stateStamper.reports.getnumberofRecordperPage(report);
		if(range==null || !range.contains("-")){range="1-"+rowsPerPage;}
		String[] values = range.split("-");
		Integer start = Integer.valueOf(values[0])-1;
		Integer end   = Integer.valueOf(values[1]);
		end = end > total?total:end;
		durables = durables.subList(start, end);
		
		List results=new Vector();
		results.add(durables);
		results.add(summaryTables);
		results.add(stateStamper.reports.getDurableReportHeaders(report));
		results.add(rowsPerPage);
		return results;
		}
	
	public static Report getInventoryReport(HttpServletRequest request) throws Throwable
		{
		Map parameters = request.getParameterMap();
		
		Report report = new Report();
		report.reportType = "RealtimeInventory";
		report.grouping = "ALL";
		report.period = "Realtime";
		report.setArea(getCurrentArea(request));
		String equipmentType = "";
		if(parameters.containsKey("equipmentType")){equipmentType=((String[])parameters.get("equipmentType"))[0];}
		if (equipmentType != null && !equipmentType.equalsIgnoreCase(""))
			{
			report.setEquipmentType(stateStamper.getEquipmentTypeByName(equipmentType));
			report.grouping = equipmentType;
			}
		Map filters = stateStamper.reports.getReportFilters(report, null);
		if(filters == null || filters.size() == 0)
			{
			report.grouping = "ALL";
			filters = stateStamper.reports.getReportFilters(report, null);
			}
		Iterator iterator = filters.keySet().iterator();
		Hashtable selectedFilter = new Hashtable();
		while (iterator.hasNext())
			{
			String filter = (String) iterator.next();
			String tmpSelected = "";
			if(parameters.containsKey(filter.toLowerCase()) ){tmpSelected=((String[])parameters.get(filter.toLowerCase()))[0];}
			if (tmpSelected == null)
				{
				tmpSelected = "";
				}
			selectedFilter.put(filter, tmpSelected);
			}
		report.setFilters(selectedFilter);
		return report;
		}
	
	public static void getInventoryExcel(OutputStream outputStream, Report report, Hashtable data)throws Throwable
		{
		List durables = stateStamper.reports.getSortedRealtimeReportData(report,data);	 					 			 
		OutputStreamWriter osw = new OutputStreamWriter(outputStream);
		BufferedWriter bw = new BufferedWriter(osw);
		List headers = stateStamper.reports.getDurableReportHeaders(report);
		StringBuffer header = new StringBuffer();
		for (int i = 0; i < headers.size(); i++)
			{
			header.append(headers.get(i));
			header.append(",");
			}
		if (header.length() > 0)
			{
			header.replace(header.length() - 1, header.length(), "\n");
			}		
		bw.write(header.toString());
		for (int j = 0; j < durables.size(); j++)
			{
			Machine mac = (Machine) durables.get(j);
			StringBuffer row = new StringBuffer();
			for (int i = 0; i < headers.size(); i++)
				{
				row.append(mac.get(((String) headers.get(i))));
				row.append(",");
				}
			if (row.length() > 0)
				{
				row.replace(row.length() - 1, row.length(), "\n");
				}
			bw.write(row.toString());
			}
		bw.flush();
		bw.close();
		}
	
	public static void getHistoryExcel(OutputStream outputStream,List data)throws Throwable
		{
		WritableWorkbook workbook = null;
		try
			{
			workbook = FormatReport.createWorkbook(outputStream);
			workbook.createSheet("Report", 0);
			WritableSheet sheet = workbook.getSheet(0);
			List<String> headers = (List)data.get(0);
			int indexTime,indexComment;
			indexTime = headers.indexOf("TIME");
			indexComment = headers.indexOf("COMMENTS");
			for(int i=0;i< headers.size();i++)
				{
				sheet.addCell(new Label(i,0,(String)headers.get(i)));
				}
			
			for(int row = 1;row  < data.size();row++)
				{
				IStatus values  = (IStatus)data.get(row);
				for(int cols = 0; cols< headers.size();cols++)
					{
					Object tmp = values.get(headers.get(cols));
					if(cols == indexComment && tmp instanceof Map)
						{
						List tmpComment = (List)((Map)tmp).get(values.get(headers.get(indexTime)));
						if(tmpComment !=null && tmpComment.size() > 2){tmp = tmpComment.get(2);}
						}
					sheet.addCell(new Label(cols,row,String.valueOf(tmp)));					
					}
				}
			workbook.write();
			}
		catch (Throwable th)
			{
			logger.error(th.getMessage(),th);
			throw th;
			}
		finally
			{
			if (workbook != null) workbook.close();
			}
		}
	
	
	public static List removeMachinesByEquipmentType(List machines,String name)
		{
		for(int i=0;i<machines.size();i++)
			{
			if(name.equalsIgnoreCase((((Machine)machines.get(i)).getEquipmentType().name ))){ machines.remove(i--); }
			}
		return machines;
		}
	
	
	
	/**
	 * @return a list with the totals
	 * the first element of the list is an integer with the sum of all the elements
	 * 
	 * */
	public static List getSummaryfromTotals(Map totals)
		{
		List summary= new Vector();
		if(totals==null)return summary;
		Set entries = totals.entrySet();
		Iterator it = entries.iterator();
		List  tmp;
		int total = 0;
		DecimalFormat twoDForm = new DecimalFormat("#.##");
		while(it.hasNext())
			{
			 Map.Entry entry = (Map.Entry)it.next();
			 Integer count = (Integer)entry.getValue();
			 total += count.intValue();
			 tmp = new Vector();
			 tmp.add(entry.getKey());
			 tmp.add(count);
			 summary.add(tmp);
			}
		
		for(int i=0;i<summary.size();i++)
			{
			List item = (List)summary.get(i);
			Integer count = (Integer)item.get(1);
			String percent = twoDForm.format( ( count/(total *1.0))*100);
			item.add(percent);
			}
		Collections.sort(summary,new DurableManager.SummaryComparator());
		((Vector)summary).insertElementAt(Integer.valueOf(total),0);
		return summary;
		}
	
	private static class SummaryComparator implements Comparator{
    public int compare(Object obj1,Object obj2)
    	{ 
    	return ((String)((List)obj1).get(0)).compareTo((String)((List)obj2).get(0)); 
    	}

	}
	
	public static class StatusListComparator implements Comparator
		{
		int idx;
		public StatusListComparator(int fieldIdx)
			{
			idx = fieldIdx;
			}
		
		public int compare(Object obj1,Object obj2)
			{
			if  ( !(obj1 instanceof List )  ||  !(obj2 instanceof List)){return 0;}
			String val1,val2;
			Object tmp =((List)obj1).get(idx);
			if( tmp == null){tmp = "";}
			val1 = tmp.toString();
			tmp =((List)obj2).get(idx);
			if( tmp == null){tmp = "";}
			val2 = tmp.toString();
			return val1.compareTo(val2);
			}
		}
	
	/**
	 * @param equipments is the list of equipments to get the total from
	 * @param field is the field to use to make the totals
	 * @return a summary of the equipments passed by the field passed 
	 * */
	public static Map getTotalsBy(List equipments,String field,String value)
		{
		HashMap totals = new HashMap();
		Machine mac;
		String category;
		Object tmpVal;
		Integer count;
		for(int i=0;equipments!=null&& i<equipments.size();i++)
			{
			 mac = (Machine)equipments.get(i);
			 boolean isList = false;
			 String separator = null;
			 String durable_id = (String)mac.get("DURABLE_ID");
			 tmpVal = mac.get(field);
			 if(tmpVal==null){tmpVal="";}
			 if(durable_id!=null)
				 {
				 Machine durable = stateStamper.getMachine(durable_id);
				 if(durable!=null)
					 {
					 EquipmentType equipmentType = durable.getEquipmentType();
					 EquipmentAttribute attribute = equipmentType.getAttribute(field);
					 if(attribute!=null)
						 {
						 isList = Boolean.valueOf(attribute.getConfigOptions().get("isList"));
						 separator = attribute.getConfigOptions().get("separator");
						 }
					 else
						 {
						 if(equipmentType.getConfigOptions().containsKey(field+".isList")
							  &&equipmentType.getConfigOptions().containsKey(field+".separator"))
							 {
							 isList = Boolean.valueOf(equipmentType.getConfigOptions().get(field+".isList"));
							 separator = equipmentType.getConfigOptions().get(field+".separator");
							 }
						 }
					 }
				 }
			 String []items = getElementsFromStr(tmpVal.toString(),isList,separator);
			 for(int j=0;j<items.length;j++)
				 {
				 category = items[j];
				 if(value != null && !category.equals(value)){continue;}
				 if(totals.containsKey(category))
					 {
					 count= (Integer)totals.get(category);
					 count++;
					 }
				 else
					 {
					 count = Integer.valueOf(1);
					 }
				 totals.put(category, count);
				 }
			}
		return totals;
		}
	
	private static String[] getElementsFromStr(String str,boolean isList,String separator)
		{
		String[] temp = null;
		if (str.startsWith("[") && str.endsWith("]"))
			{
			str = str.replace("[", "").replace("]", "");
			}
		if (Boolean.TRUE.equals(isList))
			{
			if(separator!=null)
				{
				temp = str.split(separator);
				}
			else
				{
				temp = new String[]{str};
				}
			}
		else
			{
			temp = new String[]{str};
			}

		return temp;
		}
	
	public static DurableEmail buildBusinessRuleAlarmEmail(CurrentDurableStatus durableStatus, String emailaddress, BusinessRule businessRule) throws Throwable{
		
		DurableEmail email = new DurableEmail();
		EquipmentType type = durableStatus.getEquipmentType();								
		email.setSubject(BUSINESSRULE_EMAIL_SUBJECT + " " + type.name + " " + durableStatus.eIName);
		String content = BUSINESSRULE_EMAIL_CONTENT_PART1 + " " + type.name + " " + durableStatus.eIName + " \r\n\r\n";
		content = content + BUSINESSRULE_EMAIL_CONTENT_PART2 + " \r\n";		
		Vector vector = businessRule.getConditions();		
		for (int i =0; i <vector.size(); i++){			
			BusinessRuleCondition condition = (BusinessRuleCondition)vector.get(i);
			Object percentageStr = condition.getPercentageStr();
			if (percentageStr == null) percentageStr = ""; 
			String value2 = (String)(durableStatus.get(condition.getCondValuewithoutPercentange()));
			if(value2==null)value2=condition.getCondValue();			
			content = content + condition.getAttribute() + " " + condition.getCondition() + " " + percentageStr + " "+ value2 + " \r\n";
			content = content + "For this " +  type.name + " " + durableStatus.eIName + " : " + condition.getAttribute() + " is " + durableStatus.get(condition.getAttribute()) + " \r\n";						
		}		
		content = content + " \r\n" +BUSINESSRULE_EMAIL_WARNING;
		email.setContent(content);
		email.setTo(emailaddress,durableStatus);		
		return email;		
	}	
	
	public static Map getTransitionConditions(Transitions transitions,
			String condition, IStatus status) throws Throwable {
		Hashtable returnhashtable = new Hashtable();
		if (transitions == null || !transitions.hasConditions()) {
			return returnhashtable;
		}
		List conditions = new ArrayList();
		if ("attachment".equalsIgnoreCase(condition)) {
			List tmp = transitions.getPreConditions();
			for (int i = 0; i < tmp.size(); i++) {
				TransitionCondition trancondition = (TransitionCondition) tmp
						.get(i);
				if ("Attachment".equalsIgnoreCase(trancondition.getCondValue())) {
					//conditions.add(trancondition);
					returnhashtable.put(trancondition.getAttribute(),trancondition );
				}
			}
			
		} else {
			conditions = transitions.getConditionsByConditional(condition);

			for (int i = 0; i < conditions.size(); i++) {
				TransitionCondition trancondition = (TransitionCondition) conditions
						.get(i);
				String condvalue = trancondition.getCondValue();
				String[] convalues;
				if (condvalue != null) {
					Object tmpobj = status.get(condvalue);
					if (tmpobj != null) {
						String tmpvalue = tmpobj.toString();
						if (tmpobj instanceof Vector) {
							tmpvalue = tmpvalue.substring(1,
									tmpvalue.length() - 1);
						}
						convalues = tmpvalue.split(",");
					} else {
						convalues = condvalue.split(",");
					}
				} else {
					continue;
				}

				Vector convaluesVector = new Vector();
				for (int j = 0; j < convalues.length; j++) {
					convaluesVector.add(convalues[j]);
				}
				returnhashtable.put(trancondition.getAttribute(),
						convaluesVector);
			}
		}
		return returnhashtable;
	}

	/**
	 * @return a Map with the items to display in DurableTransaction UI
	 * the elements of the map are
	 * stateList
     * currentStatus
     * the list of parents
     * prop status
     * the list of attributes
     * multiselection
	 * 
	 * */
	public static Map getTransitionDetails(String durableId,Administrator user,String toSubstate)throws Throwable
		{
		Map result= new HashMap();
		Machine machine = stateStamper.getMachine(durableId); 
		CurrentDurableStatus currentDurableStatus =  getDurableDetails(durableId);
		Substate fromSubState = currentDurableStatus.getSubstate();
		result.put("states",getNextSubstates(fromSubState, user.isAdministrator() ));
		result.put("currentStatus",currentDurableStatus);
		List parents = stateStamper.getParentList(machine);
		result.put("parents",parents);			
		List locations = getLocationList(durableId);
		result.put("locations",locations);		
		Substate toSubState = null;
		if (toSubstate != null && !toSubstate.equals(""))
			{
			toSubState = getDefaultToSubstate(Integer.valueOf(toSubstate));
			}
		Transitions transition = getTransition(fromSubState, toSubState);
		Map transAttributes =  getTransitionFields(machine, transition);
		result.put("tranAttributes",transAttributes);
		Map multipleSelectedConditions  = getTransitionConditions(transition, TransitionCondition.MULTIPLE_IN, currentDurableStatus);
		Map singleSelectedConditions = getTransitionConditions(transition, TransitionCondition.SINGLE_IN, currentDurableStatus);
		Map attachmentConditions = getTransitionConditions(transition, "attachment", currentDurableStatus);
		result.put("multiSelection",multipleSelectedConditions);
		result.put("singleSelection",singleSelectedConditions);		
		result.put("attachment",attachmentConditions);	
		return result;
		}
	
	private static List getDisplayOnlyAttributes(Transitions transition,EquipmentType eqType)
		{
		List displayAttributes = new Vector();
		if(transition == null || ! transition.hasConditions()){return displayAttributes;}
		List conds = transition.getConditionsByConditional(TransitionCondition.DISPLAY);
		for(int i=0;i<conds.size();i++)
			{
			String attribute = ((TransitionCondition)conds.get(i)).getAttribute();
			EquipmentAttribute eqAttribute = eqType.getAttribute(attribute);
			displayAttributes.add(eqAttribute);
			}
		return displayAttributes;
		}
	
	/**
	 * @return a list with the fields to be displayed for a specific transition
	 * */
	private static Map getTransitionFields(Machine machine,Transitions transition)
		{
		Map transAttributes= new LinkedHashMap();
		List UIconditions = transition.getPreConditions();
		Map field=null;
		TransitionCondition cond=null;
		Object tmpValue = null;
		String name,label=null,display,value,condValue;
		EquipmentType eqType = machine.getEquipmentType();
		for(int i=0;i<UIconditions.size();i++)	
			{
			cond = (TransitionCondition)UIconditions.get(i);
			field = new HashMap();
			name = cond.getAttribute();			
			EquipmentAttribute equipmentAttribute = eqType.getAttribute(name);
			if (equipmentAttribute!=null&&equipmentAttribute.isAttachment()){
				field.put(TRANSITION_FIELD_NAME,name);
				Attachment attach = (Attachment)machine.getAttachmentLatestAttachmentByName(name);
				display = cond.getCondition();
				if (attach ==null){
					label= TRANSITION_FIELD_NOATTACHMENT;
					value = TRANSITION_FIELD_NOATTACHMENT;					
				}
				else {
					label= attach.getDisplayFilename();
					value = attach.getFileLocation();
				}
				
				field.put(TRANSITION_FIELD_DISPLAY,display);				
				field.put(TRANSITION_FIELD_VALUE, value);
				field.put(TRANSITION_FIELD_LABEL,label);		
				transAttributes.put(name,field);
				continue;
			}
			
			if(equipmentAttribute!=null&&equipmentAttribute.getConfigOptions()!=null)
				{
				String subType = equipmentAttribute.getConfigOptions().get(TransitionCondition.SUB_TYPE);
				Boolean populate = Boolean.valueOf(equipmentAttribute.getConfigOptions().get(TransitionCondition.CHECK_NAME_POPULATE));
				if(populate&&TransitionCondition.SUB_TYPE_CORE_ID.equals(subType))
					{
					field.put(TRANSITION_FIELD_CHECKNAME,TRANSITION_FIELD_CHECKNAME);
					}
				}
			
			display = cond.getCondition();
			condValue = cond.getCondValue();
			field.put(TRANSITION_FIELD_NAME,name);
			field.put(TRANSITION_FIELD_DISPLAY,display);
			boolean counterBar = name.contains("/");
			if(counterBar)
				{
				value = getCounterBarValue(machine,name);
				label = getCounterBarLabel(eqType, name);
				field.put(TRANSITION_FIELD_DISPLAY,TransitionCondition.DISPLAY);
				field.put(TRANSITION_FIELD_STATUS,getCounterBarStatus(value));
				}
			else
				{
				if(TransitionCondition.CLEAR.equalsIgnoreCase(condValue))
					{
					value ="";
					}
				else
					{
					tmpValue = machine.get(cond.getAttribute());
					value = tmpValue!=null?tmpValue.toString():"";
					}
				EquipmentAttribute attr = eqType.getAttribute(name);
				if(attr !=null){label = attr.getAttributeAlias();}
				if(label == null || label.equals("")){label=name;}
				}
			field.put(TRANSITION_FIELD_VALUE, value);
			field.put(TRANSITION_FIELD_LABEL,label);
			transAttributes.put(name,field);
			} 	
		return transAttributes;
		}
	
	private static String getCounterBarStatus(String barValue)
		{
		int start = barValue.indexOf("(");
		int end   = barValue.indexOf("%)");
		return getPercentageStatus(Double.valueOf(barValue.substring(start+1, end)));
		}
	
	private static String getCounterBarValue(Machine machine, String label)
		{
		int countIdx = label.indexOf(":");
		int slashIdx = label.indexOf("/");
		if( countIdx == -1 ||  slashIdx == -1){return "";}
		String counter = label.substring(0,countIdx);
		String usage   = label.substring(countIdx+1,slashIdx);
		String limit   = label.substring(slashIdx+1);
		String usageName = counter+EquipmentAttribute.SEPARATOR+usage;
		String limitName = counter+EquipmentAttribute.SEPARATOR+limit;
		usage = (String)machine.get(usageName);
		limit = (String)machine.get(limitName);
		usage = usage==null?"0":usage;
		limit = limit==null?"0":limit;
		String percentage = getPercentage(usage, limit);
		//recompute the time based attributes as dd-hh-mm		
		EquipmentType eType = stateStamper.getEquipmentType(machine.equipmentType_pk);
		if (eType.isTimeRelatedAttribute(usageName))
			usage = FiscalDate.getTimeBasedThresholdValueWithElapse(eType,usageName,usage);							
		if (eType.isTimeRelatedAttribute(limitName))
			limit = FiscalDate.getTimeBasedThresholdValueWithElapse(eType,limitName,limit);		
		String value = usage +"/"+limit+" ("+percentage+"%)";
		return value;
		}
	
	public static String getPercentage(String numerator, String denominator) {
		DecimalFormat twoDForm = new DecimalFormat("#.##");
		String result = "0";
		try {
			BigDecimal usage = new BigDecimal(numerator);
			BigDecimal limit = new BigDecimal(denominator);
			BigDecimal percentage = BigDecimal.ZERO;
			if (!limit.equals(BigDecimal.ZERO)) {
				percentage = usage.divide(limit,4,RoundingMode.HALF_UP).multiply(BIG100);
				
			}
			result = twoDForm.format(percentage);
			result = result.replace(".00", "");
		} catch (Exception nfw) 
			{
			logger.error(" getPercentage("+numerator+","+denominator+")",nfw);
			} 
		return result;
	}
	
	private static String getCounterBarLabel(EquipmentType eqType,String name)
		{
		String label = null;
		int countIdx = name.indexOf(":");
		int slashIdx = name.indexOf("/");
		String counter = name.substring(0,countIdx);
		String usage   = name.substring(countIdx+1,slashIdx);
		String limit   = name.substring(slashIdx+1);
		usage = counter + EquipmentAttribute.SEPARATOR + usage;
		limit = counter + EquipmentAttribute.SEPARATOR + limit;
		EquipmentAttribute eAttributeUsage = eqType.getAttribute(usage);
		
		if (eAttributeUsage!=null&&eAttributeUsage.getAttributeAlias()!=null&&!eAttributeUsage.getAttributeAlias().equalsIgnoreCase(""))
			{
			usage = eAttributeUsage.getAttributeAlias(); 	
			}
		
		EquipmentAttribute eAttributeLimit = eqType.getAttribute(limit);
		
		if (eAttributeLimit!=null&&eAttributeLimit.getAttributeAlias()!=null&&!eAttributeLimit.getAttributeAlias().equalsIgnoreCase(""))
			{
			limit = eAttributeLimit.getAttributeAlias(); 	
			}
		if (limit.contains(EquipmentAttribute.SEPARATOR ) && usage.contains(EquipmentAttribute.SEPARATOR))
			{
			label = name;
			}
		else{
			label = usage + "/" + limit;
			}

		return label;
		}
	
	private static String getPercentageStatus(Double percent)
		{
		String percentageStatus;
		if(percent<=50){percentageStatus=TRANSITION_PERCENTAGE_STATUS_OK;}
		else if(percent<=100){percentageStatus=TRANSITION_PERCENTAGE_STATUS_WARNING;}
		else {percentageStatus=TRANSITION_PERCENTAGE_STATUS_CRITICAL;}
		return percentageStatus;
		}
	
	private static Substate getDefaultToSubstate(Integer substatePk)throws Throwable 
		{
		Substate defaultSub=null;
		Substate toSubState = new Substate();
		toSubState.primaryKey = substatePk;
		Vector tmpvector = Persistence.search(toSubState);
		if (tmpvector !=null&&!tmpvector.isEmpty()){defaultSub = (Substate)tmpvector.get(0);}
		return defaultSub;
		}
	
	/**
	 * get Location list for a given durable Id
	 * */
	public static List getLocationList(String durableId) throws Throwable
		{
			Machine durable = stateStamper.getMachine(durableId);
			return getLocationList(durable);
		}
	
	/**
	 * get Location list for a given durable
	 * */
	
	public static List getLocationList(Machine durable) throws Throwable
		{		
		List locations = (Vector) Persistence.search(new Location());
		List filteredLocations = new ArrayList();

		if (durable.location != null)
			{
			filteredLocations.add(durable.location);
			}
		for (int i = 0; i < locations.size(); i++)
			{
			Location loc = (Location) locations.get(i);
			if ((durable.location == null || !durable.location.equals(loc.getName())) && loc.isSupported(durable)
					&& loc.hasRoom())
				{
				filteredLocations.add(loc.getName());
				}
			}
		Collections.sort(filteredLocations);
		return filteredLocations;
		}
	
	private static Map getNextSubstates(Substate fromSubState, boolean allStates) throws Throwable
		{
		Map toSubStates = new HashMap();
		List defined = getDefinedNextSubstate(fromSubState);
		toSubStates.put("defined", defined);
		if (allStates)
			{
			Substate sub = new Substate();
			sub.equipmenttypePK = fromSubState.equipmenttypePK;
			List forced = (List) Persistence.search(sub);
			forced.removeAll(defined);
			toSubStates.put("forced", forced);
			}
		return toSubStates;
		}
	
	private static Object[] convertDurableStatustoArray(Machine durable, String[] attributes)
		{
		Object[] results = new Object[attributes.length];
		for (int i = 0; i < attributes.length; i++)
			{
			Object tmp = durable.get(attributes[i]);
			if (tmp==null) 
				{
				results[i] =""; 
				}
			else 
				results[i] = tmp;
			}
		return results;
		}
	/**
	 * get list of unique value to populate a filter dropdown list from 
	 * durables in the memory according to the filter name and equipment type
	 * @param equipmentType the equipmentType
	 * @param filter the name of filter
	 * @return A set contains a list of unique value 
	 * @throws Throwable
	 */
	public static Set getFilterValue (EquipmentType equipmentType, Area area,  String filter) throws Throwable{
		Map durables = stateStamper.getMachines(equipmentType,area.getPrimaryKey());
		Set durableKeyset = durables.keySet();
		Iterator it = durableKeyset.iterator();
		Set result = new HashSet();
		while (it.hasNext())
			{
			Machine durable = (Machine) (durables.get(it.next()));	
			Object tmpvalue = durable.get(filter);
			if (tmpvalue!=null)
				{
				result.add(tmpvalue);
				}
			}
		return result;	
	}
	
	
	/**
	 * get list of unique value to populate a filter dropdown list from 
	 * results  according to the filter name and equipment type
	 * @param equipmentType the equipmentType
	 * @param filter the name of filter
	 * @return A set contains a list of unique value 
	 * @throws Throwable
	 */
	public static Set getFilterValuefromHistory(EquipmentType equipmentType, Area area, String filter, List resultList) throws Throwable
		{
		if (filter.equalsIgnoreCase("Durable_ID"))
			{
			return getFilterValue(equipmentType,area,filter);
			}
		else
			{
			Iterator it = resultList.iterator();
			Set result = new HashSet();
			while (it.hasNext())
				{
				DurableStatus durableStatus = (DurableStatus) (it.next());
				Object tmpvalue = durableStatus.get(filter);
				if (tmpvalue != null)
					{
					result.add(tmpvalue);
					}
				}
			return result;
			}
		}

	public static EquipmentType getEquipmentTypebyDurableID(String durableID){
		Machine durable = stateStamper.getMachine(durableID);
		return durable.getEquipmentType();
	}

	public static Vector getDurablesForReport(EquipmentType equipmentType, Area area, Map filter, String orderBy, Boolean isAscendent, String[] attributes) throws Throwable
		{		
		Vector result = getDurablesForReport(equipmentType, area, filter, orderBy, isAscendent);
		Vector resultVector = new Vector();
		for (int i = 0; i<result.size(); i++) 
			{
			resultVector.add(convertDurableStatustoArray((Machine)result.get(i),attributes));
			}
		return resultVector;
		}	
	
	public static Vector getDurablesForReport(EquipmentType equipmentType, Area area, Map filter, String orderBy, Boolean isAscendent) throws Throwable
		{
		Map durables = null;
		if (equipmentType == null)
			{
			durables = stateStamper.getAllDurables();
			}
		else
			{
			durables = stateStamper.getMachines(equipmentType,area.getPrimaryKey());
			}
		Set durableKeyset = durables.keySet();
		Iterator it = durableKeyset.iterator();
		Vector result = new Vector();
		while (it.hasNext())
			{
			Machine durable = (Machine) (durables.get(it.next()));
			if (filterDurable(durable, filter))
				{
				result.add(durable);
				}
			}
		String order = isAscendent?"ASC":"DESC";
		Collections.sort(result,new IStatusComparator(orderBy,order));
		return result;
		}
	
	private static boolean filterDurable(Machine durable,Map filter)
		{
		boolean match = true;
		if(filter == null || filter.size() == 0){return match;}
		Set filterSet = filter.keySet();
		Iterator iterator = filterSet.iterator();
		while (iterator.hasNext() && match)
			{
			String key = (String) iterator.next();
			String value = filter.get(key).toString();
			if (value ==null || value.equals("")){continue;} 
			String statusValue =null;
			Object statusValueObj =  durable.get(key);
			if (statusValueObj == null)
				{
				match = false;
				}
			else {
				statusValue = statusValueObj.toString();
				}
			
			if (statusValue == null)
				{
				match = false;
				}
			else if (statusValue.contains(","))
				{
				match = statusValue.contains(value);
				}
			else if(value.contains(",") )
				{
				match = value.contains(statusValue);
				}
			else if(value.endsWith("*"))
				{
				match = statusValue.startsWith(value.substring(0, value.length()-1));
				}
			else
				{
				match = statusValue.equals(value);
				}
			}
		return match;
		}

	public static boolean associateDurable(String durableId, String parantId, String coreId){
		TransactionManager txManager = new TransactionManager();
		try
			{
			txManager.beginTransaction();
			Machine durable = (Machine) stateStamper.getMachine(durableId).clone();
			durable.workstation = parantId;
			DurableStatus status = (DurableStatus) durable.context.get("durableStatus");
			durable.context.put("OPERATOR_ID", coreId);
			// since we are going to store the Durable, we need to pass the
			// coreID of the operator.
			stateStamper.storeDurable(durable, txManager);
			txManager.executeBatch();
			return true;
			}
		catch (Throwable ex)
			{
			txManager.rollback();
			logger.error("associateDurable("+durableId+","+parantId+","+coreId,ex);
			return false;
			}
		finally 
			{
			txManager.close();
			}
	}

	private static Map getSelectedFilters(HttpServletRequest request, Map filters)throws Throwable
	{
	Map selectedFilter = new Hashtable();
	Iterator iterator = filters.keySet().iterator();
	while (iterator.hasNext())
		{
		String filter = (String)iterator.next();
		String tmpSelected = request.getParameter(filter);
		if (tmpSelected == null ){tmpSelected="";}
		selectedFilter.put(filter, tmpSelected);
		}
	if(request.getParameterMap().containsKey("activity"))
		{
	    EquipmentType eqType= getCurrentEquipmentType(request);
	    String activity = (String)request.getParameter("activity");
	    List transitions = stateStamper.getTransitionsByActivity(eqType, activity);
	    if(transitions.size()>0)
	    	{ 
	    	Substate from = ((Transitions)transitions.get(0)).getFromState();
	    	selectedFilter.put("STATE",from.e10);
	    	selectedFilter.put("SUBSTATE",from.substate);
	    	}
	   }
	return selectedFilter;
	}

	public static boolean addcomment(String durable_id, String status_PK, String user, String content) throws Throwable
		{
		try
			{
			Date now = new Date();
			
			Machine durable = stateStamper.getMachine(durable_id);
			DurableStatus status = (DurableStatus) durable.context.get("durableStatus");
			if (!((new Integer(status_PK)).equals(status.STATUS_PK))){
				return false;
			}
			else {
				status.putNewComments(user, formatter.format(now),content);
				Persistence.store(status);
				return true;
			}			
			}
		catch (Throwable ex)
			{
			logger.error("addcomment("+durable_id+","+status_PK+","+user+","+content+")",ex);
			return false;
			}
		}
	
	public static Vector generateDurableSimpleReport(HttpServletRequest request, HttpServletResponse response, boolean realtime, Report report) throws Throwable
		{
		Map filters = new Hashtable();
		Map selectedfilter = new Hashtable();
		HttpSession session = request.getSession(true);
		String durableId = request.getParameter("durable_id");
		EquipmentType eqType = report.getEquipmentType();
		if (realtime)
			{
			filters = StateStamper.reports.getReportFilters(report, null);
			selectedfilter = getSelectedFilters(request, filters);
			report.setFilters(selectedfilter);
			}

		Hashtable data = StateStamper.reports.generateDurableReport(report, response, false, false);
		List resultList = new ArrayList();
		if (realtime)
			{
			resultList = StateStamper.reports.getSortedRealtimeReportData(report, data, eqType);
			}
		else
			{
			Map history = StateStamper.reports.getHistoryReportDataAsDurableStatus(report, data);
			List values = new ArrayList(history.values());
			for (int i = 0; i< values.size();i++)
				{
				resultList.addAll((List) values.get(i));
				}
			resultList = DurableManager.filterDurableStatusByEquipmentType(eqType,report.getArea(),resultList);
			filters = StateStamper.reports.getReportFilters(report, resultList);
			selectedfilter = getSelectedFilters(request, filters);
			resultList = StateStamper.reports.getFilteredHistoricalReport(resultList, selectedfilter);
			report.setColumns(StateStamper.reports.getReportDefinition(report));
			String orderCriteria[] = report.getColumns()[3].split(",");
			String orderField = orderCriteria[0];
			String asc = Boolean.valueOf(orderCriteria[1])?"ASC":"DESC";
			Collections.sort(resultList,new IStatusComparator(orderField,asc));
			}

		List reportHeaders = StateStamper.reports.getDurableReportHeaders(report);
		List reportHeadersDisplay = DurableManager.getDisplayHeaders(reportHeaders,eqType);
		String activity = request.getParameter("activity");
		if (activity==null||activity.equalsIgnoreCase("")||activity.equalsIgnoreCase("null")){
			activity = null;
		}
		Vector output = new Vector();
		output.add(reportHeaders);
		output.add(resultList);
		output.add(filters);
		output.add(selectedfilter);
		output.add(report);
		output.add(activity);
		output.add(reportHeadersDisplay);
		return output;
		}

	public static Vector generateDurableSimpleReport(HttpServletRequest request, HttpServletResponse response, boolean realtime) throws Throwable
		{
		Report report = buildReport(request, response, realtime);
		return generateDurableSimpleReport(request, response, realtime, report);

		}

	public static Report buildReport(HttpServletRequest request, HttpServletResponse response, boolean realtime) throws Throwable
		{
		Report report = new Report();
		report.reportType = request.getParameter("reportType");
		report.grouping = request.getParameter("grouping");
		if (realtime)
			{
			report.period = "Realtime";
			}
		else
			{
			report.period = "raw";
			}
		report.setEquipmentType(getCurrentEquipmentType(request));
		report.setArea(getCurrentArea(request));
		report = getDefaultReport(report);
		return report;
		}
	
	public static boolean validateLocation(Machine durable, String newLocation, String oldLocation)throws Throwable
		{
		Location newLoc =null;
		if (newLocation != null && !newLocation.equals(oldLocation))
			{
			newLoc = stateStamper.getLocationByName(newLocation);
			if( newLoc ==null || !newLoc.isSupported(durable) ){throw new RuntimeException("Location not supported");}
			if( !newLoc.hasRoom()){throw new RuntimeException("Location does not have room for this item");}
			}
		return true;
		}
	
	public static boolean updateLocationCount(Machine durable, String newLocation, String oldLocation,TransactionManager txManager) throws Throwable
		{
		boolean storeNewLoc = false;
		boolean storeOldLoc = false;
		Location newLoc =null;
		Location oldLoc =null;
		if (newLocation != null && oldLocation != null && newLocation.equals(oldLocation))
			{
			return true;
			}
		else
			{
			if (newLocation != null && !newLocation.equals(""))
				{
				newLoc = stateStamper.getLocationByName(newLocation);
				if (newLoc.isSupported(durable)&&newLoc.addItem())
					{		
					storeNewLoc = true;
					}
				else
					{
					return false;
					}
				}
			if (oldLocation != null && !oldLocation.equals(""))
				{
				oldLoc = stateStamper.getLocationByName(oldLocation);
				oldLoc.removeItem();
				storeOldLoc = true;				
				}
			}
		if (storeNewLoc)txManager.store(newLoc);
		if (storeOldLoc)txManager.store(oldLoc);
		return true;
		}
	
	
	public static void updateLocationName(String newName, String oldName, TransactionManager txManager) throws Throwable
		{
			Iterator it =stateStamper.getMachines().keySet().iterator();
			while (it.hasNext()) {
				String durableName = (String) it.next();
				Machine durable = stateStamper.getMachine(durableName);
				if (durable.location !=null && durable.location.equals(oldName))
					{						
						durable.location = newName;
						DurableStatus status = (DurableStatus)durable.context.get("durableStatus");
						if (status!=null){
							status.location = newName;
							txManager.store(status);
						}						
						txManager.store(durable);						
					}
			}
		}
	
	/**
	 * check if there is a report definition for report passed if not it updates the report grouping
	 * to use the default grouping (DURABLE) for all durables
	 * @param report to verify it has a definition
	 * @return the updated report  
	 * */
	public static Report getDefaultReport(Report report)
		{
		if(stateStamper.reports.getReportDefinition(report) == null)
			{
			report.grouping = "DURABLE";
			}
		return report;
		}
	
	/**
	 * validate a Durable according to the EI Validation Business Rule 
	 * @param durableId the durableId to validate
	 * @return a Map containing the result from the validation of the durable
	 * the map contains the following keys
	 * EQUIPMENT_NAME:the name of the equipment validated same than the durableId passed
	 * TYPE: the equipment type of the equipment. 
	 * CLASS: class of the equipment
	 * STATE:E10 State
	 * VALIDATION:boolean value of the validation of the durable according to the EI Validation Business Rule 
	 * INVALID_CAUSE:a message explaining the cause why this item is not valid, only defined when VALIDATION = false 
	 * **/
	public static Map validateDurable(String durableId)
		{
		Map result = new HashMap(); 
		result.put("EQUIPMENT_NAME", durableId);
		Machine durable = stateStamper.getMachine(durableId);
		if(durable == null)
			{
			result.put("VALIDATION","false");
			result.put("INVALID_CAUSE", durableId +" not defined ");
			}
		else
			{
			result.put("EQUIPMENT_NAME", durableId);
			result.put("TYPE", durable.getEquipmentType().name);
			result.put("CLASS", durable.type);
			result.put("STATE", durable.state);
			List validation = isValidDurable(durable); 
			Boolean valid = (Boolean) validation.get(0);
			result.put("VALIDATION",String.valueOf(valid));
			if(!valid){result.put("INVALID_CAUSE" , validation.get(1));}
			}
		return result;
		}
	
	/**
	 * @param durable the durable to validate
	 * @return a List containing 
	 * 1) boolean indicating if the machine met the conditions defined in the EI Validation business rule for this equipment type
	 * 2) a String with the reason this machine is not valid
	 * */
	private static List isValidDurable(Machine durable)
		{
		List res = new ArrayList();
		Boolean valid = true;
		StringBuffer msg= new StringBuffer();
		try{
		List rules = durable.getEquipmentType().getBusinessRulesBy("EI Validation");
		if(rules != null && rules.size() > 0)
			{
			BusinessRule eiValidation =  (BusinessRule)rules.get(0);
			valid = eiValidation.conditionsTrue(durable);
			if(!valid)
				{
				List conds = eiValidation.getConditions();
				msg = new StringBuffer("Condition not met : "); 
				for(int i = 0; conds !=null && i< conds.size();i++)
					{
					msg.append(((BusinessRuleCondition)conds.get(i)).toExtString() +";" );
					}
				}
			}
		}catch(Exception e){e.printStackTrace();}
		res.add(valid);
		res.add(msg.toString());
		return res;
		}
	
	public static String getWorkWeekbasedCounterbarValue (String value) {
		if(value.equalsIgnoreCase("-")){
			return value;						
		}	
		int indexLeftBrac = value.indexOf('('); 
		int indexSlash = value.indexOf("\\");
		int indexRightBrac = value.indexOf(')'); 
		String difference = "0";
		String usageValue =  value.substring(0,indexSlash);  
		String limitValue =  value.substring(indexSlash+1 ,indexLeftBrac-1);
		usageValue = (usageValue==null||usageValue.equalsIgnoreCase(""))?"0":usageValue;
		limitValue = (limitValue==null||limitValue.equalsIgnoreCase(""))?"0":limitValue;
		
		String workWeek = "";
		Date now = new Date();
		try{
			if (limitValue.equalsIgnoreCase("0")) {
				workWeek = "-";
			} else {
				Long differenceLong = Long.parseLong(limitValue)
						- Long.parseLong(usageValue);
				Long nowinLong = now.getTime();
				nowinLong = nowinLong + differenceLong;
				Date tmpDate = new Date(nowinLong);
				FiscalDate aux = new FiscalDate("Week",
						formatter.format(tmpDate));
				workWeek = aux.fDate;
			}
		}catch (Exception ex) {
			ex.printStackTrace();
		}finally{
			return workWeek;
		}	
	}
	
	public static String getTimebasedCounterbarValue(String value) {
		if(value.equalsIgnoreCase("-")){
			return value;						
		}		
		int indexLeftBrac = value.indexOf('('); 
		int indexSlash = value.indexOf("\\");
		int indexRightBrac = value.indexOf(')'); 
		String difference = "0";
		String usageValue =  value.substring(0,indexSlash);  
		String limitValue =  value.substring(indexSlash+1 ,indexLeftBrac-1);
		usageValue = (usageValue==null||usageValue.equalsIgnoreCase(""))?"0":usageValue;
		limitValue = (limitValue==null||limitValue.equalsIgnoreCase(""))?"0":limitValue;
		
		try{
			Long differenceLong = Long.parseLong(limitValue) - Long.parseLong(usageValue);
			if (differenceLong >= 0) 			
				difference = FiscalDate.getElapsedTimeinDays(differenceLong);
			else 
				difference = "-" + FiscalDate.getElapsedTimeinDays(0L- differenceLong);
			usageValue = FiscalDate.getElapsedTimeinDays(Long.parseLong(usageValue));
			limitValue = FiscalDate.getElapsedTimeinDays(Long.parseLong(limitValue));
		}catch (Exception ex) {
			ex.printStackTrace();
			return "";
		}finally{
		}
		return usageValue +" days\\"+limitValue + " days("+ difference +" days left)";
	}
	
		
	public static String getCounterbarColor(Machine durable, EquipmentType eqType, String header){
		String value = (String)durable.get(header);
		List rules = eqType.getBusinessRulesBy(BusinessRule.OPEN_REPORT);
		if(!header.startsWith("COUNTERBAR")||value.equalsIgnoreCase("-") ) {
			return "";						
		}
		
		int indexLeftBrac = header.indexOf('('); 
		int indexSlash = header.indexOf("\\");
		int indexRightBrac = header.indexOf(')'); 
				
		String usage = header.substring(indexLeftBrac+1,indexSlash);
		
		String limit = header.substring(indexSlash+1,indexRightBrac);  
		
		indexLeftBrac = value.indexOf('('); 
		indexSlash = value.indexOf("\\");
		indexRightBrac = value.indexOf(')'); 
		
		String usageValue =  value.substring(0,indexSlash);  
		String limitValue =  value.substring(indexSlash+1 ,indexLeftBrac-1);
		
		usageValue = (usageValue==null||usageValue.equalsIgnoreCase(""))?"0":usageValue;
		limitValue = (limitValue==null||limitValue.equalsIgnoreCase(""))?"0":limitValue;
		//limitValue 
		if (limitValue.equals("0")) {
			return "";
		}
		
		durable.setEquipmentType(eqType);
		String usageRealName = durable.getRealname(usage);
		String limitRealName = durable.getRealname(limit);
		
		if (durable.dynamicAttributes != null)
			{
			Set<String> keys = durable.dynamicAttributes.keySet();
			for (String key : keys)
				{
				String attrName = key;
				Object attrValue = durable.dynamicAttributes.get(key);
				durable.context.put(attrName, attrValue);
				}
			}
		durable.context.put(usageRealName,usageValue);
		durable.context.put(limitRealName,limitValue);
		durable.setEquipmentType(null);//remove the EquipmentType to prevent get method query attributes instead from contexts;
		
		
		for (int i = 0 ; i <  rules.size(); i++) {
			BusinessRule rule = (BusinessRule) rules.get(i);			
			Vector conditions = rule.getConditions();			
			if (conditions !=null){
				for (int j = 0 ; j <  conditions.size(); j++) {					
					BusinessRuleCondition condition = (BusinessRuleCondition)conditions.get(j);
					String attribute = condition.getAttribute();
					if (attribute==null||!attribute.equalsIgnoreCase(usageRealName)){
						continue;
					}else {						
						break;
					}					
				}				
			}			
			else {
				continue;
			}
						
			if (rule.conditionsTrue(durable)){					
				Vector actions = rule.getActions();							
				BusinessRuleAction action = (BusinessRuleAction)actions.get(0);
				String actionName = action.getAction();
				if (actionName!=null && actionName.equalsIgnoreCase(BusinessRuleAction.ALARM_REPORT) ) 
					{
					return action.getActionParameters().get(TransitionAction.PARAM_COLOR);
					}																				
			}
		}
		
		return "";
	}
	
	public static List getDisplayHeaders(List headers, EquipmentType eType) {
		List rules = eType.getBusinessRulesBy(BusinessRule.OPEN_REPORT);
		List returnList = new ArrayList();
		Map mappings = new HashMap();
		for (int i = 0; i < rules.size(); i++) {
			BusinessRule rule = (BusinessRule) rules.get(i);
			// no need condition check
			Vector actions = rule.getActions();
			if (actions == null || actions.isEmpty()) continue;
			BusinessRuleAction action = (BusinessRuleAction) actions.get(0);
			String actionName = action.getAction();

			if (actionName != null&& actionName.equalsIgnoreCase(BusinessRuleAction.CHANGE_HEADER)) 
				{
				Map<String, String> parameters = action.getActionParameters();
				Set<Entry<String, String>> entrySet = parameters.entrySet();
				for (Entry<String, String> temp : entrySet)
					{
					mappings.put(temp.getKey(), temp.getValue());
					}
				}
		}

		for (int i = 0; i < headers.size(); i++) {
			//"STATE-SUBSTATE" header is hard coded to convert to FROM STATE and TO STATE in the jsp page, so we do not rename them
			if (mappings.containsKey(headers.get(i))&& !"STATE-SUBSTATE".equalsIgnoreCase((String)headers.get(i))) {
				Object newName = mappings.get(headers.get(i));
				returnList.add(newName);
			} else {
				returnList.add(headers.get(i));
			}
		}
		return returnList;
	}
	
	public static List getMachinesMetBusinessRule(BusinessRule rule,List machines)
		{
		List result= new ArrayList();
		for(int i=0;machines!=null&&i<machines.size();i++)
			{
			Machine mac = (Machine)machines.get(i);
			try{
			if(rule.conditionsTrue(mac))
				{
				result.add(mac);
				}
			}catch(RuntimeException re){}
			}
		return result;
		}

	public static Map getMonitorResults(EquipmentType eqType,Area area,String monitor)throws Throwable
		{
		BusinessRule rule = (BusinessRule)eqType.getBusinessRuleByName(monitor);
		BusinessRuleAction  action = (BusinessRuleAction)((List)rule.getActionsBy(BusinessRuleAction.DISPLAY_MONITOR)).get(0);
		Map<String, String> params = action.getActionParameters();
		String def = (String)params.get("Display_definition");
		Map results = new HashMap();
		Report report = new Report();
		report.reportType = def;
		report.grouping = eqType.name ;
		report.period = "Realtime";
		report.setEquipmentType(eqType);
		report.setArea(area);
		String[] repDef = (String[])stateStamper.getReportDefinition(report);
		String orderBy = repDef[3];
		if(orderBy != null && orderBy.contains(",")){orderBy = orderBy.substring(1,orderBy.indexOf(","));}
		List<String> reportFields =  new ArrayList(Arrays.asList(repDef)).subList(7,repDef.length);//7 is the position where the fields start in the report definition
		List headers = new ArrayList(reportFields.size());
		for(int i = 0; i<reportFields.size();i++)
			{
			String field = reportFields.get(i);
			if (eqType.hasAttribute(field)) {headers.add(eqType.getAttributeAlias(field));}
			else                            {headers.add(field);}
			}
		BusinessRule criticalRule = (BusinessRule)eqType.getBusinessRuleByName(monitor.replace("_Main","_Critical"));
		String cond = "";
		List criticalResults = null;
		if(criticalRule != null)
			{
			cond = criticalRule.getConditionsAsStringWithAlias(eqType);
			results.put("critical_conditions",cond);
			criticalResults = getBusinessRuleResult(eqType,area,criticalRule,reportFields,orderBy);
			criticalResults.set(0,headers);
			results.put("critical_results",criticalResults);
			}
		BusinessRule warningRule = (BusinessRule)eqType.getBusinessRuleByName(monitor.replace("_Main","_Warning"));
		cond = "";
		List warningResults = null;
		if(warningRule != null)
			{
			cond = warningRule.getConditionsAsStringWithAlias(eqType);
			results.put("warning_conditions",cond);
			warningResults = getBusinessRuleResult(eqType,area,warningRule,reportFields,orderBy);
			warningResults.set(0,headers);
			results.put("warning_results",warningResults);
			}
		return results;
		}
	
	
	/**
	 * @return
	 * 
	 * */
	private static List getBusinessRuleResult(EquipmentType eqType,Area area,BusinessRule rule,List<String> fields,String orderBy)throws Throwable
		{
		List results = null;
		List macs = DurableManager.getMachinesMetBusinessRule(rule, new ArrayList(stateStamper.getMachines(eqType,area.getPrimaryKey()).values()));
		results = new ArrayList(macs.size());
		Object value;
		for(int i = 0;macs != null && i < macs.size();i++)
			{
			List row = new ArrayList(fields.size());
			Machine mac = (Machine)macs.get(i);
			for(int j = 0; j < fields.size(); j++)
				{
				value = mac.get(fields.get(j));
				if(value != null){value = value.toString();}
				else {value = "";}
				value = FiscalDate.getTimeBasedThresholdValueWithElapse(eqType,fields.get(j),value.toString());
				row.add(value.toString());
				}
			results.add(row);
			}
		
		int orderIdx = fields.indexOf(orderBy);
		if(orderIdx > -1){Collections.sort(results,new StatusListComparator(orderIdx));}
		results.add(0,fields);
		return results;
		}
	
	
	public static List<String> getAttachableChildren(String durableId)
		{
		List attachableChildren = new ArrayList();
		Machine durable = stateStamper.getMachine(durableId);
		if(durable == null){return attachableChildren;}
		Map params = getAttachableChildrenParams(durableId);
		EquipmentType equipmentType = stateStamper.getEquipmentTypeByName((String)params.get("CHILDREN_EQUIPMENT_TYPE"));
		Map durables = new HashMap();
		durables.putAll(stateStamper.getMachines(equipmentType,durable.getArea_pk()));
		filterAttachedDurables(durables);
		attachableChildren.addAll(durables.keySet());
		attachableChildren.addAll(durable.getChildrenByEquipmentType(equipmentType.name));
		Collections.sort(attachableChildren);
		return attachableChildren;
		}
	
	/**
	 * filter the durables that are attached from the map of durables received
	 * @param a map of durableId,durables this map is updated
	 * @return the updated map passed as parameter
	 * */
	private static Map filterAttachedDurables(Map<String,Machine> durables)
		{
		for(Iterator<Map.Entry<String, Machine>> it = durables.entrySet().iterator();it.hasNext();)
			{
			Map.Entry<String, Machine> entry = it.next();
			Machine durable = entry.getValue();
			if(durable.isAttached())
				{
				it.remove();
				}
			}
		return durables;
		}
	
	public static int getAttachableChildrenNumber(String durableId)
		{
		int childrenNumber = 0;
		Machine durable = stateStamper.getMachine(durableId);
		if(durable == null){return childrenNumber;}
		Map params =  getAttachableChildrenParams(durableId);
		String tmp = (String)params.get("CHILDREN_NUMBER");
		if(tmp != null && tmp.startsWith("$"))
			{
			Object obj = durable.get(tmp.substring(1)); 
			if(obj != null){tmp = obj.toString();}
			}
		try{childrenNumber = Integer.valueOf(tmp);}
		catch(NumberFormatException e){childrenNumber = 1;}
		return childrenNumber;
		}
	
	public static String getChildrenTypeForAttachChildAction(String durableId)
		{
		return String.valueOf(getAttachableChildrenParams(durableId).get("CHILDREN_EQUIPMENT_TYPE"));
		}
	
	/**
	 * returns a Map where the key is the Machine Id and the value the index to attach it 
	 * */
	public static Map<String,String> getChildrenIdsToAttach(Map params)
		{
		Map<String,String> childrenIds = new HashMap();
		for(Object obj:params.entrySet())
			{
			Map.Entry param = (Map.Entry)obj;
			String key = (String)param.getKey();
			String values[] = (String[])param.getValue();
			if(key.startsWith("child_"))
				{
				String value  = values[0];
				String idx = key.substring("child_".length());
				if( value!=null && !value.equals(""))
					{
					childrenIds.put(value,idx);
					}
				}
			}
		return childrenIds;
		}
	
	public static Map getAttachableChildrenParams(String durableId)
		{
		Map attachParams = new HashMap();
		Machine durable = stateStamper.getMachine(durableId);
		if(durable == null){return attachParams;}
		List attachChild = new ArrayList(); 
		attachChild.addAll(durable.getEquipmentType().getBusinessRulesBy(BusinessRule.ATTACH_CHILD));
		if(attachChild.size() > 0)
			{
			List actions = ((BusinessRule)attachChild.get(0)).getActionsBy(BusinessRuleAction.ATTACH_MULTIPLE);
			if(actions != null && actions.size() > 0)
				{
				BusinessRuleAction action = (BusinessRuleAction)actions.get(0);
				return action.getActionParameters();
				}
			}
		return attachParams;
		}
	
	public static Map attachChildrenToParent(String parent,Map<String,String> children,Administrator user)
		{
		Map result = new HashMap();
		List<String> childrenId = new ArrayList(children.keySet());
		Machine durable = stateStamper.getMachine(parent);
		int childrenNumber = getAttachableChildrenNumber(parent);
		if(durable == null){return result;}
		BusinessRuleAction setIndex = null;
		List actions = ((BusinessRule)durable.getEquipmentType().getBusinessRulesBy(BusinessRule.ATTACH_CHILD).get(0)).getActionsBy(BusinessRuleAction.SET_INDEX);
		if(actions != null && actions.size() > 0){setIndex = (BusinessRuleAction)actions.get(0);}
		if(children.size() > childrenNumber ){throw new IllegalArgumentException("More children than allowed");}
		List<String> currentChildren = durable.getChildrenByEquipmentType(getChildrenTypeForAttachChildAction(parent));
		currentChildren.removeAll(childrenId);
		List detached = new ArrayList(), attached = new ArrayList(), tried = new ArrayList();
		//detach the current children that are not in the new children to Attach
		for(String childToDetach:currentChildren)
			{
			if(associateDurable(childToDetach, "", user.coreId)){detached.add(childToDetach);}
			}
		for(String child:childrenId)
			{
			if(!child.equals(""))
				{
				setIndex(setIndex,child,children.get(child));
				if(associateDurable(child, parent, user.coreId)){attached.add(child);}
				else {tried.add(child);}
				}
			}
		
		Collections.sort(detached);
		result.put("detached",detached);
		Collections.sort(attached);
		result.put("attached",attached);
		Collections.sort(detached);
		result.put("tried",tried);
		return result;
		}
	
	/**
	 * assign an index to the attached durable in the variable configured in the UPDATE_MULTIPLE_BY_INDEX action
	 * */
	private static void setIndex(BusinessRuleAction setIndex,String durableId,String index)
		{
		if(setIndex == null)return;
		String idxVar = setIndex.getActionParameters().get("INDEX_VAR");
		Machine durable = stateStamper.getMachine(durableId);
		if(idxVar == null || durable == null || !durable.has(idxVar))return;
		durable.set(idxVar,index);
		}

	public static String getAttachChildIndexVariable(String durableId)
		{
		String indexVar = null;
		Machine durable = stateStamper.getMachine(durableId);
		List actions = ((BusinessRule)durable.getEquipmentType().getBusinessRulesBy(BusinessRule.ATTACH_CHILD).get(0)).getActionsBy(BusinessRuleAction.SET_INDEX);
		BusinessRuleAction action = null;
		if(actions != null && actions.size() > 0)
			{
			action = (BusinessRuleAction)actions.get(0);
			indexVar = action.getActionParameters().get("INDEX_VAR");
			}
		return indexVar;
		}
	
	public static List filterDurableStatusByEquipmentType(EquipmentType eqType, Area area, List<DurableStatus> resultList)
		{
		List filtered = new ArrayList();
		Map eqTypeMachines = stateStamper.getMachines(eqType,area.getPrimaryKey());
		for(DurableStatus dStatus : resultList )
			{
			if(eqTypeMachines.containsKey(dStatus.DURABLE_ID ))
				{
				filtered.add(dStatus);
				}
			}
		return filtered;
		}
	
	/**
	 * returns a Map with the children of the equipment type specified for the parent passed 
	 * the map returned has machine id as the key and the machine object as the value
	 * */
	public static Map<String, Machine> getCurrentChildren(Machine parent, String equipmentType)
		{
		Map<String, Machine> currentChildren = new HashMap();
		List<String> childrenIds = parent.getChildrenByEquipmentType(equipmentType);
		for (String childId : childrenIds)
			{
			currentChildren.put(childId, stateStamper.getMachine(childId));
			}
		return currentChildren;
		}

 	public static Map getMapFromJson(JSONObject json)
		{
		Map result = new HashMap();
		try
			{
			Iterator it = json.keys();
			String name, value;
			while (it.hasNext())
				{
				name = it.next().toString();
				value = json.getString(name);
				if (value == null) { value = "";}
				result.put(name, value);
				}
			}
		catch (JSONException e){}
		return result;
		}
	
	public static Map getJSONParametersMap(String src)
		{
		Map result = new HashMap();
		try{result.putAll(getMapFromJson(new JSONObject(src)));}
		catch(JSONException jsonEx){}
		return result;
		}
	
	public static List durableRealtimeSimpleHistoryReport(HttpServletRequest request,HttpServletResponse response)
		{
		List results = new ArrayList();
		Report report = new Report();
		if (request.getParameter("transitionOnly").equals("yes"))
			{
			report.setTransitionOnly(true);
			report.reportType = "History";
			}
		else
			{
			report.setTransitionOnly(false);
			report.reportType = "History_Details";
			}							
		EquipmentType et = getCurrentEquipmentType(request);
		report.grouping = et.name;
		report.period = "Raw";
		report.setArea(getCurrentArea(request));
		String startDate = "";
		String endDate = "";
		startDate = request.getParameter("startdate");
		endDate = request.getParameter("enddate");
		SimpleDateFormat dateformat = new SimpleDateFormat("yyyy-MM-dd");

		if (startDate != null && !startDate.trim().equals("") && !startDate.equalsIgnoreCase("null"))
			{
			try {report.startDate = dateformat.parse(startDate);}
			catch (ParseException e){e.printStackTrace();}
			}
		else {
			startDate = "";
		}

		if (endDate != null && !endDate.trim().equals("") && !endDate.equalsIgnoreCase("null"))
			{
			try	{report.endDate = dateformat.parse(endDate);}
			catch (ParseException e){e.printStackTrace();}
			}
		else{
			endDate= "";
		}
		String durableId = request.getParameter("DURABLE_ID");
		Map hashMap = new HashMap();
		if(durableId != null && !durableId.equals(""))
			{
			Machine selectedDurable = stateStamper.getMachine(durableId);
			hashMap.put(durableId, selectedDurable);
			report.setEquipmentType(selectedDurable.getEquipmentType());
			}
		else
			{
			report.setEquipmentType(et);
			}
		report.machines = hashMap;
		boolean realtime = false;

		report.setAternativeDisplay(true);
		report = getDefaultReport(report);
		try
			{
			results = generateDurableSimpleReport(request, response, realtime, report);
			results.add(request.getParameter("transitionOnly"));
			results.add(startDate);
			results.add(endDate);
			}
		catch (Throwable e)
			{
			logger.error(e.getMessage(),e);
			}
		return results;
		}
	
	public static void durableRealtimeSimpleReportExcel(HttpServletRequest request,HttpServletResponse response)throws Throwable 
		{
		List results = durableRealtimeSimpleHistoryReport(request,response);
		List data = new ArrayList();
		List headtmp = (List)results.get(6);
		List head = new ArrayList();
		for(int i = 0;i< headtmp.size();i++)
			{
			String s = String.valueOf(headtmp.get(i)); 
			if(s.equals("STATUS_PK")){continue;}
			else if(s.equals("STATE-SUBSTATE"))
				{
				head.add("FROMSTATE");
				head.add("TOSTATE");
				}
			else head.add(headtmp.get(i));
			}
		data.add(head);
		data.addAll((List)results.get(1));
		getHistoryExcel(response.getOutputStream(),data);
		}
	
	private static Area getCurrentArea(HttpServletRequest request)
		{
		Area area = (Area)request.getSession().getAttribute("area");
		if(area == null || area.getPrimaryKey() == null) area = stateStamper.getDefaultArea();
		return area;
		}
	
	private static EquipmentType getCurrentEquipmentType(HttpServletRequest request)
		{
		EquipmentType type = (EquipmentType)request.getSession().getAttribute("equipmentType");
		if(type == null || type.primaryKey == null) type = stateStamper.getDefaultEquipmentType();
		return type;
		}

}
