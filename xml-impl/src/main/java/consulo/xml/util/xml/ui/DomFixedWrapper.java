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
package consulo.xml.util.xml.ui;

import consulo.xml.util.xml.DomElement;
import consulo.xml.util.xml.GenericDomValue;
import jakarta.annotation.Nonnull;

import java.lang.reflect.InvocationTargetException;

/**
 * @author peter
 */
public class DomFixedWrapper<T> extends DomWrapper<T>{
  private final GenericDomValue myDomElement;

  public DomFixedWrapper(final GenericDomValue<? extends T> domElement) {
    myDomElement = domElement;
  }

  public DomElement getWrappedElement() {
    return myDomElement;
  }

  public void setValue(final T value) throws IllegalAccessException, InvocationTargetException {
    DomUIFactory.SET_VALUE_METHOD.invoke(getWrappedElement(), value);
  }

  public T getValue() throws IllegalAccessException, InvocationTargetException {
    final DomElement element = getWrappedElement();
    return element.isValid() ? (T)DomUIFactory.GET_VALUE_METHOD.invoke(element) : null;
  }

  @Nonnull
  public DomElement getExistingDomElement() {
    return myDomElement;
  }


}
