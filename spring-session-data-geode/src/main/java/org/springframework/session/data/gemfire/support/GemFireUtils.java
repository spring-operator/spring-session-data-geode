/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.session.data.gemfire.support;

import java.io.Closeable;
import java.io.IOException;
import java.util.Optional;

import org.apache.geode.cache.Cache;
import org.apache.geode.cache.DataPolicy;
import org.apache.geode.cache.GemFireCache;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.RegionAttributes;
import org.apache.geode.cache.RegionShortcut;
import org.apache.geode.cache.client.ClientCache;
import org.apache.geode.cache.client.ClientRegionShortcut;
import org.apache.geode.internal.cache.GemFireCacheImpl;

/**
 * {@link GemFireUtils} is an abstract, extensible utility class for working with Apache Geode and Pivotal GemFire
 * objects and types.
 *
 * @author John Blum
 * @see org.apache.geode.cache.Cache
 * @see org.apache.geode.cache.GemFireCache
 * @see org.apache.geode.cache.Region
 * @see org.apache.geode.cache.client.ClientCache
 * @since 1.1.0
 */
public abstract class GemFireUtils {

	/**
	 * Null-safe method to close the given {@link Closeable} object.
	 *
	 * @param obj the {@link Closeable} object to close.
	 * @return true if the {@link Closeable} object is not null and was successfully
	 * closed, otherwise return false.
	 * @see java.io.Closeable
	 */
	public static boolean close(Closeable obj) {

		if (obj != null) {
			try {
				obj.close();
				return true;
			}
			catch (IOException ignore) { }
		}

		return false;
	}

	/**
	 * Determines whether the Pivotal GemFire cache is a client.
	 *
	 * @param gemfireCache a reference to the Pivotal GemFire cache.
	 * @return a boolean value indicating whether the Pivotal GemFire cache is a client.
	 * @see org.apache.geode.cache.client.ClientCache
	 * @see org.apache.geode.cache.GemFireCache
	 */
	public static boolean isClient(GemFireCache gemfireCache) {

		boolean client = gemfireCache instanceof ClientCache;

		client &= (!(gemfireCache instanceof GemFireCacheImpl) || ((GemFireCacheImpl) gemfireCache).isClient());

		return client;
	}

	/**
	 * Determines whether the Pivotal GemFire cache is a peer.
	 *
	 * @param gemFireCache a reference to the Pivotal GemFire cache.
	 * @return a boolean value indicating whether the Pivotal GemFire cache is a peer.
	 * @see org.apache.geode.cache.Cache
	 * @see org.apache.geode.cache.GemFireCache
	 */
	public static boolean isPeer(GemFireCache gemFireCache) {
		return gemFireCache instanceof Cache && !isClient(gemFireCache);
	}

	/**
	 * Determines whether the given {@link ClientRegionShortcut} is local only.
	 *
	 * @param shortcut the ClientRegionShortcut to evaluate.
	 * @return a boolean value indicating if the {@link ClientRegionShortcut} is local or
	 * not.
	 * @see org.apache.geode.cache.client.ClientRegionShortcut
	 */
	public static boolean isLocal(ClientRegionShortcut shortcut) {

		switch (shortcut) {
			case LOCAL:
			case LOCAL_HEAP_LRU:
			case LOCAL_OVERFLOW:
			case LOCAL_PERSISTENT:
			case LOCAL_PERSISTENT_OVERFLOW:
				return true;
			default:
				return false;
		}
	}

	/**
	 * Determines whether the given {@link ClientRegionShortcut} is a proxy-based shortcut.
	 *
	 * "Proxy"-based {@link Region Regions} keep no local state.
	 *
	 * @param shortcut {@link ClientRegionShortcut} to evaluate.
	 * @return a boolean value indicating whether the {@link ClientRegionShortcut} refers to a Proxy-based shortcut.
	 * @see org.apache.geode.cache.client.ClientRegionShortcut
	 */
	public static boolean isProxy(ClientRegionShortcut shortcut) {

		switch (shortcut) {
			case PROXY:
				return true;
			default:
				return false;
		}
	}

	/**
	 * Determines whether the given {@link Region} is a {@literal PROXY}.
	 *
	 * @param region {@link Region} to evaluate as a {@literal PROXY}; must not be {@literal null}.
	 * @return a boolean value indicating whether the {@link Region} is a {@literal PROXY}.
	 * @see org.apache.geode.cache.DataPolicy
	 * @see org.apache.geode.cache.Region
	 */
	public static boolean isProxy(Region<?, ?> region) {

		RegionAttributes regionAttributes = region.getAttributes();

		DataPolicy regionDataPolicy = regionAttributes.getDataPolicy();

		return DataPolicy.EMPTY.equals(regionDataPolicy)
			|| Optional.ofNullable(regionDataPolicy)
				.filter(DataPolicy.PARTITION::equals)
				.map(it -> regionAttributes.getPartitionAttributes())
				.filter(partitionAttributes -> partitionAttributes.getLocalMaxMemory() <= 0)
				.isPresent();
	}

	/**
	 * Determines whether the {@link RegionShortcut} is a Proxy-based shortcut.
	 *
	 * "Proxy"-based {@link Region Regions} keep no local state.
	 *
	 * @param shortcut {@link RegionShortcut} to evaluate.
	 * @return a boolean value indicating whether the {@link RegionShortcut} refers to a Proxy-based shortcut.
	 * @see org.apache.geode.cache.RegionShortcut
	 */
	public static boolean isProxy(RegionShortcut shortcut) {

		switch (shortcut) {
			case PARTITION_PROXY:
			case PARTITION_PROXY_REDUNDANT:
			case REPLICATE_PROXY:
				return true;
			default:
				return false;
		}
	}

	/**
	 * Converts a {@link Region} name to a {@link Region} path.
	 *
	 * @param regionName a String specifying the name of the {@link Region}.
	 * @return a String path for the given {@link Region} by name.
	 * @see org.apache.geode.cache.Region#getFullPath()
	 * @see org.apache.geode.cache.Region#getName()
	 */
	public static String toRegionPath(String regionName) {
		return String.format("%1$s%2$s", Region.SEPARATOR, regionName);
	}
}
