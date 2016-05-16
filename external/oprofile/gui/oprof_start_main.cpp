/**
 * @file oprof_start_main.cpp
 * main routine for GUI start
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author Philippe Elie
 * @author John Levon
 */

#include <qapplication.h>

#include "oprof_start.h"

int main(int argc, char* argv[])
{
	QApplication a(argc, argv);

	oprof_start* dlg = new oprof_start();

	a.setMainWidget(dlg);

	dlg->show();

	return a.exec();
}
