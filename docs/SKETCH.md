# Dal disegno al layout

Usa una tavola 1920×1080 e lascia un margine sicuro attorno ai contenuti. `ReferenceHome.BOXES` definisce i rettangoli come sinistra, alto, destra, basso; il disegno è ridimensionato uniformemente e centrato sul display. Non cambiare l'ordine degli indici senza aggiornare anche `MainActivity.open` e le etichette di accessibilità.

Indici: 0 orologio/meteo, 1 F1, 2 calcio, 3–6 app, 7 catalogo, 8 TV, 9 AV, 10–11 HDMI, 12 altre sorgenti, 13 impostazioni, 14 rete. Aggiorna sia le coordinate dei bersagli sia le coordinate del disegno Canvas quando sposti un elemento. Verifica che il bordo di focus non venga tagliato.

Per modificare il tema cambia shader/colori e gerarchie tipografiche in ReferenceHome. Font locali, niente download al rendering. I nomi asset opzionali sono netflix.png, prime.png, youtube.png e spotify.png per compatibilità con il renderer: sostituiscili o rinominali nel codice quando scegli altre app. Nessun logo di terzi è incluso. Mantieni le immagini proporzionate e leggere.

I provider attuali sono adattatori specifici: ReferenceData contiene URL e parser, MainActivity cache e finestre dettagli, ReferenceHome rappresentazione. Un nuovo tipo di widget richiede modificare tutti e tre, documentare fonte/licenza, trattare dati assenti e aggiungere verifiche del parser. Non inserire credenziali nell'APK o nel repository.

Config.java: inserisci coordinate numeriche e nome di una località, fuso IANA, ID costruttore Jolpica, ID squadra e campionato ESPN; abilita soltanto gli adattatori configurati. I campi sono stringhe Java, non un editor con validazione: usa identificativi validi dei provider. Per ripartire senza cache disinstalla solo la propria build di prova o cancella i suoi dati dalle impostazioni, dopo aver valutato cosa si perde.
