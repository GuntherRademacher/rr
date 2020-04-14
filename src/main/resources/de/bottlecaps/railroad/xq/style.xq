(:~
 : The CSS definitions module.
 :)
module namespace style="de/bottlecaps/railroad/xq/style.xq";

import module namespace c="de/bottlecaps/railroad/xq/color.xq" at "color.xq";
import module namespace v="de/bottlecaps/railroad/xq/ast-to-svg.xq" at "ast-to-svg.xq";

declare namespace xhtml="http://www.w3.org/1999/xhtml";

(:~
 : The default hue.
 :)
declare variable $style:default-hue := 48;

(:~
 : The default color code.
 :)
declare variable $style:default-color := c:rgb(c:hsl-to-rgb($style:default-hue, $c:default-saturation, $c:default-lightness));

(:~
 : The background color.
 :
 : @param $color the base color code.
 : @return the rgb color string.
 :)
declare function style:color-background($color as xs:string) {c:relative-color($color, 1.0, 0.97)};

(:~
 : The foreground color.
 :
 : @param $color the base color code.
 : @return the rgb color string.
 :)
declare function style:color-foreground($color as xs:string) {c:relative-color($color, 1.0, 0.03)};

(:~
 : The background color of buttons.
 :
 : @param $color the base color code.
 : @return the rgb color string.
 :)
declare function style:color-bg-button($color as xs:string) {c:relative-color($color, 0.60, 0.86)};

(:~
 : The foreground color of buttons.
 :
 : @param $color the base color code.
 : @return the rgb color string.
 :)
declare function style:color-fg-button($color as xs:string) {c:relative-color($color, 1.0, 0.25)};

(:~
 : The highlited background color.
 :
 : @param $color the base color code.
 : @return the rgb color string.
 :)
declare function style:color-bg-hilite($color as xs:string) {c:relative-color($color, 1.0, 0.91)};

(:~
 : The highlited foreground color.
 :
 : @param $color the base color code.
 : @return the rgb color string.
 :)
declare function style:color-fg-hilite($color as xs:string) {c:relative-color($color, 1.0, 0.01)};

(:~
 : The text editor background color.
 :
 : @param $color the base color code.
 : @return the rgb color string.
 :)
declare function style:color-text($color as xs:string) {c:relative-color($color, 1.0, 0.99)};

(:~
 : Construct standard xhtml:style element for use by all pages.
 :
 : @param $color the base color code.
 : @return the xhtml:style element
 :)
declare function style:css($color as xs:string, $width as xs:integer?) as element(xhtml:style)
{
  <style type="text/css" xmlns="http://www.w3.org/1999/xhtml">
    ::-moz-selection
    {{
      color: {style:color-background($color)};
      background: {style:color-foreground($color)};
    }}
    ::selection
    {{
      color: {style:color-background($color)};
      background: {style:color-foreground($color)};
    }}
    .ebnf a, .grammar a
    {{
      text-decoration: none;
    }}
    .ebnf a:hover, .grammar a:hover
    {{
      color: {style:color-fg-hilite($color)};
      text-decoration: underline;
    }}
    .signature
    {{
      color: {style:color-fg-button($color)};
      font-size: 11px;
      text-align: right;
    }}
    body
    {{
      font: normal 12px Verdana, sans-serif;
      color: {style:color-foreground($color)};
      background: {style:color-background($color)};
    }}
    a:link, a:visited
    {{
      color: {style:color-foreground($color)};
    }}
    a:link.signature, a:visited.signature
    {{
      color: {style:color-fg-button($color)};
    }}
    a.button, #tabs li a
    {{
      padding: 0.25em 0.5em;
      border: 1px solid {style:color-fg-button($color)};
      background: {style:color-bg-button($color)};
      color: {style:color-fg-button($color)};
      text-decoration: none;
      font-weight: bold;
    }}
    a.button:hover, #tabs li a:hover
    {{
      color: {style:color-fg-hilite($color)};
      background: {style:color-bg-hilite($color)};
      border-color: {style:color-fg-hilite($color)};
    }}
    #tabs
    {{
      padding: 3px 10px;
      margin-left: 0;
      margin-top: 58px;
      border-bottom: 1px solid {style:color-foreground($color)};
    }}
    #tabs li
    {{
      list-style: none;
      margin-left: 5px;
      display: inline;
    }}
    #tabs li a
    {{
      border-bottom: 1px solid {style:color-foreground($color)};
    }}
    #tabs li a.active
    {{
      color: {style:color-foreground($color)};
      background: {style:color-background($color)};
      border-color: {style:color-foreground($color)};
      border-bottom: 1px solid {style:color-background($color)};
      outline: none;
    }}
    #divs div
    {{
      display: none;
      overflow:auto;
    }}
    #divs div.active
    {{
      display: block;
    }}
    #text
    {{
      border-color: {style:color-fg-button($color)};
      background: {style:color-text($color)};
      color: {style:color-fg-hilite($color)};
    }}
    .small
    {{
      vertical-align: top;
      text-align: right;
      font-size: 9px;
      font-weight: normal;
      line-height: 120%;
    }}
    td.small
    {{
      padding-top: 0px;
    }}
    .hidden
    {{
      visibility: hidden;
    }}
    td:hover .hidden
    {{
      visibility: visible;
    }}
    div.download
    {{
      display: none;
      background: {style:color-background($color)};
      position: absolute;
      right: 34px;
      top: 94px;
      padding: 10px;
      border: 1px dotted {style:color-foreground($color)};
    }}
    #divs div.ebnf, .ebnf code
    {{
      display: block;
      padding: {$v:padding}px;
      background: {style:color-bg-hilite($color)};
      width: {($width, $v:page-width)[1]}px;
    }}
    #divs div.grammar
    {{
      display: block;
      padding-left: 16px;
      padding-top: 2px;
      padding-bottom: 2px;
      background: {style:color-bg-hilite($color)};
    }}
    pre
    {{
      margin: 0px;
    }}
    .ebnf div
    {{
      padding-left: 13ch;
      text-indent: -13ch;
    }}
    .ebnf code, .grammar code, textarea, pre
    {{
      font:12px SFMono-Regular,Consolas,Liberation Mono,Menlo,Courier,monospace;
    }}
    tr.option-line td:first-child
    {{
      text-align: right
    }}
    tr.option-text td
    {{
      padding-bottom: 10px
    }}
    table.palette
    {{
      border-top: 1px solid {style:color-fg-hilite($color)};
      border-right: 1px solid {style:color-fg-hilite($color)};
      margin-bottom: 4px
    }}
    td.palette
    {{
      border-bottom: 1px solid {style:color-fg-hilite($color)};
      border-left: 1px solid {style:color-fg-hilite($color)};
    }}
    a.palette
    {{
      padding: 2px 3px 2px 10px;
      text-decoration: none;
    }}
    .palette
    {{
      -webkit-user-select: none;
      -khtml-user-select: none;
      -moz-user-select: none;
      -o-user-select: none;
      -ms-user-select: none;
    }}
  </style>
};
