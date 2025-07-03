package com.github.pwrlabs.pwrj.Utils;

/**
 * NewError class.
 */
public class NewError {

/**
 * errorIf method.
 * @param condition parameter
 * @param message parameter
 * @throws ValidationException exception
 */
    public static void errorIf(boolean condition, String message) throws ValidationException {
        if (condition) { 

            throw new ValidationException(message);
        }
    }
}

