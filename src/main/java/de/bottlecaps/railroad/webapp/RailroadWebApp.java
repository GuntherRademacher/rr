package de.bottlecaps.railroad.webapp;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Base64;
import java.util.List;

import javax.xml.transform.OutputKeys;

import de.bottlecaps.railroad.RailroadVersion;
import de.bottlecaps.railroad.core.Download;
import de.bottlecaps.railroad.core.ExtensionFunctions;
import de.bottlecaps.railroad.core.ResourceModuleUriResolver;
import de.bottlecaps.railroad.core.TextWidth;
import de.bottlecaps.webapp.Request;
import de.bottlecaps.webapp.Response;
import net.sf.saxon.lib.Feature;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.Serializer;
import net.sf.saxon.s9api.Serializer.Property;
import net.sf.saxon.s9api.XQueryCompiler;
import net.sf.saxon.s9api.XQueryEvaluator;
import net.sf.saxon.s9api.XQueryExecutable;
import net.sf.saxon.s9api.XdmAtomicValue;
import net.sf.saxon.s9api.XdmValue;

public class RailroadWebApp
{
  private static final String DOWNLOAD_PATH = "/download/" + Download.DOWNLOAD_FILENAME;
  private static final String TEXT_XML = "text/xml";
  private static final String TEXT_HTML = "text/html";
  private static final String TEXT_PLAIN = "text/plain";
  private static final String APPLICATION_ZIP = "application/zip";
  private static final String UTF_8 = "UTF-8";
  private static Processor processor;
  private static XQueryExecutable executable;

  static
  {
    processor = new Processor(false);
    processor.setConfigurationProperty(Feature.XSD_VERSION, "1.1");

    new ExtensionFunctions().initialize(processor.getUnderlyingConfiguration());
    new TextWidth.SaxonInitializer().initialize(processor.getUnderlyingConfiguration());

    XQueryCompiler compiler = processor.newXQueryCompiler();
    compiler.setModuleURIResolver(ResourceModuleUriResolver.instance);
    try
    {
      String query =
          "import module namespace ui=\"de/bottlecaps/railroad/xq/user-interface.xq\";\n" +
          "ui:ui()";
      executable = compiler.compile(query);
    }
    catch (SaxonApiException e)
    {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  public void doRequest(Request request, Response response) throws IOException
  {
    String contextPath = request.getContextPath();
    String pathInfo = request.getPathInfo();
    if (pathInfo == null || "/".equals(pathInfo))
    {
      response.setContentType(TEXT_HTML);
      response.setCharacterEncoding(UTF_8);
      try (Writer writer = response.getWriter()) {
        writer.write(
            "<html>\n"
          + "  <head>\n"
          + "    <meta http-equiv=\"refresh\" content=\"0; url=" + contextPath + "/ui\">\n"
          + "    <meta http-equiv=\"expires\" content=\"0\">\n"
          + "  </head>\n"
          + "</html>");
      }
    }
    else if (! pathInfo.equals("/ui"))
    {
      if (! pathInfo.equals(DOWNLOAD_PATH) || ! download(response))
      {
        try (InputStream inputStream = RailroadWebApp.class.getResourceAsStream("/htdocs" + pathInfo)) {
          if (inputStream == null)
          {
            response.setContentType(TEXT_PLAIN);
            response.setCharacterEncoding(UTF_8);
            response.setStatus(404);
            try (Writer writer = response.getWriter())
            {
              writer.write("HTTP 404 Not Found");
            }
          }
          else
          {
            String contentType = contentType(pathInfo);
            if (contentType != null)
              response.setContentType(contentType);
            try (OutputStream outputStream = response.getOutputStream())
            {
              Download.copy(inputStream, outputStream);
            }
            catch (Exception e)
            {
            }
          }
        }
      }
    }
    else
    {
      try
      {
        response.setCharacterEncoding(UTF_8);
        response.setContentType(TEXT_XML);
        Serializer serializer = processor.newSerializer();

        // establish default properties

        ExtensionFunctions.setSerializationProperty(OutputKeys.METHOD, "xml", response, serializer);
        ExtensionFunctions.setSerializationProperty(OutputKeys.MEDIA_TYPE, TEXT_XML, response, serializer);
        ExtensionFunctions.setSerializationProperty(OutputKeys.ENCODING, UTF_8, response, serializer);

        XQueryEvaluator evaluator = executable.load();
        evaluator.setExternalVariable(new QName("de/bottlecaps/railroad/xq/ast-to-svg.xq", "version"), new XdmAtomicValue(RailroadVersion.VERSION));
        evaluator.setExternalVariable(new QName("de/bottlecaps/railroad/xq/ast-to-svg.xq", "java-version"), new XdmAtomicValue(Download.javaVersion()));
        evaluator.setExternalVariable(new QName("de/bottlecaps/railroad/xq/ast-to-svg.xq", "date"), new XdmAtomicValue(RailroadVersion.DATE));

        List<String> parameterValues = ExtensionFunctions.parameterValues(request, "padding");
        if (parameterValues.size() > 0 && parameterValues.get(0).matches("[0-9]+"))
          evaluator.setExternalVariable(new QName("de/bottlecaps/railroad/xq/ast-to-svg.xq", "padding"), new XdmAtomicValue(Integer.parseInt(parameterValues.get(0))));

        parameterValues = ExtensionFunctions.parameterValues(request, "strokewidth");
        if (parameterValues.size() > 0 && parameterValues.get(0).matches("[0-9]+"))
          evaluator.setExternalVariable(new QName("de/bottlecaps/railroad/xq/ast-to-svg.xq", "stroke-width"), new XdmAtomicValue(Integer.parseInt(parameterValues.get(0))));

        ExtensionFunctions.request.set(request, evaluator);
        ExtensionFunctions.response.set(response, evaluator);
        ExtensionFunctions.serializer.set(serializer, evaluator);

        XdmValue result = evaluator.evaluate();

//        System.out.println("Output properties: ");
//        for (Serializer.Property property : Serializer.Property.values())
//        {
//          String value = serializer.getOutputProperty(property);
//          if (value != null)
//          {
//            System.out.println(property + ": " + value);
//          }
//        }
//        System.out.println("End output properties");

        if ("application/zip".equals(serializer.getOutputProperty(Property.MEDIA_TYPE)))
        {
          ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
          serializer.setOutputStream(byteArrayOutputStream);
          serializer.setOutputProperty(Property.METHOD, "text");
          serializer.setOutputProperty(Property.ENCODING, UTF_8);
          serializer.serializeXdmValue(result);
          serializer.close();
          String base64 = new String(byteArrayOutputStream.toByteArray(), UTF_8);

          try (OutputStream outputStream = response.getOutputStream()) {
            outputStream.write(Base64.getDecoder().decode(base64));
          }
        }
        else
        {
          try (OutputStream outputStream = response.getOutputStream()) {
            serializer.setOutputStream(outputStream);
            serializer.serializeXdmValue(result);
            serializer.close();
          }
        }
      }
      catch (SaxonApiException e)
      {
        throw new RuntimeException(e.getMessage(), e);
      }
    }
  }

  private boolean download(Response response) throws IOException
  {
    File warFile = Download.warFile();
    if (warFile == null)
    {
      return false;
    }
    else
    {
      response.setStatus(200);
      response.setContentType(APPLICATION_ZIP);
      response.setHeader("Content-Disposition", "attachment; filename=" + Download.DOWNLOAD_FILENAME);
      OutputStream outputStream = response.getOutputStream();
      Download.distZip(outputStream);
      return true;
    }
  }

  private String contentType(String pathInfo)
  {
    if (pathInfo.endsWith(".png"))
      return "image/png";
    else if (pathInfo.endsWith(".ico"))
      return "image/x-icon";
    else if (pathInfo.endsWith(".zip"))
      return APPLICATION_ZIP;
    else
      return null;
  }
}
