package de.bottlecaps.webapp.servlet;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

import javax.servlet.http.HttpServletResponse;

import de.bottlecaps.webapp.Response;

public class ServletResponse implements Response
{
  private HttpServletResponse httpServletResponse;

  public ServletResponse(HttpServletResponse httpServletResponse)
  {
    this.httpServletResponse = httpServletResponse;
  }

  @Override
  public void setCharacterEncoding(String encoding)
  {
    httpServletResponse.setCharacterEncoding(encoding);
  }

  @Override
  public void setContentType(String contentType)
  {
    httpServletResponse.setContentType(contentType);
  }

  @Override
  public OutputStream getOutputStream()
  {
    try
    {
      return httpServletResponse.getOutputStream();
    }
    catch (IOException e)
    {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Writer getWriter()
  {
    try
    {
      return httpServletResponse.getWriter();
    }
    catch (IOException e)
    {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void setHeader(String name, String value)
  {
    httpServletResponse.setHeader(name, value);
  }

  @Override
  public void setStatus(int status)
  {
    httpServletResponse.setStatus(status);
  }

}
