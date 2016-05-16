RM=rm
MANDIR=$(DESTDIR)@mandir@/man1

XSLTPROC=xsltproc
XSLTPROC_FLAGS=@XSLTPROC_FLAGS@
XHTML_STYLESHEET=$(srcdir)/xsl/xhtml.xsl
CHUNK_XHTML_STYLESHEET=$(srcdir)/xsl/xhtml-chunk.xsl
XML_CATALOG_FILES=xsl/catalog.xml
STYLESHEETS=$(CHUNK_XHTML_STYLESHEET) $(srcdir)/xsl/xhtml-common.xsl

man_MANS = \
	oprofile.1 \
	opcontrol.1 \
	opreport.1 \
	opannotate.1 \
	opgprof.1 \
	ophelp.1 \
	oparchive.1 \
	opimport.1

htmldir = $(prefix)/share/doc/oprofile
dist_html_DATA = oprofile.html internals.html opreport.xsd op-jit-devel.html

if have_xsltproc

oprofile.html: ${top_srcdir}/doc/oprofile.xml
	XML_CATALOG_FILES=$(XML_CATALOG_FILES) $(XSLTPROC) $(XSLTPROC_FLAGS) -o $@ --stringparam version @VERSION@ $(XHTML_STYLESHEET) $<

op-jit-devel.html: ${top_srcdir}/doc/op-jit-devel.xml
	XML_CATALOG_FILES=$(XML_CATALOG_FILES) $(XSLTPROC) $(XSLTPROC_FLAGS) -o $@ --stringparam version @VERSION@ $(XHTML_STYLESHEET) $<

internals.html: ${top_srcdir}/doc/internals.xml
	XML_CATALOG_FILES=$(XML_CATALOG_FILES) $(XSLTPROC) $(XSLTPROC_FLAGS) -o $@ --stringparam version @VERSION@ $(XHTML_STYLESHEET) $<

# rules to generate oprofile.sf.net/doc files

doc/index.html: ${top_srcdir}/doc/oprofile.xml
	-mkdir doc/
	$(XSLTPROC) -o doc/ $(XSLTPROC_FLAGS) --stringparam version @VERSION@ $(CHUNK_XHTML_STYLESHEET) $<

doc/devel/index.html: ${top_srcdir}/doc/op-jit-devel.xml
	-mkdir doc/devel/
	$(XSLTPROC) -o doc/devel/ $(XSLTPROC_FLAGS) --stringparam version @VERSION@ $(CHUNK_XHTML_STYLESHEET) $<

doc/internals/index.html: ${top_srcdir}/doc/internals.xml
	-mkdir doc/internals/
	$(XSLTPROC) -o doc/internals/ $(XSLTPROC_FLAGS) --stringparam version @VERSION@ $(CHUNK_XHTML_STYLESHEET) $<

chunk: doc/index.html doc/internals/index.html doc/devel/index.html
	cp ${top_srcdir}/doc/buffers.png doc/internals/

else

oprofile.html:
	touch $@

internals.html:
	touch $@

op-jit-devel.html:
	touch $@

chunk:

endif

distclean-local:
	$(RM) -f xsl/catalog-1.xml xsl/catalog.xml

clean-local:
	$(RM) -f $(generated_mans)

# these files are not cleaned by make uninstall automake bug ?
uninstall-local:
	rm -f @mandir@/cat1/oprofile.1.gz
	@for f in $(LINK_LIST); do		\
		rm -f $(CATDIR)/cat1/$$f.gz;	\
	done

EXTRA_DIST = \
	oprofile.1 \
	oprofile.1.in \
	oprofile.xml \
	op-jit-devel.xml \
	internals.xml \
	buffers.png \
	xsl/catalog-1.xml.in \
	xsl/xhtml.xsl \
	xsl/xhtml-common.xsl \
	xsl/xhtml-chunk.xsl \
	srcdoc/Doxyfile.in \
	srcdoc/Makefile
