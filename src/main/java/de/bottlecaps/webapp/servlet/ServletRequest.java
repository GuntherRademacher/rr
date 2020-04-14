package de.bottlecaps.webapp.servlet;

import java.io.IOException;
import java.util.AbstractCollection;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;

import de.bottlecaps.webapp.Cookie;
import de.bottlecaps.webapp.MultiPart;
import de.bottlecaps.webapp.Request;

public class ServletRequest implements Request
{
  private HttpServletRequest httpServletRequest;

  public ServletRequest(HttpServletRequest httpServletRequest)
  {
    this.httpServletRequest = httpServletRequest;
  }

  @Override
  public Collection<MultiPart> getParts() throws IOException
  {
    try
    {
      Collection<Part> parts = httpServletRequest.getParts();
      return new AbstractCollection<MultiPart>() {

        @Override
        public int size()
        {
          return parts.size();
        }

        @Override
        public Iterator<MultiPart> iterator()
        {
          Iterator<Part> iterator = parts.iterator();
          return new Iterator<MultiPart>() {

            @Override
            public boolean hasNext()
            {
              return iterator.hasNext();
            }

            @Override
            public MultiPart next()
            {
              return new ServletMultiPart(iterator.next());
            }
          };
        }
      };
    }
    catch (ServletException e)
    {
      throw new RuntimeException(e);
    }
  }

  @Override
  public String getContextPath()
  {
    return httpServletRequest.getContextPath();
  }

  @Override
  public String getCharacterEncoding()
  {
    return httpServletRequest.getCharacterEncoding();
  }

  @Override
  public String[] getParameterValues(String name)
  {
    return httpServletRequest.getParameterValues(name);
  }

  @Override
  public String getContentType()
  {
    return httpServletRequest.getContentType();
  }

  @Override
  public MultiPart getPart(String partName) throws IOException
  {
    try
    {
      return new ServletMultiPart(httpServletRequest.getPart(partName));
    }
    catch (ServletException e)
    {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Enumeration<String> getParameterNames()
  {
    return httpServletRequest.getParameterNames();
  }

  @Override
  public String getHeader(String name)
  {
    return httpServletRequest.getHeader(name);
  }

  @Override
  public String getMethod()
  {
    return httpServletRequest.getMethod();
  }

  @Override
  public Cookie[] getCookies()
  {
    javax.servlet.http.Cookie[] cookies = httpServletRequest.getCookies();
    return cookies == null
        ? new Cookie[0]
        : Arrays.stream(cookies)
          .map(ServletCookie::new)
          .toArray(Cookie[]::new);
  }

  @Override
  public String getPathInfo()
  {
    return httpServletRequest.getPathInfo();
  }

  @Override
  public String getRequestURI()
  {
    return httpServletRequest.getRequestURI();
  }

}
