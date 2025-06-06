/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package org.intellij.plugins.relaxNG;

import consulo.annotation.component.ExtensionImpl;
import consulo.document.util.TextRange;
import consulo.language.ast.ASTNode;
import consulo.language.psi.AbstractElementManipulator;
import consulo.language.psi.PsiManager;
import consulo.language.util.IncorrectOperationException;
import org.intellij.plugins.relaxNG.compact.RncTokenTypes;
import org.intellij.plugins.relaxNG.compact.psi.RncFileReference;
import org.intellij.plugins.relaxNG.compact.psi.util.RenameUtil;

import jakarta.annotation.Nonnull;

/**
 * @author peter
 */
@ExtensionImpl
public class RncFileReferenceManipulator extends AbstractElementManipulator<RncFileReference>
{
	@Override
	public RncFileReference handleContentChange(@Nonnull RncFileReference element, @Nonnull TextRange range, String newContent) throws
			IncorrectOperationException
	{
		final ASTNode node = element.getNode();
		assert node != null;

		final ASTNode literal = node.findChildByType(RncTokenTypes.LITERAL);
		if(literal != null)
		{
			assert range.equals(element.getReferenceRange());
			final PsiManager manager = element.getManager();
			final ASTNode newChild = RenameUtil.createLiteralNode(manager, newContent);
			literal.getTreeParent().replaceChild(literal, newChild);
		}
		return element;
	}

	@Nonnull
	@Override
	public TextRange getRangeInElement(@Nonnull RncFileReference element)
	{
		return element.getReferenceRange();
	}

	@Nonnull
	@Override
	public Class<RncFileReference> getElementClass()
	{
		return RncFileReference.class;
	}
}
