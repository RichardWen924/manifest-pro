package com.manifestreader.common.constant;

public final class RedisKeyConstants {

    public static final String AUTH_TOKEN = "auth:token:";
    public static final String AUTH_REFRESH_TOKEN = "auth:refresh:";
    public static final String AUTH_TOKEN_BLACKLIST = "auth:blacklist:";
    public static final String AUTH_USER_SESSION = "auth:session:";
    public static final String USER_PROFILE = "user:profile:";
    public static final String LOGIN_CAPTCHA = "login:captcha:";

    private RedisKeyConstants() {
    }
}
