/**
 * Copyright (C) Marcin Szeląg
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.rulelearn.converters;

import org.rulelearn.data.Attribute;
import org.rulelearn.data.EvaluationAttribute;

/**
 * Cleaner of all used volatile caches.
 * 
 * @author Marcin Szeląg
 */
public class VolatileCachesCleaner {

	/**
	 * Clears volatile caches of all used evaluation field caching factories.
	 *  
	 * @param attributes attributes of an information table
	 */
	static void clearVolatileCaches(Attribute[] attributes) {
		//clear volatile caches of all used evaluation field caching factories
		for (int i = 0; i < attributes.length; i++) {
			if (attributes[i] instanceof EvaluationAttribute) {
				((EvaluationAttribute)attributes[i]).getValueType().getCachingFactory().clearVolatileCache(); //clears volatile cache of the caching factory corresponding to current evaluation attribute
			}
		}
	}
}
