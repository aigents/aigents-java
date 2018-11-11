/*
 * Copyright 2005-2008, Anton Kolonin,
 * All rights reserved.
 *
 * This software is proprietary information belonging to Anton Kolonin. 
 * You shall not disclose this and shall not use it in any way 
 * without of written permission granted by Anton Kolonin.
 */
package net.webstructor.mine.auth;

import net.webstructor.mine.util.StringContext;
import net.webstructor.mine.store.Storage;
import net.webstructor.mine.core.BasicCoreRelation;

public class AdmSession {
	AdmServer m_server;
	StringContext m_stringContext;
	public AdmSession(AdmServer server, StringContext stringContext){
		m_server = server;
		m_stringContext = stringContext;
	}
	public String addAsset(String[] args) throws Exception {
		Storage st = m_server.m_storage;  
		//2,3,4
		int a = st.addRel(new BasicCoreRelation(StoreSchema.Asset,args[2]));
		int d = st.addRel(new BasicCoreRelation(StoreSchema.Description,args[3])); 
		int p = st.addRel(new BasicCoreRelation(StoreSchema.Path,args[4]));
		st.addRel(new BasicCoreRelation(StoreSchema.AssetDescription,new int[]{a,d})); 
		st.addRel(new BasicCoreRelation(StoreSchema.AssetPath,new int[]{a,p})); 
		return Integer.toString(a);
	}
	/*
	o	getadmsession => <sessionid>
	o	getinfsession => <sessionid>
	o	getgroups;<sessionid> => <groupid>;<groupname> * (all visible groups)
	o	getusergroups;<sessionid>;<userid> => <groupid>;<groupname>;<accessinfo>*
	o	getgroupassets;<sessionid>;<groupid> => <assetid>;<assetname>;< accessinfo >*
	o	getassetgroups;<sessionid>;<assetid> => <groupid>;<groupname>;< accessinfo >*
	o	getgroupusers;<sessionid>;<groupid> => <userid>;<username>;< accessinfo >*
	o	addgroup;<sessionid>;<groupname>;<description> => <groupid>
	o	updategroup; <sessionid>;<groupid>;<groupname>;<description> => <groupid>
	o	deletegroup; <sessionid>;<groupid> => <groupid>
	o	adduser; <sessionid>;<username>;<login>;<password>;<email>;<groupid> => <userid>
	o	updateuser; <sessionid>;<userid>;<username>;<login>;<password>;<email> => <userid>
	o	deleteuser; <sessionid>;<userid> => <userid>
	o	addusergroup;<sessionid>;<userid>;<groupid>; <accessinfo>
	o	updateusergroup;<sessionid>;<userid>;<groupid>; <accessinfo>
	o	deleteusergroup;<sessionid>;<userid>;<groupid>
	o	addasset; <sessionid>;<assetname>;<description>;<path> => <assetid>
	o	updateasset; <sessionid>;<assetid>;<assetname>;<description>;<path> => < assetid >
	o	deleteasset; <sessionid>;<assetid> => < assetid >
	o	addgroupasset;<sessionid>;<groupid>;<assetid>; <accessinfo>
	o	updategroupasset;<sessionid>;< groupid >;<assetid>; <accessinfo>
	o	deletegroupasset;<sessionid>;< groupid >;<assetid>
	*/
	
	public String handle(String[] args) throws Exception {
		String result = null;
		if (args[0].equalsIgnoreCase("addasset"))
			//o	addasset; <sessionid>;<assetname>;<description>;<path> => <assetid>
			result = addAsset(args);
		else
		if (args[0].equalsIgnoreCase("xxx")) { // commit auth DB only
		}
		return result;
	}
}
