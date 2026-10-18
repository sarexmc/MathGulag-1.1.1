# Gulag (Forge 1.20.1)

Mod de gulag tipo "bomba caliente": HUD con venas que laten, filtro rojo, pinchos con numeros,
pregunta de un solo digito, TNT en la cabeza y confeti del totem al explotar.

## Compilar el .jar en GitHub (sin instalar nada)
1. Crea un repositorio nuevo en GitHub.
2. Sube TODO el contenido de esta carpeta (incluida `.github/workflows/build.yml`).
   Si el navegador no sube la carpeta oculta `.github`, crea el archivo a mano:
   "Add file > Create new file", nombre `.github/workflows/build.yml`, y pega el contenido.
3. Ve a la pestana **Actions** > "Build mod". Si no arranca solo, pulsa **Run workflow**.
4. Cuando termine (verde), entra a la ejecucion y descarga el artefacto **gulag-mod**: dentro esta `gulag-1.0.0.jar`.
5. Pon el .jar en la carpeta `mods` de Minecraft 1.20.1 con Forge 47.x (servidor y todos los jugadores).

## Uso
- `/gulag <jugadores>`  inicia el gulag (acepta @a, @p, nombres...). Funciona con 1 jugador para probar:
  golpea cualquier criatura para "pasar" la bomba.
- `/gulag stop`  detiene el gulag.
- Quien tiene la bomba ve la pregunta (ej. `9 - 3`) y los numeros: clic en el numero o teclea el digito.
  Fallar o quedarse sin tiempo = explosion + muerte. Acertar = HUD verde y debe golpear a otro jugador.

## Ajustes rapidos
- Tiempos: `QUESTION_TICKS` y `PASS_TICKS` en `game/GulagManager.java` (12 s y 10 s).
- Posiciones de los pinchos: `SLOT` en `client/ClientState.java`.
