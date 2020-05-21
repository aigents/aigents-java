/*
 * MIT License
 * 
 * Copyright (c) 2005-2020 by Anton Kolonin, Aigents®
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

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Date;

import net.webstructor.util.Array;

//TODO: push to LTM/Filer not immediately, but either A) upon forgetting or B) after is confirmed?
public class Archiver {
	Environment env;
	Filer filer;
	public Archiver(Environment env){
		filer = new Filer(env);
	}
	//TODO: given configured long-term retention period
	public void clear(Date till){
		filer.del("is-text",till);
		filer.del("is-instances",till);
	}
	public void clear(){
		clear(null);
	}
	public void put(String source, String text){
		try {
			filer.put(new String[]{"is-text",URLEncoder.encode(source,"UTF8")},text,null);
		} catch (UnsupportedEncodingException e) {
			env.error("Archiver encoding "+source, e);
		}
	}
	public String get(String source){
		try {
			return filer.get(new String[]{"is-text",URLEncoder.encode(source,"UTF8")},true);
		} catch (UnsupportedEncodingException e) {
			env.error("Archiver encoding "+source, e);
		}
		return null;
	}
	/*
	public void put(String thing,String instance,String source){
		try {
			thing = URLEncoder.encode(thing,"UTF8");
			instance = URLEncoder.encode(instance,"UTF8");
			if (AL.empty(source))
				filer.put(new String[]{"is-instances",thing,instance},null);
			else {
				source = URLEncoder.encode(source,"UTF8");
				filer.put(new String[]{"is-instances",thing,instance,source},null);
			}
		} catch (UnsupportedEncodingException e) {
			env.error("Archiver encoding "+thing+" "+instance+" ("+source+")", e);
		}
	}
	public String get(String thing,String instance,String source){
		try {
			thing = URLEncoder.encode(thing,"UTF8");
			instance = URLEncoder.encode(instance,"UTF8");
			if (AL.empty(source))
				return filer.get(new String[]{"is-instances",thing,instance},false);
			else {
				source = URLEncoder.encode(source,"UTF8");
				return filer.get(new String[]{"is-instances",thing,instance,source},false);
			}
		} catch (UnsupportedEncodingException e) {
			env.error("Archiver encoding "+thing+" "+instance+" ("+source+")", e);
		}
		return null;
	}
	*/
	public void update(String thing,String instance,Date time){
		try {
			thing = URLEncoder.encode(thing,"UTF8");
			instance = URLEncoder.encode(instance,"UTF8");
			filer.put(new String[]{"is-instances",thing,instance},null,time);
		} catch (UnsupportedEncodingException e) {
			env.error("Archiver encoding "+thing+" "+instance, e);
		}
	}
	public static String[] path(String dir, String file) throws UnsupportedEncodingException {
		return new String[] {URLEncoder.encode(dir,"UTF8"),URLEncoder.encode(file,"UTF8")};
	}
	public static String[] path(String dir, String subdir, String file) throws UnsupportedEncodingException {
		return new String[] {URLEncoder.encode(dir,"UTF8"),URLEncoder.encode(subdir,"UTF8"),URLEncoder.encode(file,"UTF8")};
	}
	public boolean exists(String thing,String instance,Date date){
		try {
			if (date == null) {
				String[] latest = filer.latest(path("is-instances",thing));
				return Array.contains(latest, URLEncoder.encode(instance,"UTF8")); 
			}else {
				String[] path = path("is-instances",thing,instance);
				File file = filer.getFile(path);
				//assume exists if present and has the same or older date!!! 
				return file != null && (date == null || file.lastModified() >= date.getTime()) ? true : false;
			}
		} catch (UnsupportedEncodingException e) {
			env.error("Archiver encoding "+thing+" "+instance, e);
			return false;
		}
	}
}
