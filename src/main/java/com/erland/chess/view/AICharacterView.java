package com.erland.chess.view;

import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;
import com.erland.chess.ai.AICharacter;
import com.erland.chess.ai.AICharacterManager;

/**
 * View untuk mengelola karakter AI - create, view, delete
 */
public class AICharacterView {
    private final Stage primaryStage;
    private final BorderPane root;
    private final AICharacterManager characterManager;
    
    private VBox characterListBox;
    private ScrollPane scrollPane;
    
    public AICharacterView(Stage stage) {
        this.primaryStage = stage;
        this.characterManager = AICharacterManager.getInstance();
        this.root = new BorderPane();
        root.getStyleClass().add("ai-character-container");
        
        createUI();
        refreshCharacterList();
    }
    
    private void createUI() {
        // Header
        HBox header = createHeader();
        root.setTop(header);
        
        // Character list
        characterListBox = new VBox(15);
        characterListBox.setPadding(new Insets(20));
        characterListBox.setAlignment(Pos.TOP_CENTER);
        
        scrollPane = new ScrollPane(characterListBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        
        root.setCenter(scrollPane);
        
        // Footer with leaderboard button
        HBox footer = createFooter();
        root.setBottom(footer);
    }
    
    private HBox createHeader() {
        HBox header = new HBox(20);
        header.setPadding(new Insets(20));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: rgba(0, 0, 0, 0.3);");
        
        Label title = new Label("🤖 AI Characters");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 36));
        title.setTextFill(Color.web("#ffd700"));
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Button btnCreate = new Button("➕ Create New Character");
        btnCreate.getStyleClass().add("game-button");
        btnCreate.setOnAction(e -> showCreateCharacterDialog());
        
        Button btnRefresh = new Button("🔄 Refresh");
        btnRefresh.getStyleClass().add("game-button");
        btnRefresh.setOnAction(e -> refreshCharacterList());
        
        Button btnBack = new Button("← Back");
        btnBack.getStyleClass().add("game-button");
        btnBack.setOnAction(e -> backToMenu());
        
        header.getChildren().addAll(title, spacer, btnCreate, btnRefresh, btnBack);
        return header;
    }
    
    private HBox createFooter() {
        HBox footer = new HBox(10);
        footer.setPadding(new Insets(15));
        footer.setAlignment(Pos.CENTER);
        footer.setStyle("-fx-background-color: rgba(0, 0, 0, 0.3);");
        
        Button btnLeaderboard = new Button("🏆 View Leaderboard");
        btnLeaderboard.getStyleClass().add("game-button");
        btnLeaderboard.setOnAction(e -> showLeaderboard());
        
        Button btnExport = new Button("💾 Export Stats");
        btnExport.getStyleClass().add("game-button");
        btnExport.setOnAction(e -> exportStats());
        
        footer.getChildren().addAll(btnLeaderboard, btnExport);
        return footer;
    }
    
    private void refreshCharacterList() {
        characterListBox.getChildren().clear();
        
        var characters = characterManager.getCharactersByElo();
        
        if (characters.isEmpty()) {
            Label emptyLabel = new Label("No AI characters yet. Create one to get started!");
            emptyLabel.setTextFill(Color.GRAY);
            emptyLabel.setFont(Font.font(16));
            characterListBox.getChildren().add(emptyLabel);
        } else {
            for (int i = 0; i < characters.size(); i++) {
                AICharacter character = characters.get(i);
                VBox card = createCharacterCard(character, i + 1);
                characterListBox.getChildren().add(card);
                
                // Animate entrance
                animateCardEntrance(card, i);
            }
        }
    }
    
    private VBox createCharacterCard(AICharacter character, int rank) {
        VBox card = new VBox(10);
        card.getStyleClass().add("character-card");
        card.setPadding(new Insets(20));
        card.setMaxWidth(800);
        
        // Header row with rank and name
        HBox headerRow = new HBox(15);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        
        Label rankLabel = new Label("#" + rank);
        rankLabel.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        rankLabel.setTextFill(getRankColor(rank));
        rankLabel.setMinWidth(50);
        
        Label nameLabel = new Label(character.getName());
        nameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 28));
        nameLabel.setTextFill(Color.WHITE);
        
        Label eloLabel = new Label("ELO: " + character.getEloRating());
        eloLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        eloLabel.setTextFill(Color.web("#f39c12"));
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        headerRow.getChildren().addAll(rankLabel, nameLabel, spacer, eloLabel);
        
        // Info grid
        GridPane infoGrid = new GridPane();
        infoGrid.setHgap(30);
        infoGrid.setVgap(10);
        
        addInfoRow(infoGrid, 0, "Level:", character.getStrengthLevel(), Color.web("#3498db"));
        addInfoRow(infoGrid, 1, "Playing Style:", character.getPlayingStyle(), Color.web("#9b59b6"));
        addInfoRow(infoGrid, 2, "Games Played:", String.valueOf(character.getGamesPlayed()), Color.web("#95a5a6"));
        addInfoRow(infoGrid, 3, "Win Rate:", String.format("%.1f%%", character.getWinRate()), Color.web("#27ae60"));
        
        // Personality bars
        VBox personalityBox = new VBox(8);
        Label personalityLabel = new Label("Personality Traits:");
        personalityLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        personalityLabel.setTextFill(Color.WHITE);
        personalityBox.getChildren().add(personalityLabel);
        
        addPersonalityBar(personalityBox, "Aggression", character.getAggression(), Color.web("#e74c3c"));
        addPersonalityBar(personalityBox, "Defense", character.getDefensiveness(), Color.web("#3498db"));
        addPersonalityBar(personalityBox, "Risk Taking", character.getRiskTaking(), Color.web("#f39c12"));
        addPersonalityBar(personalityBox, "Patience", character.getPatience(), Color.web("#9b59b6"));
        addPersonalityBar(personalityBox, "Tactical", character.getTactical(), Color.web("#27ae60"));
        addPersonalityBar(personalityBox, "Positional", character.getPositional(), Color.web("#16a085"));
        
        // Action buttons
        HBox actionBox = new HBox(10);
        actionBox.setAlignment(Pos.CENTER);
        actionBox.setPadding(new Insets(10, 0, 0, 0));
        
        Button btnPlayAgainst = new Button("⚔️ Play Against");
        btnPlayAgainst.getStyleClass().add("game-button");
        btnPlayAgainst.setOnAction(e -> playAgainstCharacter(character));
        
        Button btnViewDetails = new Button("📊 View Details");
        btnViewDetails.getStyleClass().add("game-button");
        btnViewDetails.setOnAction(e -> viewCharacterDetails(character));
        
        Button btnDelete = new Button("🗑️ Delete");
        btnDelete.getStyleClass().addAll("game-button", "cancel-button");
        btnDelete.setOnAction(e -> deleteCharacter(character));
        
        actionBox.getChildren().addAll(btnPlayAgainst, btnViewDetails, btnDelete);
        
        card.getChildren().addAll(headerRow, new Separator(), infoGrid, personalityBox, actionBox);
        return card;
    }
    
    private void addInfoRow(GridPane grid, int row, String label, String value, Color valueColor) {
        Label lblLabel = new Label(label);
        lblLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        lblLabel.setTextFill(Color.web("#95a5a6"));
        
        Label lblValue = new Label(value);
        lblValue.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        lblValue.setTextFill(valueColor);
        
        grid.add(lblLabel, 0, row);
        grid.add(lblValue, 1, row);
    }
    
    private void addPersonalityBar(VBox container, String label, double value, Color color) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        
        Label nameLabel = new Label(label);
        nameLabel.setFont(Font.font(12));
        nameLabel.setTextFill(Color.WHITE);
        nameLabel.setMinWidth(100);
        
        ProgressBar bar = new ProgressBar(value);
        bar.setPrefWidth(200);
        bar.setStyle(String.format("-fx-accent: %s;", toRgbString(color)));
        
        Label valueLabel = new Label(String.format("%.0f%%", value * 100));
        valueLabel.setTextFill(color);
        valueLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        
        row.getChildren().addAll(nameLabel, bar, valueLabel);
        container.getChildren().add(row);
    }
    
    private void animateCardEntrance(VBox card, int index) {
        card.setOpacity(0);
        card.setTranslateX(-50);
        
        FadeTransition fade = new FadeTransition(Duration.millis(400), card);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.setDelay(Duration.millis(index * 80));
        
        TranslateTransition slide = new TranslateTransition(Duration.millis(400), card);
        slide.setFromX(-50);
        slide.setToX(0);
        slide.setDelay(Duration.millis(index * 80));
        
        new ParallelTransition(fade, slide).play();
    }
    
    private void showCreateCharacterDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Create AI Character");
        dialog.setHeaderText("Create a new AI character with custom traits");
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(15);
        grid.setPadding(new Insets(20));
        
        TextField nameField = new TextField();
        nameField.setPromptText("Character name");
        
        Spinner<Integer> eloSpinner = new Spinner<>(400, 2800, 1000, 100);
        eloSpinner.setEditable(true);
        
        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Initial ELO:"), 0, 1);
        grid.add(eloSpinner, 1, 1);
        
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                String name = nameField.getText().trim();
                if (!name.isEmpty()) {
                    try {
                        AICharacter character = characterManager.createCharacter(name, eloSpinner.getValue());
                        refreshCharacterList();
                        
                        Alert success = new Alert(Alert.AlertType.INFORMATION);
                        success.setTitle("Success");
                        success.setHeaderText("Character Created!");
                        success.setContentText(character.toString());
                        success.showAndWait();
                    } catch (IllegalArgumentException e) {
                        showError(e.getMessage());
                    }
                } else {
                    showError("Please enter a character name!");
                }
            }
        });
    }
    
    private void playAgainstCharacter(AICharacter character) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Play Against AI");
        confirm.setHeaderText("Play against " + character.getName() + "?");
        confirm.setContentText(String.format("ELO: %d\nStyle: %s\nLevel: %s",
            character.getEloRating(), character.getPlayingStyle(), character.getStrengthLevel()));
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                // Start game with this AI character
                BoardView boardView = new BoardView(primaryStage, MenuView.GameMode.VS_COMPUTER, 
                                                    character, true);
                primaryStage.getScene().setRoot(boardView.getRoot());
            }
        });
    }
    
    private void viewCharacterDetails(AICharacter character) {
        Alert details = new Alert(Alert.AlertType.INFORMATION);
        details.setTitle("Character Details");
        details.setHeaderText(character.getName());
        
        StringBuilder content = new StringBuilder();
        content.append(String.format("ELO Rating: %d\n", character.getEloRating()));
        content.append(String.format("Strength Level: %s\n", character.getStrengthLevel()));
        content.append(String.format("Playing Style: %s\n\n", character.getPlayingStyle()));
        
        content.append("=== Statistics ===\n");
        content.append(String.format("Games Played: %d\n", character.getGamesPlayed()));
        content.append(String.format("Wins: %d\n", character.getGamesWon()));
        content.append(String.format("Losses: %d\n", character.getGamesLost()));
        content.append(String.format("Draws: %d\n", character.getGamesDraw()));
        content.append(String.format("Win Rate: %.1f%%\n\n", character.getWinRate()));
        
        content.append("=== Performance ===\n");
        content.append(String.format("Brilliant Moves: %d\n", character.getBrilliantMoves()));
        content.append(String.format("Blunders: %d\n", character.getBlunders()));
        content.append(String.format("Average Accuracy: %.1f%%\n\n", character.getAverageAccuracy()));
        
        content.append("=== Personality ===\n");
        content.append(String.format("Aggression: %.0f%%\n", character.getAggression() * 100));
        content.append(String.format("Defensiveness: %.0f%%\n", character.getDefensiveness() * 100));
        content.append(String.format("Risk Taking: %.0f%%\n", character.getRiskTaking() * 100));
        content.append(String.format("Patience: %.0f%%\n", character.getPatience() * 100));
        content.append(String.format("Tactical: %.0f%%\n", character.getTactical() * 100));
        content.append(String.format("Positional: %.0f%%\n", character.getPositional() * 100));
        
        if (!character.getFavoriteOpenings().isEmpty()) {
            content.append("\n=== Favorite Openings ===\n");
            content.append(String.join(", ", character.getFavoriteOpenings()));
        }
        
        TextArea textArea = new TextArea(content.toString());
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setPrefRowCount(20);
        
        details.getDialogPane().setContent(textArea);
        details.showAndWait();
    }
    
    private void deleteCharacter(AICharacter character) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Character");
        confirm.setHeaderText("Delete " + character.getName() + "?");
        confirm.setContentText("This action cannot be undone!");
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                if (characterManager.deleteCharacter(character.getName())) {
                    refreshCharacterList();
                    showInfo("Character deleted successfully!");
                } else {
                    showError("Failed to delete character!");
                }
            }
        });
    }
    
    private void showLeaderboard() {
        Alert leaderboard = new Alert(Alert.AlertType.INFORMATION);
        leaderboard.setTitle("AI Characters Leaderboard");
        leaderboard.setHeaderText("Top AI Characters");
        
        TextArea textArea = new TextArea(characterManager.getLeaderboard());
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setPrefRowCount(20);
        textArea.setFont(Font.font("Courier New", 12));
        
        leaderboard.getDialogPane().setContent(textArea);
        leaderboard.showAndWait();
    }
    
    private void exportStats() {
        String filename = "ai_character_stats_" + System.currentTimeMillis() + ".txt";
        characterManager.exportStats(filename);
        showInfo("Stats exported to: " + filename);
    }
    
    private Color getRankColor(int rank) {
        if (rank == 1) return Color.web("#ffd700"); // Gold
        if (rank == 2) return Color.web("#c0c0c0"); // Silver
        if (rank == 3) return Color.web("#cd7f32"); // Bronze
        return Color.web("#95a5a6");
    }
    
    private String toRgbString(Color color) {
        return String.format("rgb(%d,%d,%d)", 
            (int)(color.getRed() * 255),
            (int)(color.getGreen() * 255),
            (int)(color.getBlue() * 255));
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
    
    private void backToMenu() {
        MenuView menuView = new MenuView(primaryStage);
        primaryStage.getScene().setRoot(menuView.getRoot());
    }
    
    public Parent getRoot() {
        return root;
    }
}