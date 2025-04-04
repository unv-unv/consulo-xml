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

package consulo.xml.util.xml.actions.generate;

import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiWhiteSpace;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.xml.psi.xml.XmlElement;
import consulo.xml.psi.xml.XmlTag;
import consulo.xml.util.xml.DomElement;
import consulo.xml.util.xml.DomUtil;
import consulo.xml.util.xml.reflect.DomCollectionChildDescription;
import consulo.xml.util.xml.ui.actions.generate.GenerateDomElementProvider;
import consulo.document.Document;
import consulo.util.lang.reflect.ReflectionUtil;

import java.util.List;

import jakarta.annotation.Nullable;

/**
 * User: Sergey.Vasiliev
 */
public abstract class DefaultGenerateElementProvider<T extends DomElement> extends GenerateDomElementProvider<T> {
  private final Class<T> myChildElementClass;

  public DefaultGenerateElementProvider(final String name, Class<T> childElementClass) {
    super(name);

    myChildElementClass = childElementClass;
  }


  @Nullable
  public T generate(final Project project, final Editor editor, final PsiFile file) {
    return generate(getParentDomElement(project, editor, file), editor);
  }

  @Nullable
  protected abstract DomElement getParentDomElement(final Project project, final Editor editor, final PsiFile file);

  @SuppressWarnings({"unchecked"})
  @Nullable
  public T generate(@Nullable final DomElement parent, final Editor editor) {
    if (parent == null) {
      return null;
    }

    final List<? extends DomCollectionChildDescription> list = parent.getGenericInfo().getCollectionChildrenDescriptions();

    for (DomCollectionChildDescription childDescription : list) {
      if (ReflectionUtil.getRawType(childDescription.getType()).isAssignableFrom(myChildElementClass)) {

        XmlTag parentTag = parent.getXmlTag();
        if (editor != null && parentTag != null) {
          int offset = editor.getCaretModel().getOffset();
          PsiFile file = parentTag.getContainingFile();
          PsiElement psiElement = file.findElementAt(offset);

          if (psiElement instanceof PsiWhiteSpace && PsiTreeUtil.isAncestor(parentTag, psiElement, false)) {
              Document document = editor.getDocument();
            XmlTag childTag = parentTag.createChildTag(childDescription.getXmlElementName(), null, null, true);
            String text = childTag.getText();
            document.insertString(offset, text);
            Project project = editor.getProject();
            assert project != null;
            PsiDocumentManager documentManager = PsiDocumentManager.getInstance(project);
            documentManager.commitDocument(document);
            PsiElement element = file.findElementAt(offset + 1);
            T domElement = DomUtil.findDomElement(element, myChildElementClass);
            if (domElement != null) return domElement;
            document.deleteString(offset, offset + text.length());
            documentManager.commitDocument(document);
          }
        }

        int index = getCollectionIndex(parent, childDescription, editor);

        return index < 0
               ? (T)childDescription.addValue(parent, myChildElementClass)
               : (T)childDescription.addValue(parent, myChildElementClass, index);
      }
    }
    return null;
  }

  private static int getCollectionIndex(final DomElement parent,
                                        final DomCollectionChildDescription childDescription,
                                        final Editor editor) {
    int offset = editor.getCaretModel().getOffset();

    for (int i = 0; i < childDescription.getValues(parent).size(); i++) {
      DomElement element = childDescription.getValues(parent).get(i);
      XmlElement xmlElement = element.getXmlElement();
      if (xmlElement != null && xmlElement.getTextRange().getStartOffset() >= offset) {
        return i;
      }
    }

    return -1;
  }
}
