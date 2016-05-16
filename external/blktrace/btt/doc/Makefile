DOCTMP	= btt.log btt.aux btt.dvi btt.toc btt.tex.bak

all: btt.pdf

btt.tex: activity.eps qhist.eps dhist.eps seek.eps
	@touch btt.tex

btt.pdf: btt.tex
	@latex btt.tex
	@latex btt.tex
	@dvipdfm -p a4 btt
	@rm -rf $(DOCTMP)

clean:
	-rm -f btt.pdf $(DOCTMP)
