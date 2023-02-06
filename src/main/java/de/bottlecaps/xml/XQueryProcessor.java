package de.bottlecaps.xml;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public interface XQueryProcessor {

  public static XQueryProcessor defaultXQueryProcessor() {
    return Saxon.instance;
  }

  public interface Result {
    Object getResultObject();
    void serialize(OutputStream outputStream, Map<String, String> outputOptions) throws Exception;

    public default String serializeToString(Map<String, String> outputOptions) throws Exception {
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      serialize(outputStream, outputOptions);
      return outputStream.toString(StandardCharsets.UTF_8);
    }
  }

  public interface Plan {
    Result evaluate(Map<String, Object> externalVars) throws Exception;
  }

  public Plan compile(String query) throws Exception;

  public Result parseXml(String xml) throws Exception;
}