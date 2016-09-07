package com.freescale.stateStamper.model.entity;

public class Action {
	private Integer primarKey;

	public Integer getPrimarKey() {
		return primarKey;
	}

	public void setPrimarKey(Integer primarKey) {
		this.primarKey = primarKey;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public String getParameters() {
		return parameters;
	}

	public void setParameters(String parameters) {
		this.parameters = parameters;
	}

	public String getPriority() {
		return priority;
	}

	public void setPriority(String priority) {
		this.priority = priority;
	}

	private String action, parameters, priority;

}
