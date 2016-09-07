package com.freescale.stateStamper.action;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.freescale.stateStamper.model.entity.Administrator;
import com.opensymphony.xwork2.Action;
import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.interceptor.AbstractInterceptor;

public class LoginInterceptor extends AbstractInterceptor
	{

	/**
	 * 
	 */
	private static final long serialVersionUID = 4033490835737693471L;

	@Override
	public String intercept(ActionInvocation invocation) throws Exception
		{
		ActionContext ctx = invocation.getInvocationContext();
		Map session = ctx.getSession();
		Administrator user = (Administrator) session.get("user");
		if (user != null && StringUtils.isNotEmpty(user.coreId))
			{
			return invocation.invoke();
			}
		else
			{
			return Action.LOGIN;
			}
		}

	}
