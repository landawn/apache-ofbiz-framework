package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.MarketingCampaignEntity;

public interface MarketingCampaignDao extends CrudDao<MarketingCampaignEntity, String, SQLBuilder.PSC, MarketingCampaignDao> {
}
