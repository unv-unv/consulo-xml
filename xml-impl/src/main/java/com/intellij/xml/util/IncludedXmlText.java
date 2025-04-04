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

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import consulo.xml.psi.xml.XmlElement;
import consulo.xml.psi.xml.XmlTag;
import consulo.xml.psi.xml.XmlTagChild;
import consulo.xml.psi.xml.XmlText;
import consulo.language.util.IncorrectOperationException;

/**
 * @author peter
 */
public class IncludedXmlText extends IncludedXmlElement<XmlText> implements XmlText {
    public IncludedXmlText(@Nonnull XmlText original, @Nullable XmlTag parent) {
        super(original, parent);
    }

    @Override
    public XmlTag getParentTag() {
        return (XmlTag)getParent();
    }

    @Override
    public XmlTagChild getNextSiblingInTag() {
        return null;
    }

    @Override
    public XmlTagChild getPrevSiblingInTag() {
        return null;
    }

    @Override
    public String getText() {
        return getOriginal().getText();
    }

    public String getValue() {
        return getOriginal().getValue();
    }

    public void setValue(String s) throws IncorrectOperationException {
        throw new UnsupportedOperationException("Can't modify included elements");
    }

    public XmlElement insertAtOffset(XmlElement element, int displayOffset) throws consulo.language.util.IncorrectOperationException {
        throw new UnsupportedOperationException("Can't modify included elements");
    }

    public void insertText(String text, int displayOffset) throws consulo.language.util.IncorrectOperationException {
        throw new UnsupportedOperationException("Can't modify included elements");
    }

    public void removeText(int displayStart, int displayEnd) throws consulo.language.util.IncorrectOperationException {
        throw new UnsupportedOperationException("Can't modify included elements");
    }

    public int physicalToDisplay(int offset) {
        return getOriginal().physicalToDisplay(offset);
    }

    public int displayToPhysical(int offset) {
        return getOriginal().displayToPhysical(offset);
    }

    @Nullable
    public XmlText split(int displayIndex) {
        throw new UnsupportedOperationException("Can't modify included elements");
    }
}
