package com.freescale.stateStamper.db;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class GsonHelper
	{
	private static GsonBuilder gsonBuilder = null;
	private static Gson gson = null;
	
	private static void init()
		{
		gsonBuilder = new GsonBuilder();
		gsonBuilder.serializeSpecialFloatingPointValues();
		gsonBuilder.enableComplexMapKeySerialization();
		gsonBuilder.serializeNulls();
		gsonBuilder.disableHtmlEscaping();
		gson = gsonBuilder.create();
		}

	public synchronized static <T> T fromJson(String json, Class<T> classOfT)
		{
		if(gson==null)init();
		return gson.fromJson(json, classOfT);
		}

	public synchronized static String toJson(Object obj)
		{
		if(gson==null)init();
		return gson.toJson(obj);
		}
	}