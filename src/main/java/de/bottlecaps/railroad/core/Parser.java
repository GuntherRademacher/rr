// This file was generated on Mon Feb 10, 2025 11:01 (UTC+01) by REx v6.2-SNAPSHOT which is Copyright (c) 1979-2025 by Gunther Rademacher <grd@gmx.net>
// REx command line: -tree -a none -java -saxon -name de.bottlecaps.railroad.core.Parser Parser.ebnf

package de.bottlecaps.railroad.core;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import net.sf.saxon.Configuration;
import net.sf.saxon.event.Builder;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.lib.ExtensionFunctionCall;
import net.sf.saxon.lib.ExtensionFunctionDefinition;
import net.sf.saxon.lib.Initializer;
import net.sf.saxon.om.AttributeInfo;
import net.sf.saxon.om.NoNamespaceName;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.SmallAttributeMap;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.AnySimpleType;
import net.sf.saxon.type.AnyType;
import net.sf.saxon.value.SequenceType;
import net.sf.saxon.expr.parser.Loc;
import net.sf.saxon.om.AttributeMap;
import net.sf.saxon.om.EmptyAttributeMap;
import net.sf.saxon.om.NamespaceMap;
import net.sf.saxon.s9api.Location;
import net.sf.saxon.str.StringView;

public class Parser
{
  public static class ParseException extends RuntimeException
  {
    private static final long serialVersionUID = 1L;
    private int begin, end, offending, expected, state;

    public ParseException(int b, int e, int s, int o, int x)
    {
      begin = b;
      end = e;
      state = s;
      offending = o;
      expected = x;
    }

    @Override
    public String getMessage()
    {
      return offending < 0
           ? "lexical analysis failed"
           : "syntax error";
    }

    public void serialize(EventHandler eventHandler)
    {
    }

    public int getBegin() {return begin;}
    public int getEnd() {return end;}
    public int getState() {return state;}
    public int getOffending() {return offending;}
    public int getExpected() {return expected;}
    public boolean isAmbiguousInput() {return false;}
  }

  public interface EventHandler
  {
    public void reset(CharSequence string);
    public void startNonterminal(String name, int begin);
    public void endNonterminal(String name, int end);
    public void terminal(String name, int begin, int end);
    public void whitespace(int begin, int end);
  }

  public static class TopDownTreeBuilder implements EventHandler
  {
    private CharSequence input = null;
    private Nonterminal[] stack = new Nonterminal[64];
    private int top = -1;

    @Override
    public void reset(CharSequence input)
    {
      this.input = input;
      top = -1;
    }

    @Override
    public void startNonterminal(String name, int begin)
    {
      Nonterminal nonterminal = new Nonterminal(name, begin, begin, new Symbol[0]);
      if (top >= 0) addChild(nonterminal);
      if (++top >= stack.length) stack = Arrays.copyOf(stack, stack.length << 1);
      stack[top] = nonterminal;
    }

    @Override
    public void endNonterminal(String name, int end)
    {
      stack[top].end = end;
      if (top > 0) --top;
    }

    @Override
    public void terminal(String name, int begin, int end)
    {
      addChild(new Terminal(name, begin, end));
    }

    @Override
    public void whitespace(int begin, int end)
    {
    }

    private void addChild(Symbol s)
    {
      Nonterminal current = stack[top];
      current.children = Arrays.copyOf(current.children, current.children.length + 1);
      current.children[current.children.length - 1] = s;
    }

    public void serialize(EventHandler e)
    {
      e.reset(input);
      stack[0].send(e);
    }
  }

  public static abstract class Symbol
  {
    public String name;
    public int begin;
    public int end;

    protected Symbol(String name, int begin, int end)
    {
      this.name = name;
      this.begin = begin;
      this.end = end;
    }

    public abstract void send(EventHandler e);
  }

  public static class Terminal extends Symbol
  {
    public Terminal(String name, int begin, int end)
    {
      super(name, begin, end);
    }

    @Override
    public void send(EventHandler e)
    {
      e.terminal(name, begin, end);
    }
  }

  public static class Nonterminal extends Symbol
  {
    public Symbol[] children;

    public Nonterminal(String name, int begin, int end, Symbol[] children)
    {
      super(name, begin, end);
      this.children = children;
    }

    @Override
    public void send(EventHandler e)
    {
      e.startNonterminal(name, begin);
      int pos = begin;
      for (Symbol c : children)
      {
        if (pos < c.begin) e.whitespace(pos, c.begin);
        c.send(e);
        pos = c.end;
      }
      if (pos < end) e.whitespace(pos, end);
      e.endNonterminal(name, end);
    }
  }

  public static class SaxonTreeBuilder implements EventHandler
  {
    private CharSequence input;
    private Builder builder;
    private AnyType anyType;

    public SaxonTreeBuilder(Builder b)
    {
      input = null;
      builder = b;
      anyType = AnyType.getInstance();
    }

    @Override
    public void reset(CharSequence string)
    {
      input = string;
    }

    @Override
    public void startNonterminal(String name, int begin)
    {
      try
      {
        builder.startElement(new NoNamespaceName(name), anyType, NO_ATTRIBUTES, NO_NAMESPACES, LOCATION, 0);
      }
      catch (XPathException e)
      {
        throw new RuntimeException(e);
      }
    }

    @Override
    public void endNonterminal(String name, int end)
    {
      try
      {
        builder.endElement();
      }
      catch (XPathException e)
      {
        throw new RuntimeException(e);
      }
    }

    @Override
    public void terminal(String name, int begin, int end)
    {
      if (name.charAt(0) == '\'')
      {
        name = "TOKEN";
      }
      startNonterminal(name, begin);
      characters(begin, end);
      endNonterminal(name, end);
    }

    @Override
    public void whitespace(int begin, int end)
    {
      characters(begin, end);
    }

    private void characters(int begin, int end)
    {
      if (begin < end)
      {
        try
        {
          builder.characters(StringView.of(input.subSequence(begin, end).toString()), LOCATION, 0);
        }
        catch (XPathException e)
        {
          throw new RuntimeException(e);
        }
      }
    }
  }

  private static final AttributeMap NO_ATTRIBUTES = EmptyAttributeMap.getInstance();
  private static final NamespaceMap NO_NAMESPACES = NamespaceMap.emptyMap();
  private static final Location LOCATION = Loc.NONE;

  public static class SaxonInitializer implements Initializer
  {
    @Override
    public void initialize(Configuration conf)
    {
      conf.registerExtensionFunction(new SaxonDefinition_Grammar());
    }
  }

  public static Sequence parseGrammar(XPathContext context, String input) throws XPathException
  {
    Builder builder = context.getController().makeBuilder();
    builder.open();
    Parser parser = new Parser(input, new SaxonTreeBuilder(builder));
    try
    {
      parser.parse_Grammar();
    }
    catch (ParseException pe)
    {
      buildError(parser, pe, builder);
    }
    return builder.getCurrentRoot();
  }

  public static class SaxonDefinition_Grammar extends SaxonDefinition
  {
    @Override
    public String functionName() {return "parse-Grammar";}
    @Override
    public Sequence execute(XPathContext context, String input) throws XPathException
    {
      return parseGrammar(context, input);
    }
  }

  public static abstract class SaxonDefinition extends ExtensionFunctionDefinition
  {
    abstract String functionName();
    abstract Sequence execute(XPathContext context, String input) throws XPathException;

    @Override
    public StructuredQName getFunctionQName() {return new StructuredQName("p", "de/bottlecaps/railroad/core/Parser", functionName());}
    @Override
    public SequenceType[] getArgumentTypes() {return new SequenceType[] {SequenceType.SINGLE_STRING};}
    @Override
    public SequenceType getResultType(SequenceType[] suppliedArgumentTypes) {return SequenceType.SINGLE_NODE;}

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

  private static void buildError(Parser parser, ParseException pe, Builder builder) throws XPathException
  {
    builder.close();
    builder.reset();
    builder.open();
    List<AttributeInfo> attributes = new ArrayList<>();
    AnySimpleType anySimpleType = AnySimpleType.getInstance();
    attributes.add(new AttributeInfo(new NoNamespaceName("b"), anySimpleType, Integer.toString(pe.getBegin() + 1), LOCATION, 0));
    attributes.add(new AttributeInfo(new NoNamespaceName("e"), anySimpleType, Integer.toString(pe.getEnd() + 1), LOCATION, 0));
    if (pe.getOffending() < 0)
    {
      attributes.add(new AttributeInfo(new NoNamespaceName("s"), anySimpleType, Integer.toString(pe.getState()), LOCATION, 0));
    }
    else
    {
      attributes.add(new AttributeInfo(new NoNamespaceName("o"), anySimpleType, Integer.toString(pe.getOffending()), LOCATION, 0));
      attributes.add(new AttributeInfo(new NoNamespaceName("x"), anySimpleType, Integer.toString(pe.getExpected()), LOCATION, 0));
    }
    builder.startElement(new NoNamespaceName("ERROR"), AnyType.getInstance(), new SmallAttributeMap(attributes), NO_NAMESPACES, LOCATION, 0);
    builder.characters(StringView.of(parser.getErrorMessage(pe)), LOCATION, 0);
    builder.endElement();
  }

  public Parser(CharSequence string, EventHandler t)
  {
    initialize(string, t);
  }

  public void initialize(CharSequence source, EventHandler parsingEventHandler)
  {
    eventHandler = parsingEventHandler;
    input = source;
    size = source.length();
    reset(0, 0, 0);
  }

  public CharSequence getInput()
  {
    return input;
  }

  public int getTokenOffset()
  {
    return b0;
  }

  public int getTokenEnd()
  {
    return e0;
  }

  public final void reset(int l, int b, int e)
  {
            b0 = b; e0 = b;
    l1 = l; b1 = b; e1 = e;
    l2 = 0; b2 = 0; e2 = 0;
    l3 = 0; b3 = 0; e3 = 0;
    end = e;
    eventHandler.reset(input);
  }

  public void reset()
  {
    reset(0, 0, 0);
  }

  public static String getOffendingToken(ParseException e)
  {
    return e.getOffending() < 0 ? null : TOKEN[e.getOffending()];
  }

  public static String[] getExpectedTokenSet(ParseException e)
  {
    String[] expected;
    if (e.getExpected() >= 0)
    {
      expected = new String[]{TOKEN[e.getExpected()]};
    }
    else
    {
      expected = getTokenSet(- e.getState());
    }
    return expected;
  }

  public String getErrorMessage(ParseException e)
  {
    String message = e.getMessage();
    String[] tokenSet = getExpectedTokenSet(e);
    String found = getOffendingToken(e);
    int size = e.getEnd() - e.getBegin();
    message += (found == null ? "" : ", found " + found)
            + "\nwhile expecting "
            + (tokenSet.length == 1 ? tokenSet[0] : java.util.Arrays.toString(tokenSet))
            + "\n"
            + (size == 0 || found != null ? "" : "after successfully scanning " + size + " characters beginning ");
    String prefix = input.subSequence(0, e.getBegin()).toString();
    int line = prefix.replaceAll("[^\n]", "").length() + 1;
    int column = prefix.length() - prefix.lastIndexOf('\n');
    return message
         + "at line " + line + ", column " + column + ":\n..."
         + input.subSequence(e.getBegin(), Math.min(input.length(), e.getBegin() + 64))
         + "...";
  }

  public void parse_Grammar()
  {
    eventHandler.startNonterminal("Grammar", e0);
    for (;;)
    {
      lookahead1W(19);              // Whitespace | NCName | DocComment | '.' | '<?'
      if (l1 != 31)                 // '<?'
      {
        break;
      }
      whitespace();
      parse_ProcessingInstruction();
    }
    for (;;)
    {
      whitespace();
      parse_Production();
      lookahead1W(21);              // Whitespace | NCName | DocComment | EOF | '.' | '<?ENCORE?>' | '<?TOKENS?>'
      if (l1 != 3                   // NCName
       && l1 != 15                  // DocComment
       && l1 != 27)                 // '.'
      {
        break;
      }
    }
    if (l1 == 33)                   // '<?TOKENS?>'
    {
      consume(33);                  // '<?TOKENS?>'
      for (;;)
      {
        lookahead1W(23);            // Whitespace | NCName | StringLiteral | DocComment | EOF | EquivalenceLookAhead |
                                    // '.' | '<?ENCORE?>'
        if (l1 == 16                // EOF
         || l1 == 32)               // '<?ENCORE?>'
        {
          break;
        }
        switch (l1)
        {
        case 3:                     // NCName
          lookahead2W(22);          // Whitespace | Context | '::=' | '<<' | '>>' | '?' | '\\'
          break;
        default:
          lk = l1;
        }
        switch (lk)
        {
        case 15:                    // DocComment
        case 27:                    // '.'
        case 1859:                  // NCName '::='
        case 2307:                  // NCName '?'
          whitespace();
          parse_Production();
          break;
        case 2563:                  // NCName '\\'
          whitespace();
          parse_Delimiter();
          break;
        case 17:                    // EquivalenceLookAhead
          whitespace();
          parse_Equivalence();
          break;
        default:
          whitespace();
          parse_Preference();
        }
      }
    }
    if (l1 == 32)                   // '<?ENCORE?>'
    {
      consume(32);                  // '<?ENCORE?>'
      for (;;)
      {
        lookahead1W(12);            // Whitespace | EOF | '<?'
        if (l1 != 31)               // '<?'
        {
          break;
        }
        whitespace();
        parse_ProcessingInstruction();
      }
    }
    consume(16);                    // EOF
    eventHandler.endNonterminal("Grammar", e0);
  }

  private void parse_ProcessingInstruction()
  {
    eventHandler.startNonterminal("ProcessingInstruction", e0);
    consume(31);                    // '<?'
    lookahead1(0);                  // NCName
    consume(3);                     // NCName
    lookahead1(8);                  // S | '?>'
    if (l1 == 14)                   // S
    {
      for (;;)
      {
        consume(14);                // S
        lookahead1(7);              // ProcessingInstructionContents | S
        if (l1 != 14)               // S
        {
          break;
        }
      }
      consume(2);                   // ProcessingInstructionContents
    }
    lookahead1(2);                  // '?>'
    consume(37);                    // '?>'
    eventHandler.endNonterminal("ProcessingInstruction", e0);
  }

  private void parse_Production()
  {
    eventHandler.startNonterminal("Production", e0);
    if (l1 == 15)                   // DocComment
    {
      consume(15);                  // DocComment
    }
    lookahead1W(10);                // Whitespace | NCName | '.'
    switch (l1)
    {
    case 3:                         // NCName
      consume(3);                   // NCName
      break;
    default:
      consume(27);                  // '.'
    }
    lookahead1W(15);                // Whitespace | '::=' | '?'
    if (l1 == 36)                   // '?'
    {
      consume(36);                  // '?'
    }
    lookahead1W(4);                 // Whitespace | '::='
    consume(29);                    // '::='
    lookahead1W(39);                // Whitespace | NCName | StringLiteral | CharCode | UrlIntroducer | WsExplicit |
                                    // WsDefinition | DocComment | EOF | EquivalenceLookAhead | '$' | '&' | '(' | '.' |
                                    // '/' | '<?' | '<?ENCORE?>' | '<?TOKENS?>' | '[' | '[^' | '|'
    switch (l1)
    {
    case 10:                        // UrlIntroducer
      whitespace();
      parse_Link();
      break;
    default:
      whitespace();
      parse_Alternatives();
    }
    lookahead1W(27);                // Whitespace | NCName | StringLiteral | WsExplicit | WsDefinition | DocComment |
                                    // EOF | EquivalenceLookAhead | '.' | '<?ENCORE?>' | '<?TOKENS?>'
    if (l1 == 12                    // WsExplicit
     || l1 == 13)                   // WsDefinition
    {
      whitespace();
      parse_Option();
    }
    eventHandler.endNonterminal("Production", e0);
  }

  private void parse_Alternatives()
  {
    eventHandler.startNonterminal("Alternatives", e0);
    parse_Alternative();
    lookahead1W(33);                // Whitespace | NCName | StringLiteral | WsExplicit | WsDefinition | DocComment |
                                    // EOF | EquivalenceLookAhead | '.' | '/' | '<?ENCORE?>' | '<?TOKENS?>' | '|'
    if (l1 == 28                    // '/'
     || l1 == 42)                   // '|'
    {
      switch (l1)
      {
      case 42:                      // '|'
        for (;;)
        {
          consume(42);              // '|'
          lookahead1W(37);          // Whitespace | NCName | StringLiteral | CharCode | WsExplicit | WsDefinition |
                                    // DocComment | EOF | EquivalenceLookAhead | '$' | '&' | '(' | '.' | '<?' |
                                    // '<?ENCORE?>' | '<?TOKENS?>' | '[' | '[^' | '|'
          whitespace();
          parse_Alternative();
          lookahead1W(31);          // Whitespace | NCName | StringLiteral | WsExplicit | WsDefinition | DocComment |
                                    // EOF | EquivalenceLookAhead | '.' | '<?ENCORE?>' | '<?TOKENS?>' | '|'
          if (l1 != 42)             // '|'
          {
            break;
          }
        }
        break;
      default:
        for (;;)
        {
          consume(28);              // '/'
          lookahead1W(36);          // Whitespace | NCName | StringLiteral | CharCode | WsExplicit | WsDefinition |
                                    // DocComment | EOF | EquivalenceLookAhead | '$' | '&' | '(' | '.' | '/' | '<?' |
                                    // '<?ENCORE?>' | '<?TOKENS?>' | '[' | '[^'
          whitespace();
          parse_Alternative();
          lookahead1W(30);          // Whitespace | NCName | StringLiteral | WsExplicit | WsDefinition | DocComment |
                                    // EOF | EquivalenceLookAhead | '.' | '/' | '<?ENCORE?>' | '<?TOKENS?>'
          if (l1 != 28)             // '/'
          {
            break;
          }
        }
      }
    }
    eventHandler.endNonterminal("Alternatives", e0);
  }

  private void parse_Alternative()
  {
    eventHandler.startNonterminal("Alternative", e0);
    parse_CompositeExpression();
    lookahead1W(35);                // Whitespace | NCName | StringLiteral | WsExplicit | WsDefinition | DocComment |
                                    // EOF | EquivalenceLookAhead | '&' | '.' | '/' | '<?ENCORE?>' | '<?TOKENS?>' | '|'
    if (l1 == 19)                   // '&'
    {
      consume(19);                  // '&'
      lookahead1W(38);              // Whitespace | NCName | StringLiteral | CharCode | WsExplicit | WsDefinition |
                                    // DocComment | EOF | EquivalenceLookAhead | '$' | '(' | '.' | '/' | '<?' |
                                    // '<?ENCORE?>' | '<?TOKENS?>' | '[' | '[^' | '|'
      whitespace();
      parse_CompositeExpression();
    }
    eventHandler.endNonterminal("Alternative", e0);
  }

  private void parse_CompositeExpression()
  {
    eventHandler.startNonterminal("CompositeExpression", e0);
    switch (l1)
    {
    case 3:                         // NCName
      lookahead2W(52);              // Whitespace | NCName | Context | StringLiteral | CharCode | WsExplicit |
                                    // WsDefinition | DocComment | EOF | EquivalenceLookAhead | '$' | '&' | '(' | ')' |
                                    // '*' | '**' | '+' | '++' | '-' | '.' | '/' | '::=' | '<<' | '<?' | '<?ENCORE?>' |
                                    // '<?TOKENS?>' | '>>' | '?' | '[' | '[^' | '\\' | '|'
      switch (lk)
      {
      case 259:                     // NCName Context
        lookahead3W(50);            // Whitespace | NCName | StringLiteral | CharCode | WsExplicit | WsDefinition |
                                    // DocComment | EOF | EquivalenceLookAhead | '$' | '&' | '(' | ')' | '*' | '**' |
                                    // '+' | '++' | '-' | '.' | '/' | '<<' | '<?' | '<?ENCORE?>' | '<?TOKENS?>' | '>>' |
                                    // '?' | '[' | '[^' | '|'
        break;
      case 2307:                    // NCName '?'
        lookahead3W(44);            // Whitespace | NCName | StringLiteral | CharCode | WsExplicit | WsDefinition |
                                    // DocComment | EOF | EquivalenceLookAhead | '$' | '&' | '(' | ')' | '**' | '++' |
                                    // '-' | '.' | '/' | '::=' | '<?' | '<?ENCORE?>' | '<?TOKENS?>' | '[' | '[^' | '|'
        break;
      }
      break;
    case 5:                         // StringLiteral
      lookahead2W(51);              // Whitespace | NCName | Context | StringLiteral | CharCode | WsExplicit |
                                    // WsDefinition | DocComment | EOF | EquivalenceLookAhead | '$' | '&' | '(' | ')' |
                                    // '*' | '**' | '+' | '++' | '-' | '.' | '/' | '<<' | '<?' | '<?ENCORE?>' |
                                    // '<?TOKENS?>' | '>>' | '?' | '[' | '[^' | '|'
      switch (lk)
      {
      case 261:                     // StringLiteral Context
        lookahead3W(50);            // Whitespace | NCName | StringLiteral | CharCode | WsExplicit | WsDefinition |
                                    // DocComment | EOF | EquivalenceLookAhead | '$' | '&' | '(' | ')' | '*' | '**' |
                                    // '+' | '++' | '-' | '.' | '/' | '<<' | '<?' | '<?ENCORE?>' | '<?TOKENS?>' | '>>' |
                                    // '?' | '[' | '[^' | '|'
        break;
      }
      break;
    case 27:                        // '.'
      lookahead2W(48);              // Whitespace | NCName | StringLiteral | CharCode | WsExplicit | WsDefinition |
                                    // DocComment | EOF | EquivalenceLookAhead | '$' | '&' | '(' | ')' | '*' | '**' |
                                    // '+' | '++' | '-' | '.' | '/' | '::=' | '<?' | '<?ENCORE?>' | '<?TOKENS?>' | '?' |
                                    // '[' | '[^' | '|'
      switch (lk)
      {
      case 2331:                    // '.' '?'
        lookahead3W(44);            // Whitespace | NCName | StringLiteral | CharCode | WsExplicit | WsDefinition |
                                    // DocComment | EOF | EquivalenceLookAhead | '$' | '&' | '(' | ')' | '**' | '++' |
                                    // '-' | '.' | '/' | '::=' | '<?' | '<?ENCORE?>' | '<?TOKENS?>' | '[' | '[^' | '|'
        break;
      }
      break;
    default:
      lk = l1;
    }
    if (lk != 12                    // WsExplicit
     && lk != 13                    // WsDefinition
     && lk != 15                    // DocComment
     && lk != 16                    // EOF
     && lk != 17                    // EquivalenceLookAhead
     && lk != 19                    // '&'
     && lk != 21                    // ')'
     && lk != 28                    // '/'
     && lk != 32                    // '<?ENCORE?>'
     && lk != 33                    // '<?TOKENS?>'
     && lk != 42                    // '|'
     && lk != 1859                  // NCName '::='
     && lk != 1883                  // '.' '::='
     && lk != 1923                  // NCName '<<'
     && lk != 1925                  // StringLiteral '<<'
     && lk != 2243                  // NCName '>>'
     && lk != 2245                  // StringLiteral '>>'
     && lk != 2563                  // NCName '\\'
     && lk != 121091                // NCName '?' '::='
     && lk != 121115                // '.' '?' '::='
     && lk != 123139                // NCName Context '<<'
     && lk != 123141                // StringLiteral Context '<<'
     && lk != 143619                // NCName Context '>>'
     && lk != 143621)               // StringLiteral Context '>>'
    {
      whitespace();
      parse_Item();
      lookahead1W(42);              // Whitespace | NCName | StringLiteral | CharCode | WsExplicit | WsDefinition |
                                    // DocComment | EOF | EquivalenceLookAhead | '$' | '&' | '(' | ')' | '**' | '++' |
                                    // '-' | '.' | '/' | '<?' | '<?ENCORE?>' | '<?TOKENS?>' | '[' | '[^' | '|'
      switch (l1)
      {
      case 26:                      // '-'
        consume(26);                // '-'
        lookahead1W(24);            // Whitespace | NCName | StringLiteral | CharCode | '$' | '(' | '.' | '<?' | '[' |
                                    // '[^'
        whitespace();
        parse_Item();
        break;
      case 23:                      // '**'
        consume(23);                // '**'
        lookahead1W(24);            // Whitespace | NCName | StringLiteral | CharCode | '$' | '(' | '.' | '<?' | '[' |
                                    // '[^'
        whitespace();
        parse_Item();
        break;
      case 25:                      // '++'
        consume(25);                // '++'
        lookahead1W(24);            // Whitespace | NCName | StringLiteral | CharCode | '$' | '(' | '.' | '<?' | '[' |
                                    // '[^'
        whitespace();
        parse_Item();
        break;
      default:
        for (;;)
        {
          lookahead1W(40);          // Whitespace | NCName | StringLiteral | CharCode | WsExplicit | WsDefinition |
                                    // DocComment | EOF | EquivalenceLookAhead | '$' | '&' | '(' | ')' | '.' | '/' |
                                    // '<?' | '<?ENCORE?>' | '<?TOKENS?>' | '[' | '[^' | '|'
          switch (l1)
          {
          case 3:                   // NCName
            lookahead2W(49);        // Whitespace | NCName | Context | StringLiteral | CharCode | WsExplicit |
                                    // WsDefinition | DocComment | EOF | EquivalenceLookAhead | '$' | '&' | '(' | ')' |
                                    // '*' | '+' | '.' | '/' | '::=' | '<<' | '<?' | '<?ENCORE?>' | '<?TOKENS?>' |
                                    // '>>' | '?' | '[' | '[^' | '\\' | '|'
            switch (lk)
            {
            case 259:               // NCName Context
              lookahead3W(45);      // Whitespace | NCName | StringLiteral | CharCode | WsExplicit | WsDefinition |
                                    // DocComment | EOF | EquivalenceLookAhead | '$' | '&' | '(' | ')' | '*' | '+' |
                                    // '.' | '/' | '<<' | '<?' | '<?ENCORE?>' | '<?TOKENS?>' | '>>' | '?' | '[' | '[^' |
                                    // '|'
              break;
            case 2307:              // NCName '?'
              lookahead3W(41);      // Whitespace | NCName | StringLiteral | CharCode | WsExplicit | WsDefinition |
                                    // DocComment | EOF | EquivalenceLookAhead | '$' | '&' | '(' | ')' | '.' | '/' |
                                    // '::=' | '<?' | '<?ENCORE?>' | '<?TOKENS?>' | '[' | '[^' | '|'
              break;
            }
            break;
          case 5:                   // StringLiteral
            lookahead2W(46);        // Whitespace | NCName | Context | StringLiteral | CharCode | WsExplicit |
                                    // WsDefinition | DocComment | EOF | EquivalenceLookAhead | '$' | '&' | '(' | ')' |
                                    // '*' | '+' | '.' | '/' | '<<' | '<?' | '<?ENCORE?>' | '<?TOKENS?>' | '>>' | '?' |
                                    // '[' | '[^' | '|'
            switch (lk)
            {
            case 261:               // StringLiteral Context
              lookahead3W(45);      // Whitespace | NCName | StringLiteral | CharCode | WsExplicit | WsDefinition |
                                    // DocComment | EOF | EquivalenceLookAhead | '$' | '&' | '(' | ')' | '*' | '+' |
                                    // '.' | '/' | '<<' | '<?' | '<?ENCORE?>' | '<?TOKENS?>' | '>>' | '?' | '[' | '[^' |
                                    // '|'
              break;
            }
            break;
          case 27:                  // '.'
            lookahead2W(43);        // Whitespace | NCName | StringLiteral | CharCode | WsExplicit | WsDefinition |
                                    // DocComment | EOF | EquivalenceLookAhead | '$' | '&' | '(' | ')' | '*' | '+' |
                                    // '.' | '/' | '::=' | '<?' | '<?ENCORE?>' | '<?TOKENS?>' | '?' | '[' | '[^' | '|'
            switch (lk)
            {
            case 2331:              // '.' '?'
              lookahead3W(41);      // Whitespace | NCName | StringLiteral | CharCode | WsExplicit | WsDefinition |
                                    // DocComment | EOF | EquivalenceLookAhead | '$' | '&' | '(' | ')' | '.' | '/' |
                                    // '::=' | '<?' | '<?ENCORE?>' | '<?TOKENS?>' | '[' | '[^' | '|'
              break;
            }
            break;
          default:
            lk = l1;
          }
          if (lk == 12              // WsExplicit
           || lk == 13              // WsDefinition
           || lk == 15              // DocComment
           || lk == 16              // EOF
           || lk == 17              // EquivalenceLookAhead
           || lk == 19              // '&'
           || lk == 21              // ')'
           || lk == 28              // '/'
           || lk == 32              // '<?ENCORE?>'
           || lk == 33              // '<?TOKENS?>'
           || lk == 42              // '|'
           || lk == 1859            // NCName '::='
           || lk == 1883            // '.' '::='
           || lk == 1923            // NCName '<<'
           || lk == 1925            // StringLiteral '<<'
           || lk == 2243            // NCName '>>'
           || lk == 2245            // StringLiteral '>>'
           || lk == 2563            // NCName '\\'
           || lk == 121091          // NCName '?' '::='
           || lk == 121115          // '.' '?' '::='
           || lk == 123139          // NCName Context '<<'
           || lk == 123141          // StringLiteral Context '<<'
           || lk == 143619          // NCName Context '>>'
           || lk == 143621)         // StringLiteral Context '>>'
          {
            break;
          }
          whitespace();
          parse_Item();
        }
      }
    }
    eventHandler.endNonterminal("CompositeExpression", e0);
  }

  private void parse_Item()
  {
    eventHandler.startNonterminal("Item", e0);
    parse_Primary();
    lookahead1W(47);                // Whitespace | NCName | StringLiteral | CharCode | WsExplicit | WsDefinition |
                                    // DocComment | EOF | EquivalenceLookAhead | '$' | '&' | '(' | ')' | '*' | '**' |
                                    // '+' | '++' | '-' | '.' | '/' | '<?' | '<?ENCORE?>' | '<?TOKENS?>' | '?' | '[' |
                                    // '[^' | '|'
    if (l1 == 22                    // '*'
     || l1 == 24                    // '+'
     || l1 == 36)                   // '?'
    {
      switch (l1)
      {
      case 36:                      // '?'
        consume(36);                // '?'
        break;
      case 22:                      // '*'
        consume(22);                // '*'
        break;
      default:
        consume(24);                // '+'
      }
    }
    eventHandler.endNonterminal("Item", e0);
  }

  private void parse_Primary()
  {
    eventHandler.startNonterminal("Primary", e0);
    switch (l1)
    {
    case 3:                         // NCName
    case 5:                         // StringLiteral
      parse_NameOrString();
      break;
    case 31:                        // '<?'
      parse_ProcessingInstruction();
      break;
    case 6:                         // CharCode
      consume(6);                   // CharCode
      break;
    case 18:                        // '$'
      consume(18);                  // '$'
      break;
    case 27:                        // '.'
      consume(27);                  // '.'
      break;
    case 20:                        // '('
      consume(20);                  // '('
      lookahead1W(32);              // Whitespace | NCName | StringLiteral | CharCode | '$' | '(' | ')' | '.' | '/' |
                                    // '<?' | '[' | '[^' | '|'
      whitespace();
      parse_Choice();
      consume(21);                  // ')'
      break;
    default:
      parse_CharClass();
    }
    eventHandler.endNonterminal("Primary", e0);
  }

  private void parse_Choice()
  {
    eventHandler.startNonterminal("Choice", e0);
    parse_CompositeExpression();
    lookahead1W(17);                // Whitespace | ')' | '/' | '|'
    if (l1 != 21)                   // ')'
    {
      switch (l1)
      {
      case 42:                      // '|'
        for (;;)
        {
          consume(42);              // '|'
          lookahead1W(29);          // Whitespace | NCName | StringLiteral | CharCode | '$' | '(' | ')' | '.' | '<?' |
                                    // '[' | '[^' | '|'
          whitespace();
          parse_CompositeExpression();
          lookahead1W(14);          // Whitespace | ')' | '|'
          if (l1 != 42)             // '|'
          {
            break;
          }
        }
        break;
      default:
        for (;;)
        {
          consume(28);              // '/'
          lookahead1W(28);          // Whitespace | NCName | StringLiteral | CharCode | '$' | '(' | ')' | '.' | '/' |
                                    // '<?' | '[' | '[^'
          whitespace();
          parse_CompositeExpression();
          lookahead1W(13);          // Whitespace | ')' | '/'
          if (l1 != 28)             // '/'
          {
            break;
          }
        }
      }
    }
    eventHandler.endNonterminal("Choice", e0);
  }

  private void parse_NameOrString()
  {
    eventHandler.startNonterminal("NameOrString", e0);
    switch (l1)
    {
    case 3:                         // NCName
      consume(3);                   // NCName
      lookahead1W(51);              // Whitespace | NCName | Context | StringLiteral | CharCode | WsExplicit |
                                    // WsDefinition | DocComment | EOF | EquivalenceLookAhead | '$' | '&' | '(' | ')' |
                                    // '*' | '**' | '+' | '++' | '-' | '.' | '/' | '<<' | '<?' | '<?ENCORE?>' |
                                    // '<?TOKENS?>' | '>>' | '?' | '[' | '[^' | '|'
      if (l1 == 4)                  // Context
      {
        consume(4);                 // Context
      }
      break;
    default:
      consume(5);                   // StringLiteral
      lookahead1W(51);              // Whitespace | NCName | Context | StringLiteral | CharCode | WsExplicit |
                                    // WsDefinition | DocComment | EOF | EquivalenceLookAhead | '$' | '&' | '(' | ')' |
                                    // '*' | '**' | '+' | '++' | '-' | '.' | '/' | '<<' | '<?' | '<?ENCORE?>' |
                                    // '<?TOKENS?>' | '>>' | '?' | '[' | '[^' | '|'
      if (l1 == 4)                  // Context
      {
        consume(4);                 // Context
      }
    }
    eventHandler.endNonterminal("NameOrString", e0);
  }

  private void parse_CharClass()
  {
    eventHandler.startNonterminal("CharClass", e0);
    switch (l1)
    {
    case 38:                        // '['
      consume(38);                  // '['
      break;
    default:
      consume(39);                  // '[^'
    }
    for (;;)
    {
      lookahead1(18);               // CharCode | Char | CharRange | CharCodeRange
      switch (l1)
      {
      case 7:                       // Char
        consume(7);                 // Char
        break;
      case 6:                       // CharCode
        consume(6);                 // CharCode
        break;
      case 8:                       // CharRange
        consume(8);                 // CharRange
        break;
      default:
        consume(9);                 // CharCodeRange
      }
      lookahead1(20);               // CharCode | Char | CharRange | CharCodeRange | ']'
      if (l1 == 41)                 // ']'
      {
        break;
      }
    }
    consume(41);                    // ']'
    eventHandler.endNonterminal("CharClass", e0);
  }

  private void parse_Link()
  {
    eventHandler.startNonterminal("Link", e0);
    consume(10);                    // UrlIntroducer
    lookahead1(1);                  // URL
    consume(11);                    // URL
    lookahead1(3);                  // ']'
    consume(41);                    // ']'
    eventHandler.endNonterminal("Link", e0);
  }

  private void parse_Option()
  {
    eventHandler.startNonterminal("Option", e0);
    switch (l1)
    {
    case 12:                        // WsExplicit
      consume(12);                  // WsExplicit
      break;
    default:
      consume(13);                  // WsDefinition
    }
    eventHandler.endNonterminal("Option", e0);
  }

  private void parse_Preference()
  {
    eventHandler.startNonterminal("Preference", e0);
    parse_NameOrString();
    lookahead1W(16);                // Whitespace | '<<' | '>>'
    switch (l1)
    {
    case 35:                        // '>>'
      consume(35);                  // '>>'
      break;
    default:
      consume(30);                  // '<<'
    }
    for (;;)
    {
      lookahead1W(9);               // Whitespace | NCName | StringLiteral
      whitespace();
      parse_NameOrString();
      lookahead1W(23);              // Whitespace | NCName | StringLiteral | DocComment | EOF | EquivalenceLookAhead |
                                    // '.' | '<?ENCORE?>'
      switch (l1)
      {
      case 3:                       // NCName
        lookahead2W(34);            // Whitespace | NCName | Context | StringLiteral | DocComment | EOF |
                                    // EquivalenceLookAhead | '.' | '::=' | '<<' | '<?ENCORE?>' | '>>' | '?' | '\\'
        switch (lk)
        {
        case 259:                   // NCName Context
          lookahead3W(25);          // Whitespace | NCName | StringLiteral | DocComment | EOF | EquivalenceLookAhead |
                                    // '.' | '<<' | '<?ENCORE?>' | '>>'
          break;
        }
        break;
      case 5:                       // StringLiteral
        lookahead2W(26);            // Whitespace | NCName | Context | StringLiteral | DocComment | EOF |
                                    // EquivalenceLookAhead | '.' | '<<' | '<?ENCORE?>' | '>>'
        switch (lk)
        {
        case 261:                   // StringLiteral Context
          lookahead3W(25);          // Whitespace | NCName | StringLiteral | DocComment | EOF | EquivalenceLookAhead |
                                    // '.' | '<<' | '<?ENCORE?>' | '>>'
          break;
        }
        break;
      default:
        lk = l1;
      }
      if (lk == 15                  // DocComment
       || lk == 16                  // EOF
       || lk == 17                  // EquivalenceLookAhead
       || lk == 27                  // '.'
       || lk == 32                  // '<?ENCORE?>'
       || lk == 1859                // NCName '::='
       || lk == 1923                // NCName '<<'
       || lk == 1925                // StringLiteral '<<'
       || lk == 2243                // NCName '>>'
       || lk == 2245                // StringLiteral '>>'
       || lk == 2307                // NCName '?'
       || lk == 2563                // NCName '\\'
       || lk == 123139              // NCName Context '<<'
       || lk == 123141              // StringLiteral Context '<<'
       || lk == 143619              // NCName Context '>>'
       || lk == 143621)             // StringLiteral Context '>>'
      {
        break;
      }
    }
    eventHandler.endNonterminal("Preference", e0);
  }

  private void parse_Delimiter()
  {
    eventHandler.startNonterminal("Delimiter", e0);
    consume(3);                     // NCName
    lookahead1W(6);                 // Whitespace | '\\'
    consume(40);                    // '\\'
    for (;;)
    {
      lookahead1W(9);               // Whitespace | NCName | StringLiteral
      whitespace();
      parse_NameOrString();
      lookahead1W(23);              // Whitespace | NCName | StringLiteral | DocComment | EOF | EquivalenceLookAhead |
                                    // '.' | '<?ENCORE?>'
      switch (l1)
      {
      case 3:                       // NCName
        lookahead2W(34);            // Whitespace | NCName | Context | StringLiteral | DocComment | EOF |
                                    // EquivalenceLookAhead | '.' | '::=' | '<<' | '<?ENCORE?>' | '>>' | '?' | '\\'
        switch (lk)
        {
        case 259:                   // NCName Context
          lookahead3W(25);          // Whitespace | NCName | StringLiteral | DocComment | EOF | EquivalenceLookAhead |
                                    // '.' | '<<' | '<?ENCORE?>' | '>>'
          break;
        }
        break;
      case 5:                       // StringLiteral
        lookahead2W(26);            // Whitespace | NCName | Context | StringLiteral | DocComment | EOF |
                                    // EquivalenceLookAhead | '.' | '<<' | '<?ENCORE?>' | '>>'
        switch (lk)
        {
        case 261:                   // StringLiteral Context
          lookahead3W(25);          // Whitespace | NCName | StringLiteral | DocComment | EOF | EquivalenceLookAhead |
                                    // '.' | '<<' | '<?ENCORE?>' | '>>'
          break;
        }
        break;
      default:
        lk = l1;
      }
      if (lk == 15                  // DocComment
       || lk == 16                  // EOF
       || lk == 17                  // EquivalenceLookAhead
       || lk == 27                  // '.'
       || lk == 32                  // '<?ENCORE?>'
       || lk == 1859                // NCName '::='
       || lk == 1923                // NCName '<<'
       || lk == 1925                // StringLiteral '<<'
       || lk == 2243                // NCName '>>'
       || lk == 2245                // StringLiteral '>>'
       || lk == 2307                // NCName '?'
       || lk == 2563                // NCName '\\'
       || lk == 123139              // NCName Context '<<'
       || lk == 123141              // StringLiteral Context '<<'
       || lk == 143619              // NCName Context '>>'
       || lk == 143621)             // StringLiteral Context '>>'
      {
        break;
      }
    }
    eventHandler.endNonterminal("Delimiter", e0);
  }

  private void parse_Equivalence()
  {
    eventHandler.startNonterminal("Equivalence", e0);
    consume(17);                    // EquivalenceLookAhead
    lookahead1W(11);                // Whitespace | StringLiteral | '['
    whitespace();
    parse_EquivalenceCharRange();
    lookahead1W(5);                 // Whitespace | '=='
    consume(34);                    // '=='
    lookahead1W(11);                // Whitespace | StringLiteral | '['
    whitespace();
    parse_EquivalenceCharRange();
    eventHandler.endNonterminal("Equivalence", e0);
  }

  private void parse_EquivalenceCharRange()
  {
    eventHandler.startNonterminal("EquivalenceCharRange", e0);
    switch (l1)
    {
    case 5:                         // StringLiteral
      consume(5);                   // StringLiteral
      break;
    default:
      consume(38);                  // '['
      lookahead1(18);               // CharCode | Char | CharRange | CharCodeRange
      switch (l1)
      {
      case 7:                       // Char
        consume(7);                 // Char
        break;
      case 6:                       // CharCode
        consume(6);                 // CharCode
        break;
      case 8:                       // CharRange
        consume(8);                 // CharRange
        break;
      default:
        consume(9);                 // CharCodeRange
      }
      lookahead1(3);                // ']'
      consume(41);                  // ']'
    }
    eventHandler.endNonterminal("EquivalenceCharRange", e0);
  }

  private void consume(int t)
  {
    if (l1 == t)
    {
      whitespace();
      eventHandler.terminal(TOKEN[l1], b1, e1);
      b0 = b1; e0 = e1; l1 = l2; if (l1 != 0) {
      b1 = b2; e1 = e2; l2 = l3; if (l2 != 0) {
      b2 = b3; e2 = e3; l3 = 0; }}
    }
    else
    {
      error(b1, e1, 0, l1, t);
    }
  }

  private void whitespace()
  {
    if (e0 != b1)
    {
      eventHandler.whitespace(e0, b1);
      e0 = b1;
    }
  }

  private int matchW(int tokenSetId)
  {
    int code;
    for (;;)
    {
      code = match(tokenSetId);
      if (code != 1)                // Whitespace
      {
        break;
      }
    }
    return code;
  }

  private void lookahead1W(int tokenSetId)
  {
    if (l1 == 0)
    {
      l1 = matchW(tokenSetId);
      b1 = begin;
      e1 = end;
    }
  }

  private void lookahead2W(int tokenSetId)
  {
    if (l2 == 0)
    {
      l2 = matchW(tokenSetId);
      b2 = begin;
      e2 = end;
    }
    lk = (l2 << 6) | l1;
  }

  private void lookahead3W(int tokenSetId)
  {
    if (l3 == 0)
    {
      l3 = matchW(tokenSetId);
      b3 = begin;
      e3 = end;
    }
    lk |= l3 << 12;
  }

  private void lookahead1(int tokenSetId)
  {
    if (l1 == 0)
    {
      l1 = match(tokenSetId);
      b1 = begin;
      e1 = end;
    }
  }

  private int error(int b, int e, int s, int l, int t)
  {
    throw new ParseException(b, e, s, l, t);
  }

  private int lk, b0, e0;
  private int l1, b1, e1;
  private int l2, b2, e2;
  private int l3, b3, e3;
  private EventHandler eventHandler = null;
  private CharSequence input = null;
  private int size = 0;
  private int begin = 0;
  private int end = 0;

  private int match(int tokenSetId)
  {
    boolean nonbmp = false;
    begin = end;
    int current = end;
    int result = INITIAL[tokenSetId];
    int state = 0;

    for (int code = result & 255; code != 0; )
    {
      int charclass;
      int c0 = current < size ? input.charAt(current) : 0;
      ++current;
      if (c0 < 0x80)
      {
        charclass = MAP0[c0];
      }
      else if (c0 < 0xd800)
      {
        int c1 = c0 >> 3;
        charclass = MAP1[(c0 & 7) + MAP1[(c1 & 31) + MAP1[c1 >> 5]]];
      }
      else
      {
        if (c0 < 0xdc00)
        {
          int c1 = current < size ? input.charAt(current) : 0;
          if (c1 >= 0xdc00 && c1 < 0xe000)
          {
            nonbmp = true;
            ++current;
            c0 = ((c0 & 0x3ff) << 10) + (c1 & 0x3ff) + 0x10000;
          }
        }

        int lo = 0, hi = 1;
        for (int m = 1; ; m = (hi + lo) >> 1)
        {
          if (MAP2[m] > c0) {hi = m - 1;}
          else if (MAP2[2 + m] < c0) {lo = m + 1;}
          else {charclass = MAP2[4 + m]; break;}
          if (lo > hi) {charclass = 0; break;}
        }
      }

      state = code;
      int i0 = (charclass << 8) + code - 1;
      code = TRANSITION[(i0 & 7) + TRANSITION[i0 >> 3]];

      if (code > 255)
      {
        result = code;
        code &= 255;
        end = current;
      }
    }

    result >>= 8;
    if (result == 0)
    {
      end = current - 1;
      int c1 = end < size ? input.charAt(end) : 0;
      if (c1 >= 0xdc00 && c1 < 0xe000)
      {
        --end;
      }
      return error(begin, end, state, -1, -1);
    }
    else if ((result & 64) != 0)
    {
      end = begin;
      if (nonbmp)
      {
        for (int i = result >> 7; i > 0; --i)
        {
          int c1 = end < size ? input.charAt(end) : 0;
          ++end;
          if (c1 >= 0xd800 && c1 < 0xdc000)
          {
            ++end;
          }
        }
      }
      else
      {
        end += (result >> 7);
      }
    }
    else if (nonbmp)
    {
      for (int i = result >> 7; i > 0; --i)
      {
        --end;
        int c1 = end < size ? input.charAt(end) : 0;
        if (c1 >= 0xdc00 && c1 < 0xe000)
        {
          --end;
        }
      }
    }
    else
    {
      end -= result >> 7;
    }

    if (end > size) end = size;
    return (result & 63) - 1;
  }

  private static String[] getTokenSet(int tokenSetId)
  {
    java.util.ArrayList<String> expected = new java.util.ArrayList<>();
    int s = tokenSetId < 0 ? - tokenSetId : INITIAL[tokenSetId] & 255;
    for (int i = 0; i < 43; i += 32)
    {
      int j = i;
      int i0 = (i >> 5) * 178 + s - 1;
      int f = EXPECTED[(i0 & 3) + EXPECTED[i0 >> 2]];
      for ( ; f != 0; f >>>= 1, ++j)
      {
        if ((f & 1) != 0)
        {
          expected.add(TOKEN[j]);
        }
      }
    }
    return expected.toArray(new String[]{});
  }

  private static final int[] MAP0 =
  {
    /*   0 */ 52, 0, 0, 0, 0, 0, 0, 0, 0, 1, 2, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3, 4, 5,
    /*  35 */ 6, 7, 4, 8, 9, 10, 11, 12, 13, 4, 14, 15, 16, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 18, 4, 19, 20, 21,
    /*  63 */ 22, 4, 23, 23, 24, 23, 25, 23, 26, 26, 26, 26, 27, 26, 26, 28, 29, 26, 26, 30, 31, 32, 26, 26, 26, 26, 26,
    /*  90 */ 26, 33, 34, 35, 36, 26, 4, 23, 23, 37, 38, 39, 40, 26, 26, 41, 26, 26, 42, 26, 43, 44, 45, 26, 26, 46, 47,
    /* 117 */ 26, 26, 48, 49, 26, 26, 4, 50, 4, 4, 4
  };

  private static final int[] MAP1 =
  {
    /*    0 */ 216, 291, 323, 383, 415, 908, 351, 815, 815, 447, 479, 511, 543, 575, 621, 882, 589, 681, 815, 815, 815,
    /*   21 */ 815, 815, 815, 815, 815, 815, 815, 815, 815, 713, 745, 821, 649, 815, 815, 815, 815, 815, 815, 815, 815,
    /*   42 */ 815, 815, 815, 815, 815, 815, 777, 809, 815, 815, 815, 815, 815, 815, 815, 815, 815, 815, 815, 815, 815,
    /*   63 */ 815, 815, 815, 815, 815, 815, 815, 815, 815, 815, 815, 815, 815, 815, 815, 247, 247, 247, 247, 247, 247,
    /*   84 */ 247, 247, 247, 247, 247, 247, 247, 247, 247, 247, 247, 247, 247, 247, 247, 247, 247, 247, 247, 247, 247,
    /*  105 */ 247, 247, 247, 247, 247, 247, 247, 247, 247, 247, 247, 247, 247, 247, 247, 247, 247, 247, 247, 247, 247,
    /*  126 */ 247, 247, 247, 247, 247, 247, 247, 247, 247, 247, 247, 247, 247, 247, 247, 247, 247, 247, 247, 247, 247,
    /*  147 */ 247, 247, 247, 247, 247, 247, 247, 247, 247, 247, 247, 247, 259, 815, 815, 815, 815, 815, 815, 815, 815,
    /*  168 */ 815, 815, 815, 815, 247, 247, 247, 247, 247, 247, 247, 247, 247, 247, 247, 247, 247, 247, 247, 247, 247,
    /*  189 */ 247, 247, 247, 247, 247, 247, 247, 247, 247, 247, 247, 247, 247, 247, 247, 247, 247, 247, 247, 247, 247,
    /*  210 */ 247, 247, 247, 247, 247, 853, 940, 949, 941, 941, 957, 965, 973, 979, 987, 1010, 1018, 1035, 1053, 1071,
    /*  230 */ 1079, 1087, 1262, 1262, 1262, 1262, 1262, 1262, 1433, 1262, 1254, 1254, 1255, 1254, 1254, 1254, 1255,
    /*  247 */ 1254, 1254, 1254, 1254, 1254, 1254, 1254, 1254, 1254, 1254, 1254, 1254, 1254, 1254, 1254, 1254, 1254,
    /*  264 */ 1254, 1254, 1254, 1254, 1254, 1254, 1254, 1254, 1254, 1254, 1254, 1254, 1254, 1254, 1254, 1256, 1262,
    /*  281 */ 1262, 1262, 1262, 1262, 1262, 1262, 1262, 1262, 1262, 1262, 1254, 1254, 1254, 1254, 1254, 1254, 1342,
    /*  298 */ 1255, 1253, 1252, 1254, 1254, 1254, 1254, 1254, 1255, 1254, 1254, 1254, 1254, 1254, 1254, 1254, 1254,
    /*  315 */ 1258, 1418, 1254, 1254, 1254, 1254, 1062, 1421, 1254, 1254, 1254, 1262, 1262, 1262, 1262, 1262, 1262,
    /*  332 */ 1262, 1254, 1254, 1254, 1254, 1254, 1254, 1254, 1254, 1254, 1254, 1254, 1261, 1262, 1420, 1260, 1262,
    /*  349 */ 1388, 1262, 1262, 1262, 1262, 1262, 1253, 1254, 1254, 1259, 1131, 1308, 1387, 1262, 1382, 1388, 1131,
    /*  366 */ 1254, 1254, 1254, 1254, 1254, 1254, 1254, 1254, 1344, 1254, 1255, 1142, 1382, 1297, 1196, 1382, 1388,
    /*  383 */ 1382, 1382, 1382, 1382, 1382, 1382, 1382, 1382, 1384, 1262, 1262, 1262, 1388, 1262, 1262, 1262, 1367,
    /*  400 */ 1231, 1254, 1254, 1251, 1254, 1254, 1254, 1254, 1255, 1255, 1407, 1252, 1254, 1258, 1262, 1253, 1100,
    /*  417 */ 1254, 1254, 1254, 1254, 1254, 1254, 1254, 1254, 1253, 1100, 1254, 1254, 1254, 1254, 1109, 1262, 1254,
    /*  434 */ 1254, 1254, 1254, 1254, 1254, 1122, 1042, 1254, 1254, 1254, 1123, 1256, 1260, 1446, 1254, 1254, 1254,
    /*  451 */ 1254, 1254, 1254, 1160, 1382, 1384, 1197, 1254, 1178, 1382, 1262, 1262, 1446, 1122, 1343, 1254, 1254,
    /*  468 */ 1252, 1060, 1192, 1169, 1181, 1433, 1207, 1178, 1382, 1260, 1262, 1218, 1241, 1343, 1254, 1254, 1252,
    /*  485 */ 1397, 1192, 1184, 1181, 1262, 1229, 1434, 1382, 1239, 1262, 1446, 1230, 1251, 1254, 1254, 1252, 1249,
    /*  502 */ 1160, 1272, 1114, 1262, 1262, 994, 1382, 1262, 1262, 1446, 1122, 1343, 1254, 1254, 1252, 1340, 1160,
    /*  519 */ 1198, 1181, 1434, 1207, 1045, 1382, 1262, 1262, 1002, 1023, 1285, 1281, 1063, 1023, 1133, 1045, 1199,
    /*  536 */ 1196, 1433, 1262, 1433, 1382, 1262, 1262, 1446, 1100, 1252, 1254, 1254, 1252, 1101, 1045, 1273, 1196,
    /*  553 */ 1435, 1262, 1045, 1382, 1262, 1262, 1002, 1100, 1252, 1254, 1254, 1252, 1101, 1045, 1273, 1196, 1435,
    /*  570 */ 1264, 1045, 1382, 1262, 1262, 1002, 1100, 1252, 1254, 1254, 1252, 1254, 1045, 1170, 1196, 1433, 1262,
    /*  587 */ 1045, 1382, 1262, 1262, 1262, 1262, 1262, 1262, 1262, 1262, 1262, 1262, 1262, 1262, 1262, 1262, 1262,
    /*  604 */ 1262, 1262, 1262, 1262, 1262, 1254, 1254, 1254, 1254, 1256, 1262, 1254, 1254, 1254, 1254, 1255, 1262,
    /*  621 */ 1253, 1254, 1254, 1254, 1254, 1255, 1293, 1387, 1305, 1383, 1382, 1388, 1262, 1262, 1262, 1262, 1210,
    /*  638 */ 1317, 1419, 1253, 1327, 1337, 1293, 1152, 1352, 1384, 1382, 1388, 1262, 1262, 1262, 1262, 1264, 1027,
    /*  655 */ 1262, 1262, 1262, 1262, 1262, 1262, 1262, 1262, 1262, 1262, 1259, 1262, 1262, 1262, 1262, 1262, 1262,
    /*  672 */ 1262, 1262, 1262, 1262, 1262, 1262, 1262, 1262, 1262, 1249, 1396, 1259, 1262, 1262, 1262, 1262, 1405,
    /*  689 */ 1261, 1405, 1062, 1416, 1329, 1061, 1209, 1262, 1262, 1262, 1262, 1264, 1262, 1319, 1263, 1283, 1259,
    /*  706 */ 1262, 1262, 1262, 1262, 1429, 1261, 1431, 1254, 1254, 1254, 1254, 1254, 1254, 1254, 1254, 1254, 1254,
    /*  723 */ 1254, 1254, 1254, 1254, 1254, 1254, 1254, 1254, 1254, 1258, 1254, 1254, 1254, 1254, 1254, 1254, 1254,
    /*  740 */ 1254, 1254, 1254, 1254, 1260, 1254, 1254, 1256, 1256, 1254, 1254, 1254, 1254, 1256, 1256, 1254, 1408,
    /*  757 */ 1254, 1254, 1254, 1256, 1254, 1254, 1254, 1254, 1254, 1254, 1100, 1134, 1221, 1257, 1123, 1258, 1254,
    /*  774 */ 1257, 1221, 1257, 1092, 1262, 1262, 1262, 1253, 1309, 1168, 1262, 1253, 1254, 1254, 1254, 1254, 1254,
    /*  791 */ 1254, 1254, 1254, 1254, 1257, 999, 1253, 1254, 1254, 1254, 1254, 1254, 1254, 1254, 1254, 1254, 1254,
    /*  808 */ 1443, 1418, 1254, 1254, 1254, 1254, 1257, 1262, 1262, 1262, 1262, 1262, 1262, 1262, 1262, 1262, 1262,
    /*  825 */ 1262, 1262, 1262, 1262, 1262, 1262, 1262, 1262, 1262, 1262, 1262, 1262, 1262, 1262, 1262, 1262, 1262,
    /*  842 */ 1262, 1262, 1262, 1262, 1262, 1382, 1385, 1365, 1262, 1262, 1262, 1254, 1254, 1254, 1254, 1254, 1254,
    /*  859 */ 1254, 1254, 1254, 1254, 1254, 1254, 1254, 1254, 1254, 1254, 1254, 1254, 1254, 1254, 1258, 1262, 1262,
    /*  876 */ 1262, 1262, 1262, 1262, 1262, 1262, 1262, 1262, 1262, 1388, 1382, 1388, 1375, 1357, 1254, 1253, 1254,
    /*  893 */ 1254, 1254, 1260, 1381, 1382, 1273, 1386, 1272, 1381, 1382, 1384, 1381, 1365, 1262, 1262, 1262, 1262,
    /*  910 */ 1262, 1262, 1262, 1262, 1253, 1254, 1254, 1254, 1255, 1431, 1253, 1254, 1254, 1254, 1255, 1262, 1381,
    /*  927 */ 1382, 1166, 1382, 1382, 1148, 1362, 1262, 1254, 1254, 1254, 1259, 1259, 1262, 52, 0, 0, 0, 0, 0, 0, 0, 0,
    /*  949 */ 0, 1, 2, 0, 0, 1, 0, 0, 3, 4, 5, 6, 7, 4, 8, 9, 10, 11, 12, 13, 4, 14, 15, 16, 17, 17, 17, 17, 17, 17,
    /*  979 */ 17, 17, 18, 4, 19, 20, 21, 22, 4, 23, 23, 24, 23, 25, 23, 26, 4, 4, 4, 4, 4, 51, 51, 4, 4, 51, 51, 4, 26,
    /* 1008 */ 26, 26, 26, 26, 26, 27, 26, 26, 28, 29, 26, 26, 30, 31, 32, 26, 26, 26, 4, 4, 4, 26, 26, 4, 4, 26, 4, 26,
    /* 1036 */ 26, 26, 33, 34, 35, 36, 26, 4, 4, 26, 26, 4, 4, 4, 4, 51, 51, 4, 23, 23, 37, 38, 39, 40, 26, 4, 26, 4, 4,
    /* 1065 */ 4, 26, 26, 4, 4, 4, 26, 41, 26, 26, 42, 26, 43, 44, 45, 26, 26, 46, 47, 26, 26, 48, 49, 26, 26, 4, 50, 4,
    /* 1093 */ 4, 4, 4, 4, 51, 4, 26, 26, 26, 26, 26, 26, 4, 26, 26, 26, 26, 26, 4, 51, 51, 51, 51, 4, 51, 51, 51, 4, 4,
    /* 1122 */ 26, 26, 26, 26, 26, 4, 4, 26, 26, 51, 26, 26, 26, 26, 26, 26, 26, 4, 26, 4, 26, 26, 26, 26, 4, 26, 51,
    /* 1149 */ 51, 4, 51, 51, 51, 4, 51, 51, 26, 4, 4, 26, 26, 4, 4, 51, 26, 51, 51, 4, 51, 51, 51, 51, 51, 4, 4, 51,
    /* 1177 */ 51, 26, 26, 51, 51, 4, 4, 51, 51, 51, 4, 4, 4, 4, 51, 26, 26, 4, 4, 51, 4, 51, 51, 51, 51, 4, 4, 4, 51,
    /* 1206 */ 51, 4, 4, 4, 4, 26, 26, 4, 26, 4, 4, 26, 4, 4, 51, 4, 4, 26, 26, 26, 4, 26, 26, 4, 26, 26, 26, 26, 4, 26,
    /* 1236 */ 4, 26, 26, 51, 51, 26, 26, 26, 4, 4, 4, 4, 26, 26, 4, 26, 26, 4, 26, 26, 26, 26, 26, 26, 26, 26, 4, 4, 4,
    /* 1265 */ 4, 4, 4, 4, 4, 26, 4, 51, 51, 51, 51, 51, 51, 4, 51, 51, 4, 26, 26, 4, 26, 4, 26, 26, 26, 26, 4, 4, 26,
    /* 1294 */ 51, 26, 26, 51, 51, 51, 51, 51, 26, 26, 51, 26, 26, 26, 26, 26, 26, 51, 51, 51, 51, 51, 51, 26, 4, 26, 4,
    /* 1321 */ 4, 26, 4, 4, 26, 26, 4, 26, 26, 26, 4, 26, 4, 26, 4, 26, 4, 4, 26, 26, 4, 26, 26, 4, 4, 26, 26, 26, 26,
    /* 1350 */ 26, 4, 26, 26, 26, 26, 26, 4, 51, 4, 4, 4, 4, 51, 51, 4, 51, 4, 4, 4, 4, 4, 4, 26, 51, 4, 4, 4, 4, 4, 51,
    /* 1381 */ 4, 51, 51, 51, 51, 51, 51, 51, 51, 4, 4, 4, 4, 4, 4, 4, 26, 4, 26, 26, 4, 26, 26, 4, 4, 4, 4, 4, 26, 4,
    /* 1411 */ 26, 4, 26, 4, 26, 4, 26, 4, 4, 4, 4, 4, 26, 26, 26, 26, 26, 26, 4, 4, 4, 26, 4, 4, 4, 4, 4, 4, 4, 51, 51,
    /* 1442 */ 4, 26, 26, 26, 4, 51, 51, 51, 4, 26, 26, 26
  };

  private static final int[] MAP2 =
  {
    /* 0 */ 57344, 65536, 65533, 1114111, 4, 4
  };

  private static final int[] INITIAL =
  {
    /*  0 */ 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29,
    /* 29 */ 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53
  };

  private static final int[] TRANSITION =
  {
    /*    0 */ 2072, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 2072,
    /*   17 */ 2072, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 2516, 1696,
    /*   34 */ 1822, 1697, 1697, 1697, 1827, 1705, 2222, 2072, 2577, 3044, 2112, 1917, 1730, 1748, 1762, 1777, 2388,
    /*   51 */ 1769, 3273, 3153, 1789, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 2516, 1696, 1822, 1697,
    /*   68 */ 1697, 1697, 1827, 1705, 2222, 2072, 1754, 3044, 2112, 1917, 1730, 1748, 1799, 1777, 1805, 1769, 3273,
    /*   85 */ 3153, 1789, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 2516, 1696, 1816, 1697, 1697, 1697,
    /*  102 */ 1827, 1842, 1860, 2914, 1874, 1882, 2112, 1893, 1912, 1748, 1762, 1777, 2388, 1769, 3273, 3153, 1789,
    /*  119 */ 2072, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 1832, 2072, 2876, 2072, 2072, 2072, 1791, 3024,
    /*  136 */ 1860, 2914, 1874, 1882, 3267, 1958, 1912, 2030, 1762, 2391, 2388, 1769, 3273, 1903, 2427, 2072, 2072,
    /*  153 */ 2072, 2072, 2072, 2072, 2072, 2072, 2072, 1832, 2450, 2444, 1849, 1849, 1849, 1852, 2917, 1860, 2914,
    /*  170 */ 1874, 1882, 3267, 1958, 1912, 2030, 1762, 2391, 2388, 1769, 3273, 1903, 2427, 2072, 2072, 2072, 2072,
    /*  187 */ 2072, 2072, 2072, 2072, 2072, 2602, 2072, 2368, 1925, 1930, 1934, 1937, 3024, 1860, 3341, 1945, 1882,
    /*  204 */ 2351, 1958, 1953, 3042, 1971, 1885, 2169, 1769, 3273, 1903, 2427, 2072, 2072, 2072, 2072, 2072, 2072,
    /*  221 */ 2072, 2072, 2072, 1832, 2072, 2876, 1979, 1984, 1988, 1991, 3024, 1860, 2914, 1874, 1882, 3267, 1958,
    /*  238 */ 1912, 2030, 1762, 2391, 2388, 1769, 3273, 1903, 2427, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 2072,
    /*  255 */ 2072, 1832, 2072, 2876, 2072, 2174, 1999, 2002, 3024, 1860, 2914, 1874, 1882, 3267, 1958, 1912, 2030,
    /*  272 */ 1762, 2391, 2388, 1769, 3273, 1903, 2427, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 1832,
    /*  289 */ 2598, 2592, 2010, 2010, 2010, 2013, 3024, 2021, 2914, 1874, 1882, 3267, 1958, 1912, 2030, 1762, 2391,
    /*  306 */ 2388, 1769, 3273, 1903, 2427, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 1832, 2072, 2876,
    /*  323 */ 2038, 2043, 2047, 2050, 3024, 1860, 2914, 1874, 1882, 3267, 1958, 1912, 2030, 1762, 2391, 2388, 1769,
    /*  340 */ 3273, 1903, 2427, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 1832, 3106, 2058, 3107, 2071,
    /*  357 */ 2081, 2084, 3024, 1860, 2914, 1874, 1882, 3267, 1958, 1912, 2030, 1762, 2391, 2388, 1769, 3273, 1903,
    /*  374 */ 2427, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 1832, 2072, 2876, 2072, 2072, 2405, 2092,
    /*  391 */ 2117, 2105, 3297, 2125, 2133, 1712, 2150, 2145, 1781, 2163, 3247, 3244, 2155, 1718, 1722, 2182, 2072,
    /*  408 */ 2072, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 1832, 2072, 2876, 2072, 2072, 2530, 2192, 3024, 1860,
    /*  425 */ 2914, 2205, 1882, 3267, 1958, 1912, 2030, 1762, 2391, 2388, 1769, 3273, 1903, 2427, 2072, 2072, 2072,
    /*  442 */ 2072, 2072, 2072, 2072, 2072, 2072, 1832, 2072, 2876, 2072, 2072, 3010, 3017, 3024, 2213, 2914, 1874,
    /*  459 */ 2230, 2237, 1958, 1912, 3212, 1762, 2391, 2825, 1769, 3149, 1903, 2427, 2072, 2072, 2072, 2072, 2072,
    /*  476 */ 2072, 2072, 2072, 2072, 1832, 3039, 2956, 2963, 2963, 2963, 2966, 3024, 1860, 2914, 1874, 2245, 3267,
    /*  493 */ 1958, 1912, 2030, 1762, 2391, 2825, 1769, 3149, 1903, 2427, 2072, 2072, 2072, 2072, 2072, 2072, 2072,
    /*  510 */ 2072, 2072, 2454, 2262, 2269, 2275, 2283, 2289, 2292, 3200, 2300, 3195, 2333, 1882, 3166, 2341, 2362,
    /*  527 */ 2314, 2382, 2613, 2388, 1769, 3273, 1904, 2851, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 2072,
    /*  544 */ 1832, 2072, 2876, 2072, 2072, 2072, 2254, 3024, 2601, 2914, 1874, 2399, 2413, 2421, 1912, 2325, 1762,
    /*  561 */ 2869, 2825, 3069, 3149, 1903, 2427, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 2137, 2073,
    /*  578 */ 2797, 2072, 2027, 2802, 2437, 2697, 1860, 2914, 2462, 1882, 2661, 1958, 2470, 2639, 1762, 2391, 2388,
    /*  595 */ 1769, 3273, 1903, 2427, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 1832, 2550, 2483, 2491,
    /*  612 */ 2499, 2503, 2511, 3024, 2524, 3261, 2538, 1882, 3267, 1958, 1912, 2030, 1762, 2391, 2388, 1769, 3273,
    /*  629 */ 1903, 2427, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 2197, 2072, 2876, 2072, 2072, 2072,
    /*  646 */ 1791, 2063, 1860, 2914, 1874, 2546, 3267, 2558, 1912, 2571, 2585, 1808, 2610, 1769, 3273, 1903, 2427,
    /*  663 */ 2072, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 1832, 2072, 2621, 2636, 2251, 3344, 2647, 1834,
    /*  680 */ 2655, 2914, 1874, 1882, 3267, 1958, 1912, 2030, 1762, 2391, 2388, 1769, 2667, 1903, 2427, 2072, 2072,
    /*  697 */ 2072, 2072, 2072, 2072, 2072, 2072, 2072, 1866, 2679, 2790, 2072, 3227, 2683, 2691, 2755, 2705, 2713,
    /*  714 */ 2721, 1882, 2351, 1958, 2729, 3042, 1762, 2391, 2388, 1963, 3273, 1903, 2427, 2072, 2072, 2072, 2072,
    /*  731 */ 2072, 2072, 2072, 2072, 2072, 3022, 3103, 3097, 2742, 2742, 2742, 2744, 3024, 2601, 2752, 1874, 2399,
    /*  748 */ 2413, 2421, 1912, 2325, 1762, 3122, 2825, 3075, 3149, 1903, 2427, 2072, 2072, 2072, 2072, 2072, 2072,
    /*  765 */ 2072, 2072, 2072, 3022, 3103, 3097, 2742, 2742, 2742, 2744, 3024, 2601, 2752, 1874, 2399, 2413, 2421,
    /*  782 */ 2763, 2325, 1762, 3122, 2825, 3075, 3149, 1903, 2427, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 2072,
    /*  799 */ 2072, 3022, 3103, 3097, 2742, 2742, 2742, 2744, 3024, 2601, 2752, 1874, 2783, 2413, 2421, 1912, 2325,
    /*  816 */ 2819, 3122, 2987, 3075, 3149, 1903, 2427, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 3022,
    /*  833 */ 3103, 3097, 2742, 2742, 2742, 2744, 3024, 1860, 2752, 1874, 2245, 3267, 1958, 1912, 2030, 1762, 2828,
    /*  850 */ 2825, 2346, 3149, 1903, 2427, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 3022, 3103, 3097,
    /*  867 */ 2742, 2742, 2742, 2744, 3024, 1860, 2752, 1874, 2245, 3267, 1958, 2836, 2030, 1762, 2828, 2825, 2346,
    /*  884 */ 3149, 1903, 2427, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 3022, 3103, 3097, 2742, 2742,
    /*  901 */ 2742, 2744, 3024, 1860, 2752, 1874, 2245, 3267, 2844, 1912, 2030, 1762, 3055, 2825, 2346, 3149, 1903,
    /*  918 */ 2427, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 3022, 3103, 3097, 2742, 2742, 2742, 2744,
    /*  935 */ 3024, 1860, 2752, 1874, 2245, 3267, 2862, 1912, 2030, 2894, 2828, 2825, 2346, 3149, 1903, 2427, 2072,
    /*  952 */ 2072, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 3022, 3103, 3097, 2742, 2742, 2742, 2744, 3024, 1860,
    /*  969 */ 2752, 1874, 2245, 3267, 1958, 1912, 2030, 1762, 3090, 2825, 2346, 3149, 1903, 2427, 2072, 2072, 2072,
    /*  986 */ 2072, 2072, 2072, 2072, 2072, 2072, 3022, 3103, 3097, 2742, 2742, 2742, 2744, 3024, 1860, 2752, 1874,
    /* 1003 */ 2245, 3267, 1958, 1912, 2030, 1762, 2828, 2900, 2346, 3149, 1903, 2427, 2072, 2072, 2072, 2072, 2072,
    /* 1020 */ 2072, 2072, 2072, 2072, 3022, 3103, 3097, 2742, 2742, 2742, 2744, 3024, 1860, 2752, 1874, 2908, 3267,
    /* 1037 */ 1958, 1912, 2030, 1762, 2828, 2825, 2346, 3149, 1903, 2427, 2072, 2072, 2072, 2072, 2072, 2072, 2072,
    /* 1054 */ 2072, 2072, 1832, 2807, 2628, 2925, 2930, 2938, 2941, 3024, 1860, 2914, 1874, 1882, 3267, 1958, 1912,
    /* 1071 */ 2030, 1762, 2391, 2388, 1769, 3273, 1903, 2427, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 2072,
    /* 1088 */ 2097, 2072, 3129, 2072, 2219, 2072, 3134, 2886, 1860, 2914, 1874, 1882, 3267, 1958, 1912, 2030, 1762,
    /* 1105 */ 2391, 2388, 1769, 3273, 1903, 2427, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 2374, 2072,
    /* 1122 */ 2373, 2072, 2072, 2072, 2072, 3024, 1860, 2072, 2429, 2354, 2949, 1735, 2811, 2974, 2981, 1885, 1768,
    /* 1139 */ 1740, 3273, 1903, 2427, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 1832, 2072, 2770, 2321,
    /* 1156 */ 2321, 2184, 2775, 3024, 1860, 3291, 2995, 1882, 3267, 1958, 1912, 2030, 1762, 2391, 2388, 1769, 3273,
    /* 1173 */ 1903, 2427, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 3022, 3103, 3097, 2742, 2742, 2742,
    /* 1190 */ 2744, 3024, 2601, 2752, 1874, 2399, 2413, 2421, 1912, 2325, 1762, 3122, 2825, 3075, 3181, 1903, 2427,
    /* 1207 */ 2072, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 3022, 3103, 3097, 2742, 2742, 2742, 2744, 3024,
    /* 1224 */ 2601, 2752, 1874, 2399, 2413, 2421, 1912, 2325, 1762, 3003, 2825, 3075, 3149, 1903, 2427, 2072, 2072,
    /* 1241 */ 2072, 2072, 2072, 2072, 2072, 2072, 2072, 3022, 3103, 3097, 2742, 2742, 2742, 2744, 3024, 2601, 2752,
    /* 1258 */ 1874, 2399, 2413, 2421, 1912, 2325, 1762, 3032, 3052, 3075, 3149, 1903, 2427, 2072, 2072, 2072, 2072,
    /* 1275 */ 2072, 2072, 2072, 2072, 2072, 3022, 3103, 3097, 2742, 2742, 2742, 2744, 3024, 2601, 2752, 1874, 2399,
    /* 1292 */ 2413, 2421, 1912, 2325, 1762, 3122, 2825, 3063, 3149, 1903, 2427, 2072, 2072, 2072, 2072, 2072, 2072,
    /* 1309 */ 2072, 2072, 2072, 3022, 3103, 3097, 2742, 2742, 2742, 2744, 3024, 1860, 2752, 1874, 2245, 3267, 1958,
    /* 1326 */ 1912, 2030, 1762, 2828, 2825, 2563, 3083, 1900, 2427, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 2072,
    /* 1343 */ 2072, 3022, 3103, 3097, 2742, 2742, 2742, 2744, 3024, 1860, 2752, 1874, 2245, 3267, 1958, 1912, 2030,
    /* 1360 */ 1762, 2828, 2825, 2346, 3115, 1903, 2427, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 3022,
    /* 1377 */ 3103, 3097, 2742, 2742, 2742, 2744, 3024, 1860, 2752, 1874, 2245, 3267, 1958, 1912, 2030, 1762, 2828,
    /* 1394 */ 2825, 2346, 3142, 3277, 2427, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 3022, 3103, 3097,
    /* 1411 */ 2742, 2742, 2742, 2744, 3024, 1860, 2752, 1874, 2245, 3267, 1958, 1912, 2030, 1762, 2828, 2825, 2346,
    /* 1428 */ 3149, 2671, 2427, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 3022, 3103, 3097, 2742, 2742,
    /* 1445 */ 2742, 2744, 3024, 1860, 2752, 1874, 2245, 3267, 1958, 1912, 2030, 1762, 2828, 2825, 3161, 3149, 1903,
    /* 1462 */ 2427, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 3022, 3103, 3097, 2742, 2742, 2742, 2744,
    /* 1479 */ 3024, 1860, 2752, 1874, 2245, 3267, 2475, 1912, 2030, 1762, 2828, 2825, 2346, 3149, 1903, 2427, 2072,
    /* 1496 */ 2072, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 3022, 3103, 3097, 2742, 2742, 2742, 2744, 3024, 1860,
    /* 1513 */ 2752, 1874, 2245, 3267, 1958, 1912, 2030, 1762, 2828, 2825, 2346, 3149, 3174, 2427, 2072, 2072, 2072,
    /* 1530 */ 2072, 2072, 2072, 2072, 2072, 2072, 3022, 3103, 3097, 2742, 2742, 2742, 2744, 3024, 1860, 2752, 1874,
    /* 1547 */ 2245, 2307, 2734, 1912, 2030, 1762, 2828, 2825, 2346, 3149, 1903, 2427, 2072, 2072, 2072, 2072, 2072,
    /* 1564 */ 2072, 2072, 2072, 2072, 3022, 3103, 3097, 2742, 2742, 2742, 2744, 3024, 3189, 3208, 1874, 2245, 3220,
    /* 1581 */ 1958, 1912, 2030, 3238, 2828, 3255, 2346, 3149, 1903, 2427, 2072, 2072, 2072, 2072, 2072, 2072, 2072,
    /* 1598 */ 2072, 2072, 1832, 2853, 3285, 2854, 3305, 3310, 3313, 3024, 1860, 2914, 1874, 1882, 3267, 1958, 1912,
    /* 1615 */ 2030, 1762, 2391, 2388, 1769, 3273, 1903, 2427, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 2072,
    /* 1632 */ 1832, 2072, 2876, 2072, 2072, 2072, 2254, 3024, 1860, 2914, 1874, 2245, 3267, 1958, 1912, 2030, 1762,
    /* 1649 */ 2391, 2825, 1769, 3149, 1903, 2427, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 2881,
    /* 1666 */ 3230, 3321, 3326, 3327, 3335, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 2072,
    /* 1683 */ 2072, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 2072, 3840, 569, 569, 569, 569,
    /* 1701 */ 569, 569, 569, 569, 569, 0, 0, 0, 0, 62, 62, 0, 0, 105, 100, 100, 0, 103, 0, 0, 0, 103, 103, 103, 103,
    /* 1726 */ 103, 176, 103, 103, 0, 100, 3202, 0, 117, 117, 0, 0, 0, 0, 0, 87, 87, 0, 0, 0, 109, 87, 0, 122, 0, 0, 0,
    /* 1753 */ 87, 0, 100, 0, 0, 0, 0, 87, 512, 0, 3202, 0, 0, 0, 134, 135, 0, 87, 87, 0, 0, 0, 0, 87, 137, 52117, 0, 0,
    /* 1781 */ 0, 0, 134, 0, 0, 103, 0, 100, 177, 87, 0, 0, 0, 0, 0, 0, 55, 0, 0, 3202, 0, 0, 0, 134, 144, 0, 87, 87,
    /* 1809 */ 52117, 0, 0, 0, 0, 134, 124, 569, 569, 2117, 569, 2117, 569, 569, 569, 0, 569, 0, 569, 569, 569, 569,
    /* 1831 */ 569, 0, 55, 0, 0, 0, 0, 0, 62, 0, 64, 569, 0, 0, 0, 0, 62, 62, 64, 64, 64, 64, 64, 64, 64, 64, 0, 55, 0,
    /* 1860 */ 65, 0, 0, 0, 0, 34816, 0, 0, 56, 0, 0, 0, 0, 33599, 96, 101, 0, 0, 0, 0, 87, 600, 0, 2304, 0, 87, 0, 0,
    /* 1888 */ 0, 0, 0, 134, 0, 117, 0, 0, 121, 122, 0, 111, 87, 172, 173, 87, 87, 87, 87, 87, 87, 87, 87, 3328, 0, 128,
    /* 1914 */ 3202, 0, 117, 117, 0, 0, 0, 122, 0, 111, 87, 76, 0, 0, 0, 76, 76, 0, 0, 0, 76, 76, 76, 76, 76, 76, 76,
    /* 1941 */ 76, 0, 0, 0, 97, 97, 0, 0, 0, 0, 87, 600, 0, 121, 0, 131, 117, 117, 0, 0, 121, 0, 0, 87, 87, 0, 163, 164,
    /* 1969 */ 0, 87, 0, 139, 0, 0, 0, 134, 135, 146, 4864, 0, 0, 0, 4864, 4864, 0, 0, 0, 4864, 4864, 4864, 4864, 4864,
    /* 1993 */ 4864, 4864, 4864, 0, 55, 0, 5120, 5120, 5120, 5120, 5120, 5120, 5120, 5120, 0, 55, 0, 65, 65, 65, 65, 65,
    /* 2015 */ 65, 65, 65, 0, 55, 0, 1536, 0, 0, 0, 0, 34816, 0, 0, 59, 0, 0, 0, 0, 0, 87, 0, 100, 5376, 0, 0, 0, 5376,
    /* 2043 */ 5376, 0, 0, 0, 5376, 5376, 5376, 5376, 5376, 5376, 5376, 5376, 0, 55, 0, 0, 5632, 2117, 0, 2117, 0, 0, 0,
    /* 2066 */ 8960, 0, 62, 62, 64, 5632, 0, 0, 0, 0, 0, 0, 0, 0, 59, 5632, 5632, 5632, 5632, 5632, 5632, 5632, 5632, 0,
    /* 2090 */ 55, 0, 5971, 5888, 5971, 5971, 5971, 0, 55, 0, 0, 0, 0, 61, 62, 65, 0, 0, 0, 0, 34816, 92, 0, 0, 111,
    /* 2115 */ 100, 100, 0, 87, 0, 0, 0, 62, 62, 64, 96, 101, 6144, 0, 0, 0, 103, 600, 0, 2304, 0, 105, 0, 0, 0, 0, 59,
    /* 2142 */ 0, 0, 62, 0, 128, 3202, 0, 118, 118, 0, 0, 121, 0, 0, 103, 103, 0, 0, 0, 0, 103, 0, 3202, 0, 0, 0, 143,
    /* 2169 */ 135, 0, 87, 87, 156, 0, 0, 0, 5120, 5120, 5120, 0, 5120, 178, 103, 0, 0, 0, 0, 0, 0, 1353, 0, 6484, 6400,
    /* 2194 */ 6484, 6484, 6484, 0, 55, 0, 0, 0, 60, 0, 62, 96, 101, 0, 6656, 0, 0, 87, 600, 65, 0, 0, 0, 90, 34906, 0,
    /* 2220 */ 0, 61, 0, 0, 0, 0, 0, 34816, 0, 0, 0, 2304, 0, 87, 0, 1374, 0, 108, 0, 87, 100, 114, 0, 87, 116, 0, 2304,
    /* 2247 */ 0, 87, 0, 1374, 0, 0, 68, 0, 0, 0, 0, 0, 1078, 55, 0, 0, 58, 58, 58, 58, 7482, 58, 58, 7482, 2117, 71,
    /* 2273 */ 2117, 71, 58, 71, 71, 79, 7482, 58, 7503, 79, 7482, 7503, 71, 7503, 7503, 79, 7503, 7503, 7503, 7503,
    /* 2293 */ 7503, 7503, 7503, 7503, 0, 0, 0, 65, 0, 0, 0, 0, 34816, 600, 0, 0, 112, 100, 100, 0, 87, 0, 0, 135, 0, 0,
    /* 2319 */ 87, 138, 0, 0, 1353, 0, 0, 0, 0, 0, 125, 87, 0, 100, 96, 96, 0, 0, 0, 102, 87, 600, 629, 0, 0, 121, 123,
    /* 2346 */ 0, 87, 87, 52130, 0, 0, 0, 87, 0, 0, 0, 87, 0, 0, 0, 109, 127, 121, 3202, 0, 117, 4096, 0, 0, 70, 0, 70,
    /* 2373 */ 0, 0, 0, 0, 10752, 0, 0, 0, 62, 127, 3202, 0, 0, 0, 134, 135, 0, 87, 87, 52117, 0, 0, 0, 0, 134, 0, 0,
    /* 2400 */ 2304, 1896, 87, 0, 1374, 0, 0, 83, 5888, 83, 5888, 5888, 5971, 0, 1890, 87, 100, 100, 0, 87, 1896, 117,
    /* 2422 */ 0, 0, 121, 0, 125, 87, 87, 0, 0, 0, 0, 0, 0, 87, 600, 59, 59, 0, 0, 59, 0, 86, 0, 0, 2117, 0, 2117, 0, 0,
    /* 2451 */ 64, 0, 64, 0, 0, 0, 0, 58, 58, 58, 62, 96, 96, 0, 0, 0, 0, 87, 600, 0, 129, 3202, 0, 117, 117, 0, 0, 121,
    /* 2479 */ 0, 0, 87, 126, 67, 0, 2117, 66, 2117, 72, 67, 74, 66, 78, 78, 72, 66, 66, 72, 72, 66, 72, 78, 72, 80, 80,
    /* 2505 */ 80, 80, 80, 85, 85, 80, 80, 85, 85, 85, 85, 0, 55, 0, 0, 569, 569, 569, 3902, 65, 0, 7936, 0, 0, 34816,
    /* 2530 */ 0, 0, 84, 6400, 84, 6400, 6400, 6484, 96, 101, 0, 0, 7936, 0, 87, 600, 7680, 2304, 0, 87, 0, 0, 0, 0, 66,
    /* 2555 */ 0, 0, 0, 117, 0, 0, 121, 124, 0, 87, 87, 52130, 0, 0, 0, 165, 0, 124, 0, 20992, 0, 87, 0, 100, 0, 0, 0,
    /* 2582 */ 0, 87, 600, 0, 3202, 0, 0, 0, 134, 145, 0, 0, 2117, 0, 2117, 0, 0, 65, 0, 65, 0, 0, 0, 0, 0, 0, 0, 62,
    /* 2610 */ 21137, 0, 87, 87, 52117, 0, 0, 0, 0, 144, 0, 68, 0, 2117, 0, 2117, 0, 68, 0, 0, 2117, 0, 2117, 0, 0, 75,
    /* 2636 */ 0, 68, 68, 0, 0, 0, 0, 0, 137, 0, 113, 0, 68, 68, 68, 68, 0, 55, 9728, 65, 0, 0, 9216, 0, 34816, 0, 0,
    /* 2663 */ 87, 113, 113, 0, 87, 0, 8448, 8704, 87, 87, 87, 87, 175, 87, 87, 87, 56, 0, 0, 0, 0, 0, 0, 9472, 0, 9472,
    /* 2689 */ 9472, 9472, 9472, 9472, 9472, 9472, 9472, 0, 0, 0, 89, 0, 0, 62, 62, 64, 65, 8192, 0, 0, 0, 34816, 0, 93,
    /* 2713 */ 0, 95, 96, 0, 0, 95, 0, 8285, 96, 96, 0, 0, 8285, 0, 87, 600, 0, 121, 3202, 0, 117, 117, 0, 0, 121, 0, 0,
    /* 2740 */ 112, 87, 1078, 1078, 1078, 1078, 1078, 1078, 1078, 1078, 55, 0, 1374, 0, 96, 0, 0, 0, 0, 0, 33599, 33599,
    /* 2762 */ 64, 0, 128, 3202, 0, 117, 117, 132, 0, 0, 2117, 0, 2117, 0, 1353, 0, 1353, 1353, 0, 55, 0, 0, 2304, 1896,
    /* 2786 */ 87, 106, 1374, 106, 0, 0, 2117, 0, 2117, 0, 9472, 0, 0, 2117, 0, 2117, 0, 59, 0, 59, 59, 0, 0, 0, 9984,
    /* 2811 */ 0, 0, 0, 0, 117, 117, 0, 0, 0, 3202, 0, 0, 142, 134, 135, 0, 87, 87, 52117, 3222, 0, 0, 0, 134, 0, 0,
    /* 2837 */ 128, 3202, 0, 117, 117, 0, 133, 117, 119, 0, 121, 0, 0, 87, 87, 3584, 0, 0, 0, 0, 0, 0, 11008, 0, 11008,
    /* 2862 */ 117, 0, 120, 121, 0, 0, 87, 87, 52117, 0, 2700, 0, 0, 134, 0, 0, 2117, 0, 2117, 0, 0, 0, 0, 4352, 0, 0,
    /* 2888 */ 0, 0, 10496, 62, 62, 64, 0, 3202, 0, 141, 0, 134, 135, 0, 87, 87, 52117, 3222, 0, 158, 0, 2304, 0, 87,
    /* 2912 */ 107, 1374, 0, 0, 96, 0, 0, 0, 0, 0, 62, 62, 1536, 10061, 75, 75, 75, 10061, 10061, 75, 75, 75, 10065,
    /* 2935 */ 10065, 10065, 10066, 10065, 10065, 10065, 10065, 10065, 10065, 10065, 10065, 0, 55, 0, 109, 0, 87, 0,
    /* 2953 */ 109, 0, 87, 0, 0, 2117, 7168, 2117, 7168, 0, 7168, 7168, 7168, 7168, 7168, 7168, 7168, 7168, 1078, 55, 0,
    /* 2974 */ 109, 0, 0, 0, 109, 87, 0, 109, 0, 0, 0, 0, 134, 135, 0, 87, 87, 52117, 3222, 157, 0, 10336, 10341, 0, 0,
    /* 2999 */ 0, 0, 87, 600, 147, 52117, 3222, 2700, 0, 0, 134, 0, 0, 6912, 0, 6912, 0, 0, 6912, 0, 6912, 6912, 6912,
    /* 3022 */ 1078, 55, 0, 0, 0, 0, 0, 62, 62, 64, 148, 52117, 3222, 2700, 0, 0, 134, 0, 0, 7168, 0, 0, 0, 0, 0, 87, 0,
    /* 3049 */ 0, 0, 0, 135, 0, 154, 87, 52117, 3222, 0, 0, 152, 134, 0, 159, 160, 87, 52130, 0, 0, 159, 87, 87, 0, 0,
    /* 3074 */ 0, 159, 87, 87, 52130, 0, 0, 159, 87, 87, 52130, 0, 0, 87, 168, 169, 87, 52117, 3222, 0, 151, 0, 134, 0,
    /* 3098 */ 0, 2117, 1078, 2117, 1078, 0, 1078, 1078, 0, 0, 0, 0, 0, 5632, 5632, 0, 0, 166, 52130, 0, 0, 87, 87, 87,
    /* 3122 */ 87, 52117, 3222, 2700, 0, 0, 134, 0, 0, 2117, 0, 2117, 0, 61, 0, 0, 61, 0, 55, 0, 87, 52130, 0, 0, 167,
    /* 3147 */ 87, 87, 87, 52130, 0, 0, 87, 87, 87, 87, 87, 174, 87, 87, 0, 87, 161, 52130, 0, 0, 0, 87, 0, 0, 115, 512,
    /* 3173 */ 0, 171, 87, 87, 174, 87, 87, 87, 87, 52130, 0, 0, 87, 87, 87, 170, 65, 0, 0, 0, 0, 34907, 0, 0, 96, 0, 0,
    /* 3200 */ 0, 600, 0, 0, 0, 62, 62, 64, 1374, 0, 96, 98, 0, 0, 0, 0, 136, 87, 0, 100, 110, 0, 87, 100, 100, 0, 87,
    /* 3227 */ 0, 0, 9472, 0, 0, 0, 0, 0, 4352, 0, 4352, 0, 3202, 140, 0, 0, 134, 135, 0, 103, 103, 52117, 0, 0, 0, 0,
    /* 3253 */ 143, 0, 135, 153, 87, 155, 52117, 3222, 0, 0, 96, 0, 0, 7936, 0, 0, 87, 100, 100, 0, 87, 0, 0, 0, 87, 87,
    /* 3279 */ 87, 87, 87, 87, 177, 87, 0, 11008, 2117, 0, 2117, 0, 0, 0, 96, 0, 10240, 0, 0, 0, 96, 0, 0, 0, 99, 0,
    /* 3305 */ 11008, 11008, 0, 11008, 0, 11008, 11008, 11008, 11008, 11008, 11008, 11008, 11008, 0, 55, 0, 0, 4352,
    /* 3323 */ 4352, 4352, 0, 0, 4352, 4352, 4352, 4352, 4352, 4352, 4352, 4352, 4352, 4352, 4352, 4352, 4352, 0, 0, 0,
    /* 3343 */ 97, 0, 0, 0, 0, 0, 68, 68, 0
  };

  private static final int[] EXPECTED =
  {
    /*   0 */ 137, 96, 89, 93, 100, 104, 108, 112, 116, 120, 124, 128, 132, 136, 165, 234, 141, 148, 279, 155, 159, 163,
    /*  22 */ 169, 278, 173, 185, 150, 189, 205, 196, 228, 203, 206, 227, 192, 226, 210, 179, 211, 151, 218, 215, 215,
    /*  43 */ 215, 224, 232, 243, 238, 176, 220, 248, 251, 255, 259, 262, 265, 268, 272, 244, 241, 284, 275, 199, 181,
    /*  64 */ 144, 198, 284, 198, 283, 284, 285, 289, 284, 284, 288, 284, 284, 287, 284, 286, 284, 284, 288, 286, 284,
    /*  85 */ 288, 284, 284, 284, 16384, 42, 134217738, 34, -2147418110, 270532610, 2097154, 536870914, 2, 2, 16388,
    /* 100 */ 1073741826, 270532610, 960, -2013233142, 960, 134316042, 1610612754, 134447146, -2011955094, 1208188970,
    /* 110 */ 1208188986, 134459434, -1741422486, -2009857942, 402894890, 134459434, -1741422486, 402894890, 1745059898,
    /* 119 */ 403419178, -1742753686, -2011189142, -1743277974, -1742752662, -1740656534, -1203785622, -1631604630,
    /* 127 */ -1182814102, -1094733718, -645943190, -645943174, -1610633110, -1073762198, -109072262, -536891286,
    /* 135 */ -536891270, -20358, 8, 2048, 0, 0, 32, -2147483648, 1073741824, 0, 3, 128, 128, 256, 960, 32770, 0, 0,
    /* 153 */ 131072, 8194, 0, 1073741824, 45058, -2147483648, 131072, 132096, 8388608, 33554432, -1073741824, 2048, 2,
    /* 166 */ 2, 536870912, 0, 536870912, 256, 576, 32770, 131072, 64, 45058, 1024, 16, 8, 1024, 2048, 0, 0, 128, 1,
    /* 185 */ 132096, 2048, 2, 576, 131072, 131072, 12290, 12290, 1024, 2048, 512, 32768, 32768, 0, 0, 3, 0, 1, 131072,
    /* 204 */ 12290, 1024, 132096, 2048, 512, 0, 131072, 131072, 8194, 4098, 1024, 8194, 4098, 8194, 4098, 1024, 0, 0,
    /* 222 */ 512, 3, 8194, 8194, 0, 0, 131072, 131072, 131072, 131072, 32, 512, 0, 4, 4, 32, 0, 64, 0, 0, 4, 256, 0,
    /* 245 */ 32, 0, 0, 280, 1, 192, 9, 3, 192, 1216, 3, 1027, 1216, 1027, 281, 1027, 195, 1219, 1219, 1219, 1219, 1235,
    /* 267 */ 1219, 1243, 1235, 1235, 1499, 1243, 1243, 1499, 0, 8, 0, 0, 16, 0, 131072, 64, 1, 0, 0, 0, 0, 1, 2, 0, 0,
    /* 292 */ 0
  };

  private static final String[] TOKEN =
  {
    "%ERROR",
    "Whitespace",
    "ProcessingInstructionContents",
    "NCName",
    "Context",
    "StringLiteral",
    "CharCode",
    "Char",
    "CharRange",
    "CharCodeRange",
    "'['",
    "URL",
    "'/*ws:explicit*/'",
    "'/*ws:definition*/'",
    "S",
    "DocComment",
    "EOF",
    "EquivalenceLookAhead",
    "'$'",
    "'&'",
    "'('",
    "')'",
    "'*'",
    "'**'",
    "'+'",
    "'++'",
    "'-'",
    "'.'",
    "'/'",
    "'::='",
    "'<<'",
    "'<?'",
    "'<?ENCORE?>'",
    "'<?TOKENS?>'",
    "'=='",
    "'>>'",
    "'?'",
    "'?>'",
    "'['",
    "'[^'",
    "'\\\\'",
    "']'",
    "'|'"
  };
}

// End
