package de.bottlecaps.fatjar;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public class Loader
{
  public static final String FATJAR_MAIN_CLASS = "FatJar-Main-Class";
  public static final String FATJAR_JARS = "FatJar-Jars";
  public static final String FATJAR_PROTOCOL = "fatjar";

  public static void main(String[] args) throws Exception
  {
    ClassLoader jarClassLoader = Loader.class.getClassLoader();
    URL.setURLStreamHandlerFactory(protocol ->
    {
      return ! FATJAR_PROTOCOL.equals(protocol) ? null :
        new URLStreamHandler()
        {
          @Override
          protected URLConnection openConnection(URL url) throws IOException
          {
            return new URLConnection(url)
            {
              @Override
              public void connect() throws IOException
              {
              }

              @Override
              public InputStream getInputStream() throws IOException
              {
                return Objects.requireNonNull(jarClassLoader.getResourceAsStream(url.getPath()), "failed to load " + url);
              }
            };
          }
        };
    });

    String classPath = Loader.class.getResource(Loader.class.getSimpleName() + ".class").toString();
    if (! classPath.startsWith("jar"))
      throw new RuntimeException("not loaded from jar - cannot find manifest");
    String manifestPath = classPath.substring(0, classPath.lastIndexOf("!") + 2) + JarFile.MANIFEST_NAME;
    InputStream manifestStream = new URL(manifestPath).openStream();
    Attributes mainAttributes = new Manifest(manifestStream).getMainAttributes();
    String jars = Objects.requireNonNull(mainAttributes.getValue(FATJAR_JARS), "missing manifest attribute: " + FATJAR_JARS);
    CodeSource jarCodeSource = Objects.requireNonNull(Loader.class.getProtectionDomain().getCodeSource(), "failed to identify jar file");
    List<URL> urls = new ArrayList<>(Collections.singletonList(jarCodeSource.getLocation()));
    for (String jarResource : jars.split(" "))
      urls.add(new URL(FATJAR_PROTOCOL + ":" + jarResource));
    try (URLClassLoader urlClassLoader = new URLClassLoader(urls.toArray(new URL[urls.size()]), jarClassLoader.getParent()))
    {
      Thread.currentThread().setContextClassLoader(urlClassLoader);
      String mainClass = Objects.requireNonNull(mainAttributes.getValue(FATJAR_MAIN_CLASS), "missing manifest attribute: " + FATJAR_MAIN_CLASS);
      urlClassLoader
          .loadClass(mainClass)
          .getMethod("main", String[].class)
          .invoke(null, (Object) args);
    }
  }
}
