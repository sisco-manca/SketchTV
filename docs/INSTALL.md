# Installazione e ripristino

Non serve collegare un televisore per compilare. Provare prima su un dispositivo di test. Namespace di questa versione: dev.sketchtv.home; non aggiorna altre applicazioni con identità diversa.

1. Compila e conserva la chiave di firma fuori da Git.
2. Abilita temporaneamente il debugging e autorizza solo il proprio computer. Per il Wi-Fi segui il metodo supportato dal firmware, senza esporre ADB su Internet.
3. Usa `adb devices -l` per trovare il seriale e poi `sh scripts/install.sh SERIAL`. Il comando chiede conferma e installa solo l'APK. Non cambia Home predefinita, app di sistema, animazioni o Accessibilità.
4. Apri Sketch TV dalla lista applicazioni. Per la Home predefinita usa la scelta prevista nelle impostazioni del televisore, annotando prima quella originale.
5. Se il firmware impedisce la scelta, il servizio «Tasto Home · Sketch TV» può essere attivato manualmente in Accessibilità. È opzionale e richiede una decisione separata. Alcuni firmware possono mostrare brevemente la propria Home prima del reindirizzamento. Non disabilitare il launcher di sistema per nasconderlo.
6. Verifica la checklist e spegni tutti i metodi di debugging attivati.

## Tornare indietro

Disattiva soltanto «Tasto Home · Sketch TV» da Accessibilità, scegli la Home originale nelle impostazioni e verifica il tasto Home. Se desideri rimuovere Sketch TV, disinstallala dalla normale schermata app dopo questi passaggi. Gli aggiornamenti richiedono la stessa firma e un versionCode maggiore; una chiave diversa non può aggiornare un'installazione esistente.

Gli script non disabilitano né eliminano altre app. Non alterare Google Play services, Play Store, impostazioni, System UI, setup, tastiera, Bluetooth, framework TV, ingressi, launcher di sistema o componenti sconosciuti. Nessuno script di pulizia del dispositivo di origine è incluso.

L’ottimizzazione opzionale del dispositivo è separata dall’installazione: vedi DEVICE.md. Non comporta alcuna disabilitazione di pacchetti.
