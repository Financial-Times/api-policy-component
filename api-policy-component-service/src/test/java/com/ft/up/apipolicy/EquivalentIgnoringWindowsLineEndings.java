package com.ft.up.apipolicy;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;

/**
 * On Windows, strips carriage returns before performing a strict equality check.
 *
 * <p>On Linux, equivalent to {@link CoreMatchers#equalTo(Object)}.
 *
 * @author Simon.Gibbs
 */
public class EquivalentIgnoringWindowsLineEndings extends TypeSafeDiagnosingMatcher<String> {

  private String expectedValue;

  private EquivalentIgnoringWindowsLineEndings(String expectedValue) {
    this.expectedValue = expectedValue;
  }

  public static Matcher<String> equivalentToUnixString(String expectedValue) {
    return new EquivalentIgnoringWindowsLineEndings(expectedValue);
  }

  @Override
  protected boolean matchesSafely(String actual, Description description) {
    String unixString = actual.replace("\r\n", "\n");
    return unixString.equals(expectedValue);
  }

  @Override
  public void describeTo(Description description) {
    description.appendText("a Windows version of the UNIX string: ");
    description.appendText(expectedValue);
  }
}
