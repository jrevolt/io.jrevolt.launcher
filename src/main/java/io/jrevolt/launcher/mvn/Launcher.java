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
package io.jrevolt.launcher.mvn;

import io.jrevolt.launcher.LauncherCfg;
import io.jrevolt.launcher.LauncherException;
import io.jrevolt.launcher.RepositorySupport;
import io.jrevolt.launcher.url.UrlSupport;
import io.jrevolt.launcher.util.IOHelper;
import io.jrevolt.launcher.util.Log;

import org.springframework.boot.loader.LaunchedURLClassLoader;
import org.springframework.boot.loader.archive.Archive;
import org.springframework.boot.loader.archive.JarFileArchive;
import org.springframework.boot.loader.jar.JarFile;

import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Specialized implementation of the {@code Launcher} that intelligently downloads dependencies from configured Maven
 * repository.
 *
 * @author Patrik Beno
 * @see org.springframework.boot.loader.ExecutableArchiveLauncher
 */
public class Launcher {

	static {
		UrlSupport.init();
	}

	private Artifact artifact;

	private String mainClass;

	public Launcher(Artifact artifact) {
		this.artifact = artifact;
		this.mainClass = artifact.getMainClass();
	}

	protected String getMainClass() throws Exception {
		if (artifact.getMainClass() == null) {
			mainClass = artifact.getArchive().getManifest().getMainAttributes().getValue("Main-Class");
		}
		return mainClass;
	}

	protected List<Archive> getClassPathArchives() {
		try {
			return getClassPathArchives(artifact);
		} catch (LauncherException e) {
			throw e;
		} catch (Exception e) {
			throw new LauncherException(e);
		}
	}


	protected ClassLoader createClassLoader(List<Archive> archives) throws Exception {
		List<URL> urls = new ArrayList<URL>(archives.size());
		for (Archive archive : archives) {
			urls.add(archive.getUrl());
		}
		return createClassLoader(urls.toArray(new URL[urls.size()]));
	}

	protected ClassLoader createClassLoader(URL[] urls) throws Exception {
		return new URLClassLoader(urls, null);
	}

	protected void launch(String[] args) {
		try {
			JarFile.registerUrlProtocolHandler();
			ClassLoader classLoader = createClassLoader(getClassPathArchives());
			String main = getMainClass();
			if (main == null && args.length > 0) {
				main = args[0];
				args = Arrays.copyOfRange(args, 1, args.length);
			}
			launch(args, main, classLoader);
		} catch (LauncherException e) {
			throw e;
		} catch (Exception e) {
			throw new LauncherException(e);
		}
	}

	/// API for embedded use

	/**
	 * Resolves given artifact and all its dependencies, and configures a class loader linked to a specified parent.
	 * Caller is responsible for a proper use of the resulting class loader and all the classes loaded.
	 *
	 * @param artifact
	 * @param parent
	 * @return
	 */
	public ClassLoader resolve(Artifact artifact, List<Artifact> ext, ClassLoader parent) {
		try {
			List<Archive> archives = getClassPathArchives(artifact);
			List<URL> urls = new ArrayList<URL>(archives.size());
			for (Archive archive : archives) {
				urls.add(archive.getUrl());
				IOHelper.close(archive);
			}
			ClassLoader cl = new LaunchedURLClassLoader(urls.toArray(new URL[urls.size()]), parent);
			return cl;
		} catch (Exception e) {
			throw new LauncherException(e,
												 "Cannot resolve artifact or its dependencies: " + artifact.asString());
		}
	}

	public ClassLoader resolve(Artifact artifact, ClassLoader parent) {
		return resolve(artifact, null, parent);
	}

	///

	protected List<Archive> getClassPathArchives(Artifact mvnartifact) throws Exception {
		List<Artifact> artifacts = RepositorySupport.resolve(mvnartifact);
		List<Archive> archives = new LinkedList<Archive>();
		for (Artifact a : artifacts) {
			archives.add(new JarFileArchive(a.getFile()));
		}
		return archives;
	}

	public void launch(Queue<String> args) throws Exception {
		launch(args.toArray(new String[args.size()]));
	}

	protected void launch(final String[] args, final String mainClass, final ClassLoader classLoader) throws Exception {
		if (!LauncherCfg.execute.asBoolean()) {
			Log.warn("Application updated. Execution skipped (%s=false)", LauncherCfg.execute.getPropertyName());
			return;
		}
		if (LauncherCfg.debug.asBoolean()) {
			Log.debug("## Application Arguments:");
			for (String s : args) {
				Log.debug(s);
			}
			Log.debug("##");
		}
		//LauncherCfg.export(); // TODO review this

		if (mainClass == null) {
			throw new LauncherException("Missing Main-Class in manifest");
		}

		Runnable runner = new Runnable() {
			@Override
			public void run() {
				try {
					Class.forName(mainClass, false, classLoader)
							.getMethod("main", String[].class)
							.invoke(null, (Object) args);
				} catch (InvocationTargetException | IllegalAccessException | ClassNotFoundException | NoSuchMethodException e) {
					throw new LauncherException(e);
				}
			}
		};

		Thread runnerThread = new Thread(runner);
		runnerThread.setContextClassLoader(classLoader);
		runnerThread.setName("main");
		runnerThread.start();
	}


	///

	private Log.Level toLevel(Artifact.Status status) {
		switch (status) {
			case Invalid:
			case NotFound:
			case Downloadable:
				return Log.Level.WRN;
			case Downloaded:
			case Updated:
				return LauncherCfg.isDebugEnabled() ? Log.Level.DBG : Log.Level.INF;
			default:
				return Log.Level.DBG;
		}
	}


}
