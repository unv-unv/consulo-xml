/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.xml.psi.impl.source.resolve.reference.impl.providers;

import com.intellij.xml.XmlNSDescriptor;
import com.intellij.xml.util.XmlUtil;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.access.RequiredWriteAction;
import consulo.document.util.TextRange;
import consulo.language.psi.*;
import consulo.language.psi.path.FileReferenceUtil;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.language.util.IncorrectOperationException;
import consulo.localize.LocalizeValue;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import consulo.xml.impl.localize.XmlErrorLocalize;
import consulo.xml.javaee.ExternalResourceManager;
import consulo.xml.javaee.ExternalResourceManagerEx;
import consulo.xml.psi.xml.XmlAttribute;
import consulo.xml.psi.xml.XmlDocument;
import consulo.xml.psi.xml.XmlFile;
import consulo.xml.psi.xml.XmlTag;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NonNls;

import java.util.function.Predicate;

/**
 * @author Dmitry Avdeev
 */
public class URLReference implements PsiReference, EmptyResolveMessageProvider {
  @NonNls
  public static final String TARGET_NAMESPACE_ATTR_NAME = "targetNamespace";

  private final PsiElement myElement;
  private final TextRange myRange;
  private final boolean mySoft;
  private boolean myIncorrectResourceMapped;

  public URLReference(PsiElement element) {
    this(element, null, false);
  }

  public URLReference(PsiElement element, @Nullable TextRange range, boolean soft) {
    myElement = element;
    myRange = range;
    mySoft = soft;
  }

  @RequiredReadAction
  @Override
  public PsiElement getElement() {
    return myElement;
  }

  @RequiredReadAction
  @Override
  public TextRange getRangeInElement() {
    return myRange != null ? myRange : ElementManipulators.getValueTextRange(myElement);
  }

  @RequiredReadAction
  @Override
  @Nullable
  public PsiElement resolve() {
    myIncorrectResourceMapped = false;
    final String canonicalText = getCanonicalText();

    if (canonicalText.isEmpty()) {
      final XmlAttribute attr = PsiTreeUtil.getParentOfType(getElement(), XmlAttribute.class);

      if (attr != null && attr.isNamespaceDeclaration() && attr.getNamespacePrefix().isEmpty()
        || ExternalResourceManagerEx.getInstanceEx().isIgnoredResource(canonicalText)) {
        // Namespaces in XML 1.0 2nd edition, Section 6.2, last paragraph
        // The attribute value in a default namespace declaration MAY be empty. This has the same effect, within the scope of the declaration,
        // of there being no default namespace
        return myElement;
      }
      return null;
    }

    if (ExternalResourceManagerEx.getInstanceEx().isIgnoredResource(canonicalText)) {
      return myElement;
    }
    final XmlTag tag = PsiTreeUtil.getParentOfType(myElement, XmlTag.class);
    if (tag != null && canonicalText.equals(tag.getAttributeValue(TARGET_NAMESPACE_ATTR_NAME))) {
      return tag;
    }

    final PsiFile containingFile = myElement.getContainingFile();

    if (tag != null && tag.getAttributeValue("schemaLocation", XmlUtil.XML_SCHEMA_INSTANCE_URI) == null) {
      final PsiFile file =
        ExternalResourceManager.getInstance().getResourceLocation(canonicalText, containingFile, tag.getAttributeValue("version"));
      if (file != null) {
        return file;
      }
    }

    if (containingFile instanceof XmlFile) {
      final XmlDocument document = ((XmlFile)containingFile).getDocument();
      assert document != null;
      final XmlTag rootTag = document.getRootTag();

      if (rootTag == null) {
        return ExternalResourceManager.getInstance().getResourceLocation(canonicalText, containingFile, null);
      }
      final XmlNSDescriptor nsDescriptor = rootTag.getNSDescriptor(canonicalText, true);
      if (nsDescriptor != null) {
        return nsDescriptor.getDescriptorFile();
      }

      final String url = ExternalResourceManager.getInstance().getResourceLocation(canonicalText, myElement.getProject());
      if (!url.equals(canonicalText)) {
        myIncorrectResourceMapped = true;
        return null;
      }

      if (tag == rootTag && (tag.getNamespace().equals(XmlUtil.XML_SCHEMA_URI) || tag.getNamespace().equals(XmlUtil.WSDL_SCHEMA_URI))) {
        for (XmlTag t : tag.getSubTags()) {
          final String name = t.getLocalName();
          if ("import".equals(name)) {
            if (canonicalText.equals(t.getAttributeValue("namespace"))) {
              return t;
            }
          }
          else if (!"include".equals(name) && !"redefine".equals(name) && !"annotation".equals(name)) {
            break;
          }
        }
      }

      final PsiElement[] result = new PsiElement[1];
      processWsdlSchemas(rootTag, t ->
      {
        if (canonicalText.equals(t.getAttributeValue(TARGET_NAMESPACE_ATTR_NAME))) {
          result[0] = t;
          return false;
        }
        for (XmlTag anImport : t.findSubTags("import", t.getNamespace())) {
          if (canonicalText.equals(anImport.getAttributeValue("namespace"))) {
            final XmlAttribute location = anImport.getAttribute("schemaLocation");
            if (location != null) {
              result[0] = FileReferenceUtil.findFile(location.getValueElement());
            }
          }
        }
        return true;
      });

      return result[0];
    }
    return null;
  }

  @RequiredReadAction
  @Override
  @Nonnull
  public String getCanonicalText() {
    final String text = myElement.getText();
    if (text.length() > 1) {
      return myRange == null ? text.substring(1, text.length() - 1) : myRange.substring(text);
    }

    return "";
  }

  @RequiredWriteAction
  @Override
  public PsiElement handleElementRename(String newElementName) throws IncorrectOperationException {
    final TextRange textRangeInElement = getRangeInElement();
    final PsiElement elementToChange = myElement.findElementAt(textRangeInElement.getStartOffset());
    assert elementToChange != null;
    final ElementManipulator<PsiElement> manipulator = ElementManipulators.getManipulator(elementToChange);
    assert manipulator != null;
    final int offsetFromStart =
      myElement.getTextRange().getStartOffset() + textRangeInElement.getStartOffset() - elementToChange.getTextOffset();

    manipulator.handleContentChange(
      elementToChange,
      new TextRange(offsetFromStart, offsetFromStart + textRangeInElement.getLength()),
      newElementName
    );
    return myElement;
  }

  @RequiredWriteAction
  @Override
  public PsiElement bindToElement(@Nonnull PsiElement element) throws IncorrectOperationException {
    assert element instanceof PsiFile;

    if (!XmlUtil.isUrlText(getCanonicalText(), element.getProject())) {
      // TODO: this should work!
      final VirtualFile virtualFile = ((PsiFile)element).getVirtualFile();
      assert virtualFile != null;
      handleElementRename(VirtualFileUtil.fixIDEAUrl(virtualFile.getPresentableUrl()));
    }
    return myElement;
  }

  @RequiredReadAction
  @Override
  public boolean isReferenceTo(PsiElement element) {
    return myElement.getManager().areElementsEquivalent(resolve(), element);
  }

  @RequiredReadAction
  @Override
  @Nonnull
  public Object[] getVariants() {
    return EMPTY_ARRAY;
  }

  public boolean isSchemaLocation() {
    return false;
  }

  @RequiredReadAction
  @Override
  public boolean isSoft() {
    return mySoft;
  }

  @Nonnull
  @Override
  public LocalizeValue buildUnresolvedMessage(@Nonnull String s) {
    return myIncorrectResourceMapped
      ? XmlErrorLocalize.registeredResourceIsNotRecognized()
      : XmlErrorLocalize.uriIsNotRegistered();
  }

  public static void processWsdlSchemas(final XmlTag rootTag, Predicate<XmlTag> processor) {
    if ("definitions".equals(rootTag.getLocalName())) {
      final String nsPrefix = rootTag.getNamespacePrefix();
      final String types = nsPrefix.isEmpty() ? "types" : nsPrefix + ":types";
      final XmlTag subTag = rootTag.findFirstSubTag(types);

      if (subTag != null) {
        for (int i = 0; i < XmlUtil.SCHEMA_URIS.length; i++) {
          final XmlTag[] tags = subTag.findSubTags("schema", XmlUtil.SCHEMA_URIS[i]);
          for (XmlTag t : tags) {
            if (!processor.test(t)) {
              return;
            }
          }
        }
      }
    }
  }
}
