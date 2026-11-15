(:~
 : The CSS definitions module.
 :)
module namespace style="de/bottlecaps/railroad/xq/style.xq";

import module namespace c="de/bottlecaps/railroad/xq/color.xq" at "color.xq";
import module namespace s="de/bottlecaps/railroad/xq/ast-to-svg.xq" at "ast-to-svg.xq";

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
 : @param $mode "light" or "dark" (another value is treated as "light").
 : @return the rgb color string.
 :)
declare function style:color-background($color as xs:string, $mode as xs:string) {c:color($color, "ui-background", 0, $mode)};

(:~
 : The foreground color.
 :
 : @param $color the base color code.
 : @param $mode "light" or "dark" (another value is treated as "light").
 : @return the rgb color string.
 :)
declare function style:color-foreground($color as xs:string, $mode as xs:string) {c:color($color, "ui-foreground", 0, $mode)};

(:~
 : The background color of buttons.
 :
 : @param $color the base color code.
 : @param $mode "light" or "dark" (another value is treated as "light").
 : @return the rgb color string.
 :)
declare function style:color-bg-button($color as xs:string, $mode as xs:string) {c:color($color, "ui-button-background", 0, $mode)};

(:~
 : The foreground color of buttons.
 :
 : @param $color the base color code.
 : @param $mode "light" or "dark" (another value is treated as "light").
 : @return the rgb color string.
 :)
declare function style:color-fg-button($color as xs:string, $mode as xs:string) {c:color($color, "ui-button-foreground", 0, $mode)};

(:~
 : The highlited background color.
 :
 : @param $color the base color code.
 : @param $mode "light" or "dark" (another value is treated as "light").
 : @return the rgb color string.
 :)
declare function style:color-bg-hilite($color as xs:string, $mode as xs:string) {c:color($color, "ui-highlight-background", 0, $mode)};

(:~
 : The highlited foreground color.
 :
 : @param $color the base color code.
 : @param $mode "light" or "dark" (another value is treated as "light").
 : @return the rgb color string.
 :)
declare function style:color-fg-hilite($color as xs:string, $mode as xs:string) {c:color($color, "ui-highlight-foreground", 0, $mode)};

(:~
 : The text editor background color.
 :
 : @param $color the base color code.
 : @param $mode "light" or "dark" (another value is treated as "light").
 : @return the rgb color string.
 :)
declare function style:color-text($color as xs:string, $mode as xs:string) {c:color($color, "ui-editor-background", 0, $mode)};

(:~
 : Creates the CSS rules for a mode. A rule is a string, it may span multiple
 : lines. For mode "auto", these are the rules for light mode, followed by a
 : media query for dark color schemes, that contains those rules for dark
 : mode, that differ from light mode, so that they override them. The rules
 : created for both modes must be the same in number and order, so that they
 : can be compared.
 :
 : @param $mode "light", "dark" or "auto".
 : @param $rules a function that creates the rules for mode "light" or "dark".
 : @return the rules.
 :)
declare function style:for-modes($mode as xs:string, $rules as function(xs:string) as xs:string*) as xs:string*
{
  if ($mode eq "auto") then
    let $light := $rules("light")
    let $dark := $rules("dark")
    let $_ := if (count($light) ne count($dark)) then error(xs:QName("style:rules"), "different rules in light mode and dark mode") else ()
    let $overrides :=
      for $rule at $i in $dark
      where $rule ne $light[$i]
      return "&#xA;      " || replace($rule, "&#xA;", "&#xA;  ")
    return
    (
      $light,
      "@media (prefers-color-scheme: dark) {" || string-join($overrides) || "&#xA;    }"
    )
  else
    $rules($mode)
};

(:~
 : Formats CSS rules as the content of a style element.
 :
 : @param $rules the rules.
 : @return the CSS text.
 :)
declare function style:to-css($rules as xs:string*) as xs:string
{
  string-join(for $rule in $rules return "&#xA;    " || $rule) || "&#xA;  "
};

(:~
 : Construct standard xhtml:style content for use by all pages.
 :
 : @param $color the base color code.
 : @param $mode "light", "dark" or "auto".
 : @return the xhtml:style element content as a string
 :)
declare function style:css($color as xs:string, $mode as xs:string, $width as xs:integer?) as xs:string
{
  style:to-css(style:for-modes($mode, function($m) {style:css-text($color, $m, $width)}))
};

declare %private function style:css-text($color as xs:string, $mode as xs:string, $width as xs:integer?) as xs:string*
{
  <style type="text/css" xmlns="http://www.w3.org/1999/xhtml">
    <r>:root
    {{
      color-scheme: {$mode};
    }}</r>
    <r>::-moz-selection
    {{
      color: {style:color-background($color, $mode)};
      background: {style:color-foreground($color, $mode)};
    }}</r>
    <r>::selection
    {{
      color: {style:color-background($color, $mode)};
      background: {style:color-foreground($color, $mode)};
    }}</r>
    <r>body
    {{
      font: normal 12px Verdana, sans-serif;
      color: {style:color-foreground($color, $mode)};
      background: {style:color-background($color, $mode)};
    }}</r>
    <r>.ebnf a
    {{
      text-decoration: none;
    }}</r>
    <r>.ebnf a:hover
    {{
      color: {style:color-fg-hilite($color, $mode)};
      text-decoration: underline;
    }}</r>
    <r>.signature
    {{
      color: {style:color-fg-button($color, $mode)};
      font-size: 11px;
      text-align: right;
    }}</r>
    <r>a:link, a:visited
    {{
      color: {style:color-foreground($color, $mode)};
    }}</r>
    <r>a:link.signature, a:visited.signature
    {{
      color: {style:color-fg-button($color, $mode)};
    }}</r>
    <r>div.ebnf
    {{
      padding: {$s:padding}px;
      background: {style:color-bg-hilite($color, $mode)};
      width: {($width, $s:page-width)[1]}px;
    }}</r>
    <r>.ebnf div
    {{
      padding-left: 13ch;
      text-indent: -13ch;
    }}</r>
    <r>.ebnf code
    {{
      font:12px SFMono-Regular,Consolas,Liberation Mono,Menlo,Courier,monospace;
    }}</r>
  </style>/*/string()
};

(:~
 : Construct xhtml:style content for use by UI.
 :
 : @param $color the base color code.
 : @param $mode "light", "dark" or "auto".
 : @return the xhtml:style element content as a string
 :)
declare function style:ui-css($color as xs:string, $mode as xs:string) as xs:string
{
  style:to-css(style:for-modes($mode, function($m) {style:ui-css-text($color, $m)}))
};

declare %private function style:ui-css-text($color as xs:string, $mode as xs:string) as xs:string*
{
  <style type="text/css" xmlns="http://www.w3.org/1999/xhtml">
    <r>.grammar a
    {{
      text-decoration: none;
    }}</r>
    <r>.grammar a:hover
    {{
      color: {style:color-fg-hilite($color, $mode)};
      text-decoration: underline;
    }}</r>
    <r>.grammar code, #editor, pre
    {{
      font:12px SFMono-Regular,Consolas,Liberation Mono,Menlo,Courier,monospace;
    }}</r>
    <r>a.button, #tabs li a
    {{
      padding: 0.25em 0.5em;
      border: 1px solid {style:color-fg-button($color, $mode)};
      background: {style:color-bg-button($color, $mode)};
      color: {style:color-fg-button($color, $mode)};
      text-decoration: none;
      font-weight: bold;
    }}</r>
    <r>a.button:hover, #tabs li a:hover
    {{
      color: {style:color-fg-hilite($color, $mode)};
      background: {style:color-bg-hilite($color, $mode)};
      border-color: {style:color-fg-hilite($color, $mode)};
    }}</r>
    <r>#tabs
    {{
      padding: 3px 10px;
      margin-left: 0;
      margin-top: 58px;
      border-bottom: 1px solid {style:color-foreground($color, $mode)};
    }}</r>
    <r>#tabs li
    {{
      list-style: none;
      margin-left: 5px;
      display: inline;
    }}</r>
    <r>#tabs li a
    {{
      border-bottom: 1px solid {style:color-foreground($color, $mode)};
    }}</r>
    <r>#tabs li a.active
    {{
      color: {style:color-foreground($color, $mode)};
      background: {style:color-background($color, $mode)};
      border-color: {style:color-foreground($color, $mode)};
      border-bottom: 1px solid {style:color-background($color, $mode)};
      outline: none;
    }}</r>
    <r>#divs div[name="div"]
    {{
      display: none;
      overflow:auto;
    }}</r>
    <r>#divs div[name="div"].active
    {{
      display: block;
    }}</r>
    <r>#editor
    {{
      height: 100%;
      width:100%;
      margin-bottom: 10px;
      border: 1px solid {style:color-fg-button($color, $mode)};
      background: {style:color-text($color, $mode)};
      color: {style:color-fg-hilite($color, $mode)};
    }}</r>
    <r>.small
    {{
      vertical-align: top;
      text-align: right;
      font-size: 9px;
      font-weight: normal;
      line-height: 120%;
    }}</r>
    <r>td.small
    {{
      padding-top: 0px;
    }}</r>
    <r>.hidden
    {{
      visibility: hidden;
    }}</r>
    <r>td:hover .hidden
    {{
      visibility: visible;
    }}</r>
    <r>div.download
    {{
      display: none;
      background: {style:color-background($color, $mode)};
      position: absolute;
      right: 34px;
      top: 94px;
      padding: 10px;
      border: 1px dotted {style:color-foreground($color, $mode)};
      width: 220px;
      max-width: 220px;
    }}</r>
    <r>.download-option
    {{
      width: 100px;
      height: 22px;
      max-width: 100px;
      max-height: 22px;
    }}</r>
    <r>div.grammar
    {{
      padding-left: 16px;
      padding-top: 2px;
      padding-bottom: 2px;
      background: {style:color-bg-hilite($color, $mode)};
    }}</r>
    <r>tr.option-line td:first-child
    {{
      text-align: right
    }}</r>
    <r>tr.option-text td
    {{
      padding-bottom: 10px
    }}</r>
    <r>pre
    {{
      margin: 0px;
    }}</r>
    <r>table.palette
    {{
      border-top: 1px solid {style:color-fg-hilite($color, $mode)};
      border-right: 1px solid {style:color-fg-hilite($color, $mode)};
      margin-bottom: 4px
    }}</r>
    <r>td.palette
    {{
      border-bottom: 1px solid {style:color-fg-hilite($color, $mode)};
      border-left: 1px solid {style:color-fg-hilite($color, $mode)};
    }}</r>
    <r>a.palette
    {{
      padding: 2px 3px 2px 10px;
      text-decoration: none;
    }}</r>
    <r>.palette
    {{
      -webkit-user-select: none;
      -khtml-user-select: none;
      -moz-user-select: none;
      -o-user-select: none;
      -ms-user-select: none;
    }}</r>
    <r>.ace_cursor
    {{
      color: {style:color-foreground($color, $mode)} !important;
    }}</r>
    <r>.ace_gutter
    {{
      color: {style:color-foreground($color, $mode)} !important;
      background: {style:color-background($color, $mode)} !important;
    }}</r>
    <r>.ace_search_field, .ace_searchbtn, .ace_button, .ace_search_options, .ace_search_counter
    {{
      color: {style:color-foreground($color, $mode)} !important;
    }}</r>
    <r>.ace_gutter-layer
    {{
      background: {style:color-background($color, $mode)} !important;
      border-right: 1px dashed {style:color-fg-button($color, $mode)} !important;
    }}</r>
    <r>.ace_active-line
    {{
      background: {style:color-background($color, $mode)} !important;
    }}</r>
    <r>.ace_search
    {{
      border-color: {style:color-fg-button($color, $mode)} !important;
    }}</r>
    <r>.ace_selection, .ace_search
    {{
      background: {style:color-bg-button($color, $mode)} !important;
    }}</r>
    <r>.ace_gutter-active-line
    {{
      background: {style:color-bg-hilite($color, $mode)} !important;
    }}</r>
    <r>.ace_search_field, .ace_searchbtn, .ace_button:hover
    {{
      background: {style:color-text($color, $mode)} !important;
    }}</r>
    <r>.ace_button.checked
    {{
      border-color: {style:color-foreground($color, $mode)} !important;
    }}</r>
    <r>.ace_searchbtn:hover
    {{
      background: {style:color-bg-hilite($color, $mode)} !important;
    }}</r>
  </style>/*/string()
};