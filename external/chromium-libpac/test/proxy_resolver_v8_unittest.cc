// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#define LOG_TAG "ProxyResolverTest"

#include <utils/Log.h>
#include "android_runtime/AndroidRuntime.h"
#include <string.h>

#include "proxy_test_script.h"
#include "proxy_resolver_v8.h"
#include "include/gtest/gtest.h"

using namespace android;
namespace net {
namespace {

// Javascript bindings for ProxyResolverV8, which returns mock values.
// Each time one of the bindings is called into, we push the input into a
// list, for later verification.
class MockJSBindings : public ProxyResolverJSBindings, public ProxyErrorListener {
 public:
  MockJSBindings() : my_ip_address_count(0), my_ip_address_ex_count(0) {}

  virtual bool MyIpAddress(std::string* ip_address) {
    my_ip_address_count++;
    *ip_address = my_ip_address_result;
    return !my_ip_address_result.empty();
  }

  virtual bool MyIpAddressEx(std::string* ip_address_list) {
    my_ip_address_ex_count++;
    *ip_address_list = my_ip_address_ex_result;
    return !my_ip_address_ex_result.empty();
  }

  virtual bool DnsResolve(const std::string& host, std::string* ip_address) {
    dns_resolves.push_back(host);
    *ip_address = dns_resolve_result;
    return !dns_resolve_result.empty();
  }

  virtual bool DnsResolveEx(const std::string& host,
                            std::string* ip_address_list) {
    dns_resolves_ex.push_back(host);
    *ip_address_list = dns_resolve_ex_result;
    return !dns_resolve_ex_result.empty();
  }

  virtual void AlertMessage(String16 message) {
    String8 m8(message);
    std::string mstd(m8.string());

    ALOGD("PAC-alert: %s\n", mstd.c_str());  // Helpful when debugging.
    alerts.push_back(mstd);
  }

  virtual void ErrorMessage(const String16 message) {
    String8 m8(message);
    std::string mstd(m8.string());

    ALOGD("PAC-error: %s\n", mstd.c_str());  // Helpful when debugging.
    errors.push_back(mstd);
  }

  virtual void Shutdown() {}

  // Mock values to return.
  std::string my_ip_address_result;
  std::string my_ip_address_ex_result;
  std::string dns_resolve_result;
  std::string dns_resolve_ex_result;

  // Inputs we got called with.
  std::vector<std::string> alerts;
  std::vector<std::string> errors;
  std::vector<std::string> dns_resolves;
  std::vector<std::string> dns_resolves_ex;
  int my_ip_address_count;
  int my_ip_address_ex_count;
};

// This is the same as ProxyResolverV8, but it uses mock bindings in place of
// the default bindings, and has a helper function to load PAC scripts from
// disk.
class ProxyResolverV8WithMockBindings : public ProxyResolverV8 {
 public:
  ProxyResolverV8WithMockBindings(MockJSBindings* mock_js_bindings) :
      ProxyResolverV8(mock_js_bindings, mock_js_bindings), mock_js_bindings_(mock_js_bindings) {
  }

  MockJSBindings* mock_js_bindings() const {
    return mock_js_bindings_;
  }

 private:
  MockJSBindings* mock_js_bindings_;
};

// Doesn't really matter what these values are for many of the tests.
const String16 kQueryUrl("http://www.google.com");
const String16 kQueryHost("www.google.com");
String16 kResults;

String16 currentPac;
#define SCRIPT(x) (currentPac = String16(x))

void addString(std::vector<std::string>* list, std::string str) {
  if (str.compare(0, 6, "DIRECT") == 0) {
    list->push_back("DIRECT");
  } else if (str.compare(0, 6, "PROXY ") == 0) {
    list->push_back(str.substr(6));
  } else {
    ALOGE("Unrecognized proxy string");
  }
}

std::vector<std::string> string16ToProxyList(String16 response) {
    std::vector<std::string> ret;
    String8 response8(response);
    std::string rstr(response8.string());
    if (rstr.find(';') == std::string::npos) {
        addString(&ret, rstr);
        return ret;
    }
    char str[128];
    rstr.copy(str, 0, rstr.length());
    const char* pch = strtok(str, ";");

    while (pch != NULL) {
        // Skip leading whitespace
        while ((*pch) == ' ') ++pch;
        std::string pstring(pch);
        addString(&ret, pstring);

        pch = strtok(NULL, "; \t");
    }

    return ret;
}

std::string StringPrintf(std::string str, int d) {
    char buf[30];
    sprintf(buf, str.c_str(), d);
    return std::string(buf);
}

TEST(ProxyResolverV8Test, Direct) {
  ProxyResolverV8WithMockBindings resolver(new MockJSBindings());
  int result = resolver.SetPacScript(SCRIPT(DIRECT_JS));
  EXPECT_EQ(OK, result);

  result = resolver.GetProxyForURL(kQueryUrl, kQueryHost, &kResults);

  EXPECT_EQ(OK, result);
  std::vector<std::string> proxies = string16ToProxyList(kResults);
  EXPECT_EQ(proxies.size(), 1U);
  EXPECT_EQ("DIRECT",proxies[0]);

  EXPECT_EQ(0U, resolver.mock_js_bindings()->alerts.size());
  EXPECT_EQ(0U, resolver.mock_js_bindings()->errors.size());
}

TEST(ProxyResolverV8Test, ReturnEmptyString) {
  ProxyResolverV8WithMockBindings resolver(new MockJSBindings());
  int result = resolver.SetPacScript(SCRIPT(RETURN_EMPTY_STRING_JS));
  EXPECT_EQ(OK, result);

  result = resolver.GetProxyForURL(kQueryUrl, kQueryHost, &kResults);

  EXPECT_EQ(OK, result);
  std::vector<std::string> proxies = string16ToProxyList(kResults);
  EXPECT_EQ(proxies.size(), 0U);

  EXPECT_EQ(0U, resolver.mock_js_bindings()->alerts.size());
  EXPECT_EQ(0U, resolver.mock_js_bindings()->errors.size());
}

TEST(ProxyResolverV8Test, Basic) {
  ProxyResolverV8WithMockBindings resolver(new MockJSBindings());
  int result = resolver.SetPacScript(SCRIPT(PASSTHROUGH_JS));
  EXPECT_EQ(OK, result);

  // The "FindProxyForURL" of this PAC script simply concatenates all of the
  // arguments into a pseudo-host. The purpose of this test is to verify that
  // the correct arguments are being passed to FindProxyForURL().
  {
    String16 queryUrl("http://query.com/path");
    String16 queryHost("query.com");
    result = resolver.GetProxyForURL(queryUrl, queryHost, &kResults);
    EXPECT_EQ(OK, result);
    std::vector<std::string> proxies = string16ToProxyList(kResults);
    EXPECT_EQ(1U, proxies.size());
    EXPECT_EQ("http.query.com.path.query.com", proxies[0]);
  }
  {
    String16 queryUrl("ftp://query.com:90/path");
    String16 queryHost("query.com");
    int result = resolver.GetProxyForURL(queryUrl, queryHost, &kResults);

    EXPECT_EQ(OK, result);
    // Note that FindProxyForURL(url, host) does not expect |host| to contain
    // the port number.
    std::vector<std::string> proxies = string16ToProxyList(kResults);
    EXPECT_EQ(1U, proxies.size());
    EXPECT_EQ("ftp.query.com.90.path.query.com", proxies[0]);

    EXPECT_EQ(0U, resolver.mock_js_bindings()->alerts.size());
    EXPECT_EQ(0U, resolver.mock_js_bindings()->errors.size());
  }

  // We call this so we'll have code coverage of the function and valgrind will
  // make sure nothing bad happens.
  //
  // NOTE: This is here instead of in its own test so that we'll be calling it
  // after having done something, in hopes it won't be a no-op.
  resolver.PurgeMemory();
}

TEST(ProxyResolverV8Test, BadReturnType) {
  // These are the files of PAC scripts which each return a non-string
  // types for FindProxyForURL(). They should all fail with
  // ERR_PAC_SCRIPT_FAILED.
  static const String16 files[] = {
      String16(RETURN_UNDEFINED_JS),
      String16(RETURN_INTEGER_JS),
      String16(RETURN_FUNCTION_JS),
      String16(RETURN_OBJECT_JS),
      String16(RETURN_NULL_JS)
  };

  for (size_t i = 0; i < 5; ++i) {
    ProxyResolverV8WithMockBindings resolver(new MockJSBindings());
    int result = resolver.SetPacScript(files[i]);
    EXPECT_EQ(OK, result);

    result = resolver.GetProxyForURL(kQueryUrl, kQueryHost, &kResults);

    EXPECT_EQ(ERR_PAC_SCRIPT_FAILED, result);

    MockJSBindings* bindings = resolver.mock_js_bindings();
    EXPECT_EQ(0U, bindings->alerts.size());
    ASSERT_EQ(1U, bindings->errors.size());
    EXPECT_EQ("FindProxyForURL() did not return a string.",
              bindings->errors[0]);
  }
}

// Try using a PAC script which defines no "FindProxyForURL" function.
TEST(ProxyResolverV8Test, NoEntryPoint) {
  ProxyResolverV8WithMockBindings resolver(new MockJSBindings());
  int result = resolver.SetPacScript(SCRIPT(NO_ENTRYPOINT_JS));
  EXPECT_EQ(ERR_PAC_SCRIPT_FAILED, result);

  result = resolver.GetProxyForURL(kQueryUrl, kQueryHost, &kResults);

  EXPECT_EQ(ERR_FAILED, result);
}

// Try loading a malformed PAC script.
TEST(ProxyResolverV8Test, ParseError) {
  ProxyResolverV8WithMockBindings resolver(new MockJSBindings());
  int result = resolver.SetPacScript(SCRIPT(MISSING_CLOSE_BRACE_JS));
  EXPECT_EQ(ERR_PAC_SCRIPT_FAILED, result);

  result = resolver.GetProxyForURL(kQueryUrl, kQueryHost, &kResults);

  EXPECT_EQ(ERR_FAILED, result);

  MockJSBindings* bindings = resolver.mock_js_bindings();
  EXPECT_EQ(0U, bindings->alerts.size());

  // We get one error during compilation.
  ASSERT_EQ(1U, bindings->errors.size());

  EXPECT_EQ("Uncaught SyntaxError: Unexpected end of input",
            bindings->errors[0]);
}

// Run a PAC script several times, which has side-effects.
TEST(ProxyResolverV8Test, SideEffects) {
  ProxyResolverV8WithMockBindings resolver(new MockJSBindings());
  int result = resolver.SetPacScript(SCRIPT(SIDE_EFFECTS_JS));

  // The PAC script increments a counter each time we invoke it.
  for (int i = 0; i < 3; ++i) {
    result = resolver.GetProxyForURL(kQueryUrl, kQueryHost, &kResults);
    EXPECT_EQ(OK, result);
    std::vector<std::string> proxies = string16ToProxyList(kResults);
    EXPECT_EQ(1U, proxies.size());
    EXPECT_EQ(StringPrintf("sideffect_%d", i),
              proxies[0]);
  }

  // Reload the script -- the javascript environment should be reset, hence
  // the counter starts over.
  result = resolver.SetPacScript(SCRIPT(SIDE_EFFECTS_JS));
  EXPECT_EQ(OK, result);

  for (int i = 0; i < 3; ++i) {
    result = resolver.GetProxyForURL(kQueryUrl, kQueryHost, &kResults);
    EXPECT_EQ(OK, result);
    std::vector<std::string> proxies = string16ToProxyList(kResults);
    EXPECT_EQ(1U, proxies.size());
    EXPECT_EQ(StringPrintf("sideffect_%d", i),
              proxies[0]);
  }
}

// Execute a PAC script which throws an exception in FindProxyForURL.
TEST(ProxyResolverV8Test, UnhandledException) {
  ProxyResolverV8WithMockBindings resolver(new MockJSBindings());
  int result = resolver.SetPacScript(SCRIPT(UNHANDLED_EXCEPTION_JS));
  EXPECT_EQ(OK, result);

  result = resolver.GetProxyForURL(kQueryUrl, kQueryHost, &kResults);

  EXPECT_EQ(ERR_PAC_SCRIPT_FAILED, result);

  MockJSBindings* bindings = resolver.mock_js_bindings();
  EXPECT_EQ(0U, bindings->alerts.size());
  ASSERT_EQ(1U, bindings->errors.size());
  EXPECT_EQ("Uncaught ReferenceError: undefined_variable is not defined",
            bindings->errors[0]);
}

TEST(ProxyResolverV8Test, ReturnUnicode) {
  ProxyResolverV8WithMockBindings resolver(new MockJSBindings());
  int result = resolver.SetPacScript(SCRIPT(RETURN_UNICODE_JS));
  EXPECT_EQ(OK, result);

  result = resolver.GetProxyForURL(kQueryUrl, kQueryHost, &kResults);

  // The result from this resolve was unparseable, because it
  // wasn't ASCII.
  EXPECT_EQ(ERR_PAC_SCRIPT_FAILED, result);
}

// Test the PAC library functions that we expose in the JS environmnet.
TEST(ProxyResolverV8Test, JavascriptLibrary) {
  ALOGE("Javascript start");
  ProxyResolverV8WithMockBindings resolver(new MockJSBindings());
  int result = resolver.SetPacScript(SCRIPT(PAC_LIBRARY_UNITTEST_JS));
  EXPECT_EQ(OK, result);

  result = resolver.GetProxyForURL(kQueryUrl, kQueryHost, &kResults);

  // If the javascript side of this unit-test fails, it will throw a javascript
  // exception. Otherwise it will return "PROXY success:80".
  EXPECT_EQ(OK, result);
  std::vector<std::string> proxies = string16ToProxyList(kResults);
  EXPECT_EQ(1U, proxies.size());
  EXPECT_EQ("success:80", proxies[0]);

  EXPECT_EQ(0U, resolver.mock_js_bindings()->alerts.size());
  EXPECT_EQ(0U, resolver.mock_js_bindings()->errors.size());
}

// Try resolving when SetPacScriptByData() has not been called.
TEST(ProxyResolverV8Test, NoSetPacScript) {
  ProxyResolverV8WithMockBindings resolver(new MockJSBindings());


  // Resolve should fail, as we are not yet initialized with a script.
  int result = resolver.GetProxyForURL(kQueryUrl, kQueryHost, &kResults);
  EXPECT_EQ(ERR_FAILED, result);

  // Initialize it.
  result = resolver.SetPacScript(SCRIPT(DIRECT_JS));
  EXPECT_EQ(OK, result);

  // Resolve should now succeed.
  result = resolver.GetProxyForURL(kQueryUrl, kQueryHost, &kResults);
  EXPECT_EQ(OK, result);

  // Clear it, by initializing with an empty string.
  resolver.SetPacScript(SCRIPT());

  // Resolve should fail again now.
  result = resolver.GetProxyForURL(kQueryUrl, kQueryHost, &kResults);
  EXPECT_EQ(ERR_FAILED, result);

  // Load a good script once more.
  result = resolver.SetPacScript(SCRIPT(DIRECT_JS));
  EXPECT_EQ(OK, result);
  result = resolver.GetProxyForURL(kQueryUrl, kQueryHost, &kResults);
  EXPECT_EQ(OK, result);

  EXPECT_EQ(0U, resolver.mock_js_bindings()->alerts.size());
  EXPECT_EQ(0U, resolver.mock_js_bindings()->errors.size());
}

// Test marshalling/un-marshalling of values between C++/V8.
TEST(ProxyResolverV8Test, V8Bindings) {
  ProxyResolverV8WithMockBindings resolver(new MockJSBindings());
  MockJSBindings* bindings = resolver.mock_js_bindings();
  bindings->dns_resolve_result = "127.0.0.1";
  int result = resolver.SetPacScript(SCRIPT(BINDINGS_JS));
  EXPECT_EQ(OK, result);

  result = resolver.GetProxyForURL(kQueryUrl, kQueryHost, &kResults);

  EXPECT_EQ(OK, result);
  std::vector<std::string> proxies = string16ToProxyList(kResults);
  EXPECT_EQ(1U, proxies.size());
  EXPECT_EQ("DIRECT", proxies[0]);

  EXPECT_EQ(0U, resolver.mock_js_bindings()->errors.size());

  // Alert was called 5 times.
  ASSERT_EQ(5U, bindings->alerts.size());
  EXPECT_EQ("undefined", bindings->alerts[0]);
  EXPECT_EQ("null", bindings->alerts[1]);
  EXPECT_EQ("undefined", bindings->alerts[2]);
  EXPECT_EQ("[object Object]", bindings->alerts[3]);
  EXPECT_EQ("exception from calling toString()", bindings->alerts[4]);

  // DnsResolve was called 8 times, however only 2 of those were string
  // parameters. (so 6 of them failed immediately).
  ASSERT_EQ(2U, bindings->dns_resolves.size());
  EXPECT_EQ("", bindings->dns_resolves[0]);
  EXPECT_EQ("arg1", bindings->dns_resolves[1]);

  // MyIpAddress was called two times.
  EXPECT_EQ(2, bindings->my_ip_address_count);

  // MyIpAddressEx was called once.
  EXPECT_EQ(1, bindings->my_ip_address_ex_count);

  // DnsResolveEx was called 2 times.
  ASSERT_EQ(2U, bindings->dns_resolves_ex.size());
  EXPECT_EQ("is_resolvable", bindings->dns_resolves_ex[0]);
  EXPECT_EQ("foobar", bindings->dns_resolves_ex[1]);
}

// Test calling a binding (myIpAddress()) from the script's global scope.
// http://crbug.com/40026
TEST(ProxyResolverV8Test, BindingCalledDuringInitialization) {
  ProxyResolverV8WithMockBindings resolver(new MockJSBindings());

  int result = resolver.SetPacScript(SCRIPT(BINDING_FROM_GLOBAL_JS));
  EXPECT_EQ(OK, result);

  MockJSBindings* bindings = resolver.mock_js_bindings();

  // myIpAddress() got called during initialization of the script.
  EXPECT_EQ(1, bindings->my_ip_address_count);

  result = resolver.GetProxyForURL(kQueryUrl, kQueryHost, &kResults);

  EXPECT_EQ(OK, result);
  std::vector<std::string> proxies = string16ToProxyList(kResults);
  EXPECT_EQ(1U, proxies.size());
  EXPECT_NE("DIRECT", proxies[0]);
  EXPECT_EQ("127.0.0.1:80", proxies[0]);

  // Check that no other bindings were called.
  EXPECT_EQ(0U, bindings->errors.size());
  ASSERT_EQ(0U, bindings->alerts.size());
  ASSERT_EQ(0U, bindings->dns_resolves.size());
  EXPECT_EQ(0, bindings->my_ip_address_ex_count);
  ASSERT_EQ(0U, bindings->dns_resolves_ex.size());
}

// Try loading a PAC script that ends with a comment and has no terminal
// newline. This should not cause problems with the PAC utility functions
// that we add to the script's environment.
// http://crbug.com/22864
TEST(ProxyResolverV8Test, EndsWithCommentNoNewline) {
  ProxyResolverV8WithMockBindings resolver(new MockJSBindings());
  int result = resolver.SetPacScript(SCRIPT(ENDS_WITH_COMMENT_JS));
  EXPECT_EQ(OK, result);

  result = resolver.GetProxyForURL(kQueryUrl, kQueryHost, &kResults);

  EXPECT_EQ(OK, result);
  std::vector<std::string> proxies = string16ToProxyList(kResults);
  EXPECT_EQ(1U, proxies.size());
  EXPECT_NE("DIRECT", proxies[0]);
  EXPECT_EQ("success:80", proxies[0]);
}

// Try loading a PAC script that ends with a statement and has no terminal
// newline. This should not cause problems with the PAC utility functions
// that we add to the script's environment.
// http://crbug.com/22864
TEST(ProxyResolverV8Test, EndsWithStatementNoNewline) {
  ProxyResolverV8WithMockBindings resolver(new MockJSBindings());
  int result = resolver.SetPacScript(
      SCRIPT(ENDS_WITH_STATEMENT_NO_SEMICOLON_JS));
  EXPECT_EQ(OK, result);

  result = resolver.GetProxyForURL(kQueryUrl, kQueryHost, &kResults);

  EXPECT_EQ(OK, result);
  std::vector<std::string> proxies = string16ToProxyList(kResults);
  EXPECT_EQ(1U, proxies.size());
  EXPECT_NE("DIRECT", proxies[0]);
  EXPECT_EQ("success:3", proxies[0]);
}

// Test the return values from myIpAddress(), myIpAddressEx(), dnsResolve(),
// dnsResolveEx(), isResolvable(), isResolvableEx(), when the the binding
// returns empty string (failure). This simulates the return values from
// those functions when the underlying DNS resolution fails.
TEST(ProxyResolverV8Test, DNSResolutionFailure) {
  ProxyResolverV8WithMockBindings resolver(new MockJSBindings());
  int result = resolver.SetPacScript(SCRIPT(DNS_FAIL_JS));
  EXPECT_EQ(OK, result);

  result = resolver.GetProxyForURL(kQueryUrl, kQueryHost, &kResults);

  EXPECT_EQ(OK, result);
  std::vector<std::string> proxies = string16ToProxyList(kResults);
  EXPECT_EQ(1U, proxies.size());
  EXPECT_NE("DIRECT", proxies[0]);
  EXPECT_EQ("success:80", proxies[0]);
}

TEST(ProxyResolverV8Test, DNSResolutionOfInternationDomainName) {
    return;
  ProxyResolverV8WithMockBindings resolver(new MockJSBindings());
  int result = resolver.SetPacScript(String16(INTERNATIONAL_DOMAIN_NAMES_JS));
  EXPECT_EQ(OK, result);

  // Execute FindProxyForURL().
  result = resolver.GetProxyForURL(kQueryUrl, kQueryHost, &kResults);

  EXPECT_EQ(OK, result);
  std::vector<std::string> proxies = string16ToProxyList(kResults);
  EXPECT_EQ(1U, proxies.size());
  EXPECT_EQ("DIRECT", proxies[0]);

  // Check that the international domain name was converted to punycode
  // before passing it onto the bindings layer.
  MockJSBindings* bindings = resolver.mock_js_bindings();

  ASSERT_EQ(1u, bindings->dns_resolves.size());
  EXPECT_EQ("xn--bcher-kva.ch", bindings->dns_resolves[0]);

  ASSERT_EQ(1u, bindings->dns_resolves_ex.size());
  EXPECT_EQ("xn--bcher-kva.ch", bindings->dns_resolves_ex[0]);
}

}  // namespace
}  // namespace net
