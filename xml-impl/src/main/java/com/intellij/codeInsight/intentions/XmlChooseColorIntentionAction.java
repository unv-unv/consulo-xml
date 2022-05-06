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
package com.intellij.codeInsight.intentions;

import java.awt.Color;

import javax.annotation.Nonnull;
import javax.swing.JComponent;

import consulo.codeEditor.Editor;
import consulo.ide.impl.idea.codeInsight.intention.PsiElementBaseIntentionAction;
import consulo.ide.impl.idea.ui.ColorChooser;
import consulo.language.editor.CodeInsightBundle;
import consulo.language.editor.FileModificationService;
import consulo.application.Result;
import consulo.language.editor.WriteCommandAction;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.project.Project;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiManager;
import com.intellij.psi.XmlElementFactory;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlAttributeValue;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.awt.util.ColorUtil;
import consulo.util.lang.Comparing;
import consulo.language.util.IncorrectOperationException;
import consulo.util.lang.StringUtil;

/**
 * @author Konstantin Bulenkov
 */
public class XmlChooseColorIntentionAction extends PsiElementBaseIntentionAction
{
	public XmlChooseColorIntentionAction()
	{
		setText(CodeInsightBundle.message("intention.color.chooser.dialog"));
	}

	public boolean isAvailable(@Nonnull final Project project, final Editor editor, @Nonnull final PsiElement element)
	{
		final PsiElement parent = element.getParent();
		return parent instanceof XmlAttributeValue && ColorUtil.fromHex(((XmlAttributeValue) parent).getValue(), null) != null;
	}

	@Nonnull
	public String getFamilyName()
	{
		return getText();
	}

	@Override
	public void invoke(@Nonnull Project project, Editor editor, @Nonnull PsiElement element) throws IncorrectOperationException
	{
		chooseColor(editor.getComponent(), element, getText());
	}

	public static void chooseColor(JComponent editorComponent, PsiElement element, String caption)
	{
		final XmlAttributeValue literal = PsiTreeUtil.getParentOfType(element, XmlAttributeValue.class, false);
		if(literal == null)
		{
			return;
		}
		final String text = StringUtil.unquoteString(literal.getValue());

		Color oldColor;
		try
		{
			oldColor = Color.decode(text);
		}
		catch(NumberFormatException e)
		{
			oldColor = JBColor.GRAY;
		}

		final Color temp = oldColor;
		ColorChooser.chooseColor(editorComponent, caption, temp, true, color ->
		{
			if(color == null)
			{
				return;
			}

			if(!Comparing.equal(color, temp))
			{
				if(!FileModificationService.getInstance().preparePsiElementForWrite(element))
				{
					return;
				}

				final String newText = "#" + ColorUtil.toHex(color);
				final PsiManager manager = literal.getManager();
				final XmlAttribute newAttribute = XmlElementFactory.getInstance(manager.getProject()).createXmlAttribute("name", newText);

				new WriteCommandAction(element.getProject(), caption)
				{
					@Override
					protected void run(Result result) throws Throwable
					{
						final XmlAttributeValue valueElement = newAttribute.getValueElement();
						assert valueElement != null;
						literal.replace(valueElement);
					}
				}.execute();
			}
		});
	}
}

