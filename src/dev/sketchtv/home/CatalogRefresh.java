package dev.sketchtv.home;
/** Dirty state is consumed by rendering, never by a scan completed while the Activity is hidden. */
final class CatalogRefresh {
    private long fingerprint=Long.MIN_VALUE;
    private boolean dirty=true;
    void accept(long value){dirty|=fingerprint!=value;fingerprint=value;}
    void invalidate(){dirty=true;}
    boolean needsRender(){return dirty;}
    void rendered(){dirty=false;}
}
