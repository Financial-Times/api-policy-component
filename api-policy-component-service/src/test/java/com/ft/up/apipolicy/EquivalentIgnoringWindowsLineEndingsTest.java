package com.ft.up.apipolicy;

import static com.ft.up.apipolicy.EquivalentIgnoringWindowsLineEndings.equivalentToUnixString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

import org.junit.Test;

/**
 * EquivalentIgnoringWindowsLineEndingsTest
 *
 * @author Simon.Gibbs
 */
public class EquivalentIgnoringWindowsLineEndingsTest {

  @Test
  public void shouldMatchAWindowsStringToUNIXOne() {
    assertThat("hello\r\nworld", equivalentToUnixString("hello\nworld"));
  }

  @Test
  public void shouldNotMatchDifferentStrings() {
    assertThat("hello\r\nfred", not(equivalentToUnixString("hello\nworld")));
  }

  @Test
  public void shouldNotTreatOtherWhitespaceAsEquivalent() {
    assertThat("hello\tworld", not(equivalentToUnixString("hello world")));
  }
}
