package org.eclipse.wb.internal.core.model;

import org.eclipse.osgi.util.NLS;

public class ModelMessages extends NLS {
  private static final String BUNDLE_NAME = "org.eclipse.wb.internal.core.model.ModelMessages"; //$NON-NLS-1$
  public static String CharacterPropertyEditor_notValid;
  public static String DoubleObjectPropertyEditor_notValidDouble;
  public static String DoublePropertyEditor_notValidDouble;
  public static String FloatPropertyEditor_notValidFloat;
  public static String IntegerObjectPropertyEditor_notValidInt;
  public static String IntegerPropertyEditor_notValidInt;
  public static String LongObjectPropertyEditor_notValidLong;
  public static String LongPropertyEditor_notValidLong;
  public static String ShortObjectPropertyEditor_notValidShort;
  public static String ShortPropertyEditor_notValidShort;
  public static String StringArrayPropertyEditor_hint;
  public static String StringArrayPropertyEditor_itemsLabel;
  public static String StringPropertyDialog_title;
  static {
    // initialize resource bundle
    NLS.initializeMessages(BUNDLE_NAME, ModelMessages.class);
  }

  private ModelMessages() {
  }
}
