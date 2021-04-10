# Battleship_GUI
A GUI for a Battleship AI, made using JavaFX

To get this project running, assuming you are using IntelliJ, once unzipped and opened, go to:<br/><br/>**File &#8594; Project Structure &#8594; Modules &#8594; Dependencies**, and check mark the *'javafx-sdk-11.0'*. Click *'Apply'*, then *'OK'*.<br/><br/>Next go to: **Run &#8594; Edit Configurations**, and in the *'VM options'*, add:<br/> **--module-path working\directory\javafx-sdk-11.0.2\lib --add-modules=javafx.controls**, where *'working\directory'* is the contents of the *'Working directory'* line below. Click *'OK'*.<br/><br/>Now you can run Main.java.
