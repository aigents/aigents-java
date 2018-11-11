/*
 * Copyright 2005-2008, Anton Kolonin,
 * All rights reserved.
 *
 * This software is proprietary information belonging to Anton Kolonin. 
 * You shall not disclose this and shall not use it in any way 
 * without of written permission granted by Anton Kolonin.
 */
package net.webstructor.mine.inf;

import net.webstructor.mine.util.StringContext;
import net.webstructor.mine.util.StringUtil;
import net.webstructor.mine.core.BasicCoreRelation;

public class InfSession {
	
	int m_maxNumber = 10;
	float m_minRelevance = 0.1f;  
	float m_minSalience = 0.75f;  
	
	StringContext m_stringContext;
	InfServer m_server;
	
	public InfSession(String path,StringContext stringContext) throws Exception {
		m_stringContext = stringContext;
		m_server = InfServer.getInstance(path);
	}
	
	public String commit() throws Exception {
		m_server.m_storage.commit();
		return "0";
	}
	
	public void close() {
		m_stringContext = null;
		m_server = null;
	}
	
	//	getlimits;<sessionid> => <maxn>;<minrelevance>;<minsalience>
	String getLimits(){
		String[] limits = new String[] {
			new Integer(m_maxNumber).toString(),
			new Float(m_minRelevance).toString(),
			new Float(m_minSalience).toString()
		};  
		return StringUtil.toString(limits,m_stringContext.m_cmdDelimeters);
	}
	
	//	setlimits;<sessionid>;<maxn>;<minrelevance>;<minsalience> => <maxn>;<minrelevance>;<minsalience>
	String setLimits(String[] args){
		if (args.length>2)
			m_maxNumber = Integer.parseInt(args[2]);
		if (args.length>3)
			m_minRelevance = Float.parseFloat(args[3]);
		if (args.length>4)
			m_minSalience = Float.parseFloat(args[4]);
		return getLimits();
	}
	
	public String getRels(int typeId) throws Exception {
		return StringUtil.toString(m_server.m_storage.getRels(typeId),m_stringContext);
	}

	public String addRel(int typeId, String[] args) throws Exception {
		if (args.length<3)
			throw new InfException("Insufficient arguments");
		BasicCoreRelation rel = new BasicCoreRelation(typeId,args[2]);
		return new Integer( m_server.m_storage.addRel(rel) ).toString();
	}
	
	public String addRelByIdAndName(String[] args) throws Exception {
		if (args.length<4)
			throw new InfException("Insufficient arguments");
		BasicCoreRelation rel = new BasicCoreRelation(Integer.parseInt(args[2]),args[3]);
		return new Integer( m_server.m_storage.addRel(rel) ).toString();
	}
	
	public String handle(String[] args) throws Exception {
		String result = null;
		String cmd = args[0]; 
		if (cmd.equalsIgnoreCase("getlimits"))
			return getLimits();
		else
		if (cmd.equalsIgnoreCase("setlimits"))
			return setLimits(args);
		else
		if (cmd.equalsIgnoreCase("getdocsrcs"))
			return getRels(StoreSchema.DocumentSource);
		else
		if (cmd.equalsIgnoreCase("getdomains"))
			return getRels(StoreSchema.Domain);
		else
		if (cmd.equalsIgnoreCase("getfeattypes"))
			return getRels(StoreSchema.FeatureType);
		else
		if (cmd.equalsIgnoreCase("adddom"))
			return addRel(StoreSchema.Domain,args);
		else
		if (cmd.equalsIgnoreCase("adddocsrc"))
			return addRel(StoreSchema.DocumentSource,args);
		else
		if (cmd.equalsIgnoreCase("adddoc"))
			return addRelByIdAndName(args);
		else
		if (cmd.equalsIgnoreCase("addcat"))
			return addRelByIdAndName(args);
		else
		if (cmd.equalsIgnoreCase("addfeat"))
			return addRelByIdAndName(args);
		
		/*
		o	getdoctext; <sessionid>;<docid> => <doctext> (parsed to text)
		o	getdochighlight; <sessionid>;<docid> => <dochighlight> (highlighted text)
		o	getdocs;<sessionid>;<docsrcid>;<text>;<catids> => <docid>;<docname>;<relevance> * 
		o	getcats;<sessionid>;<domid>;<text> => <catid>;<catname>;<relevance> *  
		o	getcatdocs;<sessionid>;<catid>;<docsrcid> => <catdocid>;<docsrcname>;<docname>;<relevance>;<confirmation> *
		o	getdoccats;<sessionid>;<docid>;<domid>; => <doccatid>;<domname>;<catname>;<relevance>;<confirmation> *
		o	getdocfeats;<sessionid>;<docid>;<feattypeid>;<catid> => <docfeatid>;<feattypename>;<featname>;<relevance>;<confirmation> *
		o	getcatfeats;<sessionid>;<catid>;<feattypeid>;<docid> => <catfeatid>;<feattypename>;<featname>;<relevance>;<confirmation> *
		o	getcatdoms; <sessionid>;<catid> => <catdomid>;<catdomname>;<domname> * (table header)
		o	getobjs; <sessionid>;<catid>;<docid>;<catids> => <objid>;<catname> �* (table rows)
		o	getobjcats; <sessionid>;<objid> => <catdomid>;<catdomname>;<domname>;<catnames> *
		o	getobjcats; <sessionid>;<objid>; <catdomid> => <objcatid>;<objcatname>;<relevance>;<confirmation> *
		o	updatedoc; <sessionid>;<docid>;<docname> => <docid> (update url/body and/or reprocess the associations)
		o	deletedoc; <sessionid>;<docid> => <docid>
		o	updatecat; <sessionid>;<catid>;<catname> => <catid> (update name and/or reprocess the associations)
		o	deletecat; <sessionid>;<catid> => <catid>
		o	deletefeat; <sessionid>;<featid> => <featid>
		o	adddoccat; <sessionid>;<docid><catid> => <doccatid>
		o	updatedoccat; <sessionid>;<doccatid>;<confirmation> => <doccatid>
		o	addcatfeat; <sessionid>;<cattid>; <featid> => <catfeatid>
		o	updatecatfeat;<sessionid>;<catfeatid>; <confirmation> => <catfeatid>
		o	addcatdom; <sessionid>;<catid>;<domid>;<catdomname> => <catdomid>
		o	updatecatdom; <sessionid>;<catdomid>;<catdomname> => <catdomid>
		o	deletecatdom; <sessionid>;<catdomid> => <catdomid>
		o	addobj;<sessionid>;<catid> => <objid>
		o	deleteobj;<sessionid>;<objid> => <objid>
		o	addobjcat;<sessionid>;<objid>;<catdomid>;<catid> => <objcatid>
		o	updateobjcat;<sessionid>; <objcatid>; <catid> => <objcatid>
		o	deleteobjcat;<sessionid>;<objcatid> => <objcatid>
		*/
		
		return result;
	}
	
}
