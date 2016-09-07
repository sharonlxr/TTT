package com.freescale.stateStamper.model.entity;

import org.apache.commons.lang.StringUtils;

import com.freescale.stateStamper.util.TTTUtil;

public abstract class IStatus
	{
	public abstract Object get(String name);
	public abstract boolean has(String name);
	public Object get(EquipmentType eqType,String name)
		{
		Object value = get(name);
		if(eqType!=null)
			{
			EquipmentAttribute attribute = eqType.getAttribute(name);
			if(attribute!=null&&attribute.getConfigOptions()!=null)
				{
				String subType = attribute.getConfigOptions().get(TransitionCondition.SUB_TYPE);
				Boolean display = Boolean.valueOf(attribute.getConfigOptions().get(TransitionCondition.CHECK_NAME_DISPLAY));
				if(display&&TransitionCondition.SUB_TYPE_CORE_ID.equals(subType)&&value!=null)
					{
					String fullName = TTTUtil.getUserFullName(String.valueOf(value));
					if(StringUtils.isEmpty(fullName))
						{
						value="";
						}
					else
						{
						value = fullName;
						}
					}
				}
			}
		return value;
		} 
	
	}
