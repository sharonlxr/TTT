package com.freescale.stateStamper.db;

import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import junit.framework.TestCase;

import com.freescale.stateStamper.util.Map2StringUtil;

public class Map2StringUtilTestCase extends TestCase 
	{
	Map<String,String> map = new TreeMap<String,String>();
	String mapString;
	protected void setUp() throws Exception
		{
		map.put("A", "B,C");
		map.put("D", "E");
		mapString = "A=[B,C],D=E";
		super.setUp();
		}
	public void testMap2String()
		{
		String map2Str = Map2StringUtil.map2Str(map);
		assertEquals(map2Str, mapString);
		}
	public void testString2Map()
		{
		Map<String, String> str2Map = Map2StringUtil.str2Map(mapString);
		for(Entry<String,String> entry:str2Map.entrySet())
			{
			assertTrue(map.containsKey(entry.getKey()));
			assertTrue(map.containsValue(entry.getValue()));
			}
		}
	
	}
