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
	/** The data storage chronicle */
	private final ChronicleController chronicleController;
	
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
		chronicleController = ChronicleController.getInstance();
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
			public void onIntervalSwitch(CurrentInterval ci) {
				log("\n\t=========================================\n\tInterval Switch:" + ci.currentIntervalId + "\n\tStart:" + sdf.format(ci.getStartDate()) + "\n\tEnd:" + sdf.format(ci.getEndDate()) + "\n\t=========================================");
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
	 * <p>Title: CurrentInterval</p>
	 * <p>Description: A container class for the current interval data, collected in this class so it can be maintained atomically</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.jmxstats.core.Controller.CurrentInterval</code></p>
	 */
	public static class CurrentInterval {
		/** Date formatter */
		private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss,S");
		/** The interval ID factory */
		private static final AtomicLong INTERVAL_SERIAL = new AtomicLong(-1);
		/** The current instance */
		private static AtomicReference<CurrentInterval> CURRENT = new AtomicReference<CurrentInterval>(null); 
		/** The current interval ID */
		private final long currentIntervalId;
		/** The start time of the interval */
		private final long startTime;
		/** The end time of the interval */
		private final long endTime;
		
		/**
		 * Creates the next CurrentInterval
		 * @return the created interval
		 */
		private static CurrentInterval next() {
			CurrentInterval current = CURRENT.get();
			if(current==null) {
				synchronized(CURRENT) {
					current = CURRENT.get();
					if(current==null) {
						long time = SystemClock.roundDownTime();
						current = new CurrentInterval(time, time + SystemClock.INTERVAL-1);
						CURRENT.set(current);						
					}
				}
			} else {
				current = new CurrentInterval(current.endTime+1, current.endTime+SystemClock.INTERVAL);
				CURRENT.set(current);				
			}
			return current;
		}
		
		


		/**
		 * Creates a new CurrentInterval
		 * @param startTime The start time of the interval
		 * @param endTime The end time of the interval
		 */
		private CurrentInterval(long startTime, long endTime) {
			this.currentIntervalId = INTERVAL_SERIAL.incrementAndGet();
			this.startTime = startTime;
			this.endTime = endTime;
		}

		/**
		 * Returns the current interval ID
		 * @return the currentIntervalId
		 */
		public long getCurrentIntervalId() {
			return currentIntervalId;
		}

		/**
		 * Returns the start time of the interval 
		 * @return the startTime
		 */
		public long getStartTime() {
			return startTime;
		}

		/**
		 * Returns the end time of the interval 
		 * @return the endTime
		 */
		public long getEndTime() {
			return endTime;
		}
		
		/**
		 * Returns the start date of the interval 
		 * @return the start date
		 */
		public Date getStartDate() {
			return new Date(startTime);
		}

		/**
		 * Returns the end date of the interval 
		 * @return the end date
		 */
		public Date getEndDate() {
			return new Date(endTime);
		}

		/**
		 * {@inheritDoc}
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			String start, end;
			synchronized(sdf) {
				start = sdf.format(getStartDate());
				end = sdf.format(getEndDate());
			}
			StringBuilder builder = new StringBuilder();
			builder.append("CurrentInterval [currentIntervalId=");
			builder.append(currentIntervalId);
			builder.append(", startDate=");
			builder.append(start);
			builder.append(", endDate=");
			builder.append(end);
			builder.append("]");
			return builder.toString();
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
					+ (int) (currentIntervalId ^ (currentIntervalId >>> 32));
			return result;
		}


		/**
		 * {@inheritDoc}
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			CurrentInterval other = (CurrentInterval) obj;
			if (currentIntervalId != other.currentIntervalId) {
				return false;
			}
			return true;
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
		/** The current interval to pass to listeners */
		static private CurrentInterval _currentInterval;
		
		/**
		 * Updates the interval data
		 * @param currentInterval The current interval
		 */
		public static void update(CurrentInterval currentInterval) {
			_currentInterval = currentInterval;
		}

		/**
		 * Creates a new IntervalListenerRunnable
		 * @param listener The listener to wrap
		 */
		public IntervalListenerRunnable(IntervalListener listener) {
			this.listener = listener;
		}
		
		public Void call() {
			listener.onIntervalSwitch(_currentInterval);
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
		/** A reference to the current clock impl. */
		private static final AtomicReference<SystemClock> currentClock;
		/** Tracks the current interval */
		private static final AtomicReference<CurrentInterval> CURRENT_INTERVAL;
		/** Startup latch */
		private static final CountDownLatch startupLatch = new CountDownLatch(1); 
		/** The VM start time in ms. */
		public static final long startTime = ManagementFactory.getRuntimeMXBean().getStartTime();
		/** The test time */
		private static final AtomicLong testTime = new AtomicLong(0L);
		/** Holds the start timestamp of an elapsed time measurement */
		private static final ThreadLocal<long[]> timerStart = new ThreadLocal<long[]>() {
			protected long[] initialValue() {
				return new long[1];
			}
		};
		
		
		
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
				startupLatch.countDown();
				while(true) {
					try {					
						Thread.currentThread().join();						
					} catch (InterruptedException ex) {
						Thread.interrupted();
						final CurrentInterval ci = CurrentInterval.next();
						CURRENT_INTERVAL.set(ci);
						
						if(!listeners.isEmpty()) {
							IntervalListenerRunnable.update(ci);
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
		
		
		
		
		static {
			try {
				INTERVAL = Long.parseLong(System.getProperty(INTERVAL_PROP, "15000"));
			} catch (Exception e) {
				INTERVAL = 15000;
			}
			currentClock = new AtomicReference<SystemClock>(DIRECT);
			CURRENT_INTERVAL = new AtomicReference<CurrentInterval>(CurrentInterval.next());					
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
