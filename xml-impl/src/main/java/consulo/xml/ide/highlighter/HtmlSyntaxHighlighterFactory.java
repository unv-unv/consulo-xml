package consulo.xml.ide.highlighter;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.language.Language;
import consulo.language.editor.highlight.SingleLazyInstanceSyntaxHighlighterFactory;
import consulo.language.editor.highlight.SyntaxHighlighter;
import consulo.xml.lang.html.HTMLLanguage;
import jakarta.inject.Inject;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 14.02.2015
 */
@ExtensionImpl
public class HtmlSyntaxHighlighterFactory extends SingleLazyInstanceSyntaxHighlighterFactory {
  private final Application myApplication;

  @Inject
  public HtmlSyntaxHighlighterFactory(Application application) {
    myApplication = application;
  }

  @Nonnull
  @Override
  protected SyntaxHighlighter createHighlighter() {
    return new HtmlFileHighlighter(myApplication);
  }

  @Nonnull
  @Override
  public Language getLanguage() {
    return HTMLLanguage.INSTANCE;
  }
}
