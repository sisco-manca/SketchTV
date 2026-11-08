package dev.sketchtv.home;
import java.util.*;

/** One widget owns its endpoints, attribution, refresh interval and parser. */
interface WidgetProvider {
    String id();
    String title();
    boolean enabled();
    long intervalMillis();
    List<Source> sources();
    Content parse(List<String> responses) throws Exception;

    final class Source {
        final String name,url,license;
        Source(String name,String url,String license){this.name=name;this.url=url;this.license=license;}
    }
    final class Content {
        final String title;
        final List<String> lines;
        Content(String title,String... lines){
            if(!validText(title,100)||lines==null||lines.length==0||lines.length>5)throw new IllegalArgumentException("Invalid widget content size");
            for(String line:lines)if(!validText(line,240))throw new IllegalArgumentException("Widget rows must be short, single-line text");
            this.title=title;this.lines=Collections.unmodifiableList(Arrays.asList(lines.clone()));
        }
        private static boolean validText(String text,int max){if(text==null||text.isEmpty()||text.length()>max)return false;for(int i=0;i<text.length();i++)if(Character.isISOControl(text.charAt(i)))return false;return true;}
    }
}
