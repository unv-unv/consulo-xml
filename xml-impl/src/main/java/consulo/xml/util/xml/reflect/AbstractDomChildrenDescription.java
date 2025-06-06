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

package consulo.xml.util.xml.reflect;

import consulo.language.pom.PomTarget;
import consulo.project.Project;
import consulo.language.psi.PsiElement;
import consulo.xml.util.xml.AnnotatedElement;
import consulo.xml.util.xml.DomElement;
import consulo.xml.util.xml.DomNameStrategy;
import consulo.xml.util.xml.ElementPresentationTemplate;
import consulo.util.dataholder.Key;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.lang.reflect.Type;
import java.util.List;

/**
 * @author peter
 */
public interface AbstractDomChildrenDescription extends AnnotatedElement, PomTarget {
  @Nonnull
  List<? extends DomElement> getValues(@Nonnull DomElement parent);

  @Nonnull
  List<? extends DomElement> getStableValues(@Nonnull DomElement parent);

  @Nonnull
  Type getType();

  @Nonnull
  DomNameStrategy getDomNameStrategy(@Nonnull DomElement parent);

  <T> T getUserData(Key<T> key);

  @Nullable
  ElementPresentationTemplate getPresentationTemplate();

  @Nullable
  PsiElement getDeclaration(Project project);

  @Nullable
  DomElement getDomDeclaration();

  boolean isStubbed();
}
