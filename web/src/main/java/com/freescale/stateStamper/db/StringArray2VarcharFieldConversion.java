package com.freescale.stateStamper.db;


import org.apache.ojb.broker.accesslayer.conversions.FieldConversion;

public class StringArray2VarcharFieldConversion implements FieldConversion{
    private static final String NULLVALUE = "#NULL#";
    private static final String EMPTYCOLLEC = "#EMPTY#";
    private static final String SEPARATOR = "#";

	private static final long serialVersionUID = 1L;

	public Object javaToSql(Object javaObj){
		if(javaObj==null){return NULLVALUE;}
		if (!(javaObj instanceof String[])){
            throw new IllegalArgumentException("source is not a String[],it is a  "+javaObj.getClass().getName() );
        }
		String []sArr=(String [])javaObj;
		if(sArr.length==0)return EMPTYCOLLEC;
		StringBuffer result=new StringBuffer();
		for (int i = 0; i < sArr.length; i++) {
			result.append(sArr[i]);
			result.append(SEPARATOR);
		}
		return result.toString();
	}

	public Object sqlToJava(Object sqlObj){
		if (sqlObj == null){return null;}
        if (sqlObj.toString().equals(NULLVALUE)){return null;}
        if (sqlObj.toString().equals(EMPTYCOLLEC)){return new String[0];}
        
        String[] result=sqlObj.toString().split(SEPARATOR);
		return result;
	}

}
