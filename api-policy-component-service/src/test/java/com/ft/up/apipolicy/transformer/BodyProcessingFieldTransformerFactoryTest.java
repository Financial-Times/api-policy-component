package com.ft.up.apipolicy.transformer;

import static com.ft.up.apipolicy.EquivalentIgnoringWindowsLineEndings.equivalentToUnixString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import com.ft.bodyprocessing.transformer.FieldTransformer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class BodyProcessingFieldTransformerFactoryTest {


    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private FieldTransformer bodyTransformer;
    private static final String TRANSACTION_ID = "tid_test";

    @Before
    public void setup() {
        bodyTransformer = new BodyProcessingFieldTransformerFactory().newInstance();
    }

    @Test
    public void shouldStripOutEmptyTags() {
        String original = "<body><p><strong>Text Formatting</strong>\n</p>\n<p>This is an example of bold text : <strong>This text is bold &lt;b&gt;</strong>\n</p>\n<p>This is an example of italic text : <em>This text is italic &lt;i&lt;</em>\n</p>\n<p><em>Emphasis</em>\n</p>\n<p><strong>Strong</strong>\n</p>\n<p><sup>Superscript</sup>\n</p>\n<p><sub>Subscript</sub>\n</p>\n<p>Underline\n</p>\n<p><strong>Links</strong>\n</p>\n<p>This is a link for an FT article that has been cut and paste from a browser: <a href=\"http://www.ft.com/cms/s/2/e78a8668-c997-11e1-aae2-002128161462.html\" title=\"www.ft.com\">http://www.ft.com/cms/s/2/e78a8668-c997-11e1-aae2-002128161462.html</a>.</p>\n<p>This link was added using Right-click, Insert Hyperlink: <ft-content type=\"http://www.ft.com/ontology/content/Article\" url=\"http://int.api.ft.com/content/9b9fed88-d986-11e2-bce1-002128161462\">A story about something financial</ft-content>\n</p>\n<p>This link was added using drag and drop of an article: <ft-content type=\"http://www.ft.com/ontology/content/Article\" url=\"http://int.api.ft.com/content/9b9fed88-d986-11e2-bce1-002128161462\">A story about something financial that was dragged and dropped</ft-content>\n</p>\n<p>This link was added by cutting and pasing a URL of a valid FT article: <ft-content title=\"www.ft.com\" type=\"http://www.ft.com/ontology/content/Article\" url=\"http://int.api.ft.com/content/f3b60ad0-acda-11e2-a7c4-002128161462\">http://www.ft.com/cms/s/2/f3b60ad0-acda-11e2-a7c4-002128161462.html</ft-content>\n</p>\n<p>This link was added by cutting and pasting a URL from the BBC: <a href=\"http://www.bbc.co.uk/news/world-africa-24577711\" title=\"www.bbc.co.uk\">http://www.bbc.co.uk/news/world-africa-24577711</a>\n</p>\n<p><strong>Lists</strong>\n</p>\n<p>The following is a bulleted list:</p>\n<ul><li>Item 1</li>\n<li>Item 2</li>\n<li>Item 3</li>\n</ul>\n<p><strong>Some Typical Body Text</strong>\n</p>\n<p>The UK government has struck a deal with the French utility EDF to build the country’s first new nuclear plant in a generation.</p>\n<p>The agreement was reached after the government guaranteed a price of £92.50 per megawatt hour for electricity produced at Hinkley Point C in Somerset. The “strike price”, fully indexed to consumer price inflation, is roughly double the current price of power.</p>\n<p>It forms the centrepiece of a long-awaited deal between the government and EDF and paves the way for the construction of the £16bn power station which, when completed in 2023, will provide 7 per cent of the UK’s electricity.</p>\n<p>Prime minister David Cameron welcomed the deal, saying it would create 25,000 jobs and that it marked “the next generation of nuclear power in Britain, which has an important part to play in contributing to our future energy needs and our longer term security of supply’’.</p>\n<p><strong>Methode Special Characters</strong>\n</p>\n<p><em>Bullets</em>\n</p>\n<p>• Text wtih an inserted Bullet Character</p>\n<p>■ Text with an inserted Black Square Character</p>\n<p><em>Spaces and hyphens</em>\n</p>\n<p>Text separated by a normal keyboard-hyphen</p>\n<p>Text separated by an Em dash: apple—banana</p>\n<p>Text separated by an En dash: apple–banana</p>\n<p>Two words separated by an Em space: apple banana</p>\n<p>Two words separated by an En space: apple banana</p>\n<p>Two words separated by a Thin space: apple banana</p>\n<p>Two words separated by a Hair space: apple banana</p>\n<p>Two words separated by a Figure space: apple banana</p>\n<p>Two words separated by a Punctuation space: apple banana</p>\n<p>Two words separated by a Non-breaking space: apple banana</p>\n<p><strong>Strike Out Text</strong>\n</p>\n<p>The text between this sentence and the one after (starting “Apple”) is struck out and hidden from <strong>all channels</strong>. Apple shares rocketed today on news of the forthcoming iWatch.</p>\n<p>The next entire paragraph is struck out and hidden from <strong>all channels</strong>:</p>\n<p>The next entire paragraph is struck out and <strong>hidden from the newspaper channel</strong>:</p>\n<p>The next entire paragraph is struck out and <strong>restricted to only the newspaper channel and should not be seen by web users:</strong>\n</p>\n<p><strong>Company Data</strong>\n</p>\n<p>This is an example of company share data displayed on hover. Google released their third quarter results today on the back of record smartphone sales.</p>\n<p><strong>Tables</strong>\n</p>\n<p><em>Table added using Insert &gt; Table</em>\n</p>\n<p><em>Table added using Insert &gt; Component</em>\n</p>\n\n<table class=\"data-table\"><caption>Table (Falcon Style)</caption>\n<thead><tr><th>Column A</th>\n<th>Column B</th>\n<th>Column C</th>\n<th>Column D</th>\n</tr>\n</thead>\n<tbody><tr><td>0</td>\n<td>1</td>\n<td>2</td>\n<td>3</td>\n</tr>\n<tr><td>4</td>\n<td>5</td>\n<td>6</td>\n<td>7</td>\n</tr>\n</tbody>\n</table>\n\n\n\n<p><strong>Currency Characters</strong>\n</p>\n<p>Sterling: £</p>\n<p>Dollar: $</p>\n<p>Yen: ¥</p>\n<p>Euro: €</p>\n<p><strong>Pull Quotes</strong>\n</p>\n<p><pull-quote><pull-quote-text>Think left and think right and think low and think high. Oh, the thinks you can think up if only you try!</pull-quote-text><pull-quote-source>Dr. Seuss</pull-quote-source></pull-quote>\n</p>\n<p>The UK government has struck a deal with the French utility EDF to build the country’s first new nuclear plant in a generation.</p>\n<p>The agreement was reached after the government guaranteed a price of £92.50 per megawatt hour for electricity produced at Hinkley Point C in Somerset. The “strike price”, fully indexed to consumer price inflation, is roughly double the current price of power.</p>\n<p>It forms the centrepiece of a long-awaited deal between the government and EDF and paves the way for the construction of the £16bn power station which, when completed in 2023, will provide 7 per cent of the UK’s electricity.</p>\n<p><strong>Typical Body Text With Promo Box</strong>\n</p>\n<p>Prime minister David Cameron welcomed the deal, saying it would create 25,000 jobs and that it marked “the next generation of nuclear power in Britain, which has an important part to play in contributing to our future energy needs and our longer term security of supply’’.</p>\n<p><strong>Images</strong>\n</p>\n<p>The first image is a GIF, the second a JPEG. Text should wrap around inline images, so the following is some typical body text.</p>\n<p>The UK government has struck a deal with the French utility EDF to build the country’s first new nuclear plant in a generation.</p>\n<p>The agreement was reached after the government guaranteed a price of £92.50 per megawatt hour for electricity produced at Hinkley Point C in Somerset. The “strike price”, fully indexed to consumer price inflation, is roughly double the current price of power.</p>\n<p>It forms the centrepiece of a long-awaited deal between the government and EDF and paves the way for the construction of the £16bn power station which, when completed in 2023, will provide 7 per cent of the UK’s electricity.</p>\n<p><strong>Embedded Slideshow</strong>\n</p>\n<p><a href=\"http://www.ft.com/cms/s/49336a18-051c-11e3-98a0-002128161462.html#slide0\"></a>\n</p>\n<p><strong>Flash</strong>\n</p>\n<p><strong>Special Characters List</strong>\n</p>\n<p>Space (   )</p>\n<p>Exclamation mark ( ! )</p>\n<p>Double quotes (or speech marks) ( “ )</p>\n<p>Number ( # )</p>\n<p>Dollar ( $ )</p>\n<p>Procenttecken ( % )</p>\n<p>Ampersand ( &amp; )</p>\n<p>Single quote ( ‘ )</p>\n<p>Open parenthesis (or open bracket) ( ( )</p>\n<p>Close parenthesis (or close bracket) ( ) )</p>\n<p>Asterisk ( * )</p>\n<p>Plus ( + )</p>\n<p>Comma ( , )</p>\n<p>Hyphen ( - )</p>\n<p>Period, dot or full stop ( . )</p>\n<p>Slash or divide ( / )</p>\n<p>Zero ( 0 )</p>\n<p>One ( 1 )</p>\n<p>Two ( 2 )</p>\n<p>Three ( 3 )</p>\n<p>Four ( 4 )</p>\n<p>Five ( 5 )</p>\n<p>Six ( 6 )</p>\n<p>Seven ( 7 )</p>\n<p>Eight ( 8 )</p>\n<p>Nine ( 9 )</p>\n<p>Colon ( : )</p>\n<p>Semicolon ( ; )</p>\n<p>Less than (or open angled bracket) ( &lt; )</p>\n<p>Equals ( = )</p>\n<p>Greater than (or close angled bracket) ( &gt; )</p>\n<p>Question mark ( ? )</p>\n<p>At symbol ( @ )</p>\n<p>Uppercase A ( A )</p>\n<p>Uppercase B ( B )</p>\n<p>Uppercase C ( C )</p>\n<p>Uppercase D ( D )</p>\n<p>Uppercase E ( E )</p>\n<p>Uppercase F ( F )</p>\n<p>Uppercase G ( G )</p>\n<p>Uppercase H ( H )</p>\n<p>Uppercase I ( I )</p>\n<p>Uppercase J ( J )</p>\n<p>Uppercase K ( K )</p>\n<p>Uppercase L ( L )</p>\n<p>Uppercase M ( M )</p>\n<p>Uppercase N ( N )</p>\n<p>Uppercase O ( O )</p>\n<p>Uppercase P ( P )</p>\n<p>Uppercase Q ( Q )</p>\n<p>Uppercase R ( R )</p>\n<p>Uppercase S ( S )</p>\n<p>Uppercase T ( T )</p>\n<p>Uppercase U ( U )</p>\n<p>Uppercase V ( V )</p>\n<p>Uppercase W ( W )</p>\n<p>Uppercase X ( X )</p>\n<p>Uppercase Y ( Y )</p>\n<p>Uppercase Z ( Z )</p>\n<p>Opening bracket ( [ )</p>\n<p>Backslash ( \\\\ )</p>\n<p>Closing bracket ( ] )</p>\n<p>Caret - circumflex ( ^ )</p>\n<p>Underscore ( _ )</p>\n<p>Grave accent ( ` )</p>\n<p>Lowercase a ( a )</p>\n<p>Lowercase b ( b )</p>\n<p>Lowercase c ( c )</p>\n<p>Lowercase d ( d )</p>\n<p>Lowercase e ( e )</p>\n<p>Lowercase f ( f )</p>\n<p>Lowercase g ( g )</p>\n<p>Lowercase h ( h )</p>\n<p>Lowercase i ( i )</p>\n<p>Lowercase j ( j )</p>\n<p>Lowercase k ( k )</p>\n<p>Lowercase l ( l )</p>\n<p>Lowercase m ( m )</p>\n<p>Lowercase n ( n )</p>\n<p>Lowercase o ( o )</p>\n<p>Lowercase p ( p )</p>\n<p>Lowercase q ( q )</p>\n<p>Lowercase r ( r )</p>\n<p>Lowercase s ( s )</p>\n<p>Lowercase t ( t )</p>\n<p>Lowercase u ( u )</p>\n<p>Lowercase v ( v )</p>\n<p>Lowercase w ( w )</p>\n<p>Lowercase x ( x )</p>\n<p>Lowercase y ( y )</p>\n<p>Lowercase z ( z )</p>\n<p>Opening brace ( { )</p>\n<p>Vertical bar ( | )</p>\n<p>Closing brace ( } )</p>\n<p>Equivalency sign - tilde ( ~ )</p>\n<p>Delete ( )</p>\n<p>Euro sign ( € )</p>\n<p>Single low-9 quotation mark ( ‚ )</p>\n<p>Latin small letter f with hook ( ƒ )</p>\n<p>Double low-9 quotation mark ( „ )</p>\n<p>Horizontal ellipsis ( … )</p>\n<p>Dagger ( † )</p>\n<p>Double dagger ( ‡ )</p>\n<p>Modifier letter circumflex accent ( ˆ )</p>\n<p>Per mille sign ( ‰ )</p>\n<p>Latin capital letter S with caron ( Š )</p>\n<p>Single left-pointing angle quotation ( ‹ )</p>\n<p>Latin capital ligature OE ( Œ )</p>\n<p>Latin captial letter Z with caron ( Ž )</p>\n<p>Left single quotation mark ( ‘ )</p>\n<p>Right single quotation mark ( ’ )</p>\n<p>Left double quotation mark ( “ )</p>\n<p>Right double quotation mark ( ” )</p>\n<p>Bullet ( • )</p>\n<p>En dash ( – )</p>\n<p>Em dash ( — )</p>\n<p>Small tilde ( ˜ )</p>\n<p>Trade mark sign ( ™ )</p>\n<p>Latin small letter S with caron ( š )</p>\n<p>Single right-pointing angle quotation mark ( › )</p>\n<p>Latin small ligature oe ( œ )</p>\n<p>Latin small letter z with caron ( ž )</p>\n<p>Latin capital letter Y with diaeresis ( Ÿ )</p>\n<p>Non-breaking space (   )</p>\n<p>Inverted exclamation mark ( ¡ )</p>\n<p>Cent sign ( ¢ )</p>\n<p>Pound sign ( £ )</p>\n<p>Currency sign ( ¤ )</p>\n<p>Yen sign ( ¥ )</p>\n<p>Pipe, Broken vertical bar ( ¦ )</p>\n<p>Section sign ( § )</p>\n<p>Spacing diaeresis - umlaut ( ¨ )</p>\n<p>Copyright sign ( © )</p>\n<p>Feminine ordinal indicator ( ª )</p>\n<p>Left double angle quotes ( « )</p>\n<p>Not sign ( ¬ )</p>\n<p>Soft hyphen ( )</p>\n<p>Registered trade mark sign ( ® )</p>\n<p>Spacing macron - overline ( ¯ )</p>\n<p>Degree sign ( ° )</p>\n<p>Plus-or-minus sign ( ± )</p>\n<p>Superscript two - squared ( ² )</p>\n<p>Superscript three - cubed ( ³ )</p>\n<p>Acute accent - spacing acute ( ´ )</p>\n<p>Micro sign ( µ )</p>\n<p>Pilcrow sign - paragraph sign ( ¶ )</p>\n<p>Middle dot - Georgian comma ( · )</p>\n<p>Spacing cedilla ( ¸ )</p>\n<p>Superscript one ( ¹ )</p>\n<p>Masculine ordinal indicator ( º )</p>\n<p>Right double angle quotes ( » )</p>\n<p>Fraction one quarter ( ¼ )</p>\n<p>Fraction one half ( ½ )</p>\n<p>Fraction three quarters ( ¾ )</p>\n<p>Inverted question mark ( ¿ )</p>\n<p>Latin capital letter A with grave ( À )</p>\n<p>Latin capital letter A with acute ( Á )</p>\n<p>Latin capital letter A with circumflex ( Â )</p>\n<p>Latin capital letter A with tilde ( Ã )</p>\n<p>Latin capital letter A with diaeresis ( Ä )</p>\n<p>Latin capital letter A with ring above ( Å )</p>\n<p>Latin capital letter AE ( Æ )</p>\n<p>Latin capital letter C with cedilla ( Ç )</p>\n<p>Latin capital letter E with grave ( È )</p>\n<p>Latin capital letter E with acute ( É )</p>\n<p>Latin capital letter E with circumflex ( Ê )</p>\n<p>Latin capital letter E with diaeresis ( Ë )</p>\n<p>Latin capital letter I with grave ( Ì )</p>\n<p>Latin capital letter I with acute ( Í )</p>\n<p>Latin capital letter I with circumflex ( Î )</p>\n<p>Latin capital letter I with diaeresis ( Ï )</p>\n<p>Latin capital letter ETH ( Ð )</p>\n<p>Latin capital letter N with tilde ( Ñ )</p>\n<p>Latin capital letter O with grave ( Ò )</p>\n<p>Latin capital letter O with acute ( Ó )</p>\n<p>Latin capital letter O with circumflex ( Ô )</p>\n<p>Latin capital letter O with tilde ( Õ )</p>\n<p>Latin capital letter O with diaeresis ( Ö )</p>\n<p>Multiplication sign ( × )</p>\n<p>Latin capital letter O with slash ( Ø )</p>\n<p>Latin capital letter U with grave ( Ù )</p>\n<p>Latin capital letter U with acute ( Ú )</p>\n<p>Latin capital letter U with circumflex ( Û )</p>\n<p>Latin capital letter U with diaeresis ( Ü )</p>\n<p>Latin capital letter Y with acute ( Ý )</p>\n<p>Latin capital letter THORN ( Þ )</p>\n<p>Latin small letter sharp s - ess-zed ( ß )</p>\n<p>Latin small letter a with grave ( à )</p>\n<p>Latin small letter a with acute ( á )</p>\n<p>Latin small letter a with circumflex ( â )</p>\n<p>Latin small letter a with tilde ( ã )</p>\n<p>Latin small letter a with diaeresis ( ä )</p>\n<p>Latin small letter a with ring above ( å )</p>\n<p>Latin small letter ae ( æ )</p>\n<p>Latin small letter c with cedilla ( ç )</p>\n<p>Latin small letter e with grave ( è )</p>\n<p>Latin small letter e with acute ( é )</p>\n<p>Latin small letter e with circumflex ( ê )</p>\n<p>Latin small letter e with diaeresis ( ë )</p>\n<p>Latin small letter i with grave ( ì )</p>\n<p>Latin small letter i with acute ( í )</p>\n<p>Latin small letter i with circumflex ( î )</p>\n<p>Latin small letter i with diaeresis ( ï )</p>\n<p>Latin small letter eth ( ð )</p>\n<p>Latin small letter n with tilde ( ñ )</p>\n<p>Latin small letter o with grave ( ò )</p>\n<p>Latin small letter o with acute ( ó )</p>\n<p>Latin small letter o with circumflex ( ô )</p>\n<p>Latin small letter o with tilde ( õ )</p>\n<p>Latin small letter o with diaeresis ( ö )</p>\n<p>Division sign ( ÷ )</p>\n<p>Latin small letter o with slash ( ø )</p>\n<p>Latin small letter u with grave ( ù )</p>\n<p>Latin small letter u with acute ( ú )</p>\n<p>Latin small letter u with circumflex ( û )</p>\n<p>Latin small letter u with diaeresis ( ü )</p>\n<p>Latin small letter y with acute ( ý )</p>\n<p>Latin small letter thorn ( þ )</p>\n<p>Latin small letter y with diaeresis ( ÿ )</p>\n<p>END OF LIST</p>\n<p><strong>-- Paragraphs -- </strong>\n</p>\n<p>This is a paragraph.</p>\n<p>This is another paragraph.</p>\n<p>This paragraph contains a nested <p>This is the nested paragraph.</p> paragraph.</p>\n<p>The next paragraph is empty.</p>\n<p>The previous paragraph is empty.</p>\n<p>Paragraph tag with title and onclick attributes</p>\n<p>Text separated by line break 1<br></br>Text separated by line break 2</p>\n<h1>Heading 1</h1>\n<h2>Heading 2</h2>\n<h3>Heading 3</h3>\n<h4>Heading 4</h4>\n<h5>Heading 5</h5>\n<h6>Heading 6</h6>\n<ol>Ordered List<li>Item 1 </li>\n<li>Item 2</li>\n<li>Item 3</li>\n</ol>\n<ul>Unordered List  <li>Item 1</li>\n<li>Item 2</li>\n<li>Item 3</li>\n</ul>\n<p><strong>Strong With Title Attribute</strong>\n</p>\n<p><strong>-- Self Closing hr Tag --</strong>\n</p>\n<p><strong>-- Content Within Unwanted Tags --</strong>\n</p>\n<p>Script Tag: \n\n</p>\n\n</body>";
        String expected = "<body><p><strong>Text Formatting</strong>\n</p>\n<p>This is an example of bold text : <strong>This text is bold &lt;b></strong>\n</p>\n<p>This is an example of italic text : <em>This text is italic &lt;i&lt;</em>\n</p>\n<p><em>Emphasis</em>\n</p>\n<p><strong>Strong</strong>\n</p>\n<p><sup>Superscript</sup>\n</p>\n<p><sub>Subscript</sub>\n</p>\n<p>Underline\n</p>\n<p><strong>Links</strong>\n</p>\n<p>This is a link for an FT article that has been cut and paste from a browser: <a href=\"http://www.ft.com/cms/s/2/e78a8668-c997-11e1-aae2-002128161462.html\" title=\"www.ft.com\">http://www.ft.com/cms/s/2/e78a8668-c997-11e1-aae2-002128161462.html</a>.</p>\n<p>This link was added using Right-click, Insert Hyperlink: <ft-content type=\"http://www.ft.com/ontology/content/Article\" url=\"http://int.api.ft.com/content/9b9fed88-d986-11e2-bce1-002128161462\">A story about something financial</ft-content>\n</p>\n<p>This link was added using drag and drop of an article: <ft-content type=\"http://www.ft.com/ontology/content/Article\" url=\"http://int.api.ft.com/content/9b9fed88-d986-11e2-bce1-002128161462\">A story about something financial that was dragged and dropped</ft-content>\n</p>\n<p>This link was added by cutting and pasing a URL of a valid FT article: <ft-content title=\"www.ft.com\" type=\"http://www.ft.com/ontology/content/Article\" url=\"http://int.api.ft.com/content/f3b60ad0-acda-11e2-a7c4-002128161462\">http://www.ft.com/cms/s/2/f3b60ad0-acda-11e2-a7c4-002128161462.html</ft-content>\n</p>\n<p>This link was added by cutting and pasting a URL from the BBC: <a href=\"http://www.bbc.co.uk/news/world-africa-24577711\" title=\"www.bbc.co.uk\">http://www.bbc.co.uk/news/world-africa-24577711</a>\n</p>\n<p><strong>Lists</strong>\n</p>\n<p>The following is a bulleted list:</p>\n<ul><li>Item 1</li>\n<li>Item 2</li>\n<li>Item 3</li>\n</ul>\n<p><strong>Some Typical Body Text</strong>\n</p>\n<p>The UK government has struck a deal with the French utility EDF to build the country’s first new nuclear plant in a generation.</p>\n<p>The agreement was reached after the government guaranteed a price of £92.50 per megawatt hour for electricity produced at Hinkley Point C in Somerset. The “strike price”, fully indexed to consumer price inflation, is roughly double the current price of power.</p>\n<p>It forms the centrepiece of a long-awaited deal between the government and EDF and paves the way for the construction of the £16bn power station which, when completed in 2023, will provide 7 per cent of the UK’s electricity.</p>\n<p>Prime minister David Cameron welcomed the deal, saying it would create 25,000 jobs and that it marked “the next generation of nuclear power in Britain, which has an important part to play in contributing to our future energy needs and our longer term security of supply’’.</p>\n<p><strong>Methode Special Characters</strong>\n</p>\n<p><em>Bullets</em>\n</p>\n<p>• Text wtih an inserted Bullet Character</p>\n<p>■ Text with an inserted Black Square Character</p>\n<p><em>Spaces and hyphens</em>\n</p>\n<p>Text separated by a normal keyboard-hyphen</p>\n<p>Text separated by an Em dash: apple—banana</p>\n<p>Text separated by an En dash: apple–banana</p>\n<p>Two words separated by an Em space: apple banana</p>\n<p>Two words separated by an En space: apple banana</p>\n<p>Two words separated by a Thin space: apple banana</p>\n<p>Two words separated by a Hair space: apple banana</p>\n<p>Two words separated by a Figure space: apple banana</p>\n<p>Two words separated by a Punctuation space: apple banana</p>\n<p>Two words separated by a Non-breaking space: apple banana</p>\n<p><strong>Strike Out Text</strong>\n</p>\n<p>The text between this sentence and the one after (starting “Apple”) is struck out and hidden from <strong>all channels</strong>. Apple shares rocketed today on news of the forthcoming iWatch.</p>\n<p>The next entire paragraph is struck out and hidden from <strong>all channels</strong>:</p>\n<p>The next entire paragraph is struck out and <strong>hidden from the newspaper channel</strong>:</p>\n<p>The next entire paragraph is struck out and <strong>restricted to only the newspaper channel and should not be seen by web users:</strong>\n</p>\n<p><strong>Company Data</strong>\n</p>\n<p>This is an example of company share data displayed on hover. Google released their third quarter results today on the back of record smartphone sales.</p>\n<p><strong>Tables</strong>\n</p>\n<p><em>Table added using Insert > Table</em>\n</p>\n<p><em>Table added using Insert > Component</em>\n</p>\n<p><strong>Currency Characters</strong>\n</p>\n<p>Sterling: £</p>\n<p>Dollar: $</p>\n<p>Yen: ¥</p>\n<p>Euro: €</p>\n<p><strong>Pull Quotes</strong>\n</p>\n<p>The UK government has struck a deal with the French utility EDF to build the country’s first new nuclear plant in a generation.</p>\n<p>The agreement was reached after the government guaranteed a price of £92.50 per megawatt hour for electricity produced at Hinkley Point C in Somerset. The “strike price”, fully indexed to consumer price inflation, is roughly double the current price of power.</p>\n<p>It forms the centrepiece of a long-awaited deal between the government and EDF and paves the way for the construction of the £16bn power station which, when completed in 2023, will provide 7 per cent of the UK’s electricity.</p>\n<p><strong>Typical Body Text With Promo Box</strong>\n</p>\n<p>Prime minister David Cameron welcomed the deal, saying it would create 25,000 jobs and that it marked “the next generation of nuclear power in Britain, which has an important part to play in contributing to our future energy needs and our longer term security of supply’’.</p>\n<p><strong>Images</strong>\n</p>\n<p>The first image is a GIF, the second a JPEG. Text should wrap around inline images, so the following is some typical body text.</p>\n<p>The UK government has struck a deal with the French utility EDF to build the country’s first new nuclear plant in a generation.</p>\n<p>The agreement was reached after the government guaranteed a price of £92.50 per megawatt hour for electricity produced at Hinkley Point C in Somerset. The “strike price”, fully indexed to consumer price inflation, is roughly double the current price of power.</p>\n<p>It forms the centrepiece of a long-awaited deal between the government and EDF and paves the way for the construction of the £16bn power station which, when completed in 2023, will provide 7 per cent of the UK’s electricity.</p>\n<p><strong>Embedded Slideshow</strong>\n</p>\n<p><strong>Flash</strong>\n</p>\n<p><strong>Special Characters List</strong>\n</p>\n<p>Space (   )</p>\n<p>Exclamation mark ( ! )</p>\n<p>Double quotes (or speech marks) ( “ )</p>\n<p>Number ( # )</p>\n<p>Dollar ( $ )</p>\n<p>Procenttecken ( % )</p>\n<p>Ampersand ( &amp; )</p>\n<p>Single quote ( ‘ )</p>\n<p>Open parenthesis (or open bracket) ( ( )</p>\n<p>Close parenthesis (or close bracket) ( ) )</p>\n<p>Asterisk ( * )</p>\n<p>Plus ( + )</p>\n<p>Comma ( , )</p>\n<p>Hyphen ( - )</p>\n<p>Period, dot or full stop ( . )</p>\n<p>Slash or divide ( / )</p>\n<p>Zero ( 0 )</p>\n<p>One ( 1 )</p>\n<p>Two ( 2 )</p>\n<p>Three ( 3 )</p>\n<p>Four ( 4 )</p>\n<p>Five ( 5 )</p>\n<p>Six ( 6 )</p>\n<p>Seven ( 7 )</p>\n<p>Eight ( 8 )</p>\n<p>Nine ( 9 )</p>\n<p>Colon ( : )</p>\n<p>Semicolon ( ; )</p>\n<p>Less than (or open angled bracket) ( &lt; )</p>\n<p>Equals ( = )</p>\n<p>Greater than (or close angled bracket) ( > )</p>\n<p>Question mark ( ? )</p>\n<p>At symbol ( @ )</p>\n<p>Uppercase A ( A )</p>\n<p>Uppercase B ( B )</p>\n<p>Uppercase C ( C )</p>\n<p>Uppercase D ( D )</p>\n<p>Uppercase E ( E )</p>\n<p>Uppercase F ( F )</p>\n<p>Uppercase G ( G )</p>\n<p>Uppercase H ( H )</p>\n<p>Uppercase I ( I )</p>\n<p>Uppercase J ( J )</p>\n<p>Uppercase K ( K )</p>\n<p>Uppercase L ( L )</p>\n<p>Uppercase M ( M )</p>\n<p>Uppercase N ( N )</p>\n<p>Uppercase O ( O )</p>\n<p>Uppercase P ( P )</p>\n<p>Uppercase Q ( Q )</p>\n<p>Uppercase R ( R )</p>\n<p>Uppercase S ( S )</p>\n<p>Uppercase T ( T )</p>\n<p>Uppercase U ( U )</p>\n<p>Uppercase V ( V )</p>\n<p>Uppercase W ( W )</p>\n<p>Uppercase X ( X )</p>\n<p>Uppercase Y ( Y )</p>\n<p>Uppercase Z ( Z )</p>\n<p>Opening bracket ( [ )</p>\n<p>Backslash ( \\\\ )</p>\n<p>Closing bracket ( ] )</p>\n<p>Caret - circumflex ( ^ )</p>\n<p>Underscore ( _ )</p>\n<p>Grave accent ( ` )</p>\n<p>Lowercase a ( a )</p>\n<p>Lowercase b ( b )</p>\n<p>Lowercase c ( c )</p>\n<p>Lowercase d ( d )</p>\n<p>Lowercase e ( e )</p>\n<p>Lowercase f ( f )</p>\n<p>Lowercase g ( g )</p>\n<p>Lowercase h ( h )</p>\n<p>Lowercase i ( i )</p>\n<p>Lowercase j ( j )</p>\n<p>Lowercase k ( k )</p>\n<p>Lowercase l ( l )</p>\n<p>Lowercase m ( m )</p>\n<p>Lowercase n ( n )</p>\n<p>Lowercase o ( o )</p>\n<p>Lowercase p ( p )</p>\n<p>Lowercase q ( q )</p>\n<p>Lowercase r ( r )</p>\n<p>Lowercase s ( s )</p>\n<p>Lowercase t ( t )</p>\n<p>Lowercase u ( u )</p>\n<p>Lowercase v ( v )</p>\n<p>Lowercase w ( w )</p>\n<p>Lowercase x ( x )</p>\n<p>Lowercase y ( y )</p>\n<p>Lowercase z ( z )</p>\n<p>Opening brace ( { )</p>\n<p>Vertical bar ( | )</p>\n<p>Closing brace ( } )</p>\n<p>Equivalency sign - tilde ( ~ )</p>\n<p>Delete ( )</p>\n<p>Euro sign ( € )</p>\n<p>Single low-9 quotation mark ( ‚ )</p>\n<p>Latin small letter f with hook ( ƒ )</p>\n<p>Double low-9 quotation mark ( „ )</p>\n<p>Horizontal ellipsis ( … )</p>\n<p>Dagger ( † )</p>\n<p>Double dagger ( ‡ )</p>\n<p>Modifier letter circumflex accent ( ˆ )</p>\n<p>Per mille sign ( ‰ )</p>\n<p>Latin capital letter S with caron ( Š )</p>\n<p>Single left-pointing angle quotation ( ‹ )</p>\n<p>Latin capital ligature OE ( Œ )</p>\n<p>Latin captial letter Z with caron ( Ž )</p>\n<p>Left single quotation mark ( ‘ )</p>\n<p>Right single quotation mark ( ’ )</p>\n<p>Left double quotation mark ( “ )</p>\n<p>Right double quotation mark ( ” )</p>\n<p>Bullet ( • )</p>\n<p>En dash ( – )</p>\n<p>Em dash ( — )</p>\n<p>Small tilde ( ˜ )</p>\n<p>Trade mark sign ( ™ )</p>\n<p>Latin small letter S with caron ( š )</p>\n<p>Single right-pointing angle quotation mark ( › )</p>\n<p>Latin small ligature oe ( œ )</p>\n<p>Latin small letter z with caron ( ž )</p>\n<p>Latin capital letter Y with diaeresis ( Ÿ )</p>\n<p>Non-breaking space (   )</p>\n<p>Inverted exclamation mark ( ¡ )</p>\n<p>Cent sign ( ¢ )</p>\n<p>Pound sign ( £ )</p>\n<p>Currency sign ( ¤ )</p>\n<p>Yen sign ( ¥ )</p>\n<p>Pipe, Broken vertical bar ( ¦ )</p>\n<p>Section sign ( § )</p>\n<p>Spacing diaeresis - umlaut ( ¨ )</p>\n<p>Copyright sign ( © )</p>\n<p>Feminine ordinal indicator ( ª )</p>\n<p>Left double angle quotes ( « )</p>\n<p>Not sign ( ¬ )</p>\n<p>Soft hyphen ( )</p>\n<p>Registered trade mark sign ( ® )</p>\n<p>Spacing macron - overline ( ¯ )</p>\n<p>Degree sign ( ° )</p>\n<p>Plus-or-minus sign ( ± )</p>\n<p>Superscript two - squared ( ² )</p>\n<p>Superscript three - cubed ( ³ )</p>\n<p>Acute accent - spacing acute ( ´ )</p>\n<p>Micro sign ( µ )</p>\n<p>Pilcrow sign - paragraph sign ( ¶ )</p>\n<p>Middle dot - Georgian comma ( · )</p>\n<p>Spacing cedilla ( ¸ )</p>\n<p>Superscript one ( ¹ )</p>\n<p>Masculine ordinal indicator ( º )</p>\n<p>Right double angle quotes ( » )</p>\n<p>Fraction one quarter ( ¼ )</p>\n<p>Fraction one half ( ½ )</p>\n<p>Fraction three quarters ( ¾ )</p>\n<p>Inverted question mark ( ¿ )</p>\n<p>Latin capital letter A with grave ( À )</p>\n<p>Latin capital letter A with acute ( Á )</p>\n<p>Latin capital letter A with circumflex ( Â )</p>\n<p>Latin capital letter A with tilde ( Ã )</p>\n<p>Latin capital letter A with diaeresis ( Ä )</p>\n<p>Latin capital letter A with ring above ( Å )</p>\n<p>Latin capital letter AE ( Æ )</p>\n<p>Latin capital letter C with cedilla ( Ç )</p>\n<p>Latin capital letter E with grave ( È )</p>\n<p>Latin capital letter E with acute ( É )</p>\n<p>Latin capital letter E with circumflex ( Ê )</p>\n<p>Latin capital letter E with diaeresis ( Ë )</p>\n<p>Latin capital letter I with grave ( Ì )</p>\n<p>Latin capital letter I with acute ( Í )</p>\n<p>Latin capital letter I with circumflex ( Î )</p>\n<p>Latin capital letter I with diaeresis ( Ï )</p>\n<p>Latin capital letter ETH ( Ð )</p>\n<p>Latin capital letter N with tilde ( Ñ )</p>\n<p>Latin capital letter O with grave ( Ò )</p>\n<p>Latin capital letter O with acute ( Ó )</p>\n<p>Latin capital letter O with circumflex ( Ô )</p>\n<p>Latin capital letter O with tilde ( Õ )</p>\n<p>Latin capital letter O with diaeresis ( Ö )</p>\n<p>Multiplication sign ( × )</p>\n<p>Latin capital letter O with slash ( Ø )</p>\n<p>Latin capital letter U with grave ( Ù )</p>\n<p>Latin capital letter U with acute ( Ú )</p>\n<p>Latin capital letter U with circumflex ( Û )</p>\n<p>Latin capital letter U with diaeresis ( Ü )</p>\n<p>Latin capital letter Y with acute ( Ý )</p>\n<p>Latin capital letter THORN ( Þ )</p>\n<p>Latin small letter sharp s - ess-zed ( ß )</p>\n<p>Latin small letter a with grave ( à )</p>\n<p>Latin small letter a with acute ( á )</p>\n<p>Latin small letter a with circumflex ( â )</p>\n<p>Latin small letter a with tilde ( ã )</p>\n<p>Latin small letter a with diaeresis ( ä )</p>\n<p>Latin small letter a with ring above ( å )</p>\n<p>Latin small letter ae ( æ )</p>\n<p>Latin small letter c with cedilla ( ç )</p>\n<p>Latin small letter e with grave ( è )</p>\n<p>Latin small letter e with acute ( é )</p>\n<p>Latin small letter e with circumflex ( ê )</p>\n<p>Latin small letter e with diaeresis ( ë )</p>\n<p>Latin small letter i with grave ( ì )</p>\n<p>Latin small letter i with acute ( í )</p>\n<p>Latin small letter i with circumflex ( î )</p>\n<p>Latin small letter i with diaeresis ( ï )</p>\n<p>Latin small letter eth ( ð )</p>\n<p>Latin small letter n with tilde ( ñ )</p>\n<p>Latin small letter o with grave ( ò )</p>\n<p>Latin small letter o with acute ( ó )</p>\n<p>Latin small letter o with circumflex ( ô )</p>\n<p>Latin small letter o with tilde ( õ )</p>\n<p>Latin small letter o with diaeresis ( ö )</p>\n<p>Division sign ( ÷ )</p>\n<p>Latin small letter o with slash ( ø )</p>\n<p>Latin small letter u with grave ( ù )</p>\n<p>Latin small letter u with acute ( ú )</p>\n<p>Latin small letter u with circumflex ( û )</p>\n<p>Latin small letter u with diaeresis ( ü )</p>\n<p>Latin small letter y with acute ( ý )</p>\n<p>Latin small letter thorn ( þ )</p>\n<p>Latin small letter y with diaeresis ( ÿ )</p>\n<p>END OF LIST</p>\n<p><strong>-- Paragraphs -- </strong>\n</p>\n<p>This is a paragraph.</p>\n<p>This is another paragraph.</p>\n<p>This paragraph contains a nested <p>This is the nested paragraph.</p> paragraph.</p>\n<p>The next paragraph is empty.</p>\n<p>The previous paragraph is empty.</p>\n<p>Paragraph tag with title and onclick attributes</p>\n<p>Text separated by line break 1<br/>Text separated by line break 2</p>\n<h1>Heading 1</h1>\n<h2>Heading 2</h2>\n<h3>Heading 3</h3>\n<h4>Heading 4</h4>\n<h5>Heading 5</h5>\n<h6>Heading 6</h6>\n<ol>Ordered List<li>Item 1 </li>\n<li>Item 2</li>\n<li>Item 3</li>\n</ol>\n<ul>Unordered List  <li>Item 1</li>\n<li>Item 2</li>\n<li>Item 3</li>\n</ul>\n<p><strong>Strong With Title Attribute</strong>\n</p>\n<p><strong>-- Self Closing hr Tag --</strong>\n</p>\n<p><strong>-- Content Within Unwanted Tags --</strong>\n</p>\n<p>Script Tag: \n\n</p>\n\n</body>";

        checkTransformation(original, expected);
    }


    @Test
    public void shouldRemoveVideoAndStripOutSurroundingTag(){
        String original = "<body><p>He and a shellshocked woman called Hadiza sheltered there for three days while the marauding militants looted and burnt houses. At night the two curled up beside the bullet-pocked wall and fell into an exhausted sleep to the sound of celebratory gunshots. One morning Hadiza crept out to find water and never returned. By nightfall, Idris decided to run.</p>\n<p><a href=\"http://player.vimeo.com/video/69104660\"></a></p>\n<p>“When I reached the bush, I was relieved at first but then I saw bodies everywhere. I walked through five villages and each one I passed was empty except for dead bodies.”</p>\n\n\n\n</body>";
        String expected = "<body><p>He and a shellshocked woman called Hadiza sheltered there for three days while the marauding militants looted and burnt houses. At night the two curled up beside the bullet-pocked wall and fell into an exhausted sleep to the sound of celebratory gunshots. One morning Hadiza crept out to find water and never returned. By nightfall, Idris decided to run.</p>\n<p>“When I reached the bush, I was relieved at first but then I saw bodies everywhere. I walked through five villages and each one I passed was empty except for dead bodies.”</p>\n\n\n\n</body>" ;

        checkTransformation(original, expected);
    }


    @Test
    public void shouldRemoveFastFTVideoAttachment(){
        String original = "<body><p>Anton Howes discusses the standard theories regarding the causes of a 1500% GDP increase during the industrial revolution.</p><a href=\"https://www.youtube.com/watch?v=nkEa0zTdJ-8\" data-asset-type=\"video\" data-embedded=\"true\" title=\"Causes of the Industrial Revolution\">Causes of the Industrial Revolution</a></body>";
        String expected = "<body><p>Anton Howes discusses the standard theories regarding the causes of a 1500% GDP increase during the industrial revolution.</p></body>" ;

        checkTransformation(original, expected);
    }

    @Test
    public void shouldRemoveOldStyleFastFTVideoAttachment(){
        String original = "<body><p>Anton Howes discusses the standard theories regarding the causes of a 1500% GDP increase during the industrial revolution.</p><a href=\"https://www.youtube.com/watch?v=nkEa0zTdJ-8\"></a></body>";
        String expected = "<body><p>Anton Howes discusses the standard theories regarding the causes of a 1500% GDP increase during the industrial revolution.</p></body>" ;

        checkTransformation(original, expected);
    }

    @Test
    public void shouldNotRemoveAStandardLinkEvenToYouTube(){
        String original = "<body><p>Anton Howes discusses the standard theories regarding the causes of a 1500% GDP increase during the industrial revolution.</p><a href=\"https://www.youtube.com/watch?v=nkEa0zTdJ-8\">Informative Video</a></body>";

        checkTransformation(original, original);
    }

    @Test
    public void shouldRemoveInlineImagesAndStripOutSurroundingTag(){
        String original = "<body><p>He sheltered there.</p>\n<p><ft-content type=\"http://www.ft.com/ontology/content/ImageSet\" url=\"https://api.ft.com/content/a4456c3a-6a6a-11e4-8fca-00144feabdc0\">image title</ft-content></p>\n<p>“I saw bodies everywhere.”</p>\n\n\n\n</body>";
        String expected = "<body><p>He sheltered there.</p>\n<p>“I saw bodies everywhere.”</p>\n\n\n\n</body>" ;

        checkTransformation(original, expected);
    }

    @Test
    public void shouldRetainInlineArticleReferences(){
        String original = "<body><p>He sheltered there.</p>\n<p><ft-content type=\"http://www.ft.com/ontology/content/Article\" url=\"https://api.ft.com/content/b4456c3a-6a6a-11e4-8fca-00144feabdc1\">article title</ft-content></p>\n<p>“I saw bodies everywhere.”</p>\n\n\n\n</body>";

        checkTransformation(original, original);
    }

    @Test
    public void testShouldRemoveInlineExternalImages() throws Exception {
        String original = "<body><p>He sheltered there.<img alt=\"Saloua Raouda Choucair's ‘Composition'\" height=\"445\" src=\"http://im.ft-static.com/content/images/7784185e-a888-11e2-8e5d-00144feabdc0.img\" width=\"600\"/></p>\n\n\n\n</body>";
        String expected = "<body><p>He sheltered there.</p>\n\n\n\n</body>" ;

        checkTransformation(original, expected);
    }

    private void checkTransformation(String originalBody, String expectedTransformedBody) {
        String actualTransformedBody = bodyTransformer.transform(originalBody, TRANSACTION_ID);
        assertThat(actualTransformedBody, is(equivalentToUnixString(expectedTransformedBody)));
    }

}
