package com.google.android.droiddriver.finders;

import junit.framework.TestCase;

/**
 * Test XPaths.
 */
public class XPathsTest extends TestCase {
  public void testQuoteXPathLiteral() {
    assertEquals("concat(\"a\",'\"',\"'b\")", XPaths.quoteXPathLiteral("a\"'b"));
  }
}
