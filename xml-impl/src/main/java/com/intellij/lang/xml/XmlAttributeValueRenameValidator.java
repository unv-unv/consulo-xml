/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package com.intellij.lang.xml;

import consulo.language.editor.refactoring.rename.RenameInputValidator;
import consulo.language.pattern.ElementPattern;
import consulo.language.psi.PsiElement;
import com.intellij.psi.xml.XmlAttributeValue;
import consulo.language.util.ProcessingContext;

import static consulo.language.pattern.PlatformPatterns.psiElement;

public class XmlAttributeValueRenameValidator implements RenameInputValidator {
  @Override
  public ElementPattern<? extends PsiElement> getPattern() {
    return psiElement(XmlAttributeValue.class);
  }

  public boolean isInputValid(final String newName, final PsiElement element, final ProcessingContext context) {
    return true;
  }
}
