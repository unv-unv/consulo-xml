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

package org.intellij.plugins.relaxNG.compact.parser;

import consulo.language.parser.PsiParser;
import consulo.language.version.LanguageVersion;
import org.intellij.plugins.relaxNG.compact.RncElementTypes;
import jakarta.annotation.Nonnull;
import consulo.language.ast.ASTNode;
import consulo.language.parser.PsiBuilder;
import consulo.language.ast.IElementType;

/**
 * Created by IntelliJ IDEA.
* User: sweinreuter
* Date: 07.08.2007
*/
public class RncParser implements PsiParser {

  @Override
  @Nonnull
  public ASTNode parse(IElementType root, PsiBuilder builder, LanguageVersion languageVersion) {
    final PsiBuilder.Marker fileMarker = builder.mark();
    final PsiBuilder.Marker docMarker = builder.mark();

    new PatternParsing(builder).parse();

    while (!builder.eof()) {
      builder.error("Unexpected token");
      builder.advanceLexer();
    }

    docMarker.done(RncElementTypes.DOCUMENT);
    fileMarker.done(root);
    return builder.getTreeBuilt();
  }
}
