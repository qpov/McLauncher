# Компиляция (Windows PowerShell или CMD):
javac -cp ".;lib/com/google/code/gson/gson/2.11.0/gson-2.11.0.jar;lib\flatlaf-3.5.4.jar;lib" -d bin src\com\launcher\*.java

# Запуск:
java -cp "bin;lib/com/google/code/gson/gson/2.11.0/gson-2.11.0.jar;lib\flatlaf-3.5.4.jar" com.launcher.LauncherUI

# Создание исполняемого JAR:
jar cfm McLauncher.jar manifest.mf -C bin .

# Тестовый запуск JAR:
java -jar McLauncher.jar

# Getdown
java -jar getdown-launcher-1.8.7.jar