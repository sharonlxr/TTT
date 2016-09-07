/* COPYRIGHT FREESCALE SEMICONDUCTOR INC., AUSTIN TEXAS, 2008 */
/* $Id: Context.java,v 1.4 2008/10/11 01:07:55 ra7956 Exp $ */

package com.freescale.sc.model.entity.event;

import java.io.Serializable;

public class Context implements Serializable
	{
	public String name;
	public String value;
	public String units;
	
	public Context() {}
	
	public Context(String name, String value, String units)
		{
		this.name=name;
		this.value=value;
		this.units=units;
		}
	
	public Context copy() {return new Context(name,value,units);}
	
	public void set(String value) {this.value = value;}
	}