/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.xml.util;

import java.util.Map;

import jakarta.annotation.Nonnull;

import consulo.language.psi.meta.PsiMetaData;
import consulo.logging.Logger;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nullable;

import consulo.language.psi.PsiElement;
import consulo.xml.psi.impl.source.xml.XmlTagValueImpl;
import consulo.xml.psi.xml.XmlAttribute;
import consulo.xml.psi.xml.XmlTag;
import consulo.xml.psi.xml.XmlTagChild;
import consulo.xml.psi.xml.XmlTagValue;
import consulo.language.util.IncorrectOperationException;
import com.intellij.xml.XmlElementDescriptor;
import com.intellij.xml.XmlNSDescriptor;

/**
 * @author peter
 */
public class IncludedXmlTag extends IncludedXmlElement<XmlTag> implements XmlTag {
    private static final Logger LOG = Logger.getInstance("#IncludedXmlTag");

    public IncludedXmlTag(@Nonnull XmlTag original, @Nullable PsiElement parent) {
        super(original, parent);
    }

    @Override
    @Nullable
    public XmlTag getParentTag() {
        return getParent() instanceof XmlTag parentTag ? parentTag : null;
    }

    @Override
    @Nonnull
    @NonNls
    public String getName() {
        return getOriginal().getName();
    }

    @Override
    public PsiElement setName(@NonNls @Nonnull String name) throws IncorrectOperationException {
        throw new UnsupportedOperationException("Can't modify included tags");
    }

    @Override
    @Nonnull
    @NonNls
    public String getNamespace() {
        XmlTag original = getOriginal();
        LOG.assertTrue(original.isValid());
        return original.getNamespace();
    }

    @Override
    @Nonnull
    @NonNls
    public String getLocalName() {
        return getOriginal().getLocalName();
    }

    @Override
    @Nullable
    public XmlElementDescriptor getDescriptor() {
        return getOriginal().getDescriptor();
    }

    @Override
    @Nonnull
    public XmlAttribute[] getAttributes() {
        XmlAttribute[] original = getOriginal().getAttributes();
        XmlAttribute[] attributes = new XmlAttribute[original.length];
        for (int i = 0; i < original.length; i++) {
            XmlAttribute attribute = original[i];
            attributes[i] = new IncludedXmlAttribute(attribute, this);
        }
        return attributes;
    }

    @Override
    @Nullable
    public XmlAttribute getAttribute(@NonNls String name, @NonNls String namespace) {
        XmlAttribute attribute = getOriginal().getAttribute(name, namespace);
        return attribute == null ? null : new IncludedXmlAttribute(attribute, this);
    }

    @Override
    @Nullable
    public XmlAttribute getAttribute(@NonNls String qname) {
        XmlAttribute attribute = getOriginal().getAttribute(qname);
        return attribute == null ? null : new IncludedXmlAttribute(attribute, this);
    }

    @Override
    @Nullable
    public String getAttributeValue(@NonNls String name, @NonNls String namespace) {
        return getOriginal().getAttributeValue(name, namespace);
    }

    @Override
    @Nullable
    public String getAttributeValue(@NonNls String qname) {
        return getOriginal().getAttributeValue(qname);
    }

    @Override
    public XmlAttribute setAttribute(@NonNls String name, @NonNls String namespace, @NonNls String value)
        throws IncorrectOperationException {
        throw new UnsupportedOperationException("Can't modify included tags");
    }

    @Override
    public XmlAttribute setAttribute(@NonNls String qname, @NonNls String value) throws IncorrectOperationException {
        throw new UnsupportedOperationException("Can't modify included tags");
    }

    @Override
    public XmlTag createChildTag(
        @NonNls String localName,
        @NonNls String namespace,
        @Nullable @NonNls String bodyText,
        boolean enforceNamespacesDeep
    ) {
        return getOriginal().createChildTag(localName, namespace, bodyText, enforceNamespacesDeep);
    }

    @Override
    public XmlTag addSubTag(XmlTag subTag, boolean first) {
        throw new UnsupportedOperationException("Can't modify included tags");
    }

    @Override
    @Nonnull
    public XmlTag[] getSubTags() {
        return wrapTags(getOriginal().getSubTags());
    }

    private XmlTag[] wrapTags(XmlTag[] original) {
        XmlTag[] result = new XmlTag[original.length];
        for (int i = 0; i < original.length; i++) {
            result[i] = new IncludedXmlTag(original[i], this);
        }
        return result;
    }

    @Override
    @Nonnull
    public XmlTag[] findSubTags(@NonNls String qname) {
        return wrapTags(getOriginal().findSubTags(qname));
    }

    @Override
    @Nonnull
    public XmlTag[] findSubTags(@NonNls String localName, @NonNls String namespace) {
        return wrapTags(getOriginal().findSubTags(localName, namespace));
    }

    @Override
    @Nullable
    public XmlTag findFirstSubTag(@NonNls String qname) {
        XmlTag tag = getOriginal().findFirstSubTag(qname);
        return tag == null ? null : new IncludedXmlTag(tag, this);
    }

    @Override
    @Nonnull
    @NonNls
    public String getNamespacePrefix() {
        return getOriginal().getNamespacePrefix();
    }

    @Override
    @Nonnull
    @NonNls
    public String getNamespaceByPrefix(@NonNls String prefix) {
        return getOriginal().getNamespaceByPrefix(prefix);
    }

    @Override
    @Nullable
    public String getPrefixByNamespace(@NonNls String namespace) {
        return getOriginal().getPrefixByNamespace(namespace);
    }

    @Override
    public String[] knownNamespaces() {
        return getOriginal().knownNamespaces();
    }

    @Override
    public boolean hasNamespaceDeclarations() {
        return getOriginal().hasNamespaceDeclarations();
    }

    @Override
    @Nonnull
    public Map<String, String> getLocalNamespaceDeclarations() {
        return getOriginal().getLocalNamespaceDeclarations();
    }

    @Override
    @Nonnull
    public XmlTagValue getValue() {
        return XmlTagValueImpl.createXmlTagValue(this);
    }

    @Override
    @Nullable
    public XmlNSDescriptor getNSDescriptor(@NonNls String namespace, boolean strict) {
        return getOriginal().getNSDescriptor(namespace, strict);
    }

    @Override
    public boolean isEmpty() {
        return getOriginal().isEmpty();
    }

    @Override
    public void collapseIfEmpty() {
        throw new UnsupportedOperationException("Can't modify included tags");
    }

    @Override
    @Nullable
    @NonNls
    public String getSubTagText(@NonNls String qname) {
        return getOriginal().getSubTagText(qname);
    }

    @Override
    public PsiMetaData getMetaData() {
        return null;
    }

    @Override
    public XmlTagChild getNextSiblingInTag() {
        return null;
    }

    @Override
    public XmlTagChild getPrevSiblingInTag() {
        return null;
    }
}
