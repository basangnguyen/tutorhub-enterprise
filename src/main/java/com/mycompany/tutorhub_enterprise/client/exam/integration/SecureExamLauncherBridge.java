package com.mycompany.tutorhub_enterprise.client.exam.integration;

import java.io.File;
import java.util.logging.Logger;

public class SecureExamLauncherBridge {
    private static final Logger LOGGER = Logger.getLogger(SecureExamLauncherBridge.class.getName());

    public static void launch() throws Exception {
        launchExam(-1);
    }

    public static void launchExam(int examId) throws Exception {
        File runBat = findExecutable();
        if (runBat == null || !runBat.exists()) {
            throw new Exception("Launcher not found.");
        }

        LOGGER.info("Launching Secure Exam [2I.9.5 Diagnostics]");
        LOGGER.info("Resolved launcher path: " + runBat.getAbsolutePath());
        LOGGER.info("Selected launcher file: " + runBat.getName());

        ProcessBuilder pb;
        if (examId > 0) {
            LOGGER.info("Command args: --exam-id " + examId);
            pb = new ProcessBuilder("cmd", "/c", "start", "\"\"", runBat.getName(), "--exam-id", String.valueOf(examId));
        } else {
            LOGGER.info("Command args: <none>");
            pb = new ProcessBuilder(runBat.getName());
        }
        
        if (runBat.getName().equals("run_gui.bat")) {
            // Priority: Use javaw.exe directly to avoid any cmd window flash
            File appHome = runBat.getParentFile();
            File javaExe = new File(appHome, "runtime/bin/javaw.exe");
            File jarFile = new File(appHome, "app/TutorHub_Maven-1.0-SNAPSHOT-jar-with-dependencies.jar");
            File configFile = new File(appHome, "app/application.properties");
            String mainClass = "com.mycompany.tutorhub_enterprise.client.exam.ui.TSEProductionParentSubmitLabLauncher";

            if (javaExe.exists() && jarFile.exists()) {
                if (examId > 0) {
                    pb = new ProcessBuilder(javaExe.getAbsolutePath(), 
                        "-Dtutorhub.app.root=" + appHome.getAbsolutePath(),
                        "-Dtutorhub.app.jar=" + jarFile.getAbsolutePath(),
                        "-Dtutorhub.config=" + configFile.getAbsolutePath(),
                        "-cp", jarFile.getAbsolutePath(),
                        mainClass,
                        "--exam-id", String.valueOf(examId));
                } else {
                    pb = new ProcessBuilder(javaExe.getAbsolutePath(), 
                        "-Dtutorhub.app.root=" + appHome.getAbsolutePath(),
                        "-Dtutorhub.app.jar=" + jarFile.getAbsolutePath(),
                        "-Dtutorhub.config=" + configFile.getAbsolutePath(),
                        "-cp", jarFile.getAbsolutePath(),
                        mainClass);
                }
            } else {
                // Fallback to run_gui.bat if files are missing
                if (examId > 0) {
                    pb = new ProcessBuilder(runBat.getAbsolutePath(), "--exam-id", String.valueOf(examId));
                } else {
                    pb = new ProcessBuilder(runBat.getAbsolutePath());
                }
            }
        } else {
            if (examId > 0) {
                pb = new ProcessBuilder("cmd", "/c", "start", "\"\"", runBat.getName(), "--exam-id", String.valueOf(examId));
            } else {
                pb = new ProcessBuilder("cmd", "/c", "start", "\"\"", runBat.getName());
            }
        }
        
        pb.directory(runBat.getParentFile()); // Set working directory
        pb.start();
    }

    private static File findExecutable() {
        // 1. Check System Property
        String sysProp = System.getProperty("tutorhub.secureExam.runBat");
        if (sysProp != null && new File(sysProp).exists()) {
            return new File(sysProp);
        }

        // 2. Check common paths
        String localAppData = System.getenv("LOCALAPPDATA");
        String programFiles = System.getenv("ProgramFiles");
        String programFilesX86 = System.getenv("ProgramFiles(x86)");

        String[] bases = {
            // Priority 1: Dev mode local builds (useful for testing latest changes without installing)
            System.getProperty("user.dir") + "\\dist\\TutorHubSecureExam\\",
            System.getProperty("user.dir") + "\\dist\\TutorHubSecureExam_jlink_lab\\",
            // Priority 2: Installed paths
            localAppData != null ? localAppData + "\\TutorHubSecureExam\\" : null,
            localAppData != null ? localAppData + "\\Programs\\TutorHubSecureExam\\" : null,
            programFiles != null ? programFiles + "\\TutorHubSecureExam\\" : null,
            programFilesX86 != null ? programFilesX86 + "\\TutorHubSecureExam\\" : null
        };

        for (String base : bases) {
            if (base != null) {
                File runGui = new File(base + "run_gui.bat");
                if (runGui.exists()) {
                    return runGui;
                }
                File runBat = new File(base + "run.bat");
                if (runBat.exists()) {
                    return runBat;
                }
            }
        }

        return null;
    }
}
