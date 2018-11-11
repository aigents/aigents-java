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

import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JComponent;
import javax.swing.JTextArea;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.KeyEvent;

public class Tabs extends JPanel {
	
	private static final long serialVersionUID = -5085274979129876188L;

    JTabbedPane tabbedPane;    
    JComponent sitesPanel;
    JComponent thingsPanel;
    JComponent peersPanel;
    JComponent newsPanel;
    JComponent propsPanel;
    JScrollPane chatPanel;
	
	public Tabs() {
        super(new GridLayout(1, 1));
        
        tabbedPane = new JTabbedPane();
        int tabIndex = 0;
        
        App app = App.getApp();
        
        thingsPanel = new Table(Table.things);
        tabbedPane.addTab("Things", createImageIcon("/things32.png"), thingsPanel, "Things or subjects of your interest");
        tabbedPane.setMnemonicAt(tabIndex++, KeyEvent.VK_1);
        
        sitesPanel = new Table(Table.sites);
        sitesPanel.setPreferredSize(new Dimension(450, 450));
        tabbedPane.addTab("Sites", createImageIcon("/sites32.png"), sitesPanel, "Sites with things interesting for you");
        tabbedPane.setMnemonicAt(tabIndex++, KeyEvent.VK_2);
        
        newsPanel = new Table(Table.news);
        tabbedPane.addTab("News", createImageIcon("/news32.png"), newsPanel, "News from the sites about your things");
        tabbedPane.setMnemonicAt(tabIndex++, KeyEvent.VK_3);

        peersPanel = new Table(Table.peers);
        tabbedPane.addTab("Peers", createImageIcon("/folks32.png"), peersPanel, "Peers that you are sharing news with");
        tabbedPane.setMnemonicAt(tabIndex++, KeyEvent.VK_4);
        
        app.chat = new JTextArea(5, 30);
        app.chat.setEditable(false);
        app.chat.setLineWrap(true);
        chatPanel = new JScrollPane(App.getApp().chat);
        tabbedPane.addTab("Talks", createImageIcon("/talks32.png"), chatPanel,"Log of your conversation with me, your Automatic Intelligent Internet Agent");
        tabbedPane.setMnemonicAt(tabIndex++, KeyEvent.VK_5);
        
        propsPanel = new Table(Table.props);
        tabbedPane.addTab("Props", createImageIcon("/edit32.png"), propsPanel, "Properties of the thing");
        tabbedPane.setMnemonicAt(tabIndex++, KeyEvent.VK_6);

        add(tabbedPane);
        tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

        ChangeListener changeListener = new ChangeListener() {
            public void stateChanged(ChangeEvent changeEvent) {
              JTabbedPane sourceTabbedPane = (JTabbedPane) changeEvent.getSource();
              int index = sourceTabbedPane.getSelectedIndex();
              Component c = sourceTabbedPane.getComponentAt(index);
              if (c instanceof Table)
            	  //App.getApp().chatter.request(((Table)c).model);
            	  (((Table)c).model).selected();
              App.getApp().bar.setVisibility(c);
            }
         };
         tabbedPane.addChangeListener(changeListener);		
	}
    
	void setSelected(Component component) {
		tabbedPane.setSelectedComponent(component);
	}

	void setEnabled(Component component, boolean enabled) {
		int i = tabbedPane.indexOfComponent(component);
		if (i != -1)
			tabbedPane.setEnabledAt(i,enabled);
	}
	
    // Returns an ImageIcon, or null if the path was invalid.
    protected static ImageIcon createImageIcon(String path) {
    	return createImageIcon(path,null);
    }
    
    protected static ImageIcon createImageIcon(String path, String description) {
        java.net.URL imgURL = Tabs.class.getResource(path);
        if (imgURL != null) {
            return description == null ? new ImageIcon(imgURL) : new ImageIcon(imgURL, description);
        } else {
            //TODO:("Couldn't find file: " + path);
            return null;
        }
    }
    
    
}
