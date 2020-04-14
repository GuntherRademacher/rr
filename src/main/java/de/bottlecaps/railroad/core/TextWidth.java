package de.bottlecaps.railroad.core;

import net.sf.saxon.Configuration;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.lib.ExtensionFunctionCall;
import net.sf.saxon.lib.ExtensionFunctionDefinition;
import net.sf.saxon.lib.Initializer;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.Int64Value;
import net.sf.saxon.value.SequenceType;

public class TextWidth
{
  public static int normal(String text)
  {
    int width = 0;
    for (int i = 0; i < text.length(); )
    {
      int codePoint = text.codePointAt(i);
      width += Normal.get(codePoint);
      i += Character.charCount(codePoint);
    }
    return (width + 5) / 10;
  }

  public static int bold(String text)
  {
    int width = 0;
    for (int i = 0; i < text.length(); )
    {
      int codePoint = text.codePointAt(i);
      width += Bold.get(codePoint);
      i += Character.charCount(codePoint);
    }
    return (width + 5) / 10;
  }

  public static class SaxonDefinition_normal extends SaxonDefinition
  {
    @Override
    public String functionName() {return "normal";}
    @Override
    public Sequence execute(XPathContext context, String input) throws XPathException
    {
      return Int64Value.makeIntegerValue(normal(input));
    }
  }

  public static class SaxonInitializer implements Initializer
  {
    @Override
    public void initialize(Configuration conf)
    {
      conf.registerExtensionFunction(new SaxonDefinition_normal());
      conf.registerExtensionFunction(new SaxonDefinition_bold());
    }
  }

  public static class SaxonDefinition_bold extends SaxonDefinition
  {
    @Override
    public String functionName() {return "bold";}
    @Override
    public Sequence execute(XPathContext context, String input) throws XPathException
    {
      return Int64Value.makeIntegerValue(bold(input));
    }
  }

  public static abstract class SaxonDefinition extends ExtensionFunctionDefinition
  {
    abstract String functionName();
    abstract Sequence execute(XPathContext context, String input) throws XPathException;

    @Override
    public StructuredQName getFunctionQName() {return new StructuredQName("text-width", TextWidth.class.getSimpleName(), functionName());}
    @Override
    public SequenceType[] getArgumentTypes() {return new SequenceType[] {SequenceType.SINGLE_STRING};}
    @Override
    public SequenceType getResultType(SequenceType[] suppliedArgumentTypes) {return SequenceType.SINGLE_INTEGER;}

    @Override
    public ExtensionFunctionCall makeCallExpression()
    {
      return new ExtensionFunctionCall()
      {
        @Override
        public Sequence call(XPathContext context, Sequence[] arguments) throws XPathException
        {
          return execute(context, arguments[0].iterate().next().getStringValue());
        }
      };
    }
  }

}
