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

public class BasicServer implements Server //, IDisposable
{
        static BasicServer m_server = null;
 
        Reader[] m_readers = null;
        Calculator m_calculator = null;
        Tokenizer m_tokenizer = null;
        Storager m_storager = null;
        Processor m_processor = null;
        Interactor m_interactor = null;

        protected BasicServer()
        {
            init("./webcat.txt");
        }

        protected BasicServer(String path)
        {
            init(path);
        }

        void init(String path) 
        {
        	try {
	            m_storager = new BasicStorager();
	            m_storager.startUp(path);
	
	            m_readers = new Reader[3];
	            m_readers[0] = new HttpFileReader();
	            m_readers[1] = new TextFileReader();
	            m_readers[2] = new TextStringReader();
	
	            m_tokenizer = new BasicTokenizer(this);
	            m_calculator = new BasicCalculator(this);
	            m_processor = new BasicProcessor(this);
	            m_interactor = new BasicInteractor(this);
        	} catch (Exception e) {
        		System.out.println(e.getMessage());
        		e.printStackTrace();
        	}
        }
        //@Override
        public void finalize()
        {
        	try {
        		getStorager().shutDown();
        	} catch (Exception e) {
        		System.out.println(e.getMessage());
        		e.printStackTrace();
        	}
            m_server = null;
            // help gc
            m_interactor = null;
            m_processor = null;
            m_storager = null;
            m_calculator = null;
            m_tokenizer = null;

            m_readers[0] = null;
            m_readers[1] = null;
            m_readers = null;
        }

        public static Server getInstance()
        {
            if (m_server == null)
                m_server = new BasicServer();
            return m_server;
        }

        public static Server getInstance(String connection)
        {
            if (m_server == null)
                m_server = new BasicServer(connection);
            return m_server;
        }

        public Reader[] getReaders()
        {
            return m_readers;
        }

        public Tokenizer getTokenizer()
        {
            return m_tokenizer;
        }

        public Interactor getInteractor()
        {
            return m_interactor;
        }

        public Processor getProcessor()
        {
            return m_processor;
        }

        public Calculator getCalculator()
        {
            return m_calculator;
        }

        public Storager getStorager()
        {
            return m_storager;
        }
}
