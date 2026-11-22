package com.niixlabs.inputtweaks.core.share;

import com.niixlabs.inputtweaks.api.trigger.TriggerMode;
import com.niixlabs.inputtweaks.core.model.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public final class ShareCodeCodec {
    private static final String PREFIX = "IT1:";

    private ShareCodeCodec() {
    }

    public static String encode(ProfileData profile) {
        StringBuilder sb = new StringBuilder();
        sb.append(escape(profile.name())).append('\n');

        for (Map.Entry<String, ProfileEntry> kv : profile.entries().entrySet()) {
            ProfileEntry e = kv.getValue();
            if (e.isEmpty()) continue;

            String keyStr = e.chord() != null ? e.chord().primary().serialize() : "";
            String modsStr = e.chord() != null
                    ? String.join(",", e.chord().modifiers().stream().map(KeyCode::serialize).toList())
                    : "";
            String prioStr = e.priority() != null ? e.priority().toString() : "";
            String polStr = e.policy() != null ? e.policy().name() : "";
            String ctxStr = e.contextId() != null ? e.contextId() : "";
            String trigStr = e.triggerMode() != null ? e.triggerMode().name() : "";

            sb.append(escape(kv.getKey())).append('|')
                    .append(keyStr).append('|')
                    .append(modsStr).append('|')
                    .append(prioStr).append('|')
                    .append(polStr).append('|')
                    .append(ctxStr).append('|')
                    .append(trigStr).append('\n');
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             GZIPOutputStream gzip = new GZIPOutputStream(baos)) {
            gzip.write(sb.toString().getBytes(StandardCharsets.UTF_8));
            gzip.finish();
            return PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(baos.toByteArray());
        } catch (IOException ex) {
            return "";
        }
    }

    public static Optional<ProfileData> decode(String code) {
        if (code == null || !code.trim().startsWith(PREFIX)) {
            return Optional.empty();
        }
        String payload = code.trim().substring(PREFIX.length());
        try {
            byte[] compressed = Base64.getUrlDecoder().decode(payload);
            String text;
            try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(compressed))) {
                text = new String(gzip.readAllBytes(), StandardCharsets.UTF_8);
            }

            String[] lines = text.split("\\R");
            if (lines.length == 0 || lines[0].isBlank()) return Optional.empty();

            String name = unescape(lines[0]);
            Map<String, ProfileEntry> entries = new LinkedHashMap<>();

            for (int i = 1; i < lines.length; i++) {
                if (lines[i].isBlank()) continue;
                String[] p = lines[i].split("\\|", -1);
                if (p.length < 7) continue;

                String id = unescape(p[0]);
                KeyChord chord = null;
                if (!p[1].isEmpty()) {
                    KeyCode primary = KeyCode.parse(p[1]);
                    List<KeyCode> mods = new ArrayList<>();
                    if (!p[2].isEmpty()) {
                        for (String m : p[2].split(",")) {
                            mods.add(KeyCode.parse(m));
                        }
                    }
                    chord = KeyChord.of(primary, mods);
                }
                Integer priority = p[3].isEmpty() ? null : Integer.parseInt(p[3]);
                ConflictPolicy policy = p[4].isEmpty() ? null : ConflictPolicy.valueOf(p[4]);
                String ctx = p[5].isEmpty() ? null : p[5];
                TriggerMode trig = p[6].isEmpty() ? null : TriggerMode.valueOf(p[6]);
                entries.put(id, new ProfileEntry(chord, priority, policy, ctx, trig));
            }
            return Optional.of(new ProfileData(name, entries));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("|", "\\p").replace("\n", "");
    }

    private static String unescape(String s) {
        return s.replace("\\p", "|").replace("\\\\", "\\");
    }
}
