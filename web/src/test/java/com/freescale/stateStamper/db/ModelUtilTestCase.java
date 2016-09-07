package com.freescale.stateStamper.db;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import com.freescale.stateStamper.model.entity.Model;
import com.freescale.stateStamper.util.ModelUtil;

import junit.framework.TestCase;

public class ModelUtilTestCase extends TestCase
	{
	private Model model = new Model();
	String namingRule = "LB-${tester}-${nickName}-${pkgCode}-${parellel}-${handler}";
	protected void setUp() throws Exception
		{
		model.setPlatform("MS32T");
		HashMap map = new HashMap<String,String>();
		map.put("tester", "J750");
		map.put("nickName", "ABCD");
		map.put("pkgCode", "BGA");
		map.put("parellel", "X3");
		map.put("handler", "CASTLE");	
		model.setModelAttributes(map);
		}
	
	public void testGetModelDescription()
		{
		String modelDescription = ModelUtil.getModelDescription(model, namingRule);
		assertEquals("LB-J750-ABCD-BGA-X3-CASTLE",modelDescription);
		}
	public void testGenerateFilteredModelName()
		{
		assertNotNull(ModelUtil.generateFilterModelToken());
		}
	public void testGenerateGenericModelName()
		{
		assertNotNull(ModelUtil.generateGenericModelToken());
		}
	}
