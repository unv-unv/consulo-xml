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
package consulo.xml.javaee;

import com.intellij.xml.config.ConfigFileSearcher;
import com.intellij.xml.config.ConfigFilesTreeBuilder;
import com.intellij.xml.index.IndexedRelevantResource;
import com.intellij.xml.index.XmlNamespaceIndex;
import com.intellij.xml.index.XsdNamespaceBuilder;
import com.intellij.xml.util.XmlUtil;
import consulo.application.ApplicationPropertiesComponent;
import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.EditorFontType;
import consulo.dataContext.DataManager;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.ide.impl.idea.openapi.fileChooser.FileSystemTree;
import consulo.ide.impl.idea.openapi.fileChooser.FileSystemTreeFactory;
import consulo.language.editor.CommonDataKeys;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.util.ModuleUtilCore;
import consulo.module.Module;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.JBTabbedPane;
import consulo.ui.ex.awt.ScrollPaneFactory;
import consulo.ui.ex.awt.event.DocumentAdapter;
import consulo.ui.ex.awt.tree.ColoredTreeCellRenderer;
import consulo.ui.ex.awt.tree.TreeUtil;
import consulo.util.collection.ContainerUtil;
import consulo.util.io.FileUtil;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Dmitry Avdeev
 *         Date: 7/17/12
 */
public class MapExternalResourceDialog extends DialogWrapper
{

	private static final String MAP_EXTERNAL_RESOURCE_SELECTED_TAB = "map.external.resource.selected.tab";
	private JTextField myUri;
	private JPanel myMainPanel;
	private JTree mySchemasTree;
	private JPanel myExplorerPanel;
	private JBTabbedPane myTabs;
	private final FileSystemTree myExplorer;
	private String myLocation;

	public MapExternalResourceDialog(String uri, @Nonnull Project project, @Nullable PsiFile file, @Nullable String location)
	{
		super(project);
		setTitle("Map External Resource");
		myUri.setText(uri);

		DefaultMutableTreeNode root = new DefaultMutableTreeNode();
		mySchemasTree.setModel(new DefaultTreeModel(root));
		ConfigFileSearcher searcher = new ConfigFileSearcher(file == null ? null : ModuleUtilCore.findModuleForPsiElement(file), project)
		{
			@Override
			public Set<PsiFile> search(@Nullable Module module, @Nonnull Project project)
			{
				List<IndexedRelevantResource<String, XsdNamespaceBuilder>> resources = XmlNamespaceIndex.getAllResources(module, project, null);

				HashSet<PsiFile> files = new HashSet<PsiFile>();
				PsiManager psiManager = PsiManager.getInstance(project);
				for(IndexedRelevantResource<String, XsdNamespaceBuilder> resource : resources)
				{
					VirtualFile file = resource.getFile();
					PsiFile psiFile = psiManager.findFile(file);
					ContainerUtil.addIfNotNull(files, psiFile);
				}
				return files;
			}
		};
		searcher.search();
		new ConfigFilesTreeBuilder(mySchemasTree).buildTree(root, searcher);
		TreeUtil.expandAll(mySchemasTree);
		mySchemasTree.setRootVisible(false);
		mySchemasTree.setShowsRootHandles(true);

		ColoredTreeCellRenderer renderer = new ColoredTreeCellRenderer()
		{
			@RequiredUIAccess
			@Override
			public void customizeCellRenderer(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus)
			{
				ConfigFilesTreeBuilder.renderNode(value, expanded, this);
			}
		};
		renderer.setFont(EditorColorsManager.getInstance().getGlobalScheme().getFont(EditorFontType.PLAIN));

		mySchemasTree.setCellRenderer(renderer);
		MouseAdapter mouseAdapter = new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				if(e.getClickCount() > 1 && isOKActionEnabled())
				{
					doOKAction();
				}
			}
		};
		mySchemasTree.addMouseListener(mouseAdapter);

		myUri.getDocument().addDocumentListener(new DocumentAdapter()
		{
			@Override
			protected void textChanged(DocumentEvent e)
			{
				validateInput();
			}
		});
		mySchemasTree.getSelectionModel().addTreeSelectionListener(new TreeSelectionListener()
		{
			@Override
			public void valueChanged(TreeSelectionEvent e)
			{
				validateInput();
			}
		});

		myExplorer = FileSystemTreeFactory.getInstance().createFileSystemTree(project, new FileChooserDescriptor(true, false, false, false, true, false));

		myExplorer.addListener(new FileSystemTree.Listener()
		{
			@Override
			public void selectionChanged(List<? extends VirtualFile> selection)
			{
				validateInput();
			}
		}, myExplorer);
		myExplorer.getTree().addMouseListener(mouseAdapter);

		myExplorerPanel.add(ScrollPaneFactory.createScrollPane(myExplorer.getTree()), BorderLayout.CENTER);

		AnAction actionGroup = ActionManager.getInstance().getAction("FileChooserToolbar");
		ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.UNKNOWN, (ActionGroup) actionGroup, true);
		toolbar.setTargetComponent(myExplorerPanel);
		myExplorerPanel.add(toolbar.getComponent(), BorderLayout.NORTH);

		PsiFile schema = null;
		if(file != null)
		{
			schema = XmlUtil.findNamespaceByLocation(file, uri);
		}
		else if(location != null)
		{
			VirtualFile virtualFile = VirtualFileUtil.findRelativeFile(location, null);
			if(virtualFile != null)
			{
				schema = PsiManager.getInstance(project).findFile(virtualFile);
			}
		}

		if(schema != null)
		{
			DefaultMutableTreeNode node = TreeUtil.findNodeWithObject(root, schema);
			if(node != null)
			{
				TreeUtil.selectNode(mySchemasTree, node);
			}
			myExplorer.select(schema.getVirtualFile(), null);
		}

		int index = ApplicationPropertiesComponent.getInstance().getInt(MAP_EXTERNAL_RESOURCE_SELECTED_TAB, 0);
		myTabs.setSelectedIndex(index);
		myTabs.getModel().addChangeListener(new ChangeListener()
		{
			@Override
			public void stateChanged(ChangeEvent e)
			{
                ApplicationPropertiesComponent.getInstance().setValue(MAP_EXTERNAL_RESOURCE_SELECTED_TAB, Integer.toString(myTabs.getSelectedIndex()));
			}
		});
		init();
	}

	@Override
	protected void processDoNotAskOnOk(int exitCode)
	{
		super.processDoNotAskOnOk(exitCode);
		// store it since explorer will be disposed
		myLocation = getResourceLocation();
	}

	private void validateInput()
	{
		setOKActionEnabled(!StringUtil.isEmpty(myUri.getText()) && getResourceLocation() != null);
	}

	@Override
	protected JComponent createCenterPanel()
	{
		return myMainPanel;
	}

	@Override
	public JComponent getPreferredFocusedComponent()
	{
		return StringUtil.isEmpty(myUri.getText()) ? myUri : mySchemasTree;
	}

	public String getUri()
	{
		return myUri.getText();
	}

	@Nullable
	public String getResourceLocation()
	{
		if(myLocation != null)
		{
			return myLocation;
		}

		if(myTabs.getSelectedIndex() == 0)
		{
			TreePath path = mySchemasTree.getSelectionPath();
			if(path == null)
			{
				return null;
			}
			Object object = ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();
			if(!(object instanceof PsiFile))
			{
				return null;
			}
			return FileUtil.toSystemIndependentName(((PsiFile) object).getVirtualFile().getPath());
		}
		else
		{
			VirtualFile file = myExplorer.getSelectedFile();
			return file == null ? null : FileUtil.toSystemIndependentName(file.getPath());
		}
	}

	private void createUIComponents()
	{
		myExplorerPanel = new JPanel(new BorderLayout());
		DataManager.registerDataProvider(myExplorerPanel, dataId ->
		{
			if(CommonDataKeys.VIRTUAL_FILE_ARRAY == dataId)
			{
				return myExplorer.getSelectedFiles();
			}
			else if(FileSystemTree.DATA_KEY == dataId)
			{
				return myExplorer;
			}
			return null;
		});
	}
}
