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
package org.helios.jmxstats.core.metric;

import java.util.Properties;

import org.helios.jmxstats.core.Controller.SystemClock;
import org.helios.jmxstats.core.Controller.SystemClock.ElapsedTime;

/**
 * <p>Title: BaseMetric</p>
 * <p>Description: The base concrete metric type</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jmxstats.core.metric.BaseMetric</code></p>
 */
public class BaseMetric implements IMetric {
	/** The ID of the metric */
	protected long id;
	/** The metric name */
	protected String name;
	/** The metric type */
	protected MetricType type;
	
	/** The interval start time */
	protected long startTime;
	/** The interval end time */
	protected long endTime;
	/** The count of events for the current interval */
	protected long count;
	/** The average value for the current interval */
	protected long average;
	/** The maximum value for the current interval */
	protected long maximum;
	/** The minimum value for the current interval */
	protected long minimum;
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.jmxstats.core.metric.IMetric#process(long)
	 */
	@Override
	public IMetric process(long value) {
		
		return this;
	}
 
	/**
	 * {@inheritDoc}
	 * @see org.helios.jmxstats.core.metric.IMetric#reset(long)
	 */
	@Override
	public IMetric reset(long currentTime) {
		
		return this;
	}
	/**
	 * {@inheritDoc}
	 * @see org.helios.jmxstats.core.metric.IMetric#getId()
	 */
	@Override
	public long getId() {
		return id;
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.jmxstats.core.metric.IMetric#getName()
	 */
	@Override
	public String getName() {
		return name;
	}
	/**
	 * {@inheritDoc}
	 * @see org.helios.jmxstats.core.metric.IMetric#getStartTime()
	 */
	@Override
	public long getStartTime() {
		return startTime;
	}
	/**
	 * {@inheritDoc}
	 * @see org.helios.jmxstats.core.metric.IMetric#getEndTime()
	 */
	@Override
	public long getEndTime() {
		return endTime;
	}
	/**
	 * {@inheritDoc}
	 * @see org.helios.jmxstats.core.metric.IMetric#getCount()
	 */
	@Override
	public long getCount() {
		return count;
	}
	/**
	 * {@inheritDoc}
	 * @see org.helios.jmxstats.core.metric.IMetric#getAverage()
	 */
	@Override
	public long getAverage() {
		return average;
	}
	/**
	 * {@inheritDoc}
	 * @see org.helios.jmxstats.core.metric.IMetric#getMaximum()
	 */
	@Override
	public long getMaximum() {
		return maximum;
	}
	/**
	 * {@inheritDoc}
	 * @see org.helios.jmxstats.core.metric.IMetric#getMinimum()
	 */
	@Override
	public long getMinimum() {
		return minimum;
	}
	/**
	 * {@inheritDoc}
	 * @see org.helios.jmxstats.core.metric.IMetric#getType()
	 */
	@Override
	public MetricType getType() {
		return type;
	}
	
	public static long longHashCode(CharSequence name) {
		if(name==null) return 0;
		StringBuilder b = new StringBuilder(name.toString());
		long hash = b.toString().hashCode();
		hash += b.reverse().hashCode();
		return hash;
	}
	
	public static long hashCode(CharSequence name) {
		if(name==null) return 0;
		return name.toString().hashCode();
	}
	
	
	public static void main(String[] args) {
		log("LongHashCode Test");
		int loopCount = 10000;
		Properties p = System.getProperties();
		String[] names = new String[p.size()*3];
		int x = 0;
		for(String key: p.stringPropertyNames()) {
			String value = p.getProperty(key);
			names[x] = key;
			x++;
			names[x] = value;
			x++;
			names[x] = key+"="+value;
		}
		log("Testing with " + x + " names.");
		long t = Long.MIN_VALUE;
		for(int i = 0; i < loopCount; i++) {
			for(String s: names) {
				//t += longHashCode(s);
				t += hashCode(s);
			}			
			t = Long.MIN_VALUE;
		}
		log("Warmup Complete");
		SystemClock.startTimer();
		t = Long.MIN_VALUE;
		for(int i = 0; i < loopCount; i++) {
			for(String s: names) {
				//t += longHashCode(s);
				t += hashCode(s);
			}			
			t = Long.MIN_VALUE;
		}
		ElapsedTime et = SystemClock.endTimer();
		log("Elapsed Time:" + et);
		log("Average ns:" + et.avgNs(loopCount*x));
	}
	
	public static void log(Object msg) {
		System.out.println(msg);
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("BaseMetric [id=");
		builder.append(id);
		builder.append(", ");
		if (name != null) {
			builder.append("name=");
			builder.append(name);
			builder.append(", ");
		}
		if (type != null) {
			builder.append("type=");
			builder.append(type);
			builder.append(", ");
		}
		builder.append("startTime=");
		builder.append(startTime);
		builder.append(", endTime=");
		builder.append(endTime);
		builder.append(", count=");
		builder.append(count);
		builder.append(", average=");
		builder.append(average);
		builder.append(", maximum=");
		builder.append(maximum);
		builder.append(", minimum=");
		builder.append(minimum);
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
		result = prime * result + (int) (id ^ (id >>> 32));
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
		BaseMetric other = (BaseMetric) obj;
		if (id != other.id) {
			return false;
		}
		return true;
	}
}
