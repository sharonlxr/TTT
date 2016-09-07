package com.freescale.stateStamper.db;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.ojb.broker.accesslayer.conversions.FieldConversion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class Map2Json implements FieldConversion
	{
	private static final long serialVersionUID = 1L;
	private static final String NULLVALUE = "{}";
	private static Logger logger;
	static
		{
		logger = LoggerFactory.getLogger("StateStamper");
		}

	public Object javaToSql(Object source)
		{
		if (source == null)
			{
			return NULLVALUE;
			}
		if (!(source instanceof Map))
			throw new IllegalArgumentException("source is not a Map is a " + source.getClass().getName());
		Map ordered = new TreeMap((Map) source);
		return GsonHelper.toJson(ordered);
		}

	public Object sqlToJava(Object source)
		{
		Map<String, Object> map = new HashMap<String, Object>();
		try
			{
			if (source == null) return map;
			if (source.equals(NULLVALUE)) return map;
			if (!(source instanceof String))
				throw new IllegalArgumentException("source is not a Varchar is a " + source.getClass().getName());
			String src = (String) source;
			map = GsonHelper.fromJson(src, HashMap.class);
			}
		catch (Throwable t)
			{
			logger.error("Map2Json.sqlToJava" + t.getMessage());
			t.printStackTrace();
			}
		return map;
		}


	/**
	 * @return if the passed string is a valid representation of a Map (as
	 *         returned by Map.toString)
	 * 
	 * */
	public boolean isMapStr(String str)
		{
		return str != null && str.startsWith("{") && str.endsWith("}");

		}

	/**
	 * @param source
	 *            a string representation of a List as returned by toString
	 * @return a List created from the string argument if argument is null or
	 *         not a valid string returns null
	 * */
	public List str2List(String source)
		{
		if (source == null) return null;
		if (!isListStr(source)) return null;
		// remove the [ and ]
		source = source.substring(1);
		source = source.substring(0, source.length() - 1);
		List result = new ArrayList();
		String[] elements = getElements(source);
		for (String e : elements)
			{
			String s = e.trim();
			if (!s.equals("")) result.add(s);
			}
		return result;
		}

	/**
	 * @return if the passed string is a valid representation of a List (as
	 *         returned by List.toString)
	 * 
	 * */
	public boolean isListStr(String str)
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
	public Map str2Map(String source)
		{
		if (source == null) return null;
		if (!isMapStr(source)) return null;
		// remove the { and }
		source = source.substring(1);
		source = source.substring(0, source.length() - 1);
		Map map = new HashMap();
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
				if (isListStr(val)) value = str2List(val);
				else if (isMapStr(val)) value = str2Map(val);
				else value = val;
				}
			if (key == null)
				{
				key = "";
				logger.error("Null Key in sqlToJava.str2Map with " + source);
				}
			if (value == null) value = "";
			map.put(key, value);
			}

		return map;
		}
	}
