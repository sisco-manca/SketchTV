# Dal disegno alla Home

1. Prepara uno sketch 16:9 con margini, riquadri ed etichette. Può essere un wireframe o un'immagine di riferimento.
2. Avvia `python3 tools/preview.py` e apri l'indirizzo locale indicato.
3. Carica lo sketch nella sezione «Sovrapponi il tuo sketch». L'immagine resta nel browser: non viene inviata al server o salvata nel progetto.
4. Seleziona un riquadro con le frecce o un clic, modifica etichetta/X/Y/larghezza/altezza e premi «Applica al progetto». Il clic simula anche l'azione, con una finestra esplicativa. Per aggiungere/rimuovere riquadri, cambiare colori, app o provider modifica il JSON.
5. Correggi gli errori indicati: il validatore blocca l'esportazione se ci sono sovrapposizioni, margini insufficienti, azioni sconosciute o widget configurati in modo incompleto. L'ultima anteprima valida resta visibile.
6. Esporta il JSON e sostituisci `project.json` nel checkout, oppure indica quel file con `PROJECT=... sh build.sh`.
7. Verifica la Home su un dispositivo di prova. Geometria e focus derivano dallo stesso file, ma rasterizzazione dei font, metriche native e prestazioni Android non sono riprodotti esattamente dal browser.

Le immagini opzionali vanno in `assets/` e nel campo `asset` del riquadro. Servile nello studio con nomi PNG semplici, senza percorsi. Il renderer mantiene le proporzioni; un'immagine mancante mostra l'etichetta. Limita dimensioni e peso e usa soltanto asset di cui possiedi i diritti necessari.

Lo studio non è ancora un editor drag-and-drop o un sistema di riconoscimento dello sketch. Serve a confrontare proporzioni e rendere verificabile il passaggio fra disegno e APK.
