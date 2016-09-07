package com.freescale.stateStamper.model.entity;

import java.io.File;
import java.io.Serializable;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.sql.Blob;  
import org.apache.commons.fileupload.*;
public class Attachment implements Serializable, Comparator {

	private static final long serialVersionUID = 1L;
	private Integer attachmentPk;
	private Integer statusPk;
	private Integer equipmentPk;	
	private String displayFilename;
	private String fileLocation;
	private String time;	
	private DurableStatus status;
	private String type;
	private FileItem fileContent;
	
	
	public FileItem getFileContent() {
		return fileContent;
	}
	public void setFileContent(FileItem fileContent) {
		this.fileContent = fileContent;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public DurableStatus getStatus() {
		return status;
	}
	public void setStatus(DurableStatus status) {
		this.status = status;
	}
	public Integer getAttachmentPk() {
		return attachmentPk;
	}
	public void setAttachmentPk(Integer attachmentPk) {
		this.attachmentPk = attachmentPk;
	}
	public Integer getStatusPk() {
		return statusPk;
	}
	public void setStatusPk(Integer statusPk) {
		this.statusPk = statusPk;
	}
	public Integer getEquipmentPk() {
		return equipmentPk;
	}
	public void setEquipmentPk(Integer equipmentPk) {
		this.equipmentPk = equipmentPk;
	}
	public String getDisplayFilename() {
		return displayFilename;
	}
	public void setDisplayFilename(String displayFilename) {
		this.displayFilename = displayFilename;
	}
	public String getFileLocation() {
		return fileLocation;
	}
	public void setFileLocation(String fileLocation) {
		this.fileLocation = fileLocation;
	}
	public String getTime() {
		return time;
	}
	public void setTime(String time) {
		this.time = time;
	}
	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	public int compare(Object obj1,Object obj2)
	{ 
	return ((Attachment)obj2).getTime().compareTo(((Attachment)obj1).getTime()); 
	}

}
