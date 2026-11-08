# Configurazione

La fonte unica è `project.json`. La build genera `Config.java` e `LayoutSpec.java` in `out/gen`: non modificarli. Per controllare un progetto senza compilare:

```sh
python3 tools/configure.py --check
python3 tools/configure.py altro-progetto.json --check
```

## Schermata e tema

`canvas` usa 1920×1080 come coordinate di progetto, ridimensionate uniformemente sul display. `safeMargin` (8–120 pixel di progetto) protegge i bordi. Fra i riquadri servono almeno 12 pixel per il focus; i rettangoli non possono sovrapporsi.

`theme` contiene background/backgroundEnd, text/muted, card/cardText, accent, radius (0–60), textScale (0.75–1.4). Colori in formato `#RRGGBB`. Verificare contrasto e leggibilità prima di installare. Il font locale è Poppins Regular/Bold.

`clock` contiene un fuso IANA e una locale, per esempio `UTC` e `it-IT`. Lo studio e Android usano entrambi il fuso indicato.

## Riquadri

Ogni elemento di `tiles` contiene:

| Campo | Significato |
| --- | --- |
| id | Nome unico, lettere minuscole, numeri, trattino o underscore |
| kind | clock, widget, app, source o control |
| label | Etichetta visibile/accessibile |
| rect | `[x, y, larghezza, altezza]`, minimi per contenuto: clock 400×240, widget 280×260, app 120×90, source 180×80, control 100×70 |
| action | Azione da eseguire quando si preme OK |
| color | Colore di sfondo opzionale |
| asset | Nome PNG opzionale presente in assets/, senza percorsi |

Azioni: `apps`, `tv`, `av`, `hdmi1`, `hdmi2`, `sources`, `settings`, `network`, `app:PACKAGE` o `widget:ID`. `app:` vuoto crea un segnaposto che spiega come configurarlo. Sono supportati da 1 a 40 riquadri; non è necessario mantenerne 15 o conservare un ordine particolare. Il riquadro clock è progettato per il widget weather.

Il generatore calcola vicini sinistra/su/destra/giù favorendo i riquadri allineati, poi la distanza. Se non esiste un vicino nella direzione richiesta, il focus resta sul riquadro corrente. Verificare sempre il percorso nello studio e sul televisore.

## Widget

Le configurazioni `weather`, `f1` e `football` includono `enabled` e `refreshMinutes` (10–1440 minuti). I provider disabilitati non fanno richieste neppure con aggiornamento manuale.

- weather: city, latitude (−90..90), longitude (−180..180).
- f1: constructorId del provider, name di visualizzazione.
- football: teamId e leagueId del provider, name di visualizzazione.

Non inserire nome e ID nello stesso campo: gli ID sono valori pubblicati dai provider, non dedotti da un nome. Configurare gli ID prima di abilitare un widget. La cache è separata per provider ed endpoint, quindi cambiare località o squadra non riutilizza risposte della precedente configurazione.

Il JSON viene incorporato nella logica generata dell'APK: non è una posizione sicura per segreti. Il file iniziale non contiene preferenze personali. Non caricare in Git configurazioni contenenti dati che non desideri condividere.

La build include solo gli asset referenziati e font/licenza: PNG massimo 4 MiB e 8192 pixel per lato, totale selezionato 12 MiB. La Home impone inoltre un budget di 4 MiB per i bitmap decodificati. Se un’immagine manca o supera il budget disponibile viene mostrata l’etichetta.
