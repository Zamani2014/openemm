AGN.Lib.Controller.new('import-profile', function () {
  var genderMappingRowTemplate;
  var $genderMappingTable;
  var isGenderSectionFocused = false;

  this.addDomInitializer('new-recipients-action-select', function() {
    setMailingListSelectionEnabled(this.el.val() <= 0);
  });

  this.addAction({click: 'save'}, function () {
    if (isTextGendersUnique(getGenderTextValues(getGenderTableLastRow()))) {
      var form = AGN.Lib.Form.get($('#importProfileForm'));
      setGenderMappingsToForm(form);
      form.setValue('isGenderSectionFocused', isGenderSectionFocused);
      form.submit();
    }
  });

  this.addDomInitializer('import-profile-view', function () {
    $('#importProfileForm').on('click change', function (event) {
      var $el = $(event.target)
      isGenderSectionFocused = ($el.is('input') || $el.is('select') || $el.is('button') || $el.parent().is('button'))
        && $el.parents('#recipient-import-gender-settings').length > 0;
    });
    initGenderMappingTable(sortMappingsByValue(this.config.genderMappings));
  });

  this.addAction({change: 'new-recipients-action-select'}, function() {
    setMailingListSelectionEnabled(this.el.val() <= 0);
  });

  this.addAction({click: 'add-gender-mapping', enterdown: 'gender-enterdown'}, function () {
    this.event.preventDefault();
    if (isLastRowCanBeAddedToTable()) {
      replaceNewButtonWithDeleteButton();
      appendRowToGenderMappingTable('', '');
      getGenderTableLastRow().find('[data-gender-text-value]').focus();
    }
  });

  this.addAction({click: 'delete-gender-mapping'}, function () {
    this.el.closest('tr').remove();
  });

  var setMailingListSelectionEnabled = function(isEnabled) {
    $('#mailinglists').toggleClass('hidden', !isEnabled);
    $('#mailinglists-to-show').toggleClass('hidden', isEnabled);
  };

  this.addDomInitializer('allMailinglists-checkbox', function() {
    setSeparateMailinglistChecboxes($('#allMalinglistsCheckbox').is(':checked'));
  });

  //#all-mailinglists-wrapper is visible just for ${allowedModesForAllMailinglists} import modes
  //if #allMalinglistsCheckbox checkbox is activated and is not visible just deactivate it
  this.addAction({change: 'mode-select-change'}, function() {
    var visible = $('#all-mailinglists-wrapper').is(':visible');
    if (!visible && $('#allMalinglistsCheckbox').is(':checked')) {
      $('#allMalinglistsCheckbox').click();
    }
  });

  this.addAction({change: 'allMailinglists-checkbox'}, function() {
    setSeparateMailinglistChecboxes($(this.el).is(':checked'));
  });

  var setSeparateMailinglistChecboxes = function (disabled) {
    $('#mailinglists [type="checkbox"]').prop('disabled', disabled);
  };

  function initGenderMappingTable(mappings) {
    $genderMappingTable = $('#recipient-import-gender-settings tbody');
    genderMappingRowTemplate = AGN.Lib.Template.prepare('gender-settings-table-row');
    _.each(mappings, function (mapping) {
      appendRowToGenderMappingTable(mapping[0], mapping[1]);
    });
    appendRowToGenderMappingTable('', '');
  }

  function getGenderTextValuesStr($row) {
    var $el = $row.find('[data-gender-text-value]');
    if ($el.is('input')) {
      return $el.val().trim();
    } else {
      return $el.text().trim();
    }
  }

  function getGenderTextValues($row) {
    var textValuesStr = getGenderTextValuesStr($row);
    var result = textValuesStr.length ? textValuesStr.split(',') : []
    return new Set(_.without(_.map(result, _.trim), ''));
  }

  function getGenderIntValue($row) {
    var $el = $row.find('[data-gender-int-value]');
    return $el.is('select') ? $el.val() : $el.text()[0];
  }

  function appendRowToGenderMappingTable(textValues, intValue) {
    if (intValue === 0 || intValue) {
      intValue += ' (' + t("import.gender.short." + intValue) + ')';
    } else {
      intValue = '';
    }
    $genderMappingTable.append(genderMappingRowTemplate({intValue: intValue, textValues: textValues}));
    AGN.Lib.CoreInitializer.run("select", $genderMappingTable.find('[data-gender-int-value]:last-child'));
  }

  function replaceNewButtonWithDeleteButton() {
    var newBtn = $genderMappingTable.find('[data-action="add-gender-mapping"]')
    newBtn.after("<a href='#' class='btn btn-regular btn-alert' data-action='delete-gender-mapping'>" +
      "<i class='icon icon-trash-o'></i></a>");
    newBtn.remove();
  }

  function isLastRowCanBeAddedToTable() {
    var textValues = getGenderTextValues(getGenderTableLastRow());
    if (textValues.size < 1) {
      AGN.Lib.Messages(t("defaults.error"), t("import.gender.error.empty"), 'alert');
      return false;
    }
    return isTextGendersUnique(textValues);
  }

  function isTextGendersUnique(genders) {
    var rows = $genderMappingTable.find('[data-gender-settings-row]').toArray();
    for (var i = 0; i < rows.length - 1; i++) {
      if (_.intersection(Array.from(genders), Array.from(getGenderTextValues($(rows[i])))).length) {
        AGN.Lib.Messages(t("defaults.error"), t("import.gender.error.duplicate"), 'alert');
        return false;
      }
    }
    return true;
  }

  function getGenderTableLastRow() {
    return $genderMappingTable.find('[data-gender-settings-row]:last-child');
  }

  function collectGenderMappings() {
    var mappings = {};
    _.each($genderMappingTable.find('[data-gender-settings-row]'), function (row) {
      var $row = $(row);
      var textValuesStr = getGenderTextValuesStr($row);
      var intValue = getGenderIntValue($row);
      if (textValuesStr.length) {
        mappings[intValue] = !mappings.hasOwnProperty(intValue)
          ? textValuesStr
          : mappings[intValue] + ', ' + textValuesStr;
      }
    })
    return mappings;
  }

  function setGenderMappingsToForm(form) {
    _.each(collectGenderMappings(), function (textValuesStr, intValue) {
      form.setValue('profile.genderMappingsToSave.' + intValue, textValuesStr);
    });
  }

  function sortMappingsByValue(mappings) {
    var result = [];
    Object.keys(mappings).forEach(function (key) {
      result.push([key, mappings[key]]);
    });

    result.sort(function (a, b) {
      return a[1] - b[1];
    });
    return result;
  }
});
