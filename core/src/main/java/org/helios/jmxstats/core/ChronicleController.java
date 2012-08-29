/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2007, Helios Development Group and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org. 
 *
 */
package org.helios.jmxstats.core;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

import vanilla.java.chronicle.Excerpt;
import vanilla.java.chronicle.impl.IndexedChronicle;

/**
 * <p>Title: ChronicleController</p>
 * <p>Description: Singleton controller for managing the jmxstats chronicle</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jmxstats.core.ChronicleController</code></p>
 */

public class ChronicleController {
	/** The chronicle file name */
	protected final String chronicleName;
	/** The chronicle parth */
	protected final String chroniclePath;	
	/** The chronicle */
	protected final IndexedChronicle chronicle;
	/** The number of entries in the chronicle */
	protected final AtomicLong entryCount = new AtomicLong(0);
	
	
	
	/** The singleton instance */
	private static volatile ChronicleController instance = null;
	/** The singleton instance ctor lock */
	private static final Object lock = new Object();
	
	/** The system property that defines the chronicle name */
	public static final String CHRONICLE_PROP = "org.helios.jmxstat.chronicle.name";
	/** The default chronicle name */
	public static final String DEFAULT_CHRONICLE_NAME = "jmxstats";
	/** The chronicle home directory */
	public static final File CHRONICLE_HOME_DIR = new File(System.getProperty("user.home") + File.separator + ".jmxstats");
	/** The default chronicle databit size estimate */
	public static final int CHRONICLE_SIZE_EST = 100;
	
	/**
	 * Acquires the ChronicleController singleton instance
	 * @return the ChronicleController singleton instance
	 */
	public static ChronicleController getInstance() {
		if(instance == null) {
			synchronized(lock) {
				if(instance == null) {
					instance = new ChronicleController();
				}
			}
		}
		return instance;
	}
	
	private ChronicleController() {
		chronicleName = System.getProperty(CHRONICLE_PROP, DEFAULT_CHRONICLE_NAME);
		if(!CHRONICLE_HOME_DIR.exists()) {
			if(!CHRONICLE_HOME_DIR.mkdir()) {
				throw new RuntimeException("Failed to create jmxstats home directory [" + CHRONICLE_HOME_DIR + "]", new Throwable());
			}
		} else {
			if(!CHRONICLE_HOME_DIR.isDirectory()) {
				throw new RuntimeException("jmxstats home directory [" + CHRONICLE_HOME_DIR + "] is a file not a directory", new Throwable());
			}
		}
		chroniclePath = CHRONICLE_HOME_DIR + File.separator + chronicleName;
		try {
			chronicle = new IndexedChronicle(chroniclePath, CHRONICLE_SIZE_EST);
		} catch (IOException e) {
			throw new RuntimeException("Failed to create chronicle on path [" + chroniclePath + "]", e);
		}
		log("Initialized chronicle [" + chronicle.name() + "] on path [" + chroniclePath + "] with size [" + chronicle.size() + "]");
		if(chronicle.size()==0) initControlBlock();
		else updateEntryCount();
	}
	
	private void updateEntryCount() {
		Excerpt<IndexedChronicle> ex = chronicle.createExcerpt();
		ex.index(0);
		long cnt = ex.readLong();
		entryCount.set(cnt);
		log("Read Entry Count [" + cnt + "]");
	}
	
	private void initControlBlock() {
		Excerpt<IndexedChronicle> ex = chronicle.createExcerpt();
		ex.startExcerpt(8);
		ex.writeLong(entryCount.get());
		ex.finish();
		log("Created ControlBlock [" + ex.index() + "]");
	}
	
	/**
	 * Console logger
	 * @param msg The message to log
	 */
	public static void log(Object msg) {
		System.out.println(msg);
	}
	
}
