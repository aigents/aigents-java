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

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableModel;

import net.webstructor.al.AL;
import net.webstructor.al.Writer;
import net.webstructor.peer.Peer;
import net.webstructor.util.Array;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.HashMap;

class DataModelThings extends DataModel {
	private static final long serialVersionUID = 1745843879510745984L;
	String type() {return AL.knows;}	
}

class DataModelSites extends DataModel {
	private static final long serialVersionUID = 7267362926928642204L;
	String type() {return AL.sites;}	
}

class DataModelNews extends DataModel {
	private static final long serialVersionUID = -2323943132579856140L;
	private String[] columnNames = {"Trust","Times","Sources","Text"};
	
	String type() {return "news";}

	String addRequest(String str) {
		return null;	    
	}

    public void init(JTable table, JScrollPane panel) {
    	this.table = table;
        table.setToolTipText("Check/uncheck to set your trust, hold and release to open site.");
    	//table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    	Dimension tableSize =  table.getPreferredSize();
    	table.getColumnModel().getColumn(0).setMinWidth(24);
    	table.getColumnModel().getColumn(0).setMaxWidth(40);
    	table.getColumnModel().getColumn(0).setPreferredWidth(Math.round(tableSize.width*0.01f));
    	table.getColumnModel().getColumn(1).setMinWidth(40);
       	table.getColumnModel().getColumn(1).setMaxWidth(100);
    	table.getColumnModel().getColumn(1).setPreferredWidth(Math.round(tableSize.width*0.04f));
    	table.getColumnModel().getColumn(2).setPreferredWidth(Math.round(tableSize.width*0.25f));
    	table.getColumnModel().getColumn(3).setPreferredWidth(Math.round(tableSize.width*0.70f));
    }
	
	void buildKey(StringBuilder sb, int r) {
		for (int c = 0, keycount = 0; c < this.getColumnCount(); c++)
		if (!getColumnName(c).toLowerCase().equals(AL.trust)) {
			String val = (String)this.getValueAt(r, c);
			if (!AL.empty(val)) {
				if (keycount > 0)
					sb.append(AL.space).append("and").append(AL.space);
				keycount++;
				sb.append(this.getColumnName(c).toLowerCase()).append(AL.space);
				if (getColumnName(c).toLowerCase().equals(AL.text))
					Writer.quote(sb,val);//encode texts unconditionally
				else
					Writer.toString(sb,val);
			}
		}
	}

	String editRequest(int row, int col, String oldValue, String newValue) {
		StringBuilder sb = new StringBuilder();
		buildKey(sb,row);
		sb.append(AL.space);
		sb.append(AL.trust).append(AL.space);
		Writer.toString(sb,newValue);
		sb.append(AL.period);
        return sb.toString();
	}
    	
    String deleteRequest(Table table) {
    	int[] rows = table.table.getSelectedRows();
		if (rows.length > 0) {
			StringBuilder sb = new StringBuilder();
			for (int i=0; i<rows.length; i++) {
				if (i > 0)
					sb.append(AL.space);
				buildKey(sb, rows[i]);
				sb.append(" new false, trust false");
				sb.append(AL.period);
			}
//TODO: remove thing itself, not knows link only or do GC on given thing otherwise
			return sb.toString();
		}
		return null;
	}

	public boolean isCellEditable(int row, int col) {
        return col == 0 ? true : false;
    }

    protected String[] getColumnNames() {
        return columnNames;
    }
    
	String updateRequest() {
		return "What new true times, sources, text, trust?";
	}
    	
	void update(String string) {
		//TODO:optimize and move all AL-parsing into separate "linguistic" framework
		data = AL.parseToGrid(string,Array.toLower(getColumnNames()),",");
		Arrays.sort(data,new NewsComparator());
		int[] selected = table.getSelectedRows();
		fireTableDataChanged();
		if (selected.length > 0 && table.getRowCount() > 0) {
			int sel = selected[0] >= table.getRowCount() ? table.getRowCount() - 1 : selected[0];
			table.setRowSelectionInterval(sel,sel);
		}
		int pending = 0;
		for (int i = 0; i < data.length; i++)
			if (!((Boolean)data[i][0]).booleanValue())
				pending++;
		App.getApp().displayPending(pending);
	}

    public void open(int row) {
    	//TODO: open on any column click, if url for site is present!
    	open((String)getValueAt(row, 2));
    }
	
}

class DataModelPeers extends DataModel {
	private static final long serialVersionUID = 676971676171984202L;
	//TODO: unify the two
	private String[] columnNames = {"Share", "Name","Surname","Email"};//TODO: Peer, else?
	private String[] propNames   = Array.toLower(columnNames);
	
	String type() {return "peers";}

    public void init(JTable table, JScrollPane panel) {
    	this.table = table;
    	table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);//TODO: multiple
        table.setToolTipText("Check/uncheck to set sharing, double-click to edit.");
    	Dimension tableSize =  table.getPreferredSize();
    	table.getColumnModel().getColumn(0).setMinWidth(24);
    	table.getColumnModel().getColumn(0).setMaxWidth(40);
    	table.getColumnModel().getColumn(0).setPreferredWidth(Math.round(tableSize.width*0.01f));
    	table.getColumnModel().getColumn(1).setPreferredWidth(Math.round(tableSize.width*0.30f));
    	table.getColumnModel().getColumn(2).setPreferredWidth(Math.round(tableSize.width*0.30f));
    	table.getColumnModel().getColumn(3).setPreferredWidth(Math.round(tableSize.width*0.39f));
    }
	
	String addRequest(String str) {
		//TODO: for authorized user only
		if (!AL.empty(str)) {
			String q = Peer.qualifier(str);
			if (!AL.empty(q))
				return "There is peer and trust true and "+q+".";
		}
 		return null;
	}
		
	String deleteRequest(Table table) {
		//TODO: for authorized user only
 		return null;
	}

	public boolean isCellEditable(int row, int col) {
        //return col == 0;//can check only!?
		//TODO: if row is editable?
        return true;
    }

    protected String[] getColumnNames() {
        return columnNames;
    }
    
    public Class getColumnClass(int c) {    	
        return c == 0 ? Boolean.class : String.class;
    }

	String updateRequest() {
		//return "What is peer "+AL.propList(propNames)+"?";	    
		return "What is peer, trust true "+AL.propList(propNames)+"?";	    
	}
    	
	void update(String string) {
		//TODO:optimize and move all AL-parsing into separate "linguistic" framework
		data = AL.parseToGrid(string,propNames,",");
		Arrays.sort(data,new StringArrayComparator());
		fireTableDataChanged();
	}

    public void open(int row) {
    	String q = AL.buildQualifier(propNames,data[row],1);//
    	App.getApp().showPropTabs(q,Peer.editables,false);
    }
	
	String editRequest(int row, int col, String oldValue, String newValue) {
		StringBuilder sb = new StringBuilder();
		sb.append("is peer, name ");
		Writer.toString(sb,data[row][1]);
		sb.append(", surname ");
		Writer.toString(sb,data[row][2]);
		sb.append(", email ");
		Writer.toString(sb,data[row][3]);
		sb.append(' ').append(propNames[col]).append(' ');
		Writer.toString(sb,newValue);
		sb.append(AL.period);
        return sb.toString();
	}
}


class DataModelProps extends DataModel {
	private static final long serialVersionUID = -6752349192651495856L;
	private String[] columnNames = {"Name","Value"};
	
	String type() { return "properties"; }
	String addRequest(String str) { return null; }
	String deleteRequest(Table table) { return null; }
    protected String[] getColumnNames() { return columnNames; }
    
	private String[] propNames = null;
	private String qualifier = null;
	boolean polling = false; //whether to poll for data or or not
	private boolean[] dirtyFlags = null; 
	
    public void init(JTable table, JScrollPane panel) {
    	final DataModelProps model = this;  
       	this.table = table;
    	table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setToolTipText("Double-click to edit property, enter to confrim changes and close the sheet.");
    	//http://stackoverflow.com/questions/13516730/disable-enter-key-from-moving-down-a-row-in-jtable
    	table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),"enter");
    	table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),"escape");
    	table.getActionMap().put("enter", new AbstractAction() {
			private static final long serialVersionUID = -6687130096137249519L;
			public void actionPerformed(ActionEvent e) {
        		String request = model.changeRequest();
                if (request != null) {
                	App.getApp().chatter.request(model,request);
                }
            }
        });
    	table.getActionMap().put("escape", new AbstractAction() {
			private static final long serialVersionUID = -6687130096137249519L;
			public void actionPerformed(ActionEvent e) {
				escape();
            }
        });
    }
	
    private void escape() {
		if (App.getApp().chatter.logged)
			App.getApp().showWorkTabs();
		else
			App.getApp().showLoginTabs();
    }
    
	public boolean isCellEditable(int row, int col) {
        return col > 0;
    }

    public Class getColumnClass(int c) {    	
        return String.class;
    }

	void setContext(String qualifier,String[] propNames) {
		this.qualifier = qualifier;
		this.propNames = propNames;
		dirtyFlags = new boolean[propNames.length];
	}
	
    public void setValueAt(Object value, int row, int col) {
    	//do nothing other than keep value for further submission on Ok
    	if (!data[row][col].equals(value)) {
    		dirtyFlags[row] = true;
    		data[row][col] = value; 
    		fireTableCellUpdated(row, col);
    	}
    }

    String updateRequest() {
		return qualifier == null ? null : "What "+qualifier+" "+AL.propList(propNames)+"?"; 
	}
    	
	void confirm() {
		App.getApp().showWorkTabs();
	}

	public void selected() {
		//if (qualifier.startsWith("My")) {//don't poll data for questions to Me
		if (!polling) {//don't poll data for questions to Me
			if (propNames.length == 0) // just emergency escape :-/
				escape();
			data = new Object[propNames.length][2];
			for (int i = 0; i < propNames.length; i++) {
				data[i][0] = propNames[i];
				data[i][1] = "";
			}
			fireTableDataChanged();
			//http://www.java2s.com/Tutorial/Java/0240__Swing/ProgrammaticallyStartingCellEditinginaJTableComponent.htm
			table.setSurrendersFocusOnKeystroke(true);
			table.setRowSelectionInterval(0,0);
			table.editCellAt(0, 1);
		} 
		else { // otherwise go to request data as usual
			data = new Object[0][];
			table.setSurrendersFocusOnKeystroke(false);
			super.selected();
		}
	}
    
	void update(String string) {
		//TODO:optimize and move all AL-parsing into separate "linguistic" framework
		data = AL.parseToSheet(string,propNames,",");
		//Arrays.sort(data,new StringArrayComparator());
		fireTableDataChanged();
	}

	String changeRequest() {
		HashMap map = new HashMap();
		for (int i = 0; i < data.length; i++) {
			//TODO: enable blank strings, consider some A MUST attributes?
			if (data[i][1] instanceof String && !AL.empty((String)data[i][1]) &&
				dirtyFlags != null && dirtyFlags[i])
				map.put(data[i][0],data[i][1]);
		}
		return qualifier == null || map.size() == 0 ? null :
			qualifier + " " + AL.buildQualifier(propNames,map)+".";
	}
	
    public void open(int row) {
    	//TODO:!!!
    	//open((String) getValueAt(row, 1));
    }
	
}


// it uses a custom cell editor to validate integer input.
public class Table extends JPanel {
	private static final long serialVersionUID = 7577398893023570780L;

	private static final int DOUBLE_CLICK_PERIOD = 200;
	private static final int SINGLE_PRESS_PERIOD = 900;
	
	static DataModel things = new DataModelThings();
	static DataModel sites = new DataModelSites();
	static DataModel peers = new DataModelPeers();
	static DataModel news = new DataModelNews();
	static DataModel props = new DataModelProps();
	
	final DataModel model;
	JTable table;
	
    public Table(DataModel datamodel) {
        super(new GridLayout(1,0));

        //final JTable table = new JTable(this.model = model);
        final JTable table = new JTable(model = datamodel);
//        table.setToolTipText("Double-click to edit, hold and release to open. ");

        this.table = table;
        table.setPreferredScrollableViewportSize(new Dimension(500, 70));
        table.setFillsViewportHeight(true);

        //Create the scroll pane and add the table to it.
        JScrollPane scrollPane = new JScrollPane(table);

        //Set up stricter input validation for the integer column.
        //table.setDefaultEditor(Integer.class, new IntegerEditor(0, 100));
        
	//If we didn't want this editor to be used for other
	//Integer columns, we'd do this:
	//table.getColumnModel().getColumn(3).setCellEditor(
	//	new IntegerEditor(0, 100));
	
        //table.getColumnModel().getColumn(1).setPreferredWidth(50);

        //TODO: handle links and clicks better
        //http://stackoverflow.com/questions/4256680/click-hyperlink-in-jtable        
        table.addMouseListener(new MouseAdapter() {
        	
        	int rowPressed = -1;
        	long timePressed = 0;

        	public void mousePressed(MouseEvent e) {
        		rowPressed = table.getSelectedRow();
        	    timePressed = System.currentTimeMillis();
        	}
        	
        	public void mouseReleased(MouseEvent e) {
        		int row = table.getSelectedRow();
        		//int col = table.getSelectedColumn();
        		long time = System.currentTimeMillis() - timePressed;
        	    if (rowPressed == row 
	        	    && DOUBLE_CLICK_PERIOD < time && time < SINGLE_PRESS_PERIOD) {        	    	
        	    	//String value = ((String)table.getModel().getValueAt(row, col));
        	    	if (table.getSelectedRowCount() <= 1)
        	    		//if (AL.empty(value))
        	    			//model.open(value.toLowerCase());
    	    			model.open(row);
        	    }
        	    rowPressed = -1;
        	}

            public void mouseEntered(MouseEvent e) {
            	TableModel model = table.getModel();
            	String type = ((DataModel)model).type();
            	if (model instanceof DataModel && type.equals("sites") || type.equals("news"))
    	    		table.setCursor(new Cursor(Cursor.HAND_CURSOR));
            }
            
            public void mouseExited(MouseEvent e) {
            	table.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            }
            
        });                
        //Add the scroll pane to this panel.
        add(scrollPane);
        model.init(table,scrollPane);
    }
    
    String[] selected(int col) {
    	int[] rows = table.getSelectedRows();
    	String[] names = new String[rows.length];
    	for (int i=0; i<rows.length; i++)
    		names[i] = (String) table.getModel().getValueAt(rows[i], col);
    	return names;
    }

    void openIfSelected() {
    	int[] rows = table.getSelectedRows();
    	if (rows.length == 1)
    		model.open(rows[0]);
    }
}
