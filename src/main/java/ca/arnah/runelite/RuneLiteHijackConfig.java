package ca.arnah.runelite;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(RuneLiteHijackConfig.GROUP_NAME)
public interface RuneLiteHijackConfig extends Config{
	
	String GROUP_NAME = "runelitehijack";
	
	@ConfigItem(keyName = "pluginhub", name = "Plugin Hubs", description = "List of plugin hubs separated by commas", position = 1)
	default String urls(){
		return RuneLiteHijackProperties.PLUGINHUB_BASE_URL;
	}
}
