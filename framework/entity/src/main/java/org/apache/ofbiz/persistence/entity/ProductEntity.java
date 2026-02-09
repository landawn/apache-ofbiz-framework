package org.apache.ofbiz.persistence.entity;

import com.landawn.abacus.annotation.Column;
import com.landawn.abacus.annotation.Entity;
import com.landawn.abacus.annotation.Id;
import com.landawn.abacus.annotation.Table;
import java.math.BigDecimal;
import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "PRODUCT")
@Table(name = "PRODUCT")
public class ProductEntity {
    @Id
    @Column(name = "PRODUCT_ID")
    private String productId;

    @Column(name = "PRODUCT_TYPE_ID")
    private String productTypeId;

    @Column(name = "PRIMARY_PRODUCT_CATEGORY_ID")
    private String primaryProductCategoryId;

    @Column(name = "FACILITY_ID")
    private String facilityId;

    @Column(name = "INTRODUCTION_DATE")
    private Timestamp introductionDate;

    @Column(name = "RELEASE_DATE")
    private Timestamp releaseDate;

    @Column(name = "SUPPORT_DISCONTINUATION_DATE")
    private Timestamp supportDiscontinuationDate;

    @Column(name = "SALES_DISCONTINUATION_DATE")
    private Timestamp salesDiscontinuationDate;

    @Column(name = "SALES_DISC_WHEN_NOT_AVAIL")
    private String salesDiscWhenNotAvail;

    @Column(name = "INTERNAL_NAME")
    private String internalName;

    @Column(name = "BRAND_NAME")
    private String brandName;

    @Column(name = "COMMENTS")
    private String comments;

    @Column(name = "PRODUCT_NAME")
    private String productName;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "LONG_DESCRIPTION")
    private String longDescription;

    @Column(name = "PRICE_DETAIL_TEXT")
    private String priceDetailText;

    @Column(name = "SMALL_IMAGE_URL")
    private String smallImageUrl;

    @Column(name = "MEDIUM_IMAGE_URL")
    private String mediumImageUrl;

    @Column(name = "LARGE_IMAGE_URL")
    private String largeImageUrl;

    @Column(name = "DETAIL_IMAGE_URL")
    private String detailImageUrl;

    @Column(name = "ORIGINAL_IMAGE_URL")
    private String originalImageUrl;

    @Column(name = "DETAIL_SCREEN")
    private String detailScreen;

    @Column(name = "INVENTORY_MESSAGE")
    private String inventoryMessage;

    @Column(name = "INVENTORY_ITEM_TYPE_ID")
    private String inventoryItemTypeId;

    @Column(name = "REQUIRE_INVENTORY")
    private String requireInventory;

    @Column(name = "QUANTITY_UOM_ID")
    private String quantityUomId;

    @Column(name = "QUANTITY_INCLUDED")
    private BigDecimal quantityIncluded;

    @Column(name = "PIECES_INCLUDED")
    private BigDecimal piecesIncluded;

    @Column(name = "REQUIRE_AMOUNT")
    private String requireAmount;

    @Column(name = "FIXED_AMOUNT")
    private BigDecimal fixedAmount;

    @Column(name = "AMOUNT_UOM_TYPE_ID")
    private String amountUomTypeId;

    @Column(name = "WEIGHT_UOM_ID")
    private String weightUomId;

    @Column(name = "SHIPPING_WEIGHT")
    private BigDecimal shippingWeight;

    @Column(name = "PRODUCT_WEIGHT")
    private BigDecimal productWeight;

    @Column(name = "HEIGHT_UOM_ID")
    private String heightUomId;

    @Column(name = "PRODUCT_HEIGHT")
    private BigDecimal productHeight;

    @Column(name = "SHIPPING_HEIGHT")
    private BigDecimal shippingHeight;

    @Column(name = "WIDTH_UOM_ID")
    private String widthUomId;

    @Column(name = "PRODUCT_WIDTH")
    private BigDecimal productWidth;

    @Column(name = "SHIPPING_WIDTH")
    private BigDecimal shippingWidth;

    @Column(name = "DEPTH_UOM_ID")
    private String depthUomId;

    @Column(name = "PRODUCT_DEPTH")
    private BigDecimal productDepth;

    @Column(name = "SHIPPING_DEPTH")
    private BigDecimal shippingDepth;

    @Column(name = "DIAMETER_UOM_ID")
    private String diameterUomId;

    @Column(name = "PRODUCT_DIAMETER")
    private BigDecimal productDiameter;

    @Column(name = "PRODUCT_RATING")
    private BigDecimal productRating;

    @Column(name = "RATING_TYPE_ENUM")
    private String ratingTypeEnum;

    @Column(name = "RETURNABLE")
    private String returnable;

    @Column(name = "TAXABLE")
    private String taxable;

    @Column(name = "CHARGE_SHIPPING")
    private String chargeShipping;

    @Column(name = "AUTO_CREATE_KEYWORDS")
    private String autoCreateKeywords;

    @Column(name = "INCLUDE_IN_PROMOTIONS")
    private String includeInPromotions;

    @Column(name = "IS_VIRTUAL")
    private String isVirtual;

    @Column(name = "IS_VARIANT")
    private String isVariant;

    @Column(name = "VIRTUAL_VARIANT_METHOD_ENUM")
    private String virtualVariantMethodEnum;

    @Column(name = "ORIGIN_GEO_ID")
    private String originGeoId;

    @Column(name = "REQUIREMENT_METHOD_ENUM_ID")
    private String requirementMethodEnumId;

    @Column(name = "BILL_OF_MATERIAL_LEVEL")
    private BigDecimal billOfMaterialLevel;

    @Column(name = "RESERV_MAX_PERSONS")
    private BigDecimal reservMaxPersons;

    @Column(name = "RESERV2ND_P_P_PERC")
    private BigDecimal reserv2ndPPPerc;

    @Column(name = "RESERV_NTH_P_P_PERC")
    private BigDecimal reservNthPPPerc;

    @Column(name = "CONFIG_ID")
    private String configId;

    @Column(name = "CREATED_DATE")
    private Timestamp createdDate;

    @Column(name = "CREATED_BY_USER_LOGIN")
    private String createdByUserLogin;

    @Column(name = "LAST_MODIFIED_DATE")
    private Timestamp lastModifiedDate;

    @Column(name = "LAST_MODIFIED_BY_USER_LOGIN")
    private String lastModifiedByUserLogin;

    @Column(name = "IN_SHIPPING_BOX")
    private String inShippingBox;

    @Column(name = "DEFAULT_SHIPMENT_BOX_TYPE_ID")
    private String defaultShipmentBoxTypeId;

    @Column(name = "LOT_ID_FILLED_IN")
    private String lotIdFilledIn;

    @Column(name = "ORDER_DECIMAL_QUANTITY")
    private String orderDecimalQuantity;
}
