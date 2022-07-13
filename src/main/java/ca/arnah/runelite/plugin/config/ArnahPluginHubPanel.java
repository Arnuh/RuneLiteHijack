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
package ca.arnah.runelite.plugin.config;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.LayoutStyle;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import ca.arnah.runelite.events.ArnahPluginsChanged;
import ca.arnah.runelite.plugin.ArnahPluginClient;
import ca.arnah.runelite.plugin.ArnahPluginManager;
import ca.arnah.runelite.plugin.ArnahPluginManifest;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.html.HtmlEscapers;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.Config;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.config.PluginSearch;
import net.runelite.client.plugins.config.SearchablePlugin;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.IconTextField;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.SwingUtil;

@Slf4j
public class ArnahPluginHubPanel extends PluginPanel{
	
	private static final ImageIcon MISSING_ICON;
	private static final ImageIcon HELP_ICON;
	private static final ImageIcon HELP_ICON_HOVER;
	private static final ImageIcon CONFIGURE_ICON;
	private static final ImageIcon CONFIGURE_ICON_HOVER;
	private static final Pattern SPACES = Pattern.compile(" +");
	private static final ImageIcon SECTION_EXPAND_ICON;
	private static final ImageIcon SECTION_EXPAND_ICON_HOVER;
	private static final ImageIcon SECTION_RETRACT_ICON;
	private static final ImageIcon SECTION_RETRACT_ICON_HOVER;
	static final ImageIcon BACK_ICON;
	static final ImageIcon BACK_ICON_HOVER;
	
	static{
		Class<?> memes = null, memes2 = null;
		try{
			memes = Class.forName("net.runelite.client.plugins.config.PluginHubPanel");
			memes2 = Class.forName("net.runelite.client.plugins.config.ConfigPanel");
		}catch(ClassNotFoundException ex){
			ex.printStackTrace();
		}
		// null :)
		BufferedImage missingIcon = ImageUtil.getResourceStreamFromClass(memes, "pluginhub_missingicon.png");
		MISSING_ICON = new ImageIcon(missingIcon);
		
		BufferedImage helpIcon = ImageUtil.getResourceStreamFromClass(memes, "pluginhub_help.png");
		HELP_ICON = new ImageIcon(helpIcon);
		HELP_ICON_HOVER = new ImageIcon(ImageUtil.alphaOffset(helpIcon, -100));
		
		BufferedImage configureIcon = ImageUtil.getResourceStreamFromClass(memes, "pluginhub_configure.png");
		CONFIGURE_ICON = new ImageIcon(configureIcon);
		CONFIGURE_ICON_HOVER = new ImageIcon(ImageUtil.alphaOffset(configureIcon, -100));
		
		//
		final BufferedImage backIcon = ImageUtil.getResourceStreamFromClass(memes2, "config_back_icon.png");
		BACK_ICON = new ImageIcon(backIcon);
		BACK_ICON_HOVER = new ImageIcon(ImageUtil.alphaOffset(backIcon, -100));
		
		BufferedImage sectionRetractIcon = ImageUtil.getResourceStreamFromClass(memes2, "/util/arrow_right.png");
		sectionRetractIcon = ImageUtil.luminanceOffset(sectionRetractIcon, -121);
		SECTION_EXPAND_ICON = new ImageIcon(sectionRetractIcon);
		SECTION_EXPAND_ICON_HOVER = new ImageIcon(ImageUtil.alphaOffset(sectionRetractIcon, -100));
		final BufferedImage sectionExpandIcon = ImageUtil.rotateImage(sectionRetractIcon, Math.PI / 2);
		SECTION_RETRACT_ICON = new ImageIcon(sectionExpandIcon);
		SECTION_RETRACT_ICON_HOVER = new ImageIcon(ImageUtil.alphaOffset(sectionExpandIcon, -100));
	}
	
	private class PluginItem extends JPanel implements SearchablePlugin{
		
		private static final int HEIGHT = 70;
		private static final int ICON_WIDTH = 48;
		private static final int BOTTOM_LINE_HEIGHT = 16;
		
		private final ArnahPluginManifest manifest;
		
		@Getter
		private final List<String> keywords = new ArrayList<>();
		
		@Getter
		private final boolean installed;
		
		PluginItem(ArnahPluginManifest newManifest, Collection<Plugin> loadedPlugins, boolean installed){
			ArnahPluginManifest loaded = null;
			if(!loadedPlugins.isEmpty()){
				loaded = ArnahPluginManager.getExternalPluginManifest(loadedPlugins.iterator().next().getClass());
			}
			
			manifest = newManifest == null ? loaded : newManifest;
			this.installed = installed;
			if(manifest != null){
				Collections.addAll(keywords, SPACES.split(manifest.getDisplayName().toLowerCase()));
				
				if(manifest.getDescription() != null){
					Collections.addAll(keywords, SPACES.split(manifest.getDescription().toLowerCase()));
				}
				
				Collections.addAll(keywords, manifest.getProvider().toLowerCase());
			}
			
			setBackground(ColorScheme.DARKER_GRAY_COLOR);
			setOpaque(true);
			
			GroupLayout layout = new GroupLayout(this);
			setLayout(layout);
			
			JLabel pluginName = new JLabel(manifest.getDisplayName());
			pluginName.setFont(FontManager.getRunescapeBoldFont());
			pluginName.setToolTipText(manifest.getDisplayName());
			
			String descriptionText = manifest.getDescription();
			if(!descriptionText.startsWith("<html>")){
				descriptionText = "<html>" + HtmlEscapers.htmlEscaper().escape(descriptionText) + "</html>";
			}
			JLabel description = new JLabel(descriptionText);
			description.setVerticalAlignment(JLabel.TOP);
			description.setToolTipText(descriptionText);
			
			JLabel icon = new JLabel();
			icon.setHorizontalAlignment(JLabel.CENTER);
			icon.setIcon(MISSING_ICON);
			
			JButton configure = new JButton(CONFIGURE_ICON);
			configure.setRolloverIcon(CONFIGURE_ICON_HOVER);
			SwingUtil.removeButtonDecorations(configure);
			configure.setToolTipText("Configure");
			if(loaded != null){
				String search = null;
				if(loadedPlugins.size() > 1){
					search = loaded.getInternalName();
				}else{
					Plugin plugin = loadedPlugins.iterator().next();
					Config cfg = pluginManager.getPluginConfigProxy(plugin);
					if(cfg == null){
						search = loaded.getInternalName();
					}else{
						configure.addActionListener(l->pluginListPanel.openConfigurationPanel(plugin));
					}
				}
				
				if(search != null){
					final String javaIsABadLanguage = search;
					configure.addActionListener(l->pluginListPanel.openWithFilter(javaIsABadLanguage));
				}
			}else{
				configure.setVisible(false);
			}
			
			boolean install = !installed;
			boolean update = loaded != null && newManifest != null && !newManifest.equals(loaded);
			boolean remove = !install && !update;
			JButton addrm = new JButton();
			if(install){
				addrm.setText("Install");
				addrm.setBackground(new Color(0x28BE28));
				addrm.addActionListener(l->{
					addrm.setText("Installing");
					addrm.setBackground(ColorScheme.MEDIUM_GRAY_COLOR);
					externalPluginManager.install(manifest.getInternalName());
				});
			}else if(remove){
				addrm.setText("Remove");
				addrm.setBackground(new Color(0xBE2828));
				addrm.addActionListener(l->{
					addrm.setText("Removing");
					addrm.setBackground(ColorScheme.MEDIUM_GRAY_COLOR);
					externalPluginManager.remove(manifest.getInternalName());
				});
			}else{
				assert update;
				addrm.setText("Update");
				addrm.setBackground(new Color(0x1F621F));
				addrm.addActionListener(l->{
					addrm.setText("Updating");
					addrm.setBackground(ColorScheme.MEDIUM_GRAY_COLOR);
					externalPluginManager.update();
				});
			}
			addrm.setBorder(new LineBorder(addrm.getBackground().darker()));
			addrm.setFocusPainted(false);
			
			layout.setHorizontalGroup(layout.createSequentialGroup().addComponent(icon, ICON_WIDTH, ICON_WIDTH, ICON_WIDTH).addGap(5).addGroup(layout.createParallelGroup().addGroup(layout.createSequentialGroup().addComponent(pluginName, 0, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)).addComponent(description, 0, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE).addGroup(layout.createSequentialGroup().addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.PREFERRED_SIZE, 100).addComponent(configure, 0, 24, 24).addComponent(addrm, 0, 50, GroupLayout.PREFERRED_SIZE).addGap(5))));
			
			int lineHeight = description.getFontMetrics(description.getFont()).getHeight();
			layout.setVerticalGroup(layout.createParallelGroup().addComponent(icon, HEIGHT, GroupLayout.DEFAULT_SIZE, HEIGHT + lineHeight).addGroup(layout.createSequentialGroup().addGap(5).addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(pluginName)).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE).addComponent(description, lineHeight, GroupLayout.PREFERRED_SIZE, lineHeight * 2).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE).addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(configure, BOTTOM_LINE_HEIGHT, BOTTOM_LINE_HEIGHT, BOTTOM_LINE_HEIGHT).addComponent(addrm, BOTTOM_LINE_HEIGHT, BOTTOM_LINE_HEIGHT, BOTTOM_LINE_HEIGHT)).addGap(5)));
		}
		
		@Override
		public String getSearchableName(){
			return manifest.getDisplayName();
		}
	}
	
	private final ArnahPluginListPanel pluginListPanel;
	private final ArnahPluginManager externalPluginManager;
	private final PluginManager pluginManager;
	private final ArnahPluginClient externalPluginClient;
	private final ScheduledExecutorService executor;
	
	private final IconTextField searchBar;
	private final JLabel refreshing;
	private final JPanel mainPanel;
	private List<PluginItem> plugins = null;
	
	@Inject
	public ArnahPluginHubPanel(ArnahPluginListPanel pluginListPanel, ArnahPluginManager externalPluginManager, PluginManager pluginManager, ArnahPluginClient externalPluginClient, ScheduledExecutorService executor){
		super(false);
		this.pluginListPanel = pluginListPanel;
		this.externalPluginManager = externalPluginManager;
		this.pluginManager = pluginManager;
		this.externalPluginClient = externalPluginClient;
		this.executor = executor;
		
		{
			Object refresh = "this could just be a lambda, but no, it has to be abstracted";
			getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), refresh);
			getActionMap().put(refresh, new AbstractAction(){
				@Override
				public void actionPerformed(ActionEvent e){
					reloadPluginList();
				}
			});
		}
		
		GroupLayout layout = new GroupLayout(this);
		setLayout(layout);
		setBackground(ColorScheme.DARK_GRAY_COLOR);
		
		searchBar = new IconTextField();
		searchBar.setIcon(IconTextField.Icon.SEARCH);
		searchBar.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		searchBar.setHoverBackgroundColor(ColorScheme.DARK_GRAY_HOVER_COLOR);
		searchBar.getDocument().addDocumentListener(new DocumentListener(){
			@Override
			public void insertUpdate(DocumentEvent e){
				filter();
			}
			
			@Override
			public void removeUpdate(DocumentEvent e){
				filter();
			}
			
			@Override
			public void changedUpdate(DocumentEvent e){
				filter();
			}
		});
		
		JLabel externalPluginWarning = new JLabel("<html>Runelite sucks</html>");
		externalPluginWarning.setBackground(new Color(0xFFBB33));
		externalPluginWarning.setForeground(Color.BLACK);
		externalPluginWarning.setBorder(new EmptyBorder(5, 5, 5, 2));
		externalPluginWarning.setOpaque(true);
		
		JLabel externalPluginWarning2 = new JLabel("Use at your own risk!");
		externalPluginWarning2.setHorizontalAlignment(JLabel.CENTER);
		externalPluginWarning2.setFont(FontManager.getRunescapeBoldFont());
		externalPluginWarning2.setBackground(externalPluginWarning.getBackground());
		externalPluginWarning2.setForeground(externalPluginWarning.getForeground());
		externalPluginWarning2.setBorder(new EmptyBorder(0, 5, 5, 5));
		externalPluginWarning2.setOpaque(true);
		
		JButton backButton = new JButton(BACK_ICON);
		backButton.setRolloverIcon(BACK_ICON_HOVER);
		SwingUtil.removeButtonDecorations(backButton);
		backButton.setToolTipText("Back");
		backButton.addActionListener(l->pluginListPanel.getMuxer().popState());
		
		mainPanel = new JPanel();
		mainPanel.setBorder(BorderFactory.createEmptyBorder(0, 7, 7, 7));
		mainPanel.setLayout(new DynamicGridLayout(0, 1, 0, 5));
		mainPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		
		refreshing = new JLabel("Loading...");
		refreshing.setHorizontalAlignment(JLabel.CENTER);
		
		JPanel mainPanelWrapper = new FixedWidthPanel();
		mainPanelWrapper.setLayout(new BorderLayout());
		mainPanelWrapper.add(mainPanel, BorderLayout.NORTH);
		mainPanelWrapper.add(refreshing, BorderLayout.CENTER);
		
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		// Can't use Short.MAX_VALUE like the docs say because of JDK-8079640
		scrollPane.setPreferredSize(new Dimension(0x7000, 0x7000));
		scrollPane.setViewportView(mainPanelWrapper);
		
		layout.setVerticalGroup(layout.createSequentialGroup().addComponent(externalPluginWarning).addComponent(externalPluginWarning2).addGap(10).addGroup(layout.createParallelGroup().addComponent(backButton).addComponent(searchBar)).addGap(10).addComponent(scrollPane));
		
		layout.setHorizontalGroup(layout.createParallelGroup().addComponent(externalPluginWarning, 0, Short.MAX_VALUE, Short.MAX_VALUE).addComponent(externalPluginWarning2, 0, Short.MAX_VALUE, Short.MAX_VALUE).addGroup(layout.createSequentialGroup().addComponent(backButton).addComponent(searchBar).addGap(10)).addComponent(scrollPane));
		
		revalidate();
		
		refreshing.setVisible(false);
		reloadPluginList();
	}
	
	private void reloadPluginList(){
		if(refreshing.isVisible()){
			return;
		}
		
		refreshing.setVisible(true);
		mainPanel.removeAll();
		
		executor.submit(()->{
			List<ArnahPluginManifest> manifest;
			try{
				manifest = externalPluginClient.downloadManifest();
			}catch(IOException ex){
				log.error("", ex);
				SwingUtilities.invokeLater(()->{
					refreshing.setVisible(false);
					mainPanel.add(new JLabel("Downloading the plugin manifest failed"));
					
					JButton retry = new JButton("Retry");
					retry.addActionListener(l->reloadPluginList());
					mainPanel.add(retry);
				});
				return;
			}
			
			reloadPluginList(manifest);
		});
	}
	
	private void reloadPluginList(List<ArnahPluginManifest> manifest){
		Map<String, ArnahPluginManifest> manifests = manifest.stream()
			.collect(ImmutableMap.toImmutableMap(ArnahPluginManifest::getInternalName, Function.identity()));
		
		Multimap<String, Plugin> loadedPlugins = HashMultimap.create();
		for(Plugin p : pluginManager.getPlugins()){
			Class<? extends Plugin> clazz = p.getClass();
			ArnahPluginManifest mf = ArnahPluginManager.getExternalPluginManifest(clazz);
			if(mf != null){
				loadedPlugins.put(mf.getInternalName(), p);
			}
		}
		
		Set<String> installed = new HashSet<>(externalPluginManager.getInstalledExternalPlugins());
		
		SwingUtilities.invokeLater(()->{
			plugins = Sets.union(manifests.keySet(), loadedPlugins.keySet()).stream()
				.map(id->new PluginItem(manifests.get(id), loadedPlugins.get(id), installed.contains(id)))
				.collect(Collectors.toList());
			
			refreshing.setVisible(false);
			filter();
		});
	}
	
	void filter(){
		if(refreshing.isVisible()){
			return;
		}
		
		mainPanel.removeAll();
		
		Stream<PluginItem> stream = plugins.stream();
		
		String query = searchBar.getText();
		boolean isSearching = query != null && !query.trim().isEmpty();
		if(isSearching){
			PluginSearch.search(plugins, query).forEach(mainPanel::add);
		}else{
			stream.sorted(Comparator.comparing(PluginItem::isInstalled).thenComparing(p->p.manifest.getDisplayName())).forEach(mainPanel::add);
		}
		
		mainPanel.revalidate();
	}
	
	@Override
	public void onActivate(){
		revalidate();
		searchBar.setText("");
		reloadPluginList();
		searchBar.requestFocusInWindow();
	}
	
	@Subscribe
	private void onArnahPluginsChanged(ArnahPluginsChanged ev){
		SwingUtilities.invokeLater(()->reloadPluginList(ev.getLoadedManifest()));
	}
}