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
package consulo.xml.ide.structureView.xml;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.xml.psi.xml.XmlFile;
import consulo.component.extension.ExtensionPointName;
import consulo.fileEditor.structureView.StructureViewBuilder;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

@ExtensionAPI(ComponentScope.APPLICATION)
public interface XmlStructureViewBuilderProvider {
  ExtensionPointName<XmlStructureViewBuilderProvider> EP_NAME = ExtensionPointName.create(XmlStructureViewBuilderProvider.class);

  @Nullable
  StructureViewBuilder createStructureViewBuilder(@Nonnull XmlFile file);
}
