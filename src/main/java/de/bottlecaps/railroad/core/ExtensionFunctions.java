package de.bottlecaps.railroad.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Scanner;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;

import de.bottlecaps.webapp.Cookie;
import de.bottlecaps.webapp.MultiPart;
import de.bottlecaps.webapp.Request;
import de.bottlecaps.webapp.Response;
import net.sf.saxon.Configuration;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.lib.ExtensionFunctionCall;
import net.sf.saxon.lib.ExtensionFunctionDefinition;
import net.sf.saxon.lib.Initializer;
import net.sf.saxon.lib.ParseOptions;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.s9api.Axis;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.Serializer;
import net.sf.saxon.s9api.XQueryEvaluator;
import net.sf.saxon.s9api.XdmValue;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.AxisIterator;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.BooleanValue;
import net.sf.saxon.value.EmptySequence;
import net.sf.saxon.value.ObjectValue;
import net.sf.saxon.value.SequenceExtent;
import net.sf.saxon.value.SequenceType;
import net.sf.saxon.value.StringValue;

public class ExtensionFunctions implements Initializer
{
  private static final String UTF_8 = "UTF-8";

  public static class GlobalVariable<T>
  {
    private StructuredQName structuredQName;
    private QName qname;

    public GlobalVariable(Class<T> clazz)
    {
      structuredQName = new StructuredQName("", "", clazz.getName());
      qname = new QName(structuredQName);
    }

    public T get(XPathContext context)
    {
      try
      {
        @SuppressWarnings("unchecked")
        ObjectValue<T> parameter = (ObjectValue<T>) context.getController().getParameter(structuredQName);
        return parameter.getObject();
      }
      catch (RuntimeException e)
      {
        throw e;
      }
      catch (Exception e)
      {
        throw new RuntimeException("failed with exception", e);
      }
    }

    public void set(T value, XQueryEvaluator evaluator)
    {
      evaluator.setExternalVariable(qname, XdmValue.wrap(new ObjectValue<>(value)));
    }
  }

  public static final GlobalVariable<Request> request = new GlobalVariable<>(Request.class);
  public static final GlobalVariable<Response> response = new GlobalVariable<>(Response.class);
  public static final GlobalVariable<Serializer> serializer = new GlobalVariable<>(Serializer.class);

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
    configuration.registerExtensionFunction(new Parser.SaxonDefinition_Grammar());
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

  public static List<String> parameterValues(Request httpRequest, String name)
  {
    List<String> parameterList = new ArrayList<>();
    if (isMultipart(httpRequest))
    {
      try
      {
        Collection<MultiPart> parts = httpRequest.getParts();
        if (parts != null)
        {
          for (MultiPart part : parts)
          {
            if (name.equals(part.getName()))
            {
              String value = ExtensionFunctions.toString(new CrLfNormalizer(part.getInputStream()), httpRequest.getCharacterEncoding());
              parameterList.add(value);
            }
          }
        }
      }
      catch (IOException e)
      {
        throw new RuntimeException(e);
      }
    }
    else
    {
      String[] parameterValues = httpRequest.getParameterValues(name);
      if (parameterValues != null)
      {
        for (String pv : parameterValues)
        {
          parameterList.add(CrLfNormalizer.normalize(pv));
        }
      }
    }
    return parameterList;
  }

  public static void setSerializationProperty(String name, String value, Response response, Serializer serializer)
  {
    Serializer.Property property = null;
    for (Serializer.Property p : Serializer.Property.values())
    {
      if (p.toString().equals(name))
      {
        property = p;
        break;
      }
    }
    serializer.setOutputProperty(property, value);
    switch (property)
    {
    case MEDIA_TYPE:
      response.setContentType(value);
      break;
    case ENCODING:
      response.setCharacterEncoding(value);
      break;
    default:
      ;
    }
  }

  private static NodeInfo parseXml(XPathContext context, String xml, String contentType)
  {
    return parseXml(context, new StreamSource(new StringReader(xml)), contentType);
  }

  private static boolean isXml(String contentType)
  {
    return contentType != null && (contentType.endsWith("/xml") || contentType.endsWith("+xml"));
  }

  private static boolean isHtml(String contentType)
  {
    return contentType != null && contentType.contains("html") && ! isXml(contentType);
  }

  private static NodeInfo parseXml(XPathContext context, StreamSource xml, String contentType)
  {
    try
    {
      XMLReader xmlReader;
      if (isHtml(contentType))
      {
        xmlReader = new org.ccil.cowan.tagsoup.Parser();
        InputStream is = xml.getInputStream();
        if (is != null)
        {
          ByteArrayOutputStream baos = new ByteArrayOutputStream();
          byte[] buffer = new byte[32768];
          for (int n; (n = is.read(buffer, 0, buffer.length)) != -1; )
          {
            baos.write(buffer, 0, n);
          }
          baos.flush();
          xml = new StreamSource(new StringReader(baos.toString(StandardCharsets.UTF_8.toString())));
        }
      }
      else
      {
        xmlReader = createXmlReader();
      }
      xmlReader.setFeature("http://xml.org/sax/features/validation", false);
      ParseOptions parseOptions = new ParseOptions();
      parseOptions.setXMLReader(xmlReader);
      return context.getConfiguration().buildDocumentTree(xml, parseOptions).getRootNode();
    }
    catch (XPathException | SAXException | IOException | ParserConfigurationException e)
    {
      throw new RuntimeException(e);
    }
  }

  private static XMLReader createXmlReader() throws SAXException, SAXNotSupportedException, ParserConfigurationException
  {
    SAXParserFactory parserFactory = SAXParserFactory.newInstance();
    SAXParser parser = parserFactory.newSAXParser();
    XMLReader xmlReader = parser.getXMLReader();
    xmlReader.setEntityResolver(new EntityResolver()
    {
      @Override
      public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException
      {
        String id = publicId == null ? systemId : systemId == null ? publicId : publicId + ", " + systemId;
        throw new SAXParseException("Unsupported entity: " + id, null);
      }
    });
    try
    {
      xmlReader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
    }
    catch (SAXNotRecognizedException e)
    {
    }
    return xmlReader;
  }

  private static boolean isMultipart(Request request)
  {
    return request.getContentType() != null &&
           request.getContentType().toLowerCase().indexOf("multipart/form-data") > -1;
  }

  private static String toString(InputStream inputStream, String encoding)
  {
    if (encoding == null)
    {
      encoding = UTF_8;
    }
    try (Scanner s = new Scanner(inputStream, encoding)) {
      s.useDelimiter("\\A");
      String value = s.hasNext() ? s.next() : "";
      return value;
    }
  }

  private static String escapeXml(String string)
  {
    return string.replace("&", "&amp;")
                 .replace("<", "&lt;")
                 .replace(">", "&gt;");
  }

  public static class SetSerializationParameters extends ExtensionFunction
  {
    private static final String SERIALIZATION_PARAMETERS = "serialization-parameters";
    private static final String SERIALIZATION_NAMESPACE = "http://www.w3.org/2010/xslt-xquery-serialization";

    public SetSerializationParameters()
    {
      super
      (
        "{http://bottlecaps.de/webapp}set-serialization-parameters",
        new SequenceType[] {SequenceType.SINGLE_NODE},
        SequenceType.ANY_SEQUENCE,
        new ExtensionFunctionCall()
        {
          @Override
          public Sequence call(XPathContext context, Sequence[] arguments) throws XPathException
          {
            NodeInfo node = (NodeInfo) arguments[0].head();
            if (SERIALIZATION_NAMESPACE.equals(node.getURI()) &&
                SERIALIZATION_PARAMETERS.equals(node.getLocalPart()) &&
                node.hasChildNodes())
            {
              for (AxisIterator i = node.iterateAxis(Axis.CHILD.getAxisNumber()); (node = i.next()) != null; )
              {
                if (node.getNodeKind() == Type.ELEMENT)
                {
                  if (SERIALIZATION_NAMESPACE.equals(node.getURI()))
                  {
                    String name = node.getLocalPart();
                    String value = node.getAttributeValue("", "value");
                    setSerializationProperty(name, value, response.get(context), serializer.get(context));
                  }
                }
              }
            }
            return EmptySequence.getInstance();
          }
        }
      );
    }
  }

  public static class Decode extends ExtensionFunction
  {
    public Decode()
    {
      super
      (
        "{de/bottlecaps/railroad/xq/user-interface.xq}decode-base64",
        new SequenceType[] {SequenceType.SINGLE_STRING},
        SequenceType.SINGLE_STRING,
        new ExtensionFunctionCall()
        {
          @Override
          public Sequence call(XPathContext context, Sequence[] arguments) throws XPathException
          {
            String base64 = arguments[0].head().getStringValue();
            return new StringValue(new String(Base64.getDecoder().decode(base64), StandardCharsets.UTF_8));
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
        "{http://bottlecaps.de/webapp}binary-file",
        new SequenceType[] {SequenceType.SINGLE_STRING},
        SequenceType.OPTIONAL_STRING,
        new ExtensionFunctionCall()
        {
          @Override
          public Sequence call(XPathContext context, Sequence[] arguments) throws XPathException
          {
            try
            {
              String fileName = arguments[0].head().getStringValue();
              Collection<MultiPart> parts = request.get(context).getParts();
              if (parts != null)
              {
                for (MultiPart part : parts)
                {
                  if (fileName.equals(fileName(part)))
                  {
                    InputStream inputStream = part.getInputStream();
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    byte[] buffer = new byte[0x8000];
                    for (int length; (length = inputStream.read(buffer)) > 0;)
                      outputStream.write(buffer, 0, length);
                    return new StringValue(Base64.getEncoder().encodeToString(outputStream.toByteArray()));
                  }
                }
              }
              return EmptySequence.getInstance();
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

  private static String fileName(MultiPart part)
  {
    for (String fragment : part.getHeader("Content-Disposition").split(";"))
    {
      if (fragment.trim().startsWith("filename"))
      {
        return fragment.substring(fragment.indexOf('=') + 1).trim().replace("\"", "");
      }
    }
    return null;
  }

  public static class Part extends ExtensionFunction
  {
    public Part()
    {
      super
      (
        "{http://bottlecaps.de/webapp}part",
        new SequenceType[] {SequenceType.SINGLE_STRING},
        SequenceType.SINGLE_NODE,
        new ExtensionFunctionCall()
        {
          @Override
          public Sequence call(XPathContext context, Sequence[] arguments) throws XPathException
          {
            try
            {
              String partName = arguments[0].head().getStringValue();
              String filename = fileName(request.get(context).getPart(partName));
              String partXml = "<request:part xmlns:request=\"http://bottlecaps.de/webapp\"><request:body" +
                               (filename == null ? "" : " request:filename=\"" + filename + "\"") +
                               "/></request:part>";
              NodeInfo part = parseXml(context, partXml, "text/xml");
              return part.iterateAxis(Axis.CHILD.getAxisNumber()).next();
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
        "{http://bottlecaps.de/webapp}part-names",
        new SequenceType[] {},
        SequenceType.STRING_SEQUENCE,
        new ExtensionFunctionCall()
        {
          @Override
          public Sequence call(XPathContext context, Sequence[] arguments) throws XPathException
          {
            List<StringValue> result = new ArrayList<>();
            try
            {
              Collection<MultiPart> parts = request.get(context).getParts();
              if (parts != null)
              {
                for (MultiPart part : parts)
                {
                  result.add(new StringValue(part.getName()));
                }
              }
            }
            catch (IOException e)
            {
              throw new RuntimeException(e);
            }
            return new SequenceExtent(result);
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
        "{http://bottlecaps.de/webapp}parameter-names",
        new SequenceType[] {},
        SequenceType.STRING_SEQUENCE,
        new ExtensionFunctionCall()
        {
          @Override
          public Sequence call(XPathContext context, Sequence[] arguments) throws XPathException
          {
            List<StringValue> result = new ArrayList<>();
            Enumeration<String> parameterNames = request.get(context).getParameterNames();
            if (parameterNames != null)
            {
              while (parameterNames.hasMoreElements())
              {
                result.add(new StringValue(parameterNames.nextElement()));
              }
            }
            return new SequenceExtent(result);
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
            try
            {
              String url = arguments[1].head().getStringValue();
              URLConnection connection = new URL(url).openConnection();
              int responseCode = connection instanceof HttpURLConnection ? ((HttpURLConnection) connection).getResponseCode() : 200;
              String resp = "<" + RESPONSE + " xmlns=\"" + HTTPCLIENT_NAMESPACE + "\" status=\"" + responseCode + "\"/>";
              List<Item> result = new ArrayList<>();
              result.add(parseXml(context, resp, "text/xml").iterateAxis(Axis.CHILD.getAxisNumber()).next());
              String contentType = connection.getContentType();
              if (isHtml(contentType) || isXml(contentType))
              {
                StreamSource source = new StreamSource((InputStream) connection.getContent());
                source.setSystemId(url);
                result.add(parseXml(context, source, contentType));
              }
              else
              {
                String value = ExtensionFunctions.toString((InputStream) connection.getContent(), connection.getContentEncoding());
                result.add(new StringValue(value));
              }
              return new SequenceExtent(result);
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

  public static class HeaderValue extends ExtensionFunction
  {
    public HeaderValue()
    {
      super
      (
        "{http://bottlecaps.de/webapp}header-value",
        new SequenceType[] {SequenceType.SINGLE_STRING},
        SequenceType.OPTIONAL_STRING,
        new ExtensionFunctionCall()
        {
          @Override
          public Sequence call(XPathContext context, Sequence[] arguments) throws XPathException
          {
            String name = arguments[0].head().getStringValue();
            return new StringValue(request.get(context).getHeader(name));
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
        "{http://bottlecaps.de/webapp}method-get",
        new SequenceType[] {},
        SequenceType.SINGLE_BOOLEAN,
        new ExtensionFunctionCall()
        {
          @Override
          public Sequence call(XPathContext context, Sequence[] arguments) throws XPathException
          {
            return BooleanValue.get("GET".equals(request.get(context).getMethod()));
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
        "{http://bottlecaps.de/webapp}set-header",
        new SequenceType[] {SequenceType.SINGLE_STRING, SequenceType.SINGLE_STRING},
        SequenceType.ANY_SEQUENCE,
        new ExtensionFunctionCall()
        {
          @Override
          public Sequence call(XPathContext context, Sequence[] arguments) throws XPathException
          {
            String name = arguments[0].head().getStringValue();
            String value = arguments[1].head().getStringValue();
            response.get(context).setHeader(name, value);
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
        "{http://bottlecaps.de/webapp}user-agent",
        new SequenceType[] {},
        SequenceType.OPTIONAL_STRING,
        new ExtensionFunctionCall()
        {
          @Override
          public Sequence call(XPathContext context, Sequence[] arguments) throws XPathException
          {
            String userAgent = request.get(context).getHeader("User-Agent");
            return userAgent == null
                 ? EmptySequence.getInstance()
                 : new StringValue(userAgent);
          }
        }
      );
    }
  }

  public static class SetContentType extends ExtensionFunction
  {
    public SetContentType()
    {
      super
      (
        "{http://bottlecaps.de/webapp}set-content-type",
        new SequenceType[] {SequenceType.SINGLE_STRING},
        SequenceType.ANY_SEQUENCE,
        new ExtensionFunctionCall()
        {
          @Override
          public Sequence call(XPathContext context, Sequence[] arguments) throws XPathException
          {
            String contentType = arguments[0].head().getStringValue();
            setSerializationProperty(OutputKeys.MEDIA_TYPE, contentType, response.get(context), serializer.get(context));
            return EmptySequence.getInstance();
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
        "{de/bottlecaps/railroad/xq/user-interface.xq}xhtml-to-zip",
        new SequenceType[]{SequenceType.SINGLE_STRING},
        SequenceType.SINGLE_ATOMIC,
        new ExtensionFunctionCall()
        {
          @Override
          public Sequence call(XPathContext context, Sequence[] arguments) throws XPathException
          {
            try
            {
              String xhtml = arguments[0].head().getStringValue();
              Source source = new SAXSource(createXmlReader(), new InputSource(new StringReader(xhtml)));
              ByteArrayOutputStream baos = new ByteArrayOutputStream();
              new XhtmlToZip().convert(source, baos);
              return new StringValue(Base64.getEncoder().encodeToString(baos.toByteArray()));
            }
            catch (Exception e)
            {
              throw e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e);
            }
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
        "{de/bottlecaps/railroad/xq/user-interface.xq}parse-xml",
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
              return parseXml(context, string, "text/xml");
            }
            catch (Exception e)
            {
              throw e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e);
            }
          }
        }
       );
    }
  }

  public static class CookieGet extends ExtensionFunction
  {
    private static final String COOKIE_NAMESPACE = "http://bottlecaps.de/webapp";

    public CookieGet()
    {
      super
      (
        "{http://bottlecaps.de/webapp}get-cookie",
        new SequenceType[]{SequenceType.SINGLE_STRING},
        SequenceType.NODE_SEQUENCE,
        new ExtensionFunctionCall()
        {
          @Override
          public Sequence call(XPathContext context, Sequence[] arguments) throws XPathException
          {
            String name = arguments[0].head().getStringValue();
            Cookie[] cookies = request.get(context).getCookies();
            if (cookies != null)
            {
              for (Cookie cookie : cookies)
              {
                if (cookie.getName().equals(name))
                {
                  String xmlCookie =
                      "<cookie xmlns='" + COOKIE_NAMESPACE + "' name='" + escapeXml(cookie.getName()) + "'>" +
                      escapeXml(cookie.getValue()) +
                      "</cookie>";
                  return parseXml(context, xmlCookie, "text/xml");
                }
              }
            }
            return EmptySequence.getInstance();
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
        "{http://bottlecaps.de/webapp}parameter-values",
        new SequenceType[]{SequenceType.STRING_SEQUENCE},
        SequenceType.STRING_SEQUENCE,
        new ExtensionFunctionCall()
        {
          @Override
          public Sequence call(XPathContext context, Sequence[] arguments) throws XPathException
          {
            String name = arguments[0].head().getStringValue();
            Request httpRequest = request.get(context);
            List<StringValue> result = new ArrayList<>();
            for (String value : parameterValues(httpRequest, name))
            {
              result.add(new StringValue(value));
            }
            return new SequenceExtent(result);
          }
        }
      );
    }
  }

  public static class GetRequestURI extends ExtensionFunction
  {
    public GetRequestURI()
    {
      super
      (
        "{http://bottlecaps.de/webapp}request-uri",
        new SequenceType[]{},
        SequenceType.SINGLE_STRING,
        new ExtensionFunctionCall()
        {
          @Override
          public Sequence call(XPathContext context, Sequence[] arguments) throws XPathException
          {
            Request httpRequest = request.get(context);
            return new StringValue(httpRequest.getRequestURI());
          }
        }
      );
    }
  }
}
