package dev.sketchtv.home;
import org.json.*;
import java.text.SimpleDateFormat;
import java.util.*;

final class BuiltinParsers {
    static String date(String raw)throws Exception {
        SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd",Locale.US);f.setLenient(false);f.setTimeZone(TimeZone.getTimeZone("UTC"));
        SimpleDateFormat out=new SimpleDateFormat("dd/MM/yyyy",Locale.ITALY);out.setTimeZone(TimeZone.getTimeZone("UTC"));return out.format(f.parse(raw.substring(0,10)));
    }
    static String[] parse(int type,String raw)throws Exception {
        JSONObject j=new JSONObject(raw);
        if(type==0){JSONObject c=j.getJSONObject("current"),d=j.getJSONObject("daily");double current=c.getDouble("temperature_2m"),min=d.getJSONArray("temperature_2m_min").getDouble(0),max=d.getJSONArray("temperature_2m_max").getDouble(0);int rain=d.getJSONArray("precipitation_probability_max").getInt(0);if(!temperature(current)||!temperature(min)||!temperature(max)||min>max||rain<0||rain>100)throw new Exception("Valori meteo non validi");String time=c.getString("time");date(time);if(!time.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}.*"))throw new Exception("Data meteo assente");return new String[]{Math.round(current)+"°",time,"Min "+Math.round(min)+"° · Max "+Math.round(max)+"° · Prob. pioggia "+rain+"%"};}
        if(type==1){
            JSONObject table=j.getJSONObject("MRData").getJSONObject("RaceTable");JSONArray races=table.getJSONArray("Races");
            if(races.length()==0)throw new Exception("Nessun GP");JSONObject race=races.getJSONObject(races.length()-1);
            JSONArray results=race.getJSONArray("Results");ArrayList<JSONObject> drivers=new ArrayList<>();
            for(int i=0;i<results.length();i++){JSONObject r=results.getJSONObject(i);if(Config.CONSTRUCTOR_ID.equals(r.getJSONObject("Constructor").getString("constructorId")))drivers.add(r);}
            if(drivers.size()<2)throw new Exception("Risultati Costruttore incompleti");
            Collections.sort(drivers,(a,b)->a.optInt("position",99)-b.optInt("position",99));
            JSONObject a=drivers.get(0),b=drivers.get(1);
            return new String[]{race.getString("raceName")+" · "+date(race.getString("date")),a.getJSONObject("Driver").getString("familyName"),position(a),b.getJSONObject("Driver").getString("familyName"),position(b),table.getString("season"),race.getString("round")};
        }
        if(type==2){
            JSONObject table=j.getJSONObject("MRData").getJSONObject("StandingsTable");JSONArray lists=table.getJSONArray("StandingsLists");
            if(lists.length()==0)throw new Exception("Classifica assente");JSONObject list=lists.getJSONObject(lists.length()-1);JSONArray entries=list.getJSONArray("ConstructorStandings");
            for(int i=0;i<entries.length();i++){JSONObject e=entries.getJSONObject(i);if(Config.CONSTRUCTOR_ID.equals(e.getJSONObject("Constructor").getString("constructorId")))return new String[]{positive(e.getString("position")),list.getString("season"),list.getString("round")};}
            throw new Exception("Costruttore non presente");
        }
        if(type==3){
            JSONArray events=j.getJSONArray("events");JSONObject best=null;
            for(int i=0;i<events.length();i++){JSONObject e=events.getJSONObject(i),c=e.getJSONArray("competitions").getJSONObject(0);if(c.getJSONObject("status").getJSONObject("type").optBoolean("completed",false) && (best==null||e.getString("date").compareTo(best.getString("date"))>0))best=e;}
            if(best==null)throw new Exception("Partite concluse assenti");JSONArray teams=best.getJSONArray("competitions").getJSONObject(0).getJSONArray("competitors");JSONObject h=null,a=null;
            for(int i=0;i<teams.length();i++){JSONObject t=teams.getJSONObject(i);if("home".equals(t.getString("homeAway")))h=t;else if("away".equals(t.getString("homeAway")))a=t;}
            if(h==null||a==null||!(Config.TEAM_ID.equals(h.getString("id"))||Config.TEAM_ID.equals(a.getString("id"))))throw new Exception("Squadre non valide");
            return new String[]{h.getJSONObject("team").getString("shortDisplayName"),score(h),a.getJSONObject("team").getString("shortDisplayName"),score(a),date(best.getString("date")),String.valueOf(best.getJSONObject("season").getInt("year"))};
        }
        JSONObject entry=findTeam(j,0);if(entry==null)throw new Exception("Squadra non presente");JSONArray stats=entry.getJSONArray("stats");
        for(int i=0;i<stats.length();i++){JSONObject s=stats.getJSONObject(i);if("rank".equals(s.getString("name")))return new String[]{positive(s.getString("displayValue")),String.valueOf(j.getJSONObject("season").getInt("year"))};}
        throw new Exception("Posizione assente");
    }
    private static JSONObject findTeam(Object v,int depth)throws Exception {
        if(depth>24)return null;
        if(v instanceof JSONObject){JSONObject o=(JSONObject)v;if(o.optJSONObject("team")!=null && Config.TEAM_ID.equals(o.getJSONObject("team").optString("id")) && o.has("stats"))return o;Iterator<String> keys=o.keys();while(keys.hasNext()){JSONObject found=findTeam(o.get(keys.next()),depth+1);if(found!=null)return found;}}
        else if(v instanceof JSONArray){JSONArray a=(JSONArray)v;for(int i=0;i<a.length();i++){JSONObject found=findTeam(a.get(i),depth+1);if(found!=null)return found;}}
        return null;
    }
    private static boolean temperature(double value){return !Double.isNaN(value)&&!Double.isInfinite(value)&&value>=-100&&value<=70;}
    private static String position(JSONObject r)throws Exception {String p=r.getString("positionText");return p.matches("[0-9]+")?"P"+positive(p):r.getString("status");}
    private static String positive(String s)throws Exception {if(!s.matches("[0-9]{1,2}")||Integer.parseInt(s)<1)throw new Exception("Posizione non valida");return s;}
    private static String score(JSONObject t)throws Exception {Object s=t.get("score");String v=s instanceof JSONObject?((JSONObject)s).getString("displayValue"):String.valueOf(s);if(!v.matches("[0-9]{1,2}"))throw new Exception("Punteggio non valido");return v;}
}
