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

import org.rulelearn.core.InvalidValueException;
import org.rulelearn.data.AttributePreferenceType;
import org.rulelearn.data.AttributeType;
import org.rulelearn.data.EvaluationAttribute;
import org.rulelearn.types.EvaluationField;
import org.rulelearn.types.UnknownSimpleFieldMV2;

/**
 * Converts attributes from jRS memory representation to ruleLearn memory representation.
 *
 * @author Marcin Szeląg (<a href="mailto:marcin.szelag@cs.put.poznan.pl">marcin.szelag@cs.put.poznan.pl</a>)
 */
public class jRSAttributes2rLAttributes {
	
	/**
	 * Converts attributes from jRS memory representation to ruleLearn memory representation.
	 * 
	 * @param jRSAttributes attributes in jRS memory representation
	 * @return attributes in ruleLearn memory representation
	 * 
	 * @throws InvalidValueException if any attribute has incorrect preference type or type (kind)
	 * @throws UnsupportedOperationException if any jRS attribute has initial value of type other than integer, float, or enum
	 */
	EvaluationAttribute[] convertJRSAttributes2rLAttributes(pl.poznan.put.cs.idss.jrs.types.Attribute[] jRSAttributes) {
		EvaluationAttribute[] rLAttributes = new EvaluationAttribute[jRSAttributes.length];
		pl.poznan.put.cs.idss.jrs.types.Attribute jRSattribute;
		AttributePreferenceType preferenceType;
		EvaluationField valueType;
		AttributeType attributeType;
		
		for (int j = 0; j < jRSAttributes.length; j++) {
			jRSattribute = jRSAttributes[j];
			
			switch (jRSattribute.getPreferenceType()) {
			case pl.poznan.put.cs.idss.jrs.types.Attribute.NONE:
				preferenceType = AttributePreferenceType.NONE;
				break;
			case pl.poznan.put.cs.idss.jrs.types.Attribute.COST:
				preferenceType = AttributePreferenceType.COST;
				break;
			case pl.poznan.put.cs.idss.jrs.types.Attribute.GAIN:
				preferenceType = AttributePreferenceType.GAIN;
				break;
			default: throw new InvalidValueException("Incorrect preference type of attribute no. " + (j+1));
			}
			
			switch (jRSattribute.getKind()) {
			case pl.poznan.put.cs.idss.jrs.types.Attribute.DESCRIPTION:
				attributeType = AttributeType.DESCRIPTION;
				break;
			case pl.poznan.put.cs.idss.jrs.types.Attribute.DECISION:
				attributeType = AttributeType.DECISION;
				break;
			case pl.poznan.put.cs.idss.jrs.types.Attribute.NONE:
				attributeType = AttributeType.CONDITION;
				break;
			default: throw new InvalidValueException("Incorrect type (kind) of attribute no. " + (j+1));
			}
			
			valueType = (new jRSField2rLField()).convertjRSField2rLField(jRSattribute.getInitialValue(), preferenceType);
			
			//EvaluationAttribute(String name, boolean active, AttributeType type, EvaluationField valueType, UnknownSimpleField missingValueType, AttributePreferenceType preferenceType)
			rLAttributes[j] = new EvaluationAttribute(jRSattribute.getName(), jRSattribute.getActive(), attributeType, valueType, new UnknownSimpleFieldMV2(), preferenceType);
		} //for (j)
		
		return rLAttributes;
	}
	
}
