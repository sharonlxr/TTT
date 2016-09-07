package com.freescale.stateStamper.db;

import junit.framework.TestCase;

public class StringArray2VarcharFieldConversionTestCase extends TestCase {
	StringArray2VarcharFieldConversion conv;
    private static final String NULLVALUE = "#NULL#";
    private static final String EMPTYCOLLEC = "#EMPTY#";

    protected void setUp() throws Exception {
    	super.setUp();
    	conv= new StringArray2VarcharFieldConversion();
    }
    
	public void testJavaToSql_Null(){
		assertEquals(NULLVALUE, conv.javaToSql(null));
	}
	
	public void testJavaToSql_NotStringArr(){
		Object o = new Object();																						
		try{
			conv.javaToSql(o);
			fail("javaToSql must trhow llegalArgumentException when called with argument different than String[]");
		}catch(IllegalArgumentException ie){
		}catch(Throwable t){
			fail("Not expected exception:" + t.getClass().getName());
		}
	}
	
	public void testJavaToSql_Empty(){
		String [] s= {};
		assertEquals(EMPTYCOLLEC, conv.javaToSql(s));
	}
	
	public void testJavaToSql(){
		String[] s={"Hello","World"};
		assertEquals("Hello#World#",conv.javaToSql(s) );
	}
	
	public void testSqlToJava_Null(){
		assertNull(conv.sqlToJava(NULLVALUE));
	}
	
	public void testSqlToJava_Empty(){
		assertTrue( conv.sqlToJava(EMPTYCOLLEC) instanceof String[]) ;
		assertEquals(0,((String[])conv.sqlToJava(EMPTYCOLLEC)).length);
	}
	
	public void testSqlToJava(){
		String[] s={"Hello","World"};
		String[] s2=(String[])conv.sqlToJava("Hello#World#");
		assertEquals(s.length,s2.length );
		for(int i=0;i< s.length;i++){
			assertEquals(s[i], s2[i]);
		}
	}
	
}
