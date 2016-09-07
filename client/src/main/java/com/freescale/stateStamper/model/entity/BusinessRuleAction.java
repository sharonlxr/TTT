package com.freescale.stateStamper.model.entity;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;

import com.freescale.stateStamper.util.Map2StringUtil;

public class BusinessRuleAction implements Serializable
	{
	private static final long serialVersionUID = 1L;
	private Integer primaryKey;
	private String action;
	private Map<String, String> actionParameters;
	private Integer businessRulePk;

	public static final String UPDATE_DURABLE = "Update Durable";
	public static final String LOG_UPDATE_DURABLE = "Log on updated variable";
	public static final String INCREASE = "Increase";
	public static final String INIT_STATE = "Initialize State";
	public static final String SEND_EMAIL = "Send e-mail";
	public static final String SEND_PAGE = "Send Page";
	public static final String ALARM_WINDOW = "Alarm on Window";
	public static final String START_TIMER = "Start Timer";
	public static final String UPDATE_STATE_VIA_EPR = "Update state via EPR";
	public static final String GENERATE_FILE = "Generate File";
	public static final String RESET_COUNTER = "Reset Counter";
	public static final String GENERATE_FILE_TEMPLATE = "Generate File from Template";
	public static final String MONITOR_PAGE = "Display in Monitor Page";
	public static final String ALARM_REPORT = "Display CounterBar";
	public static final String UPDATE_MULTIPLE = "Update Multiple";
	public static final String CHANGE_HEADER = "Change Header";
	public static final String DISPLAY_MONITOR = "Display Monitor";
	public static final String EMAIL_MONITOR = "E-mail monitor";
	public static final String ATTACH_MULTIPLE = "Attach Multiple";
	public static final String SET_INDEX = "Set Index";
	public static final String UPDATE_MULTIPLE_BY_INDEX = "Update Multiple by Index";
	public static final String UPDATE_ATTRIBUTE_EXTERNAL = "Update Attribute with External Source";
	public static final String ADD_VERIFICATION_CHECKBOX = "Add Verification Checkbox";
	public static final String AUTO_NAME_GENERATION = "Auto Model Name";
	public static final String AUTO_DESCRIPTION_GENERATION = "Auto Model Description";
	public static final String MANDATORY_ATTRIBUTES = "Mandatory Fields";
	public static final String OPTIONAL_ATTRIBUTES = "Optional Fields";
	public static final String DROPDOWN_ATTRIBUTES = "Dropdown Values";
	public static final String GENESIS_ATTRIBUTES = "Genesis Values";
	public static final String CREATOR_ROLE = "Creator Role";
	public static final String RELEASER_ROLE = "Releaser Role";
	public static final String APPROVER_ROLE = "Approver Role";
	public static final String APPROVAL_PROCESS = "Approval Process";
	public static final String OVERWRITE_DURABLE_ATTRIBUTES = "Overwrite Durable Attributes";
	public static final String MODEL_MAPPING_CONFIGURATION = "Model Mapping Configuration";

	public Integer getPrimaryKey()
		{
		return primaryKey;
		}

	public void setPrimaryKey(Integer primaryKey)
		{
		this.primaryKey = primaryKey;
		}

	public String getAction()
		{
		return action;
		}

	public void setAction(String action)
		{
		this.action = action;
		}

	public Map<String, String> getActionParameters()
		{
		return actionParameters;
		}

	public void setActionParameters(Map<String, String> actionParameters)
		{
		this.actionParameters = actionParameters;
		}

	public Integer getBusinessRulePk()
		{
		return businessRulePk;
		}

	public void setBusinessRulePk(Integer businessRulePk)
		{
		this.businessRulePk = businessRulePk;
		}

	public static String getActionsAsString()
		{
		List actions = new ArrayList();
		Field field, fields[] = BusinessRuleAction.class.getDeclaredFields();
		int modifiers;
		for (int i = 0; i < fields.length; i++)
			{
			field = fields[i];
			modifiers = field.getModifiers();
			if (Modifier.isFinal(modifiers) && Modifier.isStatic(modifiers) && Modifier.isPublic(modifiers)
					&& field.getType().getName().endsWith("String"))
				{
				try
					{
					actions.add(field.get(null));
					}
				catch (IllegalArgumentException e)
					{
					e.printStackTrace();
					}
				catch (IllegalAccessException e)
					{
					e.printStackTrace();
					}
				}
			}
		Collections.sort(actions);
		return actions.toString().replace("[", "").replace("]", "").replace(", ", ",");
		}

	public String getActionParameterStr()
		{
		return Map2StringUtil.map2Str(actionParameters);
		}
	

	public void setActionParameterStr(String actionParameterStr)
		{
		this.actionParameters = Map2StringUtil.str2Map(actionParameterStr);
		}
	}
