/*

    Copyright (C) 2019 AGNITAS AG (https://www.agnitas.org)

    This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
    This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.
    You should have received a copy of the GNU Affero General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.

*/

package com.agnitas.emm.core.target.service.impl;

import static com.agnitas.beans.ComMailing.NONE_SPLIT_ID;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.agnitas.beans.DynamicTagContent;
import org.agnitas.beans.Mailing;
import org.agnitas.beans.MailingComponent;
import org.agnitas.beans.Recipient;
import org.agnitas.beans.impl.PaginatedListImpl;
import org.agnitas.dao.MailingComponentDao;
import org.agnitas.dao.exception.target.TargetGroupPersistenceException;
import org.agnitas.emm.core.commons.util.ConfigService;
import org.agnitas.emm.core.target.exception.TargetGroupException;
import org.agnitas.emm.core.target.exception.TargetGroupIsInUseException;
import org.agnitas.emm.core.target.exception.UnknownTargetGroupIdException;
import org.agnitas.emm.core.target.service.TargetGroupLocator;
import org.agnitas.emm.core.target.service.UserActivityLog;
import org.agnitas.emm.core.velocity.VelocityCheck;
import org.agnitas.service.ColumnInfoService;
import org.agnitas.util.beanshell.BeanShellInterpreterFactory;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.log4j.Logger;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.web.util.UriComponentsBuilder;

import com.agnitas.beans.ComAdmin;
import com.agnitas.beans.ComMailing;
import com.agnitas.beans.ComTarget;
import com.agnitas.beans.DynamicTag;
import com.agnitas.beans.ListSplit;
import com.agnitas.beans.TargetLight;
import com.agnitas.beans.impl.TargetLightImpl;
import com.agnitas.dao.ComMailingDao;
import com.agnitas.dao.ComRecipientDao;
import com.agnitas.dao.ComTargetDao;
import com.agnitas.emm.core.Permission;
import com.agnitas.emm.core.beans.Dependent;
import com.agnitas.emm.core.profilefields.ProfileFieldException;
import com.agnitas.emm.core.profilefields.service.ProfileFieldService;
import com.agnitas.emm.core.target.TargetExpressionUtils;
import com.agnitas.emm.core.target.TargetUtils;
import com.agnitas.emm.core.target.beans.RawTargetGroup;
import com.agnitas.emm.core.target.beans.TargetComplexityGrade;
import com.agnitas.emm.core.target.beans.TargetGroupDependentType;
import com.agnitas.emm.core.target.complexity.bean.TargetComplexityEvaluationCache;
import com.agnitas.emm.core.target.complexity.bean.impl.TargetComplexityEvaluationCacheImpl;
import com.agnitas.emm.core.target.complexity.service.TargetComplexityEvaluator;
import com.agnitas.emm.core.target.eql.EqlAnalysisResult;
import com.agnitas.emm.core.target.eql.EqlFacade;
import com.agnitas.emm.core.target.eql.codegen.CodeGeneratorException;
import com.agnitas.emm.core.target.eql.codegen.resolver.ProfileFieldResolveException;
import com.agnitas.emm.core.target.eql.codegen.resolver.ReferenceTableResolveException;
import com.agnitas.emm.core.target.eql.parser.EqlParserException;
import com.agnitas.emm.core.target.eql.referencecollector.SimpleReferenceCollector;
import com.agnitas.emm.core.target.exception.EqlFormatException;
import com.agnitas.emm.core.target.service.ComTargetService;
import com.agnitas.emm.core.target.service.RecipientTargetGroupMatcher;
import com.agnitas.emm.core.target.service.TargetLightsOptions;
import com.agnitas.emm.core.target.service.TargetSavingAndAnalysisResult;
import com.phloc.commons.collections.pair.Pair;

import bsh.Interpreter;

/**
 * Implementation of {@link ComTargetService} interface.
 */
public class ComTargetServiceImpl implements ComTargetService {

	/**
	 * The logger.
	 */
	private static final transient Logger logger = Logger.getLogger(ComTargetServiceImpl.class);

	private static final String SIMPLE_TARGET_EXPRESSION = "1=1";

	private static final Pattern GENDER_EQUATION_PATTERN = Pattern.compile("cust\\.gender\\s*=\\s*(-?\\d+)");

	/**
	 * DAO accessing target groups.
	 */
    protected ComTargetDao targetDao;

    /**
     * DAO accessing mailing components.
     */
    private MailingComponentDao mailingComponentDao;

    /** DAO for accessing mailing data. */
    private ComMailingDao mailingDao;

    /**
     * Component to locate target groups.
     */
    private TargetGroupLocator targetGroupLocator;

	/** DAO for accessing recipient data. */
	private ComRecipientDao recipientDao;

	/** Facade for EQL logic. */
	private EqlFacade eqlFacade;

	/** Service dealing with profile fields. */
	private ProfileFieldService profileFieldService;

	private TargetComplexityEvaluator complexityEvaluator;

	private BeanShellInterpreterFactory beanShellInterpreterFactory;

	private ColumnInfoService columnInfoService;

	// ---------------------------------------------------------------------------------------- Business Code

    @Override
	public void deleteTargetGroup(int targetGroupID, @VelocityCheck int companyID) throws TargetGroupException, TargetGroupPersistenceException {
		if (logger.isInfoEnabled()) {
			logger.info("Deleting target group " + targetGroupID + " of company " + companyID);
		}

		TargetGroupLocator.TargetDeleteStatus status = targetGroupLocator.isTargetGroupCanBeDeleted(companyID, targetGroupID);

		switch (status) {
			case CAN_BE_FULLY_DELETED_FROM_DB: {
				targetDao.deleteTargetReally(targetGroupID, companyID);
				break;
			}
			case CAN_BE_MARKED_AS_DELETED: {
				targetDao.deleteTarget(targetGroupID, companyID);
				break;
			}
			case CANT_BE_DELETED: {
				if (logger.isInfoEnabled()) {
					logger.info("Cannot delete target group " + targetGroupID + " - target group is in use");
				}

				throw new TargetGroupIsInUseException(targetGroupID);
			}
			default:
				throw new TargetGroupException("Invalid target group status");
		}
	}

    @Override
    public boolean hasMailingDeletedTargetGroups(Mailing mailing) {

		if ( logger.isInfoEnabled()) {
			logger.info( "Checking mailing " + mailing.getId() + " for deleted target groups");
		}

    	Set<Integer> targetIds = getAllTargetIdsForMailing( mailing);

    	for( int targetId : targetIds) {
    		ComTarget target = targetDao.getTarget(targetId, mailing.getCompanyID());

    		if ( target == null) {
    			if ( logger.isInfoEnabled()) {
    				logger.info( "Found non-existing target group " + targetId + ". It's assumed to be physically deleted.");
    			}

    			continue;
    		}

    		if ( target.getDeleted() != 0) {
    			if ( logger.isInfoEnabled()) {
    				logger.info( "Found deleted target group " + targetId + ".");
    			}

    			return true;
    		}
    	}

    	if ( logger.isInfoEnabled()) {
			logger.info( "Mailing " + mailing.getId() + " does not contain any deleted target groups");
		}

    	return false;
    }

	private Set<Integer> getAllTargetIdsForMailing( Mailing mailing) {

		if ( logger.isDebugEnabled()) {
			logger.debug( "Collecting target groups IDs for mailing " + mailing.getId());
		}

    	Set<Integer> targetIds = new HashSet<>();

    	targetIds.addAll(getTargetIdsFromExpression(mailing));
    	targetIds.addAll(getTargetIdsFromContent(mailing));
    	targetIds.addAll(getTargetIdsFromAttachments(mailing));

		if ( logger.isDebugEnabled()) {
			logger.debug( "Collected " + mailing.getId() + " different target group IDs in total for mailing " + mailing.getId());
		}

    	return targetIds;
	}

	@Override
    public Set<Integer> getTargetIdsFromExpression(Mailing mailing) {
		if ( logger.isDebugEnabled()) {
			logger.debug( "Collecting target groups IDs for mailing " + mailing.getId() + " from mailing target expression.");
		}

    	if (mailing == null) {
    		return new HashSet<>();
    	}

		String expression = mailing.getTargetExpression();
		Set<Integer> targetIds = TargetExpressionUtils.getTargetIds(expression);

		if ( logger.isDebugEnabled()) {
			logger.debug( "Collected " + mailing.getId() + " different target group IDs from target expression for mailing " + mailing.getId());
		}

		return targetIds;
    }

    private Set<Integer> getTargetIdsFromContent(Mailing mailing) {
		if ( logger.isDebugEnabled()) {
			logger.debug( "Collecting target groups IDs for mailing " + mailing.getId() + " from content blocks.");
		}

    	Set<Integer> targetIds = new HashSet<>();

    	for (DynamicTag tag : mailing.getDynTags().values()) {
    		for( Object contentObject : tag.getDynContent().values()) {
    			DynamicTagContent content = (DynamicTagContent) contentObject;
    			targetIds.add( content.getTargetID());
    		}
    	}

		if ( logger.isDebugEnabled()) {
			logger.debug( "Collected " + mailing.getId() + " different target group IDs from content blocks for mailing " + mailing.getId());
		}

    	return targetIds;
    }

    private Set<Integer> getTargetIdsFromAttachments(Mailing mailing) {
		if ( logger.isDebugEnabled()) {
			logger.debug( "Collecting target groups IDs for mailing " + mailing.getId() + " from attachments.");
		}

		List<MailingComponent> result = mailingComponentDao.getMailingComponents(mailing.getId(), mailing.getCompanyID(), MailingComponent.TYPE_ATTACHMENT);

    	Set<Integer> targetIds = new HashSet<>();
    	for( MailingComponent component : result) {
    		targetIds.add( component.getTargetID());
    	}

		if ( logger.isDebugEnabled()) {
			logger.debug( "Collected " + mailing.getId() + " different target group IDs from attachments for mailing " + mailing.getId());
		}

    	return targetIds;
    }

    // -------------------------------------------------------------- Dependency Injection
    /**
     * Set DAO for accessing target group data.
     *
     * @param targetDao DAO for accessing target group data
     */
    @Required
    public void setTargetDao(ComTargetDao targetDao) {
    	this.targetDao = targetDao;
    }

    /**
     * Set DAO for accessing mailing component data.
     *
     * @param mailingComponentDao DAO for accessing mailing component data
     */
    @Required
    public void setMailingComponentDao( MailingComponentDao mailingComponentDao) {
    	this.mailingComponentDao = mailingComponentDao;
    }

    /**
     * Sets DAO for accessing mailing data.
     *
     * @param mailingDao DAO for accessing mailing data
     */
	@Required
	public void setMailingDao(ComMailingDao mailingDao) {
		this.mailingDao = mailingDao;
	}

    /**
     * Set locator for target groups.
     *
     * @param locator locator for target groups
     */
    @Required
    public void setTargetGroupLocator(TargetGroupLocator locator) {
    	this.targetGroupLocator = locator;
    }

    /**
     * Sets DAO for accessing recipient data.
     *
     * @param recipientDao DAO for accessing recipient data
     */
	@Required
	public void setRecipientDao(ComRecipientDao recipientDao) {
		this.recipientDao = recipientDao;
	}

	/**
	 * Sets the EQL logic facade.
	 *
	 * @param eqlFacade EQL logic facade
	 */
	@Required
	public void setEqlFacade(EqlFacade eqlFacade) {
		this.eqlFacade = eqlFacade;
	}

	/**
	 * Checks if target group rules differ by comparing EQL code.
	 * 
	 * @param newTarget new target group
	 * @param oldTarget target group from DB
	 * 
	 * @return <code>true</code> if target groups differ
	 */
	private static final boolean isTargetGroupModified(final ComTarget newTarget, final ComTarget oldTarget) {
		return Objects.equals(newTarget.getEQL(), oldTarget.getEQL());
	}

    @Override
    public int saveTarget(ComAdmin admin, ComTarget newTarget, ComTarget target, ActionMessages errors, UserActivityLog userActivityLog) throws Exception {	// TODO: Remove "ActionMessages" to remove dependencies to Struts
    	final TargetSavingAndAnalysisResult result = saveTargetWithAnalysis(admin, newTarget, target, errors, userActivityLog);

    	return result.getTargetID();
    }

    @Override
	public TargetSavingAndAnalysisResult saveTargetWithAnalysis(ComAdmin admin, ComTarget newTarget, ComTarget target, ActionMessages errors, UserActivityLog userActivityLog) throws Exception {	// TODO: Remove "ActionMessages" to remove dependencies to Struts
        if (target == null) {
            // be sure to use id 0 if there is no existing object
            newTarget.setId(0);
        }

        if (validateTargetDefinition(admin.getCompanyID(), newTarget.getEQL())) {
			newTarget.setComplexityIndex(calculateComplexityIndex(newTarget.getEQL(), admin.getCompanyID()));

			final int newId = targetDao.saveTarget(newTarget);

			// Check for maximum "compare to"-value of gender equations
			// Must be done after saveTarget(..)-call because there the new target sql expression is generated
			if (newTarget.getTargetSQL().contains("cust.gender")) {
				final int maxGenderValue = getMaxGenderValue(admin);

				Matcher matcher = GENDER_EQUATION_PATTERN.matcher(newTarget.getTargetSQL());
				while (matcher.find()) {
					int genderValue = NumberUtils.toInt(matcher.group(1));
					if (genderValue < 0 || genderValue > maxGenderValue) {
						errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("error.gender.invalid"));
						break;
					}
				}
			} else if (newTarget.getTargetSQL().equals("1=0")) {
				errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("error.target.definition"));
			}
			newTarget.setId(newId);

			final EqlAnalysisResult analysisResultOrNull = this.eqlFacade.analyseEql(newTarget.getEQL());

			logTargetGroupSave(admin, newTarget, target, userActivityLog);

			return new TargetSavingAndAnalysisResult(newId, analysisResultOrNull);
		} else {
            errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("error.target.definition"));
            return new TargetSavingAndAnalysisResult(0, null);
        }
    }

	@Override
	public int saveTarget(ComTarget target) throws TargetGroupPersistenceException {
		try {
			eqlFacade.convertEqlToSql(target.getEQL(), target.getCompanyID());
		} catch (EqlParserException | CodeGeneratorException | ProfileFieldResolveException | ReferenceTableResolveException e) {
			throw new EqlFormatException("Error parsing EQL representation.", e);
		}
		return targetDao.saveTarget(target);
	}

	@Override
	public void bulkDelete( Set<Integer> targetIds, @VelocityCheck int companyId) throws TargetGroupPersistenceException, TargetGroupException {
		for(int targetId : targetIds) {
		    this.deleteTargetGroup(targetId, companyId);
		}
	}

	@Override
	public String getSQLFromTargetExpression(Mailing mailing, boolean appendListSplit) {
		String targetExpression = mailing.getTargetExpression();

		// if the final SQL expression should contain split-target-SQL - append it to expression and target list
		int splitId = 0;

		if (appendListSplit) {
			splitId = ((ComMailing) mailing).getSplitID();
		}

		return getSQLFromTargetExpression(targetExpression, splitId, mailing.getCompanyID());
	}

	@Override
	public String getSQLFromTargetExpression(String targetExpression, @VelocityCheck int companyId) {
		return getSQLFromTargetExpression(targetExpression, new TargetSqlCachingResolver(companyId));
	}

	@Override
	public String getSQLFromTargetExpression(String targetExpression, int splitId, @VelocityCheck int companyId) {
		if (splitId > 0) {
			if (StringUtils.isNotBlank(targetExpression)) {
				targetExpression = "(" + targetExpression + ")&" + splitId;
			} else {
				targetExpression = Integer.toString(splitId);
			}
		}

		if (StringUtils.isNotBlank(targetExpression)) {
			return getSQLFromTargetExpression(targetExpression, new TargetSqlCachingResolver(companyId));
		} else {
			return "";
		}
	}

	private String getSQLFromTargetExpression(String targetExpression, TargetSqlResolver resolver) {
    	if (StringUtils.isBlank(targetExpression)) {
    		return targetExpression;
		}

		// the regex will find all numbers, spaces and symbols "(", ")", "&", "|", "!"
		String targetExpressionRegex = "[&|()! ]|[\\d]+";

		// iterate through the tokens of target expression (we should replace targets IDs
		// with targetSQL; boolean operators - with SQL boolean operators)
		Pattern pattern = Pattern.compile(targetExpressionRegex);
		Matcher matcher = pattern.matcher(targetExpression);
		StringBuilder sqlExpression = new StringBuilder("");

		while (matcher.find()) {
			String token = matcher.group();

			switch (token) {
				case "&":
					// replace boolean operators according to SQL syntax
					sqlExpression.append(" AND ");
					break;

				case "|":
					sqlExpression.append(" OR ");
					break;

				case "!":
					sqlExpression.append(" NOT ");
					break;

				default:
					// If token is targetId - replace it with target SQL-expression
					if (StringUtils.isNumeric(token)) {
						// Wrap target SQL expression with brackets (the unwrapped SQL can break the whole SQL expression).
						sqlExpression.append('(').append(resolver.resolve(Integer.parseInt(token))).append(')');
					} else {
						// If it's "(", ")" or space - leave as it is.
						sqlExpression.append(token);
					}
			}
		}

		return sqlExpression.toString();
	}

	@Override
	public String getTargetSQL(int targetId, @VelocityCheck int companyId) {
    	String sql = targetDao.getTargetSQL(targetId, companyId);

		if (StringUtils.isBlank(sql)) {
			return null;
		}

		return sql;
	}

	@Override
	public String getTargetSQLWithSimpleIfNotExists(int targetId, @VelocityCheck int companyId) {
    	String sql = SIMPLE_TARGET_EXPRESSION;
		if (targetId > 0) {
			ComTarget targetGroup = targetDao.getTarget(targetId, companyId);
			if (targetGroup != null) {
				sql = targetGroup.getTargetSQL();
			}
		}
		return sql;
	}

	@Override
	public String getTargetSQL(int targetId, @VelocityCheck int companyId, boolean isPositive) {
		String sql = getTargetSQL(targetId, companyId);

		if (sql == null || isPositive) {
			return sql;
		} else {
			return " NOT " + sql;
		}
	}

	@Override
	public String getMailingSqlTargetExpression(int mailingId, @VelocityCheck int companyId, boolean appendListSplit) {
    	String expression = mailingDao.getTargetExpression(mailingId, companyId, appendListSplit);
    	return getSQLFromTargetExpression(expression, new TargetSqlCachingResolver(companyId));
	}

	@Override
	public ComTarget getTargetGroupOrNull(int targetId, int companyId) {
		return targetDao.getTarget(targetId, companyId);
	}

	@Override
	public final ComTarget getTargetGroup(final int targetId, final int companyId) throws UnknownTargetGroupIdException {
		if (logger.isInfoEnabled()) {
			logger.info(String.format("Retrieving target group ID %d for company ID %d", targetId, companyId));
		}

		final ComTarget result = this.targetDao.getTarget(targetId, companyId);

		if (result == null) {
			// Logged at INFO level, because this is not an internally caused error
			if (logger.isInfoEnabled()) {
				logger.info(String.format("Target ID %d not found for company ID %d", targetId, companyId));
			}

			throw new UnknownTargetGroupIdException(targetId);
		} else {
			if (logger.isInfoEnabled()) {
				logger.info(String.format("Found target group ID %d for company ID %d", targetId, companyId));
			}

			return result;
		}
	}

	@Override
	public boolean lockTargetGroup(@VelocityCheck int companyId, int targetId) {
		if (targetId > 0) {
			targetDao.updateTargetLockState(targetId, companyId, true);
			return true;
		}
		return false;
	}

	@Override
	public boolean unlockTargetGroup(@VelocityCheck int companyId, int targetId) {
		if (targetId > 0) {
			targetDao.updateTargetLockState(targetId, companyId, false);
			return true;
		}
		return false;
	}

	/**
	 * Removes recipients affected in a target group.
	 */
	@Override
	public void deleteRecipients(int targetId, @VelocityCheck int companyId) {
		ComTarget target = targetDao.getTarget(targetId, companyId);
		recipientDao.deleteRecipients(companyId, target.getTargetSQL());
	}

	@Override
	public String getTargetName(int targetId, @VelocityCheck int companyId) {
		return getTargetName(targetId, companyId, false);
	}

	@Override
	public String getTargetName(int targetId, @VelocityCheck int companyId, boolean includeDeleted) {
		return targetDao.getTargetName(targetId, companyId, includeDeleted);
	}

	private void logTargetGroupSave(ComAdmin admin, ComTarget newTarget, ComTarget target, UserActivityLog userActivityLog) {
        try {
			final String description = newTarget.getTargetName() + " (" + newTarget.getId() + ")";

            if (target == null) {
                userActivityLog.write(admin, "create target group", description + " created");
            } else {
                // Log target group changes:
                // log rule(s) changes
                if (isTargetGroupModified(newTarget, target)) {
                    userActivityLog.write(admin, "edit target group", "Changed rule in target group " + description);
                }

                // Log name changes
                if (!target.getTargetName().equals(newTarget.getTargetName())) {
                    userActivityLog.write(admin, "edit target group", target.getTargetName() +
							" (" + newTarget.getId() + ") renamed as " + newTarget.getTargetName());
                }

				final String oldDescription = StringUtils.trimToNull(target.getTargetDescription());
				final String newDescription = StringUtils.trimToNull(newTarget.getTargetDescription());

                // Log description changes
                if (!StringUtils.equals(oldDescription, newDescription)) {
					if (oldDescription == null) {
						userActivityLog.write(admin, "edit target group", description + ". Description added");
					} else {
						if (newDescription == null) {
							userActivityLog.write(admin, "edit target group", description + ". Description removed");
						} else {
							userActivityLog.write(admin, "edit target group", description + ". Description changed");
						}
					}
                }

				// Log if "For admin- and test-delivery" checkbox changed
				if (target.isAdminTestDelivery() != newTarget.isAdminTestDelivery()) {
					String state = (newTarget.isAdminTestDelivery() ? "checked" : "unchecked");
					userActivityLog.write(admin, "edit target group", newTarget.getTargetName() +
							" (" + newTarget.getId() + "). For admin- and test-delivery " + state);
				}
            }

            if (logger.isInfoEnabled()) {
                logger.info("saveTarget: save target " + newTarget.getId());
            }
        } catch (Exception e) {
            if (logger.isInfoEnabled()) {
                logger.error("Log Target Group changes error: " + e);
            }
        }
    }

	@Override
	public boolean checkIfTargetNameAlreadyExists(@VelocityCheck int companyId, String targetName, int targetId) {
		String existingName = StringUtils.defaultString(targetDao.getTargetName(targetId, companyId, true));

		// Allow to keep existing name anyway.
		return !existingName.equals(targetName) && targetDao.isTargetNameInUse(companyId, targetName, false);
	}

	@Override
	public boolean checkIfTargetNameIsValid(String targetShortname) {
		return StringUtils.isNotBlank(targetShortname) &&
				!StringUtils.equalsIgnoreCase(targetShortname.trim(), "Alle Empfänger") &&
				!StringUtils.equalsIgnoreCase(targetShortname.trim(), "Blacklist");
	}

	@Override
	public boolean isWorkflowManagerListSplit(final int companyID, final int targetID) throws UnknownTargetGroupIdException {
		ComTarget target = this.getTargetGroupOrNull(targetID, companyID);

		if (target == null || target.getId() == 0) {
			throw new UnknownTargetGroupIdException(targetID);
		}

		return target.isWorkflowManagerListSplit();
	}

	@Override
	public List<TargetLight> getWsTargetLights(final int companyId) {
		return this.targetDao.getTargetLights(companyId);
	}

	@Override
	public List<TargetLight> getTargetLights(final ComAdmin admin) {
		return getTargetLights(admin, true, true, false);
	}

	@Override
	public List<TargetLight> getTargetLights(final ComAdmin admin, boolean worldDelivery, boolean adminTestDelivery,
			boolean content) {
		return getTargetLights(admin, false, worldDelivery, adminTestDelivery, content);
	}

	@Override
	public List<TargetLight> getTargetLights(final ComAdmin admin,  boolean includeDeleted, boolean worldDelivery, boolean adminTestDelivery,
			boolean content) {
		TargetLightsOptions options = TargetLightsOptions.builder()
				.setCompanyId(admin.getCompanyID())
				.setWorldDelivery(worldDelivery)
				.setAdminTestDelivery(adminTestDelivery)
				.setIncludeDeleted(includeDeleted)
				.setContent(content)
				.build();

		return getTargetLights(options);
	}

	@Override
	public List<TargetLight> getTargetLights(TargetLightsOptions options) {
		try {
			return targetDao.getTargetLightsBySearchParameters(options);
		} catch (Exception e) {
			logger.error("Getting target light error: " + e.getMessage(), e);
		}

		return new ArrayList<>();
	}

	@Override
	public List<ListSplit> getListSplits(@VelocityCheck int companyId) {
		List<TargetLight> targets = targetDao.getSplitTargetLights(companyId, "");

		if (CollectionUtils.isNotEmpty(targets)) {
			return targets.stream()
					.map(TargetLight::toListSplit)
					.filter(split -> split != null)
					.sorted((s1, s2) -> {
						// Primary sorting criteria
						int d = s1.getParts().length - s2.getParts().length;
						if (d == 0) {
							// Secondary sorting criteria
							return s1.getPartIndex() - s2.getPartIndex();
						}
						return d;
					})
					.collect(Collectors.toList());
		}

		return Collections.emptyList();
	}

	@Override
	public int getTargetListSplitId(String splitBase, String splitPart, boolean isWmSplit) {
		if (StringUtils.isEmpty(splitBase) || StringUtils.isEmpty(splitPart)) {
            return ComMailing.NONE_SPLIT_ID;
        }

        switch (StringUtils.lowerCase(splitBase)) {
            case ComMailing.NONE_SPLIT:
                return ComMailing.NONE_SPLIT_ID;
            case ComMailing.YES_SPLIT:
                return ComMailing.YES_SPLIT_ID;
            default:
            	//nothing
        }

		String prefix = isWmSplit ? TargetLight.LIST_SPLIT_CM_PREFIX : TargetLight.LIST_SPLIT_PREFIX;
        int targetId = targetDao.getTargetSplitID(prefix + splitBase + "_" + splitPart);
        return targetId > 0 ? targetId : NONE_SPLIT_ID;
	}

	@Override
	public String getTargetSplitName(int splitId) {
		return targetDao.getTargetSplitName(splitId);
	}

	private interface TargetSqlResolver {
		String resolve(int targetId);
	}

	private class TargetSqlCachingResolver implements TargetSqlResolver {
		private int companyId;
		private Map<Integer, String> cache = new HashMap<>();

		public TargetSqlCachingResolver(int companyId) {
			this.companyId = companyId;
		}

		@Override
		public String resolve(int targetId) {
			return cache.computeIfAbsent(targetId, id -> {
				String sql = targetDao.getTargetSQL(targetId, companyId);
				// If target not found - that means that it's a list-split target.
				if (sql == null) {
					return targetDao.getTargetSQL(targetId, 0);
				}
				return sql;
			});
		}
	}

	@Override
	public final List<TargetLight> listTargetGroupsUsingProfileFieldByDatabaseName(final String fieldNameOnDatabase, @VelocityCheck final int companyID) {
		try {
			String fieldShortname = profileFieldService.translateDatabaseNameToVisibleName(companyID, fieldNameOnDatabase);
			List<RawTargetGroup> list = targetDao.listRawTargetGroups(companyID, fieldShortname);

			return list.stream()
					.filter(t -> checkTargetGroupReferencesProfileField(t, fieldShortname, companyID))
					.map(ComTargetServiceImpl::rawToTargetLight)
					.collect(Collectors.toList());
		} catch (ProfileFieldException e) {
			return Collections.emptyList();
		}
	}

	@Override
	public final List<TargetLight> listTargetGroupsUsingReferenceTable(final String tableName, @VelocityCheck final int companyID) {
		final List<RawTargetGroup> list = targetDao.listRawTargetGroups(companyID, tableName);

		return list.stream()
				.filter(t -> checkTargetGroupReferencesReferenceTable(t, tableName, companyID))
				.map(ComTargetServiceImpl::rawToTargetLight)
				.collect(Collectors.toList());
	}

	@Override
	public final List<TargetLight> listTargetGroupsUsingReferenceTableColumn(final String tableName, final String columnName, @VelocityCheck final int companyID) {
		final List<RawTargetGroup> list = targetDao.listRawTargetGroups(companyID, tableName, columnName);

		return list.stream()
				.filter(t -> checkTargetGroupReferencesReferenceTableColumn(t, tableName, columnName, companyID))
				.map(ComTargetServiceImpl::rawToTargetLight)
				.collect(Collectors.toList());
	}

	@Override
	public String toViewUri(int targetId) {
		return UriComponentsBuilder.fromPath("/targetQB.do")
				.queryParam("method", "show")
				.queryParam("targetID", targetId)
				.toUriString();
	}

	/**
	 * Utility method to create {@link TargetLight} from {@link RawTargetGroup}.
	 *
	 * @param raw {@link RawTargetGroup}
	 *
	 * @return TargetLight created from raw target group
	 */
	private static final TargetLight rawToTargetLight(final RawTargetGroup raw) {
		final TargetLight light = new TargetLightImpl();

		light.setId(raw.getId());
		light.setTargetName(raw.getName());

		return light;
	}

	/**
	 * Checks if given target group references given profile field.
	 *
	 * @param targetGroup target group to check
	 * @param fieldShortname visible name of profile field
	 * @param companyID company ID of target group
	 *
	 * @return <code>true</code> if target group references given profile field
	 */
	private final boolean checkTargetGroupReferencesProfileField(final RawTargetGroup targetGroup, final String fieldShortname, final int companyID) {
		try {
			final String eql = normalizeToEQL(targetGroup.getEql(), companyID);
			final SimpleReferenceCollector collector = new SimpleReferenceCollector();

			this.eqlFacade.convertEqlToSql(eql, companyID, collector);

			return collector.getReferencedProfileFields().stream().anyMatch(fieldShortname::equalsIgnoreCase);
		} catch(final EqlParserException |
                CodeGeneratorException |
                ProfileFieldResolveException |
                ReferenceTableResolveException e) {
			return false;	// Invalid target group
		}
    }

	private final boolean checkTargetGroupReferencesReferenceTable(final RawTargetGroup targetGroup, final String tableName, final int companyID) {
		try {
			final String eql = normalizeToEQL(targetGroup.getEql(), companyID);
			final SimpleReferenceCollector collector = new SimpleReferenceCollector();

			this.eqlFacade.convertEqlToSql(eql, companyID, collector);

			return collector.getReferencedReferenceTables().contains(tableName.toLowerCase());
		} catch(final EqlParserException |
                CodeGeneratorException |
                ReferenceTableResolveException |
                ProfileFieldResolveException e) {
			return false;	// Invalid target group
		}
    }

	private final boolean checkTargetGroupReferencesReferenceTableColumn(final RawTargetGroup targetGroup, final String tableName, final String columnName, final int companyID) {
		try {
			final String eql = normalizeToEQL(targetGroup.getEql(), companyID);
			final SimpleReferenceCollector collector = new SimpleReferenceCollector();

			this.eqlFacade.convertEqlToSql(eql, companyID, collector);

			return collector.getReferencedRefTableColumns().stream()
					.anyMatch(ref -> ref.getTable().equalsIgnoreCase(tableName) && ref.getColumn().equalsIgnoreCase(columnName));
		} catch(final EqlParserException |
                ReferenceTableResolveException |
                CodeGeneratorException |
                ProfileFieldResolveException e) {
			return false;	// Invalid target group
		}
    }
	/**
	 * Returns an EQL expression for given information.
	 *
	 * The conversion rule is:
	 * <ol>
	 *   <li>If a non-empty EQL expression is given, this expression is returned.</li>
	 *   <li>An empty EQL expression is returned as fallback.</li>
	 * </ol>
	 *
	 * @param eql EQL expression from target group
	 * @param companyID company ID of target group
	 *
	 * @return EQL expression representing target group settings
	 */
	private final String normalizeToEQL(final String eql, final int companyID) {
		if (eql != null) {
			return eql;
		} else {
			return "";
		}
	}

	@Override
	public final RecipientTargetGroupMatcher createRecipientTargetGroupMatcher(final int customerID, final int companyID) throws Exception {
		final Interpreter beanShellInterpreter = this.beanShellInterpreterFactory.createBeanShellInterpreter(companyID, customerID);

		return new BeanShellRecipientTargetGroupMatcher(companyID, beanShellInterpreter, this.eqlFacade);
	}

	@Override
	public final RecipientTargetGroupMatcher createRecipientTargetGroupMatcher(final Map<String, Object> recipientData, final int companyID) throws Exception {
		final Interpreter beanShellInterpreter = this.beanShellInterpreterFactory.createBeanShellInterpreter(companyID, recipientData, columnInfoService.getColumnInfoMap(companyID));

		return new BeanShellRecipientTargetGroupMatcher(companyID, beanShellInterpreter, this.eqlFacade);
	}

	@Override
	public List<TargetLight> getTargetLights(@VelocityCheck int companyId, Collection<Integer> targetGroups, boolean includeDeleted) {
		return targetDao.getTargetLights(companyId, targetGroups, includeDeleted);
	}

	@Override
	public List<TargetLight> getSplitTargetLights(@VelocityCheck int companyId, String splitType) {
		return targetDao.getSplitTargetLights(companyId, splitType);
	}

	@Override
	public PaginatedListImpl<Dependent<TargetGroupDependentType>> getDependents(@VelocityCheck int companyId, int targetId,
                                                                                Set<TargetGroupDependentType> allowedTypes, int pageNumber,
                                                                                int pageSize, String sortColumn, String order) {
		return targetDao.getDependents(companyId, targetId, allowedTypes, pageNumber, pageSize, sortColumn, order);
	}

	@Override
	public Map<Integer, TargetComplexityGrade> getTargetComplexities(int companyId) {
		Map<Integer, Integer> complexityIndicesMap = targetDao.getTargetComplexityIndices(companyId);
		Map<Integer, TargetComplexityGrade> complexityGradeMap = new HashMap<>(complexityIndicesMap.size());

		int recipientsCount = recipientDao.getNumberOfRecipients(companyId);

		complexityIndicesMap.forEach((targetId, complexityIndex) ->
			complexityGradeMap.put(targetId, TargetUtils.getComplexityGrade(complexityIndex, recipientsCount))
		);

		return complexityGradeMap;
	}

	@Override
	public TargetComplexityGrade getTargetComplexityGrade(@VelocityCheck int companyId, int targetId) {
		Integer complexityIndex = targetDao.getTargetComplexityIndex(companyId, targetId);

		if (complexityIndex == null || complexityIndex < 0) {
			return null;
		}

		return TargetUtils.getComplexityGrade(complexityIndex, recipientDao.getNumberOfRecipients(companyId));
	}

	@Override
	public int calculateComplexityIndex(String eql, @VelocityCheck int companyId) {
		return calculateComplexityIndex(eql, companyId, new TargetComplexityEvaluationCacheImpl());
	}

	@Override
	public int calculateComplexityIndex(String eql, @VelocityCheck int companyId, TargetComplexityEvaluationCache cache) {
		try {
			return complexityEvaluator.evaluate(eql, companyId, cache);
		} catch (Exception e) {
			logger.error("Target group complexity index evaluation failed", e);
			return 0;
		}
	}

	@Override
	public void initializeComplexityIndex(@VelocityCheck int companyId) {
		Map<Integer, Integer> complexities = new HashMap<>();
		TargetComplexityEvaluationCache cache = new TargetComplexityEvaluationCacheImpl();

		for (Pair<Integer, String> pair : targetDao.getTargetsToInitializeComplexityIndices(companyId)) {
			try {
				complexities.put(pair.getFirst(), calculateComplexityIndex(pair.getSecond(), companyId, cache));
			} catch (Exception e) {
				logger.error("Error occurred: " + e.getMessage(), e);
			}
		}

		targetDao.saveComplexityIndices(companyId, complexities);
	}

	@Override
	public List<TargetLight> getAccessLimitationTargetLights(final ComAdmin admin) {
		return Collections.emptyList();
	}

	@Override
	public boolean isBasicFullTextSearchSupported(){
		return targetDao.isBasicFullTextSearchSupported();
	}

	@Override
	public boolean isRecipientMatchTarget(ComAdmin admin, int targetGroupId, Recipient recipient) {
		try {
			String targetExpression = targetDao.getTargetSQL(targetGroupId, admin.getCompanyID());
			if (StringUtils.isEmpty(targetExpression)) {
				return true;
			}
			return recipientDao.isNotSavedRecipientDataMatchTarget(admin.getCompanyID(), targetExpression, recipient);
		} catch (Exception e) {
			logger.error("Error occurs while checking if recipient match target group: " + e.getMessage(), e);
		}

		return false;
	}

	@Override
	public boolean isRecipientMatchTarget(ComAdmin admin, int targetGroupId, int customerId) {
		try {
			String targetExpression = targetDao.getTargetSQL(targetGroupId, admin.getCompanyID());
			if (StringUtils.isEmpty(targetExpression)) {
				return true;
			}
			return recipientDao.isRecipientMatchTarget(admin.getCompanyID(), targetExpression, customerId);
		} catch (Exception e) {
			logger.error("Error occurs while checking if recipient match target group: " + e.getMessage(), e);
		}

		return false;
	}

	/**
	 * Sets the service handling profile fields.
	 *
	 * @param service service handling profile fields
	 */
	@Required
	public final void setProfileFieldService(final ProfileFieldService service) {
		this.profileFieldService = Objects.requireNonNull(service, "Profile field service cannot be null");
	}

	@Required
	public final void setBeanShellInterpreterFactory(final BeanShellInterpreterFactory factory) {
		this.beanShellInterpreterFactory = Objects.requireNonNull(factory, "BeanShellInterpreterFactory is null");
	}

	@Required
	public void setTargetComplexityEvaluator(TargetComplexityEvaluator complexityEvaluator) {
		this.complexityEvaluator = complexityEvaluator;
	}

	@Required
	public void setColumnInfoService(ColumnInfoService columnInfoService) {
		this.columnInfoService = columnInfoService;
	}

	private int getMaxGenderValue(ComAdmin admin) {
		if (admin.permissionAllowed(Permission.RECIPIENT_GENDER_EXTENDED)) {
			return ConfigService.MAX_GENDER_VALUE_EXTENDED;
		} else {
			return ConfigService.MAX_GENDER_VALUE_BASIC;
		}
	}

	private boolean validateTargetDefinition(int companyId, String eql) {
		try {
			eqlFacade.convertEqlToSql(eql, companyId);
			return true;
		} catch (Exception e) {
			logger.error("Error occurred: " + e.getMessage(), e);
			return false;
		}
	}
}
