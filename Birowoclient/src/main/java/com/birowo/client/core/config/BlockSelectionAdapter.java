package com.birowo.client.core.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

/**
 * Reads and writes a list of blocks together with their colours and their ways of being shown.
 *
 * <p>The list is written as an array of objects, so that a person can read it:
 *
 * <pre>
 * [ { "id": "minecraft:chest", "colour": "#F9801D", "shape": "LINES", "tracer": true } ]
 * </pre>
 *
 * <p>A list that was written before the blocks had colours of their own is an array of plain
 * identifiers. Such a list is still read, and every block in it is given the colour it has by
 * default, so that an old configuration file keeps working.
 */
final class BlockSelectionAdapter extends TypeAdapter<List<BlockSelection>> {
	/**
	 * The colour of a block that has no colour of its own.
	 */
	private static final int FALLBACK = 0x4D9BFF;

	/**
	 * The colour every block has by default, by identifier.
	 */
	private final Map<String, Integer> defaults;

	/**
	 * @param defaults the colour every block has by default, by identifier
	 */
	BlockSelectionAdapter(Map<String, Integer> defaults) {
		this.defaults = Map.copyOf(defaults);
	}

	@Override
	public void write(JsonWriter out, List<BlockSelection> value) throws IOException {
		out.beginArray();

		for (BlockSelection selection : value) {
			out.beginObject();
			out.name("id").value(selection.identifier());
			out.name("colour").value(String.format("#%06X", selection.colour() & 0xFFFFFF));

			EspSettings settings = selection.settings();

			if (settings.shape() != null) {
				out.name("shape").value(settings.shape().name());
			}

			if (settings.tracer() != null) {
				out.name("tracer").value(settings.tracer());
			}

			if (settings.tracerColour() >= 0) {
				out.name("tracerColour").value(String.format("#%06X", settings.tracerColour() & 0xFFFFFF));
			}

			if (settings.sideColour() >= 0) {
				out.name("sideColour").value(String.format("#%06X", settings.sideColour() & 0xFFFFFF));
			}

			if (settings.alpha() >= 0) {
				out.name("alpha").value(settings.alpha());
			}

			if (settings.lineWidth() >= 0) {
				out.name("lineWidth").value(settings.lineWidth());
			}

			if (settings.tracerWidth() >= 0) {
				out.name("tracerWidth").value(settings.tracerWidth());
			}

			out.endObject();
		}

		out.endArray();
	}

	@Override
	public List<BlockSelection> read(JsonReader in) throws IOException {
		if (in.peek() == JsonToken.NULL) {
			in.nextNull();
			return List.of();
		}

		if (in.peek() != JsonToken.BEGIN_ARRAY) {
			// The value is not a list at all, so the setting keeps the value it has.
			return null;
		}

		List<BlockSelection> selections = new ArrayList<>();
		in.beginArray();

		while (in.hasNext()) {
			JsonToken token = in.peek();

			if (token == JsonToken.STRING) {
				String identifier = in.nextString();
				selections.add(new BlockSelection(identifier, colourOf(identifier)));
			} else if (token == JsonToken.BEGIN_OBJECT) {
				BlockSelection selection = readObject(in);

				if (selection != null) {
					selections.add(selection);
				}
			} else {
				in.skipValue();
			}
		}

		in.endArray();
		return List.copyOf(selections);
	}

	/**
	 * Reads one block out of an object.
	 *
	 * @param in the reader to read from
	 * @return the block, or {@code null} when the object names no block
	 * @throws IOException when the reader fails
	 */
	private BlockSelection readObject(JsonReader in) throws IOException {
		String identifier = null;
		int colour = FALLBACK;
		EspSettings settings = EspSettings.DEFAULT;

		in.beginObject();

		while (in.hasNext()) {
			String name = in.nextName();

			switch (name) {
				case "id" -> identifier = in.nextString();
				case "colour" -> colour = parseColour(in.nextString(), colour);
				case "shape" -> settings = settings.withShape(parseShape(in.nextString()));
				case "tracer" -> settings = settings.withTracer(in.nextBoolean());
				case "tracerColour" -> settings = settings.withTracerColour(parseColour(in.nextString(), -1));
				case "sideColour" -> settings = settings.withSideColour(parseColour(in.nextString(), -1));
				case "alpha" -> settings = settings.withAlpha(parseNumber(in, -1));
				case "lineWidth" -> settings = settings.withLineWidth(parseNumber(in, -1));
				case "tracerWidth" -> settings = settings.withTracerWidth(parseNumber(in, -1));
				default -> in.skipValue();
			}
		}

		in.endObject();

		if (identifier == null) {
			return null;
		}

		return new BlockSelection(identifier, colour, settings);
	}

	/**
	 * @param text the name of the way the block is drawn
	 * @return the way the block is drawn, or {@code null} when the name is not known
	 */
	private EspShape parseShape(String text) {
		for (EspShape shape : EspShape.values()) {
			if (shape.name().equalsIgnoreCase(text.trim())) {
				return shape;
			}
		}

		return null;
	}

	/**
	 * @param text the colour as it is written in the file
	 * @param fallback the colour to use when the text is not a colour
	 * @return the colour as a {@code 0xRRGGBB} value
	 */
	private int parseColour(String text, int fallback) {		String trimmed = text.trim();

		if (trimmed.startsWith("#")) {
			trimmed = trimmed.substring(1);
		}

		try {
			return Integer.parseInt(trimmed, 16) & 0xFFFFFF;
		} catch (NumberFormatException e) {
			return fallback;
		}
	}

	/**
	 * Reads a plain number out of the file.
	 *
	 * @param in       the reader to read from
	 * @param fallback the number to use when the file holds something else
	 * @return the number
	 * @throws IOException when the reader fails
	 */
	private int parseNumber(JsonReader in, int fallback) throws IOException {
		if (in.peek() == JsonToken.NUMBER) {
			return in.nextInt();
		}

		in.skipValue();

		return fallback;
	}

	/**
	 * @param identifier the identifier of a block
	 * @return the colour the block has by default
	 */
	private int colourOf(String identifier) {
		return defaults.getOrDefault(identifier, FALLBACK);
	}
}
