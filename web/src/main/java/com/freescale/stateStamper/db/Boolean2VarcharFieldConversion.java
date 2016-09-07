package com.freescale.stateStamper.db;

import org.apache.ojb.broker.accesslayer.conversions.FieldConversion;

public class Boolean2VarcharFieldConversion implements FieldConversion {
	private static final long serialVersionUID = 8603909641050861664L;
	private static String YES = "Y";
    private static String NO = "N";

	public Object javaToSql(Object source) {
		if (source instanceof Boolean){
            if (source.equals(Boolean.TRUE)){
                return YES;
            }else{
                return NO;
            }
        }else{
            return source;
        }
	}

	public Object sqlToJava(Object source) {
		if(! (source instanceof String) )return Boolean.FALSE;
		return new Boolean(source.equals("Y")); 
	}

}
