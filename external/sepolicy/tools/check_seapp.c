#include <stdio.h>
#include <stdarg.h>
#include <ctype.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <string.h>
#include <errno.h>
#include <stdint.h>
#include <search.h>
#include <sepol/sepol.h>
#include <sepol/policydb/policydb.h>

#define TABLE_SIZE 1024
#define KVP_NUM_OF_RULES (sizeof(rules) / sizeof(key_map))
#define log_set_verbose() do { logging_verbose = 1; log_info("Enabling verbose\n"); } while(0)
#define log_error(fmt, ...) log_msg(stderr, "Error: ", fmt, ##__VA_ARGS__)
#define log_warn(fmt, ...) log_msg(stderr, "Warning: ", fmt, ##__VA_ARGS__)
#define log_info(fmt, ...) if (logging_verbose ) { log_msg(stdout, "Info: ", fmt, ##__VA_ARGS__); }

typedef struct line_order_list line_order_list;
typedef struct hash_entry hash_entry;
typedef enum key_dir key_dir;
typedef enum data_type data_type;
typedef enum rule_map_switch rule_map_switch;
typedef enum map_match map_match;
typedef struct key_map key_map;
typedef struct kvp kvp;
typedef struct rule_map rule_map;
typedef struct policy_info policy_info;

enum map_match {
	map_no_matches,
	map_input_matched,
	map_matched
};

/**
 * Whether or not the "key" from a key vaue pair is considered an
 * input or an output.
 */
enum key_dir {
	dir_in, dir_out
};

/**
 * Used as options to rule_map_free()
 *
 * This is needed to get around the fact that GNU C's hash_map doesn't copy the key, so
 * we cannot free a key when overrding rule_map's in the table.
 */
enum rule_map_switch {
	rule_map_preserve_key, /** Used to preserve the key in the rule_map, ie don't free it*/
	rule_map_destroy_key   /** Used when you need a full free of the rule_map structure*/
};

/**
 * The expected "type" of data the value in the key
 * value pair should be.
 */
enum data_type {
	dt_bool, dt_string
};

/**
 * This list is used to store a double pointer to each
 * hash table / line rule combination. This way a replacement
 * in the hash table automatically updates the list. The list
 * is also used to keep "first encountered" ordering amongst
 * the encountered key value pairs in the rules file.
 */
struct line_order_list {
	hash_entry *e;
	line_order_list *next;
};

/**
 * The workhorse of the logic. This struct maps key value pairs to
 * an associated set of meta data maintained in rule_map_new()
 */
struct key_map {
	char *name;
	key_dir dir;
	data_type type;
	char *data;
};

/**
 * Key value pair struct, this represents the raw kvp values coming
 * from the rules files.
 */
struct kvp {
	char *key;
	char *value;
};

/**
 * Rules are made up of meta data and an associated set of kvp stored in a
 * key_map array.
 */
struct rule_map {
	char *key; /** key value before hashing */
	int length; /** length of the key map */
	int lineno; /** Line number rule was encounter on */
	rule_map *next; /** next pointer used in hash table for chaining on collision */
	key_map m[]; /** key value mapping */
};

struct hash_entry {
	rule_map *r; /** The rule map to store at that location */
};

/**
 * Data associated for a policy file
 */
struct policy_info {

	char *policy_file_name; /** policy file path name */
	FILE *policy_file;      /** file handle to the policy file */
	sepol_policydb_t *db;
	sepol_policy_file_t *pf;
	sepol_handle_t *handle;
	sepol_context_t *con;
};

/** Set to !0 to enable verbose logging */
static int logging_verbose = 0;

/** set to !0 to enable strict checking of duplicate entries */
static int is_strict = 0;

/** file handle to the output file */
static FILE *output_file = NULL;

/** file handle to the input file */
static FILE *input_file = NULL;

/** output file path name */
static char *out_file_name = NULL;

/** input file path name */
static char *in_file_name = NULL;

static policy_info pol = {
	.policy_file_name = NULL,
	.policy_file = NULL,
	.db = NULL,
	.pf = NULL,
	.handle = NULL,
	.con = NULL
};

/**
 * The heart of the mapping process, this must be updated if a new key value pair is added
 * to a rule.
 */
key_map rules[] = {
                /*Inputs*/
                { .name = "isSystemServer", .type = dt_bool,   .dir = dir_in,  .data = NULL },
                { .name = "user",           .type = dt_string, .dir = dir_in,  .data = NULL },
                { .name = "seinfo",         .type = dt_string, .dir = dir_in,  .data = NULL },
                { .name = "name",           .type = dt_string, .dir = dir_in,  .data = NULL },
                { .name = "sebool",         .type = dt_string, .dir = dir_in,  .data = NULL },
                /*Outputs*/
                { .name = "domain",         .type = dt_string, .dir = dir_out, .data = NULL },
                { .name = "type",           .type = dt_string, .dir = dir_out, .data = NULL },
                { .name = "levelFromUid",   .type = dt_bool,   .dir = dir_out, .data = NULL },
                { .name = "levelFrom",      .type = dt_string,   .dir = dir_out, .data = NULL },
                { .name = "level",          .type = dt_string, .dir = dir_out, .data = NULL },
};

/**
 * Head pointer to a linked list of
 * rule map table entries, used for
 * preserving the order of entries
 * based on "first encounter"
 */
static line_order_list *list_head = NULL;

/**
 * Pointer to the tail of the list for
 * quick appends to the end of the list
 */
static line_order_list *list_tail = NULL;

/**
 * Send a logging message to a file
 * @param out
 * 	Output file to send message too
 * @param prefix
 * 	A special prefix to write to the file, such as "Error:"
 * @param fmt
 * 	The printf style formatter to use, such as "%d"
 */
static void __attribute__ ((format(printf, 3, 4)))
log_msg(FILE *out, const char *prefix, const char *fmt, ...) {

	fprintf(out, "%s", prefix);
	va_list args;
	va_start(args, fmt);
	vfprintf(out, fmt, args);
	va_end(args);
}

/**
 * Checks for a type in the policy.
 * @param db
 * 	The policy db to search
 * @param type
 * 	The type to search for
 * @return
 * 	1 if the type is found, 0 otherwise.
 * @warning
 * 	This function always returns 1 if libsepol is not linked
 * 	statically to this executable and LINK_SEPOL_STATIC is not
 * 	defined.
 */
int check_type(sepol_policydb_t *db, char *type) {

	int rc = 1;
#if defined(LINK_SEPOL_STATIC)
	policydb_t *d = (policydb_t *)db;
	hashtab_datum_t dat;
	dat = hashtab_search(d->p_types.table, type);
	rc = (dat == NULL) ? 0 : 1;
#endif
	return rc;
}

/**
 * Validates a key_map against a set of enforcement rules, this
 * function exits the application on a type that cannot be properly
 * checked
 *
 * @param m
 * 	The key map to check
 * @param lineno
 * 	The line number in the source file for the corresponding key map
 * @return
 * 	1 if valid, 0 if invalid
 */
static int key_map_validate(key_map *m, int lineno) {

	int rc = 1;
	int ret = 1;
	int resp;
	char *key = m->name;
	char *value = m->data;
	data_type type = m->type;
	sepol_bool_key_t *se_key;

	log_info("Validating %s=%s\n", key, value);

	 /* Booleans can always be checked for sanity */
	if (type == dt_bool && (!strcmp("true", value) || !strcmp("false", value))) {
		goto out;
	}
	else if (type == dt_bool) {
		log_error("Expected boolean value got: %s=%s on line: %d in file: %s\n",
				key, value, lineno, out_file_name);
		rc = 0;
		goto out;
	}

	if (!strcasecmp(key, "levelFrom") &&
	    (strcasecmp(value, "none") && strcasecmp(value, "all") &&
	     strcasecmp(value, "app") && strcasecmp(value, "user"))) {
		log_error("Unknown levelFrom=%s on line: %d in file: %s\n",
			  value, lineno, out_file_name);
		rc = 0;
		goto out;
	}

	/*
	 * If there is no policy file present,
	 * then it is not going to enforce the types against the policy so just return.
	 * User and name cannot really be checked.
	 */
	if (!pol.policy_file) {
		goto out;
	}
	else if (!strcasecmp(key, "sebool")) {

		ret = sepol_bool_key_create(pol.handle, value, &se_key);
		if (ret < 0) {
			log_error("Could not create selinux boolean key, error: %s\n",
					strerror(errno));
			rc = 0;
			goto out;
		}

		ret = sepol_bool_exists(pol.handle, pol.db, se_key, &resp);
		if (ret < 0) {
			log_error("Could not check selinux boolean, error: %s\n",
					strerror(errno));
			rc = 0;
			sepol_bool_key_free(se_key);
			goto out;
		}

		if(!resp) {
			log_error("Could not find selinux boolean \"%s\" on line: %d in file: %s\n",
					value, lineno, out_file_name);
			rc = 0;
			sepol_bool_key_free(se_key);
			goto out;
		}
		sepol_bool_key_free(se_key);
	}
	else if (!strcasecmp(key, "type") || !strcasecmp(key, "domain")) {

		if(!check_type(pol.db, value)) {
			log_error("Could not find selinux type \"%s\" on line: %d in file: %s\n", value,
					lineno, out_file_name);
			rc = 0;
		}
		goto out;
	}
	else if (!strcasecmp(key, "level")) {

		ret = sepol_mls_check(pol.handle, pol.db, value);
		if (ret < 0) {
			log_error("Could not find selinux level \"%s\", on line: %d in file: %s\n", value,
					lineno, out_file_name);
			rc = 0;
			goto out;
		}
	}

out:
	log_info("Key map validate returning: %d\n", rc);
	return rc;
}

/**
 * Prints a rule map back to a file
 * @param fp
 * 	The file handle to print too
 * @param r
 * 	The rule map to print
 */
static void rule_map_print(FILE *fp, rule_map *r) {

	int i;
	key_map *m;

	for (i = 0; i < r->length; i++) {
		m = &(r->m[i]);
		if (i < r->length - 1)
			fprintf(fp, "%s=%s ", m->name, m->data);
		else
			fprintf(fp, "%s=%s", m->name, m->data);
	}
}

/**
 * Compare two rule maps for equality
 * @param rmA
 * 	a rule map to check
 * @param rmB
 * 	a rule map to check
 * @return
 *  a map_match enum indicating the result
 */
static map_match rule_map_cmp(rule_map *rmA, rule_map *rmB) {

	int i;
	int j;
	int inputs_found = 0;
	int num_of_matched_inputs = 0;
	int input_mode = 0;
	int matches = 0;
	key_map *mA;
	key_map *mB;

	if (rmA->length != rmB->length)
		return map_no_matches;

	for (i = 0; i < rmA->length; i++) {
		mA = &(rmA->m[i]);

		for (j = 0; j < rmB->length; j++) {
			mB = &(rmB->m[j]);
			input_mode = 0;

			if (mA->type != mB->type)
				continue;

			if (strcmp(mA->name, mB->name))
				continue;

			if (strcmp(mA->data, mB->data))
				continue;

			if (mB->dir != mA->dir)
				continue;
			else if (mB->dir == dir_in) {
				input_mode = 1;
				inputs_found++;
			}

			if (input_mode) {
				log_info("Matched input lines: name=%s data=%s\n", mA->name, mA->data);
				num_of_matched_inputs++;
			}

			/* Match found, move on */
			log_info("Matched lines: name=%s data=%s", mA->name, mA->data);
			matches++;
			break;
		}
	}

	/* If they all matched*/
	if (matches == rmA->length) {
		log_info("Rule map cmp MATCH\n");
		return map_matched;
	}

	/* They didn't all match but the input's did */
	else if (num_of_matched_inputs == inputs_found) {
		log_info("Rule map cmp INPUT MATCH\n");
		return map_input_matched;
	}

	/* They didn't all match, and the inputs didn't match, ie it didn't
	 * match */
	else {
		log_info("Rule map cmp NO MATCH\n");
		return map_no_matches;
	}
}

/**
 * Frees a rule map
 * @param rm
 * 	rule map to be freed.
 */
static void rule_map_free(rule_map *rm, rule_map_switch s) {

	int i;
	int len = rm->length;
	for (i = 0; i < len; i++) {
		key_map *m = &(rm->m[i]);
		free(m->data);
	}

/* hdestroy() frees comparsion keys for non glibc */
#ifdef __GLIBC__
	if(s == rule_map_destroy_key && rm->key)
		free(rm->key);
#endif

	free(rm);
}

static void free_kvp(kvp *k) {
	free(k->key);
	free(k->value);
}

/**
 * Given a set of key value pairs, this will construct a new rule map.
 * On error this function calls exit.
 * @param keys
 * 	Keys from a rule line to map
 * @param num_of_keys
 * 	The length of the keys array
 * @param lineno
 * 	The line number the keys were extracted from
 * @return
 * 	A rule map pointer.
 */
static rule_map *rule_map_new(kvp keys[], unsigned int num_of_keys, int lineno) {

	unsigned int i = 0, j = 0;
	rule_map *new_map = NULL;
	kvp *k = NULL;
	key_map *r = NULL, *x = NULL;

	new_map = calloc(1, (num_of_keys * sizeof(key_map)) + sizeof(rule_map));
	if (!new_map)
		goto oom;

	new_map->length = num_of_keys;
	new_map->lineno = lineno;

	/* For all the keys in a rule line*/
	for (i = 0; i < num_of_keys; i++) {
		k = &(keys[i]);
		r = &(new_map->m[i]);

		for (j = 0; j < KVP_NUM_OF_RULES; j++) {
			x = &(rules[j]);

			/* Only assign key name to map name */
			if (strcasecmp(k->key, x->name)) {
				if (i == KVP_NUM_OF_RULES) {
					log_error("No match for key: %s\n", k->key);
					goto err;
				}
				continue;
			}

			memcpy(r, x, sizeof(key_map));

			/* Assign rule map value to one from file */
			r->data = strdup(k->value);
			if (!r->data)
				goto oom;

			/* Enforce type check*/
			log_info("Validating keys!\n");
			if (!key_map_validate(r, lineno)) {
				log_error("Could not validate\n");
				goto err;
			}

			/* Only build key off of inputs*/
			if (r->dir == dir_in) {
				char *tmp;
				int key_len = strlen(k->key);
				int val_len = strlen(k->value);
				int l = (new_map->key) ? strlen(new_map->key) : 0;
				l = l + key_len + val_len;
				l += 1;

				tmp = realloc(new_map->key, l);
				if (!tmp)
					goto oom;

				if (!new_map->key)
					memset(tmp, 0, l);

				new_map->key = tmp;

				strncat(new_map->key, k->key, key_len);
				strncat(new_map->key, k->value, val_len);
			}
			break;
		}
		free_kvp(k);
	}

	if (new_map->key == NULL) {
		log_error("Strange, no keys found, input file corrupt perhaps?\n");
		goto err;
	}

	return new_map;

oom:
	log_error("Out of memory!\n");
err:
	if(new_map) {
		rule_map_free(new_map, rule_map_destroy_key);
		for (; i < num_of_keys; i++) {
			k = &(keys[i]);
			free_kvp(k);
		}
	}
	exit(EXIT_FAILURE);
}

/**
 * Print the usage of the program
 */
static void usage() {
	printf(
	        "checkseapp [options] <input file>\n"
		        "Processes an seapp_contexts file specified by argument <input file> (default stdin) "
		        "and allows later declarations to override previous ones on a match.\n"
		        "Options:\n"
		        "-h - print this help message\n"
			"-s - enable strict checking of duplicates. This causes the program to exit on a duplicate entry with a non-zero exit status\n"
		        "-v - enable verbose debugging informations\n"
		        "-p policy file - specify policy file for strict checking of output selectors against the policy\n"
		        "-o output file - specify output file, default is stdout\n");
}

static void init() {

	/* If not set on stdin already */
	if(!input_file) {
		log_info("Opening input file: %s\n", in_file_name);
		input_file = fopen(in_file_name, "r");
		if (!input_file) {
			log_error("Could not open file: %s error: %s\n", in_file_name, strerror(errno));
			exit(EXIT_FAILURE);
		}
	}

	/* If not set on std out already */
	if(!output_file) {
		output_file = fopen(out_file_name, "w+");
		if (!output_file) {
			log_error("Could not open file: %s error: %s\n", out_file_name, strerror(errno));
			exit(EXIT_FAILURE);
		}
	}

	if (pol.policy_file_name) {

		log_info("Opening policy file: %s\n", pol.policy_file_name);
		pol.policy_file = fopen(pol.policy_file_name, "rb");
		if (!pol.policy_file) {
			log_error("Could not open file: %s error: %s\n",
					pol.policy_file_name, strerror(errno));
			exit(EXIT_FAILURE);
		}

		pol.handle = sepol_handle_create();
		if (!pol.handle) {
			log_error("Could not create sepolicy handle: %s\n",
					strerror(errno));
			exit(EXIT_FAILURE);
		}

		if (sepol_policy_file_create(&pol.pf) < 0) {
			log_error("Could not create sepolicy file: %s!\n",
					strerror(errno));
			exit(EXIT_FAILURE);
		}

		sepol_policy_file_set_fp(pol.pf, pol.policy_file);
		sepol_policy_file_set_handle(pol.pf, pol.handle);

		if (sepol_policydb_create(&pol.db) < 0) {
			log_error("Could not create sepolicy db: %s!\n",
					strerror(errno));
			exit(EXIT_FAILURE);
		}

		if (sepol_policydb_read(pol.db, pol.pf) < 0) {
			log_error("Could not lod policy file to db: %s!\n",
					strerror(errno));
			exit(EXIT_FAILURE);
		}
	}

	log_info("Policy file set to: %s\n", (pol.policy_file_name == NULL) ? "None" : pol.policy_file_name);
	log_info("Input file set to: %s\n", (in_file_name == NULL) ? "stdin" : in_file_name);
	log_info("Output file set to: %s\n", (out_file_name == NULL) ? "stdout" : out_file_name);

#if !defined(LINK_SEPOL_STATIC)
	log_warn("LINK_SEPOL_STATIC is not defined\n""Not checking types!");
#endif

}

/**
 * Handle parsing and setting the global flags for the command line
 * options. This function calls exit on failure.
 * @param argc
 * 	argument count
 * @param argv
 * 	argument list
 */
static void handle_options(int argc, char *argv[]) {

	int c;
	int num_of_args;

	while ((c = getopt(argc, argv, "ho:p:sv")) != -1) {
		switch (c) {
		case 'h':
			usage();
			exit(EXIT_SUCCESS);
		case 'o':
			out_file_name = optarg;
			break;
		case 'p':
			pol.policy_file_name = optarg;
			break;
		case 's':
			is_strict = 1;
			break;
		case 'v':
			log_set_verbose();
			break;
		case '?':
			if (optopt == 'o' || optopt == 'p')
				log_error("Option -%c requires an argument.\n", optopt);
			else if (isprint (optopt))
				log_error("Unknown option `-%c'.\n", optopt);
			else {
				log_error(
						"Unknown option character `\\x%x'.\n",
						optopt);
			}
		default:
			exit(EXIT_FAILURE);
		}
	}

	num_of_args = argc - optind;

	if (num_of_args > 1) {
		log_error("Too many arguments, expected 0 or 1, argument, got %d\n", num_of_args);
		usage();
		exit(EXIT_FAILURE);
	} else if (num_of_args == 1) {
		in_file_name = argv[argc - 1];
	} else {
		input_file = stdin;
		in_file_name = "stdin";
	}

	if (!out_file_name) {
		output_file = stdout;
		out_file_name = "stdout";
	}
}

/**
 * Adds a rule_map double pointer, ie the hash table pointer to the list.
 * By using a double pointer, the hash table can have a line be overridden
 * and the value is updated in the list. This function calls exit on failure.
 * @param rm
 * 	the rule_map to add.
 */
static void list_add(hash_entry *e) {

	line_order_list *node = malloc(sizeof(line_order_list));
	if (node == NULL)
		goto oom;

	node->next = NULL;
	node->e = e;

	if (list_head == NULL)
		list_head = list_tail = node;
	else {
		list_tail->next = node;
		list_tail = list_tail->next;
	}
	return;

oom:
	log_error("Out of memory!\n");
	exit(EXIT_FAILURE);
}

/**
 * Free's the rule map list, which ultimatley contains
 * all the malloc'd rule_maps.
 */
static void list_free() {
	line_order_list *cursor, *tmp;
	hash_entry *e;

	cursor = list_head;
	while (cursor) {
		e = cursor->e;
		rule_map_free(e->r, rule_map_destroy_key);
		tmp = cursor;
		cursor = cursor->next;
		free(e);
		free(tmp);
	}
}

/**
 * Adds a rule to the hash table and to the ordered list if needed.
 * @param rm
 * 	The rule map to add.
 */
static void rule_add(rule_map *rm) {

	map_match cmp;
	ENTRY e;
	ENTRY *f;
	hash_entry *entry;
	hash_entry *tmp;
	char *preserved_key;

	e.key = rm->key;

	log_info("Searching for key: %s\n", e.key);
	/* Check to see if it has already been added*/
	f = hsearch(e, FIND);

	/*
	 * Since your only hashing on a partial key, the inputs we need to handle
	 * when you want to override the outputs for a given input set, as well as
	 * checking for duplicate entries.
	 */
	if(f) {
		log_info("Existing entry found!\n");
		tmp = (hash_entry *)f->data;
		cmp = rule_map_cmp(rm, tmp->r);
		log_info("Comparing on rule map ret: %d\n", cmp);
		/* Override be freeing the old rule map and updating
		   the pointer */
		if(cmp != map_matched) {

			/*
			 * DO NOT free key pointers given to the hash map, instead
			 * free the new key. The ordering here is critical!
			 */
			preserved_key = tmp->r->key;
			rule_map_free(tmp->r, rule_map_preserve_key);
/*  hdestroy() frees comparsion keys for non glibc */
#ifdef __GLIBC__
			free(rm->key);
#endif
			rm->key = preserved_key;
			tmp->r = rm;
		}
		/* Duplicate */
		else {
			/* if is_strict is set, then don't allow duplicates */
			if(is_strict) {
				log_error("Duplicate line detected in file: %s\n"
					"Lines %d and %d match!\n",
					out_file_name, tmp->r->lineno, rm->lineno);
				rule_map_free(rm, rule_map_destroy_key);
				goto err;
			}

			/* Allow duplicates, just drop the entry*/
			log_info("Duplicate line detected in file: %s\n"
					"Lines %d and %d match!\n",
					out_file_name, tmp->r->lineno, rm->lineno);
			rule_map_free(rm, rule_map_destroy_key);
		}
	}
	/* It wasn't found, just add the rule map to the table */
	else {

		entry = malloc(sizeof(hash_entry));
		if (!entry)
			goto oom;

		entry->r = rm;
		e.data = entry;

		f = hsearch(e, ENTER);
		if(f == NULL) {
			goto oom;
		}

		/* new entries must be added to the ordered list */
		entry->r = rm;
		list_add(entry);
	}

	return;
oom:
	if (e.key)
		free(e.key);
	if (entry)
		free(entry);
	if (rm)
		free(rm);
	log_error("Out of memory in function: %s\n", __FUNCTION__);
err:
	exit(EXIT_FAILURE);
}

/**
 * Parses the seapp_contexts file and adds them to the
 * hash table and ordered list entries when it encounters them.
 * Calls exit on failure.
 */
static void parse() {

	char line_buf[BUFSIZ];
	char *token;
	unsigned lineno = 0;
	char *p, *name = NULL, *value = NULL, *saveptr;
	size_t len;
	kvp keys[KVP_NUM_OF_RULES];
	int token_cnt = 0;

	while (fgets(line_buf, sizeof line_buf - 1, input_file)) {

		lineno++;
		log_info("Got line %d\n", lineno);
		len = strlen(line_buf);
		if (line_buf[len - 1] == '\n')
			line_buf[len - 1] = '\0';
		p = line_buf;
		while (isspace(*p))
			p++;
		if (*p == '#' || *p == '\0')
			continue;

		token = strtok_r(p, " \t", &saveptr);
		if (!token)
			goto err;

		token_cnt = 0;
		memset(keys, 0, sizeof(kvp) * KVP_NUM_OF_RULES);
		while (1) {

			name = token;
			value = strchr(name, '=');
			if (!value)
				goto err;
			*value++ = 0;

			keys[token_cnt].key = strdup(name);
			if (!keys[token_cnt].key)
				goto oom;

			keys[token_cnt].value = strdup(value);
			if (!keys[token_cnt].value)
				goto oom;

			token_cnt++;

			token = strtok_r(NULL, " \t", &saveptr);
			if (!token)
				break;

		} /*End token parsing */

		rule_map *r = rule_map_new(keys, token_cnt, lineno);
		rule_add(r);

	} /* End file parsing */
	return;

err:
	log_error("reading %s, line %u, name %s, value %s\n",
			in_file_name, lineno, name, value);
	exit(EXIT_FAILURE);
oom:
	log_error("In function %s:  Out of memory\n", __FUNCTION__);
	exit(EXIT_FAILURE);
}

/**
 * Should be called after parsing to cause the printing of the rule_maps
 * stored in the ordered list, head first, which preserves the "first encountered"
 * ordering.
 */
static void output() {

	rule_map *r;
	line_order_list *cursor;
	cursor = list_head;

	while (cursor) {
		r = cursor->e->r;
		rule_map_print(output_file, r);
		cursor = cursor->next;
		fprintf(output_file, "\n");
	}
}

/**
 * This function is registered to the at exit handler and should clean up
 * the programs dynamic resources, such as memory and fd's.
 */
static void cleanup() {

	/* Only close this when it was opened by me and not the crt */
	if (out_file_name && output_file) {
		log_info("Closing file: %s\n", out_file_name);
		fclose(output_file);
	}

	/* Only close this when it was opened by me  and not the crt */
	if (in_file_name && input_file) {
		log_info("Closing file: %s\n", in_file_name);
		fclose(input_file);
	}

	if (pol.policy_file) {

		log_info("Closing file: %s\n", pol.policy_file_name);
		fclose(pol.policy_file);

		if (pol.db)
			sepol_policydb_free(pol.db);

		if (pol.pf)
			sepol_policy_file_free(pol.pf);

		if (pol.handle)
			sepol_handle_destroy(pol.handle);
	}

	log_info("Freeing list\n");
	list_free();
	hdestroy();
}

int main(int argc, char *argv[]) {
	if (!hcreate(TABLE_SIZE)) {
		log_error("Could not create hash table: %s\n", strerror(errno));
		exit(EXIT_FAILURE);
	}
	atexit(cleanup);
	handle_options(argc, argv);
	init();
	log_info("Starting to parse\n");
	parse();
	log_info("Parsing completed, generating output\n");
	output();
	log_info("Success, generated output\n");
	exit(EXIT_SUCCESS);
}
