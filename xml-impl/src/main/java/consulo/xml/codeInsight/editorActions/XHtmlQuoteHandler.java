package consulo.xml.codeInsight.editorActions;

import consulo.annotation.component.ExtensionImpl;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.xml.ide.highlighter.XHtmlFileType;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 02-Aug-22
 */
@ExtensionImpl
public class XHtmlQuoteHandler extends HtmlQuoteHandler
{
	@Nonnull
	@Override
	public FileType getFileType()
	{
		return XHtmlFileType.INSTANCE;
	}
}
