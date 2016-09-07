package com.freescale.stateStamper.view;

import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.freescale.clientUtil.Form;
import com.freescale.clientUtil.Util;
import com.freescale.stateStamper.model.entity.EquipmentType;
import com.freescale.stateStamper.model.entity.Location;

public class LocationForm extends Form
	{
	private static final long serialVersionUID = 1L;
	private String fieldsObj;
	private String lenObj;
	private List<EquipmentType> equipmentTypes;

	public LocationForm(String type, Location object, Object parent) throws Throwable
		{
		init(type, object, parent);
		}

	private void init(String type, Location object, Object parent) throws Throwable
		{
		this.equipmentTypes = (List<EquipmentType>) Util.request(new EquipmentType(), "search.do", false);
		fieldsObj = Util.getProperty(type + ".properties");
		lenObj = Util.getProperty(type + ".length");
		StringBuffer fieldStr = new StringBuffer(fieldsObj);
		StringBuffer lenStr = new StringBuffer(lenObj);
		for (int i = 0; equipmentTypes != null && i < equipmentTypes.size(); i++)
			{
			fieldStr.append("," + equipmentTypes.get(i).name);
			lenStr.append(",32");
			}
		Util.setProperty(type + ".properties", fieldStr.toString());
		Util.setProperty(type + ".length", lenStr.toString());
		super.init(type, object, parent, 0);
		JScrollPane scrollPane = new JScrollPane(getParentBody());
		scrollPane.setOpaque(false);
		scrollPane.getViewport().setOpaque(false);
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		add(scrollPane, BorderLayout.CENTER);
		scrollPane.addMouseListener(new MouseAdapter()
			{
				public void mousePressed(MouseEvent event)
					{
					processEvent(event);
					}
			});
		}

	private JPanel getParentBody()
		{
		JPanel body = null;
		try
			{
			Class form = getClass().getSuperclass();
			Field field = form.getDeclaredField("body");
			field.setAccessible(true);
			body = (JPanel) field.get(this);
			}
		catch (Throwable t)
			{
			t.printStackTrace();
			}
		return body;
		}

	public Object[] getValueType(String fieldName)
		{
		Object[] result = null;
		try
			{
			String value = "";
			List<String> list = null;
			boolean found = false;
			for (int i = 0; i < equipmentTypes.size() && !found; i++)
				{
				found = equipmentTypes.get(i).name.equals(fieldName);
				}
			if (found)
				{
				Location location = (Location) object;
				if (location.getComponents() != null)
					{
					list = location.getComponents().get(fieldName);
					if (list != null)
						{
						for (String item : list)
							{
							value = value + item + ";";
							}
						value = value.substring(0, value.length() - 1);
						}
					}
				result = new Object[] { value, String.class };
				}
			else
				{
				result = super.getValueType(fieldName);
				}
			}
		catch (Throwable t)
			{
			t.printStackTrace();
			}
		return result;
		}

	public Object getObject() throws Throwable
		{
		// restore properties for object fields only
		Util.setProperty(type + ".properties", fieldsObj);
		Util.setProperty(type + ".length", lenObj);
		super.getObject();
		Map<String, List<String>> locations = new HashMap<String, List<String>>();
		for (EquipmentType eqType : equipmentTypes)
			{
			String name = eqType.name;
			String value = getValue(name);
			if (value != null && !value.equals(""))
				{
				String[] valueArray = value.split(";");
				List<String> valueVector = Arrays.asList(valueArray);
				locations.put(name, valueVector);
				}
			}
		Location location = (Location) object;
		location.setComponents(locations);
		return object;
		}

	public void setObject(Object object) throws Throwable
		{
		super.setObject(object);
		Location location = (Location) object;
		Map<String, List<String>> locations = location.getComponents();
		for (EquipmentType eqType : equipmentTypes)
			{
			String name = eqType.name;
			List<String> values = locations.get(name);
			String value = "";
			for (String val : values)
				{
				value = value + val + ";";
				}
			value = value.substring(0, value.length() - 1);
			setValue(name, value);
			}
		}

}
