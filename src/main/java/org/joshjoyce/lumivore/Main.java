package org.joshjoyce.lumivore;

import javax.swing.*;
import java.awt.*;

public class Main {
    public static void main(String[] args) {
        var frame = new JFrame("Lumivore");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        var menuBar = new JMenuBar();
        var fileMenu = new JMenu();
        menuBar.add(fileMenu);
        fileMenu.add(new JMenuItem("File"));
        frame.add(menuBar);
        frame.setLayout(new FlowLayout());
        frame.pack();
        frame.setVisible(true);
    }
}
