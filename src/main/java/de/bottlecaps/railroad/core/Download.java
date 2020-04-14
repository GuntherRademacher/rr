package de.bottlecaps.railroad.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import de.bottlecaps.railroad.Railroad;
import de.bottlecaps.railroad.RailroadVersion;

public class Download
{
  public static final String WAR_FILENAME = RailroadVersion.PROJECT_NAME + ".war";
  public static final String DOWNLOAD_FILENAME =
    RailroadVersion.PROJECT_NAME +
    "-" + RailroadVersion.VERSION +
    "-java" + javaVersion() +
    ".zip";

  public static void distZip(OutputStream outputStream) throws IOException, FileNotFoundException
  {
    File warFile = warFile();
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
            zipOutputStream.putNextEntry(entry);
            copy(warStream, zipOutputStream);
          }
      }

      ZipEntry readmeEntry = new ZipEntry("README.txt");
      readmeEntry.setTime(warFile.lastModified());
      zipOutputStream.putNextEntry(readmeEntry);
      try (PrintStream readMe = new PrintStream(zipOutputStream))
      {
        Railroad.usage(readMe, warFile.getName());
      }
    }
  }

  public static void copy(InputStream in, OutputStream out) throws IOException
  {
    byte[] buffer = new byte[0x8000];
    for (int length; (length = in.read(buffer)) > 0;)
      out.write(buffer, 0, length);
  }

  public static File warFile()
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
          WAR_FILENAME);
      if (file.exists())
        return file;
    }

    String javaCommand = System.getProperty("sun.java.command");
    if (javaCommand != null)
    {
      for (String arg : javaCommand.split(" "))
      {
        String[] path = arg.split("[\\\\/]");
        if (path[path.length - 1].equalsIgnoreCase(WAR_FILENAME))
        {
          File file = new File(arg);
          if (file.exists())
            return file;
        }
      }
    }
    return null;
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
