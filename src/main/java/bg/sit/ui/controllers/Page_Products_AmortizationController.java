/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bg.sit.ui.controllers;

import bg.sit.business.ValidationUtil;
import bg.sit.business.entities.Amortization;
import bg.sit.business.enums.RoleType;
import bg.sit.business.services.AmortizationService;
import bg.sit.session.SessionHelper;
import bg.sit.ui.MessagesUtil;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.Set;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javax.validation.ConstraintViolation;

public class Page_Products_AmortizationController implements Initializable {

    AmortizationService amortizationService;
    @FXML
    private TableView<Amortization> table;
    @FXML
    private TableColumn<Amortization, String> nameColumn;
    @FXML
    private TableColumn<Amortization, Double> DecPriceColumn;
    @FXML
    private TableColumn<Amortization, Integer> perDayColumn;
    @FXML
    private TableColumn<Amortization, Integer> RLColumn;
    @FXML
    private ComboBox<String> CBDelete;
    @FXML
    private TextField txtName;
    @FXML
    private TextField txtDecPrice;
    @FXML
    private TextField txtPerDay;
    @FXML
    private TextField txtRL;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        SessionHelper.getCurrentUser();
        setTable();
        CBDelete.setItems(FXCollections.observableArrayList("Деактивиране", "Принудително изтриване"));
    }

    private void initTable() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<Amortization, String>("name"));
        DecPriceColumn.setCellValueFactory(new PropertyValueFactory<Amortization, Double>("price"));
        perDayColumn.setCellValueFactory(new PropertyValueFactory<Amortization, Integer>("days"));
        RLColumn.setCellValueFactory(new PropertyValueFactory<Amortization, Integer>("repeatLimit"));
    }

    private void setTable() {
        initTable();
        amortizationService = new AmortizationService();
        if (SessionHelper.getCurrentUser().getRoleType() == RoleType.ADMIN) {
            table.getItems().addAll(FXCollections.observableArrayList(amortizationService.getAmortizations()));
        } else {
            table.getItems().addAll(FXCollections.observableArrayList(amortizationService.getAmortizations(SessionHelper.getCurrentUser().getId())));
        }
    }

    private void clearForm() {
        table.getItems().clear();
    }

    public void ADD(ActionEvent event) {
        Amortization amort = new Amortization();

        try {
            amort.setPrice(Double.parseDouble(txtDecPrice.getText()));
            amort.setRepeatLimit(Integer.parseInt(txtRL.getText()));
            amort.setDays(Integer.parseInt(txtPerDay.getText()));
            amort.setName(txtName.getText());
        } catch (Exception e) {
            MessagesUtil.showMessage("Невалидни или празни дании!", Alert.AlertType.ERROR);
            return;
        }

        Set<ConstraintViolation<Amortization>> validations = ValidationUtil.getValidator().validate(amort);

        if (!validations.isEmpty()) {
            ValidationUtil.ShowErrors(validations);
        } else {

            clearForm();
            amortizationService = new AmortizationService();
            amortizationService.addAmortization(txtName.getText(), Double.parseDouble(txtDecPrice.getText()), Integer.parseInt(txtPerDay.getText()), Integer.parseInt(txtRL.getText()));
            setTable();
        }
        txtName.clear();
        txtDecPrice.clear();
        txtPerDay.clear();
        txtRL.clear();
    }

    public void EDIT(ActionEvent event) throws IOException {
        Amortization amort = new Amortization();

        try {
            amort.setPrice(Double.parseDouble(txtDecPrice.getText()));
            amort.setRepeatLimit(Integer.parseInt(txtRL.getText()));
            amort.setDays(Integer.parseInt(txtPerDay.getText()));
            amort.setName(txtName.getText());
        } catch (Exception e) {
            MessagesUtil.showMessage("Невалидни или празни дании!", Alert.AlertType.ERROR);
            return;
        }

        Set<ConstraintViolation<Amortization>> validations = ValidationUtil.getValidator().validate(amort);

        Amortization amortization = table.getSelectionModel().getSelectedItem();
        if (amortization != null) {
            amortizationService = new AmortizationService();
            amortizationService.updateAmortization(amortization.getId(), txtName.getText(), Double.parseDouble(txtDecPrice.getText()), Integer.parseInt(txtPerDay.getText()), Integer.parseInt(txtRL.getText()));

        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Опс...");
            alert.setHeaderText("Моля, посочете потребителя който искате да промените.");
            alert.showAndWait();
        }
        clearForm();
        setTable();
        txtName.clear();
        txtDecPrice.clear();
        txtPerDay.clear();
        txtRL.clear();
    }

    public void DELETE(ActionEvent event) throws IOException {
        amortizationService = new AmortizationService();//TODO
        Amortization amortization = table.getSelectionModel().getSelectedItem();
        if (amortization != null) {

            if (CBDelete.getSelectionModel().getSelectedIndex() == 2) {
                amortizationService.forceDeleteAmortization(amortization.getId());
            } else {
                amortizationService.deleteAmortization(amortization.getId());
            }
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Опс...");
            alert.setHeaderText("Моля, посочете потребителя който искате да изтриете.");
            alert.showAndWait();
        }
        clearForm();
        setTable();
        CBDelete.valueProperty().set(null);
    }
}
