The JavaParser and JavaLexer classes were built using ANTLR.
To regenerate these classes, download the ANTLR java binaries
JAR file from http://www.antlr.org/download.html.
Then run that JAR from the command line
("How do I use ANTLR v3 from the command line" -
http://www.antlr.org/wiki/pages/viewpage.action?pageId=729)
using the -debug flag so that a parse tree is generated
(see "How can I build parse trees not ASTs" on the ANTLR FAQ -  http://www.antlr.org/wiki/pages/viewpage.action?pageId=1760).

When this step was last done, there were some extra files generated,
these were ignored and discarded. For use, see the Parse Trees link
above for a basic example.

Steps:
java -Xmx1G -jar ~/Downloads/antlr-3.3-complete.jar -debug src/com/google/doclava/parser/Java.g
