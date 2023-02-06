package de.bottlecaps.railroad.webapp;

import static de.bottlecaps.xml.XQueryProcessor.defaultXQueryProcessor;
import static java.util.function.Function.identity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import de.bottlecaps.railroad.Railroad;
import de.bottlecaps.railroad.RailroadVersion;
import de.bottlecaps.railroad.core.Download;
import de.bottlecaps.railroad.core.ExtensionFunctions;
import de.bottlecaps.webapp.Request;
import de.bottlecaps.webapp.Response;
import de.bottlecaps.xml.XQueryProcessor.Plan;
import de.bottlecaps.xml.XQueryProcessor.Result;

public class RailroadWebApp
{
  private static final String DOWNLOAD_PATH = "/download/" + Download.DOWNLOAD_FILENAME;
  private static final String TEXT_XML = "text/xml";
  private static final String TEXT_HTML = "text/html";
  private static final String TEXT_PLAIN = "text/plain";
  private static final String APPLICATION_ZIP = "application/zip";
  private static final String UTF_8 = "UTF-8";

  private static Plan queryPlan;

  static
  {
    try
    {
      String moduleNamespace = "de/bottlecaps/railroad/xq/user-interface.xq";
      URL moduleURL = Thread.currentThread().getContextClassLoader().getResource(moduleNamespace);
      String query =
          "import module namespace ui='" + moduleNamespace + "' at '" + moduleURL + "';\n" +
          "ui:ui()";
      queryPlan = defaultXQueryProcessor().compile(query);
    }
    catch (Exception e)
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
      if (pathInfo.equals(DOWNLOAD_PATH) && Download.warFile(request) != null)
      {
        response.setStatus(200);
        response.setContentType(APPLICATION_ZIP);
        response.setHeader("Content-Disposition", "attachment; filename=" + Download.DOWNLOAD_FILENAME);
        OutputStream outputStream = response.getOutputStream();
        Download.distZip(
                (out, warName) -> Railroad.usage(out, warName),
                identity(),
                Download.warFile(request),
                outputStream);
      }
      else
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
        // establish default properties

        Map<String, String> outputOptions = new HashMap<>();
        outputOptions.put("method", "xml");
        outputOptions.put("media-type", TEXT_XML);
        outputOptions.put("encoding", UTF_8);

        Map<String, Object> externalVariables = new HashMap<>();
        externalVariables.put("{de/bottlecaps/railroad/xq/ast-to-svg.xq}version", RailroadVersion.VERSION);
        externalVariables.put("{de/bottlecaps/railroad/xq/ast-to-svg.xq}java-version", Download.javaVersion());
        externalVariables.put("{de/bottlecaps/railroad/xq/ast-to-svg.xq}date", RailroadVersion.DATE);

        String[] parameterValues = ExtensionFunctions.parameterValues(request, "padding");
        if (parameterValues.length > 0 && parameterValues[0].matches("[0-9]+")) {
          externalVariables.put("{de/bottlecaps/railroad/xq/ast-to-svg.xq}padding", Integer.parseInt(parameterValues[0]));
        }

        parameterValues = ExtensionFunctions.parameterValues(request, "strokewidth");
        if (parameterValues.length > 0 && parameterValues[0].matches("[0-9]+")) {
          externalVariables.put("{de/bottlecaps/railroad/xq/ast-to-svg.xq}stroke-width", Integer.parseInt(parameterValues[0]));
        }

        externalVariables.put("{de/bottlecaps/railroad/xq/user-interface.xq}request", request);
        externalVariables.put("{de/bottlecaps/railroad/xq/user-interface.xq}response", response);
        externalVariables.put("{de/bottlecaps/railroad/xq/user-interface.xq}output-options", outputOptions);

        Result result = queryPlan.evaluate(externalVariables);

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

        if (APPLICATION_ZIP.equals(outputOptions.get("media-type")))
        {
          outputOptions.put("method", "text");
          outputOptions.put("encoding", UTF_8);
          setResponseParameters(response, outputOptions);

          ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
          result.serialize(byteArrayOutputStream, outputOptions);
          String base64 = new String(byteArrayOutputStream.toByteArray(), StandardCharsets.UTF_8);

          try (OutputStream outputStream = response.getOutputStream()) {
            outputStream.write(Base64.getDecoder().decode(base64));
          }
        }
        else
        {
          setResponseParameters(response, outputOptions);
          try (OutputStream outputStream = response.getOutputStream()) {
            result.serialize(outputStream, outputOptions);
          }
        }
      }
      catch (Exception e)
      {
        throw new RuntimeException(e.getMessage(), e);
      }
    }
  }

  private void setResponseParameters(Response response, Map<String, String> outputOptions) {
    String mediaType = outputOptions.get("media-type");
    response.setContentType(mediaType);
    String encoding = outputOptions.get("encoding");
    response.setCharacterEncoding(encoding);
  }

  private static String contentType(String pathInfo)
  {
    if (pathInfo.endsWith(".js"))
      return "text/javascript";
    else if (pathInfo.endsWith(".ico"))
      return "image/x-icon";
    else if (pathInfo.endsWith(".zip"))
      return APPLICATION_ZIP;
    else
      return null;
  }
}
