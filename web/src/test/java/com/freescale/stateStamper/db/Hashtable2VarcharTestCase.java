package com.freescale.stateStamper.db;

import junit.framework.TestCase;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

public class Hashtable2VarcharTestCase extends TestCase {
    Hashtable2Varchar convert;
	protected void setUp() throws Exception {
		super.setUp();
		convert = new Hashtable2Varchar();
	}
	
	public void testJavaToSqlNull(){
		assertEquals("{}",convert.javaToSql(null)) ;
	}
	
	public void testJavaToSqlEmpty(){
		Hashtable<String, String>  table = new Hashtable<String, String>();
		assertEquals("{}",convert.javaToSql(table));
	}
	
	public void testJavaToSql(){
		Hashtable<String, String>  table = new Hashtable<String, String>();
		table.put("A", "Alpha");
		assertEquals("{A=Alpha}",convert.javaToSql(table));
		table.put("B", "Beta");
		assertEquals("{A=Alpha, B=Beta}",convert.javaToSql(table));
	}
	
	public void testSqlToJavaNull(){
		Hashtable<String, String> empty=new Hashtable<String, String>();
		assertEquals(empty,convert.sqlToJava(null));
	}
	
	public void testSqlToJavaEmpty(){
		Hashtable<String, String> empty=new Hashtable<String, String>(); 
		assertEquals(empty,convert.sqlToJava("{}"));
	}
	
	public void testSqlToJava()
	   {
		String key = "A";
		String value = "Alpha";
		String sql= "{"+key+"="+value+"}";
		Hashtable table = (Hashtable)convert.sqlToJava(sql);
		assertEquals(1,table.size());
		assertTrue(table.containsKey(key));
		assertEquals(value,table.get(key));
		
		String key2= "B";
		String value2="Beta";
		sql=sql.substring(0,sql.length()-1);
		sql+=", " +key2+"="+value2+"}";
		table = (Hashtable)convert.sqlToJava(sql);
		assertEquals(2,table.size());
		assertTrue(table.containsKey(key));
		assertEquals(value,table.get(key));
		
		assertTrue(table.containsKey(key2));
		assertEquals(value2,table.get(key2));

	   }
	
	public void testSqlToJavaVector()
	   {
		String key = "V";
		Vector value = new Vector();
		value.add("1");
		String sql= "{"+key+"="+value+"}";
		Hashtable table = (Hashtable)convert.sqlToJava(sql);
		assertEquals(1,table.size());
		assertTrue(table.containsKey(key));
		assertEquals(value,table.get(key));
		
		value.add("2");
		sql= "{"+key+"="+value+"}";
		table = (Hashtable)convert.sqlToJava(sql);
		assertEquals(1,table.size());
		assertTrue(table.containsKey(key));
		assertEquals(value,table.get(key));
		
		String key2="X";
		Vector value2= new Vector();
		value2.add("4");
		value2.add("5");
		sql="{"+key+"="+value+","+key2+"="+value2+"}";
		table = (Hashtable)convert.sqlToJava(sql);
		assertEquals(2,table.size());
		assertTrue(table.containsKey(key));
		assertEquals(value,table.get(key));
		
		assertTrue(table.containsKey(key2));
		assertEquals(value2,table.get(key2));
		
 
		
	   }
	public void testJavaVector2Sql()
		{
		Map obj = new Hashtable();
		String key = "V";
		String val;
		Vector v = new Vector();
		v.add("1");
		v.add("2");
		obj.put(key,v);
		String sql=convert.javaToSql(obj).toString();
		assertEquals("{"+key+"="+v.toString()+"}", sql);
		Hashtable java = (Hashtable)convert.sqlToJava(sql);
		assertEquals(obj, java);
		}

	public void _testSqlToJavaNested()
	   {
		String key="H";
		Hashtable value = new Hashtable();
		value.put("K1", "V1");
		String sql= "{"+key+"="+value+"}";
		Hashtable table = (Hashtable)convert.sqlToJava(sql);
		assertEquals(1,table.size());
		assertTrue(table.containsKey(key));
		assertEquals(value,table.get(key));
//		value.put("H1", "V1");
	   }
	
	public void testStr2Vector(){
		Vector source = new Vector();
		Vector result;
		
		result = (Vector)convert.str2Vector(source.toString());
		assertEquals(0,result.size());
		assertEquals(source,convert.str2Vector(source.toString()) );
		
		source.add("A");
		result = ((Vector)convert.str2Vector(source.toString()));
		assertEquals(1,result.size());
		assertEquals(source,convert.str2Vector(source.toString()) );
		
		source.add("B");
		result = ((Vector)convert.str2Vector(source.toString()));
		assertEquals(2,result.size());
		assertEquals(source,convert.str2Vector(source.toString()) );
	}

	public void testGetElements(){
		String source = "A=Alpha";
		String [] elements;
		elements = convert.getElements(source);
		assertEquals(1,elements.length);
		assertEquals(source,elements[0]);
		
		source = "A=Alpha,B=Beta";
		elements = convert.getElements(source);
		assertEquals(2,elements.length);
		String s= source.substring(0,source.indexOf(","));
		assertEquals(s,elements[0]);
		
	}
	
	public void testGetElements_Vector(){
		String source = "v=[1]";
		String [] elements;
		elements = convert.getElements(source);
		assertEquals(1,elements.length);
		assertEquals(source,elements[0]);

		source="v=[1,2,3]";
		elements = convert.getElements(source);
		assertEquals(1,elements.length);
		assertEquals(source,elements[0]);

		source="v=[1,2,3],x=[4,5]";
		elements = convert.getElements(source);
		assertEquals(2,elements.length);
		String s = source.substring(0,source.indexOf("]")+1);
		
		assertEquals( s,elements[0]);
		s = source.substring(source.indexOf("]")+2);
		assertEquals(s, elements[1]);
	}
	
	String array2Str(String[] src){
		StringBuffer sb = new StringBuffer();
		for(int i=0; i < src.length;i++){
		  sb.append(src[i]+",");	
		}
		return sb.toString();
	}
	public void testGetElements_Table(){
		String source = "m={1}";
		String [] elements;
		elements = convert.getElements(source);
		assertEquals(1,elements.length);
		assertEquals(source,elements[0]);

		source="m={x},n={y}";
		elements = convert.getElements(source);
		assertEquals(2,elements.length);
		String s = source.substring(0,source.indexOf(",") );
		assertEquals(s,elements[0]);
		s = source.substring(source.indexOf(",")+1,source.length() );
		assertEquals(s,elements[1]);
	}
	
	
	public void testSort_Table()
		{
		Map table = new Hashtable();
		table.put("A", "1");
		table.put("C", "3");
		table.put("B", "2");
		String sql = convert.javaToSql(table).toString();
		String []elements = sql.split(",");
		for(int i=0;i<elements.length;i++)
			{
			 assertTrue( elements[i].indexOf(""+(i+1)) >-1);
			}
		}

}
