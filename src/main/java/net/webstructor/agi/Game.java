/*
 * MIT License
 * 
 * Copyright (c) 2005-2021 by Anton Kolonin, Aigents®
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
package net.webstructor.agi;

import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.awt.Graphics;

abstract class Game {
    static Random random = new Random(0);
    //static Random random = ThreadLocalRandom.current();
	abstract void init();
	abstract State next(Integer action);
	abstract void printHeader();
	abstract void printState();
	abstract void printFooter();
	abstract void render(Graphics g);
	abstract String getTitle();
	abstract String toString(State s);
	abstract Set<Integer> domain(String key);
	public static int random(int[] states) {
	    return states[random.nextInt(states.length)];
	}
	public static int random(Integer[] states) {
	    return states[random.nextInt(states.length)];
	}
	public static int random(int min, int max) {
		int arg = max - min + 1;
	    int rand = random.nextInt(arg) + min;
	    return rand;
	}
}

