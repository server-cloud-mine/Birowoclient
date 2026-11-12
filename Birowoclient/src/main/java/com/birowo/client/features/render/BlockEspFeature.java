package com.birowo.client.features.render;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;

import com.birowo.client.core.config.BlockListValue;
import com.birowo.client.core.config.BlockSelection;
import com.birowo.client.core.config.ConfigValue;
import com.birowo.client.core.config.ConfigValues;
import com.birowo.client.core.config.EspSettings;
import com.birowo.client.core.config.EspShape;
import com.birowo.client.core.feature.Feature;
import com.birowo.client.core.feature.FeatureCategory;
import com.birowo.client.core.util.NeighbourFaces;
import com.birowo.client.core.util.ScanReach;
import com.birowo.client.core.util.Veins;
import com.birowo.client.platform.render.CameraView;
import com.birowo.client.platform.render.ShapeRenderer;
import com.birowo.client.platform.render.WorldLayers;
import com.birowo.client.platform.render.WorldRenderable;
import com.birowo.client.platform.world.BlockChanges;
import com.birowo.client.platform.world.BlockColours;
import com.birowo.client.platform.world.BlockList;
import com.birowo.client.platform.world.BlockPreset;
import com.birowo.client.platform.world.BlockScanner;

/**
 * Shows the blocks that are worth looking for.
 *
 * <p>The blocks that are shown are the ones the player has chosen in the picker of the client.
 * The world is looked through chunk by chunk: a chunk is made of sections of sixteen by sixteen
 * by sixteen blocks, and a section that holds none of the blocks that are looked for is skipped
 * without being read, so a chunk that holds nothing interesting costs almost nothing.
 *
 * <p>Every block has a colour and a way of being drawn of its own, and both are chosen in the
 * picker: the block may be drawn by its edges, by its faces, by both, or as a filled box.
 *
 * <p>Blocks of the same kind that touch each other are drawn as one shape: the edges and the
 * faces that are hidden inside the group are left out. A line is drawn to the middle of such a
 * group rather than to every block of it, so that a whole vein of ore does not fill the screen
 * with lines.
 *
 * <p>Looking through a chunk costs a walk through every block of it, which is far too much to
 * do while the game is being drawn. The chunks are therefore looked through by a thread of its
 * own, and the game only ever looks at what that thread has already found. The blocks are
 * gathered into their groups in the same way: on that thread, and only now and then, because
 * the groups are only needed for the lines.
 *
 * <p>The chunks that are nearest to the player are looked through first and are drawn first, so
 * that what is around the player is shown first and the far away parts of the world follow
 * later. A frame never draws more than a fixed number of blocks and lines, so that a world
 * that holds a great many of them cannot take the game down.
 *
 * <p>How far the world is looked through is given in blocks, and it is never larger than the
 * distance the game itself shows the world at: a chunk that is not loaded holds nothing that
 * could be shown anyway.
 */
public final class BlockEspFeature extends Feature implements WorldRenderable {
	/**
	 * The logger of the feature, which says what the feature is looking for.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger("BirowoClient/BlockESP");

	/**
	 * How many chunks are handed to the thread that looks through the world in one tick.
	 *
	 * <p>The thread does the work, so a few chunks at a time are enough to keep it busy without
	 * filling the queue with work that is thrown away when the player walks on.
	 */
	private static final int CHUNKS_PER_TICK = 2;

	/**
	 * How many ticks pass between two looks through the chunk the player is in.
	 */
	private static final int RESCAN_INTERVAL = 20;

	/**
	 * The greatest number of lines that are drawn in one frame.
	 *
	 * <p>A chunk may hold hundreds of blocks, and a line for every one of them would be more than
	 * the eye can follow anyway.
	 */
	private static final int MAXIMUM_TRACERS = 64;

	/**
	 * The greatest number of blocks that are drawn in one frame.
	 *
	 * <p>A block is drawn with twelve lines, so a great many of them would cost more than the
	 * game can spare. The blocks that are left out are the ones that are the furthest away,
	 * because the chunks are drawn in the order of their distance.
	 */
	private static final int MAXIMUM_BLOCKS = 2048;

	/**
	 * How many ticks pass between two builds of the groups of blocks.
	 *
	 * <p>The groups are only needed for the lines, and the lines may wait a moment. The blocks
	 * themselves are drawn straight from what was found, so they never wait.
	 */
	private static final int VEIN_INTERVAL = 4;

	/**
	 * How much of the colour of a block is kept when its faces are drawn, when nothing else is
	 * said about it.
	 */
	private static final int DEFAULT_SIDE_ALPHA = 0x59;

	/**
	 * How much of the colour of a block is kept when it is drawn as a filled box, when nothing
	 * else is said about it.
	 */
	private static final int DEFAULT_FILL_ALPHA = 0x40;

	private final BlockListValue blocks = setting(ConfigValues.ofBlockList("blocks",
			BlockPreset.VALUABLE.identifiers(), BlockColours.defaults()));

	private final ConfigValue<Integer> distance = setting(ConfigValues.ofInt("distance", 64));

	private final ConfigValue<EspShape> shape = setting(ConfigValues.ofEnum("shape", EspShape.LINES));

	private final ConfigValue<Boolean> tracer = setting(ConfigValues.ofBoolean("tracer", true));

	private final ConfigValue<Boolean> tracerThroughWalls = setting(ConfigValues.ofBoolean("tracer_through_walls",
			true));

	private final ConfigValue<Integer> tracerWidth = setting(ConfigValues.ofInt("tracer_width", 1));

	private final ConfigValue<Integer> lineWidth = setting(ConfigValues.ofInt("line_width", 1));

	private final ConfigValue<Integer> sideAlpha = setting(ConfigValues.ofInt("side_alpha", DEFAULT_SIDE_ALPHA));

	private final ConfigValue<Integer> fillAlpha = setting(ConfigValues.ofInt("fill_alpha", DEFAULT_FILL_ALPHA));

	/**
	 * The blocks that were found, by the chunk they belong to, the chunks nearest to the player
	 * first.
	 */
	private final Map<Long, List<BlockScanner.Found>> found = new LinkedHashMap<>();

	/**
	 * The blocks that the thread that looks through the world has found.
	 */
	private final Map<Long, List<BlockPos>> incoming = new ConcurrentHashMap<>();

	/**
	 * The chunks that have been looked through already.
	 */
	private final Set<Long> searched = new HashSet<>();

	/**
	 * The chunks that were not loaded yet and are to be looked through later.
	 */
	private final List<ChunkPos> waiting = new ArrayList<>();

	/**
	 * The chunks that are still to be looked through.
	 */
	private List<ChunkPos> pending = List.of();

	/**
	 * How far through the list of pending chunks the scanner is.
	 */
	private int cursor;

	/**
	 * How many ticks have passed since the chunk of the player was looked through again.
	 */
	private int ticksSinceRescan;

	/**
	 * The chunk the player was in when the list was last built.
	 */
	private ChunkPos centre;

	/**
	 * The radius the list was last built with.
	 */
	private int builtRadius = -1;

	/**
	 * The blocks that are looked for, with the way they are shown.
	 */
	private Map<Block, BlockSelection> wanted = Map.of();

	/**
	 * The blocks the list of blocks was last read from.
	 */
	private Map<String, BlockSelection> wantedSource = Map.of();

	/**
	 * The groups of blocks that touch each other, with the middle of every group.
	 */
	private List<Veins.Vein> veins = List.of();

	/**
	 * The way every kind of block is shown, by the number the kind is given.
	 */
	private Map<Integer, BlockSelection> byKind = Map.of();

	/**
	 * Whether the groups have to be built again.
	 */
	private boolean veinsDirty = true;

	/**
	 * How many ticks have passed since the groups were built.
	 */
	private int ticksSinceVeins;

	/**
	 * Which kinds of block fill their whole block, kept between the scans.
	 */
	private final Map<Block, Boolean> fullCubes = new HashMap<>();

	/**
	 * The thread that looks through the world.
	 */
	private ExecutorService worker;

	/**
	 * The chunks that hold a block that has changed, and that are to be looked through again.
	 */
	private final Set<Long> changed = new HashSet<>();

	public BlockEspFeature() {
		super("blockesp", "BlockESP", "Shows the blocks that are worth looking for.",
				FeatureCategory.RENDER);

		// The way a block is drawn is shown by its name rather than by the name of the constant.
		shape.format(value -> value.displayName());

		// The distance is given in blocks, and it is said so.
		distance.format(value -> value + " blocks");
	}

	@Override
	protected void onEnable() {
		// The game does not say anything when a block is broken or placed, so the changes of the
		// world are listened to.
		BlockChanges.listen(this::onBlockChanged);

		// The world is looked through by a thread of its own, so that the game is never held up
		// by a chunk that holds a great many blocks.
		worker = Executors.newSingleThreadExecutor(task -> {
			Thread thread = new Thread(task, "BirowoClient/BlockESP");
			thread.setDaemon(true);

			return thread;
		});
	}

	@Override
	protected void onDisable() {
		BlockChanges.listen(null);
		changed.clear();

		if (worker != null) {
			worker.shutdownNow();
			worker = null;
		}

		// What was found belongs to the world the feature was looking at, and the world may have
		// changed while the feature was off.
		found.clear();
		incoming.clear();
		searched.clear();
		waiting.clear();
		pending = List.of();
		cursor = 0;
		centre = null;
		builtRadius = -1;
		veins = List.of();
		veinsDirty = true;
	}

	@Override
	public void onTick() {
		MinecraftClient client = MinecraftClient.getInstance();

		if (client.player == null || client.world == null) {
			return;
		}

		// The blocks that are looked for are read again whenever the player has changed them.
		Map<String, BlockSelection> chosen = new LinkedHashMap<>();

		for (BlockSelection selection : blocks.selections()) {
			chosen.put(selection.identifier(), selection);
		}

		if (!chosen.equals(wantedSource)) {
			wantedSource = Map.copyOf(chosen);
			wanted = BlockList.resolveSelections(chosen);

			// The log says what the feature is looking for, which is the first thing to look at
			// when nothing is shown.
			LOGGER.info("BlockESP is looking for {} blocks", wanted.size());

			// What was found belongs to the blocks that were looked for before, so it is thrown
			// away and the world is looked through again.
			found.clear();
			incoming.clear();
			searched.clear();
			waiting.clear();
			pending = List.of();
			cursor = 0;
			centre = null;
			builtRadius = -1;
			veinsDirty = true;
		}

		ChunkPos position = client.player.getChunkPos();
		int reach = reach(client);

		if (!position.equals(centre) || reach != builtRadius) {
			rebuild(position, reach);
		}

		collect();
		scan(client);
		rescan(client);
		buildVeins(client);
	}

	/**
	 * How many chunks around the player are looked through.
	 *
	 * <p>The distance is given in blocks, and it is never larger than the distance the game
	 * itself shows the world at: a chunk that is not loaded holds nothing that could be shown.
	 *
	 * @param client the client the world belongs to
	 * @return the number of chunks around the player
	 */
	private int reach(MinecraftClient client) {
		int viewDistance = client.options.getViewDistance().getValue();

		return ScanReach.chunks(distance.value(), viewDistance);
	}

	/**
	 * Takes over what the thread that looks through the world has found.
	 *
	 * <p>The shape of the blocks and the sides of them that are covered are read from the world
	 * here, because the world may only be read by the thread of the game.
	 *
	 * <p>The chunks are put in the order of their distance again, so that the blocks that are
	 * nearest to the player are the ones that are drawn first.
	 */
	private void collect() {
		if (incoming.isEmpty()) {
			return;
		}

		MinecraftClient client = MinecraftClient.getInstance();

		for (Map.Entry<Long, List<BlockPos>> entry : incoming.entrySet()) {
			found.put(entry.getKey(), BlockScanner.describe(client.world, entry.getValue(), fullCubes));
			veinsDirty = true;
		}

		incoming.clear();
		order();
	}

	/**
	 * Puts the chunks that were found in the order of their distance.
	 */
	private void order() {
		MinecraftClient client = MinecraftClient.getInstance();

		if (client.player == null) {
			return;
		}

		ChunkPos position = client.player.getChunkPos();
		List<Map.Entry<Long, List<BlockScanner.Found>>> entries = new ArrayList<>(found.entrySet());

		entries.sort(Comparator.comparingInt(entry -> {
			ChunkPos place = new ChunkPos(entry.getKey());
			return place.getSquaredDistance(position);
		}));

		found.clear();

		for (Map.Entry<Long, List<BlockScanner.Found>> entry : entries) {
			found.put(entry.getKey(), entry.getValue());
		}
	}

	/**
	 * Hands a few chunks to the thread that looks through the world.
	 *
	 * @param client the client the world belongs to
	 */
	private void scan(MinecraftClient client) {
		if (wanted.isEmpty() || worker == null) {
			return;
		}

		int handed = 0;

		// The chunks that were not loaded yet are looked at again first.
		for (Iterator<ChunkPos> iterator = waiting.iterator(); iterator.hasNext() && handed < CHUNKS_PER_TICK;) {
			ChunkPos position = iterator.next();
			Chunk chunk = chunkAt(client, position);

			if (chunk == null) {
				continue;
			}

			iterator.remove();
			hand(client, position, chunk);
			handed++;
		}

		while (cursor < pending.size() && handed < CHUNKS_PER_TICK) {
			ChunkPos position = pending.get(cursor++);

			// A chunk that has been looked through already is not looked through again; the
			// chunks that were looked through are kept, so that the player does not have to wait
			// for the whole world to be looked through again after every step.
			if (!searched.add(position.toLong())) {
				continue;
			}

			Chunk chunk = chunkAt(client, position);

			if (chunk == null) {
				// The chunk is not loaded yet, so it is looked at again later.
				searched.remove(position.toLong());
				waiting.add(position);
				continue;
			}

			hand(client, position, chunk);
			handed++;
		}

		// The chunks in which a block has been broken or placed are looked through again.
		for (Iterator<Long> iterator = changed.iterator(); iterator.hasNext() && handed < CHUNKS_PER_TICK;) {
			long key = iterator.next();
			iterator.remove();

			ChunkPos position = new ChunkPos(key);
			Chunk chunk = chunkAt(client, position);

			if (chunk == null) {
				continue;
			}

			hand(client, position, chunk);
			handed++;
		}
	}

	/**
	 * Looks through the chunk the player is in once more.
	 *
	 * <p>The chunks are looked through once and the result is kept, so that a block that is
	 * broken or placed would still be shown. The chunk the player is in is the one that is looked
	 * at again every now and then, which is where the player is working.
	 *
	 * @param client the client the world belongs to
	 */
	private void rescan(MinecraftClient client) {
		ticksSinceRescan++;

		if (ticksSinceRescan < RESCAN_INTERVAL) {
			return;
		}

		ticksSinceRescan = 0;

		ChunkPos position = client.player.getChunkPos();
		Chunk chunk = chunkAt(client, position);

		if (chunk == null) {
			return;
		}

		hand(client, position, chunk);
	}

	/**
	 * The chunk of the world at a place, when it is loaded.
	 *
	 * @param client   the client the world belongs to
	 * @param position the place of the chunk
	 * @return the chunk, or {@code null} when it is not loaded
	 */
	private Chunk chunkAt(MinecraftClient client, ChunkPos position) {
		return client.world.getChunkManager().getChunk(position.x, position.z, ChunkStatus.FULL, false);
	}

	/**
	 * Hands a chunk to the thread that looks through the world.
	 *
	 * <p>The thread only reads the chunk itself. Everything that needs the world is done by the
	 * thread of the game, because the world may only be read by that thread.
	 *
	 * @param client   the client the world belongs to
	 * @param position the place of the chunk
	 * @param chunk    the chunk that is to be looked through
	 */
	private void hand(MinecraftClient client, ChunkPos position, Chunk chunk) {
		if (worker == null) {
			return;
		}

		long key = position.toLong();
		Set<Block> blocks = wanted.keySet();
		int bottom = client.world.getBottomY();

		worker.submit(() -> {
			try {
				incoming.put(key, BlockScanner.find(chunk, blocks, bottom));
			} catch (Throwable t) {
				LOGGER.warn("BlockESP could not look through the chunk {} {}", position.x, position.z, t);
			}
		});
	}

	/**
	 * Builds the list of chunks that are to be looked through and forgets what was found before.
	 *
	 * <p>The chunks that are nearest to the player are looked through first, so that what is
	 * around the player is shown first and the far away parts of the world follow later. The
	 * blocks that were found are put in the same order, so that they are drawn in that order as
	 * well.
	 *
	 * @param position the chunk the player is in
	 * @param reach    how many chunks are looked at around the player
	 */
	private void rebuild(ChunkPos position, int reach) {
		List<ChunkPos> chunks = new ArrayList<>();
		Set<Long> inside = new HashSet<>();

		for (int x = -reach; x <= reach; x++) {
			for (int z = -reach; z <= reach; z++) {
				ChunkPos chunk = new ChunkPos(position.x + x, position.z + z);
				chunks.add(chunk);
				inside.add(chunk.toLong());
			}
		}

		chunks.sort(Comparator.comparingInt(chunk -> chunk.getSquaredDistance(position)));

		this.pending = chunks;
		this.cursor = 0;
		this.centre = position;
		this.builtRadius = reach;

		// The blocks that were found are kept while the chunks are looked through again, so that
		// the overlay does not blink every time the player walks into the next chunk. They are
		// put in the order of the distance, so that the nearest ones are drawn first.
		Map<Long, List<BlockScanner.Found>> kept = new LinkedHashMap<>();

		for (ChunkPos chunk : chunks) {
			List<BlockScanner.Found> blocks = found.get(chunk.toLong());

			if (blocks != null) {
				kept.put(chunk.toLong(), blocks);
			}
		}

		found.clear();
		found.putAll(kept);

		searched.removeIf(key -> !inside.contains(key));
		waiting.removeIf(place -> !inside.contains(place.toLong()));
		veinsDirty = true;
	}

	/**
	 * Remembers that a block of the world has changed.
	 *
	 * <p>The chunk the block lies in is looked through again, so that a block that is broken
	 * disappears at once and a block that is placed appears at once.
	 *
	 * @param pos   the place of the block
	 * @param state the state the block has now
	 */
	private void onBlockChanged(BlockPos pos, BlockState state) {
		changed.add(ChunkPos.toLong(pos.getX() >> 4, pos.getZ() >> 4));

		// The groups are built again at once, so that the line to a block that has just been
		// placed is there right away.
		veinsDirty = true;
		ticksSinceVeins = VEIN_INTERVAL;
	}

	/**
	 * Builds the groups of blocks that touch each other.
	 *
	 * <p>A group is a set of blocks of the same kind that are joined to each other. The line to a
	 * group is drawn to its middle, so that a vein of ore is marked by one line instead of by a
	 * line for every block in it.
	 *
	 * <p>The groups are built every now and then rather than in every tick, because they are
	 * only needed for the lines and building them costs a walk through every block that was
	 * found. They are put in the order of their distance, so that the lines nearest to the
	 * player are the ones that are drawn when there are more of them than a frame may hold.
	 */
	private void buildVeins(MinecraftClient client) {
		ticksSinceVeins++;

		if (!veinsDirty || ticksSinceVeins < VEIN_INTERVAL) {
			return;
		}

		ticksSinceVeins = 0;
		veinsDirty = false;

		int total = 0;

		for (List<BlockScanner.Found> blocks : found.values()) {
			total += blocks.size();
		}

		if (total == 0) {
			veins = List.of();
			byKind = Map.of();
			return;
		}

		int[] xs = new int[total];
		int[] ys = new int[total];
		int[] zs = new int[total];
		int[] kinds = new int[total];
		int[] faces = new int[total];

		Map<Block, Integer> numberOfKind = new LinkedHashMap<>();
		Map<Integer, BlockSelection> selections = new HashMap<>();
		int index = 0;

		for (List<BlockScanner.Found> blocks : found.values()) {
			for (BlockScanner.Found block : blocks) {
				Integer kind = numberOfKind.get(block.block());

				if (kind == null) {
					kind = numberOfKind.size();
					numberOfKind.put(block.block(), kind);

					BlockSelection selection = wanted.get(block.block());

					if (selection != null) {
						selections.put(kind, selection);
					}
				}

				BlockPos pos = block.pos();

				xs[index] = pos.getX();
				ys[index] = pos.getY();
				zs[index] = pos.getZ();
				kinds[index] = kind;
				faces[index] = block.faces();
				index++;
			}
		}

		Veins.Result result = Veins.group(xs, ys, zs, kinds, faces);

		Vec3d eye = client.player.getEyePos();
		List<Veins.Vein> list = new ArrayList<>(result.veins());

		list.sort(Comparator.comparingDouble(vein -> (vein.x() - eye.getX()) * (vein.x() - eye.getX())
				+ (vein.y() - eye.getY()) * (vein.y() - eye.getY())
				+ (vein.z() - eye.getZ()) * (vein.z() - eye.getZ())));

		veins = List.copyOf(list);
		byKind = Map.copyOf(selections);
	}

	@Override
	public void onWorldRender(WorldRenderContext context) {
		if (found.isEmpty()) {
			return;
		}

		MinecraftClient client = MinecraftClient.getInstance();

		if (client.player == null || client.world == null) {
			return;
		}

		Camera camera = client.gameRenderer.getCamera();
		Vec3d eye = camera.getCameraPos();
		Vec3d forward = CameraView.forward(camera);
		MatrixStack.Entry entry = context.matrices().peek();
		EspShape defaultShape = shape.value();
		float defaultLineWidth = Math.max(1.0F, lineWidth.value());
		double reach = Math.max(1, distance.value());
		int drawn = 0;

		VertexConsumer edges = context.consumers().getBuffer(WorldLayers.linesNoDepth());
		VertexConsumer sides = null;

		for (List<BlockScanner.Found> blocks : found.values()) {
			for (BlockScanner.Found block : blocks) {
				if (drawn >= MAXIMUM_BLOCKS) {
					// There are more blocks than the game can draw in one frame; the rest is left
					// for the frames to come. The blocks that are left out are the ones that are
					// the furthest away, because the chunks are drawn in the order of their
					// distance.
					drawTracers(context, entry, eye, camera);
					return;
				}

				BlockSelection selection = wanted.get(block.block());

				if (selection == null) {
					continue;
				}

				EspSettings settings = selection.settings();
				EspShape way = settings.shape() != null ? settings.shape() : defaultShape;
				int lineColour = selection.argb();
				int sideColour = sideColour(selection);
				float width = settings.lineWidth() >= 0 ? Math.max(1.0F, settings.lineWidth())
						: defaultLineWidth;

				Box box = block.box();

				double minX = box.minX - eye.x;
				double minY = box.minY - eye.y;
				double minZ = box.minZ - eye.z;
				double maxX = box.maxX - eye.x;
				double maxY = box.maxY - eye.y;
				double maxZ = box.maxZ - eye.z;

				// A block that is too far away is not shown at all.
				if (!withinReach(minX, minY, minZ, maxX, maxY, maxZ, reach)) {
					continue;
				}

				// A block that lies behind the camera cannot be seen, whatever it is drawn with.
				if (behind(minX, minY, minZ, maxX, maxY, maxZ, forward)) {
					continue;
				}

				if (sides == null && (way.sides() || way.filled())) {
					sides = context.consumers().getBuffer(WorldLayers.filledBoxNoDepth());
				}

				drawn++;

				if (way.lines()) {
					// The edges that lie inside a group of blocks are left out, so that a vein of
					// ore looks like one shape.
					ShapeRenderer.drawBoxEdges(edges, entry, minX, minY, minZ, maxX, maxY, maxZ, lineColour,
							width, block.faces());
				}

				if (way.sides()) {
					for (int face : NeighbourFaces.ALL) {
						if (NeighbourFaces.has(block.faces(), face)) {
							continue;
						}

						ShapeRenderer.drawBoxFace(sides, entry, face, minX, minY, minZ, maxX, maxY, maxZ,
								sideColour);
					}
				}

				if (way.filled()) {
					int fill = (sideColour & 0x00FFFFFF) | (fillAlpha(selection) << 24);

					ShapeRenderer.drawBoxFill(sides, entry, minX, minY, minZ, maxX, maxY, maxZ, fill, fill);
				}
			}
		}

		drawTracers(context, entry, eye, camera);
	}

	/**
	 * Draws the line to every group of blocks that has one.
	 *
	 * <p>The line begins a little in front of the camera, on the way to the group, so that it
	 * begins in the middle of the screen without being in the eye of the player.
	 *
	 * @param context the render context of the current frame
	 * @param entry   the transformation the lines are drawn with
	 * @param eye     the place of the camera in the world
	 * @param camera  the camera of the client
	 */
	private void drawTracers(WorldRenderContext context, MatrixStack.Entry entry, Vec3d eye, Camera camera) {
		if (veins.isEmpty()) {
			return;
		}

		VertexConsumer consumer = context.consumers().getBuffer(tracerThroughWalls.value()
				? WorldLayers.linesNoDepth()
				: WorldLayers.linesWithDepth());
		boolean fallback = tracer.value();
		float fallbackWidth = Math.max(1.0F, tracerWidth.value());
		double reach = Math.max(1, distance.value());
		int drawn = 0;

		for (Veins.Vein vein : veins) {
			if (drawn >= MAXIMUM_TRACERS) {
				break;
			}

			BlockSelection selection = byKind.get(vein.kind());

			if (selection == null) {
				continue;
			}

			EspSettings settings = selection.settings();

			if (settings.tracer() != null ? !settings.tracer() : !fallback) {
				continue;
			}

			Vec3d middle = new Vec3d(vein.x(), vein.y(), vein.z());

			// A group that is too far away is not shown at all, so that the lines do not reach
			// further than the blocks themselves do.
			double dx = middle.x - eye.x;
			double dy = middle.y - eye.y;
			double dz = middle.z - eye.z;

			if (dx * dx + dy * dy + dz * dz > reach * reach) {
				continue;
			}

			int colour = tracerColour(selection);
			float width = settings.tracerWidth() >= 0 ? Math.max(1.0F, settings.tracerWidth()) : fallbackWidth;

			Vec3d start = CameraView.start(camera, middle);

			if (start == null) {
				// The group is behind the camera, so there is no line to it.
				continue;
			}

			ShapeRenderer.drawSegment(consumer, entry,
					start.x - eye.x, start.y - eye.y, start.z - eye.z,
					middle.x - eye.x, middle.y - eye.y, middle.z - eye.z,
					colour, width);

			drawn++;
		}
	}

	/**
	 * The colour the line to a group is drawn in.
	 *
	 * @param selection the block and the way it is shown
	 * @return the colour of the line
	 */
	private int tracerColour(BlockSelection selection) {
		int colour = selection.settings().tracerColour();

		return colour >= 0 ? 0xFF000000 | (colour & 0xFFFFFF) : selection.argb();
	}

	/**
	 * Whether a block is close enough to be shown.
	 *
	 * @param minX  the left edge of the block, counted from the camera
	 * @param minY  the lower edge of the block, counted from the camera
	 * @param minZ  the front edge of the block, counted from the camera
	 * @param maxX  the right edge of the block, counted from the camera
	 * @param maxY  the upper edge of the block, counted from the camera
	 * @param maxZ  the back edge of the block, counted from the camera
	 * @param reach how far the blocks are shown, in blocks
	 * @return whether the block is within reach
	 */
	private boolean withinReach(double minX, double minY, double minZ, double maxX, double maxY, double maxZ,
			double reach) {
		return nearest(minX, maxX) * nearest(minX, maxX)
				+ nearest(minY, maxY) * nearest(minY, maxY)
				+ nearest(minZ, maxZ) * nearest(minZ, maxZ) <= reach * reach;
	}

	/**
	 * Whether a block lies behind the camera.
	 *
	 * @param minX    the left edge of the block, counted from the camera
	 * @param minY    the lower edge of the block, counted from the camera
	 * @param minZ    the front edge of the block, counted from the camera
	 * @param maxX    the right edge of the block, counted from the camera
	 * @param maxY    the upper edge of the block, counted from the camera
	 * @param maxZ    the back edge of the block, counted from the camera
	 * @param forward the direction the camera looks at
	 * @return whether the block cannot be seen at all
	 */
	private boolean behind(double minX, double minY, double minZ, double maxX, double maxY, double maxZ,
			Vec3d forward) {
		double depth = (minX + maxX) * 0.5D * forward.x
				+ (minY + maxY) * 0.5D * forward.y
				+ (minZ + maxZ) * 0.5D * forward.z;

		// A block that is a little behind the camera is still partly in sight.
		return depth < -1.0D;
	}

	/**
	 * How far the nearest point of a box is from the camera, along one side.
	 *
	 * @param min the lower edge of the box, counted from the camera
	 * @param max the upper edge of the box, counted from the camera
	 * @return the distance to the nearest point of the box
	 */
	private static double nearest(double min, double max) {
		if (min <= 0.0D && max >= 0.0D) {
			// The camera is beside the box, so the nearest point of it is at the camera.
			return 0.0D;
		}

		return Math.min(Math.abs(min), Math.abs(max));
	}

	/**
	 * The colour the faces of a block are drawn in.
	 *
	 * @param selection the block and the way it is shown
	 * @return the colour of the faces
	 */
	private int sideColour(BlockSelection selection) {
		EspSettings settings = selection.settings();
		int colour = settings.sideColour() >= 0 ? settings.sideColour() : selection.colour();

		return (colour & 0x00FFFFFF) | (sideAlpha(selection) << 24);
	}

	/**
	 * @param selection the block and the way it is shown
	 * @return how much of the colour of the faces is kept
	 */
	private int sideAlpha(BlockSelection selection) {
		int alpha = selection.settings().alpha();

		return alpha >= 0 ? alpha : Math.min(255, Math.max(0, sideAlpha.value()));
	}

	/**
	 * @param selection the block and the way it is shown
	 * @return how much of the colour of the filled box is kept
	 */
	private int fillAlpha(BlockSelection selection) {
		int alpha = selection.settings().alpha();

		return alpha >= 0 ? alpha : Math.min(255, Math.max(0, fillAlpha.value()));
	}
}
