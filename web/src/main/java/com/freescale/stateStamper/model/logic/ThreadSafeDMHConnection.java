package com.freescale.stateStamper.model.logic;

import com.freescale.sc.model.entity.event.EquipmentEvent;

public class ThreadSafeDMHConnection {	
	private DMH dMHConnection;
	
	public ThreadSafeDMHConnection(DMH dMHConnection){
		this.dMHConnection = dMHConnection;
	}
	
	public synchronized void send (EquipmentEvent equipmentEvent){
		this.dMHConnection.send(equipmentEvent);
	}
	
	public synchronized void close (){
		this.dMHConnection.close();
		this.dMHConnection = null;
	}
}
