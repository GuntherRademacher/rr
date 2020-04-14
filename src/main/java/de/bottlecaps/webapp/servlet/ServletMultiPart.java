package de.bottlecaps.webapp.servlet;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.http.Part;

import de.bottlecaps.webapp.MultiPart;

public class ServletMultiPart implements MultiPart
{
  private Part part;

  public ServletMultiPart(Part part)
  {
    this.part = part;
  }

  @Override
  public String getName()
  {
    return part.getName();
  }

  @Override
  public InputStream getInputStream() throws IOException
  {
    return part.getInputStream();
  }

  @Override
  public String getHeader(String name)
  {
    return part.getHeader(name);
  }

}
