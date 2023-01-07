package de.bottlecaps.railroad;

import java.awt.Color;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import de.bottlecaps.railroad.core.Download;
import de.bottlecaps.railroad.core.Parser;
import de.bottlecaps.railroad.core.ResourceModuleUriResolver;
import de.bottlecaps.railroad.core.TextWidth;
import de.bottlecaps.railroad.core.XhtmlToZip;
import net.sf.saxon.Configuration;
import net.sf.saxon.lib.Feature;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.Serializer;
import net.sf.saxon.s9api.XQueryCompiler;
import net.sf.saxon.s9api.XQueryEvaluator;
import net.sf.saxon.s9api.XQueryExecutable;
import net.sf.saxon.s9api.XdmAtomicValue;
import net.sf.saxon.s9api.XdmEmptySequence;
import net.sf.saxon.s9api.XdmNode;

public class RailroadGenerator
{
  public static final String RR_URL = "https://bottlecaps.de/" + RailroadVersion.PROJECT_NAME;

  public enum OutputType
  {
    XHTML_SVG
      {
        @Override
        protected void produce(Processor processor, XQueryCompiler compiler, XQueryEvaluator xqueryEvaluator, OutputStream output) throws Exception
        {
          Serializer serializer = processor.newSerializer(output);
          serializer.setOutputProperty(Serializer.Property.METHOD, "xhtml");
          serializer.setOutputProperty(Serializer.Property.ENCODING, StandardCharsets.UTF_8.name());
          serializer.setOutputProperty(Serializer.Property.OMIT_XML_DECLARATION, "yes");
          serializer.setOutputProperty(Serializer.Property.VERSION, "1.0");
          serializer.setOutputProperty(Serializer.Property.DOCTYPE_SYSTEM, "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd");
          serializer.setOutputProperty(Serializer.Property.DOCTYPE_PUBLIC, "-//W3C//DTD XHTML 1.0 Transitional//EN");
          serializer.setOutputProperty(Serializer.Property.INDENT, "yes");
          xqueryEvaluator.run(serializer);
        }
      },

    HTML_PNG_ZIP
      {
        @Override
        protected void produce(Processor processor, XQueryCompiler compiler, XQueryEvaluator xqueryEvaluator, OutputStream output) throws Exception
        {
          XdmNode node = (XdmNode) xqueryEvaluator.iterator().next();
          new XhtmlToZip().convert(node.getUnderlyingNode(), output);
        }
      },

    MARKDOWN_SVG
      {
        @Override
        protected void produce(Processor processor, XQueryCompiler compiler, XQueryEvaluator xqueryEvaluator, OutputStream output) throws Exception
        {
          Serializer serializer = processor.newSerializer(output);
          serializer.setOutputProperty(Serializer.Property.METHOD, "text");
          serializer.setOutputProperty(Serializer.Property.ENCODING, StandardCharsets.UTF_8.name());
          XQueryEvaluator toMarkdown = compiler.compile(
            "import module namespace m='de/bottlecaps/railroad/xq/xhtml-to-md.xq';\n" +
            "declare variable $xhtml external;\n" +
            "m:transform($xhtml)").load();
          toMarkdown.setExternalVariable(new QName("xhtml"), (XdmNode) xqueryEvaluator.iterator().next());
          toMarkdown.run(processor.newSerializer(output));
        }
      };

    protected abstract void produce(Processor processor, XQueryCompiler compiler, XQueryEvaluator xqueryEvaluator, OutputStream output) throws Exception;
  }

  private OutputStream output = System.out;
  private OutputType outputType = OutputType.XHTML_SVG;
  private boolean showEbnf = true;
  private boolean factoring = true;
  private boolean recursionElimination = true;
  private boolean inlineLiterals = true;
  private boolean keepEpsilon = true;
  private Color baseColor;
  private int colorOffset;
  private Integer padding;
  private Integer strokeWidth;
  private Integer width;

  /**
   * @param grammar input grammar in W3C EBNF notation
   */
  public void generate(String grammar) throws Exception
  {
    if (grammar == null)
      throw new IllegalArgumentException("grammar cannot be null");
    if (grammar.isEmpty())
      throw new IllegalArgumentException("grammar cannot be empty");

    Configuration configuration = new Configuration();
    configuration.registerExtensionFunction(new Parser.SaxonDefinition_Grammar());
    Processor processor = new Processor(configuration);
    processor.setConfigurationProperty(Feature.XSD_VERSION, "1.1");
    new TextWidth.SaxonInitializer().initialize(processor.getUnderlyingConfiguration());

    XQueryCompiler compiler = processor.newXQueryCompiler();
    compiler.setModuleURIResolver(ResourceModuleUriResolver.instance);
    String query =
        "import module namespace i='de/bottlecaps/railroad/xq/basic-interface.xq';\n" +
        "declare variable $ebnf external;\n" +
        "declare variable $show-ebnf external;\n" +
        "declare variable $recursion-elimination external;\n" +
        "declare variable $factoring external;\n" +
        "declare variable $inline external;\n" +
        "declare variable $keep external;\n" +
        "declare variable $width external;\n" +
        "declare variable $color external;\n" +
        "declare variable $spread external;\n" +
        "i:ebnf-to-xhtml($ebnf, $show-ebnf, $recursion-elimination, $factoring, $inline, $keep, $width, $color, $spread, '" + RR_URL + "')";
    XQueryExecutable executable = compiler.compile(query);
    XQueryEvaluator xqueryEvaluator = executable.load();

    xqueryEvaluator.setExternalVariable(new QName("ebnf"), new XdmAtomicValue(grammar));
    xqueryEvaluator.setExternalVariable(new QName("show-ebnf"), new XdmAtomicValue(showEbnf));
    xqueryEvaluator.setExternalVariable(new QName("recursion-elimination"), new XdmAtomicValue(recursionElimination));
    xqueryEvaluator.setExternalVariable(new QName("factoring"), new XdmAtomicValue(factoring));
    xqueryEvaluator.setExternalVariable(new QName("inline"), new XdmAtomicValue(inlineLiterals));
    xqueryEvaluator.setExternalVariable(new QName("keep"), new XdmAtomicValue(keepEpsilon));
    xqueryEvaluator.setExternalVariable(new QName("width"), width == null ? XdmEmptySequence.getInstance() : new XdmAtomicValue(width));
    xqueryEvaluator.setExternalVariable(new QName("color"), baseColor == null ? XdmEmptySequence.getInstance() : new XdmAtomicValue(toHexString(baseColor)));
    xqueryEvaluator.setExternalVariable(new QName("spread"), new XdmAtomicValue(colorOffset));
    xqueryEvaluator.setExternalVariable(new QName("de/bottlecaps/railroad/xq/ast-to-svg.xq", "version"), new XdmAtomicValue(RailroadVersion.VERSION));
    xqueryEvaluator.setExternalVariable(new QName("de/bottlecaps/railroad/xq/ast-to-svg.xq", "java-version"), new XdmAtomicValue(Download.javaVersion()));
    xqueryEvaluator.setExternalVariable(new QName("de/bottlecaps/railroad/xq/ast-to-svg.xq", "date"), new XdmAtomicValue(RailroadVersion.DATE));
    if (padding != null)
      xqueryEvaluator.setExternalVariable(new QName("de/bottlecaps/railroad/xq/ast-to-svg.xq", "padding"), new XdmAtomicValue(padding));
    if (strokeWidth != null)
      xqueryEvaluator.setExternalVariable(new QName("de/bottlecaps/railroad/xq/ast-to-svg.xq", "stroke-width"), new XdmAtomicValue(strokeWidth));

    outputType.produce(processor, compiler, xqueryEvaluator, output);
  }

  private String toHexString(Color color)
  {
    return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
  }

  /**
   * @param output output stream, defaults to {@code System.out}
   */
  public void setOutput(OutputStream output)
  {
    this.output = output;
  }

  /**
   * @param outputType type of output file, defaults to {@link OutputType#XHTML_SVG}
   */
  public void setOutputType(OutputType outputType)
  {
    if (outputType == null)
      throw new IllegalArgumentException("outputType cannot be null");

    this.outputType = outputType;
  }

  /**
   * @param showEbnf whether to show EBNF next to generated diagrams, defaults to {@code true}
   */
  public void setShowEbnf(boolean showEbnf)
  {
    this.showEbnf = showEbnf;
  }

  /**
   * @param factoring whether to enable left and right factoring, defaults to {@code true}.
   */
  public void setFactoring(boolean factoring)
  {
    this.factoring = factoring;
  }

  /**
   * @param recursionElimination whether to enable direct recursion elimination, defaults to {@code true}
   */
  public void setRecursionElimination(boolean recursionElimination)
  {
    this.recursionElimination = recursionElimination;
  }

  /**
   * @param inlineLiterals whether to inline nonterminals that derive to single literals, defaults to {@code true}.
   */
  public void setInlineLiterals(boolean inlineLiterals)
  {
    this.inlineLiterals = inlineLiterals;
  }

  /**
   * @param keepEpsilon whether to keep nonterminal references that derive to epsilon (nothing) only, defaults to {@code true}.
   */
  public void setKeepEpsilon(boolean keepEpsilon)
  {
    this.keepEpsilon = keepEpsilon;
  }

  /**
   * @param baseColor the base color to use, defaults to {@code RGB 255,219,77} / {@code HSL 48,100%,65%}
   */
  public void setBaseColor(Color baseColor)
  {
    this.baseColor = baseColor;
  }

  /**
   * @param colorOffset hue offset to secondary color in degrees, defaults to {@code 0}
   */
  public void setColorOffset(int colorOffset)
  {
    this.colorOffset = colorOffset;
  }

  /**
   * @param padding padding to apply, defaults to {@code 10}
   */
  public void setPadding(int padding)
  {
    this.padding = padding;
  }

  /**
   * @param strokeWidth stroke width to set, defaults to {@code 1}
   */
  public void setStrokeWidth(int strokeWidth)
  {
    this.strokeWidth = strokeWidth;
  }

  /**
   * @param width if exceeded, generator tries to break graphics into multiple lines; defaults to {@code 922}
   */
  public void setWidth(int width)
  {
    this.width = width;
  }
}
