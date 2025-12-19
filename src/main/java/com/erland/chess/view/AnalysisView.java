package com.erland.chess.view;

import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;
import com.erland.chess.model.Board;
import com.google.gson.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class AnalysisView {
    private Stage primaryStage;
    private BorderPane root;
    private BoardView boardView;
    private Board board;
    
    private VBox contentBox;
    private ScrollPane scrollPane;
    
    // Analysis data
    private JsonObject gameData;
    private Map<String, Integer> pieceActivity = new HashMap<>();
    private int whiteCaptures = 0;
    private int blackCaptures = 0;
    
    public AnalysisView(Stage stage, Board board, BoardView boardView) {
        this.primaryStage = stage;
        this.board = board;
        this.boardView = boardView;
        
        loadAnalysisData();
        createUI();
    }
    
    private void loadAnalysisData() {
        try {
            // Find most recent game file
            Path bridgeDir = Paths.get("python_bridge");
            if (!Files.exists(bridgeDir)) {
                System.err.println("Bridge directory not found");
                return;
            }
            
            Optional<Path> latestFile = Files.list(bridgeDir)
                .filter(p -> p.getFileName().toString().startsWith("game_"))
                .filter(p -> p.getFileName().toString().endsWith(".json"))
                .max(Comparator.comparingLong(p -> {
                    try {
                        return Files.getLastModifiedTime(p).toMillis();
                    } catch (IOException e) {
                        return 0;
                    }
                }));
            
            if (latestFile.isPresent()) {
                String content = Files.readString(latestFile.get());
                gameData = JsonParser.parseString(content).getAsJsonObject();
                analyzeGameData();
            }
        } catch (Exception e) {
            System.err.println("Error loading analysis data: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void analyzeGameData() {
        if (gameData == null || !gameData.has("moves")) return;
        
        JsonArray moves = gameData.getAsJsonArray("moves");
        
        for (JsonElement moveElem : moves) {
            JsonObject move = moveElem.getAsJsonObject();
            String piece = move.get("piece").getAsString();
            String color = move.get("color").getAsString();
            
            // Count piece activity
            pieceActivity.put(piece, pieceActivity.getOrDefault(piece, 0) + 1);
            
            // Count captures
            if (!move.get("captured").isJsonNull()) {
                if (color.equals("white")) {
                    whiteCaptures++;
                } else {
                    blackCaptures++;
                }
            }
        }
    }
    
    private void createUI() {
        root = new BorderPane();
        root.getStyleClass().add("analysis-container");
        
        // Header
        HBox header = createHeader();
        root.setTop(header);
        
        // Content
        contentBox = new VBox(20);
        contentBox.setPadding(new Insets(30));
        contentBox.setAlignment(Pos.TOP_CENTER);
        
        // Add analysis sections
        contentBox.getChildren().addAll(
            createGameSummaryCard(),
            createStatisticsCard(),
            createPieceActivityCard(),
            createMoveQualityCard(),
            createRecommendationsCard(),
            createChartSection()
        );
        
        scrollPane = new ScrollPane(contentBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        
        root.setCenter(scrollPane);
        
        // Animate entrance
        animateEntrance();
    }
    
    private HBox createHeader() {
        HBox header = new HBox(20);
        header.setPadding(new Insets(20));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: rgba(0, 0, 0, 0.3);");
        
        Label title = new Label("📊 Game Analysis");
        title.getStyleClass().add("analysis-title");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 36));
        title.setTextFill(Color.web("#ffd700"));
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Button btnRunPython = new Button("🐍 Run Python Analysis");
        btnRunPython.getStyleClass().add("game-button");
        btnRunPython.setOnAction(e -> runPythonAnalysis());
        
        Button btnBack = new Button("← Back to Game");
        btnBack.getStyleClass().add("game-button");
        btnBack.setOnAction(e -> backToGame());
        
        Button btnMenu = new Button("🏠 Menu");
        btnMenu.getStyleClass().add("game-button");
        btnMenu.setOnAction(e -> backToMenu());
        
        header.getChildren().addAll(title, spacer, btnRunPython, btnBack, btnMenu);
        return header;
    }
    
    private VBox createGameSummaryCard() {
        VBox card = createCard("Game Summary");
        
        GridPane grid = new GridPane();
        grid.setHgap(30);
        grid.setVgap(15);
        grid.setPadding(new Insets(15));
        
        String result = board.gameState.toString();
        int totalMoves = board.moveHistory.size();
        String timestamp = gameData != null ? 
            gameData.get("timestamp").getAsString() : "Unknown";
        String comment = gameData != null && gameData.has("user_comment") ? 
            gameData.get("user_comment").getAsString() : "No comment";
        
        addStatRow(grid, 0, "Result:", result, Color.web("#3fc380"));
        addStatRow(grid, 1, "Total Moves:", String.valueOf(totalMoves), Color.web("#4aa8eb"));
        addStatRow(grid, 2, "Date:", timestamp, Color.web("#f39c12"));
        addStatRow(grid, 3, "Comment:", comment, Color.web("#9b59b6"));
        
        card.getChildren().add(grid);
        return card;
    }
    
    private VBox createStatisticsCard() {
        VBox card = createCard("Move Statistics");
        
        GridPane grid = new GridPane();
        grid.setHgap(30);
        grid.setVgap(15);
        grid.setPadding(new Insets(15));
        
        int whiteMoves = (int) Math.ceil(board.moveHistory.size() / 2.0);
        int blackMoves = board.moveHistory.size() / 2;
        int totalCaptures = whiteCaptures + blackCaptures;
        double avgGameLength = board.moveHistory.size();
        
        addStatRow(grid, 0, "White Moves:", String.valueOf(whiteMoves), Color.WHITE);
        addStatRow(grid, 1, "Black Moves:", String.valueOf(blackMoves), Color.GRAY);
        addStatRow(grid, 2, "White Captures:", String.valueOf(whiteCaptures), Color.web("#3fc380"));
        addStatRow(grid, 3, "Black Captures:", String.valueOf(blackCaptures), Color.web("#e74c3c"));
        addStatRow(grid, 4, "Total Captures:", String.valueOf(totalCaptures), Color.web("#f39c12"));
        addStatRow(grid, 5, "Average Move Time:", "N/A", Color.web("#9b59b6"));
        
        card.getChildren().add(grid);
        return card;
    }
    
    private VBox createPieceActivityCard() {
        VBox card = createCard("Piece Activity");
        
        if (pieceActivity.isEmpty()) {
            Label emptyLabel = new Label("No activity data available");
            emptyLabel.setTextFill(Color.GRAY);
            card.getChildren().add(emptyLabel);
            return card;
        }
        
        VBox activityBox = new VBox(10);
        activityBox.setPadding(new Insets(15));
        
        // Sort by activity
        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(pieceActivity.entrySet());
        sorted.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        
        for (Map.Entry<String, Integer> entry : sorted) {
            HBox row = new HBox(15);
            row.setAlignment(Pos.CENTER_LEFT);
            
            Label pieceLabel = new Label(getPieceEmoji(entry.getKey()) + " " + entry.getKey());
            pieceLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
            pieceLabel.setTextFill(Color.WHITE);
            pieceLabel.setPrefWidth(120);
            
            ProgressBar bar = new ProgressBar();
            bar.setPrefWidth(300);
            double progress = entry.getValue() / (double) board.moveHistory.size();
            bar.setProgress(progress);
            bar.setStyle("-fx-accent: #3fc380;");
            
            Label countLabel = new Label(entry.getValue() + " moves");
            countLabel.setTextFill(Color.web("#95a5a6"));
            
            row.getChildren().addAll(pieceLabel, bar, countLabel);
            activityBox.getChildren().add(row);
        }
        
        card.getChildren().add(activityBox);
        return card;
    }
    
    private VBox createMoveQualityCard() {
        VBox card = createCard("Move Quality Assessment");
        
        VBox qualityBox = new VBox(15);
        qualityBox.setPadding(new Insets(15));
        
        // Simulated quality metrics (in real implementation, get from Python)
        addQualityBar(qualityBox, "Brilliant Moves", 2, Color.web("#00d9ff"));
        addQualityBar(qualityBox, "Good Moves", 15, Color.web("#3fc380"));
        addQualityBar(qualityBox, "Normal Moves", 20, Color.web("#f39c12"));
        addQualityBar(qualityBox, "Inaccuracies", 5, Color.web("#e67e22"));
        addQualityBar(qualityBox, "Mistakes", 2, Color.web("#e74c3c"));
        addQualityBar(qualityBox, "Blunders", 0, Color.web("#8e44ad"));
        
        card.getChildren().add(qualityBox);
        return card;
    }
    
    private void addQualityBar(VBox container, String label, int count, Color color) {
        HBox row = new HBox(15);
        row.setAlignment(Pos.CENTER_LEFT);
        
        Label nameLabel = new Label(label);
        nameLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 14));
        nameLabel.setTextFill(Color.WHITE);
        nameLabel.setPrefWidth(150);
        
        ProgressBar bar = new ProgressBar();
        bar.setPrefWidth(250);
        double maxMoves = board.moveHistory.size();
        bar.setProgress(count / maxMoves);
        bar.setStyle(String.format("-fx-accent: %s;", toRgbString(color)));
        
        Label countLabel = new Label(String.valueOf(count));
        countLabel.setTextFill(color);
        countLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        
        row.getChildren().addAll(nameLabel, bar, countLabel);
        container.getChildren().add(row);
    }
    
    private VBox createRecommendationsCard() {
        VBox card = createCard("Improvement Recommendations");
        
        VBox recBox = new VBox(10);
        recBox.setPadding(new Insets(15));
        
        // Generate recommendations based on game data
        List<String> recommendations = generateRecommendations();
        
        for (int i = 0; i < recommendations.size(); i++) {
            HBox recRow = new HBox(10);
            recRow.setAlignment(Pos.TOP_LEFT);
            
            Label numLabel = new Label((i + 1) + ".");
            numLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
            numLabel.setTextFill(Color.web("#3fc380"));
            numLabel.setMinWidth(30);
            
            Label recLabel = new Label(recommendations.get(i));
            recLabel.setWrapText(true);
            recLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 14));
            recLabel.setTextFill(Color.WHITE);
            
            recRow.getChildren().addAll(numLabel, recLabel);
            recBox.getChildren().add(recRow);
        }
        
        card.getChildren().add(recBox);
        return card;
    }
    
    private List<String> generateRecommendations() {
        List<String> recs = new ArrayList<>();
        
        if (board.moveHistory.size() < 20) {
            recs.add("Practice longer games to improve endgame skills");
        }
        
        if (whiteCaptures + blackCaptures < board.moveHistory.size() * 0.1) {
            recs.add("Look for more tactical opportunities and piece exchanges");
        }
        
        if (pieceActivity.getOrDefault("Pawn", 0) > board.moveHistory.size() * 0.4) {
            recs.add("Develop your pieces more actively in the opening");
        }
        
        recs.add("Study common opening patterns to improve your repertoire");
        recs.add("Practice tactical puzzles to sharpen your calculation");
        recs.add("Review your games regularly to identify recurring mistakes");
        
        return recs;
    }
    
    private VBox createChartSection() {
        VBox card = createCard("Visual Analysis");
        
        // Create pie chart for game result distribution
        PieChart pieChart = new PieChart();
        pieChart.setTitle("Captures Distribution");
        pieChart.getData().addAll(
            new PieChart.Data("White Captures", whiteCaptures),
            new PieChart.Data("Black Captures", blackCaptures),
            new PieChart.Data("No Capture", board.moveHistory.size() - whiteCaptures - blackCaptures)
        );
        pieChart.setLegendVisible(true);
        
        card.getChildren().add(pieChart);
        return card;
    }
    
    private VBox createCard(String title) {
        VBox card = new VBox(10);
        card.getStyleClass().add("analysis-card");
        card.setMaxWidth(900);
        
        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        titleLabel.setTextFill(Color.web("#ffd700"));
        
        Separator separator = new Separator();
        separator.setStyle("-fx-background-color: rgba(255, 255, 255, 0.2);");
        
        card.getChildren().addAll(titleLabel, separator);
        return card;
    }
    
    private void addStatRow(GridPane grid, int row, String label, String value, Color valueColor) {
        Label lblLabel = new Label(label);
        lblLabel.getStyleClass().add("stat-label");
        lblLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        lblLabel.setTextFill(Color.web("#95a5a6"));
        
        Label lblValue = new Label(value);
        lblValue.getStyleClass().add("stat-value");
        lblValue.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        lblValue.setTextFill(valueColor);
        
        grid.add(lblLabel, 0, row);
        grid.add(lblValue, 1, row);
    }
    
    private String getPieceEmoji(String piece) {
        switch (piece) {
            case "King": return "♔";
            case "Queen": return "♕";
            case "Rook": return "♖";
            case "Bishop": return "♗";
            case "Knight": return "♘";
            case "Pawn": return "♙";
            default: return "•";
        }
    }
    
    private String toRgbString(Color color) {
        return String.format("rgb(%d,%d,%d)", 
            (int)(color.getRed() * 255),
            (int)(color.getGreen() * 255),
            (int)(color.getBlue() * 255));
    }
    
    private void animateEntrance() {
        for (int i = 0; i < contentBox.getChildren().size(); i++) {
            var child = contentBox.getChildren().get(i);
            child.setOpacity(0);
            child.setTranslateY(30);
            
            FadeTransition fade = new FadeTransition(Duration.millis(500), child);
            fade.setFromValue(0);
            fade.setToValue(1);
            fade.setDelay(Duration.millis(i * 100));
            
            TranslateTransition slide = new TranslateTransition(Duration.millis(500), child);
            slide.setFromY(30);
            slide.setToY(0);
            slide.setDelay(Duration.millis(i * 100));
            
            new ParallelTransition(fade, slide).play();
        }
    }
    
    private void runPythonAnalysis() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Python Analysis");
        alert.setHeaderText("Running Python analysis scripts...");
        alert.setContentText("This will execute chess_analyzer.py\nCheck console for output.");
        
        ButtonType runBtn = new ButtonType("Run", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelBtn = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(runBtn, cancelBtn);
        
        alert.showAndWait().ifPresent(response -> {
            if (response == runBtn) {
                executePythonScript();
            }
        });
    }
    
    private void executePythonScript() {
        new Thread(() -> {
            try {
                ProcessBuilder pb = new ProcessBuilder("python", "chess_analyzer.py");
                pb.redirectErrorStream(true);
                Process process = pb.start();
                
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));
                
                String line;
                StringBuilder output = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                    output.append(line).append("\n");
                }
                
                int exitCode = process.waitFor();
                
                String finalOutput = output.toString();
                javafx.application.Platform.runLater(() -> {
                    if (exitCode == 0) {
                        showPythonOutput(finalOutput);
                    } else {
                        showError("Python script failed with exit code: " + exitCode);
                    }
                });
                
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> 
                    showError("Error running Python script: " + e.getMessage()));
            }
        }).start();
    }
    
    private void showPythonOutput(String output) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Python Analysis Results");
        alert.setHeaderText("Analysis Complete!");
        
        TextArea textArea = new TextArea(output);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);
        
        alert.getDialogPane().setContent(textArea);
        alert.showAndWait();
    }
    
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void backToGame() {
        primaryStage.getScene().setRoot(boardView.getRoot());
    }
    
    private void backToMenu() {
        MenuView menuView = new MenuView(primaryStage);
        primaryStage.getScene().setRoot(menuView.getRoot());
    }
    
    public Parent getRoot() {
        return root;
    }
}