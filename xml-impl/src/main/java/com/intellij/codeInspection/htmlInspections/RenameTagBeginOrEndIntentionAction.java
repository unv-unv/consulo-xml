/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

package com.intellij.codeInspection.htmlInspections;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.intellij.codeInsight.daemon.XmlErrorMessages;
import consulo.document.Document;
import consulo.codeEditor.Editor;
import consulo.language.psi.PsiDocumentManager;
import consulo.project.Project;
import consulo.document.util.TextRange;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiErrorElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiWhiteSpace;
import consulo.language.ast.IElementType;
import consulo.language.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlToken;
import com.intellij.psi.xml.XmlTokenType;
import com.intellij.xml.util.XmlTagUtil;
import consulo.language.editor.FileModificationService;
import consulo.language.editor.intention.IntentionAction;
import consulo.language.util.IncorrectOperationException;

/**
 * @author spleaner
 */
public class RenameTagBeginOrEndIntentionAction implements IntentionAction
{
	private final boolean myStart;
	private final String myTargetName;
	private final String mySourceName;

	RenameTagBeginOrEndIntentionAction(@Nonnull final String targetName, @Nonnull final String sourceName, final boolean start)
	{
		myTargetName = targetName;
		mySourceName = sourceName;
		myStart = start;
	}

	@Override
	@Nonnull
	public String getFamilyName()
	{
		return getName();
	}

	@Override
	@Nonnull
	public String getText()
	{
		return getName();
	}

	@Override
	public boolean isAvailable(@Nonnull final Project project, final Editor editor, final PsiFile file)
	{
		return true;
	}

	@Override
	public void invoke(@Nonnull final Project project, final Editor editor, final PsiFile file) throws IncorrectOperationException
	{
		final int offset = editor.getCaretModel().getOffset();
		PsiElement psiElement = file.findElementAt(offset);

		if(psiElement == null || !psiElement.isValid())
		{
			return;
		}
		if(!FileModificationService.getInstance().prepareFileForWrite(psiElement.getContainingFile()))
		{
			return;
		}

		if(psiElement instanceof PsiWhiteSpace)
		{
			psiElement = PsiTreeUtil.prevLeaf(psiElement);
		}
		if(psiElement instanceof XmlToken)
		{
			final IElementType tokenType = ((XmlToken) psiElement).getTokenType();
			if(tokenType != XmlTokenType.XML_NAME)
			{
				if(tokenType == XmlTokenType.XML_TAG_END)
				{
					psiElement = psiElement.getPrevSibling();
					if(psiElement == null)
					{
						return;
					}
				}
			}

			PsiElement target = null;
			final String text = psiElement.getText();
			if(!myTargetName.equals(text))
			{
				target = psiElement;
			}
			else
			{
				// we're in the other
				target = findOtherSide(psiElement, myStart);
			}

			if(target != null)
			{
				final Document document = PsiDocumentManager.getInstance(project).getDocument(file);
				if(document != null)
				{
					final TextRange textRange = target.getTextRange();
					document.replaceString(textRange.getStartOffset(), textRange.getEndOffset(), myTargetName);
				}
			}

		}
	}

	@Nullable
	public static PsiElement findOtherSide(PsiElement psiElement, final boolean start)
	{
		PsiElement target = null;
		PsiElement parent = psiElement.getParent();
		if(parent instanceof PsiErrorElement)
		{
			parent = parent.getParent();
		}

		if(parent instanceof XmlTag)
		{
			if(start)
			{
				target = XmlTagUtil.getStartTagNameElement((XmlTag) parent);
			}
			else
			{
				target = XmlTagUtil.getEndTagNameElement((XmlTag) parent);
				if(target == null)
				{
					final PsiErrorElement errorElement = PsiTreeUtil.getChildOfType(parent, PsiErrorElement.class);
					target = XmlWrongClosingTagNameInspection.findEndTagName(errorElement);
				}
			}
		}
		return target;
	}

	@Override
	public boolean startInWriteAction()
	{
		return true;
	}

	@Nonnull
	public String getName()
	{
		return myStart ? XmlErrorMessages.message("rename.start.tag.name.intention", mySourceName, myTargetName) : XmlErrorMessages.message("rename.end.tag.name.intention", mySourceName, myTargetName);
	}
}
