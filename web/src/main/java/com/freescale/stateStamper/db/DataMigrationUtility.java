package com.freescale.stateStamper.db;

import java.io.File;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataMigrationUtility
	{
	private static Connection conn = null;
	private static Map2Json map2Json = new Map2Json();
	private List<String> statusAttributes = new ArrayList<String>();
	private List<String> equipmentAttributes = new ArrayList<String>();
	private Map<String, String> attributesTypeMap = new HashMap<String, String>();
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
	private String[] durableStatusColumns = new String[] { "STATUS_PK", "EQUIPMENT_PK", "EVENT_PK", "TIME",
			"STATE", "SUBSTATE", "OPERATOR_ID", "DURABLE_ID", "ATTRIBUTES", "COMMENTS", "WORKSTATION", "LOCATION",
			"DURATION" };
	private String[] equipmentColumns = new String[] { "EQUIPMENT_PK", "SERIAL_NUMBER", "EQUIPMENT_NAME", "CLASS",
			"ENGINEER_CONTACT", "VENDOR", "WORKSTATION", "LOCATION", "EQUIPMENT_TYPE_PK", "AREA_PK", "MES_NAME",
			"PIC_HOST", "PIC_PORT", "DMHBOX", "PLATFORM", "DEVICE_GROUP", "DYNAMIC_ATTRIBUTES", "CHILDREN" };
	private String[] requiredFields = new String[] { "SERIAL_NUMBER", "EQUIPMENT_NAME", "TYPE",
			"ENGINEER_CONTACT", "PLATFORM", "DEVICE_GROUP" };
	private static Logger logger;
	private Map<String, String> equipments = new HashMap<String, String>();
	private Set<String> statusList = new HashSet<String>();

	public DataMigrationUtility()
		{
		logger = LoggerFactory.getLogger("StateStamper");
		}

	/**
	 * args[0] whould be the file exact path. args[1] should be equipment type
	 * Pk
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception
		{
		try
			{
			DataMigrationUtility dmUtility = new DataMigrationUtility();
			String fileName = null;
			int equipmentTypePK = 0;
			for (int i = 0; i < args.length; i++)
				{
				if ("-file".equalsIgnoreCase(args[i]))
					{
					fileName = args[i + 1];
					}
				if ("-equipment_type_pk".equalsIgnoreCase(args[i]))
					{
					equipmentTypePK = Integer.parseInt(args[i + 1]);
					}
				}
			if (StringUtils.isEmpty(fileName))
				{
				throw new Exception(
						"Please input filename after -file, the command line should be like java -jar migrator.jar -file c://xx.excel -equipment_type_pk 353");
				}
			if (equipmentTypePK == 0)
				{
				throw new Exception(
						"Please input equipment type pk after -equipment_type_pk, the command line should be like java -jar migrator.jar -file c://xx.excel -equipment_type_pk 353");
				}

			Date start = new Date();
			logger.info("Job start at " + dmUtility.sdf.format(start));
			dmUtility.importData(fileName, equipmentTypePK);
			Date end = new Date();
			logger.info("Job end at " + dmUtility.sdf.format(end));
			logger.info("Totally spend " + (end.getTime() - start.getTime()) + " milliseconds");
			}
		catch (Exception e)
			{
			logger.error(e.getMessage());
			e.printStackTrace();
			}
		}

	private void loadEquipments() throws SQLException
		{
		equipments.clear();
		PreparedStatement statement = conn.prepareStatement("SELECT EQUIPMENT_NAME, EQUIPMENT_PK FROM EQUIPMENT");
		ResultSet rs = statement.executeQuery();
		while (rs.next())
			{
			String equipmentName = rs.getString(1);
			String primaryKey = rs.getString(2);
			equipments.put(equipmentName, primaryKey);
			}
		}

	private void loadDurableStatus() throws SQLException
		{
		statusList.clear();
		PreparedStatement statement = conn.prepareStatement("SELECT DURABLE_ID FROM DURABLE_STATUS");
		ResultSet rs = statement.executeQuery();
		while (rs.next())
			{
			String durableId = rs.getString(1);
			statusList.add(durableId);
			}
		}

	private void loadEquipmentAttributes(Integer equipmentTypePk) throws Exception
		{
		PreparedStatement statement = conn
				.prepareStatement("SELECT ATTRIBUTE_NAME, BELONGS, TYPE FROM EQUIPMENT_ATTRIBUTE WHERE EQUIPMENT_TYPE_PK=?");
		statement.setInt(1, equipmentTypePk);
		ResultSet rs = statement.executeQuery();
		while (rs.next())
			{
			String attributeName = rs.getString(1);
			String belongs = rs.getString(2);
			String type = rs.getString(3);
			attributesTypeMap.put(attributeName, type);
			if ("STATUS".equals(belongs))
				{
				statusAttributes.add(attributeName);
				}
			if ("EQUIPMENT".equals(belongs)&&!attributeName.endsWith("eIVariable") && !attributeName.endsWith("units"))
				{
				equipmentAttributes.add(attributeName);
				}
			}
		if (attributesTypeMap.size() == 0)
			throw new Exception("No Equipment Attribute found for equipment type " + equipmentTypePk);
		}

	public void importData(String fileName, Integer equipmentTypePk) throws Exception
		{
		clearMemory();
		getConnection();
		loadEquipmentAttributes(equipmentTypePk);
		List<Map<String, String>> data = loadExcelData(fileName, equipmentTypePk);
		importDurable(data);
		importDurableStatus(data);
		closeConnection();
		}

	private void clearMemory()
		{
		statusAttributes.clear();
		equipmentAttributes.clear();
		statusAttributes.clear();
		equipments.clear();
		statusList.clear();
		}

	private List<Map<String, String>> loadExcelData(String excelName, Integer equipmentTypePk) throws Exception
		{
		List<Map<String, String>> results = new ArrayList<Map<String, String>>();
		Workbook book = Workbook.getWorkbook(new File(excelName));
		Sheet sheet = book.getSheet(0);
		int columnum = sheet.getColumns();
		int rownum = sheet.getRows();
		List<String> headers = new ArrayList<String>();
		for (int j = 0; j < columnum; j++)
			{
			Cell headerCell = sheet.getCell(j, 0);
			headers.add(headerCell.getContents());
			}
		for (String field : requiredFields)
			{
			if (!headers.contains(field)) throw new Exception("Missing required column " + field);
			}
		for (int i = 1; i < rownum; i++)
			{
			Map<String, String> durable = new HashMap<String, String>();
			durable.put("EQUIPMENT_TYPE_PK", String.valueOf(equipmentTypePk));
			for (int j = 0; j < columnum; j++)
				{
				Cell valueCell = sheet.getCell(j, i);
				String value = valueCell.getContents();
				Cell keyCell = sheet.getCell(j, 0);
				String key = keyCell.getContents();
				durable.put(key, value);
				}
			results.add(durable);
			}
		book.close();
		return results;
		}

	private void importDurable(List<Map<String, String>> data) throws Exception
		{
		logger.info("Import Durable start at " + sdf.format(new Date()));
		loadEquipments();
		String sql = generateInsertSql("EQUIPMENT", equipmentColumns);
		try
			{
			PreparedStatement prepareStatement = conn.prepareStatement(sql);
			int length = equipmentColumns.length;
			int success = 0;
			int error = 0;
			int index = 0;
			for (Map<String, String> durable : data)
				{
				index++;
				try
					{
					for (int i = 1; i < length; i++)
						{
						String column = equipmentColumns[i];
						String value = retrieveDurableValue(column, durable);
						prepareStatement.setString(i, value);
						}
					prepareStatement.executeUpdate();
					success++;
					}
				catch (Exception e)
					{
					error++;
					logger.error("Row:" + index + ";" + e.getMessage());
					}
				}
			logger.info("Total Migrate Equipment Data:" + index + ";Success:" + success + ";Error:" + error);
			prepareStatement.close();
			}
		catch (SQLException e)
			{
			e.printStackTrace();
			}
		logger.info("Import Durable end at " + sdf.format(new Date()));
		}

	public String retrieveDurableValue(String column, Map<String, String> data) throws Exception
		{
		String result = null;
		if ("CLASS".equalsIgnoreCase(column))
			{
			result = data.get("TYPE");
			}
		else if ("EQUIPMENT_NAME".equalsIgnoreCase(column))
			{
			result = getEquipmentName(data);
			}
		else if ("MES_NAME".equalsIgnoreCase(column))
			{
			result = getDurableID(data);
			}
		else if ("LOCATION".equalsIgnoreCase(column))
			{
			result = getLocation(data);
			}
		else if ("CHILDREN".equalsIgnoreCase(column))
			{
			result = "{}";
			}
		else if ("DYNAMIC_ATTRIBUTES".equalsIgnoreCase(column))
			{
			result = getAttributes(data, equipmentAttributes);
			}
		else
			{
			result = data.get(column);
			}
		return result;
		}

	private void importDurableStatus(List<Map<String, String>> data) throws SQLException
		{
		logger.info("Import Durable Status start at " + sdf.format(new Date()));
		loadEquipments();
		loadDurableStatus();
		String sql = generateInsertSql("DURABLE_STATUS", durableStatusColumns);
		try
			{
			PreparedStatement prepareStatement = conn.prepareStatement(sql);
			int length = durableStatusColumns.length;
			int success = 0;
			int error = 0;
			int index = 0;
			for (Map<String, String> durable : data)
				{
				index++;
				try
					{
					for (int i = 1; i < length; i++)
						{
						String column = durableStatusColumns[i];
						String value = retrieveDurableStatusValue(column, durable);
						prepareStatement.setString(i, value);
						}
					prepareStatement.executeUpdate();
					success++;
					}
				catch (Exception e)
					{
					error++;
					logger.error("Row:" + index + ";" + e.getMessage());
					}

				}
			logger.info("Total Migrate Status Data:" + index + ";Success:" + success + ";Error:" + error);
			}
		catch (SQLException e)
			{
			e.printStackTrace();
			}
		logger.info("Import Durable Status end at " + sdf.format(new Date()));
		}

	public String retrieveDurableStatusValue(String column, Map<String, String> data) throws Exception
		{
		String result = null;
		if ("TIME".equalsIgnoreCase(column))
			{
			result = sdf.format(new Date());
			}
		else if ("STATE".equalsIgnoreCase(column))
			{
			result = "Unknown";
			}
		else if ("SUBSTATE".equalsIgnoreCase(column))
			{
			result = "Unknown";
			}
		else if ("DURABLE_ID".equalsIgnoreCase(column))
			{
			result = getDurableID(data);
			}
		else if ("OPERATOR_ID".equalsIgnoreCase(column))
			{
			result = data.get("ENGINEER_CONTACT");
			}
		else if ("ATTRIBUTES".equalsIgnoreCase(column))
			{
			result = getAttributes(data, statusAttributes);
			}
		else if ("EQUIPMENT_PK".equalsIgnoreCase(column))
			{
			result = getEquipmentPK(data);
			}
		else if ("LOCATION".equalsIgnoreCase(column))
			{
			result = getLocation(data);
			}
		else if ("COMMENTS".equalsIgnoreCase(column))
			{
			result = getComments(data);
			}
		else
			{
			result = data.get(column);
			}
		return result;
		}

	private String getAttributes(Map<String, String> data, List<String> attributes)
		{
		String result;
		Map<String, String> map = new HashMap<String, String>();
		for (String attribute : attributes)
			{
			String value = null;
			if ("Counter".equalsIgnoreCase(attributesTypeMap.get(attribute)))
				{
				value = "0";
				}
			else
				{
				value = "";
				}
			for (Entry<String, String> entry : data.entrySet())
				{
				if (attribute.equalsIgnoreCase(entry.getKey()))
					{
					value = entry.getValue();
					break;
					}
				}
			map.put(attribute, value);
			}
		result = (String) map2Json.javaToSql(map);
		return result;
		}

	private String getEquipmentName(Map<String, String> data) throws Exception
		{
		String result = data.get("EQUIPMENT_NAME");
		if (!equipments.containsKey(result))
			{
			equipments.put(result, null);
			}
		else
			{
			throw new Exception("EQUIPMENT TABLE:Duplicate " + result);
			}
		return result;
		}

	private String getDurableID(Map<String, String> data) throws Exception
		{
		String result = data.get("EQUIPMENT_NAME");
		if (statusList.contains(result))
			{
			throw new Exception("DURABLE_STATUS TABLE:Duplicate " + result);
			}
		else
			{
			statusList.add(result);
			}
		return result;
		}

	private String getEquipmentPK(Map<String, String> data)
		{
		String result;
		result = equipments.get(data.get("EQUIPMENT_NAME"));
		return result;
		}

	private String getComments(Map<String, String> data)
		{
		String result;
		String date = sdf.format(new Date());
		Map<String, List<String>> comments = new HashMap<String, List<String>>();
		List<String> temp = new ArrayList<String>();
		temp.add(data.get("ENGINEER_CONTACT"));
		temp.add(date);
		temp.add("Data migration");
		comments.put(date, temp);
		result = (String) map2Json.javaToSql(comments);
		return result;
		}

	private String getLocation(Map<String, String> data)
		{
		String result;
		result = data.get("LOCATION");
		if (StringUtils.isEmpty(result))
			{
			result = "Unknown";
			}
		return result;
		}

	public String generateInsertSql(String table, String[] columns)
		{
		StringBuilder sql = new StringBuilder();
		sql.append("INSERT INTO " + table + " (");
		int length = columns.length;
		for (int i = 0; i < length; i++)
			{
			if (i == 0)
				{
				sql.append(columns[i]);
				}
			else
				{
				sql.append("," + columns[i]);
				}
			}
		sql.append(") values (");
		for (int i = 0; i < length; i++)
			{
			if (i == 0)
				{
				if (table.equals("EQUIPMENT"))
					{
					sql.append("EQUIPMENT_SEQ.NEXTVAL");
					}
				else
					{
					sql.append("DURABLE_STATUS_SEQ.NEXTVAL");
					}
				}
			else
				{
				sql.append(",?");
				}
			}
		sql.append(")");
		return sql.toString();
		}

	private static void getConnection()
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
		String driverName = (String) prop.get("DRIVER_CLASS");
		String connection = (String) prop.getProperty("CONNECTION_STRING");
		String userName = (String) prop.getProperty("USER_NAME");
		String password = (String) prop.getProperty("PASS_WORD");
		try
			{
			Class.forName(driverName);
			conn = DriverManager.getConnection(connection, userName, password);
			}
		catch (Exception e)
			{
			e.printStackTrace();
			}
		}

	private static void closeConnection()
		{
		if (conn != null)
			{
			try
				{
				conn.close();
				}
			catch (SQLException e)
				{
				e.printStackTrace();
				}
			}
		conn = null;
		}
	}
