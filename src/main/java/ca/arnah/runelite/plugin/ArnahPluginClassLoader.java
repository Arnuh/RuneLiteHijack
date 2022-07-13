package ca.arnah.runelite.plugin;

import java.net.URL;
import java.net.URLClassLoader;
import lombok.Getter;

/**
 * @author Arnah
 * @since Nov 08, 2020
 */
class ArnahPluginClassLoader extends URLClassLoader{
	
	@Getter
	private final ArnahPluginManifest manifest;
	
	ArnahPluginClassLoader(ArnahPluginManifest manifest, URL[] urls){
		super(urls, ArnahPluginClassLoader.class.getClassLoader());
		this.manifest = manifest;
	}
}