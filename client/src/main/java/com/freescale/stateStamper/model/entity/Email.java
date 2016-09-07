package com.freescale.stateStamper.model.entity;

import java.io.*;

public class Email implements Serializable
	{
    private static final long serialVersionUID = -9102496372353250946L;
    public  static final String TO = "To";
    public  static final String CC = "Cc";
 
	public String email, type;
	
	public String toString() {return email;}
	}
