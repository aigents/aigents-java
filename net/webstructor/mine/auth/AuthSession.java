/*
 * Copyright 2005-2008, Anton Kolonin,
 * All rights reserved.
 *
 * This software is proprietary information belonging to Anton Kolonin. 
 * You shall not disclose this and shall not use it in any way 
 * without of written permission granted by Anton Kolonin.
 */
package net.webstructor.mine.auth;

import net.webstructor.mine.core.CoreRelation;
import net.webstructor.mine.inf.InfSession;
import net.webstructor.mine.util.StringUtil;
import net.webstructor.mine.util.StringContext;

public class AuthSession {
		AdmServer m_server;
		AdmSession m_admSession;
		InfSession m_infSession = null; // no access by default
		StringContext m_stringContext;
		public AuthSession(AdmServer server,AdmSession admSession,StringContext stringContext){
			m_server = server;
			m_admSession = admSession;
			m_stringContext = stringContext; 
		}
		public Integer getId(){
			return new Integer(0); 
			//return Integer(hashCode());
		}
		public String logout(){
			//TODO destroy session
			return getId().toString();
		}
		public String commit() throws Exception {
			m_server.m_storage.commit();
			if (this.m_infSession != null)
				this.m_infSession.commit();
			return getId().toString();
		}
		public String getAssets() throws Exception {
			//TODO list assets available in auth db and accessible by user
			return StringUtil.toString(m_server.m_storage.getRels(StoreSchema.Asset),m_stringContext);
		}
		public String setAsset(String[] args) throws Exception {
			if (args.length<3)
				throw new Exception("Invalid assetid");
			CoreRelation asset = m_server.m_storage.getRel(Integer.parseInt(args[2]));
			if (asset != null) {
				if (this.m_infSession != null)
					this.m_infSession.close();
				String path = m_server.m_storage.getName(asset.getId(),StoreSchema.AssetPath);
				if (path != null) {
					m_infSession = new InfSession(path,m_stringContext);
					return path;
					//return args[2];
				}
			}
			return null;
		}
		public AdmSession getAdmSession(){
			return m_admSession;
		}
		public InfSession getInfSession(){
			return m_infSession;
		}
		public String handle(String[] args) throws Exception {
			String result = null;
			if (m_infSession != null)
				result = m_infSession.handle(args);
			if (result == null && m_admSession != null)
				result = m_admSession.handle(args);
			if (result == null)
			{
					if (args[0].equalsIgnoreCase("logout"))
						result = logout();
					else
					if (args[0].equalsIgnoreCase("getassets"))
						//getassets;<sessionid> => <assetid>;<assetname> * (all visible assets)
						result = getAssets();
					else
					if (args[0].equalsIgnoreCase("setasset"))
						//o	setasset;<sessionid>;<assetid> => <assetid> (set current asset for session)
						result = setAsset(args);
					else
					if (args[0].equalsIgnoreCase("commit")) { // commit auth DB only
						//o commit; <sessionid> => <sessionid> 
						result = commit();
					}
			}
			return result;
		}
}
