# 📺 Guía de Instalación y Configuración / Installation & Setup Guide
## Android VideoInfoOverlay (Google TV Streamer & Android TV)

---

# 🇪🇸 Español

### ⚠️ Importante sobre Google TV y Android 14
En Android 14 (Google TV Streamer y televisores modernos), el sistema bloquea silenciosamente los servicios de accesibilidad para aplicaciones instaladas fuera de la tienda (sideloaded). 
**El síntoma:** En la interfaz de Google TV **NO aparece ningún mensaje de error ni advertencia**. Simplemente, al intentar encender el interruptor de **Accesibilidad**, **el toggle se apaga solo de inmediato y vuelve a quedar en "Desactivado"**.

Para evitar o solucionar esto, sigue los pasos a continuación.

---

### Paso 1: Activar Opciones de Desarrollador en el Televisor
1. En tu Google TV, ve a **Ajustes > Sistema > Información**.
2. Desplázate hasta **Compilación del SO de Android TV** y presiona el botón central del control remoto **7 veces seguidas** hasta que veas el aviso: *"¡Ya eres desarrollador!"*.
3. Vuelve a **Ajustes > Sistema > Opciones para desarrolladores**:
   - Activa **Depuración por USB** (*USB Debugging*).
   - Activa **Depuración inalámbrica** (*Wireless Debugging*).
   > **Nota para Google TV Streamer:** Si tienes un cable de red Ethernet conectado, desconéctalo momentáneamente mientras configuras la depuración por Wi-Fi (Google TV desactiva la depuración inalámbrica si detecta cable Ethernet conectado).

---

### Paso 2: Instalación y Desbloqueo del Servicio

#### Opción A: Instalación limpia por ADB desde PC (Recomendada - 1 solo paso)
Si instalas el APK directamente desde una computadora conectada por Wi-Fi, se desbloquea todo automáticamente:
1. Conecta tu computadora a la misma red Wi-Fi de tu televisor.
2. Abre una terminal (PowerShell o CMD) y escribe:
   ```bash
   adb connect <IP_DE_TU_TELEVISOR>:5555
   ```
   *(Acepta en la pantalla de tu TV la ventana que dice "Permitir siempre la depuración desde este equipo").*
3. Instala el APK:
   ```bash
   adb install -r Android_VideoInfoOverlay_21.0-debug.apk
   ```
4. Desbloquea el permiso para que el interruptor no se apague solo:
   ```bash
   adb shell appops set com.signde.androidvideooverlay ACCESS_RESTRICTED_SETTINGS allow
   ```

---

#### Opción B: Si ya instalaste el APK por USB o apps ("Downloader" / "Send Files to TV")
Si ya instalaste el APK y al ir a **Accesibilidad** el interruptor **se apaga solo al instante**:
1. Conecta tu PC o celular a la terminal ADB del TV:
   ```bash
   adb connect <IP_DE_TU_TELEVISOR>:5555
   ```
2. Ejecuta este comando para permitir que el interruptor se quede encendido:
   ```bash
   adb shell appops set com.signde.androidvideooverlay ACCESS_RESTRICTED_SETTINGS allow
   ```

---

### Paso 3: Encender el Servicio de Accesibilidad
1. En el TV, ve a **Ajustes > Sistema > Accesibilidad**.
2. Selecciona **Android VideoInfoOverlay**.
3. Enciende el interruptor **Habilitar** (*Enable*). *(Ahora se quedará encendido fijamente).*

---

### Paso 4: Cómo Mostrar / Ocultar el Overlay
Una vez encendido el servicio de accesibilidad, el overlay **no** se muestra automáticamente todo el tiempo para no tapar la pantalla. Tú lo activas cuando quieras:

- **Desde la lista de apps:** Abre la app **Android VideoInfoOverlay** en la pantalla principal. Actúa como un interruptor invisible: si está oculto lo muestra, y si está visible lo apaga.
- **Con el botón del control remoto (Google TV Streamer):**
  1. Ve a **Ajustes > Mandos y accesorios > Configurar botones del mando > Botón personalizable** (el botón con la estrella `*`).
  2. Elige **Abrir aplicación** y selecciona **Android VideoInfoOverlay**.
  3. ¡Listo! Al presionar el botón de la estrella en cualquier momento (viendo Netflix, YouTube, Prime, Plex, etc.), el overlay aparecerá o desaparecerá al instante.

---
---

# 🇬🇧 English

### ⚠️ Important Notice for Google TV and Android 14
On Android 14 (Google TV Streamer and modern Google TVs), the operating system silently restricts accessibility services for apps installed outside the Play Store (sideloaded).
**The symptom:** Google TV's interface **does NOT display any restricted settings warning dialog**. Instead, when you attempt to flip the **Accessibility** switch to ON, **the toggle immediately flips back to OFF by itself**.

Follow the steps below to properly install and allow the toggle to stay enabled.

---

### Step 1: Enable Developer Options on TV
1. On your Google TV, navigate to **Settings > System > About**.
2. Scroll down to **Android TV OS build** and press the center remote button **7 times** until you see *"You are now a developer!"*.
3. Go back to **Settings > System > Developer options**:
   - Turn ON **USB debugging**.
   - Turn ON **Wireless debugging**.
   > **Note for Google TV Streamer:** If an Ethernet cable is plugged in, disconnect it temporarily while setting up over Wi-Fi (Google TV automatically turns Wireless Debugging off if wired Ethernet is active).

---

### Step 2: Installation & Permission Unlock

#### Option A: Direct ADB Install from PC (Recommended - Clean Setup)
1. Make sure your PC and TV are on the same Wi-Fi network.
2. Open a terminal (PowerShell or Terminal) and run:
   ```bash
   adb connect <TV_IP_ADDRESS>:5555
   ```
   *(Check "Always allow from this computer" on the TV prompt).*
3. Install the APK:
   ```bash
   adb install -r Android_VideoInfoOverlay_21.0-debug.apk
   ```
4. Unlock the restricted toggle permission:
   ```bash
   adb shell appops set com.signde.androidvideooverlay ACCESS_RESTRICTED_SETTINGS allow
   ```

---

#### Option B: If Sideloaded via USB or TV Apps ("Downloader" / "Send Files to TV")
If you already installed the APK and in **Accessibility** the toggle **flips back to OFF automatically**:
1. Connect via ADB from your PC or smartphone:
   ```bash
   adb connect <TV_IP_ADDRESS>:5555
   ```
2. Run this command to allow the accessibility toggle to stay ON:
   ```bash
   adb shell appops set com.signde.androidvideooverlay ACCESS_RESTRICTED_SETTINGS allow
   ```

---

### Step 3: Turn ON Accessibility
1. On your TV, navigate to **Settings > System > Accessibility**.
2. Select **Android VideoInfoOverlay**.
3. Turn the **Enable** toggle ON. *(It will now remain enabled).*

---

### Step 4: Showing & Hiding the Overlay
Once the accessibility service is running, the overlay does not permanently block your screen. You toggle it on demand:

- **From the Apps List:** Open the **Android VideoInfoOverlay** app from your home screen apps row. It serves as an instant toggle: showing the overlay if hidden, and hiding it if shown.
- **Using the Remote Shortcut (Google TV Streamer):**
  1. Navigate to **Settings > Remotes & Accessories > Set up remote buttons > Customizable button** (the star `*` button).
  2. Select **Open an app** and choose **Android VideoInfoOverlay**.
  3. Press the star button on your remote at any time during video playback (Netflix, YouTube, Prime Video, Plex, etc.) to instantly toggle the overlay!
