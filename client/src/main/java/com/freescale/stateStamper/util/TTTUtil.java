package com.freescale.stateStamper.util;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchResult;

import org.apache.commons.lang.StringUtils;


public class TTTUtil
	{
	private static final String APPLICATION_FILTER = "ou=application users,ou=applications,ou=intranet,dc=motorola,dc=com";
	private static final String	USER_ATTRIBUTE = "uid";
	private static final String USER_FILTER = "ou=people,ou=intranet,dc=motorola,dc=com";
	private static final String URL = "ldaps://fsl-ids.freescale.net:636";
	private static final String SERVICE_USER = "EWM";
	private static final String SERVICE_PASSWORD = "zd31_Ewm1";
	private static Hashtable<String,String> env = new Hashtable<String,String>();
	private static Map<String,String> namesMap = new HashMap<String,String>();
	
	static
		{
		init();
		}
	/**
	 * 
	 * @param user
	 * @return
	 * @throws Throwable
	 */
	public static String getUserFullName(String user)
		{
		String result = null;
		if(StringUtils.isEmpty(user))return user;
		user = user.toUpperCase();
		if(namesMap.containsKey(user))
			{
			return namesMap.get(user);
			}
		try
			{
			DirContext ctx = new InitialDirContext(env);
			Attributes attributes = new BasicAttributes(true);
			attributes.put(new BasicAttribute(USER_ATTRIBUTE, user));
			NamingEnumeration search = ctx.search(USER_FILTER,attributes);
			if (search.hasMore())
				{
				SearchResult searchResult = (SearchResult) search.next();
				Attribute attribute = searchResult.getAttributes().get("motCommonNames");
				if(attribute!=null)
					{
					result = (String)attribute.get(0);
					namesMap.put(user, result);
					}
				}
			ctx.close();
			}
		catch (NamingException e)
			{
			e.printStackTrace();
			}
		return result;
		}
	/**
	 * get all LDAP infos, it will be here for future usage
	 */
	public static String getInfo(String user) throws Throwable
		{
		String result = null;
		init();
		DirContext ctx = new InitialDirContext(env);
		Attributes attributes = new BasicAttributes(true);
		attributes.put(new BasicAttribute(USER_ATTRIBUTE, user));
		NamingEnumeration search = ctx.search(USER_FILTER,attributes);
		if (search.hasMore())
			{
			SearchResult searchResult = (SearchResult) search.next();
			NamingEnumeration all = searchResult.getAttributes().getAll();
			while(all.hasMore())
				{
				Attribute next = (Attribute) all.next();
				System.out.println(next);
				}
			}
		ctx.close();
		return result;
		}

	private static void init()
		{
		namesMap.clear();
		env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
		env.put(Context.PROVIDER_URL, URL);
		env.put(Context.SECURITY_AUTHENTICATION, "simple");
		env.put(Context.SECURITY_PRINCIPAL, "cn=" + SERVICE_USER + "," + APPLICATION_FILTER);
		env.put(Context.SECURITY_CREDENTIALS, SERVICE_PASSWORD);
		}
	
	public static void main(String[] args) throws Throwable
		{
		System.out.println(TTTUtil.getInfo("B14275"));
		}
	}
