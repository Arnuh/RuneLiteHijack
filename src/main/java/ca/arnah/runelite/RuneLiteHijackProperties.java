package ca.arnah.runelite;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import okhttp3.HttpUrl;

/**
 * @author Arnah
 * @since Dec 15, 2021
 **/
public class RuneLiteHijackProperties{
	
	private static final String PLUGINHUB_BASE = "runelitehijack.pluginhub.url";
	
	public static List<HttpUrl> getPluginHubBase(){
		return Arrays.stream(System.getProperty(PLUGINHUB_BASE, "https://raw.githubusercontent.com/Arnuh/RuneLiteHijack-PluginHub/master/").split(","))
			.map(HttpUrl::parse)
			.collect(Collectors.toList());
	}
}
