package com.freescale.stateStamper.model.entity;

import java.util.Comparator;

public class IStatusComparator implements Comparator
	{
	String field,order;
	Class fieldType;
	
	public IStatusComparator(String field)
		{
		this(field,"ASC");
		}
	
	public IStatusComparator(String field,String order)
		{
		this.field = field;
		this.order = order;
		}

	public int compare(Object o1, Object o2)
		{
		IStatus status1 = (IStatus)o1;
		IStatus status2 = (IStatus)o2;
		Object tmp1,tmp2;
		tmp1 = status1.get(field);
		tmp2 = status2.get(field);
		getFieldType(tmp1, tmp2);
		int result = 0;
		if(this.fieldType.equals(Integer.class))
			{
			result = compareInteger((Integer)tmp1,(Integer)tmp2);
			}
		else
			{
			result = compareString((String)tmp1,(String)tmp2);
			}
		if(order.equalsIgnoreCase("desc")){result = -result;}
		return result;
		}

	private void getFieldType(Object tmp1, Object tmp2)
		{
		if(tmp1!=null)
			{
			this.fieldType = tmp1.getClass();
			}
		else if(tmp2!=null)
			{
			this.fieldType = tmp2.getClass();
			}
		else 
			{
			this.fieldType = String.class;
			}
		}
	
	public int compareInteger(Integer o1, Integer o2)
		{
		if(o1==null)o1 = Integer.valueOf(0);
		if(o2==null)o2 = Integer.valueOf(0);
		return o1.compareTo(o2);
		}
	
	public int compareString(String o1, String o2)
		{
		if(o1==null)o1 = "";
		if(o2==null)o2 = "";
		return o1.compareTo(o2);
		}

	}
