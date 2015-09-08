package io.jrevolt.launcher;

import io.jrevolt.launcher.mvn.Artifact;
import io.jrevolt.launcher.mvn.Resolver;
import io.jrevolt.launcher.mvn.ResolverContext;
import io.jrevolt.launcher.util.IOHelper;

import java.util.concurrent.ExecutionException;

/**
 * @author <a href="mailto:patrikbeno@gmail.com">Patrik Beno</a>
 */
public class RepositorySupport {

    static public Artifact resolve(String mvnuri) {
        Artifact ma = Artifact.parse(mvnuri);
        ResolverContext ctx = null;
        try {
            ctx = new ResolverContext(ma);
            try {
                ctx.startProgressMonitor();
                Resolver r = new Resolver(ctx, ma);
                return r.getResolvedArtifact();
            } finally {
                ctx.stopProgressMonitor();
            }
        } catch (ExecutionException e) {
            throw new LauncherException(e);
        } catch (InterruptedException e) {
            throw new LauncherException(e);
        } finally {
            IOHelper.close(ctx);
        }
    }

}
