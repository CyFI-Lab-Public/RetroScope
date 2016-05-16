#
# This ProGuard configuration file illustrates how to process J2ME midlets.
# Usage:
#     java -jar proguard.jar @midlets.pro
#

# Specify the input jars, output jars, and library jars.

-injars  in.jar
-outjars out.jar

-libraryjars /usr/local/java/wtk2.1/lib/midpapi20.jar
-libraryjars /usr/local/java/wtk2.1/lib/cldcapi11.jar

# Preverify the code suitably for Java Micro Edition.

-microedition

# Allow methods with the same signature, except for the return type,
# to get the same obfuscation name.

-overloadaggressively

# Put all obfuscated classes into the nameless root package.

-repackageclasses ''

# Allow classes and class members to be made public.

-allowaccessmodification

# On Windows, you can't use mixed case class names,
# should you still want to use the preverify tool.
#
# -dontusemixedcaseclassnames

# Preserve all public midlets.

-keep public class * extends javax.microedition.midlet.MIDlet

# Print out a list of what we're preserving.

-printseeds

# Preserve all native method names and the names of their classes.

-keepclasseswithmembernames class * {
    native <methods>;
}

# Your midlet may contain more items that need to be preserved; 
# typically classes that are dynamically created using Class.forName:

# -keep public class mypackage.MyClass
# -keep public interface mypackage.MyInterface
# -keep public class * implements mypackage.MyInterface
