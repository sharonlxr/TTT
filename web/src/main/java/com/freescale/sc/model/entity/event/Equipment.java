/* COPYRIGHT FREESCALE SEMICONDUCTOR INC., AUSTIN TEXAS, 2008 */
/* $Id: Equipment.java,v 1.4 2008/10/11 01:07:55 ra7956 Exp $ */

package com.freescale.sc.model.entity.event;

import java.io.Serializable;
import java.util.*;

public class Equipment implements Serializable, Cloneable
	{
	public String type;
	public String id;
	public String location;
	public boolean isPrimary;
	public Vector dependentTools = new Vector();
	
	public Equipment() {}
	
	public Equipment(String type, String id, String location, boolean isPrimary, Vector dependentTools)
		{
		this.type=type;
		this.id=id;
		this.location=location;
		this.isPrimary=isPrimary;
		this.dependentTools=dependentTools;
		}
	
	public Equipment copy()
		{
		Equipment aux = new Equipment(type,id,location,isPrimary,new Vector());
		if (dependentTools != null) for (int i=0; i<dependentTools.size(); i++) aux.dependentTools.addElement(((Equipment)dependentTools.elementAt(i)).copy());
		return aux;
		}
	}
