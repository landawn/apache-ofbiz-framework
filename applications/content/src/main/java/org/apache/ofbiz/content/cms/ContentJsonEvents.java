/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/
package org.apache.ofbiz.content.cms;

import java.io.IOException;
import java.nio.charset.Charset;
import java.sql.Timestamp;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.ofbiz.base.lang.JSON;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericEntityNotFoundException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.transaction.TransactionUtil;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.entity.util.EntityQuery;

import org.apache.ofbiz.persistence.entity.x;
public class ContentJsonEvents {

    public static final int CONTENT_NAME_MAX_LENGTH = 27;

    public static String getContentAssocs(HttpServletRequest request, HttpServletResponse response) throws GenericEntityException, IOException {
        Delegator delegator = (Delegator) request.getAttribute("delegator");

        String contentId = request.getParameter("contentId");

        EntityCondition condition = EntityCondition.makeCondition(
                EntityCondition.makeCondition(UtilMisc.toMap("contentId", contentId)),
                EntityUtil.getFilterByDateExpr());
        List<GenericValue> assocs = delegator.findList("ContentAssoc", condition, null, null, null, false);

        List<Map<String, Object>> nodes = new LinkedList<>();
        for (GenericValue assoc : assocs) {
            nodes.add(getTreeNode(assoc));
        }

        nodes.sort((node1, node2) -> {
            Map<String, Object> data1 = UtilGenerics.cast(node1.get("data"));
            Map<String, Object> data2 = UtilGenerics.cast(node2.get("data"));
            if (data1 == null || data2 == null) {
                return 0;
            }

            String title1 = (String) data1.get("title");
            String title2 = (String) data2.get("title");
            if (title1 == null || title2 == null) {
                return 0;
            }

            return title1.toLowerCase(Locale.getDefault()).compareTo(title2.toLowerCase(Locale.getDefault()));
        });
        IOUtils.write(JSON.from(nodes).toString(), response.getOutputStream(), Charset.defaultCharset());

        return "success";
    }

    public static String moveContent(HttpServletRequest request, HttpServletResponse response) throws GenericEntityException, IOException {
        final Delegator delegator = (Delegator) request.getAttribute("delegator");

        final String contentIdTo = request.getParameter("contentIdTo");
        final String contentIdFrom = request.getParameter("contentIdFrom");
        final String contentIdFromNew = request.getParameter("contentIdFromNew");
        final String contentAssocTypeId = request.getParameter("contentAssocTypeId");
        final Timestamp fromDate = Timestamp.valueOf(request.getParameter("fromDate"));

        final Timestamp now = UtilDateTime.nowTimestamp();
        GenericValue assoc = TransactionUtil.inTransaction(() -> {
            GenericValue oldAssoc = EntityQuery.use(delegator).from("ContentAssoc")
                    .where("contentIdTo", contentIdTo, "contentId", contentIdFrom, "contentAssocTypeId",
                           contentAssocTypeId, "fromDate", fromDate)
                    .queryOne();
            if (oldAssoc == null) {
                throw new GenericEntityNotFoundException("Could not find ContentAssoc by primary key [contentIdTo: $contentIdTo, contentId: "
                        + "$contentIdFrom, contentAssocTypeId: $contentAssocTypeId, fromDate: $fromDate]");
            }
            GenericValue newAssoc = (GenericValue) oldAssoc.clone();

            oldAssoc.set(x.thruDate, now);
            oldAssoc.store();

            newAssoc.set(x.contentId, contentIdFromNew);
            newAssoc.set(x.fromDate, now);
            newAssoc.set(x.thruDate, null);
            delegator.clearCacheLine(delegator.create(newAssoc));

            return newAssoc;
        }, String.format("move content [%s] from [%s] to [%s]", contentIdTo, contentIdFrom, contentIdFromNew), 0, true).call();

        IOUtils.write(JSON.from(getTreeNode(assoc)).toString(), response.getOutputStream(), Charset.defaultCharset());

        return "success";
    }

    public static String deleteContent(HttpServletRequest request, HttpServletResponse response) throws GenericEntityException {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        String contentId = request.getParameter("contentId");

        deleteContent(delegator, contentId);

        return "success";
    }

    public static void deleteContent(Delegator delegator, String contentId) throws GenericEntityException {
        Timestamp now = UtilDateTime.nowTimestamp();
        EntityCondition condition = EntityCondition.makeCondition(
                EntityCondition.makeCondition(UtilMisc.toMap("contentIdTo", contentId)),
                EntityUtil.getFilterByDateExpr());
        List<GenericValue> assocs = delegator.findList("ContentAssoc", condition, null, null, null, true);
        for (GenericValue assoc : assocs) {
            assoc.set(x.thruDate, now);
            delegator.store(assoc);
        }
        deleteWebPathAliases(delegator, contentId);
    }

    private static void deleteWebPathAliases(Delegator delegator, String contentId) throws GenericEntityException {
        Timestamp now = UtilDateTime.nowTimestamp();
        EntityCondition condition = EntityCondition.makeCondition(
                EntityCondition.makeCondition(UtilMisc.toMap("contentId", contentId)),
                EntityUtil.getFilterByDateExpr());
        List<GenericValue> pathAliases = delegator.findList("WebSitePathAlias", condition, null, null, null, true);
        for (GenericValue alias : pathAliases) {
            alias.set(x.thruDate, now);
            delegator.store(alias);
        }
        List<GenericValue> subContents = delegator.findList("ContentAssoc", condition, null, null, null, true);
        for (GenericValue subContentAssoc : subContents) {
            deleteWebPathAliases(delegator, subContentAssoc.getString(x.contentIdTo));
        }
    }

    private static Map<String, Object> getTreeNode(GenericValue assoc) throws GenericEntityException {
        GenericValue content = assoc.getRelatedOne(x.ToContent, true);
        String contentName = assoc.getString(x.contentIdTo);
        if (content != null && content.getString(x.contentName) != null) {
            contentName = content.getString(x.contentName);
            if (contentName.length() > CONTENT_NAME_MAX_LENGTH) {
                contentName = contentName.substring(0, CONTENT_NAME_MAX_LENGTH);
            }
        }

        Map<String, Object> data = UtilMisc.toMap("title", (Object) contentName);

        Map<String, Object> attr = UtilMisc.toMap(
                "id", assoc.get(x.contentIdTo),
                "contentId", assoc.get(x.contentId),
                "fromDate", assoc.getTimestamp(x.fromDate).toString(),
                "contentAssocTypeId", assoc.get(x.contentAssocTypeId));

        Map<String, Object> node = UtilMisc.toMap("data", (Object) data, "attr", (Object) attr);

        List<GenericValue> assocChildren = content != null ? content.getRelated(x.FromContentAssoc, null, null, true) : null;
        assocChildren = EntityUtil.filterByDate(assocChildren);
        if (!CollectionUtils.isEmpty(assocChildren)) {
            node.put("state", "closed");
        }
        return node;
    }
}
