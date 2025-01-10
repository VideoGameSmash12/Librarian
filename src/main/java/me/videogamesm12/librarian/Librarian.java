package me.videogamesm12.librarian;

import com.google.common.eventbus.EventBus;
import lombok.Getter;
import me.videogamesm12.librarian.api.AbstractEventListener;
import me.videogamesm12.librarian.api.IMechanicFactory;
import me.videogamesm12.librarian.api.IWrappedHotbarStorage;
import me.videogamesm12.librarian.api.addon.AddonMeta;
import me.videogamesm12.librarian.api.addon.IAddon;
import me.videogamesm12.librarian.api.event.CacheClearEvent;
import me.videogamesm12.librarian.api.event.NavigationEvent;
import me.videogamesm12.librarian.api.event.ReloadPageEvent;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigInteger;
import java.util.*;

@Getter
public class Librarian implements ClientModInitializer
{
	private static final List<String> requiredApis = Arrays.asList("fabric", "legacy-fabric-api", "osl");
	@Getter
	private static final Logger logger = LogManager.getLogger("Librarian");

	@Getter
	private static Librarian instance;

	private IMechanicFactory mechanic;
	private List<AbstractEventListener> listeners = new ArrayList<>();
	private Map<Class<? extends IAddon>, IAddon> addons = new HashMap<>();

	private BigInteger currentPageNumber = BigInteger.ZERO;
	private final Map<BigInteger, IWrappedHotbarStorage> map = new HashMap<>();

	private EventBus eventBus = new EventBus();

	@Override
	public void onInitializeClient()
	{
		instance = this;

		// We need a mechanic to manufacture the nitty-gritty version-specific code.
		mechanic = FabricLoader.getInstance().getEntrypoints("librarian-mechanic", IMechanicFactory.class).stream()
				.findAny().orElseThrow(() -> {
					if (requiredApis.stream().noneMatch(e -> FabricLoader.getInstance().isModLoaded(e)))
						return new IllegalStateException("Missing one or more mods that Librarian requires for this version");
					else
						return new IllegalStateException("This version of Minecraft is not supported");
				});

		// Event listeners can be specified for various different events
		listeners.addAll(FabricLoader.getInstance().getEntrypoints("librarian-listener", AbstractEventListener.class));

		// Add-ons
		FabricLoader.getInstance().getEntrypoints("librarian-addon", IAddon.class).stream().filter(addon ->
		{
			if (!addon.getClass().isAnnotationPresent(AddonMeta.class)) return false;
			AddonMeta meta = addon.getClass().getAnnotation(AddonMeta.class);

			return Arrays.stream(meta.requiredMods()).allMatch(mod -> FabricLoader.getInstance().isModLoaded(mod))
					&& Arrays.stream(meta.incompatibleMods()).noneMatch(mod -> FabricLoader.getInstance().isModLoaded(mod));
		}).forEach(addon -> addons.put(addon.getClass(), addon));

		// Initialize our add-ons
		addons.values().forEach(IAddon::init);
	}

	public <T extends IAddon> T getAddon(Class<T> clazz)
	{
		return (T) addons.get(clazz);
	}

	public <T extends IAddon> Optional<T> getOptionalAddon(Class<T> clazz)
	{
		return Optional.ofNullable(getAddon(clazz));
	}

	public <T extends IAddon> Optional<T> getOptionalAddon(String clazz)
	{
		Class<T> glass;

		try
		{
			glass = (Class<T>) Class.forName(clazz, false, getClass().getClassLoader());
			return getOptionalAddon(glass);
		}
		catch (NoClassDefFoundError | ClassNotFoundException ex)
		{
			return Optional.empty();
		}
	}

	public IWrappedHotbarStorage getCurrentPage()
	{
		return getHotbarPage(currentPageNumber);
	}

	public void setPage(BigInteger value)
	{
		BigInteger previous = currentPageNumber;
		currentPageNumber = value;
		eventBus.post(new NavigationEvent(previous, currentPageNumber));
	}

	public void setPage(long value)
	{
		setPage(BigInteger.valueOf(value));
	}

	public void advanceBy(long value)
	{
		setPage(currentPageNumber.add(BigInteger.valueOf(value)));
	}

	public void nextPage()
	{
		setPage(currentPageNumber.add(BigInteger.ONE));
	}

	public void previousPage()
	{
		setPage(currentPageNumber.subtract(BigInteger.ONE));
	}

	public void reloadCurrentPage()
	{
		getCurrentPage().load();
		eventBus.post(new ReloadPageEvent(currentPageNumber));
	}

	public void clearCache()
	{
		map.clear();
		eventBus.post(new CacheClearEvent(currentPageNumber));
	}

	public IWrappedHotbarStorage getHotbarPage(BigInteger page)
	{
		if (!map.containsKey(page))
		{
			map.put(page, mechanic.createHotbarStorage(page));
		}

		return map.get(page);
	}
}
