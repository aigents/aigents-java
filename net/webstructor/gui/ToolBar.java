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

import javax.swing.Box;
import javax.swing.JToolBar;
import javax.swing.JButton;
import javax.swing.ImageIcon;
import javax.swing.JTextField;
import javax.swing.JPanel;

import net.webstructor.agent.Body;
import net.webstructor.al.AL;
import net.webstructor.peer.Peer;

import java.net.URL;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ToolBar extends JPanel implements ActionListener {
	private static final long serialVersionUID = -5650657616545357628L;
    final static String DEFAULT_TEXT = "Type here and hit ENTER";
    protected String newline = "\n";
    static final private String SAY = "say";
    static final private String ADD = "add";
    static final private String DELETE = "del";
    static final private String OK = "ok";
    static final private String CANCEL = "cancel";
    static final private String CLEAR = "clear";
    static final private String PREFS = "prefs";
    static final private String PROPS = "props";
    static final private String OPEN = "edit";
    static final private String ENTER = "enter";
    
	final JTextField text;
	final JTextField info;
	JButton say,clear,add,ok,cancel,del,open,pref,prop;

    public ToolBar() {
        super(new BorderLayout());

        //Create the toolbar.
        JToolBar toolBar = new JToolBar("Still draggable");
        
        text = new JTextField(DEFAULT_TEXT);
        text.setColumns(10);
        text.addActionListener(this);
        text.setActionCommand(ENTER);
        text.addMouseListener(new MouseAdapter(){
             public void mouseClicked(MouseEvent e){
            	 if (text.getText().equals(DEFAULT_TEXT))
            		 text.setText("");
             }
        });

        info = new JTextField(null);
        info.setEditable(false);
        info.setEnabled(false);
        
        toolBar.add(text);       
        toolBar.add(info);       
        toolBar.add(Box.createHorizontalGlue());//to have the rest aligned to the right
        toolBar.add(say=makeNavigationButton(this,"talks32.png", SAY, "Say something", "Say"));
        toolBar.add(clear=makeNavigationButton(this,"del32.png", CLEAR, "Clear the log", "Clear"));
        toolBar.add(add=makeNavigationButton(this,"add32.png", ADD,  "Add a thing", "Add"));
        toolBar.add(del=makeNavigationButton(this,"del32.png", DELETE, "Delete the thing", "Delete"));
        toolBar.add(ok=makeNavigationButton(this,"ok32.png", OK,  "Ok to change", "Ok"));
        toolBar.add(cancel=makeNavigationButton(this,"del32.png", CANCEL, "Cancel changes", "Cancel"));
        toolBar.add(open=makeNavigationButton(this,"edit32.png", OPEN, "Open the site", "Edit"));
        toolBar.add(pref=makeNavigationButton(this,"folks32.png", PREFS, "Change your properties", "Your self"));
        toolBar.add(prop=makeNavigationButton(this,"aigent32.png", PROPS, "Change my properties", "My self"));
        
        toolBar.setFloatable(false);
        toolBar.setRollover(true);

        //setPreferredSize(new Dimension(450, 130));
        add(toolBar, BorderLayout.PAGE_START); 
    }

    void setVisibility(Component c) {
    	App app = App.getApp();
        if (c == app.tabs.chatPanel) {
        	app.bar.text.setVisible(true);
        	app.bar.info.setVisible(false);
      	  App.getApp().bar.say.setVisible(true);
          App.getApp().bar.clear.setVisible(true);
      	  App.getApp().bar.add.setVisible(false);
      	  App.getApp().bar.del.setVisible(false);
      	  App.getApp().bar.ok.setVisible(false);
      	  App.getApp().bar.cancel.setVisible(false);
      	  App.getApp().bar.open.setVisible(false);
      	  App.getApp().bar.pref.setVisible(true);
      	  App.getApp().bar.prop.setVisible(true);
      	  App.getApp().eyes.point(Eyes.RIGHT);
        }
        else
        if (c == app.tabs.sitesPanel) {
            app.bar.text.setVisible(true);
        	app.bar.info.setVisible(false);
      	  App.getApp().bar.say.setVisible(false);
          App.getApp().bar.clear.setVisible(false);
      	  App.getApp().bar.add.setVisible(true);
      	  App.getApp().bar.del.setVisible(true);
      	  App.getApp().bar.ok.setVisible(false);
      	  App.getApp().bar.cancel.setVisible(false);
      	  App.getApp().bar.open.setVisible(true);
      	  App.getApp().bar.pref.setVisible(true);
      	  App.getApp().bar.prop.setVisible(true);
      	  App.getApp().eyes.point(Eyes.LEFT);
        }
        else
        if (c == app.tabs.thingsPanel) {
        	app.bar.text.setVisible(true);
        	app.bar.info.setVisible(false);
      	  App.getApp().bar.say.setVisible(false);
          App.getApp().bar.clear.setVisible(false);
      	  App.getApp().bar.add.setVisible(true);
      	  App.getApp().bar.del.setVisible(true);
      	  App.getApp().bar.ok.setVisible(false);
      	  App.getApp().bar.cancel.setVisible(false);
      	  App.getApp().bar.open.setVisible(false);
      	  App.getApp().bar.pref.setVisible(true);
      	  App.getApp().bar.prop.setVisible(true);
      	  App.getApp().eyes.point(Eyes.LEFT);
        }
        else
        if (c == app.tabs.newsPanel) {
        	app.bar.text.setVisible(false);//TODO:enable for filterting only
        	app.bar.info.setVisible(true);
      	  App.getApp().bar.say.setVisible(false);
          App.getApp().bar.clear.setVisible(false);
      	  App.getApp().bar.add.setVisible(false);
      	  App.getApp().bar.del.setVisible(true);
      	  App.getApp().bar.ok.setVisible(false);
      	  App.getApp().bar.cancel.setVisible(false);
      	  App.getApp().bar.open.setVisible(true);
      	  App.getApp().bar.pref.setVisible(true);
      	  App.getApp().bar.prop.setVisible(true);
      	  App.getApp().eyes.point(Eyes.DOWN);
        }
        else
        if (c == app.tabs.peersPanel) {
           	app.bar.text.setVisible(true);//TODO:use for filtering/searching
        	app.bar.info.setVisible(false);
          App.getApp().bar.say.setVisible(false);
          App.getApp().bar.clear.setVisible(false);
      	  App.getApp().bar.add.setVisible(true);
      	  App.getApp().bar.del.setVisible(false);//TODO:make it possible to delete peers!?
      	  App.getApp().bar.ok.setVisible(false);
      	  App.getApp().bar.cancel.setVisible(false);
      	  App.getApp().bar.open.setVisible(false);
      	  App.getApp().bar.pref.setVisible(true);
      	  App.getApp().bar.prop.setVisible(true);
      	  App.getApp().eyes.point(Eyes.DOWN);
        }
        else
        if (c == app.tabs.propsPanel) {
           	app.bar.text.setVisible(false);
        	app.bar.info.setVisible(true);
          App.getApp().bar.say.setVisible(false);
          App.getApp().bar.clear.setVisible(false);
      	  App.getApp().bar.add.setVisible(false);
      	  App.getApp().bar.del.setVisible(false);
      	  App.getApp().bar.ok.setVisible(true);
      	  App.getApp().bar.cancel.setVisible(true);
      	  App.getApp().bar.open.setVisible(false);
      	  App.getApp().bar.pref.setVisible(false);
      	  App.getApp().bar.prop.setVisible(false);
      	  App.getApp().eyes.point(Eyes.RIGHT);
        }
    }
    
    protected static JButton makeNavigationButton(
    		ActionListener panel,
    		String imageName, String actionCommand, String toolTipText, String altText) {
        //Look for the image.
        String imgLocation = "/" + imageName;
        URL imageURL = ToolBar.class.getResource(imgLocation);

        //Create and initialize the button.
        JButton button = new JButton();
        button.setActionCommand(actionCommand);
        button.setToolTipText(toolTipText);
        button.addActionListener(panel);

        if (imageURL != null) {                      //image found
            button.setIcon(new ImageIcon(imageURL, altText));
        } else {                                     //no image found
            button.setText(altText);
            //TODO:("Resource not found: " + imgLocation);
        }

        return button;
    }

    private String getText() {
        String str = text.getText();
        if (str.equals(DEFAULT_TEXT)) {
        	text.selectAll();
        	return "";
        }
        if (str.length() == 0)
        	text.setText(DEFAULT_TEXT);
        return str;
    }
    
    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();
        App app = App.getApp();

        if (CLEAR.equals(cmd)) {
        	app.chat.setText(null);
        }
        else
        if (app.bar.text.isVisible() && (ENTER.equals(cmd) || SAY.equals(cmd) || ADD.equals(cmd))) {
        	String str = getText();
        	if (AL.empty(str)) {
        		if (SAY.equals(cmd))
        			text.setText(DEFAULT_TEXT);
        	}
        	else {
            	DataModel model = App.getApp().dataModel();
            	if (model != null) {            		
            		if (model.table.isEditing())
            			model.table.getCellEditor().stopCellEditing();
            		String request = model.addRequest(str);
            		if (!AL.empty(request)) {
            			App.getApp().chatter.request(model,request);
                        text.setText("");
            		}
            	}
            	else
            	{
            		app.inputText(str);
            		//app.tabs.setSelected(app.tabs.chatPanel);
                    text.setText("");
            	}
        	}
        }
        else        	
        if (!app.bar.text.isVisible() && OK.equals(cmd)) { // confirm property changes
        	DataModel model = App.getApp().dataModel();
        	if (model != null && model instanceof DataModelProps) {
        		if (model.table.isEditing())
        			model.table.getCellEditor().stopCellEditing();
        		DataModelProps props = (DataModelProps)model;
        		String request = props.changeRequest();
                if (request != null)
                	App.getApp().chatter.request(model,request);
                else
                	App.getApp().showWorkTabs();
        	}
        } 
        else 
        if (DELETE.equals(cmd) || CANCEL.equals(cmd)) {
        	Table table = app.table();
        	if (table != null) {
        		if (table.table.isEditing())
        			table.table.getCellEditor().stopCellEditing();
                String request = table.model.deleteRequest(table);
                if (request != null)
                	App.getApp().chatter.request(table.model,request);
                else
                if (table.model instanceof DataModelProps)
                	App.getApp().showWorkTabs();
        	}        	
        } 
        else 
        if (OPEN.equals(cmd)) {
        	Table table = app.table();
        	if (table != null) {
        		table.openIfSelected();
        	}
        } 
        else 
        if (PREFS.equals(cmd)) {
        	String q = "My ";
        	App.getApp().showPropTabs(q,Peer.editables,true); 	
        } 
        else 
        if (PROPS.equals(cmd)) {
        	String q = "is self";
        	App.getApp().showPropTabs(q,Body.strings,true); 	
        } 
                
        app.eyes.point(Eyes.DOWN);

    }

}
