/**
 * @file xml_utils.cpp
 * utility routines for generating XML
 *
 * @remark Copyright 2006 OProfile authors
 * @remark Read the file COPYING
 *
 * @author Dave Nomura
 */

#include <iostream>
#include <sstream>

#include "xml_utils.h"
#include "format_output.h"
#include "arrange_profiles.h"
#include "op_bfd.h"
#include "cverb.h"

using namespace std;

bool want_xml = false;

size_t nr_classes = 0;
size_t nr_cpus = 0;
size_t nr_events = 0;
sym_iterator symbols_begin;
sym_iterator symbols_end;
// handle on xml_formatter object
format_output::xml_formatter * xml_out;
xml_utils * xml_support;
size_t xml_utils::events_index = 0;
bool xml_utils::has_nonzero_masks = false;
ostringstream xml_options;



namespace {

bool has_separated_cpu_info()
{
	return classes.v[0].ptemplate.cpu != "all";
}


string get_event_num(size_t pclass)
{
	return classes.v[pclass].ptemplate.event;
}


size_t get_next_event_num_pclass(size_t start)
{
	string cur_event = get_event_num(start);
	size_t i;
	for (i = start;
		i < nr_classes && get_event_num(i) == cur_event;
		++i) ;
	return i;
}


void dump_symbol(string const & prefix, sym_iterator it, bool want_nl = true)
{
	if (it == symbols_end)
		cverb << vxml << prefix << "END";
	else
		cverb << vxml << prefix << symbol_names.name((*it)->name);
	if (want_nl)
		cverb << vxml << endl;
}


void dump_symbols(string const & prefix, sym_iterator b, sym_iterator e)
{
	if (b == (sym_iterator)0)
		return;

	for (sym_iterator it = b; it != e; ++it)
		dump_symbol(prefix, it, true);
}



void dump_classes()
{
	cverb << vxml << "<!-- classes dump" << endl;
	cverb << vxml << classes.event;
	cverb << vxml << "classes.size= " << classes.v.size() << endl;
	for (size_t i = 0; i < classes.v.size(); ++i) {
		cverb << vxml << "--- class " << i << ":" << classes.v[i].name << " ---" << endl;
		cverb << vxml << classes.v[i].ptemplate;
	}
	cverb << vxml << "-->" << endl;
}


bool has_separated_thread_info()
{
	return classes.v[0].ptemplate.tid != "all";
}


string get_cpu_num(size_t pclass)
{
	return classes.v[pclass].ptemplate.cpu;
}


};  // anonymous namespace

xml_utils::xml_utils(format_output::xml_formatter * xo,
		     symbol_collection const & s, size_t nc,
		     extra_images const & extra)
	:
	has_subclasses(false),
	bytes_index(0),
	extra_found_images(extra)
{
	xml_out = xo;
	nr_classes = nc;
	symbols_begin = s.begin();
	symbols_end = s.end();
	multiple_events = get_next_event_num_pclass(0) != nr_classes;

	if (has_separated_cpu_info()) {
		size_t cpus = 0;
		// count number of cpus
		for (size_t p = 0; p < nr_classes; ++p)  {
			size_t cpu = atoi(classes.v[p].ptemplate.cpu.c_str());
			if (cpu > cpus) cpus = cpu;
		}
		// cpus names start with 0
		nr_cpus = cpus + 1;
	}
}


string xml_utils::get_timer_setup(size_t count)
{
	return open_element(TIMER_SETUP, true) +
		init_attr(RTC_INTERRUPTS, count) + close_element();
}


string xml_utils::get_event_setup(string event, size_t count,
                                      string unit_mask)
{
	ostringstream str;

	str << open_element(EVENT_SETUP, true);
	str << init_attr(TABLE_ID, events_index++);
	str << init_attr(EVENT_NAME, event);
	if (unit_mask.size() != 0) str << init_attr(UNIT_MASK, unit_mask);
	str << init_attr(SETUP_COUNT, (size_t)count) + close_element();
	return str.str();
}


string xml_utils::get_profile_header(string cpu_name, double const speed)
{
	ostringstream str;
	string cpu_type;
	string processor;
	string::size_type slash_pos = cpu_name.find("/");

	if (slash_pos == string::npos) {
		cpu_type = cpu_name;
		processor = "";
	} else {
		cpu_type = cpu_name.substr(0, slash_pos);
		processor = cpu_name.substr(slash_pos+1);
	}

	str << init_attr(CPU_NAME, cpu_type) << endl;
	if (processor.size() > 0) 
		str << init_attr(PROCESSOR, string(processor)) << endl;
	if (nr_cpus > 1) str << init_attr(SEPARATED_CPUS, nr_cpus) << endl;
	str << init_attr(MHZ, speed) << endl;

	return str.str();
}


void xml_utils::set_nr_cpus(size_t cpus)
{
	nr_cpus = cpus;
}

void xml_utils::set_nr_events(size_t events)
{
	nr_events = events;
}

void xml_utils::set_has_nonzero_masks()
{
	has_nonzero_masks = true;
}


void xml_utils::add_option(tag_t tag, string const & value)
{
	xml_options << init_attr(tag, value);
}


void xml_utils::add_option(tag_t tag, list<string> const & value)
{
	list<string>::const_iterator begin = value.begin();
	list<string>::const_iterator end = value.end();
	list<string>::const_iterator cit = begin;
	ostringstream str;

	for (; cit != end; ++cit) {
		if (cit != begin)
			str << ",";
		str << *cit;
	}
	xml_options << init_attr(tag, str.str());
}


void xml_utils::add_option(tag_t tag, vector<string> const & value)
{
	vector<string>::const_iterator begin = value.begin();
	vector<string>::const_iterator end = value.end();
	vector<string>::const_iterator cit = begin;
	ostringstream str;

	for (; cit != end; ++cit) {
		if (cit != begin)
			str << ",";
		str << *cit;
	}
	xml_options << init_attr(tag, str.str());
}


void xml_utils::add_option(tag_t tag, bool value)
{
	xml_options << init_attr(tag, (value ? "true" : "false"));
}


void xml_utils::output_xml_header(string const & command_options,
                       string const & cpu_info, string const & events)
{
	// the integer portion indicates the schema version and should change
	// both here and in the schema file when major changes are made to
	// the schema.  changes to opreport, or minor changes to the schema
	// can be indicated by changes to the fraction part.
	string const schema_version = "3.0";

	// This is the XML version, not schema version.
	string const xml_header = "<?xml version=\"1.0\" ?>";

	cout << xml_header << endl;
	cout << open_element(PROFILE, true);
	cout << init_attr(SCHEMA_VERSION, schema_version);

	cout << cpu_info;
	cout << init_attr(TITLE, "opreport " + command_options);
	cout << close_element(NONE, true);

	cout << open_element(OPTIONS, true) << xml_options.str();
	cout << close_element();

	cout << open_element(SETUP) << events;
	cout << close_element(SETUP) << endl;
}

class subclass_info_t {
public:
	string unitmask;
	string subclass_name;
};

typedef growable_vector<subclass_info_t> subclass_array_t;
typedef growable_vector<subclass_array_t> event_subclass_t;
typedef growable_vector<event_subclass_t> cpu_subclass_t;

void xml_utils::build_subclasses(ostream & out)
{
	size_t subclasses = 0;
	string subclass_name;
	// when --separate=cpu we will have an event_subclass array for each cpu
	cpu_subclass_t cpu_subclasses;

	event_subclass_t event_subclasses;

	if (nr_cpus <= 1 && nr_events <= 1 && !has_nonzero_masks)
		return;

	out << open_element(CLASSES);
	for (size_t i = 0; i < classes.v.size(); ++i) {
		profile_class & pclass = classes.v[i];
		size_t event = atoi(pclass.ptemplate.event.c_str());

		subclass_array_t * sc_ptr;

		// select the right subclass array
		if (nr_cpus == 1) {
			sc_ptr = &event_subclasses[event];
		} else {
			size_t cpu = atoi(pclass.ptemplate.cpu.c_str());
			sc_ptr = &cpu_subclasses[cpu][event];
		}

		// search for an existing unitmask
		subclass_name = "";
		for (size_t j = 0; j < sc_ptr->size(); ++j) {
			if ((*sc_ptr)[j].unitmask == pclass.ptemplate.unitmask) {
				subclass_name = (*sc_ptr)[j].subclass_name;
				break;
			}
		}

		if (subclass_name.size() == 0) {
			ostringstream str;
			size_t new_index = sc_ptr->size();

			// no match found, create a new entry
			str << "c" << subclasses++;
			subclass_name = str.str();
			(*sc_ptr)[new_index].unitmask = pclass.ptemplate.unitmask;
			(*sc_ptr)[new_index].subclass_name = subclass_name;
			out << open_element(CLASS, true);
			out << init_attr(NAME, subclass_name);
			if (nr_cpus > 1) 
				out << init_attr(CPU_NUM, pclass.ptemplate.cpu);
			if (nr_events > 1) 
				out << init_attr(EVENT_NUM, event);
			if (has_nonzero_masks) 
				out << init_attr(EVENT_MASK, pclass.ptemplate.unitmask);
			out << close_element();
		}

		pclass.name = subclass_name;
	}
	out << close_element(CLASSES);
	has_subclasses = true;
}


string
get_counts_string(count_array_t const & counts, size_t begin, size_t end)
{
	ostringstream str;
	bool got_count = false;

	// if no cpu separation then return a simple count, omit zero counts
	if (nr_cpus == 1) {
		size_t count = counts[begin];
		if (count == 0)
			return "";
		str << count;
		return str.str();
	}

	for (size_t p = begin; p != end; ++p) {
		size_t count = counts[p];
		if (p != begin) str << ",";
		if (count != 0) {
			got_count = true;
			str << count;
		}
	}
	return got_count ? str.str() : "";
}


void
xml_utils::output_symbol_bytes(ostream & out, symbol_entry const * symb,
			       size_t sym_id, op_bfd const & abfd)
{
	size_t size = symb->size;
	scoped_array<unsigned char> contents(new unsigned char[size]);
	if (abfd.get_symbol_contents(symb->sym_index, contents.get())) {
		string const name = symbol_names.name(symb->name);
		out << open_element(BYTES, true) << init_attr(TABLE_ID, sym_id);
		out << close_element(NONE, true);
		for (size_t i = 0; i < size; ++i) {
			char hex_map[] = "0123456789ABCDEF";
			char hex[2];
			hex[0] = hex_map[(contents[i] >> 4) & 0xf];
			hex[1] = hex_map[contents[i] & 0xf];
			out << hex[0] << hex[1];
		}
		out << close_element(BYTES);
	}
}


bool
xml_utils::output_summary_data(ostream & out, count_array_t const & summary, size_t pclass)
{
	size_t const count = summary[pclass];

	if (count == 0)
		return false;

	out << open_element(COUNT, has_subclasses);
	if (has_subclasses) {
		out << init_attr(CLASS, classes.v[pclass].name);
		out << close_element(NONE, true);
	}
	out << count;
	out << close_element(COUNT);
	return true;
}

class module_info {
public:
	module_info()
		{ lo = hi = 0; name = ""; begin = end = (sym_iterator)0;}
	void dump();
	void build_module(string const & n, sym_iterator it,
	                  size_t l, size_t h);
	string get_name() { return name; }
	void set_lo(size_t l) { lo = l; }
	void set_hi(size_t h) { hi = h; }
	count_array_t const & get_summary() { return summary; }
	void set_begin(sym_iterator b);
	void set_end(sym_iterator e);
	void add_to_summary(count_array_t const & counts);
	void output(ostream & out);
	bool is_closed(string const & n);
protected:
	void output_summary(ostream & out);
	void output_symbols(ostream & out, bool is_module);

	string name;
	sym_iterator begin;
	sym_iterator end;

	// summary sample data
	count_array_t summary;

	// range of profile classes approprate for this module
	size_t lo;
	size_t hi;
};

class thread_info : public module_info {
public:
	thread_info() { nr_modules = 0; }

	void build_thread(string const & tid, size_t l, size_t h);
	bool add_modules(string const & module, sym_iterator it);
	void add_module_symbol(string const & n, sym_iterator it);
	void summarize();
	void set_end(sym_iterator end);
	string const get_tid() { return thread_id; }
	void output(ostream & out);
	void dump();
private:
	// indices into the classes array applicable to this process
	size_t nr_modules;
	string thread_id;
	growable_vector<module_info> my_modules;
};

class process_info : public module_info {
public:
	process_info() { nr_threads = 0; }
	void build_process(string const & pid, size_t l, size_t h);
	void add_thread(string const & tid, size_t l, size_t h);
	void add_modules(string const & module,
		string const & app_name, sym_iterator it);
	void summarize();
	void set_end(sym_iterator end);
	void output(ostream & out);
	void dump();
private:
	size_t nr_threads;
	string process_id;
	growable_vector<thread_info> my_threads;

};
class process_root_info {
public:
	process_root_info() { nr_processes = 0; }
	process_info * add_process(string const & pid, size_t lo, size_t hi);
	void add_modules(string const & module, string const & app_name,
		sym_iterator it);
	void summarize();
	void summarize_processes(extra_images const & extra_found_images);
	void set_process_end();
	void output_process_symbols(ostream & out);
	void dump_processes();
private:
	size_t nr_processes;

	growable_vector<process_info> processes;
};

class binary_info : public module_info {
public:
	binary_info() { nr_modules = 0; }
	void output(ostream & out);
	binary_info * build_binary(string const & n);
	void add_module_symbol(string const & module, string const & app,
		sym_iterator it);
	void close_binary(sym_iterator it);
	void dump();
private:
	size_t nr_modules;

	growable_vector<module_info> my_modules;
};


class binary_root_info {
public:
	binary_root_info() { nr_binaries = 0; }
	binary_info * add_binary(string const & n, sym_iterator it);
	void summarize_binaries(extra_images const & extra_found_images);
	void output_binary_symbols(ostream & out);
	void dump_binaries();
private:
	size_t nr_binaries;

	growable_vector<binary_info> binaries;
};

static process_root_info processes_root;
static binary_root_info binaries_root;


void module_info::
build_module(string const & n, sym_iterator it, size_t l, size_t h)
{
	name = n;
	begin = it;
	lo = l;
	hi = h;
}


void module_info::add_to_summary(count_array_t const & counts)
{
	for (size_t pclass = lo ; pclass <= hi; ++pclass)
		summary[pclass] += counts[pclass];
}


void module_info::set_begin(sym_iterator b)
{
	if (begin == (sym_iterator)0)
		begin = b;
}


void module_info::set_end(sym_iterator e)
{
	if (end == (sym_iterator)0)
		end = e;
}


bool module_info::is_closed(string const & n)
{
	return (name == n) && end != (sym_iterator)0;
}


void module_info::dump()
{
	cverb << vxml << "	module:class(" << lo << "," << hi << ")=";
	cverb << vxml << name << endl;
	dump_symbols("		", begin, end);
}


void module_info::output(ostream & out)
{
	out << open_element(MODULE, true);
	out << init_attr(NAME, name) << close_element(NONE, true);
	output_summary(out);
	output_symbols(out, true);
	out << close_element(MODULE);
}


void module_info::output_summary(ostream & out)
{
	for (size_t p = lo; p <= hi; ++p)
		(void)xml_support->output_summary_data(out, summary, p);
}


void module_info::output_symbols(ostream & out, bool is_module)
{
	if (begin == (sym_iterator)0)
		return;

	for (sym_iterator it = begin; it != end; ++it)
		xml_out->output_symbol(out, *it, lo, hi, is_module);
}


void binary_info::close_binary(sym_iterator it)
{
	set_end(it);
	if (nr_modules > 0) {
		module_info & m = my_modules[nr_modules-1];
		m.set_end(it);
	}
}


void binary_info::dump()
{
	cverb << vxml << "app_name=" << name << endl;
	if (begin != (sym_iterator)0)
		dump_symbols("	", begin, end);

	for (size_t i = 0; i < nr_modules; ++i)
		my_modules[i].dump();
}


void binary_info::
add_module_symbol(string const & module, string const & app,
	sym_iterator it)
{
	size_t m = nr_modules;

	if (module == app) {
		// set begin symbol for binary if not set
		set_begin(it);

		if (m > 0) {
			// close out current module
			module_info & mod = my_modules[m-1];
			mod.set_end(it);
		}

		// add symbol count to binary count
		add_to_summary((*it)->sample.counts);
		return;
	}

	string current_module_name = (m == 0 ? "" : my_modules[m-1].get_name());
	if (module != current_module_name) {
		// we have a module distinct from it's binary: --separate=lib
		// and this is the first symbol for this module
		if (m != 0) {
			// close out current module
			module_info & mod = my_modules[m-1];
			mod.set_end(it);
			add_to_summary(mod.get_summary());
		}

		// mark end of enclosing binary symbols if there have been any
		// NOTE: it is possible for the binary's symbols to follow its
		// module symbols
		if (begin != (sym_iterator)0 && end == (sym_iterator)0)
			set_end(it);

		// build the new module
		nr_modules++;
		my_modules[m].build_module(module, it, 0, nr_classes-1);
	}

	// propagate this symbols counts to the module
	my_modules[nr_modules-1].add_to_summary((*it)->sample.counts);
}


void binary_root_info::
summarize_binaries(extra_images const & extra_found_images)
{
	binary_info * current_binary = 0;
	string current_binary_name = "";

	for (sym_iterator it = symbols_begin ; it != symbols_end; ++it) {
		string binary = get_image_name((*it)->app_name,
			image_name_storage::int_filename, extra_found_images);
		string module = get_image_name((*it)->image_name,
			image_name_storage::int_filename, extra_found_images);

		if (binary != current_binary_name) {
			current_binary = binaries_root.add_binary(binary, it);
			current_binary_name = binary;
		}

		current_binary->add_module_symbol(module, binary, it);
	}

	// close out last binary and module
	current_binary->close_binary(symbols_end);
}


process_info *
process_root_info::add_process(string const & pid, size_t lo, size_t hi)
{
	processes[nr_processes].build_process(pid, lo, hi);
	return &processes[nr_processes++];
}


void process_root_info::
add_modules(string const & module, string const & app_name,
	sym_iterator it)
{
	for (size_t p = 0; p < nr_processes; ++p)
		processes[p].add_modules(module, app_name, it);
}



void process_root_info::summarize()
{
	for (size_t p = 0; p < nr_processes; ++p)
		processes[p].summarize();
}


void process_root_info::
summarize_processes(extra_images const & extra_found_images)
{
	// add modules to the appropriate threads in the process hierarchy
	for (sym_iterator it = symbols_begin ; it != symbols_end; ++it) {
		string binary = get_image_name((*it)->app_name, 
			image_name_storage::int_filename, extra_found_images);
		string module = get_image_name((*it)->image_name,
			image_name_storage::int_filename, extra_found_images);

		processes_root.add_modules(module, binary, it);
	}

	// set end symbol boundary for all modules in all threads
	processes_root.set_process_end();

	// propagate summaries to process/thread
	processes_root.summarize();
}


void process_root_info::set_process_end()
{
	for (size_t p = 0; p < nr_processes; ++p)
		processes[p].set_end(symbols_end);
}

void process_root_info::output_process_symbols(ostream & out)
{
	for (size_t p = 0; p < nr_processes; ++p)
		processes[p].output(out);
}


void process_root_info::dump_processes()
{
	cverb << vxml << "<!-- processes_dump:" << endl;
	for (size_t p = 0; p < nr_processes; ++p)
		processes[p].dump();
	cverb << vxml << "end processes_dump -->" << endl;
}

binary_info *
binary_info::build_binary(string const & n)
{
	name = n;
	lo = 0;
	hi = nr_classes-1;
	return this;
}


void binary_info::output(ostream & out)
{
	out << open_element(BINARY, true);
	out << init_attr(NAME, name) << close_element(NONE, true);

	output_summary(out);
	output_symbols(out, false);
	for (size_t a = 0; a < nr_modules; ++a)
		my_modules[a].output(out);

	out << close_element(BINARY);
}


binary_info *
binary_root_info::add_binary(string const & n, sym_iterator it)
{
	size_t a = nr_binaries++;

	// close out previous binary and module
	if (a > 0) binaries[a-1].close_binary(it);
	return binaries[a].build_binary(n);
}


void binary_root_info::output_binary_symbols(ostream & out)
{
	for (size_t a = 0; a < nr_binaries; ++a)
		binaries[a].output(out);
}


void binary_root_info::dump_binaries()
{
	cverb << vxml << "<!-- binaries_dump:" << endl;
	for (size_t p = 0; p < nr_binaries; ++p)
		binaries[p].dump();
	cverb << vxml << "end processes_dump -->" << endl;
}


void process_info::build_process(string const & pid, size_t l, size_t h)
{
	process_id = pid;
	lo = l;
	hi = h;
}


void process_info::add_thread(string const & tid, size_t l, size_t h)
{
	my_threads[nr_threads++].build_thread(tid, l, h);
}


void process_info::add_modules(string const & module,
	string const & app_name, sym_iterator it)
{
	bool added = false;
	for (size_t t = 0; t < nr_threads; ++t)
		added |= my_threads[t].add_modules(module, it);
	if (added && name.size() == 0) name = app_name;
}


void process_info::summarize()
{
	for (size_t t = 0; t < nr_threads; ++t) {
		thread_info & thr = my_threads[t];
		thr.summarize();
		add_to_summary(thr.get_summary());
	}
}


void thread_info::build_thread(string const & tid, size_t l, size_t h)
{
	thread_id = tid;
	lo = l;
	hi = h;
}


void thread_info::summarize()
{
	for (size_t m = 0; m < nr_modules; ++m)
		add_to_summary(my_modules[m].get_summary());
}


void thread_info::set_end(sym_iterator end)
{
	for (size_t m = 0; m < nr_modules; ++m)
		my_modules[m].set_end(end);
}


void thread_info::add_module_symbol(string const & n, sym_iterator it)
{
	module_info & m = my_modules[nr_modules++];
	m.build_module(n, it, lo, hi);
	m.add_to_summary((*it)->sample.counts);
}

void thread_info::output(ostream & out)
{
	ostringstream thread_summary;
	ostringstream modules_output;

	output_summary(thread_summary);

	for (size_t m = 0; m < nr_modules; ++m)
		my_modules[m].output(modules_output);

	// ignore threads with no sample data
	if (modules_output.str().size() == 0 && thread_summary.str().size() == 0)
		return;

	out << open_element(THREAD, true);
	out << init_attr(THREAD_ID, thread_id) << close_element(NONE, true);
	out << thread_summary.str();
	out << modules_output.str();
	out << close_element(THREAD);
}


bool thread_info::add_modules(string const & module, sym_iterator it)
{
	string old_name =
		(nr_modules == 0 ? "" : my_modules[nr_modules-1].get_name());
	if (nr_modules > 0 && old_name != module) {
		module_info & m = my_modules[nr_modules-1];
		// close out previous module if it hasn't already been closed out
		if (!m.is_closed(old_name))
			m.set_end(it);
	}

	// add a new module for this symbol if it has a non-zero count
	if (nr_modules == 0 || module != old_name) {
		if (has_sample_counts((*it)->sample.counts, lo, hi)) {
			add_module_symbol(module, it);
			return true;
		}
	} else {
		// propagate symbols count to module
		my_modules[nr_modules-1].add_to_summary((*it)->sample.counts);
	}
	return false;
}


void thread_info::dump()
{
	cverb << vxml << "tid=" << thread_id << endl;
	for (size_t i = 0; i < nr_modules; ++i)
		my_modules[i].dump();
}


void process_info::set_end(sym_iterator end)
{
	for (size_t t = 0; t < nr_threads; ++t)
		my_threads[t].set_end(end);
}


void process_info::output(ostream & out)
{
	ostringstream process_summary;
	ostringstream thread_output;

	output_summary(process_summary);

	for (size_t t = 0; t < nr_threads; ++t)
		my_threads[t].output(thread_output);

	// ignore processes with no sample data
	if (thread_output.str().size() == 0 && process_summary.str().size() == 0)
		return;

	out << open_element(PROCESS, true);
	out << init_attr(PROC_ID, process_id);
	out << init_attr(NAME, name) << close_element(NONE, true);
	out << process_summary.str();
	out << thread_output.str();
	out << close_element(PROCESS);
}


void process_info::dump()
{
	cverb << vxml << "pid=" << process_id << " app=" << name << endl;
	for (size_t i = 0; i < nr_threads; ++i)
		my_threads[i].dump();
}

size_t get_next_tgid_pclass(size_t start)
{
	string cur_tgid = classes.v[start].ptemplate.tgid;
	size_t i = start;
	for (i = start;
		i < nr_classes && classes.v[i].ptemplate.tgid == cur_tgid;
		++i) ;
	return i;
}


size_t get_next_tid_pclass(size_t start)
{
	string cur_tid = classes.v[start].ptemplate.tid;
	size_t i;
	for (i = start;
		i < nr_classes && classes.v[i].ptemplate.tid == cur_tid;
		++i) ;
	return i;
}


// build the process/thread/module hierarchy that will allow us later
// to collect the summary sample data at each level and then
// traverse the hierarchy to intersperse the summary data for the
// symbols
void build_process_tree()
{
	size_t tgid = 0;
	size_t tid = 0;

	// build the structure representing the process/thread/module hierarchy
	// for holding the summary data associated with each level and to be
	// traversed when outputting the body of the XML
	do {
		size_t next_tgid = get_next_tgid_pclass(tgid);
		string const tgid_str = classes.v[tgid].ptemplate.tgid;

		process_info * p = processes_root.add_process(tgid_str, tgid, next_tgid-1);

		do {
			size_t next_tid = get_next_tid_pclass(tid);

			// build array of threads associated with this process
			p->add_thread(classes.v[tid].ptemplate.tid, tid, next_tid-1);
			tid = next_tid;
		} while (tid != next_tgid);
		tgid = next_tgid;
	} while (tgid != nr_classes);
}

void xml_utils::output_program_structure(ostream & out)
{

	if (cverb << vxml)
		dump_classes();

	if (has_separated_thread_info()) {
		build_process_tree();
		processes_root.summarize_processes(extra_found_images);
		if (cverb << vxml)
			processes_root.dump_processes();
		processes_root.output_process_symbols(out);
	} else {
		binaries_root.summarize_binaries(extra_found_images);
		if (cverb << vxml)
			binaries_root.dump_binaries();
		binaries_root.output_binary_symbols(out);
	}
}
