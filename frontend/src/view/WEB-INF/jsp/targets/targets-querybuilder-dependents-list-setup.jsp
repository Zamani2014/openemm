<%@ page language="java" contentType="text/html; charset=utf-8" errorPage="/error.do" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://struts.apache.org/tags-bean" prefix="bean" %>
<%@ taglib uri="http://struts.apache.org/tags-html" prefix="html" %>
<%@ taglib prefix="emm" uri="https://emm.agnitas.de/jsp/jsp/common" %>

<%--@elvariable id="editTargetForm" type="com.agnitas.emm.core.target.web.QueryBuilderTargetGroupForm"--%>

<c:set var="isTabsMenuShown" 		value="true" 									scope="request" />

<c:set var="agnNavigationKey" 		value="TargetQBEdit" 							scope="request" />
<emm:instantiate var="agnNavHrefParams" type="java.util.LinkedHashMap" scope="request">
    <c:set target="${agnNavHrefParams}" property="target-id" value="${editTargetForm.targetID}"/>
</emm:instantiate>

<c:set var="agnTitleKey" 			value="Target" 									scope="request" />
<c:set var="agnSubtitleKey" 		value="Target" 									scope="request" />
<c:set var="sidemenu_active" 		value="Targetgroups" 							scope="request" />
<c:set var="sidemenu_sub_active" 	value="none" 									scope="request" />
<c:set var="agnHighlightKey" 		value="default.usedIn" 							scope="request" />
<c:set var="isBreadcrumbsShown" 	value="true" 									scope="request" />
<c:set var="agnBreadcrumbsRootKey" 	value="Targetgroups" 							scope="request" />
<c:set var="agnHelpKey" 			value="targetGroupView" 						scope="request" />

<emm:instantiate var="agnBreadcrumbs" type="java.util.LinkedHashMap" scope="request">
    <emm:instantiate var="agnBreadcrumb" type="java.util.LinkedHashMap">
        <c:set target="${agnBreadcrumbs}" property="0" value="${agnBreadcrumb}"/>
        <c:set target="${agnBreadcrumb}" property="textKey" value="default.Overview"/>
        <c:set target="${agnBreadcrumb}" property="url">
            <c:url value="/target/list.action" />
        </c:set>
    </emm:instantiate>

    <emm:instantiate var="agnBreadcrumb" type="java.util.LinkedHashMap">
        <c:set target="${agnBreadcrumbs}" property="1" value="${agnBreadcrumb}"/>
        <c:set target="${agnBreadcrumb}" property="text" value="${editTargetForm.shortname}"/>
    </emm:instantiate>
</emm:instantiate>

<jsp:useBean id="itemActionsSettings" class="java.util.LinkedHashMap" scope="request">
    <c:if test="${not empty editTargetForm.workflowForwardParams}">
        <jsp:useBean id="element" class="java.util.LinkedHashMap" scope="request">
            <c:set target="${itemActionsSettings}" property="4" value="${element}"/>
            <c:set target="${element}" property="btnCls" value="btn btn-secondary btn-regular"/>
            <c:set target="${element}" property="iconBefore" value="icon-angle-left"/>
            <c:set target="${element}" property="type" value="href"/>
            <c:set target="${element}" property="url">
                <c:url value="/workflow/${editTargetForm.workflowId}/view.action">
                    <c:param name="forwardParams" value="${editTargetForm.workflowForwardParams};elementValue=${editTargetForm.targetID}"/>
                </c:url>
            </c:set>
            <c:set target="${element}" property="name">
                <bean:message key="button.Back"/>
            </c:set>
        </jsp:useBean>
    </c:if>
</jsp:useBean>
