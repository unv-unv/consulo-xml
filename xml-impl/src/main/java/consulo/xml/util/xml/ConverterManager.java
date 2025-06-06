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

package consulo.xml.util.xml;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author Dmitry Avdeev
 * @see DomManager#getConverterManager()
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface ConverterManager {

  void addConverter(Class clazz, Converter converter);

  @Nonnull
  Converter getConverterInstance(Class<? extends Converter> converterClass);

  @Nullable
  Converter getConverterByClass(Class<?> convertingClass);

  /**
   * Registers the given {@link Converter} implementation at runtime.
   *
   * @param converterInterface Interface defined in {@link DomElement} definition.
   * @param converterImpl      Implementation to use.
   * @deprecated use com.intellij.dom.converter extension instead
   */
  <T extends Converter> void registerConverterImplementation(Class<T> converterInterface, T converterImpl);
}
