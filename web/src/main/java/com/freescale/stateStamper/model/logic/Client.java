package com.freescale.stateStamper.model.logic;

import com.freescale.sc.model.entity.event.*;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.*;

import org.exolab.castor.mapping.Mapping;
import org.exolab.castor.xml.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

	
/**
 * Process the equipment event messages sent by the client socket
 */
public class Client extends Thread
	{
	private Socket socket;
	private Unmarshaller unmarshaller;
	private StateTransition stateTransition;
	private Vector threads;
	private Logger logger;
	private Date start;
	private String host;
	private long messages;
	private boolean log, run;
	private PrintWriter out;
	
	/**
	 * Creates and starts a thread to process a connection from a client
	 * @param socket Socket to read from
	 * @param mapping Mapping object to unmarshall XML message
	 * @param baseDirectory Directory where to generate temporary files
	 * @param diamondDirectory Diamond directory to send files to 
	 * @param DMHConnection Commection to PIC
	 * @throws Throwable
	 */
	public Client(Vector threads, Socket socket, Mapping mapping, StateTransition stateTransition, boolean log) throws Throwable
		{
		super("State Stamper client");
		this.socket = socket;
		this.stateTransition = stateTransition;
		this.threads = threads;
		this.log = log;
		socket.setSoTimeout(7200000);  // Timeout after 2 hours
		unmarshaller = new Unmarshaller(mapping);
		logger = LoggerFactory.getLogger("client_message");
		start = new Date();
		host = socket.getInetAddress().getHostName();
		threads.addElement(this);
		run = true;
		start();
		}
	
	/**
	 * Destroy this object and all the clients
	 */ 
	public void destroy()
		{
		run = false;
		interrupt();
		try {socket.close();} catch (Throwable t) {}
		socket=null;
		}
	
	/**
	 * Run method for this thread
	 */
	public void run()
		{
		char[] buffer = new char[131072]; // 128 Kb buffer
		String events = "";
		InputStreamReader inputStream=null;
		try
			{
			inputStream = new InputStreamReader(socket.getInputStream());
			while (run)
				{
				int length = inputStream.read(buffer);
				if(length > 0){
					if(stateTransition.isClientAcknowledgment()){
						out = new PrintWriter(socket.getOutputStream());out.println("HTTP/1.0 200 OK "+socket.getRemoteSocketAddress() + "\n");out.flush();
					}
				}else
					{
					logger.error("Inputstream.read for" + host + " returned " + length );
					break;
					}
				
				events += new String(buffer,0,length);
				int index1 = events.indexOf("<EquipmentEvent>");
				while (index1 > -1)
					{
					int index2 = events.indexOf("</EquipmentEvent>",index1);
					if (index2 == -1) break;  // If there is not a complete event, exit the loop
					String event = events.substring(index1,index2+17);  // Length of </EquipmentEvent> is 17 characters
					events = events.substring(index2+17);
					try
						{					
						EquipmentEvent equipmentEvent = (EquipmentEvent)unmarshaller.unmarshal(new CharArrayReader(event.toCharArray()));				
						messages++;
					  	if (log) log(equipmentEvent); // Log equip event if needed
						stateTransition.event(equipmentEvent); // Perform state transition on this event
						}
					catch (Throwable t) 
						{
						logger.error(t.getMessage(),t);
						}
					index1 = events.indexOf("<EquipmentEvent>");
					}
				try {Thread.sleep(1000);} catch (Throwable t) {}				
				}
			}
		catch(SocketException s)
			{
			if(s.getMessage().equals("Connection reset")){  logger.info(host + " closed the connection");}
			else {logger.error(s.getMessage()+" for host "+host,s);}
			}
		catch (Throwable t)
			{
			logger.error(t.getMessage() + " for host " + host,t);
			}
		finally
			{
			try {inputStream.close();out.close();} catch (Throwable t2) {}
			inputStream=null;
			try {socket.close();} catch (Throwable t2) {}
			socket=null;
			threads.removeElement(this);
			}
		}
	
	/**
	 * Returns a String array with statistics about thie socket client: host, start time and number of messages received
	 * @return
	 */
	public String[] getStatistics() {return new String[]{host,""+(new Date().getTime()-start.getTime())/1000,""+messages};}
	
	/**
	 * Logs an equipment event
	 * @param equipmentEvent The equipment event to log
	 */
	private void log(EquipmentEvent equipmentEvent)
		{
		StringBuffer buffer = new StringBuffer();
		buffer.append("\nEvent from " + host +" " +equipmentEvent.state+" "+equipmentEvent.event.id+" "+equipmentEvent.startTime+"\n");
		buffer.append("Equipment "+equipmentEvent.equipment.id+" "+equipmentEvent.equipment.type+"\n");
		for (int i=0; i<equipmentEvent.contexts.size(); i++)
			{
			Context context = (Context)equipmentEvent.contexts.elementAt(i);
			buffer.append("Context "+context.name+" "+context.value+"\n");
			}
		for (int i=0; i<equipmentEvent.summings.size(); i++)
			{
			Summing summing = (Summing)equipmentEvent.summings.elementAt(i);
			buffer.append("Summing "+summing.name+" "+summing.value+" "+summing.count+"\n");
			}
		buffer.append("\n");
		logger.info(buffer.toString());
		}
	}
