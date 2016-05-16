# HOW TO BUILD SOURCE.ANDROID.COM #

source.android.com contains tutorials, references, and miscellaneous
information relating to the Android Open Source Project (AOSP). The current
iteration of this site is fully static HTML (notably lacking in javascript and
doxygen content), and is and/or was maintained by skyler (illustrious intern
under Dan Morrill and assistant to the almighty JBQ).

## Short Instructions ##

Run the build script, from the same directory as this file:

    python scripts/build.py

This generates the directory ./out, which is the fully built site. Hoorah. 

The included scripts/micro-httpd.py script is helpful for testing the site on
your own machine. Running it will start up a tiny HTTP server that you can hit
to test changes in a browser:

    cd ./out
    HTTP_PORT=8080 python ../scripts/micro-httpd.py

### Markdown ###
Markdown is a very simple markup format for plain-text that converts it to
decent HTML. Useful docs:
http://daringfireball.net/projects/markdown/syntax

Yes, it was created by John Gruber himself. BWAHAHA!

### Dependencies ###

You need the Python markdown implementation. The original Perl impl probably
will NOT work.

For (Goo|U)buntu:
% sudo apt-get install python-markdown

For Mac:
$ sudo easy_install ElementTree
$ sudo easy_install Markdown

More information here:
http://www.freewisdom.org/projects/python-markdown/Installation

### Contents Included in Box ###

Necessary source files include:

    src/        individual page content in markdown format
    templates/  templates for page content

and the following content which is copied directly:

    assets/     stylish things that make the page look pretty
    images/     exactly what it sounds like

### Structure of Site Source ###

The build script assumes that
- Every .md file under src/ is an individual page in markdown format.
- Each directory under src/ is a tab of source.android.com and contains its
  particular sidebar.  Note, the sidebar in the root of site_src/ itself is
  present but empty.
- Please use .md if possible (because this will pick up the global site CSS
  and layout.) But the build.py script will indeed copy arbitrary files to the
  output dir, so it is possible to simply place .html, .pdf, and similar files
  to the src/ tree and they will be copied directly to ./out.


# HOW TO PUSH SOURCE.ANDROID.COM TO PROD #
Coming soon. For now, harass morrildl, jbq, and/or btmura.


# SORDID HISTORY OF SOURCE.ANDROID.COM #

Once upon a time, source.android.com used to be a site on Sites.
Then it was rewritten to use the developer SDK docs, but this was hard to edit
and overkill.
Now it is as you see it.
