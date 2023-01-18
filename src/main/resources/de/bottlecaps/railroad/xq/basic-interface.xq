(:~
 : The basic interface module, containing code that drives parsing and
 : conversion to svg and xhtml.
 :)
module namespace i="de/bottlecaps/railroad/xq/basic-interface.xq";

declare namespace p="Parser";
import module namespace a="de/bottlecaps/railroad/xq/cst-to-ast.xq" at "cst-to-ast.xq";
import module namespace v="de/bottlecaps/railroad/xq/ast-to-svg.xq" at "ast-to-svg.xq";
import module namespace t="de/bottlecaps/railroad/xq/transform-ast.xq" at "transform-ast.xq";
import module namespace style="de/bottlecaps/railroad/xq/style.xq" at "style.xq";

declare namespace xhtml="http://www.w3.org/1999/xhtml";

declare function i:ebnf-to-xhtml($ebnf as xs:string,
                                 $show-ebnf as xs:boolean?,
                                 $recursion-elimination as xs:boolean,
                                 $factoring as xs:boolean,
                                 $inline as xs:boolean,
                                 $keep as xs:boolean,
                                 $width as xs:integer?,
                                 $color as xs:string?,
                                 $spread as xs:integer,
                                 $uri as xs:string?) as element(xhtml:html)
{
  <html xmlns="http://www.w3.org/1999/xhtml">
    <head>{v:head(($color, $style:default-color)[1], $spread, $width)}</head>
    <body>{i:ebnf-to-svg($ebnf, $show-ebnf, $recursion-elimination, $factoring, $inline, $keep, $width, $color, $spread, $uri)}</body>
  </html>
};

declare function i:ebnf-to-svg($ebnf as xs:string,
                               $show-ebnf as xs:boolean?,
                               $recursion-elimination as xs:boolean,
                               $factoring as xs:boolean,
                               $inline as xs:boolean,
                               $keep as xs:boolean,
                               $width as xs:integer?,
                               $color as xs:string?,
                               $spread as xs:integer,
                               $uri as xs:string?) as node()*
{
  let $parse-tree := p:parse-Grammar($ebnf)
  return
    if ($parse-tree/self::ERROR) then
      error(xs:QName("i:ebnf-to-svg"), data($parse-tree))
    else
      let $ast := a:ast($parse-tree)
      let $ast := t:transform($ast,
                              if ($recursion-elimination) then "full" else "none",
                              if ($factoring) then "full-left" else "none",
                              $inline,
                              $keep
                             )
      return v:svg
      (
        $ast,
        ($show-ebnf, true())[1],
        ($width, $v:page-width)[1],
        ($color, $style:default-color)[1],
        $spread,
        false(),
        $uri
      )
};
