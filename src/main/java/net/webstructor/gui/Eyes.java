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
package net.webstructor.gui;

import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JLabel;

//TODO:more intelligent behavior
public class Eyes extends Thread {

	public final static int DOWN = 4;
	public final static int LEFT = 5;
	public final static int RIGHT = 7;
	
    static JLabel label;
    static ImageIcon[] icons = new ImageIcon[9];
    //static long delays[] = {5000,200,5000,200,5000,200,300,300,300,300,200};
 
	private Integer viewpoint = new Integer(0);
    private long delay = 10000;//delays[0];
    
    public Eyes() {
		try {
			/**/
			Eyes.icons[0] = Eyes.icons[8] = new ImageIcon(ImageIO.read(Tabs.class.getResource("/eyes_normal.png")));
			Eyes.icons[1] = new ImageIcon(ImageIO.read(Tabs.class.getResource("/eyes_small.png")));
			Eyes.icons[2] = new ImageIcon(ImageIO.read(Tabs.class.getResource("/eyes_sleep.png")));
			Eyes.icons[3] = new ImageIcon(ImageIO.read(Tabs.class.getResource("/eyes_large.png")));
			Eyes.icons[4] = new ImageIcon(ImageIO.read(Tabs.class.getResource("/eyes_down.png")));
			Eyes.icons[5] = new ImageIcon(ImageIO.read(Tabs.class.getResource("/eyes_left.png")));
			Eyes.icons[6] = new ImageIcon(ImageIO.read(Tabs.class.getResource("/eyes_up.png")));
			Eyes.icons[7] = new ImageIcon(ImageIO.read(Tabs.class.getResource("/eyes_right.png")));	
			/*	
			Eyes.icons[0] = new ImageIcon(ImageIO.read(Tabs.class.getResource("/eyes_normal.png")));
			Eyes.icons[1] = Eyes.icons[3] = Eyes.icons[5] = 
			Eyes.icons[10] = new ImageIcon(ImageIO.read(Tabs.class.getResource("/eyes_sleep.png")));
			Eyes.icons[2] = new ImageIcon(ImageIO.read(Tabs.class.getResource("/eyes_small.png")));
			Eyes.icons[4] = new ImageIcon(ImageIO.read(Tabs.class.getResource("/eyes_large.png")));			
			Eyes.icons[6] = new ImageIcon(ImageIO.read(Tabs.class.getResource("/eyes_down.png")));
			Eyes.icons[7] = new ImageIcon(ImageIO.read(Tabs.class.getResource("/eyes_left.png")));
			Eyes.icons[8] = new ImageIcon(ImageIO.read(Tabs.class.getResource("/eyes_up.png")));
			Eyes.icons[9] = new ImageIcon(ImageIO.read(Tabs.class.getResource("/eyes_right.png")));	
			*/
		} catch (IOException e) {
			//TODO: what?
		} 
    }
        
	public void run() {
		for (;;) {
			try {
				sleep(delay);
			} catch (InterruptedException e) {
				//TODO: what?
			}
			synchronized (viewpoint) {
				int iplus = viewpoint.intValue() + 1;
				if (iplus >= icons.length)
					iplus = 0;
				label.setIcon(icons[iplus]);
				//delay = delays[iplus];
				viewpoint = new Integer(iplus);
			}
		}
	}
	
	public void point(int set) {
		if (0 <= set && set < icons.length)
			synchronized (viewpoint) {
				viewpoint = new Integer(set);
				label.setIcon(icons[set]);
			}
	}
}

