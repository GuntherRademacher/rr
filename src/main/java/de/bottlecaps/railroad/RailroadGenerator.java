package de.bottlecaps.railroad;

import static de.bottlecaps.xml.XQueryProcessor.defaultXQueryProcessor;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import de.bottlecaps.railroad.core.Download;
import de.bottlecaps.railroad.core.OutputOptions;
import de.bottlecaps.railroad.core.XhtmlToZip;
import de.bottlecaps.xml.XQueryProcessor.Result;

/** This class provides the RR API. */
public class RailroadGenerator
{
  /** URL to the online railroad diagram generator. */
  public static final String RR_URL = "https://www.bottlecaps.de/" + RailroadVersion.PROJECT_NAME;

  /** Text formats. */
  public enum TextFormat
  {
    /** XHTML text format. */
    XHTML("xhtml", OutputOptions.XHTML),
    /** HTML text format. */
    HTML("html", OutputOptions.HTML),
    /** Markdown text format. */
    MARKDOWN("md", OutputOptions.TEXT);

    private final String value;
    private final OutputOptions outputOptions;

    private TextFormat(String value, OutputOptions outputOptions)
    {
      this.value = value;
      this.outputOptions = outputOptions;
    }
  };

  /** Graphics formats. */
  public enum GraphicsFormat
  {
    /** SVG graphics format. */
    SVG,
    /** PNG graphics format. */
    PNG,
  };

  private OutputStream output = System.out;
  private TextFormat textFormat = TextFormat.XHTML;
  private GraphicsFormat graphicsFormat = GraphicsFormat.SVG;
  private boolean embedded = true;
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

  /** Default constructor. */
  public RailroadGenerator() {
  }

  /**
   * Generate a railroad diagram from the given grammar.
   * @param grammar input grammar in W3C EBNF notation
   * @throws Exception exception
   */
  public void generate(String grammar) throws Exception
  {
    if (grammar == null)
      throw new IllegalArgumentException("grammar cannot be null");
    if (grammar.isEmpty())
      throw new IllegalArgumentException("grammar cannot be empty");

    String moduleNamespace = "de/bottlecaps/railroad/xq/basic-interface.xq";
    URL moduleURL = Thread.currentThread().getContextClassLoader().getResource(moduleNamespace);
    String query =
        "import module namespace i='"+ moduleNamespace + "' at '" + moduleURL + "';\n" +
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

    Map<String, Object> externalVars = new HashMap<>();
    externalVars.put("ebnf", grammar);
    externalVars.put("show-ebnf", showEbnf);
    externalVars.put("recursion-elimination", recursionElimination);
    externalVars.put("factoring", factoring);
    externalVars.put("inline", inlineLiterals);
    externalVars.put("keep", keepEpsilon);
    externalVars.put("width", width);
    externalVars.put("color", baseColor == null ? null : toHexString(baseColor));
    externalVars.put("spread", colorOffset);
    externalVars.put("{de/bottlecaps/railroad/xq/ast-to-svg.xq}version", RailroadVersion.VERSION);
    externalVars.put("{de/bottlecaps/railroad/xq/ast-to-svg.xq}java-version", Download.javaVersion());
    externalVars.put("{de/bottlecaps/railroad/xq/ast-to-svg.xq}date", RailroadVersion.DATE);
    if (padding != null)
      externalVars.put("{de/bottlecaps/railroad/xq/ast-to-svg.xq}padding", padding);
    if (strokeWidth != null)
      externalVars.put("{de/bottlecaps/railroad/xq/ast-to-svg.xq}stroke-width", strokeWidth);

    Result xhtml = defaultXQueryProcessor()
      .compile(query)
      .evaluate(externalVars);

    if (! embedded)
    {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      xhtml.serialize(baos, OutputOptions.XHTML.value());
      new XhtmlToZip().convert(baos.toString(StandardCharsets.UTF_8), textFormat.value, graphicsFormat.name().toLowerCase(), output);
    }
    else if (textFormat == TextFormat.XHTML && graphicsFormat == GraphicsFormat.SVG)
    {
      xhtml.serialize(output, OutputOptions.XHTML.value());
    }
    else
    {
      String disassembleNamespace = "de/bottlecaps/railroad/xq/disassemble.xq";
      URL disassembleURL = Thread.currentThread().getContextClassLoader().getResource(disassembleNamespace);
      String toMarkdown =
          "import module namespace d='" + disassembleNamespace + "' at '" + disassembleURL + "';\n" +
          "declare variable $xhtml external;\n" +
          "declare variable $text-format external;\n" +
          "declare variable $img-format external;\n" +
          "d:disassemble($xhtml, $text-format, $img-format, true())/files/file/node()";
      defaultXQueryProcessor()
        .compile(toMarkdown)
        .evaluate(Map.of(
            "xhtml", xhtml,
            "text-format", textFormat.value,
            "img-format", graphicsFormat.name().toLowerCase()))
        .serialize(output, textFormat.outputOptions.value());
    }
  }

  private String toHexString(Color color)
  {
    return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
  }

  /**
   * Sets the output stream.
   * @param output output stream, defaults to {@code System.out}
   */
  public void setOutput(OutputStream output)
  {
    this.output = output;
  }

  /**
   * Sets the text output format.
   * @param textFormat format of text output, defaults to {@link TextFormat#XHTML}
   */
  public void setTextFormat(TextFormat textFormat)
  {
    if (textFormat == null)
      throw new IllegalArgumentException("textFormat cannot be null");

    this.textFormat = textFormat;
  }

  /**
   * Sets the graphics output format.
   * @param graphicsFormat format of graphics output, defaults to {@link GraphicsFormat#SVG}
   */
  public void setGraphicsFormat(GraphicsFormat graphicsFormat)
  {
    if (graphicsFormat == null)
      throw new IllegalArgumentException("graphicsFormat cannot be null");

    this.graphicsFormat = graphicsFormat;
  }

  /**
   * Sets whether to embed graphics in text, in a single output file.
   * @param embedded whether to embed graphics in text, in a single output file. Defaults to {@code true}.
   * When {@code false}, the output will be a zip with text output and graphics in separate files.
   */
  public void setEmbedded(boolean embedded)
  {
    this.embedded = embedded;
  }

  /**
   * Sets whether to show EBNF next to generated diagrams.
   * @param showEbnf whether to show EBNF next to generated diagrams, defaults to {@code true}
   */
  public void setShowEbnf(boolean showEbnf)
  {
    this.showEbnf = showEbnf;
  }

  /**
   * Sets whether to enable left and right factoring.
   * @param factoring whether to enable left and right factoring, defaults to {@code true}.
   */
  public void setFactoring(boolean factoring)
  {
    this.factoring = factoring;
  }

  /**
   * Sets whether to enable direct recursion elimination.
   * @param recursionElimination whether to enable direct recursion elimination, defaults to {@code true}
   */
  public void setRecursionElimination(boolean recursionElimination)
  {
    this.recursionElimination = recursionElimination;
  }

  /**
   * Sets whether to inline nonterminals that derive to single literals.
   * @param inlineLiterals whether to inline nonterminals that derive to single literals, defaults to {@code true}.
   */
  public void setInlineLiterals(boolean inlineLiterals)
  {
    this.inlineLiterals = inlineLiterals;
  }

  /**
   * Sets whether to keep nonterminal references that derive to epsilon (nothing) only.
   * @param keepEpsilon whether to keep nonterminal references that derive to epsilon (nothing) only, defaults to {@code true}.
   */
  public void setKeepEpsilon(boolean keepEpsilon)
  {
    this.keepEpsilon = keepEpsilon;
  }

  /**
   * Sets the base color to use.
   * @param baseColor the base color to use, defaults to {@code RGB 255,219,77} / {@code HSL 48,100%,65%}
   */
  public void setBaseColor(Color baseColor)
  {
    this.baseColor = baseColor;
  }

  /**
   * Sets the hue offset to secondary color in degrees.
   * @param colorOffset hue offset to secondary color in degrees, defaults to {@code 0}
   */
  public void setColorOffset(int colorOffset)
  {
    this.colorOffset = colorOffset;
  }

  /**
   * Sets the padding to apply.
   * @param padding padding to apply, defaults to {@code 10}
   */
  public void setPadding(int padding)
  {
    this.padding = padding;
  }

  /**
   * Sets the stroke width to apply.
   * @param strokeWidth stroke width to set, defaults to {@code 1}
   */
  public void setStrokeWidth(int strokeWidth)
  {
    this.strokeWidth = strokeWidth;
  }

  /**
   * Sets the width of the graphics.
   * @param width if exceeded, generator tries to break graphics into multiple lines; defaults to {@code 992}
   */
  public void setWidth(int width)
  {
    this.width = width;
  }
}