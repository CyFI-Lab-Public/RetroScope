/**
 * @file oprof_start.cpp
 * The GUI start main class
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author Philippe Elie
 * @author John Levon
 */

#include <sys/stat.h>
#include <unistd.h>

#include <ctime>
#include <cstdio>
#include <cmath>
#include <sstream>
#include <iostream>
#include <fstream>
#include <algorithm>

#include <qlineedit.h>
#include <qlistview.h>
#include <qcombobox.h>
#include <qlistbox.h>
#include <qfiledialog.h>
#include <qbuttongroup.h>
#include <qcheckbox.h>
#include <qtabwidget.h>
#include <qmessagebox.h>
#include <qvalidator.h>
#include <qlabel.h>
#include <qpushbutton.h>
#include <qheader.h>

#include "config.h"
#include "oprof_start.h"
#include "op_config.h"
#include "op_config_24.h"
#include "string_manip.h"
#include "op_cpufreq.h"
#include "op_alloc_counter.h"
#include "oprof_start_util.h"
#include "file_manip.h"

#include "op_hw_config.h"

using namespace std;

static char const * green_xpm[] = {
"16 16 2 1",
" 	c None",
".	c #00FF00",
"    .......     ",
"  ...........   ",
" .............  ",
" .............  ",
"............... ",
"............... ",
"............... ",
"............... ",
"............... ",
"............... ",
"............... ",
" .............  ",
" .............  ",
"  ...........   ",
"    .......     ",
"                " };

static char const * red_xpm[] = {
"16 16 2 1",
" 	c None",
".	c #FF0000",
"    .......     ",
"  ...........   ",
" .............  ",
" .............  ",
"............... ",
"............... ",
"............... ",
"............... ",
"............... ",
"............... ",
"............... ",
" .............  ",
" .............  ",
"  ...........   ",
"    .......     ",
"                " };

static QPixmap * green_pixmap;
static QPixmap * red_pixmap;


op_event_descr::op_event_descr()
	:
	counter_mask(0),
	val(0),
	unit(0),
	min_count(0)
{
}


oprof_start::oprof_start()
	:
	oprof_start_base(0, 0, false, 0),
	event_count_validator(new QIntValidator(event_count_edit)),
	current_event(0),
	cpu_speed(op_cpu_frequency()),
	total_nr_interrupts(0)
{
	green_pixmap = new QPixmap(green_xpm);
	red_pixmap = new QPixmap(red_xpm);
	vector<string> args;
	args.push_back("--init");

	if (do_exec_command(OP_BINDIR "/opcontrol", args))
		exit(EXIT_FAILURE);

	cpu_type = op_get_cpu_type();
	op_nr_counters = op_get_nr_counters(cpu_type);

	if (cpu_type == CPU_TIMER_INT) {
		setup_config_tab->removePage(counter_setup_page);
	} else {
		fill_events();
	}

	op_interface interface = op_get_interface();
	if (interface == OP_INTERFACE_NO_GOOD) {
		QMessageBox::warning(this, 0, "Couldn't determine kernel"
		                     " interface version");
		exit(EXIT_FAILURE);
	}
	bool is_26 = interface == OP_INTERFACE_26;

	if (is_26) {
		note_table_size_edit->hide();
		note_table_size_label->hide();
		if (!op_file_readable("/dev/oprofile/backtrace_depth")) {
			callgraph_depth_label->hide();
			callgraph_depth_edit->hide();
		}
	} else {
		callgraph_depth_label->hide();
		callgraph_depth_edit->hide();
		buffer_watershed_label->hide();
		buffer_watershed_edit->hide();
		cpu_buffer_size_label->hide();
		cpu_buffer_size_edit->hide();
	}

	// setup the configuration page.
	kernel_filename_edit->setText(config.kernel_filename.c_str());

	no_vmlinux->setChecked(config.no_kernel);

	buffer_size_edit->setText(QString().setNum(config.buffer_size));
	buffer_watershed_edit->setText(QString().setNum(config.buffer_watershed));
	cpu_buffer_size_edit->setText(QString().setNum(config.cpu_buffer_size));
	note_table_size_edit->setText(QString().setNum(config.note_table_size));
	callgraph_depth_edit->setText(QString().setNum(config.callgraph_depth));
	verbose->setChecked(config.verbose);
	separate_lib_cb->setChecked(config.separate_lib);
	separate_kernel_cb->setChecked(config.separate_kernel);
	separate_cpu_cb->setChecked(config.separate_cpu);
	separate_thread_cb->setChecked(config.separate_thread);

	// the unit mask check boxes
	hide_masks();

	event_count_edit->setValidator(event_count_validator);
	QIntValidator * iv;
	iv = new QIntValidator(OP_MIN_BUF_SIZE, OP_MAX_BUF_SIZE, buffer_size_edit);
	buffer_size_edit->setValidator(iv);
	iv = new QIntValidator(OP_MIN_NOTE_TABLE_SIZE, OP_MAX_NOTE_TABLE_SIZE, note_table_size_edit);
	note_table_size_edit->setValidator(iv);
	iv = new QIntValidator(0, INT_MAX, callgraph_depth_edit);
	callgraph_depth_edit->setValidator(iv);
	iv = new QIntValidator(0, INT_MAX, buffer_watershed_edit);
	buffer_watershed_edit->setValidator(iv);
	iv = new QIntValidator(0, OP_MAX_CPU_BUF_SIZE, cpu_buffer_size_edit);
	cpu_buffer_size_edit->setValidator(iv);

	// daemon status timer
	startTimer(5000);
	timerEvent(0);

	resize(minimumSizeHint());

	// force the pixmap re-draw
	event_selected();
}


void oprof_start::fill_events()
{
	// we need to build the event descr stuff before loading the
	// configuration because we use locate_event to get an event descr
	// from its name.
	struct list_head * pos;
	struct list_head * events = op_events(cpu_type);

	list_for_each(pos, events) {
		struct op_event * event = list_entry(pos, struct op_event, event_next);

		op_event_descr descr;

		descr.counter_mask = event->counter_mask;
		descr.val = event->val;
		if (event->unit->num) {
			descr.unit = event->unit;
		} else {
			descr.unit = 0;
		}

		descr.name = event->name;
		descr.help_str = event->desc;
		descr.min_count = event->min_count;

		for (uint ctr = 0; ctr < op_nr_counters; ++ctr) {
			uint count;

			if (!(descr.counter_mask & (1 << ctr)))
				continue;

			if (cpu_type == CPU_RTC) {
				count = 1024;
			} else {
				/* setting to cpu Hz / 2000 gives a safe value for
				 * all events, and a good one for most.
				 */
				if (cpu_speed)
					count = int(cpu_speed * 500);
				else
					count = descr.min_count * 100;
			}

			event_cfgs[descr.name].count = count;
			event_cfgs[descr.name].umask = 0;
			if (descr.unit)
				event_cfgs[descr.name].umask = descr.unit->default_mask;
			event_cfgs[descr.name].os_ring_count = 1;
			event_cfgs[descr.name].user_ring_count = 1;
		}

		v_events.push_back(descr);
	}

	events_list->header()->hide();
	events_list->setSorting(-1);

	fill_events_listbox();

	read_set_events();

	// FIXME: why this ?
	if (cpu_type == CPU_RTC)
		events_list->setCurrentItem(events_list->firstChild());

	load_config_file();
}


namespace {

/// find the first item with the given text in column 0 or return NULL
QListViewItem * findItem(QListView * view, char const * name)
{
	// Qt 2.3.1 does not have QListView::findItem()
	QListViewItem * item = view->firstChild();

	while (item && strcmp(item->text(0).latin1(), name))
		item = item->nextSibling();

	return item;
}

};


void oprof_start::setup_default_event()
{
	struct op_default_event_descr descr;
	op_default_event(cpu_type, &descr);

	event_cfgs[descr.name].umask = descr.um;
	event_cfgs[descr.name].count = descr.count;
	event_cfgs[descr.name].user_ring_count = 1;
	event_cfgs[descr.name].os_ring_count = 1;

	QListViewItem * item = findItem(events_list, descr.name);
	if (item)
		item->setSelected(true);
}


void oprof_start::read_set_events()
{
	string name = get_config_filename(".oprofile/daemonrc");

	ifstream in(name.c_str());

	if (!in) {
		setup_default_event();
		return;
	}

	string str;

	bool one_enabled = false;

	while (getline(in, str)) {
		string const val = split(str, '=');
		string const name = str;

		if (!is_prefix(name, "CHOSEN_EVENTS_"))
			continue;

		one_enabled = true;

		// CHOSEN_EVENTS_#nr=CPU_CLK_UNHALTED:10000:0:1:1
		vector<string> parts = separate_token(val, ':');

		if (parts.size() != 5 && parts.size() != 2) {
			cerr << "invalid configuration file\n";
			// FIXME
			exit(EXIT_FAILURE);
		}

		string ev_name = parts[0];
		event_cfgs[ev_name].count =
			op_lexical_cast<unsigned int>(parts[1]);

		// CPU_CLK_UNHALTED:10000 is also valid
		if (parts.size() == 5) {
			event_cfgs[ev_name].umask =
				op_lexical_cast<unsigned int>(parts[2]);
			event_cfgs[ev_name].user_ring_count =
				op_lexical_cast<unsigned int>(parts[3]);
			event_cfgs[ev_name].os_ring_count =
				op_lexical_cast<unsigned int>(parts[4]);
		} else {
			event_cfgs[ev_name].umask = 0;
			event_cfgs[ev_name].user_ring_count = 1;
			event_cfgs[ev_name].os_ring_count = 1;
		}

		QListViewItem * item = findItem(events_list, ev_name.c_str());
		if (item)
			item->setSelected(true);
	}

	// use default event if none set
	if (!one_enabled)
		setup_default_event();
}


void oprof_start::load_config_file()
{
	string name = get_config_filename(".oprofile/daemonrc");

	ifstream in(name.c_str());
	if (!in) {
		if (!check_and_create_config_dir())
			return;

		ofstream out(name.c_str());
		if (!out) {
			QMessageBox::warning(this, 0, "Unable to open configuration "
				"file ~/.oprofile/daemonrc");
		}
		return;
	}

	in >> config;
}


// user request a "normal" exit so save the config file.
void oprof_start::accept()
{
	// record the previous settings
	record_selected_event_config();

	save_config();

	QDialog::accept();
}


void oprof_start::closeEvent(QCloseEvent *)
{
	accept();
}


void oprof_start::timerEvent(QTimerEvent *)
{
	static time_t last = time(0);

	daemon_status dstat;

	flush_profiler_data_btn->setEnabled(dstat.running);
	stop_profiler_btn->setEnabled(dstat.running);
	start_profiler_btn->setEnabled(!dstat.running);
	reset_sample_files_btn->setEnabled(!dstat.running);

	if (!dstat.running) {
		daemon_label->setText("Profiler is not running.");
		return;
	}

	ostringstream ss;
	ss << "Profiler running:";

	time_t curr = time(0);
	total_nr_interrupts += dstat.nr_interrupts;

	if (curr - last)
		ss << " (" << dstat.nr_interrupts / (curr - last) << " interrupts / second, total " << total_nr_interrupts << ")";

	daemon_label->setText(ss.str().c_str());

	last = curr;
}


void oprof_start::fill_events_listbox()
{
	setUpdatesEnabled(false);

	for (vector<op_event_descr>::reverse_iterator cit = v_events.rbegin();
	     cit != v_events.rend(); ++cit) {
		new QListViewItem(events_list, cit->name.c_str());
	}

	setUpdatesEnabled(true);
	update();
}


void oprof_start::display_event(op_event_descr const & descr)
{
	setUpdatesEnabled(false);

	setup_unit_masks(descr);
	os_ring_count_cb->setEnabled(true);
	user_ring_count_cb->setEnabled(true);
	event_count_edit->setEnabled(true);

	event_setting & cfg = event_cfgs[descr.name];

	os_ring_count_cb->setChecked(cfg.os_ring_count);
	user_ring_count_cb->setChecked(cfg.user_ring_count);
	QString count_text;
	count_text.setNum(cfg.count);
	event_count_edit->setText(count_text);
	event_count_validator->setRange(descr.min_count, max_perf_count());

	setUpdatesEnabled(true);
	update();
}


bool oprof_start::is_selectable_event(QListViewItem * item)
{
	if (item->isSelected())
		return true;

	selected_events.insert(item);

	bool ret = false;
	if (alloc_selected_events())
		ret = true;

	selected_events.erase(item);

	return ret;
}


void oprof_start::draw_event_list()
{
	QListViewItem * cur;
	for (cur = events_list->firstChild(); cur; cur = cur->nextSibling()) {
		if (is_selectable_event(cur))
			cur->setPixmap(0, *green_pixmap);
		else
			cur->setPixmap(0, *red_pixmap);
	}
}


bool oprof_start::alloc_selected_events() const
{
	vector<op_event const *> events;

	set<QListViewItem *>::const_iterator it;
	for (it = selected_events.begin(); it != selected_events.end(); ++it)
		events.push_back(find_event_by_name((*it)->text(0).latin1(),0,0));

	size_t * map =
		map_event_to_counter(&events[0], events.size(), cpu_type);

	if (!map)
		return false;

	free(map);
	return true;
}

void oprof_start::event_selected()
{
	// The deal is simple: QT lack of a way to know what item was the last
	// (de)selected item so we record a set of selected items and diff
	// it in the appropriate way with the previous list of selected items.

	set<QListViewItem *> current_selection;
	QListViewItem * cur;
	for (cur = events_list->firstChild(); cur; cur = cur->nextSibling()) {
		if (cur->isSelected())
			current_selection.insert(cur);
	}

	// First remove the deselected item.
	vector<QListViewItem *> new_deselected;
	set_difference(selected_events.begin(), selected_events.end(),
		       current_selection.begin(), current_selection.end(),
		       back_inserter(new_deselected));
	vector<QListViewItem *>::const_iterator it;
	for (it = new_deselected.begin(); it != new_deselected.end(); ++it)
		selected_events.erase(*it);

	// Now try to add the newly selected item if enough HW resource exists
	vector<QListViewItem *> new_selected;
	set_difference(current_selection.begin(), current_selection.end(),
		       selected_events.begin(), selected_events.end(),
		       back_inserter(new_selected));
	for (it = new_selected.begin(); it != new_selected.end(); ++it) {
		selected_events.insert(*it);
		if (!alloc_selected_events()) {
			(*it)->setSelected(false);
			selected_events.erase(*it);
		} else {
			current_event = *it;
		}
	}

	draw_event_list();

	if (current_event)
		display_event(locate_event(current_event->text(0).latin1()));
}


void oprof_start::event_over(QListViewItem * item)
{
	op_event_descr const & descr = locate_event(item->text(0).latin1());

	string help_str = descr.help_str.c_str();
	if (!is_selectable_event(item)) {
		help_str += " conflicts with:";

		set<QListViewItem *>::const_iterator it;
		for (it = selected_events.begin(); 
		     it != selected_events.end(); ) {
			QListViewItem * temp = *it;
			selected_events.erase(it++);
			if (is_selectable_event(item)) {
				help_str += " ";
				help_str += temp->text(0).latin1();
			}
			selected_events.insert(temp);
		}
	}

	event_help_label->setText(help_str.c_str());
}


/// select the kernel image filename
void oprof_start::choose_kernel_filename()
{
	string name = kernel_filename_edit->text().latin1();
	string result = do_open_file_or_dir(name, false);

	if (!result.empty())
		kernel_filename_edit->setText(result.c_str());
}


// this record the current selected event setting in the event_cfg[] stuff.
// FIXME: need validation?
void oprof_start::record_selected_event_config()
{
	if (!current_event)
		return;

	string name(current_event->text(0).latin1());

	event_setting & cfg = event_cfgs[name];
	op_event_descr const & curr = locate_event(name);

	cfg.count = event_count_edit->text().toUInt();
	cfg.os_ring_count = os_ring_count_cb->isChecked();
	cfg.user_ring_count = user_ring_count_cb->isChecked();
	cfg.umask = get_unit_mask(curr);
}


// validate and save the configuration (The qt validator installed
// are not sufficient to do the validation)
bool oprof_start::record_config()
{
	config.kernel_filename = kernel_filename_edit->text().latin1();
	config.no_kernel = no_vmlinux->isChecked();

	uint temp = buffer_size_edit->text().toUInt();
	if (temp < OP_MIN_BUF_SIZE || temp > OP_MAX_BUF_SIZE) {
		ostringstream error;

		error << "buffer size out of range: " << temp
		      << " valid range is [" << OP_MIN_BUF_SIZE << ", "
		      << OP_MAX_BUF_SIZE << "]";

		QMessageBox::warning(this, 0, error.str().c_str());

		return false;
	}
	config.buffer_size = temp;

	temp = buffer_watershed_edit->text().toUInt();
	// watershed above half of buffer size make little sense.
	if (temp > config.buffer_size / 2) {
		ostringstream error;

		error << "buffer watershed out of range: " << temp
		      << " valid range is [0 (use default), buffer size/2] "
		      << "generally 0.25 * buffer size is fine";

		QMessageBox::warning(this, 0, error.str().c_str());

		return false;
	}
	config.buffer_watershed = temp;

	temp = cpu_buffer_size_edit->text().toUInt();
	if ((temp != 0 && temp < OP_MIN_CPU_BUF_SIZE) ||
	    temp > OP_MAX_CPU_BUF_SIZE) {
		ostringstream error;

		error << "cpu buffer size out of range: " << temp
		      << " valid range is [" << OP_MIN_CPU_BUF_SIZE << ", "
		      << OP_MAX_CPU_BUF_SIZE << "] (size = 0: use default)";

		QMessageBox::warning(this, 0, error.str().c_str());

		return false;
	}
	config.cpu_buffer_size = temp;

	temp = note_table_size_edit->text().toUInt();
	if (temp < OP_MIN_NOTE_TABLE_SIZE || temp > OP_MAX_NOTE_TABLE_SIZE) {
		ostringstream error;

		error << "note table size out of range: " << temp
		      << " valid range is [" << OP_MIN_NOTE_TABLE_SIZE << ", "
		      << OP_MAX_NOTE_TABLE_SIZE << "]";

		QMessageBox::warning(this, 0, error.str().c_str());

		return false;
	}
	config.note_table_size = temp;

	temp = callgraph_depth_edit->text().toUInt();
	if (temp > INT_MAX) {
		ostringstream error;

		error << "callgraph depth  out of range: " << temp
		      << " valid range is [" << 0 << ", "
		      << INT_MAX << "]";

		QMessageBox::warning(this, 0, error.str().c_str());

		return false;
	}
	config.callgraph_depth = temp;

	config.verbose = verbose->isChecked();
	config.separate_lib = separate_lib_cb->isChecked();
	config.separate_kernel = separate_kernel_cb->isChecked();
	config.separate_cpu = separate_cpu_cb->isChecked();
	config.separate_thread = separate_thread_cb->isChecked();

	return true;
}


void oprof_start::get_unit_mask_part(op_event_descr const & descr, uint num,
                                     bool selected, uint & mask)
{
	if (!selected)
		return;
	if  (num >= descr.unit->num)
		return;

	if (descr.unit->unit_type_mask == utm_bitmask)
		mask |= descr.unit->um[num].value;
	else
		mask = descr.unit->um[num].value;
}


// return the unit mask selected through the unit mask check box
uint oprof_start::get_unit_mask(op_event_descr const & descr)
{
	uint mask = 0;

	if (!descr.unit)
		return 0;

	// mandatory mask is transparent for user.
	if (descr.unit->unit_type_mask == utm_mandatory) {
		mask = descr.unit->default_mask;
		return mask;
	}

	get_unit_mask_part(descr, 0, check0->isChecked(), mask);
	get_unit_mask_part(descr, 1, check1->isChecked(), mask);
	get_unit_mask_part(descr, 2, check2->isChecked(), mask);
	get_unit_mask_part(descr, 3, check3->isChecked(), mask);
	get_unit_mask_part(descr, 4, check4->isChecked(), mask);
	get_unit_mask_part(descr, 5, check5->isChecked(), mask);
	get_unit_mask_part(descr, 6, check6->isChecked(), mask);
	get_unit_mask_part(descr, 7, check7->isChecked(), mask);
	get_unit_mask_part(descr, 8, check8->isChecked(), mask);
	get_unit_mask_part(descr, 9, check9->isChecked(), mask);
	get_unit_mask_part(descr, 10, check10->isChecked(), mask);
	get_unit_mask_part(descr, 11, check11->isChecked(), mask);
	get_unit_mask_part(descr, 12, check12->isChecked(), mask);
	get_unit_mask_part(descr, 13, check13->isChecked(), mask);
	get_unit_mask_part(descr, 14, check14->isChecked(), mask);
	get_unit_mask_part(descr, 15, check15->isChecked(), mask);
	return mask;
}


void oprof_start::hide_masks()
{
	check0->hide();
	check1->hide();
	check2->hide();
	check3->hide();
	check4->hide();
	check5->hide();
	check6->hide();
	check7->hide();
	check8->hide();
	check9->hide();
	check10->hide();
	check11->hide();
	check12->hide();
	check13->hide();
	check14->hide();
	check15->hide();
}


void oprof_start::setup_unit_masks(op_event_descr const & descr)
{
	op_unit_mask const * um = descr.unit;

	hide_masks();

	if (!um || um->unit_type_mask == utm_mandatory)
		return;

	event_setting & cfg = event_cfgs[descr.name];

	unit_mask_group->setExclusive(um->unit_type_mask == utm_exclusive);

	for (size_t i = 0; i < um->num ; ++i) {
		QCheckBox * check = 0;
		switch (i) {
			case 0: check = check0; break;
			case 1: check = check1; break;
			case 2: check = check2; break;
			case 3: check = check3; break;
			case 4: check = check4; break;
			case 5: check = check5; break;
			case 6: check = check6; break;
			case 7: check = check7; break;
			case 8: check = check8; break;
			case 9: check = check9; break;
			case 10: check = check10; break;
			case 11: check = check11; break;
			case 12: check = check12; break;
			case 13: check = check13; break;
			case 14: check = check14; break;
			case 15: check = check15; break;
		}
		check->setText(um->um[i].desc);
		if (um->unit_type_mask == utm_exclusive)
			check->setChecked(cfg.umask == um->um[i].value);
		else
			check->setChecked(cfg.umask & um->um[i].value);

		check->show();
	}
	unit_mask_group->setMinimumSize(unit_mask_group->sizeHint());
	setup_config_tab->setMinimumSize(setup_config_tab->sizeHint());
}


uint oprof_start::max_perf_count() const
{
	return cpu_type == CPU_RTC ? OP_MAX_RTC_COUNT : OP_MAX_PERF_COUNT;
}


void oprof_start::on_flush_profiler_data()
{
	vector<string> args;
	args.push_back("--dump");

	if (daemon_status().running)
		do_exec_command(OP_BINDIR "/opcontrol", args);
	else
		QMessageBox::warning(this, 0, "The profiler is not started.");
}


// user is happy of its setting.
void oprof_start::on_start_profiler()
{
	// save the current settings
	record_selected_event_config();

	bool one_enable = false;

	QListViewItem * cur;
	for (cur = events_list->firstChild(); cur; cur = cur->nextSibling()) {
		if (!cur->isSelected())
			continue;

		// the missing reference is intended: gcc 2.91.66 can compile
		// "op_event_descr const & descr = ..." w/o a warning
		op_event_descr const descr =
			locate_event(cur->text(0).latin1());

		event_setting & cfg = event_cfgs[cur->text(0).latin1()];

		one_enable = true;

		if (!cfg.os_ring_count && !cfg.user_ring_count) {
			QMessageBox::warning(this, 0, "You must select to "
					 "profile at least one of user binaries/kernel");
			return;
		}

		if (cfg.count < descr.min_count || 
		    cfg.count > max_perf_count()) {
			ostringstream out;

			out << "event " << descr.name << " count of range: "
			    << cfg.count << " must be in [ "
			    << descr.min_count << ", "
			    << max_perf_count()
			    << "]";

			QMessageBox::warning(this, 0, out.str().c_str());
			return;
		}

		if (descr.unit &&
		    descr.unit->unit_type_mask == utm_bitmask &&
		    cfg.umask == 0) {
			ostringstream out;

			out << "event " << descr.name << " invalid unit mask: "
			    << cfg.umask << endl;

			QMessageBox::warning(this, 0, out.str().c_str());
			return;
		}
	}

	if (one_enable == false && cpu_type != CPU_TIMER_INT) {
		QMessageBox::warning(this, 0, "No counters enabled.\n");
		return;
	}

	if (daemon_status().running) {
		// gcc 2.91 work around
		int user_choice = 0;
		user_choice =
			QMessageBox::warning(this, 0,
					     "Profiler already started:\n\n"
					     "stop and restart it?",
					     "&Restart", "&Cancel", 0, 0, 1);

		if (user_choice == 1)
			return;

		// this flush profiler data also.
		on_stop_profiler();
	}

	vector<string> args;

	// save_config validate and setup the config
	if (save_config()) {
		// now actually start
		args.push_back("--start");
		if (config.verbose)
			args.push_back("--verbose");
		do_exec_command(OP_BINDIR "/opcontrol", args);
	}

	total_nr_interrupts = 0;
	timerEvent(0);
}


bool oprof_start::save_config()
{
	if (!record_config())
		return false;

	vector<string> args;

	// saving config is done by running opcontrol --setup with appropriate
	// setted parameters so we use the same config file as command line
	// tools

	args.push_back("--setup");

	bool one_enabled = false;

	vector<string> tmpargs;
	tmpargs.push_back("--setup");

	QListViewItem * cur;
	for (cur = events_list->firstChild(); cur; cur = cur->nextSibling()) {
		if (!cur->isSelected())
			continue;

		event_setting & cfg = event_cfgs[cur->text(0).latin1()];

		op_event_descr const & descr =
			locate_event(cur->text(0).latin1());

		one_enabled = true;

		string arg = "--event=" + descr.name;
		arg += ":" + op_lexical_cast<string>(cfg.count);
		arg += ":" + op_lexical_cast<string>(cfg.umask);
		arg += ":" + op_lexical_cast<string>(cfg.os_ring_count);
		arg += ":" + op_lexical_cast<string>(cfg.user_ring_count);

		tmpargs.push_back(arg);
	}

	// only set counters if at least one is enabled
	if (one_enabled)
		args = tmpargs;

	if (config.no_kernel) {
		args.push_back("--no-vmlinux");
	} else {
		args.push_back("--vmlinux=" + config.kernel_filename);
	}

	args.push_back("--buffer-size=" +
	       op_lexical_cast<string>(config.buffer_size));

	if (op_get_interface() == OP_INTERFACE_24) {
		args.push_back("--note-table-size=" +
		       op_lexical_cast<string>(config.note_table_size));
	} else {
		args.push_back("--buffer-watershed=" +
		       op_lexical_cast<string>(config.buffer_watershed));
		args.push_back("--cpu-buffer-size=" +
		       op_lexical_cast<string>(config.cpu_buffer_size));
		if (op_file_readable("/dev/oprofile/backtrace_depth")) {
			args.push_back("--callgraph=" +
		              op_lexical_cast<string>(config.callgraph_depth));
		}
	}

	string sep = "--separate=";

	if (config.separate_lib)
		sep += "library,";
	if (config.separate_kernel)
		sep += "kernel,";
	if (config.separate_cpu)
		sep += "cpu,";
	if (config.separate_thread)
		sep += "thread,";

	if (sep == "--separate=")
		sep += "none";
	args.push_back(sep);

	// 2.95 work-around, it didn't like return !do_exec_command() 
	bool ret = !do_exec_command(OP_BINDIR "/opcontrol", args);
	return ret;
}


// flush and stop the profiler if it was started.
void oprof_start::on_stop_profiler()
{
	vector<string> args;
	args.push_back("--shutdown");

	if (daemon_status().running)
		do_exec_command(OP_BINDIR "/opcontrol", args);
	else
		QMessageBox::warning(this, 0, "The profiler is already stopped.");

	timerEvent(0);
}


void oprof_start::on_separate_kernel_cb_changed(int state)
{
	if (state == 2)
		separate_lib_cb->setChecked(true);
}

void oprof_start::on_reset_sample_files()
{
	int ret = QMessageBox::warning(this, 0, "Are you sure you want to "
	       "reset your last profile session ?", "Yes", "No", 0, 0, 1);
	if (!ret) {
		vector<string> args;
		args.push_back("--reset");
		if (!do_exec_command(OP_BINDIR "/opcontrol", args))
			// the next timer event will overwrite the message
			daemon_label->setText("Last profile session reseted.");
		else
			QMessageBox::warning(this, 0,
			     "Can't reset profiling session.");
	}
}


/// function object for matching against name
class event_name_eq {
	string name_;
public:
	explicit event_name_eq(string const & s) : name_(s) {}
	bool operator()(op_event_descr const & d) const {
		return d.name == name_;
	}
};


// helper to retrieve an event descr through its name.
op_event_descr const & oprof_start::locate_event(string const & name) const
{
	return *(find_if(v_events.begin(), v_events.end(), event_name_eq(name)));
}
