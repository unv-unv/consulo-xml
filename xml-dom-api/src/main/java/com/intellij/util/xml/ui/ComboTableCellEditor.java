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
package com.intellij.util.xml.ui;

import java.awt.Component;
import java.util.List;
import java.util.Map;

import javax.swing.DefaultCellEditor;
import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JTable;

import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Factory;
import com.intellij.openapi.util.Pair;
import com.intellij.util.containers.HashMap;

/**
 * @author peter
 */
public class ComboTableCellEditor extends DefaultCellEditor {
  private final boolean myNullable;
  private final Factory<List<Pair<String, consulo.ui.image.Image>>> myDataFactory;
  private Map<String, consulo.ui.image.Image> myData;
  private static final Pair<String,Icon> EMPTY = Pair.create(" ", null);

  public ComboTableCellEditor(Factory<List<Pair<String, consulo.ui.image.Image>>> dataFactory, final boolean nullable) {
    super(new JComboBox());
    myDataFactory = dataFactory;
    myNullable = nullable;
    setClickCountToStart(2);
    JComboBox comboBox = (JComboBox)editorComponent;
    comboBox.setBorder(null);
    comboBox.putClientProperty("JComboBox.isTableCellEditor", Boolean.TRUE);
    ComboControl.initComboBox(comboBox, new Condition<String>() {
      public boolean value(final String object) {
        return myData != null && myData.containsKey(object) || myNullable && EMPTY.first == object;
      }
    });
  }

  public ComboTableCellEditor(Class<? extends Enum> anEnum, final boolean nullable) {
    this(ComboControl.createEnumFactory(anEnum), nullable);
  }

  public Object getCellEditorValue() {
    final Pair<String,Icon> cellEditorValue = (Pair<String,Icon>)super.getCellEditorValue();
    return EMPTY == cellEditorValue || null == cellEditorValue ? null : cellEditorValue.first;
  }

  public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
    final List<Pair<String, consulo.ui.image.Image>> list = myDataFactory.create();
    myData = new HashMap<>();

    final JComboBox comboBox = (JComboBox)editorComponent;
    comboBox.removeAllItems();
    if (myNullable) {
      comboBox.addItem(EMPTY);
    }
    for (final Pair<String, consulo.ui.image.Image> pair : list) {
      myData.put(pair.first, pair.second);
      comboBox.addItem(pair);
    }
    final Pair<Object, consulo.ui.image.Image> pair = Pair.create(value, myData.get(value));
    comboBox.setEditable(true);
    super.getTableCellEditorComponent(table, pair, isSelected, row, column);
    comboBox.setEditable(false);
    return comboBox;
  }
}
