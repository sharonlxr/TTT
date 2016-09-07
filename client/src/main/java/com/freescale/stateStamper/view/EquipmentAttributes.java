package com.freescale.stateStamper.view;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;


import com.freescale.clientUtil.Form;
import com.freescale.clientUtil.Table;
import com.freescale.clientUtil.Util;
import com.freescale.stateStamper.model.entity.ComplexAttribute;
import com.freescale.stateStamper.model.entity.EquipmentAttribute;
import com.freescale.stateStamper.model.entity.EquipmentType;
import com.freescale.stateStamper.model.entity.ThresholdCounter;

public class EquipmentAttributes extends Table implements ActionListener 
	{
	private static final long serialVersionUID = 1L;
	public String oldCounterName;
	private Map tcAlias;
	private Map updates;

	public EquipmentAttributes(String type, Vector records, Color[] colors, EquipmentTypeSetup parent)
		{
		super(type, records, colors, parent, false);
		tcAlias = new HashMap();
		updates = new HashMap();
		updates.put("equipmentType", parent.getEquipmentType());
		updates.put("added", new Vector());
		updates.put("edited", new HashMap());
		updates.put("deleted", new Vector());
		}
	

	public void save() throws Throwable
		{
		try
			{
			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			List rows= getRecords();
			for(int i=0;rows!=null&&i<rows.size();i++)
				{
				EquipmentAttribute attribute = (EquipmentAttribute)rows.get(i);
				if(EquipmentAttribute.THRESHOLDCOUNTER.equals( attribute.type))
					{
					boolean newAttribute = attribute.typeKey ==null || attribute.typeKey.intValue() ==0; 
					if(newAttribute)
						{
						ThresholdCounter counter = new ThresholdCounter(attribute.name);
						Map countAttribs=counter.getAttributesMap();
						countAttribs.remove(attribute.name);
						rows.addAll( countAttribs.values());
						List adds = (List)updates.get("added");
						adds.addAll(countAttribs.values());
						}
					else
						{
						Map edit = (Map)updates.get("edited");
						if(edit.containsKey(attribute.primaryKey))
							{
							List pair = (List)edit.get(attribute.primaryKey);
							EquipmentAttribute before = (EquipmentAttribute)pair.get(0);
							EquipmentAttribute after = (EquipmentAttribute)pair.get(1);
							List subAttribute = ((EquipmentType)updates.get("equipmentType")).getSubAttributes(before.getName());
							List subPair; 
							for(int j=0;subAttribute != null && j<subAttribute.size();j++)
								{
								EquipmentAttribute oldSub = (EquipmentAttribute)subAttribute.get(j);
								EquipmentAttribute newSub = new EquipmentAttribute(oldSub);
								String subName =  oldSub.getName();
								subName = subName.replace(before.getName(),after.getName());
								newSub.setName(subName);
								subPair = new Vector();
								subPair.add(oldSub);
								subPair.add(newSub);
								edit.put(oldSub.primaryKey, subPair);
								rows.add(newSub);
								}
							
							}
						else if(tcAlias.containsKey(attribute.getName()))
							{
							List alias = (List)tcAlias.get(attribute.getName());
							rows.addAll(getSubAttributesWithAlias(alias));
							}
						}
					}
				attribute.typeKey= ((EquipmentTypeSetup)parent).getEquipmentType().primaryKey;
				}
			Util.request(records,"store.do");
			deleteRecords();
			records =((EquipmentTypeSetup)parent).reloadEquipmentType().getFilteredAttributes();
			if(updatesNeeded(updates)){Util.request(updates,"updateEquipmentAttributes.do");}
			clearUpdates();
			search();
			Util.createWindow("Success", new Form("Success", "", this, 0), this);
			}
		catch (Throwable e)
			{
			e.printStackTrace();
			throw e;
			}
		finally
			{
			setCursor(Cursor.getDefaultCursor());
			}
		}
	
	public void delete()throws Throwable
		{
		Vector remove = new Vector();
		for (int i = 0; i < getSelectedRows().length; i++)
			{
			int index = getSelectedRows()[i];
			if (index < records.size()) remove.addElement(records.elementAt(index));
			}
		records.removeAll(remove);
		rowsDeleted(0, records.size() - 1);
		List deleted = (List)updates.get("deleted");
		deleted.addAll(remove);
		}
	
	public void edit() 
		{
		try
			{
			EquipmentAttribute edited = (EquipmentAttribute)records.elementAt(table.getSelectedRows()[0]);
			EquipmentAttribute current =  new EquipmentAttribute(edited);
			Map edits = (Map)updates.get("edited");
			if(! edits.containsKey(edited.primaryKey))
				{
				List updatePair = new Vector(2);
				updatePair.add(current);
				edits.put(current.primaryKey, updatePair);
				}
			String buttons = null;
			if(EquipmentAttribute.THRESHOLDCOUNTER.equalsIgnoreCase(edited.getType()))
				{
				buttons =Util.getProperty("EquipmentAttribute.buttons");
				String aliasButtons="Add Alias,"+buttons;
				Util.setProperty("EquipmentAttribute.buttons", aliasButtons);
				}
			Form fAttribute = new Form("EquipmentAttribute", edited, this, 0); 
			Util.createWindow(type,fAttribute, this);
			if(buttons !=null){Util.setProperty("EquipmentAttribute.buttons", buttons);}
			}
		catch (Throwable e)
			{
			e.printStackTrace();
			}
		}
	
	public void edit(Table table)
		{
		try	{table.edit();}
		catch (Throwable e)	{e.printStackTrace();}
		}
	
	public void save(Form form) throws Throwable
		{
		EquipmentAttribute attribute = (EquipmentAttribute)form.getObject();
		form.cancel();
		boolean isNewAttribute = attribute.primaryKey == null; 
		if(isNewAttribute)
			{
			records.add(attribute);
			List added = (List)updates.get("added");
			added.add(attribute);
			}
		else
			{
			Map edited = (Map)updates.get("edited");
			List edits = (List)edited.get(attribute.primaryKey);
			edits.add(1,attribute);
			}
		if (sort && records != null){Collections.sort(records, new Util.Compare());}
		model.fireTableRowsInserted(0, records.size() - 1);
		resize();
		}
	
	public void fieldChanged(Form form, String field, Object object) throws Throwable
		{
		if(EquipmentAttribute.THRESHOLDCOUNTER.equals(object))
			{
			EquipmentAttribute attribute = (EquipmentAttribute)form.getObject();
			ThresholdCounter counter = new ThresholdCounter(attribute.name);
			Form config = new Form("CounterType", counter, this);
			Util.createWindow("ThresholdCounter Configuration", config, this);
			counter  = (ThresholdCounter)config.getObject();
			Map configOptions = new Hashtable();
			String tmp =counter.getEIVariable();
			configOptions.put("eIVariable",tmp!=null?tmp:"");
			configOptions.put("units", counter.getUnits());
			attribute.setConfigOptions(configOptions);
			}
		}

	public void search() throws Throwable
		{
		records = ((EquipmentTypeSetup) parent).reloadEquipmentType().getFilteredAttributes();
		if (sort && records != null) Collections.sort(records, new Util.Compare());
		model.fireTableRowsInserted(0, records.size() - 1);
		resize();
		}

	public void oK(Table table)
		{
		List alias = table.getRecords();
		if(alias.size() > 0){tcAlias.put(table.getName(), alias);}
		table.cancel();
		}
	
	
	public void cancel(Table table)
		{
		clearUpdates();
		table.cancel();
		}
	
	public void cancel(Form form)
		{
		try
			{
			EquipmentAttribute attribute = (EquipmentAttribute) form.getObject();
			boolean edited = attribute.primaryKey != null;
			Map edits = (Map)updates.get("edited");
			if(edited && edits.containsKey(attribute.primaryKey))
				{
				List tmp = (List)edits.get(attribute.primaryKey);
				if(tmp !=null && tmp.size() == 1)
					{
					edits.remove(attribute.primaryKey);
					}
				}
			}
		catch (Throwable t)
			{}
		form.cancel();
		}
	

	public void addAlias(Form form)throws Throwable 
		{
		EquipmentAttribute attribute = (EquipmentAttribute)form.getObject();
		Vector subAttribute = ((EquipmentType)updates.get("equipmentType")).getSubAttributes(attribute.getName());
		Table alias =  new Table("ComplexAttributeAlias",subAttribute, null,this, true);
		alias.setName(attribute.getName());
		Util.createWindow("Alias", alias, this);
		}

	private void deleteRecords()throws Throwable
	{
	List deleted = (List)updates.get("deleted");
	for(int i = 0; i< deleted.size();i++)
		{
		EquipmentAttribute toDelete = (EquipmentAttribute)deleted.get(i);
		if(EquipmentAttribute.THRESHOLDCOUNTER.equals( toDelete.getType()))
			{
			List attributes = ((EquipmentType)updates.get("equipmentType")).getSubAttributes(toDelete.getName());
			deleted.addAll(attributes);
			}
		Util.request(toDelete, "delete.do");
		}
	}

	private void clearUpdates()
		{
		List tmp = (List)updates.get("added");
		tmp.clear();
		tmp = (List)updates.get("deleted");
		tmp.clear();
		Map tmpEd = (Map)updates.get("edited");
		tmpEd.clear();
		}
	
	private boolean updatesNeeded(Map attsUpdates)
		{
		boolean update = false;
		EquipmentType eqType = (EquipmentType)attsUpdates.get("equipmentType");
        update = (eqType != null &&  !eqType.name.equals("Tester") ) &&
                 (
                 ((List)attsUpdates.get("added")).size() > 0   || 
	             ((List)attsUpdates.get("deleted")).size() > 0 ||
	             ((Map)attsUpdates.get("edited")).size() > 0
	             );
	             
		return update;
		}
	
	
	private List getSubAttributesWithAlias(List alias)
		{
		List sub = new Vector();
		List fields = ((EquipmentTypeSetup)parent).getEquipmentType().getAttributes();
		for(int j=0;alias!=null && j< alias.size();j++)
			{
			EquipmentAttribute fieldAlias = (EquipmentAttribute)alias.get(j);
			for(int k =0;fields !=null && k<fields.size();k++)
				{
				EquipmentAttribute field = (EquipmentAttribute)fields.get(k);
				if(field.getName().equals(fieldAlias.getName()))
					{
					field.setAttributeAlias(fieldAlias.getAttributeAlias());
					sub.add(field);
					}
				}
			}
		return sub;
		}

	}

