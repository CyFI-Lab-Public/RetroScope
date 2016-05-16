/**
 * @file unique_storage.h
 * Unique storage of values
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author Philippe Elie
 * @author John Levon
 */

#ifndef UNIQUE_STORAGE_H
#define UNIQUE_STORAGE_H

#include <vector>
#include <map>
#include <stdexcept>

/**
 * Store values such that only one copy of the value
 * is ever stored.
 *
 * I is an arbitrary typename that's never
 * used.
 *
 * It is a required parameter in order to enforce
 * type-safety for a collection.
 *
 * The value type "V" must be default-constructible,
 * and this is the value returned by a stored id_value
 * where .set() is false
 */
template <typename I, typename V> class unique_storage {

public:
	unique_storage() {
		// id 0
		values.push_back(V());
	}

	virtual ~unique_storage() {}

	typedef std::vector<V> stored_values;

	/// the actual ID type
	struct id_value {
		/// id == 0 means "empty" / "undefined"
		id_value() : id(0) {}

		/// does this ID map to a non-default value ?
		bool set() const {
			return id;
		}

		bool operator<(id_value const & rhs) const {
			return id < rhs.id;
		}

		bool operator==(id_value const & rhs) const {
			return id == rhs.id;
		}

		bool operator!=(id_value const & rhs) const {
			return !(id == rhs.id);
		}

	private:
		friend class unique_storage<I, V>;

		typedef typename stored_values::size_type size_type;

		explicit id_value(size_type s) : id(s) {}

		/// actual ID value
		size_type id;
	};


	/// ensure this value is available
	id_value const create(V const & value) {
		typename id_map::value_type val(value, id_value(values.size()));
		std::pair<typename id_map::iterator, bool>
			inserted = ids.insert(val);
		if (inserted.second)
			values.push_back(value);

		return inserted.first->second;
	}


	/// return the stored value for the given ID
	V const & get(id_value const & id) const {
		// some stl lack at(), so we emulate it
		if (id.id < values.size())
			return values[id.id];

		throw std::out_of_range("unique_storage::get(): out of bounds");
	}

private:
	typedef std::map<V, id_value> id_map;

	/// the contained values
	stored_values values;

	/// map from ID to value
	id_map ids;
};

#endif /* !UNIQUE_STORAGE_H */
