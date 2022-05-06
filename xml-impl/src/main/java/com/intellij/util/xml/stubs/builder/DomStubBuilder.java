/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.util.xml.stubs.builder;

import com.intellij.ide.highlighter.XmlFileType;
import consulo.component.extension.Extensions;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.psi.stub.FileContent;
import consulo.project.Project;
import consulo.util.xml.fastReader.XmlFileHeader;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.stub.BinaryFileStubBuilder;
import consulo.language.psi.stub.Stub;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import consulo.ide.impl.idea.util.indexing.FileBasedIndexImpl;
import com.intellij.util.xml.*;
import com.intellij.util.xml.impl.DomManagerImpl;
import com.intellij.util.xml.stubs.FileStub;
import com.intellij.xml.util.XmlUtil;
import consulo.logging.Logger;
import consulo.util.dataholder.Key;

/**
 * @author Dmitry Avdeev
 *         Date: 8/2/12
 */
public class DomStubBuilder implements BinaryFileStubBuilder
{

	public final static Key<FileContent> CONTENT_FOR_DOM_STUBS = Key.create("dom stubs content");
	private final static Logger LOG = Logger.getInstance(DomStubBuilder.class);

	@Override
	public boolean acceptsFile(VirtualFile file)
	{
		FileType fileType = file.getFileType();
		return fileType == XmlFileType.INSTANCE && !FileBasedIndexImpl.isProjectOrWorkspaceFile(file, fileType);
	}

	@Override
	public Stub buildStubTree(FileContent fileContent)
	{
		VirtualFile file = fileContent.getFile();
		Project project = fileContent.getProject();
		PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
		if(!(psiFile instanceof XmlFile))
		{
			return null;
		}

		XmlFile xmlFile = (XmlFile) psiFile;
		try
		{
			XmlUtil.BUILDING_DOM_STUBS.set(Boolean.TRUE);
			psiFile.putUserData(CONTENT_FOR_DOM_STUBS, fileContent);
			DomFileElement<? extends DomElement> fileElement = DomManager.getDomManager(project).getFileElement(xmlFile);
			if(fileElement == null || !fileElement.getFileDescription().hasStubs())
			{
				return null;
			}

			XmlFileHeader header = DomService.getInstance().getXmlFileHeader(xmlFile);
			if(header.getRootTagLocalName() == null)
			{
				LOG.error("null root tag for " + fileElement + " for " + file);
			}
			FileStub fileStub = new FileStub(header);
			XmlTag rootTag = xmlFile.getRootTag();
			if(rootTag != null)
			{
				new DomStubBuilderVisitor(DomManagerImpl.getDomManager(project)).visitXmlElement(rootTag, fileStub, 0);
			}
			return fileStub;
		}
		finally
		{
			XmlUtil.BUILDING_DOM_STUBS.set(Boolean.FALSE);
			psiFile.putUserData(CONTENT_FOR_DOM_STUBS, null);
		}
	}

	@Override
	public int getStubVersion()
	{
		int version = 11;
		DomFileDescription[] descriptions = Extensions.getExtensions(DomFileDescription.EP_NAME);
		for(DomFileDescription description : descriptions)
		{
			version += description.getStubVersion();
		}
		return version;
	}
}
