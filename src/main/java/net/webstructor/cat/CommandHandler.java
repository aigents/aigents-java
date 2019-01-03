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
package net.webstructor.cat;

import net.webstructor.main.Logger;

public class CommandHandler
{
		static final String m_delims = ",;"; //",;"; // use " " for legacy HTML JS stuff
		
        static Server s_server = null;
        static String m_path = null;

        Interactor m_itr;
        String m_input;
        String m_output = null;
        String m_lineBreak;
        String m_fieldBreak;
        String m_elementBreak;

        public static Server getServer(String path)
        {
            m_path = path;
            return getServer();
        }

        static Server getServer()
        {
            if (s_server == null)
            {
                //m_path = Application.UserAppDataPath;//this points to Local Settings...
                //m_path = Application.CommonAppDataPath;//this points to Local Settings...
                //String path = Application.ExecutablePath;//explodes under IIS
                //String path = System.Reflection.Assembly.GetExecutingAssembly().Location;//under IIS, points to dll in temporary folder under .NET framework location
                //int slash = path.LastIndexOf('\\');
                //path = path.Substring(0,slash+1)+"ELE.txt";
                if (m_path==null || m_path.length() == 0)
                    m_path=".";
                s_server = BasicServer.getInstance(m_path+"/asset.txt");
            }
            return s_server;
        }

        public CommandHandler(String input,String lbr,String fbr,String ebr)
		{
            m_input = input;
            m_lineBreak = lbr;
            m_fieldBreak = fbr;
            m_elementBreak = ebr;
            m_itr = getServer().getInteractor();
        }

        synchronized String handle(String[] args) throws Exception
        {
            if (args==null || args.length==0 || args[0]==null)
                return "No input.";
            
            String head = args[0].toLowerCase();

            if (head.equals("getcats")) 
                return StringUtil.toString(m_itr.getCats(StringUtil.toInt(args[1]),args.length>2?StringUtil.toInt(args[2]):-1,args.length>3?StringUtil.toInt(args[3]):-1),m_lineBreak);
            else
            if (head.equals("addcat")) 
                return StringUtil.toString(m_itr.addCat(StringUtil.toInt(args[1]),args[2]));
            else 
            if (head.equals("delcat")) 
                return StringUtil.toString(m_itr.delCat(StringUtil.toInt(args[1])));
            else 
            if (head.equals("getdoccnt")) 
                return StringUtil.toString(m_itr.getDocCnt(StringUtil.toInt(args[1])));
            else 
            if (head.equals("getdocs")) 
                return StringUtil.toString(m_itr.getDocs(StringUtil.toInt(args[1])),m_lineBreak);
            else 
            if (head.equals("getdocsfromto")) 
                return StringUtil.toString(m_itr.getDocs(StringUtil.toInt(args[1]),StringUtil.toInt(args[2]),StringUtil.toInt(args[3])),m_lineBreak);
            else 
            if (head.equals("adddoc")) 
                return StringUtil.toString(m_itr.addDoc(StringUtil.toInt(args[1]),StringUtil.toStringFrom(args,2),true,true));           
            if (head.equals("adddocgetcats")) 
                return StringUtil.toString(m_itr.addDocGetCats(StringUtil.toInt(args[1]),StringUtil.toInt(args[2]),StringUtil.toStringFrom(args,3)),m_lineBreak);
            else 
            if (head.equals("deldoc")) 
                return StringUtil.toString(m_itr.delDoc(StringUtil.toInt(args[1])));
            else
            if (head.equals("updatedoc")) 
                return StringUtil.toString(m_itr.updateDoc(StringUtil.toInt(args[1]),false));
            else  
            if (head.equals("getdocfeatures")) 
                return StringUtil.toString(m_itr.getDocFeatures(StringUtil.toInt(args[1]),args.length>2?StringUtil.toInt(args[2]):0),m_lineBreak);
            else
            if (head.equals("adddocfeature")) 
                return StringUtil.toString(m_itr.addDocFeature(StringUtil.toInt(args[1]),args[2],StringUtil.toInt(args[3])));
            else 
            if (head.equals("setdocfeature")) 
                return StringUtil.toString(m_itr.setDocFeature(StringUtil.toInt(args[1]),StringUtil.toInt(args[2])));
            else 
            if (head.equals("getcatfeatures")) 
                return StringUtil.toString(m_itr.getCatFeatures(StringUtil.toInt(args[1]),args.length>2?StringUtil.toInt(args[2]):0),m_lineBreak);
            else 
            if (head.equals("getcatdocfeatures")) 
                return StringUtil.toString(m_itr.getCatDocFeatures(StringUtil.toInt(args[1]),args.length>2?StringUtil.toInt(args[2]):0),m_lineBreak);
            else 
                if (head.equals("addcatfeature")) 
                return StringUtil.toString(m_itr.addCatFeature(StringUtil.toInt(args[1]),args[2],StringUtil.toInt(args[3])));
            else 
            if (head.equals("setcatfeature")) 
                return StringUtil.toString(m_itr.setCatFeature(StringUtil.toInt(args[1]),StringUtil.toInt(args[2])));
            else 
            if (head.equals("getcatdocs")) 
                return StringUtil.toString(m_itr.getCatDocs(StringUtil.toInt(args[1]),args.length>2?StringUtil.toInt(args[2]):0,args.length>3?StringUtil.toInt(args[3]):0),m_lineBreak);
            else 
            if (head.equals("getdoccats")) 
                return StringUtil.toString(m_itr.getDocCats(StringUtil.toInt(args[1]),args.length>2?StringUtil.toInt(args[2]):0,args.length>3?StringUtil.toInt(args[3]):0),m_lineBreak);
            else 
            if (head.equals("adddoccat")) 
                return StringUtil.toString(m_itr.addDocCat(StringUtil.toInt(args[1]),StringUtil.toInt(args[2]),StringUtil.toInt(args[3])));
            else 
            if (head.equals("setdoccat")) 
                return StringUtil.toString(m_itr.setDocCat(StringUtil.toInt(args[1]),StringUtil.toInt(args[2])));
            else 
            if (head.equals("getdocdata")) 
                return m_itr.getDocData(StringUtil.toInt(args[1]));
            else 
            if (head.equals("getcatdocdata")) 
                return m_itr.getCatDocData(StringUtil.toInt(args[1]));
            else 
            if (head.equals("getcatdochighlight")) 
                return m_itr.getCatDocHighlight(StringUtil.toInt(args[1]));
            else 
            if (head.equals("getdoctokennames")) 
                return StringUtil.toString(m_itr.getDocTokenNames(StringUtil.toInt(args[1]))," ");
            else 
            if (head.equals("getdochighlight")) 
                return m_itr.getDocHighlight(StringUtil.toInt(args[1]),args.length>2?StringUtil.toInt(args[2]):0);
            else 
            if (head.equals("updatecat")) 
                return StringUtil.toString(m_itr.updateCat(StringUtil.toInt(args[1]),args.length>2?StringUtil.toBoolean(args[2],true):true));
            else 
            if (head.equals("getdomains")) 
                return StringUtil.toString(m_itr.getDomains(),m_lineBreak);
            else 
            if (head.equals("getsources")) 
                return StringUtil.toString(m_itr.getSources(),m_lineBreak);

            //backdoor hacks: 
            else 
            if (head.equals("commit")) 
                return StringUtil.toString(BasicServer.getInstance().getStorager().commit());
            if (head.equals("addnamesbatch")) 
                return StringUtil.toString(((BasicStorager)BasicServer.getInstance().getStorager()).addNamesBatch(StringUtil.toInt(args[1]),args[2],StringUtil.toInt(args[3])));
            else
            if (head.equals("adddocsbatch")) 
                return StringUtil.toString(((BasicProcessor)BasicServer.getInstance().getProcessor()).addDocsBatch(StringUtil.toInt(args[1]),args[2]));
            else
            if (head.equals("addcatsbatch")) 
                return StringUtil.toString(((BasicProcessor)BasicServer.getInstance().getProcessor()).addCatsBatch(StringUtil.toInt(args[1]),args[2]));
            else
                return "Wrong function.";
        }

        /// <summary>
        ///  
        /// </summary>
        /// <returns>true if completed, false if not</returns>
        public boolean run()
        {
            try {                
                Logger l = Logger.getLogger();
            	long ticket = l.logIn(m_input);//synchronized!
                String[] input = StringUtil.toTokens( m_input, m_delims );
                m_output = handle(input);//synchronized!
                l.logOut( StringUtil.first(m_output,80), ticket);//synchronized!
            } catch (Exception e) {
                m_output = "Error: "+e.toString();
                e.printStackTrace();
            }
            return true;
        }   


        public String getOutput()
        {
            return m_output;
        }   

}
