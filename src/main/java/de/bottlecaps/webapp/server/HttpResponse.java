package de.bottlecaps.webapp.server;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

import com.sun.net.httpserver.HttpExchange;

import de.bottlecaps.webapp.Response;

@SuppressWarnings("all")
public class HttpResponse implements Response
{
  private HttpExchange httpExchange;
  private String contentType;
  private String encoding;
  private OutputStream outputStream;
  private int status = 200;

  public HttpResponse(HttpExchange httpExchange)
  {
    this.httpExchange = httpExchange;
  }

  @Override
  public void setCharacterEncoding(String encoding)
  {
    this.encoding = encoding;
  }

  @Override
  public void setContentType(String contentType)
  {
    this.contentType = contentType;
  }

  @Override
  public OutputStream getOutputStream()
  {
    if (outputStream == null)
    {
      boolean isHeadRequest = httpExchange.getRequestMethod().equals("HEAD");
      try
      {
        if (contentType != null)
        {
          String value = contentType;
          if (encoding != null)
            value += "; charset=" + encoding;
          httpExchange.getResponseHeaders().set("Content-Type", value);
        }
        int contentLength = isHeadRequest ? -1 : 0;
        httpExchange.sendResponseHeaders(status, contentLength);
      }
      catch (IOException e)
      {
        e.printStackTrace(System.err);
        throw new RuntimeException(e.getMessage(), e);
      }
      outputStream = ! isHeadRequest
          ? httpExchange.getResponseBody()
          : new OutputStream()
              {
                @Override
                public void write(int b) throws IOException
                {
                }
              };
    }
    return outputStream;
  }

  @Override
  public Writer getWriter()
  {
    try
    {
      return new OutputStreamWriter(getOutputStream(), StandardCharsets.UTF_8);
    }
    catch (Exception e)
    {
      e.printStackTrace(System.err);
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  @Override
  public void setHeader(String name, String value)
  {
    httpExchange.getResponseHeaders().set(name, value);
  }

  @Override
  public void setStatus(int status)
  {
    this.status = status;
  }

}
