package io.jrevolt.launcher.mvn;

import io.jrevolt.launcher.LauncherCfg;
import io.jrevolt.launcher.url.UrlSupport;

import org.junit.Test;

/**
 * @author <a href="mailto:patrikbeno@gmail.com">Patrik Beno</a>
 */
public class LauncherCfgTest {

   @Test
	public void defaults() {
		System.setProperty(LauncherCfg.quiet.getPropertyName(), "true");
		System.setProperty(LauncherCfg.ansi.getPropertyName(), "false");
		System.setProperty(LauncherCfg.debug.getPropertyName(), "true");
		LauncherCfg.configure();
	}

//	@Test
//	public void


}
