/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.clerezza.triaxrs.util;

import java.util.Comparator;

import javax.ws.rs.core.MediaType;

/**
 * Sorts media types in accordance with an accept-header inconsistently
 *
 * This comparator is not consistent with equals
 *
 * @author reto
 */
public class InconsistentMediaTypeComparator implements Comparator<MediaType> {

	private AcceptHeader acceptHeader;

	public InconsistentMediaTypeComparator() {
	}

	public InconsistentMediaTypeComparator(AcceptHeader acceptHeader) {
		this.acceptHeader = acceptHeader;
	}


	
	@Override
	public int compare(MediaType o1, MediaType o2) {
		if (o1.equals(o2)) return 0;
		if (acceptHeader != null) {
			if (acceptHeader.getAcceptedQuality(o1) > acceptHeader.getAcceptedQuality(o2)) {
				return -1;
			}
			if (acceptHeader.getAcceptedQuality(o1) < acceptHeader.getAcceptedQuality(o2)) {
				return 1;
			}
		}
		return MediaTypeComparator.inconsistentCompare(o1, o2);
		
	}

}
