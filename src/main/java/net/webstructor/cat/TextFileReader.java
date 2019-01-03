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
package net.webstructor.cat;

import java.io.File;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.io.BufferedReader;
import java.io.IOException;

public class TextFileReader implements Reader
{
		public TextFileReader()
		{
        }

        public boolean canReadDoc(String docName)
        {
            // 200 - hardcoded for MAXPATH
            if (docName!=null && docName.length()<200 && docName.toLowerCase().indexOf("http://")==-1)
            {
                try {
                	File fi = new File(docName);
                    return fi.exists() && fi.isFile();
                } catch (Exception e) {
                    if (e!=null) // fool compiler
                        return false;
                }
            }
            return false;
        }

        public String readDocData(String docName) throws IOException
        {
        	/*
            StreamReader sr = 
                new StreamReader(
                new BufferedStream(
                new FileStream(docName,FileMode.Open,FileAccess.Read)
                ));
            String data = sr.ReadToEnd();
            sr.Close();
            return data;
            */
        	StringBuffer sb = new StringBuffer();
            BufferedReader br = null;
    		try {
    			String sCurrentLine;
    			br = new BufferedReader(new InputStreamReader(new FileInputStream(docName), "UTF8"));
    			//br = new BufferedReader(new FileReader(docName));
    			while ((sCurrentLine = br.readLine()) != null) {
    				sb.append(sCurrentLine).append('\n');//so the file can be tokenized by lines next
    			}
    		} catch (IOException e) {
    			e.printStackTrace();
                throw new IOException ("Reading " + docName + ": "+e.toString());
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
