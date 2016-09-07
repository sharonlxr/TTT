package com.freescale.stateStamper.view;

import java.awt.BorderLayout;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JLabel;

import org.apache.commons.lang.StringUtils;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.RoundingMode;

import com.freescale.clientUtil.*;
import com.freescale.stateStamper.model.entity.EquipmentType;
import com.freescale.stateStamper.model.entity.Machine;
import com.freescale.stateStamper.model.entity.EquipmentAttribute;
import com.freescale.stateStamper.model.entity.Model;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class EquipmentForm extends Form implements KeyListener{
	private static final long serialVersionUID = 1L;
	private Vector dynamicattr;
	private String fieldsObj;
	private String lenObj;
	private static final String DAY_DENOMINATOR = "86400000";
	private EquipmentType equipmentType;
	private List parentFilterFields;
	
	public EquipmentForm(String type, Machine object, Object parent,Vector dynamicattr) throws Throwable
		{
		equipmentType = object.getEquipmentType();
		init(type, object, parent, 0, dynamicattr);
		}
	
	private void init(String type, Machine object, Object parent, int width,Vector dynamicAttr) throws Throwable
		{
		this.dynamicattr = dynamicAttr;
		fieldsObj=Util.getProperty(type + ".properties");
		lenObj=Util.getProperty(type + ".length");
		StringBuffer fieldStr = new StringBuffer(fieldsObj);
		StringBuffer lenStr   = new StringBuffer(lenObj);
		for(int i=0;dynamicattr!=null && i<dynamicattr.size();i++)
			{
			fieldStr.append(","+((EquipmentAttribute)dynamicattr.get(i)).name);
			lenStr.append(",16");
			}
		Util.setProperty(type+".properties",fieldStr.toString() );
		Util.setProperty(type+".length", lenStr.toString());
		super.init(type,object,parent,width);
		parentFilterFields = (List)Util.request(equipmentType,"getParentListFilterFields.do");
		setUpParentFilter(parentFilterFields);
		markRequiredFields();
		JScrollPane scrollPane = new JScrollPane(getParentBody());
		scrollPane.setOpaque(false);
		scrollPane.getViewport().setOpaque(false);
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		add(scrollPane, BorderLayout.CENTER);
		JComponent component = getFieldObjByName("type");
		component.addKeyListener(this);
		if("MES".equals(equipmentType.getModelSource()))
			{
			JComboBox componentPlatform = (JComboBox)getFieldObjByName("platform");
			componentPlatform.addItemListener(new PlatformListener(this,object));
			if("MES".equals(equipmentType.getChildSource()))
				{
				JComboBox componentModel = (JComboBox)getFieldObjByName("modelName");
				componentModel.addItemListener(new ModelListener(this,object));
				}
			}
		if("TTT".equals(equipmentType.getModelSource()))
			{
			JComboBox componentPlatform = (JComboBox)getFieldObjByName("platform");
			componentPlatform.addItemListener(new PlatformListener(this,object));
			JComboBox componentModelDescription = (JComboBox)getFieldObjByName("modelDescription");
			componentModelDescription.addItemListener(new ModelListener(this,object));
			}
		//only durable need to sync with genesis, genesis can get realtime tester data from sc2
		if(!"Tester".equals(equipmentType.name))
			{
			if(object.primaryKey!=null && equipmentType.isMesEquipmentSyncEnabled())
				{
				this.setEnabled("eIName",false);
				}
			else
				{
				this.setEnabled("eIName",true);
				}
			}
		if("TTT".equalsIgnoreCase(equipmentType.getModelSource()))
			{
			this.setEnabled("modelName",false);
			}
		}
	
	private void setUpParentFilter(List filterFields)throws Throwable
		{
		ParentEventListener parentListener = new ParentEventListener();
		String field;
		for(int i=0;i< filterFields.size();i++)
			{
			field = (String)filterFields.get(i);
			JComponent component = getFieldObjByName(field);
			if(component instanceof JTextField)
				{
				((JTextField)component).addActionListener(parentListener);
				((JTextField)component).addFocusListener(parentListener);
				}
			
			}
		}
	
	/**
	 * look for the fields that are required and add a * to the text of the Label
	 * */
	private void markRequiredFields()
		{
		List requiredFields = getRequiredFields();
		JPanel body = getParentBody();
		Component[] components = body.getComponents();
		for(int i = 0 ; i < components.length ; i++)
			{
			JLabel label;
			if( components[i] instanceof JLabel )
				{
				label = (JLabel)components[i];
				if( requiredFields.contains(label.getText()))
					{
					label.setText(label.getText().replace(":","*:"));
					}
				}
			}
		}
	
	private List getRequiredFields()
		{
		List requiredFields = new ArrayList();
		String[] fields = Util.getProperty(type + ".properties").split(",");
		String[] required = Util.getProperty(type + ".required").split(",");
		for(int i = 0;i<fields.length;i++)
			{
			if( i < required.length && "Y".equalsIgnoreCase(required[i]) )
				{
				requiredFields.add(Util.getString(fields[i]) + ":");
				}
			}
		return requiredFields;
		}
	
	private JComponent getFieldObjByName(String fieldName)throws Throwable {
		Class form = getClass().getSuperclass();
		Field field = form.getDeclaredField("hashtable");
		field.setAccessible(true);
		Hashtable hashtable = (Hashtable)field.get(this);
		return (JComponent)(hashtable.get(fieldName));
	}
	
	private void setFieldObjByName(String fieldName, Component component )throws Throwable {
		Class form = getClass().getSuperclass();
		Field field = form.getDeclaredField("hashtable");
		field.setAccessible(true);
		Hashtable hashtable = (Hashtable)field.get(this);
		hashtable.put(fieldName,component);
	}
	
	private JPanel getParentBody()
		{
		JPanel body=null;
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
		try{
		Object value = null;
		Class propertyType = null;
		boolean found = false;
		EquipmentAttribute attr = null;
		for (int i = 0; i < dynamicattr.size() && !found; i++)
			{
			attr = (EquipmentAttribute) dynamicattr.get(i);
			found = attr.name.equals(fieldName);			
			}
		if (found)
			{
			Machine equip = (Machine) object;
			if (equip.dynamicAttributes != null)
				{
				value = equip.dynamicAttributes.get(fieldName);
				if ((value == null || value.equals(""))&&attr.type.equalsIgnoreCase("Counter")) value = "0";
				if (equipmentType.isTimeRelatedAttribute(fieldName)){
					value = getPercentage(value.toString(),DAY_DENOMINATOR) ;						
				}
				propertyType = String.class;
				result = new Object[] { value, propertyType };
				}
			else
				result = new Object[] {"", String.class };
			}
		else
			{
			result = super.getValueType(fieldName);
			}
		}catch(Throwable t){t.printStackTrace();}
		return result;
		}
	
	public Object getObject() throws Throwable
	    {
	    //restore properties for object fields only
		Util.setProperty(type+".properties",fieldsObj);
		Util.setProperty(type+".length", lenObj);
	    super.getObject();
		Map hashMap = new HashMap();
		for (int i = 0; i < dynamicattr.size(); i++) 
			{
		    EquipmentAttribute attribute = (EquipmentAttribute)dynamicattr.get(i); 
			String value = getValue(attribute.name);		
			if ((value == null || value.equals(""))&&attribute.type.equalsIgnoreCase("Counter")) value = "0";
			if (equipmentType.isTimeRelatedAttribute(attribute.name)){
				value = (new BigDecimal(value)).multiply(new BigDecimal(DAY_DENOMINATOR)).toString();
			}
			
			if (value != null && !value.equals("")){hashMap.put(attribute.name, value);}
			else if ("Y".equalsIgnoreCase( attribute.required)){throw new Throwable("Field " + attribute.name+ " is required");}
			else {hashMap.put(attribute.name, "");}
			}
		Machine equip = (Machine) object;
		equip.dynamicAttributes = hashMap;
		return object;
		}
	
	
	public void setObject(Object object) throws Throwable 
	    {
	    super.setObject(object);
		Machine equip = (Machine) object;
		Map<String, String> hashMap = equip.dynamicAttributes;
		for (int i = 0; i < dynamicattr.size(); i++) 
			{
			String value = hashMap.get(((EquipmentAttribute)dynamicattr.get(i)).name);
			if (value == null){value = "";}				
			setValue(((EquipmentAttribute)dynamicattr.get(i)).name, value);
		    }
		}

	public void keyTyped(KeyEvent e) {
		
	
	}

	 /** Handle the key-pressed event from the text field. */
	public void keyPressed(KeyEvent e) {
		
	}

	/** Handle the key-released event from the text field. */
	public void keyReleased(KeyEvent e) {
		JComponent component = (JComponent) e.getSource();
		// refresh location list
		if (component instanceof JTextField) {
			try {
				String value = ((JTextField) component).getText();
				Machine durable = (Machine) object;
				durable.type = value;
				Class form = getClass().getSuperclass();
				Field field = form.getDeclaredField("parent");
				field.setAccessible(true);
				Equipments equipments = (Equipments) field.get(this);
				equipments.setLocation(durable);
				String values = Util.getProperty("Machines.location.values");
				if (values != null) {
					field = form.getDeclaredField("body");
					field.setAccessible(true);
					JPanel jPanel = (JPanel)field.get(this);
					int postion = getComponentIndex(getFieldObjByName("location"));
					jPanel.remove(postion);
					jPanel.revalidate();
					String[] auxValues = values.split(",");
					for (int j = 0; j < auxValues.length; j++)
						auxValues[j] = auxValues[j].trim();
					JComponent locationComponent = new JComboBox(auxValues);
					setFieldObjByName("location", locationComponent);
					locationComponent.revalidate();													
					jPanel.add(locationComponent, postion);
					jPanel.revalidate();
					jPanel.repaint();				
				}
			} catch (Throwable ex) {
				ex.printStackTrace();
			}
		}
	}
	
	private int getComponentIndex(Component component) {
	    if (component != null && component.getParent() != null) {
	      Container c = component.getParent();
	      for (int i = 0; i < c.getComponentCount(); i++) {
	        if (c.getComponent(i) == component)
	          return i;
	      }
	    }
    	return -1;
	}
	
	private String getPercentage(String numerator, String denominator) {
		DecimalFormat twoDForm = new DecimalFormat("#.##");
		String result = "0";
		try {
			BigDecimal usage = new BigDecimal(numerator);
			BigDecimal limit = new BigDecimal(denominator);
			BigDecimal percentage = BigDecimal.ZERO;
			if (!limit.equals(BigDecimal.ZERO)) {
				percentage = usage.divide(limit,2,RoundingMode.HALF_UP);
			}
			result = twoDForm.format(percentage);
			result = result.replace(".00", "");
		} catch (Exception nfw) {
			nfw.printStackTrace();
		} 
		return result;
	}
	
	class ParentEventListener implements ActionListener,FocusListener 
	{
	public void actionPerformed(ActionEvent e){refreshWorkstation();}
	public void focusLost(FocusEvent e)       {refreshWorkstation();}
	public void focusGained(FocusEvent e){}
		
	public void refreshWorkstation()
		{
		try
			{
			Machine durable = (Machine) object;
			for (int i = 0; i < parentFilterFields.size(); i++)
				{
				JComponent jcomponent = getFieldObjByName((String) parentFilterFields.get(i));
				durable.set((String) parentFilterFields.get(i), ((JTextField) jcomponent).getText());
				}
			Class form = getClass().getEnclosingClass().getSuperclass();
			Field field = form.getDeclaredField("parent");
			field.setAccessible(true);
			Equipments equipments = (Equipments) field.get(EquipmentForm.this);
			equipments.setWorkstation(durable);
			String values = Util.getProperty("Machines.workstation.values");
			if (values != null)
				{
				field = form.getDeclaredField("body");
				field.setAccessible(true);
				JPanel jPanel = (JPanel) field.get(EquipmentForm.this);
				int position = getComponentIndex(getFieldObjByName("workstation"));
				jPanel.remove(position);
				jPanel.revalidate();
				String[] auxValues = values.split(",");
				for (int j = 0; j < auxValues.length; j++)
					auxValues[j] = auxValues[j].trim();
				JComponent workstationComponent = new JComboBox(auxValues);
				setFieldObjByName("workstation", workstationComponent);
				workstationComponent.revalidate();
				jPanel.add(workstationComponent, position);
				jPanel.revalidate();
				jPanel.repaint();
				}
			}
		catch (Throwable ex)
			{
			ex.printStackTrace();
			}
		}
	}
	
	public class ModelListener implements ItemListener
		{
		Form form = null;
		Machine machine = null;
		public ModelListener(Form form,Machine machine)
			{
			this.form = form;
			this.machine = machine;
			}

		public void itemStateChanged(ItemEvent e)
			{
			JComboBox comboBox = (JComboBox) e.getSource();
			try
				{
				String selectedItem = (String)comboBox.getSelectedItem();
				if (e.getStateChange() == ItemEvent.SELECTED)
					{
					if(StringUtils.isNotEmpty(selectedItem))
						{
						if("TTT".equalsIgnoreCase(machine.getEquipmentType().getModelSource()))
							{
							Model model = (Model) Util.request(selectedItem, "getModelByDescription.do", false);
							form.setValue("modelName", model.getModelName());
							if(model!=null&&!equipmentType.name.equals("Tester"))
								{
								form.setValue("engineer_contact", model.getEngineerContact());
								form.setValue("vendor", model.getVendor());
								for(Entry<String,String> entry:model.getModelAttributes().entrySet())
									{
									JComponent component = form.getComponent(entry.getKey());
									if(component!=null)
										{
										form.setValue(entry.getKey(), entry.getValue());
										}
									}
								for(Entry<String,String> entry:model.getDynamicAttributes().entrySet())
									{
									JComponent component = form.getComponent(entry.getKey());
									if(component!=null)
										{
										form.setValue(entry.getKey(), entry.getValue());
										}
									}
								}
							}
						else if("MES".equalsIgnoreCase(machine.getEquipmentType().getChildSource()))
							{
							List<String> childModelList = (List<String>) Util.request(selectedItem, "getChildModel.do", false);
							form.setValue("childModel", StringUtils.join(childModelList, ","));
							}
						form.updateUI();
						}
					}
				}
			catch (Throwable throwable)
				{
				throwable.printStackTrace();
				}
			}

		}

	public class PlatformListener implements ItemListener
		{
		Form form = null;
		Machine machine = null;
		public PlatformListener(Form form,Machine machine)
			{
			this.form = form;
			this.machine = machine;
			}
		public void itemStateChanged(ItemEvent e)
			{
			JComboBox comboBox = (JComboBox) e.getSource();
			try
				{
				if (e.getStateChange() == ItemEvent.SELECTED)
					{
					String platform = (String)comboBox.getSelectedItem();
					if(StringUtils.isNotEmpty(platform))
						{
						List<String> modelList = (List<String>) Util.request(platform, "getMachineModel.do", false);
						Collections.sort(modelList);
						if("TTT".equalsIgnoreCase(machine.getEquipmentType().getModelSource()))
							{
							form.setValues("modelDescription", new Vector(modelList));
							}
						else if("MES".equalsIgnoreCase(machine.getEquipmentType().getModelSource()))
							{
							form.setValues("modelName", new Vector(modelList));
							}
						form.updateUI();
						}
					}
				}
			catch (Throwable throwable)
				{
				throwable.printStackTrace();
				}
			}
		}

}
