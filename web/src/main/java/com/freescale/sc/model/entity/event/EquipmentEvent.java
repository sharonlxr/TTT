/* COPYRIGHT FREESCALE SEMICONDUCTOR INC., AUSTIN TEXAS, 2008 */
/* $Id: EquipmentEvent.java,v 1.6 2008/10/11 01:07:55 ra7956 Exp $ */

package com.freescale.sc.model.entity.event;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.*;
import com.freescale.stateStamper.model.entity.*;

public class EquipmentEvent implements Serializable,IEquipmentEvent
	{
	public static final String PRODUCTIVE = "Productive";
	public static final String STANDBY = "Standby";
	public static final String ENGINEERING = "Engineering";
	public static final String SCHEDULED_DOWN = "Scheduled Down";
	public static final String UNSCHEDULED_DOWN = "Unscheduled Down";
	public static final String NON_SCHEDULED = "Non Scheduled";
	public Equipment equipment;
	public Event event;
	public Vector contexts = new Vector();
	public Vector summings = new Vector();
	public String startTime;
	public String state;
	
	public EquipmentEvent() {}
	
	public EquipmentEvent copy()
		{
		EquipmentEvent aux = new EquipmentEvent();
		if (equipment != null) aux.equipment = equipment.copy();
		if (event != null) aux.event = event.copy();
		if (contexts != null) for (int i=0; i<contexts.size(); i++) aux.contexts.addElement(((Context)contexts.elementAt(i)).copy());
		if (summings != null) for (int i=0; i<summings.size(); i++) aux.summings.addElement(((Summing)summings.elementAt(i)).copy());
		if (startTime != null) aux.startTime = startTime;
		aux.state = state;
		return aux;
		}
	
	public void setTime(Date date)
		{
		SimpleDateFormat timeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		timeFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		startTime=timeFormat.format(date);
		}
	}
