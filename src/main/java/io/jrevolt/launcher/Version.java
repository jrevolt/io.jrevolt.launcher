package io.jrevolt.launcher;

import io.jrevolt.launcher.util.IOHelper;

import java.io.InputStream;
import java.net.URL;
import java.util.jar.Manifest;

/**
 * @author <a href="mailto:patrikbeno@gmail.com">Patrik Beno</a>
 */
public class Version {

    private String specificationVersion;
    private String implementationVersion;
    private String scmRevision;

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
            v.scmRevision = mf.getMainAttributes().getValue("SCM-Revision");
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

    public String getScmRevision() {
        return scmRevision;
    }

    public String getVersionString() {
        return String.format("JRevolt Launcher (%s)", scmRevision);
    }
}
