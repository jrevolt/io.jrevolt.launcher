package io.jrevolt.launcher;

import io.jrevolt.launcher.vault.Vault;
import org.junit.After;
import org.junit.Before;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * @author <a href="mailto:patrikbeno@gmail.com">Patrik Beno</a>
 */
public abstract class AbstractTest {

    Path crt, key;

    @Before
    public void before() throws Exception {
        UUID uuid = UUID.randomUUID();
        crt = Paths.get(System.getProperty("java.io.tmpdir"), uuid.toString()+".crt");
        key = Paths.get(System.getProperty("java.io.tmpdir"), uuid.toString() + ".key");
        Files.copy(AbstractTest.class.getResourceAsStream("/test.crt"), crt, StandardCopyOption.REPLACE_EXISTING);
        Files.copy(AbstractTest.class.getResourceAsStream("/test.key"), key, StandardCopyOption.REPLACE_EXISTING);
        System.setProperty("jrevolt.launcher.defaults", "classpath:LauncherCfg.properties");
        LauncherCfg.configure();

        System.setProperty("jrevolt.vault.user.certFile", crt.toString());
        System.setProperty("jrevolt.vault.user.keyFile", key.toString());
        Vault.instance();
    }

    @After
    public void after() throws Exception {
        Vault.close();
        Files.deleteIfExists(crt);
        Files.deleteIfExists(key);
    }
}
