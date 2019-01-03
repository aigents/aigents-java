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

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import net.webstructor.agent.Body;

public class Tray {
	private TrayIcon icon = null;
	private Image havenews = Tabs.createImageIcon("/aigentnews128.png", "Aigents news").getImage();
	private Image nonews = Tabs.createImageIcon("/aigent128.png", "Aigents no news").getImage();
	
    public Tray() {
         if (!SystemTray.isSupported()) {
            return;
        }
        final PopupMenu popup = new PopupMenu();
        final TrayIcon trayIcon = new TrayIcon(Tabs.createImageIcon("/aigentnews128.png", "Aigents news").getImage());
        final SystemTray tray = SystemTray.getSystemTray();
        
        MenuItem aboutItem = new MenuItem("About");
        MenuItem exitItem = new MenuItem("Exit");
        popup.add(aboutItem);
        popup.add(exitItem);
        
        trayIcon.setImageAutoSize(true);
        trayIcon.setPopupMenu(popup);
        //trayIcon.setToolTip(null);//cnt == 0 TODO: make dependant on actual news count
        //trayIcon.setToolTip("0 news from aigents");//cnt > 0 TODO: make dependant on actual news count
        setStatus(null);
        
        try {
            tray.add(trayIcon);
        } catch (AWTException e) {
            return;
        }
        
        trayIcon.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(null,"Aigents is running",Body.APPNAME,JOptionPane.NO_OPTION);
            }
        });
        
        aboutItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(null,Body.notice(),Body.APPNAME,JOptionPane.NO_OPTION);
            }
        });
        
        exitItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                tray.remove(trayIcon);
                System.exit(0);//TODO: handle by body with autosave
            }
        });
        
        icon = trayIcon;        
    }
    
    void setStatus(String text) {
    	if (icon != null) {
    		icon.setToolTip(text == null ? null : text+" news from aigents");
    		icon.setImage(text == null ? nonews : havenews);
    	}
    }

}

