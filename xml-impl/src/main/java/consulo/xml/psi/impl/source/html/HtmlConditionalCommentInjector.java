/*
 * Copyright 2000-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.xml.psi.impl.source.html;

import consulo.annotation.component.ExtensionImpl;
import consulo.document.util.TextRange;
import consulo.language.Language;
import consulo.language.ast.ASTNode;
import consulo.language.ast.TokenSet;
import consulo.language.inject.MultiHostInjector;
import consulo.language.inject.MultiHostRegistrar;
import consulo.language.psi.PsiComment;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiLanguageInjectionHost;
import consulo.util.lang.Pair;
import consulo.xml.psi.xml.XmlComment;
import consulo.xml.psi.xml.XmlTokenType;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author spleaner
 */
@ExtensionImpl
public class HtmlConditionalCommentInjector implements MultiHostInjector {

  /**
   * Allows to check if given element is a
   * <a href="http://msdn.microsoft.com/en-us/library/ms537512(v=vs.85).aspx">conditional comment</a>.
   * 
   * @param host  target element to check
   * @return      <code>true</code> if given element is conditional comment; <code>false</code> otherwise
   */
  public static boolean isConditionalComment(@Nonnull PsiElement host) {
    return parseConditionalCommentBoundaries(host) != null;
  }

  /**
   * Tries to parse given element as <a href="http://msdn.microsoft.com/en-us/library/ms537512(v=vs.85).aspx">conditional comment</a>.
   * 
   * @param host  target element to parse
   * @return      <code>null</code> if given element is not a conditional comment;
   *              pair like <code>(conditional comment start element; conditional comment end element)</code> otherwise
   */
  @Nullable
  private static Pair<ASTNode, ASTNode> parseConditionalCommentBoundaries(@Nonnull PsiElement host) {
    if (!(host instanceof XmlComment)) {
      return null;
    }
    final ASTNode comment = host.getNode();
    if (comment == null) {
      return null;
    }
    final ASTNode conditionalStart = comment.findChildByType(TokenSet.create(XmlTokenType.XML_CONDITIONAL_COMMENT_START_END));
    if (conditionalStart == null) {
      return null;
    }
    final ASTNode conditionalEnd = comment.findChildByType(TokenSet.create(XmlTokenType.XML_CONDITIONAL_COMMENT_END_START));
    if (conditionalEnd == null) {
      return null;
    }
    final ASTNode endOfEnd = comment.findChildByType(TokenSet.create(XmlTokenType.XML_CONDITIONAL_COMMENT_END));
    return endOfEnd == null ? null : new Pair<ASTNode, ASTNode>(conditionalStart, conditionalEnd);
  }

  @Nonnull
  @Override
  public Class<? extends PsiElement> getElementClass() {
    return PsiComment.class;
  }

  @Override
  public void injectLanguages(@Nonnull final MultiHostRegistrar registrar, @Nonnull final PsiElement host) {
    Pair<ASTNode, ASTNode> pair = parseConditionalCommentBoundaries(host);
    if (pair == null) {
      return;
    }
    final TextRange textRange = host.getTextRange();
    final int startOffset = textRange.getStartOffset();
    Language language = host.getParent().getLanguage();
    ASTNode conditionalStart = pair.first;
    ASTNode conditionalEnd = pair.second;
    TextRange range = new TextRange(conditionalStart.getTextRange().getEndOffset() - startOffset, conditionalEnd.getStartOffset() - startOffset);
    if (range.getStartOffset() < range.getEndOffset()) {
      registrar.startInjecting(language).addPlace(null, null, (PsiLanguageInjectionHost)host, range).doneInjecting();
    }
  }
}
