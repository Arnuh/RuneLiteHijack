Random project I made to run my own custom or modified external plugins on the original RuneLite files.
**Not a detected 3rd party client.** Does not expand on RuneLites api like some RuneLite forks use to do.

# Installation

- Download latest RuneLiteHijack from the releases section.
- Find RuneLite install directory.
- Place `RuneLiteHijack.jar` in the same folder as `config.json`
- Open `config.json`
- Add a comma after `"RuneLite.jar"`
- Add `"RuneLiteHijack.jar"` after classPath entry `"RuneLite.jar"`
- Change mainClass to `ca.arnah.runelite.LauncherHijack`
- Run RuneLite normally.

![example](https://im.arnah.ca/3cB8zf5ZaE.png)

If properly done, you should see "RuneLiteHijack Plugin Hub" or a "RuneLiteHijack" plugin in plugin configuration

![example](https://im.arnah.ca/Bn1tEIgJLC9rWGF.png)

# Adding additional Plugin Hubs

RuneLite Hijack comes with a builtin plugin that allows you to add additional plugin hubs. You can access this plugin like the similar "RuneLite" builtin
plugin.

![example](https://im.arnah.ca/c4orAVtodc7VkeE.png)

The default plugin hub is a GitHub repository viewable [here](https://github.com/Arnuh/RuneLiteHijack-PluginHub). You can use this as a template to create
your own plugin hub.

For GitHub repositories, you need to make sure the url is a "raw githubusercontent" link
like the following `https://raw.githubusercontent.com/Arnuh/RuneLiteHijack-PluginHub/master/`.

With the "Plugin Hubs" settings, you can add additional plugin hubs to the list by using a comma separated list of urls.

![Plugin Hub Example](https://im.arnah.ca/jHNzJb81jtxWDfP.png)

# Structure of Plugin Hubs

Every Plugin Hub must have a `plugins.json` file at the root, which will list all available plugins. The "provider" value will be a subfolder which will
contain the `internalName.jar` file.

Description of each property for each plugin in `plugins.json`:

- "internalName" is the name of the plugin jar file, not including the extension.
- "hash" is the SHA-256 hash of the plugin jar file.
- "size" is the size of the plugin jar file in bytes.
- "plugins" is a list of main plugin classes
- "displayName" is the name of the plugin when displayed in the plugin hub.
- "provider" is the name of the subfolder containing the plugin jar file.

Example viewable [here](https://github.com/Arnuh/RuneLiteHijack-PluginHub/tree/master).

# How it works

Since RuneLite lets you add to the classpath and modify the main class, this project is able to proceed normally, while loading alongside RuneLite.

1. LauncherHijack launches the normal RuneLite Launcher and waits for the Client to start while scanning for the Client ClassLoader.
2. When the ClassLoader is found, LauncherHijack adds the RuneLiteHijack jar into the ClassLoader and creates ClientHijack.
3. ClientHijacks waits for the RuneLite Injector to be initialized and adds HijackedClient into the injector.
4. With HijackedClient being created in RuneLites classloader and being started via the Injector, RuneLiteHijack has full access to the same functionality as
   RuneLite.
5. With the access given, RuneLiteHijack initializes its Plugin Manager and adds the button to the original plugin ui.

Limitations are being restricted to what can be modified at runtime in java.