package com.launcher.ui.settings;

import javax.swing.*;
import javax.swing.text.AbstractDocument;
import javax.swing.text.DocumentFilter;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class SettingsGeneral extends JPanel {
    private Properties settings;

    public SettingsGeneral(Properties settings) {
        this.settings = settings;

        // Основная панель с вертикальным BoxLayout (каждая строка идёт под предыдущей)
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);
        setBackground(Color.BLACK);

        // ------------------ Первая строка (Макс. ОЗУ) ------------------
        JPanel ramPanel = new JPanel();
        ramPanel.setLayout(new BoxLayout(ramPanel, BoxLayout.X_AXIS));
        ramPanel.setOpaque(false);
        // Выравниваем панель по левой границе внутри BoxLayout
        ramPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel ramLabel = new JLabel("Макс. ОЗУ (ГБ):");
        ramLabel.setForeground(Color.WHITE);
        ramPanel.add(ramLabel);

        // Поле для ввода ОЗУ (короткое)
        JTextField ramField = new JTextField(settings.getProperty("ram", "2"));
        ramField.setColumns(2); // влияет на «минимальную» ширину
        Dimension fieldSize = new Dimension(40, ramField.getPreferredSize().height);
        ramField.setPreferredSize(fieldSize);
        ramField.setMaximumSize(fieldSize);

        ramField.setBackground(Color.BLACK);
        ramField.setForeground(Color.WHITE);
        ramField.setBorder(BorderFactory.createLineBorder(Color.WHITE));

        // Разрешаем ввод только цифр
        ((AbstractDocument) ramField.getDocument()).setDocumentFilter(new DigitFilter());

        // При потере фокуса сохраняем значение
        ramField.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent e) {
                settings.setProperty("ram", ramField.getText().trim());
                saveSettings();
            }
        });
        ramPanel.add(ramField);

        // Добавляем строку "Макс. ОЗУ" в панель
        add(ramPanel);

        // ------------------ Вторая строка (Скрывать лаунчер) ------------------
        JPanel checkboxPanel = new JPanel();
        checkboxPanel.setLayout(new BoxLayout(checkboxPanel, BoxLayout.X_AXIS));
        checkboxPanel.setOpaque(false);
        // Выравниваем панель по левой границе внутри BoxLayout
        checkboxPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JCheckBox hideLauncherCheckBox = new JCheckBox(
                "Скрывать лаунчер",
                Boolean.parseBoolean(settings.getProperty("hideLauncher", "true"))
        );
        hideLauncherCheckBox.setOpaque(false);  // полностью прозрачный фон
        hideLauncherCheckBox.setForeground(Color.WHITE);
        hideLauncherCheckBox.setBorder(BorderFactory.createEmptyBorder());

        hideLauncherCheckBox.addActionListener(e -> {
            settings.setProperty("hideLauncher", Boolean.toString(hideLauncherCheckBox.isSelected()));
            saveSettings();
        });
        checkboxPanel.add(hideLauncherCheckBox);

        // Добавляем строку с чекбоксом сразу под строкой "Макс. ОЗУ"
        add(checkboxPanel);
    }

    private void saveSettings() {
        File file = new File("settings.txt");
        try (FileOutputStream fos = new FileOutputStream(file)) {
            settings.store(fos, "Launcher Settings");
            fos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Фильтр для ввода только цифр
    private static class DigitFilter extends DocumentFilter {
        @Override
        public void insertString(DocumentFilter.FilterBypass fb, int offset, String string,
                                 javax.swing.text.AttributeSet attr)
                throws javax.swing.text.BadLocationException {
            if (string.matches("\\d+")) {
                super.insertString(fb, offset, string, attr);
            }
        }

        @Override
        public void replace(DocumentFilter.FilterBypass fb, int offset, int length, String text,
                            javax.swing.text.AttributeSet attrs)
                throws javax.swing.text.BadLocationException {
            if (text.matches("\\d+")) {
                super.replace(fb, offset, length, text, attrs);
            }
        }
    }
}
