#include "tester.h"
#include "rb_tree.h"
#include <iostream>
#include <utility>
#include <cstring>

class TestElem {
public:
  int i;
  bool operator<(const TestElem& b) const
  { return i < b.i; }
};

typedef BDS_RB_Tree<TestElem> tree_type;

test_f(init(tree_type& tree))
{
  test(tree.empty() == true);
  test(tree.begin() == tree.end());
}

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
  // infix
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

  // postfix ... its easier to just make a new tree
  BDS_RB_Tree<char> char_tree;
  static const char * infix   = "abcdefghij";
  static const char * postfix = "acbegjihfd";
  static const char * prefix  = "dbacfehgij";
  int i;
  for(i = 0; i < strlen(infix); i++)
  {
    char_tree.insert(infix[i]);
  }

  i = 0;
  BDS_RB_Tree<char>::iterator_postfix it2;
  for(it2 = char_tree.begin_postfix(); it2 != char_tree.end_postfix(); ++it2)
  {
    test(*it2 == postfix[i]);
    i++;
  }
  test(i == strlen(postfix));
  i--;
  for(it2 = --(char_tree.end_postfix()); it2 != char_tree.begin_postfix(); --it2)
  {
    test(*it2 == postfix[i]);
    i--;
  }
  test(it2 == char_tree.begin_postfix());
  test(*it2 == postfix[0]);
  test(i == 0);
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

int main ()
{
  tree_type tree;
  call_test(init(tree));
  call_test(insert(tree));
  call_test(iterators(tree));
  call_test(upper_bound(tree));
  call_test(contains(tree));
  call_test(erase(tree));
  call_test(clear(tree));
  return 0;
}
