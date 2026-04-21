package com.manifest.auth.security;

import com.manifestreader.common.exception.BusinessException;
import com.manifestreader.common.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.security.PrivateKey;
import java.security.PublicKey;

@Component
public class LocalKeyProvider implements KeyProvider {

    @Override
    public PublicKey getPublicKey() {
        // TODO 从 manifest.auth.token.public-key-path 或环境变量加载 RSA 公钥。
        throw new BusinessException(ErrorCode.NOT_IMPLEMENTED);
    }

    @Override
    public PrivateKey getPrivateKey() {
        // TODO 从 manifest.auth.token.private-key-path 或环境变量加载 RSA 私钥。
        throw new BusinessException(ErrorCode.NOT_IMPLEMENTED);
    }

    @Override
    public String getPublicKeyPem() {
        // TODO 返回 PEM 格式公钥，供 gateway 或运维配置使用。
        return "";
    }
}
