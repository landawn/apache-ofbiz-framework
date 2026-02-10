/*
 Licensed to the Apache Software Foundation (ASF) under one
 or more contributor license agreements.  See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership.  The ASF licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.
 */

package org.apache.ofbiz.content.blog;

import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.content.content.ContentWorker;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.persistence.dao.ContentAssocDao;
import org.apache.ofbiz.persistence.dao.ContentDao;
import org.apache.ofbiz.persistence.dao.DaoRegistry;
import org.apache.ofbiz.persistence.entity.ContentAssocEntity;
import org.apache.ofbiz.persistence.entity.ContentEntity;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.Beans;
import com.rometools.rome.feed.synd.SyndContent;
import com.rometools.rome.feed.synd.SyndContentImpl;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndEntryImpl;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.feed.synd.SyndFeedImpl;


import org.apache.ofbiz.persistence.entity.x;
import org.apache.ofbiz.model.ServiceContext;
import org.apache.ofbiz.model.BlogRssServicesContext;
/**
 * BlogRssServices
 */
public class BlogRssServices {

    private static final String MODULE = BlogRssServices.class.getName();
    private static final String RESOURCE = x.ContentUiLabels;
    public static final String MIME_TYPE_ID = x.text_html;
    public static final String MAP_KEY = x.SUMMARY;

    public static Map<String, Object> generateBlogRssFeed(DispatchContext dctx, BlogRssServicesContext context) {
        GenericValue userLogin = (GenericValue) context.get(x.userLogin);
        String contentId = (String) context.get(x.blogContentId);
        String entryLink = (String) context.get(x.entryLink);
        String feedType = (String) context.get(x.feedType);
        Locale locale = (Locale) context.get(x.locale);

        // create the main link
        String mainLink = (String) context.get(x.mainLink);
        mainLink = mainLink + x.blogContentId_01fdada5 + contentId;

        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();

        // get the main blog content
        GenericValue content = null;
        try {
            ContentDao contentDao = DaoRegistry.getDao(delegator, x.Content, ContentDao.class);
            ContentEntity contentEntity = contentDao.get(contentId).orElse(null);
            if (contentEntity != null) {
                content = delegator.makeValue(x.Content, Beans.beanToMap(contentEntity));
            }
        } catch (Exception e) {
            Debug.logError(e, MODULE);
        }

        if (content == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    x.ContentCannotGenerateBlogRssFeed,
                    UtilMisc.toMap(x.contentId, contentId), locale));
        }

        // create the feed
        SyndFeed feed = new SyndFeedImpl();
        feed.setFeedType(feedType);
        feed.setLink(mainLink);

        feed.setTitle(content.getString(x.contentName));
        feed.setDescription(content.getString(x.description));
        feed.setEntries(generateEntryList(dispatcher, delegator, contentId, entryLink, locale, userLogin));

        Map<String, Object> resp = ServiceUtil.returnSuccess();
        resp.put(x.wireFeed, feed.createWireFeed());
        return resp;
    }

    public static List<SyndEntry> generateEntryList(LocalDispatcher dispatcher, Delegator delegator, String contentId,
                                                    String entryLink, Locale locale, GenericValue userLogin) {
        List<SyndEntry> entries = new LinkedList<>();

        List<GenericValue> contentRecs = null;
        try {
            ContentAssocDao contentAssocDao = DaoRegistry.getDao(delegator, x.ContentAssoc, ContentAssocDao.class);
            ContentDao contentDao = DaoRegistry.getDao(delegator, x.Content, ContentDao.class);
            List<ContentAssocEntity> contentAssocEntities = contentAssocDao.list(Filters.and(
                    Filters.eq(x.contentId, contentId),
                    Filters.eq(x.contentAssocTypeId, x.PUBLISH_LINK)));
            contentAssocEntities.sort(Comparator.comparing(ContentAssocEntity::getFromDate, Comparator.nullsLast(Comparator.reverseOrder())));

            contentRecs = new LinkedList<>();
            for (ContentAssocEntity contentAssocEntity : contentAssocEntities) {
                ContentEntity contentEntity = contentDao.get(contentAssocEntity.getContentIdTo()).orElse(null);
                if (contentEntity == null || !x.CTNT_PUBLISHED.equals(contentEntity.getStatusId())) {
                    continue;
                }

                Map<String, Object> contentAssocViewToFields = new HashMap<>(Beans.beanToMap(contentEntity));
                contentAssocViewToFields.put(x.contentIdStart, contentAssocEntity.getContentId());
                contentAssocViewToFields.put(x.contentIdTo, contentAssocEntity.getContentIdTo());
                contentAssocViewToFields.put(x.caContentAssocTypeId, contentAssocEntity.getContentAssocTypeId());
                contentAssocViewToFields.put(x.caFromDate, contentAssocEntity.getFromDate());

                contentRecs.add(delegator.makeValue(x.ContentAssocViewTo, contentAssocViewToFields));
            }
        } catch (Exception e) {
            Debug.logError(e, MODULE);
        }

        if (contentRecs != null) {
            for (GenericValue v : contentRecs) {
                String sub = null;
                try {
                    Map<String, Object> dummy = new HashMap<>();
                    sub = ContentWorker.renderSubContentAsText(dispatcher, v.getString(x.contentId), MAP_KEY, dummy, locale, MIME_TYPE_ID, true);
                } catch (GeneralException | IOException e) {
                    Debug.logError(e, MODULE);
                }
                if (sub != null) {
                    String thisLink = entryLink + x.articleContentId + v.getString(x.contentId) + x.blogContentId_03c87393 + contentId;
                    SyndContent desc = new SyndContentImpl();
                    desc.setType(x.text_plain);
                    desc.setValue(sub);

                    SyndEntry entry = new SyndEntryImpl();
                    entry.setTitle(v.getString(x.contentName));
                    entry.setPublishedDate(v.getTimestamp(x.createdDate));
                    entry.setDescription(desc);
                    entry.setLink(thisLink);
                    entry.setAuthor((v.getString(x.createdByUserLogin)));
                    entries.add(entry);
                }
            }
        }

        return entries;
    }
}
