package com.freescale.stateStamper.model.entity;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class DurableStatus extends IStatus implements Serializable, Cloneable
	{
	private static final long serialVersionUID = -8437483192978294506L;
	public String DURABLE_ID, TIME, STATE, SUBSTATE, OPERATOR_ID;
	public Integer STATUS_PK, EQUIPMENT_PK, EVENT_PK;
	public Map<String, String> ATTRIBUTES;
	public Map<String, List<String>> COMMENTS;
	public String workstation, location, duration;
	public static final String CURRENT_STATE_DURATION = "Current State Duration";
	protected transient Map fields;
	public String FROMSTATE, TOSTATE; // only use as place holder for historical
										// report

	public DurableStatus clone() throws CloneNotSupportedException
		{
		DurableStatus durablestatus = (DurableStatus) super.clone();
		durablestatus.fields = this.fields;
		durablestatus.ATTRIBUTES = new HashMap<String, String>();
		Set<String> keyset = this.ATTRIBUTES.keySet();
		Iterator<String> it = keyset.iterator();
		while (it.hasNext())
			{
			String key = it.next();
			String value = this.ATTRIBUTES.get(key);
			durablestatus.ATTRIBUTES.put(key, value);
			}
		return durablestatus;
		}

	public DurableStatus()
		{
		this(0);
		}

	public DurableStatus(Integer equipment_pk)
		{
		initialize();
		}

	private void writeObject(ObjectOutputStream oos) throws IOException
		{
		oos.defaultWriteObject();
		}

	private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException
		{
		ois.defaultReadObject();
		initialize();
		}

	private void initialize()
		{
		Field[] f = getClass().getDeclaredFields();
		fields = new HashMap();
		for (int i = 0; i < f.length; i++)
			{
			fields.put(f[i].getName(), f[i]);
			}
		}

	public void set(String name, Object value)
		{
		if (name == null)
			{
			return;
			}
		if (name.indexOf('*') > -1)
			{
			name = name.substring(0, name.length() - 1);
			}
		try
			{
			if (fields.containsKey(name))
				{
				Field field = (Field) fields.get(name);
				if (field.getType() == Integer.class)
					{
					Integer val = null;
					if (value == null)
						{
						val = new Integer(0);
						}
					else
						{
						try
							{
							val = Integer.valueOf(value.toString());
							}
						catch (NumberFormatException ne)
							{
							val = new Integer(0);
							}
						}
					field.set(this, val);
					}
				else if (field.getType() != String.class)
					{
					field.set(this, value);
					}
				else
					{
					field.set(this, value.toString());
					}
				}
			else
				{
				if (ATTRIBUTES == null)
					{
					ATTRIBUTES = new HashMap<String, String>();
					}
				ATTRIBUTES.put(name, String.valueOf(value));
				}
			}
		catch (Throwable e)
			{

			e.printStackTrace();
			}
		}

	public Object get(String name)
		{
		Object value = null;
		if (isCalculatedField(name))
			{
			if (name.equals(CURRENT_STATE_DURATION))
				{
				value = String.valueOf(getCurrentStateDuration());
				}
			}
		else if (fields.containsKey(name))
			{
			value = getFieldValue(name);
			}
		else if (ATTRIBUTES != null && ATTRIBUTES.containsKey(name))
			{
			if (isNeedAddElapsetime(name))
				{
				value = getTimeBaseVaribleValue(name);
				}
			else
				{
				value = ATTRIBUTES.get(name);
				}
			}
		return value;
		}

	public long getCurrentStateDuration()
		{
		long currentDuration = 0;
		DateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		try
			{
			long fromTime = simpleDateFormat.parse(TIME).getTime();
			long toTime = new Date().getTime();
			currentDuration = toTime - fromTime;
			}
		catch (Exception ex)
			{
			ex.printStackTrace();
			}
		return currentDuration;
		}

	public boolean isCalculatedField(String field)
		{
		return CURRENT_STATE_DURATION.equals(field);
		}

	public boolean isNeedAddElapsetime(String name)
		{
		try
			{
			if (name.contains(ThresholdCounter.SEPARATOR))
				{
				String[] names = name.split("\\" + ThresholdCounter.SEPARATOR);
				ThresholdCounter thresholdCounter = new ThresholdCounter(names[0]);
				thresholdCounter.loadFromStatus(this);
				if (thresholdCounter.isCountTimeElapse())
					{
					String attributename = names[1];
					if (attributename.equals("serviceUsage") || attributename.equals("lifeUsage")
							|| attributename.equals("rebuildUsage"))
						{
						return true;
						}
					}
				else
					{
					return false;
					}
				}
			return false;
			}
		catch (Throwable th)
			{
			return false;
			}
		}

	public Object getTimeBaseVaribleValue(String name)
		{
		try
			{
			String[] names = name.split("\\" + ThresholdCounter.SEPARATOR);
			ThresholdCounter thresholdCounter = new ThresholdCounter(names[0]);
			thresholdCounter.loadFromStatus(this);
			if (thresholdCounter.isCountTimeElapse() && !thresholdCounter.isLimitAttributes(name))
				{
				SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				long fromTime = simpleDateFormat.parse(this.TIME).getTime();
				long toTime = new Date().getTime();
				return String.valueOf((Long.parseLong(ATTRIBUTES.get(name).toString()) + toTime - fromTime));
				}
			else
				{
				return ATTRIBUTES.get(name);
				}
			}
		catch (Throwable throwable)
			{
			throwable.printStackTrace();
			return null;
			}
		}

	public boolean has(String name)
		{
		return fields.containsKey(name) || CURRENT_STATE_DURATION.equalsIgnoreCase(name)
				|| (ATTRIBUTES != null && ATTRIBUTES.containsKey(name));
		}

	public Map<String, String> getDynamicAttributes()
		{
		return ATTRIBUTES;
		}

	public void setDynamicAttributes(Map<String, String> dynAttr)
		{
		ATTRIBUTES = dynAttr;
		}

	public void putAll(Map<String, String> all)
		{
		ATTRIBUTES.putAll(all);
		}

	private Object getFieldValue(String name)
		{
		Object val = null;
		if (fields.containsKey(name))
			{
			Field f = (Field) fields.get(name);
			try
				{
				val = f.get(this);
				}
			catch (Exception e)
				{
				e.printStackTrace();
				}
			}
		return val;
		}

	
	@Override
	public String toString()
		{
		return "DurableStatus [DURABLE_ID=" + DURABLE_ID + ", TIME=" + TIME + ", STATE=" + STATE + ", SUBSTATE="
				+ SUBSTATE + ", STATUS_PK=" + STATUS_PK + "]";
		}

	public String toExtString()
		{
		StringBuffer sb = new StringBuffer();
		Iterator keys = fields.keySet().iterator();
		String name;
		Field field;
		Object val = null;
		while (keys.hasNext())
			{
			name = (String) keys.next();
			if (name.equals("fields")) continue;
			field = (Field) fields.get(name);
			try
				{
				val = field.get(this);
				}
			catch (IllegalArgumentException e)
				{
				e.printStackTrace();
				}
			catch (IllegalAccessException e)
				{
				e.printStackTrace();
				}
			sb.append(name + " : " + val + "\n");
			}
		return sb.toString();
		}

	public void putNewComments(String user, String time, String content)
		{
		if (this.COMMENTS == null)
			{
			this.COMMENTS = new HashMap<String, List<String>>();
			}
		if (content == null || content.trim().equals(""))
			{
			content = "N/A";
			}
		// prevent the content from breaking the JSON hashtable and vector
		// format
		content = removeillegalcharactors(content);
		List<String> comment = new ArrayList<String>();
		if (user != null && user.trim().length() == 0) user = "Unknown";
		comment.add(user);
		comment.add(time);
		comment.add(content);
		this.COMMENTS.put(time, comment);
		}

	public String getLatestComment()
		{
		String lastComment = null;
		TreeSet<String> ordered = new TreeSet<String>(COMMENTS.keySet());
		List<String> last = COMMENTS.get(ordered.last());
		lastComment = last.get(2).toString();
		lastComment = lastComment.replace(";", ",");
		return lastComment;
		}

	public static String removeillegalcharactors(String content)
		{

		if (content != null)
			{
			content = content.replace(',', ';');
			content = content.replace("=", " equals ");
			content = content.replace("\r", " ");
			content = content.replace("\n", " ");
			content = content.replace("\t", " ");
			content = content.replace('{', '(');
			content = content.replace('}', ')');
			content = content.replace('[', '(');
			content = content.replace(']', ')');

			}
		return content;
		}
	}
