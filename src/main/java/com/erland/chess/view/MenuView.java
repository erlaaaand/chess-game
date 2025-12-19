package com.erland.chess.view;

import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;
import com.erland.chess.network.GameClient;
import com.erland.chess.network.GameServer;

public class MenuView {
    private Stage primaryStage;
    private VBox root;
    
    public enum GameMode {
        VS_COMPUTER, LOCAL_MULTIPLAYER, NETWORK
    }
    
    public MenuView(Stage stage) {
        this.primaryStage = stage;
        createUI();
    }
    
    private void createUI() {
        root = new VBox(30);
        root.setAlignment(Pos.CENTER);
        root.getStyleClass().add("menu-container");
        root.setPadding(new Insets(40));
        
        // Title with animation
        Text title = new Text("♔ CHESS GAME ♔");
        title.getStyleClass().add("menu-title");
        title.setFont(Font.font("Serif", FontWeight.BOLD, 72));
        
        // Subtitle
        Text subtitle = new Text("Modern Edition with AI Analysis");
        subtitle.getStyleClass().add("menu-subtitle");
        subtitle.setFont(Font.font("Arial", FontWeight.NORMAL, 18));
        
        // Animate title
        ScaleTransition scaleTransition = new ScaleTransition(Duration.seconds(2), title);
        scaleTransition.setFromX(0.8);
        scaleTransition.setFromY(0.8);
        scaleTransition.setToX(1.0);
        scaleTransition.setToY(1.0);
        scaleTransition.setCycleCount(Timeline.INDEFINITE);
        scaleTransition.setAutoReverse(true);
        scaleTransition.play();
        
        // Buttons
        Button btnVsComputer = createMenuButton("🤖 VS Computer", "Play against AI");
        Button btnLocalMultiplayer = createMenuButton("👥 Local Multiplayer", "Play with friend");
        Button btnHostGame = createMenuButton("🌐 Host Network Game", "Host online game");
        Button btnJoinGame = createMenuButton("🔗 Join Network Game", "Join online game");
        Button btnExit = createMenuButton("❌ Exit", "Close application");
        btnExit.getStyleClass().add("exit-button");
        
        // Button actions
        btnVsComputer.setOnAction(e -> startGame(GameMode.VS_COMPUTER));
        btnLocalMultiplayer.setOnAction(e -> startGame(GameMode.LOCAL_MULTIPLAYER));
        btnHostGame.setOnAction(e -> hostNetworkGame());
        btnJoinGame.setOnAction(e -> joinNetworkGame());
        btnExit.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Exit Confirmation");
            alert.setHeaderText("Are you sure you want to exit?");
            alert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    System.exit(0);
                }
            });
        });
        
        // Add entrance animations
        addEntranceAnimation(btnVsComputer, 0);
        addEntranceAnimation(btnLocalMultiplayer, 0.1);
        addEntranceAnimation(btnHostGame, 0.2);
        addEntranceAnimation(btnJoinGame, 0.3);
        addEntranceAnimation(btnExit, 0.4);
        
        // Footer
        Text footer = new Text("© 2024 Chess Game | JavaFX + Python Integration");
        footer.setStyle("-fx-font-size: 12px; -fx-fill: #95a5a6;");
        
        // Spacer
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        
        root.getChildren().addAll(
            title, subtitle, 
            new Region(), // Small spacer
            btnVsComputer, btnLocalMultiplayer, 
            btnHostGame, btnJoinGame, btnExit,
            spacer, footer
        );
    }
    
    private Button createMenuButton(String text, String tooltip) {
        Button button = new Button(text);
        button.getStyleClass().add("menu-button");
        button.setMaxWidth(400);
        button.setMinHeight(70);
        button.setTooltip(new Tooltip(tooltip));
        
        // Hover effect
        button.setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(100), button);
            st.setToX(1.05);
            st.setToY(1.05);
            st.play();
        });
        
        button.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(100), button);
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();
        });
        
        return button;
    }
    
    private void addEntranceAnimation(Button button, double delay) {
        button.setOpacity(0);
        button.setTranslateX(-50);
        
        FadeTransition fadeIn = new FadeTransition(Duration.millis(500), button);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.setDelay(Duration.seconds(delay));
        
        TranslateTransition slideIn = new TranslateTransition(Duration.millis(500), button);
        slideIn.setFromX(-50);
        slideIn.setToX(0);
        slideIn.setDelay(Duration.seconds(delay));
        
        ParallelTransition parallel = new ParallelTransition(fadeIn, slideIn);
        parallel.play();
    }
    
    private void startGame(GameMode mode) {
        BoardView boardView = new BoardView(primaryStage, mode, null, true);
        primaryStage.getScene().setRoot(boardView.getRoot());
    }
    
    private void hostNetworkGame() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Host Network Game");
        dialog.setHeaderText("Configure Server");
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        
        TextField portField = new TextField("5555");
        portField.setPromptText("Port number");
        
        grid.add(new Label("Port:"), 0, 0);
        grid.add(portField, 1, 0);
        
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                int port = 5555;
                try {
                    String portStr = portField.getText().trim();
                    if (!portStr.isEmpty()) {
                        port = Integer.parseInt(portStr);
                        if (port < 1024 || port > 65535) {
                            showError("Port must be between 1024 and 65535!\nUsing default: 5555");
                            port = 5555;
                        }
                    }
                } catch (NumberFormatException e) {
                    showError("Invalid port number!\nUsing default: 5555");
                }
                
                final int finalPort = port;
                GameServer server = new GameServer(finalPort);
                
                // Waiting dialog
                Alert waitDialog = new Alert(Alert.AlertType.INFORMATION);
                waitDialog.setTitle("Waiting for opponent");
                waitDialog.setHeaderText("Server started on port " + finalPort);
                waitDialog.setContentText("Waiting for opponent to connect...\nShare your IP and port.");
                waitDialog.getDialogPane().lookupButton(ButtonType.OK).setVisible(false);
                
                // Start server in background
                new Thread(() -> {
                    server.start();
                    javafx.application.Platform.runLater(() -> {
                        waitDialog.close();
                        startNetworkGame(server, true);
                    });
                }).start();
                
                waitDialog.show();
            }
        });
    }
    
    private void joinNetworkGame() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Join Network Game");
        dialog.setHeaderText("Connect to Server");
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        
        TextField ipField = new TextField("localhost");
        ipField.setPromptText("Server IP");
        TextField portField = new TextField("5555");
        portField.setPromptText("Port");
        
        grid.add(new Label("Host IP:"), 0, 0);
        grid.add(ipField, 1, 0);
        grid.add(new Label("Port:"), 0, 1);
        grid.add(portField, 1, 1);
        
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                String ip = ipField.getText().trim();
                if (ip.isEmpty()) ip = "localhost";
                
                int port;
                try {
                    port = Integer.parseInt(portField.getText().trim());
                    if (port < 1024 || port > 65535) {
                        showError("Port must be between 1024 and 65535!");
                        return;
                    }
                } catch (NumberFormatException e) {
                    showError("Invalid port number!");
                    return;
                }
                
                GameClient client = new GameClient(ip, port);
                
                // Connecting dialog
                Alert connectDialog = new Alert(Alert.AlertType.INFORMATION);
                connectDialog.setTitle("Connecting");
                connectDialog.setHeaderText("Connecting to " + ip + ":" + port);
                connectDialog.setContentText("Please wait...");
                connectDialog.getDialogPane().lookupButton(ButtonType.OK).setVisible(false);
                connectDialog.show();
                
                // Try connect
                new Thread(() -> {
                    boolean connected = client.connect();
                    javafx.application.Platform.runLater(() -> {
                        connectDialog.close();
                        if (connected) {
                            showInfo("Successfully connected to server!");
                            startNetworkGame(client, false);
                        } else {
                            showError("Failed to connect!\n\nCheck:\n- IP address\n- Port number\n- Server status");
                        }
                    });
                }).start();
            }
        });
    }
    
    private void startNetworkGame(Object network, boolean isHost) {
        BoardView boardView = new BoardView(primaryStage, GameMode.NETWORK, network, isHost);
        primaryStage.getScene().setRoot(boardView.getRoot());
    }
    
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    public Parent getRoot() {
        return root;
    }
}