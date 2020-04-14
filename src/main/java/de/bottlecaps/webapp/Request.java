package de.bottlecaps.webapp;

import java.io.IOException;
import java.util.Collection;
import java.util.Enumeration;

public interface Request
{
  String getContextPath();

  Collection<MultiPart> getParts() throws IOException;

  MultiPart getPart(String partName) throws IOException;

  String getCharacterEncoding();

  Enumeration<String> getParameterNames();

  String[] getParameterValues(String name);

  String getContentType();

  String getHeader(String name);

  Object getMethod();

  Cookie[] getCookies();

  String getPathInfo();

  String getRequestURI();
}
