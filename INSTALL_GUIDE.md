# Installation & setup / Instalación y configuración

Android Video Info Overlay is experimental. Menu names and debugging options vary by device.
Android Video Info Overlay es experimental. Los menús y las opciones de depuración varían según el dispositivo.

## English

### Install and configure on the TV (no computer commands)

Guided setup is included starting with **v21.2**. Download the
[latest APK](https://github.com/signde/android-video-overlay/releases/latest/download/Android_VideoInfoOverlay.apk),
or choose a version from [Releases](https://github.com/signde/android-video-overlay/releases).
See the [README](README.md#supported-devices) for device compatibility. Available
metrics vary by device and app; support does not mean every field is available.

**Downloader:** enter code **`3517057`** in its address field, or use
[aftv.news/3517057](https://aftv.news/3517057). Both point to the latest published
APK, so the code does not change for each release. Direct download destination:

```text
https://github.com/signde/android-video-overlay/releases/latest/download/Android_VideoInfoOverlay.apk
```

1. **Install the APK.** Download it with Downloader, or transfer it to the TV and
   open it with a file manager. If Android asks, allow that installer to install
   unknown apps. You can turn that installer permission off again afterward.
   To update an existing installation, install the new APK over it; do not
   uninstall first. Existing settings and authorizations should be retained.
2. **Enable debugging.** Open **Settings > System > About** and press **Android TV
   OS build** seven times. In **Developer options**, enable **USB debugging**
   (**Network debugging** on some devices).
   Menu names may vary. The app requires local ADB on **port 5555**; enabling or
   pairing **Wireless debugging** alone is not sufficient.
3. **Open Android Video Info Overlay.** On its setup screen, select **2. Authorize
   local ADB and allow restricted settings**, read the explanation, then select
   **Continue**. This authorizes the app to request its own restricted-settings
   permission. It does not enable Accessibility or disable other services.
4. **Approve the TV's debugging prompt.** Select **Always allow from this computer**
   and **Allow** in **Allow USB debugging?** Here, the connection is from the app
   on this TV, not another computer. Only approve it if you trust this APK: ADB
   gives the app powerful shell access. Keep debugging enabled only on a trusted
   network. Wait for **Restricted settings allowed and verified** in the app.
5. **Enable Accessibility.** Select **3. Open Accessibility settings**, enable
   **Android Video Info Overlay**, and accept Android's confirmation. Other
   accessibility services can remain enabled. Press **Back** to return to setup.
6. **Show the overlay.** Wait for **Accessibility service connected. Ready to show
   the overlay**, then select **4. Show overlay and finish**. Opening the app from
   the apps list subsequently shows or hides the overlay when the service is
   connected; otherwise it opens setup. Remote-button behavior varies by device.
   Use the apps-list shortcut if Favorites / Bookmark is not assigned.

**Android 11/12:** restricted-settings permission does not exist on these versions.
Version 21.2.1 and later label step 2 **Authorize local ADB** and
verify the connection without requesting that permission. Continue to
Accessibility after **Local ADB connected and verified**. If v21.2 reports
**Unknown operation string: ACCESS_RESTRICTED_SETTINGS**, the ADB command reached
the TV; skip to step 3 to test Accessibility rather than repeatedly retrying the
unsupported command. This does not guarantee playback-metric support on that device.

To revisit setup, open **Settings > Accessibility > Android Video Info Overlay >
Configuration > Setup / permissions**.
The **Overlay configuration** button at the bottom of setup opens the appearance
and remote-button settings, even before Accessibility is enabled.

**If setup fails:**

- If the app opens without the guided setup and Accessibility is not enabled,
  check that you installed v21.2 or newer. v21.1 does not include guided setup.
- If no debugging prompt appears, or setup times out, check **USB debugging**
  (**Network debugging** on some devices) and
  retry the authorization step. Already-authorized installations may not show
  another prompt. Some devices do not expose local port 5555; report the exact
  error rather than assuming Wireless debugging pairing will fix it.
- If Accessibility switches itself off, return to setup and verify that the
  restricted-settings step succeeded. After deliberately force-stopping the app,
  you may need to enable Accessibility again.
- If Play Protect warns about the APK, review the warning and proceed only if
  you trust this APK. Do not disable Play Protect globally.
- Report the device model, Android build, failing step, and exact message.
  Successful setup does not guarantee that every playback metric is available.

The app uses an accessibility overlay. It does not need a separate **Display over
other apps** permission, root, or any other helper.

### Remote-button setup

The default toggle is **Favorites / Bookmark** (including the onn star button
where present). Not every remote has this button.

1. Open the TV's **Settings > Accessibility > Android Video Info Overlay >
   Configuration**. Accessibility may be under **System** on your TV.
   If you are already on the app's setup screen, select **Overlay configuration**.
2. Select **Overlay > Button for remote control**.
3. Choose **Favorites / Bookmark**, **TV Guide**, a colored button, or a digit
   **0–9** that your remote actually provides. **None** disables direct key handling.
4. Return to playback and press the selected button. **Long button press** is off
   by default; if enabled, hold the selected button for at least 750 ms, then release.

The current version uses a fixed list; it cannot learn an arbitrary button by
asking you to press it. Selecting a button works only if its key event reaches
the overlay service. A label on a remote does not guarantee that it sends the
matching event.

If none of the choices work, open **Android Video Info Overlay** from the apps
list to toggle it, or use **Configuration > Show / hide overlay**. These require
the accessibility service to be connected. If your device's shortcut settings or
an existing button-mapping app can launch an app, assign **Android Video Info
Overlay** as the target. This uses the app-launch toggle rather than its fixed
key list; availability depends on the device and mapper. Opening the app shows
setup instead if the accessibility service is disconnected.

### Computer-assisted fallback

The remaining English instructions are for computer-assisted troubleshooting or
older APKs. **Skip them if guided setup succeeded.** A computer connection
can grant restricted-settings access, but does not remove the overlay's requirement
for local ADB on port 5555.

### Before you begin

Download the APK from [Releases](https://github.com/signde/android-video-overlay/releases).
You can install it with Downloader, a file manager, or ADB. Installing the APK does
not automatically authorize Accessibility or the app's local ADB connection.

For computer commands, use current [Android SDK Platform Tools](https://developer.android.com/tools/releases/platform-tools).
Replace placeholders such as `TV_IP`, `PAIRING_PORT`, and `DEVICE_ID` with your own values;
do not type them literally.

### 1. Choose an ADB connection method

Enable Developer options by opening **Settings > System > About** and pressing
**Android TV OS build** seven times. Keep the computer and TV on the same local network.

These are two different connection methods. Their ports and authorization steps
are not interchangeable.

#### Port 5555: the connection used by the overlay

On the tested onn devices, enable **USB debugging** in Developer options, then run:

```sh
adb connect TV_IP:5555
adb devices -l
```

Watch the TV for **Allow USB debugging?** and approve your trusted computer.
The wording can say USB even when connecting over the network. Select **Always
allow from this computer** if appropriate.

The TV entry must show `device`, not `unauthorized` or `offline`.
Enabling USB debugging does not expose port 5555 on every Android TV device.

#### Wireless debugging: pairing and a separate connection port

If using **Wireless debugging**, open **Pair device with pairing code** on the TV:

```sh
adb pair TV_IP:PAIRING_PORT
```

Enter the displayed pairing code when prompted. Then return to the main
**Wireless debugging** screen and use its **IP address & port**, not the port
from the pairing dialog:

```sh
adb connect TV_IP:CONNECTION_PORT
adb devices -l
```

Pairing may connect automatically. The connection port can change, and neither
wireless-debugging port should be assumed to be 5555.

This connection can run the setup commands below, but the overlay currently
requires a separate, non-paired local ADB connection on **port 5555** for its metrics.
Wireless pairing alone does not configure that connection.

### 2. Verify the connection

In `adb devices -l`, copy the TV's exact identifier from the first column.
Use that as `DEVICE_ID` below, whether it looks like `192.168.1.50:5555`
or a discovery-generated name:

```sh
adb -s DEVICE_ID shell echo connected
```

Continue only when this prints `connected`.

- **Daemon started successfully:** ADB's server started on the computer; this is not TV authorization.
- **unauthorized:** For the 5555 method, accept the TV's debugging authorization prompt.
  If it does not appear, disconnect that endpoint with `adb disconnect TV_IP:5555`,
  then reconnect while watching the TV.
- **offline / device not found / no devices:** Run `adb devices -l` again and check
  the address, current connection port, and exact identifier. For Wireless debugging,
  confirm pairing completed and do not use the pairing port for `adb connect`.
- **More than one device:** Keep using `-s DEVICE_ID` to select the TV.

If still stuck, share the exact commands, their full output, and `adb devices -l`.
Do not share pairing codes. A listed device is not necessarily authorized.

### 3. Install or allow Accessibility

#### Option A: Install through ADB

Skip this if the APK is already installed. Adjust the filename for the release downloaded:

```sh
adb -s DEVICE_ID install -r Android_VideoInfoOverlay.apk
```

Then enable Accessibility as described in step 4. If the toggle immediately
switches off, try Option B.

#### Option B: APK already installed, but Accessibility will not stay enabled

Android's restricted-settings protection can block Accessibility for sideloaded apps.
On some TV interfaces this may appear as a toggle switching off without a useful
warning. It is a possible cause, not proof that every failed toggle has this cause.

If available, open the app's system **App info > Allow restricted settings**
(possibly in a three-dot menu). Only allow this for an APK you trust.

If that option is missing, after verifying your ADB connection in step 2, run:

```sh
adb -s DEVICE_ID shell appops set com.signde.androidvideooverlay ACCESS_RESTRICTED_SETTINGS allow
```

This permits restricted settings for this app only. It does not enable Accessibility
itself or replace the list of other enabled accessibility services.
If the command succeeds but the toggle still switches off, report the APK version,
device/build, installation method, and exact output so we can investigate.

### 4. Enable and use the overlay

1. Open **Settings > Accessibility** (sometimes under **System**) and enable
   **Android Video Info Overlay**. Press Back to fully close the setup screen.
2. Leave debugging enabled. When opening the overlay, approve the app's own local
   ADB connection on the TV. This authorization is separate from your computer's.
   The app needs port 5555; no root or any other helper is needed.
3. Press **Favorites / Bookmark**, or open **Android Video Info Overlay** from the apps
   list, to show or hide it. On Google TV Streamer, you can assign the customizable
   remote button to open the app in **Settings > Remotes & accessories**.

For appearance and remote-button settings, open **Settings > Accessibility >
Android Video Info Overlay > Configuration**.

The app uses an accessibility overlay, so it is not expected to appear under
**Display over other apps**.

References: [ADB connection and authorization](https://developer.android.com/tools/adb),
[wireless ADB pairing](https://android.googlesource.com/platform/packages/modules/adb/+/HEAD/docs/dev/adb_wifi.md),
[Android restricted settings](https://support.google.com/android/answer/12623953?hl=en).

## Español

### Instalar y configurar desde el TV (sin comandos de ordenador)

La configuración guiada está incluida desde **v21.2**. Descarga el
[APK más reciente](https://github.com/signde/android-video-overlay/releases/latest/download/Android_VideoInfoOverlay.apk)
o elige una versión en [Releases](https://github.com/signde/android-video-overlay/releases).
Consulta la compatibilidad de dispositivos en el [README](README.md#supported-devices).
Las métricas disponibles dependen del dispositivo y la aplicación; no se garantiza
que aparezcan todos los campos. Los botones de configuración aparecen en inglés.

**Downloader:** introduce el código **`3517057`** en el campo de dirección, o usa
[aftv.news/3517057](https://aftv.news/3517057). Ambos apuntan al APK publicado más
reciente; el código no cambia con cada versión. Destino de descarga directa:

```text
https://github.com/signde/android-video-overlay/releases/latest/download/Android_VideoInfoOverlay.apk
```

1. **Instala el APK** con Downloader o un gestor de archivos en el TV. Si Android
   lo solicita, permite que ese instalador instale aplicaciones desconocidas.
   Puedes desactivar ese permiso después. Para actualizar, instala el nuevo APK
   sobre el existente, sin desinstalar primero; los ajustes y las autorizaciones
   deberían conservarse.
2. **Activa la depuración.** En **Ajustes > Sistema > Información**, pulsa siete
   veces **Compilación del SO de Android TV**. En las opciones para desarrolladores,
   activa **Depuración por USB** (**Depuración de red** en algunos dispositivos).
   La aplicación necesita ADB local en el **puerto 5555**;
   activar o emparejar Depuración inalámbrica no basta.
3. **Abre Android Video Info Overlay** y selecciona **2. Authorize local ADB and
   allow restricted settings**. Lee la explicación y pulsa **Continue**. La app
   solicitará su propio permiso de ajustes restringidos; no activará Accesibilidad
   ni desactivará otros servicios.
4. En **¿Permitir depuración por USB?**, marca **Permitir siempre desde este
   ordenador** y acepta. La conexión procede de la propia aplicación en este TV,
   no de otro ordenador. Autorízala solo si confías en el APK: ADB proporciona
   acceso potente al sistema. Mantén la depuración activada solo en una red de
   confianza. Espera el mensaje **Restricted settings allowed and verified**.
5. Selecciona **3. Open Accessibility settings**, activa **Android Video Info Overlay**
   y acepta la confirmación. Los demás servicios pueden permanecer activados.
   Pulsa **Atrás** para volver a la configuración de la aplicación.
6. Espera **Accessibility service connected. Ready to show the overlay** y pulsa
   **4. Show overlay and finish**. Después, abrir la aplicación desde la lista de
   apps muestra u oculta el overlay si el servicio está conectado; de lo contrario,
   abre la configuración inicial. Usa este acceso si Favoritos / Bookmark no está
   asignado; el comportamiento del botón del mando depende del dispositivo.

**Android 11/12:** el permiso de ajustes restringidos no existe en estas versiones.
La versión 21.2.1 y posteriores muestran **Authorize local ADB** en el paso 2
y verifican la conexión sin solicitar ese permiso. Continúa con Accesibilidad tras
**Local ADB connected and verified**. Si v21.2 muestra **Unknown operation string:
ACCESS_RESTRICTED_SETTINGS**, el comando ADB llegó al TV; pasa al paso 3 para probar
Accesibilidad en lugar de repetir el comando no compatible. Esto no garantiza que
las métricas de reproducción funcionen en ese dispositivo.

Para repetir la configuración, abre **Ajustes > Accesibilidad > Android
Video Info Overlay > Configuración > Setup / permissions**.
El botón **Overlay configuration**, al final de la pantalla de configuración
inicial, abre los ajustes de apariencia y del mando, incluso sin Accesibilidad activada.

**Si falla:** comprueba que instalaste v21.2 o posterior y que Depuración por USB
(Depuración de red en algunos dispositivos) está activada. Reintenta la autorización
si no aparece el aviso o se agota el
tiempo; una instalación ya autorizada puede no mostrar otro aviso. Algunos
dispositivos no ofrecen ADB local en el puerto 5555. Si Accesibilidad se desactiva,
comprueba que la verificación de ajustes restringidos haya terminado correctamente.
Tras forzar la detención puede ser necesario volver a activar Accesibilidad.
Si Play Protect muestra una advertencia, revísala y continúa solo si confías en el
APK; no desactives Play Protect globalmente. Comunica el modelo, la compilación
de Android, el paso y el mensaje exacto del error.

La configuración correcta no garantiza todas las métricas de reproducción. No se
necesita el permiso **Mostrar sobre otras aplicaciones**, root ni otra aplicación
auxiliar: se utiliza un overlay de accesibilidad.

### Configurar el botón del mando

El botón predeterminado es **Favorites / Bookmark** (incluido el botón de estrella
de onn cuando está presente). No todos los mandos tienen ese botón.

1. Abre **Ajustes > Accesibilidad > Android Video Info Overlay > Configuración**.
   Accesibilidad puede estar dentro de **Sistema**. Si ya estás en la pantalla
   inicial de la app, selecciona **Overlay configuration**.
2. Selecciona **Overlay > Button for remote control**.
3. Elige **Favorites / Bookmark**, **TV Guide**, un botón de color o un número
   **0–9** que exista en tu mando. **None** desactiva la detección directa de teclas.
4. Vuelve a la reproducción y pulsa el botón elegido. **Long button press** está
   desactivado por defecto; si lo activas, mantén pulsado el botón durante al menos
   750 ms y después suéltalo.

La versión actual ofrece una lista fija; no permite aprender una tecla al pulsarla.
El evento de la tecla debe llegar al servicio del overlay. La etiqueta del botón
no garantiza que el mando envíe el evento correspondiente.

Si ninguna opción funciona, abre **Android Video Info Overlay** desde la lista de
apps para mostrarlo u ocultarlo, o usa **Configuración > Show / hide overlay**.
El servicio de accesibilidad debe estar conectado. Si los ajustes de accesos
directos del dispositivo o una app de reasignación que ya utilizas permiten abrir
una aplicación, asigna **Android Video Info Overlay** como destino. Esto utiliza
el acceso de la app en lugar de la lista fija; depende del dispositivo y del
reasignador. Si el servicio no está conectado, se abre la configuración inicial.

### Alternativa con ordenador

Las instrucciones restantes corresponden a versiones anteriores o a la resolución
de problemas con ordenador. **Omítelas si la configuración guiada funcionó.** La autorización
desde un ordenador no elimina el requisito de ADB local en el puerto 5555.

### Antes de empezar

Descarga el APK desde [Releases](https://github.com/signde/android-video-overlay/releases).
Puedes instalarlo con Downloader, un gestor de archivos o ADB. Instalar el APK no
autoriza automáticamente Accesibilidad ni la conexión ADB local de la aplicación.

Para los comandos desde un ordenador, utiliza
[Android SDK Platform Tools](https://developer.android.com/tools/releases/platform-tools).
Sustituye `TV_IP`, `PAIRING_PORT` y `DEVICE_ID` por tus propios valores;
no escribas estos marcadores literalmente.

### 1. Elige un método de conexión ADB

Abre **Ajustes > Sistema > Información** y pulsa siete veces **Compilación del SO
de Android TV** para activar las opciones para desarrolladores. El ordenador y
el televisor deben estar en la misma red local.

Son dos métodos distintos. Sus puertos y pasos de autorización no son intercambiables.

#### Puerto 5555: la conexión que utiliza el overlay

En los dispositivos onn probados, activa **Depuración por USB** en las opciones
para desarrolladores y ejecuta:

```sh
adb connect TV_IP:5555
adb devices -l
```

Acepta en el televisor **¿Permitir depuración por USB?** para tu ordenador de
confianza. El aviso puede mencionar USB aunque la conexión sea por red.
Marca **Permitir siempre desde este ordenador** si corresponde.

La entrada debe indicar `device`, no `unauthorized` ni `offline`.
Activar la depuración USB no abre el puerto 5555 en todos los dispositivos Android TV.

#### Depuración inalámbrica: emparejamiento y otro puerto de conexión

En **Depuración inalámbrica**, abre **Emparejar dispositivo con código**:

```sh
adb pair TV_IP:PAIRING_PORT
```

Introduce el código mostrado cuando se solicite. Vuelve a la pantalla principal
de **Depuración inalámbrica** y usa su **Dirección IP y puerto**, no el puerto
del cuadro de emparejamiento:

```sh
adb connect TV_IP:CONNECTION_PORT
adb devices -l
```

La conexión puede establecerse automáticamente después del emparejamiento.
El puerto de conexión puede cambiar; no supongas que ninguno de estos puertos es 5555.

Esta conexión permite ejecutar los comandos de configuración siguientes, pero el
overlay necesita una conexión ADB local independiente, sin este emparejamiento,
en el **puerto 5555** para sus métricas. El emparejamiento inalámbrico no la configura.

### 2. Comprueba la conexión

Copia el identificador exacto del televisor de la primera columna de
`adb devices -l`. Úsalo como `DEVICE_ID`, tanto si aparece como
`192.168.1.50:5555` como si es un nombre generado por descubrimiento:

```sh
adb -s DEVICE_ID shell echo connected
```

Continúa solo si imprime `connected`.

- **Daemon started successfully:** Se inició el servidor ADB del ordenador; no significa que el TV lo haya autorizado.
- **unauthorized:** Con el método 5555, acepta el aviso de autorización en el TV.
  Si no aparece, ejecuta `adb disconnect TV_IP:5555` y vuelve a conectar mirando la pantalla.
- **offline / device not found / no devices:** Repite `adb devices -l` y comprueba
  la dirección, el puerto actual y el identificador exacto. Con Depuración inalámbrica,
  confirma el emparejamiento y no uses su puerto de emparejamiento con `adb connect`.
- **Más de un dispositivo:** Usa `-s DEVICE_ID` para seleccionar el televisor.

Si el problema continúa, comparte los comandos exactos, su salida completa y
`adb devices -l`. No compartas códigos de emparejamiento. Aparecer en la lista
no significa estar autorizado.

### 3. Instala el APK o permite Accesibilidad

#### Opción A: Instalar por ADB

Omite este paso si ya está instalado. Ajusta el nombre al APK descargado:

```sh
adb -s DEVICE_ID install -r Android_VideoInfoOverlay.apk
```

Activa Accesibilidad según el paso 4. Si el interruptor se apaga inmediatamente,
prueba la opción B.

#### Opción B: APK instalado, pero Accesibilidad no permanece activada

La protección de ajustes restringidos de Android puede bloquear Accesibilidad
para aplicaciones instaladas fuera de la tienda. En algunas interfaces de TV,
el interruptor puede apagarse sin un aviso útil. Es una causa posible, no una
explicación confirmada para todos los casos.

Si está disponible, abre **Información de la aplicación > Permitir ajustes
restringidos**, posiblemente en un menú de tres puntos. Hazlo solo si confías en el APK.

Si falta esa opción, comprueba primero la conexión según el paso 2 y ejecuta:

```sh
adb -s DEVICE_ID shell appops set com.signde.androidvideooverlay ACCESS_RESTRICTED_SETTINGS allow
```

Esto permite los ajustes restringidos solo para esta aplicación. No activa
Accesibilidad ni reemplaza la lista de otros servicios de accesibilidad habilitados.
Si el comando funciona pero el interruptor sigue apagándose, indica la versión del
APK, dispositivo/compilación, método de instalación y salida exacta para investigarlo.

### 4. Activa y utiliza el overlay

1. Abre **Ajustes > Accesibilidad** (a veces dentro de **Sistema**) y activa
   **Android Video Info Overlay**. Pulsa Atrás para cerrar completamente la configuración.
2. Deja la depuración activada. Al abrir el overlay, autoriza en el TV la conexión
   ADB local de la propia aplicación. Es independiente de la autorización del ordenador.
   La aplicación necesita el puerto 5555; no requiere root ni ninguna otra aplicación auxiliar.
3. Pulsa **Favoritos / Bookmark**, o abre **Android Video Info Overlay** desde la lista
   de aplicaciones, para mostrarlo u ocultarlo. En Google TV Streamer puedes asignar
   el botón personalizable a la aplicación en **Ajustes > Mandos y accesorios**.

Para configurar la apariencia y el botón del mando, abre **Ajustes > Accesibilidad >
Android Video Info Overlay > Configuración**.

La aplicación utiliza un overlay de accesibilidad, por lo que no se espera que
aparezca en **Mostrar sobre otras aplicaciones**.

Referencias: [Conexión y autorización ADB](https://developer.android.com/tools/adb),
[emparejamiento ADB inalámbrico](https://android.googlesource.com/platform/packages/modules/adb/+/HEAD/docs/dev/adb_wifi.md),
[ajustes restringidos de Android](https://support.google.com/android/answer/12623953?hl=es).
