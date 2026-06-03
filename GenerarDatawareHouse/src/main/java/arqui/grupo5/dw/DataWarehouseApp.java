package arqui.grupo5.dw;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class DataWarehouseApp extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(DataWarehouseApp.class.getResource("dw-view.fxml"));
        Scene scene = new Scene(loader.load(), 650, 450);
        stage.setTitle("GenerarDatawareHouse — LogiMarket Peru S.A.");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
