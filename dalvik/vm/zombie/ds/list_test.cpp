#include "tester.h"
#include "list.h"
#include <iostream>
#include <algorithm>

class TestElem {
public:
  int i;
  bool operator==(const TestElem& b) const
  { return i == b.i; }
};

typedef BDS_List<TestElem> list_type;

test_f(init(list_type& list))
{
  test(list.empty() == true);
  test(list.begin() == list.end());
}

#define MAX 65500
test_f(insert(list_type& list))
{
  list_type other_list = list; 
  test(list.empty() == true);

  for(int x = 0; x <= MAX; x++)
  {
    TestElem e;
    e.i = x;
    list.push_back(e);
    test((--list.end())->i == x);
  }
  test(list.empty() != true);
  test(other_list.empty() == true);
  test(list.begin() != list.end());
}

test_f(iterators(list_type& list))
{
  list_type other_list = list; 
  other_list.clear();
  int x = 0;
  for(list_type::iterator it = list.begin();
      it != list.end(); ++it)
  {
    test(it->i == x);
    x++;
  }

  x--;
  test(x == MAX);

  list_type::iterator it;
  for(it = --list.end(); it != list.begin(); --it)
  {
    test(it->i == x);
    x--;
  }
  test(it == list.begin());
  test(it->i == 0);
}

test_f(copy(list_type& list))
{
  list_type other_list = list; 
 
  list_type::iterator it2 = other_list.begin(); 
  for(list_type::iterator it = list.begin();
      it != list.end(); ++it)
  {
    test(it->i == it2->i);
    ++it2;
  }
  test(it2 == other_list.end());

  TestElem e;
  e.i = 5;
  list_type list3;
  list3.push_back(e);
  list3.push_back(e);
  list3.push_back(e);
  list3.push_back(e);
  list3.push_back(e);
  list3.push_back(e);

  other_list.clear();
  other_list = list3;

  it2 = other_list.begin(); 
  for(list_type::iterator it = list3.begin();
      it != list3.end(); ++it)
  {
    test(it->i == it2->i);
    ++it2;
  }
  test(it2 == other_list.end());

  other_list = list; 
 
  it2 = other_list.begin(); 
  for(list_type::iterator it = list.begin();
      it != list.end(); ++it)
  {
    test(it->i == it2->i);
    ++it2;
  }
  test(it2 == other_list.end());
}

test_f(erase(list_type& list))
{
  test(list.erase(list.end()) == list.end());
  for(int x = 0; x < MAX; x+=2)
  {
    TestElem e;
    e.i = x;
    test(list.erase(std::find(list.begin(), list.end(), e))->i == x+1);
  }
  TestElem e;
  e.i = MAX;
  test(list.erase(std::find(list.begin(), list.end(), e)) == list.end());
}

test_f(clear(list_type& list))
{
  list_type other_list = list; 
  other_list.clear();
  list.clear();
  test(list.empty() == true);
  test(list.begin() == list.end());
  test(--list.end() == --list.end());
  test(--list.end() == list.begin());
}

int main ()
{
  list_type list;
  call_test(init(list));
  call_test(insert(list));
  call_test(iterators(list));
  call_test(copy(list));
  call_test(erase(list));
  call_test(clear(list));
  return 0;
}
