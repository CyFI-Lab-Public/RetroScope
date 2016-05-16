#
# Copyright (C) 2012 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

"""
A set of helpers for rendering Mako templates with a Metadata model.
"""

import metadata_model
import re
from collections import OrderedDict

_context_buf = None

def _is_sec_or_ins(x):
  return isinstance(x, metadata_model.Section) or    \
         isinstance(x, metadata_model.InnerNamespace)

##
## Metadata Helpers
##

def find_all_sections(root):
  """
  Find all descendants that are Section or InnerNamespace instances.

  Args:
    root: a Metadata instance

  Returns:
    A list of Section/InnerNamespace instances

  Remarks:
    These are known as "sections" in the generated C code.
  """
  return root.find_all(_is_sec_or_ins)

def find_parent_section(entry):
  """
  Find the closest ancestor that is either a Section or InnerNamespace.

  Args:
    entry: an Entry or Clone node

  Returns:
    An instance of Section or InnerNamespace
  """
  return entry.find_parent_first(_is_sec_or_ins)

# find uniquely named entries (w/o recursing through inner namespaces)
def find_unique_entries(node):
  """
  Find all uniquely named entries, without recursing through inner namespaces.

  Args:
    node: a Section or InnerNamespace instance

  Yields:
    A sequence of MergedEntry nodes representing an entry

  Remarks:
    This collapses multiple entries with the same fully qualified name into
    one entry (e.g. if there are multiple entries in different kinds).
  """
  if not isinstance(node, metadata_model.Section) and    \
     not isinstance(node, metadata_model.InnerNamespace):
      raise TypeError("expected node to be a Section or InnerNamespace")

  d = OrderedDict()
  # remove the 'kinds' from the path between sec and the closest entries
  # then search the immediate children of the search path
  search_path = isinstance(node, metadata_model.Section) and node.kinds \
                or [node]
  for i in search_path:
      for entry in i.entries:
          d[entry.name] = entry

  for k,v in d.iteritems():
      yield v.merge()

def path_name(node):
  """
  Calculate a period-separated string path from the root to this element,
  by joining the names of each node and excluding the Metadata/Kind nodes
  from the path.

  Args:
    node: a Node instance

  Returns:
    A string path
  """

  isa = lambda x,y: isinstance(x, y)
  fltr = lambda x: not isa(x, metadata_model.Metadata) and \
                   not isa(x, metadata_model.Kind)

  path = node.find_parents(fltr)
  path = list(path)
  path.reverse()
  path.append(node)

  return ".".join((i.name for i in path))

def has_descendants_with_enums(node):
  """
  Determine whether or not the current node is or has any descendants with an
  Enum node.

  Args:
    node: a Node instance

  Returns:
    True if it finds an Enum node in the subtree, False otherwise
  """
  return bool(node.find_first(lambda x: isinstance(x, metadata_model.Enum)))

def get_children_by_throwing_away_kind(node, member='entries'):
  """
  Get the children of this node by compressing the subtree together by removing
  the kind and then combining any children nodes with the same name together.

  Args:
    node: An instance of Section, InnerNamespace, or Kind

  Returns:
    An iterable over the combined children of the subtree of node,
    as if the Kinds never existed.

  Remarks:
    Not recursive. Call this function repeatedly on each child.
  """

  if isinstance(node, metadata_model.Section):
    # Note that this makes jump from Section to Kind,
    # skipping the Kind entirely in the tree.
    node_to_combine = node.combine_kinds_into_single_node()
  else:
    node_to_combine = node

  combined_kind = node_to_combine.combine_children_by_name()

  return (i for i in getattr(combined_kind, member))

def get_children_by_filtering_kind(section, kind_name, member='entries'):
  """
  Takes a section and yields the children of the merged kind under this section.

  Args:
    section: An instance of Section
    kind_name: A name of the kind, i.e. 'dynamic' or 'static' or 'controls'

  Returns:
    An iterable over the children of the specified merged kind.
  """

  matched_kind = next((i for i in section.merged_kinds if i.name == kind_name), None)

  if matched_kind:
    return getattr(matched_kind, member)
  else:
    return ()

##
## Filters
##

# abcDef.xyz -> ABC_DEF_XYZ
def csym(name):
  """
  Convert an entry name string into an uppercase C symbol.

  Returns:
    A string

  Example:
    csym('abcDef.xyz') == 'ABC_DEF_XYZ'
  """
  newstr = name
  newstr = "".join([i.isupper() and ("_" + i) or i for i in newstr]).upper()
  newstr = newstr.replace(".", "_")
  return newstr

# abcDef.xyz -> abc_def_xyz
def csyml(name):
  """
  Convert an entry name string into a lowercase C symbol.

  Returns:
    A string

  Example:
    csyml('abcDef.xyz') == 'abc_def_xyz'
  """
  return csym(name).lower()

# pad with spaces to make string len == size. add new line if too big
def ljust(size, indent=4):
  """
  Creates a function that given a string will pad it with spaces to make
  the string length == size. Adds a new line if the string was too big.

  Args:
    size: an integer representing how much spacing should be added
    indent: an integer representing the initial indendation level

  Returns:
    A function that takes a string and returns a string.

  Example:
    ljust(8)("hello") == 'hello   '

  Remarks:
    Deprecated. Use pad instead since it works for non-first items in a
    Mako template.
  """
  def inner(what):
    newstr = what.ljust(size)
    if len(newstr) > size:
      return what + "\n" + "".ljust(indent + size)
    else:
      return newstr
  return inner

def _find_new_line():

  if _context_buf is None:
    raise ValueError("Context buffer was not set")

  buf = _context_buf
  x = -1 # since the first read is always ''
  cur_pos = buf.tell()
  while buf.tell() > 0 and buf.read(1) != '\n':
    buf.seek(cur_pos - x)
    x = x + 1

  buf.seek(cur_pos)

  return int(x)

# Pad the string until the buffer reaches the desired column.
# If string is too long, insert a new line with 'col' spaces instead
def pad(col):
  """
  Create a function that given a string will pad it to the specified column col.
  If the string overflows the column, put the string on a new line and pad it.

  Args:
    col: an integer specifying the column number

  Returns:
    A function that given a string will produce a padded string.

  Example:
    pad(8)("hello") == 'hello   '

  Remarks:
    This keeps track of the line written by Mako so far, so it will always
    align to the column number correctly.
  """
  def inner(what):
    wut = int(col)
    current_col = _find_new_line()

    if len(what) > wut - current_col:
      return what + "\n".ljust(col)
    else:
      return what.ljust(wut - current_col)
  return inner

# int32 -> TYPE_INT32, byte -> TYPE_BYTE, etc. note that enum -> TYPE_INT32
def ctype_enum(what):
  """
  Generate a camera_metadata_type_t symbol from a type string.

  Args:
    what: a type string

  Returns:
    A string representing the camera_metadata_type_t

  Example:
    ctype_enum('int32') == 'TYPE_INT32'
    ctype_enum('int64') == 'TYPE_INT64'
    ctype_enum('float') == 'TYPE_FLOAT'

  Remarks:
    An enum is coerced to a byte since the rest of the camera_metadata
    code doesn't support enums directly yet.
  """
  return 'TYPE_%s' %(what.upper())


# Calculate a java type name from an entry with a Typedef node
def _jtypedef_type(entry):
  typedef = entry.typedef
  additional = ''

  # Hacky way to deal with arrays. Assume that if we have
  # size 'Constant x N' the Constant is part of the Typedef size.
  # So something sized just 'Constant', 'Constant1 x Constant2', etc
  # is not treated as a real java array.
  if entry.container == 'array':
    has_variable_size = False
    for size in entry.container_sizes:
      try:
        size_int = int(size)
      except ValueError:
        has_variable_size = True

    if has_variable_size:
      additional = '[]'

  try:
    name = typedef.languages['java']

    return "%s%s" %(name, additional)
  except KeyError:
    return None

# Box if primitive. Otherwise leave unboxed.
def _jtype_box(type_name):
  mapping = {
    'boolean': 'Boolean',
    'byte': 'Byte',
    'int': 'Integer',
    'float': 'Float',
    'double': 'Double',
    'long': 'Long'
  }

  return mapping.get(type_name, type_name)

def jtype_unboxed(entry):
  """
  Calculate the Java type from an entry type string, to be used whenever we
  need the regular type in Java. It's not boxed, so it can't be used as a
  generic type argument when the entry type happens to resolve to a primitive.

  Remarks:
    Since Java generics cannot be instantiated with primitives, this version
    is not applicable in that case. Use jtype_boxed instead for that.

  Returns:
    The string representing the Java type.
  """
  if not isinstance(entry, metadata_model.Entry):
    raise ValueError("Expected entry to be an instance of Entry")

  metadata_type = entry.type

  java_type = None

  if entry.typedef:
    typedef_name = _jtypedef_type(entry)
    if typedef_name:
      java_type = typedef_name # already takes into account arrays

  if not java_type:
    if not java_type and entry.enum:
      # Always map enums to Java ints, unless there's a typedef override
      base_type = 'int'

    else:
      mapping = {
        'int32': 'int',
        'int64': 'long',
        'float': 'float',
        'double': 'double',
        'byte': 'byte',
        'rational': 'Rational'
      }

      base_type = mapping[metadata_type]

    # Convert to array (enums, basic types)
    if entry.container == 'array':
      additional = '[]'
    else:
      additional = ''

    java_type = '%s%s' %(base_type, additional)

  # Now box this sucker.
  return java_type

def jtype_boxed(entry):
  """
  Calculate the Java type from an entry type string, to be used as a generic
  type argument in Java. The type is guaranteed to inherit from Object.

  It will only box when absolutely necessary, i.e. int -> Integer[], but
  int[] -> int[].

  Remarks:
    Since Java generics cannot be instantiated with primitives, this version
    will use boxed types when absolutely required.

  Returns:
    The string representing the boxed Java type.
  """
  unboxed_type = jtype_unboxed(entry)
  return _jtype_box(unboxed_type)

def _jtype_primitive(what):
  """
  Calculate the Java type from an entry type string.

  Remarks:
    Makes a special exception for Rational, since it's a primitive in terms of
    the C-library camera_metadata type system.

  Returns:
    The string representing the primitive type
  """
  mapping = {
    'int32': 'int',
    'int64': 'long',
    'float': 'float',
    'double': 'double',
    'byte': 'byte',
    'rational': 'Rational'
  }

  try:
    return mapping[what]
  except KeyError as e:
    raise ValueError("Can't map '%s' to a primitive, not supported" %what)

def jclass(entry):
  """
  Calculate the java Class reference string for an entry.

  Args:
    entry: an Entry node

  Example:
    <entry name="some_int" type="int32"/>
    <entry name="some_int_array" type="int32" container='array'/>

    jclass(some_int) == 'int.class'
    jclass(some_int_array) == 'int[].class'

  Returns:
    The ClassName.class string
  """

  return "%s.class" %jtype_unboxed(entry)

def jidentifier(what):
  """
  Convert the input string into a valid Java identifier.

  Args:
    what: any identifier string

  Returns:
    String with added underscores if necessary.
  """
  if re.match("\d", what):
    return "_%s" %what
  else:
    return what

def enum_calculate_value_string(enum_value):
  """
  Calculate the value of the enum, even if it does not have one explicitly
  defined.

  This looks back for the first enum value that has a predefined value and then
  applies addition until we get the right value, using C-enum semantics.

  Args:
    enum_value: an EnumValue node with a valid Enum parent

  Example:
    <enum>
      <value>X</value>
      <value id="5">Y</value>
      <value>Z</value>
    </enum>

    enum_calculate_value_string(X) == '0'
    enum_calculate_Value_string(Y) == '5'
    enum_calculate_value_string(Z) == '6'

  Returns:
    String that represents the enum value as an integer literal.
  """

  enum_value_siblings = list(enum_value.parent.values)
  this_index = enum_value_siblings.index(enum_value)

  def is_hex_string(instr):
    return bool(re.match('0x[a-f0-9]+$', instr, re.IGNORECASE))

  base_value = 0
  base_offset = 0
  emit_as_hex = False

  this_id = enum_value_siblings[this_index].id
  while this_index != 0 and not this_id:
    this_index -= 1
    base_offset += 1
    this_id = enum_value_siblings[this_index].id

  if this_id:
    base_value = int(this_id, 0)  # guess base
    emit_as_hex = is_hex_string(this_id)

  if emit_as_hex:
    return "0x%X" %(base_value + base_offset)
  else:
    return "%d" %(base_value + base_offset)

def enumerate_with_last(iterable):
  """
  Enumerate a sequence of iterable, while knowing if this element is the last in
  the sequence or not.

  Args:
    iterable: an Iterable of some sequence

  Yields:
    (element, bool) where the bool is True iff the element is last in the seq.
  """
  it = (i for i in iterable)

  first = next(it)  # OK: raises exception if it is empty

  second = first  # for when we have only 1 element in iterable

  try:
    while True:
      second = next(it)
      # more elements remaining.
      yield (first, False)
      first = second
  except StopIteration:
    # last element. no more elements left
    yield (second, True)

def pascal_case(what):
  """
  Convert the first letter of a string to uppercase, to make the identifier
  conform to PascalCase.

  If there are dots, remove the dots, and capitalize the letter following
  where the dot was. Letters that weren't following dots are left unchanged,
  except for the first letter of the string (which is made upper-case).

  Args:
    what: a string representing some identifier

  Returns:
    String with first letter capitalized

  Example:
    pascal_case("helloWorld") == "HelloWorld"
    pascal_case("foo") == "Foo"
    pascal_case("hello.world") = "HelloWorld"
    pascal_case("fooBar.fooBar") = "FooBarFooBar"
  """
  return "".join([s[0:1].upper() + s[1:] for s in what.split('.')])

def jkey_identifier(what):
  """
  Return a Java identifier from a property name.

  Args:
    what: a string representing a property name.

  Returns:
    Java identifier corresponding to the property name. May need to be
    prepended with the appropriate Java class name by the caller of this
    function. Note that the outer namespace is stripped from the property
    name.

  Example:
    jkey_identifier("android.lens.facing") == "LENS_FACING"
  """
  return csym(what[what.find('.') + 1:])

def jenum_value(enum_entry, enum_value):
  """
  Calculate the Java name for an integer enum value

  Args:
    enum: An enum-typed Entry node
    value: An EnumValue node for the enum

  Returns:
    String representing the Java symbol
  """

  cname = csym(enum_entry.name)
  return cname[cname.find('_') + 1:] + '_' + enum_value.name

def javadoc(text, indent = 4):
  """
  Format text block as a javadoc comment section

  Args:
    text: A multi-line string to format
    indent: baseline level of indentation for javadoc block
  Returns:
    String with:
    - Indent and * for insertion into a Javadoc comment block
    - Leading/trailing whitespace removed
    - Paragraph tags added on newlines between paragraphs

  Example:
    "This is a comment for Javadoc\n" +
    "     with multiple lines, that should be   \n" +
    "     formatted better\n" +
    "\n" +
    "    That covers multiple lines as well\n"

    transforms to
    "    * <p>\n" +
    "    * This is a comment for Javadoc\n" +
    "    * with multiple lines, that should be\n" +
    "    * formatted better\n" +
    "    * </p><p>\n" +
    "    * That covers multiple lines as well\n" +
    "    * </p>\n"
  """
  comment_prefix = " " * indent + " * ";
  comment_para = comment_prefix + "</p><p>\n";
  javatext = comment_prefix + "<p>\n";

  in_body = False # Eat empty lines at start
  first_paragraph = True
  for line in ( line.strip() for line in text.splitlines() ):
    if not line:
      in_body = False # collapse multi-blank lines into one
    else:
      # Insert para end/start after a span of blank lines except for
      # the first paragraph, which got a para start already
      if not in_body and not first_paragraph:
        javatext = javatext + comment_para

      in_body = True
      first_paragraph = False

      javatext = javatext + comment_prefix + line + "\n";

  # Close last para tag
  javatext = javatext + comment_prefix + "</p>\n";

  return javatext

def any_visible(section, kind_name, visibilities):
  """
  Determine if entries in this section have an applied visibility that's in
  the list of given visibilities.

  Args:
    section: A section of metadata
    kind_name: A name of the kind, i.e. 'dynamic' or 'static' or 'controls'
    visibilities: An iterable of visibilities to match against

  Returns:
    True if the section has any entries with any of the given visibilities. False otherwise.
  """

  for inner_namespace in get_children_by_filtering_kind(section, kind_name,
                                                        'namespaces'):
    if any(filter_visibility(inner_namespace.merged_entries, visibilities)):
      return True

  return any(filter_visibility(get_children_by_filtering_kind(section, kind_name,
                                                              'merged_entries'),
                               visibilities))


def filter_visibility(entries, visibilities):
  """
  Remove entries whose applied visibility is not in the supplied visibilities.

  Args:
    entries: An iterable of Entry nodes
    visibilities: An iterable of visibilities to filter against

  Yields:
    An iterable of Entry nodes
  """
  return (e for e in entries if e.applied_visibility in visibilities)
