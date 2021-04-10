package main;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

public class Main extends Application {

    Button nextMove = new Button();
    Button nextGame = new Button();
    Button autoPlayButton = new Button();
    String dataOuput = "";

    @Override
    public void start(Stage stage) throws Exception {

        stage.setTitle("Battleship GUI");
        Canvas canvas = new Canvas(1000, 1000);
        Group root = new Group();
        Scene scene = new Scene(root, Color.WHITE);

        root.getChildren().addAll(canvas, nextMove, autoPlayButton, nextGame);

        stage.setScene(scene);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        nextMove.relocate(500, 900);
        nextMove.setText("Next Move");
        nextMove.setScaleX(2);
        nextMove.setScaleY(2);

        nextGame.relocate(700, 900);
        nextGame.setText("Next Game");
        nextGame.setScaleX(2);
        nextGame.setScaleY(2);

        autoPlayButton.relocate(500, 950);
        autoPlayButton.setText("Auto Play: Off");
        autoPlayButton.setScaleX(1.5);
        autoPlayButton.setScaleY(1.5);

        stage.show();

        Thread t = new Thread(() -> animate(gc, canvas, scene, stage, canvas.getWidth(), canvas.getHeight()));
        t.start();
    }

    boolean playMove = false;
    boolean skipGame = false;
    boolean autoPlay = false;

    /**
     * Animation thread..
     *
     * @param gc     The drawing surface
     * @param width  the width of the canvas
     * @param height the height of the canvas
     */
    public void animate(GraphicsContext gc, Canvas canvas, Scene scene, Stage stage, double width, double height) {

        nextMove.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                playMove = true;
            }
        });

        nextGame.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                skipGame = true;
            }
        });

        autoPlayButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if (autoPlay) {
                    autoPlay = false;
                    autoPlayButton.setText("Auto Play: Off");
                } else {
                    autoPlay = true;
                    autoPlayButton.setText("Auto Play: On");
                }
            }
        });

        final int NUMBEROFGAMES = 10000;

        int totalShots = 0;
        int quickestGame = 100;
        int longestGame = 0;
        int gamesOver20 = 0;
        int gamesOver30 = 0;
        int gamesOver40 = 0;
        int gamesOver50 = 0;
        int gamesOver60 = 0;
        int gamesOver70 = 0;
        int gamesOver80 = 0;
        int gamesOver90 = 0;

        //Main animation loop
        for (int game = 0; game < NUMBEROFGAMES; game++) {
            SetTitle(stage, game, NUMBEROFGAMES);
            BattleShip battleShip = new BattleShip();
            SamBot samBot = new SamBot(battleShip, width, height, gc, canvas);

            while (!battleShip.allSunk()) {

                if (playMove || autoPlay || battleShip.totalShotsTaken() < 1 || skipGame) {
                    playMove = false;
                    gc.clearRect(0, 0, width, height);

                    samBot.fireShot();

                    dataOuput = samBot.getDataOutput();

                    gc.setFill(Color.BLACK);
                    gc.setFont(Font.font(18));
                    gc.fillText("Shots: " + battleShip.totalShotsTaken(), 50, 50);
                }

                if (battleShip.totalShotsTaken() > 60) {
                    //autoPlay = false;
                }

                if (autoPlay) {
                    //pause(10);//2 fps
                }

            }

            skipGame = false;

            int gameShots = battleShip.totalShotsTaken();
            if (gameShots < quickestGame) {
                quickestGame = gameShots;
            }
            if (gameShots > longestGame) {
                longestGame = gameShots;
            }
            if (gameShots > 90) {
                gamesOver90++;
            } else if (gameShots > 80) {
                gamesOver80++;
            } else if (gameShots > 70) {
                gamesOver70++;
            } else if (gameShots > 60) {
                gamesOver60++;
            } else if (gameShots > 50) {
                gamesOver50++;
            } else if (gameShots > 40) {
                gamesOver40++;
            } else if (gameShots > 30) {
                gamesOver30++;
            } else if (gameShots > 20) {
                gamesOver20++;
            }
            totalShots += gameShots;

        }

        String out = String.format("SampleBot - The Average # of Shots required in %d games to sink all Ships = %.2f\n", NUMBEROFGAMES, (double) totalShots / NUMBEROFGAMES);
        System.out.println(out);
        System.out.println("Quickest game: " + quickestGame + "  -  Longest game: " + longestGame);
        System.out.println(String.format("Games over 20: %d - %.2f%%", gamesOver20, ((float)gamesOver20/(float)NUMBEROFGAMES * 100)));
        System.out.println(String.format("Games over 30: %d - %.2f%%", gamesOver30, ((float)gamesOver30/(float)NUMBEROFGAMES * 100)));
        System.out.println(String.format("Games over 40: %d - %.2f%%", gamesOver40, ((float)gamesOver40/(float)NUMBEROFGAMES * 100)));
        System.out.println(String.format("Games over 50: %d - %.2f%%", gamesOver50, ((float)gamesOver50/(float)NUMBEROFGAMES * 100)));
        System.out.println(String.format("Games over 60: %d - %.2f%%", gamesOver60, ((float)gamesOver60/(float)NUMBEROFGAMES * 100)));
        System.out.println(String.format("Games over 70: %d - %.2f%%", gamesOver70, ((float)gamesOver70/(float)NUMBEROFGAMES * 100)));
        System.out.println(String.format("Games over 80: %d - %.2f%%", gamesOver80, ((float)gamesOver80/(float)NUMBEROFGAMES * 100)));
        System.out.println(String.format("Games over 90: %d - %.2f%%", gamesOver90, ((float)gamesOver90/(float)NUMBEROFGAMES * 100)));
        System.out.println();

        gc.setFill(Color.BLACK);
        gc.setFont(Font.font(18));
        gc.fillText(out, 50, 800);

    }


    public static void SetTitle(Stage stage, int game, int NUMBEROFGAMES) {
        Platform.runLater(() -> {
            stage.setTitle("Battleship GUI | Game: " + game + "/" + NUMBEROFGAMES);
        });
    }

    public static void pause(int duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException ex) {
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void stop() {
        System.out.println(dataOuput);
    }
}
