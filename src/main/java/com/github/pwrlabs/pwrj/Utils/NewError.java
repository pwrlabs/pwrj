package com.github.pwrlabs.pwrj.Utils;

public class NewError {

    public static void errorIf(boolean condition, String message) throws ValidationException {
        if (condition) {
            throw new ValidationException(message);
        }
    }
}

