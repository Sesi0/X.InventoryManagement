package bg.sit.ui.controllers;

import bg.sit.business.ValidationUtil;
import bg.sit.business.entities.User;
import bg.sit.business.services.UserService;
import bg.sit.business.tasks.DiscardProductTask;
import bg.sit.business.tasks.UpdateProductPricesForArmotizationsTask;
import bg.sit.session.SessionHelper;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javax.validation.ConstraintViolation;

public class LoginPageController implements Initializable {

    @FXML
    private AnchorPane rootPane;

    @FXML
    private TextField txtUsernameField;

    @FXML
    private TextField txtPasswordField;

    @FXML
    private Label lblStatus;

    @FXML
    private Button btnLogin;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        btnLogin.setDefaultButton(true);
        SessionHelper.initDefaults();
    }

    public void btnLogin(ActionEvent event) throws IOException {

        Set<ConstraintViolation<User>> validations = ValidationUtil.getValidator().validateValue(User.class, "username", txtUsernameField.getText());
        validations.addAll(ValidationUtil.getValidator().validateValue(User.class, "password", txtPasswordField.getText()));

        if (!validations.isEmpty()) {
            ValidationUtil.ShowErrors(validations);
        } else {
            UserService usersService = new UserService();
            if (usersService.login(txtUsernameField.getText(), txtPasswordField.getText())) {
                SessionHelper.setCurrentUser(usersService.getUserByUsername(txtUsernameField.getText(), true));
                AnchorPane pane = FXMLLoader.load(getClass().getResource("/fxml/MainPage1.fxml"));
                rootPane.getChildren().setAll(pane);

                // Start tasks
                startTasks();
            } else {
                lblStatus.setText("Login Failed");
            }
        }
    }

    public void EXIT(ActionEvent event) throws IOException {
        System.exit(0);
    }

    private void startTasks() {
        TimerTask timerDiscardProductTask = new DiscardProductTask();
        //running timer task as daemon thread
        Timer timerDiscardProduct = new Timer(true);

        // Repeat 
        timerDiscardProduct.scheduleAtFixedRate(timerDiscardProductTask, 2 * 60 * 1000, 10 * 60 * 1000);

        TimerTask timerUpdateProductPricesForArmotizationsTask = new UpdateProductPricesForArmotizationsTask();
        //running timer task as daemon thread
        Timer timerUpdateProductPricesForArmotizations = new Timer(true);

        // Repeat 
        timerUpdateProductPricesForArmotizations.scheduleAtFixedRate(timerUpdateProductPricesForArmotizationsTask, 2 * 60 * 1000, 10 * 60 * 1000);
    }

}
