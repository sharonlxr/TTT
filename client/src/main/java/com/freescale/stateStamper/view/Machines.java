package com.freescale.stateStamper.view;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.FilteredImageSource;
import java.awt.image.RGBImageFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

import com.freescale.clientUtil.Form;
import com.freescale.clientUtil.Util;
import com.freescale.stateStamper.model.entity.BusinessRule;
import com.freescale.stateStamper.model.entity.BusinessRuleAction;
import com.freescale.stateStamper.model.entity.CurrentDurableStatus;
import com.freescale.stateStamper.model.entity.DurableStatus;
import com.freescale.stateStamper.model.entity.Machine;
import com.freescale.stateStamper.model.entity.TransitionAction;

/**
 * This class displays a tree of machine groups and machines that can be
 * selected
 */

public class Machines extends JPanel implements ActionListener, Runnable
	{
	private static final int ICON_WIDTH = 78;
	private static final String[] ICONS = new String[] { "OpenFolder", "ClosedFolder", "Machine" };
	private int width, height;
	private Map machines = new HashMap(),selectedMachines= new HashMap();
	private Map selectedGroups = new HashMap(), icons = new Hashtable();
	private JLabel display = new JLabel();
	private Vector displayMachines, blink;
	private Hashtable groups;
	private List<String> refreshStatusMachines = new ArrayList<String>();
	private int dashboard;
	private boolean run;

	/**
	 * Creates a tree of machine groups and machines that can be selected
	 * 
	 * @param groups
	 *            A hash table where every entry is a group and a vector of
	 *            machines
	 * @throws Throwable
	 */
	public Machines(Hashtable groups, int dashboard) throws Throwable
		{
		this.groups = groups;
		this.dashboard = dashboard;
		setLayout(null);
		for (int i = 0; i < ICONS.length; i++)
			{
			Icon icon = Util.getIcon(ICONS[i], 24);
			icons.put(ICONS[i], icon);
			icons.put(ICONS[i] + "Selected", new ImageIcon(createImage(new FilteredImageSource(((ImageIcon) icon)
					.getImage().getSource(), new Filter()))));
			}
		Vector keys = new Vector(groups.keySet());
		Collections.sort(keys);
		for (int i = 0; i < keys.size(); i++)
			{
			String key = (String) keys.elementAt(i);
			JLabel expand = new JLabel((Icon) UIManager.get("Tree.collapsedIcon"));
			expand.addMouseListener(new MouseAdapter()
				{
					public void mousePressed(MouseEvent event)
						{
						expand((JLabel) event.getSource());
						}
				});
			add(expand);
			JLabel group = new JLabel(key, Util.getIcon("ClosedFolder", 24), SwingConstants.LEFT);
			group.addMouseListener(new MouseAdapter()
				{
					public void mousePressed(MouseEvent event)
						{
						processGroup(event);
						}
				});
			add(group);
			JPanel child = new JPanel();
			child.setLayout(null);
			child.setOpaque(false);
			child.setVisible(false);

			add(child);
			}
		run = true;
		}

	private void displayGroup(Object key, JPanel jpanel)
		{
		Vector items = (Vector) groups.get(key);
		Collections.sort(items);
		for (int j = 0; j < items.size(); j++)
			{
			Machine aux = new Machine(null, (String) items.elementAt(j), null, null);
			JLabel machine = new JLabel(FinalTest.createText(aux, dashboard, true), Util.getIcon("Machine", 24),
					SwingConstants.CENTER);
			machine.setVerticalTextPosition(SwingConstants.BOTTOM);
			machine.setHorizontalTextPosition(SwingConstants.CENTER);
			machine.addMouseListener(new MouseAdapter()
				{
					public void mousePressed(MouseEvent event)
						{
						processMachine(event);
						}
				});
			jpanel.add(machine);
			}
		}
	
	private void updateRefreshStatusList(Object key,JPanel panel)
		{
		List<String> items = (List<String>) groups.get(key);
		Collections.sort(items);
		if (panel.isVisible())
			{
			refreshStatusMachines.addAll(items);
			}
		else
			{
			refreshStatusMachines.removeAll(items);
			}
		}

	/**
	 * Return a decapitalized version of the input string. For instance
	 * DEVICE_ID becomes Device Id
	 * 
	 * @param name
	 *            The input string
	 * @return The input string decapitalized
	 */
	public static String deCapitalize(String name)
		{
		StringBuffer buffer = new StringBuffer();
		if (name.equals("TIME")) {
			name = name + "(day:hour:min)";
		} else {
			name = name.replaceAll("_", " ");
		}
		
		boolean upper = true;
		for (int i = 0; i < name.length(); i++)
			{
			if (upper)
				{
				buffer.append(name.charAt(i));
				upper = false;
				}
			else buffer.append(Character.toLowerCase(name.charAt(i)));
			if (name.charAt(i) == ' ') upper = true;
			}
		return buffer.toString();
		}

	/**
	 * Process an action event
	 * 
	 * @param event
	 *            The action event
	 */
	public void actionPerformed(ActionEvent event)
		{
		new Util.WorkerThread(this, null, event);
		}

	public void stop()
		{
		run = false;
		}

	/**
	 * Make icons blink if included in blink vector
	 */
	public void run()
		{
		while (run)
			{
			try
				{
				for (int i = 0; i < blink.size(); i++)
					{
					Object[] tmp = (Object[]) blink.elementAt(i);
					JLabel label = (JLabel) tmp[0];
					label.setOpaque(false);
					label.repaint();
					}
				Thread.sleep(1000);
				for (int i = 0; i < blink.size(); i++)
					{
					Object[] tmp = (Object[]) blink.elementAt(i);
					JLabel label = (JLabel) tmp[0];
					label.setOpaque(true);
					label.setBackground((Color) tmp[1]);
					label.repaint();
					}
				Thread.sleep(1000);
				}
			catch (Throwable t)
				{};
			}
		}

	/**
	 * Sets the size for each component based of the general width and height
	 * 
	 * @param width
	 *            The width of the panel
	 * @param height
	 *            The height of the panel
	 */
	public void setSize(int width, int height)
		{
		int y = 0;
		Component[] panels = getComponents();
		for (int i = 0; i < panels.length; i += 3)
			{
			Dimension size = panels[i].getPreferredSize();
			panels[i].setBounds(6, y + 8, size.width, size.height);
			size = panels[i + 1].getPreferredSize();
			panels[i + 1].setBounds(16, y, size.width, size.height);
			y += size.height + 4;
			if (panels[i + 2].isVisible())
				{
				int x = 16, aux = 0;
				Component[] component = ((JPanel) panels[i + 2]).getComponents();
				for (int j = 0; j < component.length; j++)
					{
					JLabel machine = (JLabel) component[j];
					size = machine.getPreferredSize();
					if (x + ICON_WIDTH + 4 > width)
						{
						x = 16;
						aux += size.height + 4;
						}
					machine.setBounds(x, aux, ICON_WIDTH, size.height);
					x += ICON_WIDTH + 4;
					}
				panels[i + 2].setBounds(0, y, width, aux + size.height);
				y += aux + size.height;
				}
			}
		this.width = width;
		this.height = y;
		super.setSize(width, y);
		}

	/**
	 * return the preferred size for this panel
	 */
	public Dimension getPreferredSize()
		{
		return new Dimension(width, height);
		}

	/**
	 * Returns a hash table with the selected machines
	 * 
	 * @return A hash table. Format is machine1=type1,machine2=type2,...
	 */
	public Map getSelected()
		{
		return selectedMachines;
		}

	/**
	 * Set the state of the machines
	 * 
	 * @param names
	 *            A hash table machine=state
	 */
	public void setState(Map machines, int dashboard)
		{
		this.machines = machines;
		updateLabels(dashboard, false);
		displayVariables();
		}

	public void updateLabels(int dashboard, boolean resize)
		{
		this.dashboard = dashboard;
		blink = new Vector();
		Component[] components = getComponents();
		for (int i = 1; i < components.length; i += 3)
			{
			if (((JPanel) components[i + 1]).isVisible())
				{
				Component[] component = ((JPanel) components[i + 1]).getComponents();
				for (int j = 0; j < component.length; j++)
					{
					JLabel label = (JLabel) component[j];
					Machine machine = (Machine) machines.get(getName(label.getText()));
					if (machine != null && machine.state != null)
						{
						label.setOpaque(true);
						Color color = FinalTest.getColor(machine, dashboard);
						label.setBackground(color);
						label.setText(FinalTest.createText(machine, dashboard, true));
						label.repaint();
						Object[] tmp = { label, color };
						if (machine.getEquipmentType() == null || machine.getEquipmentType().name.equals("Tester"))
							{
							if (FinalTest.limitExceeded(machine, dashboard)) blink.addElement(tmp);
							}
						else
							{
							Vector rules = machine.getEquipmentType().getBusinessRulesBy(BusinessRule.MONITOR);
							Object o = machine.context.get("durableStatus");
							if (rules != null && rules.size() > 0 && o != null)
								{
								Color colorDurable = OverLimitToBlink(new CurrentDurableStatus((DurableStatus) o,
										machine), rules);
								if (colorDurable != null)
									{
									Object[] tmpDurable = { label, colorDurable };
									blink.addElement(tmpDurable);
									}
								}
							}
						}
					}
				if (resize) setSize(getWidth(), getHeight());
				}
			}
		}

	/**
	 * 
	 * @param machine
	 * @param businessRules
	 * @return check to see if the durable has over limit attribute according to
	 *         the business rules
	 */

	private Color OverLimitToBlink(CurrentDurableStatus currentStatus, Vector businessRules)
		{
		try
			{
			for (int i = 0; i < businessRules.size(); i++)
				{
				BusinessRule businessRule = (BusinessRule) businessRules.get(i);
				Vector actions = businessRule.getActions();
				for (int j = 0; j < actions.size(); j++)
					{
					BusinessRuleAction action = (BusinessRuleAction) actions.get(j);
					if (action.getAction().equals(BusinessRuleAction.ALARM_WINDOW))
						{
						if (businessRule.conditionsTrue(currentStatus))
							{
							Map<String, String> actionParameters = action.getActionParameters();
							String color = actionParameters.get(TransitionAction.PARAM_COLOR);
							Object tmp = currentStatus.get(color);
							if (tmp == null)
								{
								tmp = color;
								}
							return new Color(Integer.parseInt(tmp.toString(), 16));
							}
						}
					}
				}
			return null;
			}
		catch (Throwable throwable)
			{
			throwable.printStackTrace();
			return null;
			}
		}

	/**
	 * Dismiss the form
	 * 
	 * @param form
	 *            The form
	 */
	public void dismiss(Form form)
		{
		form.cancel();
		}

	/**
	 * Expands a given group
	 * 
	 * @param expand
	 *            The group to expand
	 */
	private void expand(JLabel expand)
		{
		Component[] components = getComponents();
		for (int i = 0; i < components.length; i++)
			{
			if (components[i].equals(expand))
				{
				JLabel folder = (JLabel) components[i + 1];
				Component component = components[i + 2];
				JPanel panel = (JPanel) component;
				Object key = folder.getText();
				if (panel.isVisible())
					{
					panel.removeAll();
					folder.setIcon((Icon) icons.get("ClosedFolder"));
					expand.setIcon((Icon) UIManager.get("Tree.collapsedIcon"));
					panel.setLayout(null);
					panel.setOpaque(false);
					panel.setVisible(false);
					}
				else
					{
					displayGroup(key, panel);
					folder.setIcon((Icon) icons.get("OpenFolder"));
					expand.setIcon((Icon) UIManager.get("Tree.expandedIcon"));
					panel.setVisible(true);
					}
				updateRefreshStatusList(key,panel);
				break;
				}
			}
		setSize(this.getWidth(), this.getHeight());
		repaint();
		}
	/**
	 * update status for the visible machines 
	 * @param names list of mesNames
	 */
	public void updateMachineStatus()
		{
		try
			{
			List<Machine> updatedMachines = (List<Machine>) Util.request(refreshStatusMachines, "updateMachineStatus.do");
			for(Machine updatedMachine:updatedMachines)
				{
				Machine machine = (Machine) machines.get(updatedMachine.mESName);
				machine.context.put("SUBSTATE", updatedMachine.context.get("SUBSTATE"));
				machine.context.put("DEVICE_NAME", updatedMachine.context.get("DEVICE_NAME"));
				machine.context.put("TEMPERATURE", updatedMachine.context.get("TEMPERATURE"));
				machine.context.put("LOADBOARD_ID", updatedMachine.context.get("LOADBOARD_ID"));
				machine.context.put("TEST_PROGRAM", updatedMachine.context.get("TEST_PROGRAM"));
				machine.context.put("FLOW_ID", updatedMachine.context.get("FLOW_ID"));
				machine.context.put("LAST_STATE_TIME", updatedMachine.context.get("LAST_STATE_TIME"));
				machine.state = updatedMachine.state;
				machine.performance = new HashMap();
				machine.performance.put("LAST_HR_OEE", updatedMachine.performance.get("LAST_HR_OEE"));
				machine.performance.put("LOT_PROGRESS", updatedMachine.performance.get("LOT_PROGRESS"));
				machine.performance.put("CURRENT_SITE_PERF", updatedMachine.performance.get("CURRENT_SITE_PERF"));
				machine.performance.put("24_HR_SITE_PERF", updatedMachine.performance.get("24_HR_SITE_PERF"));
				machine.performance.put("LAST_HR_YIELD", updatedMachine.performance.get("LAST_HR_YIELD"));
				machine.performance.put("24_HR_YIELD", updatedMachine.performance.get("24_HR_YIELD"));
				}
			updateLabels(dashboard, false);
			} 
		catch (Throwable e)
			{
			e.printStackTrace();
			}
		}
	/**
	 * update all status/attributes of a set of machines
	 * @param names list of mesNames
	 */
	public void updateMachines(List<String> names)
		{
		try
			{
			List<Machine> updatedMachines = (List<Machine>) Util.request(names, "getMachinesByMesNames.do");
			for(Machine updatedMachine:updatedMachines)
				{
				machines.put(updatedMachine.mESName, updatedMachine);			
				}
			updateLabels(dashboard, false);
			} 
		catch (Throwable e)
			{
			e.printStackTrace();
			}
		}
	
	/**
	 * Process mouse click: Either displays menu options for group (right click)
	 * or toggles entire group from selected to deselected or vice versa (left
	 * click)
	 * 
	 * @param event
	 *            The mouse event for the group
	 */
	private void processGroup(MouseEvent event)
		{
		JLabel label = (JLabel) event.getSource();
		boolean selected = selectedGroups.get(label.getText()) == null;
		displayMachines = new Vector();
		Component[] components = getComponents();
		for (int i = 0; i < components.length; i++)
			{
			if (components[i].equals(label))
				{
				Component panel = components[i + 1];
				if (selected) selectedGroups.put(label.getText(), "");
				else selectedGroups.remove(label.getText());
				if (!panel.isVisible()) displayGroup(((JLabel) components[i]).getText(), (JPanel) panel);
				String icon = (panel.isVisible() ? "OpenFolder" : "ClosedFolder") + (selected ? "Selected" : "");
				label.setIcon((Icon) icons.get(icon));
				label.repaint();
				Component[] machinecomponents = ((JPanel) components[i + 1]).getComponents();
				for (int j = 0; j < machinecomponents.length; j++)
					{
					JLabel component = (JLabel) machinecomponents[j];
					displayMachines.addElement(getName(component.getText()));
					toggle(component, selected);
					}
				if (!panel.isVisible()) ((JPanel) components[i + 1]).removeAll();
				break;
				}
			}
		if (event.getButton() == MouseEvent.BUTTON1 && event.getClickCount() == 2)
			{
			try {
				String groupName = label.getText();
				List<String> names = (List<String>)groups.get(groupName);
				updateMachines(names);
				displayVariables();
			} catch (Throwable e) {
				e.printStackTrace();
			}
			
			Util.createWindow(label.getText(), new JScrollPane(display), this);
			}
		}

	/**
	 * Process mouse click: Either displays menu options for machine (right
	 * click) or toggles machine from selected to deselected or vice versa (left
	 * click)
	 * 
	 * @param event
	 *            The mouse event for the machine
	 */
	private void processMachine(MouseEvent event)
		{
		JLabel label = (JLabel) event.getSource();
		String mid = getName(label.getText());
		toggle(label, selectedMachines.get(mid) == null);
		if (event.getButton() == MouseEvent.BUTTON1 && event.getClickCount() == 2)
			{
			Machine mac = null;
			try {
				mac = (Machine) Util.request(mid, "getMachine.do");
				machines.put(mid, mac);
			} catch (Throwable e) {
				e.printStackTrace();
			}
			JPanel panel = null;
			if ("Tester".equals(mac.getEquipmentType().name))
				{
				panel = FinalTest.createReport(mac);
				}
			else
				{
				panel = FinalTest.createDurableReport(mac);
				}
			Util.createWindow(mid, panel, this);
			}
		}

	/**
	 * Selects or deselects an icon
	 * 
	 * @param label
	 *            The icon to select or deselect
	 * @param selected
	 *            Whether to select or deselect
	 */
	private void toggle(JLabel label, boolean selected)
		{
		String name = getName(label.getText());
		Machine mac = null;
		mac = (Machine) machines.get(name);
		if (selected)
			{
			selectedMachines.put(name, mac);
			}
		else selectedMachines.remove(getName(label.getText()));
		label.setIcon((Icon) icons.get("Machine" + (selected ? "Selected" : "")));
		label.repaint();
		}

	/**
	 * Updates label with all context and summing variables for selected group
	 * or machines
	 */
	private void displayVariables()
		{
		try
			{
			if (displayMachines.size() == 0) return;
			Vector contexts = new Vector(), summings = new Vector();
			for (int i = 0; i < displayMachines.size(); i++)
				{
				Machine machine = (Machine) machines.get(displayMachines.elementAt(i));
				Set<String> keys = machine.context.keySet();
				for (String key:keys)
					{
					if (!contexts.contains(key)) contexts.addElement(key);
					}
				keys = machine.summing.keySet();
				for (String key:keys)
					{
					if (!summings.contains(key)) summings.addElement(key);
					}
				}
			Collections.sort(contexts);
			Collections.sort(summings);
			StringBuffer content = new StringBuffer("<html><table cellpadding=1 cellspacing=1>");
			content.append("<td>Machine</td><td>State</td>");
			for (int j = 0; j < contexts.size(); j++)
				content.append("<td>" + deCapitalize((String) contexts.elementAt(j)) + "</td>");
			for (int j = 0; j < summings.size(); j++)
				content.append("<td>" + deCapitalize((String) summings.elementAt(j)) + "</td>");
			for (int i = 0; i < displayMachines.size(); i++)
				append(content, (Machine) machines.get(displayMachines.elementAt(i)), contexts, summings);
			display.setText(content.toString());
			display.setBorder(new EmptyBorder(4, 4, 12, 4));
			}
		catch (Throwable t)
			{}
		}

	/**
	 * Append a row with data for the given machine
	 * 
	 * @param content
	 *            The string buffer to append data to
	 * @param machine
	 *            The machine
	 * @param contexts
	 *            A vector with context variable names
	 * @param summings
	 *            A vector with summing variable names
	 */
	private void append(StringBuffer content, Machine machine, Vector contexts, Vector summings)
		{
		content.append("<tr>");
		content.append("<td>" + machine.mESName + "</td>");
		String color = " bgcolor='"
				+ Integer.toHexString(StateStamperFrame.getColor(machine.state).getRGB() & 0xFFFFFF) + "'";
		content.append("<td" + color + ">" + machine.state + "</td>");
		for (int j = 0; j < contexts.size(); j++)
			{
			String value = (String) machine.context.get(contexts.elementAt(j));
			if (value == null) value = "";
			content.append("<td>" + value + "</td>");
			}
		for (int j = 0; j < summings.size(); j++)
			{
			Integer value = (Integer) machine.summing.get(summings.elementAt(j));
			if (value == null) value = new Integer(0);
			content.append("<td>" + value + "</td>");
			}
		content.append("</tr>");
		}

	/**
	 * Extract the machine ID from the label text
	 * 
	 * @param text
	 *            The label text
	 * @return The machine ID
	 */
	private String getName(String text)
		{
		int index1 = text.indexOf("<html>"), index2 = text.indexOf("<br>");
		return text.substring(index1 + 6, index2);
		}

	/**
	 * This class turns icons blue to show that they are selected
	 */
	private class Filter extends RGBImageFilter
		{
		public Filter()
			{
			canFilterIndexColorModel = true;
			}

		public int filterRGB(int x, int y, int rgb)
			{
			int color = ((rgb >> 1) & 0x007F7F7F) + 0x382800;
			return ((rgb & 0xFF000000) | color);
			}
		}
	}