package com.birowo.client.core.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.birowo.client.core.event.EventBus;
import com.birowo.client.core.event.TickEvent;

class FeatureRegistryTest {
	/**
	 * A feature that counts what the registry does to it.
	 */
	private static final class CountingFeature extends Feature {
		private int enables;
		private int disables;
		private int ticks;
		private boolean broken;

		private CountingFeature(String id) {
			super(id, id, "a feature that counts", FeatureCategory.MISC);
		}

		@Override
		protected void onEnable() {
			enables++;
		}

		@Override
		protected void onDisable() {
			disables++;
		}

		@Override
		public void onTick() {
			ticks++;

			if (broken) {
				throw new IllegalStateException("this feature is broken");
			}
		}
	}

	@Test
	void aFeatureIsEnabledAndDisabledExactlyOnce() {
		EventBus bus = new EventBus();
		FeatureRegistry registry = new FeatureRegistry(bus);
		CountingFeature feature = new CountingFeature("counting");

		registry.register(feature);

		feature.setEnabled(true);
		feature.setEnabled(true);
		feature.setEnabled(false);
		feature.setEnabled(false);

		assertEquals(1, feature.enables);
		assertEquals(1, feature.disables);
	}

	@Test
	void onlyTheEnabledFeaturesAreTicked() {
		EventBus bus = new EventBus();
		FeatureRegistry registry = new FeatureRegistry(bus);
		CountingFeature feature = new CountingFeature("counting");

		registry.register(feature);

		bus.post(new TickEvent(0));

		assertEquals(0, feature.ticks);

		feature.setEnabled(true);
		bus.post(new TickEvent(1));

		assertEquals(1, feature.ticks);

		feature.setEnabled(false);
		bus.post(new TickEvent(2));

		assertEquals(1, feature.ticks);
	}

	@Test
	void aFeatureThatFailsDoesNotStopTheOthers() {
		EventBus bus = new EventBus();
		FeatureRegistry registry = new FeatureRegistry(bus);
		CountingFeature broken = new CountingFeature("broken");
		CountingFeature healthy = new CountingFeature("healthy");

		registry.register(broken);
		registry.register(healthy);

		broken.setEnabled(true);
		healthy.setEnabled(true);
		broken.broken = true;

		bus.post(new TickEvent(0));

		assertEquals(1, broken.ticks);
		assertEquals(1, healthy.ticks);
	}

	@Test
	void aFeatureIsFoundByItsIdentifier() {
		EventBus bus = new EventBus();
		FeatureRegistry registry = new FeatureRegistry(bus);
		CountingFeature feature = new CountingFeature("counting");

		registry.register(feature);

		assertTrue(registry.byId("counting").isPresent());
		assertTrue(registry.byId("missing").isEmpty());
	}

	@Test
	void aFeatureCannotBeRegisteredTwice() {
		EventBus bus = new EventBus();
		FeatureRegistry registry = new FeatureRegistry(bus);

		registry.register(new CountingFeature("counting"));

		assertThrows(IllegalArgumentException.class, () -> registry.register(new CountingFeature("counting")));
	}

	@Test
	void theEnabledFeaturesAreListedInTheOrderTheyWereRegistered() {
		EventBus bus = new EventBus();
		FeatureRegistry registry = new FeatureRegistry(bus);
		CountingFeature first = new CountingFeature("first");
		CountingFeature second = new CountingFeature("second");

		registry.register(first);
		registry.register(second);

		second.setEnabled(true);
		first.setEnabled(true);

		List<String> enabled = new ArrayList<>();

		for (Feature feature : registry.enabledFeatures()) {
			enabled.add(feature.id());
		}

		assertEquals(List.of("first", "second"), enabled);
		assertFalse(registry.features().isEmpty());
	}
}
