package de.bottlecaps.railroad.core;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public enum OutputOptions
{
  XHTML {
    @Override
    public Map<String, String> value() {
      return Map.of(
          "encoding", StandardCharsets.UTF_8.name(),
          "omit-xml-declaration", "yes",
          "indent", "yes",
          "method", "xhtml",
          "media-type", "application/xhtml+xml",
          "version", "1.0",
          "doctype-system", "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd",
          "doctype-public", "-//W3C//DTD XHTML 1.0 Transitional//EN");
    }
  },
  HTML {
    @Override
    public Map<String, String> value() {
      return Map.of(
          "encoding", StandardCharsets.UTF_8.name(),
          "omit-xml-declaration", "yes",
          "indent", "yes",
          "method", "html",
          "media-type", "text/html",
          "version", "4.01",
          "doctype-system", "http://www.w3.org/TR/html4/loose.dtd",
          "doctype-public", "-//W3C//DTD HTML 4.01 Transitional//EN");
    }
  },
  XML {
    @Override
    public Map<String, String> value() {
      return Map.of(
          "encoding", StandardCharsets.UTF_8.name(),
          "omit-xml-declaration", "no",
          "media-type", "text/xml",
          "indent", "yes");
    }
  },
  TEXT {
    @Override
    public Map<String, String> value() {
      return Map.of(
          "encoding", StandardCharsets.UTF_8.name(),
          "method", "text");
    }
  };

  public abstract Map<String, String> value();

}
