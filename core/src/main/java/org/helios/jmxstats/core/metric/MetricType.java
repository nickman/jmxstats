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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>Title: MetricType</p>
 * <p>Description: Defines the metric types and related variant behavior</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jmxstats.core.metric.MetricType</code></p>
 */

public enum MetricType {
	/** Standard metric accumulating the interval average and totally reset on interval switch */
	AVG,
	/** Standard metric accumulating the interval average and count reset on interval switch */
	STICKY,
	/** Delta metric accumulating the interval average and totally reset on interval switch */
	DELTA,
	/** Delta metric accumulating the interval average and count reset on interval switch */
	DELTASTICKY,
	/** Interval counter totally reset on interval switch */
	INTERVALCOUNT;
	
	/** Decodes of ordinal to MetricType */
	private static final Map<Integer, MetricType> ORD2TYPE;
	
	static {
		Map<Integer, MetricType> tmp = new HashMap<Integer, MetricType>(MetricType.values().length);
		for(MetricType mt: MetricType.values()) {
			tmp.put(mt.ordinal(), mt);
		}
		ORD2TYPE = Collections.unmodifiableMap(new HashMap<Integer, MetricType>(tmp));
	}
	
	/**
	 * Returns the MetricType that maps to the passed ordinal
	 * @param ord The ordinal to decode
	 * @return the MetricType
	 */
	public static MetricType decode(int ord) {
		MetricType mt = ORD2TYPE.get(ord);
		if(mt==null) throw new IllegalArgumentException("The passed ordinal [" + ord + "] does not map to a MetricType", new Throwable());
		return mt;
	}
	
	/**
	 * Returns the MetricType that maps to the passed name, trimming and upercasing the passed name
	 * @param name The metric type name to decode
	 * @return the MetricType 
	 */
	public static MetricType decode(CharSequence name) {
		if(name==null) throw new IllegalArgumentException("The passed name was null", new Throwable());
		MetricType mt = MetricType.valueOf(name.toString().trim().toUpperCase());
		if(mt==null) throw new IllegalArgumentException("The passed name [" + name + "] does not map to a MetricType", new Throwable());
		return mt;
	}
	
}
