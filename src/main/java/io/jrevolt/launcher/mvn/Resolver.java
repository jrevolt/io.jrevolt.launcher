package io.jrevolt.launcher.mvn;

import io.jrevolt.launcher.LauncherCfg;
import io.jrevolt.launcher.LauncherException;
import io.jrevolt.launcher.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import static io.jrevolt.launcher.util.IOHelper.close;

/**
 * @author <a href="mailto:patrikbeno@gmail.com">Patrik Beno</a>
 */
public class Resolver {

	/**
	 * Name of the manifest attribute containing the comma-delimited list Maven URIs (runtime dependencies, provided
	 * build-time)
	 */
	static public final String MF_DEPENDENCIES = "Maven-Dependencies";

	static public Comparator<Resolver> byFullArtifactName = new Comparator<Resolver>() {
		@Override
		public int compare(Resolver o1, Resolver o2) {
			return o1.getArtifact().asString().compareTo(o2.getArtifact().asString());
		}
	};

	ResolverContext context;

	Artifact artifact;

	String mainClass;

	List<Artifact> dependencies;

	Future<Resolver> fresolve;
	Future<Resolver> fdownload;

	List<Resolver> fdependencies;


	public Resolver(ResolverContext context, Artifact artifact) {
		this.context = context;
		this.artifact = artifact;
	}

	public Artifact getArtifact() {
		return artifact;
	}

	public Artifact getResolvedArtifact() throws ExecutionException, InterruptedException {
		return download().get().getArtifact();
	}

	synchronized Future<Resolver> resolve() {
		if (fresolve != null) {
			return fresolve;
		}
		return fresolve = context.resolvers.submit(new Callable<Resolver>() {
			@Override
			public Resolver call() throws Exception {
				connector().resolve(artifact);
				return Resolver.this;
			}
		});
	}

	synchronized Future<Resolver> download() {
		if (fdownload != null) {
			return fdownload;
		}
		return fdownload = context.downloaders.submit(new Callable<Resolver>() {
			@Override
			public Resolver call() throws Exception {
				resolve().get();
				connector().download(artifact);
				return Resolver.this;
			}
		});
	}

	synchronized List<Resolver> dependencies() {
		if (fdependencies != null) {
			return fdependencies;
		}
		resolveMainClassAndDependencies();
		List<Resolver> resolvers = new LinkedList<Resolver>();
		for (Artifact ma : dependencies) {
			Resolver r = new Resolver(context, ma);
			resolvers.add(r);
			r.resolve();
		}
		return resolvers;
	}

	public SortedSet<Resolver> resolveAll() {
		SortedSet<Resolver> all = new TreeSet<Resolver>(byFullArtifactName);
		resolveMainClassAndDependencies();
		all.add(this);
		all.addAll(dependencies());

		// and fire the resolver tasks
		for (Resolver r : all) {
			r.download();
		}

		return all;
	}

	private void resolveMainClassAndDependencies() {
		if (this.dependencies != null) {
			return;
		}
		JarFile jar = null;
		try {
			context.main = getArtifact();
			context.artifacts.add(getArtifact());

			File f = download().get().getArtifact().getFile();

			if (f == null || !f.exists()) {
				throw new LauncherException(
						getArtifact().getError(),
						"Cannot resolve %s (status: %s)", getArtifact(), getArtifact().getStatus());
			}

			jar = new JarFile(f);

			this.mainClass = getManifestAttribute(jar, "Main-Class");
			this.dependencies = getArtifacts(jar);

			for (Artifact ma : dependencies) {
				ma.setStatus(Artifact.Status.Resolving);
				context.artifacts.add(ma);
			}

		} catch (LauncherException e) {
			throw e;
		} catch (Exception e) {
			throw new LauncherException(e);
		} finally {
			close(jar);
		}
	}

	private String getManifestAttribute(JarFile jar, String name) {
		try {
			Manifest mf = jar.getManifest();
			if (mf == null) {
				return null;
			}

			return mf.getMainAttributes().getValue(name);
		} catch (IOException e) {
			throw new LauncherException(e);
		}
	}

	/**
	 * Load list of Maven dependencies from manifest of a specified archive
	 */
	private List<Artifact> getArtifacts(JarFile archive) {

		if (LauncherCfg.delegate.asBoolean()) {
			Log.debug("Ignoring specified dependencies (--delegate=true)");
			return Collections.emptyList();
		}

		String mfdeps = getManifestAttribute(archive, MF_DEPENDENCIES);
		String[] manifestDependencies = mfdeps != null ? mfdeps.split("[,\\p{Space}]") : new String[0];
		List<Artifact> artifacts = toArtifacts(manifestDependencies);
		return artifacts;
	}

	// parses Maven URIs and converts them into list of Maven artifacts
	private List<Artifact> toArtifacts(String[] strings) {
		if (strings == null) {
			return Collections.emptyList();
		}
		List<Artifact> result = new ArrayList<Artifact>();
		for (String s : strings) {
			if (s == null || s.trim().isEmpty()) {
				continue;
			}
			result.add(Artifact.parse(s));
		}
		return result;
	}

	RepositoryConnector connector() {
		return context.connector;
	}

}
