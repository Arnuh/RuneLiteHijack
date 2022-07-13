package ca.arnah.runelite.events;

import java.util.List;
import ca.arnah.runelite.plugin.ArnahPluginManifest;
import lombok.Value;

@Value
public class ArnahPluginsChanged{
	
	List<ArnahPluginManifest> loadedManifest;
}