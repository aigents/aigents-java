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
package net.webstructor.cat;

import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

//TODO: eliminate if not needed
public class HttpFileContext {
	protected String content_type = null;
	protected String content_encoding = null;
	protected String charset = null;
	//TODO: nicer API?
	public String data = null;
	public String text = null;
	public Date time = null;
	public ArrayList links = null;
	public Map images = null;
	public Map linkPositions = null;
	public Map titles = null;
	
	public void trash() {//help GC
		content_type = null;
		content_encoding = null;
		charset = null;
		data = null;
		text = null;
		time = null;
		links = null;
		images = null;
		linkPositions = null;
		titles = null;
	}
}
