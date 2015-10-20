/*
 * Copyright 2012-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
import java.util.function.Supplier;

import static java.lang.Math.*;
import static java.util.Objects.requireNonNull;
import static org.springframework.boot.loader.util.SystemPropertyUtils.resolvePlaceholders;

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
	 * Particular application home directory; Usually not current directory, not user directory, not a system folder.
	 * Might be an application installation folder or an application data root folder. This one is first because it is
	 * referenced by {@code defaults}. Defaults to ${java.home}
	 *
	 * @see #defaults
	 */
	apphome,

	/**
	 * Particular application name, typically base name of the script
	 */
	appname,

	/**
	 * Properties containing JRevolt Launcher user-specific configuration defaults; the file, if exists, is loaded during
	 * initialization of this property and its values are propagated into system properties (if not yet defined using
	 * {@code -Dname=value} on JVM command line)
	 * <p/>
	 * Keep this first to force loading of user-defined defaults before resolving other properties
	 */
	defaults,

	/**
	 * Maven repositories to use (comma separated list). Defaults: github,central (repository URLs are automatically
	 * exported to system properties if undefined, hence providing fallback in case of missing configuration)
	 */
	repositories,

	/**
	 * Launcher cache directory. Defaults to {@code ${user.home}/.jrevolt/cache}
	 */
	cache,

	/**
	 * Resolve main artifact and execute it as regular jar file (no dependencies resolution)
	 */
	delegate,

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

	;

	static private final Properties DEFAULTS;

	/**
	 * per context class loader configuration
	 */
	static private final Map<ClassLoader, Properties> LOADED;

	/**
	 * per context class loader configuration
	 */
	static private final Map<ClassLoader, Properties> CONTEXT;

	static private final Supplier<String> UNCONFIGURED;

	static {
		// todo review this
		UrlSupport.init();
		LauncherCfg.exportRepositoryDefaults();
		LOADED = new WeakHashMap<>();
		LOADED.put(ClassLoader.getSystemClassLoader(), new Properties());
		CONTEXT = new WeakHashMap<>();
		CONTEXT.put(ClassLoader.getSystemClassLoader(), new Properties());
		UNCONFIGURED = () -> String.format(
				"JRevolt Launcher unconfigured! Context: %s", ClassLoader.getSystemClassLoader());
		DEFAULTS = properties(null, String.format(
				"classpath:%s.properties", LauncherCfg.class.getName().replace('.','/')));
		configure();
	}

	static private Properties properties(Map<ClassLoader, Properties> ctx) {
		return requireNonNull(properties(ctx, null), UNCONFIGURED);
	}

	static private Properties properties(Map<ClassLoader, Properties> ctx, ClassLoader loader) {
		if (loader == null) {
			loader = ClassLoader.getSystemClassLoader();
		}
		Properties props = ctx.get(loader);
		if (props != null) {
			return props;
		}
		if (loader.getParent() != null) {
			return properties(ctx, loader.getParent());
		}
		return null;
	}

	static public Set<String> names() {
		Set<String> names = new HashSet<String>();
		for (LauncherCfg v : values()) {
			names.add(v.name());
		}
		return names;
	}

	// /

	public String get() {
		String pname = getPropertyName();
		return properties(CONTEXT).getProperty(pname, properties(LOADED).getProperty(pname));
	}

	public void set(String value) {
		if (value != null) {
			properties(CONTEXT).setProperty(getPropertyName(), value);
		} else {
			// todo will this work as expected in properties cascade?
			properties(CONTEXT).remove(getPropertyName());
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
		return "jrevolt.launcher." + name();
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
		return get();
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

	public Duration asDuration() {
		return Duration.parse(asString());
	}


	// /

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

		Properties loaded = properties(DEFAULTS, Objects.toString(LauncherCfg.defaults.get(),"").split(","));
		Properties context = new Properties(System.getProperties());

		LOADED.put(contextClassLoader, loaded);
		CONTEXT.put(contextClassLoader, context);

		validate();
	}

	static public void report() {
		Log.info(Version.version().getVersionString());

		if (isDebugEnabled()) {
			String header = "JRevolt Launcher configuration:";
			for (LauncherCfg v : values()) {
				if (header != null) {
					Log.debug(header);
					header = null;
				}
				Log.debug("- %-32s : %s", v.getPropertyName(), SystemPropertyUtils.resolvePlaceholders(v.asString()));
			}
		}
	}

	static public void export() {
		export(System.getProperties());
	}

	static public Properties export(Properties props) {
		for (LauncherCfg v : LauncherCfg.values()) {
			if (v.get() == null) {
				continue;
			}
			props.setProperty(v.getPropertyName(), v.asString());
		}
		return props;
	}

	static public boolean isDebugEnabled() {
		return (debug != null && debug.asBoolean()) || (debug == null && Boolean.getBoolean("jrevolt.launcher.debug"));
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
				InputStream in = url.openStream();
				Properties loaded = new Properties(props);
				loaded.load(in);
				props = loaded; // do assignment only after successful load
				Log.debug("Loaded %s", url);
			} catch (FileNotFoundException ignore) {
				// diagnostics: in debug mode, the "> Loaded" string is not printed...
				// this should be enough for this case
			} catch (IOException e) {
				if (isDebugEnabled()) {
					e.printStackTrace();
				}
			} finally {
				StatusLine.pop();
			}
		}
		return (props != null) ? props : new Properties();
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
