package com.github.pwrlabs.pwrj.Utils;

public class Hex {

    public static byte[] decode(String hexString) {
        if (hexString == null || hexString.isEmpty()) {
            return new byte[0];
        }

        String cleanedHexString = hexString.startsWith("0x") ? hexString.substring(2) : hexString;

        if (cleanedHexString.length() % 2 != 0) {
            throw new IllegalArgumentException("Invalid hexadecimal string: " + hexString);
        }

        byte[] bytes = new byte[cleanedHexString.length() / 2];

        for (int i = 0; i < cleanedHexString.length(); i += 2) {
            bytes[i / 2] = (byte) ((Character.digit(cleanedHexString.charAt(i), 16) << 4)
                    + Character.digit(cleanedHexString.charAt(i + 1), 16));
        }

        return bytes;
    }

    public static String toHexString(byte[] bytes) {
        if (bytes == null) {
            return null;
        }

        StringBuilder hexString = new StringBuilder(bytes.length * 2);

        for (byte b : bytes) {
            int value = b & 0xFF; // Convert signed byte to unsigned int
            hexString.append(String.format("%02X", value));
        }

        return hexString.toString();
    }
}
