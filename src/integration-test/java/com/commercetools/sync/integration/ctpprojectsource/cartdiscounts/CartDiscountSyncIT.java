package com.commercetools.sync.integration.ctpprojectsource.cartdiscounts;

import com.commercetools.sync.cartdiscounts.CartDiscountSync;
import com.commercetools.sync.cartdiscounts.CartDiscountSyncOptions;
import com.commercetools.sync.cartdiscounts.CartDiscountSyncOptionsBuilder;
import com.commercetools.sync.cartdiscounts.helpers.CartDiscountSyncStatistics;
import io.sphere.sdk.cartdiscounts.AbsoluteCartDiscountValue;
import io.sphere.sdk.cartdiscounts.CartDiscount;
import io.sphere.sdk.cartdiscounts.CartDiscountDraft;
import io.sphere.sdk.cartdiscounts.CartDiscountDraftBuilder;
import io.sphere.sdk.cartdiscounts.CartDiscountValue;
import io.sphere.sdk.cartdiscounts.CartPredicate;
import io.sphere.sdk.cartdiscounts.ShippingCostTarget;
import io.sphere.sdk.cartdiscounts.commands.updateactions.ChangeCartPredicate;
import io.sphere.sdk.cartdiscounts.commands.updateactions.ChangeTarget;
import io.sphere.sdk.cartdiscounts.commands.updateactions.ChangeValue;
import io.sphere.sdk.cartdiscounts.commands.updateactions.SetCustomType;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.types.CustomFieldsDraft;
import io.sphere.sdk.types.Type;
import io.sphere.sdk.utils.MoneyImpl;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static com.commercetools.sync.cartdiscounts.utils.CartDiscountReferenceReplacementUtils.buildCartDiscountQuery;
import static com.commercetools.sync.cartdiscounts.utils.CartDiscountReferenceReplacementUtils.replaceCartDiscountsReferenceIdsWithKeys;
import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.integration.commons.utils.CartDiscountITUtils.createCartDiscountCustomType;
import static com.commercetools.sync.integration.commons.utils.CartDiscountITUtils.deleteCartDiscountsFromTargetAndSource;
import static com.commercetools.sync.integration.commons.utils.CartDiscountITUtils.populateSourceProject;
import static com.commercetools.sync.integration.commons.utils.CartDiscountITUtils.populateTargetProject;
import static com.commercetools.sync.integration.commons.utils.ITUtils.deleteTypesFromTargetAndSource;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static io.sphere.sdk.models.DefaultCurrencyUnits.EUR;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;

class CartDiscountSyncIT {

    @BeforeEach
    void setup() {
        deleteCartDiscountsFromTargetAndSource();
        deleteTypesFromTargetAndSource();
        populateSourceProject();
        populateTargetProject();
    }

    @AfterAll
    static void tearDown() {
        deleteCartDiscountsFromTargetAndSource();
        deleteTypesFromTargetAndSource();
    }

    @Test
    void sync_WithoutUpdates_ShouldReturnProperStatistics() {
        // preparation
        final List<CartDiscount> cartDiscounts = CTP_SOURCE_CLIENT
            .execute(buildCartDiscountQuery())
            .toCompletableFuture().join().getResults();

        final List<CartDiscountDraft> cartDiscountDrafts = replaceCartDiscountsReferenceIdsWithKeys(cartDiscounts);

        final List<String> errorMessages = new ArrayList<>();
        final List<Throwable> exceptions = new ArrayList<>();

        final CartDiscountSyncOptions cartDiscountSyncOptions = CartDiscountSyncOptionsBuilder
            .of(CTP_TARGET_CLIENT)
            .errorCallback((error, throwable) -> {
                errorMessages.add(error);
                exceptions.add(throwable);
            })
            .build();

        final CartDiscountSync cartDiscountSync = new CartDiscountSync(cartDiscountSyncOptions);

        // test
        final CartDiscountSyncStatistics cartDiscountSyncStatistics = cartDiscountSync
            .sync(cartDiscountDrafts)
            .toCompletableFuture().join();

        // assertion
        assertThat(errorMessages).isEmpty();
        assertThat(exceptions).isEmpty();
        assertThat(cartDiscountSyncStatistics).hasValues(2, 1, 0, 0);
        assertThat(cartDiscountSyncStatistics
            .getReportMessage())
            .isEqualTo("Summary: 2 cart discounts were processed in total"
                + " (1 created, 0 updated and 0 failed to sync).");

    }

    @Test
    void sync_WithUpdates_ShouldReturnProperStatistics() {
        // preparation
        final List<CartDiscount> cartDiscounts = CTP_SOURCE_CLIENT
            .execute(buildCartDiscountQuery())
            .toCompletableFuture().join().getResults();
        final String newTypeKey = "new-type";
        createCartDiscountCustomType(newTypeKey, Locale.ENGLISH, newTypeKey, CTP_SOURCE_CLIENT);
        final Type newTargetCustomType = createCartDiscountCustomType(newTypeKey, Locale.ENGLISH, newTypeKey,
            CTP_TARGET_CLIENT);

        final List<CartDiscountDraft> cartDiscountsReferenceIdsWithKeys =
            replaceCartDiscountsReferenceIdsWithKeys(cartDiscounts);

        // Apply some changes
        final List<CartDiscountDraft> cartDiscountDrafts = cartDiscountsReferenceIdsWithKeys
            .stream()
            .map(draft -> CartDiscountDraftBuilder
                .of(draft)
                .cartPredicate(CartPredicate.of("totalPrice >= \"100 EUR\""))
                .value(AbsoluteCartDiscountValue.of(MoneyImpl.of(40, EUR)))
                .target(ShippingCostTarget.of())
                .custom(CustomFieldsDraft.ofTypeIdAndJson(newTypeKey, emptyMap()))
                .build())
            .collect(Collectors.toList());

        final List<String> errorMessages = new ArrayList<>();
        final List<Throwable> exceptions = new ArrayList<>();
        final List<UpdateAction<CartDiscount>> updateActionsList = new ArrayList<>();

        final CartDiscountSyncOptions cartDiscountSyncOptions = CartDiscountSyncOptionsBuilder
            .of(CTP_TARGET_CLIENT)
            .errorCallback((error, throwable) -> {
                errorMessages.add(error);
                exceptions.add(throwable);
            })
            .beforeUpdateCallback((updateActions, newCartDiscount, oldCartDiscount) -> {
                updateActionsList.addAll(updateActions);
                return updateActions;
            })
            .build();

        final CartDiscountSync cartDiscountSync = new CartDiscountSync(cartDiscountSyncOptions);

        // test
        final CartDiscountSyncStatistics cartDiscountSyncStatistics = cartDiscountSync
            .sync(cartDiscountDrafts)
            .toCompletableFuture().join();

        // assertion
        assertThat(errorMessages).isEmpty();
        assertThat(exceptions).isEmpty();
        assertThat(updateActionsList).containsExactly(
            ChangeValue.of(CartDiscountValue.ofAbsolute(Collections.singletonList(MoneyImpl.of(40, EUR)))),
            ChangeCartPredicate.of("totalPrice >= \"100 EUR\""),
            ChangeTarget.of(ShippingCostTarget.of()),
            SetCustomType.ofTypeIdAndJson(newTargetCustomType.getId(), emptyMap())
        );
        assertThat(cartDiscountSyncStatistics).hasValues(2, 1, 1, 0);
        assertThat(cartDiscountSyncStatistics
            .getReportMessage())
            .isEqualTo("Summary: 2 cart discounts were processed in total"
                + " (1 created, 1 updated and 0 failed to sync).");
    }
}
