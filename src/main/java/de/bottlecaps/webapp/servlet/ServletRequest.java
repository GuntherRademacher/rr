package de.bottlecaps.webapp.servlet;

import java.io.IOException;
import java.util.AbstractCollection;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Locale;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;

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

  private boolean isMultipart()
  {
    String contentType = httpServletRequest.getContentType();
    return contentType != null && contentType.toLowerCase(Locale.ROOT).startsWith("multipart/");
  }

  @Override
  public Collection<MultiPart> getParts() throws IOException
  {
    // Servlet 6 containers throw ServletException for non-multipart requests,
    // e.g. HEAD or urlencoded POST, where there simply are no parts.
    if (! isMultipart())
      return Collections.emptyList();

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
    if (! isMultipart())
      return null;

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
    jakarta.servlet.http.Cookie[] cookies = httpServletRequest.getCookies();
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
