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

package consulo.xml.util.xml.tree;

import com.intellij.xml.XmlElementDescriptor;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.project.Project;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.awt.tree.SimpleNode;
import consulo.ui.ex.awt.tree.SimpleTree;
import consulo.util.dataholder.Key;
import consulo.util.lang.reflect.ReflectionUtil;
import consulo.xml.psi.xml.XmlFile;
import consulo.xml.psi.xml.XmlTag;
import consulo.xml.util.xml.*;
import consulo.xml.util.xml.highlighting.DomElementAnnotationsManager;
import consulo.xml.util.xml.highlighting.DomElementProblemDescriptor;
import consulo.xml.util.xml.highlighting.DomElementsProblemsHolder;
import consulo.xml.util.xml.reflect.DomCollectionChildDescription;
import consulo.xml.util.xml.reflect.DomFixedChildDescription;
import consulo.xml.util.xml.ui.TooltipUtils;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.lang.reflect.Type;
import java.util.*;

public class BaseDomElementNode extends AbstractDomElementNode {
  public static final Key<Comparator<AbstractDomElementNode>> COMPARATOR_KEY = Key.create("COMPARATOR_KEY");
  public static final Key<List<Class>> CONSOLIDATED_NODES_KEY = Key.create("CONSOLIDATED_NODES_KEY");
  public static final Key<List<Class>> FOLDER_NODES_KEY = Key.create("FOLDER_NODES_KEY");

  private final DomElement myRootDomElement;
  private final DomElement myDomElement;
  private final String myTagName;
  private final boolean folder;

  public BaseDomElementNode(final DomElement modelElement) {
    this(modelElement, modelElement, null);
  }

  public BaseDomElementNode(final DomElement modelElement, final DomElement modelRootElement, SimpleNode parent) {
    super(modelElement, parent);

    myDomElement = modelElement;
    myRootDomElement = modelRootElement;
    myTagName = modelElement.getXmlElementName();
    folder = isMarkedType(modelElement.getDomElementType(), FOLDER_NODES_KEY);
  }

  public SimpleNode[] getChildren() {
    return doGetChildren(myDomElement);
  }

  public void handleDoubleClickOrEnter(SimpleTree tree, InputEvent inputEvent) {
    if (inputEvent instanceof MouseEvent) {
      inputEvent.consume();
    }
    final DomElement domElement = getDomElement();
    if (domElement.isValid()) {
      final DomElementNavigationProvider provider = DomElementsNavigationManager.getManager(domElement.getManager().getProject())
        .getDomElementsNavigateProvider(DomElementsNavigationManager.DEFAULT_PROVIDER_NAME);

      provider.navigate(domElement, true);
    }
  }

  protected final SimpleNode[] doGetChildren(final DomElement element) {
    if (!element.isValid()) return NO_CHILDREN;

    List<SimpleNode> children = new ArrayList<SimpleNode>();
    final XmlTag tag = element.getXmlTag();
    
    if (tag != null && !(tag.getContainingFile() instanceof XmlFile)) return NO_CHILDREN;
    final XmlElementDescriptor xmlElementDescriptor = tag == null ? null : tag.getDescriptor();
    final XmlElementDescriptor[] xmlDescriptors = xmlElementDescriptor == null ? null : xmlElementDescriptor.getElementsDescriptors(tag);

    for (DomFixedChildDescription description : element.getGenericInfo().getFixedChildrenDescriptions()) {
      String childName = description.getXmlElementName();
      if (xmlDescriptors != null) {
        if (findDescriptor(xmlDescriptors, childName) == -1) continue;
      }
      final List<? extends DomElement> values = description.getStableValues(element);
      if (shouldBeShown(description.getType())) {
        if (DomUtil.isGenericValueType(description.getType())) {
          for (DomElement value : values) {
            children.add(new GenericValueNode((GenericDomValue)value, this));
          }
        }
        else {
          for (DomElement domElement : values) {
            children.add(new BaseDomElementNode(domElement, myRootDomElement, this));
          }
        }
      }
    }

    for (DomCollectionChildDescription description : element.getGenericInfo().getCollectionChildrenDescriptions()) {
      if (shouldBeShown(description.getType())) {
        DomElementsGroupNode groupNode = new DomElementsGroupNode(element, description, this, myRootDomElement);
        if (isMarkedType(description.getType(), CONSOLIDATED_NODES_KEY)) {
          Collections.addAll(children, groupNode.getChildren());
        }
        else {
          children.add(groupNode);
        }
      }
    }

    AbstractDomElementNode[] childrenNodes = children.toArray(new AbstractDomElementNode[children.size()]);

    Comparator<AbstractDomElementNode> comparator = DomUtil.getFile(myDomElement).getUserData(COMPARATOR_KEY);
    if (comparator == null) {
      comparator = getDefaultComparator(element);
    }
    if (comparator != null) {
      Arrays.sort(childrenNodes, comparator);
    }

    return childrenNodes;
  }

  @Nullable
  protected Comparator<AbstractDomElementNode> getDefaultComparator(DomElement element) {
    final XmlTag tag = element.getXmlTag();
    if (tag != null) {
      final XmlElementDescriptor descriptor = tag.getDescriptor();
      if (descriptor != null) {
        final XmlElementDescriptor[] childDescriptors = descriptor.getElementsDescriptors(tag);
        if (childDescriptors != null && childDescriptors.length > 1) {
          return new Comparator<AbstractDomElementNode>() {
            public int compare(final AbstractDomElementNode o1, final AbstractDomElementNode o2) {
              return findDescriptor(childDescriptors, o1.getTagName()) - findDescriptor(childDescriptors, o2.getTagName());
            }
          };
        }
      }
    }
    return null;
  }

  protected static int findDescriptor(XmlElementDescriptor[] descriptors, String name) {
    for (int i = 0; i < descriptors.length; i++) {
      if (descriptors[i].getDefaultName().equals(name)) {
        return i;
      }
    }
    return -1;
  }

  @Nonnull
  public List<DomCollectionChildDescription> getConsolidatedChildrenDescriptions() {
    if (!myDomElement.isValid()) return Collections.emptyList();

    final List<DomCollectionChildDescription> consolidated = new ArrayList<DomCollectionChildDescription>();
    for (DomCollectionChildDescription description : myDomElement.getGenericInfo().getCollectionChildrenDescriptions()) {
      if (isMarkedType(description.getType(), CONSOLIDATED_NODES_KEY)) {
        consolidated.add(description);
      }
    }
    return consolidated;
  }

  @Nonnull
  public Object[] getEqualityObjects() {
    return new Object[]{myDomElement};
  }

  protected void doUpdate() {
    if (!myDomElement.isValid()) return;
    final Project project = myDomElement.getManager().getProject();
    if (project.isDisposed()) return;

    setIcon(getNodeIcon());
    clearColoredText();

    final DomElementAnnotationsManager manager = DomElementAnnotationsManager.getInstance(project);
    final DomElementsProblemsHolder holder = manager.getCachedProblemHolder(myDomElement);
    final List<DomElementProblemDescriptor> problems =
      holder.getProblems(myDomElement, highlightIfChildrenHaveProblems(), HighlightSeverity.ERROR);

    if (problems.size() > 0) {
      final String toolTip = TooltipUtils.getTooltipText(problems);
      addColoredFragment(getNodeName(), toolTip, getWavedAttributes(SimpleTextAttributes.STYLE_PLAIN));
      if (isShowContainingFileInfo()) {
        addColoredFragment(" (" + DomUtil.getFile(myDomElement).getName() + ")", toolTip, SimpleTextAttributes.GRAY_ATTRIBUTES);
      }
    }
    else if (myDomElement.getXmlTag() == null && !(myDomElement instanceof DomFileElement)) {
      addColoredFragment(getNodeName(), folder ? SimpleTextAttributes.GRAYED_BOLD_ATTRIBUTES : SimpleTextAttributes.GRAYED_ATTRIBUTES);
    }
    else if (folder) {
      addColoredFragment(getNodeName(), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
      final int childrenCount = getChildren().length;
      addColoredFragment(" (" + childrenCount + ')', SimpleTextAttributes.GRAY_ATTRIBUTES);
    }
    else {
      addColoredFragment(getNodeName(), SimpleTextAttributes.REGULAR_ATTRIBUTES);

      if (isShowContainingFileInfo()) {
        addColoredFragment(" (" + DomUtil.getFile(myDomElement).getName() + ")", SimpleTextAttributes.GRAY_ATTRIBUTES);
      }
    }
  }

  protected boolean isMarkedType(Type type, Key<List<Class>> key) {
    if (type == null) {
      return false;
    }
    final List<Class> classes = DomUtil.getFile(getDomElement()).getUserData(key);
    if (classes != null) {
      Class clazz = ReflectionUtil.getRawType(type);
      return classes.contains(clazz);
    }
    return false;
  }

  protected boolean highlightIfChildrenHaveProblems() {
    return true;
  }

  public String getNodeName() {
    if (!myDomElement.isValid()) return "";

    final String name = myDomElement.getPresentation().getElementName();
    return name != null && name.trim().length() > 0 ? name : getPropertyName();
  }

  public String getTagName() {
    return myTagName;
  }

  public DomElement getDomElement() {
    return myDomElement;
  }

  public boolean isAutoExpandNode() {
    return getParent() == null;
  }

  public boolean expandOnDoubleClick() {
    return true;
  }

  public boolean isShowContainingFileInfo() {
    if (!myRootDomElement.isValid()) return false;
    DomElement root = myRootDomElement;
    while (root instanceof StableElement) {
      root = ((StableElement<DomElement>) root).getWrappedElement();
    }
    return root instanceof MergedObject && ((MergedObject)root).getImplementations().size() > 1;
  }
}
