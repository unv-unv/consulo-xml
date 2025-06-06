/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.xml.usageView;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.psi.ElementDescriptionLocation;
import consulo.language.psi.ElementDescriptionProvider;
import consulo.language.psi.PsiElement;
import consulo.usage.UsageViewLongNameLocation;
import consulo.usage.UsageViewShortNameLocation;
import consulo.xml.psi.xml.XmlAttributeValue;
import consulo.xml.psi.xml.XmlTag;

import jakarta.annotation.Nonnull;

/**
 * @author yole
 */
@ExtensionImpl
public class XmlUsageViewDescriptionProvider implements ElementDescriptionProvider {
  public String getElementDescription(@Nonnull final PsiElement element, @Nonnull final ElementDescriptionLocation location) {
    if (location instanceof UsageViewShortNameLocation) {
      if (element instanceof XmlAttributeValue) {
        return ((XmlAttributeValue)element).getValue();
      }
    }

    if (location instanceof UsageViewLongNameLocation) {
      if (element instanceof XmlTag) {
        return ((XmlTag)element).getName();
      }
      else if (element instanceof XmlAttributeValue) {
        return ((XmlAttributeValue)element).getValue();
      }
    }

    return null;
  }
}
