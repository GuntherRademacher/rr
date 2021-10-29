package de.bottlecaps.webapp.server;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import com.sun.net.httpserver.HttpExchange;

import de.bottlecaps.webapp.Cookie;
import de.bottlecaps.webapp.MultiPart;
import de.bottlecaps.webapp.Request;

//TODO: header names case-insensitive

@SuppressWarnings("all")
public class HttpRequest implements Request
{
  private static final String MULTIPART_MIME_TYPE = "multipart/form-data; boundary=";
  private static final byte[] CR_LF = new byte[]{'\r', '\n'};
  private static final byte[] MINUS_MINUS = new byte[]{'-', '-'};

  private HttpExchange httpExchange;
  private String contentTypeHeader;
  private List<MultiPart> parts;
  private Cookie[] cookies = null;

  public HttpRequest(HttpExchange httpExchange) throws Exception
  {
    this.httpExchange = httpExchange;

    List<String> contentTypeHeaders = httpExchange.getRequestHeaders().get("Content-Type");
    contentTypeHeader = contentTypeHeaders == null || contentTypeHeaders.size() != 1
        ? null
        : contentTypeHeaders.get(0);
    if (contentTypeHeader != null)
    {
      if (contentTypeHeader.startsWith(MULTIPART_MIME_TYPE))
        getMultiParts();
      else
        getParameters();
    }
  }

  @Override
  public String getContextPath()
  {
    return "";
  }

  @Override
  public Collection<MultiPart> getParts() throws IOException
  {
    return parts;
  }

  @Override
  public MultiPart getPart(String partName) throws IOException
  {
    return parts.stream()
        .filter(p -> partName.equals(p.getName()))
        .findFirst()
        .orElse(null);
  }

  @Override
  public String getCharacterEncoding()
  {
    String contentType = httpExchange.getRequestHeaders().getFirst("Content-Type");
    if (contentType != null)
    {
      String charset = "charset=";
      int charsetIndex = contentType.indexOf(charset);
      if (charsetIndex >= 0)
        return contentType.substring(charsetIndex + charset.length()).replace("\"", "").trim();
    }
    return null;
  }

  @Override
  public Enumeration<String> getParameterNames()
  {
    return Collections.enumeration(getParameters().keySet());
  }

  @Override
  public String[] getParameterValues(String name)
  {
    List<String> valueList = getParameters().get(name);
    return valueList == null ? null : valueList.toArray(new String[valueList.size()]);
  }

  private Map<String, List<String>> parameters = null;
  private Map<String, List<String>> getParameters()
  {
    if (parameters != null)
      return parameters;

    parameters = new HashMap<>();
    String encoding = getCharacterEncoding();
    if (encoding == null)
      encoding = StandardCharsets.UTF_8.name();
    String query = null;
    switch (httpExchange.getRequestMethod())
    {
    case "POST":
      if ("application/x-www-form-urlencoded".equals(contentTypeHeader))
      {
        try (Scanner scanner = new Scanner(httpExchange.getRequestBody(), encoding).useDelimiter("\\A"))
        {
          query = scanner.hasNext() ? scanner.next() : "";
        }
      }
    default:
      query = httpExchange.getRequestURI().getRawQuery();
      break;
    }
    if (query != null)
    {
      Arrays.stream(query.split("&"))
          .map(parameter ->
            {
              try
              {
                return URLDecoder.decode(parameter, StandardCharsets.UTF_8.name());
              }
              catch (UnsupportedEncodingException e)
              {
                throw new RuntimeException(e.getMessage(), e);
              }
            })
          .forEach(parameter ->
            {
              int equalsSignIndex = parameter.indexOf('=');
              String key = equalsSignIndex < 0 ? parameter : parameter.substring(0, equalsSignIndex);
              String value = equalsSignIndex < 0 ? null : parameter.substring(equalsSignIndex + 1);
              parameters.compute(key, (k, v) ->
                {
                  if (v == null)
                    v = new ArrayList<>();
                  v.add(value);
                  return v;
                });
            });
    }
    return parameters;
  }

  @Override
  public String getContentType()
  {
    return contentTypeHeader;
  }

  @Override
  public String getHeader(String name)
  {
    return httpExchange.getRequestHeaders().getFirst(name);
  }

  @Override
  public Object getMethod()
  {
    return httpExchange.getRequestMethod();
  }

  @Override
  public Cookie[] getCookies()
  {
    if (cookies == null)
    {
      List<String> cookie = httpExchange
          .getRequestHeaders()
          .get("Cookie");
      if (cookie == null)
        cookies = new Cookie[0];
      else
        cookies = cookie
            .stream()
            .flatMap(v -> Arrays.stream(v.split("; ")))
            .map(v ->
              {
                String[] parts = v.split("=", -1);
                return new HttpCookie(parts[0], parts[1]);
              }
            )
            .toArray(Cookie[]::new);
    }
    return cookies;
  }

  public void getMultiParts() throws Exception
  {
    BufferedInputStream inputStream = new BufferedInputStream(httpExchange.getRequestBody());

    String boundary = contentTypeHeader.substring(MULTIPART_MIME_TYPE.length());
    byte[] delimiter = ("--" + boundary).getBytes(StandardCharsets.UTF_8);
    byte[] fragment = getFragment(inputStream, delimiter);
    if (fragment == null)
      throw new IllegalArgumentException();
    delimiter = ("\r\n--" + boundary).getBytes(StandardCharsets.UTF_8);

    parts = new ArrayList<>();
    for (;;)
    {
      fragment = getFragment(inputStream, CR_LF);
      if (fragment == null)
        throw new IllegalArgumentException();
      if (Arrays.equals(fragment, MINUS_MINUS))
        break;
      if (fragment.length != 0)
        throw new IllegalArgumentException();
      Map<String, List<String>> partHeaders = collectHeaders(inputStream);
      fragment = getFragment(inputStream, delimiter);
      if (fragment == null)
        throw new IllegalArgumentException();
      parts.add(new HttpMultiPart(partHeaders, fragment));
    }

    fragment = getFragment(inputStream, CR_LF);
    if (fragment != null)
      throw new IllegalArgumentException();
  }

  private static Map<String, List<String>> collectHeaders(BufferedInputStream inputStream) throws Exception
  {
    byte[] fragment;
    Map<String, List<String>> headers = new LinkedHashMap<>();
    for (;;)
    {
      fragment = getFragment(inputStream, new byte[]{'\r', '\n'});
      if (fragment == null)
        throw new IllegalArgumentException();
      if (fragment.length == 0)
        break;
      String rawHeader = string(fragment);
      int index = rawHeader.indexOf(": ");
      if (index < 0)
        throw new IllegalArgumentException();
      headers.compute(rawHeader.substring(0, index), (k, v) ->
      {
        if (v == null)
          v = new ArrayList<>();
        v.add(rawHeader.substring(index + 2));
        return v;
      });
    }
    return headers;
  }

  private static String string(byte[] fragment)
  {
    return new String(fragment, StandardCharsets.UTF_8);
  }

  private static byte[] getFragment(BufferedInputStream inputStream, byte[] terminator) throws Exception
  {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    int nextMatch = 0;
    for (;;)
    {
      int nextByte = inputStream.read();
      if (nextByte == -1)
      {
        if (outputStream.toByteArray().length == 0)
          return null;
        else
          throw new IllegalArgumentException();
      }
      else if (nextByte == terminator[nextMatch])
      {
        if (++nextMatch == terminator.length)
          return outputStream.toByteArray();
        if (nextMatch == 1)
          inputStream.mark(terminator.length - 1);
      }
      else if (nextMatch == 0)
      {
        outputStream.write(nextByte);
      }
      else
      {
        outputStream.write(terminator[0]);
        inputStream.reset();
        nextMatch = 0;
      }
    }
  }

  @Override
  public String getPathInfo()
  {
    return httpExchange.getRequestURI().getPath();
  }

  @Override
  public String getRequestURI()
  {
    return httpExchange.getRequestURI().toString();
  }
}
