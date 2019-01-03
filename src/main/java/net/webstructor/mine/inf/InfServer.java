/*
 * Copyright 2005-2008, Anton Kolonin,
 * All rights reserved.
 *
 * This software is proprietary information belonging to Anton Kolonin. 
 * You shall not disclose this and shall not use it in any way 
 * without of written permission granted by Anton Kolonin.
 */
package net.webstructor.mine.inf;

import java.util.Hashtable;

import net.webstructor.mine.auth.StoreSchema;
import net.webstructor.mine.store.SimpleMemoryStorage;
import net.webstructor.mine.store.Storage;

public class InfServer {

	static Hashtable m_instances = new Hashtable();
	
	public static InfServer getInstance(String path) throws Exception {
		Object instance = m_instances.get(path);
		if (instance == null) {
			instance = new InfServer(path);
			m_instances.put(path,instance);
		}
		return (InfServer)instance;
	}
	
	Storage m_storage = null; 
	
	InfServer(String path) throws Exception {
		m_storage = new SimpleMemoryStorage();
		m_storage.startUp(path,new StoreSchema());
	}
	
}
