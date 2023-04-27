(:~
 : The AST to EBNF conversion module.
 :)
module namespace b="de/bottlecaps/railroad/xq/ast-to-ebnf.xq";

declare namespace g="http://www.w3.org/2001/03/XPath/grammar";

(:~
 : The indentation in front of continuation line content.
 :)
declare variable $b:t1 := 12;
declare variable $b:t2 := 40;

(:~
 : Render a g:string, selecting an appropriate quote character.
 :
 : @param $string the g:string element to be rendered.
 : @return the rendered result.
 :)
declare function b:render-string($string as element(g:string)) as item()+
{
  let $context := ("^", data($string/@context))[$string/@context]
  let $quote := if (contains($string, "'")) then """" else "'"
  return
    if ($string/@underline) then
      element fragment
      {
        $quote,
        element u {substring($string, 1, $string/@underline)},
        string-join
        (
          (
            substring($string, $string/@underline + 1),
            $quote,
            $context
          ),
          ""
        )
      }
    else
      string-join
      (
        (
          $quote,
          string($string),
          $quote,
          $context
        ),
        ""
      )
};

(:~
 : Render end-of-file.
 :
 : @param $item the end-of-file item.
 : @return the rendered result.
 :)
declare function b:render-endOfFile($item as element()) as xs:string
{
  "$"
};

(:~
 : Replace indentation of a block of text by a fixed number of spaces.
 :
 : @param $text
 : @param $indent
 :)
declare function b:re-indent($text as xs:string?, $indent as xs:integer) as xs:string?
{
  let $trimmed := replace(replace($text, "(^\s+)|(\s+$)", ""), "&#xD;&#xA;", "&#xA;")
  let $lines := tokenize($trimmed, "&#10;")
  where exists($lines)
  return
    let $min-indent :=
      min
      (
        for $line in $lines[position() > 1]
        where normalize-space($line) != ""
        return string-length($line) - string-length(replace($line, "^ +", ""))
      )
    let $old-indent := string-join(("^", for $x in 1 to $min-indent return " "), "")
    let $new-indent := string-join(for $x in 1 to $indent return " ", "")
    return
      string-join
      (
        (
          $lines[1],
          for $line in $lines[position() > 1]
          return
          (
            if (normalize-space($line) = "") then
              ""
            else if ($old-indent = "^") then
              concat($new-indent, $line)
            else
              replace($line, $old-indent, $new-indent)
          )
        ),
        "&#10;"
      )
};

(:~
 : Render the name of a given item (g:ref or g:production).
 :
 : @param $item the item whose name should be rendered.
 : @return the rendered result.
 :)
declare function b:render-name($item as element()) as element(name)
{
  element name
  {
    replace(string-join((data($item/@name), data($item/@context)), "^"), " ", "_")
  }
};

(:~
 : Render a g:choice or g:orderedChoice element. Decide whether
 : parentheses are required. Render alternatives and separate
 : with operator.
 :
 : @param $choice the g:choice to be rendered.
 : @param $operator the actual choice operator.
 : @return the rendered result.
 :)
declare function b:render-choice($choice as element(), $operator as xs:string)
{
  let $embedded :=
    exists($choice/parent::*[self::g:optional or
                             self::g:oneOrMore or
                             self::g:zeroOrMore or
                             self::g:subtract or
                             count(b:subitems(.)) != 1])
  return
  (
    "("[$embedded],
    for $e at $i in b:subitems($choice)
    return
    (
      $operator[$i != 1],
      b:render-items($e)
    ),
    ")"[$embedded]
  )
};

(:~
 : Calculate subitems of an item, i.e. child nodes and processing instructions.
 :
 : @param $node the parent item.
 : @return the sequence of subitems.
 :)
declare function b:subitems($node as element()) as node()*
{
  for $child in $node/node()
  where $child/self::element() or $child/self::processing-instruction()
  return $child
};

(:~
 : Render a compound construct, along with a given occurrence
 : indicator. Decide whether parentheses are required and render
 : subsequence.
 :
 : @param $compound the compound item to be rendered.
 : @param $occurrence-indicator the occurrence indicator.
 : @param $original the original node, necessary for parenthesis decision.
 : @return the rendered result.
 :)
declare function b:render-compound($compound as element(),
                                   $occurrence-indicator as xs:string?,
                                   $original as element())
{
  let $items := b:subitems($compound)
  let $need-parenthesis := count($items) != 1
                       and ($occurrence-indicator
                         or $original/parent::g:subtract
                         or $original is $original/parent::g:context/*[2])
  let $rendered :=
  (
    text{"("}[$need-parenthesis],
    b:render-items($items),
    text{")"}[$need-parenthesis]
  )
  return
    if (empty($occurrence-indicator)) then
      $rendered
    else
      let $last :=
      (
        for $x at $i in $rendered
        where not($x instance of element(tab))
        return $i
      )[last()]
      return
      (
        $rendered[position() < $last],
        element fragment {$rendered[$last], text{$occurrence-indicator}},
        $rendered[position() > $last]
      )
};

(:~
 : Render a oneOrMoreWithSeparator operator. This operator is used only
 : in a normalized expression, so we only ever need this function when
 : rendering intermediate results.
 :
 : @param $oneOrMoreWithSeparator the oneOrMoreWithSeparator operator node.
 : @return the rendered result.
 :)
declare function b:render-oneOrMoreWithSeparator($oneOrMoreWithSeparator as element(g:oneOrMoreWithSeparator))
{
  "[",
  let $s := element g:sequence {b:subitems($oneOrMoreWithSeparator)[1]}
  return b:render-compound($s, (), $s),
  ";",
  let $s := element g:sequence {b:subitems($oneOrMoreWithSeparator)[2]}
  return b:render-compound($s, (), $s),
  "]+"
(:
  let $body := $oneOrMoreWithSeparator/*[1]
  let $body :=
    if ($body/self::g:sequence) then
      b:subitems($body)
    else
      $body
  let $delimiter := $oneOrMoreWithSeparator/*[2]
  let $delimiter :=
    if ($delimiter/self::g:sequence) then
      b:subitems($delimiter)
    else
      $delimiter
  return
    if (empty($delimiter)) then
      b:render-compound
      (
        element g:oneOrMore {$body},
        "+",
        $oneOrMoreWithSeparator
      )
    else
      b:render-compound
      (
        element g:sequence
        {
          (
            $body,
            element g:zeroOrMore
            {
              $delimiter,
              $body
            }
          )
        },
        (),
        $oneOrMoreWithSeparator
      )
:)
};

(:~
 : Render a subtract operator. Decide whether parentheses are required and render
 : subsequence, separated by "-" characters.
 :
 : @param $subtract the subtract operator to be rendered.
 : @return the rendered result.
 :)
declare function b:render-subtract($subtract as element(g:subtract))
{
  let $items := $subtract/*
  let $parent := $subtract/parent::element()

  let $embedded := $parent/self::g:optional
                or $parent/self::g:oneOrMore
                or $parent/self::g:zeroOrMore
                or $parent/self::g:subtract
                or $parent[not(self::g:choice) and count((element(), processing-instruction())) != 1]
  return
  (
    "("[$embedded],
    for $item at $i in $items
    return
    (
      "-"[$i != 1],
      b:render-items($item)
    ),
    ")"[$embedded]
  )
};

(:~
 : Render a context operator. Render operands, then wrap rhs operand
 : in a predicate item, i.e. precede it by "&amp;". b:render-compound
 : will make sure that we have parentheses if needed.
 :
 : @param $context the context operator to be rendered.
 : @return the rendered result.
 :)
declare function b:render-context($context as element(g:context))
{
  let $lhs := b:render-items($context/*[1])
  let $rhs := b:render-items($context/*[2])
  let $first :=
    (for $x at $i in $rhs where not($x instance of element(tab)) return $i)[1]
  return
  (
    $lhs,
    $rhs[position() < $first],
    element fragment {text{"&amp;"}, $rhs[$first]},
    $rhs[position() > $first]
  )
};

(:~
 : Render a charCode. Add a space if we are not in a charclass. Also
 : add the "#x" prefix.
 :
 : @param $charCode the charCode to be rendered.
 : @return the rendered result.
 :)
declare function b:render-charCode($charCode as node()) as xs:string
{
  concat("#x", $charCode)
};

(:~
 : Render a charCodeRange. Add "#x" prefixes and "-" character.
 :
 : @param $charCodeRange the charCodeRange to be rendered.
 : @return the rendered result.
 :)
declare function b:render-charCodeRange($charCodeRange as element(g:charCodeRange)) as xs:string
{
  concat
  (
    b:render-charCode($charCodeRange/@minValue),
    "-",
    b:render-charCode($charCodeRange/@maxValue)
  )
};

(:~
 : Render a char. Return char values, surrounded by quotes.
 :
 : @param $char the char to be rendered.
 : @return the rendered result.
 :)
declare function b:render-char($char as element(g:char)) as xs:string
{
  let $quote := if ($char = "'") then '"' else "'"
  return concat($quote, $char, $quote)
};

(:~
 : Render a charRange. Return char values, separated by "-" character.
 :
 : @param $charRange the charRange to be rendered.
 : @return the rendered result.
 :)
declare function b:render-charRange($charRange as element(g:charRange)) as xs:string
{
  concat
  (
    $charRange/@minChar,
    "-",
    $charRange/@maxChar
  )
};

(:~
 : Render a charClass. Render items, surrounded by brackets.
 :
 : @param $charClass the charClass to be rendered.
 : @return the rendered result.
 :)
declare function b:render-charClass($charClass as element(g:charClass)) as xs:string
{
  if (exists($charClass/g:char)
  and empty($charClass/*[empty(self::g:char)])
  and contains("0123456789", string-join($charClass/g:char, ""))) then
    concat("[", $charClass/g:char[1], "-", $charClass/g:char[last()], "]")
  else
    let $chars :=
      string-join
      (
        let $rendered :=
          for $c in $charClass/*
          return if ($c/self::g:char) then string($c) else b:render-items($c)
        for $item at $i in $rendered
        let $item :=
          if ($i != 1 and $item = "-") then
            "#x2D"
          else if ($i != 1 and string-length($item) = 3 and starts-with($item, "--")) then
            concat("#x2D-#x", b:to-string(16, string-to-codepoints(substring($item, 3))))
          else if ($item = "]") then
            "#x5D"
          else if (string-length($item) = 3 and starts-with($item, "]-")) then
            concat("#x5D-#x", b:to-string(16, string-to-codepoints(substring($item, 3))))
          else if ($item = "#") then
            "#x23"
          else if (string-length($item) = 3 and starts-with($item, "#-")) then
            concat("#x23-#x", b:to-string(16, string-to-codepoints(substring($item, 3))))
          else
            $item
        order by starts-with($item, "#")
        return $item,
        ""
      )
    return concat("[", replace($chars, "^\^", "#x5E"), "]")
};

(:~
 : Convert an integer value to string representation. This is a
 : tail-recursive helper for the 2-argument function of the same
 : name.
 :
 : @param $base the numeral system base: 2, 8, 10, or 16.
 : @param $todo the uncoverted integer.
 : @param $done the converted string as accumulated in previous
 : recursion levels.
 : @return the string representation.
 :)
declare function b:to-string($base as xs:integer, $todo as xs:integer, $done as xs:string?) as xs:string
{
  if ($todo eq 0) then
    ($done, "0")[1]
  else
    let $done := concat(substring("0123456789ABCDEF", 1 + $todo mod $base, 1), $done)
    return b:to-string($base, $todo idiv $base, $done)
};

(:~
 : Convert an integer value to string representation.
 :
 : @param $base the numeral system base: 2, 8, 10, or 16.
 : @param $todo the uncoverted integer.
 : @return the string representation.
 :)
declare function b:to-string($base as xs:integer, $todo as xs:integer) as xs:string
{
  b:to-string($base, $todo, ())
};

(:~
 : Render a complement charClass. Render items, surrounded by brackets,
 : and preceded by negation indicator.
 :
 : @param $complement the complement to be rendered.
 : @return the rendered result.
 :)
declare function b:render-complement($complement as element(g:complement)) as xs:string
{
  if (empty($complement/*)) then
    "."
  else if ($complement/g:charClass and count($complement/*) eq 1) then
    if (empty($complement/g:charClass/*)) then
      "."
    else
      concat("[^", substring(b:render-charClass($complement/g:charClass), 2))
  else if (($complement/g:string or $complement/g:char) and count($complement/*) eq 1) then
    concat("[^", $complement/*, "]")
  else
    error(xs:QName("b:render-complement"), concat("invalid node type: ", $complement/*[1]/local-name()))
};

(:~
 : Render a preference. Render child items, separated by "&lt;&lt;".
 :
 : @param $preference the preference to be rendered.
 : @return the rendered result.
 :)
declare function b:render-preference($preference as element(g:preference))
{
  let $lhs := $preference/*[1]
  let $all-with-lhs := $preference/../g:preference[deep-equal(*[1], $lhs)]
  where $preference is $all-with-lhs[1]
  return
  (
    <tab col="1"/>,
    b:render-items($lhs),
    <tab col="{$b:t1 - 2}"/>,
    "<<",
    b:render-items($all-with-lhs/*[2])
  )
};

(:~
 : Render a delimiter. Render child items, separated by "\\".
 :
 : @param $delimiter the delimiter to be rendered.
 : @return the rendered result.
 :)
declare function b:render-delimiter($delimiter as element(g:delimiter))
{
  let $lhs := $delimiter/*[1]
  let $all-with-lhs := $delimiter/../g:delimiter[deep-equal(*[1], $lhs)]
  where $delimiter is $all-with-lhs[1]
  return
  (
    <tab col="1"/>,
    b:render-items($lhs),
    <tab col="{$b:t1 - 2}"/>,
    "\\",
    b:render-items($all-with-lhs/*[2])
  )
};

(:~
 : Render a range equivalence declaration. Render child items, enclosed in "[" "]",
 : separated by "==".
 :
 : @param $equivalence the equivalence declaration to be rendered.
 : @return the rendered result.
 :)
declare function b:render-equivalence($equivalence as element(g:equivalence))
{
  let $lhs := $equivalence/*[1]
  let $rhs := $equivalence/*[2]
  return
  (
    <tab col="1"/>,
    concat("["[not($lhs/self::g:string)], b:render-items($lhs), "]"[not($lhs/self::g:string)]),
    <tab col="{$b:t1 - 2}"/>,
    "==",
    concat("["[not($rhs/self::g:string)], b:render-items($rhs), "]"[not($rhs/self::g:string)])
  )
};

(:~
 : Render a production. Handle name, add "::=", and render right hand
 : side items.
 :
 : @param $production the production to be rendered.
 : @return the rendered result.
 :)
declare function b:render-production($production as element(g:production))
{
  <tab col="1"/>,
  element fragment
  {
    b:render-name($production),
    "?"[$production/@nongreedy="true"]
  },
  <tab col="{$b:t1 - 3}"/>,
  "::=",
  if ($production/@xhref) then
    b:render-items($production/@xhref)
  else
    let $items := b:subitems($production)
    return
      if (count($items) != 1 or empty(($items/self::g:choice, $items/self::g:orderedChoice))) then
        b:render-items($items)
      else
        for $item at $i in b:subitems($items)
        return
        (
          (
            <tab col="{$b:t1 - 1}"/>,
            if ($items/self::g:choice) then "|" else "/"
          )
          [$i > 1],
          b:render-items($item)
        ),
  (
    <tab col="{$b:t1 - 2}"/>,
    concat("/* ws: ", data($production/@whitespace-spec), " */")
  )
  [$production/@whitespace-spec]
};

(:~
 : Render a sequence of items. For each item, dispatch to
 : appropriate handler based in item type.
 :
 : @param $items the sequence of items to be rendered.
 : @return the rendered result.
 :)
declare function b:render-items($nodes as node()*)
{
  for $node in $nodes
  return
    typeswitch ($node)
    case document-node() return b:render-items($node/*)
    case element(g:string) return b:render-string($node)
    case element(g:ref) return b:render-name($node)
    case element(g:choice) return b:render-choice($node, "|")
    case element(g:orderedChoice) return b:render-choice($node, "/")
    case element(g:sequence) return b:render-compound($node, (), $node)
    case element(g:optional) return b:render-compound($node, "?", $node)
    case element(g:oneOrMore) return b:render-compound($node, "+", $node)
    case element(g:oneOrMoreWithSeparator) return b:render-oneOrMoreWithSeparator($node)
    case element(g:zeroOrMore) return b:render-compound($node, "*", $node)
    case element(g:subtract) return b:render-subtract($node)
    case element(g:context) return b:render-context($node)
    case element(g:charClass) return b:render-charClass($node)
    case element(g:complement) return b:render-complement($node)
    case element(g:charRange) return b:render-charRange($node)
    case element(g:char) return b:render-char($node)
    case element(g:charCode) return b:render-charCode($node/@value)
    case element(g:charCodeRange) return b:render-charCodeRange($node)
    case element(g:production) return b:render-production($node)
    case element(g:grammar) return b:render-items(b:subitems($node))
    case element(g:endOfFile) return b:render-endOfFile($node)
    case element(g:preference) return b:render-preference($node)
    case element(g:delimiter) return b:render-delimiter($node)
    case element(g:equivalence) return b:render-equivalence($node)
    case processing-instruction() return $node
    case attribute(xhref) return <fragment>[<link>{data($node)}</link>]</fragment>
    default return
      error(xs:QName("b:render-items"), concat("invalid node type: ", local-name($node), "(", string($node), ")"))
};

(:~
 : Render a sequence of nodes, returning a single string. Recursively pass
 : grammar fragments from the todo list to the done list.
 :
 : @param $done the sequence of lines (strings) already rendered.
 : @param $todo the sequence of nodes to be rendered.
 : @return the rendered result.
 :)
declare function b:break-lines($done-earlier, $done-last, $todo)
{
  if (empty($todo)) then
    element ebnf
    {
      for $d at $i in ($done-earlier, $done-last)
      return (<lf>&#xA;</lf>[$i > 1], $d)
    }
  else
    let $item := $todo[1]
    let $todo := $todo[position() > 1]
    return
      typeswitch ($item)
      case element(tab) return
        let $tab := xs:integer($item/@col) - 1
        let $n := $tab - string-length($done-last)
        return
          if ($n = 0) then
            b:break-lines($done-earlier, $done-last, $todo)
          else if ($n > 0) then
            let $spaces := string-join((1 to $n)!" ")
            return b:break-lines($done-earlier, element line {$done-last/node(), text{$spaces}}, $todo)
          else if (normalize-space($done-last) != "") then
            let $spaces := string-join((1 to $tab)!" ")
            return b:break-lines(($done-earlier, $done-last), element line {$spaces}, $todo)
          else
            let $spaces := string-join((1 to $tab)!" ")
            return b:break-lines(($done-earlier, $done-last), element line {$spaces}, $todo)
      case element(fragment) return
        b:break-lines
        (
          $done-earlier,
          element line{$done-last/node(), text{" "}[$done-last != ""], $item/node()},
          $todo
        )
      case element(name) return
        b:break-lines
        (
          $done-earlier,
          element line{$done-last/node(), text{" "}[$done-last != ""], $item},
          $todo
        )
      case text() return
        b:break-lines
        (
          $done-earlier,
          element line{$done-last/node(), text{" "}[$done-last != ""], $item},
          $todo
        )
      case xs:string return
        b:break-lines
        (
          $done-earlier,
          element line{$done-last/node(), text{" "}[$done-last != ""], text{$item}},
          $todo
        )
      case processing-instruction() return
        if (local-name($item) = "TOKENS" and $item = "") then
          b:break-lines($done-earlier, $done-last, (<tab col="1"/>, "&#xa;<?TOKENS?>&#xa;", <tab col="1"/>, $todo))
        else if (local-name($item) = "ENCORE" and $item = "") then
          b:break-lines($done-earlier, $done-last, (<tab col="1"/>, "&#xa;<?ENCORE?>", <tab col="1"/>, $todo))
        else
          b:break-lines
          (
            $done-earlier,
            $done-last,
            (
              <tab col="{$b:t2}"/>,
              let $data := replace(data($item), "^#line [0-9]+ ""[^""]*""\s+", "")
              return
                if (not(contains($data, "&#xa;"))) then
                  concat("<?", local-name($item), " "[$data], $data, "?>")
                else
                (
                  concat("<?", local-name($item)),
                  <tab col="{$b:t2 + 2}"/>,
                  b:re-indent($data, $b:t2 + 2),
                  <tab col="{$b:t2}"/>,
                  "?>"
                ),
              <tab col="{if ($item/parent::g:grammar) then 1 else $b:t1 + 1}"/>,
              $todo
            )
          )
      default return
        error(xs:QName("b:break-lines"), concat("invalid input type: ", string($item)))
};

declare function b:to-html($nodes as node()*, $namespace as xs:string?) as node()*
{
  for $node at $i in $nodes
  return
    typeswitch ($node)
    case element(u) return
      element {QName($namespace, "u")} {$node/node()}
    case element(name) return
      let $qname := QName($namespace, "a")
      return
        element {$qname}
        {
          attribute href {concat("#", $node)},
          attribute title {$node},
          data($node)
        }
    case element(link) return
      let $qname := QName($namespace, "a")
      return
        element {$qname}
        {
          attribute href {$node},
          attribute title {$node},
          attribute target {"_blank"},
          data($node)
        }
    case element(lf) return
      ()
    case element(line) return
      let $qname := QName($namespace, "div")
      return element {$qname} {b:to-html($node/node(), $namespace)}
    case element() return
      b:to-html($node/node(), $namespace)
    case text() return
      if (matches($node, "^ +::= .*$") or $node[matches(., "^ +.*$")]/parent::line and $i eq 1) then
        let $spaces := string-length(replace($node, "^( +).*$", "$1"))
        return text {string-join(((1 to $spaces)!"&#xA0;", substring($node, $spaces + 1)))}
      else
        $node
    default return
      error(xs:QName("b:to-html"), concat("invalid input type: ", string($node)))
};

(:~
 : Render a grammar fragment, returning a single string.
 :
 : @param $nodes the grammar fragment to be rendered.
 : @return the rendered result.
 :)
declare function b:render($nodes as node()*) as xs:string
{
  b:break-lines((), (), b:render-items($nodes))
};

(:~
 : Render a grammar fragment as HTML, returning a sequence of nodes.
 :
 : @param $nodes the grammar fragment to be rendered.
 : @param $namespace the namespace for HTML tags.
 : @return the rendered result.
 :)
declare function b:render-as-html($nodes as node()*, $namespace as xs:string?) as node()*
{
  b:to-html(b:break-lines((), (), b:render-items($nodes)), $namespace)
};
