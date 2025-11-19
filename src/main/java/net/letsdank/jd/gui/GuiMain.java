package net.letsdank.jd.gui;

import javax.swing.*;

/**
 * Точка входа для GUI-версии.
 */
public final class GuiMain {
    public static void main(String[] args) {
        //  Используем системный L&F, чтобы на KDE выглядело максимально нативно
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        SwingUtilities.invokeLater(() -> {
            DecompilerFrame frame = new DecompilerFrame();
            frame.setVisible(true);
        });
    }
}
