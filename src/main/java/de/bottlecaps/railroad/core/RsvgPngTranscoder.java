package de.bottlecaps.railroad.core;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import net.sf.saxon.Configuration;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.Serializer;
import net.sf.saxon.s9api.XdmNode;

public class RsvgPngTranscoder implements PngTranscoder
{
  public static final String RSVG_CONVERT_PROPERTY = "rsvg.convert";
  public static final String RSVG_CONVERT_DEFAULT = "rsvg-convert";
  private String rsvgConvert = rsvgConvertPath();

  public static String rsvgConvertPath() {
    String rsvgPath = System.getProperty(RSVG_CONVERT_PROPERTY);
    return rsvgPath != null ? rsvgPath : RSVG_CONVERT_DEFAULT;
  }

  @Override
  public void transcode(XdmNode svg, OutputStream o) throws Exception {
    File pngFile = File.createTempFile(XhtmlToZip.class.getName() + "-", ".png");
    String pngFileName = pngFile.getAbsolutePath();
    String nativeFileName = pngFileName;

    // Avoid problems due to Windows file system redirection,
    // when running inside of a 64 bit container.

    if ("amd64".equals(System.getProperty("os.arch")))
    {
      nativeFileName = pngFileName.replaceFirst("\\\\[Ss][Yy][Ss][Tt][Ee][Mm]32\\\\", "\\\\sysnative\\\\");
    }

    try
    {
      String[] commandLine = {rsvgConvert, "-o", nativeFileName};
//    System.out.println("executing " + Arrays.toString(commandLine));
      Process proc = Runtime.getRuntime().exec(commandLine);

      OutputStream stdin = proc.getOutputStream();
      Serializer serializer = new Processor(new Configuration()).newSerializer(stdin);
      serializer.setOutputProperty(Serializer.Property.OMIT_XML_DECLARATION, "no");
      serializer.setOutputProperty(Serializer.Property.INDENT, "yes");
      serializer.serializeNode(svg);
      stdin.close();

      StreamCollector stderr = new StreamCollector(proc.getErrorStream());
      StreamCollector stdout = new StreamCollector(proc.getInputStream());

      try
      {
        int exitCode = proc.waitFor();
        if (exitCode != 0)
        {
          throw new RuntimeException(rsvgConvert + " returned exit code " + exitCode + ", " + stderr + " " + stdout);
        }
      }
      catch (InterruptedException e)
      {
        throw new RuntimeException(e.getMessage(), e);
      }

      try (FileInputStream fis = new FileInputStream(pngFile))
      {
        byte[] buffer = new byte[2 * 1024 * 1024];
        int size = 0;
        for (int n = 0; n >= 0; n = fis.read(buffer, size, buffer.length - size))
        {
          size += n;
          if (size == buffer.length)
          {
            for (; fis.read() >= 0; ) {}
            break;
          }
        }
        o.write(buffer, 0, size);
      }
    }
    finally
    {
      pngFile.delete();
    }
  }

  private static class StreamCollector extends Thread
  {
    InputStream stream;
    String string;

    public StreamCollector(InputStream s)
    {
      stream = s;
      string = null;
      start();
    }

    @Override
    public String toString()
    {
      if (string == null)
      {
        try
        {
          join();
        }
        catch (InterruptedException e)
        {
          throw new RuntimeException(e.getMessage(), e);
        }
      }
      return string;
    }

    @Override
    public void run()
    {
      try
      {
        byte[] buffer = new byte[2 * 1024 * 1024];
        int size = 0;
        for (int n = 0; n >= 0; n = stream.read(buffer, size, buffer.length - size))
        {
          size += n;
          if (size == buffer.length)
          {
            for (; stream.read() >= 0; ) {}
            break;
          }
        }
        string = new String(buffer, 0, size, StandardCharsets.UTF_8);
      }
      catch (IOException e)
      {
        throw new RuntimeException(e.getMessage(), e);
      }
    }
  }
}
