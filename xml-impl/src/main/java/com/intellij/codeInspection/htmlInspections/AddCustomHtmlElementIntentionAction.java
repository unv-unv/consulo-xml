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

import consulo.ide.impl.idea.util.Consumer;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.project.Project;
import consulo.language.editor.inspection.scheme.InspectionProjectProfileManager;
import consulo.language.psi.PsiElement;
import com.intellij.xml.XmlBundle;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.editor.inspection.scheme.InspectionProfile;
import consulo.util.dataholder.Key;

import javax.annotation.Nonnull;

public class AddCustomHtmlElementIntentionAction implements LocalQuickFix
{
	private final String myName;
	private final String myText;
	@Nonnull
	private final Key<HtmlUnknownElementInspection> myInspectionKey;

	public AddCustomHtmlElementIntentionAction(@Nonnull Key<HtmlUnknownElementInspection> inspectionKey, String name, String text)
	{
		myInspectionKey = inspectionKey;
		myName = name;
		myText = text;
	}

	@Override
	@Nonnull
	public String getName()
	{
		return myText;
	}

	@Override
	@Nonnull
	public String getFamilyName()
	{
		return XmlBundle.message("fix.html.family");
	}

	@Override
	public void applyFix(@Nonnull final Project project, @Nonnull final ProblemDescriptor descriptor)
	{
		final PsiElement element = descriptor.getPsiElement();

		InspectionProfile profile = InspectionProjectProfileManager.getInstance(project).getInspectionProfile();
		profile.modifyToolSettings(myInspectionKey, element, new Consumer<HtmlUnknownElementInspection>()
		{
			@Override
			public void consume(HtmlUnknownElementInspection tool)
			{
				tool.addEntry(myName);
			}
		});
	}
}
