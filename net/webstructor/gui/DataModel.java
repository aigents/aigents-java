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

import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.util.Arrays;
import java.util.Comparator;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import net.webstructor.al.AL;
import net.webstructor.al.Parser;
import net.webstructor.al.Time;
import net.webstructor.al.Writer;

class StringArrayComparator implements Comparator {
	public int compare(Object o1, Object o2) {
		//TODO: validation
		try {
			Object[] a1 = (Object[])o1;
			Object[] a2 = (Object[])o2;
			for (int i = 0; i < a1.length && i < a2.length; i++) {
				if (a1[i] instanceof String && a2[i] instanceof String) {
					int cmp = ((String)a1[i]).compareTo((String)a2[i]);
					if (cmp != 0)
						return cmp;
				}
			}
		} catch (Exception e) {} //oops, relax
		return 0;
	}
	public boolean equals(Object obj) {		
		return obj instanceof StringArrayComparator;
	}
}

class NewsComparator implements Comparator {
	public int compare(Object o1, Object o2) {
		try {
			//data:trust,date,sources,text
			Object[] a = (Object[])o1;
			Object[] b = (Object[])o2;
			int time = Time.compare((String)a[1],(String)b[1]);
			if (time != 0) {//by time
				return time;
			}
			int src = ((String)a[2]).compareTo((String)b[2]);//by source
			if (src != 0)
				return src;
			return ((String)a[3]).compareTo((String)b[3]);
		} catch (Exception e) {} //oops, relax
		return 0;
	}
	public boolean equals(Object obj) {		
		return obj instanceof NewsComparator;
	}
}

abstract class DataModel extends AbstractTableModel {
	private static final long serialVersionUID = 8989040393420931458L;

	abstract String type();

	private String[] columnNames = {"Name"};//,"Check"};
    protected Object[][] data = {};
	protected JTable table = null;

    public void init(JTable table, JScrollPane scrollPane) {    	
    	final DataModel model = this;
    	this.table = table;
        table.setToolTipText("Double-click to edit thing or site, hold and release to open site.");
    	//table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    	if (type().equals(AL.knows) || type().equals(AL.sites)) {
    		scrollPane.setDropTarget(new DropTarget() {
				private static final long serialVersionUID = -329285062683640476L;
				public synchronized void drop(DropTargetDropEvent evt) {
    		        try {
    		            evt.acceptDrop(DnDConstants.ACTION_COPY);
    		            String str = (String)evt.getTransferable().getTransferData(DataFlavor.stringFlavor);
    		            String request = model.addRequest(str);
                		if (!AL.empty(request))
                			App.getApp().chatter.request(model,request);
    		        } catch (Exception ex) {
    					//TODO: what?
    		        }
    		    }
    		});
    	}
    }

	public void selected() {
		App.getApp().chatter.request(this, updateRequest());
	}
    
	public static boolean isLink(String value) {
		String text = value.toLowerCase();
		return text.startsWith("http://") || text.startsWith("https://");
	}
		
	String updateRequest() {
		return "What my "+type()+"?";	    
	}
	
	String addRequest(String str) {
		return AL.empty(str) ? null : "My "+type()+" "+Writer.toString(str)+".";	    
	}

	void clear() {
		data = new Object[0][];
	}
	
	void confirm() {
		//App.getApp().chatter.request(this);
		App.getApp().chatter.request(this,this.updateRequest());
	}
	
	String deleteRequest(Table table) {
		String[] names = table.selected(0);
		if (names.length > 0) {
			StringBuilder sb = new StringBuilder();
			sb.append("My ").append(type()).append(AL.space);
			for (int i=0; i<names.length; i++) {
				if (i > 0)
					sb.append(AL.lister[0]).append(AL.space);
//TODO: make insensitive to no/not/~ ?
				sb.append(AL.not[1]).append(AL.space);
				Writer.toString(sb,names[i]);        				
			}
			sb.append(AL.period);
//TODO: remove thing itself, not knows link only or do GC on given thing otherwise
			return sb.toString();
		}
		return null;
	}
	
	void update(String string) {
		String[] chunks = Parser.parse(string,AL.commas+AL.periods);
		data = new Object[chunks.length][2];
		for (int i=0; i<chunks.length; i++) {
			data[i][0] = (String)chunks[i];
			data[i][1] = new Boolean(false);
		}
		Arrays.sort(data,new StringArrayComparator());
		fireTableDataChanged();
	}
	
    protected String[] getColumnNames() {
        return columnNames;
    }
    
    public final int getColumnCount() {
        return getColumnNames().length;
    }

    public final String getColumnName(int col) {
        return getColumnNames()[col];
    }

    public int getRowCount() {
        return data.length;
    }

    public Object getValueAt(int row, int col) {
        return data[row][col];
    }

    public Object[] getRowAt(int row) {
        return data[row];
    }

    public Class getColumnClass(int c) {
        return getColumnName(c).toLowerCase().equals(AL.trust) ? Boolean.class : String.class;
    }

    public boolean isCellEditable(int row, int col) {
        return true;
    }
    
	String editRequest(int row, int col, String oldValue, String newValue) {
		StringBuilder sb = new StringBuilder();
		sb.append("My ").append(type()).append(AL.space);
		sb.append(AL.not[1]).append(AL.space);
		Writer.toString(sb,oldValue);        				
		sb.append(AL.lister[0]).append(AL.space);
		Writer.toString(sb,newValue);
		sb.append(AL.period);
        return sb.toString();
	}
    
    public void setValueAt(Object value, int row, int col) {
    	//if (col == 0 && !(value).equals(data[row][col])) {
    	if (!(value).equals(data[row][col])) {
    		String oldVal = data[row][col] instanceof Boolean ? data[row][col].toString() : (String)data[row][col];
    		String newVal = value instanceof Boolean ? value.toString() : (String)value;
     		App.getApp().chatter.request(this,editRequest(row,col,oldVal,newVal));
    	}
        data[row][col] = value;
        fireTableCellUpdated(row, col);
    }
    
    public void open(String value) {
    	if (value != null && DataModel.isLink(value)) {
    		App.open(value);
    		App.getApp().chatter.request(null,"You read site "+Writer.toString(value)+"!");
    	}
    }
    
    public void open(int row) {
    	open((String) getValueAt(row, 0));
    }

}
