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
package consulo.xml.patterns;

import consulo.xml.psi.xml.XmlElement;
import consulo.xml.psi.xml.XmlEntityRef;
import consulo.xml.psi.xml.XmlText;
import consulo.language.pattern.InitialPatternCondition;
import consulo.language.pattern.PsiElementPattern;
import consulo.language.util.ProcessingContext;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author peter
 */
public class XmlElementPattern<T extends XmlElement,Self extends XmlElementPattern<T,Self>> extends PsiElementPattern<T,Self> {
  protected XmlElementPattern(final Class<T> aClass) {
    super(aClass);
  }

  public XmlElementPattern(@Nonnull final InitialPatternCondition<T> condition) {
    super(condition);
  }

  public static class Capture extends XmlElementPattern<XmlElement, Capture> {
    public Capture() {
      super(new InitialPatternCondition<XmlElement>(XmlElement.class) {
        public boolean accepts(@Nullable final Object o, final ProcessingContext context) {
          return o instanceof XmlElement;
        }
      });
    }
  }

  public static class XmlTextPattern extends XmlElementPattern<XmlText, XmlTextPattern> {
    public XmlTextPattern() {
      super(new InitialPatternCondition<XmlText>(XmlText.class) {
        public boolean accepts(@Nullable final Object o, final ProcessingContext context) {
          return o instanceof XmlText;
        }
      });
    }
  }

  public static class XmlEntityRefPattern extends XmlElementPattern<XmlEntityRef, XmlEntityRefPattern> {
    public XmlEntityRefPattern() {
      super(new InitialPatternCondition<XmlEntityRef>(XmlEntityRef.class) {
        public boolean accepts(@Nullable final Object o, final ProcessingContext context) {
          return o instanceof XmlEntityRef;
        }
      });
    }
  }

}
