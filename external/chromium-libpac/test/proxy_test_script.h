// This file is auto generated using the following command.
// Do not modify.
// 	./jstocstring.pl js-unittest proxy_test_script.h
#ifndef PROXY_TEST_SCRIPT_H_
#define PROXY_TEST_SCRIPT_H_

#define BINDING_FROM_GLOBAL_JS \
  "// Calls a bindings outside of FindProxyForURL(). This causes the code to\n" \
  "// get exercised during initialization.\n" \
  "\n" \
  "var x = myIpAddress();\n" \
  "\n" \
  "function FindProxyForURL(url, host) {\n" \
  "  return \"PROXY \" + x + \":80\";\n" \
  "}\n" \

#define BINDINGS_JS \
  "// Try calling the browser-side bound functions with varying (invalid)\n" \
  "// inputs. There is no notion of \"success\" for this test, other than\n" \
  "// verifying the correct C++ bindings were reached with expected values.\n" \
  "\n" \
  "function MyObject() {\n" \
  "  this.x = \"3\";\n" \
  "}\n" \
  "\n" \
  "MyObject.prototype.toString = function() {\n" \
  "  throw \"exception from calling toString()\";\n" \
  "}\n" \
  "\n" \
  "function expectEquals(expectation, actual) {\n" \
  "  if (!(expectation === actual)) {\n" \
  "    throw \"FAIL: expected: \" + expectation + \", actual: \" + actual;\n" \
  "  }\n" \
  "}\n" \
  "\n" \
  "function FindProxyForURL(url, host) {\n" \
  "  // Call dnsResolve with some wonky arguments.\n" \
  "  // Those expected to fail (because we have passed a non-string parameter)\n" \
  "  // will return |null|, whereas those that have called through to the C++\n" \
  "  // bindings will return '127.0.0.1'.\n" \
  "  expectEquals(null, dnsResolve());\n" \
  "  expectEquals(null, dnsResolve(null));\n" \
  "  expectEquals(null, dnsResolve(undefined));\n" \
  "  expectEquals('127.0.0.1', dnsResolve(\"\"));\n" \
  "  expectEquals(null, dnsResolve({foo: 'bar'}));\n" \
  "  expectEquals(null, dnsResolve(fn));\n" \
  "  expectEquals(null, dnsResolve(['3']));\n" \
  "  expectEquals('127.0.0.1', dnsResolve(\"arg1\", \"arg2\", \"arg3\", \"arg4\"));\n" \
  "\n" \
  "  // Call alert with some wonky arguments.\n" \
  "  alert();\n" \
  "  alert(null);\n" \
  "  alert(undefined);\n" \
  "  alert({foo:'bar'});\n" \
  "\n" \
  "  // This should throw an exception when we toString() the argument\n" \
  "  // to alert in the bindings.\n" \
  "  try {\n" \
  "    alert(new MyObject());\n" \
  "  } catch (e) {\n" \
  "    alert(e);\n" \
  "  }\n" \
  "\n" \
  "  // Call myIpAddress() with wonky arguments\n" \
  "  myIpAddress(null);\n" \
  "  myIpAddress(null, null);\n" \
  "\n" \
  "  // Call myIpAddressEx() correctly (no arguments).\n" \
  "  myIpAddressEx();\n" \
  "\n" \
  "  // Call dnsResolveEx() (note that isResolvableEx() implicity calls it.)\n" \
  "  isResolvableEx(\"is_resolvable\");\n" \
  "  dnsResolveEx(\"foobar\");\n" \
  "\n" \
  "  return \"DIRECT\";\n" \
  "}\n" \
  "\n" \
  "function fn() {}\n" \
  "\n" \

#define DIRECT_JS \
  "function FindProxyForURL(url, host) {\n" \
  "  return \"DIRECT\";\n" \
  "}\n" \
  "\n" \

#define DNS_FAIL_JS \
  "// This script should be run in an environment where all DNS resolution are\n" \
  "// failing. It tests that functions return the expected values.\n" \
  "//\n" \
  "// Returns \"PROXY success:80\" on success.\n" \
  "function FindProxyForURL(url, host) {\n" \
  "  try {\n" \
  "    expectEq(\"127.0.0.1\", myIpAddress());\n" \
  "    expectEq(\"\", myIpAddressEx());\n" \
  "\n" \
  "    expectEq(null, dnsResolve(\"not-found\"));\n" \
  "    expectEq(\"\", dnsResolveEx(\"not-found\"));\n" \
  "\n" \
  "    expectEq(false, isResolvable(\"not-found\"));\n" \
  "    expectEq(false, isResolvableEx(\"not-found\"));\n" \
  "\n" \
  "    return \"PROXY success:80\";\n" \
  "  } catch(e) {\n" \
  "    alert(e);\n" \
  "    return \"PROXY failed:80\";\n" \
  "  }\n" \
  "}\n" \
  "\n" \
  "function expectEq(expected, actual) {\n" \
  "  if (expected != actual)\n" \
  "    throw \"Expected \" + expected + \" but was \" + actual;\n" \
  "}\n" \
  "\n" \

#define ENDS_WITH_COMMENT_JS \
  "function FindProxyForURL(url, host) {\n" \
  "  return \"PROXY success:80\";\n" \
  "}\n" \
  "\n" \
  "// We end the script with a comment (and no trailing newline).\n" \
  "// This used to cause problems, because internally ProxyResolverV8\n" \
  "// would append some functions to the script; the first line of\n" \
  "// those extra functions was being considered part of the comment.\n" \

#define ENDS_WITH_STATEMENT_NO_SEMICOLON_JS \
  "// Ends with a statement, and no terminal newline.\n" \
  "function FindProxyForURL(url, host) { return \"PROXY success:\" + x; }\n" \
  "x = 3\n" \

#define INTERNATIONAL_DOMAIN_NAMES_JS \
  "// Try resolving hostnames containing non-ASCII characters.\n" \
  "\n" \
  "function FindProxyForURL(url, host) {\n" \
  "  // This international hostname has a non-ASCII character. It is represented\n" \
  "  // in punycode as 'xn--bcher-kva.ch'\n" \
  "  var idn = 'B\u00fccher.ch';\n" \
  "\n" \
  "  // We disregard the actual return value -- all we care about is that on\n" \
  "  // the C++ end the bindings were passed the punycode equivalent of this\n" \
  "  // unicode hostname.\n" \
  "  dnsResolve(idn);\n" \
  "  dnsResolveEx(idn);\n" \
  "\n" \
  "  return \"DIRECT\";\n" \
  "}\n" \
  "\n" \

#define MISSING_CLOSE_BRACE_JS \
  "// This PAC script is invalid, because there is a missing close brace\n" \
  "// on the function FindProxyForURL().\n" \
  "\n" \
  "function FindProxyForURL(url, host) {\n" \
  "  return \"DIRECT\";\n" \
  "\n" \

#define NO_ENTRYPOINT_JS \
  "var x = \"This is an invalid PAC script because it lacks a \" +\n" \
  "        \"FindProxyForURL() function\";\n" \

#define PAC_LIBRARY_UNITTEST_JS \
  "// This should output \"PROXY success:80\" if all the tests pass.\n" \
  "// Otherwise it will output \"PROXY failure:<num-failures>\".\n" \
  "//\n" \
  "// This aims to unit-test the PAC library functions, which are\n" \
  "// exposed in the PAC's execution environment. (Namely, dnsDomainLevels,\n" \
  "// timeRange, etc.)\n" \
  "\n" \
  "function FindProxyForURL(url, host) {\n" \
  "  var numTestsFailed = 0;\n" \
  "\n" \
  "  // Run all the tests\n" \
  "  for (var test in Tests) {\n" \
  "    var t = new TestContext(test);\n" \
  "\n" \
  "    // Run the test.\n" \
  "    Tests[test](t);\n" \
  "\n" \
  "    if (t.failed()) {\n" \
  "      numTestsFailed++;\n" \
  "    }\n" \
  "  }\n" \
  "\n" \
  "  if (numTestsFailed == 0) {\n" \
  "    return \"PROXY success:80\";\n" \
  "  }\n" \
  "  return \"PROXY failure:\" + numTestsFailed;\n" \
  "}\n" \
  "\n" \
  "// --------------------------\n" \
  "// Tests\n" \
  "// --------------------------\n" \
  "\n" \
  "var Tests = {};\n" \
  "\n" \
  "Tests.testDnsDomainIs = function(t) {\n" \
  "  t.expectTrue(dnsDomainIs(\"google.com\", \".com\"));\n" \
  "  t.expectTrue(dnsDomainIs(\"google.co.uk\", \".co.uk\"));\n" \
  "  t.expectFalse(dnsDomainIs(\"google.com\", \".co.uk\"));\n" \
  "  t.expectFalse(dnsDomainIs(\"www.adobe.com\", \".ad\"));\n" \
  "};\n" \
  "\n" \
  "Tests.testDnsDomainLevels = function(t) {\n" \
  "  t.expectEquals(0, dnsDomainLevels(\"www\"));\n" \
  "  t.expectEquals(2, dnsDomainLevels(\"www.google.com\"));\n" \
  "  t.expectEquals(3, dnsDomainLevels(\"192.168.1.1\"));\n" \
  "};\n" \
  "\n" \
  "Tests.testIsInNet = function(t) {\n" \
  "  t.expectTrue(\n" \
  "      isInNet(\"192.89.132.25\", \"192.89.132.25\", \"255.255.255.255\"));\n" \
  "  t.expectFalse(\n" \
  "      isInNet(\"193.89.132.25\", \"192.89.132.25\", \"255.255.255.255\"));\n" \
  "\n" \
  "  t.expectTrue(isInNet(\"192.89.132.25\", \"192.89.0.0\", \"255.255.0.0\"));\n" \
  "  t.expectFalse(isInNet(\"193.89.132.25\", \"192.89.0.0\", \"255.255.0.0\"));\n" \
  "\n" \
  "  t.expectFalse(\n" \
  "      isInNet(\"192.89.132.a\", \"192.89.0.0\", \"255.255.0.0\"));\n" \
  "};\n" \
  "\n" \
  "Tests.testIsPlainHostName = function(t) {\n" \
  "  t.expectTrue(isPlainHostName(\"google\"));\n" \
  "  t.expectFalse(isPlainHostName(\"google.com\"));\n" \
  "};\n" \
  "\n" \
  "Tests.testLocalHostOrDomainIs = function(t) {\n" \
  "  t.expectTrue(localHostOrDomainIs(\"www.google.com\", \"www.google.com\"));\n" \
  "  t.expectTrue(localHostOrDomainIs(\"www\", \"www.google.com\"));\n" \
  "  t.expectFalse(localHostOrDomainIs(\"maps.google.com\", \"www.google.com\"));\n" \
  "};\n" \
  "\n" \
  "Tests.testShExpMatch = function(t) {\n" \
  "  t.expectTrue(shExpMatch(\"foo.jpg\", \"*.jpg\"));\n" \
  "  t.expectTrue(shExpMatch(\"foo5.jpg\", \"*o?.jpg\"));\n" \
  "  t.expectFalse(shExpMatch(\"foo.jpg\", \".jpg\"));\n" \
  "  t.expectFalse(shExpMatch(\"foo.jpg\", \"foo\"));\n" \
  "};\n" \
  "\n" \
  "Tests.testSortIpAddressList = function(t) {\n" \
  "  t.expectEquals(\"::1;::2;::3\", sortIpAddressList(\"::2;::3;::1\"));\n" \
  "  t.expectEquals(\n" \
  "      \"2001:4898:28:3:201:2ff:feea:fc14;fe80::5efe:157:9d3b:8b16;157.59.139.22\",\n" \
  "      sortIpAddressList(\"157.59.139.22;\" +\n" \
  "                        \"2001:4898:28:3:201:2ff:feea:fc14;\" +\n" \
  "                        \"fe80::5efe:157:9d3b:8b16\"));\n" \
  "\n" \
  "  // Single IP address (v4 and v6).\n" \
  "  t.expectEquals(\"127.0.0.1\", sortIpAddressList(\"127.0.0.1\"));\n" \
  "  t.expectEquals(\"::1\", sortIpAddressList(\"::1\"))\n" \
  "\n" \
  "  // Verify that IPv6 address is not re-written (not reduced).\n" \
  "  t.expectEquals(\"0:0::1;192.168.1.1\", sortIpAddressList(\"192.168.1.1;0:0::1\"));\n" \
  "\n" \
  "  // Input is already sorted.\n" \
  "  t.expectEquals(\"::1;192.168.1.3\", sortIpAddressList(\"::1;192.168.1.3\"));\n" \
  "\n" \
  "  // Same-valued IP addresses (also tests stability).\n" \
  "  t.expectEquals(\"0::1;::1;0:0::1\", sortIpAddressList(\"0::1;::1;0:0::1\"));\n" \
  "\n" \
  "  // Contains extra semi-colons.\n" \
  "  t.expectEquals(\"127.0.0.1\", sortIpAddressList(\";127.0.0.1;\"));\n" \
  "\n" \
  "  // Contains whitespace (spaces and tabs).\n" \
  "  t.expectEquals(\"192.168.0.1;192.168.0.2\",\n" \
  "      sortIpAddressList(\"192.168.0.1; 192.168.0.2\"));\n" \
  "  t.expectEquals(\"127.0.0.0;127.0.0.1;127.0.0.2\",\n" \
  "      sortIpAddressList(\"127.0.0.1;	127.0.0.2;	 127.0.0.0\"));\n" \
  "\n" \
  "  // Empty lists.\n" \
  "  t.expectFalse(sortIpAddressList(\"\"));\n" \
  "  t.expectFalse(sortIpAddressList(\" \"));\n" \
  "  t.expectFalse(sortIpAddressList(\";\"));\n" \
  "  t.expectFalse(sortIpAddressList(\";;\"));\n" \
  "  t.expectFalse(sortIpAddressList(\" ;  ; \"));\n" \
  "\n" \
  "  // Invalid IP addresses.\n" \
  "  t.expectFalse(sortIpAddressList(\"256.0.0.1\"));\n" \
  "  t.expectFalse(sortIpAddressList(\"192.168.1.1;0:0:0:1;127.0.0.1\"));\n" \
  "\n" \
  "  // Call sortIpAddressList() with wonky arguments.\n" \
  "  t.expectEquals(null, sortIpAddressList());\n" \
  "  t.expectEquals(null, sortIpAddressList(null));\n" \
  "  t.expectEquals(null, sortIpAddressList(null, null));\n" \
  "};\n" \
  "\n" \
  "Tests.testIsInNetEx = function(t) {\n" \
  "  t.expectTrue(isInNetEx(\"198.95.249.79\", \"198.95.249.79/32\"));\n" \
  "  t.expectTrue(isInNetEx(\"198.95.115.10\", \"198.95.0.0/16\"));\n" \
  "  t.expectTrue(isInNetEx(\"198.95.1.1\", \"198.95.0.0/16\"));\n" \
  "  t.expectTrue(isInNetEx(\"198.95.1.1\", \"198.95.3.3/16\"));\n" \
  "  t.expectTrue(isInNetEx(\"0:0:0:0:0:0:7f00:1\", \"0:0:0:0:0:0:7f00:1/32\"));\n" \
  "  t.expectTrue(isInNetEx(\"3ffe:8311:ffff:abcd:1234:dead:beef:101\",\n" \
  "                         \"3ffe:8311:ffff::/48\"));\n" \
  "\n" \
  "  // IPv4 and IPv6 mix.\n" \
  "  t.expectFalse(isInNetEx(\"127.0.0.1\", \"0:0:0:0:0:0:7f00:1/16\"));\n" \
  "  t.expectFalse(isInNetEx(\"192.168.24.3\", \"fe80:0:0:0:0:0:c0a8:1803/32\"));\n" \
  "\n" \
  "  t.expectFalse(isInNetEx(\"198.95.249.78\", \"198.95.249.79/32\"));\n" \
  "  t.expectFalse(isInNetEx(\"198.96.115.10\", \"198.95.0.0/16\"));\n" \
  "  t.expectFalse(isInNetEx(\"3fff:8311:ffff:abcd:1234:dead:beef:101\",\n" \
  "                          \"3ffe:8311:ffff::/48\"));\n" \
  "\n" \
  "  // Call isInNetEx with wonky arguments.\n" \
  "  t.expectEquals(null, isInNetEx());\n" \
  "  t.expectEquals(null, isInNetEx(null));\n" \
  "  t.expectEquals(null, isInNetEx(null, null));\n" \
  "  t.expectEquals(null, isInNetEx(null, null, null));\n" \
  "  t.expectEquals(null, isInNetEx(\"198.95.249.79\"));\n" \
  "\n" \
  "  // Invalid IP address.\n" \
  "  t.expectFalse(isInNetEx(\"256.0.0.1\", \"198.95.249.79\"));\n" \
  "  t.expectFalse(isInNetEx(\"127.0.0.1 \", \"127.0.0.1/32\"));  // Extra space.\n" \
  "\n" \
  "  // Invalid prefix.\n" \
  "  t.expectFalse(isInNetEx(\"198.95.115.10\", \"198.95.0.0/34\"));\n" \
  "  t.expectFalse(isInNetEx(\"127.0.0.1\", \"127.0.0.1\"));  // Missing '/' in prefix.\n" \
  "};\n" \
  "\n" \
  "Tests.testWeekdayRange = function(t) {\n" \
  "  // Test with local time.\n" \
  "  MockDate.setCurrent(\"Tue Mar 03 2009\");\n" \
  "  t.expectEquals(true, weekdayRange(\"MON\", \"FRI\"));\n" \
  "  t.expectEquals(true, weekdayRange(\"TUE\", \"FRI\"));\n" \
  "  t.expectEquals(true, weekdayRange(\"TUE\", \"TUE\"));\n" \
  "  t.expectEquals(true, weekdayRange(\"TUE\"));\n" \
  "  t.expectEquals(false, weekdayRange(\"WED\", \"FRI\"));\n" \
  "  t.expectEquals(false, weekdayRange(\"SUN\", \"MON\"));\n" \
  "  t.expectEquals(false, weekdayRange(\"SAT\"));\n" \
  "  t.expectEquals(false, weekdayRange(\"FRI\", \"MON\"));\n" \
  "\n" \
  "  // Test with GMT time.\n" \
  "  MockDate.setCurrent(\"Tue Mar 03 2009 GMT\");\n" \
  "  t.expectEquals(true, weekdayRange(\"MON\", \"FRI\", \"GMT\"));\n" \
  "  t.expectEquals(true, weekdayRange(\"TUE\", \"FRI\", \"GMT\"));\n" \
  "  t.expectEquals(true, weekdayRange(\"TUE\", \"TUE\", \"GMT\"));\n" \
  "  t.expectEquals(true, weekdayRange(\"TUE\", \"GMT\"));\n" \
  "  t.expectEquals(false, weekdayRange(\"WED\", \"FRI\", \"GMT\"));\n" \
  "  t.expectEquals(false, weekdayRange(\"SUN\", \"MON\", \"GMT\"));\n" \
  "  t.expectEquals(false, weekdayRange(\"SAT\", \"GMT\"));\n" \
  "};\n" \
  "\n" \
  "Tests.testDateRange = function(t) {\n" \
  "  // dateRange(day)\n" \
  "  MockDate.setCurrent(\"Mar 03 2009\");\n" \
  "  t.expectEquals(true, dateRange(3));\n" \
  "  t.expectEquals(false, dateRange(1));\n" \
  "\n" \
  "  // dateRange(day, \"GMT\")\n" \
  "  MockDate.setCurrent(\"Mar 03 2009 GMT\");\n" \
  "  t.expectEquals(true, dateRange(3, \"GMT\"));\n" \
  "  t.expectEquals(false, dateRange(1, \"GMT\"));\n" \
  "\n" \
  "  // dateRange(day1, day2)\n" \
  "  MockDate.setCurrent(\"Mar 03 2009\");\n" \
  "  t.expectEquals(true, dateRange(1, 4));\n" \
  "  t.expectEquals(false, dateRange(4, 20));\n" \
  "\n" \
  "  // dateRange(day, month)\n" \
  "  MockDate.setCurrent(\"Mar 03 2009\");\n" \
  "  t.expectEquals(true, dateRange(3, \"MAR\"));\n" \
  "  MockDate.setCurrent(\"Mar 03 2014\");\n" \
  "  t.expectEquals(true, dateRange(3, \"MAR\"));\n" \
  "  // TODO(eroman):\n" \
  "  //t.expectEquals(false, dateRange(2, \"MAR\"));\n" \
  "  //t.expectEquals(false, dateRange(3, \"JAN\"));\n" \
  "\n" \
  "  // dateRange(day, month, year)\n" \
  "  MockDate.setCurrent(\"Mar 03 2009\");\n" \
  "  t.expectEquals(true, dateRange(3, \"MAR\", 2009));\n" \
  "  t.expectEquals(false, dateRange(4, \"MAR\", 2009));\n" \
  "  t.expectEquals(false, dateRange(3, \"FEB\", 2009));\n" \
  "  MockDate.setCurrent(\"Mar 03 2014\");\n" \
  "  t.expectEquals(false, dateRange(3, \"MAR\", 2009));\n" \
  "\n" \
  "  // dateRange(month1, month2)\n" \
  "  MockDate.setCurrent(\"Mar 03 2009\");\n" \
  "  t.expectEquals(true, dateRange(\"JAN\", \"MAR\"));\n" \
  "  t.expectEquals(true, dateRange(\"MAR\", \"APR\"));\n" \
  "  t.expectEquals(false, dateRange(\"MAY\", \"SEP\"));\n" \
  "\n" \
  "  // dateRange(day1, month1, day2, month2)\n" \
  "  MockDate.setCurrent(\"Mar 03 2009\");\n" \
  "  t.expectEquals(true, dateRange(1, \"JAN\", 3, \"MAR\"));\n" \
  "  t.expectEquals(true, dateRange(3, \"MAR\", 4, \"SEP\"));\n" \
  "  t.expectEquals(false, dateRange(4, \"MAR\", 4, \"SEP\"));\n" \
  "\n" \
  "  // dateRange(month1, year1, month2, year2)\n" \
  "  MockDate.setCurrent(\"Mar 03 2009\");\n" \
  "  t.expectEquals(true, dateRange(\"FEB\", 2009, \"MAR\", 2009));\n" \
  "  MockDate.setCurrent(\"Apr 03 2009\");\n" \
  "  t.expectEquals(true, dateRange(\"FEB\", 2009, \"MAR\", 2010));\n" \
  "  t.expectEquals(false, dateRange(\"FEB\", 2009, \"MAR\", 2009));\n" \
  "\n" \
  "  // dateRange(day1, month1, year1, day2, month2, year2)\n" \
  "  MockDate.setCurrent(\"Mar 03 2009\");\n" \
  "  t.expectEquals(true, dateRange(1, \"JAN\", 2009, 3, \"MAR\", 2009));\n" \
  "  t.expectEquals(true, dateRange(3, \"MAR\", 2009, 4, \"SEP\", 2009));\n" \
  "  t.expectEquals(true, dateRange(3, \"JAN\", 2009, 4, \"FEB\", 2010));\n" \
  "  t.expectEquals(false, dateRange(4, \"MAR\", 2009, 4, \"SEP\", 2009));\n" \
  "};\n" \
  "\n" \
  "Tests.testTimeRange = function(t) {\n" \
  "  // timeRange(hour)\n" \
  "  MockDate.setCurrent(\"Mar 03, 2009 03:34:01\");\n" \
  "  t.expectEquals(true, timeRange(3));\n" \
  "  t.expectEquals(false, timeRange(2));\n" \
  "\n" \
  "  // timeRange(hour1, hour2)\n" \
  "  MockDate.setCurrent(\"Mar 03, 2009 03:34:01\");\n" \
  "  t.expectEquals(true, timeRange(2, 3));\n" \
  "  t.expectEquals(true, timeRange(2, 4));\n" \
  "  t.expectEquals(true, timeRange(3, 5));\n" \
  "  t.expectEquals(false, timeRange(1, 2));\n" \
  "  t.expectEquals(false, timeRange(11, 12));\n" \
  "\n" \
  "  // timeRange(hour1, min1, hour2, min2)\n" \
  "  MockDate.setCurrent(\"Mar 03, 2009 03:34:01\");\n" \
  "  t.expectEquals(true, timeRange(1, 0, 3, 34));\n" \
  "  t.expectEquals(true, timeRange(1, 0, 3, 35));\n" \
  "  t.expectEquals(true, timeRange(3, 34, 5, 0));\n" \
  "  t.expectEquals(false, timeRange(1, 0, 3, 0));\n" \
  "  t.expectEquals(false, timeRange(11, 0, 16, 0));\n" \
  "\n" \
  "  // timeRange(hour1, min1, sec1, hour2, min2, sec2)\n" \
  "  MockDate.setCurrent(\"Mar 03, 2009 03:34:14\");\n" \
  "  t.expectEquals(true, timeRange(1, 0, 0, 3, 34, 14));\n" \
  "  t.expectEquals(false, timeRange(1, 0, 0, 3, 34, 0));\n" \
  "  t.expectEquals(true, timeRange(1, 0, 0, 3, 35, 0));\n" \
  "  t.expectEquals(true, timeRange(3, 34, 0, 5, 0, 0));\n" \
  "  t.expectEquals(false, timeRange(1, 0, 0, 3, 0, 0));\n" \
  "  t.expectEquals(false, timeRange(11, 0, 0, 16, 0, 0));\n" \
  "};\n" \
  "\n" \
  "// --------------------------\n" \
  "// TestContext\n" \
  "// --------------------------\n" \
  "\n" \
  "// |name| is the name of the test being executed, it will be used when logging\n" \
  "// errors.\n" \
  "function TestContext(name) {\n" \
  "  this.numFailures_ = 0;\n" \
  "  this.name_ = name;\n" \
  "};\n" \
  "\n" \
  "TestContext.prototype.failed = function() {\n" \
  "  return this.numFailures_ != 0;\n" \
  "};\n" \
  "\n" \
  "TestContext.prototype.expectEquals = function(expectation, actual) {\n" \
  "  if (!(expectation === actual)) {\n" \
  "    this.numFailures_++;\n" \
  "    this.log(\"FAIL: expected: \" + expectation + \", actual: \" + actual);\n" \
  "  }\n" \
  "};\n" \
  "\n" \
  "TestContext.prototype.expectTrue = function(x) {\n" \
  "  this.expectEquals(true, x);\n" \
  "};\n" \
  "\n" \
  "TestContext.prototype.expectFalse = function(x) {\n" \
  "  this.expectEquals(false, x);\n" \
  "};\n" \
  "\n" \
  "TestContext.prototype.log = function(x) {\n" \
  "  // Prefix with the test name that generated the log.\n" \
  "  try {\n" \
  "    alert(this.name_ + \": \" + x);\n" \
  "  } catch(e) {\n" \
  "    // In case alert() is not defined.\n" \
  "  }\n" \
  "};\n" \
  "\n" \
  "// --------------------------\n" \
  "// MockDate\n" \
  "// --------------------------\n" \
  "\n" \
  "function MockDate() {\n" \
  "  this.wrappedDate_ = new MockDate.super_(MockDate.currentDateString_);\n" \
  "};\n" \
  "\n" \
  "// Setup the MockDate so it forwards methods to \"this.wrappedDate_\" (which is a\n" \
  "// real Date object).  We can't simply chain the prototypes since Date() doesn't\n" \
  "// allow it.\n" \
  "MockDate.init = function() {\n" \
  "  MockDate.super_ = Date;\n" \
  "\n" \
  "  function createProxyMethod(methodName) {\n" \
  "    return function() {\n" \
  "      return this.wrappedDate_[methodName]\n" \
  "          .apply(this.wrappedDate_, arguments);\n" \
  "    }\n" \
  "  };\n" \
  "\n" \
  "  for (i in MockDate.methodNames_) {\n" \
  "    var methodName = MockDate.methodNames_[i];\n" \
  "    // Don't define the closure directly in the loop body, since Javascript's\n" \
  "    // crazy scoping rules mean |methodName| actually bleeds out of the loop!\n" \
  "    MockDate.prototype[methodName] = createProxyMethod(methodName);\n" \
  "  }\n" \
  "\n" \
  "  // Replace the native Date() with our mock.\n" \
  "  Date = MockDate;\n" \
  "};\n" \
  "\n" \
  "// Unfortunately Date()'s methods are non-enumerable, therefore list manually.\n" \
  "MockDate.methodNames_ = [\n" \
  "  \"toString\", \"toDateString\", \"toTimeString\", \"toLocaleString\",\n" \
  "  \"toLocaleDateString\", \"toLocaleTimeString\", \"valueOf\", \"getTime\",\n" \
  "  \"getFullYear\", \"getUTCFullYear\", \"getMonth\", \"getUTCMonth\",\n" \
  "  \"getDate\", \"getUTCDate\", \"getDay\", \"getUTCDay\", \"getHours\", \"getUTCHours\",\n" \
  "  \"getMinutes\", \"getUTCMinutes\", \"getSeconds\", \"getUTCSeconds\",\n" \
  "  \"getMilliseconds\", \"getUTCMilliseconds\", \"getTimezoneOffset\", \"setTime\",\n" \
  "  \"setMilliseconds\", \"setUTCMilliseconds\", \"setSeconds\", \"setUTCSeconds\",\n" \
  "  \"setMinutes\", \"setUTCMinutes\", \"setHours\", \"setUTCHours\", \"setDate\",\n" \
  "  \"setUTCDate\", \"setMonth\", \"setUTCMonth\", \"setFullYear\", \"setUTCFullYear\",\n" \
  "  \"toGMTString\", \"toUTCString\", \"getYear\", \"setYear\"\n" \
  "];\n" \
  "\n" \
  "MockDate.setCurrent = function(currentDateString) {\n" \
  "  MockDate.currentDateString_ = currentDateString;\n" \
  "}\n" \
  "\n" \
  "// Bind the methods to proxy requests to the wrapped Date().\n" \
  "MockDate.init();\n" \
  "\n" \

#define PASSTHROUGH_JS \
  "// Return a single-proxy result, which encodes ALL the arguments that were\n" \
  "// passed to FindProxyForURL().\n" \
  "\n" \
  "function FindProxyForURL(url, host) {\n" \
  "  if (arguments.length != 2) {\n" \
  "    throw \"Wrong number of arguments passed to FindProxyForURL!\";\n" \
  "    return \"FAIL\";\n" \
  "  }\n" \
  "\n" \
  "  return \"PROXY \" + makePseudoHost(url + \".\" + host);\n" \
  "}\n" \
  "\n" \
  "// Form a string that kind-of resembles a host. We will replace any\n" \
  "// non-alphanumeric character with a dot, then fix up the oddly placed dots.\n" \
  "function makePseudoHost(str) {\n" \
  "  var result = \"\";\n" \
  "\n" \
  "  for (var i = 0; i < str.length; ++i) {\n" \
  "    var c = str.charAt(i);\n" \
  "    if (!isValidPseudoHostChar(c)) {\n" \
  "      c = '.';  // Replace unsupported characters with a dot.\n" \
  "    }\n" \
  "\n" \
  "    // Take care not to place multiple adjacent dots,\n" \
  "    // a dot at the beginning, or a dot at the end.\n" \
  "    if (c == '.' &&\n" \
  "        (result.length == 0 || \n" \
  "         i == str.length - 1 ||\n" \
  "         result.charAt(result.length - 1) == '.')) {\n" \
  "      continue;\n" \
  "    }\n" \
  "    result += c;\n" \
  "  }\n" \
  "  return result;\n" \
  "}\n" \
  "\n" \
  "function isValidPseudoHostChar(c) {\n" \
  "  if (c >= '0' && c <= '9')\n" \
  "    return true;\n" \
  "  if (c >= 'a' && c <= 'z')\n" \
  "    return true;\n" \
  "  if (c >= 'A' && c <= 'Z')\n" \
  "    return true;\n" \
  "  return false;\n" \
  "}\n" \

#define RETURN_EMPTY_STRING_JS \
  "function FindProxyForURL(url, host) {\n" \
  "  return \"\";\n" \
  "}\n" \
  "\n" \

#define RETURN_FUNCTION_JS \
  "function FindProxyForURL(url, host) {\n" \
  "  return FindProxyForURL;\n" \
  "}\n" \
  "\n" \

#define RETURN_INTEGER_JS \
  "function FindProxyForURL(url, host) {\n" \
  "  return 0;\n" \
  "}\n" \
  "\n" \

#define RETURN_NULL_JS \
  "function FindProxyForURL(url, host) {\n" \
  "  return null;\n" \
  "}\n" \
  "\n" \

#define RETURN_OBJECT_JS \
  "function FindProxyForURL(url, host) {\n" \
  "  return {result: \"PROXY foo\"};\n" \
  "}\n" \
  "\n" \

#define RETURN_UNDEFINED_JS \
  "function FindProxyForURL(url, host) {\n" \
  "  return undefined;\n" \
  "}\n" \
  "\n" \

#define RETURN_UNICODE_JS \
  "// U+200B is the codepoint for zero-width-space.\n" \
  "function FindProxyForURL(url, host) {\n" \
  "  return \"PROXY foo.com\u200B\";\n" \
  "}\n" \

#define SIDE_EFFECTS_JS \
  "if (!gCounter) {\n" \
  "  // We write it this way so if the script gets loaded twice,\n" \
  "  // gCounter remains dirty.\n" \
  "  var gCounter = 0;\n" \
  "}\n" \
  "\n" \
  "function FindProxyForURL(url, host) {\n" \
  "  return \"PROXY sideffect_\" + gCounter++;\n" \
  "}\n" \
  "\n" \

#define SIMPLE_JS \
  "// PAC script which uses isInNet on both IP addresses and hosts, and calls\n" \
  "// isResolvable().\n" \
  "\n" \
  "function FindProxyForURL(url, host) {\n" \
  "  var my_ip = myIpAddress();\n" \
  "\n" \
  "  if (isInNet(my_ip, \"172.16.0.0\", \"255.248.0.0\")) {\n" \
  "    return \"PROXY a:80\";\n" \
  "  }\n" \
  "\n" \
  "  if (url.substring(0, 6) != \"https:\" &&\n" \
  "      isInNet(host, \"10.0.0.0\", \"255.0.0.0\")) {\n" \
  "    return \"PROXY b:80\";\n" \
  "  }\n" \
  "\n" \
  "  if (dnsDomainIs(host, \"foo.bar.baz.com\") || !isResolvable(host)) {\n" \
  "    return \"PROXY c:100\";\n" \
  "  }\n" \
  "\n" \
  "  return \"DIRECT\";\n" \
  "}\n" \

#define UNHANDLED_EXCEPTION_JS \
  "function FindProxyForURL(url, host) {\n" \
  "  // This will throw a runtime exception.\n" \
  "  return \"PROXY x\" + undefined_variable;\n" \
  "}\n" \
  "\n" \

#endif //PROXY_TEST_SCRIPT_H_
