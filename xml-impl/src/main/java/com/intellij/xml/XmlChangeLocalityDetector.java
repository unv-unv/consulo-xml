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
package com.intellij.xml;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.ChangeLocalityDetector;
import consulo.language.psi.PsiComment;
import consulo.language.psi.PsiElement;
import consulo.xml.codeInspection.DefaultXmlSuppressionProvider;
import consulo.xml.lang.xml.XMLLanguage;

import jakarta.annotation.Nonnull;

@ExtensionImpl
public class XmlChangeLocalityDetector implements ChangeLocalityDetector {
    @Override
    public PsiElement getChangeHighlightingDirtyScopeFor(@Nonnull PsiElement changedElement) {
        // rehighlight everything when inspection suppress comment changed
        if (changedElement.getLanguage() instanceof XMLLanguage
            && changedElement instanceof PsiComment
            && changedElement.getText().contains(DefaultXmlSuppressionProvider.SUPPRESS_MARK)) {
            return changedElement.getContainingFile();
        }
        return null;
    }
}
