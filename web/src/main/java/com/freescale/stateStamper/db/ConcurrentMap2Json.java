package com.freescale.stateStamper.db;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.ojb.broker.accesslayer.conversions.FieldConversion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class ConcurrentMap2Json implements FieldConversion
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
		Map<String, Object> map = new ConcurrentHashMap<String, Object>();
		try
			{
			if (source == null) return map;
			if (source.equals(NULLVALUE)) return map;
			if (!(source instanceof String))
				throw new IllegalArgumentException("source is not a Varchar is a " + source.getClass().getName());
			String src = (String) source;
			map = GsonHelper.fromJson(src, ConcurrentHashMap.class);
			}
		catch (Throwable t)
			{
			logger.error("Map2Json.sqlToJava" + t.getMessage());
			t.printStackTrace();
			}
		return map;
		}

	}
