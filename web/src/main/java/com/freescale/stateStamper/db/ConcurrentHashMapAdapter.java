package com.freescale.stateStamper.db;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class ConcurrentHashMapAdapter extends TypeAdapter<ConcurrentHashMap>
	{
	@Override
	public void write(JsonWriter writer, ConcurrentHashMap map) throws IOException
		{
		writer.beginObject();
		Set entrySet = map.entrySet();
		for (Entry entry : (Set<Entry>) entrySet)
			{
			Object key = entry.getKey();
			Object value = entry.getValue();
			writer.name((String) key);
			if (value instanceof List)
				{
				writer.beginArray();
				for (String tmp : (List<String>) value)
					{
					writer.value(tmp);
					}
				writer.endArray();
				}
			else
				{
				writer.value((String) value);
				}
			}
		writer.endObject();
		}

	@Override
	public ConcurrentHashMap read(JsonReader reader) throws IOException
		{
		ConcurrentHashMap map = new ConcurrentHashMap();
		JsonToken token = null;
		while (true)
			{
			token = reader.peek();
			if (token.equals(JsonToken.BEGIN_OBJECT))
				{
				reader.beginObject();
				}
			else if (token.equals(JsonToken.END_OBJECT))
				{
				reader.endObject();
				break;
				}
			else
				{
				if (token.equals(JsonToken.NAME))
					{
					String name = reader.nextName();
					token = reader.peek();
					if (token.equals(JsonToken.NULL))
						{
						reader.nextNull();
						map.put(name, "");
						}
					else if (token.equals(JsonToken.BEGIN_ARRAY))
						{
						reader.beginArray();
						List list = new ArrayList<String>();
						while (reader.hasNext())
							{
							JsonToken peek = reader.peek();
							if (peek.equals(JsonToken.NULL))
								{
								reader.nextNull();
								list.add("");
								}
							else
								{
								list.add(reader.nextString());
								}
							}
						reader.endArray();
						map.put(name, list);
						}
					else if (token.equals(JsonToken.STRING))
						{
						map.put(name, reader.nextString());
						}
					}
				}
			}
		return map;
		}
	}
