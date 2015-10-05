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
package io.jrevolt.launcher.mvn;

import io.jrevolt.launcher.LauncherException;
import org.springframework.boot.loader.archive.Archive;
import org.springframework.boot.loader.archive.JarFileArchive;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Comparator;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Patrik Beno
 */
public class Artifact {

	// compares artifacts by group:artifact:version:packaging:classifier
	static public Comparator<Artifact> COMPARATOR = new Comparator<Artifact>() {
		@Override
		public int compare(Artifact o1, Artifact o2) {
			return o1.asString().compareTo(o2.asString());
		}
	};

	// note: naming convention broken intentionally for simplicity; these enums are also
	// used for debug logging, and
	// the output looks better in CamelCase than UPPER_CASE.
	static public enum Status {
		Undefined, Resolving, Resolved, NotModified, Downloadable, Downloading, Downloaded, Updated, Cached, Offline, NotFound, Invalid
	}

	static public Artifact parse(String mvnuri) {
		Artifact a = tryparse(mvnuri);
		if (a == null) {
			throw new LauncherException(String.format(
					"Invalid Maven artifact URI: \"%s\". " +
					"Expected groupId:artifactId:version[:packaging[:classifier]]",
					mvnuri
			));
		}
		return a;
	}

	/**
	 * group:artifact:version[:packaging[:classifier[:mainclass]]]
	 * @param mvnuri
	 * @return
	 */
	static public Artifact tryparse(String mvnuri) {
		String[] it = mvnuri.split(":");
		String g = get(it, 0, null);
		String a = get(it, 1, null);
		String v = get(it, 2, null);
		String packaging = get(it, 3, "jar");
		String classifier = get(it, 4, null);
		String mainclass = get(it, 5, null);

		if (g == null || a == null || v == null) {
			return tryFile(new File(mvnuri));
		}

		return new Artifact(g, a, v, packaging, classifier, mainclass);
	}

	static public Artifact tryFile(File f) {
		if (!f.exists()) { return null; }
		Artifact a = new Artifact(null, null, null, null, null, null);
		a.file = f;
		a.status = Status.Offline;
		return a;
	}

	static private String get(String[] strings, int idx, String dflt) {
		String s = strings.length > idx ? strings[idx] : null;
		return s != null && s.length()>0 ? s : dflt;
	}


	private String groupId;
	private String artifactId;
	private String version;
	private String packaging;
	private String classifier;
	private String mainClass;

	private String resolvedSnapshotVersion; // e.g. 1.0-SNAPSHOT (logical) -> 1.0.20140131.123456 (timestamped)

	private Status status = Status.Undefined; // resolution status

	private URL source; // origin

    private String repositoryId;

	private File file; // cached

	private Throwable error; // resolver error, if any (for reporting purposes)

    public URLConnection con;
    public File tmp;
    public long size;
    public long downloaded;
    public int requests;


	protected Artifact(String groupId, String artifactId, String version, String packaging, String classifier, String mainClass) {
		this.groupId = groupId;
		this.artifactId = artifactId;
		this.version = version;
		this.packaging = packaging;
		this.classifier = classifier;
		this.mainClass = mainClass;
		if (version != null) { fixupExplicitSnapshotVersion(); }
	}

	public String getGroupId() {
		return groupId;
	}

	public String getArtifactId() {
		return artifactId;
	}

	public String getVersion() {
		return version;
	}

	public String getPackaging() {
		return packaging;
	}

	public String getClassifier() {
		return classifier;
	}

	public String getMainClass() {
		return mainClass;
	}

	public String getResolvedSnapshotVersion() {
		return resolvedSnapshotVersion;
	}

	public void setResolvedSnapshotVersion(String resolvedSnapshotVersion) {
		this.resolvedSnapshotVersion = resolvedSnapshotVersion;
	}

	public String getResolvedVersion() {
		return isSnapshot() ? getResolvedSnapshotVersion() : getVersion();
	}

	// /

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public void setSource(URL source) {
		this.source = source;
	}

	public URL getSource() {
		return source;
	}

    public String getRepositoryId() {
        return repositoryId;
    }

    public void setRepositoryId(String repositoryId) {
        this.repositoryId = repositoryId;
    }

    public void setFile(File file) {
		this.file = file;
	}

	public File getFile() {
		return file;
	}

	public Throwable getError() {
		return error;
	}

	public void setError(Throwable error) {
		this.error = error;
	}

	// /

	Archive getArchive() {
		try {
			return new JarFileArchive(file, file.toURI().toURL());
		}
		catch (IOException e) {
			throw new RuntimeException("Cannot create archive for " + file, e);
		}
	}

	public boolean isUpdated() {
		switch (getStatus()) {
		case Downloaded:
		case Updated:
			return true;
		default:
			return false;
		}
	}

	public boolean isSnapshot() {
		return getVersion().endsWith("-SNAPSHOT");
	}

	public boolean isRelease() {
		return !isSnapshot();
	}

    private void fixupExplicitSnapshotVersion() {
        Pattern pattern = Pattern.compile("(.*)-\\p{Digit}{8}\\.\\p{Digit}{6}-\\p{Digit}+");
        Matcher m = pattern.matcher(getVersion());
        if (m.matches()) {
            resolvedSnapshotVersion = getVersion();
			version = m.replaceFirst("$1-SNAPSHOT");
		}
    }

	// /

	public boolean isError() {
		switch (status) {
		case NotFound:
		case Invalid:
			return true;
		default:
			return false;
		}
	}

	public boolean isWarning() {
		switch (status) {
		case Downloadable:
			return true;
		default:
			return false;
		}
	}

	public String asString() {
		return asString(true);
	}

	public String asString(boolean resolved) {
		if (groupId == null && artifactId == null && file != null) {
			return file.toURI().toString();
		}
		String version = resolved ? Objects.toString(resolvedSnapshotVersion, getVersion()) : getVersion();
        StringBuilder sb = new StringBuilder()
                .append(getGroupId()).append(':')
                .append(getArtifactId()).append(':')
                .append(version).append(':')
                .append(getPackaging());
        if (getClassifier() != null) {
            sb.append(':').append(getClassifier());
        }
        return sb.toString();
	}

	/**
	 * Returns the relative path of this artifact in Maven repository
	 * @return
	 */
	public String getPath() {
		String sversion = (resolvedSnapshotVersion != null && !resolvedSnapshotVersion.isEmpty())
                ? resolvedSnapshotVersion
                : getVersion();
        StringBuilder sb = new StringBuilder()
                .append(getGroupId().replace('.', '/')).append('/')
                .append(getArtifactId()).append('/')
                .append(getVersion()).append('/')
                .append(getArtifactId()).append('-').append(sversion);
        if (getClassifier() != null) {
            sb.append('-').append(getClassifier());
        }
        sb.append('.').append(getPackaging());
		return sb.toString();
	}

    public URL getUrl() {
        try {
            return new URL(source, getPath());
        } catch (MalformedURLException e) {
            throw new UnsupportedOperationException(e);
        }
    }

	@Override
	public String toString() {
		return asString();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		Artifact that = (Artifact) o;

		if (!artifactId.equals(that.artifactId))
			return false;
		if (classifier != null ? !classifier.equals(that.classifier) : that.classifier != null)
			return false;
		if (!groupId.equals(that.groupId))
			return false;
		if (packaging != null ? !packaging.equals(that.packaging) : that.packaging != null)
			return false;
		if (!version.equals(that.version))
			return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = groupId.hashCode();
		result = 31 * result + artifactId.hashCode();
		result = 31 * result + version.hashCode();
		result = 31 * result + (packaging != null ? packaging.hashCode() : 0);
		result = 31 * result + (classifier != null ? classifier.hashCode() : 0);
		return result;
	}
}
