package com.freescale.stateStamper.util;

import com.freescale.stateStamper.model.entity.Model;


public class ModelUtil
	{
	public static String generateFilterModelToken()
		{
		String[] tokenCharacterArray = new String[] { "A", "C", "E", "F", "H", "K", "L", "M", "N", "P",
				"Q", "R", "S", "T", "W", "X", "Y", "Z", "1", "2", "3", "4", "5", "6", "7", "8", "9" };
		long base = 13122;
		int rank1 = 27 * 27;
		int rank2 = 27;
		return getTokenFromIndex(tokenCharacterArray,base,rank1,rank2);
		}
	
	public static String generateGenericModelToken()
		{
		String[] tokenCharacterArray = new String[] { "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P",
				"Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z", "1", "2", "3", "4", "5", "6", "7", "8", "9", "0"};
		long base = 46656;
		int rank1 = 36 * 36;
		int rank2 = 36;
		return getTokenFromIndex(tokenCharacterArray,base,rank1,rank2);
		}

	private static String getTokenFromIndex(String[] tokenCharacterArray,long base,int rank1,int rank2)
		{
		long total = Math.round(Math.floor(Math.random() * base));
		long rem1 = total % rank1;
		long n1 = (total - rem1) / rank1;
		String c1 = tokenCharacterArray[(int) n1];
		total = total - n1 * rank1;
		long rem2 = total % rank2;
		long n2 = (total - rem2) / rank2;
		String c2 = tokenCharacterArray[(int) n2];
		total = total - n2 * rank2;
		long n3 = total;
		String c3 = tokenCharacterArray[(int) n3];
		return c1 + c2 + c3;
		}
	
	public static String getModelDescription(Model model,String namingRule)
		{
		StringBuilder result = new StringBuilder();
		int length = namingRule.length();
		int fieldStart = -1;
		for(int i=0;i<length;i++)
			{
			char c1 = namingRule.charAt(i);
			if(c1=='$')
				{
				i++;
				char c2 = namingRule.charAt(i);
				if(c2=='{')
					{
					fieldStart=(++i);
					}
				else 
					{
					result.append(c1);
					}
				}
			else if(c1=='}')
				{
				if(fieldStart!=-1)
					{
					String fieldName = namingRule.substring(fieldStart,i);
					result.append(model.get(fieldName));
					fieldStart = -1;
					}
				else
					{
					result.append(c1);
					}
				}
			else 
				{
				if(fieldStart==-1)
					{
					result.append(c1);
					}
				}
			}
		return result.toString();
		}

	}
