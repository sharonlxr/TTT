package com.freescale.stateStamper.model.logic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.freescale.stateStamper.model.entity.*;


public class StatusSendTask implements Runnable{


	private Machine machine;
	private IStateTransition stateTransition;
	private IEquipmentEvent equipmentEvent;
	private String newState, oldState, oldSubState,  newSubState;
	private static Logger logger = LoggerFactory.getLogger("stateStamper");
	
	public StatusSendTask (Machine machine, IStateTransition stateTransition,IEquipmentEvent equipmentEvent, String newState, String oldState, String oldSubState, String newSubState) {
		this.machine = machine;
		this.stateTransition = stateTransition;
		this.equipmentEvent = equipmentEvent;
		this.newState = newState;
		this.oldState = oldState;
		this.oldSubState = oldSubState;
		this.newSubState = newSubState;
	}
	
	public void run (){
		try{
			machine.sendStatus( stateTransition, this.equipmentEvent, this.newState, this.oldState, this.oldSubState, this.newSubState);
		}
		catch (Throwable ex) {
			logger.error(machine.eIName + " " + ex.getMessage());
			ex.printStackTrace();
		}
	}
}
