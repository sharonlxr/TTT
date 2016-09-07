package com.freescale.stateStamper.model.logic;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.freescale.stateStamper.db.TransactionPersistence;
import com.freescale.stateStamper.model.entity.BatchTransactionBean;
import com.freescale.stateStamper.model.entity.Machine;

public class TransactionManager
	{
	private List<BatchTransactionBean> memorySyncActions = new ArrayList<BatchTransactionBean>();
	private Logger logger = LoggerFactory.getLogger("transactionManager");
	private TransactionPersistence tp = new TransactionPersistence();

	public TransactionManager()
		{
		
		}
	
	public void beginTransaction()
		{
		tp.beginTransaction();
		}

	public void executeBatch() throws Throwable
		{
		try
			{
			tp.commitTransaction();
			for (BatchTransactionBean single : memorySyncActions)
				{
				setMachine(single);
				}
			}
		finally
			{
			memorySyncActions.clear();
			}
		}

	public Object store(Object entity) throws Throwable
		{
		tp.store(entity);
		logger.info(entity.getClass().getName() + " has been updated/created - "+ entity.toString() );
		return entity;
		}

	public void setMachine(Object entity, TransactionMemorySyncMaster materBean)
		{
		BatchTransactionBean batchTransactionBean = new BatchTransactionBean();
		batchTransactionBean.setEntity(entity);
		batchTransactionBean.setMasterBean(materBean);
		memorySyncActions.add(batchTransactionBean);
		}

	public void delete(Object entity) throws Throwable
		{
		tp.delete(entity);
		logger.info(entity.getClass().getName() + " has been deleted - "+ entity.toString() );
		}
	
	public void rollback()
		{
		tp.rollback();
		}
	
	public void close()
		{
		tp.close();
		}
	

	private void setMachine(BatchTransactionBean bean)
		{
		if(bean.getEntity()!=null&&bean.getMasterBean()!=null)
			{
			TransactionMemorySyncMaster master = (TransactionMemorySyncMaster) bean.getMasterBean();
			master.setMachine((Machine)bean.getEntity());
			}
		}

	}