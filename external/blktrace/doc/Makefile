DOCTMP	= blktrace.log blktrace.aux blktrace.dvi

all: blktrace.pdf

blktrace.tex:
	@touch blktrace.tex

blktrace.pdf: blktrace.tex
	@latex blktrace.tex
	@latex blktrace.tex
	@dvipdfm -p a4 blktrace
	@rm -rf $(DOCTMP)

clean:
	-rm -f blktrace.pdf $(DOCTMP)
