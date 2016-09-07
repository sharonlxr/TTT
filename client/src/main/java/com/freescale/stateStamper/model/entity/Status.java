package com.freescale.stateStamper.model.entity;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Status extends IStatus{
	private Integer STATUS_PK,EQUIPMENT_PK, EVENT_PK;
	private String MID,TIME,STATE,SUBSTATE,PERIOD,LOT_ID,DEVICE_ID,TEST_PROGRAM,TEMPERATURE,INSERTIONS,RESCREEN,HANDLER_ID,SITES_AVAILABLE,LOADBOARD_ID,OPERATOR_ID,DEVICES_PROCESSED,RETEST,CNT_GOOD,TEST_TIME,HND_INDEX_TIME,SITE0_HB1,SITE0_HB$,SITE1_HB1,SITE1_HB$,SITE2_HB1,SITE2_HB$,SITE3_HB1,SITE3_HB$,SITE4_HB1,SITE4_HB$,SITE5_HB1,SITE5_HB$,SITE6_HB1,SITE6_HB$,SITE7_HB1,SITE7_HB$,SITE8_HB1,SITE8_HB$,SITE9_HB1,SITE9_HB$,SITE10_HB1,SITE10_HB$,SITE11_HB1,SITE11_HB$,SITE12_HB1,SITE12_HB$,SITE13_HB1,SITE13_HB$,SITE14_HB1,SITE14_HB$,SITE15_HB1,SITE15_HB$,COUNT,DURATION,SC_VERSION,API_VERSION,GOOD_TEST_TIME,GOOD_SYSTEM_INDEX_TIME,ALARM_TIME,VALIDATION_TIME,PROGRAM_LOAD_TIME,JAM_NUMBER,SETUP_SOURCE,FLOW_ID,CRITICAL_JAM,JAM_DESCRIPTION,CREW,DEVICE_NAME,PRODUCT_LINE,LOT_COUNT,SITE_DISABLED_DATE,SITE_DISABLED_RESPONSE_TIME,SITE_DISABLED_BIN_NUM,SITE_DISABLED_BIN_TYPE,SITE_DISABLED_FAIL_TYPE,SITE_DISABLED,SITE_DISABLED_REPAIR_TIME,LOT_PROGRESS,LOT_TYPE,SITE0_SOCKET,SITE1_SOCKET,SITE2_SOCKET,SITE3_SOCKET,SITE4_SOCKET,SITE5_SOCKET,SITE6_SOCKET,SITE7_SOCKET;
	private String SITE16_HB1,SITE16_HB$,SITE17_HB1,SITE17_HB$,SITE18_HB1,SITE18_HB$,SITE19_HB1,SITE19_HB$,SITE20_HB1,SITE20_HB$,SITE21_HB1,SITE21_HB$,SITE22_HB1,SITE22_HB$,SITE23_HB1,SITE23_HB$,SITE24_HB1,SITE24_HB$,SITE25_HB1,SITE25_HB$,SITE26_HB1,SITE26_HB$,SITE27_HB1,SITE27_HB$,SITE28_HB1,SITE28_HB$,SITE29_HB1,SITE29_HB$,SITE30_HB1,SITE30_HB$,SITE31_HB1,SITE31_HB$;
	private String TESTER_EXEC_VERSION,STEP_NAME,PSEQ_NUMBER,PROC_NAME,SHAKER1_ID,SHAKER2_ID,JRE_VERSION;
	//summary fields
	private String shift,day,week,month;
	//add for probe
	private String TOUCH_INCREMENT,NUMBER_POLISH,WAFERS_PROCESSED,REPROBE, PROBECARD_ID, PROBECARD_TYPE, COMMENTS, WAFER_ID, PASS, LOADING_ERROR;
	private String SITE0_HB8,SITE1_HB8,SITE2_HB8,SITE3_HB8,SITE4_HB8,SITE5_HB8,SITE6_HB8,SITE7_HB8,SITE8_HB8,SITE9_HB8,SITE10_HB8,SITE11_HB8,SITE12_HB8,SITE13_HB8,SITE14_HB8,SITE15_HB8,SITE16_HB8,SITE17_HB8,SITE18_HB8,SITE19_HB8,SITE20_HB8,SITE21_HB8,SITE22_HB8,SITE23_HB8,SITE24_HB8,SITE25_HB8,SITE26_HB8,SITE27_HB8,SITE28_HB8,SITE29_HB8,SITE30_HB8,SITE31_HB8;
	private String PNP_SITES_AVAILABLE,  PNP_A1_DISABLED, PNP_A2_DISABLED, PNP_A3_DISABLED, PNP_A4_DISABLED, PNP_B1_DISABLED, PNP_B2_DISABLED, PNP_B3_DISABLED, PNP_B4_DISABLED;
	private String IPNP_SITES_AVAILABLE, IPNP_1_DISABLED, IPNP_2_DISABLED, IPNP_3_DISABLED, IPNP_4_DISABLED, IPNP_5_DISABLED, IPNP_6_DISABLED, IPNP_7_DISABLED, IPNP_8_DISABLED;
	private String OPNP_SITES_AVAILABLE, OPNP_A1_DISABLED, OPNP_A2_DISABLED, OPNP_A3_DISABLED, OPNP_A4_DISABLED, OPNP_B1_DISABLED, OPNP_B2_DISABLED, OPNP_B3_DISABLED, OPNP_B4_DISABLED;
	private String TPNP_SITES_AVAILABLE, TPNP_A1_DISABLED, TPNP_A2_DISABLED, TPNP_A3_DISABLED, TPNP_A4_DISABLED, TPNP_B1_DISABLED, TPNP_B2_DISABLED, TPNP_B3_DISABLED, TPNP_B4_DISABLED;

	
	private static Map<String,Field> fields;
	private Logger logger;
	public static String DEFAULT_STATUS [] = {"MID","TIME","STATE","SUBSTATE"};

	static
		{
		Field[] f = Status.class.getDeclaredFields();
		fields = new HashMap();
		for (int i = 0; i < f.length; i++)
			{
			fields.put(f[i].getName(), f[i]);
			}
		}
	
	public Status()
		{
		logger = LoggerFactory.getLogger("stateStamper");
		}
	
	protected Map getFields(){
		return Status.fields;
	}

	/**
	 * sets any property
	 * @param name the name of the property to set
	 * @param value Object the value to set it must be either Integer or String
	 * */
	
	public void set(String name, Object value){
		if(name==null )return;
		if(name.indexOf('*')>-1){
			name = name.substring(0,name.length()-1);
		} 
		Field field = (Field)fields.get(name);
		try {
			if(field!=null){
				if(field.getType() == Integer.class ){
					Integer val=null;
					if(value == null){
						val = new Integer(0);
					}else{
						try{
							val = Integer.valueOf( value.toString() );
						}catch(NumberFormatException ne){
							val = new Integer(0);
						}
					}
					field.set(this,val );
				}else{
					if(value!=null){
						field.set(this,value.toString());
					}
				}
			}
			else
				logger.error("Field " + name + " does not belong to Status");
		} catch (IllegalArgumentException e) {
			logger.error("IllegalArgumentException with " + name + " : " + value);
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			logger.error("IllegalAccessException with " + name + " : " + value);
			e.printStackTrace();
		}
	}
	
	public boolean has(String name)
		{
		Field f = (Field)fields.get(name);
		return f!=null;
		}
	
	public Object get(String name)
		{
		if(! has(name))return null;
		Field f = (Field)fields.get(name);
		Object o=null;
		try {
			o = f.get(this);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return o;
		}
	
	public String toExtString()
		{
		StringBuilder sb = new StringBuilder();
		String name;
		Field field;
		Object val = null;
		for (Iterator it = fields.keySet().iterator(); it.hasNext();)
			{
			name = (String) it.next();
			if (name.equals("fields")) continue;
			field = (Field) fields.get(name);
			try
				{
				val = field.get(this);
				}
			catch (IllegalArgumentException e)
				{
				e.printStackTrace();
				}
			catch (IllegalAccessException e)
				{
				e.printStackTrace();
				}
			sb.append(name + " : " + val + "\n");
			}
		return sb.toString();
		}



	public String getShift()
		{
		return shift;
		}

	public void setShift(String shift)
		{
		this.shift = shift;
		}

	public String getDay()
		{
		return day;
		}

	public void setDay(String day)
		{
		this.day = day;
		}

	public String getWeek()
		{
		return week;
		}

	public void setWeek(String week)
		{
		this.week = week;
		}

	public String getMonth()
		{
		return month;
		}

	public void setMonth(String month)
		{
		this.month = month;
		}
	
}
