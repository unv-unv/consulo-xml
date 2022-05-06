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
package com.intellij.ide.util.treeView;

import consulo.language.psi.PsiElement;
import consulo.ide.impl.idea.ide.util.treeView.SmartElementDescriptor;
import consulo.project.Project;
import consulo.ui.ex.tree.NodeDescriptor;

/**
 * @author Mike
 */
public class XmlDoctypeNodeDescriptor  extends SmartElementDescriptor {
  public XmlDoctypeNodeDescriptor(Project project, NodeDescriptor parentDescriptor, PsiElement element) {
    super(project, parentDescriptor, element);
    //noinspection HardCodedStringLiteral
    myName = "DOCTYPE";
  }
}
