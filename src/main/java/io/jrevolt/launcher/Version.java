package io.jrevolt.launcher;

import io.jrevolt.launcher.util.IOHelper;

import java.io.InputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.jar.Manifest;

/**
 * @author <a href="mailto:patrikbeno@gmail.com">Patrik Beno</a>
 */
public class Version {

	private String specificationVersion;
	private String implementationVersion;
	private String scmBranch;
	private String scmRevision;
	private String buildNumber;
	private Date buildTimestamp;

	static public Version version() {
		Version v = new Version();
		InputStream in = null;
		try {
			v.specificationVersion = Version.class.getPackage().getSpecificationVersion();
			v.implementationVersion = Version.class.getPackage().getImplementationVersion();
			in = new URL(Version.class.getResource("Version.class").toString()
									 .replaceFirst("!.*", "!/META-INF/MANIFEST.MF"))
					.openStream();
			Manifest mf = new Manifest(in);
			v.scmBranch = mf.getMainAttributes().getValue("SCM-Branch");
			v.scmRevision = mf.getMainAttributes().getValue("SCM-Revision");
			v.buildNumber = mf.getMainAttributes().getValue("Build-Number");
			SimpleDateFormat sdf = new SimpleDateFormat(mf.getMainAttributes().getValue("Build-Timestamp-Format"));
			v.buildTimestamp = sdf.parse(mf.getMainAttributes().getValue("Build-Timestamp"));
		} catch (Exception ignore) {
		} finally {
			IOHelper.close(in);
		}
		return v;
	}

	public String getSpecificationVersion() {
		return specificationVersion;
	}

	public String getImplementationVersion() {
		return implementationVersion;
	}

	public String getScmBranch() {
		return scmBranch;
	}

	public String getScmRevision() {
		return scmRevision;
	}

	public String getBuildNumber() {
		return buildNumber;
	}

	public Date getBuildTimestamp() {
		return buildTimestamp;
	}

	public String getVersionString() {
		return String.format("JRevolt Launcher (%s, %tc)", scmRevision, buildTimestamp);
	}
}
