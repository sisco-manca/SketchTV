# Sketch TV

**Dal tuo sketch a una Home Android TV personale, semplice da navigare.**

Una base Android nativa per trasformare un disegno, wireframe o riferimento grafico in una Home a schermo unico, utilizzabile con il telecomando. L'obiettivo è rendere rapida la personalizzazione e piacevole la navigazione, con widget scelti da chi realizza l'interfaccia e fonti sempre dichiarate.

## Cosa funziona oggi

- Layout nativo Canvas, Poppins, bersagli di focus e clic Android, senza WebView.
- Quattro scorciatoie configurabili e «Altre app», aggiornato quando si installano o rimuovono applicazioni visibili al launcher.
- Impostazioni TV, rete e indicatore dinamico Wi-Fi/Ethernet; nessuna scansione Wi-Fi.
- Sorgenti tramite il framework Android TV, con fallback al telecomando quando il firmware non espone un ingresso.
- Adattatori per meteo Open-Meteo, Formula 1 Jolpica ed eventi/classifiche calcistiche ESPN: cache locale, richieste fuori dal thread grafico, aggiornamento ogni dieci minuti quando la Home è visibile.
- Nome della fonte, URL e data di download nei dettagli dei widget; errori e dati salvati segnalati. Dati sportivi non in diretta.
- Servizio Accessibilità facoltativo per alcuni launcher Google; non disabilita la Home originale.

La build iniziale è **neutra**: nessuna località, squadra o app preferita impostata; i widget di rete sono disabilitati finché non li configuri. Non contiene configurazioni o dati di un televisore personale.

## Dallo sketch alla TV

1. Disegna una schermata 16:9, indicando riquadri, spazi, focus del telecomando e azione di ogni elemento.
2. Segui [la guida allo sketch](docs/SKETCH.md) per riportare il disegno nelle coordinate del layout e collegare le azioni.
3. Modifica `src/dev/sketchtv/home/Config.java`: app, etichette, fuso orario, località e identificativi dei provider. Mantieni quattro package e quattro etichette, nello stesso ordine.
4. Aggiungi eventuali immagini di cui hai diritto d'uso in `assets/`. I loghi non sono necessari: senza immagini compaiono le etichette.
5. Compila, installa con conferma e verifica su un dispositivo di prova secondo [installazione e ripristino](docs/INSTALL.md).

**Non è ancora un convertitore automatico immagine → APK né un editor visuale.** La trasformazione dello sketch richiede interventi sul codice; un assistente di sviluppo può aiutare. La scelta dei widget avviene oggi modificando il codice, non da un catalogo nella TV.

## Compilazione

Servono JDK 17 e Android SDK con `platforms;android-34` e `build-tools;34.0.0`.

```sh
export JAVA_HOME="/percorso/jdk17"
export ANDROID_HOME="/percorso/android-sdk"
sh build.sh
```

APK: `out/tvhome.apk`. Al primo build viene generata una chiave locale `key.jks`: conservarla per aggiornare la propria installazione, senza pubblicarla. Per una distribuzione usa una chiave dedicata e `KEYSTORE` / `KEYSTORE_PASS`. La password predefinita di sviluppo non è adatta a una chiave di produzione.

```sh
python3 tests/check_layout.py
sh tests/test_network.sh
```

## Fluidità e funzionalità

Il codice conserva la UI, limita i bitmap, carica soltanto le icone della pagina corrente e sposta rete/cache/enumerazione ingressi fuori dal thread grafico. Questo riduce lavoro inutile; non dimostra da solo un aumento di velocità o RAM libera. Misurare ripresa Home, fotogrammi lenti e memoria sul dispositivo di destinazione. L'esperienza del progetto di origine non è una certificazione di questa build generalizzata.

La «perfetta funzionalità» è un criterio di qualità da verificare: firmware, ingresso AV/HDMI, permessi, sorgenti dati e pulsante Home possono comportarsi diversamente. Non c'è garanzia universale. Vedi [la checklist di verifica](docs/VALIDATION.md).

## Fonti, privacy e licenze

[Fonti dei widget e loro limiti](docs/SOURCES.md) · [Privacy](docs/PRIVACY.md) · [Attribuzioni](NOTICE.md).

Derivato da [Ansub/tv-detox](https://github.com/Ansub/tv-detox), commit `b22a1e368ee56d670d11115d8408e9025488240b`, con licenza MIT conservata. Poppins è incluso con licenza SIL OFL. Marchi e contenuti dei provider restano dei rispettivi titolari.

## Sviluppi successivi

- Editor di layout e importazione assistita degli sketch.
- Catalogo di widget con contratti per fonti, cache, errori e aggiornamenti.
- Configurazione da interfaccia e anteprima con navigazione telecomando.
- Matrice di compatibilità e benchmark ripetibili su più firmware.
