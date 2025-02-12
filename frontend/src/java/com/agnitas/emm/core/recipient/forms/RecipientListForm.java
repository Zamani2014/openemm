/*

    Copyright (C) 2019 AGNITAS AG (https://www.agnitas.org)

    This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
    This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.
    You should have received a copy of the GNU Affero General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.

*/

package com.agnitas.emm.core.recipient.forms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.agnitas.web.forms.PaginationForm;
import org.apache.commons.lang.StringUtils;

public class RecipientListForm extends PaginationForm {
    private static final String DEFAULT_FIELD = "email";

    private int filterMailinglistId;
    private int filterTargetId;
    private int filterUserStatus;
    private String filterUserType;
    private String searchFirstName;
    private String searchLastName;
    private String searchEmail;
    private String searchQueryBuilderRules = "[]";
    private List<String> selectedFields = new ArrayList<>();

    public int getFilterMailinglistId() {
        return filterMailinglistId;
    }

    public void setFilterMailinglistId(int filterMailinglistId) {
        this.filterMailinglistId = filterMailinglistId;
    }

    public int getFilterTargetId() {
        return filterTargetId;
    }

    public void setFilterTargetId(int filterTargetId) {
        this.filterTargetId = filterTargetId;
    }

    public int getFilterUserStatus() {
        return filterUserStatus;
    }

    public void setFilterUserStatus(int filterUserStatus) {
        this.filterUserStatus = filterUserStatus;
    }

    public String getFilterUserType() {
        return filterUserType;
    }

    public void setFilterUserType(String filterUserType) {
        this.filterUserType = filterUserType;
    }

    public String getSearchFirstName() {
        return searchFirstName;
    }

    public void setSearchFirstName(String searchFirstName) {
        this.searchFirstName = searchFirstName;
    }

    public String getSearchLastName() {
        return searchLastName;
    }

    public void setSearchLastName(String searchLastName) {
        this.searchLastName = searchLastName;
    }

    public String getSearchEmail() {
        return searchEmail;
    }

    public void setSearchEmail(String searchEmail) {
        this.searchEmail = searchEmail;
    }

    public String getSearchQueryBuilderRules() {
        return searchQueryBuilderRules;
    }

    public void setSearchQueryBuilderRules(String searchQueryBuilderRules) {
        this.searchQueryBuilderRules = searchQueryBuilderRules;
    }

    public List<String> getSelectedFields() {
        return selectedFields;
    }

    public void setSelectedFields(List<String> selectedFields) {
        this.selectedFields = selectedFields;
    }

    public boolean isDefaultColumn(String column) {
        return StringUtils.equalsIgnoreCase(DEFAULT_FIELD, column);
    }

    public boolean isSelectedColumn(String column) {
        return selectedFields.contains(column);
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = super.toMap();
        map.put("filterMailinglistId", filterMailinglistId);
        map.put("filterTargetId", filterTargetId);
        map.put("filterUserStatus", filterUserStatus);
        map.put("filterUserType", filterUserType);
        map.put("searchFirstName", searchFirstName);
        map.put("searchLastName", searchLastName);
        map.put("searchEmail", searchEmail);
        map.put("searchQueryBuilderRules", searchQueryBuilderRules);
        map.put("selectedFields", selectedFields);

        return map;
    }

    @Override
    public Object[] toArray() {
        // Each filter's name and value + all pagination parameters.
        List<Object> objects = new ArrayList<>(selectedFields.size() + 12);

        objects.add(getSort());
        objects.add(getOrder());
        objects.add(getPage());
        objects.add(getNumberOfRows());
        objects.add(filterMailinglistId);
        objects.add(filterTargetId);
        objects.add(filterUserStatus);
        objects.add(filterUserType);
        objects.add(searchFirstName);
        objects.add(searchLastName);
        objects.add(searchEmail);
        objects.add(searchQueryBuilderRules);
        objects.addAll(Collections.singletonList(selectedFields));

        return objects.toArray();
    }
}
