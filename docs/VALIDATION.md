# Verifica

## Controlli locali

```sh
python3 tools/check.py
sh build.sh
```

Test parser JVM: usa `org.json:json:20240303`, SHA-256 `3cf6cd6892e32e2b4c1c39e0f52f5248a2f5b37646fdfbb79a66b46b618414ed`, scaricato da Maven Central. Non serve nell'APK, che usa org.json di Android.

```sh
JSON_JAR=/percorso/json-20240303.jar sh tests/test_parsers.sh
```

Il browser test richiede Playwright Python e Chromium. In un ambiente di sviluppo con questi strumenti già installati:

```sh
CHROMIUM=/percorso/chromium python3 tests/test_preview.py
```

Senza `CHROMIUM` usa il Chromium gestito da Playwright. La CI installa gli strumenti ed esegue i controlli. Il test avvia un server locale temporaneo e controlla rendering, frecce, modifica/esportazione, configurazioni errate, etichette trattate come testo, schermi stretti, assenza di richieste esterne e blocco di file non autorizzati. `PREVIEW_SCREENSHOT` facoltativo salva un'immagine locale da non aggiungere a Git se contiene personalizzazioni.

Le fixture di test sono inventate. Non utilizzare log, inventari o misure di una TV personale per i test condivisi.

## Checklist sul dispositivo

La compilazione e l'anteprima non provano l'avvio su ogni firmware. Questa build generalizzata richiede il collaudo sul proprio dispositivo.

- Navigare con frecce, OK, Back, Home e Menu. Controllare focus visibile, leggibilità, tutti i riquadri raggiungibili e assenza di bordi tagliati.
- Aprire ogni app; installare e rimuovere un'app di prova e verificare il catalogo. Controllare le app prive di attività di lancio.
- Aprire impostazioni e rete; verificare Wi-Fi con segnale debole/forte, sconosciuto, offline ed Ethernet.
- Aprire ogni sorgente disponibile; quelle non esposte dal firmware devono spiegare il limite e consentire il telecomando.
- Confrontare widget con i provider, controllare fonte, data e stagioni; riprovare offline e con risposte mancanti. Nessun risultato inventato.
- Prima di attivare Accessibilità verificare Home originale e ripristino; poi verificare il ritorno da app, sorgenti e standby. Una riconnessione del servizio non deve interrompere un video.
- Misurare ripetutamente lo stesso percorso a parità di condizioni: distinguere cache fredda/calda, ripresa/avvio e standby/riavvio; riportare mediana e intervallo dei tempi. Usare `adb shell dumpsys gfxinfo dev.sketchtv.home` e `adb shell dumpsys meminfo dev.sketchtv.home` come supporto. MemFree da solo non dimostra fluidità.
- Riavviare dopo una decisione esplicita, verificare l'avvio normale e infine spegnere il debugging.

I test di `tools/device.py` usano soltanto ADB simulato: controllano consenso separato, registro prima della scrittura, valori null, crash recovery, drift, dispositivo errato, dati privati e ripristino idempotente. Non si apre una connessione reale eseguendo il checker.
