import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.jar.JarEntry;

public class Frame {
    DefaultListModel<String> blockedUrls = new DefaultListModel<>();
    DefaultListModel<String> visitedUrls = new DefaultListModel<>();
    public File visitedUrlsFile = new File("visitedUrlsFile.txt");
    public Frame() throws FileNotFoundException {
        JFrame frame = new JFrame("Proxy Manager");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(500,500);
        frame.setLayout(new GridLayout(5,1));
//        blockedUrls.addElement("www.google.com");
//        blockedUrls.addElement("youtube.com");
        JScrollPane scrollBar = new JScrollPane();
        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BorderLayout());
        JList list = new JList(blockedUrls);
        JList visitedList = new JList(visitedUrls);
        scrollBar.setViewportView(visitedList);
        visitedList.setLayoutOrientation(JList.VERTICAL);
        list.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        list.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        listPanel.add(scrollBar);
        frame.add(list);
        visitedList.setVisible(false);
        JButton button1 = new JButton("Add");
        JLabel label = new JLabel("Enter Url: ");
        JTextField textField = new JTextField();
        textField.setPreferredSize(new Dimension(400,30));
        button1.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!blockedUrls.contains(textField.getText())){
                    blockedUrls.addElement(textField.getText());
                    textField.setText("");
                }
            }
        });
        JButton button2 = new JButton("Remove");
        button2.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                blockedUrls.remove(list.getSelectedIndex());
            }
        });
        JPanel panel = new JPanel();
        JPanel menu = new JPanel();
        JButton blockedUrlsPage = new JButton("Blocked Urls");
        blockedUrlsPage.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                visitedList.setVisible(false);
                listPanel.setVisible(false);
                panel.setVisible(true);
                list.setVisible(true);
            }
        });
        JButton seenUrlsPage = new JButton("Visited Urls");
        seenUrlsPage.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String url;
                Scanner reader = null;
                try {
                    reader = new Scanner(visitedUrlsFile);
                } catch (FileNotFoundException fileNotFoundException) {
                    fileNotFoundException.printStackTrace();
                }
                while (reader.hasNextLine()){
                    url = reader.nextLine();
                    if (!visitedUrls.contains(url)) visitedUrls.addElement(url);
                }
                reader.close();
                visitedList.setVisible(true);
                listPanel.setVisible(true);
                panel.setVisible(false);
                list.setVisible(false);
            }
        });
        JButton exitButton = new JButton("Exit");
        exitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                frame.dispose();
                System.exit(0);
            }
        });
        menu.add(blockedUrlsPage);
        menu.add(seenUrlsPage);
        panel.setLayout(new FlowLayout());
        panel.add(label);
        panel.add(textField);
        panel.add(button1);
        panel.add(button2);
        frame.add(panel);
        frame.add(listPanel);
        frame.add(menu);
        frame.add(exitButton);
        frame.setVisible(true);
    }
    public ArrayList<String> getBlockedUrls(){
        ArrayList<String> blockedUrlArray = new ArrayList<>();
        for (int i = 0; i < blockedUrls.size() ; i++) {
            blockedUrlArray.add(blockedUrls.get(i));
        }
        return blockedUrlArray;
    }
    public void updateVisitedUrls(ArrayList<String > visitedUrls){
        for (int i = 0; i <visitedUrls.size() ; i++) {
            if (!this.visitedUrls.contains(visitedUrls.get(i))) this.visitedUrls.addElement(visitedUrls.get(i));
        }
    }
}
