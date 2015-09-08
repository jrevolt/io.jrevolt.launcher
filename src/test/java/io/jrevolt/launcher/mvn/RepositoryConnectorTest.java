package io.jrevolt.launcher.mvn;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import io.jrevolt.launcher.AbstractTest;

import java.io.File;

/**
 * @author <a href="mailto:patrikbeno@gmail.com">Patrik Beno</a>
 */
public class RepositoryConnectorTest extends AbstractTest {

	@Test @Ignore
	public void test() {
		Artifact a = Artifact.parse("org.springframework.boot:spring-boot-loader:1.3.0.BUILD-SNAPSHOT");
        ResolverContext context = new ResolverContext(a);
        try {
            RepositoryConnector c = new RepositoryConnector(Repository.forRepositoryId("default"), context, null);
            c.resolveSnapshotVersion(a);
            File f = c.resolve(a);
            Assert.assertNotNull(f);
        } finally {
            context.close();
        }
    }

}
