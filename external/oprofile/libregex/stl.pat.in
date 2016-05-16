# format :
# $name = "regular_definition"
# "pattern" = "substitued_pattern"
# pattern can contain reference to regular definition with ${name}
# this occurence are substitued in pattern by their definition

# regular_definition containing other regular_definition refer always to a
# previously defined regular definition so they can look like recursive but are
# not. op_regex.cpp do sucessive apply of pattern whilst change occur (with a
# hard limit on number of subsitutions) so you can apply successive change to
# translate first to an intermediate simplified form then continue substitution
# in another pattern (see iosfwd section). The number of grouping regexp is
# limited, see static const size_t max_match; in op_regex.h. Note than mangled
# name produce can be illegal as I choose to output like vector<type<T>> rather
# than vector<type<T> >

# man regex is a friend, is it your ?

$integer = "\<[0-9]+"
$identifier = "\<[_a-zA-Z][_a-zA-Z0-9]*"
$typename = "${identifier}(::${identifier})*"
$typename = "${typename}(<${typename}(, ${typename})*>)*"
# adding more substitution allow more nested templated type but we run out of
# \digit which is a wall. Indeed if you add more () grouping you need to
# rename all relevant \digit in pattern which use this regular definition
# $typename = "${typename}(<${typename}(, ${typename})*>)*"
# finally add standard C type not recognized by above patterns, the way to add
# pointer is ugly but we can't add any grouping to not overrun 9 max group
# in left pattern rules side..
$typename = "(${typename}[ ]*\**|unsigned short[ ]**\**|unsigned int[ ]*\**|unsigned long[ ]*\**|unsigned char[ ]*\**|signed char[ ]*\**|long long[ ]*\**|unsigned long long[ ]*\**|long double[ ]*\**)"
$ptrdiff_t_type = "(int|long)"


# FIXME: really discussable but simplify output and the next pattern.
"\<std::" = ""
" >" = ">"
# for these two we can't match begin of word.
"\{anonymous\}::" = ""
"\(anonymous namespace\)::" = ""

# specific to gcc 2.95
"\<basic_string<char, string_char_traits<char>, __default_alloc_template<true, 0>>" = "string"
# two pass, first shrink allocator<> then less<> allowing map with user defined
# comparator
"\<(multi)?map<${typename}, ${typename}, ${typename}, allocator<\8>>" = "\1map<\2, \8, \e>"
"\<(multi)?map<${typename}, ${typename}, less<\2>>" = "\1map<\2, \8>"

"\<bitset<(${integer}), unsigned long>" = "bitset<\1>"
"\<([io]stream_iterator)<char, ${ptrdiff_t_type}>" = "\1<char>"

# common to all supported gcc version.
"\<deque<${typename}, allocator<\1>, 0>" = "deque<\1>"
"\<(stack|queue)<${typename}, deque<\2>>" = "\1<\2>"
"\<(vector|list|deque)<${typename}, allocator<\2>>" = "\1<\2>"
# strictly speaking 3rd parameters is less<ContainerType::value_type>
"\<priority_queue<${typename}, vector<\1>, less<\1>>" = "priority_queue<\1>"
# two pass, first shrink allocator<> then less<> allowing map with user defined
# comparator
"\<(multi)?set<${typename}, ${typename}, allocator<\2>>" = "\1set<\2, \8>"
"\<(multi)?set<${typename}, less<\2>>" = "\1set<\2>"

# get ride of _Rb_tree iterator typedef, these are also mapped by map/set but
# we can't distinguish a set<pair<int, int>>::iterator and a
# map<int, int>::iterator as they decay to an identical typedef so we don't try
# to be clever here.
"\<_Rb_tree_iterator<${typename}, \1 const[ ]*&, \1 const[ ]*\*>" = "_Rb_tree<\1>::const_iterator"
"\<_Rb_tree_iterator<${typename}, \1[ ]*&, \1[ ]*\*>" = "_Rb_tree<\1>::iterator"
# important special case for map/multimap iterator
"\<_Rb_tree_iterator<(pair<${typename} const, ${typename}>), \1 const[ ]*&, \1 const[ ]*\*>" = "_Rb_tree<\1>::const_iterator"
"\<_Rb_tree_iterator<(pair<${typename} const, ${typename}>), \1[ ]*&, \1[ ]*\*>" = "_Rb_tree<\1>::iterator"
# 2.95/3.2 set/multiset implementation
"\<_Rb_tree<${typename}, \1, _Identity<\1>, ${typename}, allocator<\1>>" = "_Rb_tree<\1, \1, _Identity<\1>, \7>"
"_Rb_tree<${typename}, \1, _Identity<\1>, less<\1>>" = "_Rb_tree<\1, \1, _Identity<\1>>"
# 2.95 map/multimap implementation
"\<_Rb_tree<${typename}, pair<\1 const, (${typename}( const)?)>, _Select1st<pair<\1 const, \7>>, less<\1>, allocator<\7>>" = "_Rb_tree<\1, pair<\1 const, \7>, _Select1st<pair<\1 const, \7>>, less<\1>>"
# 3.2 map/multimap implementation
"\<_Rb_tree<${typename}, pair<\1 const, ${typename}>, _Select1st<pair<\1 const, \7>>, less<\1>, allocator<pair<\1 const, \7>>>" = "_Rb_tree<\1, pair<\1 const, \7>, _Select1st<pair<\1 const, \7>>, less<\1>>"
# now we can shrink default comparator.
"\<_Rb_tree<${typename}, pair<\1 const, (${typename}( const)?)>, _Select1st<pair<\1 const, \7>>, less<\1>>" = "_Rb_tree<\1, pair<\1 const, \7>, _Select1st<pair<\1 const, \7>>>"
# get rid of _Select1st and _Identity
# FIXME: the presence od _Identity<> and _Select1st<> allowed to quickly
# differentiate a set or a map, the rule now to differentiate them is:
# second parameter to _Rb_tree* is a pair<> ==> map else set<>. Either we need
# to document this or remove _Identity and _Select1st pattern
"\<_Identity<${typename}>" = "\1"
"\<_Select1st<pair<${typename} const, ${typename}( const)?>>" = "\1 const"

"\<_List_base<${typename}, allocator<\1>>" = "_List_base<\1>"

# 2.95 templatized operator<< and >> exist only for std::string
"\<ostream & operator<<<char, string_char_traits<char>, __default_alloc_template<true, 0>>\(ostream &, string const &\)" = "ostream & operator<<(ostream &, string const &)"
"\<istream & (operator>>|getline)<char, string_char_traits<char>, __default_alloc_template<true, 0>>\(istream &, string &\)" = "istream & \1(istream &, string &)"

# 3.0 templatized operator<< and >> exist only for std::string
"\<ostream& operator<< <char, char_traits<char>, allocator<char>>\(ostream&, string const&\)" = "ostream & operator<<(ostream &, string const &)"
"\<istream& (operator>>|getline) <char, char_traits<char>, allocator<char>>\(istream&, string&\)" = "istream & \1(istream &, string &)"

# 2.95/3.2 algorithm
"\<(${typename}( const)?) \* find<\1 \*, ${typename}>\(\1 \*, \1 \*, \9 const &, ${typename}\)" = "\1 * find(\1 *, \1 *, \9 const &, \f)"

"\<(${typename}( const)?) \* find_if<\1 \*, ${typename}>\(\1 \*, \1 \*, \9, random_access_iterator_tag)" = "\1 * find_if(\1 *, \1 *, \9, random_access_iterator_tag)"

# gcc 3.2, not tested on 3.0, 3.1 but probably work.
# FIXME: there is a potential problem here with map<int const, long>
# the pair become pair<\2, \8> not pair<\2 const, \8>, who use the above,
# is it legal ?
# two pass, first shrink allocator<> then less<> allowing map with user defined
# comparator
"\<(multi)?map<${typename}, ${typename}, ${typename}, allocator<pair<\2 const, \8>>>" = "\1map<\2, \8, \e>"
# this one exist already for 2.95 the first transformation giving a common
# form for 2.95/3.2
# "\<(multi)?map<${typename}, ${typename}, less<\2>>" = "\1map<\2, \8>"

"\<bitset<\(unsigned( long)?\)(${integer})>" = "bitset<\2>"

# iterator
"\<iterator<(input|output|forward|bidirectional|random)_iterator_tag, ${typename}, (${ptrdiff_t_type}), \8\*, \8&>" = "iterator<\1_iterator_tag, \2>"
"\<([io]stream_iterator)<${typename}, char, char_traits<char>, ${ptrdiff_t_type}>" = "\1<\2>"

# __gnu_cxx::__normal_iterator are used in two context: basic_string<> and
# vector<T> we decay them to string::iterator, vector<T>::iterator
"\<__gnu_cxx::__normal_iterator<char const\*, string>" = "string::const_iterator"
"\<__gnu_cxx::__normal_iterator<char\*, string>" = "string::iterator"
"\<__gnu_cxx::__normal_iterator<wchar_t const\*, wstring>" = "wstring::const_iterator"
"\<__gnu_cxx::__normal_iterator<wchar_t\*, wstring>" = "wstring::iterator"
"\<__gnu_cxx::__normal_iterator<${typename} const\*, vector<\1>>" = "vector<\1>::const_iterator"
"\<__gnu_cxx::__normal_iterator<${typename}\*, vector<\1>>" = "vector<\1>::iterator"

# 2.95 use only _List_iterator, 3.2 use also _List_iterator_base but since
# list::iterator is a typedef to _List_iterator we don't need to deal with
# _List_iterator_base
"\<_List_iterator<${typename}, \1[ ]*&, \1[ ]*\*>" = "list<\1>::iterator"
"\<_List_iterator<${typename}, \1 const[ ]*&, \1 const[ ]*\*>" = "list<\1>::const_iterator"

# iosfwd, std::string and std::wstring
# first translate from "basic_xxx<T, char_traits<T>>" to "basic_xxx<T>"
"\<([io]streambuf_iterator|basic_(ios|streambuf|([io]|io)stream|filebuf|[io]?fstream))<${typename}, char_traits<\4>>" = "\1<\4>"
# as above translate from "basic_xxx<T, char_traits<T>, ...>" to "basic_xxx<T>"
"\<basic_(string(buf)?|[io]?stringstream)?<${typename}, char_traits<\3>, allocator<\3>>" = "basic_\1<\3>"
# now we can translate the two above for char, wchar_t to standardese typedef
$iosfwd_name = "\<basic_(string|ios|(stream|file|string)buf|(i|o|io)stream|[io]?(fstream|stringstream))"
"\<${iosfwd_name}<char>" = "\1"
"\<${iosfwd_name}<wchar_t>" = "w\1"

# streampos and wstreampos decay to the same type, they are undistingushable
# in mangled name so substitute for the most probable, not a big deal
"\<fpos<__mbstate_t>" = "streampos"

# locale
# strictly speaking this accept num_put<..., istream_iterator<...> > or
# num_get<..., ostream_iterator<...> > but this can't compile so no big deal
"\<(money|time|num)_(put|get)<${typename}, (i|o)streambuf_iterator<\3>>" = "\1_\2<\3>"
"\<moneypunct(_byname)?<${typename}, \(bool\)0>" = "moneypunct\1<\2>"

# 3.2 algorithm
"\<(vector<${typename}>::(const_)?iterator) find<\1, ${typename}>\(\1, \1, \9 const&, ${typename}\)" = "\1 find(\1, \1, \9 const&, \f)"

"\<((string|wstring)::(const_)?iterator) find<\1, ${typename}>\(\1, \1, \4 const&, ${typename}\)" = "\1 find(\1, \1, \4 const&, \a)"

"\<(vector<${typename}>::(const_)?iterator) find_if<\1, ${typename}>\(\1, \1, \9, random_access_iterator_tag\)" = "\1 find_if(\1, \1, \9, random_access_iterator_tag)"

"\<((string|wstring)::(const_)?iterator) find_if<\1, ${typename}>\(\1, \1, \4, random_access_iterator_tag\)" = "\1 find_if(\1, \1, \4, random_access_iterator_tag)"
