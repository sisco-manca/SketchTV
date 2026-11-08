package dev.sketchtv.home;
public class NetworkSignalTest{
 public static void main(String[]a){
  int[][] cases={{-127,-2},{-200,-2},{0,-2},{20,-2},{-126,0},{-100,0},{-85,1},{-70,2},{-55,3},{-30,3}};
  for(int[] c:cases)if(NetworkSignal.wifiLevel(c[0])!=c[1])throw new AssertionError("RSSI "+c[0]);
  int previous=-1;for(int r=-126;r<0;r++){int n=NetworkSignal.wifiLevel(r);if(n<0||n>3||n<previous)throw new AssertionError("Non-monotonic signal");previous=n;}
  System.out.println("PASS: livelli RSSI, segnale sconosciuto e monotonicita su 126 valori.");
 }
}
