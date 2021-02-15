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

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JFrame;

import net.webstructor.util.Array;

@SuppressWarnings("serial")
class GamePad1 extends Canvas {
	private Game game = null;
	private JFrame frame;
	
	GamePad1(int w, int h) {
        frame = new JFrame("GamePad");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(w, h);
        frame.getContentPane().add(this);
        frame.pack();
        frame.setVisible(true);
    }

	void setGame(Game game) {
		this.game = game;
		frame.setTitle(game.getTitle());
	}
	
    public void paint(Graphics g) {
    	if (game != null)
    		game.render(g);
    }
}


@SuppressWarnings("serial")
class Diags extends Canvas {
	//static Color colors[] = {Color.BLUE,Color.CYAN,Color.GREEN,Color.MAGENTA,Color.ORANGE,Color.PINK,Color.RED};
	
	int frames = 0;
	int frames_max = 0;
	
	List<String> params = null;
	HashMap<String,ArrayList<Integer>> data = null;
	Map<String,int[]> ranges = new HashMap<String,int[]>();
	Map<String,Color> legend = new HashMap<String,Color>();
	
	Diags(int w){
		frames_max = w;
	}
	
    void init(List<String> params,List<String> colors) {
    	frames = 0;
    	this.params = params;
    	data = new HashMap<String,ArrayList<Integer>>();
    	ranges.clear();
    	legend.clear();
    	int c = 0;
    	for (String param : params) {
    		data.put(param, new ArrayList<Integer>());
    		legend.put(param,color(colors.get(c++)));
    	}
    }
   
    public void update(State state) {
    	State.updateRanges(ranges, state);
    	//System.out.format("%d %d\n",frames,frames_max);
    	if (frames >= frames_max) {
    		frames = 0;
        	for (ArrayList<Integer> l : data.values())
        		l.clear();
    	}
    	frames++;
    	for (String p : data.keySet()) {
    		Integer v = state.value(p);
    		data.get(p).add(v == null ? 0 : v);
    	}
    	//System.out.println(state);
		//System.out.format("%d\t%d\n",state.value("Expectedness"),state.value("Novelty"));
    }

    public void paint(Graphics g) {
    	if (data == null)
    		return;
		Rectangle bounds = g.getClipBounds();
    	frames_max = bounds.width;
    	int i = 0;//band number
    	int h = bounds.height / data.keySet().size();//band width
    	g.setColor(Color.BLACK);
    	g.drawLine(0, 0, bounds.width, 0);
    	if (data != null && frames > 0) {
    		for (String param : params) {
	    		int y = (i + 1) * h;
		    	g.setColor(Color.BLACK);
			    g.drawString(param, 1, y - 2);
		    	g.drawLine(0, y, bounds.width, y);
			    renderBand(g, param, legend.get(param), y-1, h-2);
			    i++;
    		}
    	}
   	}
    
    void renderBand(Graphics g, String param, Color color, int y, int h) {
		ArrayList<Integer> l = data.get(param);
		int range[] = ranges.get(param);
    	if (l != null && range != null) {
        	int y1 = 0, y2;
        	int dx = 1;
        	g.setColor(color);
    		int r = range[1] != range[0] ? range[1] - range[0] : 1;
     		for (int i = 0; i < frames; i++) {
    			y2 = y - l.get(i) * h / r;
    			if (i > 0) {
    				//System.out.format("%d %d %d %d     %d %d %d\n",i-1, y1, i, y2, l.get(i), h , r);
    				g.drawLine((i-1)*dx, y1, i*dx, y2);
    			}
    			y1 = y2;
    		}
    	}
    }
    
    Color color(String color) {
	    try {
	        Field field = Class.forName("java.awt.Color").getField(color);
	        return (Color)field.get(null);
	    } catch (Exception e) {
	        return Color.GRAY;
	    }
	}
}


@SuppressWarnings("serial")
public class GamePad extends Canvas {
	private Game game = null;
	private JFrame frame;
	protected Diags diags;
	
	GamePad(int w, int h) {
        frame = new JFrame("GamePad");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setPreferredSize(new Dimension(w, h*2));
        frame.setSize(w, h*2);
        frame.getContentPane().setLayout(new GridLayout(2,1));
        
        setSize(w, h);
        frame.getContentPane().add(this, BorderLayout.NORTH);
        
        diags = new Diags(w);
        diags.setSize(w,h);
        diags.setSize(w,h);

        frame.getContentPane().add(diags, BorderLayout.SOUTH);
        
        frame.pack();
        frame.setVisible(true);
    }

	void init(List<String> params,List<String> colors) {
		diags.init(params,colors);
	}
	
	void update(State s) {
		diags.update(s);
	}
	
	void setGame(Game game) {
		this.game = game;
		frame.setTitle(game.getTitle());
	}
	
    public void paint(Graphics g) {
    	if (game != null)
    		game.render(g);
    }

    public void repaint() {
        super.repaint();
        diags.repaint();
    }

    public static void main(String args[]) {
    	GamePad gp = new GamePad(400,200);
    	gp.init(Array.toList(new String[] {"a","b"}),Array.toList(new String[] {"red","green"}));
		State x = new State();
		int a = 0, b = 0;
		gp.repaint();
    	for (int i = 0; i < 600; i++) {
    		x.set("a", a);
    		x.set("b", b);
    		a += 1;
    		//b += 1;
    		b = i*i;
    		gp.update(x);
    		gp.repaint();
    		try {Thread.sleep(10);} catch (InterruptedException e) {e.printStackTrace();}
    	}
    }
}


