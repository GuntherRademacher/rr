(:~
 : Normalize AST, reducing EBNF to use just two operators, g:choice and
 : g:oneOrMore. This serves both for simplifying factorization rules, and
 : for a uniform appearance of the railroad graphics regardless of the
 : original formulation of grammar rules. Also some flattening of g:choice
 : and g:charClass operators is done here.
 :
 : The reverse operation, here called denormalization, attempts to reinstall
 : the other operators for better readability of EBNF grammars.
 :)
module namespace n="de/bottlecaps/railroad/xq/normalize-ast.xq";

import module namespace b="de/bottlecaps/railroad/xq/ast-to-ebnf.xq" at "ast-to-ebnf.xq";

declare namespace g="http://www.w3.org/2001/03/XPath/grammar";

(:~
 : Whether empty cases should be shown as the last alternative
 :)
declare variable $n:empty-last := false();

(:~
 : Remove any g:sequence wrappers from a grammar fragment.
 :
 : @param $nodes the grammar fragment.
 : @return the grammar fragment, freed from any g:sequence wrappers.
 :)
declare function n:unwrap-sequence($nodes as node()*) as node()*
{
  for $node in $nodes
  return
    if ($node/self::g:sequence) then
      for $c in n:children($node) return n:unwrap-sequence($c)
    else
      $node
};

(:~
 : Wrap a node sequence into a g:sequence, unless it is a
 : singleton sequence.
 :
 : @param $nodes the node sequence.
 : @return the wrapped node sequence, or single node.
 :)
declare function n:wrap-sequence($nodes as node()*) as node()
{
  if (count($nodes) eq 1) then
    $nodes
  else
    element g:sequence {$nodes}
};

(:~
 : Identify cases for creating a g:choice operator from a single candidate
 : node. Unwrap any g:sequence, and recursively identify cases, if there
 : is exactly one node. Otherwise re-wrap as a g:sequence.
 :
 : @param $node the candidate node.
 : @return the sequence of cases.
 :)
declare function n:case($node as element()) as element()+
{
  let $non-sequence := n:unwrap-sequence($node)
  return
    if (count($non-sequence) eq 1) then
      n:cases($non-sequence)
    else
      n:wrap-sequence($non-sequence)
};

(:~
 : Identify cases for normalizing an arbitrary parent operator into a g:choice
 : operator. The cases of a g:choice or g:charClass operator are the
 : alternatives of that operator. A g:optional operator has an empty case and
 : one comprising the child sequence. A g:zeroOrMore operator has an empty case
 : and a g:oneOrMore, that represents the child sequence. Any other operator
 : remains unchanged, i.e. it is a case in itself.
 :
 : @param $node the parent operator of the cases to be identified.
 : @return the sequence of cases.
 :)
declare function n:cases($node as element()) as element()*
{
  let $children := n:children($node)
  return
    typeswitch ($node)
    case element(g:choice) return
      for $c in $children return n:case($c)
    case element(g:charClass) return
      for $c in $children return n:case($c)
    case element(g:optional) return
    (
      n:case(n:wrap-sequence($children)),
      n:wrap-sequence(())
    )
    case element(g:zeroOrMore) return
    (
      element g:oneOrMore {$children},
      n:wrap-sequence(())
    )
    default return
      $node
};

(:~
 : Rewrite grammar such that there is not more than one production per
 : nonterminal. Multiple productions for the same nonterminal are
 : replaced by a single production containing a choice.
 :
 : @param $grammar the grammar.
 : @return the grammar with distinct production names.
 :)
declare function n:group-productions-by-nonterminal(
  $grammar as element(g:grammar)) as element(g:grammar)
{
  element g:grammar
  {
    let $end := n:syntax-end($grammar)
    return
    (
      $grammar/@*,
      n:children($grammar)[not(self::g:production or . is $end or . >> $end)],
      for $parser in (true(), false())
      return
      (
        let $production-group := $grammar/g:production[$parser != boolean(. >> $end)]
        for $qualified-name in distinct-values($production-group/string-join((@name, @context), "^"))
        let $name := tokenize($qualified-name, "\^")
        let $productions := $production-group[@name = $name[1] and string(@context) = string($name[2])]
        order by n:index-of-node($production-group, $productions[1])
        return
          if (count($productions) = 1) then
            $productions
          else
            element g:production
            {
              for $n in distinct-values($productions/@*/node-name(.))
              return
                attribute {$n}
                {
                  distinct-values($productions/@*[node-name(.) = $n])
                },
              element g:choice
              {
                for $p in $productions
                return
                  if ($p/*[last() = 1]/self::g:choice) then
                    $p/*/*
                  else
                    n:wrap-sequence($p/*)
              }
            },
        $end[$parser]
      ),
      n:children($grammar)[not(self::g:production or . is $end or . << $end)]
    )
  }
};

(:~
 : Normalize a grammar or fragment of a grammar, such that it becomes suitable
 : for factorization, or railroad diagram creation. A normalized grammar has
 : the g:optional and g:zeroOrMore operators replaced by equivalent combinations
 : of the g:choice and g:oneOrMore operators. Empty branches of g:choice operators
 : are ordered last.
 :
 : @param $nodes the grammar fragment to be normalized.
 : @return the normalized grammar fragment.
 :)
declare function n:normalize($nodes as node()*) as node()*
{
  for $node in $nodes
  let $children := n:children($node)
  return
    if ($node/self::g:grammar) then
      element g:grammar
      {
        let $grammar := n:group-productions-by-nonterminal($node)
        return
        (
          $grammar/@*,
          n:normalize(n:children($grammar))
        )
      }
    else if ($node/self::g:choice
          or $node/self::g:optional
          or $node/self::g:zeroOrMore
          or $node/self::g:charClass) then
      n:choice
      ((
        for $c in n:cases($node)
        return n:wrap-sequence(n:normalize(n:unwrap-sequence($c)))
      ))
    else if ($node/self::g:oneOrMore) then
      element g:oneOrMore {n:normalize($children)}
    else if ($node/self::g:charRange) then
        $node
    else if (empty($children)) then
      $node
    else if (n:is-sequence-item($children)) then
      element {node-name($node)}
      {
        $node/@*,
        n:normalize($children)
      }
    else
      element {node-name($node)}
      {
        for $c in $children
        return n:wrap-sequence(n:normalize($c))
      }
};

(:~
 : Introduce g:oneOrMoreWithSeparator operators with non-empty separators. First
 : convert g:oneOrMore to g:oneOrMoreWithSeparator with empty separator, then match
 : content of g:oneOrMoreWithSeparator with preceding nodes. This corresponds to
 : identifying 'B C' as the separator in 'A (B C A)*' and replacing accordingly.
 :
 : @param $nodes the grammar fragment to be normalized.
 : @return the normalized grammar fragment.
 :)
declare function n:introduce-separators($nodes as node()*) as node()*
{
  let $nodes := n:rewrite-oneOrMore($nodes)
  let $nodes := n:introduce-trivial-repeater($nodes)
  return $nodes
};

declare function n:rewrite-oneOrMore($nodes as node()*) as node()*
{
  let $normalized-nodes :=
    element g:sequence
    {
      for $node in $nodes
      return
        typeswitch ($node)
        case element(g:oneOrMore) return
          element g:oneOrMoreWithSeparator
          {
            $node/@*,
            n:wrap-sequence(n:rewrite-oneOrMore($node/*)),
            n:wrap-sequence(())
          }
        case element() return
          if (n:is-sequence-item($node/node())) then
            element {node-name($node)} {$node/@*, n:rewrite-oneOrMore($node/node())}
          else
            element {node-name($node)}
            {
              $node/@*,
              for $child in $node/*
              return n:wrap-sequence(n:rewrite-oneOrMore($child))
            }
        default return
          $node
    }/node()
  return
    if (not(n:is-sequence-item($nodes))) then
      $normalized-nodes
    else
      let $head :=
      (
        for $choice in $normalized-nodes/self::g:choice
        let $cases := n:children($choice)
        let $oneOrMoreCase := $cases/self::g:oneOrMoreWithSeparator
        let $emptyCase := $cases/self::g:sequence[empty(n:children(.))]
        let $args := n:children($oneOrMoreCase)
        where count($cases) eq 2
          and exists($oneOrMoreCase)
          and exists($emptyCase)
          and $args[2]/self::g:sequence[empty(n:children(.))]
        return
          for $head in n:unwrap-sequence($args[1])
          let $candidate := ($head, $head[parent::g:sequence]/following-sibling::*)
          where deep-equal($candidate, ($choice/preceding-sibling::*)[position() > last() - count($candidate)])
          return $head
      )
      let $head := $head[1]
      return
        if (empty($head)) then
          $normalized-nodes
        else
          let $separator := $head[parent::g:sequence]/preceding-sibling::*
          let $orMore := ($head, $head[parent::g:sequence]/following-sibling::*)
          let $choice := $head/ancestor::g:choice[1]
          let $content :=
            element g:sequence
            {
              ($choice/preceding-sibling::*)[position() <= last() - count($orMore)],
              element g:oneOrMoreWithSeparator
              {
                if (count($orMore) ne 1 or empty($orMore/self::g:oneOrMoreWithSeparator)) then
                (
                  n:wrap-sequence($orMore),
                  n:wrap-sequence($separator)
                )
                else
                (
                  n:wrap-sequence($orMore/*[1]),
                  n:choice
                  ((
                    if ($orMore/*[2]/self::g:choice) then $orMore/*[2]/* else $orMore/*[2],
                    if (count($separator) eq 1 and $separator/self::g:choice) then $separator/* else $separator
                  ))
                )
              },
              $choice/following-sibling::*
            }
          return n:rewrite-oneOrMore($content/node())
};

declare function n:introduce-trivial-repeater($nodes)
{
  element g:sequence
  {
    for $node in $nodes
    let $oom := $node/g:oneOrMoreWithSeparator
    return
      if ($node/self::g:choice
          and count($oom) eq 1
          and $node/g:sequence[empty(*)]
          and n:no-sequence($oom/*[1])
          and $oom/*[2]/self::g:sequence[empty(*)]
         ) then
        n:choice
        ((
          for $case in $node/*[not(self::g:sequence[empty(*)])]
          return
            if (not($case/self::g:oneOrMoreWithSeparator)) then
              $case
            else
              element g:oneOrMoreWithSeparator
              {
                $case/*[2],
                $case/*[1]
              }
        ))
      else if (not($node/self::element())) then
        $node
      else
        element {node-name($node)}
        {
          $node/@*,
          if (n:is-sequence-item($node/node())) then
            n:introduce-trivial-repeater($node/node())
          else
            for $child in $node/*
            return n:wrap-sequence(n:introduce-trivial-repeater($child))
        }
  }/node()
};

declare function n:no-sequence($node as node())
{
  $node[self::g:ref or
        self::g:char or
        self::g:charRange or
        self::g:charCode or
        self::g:charCodeRange or
        self::g:string or
        self::g:subtract] or
  $node/self::g:choice and (every $case in $node/* satisfies n:no-sequence($case)) or
  $node/self::g:oneOrMoreWithSeparator and (every $arg in $node/* satisfies n:no-sequence($arg))
};

(:~
 : Denormalize a single g:oneOrMoreWithSeparator.
 :
 : If the separator is empty, construct a g:oneOrMore with the same
 : body. Otherwise, return the body, followed by a g:zeroOrMore
 : containing the separator and the body.
 :
 : @param $node the g:oneOrMoreWithSeparator node to be denormalized.
 : @return the denormalized equivalent of $node.
 :)
declare function n:denormalize-oneOrMoreWithSeparator(
  $node as element(g:oneOrMoreWithSeparator)) as element()*
{
  let $children := n:children($node)
  let $denormalized-body := n:unwrap-sequence(n:denormalize($children[1]))
  return
    if ($children[2]/self::g:sequence[empty(n:children(.))]) then
      element g:oneOrMore {$denormalized-body}
    else
      let $replacement :=
      (
        $denormalized-body,
        element g:zeroOrMore
        {
          n:unwrap-sequence(n:denormalize($children[2])),
          $denormalized-body
        }
      )
      return $replacement
};

(:~
 : Denormalize a single g:oneOrMore.
 :
 : Construct a g:oneOrMore with the same body.
 :
 : @param $node the g:oneOrMore node to be denormalized.
 : @return the denormalized equivalent of $node.
 :)
declare function n:denormalize-oneOrMore(
  $node as element(g:oneOrMore)) as element()*
{
  element g:oneOrMore {n:denormalize(n:children($node))}
};

(:~
 : Denormalize a single g:choice. Separate charClass components
 : from other components. Reinstall any outer g:optional or
 : g:zeroOrMore operators, if appropriate.
 :
 : @param $node the g:choice node to be denormalized.
 : @return the denormalized equivalent of $node.
 :)
declare function n:denormalize-choice($node as element(g:choice)) as element()*
{
  let $cases := n:children($node)
  let $empty-cases := $cases[self::g:sequence[empty(n:children(.))]]
  let $charclass-cases := $cases[self::g:char or
                                 self::g:charRange or
                                 self::g:charCode or
                                 self::g:charCodeRange or
                                 self::g:string[string-length() eq 1]]
  let $charclass-cases :=
    if (exists($charclass-cases[not(self::g:string)])
        and (count($charclass-cases) gt 1
             or $charclass-cases/self::g:charRange
             or $charclass-cases/self::g:charCodeRange)) then
      $charclass-cases
    else
      ()
  let $denormalized-cases :=
  (
    for $case in $cases
    where empty($empty-cases[. is $case])
    return
      if ($charclass-cases[1] is $case) then
        element g:charClass
        {
          let $ordered-groups :=
            for $c in $charclass-cases
            order by exists(($c/@value, $c/@minValue))
            return
              if ($c/self::g:string) then
                element g:char{data($c)}
              else
                $c
          return
            for $g at $i in $ordered-groups
            where not(deep-equal($ordered-groups[$i - 1], $ordered-groups[$i]))
            return $g
        }
      else if (exists($charclass-cases[. is $case])) then
        ()
      else
        n:wrap-sequence(n:denormalize($case))
  )
  let $denormalized-result :=
    if (count($denormalized-cases) != 1) then
      element g:choice {$denormalized-cases}
    else
      $denormalized-cases
  return
    if (empty($empty-cases)) then
      n:unwrap-sequence($denormalized-result)
    else if ($denormalized-result/self::g:oneOrMore) then
      element g:zeroOrMore {n:children($denormalized-result)}
    else if (empty($denormalized-cases/self::g:oneOrMore)) then
      element g:optional {n:unwrap-sequence($denormalized-result)}
    else
      let $oneOrMore-case :=
      (
        for $c at $i in $denormalized-cases
        where $c/self::g:oneOrMore
        return $i
      )[1]
      return
        element g:choice
        {
          $denormalized-cases[position() < $oneOrMore-case],
          element g:zeroOrMore {$denormalized-cases[$oneOrMore-case]/node()},
          $denormalized-cases[position() > $oneOrMore-case]
        }
};

(:~
 : Denormalize a single g:subtract operator. Rewrite operator
 : with recursively denormalized operand nodes.
 :
 : @param $node the g:subtract to be denormalized.
 : @return the denormalized equivalent.
 :)
declare function n:denormalize-subtract($node as element(g:subtract)) as element(g:subtract)
{
  element g:subtract
  {
    for $child in n:children($node)
    return n:wrap-sequence(n:denormalize($child))
  }
};

(:~
 : Denormalize a grammar or fragment of a grammar. This serves for
 : reversing the effect of  prior normalization for railroad diagram
 : creation. The denormalized grammar or fragment makes use of more
 : operators and is thus better suitable for rendereing as ebnf.
 :
 : @param $nodes the grammar fragment to be denormalized.
 : @return the denormalized grammar fragment.
 :)
declare function n:denormalize($nodes as node()*) as node()*
{
  for $node in $nodes
  return
    typeswitch ($node)
    case element(g:oneOrMoreWithSeparator) return
      n:denormalize-oneOrMoreWithSeparator($node)
    case element(g:oneOrMore) return
      n:denormalize-oneOrMore($node)
    case element(g:choice) return
      n:denormalize-choice($node)
    case element(g:char) return
      if ($node/parent::g:complement) then element g:charClass {$node} else $node
    case element(g:charCode) return
      if ($node/parent::g:complement) then element g:charClass {$node} else $node
    case element(g:charRange) return
      element g:charClass {$node}
    case element(g:charCodeRange) return
      element g:charClass {$node}
    case element(g:subtract) return
      n:denormalize-subtract($node)
    case element(g:equivalence) return
      $node
    case element() return
      element {node-name($node)}
      {
        $node/@*,
        if (empty(n:children($node))) then
          $node/node()
        else
          n:denormalize(n:children($node))
      }
    default return
      $node
};

(:~
 : Return child nodes as far as relevant for grammar processing,
 : i.e. child elements and processing instructions.
 :
 : @param $e the parent node.
 : @return the child nodes.
 :)
declare function n:children($e as node()?) as node()*
{
  for $child in $e/node()
  where $child/self::element()
     or $child/self::processing-instruction()
  return $child
};

(:~
 : Node sequence based index-of. As shown in sample code of the
 : XQuery recommendation.
 :
 : @param $sequence the node sequence.
 : @return $srch the node to be searched.
 :)
declare function n:index-of-node($sequence as node()*, $srch as node()) as xs:integer*
{
  for $n at $i in $sequence where $n is $srch return $i
};

(:~
 : Find the processing instruction (if any) that marks the end of
 : the parser rules.
 :
 : @param $grammar the grammar.
 : @return the processing instruction marking the end of parser rules,
 : or empty sequence, if the grammar does not contain one.
 :)
declare function n:syntax-end($grammar as element(g:grammar)) as processing-instruction()?
{
  $grammar/processing-instruction()[local-name(.) = ("TOKENS", "ENCORE") and string(.) eq ""][1]
};

(:~
 : Normalize a g:choice operator by extracting any "empty" case.
 :
 : @param $cases the individual cases, wrapped in a g:sequence,
 : if necessary.
 : @return the normalized node sequence.
 :)
declare function n:choice($cases as element()*) as element()*
{
  let $cases :=
    for $case in $cases
    return
      if ($case/self::element(g:choice)) then
        n:children($case)
      else
        $case
  let $empty := $cases[exists(self::g:sequence[empty(n:children(.))])]

(::)
  let $cases := $cases[empty (self::g:sequence[empty(n:children(.))])]
  where exists($cases)
  return
    let $cases :=
      if ($n:empty-last or exists($cases/descendant-or-self::g:oneOrMore)) then
        ($cases, $empty[1])
      else
        ($empty[1], $cases)
(::)

(:?
  let $non-empty := $cases[empty (self::g:sequence[empty(n:children(.))])]
  return
    let $cases :=
      for $case in $cases
      where ($non-empty, $empty[1])[. is $case]
      return $case
?:)

    return
      if (empty($cases[2])) then
        n:unwrap-sequence($cases)
      else
        element g:choice {$cases}
};

(:~
 : Verify given nodes are in a sequence context, by checking that
 : none of them occur in a non-sequence context.
 :
 : @param $nodes the set of nodes.
 : @return true, if none has a non-sequence context.
 :)
declare function n:is-sequence-item($nodes as node()*) as xs:boolean
{
  not
  (
       $nodes/parent::g:choice
    or $nodes/parent::g:orderedChoice
    or $nodes/parent::g:oneOrMoreWithSeparator
    or $nodes/parent::g:subtract
  )
};

(:~
 : Reverse the order of node sequences in a grammar fragment.
 :
 : @param $nodes the grammar fragment to be reversed.
 : @return the reversed grammar fragment.
 :)
declare function n:reverse($nodes as node()*) as node()*
{
  let $reordered :=
    if ($nodes/parent::g:grammar) then
      $nodes
    else if (n:is-sequence-item($nodes)) then
      reverse($nodes)
    else
      $nodes
  for $n in $reordered
  return
    let $children := n:children($n)
    return
      if (empty($children)) then
        $n
      else
        element {node-name($n)} {$n/@*, n:reverse($children)}
};

(:~
 : Strip processing instructions from grammar fragment.
 :
 : @param $nodes the grammar fragment
 :)
declare function n:strip-pi($nodes)
{
  for $node in $nodes
  return
    typeswitch ($node)
    case document-node() return
      document
      {
        n:strip-pi($node/node())
      }

    case element() return
      element {node-name($node)}
      {
        $node/@*,
        if ($node/self::g:choice or
            $node/self::g:orderedChoice or
            $node/self::g:subtractor or
            $node/self::g:context or
            $node/self::g:delimiter or
            $node/self::g:preference) then
        (
          for $child in $node/(* | processing-instruction())
          let $stripped :=
            if ($child/self::g:sequence) then
              n:strip-pi($child/*)
            else
              n:strip-pi($child)
          return
            if (count($stripped) eq 1) then
              $stripped
            else
              element g:sequence {$stripped}
        )
        else
          n:strip-pi($node/node())
      }
    case comment() return
      $node[starts-with(., "*")]
    case processing-instruction() return
      if (local-name($node) = "TOKENS" and normalize-space($node) eq "") then
        <?TOKENS?>
      else
        ()
    default return
      $node
};
