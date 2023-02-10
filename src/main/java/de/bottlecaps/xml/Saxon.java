package de.bottlecaps.xml;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;

import de.bottlecaps.railroad.core.ExtensionFunctions;
import de.bottlecaps.webapp.Request;
import de.bottlecaps.webapp.Response;
import net.sf.saxon.Configuration;
import net.sf.saxon.jaxp.SaxonTransformerFactory;
import net.sf.saxon.lib.ParseOptions;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.Serializer;
import net.sf.saxon.s9api.XQueryCompiler;
import net.sf.saxon.s9api.XQueryEvaluator;
import net.sf.saxon.s9api.XQueryExecutable;
import net.sf.saxon.s9api.XdmAtomicValue;
import net.sf.saxon.s9api.XdmEmptySequence;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmValue;
import net.sf.saxon.value.ObjectValue;

public class Saxon implements XQueryProcessor, XsltProcessor {
  public static final Saxon instance = new Saxon();

  private Configuration configuration = new Configuration();
  private static Processor processor;
  private static XQueryCompiler compiler;
  private TransformerFactory transformerFactory;

  private Saxon()  {
    configuration = new Configuration();
    new SaxonFunctions().initialize(configuration);

    processor = new Processor(configuration);
    compiler = processor.newXQueryCompiler();
    transformerFactory = TransformerFactory.newInstance(SaxonTransformerFactory.class.getName(), Saxon.class.getClassLoader());
  }

  public static class SaxonResult implements Result {
    private XdmValue xdmValue;

    @Override
    public Object getResultObject() {
      return xdmValue;
    }

    protected SaxonResult(XdmValue xdmValue) {
      this.xdmValue = xdmValue;
    }

    public XdmValue getXdmValue() {
      return xdmValue;
    }

    @Override
    public void serialize(OutputStream outputStream, Map<String, String> outputOptions) throws Exception {
      Serializer serializer = serializer(outputOptions);
      serializer.setOutputStream(outputStream);
      serializer.serializeXdmValue(xdmValue);
      serializer.close();
    }
  }

  private static class SaxonPlan implements Plan {
    private XQueryExecutable executable;

    protected SaxonPlan(XQueryExecutable executable) {
      this.executable = executable;
    }

    @Override
    public Result evaluate(Map<String, Object> externalVars) throws Exception {
      XQueryEvaluator evaluator = executable.load();
      try {
        bindExternalVariables(evaluator, externalVars);
        return new SaxonResult(evaluator.evaluate());
      }
      finally {
        evaluator.close();
      }
    }
  }

  @Override
  public Plan compile(String query) throws Exception {
    return new SaxonPlan(compiler.compile(query));
  }

  @Override
  public void evaluateXslt(String xslt, Map<String, Object> parameters,
      OutputStream outputStream) throws Exception {

    StreamSource stylesheet = new StreamSource(new StringReader(xslt));
    Transformer transformer = transformerFactory.newTransformer(stylesheet);
    transformer.setErrorListener(new ErrorListener()  {

      @Override
      public void warning(TransformerException exception) throws TransformerException {
      }

      @Override
      public void error(TransformerException exception) throws TransformerException {
      }

      @Override
      public void fatalError(TransformerException exception) throws TransformerException {
      }
    });
    parameters.forEach((key, value) -> transformer.setParameter(key, value));
    transformer.transform(new StreamSource(new StringReader("<input/>")), new StreamResult(outputStream));
  }

  private static void bindExternalVariables(XQueryEvaluator evaluator, Map<String, Object> externalVars) {
    for (Map.Entry<String, Object> externalVar : externalVars.entrySet()) {
      javax.xml.namespace.QName qName = javax.xml.namespace.QName.valueOf(externalVar.getKey());
      QName name = new QName(qName.getNamespaceURI(), qName.getLocalPart());
      Object value = externalVar.getValue();
      XdmValue xdmValue;
      if (value == null)
        xdmValue = XdmEmptySequence.getInstance();
      else if (value instanceof Boolean)
        xdmValue = new XdmAtomicValue((Boolean) value);
      else if (value instanceof Integer)
        xdmValue = new XdmAtomicValue((Integer) value);
      else if (value instanceof String)
        xdmValue = new XdmAtomicValue((String) value);
      else if (value instanceof Request
            || value instanceof Response
            || value instanceof Map)
        xdmValue = XdmValue.wrap(new ObjectValue<>(value));
      else if (value instanceof SaxonResult)
        xdmValue = ((SaxonResult) value).xdmValue;
      else
        throw new IllegalArgumentException(value.getClass().getName());
      evaluator.setExternalVariable(name, xdmValue);
    }
  }

  private static Serializer serializer(Map<String, String> outputOptions) {
    Serializer serializer = processor.newSerializer();

    for (Map.Entry<String, String> outputOption : outputOptions.entrySet())
    {
      String name = outputOption.getKey();
      String value = outputOption.getValue();
      Serializer.Property property = null;
      for (Serializer.Property p : Serializer.Property.values())
      {
        if (p.toString().equals(name))
        {
          property = p;
          break;
        }
      }
      if (property == null)
      {
        throw new IllegalArgumentException("unsupported serialization option: " + name);
      }

      serializer.setOutputProperty(property, value);
    }
    return serializer;
  }

  @Override
  public Result parseXml(String xml) throws Exception {
    return new SaxonResult(new XdmNode(parseXml(xml, "text/xml")));
  }

  public NodeInfo parseXml(String xml, String contentType)
  {
    return parseXml(new StreamSource(new StringReader(xml)), contentType);
  }

  public static boolean isXml(String contentType)
  {
    return contentType != null && (contentType.endsWith("/xml") || contentType.endsWith("+xml"));
  }

  public static boolean isHtml(String contentType)
  {
    return contentType != null && contentType.contains("html") && ! isXml(contentType);
  }

  public NodeInfo parseXml(StreamSource xml, String contentType)
  {
    try
    {
      XMLReader xmlReader;
      if (isHtml(contentType))
      {
        xmlReader = new org.ccil.cowan.tagsoup.Parser();
        InputStream is = xml.getInputStream();
        if (is != null)
          xml = new StreamSource(new StringReader(ExtensionFunctions.toString(is, StandardCharsets.UTF_8.name())));
      }
      else
      {
        xmlReader = createXmlReader();
      }
      xmlReader.setFeature("http://xml.org/sax/features/validation", false);
      xmlReader.setFeature("http://xml.org/sax/features/namespaces", true);
      xmlReader.setFeature("http://xml.org/sax/features/namespace-prefixes", false);
      ParseOptions parseOptions = new ParseOptions().withXMLReader(xmlReader);
      return configuration.buildDocumentTree(xml, parseOptions).getRootNode();
    }
    catch (Exception e)
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

}