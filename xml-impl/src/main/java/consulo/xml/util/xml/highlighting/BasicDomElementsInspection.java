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

package consulo.xml.util.xml.highlighting;

import jakarta.annotation.Nonnull;

import consulo.xml.util.xml.DomFileDescription;
import consulo.xml.util.xml.NameValue;
import consulo.xml.util.xml.Required;
import consulo.util.lang.reflect.ReflectionUtil;
import consulo.xml.util.xml.DomElement;
import consulo.xml.util.xml.GenericAttributeValue;
import consulo.xml.util.xml.GenericDomValue;

/**
 * Provides basic inspection functionality (resolving, required values, duplicate names, custom annotations).
 */
public abstract class BasicDomElementsInspection<T extends DomElement, State> extends DomElementsInspection<T, State> {

  @SafeVarargs
  public BasicDomElementsInspection(@Nonnull Class<? extends T> domClass, Class<? extends T>... additionalClasses) {
    super(domClass, additionalClasses);
  }

  /**
   * One may want to create several inspections that check for unresolved DOM references of different types. Through
   * this method one can control these types.
   *
   * @param value GenericDomValue containing references in question
   * @return whether to check for resolve problems
   */
  protected boolean shouldCheckResolveProblems(GenericDomValue value) {
    return true;
  }

  /**
   * The default implementations checks for resolve problems (if {@link #shouldCheckResolveProblems(GenericDomValue)}
   * returns true), then runs annotators (see {@link DomFileDescription#createAnnotator()}),
   * checks for {@link Required} and {@link ExtendClass} annotation
   * problems, checks for name identity (see {@link NameValue} annotation) and custom annotation
   * checkers (see {@link DomCustomAnnotationChecker}).
   *
   * @param element element to check
   * @param holder  a place to add problems to
   * @param helper  helper object
   */
  @Override
  protected void checkDomElement(DomElement element, DomElementAnnotationHolder holder, DomHighlightingHelper helper, State state) {
    final int oldSize = holder.getSize();
    if (element instanceof GenericDomValue) {
      final GenericDomValue genericDomValue = (GenericDomValue) element;
      if (shouldCheckResolveProblems(genericDomValue)) {
        helper.checkResolveProblems(genericDomValue, holder);
      }
    }
    for (final Class<? extends T> aClass : getDomClasses()) {
      helper.runAnnotators(element, holder, aClass);
    }
    if (oldSize != holder.getSize()) return;

    if (!helper.checkRequired(element, holder).isEmpty()) return;
    if (!(element instanceof GenericAttributeValue) && !GenericDomValue.class.equals(ReflectionUtil.getRawType(element.getDomElementType()))) {
      if (!helper.checkNameIdentity(element, holder).isEmpty()) return;
    }

    helper.checkCustomAnnotations(element, holder);
  }

}
