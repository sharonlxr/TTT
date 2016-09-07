package com.freescale.stateStamper.model.logic;

import com.freescale.stateStamper.model.entity.IStatus;
import java.sql.*;
import java.util.*;
import oracle.jdbc.driver.OracleDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExternalDBSource
	{
	private Logger logger;
	private Connection conn;
	private static final String PARAM_CHAR = "$";

	public ExternalDBSource(String dbServer, String sid, String port, String user, String password)
		{
		logger = LoggerFactory.getLogger("stateStamper");
		try
			{
			String connection = "jdbc:oracle:thin:@" + dbServer + ":" + port + ":" + sid;
			logger.info("Creating External Oracle connection "+ connection +":"+user);
			DriverManager.registerDriver(new OracleDriver());
			conn = DriverManager.getConnection(connection,user, password);
			logger.info("External Oracle Connection created");
			}
		catch (SQLException e)
			{
			logger.error(e.getMessage());
			e.printStackTrace();
			}
		}
	
	public boolean close() throws SQLException
		{
		boolean close = false;
		if (conn != null && !conn.isClosed())
			{
			conn.close();
			close = true;
			}
		return close;
		}

	public List executeQuery(String sql, IStatus status)
		{
		return executeQuery(replaceParameters(sql, status));
		}

	public List executeQuery(String sql)
		{
		List result = new ArrayList();
		try
			{
			PreparedStatement stmt = conn.prepareStatement(sql);
			ResultSet rs = stmt.executeQuery(sql);
			List colNames = getColumnNames(rs);
			while (rs.next())
				{
				result.add(rs.getString(1));
				}
			rs.close();
			stmt.close();
			}
		catch (SQLException e)
			{
			e.printStackTrace();
			}
		finally
		{
		
		}
		return result;
		}

	private List getColumnNames(ResultSet rs)
		{
		List colNames = null;
		try
			{
			ResultSetMetaData rsMD = rs.getMetaData();
			int colCount = rsMD.getColumnCount();
			colNames = new ArrayList(colCount);
			for (int i = 1; i <= colCount; i++)
				{
				colNames.add(rsMD.getColumnName(i));
				}
			}
		catch (SQLException e)
			{
			e.printStackTrace();
			colNames = new ArrayList();
			}
		return colNames;
		}

	public Map getMapFromRecord(ResultSet rs, List columnNames) throws SQLException
		{
		Map record = new HashMap();

		for (int i = 0; i < columnNames.size(); i++)
			{
			String name = columnNames.get(i).toString();
			String value = rs.getString(i + 1);
			record.put(name, value);
			}
		return record;
		}
	

	private String replaceParameters(String query, IStatus status)
		{
		String result = query;
		while (result.indexOf(PARAM_CHAR) != -1)
			{
			String param = getParam(result);
			String value = String.valueOf(status.get(param.substring(param.indexOf(PARAM_CHAR) + 1)));
			if(isList(value))
				{
				result = replaceList(query,param,getListFromString(value));
				}
			else
				{
				result = result.replace(param, value);
				}
			}
		return result;
		}

	public String getParam(String src)
		{
		int idxStart = src.indexOf(PARAM_CHAR);
		if (idxStart == -1) return src;
		String result = null;
		String[] endChars = { "'", " " };

		for (String end : endChars)
			{
			int idxEnd = src.indexOf(end, idxStart);
			if (idxEnd != -1)
				{
				result = src.substring(idxStart, idxEnd);
				break;
				}
			}
		return result;
		}
	
	public String getParamName(String src)
		{
		String param = getParam(src);
		return param.substring(param.indexOf(PARAM_CHAR) + 1);
		}
	
	private static boolean isList(String value)
		{
		return value != null && value.startsWith("[") && value.endsWith("]");
		}

	private List getListFromString(String value)
		{
		List result = new ArrayList();
		if(isList(value))
			{
			String values = value.substring(1);
			values = values.substring(0,values.length()-1);
			String[] tmp = values.split(",");
			for(String s:tmp){result.add(s.trim());}
			}
		return result;
		}
	
	private String replaceList(String qry,String param,List values)
		{
		String[] tokens = qry.split(" ");
		int idxField = 0, idxOperator = 0, idxParam = 0;
		boolean found = false;
		for(int i = 0; i < tokens.length && !found;i++)
			{
			if(tokens[i].contains(param))
				{
				idxParam = i;
				idxOperator = i - 1;
				idxField = i - 2;
				found= true;
				}
			}
		if(!found || idxOperator < 0  || idxField < 0 ){return qry;}
		StringBuffer qryBeforeList = new StringBuffer(),qryAfterList = new StringBuffer(), replacedList = new StringBuffer();
		for(int i=0;i<idxField;i++){qryBeforeList.append(tokens[i]+" ");}
		for(int i=idxParam+1;i<tokens.length;i++){qryAfterList.append(tokens[i]+" ");}
		replacedList.append("( ");
		for(int i = 0;i< values.size();i++)
			{
			replacedList.append(tokens[idxField]+" ");
			replacedList.append(tokens[idxOperator]+" ");
			replacedList.append(tokens[idxParam].replace(param,(String)values.get(i))+" ");
			if(i+1 < values.size()){replacedList.append(" OR ");}
			}
		replacedList.append(" )");
		return  qryBeforeList.toString() + replacedList.toString() + qryAfterList.toString();
		}
	
	}