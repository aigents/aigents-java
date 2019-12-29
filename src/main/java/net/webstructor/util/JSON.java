/*
 * MIT License
 * 
 * Copyright (c) 2005-2019 by Anton Kolonin, Aigents®
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package net.webstructor.util;

import java.util.Date;

import javax.json.JsonArray;
import javax.json.JsonObject;

public class JSON {
	
    //http://projects.fivethirtyeight.com/facebook-primary/
    public static String getJsonString(JsonObject data, String name, String def) {
		if (data.keySet().contains(name))
			return data.isNull(name) ? def : data.getString(name);
		else
			return def;
	}
	
    public static boolean getJsonBoolean(JsonObject data, String name, boolean def) {
		return data.keySet().contains(name) ? data.getBoolean(name) : def;
	}
	
    public static JsonArray getJsonArray(JsonObject data, String name) {
		return data.keySet().contains(name) ? data.getJsonArray(name) : null;
	}
    
    public static JsonObject getJsonObject(JsonObject data, String name) {
		return data.keySet().contains(name) ? data.getJsonObject(name) : null;
	}
    
    public static long getJsonLong(JsonObject data, String name, long def) {
		return data.keySet().contains(name) ? data.getJsonNumber(name).longValue() : def;
	}
	
	public static String getJsonString(JsonObject data, String name) {
		return getJsonString(data, name, null);
	}

    public static String getJsonNumberString(JsonObject data, String name) {
		return data.keySet().contains(name) ? data.getJsonNumber(name).toString() : "";
	}
	
    public static int getJsonInt(JsonObject data, String name) {
		return data.keySet().contains(name) ? data.getJsonNumber(name).intValue() : 0;
	}
	
    public static long getJsonLong(JsonObject data, String name) {
		//return data.keySet().contains(name) ? data.getJsonNumber(name).longValueExact() : 0;
		return data.keySet().contains(name) ? data.getJsonNumber(name).longValue() : 0;
	}
	
    public static Date getJsonDateFromUnixTime(JsonObject data, String name) {
		return new Date( data.keySet().contains(name) ? data.getJsonNumber(name).longValue() * 1000 : 0 );
	}
	
}
