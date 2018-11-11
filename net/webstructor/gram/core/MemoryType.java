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
package net.webstructor.gram.core;

/** 
 * Describes memory types of the items. 
 * Each item may have multiple memory types being  
 * represented say as STRING and LINK and RELATIONSHIP at the same time. 
 */
public interface MemoryType 
{
	/* Unary items identified by char */
	public static final int CHARACTER = 1;  
	/* N-ary items identified by String (referring to N items identified by char) */
	public static final int STRING = 2; 
	/* Binary link items identified by long (OR'ed integer ids with 32-bit shift of the first) */
	public static final int LINK = 4; 
	/* N-ary relationship items identified by int[N] array (referring to N items identified by integer ids) */
	public static final int RELATIONSHIP = 8; 


    public static final int UNDEFINED = -2147483648;
}
