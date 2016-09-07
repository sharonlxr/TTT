package com.freescale.stateStamper.db;

import java.util.Vector;

import org.apache.ojb.broker.accesslayer.conversions.FieldConversion;

import com.freescale.stateStamper.model.entity.Email;

public class EmailVector2Varchar implements FieldConversion{
	
	private static final long serialVersionUID = -1470019199301743716L;
	private static final String NULLVALUE = "#NULL#";
	private static final String EMPTYCOLLEC = "#EMPTY#";
	private static final String SEPARATOR = "#";

	public Object javaToSql(Object source) {
		if(source==null){return NULLVALUE;}
		if (!(source instanceof Vector)){
            throw new IllegalArgumentException("source is not a Vector,it is a  "+source.getClass().getName() );
        }
		Vector emails =(Vector)source;
		if(emails.size()==0)return EMPTYCOLLEC;
		StringBuffer result=new StringBuffer();
		for (int i = 0; i < emails.size(); i++) {
			 result.append(email2Str((Email)emails.get(i)));
		}
		return result.toString();

	}

	public Object sqlToJava(Object source) {
		if (source == null)return null;
        if (source.toString().equals(NULLVALUE)){return null;}
        if (source.toString().equals(EMPTYCOLLEC)){return new Vector(0);}
        
        Vector result=new Vector();
        String [] temp = source.toString().split(SEPARATOR);
        for (int i = 0; i < temp.length; i++) {
			Email email = str2Email(temp[i]);
			result.add(email);
		}
		return result;
	}
	
	/**
	 * @param string in the form To=joe@fsl.com
	 * @return Email object built from the string 
	 * */
	private Email str2Email(String strEmail){
		Email email=null;
		String [] temp= strEmail.split("=");
		if(temp.length!=2){throw new IllegalArgumentException("String does not have the rigth format");}
		email = new Email();
		email.type  =temp[0];
		email.email =temp[1];
		return email;
	}
	
	private String email2Str(Email email){
		StringBuffer str = new StringBuffer();
		str.append(email.type);
		str.append("=");
		str.append(email.email);
		str.append(SEPARATOR);

		return str.toString();
	}
	
}
