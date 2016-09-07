package com.freescale.stateStamper.db;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;

import com.freescale.stateStamper.model.entity.Area;
import com.freescale.stateStamper.model.entity.EquipmentType;
import com.freescale.stateStamper.model.entity.Machine;
import com.freescale.stateStamper.model.entity.Model;

public class RTDDataMigration
	{
	private static Connection conn = null;
	private static Connection genesisConn = null;
	private static EquipmentType equipmentType = null;
	private static Area area = null;
	private static String folder = null;
	public static void main(String[] args) throws Exception
		{
		RTDDataMigration migration = new RTDDataMigration();
		int equipmentTypePK = 0;
		int areaPk = 0;
		for (int i = 0; i < args.length; i++)
			{
			if ("-path".equalsIgnoreCase(args[i]))
				{
				folder = args[i + 1];
				}
			if ("-equipment_type_pk".equalsIgnoreCase(args[i]))
				{
				equipmentTypePK = Integer.parseInt(args[i + 1]);
				}
			if ("-area_pk".equalsIgnoreCase(args[i]))
				{
				areaPk = Integer.parseInt(args[i + 1]);
				}
			}
		if (StringUtils.isEmpty(folder))
			{
			throw new Exception(
					"Please input filename after -file, the command line should be like java -jar migrator.jar -path c://sqls// -equipment_type_pk 353 -area_pk 3");
			}
		if (equipmentTypePK == 0)
			{
			throw new Exception(
					"Please input equipment type pk after -equipment_type_pk, the command line should be like java -jar migrator.jar -path c://sqls// -equipment_type_pk 353 -area_pk 3");
			}
		if (areaPk==0)
			{
			throw new Exception(
					"Please input department for the data migration");
			}
		
		try 
			{
			getConnection();
			loadEquipmentType(equipmentTypePK);
			loadArea(areaPk);
			Map<String,Machine> tttEquipments = loadTTTEquipments();
			List<String> genesisEquipments = loadGenesisEquipments();
			List<Model> tttModels = loadTTTModels();
			List<String> genarateModelSqls = genarateModelSqls(tttModels);
			List<String> generateMachinesSqls = generateMachinesSqls(tttEquipments,genesisEquipments);
			List<String> mids = generateInvalidMachines(tttEquipments,genesisEquipments);
			writeSqlToFiles(folder+"/"+"machines.sql",generateMachinesSqls);
			writeSqlToFiles(folder+"/"+"models.sql",genarateModelSqls);
			writeSqlToFiles(folder+"/"+"invalidmachines.txt",mids);
			} 
		catch(Exception e)
			{
			e.printStackTrace();
			}
		finally
			{
			closeConnection();
			}
		}
	
	private static Map<String,Machine> loadTTTEquipments() throws SQLException
		{
		Map<String,Machine> machines = new HashMap<String,Machine>();
		PreparedStatement statement = conn.prepareStatement("SELECT MES_NAME,MODEL_NAME,PLATFORM FROM EQUIPMENT WHERE EQUIPMENT_TYPE_PK=? and AREA_PK=?");
		statement.setInt(1, equipmentType.primaryKey);
		statement.setInt(2, area.getPrimaryKey());
		ResultSet rs = statement.executeQuery();
		while (rs.next())
			{
			String mesName = rs.getString(1);
			String modelName = rs.getString(2);
			String platform = rs.getString(3);
			Machine machine = new Machine();
			machine.mESName = mesName;
			machine.setModelName(modelName);
			machine.setPlatform(platform);
			machines.put(machine.mESName, machine);
			}
		rs.close();
		statement.close();
		return machines;
		}
	
	private static List<String> loadGenesisEquipments() throws SQLException
		{
		List<String> machines = new ArrayList<String>();
		PreparedStatement statement = genesisConn.prepareStatement("SELECT MACH_ID FROM MACHINES WHERE MACH_MTYP_TYPE=? AND MACH_DEPT=?");
		statement.setString(1, equipmentType.mesEquipmentType);
		statement.setString(2, area.getName());
		ResultSet rs = statement.executeQuery();
		while (rs.next())
			{
			machines.add(rs.getString(1));
			}
		rs.close();
		statement.close();
		return machines;
		}
	
	private static List<Model> loadTTTModels() throws SQLException
		{
		List<Model> models = new ArrayList<Model>();
		PreparedStatement statement = conn.prepareStatement("SELECT MODEL_NAME,MODEL_DESCRIPTION,MFR_CODE,PLATFORM FROM MODEL WHERE EQUIPMENT_TYPE_PK=? AND AREA_PK=?");
		statement.setInt(1, equipmentType.primaryKey);
		statement.setInt(2, area.getPrimaryKey());
		ResultSet rs = statement.executeQuery();
		while (rs.next())
			{
			Model model = new Model();
			model.setModelName(rs.getString(1));
			model.setModelDescription(rs.getString(2));
			model.setMfrCode(rs.getString(3));
			model.setPlatform(rs.getString(4));
			models.add(model);
			}
		rs.close();
		statement.close();
		return models;
		}
	
	private static List<String> genarateModelSqls(List<Model> tttModels)
		{
		List<String> sqls = new ArrayList<String>();
		for(Model model:tttModels)
			{
			String sql1 = "INSERT INTO MACHINE_MODELS(mach_model,description,vendor_no,platform) values('"+encode(model.getModelName())+"','"+encode(model.getModelDescription())+"','"+model.getMfrCode()+"','"+model.getPlatform()+"');";
			String sql2 = "INSERT INTO MACH_TYPE_MODEL_COMBINATIONS(MTYP_TYPE,MACH_MODEL) VALUES('"+equipmentType.mesEquipmentType+"','"+encode(model.getModelName())+"');";
			sqls.add(sql1);
			sqls.add(sql2);
			}
		return sqls;
		}
	
	private static List<String> generateMachinesSqls(Map<String,Machine> tttMachines,List<String> genesisMachines)
		{
		List<String> sqls = new ArrayList<String>();
		for(Entry<String,Machine> entry:tttMachines.entrySet())
			{
			String mid = entry.getKey();
			Machine machine = entry.getValue();
			if(genesisMachines.contains(mid))
				{
				String sql = "update MACHINES SET mach_model='"+encode(machine.getModelName())+"',MACH_PLATFORM='"+encode(machine.getPlatform())+"',MACH_STATUS='ACTIVE' where MACH_ID='"+machine.mESName+"' and MACH_MTYP_TYPE='"+equipmentType.mesEquipmentType+"';";
				sqls.add(sql);
				}
			else
				{
				String sql = "INSERT INTO MACHINES(MACH_ID,MACH_NAME,MACH_MTYP_TYPE,MACH_DEPT,MACH_ENTER_DATE,MACH_ENTER_USER,MACH_CLOSE_DATE,MACH_SITE,MACH_MODEL,EQUIPMENT,MACH_STATUS,MACH_PLATFORM) "
						+ "VALUES('"+mid+"','"+mid+"','"+equipmentType.mesEquipmentType+"','"+area.getName()+"',sysdate,'TTT',sysdate,'None','"+encode(machine.getModelName())+"','"+mid+"','ACTIVE','"+machine.getPlatform()+"');";
				sqls.add(sql);
				}
			}
		return sqls;
		}
	
	private static List<String> generateInvalidMachines(Map<String,Machine> tttMachines,List<String> genesisMachines){
		List<String> mids = new ArrayList<String>();
		for(String mid:genesisMachines)
			{
			if(!tttMachines.containsKey(mid))
				{
				mids.add(mid);
				}
			}
		return mids;
	}
	
	private static void writeSqlToFiles(String fileName,List<String> sqls) throws FileNotFoundException
		{
		PrintWriter pw = new PrintWriter(new FileOutputStream(fileName));
		for(String sql:sqls)
			{
			pw.println(sql);
			}
		pw.flush();
		pw.close();
		}
	
	private static void getConnection() throws Exception
		{
		Properties prop = new Properties();
		InputStream in = ConvertHashtableToJSON.class.getResourceAsStream("jdbc.properties");
		prop.load(in);
		String driverName = (String) prop.get("DRIVER_CLASS");
		String connection = (String) prop.getProperty("CONNECTION_STRING");
		String userName = (String) prop.getProperty("USER_NAME");
		String password = (String) prop.getProperty("PASS_WORD");
		String genesisConnection = (String) prop.getProperty("GENESIS_CONNECTION_STRING");
		String genesisUserName = (String) prop.getProperty("GENESIS_USER_NAME");
		String genesisPassword = (String) prop.getProperty("GENESIS_PASS_WORD");
		Class.forName(driverName);
		conn = DriverManager.getConnection(connection, userName, password);
		genesisConn = DriverManager.getConnection(genesisConnection, genesisUserName, genesisPassword);

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
			conn = null;
			}
		
		if(genesisConn!=null)
			{
			try
				{
				genesisConn.close();
				}
			catch (SQLException e)
				{
				e.printStackTrace();
				}
			genesisConn = null;
			}
		
		}
	
	private static void loadEquipmentType(Integer equipmentTypePk) throws Exception
		{
		PreparedStatement statement = conn.prepareStatement("SELECT TYPE_NAME,MES_TYPE FROM EQUIPMENT_TYPE WHERE EQUIPMENT_TYPE_PK=?");
		statement.setInt(1, equipmentTypePk);
		ResultSet rs = statement.executeQuery();
		while (rs.next())
			{
			equipmentType = new EquipmentType();
			String typeName = rs.getString(1);
			String mesName = rs.getString(2);
			equipmentType.name = typeName;
			equipmentType.mesEquipmentType = mesName;
			equipmentType.primaryKey = equipmentTypePk;
			break;
			}
		rs.close();
		statement.close();
		}
	
	private static void loadArea(Integer areaPk) throws Exception
		{
		PreparedStatement statement = conn.prepareStatement("SELECT NAME FROM AREA WHERE AREA_PK=?");
		statement.setInt(1, areaPk);
		ResultSet rs = statement.executeQuery();
		while (rs.next())
			{
			area = new Area();
			String name = rs.getString(1);
			area.setPrimaryKey(areaPk);
			area.setName(name);
			break;
			}
		rs.close();
		statement.close();
		}
	
	private static String encode(String s)
		{
		if(s.contains("&"))
			{
			s = s.replace("&","'||chr(38)||'");
			}
		return s;
		}
	
	}
