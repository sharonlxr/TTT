package com.freescale.stateStamper.model.entity;



public interface IStateTransition {

	
	public void sendEvent(IEquipmentEvent equipmentEvent, String newState, String oldState, String oldSubState, String newSubState) throws Throwable;
	public void executeBusinessRules(Machine machine, String latestEvent); 
}
