package com.birowo.client.platform.sound;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.AbstractSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;

/**
 * The sound that is played when the client opens.
 *
 * <p>The sound is a sound of the client itself, so it is played through the sound engine of the
 * game rather than by the client on its own: the volume of the game, the mute button and the
 * sound device of the player all work on it as they work on the sounds of the game.
 *
 * <p>The sound file is an ogg file, which is what the game can play. It is made out of the mp3
 * file that lies in the root of the project while the mod is built.
 */
public final class IntroSound {
	/**
	 * The logger of the sound, which says when the sound could not be played.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger("BirowoClient/IntroSound");

	/**
	 * The place the sound is found at.
	 */
	public static final Identifier SOUND = Identifier.of("birowo", "intro");

	/**
	 * How loud the sound is played.
	 */
	private static final float VOLUME = 1.0F;

	/**
	 * How fast the sound is played.
	 */
	private static final float PITCH = 1.0F;

	private IntroSound() {
	}

	/**
	 * Plays the sound of the introduction.
	 *
	 * <p>Nothing happens when the sound file is not there: the game starts without a sound
	 * rather than with an error on the screen.
	 *
	 * @param client the client that is starting
	 */
	public static void play(MinecraftClient client) {
		SoundManager manager = client.getSoundManager();

		if (manager.get(SOUND) == null) {
			LOGGER.warn("The sound of the introduction is missing, so the client starts without it");
			return;
		}

		manager.play(new Introduction(SOUND, VOLUME, PITCH), 0);
	}

	/**
	 * The sound of the introduction itself.
	 *
	 * <p>The sound belongs to the player rather than to a place in the world, so it is heard the
	 * same everywhere and it does not grow quieter when the player walks away from it.
	 */
	private static final class Introduction extends AbstractSoundInstance {
		/**
		 * @param id     the place the sound is found at
		 * @param volume how loud the sound is played
		 * @param pitch  how fast the sound is played
		 */
		Introduction(Identifier id, float volume, float pitch) {
			super(id, SoundCategory.MASTER, Random.create());

			this.volume = volume;
			this.pitch = pitch;
			this.relative = true;
			this.attenuationType = AttenuationType.NONE;
		}
	}
}
