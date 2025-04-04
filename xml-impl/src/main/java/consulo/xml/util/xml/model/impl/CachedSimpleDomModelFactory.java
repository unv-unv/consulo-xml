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
package consulo.xml.util.xml.model.impl;

import consulo.project.Project;
import consulo.application.util.CachedValueProvider;
import consulo.xml.psi.xml.XmlFile;
import consulo.xml.util.xml.DomElement;
import consulo.xml.util.xml.ModelMerger;
import consulo.xml.util.xml.model.DomModel;
import consulo.xml.util.xml.model.DomModelCache;
import consulo.util.dataholder.UserDataHolder;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * User: Sergey.Vasiliev
 */
public abstract class CachedSimpleDomModelFactory<T extends DomElement, M extends DomModel<T>, Scope extends UserDataHolder> extends
                                                                                                                             SimpleDomModelFactory<T, M>
    implements CachedDomModelFactory<T,M,Scope> {

  private final DomModelCache<M, XmlFile> myModelCache;

  protected CachedSimpleDomModelFactory(@Nonnull Class<T> aClass,
                          @Nonnull ModelMerger modelMerger,
                          final Project project,
                          @NonNls String name) {
    super(aClass, modelMerger);

    myModelCache = new DomModelCache<M, XmlFile>(project, name + " model") {
       @Nonnull
       protected CachedValueProvider.Result<M> computeValue(@Nonnull XmlFile file) {
         file = (XmlFile)file.getOriginalFile();

         final Scope scope = getModelScope(file);
         final M model = computeModel(file, scope);
         return new CachedValueProvider.Result<M>(model, computeDependencies(model, scope));
      }
    };
  }

  @Nullable
  public M getModelByConfigFile(@Nullable XmlFile psiFile) {
    if (psiFile == null) {
      return null;
    }
    return myModelCache.getCachedValue(psiFile);
  }

  @Nullable
  protected abstract M computeModel(@Nonnull XmlFile psiFile, @Nullable Scope scope);
}
