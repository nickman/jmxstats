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

import java.lang.management.ManagementFactory;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;


/**
 * <p>Title: Controller</p>
 * <p>Description: The controller and primary API interface for jmxstats</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jmxstats.core.Controller</code></p>
 */

public class Controller {
	/** The singleton instance */
	private static volatile Controller instance = null;
	/** The singleton instance ctor lock */
	private static final Object lock = new Object();
	
	/**
	 * Returns the Controller singleton
	 * @return the Controller singleton
	 */
	public static Controller getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new Controller();
				}
			}
		}
		return instance;
	}
	
	private Controller() {
		
	}
	
	/**
	 * Returns the metric interval for this JVM instance (ms.)
	 * @return the metric interval for this JVM instance
	 */
	public long getInterval() {
		return SystemClock.INTERVAL;
	}
	
	/**
	 * Changes the metric interval: <b>DON'T CALL THIS UNLESS YOU KNOW WHAT YOU'RE DOING</b>.
	 * @param interval The new interval.
	 */
	@SuppressWarnings("unused")
	private void setInterval(long interval) {
		if(interval<1) throw new IllegalArgumentException("The passed interval [" + interval + "] was <1", new Throwable());
		SystemClock.INTERVAL = interval;
	}
	
	/**
	 * Boostrap
	 * @param args None
	 */
	public static void main(String[] args) {
		final SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss,S");
		log("Controller\n\tStart Time:" + sdf.format(new Date(SystemClock.roundDownTime())));
//		SystemClock.setCurrentClock(SystemClock.NANO);
//		int loopCount = 100000;
//		long total = 0;
//		long currentTime = SystemClock.time();
//		for(int i = 0; i < loopCount; i++) {
//			//total += System.currentTimeMillis()%SystemClock.INTERVAL;
//			//total += currentTime%SystemClock.INTERVAL;
//			total += SystemClock.time()%SystemClock.INTERVAL;
//		}
//		log("Warmup Complete");
//		total = 0;
//		currentTime = SystemClock.time();
//		SystemClock.startTimer();
//		for(int i = 0; i < loopCount; i++) {
//			//total += System.currentTimeMillis()%SystemClock.INTERVAL;
//			//total += currentTime%SystemClock.INTERVAL;
//			total += SystemClock.time()%SystemClock.INTERVAL;
//		}
//		ElapsedTime et = SystemClock.endTimer();
//		log("Test Complete:\n\tTotal:" + total + "\n\t"  + et + "\n\tAverage ns:" + et.avgNs(loopCount));
		Controller.getInstance().addIntervalListener(new IntervalListener() {
			final SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss,S");			
			@Override
			public void onIntervalSwitch(long intervalId, long startTime, long endTime) {
				log("\n\t=========================================\n\tInterval Switch:" + intervalId + "\n\tStart:" + sdf.format(new Date(startTime)) + "\n\tEnd:" + sdf.format(new Date(endTime)) + "\n\t=========================================");
			}
		});		
		try { Thread.currentThread().join(); } catch (Exception e) {}
		
		
	}
	
	/** A set of registered interval listeners */
	private static final Set<IntervalListenerRunnable> listeners = new CopyOnWriteArraySet<IntervalListenerRunnable>();
	
	/**
	 * Registers a new {@link IntervalListener}
	 * @param listener the {@link IntervalListener} to register
	 */
	public void addIntervalListener(IntervalListener listener) {
		if(listener!=null) {
			listeners.add(new IntervalListenerRunnable(listener));
		}
	}
	
	/**
	 * <p>Title: IntervalListenerRunnable</p>
	 * <p>Description: A runnable wrapped interval listener for submitting to an executor</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.jmxstats.core.Controller.IntervalListenerRunnable</code></p>
	 */
	private static class IntervalListenerRunnable implements Callable<Void> {
		/** The wrapped listener */
		private final IntervalListener listener;
		static private long _intervalId = 0, _startTime = 0, _endTime = 0;
		
		/**
		 * Updates the interval data
		 * @param intervalId The interval ID
		 * @param startTime The new interval start time
		 * @param endTime The new interval end time
		 */
		public static void update(long intervalId, long startTime, long endTime) {
			_intervalId = intervalId;
			_startTime = startTime;
			_endTime = endTime;
		}

		/**
		 * Creates a new IntervalListenerRunnable
		 * @param listener The listener to wrap
		 */
		public IntervalListenerRunnable(IntervalListener listener) {
			this.listener = listener;
		}
		
		public Void call() {
			listener.onIntervalSwitch(_intervalId, _startTime, _endTime);
			return null;
		}

		/**
		 * {@inheritDoc}
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((listener == null) ? 0 : listener.hashCode());
			return result;
		}

		/**
		 * {@inheritDoc}
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			IntervalListenerRunnable other = (IntervalListenerRunnable) obj;
			if (listener == null) {
				if (other.listener != null)
					return false;
			} else if (!listener.equals(other.listener))
				return false;
			return true;
		}
	}
	
	/**
	 * Console logger
	 * @param msg The message to log
	 */
	public static void log(Object msg) {
		System.out.println(msg);
	}
	
	/**
	 * <p>Title: SystemClock</p>
	 * <p>Description: A synthetic time provider that can be manipulated for testing or provide batch times for performance.</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * @version $LastChangedRevision$
	 * <p><code>org.helios.jmxstats.core.Controller.SystemClock</code></p>
	 */
	public enum SystemClock {
		/** Clock that delegates directly to {@link System#currentTimeMillis()} */
		DIRECT(new DirectClock()),
		/** Clock that calculates the current time as an offset from the JVM's start time */
		NANO(new NanoClock()),
		/** A test clock for which the current time can be specified */
		TEST(new TestClock());
		
		/** The thread group for system clock threads */
		private static final ThreadGroup SystemClockThreadGroup = new ThreadGroup("SystemClock");
		/** Thread pool for executing interval actions */
		private static final ExecutorService executor = Executors.newCachedThreadPool(new ThreadFactory(){
			private final AtomicInteger serial = new AtomicInteger(0);
			@Override
			public Thread newThread(Runnable r) {
				Thread t = new Thread(SystemClockThreadGroup, r, "SystemClockWorker#" + serial.incrementAndGet());
				t.setDaemon(true);
				return t;
			}
		});
		
		/** The interval property name */
		public static final String INTERVAL_PROP = "org.helios.interval"; 
		/** The database time interval */
		private static long INTERVAL;
		/** Tracks the current interval ID */
		private static final AtomicLong INTERVAL_ID = new AtomicLong(0L);
		/** The current interval start time */
		private static final AtomicLong CURRENT_INTERVAL_START = new AtomicLong(0L);
		/** The current interval end time */
		private static final AtomicLong CURRENT_INTERVAL_END = new AtomicLong(0L);
		
		/** Startup latch */
		private static final CountDownLatch startupLatch = new CountDownLatch(1); 
		
		
		/** Runnable that awaits the interval period , then increments the interval and drops the interval barrier */
		private static final Runnable intervalRunnable = new Runnable() {
			public void run() {
				try {
					startupLatch.await();
				} catch (InterruptedException ex) {
					// HUH ?
				}
				while(true) {
					try {
						intervalThread.join(INTERVAL);					
						intervalActionThread.interrupt();
					} catch (InterruptedException ex) {
						Thread.interrupted();
					}
				}
			}			
		};
		
		/** Runnable that waits on the interval trip and then initiates execution of listener notifications  */
		private static final Runnable intervalAction = new Runnable() {
			public void run() {
				incrementIntervalTime(SystemClock.roundDownTime());
				startupLatch.countDown();
				while(true) {
					try {					
						Thread.currentThread().join();						
					} catch (InterruptedException ex) {
						Thread.interrupted();
						if(INTERVAL_ID.get()==Long.MAX_VALUE) {
							INTERVAL_ID.set(0);
						}
						long newInterval = INTERVAL_ID.incrementAndGet();
						long[] times = incrementIntervalTime();
						if(!listeners.isEmpty()) {
							IntervalListenerRunnable.update(newInterval, times[0], times[1]);
							try {
								executor.invokeAll(listeners);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
					}
				}
			}			
		};


		
		/** Interval thread to compute the new interval ID every interval */
		private static final Thread intervalThread = new Thread(SystemClockThreadGroup, intervalRunnable, "IntervalThread", 1024);
		/** Interval thread to dispatch interval events */
		private static final Thread intervalActionThread = new Thread(SystemClockThreadGroup, intervalAction, "IntervalAction");
		
		/**
		 * Initializes the interval window
		 * @param current The current start time 
		 */
		private static long[] incrementIntervalTime(long current) {
			long nextStart = current;
			long nextEnd = nextStart + INTERVAL;

			CURRENT_INTERVAL_START.set(nextStart);
			CURRENT_INTERVAL_END.set(nextEnd);
			return new long[]{nextStart, nextEnd};
		}
		
		/**
		 * Updates the interval window 
		 */
		private static long[] incrementIntervalTime() {
			long nextStart = CURRENT_INTERVAL_END.get()+1;
			long nextEnd = nextStart + INTERVAL -1;
			CURRENT_INTERVAL_START.set(nextStart);
			CURRENT_INTERVAL_END.set(nextEnd);
			return new long[]{nextStart, nextEnd};
			
			
		}
		
		
		/**
		 * Resets the interval thread
		 * @param interval The new interval in ms.
		 */
		private static void resetIntervalThread(long interval) {
			INTERVAL = interval;
			intervalThread.interrupt();
			INTERVAL_ID.set(0);
		}
		
		static {
			try {
				INTERVAL = Long.parseLong(System.getProperty(INTERVAL_PROP, "15000"));
			} catch (Exception e) {
				INTERVAL = 15000;
			}
			intervalThread.setDaemon(true);
			intervalThread.setPriority(Thread.MAX_PRIORITY);
			intervalActionThread.setDaemon(true);
			intervalActionThread.setPriority(Thread.NORM_PRIORITY);
			intervalActionThread.start();
			intervalThread.start();
		}
		

		
		public boolean isCurrentInterval(final long time) {
			final long now = time();
			if(now-time>=INTERVAL) return false;
			return false;
		}
		
		
		private SystemClock(Clock clock) {
			this.clock = clock;
		}
		
		private final Clock clock;
		
		public static long time() {
			return currentClock.get().getTime();
		}
		
		public static long roundDownTime() {
			long time = currentClock.get().getTime();
			long over = time%INTERVAL;
			return time-over;
		}
		
		
		public static SystemClock currentClock() {
			return currentClock.get();
		}
		
		public static void setCurrentClock(SystemClock clock) {
			if(clock==null) throw new IllegalArgumentException("SystemClock cannot be set to null", new Throwable());
			currentClock.set(clock);
		}
		
		
		/**
		 * Returns the current time from this clock
		 * @return the current time
		 */
		public long getTime() {
			return clock.time();
		}
		
		/** The VM start time in ms. */
		public static final long startTime = ManagementFactory.getRuntimeMXBean().getStartTime();
		/** The test time */
		private static final AtomicLong testTime = new AtomicLong(0L);
		/** A reference to the current clock impl. */
		private static final AtomicReference<SystemClock> currentClock = new AtomicReference<SystemClock>(DIRECT);
		/** Holds the start timestamp of an elapsed time measurement */
		private static final ThreadLocal<long[]> timerStart = new ThreadLocal<long[]>() {
			protected long[] initialValue() {
				return new long[1];
			}
		};
		
		
		/**
		 * Starts an elapsed timer for the current thread
		 * @return the start time in ns.
		 */
		public static long startTimer() {
			long st = System.nanoTime();
			timerStart.get()[0] = st;
			return st;
		}
		
		public static ElapsedTime endTimer() {
			return ElapsedTime.newInstance(System.nanoTime());
		}
		
		public static ElapsedTime lapTimer() {
			return ElapsedTime.newInstance(true, System.nanoTime());
		}
		
		
		/**
		 * <p>Title: ElapsedTime</p>
		 * <p>Description: Encapsulates various values associated to a timer's elapsed time.</p> 
		 */
		public static class ElapsedTime {
			public final long startNs;
			public final long endNs;
			public final long elapsedNs;
			public final long elapsedMs;
			public volatile long lastLapNs = -1L;
			public volatile long elapsedSinceLastLapNs = -1L;
			public volatile long elapsedSinceLastLapMs = -1L;
			/** Holds the start last lap of an elapsed time measurement */
			private static final ThreadLocal<long[]> lapTime = new ThreadLocal<long[]>();
		
			static ElapsedTime newInstance(long endTime) {
				return newInstance(false, endTime);
			}
			
			static ElapsedTime newInstance(boolean lap, long endTime) {
				return new ElapsedTime(lap, endTime);
			}
			
			
			public static final Map<TimeUnit, String> UNITS;
			
			static {
				Map<TimeUnit, String> tmp = new HashMap<TimeUnit, String>();
				tmp.put(TimeUnit.DAYS, "days");
				tmp.put(TimeUnit.HOURS, "hrs.");
				tmp.put(TimeUnit.MICROSECONDS, "us.");
				tmp.put(TimeUnit.MILLISECONDS, "ms.");
				tmp.put(TimeUnit.MINUTES, "min.");
				tmp.put(TimeUnit.NANOSECONDS, "ns.");
				tmp.put(TimeUnit.SECONDS, "s.");
				UNITS = Collections.unmodifiableMap(tmp);
			}
			
			private ElapsedTime(boolean lap, long endTime) {
				endNs = endTime;
				startNs = timerStart.get()[0];
				long[] lastLapRead = lapTime.get();
				if(lastLapRead!=null) {
					lastLapNs = lastLapRead[0];
				}
				if(lap) {
					lapTime.set(new long[]{endTime});
				} else {
					timerStart.remove();
					lapTime.remove();
				}
				elapsedNs = endNs-startNs;
				elapsedMs = TimeUnit.MILLISECONDS.convert(elapsedNs, TimeUnit.NANOSECONDS);
				if(lastLapNs!=-1L) {
					elapsedSinceLastLapNs = endTime -lastLapNs;
					elapsedSinceLastLapMs = TimeUnit.MILLISECONDS.convert(elapsedSinceLastLapNs, TimeUnit.NANOSECONDS);
				}				 
			}
			
			/**
			 * Returns the average elapsed time in ms. for the passed number of events
			 * @param cnt The number of events
			 * @return The average elapsed time in ms.
			 */
			public long avgMs(double cnt) {
				return _avg(elapsedMs, cnt);
			}
			
			/**
			 * Returns the average elapsed time in ns. for the passed number of events
			 * @param cnt The number of events
			 * @return The average elapsed time in ns.
			 */
			public long avgNs(double cnt) {
				return _avg(elapsedNs, cnt);
			}
			
			
			private long _avg(double time, double cnt) {
				if(time==0 || cnt==0 ) return 0L;
				double d = time/cnt;
				return (long)d;
			}
			
			public String toString() {
				StringBuilder b = new StringBuilder("[");
				b.append(elapsedNs).append("] ns.");
				b.append(" / [").append(elapsedMs).append("] ms.");
				if(elapsedSinceLastLapNs!=-1L) {
					b.append("  Elapsed Lap: [").append(elapsedSinceLastLapNs).append("] ns. / [").append(elapsedSinceLastLapMs).append("] ms.");
					
				}
				return b.toString();
			}
			
			public long elapsed() {
				return elapsed(TimeUnit.NANOSECONDS);
			}
			
			public long elapsed(TimeUnit unit) {
				if(unit==null) unit = TimeUnit.NANOSECONDS;
				return unit.convert(elapsedNs, TimeUnit.NANOSECONDS);
			}
			
			public String elapsedStr(TimeUnit unit) {
				if(unit==null) unit = TimeUnit.NANOSECONDS;
				return new StringBuilder("[").append(unit.convert(elapsedNs, TimeUnit.NANOSECONDS)).append("] ").append(UNITS.get(unit)).toString();
			}

			public String elapsedStr() {			
				return elapsedStr(TimeUnit.NANOSECONDS);
			}
			
		}
		
		
		/**
		 * <p>Title: Clock</p>
		 * <p>Description: Defines a clock impl.</p> 
		 */
		private static interface Clock {
			long time();
		}
		
		/**
		 * <p>Title: DirectClock</p>
		 * <p>Description: A clock implementation that simply returns {@code System.currentTimeMillis()}</p> 
		 */
		private static class DirectClock implements Clock {
			public long time() {
				return System.currentTimeMillis();
			}
		}
		
		private static class NanoClock implements Clock {		
			public long time() {
				return TimeUnit.MILLISECONDS.convert(System.nanoTime(), TimeUnit.NANOSECONDS) + startTime;
			}
		}
		
		private static class TestClock implements Clock {		
			public long time() {
				return testTime.get();
			}
		}
		
		public static long setTestTime(long time) {
			testTime.set(time);
			return time;
		}
		
		public static long setTestTime() {
			testTime.set(DIRECT.getTime());
			return testTime.get();
		}
		
		public static long tickTestTime() {
			return testTime.incrementAndGet();
		}
	}
	

}
