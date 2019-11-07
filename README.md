# jRS2ruleLearn
Package of converters between [java Rough Sets](http://www.cs.put.poznan.pl/mszelag/Software/software.html) (jRS) library data and rule formats to [ruleLearn](https://github.com/ruleLearn/rulelearn) (rL) library data and rule formats.<br/>
Employs jRS library included as a separate JAR, compiled with Java 11. Depends on rL library (via build.gradle dependency).

## `Configuration of text files in the project`:
UTF-8 encoding<br/>
LF line endings

## `Building with gradle` (necessary before first calculations; requires Java 11 JDK (or higher)):
**gradlew fatJar**

## `Supported conversions (jRS->rL)`:
- ISF (information system file) -> JSON file containing attributes (metadata) + JSON file containing evaluations of objects:

  **Isf2JsonConverter &lt;ISF-file-path> &lt;JSON-attributes-file-path> &lt;JSON-data-file-path> &lt;pretty>**

- &ast;.rules file -> JSON file containing attributes (metadata) + XML file (in RuleML format) containing rules:

  **Rules2RuleMLConverter &lt;rules-file-path> &lt;JSON-attributes-file-path> &lt;ruleML-file-path> &lt;pretty>**

## `Examples of use` (requires Java 11 JRE (or higher)):

?> cd ./scripts<br/>
?> Isf2JsonConverter "../data/isf/windsor.isf" "../data/json-metadata/windsor.meta.json" "../data/json-objects/windsor.data.json" pretty<br/>
?> Rules2RuleMLConverter "../data/rules/GermanCredit.rules" "../data/json-metadata/GermanCredit.meta.json" "../data/ruleml/GermanCredit.rules.xml" pretty<br/>

The above command starting with **Isf2JsonConverter** reads **windsor.isf** in the **data/isf** directory, and produces **windsor.meta.json** and **windsor.data.json** in the directories **data/json-metadata** and **data/json-objects**, respectively, both formatted in a pretty way (with additional spaces). If the last parameter (**pretty**) was not specified, both JSON files would be minified (to reduce their size).

The above command starting with **Rules2RuleMLConverter** reads **GermanCredit.rules** in the **data/rules** directory, and produces **GermanCredit.meta.json** and **GermanCredit.rules.xml** in the directories **data/json-metadata** and **data/ruleml**, respectively, with JSON file formatted in a pretty way (with additional spaces). If the last parameter (**pretty**) was not specified, resulting JSON file would be minified (to reduce its size).