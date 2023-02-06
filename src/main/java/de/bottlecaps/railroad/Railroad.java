package de.bottlecaps.railroad;

import static java.util.function.Function.identity;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import de.bottlecaps.railroad.RailroadGenerator.GraphicsFormat;
import de.bottlecaps.railroad.RailroadGenerator.TextFormat;
import de.bottlecaps.railroad.core.Download;
import de.bottlecaps.railroad.webapp.RailroadServer;

public class Railroad
{
  private static final String RR_URL = "https://bottlecaps.de/" + RailroadVersion.PROJECT_NAME;
  private static final String COLOR_PATTERN = "#[0-9a-fA-F]{6}";
  private static final String INTEGER_PATTERN = "[0-9]+";
  private static final int DEFAULT_PORT = 8080;

  public static void main(String[] args) throws Exception
  {
    RailroadGenerator generator = new RailroadGenerator();

    boolean input = false;

    int port = DEFAULT_PORT;
    boolean gui = false;
    int guiArgCount = 0;
    boolean distZip = false;
    Charset charset = null;
    boolean errors = false;

    for (int i = 0; i < args.length; ++i)
    {
      String arg = args[i];
      if (arg.equals("-suppressebnf"))
      {
        generator.setShowEbnf(false);
      }
      else if (arg.startsWith("-width:"))
      {
        String substring = arg.substring(7);
        if (substring.matches(INTEGER_PATTERN))
        {
          generator.setWidth(Integer.parseInt(substring));
        }
        else
        {
          System.err.println("invalid width value");
          System.err.println();
          errors = true;
          break;
        }
      }
      else if (arg.startsWith("-color:"))
      {
        String substring = arg.substring(7);
        if (substring.matches(COLOR_PATTERN))
        {
          Color color = Color.decode("0x" + substring.substring(1));
          generator.setBaseColor(color);
        }
        else
        {
          System.err.println("invalid color code, color code must match " + COLOR_PATTERN);
          System.err.println();
          errors = true;
          break;
        }
      }
      else if (arg.startsWith("-offset:"))
      {
        String substring = arg.substring(8);
        if (substring.matches(INTEGER_PATTERN))
        {
          generator.setColorOffset(Integer.parseInt(substring));
        }
        else
        {
          System.err.println("invalid offset value");
          System.err.println();
          errors = true;
          break;
        }
      }
      else if (arg.startsWith("-padding:"))
      {
        String substring = arg.substring(9);
        if (substring.matches(INTEGER_PATTERN))
        {
          generator.setPadding(Integer.parseInt(substring));
        }
        else
        {
          System.err.println("invalid padding value");
          System.err.println();
          errors = true;
          break;
        }
      }
      else if (arg.startsWith("-strokewidth:"))
      {
        String substring = arg.substring(13);
        if (substring.matches(INTEGER_PATTERN))
        {
          generator.setStrokeWidth(Integer.parseInt(substring));
        }
        else
        {
          System.err.println("invalid stroke width value");
          System.err.println();
          errors = true;
          break;
        }
      }
      else if (arg.equals("-gui"))
      {
        ++guiArgCount;
        gui = true;
      }
      else if (arg.startsWith("-port:"))
      {
        ++guiArgCount;
        String substring = arg.substring(6);
        if (substring.matches(INTEGER_PATTERN))
        {
          port = Integer.parseInt(substring);
        }
        else
        {
          port = -1;
        }
        if (port < 0 || port >= 0x10000)
        {
          System.err.println("invalid port value");
          System.err.println();
          errors = true;
          break;
        }
      }
      else if (arg.equals("-html"))
      {
        generator.setTextFormat(TextFormat.HTML);
      }
      else if (arg.equals("-md"))
      {
        generator.setTextFormat(TextFormat.MARKDOWN);
      }
      else if (arg.equals("-noembedded"))
      {
        generator.setEmbedded(false);
      }
      else if (arg.equals("-png"))
      {
        generator.setGraphicsFormat(GraphicsFormat.PNG);
      }
      else if (arg.startsWith("-out:"))
      {
        generator.setOutput(new FileOutputStream(arg.substring(5)));
      }
      else if (arg.equals("-keeprecursion"))
      {
        generator.setRecursionElimination(false);
      }
      else if (arg.equals("-nofactoring"))
      {
        generator.setFactoring(false);
      }
      else if (arg.equals("-noinline"))
      {
        generator.setInlineLiterals(false);
      }
      else if (arg.equals("-noepsilon"))
      {
        generator.setKeepEpsilon(false);
      }
      else if (arg.equals("-distZip"))
      {
        distZip = true;
      }
      else if (arg.startsWith("-enc:"))
      {
        charset = Charset.forName(arg.substring(5));
      }
      else if (arg.equals("-"))
      {
        input = true;
      }
      else if (arg.startsWith("-"))
      {
        System.err.println("unsupported option: " + arg);
        System.err.println();
        errors = true;
        break;
      }
      else
      {
        System.setIn(new FileInputStream(arg));
        input = true;
      }

      if (input)
      {
        if (i + 1 != args.length)
        {
          System.err.println("excessive input file specification: " + args[i + 1]);
          System.err.println();
          errors = true;
        }
        break;
      }
    }

    if (errors || (! input && ! gui && ! distZip))
    {
      String resource = Railroad.class.getResource("/" + Railroad.class.getName().replace('.',  '/') + ".class").toString();
      final String file = resource.startsWith("jar:fatjar:")
        ? RailroadVersion.PROJECT_NAME + ".war"
        : Railroad.class.getName();

      usage(System.err, file);
    }
    else if (distZip)
    {
      try (OutputStream outputStream = new FileOutputStream(Download.DOWNLOAD_FILENAME))
      {
        Download.distZip(
                (out, warName) -> Railroad.usage(out, warName),
                identity(),
                Download.warFile(),
                outputStream);
      }
    }
    else if (gui)
    {
      RailroadServer server = new RailroadServer(port);
      System.err.print("Now listening on " + "http://localhost:" + server.getPort() + "/");
      if (args.length > guiArgCount)
        System.err.print(" (ignoring all command line arguments except -gui and -port)");
      System.err.println();
      server.waitForStopRequest();
      System.err.println("Received stop request");
    }
    else
    {
      byte[] bytes = read(System.in);
      String grammar = decode(charset, bytes);
      generator.generate(grammar);
    }
  }

  public static void usage(PrintStream out, final String file)
  {
    final String jar = file.endsWith(".war") ? "-jar " : "";
    out.println("RR - Railroad Diagram Generator");
    out.println();
    out.println("  version " + RailroadVersion.VERSION);
    out.println("  released " + RailroadVersion.DATE);
    out.println("  from " + RR_URL);
    out.println();
    out.println("Usage: java " + jar + file + " {OPTION}... GRAMMAR");
    out.println("    or java " + jar + file + " -gui [-port:PORT]");
    out.println();
    out.println("  Options:");
    out.println();
    out.println("  -suppressebnf    do not show EBNF next to generated diagrams");
    out.println("  -keeprecursion   no direct recursion elimination");
    out.println("  -nofactoring     no left or right factoring");
    out.println("  -noinline        do not inline nonterminals that derive to single literals");
    out.println("  -noepsilon       remove nonterminal references that derive to epsilon only");
    out.println("  -color:COLOR     use COLOR as base color, pattern: " + COLOR_PATTERN);
    out.println("  -offset:OFFSET   hue offset to secondary color in degrees");
    out.println("  -html            create HTML output, rather than XHTML");
    out.println("  -md              create Markdown output, rather than XHTML");
    out.println("  -png             create PNG graphics, rather than SVG");
    out.println("  -noembedded      create text and graphics in separate files in a zip, rather than embedded graphics");
    out.println("  -out:FILE        create FILE, rather than writing result to standard output");
    out.println("  -width:PIXELS    try to break graphics into multiple lines, when width exceeds PIXELS (default 992)");
    out.println("  -enc:ENCODING    set grammar input encoding (default: autodetect UTF8/16 or use system encoding)");
    out.println();
    out.println("  GRAMMAR          path of grammar, in W3C style EBNF (use '-' for stdin)");
    out.println();
    out.println("  -gui             run GUI on http://localhost:" + DEFAULT_PORT + "/");
    out.println("  -port:PORT       use PORT rather than " + DEFAULT_PORT);

    if (! jar.isEmpty())
    {
      out.println();
      out.println(file + " is an executable war file. It can be run with \"java -jar\" as shown");
      out.println("above, but it can also be deployed in servlet containers like Tomcat or Jetty.");
    }
  }

  private static byte[] read(InputStream input) throws Exception
  {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    byte[] chunk = new byte[32768];
    for (int length; (length = input.read(chunk)) != -1; )
      buffer.write(chunk, 0, length);
    return buffer.toByteArray();
  }

  private static String decode(Charset charset, byte[] bytes)
  {
    if (charset == null)
    {
      return decode(bytes);
    }
    else
    {
      return decode(bytes, 0, charset);
    }
  }

  private static String decode(byte[] bytes)
  {
    final byte[] UTF_8    = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
    final byte[] UTF_16BE = {(byte) 0xFE, (byte) 0xFF};
    final byte[] UTF_16LE = {(byte) 0xFF, (byte) 0xFE};
    return startsWith(bytes, UTF_8)    ? decode(bytes,    UTF_8.length, StandardCharsets.UTF_8)
         : startsWith(bytes, UTF_16BE) ? decode(bytes, UTF_16BE.length, StandardCharsets.UTF_16BE)
         : startsWith(bytes, UTF_16LE) ? decode(bytes, UTF_16LE.length, StandardCharsets.UTF_16LE)
         :                               decode(bytes,               0, Charset.defaultCharset());
  }

  private static boolean startsWith(byte[] bytes, byte[] prefix)
  {
    return Arrays.equals(Arrays.copyOf(bytes, prefix.length), prefix);
  }

  private static String decode(byte[] bytes, int offset, Charset charset)
  {
    return new String(bytes, offset, bytes.length - offset, charset);
  }
}
