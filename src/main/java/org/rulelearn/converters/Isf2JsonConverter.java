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

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.rulelearn.core.InvalidValueException;
import org.rulelearn.data.EvaluationAttribute;
import org.rulelearn.data.InformationTable;
import org.rulelearn.data.json.InformationTableWriter;
import org.rulelearn.types.EnumerationField;
import org.rulelearn.types.EnumerationFieldCachingFactory;
import org.rulelearn.types.Field;
import org.rulelearn.types.IntegerFieldCachingFactory;
import org.rulelearn.types.RealFieldCachingFactory;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import pl.poznan.put.cs.idss.jrs.core.SimpleParseLog;
import pl.poznan.put.cs.idss.jrs.core.UnknownValueException;
import pl.poznan.put.cs.idss.jrs.core.mem.MemoryContainer;
import pl.poznan.put.cs.idss.jrs.output.OM;
import pl.poznan.put.cs.idss.jrs.output.SystemOut;
//import pl.poznan.put.cs.idss.jrs.types.Attribute;
//import pl.poznan.put.cs.idss.jrs.types.IntegerField;
//import pl.poznan.put.cs.idss.jrs.types.FloatField;
import pl.poznan.put.cs.idss.jrs.utilities.ISFLoader;

/**
 * Converts information system file (ISF) to a pair of ruleLearn JSON files:
 * file with metadata (attributes) and file with data (objects from the information table).
 *
 * @author Marcin Szeląg (<a href="mailto:marcin.szelag@cs.put.poznan.pl">marcin.szelag@cs.put.poznan.pl</a>)
 */
public class Isf2JsonConverter {
	
	/**
	 * Converts information system file (ISF) to a pair of ruleLearn JSON files:
	 * file with metadata (attributes) and file with data (objects from the information table).
	 * 
	 * @param isfFilePath path to input ISF file containing information about attributes and objects
	 * @param jsonAttributesFilePath path to output file where metadata (attributes) should be written in JSON format
	 * @param jsonDataFilePath path to output file where data (objects) should be written in JSON format
	 * @param prettyPrinting tells if pretty printing in the two output files should be applied
	 * 
	 * @throws InvalidValueException if any attribute has incorrect preference type or type (kind)
	 * @throws UnsupportedOperationException if any jRS attribute has initial value of type other than integer, float, or enum
	 */
	void convertIsf2Json(String isfFilePath, String jsonAttributesFilePath, String jsonDataFilePath, boolean prettyPrinting) {
		MemoryContainer memoryContainer = ISFLoader.loadISFIntoMemoryContainer(isfFilePath, new SimpleParseLog());
		
		if (memoryContainer != null) {
			EvaluationAttribute[] rLAttributes = (new jRSAttributes2rLAttributes()).convertJRSAttributes2rLAttributes(memoryContainer.getAttributes());
			
			InformationTable informationTable;
			Field[] row;
			List<Field[]> rows = new ObjectArrayList<Field[]>();
			pl.poznan.put.cs.idss.jrs.types.Field field;
			
			for (int i = 0; i < memoryContainer.size(); i++) {
				row = new Field[memoryContainer.getAttributes().length];
				for (int j = 0; j < row.length; j++) {
					if ((field = memoryContainer.getExample(i).getField(j)) instanceof pl.poznan.put.cs.idss.jrs.types.IntegerField) {
						try {
							int value = ((pl.poznan.put.cs.idss.jrs.types.IntegerField)field).get();
							row[j] = IntegerFieldCachingFactory.getInstance().create(
									value, rLAttributes[j].getPreferenceType(), false); //use volatile cache
						} catch (UnknownValueException exception) {
							row[j] = rLAttributes[j].getMissingValueType();
						}
						
					} else {
						if ((field = memoryContainer.getExample(i).getField(j)) instanceof pl.poznan.put.cs.idss.jrs.types.FloatField) {
							try {
								double value = ((pl.poznan.put.cs.idss.jrs.types.FloatField)field).get();
								row[j] = RealFieldCachingFactory.getInstance().create(
										value, rLAttributes[j].getPreferenceType(), false); //use volatile cache
							} catch (UnknownValueException exception) {
								row[j] = rLAttributes[j].getMissingValueType();
							}
						} else {
							if ((field = memoryContainer.getExample(i).getField(j)) instanceof pl.poznan.put.cs.idss.jrs.types.EnumField) {
								try {
									int index = ((pl.poznan.put.cs.idss.jrs.types.EnumField)field).getIndex();
									row[j] = EnumerationFieldCachingFactory.getInstance().create(
											((EnumerationField)rLAttributes[j].getValueType()).getElementList(),
											index, rLAttributes[j].getPreferenceType(), false); //use volatile cache
								} catch (UnknownValueException exception) {
									row[j] = rLAttributes[j].getMissingValueType();
								}
							} else {
								//redundant check (verified above, when attributes have been converted), but added for clarity
								throw new UnsupportedOperationException("Cannot process jRS field with value of type other than integer, float, or enum.");
							} //else
						} //else
					} //else
				} //for (j)
				rows.add(row);
			} //for (i)
			
			informationTable = new InformationTable(rLAttributes, rows, true); //accelerate by read only params
			
			VolatileCachesCleaner.clearVolatileCaches(rLAttributes); //clear volatile caches of all used evaluation field caching factories
			
			InformationTableWriter informationTableWriter = new InformationTableWriter(prettyPrinting);
			OM.println("Pretty printing set to: " + prettyPrinting);
			
			try (FileWriter fileWriter = new FileWriter(jsonAttributesFilePath)) {
				informationTableWriter.writeAttributes(informationTable, fileWriter);
				OM.println("Attributes written to file " + jsonAttributesFilePath);
				
			}
			catch (IOException ex) {
				OM.println(ex.toString());
			}
			try (FileWriter fileWriter = new FileWriter(jsonDataFilePath)) {
				informationTableWriter.writeObjects(informationTable, fileWriter);
				OM.println("Objects written to file " + jsonDataFilePath);
			}
			catch (IOException ex) {
				OM.println(ex.toString());
			}
		} //if
	}
	
	/**
	 * Application entry point.
	 * 
	 * @param args input arguments of this converter.
	 *        Syntax: Isf2JsonConverter <ISF-file-path> <JSON-attributes-file-path> <JSON-data-file-path> <use-pretty-printing>
	 * @throws UnsupportedOperationException when data cannot be converted from jRS ISF file to a pair of ruleLearn JSON files
	 */
	public static void main(String[] args) {
		//Disclaimer:
		//- only simple fields attributes are read, apart from StringField attributes
		//- only EvaluationAttribute attributes are produced
		//- missing value type of produced attributes is set to UnknownSimpleFieldMV2
		
		//set system console as default message output
		SystemOut systemOut = new SystemOut();
		OM.addOutput(systemOut);
		OM.setDefaultOutput(systemOut.getKey());
		
		if (args.length < 3) {
			OM.println("Wrong number of parameters.");
			OM.println("Syntax:");
			OM.println("Isf2JsonConverter <ISF-file-path> <JSON-attributes-file-path> <JSON-data-file-path> <pretty>");
			OM.println("Example:");
			OM.println("Isf2JsonConverter GermanCredit.isf GermanCredit.meta.json GermanCredit.data.json pretty");
			return;
		}
		
		Isf2JsonConverter isf2JsonConverter = new Isf2JsonConverter();
		isf2JsonConverter.convertIsf2Json(args[0], args[1], args[2], args.length > 3 && args[3].strip().equalsIgnoreCase("pretty"));
	}
	
}
