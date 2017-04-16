package cz.martlin.jevernote.app;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.martlin.jevernote.dataobj.misc.CommandLineData;
import cz.martlin.jevernote.misc.ConsoleLoggingConfigurer;

public class CommandsParser {

	private final Logger LOG = LoggerFactory.getLogger(getClass());

	public CommandsParser() {
	}

	public boolean process(CommandLineData data) {
		ConsoleLoggingConfigurer.setTo(data.isVerbose(), data.isDebug());

		File basePath = basePath(data);
		MainCommandsPerformer performer = new MainCommandsPerformer(basePath, data.isDryRun(), data.isInteractive());

		performer.load();
		if (!performer.isLoaded()) {
			return false;
		}

		boolean success = process(performer, data);

		performer.store();
		if (!performer.isLoaded()) {
			return false;
		}

		return success;

	}

	private boolean process(MainCommandsPerformer performer, CommandLineData data) {
		switch (data.getCommand()) {
		case "init":
			return performer.cmdInit(data.getRemoteToken());

		case "clone":
			return performer.cmdClone(data.getRemoteToken());

		case "push":
			return performer.cmdPush(data.isWeak(), data.isForce());

		case "pull":
			return performer.cmdPull(data.isWeak(), data.isForce());

		case "synchronize":
			return performer.cmdSynchronize(data.isPreferLocal());

		case "status":
			return performer.cmdStatus();
		default:
			LOG.error("Unknown command " + data.getCommand());
			return false;
		}
	}

	private File basePath(CommandLineData data) {
		if (data.getBaseDir() != null) {
			return data.getBaseDir();
		} else {
			String path = System.getProperty("user.dir");
			return new File(path);
		}

	}

}