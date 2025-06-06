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
package consulo.xml.lang.base;

import consulo.language.editor.annotation.*;
import consulo.xml.psi.xml.XmlDocument;
import consulo.xml.psi.xml.XmlFile;
import consulo.xml.psi.xml.XmlTag;
import consulo.xml.psi.xml.XmlToken;
import com.intellij.xml.XmlNSDescriptor;
import com.intellij.xml.util.XmlTagUtil;
import consulo.language.editor.intention.IntentionAction;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.util.lang.Trinity;
import consulo.xml.Validator;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author ven
 */
public abstract class XMLBasedExternalAnnotator
  extends ExternalAnnotator<XMLBasedExternalAnnotator.MyHost, XMLBasedExternalAnnotator.MyHost> {
  @Nullable
  @Override
  public MyHost collectInformation(@Nonnull PsiFile file) {
    if (!(file instanceof XmlFile)) return null;
    final XmlDocument document = ((XmlFile)file).getDocument();
    if (document == null) return null;
    XmlTag rootTag = document.getRootTag();
    XmlNSDescriptor nsDescriptor = rootTag == null ? null : rootTag.getNSDescriptor(rootTag.getNamespace(), false);

    if (nsDescriptor instanceof Validator) {
      //noinspection unchecked
      MyHost host = new MyHost();
      ((Validator<XmlDocument>)nsDescriptor).validate(document, host);
      return host;
    }
    return null;
  }

  @Nullable
  @Override
  public MyHost doAnnotate(MyHost collectedInfo) {
    return collectedInfo;
  }

  @Override
  public void apply(@Nonnull PsiFile file, MyHost annotationResult, @Nonnull AnnotationHolder holder) {
    annotationResult.apply(holder);
  }

  private static void appendFixes(final AnnotationBuilder builder, final IntentionAction... actions) {
    if (actions != null) {
      for (IntentionAction action : actions) builder.newFix(action);
    }
  }

  static class MyHost implements Validator.ValidationHost {
    private final List<Trinity<PsiElement, String, ErrorType>> messages = new ArrayList<>();

    @Override
    public void addMessage(PsiElement context, String message, @Nonnull ErrorType type) {
      messages.add(Trinity.create(context, message, type));
    }

    void apply (AnnotationHolder holder) {
      for (Trinity<PsiElement, String, ErrorType> message : messages) {
        addMessageWithFixes(message.first, message.second, message.third, holder);
      }
    }
  }


  public static void addMessageWithFixes(
    final PsiElement context,
    final String message,
    @Nonnull final Validator.ValidationHost.ErrorType type,
    AnnotationHolder myHolder,
    @Nonnull final IntentionAction... fixes
  ) {
    if (message != null && !message.isEmpty()) {
      if (context instanceof XmlTag) {
        addMessagesForTag((XmlTag)context, message, type, myHolder, fixes);
      }
      else {
        HighlightSeverity severity = type == Validator.ValidationHost.ErrorType.ERROR ? HighlightSeverity.ERROR : HighlightSeverity.WARNING;
        appendFixes(myHolder.newAnnotation(severity, message).range(context), fixes);
      }
    }
  }

  private static void addMessagesForTag(
    XmlTag tag,
    String message,
    Validator.ValidationHost.ErrorType type,
    AnnotationHolder myHolder,
    IntentionAction... actions
  ) {
    XmlToken childByRole = XmlTagUtil.getStartTagNameElement(tag);

    addMessagesForTreeChild(childByRole, type, message, myHolder, actions);

    childByRole = XmlTagUtil.getEndTagNameElement(tag);
    addMessagesForTreeChild(childByRole, type, message, myHolder, actions);
  }

  private static void addMessagesForTreeChild(
    final XmlToken childByRole,
    final Validator.ValidationHost.ErrorType type,
    final String message,
    AnnotationHolder myHolder, IntentionAction... actions
  ) {
    if (childByRole != null) {
      HighlightSeverity severity = type == Validator.ValidationHost.ErrorType.ERROR ? HighlightSeverity.ERROR : HighlightSeverity.WARNING;
      appendFixes(myHolder.newAnnotation(severity, message).range(childByRole), actions);
    }
  }
}
