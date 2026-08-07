package com.xtsh.ragchunk.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.util.HexFormat;

public final class ContentHashUtil {

    private static final int BUFFER_SIZE = 8192;

    private ContentHashUtil() {}

    /**
     * 从输入流复制到输出，并返回 SHA-256（十六进制）。用于流式归档。
     */
    public static StreamCopyResult copyWithSha256(InputStream input, OutputStream output) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (Exception e) {
            throw new IOException("SHA-256 not available", e);
        }
        long total = 0;
        try (var digestOut = new DigestOutputStream(output, digest)) {
            byte[] buf = new byte[BUFFER_SIZE];
            int n;
            while ((n = input.read(buf)) >= 0) {
                if (n == 0) {
                    continue;
                }
                digestOut.write(buf, 0, n);
                total += n;
            }
            digestOut.flush();
        }
        return new StreamCopyResult(total, HexFormat.of().formatHex(digest.digest()));
    }

    public static String sha256Hex(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(data));
        } catch (Exception e) {
            return null;
        }
    }

    public record StreamCopyResult(long bytesWritten, String sha256Hex) {}
}
