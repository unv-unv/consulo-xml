/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package consulo.xml.codeInsight.completion;

import com.intellij.xml.XmlElementDescriptor;
import com.intellij.xml.impl.schema.XmlElementDescriptorImpl;
import consulo.application.Application;
import consulo.language.editor.completion.CompletionParameters;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.language.psi.PsiElement;
import consulo.xml.psi.impl.source.xml.XmlContentDFA;
import consulo.xml.psi.xml.XmlTag;
import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * @author Dmitry Avdeev
 */
public class XmlSmartCompletionProvider {
    public void complete(CompletionParameters parameters, CompletionResultSet result, PsiElement element) {
        if (!XmlCompletionContributor.isXmlNameCompletion(parameters)) {
            return;
        }
        result.stopHere();
        if (!(element.getParent() instanceof XmlTag)) {
            return;
        }

        XmlTag tag = (XmlTag)element.getParent();
        XmlTag parentTag = tag.getParentTag();
        if (parentTag == null) {
            return;
        }
        XmlContentDFA dfa = XmlContentDFA.getContentDFA(parentTag);
        if (dfa == null) {
            return;
        }
        Application.get().runReadAction(() -> {
            for (XmlTag subTag : parentTag.getSubTags()) {
                if (subTag == tag) {
                    break;
                }
                dfa.transition(subTag);
            }
            List<XmlElementDescriptor> elements = dfa.getPossibleElements();
            for (XmlElementDescriptor elementDescriptor : elements) {
                addElementToResult(elementDescriptor, result);
            }
        });
    }

    private static void addElementToResult(@Nonnull XmlElementDescriptor descriptor, CompletionResultSet result) {
        XmlTagInsertHandler insertHandler = XmlTagInsertHandler.INSTANCE;
        if (descriptor instanceof XmlElementDescriptorImpl elementDescriptor) {
            String name = elementDescriptor.getName();
            if (name != null) {
                insertHandler = new ExtendedTagInsertHandler(name, elementDescriptor.getNamespace(), null);
            }
        }
        result.addElement(createLookupElement(descriptor).withInsertHandler(insertHandler));
    }

    public static LookupElementBuilder createLookupElement(@Nonnull XmlElementDescriptor descriptor) {
        LookupElementBuilder builder = LookupElementBuilder.create(descriptor.getName());
        if (descriptor instanceof XmlElementDescriptorImpl elementDescriptor) {
            builder = builder.withTypeText(elementDescriptor.getNamespace(), true);
        }
        return builder;
    }
}
