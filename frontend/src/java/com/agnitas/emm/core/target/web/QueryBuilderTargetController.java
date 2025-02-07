/*

    Copyright (C) 2019 AGNITAS AG (https://www.agnitas.org)

    This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
    This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.
    You should have received a copy of the GNU Affero General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.

*/

package com.agnitas.emm.core.target.web;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.agnitas.beans.impl.PaginatedListImpl;
import org.agnitas.dao.exception.target.TargetGroupTooLargeException;
import org.agnitas.emm.core.recipient.service.RecipientService;
import org.agnitas.emm.core.target.exception.UnknownTargetGroupIdException;
import org.agnitas.emm.core.velocity.VelocityCheck;
import org.agnitas.service.UserActivityLogService;
import org.agnitas.service.WebStorage;
import org.agnitas.target.TargetFactory;
import org.agnitas.util.AgnUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.agnitas.beans.ComAdmin;
import com.agnitas.beans.ComTarget;
import com.agnitas.emm.core.beans.Dependent;
import com.agnitas.emm.core.birtstatistics.recipient.dto.RecipientStatusStatisticDto;
import com.agnitas.emm.core.birtstatistics.service.BirtStatisticsService;
import com.agnitas.emm.core.mailinglist.service.MailinglistApprovalService;
import com.agnitas.emm.core.target.TargetUtils;
import com.agnitas.emm.core.target.beans.TargetComplexityGrade;
import com.agnitas.emm.core.target.beans.TargetGroupDependentType;
import com.agnitas.emm.core.target.eql.EqlDetailedAnalysisResult;
import com.agnitas.emm.core.target.eql.EqlFacade;
import com.agnitas.emm.core.target.eql.parser.EqlParserException;
import com.agnitas.emm.core.target.eql.parser.EqlSyntaxError;
import com.agnitas.emm.core.target.form.TargetDependentsListForm;
import com.agnitas.emm.core.target.form.TargetEditForm;
import com.agnitas.emm.core.target.form.validator.TargetEditFormValidator;
import com.agnitas.emm.core.target.service.ComTargetService;
import com.agnitas.emm.core.target.service.TargetCopyService;
import com.agnitas.emm.core.target.web.util.EditorContentSynchronizationException;
import com.agnitas.emm.core.target.web.util.EditorContentSynchronizer;
import com.agnitas.emm.core.target.web.util.FormHelper;
import com.agnitas.emm.core.workflow.service.util.WorkflowUtils;
import com.agnitas.messages.I18nString;
import com.agnitas.messages.Message;
import com.agnitas.service.GridServiceWrapper;
import com.agnitas.web.mvc.Popups;
import com.agnitas.web.perm.annotations.PermissionMapping;

import jakarta.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/target")
@PermissionMapping("target")
public class QueryBuilderTargetController {

    private static final Logger logger = Logger.getLogger(TargetController.class);

    private final MailinglistApprovalService mailinglistApprovalService;
    private final ComTargetService targetService;
    private final RecipientService recipientService;
    private final EditorContentSynchronizer editorContentSynchronizer;
    private final EqlFacade eqlFacade;
    private final TargetCopyService targetCopyService;
    private final TargetFactory targetFactory;
    private final UserActivityLogService userActivityLogService;
    private final BirtStatisticsService birtStatisticsService;
    private final WebStorage webStorage;
    private final GridServiceWrapper gridService;
    private final TargetEditFormValidator editFormValidator;

    public QueryBuilderTargetController(MailinglistApprovalService mailinglistApprovalService, ComTargetService targetService,
                                        RecipientService recipientService, EditorContentSynchronizer editorContentSynchronizer,
                                        EqlFacade eqlFacade, TargetCopyService targetCopyService, TargetFactory targetFactory,
                                        UserActivityLogService userActivityLogService, BirtStatisticsService birtStatisticsService,
                                        WebStorage webStorage, GridServiceWrapper gridService, TargetEditFormValidator editFormValidator) {
        this.mailinglistApprovalService = mailinglistApprovalService;
        this.targetService = targetService;
        this.recipientService = recipientService;
        this.editorContentSynchronizer = editorContentSynchronizer;
        this.eqlFacade = eqlFacade;
        this.targetCopyService = targetCopyService;
        this.targetFactory = targetFactory;
        this.userActivityLogService = userActivityLogService;
        this.birtStatisticsService = birtStatisticsService;
        this.webStorage = webStorage;
        this.gridService = gridService;
        this.editFormValidator = editFormValidator;
    }

    @RequestMapping("/{targetId:\\d+}/view.action")
    public String view(@PathVariable int targetId, ComAdmin admin, Model model, @ModelAttribute("targetEditForm") TargetEditForm form,
                       Popups popups, HttpServletRequest request, @RequestParam(required = false) boolean isMailingWizard) throws EqlParserException {
        WorkflowUtils.updateForwardParameters(request);

        if (targetId > 0 && form.getPreviousViewFormat() == null) {
            boolean exists = fillFormIfTargetExists(admin.getCompanyID(), form);
            if (!exists) {
                logger.warn("Target group does not exists. Company ID: " + admin.getCompanyID() + " Target ID: " + form.getTargetId());
                return "redirect:/target/list.action";
            }
        }

        if (form.getViewFormatOrDefault() == TargetgroupViewFormat.QUERY_BUILDER) {
            return viewQB(admin, model, form, popups, isMailingWizard);
        }

        return viewEQL(admin, model, form, popups);
    }

    @RequestMapping("/{targetId:\\d+}/save.action")
    public String save(ComAdmin admin,
                       @ModelAttribute("targetEditForm") TargetEditForm form,
                       @RequestParam(required = false) boolean showStatistic,
                       Popups popups, RedirectAttributes redirectAttributes,
                       @RequestParam(required = false) boolean isMailingWizard)
            throws UnknownTargetGroupIdException, EqlParserException {

        int mailinglistId = form.getMailinglistId();
        TargetgroupViewFormat previousFormat = form.getPreviousViewFormat();

        boolean isPrepared = doSavingPreparations(admin, form, popups);
        if (!isPrepared) {
            return "messages";
        }

        int targetId = trySave(admin, form, popups);
        if (targetId <= 0 || popups.hasAlertPopups()) {
            return "messages";
        }

        analyzeComplexity(admin.getCompanyID(), targetId, popups);

        if (showStatistic) {
            redirectAttributes.addFlashAttribute("showStatistic", true);
            redirectAttributes.addFlashAttribute("statisticUrl", getReportUrl(admin, RequestContextHolder.getRequestAttributes().getSessionId(), targetId, mailinglistId));
            redirectAttributes.addAttribute("mailinglistId", mailinglistId);
        }

        if (previousFormat != TargetEditForm.getDefaultViewFormat()) {
            redirectAttributes.addAttribute("viewFormat", previousFormat);
        }

        popups.success("default.changes_saved");

        if(isMailingWizard) {
            return "redirect:/mwTarget.do?action=viewTarget&viewTargetId="+targetId;
        }
        return "redirect:/target/" + targetId + "/view.action";
    }

    @RequestMapping("/create.action")
    public String create(ComAdmin admin, Model model, TargetEditForm form, Popups popups) throws EqlParserException {

        form.setViewFormat(TargetgroupViewFormat.QUERY_BUILDER);

        model.addAttribute("isLocked", false);

        return viewQB(admin, model, form, popups, false);
    }

    @RequestMapping("/{targetId:\\d+}/lock.action")
    public String lock(@PathVariable int targetId, ComAdmin admin, Popups popups) throws UnknownTargetGroupIdException {
        if (targetService.lockTargetGroup(admin.getCompanyID(), targetId)) {
            final ComTarget target = targetService.getTargetGroup(targetId, admin.getCompanyID());
            String logAction = "do lock target group";
            String logDescription = target.getTargetName() + " (" + targetId + ")";
            userActivityLogService.writeUserActivityLog(admin, logAction, logDescription, logger);

            popups.success("default.changes_saved");
        } else {
            popups.alert("error.target.not_saved");
        }

        return "redirect:/target/" + targetId + "/view.action";
    }

    @RequestMapping("/{targetId:\\d+}/unlock.action")
    public String unlock(@PathVariable int targetId, ComAdmin admin, Popups popups) throws UnknownTargetGroupIdException {
        if (targetService.unlockTargetGroup(admin.getCompanyID(), targetId)) {
            final ComTarget target = targetService.getTargetGroup(targetId, admin.getCompanyID());
            String logAction = "do unlock target group";
            String logDescription = target.getTargetName() + " (" + targetId + ")";
            userActivityLogService.writeUserActivityLog(admin, logAction, logDescription, logger);

            popups.success("default.changes_saved");
        } else {
            popups.alert("error.target.not_saved");
        }

        return "redirect:/target/" + targetId + "/view.action";
    }

    @RequestMapping("/{targetId:\\d+}/copy.action")
    public final String copy(@PathVariable int targetId, ComAdmin admin, TargetEditForm form, Popups popups) throws Exception {
        int companyId = admin.getCompanyID();
        ComTarget copiedTarget = form.getTargetId() != 0
                ? targetService.getTargetGroup(form.getTargetId(), companyId)
                : targetFactory.newTarget(companyId);
        copiedTarget.setId(0);
        copiedTarget.setCreationDate(null);
        copiedTarget.setChangeDate(null);

        final String newName = I18nString.getLocaleString("mailing.CopyOf", admin.getLocale()) +
                " " + copiedTarget.getTargetName();

        copiedTarget.setTargetName(newName);
        form.setShortname(newName);

        try {
            List<Message> messages = new LinkedList<>();

            int targetID = targetService.saveTarget(admin, copiedTarget, null, messages, userActivityLogService::writeUserActivityLog);

            messages.forEach(popups::alert);

            form.setTargetId(targetID);
        } catch (final TargetGroupTooLargeException e) {
            popups.alert("error.target.too_large");

            form.setTargetId(e.getTargetId());
        }

        return "redirect:/target/" + form.getTargetId() + "/view.action";
    }

    @RequestMapping("/{targetId:\\d+}/dependents.action")
    public String dependents(@PathVariable int targetId, ComAdmin admin, @ModelAttribute("dependentsForm") TargetDependentsListForm form, Model model) {
        final int companyId = admin.getCompanyID();

        webStorage.access(WebStorage.TARGET_DEPENDENTS_OVERVIEW, entry -> {
            if (form.getNumberOfRows() > 0) {
                entry.setRowsCount(form.getNumberOfRows());
                if (form.getFilterTypes() == null) {
                    entry.setFilterTypes(null);
                } else {
                    entry.setFilterTypes(Arrays.asList(form.getFilterTypes()));
                }
            } else {
                form.setNumberOfRows(entry.getRowsCount());
                form.setFilterTypes(entry.getFilterTypes().toArray(ArrayUtils.EMPTY_STRING_ARRAY));
            }
        });

        PaginatedListImpl<Dependent<TargetGroupDependentType>> dependents = targetService.getDependents(companyId, form.getTargetId(), form.getFilterTypesSet(), form.getPage(), form.getNumberOfRows(), form.getSort(), form.getOrder());

        model.addAttribute("dependents", dependents);
        model.addAttribute("targetShortname", targetService.getTargetName(form.getTargetId(), companyId));

        List<Integer> mailingIds = dependents.getList().stream()
                .filter(dependent -> TargetGroupDependentType.MAILING == dependent.getType() || TargetGroupDependentType.MAILING_CONTENT == dependent.getType())
                .map(Dependent::getId)
                .collect(Collectors.toList());

        model.addAttribute("mailingGridTemplateMap", gridService.getGridTemplateIdsByMailingIds(companyId, mailingIds));

        return "target_dependents_list";
    }

    private String viewQB(ComAdmin admin, Model model, TargetEditForm form, Popups popups, boolean isMailingWizard) {
        if (form.getPreviousViewFormat() == null) {
            form.setPreviousViewFormat(TargetgroupViewFormat.EQL);
        }

        boolean mailTrackingAvailable = AgnUtils.isMailTrackingAvailable(admin);

        EqlDetailedAnalysisResult analysisResult = eqlFacade.analyseEqlSafely(form.getEql());

        if (analysisResult.isAnaliseSuccess()) {
            if (analysisResult.isMailTrackingRequired() && !mailTrackingAvailable) {
                form.setViewFormat(TargetgroupViewFormat.EQL);
                popups.warning("warning.target.mailtrackingRequired");
            } else {
                form.setViewFormat(trySynchronizeForView(admin, form, popups, TargetgroupViewFormat.EQL));
            }
        } else {
            form.setViewFormat(form.getPreviousViewFormat());

            List<Message> errorsMessages = new LinkedList<>();
            List<EqlSyntaxError> syntaxErrors = analysisResult.getSyntaxErrors();
            syntaxErrors.forEach(syntaxError ->
                    errorsMessages.add(Message.of("error.target.eql.syntax", syntaxError.getLine(), syntaxError.getColumn(), syntaxError.getSymbol()))
            );
            errorsMessages.forEach(popups::alert);
            model.addAttribute("eqlErrors", errorsMessages);
        }

        setupCommonViewPageParams(admin, form.getTargetId(), form.getEql(), model);
        model.addAttribute("mailTrackingAvailable", mailTrackingAvailable);

        if(isMailingWizard) {
            model.addAttribute("editTargetForm", form);
            return "mailing_wizard_new_target";
        }
        return "target_view";
    }

    private String viewEQL(ComAdmin admin, Model model, TargetEditForm form, Popups popups) {
        if (form.getPreviousViewFormat() == null) {
            form.setPreviousViewFormat(TargetgroupViewFormat.EQL);
        }
        TargetgroupViewFormat viewFormat = trySynchronizeForView(admin, form, popups, TargetgroupViewFormat.QUERY_BUILDER);
        form.setViewFormat(viewFormat);

        if (popups.hasWarningPopups() || popups.hasAlertPopups()) {
            return "messages";
        }

        if (viewFormat != TargetgroupViewFormat.EQL) {
            return viewQB(admin, model, form, popups, false);
        }

        setupCommonViewPageParams(admin, form.getTargetId(), form.getEql(), model);
        return "target_view";
    }

    private TargetComplexityGrade getComplexityGrade(String eql, @VelocityCheck int companyId) {
        int complexityIndex = targetService.calculateComplexityIndex(eql, companyId);
        int recipientsCount = recipientService.getNumberOfRecipients(companyId);

        return TargetUtils.getComplexityGrade(complexityIndex, recipientsCount);
    }

    private String getReportUrl(ComAdmin admin, String sessionId, int targetId, int mailinglistIs) {
        try {
            RecipientStatusStatisticDto statisticDto = new RecipientStatusStatisticDto();
            statisticDto.setMediaType(0);
            statisticDto.setTargetId(targetId);
            statisticDto.setMailinglistId(mailinglistIs);
            statisticDto.setFormat("html");

            return birtStatisticsService.getRecipientStatusStatisticUrl(admin, sessionId, statisticDto);
        } catch (Exception e) {
            logger.error("Error during generation statistic url " + e);
        }

        return StringUtils.EMPTY;
    }

    private void setupCommonViewPageParams(ComAdmin admin, int targetId, String eql, Model model) {
        model.addAttribute("mailinglists", mailinglistApprovalService.getEnabledMailinglistsForAdmin(admin));
        model.addAttribute("complexityGrade", getComplexityGrade(eql, admin.getCompanyID()));
        model.addAttribute("isValid", targetService.isValid(admin.getCompanyID(), targetId));
        model.addAttribute("isLocked", targetService.isLocked(admin.getCompanyID(), targetId));
    }

    private boolean fillFormIfTargetExists(int companyId, TargetEditForm form) {
        try {
            ComTarget target = targetService.getTargetGroup(form.getTargetId(), companyId);
            form.setTargetId(target.getId());
            form.setShortname(target.getTargetName());
            form.setDescription(target.getTargetDescription());
            form.setEql(target.getEQL());
            form.setUseForAdminAndTestDelivery(target.isAdminTestDelivery());
            return true;
        } catch (final UnknownTargetGroupIdException e) {
            logger.warn(String.format("Unknown target group ID %d", form.getTargetId()), e);
            return false;
        }
    }

    private TargetgroupViewFormat trySynchronizeForView(ComAdmin admin, TargetEditForm form, Popups popups, TargetgroupViewFormat defaultFormat) {
        if (form.getPreviousViewFormat() == form.getViewFormatOrDefault()) {
            return form.getPreviousViewFormat();
        }
        try {
            editorContentSynchronizer.synchronizeEditors(admin, form.getPreviousViewFormat(), form);
            return form.getViewFormatOrDefault();
        } catch (final EditorContentSynchronizationException e) {
            popups.warning("targetgroup.tooComplex");

            logger.info("There was an error synchronizing editor content. Keeping current view format.", e);
            logger.warn("EQL: " + form.getEql(), e);

            return defaultFormat;
        }
    }

    private boolean trySynchronizeToEqlForSave(ComAdmin admin, TargetEditForm form, Popups popups) {
        if (form.getPreviousViewFormat() == TargetgroupViewFormat.EQL) {
            return true;
        }
        try {
            editorContentSynchronizer.synchronizeEditors(admin, form.getPreviousViewFormat(), form);
            return true;
        } catch (final EditorContentSynchronizationException e) {
            logger.info("There was an error synchronizing editor content.", e);
            popups.alert("error.target.saving");
            return false;
        }
    }

    private int trySave(ComAdmin admin, TargetEditForm form, Popups popups) throws UnknownTargetGroupIdException {
        int companyId = admin.getCompanyID();
        // Load target group or create new one
        final ComTarget oldTarget;
        final ComTarget newTarget;
        if (form.getTargetId() == 0) {
            oldTarget = null;
            newTarget = targetFactory.newTarget(companyId);
        } else {
            oldTarget = targetService.getTargetGroup(form.getTargetId(), companyId);
            newTarget = targetCopyService.copyTargetGroup(oldTarget, targetFactory.newTarget());
        }

        // Update editable properties
        FormHelper.formPropertiesToTargetGroup(newTarget, form);

        try {
            final List<Message> errors = new ArrayList<>();
            final int targetId = targetService.saveTarget(admin, newTarget, oldTarget, errors, userActivityLogService::writeUserActivityLog);

            errors.forEach(popups::alert);

            return targetId;
        } catch (final TargetGroupTooLargeException e) {
            popups.alert("error.target.too_large");
        } catch (final Exception e) {
            logger.warn("There was an error Saving the target group. ", e);

            popups.alert("error.target.saving");
        }
        return 0;
    }

    private void analyzeComplexity(int companyId, int targetId, Popups popups) {
        TargetComplexityGrade complexityGrade = targetService.getTargetComplexityGrade(companyId, targetId);

        if (complexityGrade == TargetComplexityGrade.RED) {
            popups.warning("warning.target.group.performance.red");
        } else if (complexityGrade == TargetComplexityGrade.YELLOW) {
            popups.warning("warning.target.group.performance.yellow");
        }
    }

    private void analyzeEqlForSaving(ComAdmin admin, String eql, Popups popups) {
        EqlDetailedAnalysisResult analysisResult = eqlFacade.analyseEqlSafely(eql);
        if (!analysisResult.isAnaliseSuccess()) {
            analysisResult.getSyntaxErrors().forEach(syntaxError ->
                    popups.alert("error.target.eql.syntax", syntaxError.getLine(), syntaxError.getColumn(), syntaxError.getSymbol())
            );
        } else if (analysisResult.isMailTrackingRequired() && !AgnUtils.isMailTrackingAvailable(admin)) {
            popups.warning("warning.target.mailtrackingRequired");
        }
    }

    private boolean doSavingPreparations(ComAdmin admin, TargetEditForm form, Popups popups) {
        if (form.getPreviousViewFormat() == null) {
            logger.warn("There is not previous view format. We do not know which tab use for saving.");
            popups.alert("error.target.saving");
            return false;
        }

        boolean isFormValid = editFormValidator.validate(admin.getCompanyID(), form, popups);
        if (!isFormValid) {
            return false;
        }

        boolean isSynchronized = trySynchronizeToEqlForSave(admin, form, popups);
        if (!isSynchronized) {
            return false;
        }

        analyzeEqlForSaving(admin, form.getEql(), popups);
        if (popups.hasAlertPopups()) {
            return false;
        }

        return true;
    }
}
