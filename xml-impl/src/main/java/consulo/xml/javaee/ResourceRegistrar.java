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

package consulo.xml.javaee;

import consulo.annotation.DeprecationInfo;
import org.jetbrains.annotations.NonNls;

/**
 * @author Dmitry Avdeev
 * @see StandardResourceProvider
 */
public interface ResourceRegistrar
{
	void addStdResource(@NonNls String resource, @NonNls String fileName);

	@Deprecated
	@DeprecationInfo("ClassLoader will be used from provider")
	void addStdResource(@NonNls String resource, @NonNls String fileName, Class klass);

	void addStdResource(@NonNls String resource, @NonNls String version, @NonNls String fileName);

	@Deprecated
	@DeprecationInfo("ClassLoader will be used from provider")
	void addStdResource(@NonNls String resource, @NonNls String version, @NonNls String fileName, Class klass);

	void addIgnoredResource(@NonNls String url);
}
