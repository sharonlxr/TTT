/* COPYRIGHT FREESCALE SEMICONDUCTOR INC., AUSTIN TEXAS, 2008 */
/* $Id: Event.java,v 1.5 2008/10/11 01:07:55 ra7956 Exp $ */

package com.freescale.sc.model.entity.event;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.*;

public class Event implements Serializable
	{
	public static final String EQUIP_INFO = "EQUIP_INFO";
	public static final String EQUIP_START = "EQUIP_START";
	public static final String EQUIP_STOP = "EQUIP_STOP";
	public static final String GOTO_PRODUCTION_READY = "GOTO_PRODUCTION_READY";
	public static final String ALARM_SET = "ALARM_SET";
	public static final String ALARM_CLEAR = "ALARM_CLEAR";
	public static final String GOTO_ENGINEERING = "GOTO_ENGINEERING";
	public static final String GOTO_SCHEDULED_DOWNTIME = "GOTO_SCHEDULED_DOWNTIME";
	public static final String GOTO_UNSCHEDULED_DOWNTIME = "GOTO_UNSCHEDULED_DOWNTIME";
	public static final String GOTO_NON_SCHEDULED = "GOTO_NON_SCHEDULED";

	public String id;
	public String time;
	public Event() {}
	
	public Event(String id)
		{
		this.id=id;
		SimpleDateFormat timeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		timeFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		this.time=timeFormat.format(new Date());
		}
	
	public Event(String id, String time)
		{
		this.id=id;
		this.time=time;
		}
	
	public Event copy() {return new Event(id,time);}
	}
