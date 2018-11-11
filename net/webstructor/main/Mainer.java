/*
 * MIT License
 * 
 * Copyright (c) 2005-2018 by Anton Kolonin, Aigents
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
package net.webstructor.main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import net.webstructor.core.Environment;

public class Mainer implements Environment {

	private boolean debug = true; 
	
	public Mainer(boolean debug){
		this.debug = debug;
	}
	
	public Mainer(){
	}
	
	public void debug(String str) 
	{
		if (debug)
			println(str);
	}
	
	public void error(String str, Throwable e) {
		println(str);
		if (e != null)
			e.printStackTrace();
	}
	
	//https://stackoverflow.com/questions/9732439/can-a-java-program-detect-that-its-running-low-on-heap-space
	public int checkMemory(){
		long heapSize = Runtime.getRuntime().totalMemory();
		long heapMaxSize = Runtime.getRuntime().maxMemory();
		long heapFreeSize = Runtime.getRuntime().freeMemory();
		long remaining = Math.max(heapFreeSize, heapMaxSize - heapSize);
		return (int) Math.round(((double)(heapMaxSize-remaining))/heapMaxSize*100);
	}

	public static void println(String text) 
	{
		System.out.println(text);
	}

	public static void print(String text) 
	{
		System.out.print(text);
	}

    public static boolean setCurrentDirectory(String directoryName)
    {
        boolean result = false;
        File    directory = new File(directoryName).getAbsoluteFile();
        if (directory.exists() || directory.mkdirs())
        {
            result = (System.setProperty("user.dir", directory.getAbsolutePath()) != null);
        }
        return result;
    }
	
    public static BufferedReader getReader(String location)
    {
    	BufferedReader sr = null;
        // First, try to read data file from URL
        try {
            // http://www.csharp-station.com/HowTo/HttpWebFetch.aspx
            // prepare the web page we will be asking for
            //HttpWebRequest  request  = (HttpWebRequest)WebRequest.Create(location);
            // execute the request
            //HttpWebResponse response = (HttpWebResponse)request.GetResponse();
            // we will read data via the response stream
            //sr = new StreamReader(new BufferedStream(response.GetResponseStream()));
            URL url = new URL(location);
            URLConnection conn = url.openConnection();
            sr = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        } catch (Exception e) {
            if (e!=null) // fool compiler
            // Second, try to read it from the text file
            try {
    			//sr = new BufferedReader(new FileReader(location));
    			sr = new BufferedReader(new InputStreamReader(new FileInputStream(location), "UTF-8"));
            } catch (Exception e2) {
            	try {
            		int slash = location.lastIndexOf('/');
            		if (slash != -1) { 
            			String resource = location.substring(slash);
            			sr = new BufferedReader(new InputStreamReader(Mainer.class.getResourceAsStream(resource), "UTF-8"));
            		}
            	} catch (Exception e3) {
	            	if (e3!=null) // fool compiler
	                    return null;
				}
            }
        }
        return sr;
    }
}
