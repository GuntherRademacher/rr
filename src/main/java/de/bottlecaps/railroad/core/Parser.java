// This file was generated on Sun Feb 9, 2025 21:08 (UTC+01) by REx v6.2-SNAPSHOT which is Copyright (c) 1979-2025 by Gunther Rademacher <grd@gmx.net>
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
      lookahead1W(17);              // Whitespace | NCName | DocComment | '<?'
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
      lookahead1W(21);              // Whitespace | NCName | DocComment | EOF | '<?ENCORE?>' | '<?TOKENS?>'
      if (l1 != 3                   // NCName
       && l1 != 15)                 // DocComment
      {
        break;
      }
    }
    if (l1 == 33)                   // '<?TOKENS?>'
    {
      consume(33);                  // '<?TOKENS?>'
      for (;;)
      {
        lookahead1W(22);            // Whitespace | NCName | StringLiteral | DocComment | EOF | EquivalenceLookAhead |
                                    // '<?ENCORE?>'
        if (l1 == 16                // EOF
         || l1 == 32)               // '<?ENCORE?>'
        {
          break;
        }
        switch (l1)
        {
        case 3:                     // NCName
          lookahead2W(23);          // Whitespace | Context | '::=' | '<<' | '>>' | '?' | '\\'
          break;
        default:
          lk = l1;
        }
        switch (lk)
        {
        case 15:                    // DocComment
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
    lookahead1(9);                  // S | '?>'
    if (l1 == 14)                   // S
    {
      for (;;)
      {
        consume(14);                // S
        lookahead1(8);              // ProcessingInstructionContents | S
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
    lookahead1W(4);                 // Whitespace | NCName
    consume(3);                     // NCName
    lookahead1W(15);                // Whitespace | '::=' | '?'
    if (l1 == 36)                   // '?'
    {
      consume(36);                  // '?'
    }
    lookahead1W(5);                 // Whitespace | '::='
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
                                    // EOF | EquivalenceLookAhead | '<?ENCORE?>' | '<?TOKENS?>'
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
    lookahead1W(32);                // Whitespace | NCName | StringLiteral | WsExplicit | WsDefinition | DocComment |
                                    // EOF | EquivalenceLookAhead | '/' | '<?ENCORE?>' | '<?TOKENS?>' | '|'
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
          lookahead1W(29);          // Whitespace | NCName | StringLiteral | WsExplicit | WsDefinition | DocComment |
                                    // EOF | EquivalenceLookAhead | '<?ENCORE?>' | '<?TOKENS?>' | '|'
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
          lookahead1W(28);          // Whitespace | NCName | StringLiteral | WsExplicit | WsDefinition | DocComment |
                                    // EOF | EquivalenceLookAhead | '/' | '<?ENCORE?>' | '<?TOKENS?>'
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
                                    // EOF | EquivalenceLookAhead | '&' | '/' | '<?ENCORE?>' | '<?TOKENS?>' | '|'
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
      lookahead2W(50);              // Whitespace | NCName | Context | StringLiteral | CharCode | WsExplicit |
                                    // WsDefinition | DocComment | EOF | EquivalenceLookAhead | '$' | '&' | '(' | ')' |
                                    // '*' | '**' | '+' | '++' | '-' | '.' | '/' | '::=' | '<<' | '<?' | '<?ENCORE?>' |
                                    // '<?TOKENS?>' | '>>' | '?' | '[' | '[^' | '\\' | '|'
      switch (lk)
      {
      case 259:                     // NCName Context
        lookahead3W(48);            // Whitespace | NCName | StringLiteral | CharCode | WsExplicit | WsDefinition |
                                    // DocComment | EOF | EquivalenceLookAhead | '$' | '&' | '(' | ')' | '*' | '**' |
                                    // '+' | '++' | '-' | '.' | '/' | '<<' | '<?' | '<?ENCORE?>' | '<?TOKENS?>' | '>>' |
                                    // '?' | '[' | '[^' | '|'
        break;
      case 2307:                    // NCName '?'
        lookahead3W(43);            // Whitespace | NCName | StringLiteral | CharCode | WsExplicit | WsDefinition |
                                    // DocComment | EOF | EquivalenceLookAhead | '$' | '&' | '(' | ')' | '**' | '++' |
                                    // '-' | '.' | '/' | '::=' | '<?' | '<?ENCORE?>' | '<?TOKENS?>' | '[' | '[^' | '|'
        break;
      }
      break;
    case 5:                         // StringLiteral
      lookahead2W(49);              // Whitespace | NCName | Context | StringLiteral | CharCode | WsExplicit |
                                    // WsDefinition | DocComment | EOF | EquivalenceLookAhead | '$' | '&' | '(' | ')' |
                                    // '*' | '**' | '+' | '++' | '-' | '.' | '/' | '<<' | '<?' | '<?ENCORE?>' |
                                    // '<?TOKENS?>' | '>>' | '?' | '[' | '[^' | '|'
      switch (lk)
      {
      case 261:                     // StringLiteral Context
        lookahead3W(48);            // Whitespace | NCName | StringLiteral | CharCode | WsExplicit | WsDefinition |
                                    // DocComment | EOF | EquivalenceLookAhead | '$' | '&' | '(' | ')' | '*' | '**' |
                                    // '+' | '++' | '-' | '.' | '/' | '<<' | '<?' | '<?ENCORE?>' | '<?TOKENS?>' | '>>' |
                                    // '?' | '[' | '[^' | '|'
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
     && lk != 1923                  // NCName '<<'
     && lk != 1925                  // StringLiteral '<<'
     && lk != 2243                  // NCName '>>'
     && lk != 2245                  // StringLiteral '>>'
     && lk != 2563                  // NCName '\\'
     && lk != 121091                // NCName '?' '::='
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
        lookahead1W(26);            // Whitespace | NCName | StringLiteral | CharCode | '$' | '(' | '.' | '<?' | '[' |
                                    // '[^'
        whitespace();
        parse_Item();
        break;
      case 23:                      // '**'
        consume(23);                // '**'
        lookahead1W(26);            // Whitespace | NCName | StringLiteral | CharCode | '$' | '(' | '.' | '<?' | '[' |
                                    // '[^'
        whitespace();
        parse_Item();
        break;
      case 25:                      // '++'
        consume(25);                // '++'
        lookahead1W(26);            // Whitespace | NCName | StringLiteral | CharCode | '$' | '(' | '.' | '<?' | '[' |
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
            lookahead2W(47);        // Whitespace | NCName | Context | StringLiteral | CharCode | WsExplicit |
                                    // WsDefinition | DocComment | EOF | EquivalenceLookAhead | '$' | '&' | '(' | ')' |
                                    // '*' | '+' | '.' | '/' | '::=' | '<<' | '<?' | '<?ENCORE?>' | '<?TOKENS?>' |
                                    // '>>' | '?' | '[' | '[^' | '\\' | '|'
            switch (lk)
            {
            case 259:               // NCName Context
              lookahead3W(44);      // Whitespace | NCName | StringLiteral | CharCode | WsExplicit | WsDefinition |
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
            lookahead2W(45);        // Whitespace | NCName | Context | StringLiteral | CharCode | WsExplicit |
                                    // WsDefinition | DocComment | EOF | EquivalenceLookAhead | '$' | '&' | '(' | ')' |
                                    // '*' | '+' | '.' | '/' | '<<' | '<?' | '<?ENCORE?>' | '<?TOKENS?>' | '>>' | '?' |
                                    // '[' | '[^' | '|'
            switch (lk)
            {
            case 261:               // StringLiteral Context
              lookahead3W(44);      // Whitespace | NCName | StringLiteral | CharCode | WsExplicit | WsDefinition |
                                    // DocComment | EOF | EquivalenceLookAhead | '$' | '&' | '(' | ')' | '*' | '+' |
                                    // '.' | '/' | '<<' | '<?' | '<?ENCORE?>' | '<?TOKENS?>' | '>>' | '?' | '[' | '[^' |
                                    // '|'
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
           || lk == 1923            // NCName '<<'
           || lk == 1925            // StringLiteral '<<'
           || lk == 2243            // NCName '>>'
           || lk == 2245            // StringLiteral '>>'
           || lk == 2563            // NCName '\\'
           || lk == 121091          // NCName '?' '::='
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
    lookahead1W(46);                // Whitespace | NCName | StringLiteral | CharCode | WsExplicit | WsDefinition |
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
      lookahead1W(34);              // Whitespace | NCName | StringLiteral | CharCode | '$' | '(' | ')' | '.' | '/' |
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
    lookahead1W(18);                // Whitespace | ')' | '/' | '|'
    if (l1 != 21)                   // ')'
    {
      switch (l1)
      {
      case 42:                      // '|'
        for (;;)
        {
          consume(42);              // '|'
          lookahead1W(31);          // Whitespace | NCName | StringLiteral | CharCode | '$' | '(' | ')' | '.' | '<?' |
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
          lookahead1W(30);          // Whitespace | NCName | StringLiteral | CharCode | '$' | '(' | ')' | '.' | '/' |
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
      lookahead1W(49);              // Whitespace | NCName | Context | StringLiteral | CharCode | WsExplicit |
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
      lookahead1W(49);              // Whitespace | NCName | Context | StringLiteral | CharCode | WsExplicit |
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
      lookahead1(19);               // CharCode | Char | CharRange | CharCodeRange
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
      lookahead1W(10);              // Whitespace | NCName | StringLiteral
      whitespace();
      parse_NameOrString();
      lookahead1W(22);              // Whitespace | NCName | StringLiteral | DocComment | EOF | EquivalenceLookAhead |
                                    // '<?ENCORE?>'
      switch (l1)
      {
      case 3:                       // NCName
        lookahead2W(33);            // Whitespace | NCName | Context | StringLiteral | DocComment | EOF |
                                    // EquivalenceLookAhead | '::=' | '<<' | '<?ENCORE?>' | '>>' | '?' | '\\'
        switch (lk)
        {
        case 259:                   // NCName Context
          lookahead3W(24);          // Whitespace | NCName | StringLiteral | DocComment | EOF | EquivalenceLookAhead |
                                    // '<<' | '<?ENCORE?>' | '>>'
          break;
        }
        break;
      case 5:                       // StringLiteral
        lookahead2W(25);            // Whitespace | NCName | Context | StringLiteral | DocComment | EOF |
                                    // EquivalenceLookAhead | '<<' | '<?ENCORE?>' | '>>'
        switch (lk)
        {
        case 261:                   // StringLiteral Context
          lookahead3W(24);          // Whitespace | NCName | StringLiteral | DocComment | EOF | EquivalenceLookAhead |
                                    // '<<' | '<?ENCORE?>' | '>>'
          break;
        }
        break;
      default:
        lk = l1;
      }
      if (lk == 15                  // DocComment
       || lk == 16                  // EOF
       || lk == 17                  // EquivalenceLookAhead
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
    lookahead1W(7);                 // Whitespace | '\\'
    consume(40);                    // '\\'
    for (;;)
    {
      lookahead1W(10);              // Whitespace | NCName | StringLiteral
      whitespace();
      parse_NameOrString();
      lookahead1W(22);              // Whitespace | NCName | StringLiteral | DocComment | EOF | EquivalenceLookAhead |
                                    // '<?ENCORE?>'
      switch (l1)
      {
      case 3:                       // NCName
        lookahead2W(33);            // Whitespace | NCName | Context | StringLiteral | DocComment | EOF |
                                    // EquivalenceLookAhead | '::=' | '<<' | '<?ENCORE?>' | '>>' | '?' | '\\'
        switch (lk)
        {
        case 259:                   // NCName Context
          lookahead3W(24);          // Whitespace | NCName | StringLiteral | DocComment | EOF | EquivalenceLookAhead |
                                    // '<<' | '<?ENCORE?>' | '>>'
          break;
        }
        break;
      case 5:                       // StringLiteral
        lookahead2W(25);            // Whitespace | NCName | Context | StringLiteral | DocComment | EOF |
                                    // EquivalenceLookAhead | '<<' | '<?ENCORE?>' | '>>'
        switch (lk)
        {
        case 261:                   // StringLiteral Context
          lookahead3W(24);          // Whitespace | NCName | StringLiteral | DocComment | EOF | EquivalenceLookAhead |
                                    // '<<' | '<?ENCORE?>' | '>>'
          break;
        }
        break;
      default:
        lk = l1;
      }
      if (lk == 15                  // DocComment
       || lk == 16                  // EOF
       || lk == 17                  // EquivalenceLookAhead
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
    lookahead1W(6);                 // Whitespace | '=='
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
      lookahead1(19);               // CharCode | Char | CharRange | CharCodeRange
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
      int i0 = (i >> 5) * 176 + s - 1;
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
    /* 29 */ 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51
  };

  private static final int[] TRANSITION =
  {
    /*    0 */ 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747,
    /*   17 */ 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1696, 1714,
    /*   34 */ 1701, 1716, 1716, 1716, 1706, 1971, 2694, 1787, 2928, 1747, 1724, 3120, 3115, 2421, 2875, 2341, 3220,
    /*   51 */ 2589, 2020, 3265, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1696, 1714, 1701, 1716,
    /*   68 */ 1716, 1716, 1706, 1971, 2694, 1787, 3223, 1747, 1724, 3120, 3115, 2421, 3315, 2221, 3220, 2589, 2020,
    /*   85 */ 3265, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1696, 1714, 1738, 1716, 1716, 1716,
    /*  102 */ 1706, 2007, 2049, 2447, 2928, 1746, 1724, 1756, 3115, 2421, 2875, 2341, 3220, 2589, 2020, 3265, 1747,
    /*  119 */ 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1785, 1975, 2044, 1747, 1747, 1747, 1782, 2007,
    /*  136 */ 2049, 2447, 2928, 1746, 1764, 1817, 2084, 2421, 2785, 2341, 3220, 2589, 2020, 3184, 1747, 1747, 1747,
    /*  153 */ 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1785, 1778, 2904, 1795, 1795, 1795, 1800, 2188, 2049, 2447,
    /*  170 */ 2928, 1746, 1764, 1817, 2084, 2421, 2785, 2341, 3220, 2589, 2020, 3184, 1747, 1747, 1747, 1747, 1747,
    /*  187 */ 1747, 1747, 1747, 1747, 1747, 1747, 1975, 2240, 2364, 2462, 2466, 2370, 2007, 2245, 2670, 2928, 1746,
    /*  204 */ 2410, 1811, 1825, 3167, 3148, 2341, 2925, 2589, 2020, 3184, 1747, 1747, 1747, 1747, 1747, 1747, 1747,
    /*  221 */ 1747, 1747, 1747, 1785, 1975, 2044, 1837, 1854, 1858, 1843, 2007, 2049, 2447, 2928, 1746, 1764, 1817,
    /*  238 */ 2084, 2421, 2785, 2341, 3220, 2589, 2020, 3184, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747,
    /*  255 */ 1747, 1785, 1975, 2044, 1747, 1941, 1948, 1953, 2007, 2049, 2447, 2928, 1746, 1764, 1817, 2084, 2421,
    /*  272 */ 2785, 2341, 3220, 2589, 2020, 3184, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1785,
    /*  289 */ 1866, 3237, 1878, 1878, 1878, 1883, 2133, 2049, 2447, 2928, 1746, 1764, 1817, 2084, 2421, 2785, 2341,
    /*  306 */ 3220, 2589, 2020, 3184, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1785, 1975, 2044,
    /*  323 */ 1896, 1915, 1919, 1902, 2007, 2049, 2447, 2928, 1746, 1764, 1817, 2084, 2421, 2785, 2341, 3220, 2589,
    /*  340 */ 2020, 3184, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1785, 1927, 1966, 2660, 1985,
    /*  357 */ 1997, 2002, 2007, 2049, 2447, 2928, 1746, 1764, 1817, 2084, 2421, 2785, 2341, 3220, 2589, 2020, 3184,
    /*  374 */ 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1785, 1975, 2044, 1747, 1747, 2605, 2015,
    /*  391 */ 2007, 2982, 1989, 2031, 1746, 2038, 2057, 2065, 2077, 1770, 2737, 2095, 2113, 2100, 2105, 1747, 1747,
    /*  408 */ 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1785, 1975, 2044, 1747, 1747, 2812, 2128, 2007, 2049,
    /*  425 */ 2447, 2141, 1746, 1764, 1817, 2084, 2421, 2785, 2341, 3220, 2589, 2020, 3184, 1747, 1747, 1747, 1747,
    /*  442 */ 1747, 1747, 1747, 1747, 1747, 1747, 1785, 1975, 2044, 1747, 1747, 2168, 2183, 2007, 2862, 2447, 2928,
    /*  459 */ 2196, 2210, 1817, 2084, 3108, 2785, 2341, 3163, 2148, 2020, 3184, 1747, 1747, 1747, 1747, 1747, 1747,
    /*  476 */ 1747, 1747, 1747, 1747, 1785, 1975, 2044, 2229, 2253, 2257, 2235, 2007, 2049, 2447, 2928, 2265, 1764,
    /*  493 */ 1817, 2084, 2421, 2785, 2341, 3163, 2148, 2020, 3184, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747,
    /*  510 */ 1747, 1747, 1829, 2279, 2286, 2291, 2299, 2305, 2310, 2007, 2321, 2313, 2511, 1746, 2846, 2329, 2337,
    /*  527 */ 2395, 2785, 2797, 3220, 2589, 2020, 2023, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747,
    /*  544 */ 1785, 1975, 2044, 1747, 1747, 1747, 2416, 2007, 1976, 2447, 2928, 2349, 2357, 2381, 2084, 3027, 2785,
    /*  561 */ 2389, 2403, 2760, 2020, 3184, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 2431, 2429,
    /*  578 */ 2852, 1747, 2445, 2439, 2857, 2455, 2049, 1977, 2928, 1746, 2474, 2498, 2084, 1730, 2785, 2341, 3220,
    /*  595 */ 2589, 2020, 3184, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1785, 2506, 2519, 2527,
    /*  612 */ 2535, 2539, 2547, 2007, 2560, 2568, 2576, 1746, 1764, 1817, 2084, 2421, 2785, 2341, 3220, 2589, 2020,
    /*  629 */ 3184, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1846, 1975, 2044, 1747, 1747, 1747,
    /*  646 */ 1782, 2598, 2049, 2447, 3005, 1746, 1764, 2613, 3034, 2621, 3340, 2644, 3220, 2589, 2020, 3184, 1747,
    /*  663 */ 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1785, 1975, 2652, 2668, 2658, 1870, 2678, 1907,
    /*  680 */ 2702, 2447, 2928, 1746, 1764, 1817, 2084, 2421, 2785, 2341, 3220, 2589, 2710, 3184, 1747, 1747, 1747,
    /*  697 */ 1747, 1747, 1747, 1747, 1747, 1747, 1747, 2217, 2722, 2634, 1747, 2640, 2725, 2733, 2175, 1958, 2745,
    /*  714 */ 2753, 1746, 2410, 1811, 2084, 3293, 2785, 2341, 3220, 2997, 2020, 3184, 1747, 1747, 1747, 1747, 1747,
    /*  731 */ 1747, 1747, 1747, 1747, 1747, 2780, 2793, 2805, 2820, 2820, 2820, 2824, 2007, 1976, 2202, 2928, 2349,
    /*  748 */ 2357, 2381, 2084, 3027, 2785, 2832, 2403, 2772, 2020, 3184, 1747, 1747, 1747, 1747, 1747, 1747, 1747,
    /*  765 */ 1747, 1747, 1747, 2780, 2793, 2805, 2820, 2820, 2820, 2824, 2007, 1976, 2202, 2928, 2349, 2357, 2381,
    /*  782 */ 2870, 3027, 2785, 2832, 2403, 2772, 2020, 3184, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747,
    /*  799 */ 1747, 2780, 2793, 2805, 2820, 2820, 2820, 2824, 2007, 1976, 2202, 2928, 2883, 2357, 2381, 2084, 3027,
    /*  816 */ 3135, 2832, 2891, 2772, 2020, 3184, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 2780,
    /*  833 */ 2793, 2805, 2820, 2820, 2820, 2824, 2007, 2049, 2202, 2928, 2265, 1764, 1817, 2084, 2421, 2785, 2912,
    /*  850 */ 3163, 2154, 2020, 3184, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 2780, 2793, 2805,
    /*  867 */ 2820, 2820, 2820, 2824, 2007, 2049, 2202, 2928, 2265, 1764, 1817, 2628, 2421, 2785, 2912, 3163, 2154,
    /*  884 */ 2020, 3184, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 2780, 2793, 2805, 2820, 2820,
    /*  901 */ 2820, 2824, 2007, 2049, 2202, 2928, 2265, 2936, 1817, 2084, 2421, 2785, 2944, 3163, 2154, 2020, 3184,
    /*  918 */ 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 2780, 2793, 2805, 2820, 2820, 2820, 2824,
    /*  935 */ 2007, 2049, 2202, 2928, 2265, 1764, 2959, 2084, 2421, 2967, 2912, 3163, 2154, 2020, 3184, 1747, 1747,
    /*  952 */ 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 2780, 2793, 2805, 2820, 2820, 2820, 2824, 2007, 2049,
    /*  969 */ 2202, 2928, 2265, 1764, 1817, 2084, 2421, 2785, 2975, 3163, 2154, 2020, 3184, 1747, 1747, 1747, 1747,
    /*  986 */ 1747, 1747, 1747, 1747, 1747, 1747, 2780, 2793, 2805, 2820, 2820, 2820, 2824, 2007, 2049, 2202, 2928,
    /* 1003 */ 2265, 1764, 1817, 2084, 2421, 2785, 2912, 2990, 2154, 2020, 3184, 1747, 1747, 1747, 1747, 1747, 1747,
    /* 1020 */ 1747, 1747, 1747, 1747, 2780, 2793, 2805, 2820, 2820, 2820, 2824, 2007, 2049, 2202, 2928, 3013, 1764,
    /* 1037 */ 1817, 2084, 2421, 2785, 2912, 3163, 2154, 2020, 3184, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747,
    /* 1054 */ 1747, 1747, 1785, 3042, 1934, 3054, 3062, 3070, 3075, 2007, 2049, 2447, 2928, 1746, 1764, 1817, 2084,
    /* 1071 */ 2421, 2785, 2341, 3220, 2589, 2020, 3184, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747,
    /* 1088 */ 1803, 1975, 2480, 1747, 3088, 1748, 2485, 3101, 2049, 2447, 2928, 1746, 1764, 1817, 2084, 2421, 2785,
    /* 1105 */ 2341, 3220, 2589, 2020, 3184, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 3093, 1975,
    /* 1122 */ 3092, 1747, 1747, 1747, 1747, 2007, 2694, 1747, 2928, 2069, 2898, 2590, 3128, 3020, 2552, 2341, 3002,
    /* 1139 */ 2583, 2020, 3184, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1785, 1975, 2684, 2690,
    /* 1156 */ 2690, 2087, 3143, 2007, 2049, 2120, 2928, 1746, 1764, 1817, 2084, 2421, 2785, 2341, 3220, 2589, 2020,
    /* 1173 */ 3184, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 2780, 2793, 2805, 2820, 2820, 2820,
    /* 1190 */ 2824, 2007, 1976, 2202, 2928, 2349, 2357, 2381, 2084, 3027, 2785, 2832, 2403, 2772, 2919, 3184, 1747,
    /* 1207 */ 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 2780, 2793, 2805, 2820, 2820, 2820, 2824, 2007,
    /* 1224 */ 1976, 2202, 2928, 2349, 2357, 2381, 2084, 3027, 3080, 2832, 2403, 2772, 2020, 3184, 1747, 1747, 1747,
    /* 1241 */ 1747, 1747, 1747, 1747, 1747, 1747, 1747, 2780, 2793, 2805, 2820, 2820, 2820, 2824, 2007, 1976, 2202,
    /* 1258 */ 2928, 2349, 2357, 2381, 2084, 3027, 2490, 2832, 3156, 2772, 2020, 3184, 1747, 1747, 1747, 1747, 1747,
    /* 1275 */ 1747, 1747, 1747, 1747, 1747, 2780, 2793, 2805, 2820, 2820, 2820, 2824, 2007, 1976, 2202, 2928, 2349,
    /* 1292 */ 2357, 2381, 2084, 3027, 2785, 2832, 3175, 2772, 2020, 3184, 1747, 1747, 1747, 1747, 1747, 1747, 1747,
    /* 1309 */ 1747, 1747, 1747, 2780, 2793, 2805, 2820, 2820, 2820, 2824, 2007, 2049, 2202, 2928, 2265, 1764, 1817,
    /* 1326 */ 2084, 2421, 2785, 2912, 3163, 2766, 2951, 3183, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747,
    /* 1343 */ 1747, 2780, 2793, 2805, 2820, 2820, 2820, 2824, 2007, 2049, 2202, 2928, 2265, 1764, 1817, 2084, 2421,
    /* 1360 */ 2785, 2912, 3163, 2160, 2020, 3184, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 2780,
    /* 1377 */ 2793, 2805, 2820, 2820, 2820, 2824, 2007, 2049, 2202, 2928, 2265, 1764, 1817, 2084, 2421, 2785, 2912,
    /* 1394 */ 3163, 2154, 3260, 2714, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 2780, 2793, 2805,
    /* 1411 */ 2820, 2820, 2820, 2824, 2007, 2049, 2202, 2928, 2265, 1764, 1817, 2084, 2421, 2785, 2912, 3163, 2154,
    /* 1428 */ 2020, 3214, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 2780, 2793, 2805, 2820, 2820,
    /* 1445 */ 2820, 2824, 2007, 2049, 2202, 2928, 2265, 1764, 1817, 2084, 2421, 2785, 2912, 3163, 3192, 2020, 3184,
    /* 1462 */ 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 2780, 2793, 2805, 2820, 2820, 2820, 2824,
    /* 1479 */ 2007, 2049, 2202, 2928, 2265, 1764, 3200, 2084, 2421, 2785, 2912, 3163, 2154, 2020, 3184, 1747, 1747,
    /* 1496 */ 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 2780, 2793, 2805, 2820, 2820, 2820, 2824, 2007, 2049,
    /* 1513 */ 2202, 2928, 2265, 1764, 1817, 2084, 2421, 2785, 2912, 3163, 2154, 2839, 3208, 1747, 1747, 1747, 1747,
    /* 1530 */ 1747, 1747, 1747, 1747, 1747, 1747, 2780, 2793, 2805, 2820, 2820, 2820, 2824, 2007, 2049, 2202, 2928,
    /* 1547 */ 2265, 3231, 3245, 2084, 2421, 2785, 2912, 3163, 2154, 2020, 3184, 1747, 1747, 1747, 1747, 1747, 1747,
    /* 1564 */ 1747, 1747, 1747, 1747, 2780, 2793, 2805, 2820, 2820, 2820, 2824, 2007, 1888, 2271, 2928, 3253, 1764,
    /* 1581 */ 1817, 2084, 2421, 3273, 3281, 3289, 2154, 2020, 3184, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747,
    /* 1598 */ 1747, 1747, 1785, 3301, 3310, 3302, 3330, 3323, 3335, 2007, 2049, 2447, 2928, 1746, 1764, 1817, 2084,
    /* 1615 */ 2421, 2785, 2341, 3220, 2589, 2020, 3184, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747,
    /* 1632 */ 1785, 1975, 2044, 1747, 1747, 1747, 2416, 2007, 2049, 2447, 2928, 2265, 1764, 1817, 2084, 2421, 2785,
    /* 1649 */ 2341, 3163, 2148, 2020, 3184, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 3046,
    /* 1666 */ 2373, 3348, 3359, 3362, 3351, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747,
    /* 1683 */ 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 1747, 0, 53, 0, 0, 567, 567, 567,
    /* 1703 */ 567, 0, 0, 567, 567, 567, 0, 53, 0, 567, 0, 3900, 3840, 567, 567, 567, 567, 567, 567, 567, 567, 109, 98,
    /* 1726 */ 98, 0, 85, 114, 0, 0, 0, 135, 0, 111, 0, 3200, 567, 567, 567, 2116, 2116, 567, 567, 567, 2304, 0, 0, 0,
    /* 1750 */ 0, 0, 0, 0, 0, 59, 0, 119, 120, 0, 109, 85, 0, 126, 85, 98, 98, 0, 85, 114, 0, 0, 0, 141, 133, 0, 101,
    /* 1777 */ 52115, 60, 0, 62, 62, 0, 0, 0, 0, 53, 0, 0, 0, 0, 0, 0, 0, 98, 62, 62, 62, 62, 62, 62, 62, 62, 0, 53, 0,
    /* 1806 */ 0, 0, 0, 0, 59, 0, 119, 0, 0, 85, 85, 0, 119, 0, 0, 85, 85, 0, 126, 0, 114, 114, 129, 0, 0, 0, 0, 56, 56,
    /* 1835 */ 56, 56, 0, 0, 4864, 0, 0, 0, 4864, 4864, 4864, 0, 53, 0, 0, 0, 0, 58, 0, 0, 0, 4864, 0, 4864, 4864, 4864,
    /* 1861 */ 4864, 4864, 4864, 4864, 4864, 60, 0, 63, 63, 0, 0, 0, 0, 66, 66, 0, 66, 63, 63, 63, 63, 63, 63, 63, 63,
    /* 1886 */ 0, 53, 0, 0, 0, 0, 34906, 0, 0, 93, 0, 0, 5376, 0, 0, 0, 5376, 5376, 5376, 0, 53, 0, 0, 0, 60, 0, 62, 63,
    /* 1914 */ 0, 0, 0, 5376, 0, 5376, 5376, 5376, 5376, 5376, 5376, 5376, 5376, 60, 0, 0, 0, 0, 5632, 5632, 0, 0, 0,
    /* 1937 */ 2116, 2116, 0, 72, 0, 0, 0, 5120, 5120, 5120, 0, 5120, 5120, 5120, 5120, 5120, 5120, 5120, 5120, 0, 53,
    /* 1958 */ 0, 0, 0, 0, 34816, 91, 92, 93, 0, 0, 5632, 2116, 2116, 0, 0, 0, 60, 60, 0, 0, 0, 0, 0, 0, 0, 93, 93, 0,
    /* 1986 */ 0, 5632, 0, 0, 0, 0, 0, 97, 0, 93, 99, 5632, 5632, 5632, 5632, 5632, 5632, 5632, 5632, 0, 53, 0, 0, 0,
    /* 2010 */ 60, 60, 62, 63, 0, 5969, 5969, 5969, 0, 53, 0, 0, 85, 85, 85, 85, 85, 85, 3328, 85, 3584, 6144, 0, 0, 0,
    /* 2035 */ 101, 598, 0, 102, 98, 98, 0, 101, 115, 0, 0, 0, 2116, 2116, 0, 0, 0, 0, 34816, 0, 0, 93, 0, 119, 0, 0,
    /* 2061 */ 101, 101, 0, 126, 3200, 115, 115, 0, 0, 0, 0, 0, 107, 107, 0, 0, 132, 0, 0, 101, 0, 98, 0, 3200, 114,
    /* 2086 */ 114, 0, 0, 0, 0, 0, 1353, 0, 1353, 101, 101, 52115, 0, 0, 0, 0, 101, 101, 101, 101, 101, 101, 174, 101,
    /* 2110 */ 101, 176, 101, 101, 0, 0, 0, 0, 101, 101, 0, 0, 0, 10240, 0, 0, 10333, 10339, 6482, 6482, 6482, 0, 53, 0,
    /* 2134 */ 0, 0, 60, 60, 62, 1536, 0, 0, 6656, 0, 0, 85, 598, 0, 85, 0, 0, 0, 0, 85, 85, 52128, 0, 0, 0, 85, 85,
    /* 2161 */ 52128, 0, 0, 0, 85, 164, 52128, 0, 0, 6912, 6912, 0, 0, 6912, 0, 0, 0, 33597, 33597, 62, 63, 8192, 6912,
    /* 2184 */ 6912, 6912, 1076, 53, 0, 0, 0, 60, 60, 1536, 63, 0, 2304, 0, 0, 0, 106, 106, 1375, 0, 0, 0, 0, 0, 93, 99,
    /* 2210 */ 85, 98, 112, 0, 85, 114, 116, 0, 0, 54, 0, 0, 0, 0, 0, 132, 0, 142, 0, 0, 0, 7168, 0, 0, 0, 7168, 7168,
    /* 2237 */ 7168, 1076, 53, 0, 0, 0, 69, 69, 0, 0, 0, 0, 34816, 0, 0, 94, 0, 0, 7168, 0, 7168, 7168, 7168, 7168,
    /* 2261 */ 7168, 7168, 7168, 7168, 2304, 0, 0, 0, 0, 0, 1375, 0, 96, 0, 0, 0, 93, 99, 60, 0, 56, 56, 56, 7480, 56,
    /* 2286 */ 56, 67, 7480, 2116, 2116, 67, 67, 56, 77, 7501, 77, 7480, 56, 7501, 67, 7480, 7501, 7501, 77, 7501, 7501,
    /* 2307 */ 7501, 7501, 7501, 7501, 7501, 7501, 0, 0, 0, 0, 598, 0, 93, 93, 0, 0, 598, 0, 34816, 0, 0, 93, 0, 119,
    /* 2331 */ 121, 0, 85, 85, 125, 119, 3200, 114, 4096, 0, 0, 0, 0, 0, 132, 0, 133, 0, 2304, 1895, 0, 0, 0, 0, 1375,
    /* 2356 */ 1888, 85, 98, 98, 0, 85, 114, 1895, 0, 0, 75, 0, 0, 0, 75, 75, 75, 0, 0, 0, 0, 0, 4352, 4352, 0, 0, 119,
    /* 2383 */ 0, 123, 85, 85, 0, 126, 0, 2698, 0, 0, 132, 0, 133, 0, 0, 85, 136, 0, 125, 3200, 85, 85, 52115, 3220, 0,
    /* 2408 */ 0, 157, 85, 0, 0, 0, 85, 114, 0, 0, 0, 1076, 53, 0, 0, 0, 85, 0, 98, 0, 3200, 60, 0, 0, 0, 0, 0, 0, 57,
    /* 2437 */ 0, 0, 0, 57, 0, 57, 0, 0, 0, 57, 0, 0, 0, 0, 0, 0, 93, 99, 87, 0, 0, 60, 60, 62, 63, 0, 0, 75, 0, 75, 75,
    /* 2468 */ 75, 75, 75, 75, 75, 75, 85, 111, 111, 0, 85, 114, 0, 0, 0, 2116, 2116, 0, 0, 59, 0, 53, 0, 0, 0, 132,
    /* 2494 */ 133, 0, 146, 52115, 0, 119, 0, 0, 85, 85, 0, 127, 60, 0, 0, 0, 64, 0, 0, 0, 100, 85, 598, 0, 85, 65, 64,
    /* 2521 */ 0, 2116, 2116, 70, 71, 65, 74, 74, 64, 70, 70, 70, 64, 64, 70, 74, 64, 70, 78, 78, 78, 78, 83, 83, 78,
    /* 2546 */ 83, 83, 83, 83, 0, 53, 0, 0, 0, 132, 133, 0, 85, 0, 7936, 0, 0, 0, 34816, 0, 0, 93, 0, 7936, 0, 0, 0, 0,
    /* 2574 */ 93, 99, 0, 0, 7936, 0, 85, 598, 0, 85, 0, 0, 0, 107, 85, 85, 0, 0, 0, 0, 85, 85, 0, 0, 0, 8960, 0, 60,
    /* 2602 */ 60, 62, 63, 0, 0, 81, 81, 5888, 5888, 5969, 5888, 0, 119, 122, 0, 85, 85, 0, 126, 0, 20992, 0, 85, 0, 98,
    /* 2627 */ 0, 3200, 114, 114, 0, 0, 131, 0, 0, 0, 2116, 2116, 0, 0, 9472, 0, 0, 0, 0, 0, 0, 132, 122, 21135, 0, 66,
    /* 2653 */ 0, 0, 2116, 2116, 0, 0, 66, 0, 0, 0, 0, 0, 0, 5632, 5632, 66, 66, 0, 0, 0, 0, 0, 0, 94, 94, 66, 66, 66,
    /* 2681 */ 0, 53, 9728, 0, 0, 0, 2116, 2116, 0, 0, 1353, 0, 0, 0, 0, 0, 0, 34816, 0, 0, 0, 0, 9216, 0, 0, 34816, 0,
    /* 2708 */ 0, 93, 8448, 8704, 85, 85, 85, 85, 85, 85, 175, 85, 85, 85, 33597, 54, 0, 0, 0, 0, 0, 9472, 9472, 9472,
    /* 2732 */ 9472, 9472, 9472, 9472, 0, 0, 0, 0, 0, 141, 0, 133, 0, 0, 92, 0, 0, 0, 8283, 93, 93, 0, 0, 8283, 0, 85,
    /* 2758 */ 598, 0, 85, 0, 0, 0, 157, 85, 85, 52128, 0, 0, 0, 163, 85, 52128, 0, 0, 157, 85, 85, 52128, 1076, 53, 0,
    /* 2783 */ 0, 1076, 0, 0, 0, 132, 133, 0, 85, 52115, 60, 0, 1076, 0, 0, 0, 0, 0, 142, 0, 133, 0, 0, 1076, 0, 2116,
    /* 2809 */ 2116, 1076, 1076, 0, 0, 82, 82, 6400, 6400, 6482, 6400, 1076, 1076, 1076, 1076, 1076, 1076, 1076, 1076,
    /* 2828 */ 53, 0, 0, 0, 3220, 2698, 0, 0, 132, 0, 133, 0, 0, 85, 85, 85, 85, 169, 85, 0, 0, 113, 512, 626, 0, 0, 0,
    /* 2855 */ 2116, 2116, 0, 0, 57, 0, 84, 0, 0, 0, 89, 34905, 0, 0, 93, 3200, 114, 114, 0, 130, 0, 0, 0, 132, 133, 0,
    /* 2881 */ 135, 52115, 2304, 1895, 104, 104, 0, 0, 1375, 1888, 85, 85, 52115, 3220, 155, 0, 157, 85, 0, 107, 0, 85,
    /* 2903 */ 114, 0, 0, 0, 2116, 2116, 0, 62, 0, 3220, 0, 0, 0, 132, 0, 133, 0, 0, 85, 85, 85, 168, 85, 85, 154, 0, 0,
    /* 2930 */ 0, 0, 85, 598, 0, 85, 85, 98, 98, 0, 85, 114, 0, 117, 3220, 0, 0, 150, 132, 0, 133, 0, 0, 85, 166, 167,
    /* 2956 */ 85, 85, 170, 118, 119, 0, 0, 85, 85, 0, 126, 0, 139, 0, 132, 133, 0, 85, 52115, 3220, 0, 149, 0, 132, 0,
    /* 2981 */ 133, 0, 0, 88, 0, 34816, 0, 0, 93, 85, 85, 52115, 3220, 0, 156, 0, 85, 0, 161, 162, 0, 85, 85, 0, 0, 0,
    /* 3007 */ 0, 0, 85, 598, 7680, 85, 2304, 0, 105, 0, 0, 0, 1375, 0, 0, 107, 85, 0, 107, 107, 0, 0, 123, 85, 0, 98,
    /* 3033 */ 0, 3200, 114, 114, 0, 0, 0, 0, 122, 60, 0, 0, 9984, 0, 0, 0, 0, 4352, 0, 0, 0, 72, 72, 10060, 72, 72, 72,
    /* 3060 */ 10060, 10060, 72, 72, 10060, 72, 10063, 10063, 10063, 10064, 10063, 10063, 10063, 10063, 10063, 10063,
    /* 3076 */ 10063, 10063, 0, 53, 0, 0, 0, 132, 133, 0, 145, 52115, 0, 59, 0, 0, 0, 0, 0, 0, 10752, 0, 0, 0, 0, 0, 0,
    /* 3103 */ 10496, 60, 60, 62, 63, 0, 0, 134, 85, 0, 98, 0, 3200, 114, 114, 0, 0, 0, 0, 120, 0, 109, 85, 0, 98, 0,
    /* 3129 */ 114, 114, 0, 0, 0, 107, 0, 0, 140, 132, 133, 0, 85, 52115, 0, 1353, 1353, 0, 53, 0, 0, 0, 132, 133, 144,
    /* 3154 */ 85, 0, 152, 85, 52115, 3220, 0, 0, 157, 85, 85, 52115, 3220, 0, 0, 0, 85, 0, 0, 0, 137, 85, 85, 52115,
    /* 3178 */ 3220, 0, 0, 157, 158, 171, 85, 85, 85, 85, 85, 85, 85, 85, 159, 52128, 0, 0, 0, 85, 85, 52128, 0, 119, 0,
    /* 3203 */ 0, 85, 124, 0, 126, 85, 172, 85, 85, 85, 85, 85, 85, 173, 85, 85, 85, 85, 85, 52115, 0, 0, 0, 0, 85, 512,
    /* 3229 */ 0, 85, 110, 98, 98, 0, 85, 114, 0, 0, 0, 2116, 2116, 0, 63, 0, 0, 119, 0, 0, 110, 85, 0, 126, 2304, 0, 0,
    /* 3256 */ 0, 0, 108, 1375, 0, 0, 165, 85, 85, 85, 85, 85, 172, 85, 85, 175, 85, 138, 0, 0, 132, 133, 0, 85, 52115,
    /* 3281 */ 3220, 0, 0, 0, 132, 0, 133, 151, 85, 153, 52115, 3220, 0, 0, 0, 85, 0, 0, 0, 3200, 60, 0, 0, 0, 0, 0,
    /* 3307 */ 11008, 0, 11008, 0, 0, 11008, 2116, 2116, 0, 0, 0, 132, 142, 0, 135, 52115, 11008, 11008, 11008, 11008,
    /* 3327 */ 11008, 11008, 11008, 11008, 0, 11008, 11008, 0, 11008, 11008, 11008, 0, 53, 0, 0, 0, 132, 143, 0, 85,
    /* 3347 */ 52115, 4352, 4352, 0, 4352, 4352, 4352, 0, 0, 0, 0, 0, 4352, 4352, 0, 4352, 4352, 4352, 4352, 4352, 4352,
    /* 3368 */ 4352, 4352
  };

  private static final int[] EXPECTED =
  {
    /*   0 */ 88, 99, 105, 109, 113, 121, 125, 129, 133, 137, 141, 145, 149, 196, 163, 169, 173, 116, 182, 189, 193,
    /*  21 */ 101, 176, 233, 206, 199, 212, 218, 224, 211, 213, 220, 210, 214, 209, 231, 245, 117, 237, 244, 155, 157,
    /*  42 */ 157, 159, 284, 164, 249, 278, 152, 227, 253, 256, 260, 264, 265, 269, 273, 282, 165, 90, 276, 240, 202,
    /*  63 */ 185, 178, 90, 239, 90, 90, 91, 95, 90, 90, 94, 90, 90, 93, 90, 92, 90, 90, 94, 92, 90, 94, 90, 90, 90, 8,
    /*  89 */ 2048, 0, 0, 0, 0, 1, 2, 0, 0, 0, 10, 536870914, 2, 2, 536870912, 32770, 16388, 16384, 42, 34, -2147418110,
    /* 110 */ 270532610, 2097154, 536870914, 1073741826, -2147450870, 270532610, 960, 0, 0, 131072, 8194, 960, 98314,
    /* 123 */ 229418, 1610612754, 1073971242, 1073971258, -2011955094, 241706, 268677162, 241706, -1741422486,
    /* 132 */ -2009857942, 268677162, 1610842170, -1741422486, 269201450, -1742753686, -2011189142, -1743277974,
    /* 140 */ -1742752662, -1740656534, -1203785622, -1631604630, -1094733718, -645943190, -645943174, -1610633110,
    /* 148 */ -109072262, -536891286, -536891270, -20358, 8, 0, 1024, 0, 0, 8194, 4098, 8194, 4098, 8194, 8194,
    /* 163 */ 536870912, 0, 0, 4, 256, 0, 4, 32, 32, -2147483648, 1073741824, 0, 32770, 256, 576, 0, 0, 3, 0, 16,
    /* 183 */ 1073741824, 64, 0, 3, 128, 128, 45058, -2147483648, 131072, 132096, 8388608, 33554432, -1073741824, 2048,
    /* 197 */ 0, 2, 2, 32770, 576, 0, 1, 0, 128, 45058, 1024, 132096, 2048, 512, 0, 0, 131072, 131072, 131072, 12290,
    /* 217 */ 1024, 12290, 12290, 1024, 132096, 132096, 2048, 2048, 32768, 32768, 512, 3, 1, 280, 131072, 131072,
    /* 233 */ 131072, 131072, 16, 64, 4098, 1024, 0, 0, 3, 1, 0, 131072, 8194, 4098, 1024, 2048, 0, 32, 0, 64, 9, 9,
    /* 255 */ 192, 3, 1027, 192, 1216, 1027, 281, 1216, 1027, 195, 1219, 1219, 1219, 1219, 1243, 1243, 1235, 1499, 1243,
    /* 274 */ 1243, 1499, 0, 8, 0, 0, 1024, 16, 0, 32, 0, 0, 32, 512
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
