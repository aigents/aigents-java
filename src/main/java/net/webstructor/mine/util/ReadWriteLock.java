/*
 * Copyright 2005-2008, Anton Kolonin,
 * All rights reserved.
 *
 * This software is proprietary information belonging to Anton Kolonin. 
 * You shall not disclose this and shall not use it in any way 
 * without of written permission granted by Anton Kolonin.
 */
package net.webstructor.mine.util;

public class ReadWriteLock 
{
		private int readers;
		private boolean writer;
		private Object synchronizeObject;

		public ReadWriteLock(Object obj)
		{
			readers = 0;
			writer = false;
			synchronizeObject = obj;
		}

		public ReadWriteLock()
		{
			this( new Object() );
		}

		public void readLock() throws InterruptedException
		{
			while( true )
			{
				synchronized (synchronizeObject)
				{
					if( !writer ) // no writer
					{
						readers++;
						break;
					}
					synchronizeObject.wait();
				}
			}
		}

		public void readUnlock()
		{
			synchronized (synchronizeObject)
			{
				readers--;
				if ( readers == 0 )
				{
					synchronizeObject.notifyAll();
				}
			}
		}
	

		public void writeLock() throws InterruptedException
		{
			while( true )
			{
				synchronized (synchronizeObject)
				{
					if( !writer && (readers == 0 )) // no writer or readers
					{
						writer = true;
						break;
					}
					synchronizeObject.wait();
				}
			}
		}
	
		public void writeUnlock()
		{
			synchronized (synchronizeObject)
			{
				writer = false;
				synchronizeObject.notifyAll();
			}
		}
}
