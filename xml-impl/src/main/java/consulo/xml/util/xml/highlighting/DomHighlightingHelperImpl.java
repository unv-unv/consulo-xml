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
package consulo.xml.util.xml.highlighting;

import com.intellij.xml.XmlBundle;
import consulo.codeEditor.Editor;
import consulo.ide.IdeBundle;
import consulo.language.editor.FileModificationService;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.editor.intention.SyntheticIntentionAction;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiReference;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.language.util.IncorrectOperationException;
import consulo.language.util.ProcessingContext;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.SmartList;
import consulo.util.lang.StringUtil;
import consulo.xml.codeInsight.daemon.impl.analysis.XmlHighlightVisitor;
import consulo.xml.psi.xml.XmlAttributeValue;
import consulo.xml.psi.xml.XmlElement;
import consulo.xml.psi.xml.XmlTag;
import consulo.xml.util.xml.*;
import consulo.xml.util.xml.impl.*;
import consulo.xml.util.xml.reflect.AbstractDomChildrenDescription;
import consulo.xml.util.xml.reflect.DomCollectionChildDescription;
import consulo.xml.util.xml.reflect.DomGenericInfo;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author peter
 */
public class DomHighlightingHelperImpl extends DomHighlightingHelper {
  public static final DomHighlightingHelperImpl INSTANCE = new DomHighlightingHelperImpl();
  private final GenericValueReferenceProvider myProvider = new GenericValueReferenceProvider();
  private final DomApplicationComponent myDomApplicationComponent = DomApplicationComponent.getInstance();

  public void runAnnotators(DomElement element, DomElementAnnotationHolder holder, Class<? extends DomElement> rootClass) {
    final DomElementsAnnotator annotator = myDomApplicationComponent.getAnnotator(rootClass);
    if (annotator != null) {
      annotator.annotate(element, holder);
    }
  }

  @Nonnull
  public List<DomElementProblemDescriptor> checkRequired(final DomElement element, final DomElementAnnotationHolder holder) {
    final Required required = element.getAnnotation(Required.class);
    if (required != null) {
      final XmlElement xmlElement = element.getXmlElement();
      if (xmlElement == null) {
        if (required.value()) {
          final String xmlElementName = element.getXmlElementName();
          if (element instanceof GenericAttributeValue) {
            return Arrays.asList(holder.createProblem(element, IdeBundle.message("attribute.0.should.be.defined", xmlElementName)));
          }
          return Arrays.asList(
            holder.createProblem(
              element,
              HighlightSeverity.ERROR,
              IdeBundle.message("child.tag.0.should.be.defined", xmlElementName),
              new AddRequiredSubtagFix(xmlElementName, element.getXmlElementNamespace(), element.getParent().getXmlTag())
            )
          );
        }
      }
      else if (element instanceof GenericDomValue) {
        return ContainerUtil.createMaybeSingletonList(checkRequiredGenericValue((GenericDomValue)element, required, holder));
      }
    }
    if (DomUtil.hasXml(element)) {
      final SmartList<DomElementProblemDescriptor> list = new SmartList<DomElementProblemDescriptor>();
      final DomGenericInfo info = element.getGenericInfo();
      for (final AbstractDomChildrenDescription description : info.getChildrenDescriptions()) {
        if (description instanceof DomCollectionChildDescription && description.getValues(element).isEmpty()) {
          final DomCollectionChildDescription childDescription = (DomCollectionChildDescription)description;
          final Required annotation = description.getAnnotation(Required.class);
          if (annotation != null && annotation.value()) {
            list.add(holder.createProblem(element, childDescription, IdeBundle.message("child.tag.0.should.be.defined", ((DomCollectionChildDescription)description).getXmlElementName())));
          }
        }
      }
      return list;
    }
    return Collections.emptyList();
  }

  @Nonnull
  public List<DomElementProblemDescriptor> checkResolveProblems(GenericDomValue element, final DomElementAnnotationHolder holder) {
    if (StringUtil.isEmpty(element.getStringValue())) {
      final Required required = element.getAnnotation(Required.class);
      if (required != null && !required.nonEmpty()) return Collections.emptyList();
    }

    final XmlElement valueElement = DomUtil.getValueElement(element);
    if (valueElement != null && !isSoftReference(element)) {
      final SmartList<DomElementProblemDescriptor> list = new SmartList<DomElementProblemDescriptor>();
      final PsiReference[] psiReferences = myProvider.getReferencesByElement(valueElement, new ProcessingContext());
      GenericDomValueReference domReference = ContainerUtil.findInstance(psiReferences, GenericDomValueReference.class);
      final Converter converter = WrappingConverter.getDeepestConverter(element.getConverter(), element);
      boolean hasBadResolve = false;
      if (domReference == null || !isDomResolveOK(element, domReference, converter)) {
        for (final PsiReference reference : psiReferences) {
          if (reference != domReference && hasBadResolve(reference)) {
            hasBadResolve = true;
            list.add(holder.createResolveProblem(element, reference));
          }
        }
        final boolean isResolvingConverter = converter instanceof ResolvingConverter;
        if (!hasBadResolve &&
            (domReference != null || isResolvingConverter &&
                                     hasBadResolve(domReference = new GenericDomValueReference(element)))) {
          hasBadResolve = true;
          list.add(holder.createResolveProblem(element, domReference));
        }
      }
      if (!hasBadResolve && psiReferences.length == 0 && element.getValue() == null && !PsiTreeUtil.hasErrorElements(valueElement)) {
        final LocalizeValue errorMessage = converter
          .buildUnresolvedMessage(element.getStringValue(), ConvertContextFactory.createConvertContext(DomManagerImpl.getDomInvocationHandler(element)));
        if (errorMessage != LocalizeValue.of()) {
          list.add(holder.createProblem(element, errorMessage.get()));
        }
      }
      return list;
    }
    return Collections.emptyList();
  }

  private static boolean isDomResolveOK(GenericDomValue element, GenericDomValueReference domReference, Converter converter) {
    return !hasBadResolve(domReference) ||
           converter instanceof ResolvingConverter && ((ResolvingConverter)converter).getAdditionalVariants(domReference.getConvertContext()).contains(element.getStringValue());
  }

  @Nonnull
  public List<DomElementProblemDescriptor> checkNameIdentity(DomElement element, final DomElementAnnotationHolder holder) {
    final String elementName = ElementPresentationManager.getElementName(element);
    if (StringUtil.isNotEmpty(elementName)) {
      final DomElement domElement = DomUtil.findDuplicateNamedValue(element, elementName);
      if (domElement != null) {
        final String typeName = ElementPresentationManager.getTypeNameForObject(element);
        final GenericDomValue genericDomValue = domElement.getGenericInfo().getNameDomElement(element);
        if (genericDomValue != null) {
          return Arrays.asList(holder.createProblem(genericDomValue, DomUtil.getFile(domElement).equals(DomUtil.getFile(element))
                                                                     ? IdeBundle.message("model.highlighting.identity", typeName)
                                                                     : IdeBundle.message("model.highlighting.identity.in.other.file", typeName,
                                                                                         domElement.getXmlTag().getContainingFile().getName())));
        }
      }
    }
    return Collections.emptyList();
  }

  private static boolean hasBadResolve(PsiReference reference) {
    return XmlHighlightVisitor.hasBadResolve(reference, true);
  }

  private static boolean isSoftReference(GenericDomValue value) {
    final Resolve resolve = value.getAnnotation(Resolve.class);
    if (resolve != null && resolve.soft()) return true;

    final Convert convert = value.getAnnotation(Convert.class);
    if (convert != null && convert.soft()) return true;

    final Referencing referencing = value.getAnnotation(Referencing.class);
    return referencing != null && referencing.soft();

  }

  @Nullable
  private static DomElementProblemDescriptor checkRequiredGenericValue(final GenericDomValue child, final Required required,
                                                                       final DomElementAnnotationHolder annotator) {
    final String stringValue = child.getStringValue();
    if (stringValue == null) return null;

    if (required.nonEmpty() && isEmpty(child, stringValue)) {
      return annotator.createProblem(child, IdeBundle.message("value.must.not.be.empty"));
    }
    if (required.identifier() && !isIdentifier(stringValue)) {
      return annotator.createProblem(child, IdeBundle.message("value.must.be.identifier"));
    }
    return null;
  }

  private static boolean isIdentifier(final String s) {
    if (StringUtil.isEmptyOrSpaces(s)) return false;

    if (!Character.isJavaIdentifierStart(s.charAt(0))) return false;

    for (int i = 1; i < s.length(); i++) {
      if (!Character.isJavaIdentifierPart(s.charAt(i))) return false;
    }

    return true;
  }

  private static boolean isEmpty(final GenericDomValue child, final String stringValue) {
    if (stringValue.trim().length() != 0) {
      return false;
    }
    if (child instanceof GenericAttributeValue) {
      final XmlAttributeValue value = ((GenericAttributeValue)child).getXmlAttributeValue();
      if (value != null && value.getTextRange().isEmpty()) {
        return false;
      }
    }
    return true;
  }


  private static class AddRequiredSubtagFix implements LocalQuickFix, SyntheticIntentionAction
  {
    private final String tagName;
    private final String tagNamespace;
    private final XmlTag parentTag;

    public AddRequiredSubtagFix(@Nonnull String _tagName, @Nonnull String _tagNamespace, @Nonnull XmlTag _parentTag) {
      tagName = _tagName;
      tagNamespace = _tagNamespace;
      parentTag = _parentTag;
    }

    @Nonnull
    public String getName() {
      return XmlBundle.message("insert.required.tag.fix", tagName);
    }

    @Nonnull
    public String getText() {
      return getName();
    }

    @Nonnull
    public String getFamilyName() {
      return getName();
    }

    public boolean isAvailable(@Nonnull final Project project, final Editor editor, final PsiFile file) {
      return true;
    }

    public void invoke(@Nonnull final Project project, final Editor editor, final PsiFile file) throws consulo.language.util.IncorrectOperationException {
      doFix();
    }

    public boolean startInWriteAction() {
      return true;
    }

    public void applyFix(@Nonnull final Project project, @Nonnull final ProblemDescriptor descriptor) {
      doFix();
    }

    private void doFix() {
      if (!FileModificationService.getInstance().prepareFileForWrite(parentTag.getContainingFile())) return;

      try {
        parentTag.add(parentTag.createChildTag(tagName, tagNamespace, null, false));
      }
      catch (IncorrectOperationException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
