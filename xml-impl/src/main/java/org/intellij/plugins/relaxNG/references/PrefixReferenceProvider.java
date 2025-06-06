/*
 * Copyright 2007 Sascha Weinreuter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.intellij.plugins.relaxNG.references;

import consulo.document.util.TextRange;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.LocalQuickFixProvider;
import consulo.language.psi.EmptyResolveMessageProvider;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReference;
import consulo.language.psi.PsiReferenceProvider;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.language.util.ProcessingContext;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.util.collection.ArrayUtil;
import consulo.xml.codeInspection.XmlQuickFixFactory;
import consulo.xml.lang.xml.XMLLanguage;
import consulo.xml.psi.XmlElementFactory;
import consulo.xml.psi.impl.source.resolve.reference.impl.providers.BasicAttributeValueReference;
import consulo.xml.psi.impl.source.xml.SchemaPrefix;
import consulo.xml.psi.xml.XmlAttribute;
import consulo.xml.psi.xml.XmlAttributeValue;
import consulo.xml.psi.xml.XmlTag;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/*
 * Created by IntelliJ IDEA.
 * User: sweinreuter
 * Date: 24.07.2007
 */
public class PrefixReferenceProvider extends PsiReferenceProvider {
  private static final Logger LOG = Logger.getInstance("#PrefixReferenceProvider");

  @Override
  @Nonnull
  public PsiReference[] getReferencesByElement(@Nonnull PsiElement element, @Nonnull ProcessingContext context) {
    final XmlAttributeValue value = (XmlAttributeValue)element;

    final String s = value.getValue();
    final int i = s.indexOf(':');
    if (i <= 0 || s.startsWith("xml:")) {
      return PsiReference.EMPTY_ARRAY;
    }

    return new PsiReference[]{
      new PrefixReference(value, i)
    };
  }

  private static class PrefixReference extends BasicAttributeValueReference implements EmptyResolveMessageProvider, LocalQuickFixProvider {
    public PrefixReference(XmlAttributeValue value, int length) {
      super(value, TextRange.from(1, length));
    }

    @Override
    @Nullable
    public PsiElement resolve() {
      final String prefix = getCanonicalText();
      XmlTag tag = PsiTreeUtil.getParentOfType(getElement(), XmlTag.class);
      while (tag != null) {
        if (tag.getLocalNamespaceDeclarations().containsKey(prefix)) {
          final XmlAttribute attribute = tag.getAttribute("xmlns:" + prefix, "");
          final TextRange textRange = TextRange.from("xmlns:".length(), prefix.length());
          return new SchemaPrefix(attribute, textRange, prefix);
        }
        tag = tag.getParentTag();
      }
      return null;
    }

    @Override
    public boolean isReferenceTo(PsiElement element) {
      if (element instanceof SchemaPrefix && element.getContainingFile() == myElement.getContainingFile()) {
        final PsiElement e = resolve();
        if (e instanceof SchemaPrefix) {
          final String s = ((SchemaPrefix)e).getName();
          return s != null && s.equals(((SchemaPrefix)element).getName());
        }
      }
      return super.isReferenceTo(element);
    }

    @Nullable
    @Override
    public LocalQuickFix[] getQuickFixes() {
      final PsiElement element = getElement();
      final XmlElementFactory factory = XmlElementFactory.getInstance(element.getProject());
      final String value = ((XmlAttributeValue)element).getValue();
      final String[] name = value.split(":");
      final XmlTag tag = factory.createTagFromText("<" + (name.length > 1 ? name[1] : value) + " />", XMLLanguage.INSTANCE);

      return new LocalQuickFix[]{XmlQuickFixFactory.getInstance().createNSDeclarationIntentionFix(tag, getCanonicalText(), null)};
    }

    @Nonnull
    @Override
    public LocalizeValue buildUnresolvedMessage(@Nonnull String s) {
      return LocalizeValue.localizeTODO( "Undefined namespace prefix '" + s + "'");
    }
  }
}