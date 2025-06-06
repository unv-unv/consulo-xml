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

package org.intellij.plugins.relaxNG.validation;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.ApplicationManager;
import consulo.document.Document;
import consulo.language.Language;
import consulo.language.ast.ASTNode;
import consulo.language.editor.annotation.AnnotationHolder;
import consulo.language.editor.annotation.ExternalAnnotator;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.language.psi.*;
import consulo.language.psi.resolve.PsiElementProcessor;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.logging.Logger;
import consulo.util.lang.Comparing;
import consulo.util.lang.Pair;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import consulo.xml.ide.highlighter.XmlFileType;
import consulo.xml.psi.xml.*;
import org.intellij.plugins.relaxNG.ApplicationLoader;
import org.intellij.plugins.relaxNG.compact.RncFileType;
import org.intellij.plugins.relaxNG.compact.RngCompactLanguage;
import org.intellij.plugins.relaxNG.compact.psi.RncFile;
import org.intellij.plugins.relaxNG.model.resolve.RelaxIncludeIndex;
import org.jetbrains.annotations.NonNls;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * User: sweinreuter
 * Date: 18.07.2007
 */
@ExtensionImpl
public class RngSchemaValidator extends ExternalAnnotator<RngSchemaValidator.MyValidationMessageConsumer, RngSchemaValidator.MyValidationMessageConsumer>
{
	private static final Logger LOG = Logger.getInstance(RngSchemaValidator.class);

	@Nullable
	@Override
	public MyValidationMessageConsumer collectInformation(@Nonnull final PsiFile file)
	{
		final FileType type = file.getFileType();
		if(type != XmlFileType.INSTANCE && type != RncFileType.getInstance())
		{
			return null;
		}
		final XmlFile xmlfile = (XmlFile) file;
		final XmlDocument document = xmlfile.getDocument();
		if(document == null)
		{
			return null;
		}
		if(type == XmlFileType.INSTANCE)
		{
			final XmlTag rootTag = document.getRootTag();
			if(rootTag == null)
			{
				return null;
			}
			if(!ApplicationLoader.RNG_NAMESPACE.equals(rootTag.getNamespace()))
			{
				return null;
			}
		}
		else
		{
			if(!ApplicationManager.getApplication().isUnitTestMode() && MyErrorFinder.hasError(xmlfile))
			{
				return null;
			}
		}
		final Document doc = PsiDocumentManager.getInstance(file.getProject()).getDocument(file);

		final MyValidationMessageConsumer consumer = new MyValidationMessageConsumer();
		ErrorHandler eh = new DefaultHandler()
		{
			@Override
			public void warning(SAXParseException e)
			{
				handleError(e, file, doc, consumer.warning());
			}

			@Override
			public void error(SAXParseException e)
			{
				handleError(e, file, doc, consumer.error());
			}
		};

		RngParser.parsePattern(file, eh, true);
		return consumer;
	}

	@Nullable
	@Override
	public MyValidationMessageConsumer doAnnotate(MyValidationMessageConsumer collectedInfo)
	{
		return collectedInfo;
	}

	@Override
	public void apply(@Nonnull PsiFile file, MyValidationMessageConsumer annotationResult, @Nonnull AnnotationHolder holder)
	{
		annotationResult.apply(holder);
	}

	static class MyValidationMessageConsumer
	{
		final List<Pair<PsiElement, String>> errors = new ArrayList<>();
		final List<Pair<PsiElement, String>> warnings = new ArrayList<>();

		ValidationMessageConsumer error()
		{
			return new ValidationMessageConsumer()
			{
				@Override
				public void onMessage(PsiElement context, String message)
				{
					errors.add(Pair.create(context, message));
				}
			};
		}

		ValidationMessageConsumer warning()
		{
			return new ValidationMessageConsumer()
			{
				@Override
				public void onMessage(PsiElement context, String message)
				{
					warnings.add(Pair.create(context, message));
				}
			};
		}

		void apply(AnnotationHolder holder)
		{
			MessageConsumerImpl errorc = new ErrorMessageConsumer(holder);
			MessageConsumerImpl warningc = new WarningMessageConsumer(holder);
			for(Pair<PsiElement, String> error : errors)
			{
				errorc.onMessage(error.first, error.second);
			}
			for(Pair<PsiElement, String> warning : warnings)
			{
				warningc.onMessage(warning.first, warning.second);
			}
		}
	}

	public static void handleError(SAXParseException ex, PsiFile file, Document document, ValidationMessageConsumer consumer)
	{
		final String systemId = ex.getSystemId();
		if(LOG.isDebugEnabled())
		{
			LOG.debug("RNG Schema error: " + ex.getMessage() + " [" + systemId + "]");
		}

		if(systemId != null)
		{
			final VirtualFile virtualFile = findVirtualFile(systemId);
			if(!Comparing.equal(virtualFile, file.getVirtualFile()))
			{
				return;
			}
		}

		final PsiElement at;
		final int line = ex.getLineNumber();
		if(line > 0)
		{
			final int column = ex.getColumnNumber();
			final int startOffset = document.getLineStartOffset(line - 1);

			if(column > 0)
			{
				if(file.getFileType() == RncFileType.getInstance())
				{
					final PsiElement e = file.findElementAt(startOffset + column);
					if(e == null)
					{
						at = e;
					}
					else
					{
						at = file.findElementAt(startOffset + column - 1);
					}
				}
				else
				{
					at = file.findElementAt(startOffset + column - 2);
				}
			}
			else
			{
				final PsiElement e = file.findElementAt(startOffset);
				at = e != null ? PsiTreeUtil.nextLeaf(e) : null;
			}
		}
		else
		{
			final XmlDocument d = ((XmlFile) file).getDocument();
			assert d != null;
			final XmlTag rootTag = d.getRootTag();
			assert rootTag != null;
			at = rootTag.getFirstChild();
		}

		final PsiElement host;
		if(file instanceof RncFile)
		{
			host = at;
		}
		else
		{
			host = PsiTreeUtil.getParentOfType(at, XmlAttribute.class, XmlTag.class);
		}
		if(at != null && host != null)
		{
			consumer.onMessage(host, ex.getMessage());
		}
		else
		{
			consumer.onMessage(file, ex.getMessage());
		}
	}

	public static VirtualFile findVirtualFile(String systemId)
	{
		try
		{
			return VirtualFileUtil.findRelativeFile(systemId, null);
		}
		catch(Exception e)
		{
			LOG.warn("Failed to build file from uri <" + systemId + ">", e);
			return VirtualFileManager.getInstance().findFileByUrl(VirtualFileUtil.fixURLforIDEA(systemId));
		}
	}

	@Nonnull
	@Override
	public Language getLanguage()
	{
		return RngCompactLanguage.INSTANCE;
	}

	public interface ValidationMessageConsumer
	{
		void onMessage(PsiElement context, String message);
	}

	private abstract static class MessageConsumerImpl implements ValidationMessageConsumer
	{
		protected final AnnotationHolder myHolder;

		public MessageConsumerImpl(AnnotationHolder holder)
		{
			myHolder = holder;
		}

		@Override
		public void onMessage(PsiElement host, String message)
		{
			final ASTNode node = host.getNode();
			assert node != null;

			if(host instanceof XmlAttribute)
			{
				final ASTNode nameNode = XmlChildRole.ATTRIBUTE_NAME_FINDER.findChild(node);
				createAnnotation(nameNode, message);
			}
			else if(host instanceof XmlTag)
			{
				final ASTNode start = XmlChildRole.START_TAG_NAME_FINDER.findChild(node);
				if(start != null)
				{
					createAnnotation(start, message);
				}

				final ASTNode end = XmlChildRole.CLOSING_TAG_NAME_FINDER.findChild(node);
				if(end != null)
				{
					createAnnotation(end, message);
				}
			}
			else
			{
				createAnnotation(node, message);
			}
		}

		protected abstract void createAnnotation(ASTNode node, String message);
	}

	private static class ErrorMessageConsumer extends MessageConsumerImpl
	{
		@NonNls
		private static final String MISSING_START_ELEMENT = "missing \"start\" element";
		private static final String UNDEFINED_PATTERN = "reference to undefined pattern ";

		public ErrorMessageConsumer(AnnotationHolder holder)
		{
			super(holder);
		}

		@Override
		protected void createAnnotation(ASTNode node, String message)
		{
			if (MISSING_START_ELEMENT.equals(message))
			{
				final PsiFile psiFile = node.getPsi().getContainingFile();
				if (psiFile instanceof XmlFile xmlFile)
				{
					final PsiElementProcessor.FindElement<XmlFile> processor = new PsiElementProcessor.FindElement<>();
					RelaxIncludeIndex.processBackwardDependencies(xmlFile, processor);
					if (processor.isFound())
					{
						// files that are included from other files do not need a <start> element.
						myHolder.newAnnotation(HighlightSeverity.WEAK_WARNING, message)
							.range(node);
						return;
					}
				}
			}
			else if (message != null && message.startsWith(UNDEFINED_PATTERN))
			{
				// we've got our own validation for that
				return;
			}
			myHolder.newAnnotation(HighlightSeverity.ERROR, message).range(node);
		}
	}

	private static class WarningMessageConsumer extends MessageConsumerImpl
	{

		public WarningMessageConsumer(AnnotationHolder holder)
		{
			super(holder);
		}

		@Override
		protected void createAnnotation(ASTNode node, String message)
		{
			myHolder.newAnnotation(HighlightSeverity.WARNING, message).range(node);
		}
	}

	private static class MyErrorFinder extends PsiRecursiveElementVisitor
	{
		private static final MyErrorFinder INSTANCE = new MyErrorFinder();

		private static final class HasError extends RuntimeException
		{
		}

		private static final HasError FOUND = new HasError();

		@Override
		public void visitErrorElement(PsiErrorElement element)
		{
			throw FOUND;
		}

		public static boolean hasError(PsiElement element)
		{
			try
			{
				element.accept(INSTANCE);
				return false;
			}
			catch(HasError e)
			{
				return true;
			}
		}
	}

}
