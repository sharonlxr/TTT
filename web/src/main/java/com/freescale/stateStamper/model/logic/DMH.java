package com.freescale.stateStamper.model.logic;

import com.freescale.sc.model.entity.event.*;
import java.text.SimpleDateFormat;
import java.util.*;
import pic.hume.DMH.*;
import com.freescale.stateStamper.model.entity.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * This class is an interface to the PIC
 */
public class DMH implements DMHLostServerItf
	{
	private DMHClient dmh;
	private int connected;
	private String picHost;
	private String picPort;
	private String dmhBox;
	private String localHost;
	private Logger logger;
	private SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	/**
	 * Main constructor
	 * @param picHost The PIC host
	 * @param picPort The PIC port
	 * @param dmhBox The PIC DMH box
	 */
    public DMH(String picHost, String picPort, String dmhBox)
    	{
    	this.picHost = picHost;
    	this.picPort = picPort;
    	this.dmhBox = dmhBox;
  
    	logger = LoggerFactory.getLogger("stateStamper");
    	try
    		{  	    
   			dmh = new DMHClient();
   			connected = dmh.getState();
   			logger.info("DMH client "+dmh+" status is "+connected);
   			open();
    		}
    	catch (Throwable t) {logger.error(t.getMessage());}
   
    	}
	
    /**
     * Sends an equipment event to the PIC. If the connection was down, retries once to open it
     * @param equipmentEvent The equipment event to send
     */
	public void send(EquipmentEvent equipmentEvent)
		{
		if (connected == 0) open();
		if (connected == 7)
			{
			try	{dmh.Put(dmhBox,toVFEI(equipmentEvent));}
			catch(Throwable t)
				{
				connected=dmh.getState();
				logger.error(t.getMessage());
				}
			}
		}
	
	/**
	 * Closees teh connection to the PIC
	 */
	public void close()
		{
		try
			{
			dmh.Close(picHost);
			dmh.Disconnect();
			logger.info("Disconnected "+dmh+" from "+localHost);
			}
		catch (Throwable t) {logger.error(t.getMessage());}
		}
	
	/** 
	 * This method is automatically invoked if the connection to teh PIC is lost
	 */
	public void DMHLostServer(DMHClientItf connection)
		{
		try
			{
			Thread.sleep(5000);
			try	{dmh.Disconnect();} catch (Throwable t) {logger.error(t.getMessage());}
			open();
			}
		catch(Throwable t){logger.error(t.getMessage());}
		}
	
	/**
	 * Opens a connection to the PIC
	 */
	private void open()
		{
		if (connected == 0)
			{
			try
				{
        		dmh.Init(picHost,picPort);
        		connected = dmh.getState();
                localHost = dmh.Hostname();
                logger.info("Connected "+dmh+" from "+localHost+" status is "+connected);
        		}
        	catch (Throwable t) {logger.error(t.getMessage());}
			}
		}
	
	/**
	 * Translates an equipment event to VEFI
	 * @param equipmentEvent The equipment event
	 * @return A string with the VFEI equivalent
	 */
	private String toVFEI(EquipmentEvent equipmentEvent)
		{
		StringBuffer message = new StringBuffer("CMD/A=\"EVENT_REPORT\" MID/A=\""+equipmentEvent.equipment.id+"\" ");
		message.append("EVENT_ID/A=\"EQUIP_INFO\" "); // add event ID
		message.append("EVENT_TIME/A=\""+formatter.format(new Date())+"\" "); // add time
		// add handler ID information
		Iterator iterator = equipmentEvent.equipment.dependentTools.iterator();
		while(iterator.hasNext())
			{
			Equipment equipment=(Equipment)iterator.next();
			message.append("DEPENDENT_TOOL_TYPE/A=\""+equipment.type.toUpperCase()+"\" ");
			message.append("DEPENDENT_TOOL/A=\""+equipment.id.toUpperCase()+"\" ");
			}
		// add context values
		iterator = equipmentEvent.contexts.iterator();
		while(iterator.hasNext())
			{
			Context context=(Context)iterator.next();
			message.append(context.name.trim().toUpperCase()+"/A=\""+context.value+"\" ");
			}
		// add summing values
		iterator=equipmentEvent.summings.iterator();
		while(iterator.hasNext())
			{
			Summing summing=(Summing)iterator.next();
			message.append(summing.name.trim().toUpperCase()+"/U4="+summing.value+" ");
			}
		message.append("STATE_ID/A=\""+equipmentEvent.state+"\" "); // add state ID
		return message.toString();
		}
	
	
	public boolean comparePicConn(Machine mach){

		return  picHost!=null && picHost.equals(mach.pic_Host) && 
				picPort!=null && picPort.equals(mach.pic_Port) && 
				dmhBox !=null && dmhBox.equals(mach.dmhbox);	 
	}
	

	

public boolean equals(Object that) {
	if ( !(that instanceof DMH) ) return false;
	DMH dmh = (DMH)that;
	
	if ( this.picHost!=null && this.picHost.equals(dmh.picHost)&& 
		this.picPort!=null && this.picPort.equals(dmh.picPort) && 
		this.dmhBox!=null && this.dmhBox.equals(dmh.dmhBox)) return true;
	else 
		
    return false;
    
  }



    public int hashCode () { 
		String pic = toString() ;
        if (pic!=null)
        	return pic.hashCode();
        else 
        	return 0;
    }
    
    public String toString(){
    	return picHost +":"+ picPort + ":"+dmhBox;
    }

	}
