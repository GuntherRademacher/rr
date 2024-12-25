// This file was generated on Tue Dec 24, 2024 22:28 (UTC+01) by REx v6.1-SNAPSHOT which is Copyright (c) 1979-2024 by Gunther Rademacher <grd@gmx.net>
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
      if (l1 != 29)                 // '<?'
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
    if (l1 == 31)                   // '<?TOKENS?>'
    {
      consume(31);                  // '<?TOKENS?>'
      for (;;)
      {
        lookahead1W(22);            // Whitespace | NCName | StringLiteral | DocComment | EOF | EquivalenceLookAhead |
                                    // '<?ENCORE?>'
        if (l1 == 16                // EOF
         || l1 == 30)               // '<?ENCORE?>'
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
        case 1731:                  // NCName '::='
        case 2179:                  // NCName '?'
          whitespace();
          parse_Production();
          break;
        case 2435:                  // NCName '\\'
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
    if (l1 == 30)                   // '<?ENCORE?>'
    {
      consume(30);                  // '<?ENCORE?>'
      for (;;)
      {
        lookahead1W(12);            // Whitespace | EOF | '<?'
        if (l1 != 29)               // '<?'
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
    consume(29);                    // '<?'
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
    consume(35);                    // '?>'
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
    if (l1 == 34)                   // '?'
    {
      consume(34);                  // '?'
    }
    lookahead1W(5);                 // Whitespace | '::='
    consume(27);                    // '::='
    lookahead1W(38);                // Whitespace | NCName | StringLiteral | CharCode | UrlIntroducer | WsExplicit |
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
    if (l1 == 26                    // '/'
     || l1 == 40)                   // '|'
    {
      switch (l1)
      {
      case 40:                      // '|'
        for (;;)
        {
          consume(40);              // '|'
          lookahead1W(37);          // Whitespace | NCName | StringLiteral | CharCode | WsExplicit | WsDefinition |
                                    // DocComment | EOF | EquivalenceLookAhead | '$' | '&' | '(' | '.' | '<?' |
                                    // '<?ENCORE?>' | '<?TOKENS?>' | '[' | '[^' | '|'
          whitespace();
          parse_Alternative();
          lookahead1W(29);          // Whitespace | NCName | StringLiteral | WsExplicit | WsDefinition | DocComment |
                                    // EOF | EquivalenceLookAhead | '<?ENCORE?>' | '<?TOKENS?>' | '|'
          if (l1 != 40)             // '|'
          {
            break;
          }
        }
        break;
      default:
        for (;;)
        {
          consume(26);              // '/'
          lookahead1W(36);          // Whitespace | NCName | StringLiteral | CharCode | WsExplicit | WsDefinition |
                                    // DocComment | EOF | EquivalenceLookAhead | '$' | '&' | '(' | '.' | '/' | '<?' |
                                    // '<?ENCORE?>' | '<?TOKENS?>' | '[' | '[^'
          whitespace();
          parse_Alternative();
          lookahead1W(28);          // Whitespace | NCName | StringLiteral | WsExplicit | WsDefinition | DocComment |
                                    // EOF | EquivalenceLookAhead | '/' | '<?ENCORE?>' | '<?TOKENS?>'
          if (l1 != 26)             // '/'
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
    parse_SequenceOrDifference();
    lookahead1W(35);                // Whitespace | NCName | StringLiteral | WsExplicit | WsDefinition | DocComment |
                                    // EOF | EquivalenceLookAhead | '&' | '/' | '<?ENCORE?>' | '<?TOKENS?>' | '|'
    if (l1 == 19)                   // '&'
    {
      consume(19);                  // '&'
      lookahead1W(26);              // Whitespace | NCName | StringLiteral | CharCode | '$' | '(' | '.' | '<?' | '[' |
                                    // '[^'
      whitespace();
      parse_Item();
    }
    eventHandler.endNonterminal("Alternative", e0);
  }

  private void parse_SequenceOrDifference()
  {
    eventHandler.startNonterminal("SequenceOrDifference", e0);
    switch (l1)
    {
    case 3:                         // NCName
      lookahead2W(49);              // Whitespace | NCName | Context | StringLiteral | CharCode | WsExplicit |
                                    // WsDefinition | DocComment | EOF | EquivalenceLookAhead | '$' | '&' | '(' | ')' |
                                    // '*' | '+' | '-' | '.' | '/' | '::=' | '<<' | '<?' | '<?ENCORE?>' | '<?TOKENS?>' |
                                    // '>>' | '?' | '[' | '[^' | '\\' | '|'
      switch (lk)
      {
      case 259:                     // NCName Context
        lookahead3W(46);            // Whitespace | NCName | StringLiteral | CharCode | WsExplicit | WsDefinition |
                                    // DocComment | EOF | EquivalenceLookAhead | '$' | '&' | '(' | ')' | '*' | '+' |
                                    // '-' | '.' | '/' | '<<' | '<?' | '<?ENCORE?>' | '<?TOKENS?>' | '>>' | '?' | '[' |
                                    // '[^' | '|'
        break;
      case 2179:                    // NCName '?'
        lookahead3W(42);            // Whitespace | NCName | StringLiteral | CharCode | WsExplicit | WsDefinition |
                                    // DocComment | EOF | EquivalenceLookAhead | '$' | '&' | '(' | ')' | '-' | '.' |
                                    // '/' | '::=' | '<?' | '<?ENCORE?>' | '<?TOKENS?>' | '[' | '[^' | '|'
        break;
      }
      break;
    case 5:                         // StringLiteral
      lookahead2W(47);              // Whitespace | NCName | Context | StringLiteral | CharCode | WsExplicit |
                                    // WsDefinition | DocComment | EOF | EquivalenceLookAhead | '$' | '&' | '(' | ')' |
                                    // '*' | '+' | '-' | '.' | '/' | '<<' | '<?' | '<?ENCORE?>' | '<?TOKENS?>' | '>>' |
                                    // '?' | '[' | '[^' | '|'
      switch (lk)
      {
      case 261:                     // StringLiteral Context
        lookahead3W(46);            // Whitespace | NCName | StringLiteral | CharCode | WsExplicit | WsDefinition |
                                    // DocComment | EOF | EquivalenceLookAhead | '$' | '&' | '(' | ')' | '*' | '+' |
                                    // '-' | '.' | '/' | '<<' | '<?' | '<?ENCORE?>' | '<?TOKENS?>' | '>>' | '?' | '[' |
                                    // '[^' | '|'
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
     && lk != 26                    // '/'
     && lk != 30                    // '<?ENCORE?>'
     && lk != 31                    // '<?TOKENS?>'
     && lk != 40                    // '|'
     && lk != 1731                  // NCName '::='
     && lk != 1795                  // NCName '<<'
     && lk != 1797                  // StringLiteral '<<'
     && lk != 2115                  // NCName '>>'
     && lk != 2117                  // StringLiteral '>>'
     && lk != 2435                  // NCName '\\'
     && lk != 112771                // NCName '?' '::='
     && lk != 114947                // NCName Context '<<'
     && lk != 114949                // StringLiteral Context '<<'
     && lk != 135427                // NCName Context '>>'
     && lk != 135429)               // StringLiteral Context '>>'
    {
      whitespace();
      parse_Item();
      lookahead1W(40);              // Whitespace | NCName | StringLiteral | CharCode | WsExplicit | WsDefinition |
                                    // DocComment | EOF | EquivalenceLookAhead | '$' | '&' | '(' | ')' | '-' | '.' |
                                    // '/' | '<?' | '<?ENCORE?>' | '<?TOKENS?>' | '[' | '[^' | '|'
      switch (l1)
      {
      case 24:                      // '-'
        consume(24);                // '-'
        lookahead1W(26);            // Whitespace | NCName | StringLiteral | CharCode | '$' | '(' | '.' | '<?' | '[' |
                                    // '[^'
        whitespace();
        parse_Item();
        break;
      default:
        for (;;)
        {
          lookahead1W(39);          // Whitespace | NCName | StringLiteral | CharCode | WsExplicit | WsDefinition |
                                    // DocComment | EOF | EquivalenceLookAhead | '$' | '&' | '(' | ')' | '.' | '/' |
                                    // '<?' | '<?ENCORE?>' | '<?TOKENS?>' | '[' | '[^' | '|'
          switch (l1)
          {
          case 3:                   // NCName
            lookahead2W(48);        // Whitespace | NCName | Context | StringLiteral | CharCode | WsExplicit |
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
            case 2179:              // NCName '?'
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
           || lk == 26              // '/'
           || lk == 30              // '<?ENCORE?>'
           || lk == 31              // '<?TOKENS?>'
           || lk == 40              // '|'
           || lk == 1731            // NCName '::='
           || lk == 1795            // NCName '<<'
           || lk == 1797            // StringLiteral '<<'
           || lk == 2115            // NCName '>>'
           || lk == 2117            // StringLiteral '>>'
           || lk == 2435            // NCName '\\'
           || lk == 112771          // NCName '?' '::='
           || lk == 114947          // NCName Context '<<'
           || lk == 114949          // StringLiteral Context '<<'
           || lk == 135427          // NCName Context '>>'
           || lk == 135429)         // StringLiteral Context '>>'
          {
            break;
          }
          whitespace();
          parse_Item();
        }
      }
    }
    eventHandler.endNonterminal("SequenceOrDifference", e0);
  }

  private void parse_Item()
  {
    eventHandler.startNonterminal("Item", e0);
    parse_Primary();
    lookahead1W(43);                // Whitespace | NCName | StringLiteral | CharCode | WsExplicit | WsDefinition |
                                    // DocComment | EOF | EquivalenceLookAhead | '$' | '&' | '(' | ')' | '*' | '+' |
                                    // '-' | '.' | '/' | '<?' | '<?ENCORE?>' | '<?TOKENS?>' | '?' | '[' | '[^' | '|'
    if (l1 == 22                    // '*'
     || l1 == 23                    // '+'
     || l1 == 34)                   // '?'
    {
      switch (l1)
      {
      case 34:                      // '?'
        consume(34);                // '?'
        break;
      case 22:                      // '*'
        consume(22);                // '*'
        break;
      default:
        consume(23);                // '+'
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
    case 29:                        // '<?'
      parse_ProcessingInstruction();
      break;
    case 6:                         // CharCode
      consume(6);                   // CharCode
      break;
    case 18:                        // '$'
      consume(18);                  // '$'
      break;
    case 25:                        // '.'
      consume(25);                  // '.'
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
    parse_SequenceOrDifference();
    lookahead1W(18);                // Whitespace | ')' | '/' | '|'
    if (l1 != 21)                   // ')'
    {
      switch (l1)
      {
      case 40:                      // '|'
        for (;;)
        {
          consume(40);              // '|'
          lookahead1W(31);          // Whitespace | NCName | StringLiteral | CharCode | '$' | '(' | ')' | '.' | '<?' |
                                    // '[' | '[^' | '|'
          whitespace();
          parse_SequenceOrDifference();
          lookahead1W(14);          // Whitespace | ')' | '|'
          if (l1 != 40)             // '|'
          {
            break;
          }
        }
        break;
      default:
        for (;;)
        {
          consume(26);              // '/'
          lookahead1W(30);          // Whitespace | NCName | StringLiteral | CharCode | '$' | '(' | ')' | '.' | '/' |
                                    // '<?' | '[' | '[^'
          whitespace();
          parse_SequenceOrDifference();
          lookahead1W(13);          // Whitespace | ')' | '/'
          if (l1 != 26)             // '/'
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
      lookahead1W(47);              // Whitespace | NCName | Context | StringLiteral | CharCode | WsExplicit |
                                    // WsDefinition | DocComment | EOF | EquivalenceLookAhead | '$' | '&' | '(' | ')' |
                                    // '*' | '+' | '-' | '.' | '/' | '<<' | '<?' | '<?ENCORE?>' | '<?TOKENS?>' | '>>' |
                                    // '?' | '[' | '[^' | '|'
      if (l1 == 4)                  // Context
      {
        consume(4);                 // Context
      }
      break;
    default:
      consume(5);                   // StringLiteral
      lookahead1W(47);              // Whitespace | NCName | Context | StringLiteral | CharCode | WsExplicit |
                                    // WsDefinition | DocComment | EOF | EquivalenceLookAhead | '$' | '&' | '(' | ')' |
                                    // '*' | '+' | '-' | '.' | '/' | '<<' | '<?' | '<?ENCORE?>' | '<?TOKENS?>' | '>>' |
                                    // '?' | '[' | '[^' | '|'
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
    case 36:                        // '['
      consume(36);                  // '['
      break;
    default:
      consume(37);                  // '[^'
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
      if (l1 == 39)                 // ']'
      {
        break;
      }
    }
    consume(39);                    // ']'
    eventHandler.endNonterminal("CharClass", e0);
  }

  private void parse_Link()
  {
    eventHandler.startNonterminal("Link", e0);
    consume(10);                    // UrlIntroducer
    lookahead1(1);                  // URL
    consume(11);                    // URL
    lookahead1(3);                  // ']'
    consume(39);                    // ']'
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
    case 33:                        // '>>'
      consume(33);                  // '>>'
      break;
    default:
      consume(28);                  // '<<'
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
       || lk == 30                  // '<?ENCORE?>'
       || lk == 1731                // NCName '::='
       || lk == 1795                // NCName '<<'
       || lk == 1797                // StringLiteral '<<'
       || lk == 2115                // NCName '>>'
       || lk == 2117                // StringLiteral '>>'
       || lk == 2179                // NCName '?'
       || lk == 2435                // NCName '\\'
       || lk == 114947              // NCName Context '<<'
       || lk == 114949              // StringLiteral Context '<<'
       || lk == 135427              // NCName Context '>>'
       || lk == 135429)             // StringLiteral Context '>>'
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
    consume(38);                    // '\\'
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
       || lk == 30                  // '<?ENCORE?>'
       || lk == 1731                // NCName '::='
       || lk == 1795                // NCName '<<'
       || lk == 1797                // StringLiteral '<<'
       || lk == 2115                // NCName '>>'
       || lk == 2117                // StringLiteral '>>'
       || lk == 2179                // NCName '?'
       || lk == 2435                // NCName '\\'
       || lk == 114947              // NCName Context '<<'
       || lk == 114949              // StringLiteral Context '<<'
       || lk == 135427              // NCName Context '>>'
       || lk == 135429)             // StringLiteral Context '>>'
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
    consume(32);                    // '=='
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
      consume(36);                  // '['
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
      consume(39);                  // ']'
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
    for (int i = 0; i < 41; i += 32)
    {
      int j = i;
      int i0 = (i >> 5) * 173 + s - 1;
      int i1 = i0 >> 2;
      int f = EXPECTED[(i0 & 3) + EXPECTED[(i1 & 7) + EXPECTED[i1 >> 3]]];
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
    /* 29 */ 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50
  };

  private static final int[] TRANSITION =
  {
    /*    0 */ 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812,
    /*   17 */ 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 2076, 1696,
    /*   34 */ 2081, 1698, 1698, 1698, 1706, 1808, 2452, 3100, 1723, 2334, 2761, 1735, 2764, 1976, 1743, 1761, 1903,
    /*   51 */ 1905, 3144, 3198, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 2076, 1696, 2081, 1698,
    /*   68 */ 1698, 1698, 1706, 1808, 2452, 3100, 3225, 2334, 2761, 1735, 2764, 1976, 1769, 1782, 1903, 1905, 3144,
    /*   85 */ 3198, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 2076, 1696, 2086, 1698, 1698, 1698,
    /*  102 */ 1706, 1796, 2505, 2602, 1790, 2334, 2843, 1821, 2764, 1976, 1743, 1761, 1903, 1905, 3144, 3198, 1812,
    /*  119 */ 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1983, 1811, 2023, 1812, 1812, 1812, 1981, 1796,
    /*  136 */ 2505, 2602, 1790, 2530, 3065, 2349, 1812, 1976, 1829, 1761, 1903, 1905, 3144, 3043, 1812, 1812, 1812,
    /*  153 */ 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1983, 1842, 3303, 1854, 1854, 1854, 1860, 2048, 2505, 2602,
    /*  170 */ 1790, 2530, 3065, 2349, 1812, 1976, 1829, 1761, 1903, 1905, 3144, 3043, 1812, 1812, 1812, 1812, 1812,
    /*  187 */ 1812, 1812, 1812, 1812, 1812, 1812, 1811, 1748, 2266, 2363, 2367, 2272, 1796, 2636, 2647, 1790, 2252,
    /*  204 */ 3065, 2990, 1870, 1727, 1879, 1891, 1903, 1905, 3144, 3043, 1812, 1812, 1812, 1812, 1812, 1812, 1812,
    /*  221 */ 1812, 1812, 1812, 1983, 1811, 2023, 2970, 3028, 3032, 2976, 1796, 2505, 2602, 1790, 2530, 3065, 2349,
    /*  238 */ 1812, 1976, 1829, 1761, 1903, 1905, 3144, 3043, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812,
    /*  255 */ 1812, 1983, 1811, 2023, 1812, 2940, 2943, 1899, 1796, 2505, 2602, 1790, 2530, 3065, 2349, 1812, 1976,
    /*  272 */ 1829, 1761, 1903, 1905, 3144, 3043, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1983,
    /*  289 */ 1913, 2295, 1925, 1925, 1925, 1931, 2042, 2505, 2602, 1790, 2530, 3065, 2349, 1812, 1976, 1829, 1761,
    /*  306 */ 1903, 1905, 3144, 3043, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1983, 1811, 2023,
    /*  323 */ 3159, 3244, 3248, 3165, 1796, 2505, 2602, 1790, 2530, 3065, 2349, 1812, 1976, 1829, 1761, 1903, 1905,
    /*  340 */ 3144, 3043, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1983, 1945, 2018, 2540, 2013,
    /*  357 */ 1962, 1950, 1796, 2505, 2602, 1790, 2530, 3065, 2349, 1812, 1976, 1829, 1761, 1903, 1905, 3144, 3043,
    /*  374 */ 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1983, 1811, 2023, 1812, 1812, 2398, 1970,
    /*  391 */ 1796, 1993, 1753, 2036, 2902, 2062, 2094, 2698, 2130, 2102, 2115, 1935, 1937, 2123, 2143, 1812, 1812,
    /*  408 */ 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1983, 1811, 2023, 1812, 1812, 2411, 2156, 1796, 2505,
    /*  425 */ 2602, 1790, 2530, 3065, 2349, 1812, 1976, 1829, 1761, 1903, 1905, 3144, 3043, 1812, 1812, 1812, 1812,
    /*  442 */ 1812, 1812, 1812, 1812, 1812, 1812, 1983, 1811, 2023, 1812, 1812, 2169, 2177, 1796, 2478, 2602, 1790,
    /*  459 */ 2190, 3091, 2349, 1871, 1976, 1829, 1761, 2777, 2790, 3144, 3043, 1812, 1812, 1812, 1812, 1812, 1812,
    /*  476 */ 1812, 1812, 1812, 1812, 1983, 1811, 2023, 3292, 3072, 3076, 3298, 1796, 2505, 2602, 1790, 1774, 3065,
    /*  493 */ 2349, 1812, 1976, 1829, 1761, 2777, 2790, 3144, 3043, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812,
    /*  510 */ 1812, 1812, 1846, 2198, 2205, 2210, 2225, 2218, 2231, 1796, 2242, 2234, 2260, 2252, 2282, 2662, 3014,
    /*  527 */ 2290, 1829, 2303, 1903, 1905, 3144, 2311, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812,
    /*  544 */ 1983, 1811, 2023, 1812, 1812, 1812, 2935, 1796, 1985, 2602, 2324, 1834, 3123, 2723, 1813, 1976, 2342,
    /*  561 */ 1761, 2357, 2708, 3144, 3043, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 2377, 2375,
    /*  578 */ 2135, 1812, 3098, 2249, 2385, 1796, 2505, 2591, 1790, 1715, 3065, 2568, 1812, 2393, 1829, 1761, 1903,
    /*  595 */ 1905, 3144, 3043, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1983, 2406, 2419, 2427,
    /*  612 */ 2435, 2439, 2447, 2054, 2505, 2464, 1790, 2530, 3065, 2349, 1812, 1976, 1829, 1761, 1903, 1905, 3144,
    /*  629 */ 3043, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 2978, 1811, 2023, 1812, 1812, 1812,
    /*  646 */ 1981, 2472, 2505, 2602, 2486, 2530, 2813, 2349, 3169, 1976, 2500, 2513, 1903, 1905, 3144, 3043, 1812,
    /*  663 */ 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1983, 1811, 2521, 2538, 2527, 1883, 2548, 1802,
    /*  680 */ 2561, 2602, 1790, 2530, 3065, 2349, 1812, 1976, 1829, 1761, 1903, 2623, 3144, 3043, 1812, 1812, 1812,
    /*  697 */ 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1712, 2576, 2182, 1812, 2588, 2580, 2599, 2069, 2680, 2610,
    /*  714 */ 1790, 2252, 3065, 3130, 1812, 3202, 1829, 1761, 1917, 2618, 3144, 3043, 1812, 1812, 1812, 1812, 1812,
    /*  731 */ 1812, 1812, 1812, 1812, 1812, 2631, 2644, 2655, 2670, 2670, 2670, 2675, 1796, 2274, 2602, 2324, 1834,
    /*  748 */ 3123, 2723, 1813, 1976, 2688, 1761, 2706, 2708, 3144, 3043, 1812, 1812, 1812, 1812, 1812, 1812, 1812,
    /*  765 */ 1812, 1812, 1812, 2631, 2644, 2655, 2670, 2670, 2670, 2675, 1796, 2274, 2602, 2324, 1834, 3123, 2723,
    /*  782 */ 2716, 1976, 2688, 1761, 2706, 2708, 3144, 3043, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812,
    /*  799 */ 1812, 2631, 2644, 2655, 2670, 2670, 2670, 2675, 1796, 2274, 2602, 2731, 2738, 3123, 2723, 1813, 2746,
    /*  816 */ 2688, 1761, 2754, 2708, 3144, 3043, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 2631,
    /*  833 */ 2644, 2655, 2670, 2670, 2670, 2675, 1796, 2553, 2602, 1790, 1774, 3065, 2349, 1812, 1976, 2772, 1761,
    /*  850 */ 2788, 2790, 3144, 3043, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 2631, 2644, 2655,
    /*  867 */ 2670, 2670, 2670, 2675, 1796, 2553, 2602, 1790, 1774, 3065, 2349, 2899, 1976, 2772, 1761, 2788, 2790,
    /*  884 */ 3144, 3043, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 2631, 2644, 2655, 2670, 2670,
    /*  901 */ 2670, 2675, 1796, 2553, 2602, 1790, 1774, 3058, 2349, 1812, 1976, 2772, 2798, 2788, 2790, 3144, 3043,
    /*  918 */ 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 2631, 2644, 2655, 2670, 2670, 2670, 2675,
    /*  935 */ 1796, 2553, 2602, 1790, 1774, 2836, 2349, 1812, 2806, 2772, 1761, 2788, 2790, 3144, 3043, 1812, 1812,
    /*  952 */ 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 2631, 2644, 2655, 2670, 2670, 2670, 2675, 1796, 2553,
    /*  969 */ 2602, 1790, 1774, 3065, 2349, 1812, 1976, 2821, 1761, 2788, 2790, 3144, 3043, 1812, 1812, 1812, 1812,
    /*  986 */ 1812, 1812, 1812, 1812, 1812, 1812, 2631, 2644, 2655, 2670, 2670, 2670, 2675, 1796, 2553, 2602, 1790,
    /* 1003 */ 1774, 3065, 2349, 1812, 1976, 2772, 1761, 2829, 2790, 3144, 3043, 1812, 1812, 1812, 1812, 1812, 1812,
    /* 1020 */ 1812, 1812, 1812, 1812, 2631, 2644, 2655, 2670, 2670, 2670, 2675, 1796, 2553, 2602, 2851, 1774, 3065,
    /* 1037 */ 2349, 1812, 1976, 2772, 1761, 2788, 2790, 3144, 3043, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812,
    /* 1054 */ 1812, 1812, 1983, 2859, 2316, 2872, 2880, 2887, 2893, 1796, 2505, 2602, 1790, 2530, 3065, 2349, 1812,
    /* 1071 */ 1976, 1829, 1761, 1903, 1905, 3144, 3043, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812,
    /* 1088 */ 1862, 1811, 2107, 1812, 2331, 1812, 2910, 2929, 2505, 2602, 1790, 2530, 3065, 2349, 1812, 1976, 1829,
    /* 1105 */ 1761, 1903, 1905, 3144, 3043, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 2161, 1811,
    /* 1122 */ 2160, 1812, 1812, 1812, 1812, 1796, 2452, 1812, 1723, 2951, 2695, 2780, 3230, 2956, 2964, 2986, 1903,
    /* 1139 */ 2998, 3144, 3043, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1983, 1811, 2148, 3011,
    /* 1156 */ 3011, 3318, 3022, 1796, 2505, 2000, 1790, 2530, 3065, 2349, 1812, 1976, 1829, 1761, 1903, 1905, 3144,
    /* 1173 */ 3043, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 2631, 2644, 2655, 2670, 2670, 2670,
    /* 1190 */ 2675, 1796, 2274, 2602, 2324, 1834, 3123, 2723, 1813, 1976, 2688, 1761, 2706, 2708, 3040, 3043, 1812,
    /* 1207 */ 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 2631, 2644, 2655, 2670, 2670, 2670, 2675, 1796,
    /* 1224 */ 2274, 2602, 2324, 1834, 3123, 2723, 1813, 1976, 3051, 1761, 2706, 2708, 3144, 3043, 1812, 1812, 1812,
    /* 1241 */ 1812, 1812, 1812, 1812, 1812, 1812, 1812, 2631, 2644, 2655, 2670, 2670, 2670, 2675, 1796, 2274, 2602,
    /* 1258 */ 2324, 1834, 3123, 2723, 1813, 1976, 3084, 3108, 2706, 2708, 3144, 3043, 1812, 1812, 1812, 1812, 1812,
    /* 1275 */ 1812, 1812, 1812, 1812, 1812, 2631, 2644, 2655, 2670, 2670, 2670, 2675, 1796, 2274, 2602, 2324, 1834,
    /* 1292 */ 3123, 2723, 1813, 1976, 2688, 1761, 3116, 2708, 3144, 3043, 1812, 1812, 1812, 1812, 1812, 1812, 1812,
    /* 1309 */ 1812, 1812, 1812, 2631, 2644, 2655, 2670, 2670, 2670, 2675, 1796, 2553, 2602, 1790, 1774, 3065, 2349,
    /* 1326 */ 1812, 1976, 2772, 1761, 2788, 2916, 3138, 3043, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812,
    /* 1343 */ 1812, 2631, 2644, 2655, 2670, 2670, 2670, 2675, 1796, 2553, 2602, 1790, 1774, 3065, 2349, 1812, 1976,
    /* 1360 */ 2772, 1761, 2788, 2921, 3144, 3043, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 2631,
    /* 1377 */ 2644, 2655, 2670, 2670, 2670, 2675, 1796, 2553, 2602, 1790, 1774, 3065, 2349, 1812, 1976, 2772, 1761,
    /* 1394 */ 2788, 3003, 3144, 3153, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 2631, 2644, 2655,
    /* 1411 */ 2670, 2670, 2670, 2675, 1796, 2553, 2602, 1790, 1774, 3065, 2349, 1812, 1976, 2772, 1761, 2788, 2790,
    /* 1428 */ 3145, 3043, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 2631, 2644, 2655, 2670, 2670,
    /* 1445 */ 2670, 2675, 1796, 2553, 2602, 1790, 1774, 3065, 2349, 1812, 1976, 2772, 1761, 3177, 2790, 3144, 3043,
    /* 1462 */ 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 2631, 2644, 2655, 2670, 2670, 2670, 2675,
    /* 1479 */ 1796, 2553, 2602, 1790, 1774, 3065, 3184, 1812, 1976, 2772, 1761, 2788, 2790, 3144, 3043, 1812, 1812,
    /* 1496 */ 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 2631, 2644, 2655, 2670, 2670, 2670, 2675, 1796, 2553,
    /* 1513 */ 2602, 1790, 1774, 3065, 2349, 1812, 1976, 2772, 1761, 2788, 2790, 3192, 3043, 1812, 1812, 1812, 1812,
    /* 1530 */ 1812, 1812, 1812, 1812, 1812, 1812, 2631, 2644, 2655, 2670, 2670, 2670, 2675, 1796, 2553, 2602, 1790,
    /* 1547 */ 2028, 3065, 3210, 1812, 1976, 2772, 1761, 2788, 2790, 3144, 3043, 1812, 1812, 1812, 1812, 1812, 1812,
    /* 1564 */ 1812, 1812, 1812, 1812, 2631, 2644, 2655, 2670, 2670, 2670, 2675, 1796, 2864, 3218, 1790, 2492, 3065,
    /* 1581 */ 2349, 1812, 3238, 2772, 3256, 2788, 2790, 3144, 3043, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812,
    /* 1598 */ 1812, 1812, 1983, 3264, 2007, 3265, 3273, 3278, 3286, 1796, 2505, 2602, 1790, 2530, 3065, 2349, 1812,
    /* 1615 */ 1976, 1829, 1761, 1903, 1905, 3144, 3043, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812,
    /* 1632 */ 1983, 1811, 2023, 1812, 1812, 1812, 2935, 1796, 2505, 2602, 1790, 1774, 3065, 2349, 1812, 1976, 1829,
    /* 1649 */ 1761, 2777, 2790, 3144, 3043, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1954,
    /* 1666 */ 2456, 3311, 3326, 3329, 3315, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812,
    /* 1683 */ 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 1812, 3899, 3840, 566, 566, 566,
    /* 1701 */ 566, 566, 566, 566, 566, 566, 566, 0, 52, 0, 566, 0, 0, 53, 0, 0, 0, 0, 0, 82, 108, 108, 0, 82, 595, 0,
    /* 1727 */ 82, 0, 0, 0, 134, 0, 0, 0, 0, 106, 82, 0, 95, 3197, 111, 111, 129, 130, 0, 132, 52112, 0, 0, 0, 68, 68,
    /* 1753 */ 0, 0, 0, 94, 0, 90, 96, 0, 0, 129, 0, 130, 0, 82, 82, 52112, 129, 139, 0, 132, 52112, 0, 0, 0, 1372, 0,
    /* 1779 */ 82, 95, 95, 0, 129, 0, 139, 0, 82, 82, 52112, 0, 82, 595, 0, 82, 2304, 0, 0, 59, 59, 61, 62, 0, 0, 59, 0,
    /* 1806 */ 61, 62, 0, 0, 59, 59, 0, 0, 0, 0, 0, 0, 0, 0, 120, 0, 106, 82, 0, 123, 3197, 111, 111, 129, 130, 0, 82,
    /* 1833 */ 52112, 0, 0, 0, 1372, 1885, 82, 95, 95, 59, 0, 61, 61, 0, 0, 0, 0, 55, 55, 55, 55, 61, 61, 61, 61, 61,
    /* 1859 */ 61, 61, 61, 0, 52, 0, 0, 0, 0, 0, 58, 126, 0, 0, 0, 0, 0, 0, 0, 131, 129, 130, 141, 82, 0, 0, 0, 0, 65,
    /* 1888 */ 65, 65, 65, 0, 129, 0, 130, 0, 82, 82, 151, 5120, 5120, 0, 52, 0, 0, 0, 0, 82, 82, 0, 0, 0, 82, 59, 0,
    /* 1915 */ 62, 62, 0, 0, 0, 0, 82, 82, 0, 158, 62, 62, 62, 62, 62, 62, 62, 62, 0, 52, 0, 0, 0, 0, 98, 98, 0, 0, 0,
    /* 1944 */ 98, 59, 0, 0, 0, 0, 5632, 5632, 0, 52, 0, 0, 0, 0, 4352, 0, 0, 0, 5632, 5632, 5632, 5632, 5632, 5632,
    /* 1968 */ 5632, 5632, 5888, 5888, 0, 52, 0, 0, 82, 0, 95, 0, 3197, 0, 0, 0, 52, 0, 0, 0, 0, 0, 0, 90, 0, 0, 85, 0,
    /* 1996 */ 34816, 0, 0, 90, 0, 0, 9728, 0, 0, 9818, 9824, 0, 0, 10496, 2115, 2115, 0, 0, 0, 5632, 0, 0, 0, 0, 5632,
    /* 2021 */ 2115, 2115, 0, 0, 0, 2115, 2115, 0, 0, 0, 1372, 0, 107, 95, 95, 0, 98, 595, 0, 99, 2304, 0, 0, 59, 59,
    /* 2046 */ 61, 1536, 0, 0, 59, 59, 1536, 62, 0, 0, 59, 59, 61, 62, 0, 7424, 0, 98, 112, 0, 0, 0, 116, 0, 0, 33596,
    /* 2072 */ 33596, 61, 62, 7680, 0, 52, 0, 0, 566, 566, 566, 566, 0, 0, 566, 566, 566, 2115, 2115, 566, 566, 566, 0,
    /* 2095 */ 98, 98, 0, 123, 3197, 112, 112, 138, 130, 0, 98, 52112, 0, 0, 0, 2115, 2115, 0, 0, 58, 0, 138, 0, 130, 0,
    /* 2120 */ 98, 98, 52112, 98, 98, 98, 98, 98, 98, 98, 98, 0, 95, 0, 3197, 0, 0, 0, 2115, 2115, 0, 0, 56, 171, 98,
    /* 2145 */ 98, 173, 98, 0, 0, 0, 2115, 2115, 0, 0, 1352, 6144, 6144, 0, 52, 0, 0, 0, 0, 10240, 0, 0, 0, 0, 6400, 0,
    /* 2171 */ 6400, 6400, 0, 0, 6400, 6400, 0, 6400, 1075, 52, 0, 0, 0, 0, 2115, 2115, 0, 0, 8960, 0, 103, 103, 1372,
    /* 2194 */ 0, 82, 95, 109, 59, 0, 55, 55, 55, 6967, 55, 55, 66, 6967, 2115, 2115, 66, 66, 55, 76, 6988, 76, 6967,
    /* 2217 */ 55, 6988, 6988, 6988, 6988, 6988, 6988, 6988, 6988, 66, 6967, 6988, 6988, 76, 6988, 6988, 0, 0, 0, 0,
    /* 2237 */ 595, 0, 90, 90, 0, 0, 595, 0, 34816, 0, 0, 90, 0, 56, 56, 0, 0, 0, 0, 0, 82, 0, 0, 97, 82, 595, 0, 82,
    /* 2265 */ 2304, 0, 0, 74, 0, 0, 0, 74, 74, 0, 0, 0, 0, 0, 0, 90, 1372, 110, 512, 623, 0, 0, 0, 116, 118, 82, 133,
    /* 2292 */ 0, 122, 3197, 0, 0, 0, 2115, 2115, 0, 62, 0, 0, 139, 0, 130, 0, 82, 82, 52112, 82, 82, 3328, 82, 3584, 0,
    /* 2317 */ 0, 0, 2115, 2115, 0, 71, 0, 0, 82, 595, 0, 82, 2304, 1892, 0, 58, 0, 0, 0, 0, 0, 0, 106, 95, 95, 129,
    /* 2343 */ 130, 0, 82, 52112, 0, 2695, 0, 82, 82, 0, 123, 3197, 111, 111, 3217, 0, 0, 154, 82, 82, 0, 0, 74, 0, 74,
    /* 2368 */ 74, 74, 74, 74, 74, 74, 74, 59, 0, 0, 0, 0, 0, 0, 56, 0, 0, 56, 56, 0, 81, 0, 0, 0, 84, 132, 0, 108, 0,
    /* 2397 */ 3197, 0, 0, 0, 5888, 5888, 5888, 5888, 5888, 59, 0, 0, 0, 63, 0, 0, 0, 6144, 6144, 6144, 6144, 6144, 64,
    /* 2420 */ 63, 0, 2115, 2115, 69, 70, 64, 73, 73, 63, 69, 69, 69, 63, 63, 69, 73, 63, 69, 77, 77, 77, 77, 80, 80,
    /* 2445 */ 80, 80, 80, 80, 0, 52, 0, 0, 0, 0, 34816, 0, 0, 0, 0, 0, 4352, 4352, 0, 7424, 0, 0, 0, 0, 90, 96, 7424,
    /* 2472 */ 8448, 0, 59, 59, 61, 62, 0, 0, 86, 34902, 0, 0, 90, 0, 0, 82, 595, 7168, 82, 2304, 0, 0, 105, 1372, 0,
    /* 2497 */ 82, 95, 95, 129, 140, 0, 82, 52112, 0, 0, 0, 34816, 0, 0, 90, 0, 0, 129, 119, 21132, 0, 82, 82, 52112,
    /* 2521 */ 65, 0, 0, 2115, 2115, 0, 0, 65, 0, 0, 0, 0, 0, 0, 82, 95, 95, 65, 65, 0, 0, 0, 0, 0, 0, 5632, 5632, 65,
    /* 2549 */ 65, 0, 52, 9216, 0, 0, 0, 34816, 0, 0, 90, 1372, 8704, 0, 0, 34816, 0, 0, 90, 0, 82, 82, 0, 124, 3197,
    /* 2574 */ 111, 111, 33596, 53, 0, 0, 0, 0, 0, 8960, 8960, 8960, 8960, 8960, 0, 8960, 0, 0, 0, 0, 0, 0, 90, 90, 0,
    /* 2599 */ 8960, 8960, 0, 0, 0, 0, 0, 0, 90, 96, 0, 89, 0, 0, 0, 7768, 90, 90, 7768, 159, 0, 82, 82, 0, 0, 0, 82,
    /* 2626 */ 82, 0, 7936, 8192, 82, 1075, 52, 0, 0, 1075, 0, 0, 0, 34816, 0, 0, 91, 0, 59, 0, 1075, 0, 0, 0, 0, 0, 91,
    /* 2653 */ 91, 0, 0, 1075, 0, 2115, 2115, 1075, 1075, 0, 82, 82, 122, 116, 3197, 111, 4096, 1075, 1075, 1075, 1075,
    /* 2674 */ 1075, 1075, 1075, 1075, 52, 0, 0, 0, 0, 34816, 88, 89, 90, 0, 129, 130, 0, 82, 52112, 3217, 2695, 0, 82,
    /* 2697 */ 111, 0, 0, 0, 0, 0, 129, 0, 0, 3217, 0, 0, 154, 82, 82, 52125, 0, 0, 82, 0, 127, 0, 0, 0, 0, 0, 120, 82,
    /* 2725 */ 82, 0, 123, 3197, 111, 111, 0, 82, 595, 0, 82, 2304, 1892, 101, 0, 0, 1372, 1885, 82, 95, 95, 82, 0, 95,
    /* 2749 */ 0, 3197, 0, 0, 137, 3217, 152, 0, 154, 82, 82, 52125, 0, 82, 111, 0, 0, 0, 0, 117, 0, 0, 0, 129, 130, 0,
    /* 2775 */ 82, 52112, 3217, 0, 0, 0, 82, 82, 0, 0, 0, 111, 111, 3217, 0, 0, 0, 82, 82, 52125, 0, 0, 82, 147, 129, 0,
    /* 2801 */ 130, 0, 82, 82, 52112, 82, 0, 95, 0, 3197, 0, 136, 0, 82, 111, 0, 0, 0, 116, 119, 129, 130, 0, 82, 52112,
    /* 2826 */ 3217, 0, 146, 3217, 0, 153, 0, 82, 82, 52125, 0, 82, 111, 0, 0, 115, 116, 0, 82, 111, 0, 0, 0, 116, 117,
    /* 2851 */ 0, 82, 595, 0, 82, 2304, 0, 102, 59, 0, 0, 9472, 0, 0, 0, 0, 34903, 0, 0, 90, 1372, 71, 71, 9547, 71, 71,
    /* 2877 */ 71, 9547, 9547, 71, 71, 9547, 71, 9550, 9550, 9551, 9550, 9550, 9550, 9550, 9550, 9550, 9550, 9550, 0,
    /* 2896 */ 52, 0, 0, 0, 0, 128, 0, 0, 0, 0, 0, 99, 95, 95, 58, 58, 0, 52, 0, 0, 0, 0, 160, 82, 52125, 0, 0, 82, 161,
    /* 2925 */ 52125, 0, 0, 82, 0, 9984, 59, 59, 61, 62, 0, 0, 1075, 52, 0, 0, 0, 0, 5120, 5120, 5120, 5120, 5120, 5120,
    /* 2949 */ 5120, 5120, 0, 104, 104, 0, 0, 82, 0, 104, 104, 0, 0, 0, 0, 129, 130, 0, 82, 0, 0, 0, 0, 4864, 0, 0, 0,
    /* 2976 */ 4864, 4864, 0, 52, 0, 0, 0, 0, 57, 0, 0, 129, 0, 130, 0, 82, 82, 0, 116, 0, 111, 111, 0, 104, 82, 82, 0,
    /* 3003 */ 0, 0, 82, 82, 52125, 0, 0, 162, 0, 1352, 0, 0, 0, 0, 0, 0, 130, 0, 0, 1352, 1352, 0, 52, 0, 0, 0, 0,
    /* 3030 */ 4864, 0, 4864, 4864, 4864, 4864, 4864, 4864, 4864, 4864, 82, 82, 165, 82, 82, 82, 82, 82, 0, 0, 0, 129,
    /* 3052 */ 130, 0, 142, 52112, 3217, 2695, 0, 82, 111, 0, 114, 0, 116, 0, 82, 111, 0, 0, 0, 116, 0, 0, 6656, 0,
    /* 3076 */ 6656, 6656, 6656, 6656, 6656, 6656, 6656, 6656, 129, 130, 0, 143, 52112, 3217, 2695, 0, 82, 111, 113, 0,
    /* 3096 */ 0, 116, 0, 56, 0, 0, 0, 0, 0, 0, 95, 0, 0, 129, 0, 130, 0, 149, 82, 52112, 3217, 0, 0, 154, 155, 82,
    /* 3122 */ 52125, 0, 82, 111, 1892, 0, 0, 116, 0, 82, 82, 0, 116, 3197, 111, 111, 163, 164, 82, 82, 167, 168, 82,
    /* 3145 */ 82, 82, 82, 82, 82, 82, 82, 170, 82, 172, 82, 82, 82, 0, 0, 0, 5376, 0, 0, 0, 5376, 5376, 0, 52, 0, 0, 0,
    /* 3172 */ 0, 119, 0, 20992, 0, 3217, 0, 0, 0, 82, 156, 52125, 0, 82, 121, 0, 123, 3197, 111, 111, 82, 82, 82, 166,
    /* 3196 */ 82, 82, 169, 82, 82, 172, 82, 0, 0, 0, 3197, 0, 0, 0, 0, 107, 82, 0, 123, 3197, 111, 111, 0, 93, 0, 0, 0,
    /* 3223 */ 90, 96, 0, 82, 512, 0, 82, 0, 0, 0, 104, 0, 0, 0, 104, 82, 0, 95, 0, 3197, 135, 0, 0, 5376, 0, 5376,
    /* 3249 */ 5376, 5376, 5376, 5376, 5376, 5376, 5376, 0, 129, 0, 130, 148, 82, 150, 52112, 59, 0, 0, 0, 0, 0, 10496,
    /* 3271 */ 0, 10496, 10496, 0, 10496, 10496, 0, 10496, 10496, 10496, 10496, 10496, 10496, 10496, 10496, 10496,
    /* 3287 */ 10496, 0, 52, 0, 0, 0, 0, 6656, 0, 0, 0, 6656, 6656, 1075, 52, 0, 0, 0, 0, 2115, 2115, 0, 61, 0, 4352,
    /* 3312 */ 4352, 0, 4352, 4352, 4352, 0, 0, 0, 0, 0, 0, 1352, 0, 1352, 4352, 4352, 0, 4352, 4352, 4352, 4352, 4352,
    /* 3334 */ 4352, 4352, 4352
  };

  private static final int[] EXPECTED =
  {
    /*   0 */ 11, 19, 27, 35, 43, 51, 59, 67, 74, 74, 74, 127, 82, 86, 90, 94, 97, 101, 105, 109, 113, 117, 121, 125,
    /*  24 */ 135, 259, 139, 143, 162, 147, 151, 155, 159, 170, 174, 181, 197, 188, 177, 196, 185, 190, 195, 199, 194,
    /*  45 */ 198, 206, 203, 207, 205, 211, 218, 218, 218, 214, 232, 252, 165, 222, 231, 236, 166, 240, 225, 227, 244,
    /*  66 */ 247, 253, 250, 129, 257, 129, 130, 131, 129, 129, 129, 129, 129, 129, 129, 129, 10, 134217730, 2, 2,
    /*  86 */ 16388, 16384, 42, 34, 536936450, 69206018, 2097154, 134217730, 268435458, 536903690, 69206018, 960,
    /*  98 */ -1073643510, 1073971242, 402653202, 1342406698, 1342406714, 571736170, -1073500118, -1006391254,
    /* 106 */ -1073500118, 640942186, 573833322, -1006391254, 1476624442, 640942186, -1005866966, -434130838,
    /* 114 */ -501239702, -434129814, -432033686, -415256470, -297815958, -281038742, -402673558, -151015318,
    /* 122 */ -151015302, -134238102, -134238086, -16797574, -20358, 8, 2048, 0, 0, 0, 0, 32, 32, 0, 2, 2, 134217728,
    /* 139 */ 32, 32, 536870912, 268435456, 0, 32770, 256, 960, 1342177280, 64, 0, 45058, -536870912, 131072, 132096,
    /* 154 */ -268435456, 2048, 2, 2, 134217728, 32770, 256, 576, -1073741824, 1073741824, 131072, 16, 0, 0, 256, 48,
    /* 170 */ 1073741824, 131072, 131072, 16, 64, 45058, 1024, 132096, 2048, 32768, 32768, 2048, 2, 32770, 576, 131072,
    /* 186 */ 131072, 131072, 131072, 12290, 12290, 1024, 132096, 132096, 1024, 2048, 512, 1073741824, -2147483648,
    /* 199 */ 131072, 131072, 131072, 12290, 2048, 1073741824, -2147483648, 131072, 8194, 4098, 1024, 1073741824, 1024,
    /* 212 */ 1073741824, -2147483648, 8194, 0, 0, 8, 4098, 8194, 4098, 8194, 4, 2, 0, 256, 48, 304, 304, 304, 304, 0,
    /* 232 */ 128, 0, 0, 1, 70, 2, 2, 48, 304, 256, 70, 304, 308, 310, 310, 310, 374, 374, 0, 1, 64, 0, 8, 0, 0, 0, 2,
    /* 259 */ 0, 0, 4, 4
  };

  private static final String[] TOKEN =
  {
    "(0)",
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
    "'+'",
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
