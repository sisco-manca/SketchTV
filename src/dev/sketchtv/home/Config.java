package dev.sketchtv.home;
/** Edit this file to personalize your build. No location or favorites are bundled. */
final class Config {
    static final String CITY="Località";
    static final String TIME_ZONE=java.util.TimeZone.getDefault().getID();
    static final String LATITUDE="0", LONGITUDE="0";
    static final boolean WEATHER_ENABLED=false, F1_ENABLED=false, FOOTBALL_ENABLED=false;
    // Provider IDs, not display names. Configure these before enabling sports.
    static final String CONSTRUCTOR_ID="", CONSTRUCTOR_NAME="Formula 1";
    static final String TEAM_ID="", TEAM_NAME="Calcio", LEAGUE_ID="";
    static final String[] APPS={"","","",""};
    static final String[] APP_LABELS={"App 1","App 2","App 3","App 4"};
    static boolean enabled(int row){return row==0?WEATHER_ENABLED:row<=2?F1_ENABLED:FOOTBALL_ENABLED;}
}
