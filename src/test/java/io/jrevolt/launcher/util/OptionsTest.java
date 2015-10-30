package io.jrevolt.launcher.util;

import org.springframework.boot.loader.util.SystemPropertyUtils;

import org.junit.Test;

import java.util.Properties;

/**
 * @author <a href="mailto:patrikbeno@gmail.com">Patrik Beno</a>
 */
public class OptionsTest {

	@Test
	public void nested() {
		Properties props = new Properties();
		props.setProperty("jrevolt.launcher.cfgprefix", "jrevolt.launcher");
		props.setProperty("jrevolt.launcher.appname", "JavaApp");
		String resolved = SystemPropertyUtils.resolvePlaceholders(props, "${${jrevolt.launcher.cfgprefix}.appname}");
		System.out.println(resolved);
	}


}
