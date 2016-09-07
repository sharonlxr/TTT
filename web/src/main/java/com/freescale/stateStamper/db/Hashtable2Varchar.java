package com.freescale.stateStamper.db;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.ojb.broker.accesslayer.conversions.FieldConversion;

public class Hashtable2Varchar implements FieldConversion
	{
	private static final long serialVersionUID = 1L;
	private static final String NULLVALUE = "{}";
	private static Logger logger;

	static
		{
		logger = LoggerFactory.getLogger("stateStamper");
		}

	public Object javaToSql(Object source)
		{
		if (source == null)
			{
			return NULLVALUE;
			}
		if (!(source instanceof Hashtable))
			throw new IllegalArgumentException("source is not a Hashtable is a " + source.getClass().getName());
		Map ordered = new TreeMap((Map) source);
		return ordered.toString();
		}

	public Object sqlToJava(Object source)
		{
		Hashtable<String, Object> table = new Hashtable<String, Object>();
		try
			{
			if (source == null)
				{
				return table;
				}
			if (source.equals(NULLVALUE))
				{
				return table;
				}
			if (!(source instanceof String))
				throw new IllegalArgumentException("source is not a Varchar is a " + source.getClass().getName());
			String src = (String) source;
			if (isHashtableStr(src)) table = str2Hashtable(src);
			}
		catch (Throwable t)
			{
			logger.error("Hashtable2Varchar.sqlToJava"+t.getMessage());
			t.printStackTrace();
			}
		return table;
		}

	public Hashtable str2Hashtable(String source)
		{
		if (source == null) return null;
		if (!isHashtableStr(source)) return null;
		// remove the { and }
		source = source.substring(1);
		source = source.substring(0, source.length() - 1);
		Hashtable table = new Hashtable();
		String[] pairs = getElements(source);
		for (int i = 0; i < pairs.length; i++)
			{
			String[] pair = pairs[i].split("=");
			if (pair == null) return null;
			String key = null;
			if (pair.length > 0) key = pair[0].trim();
			Object value = null;
			if (pair.length > 1)
				{
				String val = pair[1].trim();
				if (isVectorStr(val)) value = str2Vector(val);
				else if (isHashtableStr(val)) value = str2Hashtable(val);
				else value = val;
				}
			if (key == null)
				{
				key = "";
				logger.error("Null Key in sqlToJava.str2Hashtable with " + source);
				}
			if (value == null) value = "";
			table.put(key, value);
			}

		return table;
		}

	/**
	 * @return if the passed string is a valid representation of a Hashtable (as
	 *         returned by Hashtable.toString)
	 * 
	 * */
	public boolean isHashtableStr(String str)
		{
		return str != null && str.startsWith("{") && str.endsWith("}");

		}

	/**
	 * @param source
	 *            a string representation of a vector as returned by toString
	 * @return a Vector created from the string argument if argument is null or
	 *         not a valid string returns null
	 * */
	public Vector str2Vector(String source)
		{
		if (source == null) return null;
		if (!isVectorStr(source)) return null;
		// remove the [ and ]
		source = source.substring(1);
		source = source.substring(0, source.length() - 1);
		Vector result = new Vector();
		String[] elements = getElements(source);
		for (String e : elements)
			{
			String s = e.trim();
			if (!s.equals("")) result.add(s);
			}
		return result;
		}

	/**
	 * @return if the passed string is a valid representation of a Vector (as
	 *         returned by Vector.toString)
	 * 
	 * */
	public boolean isVectorStr(String str)
		{
		return str != null && str.startsWith("[") && str.endsWith("]");
		}

	public String[] getElements(String source)
		{
		String[] res = new String[0];
		if (source == null || "".equals(source.trim())) return res;
		String[] aux = source.split(",");
		final int START = 0;
		final int COMPLETE = 1;
		final int IN_V = 2;
		int state = START;
		StringBuffer auxbuff = new StringBuffer();
		List<String> elements = new ArrayList<String>();
		String e;
		for (int i = 0; i < aux.length; i++)
			{
			e = aux[i];
			switch (state)
				{
				case START:
					auxbuff.append(e);
					if (e.contains("[") && !e.contains("]")) state = IN_V;
					else
						{
						state = COMPLETE;
						}
				break;

				case IN_V:
					auxbuff.append(",");
					auxbuff.append(e);
					if (e.contains("]"))
						{
						state = COMPLETE;
						}
				break;
				}

			if (state == COMPLETE)
				{
				elements.add(auxbuff.toString());
				state = START;
				auxbuff = new StringBuffer();
				}

			}

		return elements.toArray(res);
		}

	}
