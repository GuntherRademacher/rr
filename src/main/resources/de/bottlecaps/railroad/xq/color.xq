(:~
 : The color code conversion module.
 :)
module namespace c = "de/bottlecaps/railroad/xq/color.xq";

(:~
 : The pattern to identify color codes.
 :)
declare variable $c:color-pattern := "^#[0-9A-Fa-f]{6}[^0-9A-Fa-f]?";

(:~
 : The default saturation.
 :)
declare variable $c:default-saturation := 1.0;

(:~
 : The default lightness.
 :)
declare variable $c:default-lightness := 0.65;

(:~
 : CSS3 colors as per http://www.w3.org/TR/css3-color/#svg-color
 :)
declare variable $c:css3-colors as element()+ :=
(
  <color rgb="#000000"><name>black</name></color>,
  <color rgb="#000080"><name>navy</name></color>,
  <color rgb="#00008B"><name>darkblue</name></color>,
  <color rgb="#0000CD"><name>mediumblue</name></color>,
  <color rgb="#0000FF"><name>blue</name></color>,
  <color rgb="#006400"><name>darkgreen</name></color>,
  <color rgb="#008000"><name>green</name></color>,
  <color rgb="#008080"><name>teal</name></color>,
  <color rgb="#008B8B"><name>darkcyan</name></color>,
  <color rgb="#00BFFF"><name>deepskyblue</name></color>,
  <color rgb="#00CED1"><name>darkturquoise</name></color>,
  <color rgb="#00FA9A"><name>mediumspringgreen</name></color>,
  <color rgb="#00FF00"><name>lime</name></color>,
  <color rgb="#00FF7F"><name>springgreen</name></color>,
  <color rgb="#00FFFF"><name>aqua</name><name>cyan</name></color>,
  <color rgb="#191970"><name>midnightblue</name></color>,
  <color rgb="#1E90FF"><name>dodgerblue</name></color>,
  <color rgb="#20B2AA"><name>lightseagreen</name></color>,
  <color rgb="#228B22"><name>forestgreen</name></color>,
  <color rgb="#2E8B57"><name>seagreen</name></color>,
  <color rgb="#2F4F4F"><name>darkslategray</name><name>darkslategrey</name></color>,
  <color rgb="#32CD32"><name>limegreen</name></color>,
  <color rgb="#3CB371"><name>mediumseagreen</name></color>,
  <color rgb="#40E0D0"><name>turquoise</name></color>,
  <color rgb="#4169E1"><name>royalblue</name></color>,
  <color rgb="#4682B4"><name>steelblue</name></color>,
  <color rgb="#483D8B"><name>darkslateblue</name></color>,
  <color rgb="#48D1CC"><name>mediumturquoise</name></color>,
  <color rgb="#4B0082"><name>indigo</name></color>,
  <color rgb="#556B2F"><name>darkolivegreen</name></color>,
  <color rgb="#5F9EA0"><name>cadetblue</name></color>,
  <color rgb="#6495ED"><name>cornflowerblue</name></color>,
  <color rgb="#66CDAA"><name>mediumaquamarine</name></color>,
  <color rgb="#696969"><name>dimgray</name><name>dimgrey</name></color>,
  <color rgb="#6A5ACD"><name>slateblue</name></color>,
  <color rgb="#6B8E23"><name>olivedrab</name></color>,
  <color rgb="#708090"><name>slategray</name><name>slategrey</name></color>,
  <color rgb="#778899"><name>lightslategray</name><name>lightslategrey</name></color>,
  <color rgb="#7B68EE"><name>mediumslateblue</name></color>,
  <color rgb="#7CFC00"><name>lawngreen</name></color>,
  <color rgb="#7FFF00"><name>chartreuse</name></color>,
  <color rgb="#7FFFD4"><name>aquamarine</name></color>,
  <color rgb="#800000"><name>maroon</name></color>,
  <color rgb="#800080"><name>purple</name></color>,
  <color rgb="#808000"><name>olive</name></color>,
  <color rgb="#808080"><name>gray</name><name>grey</name></color>,
  <color rgb="#87CEEB"><name>skyblue</name></color>,
  <color rgb="#87CEFA"><name>lightskyblue</name></color>,
  <color rgb="#8A2BE2"><name>blueviolet</name></color>,
  <color rgb="#8B0000"><name>darkred</name></color>,
  <color rgb="#8B008B"><name>darkmagenta</name></color>,
  <color rgb="#8B4513"><name>saddlebrown</name></color>,
  <color rgb="#8FBC8F"><name>darkseagreen</name></color>,
  <color rgb="#90EE90"><name>lightgreen</name></color>,
  <color rgb="#9370DB"><name>mediumpurple</name></color>,
  <color rgb="#9400D3"><name>darkviolet</name></color>,
  <color rgb="#98FB98"><name>palegreen</name></color>,
  <color rgb="#9932CC"><name>darkorchid</name></color>,
  <color rgb="#9ACD32"><name>yellowgreen</name></color>,
  <color rgb="#A0522D"><name>sienna</name></color>,
  <color rgb="#A52A2A"><name>brown</name></color>,
  <color rgb="#A9A9A9"><name>darkgray</name><name>darkgrey</name></color>,
  <color rgb="#ADD8E6"><name>lightblue</name></color>,
  <color rgb="#ADFF2F"><name>greenyellow</name></color>,
  <color rgb="#AFEEEE"><name>paleturquoise</name></color>,
  <color rgb="#B0C4DE"><name>lightsteelblue</name></color>,
  <color rgb="#B0E0E6"><name>powderblue</name></color>,
  <color rgb="#B22222"><name>firebrick</name></color>,
  <color rgb="#B8860B"><name>darkgoldenrod</name></color>,
  <color rgb="#BA55D3"><name>mediumorchid</name></color>,
  <color rgb="#BC8F8F"><name>rosybrown</name></color>,
  <color rgb="#BDB76B"><name>darkkhaki</name></color>,
  <color rgb="#C0C0C0"><name>silver</name></color>,
  <color rgb="#C71585"><name>mediumvioletred</name></color>,
  <color rgb="#CD5C5C"><name>indianred</name></color>,
  <color rgb="#CD853F"><name>peru</name></color>,
  <color rgb="#D2691E"><name>chocolate</name></color>,
  <color rgb="#D2B48C"><name>tan</name></color>,
  <color rgb="#D3D3D3"><name>lightgray</name><name>lightgrey</name></color>,
  <color rgb="#D8BFD8"><name>thistle</name></color>,
  <color rgb="#DA70D6"><name>orchid</name></color>,
  <color rgb="#DAA520"><name>goldenrod</name></color>,
  <color rgb="#DB7093"><name>palevioletred</name></color>,
  <color rgb="#DC143C"><name>crimson</name></color>,
  <color rgb="#DCDCDC"><name>gainsboro</name></color>,
  <color rgb="#DDA0DD"><name>plum</name></color>,
  <color rgb="#DEB887"><name>burlywood</name></color>,
  <color rgb="#E0FFFF"><name>lightcyan</name></color>,
  <color rgb="#E6E6FA"><name>lavender</name></color>,
  <color rgb="#E9967A"><name>darksalmon</name></color>,
  <color rgb="#EE82EE"><name>violet</name></color>,
  <color rgb="#EEE8AA"><name>palegoldenrod</name></color>,
  <color rgb="#F08080"><name>lightcoral</name></color>,
  <color rgb="#F0E68C"><name>khaki</name></color>,
  <color rgb="#F0F8FF"><name>aliceblue</name></color>,
  <color rgb="#F0FFF0"><name>honeydew</name></color>,
  <color rgb="#F0FFFF"><name>azure</name></color>,
  <color rgb="#F4A460"><name>sandybrown</name></color>,
  <color rgb="#F5DEB3"><name>wheat</name></color>,
  <color rgb="#F5F5DC"><name>beige</name></color>,
  <color rgb="#F5F5F5"><name>whitesmoke</name></color>,
  <color rgb="#F5FFFA"><name>mintcream</name></color>,
  <color rgb="#F8F8FF"><name>ghostwhite</name></color>,
  <color rgb="#FA8072"><name>salmon</name></color>,
  <color rgb="#FAEBD7"><name>antiquewhite</name></color>,
  <color rgb="#FAF0E6"><name>linen</name></color>,
  <color rgb="#FAFAD2"><name>lightgoldenrodyellow</name></color>,
  <color rgb="#FDF5E6"><name>oldlace</name></color>,
  <color rgb="#FF0000"><name>red</name></color>,
  <color rgb="#FF00FF"><name>fuchsia</name><name>magenta</name></color>,
  <color rgb="#FF1493"><name>deeppink</name></color>,
  <color rgb="#FF4500"><name>orangered</name></color>,
  <color rgb="#FF6347"><name>tomato</name></color>,
  <color rgb="#FF69B4"><name>hotpink</name></color>,
  <color rgb="#FF7F50"><name>coral</name></color>,
  <color rgb="#FF8C00"><name>darkorange</name></color>,
  <color rgb="#FFA07A"><name>lightsalmon</name></color>,
  <color rgb="#FFA500"><name>orange</name></color>,
  <color rgb="#FFB6C1"><name>lightpink</name></color>,
  <color rgb="#FFC0CB"><name>pink</name></color>,
  <color rgb="#FFD700"><name>gold</name></color>,
  <color rgb="#FFDAB9"><name>peachpuff</name></color>,
  <color rgb="#FFDEAD"><name>navajowhite</name></color>,
  <color rgb="#FFE4B5"><name>moccasin</name></color>,
  <color rgb="#FFE4C4"><name>bisque</name></color>,
  <color rgb="#FFE4E1"><name>mistyrose</name></color>,
  <color rgb="#FFEBCD"><name>blanchedalmond</name></color>,
  <color rgb="#FFEFD5"><name>papayawhip</name></color>,
  <color rgb="#FFF0F5"><name>lavenderblush</name></color>,
  <color rgb="#FFF5EE"><name>seashell</name></color>,
  <color rgb="#FFF8DC"><name>cornsilk</name></color>,
  <color rgb="#FFFACD"><name>lemonchiffon</name></color>,
  <color rgb="#FFFAF0"><name>floralwhite</name></color>,
  <color rgb="#FFFAFA"><name>snow</name></color>,
  <color rgb="#FFFF00"><name>yellow</name></color>,
  <color rgb="#FFFFE0"><name>lightyellow</name></color>,
  <color rgb="#FFFFF0"><name>ivory</name></color>,
  <color rgb="#FFFFFF"><name>white</name></color>
);

(:~
 : The color debug flag.
 :)
declare variable $c:debug as xs:boolean := false();

(:~
 : Convert one hexadecimal digit from numeric to string.
 :
 : @param $d the hexadecimal digit.
 : @return the corresponding string value.
 :)
declare function c:nibble($d) as xs:string
{
  substring("0123456789ABCDEF", $d + 1, 1)
};

(:~
 : Convert code of a single color (0 to 255) from numeric to
 : hexadecimal string notation.
 :
 : @param $c the numeric color code.
 : @return the color code as a two digit hexadecimal string.
 :)
declare function c:single-color-code($c) as xs:string
{
  let $c := max((0, min((255, xs:integer(255 * $c + 0.5)))))
  return concat(c:nibble(floor($c div 16)), c:nibble($c mod 16))
};

(:~
 : Convert numeric RGB values to a hexadecimal color code with a
 : '#' prefix for use in CSS or HTML.
 :
 : @param $r the red value.
 : @param $g the green value.
 : @param $b the blue value.
 : @return the RGB value for CSS or HTML use (7 chars).
 :)
declare function c:rgb($r as xs:decimal, $g as xs:decimal, $b as xs:decimal) as xs:string
{
  concat("#", c:single-color-code($r), c:single-color-code($g), c:single-color-code($b))
};

(:~
 : Convert numeric RGB values to a hexadecimal color code with a
 : '#' prefix for use in CSS or HTML.
 :
 : @param $rgb the red, green, and blue values.
 : @return the RGB value for CSS or HTML use (7 chars).
 :)
declare function c:rgb($rgb as xs:decimal+) as xs:string
{
  c:rgb($rgb[1], $rgb[2], $rgb[3])
};

(:~
 : Convert hue to RGB. Helper function for hsl-to-rgb. Reformulated in XQuery
 : from the original code at
 : <a target="_blank"
 :    xmlns="http://www.w3.org/1999/xhtml"
 :    href="http://www.w3.org/TR/css3-color/#hsl-color">http://www.w3.org/TR/css3-color/#hsl-color</a>:
 :
 : <pre xmlns="http://www.w3.org/1999/xhtml">
 :   HOW TO RETURN hue.to.rgb(m1, m2, h):
 :      IF h&lt;0: PUT h+1 IN h
 :      IF h&gt;1: PUT h-1 IN h
 :      IF h*6&lt;1: RETURN m1+(m2-m1)*h*6
 :      IF h*2&lt;1: RETURN m2
 :      IF h*3&lt;2: RETURN m1+(m2-m1)*(2/3-h)*6
 :      RETURN m1
 : </pre>
 :
 : @param $m1 the m1 value.
 : @param $m2 the m2 value.
 : @param $h the h value.
 : @return a single color value.
 :)
declare function c:hue-to-rgb($m1, $m2, $h)
{
  let $h := if ($h < 0) then $h + 1 else if ($h > 1) then $h - 1 else $h
  return
    if ($h * 6 < 1) then
      $m1 + ($m2 - $m1) * $h * 6
    else if ($h * 2 < 1) then
      $m2
    else if ($h * 3 < 2) then
      $m1 + ($m2 - $m1) * (2 div 3 - $h) * 6
    else
      $m1
};

(:~
 : Convert color from values in the HSL color model to an RGB code for
 : use in CSS or HTML. Reformulated in XQuery
 : from the original code at
 : <a target="_blank"
 :    xmlns="http://www.w3.org/1999/xhtml"
 :    href="http://www.w3.org/TR/css3-color/#hsl-color">http://www.w3.org/TR/css3-color/#hsl-color</a>:
 :
 : <pre xmlns="http://www.w3.org/1999/xhtml">
 :   HOW TO RETURN hsl.to.rgb(h, s, l):
 :      SELECT:
 :         l&lt;=0.5: PUT l*(s+1) IN m2
 :         ELSE: PUT l+s-l*s IN m2
 :      PUT l*2-m2 IN m1
 :      PUT hue.to.rgb(m1, m2, h+1/3) IN r
 :      PUT hue.to.rgb(m1, m2, h    ) IN g
 :      PUT hue.to.rgb(m1, m2, h-1/3) IN b
 :      RETURN (r, g, b)
 : </pre>
 :
 : @param $color the color code.
 : @param $s the saturation value.
 : @param $l the lightness value.
 : @return the RGB value for CSS or HTML use (7 chars).
 :)
declare function c:hsl-to-rgb($color, $s, $ll)
{
  let $ll := if ($color ge 0) then $ll else 1 - 0.8 * $ll
  let $color := if ($color ge 0) then $color else -1 - $color
  return
    if ($s = 0 or $color >= 360) then
      ($ll, $ll, $ll)
    else
      let $h := $color div 360

      let $m2 :=
        if ($ll <= 0.5) then
          $ll * ($s + 1)
        else
          $ll + $s - $ll * $s
      let $m1 := $ll * 2 - $m2
      let $r := c:hue-to-rgb($m1, $m2, $h + 1 div 3)
      let $g := c:hue-to-rgb($m1, $m2, $h)
      let $b := c:hue-to-rgb($m1, $m2, $h - 1 div 3)
      return ($r, $g, $b)
};

declare function c:unhex($codepoint as xs:integer*, $value as xs:integer) as xs:integer
{
  if (empty($codepoint)) then
    $value
  else
    c:unhex
    (
      subsequence($codepoint, 2),
      $codepoint[1] - (0, 0, 48, 55, 0, 87)[$codepoint[1] idiv 16] + $value * 16
    )
};

declare function c:unhex($hex as xs:string) as xs:integer
{
  c:unhex(string-to-codepoints($hex), 0)
};

declare function c:r-g-b($rgb)
{
  let $code := c:color-code($rgb)
  where exists($code)
  return
    for $i in 1 to 3
    return c:unhex(substring($code, 2 * $i, 2))
};

declare function c:rgb-to-hsl($color)
{
  let $r-g-b := c:r-g-b($color)
  return c:rgb-to-hsl($r-g-b[1] div 255,
                      $r-g-b[2] div 255,
                      $r-g-b[3] div 255)
};

declare function c:rgb-to-hsl($r, $g, $b)
{
  let $min := min(($r, $g, $b))
  let $max := max(($r, $g, $b))
  return
    if ($max eq 0) then
      (0, 0, 0)
    else
      let $delta := $max - $min
      let $h := if ($delta eq 0) then 0
           else if ($r eq $max)  then ($g - $b) div $delta
           else if ($g eq $max)  then 2 + ($b - $r) div $delta
           else                       4 + ($r - $g) div $delta
      let $s := if ((1 - abs($max + $min - 1)) = 0) then 0 else $delta div (1 - abs($max + $min - 1))
      let $l := ($max + $min) div 2
      return (xs:integer((($h * 60) + 360 + 0.5) mod 360), $s, $l)
};

declare function c:relative-color($color, $s, $l)
{
  let $hsl := c:rgb-to-hsl($color)
  let $h := $hsl[1]
  let $ss := $hsl[2] * $s
  return
    if ($l < 0.061 and $l > 0.039) then
      let $background-lightness := if ($l > 0.055) then 0.89
                              else if ($l > 0.045) then 0.81
                              else                      $c:default-lightness
      let $background-brightness := c:brightness(c:hsl-to-rgb($h, $ss, c:convert-lightness($background-lightness, $hsl[3], 0, 1)))
      let $dark := c:hsl-to-rgb($h, $ss, $l)
      let $light := c:hsl-to-rgb($h, $ss, 0.94)
      return
        if (abs(c:brightness($dark) - $background-brightness) > abs(c:brightness($light) - $background-brightness)) then
          c:rgb($dark)
        else
          c:rgb($light)
    else
      let $ll := if ($l < 0.5)             then c:convert-lightness($l, $hsl[3], $l - 0.01, $l + 0.05)
            else if ($l > 0.9 or $s < 0.9) then c:convert-lightness($l, $hsl[3], $l - 0.07, $l + 0.03)
            else                                c:convert-lightness($l, $hsl[3], 0, 1)
      return c:rgb(c:hsl-to-rgb($h, $ss, $ll))
  (: " /* r-c(", substring($color, 2), ", ", string($s), ",", string($l), ") hsl(", string($h), ",", string($ss), ",", string($ll), ") */" :)
};

declare function c:brightness($rgb as xs:decimal+) as xs:decimal
{
  ($rgb[1] * 299 + $rgb[2] * 587 + $rgb[3] * 114) div 1000
};

declare function c:convert-lightness($oldL as xs:decimal, $newL as xs:decimal, $min as xs:decimal, $max as xs:decimal) as xs:decimal
{
  let $factor := $max - $min
  let $oldL := ($oldL - $min) div $factor
  let $result := if ($newL <= $c:default-lightness)
            then $oldL div $c:default-lightness * $newL
            else $newL + ($oldL - $c:default-lightness) div (1 - $c:default-lightness) * (1 - $newL)
  return $min + $result * $factor
};

declare function c:color-code($color as xs:string) as xs:string?
{
  let $color := normalize-space($color)
  return
    if (matches($color, "^#[0-9a-fA-F]{6}$")) then
      upper-case($color)
    else if (matches($color, "^#[0-9a-fA-F]{3}$")) then
      replace(upper-case($color), "([0-9A-F])", "$1$1")
    else if (matches($color, "^rgb\s*\(\s*\d+\s*,\s*\d+\s*,\s*\d+\s*\)$")) then
      let $r-g-b := for $t in tokenize($color, "[rgb(),\s]")[.] return xs:integer($t)
      return c:rgb($r-g-b[1] div 255, $r-g-b[2] div 255, $r-g-b[3] div 255)
    else if (matches($color, "^hsl\s*\(\s*\d+\s*,\s*\d+\s*%\s*,\s*\d+\s*%\s\)$")) then
      let $h-s-l := for $t in tokenize($color, "[hsl()%,\s]")[.] return xs:integer($t)
      return c:rgb(c:hsl-to-rgb($h-s-l[1], $h-s-l[2] div 100, $h-s-l[3] div 100))
    else
      data($c:css3-colors[name = lower-case($color)]/@rgb)
};
