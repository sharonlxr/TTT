package com.freescale.stateStamper.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class Map2StringUtil
	{
	public static String map2Str(Map<String, String> map)
		{
		StringBuilder attributes = new StringBuilder();
		if(map==null)
			{
			return null;
			}
		for (Entry<String, String> entry : map.entrySet())
			{
			attributes.append(entry.getKey());
			attributes.append("=");
			if (isListStr(entry.getValue()))
				{
				attributes.append("[");
				attributes.append(entry.getValue());
				attributes.append("]");
				}
			else
				{
				attributes.append(entry.getValue());
				}
			attributes.append(",");
			}
		if(attributes.length()>0)
			{
			attributes.deleteCharAt(attributes.length() - 1);
			}
		return attributes.toString();
		}

	public static boolean isListStr(String str)
		{
		return str != null && str.contains(",");
		}

	public static Map<String, String> str2Map(String str)
		{
		Map<String, String> map = new HashMap<String, String>();
		if (str == null) return map;
		String value = null;
		String key = null;
		boolean isList = false;
		int start = 0;
		str = str.trim();
		int length = str.length();
		for (int i = 0; i < length; i++)
			{
			char tmp = str.charAt(i);
			if (tmp == '=')
				{
				key = str.substring(start, i);
				start = i + 1;
				}
			else if (tmp == ',')
				{
				if (!isList)
					{
					value = str.substring(start, i);
					}
				}
			else if (tmp == '[')
				{
				start = i + 1;
				isList = true;
				}
			else if (tmp == ']')
				{
				value = str.substring(start, i);
				isList = false;
				i++;
				}
			else if (i == length - 1)
				{
				value = str.substring(start, length);
				}
			if (key != null && value != null)
				{
				map.put(key, value);
				key = null;
				value = null;
				start = i + 1;
				}
			}
		return map;
		}

	}
