package com.freescale.stateStamper.model.entity;

public class FileGenerated
	{
	private String fileName,content,header,template;
	Integer timeout;
	
	public String getFileName()
		{
		return fileName;
		}
	
	public void setFileName(String fileName)
		{
		this.fileName = fileName;
		}
	
	public String getContent()
		{
		return content;
		}
	
	public void setContent(String content)
		{
		this.content = content;
		}
	
	public String getHeader()
		{
		return header;
		}
	
	public void setHeader(String header)
		{
		this.header = header;
		}
	
	public Integer getTimeout()
		{
		return timeout;
		}
	
	public void setTimeout(Integer timeout)
		{
		this.timeout = timeout;
		}

	public String getTemplate()
		{
		return template;
		}

	public void setTemplate(String template)
		{
		this.template = template;
		}

	}
