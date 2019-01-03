/*
 * Copyright 2005-2008, Anton Kolonin,
 * All rights reserved.
 *
 * This software is proprietary information belonging to Anton Kolonin. 
 * You shall not disclose this and shall not use it in any way 
 * without of written permission granted by Anton Kolonin.
 */
package net.webstructor.mine.auth;

import java.util.HashMap;
import net.webstructor.mine.util.StringContext;
import net.webstructor.mine.store.Storage;
import net.webstructor.mine.store.SimpleMemoryStorage;

public class AdmServer {

	private static AdmServer m_instance = null;
	
	private HashMap m_sessions = new HashMap();
	
	public static AdmServer getInstance(String path){
		if (m_instance == null)
			m_instance = new AdmServer(path); 
		return m_instance;
	}
	
	String m_path;
	Storage m_storage = null; 
	
	public AdmServer(String path){
		m_path = path;
	}
	
	//TODO synchronize
	public String createSession(String username,String password,StringContext stringContext){
		if (m_storage == null) {
			try {
				m_storage = new SimpleMemoryStorage();
				m_storage.startUp(m_path,new StoreSchema());
			} catch (Exception e) {
				return e.toString();
			}
		}
		//TODO check username and password
		AuthSession sess = new AuthSession(
				this,
				new AdmSession(this,stringContext),
				stringContext);
		Integer id = sess.getId();
		m_sessions.put(id,sess);
		return id.toString();
	}
	
	//TODO synchronize
	public AuthSession getSession(String sessionid){
		Integer id = Integer.valueOf(sessionid);
		return (AuthSession)m_sessions.get(id);
	}
}
