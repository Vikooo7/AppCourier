# Journeys · RutaLog Courier

Pruebas de interfaz en lenguaje natural para **Journeys de Android Studio** (Gemini). El agente las ejecuta en el emulador o en el celular y dice si pasan o fallan.

**Cómo crear cada una:** clic derecho en el módulo `app` → **New → Journey Test**, ponle el nombre del journey y copia los pasos y las verificaciones. Android Studio guarda el archivo `.journey.xml` en `app/src/journeysTest/`. Ejecútalas otra vez después de cada cambio grande.

Usan las cuentas de prueba del README contra la API publicada.

## Ver la ruta del día

1. Abre la app y espera que termine el splash.
2. Escribe courier1@rutalog.pe en Correo electrónico y 1234 en Contraseña.
3. Toca Ingresar.

**Verificar:**

- Se abre Ruta del día con el número de paradas y el mapa.
- La primera parada de la lista tiene el número 1.

## Guía equivocada al recoger

1. Inicia sesión con courier1@rutalog.pe y la contraseña 1234.
2. Toca la primera parada de tipo RECOJO.
3. Toca Escribir la guía a mano y escribe RLP-26-999999-9.
4. Confirma.

**Verificar:**

- La app avisa que la guía no coincide con la parada y el estado sigue en Por recoger.

