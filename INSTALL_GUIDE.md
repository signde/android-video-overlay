# Installation & setup / Instalación y configuración

Android VideoInfoOverlay is experimental. Menu names and debugging options vary by device.
Android VideoInfoOverlay es experimental. Los menús y las opciones de depuración varían según el dispositivo.

## English

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
adb -s DEVICE_ID install -r Android_VideoInfoOverlay_21.1-debug.apk
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
   **Android VideoInfoOverlay**. Press Back to fully close the setup screen.
2. Leave debugging enabled. When opening the overlay, approve the app's own local
   ADB connection on the TV. This authorization is separate from your computer's.
   The app needs port 5555; no root or any other helper is needed.
3. Press **Favorites / Bookmark**, or open **Android VideoInfoOverlay** from the apps
   list, to show or hide it. On Google TV Streamer, you can assign the customizable
   remote button to open the app in **Settings > Remotes & accessories**.

For appearance and remote-button settings, open **Settings > Accessibility >
Android VideoInfoOverlay > Configuration**.

The app uses an accessibility overlay, so it is not expected to appear under
**Display over other apps**.

References: [ADB connection and authorization](https://developer.android.com/tools/adb),
[wireless ADB pairing](https://android.googlesource.com/platform/packages/modules/adb/+/HEAD/docs/dev/adb_wifi.md),
[Android restricted settings](https://support.google.com/android/answer/12623953?hl=en).

## Español

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
adb -s DEVICE_ID install -r Android_VideoInfoOverlay_21.1-debug.apk
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
   **Android VideoInfoOverlay**. Pulsa Atrás para cerrar completamente la configuración.
2. Deja la depuración activada. Al abrir el overlay, autoriza en el TV la conexión
   ADB local de la propia aplicación. Es independiente de la autorización del ordenador.
   La aplicación necesita el puerto 5555; no requiere root ni ninguna otra aplicación auxiliar.
3. Pulsa **Favoritos / Bookmark**, o abre **Android VideoInfoOverlay** desde la lista
   de aplicaciones, para mostrarlo u ocultarlo. En Google TV Streamer puedes asignar
   el botón personalizable a la aplicación en **Ajustes > Mandos y accesorios**.

Para configurar la apariencia y el botón del mando, abre **Ajustes > Accesibilidad >
Android VideoInfoOverlay > Configuración**.

La aplicación utiliza un overlay de accesibilidad, por lo que no se espera que
aparezca en **Mostrar sobre otras aplicaciones**.

Referencias: [Conexión y autorización ADB](https://developer.android.com/tools/adb),
[emparejamiento ADB inalámbrico](https://android.googlesource.com/platform/packages/modules/adb/+/HEAD/docs/dev/adb_wifi.md),
[ajustes restringidos de Android](https://support.google.com/android/answer/12623953?hl=es).
