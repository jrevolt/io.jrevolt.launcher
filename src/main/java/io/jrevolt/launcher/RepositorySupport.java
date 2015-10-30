package io.jrevolt.launcher;

import io.jrevolt.launcher.mvn.Artifact;
import io.jrevolt.launcher.mvn.Resolver;
import io.jrevolt.launcher.mvn.ResolverContext;
import io.jrevolt.launcher.util.Log;
import io.jrevolt.launcher.util.StatusLine;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;

/**
 * @author <a href="mailto:patrikbeno@gmail.com">Patrik Beno</a>
 */
public class RepositorySupport {

	static public Artifact resolve(String mvnuri) {
		Artifact ma = Artifact.parse(mvnuri);
		resolve(ma);
		return ma;
	}

	static public List<Artifact> resolve(Artifact mvnartifact) {
		return resolve(mvnartifact, Collections.<Artifact>emptyList());
	}

	static public List<Artifact> resolve(Artifact mvnartifact, List<Artifact> extras) {

		List<Artifact> artifacts = new LinkedList<>();

		ResolverContext context = new ResolverContext(mvnartifact);
		try {
//            Resolver main = new Resolver(context, mvnartifact);
//            List<Resolver> extras = new LinkedList<>();
//            for (Artifact a : artifacts) { extras.add(new Resolver(context, a)); }

			int count = 0;
			int size = 0;
			int downloaded = 0;
			int errors = 0;
			int warnings = 0;
			int requests = 0;

			try {
				context.startProgressMonitor();

				// resolveAll() fires async. process
				SortedSet<Resolver> resolvers = new TreeSet<>(Resolver.byFullArtifactName);
				if (mvnartifact != null) {
					resolvers.addAll(new Resolver(context, mvnartifact).resolveAll());
				}
				for (Artifact a : extras) {
					resolvers.addAll(new Resolver(context, a).resolveAll());
				}

				Log.debug("Dependencies (alphabetical):");

				for (Resolver r : resolvers) {

					// this may block until resolved artifact is available
					Artifact ma = r.getResolvedArtifact();
					artifacts.add(ma);

					Log.log(toLevel(ma.getStatus()),
							  "- %-12s: %-80s %s",
							  ma.getStatus(), ma,
							  ma.getRepositoryId() != null
									  ? String.format("(%4dKB @%s)", ma.getFile() != null && ma.getFile().exists() ? ma.getFile()
									  .length() / 1024 : 0, ma.getRepositoryId())
									  : ""
					);
					// update some stats
					if (ma.isError()) {
						errors++;
					}
					if (ma.isWarning()) {
						warnings++;
					}
					if (ma.getFile() != null && ma.getFile().exists()) {
						size += ma.getFile().length();
					}
					downloaded += ma.downloaded;
					requests += ma.requests;
				}

				count = resolvers.size();

			} catch (InterruptedException e) {
				throw new LauncherException(e);
			} catch (ExecutionException e) {
				throw new LauncherException(e);
			} finally {
				context.stopProgressMonitor();
			}

			// if enabled, print some final report
			if (!LauncherCfg.quiet.asBoolean()) {
				long elapsed = System.currentTimeMillis() - context.created;
				Log.info(String.format(
						"Summary: %d archives, %d KB total (resolved in %d msec, downloaded %d KB in %d requests, %d KBps). Warnings/Errors: %d/%d.",
						count, size / 1024, elapsed, downloaded / 1024, requests,
						downloaded / 1024 * 1000 / elapsed,
						warnings, errors));
			}

			// if there are errors and fail-on-error property has not been reset, fail
			if (LauncherCfg.execute.asBoolean() && errors > 0 && LauncherCfg.failOnError.asBoolean()) {
				throw new LauncherException(String.format(
						"%d errors resolving dependencies. Use --%s to view details or --%s to ignore these errors and continue",
						errors, LauncherCfg.debug.name(), LauncherCfg.failOnError.name()));
			}

			return artifacts;

		} finally {
			context.close();
			StatusLine.resetLine();
		}
	}

	static private Log.Level toLevel(Artifact.Status status) {
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
