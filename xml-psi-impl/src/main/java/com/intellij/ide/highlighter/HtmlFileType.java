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
package com.intellij.ide.highlighter;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.jetbrains.annotations.NonNls;
import com.intellij.icons.AllIcons;
import com.intellij.ide.IdeBundle;
import com.intellij.lang.Language;
import com.intellij.lang.html.HTMLLanguage;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.text.XmlCharsetDetector;
import com.intellij.xml.util.HtmlUtil;
import consulo.ui.image.Image;

public class HtmlFileType extends XmlLikeFileType
{
	@NonNls
	public static final String DOT_DEFAULT_EXTENSION = ".html";

	public final static HtmlFileType INSTANCE = new HtmlFileType();

	private HtmlFileType()
	{
		super(HTMLLanguage.INSTANCE);
	}

	HtmlFileType(Language language)
	{
		super(language);
	}

	@Nonnull
	public String getId()
	{
		return "HTML";
	}

	@Nonnull
	public String getDescription()
	{
		return IdeBundle.message("filetype.description.html");
	}

	@Nonnull
	public String getDefaultExtension()
	{
		return "html";
	}

	public Image getIcon()
	{
		return AllIcons.FileTypes.Html;
	}

	public String getCharset(@Nonnull final VirtualFile file, final byte[] content)
	{
		String charset = XmlCharsetDetector.extractXmlEncodingFromProlog(content);
		if(charset != null)
		{
			return charset;
		}
		@NonNls String strContent;
		try
		{
			strContent = new String(content, "ISO-8859-1");
		}
		catch(UnsupportedEncodingException e)
		{
			return null;
		}
		Charset c = HtmlUtil.detectCharsetFromMetaTag(strContent);
		return c == null ? null : c.name();
	}

	public Charset extractCharsetFromFileContent(@Nullable final Project project, @Nullable final VirtualFile file, @Nonnull final CharSequence content)
	{
		String name = XmlCharsetDetector.extractXmlEncodingFromProlog(content);
		Charset charset = CharsetToolkit.forName(name);

		if(charset != null)
		{
			return charset;
		}
		return HtmlUtil.detectCharsetFromMetaTag(content);
	}
}
