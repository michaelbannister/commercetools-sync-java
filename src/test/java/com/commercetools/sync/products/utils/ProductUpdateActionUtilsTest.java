package com.commercetools.sync.products.utils;

import com.commercetools.sync.products.ProductSyncOptions;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.Image;
import io.sphere.sdk.products.PriceDraft;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductData;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.ProductVariant;
import io.sphere.sdk.products.ProductVariantDraft;
import io.sphere.sdk.products.ProductVariantDraftBuilder;
import io.sphere.sdk.products.ProductVariantDraftDsl;
import io.sphere.sdk.products.attributes.AttributeDraft;
import io.sphere.sdk.products.commands.updateactions.AddVariant;
import io.sphere.sdk.products.commands.updateactions.ChangeMasterVariant;
import io.sphere.sdk.products.commands.updateactions.RemoveVariant;
import io.sphere.sdk.products.commands.updateactions.SetSku;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.commercetools.sync.commons.utils.CollectionUtils.collectionToMap;
import static com.commercetools.sync.products.ProductSyncMockUtils.createProductDraftFromJson;
import static com.commercetools.sync.products.ProductSyncMockUtils.createProductFromJson;
import static com.commercetools.sync.products.utils.ProductUpdateActionUtils.buildAddVariantUpdateActionFromDraft;
import static com.commercetools.sync.products.utils.ProductUpdateActionUtils.buildChangeMasterVariantUpdateAction;
import static com.commercetools.sync.products.utils.ProductUpdateActionUtils.buildRemoveVariantUpdateActions;
import static com.commercetools.sync.products.utils.ProductUpdateActionUtils.buildVariantsUpdateActions;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;


public class ProductUpdateActionUtilsTest {

    public static final String OLD_PROD_WITH_VARIANTS =
            "com/commercetools/sync/products/utils/productVariantUpdateActionUtils/productOld.json";
    public static final String NEW_PROD_DRAFT_WITH_VARIANTS =
            "com/commercetools/sync/products/utils/productVariantUpdateActionUtils/productDraftNew.json";

    @Test
    public void buildVariantsUpdateActions_makesListOfUpdateActions() throws Exception {
        ProductSyncOptions productSyncOptions = mock(ProductSyncOptions.class);
        Product productOld = createProductFromJson(OLD_PROD_WITH_VARIANTS);
        ProductDraft productDraftNew = createProductDraftFromJson(NEW_PROD_DRAFT_WITH_VARIANTS);
        List<UpdateAction<Product>> updateActions =
            buildVariantsUpdateActions(productOld, productDraftNew, productSyncOptions, emptyMap());

        // check remove variants are the first in the list
        assertThat(updateActions.subList(0, 3))
            .contains(RemoveVariant.of(1), RemoveVariant.of(2), RemoveVariant.of(3));

        ProductVariantDraft draftMaster = productDraftNew.getMasterVariant();
        ProductVariantDraft draft5 = productDraftNew.getVariants().get(1);
        ProductVariantDraft draft6 = productDraftNew.getVariants().get(2);
        assertThat(updateActions).contains(
            buildAddVariantUpdateActionFromDraft(draftMaster),
            buildAddVariantUpdateActionFromDraft(draft5),
            buildAddVariantUpdateActionFromDraft(draft6));

        assertThat(updateActions).containsOnlyOnce(SetSku.of(4, "var-44-sku", true));

        // change master variant must be always after variants are added/updated,
        // because it set by SKU, we should be sure the master variant is already added and SKUs are actual
        assertThat(updateActions.indexOf(ChangeMasterVariant.ofSku("var-7-sku", true)))
            .isEqualTo(updateActions.size() - 1);
    }

    @Test
    public void buildRemoveVariantUpdateAction_removesMissedVariants() throws Exception {
        Product productOld = createProductFromJson(OLD_PROD_WITH_VARIANTS);
        ProductDraft productDraftNew = createProductDraftFromJson(NEW_PROD_DRAFT_WITH_VARIANTS);

        ProductData oldStaged = productOld.getMasterData().getStaged();
        Map<String, ProductVariant> oldVariants =
            collectionToMap(oldStaged.getAllVariants(), ProductVariant::getKey);

        ArrayList<ProductVariantDraft> newVariants = new ArrayList<>(productDraftNew.getVariants());
        newVariants.add(productDraftNew.getMasterVariant());

        List<RemoveVariant> updateActions = buildRemoveVariantUpdateActions(oldVariants, newVariants);
        assertThat(updateActions)
            .containsExactlyInAnyOrder(RemoveVariant.of(1), RemoveVariant.of(2), RemoveVariant.of(3));
        // removes master (1) and two other variants (2, 3)
    }

    @Test
    public void buildChangeMasterVariantUpdateAction_changesMasterVariant() throws Exception {
        Product productOld = createProductFromJson(OLD_PROD_WITH_VARIANTS);
        ProductDraft productDraftNew = createProductDraftFromJson(NEW_PROD_DRAFT_WITH_VARIANTS);

        List<ChangeMasterVariant> changeMasterVariant =
                buildChangeMasterVariantUpdateAction(productOld, productDraftNew);
        assertThat(changeMasterVariant).hasSize(1);
        assertThat(changeMasterVariant.get(0))
                .isEqualTo(ChangeMasterVariant.ofSku(productDraftNew.getMasterVariant().getSku(), true));
    }

    @Test
    public void buildAddVariantUpdateActionFromDraft_returnsProperties() throws Exception {
        List<AttributeDraft> attributeList = Collections.emptyList();
        List<PriceDraft> priceList = Collections.emptyList();
        List<Image> imageList = Collections.emptyList();
        ProductVariantDraftDsl draft = ProductVariantDraftBuilder.of()
                .attributes(attributeList)
                .prices(priceList)
                .sku("testSKU")
                .key("testKey")
                .images(imageList)
                .build();

        AddVariant addVariant = buildAddVariantUpdateActionFromDraft(draft);
        assertThat(addVariant.getAttributes()).isSameAs(attributeList);
        assertThat(addVariant.getPrices()).isSameAs(priceList);
        assertThat(addVariant.getSku()).isEqualTo("testSKU");
        assertThat(addVariant.getKey()).isEqualTo("testKey");
        assertThat(addVariant.getImages()).isSameAs(imageList);
    }
}