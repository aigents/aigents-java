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

import net.webstructor.main.*; 

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

class CmdLine extends Mainer
{
		public static void main(String[] args)
		{
			/*
			boolean b1,b2,b3,b4,b5,b6;
			b1 = StringUtil.isWord("abcd");
			b2 = StringUtil.isWord("�����");
			b3 = StringUtil.isWord("ab-��");
			b4 = StringUtil.isWord("ab34cd");
			b5 = StringUtil.isWord("ab+c.d");
			b6 = StringUtil.isWord("789");
			*/
			
			if (args[0] != null)
				setCurrentDirectory(args[0]);
            
            // initialize the server explicitly
            CommandHandler.getServer(System.getProperty("user.dir"));

            System.out.println("Hello, user!");
            for (;;)
            {
            	String s = null;
            	
            	System.out.println("You:");
            	BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
                try {
                   s = br.readLine();
                } catch (IOException ioe) {
                   System.out.println("IO error!");
                   System.exit(1);
                }                
                
                System.out.println("WebCat:");

                CommandHandler ch = new CommandHandler(s,"\n",";",",");
                ch.run();
                System.out.println(ch.getOutput());

                if (s.indexOf("shutdown")!=-1)
                    break;
            }
		}
}
