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
package test.org.helios.jmxstats.perf;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPInputStream;

import org.helios.jmxstats.core.Controller.SystemClock;
import org.helios.jmxstats.core.Controller.SystemClock.ElapsedTime;

/**
 * <p>Title: NameCachePerformance</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>test.org.helios.jmxstats.perf.NameCachePerformance</code></p>
 */

public class NameCachePerformance {
	/** The words source file */
	public static final String FILE_NAME = "src/test/resources/words.txt.gz";
	/** The number of words to load */
	public static final int WORD_COUNT = 70000;
	/** The minimum word size */
	public static final int WORD_SIZE = 7;
	/** The warmup loop count */
	public static final int WARMUP_LOOPS = 10;
	/** The measurement loop count */
	public static final int LOOPS = 1;
	/** The lookup measurement loop count */
	public static final int LOOKUP_LOOPS =2;
	
	private static int maxBufferSize = 0;
	
	/** The name cache */
	private static final Map<CharSequence, Long> namecache = new ConcurrentHashMap<CharSequence, Long>(WORD_COUNT, 0.5f, 1);
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		log("NameCachePerformance\n\tLoading words...");
//		SystemClock.startTimer();
//		Set<String> words = loadWords(FILE_NAME, WORD_COUNT, WORD_SIZE);
//		ElapsedTime et = SystemClock.endTimer();
//		log("Loaded " + WORD_COUNT + " words: " + et);
		log("Running Simple String Test");
		for(int i = 0; i < WARMUP_LOOPS; i++) {
			testStringType(loadWords(FILE_NAME, WORD_COUNT, WORD_SIZE));
			namecache.clear();
		}
		log("Warmup Complete");		
		for(int i = 0; i < LOOPS; i++) {
			long[] results = testStringType(loadWords(FILE_NAME, WORD_COUNT, WORD_SIZE));
			log("RESULTS:  Elapsed ms:" + results[0] + "  Heap MB:" + results[1] + "  Lookup Time (ms):" + results[2] + "  Lookup Per (ns):" + results[3]);
		}
		namecache.clear();
		log("Running Heap CharBuffer Test");
		for(int i = 0; i < WARMUP_LOOPS; i++) {
			testCharBufferType(loadWords(FILE_NAME, WORD_COUNT, WORD_SIZE));
			namecache.clear();
		}
		log("Warmup Complete");
		namecache.clear();
		for(int i = 0; i < LOOPS; i++) {
			long[] results = testCharBufferType(loadWords(FILE_NAME, WORD_COUNT, WORD_SIZE));
			log("RESULTS:  Elapsed ms:" + results[0] + "  Heap MB:" + results[1] + "  Lookup Time (ms):" + results[2] + "  Lookup Per (ns):" + results[3]);
		}
		log("Running Direct CharBuffer Test");
		namecache.clear();
		for(int i = 0; i < WARMUP_LOOPS; i++) {
			testDirectCharBufferType(loadWords(FILE_NAME, WORD_COUNT, WORD_SIZE));
			namecache.clear();
		}
		log("Warmup Complete");
		namecache.clear();
		for(int i = 0; i < LOOPS; i++) {
			long[] results = testDirectCharBufferType(loadWords(FILE_NAME, WORD_COUNT, WORD_SIZE));
			log("RESULTS:  Elapsed ms:" + results[0] + "  Heap MB:" + results[1] + "  Lookup Time (ms):" + results[2] + "  Lookup Per (ns):" + results[3]);
		}
		
	}
	
	public static final int MB = 1024 * 1024;
	
	protected static long getCurrentHeapUsage() {
		System.gc();
		try { Thread.currentThread().join(500); } catch (Exception e) {}
		System.gc();
		try { Thread.currentThread().join(500); } catch (Exception e) {}
		return ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed()/MB;
	}
	
	protected static long[] testStringType(final Set<String> words) {
		long[] results = new long[4];		
		SystemClock.startTimer();
		long index = 0;
		for(Iterator<String> iter = words.iterator(); iter.hasNext();) {
			namecache.put(iter.next(), index);
			iter.remove();
			index++;
		}
		ElapsedTime et = SystemClock.endTimer();
		results[0] = et.elapsedMs;
		results[1] = getCurrentHeapUsage();
		
		Set<String> lookups = loadWords(FILE_NAME, WORD_COUNT, WORD_SIZE);
		SystemClock.startTimer();
		for(int i = 0; i < LOOKUP_LOOPS; i++) {
			for(String s: lookups) {
				if(namecache.get(s)==null) {
					throw new RuntimeException("Lookup returned null", new Throwable());
				}
			}
		}
		et = SystemClock.endTimer();
		results[2] = et.elapsedMs;
		results[3] = et.avgNs(LOOKUP_LOOPS * WORD_COUNT);
		getCurrentHeapUsage();
		return results;
	}
	
	protected static long[] testCharBufferType(final Set<String> words) {
		long[] results = new long[4];		
		SystemClock.startTimer();
		long index = 0;
		for(Iterator<String> iter = words.iterator(); iter.hasNext();) {
			namecache.put(CharBuffer.wrap(iter.next()), index);
			iter.remove();
			index++;
		}
		ElapsedTime et = SystemClock.endTimer();
		results[0] = et.elapsedMs;
		results[1] = getCurrentHeapUsage();
		
		Set<String> lookups = loadWords(FILE_NAME, WORD_COUNT, WORD_SIZE);
		SystemClock.startTimer();
		for(int i = 0; i < LOOKUP_LOOPS; i++) {
			for(String s: lookups) {
				CharBuffer cb = CharBuffer.wrap(s);
				cb.asReadOnlyBuffer().flip();				
				if(namecache.get(cb)==null) {
					throw new RuntimeException("Lookup returned null", new Throwable());
				}
			}
		}
		et = SystemClock.endTimer();
		results[2] = et.elapsedMs;
		results[3] = et.avgNs(LOOKUP_LOOPS * WORD_COUNT);
		getCurrentHeapUsage();
		return results;
	}
	
	protected static long[] testDirectCharBufferType(final Set<String> words) {
		long[] results = new long[4];		
		SystemClock.startTimer();
		long index = 0;
		for(Iterator<String> iter = words.iterator(); iter.hasNext();) {			
			namecache.put(ByteBuffer.allocateDirect(maxBufferSize).asCharBuffer().append(iter.next()), index);
			iter.remove();
			index++;
		}
		ElapsedTime et = SystemClock.endTimer();
		results[0] = et.elapsedMs;
		results[1] = getCurrentHeapUsage();
		
		Set<String> lookups = loadWords(FILE_NAME, WORD_COUNT, WORD_SIZE);
		SystemClock.startTimer();
		for(int i = 0; i < LOOKUP_LOOPS; i++) {
			for(String s: lookups) {
				CharBuffer cb = ByteBuffer.allocateDirect(maxBufferSize).asCharBuffer().append(s);
				cb.asReadOnlyBuffer().flip();
				if(namecache.get(cb)==null) {
					throw new RuntimeException("Lookup returned null", new Throwable());
				}
			}
		}
		et = SystemClock.endTimer();
		results[2] = et.elapsedMs;
		results[3] = et.avgNs(LOOKUP_LOOPS * WORD_COUNT);
		getCurrentHeapUsage();
		return results;
	}
	
	
	public static void log(Object msg) {
		System.out.println(msg);
	}
	
	public static Set<String> loadWords(String fileName, int numberOfWords, int minLength) {
		Set<String> words = new HashSet<String>(numberOfWords);
		FileInputStream fis = null;
		GZIPInputStream gis = null;
		BufferedReader bis = null;
		int maxSize = 0;
		try {
			File f = new File(fileName);
			if(!f.canRead()) throw new Exception("Unable to read file [" + fileName + "]", new Throwable());
			fis = new FileInputStream(f);
			gis = new GZIPInputStream(fis);
			bis = new BufferedReader(new InputStreamReader(gis));
			int loadedWords = 0;
			String word = null;
			while((word = bis.readLine()) != null) {
				if(word.length()>=minLength) {
					words.add(word);
					if(word.length()>maxSize) maxSize = word.length();
					loadedWords++;
					if(loadedWords==numberOfWords) break;
				}
			}
			if(loadedWords<numberOfWords) throw new Exception("Failed to load [" + numberOfWords + "]. Insuffucient data. Loaded [" + loadedWords + "]", new Throwable());
			maxBufferSize = maxSize*2;
			return words;
		} catch (Exception e) {
			throw new RuntimeException("Failed to read words", e);
		} finally {
			try { bis.close(); } catch (Exception ex) {}
			try { fis.close(); } catch (Exception ex) {}
		}		
	}

}
