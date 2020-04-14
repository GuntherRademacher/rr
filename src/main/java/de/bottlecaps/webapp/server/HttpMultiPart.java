package de.bottlecaps.webapp.server;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import de.bottlecaps.webapp.MultiPart;

// TODO: header names case-insensitive

public class HttpMultiPart implements MultiPart
{
  private static final String CONTENT_DISPOSITION = "Content-Disposition";
  private static final String FORM_DATA = "form-data; ";
  private static final String NAME = "name=";

  private Map<String, List<String>> headers;
  private String name;
  private byte[] value;

  /*
   * Content-Disposition: form-data; name="file"; filename=""
   * Content-Type: application/octet-stream
   */

  public HttpMultiPart(Map<String, List<String>> headers, byte[] value) throws UnsupportedEncodingException
  {
    this.headers = headers;
    this.value = value;

    List<String> contentDispositionHeaders = headers.get(CONTENT_DISPOSITION);
    if (contentDispositionHeaders == null || contentDispositionHeaders.size() != 1)
      throw new IllegalArgumentException();
    String contentDispositionHeader = contentDispositionHeaders.get(0);
    if (! contentDispositionHeader.startsWith(FORM_DATA))
      throw new IllegalArgumentException();
    for (String attribute : contentDispositionHeader.split("; "))
    {
      if (attribute.startsWith(NAME))
      {
        if (name != null)
          throw new IllegalArgumentException();
        name = URLDecoder.decode(attribute.substring(NAME.length()).replaceAll("^\"([^\"]*)\"$", "$1"), StandardCharsets.UTF_8.name());
      }
    }
    if (name == null)
      throw new IllegalArgumentException();
  }

  @Override
  public String getName()
  {
    return name;
  }

  @Override
  public InputStream getInputStream() throws IOException
  {
    return new ByteArrayInputStream(value);
  }

  @Override
  public String getHeader(String name)
  {
    List<String> header = headers.get(name);
    return header == null
        ? null
        : header.get(0);
  }
}
