package io.jrevolt.launcher;

import io.jrevolt.launcher.mvn.Artifact;
import io.jrevolt.launcher.mvn.Launcher;
import io.jrevolt.launcher.mvn.Repository;
import io.jrevolt.launcher.util.CommandLine;
import io.jrevolt.launcher.util.IOHelper;
import io.jrevolt.launcher.vault.Vault;

import java.io.Console;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;

/**
 * @author <a href="mailto:patrikbeno@gmail.com">Patrik Beno</a>
 */
abstract class Command {

	abstract void run(CommandLine cmdline);

	static class Launch extends Command {
		@Override
		void run(CommandLine cmdline) {
			String artifact = cmdline.remainder().poll();
			if (artifact == null) {
				throw new LauncherException("Expected artifactId");
			}

			Artifact a = Artifact.tryparse(artifact);

			if (a == null) {
				throw new LauncherException(null, "Invalid artifact URI or no such file: %s", artifact);
			}

			try {
				new Launcher(a).launch(cmdline.remainder());
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	static class Config extends Command {
		@Override
		public void run(CommandLine cmdline) {
			LauncherCfg.init();
			LauncherCfg.configure();
			LauncherCfg.loglevel.set("DBG");
			LauncherCfg.quiet.set("false");
			LauncherCfg.quiet.export();
			LauncherCfg.report(true);
			LauncherCfg.report(false);
		}
	}

	static class Version extends Command {
		@Override
		public void run(CommandLine cmdline) {
			LauncherCfg.init();
			LauncherCfg.quiet.set("true");
			LauncherCfg.debug.set("false");
			System.out.println(io.jrevolt.launcher.Version.version().getVersionString());
		}
	}

	static class Encrypt extends Command {
		@Override
		public void run(CommandLine cmdline) {
			String key = option(cmdline, "key", true, "Key");
			String value = option(cmdline, "value", false, "Value");

			while (value == null) {
				Console console = System.console();

				if (console == null) {
					System.err.println("No console. Use --value.");
					return;
				}

				char[] chars = console.readPassword("Enter value: ");
				char[] repeat = console.readPassword("Repeat: ");
				if (Arrays.equals(chars, repeat)) {
					value = new String(chars);
				} else {
					System.out.println("Value mismatch! Try again!");
				}
			}

			String encrypted = Vault.instance().encrypt(value);
			PrintWriter out = null;
			try {
				System.out.println("### Raw encrypted value:");
				System.out.println(encrypted);

				System.out.println("### Encoded in properties format:");
				Properties props = new Properties();
				props.setProperty(key, encrypted);
				out = new PrintWriter(System.out);
				props.store(out, null);
			} catch (IOException e) {
				throw new RuntimeException(e);
			} finally {
				IOHelper.close(out);
			}
		}
	}

	static class Decrypt extends Command {
		@Override
		void run(CommandLine cmdline) {
			String key = option(cmdline, "key", true, "Key");
			String value = Vault.instance().getProperty(key);
			System.out.println(value);
		}
	}

	static class Repository extends Command {
		@Override
		void run(CommandLine cmdline) {
			if (cmdline.properties().isEmpty()) {
				System.out.println("repository --id=<alias> --url=<URL> --username=<username>");
				return;
			}

			String id = option(cmdline, "id", true, "Repository alias");
			String url = option(cmdline, "url", true, "Repository URL");
			String username = option(cmdline, "username", false, "Auth: user name");
			String password = option(cmdline, "password", false, "Auth: password");

			while (username != null && password == null) {
				Console console = System.console();

				if (console == null) {
					System.err.println("No console. Use --password");
					return;
				}

				char[] chars = console.readPassword("Enter password: ");
				char[] repeat = console.readPassword("Repeat: ");
				if (Arrays.equals(chars, repeat)) {
					password = new String(chars);
				} else {
					System.out.println("Value mismatch! Try again!");
				}
			}

			password = Vault.instance().encrypt(password);

			Formatter f = new Formatter(System.out);
			f.format("# jrevolt.properties%n");
			f.format(io.jrevolt.launcher.mvn.Repository.P_URL, id).format("=%s%n", url);
			if (username != null) {
				f.format(io.jrevolt.launcher.mvn.Repository.P_USERNAME, id).format("=%s%n", username);
			}
			if (password != null) {
				f.format(io.jrevolt.launcher.mvn.Repository.P_PASSWORD, id).format("=%s%n", password);
			}
			f.format("# EOF%n");
		}
	}

	static class Help extends Command {
		@Override
		void run(CommandLine cmdline) {
			readme();
			if (cmdline.properties().contains("help")) {
				System.exit(-1);
			}
		}

		void readme() {
			InputStream in = null;
			try {
				in = getClass().getResourceAsStream("README.txt");
				Scanner scanner = new Scanner(in);
				scanner.useDelimiter("\\Z");
				String s = scanner.next();
				System.out.println(s);
			} finally {
				if (in != null) {
					try {
						in.close();
					} catch (IOException ignore) {
					}
				}
			}
		}
	}

	protected String option(CommandLine cmdline, String property, boolean required, String hint) {
		String value = cmdline.properties().getProperty(property);
		if (value == null && required) {
			throw new LauncherException(String.format("Required: --%s=<%s>", property, hint));
		}
		return value;
	}


	@SuppressWarnings("unchecked")
	static <T extends Command> Map<String, Class<T>> getCommands() {
		Map<String, Class<T>> cmds = new HashMap<>();
		Arrays.stream(Command.class.getDeclaredClasses())
				.filter(c->Command.class.isAssignableFrom(c) && !Modifier.isAbstract(c.getModifiers()))
				.forEach(c-> cmds.put(c.getSimpleName().toLowerCase(), (Class<T>) c));
		return cmds;
	}

	static <T extends Command> T create(Class<T> type) {
		try {
			return type.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			throw new AssertionError(e);
		}
	}

}
