package Compartido.helper;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.control.ComboBox;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper para agregar autocompletado a ComboBox
 */
public class AutoCompleteComboBoxListener<T> implements EventHandler<KeyEvent> {

    private final ComboBox<T> comboBox;
    private ObservableList<T> data;
    private boolean moveCaretToPos = false;
    private int caretPos;

    public AutoCompleteComboBoxListener(final ComboBox<T> comboBox) {
        this.comboBox = comboBox;
        this.data = comboBox.getItems();

        this.comboBox.setEditable(true);
        this.comboBox.setOnKeyPressed(this);
        this.comboBox.setOnKeyReleased(this);
    }

    @Override
    public void handle(KeyEvent event) {
        if (event.getCode() == KeyCode.UP) {
            caretPos = -1;
            moveCaret(comboBox.getEditor().getText().length());
            return;
        } else if (event.getCode() == KeyCode.DOWN) {
            if (!comboBox.isShowing()) {
                comboBox.show();
            }
            caretPos = -1;
            moveCaret(comboBox.getEditor().getText().length());
            return;
        } else if (event.getCode() == KeyCode.BACK_SPACE) {
            moveCaretToPos = true;
            caretPos = comboBox.getEditor().getCaretPosition();
        } else if (event.getCode() == KeyCode.DELETE) {
            moveCaretToPos = true;
            caretPos = comboBox.getEditor().getCaretPosition();
        } else if (event.getCode() == KeyCode.ENTER) {
            // 🔥 NUEVO: Manejar Enter para seleccionar
            handleEnterKey();
            return;
        }

        if (event.getCode() == KeyCode.RIGHT || event.getCode() == KeyCode.LEFT
                || event.isControlDown() || event.getCode() == KeyCode.HOME
                || event.getCode() == KeyCode.END || event.getCode() == KeyCode.TAB) {
            return;
        }

        ObservableList<T> list = FXCollections.observableArrayList();
        for (T item : data) {
            String itemText = getItemText(item).toLowerCase();
            String typedText = comboBox.getEditor().getText().toLowerCase();

            if (itemText.contains(typedText)) {
                list.add(item);
            }
        }

        String t = comboBox.getEditor().getText();
        comboBox.setItems(list);
        comboBox.getEditor().setText(t);

        if (!moveCaretToPos) {
            caretPos = -1;
        }

        moveCaret(t.length());

        if (!list.isEmpty()) {
            comboBox.show();
        }
    }

    private void moveCaret(int textLength) {
        if (caretPos == -1) {
            comboBox.getEditor().positionCaret(textLength);
        } else {
            comboBox.getEditor().positionCaret(caretPos);
        }
        moveCaretToPos = false;
    }

    private String getItemText(T item) {
        if (item == null) return "";
        return item.toString();
    }

    private void handleEnterKey() {
        String texto = comboBox.getEditor().getText();

        if (texto == null || texto.isEmpty()) {
            comboBox.setValue(null);
            comboBox.hide();
            return;
        }

        // Buscar coincidencia exacta primero
        for (T item : data) {
            if (getItemText(item).equalsIgnoreCase(texto)) {
                comboBox.setValue(item);
                comboBox.hide();
                return;
            }
        }

        // Si no hay coincidencia exacta, buscar la primera que contenga el texto
        for (T item : data) {
            if (getItemText(item).toLowerCase().contains(texto.toLowerCase())) {
                comboBox.setValue(item);
                comboBox.hide();
                return;
            }
        }

        // Si no encuentra nada, dejar el texto como está
        comboBox.hide();
    }
}