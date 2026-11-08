package dev.sketchtv.home;
import java.nio.file.*;
import java.util.*;
import org.json.*;
public class WidgetParsersTest {
    static int checks;
    static String read(String name)throws Exception{return new String(Files.readAllBytes(Paths.get("tests/fixtures/"+name+".json")),"UTF-8");}
    static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
    static void fails(int type,String raw){try{BuiltinParsers.parse(type,raw);throw new AssertionError("Malformed source was accepted: "+type);}catch(AssertionError e){throw e;}catch(Exception good){checks++;}}
    public static void main(String[] args)throws Exception{
        String[] w=BuiltinParsers.parse(0,read("weather"));check(w[0].equals("18°"),"weather temperature");check(w[2].contains("25%"),"rain probability");
        String[] race=BuiltinParsers.parse(1,read("race"));check(race[1].equals("Driver A")&&race[2].equals("P2"),"driver sorted with own position");check(race[3].equals("Driver B")&&race[4].equals("P5"),"second position");
        String[] standings=BuiltinParsers.parse(2,read("constructors"));check(standings[0].equals("3"),"constructor selected by configured ID");
        String[] match=BuiltinParsers.parse(3,read("matches"));check(match[1].equals("2")&&match[3].equals("1"),"latest completed match, two score formats");check(match[0].equals("Example A")&&match[2].equals("Example B"),"home-away order");
        JSONObject schedule=new JSONObject(read("matches"));JSONArray events=schedule.getJSONArray("events");schedule.put("events",new JSONArray().put(events.get(2)).put(events.get(1)).put(events.get(0)));check(BuiltinParsers.parse(3,schedule.toString())[1].equals("2"),"independent of API ordering");
        check(BuiltinParsers.parse(4,read("standings"))[0].equals("8"),"explicit rank instead of points or row index");
        for(int i=0;i<5;i++)fails(i,"{}");
        JSONObject broken=new JSONObject(read("matches"));broken.getJSONArray("events").getJSONObject(0).getJSONArray("competitions").getJSONObject(0).getJSONArray("competitors").getJSONObject(0).put("score","unknown");fails(3,broken.toString());
        List<WidgetProvider> providers=BuiltinWidgets.create();check(providers.size()==3,"built-in registry");
        check(providers.get(0).parse(Arrays.asList(read("weather"))).lines.get(0).equals("18°"),"provider builds weather content");
        check(providers.get(1).parse(Arrays.asList(read("race"),read("constructors"))).lines.get(3).equals("Costruttori · P3"),"provider builds F1 content");
        JSONObject wrong=new JSONObject(read("constructors"));wrong.getJSONObject("MRData").getJSONObject("StandingsTable").getJSONArray("StandingsLists").getJSONObject(0).put("round","2");try{providers.get(1).parse(Arrays.asList(read("race"),wrong.toString()));throw new AssertionError("Mismatched race accepted");}catch(AssertionError e){throw e;}catch(Exception good){checks++;}
        wrong=new JSONObject(read("standings"));wrong.getJSONObject("season").put("year",2041);try{providers.get(2).parse(Arrays.asList(read("matches"),wrong.toString()));throw new AssertionError("Mismatched seasons accepted");}catch(AssertionError e){throw e;}catch(Exception good){checks++;}
        System.out.println("PASS: "+checks+" synthetic widget parser and season-coherence checks");
    }
}
