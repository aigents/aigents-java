/*
 * MIT License
 * 
 * Copyright (c) 2005-2019 by Anton Kolonin, Aigents
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
import java.nio.file.Files;
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
		File file = null;
		try {
			String dirName = path(path,false);
			if (!AL.empty(dirName)){
				File dir = env.getFile(dirName);
				if (!dir.exists())
					dir.mkdirs();
			}
			file = env.getFile(fileName);//new File(fileName);
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
			env.error("Filer putting "+fileName+" as "+file, e);
		}
	}
	
	/**
	 * Gets string data from the file on the path.
	 * @param path
	 * @return string data in terminal fiel on the end of the path
	 */
	public File getFile(String[] path){
		String fileName = path(path,true);
		if (AL.empty(fileName))
			return null;
		return env.getFile(fileName);
	}
	public String get(String[] path, boolean data){
		File file = getFile(path);
		if (file.exists() && file.isFile()){
			if (!data)
				return "";
			if (!file.canRead())
				return null;
			TextFileReader reader = new TextFileReader(env);
			try {
				//return reader.readDocData(fileName);
				return reader.readDocData(file.getPath());
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
		File file = env.getFile(fileName);
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
	
	public void del(String rootDirName, Date before){
		File file = env.getFile(rootDirName);
		del(file,before);
	}
	
	public void del(String rootDirName){ 
		del(rootDirName, null);
	}
	
	public boolean del(File file,Date before){
		long time = before == null ? Long.MAX_VALUE : before.getTime();
		if (file.isDirectory()) { 
			String[] children = file.list(); 
			for (int i=0; i<children.length; i++)
				del(new File(file, children[i]),before); 
	  	}
		//if old enough AND either is a file OR has all contents deleted earlier
		if (file.lastModified() < time && (!file.isDirectory() || file.list().length == 0)){
			//dir.delete();
			//https://stackoverflow.com/questions/46121457/file-delete-returning-false-even-file-is-writable
			try {
				if (file.exists())
					Files.delete(file.toPath());
				return true;
			} catch (IOException e) {
				env.error("Filer can not delete "+file.getAbsolutePath(), e);
				return false;
			}
		}else
			return false;
	} 

	public Object load(String path){
        try {
        	File f = env.getFile(path);
        	if(!f.exists() || f.isDirectory())
        	    return null;
        	FileInputStream fi = new FileInputStream(f.getPath());
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
		File parent = env.getFile(path).getParentFile();
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
		File parent = env.getFile(path).getParentFile();
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
			File file = env.getFile(path);
			File parent = file.getParentFile();
			if (parent != null && !parent.exists())
				parent.mkdirs();
			FileOutputStream fi = new FileOutputStream(file.getPath());
	        ObjectOutputStream out = new ObjectOutputStream(fi);
	        out.writeObject(o);
	        out.close();
	        fi.close();
		} catch (Exception e) {
            env.error("Filer can not serialize to path "+path, e);
	 	}
	}
	
	public static final int STORAGE_SPACE_BYTES_MIN = 500000000;//0.5G
	public static final int STORAGE_SPACE_RATIO_MIN = 5;//5%
	
	public static boolean isEnoughRoom(String path){
		File f = new File(AL.empty(path) ? "." : path);
		long free = f.getUsableSpace();
		if (free == 0)//if does not work (Android!?), try other way
			free = f.getFreeSpace();
		long total = f.getTotalSpace();
		if (total == 0)//if does not work, ignore
			return true;
		long ratio = Math.round(((double)free)/total * 100);
		if (free < STORAGE_SPACE_BYTES_MIN || ratio < STORAGE_SPACE_RATIO_MIN)
			return false;
		return true;
	}
	
}


