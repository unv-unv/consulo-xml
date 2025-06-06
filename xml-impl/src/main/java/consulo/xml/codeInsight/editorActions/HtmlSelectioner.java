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

/*
 * Created by IntelliJ IDEA.
 * User: mike
 * Date: Jul 18, 2002
 * Time: 10:30:17 PM
 * To change template for new class use
 * Code Style | Class Templates options (Tools | IDE Options).
 */
package consulo.xml.codeInsight.editorActions;

import com.intellij.xml.util.HtmlUtil;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorHighlighter;
import consulo.codeEditor.HighlighterIterator;
import consulo.document.util.TextRange;
import consulo.document.util.UnfairTextRange;
import consulo.language.Language;
import consulo.language.editor.action.AbstractWordSelectioner;
import consulo.language.editor.action.BraceMatchingUtil;
import consulo.language.editor.highlight.HighlighterFactory;
import consulo.language.file.FileViewProvider;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.xml.psi.xml.*;

import java.util.ArrayList;
import java.util.List;

@ExtensionImpl
public class HtmlSelectioner extends AbstractWordSelectioner {
    @Override
    public boolean canSelect(PsiElement e) {
        return canSelectElement(e);
    }

    static boolean canSelectElement(PsiElement e) {
        return e instanceof XmlToken && HtmlUtil.hasHtml(e.getContainingFile());
    }

    @Override
    @RequiredReadAction
    public List<TextRange> select(PsiElement e, CharSequence editorText, int cursorOffset, Editor editor) {
        List<TextRange> result;

        if (e instanceof XmlToken token
            && !XmlTokenSelectioner.shouldSelectToken(token)
            && token.getTokenType() != XmlTokenType.XML_DATA_CHARACTERS) {
            result = new ArrayList<>();
        }
        else {
            result = super.select(e, editorText, cursorOffset, editor);
        }

        PsiElement parent = e.getParent();
        if (parent instanceof XmlComment) {
            result.addAll(expandToWholeLine(editorText, parent.getTextRange(), true));
        }

        PsiFile psiFile = e.getContainingFile();
        FileType fileType = psiFile.getVirtualFile().getFileType();

        addAttributeSelection(result, e);
        FileViewProvider fileViewProvider = psiFile.getViewProvider();
        for (Language lang : fileViewProvider.getLanguages()) {
            PsiFile langFile = fileViewProvider.getPsi(lang);
            if (langFile != psiFile) {
                addAttributeSelection(result, fileViewProvider.findElementAt(cursorOffset, lang));
            }
        }

        EditorHighlighter highlighter = HighlighterFactory.createHighlighter(e.getProject(), psiFile.getVirtualFile());
        highlighter.setText(editorText);

        addTagSelection(editorText, cursorOffset, fileType, highlighter, result);

        return result;
    }

    private static void addTagSelection(
        CharSequence editorText,
        int cursorOffset,
        FileType fileType,
        EditorHighlighter highlighter,
        List<TextRange> result
    ) {
        int start = cursorOffset;

        while (true) {
            if (start < 0) {
                return;
            }
            HighlighterIterator i = highlighter.createIterator(start);
            if (i.atEnd()) {
                return;
            }

            while (true) {
                if (i.getTokenType() == XmlTokenType.XML_START_TAG_START) {
                    break;
                }
                i.retreat();
                if (i.atEnd()) {
                    return;
                }
            }

            start = i.getStart();
            boolean matched = BraceMatchingUtil.matchBrace(editorText, fileType, i, true);

            if (matched) {
                int tagEnd = i.getEnd();
                result.add(new TextRange(start, tagEnd));

                HighlighterIterator j = highlighter.createIterator(start);
                while (!j.atEnd() && j.getTokenType() != XmlTokenType.XML_TAG_END) j.advance();
                while (!i.atEnd() && i.getTokenType() != XmlTokenType.XML_END_TAG_START) i.retreat();

                if (!i.atEnd() && !j.atEnd()) {
                    result.add(new UnfairTextRange(j.getEnd(), i.getStart()));
                }
                if (!j.atEnd()) {
                    result.add(new TextRange(start, j.getEnd()));
                }
                if (!i.atEnd()) {
                    result.add(new TextRange(i.getStart(), tagEnd));
                }
            }

            start--;
        }
    }

    @RequiredReadAction
    private static void addAttributeSelection(List<TextRange> result, PsiElement e) {
        XmlAttribute attribute = PsiTreeUtil.getParentOfType(e, XmlAttribute.class);

        if (attribute != null) {
            result.add(attribute.getTextRange());
            XmlAttributeValue value = attribute.getValueElement();

            if (value != null) {
                TextRange range = value.getTextRange();
                result.add(range);
                if (value.getFirstChild() != null
                    && value.getFirstChild().getNode().getElementType() == XmlTokenType.XML_ATTRIBUTE_VALUE_START_DELIMITER) {
                    result.add(new TextRange(range.getStartOffset() + 1, range.getEndOffset() - 1));
                }
            }
        }
    }
}
