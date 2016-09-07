package com.freescale.stateStamper.model.entity;

import java.lang.reflect.Field;
import java.util.Map.Entry;
import java.util.Set;

public class CurrentStatus extends Status
	{
	public Status getStatus()
		{
		Status status = new Status();
		Set<Entry<String,Field>> fields = status.getFields().entrySet();
		for(Entry<String,Field> entry:fields)
			{
			String fieldName = entry.getKey();
			Field field = entry.getValue();
			if(field.getType()== Integer.class||field.getType()==String.class)
				{
				status.set(fieldName, this.get(fieldName));
				}
			}
		return status;
		}
	}
