package de.bottlecaps.railroad.core;
import java.io.InputStream;

import javax.xml.transform.stream.StreamSource;

import net.sf.saxon.lib.ModuleURIResolver;
import net.sf.saxon.trans.XPathException;

public enum ResourceModuleUriResolver implements ModuleURIResolver
{
  instance;

  @Override
  public StreamSource[] resolve(String moduleURI, String baseURI, String[] locations) throws XPathException
  {
    try
    {
      InputStream resource = Thread.currentThread().getContextClassLoader().getResourceAsStream(moduleURI);
      if (resource != null)
      {
        return new StreamSource[]{new StreamSource(new CrLfNormalizer(resource), moduleURI)};
      }
    }
    catch (Exception e)
    {
    }
    return null;
  }
}
