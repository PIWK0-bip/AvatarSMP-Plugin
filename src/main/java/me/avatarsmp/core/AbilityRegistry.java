package me.avatarsmp.core;

/**
 * Central registry of the 8 ability names/level requirements per bending element,
 * shared between the Skills GUI, the Bind GUI and the Scoreboard so all three
 * always stay in sync.
 */
public final class AbilityRegistry {

    public static final String[] FIRE_NAMES = {
            "Ognisty Cios", "Ognisty Podmuch", "Ognista Ostona", "Falisty Ogien",
            "Kombustia", "Ognisty Slup", "Inferno", "Plomienna Nowa"
    };

    public static final String[] WATER_NAMES = {
            "Płynny Krąg", "Wodna Bariera", "Skupienie Fali", "Lodowa Pułapka",
            "Tsunami", "Przypływ", "Forma Ośmiornicy", "Wodna Furia"
    };

    public static final String[] EARTH_NAMES = {
            "Rzut Kamieniem", "Kamienny Grot", "Kamienna Zbroja", "Kamienna Sciana",
            "Trzesienie Ziemi", "Ruchome Piaski", "Glaz", "Kamienna Furia"
    };

    public static final String[] AIR_NAMES = {
            "Podmuch Powietrza", "Uderzenie Powietrza", "Oslona Powietrza", "Zasysanie Powietrza",
            "Tornado", "Podryw", "Wodospad Powietrza", "Huragan"
    };

    /**
     * Matches BendingManager#isSlotUnlocked level gates exactly.
     */
    public static final int[] REQUIRED_LEVELS = {1, 1, 5, 10, 15, 20, 25, 50};

    private AbilityRegistry() {
    }

    public static String[] namesFor(Element element) {
        return switch (element) {
            case FIRE -> FIRE_NAMES;
            case WATER -> WATER_NAMES;
            case EARTH -> EARTH_NAMES;
            case AIR -> AIR_NAMES;
            default -> new String[0];
        };
    }

    public static String nameFor(Element element, int index) {
        String[] names = namesFor(element);
        if (index < 0 || index >= names.length) {
            return "???";
        }
        return names[index];
    }

    public static int requiredLevel(int abilityIndex) {
        if (abilityIndex < 0 || abilityIndex >= REQUIRED_LEVELS.length) {
            return Integer.MAX_VALUE;
        }
        return REQUIRED_LEVELS[abilityIndex];
    }
}