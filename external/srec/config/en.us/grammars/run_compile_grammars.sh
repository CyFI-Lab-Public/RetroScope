BASELINE_PAR_FILE=../baseline11k.par

GMR=enroll
grxmlcompile -par $BASELINE_PAR_FILE -grxml $GMR.grxml -vocab dictionary/$GMR.ok
make_g2g -base $GMR -out $GMR.g2g

GMR=bothtags5
grxmlcompile -par $BASELINE_PAR_FILE -grxml $GMR.grxml
make_g2g -base $GMR,addWords=100 -out $GMR.g2g

GMR=dynamic-test
grxmlcompile -par $BASELINE_PAR_FILE -grxml $GMR.grxml
make_g2g -base $GMR,addWords=1000 -out $GMR.g2g

GMR=lookup
grxmlcompile -par $BASELINE_PAR_FILE -grxml $GMR.grxml
make_g2g -base $GMR,addWords=1000 -out $GMR.g2g

GMR=digits
grxmlcompile -par $BASELINE_PAR_FILE -grxml $GMR.grxml
make_g2g -base $GMR,addWords=1000 -out $GMR.g2g

GMR=rootslot
grxmlcompile -par $BASELINE_PAR_FILE -grxml $GMR.grxml
make_g2g -base $GMR,addWords=3000 -out $GMR.g2g

GMR=boolean
grxmlcompile -par $BASELINE_PAR_FILE -grxml $GMR.grxml
make_g2g -base $GMR -out $GMR.g2g

GMR=ipaq_commands
grxmlcompile -par $BASELINE_PAR_FILE -grxml $GMR.grxml
make_g2g -base $GMR -out $GMR.g2g

GMR=homonym_test1
grxmlcompile -par $BASELINE_PAR_FILE -grxml $GMR.grxml
make_g2g -base $GMR -out $GMR.g2g

GMR=homonym_test2
grxmlcompile -par $BASELINE_PAR_FILE -grxml $GMR.grxml
make_g2g -base $GMR -out $GMR.g2g

GMR=homonym_test3
grxmlcompile -par $BASELINE_PAR_FILE -grxml $GMR.grxml
make_g2g -base $GMR -out $GMR.g2g

GMR=homonym_test4
grxmlcompile -par $BASELINE_PAR_FILE -grxml $GMR.grxml
make_g2g -base $GMR -out $GMR.g2g

GMR=slot_test1
grxmlcompile -par $BASELINE_PAR_FILE -grxml $GMR.grxml
make_g2g -base $GMR -out $GMR.g2g

GMR=slot_test2
grxmlcompile -par $BASELINE_PAR_FILE -grxml $GMR.grxml
make_g2g -base $GMR -out $GMR.g2g

