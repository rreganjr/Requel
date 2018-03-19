package com.rreganjr.requel.user;

import com.rreganjr.ApplicationException;

public class PasswordException extends ApplicationException {

    public static final String MSG_PROBLEM_ENCRYPTING_PASSWORD = "There was a problem encrypting the user's password.";
    public static final String MSG_PROBLEM_ENCRYPTING_PASSWORD_WITH_MESSAGE = "There was a problem encrypting the user's password: %s";
    public static final String MSG_PROBLEM_GENERATING_PASSWORD_SALT = "There was a problem generating the password salt value.";
    public static final String MSG_BAD_ALGORITHM_NAME = "The supplied algorithm name '%s' is not a supported SecretKeyFactory or MessageDigest algorithm name.";

    public static PasswordException problemEncryptingPassword(Exception e) {
        return new PasswordException(e, MSG_PROBLEM_ENCRYPTING_PASSWORD);
    }

    public static PasswordException problemEncryptingPassword(String message) {
        return new PasswordException(MSG_PROBLEM_ENCRYPTING_PASSWORD_WITH_MESSAGE, message);
    }

    public static PasswordException badAlgorithmName(String badAlgorithmName) {
        return new PasswordException(MSG_BAD_ALGORITHM_NAME, badAlgorithmName);
    }

    public static PasswordException problemGeneratingPasswordSalt(Exception e) {
        return new PasswordException(e, MSG_PROBLEM_GENERATING_PASSWORD_SALT);
    }

    protected PasswordException(String format, Object... args) {
        super(format, args);
    }

    protected PasswordException(Throwable cause, String format, Object... args) {
        super(cause, format, args);
    }
}
