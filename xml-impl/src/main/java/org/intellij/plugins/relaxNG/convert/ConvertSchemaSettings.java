/*
 * Copyright 2007 Sascha Weinreuter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.intellij.plugins.relaxNG.convert;

import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: sweinreuter
 * Date: 17.11.2007
 */
public interface ConvertSchemaSettings {
  @Nonnull
  SchemaType getOutputType();

  String getOutputEncoding();

  int getIndent();

  int getLineLength();

  String getOutputDestination();

  void addAdvancedSettings(List<String> inputParams, List<String> outputParams);
}
