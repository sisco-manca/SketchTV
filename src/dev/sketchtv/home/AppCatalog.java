package dev.sketchtv.home;
import android.content.*;
import android.content.pm.*;
import java.text.Collator;
import java.util.*;

/** Reads installed apps; never installs, enables, disables or removes a package. */
final class AppCatalog {
    static final String[] PINNED={"com.android.vending"};
    static final class Entry {
        final String pkg,label;final Intent intent;
        Entry(String p,String l,Intent i){pkg=p;label=l;intent=i;}
    }
    static boolean excluded(String p){return p.equals("dev.sketchtv.home")||p.equals("com.google.android.tvlauncher")||p.equals("com.google.android.apps.tv.launcherx");}
    static List<Entry> read(PackageManager pm){
        Map<String,Entry> found=new HashMap<>();
        for(String category:new String[]{Intent.CATEGORY_LEANBACK_LAUNCHER,Intent.CATEGORY_LAUNCHER}){
            List<ResolveInfo> matches=pm.queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(category),0);
            for(ResolveInfo ri:matches){ActivityInfo a=ri.activityInfo;if(a==null||a.applicationInfo==null||!a.enabled||!a.exported||!a.applicationInfo.enabled||excluded(a.packageName)||found.containsKey(a.packageName))continue;
                Intent launch=new Intent(Intent.ACTION_MAIN).addCategory(category).setClassName(a.packageName,a.name);
                found.put(a.packageName,new Entry(a.packageName,label(pm,a.applicationInfo),launch));
            }
        }
        // An enabled user app without a launcher can still be reached through its app information.
        for(ApplicationInfo a:pm.getInstalledApplications(0))if(a.enabled&&(a.flags&ApplicationInfo.FLAG_SYSTEM)==0&&!excluded(a.packageName)&&!found.containsKey(a.packageName))found.put(a.packageName,new Entry(a.packageName,label(pm,a),null));
        List<Entry> all=new ArrayList<>(found.values());Collator collator=Collator.getInstance(Locale.forLanguageTag(Config.LOCALE));
        Collections.sort(all,(a,b)->{int pa=priority(a.pkg),pb=priority(b.pkg);if(pa!=pb)return pa-pb;int n=collator.compare(a.label,b.label);return n!=0?n:a.pkg.compareTo(b.pkg);});
        return all;
    }
    static long fingerprint(List<Entry> entries){long h=1;for(Entry e:entries){h=31*h+e.pkg.hashCode();h=31*h+e.label.hashCode();h=31*h+(e.intent==null?0:e.intent.toUri(0).hashCode());}return h;}
    private static int priority(String pkg){for(int i=0;i<PINNED.length;i++)if(PINNED[i].equals(pkg))return i;return PINNED.length;}
    private static String label(PackageManager pm,ApplicationInfo app){try{CharSequence s=pm.getApplicationLabel(app);return s==null?app.packageName:s.toString();}catch(Exception e){return app.packageName;}}
}
