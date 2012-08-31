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

/**
 * <p>Title: IMetric</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jmxstats.core.metric.IMetric</code></p>
 */
public interface IMetric {

	/**
	 * Processes a new interval value for the current interval
	 * @param value the new value to process
	 * @return this metric
	 */
	public abstract IMetric process(long value);

	/**
	 * Executes an interval reset on this metric
	 * @param currentTime The common interval time
	 * @return this metric
	 */
	public abstract IMetric reset(long currentTime);

	/**
	 * Returns the globally unique metric id
	 * @return the id
	 */
	public abstract long getId();

	/**
	 * Returns the metric name
	 * @return the name
	 */
	public abstract String getName();

	/**
	 * Returns the interval start time
	 * @return the startTime
	 */
	public abstract long getStartTime();

	/**
	 * Returns the interval end time
	 * @return the endTime
	 */
	public abstract long getEndTime();

	/**
	 * Returns the interval event count
	 * @return the count
	 */
	public abstract long getCount();

	/**
	 * Returns the average value for the current interval
	 * @return the average
	 */
	public abstract long getAverage();

	/**
	 * Returns the maximum value for the current interval
	 * @return the maximum
	 */
	public abstract long getMaximum();

	/**
	 * Returns the minimum value for the current interval
	 * @return the minimum
	 */
	public abstract long getMinimum();

	/**
	 * Returns the metric type
	 * @return the type
	 */
	public abstract MetricType getType();

}