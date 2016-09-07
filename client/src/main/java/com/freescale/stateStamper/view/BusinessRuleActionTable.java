package com.freescale.stateStamper.view;

import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.Vector;

import com.freescale.clientUtil.Form;
import com.freescale.clientUtil.Table;
import com.freescale.clientUtil.Util;
import com.freescale.stateStamper.model.entity.BusinessRule;
import com.freescale.stateStamper.model.entity.BusinessRuleAction;
import com.freescale.stateStamper.model.entity.FileGenerated;

public class BusinessRuleActionTable extends Table implements ActionListener 
	{
	private static final long serialVersionUID = 1L;
	private BusinessRule rule;
	
	public BusinessRuleActionTable(BusinessRule bizRule,Vector records, Object parent)
		{
		super("BusinessRuleActions", records, null, parent, false);
		rule = bizRule;
		}

	public void fieldChanged(Form form, String field, Object object) throws Throwable
		{
		String actionStr = object.toString();
		if (BusinessRuleAction.GENERATE_FILE_TEMPLATE.equals(actionStr))
			{
			FileGenerated file = new FileGenerated();
			Form config = new Form("GenerateFileConfig", file, this);
			Util.createWindow("File Configuration", config, this);
			file = (FileGenerated) config.getObject();
			String parameters = "fileName="+file.getFileName()+",timeout="+file.getTimeout()+",template="+file.getTemplate();
			form.setValue("actionParameters", parameters);
			}
		}
	
	public void save(Form form) throws Throwable
		{
		BusinessRuleAction action = (BusinessRuleAction)form.getObject();
		action.setBusinessRulePk(rule.getPrimaryKey());
		Util.request(action, "store.do");
		form.cancel();
		search();
		Util.createWindow("Success", new Form("Success", "", this, 0), this);
		}

	public void search() throws Throwable
		{
		BusinessRuleAction action = new BusinessRuleAction();
		action.setBusinessRulePk(rule.getPrimaryKey());
		records = (Vector) Util.request(action,"search.do");
		if (sort && records != null) Collections.sort(records, new Util.Compare());
		model.fireTableRowsInserted(0, records.size() - 1);
		resize();
		}

	}
