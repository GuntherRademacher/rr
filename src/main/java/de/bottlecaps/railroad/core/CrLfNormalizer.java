package de.bottlecaps.railroad.core;
import java.io.IOException;
import java.io.InputStream;


public class CrLfNormalizer extends InputStream
{
  private InputStream input;
  private int delayed = -1;

  public CrLfNormalizer(InputStream input)
  {
    this.input = input;
  }

  @Override
  public int read() throws IOException
  {
    int current;
    if (delayed < 0)
    {
      current = input.read();
    }
    else
    {
      current = delayed;
      delayed = -1;
    }
    if (current == 0xD)
    {
      current = input.read();
      if (current != 0xA)
      {
        delayed = current;
        return 0xD;
      }
    }
    return current;
  }

  public static String normalize(String s) {
    if (s.contains("\r"))
      s = s.replaceAll("\r\n", "\n");
    return s;
  }
}