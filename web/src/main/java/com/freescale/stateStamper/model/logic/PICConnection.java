package com.freescale.stateStamper.model.logic;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;

import org.exolab.castor.mapping.Mapping;
import org.exolab.castor.xml.Marshaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.freescale.sc.model.entity.event.EquipmentEvent;
import com.freescale.clientUtil.TCPClient;

public class PICConnection
	{
	private String host;
	private int port;
	private ByteArrayOutputStream buffer;
	private TCPClient pic;
	private Logger logger;
	private static int TIMEOUT = 30000;
	private static final String PIC_ACK_REPLY = "//c 0 \"OK\"";
	
	private Marshaller marshaller;

	public PICConnection(String host,int port,Mapping mapping)
	{
	this.host = host;
	this.port = port;
	pic = new TCPClient(TIMEOUT);
	logger = LoggerFactory.getLogger("stateStamper");
	// Load XML mapping, create marshaller to write to buffer and create debug file if necessary
	try
		{
		buffer = new ByteArrayOutputStream(65536);
		marshaller = new Marshaller(new OutputStreamWriter(buffer));
		marshaller.setMapping(mapping);
		}
	catch (Throwable throwable) {logger.error("Error setting marshaller "+throwable);}
	}
	
	
	public void exit()
	{
	if(pic != null) pic.disconnect();
	}
	
	
	public void sendEvent(EquipmentEvent equipmentEvent) 
		{
		String equipmentId = equipmentEvent.equipment.id;
		// Marshall event into buffer variable
		try	{marshaller.marshal(equipmentEvent);}
		catch (Throwable t)	{logger.error("Error marshalling event " + t);}
		String message = buffer.toString();
		buffer.reset(); // Reset buffer to start marshalling on an empty one

		if (!pic.isConnected())
			{
			logger.info("Opening connection to PIC at: " + host + ":" + port);
			try
				{
				pic.connect("PIC", host, port);
				}
			catch (Throwable t)
				{
				String errMsg = "Error connecting to the PIC - host : "+ host + " port :" + port; 
				logger.error(errMsg,t);
				throw new RuntimeException(errMsg,t);
				}
			}
		String id = "ID=\""+equipmentId+"\"";
		message = message.replace(id, id+" MESID=\""+ equipmentId +"\"");
		logger.info("Sending message to PIC : " + message);
		String picReply = pic.sendRcvMsgForGenesisPic(message);
		logger.info("Reply from PIC: "+picReply);
		if( !PIC_ACK_REPLY.equals(picReply) )
			{
			String errMsg = "Error sending a message to the PIC, the reply was :" + picReply; 
			logger.error(errMsg);
			throw new RuntimeException(errMsg);
			}
		}
	
	
	}
