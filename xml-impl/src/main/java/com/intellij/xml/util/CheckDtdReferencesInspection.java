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

import java.util.Map;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import com.intellij.codeHighlighting.HighlightDisplayLevel;
import com.intellij.codeInsight.daemon.impl.analysis.XmlHighlightVisitor;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.codeInspection.XmlInspectionGroupNames;
import com.intellij.codeInspection.XmlSuppressableInspectionTool;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.XmlElementVisitor;
import com.intellij.psi.xml.XmlDoctype;
import com.intellij.psi.xml.XmlElement;
import com.intellij.psi.xml.XmlElementContentSpec;
import com.intellij.psi.xml.XmlEntityRef;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.containers.HashMap;
import com.intellij.xml.XmlBundle;

/**
 * @author Maxim Mossienko
 */
public class CheckDtdReferencesInspection extends XmlSuppressableInspectionTool {
  public boolean isEnabledByDefault() {
    return true;
  }

  @NotNull
  public PsiElementVisitor buildVisitor(@NotNull final ProblemsHolder holder, boolean isOnTheFly) {
    return new XmlElementVisitor() {

      private Map<PsiFile, Boolean> myDoctypeMap = new HashMap<PsiFile, Boolean>();

      @Override
      public void visitXmlElement(final XmlElement element) {
        if (isHtml5Doctype(element)) {
          return;
        }

        if (element instanceof XmlElementContentSpec ||
            element instanceof XmlEntityRef
          ) {
          doCheckRefs(element, holder);
        }
      }

      private boolean isHtml5Doctype(XmlElement element) {
        if (HtmlUtil.isHtml5Context(element)) {
          return true;
        }

        PsiFile file = element.getContainingFile();
        if (file instanceof XmlFile) {
          if (!myDoctypeMap.containsKey(file)) {
            myDoctypeMap.put(file, computeHtml5Doctype((XmlFile)file));
          }
          return myDoctypeMap.get(file);
        }
        return false;
      }

      private Boolean computeHtml5Doctype(XmlFile file) {
        XmlDoctype doctype = null;
        //Search for doctypes from providers
        for (HtmlDoctypeProvider provider : HtmlDoctypeProvider.EP_NAME.getExtensions()) {
          doctype = provider.getDoctype(file);
          if (doctype != null) {
            break;
          }
        }

        if (doctype != null && HtmlUtil.isHtml5Doctype(doctype)) {
          return true;
        }

        return false;
      }
    };
  }

  private static void doCheckRefs(final XmlElement element, final ProblemsHolder holder) {
    for (PsiReference ref : element.getReferences()) {
      ProgressManager.checkCanceled();
      if (XmlHighlightVisitor.hasBadResolve(ref, true)) {
        if (ref.getElement() instanceof XmlElementContentSpec) {
          final String image = ref.getCanonicalText();
          if (image.equals("-") || image.equals("O")) continue;
        }
        holder.registerProblem(ref);
      }
    }
  }

  @NotNull
  public HighlightDisplayLevel getDefaultLevel() {
    return HighlightDisplayLevel.ERROR;
  }

  @NotNull
  public String getGroupDisplayName() {
    return XmlInspectionGroupNames.XML_INSPECTIONS;
  }

  @NotNull
  public String getDisplayName() {
    return XmlBundle.message("xml.inspections.check.dtd.references");
  }

  @NotNull
  @NonNls
  public String getShortName() {
    return "CheckDtdRefs";
  }

}