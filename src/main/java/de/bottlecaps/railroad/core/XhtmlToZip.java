package de.bottlecaps.railroad.core;

import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.xml.transform.Source;

import net.sf.saxon.s9api.Axis;
import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.Serializer;
import net.sf.saxon.s9api.XQueryCompiler;
import net.sf.saxon.s9api.XQueryEvaluator;
import net.sf.saxon.s9api.XQueryExecutable;
import net.sf.saxon.s9api.XdmAtomicValue;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmNodeKind;
import net.sf.saxon.s9api.XdmSequenceIterator;
import net.sf.saxon.s9api.XdmValue;

public class XhtmlToZip
{
  private boolean verbose = false;
  private static Processor processor = null;
  private static XQueryExecutable executable = null;

  private PngTranscoder pngTranscoder;

  static
  {
    processor = new Processor(false);
    try
    {
      String query =
          "import module namespace d='de/bottlecaps/railroad/xq/disassemble.xq';\n" +
          "declare variable $input external;\n" +
          "declare variable $format external;\n" +
          "d:disassemble($input, $format)";
      XQueryCompiler xqueryCompiler = processor.newXQueryCompiler();
      xqueryCompiler.setModuleURIResolver(ResourceModuleUriResolver.instance);
      executable = xqueryCompiler.compile(query);
    }
    catch (SaxonApiException e)
    {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  public XhtmlToZip()
  {
    try
    {
      pngTranscoder = new BatikPngTranscoder();
    }
    catch (RuntimeException e)
    {
      throw e;
    }
    catch (Exception e)
    {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  private void toPng(XdmNode svg, OutputStream os) throws Exception
  {
    pngTranscoder.transcode(svg, os);
  }

  private void toXml(XdmNode e, OutputStream os) throws SaxonApiException
  {
    Serializer serializer = processor.newSerializer(os);
    serializer.setOutputProperty(Serializer.Property.OMIT_XML_DECLARATION, "no");
    serializer.setOutputProperty(Serializer.Property.INDENT, "yes");
    serializer.serializeNode(e);
  }

  private void toHtm(XdmNode e, OutputStream os) throws SaxonApiException
  {
    Serializer serializer = processor.newSerializer(os);
    serializer.setOutputProperty(Serializer.Property.OMIT_XML_DECLARATION, "yes");
    serializer.setOutputProperty(Serializer.Property.INDENT, "yes");
    serializer.setOutputProperty(Serializer.Property.METHOD, "html");
    serializer.setOutputProperty(Serializer.Property.ENCODING, StandardCharsets.UTF_8.name());
    serializer.setOutputProperty(Serializer.Property.VERSION, "4.01");
    serializer.setOutputProperty(Serializer.Property.DOCTYPE_SYSTEM, "http://www.w3.org/TR/html4/loose.dtd");
    serializer.setOutputProperty(Serializer.Property.DOCTYPE_PUBLIC, "-//W3C//DTD HTML 4.01 Transitional//EN");
    serializer.serializeNode(e);
  }

  private static XdmNode firstElementChild(XdmNode p)
  {
    for (XdmSequenceIterator<?> i = p.axisIterator(Axis.CHILD); i.hasNext(); )
    {
      XdmNode n = (XdmNode) i.next();
      if (n.getNodeKind() == XdmNodeKind.ELEMENT)
      {
        return n;
      }
    }
    return null;
  }

  private static XdmNode nextElementChild(XdmNode s)
  {
    for (XdmSequenceIterator<?> i = s.axisIterator(Axis.FOLLOWING_SIBLING); i.hasNext(); )
    {
      XdmNode n = (XdmNode) i.next();
      if (n.getNodeKind() == XdmNodeKind.ELEMENT)
      {
        return n;
      }
    }
    return null;
  }

  public void convert(Source source, OutputStream zip) throws Exception
  {
    XQueryEvaluator evaluator = executable.load();
    DocumentBuilder db = processor.newDocumentBuilder();

    evaluator.setExternalVariable(new QName("input"), db.build(source));
    evaluator.setExternalVariable(new QName("format"), new XdmAtomicValue("png"));
    XdmValue result = evaluator.evaluate();

    ZipOutputStream zipFile = new ZipOutputStream(zip);

    for (XdmNode e = firstElementChild((XdmNode) result.itemAt(0));
         e != null;
         e = nextElementChild(e))
    {
      String name = URLDecoder.decode(e.getAttributeValue(new QName("name")), StandardCharsets.UTF_8.name());
      zipFile.putNextEntry(new ZipEntry(name));

      XdmNode content = firstElementChild(e);
      if (name.endsWith(".png"))
      {
        if (verbose)
        {
          System.out.println("converting " + name + " using Batik");
        }
        toPng(content, zipFile);
      }
      else if (name.endsWith(".htm") || name.endsWith(".html"))
      {
        toHtm(content, zipFile);
      }
      else
      {
        toXml(content, zipFile);
      }

      zipFile.closeEntry();
    }

    zipFile.close();
  }
}
