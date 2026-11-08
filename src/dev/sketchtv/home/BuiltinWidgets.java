package dev.sketchtv.home;
import java.util.*;

/** Register providers here; neither the renderer nor the activity knows their data format. */
final class BuiltinWidgets {
    static List<WidgetProvider> create(){
        return Arrays.asList(
            new Basic("weather",Config.CITY,Config.WEATHER_ENABLED,Config.WEATHER_INTERVAL,
                new WidgetProvider.Source("Open-Meteo","https://api.open-meteo.com/v1/forecast?latitude="+Config.LATITUDE+"&longitude="+Config.LONGITUDE+"&current=temperature_2m,weather_code&daily=temperature_2m_max,temperature_2m_min,precipitation_probability_max&forecast_days=1&timezone=auto","CC BY 4.0")){
                public Content parse(List<String> r)throws Exception{String[] d=BuiltinParsers.parse(0,r.get(0));return new Content(title(),d[0],"Rilevazione: "+d[1].replace('T',' '),d[2]);}
            },
            new Basic("f1",Config.CONSTRUCTOR_NAME,Config.F1_ENABLED,Config.F1_INTERVAL,
                new WidgetProvider.Source("Jolpica F1","https://api.jolpi.ca/ergast/f1/current/last/constructors/"+Config.CONSTRUCTOR_ID+"/results.json","Condizioni del provider"),
                new WidgetProvider.Source("Jolpica F1","https://api.jolpi.ca/ergast/f1/current/constructorStandings.json","Condizioni del provider")){
                public Content parse(List<String> r)throws Exception{String[] d=BuiltinParsers.parse(1,r.get(0)),s=BuiltinParsers.parse(2,r.get(1));if(!d[5].equals(s[1])||!d[6].equals(s[2]))throw new Exception("Risultati e classifica non ancora allineati");return new Content(title(),d[0],d[1]+" · "+d[2],d[3]+" · "+d[4],"Costruttori · P"+s[0]);}
            },
            new Basic("football",Config.TEAM_NAME,Config.FOOTBALL_ENABLED,Config.FOOTBALL_INTERVAL,
                new WidgetProvider.Source("ESPN","https://site.api.espn.com/apis/site/v2/sports/soccer/"+Config.LEAGUE_ID+"/teams/"+Config.TEAM_ID+"/schedule","Verificare condizioni ESPN"),
                new WidgetProvider.Source("ESPN","https://site.api.espn.com/apis/v2/sports/soccer/"+Config.LEAGUE_ID+"/standings","Verificare condizioni ESPN")){
                public Content parse(List<String> r)throws Exception{String[] d=BuiltinParsers.parse(3,r.get(0)),s=BuiltinParsers.parse(4,r.get(1));if(!d[5].equals(s[1]))throw new Exception("Stagioni non allineate");return new Content(title(),d[0]+" · "+d[1],d[2]+" · "+d[3],"Ultima conclusa · "+d[4],"Classifica · "+s[0]+"ª");}
            }
        );
    }
    private abstract static class Basic implements WidgetProvider {
        private final String id,title;private final boolean enabled;private final long interval;private final List<Source> sources;
        Basic(String id,String title,boolean enabled,long interval,Source... sources){this.id=id;this.title=title;this.enabled=enabled;this.interval=interval;this.sources=Collections.unmodifiableList(Arrays.asList(sources));}
        public String id(){return id;}public String title(){return title;}public boolean enabled(){return enabled;}public long intervalMillis(){return interval;}public List<Source> sources(){return sources;}
    }
}
