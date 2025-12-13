package org.volumteerhub.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;

@Component
@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {

    @Value("${app.storage.root-dir}")
    private String rootDir;

    private static final String PUBLIC_FOLDER = "public";
    private static final String UPLOADS_FOLDER = "uploads";
    private static final String TEMP_FOLDER = "temp";


    public Path getRootPath() {
        return Paths.get(rootDir).toAbsolutePath();
    }

    public Path getPublicPath() {
        return getRootPath().resolve(PUBLIC_FOLDER);
    }

    public Path getUploadsPath() {
        return getRootPath().resolve(UPLOADS_FOLDER);
    }

    public Path getTempPath() {
        return getRootPath().resolve(TEMP_FOLDER);
    }

    public void setRootDir(String rootDir) {
        this.rootDir = rootDir;
    }
}
