/*
 * MIT License
 * 
 * Copyright (c) 2005-2017 by Anton Kolonin, Aigents
 * Copyright (c) 2018 SingularityNET
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
package net.webstructor.main;

public class Tester {
	private int total = 0;
	private int failed = 0;
	
	public void init(){
		total = 0;
		failed = 0;
	}
	
	public void check(){
		System.out.println(failed > 0 ?
				"FAILED:"+failed+" of "+total :
				"PASSED:"+total+" of "+total	);
	}
	
	public void assume(String actual,String expected){
		total++;
		if (expected.equals(actual))
			System.out.println(actual);
		else{
			System.out.println("FAILED:"+actual+" != "+expected);
			failed++;
		}
	}
	
	public void assume(int actual,int expected){
		total++;
		if (expected == actual)
			System.out.println(actual);
		else{
			System.out.println("FAILED:"+actual+" != "+expected);
			failed++;
		}
	}
	
}
