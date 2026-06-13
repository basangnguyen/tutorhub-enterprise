package com.mycompany.tutorhub_enterprise.client.exam.ui;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class TSEInputSwingAdapter {

    private static boolean skipNextInput = false;

    /**
     * Scan the container hierarchy and attach the Telex engine to any JTextComponent
     * that has the client property "tse.inputModeEnabled" set to true.
     */
    public static void installForOptIn(Container root) {
        for (Component c : root.getComponents()) {
            if (c instanceof JTextComponent) {
                JTextComponent textComp = (JTextComponent) c;
                Object enabledProp = textComp.getClientProperty("tse.inputModeEnabled");
                if (Boolean.TRUE.equals(enabledProp) && !(textComp instanceof JPasswordField)) {
                    attach(textComp);
                }
            } else if (c instanceof Container) {
                installForOptIn((Container) c);
            }
        }
    }

    /**
     * Explicitly attach the Telex engine to a single component.
     */
    public static void attach(JTextComponent component) {
        if (component instanceof JPasswordField) {
            return;
        }

        // Listener to detect modifier keys (Ctrl/Alt/Meta) to skip Telex transformation
        component.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.isControlDown() || e.isAltDown() || e.isMetaDown()) {
                    skipNextInput = true;
                }
            }
            @Override
            public void keyReleased(KeyEvent e) {
                if (!e.isControlDown() && !e.isAltDown() && !e.isMetaDown()) {
                    skipNextInput = false;
                }
            }
        });

        Document doc = component.getDocument();
        if (doc instanceof AbstractDocument) {
            AbstractDocument abstractDoc = (AbstractDocument) doc;
            // Avoid adding multiple filters if called multiple times
            if (!(abstractDoc.getDocumentFilter() instanceof TelexDocumentFilter)) {
                abstractDoc.setDocumentFilter(new TelexDocumentFilter(abstractDoc.getDocumentFilter()));
            }
        }
    }

    private static class TelexDocumentFilter extends DocumentFilter {
        private final DocumentFilter parentFilter;

        public TelexDocumentFilter(DocumentFilter parentFilter) {
            this.parentFilter = parentFilter;
        }

        @Override
        public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
            if (handleTelex(fb, offset, 0, string, attr)) {
                return;
            }
            if (parentFilter != null) {
                parentFilter.insertString(fb, offset, string, attr);
            } else {
                super.insertString(fb, offset, string, attr);
            }
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
            if (handleTelex(fb, offset, length, text, attrs)) {
                return;
            }
            if (parentFilter != null) {
                parentFilter.replace(fb, offset, length, text, attrs);
            } else {
                super.replace(fb, offset, length, text, attrs);
            }
        }

        @Override
        public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
            if (parentFilter != null) {
                parentFilter.remove(fb, offset, length);
            } else {
                super.remove(fb, offset, length);
            }
        }

        private boolean handleTelex(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
            if (text == null || text.isEmpty()) {
                return false;
            }
            
            // Only process single character insertions when in VIE mode and no modifiers active.
            // If length > 0, it means text is being replaced (like selecting text and typing).
            // We only support transforming when length == 0 (pure insertion) to avoid complex selection replace bugs.
            if (!"vi".equals(TSEInputModeManager.getInstance().getMode()) || skipNextInput || text.length() > 1 || length > 0) {
                return false;
            }

            Document doc = fb.getDocument();
            int start = offset;
            String docText = doc.getText(0, doc.getLength());

            // Find start of current word
            while (start > 0 && TSEVietnameseTelexEngine.isWordChar(docText.charAt(start - 1))) {
                start--;
            }

            if (start == offset && !TSEVietnameseTelexEngine.isWordChar(text.charAt(0))) {
                return false; // not appending to a word
            }

            String currentWord = docText.substring(start, offset);
            String wordToTransform = currentWord + text;
            String transformed = TSEVietnameseTelexEngine.transformWord(wordToTransform);

            if (!transformed.equals(wordToTransform)) {
                // If transformed differs, replace the word part
                super.replace(fb, start, offset - start + length, transformed, attrs);
                return true;
            }

            return false;
        }
    }
}
