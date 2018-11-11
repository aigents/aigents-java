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
package net.webstructor.data;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import net.webstructor.al.AL;
import net.webstructor.core.Environment;
import net.webstructor.core.Filer;

public class DataLogger extends Filer {
	protected final static char separator = '\t'; 
	protected String path = null;
	protected BufferedWriter writer = null;
	protected String context = null;
	
	public interface StringConsumer {
		boolean read(String text);
	}
	
	public DataLogger(Environment env,String context) {
		super(env);
		this.context = context;
	}

	void open(String path) throws IOException{
		if (AL.empty(path))
			return;
		boolean same = this.path != null && this.path.equals(path);
		if (this.path != null && !same)
			close();
		if (!same){
			writer = openWriter(path,true,context+" opening log");
			this.path = path;
		}
	}
	
	public boolean load(String path, StringConsumer consumer){
		if (AL.empty(path) || !(new File(path)).exists())
			return false;
		try {
			//TODO: HTTP reader option
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(path), "UTF8"));
            for (;;){
            	String line = br.readLine();
            	if (AL.empty(line) || !consumer.read(line))
            		break;
            }
            br.close();
            return true;
		} catch (Exception e) {
			env.error(context + " error", e);
			return false;
		}
	}
	
	public void write(String path,Object[] data){
		if (AL.empty(path) || AL.empty(data))
			return;
		try {
			open(path);
			if (writer == null)
				return;
			for (int i = 0; i < data.length; i++){
				if (i > 0)
					writer.append(separator);
				writer.append(data[i] != null ? data[i].toString() : "");
			}
			writer.append('\n');
		} catch (Exception e) {
			env.error(context+" writing log", e);
		}
	}
	
	public void close() throws IOException{
		if (writer != null)
			writer.close();
		writer = null;
		path = null;
	}
}
