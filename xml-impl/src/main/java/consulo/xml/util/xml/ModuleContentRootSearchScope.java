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
package consulo.xml.util.xml;

import jakarta.annotation.Nonnull;

import consulo.language.psi.scope.GlobalSearchScope;
import consulo.module.content.ModuleRootManager;
import consulo.module.Module;
import consulo.virtualFileSystem.VirtualFile;

public class ModuleContentRootSearchScope extends GlobalSearchScope {
  private final ModuleRootManager myRootManager;
  private final Module myModule;

  public ModuleContentRootSearchScope(final Module module) {
    super(module.getProject());
    myRootManager = ModuleRootManager.getInstance(module);
    myModule = module;
  }

  public boolean contains(final VirtualFile file) {
    return myRootManager.getFileIndex().isInContent(file);
  }

  public int compare(final VirtualFile file1, final VirtualFile file2) {
    return 0;
  }

  public boolean isSearchInModuleContent(@Nonnull final Module aModule) {
    return aModule == myModule;
  }

  public boolean isSearchInLibraries() {
    return false;
  }
}
