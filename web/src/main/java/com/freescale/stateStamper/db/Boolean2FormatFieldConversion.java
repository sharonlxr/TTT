package com.freescale.stateStamper.db;
import org.apache.ojb.broker.accesslayer.conversions.FieldConversion;
public class Boolean2FormatFieldConversion implements FieldConversion{
	private static final long serialVersionUID = 1L;
	private static String HTML_FORMAT = "HTML";
    private static String XLS_FORMAT = "XLS";


	public Object javaToSql(Object source){
		if (source instanceof Boolean){
            if (source.equals(Boolean.TRUE)){
                return HTML_FORMAT;
            }else{
                return XLS_FORMAT;
            }
        }else{
            return source;
        }
	}

	public Object sqlToJava(Object source){
		if( source == null)return Boolean.FALSE;
		if( !(source instanceof String))return Boolean.FALSE;
            if (source.equals(HTML_FORMAT)){
                return Boolean.TRUE;
            }else {
                return Boolean.FALSE;
            }
	}
	

}
