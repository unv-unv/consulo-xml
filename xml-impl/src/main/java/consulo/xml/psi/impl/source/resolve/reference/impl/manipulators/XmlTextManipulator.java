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

package consulo.xml.psi.impl.source.resolve.reference.impl.manipulators;

import consulo.annotation.component.ExtensionImpl;
import consulo.document.util.TextRange;
import consulo.language.psi.AbstractElementManipulator;
import consulo.language.util.IncorrectOperationException;
import consulo.util.lang.Comparing;
import consulo.xml.psi.xml.XmlText;

import jakarta.annotation.Nonnull;

/**
 * @author Gregory.Shrago
 */
@ExtensionImpl
public class XmlTextManipulator extends AbstractElementManipulator<XmlText>
{
	public XmlText handleContentChange(XmlText text, TextRange range, String newContent) throws IncorrectOperationException
	{
		final String newValue;
		final String value = text.getValue();
		if(range.equals(getRangeInElement(text)))
		{
			newValue = newContent;
		}
		else
		{
			final StringBuilder replacement = new StringBuilder(value);
			replacement.replace(
					range.getStartOffset(),
					range.getEndOffset(),
					newContent
			);
			newValue = replacement.toString();
		}
		if(Comparing.equal(value, newValue))
		{
			return text;
		}
		if(newValue.length() > 0)
		{
			text.setValue(newValue);
		}
		else
		{
			text.deleteChildRange(text.getFirstChild(), text.getLastChild());
		}
		return text;
	}

	public TextRange getRangeInElement(final XmlText text)
	{
		return getValueRange(text);
	}

	@Nonnull
	@Override
	public Class<XmlText> getElementClass()
	{
		return XmlText.class;
	}

	private static TextRange getValueRange(final XmlText xmlText)
	{
		final String value = xmlText.getValue();
		final int i = value.indexOf(value);
		final int start = xmlText.displayToPhysical(i);
		return value.length() == 0 ? new TextRange(start, start) : new TextRange(start, xmlText.displayToPhysical(i + value.length() - 1) + 1);
	}
}