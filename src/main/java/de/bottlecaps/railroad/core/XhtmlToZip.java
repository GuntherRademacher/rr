package de.bottlecaps.railroad.core;

import static de.bottlecaps.xml.XQueryProcessor.defaultXQueryProcessor;

import java.io.OutputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import de.bottlecaps.xml.XQueryProcessor.Plan;
import de.bottlecaps.xml.XQueryProcessor.Result;

public class XhtmlToZip
{
  private static Plan transformToFiles = null;
  private static Plan getSingleFileName = null;
  private static Plan getSingleFileContent = null;

  static
  {
    try
    {
      String moduleNamespace = "de/bottlecaps/railroad/xq/disassemble.xq";
      URL moduleURL = XhtmlToZip.class.getClassLoader().getResource(moduleNamespace);
      transformToFiles = defaultXQueryProcessor().compile(
          "import module namespace d='" + moduleNamespace + "' at '" + moduleURL + "';\n" +
          "declare variable $input external;\n" +
          "declare variable $text-format external;\n" +
          "declare variable $img-format external;\n" +
          "declare variable $inline external;\n" +
          "d:disassemble($input, $text-format, $img-format, $inline)");
      getSingleFileName = defaultXQueryProcessor().compile(
          "declare variable $files as document-node() external;\n" +
          "declare variable $index as xs:integer external;\n" +
          "string($files/files/file[$index]/@name)");
      getSingleFileContent = defaultXQueryProcessor().compile(
          "declare variable $files as document-node() external;\n" +
          "declare variable $index as xs:integer external;\n" +
          "$files/files/file[$index]/node()");
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

  public void convert(String xhtml, String textFormat, String graphicsFormat, OutputStream zip) throws Exception
  {
    Result files = transformToFiles.evaluate(Map.of(
        "input", defaultXQueryProcessor().parseXml(xhtml),
        "text-format", textFormat,
        "img-format", graphicsFormat,
        "inline", false));
    try (ZipOutputStream zipFile = new ZipOutputStream(zip))
    {
      for (int index = 1;; ++index) {
        Map<String, Object> externalVars = Map.of(
            "files", files,
            "index", index);
        String name = getSingleFileName
            .evaluate(externalVars)
            .serializeToString(Collections.singletonMap("method", "text"));
        if (name.isEmpty())
          break;
        Result content = getSingleFileContent
            .evaluate(externalVars);
        zipFile.putNextEntry(new ZipEntry(name));
        if (name.endsWith(".png"))
        {
          BatikImgTranscoder.PNG.transcode(content.serializeToString(Collections.emptyMap()), zipFile);
        }
        else if (name.endsWith(".htm") || name.endsWith(".html"))
        {
          content.serialize(zipFile, OutputOptions.HTML.value());
        }
        else if (name.endsWith(".xht") || name.endsWith(".xhtml"))
        {
          content.serialize(zipFile, OutputOptions.XHTML.value());
        }
        else if (name.endsWith(".svg") || name.endsWith(".xml"))
        {
          content.serialize(zipFile, OutputOptions.XML.value());
        }
        else if (name.endsWith(".md"))
        {
          content.serialize(zipFile, OutputOptions.TEXT.value());
        }
        else {
          throw new UnsupportedOperationException("serialization not supported for " + name);
        }
        zipFile.closeEntry();
      }
    }
  }
}
