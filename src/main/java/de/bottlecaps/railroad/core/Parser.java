// This file was generated on Sun Mar 12, 2023 18:19 (UTC+01) by REx v5.57 which is Copyright (c) 1979-2023 by Gunther Rademacher <grd@gmx.net>
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
      lookahead1W(15);              // Whitespace | NCName | DocComment | '<?'
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
      lookahead1W(18);              // Whitespace | NCName | DocComment | EOF | '<?ENCORE?>' | '<?TOKENS?>'
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
        lookahead1W(19);            // Whitespace | NCName | StringLiteral | DocComment | EOF | EquivalenceLookAhead |
                                    // '<?ENCORE?>'
        if (l1 == 16                // EOF
         || l1 == 30)               // '<?ENCORE?>'
        {
          break;
        }
        switch (l1)
        {
        case 3:                     // NCName
          lookahead2W(20);          // Whitespace | Context | '::=' | '<<' | '>>' | '?' | '\\'
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
    lookahead1W(13);                // Whitespace | '::=' | '?'
    if (l1 == 34)                   // '?'
    {
      consume(34);                  // '?'
    }
    lookahead1W(5);                 // Whitespace | '::='
    consume(27);                    // '::='
    lookahead1W(31);                // Whitespace | NCName | StringLiteral | CharCode | UrlIntroducer | WsExplicit |
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
    lookahead1W(24);                // Whitespace | NCName | StringLiteral | WsExplicit | WsDefinition | DocComment |
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
    if (l1 == 26                    // '/'
     || l1 == 40)                   // '|'
    {
      switch (l1)
      {
      case 40:                      // '|'
        for (;;)
        {
          consume(40);              // '|'
          lookahead1W(30);          // Whitespace | NCName | StringLiteral | CharCode | WsExplicit | WsDefinition |
                                    // DocComment | EOF | EquivalenceLookAhead | '$' | '&' | '(' | '.' | '<?' |
                                    // '<?ENCORE?>' | '<?TOKENS?>' | '[' | '[^' | '|'
          whitespace();
          parse_Alternative();
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
          lookahead1W(29);          // Whitespace | NCName | StringLiteral | CharCode | WsExplicit | WsDefinition |
                                    // DocComment | EOF | EquivalenceLookAhead | '$' | '&' | '(' | '.' | '/' | '<?' |
                                    // '<?ENCORE?>' | '<?TOKENS?>' | '[' | '[^'
          whitespace();
          parse_Alternative();
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
    if (l1 == 19)                   // '&'
    {
      consume(19);                  // '&'
      lookahead1W(23);              // Whitespace | NCName | StringLiteral | CharCode | '$' | '(' | '.' | '<?' | '[' |
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
      lookahead2W(40);              // Whitespace | NCName | Context | StringLiteral | CharCode | WsExplicit |
                                    // WsDefinition | DocComment | EOF | EquivalenceLookAhead | '$' | '&' | '(' | ')' |
                                    // '*' | '+' | '-' | '.' | '/' | '::=' | '<<' | '<?' | '<?ENCORE?>' | '<?TOKENS?>' |
                                    // '>>' | '?' | '[' | '[^' | '\\' | '|'
      switch (lk)
      {
      case 259:                     // NCName Context
        lookahead3W(37);            // Whitespace | NCName | StringLiteral | CharCode | WsExplicit | WsDefinition |
                                    // DocComment | EOF | EquivalenceLookAhead | '$' | '&' | '(' | ')' | '*' | '+' |
                                    // '-' | '.' | '/' | '<<' | '<?' | '<?ENCORE?>' | '<?TOKENS?>' | '>>' | '?' | '[' |
                                    // '[^' | '|'
        break;
      case 2179:                    // NCName '?'
        lookahead3W(34);            // Whitespace | NCName | StringLiteral | CharCode | WsExplicit | WsDefinition |
                                    // DocComment | EOF | EquivalenceLookAhead | '$' | '&' | '(' | ')' | '*' | '+' |
                                    // '-' | '.' | '/' | '::=' | '<?' | '<?ENCORE?>' | '<?TOKENS?>' | '?' | '[' | '[^' |
                                    // '|'
        break;
      }
      break;
    case 5:                         // StringLiteral
      lookahead2W(38);              // Whitespace | NCName | Context | StringLiteral | CharCode | WsExplicit |
                                    // WsDefinition | DocComment | EOF | EquivalenceLookAhead | '$' | '&' | '(' | ')' |
                                    // '*' | '+' | '-' | '.' | '/' | '<<' | '<?' | '<?ENCORE?>' | '<?TOKENS?>' | '>>' |
                                    // '?' | '[' | '[^' | '|'
      switch (lk)
      {
      case 261:                     // StringLiteral Context
        lookahead3W(37);            // Whitespace | NCName | StringLiteral | CharCode | WsExplicit | WsDefinition |
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
      switch (l1)
      {
      case 24:                      // '-'
        consume(24);                // '-'
        lookahead1W(23);            // Whitespace | NCName | StringLiteral | CharCode | '$' | '(' | '.' | '<?' | '[' |
                                    // '[^'
        whitespace();
        parse_Item();
        break;
      default:
        for (;;)
        {
          switch (l1)
          {
          case 3:                   // NCName
            lookahead2W(39);        // Whitespace | NCName | Context | StringLiteral | CharCode | WsExplicit |
                                    // WsDefinition | DocComment | EOF | EquivalenceLookAhead | '$' | '&' | '(' | ')' |
                                    // '*' | '+' | '.' | '/' | '::=' | '<<' | '<?' | '<?ENCORE?>' | '<?TOKENS?>' |
                                    // '>>' | '?' | '[' | '[^' | '\\' | '|'
            switch (lk)
            {
            case 259:               // NCName Context
              lookahead3W(35);      // Whitespace | NCName | StringLiteral | CharCode | WsExplicit | WsDefinition |
                                    // DocComment | EOF | EquivalenceLookAhead | '$' | '&' | '(' | ')' | '*' | '+' |
                                    // '.' | '/' | '<<' | '<?' | '<?ENCORE?>' | '<?TOKENS?>' | '>>' | '?' | '[' | '[^' |
                                    // '|'
              break;
            case 2179:              // NCName '?'
              lookahead3W(33);      // Whitespace | NCName | StringLiteral | CharCode | WsExplicit | WsDefinition |
                                    // DocComment | EOF | EquivalenceLookAhead | '$' | '&' | '(' | ')' | '*' | '+' |
                                    // '.' | '/' | '::=' | '<?' | '<?ENCORE?>' | '<?TOKENS?>' | '?' | '[' | '[^' | '|'
              break;
            }
            break;
          case 5:                   // StringLiteral
            lookahead2W(36);        // Whitespace | NCName | Context | StringLiteral | CharCode | WsExplicit |
                                    // WsDefinition | DocComment | EOF | EquivalenceLookAhead | '$' | '&' | '(' | ')' |
                                    // '*' | '+' | '.' | '/' | '<<' | '<?' | '<?ENCORE?>' | '<?TOKENS?>' | '>>' | '?' |
                                    // '[' | '[^' | '|'
            switch (lk)
            {
            case 261:               // StringLiteral Context
              lookahead3W(35);      // Whitespace | NCName | StringLiteral | CharCode | WsExplicit | WsDefinition |
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
    for (;;)
    {
      lookahead1W(32);              // Whitespace | NCName | StringLiteral | CharCode | WsExplicit | WsDefinition |
                                    // DocComment | EOF | EquivalenceLookAhead | '$' | '&' | '(' | ')' | '*' | '+' |
                                    // '-' | '.' | '/' | '<?' | '<?ENCORE?>' | '<?TOKENS?>' | '?' | '[' | '[^' | '|'
      if (l1 != 22                  // '*'
       && l1 != 23                  // '+'
       && l1 != 34)                 // '?'
      {
        break;
      }
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
      lookahead1W(28);              // Whitespace | NCName | StringLiteral | CharCode | '$' | '(' | ')' | '.' | '/' |
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
    if (l1 != 21)                   // ')'
    {
      switch (l1)
      {
      case 40:                      // '|'
        for (;;)
        {
          consume(40);              // '|'
          lookahead1W(26);          // Whitespace | NCName | StringLiteral | CharCode | '$' | '(' | ')' | '.' | '<?' |
                                    // '[' | '[^' | '|'
          whitespace();
          parse_SequenceOrDifference();
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
          lookahead1W(25);          // Whitespace | NCName | StringLiteral | CharCode | '$' | '(' | ')' | '.' | '/' |
                                    // '<?' | '[' | '[^'
          whitespace();
          parse_SequenceOrDifference();
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
      lookahead1W(38);              // Whitespace | NCName | Context | StringLiteral | CharCode | WsExplicit |
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
      lookahead1W(38);              // Whitespace | NCName | Context | StringLiteral | CharCode | WsExplicit |
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
      lookahead1(16);               // CharCode | Char | CharRange | CharCodeRange
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
      lookahead1(17);               // CharCode | Char | CharRange | CharCodeRange | ']'
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
    lookahead1W(14);                // Whitespace | '<<' | '>>'
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
      lookahead1W(19);              // Whitespace | NCName | StringLiteral | DocComment | EOF | EquivalenceLookAhead |
                                    // '<?ENCORE?>'
      switch (l1)
      {
      case 3:                       // NCName
        lookahead2W(27);            // Whitespace | NCName | Context | StringLiteral | DocComment | EOF |
                                    // EquivalenceLookAhead | '::=' | '<<' | '<?ENCORE?>' | '>>' | '?' | '\\'
        switch (lk)
        {
        case 259:                   // NCName Context
          lookahead3W(21);          // Whitespace | NCName | StringLiteral | DocComment | EOF | EquivalenceLookAhead |
                                    // '<<' | '<?ENCORE?>' | '>>'
          break;
        }
        break;
      case 5:                       // StringLiteral
        lookahead2W(22);            // Whitespace | NCName | Context | StringLiteral | DocComment | EOF |
                                    // EquivalenceLookAhead | '<<' | '<?ENCORE?>' | '>>'
        switch (lk)
        {
        case 261:                   // StringLiteral Context
          lookahead3W(21);          // Whitespace | NCName | StringLiteral | DocComment | EOF | EquivalenceLookAhead |
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
      lookahead1W(19);              // Whitespace | NCName | StringLiteral | DocComment | EOF | EquivalenceLookAhead |
                                    // '<?ENCORE?>'
      switch (l1)
      {
      case 3:                       // NCName
        lookahead2W(27);            // Whitespace | NCName | Context | StringLiteral | DocComment | EOF |
                                    // EquivalenceLookAhead | '::=' | '<<' | '<?ENCORE?>' | '>>' | '?' | '\\'
        switch (lk)
        {
        case 259:                   // NCName Context
          lookahead3W(21);          // Whitespace | NCName | StringLiteral | DocComment | EOF | EquivalenceLookAhead |
                                    // '<<' | '<?ENCORE?>' | '>>'
          break;
        }
        break;
      case 5:                       // StringLiteral
        lookahead2W(22);            // Whitespace | NCName | Context | StringLiteral | DocComment | EOF |
                                    // EquivalenceLookAhead | '<<' | '<?ENCORE?>' | '>>'
        switch (lk)
        {
        case 261:                   // StringLiteral Context
          lookahead3W(21);          // Whitespace | NCName | StringLiteral | DocComment | EOF | EquivalenceLookAhead |
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
      lookahead1(16);               // CharCode | Char | CharRange | CharCodeRange
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
      int i0 = (i >> 5) * 164 + s - 1;
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
    /* 29 */ 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41
  };

  private static final int[] TRANSITION =
  {
    /*    0 */ 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413,
    /*   17 */ 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 1758, 1696,
    /*   34 */ 1760, 1762, 1762, 1703, 2410, 2948, 2057, 1721, 2123, 1733, 1744, 1736, 2230, 1770, 2403, 2735, 2737,
    /*   51 */ 3185, 1782, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 1758, 1696, 1760, 1762,
    /*   68 */ 1762, 1703, 2410, 2948, 2057, 1794, 2123, 1733, 1744, 1736, 2230, 1806, 1818, 2735, 2737, 3185, 1782,
    /*   85 */ 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 1758, 1696, 1838, 1762, 1762, 1703,
    /*  102 */ 1825, 2651, 1938, 1846, 2123, 1861, 1890, 1736, 2230, 1770, 2403, 2735, 2737, 3185, 1782, 2413, 2413,
    /*  119 */ 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2658, 2412, 1910, 2413, 2413, 2657, 1825, 2651,
    /*  136 */ 1938, 1846, 1786, 1920, 3241, 1999, 2230, 1934, 2403, 2735, 2737, 3184, 3214, 2413, 2413, 2413, 2413,
    /*  153 */ 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2658, 1946, 1958, 1963, 1963, 1971, 1897, 2651, 1938, 1846,
    /*  170 */ 1786, 1920, 3241, 1999, 2230, 1934, 2403, 2735, 2737, 3184, 3214, 2413, 2413, 2413, 2413, 2413, 2413,
    /*  187 */ 2413, 2413, 2413, 2413, 2413, 2413, 2412, 1980, 1986, 1990, 1998, 1825, 2708, 1950, 1846, 2755, 1920,
    /*  204 */ 3275, 1999, 2195, 2007, 2355, 2735, 2737, 3184, 3214, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413,
    /*  221 */ 2413, 2413, 2413, 2658, 2412, 2018, 2024, 2028, 2036, 1825, 2651, 1938, 1846, 1786, 1920, 3241, 1999,
    /*  238 */ 2230, 1934, 2403, 2735, 2737, 3184, 3214, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413,
    /*  255 */ 2413, 2658, 2412, 1910, 2093, 2047, 2054, 1825, 2651, 1938, 1846, 1786, 1920, 3241, 1999, 2230, 1934,
    /*  272 */ 2403, 2735, 2737, 3184, 3214, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2658,
    /*  289 */ 2065, 2077, 2082, 2082, 2090, 1875, 2651, 1938, 1846, 1786, 1920, 3241, 1999, 2230, 1934, 2403, 2735,
    /*  306 */ 2737, 3184, 3214, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2658, 2412, 2101,
    /*  323 */ 2107, 2111, 2119, 1825, 2651, 1938, 1846, 1786, 1920, 3241, 1999, 2230, 1934, 2403, 2735, 2737, 3184,
    /*  340 */ 3214, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2658, 2412, 1910, 2859, 2131,
    /*  357 */ 2138, 1825, 2651, 1938, 1846, 1786, 1920, 3241, 1999, 2230, 1934, 2403, 2735, 2737, 3184, 3214, 2413,
    /*  374 */ 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2658, 2412, 1910, 2413, 2150, 2157, 1825,
    /*  391 */ 2174, 2720, 2190, 2069, 2203, 2223, 2573, 2334, 2266, 2341, 1880, 1882, 2278, 2286, 2413, 2413, 2413,
    /*  408 */ 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2658, 2412, 1910, 2413, 2302, 2309, 1825, 2651, 1938,
    /*  425 */ 1846, 1786, 1920, 3241, 1999, 2230, 1934, 2403, 2735, 2737, 3184, 3214, 2413, 2413, 2413, 2413, 2413,
    /*  442 */ 2413, 2413, 2413, 2413, 2413, 2413, 2658, 2412, 1910, 2413, 2327, 2376, 1825, 2362, 1938, 1846, 2389,
    /*  459 */ 2422, 3241, 2463, 2230, 1934, 2555, 2735, 1853, 3184, 3214, 2413, 2413, 2413, 2413, 2413, 2413, 2413,
    /*  476 */ 2413, 2413, 2413, 2413, 2658, 2412, 2436, 2442, 2446, 2454, 1825, 2651, 1938, 1846, 2368, 1920, 3241,
    /*  493 */ 1999, 2230, 1934, 2555, 2735, 1853, 3184, 3214, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413,
    /*  510 */ 2413, 2413, 1725, 2471, 2479, 2487, 2508, 2494, 1825, 2516, 2215, 1846, 1774, 2541, 2563, 2142, 2581,
    /*  527 */ 1934, 2596, 2735, 2737, 3184, 2625, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413,
    /*  544 */ 2658, 2412, 1910, 2413, 2413, 2942, 1825, 2010, 1938, 2645, 2500, 2666, 3241, 1912, 2230, 2674, 2555,
    /*  561 */ 2163, 2182, 3184, 3214, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2951, 2688,
    /*  578 */ 2702, 2953, 2244, 2251, 1825, 2651, 1810, 1846, 1798, 1920, 2745, 2912, 2610, 1934, 2403, 2735, 2737,
    /*  595 */ 3184, 3214, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2658, 2763, 2771, 2779,
    /*  612 */ 2784, 2792, 1868, 2680, 3218, 1846, 1786, 1920, 3241, 1999, 2230, 1934, 2403, 2735, 2737, 3184, 3214,
    /*  629 */ 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 1751, 2412, 1910, 2413, 2413, 1926,
    /*  646 */ 1825, 2651, 1938, 2806, 1786, 2820, 3241, 2864, 2230, 2837, 2852, 2735, 2737, 3184, 3214, 2413, 2413,
    /*  663 */ 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2658, 2872, 2883, 2875, 1713, 2889, 2258, 2651,
    /*  680 */ 1938, 1846, 1786, 1920, 3241, 1999, 2230, 1934, 2403, 2735, 3253, 3184, 3214, 2413, 2413, 2413, 2413,
    /*  697 */ 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2714, 2897, 2907, 2899, 2920, 2911, 3004, 2798, 2381, 1846,
    /*  714 */ 2755, 1920, 2997, 1999, 2319, 1934, 2403, 1830, 2737, 3184, 3214, 2413, 2413, 2413, 2413, 2413, 2413,
    /*  731 */ 2413, 2413, 2413, 2413, 2413, 2928, 2936, 2961, 2966, 2966, 2974, 1825, 2039, 1938, 2645, 2500, 2666,
    /*  748 */ 3241, 1912, 2230, 2982, 2555, 2180, 2182, 3184, 3214, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413,
    /*  765 */ 2413, 2413, 2413, 2928, 2936, 2961, 2966, 2966, 2974, 1825, 2039, 1938, 2645, 2500, 2666, 3241, 2990,
    /*  782 */ 2230, 2982, 2555, 2180, 2182, 3184, 3214, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413,
    /*  799 */ 2413, 2928, 2936, 2961, 2966, 2966, 2974, 1825, 2039, 1938, 3019, 2500, 2666, 3241, 1912, 2348, 2982,
    /*  816 */ 2555, 3027, 2182, 3184, 3214, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2928,
    /*  833 */ 2936, 2961, 2966, 2966, 2974, 1825, 2694, 1938, 1846, 2368, 1920, 3241, 1999, 2230, 3035, 2555, 1851,
    /*  850 */ 1853, 3184, 3214, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2928, 2936, 2961,
    /*  867 */ 2966, 2966, 2974, 1825, 2694, 1938, 1846, 2368, 1920, 3241, 2752, 2230, 3035, 2555, 1851, 1853, 3184,
    /*  884 */ 3214, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2928, 2936, 2961, 2966, 2966,
    /*  901 */ 2974, 1825, 2694, 1938, 1846, 2368, 3043, 3241, 1999, 2230, 3051, 2555, 1851, 1853, 3184, 3214, 2413,
    /*  918 */ 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2928, 2936, 2961, 2966, 2966, 2974, 1825,
    /*  935 */ 2694, 1938, 1846, 2368, 3059, 3241, 1999, 2396, 3035, 2555, 1851, 1853, 3184, 3214, 2413, 2413, 2413,
    /*  952 */ 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2928, 2936, 2961, 2966, 2966, 2974, 1825, 2694, 1938,
    /*  969 */ 1846, 2368, 1920, 3241, 1999, 2230, 3067, 2555, 1851, 1853, 3184, 3214, 2413, 2413, 2413, 2413, 2413,
    /*  986 */ 2413, 2413, 2413, 2413, 2413, 2413, 2928, 2936, 2961, 2966, 2966, 2974, 1825, 2694, 1938, 1846, 2368,
    /* 1003 */ 1920, 3241, 1999, 2230, 3035, 2555, 2827, 1853, 3184, 3214, 2413, 2413, 2413, 2413, 2413, 2413, 2413,
    /* 1020 */ 2413, 2413, 2413, 2413, 2928, 2936, 2961, 2966, 2966, 2974, 1825, 2694, 1938, 3075, 2368, 1920, 3241,
    /* 1037 */ 1999, 2230, 3035, 2555, 1851, 1853, 3184, 3214, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413,
    /* 1054 */ 2413, 2413, 2658, 3083, 3091, 3097, 3105, 3112, 1825, 2651, 1938, 1846, 1786, 1920, 3241, 1999, 2230,
    /* 1071 */ 1934, 2403, 2735, 2737, 3184, 3214, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413,
    /* 1088 */ 1972, 2412, 3120, 1708, 2414, 3128, 3136, 2651, 1938, 1846, 1786, 1920, 3241, 1999, 2230, 1934, 2403,
    /* 1105 */ 2735, 2737, 3184, 3214, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2459, 2412,
    /* 1122 */ 2461, 2413, 2413, 2413, 1825, 2948, 2413, 1721, 3144, 3152, 2166, 2726, 2570, 3160, 2237, 2735, 2732,
    /* 1139 */ 3184, 3214, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2658, 2412, 3168, 2314,
    /* 1156 */ 2270, 3174, 1825, 2651, 3011, 1846, 1786, 1920, 3241, 1999, 2230, 1934, 2403, 2735, 2737, 3184, 3214,
    /* 1173 */ 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2928, 2936, 2961, 2966, 2966, 2974,
    /* 1190 */ 1825, 2039, 1938, 2645, 2500, 2666, 3241, 1912, 2230, 2982, 2555, 2180, 2182, 3182, 3214, 2413, 2413,
    /* 1207 */ 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2928, 2936, 2961, 2966, 2966, 2974, 1825, 2039,
    /* 1224 */ 1938, 2645, 2500, 2666, 3241, 1912, 2230, 3193, 2555, 2180, 2182, 3184, 3214, 2413, 2413, 2413, 2413,
    /* 1241 */ 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2928, 2936, 2961, 2966, 2966, 2974, 1825, 2039, 1938, 2645,
    /* 1258 */ 2500, 2666, 3241, 1912, 2230, 3201, 2588, 2180, 2182, 3184, 3214, 2413, 2413, 2413, 2413, 2413, 2413,
    /* 1275 */ 2413, 2413, 2413, 2413, 2413, 2928, 2936, 2961, 2966, 2966, 2974, 1825, 2039, 1938, 2645, 2500, 2666,
    /* 1292 */ 3241, 1912, 2230, 2982, 2555, 2209, 2182, 3184, 3214, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413,
    /* 1309 */ 2413, 2413, 2413, 2928, 2936, 2961, 2966, 2966, 2974, 1825, 2694, 1938, 1846, 2368, 1920, 3241, 1999,
    /* 1326 */ 2230, 3035, 2555, 1851, 2844, 3209, 3214, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413,
    /* 1343 */ 2413, 2928, 2936, 2961, 2966, 2966, 2974, 1825, 2694, 1938, 1846, 2368, 1920, 3241, 1999, 2230, 3035,
    /* 1360 */ 2555, 1851, 3248, 3184, 3214, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2928,
    /* 1377 */ 2936, 2961, 2966, 2966, 2974, 1825, 2694, 1938, 1846, 2368, 1920, 3241, 1999, 2230, 3035, 2555, 1851,
    /* 1394 */ 2829, 3184, 3226, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2928, 2936, 2961,
    /* 1411 */ 2966, 2966, 2974, 1825, 2694, 1938, 1846, 2368, 1920, 3241, 1999, 2230, 3035, 2555, 1851, 1853, 3234,
    /* 1428 */ 3214, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2928, 2936, 2961, 2966, 2966,
    /* 1445 */ 2974, 1825, 2694, 1938, 1846, 2368, 1920, 3241, 1999, 2230, 3035, 2555, 1902, 1853, 3184, 3214, 2413,
    /* 1462 */ 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2928, 2936, 2961, 2966, 2966, 2974, 1825,
    /* 1479 */ 2694, 1938, 1846, 2368, 1920, 3261, 1999, 2230, 3035, 2555, 1851, 1853, 3184, 3214, 2413, 2413, 2413,
    /* 1496 */ 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2928, 2936, 2961, 2966, 2966, 2974, 1825, 2694, 1938,
    /* 1513 */ 1846, 2368, 1920, 3241, 1999, 2230, 3035, 2555, 1851, 1853, 3269, 3214, 2413, 2413, 2413, 2413, 2413,
    /* 1530 */ 2413, 2413, 2413, 2413, 2413, 2413, 2928, 2936, 2961, 2966, 2966, 2974, 1825, 2694, 1938, 1846, 2428,
    /* 1547 */ 1920, 3283, 1999, 2230, 3035, 2555, 1851, 1853, 3184, 3214, 2413, 2413, 2413, 2413, 2413, 2413, 2413,
    /* 1564 */ 2413, 2413, 2413, 2413, 2928, 2936, 2961, 2966, 2966, 2974, 1825, 2812, 3291, 1846, 2603, 1920, 3241,
    /* 1581 */ 1999, 2548, 3035, 2617, 1851, 1853, 3184, 3214, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413,
    /* 1598 */ 2413, 2413, 2658, 2412, 1910, 2631, 2637, 3299, 1825, 2651, 1938, 1846, 1786, 1920, 3241, 1999, 2230,
    /* 1615 */ 1934, 2403, 2735, 2737, 3184, 3214, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413,
    /* 1632 */ 2658, 2412, 1910, 2413, 2413, 2942, 1825, 2651, 1938, 1846, 2368, 1920, 3241, 1999, 2230, 1934, 2555,
    /* 1649 */ 2735, 1853, 3184, 3214, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2290,
    /* 1666 */ 2522, 2528, 2533, 2294, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413,
    /* 1683 */ 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 2413, 3890, 3840, 557, 557, 557,
    /* 1701 */ 557, 557, 557, 0, 43, 0, 557, 0, 0, 0, 49, 0, 0, 0, 0, 56, 56, 56, 56, 56, 73, 586, 0, 73, 0, 0, 0, 0,
    /* 1729 */ 46, 46, 46, 46, 73, 102, 0, 0, 0, 0, 108, 0, 0, 0, 73, 97, 73, 0, 86, 3188, 102, 102, 0, 43, 0, 0, 0, 0,
    /* 1757 */ 48, 0, 43, 0, 0, 557, 557, 557, 557, 557, 557, 557, 557, 121, 0, 123, 52103, 0, 0, 0, 0, 73, 0, 0, 101,
    /* 1782 */ 73, 73, 163, 73, 0, 0, 0, 0, 73, 86, 86, 0, 73, 512, 0, 73, 0, 0, 0, 0, 73, 99, 99, 0, 130, 0, 123,
    /* 1809 */ 52103, 0, 0, 0, 0, 81, 81, 0, 0, 120, 0, 130, 0, 73, 73, 52103, 0, 50, 50, 52, 53, 0, 0, 0, 73, 73, 0,
    /* 1836 */ 149, 150, 2106, 2106, 557, 557, 557, 557, 557, 557, 73, 586, 0, 73, 2304, 0, 0, 0, 73, 73, 52116, 0, 0,
    /* 1859 */ 73, 73, 73, 102, 0, 0, 0, 107, 108, 0, 50, 50, 52, 53, 0, 7424, 0, 50, 50, 52, 1536, 0, 0, 0, 89, 89, 0,
    /* 1886 */ 0, 0, 89, 89, 97, 73, 0, 114, 3188, 102, 102, 0, 50, 50, 1536, 53, 0, 0, 0, 73, 147, 52116, 0, 0, 2106,
    /* 1911 */ 2106, 0, 0, 0, 0, 0, 0, 111, 73, 73, 102, 0, 0, 0, 107, 0, 0, 43, 0, 0, 0, 0, 8448, 121, 0, 73, 52103, 0,
    /* 1939 */ 0, 0, 0, 81, 87, 0, 0, 50, 0, 52, 52, 0, 0, 0, 0, 82, 82, 0, 0, 2106, 2106, 0, 52, 0, 52, 52, 52, 52, 52,
    /* 1968 */ 52, 52, 52, 52, 0, 43, 0, 0, 0, 0, 0, 49, 59, 59, 0, 0, 0, 0, 0, 65, 65, 0, 65, 65, 65, 65, 65, 65, 65,
    /* 1997 */ 65, 65, 0, 0, 0, 0, 0, 0, 0, 73, 121, 132, 73, 0, 0, 0, 0, 0, 81, 0, 0, 2106, 2106, 0, 0, 0, 0, 0, 4864,
    /* 2026 */ 4864, 0, 4864, 4864, 4864, 4864, 4864, 4864, 4864, 4864, 4864, 0, 43, 0, 0, 0, 0, 0, 81, 1363, 0, 5120,
    /* 2048 */ 5120, 5120, 5120, 5120, 5120, 5120, 5120, 0, 43, 0, 0, 0, 0, 0, 86, 0, 0, 50, 0, 53, 53, 0, 0, 0, 0, 90,
    /* 2074 */ 86, 86, 0, 2106, 2106, 0, 53, 0, 53, 53, 53, 53, 53, 53, 53, 53, 53, 0, 43, 0, 0, 0, 0, 0, 5120, 5120,
    /* 2100 */ 5120, 2106, 2106, 0, 0, 0, 0, 0, 5376, 5376, 0, 5376, 5376, 5376, 5376, 5376, 5376, 5376, 5376, 5376, 0,
    /* 2121 */ 43, 0, 0, 0, 0, 0, 97, 86, 86, 0, 5632, 5632, 5632, 5632, 5632, 5632, 5632, 5632, 0, 43, 0, 0, 0, 0, 0,
    /* 2146 */ 121, 0, 0, 73, 5888, 5888, 5888, 5888, 5888, 5888, 5888, 5888, 0, 43, 0, 0, 73, 0, 0, 145, 73, 73, 0, 0,
    /* 2170 */ 0, 102, 102, 0, 76, 0, 34816, 0, 0, 81, 0, 0, 145, 73, 73, 52116, 0, 0, 73, 73, 89, 586, 0, 90, 2304, 0,
    /* 2196 */ 0, 0, 125, 0, 0, 0, 120, 89, 103, 0, 0, 0, 107, 0, 0, 145, 146, 73, 52116, 0, 0, 586, 0, 81, 81, 0, 88,
    /* 2223 */ 89, 89, 0, 114, 3188, 103, 103, 0, 86, 0, 3188, 0, 0, 0, 120, 0, 121, 0, 73, 73, 0, 0, 47, 47, 0, 0, 0,
    /* 2250 */ 0, 47, 0, 72, 0, 0, 0, 75, 0, 50, 0, 52, 53, 0, 0, 8704, 121, 0, 89, 52103, 0, 0, 0, 0, 1343, 0, 1343,
    /* 2277 */ 1343, 89, 89, 89, 89, 89, 89, 89, 162, 89, 89, 164, 89, 0, 0, 0, 0, 4352, 0, 0, 0, 0, 0, 0, 0, 6144,
    /* 2303 */ 6144, 6144, 6144, 6144, 6144, 6144, 6144, 0, 43, 0, 0, 0, 0, 0, 1343, 0, 0, 0, 0, 3188, 0, 0, 0, 120,
    /* 2327 */ 6400, 0, 6400, 0, 0, 6400, 6400, 0, 86, 0, 3188, 0, 0, 0, 129, 0, 121, 0, 89, 89, 52103, 0, 86, 0, 3188,
    /* 2352 */ 0, 0, 128, 120, 0, 121, 0, 73, 73, 142, 0, 77, 34893, 0, 0, 81, 0, 0, 1363, 0, 73, 86, 86, 0, 6400, 1066,
    /* 2378 */ 43, 0, 0, 0, 0, 0, 7759, 81, 81, 7759, 0, 94, 94, 1363, 0, 73, 86, 100, 0, 86, 0, 3188, 0, 127, 0, 120,
    /* 2404 */ 0, 121, 0, 73, 73, 52103, 0, 50, 50, 0, 0, 0, 0, 0, 0, 0, 0, 49, 73, 102, 104, 0, 0, 107, 0, 0, 1363, 0,
    /* 2432 */ 98, 86, 86, 0, 2106, 2106, 0, 0, 0, 0, 0, 6656, 6656, 0, 6656, 6656, 6656, 6656, 6656, 6656, 6656, 6656,
    /* 2454 */ 6656, 1066, 43, 0, 0, 0, 0, 0, 10240, 0, 0, 0, 0, 0, 0, 122, 73, 50, 0, 46, 46, 46, 46, 46, 57, 2106,
    /* 2480 */ 2106, 57, 57, 46, 57, 57, 46, 67, 6958, 46, 57, 6958, 6979, 67, 6979, 0, 0, 0, 0, 586, 0, 0, 1363, 1876,
    /* 2504 */ 73, 86, 86, 0, 6979, 6979, 6979, 6979, 6979, 6979, 6979, 6979, 586, 0, 34816, 0, 0, 81, 0, 0, 4352, 4352,
    /* 2526 */ 0, 4352, 4352, 0, 0, 4352, 0, 4352, 4352, 4352, 4352, 4352, 4352, 4352, 4352, 512, 614, 0, 0, 0, 107,
    /* 2547 */ 109, 0, 86, 0, 3188, 126, 0, 0, 120, 0, 121, 0, 73, 73, 52103, 3208, 73, 73, 113, 107, 3188, 102, 4096,
    /* 2570 */ 0, 95, 95, 0, 0, 0, 0, 120, 0, 0, 89, 124, 0, 113, 3188, 0, 0, 0, 120, 0, 121, 0, 140, 73, 52103, 3208,
    /* 2596 */ 130, 0, 121, 0, 73, 73, 52103, 0, 96, 1363, 0, 73, 86, 86, 0, 99, 0, 3188, 0, 0, 0, 120, 0, 121, 139, 73,
    /* 2622 */ 141, 52103, 3208, 73, 3328, 73, 3584, 0, 0, 0, 0, 10496, 0, 10496, 0, 10496, 10496, 10496, 10496, 10496,
    /* 2642 */ 10496, 10496, 10496, 73, 586, 0, 73, 2304, 1883, 0, 0, 34816, 0, 0, 81, 0, 0, 43, 0, 0, 0, 0, 0, 0, 73,
    /* 2667 */ 102, 1883, 0, 0, 107, 0, 111, 121, 0, 73, 52103, 0, 2686, 0, 0, 34816, 0, 0, 81, 0, 7424, 50, 0, 0, 0, 0,
    /* 2693 */ 47, 0, 0, 34816, 0, 0, 81, 1363, 0, 2106, 2106, 0, 0, 47, 0, 0, 0, 34816, 0, 0, 82, 0, 0, 44, 0, 0, 0, 0,
    /* 2721 */ 0, 85, 0, 81, 87, 0, 0, 95, 0, 0, 0, 95, 73, 73, 0, 0, 0, 73, 73, 0, 0, 0, 73, 73, 73, 73, 0, 115, 3188,
    /* 2750 */ 102, 102, 0, 119, 0, 0, 0, 0, 0, 73, 0, 0, 0, 50, 0, 0, 0, 54, 0, 55, 54, 2106, 2106, 60, 61, 55, 64, 64,
    /* 2778 */ 54, 60, 54, 54, 64, 54, 68, 68, 68, 71, 71, 71, 71, 71, 71, 0, 43, 0, 0, 0, 0, 0, 34816, 79, 80, 81, 0,
    /* 2805 */ 80, 73, 586, 7168, 73, 2304, 0, 0, 0, 34894, 0, 0, 81, 1363, 0, 73, 102, 0, 0, 0, 107, 110, 0, 144, 0,
    /* 2830 */ 73, 73, 52116, 0, 0, 153, 73, 131, 0, 73, 52103, 0, 0, 0, 0, 151, 73, 52116, 0, 0, 73, 154, 120, 110,
    /* 2854 */ 21123, 0, 73, 73, 52103, 0, 5632, 5632, 0, 5632, 0, 0, 0, 110, 0, 20992, 0, 73, 50, 0, 0, 0, 0, 0, 56, 0,
    /* 2880 */ 0, 0, 0, 2106, 2106, 0, 0, 56, 56, 56, 0, 43, 9216, 0, 0, 0, 0, 33587, 44, 0, 0, 0, 8960, 0, 0, 0, 0,
    /* 2907 */ 2106, 2106, 0, 0, 8960, 0, 0, 0, 0, 0, 0, 0, 123, 8960, 8960, 8960, 8960, 8960, 8960, 8960, 8960, 1066,
    /* 2929 */ 43, 0, 0, 1066, 0, 0, 0, 50, 0, 1066, 0, 0, 0, 0, 1066, 43, 0, 0, 0, 0, 0, 34816, 0, 0, 0, 0, 0, 47, 0,
    /* 2958 */ 0, 0, 0, 2106, 2106, 1066, 1066, 0, 1066, 1066, 1066, 1066, 1066, 1066, 1066, 1066, 1066, 1066, 43, 0, 0,
    /* 2979 */ 0, 0, 0, 121, 0, 73, 52103, 3208, 2686, 0, 0, 118, 0, 0, 0, 0, 0, 111, 73, 73, 0, 107, 3188, 102, 102, 0,
    /* 3005 */ 33587, 33587, 52, 53, 7680, 0, 0, 9728, 0, 0, 9809, 9815, 0, 0, 73, 586, 0, 73, 2304, 1883, 92, 92, 143,
    /* 3028 */ 0, 145, 73, 73, 52116, 0, 0, 121, 0, 73, 52103, 3208, 0, 0, 0, 73, 102, 0, 105, 0, 107, 0, 0, 121, 0, 73,
    /* 3054 */ 52103, 3208, 0, 0, 138, 73, 102, 0, 0, 106, 107, 0, 0, 121, 0, 73, 52103, 3208, 0, 137, 0, 73, 586, 0,
    /* 3078 */ 73, 2304, 0, 93, 0, 50, 0, 0, 9472, 0, 0, 0, 0, 2106, 2106, 0, 62, 0, 62, 62, 9538, 9538, 62, 9538, 9541,
    /* 3103 */ 9541, 9542, 9541, 9541, 9541, 9541, 9541, 9541, 9541, 9541, 0, 43, 0, 0, 0, 0, 0, 2106, 2106, 0, 0, 49,
    /* 3125 */ 0, 0, 0, 49, 0, 43, 0, 0, 0, 0, 0, 9984, 50, 50, 52, 53, 0, 0, 0, 95, 95, 0, 0, 73, 0, 95, 0, 73, 102, 0,
    /* 3155 */ 0, 0, 0, 0, 0, 121, 0, 73, 0, 0, 0, 0, 0, 2106, 2106, 0, 0, 1343, 0, 1343, 0, 43, 0, 0, 0, 0, 0, 73, 156,
    /* 3184 */ 73, 73, 73, 73, 73, 73, 73, 73, 160, 121, 0, 133, 52103, 3208, 2686, 0, 0, 121, 0, 134, 52103, 3208,
    /* 3206 */ 2686, 0, 0, 155, 73, 73, 158, 159, 73, 73, 73, 73, 0, 0, 0, 0, 81, 87, 7424, 0, 163, 73, 73, 73, 0, 0, 0,
    /* 3233 */ 0, 73, 73, 73, 73, 73, 73, 161, 73, 73, 0, 114, 3188, 102, 102, 0, 73, 152, 52116, 0, 0, 73, 73, 0, 7936,
    /* 3258 */ 8192, 73, 73, 73, 112, 0, 114, 3188, 102, 102, 0, 73, 73, 157, 73, 73, 160, 73, 73, 0, 107, 0, 102, 102,
    /* 3282 */ 117, 98, 73, 0, 114, 3188, 102, 102, 0, 84, 0, 0, 0, 81, 87, 0, 0, 10496, 0, 43, 0, 0, 0, 0, 0
  };

  private static final int[] EXPECTED =
  {
    /*   0 */ 11, 19, 27, 35, 43, 51, 59, 66, 69, 69, 69, 116, 77, 83, 87, 91, 95, 99, 103, 107, 111, 115, 79, 123, 120,
    /*  25 */ 152, 176, 136, 140, 149, 156, 188, 166, 173, 160, 180, 169, 159, 161, 182, 144, 162, 143, 186, 203, 145,
    /*  46 */ 192, 202, 194, 196, 196, 198, 224, 231, 132, 129, 207, 241, 238, 213, 217, 220, 223, 230, 228, 209, 229,
    /*  67 */ 235, 126, 229, 229, 229, 229, 229, 229, 229, 229, 10, 134217730, 2, 2, 134217728, 0, 16388, 16384, 42, 34,
    /*  87 */ 536936450, 134217730, 268435458, 536903690, 960, 960, -1073643510, 1073971242, 402653202, 1342406698,
    /*  97 */ 1342406714, 571736170, -1073500118, 640942186, 573833322, 1476624442, 640942186, -434130838, -501239702,
    /* 106 */ -434129814, -402673558, -285233046, -268455830, -151015318, -151015302, -134238102, -134238086, -16797574,
    /* 115 */ -20358, 8, 2048, 0, 0, 32, 536870912, 268435456, 0, 4, 4, 32, 32, 0, 0, 4, 2, 0, 8, 0, 16, 64, 0, 45058,
    /* 139 */ -536870912, 131072, 132096, -268435456, 2048, 512, 1073741824, -2147483648, 131072, 8194, 2, 2, 134217728,
    /* 152 */ 32770, 256, 960, -1073741824, 256, 576, -1073741824, 1073741824, -2147483648, 131072, 131072, 131072,
    /* 164 */ 12290, 1024, 45058, 1024, 132096, 2048, 32768, 32768, 512, 2, 32770, 576, 1073741824, 131072, 16,
    /* 179 */ 1342177280, 12290, 12290, 1024, 132096, 132096, 2048, 131072, 131072, 131072, 131072, 16, 64, 4098, 1024,
    /* 194 */ 1073741824, -2147483648, 8194, 4098, 8194, 4098, 8194, 8194, 131072, 8194, 4098, 1024, 2048, 0, 128, 0, 0,
    /* 211 */ 0, 2, 304, 48, 304, 304, 308, 308, 308, 310, 310, 310, 374, 0, 0, 8, 128, 64, 0, 0, 0, 0, 1, 64, 0, 32, 0,
    /* 238 */ 0, 48, 304, 70, 2, 2, 48
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
