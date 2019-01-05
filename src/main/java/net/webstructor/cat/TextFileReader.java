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
package net.webstructor.cat;

import java.io.File;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.io.BufferedReader;
import java.io.IOException;

import net.webstructor.core.Environment;

public class TextFileReader implements Reader
{
		protected Environment env = null;
		
		public TextFileReader()
		{
        }

		public TextFileReader(Environment env)
		{
			this.env = env;
        }

        public boolean canReadDoc(String docName)
        {
            // 200 - hardcoded for MAXPATH
            if (docName!=null && docName.length()<200 && docName.toLowerCase().indexOf("http://")==-1)
            {
                try {
                	File fi = new File(docName);
                    return fi.exists() && fi.isFile();
                } catch (Throwable e) {
                	if (env != null)
                		env.error("TextFileReader can not read "+docName, e);
                    if (e!=null) // fool compiler
                        return false;
                }
            }
            return false;
        }

        public String readDocData(String docName) throws IOException
        {
        	StringBuilder sb = new StringBuilder();
            BufferedReader br = null;
    		try {
    			String sCurrentLine;
    			br = new BufferedReader(new InputStreamReader(new FileInputStream(docName), "UTF8"));
    			//br = new BufferedReader(new FileReader(docName));
    			while ((sCurrentLine = br.readLine()) != null) {
    				sb.append(sCurrentLine).append('\n');//so the file can be tokenized by lines next
    			}
    		} catch (Throwable e) {
            	if (env != null)
            		env.error("TextFileReader can not read data "+docName, e);
    			else {
    				e.printStackTrace();
    				throw new IOException ("Reading " + docName + ": "+e.toString());
    			}
    		} finally {
    			try {
    				if (br != null)br.close();
    			} catch (IOException ex) {
    				ex.printStackTrace();
    			}
    		}
    		return sb.toString();
        }

}
