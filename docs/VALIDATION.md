# Verifica

La build generalizzata deve essere verificata sul proprio dispositivo. Il superamento della compilazione non prova l'avvio su ogni TV.

- Navigare con tutte le frecce, OK, Back, Home e Menu. Controllare focus visibile, nessun riquadro tagliato e tutto in una pagina.
- Aprire ogni app principale; installare un'app di prova da Play e verificare aggiornamento del catalogo. Verificare anche rimozione e app prive di attività di lancio.
- Aprire impostazioni e rete; verificare Wi-Fi con segnale debole/forte, sconosciuto, offline ed Ethernet.
- Aprire ogni sorgente realmente disponibile; una sorgente non esposta dal firmware deve dare una spiegazione e consentire il telecomando.
- Confrontare widget con i provider, controllare fonte e data nei dettagli; riprovare offline e con risposte mancanti. Nessun risultato inventato.
- Prima di attivare Accessibilità verificare la Home originale e il percorso di ripristino; poi verificare Home da app, sorgenti e standby. Controllare che una riconnessione del servizio non interrompa un video.
- Per le prestazioni ripetere lo stesso percorso più volte a parità di condizioni, annotare mediana dei tempi, cache fredda/calda e avvio/standby distinti. Usare `adb shell dumpsys gfxinfo dev.sketchtv.home` e `adb shell dumpsys meminfo dev.sketchtv.home` come supporto. Più MemFree da solo non dimostra maggiore fluidità.
- Riavviare soltanto dopo una decisione esplicita, verificare l'avvio normale e infine spegnere il debugging.

Verifiche locali disponibili: build/signature Android SDK 34, rettangoli non sovrapposti e RSSI monotono con gestione di segnale non disponibile. Ulteriori test del firmware e dei servizi remoti restano necessari.
