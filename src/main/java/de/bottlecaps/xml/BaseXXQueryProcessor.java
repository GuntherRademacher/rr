package de.bottlecaps.xml;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.Map;

import org.basex.core.Context;
import org.basex.io.IO;
import org.basex.io.serial.Serializer;
import org.basex.io.serial.SerializerOptions;
import org.basex.query.QueryException;
import org.basex.query.QueryProcessor;
import org.basex.query.iter.BasicIter;
import org.basex.query.value.Value;
import org.basex.query.value.item.Item;
import org.basex.query.value.node.DBNode;
import org.basex.util.options.EnumOption;
import org.basex.util.options.Option;
import org.basex.util.options.StringOption;

public enum BaseXXQueryProcessor implements XQueryProcessor {
  instance;

  private static Context context = new Context();

  public static class BaseXResult implements Result {
    private Value result;

    public BaseXResult(Value result) {
      this.result = result;
    }

    @Override
    public Object getResultObject() {
      return result;
    }

    @Override
    public void serialize(OutputStream outputStream, Map<String, String> outputOptions) throws Exception {
      try(Serializer serializer = serializer(outputStream, outputOptions)) {
        BasicIter<Item> iter = result.iter();
        for(Item item; (item = iter.next()) != null;)
          serializer.serialize(item);
      }
    }
  }

  public static class BaseXPlan implements Plan {
    private String query;

    public BaseXPlan(String query) {
      this.query = query;
    }

    @Override
    public Result evaluate(Map<String, Object> externalVars) throws Exception {
      try (QueryProcessor queryProcessor = new QueryProcessor(query, context)) {
        bindExternalVariables(queryProcessor, externalVars);
        return new BaseXResult(queryProcessor.iter().value(queryProcessor.qc, null));
      }
    }
  }

  @Override
  public Plan compile(String query) throws Exception {
    return new BaseXPlan(query);
  }

  private static void bindExternalVariables(QueryProcessor proc, Map<String, Object> externalVars) throws QueryException {
    for (Map.Entry<String, Object> externalVar : externalVars.entrySet()) {
      Object value = externalVar.getValue();
      String name = externalVar.getKey();
      if (name.startsWith("{"))
        name = "Q" + name;
      if (value instanceof BaseXResult)
        proc.variable(name, ((BaseXResult) value).result);
      else
        proc.variable(name, value);
    }
  }

  private static Serializer serializer(OutputStream outputStream, Map<String, String> outputOptions) throws IOException {
    return Serializer.get(outputStream, serializerOptions(outputOptions));
  }

  private static SerializerOptions serializerOptions(Map<String, String> outputOptions) {
    SerializerOptions serializerOptions = new SerializerOptions();
    serializerOptions.set(SerializerOptions.NEWLINE, SerializerOptions.Newline.NL);
    outputOptions.forEach((key, value) -> {
      Option<?> option = null;
      for (Field field : SerializerOptions.class.getDeclaredFields()) {
        if (Option.class.isAssignableFrom(field.getType())) {
          try {
            option = (Option<?>) field.get(null);
            if (key.equals(option.name()))
              break;
            option = null;
          }
          catch (IllegalArgumentException | IllegalAccessException e) {
            throw new RuntimeException(e.getMessage(), e);
          }
        }
      }
      if (option == null)
        throw new IllegalArgumentException("unrecognized option: " + key);
      if (option instanceof EnumOption<?>)
        serializerOptions.put(option, ((EnumOption<?>) option).get(value));
      else if (option instanceof StringOption)
        serializerOptions.put(option, value);
      else
        System.err.println("unsupported option: " + key);
    });
    return serializerOptions;
  }

  @Override
  public Result parseXml(String xml) throws Exception {
    return new BaseXResult(new DBNode(IO.get(xml)));
  }

}
