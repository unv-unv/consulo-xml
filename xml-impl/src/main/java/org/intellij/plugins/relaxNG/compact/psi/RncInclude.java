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

package org.intellij.plugins.relaxNG.compact.psi;

import jakarta.annotation.Nonnull;

import org.intellij.plugins.relaxNG.model.Include;

/**
 * Created by IntelliJ IDEA.
 * User: sweinreuter
 * Date: 11.08.2007
 */
public interface RncInclude extends RncFileReference, Include<RncElement, RncDefine> {
  @Override
  @Nonnull
  RncDefine[] getOverrides();
}
