@echo off
setlocal EnableExtensions DisableDelayedExpansion
cd /d "%~dp0"
where java >nul 2>nul
if errorlevel 1 (
  echo No se encontro Java. Instala un JDK 17 o superior y vuelve a abrir esta ventana.
  pause
  exit /b 1
)
java com.sun.tools.javac.Main -version >nul 2>nul
if errorlevel 1 (
  echo No se encontro el compilador de Java. Usa un JDK 17 o superior.
  pause
  exit /b 1
)
if not exist "src\Main.java" (
  echo Falta src\Main.java. Coloca este iniciador en la raiz del proyecto.
  pause
  exit /b 1
)
if not exist "lib\sqlite-jdbc-3.54.1.0.jar" (
  echo Falta lib\sqlite-jdbc-3.54.1.0.jar. Extrae el ZIP completo antes de ejecutar.
  pause
  exit /b 1
)
if not exist "bin" mkdir "bin"
if not exist "runtime-tmp" mkdir "runtime-tmp"
(for %%F in (src\*.java) do @echo "src/%%~nxF") > "runtime-tmp\fuentes.txt"
echo Compilando Nexo...
java com.sun.tools.javac.Main -encoding UTF-8 --add-modules jdk.httpserver -cp "lib/*" -d bin @runtime-tmp\fuentes.txt
if errorlevel 1 (
  echo.
  echo La compilacion fallo. Revisa el error mostrado arriba.
  echo La lista de archivos se genero en runtime-tmp\fuentes.txt.
  pause
  exit /b 1
)
java -Dfile.encoding=UTF-8 "-Djava.io.tmpdir=%CD%\runtime-tmp" --add-modules jdk.httpserver -cp "bin;lib/*" Main %*
if errorlevel 1 (
  echo.
  echo Revisa el error de arriba. Si el puerto 8080 esta ocupado, cierra la otra ventana de Nexo.
  pause
  exit /b 1
)
endlocal
