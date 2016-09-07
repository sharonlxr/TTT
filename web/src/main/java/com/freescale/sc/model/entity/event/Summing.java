/* COPYRIGHT FREESCALE SEMICONDUCTOR INC., AUSTIN TEXAS, 2008 */
/* $Id: Summing.java,v 1.4 2008/10/11 01:07:55 ra7956 Exp $ */

package com.freescale.sc.model.entity.event;

import java.io.Serializable;

public class Summing implements Serializable
	{
	public String name;
	public int value;
	public int count; //amount of times value has been increased
	public String units;
	
	public Summing() {}
		
	public Summing(String name, int value, int count, String units)
		{
		this.name=name;
		this.value=value;
		this.count=count;
		this.units=units;
		}
	
	public Summing copy() {return new Summing(name,value,count,units);}
	
	public void reset() {value=count=0;}
	
	public void increment(int incrementValue)
		{
		value+=incrementValue;
		count++;
		}
	}
	
