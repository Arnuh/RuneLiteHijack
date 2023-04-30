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
	private static final String PLUGINHUB_BASE_URL = "https://raw.githubusercontent.com/Arnuh/RuneLiteHijack-PluginHub/master/";
	
	public static List<HttpUrl> getPluginHubBase(){
		return Arrays.stream(getPluginHubs()).map(HttpUrl::parse).collect(Collectors.toList());
	}
	
	public static String[] getPluginHubs(){
		return System.getProperty(PLUGINHUB_BASE, PLUGINHUB_BASE_URL).split(",");
	}
	
	public static String getPluginHubProperty(){
		return System.getProperty(PLUGINHUB_BASE, PLUGINHUB_BASE_URL);
	}
}
