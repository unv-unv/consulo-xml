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
package com.intellij.xml.util;

import javax.annotation.Nonnull;

import com.intellij.codeInsight.daemon.XmlErrorMessages;
import com.intellij.codeInspection.XmlSuppressableInspectionTool;
import com.intellij.psi.*;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import consulo.language.editor.inspection.ProblemHighlightType;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.psi.ElementManipulators;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementVisitor;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiUtilCore;

/**
 * @author Dmitry Avdeev
 */
public class XmlDuplicatedIdInspection extends XmlSuppressableInspectionTool {

  @Nonnull
  @Override
  public PsiElementVisitor buildVisitor(@Nonnull final ProblemsHolder holder, final boolean isOnTheFly) {
    return new XmlElementVisitor() {
      @Override
      public void visitXmlAttributeValue(final XmlAttributeValue value) {
        if (value.getTextRange().isEmpty()) {
          return;
        }
        final PsiFile file = value.getContainingFile();
        if (!(file instanceof XmlFile)) {
          return;
        }
        PsiFile baseFile = PsiUtilCore.getTemplateLanguageFile(file);
        if (baseFile != file && !(baseFile instanceof XmlFile)) {
          return;
        }
        final XmlRefCountHolder refHolder = XmlRefCountHolder.getRefCountHolder(value);
        if (refHolder == null) return;

        final PsiElement parent = value.getParent();
        if (!(parent instanceof XmlAttribute)) return;

        final XmlTag tag = (XmlTag)parent.getParent();
        if (tag == null) return;

        checkValue(value, (XmlFile)file, refHolder, tag, holder);
      }
    };
  }

  protected void checkValue(XmlAttributeValue value, XmlFile file, XmlRefCountHolder refHolder, XmlTag tag, ProblemsHolder holder) {
    if (refHolder.isValidatable(tag.getParent()) && refHolder.isDuplicateIdAttributeValue(value)) {
      holder.registerProblem(value, XmlErrorMessages.message("duplicate.id.reference"), ProblemHighlightType.GENERIC_ERROR,
                             ElementManipulators.getValueTextRange(value));
    }
  }
}
