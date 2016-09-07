package com.freescale.stateStamper.db;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;

import com.freescale.stateStamper.model.entity.BusinessRuleAction;
import com.freescale.stateStamper.model.entity.TransitionAction;

public class ConvertHashtableToJSON
	{
	private static Connection conn;
	private static final String NULLVALUE = "{}";
	private static Map2Json map2Json = new Map2Json();
	private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	/**
	 * in the hashtable formatted data, some of the keys contains a list of
	 * values. if the value is true, the list value need to be converted to
	 * string joined by , if it is false, the value will not be changed Now only
	 * EQUIPMENT#CHILDREN,DURABLE_STATUS$COMMENTS, LOCATION#COMPONENTS are using
	 * list value, all others are treated as string
	 */
	private static HashMap<String, Boolean> convertArrayToStringFlag = new HashMap<String, Boolean>();

	public static void main(String[] args) throws ClassNotFoundException, SQLException
		{
		Date startDate = new Date();
		System.out.println("job start at "+sdf.format(startDate));
		try 
			{
			initAttributes();
			getConnection();
			resetLocation();
			resetEquipmentAttributes();
			resetDurableStatus();
			resetEquipment();
			resetTransitionActions();
			resetBusinessRulesActions();
			closeConnection();
			}
		catch(Throwable t)
			{
			t.printStackTrace();
			}
		Date endDate = new Date();
		System.out.println("job start at "+sdf.format(endDate));
		System.out.println("total time spent:"+(endDate.getTime()-startDate.getTime())+" milliseconds");
		}

	private static void initAttributes()
		{
		convertArrayToStringFlag.clear();
		convertArrayToStringFlag.put("EQUIPMENT-DYNAMIC#ATTRIBUTES", Boolean.TRUE);
		convertArrayToStringFlag.put("EQUIPMENT#CHILDREN", Boolean.FALSE);
		convertArrayToStringFlag.put("DURABLE_STATUS#ATTRIBUTES", Boolean.TRUE);
		convertArrayToStringFlag.put("DURABLE_STATUS#COMMENTS", Boolean.FALSE);
		convertArrayToStringFlag.put("EQUIPMENT_ATTRIBUTE#VALIDATION_RULE", Boolean.TRUE);
		convertArrayToStringFlag.put("LOCATION#COMPONENTS", Boolean.FALSE);
		convertArrayToStringFlag.put("TRANSITIONS_ACTIONS#ACTION_PARAMETERS", Boolean.TRUE);
		convertArrayToStringFlag.put("BUSINESS_RULES_ACTIONS#ACTION_PARAMETERS", Boolean.TRUE);
		}

	private static void resetEquipment() throws SQLException
		{
		resetData("EQUIPMENT", "EQUIPMENT_PK", new String[] { "DYNAMIC_ATTRIBUTES", "CHILDREN" });
		}

	private static void resetDurableStatus() throws SQLException
		{
		resetData("DURABLE_STATUS", "STATUS_PK", new String[] { "ATTRIBUTES", "COMMENTS" });
		}

	private static void resetEquipmentAttributes() throws SQLException
		{
		resetData("EQUIPMENT_ATTRIBUTE", "EQUIPMENT_ATTRIBUTE_PK", new String[] { "VALIDATION_RULE" });
		}

	private static void resetLocation() throws SQLException
		{
		resetData("LOCATION", "LOCATION_PK", new String[] { "COMPONENTS" });
		}

	private static void resetData(String table_name, String primaryField, String[] hashtableFields) throws SQLException
		{
		String generateQuerySql = generateQuerySql(table_name, primaryField, hashtableFields);
		String generateUpdateSql = generateUpdateSql(table_name, primaryField, hashtableFields);
		PreparedStatement query = conn.prepareStatement(generateQuerySql);
		PreparedStatement update = conn.prepareStatement(generateUpdateSql);
		ResultSet rs = query.executeQuery();
		Integer pk = null;
		String hashtableStr = null;
		String jsonStr = null;
		Map<String, Object> map = new HashMap<String, Object>();
		int count=0;
		while (rs.next())
			{
			try
				{
				pk = rs.getInt(primaryField);
				update.setInt(hashtableFields.length + 1, pk);
				int i = 1;
				for (String column_name : hashtableFields)
					{
					Boolean flag = convertArrayToStringFlag.get(table_name + "#" + column_name);
					hashtableStr = rs.getString(column_name);
					map = str2HashMap(hashtableStr, flag);
					jsonStr = (String) map2Json.javaToSql(map);
					update.setString(i, jsonStr);
					i++;
					}
				update.execute();
				}
			catch(Exception e)
				{
				e.printStackTrace();
				System.out.println("issue occured while updating "+table_name+";primary key:"+pk);
				}
			count++;
			}
		System.out.println("Table:"+table_name+"; totalRowCount:"+count);
		rs.close();
		query.close();
		update.close();
		}

	private static void resetTransitionActions() throws SQLException
		{
		resetActionParamsByAction("TRANSITIONS_ACTIONS", "TRANSITIONS_ACTIONS_PK", "ACTION", "ACTION_PARAMETERS");
		}

	private static void resetBusinessRulesActions() throws SQLException
		{
		resetActionParamsByAction("BUSINESS_RULES_ACTIONS", "BUSINESS_RULES_ACTIONS_PK", "ACTION",
				"ACTION_PARAMETERS");
		}

	private static void resetActionParamsByAction(String table_name, String primaryKey, String action_column, String actionParams_column) throws SQLException
		{
		String generateQuerySql = generateQuerySql(table_name, primaryKey, new String[] { action_column,
				actionParams_column });
		String generateUpdateSql = generateUpdateSql(table_name, primaryKey, new String[] { actionParams_column });
		PreparedStatement query = conn.prepareStatement(generateQuerySql);
		PreparedStatement update = conn.prepareStatement(generateUpdateSql);
		ResultSet rs = query.executeQuery();
		Integer pk = null;
		String hashtableStr = null;
		String jsonStr = null;
		Map<String, Object> table = new HashMap<String, Object>();
		Boolean flag = convertArrayToStringFlag.get(table_name + "#" + actionParams_column);
		int count=0;
		while (rs.next())
			{
			try
				{
				pk = rs.getInt(primaryKey);
				update.setInt(2, pk);
				String action = rs.getString(action_column);
				String params = rs.getString(actionParams_column);
				hashtableStr = formatHashtableString(table_name, action, params);
				table = str2HashMap(hashtableStr, flag);
				jsonStr = (String) map2Json.javaToSql(table);
				update.setString(1, jsonStr);
				update.execute();
				}
			catch(Exception e)
				{
				e.printStackTrace();
				System.out.println("issue occured while updating "+table_name+";primary key:"+pk);
				}
			count++;
			}
		System.out.println("Table:"+table_name+"; totalRowCount:"+count);
		rs.close();
		query.close();
		update.close();
		}

	private static Map<String, Object> str2HashMap(String source, Boolean convertArrayToString)
		{
		Map<String, Object> str2Map = new HashMap<String, Object>();
		if (source == null || NULLVALUE.equals(source)) return str2Map;
		if (source.startsWith("{" + TransitionAction.PARAM_TO))
			{
			source = source.substring(1, source.length() - 1);
			str2Map.put(TransitionAction.PARAM_TO, source.substring(3));
			}
		else
			{
			str2Map = map2Json.str2Map(source);
			List<String> list = new ArrayList<String>(str2Map.keySet());
			if (convertArrayToString == null || Boolean.TRUE.equals(convertArrayToString))
				{
				for (String key : list)
					{
					Object val = str2Map.get(key);
					if (val instanceof List)
						{
						List<String> temp = (List<String>) val;
						str2Map.put(key, StringUtils.join(temp.toArray(), ","));
						}
					}
				}
			}
		return str2Map;
		}

	private static String formatHashtableString(String table, String action, String params)
		{
		String result = null;
		if ("BUSINESS_RULES_ACTIONS".equals(table))
			{
			if (BusinessRuleAction.UPDATE_DURABLE.equalsIgnoreCase(action)
					|| BusinessRuleAction.LOG_UPDATE_DURABLE.equalsIgnoreCase(action)) result = "attribute="
					+ params;
			else if (BusinessRuleAction.ALARM_REPORT.equalsIgnoreCase(action)||BusinessRuleAction.ALARM_WINDOW.equalsIgnoreCase(action)) result = "Color=" + params;
			else if (BusinessRuleAction.MONITOR_PAGE.equalsIgnoreCase(action))
				{
				String attribute = params;
				String value = "";
				if (params.contains("="))
					{
					String[] split = params.split("=");
					attribute = split[0];
					value = split[1];
					}
				result = "count=" + attribute + ",limit=" + value + "";
				}
			else if (BusinessRuleAction.CHANGE_HEADER.equalsIgnoreCase(action))
				{
				result = params.replaceAll(":", "=");
				}
			else result = params;
			}
		else if ("TRANSITIONS_ACTIONS".equals(table))
			{
			if (TransitionAction.DECREASE_BY.equalsIgnoreCase(action)
					|| TransitionAction.INCREASE_BY.equalsIgnoreCase(action)) result = params;
			else if (TransitionAction.CLEAR.equalsIgnoreCase(action)
					|| TransitionAction.RESET.equalsIgnoreCase(action)
					|| TransitionAction.INCREASE.equalsIgnoreCase(action)
					|| TransitionAction.DECREASE.equalsIgnoreCase(action)) result = "attribute=" + params;
			else if (TransitionAction.SET.equalsIgnoreCase(action))
				{
				String attribute = params;
				String value = "";
				if (params.contains("="))
					{
					String[] split = params.split("=");
					attribute = split[0];
					value = split[1];
					}
				result = "attribute=" + attribute + ",value=" + value + "";
				}
			else result = params;
			}
		if (result == null) result = "";
		return "{" + result + "}";
		}

	private static String generateQuerySql(String table_name, String primaryField, String[] hashtableFields)
		{
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT ");
		sql.append(primaryField);
		for (String temp : hashtableFields)
			{
			sql.append("," + temp);
			}
		sql.append(" FROM ");
		sql.append(table_name);
		return sql.toString();
		}

	private static String generateUpdateSql(String table_name, String primaryField, String[] hashtableFields)
		{
		StringBuilder sql = new StringBuilder();
		sql.append("UPDATE ");
		sql.append(table_name);
		sql.append(" SET ");
		for (String temp : hashtableFields)
			{
			sql.append(temp + "=?,");
			}
		sql.deleteCharAt(sql.length() - 1);
		sql.append(" WHERE ");
		sql.append(primaryField + "=?");
		return sql.toString();
		}

	private static void getConnection() throws ClassNotFoundException, SQLException
		{
		Properties prop = new Properties();
		InputStream in = ConvertHashtableToJSON.class.getResourceAsStream("jdbc.properties");
		try
			{
			prop.load(in);
			}
		catch (IOException e)
			{
			System.err.println("cann't find jdbc.properties");
			e.printStackTrace();
			}
		String driverName  = (String)prop.get("DRIVER_CLASS");
		String connection = (String)prop.getProperty("CONNECTION_STRING");
		String userName = (String)prop.getProperty("USER_NAME");
		String password = (String)prop.getProperty("PASS_WORD");
		Class.forName(driverName);
		conn = DriverManager.getConnection(connection, userName, password);
		}

	private static void closeConnection() throws ClassNotFoundException, SQLException
		{
		conn.close();
		}
	}