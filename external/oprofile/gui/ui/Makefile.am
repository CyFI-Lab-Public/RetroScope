EXTRA_DIST = oprof_start.base.ui

if have_qt

AM_CPPFLAGS = @QT_INCLUDES@

AM_CXXFLAGS = @OP_CXXFLAGS@

noinst_LIBRARIES = liboprof_start.a
nodist_liboprof_start_a_SOURCES = oprof_start.base.cpp oprof_start.base.moc.cpp

oprof_start.base.h: oprof_start.base.ui
	$(UIC) -o $@ $<

oprof_start.base.cpp: oprof_start.base.h oprof_start.base.ui
	$(UIC) -o $@ -impl $^

oprof_start.base.moc.cpp: oprof_start.base.h
	$(MOC) -o $@ $<

clean-local:
	rm -f oprof_start.base.h oprof_start.base.cpp oprof_start.base.moc.cpp

endif
