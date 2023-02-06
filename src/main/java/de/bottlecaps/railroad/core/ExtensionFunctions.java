package de.bottlecaps.railroad.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.function.Function;
import java.util.regex.Pattern;

import de.bottlecaps.webapp.Cookie;
import de.bottlecaps.webapp.MultiPart;
import de.bottlecaps.webapp.Request;
import de.bottlecaps.webapp.Response;
import de.bottlecaps.xml.XQueryProcessor;

public class ExtensionFunctions
{
  public static String[] parameterValues(Request httpRequest, String name)
  {
    List<String> parameterList = new ArrayList<>();
    if (isMultipart(httpRequest))
    {
      try
      {
        Collection<MultiPart> parts = httpRequest.getParts();
        if (parts != null)
        {
          for (MultiPart part : parts)
          {
            if (name.equals(part.getName()))
            {
              String value = toString(new CrLfNormalizer(part.getInputStream()), httpRequest.getCharacterEncoding());
              parameterList.add(value);
            }
          }
        }
      }
      catch (IOException e)
      {
        throw new RuntimeException(e);
      }
    }
    else
    {
      String[] parameterValues = httpRequest.getParameterValues(name);
      if (parameterValues != null)
      {
        for (String pv : parameterValues)
        {
          parameterList.add(CrLfNormalizer.normalize(pv));
        }
      }
    }
    return parameterList.toArray(String[]::new);
  }

  public static String toString(InputStream inputStream, String encoding)
  {
    if (encoding == null)
    {
      encoding = StandardCharsets.UTF_8.name();
    }
    try (Scanner s = new Scanner(inputStream, encoding)) {
      s.useDelimiter("\\A");
      String value = s.hasNext() ? s.next() : "";
      return value;
    }
  }

  public static void setSerializationParameter(Map<String, String> outputProperties, String name, String value)
  {
    outputProperties.put(name, value);
  }

  private static boolean isMultipart(Request request)
  {
    return request.getContentType() != null &&
           request.getContentType().toLowerCase().indexOf("multipart/form-data") > -1;
  }

  public static String decodeBase64(String base64) {
    return new String(Base64.getDecoder().decode(base64), StandardCharsets.UTF_8);
  }

  public static String binaryFile(Request request, String fileName) {
    try
    {
      Collection<MultiPart> parts = request.getParts();
      if (parts != null)
      {
        for (MultiPart part : parts)
        {
          if (fileName.equals(fileName(part)))
          {
            InputStream inputStream = part.getInputStream();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[0x8000];
            for (int length; (length = inputStream.read(buffer)) > 0;)
              outputStream.write(buffer, 0, length);
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
          }
        }
      }
      return null;
    }
    catch (RuntimeException e)
    {
      throw e;
    }
    catch (Exception e)
    {
      throw new RuntimeException(e);
    }
  }

  private static String fileName(MultiPart part)
  {
    for (String fragment : part.getHeader("Content-Disposition").split(";"))
    {
      if (fragment.trim().startsWith("filename"))
      {
        return fragment.substring(fragment.indexOf('=') + 1).trim().replace("\"", "");
      }
    }
    return null;
  }
  public static String partFilename(Request request, String partName) throws IOException {
    return fileName(request.getPart(partName));
  }

  public static String[] partNames(Request request) throws IOException {
    List<String> result = new ArrayList<>();
    Collection<MultiPart> parts = request.getParts();
    if (parts != null)
    {
      for (MultiPart part : parts)
      {
        result.add(part.getName());
      }
    }
    return result.toArray(String[]::new);
  }

  public static String[] parameterNames(Request request) {
    return Collections.list(request.getParameterNames()).toArray(String[]::new);
  }

  public static boolean methodGet(Request request) {
    return "GET".equals(request.getMethod());
  }

  public static void setHeader(Response response, String name, String value) {
    response.setHeader(name, value);
  }

  public static String userAgent(Request request) {
    return request.getHeader("User-Agent");
  }

  public static String xhtmlToZip(String xhtml, String textFormat, String graphicsFormat) {
    try
    {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      new XhtmlToZip().convert(xhtml, textFormat, graphicsFormat, baos);
      return Base64.getEncoder().encodeToString(baos.toByteArray());
    }
    catch (RuntimeException e)
    {
      throw e;
    }
    catch (Exception e)
    {
      throw new RuntimeException(e);
    }
  }

  public static Object parseXml(String xml) throws Exception
  {
    return XQueryProcessor.defaultXQueryProcessor().parseXml(xml);
  }

  public static String getCookie(Request request, String name) throws IOException
  {
    Cookie[] cookies = request.getCookies();
    if (cookies != null)
    {
      for (Cookie cookie : cookies)
      {
        if (cookie.getName().equals(name))
        {
          return cookie.getValue();
        }
      }
    }
    return null;
  }

  public static String requestUri(Request httpRequest) {
    return httpRequest.getRequestURI();
  }

  public static int textWidthNormal(String text)
  {
    return width(text, Normal::get);
  }

  public static int textWidthBold(String text)
  {
    return width(text, Bold::get);
  }

  private static int width(String text, Function<Integer, Integer> widthByCodepoint)
  {
    int width = 0;
    for (int i = 0; i < text.length(); )
    {
      int codePoint = text.codePointAt(i);
      width += widthByCodepoint.apply(codePoint);
      i += Character.charCount(codePoint);
    }
    return (width + 5) / 10;
  }

  public static Object unicodeClass(String charClass) throws Exception
  {
    return XQueryProcessor.defaultXQueryProcessor().parseXml(unicodeClassToSerializedXML(charClass)).getResultObject();
  }

  protected static String unicodeClassToSerializedXML(String charClass)
  {
    int first = 0;
    int last = 0x10FFFF;

    Pattern pattern = Pattern.compile("\\p{" + charClass + "}$");

    int lo = 0;
    int hi = 0;
    Boolean matches = null;

    StringBuilder sb = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
    sb.append("<g:charClass xmlns:g=\"http://www.w3.org/2001/03/XPath/grammar\">\n");
    for (int i = first; i <= last + 1; ++i)
    {
      boolean inClass = i > last || i > 0xD7FF && i < 0xE000 || i > 0xFFFD && i < 0x10000
          ? false
              : pattern.matcher(new String(Character.toChars(i))).matches();

      if (matches == null)
      {
        lo = i;
      }
      else if (matches != inClass)
      {
        if (matches)
          sb.append(charClassMember(lo, hi)).append("\n");
        lo = i;
      }
      hi = i;
      matches = inClass;
    }
    sb.append("</g:charClass>");
    return sb.toString();
  }

  private static String charClassMember(int lo, int hi)
  {
    if (lo == hi)
      if (lo >= 32 && lo <= 126)
        return String.format("  <g:char>%s</g:char>", escapedChar(lo));
      else
        return String.format("  <g:charCode value=\"%04X\"/>", lo);
    else
      if (lo >= 32 && lo <= 126 && hi >= 32 && hi <= 126)
        return String.format("  <g:charRange minChar=\"%s\" maxChar=\"%s\"/>", escapedChar(lo), escapedChar(hi));
      else
        return String.format("  <g:charCodeRange minValue=\"%04X\" maxValue=\"%04X\"/>", lo, hi);
  }

  private static Object escapedChar(int lo)
  {
    return Character.valueOf((char) lo).toString()
        .replace("&", "&amp;")
        .replace("<", "&lt;");
  }

  public static String svgToPng(String svg) throws Exception
  {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    BatikImgTranscoder.PNG.transcode(svg, baos);
    return Base64.getEncoder().encodeToString(baos.toByteArray());
  }
}
