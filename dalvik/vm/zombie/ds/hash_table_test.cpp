#include "tester.h"
#include "hash_table.h"
#include <iostream>
#include <utility>

class TestVal {
public:
  int i;
};

class TestKey {
public:
  int j;
  bool operator<(const TestKey& b) const
  { return j < b.j; }
};

struct KeyHasher
{
  std::size_t operator()(const TestKey& k) const
  {
    return k.j;
  }
};

typedef BDS_Hash_Table<TestKey, TestVal, KeyHasher> ht_type;
//typedef BDS_Hash_Table<int, int> ht_type;

test_f(init(ht_type& ht))
{
  test(ht.empty() == true);
  test(ht.begin() == ht.end());
}
/*
#define MAX 65500
test_f(insert(tree_type& tree))
{
  for(int x = 0; x <= MAX; x++)
  {
    TestElem e;
    e.i = x;
    std::pair<tree_type::iterator, bool> p1 = tree.insert(e);
    std::pair<tree_type::iterator, bool> p2 = tree.insert(e);

    test(p1.second == true);
    test(p2.second == false);
    test(p2.first == p1.first);

    test(p1.first->i == x);
    if(x != 0)
      test((--(p1.first))->i == x-1);
    else
      test((--(p1.first)) == tree.end());

    test((--tree.end())->i == x);
  }
  test(tree.empty() != true);
  test(tree.begin() != tree.end());
}

test_f(iterators(tree_type& tree))
{
  int x = 0;
  for(tree_type::iterator it = tree.begin();
      it != tree.end(); ++it)
  {
    test(it->i == x);
    x++;
  }

  x--;
  test(x == MAX);

  tree_type::iterator it;
  for(it = --tree.end(); it != tree.begin(); --it)
  {
    test(it->i == x);
    x--;
  }
  test(it == tree.begin());
  test(it->i == 0);
}


test_f(upper_bound(tree_type& tree))
{
  for(int x = -1; x < MAX; x++)
  {
    TestElem e;
    e.i = x;
    test(tree.upper_bound(e)->i == x+1);
  }
  TestElem e;
  e.i = MAX;
  test(tree.upper_bound(e) == tree.end());
}

test_f(contains(tree_type& tree))
{
  for(int x = -10; x < MAX + 10; x++)
  {
    TestElem e;
    e.i = x;
    if(0 <= x && x <= MAX)
      test(tree.contains(e)->i == x);
    else
      test(tree.contains(e) == tree.end());
  }
}

test_f(erase(tree_type& tree))
{
  test(tree.erase(tree.end()) == tree.end());
  for(int x = 0; x < MAX; x+=2)
  {
    TestElem e;
    e.i = x;
    test(tree.erase(tree.contains(e))->i == x+1);
  }
  TestElem e;
  e.i = MAX;
  test(tree.erase(tree.contains(e)) == tree.end());
}

test_f(clear(tree_type& tree))
{
  tree.clear();
  test(tree.empty() == true);
  test(tree.begin() == tree.end());
}
*/
int main ()
{
  ht_type ht;
  call_test(init(ht));

/*
  call_test(insert(tree));
  call_test(iterators(tree));
  call_test(upper_bound(tree));
  call_test(contains(tree));
  call_test(erase(tree));
  call_test(clear(tree));
*/
  return 0;
}
