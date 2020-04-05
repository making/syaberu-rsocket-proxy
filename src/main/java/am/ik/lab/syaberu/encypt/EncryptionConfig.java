package am.ik.lab.syaberu.encypt;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component
@ConfigurationProperties(prefix = "encryption")
public class EncryptionConfig implements Validator {
    private String password;
    private String salt;

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    public TextEncryptor textEncryptor() {
        if (this.password != null && this.salt != null) {
            return Encryptors.delux(this.password, this.salt);
        } else {
            return Encryptors.noOpText();
        }
    }

    @Override
    public boolean supports(Class<?> aClass) {
        return EncryptionConfig.class.isAssignableFrom(aClass);
    }

    @Override
    public void validate(Object o, Errors errors) {
        if (this.password == null ^ this.salt == null) {
            errors.rejectValue("password", "",
                    "Both 'password' and 'salt' must be set");
        }
    }
}
