package de.bottlecaps.railroad.core;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import de.bottlecaps.railroad.RailroadVersion;
import de.bottlecaps.webapp.Request;

public class Download
{
  public static final String DOWNLOAD_FILENAME =
    RailroadVersion.PROJECT_NAME +
    "-" + RailroadVersion.VERSION +
    "-java" + javaVersion() +
    ".zip";

  public static void distZip(
      BiConsumer<PrintStream, String> usage,
      Function<String, String> adaptLicense,
      File warFile,
      OutputStream outputStream) throws IOException, FileNotFoundException
  {
    try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream))
    {
      ZipEntry warEntry = new ZipEntry(warFile.getName());
      warEntry.setTime(warFile.lastModified());
      zipOutputStream.putNextEntry(warEntry);
      try (InputStream warStream = new FileInputStream(warFile))
      {
        copy(warStream, zipOutputStream);
      }

      try (ZipInputStream warStream = new ZipInputStream(new FileInputStream(warFile)))
      {
        for (ZipEntry entry; (entry = warStream.getNextEntry()) != null; )
          if (entry.getName().startsWith("LICENSE/"))
          {
            ZipEntry licenseEntry = new ZipEntry(entry.getName());
            licenseEntry.setTime(warFile.lastModified());
            zipOutputStream.putNextEntry(licenseEntry);

            String licenseContent = toString(warStream);
            licenseContent = adaptLicense.apply(licenseContent);
            zipOutputStream.write(licenseContent.getBytes("UTF-8"));
          }
      }

      ZipEntry readmeEntry = new ZipEntry("README.txt");
      readmeEntry.setTime(warFile.lastModified());
      zipOutputStream.putNextEntry(readmeEntry);
      try (PrintStream readMe = new PrintStream(zipOutputStream))
      {
        usage.accept(readMe, warFile.getName());
      }
    }
  }

  public static void copy(InputStream in, OutputStream out) throws IOException
  {
    byte[] buffer = new byte[0x8000];
    for (int length; (length = in.read(buffer)) > 0;)
      out.write(buffer, 0, length);
  }

  public static String toString(InputStream in) throws IOException
  {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    byte[] buffer = new byte[0x8000];
    for (int length; (length = in.read(buffer)) > 0;)
      out.write(buffer, 0, length);
    return new String(out.toByteArray(), StandardCharsets.UTF_8);
  }

  private static Optional<File> warFile = null;

  public static File warFile()
  {
    if (warFile == null)
    {
      warFile = Optional.empty();
      String javaCommand = System.getProperty("sun.java.command");
      if (javaCommand != null)
      {
        for (String arg : javaCommand.split(" "))
        {
          String[] path = arg.split("[\\\\/]");
          if (path[path.length - 1].endsWith(".war"))
          {
            File file = new File(arg);
            if (file.exists())
            {
              warFile = Optional.of(file);
              break;
            }
          }
        }
      }
    }
    return warFile.orElse(null);
  }

  public static File warFile(Request request)
  {
    if (warFile == null)
    {
      String catalinaBase = System.getProperty("catalina.base", System.getProperty("catalina.home"));
      if (catalinaBase != null)
      {
        File file = null;
        file = new File(
            catalinaBase +
            File.separator +
            "webapps" +
            File.separator +
            request.getContextPath().substring(1) +
            ".war");
        if (file.exists())
          warFile = Optional.of(file);
      }

      if (warFile == null)
        return warFile();
    }

    return warFile.orElse(null);
  }

  public static int javaVersion()
  {
    try (InputStream classFile = Download.class.getClassLoader().getResourceAsStream(Download.class.getName().replace('.', '/') + ".class"))
    {
      byte[] bytes = new byte[8];
      classFile.read(bytes);
      return bytes[7] - 44;
    }
    catch (IOException e)
    {
      throw new RuntimeException(e.getMessage(), e);
    }
  }
}
