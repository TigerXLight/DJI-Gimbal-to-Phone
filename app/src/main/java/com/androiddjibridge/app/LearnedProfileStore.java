package com.androiddjibridge.app;

import android.content.Context;
import android.util.Log;

import com.androiddjibridge.core.CameraProfile;
import com.androiddjibridge.core.CameraProfileJsonCodec;
import com.androiddjibridge.core.CameraProfilePersistencePolicy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** App-private, validated persistence for user-learned profile JSON. */
public final class LearnedProfileStore {
    private static final String TAG = "LearnedProfileStore";
    private static final String DIRECTORY_NAME = "learned_profiles";
    private final Context context;
    private final Set<String> builtInProfileIds;
    private final Set<String> builtInPackageNames;

    public LearnedProfileStore(
            Context context,
            Set<String> builtInProfileIds,
            Set<String> builtInPackageNames) {
        if (context == null || builtInProfileIds == null || builtInPackageNames == null) {
            throw new IllegalArgumentException("store configuration is required");
        }
        this.context = context.getApplicationContext();
        this.builtInProfileIds = Collections.unmodifiableSet(new HashSet<>(builtInProfileIds));
        this.builtInPackageNames = Collections.unmodifiableSet(
                new HashSet<>(builtInPackageNames));
    }

    public List<CameraProfile> loadProfiles() {
        File directory = directory();
        File[] files = directory.listFiles((file, name) -> name.endsWith(".json"));
        if (files == null) {
            return Collections.emptyList();
        }
        Map<String, CameraProfile> byId = new LinkedHashMap<>();
        Set<String> packages = new HashSet<>(builtInPackageNames);
        for (File file : files) {
            try {
                CameraProfile profile = read(file);
                if (!CameraProfilePersistencePolicy.canPersistLearned(
                        profile, builtInProfileIds, builtInPackageNames)) {
                    Log.w(TAG, "rejected learned profile policy file=" + file.getName());
                    continue;
                }
                if (byId.containsKey(profile.getStableId())
                        || !packages.add(profile.getPackageName())) {
                    Log.w(TAG, "rejected duplicate learned profile file=" + file.getName());
                    continue;
                }
                byId.put(profile.getStableId(), profile);
            } catch (RuntimeException | IOException error) {
                Log.w(TAG, "rejected malformed learned profile file=" + file.getName(), error);
            }
        }
        List<CameraProfile> result = new ArrayList<>(byId.values());
        result.sort(Comparator.comparing(CameraProfile::getStableId));
        return Collections.unmodifiableList(result);
    }

    public boolean save(CameraProfile profile) {
        if (!CameraProfilePersistencePolicy.canPersistLearned(
                profile, builtInProfileIds, builtInPackageNames)) {
            return false;
        }
        for (CameraProfile existing : loadProfiles()) {
            if (existing.getStableId().equals(profile.getStableId())
                    || existing.getPackageName().equals(profile.getPackageName())) {
                return false;
            }
        }
        File directory = directory();
        if (!directory.exists() && !directory.mkdirs()) {
            return false;
        }
        File target = new File(directory, profile.getStableId() + ".json");
        File temporary = new File(directory, profile.getStableId() + ".json.tmp");
        try (Writer writer = new OutputStreamWriter(
                new FileOutputStream(temporary), StandardCharsets.UTF_8)) {
            writer.write(CameraProfileJsonCodec.toJson(profile));
        } catch (IOException error) {
            Log.w(TAG, "failed to write learned profile", error);
            temporary.delete();
            return false;
        }
        if (!temporary.renameTo(target)) {
            temporary.delete();
            return false;
        }
        return true;
    }

    private File directory() {
        return new File(context.getFilesDir(), DIRECTORY_NAME);
    }

    private CameraProfile read(File file) throws IOException {
        StringBuilder json = new StringBuilder();
        try (Reader reader = new InputStreamReader(
                new FileInputStream(file), StandardCharsets.UTF_8)) {
            char[] buffer = new char[1024];
            int count;
            while ((count = reader.read(buffer)) >= 0) {
                json.append(buffer, 0, count);
            }
        }
        return CameraProfileJsonCodec.fromJson(json.toString());
    }
}
