package org.robolectric.internal.bytecode;

import static com.google.common.truth.Truth.assertThat;

import android.os.Build;
import java.lang.reflect.Modifier;
import javax.annotation.Nonnull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.junit.runners.model.FrameworkMethod;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.AndroidInterceptors;
import org.robolectric.internal.AndroidConfigurer;
import org.robolectric.internal.SandboxFactory;
import org.robolectric.plugins.DefaultSdkProvider;

@RunWith(JUnit4.class)
public class AndroidSandboxClassLoaderTest {

  private ClassLoader classLoader;

  @Before
  public void setUp() throws Exception {
    InstrumentationConfiguration.Builder builder =
        InstrumentationConfiguration.newBuilder().withDefaults();
    AndroidConfigurer.configure(builder, new Interceptors());
    
    classLoader = new SandboxClassLoader(
        ClassLoader.getSystemClassLoader(),
        builder.build(),
        null);
  }

  @Test
  public void shouldMakeBuildVersionIntsNonFinal() throws Exception {
    Class<?> versionClass = loadClass(Build.VERSION.class);
    int modifiers = versionClass.getDeclaredField("SDK_INT").getModifiers();
    assertThat(Modifier.isFinal(modifiers)).named("SDK_INT should be non-final").isFalse();
  }

  ////////////////////////

  public static class DummyTest {
  }

  @Nonnull
  private InstrumentationConfiguration.Builder configureBuilder() {
    InstrumentationConfiguration.Builder builder = InstrumentationConfiguration.newBuilder();
    builder.doNotAcquirePackage("java.");
    AndroidConfigurer.configure(builder, new Interceptors(AndroidInterceptors.all()));
    return builder;
  }

  private Class<?> loadClass(Class<?> clazz) throws ClassNotFoundException {
    return classLoader.loadClass(clazz.getName());
  }
}
