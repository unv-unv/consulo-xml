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
package com.intellij.vcsUtil;

import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlText;
import consulo.codeEditor.Editor;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.document.util.TextRange;
import consulo.ide.impl.idea.openapi.vcs.VcsBundle;
import consulo.ide.impl.idea.openapi.vcs.actions.VcsContext;
import consulo.ide.impl.idea.vcsUtil.VcsSelection;
import consulo.ide.impl.idea.vcsUtil.VcsSelectionProvider;
import consulo.language.editor.TargetElementUtil;
import consulo.language.editor.TargetElementUtilExtender;
import consulo.language.psi.PsiElement;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.virtualFileSystem.VirtualFile;

import java.util.Set;

/**
 * @author yole
 */
public class XmlVcsSelectionProvider implements VcsSelectionProvider
{
	@RequiredUIAccess
	public VcsSelection getSelection(VcsContext context)
	{
		final Editor editor = context.getEditor();
		if(editor == null)
		{
			return null;
		}
		PsiElement psiElement = TargetElementUtil.findTargetElement(editor, Set.of(TargetElementUtilExtender.ELEMENT_NAME_ACCEPTED));
		if(psiElement == null || !psiElement.isValid())
		{
			return null;
		}

		final String actionName;

		if(psiElement instanceof XmlTag)
		{
			actionName = VcsBundle.message("action.name.show.history.for.tag");
		}
		else if(psiElement instanceof XmlText)
		{
			actionName = VcsBundle.message("action.name.show.history.for.text");
		}
		else
		{
			return null;
		}

		TextRange textRange = psiElement.getTextRange();
		if(textRange == null)
		{
			return null;
		}

		VirtualFile virtualFile = psiElement.getContainingFile().getVirtualFile();
		if(virtualFile == null)
		{
			return null;
		}
		if(!virtualFile.isValid())
		{
			return null;
		}

		Document document = FileDocumentManager.getInstance().getDocument(virtualFile);
		return new VcsSelection(document, textRange, actionName);
	}
}
