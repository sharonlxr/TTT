package com.freescale.stateStamper.db;

import java.util.Vector;

import com.freescale.stateStamper.model.entity.Email;

import junit.framework.TestCase;

public class EmailVector2VarcharTestCase extends TestCase {
	EmailVector2Varchar conv;
    private static final String NULLVALUE = "#NULL#";
    private static final String EMPTYCOLLEC = "#EMPTY#";
    private static final String SEPARATOR = "#";
    private static final String MyMail = "b21032@freescale.com";

	protected void setUp() throws Exception {
		super.setUp();
		conv = new EmailVector2Varchar();
	}

	
	public void testSqlToJava_Null(){
		assertNull(conv.sqlToJava(NULLVALUE));
	}
	
	public void testSqlToJava_Empty(){
		assertEquals(new Vector(),  conv.sqlToJava(EMPTYCOLLEC) );
	}
	
	public void testSqlToJava() {
		Email email = new Email();
		email.type = Email.TO;
		email.email="B21032@freescale.com";
		Email emaildb;
		Vector v = (Vector)conv.sqlToJava("To=B21032@freescale.com#");//Email.TO+"="+MyMail+SEPARATOR);
		assertEquals(1, v.size());
		emaildb= (Email)v.get(0);
		assertEquals(email.type, emaildb.type);
		assertEquals(email.email, emaildb.email);
	}
	

	public void testJavaToSql_Null() {
		assertEquals(NULLVALUE, conv.javaToSql(null));
	}


	public void testJavaToSql_Empty() {
		assertEquals(EMPTYCOLLEC, conv.javaToSql(new Vector()));
	}

	public void testJavaToSql() {
		Email email = new Email();
		email.type=Email.TO;
		email.email=MyMail;
		Vector mails = new Vector();
		mails.add(email);
		assertEquals(Email.TO+"="+MyMail+SEPARATOR, conv.javaToSql(mails));
		Email email2 = new Email();
		email2.type=Email.CC;
		email2.email="b21032@fsl.net";
		mails.add(email2);
		assertEquals(Email.TO+"="+MyMail+SEPARATOR+
				    Email.CC+"=b21032@fsl.net"+SEPARATOR,
				    conv.javaToSql(mails));
		
	}


}
