/*
 * Copyright (c) 2017, Adam <Adam@sigterm.info>
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
package ca.arnah.runelite.plugin.config;

import java.awt.BorderLayout;
import java.awt.Component;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import ca.arnah.runelite.events.ArnahPluginsChanged;
import ca.arnah.runelite.plugin.ArnahPluginManager;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginInstantiationException;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.config.ConfigPlugin;
import net.runelite.client.ui.MultiplexingPluginPanel;
import net.runelite.client.ui.PluginPanel;

@Slf4j
public class ArnahPluginListPanel{
	
	private final ConfigManager configManager;
	private final PluginManager pluginManager;
	
	private final Provider<ArnahPluginHubPanel> pluginHubPanelProvider;
	@Getter
	private static Object objPluginListPanel;
	@Getter
	private PluginPanel pluginListPanel;// PluginListPanel
	
	@Getter
	private final ArnahPluginManager externalPluginManager;
	
	private static MultiplexingPluginPanel muxer;
	
	@Inject
	public ArnahPluginListPanel(ConfigManager configManager, PluginManager pluginManager, Provider<ArnahPluginHubPanel> pluginHubPanelProvider, ArnahPluginManager externalPluginManager, EventBus eventBus){
		this.configManager = configManager;
		this.pluginManager = pluginManager;
		this.pluginHubPanelProvider = pluginHubPanelProvider;
		this.externalPluginManager = externalPluginManager;
		eventBus.register(this);
	}
	
	public void init(){
		pluginManager.getPlugins().stream()
			.filter(ConfigPlugin.class::isInstance)
			.findAny().ifPresent(plugin->{
			             try{
				             Field field = ConfigPlugin.class.getDeclaredField("pluginListPanel");
				             field.setAccessible(true);
				             pluginListPanel = (PluginPanel) (objPluginListPanel = field.get(plugin));
				             SwingUtilities.invokeLater(()->{
					             JPanel southPanel = new FixedWidthPanel();
					             southPanel.setLayout(new BorderLayout());
					             for(Component maybeAButton : pluginListPanel.getComponents()){
						             if(maybeAButton instanceof JButton){
							             JButton button = (JButton) maybeAButton;
							             if(button.getText() == null) continue;
							             if(!button.getText().equals("Plugin Hub")) continue;
							             pluginListPanel.remove(button);
							             southPanel.add(button, "North");
						             }
					             }
					             JButton externalPluginButton = new JButton("RuneLiteHijack Plugin Hub");
					             externalPluginButton.setBorder(new EmptyBorder(5, 5, 5, 5));
					             externalPluginButton.setLayout(new BorderLayout(0, PluginPanel.BORDER_OFFSET));
					             externalPluginButton.addActionListener(l->getMuxer().pushState(pluginHubPanelProvider.get()));
					             southPanel.add(externalPluginButton, "South");
					             pluginListPanel.add(southPanel, "South");
					             rebuildPluginList();
				             });
			             }catch(Exception ex){
				             log.error("Failed to initialize RuneLiteHijack Plugin Hub", ex);
			             }
		             });
	}
	
	public MultiplexingPluginPanel getMuxer(){
		if(muxer == null){
			try{
				Field field = objPluginListPanel.getClass().getDeclaredField("muxer");
				field.setAccessible(true);
				muxer = (MultiplexingPluginPanel) field.get(objPluginListPanel);
			}catch(Exception ex){
				ex.printStackTrace();
				return null;
			}
		}
		return muxer;
	}
	
	void rebuildPluginList(){
		try{
			Method method = objPluginListPanel.getClass().getDeclaredMethod("rebuildPluginList");
			method.setAccessible(true);
			method.invoke(objPluginListPanel);
		}catch(Exception ex){
			ex.printStackTrace();
		}
		refresh();
	}
	
	void refresh(){
		// update enabled / disabled status of all items
		/*pluginList.forEach(listItem ->
		{
			final Plugin plugin = listItem.getPluginConfig().getPlugin();
			if (plugin != null)
			{
				listItem.setPluginEnabled(pluginManager.isPluginEnabled(plugin));
			}
		});*/

		/*int scrollBarPosition = scrollPane.getVerticalScrollBar().getValue();

		onSearchBarChanged();
		searchBar.requestFocusInWindow();
		validate();

		scrollPane.getVerticalScrollBar().setValue(scrollBarPosition);*/
	}
	
	void openWithFilter(String filter){
		// searchBar.setText(filter);
		// onSearchBarChanged();
		// muxer.pushState(this);
	}
	
	private void openConfigurationPanel(String configGroup){
		try{
			Method method = objPluginListPanel.getClass().getDeclaredMethod("openConfigurationPanel", String.class);
			method.setAccessible(true);
			method.invoke(objPluginListPanel, configGroup);
		}catch(Exception ex){
			ex.printStackTrace();
		}
	}
	
	void openConfigurationPanel(Plugin plugin){
		try{
			Method method = objPluginListPanel.getClass().getDeclaredMethod("openConfigurationPanel", Plugin.class);
			method.setAccessible(true);
			method.invoke(objPluginListPanel, plugin);
		}catch(Exception ex){
			ex.printStackTrace();
		}
	}
	
	void openConfigurationPanel(ArnahPluginConfigurationDescriptor plugin){
		// ConfigPanel panel = configPanelProvider.get();
		// panel.init(plugin);
		// muxer.pushState(panel);
	}
	
	void startPlugin(Plugin plugin){
		pluginManager.setPluginEnabled(plugin, true);
		
		try{
			pluginManager.startPlugin(plugin);
		}catch(PluginInstantiationException ex){
			log.warn("Error when starting plugin {}", plugin.getClass().getSimpleName(), ex);
		}
	}
	
	void stopPlugin(Plugin plugin){
		pluginManager.setPluginEnabled(plugin, false);
		
		try{
			pluginManager.stopPlugin(plugin);
		}catch(PluginInstantiationException ex){
			log.warn("Error when stopping plugin {}", plugin.getClass().getSimpleName(), ex);
		}
	}
	
	private List<String> getPinnedPluginNames(){
		/*final String config = configManager.getConfiguration(RUNELITE_GROUP_NAME, PINNED_PLUGINS_CONFIG_KEY);

		if (config == null)
		{
			return Collections.emptyList();
		}

		return Text.fromCSV(config);*/
		return List.of();
	}
	
	void savePinnedPlugins(){
/*		final String value = pluginList.stream()
			.filter(ArnahPluginListItem::isPinned)
			.map(p -> p.getPluginConfig().getName())
			.collect(Collectors.joining(","));
*/
		// configManager.setConfiguration(RUNELITE_GROUP_NAME, PINNED_PLUGINS_CONFIG_KEY, value);
	}
	
	@Subscribe
	private void onArnahPluginsChanged(ArnahPluginsChanged event){
		SwingUtilities.invokeLater(this::rebuildPluginList);
	}
}