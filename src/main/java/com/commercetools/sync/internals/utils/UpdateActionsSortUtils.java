package com.commercetools.sync.internals.utils;

import io.sphere.sdk.categories.Category;
import io.sphere.sdk.products.commands.updateactions.ChangeAssetName;
import io.sphere.sdk.products.commands.updateactions.SetAssetCustomField;
import io.sphere.sdk.products.commands.updateactions.SetAssetDescription;
import io.sphere.sdk.products.commands.updateactions.SetAssetSources;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.commands.updateactions.AddAsset;
import io.sphere.sdk.products.commands.updateactions.AddExternalImage;
import io.sphere.sdk.products.commands.updateactions.AddPrice;
import io.sphere.sdk.products.commands.updateactions.AddVariant;
import io.sphere.sdk.products.commands.updateactions.ChangeAssetOrder;
import io.sphere.sdk.products.commands.updateactions.ChangeMasterVariant;
import io.sphere.sdk.products.commands.updateactions.ChangePrice;
import io.sphere.sdk.products.commands.updateactions.MoveImageToPosition;
import io.sphere.sdk.products.commands.updateactions.RemoveAsset;
import io.sphere.sdk.products.commands.updateactions.RemoveImage;
import io.sphere.sdk.products.commands.updateactions.RemovePrice;
import io.sphere.sdk.products.commands.updateactions.RemoveVariant;
import io.sphere.sdk.products.commands.updateactions.SetAssetCustomType;
import io.sphere.sdk.products.commands.updateactions.SetAssetTags;
import io.sphere.sdk.products.commands.updateactions.SetAttribute;
import io.sphere.sdk.products.commands.updateactions.SetAttributeInAllVariants;
import io.sphere.sdk.products.commands.updateactions.SetProductPriceCustomField;
import io.sphere.sdk.products.commands.updateactions.SetProductPriceCustomType;
import io.sphere.sdk.products.commands.updateactions.SetSku;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * This class is only meant for the internal use of the commercetools-sync-java library.
 */
public final class UpdateActionsSortUtils {


    /**
     * Given a list of update actions, this method returns a copy of the supplied list but sorted with the following
     * precedence:
     * <ol>
     * <li>{@link RemoveVariant} only if it's a non master variant</li>
     * <li>Variant Change actions. Check: {@link #isVariantChangeAction}</li>
     * <li>{@link AddVariant}</li>
     * <li>{@link ChangeMasterVariant}</li>
     * <li>{@link RemoveVariant} only if it's a master variant</li>
     * </ol>
     *
     * <p>This is to ensure that there are no conflicts when updating variants.
     *
     * @param updateActions list of update actions to sort.
     * @return a new sorted list of update actions.
     */
    @Nonnull
    public static List<UpdateAction<Product>> sortVariantActions(
        @Nonnull final List<UpdateAction<Product>> updateActions,
        @Nonnull final Integer masterVariantId) {

        final List<UpdateAction<Product>> actionsCopy = new ArrayList<>(updateActions);
        actionsCopy.sort((action1, action2) -> {
            if (isNonMasterVariantRemoveAction(action1, masterVariantId)
                && !isNonMasterVariantRemoveAction(action2, masterVariantId)) {
                return -1;
            }

            if (!isNonMasterVariantRemoveAction(action1, masterVariantId)
                && isNonMasterVariantRemoveAction(action2, masterVariantId)) {
                return 1;
            }

            if (!(isMasterVariantRemoveAction(action1, masterVariantId))
                && isMasterVariantRemoveAction(action2, masterVariantId)) {
                return -1;
            }

            if (isMasterVariantRemoveAction(action1, masterVariantId)
                && !isMasterVariantRemoveAction(action2, masterVariantId)) {
                return 1;
            }

            if (!(action1 instanceof ChangeMasterVariant) && action2 instanceof ChangeMasterVariant) {
                return -1;
            }

            if (action1 instanceof ChangeMasterVariant && !(action2 instanceof ChangeMasterVariant)) {
                return 1;
            }

            if (isVariantChangeAction(action1) && !isVariantChangeAction(action2)) {
                return -1;
            }

            if (!isVariantChangeAction(action1) && isVariantChangeAction(action2)) {
                return 1;
            }

            if (!(action1 instanceof AddVariant) && action2 instanceof AddVariant) {
                return -1;
            }

            if (action1 instanceof AddVariant && !(action2 instanceof AddVariant)) {
                return 1;
            }

            return 0;
        });
        return actionsCopy;
    }

    private static boolean isNonMasterVariantRemoveAction(@Nonnull final UpdateAction<Product> action,
                                                          @Nonnull final Integer masterVariantId) {
        return action instanceof RemoveVariant
            && !Objects.equals(((RemoveVariant) action).getId(), masterVariantId);
    }

    private static boolean isMasterVariantRemoveAction(@Nonnull final UpdateAction<Product> action,
                                                       @Nonnull final Integer masterVariantId) {
        return action instanceof RemoveVariant
            && Objects.equals(((RemoveVariant) action).getId(), masterVariantId);
    }

    private static boolean isVariantChangeAction(@Nonnull final UpdateAction<Product> action) {
        return isAttributeAction(action)
            || isImageAction(action)
            || isPriceAction(action)
            || isVariantAssetAction(action)
            || action instanceof SetSku;
    }

    private static boolean isVariantAssetAction(@Nonnull final UpdateAction<Product> action) {
        return action instanceof RemoveAsset
            || action instanceof ChangeAssetName
            || action instanceof SetAssetDescription
            || action instanceof SetAssetTags
            || action instanceof SetAssetSources
            || action instanceof SetAssetCustomField
            || action instanceof SetAssetCustomType
            || action instanceof ChangeAssetOrder
            || action instanceof AddAsset;
    }

    private static boolean isPriceAction(@Nonnull final UpdateAction<Product> action) {
        return action instanceof RemovePrice
            || action instanceof ChangePrice
            || action instanceof SetProductPriceCustomField
            || action instanceof SetProductPriceCustomType
            || action instanceof AddPrice;
    }

    private static boolean isImageAction(@Nonnull final UpdateAction<Product> action) {
        return action instanceof RemoveImage
            || action instanceof AddExternalImage
            || action instanceof MoveImageToPosition;
    }

    private static boolean isAttributeAction(@Nonnull final UpdateAction<Product> action) {
        return isSetAttribute(action) || isUnSetAttribute(action);
    }

    private static boolean isSetAttribute(@Nonnull final UpdateAction<Product> action) {
        return (action instanceof SetAttribute
            && Objects.nonNull(((SetAttribute) action).getValue()))
            || (action instanceof SetAttributeInAllVariants
            && Objects.nonNull(((SetAttributeInAllVariants) action).getValue()));
    }

    private static boolean isUnSetAttribute(@Nonnull final UpdateAction<Product> action) {
        return (action instanceof SetAttribute
            && Objects.isNull(((SetAttribute) action).getValue()))
            || (action instanceof SetAttributeInAllVariants
            && Objects.isNull(((SetAttributeInAllVariants) action).getValue()));
    }

    /**
     * Given a list of update actions, this method returns a copy of the supplied list but sorted with the following
     * precedence:
     * <ol>
     * <li>{@link SetAttribute} OR {@link SetAttributeInAllVariants} with null values (unsets the attribute)</li>
     * <li>{@link SetAttribute} or {@link SetAttributeInAllVariants} with non-null values (sets the attribute)</li>
     * </ol>
     *
     * <p>This is to ensure that there are no conflicts when adding a new attribute. So we first issue all the unset
     * actions, then we issue the set actions.
     *
     * @param updateActions list of update actions to sort.
     * @return a new sorted list of update actions.
     */
    @Nonnull
    public static List<UpdateAction<Product>> sortAttributeActions(
        @Nonnull final List<UpdateAction<Product>> updateActions) {

        final List<UpdateAction<Product>> actionsCopy = new ArrayList<>(updateActions);
        actionsCopy.sort((action1, action2) -> {
            if (isUnSetAttribute(action1) && !isUnSetAttribute(action2)) {
                return -1;
            }

            if (!isUnSetAttribute(action1) && isUnSetAttribute(action2)) {
                return 1;
            }
            return 0;
        });
        return actionsCopy;
    }

    /**
     * Given a list of update actions, this method returns a copy of the supplied list but sorted with the following
     * precedence:
     * <ol>
     * <li>{@link RemoveImage}</li>
     * <li>{@link AddExternalImage}</li>
     * <li>{@link MoveImageToPosition}</li>
     * </ol>
     *
     * <p>This is to ensure that there are no conflicts when adding a new image that might have a duplicate value for
     * a unique field, which could already be changed or removed. We move the image after adding it, since there is
     * no way to add the image at a certain index and moving an image doesn't require that the image already exists on
     * CTP.
     *
     * @param updateActions list of update actions to sort.
     * @return a new sorted list of update actions.
     */
    @Nonnull
    public static List<UpdateAction<Product>> sortImageActions(
        @Nonnull final List<UpdateAction<Product>> updateActions) {

        final List<UpdateAction<Product>> actionsCopy = new ArrayList<>(updateActions);
        actionsCopy.sort((action1, action2) -> {
            if (action1 instanceof RemoveImage && !(action2 instanceof RemoveImage)) {
                return -1;
            }

            if (!(action1 instanceof RemoveImage) && action2 instanceof RemoveImage) {
                return 1;
            }

            if (!(action1 instanceof MoveImageToPosition) && action2 instanceof MoveImageToPosition) {
                return -1;
            }

            if (action1 instanceof MoveImageToPosition && !(action2 instanceof MoveImageToPosition)) {
                return 1;
            }

            return 0;
        });
        return actionsCopy;
    }

    /**
     * Given a list of update actions, this method returns a copy of the supplied list but sorted with the following
     * precedence:
     * <ol>
     * <li>{@link RemovePrice}</li>
     * <li>{@link ChangePrice} or {@link SetProductPriceCustomType} or {@link SetProductPriceCustomField}</li>
     * <li>{@link AddPrice}</li>
     * </ol>
     *
     * <p>This is to ensure that there are no conflicts when adding a new price that might have a duplicate value for
     * a unique field, which could already be changed or removed.
     *
     * @param updateActions list of update actions to sort.
     * @return a new sorted list of update actions (remove, change, add).
     */
    @Nonnull
    public static List<UpdateAction<Product>> sortPriceActions(
        @Nonnull final List<UpdateAction<Product>> updateActions) {

        final List<UpdateAction<Product>> actionsCopy = new ArrayList<>(updateActions);
        actionsCopy.sort((action1, action2) -> {
            if (action1 instanceof RemovePrice && !(action2 instanceof RemovePrice)) {
                return -1;
            }

            if (!(action1 instanceof RemovePrice) && action2 instanceof RemovePrice) {
                return 1;
            }

            if (!(action1 instanceof AddPrice) && action2 instanceof AddPrice) {
                return -1;
            }

            if (action1 instanceof AddPrice && !(action2 instanceof AddPrice)) {
                return 1;
            }

            return 0;
        });
        return actionsCopy;
    }

    /**
     * Given a list of update actions, this method returns a copy of the supplied list but sorted with the following
     * precedence:
     * <ol>
     * <li>{@link RemoveAsset}</li> {@link ChangeAssetName} OR {@link SetAssetDescription} OR {@link SetAssetTags} OR
     * {@link SetAssetSources} OR {@link SetAssetCustomField} OR {@link SetAssetCustomType}</li>
     * <li>{@link ChangeAssetOrder}</li>
     * <li>{@link AddAsset}</li>
     * </ol>
     *
     * <p>This is to ensure that there are no conflicts when adding a new asset that might have a duplicate value for
     * a unique field, which could already be changed or removed. It is important to have a changeAssetOrder action
     * before an addAsset action, since changeAssetOrder requires asset ids for sorting them, and new assets don't have
     * ids yet since they are generated by CTP after an asset is created. Therefore, first set the correct order, then
     * we add the asset at the correct index.
     *
     * @param updateActions list of update actions to sort.
     * @return a new sorted list of update actions.
     */
    @Nonnull
    public static List<UpdateAction<Product>> sortProductVariantAssetActions(
        @Nonnull final List<UpdateAction<Product>> updateActions) {

        final List<UpdateAction<Product>> actionsCopy = new ArrayList<>(updateActions);
        actionsCopy.sort((action1, action2) -> {
            if (action1 instanceof RemoveAsset && !(action2 instanceof RemoveAsset)) {
                return -1;
            }

            if (!(action1 instanceof RemoveAsset) && action2 instanceof RemoveAsset) {
                return 1;
            }

            if (!(action1 instanceof AddAsset) && action2 instanceof AddAsset) {
                return -1;
            }

            if (action1 instanceof AddAsset && !(action2 instanceof AddAsset)) {
                return 1;
            }

            if (!(action1 instanceof ChangeAssetOrder) && action2 instanceof ChangeAssetOrder) {
                return -1;
            }

            if (action1 instanceof ChangeAssetOrder && !(action2 instanceof ChangeAssetOrder)) {
                return 1;
            }

            return 0;
        });
        return actionsCopy;
    }

    /**
     * Given a list of update actions, this method returns a copy of the supplied list but sorted with the following
     * precedence:
     * <ol>
     * <li>{@link io.sphere.sdk.categories.commands.updateactions.RemoveAsset}</li>
     * {@link io.sphere.sdk.categories.commands.updateactions.ChangeAssetName} OR
     * {@link io.sphere.sdk.categories.commands.updateactions.SetAssetDescription} OR
     * {@link io.sphere.sdk.categories.commands.updateactions.SetAssetTags} OR
     * {@link io.sphere.sdk.categories.commands.updateactions.SetAssetSources} OR
     * {@link io.sphere.sdk.categories.commands.updateactions.SetAssetCustomField} OR
     * {@link io.sphere.sdk.categories.commands.updateactions.SetAssetCustomType}</li>
     * <li>{@link io.sphere.sdk.categories.commands.updateactions.ChangeAssetOrder}</li>
     * <li>{@link io.sphere.sdk.categories.commands.updateactions.AddAsset}</li>
     * </ol>
     *
     * <p>This is to ensure that there are no conflicts when adding a new asset that might have a duplicate value for
     * a unique field, which could already be changed or removed. It is important to have a changeAssetOrder action
     * before an addAsset action, since changeAssetOrder requires asset ids for sorting them, and new assets don't have
     * ids yet since they are generated by CTP after an asset is created. Therefore, first set the correct order, then
     * we add the asset at the correct index.
     *
     * @param updateActions list of update actions to sort.
     * @return a new sorted list of update actions.
     */
    @Nonnull
    public static List<UpdateAction<Category>> sortCategoryAssetActions(
        @Nonnull final List<UpdateAction<Category>> updateActions) {

        final List<UpdateAction<Category>> actionsCopy = new ArrayList<>(updateActions);
        actionsCopy.sort((action1, action2) -> {
            if (action1 instanceof io.sphere.sdk.categories.commands.updateactions.RemoveAsset
                && !(action2 instanceof io.sphere.sdk.categories.commands.updateactions.RemoveAsset)) {
                return -1;
            }

            if (!(action1 instanceof io.sphere.sdk.categories.commands.updateactions.RemoveAsset)
                && action2 instanceof io.sphere.sdk.categories.commands.updateactions.RemoveAsset) {
                return 1;
            }

            if (!(action1 instanceof io.sphere.sdk.categories.commands.updateactions.AddAsset)
                && action2 instanceof io.sphere.sdk.categories.commands.updateactions.AddAsset) {
                return -1;
            }

            if (action1 instanceof io.sphere.sdk.categories.commands.updateactions.AddAsset
                && !(action2 instanceof io.sphere.sdk.categories.commands.updateactions.AddAsset)) {
                return 1;
            }

            if (!(action1 instanceof io.sphere.sdk.categories.commands.updateactions.ChangeAssetOrder)
                && action2 instanceof io.sphere.sdk.categories.commands.updateactions.ChangeAssetOrder) {
                return -1;
            }

            if (action1 instanceof io.sphere.sdk.categories.commands.updateactions.ChangeAssetOrder
                && !(action2 instanceof io.sphere.sdk.categories.commands.updateactions.ChangeAssetOrder)) {
                return 1;
            }

            return 0;
        });
        return actionsCopy;
    }

    private UpdateActionsSortUtils() {
    }
}
