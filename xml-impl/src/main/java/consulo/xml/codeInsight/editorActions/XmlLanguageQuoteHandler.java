package consulo.xml.codeInsight.editorActions;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.action.LanguageQuoteHandler;
import consulo.xml.lang.xml.XMLLanguage;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 05-Jul-22
 */
@ExtensionImpl
public class XmlLanguageQuoteHandler extends XmlBasedQuoteHandler implements LanguageQuoteHandler {
  @Nonnull
  @Override
  public Language getLanguage() {
    return XMLLanguage.INSTANCE;
  }
}
