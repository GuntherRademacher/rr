(:~
 : The user interface module, containing most of the code that deals
 : with user requests and responses served to the user.
 :)
module namespace ui="de/bottlecaps/railroad/xq/user-interface.xq";

import module namespace c="de/bottlecaps/railroad/xq/color.xq";
import module namespace style="de/bottlecaps/railroad/xq/style.xq";
import module namespace e="de/bottlecaps/railroad/xq/html-to-ebnf.xq";
import module namespace a="de/bottlecaps/railroad/xq/cst-to-ast.xq";
import module namespace s="de/bottlecaps/railroad/xq/ast-to-svg.xq";
import module namespace t="de/bottlecaps/railroad/xq/transform-ast.xq";
import module namespace m="de/bottlecaps/railroad/xq/xhtml-to-md.xq";

declare namespace p="de/bottlecaps/railroad/core/Parser";
declare namespace webapp="http://bottlecaps.de/webapp";
declare namespace http-client="http://expath.org/ns/http-client";

declare namespace math="http://www.w3.org/2005/xpath-functions/math";
declare namespace xhtml="http://www.w3.org/1999/xhtml";
declare namespace xlink="http://www.w3.org/1999/xlink";
declare namespace svg="http://www.w3.org/2000/svg";
declare namespace g="http://www.w3.org/2001/03/XPath/grammar";

(: HTTP parameter names :)

(:~
 : The "name" HTTP parameter name.
 :)
declare variable $ui:NAME := "name";

(:~
 : The "task" HTTP parameter name.
 :)
declare variable $ui:TASK := "task";

(: tab names :)

(:~
 : The "WELCOME" tab name.
 :)
declare variable $ui:WELCOME := "WELCOME";

(:~
 : The "GET" tab name.
 :)
declare variable $ui:GET := "GET";

(:~
 : The "EDIT" tab name.
 :)
declare variable $ui:EDIT := "EDIT";

(:~
 : The "VIEW" tab name.
 :)
declare variable $ui:VIEW := "VIEW";

(:~
 : The "OPTIONS" tab name.
 :)
declare variable $ui:OPTIONS := "OPTIONS";

(:~
 : The "ABOUT" tab name.
 :)
declare variable $ui:ABOUT := "ABOUT";

(: task names :)

(:~
 : The "LOAD" task name.
 :)
declare variable $ui:LOAD := "LOAD";

(:~
 : The "SAVE" task name.
 :)
declare variable $ui:SAVE := "SAVE";

(:~
 : The "XHTML" task name.
 :)
declare variable $ui:XHTML := "XHTML";

(:~
 : The "XHTML" task name.
 :)
declare variable $ui:MD := "MD";

(:~
 : The "ZIP" task name.
 :)
declare variable $ui:ZIP := "ZIP";

(: option names :)

(:~
 : The "showebnf" option name.
 :)
declare variable $ui:SHOWEBNF := "showebnf";

(:~
 : The "eliminaterecursion" option name.
 :)
declare variable $ui:ELIMINATERECURSION := "eliminaterecursion";

(:~
 : The "factoring" option name.
 :)
declare variable $ui:FACTORING := "factoring";

(:~
 : The "inline" option name.
 :)
declare variable $ui:INLINE := "inline";

(:~
 : The "keep" option name.
 :)
declare variable $ui:KEEP := "keep";

(:~
 : The handler function for the Railroad Diagram Generator.
 :
 : Dispatch request based on the http "task" parameter.
 : @return depending on the request, either an XHTML page,
 : or a text attachment for download.
 :)
declare function ui:ui() as item()*
{
  let $request-parameters := webapp:parameter-names()
  let $task :=
  (
    webapp:parameter-values($ui:TASK),
    $ui:VIEW[$request-parameters = ("text", "ebnf")],
    $ui:EDIT[$request-parameters = "uri"],
    $ui:WELCOME
  )[1]
  return if ($task = $ui:SAVE) then ui:save()
    else if ($task = $ui:LOAD) then ui:load(ui:color())
    else if ($task = $ui:XHTML) then ui:download-xhtml()
    else if ($task = $ui:MD) then ui:download-md()
    else if ($task = $ui:ZIP) then ui:download-zip()
    else ui:process($task, ui:color())
};

(:~
 : Check whether the user agent is Internet Explorer before
 : version 9. These versions cannot handle XHTML or inline SVG,
 : so we will have to send a notification.
 :
 : @return true, if the user agent is MSIE version 3 to 8.
 :)
declare function ui:is-IE-pre9()
{
  matches(webapp:user-agent(), "^[^\(]*\(compatible; MSIE [3-8]")
};

(:~
 : Send a blank page with a javascript alert, indicating that
 : we are not going to handle Internet Explorers before version
 : 9.
 :
 : @return an html element, along with a matching Content-Type
 : setting.
 :)
declare function ui:unsupported() as element(html)
{
  webapp:set-content-type("text/html"),
  <html>
    <head>
      <meta http-equiv="Content-Type" content="text/html"/>
      <title>Not supported</title>
    </head>
    <body>
      <script type="text/javascript">
        alert
        (
          "Browser does not support XHTML.\n" +
          "\n" +
          "The Railroad Diagram Generator sends XHTML with inline SVG,\n" +
          "so it requires a browser supporting this. Internet Explorer starts\n" +
          "to support XHTML and inline SVG in version 9.\n" +
          "\n" +
          "Please use another browser, e.g. Mozilla Firefox.\n"
        )
      </script>
    </body>
  </html>
};

(:~
 : Create the basic XHTML page, containing tab navigation and
 : user content.
 :
 : @param $tab the id of the tab to be shown when the page is loaded.
 : @param $text the contents of the editor's textArea.
 : @param $modified the modification flag. Indicates whether a click
 : on the "VIEW" button requires an extra server request.
 : @param $errorlog the error log to be shown on the the "VIEW" tab, if any.
 : @param $view-uri the uri of the page tp be shown on the "VIEW" tab's iframe.
 : @param $color the color code.
 : @return an HTML or XHTML page, along with appropriate http header settings.
 :)
declare function ui:html($tab as xs:string,
                                        $text as xs:string?,
                                        $modified as xs:boolean,
                                        $errorlog as node()*,
                                        $view-uri as xs:string?,
                                        $color as xs:string) as element()
{
  let $spread :=  xs:integer(ui:parameter-values("spread", "0"))
  let $width := xs:integer(ui:parameter-values("width", string($s:page-width)))
  return
  if (ui:is-IE-pre9()) then
    ui:unsupported()
  else
  (
    ui:xhtml(true()),
    webapp:set-header("Cache-Control", "no-cache"),
    <html xmlns="http://www.w3.org/1999/xhtml">
    {
      if (webapp:parameter-values("frame") = "diagram") then
      (
        <head>{s:head($color, $spread, $width)}</head>,
        <body>{$errorlog}</body>
      )
      else
      (
      <head>
        {s:head($color, $spread, $width)}
        <link href="favicon.ico" rel="shortcut icon"/>,
        <title>Railroad Diagram Generator</title>,
      </head>,
      <body onresize="resize()" onscroll="resize()" onload="onLoad()">
        {ui:logo()}
        <div id="download" class="download">
          <table border="0">
            <tr>
              <td colspan="3"><b>Download diagram</b></td>
            </tr>
            <tr>
              <td><input type="radio" name="download-type" value="svg" checked="true"/></td>
              <td style="font-size: 14px;">XHTML+SVG&#xA0;</td>
              <td style="font-size: 9px; line-height: 11px;">
                single XHTML page with<br/>
                inline SVG graphics
              </td>
            </tr>
            <tr>
              <td><input type="radio" name="download-type" value="png"/></td>
              <td style="font-size: 14px;">HTML+PNG&#xA0;</td>
              <td style="font-size:9px; line-height: 11px;">
                zip file containing HTML<br/>
                and linked PNG images
              </td>
            </tr>
<!--
            <tr>
              <td><input type="radio" name="download-type" value="md"/></td>
              <td style="font-size: 14px;">MD+SVG&#xA0;</td>
              <td style="font-size:9px; line-height: 11px;">
                single Markdown file with <br/>
                inline SVG graphics
              </td>
            </tr>
-->
            <tr>
              <td colspan="3"><small>&#xA;</small></td>
            </tr>
            <tr>
              <td colspan="3"><small>&#xA;</small></td>
            </tr>
            <tr>
              <td colspan="3"><a class="button" href="javascript:downld()">Download</a></td>
            </tr>
          </table>
        </div>
        <form name="data" method="POST" enctype="multipart/form-data" onsubmit="return false;">
          <input type="hidden" name="tz" value="0"/>
          <input type="hidden" name="{$ui:TASK}" id="{$ui:TASK}"/>
          <input type="hidden" name="frame" id="frame"/>
          <input type="hidden" name="{$ui:NAME}" id="{$ui:NAME}" value="{ui:name((), ())}"/>
          <input type="hidden" name="time" id="time" value="{webapp:parameter-values("time")}"/>
          <input type="hidden" name="color" value="{ui:color()}" style="width: 32px"/>

          <script type="text/javascript">var {$ui:WELCOME} = 0;</script>
          <script type="text/javascript">var {$ui:GET}     = 1;</script>
          <script type="text/javascript">var {$ui:EDIT}    = 2;</script>
          <script type="text/javascript">var {$ui:VIEW}    = 3;</script>
          <script type="text/javascript">var {$ui:OPTIONS} = 4;</script>
          <script type="text/javascript">var {$ui:ABOUT}   = 5;</script>

          <ul id="tabs">
            <li><a name="tab" href="javascript:tab({$ui:WELCOME})">Welcome</a>&#32;</li
           ><li><a name="tab" href="javascript:tab({$ui:GET})">Get Grammar</a>&#32;</li
           ><li><a name="tab" href="javascript:tab({$ui:EDIT})">Edit Grammar</a>&#32;</li
           ><li><a name="tab" href="javascript:tab({$ui:VIEW})">View Diagram</a>&#32;</li
           ><li><a name="tab" href="javascript:tab({$ui:OPTIONS})">Options</a>&#32;</li
           >
          </ul>

          <div id="divs">
            <div name="div">{ui:welcome-tab()}</div>
            <div name="div">
              <table border="0">
                <tr colspan="3"></tr>
                <tr>
                  <td colspan="3" align="center">
                    <p><b>From this website:</b></p>
                  </td>
                </tr>
                <tr>
                  <td align="right">EBNF Notation</td>
                  <td>
                    <input id="myUri0" type="hidden" name="myUri0" value=""/>
                    <input id="myUri1" type="radio" name="spec" onclick="setUri(this.value)" value=""/>
                  </td>
                  <td>
                    <span id="myUri2"></span>
                    <script type="text/javascript">
                      function myUrl()
                      {{
                        return document.location.href;
                      }};
                      document.getElementById("myUri0").value = myUrl();
                      document.getElementById("myUri1").value = myUrl();
                      document.getElementById("myUri2").innerHTML = myUrl();
                    </script>
                  </td>
                </tr>
                <tr>
                  <td colspan="3" align="center">
                    <p><b>From W3C specifications:</b></p>
                  </td>
                </tr>
                {
                  let $specs :=
                  (
                    "xml",
                    "xml-names",
                    "1999/REC-xpath-19991116",
                    "xpath20",
                    "xpath-30",
                    "2010/REC-xquery-20101214",
                    "xquery-30",
                    "xquery-31",
                    "xquery-update-10",
                    "xquery-update-30",
                    "xquery-sx-10",
                    "rdf-sparql-query",
                    "sparql11-query",
                    "turtle"
                  )
                  let $descriptions :=
                  (
                    "Extensible Markup Language (XML) 1.0",
                    "Namespaces in XML 1.0",
                    "XML Path Language (XPath) 1.0",
                    "XML Path Language (XPath) 2.0",
                    "XML Path Language (XPath) 3.0",
                    "XQuery 1.0: An XML Query Language (Second Edition)",
                    "XQuery 3.0: An XML Query Language",
                    "XQuery 3.1: An XML Query Language",
                    "XQuery Update Facility 1.0",
                    "XQuery Update Facility 3.0",
                    "XQuery Scripting Extension 1.0",
                    "SPARQL Query Language for RDF",
                    "SPARQL 1.1 Query Language",
                    "Turtle Terse RDF Triple Language"
                  )
                  for $spec at $i in $specs
                  return
                    <tr>
                      <td align="right">{$descriptions[$i]}</td>
                      <td><input type="radio" name="spec" onclick="setUri(this.value)" value="https://www.w3.org/TR/{$spec}/"/></td>
                      <td>https://www.w3.org/TR/{$spec}/</td>
                    </tr>
                }
                <tr>
                  <td align="right">other</td>
                  <td><input id="other" type="radio" name="spec" onclick="setUri(this.value)" value="" checked=""/></td>
                  <td><small>enter URL below</small></td>
                </tr>
                <tr><td colspan="3" align="center">&#xA0;</td></tr>
                <tr>
                  <td align="right">URL</td>
                  <td>&#xA0;</td>
                  <td><input type="text" id="uri" name="uri" size="50" onclick="other.checked = true;"/></td>
                </tr>
              </table>
            </div>
            <div name="div" style="overflow:hidden">
              <textarea id="text" name="text" onchange="changeText();" style="width:100%" spellcheck="false">{$text}</textarea>
              <input id="textChanged" name="textChanged" type="hidden" value="{$modified[.]}"/>
              <table>
                <tr>
                  <td>
                    <a class="button" href="javascript:clear()[0]">Clear</a>
                    &#32;
                    <a class="button" href="javascript:save()[0]">Save</a>
                  </td>
                  <td style="width: 60px;">&#160;</td>
                  <td>
                    <input type="file" size="42" id="file" name="file"/>
                    &#32;
                    <a class="button" href="javascript:load()">Load</a>
                  </td>
                </tr>
              </table>
            </div>
            <div name="div" style="overflow:hidden">
              <div id="errorlog" style="display:block;">{$errorlog}</div>
              <iframe frameborder="0" name="diagram" id="diagram" width="100%" height="100%" onload="diagramLoaded();">
              {
                attribute src {$view-uri}[$view-uri != ""]
              }
              </iframe>
            </div>
            <div name="div">
              <div style="display:block; max-width: 768px; overflow: hidden;">
                {
                  let $options := ui:options()
                  let $previewAst :=
                    <g:grammar xmlns:g="http://www.w3.org/2001/03/XPath/grammar">
                      <g:production name="Preview">
                        <g:choice>
                           <g:string>terminal</g:string>
                           <g:ref name="nonterminal"/>
                           <g:subtract><g:ref name="EBNF"/><g:ref name="expression"/></g:subtract>
                        </g:choice>
                      </g:production>
                    </g:grammar>
                  let $preview-color := s:color-1($style:default-color)
                  let $hsl := c:rgb-to-hsl($preview-color)
                  let $previewHtml :=
                  (
                    <style type="text/css">
                      #preview div
                      {{
                        display: block;
                      }}
                      #preview div.ebnf, .ebnf code
                      {{
                        padding-right: 0px;
                        width: 94%;
                      }}
                    </style>,
                    s:svg($previewAst, true(), $s:page-width, $preview-color, 0, true(), "/")[position() <= 5]
                  )
                  let $palette-width := 18
                  let $previewEbnf := for $x at $i in $previewHtml where $x//@class = "ebnf" return $i
                  return
                    <table>
                      <tr>
                        <td valign="top">
                          <table>
                            <tr class="option-line">
                              <td align="right" style="height: 1;">
                                <input type="hidden" name="preview-html-1" id="preview-html-1" value="{ui:serialize($previewHtml[position() < $previewEbnf])}"/>
                                <input type="hidden" name="preview-html-2" id="preview-html-2" value="{ui:serialize($previewHtml[position() = $previewEbnf])}"/>
                                <input type="hidden" name="preview-html-3" id="preview-html-3" value=
                                  "{
                                    ui:serialize
                                    ((
                                      (: $previewHtml[position() > $previewEbnf], :)
                                      if ($c:debug) then
                                        <table>{
                                          for $rgb in (s:color-1($preview-color),
                                                       c:relative-color($color, 1.0, 0.04),
                                                       s:color-nonterminal($preview-color, 0),
                                                       c:relative-color($color, 1.0, 0.05),
                                                       s:color-regexp($preview-color, 0),
                                                       c:relative-color($color, 1.0, 0.06),
                                                       style:color-background($preview-color),
                                                       style:color-bg-hilite($preview-color)
                                                       )
                                          let $r-g-b := for $x in c:r-g-b($rgb) return string($x)
                                          let $h-s-l := for $x in c:rgb-to-hsl($rgb) return string(round($x * 100) div 100)
                                          let $title := concat($rgb, "  rgb(", $r-g-b[1], ",", $r-g-b[2], ",", $r-g-b[3], ")")
                                          return <tr><td style="background-color: {$rgb}" class="palette"><a title="{$title}">&#xA0;&#xA0;&#xA0;&#xA0;&#xA0;</a></td><td>{$rgb}</td></tr>
                                        }</table>
                                      else
                                        ()
                                    ))
                                  }"/>
                                {
                                  comment {$options},
                                  element input
                                  {
                                    attribute type {"checkbox"},
                                    attribute name {"options"},
                                    attribute id  {$ui:SHOWEBNF},
                                    attribute value {$ui:SHOWEBNF},
                                    attribute onclick {"changeText();preview();"},
                                    if ($options = $ui:SHOWEBNF) then attribute checked {true()} else ()
                                  }
                                }
                              </td>
                              <td style="height: 1;" colspan="4"><b>Show EBNF</b></td>
                            </tr>
                            <tr>
                              <td style="height: 1;">&#xA0;</td>
                              <td style="height: 1;" colspan="4">
                                The corresponding EBNF will be shown next to generated diagrams. If
                                this option is unchecked, the EBNF display will be suppressed.
                              </td>
                            </tr>
                            <tr>
                              <td>&#xA0;</td>
                            </tr>
                            <tr>
                              <td align="right" style="height: 1;">
                                <input type="text" name="rgb" onchange="setRgb(this.value, true);" style="width: 67px; text-align:right"/>
                              </td>
                              <td/>
                              <td style="height: 1;"><b>Color</b></td>
                              <td colspan="2">
                                {
                                    <table cellpadding="0" cellspacing="0" class="palette">
                                      <tr class="palette">
                                      {
                                        let $angle := 360 div $palette-width
                                        for $i in (0 to $palette-width - 1)
                                        let $h := $angle * $i
                                        let $s := 1.0
                                        let $l := $c:default-lightness
                                        let $rgb := c:rgb(c:hsl-to-rgb($h, $s, $l))
                                        let $r-g-b := for $x in c:r-g-b($rgb) return string($x)
                                        let $h-s-l := for $x in c:rgb-to-hsl($rgb) return string(round($x * 100) div 100)
                                        let $title := concat($rgb, "  rgb(", $r-g-b[1], ",", $r-g-b[2], ",", $r-g-b[3], ")  hsl(", $h-s-l[1], ",", $h-s-l[2], ",", $h-s-l[3], ")")
                                        return
                                          <td style="background-color: {$rgb}" class="palette">
                                            <a href="javascript:setRgb('{$rgb}', true)" title="{$title}" class="palette"><small>&#160;</small></a>
                                         </td>
                                      }
                                      </tr>
                                    </table>
                                }
                              </td>
                            </tr>
                            <tr>
                              <td align="right" style="height: 1;"><input type="text" name="hue" onchange="setRgbFromHsl('hue', 0, 0, 359);" style="width: 24px; text-align:right"/></td>
                              <td>&#xBA;</td>
                              <td><b>Hue</b></td>
                              <td>
                                <nobr>
                                  <a href="javascript:" class="button" onmousedown="scheduleIncrement('hue', -1, 0, 359)" onmouseout="cancelIncrement()" onmouseup="cancelIncrement()"><b>&lt;</b></a>&#32;
                                  <a href="javascript:" class="button" onmousedown="scheduleIncrement('hue', +1, 0, 359)" onmouseout="cancelIncrement()" onmouseup="cancelIncrement()"><b>&gt;</b></a>
                                </nobr>
                              </td>
                              <td rowspan="4" style="vertical-align: top; padding-left: 10px;">
                                Select base color from the palette above, or enter color data into the fields on the left.
                                Adjust hue, saturation, or lightness by clicking, or holding their respective controls.
                                Arrange other colors by setting hue offset, 0 is for a monochromatic color scheme.
                              </td>
                            </tr>

                            <tr>
                              <td align="right" style="height: 1;"><input type="text" name="saturation" onchange="setRgbFromHsl('saturation', 0, 0, 100);" style="width: 24px; text-align:right"/></td>
                              <td>%</td>
                              <td><b>Saturation</b></td>
                              <td>
                                <nobr>
                                  <a href="javascript:" class="button" onmousedown="scheduleIncrement('saturation', -1, 0, 100)" onmouseout="cancelIncrement()" onmouseup="cancelIncrement()"><b>&lt;</b></a>&#32;
                                  <a href="javascript:" class="button" onmousedown="scheduleIncrement('saturation', +1, 0, 100)" onmouseout="cancelIncrement()" onmouseup="cancelIncrement()"><b>&gt;</b></a>
                                </nobr>
                              </td>
                            </tr>

                            <tr>
                              <td align="right" style="height: 1;"><input type="text" name="lightness" onchange="setRgbFromHsl('lightness', 0, 0, 100);" style="width: 24px; text-align:right"/></td>
                              <td>%</td>
                              <td><b>Lightness</b></td>
                              <td>
                                <nobr>
                                  <a href="javascript:" class="button" onmousedown="scheduleIncrement('lightness', -1, 0, 100)" onmouseout="cancelIncrement()" onmouseup="cancelIncrement()"><b>&lt;</b></a>&#32;
                                  <a href="javascript:" class="button" onmousedown="scheduleIncrement('lightness', +1, 0, 100)" onmouseout="cancelIncrement()" onmouseup="cancelIncrement()"><b>&gt;</b></a>
                                </nobr>
                              </td>
                            </tr>

                            <tr>
                              <td align="right" style="height: 1;">
                                <input type="text" id="spread" name="spread" onfocus="this.oldvalue = this.value" onchange="checkSpread();" style="width: 24px; text-align:right" value="{ui:parameter-values("spread", "0")}"/>
                              </td>
                              <td>&#xBA;</td>
                              <td><b>Hue offset</b></td>
                              <td>
                                <nobr>
                                  <a href="javascript:" class="button" onmousedown="scheduleIncrement('spread', -1, 0, 359)" onmouseout="cancelIncrement()" onmouseup="cancelIncrement()"><b>&lt;</b></a>&#32;
                                  <a href="javascript:" class="button" onmousedown="scheduleIncrement('spread', +1, 0, 359)" onmouseout="cancelIncrement()" onmouseup="cancelIncrement()"><b>&gt;</b></a>
                                </nobr>
                              </td>
                            </tr>
                            <tr>
                              <td colspan="5">&#xA0;</td>
                            </tr>

                            <tr class="option-line">
                              <td>
                                <input type="text" id="width" name="width" onfocus="this.oldvalue = this.value" onchange="checkWidth();" style="width: 40px; text-align:right;" value="{ui:parameter-values("width", string($s:page-width))}"/>
                              </td>
                              <td><small>px</small></td>
                              <td colspan="3"><b>Graphics width</b></td>
                            </tr>
                            <tr class="option-text">
                              <td>&#xA0;</td>
                              <td colspan="4">
                                When the graphics exceeds this width, attempts will
                                be made to break it and start a continuation line.
                              </td>
                            </tr>

                            <div style="display: none;">
                            <tr class="option-line">
                              <td>
                                <input type="text" id="padding" name="padding" onfocus="this.oldvalue = this.value" onchange="checkWidth();" style="width: 40px; text-align:right;" value="{ui:parameter-values("padding", string($s:padding))}"/>
                              </td>
                              <td><small>px</small></td>
                              <td colspan="3"><b>Padding</b></td>
                            </tr>
                            <tr class="option-text">
                              <td>&#xA0;</td>
                              <td colspan="4">
                                The number of pixels between text and the surrounding box.
                              </td>
                            </tr>

                            <tr class="option-line">
                              <td>
                                <input type="text" id="strokewidth" name="strokewidth" onfocus="this.oldvalue = this.value" onchange="checkWidth();" style="width: 40px; text-align:right;" value="{ui:parameter-values("strokewidth", string($s:stroke-width))}"/>
                              </td>
                              <td><small>px</small></td>
                              <td colspan="3"><b>Stroke width</b></td>
                            </tr>
                            <tr class="option-text">
                              <td>&#xA0;</td>
                              <td colspan="4">
                                The width of lines and frames in pixels.
                              </td>
                            </tr>
                            </div>

                            <tr class="option-line">
                              <td>
                              {
                                element input
                                {
                                  attribute type {"checkbox"},
                                  attribute name {"options"},
                                  attribute id  {$ui:ELIMINATERECURSION},
                                  attribute value {$ui:ELIMINATERECURSION},
                                  attribute onclick {"changeText();preview();"},
                                  if ($options = $ui:ELIMINATERECURSION) then attribute checked {true()} else ()
                                }
                              }
                              </td>
                              <td colspan="4"><b>Direct recursion elimination</b></td>
                            </tr>
                            <tr class="option-text">
                              <td>&#xA0;</td>
                              <td colspan="4">
                                Unless disabled, direct recursion will be replaced by repetition. This
                                applies to nonterminals, whose directly recursive references are
                                either left- or right-recursive only. Upon success,
                                productions are inlined, when a single reference remains.
                               </td>
                            </tr>

                            <tr class="option-line">
                              <td>
                              {
                                element input
                                {
                                  attribute type {"checkbox"},
                                  attribute name {"options"},
                                  attribute id  {$ui:FACTORING},
                                  attribute value {$ui:FACTORING},
                                  attribute onclick {"changeText();preview();"},
                                  if ($options = $ui:FACTORING) then attribute checked {true()} else ()
                                }
                              }
                              </td>
                              <td colspan="4"><b>Factoring</b></td>
                            </tr>
                            <tr class="option-text">
                              <td>&#xA0;</td>
                              <td colspan="4">
                                When checked, left and right factoring will be applied to
                                right-hand sides of all productions individually, in order
                                to achieve more compact diagrams.
                               </td>
                            </tr>

                            <tr class="option-line">
                              <td>
                              {
                                element input
                                {
                                  attribute type {"checkbox"},
                                  attribute name {"options"},
                                  attribute id  {$ui:INLINE},
                                  attribute value {$ui:INLINE},
                                  attribute onclick {"changeText();preview();"},
                                  if ($options = $ui:INLINE) then attribute checked {true()} else ()
                                }
                              }
                              </td>
                              <td colspan="4"><b>Inline literals</b></td>
                            </tr>
                            <tr class="option-text">
                              <td>&#xA0;</td>
                              <td colspan="4">
                                Replace nonterminal references by their definition, when they derive to a single
                                string literal.
                               </td>
                            </tr>

                            <tr class="option-line">
                              <td>
                              {
                                element input
                                {
                                  attribute type {"checkbox"},
                                  attribute name {"options"},
                                  attribute id  {$ui:KEEP},
                                  attribute value {$ui:KEEP},
                                  attribute onclick {"changeText();preview();"},
                                  if ($options = $ui:KEEP) then attribute checked {true()} else ()
                                }
                              }
                              </td>
                              <td colspan="4"><b>Keep epsilon nonterminals</b></td>
                            </tr>
                            <tr class="option-text">
                              <td>&#xA0;</td>
                              <td colspan="4">
                                Keep references to nonterminals, that derive to epsilon only. When unchecked,
                                they will be removed.
                               </td>
                            </tr>
                          </table>

                          <div style="display:none;">
                            <input type="checkbox" name="options" id="sendoptions" value="sendoptions" checked="true"/>
                          </div>

                        </td>
                        <td style="vertical-align: top">
                          <table>
                            <tr>
                              <td align="right" style="vertical-align: top; padding-top: 4px; padding-bottom: 10px;">
                                <a href="javascript:reset(false)" class="button" onclick="reset(false)"
                                   title="Reset to previous values that are currently in effect"><b>Cancel</b></a>
                                &#xA0;
                                <a href="javascript:reset(true)" class="button" onclick="reset(true)"
                                   title="Reset to default values"><b>Defaults</b></a>
                              </td>
                            </tr>
                            <tr>
                              <td id="preview" style="vertical-align: top; border: 1px dotted; padding-left: 6px; padding-right: 6px; padding-bottom: 6px; width: 270px; height: 280px"/>
                            </tr>
                          </table>
                        </td>
                      </tr>
                    </table>
                }
              </div>
            </div>
            <div name="div" style="overflow:hidden;">
              <iframe frameborder="0" id="frameset" width="100%" height="100%"/>
            </div>
          </div>
          <div style="display:none;">
            <textarea id="xhtml" name="xhtml"></textarea>
          </div>
        </form>
        {
          ui:spinner($color),
          if ($tab = $ui:VIEW and not($view-uri != "") and $text != "" and not($errorlog != "")) then
            ui:javascript($ui:EDIT, true())
          else
            ui:javascript($tab, false())
        }
      </body>
      )
    }
    </html>
  )
};

(:~
 : Return javascript code for the user interface XHTML page.
 :
 : @param $tab the id of the tab to be shown after loading the page.
 : @param $submit-on-load whether to submit a view request after loading
 : @return the script element containing the javascript code.
 :)
declare function ui:javascript($tab as xs:string, $submit-on-load as xs:boolean) as element(xhtml:script)
{
  let $colors :=
    string-join
    (
      (
        for $n at $i in $c:css3-colors//name
        return (", "[$i ne 1], """", $n, """: """, $n/../@rgb/string(.), """")
      ),
      ""
    )
  return
  <script type="text/javascript" xmlns="http://www.w3.org/1999/xhtml" >
    var task = document.getElementById("task");
    var name = document.getElementById("name");
    var text = document.getElementById("text");
    var other = document.getElementById("other");
    var view = document.getElementById("view");
    var textChanged = document.getElementById("textChanged");
    var oldColor = document.getElementsByName("color")[0].value;
    var oldSpread = document.getElementsByName("spread")[0].value;
    var tabs = document.getElementsByName("tab");
    var divs = getElementChildren(document.getElementById("divs"));
    var uri = document.getElementById("uri");
    var file = document.getElementById("file");
    var frameset = document.getElementById("frameset");
    var errorlog = document.getElementById("errorlog");
    var diagram = document.getElementById("diagram");
    var download = document.getElementById("download");
    var active = {$tab};
    var css3colors = {{{$colors}}};

    function okToClear()
    {{
      return text.value == "" || confirm("OK to clear current grammar?");
    }}

    function clear()
    {{
      if (okToClear())
      {{
        text.value = "";
        name.value = "";
        changeText();
        return true;
      }}
      else
      {{
        return false;
      }}
    }}

    function save()
    {{
      if (text.value != "")
      {{
        task.value = "{$ui:SAVE}";
        document.forms.data.target = "";
        document.forms.data.submit();
      }}
    }}

    function resizeEditor()
    {{
      if (active == {$ui:EDIT})
      {{
        var height = "innerHeight" in window
                   ? window.innerHeight
                   : document.documentElement.offsetHeight;
        text.style.height = (height - text.offsetTop - 55) + "px";
      }}
    }}

    function resizeDivs()
    {{
      for (var i = 0; i &lt; divs.length; ++i)
      {{
        divs[i].style.height = (window.innerHeight - divs[i].offsetTop - 10) + "px";
      }}
    }}

    function resize()
    {{
      resizeDivs();
      resizeEditor();
      resizeSpinner();
    }}

    function tab(t)
    {{
      var colorChanged = oldColor != document.getElementsByName("color")[0].value
                      || oldSpread != document.getElementsByName("spread")[0].value;
      var next = t == active ? null
               : t == {$ui:EDIT} ? "{$ui:EDIT}"
               : t == {$ui:VIEW} ? "{$ui:VIEW}"
               : (uri.value == "" &amp;&amp; ! colorChanged) ? null
               : t == {$ui:WELCOME} ? "{$ui:WELCOME}"
               : t == {$ui:GET} ? "{$ui:GET}"
               : t == {$ui:OPTIONS} ? "{$ui:OPTIONS}"
               : t == {$ui:ABOUT} ? "{$ui:ABOUT}"
               : null;
      if (next != null &amp;&amp; uri.value != "")
      {{
        if (okToClear())
        {{
          submit(next, "");
        }}
        else
        {{
          setUri("");
        }}
      }}
      else if (next != null &amp;&amp; colorChanged)
      {{
        errorlog.style.display = "none";
        submit(next, "");
      }}
      else if (next == "{$ui:VIEW}" &amp;&amp; textChanged.value != "")
      {{
        errorlog.style.display = "none";
        submit(next, "diagram");
      }}
      else
      {{
        tabs[active].className = "";
        divs[active].className = "";
        active = t;
        tabs[active].className = "active";
        divs[active].className = "active";

        if (active == {$ui:ABOUT} &amp;&amp; frameset.src == "")
        {{
          frameset.src = "doc-frameset{ui:color-arg("?")}";
        }}

        resize();
      }}

      if (active == {$ui:VIEW})
      {{
        var diagramBody = diagram.contentDocument.getElementsByTagNameNS("http://www.w3.org/1999/xhtml", "body")[0];
        var isEmpty = diagramBody.childNodes.length == 0;
        var isDiagram = diagramBody.getElementsByTagNameNS("http://www.w3.org/2000/svg", "svg").length != 0;

        if (isDiagram)
        {{
          download.style.display = "block";
        }}
        else
        {{
          download.style.display = "none";
          if (! isEmpty)
          {{
            changeText();
          }}
        }}
      }}
      else
      {{
        download.style.display = "none";
      }}
    }}

    function load()
    {{
      if (file.value != "" &amp;&amp; okToClear())
      {{
        submit("{$ui:LOAD}", "");
      }}
    }}

    function setUri(input)
    {{
      if (input == "") other.checked = true;
      uri.value = input;
      return true;
    }}

    function selfDescribe()
    {{
      if (okToClear())
      {{
        setUri(myUrl());
        submit("{$ui:VIEW}", "");
      }}
    }}

    function changeText()
    {{
      if (text.value == "")
      {{
        var diagramBody = diagram.contentDocument.getElementsByTagNameNS("http://www.w3.org/1999/xhtml", "body")[0];
        diagramBody.innerHTML = "";
        textChanged.value = null;
      }}
      else
      {{
        textChanged.value = true;
      }}
      return true;
    }}

    function submit(t, f)
    {{
      task.value = t;
      document.forms.data.target = f;
      document.forms.data.frame.value = f;
      document.forms.data.time.value = new Date().getTime();
      document.forms.data.submit();
      startSpinning();
    }}

    function resubmit()
    {{
      var t = document.forms.data.time.value;
      submit("{$ui:VIEW}", "diagram");
      document.forms.data.time.value = t;
    }}

    function onLoad()
    {{
      document.forms.data.tz.value = new Date().getTimezoneOffset();
      tab({$tab});
      {if ($submit-on-load) then "resubmit" else "showResponseTime"}();
    }}

    function showResponseTime()
    {{
      var t = document.getElementById("time");
      var s = document.getElementById("timeDisplay");
      if (t.value !== "")
      {{
        s.innerHTML = ((new Date().getTime() - parseInt(t.value)) / 1000.0) + " sec";
      }}
      setRgb(colorCode(document.getElementsByName("color")[0].value), false);
      preview();
    }}

    function diagramLoaded()
    {{
      if (spinning)
      {{
        stopSpinning();
        textChanged.value = "";
        showResponseTime();
        tab({$ui:VIEW});
      }}
      resize();
    }}

    function getElementChildren(node)
    {{
      var children = [];
      for (var child = node.firstChild; child; child = child.nextSibling)
      {{
        if (child.nodeType === 1) children.push(child);
      }}
      return children;
    }}

    function downld()
    {{
      var type = "svg";
      var inputs = document.getElementsByName("download-type");
      for (var i = 0; i &lt; inputs.length; ++i)
      {{
        if (inputs[i].checked)
        {{
          type = inputs[i].value;
          break;
        }}
      }}

      if (type === "svg")
      {{
        document.getElementById("xhtml").value = new XMLSerializer().serializeToString(diagram.contentDocument);
        task.value = "{$ui:XHTML}";
        document.forms.data.target = "";
        document.forms.data.frame.value = "";
        document.forms.data.submit();
      }}
      else if (type === "md")
      {{
        document.getElementById("xhtml").value = new XMLSerializer().serializeToString(diagram.contentDocument);
        task.value = "{$ui:MD}";
        document.forms.data.target = "";
        document.forms.data.frame.value = "";
        document.forms.data.submit();
      }}
      else if (type === "png")
      {{
        document.getElementById("xhtml").value = new XMLSerializer().serializeToString(diagram.contentDocument);
        task.value = "{$ui:ZIP}";
        document.forms.data.target = "_blank";
        document.forms.data.frame.value = "diagram";
        document.forms.data.submit();
      }}
    }}

    function setColor()
    {{
      var rgb = document.getElementsByName("rgb")[0];
      document.getElementsByName("color")[0].value = rgb.value;
      preview();
    }}

    function colorCode(colorSpec)
    {{
      colorSpec = colorSpec.trim();
      var code = css3colors[colorSpec.toLowerCase()];
      return typeof code == "undefined" ? colorSpec : code;
    }}

    function setRgb(colorSpec, propagate)
    {{
      colorSpec = colorSpec.trim();
      var code = colorCode(colorSpec);
      if (code.match(/^#[0-9a-f]{{6}}($|[^0-9a-f])/i))
      {{
        document.getElementsByName("rgb")[0].value = code != colorSpec ? colorSpec.toLowerCase() : colorSpec.substring(0, 7);
        var hue = document.getElementsByName("hue")[0];
        if (propagate || hue.value.trim() === "")
        {{
          var hsl = colorToHsl(code);
          hue.value = hsl[0];
          document.getElementsByName("saturation")[0].value = Math.round(hsl[1] * 100);
          document.getElementsByName("lightness")[0].value = Math.round(hsl[2] * 100);
        }}
        setColor();
      }}
      else
      {{
        alert("invalid value: " + colorSpec);
        colorSpec = document.getElementsByName("color")[0].value;
        document.getElementsByName("rgb")[0].value = colorSpec;
      }}
    }}

    function checkSpread()
    {{
      var input = document.getElementsByName("spread")[0];
      var value = input.value.trim();
      if (isNaN(value) || ! (value &gt;= 0 &amp;&amp; value &lt;= 359))
      {{
        alert("invalid value: " + input.value);
        input.value = input.oldvalue;
      }}
      else
      {{
        input.oldvalue = input.value;
        setRgbFromHsl('spread', 0, 0, 359);
      }}
    }}

    function checkWidth()
    {{
      var input = document.getElementById("width");
      var value = input.value.trim();
      if (isNaN(value) || ! (value &gt;= 0))
      {{
        alert("invalid value: " + input.value);
        input.value = input.oldvalue;
      }}
      else
      {{
        input.oldvalue = input.value;
        setCookies();
        changeText();
      }}
    }}

    var incrementInterval = null;

    function scheduleIncrement(name, inc, min, max)
    {{
      cancelIncrement();
      var time = new Date().getTime() + 500;
      increment = function()
      {{
        if (new Date().getTime() &gt;= time)
        {{
          setRgbFromHsl(name, inc, min, max);
        }}
      }};
      setRgbFromHsl(name, inc, min, max);
      incrementInterval = setInterval(increment, 100);
    }}

    function cancelIncrement()
    {{
      if (incrementInterval != null)
      {{
        clearInterval(incrementInterval);
        incrementInterval = null;
      }}
    }}

    function setRgbFromHsl(name, increment, min, max)
    {{
      var node = document.getElementsByName(name)[0];
      var value = Number(node.value)
      if (value !== "NaN" &amp;&amp;
          value + increment &gt;= min &amp;&amp;
          value + increment &lt;= max)
      {{
        node.value = increment == 0 ? value + increment : Math.round(value + increment);
        var h = document.getElementsByName("hue")[0].value;
        var s = document.getElementsByName("saturation")[0].value / 100;
        var l = document.getElementsByName("lightness")[0].value / 100;
        setRgb(rgb(hslToRgb(h, s, l)));
      }}
      else if (increment == 0)
      {{
        alert("invalid value: " + node.value);
        setRgb(document.getElementsByName("color")[0].value, true);
      }}
      else
      {{
        cancelIncrement();
      }}
    }}

    function colorToHsl(color)
    {{
      var c = parseInt(color.substring(1), 16);
      var r = c >> 16;
      var g = (c >> 8) &amp; 0xff;
      var b = c &amp; 0xff;
      return rgbToHsl(r, g, b);
    }}

    function preview()
    {{
      var color = colorCode(document.getElementsByName("color")[0].value);
      var html = document.getElementById("preview-html-1").value;
      if (document.getElementById("{$ui:SHOWEBNF}").checked)
      {{
        html += document.getElementById("preview-html-2").value;
      }}
      html += document.getElementById("preview-html-3").value;
      document.getElementById("preview").innerHTML = replaceColors(html, colorToHsl(color));
      setCookies();
    }}

    function setCookies()
    {{
      var optionsString = "";
      var options = document.getElementsByName("options");
      for (var i = 0; i &lt; options.length; ++i)
      {{
        if (options[i].checked)
        {{
          if (optionsString !== "")
          {{
            optionsString += "+";
          }}
          optionsString += options[i].value;
        }}
      }}
      var date = new Date();
      date.setTime(date.getTime() + (30 * 24 * 60 * 60 * 1000));
      var attributes = "; SameSite=Lax; Expires=" + date.toGMTString() + "; path={replace(webapp:request-uri(), '/+', '/')}";
      document.cookie = "options=" + optionsString + attributes;
      var color = document.getElementsByName("rgb")[0].value;
      document.cookie = "color=" + color + attributes;
      var width = document.getElementById("width").value;
      document.cookie = "width=" + width + attributes;
      var spread = document.getElementById("spread").value;
      document.cookie = "spread=" + spread + attributes;
    }}

    function rgbToHsl(r, g, b)
    {{
      r /= 255, g /= 255, b /= 255;
      var max = Math.max(r, g, b), min = Math.min(r, g, b);
      var h, s, l = (max + min) / 2;

      if (max == min)
      {{
        h = s = 0;
      }}
      else
      {{
        var d = max - min;
        s = l > 0.5 ? d / (2 - max - min) : d / (max + min);
        switch(max)
        {{
        case r: h = (g - b) / d + (g &lt; b ? 6 : 0); break;
        case g: h = (b - r) / d + 2; break;
        case b: h = (r - g) / d + 4; break;
        }}
        h *= 60;
      }}
      return [Math.round(h), Math.round(s * 100) / 100, Math.round(l * 100) / 100];
    }}

    function hue2rgb(p, q, t)
    {{
      if (t &lt; 0) t += 1;
      if (t > 1) t -= 1;
      if (t &lt; 1/6) return p + (q - p) * 6 * t;
      if (t &lt; 1/2) return q;
      if (t &lt; 2/3) return p + (q - p) * (2/3 - t) * 6;
      return p;
    }}

    function hslToRgb(h, s, l)
    {{
      var r, g, b;

      if (s == 0)
      {{
        r = g = b = l;
      }}
      else
      {{
        h /= 360;
        var q = l &lt; 0.5 ? l * (1 + s) : l + s - l * s;
        var p = 2 * l - q;
        r = hue2rgb(p, q, h + 1/3);
        g = hue2rgb(p, q, h);
        b = hue2rgb(p, q, h - 1/3);
      }}
      return [Math.round(r * 255), Math.round(g * 255), Math.round(b * 255)];
    }}

    function convertColor(oldColor, newHsl)
    {{
      var spread = Number(document.getElementsByName("spread")[0].value);

      if (spread !== 0)
      {{
        if (oldColor === "{s:color-regexp($style:default-color, 0)}")
        {{
          newHsl = [(newHsl[0] + 2 * spread) % 360, newHsl[1], newHsl[2]];
          oldColor = "{$style:default-color}";
        }}
        else if (oldColor === "{s:color-text-regexp($style:default-color, 0)}")
        {{
          newHsl = [(newHsl[0] + 2 * spread) % 360, newHsl[1], newHsl[2]];
          oldColor = "{s:color-text-terminal($style:default-color)}";
        }}
        else if (oldColor === "{s:color-nonterminal($style:default-color, 0)}")
        {{
          newHsl = [(newHsl[0] + spread) % 360, newHsl[1], newHsl[2]];
          oldColor = "{$style:default-color}";
        }}
        else if (oldColor === "{s:color-text-nonterminal($style:default-color, 0)}")
        {{
          newHsl = [(newHsl[0] + spread) % 360, newHsl[1], newHsl[2]];
          oldColor = "{s:color-text-terminal($style:default-color)}";
        }}
      }}

      var c = parseInt(oldColor.substring(1), 16);
      var r = c >> 16;
      var g = (c >> 8) &amp; 0xff;
      var b = c &amp; 0xff;
      var oldHsl = rgbToHsl(r, g, b);

      var h = newHsl[0];
      var s = newHsl[1] * oldHsl[1];
      if (oldHsl[2] &lt; 0.061 &amp;&amp; oldHsl[2] &gt; 0.039)
      {{
        var backgroundLightness = oldHsl[2] &gt; 0.055 ? 0.89
                                : oldHsl[2] &gt; 0.045 ? 0.81
                                :                        {$c:default-lightness}
        var backgroundBrightness = brightness(hslToRgb(h, s, convertLightness(backgroundLightness, newHsl[2], 0, 1)));
        var dark = hslToRgb(h, s, oldHsl[2]);
        var light = hslToRgb(h, s, 0.94);
        return Math.abs(brightness(dark) - backgroundBrightness) > Math.abs(brightness(light) - backgroundBrightness)
             ? rgb(dark)
             : rgb(light);
      }}
      else
      {{
        var l = oldHsl[2] &lt; 0.5
              ? convertLightness(oldHsl[2], newHsl[2], oldHsl[2] - 0.01, oldHsl[2] + 0.05)
              : oldHsl[2] &gt; 0.9 || oldHsl[1] &lt; 0.9
              ? convertLightness(oldHsl[2], newHsl[2], oldHsl[2] - 0.07, oldHsl[2] + 0.03)
              : convertLightness(oldHsl[2], newHsl[2], 0, 1);
        return rgb(hslToRgb(h, s, l));
      }}
    }}

    function convertLightness(oldL, newL, min, max)
    {{
      var factor = max - min;
      oldL = (oldL - min) / factor;
      var result = newL &lt;= {$c:default-lightness}
            ? oldL / {$c:default-lightness} * newL
            : newL + (oldL - {$c:default-lightness}) / (1 - {$c:default-lightness}) * (1 - newL);
      return min + result * factor;
    }}

    function rgb(rgb)
    {{
      var r = rgb[0];
      var g = rgb[1];
      var b = rgb[2];
      var hsl = rgbToHsl(r, g, b);
      var h = hsl[0];
      var s = hsl[1];
      var l = hsl[2];
      return "#" + (0x1000000 | (r &lt;&lt; 16) | (g &lt;&lt; 8) | b).toString(16).substring(1).toUpperCase(){
        if ($c:debug) then
           ' + " /* hsl(" + h + "," + Math.round(s * 100) / 100 + "," + Math.round(l * 100) / 100 + ") */"'
        else
          ()
      };
    }}

    function replaceColors(string, newHsl)
    {{
      var newString = null;
      var parts = string.split(/#/);
      for (var i in parts)
      {{
        var part = parts[i];
        if (newString == null)
          newString = part;
        else if (! part.match(/^[0-9a-f]{{6}}/i))
          newString += "#" + part;
        else
          newString += convertColor("#" + part.substring(0, 6), newHsl) + part.substring(6);
      }}
      return newString;
    }}

    function brightness(rgb)
    {{
      return (rgb[0] * 299 + rgb[1] * 587 + rgb[2] * 114) / 1000;
    }}

    function colorDifference(rgb1, rgb2)
    {{
      return (Math.max(rgb1[0], rgb2[0]) - Math.min(rgb1[0], rgb2[0]))
           + (Math.max(rgb1[1], rgb2[1]) - Math.min(rgb1[1], rgb2[1]))
           + (Math.max(rgb1[2], rgb2[2]) - Math.min(rgb1[2], rgb2[2]));
    }}

    function reset(toDefault)
    {{
      var changed = false;
      if (toDefault)
      {{
        document.getElementById("{$ui:SHOWEBNF}").checked = true;
        document.getElementById("{$ui:ELIMINATERECURSION}").checked = true;
        document.getElementById("{$ui:FACTORING}").checked = true;
        document.getElementById("{$ui:INLINE}").checked = true;
        setRgb("{$style:default-color}", true);
        document.getElementById("width").value = {$s:page-width};
        document.getElementsByName("spread")[0].value = 0;
      }}
      else
      {{
        document.getElementById("{$ui:SHOWEBNF}").checked = {ui:options() = $ui:SHOWEBNF};
        document.getElementById("{$ui:ELIMINATERECURSION}").checked = {ui:options() = $ui:ELIMINATERECURSION};
        document.getElementById("{$ui:FACTORING}").checked = {ui:options() = $ui:FACTORING};
        document.getElementById("{$ui:INLINE}").checked = {ui:options() = $ui:INLINE};
        setRgb(oldColor, true);
        document.getElementById("width").value = {ui:parameter-values("width", string($s:page-width))};
        document.getElementsByName("spread")[0].value = oldSpread;
      }}
      setCookies();
      changeText();
    }}
  </script>
};

(:~
 : Return content of WELCOME tab
 :
 : @return the div element containing the WELCOME tab content.
 :)
declare function ui:welcome-tab() as element(xhtml:div)
{
  <div style="display:block; max-width: 768px; overflow: hidden;" xmlns="http://www.w3.org/1999/xhtml">
    <p>
      <b>Welcome to Railroad Diagram Generator!</b>
    </p>
    <p>
      This is a tool for creating
      <a target="_blank" href="https://en.wikipedia.org/wiki/Syntax_diagram">syntax diagrams</a>,
      also known as railroad diagrams, from
      <a target="_blank" href="https://en.wikipedia.org/wiki/Context-free_grammar">context-free grammars</a>
      specified in
      <a target="_blank" href="https://en.wikipedia.org/wiki/EBNF">EBNF</a>. Syntax diagrams have
      been used for decades now, so the concept is well-known, and some tools for diagram generation are
      in existence. The features of this one are
      <ul>
        <li>usage of the <a target="_blank" href="https://www.w3.org/">W3C</a>'s EBNF notation,</li>
        <li>web-scraping of grammars from W3C specifications,</li>
        <li>online editing of grammars,</li>
        <li>diagram presentation in <a target="_blank" href="https://www.w3.org/Graphics/SVG/">SVG</a>,</li>
        <li>
          and it was completely written in web languages
          (<a target="_blank" href="https://en.wikipedia.org/wiki/XQuery">XQuery</a>,
          <a target="_blank" href="https://en.wikipedia.org/wiki/XHTML">XHTML</a>,
          <a target="_blank" href="https://en.wikipedia.org/wiki/Cascading_Style_Sheets">CSS</a>,
          <a target="_blank" href="https://en.wikipedia.org/wiki/JavaScript">JavaScript</a>).
        </li>
      </ul>
    </p>
    <b>Notation</b>
    <p>
      For the original description of the EBNF notation as it is used here, please refer to
      "<a target="_blank" href="https://www.w3.org/TR/2010/REC-xquery-20101214/#EBNFNotation">A.1.1 Notation</a>"
      in the <a target="_blank" href="https://www.w3.org/TR/2010/REC-xquery-20101214/">XQuery recommendation</a>. The
      <a target="_blank" href="https://www.w3.org/TR/xml/">XML recommendation</a> contains a
      similar section, "<a target="_blank" href="https://www.w3.org/TR/xml/#sec-notation">6 Notation</a>".
      Below is a self-describing grammar for the EBNF notation.
    </p>
    <p><div class="grammar">{e:notation()}</div></p>
    <p>
      For viewing railroad diagrams of this very grammar, either
      <ul>
        <li>select "EBNF Notation" from the "Get Grammar" tab,</li>
        <li>or copy and paste the above grammar to the "Edit Grammar" tab,</li>
      </ul>
      and then proceed to the "View Diagram" tab. Or just click
      <a href="javascript:selfDescribe()">here</a> for a shortcut.
    </p>
    <b>Download</b>
    <p>
      This application can be run offline, both browser-based, and as a command-line application. It comes as a Java executable
      archive and can be run with Java {$s:java-version} (or higher).
    </p>
    <p>
      This is the download link: <a href="download/rr-{$s:version}-java{$s:java-version}.zip"><span id="download-link"></span></a>
      <script type="text/javascript">
        var url = document.location.href;
        var link = document.getElementById("download-link");
        link.innerHTML = url.substring(0, url.lastIndexOf("/") + 1) + "download/rr-{$s:version}-java{$s:java-version}.zip";
      </script>
    </p>
    <b>Source Code</b>
    <p>
      The source code of this application is available on GitHub: <a href="https://github.com/GuntherRademacher/rr">https://github.com/GuntherRademacher/rr</a>
    </p>
  </div>
};

(:~
 : Return spinner to be shown while waiting for server requests.
 :
 : @param $color the color code.
 : @return a sequence of XHTML and SVG elements making up the spinner.
 :)
declare function ui:spinner($color as xs:string)
{
  let $svg-width := 100
  let $svg-height := 100

  let $sectors := 36
  let $colors := $sectors idiv 3 * 2

  let $l0 := 0.2
  let $l1 := 0.90
  let $l2 := ($l1 - $l0) div $colors

  let $sector-color :=
    for $i in 1 to $sectors
    let $sector := $sectors + 1 - $i
    return
      if ($sector > $colors) then
        c:relative-color($color, 0.7, $l1)
      else
        c:relative-color($color, 0.7, $l0 + ($sector - 1) * $l2)

  let $r1 := ($svg-width - 2) div 2
  let $r2 := ($svg-width - 2) div 5
  let $x0 := 1 + $r1
  let $y0 := 1 + $r1
  let $angle := 360 div $sectors
  let $stroke := $angle * 0.25

  return
  (
    <div id="greyout" xmlns="http://www.w3.org/1999/xhtml" style="display:none" onclick="this.style.display = 'none'"/>,
    <svg id="spinner" xmlns="http://www.w3.org/2000/svg" width="{$svg-width}" height="{$svg-height}" style="display:none">
        {
          for $sector in 1 to $sectors
          let $a1 := (($sector - 1) * $angle + $stroke) div 180 * math:pi()
          let $a2 := ($sector * $angle - $stroke) div 180 * math:pi()
          let $s1 := math:sin($a1)
          let $c1 := math:cos($a1)
          let $s2 := math:sin($a2)
          let $c2 := math:cos($a2)
          let $x1 := $x0 + $r1 * $s1
          let $y1 := $y0 - $r1 * $c1
          let $x2 := $x0 + $r1 * $s2
          let $y2 := $y0 - $r1 * $c2
          let $x3 := $x0 + $r2 * $s2
          let $y3 := $y0 - $r2 * $c2
          let $x4 := $x0 + $r2 * $s1
          let $y4 := $y0 - $r2 * $c1
          return
            <path id="sector"
                  d="M {$x1},{$y1}
                     A {$r1},{$r1} 0 0 1 {$x2},{$y2}
                     L {$x3},{$y3}
                     A {$r2},{$r2} 0 0 0 {$x4},{$y4}
                     Z"
              style="fill: {$sector-color[$sector]}; stroke: none;"/>
        }
    </svg>,
    <script type="text/javascript" xmlns="http://www.w3.org/1999/xhtml">
      var colors = ["{string-join($sector-color, """, """)}"];
      var sectors = document.getElementById("spinner").getElementsByTagNameNS("http://www.w3.org/2000/svg", "path");
      var spinning = 0;
      var greyout = document.getElementById("greyout");
      var spinner = document.getElementById("spinner");

      function resizeSpinner()
      {{
        if (spinning &gt; 0)
        {{
          var height = window.innerHeight;
          greyout.setAttribute("style", "background-color: {c:relative-color($color, 1.0, 0.91)}; opacity: 0.7; height: " + height + "px; width: 100%; position:absolute; top: 0px; left: 0px;");

          var left = Math.round(window.pageXOffset + (window.innerWidth - {$svg-width}) / 2);
          var top = Math.round(window.pageYOffset + (window.innerHeight - {$svg-height}) / 2);
          spinner.style.position = "absolute";
          spinner.style.top = top + "px";
          spinner.style.left = left + "px";
          spinner.style.display = "";
        }}
      }}

      function startSpinning()
      {{
        if (spinning == 0)
        {{
          spin(0);
        }}
      }}

      function stopSpinning()
      {{
        if (spinning)
        {{
          spinning = -1;
        }}
      }}

      function spin(color)
      {{
        if (spinning &lt; 0)
        {{
          spinning = 0;
          spinner.style.display = "none";
          greyout.style.display = "none";
        }}
        else
        {{
          if (spinning == 0)
          {{
            spinning = 1;
            resizeSpinner();
          }}
          for (var s = {$sectors - 1}; s >= 0; --s)
          {{
            sectors[(color + s) % {$sectors}].setAttribute("style", "fill:" + colors[s] + "; opacity: 0.6");
          }}
          window.setTimeout("spin((" + (color + 1) + ") % {$sectors})", {40});
        }}
      }}
    </script>
  )
};

(:~
 : Perform the "SAVE" task.
 :
 : @return the exitor text content as a plain text result, along with
 : appropriate HTTP header settings identifying it as an attachment.
 :)
declare function ui:save() as xs:string
{
  webapp:set-content-type("text/plain"),
  webapp:set-header("Content-Disposition", concat("attachment; filename=", ui:name("grammar", ".ebnf"))),
  webapp:set-serialization-parameters
  (
    <output:serialization-parameters xmlns:output="http://www.w3.org/2010/xslt-xquery-serialization">
      <output:encoding value="UTF-8"/>
      <output:indent value="no"/>
      <output:method value="text"/>
      <output:omit-xml-declaration value="yes"/>
      <output:version value="1.0"/>
    </output:serialization-parameters>
  ),
  webapp:parameter-values("text")
};

(:~
 : Process an "EDIT" or "VIEW" task. If there is a non-empty HTTP "uri"
 : parameter, extract grammar from that URI. Get syntax diagram, when
 : processing a "VIEW" task. Return user interface page equipped appropriately.
 :
 : @param $task the task to be processed.
 : @param $color the color code.
 : @return the user interface XHTML page containing the required
 : information.
 :)
declare function ui:process($task, $color) as element()
{
  let $uri := webapp:parameter-values("uri")
  let $ebnf :=
    if (not($uri != "")) then
      text {(webapp:parameter-values("text"), webapp:parameter-values("ebnf"))[1]}
    else if (ends-with(replace($uri, "[#?].*$", ""), replace(webapp:request-uri(), '/+', '/'))) then
      e:extract($uri, e:notation(), xs:integer(webapp:parameter-values("tz")))
    else if (not(matches($uri, "^https?://.*"))) then
      element xhtml:pre
      {
        concat("error: invalid URI: ", $uri),
        '&#xA;Only http or https is supported.'
      }
    else
      let $response := http-client:send-request((), $uri)
      return
        if (not($response[1]/@status = 200)) then (: check HTTP OK status :)
        (
          element xhtml:pre
          {
            "error: received response",
            data($response[1]/@status),
            "from HTTP GET request (URI:",
            concat($uri, "):")
          },
          for $r in $response
          return
          (
            <br/>,
            element xhtml:code
            {
              if ($r instance of node()) then
                ui:serialize($r)
              else
                $r
            }
          )
        )
        else if (not($response[2] instance of node())) then
          element xhtml:pre
          {
            "error: there is no XML or HTML document at", $uri,
            '&#xA;For processing a plain grammar link, proceed to the "Edit Grammar" tab and use "Load".'
          }
        else
          e:extract($uri, $response[2], xs:integer(webapp:parameter-values("tz")))
  let $precomputed :=
    if ($ebnf/self::xhtml:pre) then
      ()
    else
      ()
  let $svg :=
    if ($ebnf/self::xhtml:pre) then
      $ebnf
    else if (exists($precomputed)) then
      ()
    else if ($task != $ui:VIEW) then
      ()
    else if (not(webapp:parameter-values("frame") = "diagram")) then
      ()
    else
      ui:parse($ebnf, $color)
  return
    if ($svg/self::xhtml:pre) then
      ui:html($ui:VIEW, $ebnf[not($ebnf/self::xhtml:pre)], false(), $svg, $precomputed, $color)
    else
      ui:html($task[1], $ebnf, $ebnf != "" and empty($svg) and empty($precomputed), $svg, $precomputed, $color)
};

(:~
 : Determine name of current grammar from the "uri" parameter,
 : the "file" parameter, or the "name" parameter, in this order,
 : whichever is found non-empty first.
 :
 : @return the name.
 :)
declare function ui:name($default-name, $forced-extension) as xs:string?
{
  let $name :=
  (
    let $uri := webapp:parameter-values("uri")
    where $uri != ""
    return replace($uri, "^.*/([^/]+)/?$", "$1"),
    if (webapp:method-get()) then
      ()
    else if (webapp:part-names() = "file") then
      tokenize(translate(webapp:part("file")/webapp:body/@webapp:filename, "\", "/"), "/")[last()]
    else
      (),
    webapp:parameter-values($ui:NAME),
    $default-name
  )[. != ""][1]
  return
    if ($forced-extension) then
      concat(replace($name, "\.[^.]*$", ""), $forced-extension)
    else
      $name
};

(:~
 : Parse grammar, convert concrete syntax tree to AST, and AST
 : to SVG.
 :
 : @param $ebnf the grammar as a string.
 : @param $color the color code.
 : @return the SVG result, or an xhtml:pre node in case of any errors.
 :)
declare function ui:parse($ebnf as xs:string, $color as xs:string) as node()+
{
  let $parse-tree := p:parse-Grammar($ebnf)
  return
    if ($parse-tree instance of element(Grammar)) then
      let $options := ui:options()
      let $grammar :=
        t:transform
        (
          a:ast($parse-tree),
          if ($options = $ui:ELIMINATERECURSION) then "full" else "none",
          if ($options = $ui:FACTORING) then "full-left" else "none",
          $options = $ui:INLINE,
          $options = $ui:KEEP
        )
      return
        s:svg                                 (: convert to AST, then to SVG :)
        (
          $grammar,
          $options = $ui:SHOWEBNF,
          xs:integer(webapp:parameter-values("width")),
          $color,
          xs:integer((webapp:parameter-values("spread"), "0")[.][1]),
          false(),
          (
            webapp:parameter-values("myUri0")[.],
            "https://www.bottlecaps.de/rr/ui"
          )[1]
        )
    else
      element xhtml:pre                       (: report error :)
      {
        "error:"[not($parse-tree instance of element(ERROR))],
        data($parse-tree)
      }
};

(:~
 : Perform the "LOAD" task. Get content of "file" HTTP
 : parameter, check for valid encoding and return the user
 : interface page with the content installed in the
 : editor.
 :
 : @param $color the color code.
 : @return the uses interface XHTML page.
 :)
declare function ui:load($color as xs:string) as element(xhtml:html)
{
  let $filename := string(webapp:part("file")/webapp:body/@webapp:filename)
  let $text := ui:decode-base64(webapp:binary-file($filename))
  return
    if (ui:has-valid-encoding($text)) then
      ui:html($ui:EDIT, ui:strip-bom($text), true(), (), (), $color)
    else
      ui:html($ui:VIEW, (), false(), <xhtml:pre>error: unsupported encoding</xhtml:pre>, (), $color)
};

(:~
 : Check whether a given string has all valid codepoints.
 :
 : @param $content the string.
 : @return true, if the string has only valid codepoints.
 :)
declare function ui:has-valid-encoding($content as xs:string) as xs:boolean
{
  empty(string-to-codepoints($content)[not(. = (9, 10, 13)
                                           or . >= 20 and . <= 55295
                                           or . >= 57344 and . <= 65533
                                           or . >= 65536 and . <= 1114111)])
};

(:~
 : Strip an UTF-16 byte order mark from the beginning of a string. A byte
 : order mark may result from uploading some file (most probably from
 : a Windows client).
 :
 : @param $s the string.
 : @return the string, freed from a leading byte order mark, if one was
 : present.
 :)
declare function ui:strip-bom($s as xs:string) as xs:string
{
  if (starts-with($s, "&#xFEFF;")) then
    substring($s, 2)
  else
    $s
};

(:~
 : Construct this application's logo.
 :
 : @return an xhtml:div element containing the logo.
 :)
declare function ui:logo() as element(xhtml:div)
{
  <div style="position:absolute; right:5px; top:0px;" xmlns="http://www.w3.org/1999/xhtml">
    <table border="0">
      <tr>
        <td class="small"><span class="hidden">response time</span><br/><span id="timeDisplay" class="hidden">will be shown here</span></td>
        <td style="font-weight:bold; font-family:Arial, Sans-serif; line-height:130%; padding-top: 2px;">
          <span style="font-size: 25px;">Railroad</span><br/>
          <span style="font-size: 25px;">Diagram</span><br/>
          <span style="font-size: 21px;">Generator</span>
        </td>
        <td>{s:traffic-sign(70)}</td>
        <td class="small">
          v{$s:version}
          <br />built {$s:date}
          <br />
          <br />written by
          <br />Gunther Rademacher
          <br /><a href="mailto:grd@gmx.net" title="grd@gmx.net">grd@gmx.net</a>
        </td>
      </tr>
    </table>
  </div>
};

(:~
 : Get base color from http parameter or cookie.
 :
 : @return the base color.
 :)
declare function ui:color() as xs:string
{
  let $color := normalize-space(ui:parameter-values("color", ())[1])
  return
    if (exists(c:color-code($color))) then
      $color
    else
      $style:default-color
};

(:~
 : The color argument for get URIs.
 :
 : @param $delimiter the delimiter to be prepended to the result string.
 : @return the color argument string.
 :)
declare function ui:color-arg($delimiter as xs:string) as xs:string?
{
  let $color := ui:color()
  where not($color eq $style:default-color)
  return
    if (starts-with($color, "#")) then
      concat($delimiter, "color=%23", substring($color, 2))
    else
      concat($delimiter, "color=", string($color))
};

(:~
 : Set content-type of response to XHTML.
 :
 : @return empty.
 :)
declare function ui:xhtml($indent as xs:boolean) as item()*
{
  webapp:set-content-type("application/xhtml+xml"),
  webapp:set-serialization-parameters
  (
    <output:serialization-parameters xmlns:output="http://www.w3.org/2010/xslt-xquery-serialization">
      <output:encoding value="UTF-8"/>
      <output:indent value="{if ($indent) then "yes" else "no"}"/>
      <output:method value="xhtml"/>
      <output:omit-xml-declaration value="yes"/>
      <output:version value="1.0"/>
      <output:doctype-system value="http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd"/>
      <output:doctype-public value="-//W3C//DTD XHTML 1.0 Transitional//EN"/>
    </output:serialization-parameters>
  )
};

(:~
 : Download diagram as XHTML. The diagram is passed in HTTP parameter xhtml in
 : unparsed XHTML.
 :
 : @return the diagram as parsed XHTML.
 :)
declare function ui:download-xhtml() as xs:string
{
  webapp:set-serialization-parameters
  (
    <output:serialization-parameters xmlns:output="http://www.w3.org/2010/xslt-xquery-serialization">
      <output:encoding value="UTF-8"/>
      <output:method value="text"/>
    </output:serialization-parameters>
  ),
  webapp:set-content-type("application/xhtml+xml"),
  webapp:set-header("Content-Disposition", concat("attachment; filename=", ui:name("diagram", ".xhtml"))),
  webapp:parameter-values("xhtml")
};

(:~
 : Download diagram as Markdown. The diagram is passed in HTTP parameter xhtml as
 : unparsed XHTML.
 :
 : @return the diagram as Markdown with inline SVG.
 :)
declare function ui:download-md() as xs:string
{
  webapp:set-serialization-parameters
  (
    <output:serialization-parameters xmlns:output="http://www.w3.org/2010/xslt-xquery-serialization">
      <output:encoding value="UTF-8"/>
      <output:method value="text"/>
    </output:serialization-parameters>
  ),
  webapp:set-content-type("text/markdown; charset=UTF-8"),
  webapp:set-header("Content-Disposition", concat("attachment; filename=", ui:name("diagram", ".md"))),
  m:transform(ui:parse-xml(webapp:parameter-values("xhtml")[1]))
};

(:~
 : Download zipped PNG diagrams. The diagram is passed in HTTP parameter xhtml in
 : unparsed XHTML.
 :
 : @return the zip file.
 :)
declare function ui:download-zip()
{
  webapp:set-content-type("application/zip"),
  webapp:set-header("Content-Disposition", concat("attachment; filename=", ui:name("diagram", ".zip"))),
  ui:xhtml-to-zip(webapp:parameter-values("xhtml")[1])
};

(:~
 : Serialize node sequence to string.
 :
 : @param $nodes the node sequence.
 : @return the string.
 :)
declare function ui:serialize($nodes as node()*) as xs:string
{
  string-join
  (
    for $node in $nodes
    return
      typeswitch ($node)
      case document-node() return
        ("<?xml version=""1.0""?>", ui:serialize($node/node()))
      case element() return
        (
          "<", string(node-name($node)),
          for $p in in-scope-prefixes($node)
          let $parent := $node/parent::*
          where $p != "xml"
            and (empty($parent) or not(namespace-uri-for-prefix($p, $node) eq namespace-uri-for-prefix($p, $parent)))
          order by $p
          return
          (
            " xmlns", ":"[$p], $p[.],
            "=""",
            namespace-uri-for-prefix($p, $node),
            """"
          ),
          ui:serialize($node/@*),
          if (empty($node/node())) then
            "/>"
          else
            (">", ui:serialize($node/node()), "</", string(node-name($node)), ">")
        )
      case attribute() return
        (" ", string(node-name($node)), "=""", replace(string($node), """", "&amp;quot:"), """")
      case processing-instruction() return
        ("<?", local-name($node), if (string($node) = "") then () else (" ", string($node)), "?>")
      case comment() return
        ("<!--", string($node), "-->")
      default return
        replace(replace($node, "<", "&amp;lt;"), ">", "&amp;gt;"),
    ""
  )
};

(:~
 : Get options parameter values from HTTP servlet request or cookie.
 :
 : @param $name the parameter name.
 : @return the parameter values.
 :)
declare function ui:options() as xs:string*
{
  ui:parameter-values("options", ($ui:SHOWEBNF, $ui:ELIMINATERECURSION, $ui:FACTORING, $ui:INLINE, $ui:KEEP))
};

(:~
 : Get parameter values from HTTP servlet request or cookie.
 :
 : @param $name the parameter name.
 : @param $default the default value.
 : @return the parameter values.
 :)
declare function ui:parameter-values($name as xs:string, $default as xs:string*) as xs:string*
{
  let $values := webapp:parameter-values($name)
  return
    if (exists($values)) then
      $values
(:
    else if (not(webapp:method-get())) then
      $default
:)
    else
      let $values := webapp:get-cookie($name)[1]
      return
        if (exists($values)) then
          tokenize($values, "\+")
        else
          $default
};
