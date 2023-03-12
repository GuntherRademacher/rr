package de.bottlecaps.xml;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.xml.transform.stream.StreamSource;

import de.bottlecaps.railroad.core.ExtensionFunctions;
import de.bottlecaps.railroad.core.Parser;
import de.bottlecaps.webapp.Request;
import de.bottlecaps.webapp.Response;
import net.sf.saxon.Configuration;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.lib.ExtensionFunctionCall;
import net.sf.saxon.lib.ExtensionFunctionDefinition;
import net.sf.saxon.lib.Initializer;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.s9api.Axis;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.BooleanValue;
import net.sf.saxon.value.EmptySequence;
import net.sf.saxon.value.Int64Value;
import net.sf.saxon.value.ObjectValue;
import net.sf.saxon.value.SequenceExtent;
import net.sf.saxon.value.SequenceType;
import net.sf.saxon.value.StringValue;

public class SaxonFunctions extends ExtensionFunctions implements Initializer
{
  private abstract static class ExtensionFunction extends ExtensionFunctionDefinition
  {
    private String name;
    private SequenceType[] argumentTypes;
    private SequenceType resultType;
    private ExtensionFunctionCall call;

    public ExtensionFunction(String name, SequenceType[] argumentTypes, SequenceType resultType, ExtensionFunctionCall call)
    {
      this.name = name;
      this.argumentTypes = argumentTypes;
      this.resultType = resultType;
      this.call = call;
    }

    @Override public SequenceType[] getArgumentTypes() {return argumentTypes;}
    @Override public SequenceType getResultType(SequenceType[] arg0) {return resultType;}
    @Override public ExtensionFunctionCall makeCallExpression() {return call;}

    @Override
    public StructuredQName getFunctionQName()
    {
      javax.xml.namespace.QName qName = javax.xml.namespace.QName.valueOf(name);
      String namespace = qName.getNamespaceURI();
      String[] steps = namespace.replaceAll("/$", "").split("/");
      return new StructuredQName(steps[steps.length - 1], namespace, qName.getLocalPart());
    }
  }

  @Override
  public void initialize(Configuration configuration)
  {
    new Parser.SaxonInitializer().initialize(configuration);

    for (Class<?> declaredClass : getClass().getDeclaredClasses())
    {
      if (ExtensionFunctionDefinition.class.isAssignableFrom(declaredClass) && ! Modifier.isAbstract(declaredClass.getModifiers()))
      {
        try
        {
          configuration.registerExtensionFunction((ExtensionFunctionDefinition) declaredClass.getDeclaredConstructor().newInstance());
        }
        catch (IllegalAccessException | InstantiationException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e)
        {
          throw new RuntimeException(e.getMessage(), e);
        }
      }
    }
  }

  public static class SetSerializationParameter extends ExtensionFunction
  {
    public SetSerializationParameter()
    {
      super
      (
        "{de/bottlecaps/railroad/core/ExtensionFunctions}set-serialization-parameter",
        new SequenceType[] {SequenceType.SINGLE_ITEM, SequenceType.SINGLE_STRING, SequenceType.SINGLE_STRING},
        SequenceType.ANY_SEQUENCE,
        new ExtensionFunctionCall()
        {
          @Override
          public Sequence call(XPathContext context, Sequence[] arguments) throws XPathException
          {
            @SuppressWarnings("unchecked")
            Map<String, String> outputOptions = ((ObjectValue<Map<String, String>>) arguments[0].head()).getObject();
            String name = arguments[1].head().getStringValue();
            String value = arguments[2].head().getStringValue();
            setSerializationParameter(outputOptions, name, value);
            return EmptySequence.getInstance();
          }
        }
      );
    }
  }

  public static class DecodeBase64 extends ExtensionFunction
  {
    public DecodeBase64()
    {
      super
      (
        "{de/bottlecaps/railroad/core/ExtensionFunctions}decode-base64",
        new SequenceType[] {SequenceType.SINGLE_STRING},
        SequenceType.SINGLE_STRING,
        new ExtensionFunctionCall()
        {
          @Override
          public Sequence call(XPathContext context, Sequence[] arguments) throws XPathException
          {
            String base64 = arguments[0].head().getStringValue();
            return new StringValue(decodeBase64(base64));
          }
        }
      );
    }
  }

  public static class BinaryFile extends ExtensionFunction
  {
    public BinaryFile()
    {
      super
      (
        "{de/bottlecaps/railroad/core/ExtensionFunctions}binary-file",
        new SequenceType[] {SequenceType.SINGLE_ITEM, SequenceType.SINGLE_STRING},
        SequenceType.OPTIONAL_STRING,
        new ExtensionFunctionCall()
        {
          @Override
          public Sequence call(XPathContext context, Sequence[] arguments) throws XPathException
          {
            @SuppressWarnings("unchecked")
            Request request = ((ObjectValue<Request>) arguments[0].head()).getObject();
            String fileName = arguments[1].head().getStringValue();
            String result = binaryFile(request, fileName);
            return result == null
                ? EmptySequence.getInstance()
                : new StringValue(result);
          }
        }
      );
    }
  }

  public static class PartFilename extends ExtensionFunction
  {
    public PartFilename()
    {
      super
      (
        "{de/bottlecaps/railroad/core/ExtensionFunctions}part-filename",
        new SequenceType[] {SequenceType.SINGLE_ITEM, SequenceType.SINGLE_STRING},
        SequenceType.OPTIONAL_STRING,
        new ExtensionFunctionCall()
        {
          @Override
          public Sequence call(XPathContext context, Sequence[] arguments) throws XPathException
          {
            try
            {
              @SuppressWarnings("unchecked")
              Request request = ((ObjectValue<Request>) arguments[0].head()).getObject();
              String partName = arguments[1].head().getStringValue();
              String partFilename = partFilename(request, partName);
              return partFilename == null
                  ? EmptySequence.getInstance()
                  : new StringValue(partFilename);
            }
            catch (RuntimeException e)
            {
              throw e;
            }
            catch (Exception e)
            {
              throw new RuntimeException(e);
            }
          }
        }
      );
    }
  }

  public static class PartNames extends ExtensionFunction
  {
    public PartNames()
    {
      super
      (
        "{de/bottlecaps/railroad/core/ExtensionFunctions}part-names",
        new SequenceType[] {SequenceType.SINGLE_ITEM},
        SequenceType.STRING_SEQUENCE,
        new ExtensionFunctionCall()
        {
          @Override
          public Sequence call(XPathContext context, Sequence[] arguments) throws XPathException
          {
            try
            {
              @SuppressWarnings("unchecked")
              Request request = ((ObjectValue<Request>) arguments[0].head()).getObject();
              List<StringValue> stringValues = Arrays.stream(partNames(request))
                  .map(StringValue::new)
                  .collect(Collectors.toList());
              return new SequenceExtent.Of<>(stringValues);
            }
            catch (IOException e)
            {
              throw new RuntimeException(e);
            }

          }
        }
      );
    }
  }

  public static class ParameterNames extends ExtensionFunction
  {
    public ParameterNames()
    {
      super
      (
        "{de/bottlecaps/railroad/core/ExtensionFunctions}parameter-names",
        new SequenceType[] {SequenceType.SINGLE_ITEM},
        SequenceType.STRING_SEQUENCE,
        new ExtensionFunctionCall()
        {
          @Override
          public Sequence call(XPathContext context, Sequence[] arguments) throws XPathException
          {
            @SuppressWarnings("unchecked")
            Request request = ((ObjectValue<Request>) arguments[0].head()).getObject();
            List<StringValue> stringValues = Arrays.stream(parameterNames(request))
                .map(StringValue::new)
                .collect(Collectors.toList());
            return new SequenceExtent.Of<>(stringValues);
          }

        }
      );
    }
  }

  public static class SendRequest extends ExtensionFunction
  {
    private static final String HTTPCLIENT_NAMESPACE = "http://expath.org/ns/http-client";
    private static final String RESPONSE = "response";

    SendRequest()
    {
      super
      (
        "{" + HTTPCLIENT_NAMESPACE + "}send-request",
        new SequenceType[] {SequenceType.EMPTY_SEQUENCE, SequenceType.SINGLE_STRING},
        SequenceType.ANY_SEQUENCE,
        new ExtensionFunctionCall()
        {
          @Override
          public Sequence call(XPathContext context, Sequence[] arguments) throws XPathException
          {
            List<Item> result = new ArrayList<>();
            try
            {
              String url = arguments[1].head().getStringValue();
              URLConnection connection = new URL(url).openConnection();
              int responseCode = connection instanceof HttpURLConnection ? ((HttpURLConnection) connection).getResponseCode() : 200;
              String resp = "<" + RESPONSE + " xmlns=\"" + HTTPCLIENT_NAMESPACE + "\" status=\"" + responseCode + "\"/>";
              result.add(SaxonXQueryProcessor.instance.parseXml(resp, "text/xml").iterateAxis(Axis.CHILD.getAxisNumber()).next());
              InputStream content = (InputStream) connection.getContent();
              String contentType = connection.getContentType();
              if (SaxonXQueryProcessor.isHtml(contentType) || SaxonXQueryProcessor.isXml(contentType))
              {
                StreamSource source = new StreamSource(content);
                source.setSystemId(url);
                result.add(SaxonXQueryProcessor.instance.parseXml(source, contentType));
              }
              else
              {
                String value = ExtensionFunctions.toString(content, connection.getContentEncoding());
                result.add(new StringValue(value));
              }
            }
            catch (IOException e)
            {
              result.add(new StringValue(e.getMessage()));
            }
            return new SequenceExtent.Of<>(result);
          }
        }
      );
    }
  }

  public static class MethodGet extends ExtensionFunction
  {
    public MethodGet()
    {
      super
      (
        "{de/bottlecaps/railroad/core/ExtensionFunctions}method-get",
        new SequenceType[] {SequenceType.SINGLE_ITEM},
        SequenceType.SINGLE_BOOLEAN,
        new ExtensionFunctionCall()
        {
          @Override
          public Sequence call(XPathContext context, Sequence[] arguments) throws XPathException
          {
            @SuppressWarnings("unchecked")
            Request request = ((ObjectValue<Request>) arguments[0].head()).getObject();
            return BooleanValue.get(methodGet(request));
          }
        }
      );
    }
  }

  public static class SetHeader extends ExtensionFunction
  {
    public SetHeader()
    {
      super
      (
        "{de/bottlecaps/railroad/core/ExtensionFunctions}set-header",
        new SequenceType[] {SequenceType.SINGLE_ITEM, SequenceType.SINGLE_STRING, SequenceType.SINGLE_STRING},
        SequenceType.ANY_SEQUENCE,
        new ExtensionFunctionCall()
        {
          @Override
          public Sequence call(XPathContext context, Sequence[] arguments) throws XPathException
          {
            @SuppressWarnings("unchecked")
            Response response = ((ObjectValue<Response>) arguments[0].head()).getObject();
            String name = arguments[1].head().getStringValue();
            String value = arguments[2].head().getStringValue();
            setHeader(response, name, value);
            return EmptySequence.getInstance();
          }
        }
      );
    }
  }

  public static class UserAgent extends ExtensionFunction
  {
    public UserAgent()
    {
      super
      (
        "{de/bottlecaps/railroad/core/ExtensionFunctions}user-agent",
        new SequenceType[] {SequenceType.SINGLE_ITEM},
        SequenceType.OPTIONAL_STRING,
        new ExtensionFunctionCall()
        {
          @Override
          public Sequence call(XPathContext context, Sequence[] arguments) throws XPathException
          {
            @SuppressWarnings("unchecked")
            Request request = ((ObjectValue<Request>) arguments[0].head()).getObject();
            String userAgent = userAgent(request);
            return userAgent == null
                 ? EmptySequence.getInstance()
                 : new StringValue(userAgent);
          }
        }
      );
    }
  }

  public static class XfXhtmlToZip extends ExtensionFunction
  {
    public XfXhtmlToZip()
    {
      super
      (
        "{de/bottlecaps/railroad/core/ExtensionFunctions}xhtml-to-zip",
        new SequenceType[]{SequenceType.SINGLE_STRING, SequenceType.SINGLE_STRING, SequenceType.SINGLE_STRING},
        SequenceType.SINGLE_STRING,
        new ExtensionFunctionCall()
        {
          @Override
          public Sequence call(XPathContext context, Sequence[] arguments) throws XPathException
          {
            String xhtml = arguments[0].head().getStringValue();
            String textFormat = arguments[1].head().getStringValue();
            String graphicsFormat = arguments[2].head().getStringValue();
            return new StringValue(xhtmlToZip(xhtml, textFormat, graphicsFormat));
          }
        }
      );
    }
  }

  public static class ParseXml extends ExtensionFunction
  {
    public ParseXml()
    {
      super
      (
        "{de/bottlecaps/railroad/core/ExtensionFunctions}parse-xml",
        new SequenceType[]{SequenceType.SINGLE_STRING},
        SequenceType.SINGLE_NODE,
        new ExtensionFunctionCall()
        {
          @Override
          public Sequence call(XPathContext context, Sequence[] arguments) throws XPathException
          {
            try
            {
              String string = arguments[0].head().getStringValue();
              return SaxonXQueryProcessor.instance.parseXml(string, "text/xml");
            }
            catch (RuntimeException e)
            {
              throw e;
            }
            catch (Exception e)
            {
              throw new RuntimeException(e);
            }
          }
        }
       );
    }
  }

  public static class GetCookie extends ExtensionFunction
  {
    public GetCookie()
    {
      super
      (
        "{de/bottlecaps/railroad/core/ExtensionFunctions}get-cookie",
        new SequenceType[]{SequenceType.SINGLE_ITEM, SequenceType.SINGLE_STRING},
        SequenceType.OPTIONAL_STRING,
        new ExtensionFunctionCall()
        {
          @Override
          public Sequence call(XPathContext context, Sequence[] arguments) throws XPathException
          {
            @SuppressWarnings("unchecked")
            Request request = ((ObjectValue<Request>) arguments[0].head()).getObject();
            String name = arguments[1].head().getStringValue();
            try
            {
              String cookie = getCookie(request, name);
              return cookie == null
                  ? EmptySequence.getInstance()
                  : new StringValue(cookie);
            }
            catch (IOException e)
            {
              throw new RuntimeException(e.getMessage(), e);
            }
          }
        }
      );
    }
  }

  public static class ParameterValues extends ExtensionFunction
  {
    public ParameterValues()
    {
      super
      (
        "{de/bottlecaps/railroad/core/ExtensionFunctions}parameter-values",
        new SequenceType[]{SequenceType.SINGLE_ITEM, SequenceType.STRING_SEQUENCE},
        SequenceType.STRING_SEQUENCE,
        new ExtensionFunctionCall()
        {
          @Override
          public Sequence call(XPathContext context, Sequence[] arguments) throws XPathException
          {
            @SuppressWarnings("unchecked")
            Request httpRequest = ((ObjectValue<Request>) arguments[0].head()).getObject();
            String name = arguments[1].head().getStringValue();
            String[] result = parameterValues(httpRequest, name);
            List<StringValue> stringValues = Arrays.stream(result)
                .map(StringValue::new)
                .collect(Collectors.toList());
            return new SequenceExtent.Of<>(stringValues);
          }
        }
      );
    }
  }

  public static class RequestUri extends ExtensionFunction
  {
    public RequestUri()
    {
      super
      (
        "{de/bottlecaps/railroad/core/ExtensionFunctions}request-uri",
        new SequenceType[]{SequenceType.SINGLE_ITEM},
        SequenceType.SINGLE_STRING,
        new ExtensionFunctionCall()
        {
          @Override
          public Sequence call(XPathContext context, Sequence[] arguments) throws XPathException
          {
            @SuppressWarnings("unchecked")
            Request httpRequest = ((ObjectValue<Request>) arguments[0].head()).getObject();
            return new StringValue(requestUri(httpRequest));
          }
        }
      );
    }
  }

  public static class Normal extends ExtensionFunction
  {
    public Normal()
    {
      super
      (
        "{de/bottlecaps/railroad/core/ExtensionFunctions}text-width-normal",
        new SequenceType[]{SequenceType.SINGLE_STRING},
        SequenceType.SINGLE_INTEGER,
        new ExtensionFunctionCall()
        {
          @Override
          public Sequence call(XPathContext context, Sequence[] arguments) throws XPathException
          {
            try
            {
              String input = arguments[0].head().getStringValue();
              int result = textWidthNormal(input);
              return Int64Value.makeIntegerValue(result);
            }
            catch (RuntimeException e)
            {
              throw e;
            }
            catch (Exception e)
            {
              throw new RuntimeException(e);
            }
          }
        }
      );
    }
  }

  public static class Bold extends ExtensionFunction
  {
    public Bold()
    {
      super
      (
        "{de/bottlecaps/railroad/core/ExtensionFunctions}text-width-bold",
        new SequenceType[]{SequenceType.SINGLE_STRING},
        SequenceType.SINGLE_INTEGER,
        new ExtensionFunctionCall()
        {
          @Override
          public Sequence call(XPathContext context, Sequence[] arguments) throws XPathException
          {
            try
            {
              String input = arguments[0].head().getStringValue();
              int result = textWidthBold(input);
              return Int64Value.makeIntegerValue(result);
            }
            catch (RuntimeException e)
            {
              throw e;
            }
            catch (Exception e)
            {
              throw new RuntimeException(e);
            }
          }
        }
      );
    }
  }

  public static class UnicodeClass extends ExtensionFunction
  {
    public UnicodeClass()
    {
      super
      (
        "{de/bottlecaps/railroad/core/ExtensionFunctions}unicode-class",
        new SequenceType[]{SequenceType.SINGLE_STRING},
        SequenceType.SINGLE_NODE,
        new ExtensionFunctionCall()
        {
          @Override
          public Sequence call(XPathContext context, Sequence[] arguments) throws XPathException
          {
            try
            {
              String className = arguments[0].head().getStringValue();
              String serializedXML = unicodeClassToSerializedXML(className);
              return SaxonXQueryProcessor.instance.parseXml(serializedXML, "text/xml");
            }
            catch (RuntimeException e)
            {
              throw e;
            }
            catch (Exception e)
            {
              throw new RuntimeException(e);
            }
          }
        }
      );
    }
  }

  public static class SvgToPng extends ExtensionFunction
  {
    public SvgToPng()
    {
      super
      (
        "{de/bottlecaps/railroad/core/ExtensionFunctions}svg-to-png",
        new SequenceType[]{SequenceType.SINGLE_STRING},
        SequenceType.SINGLE_STRING,
        new ExtensionFunctionCall()
        {
          @Override
          public Sequence call(XPathContext context, Sequence[] arguments) throws XPathException
          {
            try
            {
              String svg = arguments[0].head().getStringValue();
              return new StringValue(svgToPng(svg));
            }
            catch (RuntimeException e)
            {
              throw e;
            }
            catch (Exception e)
            {
              throw new RuntimeException(e);
            }
          }
        }
      );
    }
  }

}
