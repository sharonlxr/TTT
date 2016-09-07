package com.freescale.stateStamper.model.entity;

import java.util.Comparator;

public class SortDurable implements Comparator{
		

	private String order;

	public SortDurable(String order)
		{
		this.order = order;
		}

	public int compare(Object o1, Object o2)
		{
		return ((String) ((Machine) o1).get(order)).compareTo((String) ((Machine) o2).get(order));
		}

	public boolean equals(Object obj)
		{
		return this.equals(obj);
		}
	
}
