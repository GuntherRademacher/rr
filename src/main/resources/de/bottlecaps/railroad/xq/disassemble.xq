module namespace d="de/bottlecaps/railroad/xq/disassemble.xq";

declare namespace svg="http://www.w3.org/2000/svg";
declare namespace xhtml="http://www.w3.org/1999/xhtml";
declare namespace xlink="http://www.w3.org/1999/xlink";
declare namespace xsl="http://www.w3.org/1999/XSL/Transform";

declare function d:file($name as xs:string, $type, $content)
{
  element file
  {
    attribute name {$name},
    attribute type {$type},
    $content
  }
};

declare function d:rewrite($nodes, $format, $hotspots as xs:boolean, $referenced-by as xs:boolean)
{
  for $node in $nodes
  return
    typeswitch ($node)
    case document-node() return
      document{d:rewrite($node/node(), $format, $hotspots, $referenced-by)}
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
            string(count($node/preceding::svg:svg) + 1)
        let $img-name := concat("diagram/", $name, ".", $format)
        return
        (
          d:file
          (
            $img-name,
            "xml",
            element {node-name($node)}
            {
              $node/@*,
              $node/preceding::xhtml:head/svg:svg/svg:defs,
              $node/node()
            }
          ),
          let $map-name := concat($name, ".map")
          let $links := $node//svg:a
          let $map :=
            element map
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
                element area
                {
                  attribute shape {"rect"},
                  attribute coords {string-join(($x1, $y1, string($x2), string($y2)), ",")},
                  attribute href {$link/@xlink:href},
                  attribute title {$target},
                  $link/@target
                }
            }
          let $img :=
            element img
            {
              attribute border {0},
              attribute src {$img-name},
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
                <map id="{$name}" name="{$name}">{
                  for $link in $links
                  let $rect := ($link//svg:rect)[last()]
                  let $x1 := $rect/@x
                  let $x2 := $rect/@x + $rect/@width
                  let $y1 := $rect/@y
                  let $y2 := $rect/@y + $rect/@height
                  let $target := data($link//svg:text)
                  return
                    <area>{
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
                    }</area>
                }</map>
              return d:file($map-path, "xhtml", $mapfile)
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
                <ul>{
                  for $a in $referenced-by-links
                  let $referrer := data($a/@title)
                  return <li><a href="ref-{$referrer}.htm" title="{$referrer}">{$referrer}</a></li>
                }</ul>
              return d:file($referenced-by-path, "xhtml", $referenced-by)
            )
          )
        )
    case element() return
      element {local-name($node)}
      {
        $node/@*,
        d:rewrite($node/node(), $format, $hotspots, $referenced-by)
      }
    default return
      $node
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

declare function d:disassemble($input, $format)
{
  element files
  {
    let $directive := $input//processing-instruction()[local-name() = "rr"]/tokenize(., "\s+")[.]
    let $hotspots := $directive = "hotspots"
    let $referenced-by := $directive = "referenced-by"
    let $files := d:file("index.html", "xml", d:rewrite($input, $format, $hotspots, $referenced-by))
    for $file in $files/descendant-or-self::file
    order by lower-case($file/@name)
    return element file {$file/@*, d:unnest($file/node())}
  }
};
