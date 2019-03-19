/*
 * MIT License
 * 
 * Copyright (c) 2018-2019 by Anton Kolonin, Aigents
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

import java.io.Serializable;
import java.math.BigDecimal;

public class ComplexNumber implements Serializable {
	private static final long serialVersionUID = 3960679018613833669L;
	public Number a;
	public Number b;
	public ComplexNumber(double a, double b){
		this.a = new BigDecimal(a);
		this.b = new BigDecimal(b);
	}
	public ComplexNumber(double a){
		this.a = new BigDecimal(a);
		this.b = null;
	}
	public ComplexNumber(Number a, Number b){
		this.a = a;
		this.b = b;
	}
	public ComplexNumber(Number a){
		this.a = a;
		this.b = null;
	}
	public static Number toNumber(ComplexNumber[] a){
		double sum = 0;
		for (int i = 0; i < a.length; i++){
			ComplexNumber cn = a[i];
			if (cn.a != null && cn.b != null)
				sum += cn.a.doubleValue() * cn.b.doubleValue();
			else if (cn.a != null)
				sum += cn.a.doubleValue();
		}
		return new Double(sum);
	}
	public static ComplexNumber[] add(ComplexNumber[] a, ComplexNumber[] b){
		ComplexNumber[] newa = new ComplexNumber[a.length + b.length];
		System.arraycopy(a, 0, newa, 0, a.length);
		if (b.length == 1)
			newa[a.length] = b[0];
		else
			System.arraycopy(b, 0, newa, a.length, b.length);
		return newa;
	}
}
