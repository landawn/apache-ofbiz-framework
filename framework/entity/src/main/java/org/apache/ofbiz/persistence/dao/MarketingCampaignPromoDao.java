package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.MarketingCampaignPromoEntity;

public interface MarketingCampaignPromoDao extends CrudDao<MarketingCampaignPromoEntity, MarketingCampaignPromoEntity, SQLBuilder.PSC, MarketingCampaignPromoDao> {
}
