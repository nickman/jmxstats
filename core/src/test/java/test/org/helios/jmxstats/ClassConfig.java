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
package test.org.helios.jmxstats;

import java.lang.management.ManagementFactory;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.management.ManagementService;

/**
 * <p>Title: ClassConfig</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>test.org.helios.jmxstats.ClassConfig</code></p>
 */
public class ClassConfig {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		log("CacheConfig Test");
		CacheManager manager = CacheManager.newInstance("src/test/resources/cache/cache-config.xml");
		log("CacheManager Configured [" + manager.getName() + "]");
		ManagementService ms = new ManagementService(manager, ManagementFactory.getPlatformMBeanServer(), true, true, true, true); 
		ms.init();
		try { Thread.currentThread().join(360000); } catch (Exception e) {}

	}

	public static void log(Object msg) {
		System.out.println(msg);
	}
}
