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

package consulo.xml.codeInspection;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;

import jakarta.annotation.Nonnull;

/**
 * @author Dmitry Avdeev
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class XmlSuppressionProvider
{
	public static ExtensionPointName<XmlSuppressionProvider> EP_NAME = ExtensionPointName.create(XmlSuppressionProvider.class);

	public static boolean isSuppressed(@Nonnull PsiElement element, @Nonnull String inspectionId)
	{
		for(XmlSuppressionProvider provider : EP_NAME.getExtensionList())
		{
			if(provider.isSuppressedFor(element, inspectionId))
			{
				return true;
			}
		}
		return false;
	}

	public static XmlSuppressionProvider getProvider(@Nonnull PsiFile file)
	{
		for(XmlSuppressionProvider provider : EP_NAME.getExtensionList())
		{
			if(provider.isProviderAvailable(file))
			{
				return provider;
			}
		}
		throw new RuntimeException("No providers found for " + file);
	}

	public abstract boolean isProviderAvailable(@Nonnull PsiFile file);

	public abstract boolean isSuppressedFor(@Nonnull PsiElement element, @Nonnull String inspectionId);

	public abstract void suppressForFile(@Nonnull PsiElement element, @Nonnull String inspectionId);

	public abstract void suppressForTag(@Nonnull PsiElement element, @Nonnull String inspectionId);

}
