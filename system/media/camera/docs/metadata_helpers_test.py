import unittest
from unittest import TestCase
from metadata_model import *
from metadata_helpers import *

class TestHelpers(TestCase):

  def test_enum_calculate_value_string(self):
    def compare_values_against_list(expected_list, enum):
      for (idx, val) in enumerate(expected_list):
        self.assertEquals(val,
                          enum_calculate_value_string(list(enum.values)[idx]))

    plain_enum = Enum(parent=None, values=['ON', 'OFF'])

    compare_values_against_list(['0', '1'],
                                plain_enum)

    ###
    labeled_enum = Enum(parent=None, values=['A', 'B', 'C'], ids={
      'A': '12345',
      'B': '0xC0FFEE',
      'C': '0xDEADF00D'
    })

    compare_values_against_list(['12345', '0xC0FFEE', '0xDEADF00D'],
                                labeled_enum)

    ###
    mixed_enum = Enum(parent=None,
                      values=['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H'],
                      ids={
                        'C': '0xC0FFEE',
                        'E': '123',
                        'G': '0xDEADF00D'
                      })

    expected_values = ['0', '1', '0xC0FFEE', '0xC0FFEF', '123', '124',
                       '0xDEADF00D',
                       '0xDEADF00E']

    compare_values_against_list(expected_values, mixed_enum)

  def test_enumerate_with_last(self):
    empty_list = []

    for (x, y) in enumerate_with_last(empty_list):
      self.fail("Should not return anything for empty list")

    single_value = [1]
    for (x, last) in enumerate_with_last(single_value):
      self.assertEquals(1, x)
      self.assertEquals(True, last)

    multiple_values = [4, 5, 6]
    lst = list(enumerate_with_last(multiple_values))
    self.assertListEqual([(4, False), (5, False), (6, True)], lst)

if __name__ == '__main__':
    unittest.main()
