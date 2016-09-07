
package com.freescale.stateStamper.db;

import org.apache.ojb.broker.*;
import org.apache.ojb.broker.accesslayer.ConnectionManagerImpl;

public class TransactionPersistence
	{
	PersistenceBroker transPersistence = null;
	
	public TransactionPersistence()
		{
		}
	
	public void beginTransaction()
		{
		transPersistence = PersistenceBrokerFactory.defaultPersistenceBroker();
		transPersistence.beginTransaction();
		}
	
	public void store(Object entity) throws Throwable
		{
		try
			{
			transPersistence.store(entity);
			}
		catch (Throwable t)
			{
			clean(t);
			throw t;
			}
		}
	
	public void delete(Object entity) throws Throwable
		{
		try
			{
			transPersistence.delete(entity);
			}
		catch (Throwable t)
			{
			clean(t);
			throw t;
			}
		}
	
	public void commitTransaction() throws Throwable
		{
		transPersistence.commitTransaction();
		}
	
	public void rollback()
		{
		transPersistence.abortTransaction();
		}
	
	public void close()
		{
		if(transPersistence!=null)
			{
			transPersistence.close();
			}
		transPersistence = null;
		}
	
	private void clean(Throwable t)
		{		
		if (t.toString().indexOf("ORA-01652") >= 0 || t.toString().indexOf("ORA-22275") >= 0)
			{
			try {if (transPersistence != null && transPersistence.isInTransaction()) transPersistence.abortTransaction();}
			catch (Throwable t1) {}
			((ConnectionManagerImpl)transPersistence.serviceConnectionManager()).getUnderlyingConnectionFactory().releaseAllResources();
			}
		}
	
	}