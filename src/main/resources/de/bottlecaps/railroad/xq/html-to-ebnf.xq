(:~
 : This is the web scraper module that contains functions for extracting
 : grammars out of W3C specs.
 :)
module namespace e="de/bottlecaps/railroad/xq/html-to-ebnf.xq";

declare namespace xhtml="http://www.w3.org/1999/xhtml";

(:~
 : Normalize space of given string. Similar to fn:normalize-space, but
 : extend whitespace characters by the HTML &amp;nbsp; character, and
 : return empty for whitespace-only strings.
 :
 : @param $string the string to be normalized.
 : @return the normalized string, or empty, if the string was nothing
 : but whitespace.
 :)
declare function e:normalize-space($string as xs:string?) as xs:string?
{
  normalize-space(replace($string, "[&#xA0;]+", " "))[.]
};

(:~
 : Extract text lines from an HTML fragment, ignoring any nodes below *:sup
 : elements (this is for suppressing superscripts in the grammar notation).
 :
 : First, translate all carriage returns by a space, and *:br nodes by a
 : carriage return. Then join all resulting element content, and split into
 : lines. Finally normalize whitespace of each line.
 :
 : @param $html the HTML fragment.
 : @return the text lines as a sequence of strings, one string per line.
 :)
declare function e:text-lines($html as node()*) as xs:string*
{
  let $unwanted-comment := $html//*:br[following-sibling::node()[1]/self::text() = "Note that "  and following-sibling::node()[2]/self::*:a]
  let $string :=
    string-join
    (
      for $node in $html//(*:br | text())[not(. is $unwanted-comment or . >> $unwanted-comment)]
            except $html/descendant-or-self::*:sup//node()
      return
        if ($node/self::*:br) then
          "&#xA;"
        else
          translate($node, "&#xA;", " "),
      ""
    )
  for $line in tokenize($string, "&#xA;")
  return e:normalize-space($line)
};

(:~
 : Ensure a sequence of strings is properly wrapped in EBNF comment
 : delimiters, i.e. /* comment content */. Unwrap any strings in
 : brackets or comment delimiters, then re-wrap the normalized
 : strings in comment delimiters.
 :
 : @param $strings the string sequence.
 : @return the string sequence, where each string was wrapped in EBNF
 : comment delimiters if necessary.
 :)
declare function e:comment($html as node()*) as xs:string*
{
  for $node in $html
  let $string :=
    string-join
    (
      for $node in $node/descendant-or-self::text()
            except $node/descendant-or-self::*:sup//text()
      return data($node),
      ""
    )
  let $normalized-string := e:normalize-space($string)
  let $unwrapped-string :=
    if (matches($normalized-string, "^/\*.*\*/$", "s")) then
      substring($normalized-string, 3, string-length($normalized-string) - 4)
    else if (matches($normalized-string, "^\[.*\]$", "s")) then
      substring($normalized-string, 2, string-length($normalized-string) - 2)
    else
      $normalized-string
  let $net-string := e:normalize-space($unwrapped-string)
  where $net-string
  return concat("/* ", replace($net-string, "\*/", "* /"), " */")
};

(:~
 : Extract EBNF definitions from an HTML document. Relevant definitions
 : are EBNF productions and HTML headers introducing terminal symbol
 : definitions.
 :
 : Productions are identified by table elements with a normalized value
 : of "::=". Starting from these, navigate left for the defined nonterminal,
 : and right and down (table-wise) for the right hand side and comments.
 :
 : @param $html the HTML document.
 : @return the list of definitions (production or terminal introducer). Each
 : definition comes as a definition element, whose string value equals the
 : textual representation of the definition. Left hand side and right hand
 : side of a definition are enclosed in extra markup.
 :)
declare function e:extract-definitions($html as node()) as element(definition)*
{
  let $definition-cells :=
  (
    $html//*:td[e:normalize-space(.) = "::=" or @class = "prod-mid"],
    $html//*[matches(local-name(), "^h[0-9]+$") and (contains(., "Terminal Symbols") or contains(., "Productions for terminals"))]
  )/.
  return
    if (exists($definition-cells)) then (: w3c format :)
      for $d at $i in $definition-cells
      return
        if ($d/self::*:td) then
          let $first-row := $d/ancestor::*:tr[1]
          let $next-definition-row := $definition-cells[$i + 1]/ancestor::*:tr[1]
          let $continuation-rows :=
            $first-row/following-sibling::*[empty($next-definition-row) or
                                            . << $next-definition-row]
          let $definition-cell-index := count($first-row/*:td[. << $d]) + 1
          let $lhs := e:normalize-space($first-row/*:td[$definition-cell-index - 1])
          let $indent := "        "
          return
            element definition
            {
              element lhs {$lhs},
              concat
              (
                let $spaces := string-length($indent) - string-length($lhs)
                return
                  if ($spaces < 0) then
                    concat("&#xA;", $indent)
                  else
                    string-join((for $i in 1 to $spaces return " "), ""),
                " ::= "
              ),
              element rhs
              {
                string-join
                (
                  for $row in ($first-row, $continuation-rows)
                  return e:text-lines($row/*:td[$definition-cell-index + 1]),
                  concat("&#xA;   ", $indent)
                )
              },
              for $row in ($first-row, $continuation-rows)
              let $comment := e:comment($row/*:td[position() > $definition-cell-index + 1])
              where $comment
              return element comment{concat("&#xA;  ", $indent, $comment)}
            }
        else
          element definition {element lhs {"<?TOKENS?>"}}
    else (: rr format :)
      for $rule in
        (
          $html//xhtml:pre,
          $html//*:div[@class = "ebnf"]!replace(., "(&#xA;)[ &#x9;]+", "$1")
        )[contains(., "::=")]
      let $rule := replace($rule, "&#xA0;", " ")
      return
        element definition
        {
          element lhs {substring-before($rule, "::=")!replace(., "^\s+", "")},
          "::=",
          element rhs {substring-after($rule, "::=")!replace(., "\s+$", "")}
        }
};

(:~
 : Calculate day-of-week for given date.
 :
 : @param $date the date.
 : @return the number of days since Sunday (0..6).
 :)
declare function e:days-since-sunday($date as xs:date) as xs:integer
{
  let $y := year-from-date($date)
  let $m := month-from-date($date)
  let $d := day-from-date($date)
  let $z := $y + ($m - 14) idiv 12
  let $w := ((($m + 10 - ($m + 10) idiv 13 * 12) * 13 - 1) idiv 5 + ($z - $z idiv 100 * 100) * 5 idiv 4 + $z idiv 400 - $z idiv 100 * 2 + $d + 77) mod 7
  return $w
};

(:~
 : Get a formatted timestamp for current time. Use this
 : format: "Fri Feb 11, 2011, 22:43 (UTC+01)".
 :
 : @param $timezoneOffset the time zone offset in minutes.
 : @return the formatted timestamp.
 :)
declare function e:timestamp($timezoneOffset as xs:integer?) as xs:string
{
  let $dateTime :=
    if (empty($timezoneOffset) or abs($timezoneOffset) > 14 * 60) then
      current-dateTime()
    else
      let $timezone := xs:dayTimeDuration(concat("-"[$timezoneOffset > 0], "PT", string(abs($timezoneOffset)), "M"))
      return adjust-dateTime-to-timezone(current-dateTime(), $timezone)
  let $time := xs:time($dateTime)
  let $date := xs:date($dateTime)
  return
    concat
    (
      ("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
      [e:days-since-sunday($date) + 1],
      " ",
      ("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
      [month-from-date($date)],
      " ",
      day-from-dateTime($dateTime),
      ", ",
      year-from-dateTime($dateTime),
      ", ",
      substring(string($time), 1, 5),
      " (UTC",
      let $tz-string :=
        let $tz := timezone-from-time($time)
        return
          if ($tz < xs:dayTimeDuration("PT0H")) then
            concat("-", substring(string(xs:time("00:00:00") - $tz), 1, 5))
          else
            concat("+", substring(string(xs:time("00:00:00") + $tz), 1, 5))
      return
        if (ends-with($tz-string, "00:00")) then
          ""
        else if (ends-with($tz-string, ":00")) then
          substring-before($tz-string, ":00")
        else
          $tz-string,
      ")"
    )
};

(:~
 : Get an xhtml table, in W3C grammar style, containing the self-describing
 : grammar for the W3C grammar notation.
 :
 : @return the self-describing grammar as an xhtml table
 :)
declare function e:notation() as element()
{
  <table border="0" xmlns="http://www.w3.org/1999/xhtml">
    <tr><td><pre><a name="_Grammar">Grammar</a></pre></td><td><pre> ::= </pre></td><td><pre><a href="#_Production">Production</a>*</pre></td><td>&#xA;</td></tr>
    <tr><td><pre><a name="_Production">Production</a></pre></td><td><pre> ::= </pre></td><td><pre><a href="#_NCName">NCName</a> '::=' ( <a href="#_Choice">Choice</a> | <a href="#_Link">Link</a> )</pre></td><td>&#xA;</td></tr>
    <tr><td><pre><a name="_NCName">NCName</a></pre></td><td><pre> ::= </pre></td><td><pre>[<a target="_blank" href="http://www.w3.org/TR/xml-names/#NT-NCName">http://www.w3.org/TR/xml-names/#NT-NCName</a>]</pre></td><td>&#xA;</td></tr>
    <tr><td><pre><a name="_Choice">Choice</a></pre></td><td><pre> ::= </pre></td><td><pre><a href="#_CompositeExpression">CompositeExpression</a> ( '|' <a href="#_CompositeExpression">CompositeExpression</a> )*</pre></td><td>&#xA;</td></tr>
    <tr><td><pre><a name="_CompositeExpression">CompositeExpression</a></pre></td><td><pre> ::= </pre></td><td><pre>( <a href="#_Item">Item</a> ( ( '-' | '**' | '++' ) <a href="#_Item">Item</a> | <a href="#_Item">Item</a>* ) )?</pre></td><td>&#xA;</td></tr>
    <tr><td><pre><a name="_Item">Item</a></pre></td><td><pre> ::= </pre></td><td><pre><a href="#_Primary">Primary</a> ( '?' | '*' | '+' )?</pre></td><td>&#xA;</td></tr>
    <tr><td><pre><a name="_Primary">Primary</a></pre></td><td><pre> ::= </pre></td><td><pre><a href="#_NCName">NCName</a> | <a href="#_StringLiteral">StringLiteral</a> | <a href="#_CharCode">CharCode</a> | <a href="#_CharClass">CharClass</a> | '(' <a href="#_Choice">Choice</a> ')'</pre></td><td>&#xA;</td></tr>
    <tr><td><pre><a name="_StringLiteral">StringLiteral</a></pre></td><td><pre> ::= </pre></td><td><pre>'"' [^"]* '"' | "'" [^']* "'"</pre></td><td> /* ws: explicit */&#xA;</td></tr>
    <tr><td><pre><a name="_CharCode">CharCode</a></pre></td><td><pre> ::= </pre></td><td><pre>'#x' [0-9a-fA-F]+</pre></td><td> /* ws: explicit */&#xA;</td></tr>
    <tr><td><pre><a name="_CharClass">CharClass</a></pre></td><td><pre> ::= </pre></td><td><pre>'[' '^'? ( <a href="#_Char">Char</a> | <a href="#_CharCode">CharCode</a> | <a href="#_CharRange">CharRange</a> | <a href="#_CharCodeRange">CharCodeRange</a> )+ ']'</pre></td><td> /* ws: explicit */&#xA;</td></tr>
    <tr><td><pre><a name="_Char">Char</a></pre></td><td><pre> ::= </pre></td><td><pre>[<a target="_blank" href="http://www.w3.org/TR/xml#NT-Char">http://www.w3.org/TR/xml#NT-Char</a>]</pre></td><td>&#xA;</td></tr>
    <tr><td><pre><a name="_CharRange">CharRange</a></pre></td><td><pre> ::= </pre></td><td><pre><a href="#_Char">Char</a> '-' ( <a href="#_Char">Char</a> - ']' )</pre></td><td> /* ws: explicit */&#xA;</td></tr>
    <tr><td><pre><a name="_CharCodeRange">CharCodeRange</a></pre></td><td><pre> ::= </pre></td><td><pre><a href="#_CharCode">CharCode</a> '-' <a href="#_CharCode">CharCode</a></pre></td><td> /* ws: explicit */&#xA;</td></tr>
    <tr><td><pre><a name="_Link">Link</a></pre></td><td><pre> ::= </pre></td><td><pre>'[' <a href="#_URL">URL</a> ']'</pre></td><td>&#xA;</td></tr>
    <tr><td><pre><a name="_URL">URL</a></pre></td><td><pre> ::= </pre></td><td><pre>[^#x5D:/?#]+ '://' [^#x5D#]+ ('#' <a href="#_NCName">NCName</a>)?</pre></td><td> /* ws: explicit */&#xA;</td></tr>
    <tr><td><pre><a name="_Whitespace">Whitespace</a></pre></td><td><pre> ::= </pre></td><td><pre><a href="#_S">S</a> | <a href="#_Comment">Comment</a></pre></td><td>&#xA;</td></tr>
    <tr><td><pre><a name="_S">S</a></pre></td><td><pre> ::= </pre></td><td><pre>#x9 | #xA | #xD | #x20</pre></td><td>&#xA;</td></tr>
    <tr><td><pre><a name="_Comment">Comment</a></pre></td><td><pre> ::= </pre></td><td><pre>'/*' ( [^*] | '*'+ [^*/] )* '*'* '*/'</pre></td><td> /* ws: explicit */&#xA;</td></tr>
  </table>
};

(:~
 : Extract an EBNF grammar from an HTML document. First extract definitions,
 : then discard multiple identical occurrences of the same production. Finally,
 : order definitions by last occurrence in the original document. This fits
 : well with both the XML recommendation and the XQuery recommendation. The
 : latter has an appendix repeating all productions, the former shows them
 : only inline with the actual specification. It does not fit well with the
 : XQuery and XPath Full Text spec, which shows two different grammars with
 : some overlap of nonterminal names, but different productions.
 :
 : @param $uri the URI of the document containing the grammar.
 : @param $html the document containing the grammar.
 : @param $timezoneOffset the time zone offset in minutes.
 : @return the extracted grammar.
 :)
declare function e:extract($uri as xs:string, $html as node(), $timezoneOffset as xs:integer?) as element(grammar)
{
  element grammar
  {
    let $unordered-definitions := e:extract-definitions($html)
    let $names := for $u in $unordered-definitions return data($u/lhs)
    let $distinct-names := distinct-values($names)
    let $ordered-definitions :=
      for $name in $distinct-names
      order by index-of($names, $name)[last()]
      return
        if (contains($name, "<?")) then
          element TOKENS {concat("&#xA;", $name, "&#xA;")}
        else
          let $definitions := $unordered-definitions[lhs = $name]
          let $normalized := $definitions/rhs/translate(., " &#9;&#xA;&#xD;", "")
          for $p at $i in $definitions
          where not($normalized[position() < $i] = $normalized[$i])
          return
          (
            element production
            {
              $p/node()[. << $p/rhs],
              $p/rhs,
              for $c in
                distinct-values
                (
                  $definitions[position() = index-of($normalized, $normalized[$i])]/comment
                )
              return element comment {$c}
            }
          )
    return
    (
      let $title := ($html//*:head/*:title)[1]
      let $version := (($html//*:dt[. = "This version:"])[1]/following::*:a/@href)[1]
      return
        string-join
        (
          (
            "/",
            ("* ", normalize-space($title), "&#xA; ")[$title],
            ("* version ", data($version), "&#xA; ")[$version],
            "* extracted from ", $uri, " on ", e:timestamp($timezoneOffset), "&#xA; ",
            "*/&#xA;&#xA;"
          ),
          ""
        ),
      for $p at $i in $ordered-definitions
      return ("&#xA;"[$i != 1], $p)
    )
  }
};
