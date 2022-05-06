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
package com.intellij.xml;

import javax.annotation.Nonnull;

import consulo.component.extension.ExtensionPointName;
import com.intellij.psi.xml.XmlTag;
import consulo.language.editor.inspection.ProblemsHolder;

public abstract class XmlTagRuleProvider {

  public static final ExtensionPointName<XmlTagRuleProvider> EP_NAME = ExtensionPointName.create("com.intellij.xml.tagRuleProvider");

  public abstract Rule[] getTagRule(@Nonnull XmlTag tag);

  public static class Rule {

    public static final Rule[] EMPTY_ARRAY = new Rule[0];

    public void annotate(@Nonnull XmlTag tag, ProblemsHolder holder) {

    }

    public boolean needAtLeastOneAttribute(@Nonnull XmlTag tag) {
      return false;
    }
  }

}
