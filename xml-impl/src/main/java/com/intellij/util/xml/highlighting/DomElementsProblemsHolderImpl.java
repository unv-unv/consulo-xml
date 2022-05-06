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

package com.intellij.util.xml.highlighting;

import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.DomElementVisitor;
import com.intellij.util.xml.DomFileElement;
import com.intellij.util.xml.DomUtil;
import consulo.ide.impl.idea.openapi.util.Factory;
import consulo.ide.impl.idea.util.Function;
import consulo.language.editor.annotation.Annotation;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.language.editor.rawHighlight.SeverityRegistrar;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.SmartList;
import consulo.util.lang.function.Condition;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DomElementsProblemsHolderImpl implements DomElementsProblemsHolder {
  private final Map<DomElement, Map<Class<? extends DomElementsInspection>, List<DomElementProblemDescriptor>>> myCachedErrors =
      new ConcurrentHashMap<DomElement, Map<Class<? extends DomElementsInspection>, List<DomElementProblemDescriptor>>>();
  private final Map<DomElement, Map<Class<? extends DomElementsInspection>, List<DomElementProblemDescriptor>>> myCachedChildrenErrors =
      new ConcurrentHashMap<DomElement, Map<Class<? extends DomElementsInspection>, List<DomElementProblemDescriptor>>>();
  private final List<Annotation> myAnnotations = new ArrayList<Annotation>();

  private final Function<DomElement, List<DomElementProblemDescriptor>> myDomProblemsGetter =
      new Function<DomElement, List<DomElementProblemDescriptor>>() {
        public List<DomElementProblemDescriptor> fun(final DomElement s) {
          final Map<Class<? extends DomElementsInspection>, List<DomElementProblemDescriptor>> map = myCachedErrors.get(s);
          return map != null ? ContainerUtil.concat(map.values()) : Collections.<DomElementProblemDescriptor>emptyList();
        }
      };

  private final DomFileElement myElement;

  private static final Factory<Map<Class<? extends DomElementsInspection>, List<DomElementProblemDescriptor>>> CONCURRENT_HASH_MAP_FACTORY = new Factory<Map<Class<? extends DomElementsInspection>, List<DomElementProblemDescriptor>>>() {
    public Map<Class<? extends DomElementsInspection>, List<DomElementProblemDescriptor>> create() {
      return new ConcurrentHashMap<Class<? extends DomElementsInspection>, List<DomElementProblemDescriptor>>();
    }
  };
  private static final Factory<List<DomElementProblemDescriptor>> SMART_LIST_FACTORY = new Factory<List<DomElementProblemDescriptor>>() {
    public List<DomElementProblemDescriptor> create() {
      return new SmartList<DomElementProblemDescriptor>();
    }
  };
  private final Set<Class<? extends DomElementsInspection>> myPassedInspections = new HashSet<Class<? extends DomElementsInspection>>();

  public DomElementsProblemsHolderImpl(final DomFileElement element) {
    myElement = element;
  }

  public final void appendProblems(final DomElementAnnotationHolderImpl holder, final Class<? extends DomElementsInspection> inspectionClass) {
    if (isInspectionCompleted(inspectionClass)) return;

    for (final DomElementProblemDescriptor descriptor : holder) {
      addProblem(descriptor, inspectionClass);
    }
    myAnnotations.addAll(holder.getAnnotations());
    myPassedInspections.add(inspectionClass);
  }

  public final boolean isInspectionCompleted(@Nonnull final DomElementsInspection inspection) {
    return isInspectionCompleted(inspection.getClass());
  }

  public final boolean isInspectionCompleted(final Class<? extends DomElementsInspection> inspectionClass) {
    synchronized (DomElementAnnotationsManagerImpl.LOCK) {
      return myPassedInspections.contains(inspectionClass);
    }
  }

  public final List<Annotation> getAnnotations() {
    return myAnnotations;
  }

  public final void addProblem(final DomElementProblemDescriptor descriptor, final Class<? extends DomElementsInspection> inspection) {
    consulo.ide.impl.idea.util.containers.ContainerUtil.getOrCreate(consulo.ide.impl.idea.util.containers.ContainerUtil.getOrCreate(myCachedErrors, descriptor.getDomElement(), CONCURRENT_HASH_MAP_FACTORY), inspection,
        SMART_LIST_FACTORY).add(descriptor);
    myCachedChildrenErrors.clear();
  }

  @Nonnull
  public synchronized List<DomElementProblemDescriptor> getProblems(DomElement domElement) {
    if (domElement == null || !domElement.isValid()) return Collections.emptyList();
    return myDomProblemsGetter.fun(domElement);
  }

  public List<DomElementProblemDescriptor> getProblems(final DomElement domElement, boolean includeXmlProblems) {
    return getProblems(domElement);
  }

  public List<DomElementProblemDescriptor> getProblems(final DomElement domElement,
                                                       final boolean includeXmlProblems,
                                                       final boolean withChildren) {
    if (!withChildren || domElement == null || !domElement.isValid()) {
      return getProblems(domElement);
    }

    return ContainerUtil.concat(getProblemsMap(domElement).values());
  }

  public List<DomElementProblemDescriptor> getProblems(DomElement domElement,
                                                       final boolean includeXmlProblems,
                                                       final boolean withChildren,
                                                       final HighlightSeverity minSeverity) {
    return getProblems(domElement, withChildren, minSeverity);
  }

  public List<DomElementProblemDescriptor> getProblems(final DomElement domElement, final boolean withChildren, final HighlightSeverity minSeverity) {
    return ContainerUtil.findAll(getProblems(domElement, true, withChildren), new Condition<DomElementProblemDescriptor>() {
      public boolean value(final DomElementProblemDescriptor object) {
        return SeverityRegistrar.getSeverityRegistrar(domElement.getManager().getProject()).compare(object.getHighlightSeverity(), minSeverity) >= 0;
      }
    });

  }

  @Nonnull
  private Map<Class<? extends DomElementsInspection>, List<DomElementProblemDescriptor>> getProblemsMap(final DomElement domElement) {
    final Map<Class<? extends DomElementsInspection>, List<DomElementProblemDescriptor>> map = myCachedChildrenErrors.get(domElement);
    if (map != null) {
      return map;
    }

    final Map<Class<? extends DomElementsInspection>, List<DomElementProblemDescriptor>> problems = new HashMap<Class<? extends DomElementsInspection>, List<DomElementProblemDescriptor>>();
    if (domElement == myElement) {
      for (Map<Class<? extends DomElementsInspection>, List<DomElementProblemDescriptor>> listMap : myCachedErrors.values()) {
        mergeMaps(problems, listMap);
      }
    } else {
      mergeMaps(problems, myCachedErrors.get(domElement));
      if (DomUtil.hasXml(domElement)) {
        domElement.acceptChildren(new DomElementVisitor() {
          public void visitDomElement(DomElement element) {
            mergeMaps(problems, getProblemsMap(element));
          }
        });
      }
    }

    myCachedChildrenErrors.put(domElement, problems);
    return problems;
  }

  private static <T> void mergeMaps(final Map<T, List<DomElementProblemDescriptor>> accumulator, @Nullable final Map<T, List<DomElementProblemDescriptor>> toAdd) {
    if (toAdd == null) return;
    for (final Map.Entry<T, List<DomElementProblemDescriptor>> entry : toAdd.entrySet()) {
      consulo.ide.impl.idea.util.containers.ContainerUtil.getOrCreate(accumulator, entry.getKey(), SMART_LIST_FACTORY).addAll(entry.getValue());
    }
  }

  public List<DomElementProblemDescriptor> getAllProblems() {
    return getProblems(myElement, false, true);
  }

  public List<DomElementProblemDescriptor> getAllProblems(@Nonnull DomElementsInspection inspection) {
    if (!myElement.isValid()) {
      return Collections.emptyList();
    }
    final List<DomElementProblemDescriptor> list = getProblemsMap(myElement).get(inspection.getClass());
    return list != null ? new ArrayList<DomElementProblemDescriptor>(list) : Collections.<DomElementProblemDescriptor>emptyList();
  }
}
