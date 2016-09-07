package com.freescale.stateStamper.model.entity;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class Location implements Serializable {

	private static final long serialVersionUID = 1L;
	private Integer locationPk;
	private String name;
	private Map<String, List<String>> components;
	private Integer capacity;
	private Integer count;
	public static final String ANY_CLASS = "*";
	public static final Integer LIMITLESS = -1;
	public static final String DEFAULT_LOCATION = "Unknown";

	public Integer getLocationPk() {
		return locationPk;
	}

	public void setLocationPk(Integer locationPk) {
		this.locationPk = locationPk;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Map<String, List<String>> getComponents() {
		return components;
	}

	public synchronized void setComponents(Map<String, List<String>> components) {
		this.components = components;
	}

	public Integer getCapacity() {
		return capacity;
	}

	public void setCapacity(Integer capacity) {
		if (capacity > LIMITLESS )
		{	
			this.capacity = capacity;
		}
		else
		{
			this.capacity = LIMITLESS;
		}
	}

	public Integer getCount() {
		return count;
	}

	public void setCount(Integer count) {
		this.count = count;
	}

	/**
	 * check if the Location supports the given durable
	 */

	public boolean isSupported(Machine durable) {
		EquipmentType equipmentType = durable.getEquipmentType();
		String equipmentTypeStr = equipmentType.name;
		List<String> supportList = components.get(equipmentTypeStr);
		if (supportList != null && !supportList.isEmpty()) {
			String firstItem = supportList.get(0);
			if (firstItem.equals(ANY_CLASS)
					|| supportList.contains(durable.type)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * check if the Location has any empty slot
	 */
	public boolean hasRoom() {
		if (capacity >= 0) {
			return count < capacity;
		} else if (capacity.equals(LIMITLESS)) {
			return true;
		} else {
			return true;
		}
	}

	public synchronized boolean addItem() {
		if ((capacity > 0 && count < capacity) || capacity.equals(LIMITLESS)) {
			count++;
			return true;
		} else {
			return false;
		}
	}

	public synchronized boolean removeItem() {
		if (count > 0|| capacity.equals(LIMITLESS)) {
			count--;
			return true;
		} else {
			return false;
		}
	}


}
