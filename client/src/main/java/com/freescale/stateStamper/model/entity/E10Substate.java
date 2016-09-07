package com.freescale.stateStamper.model.entity;

import java.io.Serializable;

public class E10Substate implements Serializable{

	private static final long serialVersionUID = 1L;
	public Integer primaryKey;
	public String e10;
	public String substate;
	public String toString(){
		return e10+"-"+substate;
	}

}
