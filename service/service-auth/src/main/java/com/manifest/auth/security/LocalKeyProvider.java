package com.manifest.auth.security;

import com.manifest.auth.properties.AuthTokenProperties;
import com.manifestreader.common.exception.BusinessException;
import com.manifestreader.common.exception.ErrorCode;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class LocalKeyProvider implements KeyProvider {

    private static final Logger log = LoggerFactory.getLogger(LocalKeyProvider.class);
    private static final Path DEV_PRIVATE_KEY = Path.of("service/service-auth/target/auth-keys/private.pem");
    private static final Path DEV_PUBLIC_KEY = Path.of("service/service-auth/target/auth-keys/public.pem");

    private final AuthTokenProperties properties;
    private volatile PublicKey publicKey;
    private volatile PrivateKey privateKey;
    private volatile String publicKeyPem;

    public LocalKeyProvider(AuthTokenProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void init() {
        Path publicKeyPath = resolvePath(properties.getPublicKeyPath(), DEV_PUBLIC_KEY);
        Path privateKeyPath = resolvePath(properties.getPrivateKeyPath(), DEV_PRIVATE_KEY);
        if (!Files.exists(publicKeyPath) || !Files.exists(privateKeyPath)) {
            generateDevKeyPair(publicKeyPath, privateKeyPath);
        }
        this.publicKeyPem = readPem(publicKeyPath);
        this.publicKey = readPublicKey(this.publicKeyPem);
        this.privateKey = readPrivateKey(readPem(privateKeyPath));
    }

    @Override
    public PublicKey getPublicKey() {
        return publicKey;
    }

    @Override
    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    @Override
    public String getPublicKeyPem() {
        return publicKeyPem;
    }

    private Path resolvePath(String configuredPath, Path fallback) {
        return StringUtils.hasText(configuredPath) ? Path.of(configuredPath) : fallback;
    }

    private void generateDevKeyPair(Path publicKeyPath, Path privateKeyPath) {
        try {
            Files.createDirectories(publicKeyPath.getParent());
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair keyPair = generator.generateKeyPair();
            Files.writeString(publicKeyPath, toPem("PUBLIC KEY", keyPair.getPublic().getEncoded()), StandardCharsets.UTF_8);
            Files.writeString(privateKeyPath, toPem("PRIVATE KEY", keyPair.getPrivate().getEncoded()), StandardCharsets.UTF_8);
            log.warn("Generated development RSA key pair at {}. Use AUTH_PUBLIC_KEY_PATH/AUTH_PRIVATE_KEY_PATH in production.", publicKeyPath.getParent());
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR.getCode(), "生成开发 RSA 密钥失败");
        }
    }

    private String readPem(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR.getCode(), "读取 RSA 密钥失败：" + path);
        }
    }

    private PublicKey readPublicKey(String pem) {
        try {
            byte[] bytes = parsePemBody(pem);
            return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(bytes));
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR.getCode(), "解析 RSA 公钥失败");
        }
    }

    private PrivateKey readPrivateKey(String pem) {
        try {
            byte[] bytes = parsePemBody(pem);
            return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(bytes));
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR.getCode(), "解析 RSA 私钥失败");
        }
    }

    private byte[] parsePemBody(String pem) {
        String body = pem
                .replaceAll("-----BEGIN [A-Z ]+-----", "")
                .replaceAll("-----END [A-Z ]+-----", "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(body);
    }

    private String toPem(String type, byte[] encoded) {
        return "-----BEGIN " + type + "-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.UTF_8)).encodeToString(encoded)
                + "\n-----END " + type + "-----\n";
    }
}
