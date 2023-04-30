/*
 * Copyright (c) 2019 Abex
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package ca.arnah.runelite.plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.SwingUtilities;
import ca.arnah.runelite.RuneLiteHijackProperties;
import ca.arnah.runelite.events.ArnahPluginsChanged;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.google.common.hash.HashingInputStream;
import com.google.common.io.Files;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginInstantiationException;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.ui.SplashScreen;
import net.runelite.client.util.CountingInputStream;
import net.runelite.client.util.Text;
import net.runelite.client.util.VerificationException;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * @author Arnah
 * @since Nov 08, 2020
 */
@Singleton
@Slf4j
public class ArnahPluginManager{
	
	private static final String GROUP_NAME = "hijack";
	private static final String PLUGIN_LIST_KEY = "plugins";
	
	private final ConfigManager configManager;
	private final ArnahPluginClient externalPluginClient;
	private final ScheduledExecutorService executor;
	private final PluginManager pluginManager;
	private final EventBus eventBus;
	private final OkHttpClient okHttpClient;
	
	@Inject
	public ArnahPluginManager(ConfigManager configManager, ArnahPluginClient externalPluginClient, ScheduledExecutorService executor, PluginManager pluginManager, EventBus eventBus, OkHttpClient okHttpClient){
		this.configManager = configManager;
		this.externalPluginClient = externalPluginClient;
		this.executor = executor;
		this.pluginManager = pluginManager;
		this.eventBus = eventBus;
		this.okHttpClient = okHttpClient;
	}
	
	public void loadExternalPlugins() throws PluginInstantiationException{
		refreshPlugins();
	}
	
	private void refreshPlugins(){
		Multimap<ArnahPluginManifest, Plugin> loadedExternalPlugins = HashMultimap.create();
		for(Plugin p : pluginManager.getPlugins()){
			ArnahPluginManifest m = getExternalPluginManifest(p.getClass());
			if(m != null){
				loadedExternalPlugins.put(m, p);
			}
		}
		
		List<String> installedIDs = getInstalledExternalPlugins();
		if(installedIDs.isEmpty() && loadedExternalPlugins.isEmpty()){
			return;
		}
		
		boolean startup = SplashScreen.isOpen();
		try{
			String pluginHubUrls = RuneLiteHijackProperties.getPluginHubProperty();
			double splashStart = startup ? .60 : 0;
			double splashLength = startup ? .10 : 1;
			if(!startup){
				SplashScreen.init();
			}
			SplashScreen.stage(splashStart, null, "Downloading RuneLiteHijack plugins");
			Set<ArnahPluginManifest> externalPlugins = new HashSet<>();
			List<ArnahPluginManifest> manifestList;
			try{
				ArnahPluginManifest.PLUGINS_DIR.mkdirs();
				
				manifestList = externalPluginClient.downloadManifest();
				Map<String, ArnahPluginManifest> manifests = manifestList.stream()
					.collect(ImmutableMap.toImmutableMap(ArnahPluginManifest::getInternalName, Function.identity()));
				Set<ArnahPluginManifest> needsDownload = new HashSet<>();
				Set<File> keep = new HashSet<>();
				
				for(String name : installedIDs){
					ArnahPluginManifest manifest = manifests.get(name);
					if(manifest == null){
						continue;
					}
					
					externalPlugins.add(manifest);
					
					if(!manifest.isValid()){
						needsDownload.add(manifest);
					}else{
						keep.add(manifest.getJarFile());
					}
				}
				// delete old plugins
				File[] files = ArnahPluginManifest.PLUGINS_DIR.listFiles();
				if(files != null){
					for(File fi : files){
						if(!keep.contains(fi)){
							fi.delete();
						}
					}
				}
				int toDownload = needsDownload.stream().mapToInt(ArnahPluginManifest::getSize).sum();
				int downloaded = 0;
				for(ArnahPluginManifest manifest : needsDownload){
					try(Response res = okHttpClient.newCall(new Request.Builder().url(manifest.getUrl()).build()).execute()){
						int fdownloaded = downloaded;
						HashingInputStream his = new HashingInputStream(manifest.getHashType().get(), new CountingInputStream(res.body()
							.byteStream(), i->SplashScreen.stage(splashStart + (splashLength * .2), splashStart + (splashLength * .8), null, "Downloading " + manifest.getDisplayName(), i + fdownloaded, toDownload, true)));
						Files.asByteSink(manifest.getJarFile()).writeFrom(his);
						String hash = his.hash().toString();
						if(!hash.equals(manifest.getHash())){
							log.error("{} != {}", hash, manifest.getHash());
							throw new VerificationException("Plugin " + manifest.getDisplayName() + " didn't match its hash");
						}
					}catch(IOException | VerificationException ex){
						externalPlugins.remove(manifest);
						log.error("Unable to download RuneLiteHijack plugin \"{}\"", manifest.getDisplayName(), ex);
					}
				}
			}catch(IOException ex){
				log.error("Unable to download RuneLiteHijack plugins", ex);
				return;
			}
			
			SplashScreen.stage(splashStart + (splashLength * .8), null, "Starting RuneLiteHijack plugins");
			
			// TODO(abex): make sure the plugins get fully removed from the scheduler/eventbus/other managers (iterate and check classloader)
			Set<ArnahPluginManifest> add = new HashSet<>();
			for(ArnahPluginManifest ex : externalPlugins){
				if(loadedExternalPlugins.removeAll(ex).size() <= 0){
					add.add(ex);
				}
			}
			// list of loaded external plugins that aren't in the manifest
			Collection<Plugin> remove = loadedExternalPlugins.values();
			
			for(Plugin p : remove){
				log.info("Stopping RuneLiteHijack plugin \"{}\"", p.getClass());
				try{
					SwingUtilities.invokeAndWait(()->{
						try{
							pluginManager.stopPlugin(p);
						}catch(Exception ex){
							throw new RuntimeException(ex);
						}
					});
				}catch(InterruptedException | InvocationTargetException ex){
					log.warn("Unable to stop RuneLiteHijack plugin \"{}\"", p.getClass().getName(), ex);
				}
				pluginManager.remove(p);
			}
			
			for(ArnahPluginManifest manifest : add){
				// I think this can't happen, but just in case
				if(!manifest.isValid()){
					log.warn("Invalid plugin for validated manifest: {}", manifest);
					continue;
				}
				
				log.info("Loading RuneLiteHijack plugin \"{}\"", manifest.getDisplayName());
				
				List<Plugin> newPlugins = null;
				try{
					ClassLoader cl = new ArnahPluginClassLoader(manifest, new URL[]{manifest.getJarFile().toURI().toURL()});
					List<Class<?>> clazzes = new ArrayList<>();
					URL url = cl.getResource("META-INF/extensions.idx");
					if(url != null){
						try(InputStream is = url.openStream()){
							for(String line : new String(is.readAllBytes()).split("\n")){
								if(line.startsWith("#")) continue;
								clazzes.add(cl.loadClass(line));
							}
						}
					}
					if(manifest.getPlugins() != null){
						for(String className : manifest.getPlugins()){
							clazzes.add(cl.loadClass(className));
						}
					}
					
					List<Plugin> newPlugins2 = newPlugins = pluginManager.loadPlugins(clazzes, null);
					if(!startup){
						pluginManager.loadDefaultPluginConfiguration(newPlugins);
						
						SwingUtilities.invokeAndWait(()->{
							try{
								for(Plugin p : newPlugins2){
									pluginManager.startPlugin(p);
								}
							}catch(PluginInstantiationException ex){
								throw new RuntimeException(ex);
							}
						});
					}
				}catch(ThreadDeath e){
					throw e;
				}catch(Throwable e){
					log.warn("Unable to start or load RuneLiteHijack plugin \"{}\"", manifest.getDisplayName(), e);
					if(newPlugins != null){
						for(Plugin p : newPlugins){
							try{
								SwingUtilities.invokeAndWait(()->{
									try{
										pluginManager.stopPlugin(p);
									}catch(Exception e2){
										throw new RuntimeException(e2);
									}
								});
							}catch(InterruptedException | InvocationTargetException e2){
								log.info("Unable to fully stop plugin \"{}\"", manifest.getDisplayName(), e2);
							}
							pluginManager.remove(p);
						}
					}
				}
			}
			if(!startup){
				eventBus.post(new ArnahPluginsChanged(manifestList));
			}
			// Allows plugins to adjust the plugin hub urls if they want
			// We then need to check plugins again with the new urls
			if(!pluginHubUrls.equals(RuneLiteHijackProperties.getPluginHubProperty())){
				update();
			}
		}finally{
			if(!startup){
				SplashScreen.stop();
			}
		}
	}
	
	public List<String> getInstalledExternalPlugins(){
		String externalPluginsStr = configManager.getConfiguration(GROUP_NAME, PLUGIN_LIST_KEY);
		return Text.fromCSV(externalPluginsStr == null ? "" : externalPluginsStr);
	}
	
	public void install(String key){
		Set<String> plugins = new HashSet<>(getInstalledExternalPlugins());
		if(plugins.add(key)){
			configManager.setConfiguration(GROUP_NAME, PLUGIN_LIST_KEY, Text.toCSV(plugins));
			executor.submit(this::refreshPlugins);
		}
	}
	
	public void remove(String key){
		Set<String> plugins = new HashSet<>(getInstalledExternalPlugins());
		if(plugins.remove(key)){
			configManager.setConfiguration(GROUP_NAME, PLUGIN_LIST_KEY, Text.toCSV(plugins));
			executor.submit(this::refreshPlugins);
		}
	}
	
	public void update(){
		executor.submit(this::refreshPlugins);
	}
	
	public static ArnahPluginManifest getExternalPluginManifest(Class<? extends Plugin> plugin){
		ClassLoader cl = plugin.getClassLoader();
		if(cl instanceof ArnahPluginClassLoader){
			ArnahPluginClassLoader ecl = (ArnahPluginClassLoader) cl;
			return ecl.getManifest();
		}
		return null;
	}
}