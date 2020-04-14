package de.bottlecaps.webapp;

import java.io.IOException;
import java.io.InputStream;

public interface MultiPart
{

  String getName();

  InputStream getInputStream() throws IOException;

  String getHeader(String string);

}
