package io.jrevolt.launcher.mvn;

import io.jrevolt.launcher.AbstractTest;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;


/**
 * @author <a href="mailto:patrikbeno@gmail.com">Patrik Beno</a>
 */
public class LauncherTest extends AbstractTest {

	@Test
	@Ignore
	public void test() {
		Artifact artifact = Artifact.parse("org.springframework.boot:spring-boot-loader:1.3.0.BUILD-SNAPSHOT");
		Launcher launcher = new Launcher(artifact);
		ClassLoader cl = launcher.resolve(artifact, Thread.currentThread().getContextClassLoader());
		Assert.assertNotNull(cl);

		// warning: sadly, class loader keeps using all the artifacts, and cannot be explicitly released (gc'ed)
		// the artifact spring-boot-loader artifact cannot be fully used for further testing (read-only mode, no updates)
	}
}
