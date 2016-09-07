package com.freescale.stateStamper.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;

import com.freescale.stateStamper.model.entity.Administrator;

public class SessionFilter implements Filter
	{

	private ServletContext context;
	private List<String> urlList;

	public void init(FilterConfig fConfig) throws ServletException
		{
		this.context = fConfig.getServletContext();
		this.context.log("SessionnFilter initialized");
		String urls = fConfig.getInitParameter("nonSession-URL");
		urlList = new ArrayList<String>();
		urlList = Arrays.asList(urls.split("\\s*,\\s*"));
		}

	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException
		{
		HttpServletRequest request = (HttpServletRequest) req;
		HttpServletResponse response = (HttpServletResponse) res;
		HttpSession session = request.getSession();
		Administrator admin = (Administrator) session.getAttribute("user");
		String url = request.getRequestURI();
		String action = url.substring(url.lastIndexOf("/") + 1, url.length());
		boolean avoidURLs = urlList.contains(action);

		if (avoidURLs || action.equals(""))
			{
			session = request.getSession(false);
			chain.doFilter(request, response);
			}
		else if (action.startsWith("durable"))
			{
			action = action.substring(0, action.length() - 3);
			if (!action.equals("durable_login") && !action.equals("durable_main"))
				{
				if (session.isNew() || admin == null || StringUtils.isEmpty(admin.coreId))
					{
					String originPage  = url.substring(url.lastIndexOf("/") + 1, url.length());
					String qry = request.getQueryString();
					originPage+= qry != null?"?"+qry:"";
					request.setAttribute("originPage",originPage);
					request.getRequestDispatcher("/durable_main.do").forward(request, response);
					}
				else
					{
					chain.doFilter(request, response);
					}
				}
			else
				{
				chain.doFilter(request, response);
				}
			}
		else
			{
			chain.doFilter(request, response);
			}
		}

	public void destroy()	{}
	}
