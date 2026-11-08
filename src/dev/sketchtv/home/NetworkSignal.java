package dev.sketchtv.home;
final class NetworkSignal {
    static final int OFFLINE=-1,UNKNOWN=-2,ETHERNET=4;
    static int wifiLevel(int rssi){if(rssi<=-127||rssi>=0)return UNKNOWN;if(rssi<=-100)return 0;if(rssi>=-55)return 3;return (rssi+100)*3/45;}
}
