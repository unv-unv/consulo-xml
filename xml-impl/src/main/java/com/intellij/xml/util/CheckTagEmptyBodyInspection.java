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

import com.intellij.codeInspection.*;
import com.intellij.lang.xml.XMLLanguage;
import consulo.language.editor.WriteCommandAction;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import com.intellij.psi.XmlElementVisitor;
import com.intellij.psi.xml.XmlChildRole;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlTokenType;
import com.intellij.xml.XmlBundle;
import consulo.application.Result;
import consulo.language.ast.ASTNode;
import consulo.language.editor.FileModificationService;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementVisitor;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.annotations.NonNls;

/**
 * @author Maxim Mossienko
 */
public class CheckTagEmptyBodyInspection extends XmlSuppressableInspectionTool {
  public boolean isEnabledByDefault() {
    return true;
  }

  @Nonnull
  public PsiElementVisitor buildVisitor(@Nonnull final ProblemsHolder holder, boolean isOnTheFly) {
    return new XmlElementVisitor() {
      @Override public void visitXmlTag(final XmlTag tag) {
        if (!CheckEmptyTagInspection.isTagWithEmptyEndNotAllowed(tag)) {
          final ASTNode child = XmlChildRole.START_TAG_END_FINDER.findChild(tag.getNode());

          if (child != null) {
            final ASTNode node = child.getTreeNext();

            if (node != null &&
                node.getElementType() == XmlTokenType.XML_END_TAG_START) {
              final LocalQuickFix localQuickFix = new ReplaceEmptyTagBodyByEmptyEndFix();
              holder.registerProblem(
                tag,
                XmlBundle.message("xml.inspections.tag.empty.body"),
                isCollapsableTag(tag) ? localQuickFix : null
              );
            }
          }
        }
      }
    };
  }

  @SuppressWarnings({"HardCodedStringLiteral"})
  private static boolean isCollapsableTag(final XmlTag tag) {
    final String name = tag.getName().toLowerCase();
    return tag.getLanguage() == XMLLanguage.INSTANCE ||
           "link".equals(name) || "br".equals(name) || "meta".equals(name) || "img".equals(name) || "input".equals(name) || "hr".equals(name);
  }

  @Nonnull
  public String getGroupDisplayName() {
    return XmlInspectionGroupNames.XML_INSPECTIONS;
  }

  @Nonnull
  public String getDisplayName() {
    return XmlBundle.message("xml.inspections.check.tag.empty.body");
  }

  @Nonnull
  @NonNls
  public String getShortName() {
    return "CheckTagEmptyBody";
  }

  private static class ReplaceEmptyTagBodyByEmptyEndFix implements LocalQuickFix {
    @Nonnull
    public String getName() {
      return XmlBundle.message("xml.inspections.replace.tag.empty.body.with.empty.end");
    }

    @Nonnull
    public String getFamilyName() {
      return getName();
    }

    public void applyFix(@Nonnull final Project project, @Nonnull final ProblemDescriptor descriptor) {
      final PsiElement tag = descriptor.getPsiElement();
      if (!FileModificationService.getInstance().prepareFileForWrite(tag.getContainingFile())) {
        return;
      }

      PsiDocumentManager.getInstance(project).commitAllDocuments();

      final ASTNode child = XmlChildRole.START_TAG_END_FINDER.findChild(tag.getNode());
      if (child == null) return;
      final int offset = child.getTextRange().getStartOffset();
      VirtualFile file = tag.getContainingFile().getVirtualFile();
      final Document document = FileDocumentManager.getInstance().getDocument(file);

      new WriteCommandAction(project) {
        protected void run(final Result result) throws Throwable {
          document.replaceString(offset, tag.getTextRange().getEndOffset(),"/>");
        }
      }.execute();
    }
  }
}