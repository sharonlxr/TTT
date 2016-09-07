package com.freescale.stateStamper.db;

import java.util.Vector;

import org.apache.ojb.broker.accesslayer.conversions.FieldConversion;

import com.freescale.stateStamper.model.entity.Machine;

public class MachineVector2VarcharFieldConversion implements FieldConversion{

	private static final long serialVersionUID = 3309359877669336009L;
	private static final String NULLVALUE = "#NULL#";
	private static final String EMPTYCOLLEC = "#EMPTY#";
	private static final String SEPARATOR = "#";

	public Object javaToSql(Object source) {
		if(source==null){return NULLVALUE;}
		if (!(source instanceof Vector)){
            throw new IllegalArgumentException("source is not a Vector,it is a  "+source.getClass().getName() );
        }
		Vector machines=(Vector)source;
		if(machines.size()==0)return EMPTYCOLLEC;
		StringBuffer result=new StringBuffer();
		for (int i = 0; i < machines.size(); i++) {
			 result.append(((Machine)machines.get(i)).mESName);
			 result.append(SEPARATOR);
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
			Machine machine = new Machine();
			machine.mESName = temp[i];
			result.add(machine);
		}
		return result;
	}
	
}
