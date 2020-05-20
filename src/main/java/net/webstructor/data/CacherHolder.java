/*
 * MIT License
 * 
 * Copyright (c) 2018-2020 by Anton Kolonin, Aigents®
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

import net.webstructor.core.Environment;

import java.util.HashMap;

public class CacherHolder {
	/*
	registry
	- get
	- add
	- clear all
	- update all
	- compact all
	*/
	
	private Map cachers = new HashMap();
	
	protected Environment env = null;

	public CacherHolder(Environment env){
		this.env = env;
	}
	
	public Cacher get(String name){
		synchronized (cachers){
			Object o = cachers.get(name);
			return o instanceof Cacher ? (Cacher)o : null;
		}
	}

	public boolean put(String name, Cacher o){
		synchronized (cachers){
			//NOTE - control is on the upper layer, just overwrite here
			//if (cachers.containsKey(name))
			//	return false;
			cachers.put(name, o);
			return true;
		}
	}
	
	public void free(){
		Collection all;
		synchronized (cachers){
			all = new ArrayList(cachers.keySet());
		}
//TODO: do this by reverse-LRU, so the recently used ones are released in the last turn
		for (Object o : all) {
			env.debug("Cacher free "+o+", memory "+env.checkMemory());
			Cacher c;
			synchronized (cachers){
				c = (Cacher)cachers.get(o);
			}
			if (c == null)
				env.error("Cacher free "+o+" error",null);
			else
				c.free();
//TODO: use LOWER_MEMORY_THRESHOLD < UPPER_MEMORY_THRESHOLD
			//if (env.checkMemory() < GraphCacher.MEMORY_THRESHOLD)
				//break;
		}
	}
	
	public void clear(Date till){
		Collection all;
		synchronized (cachers){
			all = new ArrayList(cachers.values());
		}
		for (Object o : all)
			((Cacher)o).clear(false,till);
	}
}
