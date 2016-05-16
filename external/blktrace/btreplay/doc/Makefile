DOCTMP  = btreplay.log btreplay.aux btreplay.dvi btreplay.toc

all: btreplay.dvi btreplay.pdf

btreplay.tex:
	@touch btreplay.tex

btreplay.dvi: btreplay.tex abstract.tex
	@latex btreplay.tex
	@latex btreplay.tex

btreplay.pdf: btreplay.dvi
	@dvipdfm -p letter btreplay

clean:
	-rm -f $(DOCTMP)
	-rm -f *.bak *.ps *.pdf
	@rm -rf btreplay
