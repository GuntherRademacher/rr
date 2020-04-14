package de.bottlecaps.webapp;

import java.io.OutputStream;
import java.io.Writer;

public interface Response
{
  void setHeader(String name, String value);

  void setCharacterEncoding(String encoding);

  void setContentType(String contentType);

  OutputStream getOutputStream();

  Writer getWriter();

  void setStatus(int i);
}
