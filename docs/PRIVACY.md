# Privacy

Questo repository è una base neutra: non include IP, nomi di utenti, modello, inventario app, log, backup, misure o credenziali di dispositivi personali. Non contiene lo storico Git del progetto di lavoro.

La Home legge le app rese visibili da Android per mostrarne l'elenco e i metadati di rete per l'indicatore locale. Non invia l'elenco app a server. Quando abiliti i widget, contatta i provider specificati in ReferenceData: essi ricevono l'indirizzo IP pubblico e i parametri richiesti, incluse le coordinate configurate per il meteo. Le risposte sono conservate nelle preferenze private dell'app. Non sono aggiunti analytics.

Il servizio Accessibilità è facoltativo e limitato agli eventi di cambio finestra dei launcher Google indicati in res/xml/home_button.xml. Non richiede lettura del contenuto dello schermo né intercettazione dei tasti. Aprire la Home originale dal menu consente di sospendere il reindirizzamento. Verificare il comportamento sul firmware interessato.

Non pubblicare keystore, APK personali, log ADB o configurazioni con dati privati. Per condividere una personalizzazione usa esempi inventati e una chiave nuova. Dopo l'installazione spegni debugging USB, di rete e wireless: la Home funziona senza un computer collegato.
