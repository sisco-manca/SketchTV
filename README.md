# Sketch TV

**Dal tuo sketch a una Home Android TV personale, semplice da navigare.**

Sketch TV è una base Android nativa con uno studio locale: confronta il tuo disegno con l'anteprima, modifica i riquadri, prova il telecomando ed esporta un progetto pronto per la compilazione.

## Prova lo studio

Servono Python 3.9+ e un browser moderno. Non serve collegare un televisore.

```sh
python3 tools/preview.py
```

Apri **http://127.0.0.1:8765**. Puoi:

- sovrapporre uno sketch, conservato nel browser, e regolarne l'opacità;
- selezionare un riquadro e cambiare etichetta, posizione e dimensioni;
- modificare tema, azioni e widget nel JSON;
- provare frecce, OK e percorsi di focus;
- importare/esportare `project.json`, dopo la validazione.

Sostituisci il `project.json` del progetto con quello esportato. Il browser non scrive automaticamente i tuoi file. Lo studio usa dati dimostrativi indicati come tali; non apre app, non contatta provider e non misura prestazioni Android.

## Un progetto, una geometria

`project.json` definisce **riquadri, azioni, colori, dimensione del testo, orologio e widget**. Il validatore genera le classi Java in fase di build. Lo stesso rettangolo guida disegno e bersaglio cliccabile; i collegamenti di focus sono generati dalla posizione dei riquadri. Nessuna coordinata duplicata da mantenere a mano.

La configurazione iniziale è neutra: nessuna località, squadra o app preferita; widget remoti disabilitati. Non sono inclusi log, backup, dati o asset di un televisore personale.

[Configurazione e personalizzazione](docs/CONFIGURATION.md) · [Dal disegno alla Home](docs/SKETCH.md)

## Widget con provenienza

Ogni provider dichiara fonti HTTPS, attribuzione, intervallo di aggiornamento e parser. Un motore comune gestisce cache, aggiornamenti, errori e tentativi successivi. Aggiungere un provider non richiede cambiare renderer o Activity.

Adattatori inclusi:

| Widget | Fonte | Configurazione richiesta |
| --- | --- | --- |
| Meteo | Open-Meteo, CC BY 4.0 | Nome località, coordinate |
| Formula 1 | Jolpica F1 | Identificativo costruttore |
| Calcio | ESPN | Identificativi squadra e campionato |

Ogni riquadro mostra la fonte; i dettagli mostrano endpoint, condizioni dichiarate, data di download e stato dei dati. Gli errori conservano gli ultimi dati validi, segnalati come salvati. Risultati e classifica F1 di gare diverse o dati calcistici di stagioni diverse non vengono combinati. Non sono risultati in diretta.

[Fonti e limiti](docs/SOURCES.md) · [Aggiungere widget](docs/WIDGETS.md)

## Navigazione Android

- Home nativa Canvas, Poppins, focus e clic gestiti da Android, senza WebView.
- Numero e ordine delle scorciatoie definiti nel progetto; «Altre app» si aggiorna con installazioni/rimozioni visibili al launcher.
- Impostazioni, rete e indicatore Wi-Fi/Ethernet senza scansioni.
- Sorgenti tramite il framework TV, con spiegazione e fallback al telecomando se un ingresso non è esposto.
- Servizio Accessibilità facoltativo per alcuni launcher Google, senza disabilitare la Home originale.

Nessuna conversione automatica immagine → interfaccia: lo sketch è un riferimento sovrapponibile e il layout si definisce nello studio. I tipi di widget disponibili si configurano nel JSON; nuovi tipi richiedono un adattatore in codice.

## Compilazione

Servono Python 3.9+, JDK 17 e Android SDK con `platforms;android-34` e `build-tools;34.0.0`.

```sh
export JAVA_HOME="/percorso/jdk17"
export ANDROID_HOME="/percorso/android-sdk"
sh build.sh
```

In alternativa: `PROJECT=altro-progetto.json sh build.sh`. La configurazione viene validata prima di sostituire l'output della build. APK: `out/tvhome.apk`.

La prima build genera `key.jks`: conservala privatamente per i tuoi aggiornamenti. Per distribuire una build usa una chiave dedicata e `KEYSTORE` / `KEYSTORE_PASS`; la password predefinita di sviluppo non è adatta a una chiave di produzione. Nessuna chiave o APK è incluso in Git.

[Installazione e ripristino](docs/INSTALL.md) · [Privacy](docs/PRIVACY.md)

## Qualità e fluidità

Rendering conservato, bitmap limitati, rete/cache/ingressi fuori dal thread grafico e nessuna ricostruzione della schermata al ritorno dalle app. Gli aggiornamenti dei widget avvengono soltanto con Home visibile e rispettano intervalli indipendenti.

La fluidità è un obiettivo da misurare. Non promettiamo maggiore RAM libera né compatibilità universale: avvio, sorgenti e Home dipendono anche dal firmware. L'anteprima browser non sostituisce un collaudo Android.

```sh
python3 tests/test_config.py
python3 tests/check_layout.py
python3 tests/check_privacy.py
sh tests/test_network.sh
sh tests/test_widgets.sh
```

[Test dei parser, browser e checklist dispositivo](docs/VALIDATION.md). La CI compila e verifica la firma, la configurazione, cache/errori, parser con dati inventati e lo studio nel browser.

## Origine e licenze

Derivato da [Ansub/tv-detox](https://github.com/Ansub/tv-detox), commit `b22a1e368ee56d670d11115d8408e9025488240b`, con licenza MIT conservata. Poppins è incluso con licenza SIL OFL. [Attribuzioni](NOTICE.md). Marchi e dati dei provider restano dei rispettivi titolari.

Prossimi sviluppi: trascinamento e ridimensionamento visuale dei riquadri, catalogo estensibile da interfaccia e benchmark su diversi firmware.
