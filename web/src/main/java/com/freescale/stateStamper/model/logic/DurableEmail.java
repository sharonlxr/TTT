package com.freescale.stateStamper.model.logic;

import java.io.File;
import java.util.Properties;
import java.util.Vector;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.SendFailedException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import com.freescale.serverUtil.EmailServer;
import com.freescale.stateStamper.model.entity.CurrentDurableStatus;

public class DurableEmail implements Runnable
	{
    private String subject = "";
	private String content = "";    
    private Vector to,cc;
    private String type;
    
    public void setType(String mimeType){type = mimeType;}
    public String getType(){return type;}
    
	public String getSubject() {
		return subject;
	}

	public void setSubject(String sUBJECT) {
		subject = sUBJECT;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String cONTENT) {
		content= cONTENT;
	}

	public Vector getTo() {
		return to;
	}

	public void setTo(Vector to) {
		this.to = to;
	}
	
	public void setTo(String addresslist){
		String[] tolist = addresslist.split(",");
		Vector toVector = new Vector(); 
		for (int i = 0; i < tolist.length; i ++){
			String emailaddresstr = tolist[i];
			if (!emailaddresstr.contains("@")) {
				emailaddresstr = emailaddresstr + "@" + FormatReport.getDomain();
			}
			toVector.add(emailaddresstr);
		}		
		setTo(toVector);		
	}
	
	public void setTo(String addresslist, CurrentDurableStatus durablestatus ){
		String[] tolist = addresslist.split(",");
		Vector toVector = new Vector(); 
		for (int i = 0; i < tolist.length; i ++){
			String emailaddresstr = tolist[i];			
			Object tmp =durablestatus.get(emailaddresstr);
			if (tmp == null)
			   	{
				tmp = emailaddresstr;
			    }
			if (!emailaddresstr.contains("@")) {
				emailaddresstr = emailaddresstr + "@"+ FormatReport.getDomain();
			}
			toVector.add(emailaddresstr);
		}		
		setTo(toVector);		
	}
	
	 

	public Vector getCc() {
		return cc;
	}

	public void setCc(Vector cc) {
		this.cc = cc;
	}

	public void send()throws Throwable {
		if (to.size() > 0 || cc.size() > 0)
			EmailServer.send(FormatReport.getEmailServer(),FormatReport.getFromUser(),FormatReport.getFromAddress(),to,cc,subject,content,null);	
	}
	
	public void run() {
		try{
		 if(type != null && type.equals("html")){sendHTML(null);}
		 else {send();}
		}
		catch(Throwable th){
			th.printStackTrace();
		}
	}
	
	public void sendHTML(File attachment)throws Throwable
		{
		// Get system properties and setup mail server
		Properties properties = System.getProperties();
		properties.put("mail.smtp.host",FormatReport.getEmailServer());
		Session session = Session.getInstance(properties,null);
		// Prepare mail message
		Message message = new MimeMessage(session);
		message.setFrom(new InternetAddress(FormatReport.getFromAddress(),FormatReport.getFromAddress()));
		if (to != null && to.size() > 0)
			{
			InternetAddress[] addresses = new InternetAddress[to.size()];
			for (int i=0; i<to.size(); i++) addresses[i] = new InternetAddress((String)to.elementAt(i));
			message.setRecipients(Message.RecipientType.TO,addresses);
			}
		if (cc != null && cc.size() > 0)
			{
			InternetAddress[] addresses = new InternetAddress[cc.size()];
			for (int i=0; i<cc.size(); i++) addresses[i] = new InternetAddress((String)cc.elementAt(i));
			message.addRecipients(Message.RecipientType.CC,addresses);
			}
		message.setSubject(subject);
		// Crate multiparts: one part is the email body, the other the attachment
		Multipart multipart = new MimeMultipart();
		MimeBodyPart mimeBodyPart = new MimeBodyPart();
		mimeBodyPart.setContent(content,"text/html");
		multipart.addBodyPart(mimeBodyPart);
		if (attachment != null)
			{
			mimeBodyPart = new MimeBodyPart();
			mimeBodyPart.setDataHandler(new DataHandler(new FileDataSource(attachment)));
			mimeBodyPart.setFileName(attachment.getName());
			multipart.addBodyPart(mimeBodyPart);
			}
		// Put parts in message
		message.setContent(multipart);
		message.saveChanges();
		// Send mail message and resend it to valid addresses if failed due to invalid addresses
		try {Transport.send(message);}
		catch (SendFailedException exception) {Transport.send(message,exception.getValidUnsentAddresses());}
		}
	}
