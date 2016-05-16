WINDOWBUILDER PROPERTY SHEET LIBRARY
-------------------------------------

This project is a fork of a subset of the WindowBuilder Eclipse
plugin: http://www.eclipse.org/windowbuilder/

Specifically, it contains the subset of WindowBuilder related to the
propertysheet, intended for reuse in AOSP by the ADT plugin.

The fork was modified as follows:
* Started with revision 424 from the trunk:
  http://dev.eclipse.org/svnroot/tools/org.eclipse.windowbuilder/trunk

* Extracted the property package from org.eclipse.wb.core:
   src/org/eclipse/wb/internal/core/model/property
  and then everything it transitively references.  This turns out to
  be a lot. I then started pruning out references to code we don't
  need, such as support for editing Java constructs such as enums, or
  dealing with a Java code model, etc.  This means some of the files
  have been edited to remove methods and fields. For example, the
  property category code was modified to no longer support the
  persistent storage of categories.

* The WindowBuilder code depended on a number of Apache Commons
  libraries such as collections, lang, etc. Since ADT already uses
  Guava, which provides a lot of the same functionality, I replaced
  all the Commons calls with Guava calls in order to avoid having to
  make ADT depend on (and load at runtime) the Commons libraries.

* Finally, the propertysheet code was made into a library instead of a
  plugin, such that it can be loaded into the ADT plugin.  This meant
  mostly rewriting the DesignerPlugin class. It has kept its name
  (since a lot of code references it for logging, resource loading
  etc), but it is no longer an actual plugin. Instead it has init and
  dispose methods for use by the AdtPlugin, and for logging it
  delegates to the ADT plugin, etc.

* Icons were moved into the DesignerPlugin package such that the
  resource loading code could use a relative path, since with an
  absolute path it would be looking in the embedding plugin's
  resources.

* To be consistent with the ADT codebase, I converted the files from
  \r\n to \n newlines. Other than that, all formatting was left
  unmodified.

* Removed unused resources such as unreferences colors from
  IColorConstants, unneeded messages from ModelMessages, and so on.

* Note also that this Eclipse project is using a modified version of
  the standard ADT Eclipse compiler settings: methods overriding other
  methods and interfaces *without* using an @Override annotation are
  ignored, since they were not using @Override annotations in the
  WindowBuilder source base.
  

ADT ENHANCEMENTS
------------------
* I also modified the propertysheet in a few ways to add features
  needed by ADT. These are all bracketed in the codebase with
   // BEGIN ADT MODIFICATIONS
   ...
   // END ADT MODIFICATIONS

  Specifically, I made the property table able to expand all and
  collapse all. Properties have sorting priorities, and have separate
  name and title attributes (and tooltips show the property name
  rather than the title.) Text property editors allow field completion
  by providing IContentProposalProvider (and optionally
  ILabelProvider) instances via their getAdapter method. And the
  property table will color values differently based on whether the
  property is modified. (This allows us to draw default attributes
  differently). Finally, the propertysheet now supports "expand by
  default" (and for certain categories to be excluded, such as
  deprecations).

WINDOW DOCKING
---------------

The window docking support (the "FlyoutControlComposite" and
supporting classes) was also included, since it's used to present the
property sheet view in ADT. This code was also modified in a couple of
minor ways, using the same modification markers as above:
- Support invisible children (where the whole flyout is hidden)
- Added a "dismiss hover" method used to hide a temporary hover
  (needed when the hovers are used with native drag & drop)
- Added a listener interface and notification when window states chane
  (used to auto-zoom the layout canvas when windows are collapsed or
  expanded).
- Changed the sizeall cursor used for dragging composites from the SWT
  SIZE_ALL cursor to the HAND cursor since (at least on Mac) the
  cursor looked wrong for docking.

UPDATES
--------

We should keep an eye on the propertysheet code in WindowBuilder and
migrate bug fixes and feature enhancements. To do that, first check
out revision 424 from
http://dev.eclipse.org/svnroot/tools/org.eclipse.windowbuilder/trunk
That's the same baseline that this fork was based on.
You can limit the checkout to just the org.eclipse.wb.core tree.

Then check out the newest revision of WindowBuilder in a separate
directory.

Now diff the two trees. Look for diffs in the packages related to the
propertysheet; this is going to be the packages that are present in
this library.  If any of the diffs are related to the propertysheet or
supporting code, apply them to this library, and then update this
document to contain the new baseline revision (use 'svnversion .' to
get the number).  Note that the diffs may need some rewriting if they
reference Apache Commons code.

Note that the ComponentsPropertiesPage.java class which is the main
window in WindowBuilder is not used in our implementation; we instead
have the PropertySheetPage class in ADT, so changes in that class
should be checked to see whether they apply to our property sheet page
(which uses the PropertyTable in a similar way, but obviously is based
around our own UI model rather than the WindowBuilder ObjectInfo
model.
