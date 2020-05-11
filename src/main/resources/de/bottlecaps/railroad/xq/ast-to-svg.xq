(:~
 : The SVG rendering module for grammars.
 :)
module namespace s="de/bottlecaps/railroad/xq/ast-to-svg.xq";

import module namespace c="de/bottlecaps/railroad/xq/color.xq" at "color.xq";
import module namespace style="de/bottlecaps/railroad/xq/style.xq" at "style.xq";
import module namespace n="de/bottlecaps/railroad/xq/normalize-ast.xq" at "normalize-ast.xq";
import module namespace b="de/bottlecaps/railroad/xq/ast-to-ebnf.xq" at "ast-to-ebnf.xq";

declare default element namespace "http://www.w3.org/2000/svg";

declare namespace g="http://www.w3.org/2001/03/XPath/grammar";
declare namespace svg="http://www.w3.org/2000/svg";
declare namespace xlink="http://www.w3.org/1999/xlink";
declare namespace xhtml="http://www.w3.org/1999/xhtml";
declare namespace text-width="TextWidth";

declare variable $s:logo-color := "#FFCC00";

(:~ The generator version :)
declare variable $s:version external;

(:~ The Java version :)
declare variable $s:java-version external;

(:~ The release date :)
declare variable $s:date external;

(:~
 : The width in pixels of the stubs of a graphic item.
 :)
declare variable $s:stub-width := 10;

(:~
 : Whether to center the alternatives of a choice. If false,
 : they will be left-aligned.
 :)
declare variable $s:center-choice := false();

(:~
 : The distance in pixels of a loop from the graphic of the
 : loop body. Also used for empty branches of a choice.
 :)
declare variable $s:loop-offset := 16;

(:~
 : The distance in pixels of a line from the graphic that
 : it is being connected to.
 :)
declare variable $s:line-offset := 16; (: half of :)

(:~
 : When a graphic breaks into multiple lines, because it
 : exceeds the desired maximum width, lines can be connected with a
 : solid line ("line") or ended to the right and started to
 : the left with a sequence of dots ("dots").
 :)
declare variable $s:line-connect := "dots"; (: line/dots :)

(:~
 : The entry and exit of the graphic for a nonterminal can be
 : marked with small circles ("circle"), arrows ("arrow") or
 : just a stub (empty sequence).
 :)
declare variable $s:begin-end := "arrow"; (: circle/arrow/() :)

(:~
 : Whether to draw shadow rectangles.
 :)
declare variable $s:shadow := true();

(:~
 : Padding between text and its container, in pixels.
 :)
declare variable $s:padding external := 10;

(:~
 : Width of lines and frames, in pixels.
 :)
declare variable $s:stroke-width external := 1;

(:~
 : The font size in pixels.
 :)
declare variable $s:font-size := 12;

(:~
 : Half of the height of a rectangle in pixels.
 :)
declare variable $s:rect-height := $s:padding + $s:font-size idiv 2;

(:~
 : The page width. An attempt will be made to break the graphic
 : into multiple lines, when it exceeds this width.
 :)
declare variable $s:page-width := 992;

(:~
 : The background color for terminal rectangles.
 :
 : @param $color the base color code.
 : @return the rgb color string.
 :)
declare function s:color-1($color as xs:string) {$color};

(:~
 : The background color for regexp polygons.
 :
 : @param $color the base color code.
 : @param $spread the hue offset.
 : @return the rgb color string.
 :)
declare function s:color-regexp($color as xs:string, $spread as xs:integer)
{
  if ($spread eq 0) then
    c:relative-color($color, 1.0, 0.89)
  else
    let $hsl := c:rgb-to-hsl($color)
    return s:color-1(c:rgb(c:hsl-to-rgb(($hsl[1] + 2 * $spread) mod 360, $hsl[2], $hsl[3])))
};

(:~
 : The background color for nonterminal rectangles.
 :
 : @param $color the base color code.
 : @param $spread the hue offset.
 : @return the rgb color string.
 :)
declare function s:color-nonterminal($color as xs:string, $spread as xs:integer)
{
  if ($spread eq 0) then
    c:relative-color($color, 1.0, 0.81)
  else
    let $hsl := c:rgb-to-hsl($color)
    return s:color-1(c:rgb(c:hsl-to-rgb(($hsl[1] + $spread) mod 360, $hsl[2], $hsl[3])))
};

(:~
 : The border color for all polygons. Also used for shadows.
 :
 : @param $color the base color code.
 : @return the rgb color string.
 :)
declare function s:color-5($color as xs:string) {c:relative-color($color, 1.0, 0.10)};

(:~
 : The text color for terminal rectangles.
 :
 : @param $color the base color code.
 : @return the rgb color string.
 :)
declare function s:color-text-terminal($color as xs:string) {c:relative-color($color, 1.0, 0.04)};

(:~
 : The text color for nonterminal rectangles.
 :
 : @param $color the base color code.
 : @parameter $spread the hue offset.
 : @return the rgb color string.
 :)
declare function s:color-text-nonterminal($color as xs:string, $spread as xs:integer)
{
  if ($spread eq 0) then
    c:relative-color($color, 1.0, 0.05)
  else
    let $hsl := c:rgb-to-hsl($color)
    return s:color-text-terminal(c:rgb(c:hsl-to-rgb(($hsl[1] + $spread) mod 360, $hsl[2], $hsl[3])))
};

(:~
 : The text color for regexp rectangles.
 :
 : @param $color the base color code.
 : @parameter $spread the hue offset.
 : @return the rgb color string.
 :)
declare function s:color-text-regexp($color as xs:string, $spread as xs:integer)
{
  if ($spread eq 0) then
    c:relative-color($color, 1.0, 0.06)
  else
    let $hsl := c:rgb-to-hsl($color)
    return s:color-text-terminal(c:rgb(c:hsl-to-rgb(($hsl[1] + 2 * $spread) mod 360, $hsl[2], $hsl[3])))
};

(:~
 : The style element to be generated with SVG graphics.
 :
 : //... ; stroke-dasharray: 1, 1}}
 :
 : @param $color the base color code.
 : @param $spread the hue offset.
 : @return the style element.
 :)
declare function s:style($color as xs:string, $spread as xs:integer)
{
  <style type="text/css">
    @namespace "http://www.w3.org/2000/svg";
    .line                 {{fill: none; stroke: {s:color-5($color)}; stroke-width: {$s:stroke-width};}}
    .bold-line            {{stroke: {s:color-text-terminal($color)}; shape-rendering: crispEdges; stroke-width: 2;}}
    .thin-line            {{stroke: {s:color-text-regexp($color, $spread)}; shape-rendering: crispEdges}}
    .filled               {{fill: {s:color-5($color)}; stroke: none;}}
    text.terminal         {{font-family: Verdana, Sans-serif;
                            font-size: {$s:font-size}px;
                            fill: {s:color-text-terminal($color)};
                            font-weight: bold;
                          }}
    text.nonterminal      {{font-family: Verdana, Sans-serif;
                            font-size: {$s:font-size}px;
                            fill: {s:color-text-nonterminal($color, $spread)};
                            font-weight: normal;
                          }}
    text.regexp           {{font-family: Verdana, Sans-serif;
                            font-size: {$s:font-size}px;
                            fill: {s:color-text-regexp($color, $spread)};
                            font-weight: normal;
                          }}
    rect, circle, polygon {{fill: {s:color-5($color)}; stroke: {s:color-5($color)};}}
    rect.terminal         {{fill: {s:color-1($color)}; stroke: {s:color-5($color)}; stroke-width: {$s:stroke-width};}}
    rect.nonterminal      {{fill: {s:color-nonterminal($color, $spread)}; stroke: {s:color-5($color)}; stroke-width: {$s:stroke-width};}}
    rect.text             {{fill: none; stroke: none;}}
    polygon.regexp        {{fill: {s:color-regexp($color, $spread)}; stroke: {s:color-5($color)}; stroke-width: {$s:stroke-width};}}
  </style>
};

(:~
 : Calculate width in pixels of a string from a width table covering
 : low (8-bit) codepoints. The result is the sum of the widths of
 : the individual characters. For characters not covered by the table,
 : an estimate is used (the average width of an upper or lower case
 : letter).
 :
 : @param $text the string.
 : @param $font-weight the font-weight option.
 : @return the width of the string.
 :)
declare function s:text-width($text as xs:string, $font-weight as xs:string) as xs:integer
{
  if ($font-weight eq "bold") then
    text-width:bold($text)
  else
    text-width:normal($text)
};

(:~
 : Get all x coordinate values from a graphics fragment.
 :
 : @param $svg the graphics fragment.
 : @return all x coordinates found in the fragment as a sequence
 : of integers.
 :)
declare function s:x($svg as node()*) as xs:integer*
{
  for $x in
  (
    $svg//data
    ((
      @x, @x1, @x2, @x + @width,
      for $t in tokenize(@d, " ")[position() mod 2 = 1]
      return translate($t, "MLQ", "")
    ))
  )
  return xs:integer($x)
};

(:~
 : Calculate characteristics of a graphics fragment. This includes
 : the coordinates of the corners and stub, and the type of shapes
 : at the top and bottom.
 :
 : @param $data the graphics fragment.
 : @return an s:dimensions element with attributes
 : y0: the y coordinate of the stub
 : x1, y1: the top left coordinates
 : x2, y2: the bottom right coordinates
 : top: type of shape at the top ("rect" or "line")
 : bottom: type of shape at the bottom ("rect" or "line")
 :)
declare function s:dimensions($data as element()*) as element(s:dimensions)?
{
  if (empty($data)) then
    ()
  else
    let $coordinates :=
      for $node in $data/descendant-or-self::*
      return
      (
        if ($node/@x1 and $node/@y1) then (xs:integer($node/@x1), xs:integer($node/@y1)) else (),
        if ($node/@x2 and $node/@y2) then (xs:integer($node/@x2), xs:integer($node/@y2)) else (),
        if ($node/@x and $node/@y) then
        (
          data(($node/@x, $node/@y)),
          if ($node/@width and $node/@height) then
            (xs:integer($node/@x + $node/@width), xs:integer($node/@y + $node/@height))
          else
            ()
        )
        else
          (),
        if ($node/@cx and $node/@cy) then
        (
          xs:integer($node/@cx           ), xs:integer($node/@cy - $node/@r),
          xs:integer($node/@cx           ), xs:integer($node/@cy + $node/@r),
          xs:integer($node/@cx - $node/@r), xs:integer($node/@cy           ),
          xs:integer($node/@cx + $node/@r), xs:integer($node/@cy           )
        )
        else
          (),
        if (exists($node/(@d, @points))) then
        (
          let $all := tokenize($node/(@d, @points), "[ ,]")
          for $p at $i in $all
          where $i mod 2 = 1
          return (xs:integer(replace($p, "[a-zA-Z]", "")), xs:integer($all[$i + 1]))
        )
        else
          ()
      )
    let $x := for $x at $i in $coordinates where $i mod 2 = 1 return $x
    let $y := for $y at $i in $coordinates where $i mod 2 = 0 return $y
    let $x1 := xs:integer(min($x))
    let $x2 := xs:integer(max($x))
    let $y1 := xs:integer(min($y))
    let $y2 := xs:integer(max($y))
    let $y0 :=
      if (empty($x1)) then
      (
        error(QName("", "empty"), "")
      )
      else
        xs:integer(avg(for $i in index-of($x, $x1) return $y[$i]) + 0.5)
    let $bottom :=
      if ($data/descendant-or-self::line[@y1 = $y2 or @y2 = $y2]) then
        "line"
      else
        "rect"
    let $top :=
      if ($data/descendant-or-self::line[@y1 = $y1 or @y2 = $y1]) then
        "line"
      else
        "rect"
    return <s:dimensions y0="{$y0}"
                  x1="{$x1}" y1="{$y1}"
                  x2="{$x2}" y2="{$y2}"
                  top="{$top}"
                  bottom="{$bottom}"
                  width="{$x2 - $x1}"
                  height="{$y2 - $y1}"/>
};

(:~
 : Draw a rectangle, possibly a shadow, some text, and stubs. For
 : nonterminals or links, create an internal or external hyperlink.
 :
 : @param $x the leftmost x coordinate of the resulting graphics.
 : @param $y the topmost y coordinate of the resulting graphics.
 : @param $text the text to be drawn inside the rectangle.
 : @param $style the font-weight option.
 : @param $class the (CSS) class of the rectangle.
 : @return the graphics fragment.
 :)
declare function s:rect($x, $y, $text, $style, $class)
{
  let $text-width := s:text-width(string-join($text, ""), $style) idiv 2 * 2
                   + $s:padding * 2
                   + (if ($class = "regexp") then 10 else 0)
  let $yt := $y + $s:rect-height + $s:font-size idiv 3
  let $text-box :=
  (
    if ($class = "regexp") then
      let $x0 := $x + $s:stub-width
      return
      (
        if ($s:shadow) then
          <polygon points="{$x0 + 2                  },{$y + 2 +     $s:rect-height
                         } {$x0 + 2 + 7              },{$y + 2
                         } {$x0 + 2 - 7 + $text-width},{$y + 2
                         } {$x0 + 2     + $text-width},{$y + 2 +     $s:rect-height
                         } {$x0 + 2 - 7 + $text-width},{$y + 2 + 2 * $s:rect-height
                         } {$x0 + 2 + 7              },{$y + 2 + 2 * $s:rect-height}"/>
        else
          (),
        <polygon points="{$x0                  },{$y +     $s:rect-height
                       } {$x0 + 7              },{$y
                       } {$x0 - 7 + $text-width},{$y
                       } {$x0     + $text-width},{$y +     $s:rect-height
                       } {$x0 - 7 + $text-width},{$y + 2 * $s:rect-height
                       } {$x0 + 7              },{$y + 2 * $s:rect-height}" class="{$class}"/>,
        let $text-node :=
          <text class="{$class}" x="{$x + $s:padding + 5 + $s:stub-width}" y="{$yt}">
          {
            for $string at $i in $text
            return
              if ($string instance of element(xhtml:a)) then
                element a
                {
                  attribute xlink:href {$string/@href},
                  attribute xlink:title {$string},
                  attribute target {"_blank"}[not(starts-with($string/@href, "#"))],
                  data($string)
                }
              else if ($string instance of element(xhtml:u)) then
                let $x1 := $x + 15 + $s:stub-width + s:text-width(string-join(subsequence($text, 1, $i - 1), ""), $style)
                let $x2 := $x1 + s:text-width($string, $style)
                return
                (
                  <line class="thin-line" x1="{$x1}" y1="{$yt + 2}" x2="{$x2}" y2="{$yt + 2}"/>,
                  text {$string}
                )
              else
                text {$string}
          }
          </text>
        return (s:link-text($text-node, $style), $text-node//line)
      )
    else
    (
      if ($s:shadow) then
        <rect x="{$x + 2 + $s:stub-width}" y="{$y + 2}" width="{$text-width}" height="{2 * $s:rect-height}">
        {
          attribute rx {$s:padding} [$class = "terminal"]
        }
        </rect>
      else
        (),
      <rect x="{$x + $s:stub-width}" y="{$y}" width="{$text-width}" height="{2 * $s:rect-height}" class="{$class}">
      {
        attribute rx {$s:padding} [$class = "terminal"]
      }
      </rect>,
      <text class="{$class}" x="{$x + $s:padding + $s:stub-width}" y="{$yt}">{string($text)}</text>,
      let $underline :=
        if (not($text instance of element(g:string))) then
          0
        else if (empty($text/@underline)) then
          0
        else
          xs:integer($text/@underline)
      where $underline ne 0
      return
        <line class="bold-line"
              x1="{$x + 10 + $s:stub-width + 1}" y1="{$yt + 3}"
              x2="{$x + 10 + $s:stub-width + s:text-width(substring($text, 1, $underline), $style)}" y2="{$yt + 3}"/>
    )
  )
  return
  (
    if ($class = "nonterminal") then
      <a xlink:href="#{$text}" xlink:title="{$text}">{$text-box}</a>
    else
      $text-box,
    <line x1="{$x}" y1="{$y + $s:rect-height}" x2="{$x + $s:stub-width}" y2="{$y + $s:rect-height}" class="line"/>,
    <line x1="{$x + $text-width + $s:stub-width}" y1="{$y + $s:rect-height}" x2="{$x + $text-width + 2 * $s:stub-width}" y2="{$y + $s:rect-height}" class="line"/>
  )
};

(:~
 : Invert nesting of svg:text and svg:a elements. The calling code creates
 : svg:text elements with nested svg:a. However this appears to be unsupported
 : by libsvg. So we invert the nesting here, such that the content of an
 : svg:a is wrapped in a nested svg:text, and text nodes outside of svg:a
 : get extra svg:text wrapping. Unfortunately we need to recalculate offsets
 : to accomplish.
 :
 : @param $nodes the input node sequence.
 : @param $style the width-by-char array.
 : @return a sequence of svg:text and svg:a elements.
 :)
declare function s:link-text($nodes as node()*, $style) as node()*
{
  for $node in $nodes
  return
    typeswitch ($node)
    case element(line) return
      ()
    case element(text) return
      s:link-text($node/node(), $style)
    case element(a) return
      element a
      {
        $node/@*,
        let $text := $node/ancestor::text[1]
        let $x := $text/@x + s:text-width(string-join($text//text()[. << $node], ""), $style)
        let $y := $text/@y
        return
        (
          element rect
          {
            attribute class {"text"},
            attribute x {$x},
            attribute y {$y + 1 - $s:font-size},
            attribute width {s:text-width(string-join($node//text(), ""), $style) - 1},
            attribute height {$s:font-size + 2}
          },
          s:link-text($node/node(), $style)
        )
      }
    case text() return
      let $space := replace($node, "[^ &#xA0;].*$", "", "s")
      let $no-space := substring($node, string-length($space) + 1)
      let $text := $node/ancestor::text[1]
      let $class := $text/@class
      let $x := $text/@x + s:text-width(string-join(($text//text()[. << $node], $space), ""), $style)
      let $y := $text/@y
      return <text class="{$class}" x="{$x}" y="{$y}">{$no-space}</text>
    default return
      error(xs:QName("s:link-text"), "unsupported node")
};

(:~
 : Connect a graphics fragment to its future environment, while it is being
 : combined vertically with other fragments.
 :
 : @param $subtree the graphics fragment.
 : @param $offset
 : @param $baseline true, if the element is the first in a series.
 : @return the graphics fragment, with connector lines.
 :)
declare function s:connect($subtree as element()*,
                           $offset,
                           $baseline as xs:boolean) as element()*
{
  let $dim := s:dimensions($subtree)
  let $x1 := $dim/@x1 - 20
  let $x2 := $dim/@x2 + 10
  let $y1 := $dim/@y0
  return
    if ($offset < 0) then
      let $y2 := $dim/@y1 + $offset
      return
      (
        $subtree,
        if ($baseline) then
        (
          (: right, skip back 1px, left + up, up, up + right :)
          <path d="M{$x1} {$y1} L{$x1 + 20} {$y1} M{$x1 + 19} {$y1} Q{$x1 + 10} {$y1} {$x1 + 10} {$y1 - 10} L{$x1 + 10} {$y2 + 10} Q{$x1 + 10} {$y2} {$x1 + 20} {$y2}" class="line"/>,
          (: right, skip back 20px, right + up, up, up + left :)
          <path d="M{$x2 - 10} {$y1} L{$x2 + 10} {$y1} M{$x2 - 10} {$y1} Q{$x2} {$y1} {$x2} {$y1 - 10} L{$x2} {$y2 + 10} Q{$x2} {$y2} {$x2 - 10} {$y2}" class="line"/>
        )
        else
        (
          (: up, up + right :)
          <path d="M{$x1 + 10} {$y1 + 9} L{$x1 + 10} {$y2 + 10} Q{$x1 + 10} {$y2} {$x1 + 20} {$y2}" class="line"/>,
          (: up, up + left :)
          <path d="M{$x2} {$y1 + 9} L{$x2} {$y2 + 10} Q{$x2} {$y2} {$x2 - 10} {$y2}" class="line"/>
        )
      )
    else
      let $y2 := $dim/@y2 + $offset
      return
      (
        $subtree,
        if ($baseline) then
        (
          <line x1="{$x1}" y1="{$y1}" x2="{$x1 + 20}" y2="{$y1}" class="line"/>,
          <line x1="{$x2 - 10}" y1="{$y1}" x2="{$x2 + 10}" y2="{$y1}" class="line"/>,
          <path d="M{$x1} {$y1} Q{$x1 + 10} {$y1} {$x1 + 10} {$y1 + 10}" class="line"/>,
          <path d="M{$x2} {$y1 + 10} Q{$x2} {$y1} {$x2 + 10} {$y1}" class="line"/>
        )
        else
        (
          <line x1="{$x1 + 10}" y1="{$y1 - 10}" x2="{$x1 + 10}" y2="{$y1 + 10}" class="line"/>,
          <line x1="{$x2}" y1="{$y1 + 10}" x2="{$x2}" y2="{$y1 - 10}" class="line"/>
        ),
        if (abs($y1 - $y2) > 20) then
        (
          <line x1="{$x1 + 10}" y1="{$y1 + 10}" x2="{$x1 + 10}" y2="{$y2 - 10}" class="line"/>,
          <line x1="{$x2}" y1="{$y2 - 10}" x2="{$x2}" y2="{$y1 + 10}" class="line"/>
        )
        else
          (),
        <path d="M{$x1 + 10} {$y2 - 10} Q{$x1 + 10} {$y2} {$x1 + 20} {$y2}" class="line"/>,
        <path d="M{$x2 - 10} {$y2} Q{$x2} {$y2} {$x2} {$y2 - 10}" class="line"/>
      )
};

(:~
 : SVG node rendering dispatcher function. Apply rendering strategy as
 : appropriate for the type of the given node. For grammar constructs that
 : do not have a syntax diagram mapping (e.g. any kind of negation),
 : denormalize and create a terminal box containing the corresponding
 : EBNF in italics.
 :
 : @param $x the x coordinate for placing the result.
 : @param $y the x coordinate for placing the result.
 : @param $node the grammar node to be rendered
 : @return the resulting graphics.
 :)
declare function s:render-node($x, $y, $node as node()) as element()*
{
  typeswitch ($node)
  case element(g:ref) return
    if ($node/@name = ".") then
      s:rect($x, $y, ".", "normal", "regexp")
    else
      s:rect($x, $y, normalize-space($node/@name), "normal", "nonterminal")
  case element(g:string) return
    s:rect($x, $y, $node, "bold", "terminal")
  case element(g:sequence) return
    s:render-horizontal($x, $y, (), $node/*)
  case element(g:choice) return
    s:render-vertical
    (
      $x, $y, +1,
      let $baseline-case :=
      (
(::)
        $node/*[descendant-or-self::g:oneOrMoreWithSeparator[*[1]/self::g:sequence[empty(*)]]],
        $node/g:sequence[empty((*, $node/*[1]/descendant-or-self::g:oneOrMoreWithSeparator))]
(::)
(:?
        $node/*[descendant-or-self::g:oneOrMoreWithSeparator[*[1]/self::g:sequence[empty(*)]]],
        $node/g:sequence[empty(*)]
?:)
      )[1]
      return ($baseline-case, $node/*[not(. is $baseline-case)])
    )
  case element(g:oneOrMoreWithSeparator) return
    s:render-vertical
    (
      $x, $y, -1,
      (
        $node/*[1],
        let $separator := n:reverse($node/*[2])
        return
          if ($separator/self::g:choice) then
            $separator/*
          else
            $separator
      )
    )
  case element(g:charClass) return
    if ($node/*[2]) then
      s:render-vertical($x, $y, +1, $node/*)
    else
      s:render-node($x, $y, $node/*)
  case element(g:char) return
    s:rect($x, $y, data($node), "bold", "terminal")
  case element(g:charRange) return
    s:rect($x, $y, concat("[", $node/@minChar, "-", $node/@maxChar, "]"), "normal", "regexp")
  case element(g:charCode) return
    s:rect($x, $y, concat("[#x", data($node/@value), "]"), "normal", "regexp")
  case element(g:charCodeRange) return
    s:rect($x, $y, concat("[#x", $node/@minValue, "-#x", $node/@maxValue, "]"), "normal", "regexp")
  case attribute(xhref) return
    s:rect($x, $y, b:render-as-html($node, namespace-uri(<xhtml:a/>)), "normal", "regexp")
  default return
    s:rect($x, $y, b:render-as-html(n:strip-pi(n:denormalize($node)), namespace-uri(<xhtml:a/>)), "normal", "regexp")
};

(:~
 : Recursively render a sequence of grammar nodes, combining them
 : horizontally. This is done (tail-) recursively, one grammar item
 : at a time, because the placement of each result is to the right
 : of any results of preceding items.
 :
 : @param $x the x coordinate for placing the result.
 : @param $y the x coordinate for placing the result.
 : @param $done the graphics created in previous recursion levels.
 : @param $todo the grammar nodes still to be done.
 : @return the resulting graphics.
 :)
declare function s:render-horizontal($x,
                                     $y,
                                     $done as node()*,
                                     $todo as node()*) as node()*
{
  if (exists($todo)) then
    if ($todo[1]/self::processing-instruction()) then
      s:render-horizontal($x, $y, ($done, $todo[1]), $todo[position() > 1])
    else
      let $rendered := s:render-node($x, $y, $todo[1])
      let $done := ($done, <g>{$rendered}</g>)
      return s:render-horizontal(max(s:x($rendered)), $y, $done, $todo[position() > 1])
  else if (exists($done)) then
    $done
  else
    <g><line x1="{$x}"      y1="{$y + $s:padding + $s:font-size idiv 2}"
             x2="{$x + 10}" y2="{$y + $s:padding + $s:font-size idiv 2}" class="line"/></g>
};

(:~
 : Recursively align a sequence of graphics, combining them
 : vertically. This is done (tail-) recursively, one grammar item
 : at a time, because the placement of each result is below or
 : above any results of preceding items.
 :
 : @param $x the x coordinate for placing the result.
 : @param $y the x coordinate for placing the result.
 : @param $y-direction +1, if aligning downward, -1 when aligning upward.
 : @param $max-width precalculated maximum width of alternatives.
 : @param $done the graphics created in previous recursion levels.
 : @param $todo the grammar nodes still to be done.
 : @return the resulting graphics.
 :)
declare function s:render-vertical-pass2($x,
                                         $y,
                                         $y-direction as xs:integer,
                                         $max-width,
                                         $done as element()*,
                                         $todo as element()*) as element()*
{
  if (empty($todo)) then
    ()
  else
    let $connector-width := 20
    let $rendered := $todo[1]/*
    let $dim := s:dimensions($rendered)
    let $extended :=
      let $width := $dim/@width
      let $extension-length := $max-width - $width
      let $ext1 := if ($s:center-choice) then $extension-length idiv 2 else 0
      let $ext2 := $extension-length - $ext1
      return
      (
        let $translate-y := if (empty($done)) then xs:integer($y) else xs:integer($y - $dim/@y1)
        return s:translate($x + $connector-width + $ext1, $translate-y, $rendered),
        let $ext-y := if (empty($done)) then $y + $s:rect-height else xs:integer($y + $dim/@y0 - $dim/@y1)
        let $connectors :=
        (
          <line x1="{$x + $connector-width}"         y1="{$ext-y}"
                x2="{$x + $connector-width + $ext1}" y2="{$ext-y}" class="line"/>
          [$ext1 != 0],
          <line x1="{$x + $connector-width + $ext1 + $width}"         y1="{$ext-y}"
                x2="{$x + $connector-width + $ext1 + $width + $ext2}" y2="{$ext-y}" class="line"/>
          [$ext2 != 0]
        )
        return $connectors
      )
    let $next := $todo[2]
    return
      if (empty($next)) then
        ($done, $extended)
      else
        let $next-dim := s:dimensions($next)

        let $y2 :=
          if (empty($done)) then
            if ($y-direction >= 0) then $dim/@y2 else $dim/@y1
          else
            if ($y-direction >= 0) then $y + $dim/@height else $y

        let $offset :=
          if ($y-direction >= 0) then
            if ($dim/@bottom = "rect" and $next-dim/@top = "rect") then 10 else $s:loop-offset
          else
            if ($dim/@top = "rect" and $next-dim/@bottom = "rect") then 10 else $s:loop-offset

        let $next-y :=
          if ($y-direction >= 0) then
            $y2 + $offset
          else
            $y2 - $offset - $next-dim/@height

        let $connected :=
          let $next-y0-offset :=
            if ($y-direction >= 0) then
              $next-dim/@y0 - $next-dim/@y1 + $offset
            else
              $next-dim/@y0 - $next-dim/@y2 - $offset
          return s:connect($extended, $next-y0-offset, empty($done))

        return
          s:render-vertical-pass2($x, $next-y, $y-direction, $max-width, ($done, $connected), ($todo[position() > 1]))
};

(:~
 : Render a sequence of grammar items, combining them vertically. The
 : items are rendered individually first, then the sequence of results
 : is combined in a second pass. We need to do it in two passes, because
 : we need the maximum item width before starting to combine.
 :
 : @param $x the x coordinate for placing the result.
 : @param $y the x coordinate for placing the result.
 : @param $y-direction +1, if aligning downward, -1 when aligning upward.
 : @param $todo the grammar nodes to be rendered.
 : @return the resulting graphics.
 :)
declare function s:render-vertical($x,
                                   $y,
                                   $y-direction as xs:integer,
                                   $todo as node()*)
{
  let $rendered := for $t in $todo return element g {s:render-node(0, 0, $t)}
  return s:render-vertical-pass2($x, $y, $y-direction, s:dimensions($rendered)/@width, (), $rendered)
};

(:~
 : Install line breaks by repositioning lines and reconnecting them
 : according to the connection options.
 :
 : @param $max-width the maximum width of all lines.
 : @param $y the y coordinate where to place the next line.
 : @param $done the lines already completed.
 : @param $todo the lines still to be readjusted and reconnected.
 : @return the readjusted, reconnected graphics.
 :)
declare function s:line-break-pass2($max-width, $y, $done, $todo)
{
  if (empty($todo)) then
    $done
  else
    let $next-dim := s:dimensions($todo[1])
    let $x :=
      if (empty($done)) then
        0
      else if (empty($todo[2])) then
        xs:integer($max-width - $next-dim/@width)
      else
        xs:integer(($max-width - $next-dim/@width) div 2)
    let $this := s:translate(xs:integer($x - $next-dim/@x1), xs:integer($y - $next-dim/@y1), $todo[1])
    let $next-y := $y + $next-dim/@height + 2 * $s:line-offset
    return
      s:line-break-pass2
      (
        $max-width,
        $next-y,
        (
          if (empty($done)) then
            ()
          else
            let $dim := s:dimensions($done[last()])
            return
            (
              $done,
              if ($s:line-connect = "line") then
                <path class="line">{
                  attribute d
                  {
                    concat
                    (
                      "M", $dim/@x2, " ", $dim/@y0, " ",
                      "Q", string($dim/@x2 + 10), " ", $dim/@y0, " ",
                           string($dim/@x2 + 10), " ", string($dim/@y0 + 10), " ",
                      "L", string($dim/@x2 + 10), " ", string($dim/@y2 + $s:line-offset - 10), " ",
                      "Q", string($dim/@x2 + 10), " ", string($dim/@y2 + $s:line-offset), " ",
                           string($dim/@x2), " ", string($dim/@y2 + $s:line-offset), " ",
                      "L", $x, " ", string($dim/@y2 + $s:line-offset), " ",
                      "Q", $x - 10, " ", string($dim/@y2 + $s:line-offset), " ",
                           $x - 10, " ", string($dim/@y2 + $s:line-offset + 10), " ",
                      "L", $x - 10, " ", string($y + $next-dim/@y0 - $next-dim/@y1 - 10), " ",
                      "Q", $x - 10, " ", string($y + $next-dim/@y0 - $next-dim/@y1), " ",
                           $x, " ", string($y + $next-dim/@y0 - $next-dim/@y1)
                    )
                  }
                }</path>
              else if ($s:line-connect = "dots") then
                <path class="line">{
                  attribute d
                  {
                    concat
                    (
                      "M", $dim/@x2 + 2, " ", $dim/@y0, " ",
                      "L", $dim/@x2 + 4, " ", $dim/@y0, " ",
                      "M", $dim/@x2 + 6, " ", $dim/@y0, " ",
                      "L", $dim/@x2 + 8, " ", $dim/@y0, " ",
                      "M", $dim/@x2 + 10, " ", $dim/@y0, " ",
                      "L", $dim/@x2 + 12, " ", $dim/@y0, " ",
                      "M", $x - 12, " ", string($y + $next-dim/@y0 - $next-dim/@y1), " ",
                      "L", $x - 10, " ", string($y + $next-dim/@y0 - $next-dim/@y1), " ",
                      "M", $x - 8, " ", string($y + $next-dim/@y0 - $next-dim/@y1), " ",
                      "L", $x - 6, " ", string($y + $next-dim/@y0 - $next-dim/@y1), " ",
                      "M", $x - 4, " ", string($y + $next-dim/@y0 - $next-dim/@y1), " ",
                      "L", $x - 2, " ", string($y + $next-dim/@y0 - $next-dim/@y1)
                    )
                  }
                }</path>
              else
                ()
            ),
            <g>{$this}</g>
        ),
        $todo[position() > 1]
      )
};

(:~
 : Attempt to break a rendered graphic result into multiple lines, if it
 : exceeds the desired maximum width. In a first (recursive) pass, calculate
 : width and identify lines based on width criteria. Then proceed to the
 : splitting and reconnecting the lines.
 :
 : @param $page-width where to break for a new line, in pixels.
 : @param $width the running width.
 : @param $max-width the running maximum width.
 : @param $done the results already proecessed in previous recursion levels.
 : @param $line the current line, as it is being assembled.
 : @param $todo the results yet to be processed.
 : @return the graphics, broken into multiple lines, if necessary and possible.
 :)
declare function s:line-break($page-width, $width, $max-width, $done, $line, $todo)
{
  if (empty($todo)) then
    s:line-break-pass2
    (
      max(($width + 10, $max-width)),
      0,
      (),
      ($done, <g>{$line}</g>[exists($line)])
    )
  else
    let $connector-width :=
      if ($s:line-connect = "dots") then
        12
      else if ($s:line-connect = "line") then
        10
      else
        0
    let $connectors-width :=
      if (empty($done)) then
        $connector-width
      else
        2 * $connector-width
    return
      if ($todo[1]/self::processing-instruction()[local-name() = "rr" and normalize-space(.) = "br"]) then
        s:line-break
        (
          $page-width,
          0,
          max(($width + $connectors-width, $max-width)),
          ($done, <g>{$line}</g>),
          (),
          $todo[position() > 1]
        )
      else if ($todo[1]/self::processing-instruction()) then
        s:line-break($page-width, $width, $max-width, $done, $line, $todo[position() > 1])
      else
        let $next-dim := s:dimensions($todo[1])
        return
          if (   $width + $next-dim/@width + $connectors-width <= $page-width
              or empty($line)) then
            s:line-break
            (
              $page-width,
              $width + $next-dim/@width,
              $max-width,
              $done,
              ($line, $todo[1]),
              $todo[position() > 1]
            )
          else
            s:line-break
            (
              $page-width,
              0,
              max(($width + $connectors-width, $max-width)),
              ($done, <g>{$line}</g>),
              (),
              $todo
            )
};

(:~
 : Render a single production to SVG graphics elements.
 :
 : @param $p the production node.
 : @return the corresponding graphics.
 :)
declare function s:render-production($p as element(g:production)) as node()*
{
  let $nodes := ($p/@xhref, $p/(*, processing-instruction()))
  return
    if ($s:begin-end = "circle") then
      let $rendered := s:render-horizontal(8, 0, (), $nodes)
      let $dim := s:dimensions($rendered/self::*)
      let $y := data($dim/@y0)
      let $x := data($dim/@x2) + 8
      let $count := count($rendered)
      for $r at $i in $rendered
      return
        if ($i = (1, $count)) then
          <g>{
            (
              <circle cx="4" cy="{$y}" r="3" class="filled"/>,
              <line x1="3" y1="{$y}" x2="8" y2="{$y}" class="line"/>
            )[$i = 1],
            $r/*,
            (
              <line x1="{$x - 3}" y1="{$y}" x2="{$x - 8}" y2="{$y}" class="line"/>,
              <circle cx="{$x - 4}" cy="{$y}" r="3" class="filled"/>
            )[$i = $count]
          }</g>
        else
          $r
    else if ($s:begin-end = "arrow") then
      let $rendered := s:render-horizontal(20, 0, (), $nodes)
      let $dim := s:dimensions($rendered/self::*)
      let $y := data($dim/@y0)
      let $x := data($dim/@x2) + 20
      let $count := count($rendered)
      for $r at $i in $rendered
      return
        if ($i = (1, $count)) then
          <g>{
            (
              <polygon points="10,{$y} 2,{$y - 4} 2,{$y + 4}"/>,
              <polygon points="18,{$y} 10,{$y - 4} 10,{$y + 4}"/>,
              <line x1="18" y1="{$y}" x2="20" y2="{$y}" class="line"/>
            )[$i = 1],
            $r/*,
            (
              <line x1="{$x - 17}" y1="{$y}" x2="{$x - 20}" y2="{$y}" class="line"/>,
              <polygon points="{$x - 10},{$y} {$x - 2},{$y - 4} {$x - 2},{$y + 4}"/>,
              <polygon points="{$x - 10},{$y} {$x - 18},{$y - 4} {$x - 18},{$y + 4}"/>
            )[$i = $count]
          }</g>
        else
          $r
    else
      s:render-horizontal(20, 0, (), $nodes)
};

(:~
 : Return an SVG element for being include in the XHTML head, containing
 : the SVG defs for the graphics to come.
 :
 : @param $color the base color code.
 : @return the svg element containing the defs.
 :)
declare function s:defs($color as xs:string)
{
  <svg xmlns="http://www.w3.org/2000/svg"><defs>{s:style($color, 0)}</defs></svg>
};

(:~
 : Convert a single production to SVG graphics:
 :
 : <ul xmlns="http://www.w3.org/1999/xhtml">
 :   <li>normalize the production,</li>
 :   <li>call the renderer,</li>
 :   <li>attempt to break into multiple lines, if exceeding the desired maximum width,</li>
 :   <li>reposition the top left corner of the resulting graphics to coordinates
 :       x=1, y=1, and</li>
 :   <li>wrap the result in an svg:svg element containing
 : width and height attributes.</li>
 : </ul>
 :
 : @param $p the production node.
 : @param $page-width where to break for a new line, in pixels.
 : @param $color the base color code.
 : @param $spread the hue offset.
 : @return the corresponding graphics element.
 :)
declare function s:convert-to-svg($p as element(g:production), $page-width as xs:integer, $color as xs:string, $spread as xs:integer, $styles as xs:boolean) as element(svg:svg)
{
  let $normalized := n:normalize($p)
  let $rendered := s:line-break($page-width, 0, 0, (), (), s:render-production(n:introduce-separators($normalized)))
  let $dimensions := s:dimensions($rendered)
  let $width := $dimensions/@width + 2
  let $height := $dimensions/@height + 2
  return
    <svg xmlns="http://www.w3.org/2000/svg"
         xmlns:xlink="http://www.w3.org/1999/xlink"
         width="{$width + 1}" height="{$height + 1}">
      {
        if ($styles) then <defs>{s:style($color, $spread)}</defs> else (),
        s:translate(1 - xs:integer($dimensions/@x1), 1 - xs:integer($dimensions/@y1), $rendered)
      }
    </svg>
};

(:~
 : Calculate x adjustment indicated by any translate operations
 : applicable to the given node. Recursively search ancestors for
 : any translations, accumulating x adjustment values.
 :
 : @param $context the node to start from.
 : @return the total x adjustment applicable to the given node.
 :)
declare function s:x-adjustment($context as node()?) as xs:integer
{
  if (empty($context)) then
    0
  else
    let $transform := $context/@transform
    return
      if (empty($transform)) then
        s:x-adjustment($context/..)
      else
        let $x := substring-before(substring-after($transform, "translate("), ")")
        let $x := if (contains($x, ",")) then substring-before($x, ",") else $x
        return xs:integer($x) + s:x-adjustment($context/..)
};

(:~
 : Calculate y adjustment indicated by any translate operations
 : applicable to the given node. Recursively search ancestors for
 : any translations, accumulating y adjustment values.
 :
 : @param $context the node to start from.
 : @return the total x adjustment applicable to the given node.
 :)
declare function s:y-adjustment($context as node()?) as xs:integer
{
  if (empty($context)) then
    0
  else
    let $transform := $context/@transform
    return
      if (empty($transform)) then
        s:y-adjustment($context/..)
      else
        let $y := substring-before(substring-after($transform, "translate("), ")")
        let $y := if (contains($y, ",")) then substring-after($y, ",") else "0"
        return xs:integer($y) + s:y-adjustment($context/..)
};

(:~
 : Adjust an x value by any translations implied by given context
 : node.
 :
 : @param $x the x value as a string.
 : @param $context the context node.
 : @return the adjusted x value.
 :)
declare function s:adjust-x($x as xs:string, $context as node()) as xs:integer
{
  xs:integer($x) + s:x-adjustment($context)
};

(:~
 : Adjust an y value by any translations implied by given context
 : node.
 :
 : @param $y the y value as a string.
 : @param $context the context node.
 : @return the adjusted y value.
 :)
declare function s:adjust-y($y as xs:string, $context as node()) as xs:integer
{
  xs:integer($y) + s:y-adjustment($context)
};

(:~
 : Apply SVG translate operations to a single attribute node. For
 : attributes containing x or y values, any translations implied by
 : the attribute's context are applied. "transform" attributes are
 : dropped, as their very effect is applied during the translation
 : performed here.
 :
 : @param $a the attribute to be translated.
 : @return the translated attribute node, or empty, if the attribute
 : became obsolete.
 :)
declare function s:translate-attribute($a as attribute()) as attribute()?
{
  if (node-name($a) = QName("", "transform")) then
    ()
  else if (node-name($a) = (QName("", "width"),
                            QName("", "height"))) then
    attribute {node-name($a)} {xs:integer($a)}
  else if (node-name($a) = (QName("", "x"),
                            QName("", "cx"),
                            QName("", "x1"),
                            QName("", "x2"))) then
    attribute {node-name($a)} {s:adjust-x(data($a), $a)}
  else if (node-name($a) = (QName("", "y"),
                            QName("", "cy"),
                            QName("", "y1"),
                            QName("", "y2"))) then
    attribute {node-name($a)} {s:adjust-y(data($a), $a)}
  else if (node-name($a) = (QName("", "d"),
                            QName("", "points"))) then
    attribute {node-name($a)}
    {
      string-join
      (
        let $tokens := tokenize($a, "[ ,]")
        for $x at $i in $tokens
        where ($i mod 2) = 1
        return
          let $prefix := if (matches($x, "^\p{Nd}")) then "" else substring($x, 1, 1)
          let $x := substring-after($x, $prefix)
          let $y := $tokens[$i + 1]
          return concat($prefix, s:adjust-x($x, $a), " ", s:adjust-y($y, $a)),
        " "
      )
    }
  else
    $a
};

(:~
 : Apply SVG translate operations to a graphics fragment. Recursively
 : visit nodes of the fragment, invoking s:translate-attribute to perform
 : the actual translation. As a side effect, drop any text nodes containing
 : nothing but whitespace, and equip svg:text nodes not containing x or y
 : coordinate values with appropriate attributes.
 :
 : @param $nodes the graphics fragment.
 : @return the translated graphics fragment. Any "transform" attributes have
 : been dropped, because they have become obsolete.
 :)
declare function s:translate($nodes as node()*) as node()*
{
  for $node in $nodes
  return
    typeswitch ($node)
    case document-node() return
      s:translate($node/node())
    case element(svg:g) return
      s:translate($node/node())
    case element() return
      element {node-name($node)}
      {
        if ($node/self::svg:text) then
        (
          if (exists($node/@x)) then () else attribute x {s:x-adjustment($node)},
          if (exists($node/@y)) then () else attribute y {s:y-adjustment($node)}
        )
        else
          (),
        s:translate($node/@*),
        s:translate($node/node())
      }
    case attribute() return
      s:translate-attribute($node)
    case text() return
      $node[normalize-space(.) != ""]
    default return
      $node
};

(:~
 : Reposition a graphics fragment by given x and y displacement values. The
 : effect is achieved by wrapping the fragment in an svg:g element with
 : a corresponding translate transformation, and subsequently evaluating this
 : translation by rewriting the fragment. Used for positioning components of
 : a bigger graphic, keeping absolute coordinates.
 :
 : @param $x the x displacement value.
 : @param $y the y displacement value.
 : @param $svg the graphics fragment.
 : @return the translated graphics fragment.
 :)
declare function s:translate($x as xs:integer, $y as xs:integer, $svg as element()*) as element()*
{
  if ($x = 0 and $y = 0) then
    $svg
  else
    s:translate(<g transform="translate({$x}, {$y})">{$svg}</g>)
};

(:~
 : Draw circular "railroad crossing" traffic sign, which is used
 : for this application's logo.
 :
 : @param $diameter the diameter in pixels of the traffic sign.
 : @return the SVG graphics making up the traffic sign.
 :)
declare function s:traffic-sign($diameter as xs:integer) as element(svg:svg)
{
  let $dark := c:rgb(c:hsl-to-rgb(c:rgb-to-hsl($s:logo-color)[1], 1.0, 0.1))
  return
    <svg xmlns="http://www.w3.org/2000/svg" width="{$diameter}" height="{$diameter}">
      <g transform="scale({round($diameter div 90 * 1000) div 1000})">
        <circle cx="45" cy="45" r="45" style="stroke:none; fill:{$s:logo-color}"/>
        <circle cx="45" cy="45" r="42" style="stroke:{$dark}; stroke-width:2px; fill:{$s:logo-color}"/>
        <line x1="15" y1="15" x2="75" y2="75" stroke="{$dark}" style="stroke-width:9px;"/>
        <line x1="15" y1="75" x2="75" y2="15" stroke="{$dark}" style="stroke-width:9px;"/>
        <text x="7" y="54" style="font-size:26px; font-family:Arial, Sans-serif; font-weight:bold; fill: {$dark}">R</text>
        <text x="64" y="54" style="font-size:26px; font-family:Arial, Sans-serif; font-weight:bold; fill: {$dark}">R</text>
      </g>
    </svg>
};

(:~
 :
 :)
declare function s:combine-paths($nodes)
{
  for $node in $nodes
  return
    if ($node/@class = "line") then
      s:combine-path($node)
    else
      typeswitch ($node)
      case element() return
        element {node-name($node)} {$node/@*, s:combine-paths($node/node())}
      case document-node() return
        document {s:combine-paths($node/node())}
      default return
        $node
};

(:~
 :
 :)
declare function s:combine-path($node)
{
  if (not($node/@class = "line")) then
    $node
  else
    let $paths := $node/ancestor::svg:svg[1]//*[@class = "line"]
    return
      if (not($node is $paths[last()])) then
        ()
      else
        element svg:path
        {
          $node/@class,
          attribute d
          {
            s:relativize-path
            (
              string-join
              (
                for $node in $paths
                return
                  if ($node/self::svg:path) then
                    $node/@d
                  else
                  (
                    concat("M", string($node/@x1)), $node/@y1,
                    if ($node/@x1 eq $node/@x2) then
                      concat("v", string($node/@y2 - $node/@y1))
                    else if ($node/@y1 eq $node/@y2) then
                      concat("h", string($node/@x2 - $node/@x1))
                    else
                      (concat("l", string($node/@x2 - $node/@x1)), string($node/@y2 - $node/@y1))
                  ),
                " "
              )
            )
          }
        }
};

(:~
 :
 :)
declare function s:relativize-path($done, $todo, $x, $y)
{
  if (empty($todo)) then
    $done
  else
    let $c := $todo[1]
    let $type := $c/@type
    let $a := for $i in tokenize($c/@args, "\s+") return xs:integer($i)
    let $todo := subsequence($todo, 2)
    return
      if ($type = "M") then
        s:relativize-path(($done, <command type="m" args="{$a[1] - $x, $a[2] - $y}"/>), $todo, $a[1],  $a[2])
      else if ($type = "m") then
        s:relativize-path(($done, $c), $todo, $a[1],  $a[2])
      else if ($type = "L") then
        s:relativize-path(($done, <command type="l" args="{$a[1] - $x, $a[2] - $y}"/>), $todo, $a[1],  $a[2])
      else if ($type = "l") then
        s:relativize-path(($done, $c), $todo, $x + $a[1],  $y + $a[2])
      else if ($type = "h") then
        s:relativize-path(($done, $c), $todo, $x + $a[1],  $y)
      else if ($type = "v") then
        s:relativize-path(($done, $c), $todo, $x,  $y + $a[1])
      else if ($type = "Q") then
        s:relativize-path(($done, <command type="q" args="{$a[1] - $x, $a[2] - $y, $a[3] - $x, $a[4] - $y}"/>), $todo, $a[3],  $a[4])
      else if ($type = "q") then
        s:relativize-path(($done, $c), $todo, $x + $a[3],  $y + $a[4])
      else
        error(node-name($c), concat(local-name($c), " - ", $type))
};

(:~
 :
 :)
declare function s:relativize-path($path)
{
  let $commands := for $t in tokenize($path, "[^A-Za-z]+") return normalize-space($t)[.]
  let $args := for $t in tokenize($path, "[A-Za-z]+") return normalize-space($t)[.]
  let $relative-commands :=
    s:relativize-path((), for $c at $i in $commands return <command type="{$c}" args="{$args[$i]}"/>, 0, 0)
  return string-join(for $c in $relative-commands return concat($c/@type, $c/@args), " ")
};

(:~
 :
 :)
declare function s:process-annotations($nodes as node()*) as node()*
{
  for $node in $nodes
  return
    typeswitch($node)
    case document-node() return
      document {s:process-annotations($node/node())}
    case element(g:orderedChoice) return
      element g:choice
      {
        $node/@*,
        s:process-annotations($node/node())
      }
    case element(g:string) return
      let $pi :=
        $node/following-sibling::processing-instruction()
        [local-name() = "rr" and preceding-sibling::*[1] is $node]
      let $underline := ($pi[matches(., "^\s*u(=-?[0-9]+)?\s*$")])[last()]
      return
        if (empty($underline)) then
          $node
        else
          let $width := normalize-space(substring-after($underline, "="))[.]
          return
            element g:string
            {
              $node/@*,
              attribute underline
              {
                if (empty($width)) then
                  string-length($node)
                else if (starts-with($width, "-")) then
                  string-length($node) + xs:integer($width)
                else
                  $width
              },
              string($node)
            }
    case element() return
      element {node-name($node)}
      {
        $node/@*,
        s:process-annotations($node/node())
      }
    default return
      $node
};

(:~
 : Construct standard xhtml head entries.
 :
 : @param $color the color code.
 : @return the list of standard xhtml head entries.
 :)
declare function s:head($color as xs:string, $page-width as xs:integer?, $styles as xs:boolean) as element()+
{
  <meta http-equiv="Content-Type" content="application/xhtml+xml" xmlns="http://www.w3.org/1999/xhtml"/>,
  <meta name="generator" content="Railroad Diagram Generator {$s:version}" xmlns="http://www.w3.org/1999/xhtml"/>,
  if ($styles) then
    (
      style:css($color, $page-width),
      s:defs($color)
    )
  else
    ()
};

(:~
 : Render a complete grammar into an annotated sequence of SVG graphics
 : elements, one for each nonterminal. Each graphic is wrapped in an
 : XHTML paragraph with an anchor node and a list of links to referencing
 : nonterminals.
 :
 : @param $grammar the grammar.
 : @param $showEbnf whether to show EBNF next to generated diagrams.
 : @param $page-width where to break for a new line, in pixels.
 : @param $color the base color code.
 : @param $spread the hue offset.
 : @param $uri the rr generator link.
 : @return a list of XHTML elements and processing-instructions.
 :)
declare function s:svg($grammar as element(g:grammar), $showEbnf as xs:boolean, $page-width as xs:integer, $color as xs:string, $spread as xs:integer, $styles as xs:boolean, $uri as xs:string) as node()*
{
  let $g := n:group-productions-by-nonterminal($grammar)
  let $productions := $g//g:production
  let $count := count($productions)
  where $count > 0
  return
  (
    $grammar/processing-instruction()[local-name() = "rr"],
    for $production in $productions
    let $p := s:process-annotations($production)
    let $anchor := data($p/@name)
    let $svg := s:combine-paths(s:convert-to-svg($p, $page-width, $color, $spread, $styles))
    let $references :=
      for $ref in $g/g:production[.//g:ref/@name = $anchor]/@name
      order by $ref
      return data($ref)
    return
    (
      <xhtml:p style="font-size: {$s:font-size + 2}px; font-weight:bold"><xhtml:a name="{$anchor}">{$anchor}:</xhtml:a></xhtml:p>,
      $svg,
      if ($showEbnf) then
        <xhtml:p><xhtml:div class="ebnf"><xhtml:code>{b:render-as-html(n:strip-pi($p), namespace-uri(<xhtml:a/>))}</xhtml:code></xhtml:div></xhtml:p>
      else
        (),
      <xhtml:p>
        {
          if (empty($references)) then
            "no references"
          else
          (
            "referenced by:",
            <xhtml:ul>{
              for $r in $references
              return <xhtml:li><xhtml:a href="#{$r}" title="{$r}">{$r}</xhtml:a></xhtml:li>
            }</xhtml:ul>
          )
        }
      </xhtml:p>,
      <xhtml:br/>
    ),
    <xhtml:hr/>,
    <xhtml:p>
      <xhtml:table border="0" class="signature">
        <xhtml:tr>
          <xhtml:td style="width: 100%">&#xA0;</xhtml:td>
          <xhtml:td valign="top">
            <xhtml:nobr class="signature">... generated by <xhtml:a name="Railroad-Diagram-Generator" class="signature" title="{$uri}" href="{$uri}" target="_blank">RR - Railroad Diagram Generator</xhtml:a></xhtml:nobr>
          </xhtml:td>
          <xhtml:td><xhtml:a name="Railroad-Diagram-Generator" title="{$uri}" href="{$uri}" target="_blank">{s:traffic-sign(16)}</xhtml:a></xhtml:td>
        </xhtml:tr>
      </xhtml:table>
    </xhtml:p>
  )
};
