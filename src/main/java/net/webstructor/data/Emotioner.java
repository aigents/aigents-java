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
package net.webstructor.data;

class Emoticon {
	String[] faces;
}

//https://apps.timwhitlock.info/emoji/tables/unicode
//https://unicode.org/emoji/charts/full-emoji-list.html#1f642
//https://lemire.me/blog/2018/06/15/emojis-java-and-strings/
//https://www.branah.com/unicode-converter
public class Emotioner {
	public static final String positive = "\ud83d\ude0a";//😊0001f60a//smiling face with smiling eyes
	public static final String negative = "\ud83d\ude1e";//😞0001f61e//disappointed face
	public static final String flushed = "\ud83d\ude33";//😳0001F633//flushed face
	public static final String emotion(int s) {//sentiment
		return s < -50 ? Emotioner.negative : s > 50 ? Emotioner.positive : "";
	}
	public static final String emotion(int[] s) {//sentiment
		return emotion(s[2]);
	}
	public static void main(String args[]){
		String x = positive;
		System.out.println(x);
		System.out.println(x.length());
		char c = x.charAt(0);
		System.out.format("%05X\n", (int)c);
	}
}
