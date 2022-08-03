/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.xml.codeInspection.htmlInspections;

import com.intellij.xml.XmlBundle;
import consulo.language.editor.inspection.InspectionsBundle;
import consulo.language.editor.inspection.UnfairLocalInspectionTool;
import consulo.language.editor.inspection.scheme.InspectionProfileEntry;
import consulo.language.editor.intention.IntentionAction;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.logging.Logger;
import consulo.util.dataholder.Key;
import consulo.xml.codeInspection.XmlInspectionGroupNames;
import consulo.xml.codeInspection.XmlSuppressableInspectionTool;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;

public abstract class RequiredAttributesInspectionBase extends XmlSuppressableInspectionTool implements XmlEntitiesInspection, UnfairLocalInspectionTool
{
	protected static final Logger LOG = Logger.getInstance(RequiredAttributesInspectionBase.class);

	public static final Key<InspectionProfileEntry> SHORT_NAME_KEY = Key.create(REQUIRED_ATTRIBUTES_SHORT_NAME);
	public String myAdditionalRequiredHtmlAttributes = "";

	private static String appendName(String toAppend, String text)
	{
		if(!toAppend.isEmpty())
		{
			toAppend += "," + text;
		}
		else
		{
			toAppend = text;
		}
		return toAppend;
	}

	@Nonnull
	@Override
	public HighlightDisplayLevel getDefaultLevel()
	{
		return HighlightDisplayLevel.ERROR;
	}

	@Override
	@Nonnull
	public String getGroupDisplayName()
	{
		return XmlInspectionGroupNames.HTML_INSPECTIONS;
	}

	@Override
	@Nonnull
	public String getDisplayName()
	{
		return InspectionsBundle.message("inspection.required.attributes.display.name");
	}

	@Override
	@Nonnull
	@NonNls
	public String getShortName()
	{
		return REQUIRED_ATTRIBUTES_SHORT_NAME;
	}

	@Override
	public String getAdditionalEntries()
	{
		return myAdditionalRequiredHtmlAttributes;
	}

	@Override
	public void addEntry(String text)
	{
		myAdditionalRequiredHtmlAttributes = appendName(getAdditionalEntries(), text);
	}

	@Override
	public boolean isEnabledByDefault()
	{
		return true;
	}

	public static IntentionAction getIntentionAction(String name)
	{
		return new AddHtmlTagOrAttributeToCustomsIntention(SHORT_NAME_KEY, name, XmlBundle.message("add.optional.html.attribute", name));
	}
}
