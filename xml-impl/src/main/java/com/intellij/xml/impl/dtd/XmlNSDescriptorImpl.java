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
package com.intellij.xml.impl.dtd;

import com.intellij.xml.XmlElementDescriptor;
import com.intellij.xml.XmlNSDescriptorEx;
import com.intellij.xml.impl.ExternalDocumentValidator;
import com.intellij.xml.util.XmlUtil;
import consulo.application.dumb.DumbAware;
import consulo.application.util.CachedValue;
import consulo.application.util.CachedValueProvider;
import consulo.application.util.CachedValuesManager;
import consulo.application.util.SimpleFieldCache;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.filter.ClassFilter;
import consulo.language.psi.resolve.FilterElementProcessor;
import consulo.language.psi.resolve.PsiElementProcessor;
import consulo.xml.Validator;
import consulo.xml.javaee.ExternalResourceManager;
import consulo.xml.lang.dtd.DTDLanguage;
import consulo.xml.psi.xml.*;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.*;

/**
 * @author Mike
 */
public class XmlNSDescriptorImpl implements XmlNSDescriptorEx, Validator<XmlDocument>, DumbAware {
    private XmlElement myElement;
    private XmlFile myDescriptorFile;

    private static final SimpleFieldCache<CachedValue<Map<String, XmlElementDescriptor>>, XmlNSDescriptorImpl> myCachedDeclsCache =
        new SimpleFieldCache<>() {
            @Override
            protected final CachedValue<Map<String, XmlElementDescriptor>> compute(final XmlNSDescriptorImpl xmlNSDescriptor) {
                return xmlNSDescriptor.doBuildDeclarationMap();
            }

            @Override
            protected final CachedValue<Map<String, XmlElementDescriptor>> getValue(final XmlNSDescriptorImpl xmlNSDescriptor) {
                return xmlNSDescriptor.myCachedDecls;
            }

            @Override
            protected final void putValue(
                final CachedValue<Map<String, XmlElementDescriptor>> cachedValue,
                final XmlNSDescriptorImpl xmlNSDescriptor
            ) {
                xmlNSDescriptor.myCachedDecls = cachedValue;
            }
        };

    private volatile CachedValue<Map<String, XmlElementDescriptor>> myCachedDecls;
    private static final XmlUtil.DuplicationInfoProvider<XmlElementDecl> XML_ELEMENT_DECL_PROVIDER =
        new XmlUtil.DuplicationInfoProvider<>() {
            @Override
            public String getName(@Nonnull final XmlElementDecl psiElement) {
                return psiElement.getName();
            }

            @Override
            @Nonnull
            public String getNameKey(@Nonnull final XmlElementDecl psiElement, @Nonnull final String name) {
                return name;
            }

            @Override
            @Nonnull
            public PsiElement getNodeForMessage(@Nonnull final XmlElementDecl psiElement) {
                return psiElement.getNameElement();
            }
        };

    public XmlNSDescriptorImpl() {
    }

    @Override
    public XmlFile getDescriptorFile() {
        return myDescriptorFile;
    }

    public XmlElementDescriptor[] getElements() {
        final Collection<XmlElementDescriptor> declarations = buildDeclarationMap().values();
        return declarations.toArray(new XmlElementDescriptor[declarations.size()]);
    }

    private Map<String, XmlElementDescriptor> buildDeclarationMap() {
        return myCachedDeclsCache.get(this).getValue();
    }

    // Read-only calculation
    private CachedValue<Map<String, XmlElementDescriptor>> doBuildDeclarationMap() {
        return CachedValuesManager.getManager(myElement.getProject()).createCachedValue(() ->
        {
            final List<XmlElementDecl> result = new ArrayList<>();
            myElement.processElements(new FilterElementProcessor(new ClassFilter(XmlElementDecl.class), result), getDeclaration());
            final Map<String, XmlElementDescriptor> ret = new LinkedHashMap<>((int)(result.size() * 1.5));
            Set<PsiFile> dependencies = new HashSet<>(1);
            dependencies.add(myDescriptorFile);

            for (final XmlElementDecl xmlElementDecl : result) {
                final String name = xmlElementDecl.getName();
                if (name != null) {
                    if (!ret.containsKey(name)) {
                        ret.put(name, new XmlElementDescriptorImpl(xmlElementDecl));
                        // if element descriptor was produced from entity reference use proper dependency
                        PsiElement dependingElement = xmlElementDecl.getUserData(XmlElement.DEPENDING_ELEMENT);
                        if (dependingElement != null) {
                            PsiFile dependingElementContainingFile = dependingElement.getContainingFile();
                            if (dependingElementContainingFile != null) {
                                dependencies.add(dependingElementContainingFile);
                            }
                        }
                    }
                }
            }
            return new CachedValueProvider.Result<>(ret, dependencies.toArray());
        }, false);
    }

    @Override
    public XmlElementDescriptor getElementDescriptor(@Nonnull XmlTag tag) {
        String name = tag.getName();
        return getElementDescriptor(name);
    }

    @Override
    @Nonnull
    public XmlElementDescriptor[] getRootElementsDescriptors(@Nullable final XmlDocument document) {
        // Suggest more appropriate variant if DOCTYPE <element_name> exists
        final XmlProlog prolog = document != null ? document.getProlog() : null;

        if (prolog != null) {
            final XmlDoctype doctype = prolog.getDoctype();

            if (doctype != null) {
                final XmlElement element = doctype.getNameElement();

                if (element != null) {
                    final XmlElementDescriptor descriptor = getElementDescriptor(element.getText());

                    if (descriptor != null) {
                        return new XmlElementDescriptor[]{descriptor};
                    }
                }
            }
        }

        return getElements();
    }

    public final XmlElementDescriptor getElementDescriptor(String name) {
        return buildDeclarationMap().get(name);
    }

    @Override
    public PsiElement getDeclaration() {
        return myElement;
    }

    @Override
    public String getName(PsiElement context) {
        return getName();
    }

    @Override
    public String getName() {
        return myDescriptorFile.getName();
    }

    @Override
    public void init(PsiElement element) {
        myElement = (XmlElement)element;
        myDescriptorFile = (XmlFile)element.getContainingFile();

        if (myElement instanceof XmlFile file) {
            myElement = file.getDocument();
        }
    }

    @Override
    public Object[] getDependences() {
        return new Object[]{
            myElement,
            ExternalResourceManager.getInstance()
        };
    }

    @Override
    public void validate(@Nonnull XmlDocument document, @Nonnull ValidationHost host) {
        if (document.getLanguage() == DTDLanguage.INSTANCE) {
            final List<XmlElementDecl> decls = new ArrayList<>(3);

            XmlUtil.processXmlElements(document, element -> {
                if (element instanceof XmlElementDecl elementDecl) {
                    decls.add(elementDecl);
                }
                return true;
            }, false);
            XmlUtil.doDuplicationCheckForElements(
                decls.toArray(new XmlElementDecl[decls.size()]),
                new HashMap<>(decls.size()),
                XML_ELEMENT_DECL_PROVIDER,
                host
            );
            return;
        }
        ExternalDocumentValidator.doValidation(document, host);
    }

    @Override
    public XmlElementDescriptor getElementDescriptor(String localName, String namespace) {
        return getElementDescriptor(localName);
    }
}
