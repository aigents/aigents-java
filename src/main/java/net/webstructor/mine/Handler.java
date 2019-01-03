/*
 * Copyright 2005-2008, Anton Kolonin,
 * All rights reserved.
 *
 * This software is proprietary information belonging to Anton Kolonin. 
 * You shall not disclose this and shall not use it in any way 
 * without of written permission granted by Anton Kolonin.
 */
package net.webstructor.mine;

//import java.util.Vector;
import net.webstructor.mine.auth.AdmServer;
import net.webstructor.mine.auth.AuthSession;
import net.webstructor.mine.util.StringUtil;
import net.webstructor.mine.util.StringContext;

/**
 * Statice/singleton class implementing textual command-line interface with
 * IDD server, may be used to implement HTTP server, single-user currently
 * and multi-user in the future 
 * @author Anton
 * @since 20070518
 */
public class Handler 
{
	AdmServer m_authServer = null;

	public static final StringContext m_stringContext = new StringContext();
	{
		m_stringContext.m_cmdDelimeters = ";"; //"|\t";
		m_stringContext.m_httpLineBreak = "<br>"; //"\n"
		m_stringContext.m_itemDelimeters = ","; 
	}
	
	/*
	public static final String learn = "learn";
	public static final String discern = "discern";
	public static final String listdiscern = "listdiscern";
	public static final String listcatfeats = "listcatfeats";
	public static final String listfeatcats = "listfeatcats";
	public static final String listcatdoms = "listcatdoms";
	public static final String listcatcats = "listcatcats";
	public static final String listcats = "listcats";
	public static final String listdomcats = "listdomcats";
	public static final String listcatcatsback = "listcatcatsback";
	*/
	
	public Handler(AdmServer server)
	{
		m_authServer = server;
		
		/*
		TextCat.setServer(serv); //refer to static/singleton!!!

		CatIns.m_columnId   =  -1;
		CatIns.m_columnDesc = 0;
		CatIns.m_columnTags = 1;
		CatIns.m_columnTaxo = -1;
		CatIns.m_columnVendor = -1;
	*/
	}

	/*
	String listFeatRels(int typeId,int feats[],Context context) throws Exception
	{
		if (feats!=null && feats.length>0)
		{
			Storager stor = m_serv.getStorager();
			Processor proc = m_serv.getProcessor();
			int fakeTextId = 1;
			SimpleMemory mem = new SimpleMemory(Id.TEXTCATEGORY,new int[]{Id.TEXT,Id.CATEGORY},false,false); 
			for (int f=0;f<feats.length;f++)
			{
				Relation rels[] = context.m_filterDomains==null
					? stor.getRels(typeId,feats[f],1)
					: stor.getRels(typeId,feats[f],1,0,context.m_filterDomains);
				if (rels!=null && rels.length>0)
				{
		            for (int cf=0;cf<rels.length;cf++)
		            {
		            	SimpleRelation rel = (SimpleRelation)rels[cf];
		        		int rid = rel.getId();
		        		float relevance = context.m_filterDomains == null
		        			? proc.getRelevance(typeId,rid)
		        			: proc.getRelevance(typeId,rid,0,context.m_filterDomains); 
		            	if (context.m_bBooleanAndMode)
		            		TextCat.posEvidenceRel(mem,Id.TEXTCATEGORY,new int[]{fakeTextId,rel.getIds()[0]},relevance,1);
		            	else
		            		TextCat.posEvidenceRel(mem,Id.TEXTCATEGORY,new int[]{fakeTextId,rel.getIds()[0]},relevance);
		            }
				}
			}
	        Relation[] rels = context.m_filterDomains==null 
	        	? mem.getRels(fakeTextId,0)
        		: stor.getRels(mem,fakeTextId,0,1,context.m_filterDomains);

        	return StringUtil.toString(TextCat.toRelevantItems(
        			m_serv,rels,1,context,1,context.m_filterDomains,true,false,
        			context.m_bBooleanAndMode?feats.length:0),"\n");
		}
		return "";
	}
*/
/*	
	String listCatRels(int typeId,int cats[][],Context context,int dimFrom,int dimTo) throws Exception//20070601
	{
		//int dimFrom = 0;//20070601
		//int dimTo = 1;//20070601
		
		if (cats!=null)
		{
			Storager stor = m_serv.getStorager();
			Processor proc = m_serv.getProcessor();
			int fakeTextId = 1;
			int booleanAndDimension = 0;
			SimpleMemory mem = new SimpleMemory(typeId,new int[]{Id.TEXT,Id.CATEGORY},false,false); 
			for (int d=0;d<cats.length;d++)
			if (cats[d]!=null)
			for (int c=0;c<cats[d].length;c++)
			{
        		booleanAndDimension++;
				Relation rels[] = context.m_filterDomains==null
					? stor.getRels(typeId,cats[d][c],dimFrom)//20070601
					: stor.getRels(typeId,cats[d][c],dimFrom,dimTo,context.m_filterDomains);//20070601
				if (rels!=null && rels.length>0)
				{
		            for (int cf=0;cf<rels.length;cf++)
		            {
		            	SimpleRelation rel = (SimpleRelation)rels[cf];
		        		int rid = rel.getId();
		        		float relevance = context.m_filterDomains == null
		        			? proc.getRelevance(typeId,rid)
		        			: proc.getRelevance(typeId,rid,dimTo,context.m_filterDomains);//20070601 
		            	if (context.m_bBooleanAndMode)
		            		TextCat.posEvidenceRel(mem,typeId,new int[]{fakeTextId,rel.getIds()[dimTo]},relevance,1);//20070601
		            	else
		            		TextCat.posEvidenceRel(mem,typeId,new int[]{fakeTextId,rel.getIds()[dimTo]},relevance);//20070601
		            }
				}
			}
	        Relation[] rels = context.m_filterDomains==null 
        	? mem.getRels(fakeTextId,0)
    		: stor.getRels(mem,fakeTextId,0,1,context.m_filterDomains);
		
        	return StringUtil.toString(TextCat.toRelevantItems(
        			m_serv,rels,1,context,1,context.m_filterDomains,true,false,
        			context.m_bBooleanAndMode?booleanAndDimension:0),"\n");
		}
		return "";
	}
*/	
	/*
	learn|<desc>|<tags>[|<domains>] 
	- learns associations between features in <desc> and categories in <tags> for domains listed in <domains>
	- returns Ok
	*/
	/*
	public String doLearn(String line,String args[]) throws Exception
	{
		int targetDomains[] = args.length>3? StringUtil.parseIntArray(args[3],",;"): null;
		CatIns.processLine(m_serv.getStorager(),line,targetDomains);
		return "Ok";
	}
*/
	/*
	discern|<desc>|[<tags>]|[<domains>]|[<cpf>]|[<minRel>]|[<maxN>]
	- discerns categories for domains listed in <domains>
	- returns line in conventional IDD format, delimited with tabs or |: 
	...|tags|pofs|POF|QA|Tokens_Cover|Tags_Cover|N|POF_N|QA_N|...
	*/
	/*
	public String doDiscern(String line,String args[]) throws Exception
	{
		Context context = new Context(args,3,4,5,6);
		SimpleMemory mem = new SimpleMemory(Id.TEXTCATEGORY,new int[]{Id.TEXT,Id.CATEGORY},false,false);//20070323;
		return FileCat.processLine(m_serv,mem,1,line,context);
	}
	*/

	/*
	listdiscern|<desc>|[<tags>]|[<domains>]|[<cpf>]|[<minRel>]|[<maxN>]
	- discerns categories for domains listed in <domains>
	- returns line in list format, each line for individual attribute/domains:
	tag_id|tag_QA_values|tag_values|tag_POFs|tag_QA
	*/
	/*
	public String doListDiscern(String line,String args[]) throws Exception
	{
		Context context = new Context(args,3,4,5,6);
		context.m_bStdFormat = false;
		SimpleMemory mem = new SimpleMemory(Id.TEXTCATEGORY,new int[]{Id.TEXT,Id.CATEGORY},false,false);//20070323;
		return FileCat.processLine(m_serv,mem,1,line,context);
	}
*/
	/*
	listcatfeats|<tags>|[<minRel>]|[<maxN>]|[<booleanMode>]
	- lists features associated with categories listed in <tags> 
	- returns line in list format, each line for individual feature:
	id|evidence|relevance|POF|relativePOF|name
	*/
	/*
	public String doListCatFeats(String line,String args[]) throws Exception
	{
		int cats[][] = FileCat.parseTags(m_serv.getStorager(),args[1],null);
		Context context = new Context();
		context.m_minRel = args.length>2? StringUtil.toFloat(args[2],0): 0;
		context.m_maxN = args.length>3? StringUtil.toInt(args[3],0): 0;
		context.m_bBooleanAndMode = args.length>4? StringUtil.toBoolean(args[4],false): false;
		return listCatRels(Id.CATEGORYFEATURE,cats,context,0,1);//20070601
	}
	*/

	/*
	listfeatcats|<features>|<domain>|[<minRel>]|[<maxN>]|[<booleanMode>]
	- lists categories associated with features in <features> for domains in <domains>
	- returns line in list format, each line for individual category:
	id|evidence|relevance|POF|relativePOF|name
	*/
	/*
	public String doListFeatCats(String line,String args[]) throws Exception
	{
		String feats[] = args.length>1? StringUtil.toTokens(args[1]," ",false): null;
		if (feats==null || feats.length==0)
			return "No features.";
		Vector rels = new Vector();
		for (int i=0;i<feats.length;i++)
		{
			CatIns.lock(false);
			Relation rel = m_serv.getStorager().getRel(Id.FEATURE,feats[i],false);//case sensitive, no ignore case
			CatIns.unlock(false);
			if (rel!=null)
				rels.add(rel);
		}
		if (rels.size()==0)
			return "No known features.";
		int featIds[] = new int[rels.size()];
		for (int i=0;i<feats.length;i++)
			featIds[i] = ((Relation)rels.get(i)).getId();
		
		Context context = new Context();
		context.m_filterDomains = args.length>2? FileCat.parseDomArray(args[2]): null;
		context.m_minRel = args.length>3? StringUtil.toFloat(args[3],0): 0;
		context.m_maxN = args.length>4? StringUtil.toInt(args[4],0): 0;
		context.m_bBooleanAndMode = args.length>5? StringUtil.toBoolean(args[5],false): false;
		return listFeatRels(Id.CATEGORYFEATURE,featIds,context);
	}
	*/

	/*
	listcatdoms|<tags>|[<minRel>]|[<maxN>]|[<booleanMode>]
	- lists domains associated with categories listed in <tags> 
	- returns line in list format, each line for individual domain:
	id|evidence|relevance|POF|relativePOF|name
	*/
	/*
	public String doListCatDoms(String line,String args[]) throws Exception
	{
		int cats[][] = FileCat.parseTags(m_serv.getStorager(),args[1],null);
		Context context = new Context();
		context.m_minRel = args.length>2? StringUtil.toFloat(args[2],0): 0;
		context.m_maxN = args.length>3? StringUtil.toInt(args[3],0): 0;
		context.m_bBooleanAndMode = args.length>4? StringUtil.toBoolean(args[4],false): false;
		return listCatRels(Id.CATEGORYDOMAIN,cats,context,0,1);//20070601
	}
	*/

	/*
	listcatcats|<tags>|<domain>|[<minRel>]|[<maxN>]|[<booleanMode>]
	- lists categories associated with categories listed in <tags> under domain <domain>
	- returns line in list format, each line for individual category:
	id|evidence|relevance|POF|relativePOF|name
	*/
	/*
	public String doListCatCats(String line,String args[]) throws Exception
	{
		int cats[][] = FileCat.parseTags(m_serv.getStorager(),args[1],null);
		Context context = new Context();
		context.m_filterDomains = args.length>2? FileCat.parseDomArray(args[2]): null;
		context.m_minRel = args.length>3? StringUtil.toFloat(args[3],0): 0;
		context.m_maxN = args.length>4? StringUtil.toInt(args[4],0): 0;
		context.m_bBooleanAndMode = args.length>5? StringUtil.toBoolean(args[5],false): false;
		return listCatRels(Id.CATEGORYCATEGORY,cats,context,0,1);//20070601
	}
	*/
	
	/**
	 * 
	 * listcats|<domain>|[<minRel>]|[<maxN>]
	 * - lists categories under domain <domain>
	 * - answers on 'what are the categories under domain "SIZE"?'
	 */
	/*
	public String doListCats(String line,String args[]) throws Exception
	{
		Context context = new Context();
		context.m_filterDomains = args.length>1? FileCat.parseDomArray(args[1]): null;
		if (context.m_filterDomains==null || context.m_filterDomains.length!=1)
			return "Invalid number of domains.";
		context.m_minRel = args.length>2? StringUtil.toFloat(args[2],0): 0;
		context.m_maxN = args.length>3? StringUtil.toInt(args[3],0): 0;
		Relation rels[] = m_serv.getStorager().getRels(Id.CATEGORY,context.m_filterDomains[0],0); 
		return StringUtil.toString(TextCat.toRelevantItems(
				m_serv,rels,-1,context,-1,null,true,false),"\n");
	}
	*/

	/**
	 * 
	 * listdomcats|<domains>|[<domain>]|[<minRel>]|[<maxN>]|[<booleanMode>]
	 * - answers on 'what are the NOUN categories associating the domain "SIZE"?
	 * - answers on 'what are the NOUN categories associating the domains "SIZE" and/or "COLOR"?
	 */
	/*
	public String doListDomCats(String line,String args[]) throws Exception
	{
		int doms[] = args.length>1? FileCat.parseDomArray(args[1]): null;
		if (doms==null || doms.length<1)
			return "No domains.";
		Context context = new Context();
		context.m_filterDomains = args.length>2? FileCat.parseDomArray(args[2]): null;
		context.m_minRel = args.length>3? StringUtil.toFloat(args[3],0): 0;
		context.m_maxN = args.length>4? StringUtil.toInt(args[4],0): 0;
		context.m_bBooleanAndMode = args.length>5? StringUtil.toBoolean(args[5],false): false;
		return listFeatRels(Id.CATEGORYDOMAIN,doms,context);
	}
	*/

	/**
	 * 
	 * listcatcatsback|<tags>|[<domain>]|[<minRel>]|[<maxN>]|[<booleanMode>]
	 * - answers on 'what are the NOUN categories associating the category "SMALL" under domain "SIZE"?
	 * - answers on 'what are the NOUN categories associating the categories "SMALL" and/or "MEDIUM" under domain "SIZE"?
	 */
	/*
	public String doListCatCatsBack(String line,String args[]) throws Exception
	{
		int cats[][] = FileCat.parseTags(m_serv.getStorager(),args[1],null);
		Context context = new Context();
		context.m_filterDomains = args.length>2? FileCat.parseDomArray(args[2]): null;
		context.m_minRel = args.length>3? StringUtil.toFloat(args[3],0): 0;
		context.m_maxN = args.length>4? StringUtil.toInt(args[4],0): 0;
		context.m_bBooleanAndMode = args.length>5? StringUtil.toBoolean(args[5],false): false;
		return listCatRels(Id.CATEGORYCATEGORY,cats,context,1,0);//20070601
	}
	*/
	
	public String error(String result) throws Exception
	{
		return "Error="+result;
	}
	
	/**
	 * @param cmd
	 * @return
	 * @throws Exception
	 */
	public String handle(String cmd) throws Exception
	{
		String result;
		String args[] = StringUtil.toTokens(cmd,m_stringContext.m_cmdDelimeters,true);

		try {
			if (args[0].equalsIgnoreCase("login"))
			{
				result = m_authServer.createSession(args[1],args[2],m_stringContext);//login,password
				if (result == null)
					result = error("Invalid login"); 
			}
			else
			if (args.length<2)
				result = error("Insufficient arguments");
			else
			{
				AuthSession session = m_authServer.getSession(args[1]);//sessionid
				if (session != null)
				{
					result = session.handle(args);
					if (result == null)
						result = error("Invalid request"); 
				}
				else
					result = error( cmd ); 
			}
		} catch (Exception e) {
			result = error( e.toString() );
e.printStackTrace();			
		}
		
		return result; 
	}

}
