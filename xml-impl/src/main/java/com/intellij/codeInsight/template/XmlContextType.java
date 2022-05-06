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
package com.intellij.codeInsight.template;

import javax.annotation.Nonnull;

import consulo.language.editor.CodeInsightBundle;
import consulo.language.Language;
import com.intellij.lang.xml.XMLLanguage;
import consulo.language.psi.PsiUtilCore;
import consulo.language.editor.template.context.TemplateContextType;
import consulo.language.psi.PsiFile;

/**
 * @author yole
 */
public class XmlContextType extends TemplateContextType {
  public XmlContextType() {
    super("XML", CodeInsightBundle.message("dialog.edit.template.checkbox.xml"));
  }

  @Override
  public boolean isInContext(@Nonnull PsiFile file, int offset) {
    return file.getLanguage().isKindOf(XMLLanguage.INSTANCE) && !isEmbeddedContent(file, offset) &&
           !HtmlContextType.isMyLanguage(PsiUtilCore.getLanguageAtOffset(file, offset));
  }

  public static boolean isEmbeddedContent(@Nonnull final PsiFile file, final int offset) {
    Language languageAtOffset = PsiUtilCore.getLanguageAtOffset(file, offset);
    return !(languageAtOffset.isKindOf(XMLLanguage.INSTANCE) || languageAtOffset instanceof XMLLanguage);
  }
}
