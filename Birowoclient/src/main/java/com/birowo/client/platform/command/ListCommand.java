package com.birowo.client.platform.command;

import com.birowo.client.core.command.Command;
import com.birowo.client.core.feature.Feature;
import com.birowo.client.core.feature.FeatureRegistry;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

/**
 * Lists every registered feature and whether it is enabled.
 */
public final class ListCommand implements Command {
	private final FeatureRegistry featureRegistry;

	/**
	 * @param featureRegistry the registry the features are read from
	 */
	public ListCommand(FeatureRegistry featureRegistry) {
		this.featureRegistry = featureRegistry;
	}

	@Override
	public String name() {
		return "list";
	}

	@Override
	public String description() {
		return "Lists every feature and its state";
	}

	@Override
	public String usage() {
		return "";
	}

	@Override
	public boolean execute(String[] args) {
		StringBuilder message = new StringBuilder("Features:");

		for (Feature feature : featureRegistry.features()) {
			message.append(' ')
					.append(feature.id())
					.append('=')
					.append(feature.enabled() ? "on" : "off");
		}

		MinecraftClient client = MinecraftClient.getInstance();

		if (client.player != null) {
			client.player.sendMessage(Text.literal("[Birowo] " + message), false);
		}

		return true;
	}
}
