package com.freescale.stateStamper.model.entity;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

public class TransitionAction implements Serializable
	{
	private static final long serialVersionUID = 1L;
	private Integer primaryKey;
	private String action;
	private Map<String, String> actionParameters;
	private Integer transitionPk;
	public static final String UPDATE_DURABLE = "Update Durable";
	public static final String LOG_UPDATE_DURABLE = "Log on updated variable";
	public static final String INCREASE = "Increase";
	public static final String DECREASE = "Decrease";
	public static final String SET = "Set";
	public static final String RESET = "Reset";
	public static final String SEND_EMAIL = "Send e-mail";
	public static final String SEND_PAGE = "Send Page";
	public static final String ALARM_WINDOW = "Alarm on Window";
	public static final String START_TIMER = "Start Timer";
	public static final String UPDATE_STATE_VIA_EPR = "Update state via EPR";
	public static final String GENERATE_FILE = "Generate File";
	public static final String INCREASE_BY = "Increase by";
	public static final String DECREASE_BY = "Decrease by";
	public static final String PARAM_ATTRIBUTE = "attribute";
	public static final String PARAM_VALUE = "value";
	public static final String PARAM_COLOR = "Color";
	public static final String PARAM_TO = "To";
	public static final String PARAM_BY = "by";
	public static final String CLEAR = "Clear";
	public static final String PARAM = "$";
	public static final String SEND_EPR = "Send EPR";

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

	public Integer getTransitionPk()
		{
		return transitionPk;
		}

	public void setTransitionPk(Integer transPk)
		{
		transitionPk = transPk;
		}

	public boolean onDurable()
		{
		return action != null&& (action.equals(INCREASE) || action.equals(RESET) || action.equals(SET)
			|| action.equals(DECREASE) || action.equals(INCREASE_BY) || action.equals(DECREASE_BY) || action.equals(CLEAR) || action.equals(SEND_EPR)) ;
		}

	public String getActionParameterStr()
		{
		StringBuilder actionParameterStr = new StringBuilder();;
		if (actionParameters != null)
			{
			actionParameterStr.append(actionParameters.toString());
			actionParameterStr.deleteCharAt(0);
			actionParameterStr.deleteCharAt(actionParameterStr.length() - 1);
			}
		return actionParameterStr.toString();
		}

	public void setActionParameterStr(String actionParameterStr)
		{
		actionParameters = new HashMap<String, String>();
		if (StringUtils.isNotEmpty(actionParameterStr))
			{
			String[] elements = actionParameterStr.split(",");
			for (int i = 0; elements != null && i < elements.length; i++)
				{
				String[] tmp = elements[i].split("=");
				if (tmp != null && tmp.length == 2)
					{
					actionParameters.put(tmp[0].trim(), tmp[1].trim());
					}
				}
			}
		}
	}
