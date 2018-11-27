package io.rapidpro.surveyor.data;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import io.rapidpro.surveyor.Logger;
import io.rapidpro.surveyor.net.TembaException;

/**
 * Directory based service for org configurations
 */
public class OrgService {

    private File rootDir;
    private Logger log;

    private Map<String, Org> cache = new HashMap<>();

    public OrgService(File rootDir, Logger log) {
        this.rootDir = rootDir;
        this.log = log;

        log.d("OrgService created for directory " + this.rootDir.getAbsolutePath());
    }

    public Org get(String uuid) throws IOException {
        if (cache.containsKey(uuid)) {
            log.d("Returning cached org " + uuid);
            return cache.get(uuid);
        }

        File directory = new File(rootDir, uuid);
        Org org = Org.load(directory);
        log.d("Loaded org " + uuid);
        cache.put(uuid, org);
        return org;
    }

    /**
     * Fetches an org using the given API token and saves it to the org storage
     *
     * @param uuid  the UUID of the org
     * @param name  the name of the org
     * @param token the API token
     */
    public Org getOrFetch(String uuid, String name, String token) throws TembaException, IOException {
        File directory = new File(rootDir, uuid);
        if (directory.exists() && directory.isDirectory()) {
            return get(uuid);
        }

        Org org = Org.create(directory, name, token);
        org.refresh(false, null);
        return org;
    }

    public void clearCache() {
        cache.clear();
    }
}
