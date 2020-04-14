(:~
 : Factorization and inlining.
 :)
module namespace t="de/bottlecaps/railroad/xq/transform-ast.xq";
import module namespace r="de/bottlecaps/railroad/xq/eliminate-recursion.xq" at "eliminate-recursion.xq";
import module namespace n="de/bottlecaps/railroad/xq/normalize-ast.xq" at "normalize-ast.xq";
import module namespace b="de/bottlecaps/railroad/xq/ast-to-ebnf.xq" at "ast-to-ebnf.xq";
declare namespace g="http://www.w3.org/2001/03/XPath/grammar";

(:~
 : The maximum number of alternatives, that is allowed in factorization.
 :)
declare variable $t:alternative-limit := 64;

(:~
 : Check whether a production is trivial, i.e. its body is
 : a single string or empty.
 :
 : @param $p the production.
 : @param $productions the set of productions.
 : @param $string true, if single strings are considered trivial.
 : @param $empty true, if empty bodies are considered trivial.
 : @return true, if the production is trivial.
 :)
declare function t:is-trivial($p as element(g:production),
                              $productions as element(g:production)*,
                              $string as xs:boolean,
                              $empty as xs:boolean) as xs:boolean
{
  empty($productions[@name eq $p/@name and not(. is $p)]) and
  (($string and count($p/*) eq 1 and exists($p/g:string)) or
   ($empty and count($p/*) eq 0))
};

(:~
 : Rewrite a grammar, removing any trivial nonterminals. This involves
 : dropping trivial productions and replacing their references by their
 : respective bodies.
 :
 : @param $grammar the grammar to be rewritten.
 : @param $inline the string inlining option.
 : @param $remove the empty inlining option.
 : @return the grammar, rewritten.
 :)
declare function t:remove-trivial-nonterminals($grammar as element(g:grammar),
                                               $inline as xs:boolean,
                                               $remove as xs:boolean) as element(g:grammar)
{
  let $productions := $grammar/g:production
  return
    if (count($productions) < 2) then
      $grammar
    else
      let $rewrite := t:inline($grammar, $productions[t:is-trivial(., $productions, $inline, $remove)])
      return
        if (empty($rewrite/g:production) or deep-equal($grammar, $rewrite)) then
          $grammar
        else
          t:remove-trivial-nonterminals($rewrite, $inline, $remove)
};

(:~
 : Apply factoring to grammar.
 :
 : @param $grammar the grammar.
 : @param $factoring the factoring options, i.e. "none", "left-only", "full-left", "right-only", "full-right"
 : "left-right", or "right-left".
 : @return the transformed grammar.
 :)
declare function t:factorize($grammar as element(g:grammar),
                             $factoring as xs:string) as element(g:grammar)
{
  if ($factoring = "none") then
    $grammar
  else
    let $g3 := n:normalize($grammar)
    let $g4 :=
      n:denormalize
      (
        n:introduce-separators
        (
          if ($factoring = "left-only") then
            t:left-factorize($g3)
          else if ($factoring = "right-only") then
            t:right-factorize($g3)
          else if ($factoring = "full-left") then
            t:left-factorize(t:right-factorize(t:left-factorize($g3)))
          else if ($factoring = "full-right") then
            t:right-factorize(t:left-factorize(t:right-factorize($g3)))
          else
            error(xs:QName("t:factorize"), concat("invalid argument: $factoring: ", $factoring))
        )
      )
    return
      if (deep-equal($grammar, $g4)) then
        $grammar
      else
        t:factorize($g4, $factoring)
};

(:~
 : Apply transformations to grammar.
 :
 : @param $grammar the grammar.
 : @param $recursion-removal the removal options.
 : @param $factoring the factoring options.
 : @param $inline the string inlining option.
 : @param $keep the empty keeping option.
 : @return the transformed grammar.
 :)
declare function t:transform($grammar as element(g:grammar),
                             $recursion-removal as xs:string*,
                             $factoring as xs:string,
                             $inline as xs:boolean,
                             $keep as xs:boolean) as element(g:grammar)
{
  let $remove := not($keep)
  let $g1 := if ($inline or $remove) then t:remove-trivial-nonterminals($grammar, $inline, $remove) else $grammar
  let $g2 := if (exists($recursion-removal)) then r:eliminate-recursion($g1, $recursion-removal) else $g1
  let $g3 := if ($factoring = ("", "none")) then $g2 else t:factorize($g2, $factoring)
  return
    if (empty($recursion-removal)) then
      $g3
    else
      let $g4 := t:inline($g3, r:eliminated-recursion-with-single-reference($grammar, $g3, $recursion-removal))
      let $g5 := if ($factoring = ("", "none")) then $g4 else t:factorize($g4, $factoring)
      return $g5
};

(:~
 : Apply single-production right-factoring transformation to node $ast.
 :
 : @param $ast the grammar to be transformed.
 : @return the transformed grammar.
 :)
declare function t:right-factorize($ast as node()) as node()
{
  let $reversed := n:reverse($ast)
  let $right-factored := t:left-factorize($reversed)
  return
    if (deep-equal($reversed, $right-factored)) then
      $ast
    else
      n:reverse($right-factored)
};

(:~
 : Apply single-production left-factoring transformation to nodes $todo.
 :
 : @param $todo the nodes to be transformed.
 : @param $done the intermediate result, that was calculated in preceding
 : recursion levels of this tail-recursive transformation.
 : @return the transformed nodes.
 :)
declare function t:left-factorize($todo as node()*, $done as node()*) as node()*
{
  if (empty($todo)) then
    $done
  else if ($todo[1]/self::g:choice and count($todo[1]/*) gt $t:alternative-limit) then
    t:left-factorize($todo[position() gt 1], ($done, $todo[1]))
  else
    let $node := $todo[1]
    let $children := n:children($node)
    let $left-factor :=
      if (not($node/self::g:choice)) then
        ()
      else
        (
          for $c in $children
          let $case := n:unwrap-sequence($c)[1]
          where
            some $d in $children[. << $c]
            satisfies deep-equal($case, n:unwrap-sequence($d)[1])
          return $case
        )[1]
    let $left-factor-choice :=
      if ($left-factor or not($node/self::g:choice)) then
        ()
      else
        (
          for $c in $children
          let $case := n:unwrap-sequence($c)[1]
          where $case/self::g:choice
            and
            (
              every $subcase in n:children($case)
              satisfies
                some $d in $children
                satisfies deep-equal($subcase, $d)
            )
          return $case
        )[1]

(:
    (: A |  B+    D | (B+|) E | F
    => A | (B+|)B D | (B+|) E | F
    :)

    find prefix of:            B+
    use it to create this:    (B+|)
    and verify prefix exists: (B+|)
    if so, replace             B+
    by                        (B+|)B

    do not flatten or the like, and have the left-factor rule catch
    it in the next step.
:)
    let $left-factor-oom :=
      if ($left-factor or $left-factor-choice or not($node/self::g:choice)) then
        ()
      else
        (
          for $c in $children
          let $oneOrMore := n:unwrap-sequence($c)[1]

          where $oneOrMore/self::g:oneOrMore
          return
            let $hs := n:children($oneOrMore)
            let $choice :=
              element g:choice
              {
                $oneOrMore,
                n:wrap-sequence(())
              }
            where
              some $d in $children
              satisfies deep-equal($choice, n:unwrap-sequence($d)[1])
              return ($oneOrMore, n:wrap-sequence(($choice, $hs)))
        )[position() le 2]

    let $single-child := if (count($children) eq 1) then $children else ()
    let $empty := $children/self::g:sequence[empty(n:children(.))]
    let $non-empty := $children[not(. is $empty)]
    return
      if (exists($left-factor)) then

        (: (A|B C|B D|E) => (A|B(C|D)|E):)

        let $choice :=
          let $factored :=
            for $c at $i in $children
            let $elements := n:unwrap-sequence($c)
            where deep-equal($left-factor, $elements[1])
            return $i
          let $cases :=
          (
            $children[position() lt $factored[1]],
            n:wrap-sequence
            ((
              $left-factor,
              n:choice
              (
                for $c at $i in $children[position() = $factored]
                return n:wrap-sequence(n:unwrap-sequence($c)[position() gt 1])
              )
            )),
            for $c at $i in $children
            where not($i = $factored) and $i gt $factored[1]
            return $c
          )
          return n:choice($cases)
        return
          t:left-factorize(($choice, $todo[position() gt 1]), $done)

      else if (exists($left-factor-choice)) then

        (: (A|(B|C)D|B|C|E) => (A|(B|C)D|(B|C)|E) :)

        let $choice :=
          let $factored :=
            for $c at $i in $children
            where some $d in n:children($left-factor-choice) satisfies deep-equal($d, $c)
            return $i
          let $cases :=
          (
            $children[position() lt $factored[1]],
            $left-factor-choice,
            $children[not(position() = $factored) and position() gt $factored[1]]
          )
          return element g:choice {$cases} (: no flattening here :)
        return
          t:left-factorize(($choice, $todo[position() gt 1]), $done)

      else if (exists($left-factor-oom)) then

        let $choice :=
          element g:choice (: no flattening here :)
          {
            for $c at $i in $children
            let $elements := n:unwrap-sequence($c)
            return
              if (deep-equal($left-factor-oom[1], $elements[1])) then
                n:wrap-sequence((n:unwrap-sequence($left-factor-oom[2]), $elements[position() gt 1]))
              else
                $c
          }
        return t:left-factorize(($choice, $todo[position() gt 1]), $done)

      else if (count($children) eq 2
           and $node/self::g:choice
           and exists($empty)
           and n:is-sequence-item($node)
           and exists(n:unwrap-sequence($non-empty))
           and deep-equal(n:unwrap-sequence($non-empty)[1], $todo[2])) then

        (: (A B|) A => A (B A|) :)

        t:left-factorize
        (
          (
            $todo[2],
            n:choice
            ((
              $empty[. << $non-empty],
              n:wrap-sequence
              ((
                n:unwrap-sequence($non-empty)[position() gt 1],
                $todo[2]
              )),
              $empty[. >> $non-empty]
            )),
            $todo[position() gt 2]
          ),
          $done
        )

      else if ($node/self::g:oneOrMore
           and n:is-sequence-item($node)
           and exists($children)
           and deep-equal($children[1], $todo[2])) then

        (: (A B)+ A => A (B A)+ :)

        t:left-factorize
        (
          (
            $todo[2],
            element g:oneOrMore
            {
              $children[position() gt 1],
              $todo[2]
            },
            $todo[position() gt 2]
          ),
          $done
        )

      else if ($node/self::g:choice
           and $children[1]/self::g:oneOrMore
           and $children[2]/self::g:sequence[empty(n:children(.))]
           and n:is-sequence-item($node)
           and exists(n:children($children[1]))
           and deep-equal(n:children($children[1])[1], $todo[2])) then

        (: ((A B)+|) A => A ((B A)+|) :)

        t:left-factorize
        (
          (
            $todo[2],
            element g:choice
            {
              element g:oneOrMore
              {
                n:children($children[1])[position() gt 1],
                $todo[2]
              },
              $children[2]
            },
            $todo[position() gt 2]
          ),
          $done
        )

      else if (empty($children)) then

        t:left-factorize($todo[position() gt 1], ($done, $node))

      else

        t:left-factorize
        (
          $todo[position() gt 1],
          (
            $done,
            element {node-name($node)}
            {
              $node/@*,
              if (n:is-sequence-item($children)) then
                t:left-factorize($children, ())
              else
                for $c in $children
                return n:wrap-sequence(t:left-factorize(n:unwrap-sequence($c), ()))
            }
          )
        )
};

(:~
 : Apply single-production left-factoring transformation to node $ast.
 :
 : @param $ast the grammar to be transformed.
 : @return the transformed grammar.
 :)
declare function t:left-factorize($ast as node()) as node()
{
  let $left-factored := t:left-factorize($ast, ())
  return
    if (deep-equal($ast, $left-factored)) then
      $ast
    else
      t:left-factorize($left-factored)
};

(:~
 : Rewrite a grammar fragment, while inlining some nonterminal references. Drop
 : the inlined productions from the result.
 :
 : @param $nodes the grammar fragment.
 : @param $inline-nonterminals the set of nonterminal productions
 : that will be inlined to their reference context.
 : @return the rewritten grammar fragment.
 :)
declare function t:inline($nodes as node()*,
                          $inline-nonterminals as element(g:production)*) as node()*
{
  for $node in $nodes
  return
    typeswitch ($node)
    case element(g:production) return
      if ($node/@name = $inline-nonterminals/@name) then
        ()
      else
        element g:production
        {
          $node/@*,
          t:inline($node/node(), $inline-nonterminals)
        }
    case element(g:ref) return
      if ($node/@context or not($node/@name = $inline-nonterminals/@name)) then
        $node
      else
        let $definition := $inline-nonterminals[@name eq $node/@name]
        return t:inline(n:children($definition), $inline-nonterminals)
    case element() return
      let $children := n:children($node)
      let $replacement :=
        if (empty($children)) then
          $node/node()
        else
          for $c in $children
          return
            if (n:is-sequence-item($c)) then
              t:inline($c, $inline-nonterminals)
            else
              n:wrap-sequence(t:inline(n:unwrap-sequence($c), $inline-nonterminals))
      return
        if (exists($children) and empty($replacement) and ($node/self::g:zeroOrMore or $node/self::g:oneOrMore or $node/self::g:optional)) then
          ()
        else
          element {node-name($node)}
          {
            $node/@*,
            $replacement
          }
    default return
      $node
};
