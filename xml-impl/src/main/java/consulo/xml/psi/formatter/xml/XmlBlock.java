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
package consulo.xml.psi.formatter.xml;

import consulo.language.ast.ASTNode;
import consulo.xml.lang.xml.XMLLanguage;
import consulo.language.ast.TokenType;
import consulo.language.codeStyle.Alignment;
import consulo.language.codeStyle.Block;
import consulo.language.codeStyle.ChildAttributes;
import consulo.language.codeStyle.Indent;
import consulo.language.codeStyle.Spacing;
import consulo.language.impl.psi.SourceTreeToPsiMap;
import consulo.logging.Logger;
import consulo.document.util.TextRange;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiWhiteSpace;
import consulo.language.codeStyle.AbstractBlock;
import consulo.language.psi.OuterLanguageElement;
import consulo.language.ast.IElementType;
import consulo.xml.psi.xml.XmlDocument;
import consulo.xml.psi.xml.XmlElementType;
import consulo.xml.psi.xml.XmlTag;
import consulo.util.collection.SmartList;
import consulo.language.codeStyle.Wrap;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class XmlBlock extends AbstractXmlBlock {
  private final Indent myIndent;
  private final TextRange myTextRange;

  private static final Logger LOG = Logger.getInstance(XmlBlock.class);

  public XmlBlock(final ASTNode node,
                    final Wrap wrap,
                    final Alignment alignment,
                    final XmlFormattingPolicy policy,
                    final Indent indent,
                    final TextRange textRange) {
    this(node, wrap, alignment, policy, indent, textRange, false);
  }

  public XmlBlock(final ASTNode node,
                  final Wrap wrap,
                  final Alignment alignment,
                  final XmlFormattingPolicy policy,
                  final Indent indent,
                  final TextRange textRange,
                  final boolean preserveSpace) {
    super(node, wrap, alignment, policy, preserveSpace);
    myIndent = indent;
    myTextRange = textRange;
  }

  @Nonnull
  public TextRange getTextRange() {
    if (myTextRange != null && !(isCDATAStart() || isCDATAEnd())) {
      return myTextRange;
    }
    else {
      return super.getTextRange();
    }
  }

  protected List<Block> buildChildren() {

    //
    // Fix for EA-19269:
    // Split XML attribute value to the value itself and delimiters (needed for the case when it contains
    // template language tags inside).
    //
    if (myNode.getElementType() == XmlElementType.XML_ATTRIBUTE_VALUE) {
      return splitAttribute(myNode, myXmlFormattingPolicy);
    }

    if (myNode.getElementType() == XmlElementType.XML_COMMENT) {
      List<Block> result = new SmartList<Block>();
      if (buildInjectedPsiBlocks(result, myNode, myWrap, null, Indent.getNoneIndent())) {
        return result;
      }
      return splitComment();
    } 

    if (myNode.getFirstChildNode() != null) {
      boolean keepWhitespaces = shouldKeepWhitespaces();
      final ArrayList<Block> result = new ArrayList<Block>(5);
      ASTNode child = myNode.getFirstChildNode();
      while (child != null) {
        if (child.getTextLength() > 0) {
          if (containsWhiteSpacesOnly(child)) {
            if (keepWhitespaces) {
              result.add(new ReadOnlyBlock(child));
            }
          }
          else {
            child = processChild(result, child, getDefaultWrap(child), null, getChildDefaultIndent());
          }
        }
        if (child != null) {
          LOG.assertTrue(child.getTreeParent() == myNode);
          child = child.getTreeNext();
        }
      }
      return result;
    }
    else {
      return EMPTY;
    }
  }

  private boolean shouldKeepWhitespaces() {
    if (myNode.getElementType() == XmlElementType.XML_TEXT) {
      if (myXmlFormattingPolicy.getShouldKeepWhiteSpaces()) {
        return true;
      }
      else {
        final ASTNode treeParent = myNode.getTreeParent();
        final XmlTag tag = getTag(treeParent);
        if (tag != null) {
          if (myXmlFormattingPolicy.keepWhiteSpacesInsideTag(tag)) {
            return true;
          }
        }
      }
    }
    return false;
  }


  private List<Block> splitAttribute(ASTNode node, XmlFormattingPolicy formattingPolicy) {
    final ArrayList<Block> result = new ArrayList<Block>(3);
    ASTNode child = node.getFirstChildNode();
    while (child != null) {
      if (child.getElementType() == XmlElementType.XML_ATTRIBUTE_VALUE_START_DELIMITER ||
          child.getElementType() == XmlElementType.XML_ATTRIBUTE_VALUE_END_DELIMITER) {
        result.add(new XmlBlock(child, null, null, formattingPolicy, null, null, isPreserveSpace()));
      }
      else if (!child.getPsi().getLanguage().isKindOf(XMLLanguage.INSTANCE) && containsOuterLanguageElement(child)) {
        // Fix for EA-20311:
        // In case of another embedded language create a splittable XML block which can be
        // merged with other language's code blocks.
        createLeafBlocks(child, result);
      }
      else if (child.getElementType() != TokenType.ERROR_ELEMENT || child.getFirstChildNode() != null) {
        result.add(new ReadOnlyBlock(child));
      }
      child = child.getTreeNext();
    }
    return result;
  }

  private void createLeafBlocks(ASTNode node, List<Block> result) {
    if (node instanceof OuterLanguageElement) {
      processChild(result, node, null, null, null);
      return;
    }

    ASTNode child = node.getFirstChildNode();
    if (child == null && !(node instanceof PsiWhiteSpace) && node.getElementType() != TokenType.ERROR_ELEMENT) {
      result.add(new ReadOnlyBlock(node));
      return;
    }
    while (child != null) {
      createLeafBlocks(child, result);
      child = child.getTreeNext();
    }
  }


  private static boolean containsOuterLanguageElement(ASTNode node) {
    if (node instanceof OuterLanguageElement) {
      return true;
    }
    ASTNode child = node.getFirstChildNode();
    while (child != null) {
      if (child instanceof OuterLanguageElement) {
        return true;
      }
      if (containsOuterLanguageElement(child)) return true;
      child = child.getTreeNext();
    }
    return false;
  }


  private List<Block> splitComment() {
    if (myNode.getElementType() != XmlElementType.XML_COMMENT) return EMPTY;
    final ArrayList<Block> result = new ArrayList<Block>(3);
    ASTNode child = myNode.getFirstChildNode();
    boolean hasOuterLangElements = false;
    while (child != null) {
      if (child instanceof OuterLanguageElement) {
        hasOuterLangElements = true;
      }
      result.add(new XmlBlock(child, null, null, myXmlFormattingPolicy, getChildIndent(), null, isPreserveSpace()));
      child = child.getTreeNext();
    }
    if (hasOuterLangElements) {
      return result;
    }
    else {
      return EMPTY;
    }
  }

  protected @Nullable
  Wrap getDefaultWrap(ASTNode node) {
    return null;
  }

  @Nullable
  protected Indent getChildDefaultIndent() {
    if (myNode.getElementType() == XmlElementType.HTML_DOCUMENT) {
      return Indent.getNoneIndent();
    }
    if (myNode.getElementType() == TokenType.DUMMY_HOLDER) {
      return Indent.getNoneIndent();
    }
    if (myNode.getElementType() == XmlElementType.XML_PROLOG) {
      return Indent.getNoneIndent();
    }
    else {
      return null;
    }
  }

  public Spacing getSpacing(Block child1, @Nonnull Block child2) {
    if (!(child1 instanceof AbstractBlock) || !(child2 instanceof AbstractBlock)) {
      return null;
    }

    final IElementType elementType = myNode.getElementType();
    final ASTNode node1 = ((AbstractBlock)child1).getNode();
    final IElementType type1 = node1.getElementType();
    final ASTNode node2 = ((AbstractBlock)child2).getNode();
    final IElementType type2 = node2.getElementType();

    if ((isXmlTag(node2) || type2 == XmlElementType.XML_END_TAG_START || type2 == XmlElementType.XML_TEXT) && myXmlFormattingPolicy
      .getShouldKeepWhiteSpaces()) {
      return Spacing.getReadOnlySpacing();
    }

    if (elementType == XmlElementType.XML_TEXT) {
      return getSpacesInsideText(type1, type2);

    }
    else if (elementType == XmlElementType.XML_ATTRIBUTE) {
      return getSpacesInsideAttribute(type1, type2);
    }

    if (type1 == XmlElementType.XML_PROLOG) {
      return createDefaultSpace(true, false);
    }

    if (elementType == XmlElementType.XML_DOCTYPE) {
      return createDefaultSpace(true, false);
    }

    return createDefaultSpace(false, false);
  }

  private Spacing getSpacesInsideAttribute(final IElementType type1, final IElementType type2) {
    if (type1 == XmlElementType.XML_EQ || type2 == XmlElementType.XML_EQ) {
      int spaces = myXmlFormattingPolicy.getShouldAddSpaceAroundEqualityInAttribute() ? 1 : 0;
      return Spacing
        .createSpacing(spaces, spaces, 0, myXmlFormattingPolicy.getShouldKeepLineBreaks(), myXmlFormattingPolicy.getKeepBlankLines());
    }
    else {
      return createDefaultSpace(false, false);
    }
  }

  private Spacing getSpacesInsideText(final IElementType type1, final IElementType type2) {
    if (type1 == XmlElementType.XML_DATA_CHARACTERS && type2 == XmlElementType.XML_DATA_CHARACTERS) {
      return Spacing
        .createSpacing(1, 1, 0, myXmlFormattingPolicy.getShouldKeepLineBreaksInText(), myXmlFormattingPolicy.getKeepBlankLines());
    }
    else {
      return createDefaultSpace(false, true);
    }
  }

  public Indent getIndent() {
    if (myNode.getElementType() == XmlElementType.XML_PROLOG || myNode.getElementType() == XmlElementType.XML_DOCTYPE ||
        SourceTreeToPsiMap.treeElementToPsi(myNode) instanceof XmlDocument) {
      return Indent.getNoneIndent();
    }
    return myIndent;
  }

  public boolean insertLineBreakBeforeTag() {
    return false;
  }

  public boolean removeLineBreakBeforeTag() {
    return false;
  }

  public boolean isTextElement() {
    return myNode.getElementType() == XmlElementType.XML_TEXT || myNode.getElementType() == XmlElementType.XML_DATA_CHARACTERS ||
           myNode.getElementType() == XmlElementType.XML_CHAR_ENTITY_REF;
  }

  @Override
  @Nonnull
  public ChildAttributes getChildAttributes(final int newChildIndex) {
    if (myNode.getPsi() instanceof PsiFile) {
      return new ChildAttributes(Indent.getNoneIndent(), null);
    }
    else {
      return super.getChildAttributes(newChildIndex);
    }
  }

  public XmlFormattingPolicy getPolicy() {
    return myXmlFormattingPolicy;
  }
}
