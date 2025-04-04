/*
 * Copyright 2006 Sascha Weinreuter
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
package consulo.xml.intelliLang.inject.config.ui;

import consulo.language.editor.ui.awt.EditorTextField;
import consulo.language.editor.ui.awt.LanguageTextField;
import consulo.language.inject.advanced.ui.AbstractInjectionPanel;
import consulo.project.Project;
import consulo.ui.ex.awt.ComboBox;
import consulo.util.dataholder.Key;
import consulo.xml.intelliLang.inject.config.AbstractTagInjection;
import consulo.xml.intelliLang.inject.config.XmlTagInjection;
import consulo.xml.javaee.ExternalResourceManager;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TagPanel extends AbstractInjectionPanel<AbstractTagInjection>
{
	public static final Key<List<String>> URI_MODEL = Key.create("URI_MODEL");

	private JPanel myRoot;

	private EditorTextField myLocalName;
	private ComboBox myNamespace;
	private JCheckBox myApplyRecursivelyCheckBox;

	public TagPanel(Project project, AbstractTagInjection injection)
	{
		super(injection, project);

		myNamespace.setModel(createNamespaceUriModel(getProject()));
		myLocalName.getDocument().addDocumentListener(new TreeUpdateListener());
	}

	public static ComboBoxModel createNamespaceUriModel(Project project)
	{
		final List<String> data = project.getUserData(URI_MODEL);
		if(data != null)
		{
			return new DefaultComboBoxModel(data.toArray());
		}

		final List<String> urls = new ArrayList<String>(Arrays.asList(ExternalResourceManager.getInstance().getResourceUrls(null, true)));
		Collections.sort(urls);

//		final JspSupportProxy jspSupport = JspSupportProxy.getInstance();
//		if(jspSupport != null)
//		{
//			final List<String> tlds = new ArrayList<String>();
//			final Module[] modules = ModuleManager.getInstance(project).getModules();
//			for(final Module module : modules)
//			{
//				final String[] tldUris = ApplicationManager.getApplication().runReadAction(new Computable<String[]>()
//				{
//					@Override
//					public String[] compute()
//					{
//						return jspSupport.getPossibleTldUris(module);
//					}
//				});
//				for(String uri : tldUris)
//				{
//					if(!tlds.contains(uri))
//					{
//						tlds.add(uri);
//					}
//				}
//			}
//			Collections.sort(tlds);
//
//			// TLD URIs are intentionally kept above the other URIs to make it easier to find them
//			urls.addAll(0, tlds);
//		}

		project.putUserData(URI_MODEL, urls);
		return new DefaultComboBoxModel(urls.toArray());
	}

	@Override
	public JPanel getComponent()
	{
		return myRoot;
	}

	@Override
	protected void resetImpl()
	{
		myLocalName.setText(getOrigInjection().getTagName());
		myNamespace.getEditor().setItem(getOrigInjection().getTagNamespace());
		final boolean isXmlTag = getOrigInjection() instanceof XmlTagInjection;
		myApplyRecursivelyCheckBox.setVisible(isXmlTag);
		if(isXmlTag)
		{
			myApplyRecursivelyCheckBox.setSelected(((XmlTagInjection) getOrigInjection()).isApplyToSubTagTexts());
		}
	}

	@Override
	protected void apply(AbstractTagInjection i)
	{
		i.setTagName(myLocalName.getText());
		i.setTagNamespace(getNamespace());
		if(i instanceof XmlTagInjection)
		{
			((XmlTagInjection) i).setApplyToSubTagTexts(myApplyRecursivelyCheckBox.isSelected());
		}
	}

	private String getNamespace()
	{
		final String s = (String) myNamespace.getEditor().getItem();
		return s != null ? s : "";
	}

	private void createUIComponents()
	{
		myLocalName = new LanguageTextField(RegExpLanguageDelegate.RegExp.get(), getProject(), getOrigInjection().getTagName());
		myNamespace = new ComboBox(200);
	}
}

