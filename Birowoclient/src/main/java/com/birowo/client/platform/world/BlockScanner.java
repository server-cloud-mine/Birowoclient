package com.birowo.client.platform.world;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;

import com.birowo.client.core.util.NeighbourFaces;

/**
 * Looks through the chunks of the world for the blocks that are worth showing.
 *
 * <p>A chunk is made of sections of sixteen by sixteen by sixteen blocks. A section that holds
 * none of the blocks that are looked for is skipped without being read, because that check only
 * walks through the few kinds of block the section is made of.
 *
 * <p>For every block that is found, the sides that are covered by a block of the same kind are
 * remembered as well. The overlay uses them to leave out the edges and the faces that are hidden
 * inside a group of blocks, which is what makes a vein of ore look like one shape instead of a
 * heap of boxes.
 *
 * <p>The scanner is not meant to be run over the whole world at once: a chunk holds thousands of
 * blocks. The caller decides how many chunks are looked through in one tick.
 */
public final class BlockScanner {
	/**
	 * The height, the width and the depth of a section, in blocks.
	 */
	private static final int SECTION_SIZE = 16;

	private BlockScanner() {
	}

	/**
	 * One block that was found.
	 *
	 * @param block the kind of the block, which is what the colour of the block is looked up by
	 * @param pos   the place of the block, in world coordinates
	 * @param box   the outline of the block, in world coordinates
	 * @param faces the sides of the block that are covered by a block of the same kind
	 */
	public record Found(Block block, BlockPos pos, Box box, int faces) {
		/**
		 * @return the middle of the block, in world coordinates
		 */
		public Vec3d centre() {
			return new Vec3d((box.minX + box.maxX) / 2.0D, (box.minY + box.maxY) / 2.0D,
					(box.minZ + box.maxZ) / 2.0D);
		}
	}

	/**
	 * Looks through one chunk for the blocks that are wanted.
	 *
	 * <p>Only the chunk itself is read, so that this may be done by a thread of its own: reading
	 * the world from another thread may make the game load a chunk, which is a thing that only
	 * the thread of the game may do.
	 *
	 * @param chunk  the chunk that is looked through
	 * @param wanted the blocks that are looked for
	 * @param bottom the height of the bottom of the world
	 * @return the places of the blocks that were found
	 */
	public static List<BlockPos> find(Chunk chunk, Set<Block> wanted, int bottom) {
		List<BlockPos> positions = new ArrayList<>();
		ChunkPos position = chunk.getPos();
		BlockPos.Mutable cursor = new BlockPos.Mutable();
		ChunkSection[] sections = chunk.getSectionArray();

		for (int index = 0; index < sections.length; index++) {
			ChunkSection section = sections[index];

			if (section.isEmpty() || !section.hasAny(state -> wanted.contains(state.getBlock()))) {
				continue;
			}

			int baseY = bottom + index * SECTION_SIZE;

			for (int y = 0; y < SECTION_SIZE; y++) {
				for (int x = 0; x < SECTION_SIZE; x++) {
					for (int z = 0; z < SECTION_SIZE; z++) {
						BlockState state = section.getBlockState(x, y, z);

						if (!wanted.contains(state.getBlock())) {
							continue;
						}

						cursor.set(position.getStartX() + x, baseY + y, position.getStartZ() + z);
						positions.add(cursor.toImmutable());
					}
				}
			}
		}

		return positions;
	}

	/**
	 * Works out what is to be shown for the blocks that were found.
	 *
	 * <p>The shape of a block and the sides of it that are covered are read from the world, so
	 * this is done by the thread of the game.
	 *
	 * @param world     the world the blocks are in
	 * @param positions the places of the blocks that were found
	 * @param fullCubes which kinds of block fill their whole block, kept between the scans
	 * @return the blocks that were found, in world coordinates
	 */
	public static List<Found> describe(ClientWorld world, List<BlockPos> positions,
			Map<Block, Boolean> fullCubes) {
		List<Found> found = new ArrayList<>(positions.size());

		for (BlockPos pos : positions) {
			BlockState state = world.getBlockState(pos);

			found.add(new Found(state.getBlock(), pos, outline(world, pos, state, fullCubes),
					coveredFaces(world, pos, state, fullCubes)));
		}

		return found;
	}

	/**
	 * The outline of one block.
	 *
	 * @param world     the world the block is in
	 * @param pos       the place of the block, in world coordinates
	 * @param state     the state of the block
	 * @param fullCubes which kinds of block fill their whole block, kept between the scans
	 * @return the outline of the block, in world coordinates
	 */
	private static Box outline(ClientWorld world, BlockPos pos, BlockState state, Map<Block, Boolean> fullCubes) {
		if (isFullCube(world, pos, state, fullCubes)) {
			// A block that fills its whole block is drawn as a plain box, so its shape is never
			// asked for. Almost every block that is worth showing is a plain box, and the shape
			// of a block is the most expensive part of looking through a chunk.
			return new Box(pos);
		}

		VoxelShape shape = state.getOutlineShape(world, pos);

		if (shape.isEmpty()) {
			return new Box(pos);
		}

		return shape.getBoundingBox().offset(pos);
	}

	/**
	 * The sides of a block that are covered by a block of the same kind.
	 *
	 * <p>Two blocks are only joined when their shapes touch: a chest that stands on another chest
	 * does not touch it, because the shape of a chest stops short of the top of the block.
	 *
	 * @param world     the world the block is in
	 * @param pos       the place of the block
	 * @param state     the state of the block
	 * @param fullCubes which kinds of block fill their whole block, kept between the scans
	 * @return the sides that are covered, as bits
	 */
	private static int coveredFaces(ClientWorld world, BlockPos pos, BlockState state, Map<Block, Boolean> fullCubes) {
		Block block = state.getBlock();
		boolean cube = isFullCube(world, pos, state, fullCubes);
		int faces = 0;

		for (Direction direction : Direction.values()) {
			BlockPos neighbourPos = pos.offset(direction);
			BlockState neighbourState = world.getBlockState(neighbourPos);

			if (neighbourState.getBlock() != block) {
				continue;
			}

			if (cube && isFullCube(world, neighbourPos, neighbourState, fullCubes)) {
				// Two blocks that both fill their whole block always touch each other.
				faces |= bitOf(direction);
				continue;
			}

			VoxelShape shape = cube ? VoxelShapes.fullCube() : state.getOutlineShape(world, pos);
			VoxelShape neighbourShape = neighbourState.getOutlineShape(world, neighbourPos);

			if (shape.isEmpty()) {
				shape = VoxelShapes.fullCube();
			}

			if (neighbourShape.isEmpty()) {
				neighbourShape = VoxelShapes.fullCube();
			}

			if (touching(shape, neighbourShape, direction)) {
				faces |= bitOf(direction);
			}
		}

		return faces;
	}

	/**
	 * Whether a kind of block fills its whole block.
	 *
	 * <p>The answer is the same for every block of that kind, so it is kept and looked up only
	 * once for every kind of block.
	 *
	 * @param world     the world the block is in
	 * @param pos       the place of the block
	 * @param state     the state of the block
	 * @param fullCubes which kinds of block fill their whole block, kept between the scans
	 * @return whether the block fills its whole block
	 */
	private static boolean isFullCube(ClientWorld world, BlockPos pos, BlockState state, Map<Block, Boolean> fullCubes) {
		Boolean known = fullCubes.get(state.getBlock());

		if (known != null) {
			return known;
		}

		boolean cube = state.isFullCube(world, pos);
		fullCubes.put(state.getBlock(), cube);

		return cube;
	}

	/**
	 * @param shape          the shape of a block
	 * @param neighbourShape the shape of the block next to it
	 * @param direction      the side the neighbour lies on
	 * @return whether the two shapes touch each other
	 */
	private static boolean touching(VoxelShape shape, VoxelShape neighbourShape, Direction direction) {
		Direction.Axis axis = direction.getAxis();
		boolean positive = direction.getDirection() == Direction.AxisDirection.POSITIVE;

		if (positive) {
			return shape.getMax(axis) >= 1.0D && neighbourShape.getMin(axis) <= 0.0D;
		}

		return shape.getMin(axis) <= 0.0D && neighbourShape.getMax(axis) >= 1.0D;
	}

	/**
	 * @param direction the side of a block
	 * @return the bit of that side
	 */
	private static int bitOf(Direction direction) {
		return switch (direction) {
			case DOWN -> NeighbourFaces.DOWN;
			case UP -> NeighbourFaces.UP;
			case NORTH -> NeighbourFaces.NORTH;
			case SOUTH -> NeighbourFaces.SOUTH;
			case WEST -> NeighbourFaces.WEST;
			case EAST -> NeighbourFaces.EAST;
		};
	}
}
