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

import java.security.NoSuchAlgorithmException;

import org.rulelearn.data.AttributePreferenceType;
import org.rulelearn.types.ElementList;
import org.rulelearn.types.EnumerationField;
import org.rulelearn.types.EnumerationFieldCachingFactory;
import org.rulelearn.types.EvaluationField;
import org.rulelearn.types.IntegerField;
import org.rulelearn.types.IntegerFieldCachingFactory;
import org.rulelearn.types.RealField;
import org.rulelearn.types.RealFieldCachingFactory;

import pl.poznan.put.cs.idss.jrs.types.EnumDomain;

/**
 * Converts information table field from jRS memory representation to ruleLearn memory representation.
 *
 * @author Marcin Szeląg (<a href="mailto:marcin.szelag@cs.put.poznan.pl">marcin.szelag@cs.put.poznan.pl</a>)
 */
public class jRSField2rLField {
	
	/**
	 * Converts given jRS field to corresponding ruleLearn field.
	 * The correspondence is as follows:
	 * <ol>
	 * <li>{@link pl.poznan.put.cs.idss.jrs.types.IntegerField} is converted to {@link IntegerField},</li>
	 * <li>{@link pl.poznan.put.cs.idss.jrs.types.FloatField} is converted to {@link RealField},</li>
	 * <li>{@link pl.poznan.put.cs.idss.jrs.types.EnumField} is converted to {@link EnumerationField}</li>
	 * </ol>
	 * Uses respective caching factory with volatile cache.
	 * 
	 * @param jRSField jRS field to be converted to ruleLearn field
	 * @param preferenceType preference type of the respective ruleLearn attribute
	 * 
	 * @return ruleLearn evaluation field corresponding to given jRS field
	 * @throws UnsupportedOperationException if type of given jRS field is different than {@link pl.poznan.put.cs.idss.jrs.types.IntegerField},
	 *         {@link pl.poznan.put.cs.idss.jrs.types.FloatField} or {@link pl.poznan.put.cs.idss.jrs.types.EnumField}
	 */
	public EvaluationField convertjRSField2rLField(pl.poznan.put.cs.idss.jrs.types.Field jRSField, AttributePreferenceType preferenceType) {
		EvaluationField rLField = null;
		
		if (jRSField instanceof pl.poznan.put.cs.idss.jrs.types.IntegerField) {
			rLField = IntegerFieldCachingFactory.getInstance().create(((pl.poznan.put.cs.idss.jrs.types.IntegerField)jRSField).get(), preferenceType, false);
		} else {
			if (jRSField instanceof pl.poznan.put.cs.idss.jrs.types.FloatField) {
				rLField = RealFieldCachingFactory.getInstance().create(((pl.poznan.put.cs.idss.jrs.types.FloatField)jRSField).get(), preferenceType, false);
			} else {
				if (jRSField instanceof pl.poznan.put.cs.idss.jrs.types.EnumField) {
					EnumDomain enumDomain = ((pl.poznan.put.cs.idss.jrs.types.EnumField)jRSField).getDomain();
					String[] elements = new String[enumDomain.size()];
					for (int elementIndex = 0; elementIndex < elements.length; elementIndex++) {
						elements[elementIndex] = enumDomain.getName(elementIndex);
					}
					
					ElementList elementList;
					try {
						elementList = new ElementList(elements);
					} catch (NoSuchAlgorithmException exception) {
						throw new UnsupportedOperationException("Cannot process jRS enum field."); //this should not happen if default algorithm in ElementList is set correctly
					}
					rLField = EnumerationFieldCachingFactory.getInstance().create(elementList, ((pl.poznan.put.cs.idss.jrs.types.EnumField)jRSField).getIndex(), preferenceType, false);
				} else {
					throw new UnsupportedOperationException("Cannot process jRS field of type other than integer, float, or enum.");
				} //else
			} //else
		} //else
		
		return rLField;
	}

}
