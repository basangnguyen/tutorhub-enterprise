import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

public class TestTldraw extends Application {
    @Override
    public void start(Stage primaryStage) {
        WebView webView = new WebView();
        webView.getEngine().loadContent(
            "<!DOCTYPE html>\n" +
            "<html lang='en'>\n" +
            "  <head>\n" +
            "    <meta charset='utf-8' />\n" +
            "    <style>\n" +
            "       html, body, #root { margin: 0; padding: 0; width: 100%; height: 100%; overflow: hidden; }\n" +
            "    </style>\n" +
            "    <link rel='stylesheet' href='https://unpkg.com/@tldraw/tldraw@3.1.0/tldraw.css' />\n" +
            "    <script src='https://unpkg.com/react@18/umd/react.production.min.js'></script>\n" +
            "    <script src='https://unpkg.com/react-dom@18/umd/react-dom.production.min.js'></script>\n" +
            "  </head>\n" +
            "  <body>\n" +
            "    <div id='root'></div>\n" +
            "    <script type='module'>\n" +
            "      import { Tldraw } from 'https://esm.sh/@tldraw/tldraw@3.1.0?bundle';\n" +
            "      const root = ReactDOM.createRoot(document.getElementById('root'));\n" +
            "      root.render(React.createElement(Tldraw));\n" +
            "    </script>\n" +
            "  </body>\n" +
            "</html>"
        );

        BorderPane root = new BorderPane(webView);
        Scene scene = new Scene(root, 800, 600);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Tldraw Test");
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
