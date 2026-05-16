package arqui.grupo5.vendiasender;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class VendiaSender extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(VendiaSender.class.getResource("sender-view.fxml"));
        Scene scene = new Scene(loader.load(), 720, 580);
        stage.setTitle("VendiaSender — LogiMarket Perú S.A.");
        stage.setResizable(false);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
