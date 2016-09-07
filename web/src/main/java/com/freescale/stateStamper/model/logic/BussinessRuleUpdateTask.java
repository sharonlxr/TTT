package com.freescale.stateStamper.model.logic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.freescale.stateStamper.model.entity.*;


public class BussinessRuleUpdateTask implements Runnable{


	private Machine machine;
	private StateTransition stateTransition;
	private String latestEvent;
	private static Logger logger = LoggerFactory.getLogger("stateStamper");
	
	public BussinessRuleUpdateTask (Machine machine, StateTransition stateTransition, String latestEvent) {
		this.machine = machine;
		this.stateTransition = stateTransition;
		this.latestEvent = latestEvent;
	}
	
	public void run (){
		try{
			machine.executeEPRrules(stateTransition,latestEvent);
		}
		catch (Throwable th){
			logger.error(machine.eIName + " " + th.getMessage());
			th.printStackTrace();
		}
	}
}
