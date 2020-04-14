(:~
 : Eliminating direct recursion from an EBNF grammar.
 :)
module namespace r="de/bottlecaps/railroad/xq/eliminate-recursion.xq";
import module namespace n="de/bottlecaps/railroad/xq/normalize-ast.xq" at "normalize-ast.xq";
import module namespace b="de/bottlecaps/railroad/xq/ast-to-ebnf.xq" at "ast-to-ebnf.xq";
declare namespace g="http://www.w3.org/2001/03/XPath/grammar";

(:~
 : Remove left-recursion from grammar.
 :
 : @param $ast the grammar.
 : @return the grammar, with left-recursive productions
 : transformed to equivalent EBNF constructs.
 :)
declare function r:eliminate-left-recursion($ast as element(g:grammar)) as element(g:grammar)
{
  if (not(r:is-left-recursive($ast))) then
    $ast
  else
    element g:grammar
    {
      $ast/@*,
      for $p in $ast/node()
      return
        if (not($p/self::g:production and r:is-left-recursive($p))) then
          $p
        else
          let $non-recursive-p :=
            element g:production
            {
              $p/@*,
              let $cases := r:left-unfold-cases($p/node())
              let $non-recursive-cases :=
                for $case in $cases
                let $sequence := n:unwrap-sequence($case)
                where not($sequence[1]/self::g:ref[empty(@context) and @name eq $p/@name])
                return $case
              let $recursive-cases :=
                for $case in $cases
                let $sequence := n:unwrap-sequence($case)
                where $sequence[1]/self::g:ref[empty(@context) and @name eq $p/@name]
                return n:wrap-sequence(subsequence($sequence, 2))
              return
                if (deep-equal($recursive-cases, $non-recursive-cases)) then
                  element g:oneOrMore {n:choice($recursive-cases)}
                else
                (
                  n:choice($non-recursive-cases),
                  element g:zeroOrMore {n:choice($recursive-cases)}
                )
            }
          return
            if (r:is-left-recursive($non-recursive-p)) then
              error
              (
                xs:QName("r:eliminate-left-recursion"),
                concat
                (
                  "failed to eliminate direct left recursion from this production:&#xA;  ",
                  replace(b:render($p), "&#xA;", "&#xA;  "),
                  "&#xA;result was:&#xA;",
                  replace(b:render($non-recursive-p), "&#xA;", "&#xA;  ")
                )
              )
            else
              $non-recursive-p
    }
};

(:~
 : Remove direct right-recursion from grammar.
 :
 : @param $ast the grammar.
 : @return the grammar, with (purely) left- or right-recursive productions
 : transformed to equivalent EBNF constructs.
 :)
declare function r:eliminate-right-recursion($ast as node()) as node()
{
  if (not(r:is-right-recursive($ast))) then
    $ast
  else
    let $reversed := n:reverse($ast)
    let $removed := r:eliminate-left-recursion($reversed)
    return
      if (deep-equal($reversed, $removed)) then
        $ast
      else
        n:reverse($removed)
};

(:~
 : Remove recursion from grammar.
 :
 : @param $grammar the grammar.
 : @param $recursion-removal the removal options, i.e. "left" and/or "right".
 : @return the transformed grammar.
 :)
declare function r:eliminate-recursion($grammar as element(g:grammar),
                                       $recursion-removal as xs:string) as element(g:grammar)
{
  if ($recursion-removal eq "none") then
    $grammar
  else if ($recursion-removal = ("full", "left", "right")) then
    let $g1 := n:group-productions-by-nonterminal($grammar)
    let $g2 :=
      if ($recursion-removal = ("left", "full")) then
        r:eliminate-left-recursion($g1)
      else
        $g1
    let $g3 :=
      if ($recursion-removal = ("right", "full")) then
        r:eliminate-right-recursion($g2)
      else
        $g2
    return $g3
  else
    error(xs:QName("r:eliminate-recursion"), concat("invalid argument: $recursion-removal: ", string-join($recursion-removal, ", ")))
};

declare function r:eliminated-recursion-with-single-reference($before as element(g:grammar),
                                                              $after as element(g:grammar),
                                                              $recursion-removal as xs:string) as element(g:production)*
{
  let $recursive-production-names :=
    for $p in $before/g:production
    let $q := $after/g:production[@name = $p/@name and empty(@context)]
    where ($recursion-removal = ("full", "left") and r:is-left-recursive($p)
        or $recursion-removal = ("full", "right") and r:is-right-recursive($p))
      and not(r:is-left-recursive($q))
      and not(r:is-right-recursive($q))
    return $p/@name
  for $p in $after/g:production
  where $recursive-production-names = $p/@name
    and count($after//g:ref[empty(@context) and @name = $p/@name]) eq 1
  return $p
};

declare function r:is-left-recursive($node as element()?) as xs:boolean
{
  if ($node/self::g:grammar) then
    some $production in $node/g:production satisfies r:is-left-recursive($production)
  else if ($node/self::g:production) then
    let $recursive-refs := $node//g:ref[empty(@context) and @name = $node/@name]
    return exists($recursive-refs)
       and (every $ref in $recursive-refs satisfies r:is-left-recursive($ref))
       and r:has-non-left-recursive-cases($node)
  else
    let $production := $node/ancestor-or-self::g:production
    return
      if ($node/self::g:ref) then
        empty($node/@context) and $node/@name eq $production/@name and
        (every $ancestor in $node/ancestor-or-self::*[. >> $production and not(parent::g:choice)] satisfies not($ancestor/self::g:oneOrMore) and not($ancestor/self::g:zeroOrMore) and empty($ancestor/preceding-sibling::*))
      else if ($node/self::g:choice) then
        some $case in $node/* satisfies r:is-left-recursive($case)
      else if (not($node/self::g:production or $node/self::g:sequence or $node/self::g:optional)) then
        false()
      else
        let $first := $node/*[1]
        return r:is-left-recursive($first)
};

declare function r:is-right-recursive($node as element()?) as xs:boolean
{
  if ($node/self::g:grammar) then
    some $production in $node/g:production satisfies r:is-right-recursive($production)
  else if ($node/self::g:production) then
    let $recursive-refs := $node//g:ref[empty(@context) and @name = $node/@name]
    return exists($recursive-refs)
       and (every $ref in $recursive-refs satisfies r:is-right-recursive($ref))
       and r:has-non-right-recursive-cases($node)
  else
    let $production := $node/ancestor-or-self::g:production
    return
      if ($node/self::g:ref) then
        empty($node/@context) and $node/@name eq $production/@name and
        (every $ancestor in $node/ancestor-or-self::*[. >> $production and not(parent::g:choice)] satisfies not($ancestor/self::g:oneOrMore) and not($ancestor/self::g:zeroOrMore) and empty($ancestor/following-sibling::*))
      else if ($node/self::g:choice) then
        some $case in $node/* satisfies r:is-right-recursive($case)
      else if (not($node/self::g:production or $node/self::g:sequence or $node/self::g:optional)) then
        false()
      else
        let $last := $node/*[last()]
        return r:is-right-recursive($last)
};

declare function r:has-non-left-recursive-cases($p as element(g:production)) as xs:boolean
{
  let $cases := r:left-unfold-cases($p/node())
  let $non-recursive-cases :=
    for $case in $cases
    let $sequence := n:unwrap-sequence($case)
    where not($sequence[1]/self::g:ref[empty(@context) and @name eq $p/@name])
    return $case
  return exists($non-recursive-cases)
};

declare function r:has-non-right-recursive-cases($p as element(g:production)) as xs:boolean
{
  r:has-non-left-recursive-cases(n:reverse($p))
};

declare function r:left-unfold-cases($nodes as element()*) as element()*
{
  let $head := $nodes[1]
  let $tail := subsequence($nodes, 2)
  return
    if ($head/self::g:production) then
      error(xs:QName("r:left-unfold-cases"), concat("invalid argument: ", local-name($head)))
    else if (not(r:is-left-recursive($head))) then
      n:wrap-sequence($nodes)
    else
      typeswitch ($head)
      case element(g:choice) return
          for $case in $head/*
          return
            if (not(r:is-left-recursive($case))) then
              n:wrap-sequence((n:unwrap-sequence($case), $tail))
            else
              for $subcase in r:left-unfold-cases(n:unwrap-sequence($case))
              let $subcase-items := n:unwrap-sequence($subcase)
              return n:wrap-sequence(($subcase-items, $tail))
      case element(g:optional) return
      (
        n:wrap-sequence($tail),
        for $case in r:left-unfold-cases(n:children($head))
        return n:wrap-sequence((n:unwrap-sequence($case), $tail))
      )
      default return
        n:wrap-sequence($nodes)
};
