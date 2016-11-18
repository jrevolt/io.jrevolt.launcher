package io.jrevolt.launcher;

import io.jrevolt.launcher.url.UrlSupport;
import io.jrevolt.launcher.util.Log;
import io.jrevolt.launcher.util.StatusLine;

import org.springframework.boot.loader.util.SystemPropertyUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Duration;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.WeakHashMap;

import static java.lang.Math.*;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * JRevolt Launcher bootstrap configuration (loaded from external properties file or system properties).
 * <p/>
 * Each configuration property has its built-in default. Defaults can be either overridden from command line (using
 * {@code -Djrevolt.launcher.enumName=value} property syntax), or from a property file specified in {@code
 * -Djrevolt.launcher.defaults} property (which itself defaults to {@code $ user.home}/.jrevolt/defaults.properties}).
 *
 * @author Patrik Beno
 * @see io.jrevolt.launcher.mvn.Launcher
 */
public enum LauncherCfg {

	// naming convention broken intentionally: enum name() is also property name used in
	// -Djrevolt.launcher.{name}={value} or --jrevolt.launcher.{name}={value}

	/**
	 * Particular application name, typically base name of the script
	 */
	appname,

	/**
	 * Particular application home directory; Usually not current directory, not user directory, not a system folder.
	 * Might be an application installation folder or an application data root folder. This one is first because it is
	 * referenced by {@code defaults}. Defaults to ${java.home}
	 *
	 * @see #defaults
	 */
	apphome,

	mode, // (user|system)

	/**
	 * - {@code ${user.home}/.jrevolt}
	 * - {@code /etc/jrevolt}
	 */
	conf,

	/**
	 * Launcher cache directory. Defaults to
	 * - {@code ${user.home}/.jrevolt/cache}
	 * - {@code /var/cache/jrevolt}
	 */
	cache,

	/**
	 * - {@code ${user.home}/.jrevolt/logs}
	 * - {@code /var/log/jrevolt}
	 */
	logs,

	/**
	 * Maven repositories to use (comma separated list). Defaults: github,central (repository URLs are automatically
	 * exported to system properties if undefined, hence providing fallback in case of missing configuration)
	 */
	repositories,


	/**
	 * Resolve main artifact and execute it as regular jar file (no dependencies resolution)
	 */
	delegate,

	/**
	 * If set, no artifacts are downloaded or updated from remote repository, and any artifact may end up unresolved
	 * (NotFound). Launcher rejects incomplete class paths and aborts, unless overriden using {@link #failOnError}.
	 */
	offline,

	/**
	 * If set, downloads are verified using SHA1 signature provided by remote repository: signature mismatch is
	 * considered an error, and the artifact is marked {@code Invalid}. Enabled by default.
	 *
	 * @see io.jrevolt.launcher.mvn.Artifact.Status#Invalid
	 */
	verify,

	/**
	 * If set, cache is ignored and all artifacts are re-downloaded.
	 */
	ignoreCache,

	/**
	 * If set, errors like missing artifacts and checksum mismatches cause launcher to reject application execution.
	 * Enabled by default.
	 */
	failOnError,

	/**
	 * Once downloaded, release is considered immutable and not subject to change/update (default). Override this flag to
	 * make launcher check for remote updates of releases (this usually leads to slower application startup and {@code
	 * NotModified} statuses on release artifacts.
	 */
	updateReleases,

	/**
	 * Snapshots are considered volatile artifacts and are checked for updates (true) unless this check is disabled
	 * (false). Disable this to speed up application startup if you do not require latest snapshot updates, or you are
	 * aware there are no updates available.
	 */
	updateSnapshots,

	/**
	 * If reset (false), JRevolt Launcher checks for and downloads updates but won't actually execute the application.
	 * Default: {@code true}
	 */
	execute,

	/**
	 * Shortcut configuration property designed to force global update without the need to separately set individual
	 * fine-graned properties. Default is {@code false}.
	 *
	 * @see #updateSnapshots
	 * @see #updateReleases
	 * @see #updateInterval
	 * @see #offline
	 * @see #ignoreCache
	 */
	update,

	/**
	 * Specifies interval (in minutes) within which result of the latest successful update operation remains valid. This
	 * means that remote repository is checked only once in a specified interval. In other words, if you run the
	 * application twice, it will check for updates only once (first run). Second run will behave as if the offline mode
	 * was enabled (no remote repository contact whatsoever, unless the dependency is missing). Next update check will be
	 * allowed only after the specified update interval elapsed. Default: 1 day (1440 minutes)
	 */
	updateInterval,

	skipDownload,

	resolvers,

	downloaders,

	retries,

	/// logging & output

	loglevel,

	/**
	 * Enable configuration and connector logging
	 */
	debug,

	/**
	 * Supresses status line and all other messages except errors
	 */
	quiet,

	/**
	 * Enables ANSI coloring sequences in logging; When false, status line is disabled (as it depends on availability
	 * of ANSI sequences)
	 */
	ansi,

	;

	static private enum Mode { user, system }

	static private class MyProperties extends Properties {
		public MyProperties(Properties defaults) {
			super(defaults);
		}
		void setDefaults(Properties props) {
			this.defaults = props;
		}
	}

	static private final String PROPERTY_NAME_PREFIX = "jrevolt.launcher.";

	static private final Properties DEFAULTS, DEFAULTS_USER, DEFAULTS_SYSTEM;

	/**
	 * per context class loader configuration
	 */
	static private final Map<ClassLoader, Properties> CONTEXT = new WeakHashMap<>();


	static {

		UrlSupport.init();

		// pre-boot config
		ClassLoader cl = ClassLoader.getSystemClassLoader();
		CONTEXT.put(cl, new Properties());

		//noinspection UnusedLabel
		loadDefaults: {
			String path = LauncherCfg.class.getName().replace('.', '/');
			DEFAULTS = load(format("classpath:%s.properties", path), null);
			DEFAULTS_USER = load(format("classpath:%s-user.properties", path), DEFAULTS);
			DEFAULTS_SYSTEM = load(format("classpath:%s-system.properties", path), DEFAULTS);
			CONTEXT.put(cl, new Properties(DEFAULTS_USER));
		}
	}

	static public void init() {} // just to make sure static{} is called

	static private Properties properties() {
		return properties(Thread.currentThread().getContextClassLoader());
	}

	static private Properties properties(ClassLoader loader) {
//		loader = Optional.ofNullable(loader)
//				.orElseGet(()->Optional.ofNullable(Thread.currentThread().getContextClassLoader())
//						.orElseGet(ClassLoader::getSystemClassLoader));
		if (loader == null) {
			loader = Thread.currentThread().getContextClassLoader();
		}
		if (loader == null) {
			loader = ClassLoader.getSystemClassLoader();
		}
		Properties props = CONTEXT.get(loader);
		if (props != null) {
			return props;
		}
		if (loader.getParent() != null) {
			return properties(loader.getParent());
		}
		return System.getProperties();
	}

	static public Set<String> names() {
		Set<String> names = new HashSet<>();
		for (LauncherCfg v : values()) {
			names.add(v.name());
		}
		return names;
	}

	///

	public String get() {
		return get(getPropertyName(), true);
	}

	static String get(String pname, boolean resolve) {
		return get(properties(), pname, resolve);
	}

	static String get(Properties props, String pname, boolean resolve) {
		String value = SystemPropertyUtils.getProperty(pname, props.getProperty(pname));
		String resolved = resolve ? SystemPropertyUtils.resolvePlaceholders(props, value) : null;
		return resolve ? resolved : value;
	}

	public void set(String value) {
		if (value != null) {
			properties().setProperty(getPropertyName(), value);
		} else {
			// todo will this work as expected in properties cascade?
			properties().remove(getPropertyName());
		}
	}

	public void set(Object object) {
		set(Objects.toString(object));
	}

	/**
	 * Return recognized system property name mapped to this enum constant; all supported properties use common {@code
	 * jrevolt.launcher.*} prefix
	 */
	public String getPropertyName() {
		return PROPERTY_NAME_PREFIX + name();
	}

	/**
	 * Returns {@code true} if the property value is defined (i.e. not null)
	 */
	public boolean isDefined() {
		return System.getProperty(getPropertyName()) != null;
	}

	/**
	 * Returns value as {@code String}
	 *
	 * @return
	 */
	public String raw() {
		return get(getPropertyName(), false);
	}

	public String asString() {
		return resolvePlaceholders(get());
	}

	public List<String> asList() {
		return Arrays.asList(asString().split(","));
	}

	public boolean asBoolean() {
		return Boolean.parseBoolean(asString());
	}

	public int asInt() {
		return Integer.parseInt(asString());
	}

	public long asLong() {
		return Long.parseLong(asString());
	}

	public URL asURL(boolean directory) {
		return url(asString(), directory);
	}

	public URI asURI(boolean directory) {
		try {
			return url(asString(), directory).toURI();
		} catch (URISyntaxException e) {
			throw new LauncherException(e);
		}
	}

	public File asFile() {
		return new File(asString());
	}

	public String asFileUri() {
		return "file:"+get();
	}

	public Duration asDuration() {
		return Duration.parse(asString());
	}

	public void export() {
		System.setProperty(getPropertyName(), properties().getProperty(getPropertyName()));
	}

	///

	static private void validate() {
		fix(resolvers, 1, 10);
		fix(downloaders, 1, 3);
		fixPeriod(updateInterval, Duration.ZERO, Duration.ofDays(365));
	}

	static public void configure() {
		configure(null);
	}

	static public void configure(ClassLoader contextClassLoader) {
		if (contextClassLoader == null) {
			contextClassLoader = ClassLoader.getSystemClassLoader();
		}

		Log.info(Version.version().getVersionString());

		MyProperties conf = load(LauncherCfg.conf.asFileUri(), DEFAULTS);
		CONTEXT.put(contextClassLoader, conf);
		switch (Mode.valueOf(LauncherCfg.mode.get())) {
			case user: conf.setDefaults(DEFAULTS_USER); break;
			case system: conf.setDefaults(DEFAULTS_SYSTEM); break;
		}
		exportSystemProperties(properties());

		validate();
	}


	static public void report(boolean resolved) {
		if (isDebugEnabled()) {
			String header = format("JRevolt Launcher configuration:");
			for (LauncherCfg v : values()) {
				if (header != null) {
					Log.debug(header);
					header = null;
				}
				Log.debug("- %-32s : %s", v.getPropertyName(), resolved ? v.asString() : v.raw());
			}
		}
	}

	static private void exportSystemProperties(Properties props) {
		Log.debug("Overriding system properties");
		Properties sysprops = System.getProperties();
		props.stringPropertyNames().stream()
				.filter(name->!name.startsWith(PROPERTY_NAME_PREFIX))
				.filter(name->!sysprops.containsKey(name))
				.forEach(name->{
					String value = props.getProperty(name);
					Log.debug(" - %s = %s", name, value);
					sysprops.setProperty(name, value);
				});
	}

	static public boolean isDebugEnabled() {
		return Log.isLevelEnabled(Log.Level.DBG) || debug.asBoolean();
	}

	static private String list(String... properties) {
		StringBuilder sb = new StringBuilder();
		for (String s : properties) {
			if (sb.length() > 0) {
				sb.append(",");
			}
			sb.append(s);
		}
		return sb.toString();
	}

	static private Properties properties(String... uris) {
		return properties(properties(), uris);
	}

	static private Properties properties(Properties defaults, String... uris) {
		Properties props = defaults;
		for (int i = uris.length - 1; i >= 0; i--) {
			String uri = uris[i];
			if (uri.trim().isEmpty()) { continue; }

			URL url = url(uri, false);
			if (url == null) {
				continue;
			}
			StatusLine.push("Loading %s", url);
			try {
				props = load(url, props);
				Log.debug("Loaded %s", url);
			} finally {
				StatusLine.pop();
			}
		}
		return (props != null) ? props : new Properties();
	}

	static private MyProperties load(String url, Properties defaults) {
		return load(url(url), defaults);

	}

	static private MyProperties load(URL url, Properties defaults) {
		try (InputStream in = url.openStream()) {
			MyProperties props = new MyProperties(defaults);
			props.load(in);
			return props;
		} catch (FileNotFoundException ignore) {
			return new MyProperties(defaults);
		} catch (IOException e) {
			throw new LauncherException(e);
		}
	}

	static private URL url(String surl) {
		return url(surl, false);
	}

	static private URL url(String surl, boolean isDirectory) {
		try {
			surl = surl.trim();
			if (isDirectory && !surl.endsWith("/")) {
				surl += "/";
			}
			String resolved = resolvePlaceholders(surl).replace('\\', '/');
			return new URL(resolved);
		} catch (MalformedURLException e) {
			throw new LauncherException(e, "Invalid URL: " + surl);
		}
	}

	private static void fix(LauncherCfg cfg, int min, int max) {
		int actual = cfg.asInt();
		int adjusted = min(max(actual, min), max);
		if (actual != adjusted) {
			cfg.set(Integer.toString(adjusted));
			Log.warn("Adjusting invalid or out of range configuration value: --%s=%s (was: %s)", cfg.name(), adjusted,
						actual);
		}
	}

	private static void fixPeriod(LauncherCfg cfg, Duration min, Duration max) {
		long actual = cfg.asDuration().getSeconds();
		long adjusted = min(max(actual, min.getSeconds()), max.getSeconds());
		if (actual != adjusted) {
			cfg.set(Duration.ofSeconds(adjusted).toString());
			Log.warn("Adjusting invalid or out of range configuration value: --%s=%s (was: %s)", cfg.name(), adjusted,
						actual);
		}
	}

	static protected void exportRepositoryDefaults() {
		InputStream in = null;
		try {
			Properties system = System.getProperties();
			in = Main.class.getResourceAsStream("repo-defaults.properties");
			Properties defaults = new Properties();
			defaults.load(in);
			Enumeration<?> names = defaults.propertyNames();
			while (names.hasMoreElements()) {
				String name = (String) names.nextElement();
				if (!system.containsKey(name)) {
					system.setProperty(name, defaults.getProperty(name));
				}
			}
		} catch (IOException ignore) {
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException ignore) {
				}
			}
		}
	}

	static private String resolvePlaceholders(String text) {
		return resolvePlaceholders(properties(), text);
	}

	static private String resolvePlaceholders(Properties props, String text) {
		return SystemPropertyUtils.resolvePlaceholders(props, text);
	}

	///

	@Override
	public String toString() {
		return name() + "=" + raw();
	}

	static public boolean isConsole() {
		return System.console() != null;
	}

	static public boolean isCygwin() {
		return System.console() == null && System.getenv("CYGWIN") != null;
	}

	static public boolean isIntelliJ() {
		try {
			return Class.forName("com.intellij.rt.execution.application.AppMain") != null;
		} catch (ClassNotFoundException ignore) {
			return false;
		}
	}
}
