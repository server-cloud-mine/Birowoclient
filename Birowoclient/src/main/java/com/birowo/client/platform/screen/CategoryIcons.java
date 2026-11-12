package com.birowo.client.platform.screen;

import java.util.EnumMap;
import java.util.Map;

import org.joml.Matrix3x2fStack;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import com.birowo.client.core.feature.FeatureCategory;

/**
 * The picture that stands for a category in the header of its panel.
 *
 * <p>The pictures are the items of the game itself, so that the header of a panel shows the same
 * thing the player would find in their own inventory and no new asset has to be made.
 *
 * <p>The stacks are made once and kept, because the interface is drawn again on every frame and
 * a stack must not be built again and again.
 */
public final class CategoryIcons {
	/**
	 * The width and the height of an item picture, in pixels.
	 */
	public static final int ITEM_SIZE = 16;

	private static final Map<FeatureCategory, ItemStack> STACKS = new EnumMap<>(FeatureCategory.class);

	static {
		STACKS.put(FeatureCategory.COMBAT, stackOf(Items.DIAMOND_SWORD));
		STACKS.put(FeatureCategory.MOVEMENT, stackOf(Items.FEATHER));
		STACKS.put(FeatureCategory.RENDER, stackOf(Items.SPYGLASS));
		STACKS.put(FeatureCategory.CLIENT, stackOf(Items.NETHER_STAR));
		STACKS.put(FeatureCategory.MISC, stackOf(Items.COMPASS));
	}

	private CategoryIcons() {
	}

	/**
	 * @param category the category to draw a picture for
	 * @return the item that stands for that category
	 */
	public static ItemStack stackFor(FeatureCategory category) {
		return STACKS.get(category);
	}

	/**
	 * Draws the picture of a category.
	 *
	 * <p>An item is drawn as a square of sixteen pixels, so the matrix of the screen is scaled
	 * around it to make it the size the panel is drawn at. The picture is drawn in the layer of the
	 * items of the game, so it always sits on top of the panel it belongs to.
	 *
	 * <p>An item cannot be made transparent, because the game draws it in a layer of its own.
	 * The picture is therefore made to grow out of the middle of its place instead, so that it
	 * comes and goes with the panel it belongs to.
	 *
	 * @param context  the draw context of the current frame
	 * @param category the category to draw a picture for
	 * @param x        the left edge of the place of the picture, in scaled GUI pixels
	 * @param y        the top edge of the place of the picture, in scaled GUI pixels
	 * @param scale    the scale the panel is drawn at
	 * @param alpha    the opacity of the panel, between zero and one
	 */
	public static void draw(DrawContext context, FeatureCategory category, int x, int y, float scale, float alpha) {
		ItemStack stack = STACKS.get(category);

		if (stack == null || alpha <= 0.02F) {
			return;
		}

		float size = scale * Math.min(alpha, 1.0F);
		float centerX = x + ITEM_SIZE * scale / 2.0F;
		float centerY = y + ITEM_SIZE * scale / 2.0F;

		Matrix3x2fStack matrices = context.getMatrices();

		matrices.pushMatrix();
		matrices.translate(centerX, centerY);
		matrices.scale(size, size);
		matrices.translate(-ITEM_SIZE / 2.0F, -ITEM_SIZE / 2.0F);
		context.drawItem(stack, 0, 0);
		matrices.popMatrix();
	}

	/**
	 * @param item the item to make a stack of
	 * @return a stack of that item
	 */
	private static ItemStack stackOf(Item item) {
		return new ItemStack(item);
	}
}
