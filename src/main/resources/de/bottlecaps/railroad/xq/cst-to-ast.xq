(:~
 : The parse tree rewriter. Contains functions for converting
 : the concrete syntax tree into the abstact syntax tree (AST).
 :
 : We are using the XPath grammar namespace here, i.e.
 : "http://www.w3.org/2001/03/XPath/grammar", though actually
 : using a few minor modifications of the original structure.
 :)
module namespace a="de/bottlecaps/railroad/xq/cst-to-ast.xq";

declare namespace g="http://www.w3.org/2001/03/XPath/grammar";

(:~
 : Convert a char code from the EBNF input into the representation
 : for AST use, i.e. strip the leading "#x" and ensure the result
 : has at least 4 hex digits.
 :
 : @param $code the char code as it occurred in the input, with a "#x"
 : prefix.
 : @return the char code string normalized to at least 4 hex digits.
 :)
declare function a:charCode($code as xs:string) as xs:string
{
  concat(substring('00000', string-length($code)), substring($code, 3))
};

(:~
 : Recursively transform a fragment of the concrete parse tree of an
 : EBNF grammar in to an AST node.
 :
 : @param $nodes sequence of nodes of the concrete parse tree,
 : representing the grammar fragment to be transformed.
 : @return the AST fragment for the parse tree fragment, which is
 : a sequence of elements in the XPath grammar namespace, or an
 : xhref attribute node.
 :)
declare function a:ast($nodes as node()*) as node()*
{
  for $node in $nodes
  return
    typeswitch ($node)
    case element(NCName) return
      element g:ref {attribute name {$node}}
    case element(StringLiteral) return
      let $string := $node
      return
        element g:string {substring($string, 2, string-length($string) - 2)}
    case element(NameOrString) return
      let $a := a:ast($node/*[1])
      return
        if (count($node/*) = 2) then
          element {node-name($a)}
          {
            $a/@*,
            attribute context {substring($node/*[2], 2)},
            $a/node()
          }
        else
          $a
    case element(Char) return
      element g:char {string($node)}
    case element(CharRange) return
      element g:charRange
      {
        attribute minChar {substring($node, 1, 1)},
        attribute maxChar {substring($node, 3, 1)}
      }
    case element(CharCode) return
      element g:charCode {attribute value {a:charCode($node)}}
    case element(CharCodeRange) return
      element g:charCodeRange
      {
        attribute minValue {a:charCode(substring-before($node, '-'))},
        attribute maxValue {a:charCode(substring-after($node, '-'))}
      }
    case element(CharClass) return
      let $ast := a:ast($node/*[not(self::TOKEN)])
      return
        if ($node/TOKEN = "[^") then
            element g:complement {element g:charClass{$ast}}
          else
            element g:charClass {$ast}
    case element(ProcessingInstruction) return
      processing-instruction {$node/NCName} {data($node/ProcessingInstructionContents)}
    case element(Link) return
      attribute xhref {$node/URL}
    case element(Primary) return
      if ($node/TOKEN = ".") then
        element g:ref {attribute name {"."}}
      else if ($node/TOKEN = "$") then
        element g:endOfFile {}
      else
        a:ast($node/*[not(self::TOKEN)])
    case element(Item) return
      a:closure(a:ast($node/Primary), $node/TOKEN)
    case element(SequenceOrDifference) return
      if ($node/TOKEN = "-") then
        element g:subtract
        {
          for $i in $node/Item
          let $ast := a:ast($i)
          return if (count($ast) = 1) then $ast else element g:sequence {$ast}
        }
      else
        a:ast($node/Item)
    case element(Choice) return
      if (count($node/SequenceOrDifference) = 1) then
        a:ast($node/SequenceOrDifference)
      else if ($node/TOKEN = "|") then
          element g:choice
          {
            for $s in $node/SequenceOrDifference
            let $ast := a:ast($s)
            return if (count($ast) = 1) then $ast else element g:sequence {$ast}
          }
      else
          element g:orderedChoice
          {
            for $s in $node/SequenceOrDifference
            let $ast := a:ast($s)
            return if (count($ast) = 1) then $ast else element g:sequence {$ast}
          }
    case element(Alternative) return
      if ($node/TOKEN = "&amp;") then
        element g:context
        {
          for $i in $node/*[not(self::TOKEN)]
          let $ast := a:ast($i)
          return if (count($ast) = 1) then $ast else element g:sequence {$ast}
        }
      else
        a:ast($node/SequenceOrDifference)
    case element(Alternatives) return
      if (count($node/Alternative) = 1) then
        a:ast($node/Alternative)
      else if ($node/TOKEN = "|") then
          element g:choice
          {
            for $s in $node/Alternative
            let $ast := a:ast($s)
            return if (count($ast) = 1) then $ast else element g:sequence {$ast}
          }
      else
          element g:orderedChoice
          {
            for $s in $node/Alternative
            let $ast := a:ast($s)
            return if (count($ast) = 1) then $ast else element g:sequence {$ast}
          }
    case element(Preference) return
      let $lhs := $node/NameOrString[1]
      let $lhs-ast := a:ast($lhs)
      for $rhs in $node/NameOrString[. >> $lhs]
      return
        if ($node/TOKEN = "<<") then
          element g:preference {$lhs-ast, a:ast($rhs)}
        else
          element g:preference {a:ast($rhs), $lhs-ast}
    case element(Delimiter) return
      let $lhs-ast := a:ast($node/NCName)
      for $rhs in $node/NameOrString
      return element g:delimiter {$lhs-ast, a:ast($rhs)}
    case element(Equivalence) return
      let $lhs := a:ast($node/EquivalenceCharRange[1]/*[not(self::TOKEN)])
      let $rhs := a:ast($node/EquivalenceCharRange[2]/*[not(self::TOKEN)])
      return element g:equivalence {$lhs, $rhs}
    case element(Production) return
      element g:production
      {
        attribute name {$node/NCName},
        attribute nongreedy {true()}[$node/TOKEN = "?"],
        attribute whitespace-spec {"explicit"  }[contains($node/Option, "explicit"  )],
        attribute whitespace-spec {"definition"}[contains($node/Option, "definition")],
        a:ast($node/(Alternatives | Link))
      }
    case element(EOF) return
      ()
    case element(Grammar) return
      element g:grammar
      {
        let $terminals := $node/TOKEN[. = "<?TOKENS?>"]
        let $encore := $node/TOKEN[. = "<?ENCORE?>"]
        let $syntax-end := ($terminals, $encore)[1]
        return
        (
          a:ast($node/*[not(. is $syntax-end or . >> $syntax-end)]),
          if ($terminals) then
          (
            <?TOKENS?>,
            a:ast($node/*[. >> $terminals and not(. is $encore or . >> $encore)])
          )
          else
            (),
          if ($encore) then
          (
            <?ENCORE?>,
            a:ast($node/ProcessingInstruction[. >> $encore])
          )
          else
            ()
        )
      }
    default return
      error(xs:QName("a:ast"), concat("invalid parse tree node type: ", local-name($node)))
};

(:~
 : Apply closure operators to a given ASR fragment.
 :
 : @param $ast the AST fragment.
 : @param $operators the closure operators.
 : @return the AST fragment for the parse tree fragment, which is
 : a sequence of elements in the XPath grammar namespace, or an
 : xhref attribute node.
 :)
declare function a:closure($ast as element()*, $operators as element(TOKEN)*) as element()*
{
  if (empty($operators)) then
    $ast
  else
    let $operator := $operators[1]
    return
      a:closure
      (
        if ($operator = "?") then
          if (count($ast) != 1) then
            element g:optional {$ast}
          else
            typeswitch ($ast)
            case element(g:optional) return $ast
            case element(g:zeroOrMore) return $ast
            case element(g:oneOrMore) return element g:zeroOrMore{$ast/*}
            default return
              element g:optional {$ast}
        else if ($operator = "+") then
          if (count($ast) != 1) then
            element g:oneOrMore {$ast}
          else
            typeswitch ($ast)
            case element(g:optional) return element g:zeroOrMore{$ast/*}
            case element(g:zeroOrMore) return $ast
            case element(g:oneOrMore) return $ast
            default return
              element g:oneOrMore {$ast}
        else if ($operator = "*") then
          if (count($ast) != 1) then
            element g:zeroOrMore {$ast}
          else
            typeswitch ($ast)
            case element(g:optional) return element g:zeroOrMore{$ast/*}
            case element(g:zeroOrMore) return $ast
            case element(g:oneOrMore) return element g:zeroOrMore{$ast/*}
            default return
              element g:zeroOrMore {$ast}
        else
          error(xs:QName("a:closure"), concat("invalid operator: ", $operator)),
        subsequence($operators, 2)
      )
};
