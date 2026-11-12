# Birowo Client

A Fabric client for Minecraft 1.21.11, built as a feature-driven shell: the framework, the
interface, the configuration and the command line are in place, and every feature is written
against them.

## Layout

The sources are split into three layers, and the split is the rule the whole project follows.

```
com.birowo.client
├── BirowoClient.java      the entry point; it wires everything together and owns nothing else
│
├── core/                  pure Java, no Minecraft on the classpath
│   ├── command/           the commands of the client and the manager that dispatches them
│   ├── config/            the configuration file and the typed settings it stores
│   ├── event/             the event bus and the events that travel on it
│   ├── feature/           the Feature base class, its categories and the registry
│   ├── gui/               the state of the interface: theme, panels, layout, animations
│   ├── keybind/           the contract for a key binding; the game-backed one is in platform
│   └── util/              small, tested pieces of arithmetic
│
├── features/              the features themselves, one package per group
│   ├── combat/            TriggerBot
│   ├── movement/          AutoSprint
│   ├── render/            BlockESP, Tracer
│   └── misc/              AutoTool
│
└── platform/              everything that touches Minecraft
    ├── command/           the commands, wired into the Brigadier client command tree
    ├── keybind/           key bindings backed by the key bindings of the game
    ├── mixin/             the mixins that reach into the game
    ├── render/            the render passes and the primitives that draw in the world
    ├── rotation/          the rotation the server is told about
    ├── screen/            the screens of the client, the interface among them
    └── world/             reading the world
```

A class in `core` never imports a Minecraft class. It reaches the game through a contract that
the `platform` layer implements, which is what makes the core testable: the tests in
`src/test/java` run without the game, and they cover the arithmetic, the event bus, the
configuration, the registry and the state of the interface.

The features in `features` are the one layer that is written against the game itself: a feature
that draws in the world or that reads the player has to speak to Minecraft. What it shares with
the rest of the client — the layers it draws with, the way it looks at the blocks around it —
comes from the `platform` layer.

## Writing a feature

A feature is a subclass of `Feature`. It declares what it is called, which category it is listed
under, and the settings it has:

```java
package com.birowo.client.features.combat;

import com.birowo.client.core.config.ConfigValues;
import com.birowo.client.core.feature.Feature;
import com.birowo.client.core.feature.FeatureCategory;

public final class ExampleFeature extends Feature {
	private final ConfigValue<Boolean> enabled = setting(ConfigValues.ofBoolean("enabled", true));

	public ExampleFeature() {
		super("example", "Example", "Does something useful.", FeatureCategory.COMBAT);
	}

	@Override
	public void onTick() {
		// runs once per tick, while the feature is enabled
	}
}
```

Registering it is one line in `BirowoClient`:

```java
featureRegistry.register(new ExampleFeature());
```

That is all. A registered feature is ticked by the registry, drawn in the interface under its
category, listed by `.birowo list`, switched with `.birowo toggle example`, and its settings are
written to the configuration file and restored when the client starts again.

## The interface

The interface is opened with the **R** key and is not a feature of its own: it is the window
through which the features are seen. It lists one panel per category, a search panel, and the
settings of a feature when a row is clicked twice. The positions of the panels are kept in
`birowo_gui.json`, apart from the configuration of the features in `birowo.json`, because they
belong to the interface rather than to any feature.

The interface is drawn in one blue and in the neutrals that sit next to it. The headers fade
from one shade of blue to another, the panels and the switches glow when the cursor is over
them, and every movement is eased: the panels grow into place, the knob of a switch slides
rather than jumps, and the panel of the settings fades in and slides down into place. Nothing
in the interface is drawn with a texture of its own, so it stays sharp at every GUI scale.

## The features

| Feature      | Category  | What it does                                                  |
| ------------ | --------- | ------------------------------------------------------------- |
| TriggerBot   | Combat    | attacks what the player is looking at                        |
| AutoSprint   | Movement  | sprints without the sprint key having to be held              |
| FreeCam      | Movement  | lets the camera leave the player                              |
| AutoTool     | Misc      | puts the right tool into the hand before a block is mined     |
| BlockESP     | Render    | shows the blocks that are worth looking for                   |
| Tracer       | Render    | draws a line to the target the player is looking at           |

Every one of them is switched on and off in the interface, listed by `.birowo list` and
switched with `.birowo toggle <name>`. Their settings are written to `birowo.json` and restored
when the client starts again.

The TriggerBot uses the attack of the game itself, which is a private method of the client and
is reached through a mixin. That way the cooldown of the attack, the charge a weapon needs and
the way a sweep is made are the ones of the game, and the server sees an attack that is no
different from any other. The attack waits for the weapon to be charged: the setting `charge`
says how much of the charge is needed before the attack is made, a hundred being a weapon that
is fully charged and therefore one that deals its full damage.

The Tracer can be drawn in two ways. On the screen it starts in the middle of the screen, at the
crosshair, and ends at the target; the line is always the same width and is never hidden by what
is in front of it. In the world it starts at the eye of the player and ends at the middle of the
target; it is a part of the world, so it grows thinner as it goes further away and it can be
hidden by what stands in front of it. The place of the target on the screen is the one the game
itself computes, with the very matrix it draws the world with.

### The free camera

The camera can be let go of the player with the **V** key, and put back with the same key. While it
is free it can be anywhere and it can look anywhere: the mouse turns it and the keys of the
movement move it, the space bar lifts it and the sneak key lowers it. The setting `speed` says how
fast it travels, and the **wheel of the mouse** changes it while the camera is free; the wheel
belongs to the camera then, and the game does not hear of it.

The camera travels with the time that has passed since the last frame, so that it covers the same
distance whether the game runs at sixty frames a second or at two hundred. Holding the sprint key
makes it travel twice as fast.

While the camera is free the game is looked at the way it always was: the view does not bob and
the field of view does not change with the speed. Both are put back the way they were when the
camera is returned to the player.

The player is left alone while the camera is free. It keeps its place and its direction, the keys
of the movement are thrown away before they reach it, and the movement packets carry the same
position and the same rotation as they did before — the server sees a player that stands still and
does not even look around. A player that was sneaking keeps sneaking, so that it does not fall off
a ledge while the camera is somewhere else; the setting `stay_sneaking` says whether it does.

The camera is not written to the configuration file, so it is never free by accident when the game
is opened.

### Choosing the blocks

The BlockESP does not look for a fixed group of blocks: the blocks are chosen by the player.
The setting `blocks` opens a picker of its own, in which every block of the game can be found
and chosen:

- the field at the top searches by the name of the block and by its identifier, so that
  `diamond` finds both the diamond ore and the diamond block;
- the groups at the bottom fill the list with the ores, with everything that is worth carrying
  away or with the blocks that are rare;
- the button `Looked at` adds the block the player is looking at, which is the quickest way to
  build a list;
- a block that is chosen is marked with a bar of its own colour and with the colour itself, and a
  block that is clicked again is taken away;
- the button `Reset` puts the list of blocks that are worth looking for back.

Every block has a colour of its own, so that the blocks of one list can be told apart at a
glance. A chest is orange, a diamond ore is the colour of a diamond, and the shulker boxes
keep the colours they are named after. Clicking a block with the **right** button opens the way
that block is shown: a bar for the hue, a bar for the saturation, a bar for the brightness and
the sixteen colours of the dyes of the game.

That screen holds more than the colour. The block may be drawn by its edges, by its faces, by both
or as a filled box, and a line may be drawn from the middle of the screen to it, in a colour of its
own. Every block may be shown in its own way; a block that is not given one follows the setting
`shape` and the setting `tracer` of the feature.

The BlockESP draws every block in the colour it was given. Blocks of the same kind that touch each
other are drawn as one shape: the edges and the faces that are hidden inside the group are left
out, so that a vein of ore looks like one lump instead of a heap of boxes. A line is drawn to the
middle of such a group rather than to every block of it, so that a whole vein does not fill the
screen with lines.

The overlay keeps up with the world: when a block is broken or placed, the chunk it lies in is
looked through again, so the block disappears at once and a new one appears at once.

The feature writes in the log how many blocks it is looking for, which is the first thing to look
at when nothing is shown.

The list is kept in the configuration file as the identifiers of the blocks, their colours and the
way they are shown:

```json
[ { "id": "minecraft:chest", "colour": "#F9801D", "shape": "LINES", "tracer": true } ]
```

A list that was written before the blocks had colours of their own is an array of plain
identifiers, and is still read; every block in it is given the colour it has by default.

## The opening animation

When the game opens, the client plays the logo over the title screen: the blue slash burns from
one end to the other, the letter appears out of it, and the name is typed out letter by letter.
The logo holds for a moment and then fades away, leaving the title screen as it was. Pressing
**Escape** skips it.

The same logo is shown when the player joins a server. It is drawn on the HUD rather than as a
screen of its own, so that the player can play while it is on screen: it fades in over the
world, holds for a moment and fades away again.

The animation is split in two, like everything else here. `core/gui/IntroTimeline` and
`core/gui/JoinLogoTimeline` hold the timing and turn a number of seconds into a frame; they are
pure Java and are covered by the tests. `platform/screen/IntroScreen` and
`platform/screen/JoinLogoOverlay` are the stages: they draw the three layers of the logo and ask
the timeline what should be on screen. The layers themselves live in
`assets/birowo/textures/gui/`: `logo_mark.png` for the letter, `logo_slash.png` for the slash and
`logo_word.png` for the name.

## Commands

| Command              | What it does                                  |
| -------------------- | --------------------------------------------- |
| `.birowo`            | lists the commands                            |
| `.birowo list`       | lists every feature and whether it is enabled  |
| `.birowo toggle <f>` | switches a feature on or off                   |

## Building

The build uses the Gradle wrapper and needs a Java 21 toolchain, which the toolchain resolver in
`settings.gradle` provisions on its own.

```bash
./gradlew build      # compiles and runs the tests
./gradlew test       # runs the tests only
./gradlew runClient  # starts the game with the client
```

## Configuration

| File          | What it holds                                        |
| ------------- | ---------------------------------------------------- |
| `birowo.json` | the state and the settings of every feature           |
| `birowo_gui.json` | the positions of the panels of the interface       |

Both files are written to the config directory of the game. A key the current build does not know
about is kept verbatim, so a file written by another version never costs the user their settings,
and a file that cannot be parsed is backed up rather than thrown away.
