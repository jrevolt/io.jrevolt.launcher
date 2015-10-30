package io.jrevolt.launcher.util;

import io.jrevolt.launcher.LauncherCfg;

import java.io.PrintStream;

import static io.jrevolt.launcher.util.Log.Level.*;

/**
 * @author <a href="mailto:patrikbeno@gmail.com">Patrik Beno</a>
 */
public class Log {

	static public enum Level {
		DBG(""), INF("\033[1m"), WRN("\033[33m"), ERR("\033[;31m");

		String ansi;

		Level(String ansi) {
			this.ansi = ansi;
		}
	}

	static private final long STARTED = System.currentTimeMillis();

	static public boolean isDebug() {
		return LauncherCfg.isDebugEnabled();
	}

	static public synchronized void log(Level level, String message, Object... args) {
		switch (level) {
			case DBG:
				if (!isDebug()) break;
			default:
				log(out(), level, message, args);
		}
	}

	static public synchronized void debug(String message, Object... args) {
		if (isDebug()) {
			log(out(), DBG, message, args);
		}
	}

	static public synchronized void info(String message, Object... args) {
		if (LauncherCfg.quiet.asBoolean()) {
			return;
		}
		log(out(), INF, message, args);
	}

	static public synchronized void warn(String message, Object... args) {
		if (LauncherCfg.quiet.asBoolean()) {
			return;
		}
		log(out(), WRN, message, args);
	}

	static public synchronized void error(Throwable thrown, String message, Object... args) {
		out().flush();
		err().flush();
		log(err(), ERR, message, args);
		int level = 0;
		for (Throwable t = thrown; t != null; t = t.getCause(), level++) {
			log(err(), ERR, (level == 0) ? "%s" : "Caused by: %s", t);
		}
		if (thrown != null && LauncherCfg.debug.asBoolean()) {
			thrown.printStackTrace(err());
		}
	}

	static private synchronized void log(final PrintStream out, Level level, String message, Object... args) {
		boolean ansi = LauncherCfg.ansi.asBoolean();
		StatusLine.resetLine();
		out.print(ansi ? level.ansi : "");
//        out.printf("%4d [%s] ", System.currentTimeMillis() - STARTED, level);
		out.printf("[%s] ", level);
		out.printf(message, args);
		out.print(ansi ? "\033[0m" : "");
		out.println();
		StatusLine.refresh();
	}

	///

	static PrintStream out() {
		return System.out;
	}

	static PrintStream err() {
		return System.err;
	}


}
