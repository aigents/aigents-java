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

//TODO: consider interface and Entity at some point!?
public abstract class Anything { // implements ORObject

	public String[] getNamesAvailable() {
		return null;
	}
	
	public String[] getNamesPossible() {
		return null;
	}
	
	public boolean del() {
		return false;
	}

	public boolean act(String name, Anything argument) {
		return false;
	}
	
	public String   getString(String[] names) {
		String name;
		for (int i=0;i<names.length;i++)
			if ((name = getString(names[i])) != null)
				return name;
		return null;
	}

	public final String getString(String name,String bydefault) {
		String value = getString(name);
		if (value != null)
			return value; 
		setString(name, bydefault);
		return bydefault; 
	}

	//TODO: remove but deal with Property then!
	public String getString(String name) {		
		return null;
	}
	
	//TODO: remove but deal with Property then!
	public Anything setString(String name,String value) {
		//TODO: throw?
		return this;
	}

	public Anything set(String name,Object value) {
		//TODO: throw?
		return this;
	}

}
