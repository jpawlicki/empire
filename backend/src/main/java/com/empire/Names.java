package com.empire;

import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

public class Names {
    enum Gender {
        @SerializedName("male") MALE,
        @SerializedName("female") FEMALE,
        @SerializedName("neutral") NEUTRAL
    }

    private static Map<Culture, List<Map<Gender, List<String>>>> names;

    static{
        String s = StringUtil.readResourceAsString("names.json");
        Type t = new TypeToken<Map<Culture, List<Map<Gender, List<String>>>>>(){}.getType();
        names = JsonUtils.gson.fromJson(s, t);
    }

    public static String getRandomName(Culture culture, Gender gender) {
        switch (culture) {
            case EOLSUNG:
                return getRandomNameEolsung(gender);
            case TYRGAETAN:
                return getRandomNameTyrgaetan(gender);
            case TAVIAN:
                return getRandomNameTavian(gender);
            case HANSA:
                return getRandomNameHansa(gender);
            case ANPILAYN:
                return getRandomNameAnpilayn(gender);
            default:
                throw new RuntimeException("Unrecognized culture: " + culture);
        }
    }

    private static String getRandomNameEolsung(Gender gender) {
        String n1 = pickName(Culture.EOLSUNG, gender, 0);
        String n2 = Math.random() < 0.4 ? (" " + pickName(Culture.EOLSUNG, gender, 0) + (gender == Gender.MALE ? "son" : "dottr")) : "";
        return n1 + n2;
    }

    private static String getRandomNameTyrgaetan(Gender gender) {
        String n1 = pickName(Culture.EOLSUNG, gender, 0);
        String n2 = pickName(Culture.EOLSUNG, Gender.NEUTRAL, 0) + (Math.random() < 0.5 ? "ki" : "kun");
        return n1 + n2;
    }

    private static String getRandomNameTavian(Gender gender) {
        return pickName(Culture.TAVIAN, gender, 0);
    }

    private static String getRandomNameHansa(Gender gender) {
        String n1 = pickName(Culture.HANSA, gender, 0);
        String n2 = pickName(Culture.HANSA, Gender.NEUTRAL, 0);
        return n1 + " of " + n2;
    }

    private static String getRandomNameAnpilayn(Gender gender) {
        String n1 = pickName(Culture.ANPILAYN, gender, 0);
        String n2 = pickName(Culture.ANPILAYN, gender, 1);
        String n3 = pickName(Culture.ANPILAYN, gender, 2);
        return n1 + " " + n2 + " " + n3;
    }

    private static String pickName(Culture culture, Gender gender, int num){
        List<Map<Gender, List<String>>> cultureNames = names.get(culture);
        if (num < 0 || num > cultureNames.size() - 1) pick(cultureNames.get(0).get(gender));
        Map<Gender, List<String>> n = cultureNames.get(num);
        return pick(cultureNames.get(num).get(gender));
    }

    private static <T> T pick(List<T> a) {
        return a.get((int)(Math.random() * a.size()));
    }

    public static void main(String[] args) {
        System.out.println(Names.getRandomName(Culture.ANPILAYN, Gender.MALE));
    }
}
