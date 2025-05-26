package com.socialcipher.auth.util;

public class Ciphers {
    public static String saltyLanguage(String text) {
        String vowelsRuLower = "аеёиоуыэюя";
        String vowelsRuUpper = "АЕЁИОУЫЭЮЯ";
        String vowelsEnLower = "aeiouy";
        String vowelsEnUpper = "AEIOUY";
        StringBuilder result = new StringBuilder();
        for (char c : text.toCharArray()) {
            result.append(c);
            if (vowelsRuLower.indexOf(c) != -1) {
                result.append('с').append(c);
            } else if (vowelsEnLower.indexOf(c) != -1) {
                result.append('c').append(c);
            } else if (vowelsRuUpper.indexOf(c) != -1) {
                result.append('С').append(c);
            } else if (vowelsEnUpper.indexOf(c) != -1) {
                result.append('C').append(c);
            }

        }
        return result.toString();
    }

    public static String caesarCipher(String text, int shift) {
        StringBuilder result = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (c >= 'A' && c <= 'Z') { // латиница, верхний регистр
                result.append((char) ('A' + (c - 'A' + shift + 26) % 26));
            } else if (c >= 'a' && c <= 'z') { // латиница, нижний регистр
                result.append((char) ('a' + (c - 'a' + shift + 26) % 26));
            } else if (c >= 'А' && c <= 'Я') { // кириллица, верхний регистр
                result.append((char) ('А' + (c - 'А' + shift + 32) % 32));
            } else if (c >= 'а' && c <= 'я') { // кириллица, нижний регистр
                result.append((char) ('а' + (c - 'а' + shift + 32) % 32));
            } else {
                result.append(c); // не буква — не шифруем
            }
        }
        return result.toString();
    }

    public static String caesarDecipher(String text, int shift) {
        // Для расшифровки просто сдвигаем в обратную сторону
        return caesarCipher(text, -shift);
    }

    public static String saltyLanguageDecipher(String text) {
        String vowelsRu = "аеёиоуыэюяАЕЁИОУЫЭЮЯ";
        String vowelsEn = "aeiouyAEIOUY";
        StringBuilder result = new StringBuilder();
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            result.append(c);
            if (vowelsRu.indexOf(c) != -1) {
                // Проверяем, есть ли после гласной "с" + та же гласная
                if (i + 2 < text.length() && text.charAt(i + 1) == 'с' && text.charAt(i + 2) == c) {
                    i += 2; // пропускаем "с" и дублированную гласную
                }
            } else if (vowelsEn.indexOf(c) != -1) {
                // Проверяем, есть ли после гласной "c" + та же гласная
                if (i + 2 < text.length() && text.charAt(i + 1) == 'c' && text.charAt(i + 2) == c) {
                    i += 2; // пропускаем "c" и дублированную гласную
                }
            }
            i++;
        }
        return result.toString();
    }
}
