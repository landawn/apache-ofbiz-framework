package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.MarketingCampaignPromoEntity;

public interface MarketingCampaignPromoDao extends CrudDao<MarketingCampaignPromoEntity, MarketingCampaignPromoEntity, SqlBuilder.PSC, MarketingCampaignPromoDao> {
}
