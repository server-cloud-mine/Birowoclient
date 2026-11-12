package com.birowo.client.platform.command;

import java.util.Locale;

import com.birowo.client.core.command.Command;
import com.birowo.client.core.feature.Feature;
import com.birowo.client.core.feature.FeatureRegistry;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

/**
 * Enables or disables a feature by its identifier.
 */
public final class ToggleCommand implements Command {
	private final FeatureRegistry featureRegistry;

	/**
	 * @param featureRegistry the registry the feature is looked up in
	 */
	public ToggleCommand(FeatureRegistry featureRegistry) {
		this.featureRegistry = featureRegistry;
	}

	@Override
	public String name() {
		return "toggle";
	}

	@Override
	public String description() {
		return "Enables or disables a feature";
	}

	@Override
	public String usage() {
		return "<feature>";
	}

	@Override
	public boolean execute(String[] args) {
		if (args.length != 1) {
			reply("Usage: .birowo toggle <feature>");
			return false;
		}

		String id = args[0].toLowerCase(Locale.ROOT);
		Feature feature = featureRegistry.byId(id).orElse(null);

		if (feature == null) {
			reply("Unknown feature: " + args[0]);
			return false;
		}

		feature.toggle();
		reply(feature.displayName() + (feature.enabled() ? " enabled" : " disabled"));
		return true;
	}

	private static void reply(String message) {
		MinecraftClient client = MinecraftClient.getInstance();

		if (client.player != null) {
			client.player.sendMessage(Text.literal("[Birowo] " + message), false);
		}
	}
}
