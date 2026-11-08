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
        Content(String title,String... lines){this.title=title;this.lines=Collections.unmodifiableList(Arrays.asList(lines.clone()));}
    }
}
