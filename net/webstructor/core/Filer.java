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
package net.webstructor.core;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;

import net.webstructor.al.AL;
import net.webstructor.cat.TextFileReader;

//TODO: make this more smart and universal 
public class Filer {
	protected Environment env;
	public Filer (Environment env){
		this.env = env;
	}
	/*
	private File getPath(Seq cond){
		if (cond.size() != 2 || !(cond.get(0) instanceof String) || !(cond.get(1) instanceof String))
			return null;
		File path = new File( ((String)cond.get(0)) + "/" + ((String)cond.get(1)) );
		return path.exists()? path : null;
	}
	Collection get(Seq query){
		if (AL.empty(query))
			return null;
		if (query.size() != 2)
			return null;
		if (!(query.get(0) instanceof Seq) || !(query.get(1) instanceof String))
			return null;
		File path = getPath((Seq)query.get(0));
		if (path == null || !path.isDirectory())
			return null;
		File[] files = path.listFiles();
		if (AL.empty(files))
			return null;
		ArrayList result = new ArrayList(files.length);
		for (int i = 0; i < files.length; i++){
			if (files[i].isDirectory()){
				Thing t = new Thing();
			}
		}
		return null;
	}
	*/
	
	private static final int MAX_FILE_NAME_LENGTH = 255;
	
	String path(String[] path,boolean includeFilePart){
		if (AL.empty(path))
			return null;
		StringBuilder b = new StringBuilder();
		for (int i = 0; i < path.length; i++){
			if (!includeFilePart && i == (path.length - 1))
				break;
			if (b.length() != 0)
				b.append('/');
			String part = path[i].length() <= MAX_FILE_NAME_LENGTH ? path[i] : path[i].substring(0, MAX_FILE_NAME_LENGTH);
			b.append(part);
		}
		return b.toString();
	}

	/**
	 * Gets string data into the file on the path.
	 * @param path
	 * @param data
	 */
	public void put(String[] path,String data,Date time){
		String fileName = path(path,true);
		if (AL.empty(fileName))
			return;
		try {
			String dirName = path(path,false);
			if (!AL.empty(dirName)){
				File dir = new File(dirName);
				if (!dir.exists())
					dir.mkdirs();
			}
			File file = new File(fileName);
			file.createNewFile();
			BufferedWriter wr = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));
			if (!AL.empty(data))
				wr.write(data);
			wr.close();
			if (time != null)
				file.setLastModified(time.getTime());
		} catch (IOException e) {
			//e.g. long path
			//http://stackoverflow.com/questions/14484368/how-do-i-detemine-the-max-path-length-allowed-when-creating-a-file-in-java
			env.error("Filer putting", e);
		}
	}
	
	/**
	 * Gets string data from the file on the path.
	 * @param path
	 * @return string data in terminal fiel on the end of the path
	 */
	public String get(String[] path, boolean data){
		String fileName = path(path,true);
		if (AL.empty(fileName))
			return null;
		File file = new File(fileName);
		if (file.exists() && file.isFile()){
			if (!data)
				return "";
			if (!file.canRead())
				return null;
			TextFileReader reader = new TextFileReader();
			try {
				return reader.readDocData(fileName);
			} catch (IOException e) {
				//e.g. long path
				//http://stackoverflow.com/questions/14484368/how-do-i-detemine-the-max-path-length-allowed-when-creating-a-file-in-java
				env.error("Filer getting", e);
			}
		}
		return null;
	}
	
	public String[] latest(String[] path){
		String fileName = path(path,true);
		if (AL.empty(fileName))
			return null;
		File file = new File(fileName);
		if (file.exists() && file.isDirectory()){
			//TODO: iterate all files and found collection of the latest ones and return it
			File[] files = file.listFiles();
			long last = 0;
			ArrayList latest = new ArrayList();
			for (int i = 0; i < files.length; i++){
				long time = files[i].lastModified();
				if (last < time){
					latest.clear();
					last = time;
					latest.add(files[i].getName());
				} else
				if (last == time){
					latest.add(files[i].getName());
				}//else ignore
			}
			return (String[])latest.toArray(new String[]{});
		}
		return null;
	}
	
	public void del(String rootDirName){ 
		File file = new File(rootDirName);
		del(file);
	}
	
	public boolean del(File dir){ 
		if (dir.isDirectory()) { 
			String[] children = dir.list(); 
			for (int i=0; i<children.length; i++)
				del(new File(dir, children[i])); 
	  	}  
	  	return dir.delete(); 
	} 

	public Object load(String path){
        try {
        	File f = new File(path);
        	if(!f.exists() || f.isDirectory())
        	    return null;
        	FileInputStream fi = new FileInputStream(path);
            ObjectInputStream in = new ObjectInputStream(fi);
            Object o = in.readObject();
            in.close();
            fi.close();
            return o;
        }
        catch (Exception e) {
            env.error("Can't deserialize from path "+path, e);
            return null;
        }
	}

	public BufferedWriter openWriter(String path, boolean append, String status) {
		if (AL.empty(path))
			return null;
		File parent = new File(path).getParentFile();
		if (parent != null && !parent.exists())
			parent.mkdirs();
		try {
			return new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path,true), "UTF-8"));
		} catch (Exception e) {
			env.error(status, e);
			return null;
		}
	}
	
	public PrintStream openStream(String path, boolean append, String status) {
		if (AL.empty(path))
			return null;
		File parent = new File(path).getParentFile();
		if (parent != null && !parent.exists())
			parent.mkdirs();
		try {
		    BufferedOutputStream bostream = new BufferedOutputStream(new FileOutputStream(path,true));
		    return new PrintStream(bostream,true,"UTF-8");
		} catch (Exception e) {
			env.error(status, e);
			return null;
		}
	}
	
	public void save(String path, Serializable o){
		try {
			File parent = new File(path).getParentFile();
			if (parent != null && !parent.exists())
				parent.mkdirs();
			FileOutputStream fi = new FileOutputStream(path);
	        ObjectOutputStream out = new ObjectOutputStream(fi);
	        out.writeObject(o);
	        out.close();
	        fi.close();
		} catch (IOException e) {
            env.error("Can't serialize to path "+path, e);
	 	}
	}
	
}


