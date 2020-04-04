package am.ik.lab.syaberu;

import org.springframework.stereotype.Component;

@Component
public class ApiKeyEncryptor {
    public static final String PREFIX = "ENC:";
    private final EncryptionConfig config;

    public ApiKeyEncryptor(EncryptionConfig config) {
        this.config = config;
    }

    public String encrypt(String apiKey) {
        if (apiKey.startsWith(PREFIX)) {
            // already encrypted
            return apiKey;
        }
        return PREFIX + config.textEncryptor().encrypt(apiKey);
    }
}
