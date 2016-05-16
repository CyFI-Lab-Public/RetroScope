These files are examples to get you started. Since you cannot commit 
your files into CVS here, just point the scripts at your own properties
files, similar in content to these, and run accordingly. 

You can also run the Ant scripts without properties files if you set 
the attributes statically in your ant script.

Or, you can call the Tasks themselves within another Java class. 
RSSFeedAddEntryTask is an example of this, in that it optionally 
calls RSSFeedCreateFeedTask if no existing feed file already exists. 
RSSFeedGetPropertyTask is a wrapper for RSSFeedUpdateEntryTask, which 
also queries for an attribute value, but instead of changing it, 
simply returns it.