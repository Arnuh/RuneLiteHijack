Random project I made to run my own custom or modified external plugins on the original RuneLite files.
**Not a detected 3rd party client.** Does not expand on RuneLites api like some RuneLite forks use to do.

# Installation

- Download latest RuneLiteHijack.
- Find RuneLite install directory.
- Place `RuneLiteHijack.jar` in the same folder as `config.json`
- Open `config.json`
- Add a comma after `"RuneLite.jar"`
- Add `"RuneLiteHijack.jar"` after classPath entry `"RuneLite.jar"`
- Change mainClass to `ca.arnah.runelite.LauncherHijack`
- Run RuneLite normally.

![example](https://im.arnah.ca/3cB8zf5ZaE.png)

If properly done, you should see "RuneLiteHijack Plugin Hub" in plugin configuration
![example](https://im.arnah.ca/m7sXtfeFvH.png)

# How it works

Since RuneLite lets you add to the classpath and modify the main class, this project is able to proceed normally, while loading alongside RuneLite.

1. LauncherHijack launches the normal RuneLite Launcher and waits for the Client to start while scanning for the Client ClassLoader.
2. When the ClassLoader is found, LauncherHijack adds the RuneLiteHijack jar into the ClassLoader and creates ClientHijack.
3. ClientHijacks waits for the RuneLite Injector to be initialized and adds HijackedClient into the injector.
4. With HijackedClient being created in RuneLites classloader and being started via the Injector, RuneLiteHijack has full access to the same functionality as RuneLite.
5. With the access given, RuneLiteHijack initializes its Plugin Manager and adds the button to the original plugin ui.

Limitations are being restricted to what can be modified at runtime in java.