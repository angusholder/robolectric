package org.robolectric;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.internal.Instrument;
import org.robolectric.internal.SandboxTestRunner;
import org.robolectric.internal.bytecode.SandboxConfig;
import org.robolectric.internal.bytecode.ShadowDecorator;

@RunWith(SandboxTestRunner.class)
public class MockOfTest {
  @Test
  @SandboxConfig(shadows = {ShadowThing.class})
  public void testRealMock() throws Exception {
    Thing thing = new Thing();

    Thing mockThing = mock(Thing.class);
    when(mockThing.doThing("value"))
        .thenReturn("from mock");
    assertThat(mockThing.doThing("value"))
        .isEqualTo("from mock");
  }

  @Test
  @SandboxConfig(shadows = {ShadowThing.class})
  public void testRoboMock() throws Exception {
    Thing thing = new Thing();

    when(mockOf(thing).doThing(eq("value")))
        .thenReturn("from mock");
    assertThat(thing.doThing("value"))
        .isEqualTo("from mock");
  }

  public static <T> T mockOf(T object) {
    try {
      Class<?> clazz = object.getClass();
      Field mockField = clazz.getDeclaredField(ShadowDecorator.MOCK_FIELD_NAME);
      mockField.setAccessible(true);
      Object mockObj = mockField.get(object);
      if (mockObj == null) {
        mockObj = mock(clazz);
        mockField.set(object, mockObj);
      }
      return (T) mockObj;
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  @Implements(Thing.class)
  public static class ShadowThing {
    static boolean wasCalled = false;

    public static ShadowingTest.AccountManager get(Object arg) {
      wasCalled = true;
      ShadowingTest.ShadowAccountManagerForTests.arg = arg;
      return mock(ShadowingTest.AccountManager.class);
    }
  }

  @Instrument
  static class Thing {
    public String doThing(String arg) {
      return "from real impl: " + arg;
    }
  }

}
