# Coin Catcher

Small Java Swing platformer.

## Structure

- `Main.java`: app entry and menu
- `GamePanel.java`: game loop, input, score, HUD, coin lifecycle
- `Player.java`: player movement and physics
- `HazardManager.java`: enemies and obstacles
- `WorldMap.java`: platforms, ground drawing, coin spawn analysis
- `GameConfig.java`: shared constants and colors

## Run

Compile all sources:

```powershell
Set-Location "C:\Maxim\test\coin_catcher"
javac src\main\java\*.java
```

Start the game:

```powershell
Set-Location "C:\Maxim\test\coin_catcher"
java -cp src\main\java Main
```

