/*

    Copyright (C) 2019 AGNITAS AG (https://www.agnitas.org)

    This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
    This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.
    You should have received a copy of the GNU Affero General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.

*/

package com.agnitas.emm.core.recipient.dto;

import static com.agnitas.beans.ProfileField.MODE_EDIT_EDITABLE;
import static com.agnitas.beans.ProfileField.MODE_EDIT_READONLY;

import java.util.List;

import org.agnitas.util.DbColumnType;

public class RecipientColumnDefinition {
    private String columnName;
    private String shortname;
    private DbColumnType.SimpleDataType dataType;
    private int editMode;
    private boolean nullable;
    private int maxSize;
    private boolean lineAfter;
    private String defaultValue;
    private List<String> fixedValues;
    private boolean isMainColumn;

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setShortname(String shortname) {
        this.shortname = shortname;
    }

    public String getShortname() {
        return shortname;
    }

    public void setDataType(DbColumnType.SimpleDataType dataType) {
        this.dataType = dataType;
    }

    public DbColumnType.SimpleDataType getDataType() {
        return dataType;
    }

    public void setEditMode(int editMode) {
        this.editMode = editMode;
    }

    public int getEditMode() {
        return editMode;
    }

    public boolean isReadable() {
        return editMode == MODE_EDIT_EDITABLE || editMode == MODE_EDIT_READONLY;
    }

    public boolean isWritable() {
        return editMode == MODE_EDIT_EDITABLE;
    }

    public void setNullable(boolean nullable) {
        this.nullable = nullable;
    }

    public boolean isNullable() {
        return nullable;
    }
    public void setMaxSize(int maxSize) {
        this.maxSize = maxSize;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public boolean isLineAfter() {
        return lineAfter;
    }

    public void setLineAfter(boolean lineAfter) {
        this.lineAfter = lineAfter;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setFixedValues(List<String> fixedValues) {
        this.fixedValues = fixedValues;
    }

    public List<String> getFixedValues() {
        return fixedValues;
    }

    public boolean isMainColumn() {
        return isMainColumn;
    }

    public void setMainColumn(boolean mainColumn) {
        isMainColumn = mainColumn;
    }
}
