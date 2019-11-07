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

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.rulelearn.core.InvalidValueException;
import org.rulelearn.data.EvaluationAttribute;
import org.rulelearn.data.EvaluationAttributeWithContext;
import org.rulelearn.data.InformationTable;
import org.rulelearn.data.json.InformationTableWriter;
import org.rulelearn.rules.Condition;
import org.rulelearn.rules.ConditionAtLeastObjectVSThreshold;
import org.rulelearn.rules.ConditionAtLeastThresholdVSObject;
import org.rulelearn.rules.ConditionAtMostObjectVSThreshold;
import org.rulelearn.rules.ConditionAtMostThresholdVSObject;
import org.rulelearn.rules.ConditionEqualObjectVSThreshold;
import org.rulelearn.rules.ConditionEqualThresholdVSObject;
import org.rulelearn.rules.Rule;
import org.rulelearn.rules.RuleCharacteristics;
import org.rulelearn.rules.RuleSetWithCharacteristics;
import org.rulelearn.rules.RuleType;
import org.rulelearn.rules.ruleml.RuleMLBuilder;
import org.rulelearn.types.EvaluationField;
import org.rulelearn.types.Field;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import pl.poznan.put.cs.idss.jrs.core.SimpleParseLog;
import pl.poznan.put.cs.idss.jrs.output.OM;
import pl.poznan.put.cs.idss.jrs.output.SystemOut;
import pl.poznan.put.cs.idss.jrs.rules.Relation;
import pl.poznan.put.cs.idss.jrs.rules.RelationAtLeast;
import pl.poznan.put.cs.idss.jrs.rules.RelationAtMost;
import pl.poznan.put.cs.idss.jrs.rules.RelationEqual;
import pl.poznan.put.cs.idss.jrs.rules.RuleStatistics;
import pl.poznan.put.cs.idss.jrs.rules.RulesContainer;
import pl.poznan.put.cs.idss.jrs.rules.SingleCondition;

/**
 * Converts *.rules file, used in jRS library to store decision rules, to a pair of files:
 * <ul>
 * <li>JSON file with attributes (metadata) stored in given *.rules file,</li>
 * <li>RuleML xml file, used to store decision rules in ruleLearn library.</li>
 * </ul>
 *
 * @author Marcin Szeląg (<a href="mailto:marcin.szelag@cs.put.poznan.pl">marcin.szelag@cs.put.poznan.pl</a>)
 */
public class Rules2RuleMLConverter {
	
	/**
	 * Pair composed of a decision rule and its characteristics.
	 * 
	 * @author Marcin Szeląg
	 */
	private class RuleWithCharacteristics {
		private Rule rule;
		private RuleCharacteristics ruleCharacteristics;
		
		private RuleWithCharacteristics(Rule rule, RuleCharacteristics ruleCharacteristics) {
			this.rule = rule;
			this.ruleCharacteristics = ruleCharacteristics;
		}
	}
	
	/**
	 * Converts single jRS condition into corresponding ruleLearn condition.
	 * 
	 * @param jRSRuleType type of jRS rule that given jRS condition is part of;
	 *        has to be one of {@link pl.poznan.put.cs.idss.jrs.rules.Rule#CERTAIN} or {@link pl.poznan.put.cs.idss.jrs.rules.Rule#POSSIBLE}
	 *        
	 * @param jRSCondition jRS condition that should be converted to ruleLearn condition
	 * @param rlAttribute ruleLearn attribute to be used in the returned condition
	 * 
	 * @return ruleLearn condition corresponding to given jRS condition
	 * @throws UnsupportedOperationException if conversion could not be performed due to one of the following reasons:
	 *         <ul>
	 *         <li>{@code jRSRuleType} is neither {@link pl.poznan.put.cs.idss.jrs.rules.Rule#CERTAIN} nor {@link pl.poznan.put.cs.idss.jrs.rules.Rule#POSSIBLE},</li>
	 *         <li>if relation stored in given jRS condition is neither >=, <=, or =,</li>
	 *         <li>if type of the given condition is not {@link SingleCondition}</li>
	 *         </ul>
	 */
	Condition<EvaluationField> convertSingleJRSCondition(int jRSRuleType, pl.poznan.put.cs.idss.jrs.rules.Condition jRSCondition, EvaluationAttribute rlAttribute) {
		
		Condition<EvaluationField> condition;
		Relation relation;
		int attributeIndex;
		
		if (jRSCondition instanceof SingleCondition) {
			relation = ((SingleCondition)jRSCondition).getRelation();
			attributeIndex = jRSCondition.getAttributeInfo().getAttributeNumber();
			
			if (relation instanceof RelationAtLeast) {
				if (jRSRuleType == pl.poznan.put.cs.idss.jrs.rules.Rule.CERTAIN) {
					condition = 
							new ConditionAtLeastThresholdVSObject<EvaluationField>(new EvaluationAttributeWithContext(rlAttribute, attributeIndex),
							(new jRSField2rLField()).convertjRSField2rLField(relation.getReferenceValue(), rlAttribute.getPreferenceType()));
				} else {
					if (jRSRuleType == pl.poznan.put.cs.idss.jrs.rules.Rule.POSSIBLE) {
						condition =
								new ConditionAtLeastObjectVSThreshold<EvaluationField>(new EvaluationAttributeWithContext(rlAttribute, attributeIndex),
								(new jRSField2rLField()).convertjRSField2rLField(relation.getReferenceValue(), rlAttribute.getPreferenceType()));
					} else {
						throw new UnsupportedOperationException("Could not convert jRS rule of type other than CERTAIN or POSSIBLE.");
					}
				}
			} else {
				if (relation instanceof RelationAtMost) {
					if (jRSRuleType == pl.poznan.put.cs.idss.jrs.rules.Rule.CERTAIN) {
						condition =
								new ConditionAtMostThresholdVSObject<EvaluationField>(new EvaluationAttributeWithContext(rlAttribute, attributeIndex),
								(new jRSField2rLField()).convertjRSField2rLField(relation.getReferenceValue(), rlAttribute.getPreferenceType()));
					} else {
						if (jRSRuleType == pl.poznan.put.cs.idss.jrs.rules.Rule.POSSIBLE) {
							condition =
									new ConditionAtMostObjectVSThreshold<EvaluationField>(new EvaluationAttributeWithContext(rlAttribute, attributeIndex),
									(new jRSField2rLField()).convertjRSField2rLField(relation.getReferenceValue(), rlAttribute.getPreferenceType()));
						} else {
							throw new UnsupportedOperationException("Could not convert jRS rule of type other than CERTAIN or POSSIBLE");
						}
					}
				} else {
					if (relation instanceof RelationEqual) {
						if (jRSRuleType == pl.poznan.put.cs.idss.jrs.rules.Rule.CERTAIN) {
							condition =
									new ConditionEqualThresholdVSObject<EvaluationField>(new EvaluationAttributeWithContext(rlAttribute, attributeIndex),
									(new jRSField2rLField()).convertjRSField2rLField(relation.getReferenceValue(), rlAttribute.getPreferenceType()));
						} else {
							if (jRSRuleType == pl.poznan.put.cs.idss.jrs.rules.Rule.POSSIBLE) {
								condition =
										new ConditionEqualObjectVSThreshold<EvaluationField>(new EvaluationAttributeWithContext(rlAttribute, attributeIndex),
										(new jRSField2rLField()).convertjRSField2rLField(relation.getReferenceValue(), rlAttribute.getPreferenceType()));
							} else {
								throw new UnsupportedOperationException("Could not convert jRS rule of type other than CERTAIN or POSSIBLE");
							}
						}
					} else {
						throw new UnsupportedOperationException("Could not convert jRS relation other than >=, <= or =.");
					}
				}
			}
		} else {
			throw new UnsupportedOperationException("Could not convert jRS condition other than single condition.");
		}
		
		return condition;
	}
	
	/**
	 * Converts {@link pl.poznan.put.cs.idss.jrs.rules.RuleStatistics statistics of a jRS rule} to {@link RuleCharacteristics characteristics of a ruleLearn rule}.
	 * 
	 * @param ruleStatistics {@link pl.poznan.put.cs.idss.jrs.rules.RuleStatistics statistics of a jRS rule} that should be converted
	 * @return {@link RuleCharacteristics characteristics of a ruleLearn rule}, corresponding to given rule statistics
	 */
	RuleCharacteristics convertRuleStatistics2RuleCharacteristics(RuleStatistics ruleStatistics) {
		RuleCharacteristics ruleCharacteristics = new RuleCharacteristics();
		
		if (ruleStatistics != null) {
			if (ruleStatistics.statisticIsStored(pl.poznan.put.cs.idss.jrs.rules.RuleStatistics.SUPPORT)) {
				ruleCharacteristics.setSupport(ruleStatistics.getSupport());
			}
			if (ruleStatistics.statisticIsStored(pl.poznan.put.cs.idss.jrs.rules.RuleStatistics.STRENGTH)) {
				ruleCharacteristics.setStrength(ruleStatistics.getStrength());
			}
			if (ruleStatistics.statisticIsStored(pl.poznan.put.cs.idss.jrs.rules.RuleStatistics.CONFIDENCE)) {
				ruleCharacteristics.setConfidence(ruleStatistics.getConfidence());
			}
			if (ruleStatistics.statisticIsStored(pl.poznan.put.cs.idss.jrs.rules.RuleStatistics.COVERAGE_FACTOR)) {
				ruleCharacteristics.setCoverageFactor(ruleStatistics.getCoverageFactor());
			}
			if (ruleStatistics.statisticIsStored(pl.poznan.put.cs.idss.jrs.rules.RuleStatistics.QUANTITY_OF_COVERED_EXAMPLES)) {
				ruleCharacteristics.setCoverage(ruleStatistics.getQuantityOfCoveredExamples());
			}
			if (ruleStatistics.statisticIsStored(pl.poznan.put.cs.idss.jrs.rules.RuleStatistics.QUANTITY_OF_NEGATIVE_COVERED_EXAMPLES)) {
				ruleCharacteristics.setNegativeCoverage(ruleStatistics.getQuantityOfNegativeCoveredExamples());
			}
			if (ruleStatistics.statisticIsStored(pl.poznan.put.cs.idss.jrs.rules.RuleStatistics.INCONSISTENCY_MEASURE)) {
				ruleCharacteristics.setEpsilon(ruleStatistics.getInconsistencyMeasureValue());
			}
			if (ruleStatistics.statisticIsStored(pl.poznan.put.cs.idss.jrs.rules.RuleStatistics.EPSILON_PRIM_MEASURE)) {
				ruleCharacteristics.setEpsilonPrime(ruleStatistics.getEpsilonPrimMeasureValue());
			}
			if (ruleStatistics.statisticIsStored(pl.poznan.put.cs.idss.jrs.rules.RuleStatistics.F_CONFIRMATION_MEASURE)) {
				ruleCharacteristics.setFConfirmation(ruleStatistics.getFConfirmationMeasureValue());
			}
			if (ruleStatistics.statisticIsStored(pl.poznan.put.cs.idss.jrs.rules.RuleStatistics.A_CONFIRMATION_MEASURE)) {
				ruleCharacteristics.setAConfirmation(ruleStatistics.getAConfirmationMeasureValue());
			}
			if (ruleStatistics.statisticIsStored(pl.poznan.put.cs.idss.jrs.rules.RuleStatistics.Z_CONFIRMATION_MEASURE)) {
				ruleCharacteristics.setZConfirmation(ruleStatistics.getZConfirmationMeasureValue());
			}
			if (ruleStatistics.statisticIsStored(pl.poznan.put.cs.idss.jrs.rules.RuleStatistics.L_CONFIRMATION_MEASURE)) {
				ruleCharacteristics.setLConfirmation(ruleStatistics.getLConfirmationMeasureValue());
			}
			if (ruleStatistics.statisticIsStored(pl.poznan.put.cs.idss.jrs.rules.RuleStatistics.C1_CONFIRMATION_MEASURE)) {
				ruleCharacteristics.setC1Confirmation(ruleStatistics.getC1ConfirmationMeasureValue());
			}
		}
		
		return ruleCharacteristics;
	}
	
	/**
	 * Converts given jRS rule to corresponding ruleLearn rule + its characteristics.
	 * 
	 * @param jRSRule jRS rule (along with its statistics)
	 * @return {@link RuleWithCharacteristics pair} composed of ruleLearn rule and its characteristics
	 * 
	 * @throws UnsupportedOperationException if type of any condition in the given jRS rule is other than {@link SingleCondition}
	 * @throws UnsupportedOperationException if and relation of any condition in the given jRS rule is
	 * @throws InvalidValueException if given jRS rule has type other than {@link pl.poznan.put.cs.idss.jrs.rules.Rule.CERTAIN}
	 *         or {@link pl.poznan.put.cs.idss.jrs.rules.Rule.POSSIBLE}
	 */
	RuleWithCharacteristics convertJRSRule2rlRule(pl.poznan.put.cs.idss.jrs.rules.Rule jRSRule, EvaluationAttribute[] rlAttributes) {
		pl.poznan.put.cs.idss.jrs.rules.Condition[] jRSConditions = jRSRule.getConditionsAsArray();
		pl.poznan.put.cs.idss.jrs.rules.Condition jRSDecision = jRSRule.getDecisions()[0];
		
		List<Condition<EvaluationField>> conditions = new ObjectArrayList<Condition<EvaluationField>>();
		Condition<EvaluationField> decision;
		
		for (pl.poznan.put.cs.idss.jrs.rules.Condition jRSCondition : jRSConditions) {
			conditions.add(convertSingleJRSCondition(jRSRule.getType(), jRSCondition, rlAttributes[jRSCondition.getAttributeInfo().getAttributeNumber()]));
		}
		decision = convertSingleJRSCondition(jRSRule.getType(), jRSDecision, rlAttributes[jRSDecision.getAttributeInfo().getAttributeNumber()]);
		
		RuleType ruleType;
		switch (jRSRule.getType()) {
		case pl.poznan.put.cs.idss.jrs.rules.Rule.CERTAIN:
			ruleType = RuleType.CERTAIN;
			break;
		case pl.poznan.put.cs.idss.jrs.rules.Rule.POSSIBLE:
			ruleType = RuleType.POSSIBLE;
			break;
		default: throw new InvalidValueException("Incorrect rule type. Only certain and possible rules can be converted to ruleLearn's RuleML format.");
		}
		
		Rule rule = new Rule(ruleType, conditions, decision);
		RuleCharacteristics ruleCharacteristics = convertRuleStatistics2RuleCharacteristics(jRSRule.getRuleStatistics());
		
		return new RuleWithCharacteristics(rule, ruleCharacteristics);
	}
	
	/**
	 * Converts *.rules file produced by jRS library (storing attributes and decision rules) to a pair of ruleLearn files:
	 * JSON file with metadata (attributes) and XML file with the rules, in RuleML format.
	 * 
	 * @param rulesFilePath path to input *.rules file containing information about attributes and rules (possibly along with their statistics)
	 * @param jsonAttributesFilePath path to output file where metadata (attributes) should be written in JSON format
	 * @param ruleMLFilePath path to output XML file where rules should be written in RuleML format
	 * @param prettyPrinting tells if pretty printing in the two output files should be applied
	 * 
	 * @throws InvalidValueException if any attribute has incorrect preference type or type (kind)
	 * @throws UnsupportedOperationException if any jRS attribute has initial value of type other than integer, float, or enum
	 */
	void convertRules2RuleML(String rulesFilePath, String jsonAttributesFilePath, String ruleMLFilePath, boolean prettyPrinting) {
		RulesContainer ruleContainer;
		try {
			ruleContainer = RulesContainer.loadRules(rulesFilePath, new SimpleParseLog());
		} catch (FileNotFoundException exception) {
			OM.println("Rules could not be read from file " + rulesFilePath);
			return;
		}
		
		if (ruleContainer != null) {
			EvaluationAttribute[] rLAttributes = (new jRSAttributes2rLAttributes()).convertJRSAttributes2rLAttributes(ruleContainer.getLearningAttributes());
			
			InformationTable informationTable = new InformationTable(rLAttributes, new ObjectArrayList<Field[]>(), true); //empty list of rows (i.e., no objects!)
			
			InformationTableWriter informationTableWriter = new InformationTableWriter(prettyPrinting);
			OM.println("Pretty printing in JSON file with metadata (attributes) set to: " + prettyPrinting);
			
			try (FileWriter fileWriter = new FileWriter(jsonAttributesFilePath)) {
				informationTableWriter.writeAttributes(informationTable, fileWriter);
				OM.println("Attributes written to file " + jsonAttributesFilePath);
				
			}
			catch (IOException ex) {
				OM.println(ex.toString());
			}
			
			//---
			
			ArrayList<pl.poznan.put.cs.idss.jrs.rules.Rule> jRSRules = new ArrayList<pl.poznan.put.cs.idss.jrs.rules.Rule>();
			
			//put all jRS rules on one list
			if (ruleContainer.containsRules(pl.poznan.put.cs.idss.jrs.rules.Rule.CERTAIN, pl.poznan.put.cs.idss.jrs.rules.Rule.AT_LEAST)) {
				jRSRules.addAll(ruleContainer.getRules(pl.poznan.put.cs.idss.jrs.rules.Rule.CERTAIN, pl.poznan.put.cs.idss.jrs.rules.Rule.AT_LEAST));
			}
			if (ruleContainer.containsRules(pl.poznan.put.cs.idss.jrs.rules.Rule.POSSIBLE, pl.poznan.put.cs.idss.jrs.rules.Rule.AT_LEAST)) {
				jRSRules.addAll(ruleContainer.getRules(pl.poznan.put.cs.idss.jrs.rules.Rule.POSSIBLE, pl.poznan.put.cs.idss.jrs.rules.Rule.AT_LEAST));
			}
			
			if (ruleContainer.containsRules(pl.poznan.put.cs.idss.jrs.rules.Rule.CERTAIN, pl.poznan.put.cs.idss.jrs.rules.Rule.AT_MOST)) {
				jRSRules.addAll(ruleContainer.getRules(pl.poznan.put.cs.idss.jrs.rules.Rule.CERTAIN, pl.poznan.put.cs.idss.jrs.rules.Rule.AT_MOST));
			}
			if (ruleContainer.containsRules(pl.poznan.put.cs.idss.jrs.rules.Rule.POSSIBLE, pl.poznan.put.cs.idss.jrs.rules.Rule.AT_MOST)) {
				jRSRules.addAll(ruleContainer.getRules(pl.poznan.put.cs.idss.jrs.rules.Rule.POSSIBLE, pl.poznan.put.cs.idss.jrs.rules.Rule.AT_MOST));
			}
			
			if (ruleContainer.containsRules(pl.poznan.put.cs.idss.jrs.rules.Rule.CERTAIN, pl.poznan.put.cs.idss.jrs.rules.Rule.EQUAL)) {
				jRSRules.addAll(ruleContainer.getRules(pl.poznan.put.cs.idss.jrs.rules.Rule.CERTAIN, pl.poznan.put.cs.idss.jrs.rules.Rule.EQUAL));
			}
			if (ruleContainer.containsRules(pl.poznan.put.cs.idss.jrs.rules.Rule.POSSIBLE, pl.poznan.put.cs.idss.jrs.rules.Rule.EQUAL)) {
				jRSRules.addAll(ruleContainer.getRules(pl.poznan.put.cs.idss.jrs.rules.Rule.POSSIBLE, pl.poznan.put.cs.idss.jrs.rules.Rule.EQUAL));
			}
			
			Rule[] rlRules = new Rule[jRSRules.size()]; //rules in ruleLearn's RuleML format
			RuleCharacteristics[] rlRuleCharacteristics = new RuleCharacteristics[jRSRules.size()];
			
			RuleWithCharacteristics rLRuleWithCharacteristics;
			
			int index = 0;
			for (pl.poznan.put.cs.idss.jrs.rules.Rule jRSRule : jRSRules) {
				rLRuleWithCharacteristics = convertJRSRule2rlRule(jRSRule, rLAttributes);
				rlRules[index] = rLRuleWithCharacteristics.rule;
				rlRuleCharacteristics[index] = rLRuleWithCharacteristics.ruleCharacteristics;
				index++;
			}
			
			VolatileCachesCleaner.clearVolatileCaches(rLAttributes); //clear volatile caches of all used evaluation field caching factories
			
			writeRuleML(new RuleSetWithCharacteristics(rlRules, rlRuleCharacteristics, true), ruleMLFilePath);
		} //if
	}
	
	/**
	 * Writes to file, in RuleML format, given rules and their characteristics.
	 * 
	 * @param ruleSetWithCharacteristics set of rules along with their characteristics
	 * @param ruleMLFilePath path to disk file where rules and their characteristics should be written in RuleML format
	 */
	void writeRuleML(RuleSetWithCharacteristics ruleSetWithCharacteristics, String ruleMLFilePath) {
		RuleMLBuilder ruleMLBuilder = new RuleMLBuilder();
		String ruleML = ruleMLBuilder.toRuleMLString(ruleSetWithCharacteristics, 1);
		
		try (FileWriter fileWriter = new FileWriter(ruleMLFilePath)) {
			fileWriter.write(ruleML);
			OM.println("Rules written to file " + ruleMLFilePath);
			fileWriter.close();
		}
		catch (IOException ex) {
			OM.println(ex.toString());
		}
	}
	
	/**
	 * Application entry point.
	 * 
	 * @param args input arguments of this converter.
	 *        Syntax: Rules2RuleMLConverter <rules-file-path> <JSON-attributes-file-path> <ruleML-file-path> <use-pretty-printing>
	 * @throws UnsupportedOperationException when rules cannot be converted from jRS rules file to a pair of ruleLearn JSON file with attributes
	 *         and XML file with rules in RuleML format
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
			OM.println("Rules2RuleMLConverter <rules-file-path> <JSON-attributes-file-path> <ruleML-file-path> <pretty>");
			OM.println("Example:");
			OM.println("Rules2RuleMLConverter GermanCredit.rules GermanCredit.meta.json GermanCredit.rules.xml pretty");
			return;
		}
		
		Rules2RuleMLConverter rules2RuleMLConverter = new Rules2RuleMLConverter();
		rules2RuleMLConverter.convertRules2RuleML(args[0], args[1], args[2], args.length > 3 && args[3].strip().equalsIgnoreCase("pretty"));
	}
	
}
