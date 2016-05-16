These scripts are examples to get you started.

== Assembly ==

To build the feedTools.jar, use buildFeedToolsJar.sh (or .xml).
To create a zip of all the RSS-related code, use buildFeedToolsZip.sh (or .xml).

== Feed Manipulation ==

To do feed manipulation, like creating a feed, adding a entry to a feed, querying 
for attribute values in a feed, or changing attribute values in a feed, look at 
feedManipulation.sh (and .xml) and create a copy similar to suit your needs.

== Feed Publishing ==

To publish a feed, use a script similar to feedPublish.sh (and .xml). 

== Feed Validation ==

To validate a feed against the latest schema, you can use one of two EMF-based ant tasks. 
Examples of both are shown in feedValidate.xml, including the classpath required to run the task.
In the Dynamic case, the schema is used to validate the feed xml; in the Generated case, the 
schema is used to create an .ecore model, which is used to generate model implementation and
validation code, which is then used to validate the feed xml. Because the two techniques yield 
slightly different error messages when validating invalid feed data, they are both provided for
comparison.

To build the feedValidator.jar, use buildFeedValidatorJar.xml.

== Feed Watching (And Response) ==

To watch a feed for changes or the appearance of specific attribute values (like 
test results), use a script similar to feedWatch.sh (and .xml), along with properties 
like those in properties/feedWatch.*.properties

The script sendEmailAlert.sh is provided as an example of what to in response to 
a feed change. You can customize the response to suit your needs. 

Additional documentation can be found in the *.xml Ant scripts and *.properties files.