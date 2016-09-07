package com.freescale.stateStamper.db;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Map2JsonTestCase extends TestCase
	{
	Map2Json convert;

	protected void setUp() throws Exception
		{
		super.setUp();
		convert = new Map2Json();
		}

	public void testJavaToSqlNull()
		{
		assertEquals("{}", convert.javaToSql(null));
		}

	public void testJavaToSqlEmpty()
		{
		Map<String, String> map = new HashMap<String, String>();
		assertEquals("{}", convert.javaToSql(map));
		}

	public void testJavaToSql()
		{
		Map<String, String> map = new HashMap<String, String>();
		map.put("A", "Alpha");
		assertEquals("{\"A\":\"Alpha\"}", convert.javaToSql(map));
		map.put("B", "Beta");
		assertEquals("{\"A\":\"Alpha\",\"B\":\"Beta\"}", convert.javaToSql(map));
		}

	public void testSqlToJavaNull()
		{
		Map<String, String> empty = new HashMap<String, String>();
		assertEquals(empty, convert.sqlToJava(null));
		}

	public void testSqlToJavaEmpty()
		{
		Map<String, String> empty = new HashMap<String, String>();
		assertEquals(empty, convert.sqlToJava("{}"));
		}

	public void testSqlToJava()
		{
		String key = "A";
		String value = "Alpha";
		String sql = "{\"" + key + "\":\"" + value + "\"}";
		Map map = (Map) convert.sqlToJava(sql);
		assertEquals(1, map.size());
		assertTrue(map.containsKey(key));
		assertEquals(value, map.get(key));

		String key2 = "B";
		String value2 = "Beta";
		sql = sql.substring(0, sql.length() - 1);
		sql += ",\"" + key2 + "\":\"" + value2 + "\"}";
		map = (Map) convert.sqlToJava(sql);
		assertEquals(2, map.size());
		assertTrue(map.containsKey(key));
		assertEquals(value, map.get(key));

		assertTrue(map.containsKey(key2));
		assertEquals(value2, map.get(key2));

		}

	public void testSqlToJavaList()
		{
		String key = "V";
		List<String> value = new ArrayList<String>();
		value.add("1");
		String sql = "{\"" + key + "\":[\"1\"]}";
		Map map = (Map) convert.sqlToJava(sql);
		assertEquals(1, map.size());
		assertTrue(map.containsKey(key));
		assertEquals(value, map.get(key));

		value.add("2");
		sql = "{\"" + key + "\":[\"1\",\"2\"]}";
		map = (Map) convert.sqlToJava(sql);
		assertEquals(1, map.size());
		assertTrue(map.containsKey(key));
		assertEquals(value, map.get(key));

		String key2 = "X";
		List<String> value2 = new ArrayList<String>();
		value2.add("4");
		value2.add("5");
		sql = "{\"" + key + "\":[\"1\",\"2\"],\"" + key2 + "\":[\"4\",\"5\"]}";
		map = (Map) convert.sqlToJava(sql);
		assertEquals(2, map.size());
		assertTrue(map.containsKey(key));
		assertEquals(value, map.get(key));

		assertTrue(map.containsKey(key2));
		assertEquals(value2, map.get(key2));

		}

	public void testJavaList2Sql()
		{
		Map<String, List<String>> obj = new HashMap<String, List<String>>();
		String key = "V";
		List<String> v = new ArrayList<String>();
		v.add("1");
		v.add("2");
		obj.put(key, v);
		String sql = convert.javaToSql(obj).toString();
		assertEquals("{\"" + key + "\":[\"1\",\"2\"]}", sql);
		Map java = (Map) convert.sqlToJava(sql);
		assertEquals(obj, java);
		}

	public void testSqlToJavaNested()
		{
		String key = "H";
		Map<String, String> value = new HashMap<String, String>();
		value.put("K1", "V1");
		String sql = "{\"" + key + "\":" + value + "}";
		Map map = (Map) convert.sqlToJava(sql);
		assertEquals(1, map.size());
		assertTrue(map.containsKey(key));
		assertEquals(value, map.get(key));
		}

	public void testSortMap()
		{
		Map<String, String> map = new HashMap<String, String>();
		map.put("A", "1");
		map.put("C", "3");
		map.put("B", "2");
		String sql = convert.javaToSql(map).toString();
		String[] elements = sql.split(",");
		for (int i = 0; i < elements.length; i++)
			{
			assertTrue(elements[i].indexOf("" + (i + 1)) > -1);
			}
		}
	
	public void testStr2Map()
		{
		Map<String, String> map = new HashMap<String, String>();
		map.put("A", "1");
		map.put("C", "3");
		map.put("B", "2");
		Map mp2 = convert.str2Map(map.toString());
		assertEquals(map, mp2);
		}
	}
