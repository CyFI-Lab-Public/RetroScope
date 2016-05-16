/**
 * Copyright (c) 2004, Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.mail.common.html.parser;

import com.google.common.collect.Maps;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * HTML4 contains HTML 4.0 definitions and specifications
 * See http://www.w3.org/TR/html401/
 * See http://www.w3.org/TR/html401/index/elements.html
 * See http://www.w3.org/TR/html401/index/attributes.html
 *
 * @author jlim@google.com (Jing Yee Lim)
 */
public final class HTML4 {

  /** Map of all elements */
  private static final HashMap<String,HTML.Element> elements = Maps.newHashMap();

  /** Map of all attributes */
  private static final HashMap<String,HTML.Attribute> attributes = Maps.newHashMap();

  /** Default Whitelist */
  private static final HtmlWhitelist defaultWhitelist = new HtmlWhitelist() {
    /**
     * @see com.google.common.html.parser.HtmlWhitelist#lookupElement(String)
     */
    public HTML.Element lookupElement(String name) {
      return HTML4.lookupElement(name);
    }

    /**
     * @see com.google.common.html.parser.HtmlWhitelist#lookupAttribute(String)
     */
    public HTML.Attribute lookupAttribute(String name) {
      return HTML4.lookupAttribute(name);
    }
  };

  /** Gets the default Whitelist */
  public static HtmlWhitelist getWhitelist() {
    return HTML4.defaultWhitelist;
  }

  /** Looks for a HTML4 element */
  public static HTML.Element lookupElement(String name) {
    return elements.get(name.toLowerCase());
  }

  /** Looks for a HTML4 attribute */
  public static HTML.Attribute lookupAttribute(String name) {
    return attributes.get(name.toLowerCase());
  }

  /**
   * @return Unmodifiable Map of all valid HTML4 elements.  Key is lowercase
   * element name.
   */
  public static Map<String, HTML.Element> getAllElements() {
    return Collections.unmodifiableMap(elements);
  }

  /**
   * @return Unmodifiable Map of all valid HTML4 attributes.  Key is lowercase
   * attribute name.
   */
  public static Map<String, HTML.Attribute> getAllAttributes() {
    return Collections.unmodifiableMap(attributes);
  }

  /** Creates and adds a element to the map */
  private static HTML.Element addElement(String tag, String flags) {
    return addElement(tag, flags, HTML.Element.Flow.NONE);
  }

  /** Creates and adds a element to the map */
  private static HTML.Element addElement(String tag, String flags, HTML.Element.Flow flow) {
    return addElement(tag, flags, flow, HTML.Element.NO_TYPE);
  }

  /** Creates and adds a element to the map */
  private static HTML.Element addTableElement(String tag, String flags, HTML.Element.Flow flow) {
    return addElement(tag, flags, flow, HTML.Element.TABLE_TYPE);
  }

  /** Creates and adds a element to the map */
  private static HTML.Element addElement(String tag, String flags, HTML.Element.Flow flow,
      int type) {
    tag = tag.toLowerCase();

    boolean empty = false;
    boolean optionalEndTag = false;
    boolean breaksFlow = false;
    for (int i = 0; i < flags.length(); i++) {
      switch (flags.charAt(i)) {
        case 'E': empty = true; break;
        case 'O': optionalEndTag = true; break;
        case 'B': breaksFlow = true; break;
        default: throw new Error("Unknown element flag");
      }
    }
    HTML.Element element = new HTML.Element(tag, type, empty, optionalEndTag, breaksFlow, flow);
    elements.put(tag, element);
    return element;
  }

  /** Creates and add an attribute to the map */
  private static HTML.Attribute addAttribute(String attribute) {
    return addAttribute(attribute, HTML.Attribute.NO_TYPE);
  }

  private static HTML.Attribute addAttribute(String attribute, int type) {
    return addAttribute(attribute, type, null);
  }

  private static HTML.Attribute addAttribute(String attribute,
                                             int type,
                                             String[] values) {
    attribute = attribute.toLowerCase();
    Set<String> valueSet = null;
    if (values != null) {
      valueSet = new HashSet<String>();
      for (String x : values) {
        valueSet.add(x.toLowerCase());
      }
      valueSet = Collections.unmodifiableSet(valueSet);
    }
    HTML.Attribute attr = new HTML.Attribute(attribute, type, valueSet);
    attributes.put(attribute, attr);
    return attr;
  }

  /**
   * All HTML4 elements.
   *
   * Block vs inline flow:
   * http://www.w3.org/TR/REC-html40/sgml/dtd.html#block
   * http://www.w3.org/TR/REC-html40/sgml/dtd.html#inline
   * Some deprecated elements aren't listed there so we make an educated guess:
   * - CENTER is equivalent to DIV align=center, so we make it BLOCK.
   * - S, STRIKE and FONT are clearly inline like U, I, etc.
   * - MENU and DIR are like OL and UL, so we make them block.
   *
   * Optional end tag and empty:
   * http://www.w3.org/TR/REC-html40/index/elements.html
   */
  public static final HTML.Element
    A_ELEMENT          = addElement("A", "", HTML.Element.Flow.INLINE),
    ABBR_ELEMENT       = addElement("ABBR", "", HTML.Element.Flow.INLINE),
    ACRONYM_ELEMENT    = addElement("ACRONYM", "", HTML.Element.Flow.INLINE),
    ADDRESS_ELEMENT    = addElement("ADDRESS", "", HTML.Element.Flow.BLOCK),
    APPLET_ELEMENT     = addElement("APPLET", ""),
    AREA_ELEMENT       = addElement("AREA", "E"),
    B_ELEMENT          = addElement("B", "", HTML.Element.Flow.INLINE),
    BASE_ELEMENT       = addElement("BASE", "E"),
    BASEFONT_ELEMENT   = addElement("BASEFONT", "E"),
    BDO_ELEMENT        = addElement("BDO", "", HTML.Element.Flow.INLINE),
    BIG_ELEMENT        = addElement("BIG", "", HTML.Element.Flow.INLINE),
    BLOCKQUOTE_ELEMENT = addElement("BLOCKQUOTE", "B", HTML.Element.Flow.BLOCK),
    BODY_ELEMENT       = addElement("BODY", "O"),
    BR_ELEMENT         = addElement("BR", "EB", HTML.Element.Flow.INLINE),
    BUTTON_ELEMENT     = addElement("BUTTON", "", HTML.Element.Flow.INLINE),
    CAPTION_ELEMENT    = addTableElement("CAPTION", "", HTML.Element.Flow.NONE),
    CENTER_ELEMENT     = addElement("CENTER", "B", HTML.Element.Flow.BLOCK),
    CITE_ELEMENT       = addElement("CITE", "", HTML.Element.Flow.INLINE),
    CODE_ELEMENT       = addElement("CODE", "", HTML.Element.Flow.INLINE),
    COL_ELEMENT        = addTableElement("COL", "E", HTML.Element.Flow.NONE),
    COLGROUP_ELEMENT   = addTableElement("COLGROUP", "O", HTML.Element.Flow.NONE),
    DD_ELEMENT         = addElement("DD", "OB"),
    DEL_ELEMENT        = addElement("DEL", ""),
    DFN_ELEMENT        = addElement("DFN", "", HTML.Element.Flow.INLINE),
    DIR_ELEMENT        = addElement("DIR", "B", HTML.Element.Flow.BLOCK),
    DIV_ELEMENT        = addElement("DIV", "B", HTML.Element.Flow.BLOCK),
    DL_ELEMENT         = addElement("DL", "B", HTML.Element.Flow.BLOCK),
    DT_ELEMENT         = addElement("DT", "OB"),
    EM_ELEMENT         = addElement("EM", "", HTML.Element.Flow.INLINE),
    FIELDSET_ELEMENT   = addElement("FIELDSET", "", HTML.Element.Flow.BLOCK),
    FONT_ELEMENT       = addElement("FONT", "", HTML.Element.Flow.INLINE),
    FORM_ELEMENT       = addElement("FORM", "B", HTML.Element.Flow.BLOCK),
    FRAME_ELEMENT      = addElement("FRAME", "E"),
    FRAMESET_ELEMENT   = addElement("FRAMESET", ""),
    H1_ELEMENT         = addElement("H1", "B", HTML.Element.Flow.BLOCK),
    H2_ELEMENT         = addElement("H2", "B", HTML.Element.Flow.BLOCK),
    H3_ELEMENT         = addElement("H3", "B", HTML.Element.Flow.BLOCK),
    H4_ELEMENT         = addElement("H4", "B", HTML.Element.Flow.BLOCK),
    H5_ELEMENT         = addElement("H5", "B", HTML.Element.Flow.BLOCK),
    H6_ELEMENT         = addElement("H6", "B", HTML.Element.Flow.BLOCK),
    HEAD_ELEMENT       = addElement("HEAD", "OB"),
    HR_ELEMENT         = addElement("HR", "EB", HTML.Element.Flow.BLOCK),
    HTML_ELEMENT       = addElement("HTML", "OB"),
    I_ELEMENT          = addElement("I", "", HTML.Element.Flow.INLINE),
    IFRAME_ELEMENT     = addElement("IFRAME", ""),
    IMG_ELEMENT        = addElement("IMG", "E", HTML.Element.Flow.INLINE),
    INPUT_ELEMENT      = addElement("INPUT", "E", HTML.Element.Flow.INLINE),
    INS_ELEMENT        = addElement("INS", ""),
    ISINDEX_ELEMENT    = addElement("ISINDEX", "EB"),
    KBD_ELEMENT        = addElement("KBD", "", HTML.Element.Flow.INLINE),
    LABEL_ELEMENT      = addElement("LABEL", "", HTML.Element.Flow.INLINE),
    LEGEND_ELEMENT     = addElement("LEGEND", ""),
    LI_ELEMENT         = addElement("LI", "OB"),
    LINK_ELEMENT       = addElement("LINK", "E"),
    MAP_ELEMENT        = addElement("MAP", "", HTML.Element.Flow.INLINE),
    MENU_ELEMENT       = addElement("MENU", "B", HTML.Element.Flow.BLOCK),
    META_ELEMENT       = addElement("META", "E"),
    NOFRAMES_ELEMENT   = addElement("NOFRAMES", "B"),
    NOSCRIPT_ELEMENT   = addElement("NOSCRIPT", "", HTML.Element.Flow.BLOCK),
    OBJECT_ELEMENT     = addElement("OBJECT", "", HTML.Element.Flow.INLINE),
    OL_ELEMENT         = addElement("OL", "B", HTML.Element.Flow.BLOCK),
    OPTGROUP_ELEMENT   = addElement("OPTGROUP", ""),
    OPTION_ELEMENT     = addElement("OPTION", "O"),
    P_ELEMENT          = addElement("P", "OB", HTML.Element.Flow.BLOCK),
    PARAM_ELEMENT      = addElement("PARAM", "E"),
    PRE_ELEMENT        = addElement("PRE", "B", HTML.Element.Flow.BLOCK),
    Q_ELEMENT          = addElement("Q", "", HTML.Element.Flow.INLINE),
    S_ELEMENT          = addElement("S", "", HTML.Element.Flow.INLINE),
    SAMP_ELEMENT       = addElement("SAMP", "", HTML.Element.Flow.INLINE),
    SCRIPT_ELEMENT     = addElement("SCRIPT", "", HTML.Element.Flow.INLINE),
    SELECT_ELEMENT     = addElement("SELECT", "", HTML.Element.Flow.INLINE),
    SMALL_ELEMENT      = addElement("SMALL", "", HTML.Element.Flow.INLINE),
    SPAN_ELEMENT       = addElement("SPAN", "", HTML.Element.Flow.INLINE),
    STRIKE_ELEMENT     = addElement("STRIKE", "", HTML.Element.Flow.INLINE),
    STRONG_ELEMENT     = addElement("STRONG", "", HTML.Element.Flow.INLINE),
    STYLE_ELEMENT      = addElement("STYLE", ""),
    SUB_ELEMENT        = addElement("SUB", "", HTML.Element.Flow.INLINE),
    SUP_ELEMENT        = addElement("SUP", "", HTML.Element.Flow.INLINE),
    TABLE_ELEMENT      = addTableElement("TABLE", "B", HTML.Element.Flow.BLOCK),
    TBODY_ELEMENT      = addTableElement("TBODY", "O", HTML.Element.Flow.NONE),
    TD_ELEMENT         = addTableElement("TD", "OB", HTML.Element.Flow.NONE),
    TEXTAREA_ELEMENT   = addElement("TEXTAREA", "", HTML.Element.Flow.INLINE),
    TFOOT_ELEMENT      = addTableElement("TFOOT", "O", HTML.Element.Flow.NONE),
    TH_ELEMENT         = addTableElement("TH", "OB", HTML.Element.Flow.NONE),
    THEAD_ELEMENT      = addTableElement("THEAD", "O", HTML.Element.Flow.NONE),
    TITLE_ELEMENT      = addElement("TITLE", "B"),
    TR_ELEMENT         = addTableElement("TR", "OB", HTML.Element.Flow.NONE),
    TT_ELEMENT         = addElement("TT", "", HTML.Element.Flow.INLINE),
    U_ELEMENT          = addElement("U", "", HTML.Element.Flow.INLINE),
    UL_ELEMENT         = addElement("UL", "B", HTML.Element.Flow.BLOCK),
    VAR_ELEMENT        = addElement("VAR", "", HTML.Element.Flow.INLINE);

  /**
   * All the HTML4 attributes
   */
  public static final HTML.Attribute
    ABBR_ATTRIBUTE           = addAttribute("ABBR"),
    ACCEPT_ATTRIBUTE         = addAttribute("ACCEPT"),
    ACCEPT_CHARSET_ATTRIBUTE = addAttribute("ACCEPT-CHARSET"),
    ACCESSKEY_ATTRIBUTE      = addAttribute("ACCESSKEY"),
    ACTION_ATTRIBUTE         = addAttribute("ACTION", HTML.Attribute.URI_TYPE),
    ALIGN_ATTRIBUTE          = addAttribute("ALIGN",
        HTML.Attribute.ENUM_TYPE,
        new String[] {"left", "center", "right", "justify",
            "char", "top", "bottom", "middle"}),
    ALINK_ATTRIBUTE          = addAttribute("ALINK"),
    ALT_ATTRIBUTE            = addAttribute("ALT"),
    ARCHIVE_ATTRIBUTE        = addAttribute("ARCHIVE", HTML.Attribute.URI_TYPE),
    AXIS_ATTRIBUTE           = addAttribute("AXIS"),
    BACKGROUND_ATTRIBUTE     = addAttribute("BACKGROUND", HTML.Attribute.URI_TYPE),
    BGCOLOR_ATTRIBUTE        = addAttribute("BGCOLOR"),
    BORDER_ATTRIBUTE         = addAttribute("BORDER"),
    CELLPADDING_ATTRIBUTE    = addAttribute("CELLPADDING"),
    CELLSPACING_ATTRIBUTE    = addAttribute("CELLSPACING"),
    CHAR_ATTRIBUTE           = addAttribute("CHAR"),
    CHAROFF_ATTRIBUTE        = addAttribute("CHAROFF"),
    CHARSET_ATTRIBUTE        = addAttribute("CHARSET"),
    CHECKED_ATTRIBUTE        = addAttribute("CHECKED", HTML.Attribute.BOOLEAN_TYPE),
    CITE_ATTRIBUTE           = addAttribute("CITE", HTML.Attribute.URI_TYPE),
    CLASS_ATTRIBUTE          = addAttribute("CLASS"),
    CLASSID_ATTRIBUTE        = addAttribute("CLASSID", HTML.Attribute.URI_TYPE),
    CLEAR_ATTRIBUTE          = addAttribute("CLEAR",
        HTML.Attribute.ENUM_TYPE,
        new String[] {"left", "all", "right", "none"}),
    CODE_ATTRIBUTE           = addAttribute("CODE"),
    CODEBASE_ATTRIBUTE       = addAttribute("CODEBASE", HTML.Attribute.URI_TYPE),
    CODETYPE_ATTRIBUTE       = addAttribute("CODETYPE"),
    COLOR_ATTRIBUTE          = addAttribute("COLOR"),
    COLS_ATTRIBUTE           = addAttribute("COLS"),
    COLSPAN_ATTRIBUTE        = addAttribute("COLSPAN"),
    COMPACT_ATTRIBUTE        = addAttribute("COMPACT", HTML.Attribute.BOOLEAN_TYPE),
    CONTENT_ATTRIBUTE        = addAttribute("CONTENT"),
    COORDS_ATTRIBUTE         = addAttribute("COORDS"),
    DATA_ATTRIBUTE           = addAttribute("DATA", HTML.Attribute.URI_TYPE),
    DATETIME_ATTRIBUTE       = addAttribute("DATETIME"),
    DECLARE_ATTRIBUTE        = addAttribute("DECLARE", HTML.Attribute.BOOLEAN_TYPE),
    DEFER_ATTRIBUTE          = addAttribute("DEFER", HTML.Attribute.BOOLEAN_TYPE),
    DIR_ATTRIBUTE            = addAttribute("DIR",
        HTML.Attribute.ENUM_TYPE,
        new String[] {"ltr", "rtl"}),
    DISABLED_ATTRIBUTE       = addAttribute("DISABLED", HTML.Attribute.BOOLEAN_TYPE),
    ENCTYPE_ATTRIBUTE        = addAttribute("ENCTYPE"),
    FACE_ATTRIBUTE           = addAttribute("FACE"),
    FOR_ATTRIBUTE            = addAttribute("FOR"),
    FRAME_ATTRIBUTE          = addAttribute("FRAME"),
    FRAMEBORDER_ATTRIBUTE    = addAttribute("FRAMEBORDER",
        HTML.Attribute.ENUM_TYPE,
        new String[] {"1", "0"}),
    HEADERS_ATTRIBUTE        = addAttribute("HEADERS"),
    HEIGHT_ATTRIBUTE         = addAttribute("HEIGHT"),
    HREF_ATTRIBUTE           = addAttribute("HREF", HTML.Attribute.URI_TYPE),
    HREFLANG_ATTRIBUTE       = addAttribute("HREFLANG"),
    HSPACE_ATTRIBUTE         = addAttribute("HSPACE"),
    HTTP_EQUIV_ATTRIBUTE     = addAttribute("HTTP-EQUIV"),
    ID_ATTRIBUTE             = addAttribute("ID"),
    ISMAP_ATTRIBUTE          = addAttribute("ISMAP", HTML.Attribute.BOOLEAN_TYPE),
    LABEL_ATTRIBUTE          = addAttribute("LABEL"),
    LANG_ATTRIBUTE           = addAttribute("LANG"),
    LANGUAGE_ATTRIBUTE       = addAttribute("LANGUAGE"),
    LINK_ATTRIBUTE           = addAttribute("LINK"),
    LONGDESC_ATTRIBUTE       = addAttribute("LONGDESC", HTML.Attribute.URI_TYPE),
    MARGINHEIGHT_ATTRIBUTE   = addAttribute("MARGINHEIGHT"),
    MARGINWIDTH_ATTRIBUTE    = addAttribute("MARGINWIDTH"),
    MAXLENGTH_ATTRIBUTE      = addAttribute("MAXLENGTH"),
    MEDIA_ATTRIBUTE          = addAttribute("MEDIA"),
    METHOD_ATTRIBUTE         = addAttribute("METHOD",
        HTML.Attribute.ENUM_TYPE,
        new String[] {"get", "post"}),
    MULTIPLE_ATTRIBUTE       = addAttribute("MULTIPLE", HTML.Attribute.BOOLEAN_TYPE),
    NAME_ATTRIBUTE           = addAttribute("NAME"),
    NOHREF_ATTRIBUTE         = addAttribute("NOHREF", HTML.Attribute.BOOLEAN_TYPE),
    NORESIZE_ATTRIBUTE       = addAttribute("NORESIZE", HTML.Attribute.BOOLEAN_TYPE),
    NOSHADE_ATTRIBUTE        = addAttribute("NOSHADE", HTML.Attribute.BOOLEAN_TYPE),
    NOWRAP_ATTRIBUTE         = addAttribute("NOWRAP", HTML.Attribute.BOOLEAN_TYPE),
    OBJECT_ATTRIBUTE         = addAttribute("OBJECT"),
    ONBLUR_ATTRIBUTE         = addAttribute("ONBLUR", HTML.Attribute.SCRIPT_TYPE),
    ONCHANGE_ATTRIBUTE       = addAttribute("ONCHANGE", HTML.Attribute.SCRIPT_TYPE),
    ONCLICK_ATTRIBUTE        = addAttribute("ONCLICK", HTML.Attribute.SCRIPT_TYPE),
    ONDBLCLICK_ATTRIBUTE     = addAttribute("ONDBLCLICK", HTML.Attribute.SCRIPT_TYPE),
    ONFOCUS_ATTRIBUTE        = addAttribute("ONFOCUS", HTML.Attribute.SCRIPT_TYPE),
    ONKEYDOWN_ATTRIBUTE      = addAttribute("ONKEYDOWN", HTML.Attribute.SCRIPT_TYPE),
    ONKEYPRESS_ATTRIBUTE     = addAttribute("ONKEYPRESS", HTML.Attribute.SCRIPT_TYPE),
    ONKEYUP_ATTRIBUTE        = addAttribute("ONKEYUP", HTML.Attribute.SCRIPT_TYPE),
    ONLOAD_ATTRIBUTE         = addAttribute("ONLOAD", HTML.Attribute.SCRIPT_TYPE),
    ONMOUSEDOWN_ATTRIBUTE    = addAttribute("ONMOUSEDOWN", HTML.Attribute.SCRIPT_TYPE),
    ONMOUSEMOVE_ATTRIBUTE    = addAttribute("ONMOUSEMOVE", HTML.Attribute.SCRIPT_TYPE),
    ONMOUSEOUT_ATTRIBUTE     = addAttribute("ONMOUSEOUT", HTML.Attribute.SCRIPT_TYPE),
    ONMOUSEOVER_ATTRIBUTE    = addAttribute("ONMOUSEOVER", HTML.Attribute.SCRIPT_TYPE),
    ONMOUSEUP_ATTRIBUTE      = addAttribute("ONMOUSEUP", HTML.Attribute.SCRIPT_TYPE),
    ONRESET_ATTRIBUTE        = addAttribute("ONRESET", HTML.Attribute.SCRIPT_TYPE),
    ONSELECT_ATTRIBUTE       = addAttribute("ONSELECT", HTML.Attribute.SCRIPT_TYPE),
    ONSUBMIT_ATTRIBUTE       = addAttribute("ONSUBMIT", HTML.Attribute.SCRIPT_TYPE),
    ONUNLOAD_ATTRIBUTE       = addAttribute("ONUNLOAD", HTML.Attribute.SCRIPT_TYPE),
    PROFILE_ATTRIBUTE        = addAttribute("PROFILE", HTML.Attribute.URI_TYPE),
    PROMPT_ATTRIBUTE         = addAttribute("PROMPT"),
    READONLY_ATTRIBUTE       = addAttribute("READONLY", HTML.Attribute.BOOLEAN_TYPE),
    REL_ATTRIBUTE            = addAttribute("REL"),
    REV_ATTRIBUTE            = addAttribute("REV"),
    ROWS_ATTRIBUTE           = addAttribute("ROWS"),
    ROWSPAN_ATTRIBUTE        = addAttribute("ROWSPAN"),
    RULES_ATTRIBUTE          = addAttribute("RULES"),
    SCHEME_ATTRIBUTE         = addAttribute("SCHEME"),
    SCOPE_ATTRIBUTE          = addAttribute("SCOPE"),
    SCROLLING_ATTRIBUTE      = addAttribute("SCROLLING",
        HTML.Attribute.ENUM_TYPE,
        new String[] {"yes", "no", "auto"}),
    SELECTED_ATTRIBUTE       = addAttribute("SELECTED", HTML.Attribute.BOOLEAN_TYPE),
    SHAPE_ATTRIBUTE          = addAttribute("SHAPE"),
    SIZE_ATTRIBUTE           = addAttribute("SIZE"),
    SPAN_ATTRIBUTE           = addAttribute("SPAN"),
    SRC_ATTRIBUTE            = addAttribute("SRC", HTML.Attribute.URI_TYPE),
    STANDBY_ATTRIBUTE        = addAttribute("STANDBY"),
    START_ATTRIBUTE          = addAttribute("START"),
    STYLE_ATTRIBUTE          = addAttribute("STYLE"),
    SUMMARY_ATTRIBUTE        = addAttribute("SUMMARY"),
    TABINDEX_ATTRIBUTE       = addAttribute("TABINDEX"),
    TARGET_ATTRIBUTE         = addAttribute("TARGET"),
    TEXT_ATTRIBUTE           = addAttribute("TEXT"),
    TITLE_ATTRIBUTE          = addAttribute("TITLE"),
    TYPE_ATTRIBUTE           = addAttribute("TYPE"),
    USEMAP_ATTRIBUTE         = addAttribute("USEMAP", HTML.Attribute.URI_TYPE),
    VALIGN_ATTRIBUTE         = addAttribute("VALIGN",
        HTML.Attribute.ENUM_TYPE,
        new String[] {"top", "middle", "bottom", "baseline"}),
    VALUE_ATTRIBUTE          = addAttribute("VALUE"),
    VALUETYPE_ATTRIBUTE      = addAttribute("VALUETYPE",
        HTML.Attribute.ENUM_TYPE,
        new String[] {"data", "ref", "object"}),
    VERSION_ATTRIBUTE        = addAttribute("VERSION"),
    VLINK_ATTRIBUTE          = addAttribute("VLINK"),
    VSPACE_ATTRIBUTE         = addAttribute("VSPACE"),
    WIDTH_ATTRIBUTE          = addAttribute("WIDTH");
}