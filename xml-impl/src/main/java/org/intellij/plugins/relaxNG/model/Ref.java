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

package org.intellij.plugins.relaxNG.model;

import jakarta.annotation.Nullable;

import consulo.language.psi.PsiElement;

/**
 * Created by IntelliJ IDEA.
 * User: sweinreuter
 * Date: 05.09.2007
 */
public interface Ref<D extends Define, E extends PsiElement> extends Pattern<E> {
  @Nullable
  D getPattern();

  @Nullable
  String getReferencedName();
}
