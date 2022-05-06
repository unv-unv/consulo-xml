/*
 * Copyright 2006 Sascha Weinreuter
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
package org.intellij.plugins.intelliLang.inject.config;

import com.intellij.psi.xml.XmlTag;
import consulo.ide.impl.intelliLang.inject.InjectorUtils;
import consulo.ide.impl.intelliLang.inject.config.BaseInjection;
import consulo.ide.impl.intelliLang.inject.config.InjectionPlace;
import consulo.util.lang.StringUtil;

import javax.annotation.Nonnull;

public class XmlTagInjection extends AbstractTagInjection {

  public XmlTagInjection() {
    setTagName("<none>");
  }

  public boolean isApplicable(@Nonnull final XmlTag context) {
    return matches(context) && matchXPath(context);
  }

  @Nonnull
  public String getDisplayName() {
    final String name = getTagName();
    return name.length() > 0 ? name : "*";
  }

  @Override
  public XmlTagInjection copy() {
    return new XmlTagInjection().copyFrom(this);
  }

  public XmlTagInjection copyFrom(@Nonnull BaseInjection o) {
    super.copyFrom(o);
    return this;
  }

  @Override
  public void generatePlaces() {
    setInjectionPlaces(new InjectionPlace(getCompiler().createElementPattern(getPatternString(this), getDisplayName()), true));
  }

  public static String getPatternString(final AbstractTagInjection injection) {
    final String name = injection.getTagName();
    final String namespace = injection.getTagNamespace();
    final StringBuilder result = new StringBuilder("xmlTag()");
    if (StringUtil.isNotEmpty(name)) InjectorUtils.appendStringPattern(result, ".withLocalName(", name, ")");
    if (StringUtil.isNotEmpty(namespace)) InjectorUtils.appendStringPattern(result, ".withNamespace(", namespace, ")");
    return result.toString();
  }


}
