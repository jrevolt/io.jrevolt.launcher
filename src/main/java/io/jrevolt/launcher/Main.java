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
package io.jrevolt.launcher;

import io.jrevolt.launcher.url.UrlSupport;
import io.jrevolt.launcher.util.CommandLine;
import io.jrevolt.launcher.util.Log;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;

import static java.util.Arrays.asList;

/**
 * Start class for MvnLauncher; {@code spring-boot-loader} is executable.
 *
 * @author Patrik Beno
 */
public class Main {

	static private class Action {
		Command cmd;
		CommandLine cmdline;

		Action(Command cmd, CommandLine cmdline) {
			this.cmd = cmd;
			this.cmdline = cmdline;
		}
	}

	static {
		UrlSupport.init();
	}

	public static void main(String[] args) throws Exception {
		try {
			Thread.currentThread().setName("JRevolt:Launcher");
			Action action = new Main().prepare(new LinkedList<>(asList(args)));
			action.cmd.run(action.cmdline);
		} catch (LauncherException e) {
			Log.error(e, "Could not launch application! %s",
						 Arrays.asList(LauncherCfg.offline, LauncherCfg.skipDownload));
			System.exit(-1);
		}
	}

	<T extends Command>
	Action prepare(Queue<String> args) {

		String cmd;
		Class<? extends Command> type;
		Command command;
		Map<String, Class<T>> commands = Command.getCommands();

		CommandLine cmdline = CommandLine.parse(args);
		cmd = cmdline.remainder().peek();

		// undefined command or --help required
		if (cmd == null || cmdline.properties().contains("help")) {
			type = Command.Help.class;

		} else if (commands.containsKey(cmd)) { // standard supported command
			cmdline.remainder().remove();
			type = commands.get(cmd);

		} else { // unsupported command or an artifactId (assume `launch` command)
			type = Command.Launch.class;
		}

		command = Command.create(type);

		exportOptions(cmdline.properties());

		return new Action(command, CommandLine.parse(cmdline.remainder()));
	}

	void exportOptions(Properties properties) {
		Set<String> valid = LauncherCfg.names();
		Set<String> names = properties.stringPropertyNames();
		for (String name : names) {
			String fqname = (valid.contains(name)) ? LauncherCfg.valueOf(name).getPropertyName() : null;
			String value = properties.getProperty(name);
			System.getProperties().setProperty(
					fqname != null ? fqname : name,
					value != null && !value.isEmpty() ? value : "true");
		}
	}

}
