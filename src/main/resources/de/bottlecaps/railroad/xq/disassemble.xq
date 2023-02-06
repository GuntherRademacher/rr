module namespace d="de/bottlecaps/railroad/xq/disassemble.xq";

declare namespace svg="http://www.w3.org/2000/svg";
declare namespace xhtml="http://www.w3.org/1999/xhtml";
declare namespace xlink="http://www.w3.org/1999/xlink";
declare namespace xsl="http://www.w3.org/1999/XSL/Transform";
declare namespace output="http://www.w3.org/2010/xslt-xquery-serialization";

declare namespace xf="de/bottlecaps/railroad/core/ExtensionFunctions";

declare function d:file($name as xs:string, $content)
{
  element file
  {
    attribute name {$name},
    $content
  }
};

declare function d:squeeze-svg($nodes as node()*) as node()*
{
  for $node in $nodes
  return
    typeswitch ($node)
    case document-node() return
      document {d:squeeze-svg($node/*)}
    case element() return
      if ($node/self::svg:a) then
        d:squeeze-svg($node/node())
      else
        element {node-name($node)} {d:squeeze-svg(($node/@*, $node/node()))}
    case text() return
      if ($node/parent::svg:text) then
        $node
      else
        text {normalize-space($node)}[.]
    case attribute() return
      attribute {node-name($node)} {normalize-space($node)}
    default return
      ()
};

declare function d:data-url($content-type as xs:string, $content as xs:string) as xs:string
{
  concat("data:", $content-type, ",", encode-for-uri($content))
};

declare function d:element-name($text-format as xs:string, $local-name as xs:string) as xs:QName
{
  QName("http://www.w3.org/1999/xhtml"[$text-format eq "xhtml"], $local-name)
};

declare function d:rewrite-html($nodes as node()*, $text-format as xs:string, $img-format as xs:string, $inline as xs:boolean, $hotspots as xs:boolean, $referenced-by as xs:boolean)
{
  for $node in $nodes
  return
    typeswitch ($node)
    case document-node() return
      document{d:rewrite-html($node/node(), $text-format, $img-format, $inline, $hotspots, $referenced-by)}
    case element(svg:svg) return
      if (empty($node/ancestor::xhtml:body)) then
        ()
      else
        let $name := $node/preceding::xhtml:p[1]/(.//xhtml:a, following::xhtml:a)[1]/@name
        let $name :=
          if ($name) then
            $name
          else if (contains($node/ancestor::xhtml:td[1]/preceding-sibling::xhtml:td[1], "Railroad Diagram Generator")) then
            concat("rr-", normalize-space(substring-after(root($node)/xhtml:html/xhtml:head/xhtml:meta[@name = "generator"]/@content, "Railroad Diagram Generator")))
          else
            string(count($node/preceding::svg:svg))
        let $svg :=
          element {node-name($node)}
          {
            $node/@*,
            $node/preceding::xhtml:head/svg:svg/svg:defs,
            $node/node()
          }
        let $img-link :=
          if (not($inline)) then
            concat("diagram/", $name, ".", $img-format)
          else
            let $serialized-svg := serialize(d:squeeze-svg($svg), <output:serialization-parameters/>)
            return
              if ($img-format eq "png") then
                d:data-url("image/png;base64", xf:svg-to-png($serialized-svg))
              else
                d:data-url("image/svg+xml", $serialized-svg)
        return
        (
          if ($inline) then
            ()
          else
            d:file($img-link, $svg),
          let $map-name := concat($name, ".map")
          let $links := $node//svg:a
          let $map :=
            element {d:element-name($text-format, "map")}
            {
              attribute name {$map-name},
              for $link in $links
              let $rect := ($link//svg:rect)[last()]
              let $x1 := $rect/@x
              let $x2 := $rect/@x + $rect/@width
              let $y1 := $rect/@y
              let $y2 := $rect/@y + $rect/@height
              let $target := data($link//svg:text)
              return
                element {d:element-name($text-format, "area")}
                {
                  attribute shape {"rect"},
                  attribute coords {string-join(($x1, $y1, string($x2), string($y2)), ",")},
                  attribute href {$link/@xlink:href},
                  attribute title {$target},
                  $link/@target
                }
            }
          let $img :=
            element {d:element-name($text-format, "img")}
            {
              attribute border {0},
              attribute src {$img-link},
              $node/@height,
              $node/@width,
              attribute usemap {concat("#", $map-name)}[exists($map/area)]
            }
          return
          (
            $img,
            $map[exists($img/@usemap)],
            if (not($hotspots)) then
              ()
            else
            (
              let $map-path := concat("hotspots/", $name, ".htm")
              let $mapfile :=
                element {d:element-name($text-format, "map")}
                {
                  attribute id {$name},
                  attribute name {$name},
                  for $link in $links
                  let $rect := ($link//svg:rect)[last()]
                  let $x1 := $rect/@x
                  let $x2 := $rect/@x + $rect/@width
                  let $y1 := $rect/@y
                  let $y2 := $rect/@y + $rect/@height
                  let $target := data($link//svg:text)
                  return
                    element {d:element-name($text-format, "area")}
                    {
                      attribute shape {"rect"},
                      attribute coords {string-join(($x1, $y1, string($x2), string($y2)), ",")},
                      if (matches($rect/following-sibling::svg:text[1], "\i\c+")) then
                        attribute href
                        {
                          if (matches($target, "^[A-Za-z][-+.A-Za-z0-9]*://")) then
                            $target
                          else
                            concat("ref-", $target, ".htm")
                        }
                      else
                        attribute href {"xqr-lexical.htm"},
                      attribute title {$target},
                      attribute alt {$target}
                    }
                }
              return d:file($map-path, $mapfile)
            ),
            if (not($referenced-by)) then
              ()
            else
            (
              let $referenced-by-links :=
                if ($node/following-sibling::xhtml:p[1]/xhtml:div/@class = "ebnf") then
                  $node/following-sibling::xhtml:p[2]//xhtml:li/xhtml:a
                else
                  $node/following-sibling::xhtml:p[1]//xhtml:li/xhtml:a
              let $referenced-by-path := concat("referenced-by/", $name, ".htm")
              let $referenced-by :=
                element {d:element-name($text-format, "ul")}
                {
                  for $a in $referenced-by-links
                  let $referrer := data($a/@title)
                  return
                    element {d:element-name($text-format, "li")}
                    {
                      element {d:element-name($text-format, "a")}
                      {
                        attribute href {concat("ref-", $referrer, ".htm")},
                        attribute title {$referrer},
                        $referrer
                      }
                    }
                }
              return d:file($referenced-by-path, $referenced-by)
            )
          )
        )
    case element() return
      element {if ($text-format eq "html") then local-name($node) else node-name($node)}
      {
        $node/@*,
        d:rewrite-html($node/node(), $text-format, $img-format, $inline, $hotspots, $referenced-by)
      }
    case text() return
      $node[normalize-space(.)]
    default return
      $node
};

declare function d:rewrite-to-md($nodes as node()*, $img-format as xs:string, $inline as xs:boolean)
{
  for $node in $nodes
  return
    typeswitch ($node)
    case document-node() return
      d:rewrite-to-md($node/node(), $img-format, $inline)
    case element(svg:svg) return
      if (empty($node/ancestor::xhtml:body)) then
        ()
      else
        let $name := $node/preceding::xhtml:p[1]/(.//xhtml:a, following::xhtml:a)[1]/@name
        let $name :=
          if ($name) then
            $name
          else if (contains($node/ancestor::xhtml:td[1]/preceding-sibling::xhtml:td[1], "Railroad Diagram Generator")) then
            concat("rr-", normalize-space(substring-after(root($node)/xhtml:html/xhtml:head/xhtml:meta[@name = "generator"]/@content, "Railroad Diagram Generator")))
          else
            string(count($node/preceding::svg:svg))
        let $ebnf := string-join($node/following::xhtml:div[@class="ebnf"][1]/xhtml:code/*, "&#xA;")
          !replace(., "&#xA0;", " ")


        let $svg :=
          element {node-name($node)}
          {
            $node/@*,
            $node/preceding::xhtml:head/svg:svg/svg:defs,
            $node/node()
          }
        let $img-url :=
          if (not($inline)) then
            concat("diagram/", $name, ".", $img-format)
          else
            let $serialized-svg := serialize(d:squeeze-svg($svg), <output:serialization-parameters/>)
            return
              if ($img-format eq "png") then
                d:data-url("image/png;base64", xf:svg-to-png($serialized-svg))
              else
                d:data-url("image/svg+xml", $serialized-svg)
        let $img-link := concat("![", $name, "](", $img-url, ")")
        return
        (
          if ($inline) then
            ()
          else
            d:file($img-url, $svg),
          if (empty($node/following::svg:svg)) then
          (
            "## &#xA;",
            $img-link,
            " <sup>generated by [RR - Railroad Diagram Generator][RR]</sup>&#xA;",
            "&#xA;",
            "[RR]: http://bottlecaps.de/rr/ui"
          )
          else
          (
            "**", string($name), ":**&#xA;&#xA;",
            $img-link,
            "&#xA;&#xA;",
            if ($ebnf[.]) then
            (
              "```&#xA;",
              $ebnf,
              "&#xA;```&#xA;&#xA;"
            )
            else
              (),
            let $referenced-by-links :=
              if ($node/following-sibling::xhtml:p[1]/xhtml:div/@class = "ebnf") then
                $node/following-sibling::xhtml:p[2]//xhtml:li/xhtml:a
              else
                $node/following-sibling::xhtml:p[1]//xhtml:li/xhtml:a
            where exists($referenced-by-links)
            return
            (
              "referenced by:&#xA;&#xA;",
              for $a in $referenced-by-links
              return ("* ", string($a/@title), "&#xA;"),
              "&#xA;"
            )
          )
        )
    case element() return
      d:rewrite-to-md($node/node(), $img-format, $inline)
    default return
      ()
};

declare function d:unnest($nodes as node()*) as node()*
{
  for $node in $nodes
  return
    typeswitch ($node)
    case element(file) return
      ()
    case element() return
      element {node-name($node)} {$node/@*, d:unnest($node/node())}
    default return
      $node
};

declare function d:disassemble($input as document-node(element(xhtml:html)), $text-format as xs:string, $img-format as xs:string, $inline as xs:boolean) as document-node(element(files))
{
  document
  {
    element files
    {
      if ($text-format eq "md") then
        let $files := d:rewrite-to-md($input, $img-format, $inline)
        return
        (
          d:file(concat("index.", $text-format), string-join($files[. instance of xs:string], "")),
          for $file in $files[. instance of element(file)]
          order by lower-case($file/@name)
          return $file
        )
      else
        let $directive := $input//processing-instruction()[local-name() = "rr"]/tokenize(., "\s+")[.]
        let $hotspots := $directive = "hotspots"
        let $referenced-by := $directive = "referenced-by"
        let $files := d:file(concat("index.", $text-format), d:rewrite-html($input, $text-format, $img-format, $inline, $hotspots, $referenced-by))
        for $file in $files/descendant-or-self::file
        order by lower-case($file/@name)
        return element file {$file/@*, d:unnest($file/node())}
    }
  }
};
