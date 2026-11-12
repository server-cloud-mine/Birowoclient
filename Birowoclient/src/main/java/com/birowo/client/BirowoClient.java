package com.birowo.client;

import java.nio.file.Path;

import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.birowo.client.core.command.CommandManager;
import com.birowo.client.core.config.ConfigStore;
import com.birowo.client.core.event.EventBus;
import com.birowo.client.core.event.FeatureToggleEvent;
import com.birowo.client.core.event.TickEvent;
import com.birowo.client.core.feature.FeatureRegistry;
import com.birowo.client.core.gui.ClientGuiHost;
import com.birowo.client.core.gui.GuiLayoutStore;
import com.birowo.client.core.gui.GuiScreenLauncher;
import com.birowo.client.features.combat.TriggerBotFeature;
import com.birowo.client.features.misc.AutoToolFeature;
import com.birowo.client.features.movement.FreeCamFeature;
import com.birowo.client.features.movement.SprintFeature;
import com.birowo.client.features.render.BlockEspFeature;
import com.birowo.client.features.render.PlayerEspFeature;
import com.birowo.client.features.render.TracerFeature;
import com.birowo.client.platform.command.ClientCommands;
import com.birowo.client.platform.command.HelpCommand;
import com.birowo.client.platform.command.ListCommand;
import com.birowo.client.platform.command.ToggleCommand;
import com.birowo.client.platform.keybind.KeyBindManager;
import com.birowo.client.platform.render.RenderDispatcher;
import com.birowo.client.platform.screen.IntroScreen;
import com.birowo.client.platform.screen.JoinLogoOverlay;
import com.birowo.client.platform.screen.MinecraftGuiScreenLauncher;
import com.birowo.client.platform.sound.IntroSound;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;

/**
 * Entry point of the client.
 *
 * <p>Wiring lives here and nowhere else: the core has no Minecraft dependency, the platform holds
 * every Minecraft-facing service, and a feature is a plain object that the registry drives.
 *
 * <p>The client itself owns no feature. A feature is registered with the
 * {@link FeatureRegistry}, and the registry is what the interface, the commands and the
 * configuration file work on.
 */
public final class BirowoClient implements ClientModInitializer {
	/**
	 * The mod identifier, used for the config file and the translation namespace.
	 */
	public static final String MOD_ID = "birowo";

	/**
	 * The key the interface is opened with.
	 */
	private static final int OPEN_INTERFACE_KEY = GLFW.GLFW_KEY_R;

	/**
	 * The key the camera is let go with.
	 */
	private static final int FREE_CAM_KEY = GLFW.GLFW_KEY_V;

	private static final Logger LOGGER = LoggerFactory.getLogger("BirowoClient");

	private final EventBus eventBus = new EventBus();
	private final FeatureRegistry featureRegistry = new FeatureRegistry(eventBus);
	private final CommandManager commandManager = new CommandManager();
	private final KeyBindManager keyBindManager = new KeyBindManager("keys");

	private long tickCount;

	private boolean introPending;

	@Override
	public void onInitializeClient() {
		RenderDispatcher renderDispatcher = new RenderDispatcher(featureRegistry);

		GuiScreenLauncher guiLauncher = new MinecraftGuiScreenLauncher();
		GuiLayoutStore guiLayoutStore = new GuiLayoutStore(guiLayoutPath());
		ClientGuiHost guiHost = new ClientGuiHost(featureRegistry, guiLauncher, guiLayoutStore);

		// The interface is the window through which the features are seen, so it is not a feature
		// of its own: it is opened by a key binding and it lists whatever is registered.
		keyBindManager.bind("clickgui", "key.birowo.clickgui", OPEN_INTERFACE_KEY, guiHost::open);

		commandManager.register(new ToggleCommand(featureRegistry));
		commandManager.register(new ListCommand(featureRegistry));
		commandManager.register(new HelpCommand(commandManager));

		ClientCommands.register(commandManager);

		// The features are registered here and nowhere else: the interface, the commands and the
		// configuration file all work on whatever is in the registry.
		featureRegistry.register(new TriggerBotFeature());
		featureRegistry.register(new SprintFeature());
		featureRegistry.register(new AutoToolFeature());
		featureRegistry.register(new BlockEspFeature());
		featureRegistry.register(new PlayerEspFeature());
		featureRegistry.register(new TracerFeature());

		// The camera is let go by a key rather than by the interface, because it is something that
		// is used while the game is being played and not something that is set up beforehand.
		FreeCamFeature freeCam = new FreeCamFeature();
		featureRegistry.register(freeCam);

		keyBindManager.bind("freecam", "key.birowo.freecam", FREE_CAM_KEY,
				() -> freeCam.setEnabled(!freeCam.enabled()));

		// The start of the tick is used rather than the end so that whatever a feature does in a
		// tick is picked up by the movement packet sent later in the same tick.
		ClientTickEvents.START_CLIENT_TICK.register(this::onStartTick);
		ClientTickEvents.END_CLIENT_TICK.register(this::onEndTick);
		WorldRenderEvents.AFTER_ENTITIES.register(renderDispatcher::onWorldRender);

		// The overlay is drawn after the vanilla HUD elements so that it sits above the world but
		// below the chat.
		HudElementRegistry.attachElementAfter(VanillaHudElements.MISC_OVERLAYS,
				Identifier.of(MOD_ID, "overlay"),
				(context, tickCounter) -> renderDispatcher.onHudRender(context, tickCounter));

		// The logo of the client is shown when the player joins a server. It is drawn on the HUD
		// rather than as a screen, so that the player can play while it is on screen.
		JoinLogoOverlay joinLogo = new JoinLogoOverlay();

		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> joinLogo.start());

		HudElementRegistry.attachElementAfter(VanillaHudElements.MISC_OVERLAYS,
				Identifier.of(MOD_ID, "join_logo"),
				(context, tickCounter) -> joinLogo.render(context));

		ConfigStore configStore = new ConfigStore(configPath());
		configStore.load(featureRegistry.features());

		// Subscribed after the load so that restoring the saved state does not immediately write
		// it back; only a genuine toggle from here on changes the file.
		eventBus.subscribe(FeatureToggleEvent.class, event -> configStore.save(featureRegistry.features()));
		// The animation is played once, when the game opens. It waits for the first tick so that
		// it is played over the title screen rather than under it.
		ClientLifecycleEvents.CLIENT_STARTED.register(client -> introPending = true);

		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
			configStore.save(featureRegistry.features());
			guiHost.saveLayout();
		});

		LOGGER.info("BirowoClient ready with {} features", featureRegistry.features().size());
	}

	private void onStartTick(MinecraftClient client) {
		keyBindManager.onTick();
		eventBus.post(new TickEvent(tickCount++));
	}

	private void onEndTick(MinecraftClient client) {
		if (!introPending) {
			return;
		}

		introPending = false;
		client.setScreen(new IntroScreen(client.currentScreen));

		// The sound of the introduction is played with the animation, so that the client is
		// heard as well as seen.
		IntroSound.play(client);
	}

	private static Path configPath() {
		return FabricLoader.getInstance().getConfigDir().resolve(MOD_ID + ".json");
	}

	private static Path guiLayoutPath() {
		return FabricLoader.getInstance().getConfigDir().resolve(MOD_ID + "_gui.json");
	}
}
