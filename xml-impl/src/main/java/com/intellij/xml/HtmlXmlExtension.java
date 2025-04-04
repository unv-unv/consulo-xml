package com.intellij.xml;

import com.intellij.xml.util.HtmlUtil;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.psi.PsiFile;
import consulo.xml.ide.highlighter.HtmlFileType;
import consulo.xml.psi.xml.XmlDocument;

import jakarta.annotation.Nullable;

/**
 * @author Dmitry Avdeev
 * Date: 24.10.13
 */
@ExtensionImpl
public class HtmlXmlExtension extends DefaultXmlExtension {
    @Override
    public boolean isAvailable(PsiFile file) {
        return file.getFileType() == HtmlFileType.INSTANCE;
    }

    @Nullable
    @Override
    public String[][] getNamespacesFromDocument(XmlDocument parent, boolean declarationsExist) {
        String[][] namespaces = super.getNamespacesFromDocument(parent, false);
        if (namespaces == null || !HtmlUtil.isHtml5Document(parent)) {
            return namespaces;
        }

        for (String[] namespace : namespaces) {
            if ("xlink".equals(namespace[0])) {
                return namespaces;
            }
        }

        String[][] newNamespaces = new String[namespaces.length + 1][2];
        System.arraycopy(namespaces, 0, newNamespaces, 0, namespaces.length);
        newNamespaces[namespaces.length] = new String[]{
            "xlink",
            "http://www.w3.org/1999/xlink"
        };
        return newNamespaces;
    }
}
