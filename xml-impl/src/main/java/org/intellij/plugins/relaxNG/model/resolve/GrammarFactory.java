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

package org.intellij.plugins.relaxNG.model.resolve;

import consulo.xml.psi.xml.XmlFile;
import consulo.xml.util.xml.DomFileElement;
import consulo.xml.util.xml.DomManager;
import org.intellij.plugins.relaxNG.compact.psi.RncFile;
import org.intellij.plugins.relaxNG.model.Grammar;
import org.intellij.plugins.relaxNG.xml.dom.RngGrammar;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Created by IntelliJ IDEA.
 * User: sweinreuter
 * Date: 24.11.2007
 */
public class GrammarFactory {
  @Nullable
  public static Grammar getGrammar(@Nonnull XmlFile element) {
    if (element instanceof RncFile) {
      return ((RncFile)element).getGrammar();
    }
    final DomFileElement<RngGrammar> fileElement = DomManager.getDomManager(element.getProject()).getFileElement(element, RngGrammar.class);
    return fileElement != null ? fileElement.getRootElement() : null;
  }
}
