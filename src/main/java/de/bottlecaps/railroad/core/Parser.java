// This file was generated on Sat Jan 25, 2025 22:18 (UTC+01) by REx v6.1-SNAPSHOT which is Copyright (c) 1979-2025 by Gunther Rademacher <grd@gmx.net>
// REx command line: -tree -a none -java -basex -name de.bottlecaps.railroad.core.Parser Parser.ebnf

package de.bottlecaps.railroad.core;

import java.io.IOException;
import java.util.Arrays;
import org.basex.build.MemBuilder;
import org.basex.build.SingleParser;
import org.basex.core.MainOptions;
import org.basex.io.IOContent;
import org.basex.query.value.item.Str;
import org.basex.query.value.node.ANode;
import org.basex.query.value.node.DBNode;
import org.basex.util.Atts;
import org.basex.util.Token;

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

  public static ANode parseGrammar(Str str) throws IOException
  {
    BaseXFunction baseXFunction = new BaseXFunction()
    {
      @Override
      public void execute(Parser p) {p.parse_Grammar();}
    };
    return baseXFunction.call(str);
  }

  public static abstract class BaseXFunction
  {
    protected abstract void execute(Parser p);

    public ANode call(Str str) throws IOException
    {
      String input = str.toJava();
      SingleParser singleParser = new SingleParser(new IOContent(""), new MainOptions())
      {
        @Override
        protected void parse() throws IOException {}
      };
      MemBuilder memBuilder = new MemBuilder(input, singleParser);
      memBuilder.init();
      BaseXTreeBuilder treeBuilder = new BaseXTreeBuilder(memBuilder);
      Parser parser = new Parser();
      parser.initialize(input, treeBuilder);
      try
      {
        execute(parser);
      }
      catch (ParseException pe)
      {
        memBuilder = new MemBuilder(input, singleParser);
        memBuilder.init();
        Atts atts = new Atts();
        atts.add(Token.token("b"), Token.token(pe.getBegin() + 1));
        atts.add(Token.token("e"), Token.token(pe.getEnd() + 1));
        if (pe.getOffending() < 0)
        {
          atts.add(Token.token("s"), Token.token(pe.getState()));
        }
        else
        {
          atts.add(Token.token("o"), Token.token(pe.getOffending()));
          atts.add(Token.token("x"), Token.token(pe.getExpected()));
        }
        memBuilder.openElem(Token.token("ERROR"), atts, new Atts());
        memBuilder.text(Token.token(parser.getErrorMessage(pe)));
        memBuilder.closeElem();
      }
      return new DBNode(memBuilder.data());
    }
  }

  public static class BaseXTreeBuilder implements EventHandler
  {
    private CharSequence input;
    private MemBuilder builder;
    private Atts nsp = new Atts();
    private Atts atts = new Atts();

    public BaseXTreeBuilder(MemBuilder b)
    {
      input = null;
      builder = b;
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
        builder.openElem(Token.token(name), atts, nsp);
      }
      catch (IOException e)
      {
        throw new RuntimeException(e);
      }
    }

    @Override
    public void endNonterminal(String name, int end)
    {
      try
      {
        builder.closeElem();
      }
      catch (IOException e)
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
          builder.text(Token.token(input.subSequence(begin, end).toString()));
        }
        catch (IOException e)
        {
          throw new RuntimeException(e);
        }
      }
    }
  }

  public Parser()
  {
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
      lookahead1W(26);              // Whitespace | NCName | StringLiteral | CharCode | '$' | '(' | '.' | '<?' | '[' |
                                    // '[^'
      whitespace();
      parse_Item();
    }
    eventHandler.endNonterminal("Alternative", e0);
  }

  private void parse_CompositeExpression()
  {
    eventHandler.startNonterminal("CompositeExpression", e0);
    switch (l1)
    {
    case 3:                         // NCName
      lookahead2W(49);              // Whitespace | NCName | Context | StringLiteral | CharCode | WsExplicit |
                                    // WsDefinition | DocComment | EOF | EquivalenceLookAhead | '$' | '&' | '(' | ')' |
                                    // '*' | '**' | '+' | '++' | '-' | '.' | '/' | '::=' | '<<' | '<?' | '<?ENCORE?>' |
                                    // '<?TOKENS?>' | '>>' | '?' | '[' | '[^' | '\\' | '|'
      switch (lk)
      {
      case 259:                     // NCName Context
        lookahead3W(47);            // Whitespace | NCName | StringLiteral | CharCode | WsExplicit | WsDefinition |
                                    // DocComment | EOF | EquivalenceLookAhead | '$' | '&' | '(' | ')' | '*' | '**' |
                                    // '+' | '++' | '-' | '.' | '/' | '<<' | '<?' | '<?ENCORE?>' | '<?TOKENS?>' | '>>' |
                                    // '?' | '[' | '[^' | '|'
        break;
      case 2307:                    // NCName '?'
        lookahead3W(42);            // Whitespace | NCName | StringLiteral | CharCode | WsExplicit | WsDefinition |
                                    // DocComment | EOF | EquivalenceLookAhead | '$' | '&' | '(' | ')' | '**' | '++' |
                                    // '-' | '.' | '/' | '::=' | '<?' | '<?ENCORE?>' | '<?TOKENS?>' | '[' | '[^' | '|'
        break;
      }
      break;
    case 5:                         // StringLiteral
      lookahead2W(48);              // Whitespace | NCName | Context | StringLiteral | CharCode | WsExplicit |
                                    // WsDefinition | DocComment | EOF | EquivalenceLookAhead | '$' | '&' | '(' | ')' |
                                    // '*' | '**' | '+' | '++' | '-' | '.' | '/' | '<<' | '<?' | '<?ENCORE?>' |
                                    // '<?TOKENS?>' | '>>' | '?' | '[' | '[^' | '|'
      switch (lk)
      {
      case 261:                     // StringLiteral Context
        lookahead3W(47);            // Whitespace | NCName | StringLiteral | CharCode | WsExplicit | WsDefinition |
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
      lookahead1W(41);              // Whitespace | NCName | StringLiteral | CharCode | WsExplicit | WsDefinition |
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
          lookahead1W(39);          // Whitespace | NCName | StringLiteral | CharCode | WsExplicit | WsDefinition |
                                    // DocComment | EOF | EquivalenceLookAhead | '$' | '&' | '(' | ')' | '.' | '/' |
                                    // '<?' | '<?ENCORE?>' | '<?TOKENS?>' | '[' | '[^' | '|'
          switch (l1)
          {
          case 3:                   // NCName
            lookahead2W(46);        // Whitespace | NCName | Context | StringLiteral | CharCode | WsExplicit |
                                    // WsDefinition | DocComment | EOF | EquivalenceLookAhead | '$' | '&' | '(' | ')' |
                                    // '*' | '+' | '.' | '/' | '::=' | '<<' | '<?' | '<?ENCORE?>' | '<?TOKENS?>' |
                                    // '>>' | '?' | '[' | '[^' | '\\' | '|'
            switch (lk)
            {
            case 259:               // NCName Context
              lookahead3W(43);      // Whitespace | NCName | StringLiteral | CharCode | WsExplicit | WsDefinition |
                                    // DocComment | EOF | EquivalenceLookAhead | '$' | '&' | '(' | ')' | '*' | '+' |
                                    // '.' | '/' | '<<' | '<?' | '<?ENCORE?>' | '<?TOKENS?>' | '>>' | '?' | '[' | '[^' |
                                    // '|'
              break;
            case 2307:              // NCName '?'
              lookahead3W(40);      // Whitespace | NCName | StringLiteral | CharCode | WsExplicit | WsDefinition |
                                    // DocComment | EOF | EquivalenceLookAhead | '$' | '&' | '(' | ')' | '.' | '/' |
                                    // '::=' | '<?' | '<?ENCORE?>' | '<?TOKENS?>' | '[' | '[^' | '|'
              break;
            }
            break;
          case 5:                   // StringLiteral
            lookahead2W(44);        // Whitespace | NCName | Context | StringLiteral | CharCode | WsExplicit |
                                    // WsDefinition | DocComment | EOF | EquivalenceLookAhead | '$' | '&' | '(' | ')' |
                                    // '*' | '+' | '.' | '/' | '<<' | '<?' | '<?ENCORE?>' | '<?TOKENS?>' | '>>' | '?' |
                                    // '[' | '[^' | '|'
            switch (lk)
            {
            case 261:               // StringLiteral Context
              lookahead3W(43);      // Whitespace | NCName | StringLiteral | CharCode | WsExplicit | WsDefinition |
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
    lookahead1W(45);                // Whitespace | NCName | StringLiteral | CharCode | WsExplicit | WsDefinition |
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
      lookahead1W(48);              // Whitespace | NCName | Context | StringLiteral | CharCode | WsExplicit |
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
      lookahead1W(48);              // Whitespace | NCName | Context | StringLiteral | CharCode | WsExplicit |
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
      int i0 = (i >> 5) * 175 + s - 1;
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
    /* 29 */ 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50
  };

  private static final int[] TRANSITION =
  {
    /*    0 */ 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067,
    /*   17 */ 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 1696, 1714,
    /*   34 */ 1701, 1716, 1716, 1716, 1724, 3063, 1730, 2562, 1957, 1790, 1742, 1764, 1759, 2017, 2531, 2063, 2465,
    /*   51 */ 2467, 2515, 2978, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 1696, 1714, 1701, 1716,
    /*   68 */ 1716, 1716, 1724, 3063, 1730, 2562, 1927, 1790, 1742, 1764, 1759, 2017, 2682, 2403, 2465, 2467, 2515,
    /*   85 */ 2978, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 1696, 1714, 1706, 1716, 1716, 1716,
    /*  102 */ 1724, 3095, 3101, 2724, 1975, 1790, 1742, 1772, 1759, 2017, 2531, 2063, 2465, 2467, 2515, 2978, 3067,
    /*  119 */ 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 1749, 3066, 2827, 3067, 3067, 3067, 1747, 3095,
    /*  136 */ 3101, 2724, 1975, 3068, 1742, 1780, 1788, 2017, 2460, 2063, 2465, 2467, 2515, 2516, 3067, 3067, 3067,
    /*  153 */ 3067, 3067, 3067, 3067, 3067, 3067, 3067, 1749, 1798, 3081, 1810, 1810, 1810, 1816, 2024, 3101, 2724,
    /*  170 */ 1975, 3068, 1742, 1780, 1788, 2017, 2460, 2063, 2465, 2467, 2515, 2516, 3067, 3067, 3067, 3067, 3067,
    /*  187 */ 3067, 3067, 3067, 3067, 3067, 3067, 3066, 1917, 2538, 2689, 2693, 2544, 3095, 2909, 2645, 1975, 3068,
    /*  204 */ 1922, 1826, 1840, 2002, 2660, 2063, 2847, 2467, 2515, 2516, 3067, 3067, 3067, 3067, 3067, 3067, 3067,
    /*  221 */ 3067, 3067, 3067, 1749, 3066, 2827, 3253, 1851, 1855, 3259, 3095, 3101, 2724, 1975, 3068, 1742, 1780,
    /*  238 */ 1788, 2017, 2460, 2063, 2465, 2467, 2515, 2516, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067,
    /*  255 */ 3067, 1749, 3066, 2827, 3067, 3327, 3330, 1863, 3095, 3101, 2724, 1975, 3068, 1742, 1780, 1788, 2017,
    /*  272 */ 2460, 2063, 2465, 2467, 2515, 2516, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 1749,
    /*  289 */ 1875, 3145, 1887, 1887, 1887, 1893, 2903, 3101, 2724, 1975, 3068, 1742, 1780, 1788, 2017, 2460, 2063,
    /*  306 */ 2465, 2467, 2515, 2516, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 1749, 3066, 2827,
    /*  323 */ 1906, 1935, 1939, 1912, 3095, 3101, 2724, 1975, 3068, 1742, 1780, 1788, 2017, 2460, 2063, 2465, 2467,
    /*  340 */ 2515, 2516, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 1749, 1947, 1970, 2634, 1965,
    /*  357 */ 1983, 1952, 3095, 3101, 2724, 1975, 3068, 1742, 1780, 1788, 2017, 2460, 2063, 2465, 2467, 2515, 2516,
    /*  374 */ 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 1749, 3066, 2827, 3067, 3067, 1991, 1998,
    /*  391 */ 3095, 2010, 2097, 2125, 1751, 2038, 2051, 2059, 2258, 3174, 2585, 3179, 3181, 2071, 2077, 3067, 3067,
    /*  408 */ 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 1749, 3066, 2827, 3067, 3067, 2085, 2092, 3095, 3101,
    /*  425 */ 2724, 2105, 3068, 1742, 1780, 1788, 2017, 2460, 2063, 2465, 2467, 2515, 2516, 3067, 3067, 3067, 3067,
    /*  442 */ 3067, 3067, 3067, 3067, 3067, 3067, 1749, 3066, 2827, 3067, 3067, 2113, 2120, 3095, 2154, 2724, 1975,
    /*  459 */ 2187, 2133, 1780, 1788, 2147, 2460, 2063, 2510, 2667, 2515, 2516, 3067, 3067, 3067, 3067, 3067, 3067,
    /*  476 */ 3067, 3067, 3067, 3067, 1749, 3066, 2827, 2176, 2195, 2199, 2182, 3095, 3101, 2724, 1975, 2748, 1742,
    /*  493 */ 1780, 1788, 2017, 2460, 2063, 2510, 2667, 2515, 2516, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067,
    /*  510 */ 3067, 3067, 1734, 2207, 2214, 2219, 2234, 2227, 2240, 3095, 2251, 2243, 2168, 3068, 2317, 2273, 2281,
    /*  527 */ 2161, 2460, 2737, 2465, 2467, 2515, 2612, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067,
    /*  544 */ 1749, 3066, 2827, 3067, 3067, 3067, 3220, 3095, 2361, 2724, 1975, 2294, 2289, 2302, 1788, 2310, 2460,
    /*  561 */ 2330, 2930, 2788, 2515, 2516, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 2346, 2344,
    /*  578 */ 2875, 3067, 2359, 2354, 2369, 3095, 3101, 1843, 1975, 3068, 2377, 2390, 1788, 2890, 2460, 2063, 2465,
    /*  595 */ 2467, 2515, 2516, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 1749, 2398, 2411, 2419,
    /*  612 */ 2427, 2432, 2440, 3115, 3101, 2453, 3121, 3068, 1742, 1780, 1788, 2017, 2460, 2063, 2465, 2467, 2515,
    /*  629 */ 2516, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3261, 3066, 2827, 3067, 3067, 3067,
    /*  646 */ 1747, 2476, 3101, 2724, 2043, 3068, 1742, 2490, 2498, 2524, 2763, 2445, 2465, 2467, 2515, 2516, 3067,
    /*  663 */ 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 1749, 3066, 2554, 2570, 2560, 2832, 2580, 2715,
    /*  680 */ 2593, 2724, 1975, 3068, 1742, 1780, 1788, 2017, 2460, 2063, 2465, 2849, 2608, 2516, 3067, 3067, 3067,
    /*  697 */ 3067, 3067, 3067, 3067, 3067, 3067, 3067, 2721, 2620, 3049, 3067, 2632, 2624, 2642, 2653, 2139, 2675,
    /*  714 */ 2701, 3068, 1922, 1832, 1788, 3230, 2460, 2063, 2465, 2709, 2515, 2516, 3067, 3067, 3067, 3067, 3067,
    /*  731 */ 3067, 3067, 3067, 3067, 3067, 2732, 2745, 2756, 2778, 2778, 2778, 2783, 3095, 2572, 2724, 1975, 2294,
    /*  748 */ 2289, 2302, 1788, 2310, 2505, 2330, 2930, 2796, 2515, 2516, 3067, 3067, 3067, 3067, 3067, 3067, 3067,
    /*  765 */ 3067, 3067, 3067, 2732, 2745, 2756, 2778, 2778, 2778, 2783, 3095, 2572, 2724, 1975, 2294, 2289, 2302,
    /*  782 */ 2822, 2310, 2505, 2330, 2930, 2796, 2515, 2516, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067,
    /*  799 */ 3067, 2732, 2745, 2756, 2778, 2778, 2778, 2783, 3095, 2572, 2724, 1975, 2840, 2289, 2302, 1788, 2310,
    /*  816 */ 2857, 2330, 2972, 2796, 2515, 2516, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 2732,
    /*  833 */ 2745, 2756, 2778, 2778, 2778, 2783, 3095, 2030, 2724, 1975, 2748, 1742, 1780, 1788, 2017, 2505, 2063,
    /*  850 */ 2510, 2802, 2515, 2516, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 2732, 2745, 2756,
    /*  867 */ 2778, 2778, 2778, 2783, 3095, 2030, 2724, 1975, 2748, 1742, 1780, 2870, 2017, 2505, 2063, 2510, 2802,
    /*  884 */ 2515, 2516, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 2732, 2745, 2756, 2778, 2778,
    /*  901 */ 2778, 2783, 3095, 2030, 2724, 1975, 2748, 2883, 1780, 1788, 2017, 2505, 3160, 2510, 2802, 2515, 2516,
    /*  918 */ 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 2732, 2745, 2756, 2778, 2778, 2778, 2783,
    /*  935 */ 3095, 2030, 2724, 1975, 2748, 2917, 1780, 1788, 2017, 2925, 2063, 2510, 2802, 2515, 2516, 3067, 3067,
    /*  952 */ 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 2732, 2745, 2756, 2778, 2778, 2778, 2783, 3095, 2030,
    /*  969 */ 2724, 1975, 2748, 1742, 1780, 1788, 2017, 2505, 2938, 2510, 2802, 2515, 2516, 3067, 3067, 3067, 3067,
    /*  986 */ 3067, 3067, 3067, 3067, 3067, 3067, 2732, 2745, 2756, 2778, 2778, 2778, 2783, 3095, 2030, 2724, 1975,
    /* 1003 */ 2748, 1742, 1780, 1788, 2017, 2505, 2063, 2952, 2802, 2515, 2516, 3067, 3067, 3067, 3067, 3067, 3067,
    /* 1020 */ 3067, 3067, 3067, 3067, 2732, 2745, 2756, 2778, 2778, 2778, 2783, 3095, 2030, 2724, 1975, 2965, 1742,
    /* 1037 */ 1780, 1788, 2017, 2505, 2063, 2510, 2802, 2515, 2516, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067,
    /* 1054 */ 3067, 3067, 1749, 2986, 3301, 2999, 3007, 3014, 3020, 3095, 3101, 2724, 1975, 3068, 1742, 1780, 1788,
    /* 1071 */ 2017, 2460, 2063, 2465, 2467, 2515, 2516, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067,
    /* 1088 */ 1818, 3066, 2991, 3067, 3033, 2546, 3044, 3057, 3101, 2724, 1975, 3068, 1742, 1780, 1788, 2017, 2460,
    /* 1105 */ 2063, 2465, 2467, 2515, 2516, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 1898, 3066,
    /* 1122 */ 1897, 3067, 3067, 3067, 3067, 3095, 1730, 3067, 1957, 3225, 3076, 2468, 3089, 3109, 2897, 2063, 2665,
    /* 1139 */ 2322, 2515, 2516, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 1749, 3066, 3025, 3129,
    /* 1156 */ 3129, 1867, 3140, 3095, 3101, 3153, 1975, 3068, 1742, 1780, 1788, 2017, 2460, 2063, 2465, 2467, 2515,
    /* 1173 */ 2516, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 2732, 2745, 2756, 2778, 2778, 2778,
    /* 1190 */ 2783, 3095, 2572, 2724, 1975, 2294, 2289, 2302, 1788, 2310, 2505, 2330, 2930, 2796, 3274, 2516, 3067,
    /* 1207 */ 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 2732, 2745, 2756, 2778, 2778, 2778, 2783, 3095,
    /* 1224 */ 2572, 2724, 1975, 2294, 2289, 2302, 1788, 2310, 2770, 2330, 2930, 2796, 2515, 2516, 3067, 3067, 3067,
    /* 1241 */ 3067, 3067, 3067, 3067, 3067, 3067, 3067, 2732, 2745, 2756, 2778, 2778, 2778, 2783, 3095, 2572, 2724,
    /* 1258 */ 1975, 2294, 2289, 2302, 1788, 2310, 2600, 3189, 2930, 2796, 2515, 2516, 3067, 3067, 3067, 3067, 3067,
    /* 1275 */ 3067, 3067, 3067, 3067, 3067, 2732, 2745, 2756, 2778, 2778, 2778, 2783, 3095, 2572, 2724, 1975, 2294,
    /* 1292 */ 2289, 2302, 1788, 2310, 2505, 2330, 2945, 2796, 2515, 2516, 3067, 3067, 3067, 3067, 3067, 3067, 3067,
    /* 1309 */ 3067, 3067, 3067, 2732, 2745, 2756, 2778, 2778, 2778, 2783, 3095, 2030, 2724, 1975, 2748, 1742, 1780,
    /* 1326 */ 1788, 2017, 2505, 2063, 2510, 2814, 2336, 2516, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067,
    /* 1343 */ 3067, 2732, 2745, 2756, 2778, 2778, 2778, 2783, 3095, 2030, 2724, 1975, 2748, 1742, 1780, 1788, 2017,
    /* 1360 */ 2505, 2063, 2510, 2808, 2515, 2516, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 2732,
    /* 1377 */ 2745, 2756, 2778, 2778, 2778, 2783, 3095, 2030, 2724, 1975, 2748, 1742, 1780, 1788, 2017, 2505, 2063,
    /* 1394 */ 2510, 2802, 3197, 3279, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 2732, 2745, 2756,
    /* 1411 */ 2778, 2778, 2778, 2783, 3095, 2030, 2724, 1975, 2748, 1742, 1780, 1788, 2017, 2505, 2063, 2510, 2802,
    /* 1428 */ 2515, 3167, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 2732, 2745, 2756, 2778, 2778,
    /* 1445 */ 2778, 2783, 3095, 2030, 2724, 1975, 2748, 1742, 1780, 1788, 2017, 2505, 2063, 2862, 2802, 2515, 2516,
    /* 1462 */ 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 2732, 2745, 2756, 2778, 2778, 2778, 2783,
    /* 1479 */ 3095, 2030, 2724, 1975, 2748, 1742, 3205, 1788, 2017, 2505, 2063, 2510, 2802, 2515, 2516, 3067, 3067,
    /* 1496 */ 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 2732, 2745, 2756, 2778, 2778, 2778, 2783, 3095, 2030,
    /* 1513 */ 2724, 1975, 2748, 1742, 1780, 1788, 2017, 2505, 2063, 2510, 2802, 2957, 3213, 3067, 3067, 3067, 3067,
    /* 1530 */ 3067, 3067, 3067, 3067, 3067, 3067, 2732, 2745, 2756, 2778, 2778, 2778, 2783, 3095, 2030, 2724, 1975,
    /* 1547 */ 3036, 1742, 3238, 1788, 2017, 2505, 2063, 2510, 2802, 2515, 2516, 3067, 3067, 3067, 3067, 3067, 3067,
    /* 1564 */ 3067, 3067, 3067, 3067, 2732, 2745, 2756, 2778, 2778, 2778, 2783, 3095, 2482, 3246, 1975, 1802, 1742,
    /* 1581 */ 1780, 1788, 2265, 2505, 2382, 3269, 2802, 2515, 2516, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067,
    /* 1598 */ 3067, 3067, 1749, 3287, 3296, 3288, 3309, 3314, 3322, 3095, 3101, 2724, 1975, 3068, 1742, 1780, 1788,
    /* 1615 */ 2017, 2460, 2063, 2465, 2467, 2515, 2516, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067,
    /* 1632 */ 1749, 3066, 2827, 3067, 3067, 3067, 3220, 3095, 3101, 2724, 1975, 2748, 1742, 1780, 1788, 2017, 2460,
    /* 1649 */ 2063, 2510, 2667, 2515, 2516, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 1879,
    /* 1666 */ 3132, 3338, 3350, 3353, 3342, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067,
    /* 1683 */ 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 3067, 0, 52, 0, 0, 566, 566, 566,
    /* 1703 */ 566, 0, 0, 566, 566, 566, 2115, 2115, 566, 566, 566, 3899, 3840, 566, 566, 566, 566, 566, 566, 566, 566,
    /* 1724 */ 566, 566, 0, 52, 0, 566, 0, 0, 0, 34816, 0, 0, 0, 0, 55, 55, 55, 55, 97, 97, 0, 84, 113, 0, 0, 0, 52, 0,
    /* 1752 */ 0, 0, 0, 0, 0, 0, 101, 113, 113, 0, 0, 0, 0, 119, 0, 108, 84, 0, 97, 3199, 118, 119, 0, 108, 84, 0, 125,
    /* 1779 */ 3199, 118, 0, 0, 84, 84, 0, 125, 3199, 113, 113, 0, 0, 0, 0, 0, 0, 0, 108, 59, 0, 61, 61, 0, 0, 0, 0,
    /* 1806 */ 107, 1374, 0, 84, 61, 61, 61, 61, 61, 61, 61, 61, 0, 52, 0, 0, 0, 0, 0, 58, 118, 0, 0, 84, 84, 0, 118, 0,
    /* 1834 */ 0, 84, 84, 0, 118, 3199, 113, 113, 128, 0, 0, 0, 0, 0, 92, 92, 0, 0, 0, 4864, 0, 4864, 4864, 4864, 4864,
    /* 1859 */ 4864, 4864, 4864, 4864, 5120, 5120, 0, 52, 0, 0, 0, 0, 1352, 0, 1352, 0, 59, 0, 62, 62, 0, 0, 0, 0, 4352,
    /* 1884 */ 0, 0, 0, 62, 62, 62, 62, 62, 62, 62, 62, 0, 52, 0, 0, 0, 0, 10752, 0, 0, 0, 0, 0, 0, 5376, 0, 0, 0, 5376,
    /* 1913 */ 5376, 0, 52, 0, 0, 0, 0, 68, 68, 0, 0, 0, 84, 113, 0, 0, 0, 84, 512, 0, 84, 0, 0, 0, 5376, 0, 5376, 5376,
    /* 1941 */ 5376, 5376, 5376, 5376, 5376, 5376, 59, 0, 0, 0, 0, 5632, 5632, 0, 52, 0, 0, 0, 0, 84, 597, 0, 84, 0, 0,
    /* 1966 */ 0, 5632, 0, 0, 0, 0, 5632, 2115, 2115, 0, 0, 0, 84, 597, 0, 84, 2304, 5632, 5632, 5632, 5632, 5632, 5632,
    /* 1989 */ 5632, 5632, 0, 80, 80, 5888, 5888, 5968, 5888, 5968, 5968, 0, 52, 0, 0, 84, 0, 0, 0, 136, 0, 0, 87, 0,
    /* 2013 */ 34816, 0, 0, 92, 0, 0, 84, 0, 97, 0, 3199, 0, 0, 59, 59, 1536, 62, 0, 0, 0, 34816, 0, 0, 92, 1374, 97,
    /* 2039 */ 97, 0, 100, 114, 0, 0, 0, 84, 597, 7680, 84, 2304, 118, 0, 0, 100, 100, 0, 125, 3199, 114, 114, 0, 0, 0,
    /* 2064 */ 0, 0, 131, 0, 132, 0, 84, 0, 100, 100, 100, 100, 100, 100, 100, 173, 100, 100, 175, 100, 0, 0, 81, 81,
    /* 2088 */ 6400, 6400, 6481, 6400, 6481, 6481, 0, 52, 0, 0, 0, 0, 96, 0, 92, 98, 6144, 6656, 0, 0, 84, 597, 0, 84,
    /* 2112 */ 2304, 0, 6912, 6912, 0, 0, 6912, 0, 6912, 6912, 1075, 52, 0, 0, 0, 0, 100, 597, 0, 101, 2304, 97, 111, 0,
    /* 2136 */ 84, 113, 115, 0, 0, 0, 34816, 90, 91, 92, 0, 0, 133, 84, 0, 97, 0, 3199, 0, 0, 88, 34904, 0, 0, 92, 0, 0,
    /* 2163 */ 84, 135, 0, 124, 3199, 0, 0, 99, 84, 597, 0, 84, 2304, 0, 0, 7168, 0, 0, 0, 7168, 7168, 1075, 52, 0, 0,
    /* 2188 */ 0, 0, 105, 105, 1374, 0, 84, 0, 0, 7168, 0, 7168, 7168, 7168, 7168, 7168, 7168, 7168, 7168, 59, 0, 55,
    /* 2210 */ 55, 55, 7479, 55, 55, 66, 7479, 2115, 2115, 66, 66, 55, 76, 7500, 76, 7479, 55, 7500, 7500, 7500, 7500,
    /* 2231 */ 7500, 7500, 7500, 7500, 66, 7479, 7500, 7500, 76, 7500, 7500, 0, 0, 0, 0, 597, 0, 92, 92, 0, 0, 597, 0,
    /* 2254 */ 34816, 0, 0, 92, 0, 0, 100, 0, 97, 0, 3199, 0, 0, 84, 0, 97, 0, 3199, 137, 118, 120, 0, 84, 84, 124, 118,
    /* 2280 */ 3199, 113, 4096, 0, 0, 0, 0, 0, 132, 97, 97, 0, 84, 113, 1894, 0, 0, 0, 0, 1374, 1887, 84, 118, 0, 122,
    /* 2305 */ 84, 84, 0, 125, 3199, 0, 122, 84, 0, 97, 0, 3199, 0, 0, 112, 512, 625, 0, 0, 0, 106, 84, 84, 0, 0, 2697,
    /* 2331 */ 0, 0, 131, 0, 132, 0, 84, 165, 166, 84, 84, 169, 170, 59, 0, 0, 0, 0, 0, 0, 56, 0, 0, 56, 0, 56, 0, 0, 0,
    /* 2360 */ 56, 0, 0, 0, 0, 0, 0, 92, 0, 0, 56, 0, 83, 0, 0, 0, 86, 110, 110, 0, 84, 113, 0, 0, 0, 131, 0, 132, 150,
    /* 2389 */ 84, 118, 0, 0, 84, 84, 0, 126, 3199, 59, 0, 0, 0, 63, 0, 0, 0, 131, 0, 141, 0, 84, 64, 63, 0, 2115, 2115,
    /* 2416 */ 69, 70, 64, 73, 73, 63, 69, 69, 69, 63, 63, 69, 73, 63, 69, 77, 77, 77, 77, 82, 82, 77, 82, 82, 82, 82,
    /* 2442 */ 0, 52, 0, 0, 0, 0, 131, 121, 21134, 0, 84, 7936, 0, 0, 0, 0, 92, 98, 0, 0, 131, 132, 0, 84, 52114, 0, 0,
    /* 2469 */ 0, 0, 84, 84, 0, 0, 0, 8960, 0, 59, 59, 61, 62, 0, 0, 0, 34905, 0, 0, 92, 1374, 118, 121, 0, 84, 84, 0,
    /* 2496 */ 125, 3199, 113, 113, 0, 0, 0, 0, 121, 0, 0, 131, 132, 0, 84, 52114, 3219, 0, 0, 0, 84, 84, 84, 84, 84,
    /* 2521 */ 84, 84, 0, 20992, 0, 84, 0, 97, 0, 3199, 0, 0, 131, 132, 0, 134, 52114, 0, 0, 74, 0, 0, 0, 74, 74, 0, 0,
    /* 2548 */ 0, 0, 0, 0, 58, 0, 65, 0, 0, 2115, 2115, 0, 0, 65, 0, 0, 0, 0, 0, 0, 97, 0, 65, 65, 0, 0, 0, 0, 0, 0, 92,
    /* 2579 */ 1374, 65, 65, 0, 52, 9728, 0, 0, 0, 140, 0, 132, 0, 100, 9216, 0, 0, 34816, 0, 0, 92, 0, 0, 131, 132, 0,
    /* 2605 */ 145, 52114, 3219, 8704, 84, 84, 84, 84, 84, 84, 84, 3328, 84, 3584, 0, 33596, 53, 0, 0, 0, 0, 0, 9472,
    /* 2628 */ 9472, 9472, 9472, 9472, 0, 9472, 0, 0, 0, 0, 0, 0, 5632, 5632, 9472, 9472, 0, 0, 0, 0, 0, 0, 93, 93, 0,
    /* 2653 */ 0, 0, 33596, 33596, 61, 62, 8192, 0, 0, 131, 132, 143, 84, 0, 0, 0, 0, 0, 84, 84, 52127, 0, 91, 0, 0, 0,
    /* 2679 */ 8282, 92, 92, 0, 0, 131, 141, 0, 134, 52114, 0, 0, 74, 0, 74, 74, 74, 74, 74, 74, 74, 74, 0, 8282, 0, 84,
    /* 2705 */ 597, 0, 84, 2304, 0, 160, 161, 0, 84, 84, 0, 0, 59, 0, 61, 62, 0, 0, 53, 0, 0, 0, 0, 0, 92, 98, 0, 1075,
    /* 2733 */ 52, 0, 0, 1075, 0, 0, 0, 141, 0, 132, 0, 84, 59, 0, 1075, 0, 0, 0, 0, 0, 1374, 0, 84, 0, 1075, 0, 2115,
    /* 2760 */ 2115, 1075, 1075, 0, 0, 131, 142, 0, 84, 52114, 0, 0, 131, 132, 0, 144, 52114, 3219, 1075, 1075, 1075,
    /* 2781 */ 1075, 1075, 1075, 1075, 1075, 52, 0, 0, 0, 0, 156, 84, 84, 52127, 0, 52127, 0, 0, 156, 84, 84, 52127, 0,
    /* 2804 */ 0, 0, 84, 84, 52127, 0, 0, 0, 84, 163, 52127, 0, 0, 0, 162, 84, 52127, 0, 113, 113, 0, 129, 0, 0, 0, 0,
    /* 2830 */ 2115, 2115, 0, 0, 0, 65, 65, 0, 65, 65, 1894, 103, 103, 0, 0, 1374, 1887, 84, 153, 0, 0, 0, 0, 84, 84, 0,
    /* 2856 */ 8448, 0, 139, 131, 132, 0, 84, 52114, 3219, 0, 0, 0, 84, 158, 113, 113, 0, 0, 130, 0, 0, 0, 2115, 2115,
    /* 2880 */ 0, 0, 56, 97, 97, 0, 84, 113, 0, 116, 0, 0, 134, 0, 110, 0, 3199, 0, 0, 131, 132, 0, 84, 0, 0, 59, 59,
    /* 2907 */ 61, 1536, 0, 0, 0, 34816, 0, 0, 93, 0, 97, 97, 0, 84, 113, 0, 0, 117, 138, 0, 131, 132, 0, 84, 52114,
    /* 2932 */ 3219, 0, 0, 156, 84, 84, 0, 148, 0, 131, 0, 132, 0, 84, 52114, 3219, 0, 0, 156, 157, 84, 52114, 3219, 0,
    /* 2956 */ 155, 0, 84, 84, 84, 84, 168, 84, 84, 0, 104, 0, 0, 0, 1374, 0, 84, 52114, 3219, 154, 0, 156, 84, 84, 171,
    /* 2981 */ 84, 84, 174, 84, 0, 59, 0, 0, 9984, 0, 0, 0, 0, 2115, 2115, 0, 0, 58, 71, 71, 10059, 71, 71, 71, 10059,
    /* 3006 */ 10059, 71, 71, 10059, 71, 10062, 10062, 10063, 10062, 10062, 10062, 10062, 10062, 10062, 10062, 10062, 0,
    /* 3023 */ 52, 0, 0, 0, 0, 2115, 2115, 0, 0, 1352, 0, 58, 0, 0, 0, 0, 0, 0, 1374, 0, 109, 0, 58, 0, 52, 0, 0, 0, 0,
    /* 3052 */ 2115, 2115, 0, 0, 9472, 0, 10496, 59, 59, 61, 62, 0, 0, 59, 59, 0, 0, 0, 0, 0, 0, 0, 0, 84, 0, 106, 0,
    /* 3079 */ 84, 113, 0, 0, 0, 2115, 2115, 0, 61, 0, 113, 113, 0, 0, 0, 106, 0, 0, 59, 59, 61, 62, 0, 0, 0, 34816, 0,
    /* 3106 */ 0, 92, 0, 0, 106, 84, 0, 106, 106, 0, 0, 59, 59, 61, 62, 0, 7936, 0, 84, 597, 0, 84, 2304, 0, 1352, 0, 0,
    /* 3133 */ 0, 0, 0, 0, 4352, 4352, 0, 1352, 1352, 0, 52, 0, 0, 0, 0, 2115, 2115, 0, 62, 0, 0, 0, 10240, 0, 0, 10332,
    /* 3159 */ 10338, 0, 0, 149, 131, 0, 132, 0, 84, 172, 84, 84, 84, 84, 84, 0, 0, 140, 132, 0, 100, 52114, 0, 0, 0, 0,
    /* 3185 */ 100, 100, 0, 0, 2697, 0, 0, 131, 0, 132, 0, 151, 0, 164, 84, 84, 84, 84, 84, 84, 118, 0, 0, 84, 123, 0,
    /* 3211 */ 125, 3199, 171, 84, 84, 84, 84, 84, 84, 0, 0, 1075, 52, 0, 0, 0, 0, 106, 106, 0, 0, 84, 0, 0, 0, 3199, 0,
    /* 3238 */ 118, 0, 0, 109, 84, 0, 125, 3199, 0, 95, 0, 0, 0, 92, 98, 0, 0, 4864, 0, 0, 0, 4864, 4864, 0, 52, 0, 0,
    /* 3265 */ 0, 0, 57, 0, 152, 52114, 3219, 0, 0, 0, 84, 84, 84, 167, 84, 84, 84, 174, 84, 84, 84, 0, 59, 0, 0, 0, 0,
    /* 3292 */ 0, 11008, 0, 11008, 0, 0, 11008, 2115, 2115, 0, 0, 0, 2115, 2115, 0, 71, 0, 11008, 0, 11008, 11008, 0,
    /* 3314 */ 11008, 11008, 11008, 11008, 11008, 11008, 11008, 11008, 11008, 11008, 0, 52, 0, 0, 0, 0, 5120, 5120,
    /* 3332 */ 5120, 5120, 5120, 5120, 5120, 5120, 4352, 4352, 0, 4352, 4352, 4352, 0, 0, 0, 0, 0, 0, 4352, 4352, 0,
    /* 3353 */ 4352, 4352, 4352, 4352, 4352, 4352, 4352, 4352
  };

  private static final int[] EXPECTED =
  {
    /*   0 */ 133, 88, 92, 96, 100, 103, 107, 111, 115, 119, 123, 127, 131, 195, 249, 139, 146, 164, 142, 150, 154, 197,
    /*  22 */ 163, 158, 175, 162, 168, 174, 179, 183, 169, 188, 182, 170, 181, 184, 192, 201, 205, 203, 209, 211, 211,
    /*  43 */ 213, 273, 220, 217, 233, 237, 241, 244, 253, 257, 261, 263, 267, 271, 248, 222, 135, 229, 284, 280, 277,
    /*  64 */ 247, 223, 283, 223, 223, 226, 223, 223, 225, 223, 223, 224, 228, 223, 227, 223, 225, 223, 227, 225, 223,
    /*  85 */ 223, 223, 223, 10, 536870914, 2, 2, 16388, 16384, 42, 34, -2147418110, 270532610, 2097154, 536870914,
    /* 100 */ 1073741826, -2147450870, 270532610, 960, 98314, 229418, 1610612754, 1073971242, 1073971258, -2011955094,
    /* 110 */ 241706, 268677162, 241706, -1741422486, -2009857942, 268677162, 1610842170, -1741422486, 269201450,
    /* 119 */ -1742753686, -2011189142, -1742752662, -1740656534, -1203785622, -1631604630, -1094733718, -645943190,
    /* 127 */ -645943174, -1610633110, -109072262, -536891286, -536891270, -20358, 8, 2048, 0, 0, 0, 8, 32, 32,
    /* 141 */ -2147483648, 1073741824, 64, 0, 45058, 0, 32770, 256, 960, -2147483648, 131072, 132096, 8388608, 33554432,
    /* 155 */ -1073741824, 2048, 2, 131072, 16, 64, 45058, 32770, 576, 0, 0, 131072, 16, 131072, 131072, 131072, 12290,
    /* 172 */ 1024, 2048, 12290, 1024, 132096, 2048, 2, 32768, 32768, 512, 0, 0, 131072, 131072, 131072, 8194, 132096,
    /* 189 */ 132096, 2048, 512, 4098, 1024, 2048, 0, 2, 2, 536870912, 32770, 256, 0, 131072, 8194, 4098, 1024, 0, 0,
    /* 208 */ 131072, 0, 8194, 4098, 8194, 4098, 8194, 8194, 0, 32, 0, 64, 0, 4, 256, 0, 0, 0, 0, 1, 2, 0, 0, 0, 3, 0,
    /* 234 */ 1024, 16, 8, 0, 1024, 0, 512, 3, 1, 280, 9, 192, 3, 3, 0, 0, 0, 4, 4, 1027, 192, 1216, 1027, 281, 1216,
    /* 259 */ 1027, 195, 1219, 1219, 1219, 1219, 1243, 1243, 1235, 1499, 1243, 1243, 1499, 0, 0, 32, 512, 0, 128, 128,
    /* 279 */ 0, 0, 128, 0, 3, 1, 0, 0, 1
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
