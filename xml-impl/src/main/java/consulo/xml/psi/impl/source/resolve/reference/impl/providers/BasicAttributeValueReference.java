/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.xml.psi.impl.source.resolve.reference.impl.providers;

import jakarta.annotation.Nonnull;
import consulo.document.util.TextRange;
import consulo.language.psi.ElementManipulators;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReference;
import consulo.language.util.IncorrectOperationException;

/**
 * @author peter
*/
public abstract class BasicAttributeValueReference implements PsiReference {
  protected PsiElement myElement;
  protected TextRange myRange;

  public BasicAttributeValueReference(final PsiElement element) {
    this ( element, ElementManipulators.getValueTextRange(element));
  }

  public BasicAttributeValueReference(final PsiElement element, int offset) {
    this ( element, new TextRange(offset, element.getTextLength() - offset));
  }

  public BasicAttributeValueReference(final PsiElement element, TextRange range) {
    myElement = element;
    myRange = range;
  }

  public PsiElement getElement() {
    return myElement;
  }

  public TextRange getRangeInElement() {
    return myRange;
  }

  @Nonnull
  public String getCanonicalText() {
    final String s = myElement.getText();
    if (myRange.getStartOffset() < s.length() && myRange.getEndOffset() <= s.length()) {
      return myRange.substring(s);
    }
    return "";
  }

  public PsiElement handleElementRename(String newElementName) throws consulo.language.util.IncorrectOperationException {
    return ElementManipulators.getManipulator(myElement).handleContentChange(
      myElement,
      getRangeInElement(),
      newElementName
    );
  }

  public PsiElement bindToElement(@Nonnull PsiElement element) throws IncorrectOperationException {
    return null;
  }

  public boolean isReferenceTo(PsiElement element) {
    return myElement.getManager().areElementsEquivalent(element, resolve());
  }
}
