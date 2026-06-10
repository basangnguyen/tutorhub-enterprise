
package com.mycompany.tutorhub_enterprise.client;

import com.calendarfx.model.Calendar;
import com.calendarfx.model.CalendarEvent;
import com.calendarfx.model.CalendarSource;
import com.calendarfx.model.Entry;
import com.calendarfx.view.CalendarView;
import com.mycompany.tutorhub_enterprise.models.CalendarEventModel;
import com.mycompany.tutorhub_enterprise.models.CalendarPollModel;
import com.mycompany.tutorhub_enterprise.models.CalendarTaskModel;
import com.mycompany.tutorhub_enterprise.server.dao.CalendarEventDAO;
import com.mycompany.tutorhub_enterprise.server.dao.CalendarPollDAO;
import com.mycompany.tutorhub_enterprise.server.dao.CalendarTaskDAO;
import com.mycompany.tutorhub_enterprise.server.dao.TutorScheduleDAO;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.File;

import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.TextAlignment;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import org.controlsfx.control.PopOver;

public class ScheduleTab
extends JPanel {
    private final TutorScheduleDAO scheduleDAO = new TutorScheduleDAO();
    private final CalendarEventDAO eventDAO = new CalendarEventDAO();
    private final CalendarTaskDAO taskDAO = new CalendarTaskDAO();
    private final CalendarPollDAO pollDAO = new CalendarPollDAO();
    private int currentTutorId = 2;
    private JFXPanel fxPanel;
    private CalendarSource myCalendarSource;
    private EventHandler<CalendarEvent> dragDropHandler;
    private final Map<String, CalendarEventModel> entryModelMap = new HashMap<String, CalendarEventModel>();
    private final Map<String, CalendarPollModel> entryPollMap = new HashMap<String, CalendarPollModel>();
    private CalendarView calendarViewRef;
    private int calendarColorCounter = 0;
    private VBox sideListBox;
    private StackPane rightPanelWrapper;

    public ScheduleTab() {
        this.setLayout(new BorderLayout());
        this.setBackground(Color.decode("#F8FAFC"));
        this.fxPanel = new JFXPanel();
        this.add((Component)this.fxPanel, "Center");
        Platform.runLater(() -> this.initFX(this.fxPanel));
    }

    private void initFX(JFXPanel fxPanel) {
        CalendarView calendarView;
        this.calendarViewRef = calendarView = new CalendarView();
        calendarView.setTransitionsEnabled(true);
        calendarView.setShowToolBar(false);
        calendarView.setShowSearchField(false);
        calendarView.setShowPrintButton(false);
        calendarView.setShowPageToolBarControls(false);
        calendarView.setShowSourceTray(false);
        calendarView.showMonthPage();
        this.myCalendarSource = new CalendarSource("TutorHub Lịch Của T\u00F4i");
        calendarView.getCalendarSources().setAll(new CalendarSource[]{this.myCalendarSource});
        calendarView.setCalendarSourceFactory(param -> {
            Platform.runLater(() -> this.openCreateEventPopup(calendarView, LocalDate.now(), LocalTime.now()));
            return new CalendarSource("__TEMP__");
        });
        this.dragDropHandler = event -> {
            if (event.getEventType().equals(CalendarEvent.ENTRY_INTERVAL_CHANGED)) {
                Entry entry = event.getEntry();
                try {
                    final int id = Integer.parseInt(entry.getId());
                    LocalDate newDate = entry.getStartDate();
                    final LocalTime newStart = entry.getStartTime();
                    final LocalTime newEnd = entry.getEndTime();
                    int currentJavaDay = newDate.getDayOfWeek().getValue();
                    final int targetDBDayOfWeek = currentJavaDay == 7 ? 1 : currentJavaDay + 1;
                    SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>(){

                        @Override
                        protected Void doInBackground() throws Exception {
                            ScheduleTab.this.eventDAO.updateEventTime(id, newStart, newEnd, targetDBDayOfWeek);
                            return null;
                        }

                        @Override
                        protected void done() {
                            ScheduleTab.this.reloadCalendarData();
                        }
                    };
                    worker.execute();
                }
                catch (NumberFormatException numberFormatException) {
                    // empty catch block
                }
            }
        };
        calendarView.setEntryFactory(param -> {
            LocalDate clickedDate = param.getZonedDateTime().toLocalDate();
            LocalTime clickedTime = param.getZonedDateTime().toLocalTime();
            this.openCreateEventPopup(calendarView, clickedDate, clickedTime);
            return null;
        });
        calendarView.setEntryDetailsPopOverContentCallback(param -> {
            Entry entry = param.getEntry();
            String entryId = entry.getId();
            CalendarEventModel evModel = this.entryModelMap.get(entryId);
            CalendarPollModel pollModel = this.entryPollMap.get(entryId);
            return this.buildPopoverContentNode(evModel, pollModel, entry.getTitle(), entry.getStartDate(), entry.getStartTime(), entry.getEndTime(), entry.isFullDay(), () -> param.getPopOver().hide());
        });
        this.reloadCalendarData();
        calendarView.setRequestedTime(LocalTime.now());
        this.sideListBox = new VBox(12.0);
        this.sideListBox.setPadding(new Insets(16.0, 12.0, 16.0, 0.0));
        this.buildSidePanelContent();
        ScrollPane sideScroll = new ScrollPane((Node)this.sideListBox);
        sideScroll.setFitToWidth(true);
        sideScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sideScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
        sideScroll.setPrefWidth(300.0);
        sideScroll.setMaxWidth(300.0);
        BorderPane mainLayout = new BorderPane();
        mainLayout.setCenter((Node)calendarView);
        mainLayout.setStyle("-fx-background-color: #FFFFFF;");
        this.rightPanelWrapper = new StackPane(new Node[]{sideScroll});
        this.rightPanelWrapper.setStyle("-fx-border-color: #E8ECF0; -fx-border-width: 0 0 0 1; -fx-background-color: #FAFBFC;");
        mainLayout.setRight((Node)this.rightPanelWrapper);
        mainLayout.setTop((Node)this.buildCustomToolbar(calendarView));
        Scene scene = new Scene((Parent)mainLayout);
        try {
            URL cssUrl = this.getClass().getResource("/calendar-theme.css");
            if (cssUrl != null) {
                calendarView.getStylesheets().add(cssUrl.toExternalForm());
            }
        }
        catch (Exception var7) {
            var7.printStackTrace();
        }
        fxPanel.setScene(scene);
    }

    private HBox buildCustomToolbar(CalendarView calendarView) {
        HBox toolbar = new HBox(16.0);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(12.0, 20.0, 12.0, 20.0));
        toolbar.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: transparent transparent #E8ECF0 transparent; -fx-border-width: 0 0 1px 0;");
        Label lblTitle = new Label("\ud83d\udcc5 Lịch d\u1EA1y");
        lblTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1E293B;");
        Button btnToday = new Button("H\u00F4m nay");
        btnToday.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #DADCE0; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-text-fill: #3C4043; -fx-font-weight: bold; -fx-font-size: 13px; -fx-padding: 6px 14px; -fx-cursor: hand;");
        btnToday.setOnAction(e -> calendarView.setDate(LocalDate.now()));
        Button btnPrev = new Button("<");
        btnPrev.setStyle(btnToday.getStyle());
        Button btnNext = new Button(">");
        btnNext.setStyle(btnToday.getStyle());
        HBox navBox = new HBox(0.0, new Node[]{btnPrev, btnNext});
        Label lblDate = new Label();
        lblDate.setStyle("-fx-font-size: 18px; -fx-text-fill: #3C4043;");
        DateTimeFormatter monthYearFmt = DateTimeFormatter.ofPattern("'Th\u00E1ng' M, yyyy", new Locale("vi"));
        calendarView.dateProperty().addListener((obs, oldVal, newVal) -> lblDate.setText(newVal.format(monthYearFmt)));
        lblDate.setText(calendarView.getDate().format(monthYearFmt));
        Region spacer = new Region();
        HBox.setHgrow((Node)spacer, (Priority)Priority.ALWAYS);
        ToggleGroup viewGroup = new ToggleGroup();
        btnPrev.setOnAction(e -> {
            Toggle t = viewGroup.getSelectedToggle();
            if (t != null && t.getUserData().equals("Day")) {
                calendarView.setDate(calendarView.getDate().minusDays(1L));
            } else if (t != null && t.getUserData().equals("Week")) {
                calendarView.setDate(calendarView.getDate().minusWeeks(1L));
            } else if (t != null && t.getUserData().equals("Month")) {
                calendarView.setDate(calendarView.getDate().minusMonths(1L));
            } else if (t != null && t.getUserData().equals("Year")) {
                calendarView.setDate(calendarView.getDate().minusYears(1L));
            }
        });
        btnNext.setOnAction(e -> {
            Toggle t = viewGroup.getSelectedToggle();
            if (t != null && t.getUserData().equals("Day")) {
                calendarView.setDate(calendarView.getDate().plusDays(1L));
            } else if (t != null && t.getUserData().equals("Week")) {
                calendarView.setDate(calendarView.getDate().plusWeeks(1L));
            } else if (t != null && t.getUserData().equals("Month")) {
                calendarView.setDate(calendarView.getDate().plusMonths(1L));
            } else if (t != null && t.getUserData().equals("Year")) {
                calendarView.setDate(calendarView.getDate().plusYears(1L));
            }
        });
        String unselectedStyle = "-fx-background-color: transparent; -fx-text-fill: #3C4043; -fx-font-weight: bold; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 6px 14px; -fx-background-radius: 6px;";
        String selectedStyle = "-fx-background-color: #E8F0FE; -fx-text-fill: #1A73E8; -fx-font-weight: bold; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 6px 14px; -fx-background-radius: 6px;";
        ToggleButton btnDay = new ToggleButton("Ng\u00E0y");
        btnDay.setUserData("Day");
        ToggleButton btnWeek = new ToggleButton("Tu\u1EA7n");
        btnWeek.setUserData("Week");
        ToggleButton btnMonth = new ToggleButton("Th\u00E1ng");
        btnMonth.setUserData("Month");
        ToggleButton btnYear = new ToggleButton("N\u0103m");
        btnYear.setUserData("Year");
        btnDay.setToggleGroup(viewGroup);
        btnWeek.setToggleGroup(viewGroup);
        btnMonth.setToggleGroup(viewGroup);
        btnYear.setToggleGroup(viewGroup);
        viewGroup.selectToggle((Toggle)btnWeek);
        Consumer<ToggleButton> updateStyles = t -> {
            btnDay.setStyle(unselectedStyle);
            btnWeek.setStyle(unselectedStyle);
            btnMonth.setStyle(unselectedStyle);
            btnYear.setStyle(unselectedStyle);
            if (t != null) {
                t.setStyle(selectedStyle);
            }
        };
        updateStyles.accept(btnWeek);
        viewGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                viewGroup.selectToggle(oldVal);
            } else {
                updateStyles.accept((ToggleButton)newVal);
                if (newVal == btnDay) {
                    calendarView.showDayPage();
                } else if (newVal == btnWeek) {
                    calendarView.showMonthPage();
                } else if (newVal == btnMonth) {
                    calendarView.showMonthPage();
                } else if (newVal == btnYear) {
                    calendarView.showYearPage();
                }
            }
        });
        HBox viewSwitcher = new HBox(4.0, new Node[]{btnDay, btnWeek, btnMonth, btnYear});
        viewSwitcher.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #DADCE0; -fx-border-radius: 6px; -fx-padding: 2px;");
        Button btnAdd = new Button("+ T\u1EA1o lịch");
        btnAdd.setStyle("-fx-background-color: #1A73E8; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px; -fx-padding: 6px 16px; -fx-background-radius: 6px; -fx-cursor: hand;");
        btnAdd.setOnAction(e -> this.openCreateEventPopup(calendarView, calendarView.getDate(), LocalTime.now()));
        boolean isAllDayShowing = true;
        if (calendarView.getWeekPage() != null && calendarView.getWeekPage().getDetailedWeekView() != null) {
            isAllDayShowing = calendarView.getWeekPage().getDetailedWeekView().isShowAllDayView();
        }
        Button btnToggleAllDay = new Button(isAllDayShowing ? "Thu g\u1ECDn All Day \u2227" : "M\u1EDF All Day \u2228");
        btnToggleAllDay.setStyle("-fx-background-color: transparent; -fx-border-color: #DADCE0; -fx-border-radius: 6px; -fx-text-fill: #5F6368; -fx-font-size: 12px; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 5px 12px;");
        btnToggleAllDay.setOnAction(e -> {
            boolean currentShowing = calendarView.getWeekPage().getDetailedWeekView().isShowAllDayView();
            calendarView.getWeekPage().getDetailedWeekView().setShowAllDayView(!currentShowing);
            if (calendarView.getDayPage().getDetailedDayView() != null) {
                calendarView.getDayPage().getDetailedDayView().setShowAllDayView(!currentShowing);
            }
            btnToggleAllDay.setText(!currentShowing ? "Thu g\u1ECDn All Day \u2227" : "M\u1EDF All Day \u2228");
        });
        Button btnTogglePanel = new Button("\u2630 Panel");
        btnTogglePanel.setStyle(btnToday.getStyle());
        btnTogglePanel.setOnAction(e -> {
            if (this.rightPanelWrapper != null) {
                boolean isVisible = this.rightPanelWrapper.isVisible();
                this.rightPanelWrapper.setVisible(!isVisible);
                this.rightPanelWrapper.setManaged(!isVisible);
            }
        });
        toolbar.getChildren().addAll(new Node[]{lblTitle, new Label("   "), btnToday, navBox, lblDate, spacer, btnToggleAllDay, viewSwitcher, btnAdd, btnTogglePanel});
        return toolbar;
    }

    private ImageView getWebIcon(String url, double size) {
        try {
            ImageView icon = new ImageView(new Image(url, true));
            icon.setFitWidth(size);
            icon.setFitHeight(size);
            icon.setPreserveRatio(true);
            icon.setSmooth(true);
            return icon;
        }
        catch (Exception var5) {
            return new ImageView();
        }
    }

    private List<String> generateTimeOptions() {
        ArrayList<String> times = new ArrayList<String>();
        for (int h = 0; h < 24; ++h) {
            times.add(String.format("%02d:00", h));
            times.add(String.format("%02d:30", h));
        }
        return times;
    }

    private String formatTimeCustom(LocalTime time) {
        int minute = time.getMinute() < 30 ? 0 : 30;
        return String.format("%02d:%02d", time.getHour(), minute);
    }

    private LocalTime parseTimeString(String timeStr) {
        try {
            String[] parts = timeStr.split(":");
            return LocalTime.of(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
        }
        catch (Exception var3) {
            return LocalTime.of(8, 0);
        }
    }

    private void reloadCalendarData() {
        this.myCalendarSource.getCalendars().clear();
        this.entryModelMap.clear();
        this.entryPollMap.clear();
        this.calendarColorCounter = 0;
        Calendar.Style[] colorStyles = new Calendar.Style[]{Calendar.Style.STYLE1, Calendar.Style.STYLE2, Calendar.Style.STYLE3, Calendar.Style.STYLE4, Calendar.Style.STYLE5, Calendar.Style.STYLE6, Calendar.Style.STYLE7};
        Calendar taskCalendar = new Calendar("\ud83d\udccb Việc c\u1EA7n l\u00E0m của t\u00F4i");
        taskCalendar.setStyle(Calendar.Style.STYLE4);
        this.myCalendarSource.getCalendars().add(taskCalendar);
        Calendar pollCalendar = new Calendar("\ud83d\udcca Kh\u1EA3o s\u00E1t lịch");
        pollCalendar.setStyle(Calendar.Style.STYLE6);
        this.myCalendarSource.getCalendars().add(pollCalendar);
        HashMap<String, Calendar> dynamicCalendars = new HashMap<String, Calendar>();
        for (CalendarEventModel evModel : this.eventDAO.getEventsByTutor(this.currentTutorId)) {
            String groupKey = "event_" + evModel.getEventId();
            if (!dynamicCalendars.containsKey(groupKey)) {
                Calendar newCal = new Calendar(evModel.getTitle());
                Calendar.Style style = colorStyles[this.calendarColorCounter % colorStyles.length];
                ++this.calendarColorCounter;
                newCal.setStyle(style);
                newCal.addEventHandler(this.dragDropHandler);
                dynamicCalendars.put(groupKey, newCal);
                this.myCalendarSource.getCalendars().add(newCal);
            }
            Entry entry = new Entry(evModel.getTitle());
            entry.setId(String.valueOf(evModel.getEventId()));
            entry.setFullDay(evModel.isAllDay());
            entry.changeStartDate(evModel.getStartTime().toLocalDate());
            entry.changeEndDate(evModel.getEndTime().toLocalDate());
            if (!evModel.isAllDay()) {
                entry.changeStartTime(evModel.getStartTime().toLocalTime());
                entry.changeEndTime(evModel.getEndTime().toLocalTime());
            }
            entry.setLocation(evModel.getLocation() != null ? evModel.getLocation() : "");
            this.entryModelMap.put(String.valueOf(evModel.getEventId()), evModel);
            (dynamicCalendars.get(groupKey)).addEntry(entry);
        }
        for (CalendarTaskModel taskModel : this.taskDAO.getTasksByTutor(this.currentTutorId)) {
            if (taskModel.getDeadline() == null) continue;
            Object taskTitle = taskModel.getTitle();
            if ("DONE".toUpperCase().equals(taskModel.getStatus())) {
                taskTitle = "\u2705 [Xong] " + (String)taskTitle;
            }
            Entry taskEntry = new Entry((String)taskTitle);
            taskEntry.setId("TASK_" + taskModel.getTaskId());
            LocalDate dDate = taskModel.getDeadline().toLocalDate();
            LocalTime dTime = taskModel.getDeadline().toLocalTime();
            taskEntry.changeStartDate(dDate);
            taskEntry.changeEndDate(dDate);
            taskEntry.changeStartTime(dTime.minusMinutes(30L));
            taskEntry.changeEndTime(dTime);
            taskEntry.setLocation("Danh mục: " + taskModel.getTaskList());
            taskCalendar.addEntry(taskEntry);
        }
        for (CalendarPollModel pollModel : this.pollDAO.getPollsByTutor(this.currentTutorId)) {
            String pollEntryId = "POLL_" + pollModel.getPollId();
            LocalDate pollDisplayDate = LocalDate.now();
            try {
                String dateList = pollModel.getDateList();
                if (dateList != null && dateList.length() > 4) {
                    String firstDate = dateList.replaceAll("^\\[\"([^\"]+).*", "$1");
                    pollDisplayDate = LocalDate.parse(firstDate);
                }
            }
            catch (Exception dateList) {
                // empty catch block
            }
            Entry pollEntry = new Entry("\ud83d\udcca " + pollModel.getTitle());
            pollEntry.setId(pollEntryId);
            pollEntry.changeStartDate(pollDisplayDate);
            pollEntry.changeEndDate(pollDisplayDate);
            pollEntry.changeStartTime(pollModel.getStartTime());
            pollEntry.changeEndTime(pollModel.getEndTime());
            pollEntry.setLocation("Kh\u1EA3o s\u00E1t \u00B7 M\u00E3: " + pollModel.getUniqueCode());
            this.entryPollMap.put(pollEntryId, pollModel);
            pollCalendar.addEntry(pollEntry);
        }
        if (this.sideListBox != null) {
            this.buildSidePanelContent();
        }
    }

    private Calendar.Style mapHexToStyle(String hex) {
        String var2;
        if (hex == null) {
            return Calendar.Style.STYLE5;
        }
        switch (var2 = hex.toUpperCase()) {
            case "#4F46E5": {
                return Calendar.Style.STYLE1;
            }
            case "#EA4335": {
                return Calendar.Style.STYLE2;
            }
            case "#10B981": {
                return Calendar.Style.STYLE3;
            }
            case "#F59E0B": {
                return Calendar.Style.STYLE4;
            }
            case "#7C3AED": {
                return Calendar.Style.STYLE5;
            }
            case "#06B6D4": {
                return Calendar.Style.STYLE6;
            }
        }
        return Calendar.Style.STYLE5;
    }

    private void sendEmailNotificationAsync(String emailList, CalendarEventModel event) {
        SwingWorker<Void, Void> emailWorker = new SwingWorker<Void, Void>(){

            @Override
            protected Void doInBackground() throws Exception {
                return null;
            }
        };
        emailWorker.execute();
    }

    private void sendPollEmailNotificationAsync(String emailList, CalendarPollModel poll, String pollLink, String location, String meetLink, String attachPath) {
        SwingWorker<Void, Void> emailWorker = new SwingWorker<Void, Void>(){

            @Override
            protected Void doInBackground() throws Exception {
                return null;
            }
        };
        emailWorker.execute();
    }

    private void openCreateEventPopup(CalendarView calendarView, LocalDate clickedDate, LocalTime clickedTime) {
        VBox popupContent = new VBox(15.0);
        popupContent.setPadding(new Insets(24.0));
        popupContent.setStyle("-fx-background-color: #FFFFFF; -fx-pref-width: 520px;");
        PopOver popOver = new PopOver((Node)popupContent);
        popOver.setArrowLocation(PopOver.ArrowLocation.LEFT_TOP);
        popOver.setCornerRadius(16.0);
        popOver.setDetachable(false);
        popOver.setAutoHide(false);
        HBox headerBox = new HBox(10.0);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        TextField txtTitle = new TextField();
        txtTitle.setPromptText("Th\u00EAm ti\u00EAu \u0111\u1EC1 s\u1EF1 ki\u1EC7n");
        txtTitle.setStyle("-fx-font-size: 18px; -fx-border-color: #7C3AED; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-background-color: transparent; -fx-padding: 12px; -fx-prompt-text-fill: #9CA3AF;");
        HBox.setHgrow((Node)txtTitle, (Priority)Priority.ALWAYS);
        Button btnClose = new Button("\u2715");
        btnClose.setStyle("-fx-background-color: transparent; -fx-text-fill: #9CA3AF; -fx-font-size: 20px; -fx-font-weight: bold; -fx-cursor: hand;");
        btnClose.setOnAction(e -> popOver.hide());
        headerBox.getChildren().addAll(new Node[]{txtTitle, btnClose});
        HBox tabContainer = new HBox(5.0);
        tabContainer.setStyle("-fx-background-color: #F3F4F6; -fx-background-radius: 8px; -fx-padding: 4px;");
        Button btnEventTab = new Button("S\u1EF1 ki\u1EC7n", (Node)this.getWebIcon("https://img.icons8.com/fluency/48/calendar.png", 18.0));
        Button btnTaskTab = new Button("Nhi\u1EC7m v\u1EE5", (Node)this.getWebIcon("https://img.icons8.com/fluency/48/checked-checkbox.png", 18.0));
        Button btnPollTab = new Button("Kh\u1EA3o s\u00E1t lịch", (Node)this.getWebIcon("https://img.icons8.com/fluency/48/poll.png", 18.0));
        String activeTabStyle = "-fx-background-color: #FFFFFF; -fx-text-fill: #7C3AED; -fx-font-weight: bold; -fx-background-radius: 6px; -fx-cursor: hand; -fx-padding: 8px 16px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 2, 0, 0, 1);";
        String inactiveTabStyle = "-fx-background-color: transparent; -fx-text-fill: #6B7280; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 8px 16px;";
        btnEventTab.setStyle(activeTabStyle);
        btnTaskTab.setStyle(inactiveTabStyle);
        btnPollTab.setStyle(inactiveTabStyle);
        tabContainer.getChildren().addAll(new Node[]{btnEventTab, btnTaskTab, btnPollTab});
        String rowStyle = "-fx-background-color: #FFFFFF; -fx-border-color: #E5E7EB; -fx-border-radius: 8px; -fx-padding: 8px 12px; -fx-alignment: center-left;";
        String transparentInputStyle = "-fx-background-color: transparent; -fx-border-width: 0; -fx-font-size: 14px;";
        String cmbStyle = "-fx-background-color: #EDE9FE; -fx-background-radius: 4px; -fx-text-fill: #7C3AED; -fx-font-weight: bold;";
        List<String> timeOptions = this.generateTimeOptions();
        VBox bodyContent = new VBox();
        VBox eventBody = new VBox(12.0);
        HBox timeRow = new HBox(15.0);
        timeRow.setStyle(rowStyle);
        DatePicker datePicker = new DatePicker(clickedDate);
        datePicker.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
        Button btnAddTime = new Button("Th\u00EAm gi\u1EDD");
        btnAddTime.setStyle("-fx-background-color: #F3F4F6; -fx-text-fill: #4B5563; -fx-background-radius: 4px; -fx-cursor: hand;");
        ComboBox cmbStartTime = new ComboBox();
        ComboBox cmbEndTime = new ComboBox();
        cmbStartTime.getItems().addAll(timeOptions);
        cmbEndTime.getItems().addAll(timeOptions);
        cmbStartTime.setValue(this.formatTimeCustom(clickedTime));
        cmbEndTime.setValue(this.formatTimeCustom(clickedTime.plusHours(1L)));
        cmbStartTime.setStyle(cmbStyle);
        cmbEndTime.setStyle(cmbStyle);
        timeRow.getChildren().addAll(new Node[]{this.getWebIcon("https://img.icons8.com/fluency/48/clock.png", 22.0), datePicker, btnAddTime});
        boolean[] isTimeAdded = new boolean[]{false};
        btnAddTime.setOnAction(e -> {
            isTimeAdded[0] = true;
            timeRow.getChildren().remove(btnAddTime);
            timeRow.getChildren().addAll(new Node[]{cmbStartTime, new Label(" - "), cmbEndTime});
        });
        HBox guestRow = new HBox(15.0);
        guestRow.setStyle(rowStyle);
        TextField txtGuests = new TextField();
        ComboBox cmbReminder = new ComboBox();
        cmbReminder.getItems().addAll(new String[]{"Kh\u00F4ng nhắc", "Trước 5 ph\u00FAt", "Trước 10 ph\u00FAt", "Trước 30 ph\u00FAt", "Trước 1 gi\u1EDD"});
        cmbReminder.getSelectionModel().selectFirst();
        cmbReminder.setStyle("-fx-background-color: transparent; -fx-border-color: #E2E8F0; -fx-border-radius: 6px; -fx-padding: 2px;");
        cmbReminder.setPrefWidth(120.0);
        txtGuests.setPromptText("Th\u00EAm khách m\u1EDDi hoặc email");
        txtGuests.setStyle(transparentInputStyle);
        HBox.setHgrow((Node)txtGuests, (Priority)Priority.ALWAYS);
        guestRow.getChildren().addAll(new Node[]{this.getWebIcon("https://img.icons8.com/fluency/48/group.png", 22.0), txtGuests, new Label(" "), this.getWebIcon("https://img.icons8.com/color/48/alarm.png", 22.0), cmbReminder});
        Button btnAddMeet = new Button("Th\u00EAm cuộc g\u1ECDi video (TutorHub Meet)");
        btnAddMeet.setGraphic((Node)this.getWebIcon("https://img.icons8.com/fluency/48/video-call.png", 22.0));
        btnAddMeet.setMaxWidth(Double.MAX_VALUE);
        btnAddMeet.setStyle("-fx-background-color: #F3E8FF; -fx-text-fill: #7C3AED; -fx-font-weight: bold; -fx-background-radius: 8px; -fx-cursor: hand; -fx-padding: 12px 15px; -fx-alignment: center-left; -fx-font-size: 14px;");
        String[] meetingLink = new String[]{""};
        btnAddMeet.setOnAction(e -> {
            String r = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            meetingLink[0] = "https://meet.jit.si/TutorHub_Meeting_" + r;
            btnAddMeet.setText("Link ph\u00F2ng: TutorHub_Meeting_" + r);
        });
        HBox locRow = new HBox(15.0);
        locRow.setStyle(rowStyle);
        TextField txtLocation = new TextField();
        txtLocation.setPromptText("Th\u00EAm địa \u0111i\u1EC3m");
        txtLocation.setStyle(transparentInputStyle);
        HBox.setHgrow((Node)txtLocation, (Priority)Priority.ALWAYS);
        Button btnMap = new Button("", (Node)this.getWebIcon("https://img.icons8.com/fluency/48/google-maps-new.png", 22.0));
        btnMap.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 0;");
        btnMap.setOnAction(e -> {
            if (!txtLocation.getText().isEmpty()) {
                try {
                    Desktop.getDesktop().browse(new URI("https://www.google.com/maps/search/?api=1&query=" + URLEncoder.encode(txtLocation.getText(), "UTF-8")));
                }
                catch (Exception exception) {
                    // empty catch block
                }
            }
        });
        locRow.getChildren().addAll(new Node[]{this.getWebIcon("https://img.icons8.com/fluency/48/map-marker.png", 22.0), txtLocation, btnMap});
        HBox descRow = new HBox(15.0);
        descRow.setStyle(rowStyle);
        TextArea txtEventDesc = new TextArea();
        txtEventDesc.setPromptText("Th\u00EAm m\u00F4 t\u1EA3 hoặc n\u1ED9i dung th\u01B0 m\u1EDDi...");
        txtEventDesc.setStyle("-fx-background-color: transparent; -fx-control-inner-background: transparent; -fx-border-width: 0; -fx-font-size: 14px;");
        txtEventDesc.setPrefRowCount(3);
        txtEventDesc.setWrapText(true);
        HBox.setHgrow((Node)txtEventDesc, (Priority)Priority.ALWAYS);
        descRow.getChildren().addAll(new Node[]{this.getWebIcon("https://img.icons8.com/fluency/48/align-left.png", 22.0), txtEventDesc});
        HBox attachRow = new HBox(15.0);
        attachRow.setStyle(rowStyle);
        VBox attachContent = new VBox(5.0);
        Button btnAttach = new Button("\u0110\u00EDnh k\u00E8m t\u1EC7p t\u1EEB m\u00E1y t\u00EDnh");
        btnAttach.setStyle("-fx-background-color: transparent; -fx-text-fill: #6B7280; -fx-cursor: hand; -fx-font-size: 14px; -fx-padding: 0;");
        Label lblFileName = new Label("");
        lblFileName.setStyle("-fx-text-fill: #7C3AED; -fx-font-weight: bold; -fx-font-size: 12px;");
        String[] attachedFilePath = new String[]{""};
        btnAttach.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Ch\u1ECDn t\u1EC7p \u0111\u00EDnh k\u00E8m");
            File f = fc.showOpenDialog(btnAttach.getScene().getWindow());
            if (f != null) {
                attachedFilePath[0] = f.getAbsolutePath();
                lblFileName.setText("\ud83d\udcc4 " + f.getName());
            }
        });
        attachContent.getChildren().addAll(new Node[]{btnAttach, lblFileName});
        attachRow.getChildren().addAll(new Node[]{this.getWebIcon("https://img.icons8.com/color/48/google-drive--v2.png", 22.0), attachContent});
        eventBody.getChildren().addAll(new Node[]{timeRow, guestRow, btnAddMeet, locRow, descRow, attachRow});
        VBox taskBody = new VBox(12.0);
        HBox deadlineBox = new HBox(15.0);
        deadlineBox.setStyle(rowStyle);
        DatePicker dateDeadline = new DatePicker(clickedDate);
        dateDeadline.setStyle("-fx-background-color: transparent;");
        deadlineBox.getChildren().addAll(new Node[]{this.getWebIcon("https://img.icons8.com/fluency/48/bullseye.png", 22.0), dateDeadline});
        HBox taskListBox = new HBox(15.0);
        taskListBox.setStyle(rowStyle);
        ComboBox cmbTaskList = new ComboBox();
        cmbTaskList.getItems().addAll(new String[]{"Việc c\u1EA7n l\u00E0m của t\u00F4i", "H\u1ED3 s\u01A1 học vi\u00EAn", "C\u00F4ng việc trung t\u00E2m"});
        cmbTaskList.getSelectionModel().selectFirst();
        cmbTaskList.setStyle("-fx-background-color: transparent;");
        taskListBox.getChildren().addAll(new Node[]{this.getWebIcon("https://img.icons8.com/fluency/48/clipboard.png", 22.0), cmbTaskList});
        HBox descTaskBox = new HBox(15.0);
        descTaskBox.setStyle(rowStyle);
        TextField txtTaskDesc = new TextField();
        txtTaskDesc.setPromptText("Th\u00EAm m\u00F4 t\u1EA3 nhi\u1EC7m v\u1EE5");
        txtTaskDesc.setStyle(transparentInputStyle);
        HBox.setHgrow((Node)txtTaskDesc, (Priority)Priority.ALWAYS);
        descTaskBox.getChildren().addAll(new Node[]{this.getWebIcon("https://img.icons8.com/fluency/48/align-left.png", 22.0), txtTaskDesc});
        taskBody.getChildren().addAll(new Node[]{deadlineBox, taskListBox, descTaskBox});
        VBox pollBody = new VBox(12.0);
        HBox datePollRow = new HBox(10.0);
        datePollRow.setStyle(rowStyle);
        datePollRow.setAlignment(Pos.CENTER_LEFT);
        DatePicker dpPollStart = new DatePicker(clickedDate);
        dpPollStart.setStyle("-fx-background-color: transparent; -fx-max-width: 130px;");
        DatePicker dpPollEnd = new DatePicker(clickedDate.plusDays(6L));
        dpPollEnd.setStyle("-fx-background-color: transparent; -fx-max-width: 130px;");
        datePollRow.getChildren().addAll(new Node[]{this.getWebIcon("https://img.icons8.com/fluency/48/calendar.png", 22.0), new Label("T\u1EEB:"), dpPollStart, new Label("\u0111\u1EBFn:"), dpPollEnd});
        HBox timePollRow = new HBox(10.0);
        timePollRow.setStyle(rowStyle);
        timePollRow.setAlignment(Pos.CENTER_LEFT);
        ComboBox cmbPollStart = new ComboBox();
        ComboBox cmbPollEnd = new ComboBox();
        cmbPollStart.getItems().addAll(timeOptions);
        cmbPollEnd.getItems().addAll(timeOptions);
        cmbPollStart.setValue("08:00");
        cmbPollEnd.setValue("17:00");
        cmbPollStart.setStyle(cmbStyle);
        cmbPollEnd.setStyle(cmbStyle);
        timePollRow.getChildren().addAll(new Node[]{this.getWebIcon("https://img.icons8.com/fluency/48/clock.png", 22.0), new Label("Khung gi\u1EDD:"), cmbPollStart, new Label("\u0111\u1EBFn:"), cmbPollEnd});
        HBox guestPollRow = new HBox(15.0);
        guestPollRow.setStyle(rowStyle);
        TextField txtGuestsPoll = new TextField();
        ComboBox cmbReminderPoll = new ComboBox();
        cmbReminderPoll.getItems().addAll(new String[]{"Kh\u00F4ng nhắc", "Trước 5 ph\u00FAt", "Trước 10 ph\u00FAt", "Trước 30 ph\u00FAt", "Trước 1 gi\u1EDD"});
        cmbReminderPoll.getSelectionModel().selectFirst();
        cmbReminderPoll.setStyle("-fx-background-color: transparent; -fx-border-color: #E2E8F0; -fx-border-radius: 6px; -fx-padding: 2px;");
        cmbReminderPoll.setPrefWidth(120.0);
        txtGuestsPoll.setPromptText("Email khách m\u1EDDi kh\u1EA3o s\u00E1t");
        txtGuestsPoll.setStyle(transparentInputStyle);
        HBox.setHgrow((Node)txtGuestsPoll, (Priority)Priority.ALWAYS);
        guestPollRow.getChildren().addAll(new Node[]{this.getWebIcon("https://img.icons8.com/fluency/48/group.png", 22.0), txtGuestsPoll, new Label(" "), this.getWebIcon("https://img.icons8.com/color/48/alarm.png", 22.0), cmbReminderPoll});
        Button btnAddMeetPoll = new Button("Th\u00EAm cuộc g\u1ECDi video d\u1EF1 ki\u1EBFn");
        btnAddMeetPoll.setGraphic((Node)this.getWebIcon("https://img.icons8.com/fluency/48/video-call.png", 22.0));
        btnAddMeetPoll.setMaxWidth(Double.MAX_VALUE);
        btnAddMeetPoll.setStyle("-fx-background-color: #F3E8FF; -fx-text-fill: #7C3AED; -fx-font-weight: bold; -fx-background-radius: 8px; -fx-cursor: hand; -fx-padding: 12px 15px; -fx-alignment: center-left; -fx-font-size: 14px;");
        String[] meetingLinkPoll = new String[]{""};
        btnAddMeetPoll.setOnAction(e -> {
            String r = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            meetingLinkPoll[0] = "https://meet.jit.si/TutorHub_Meeting_" + r;
            btnAddMeetPoll.setText("Link ph\u00F2ng: TutorHub_Meeting_" + r);
        });
        HBox locPollRow = new HBox(15.0);
        locPollRow.setStyle(rowStyle);
        TextField txtLocationPoll = new TextField();
        txtLocationPoll.setPromptText("Địa \u0111i\u1EC3m d\u1EF1 ki\u1EBFn");
        txtLocationPoll.setStyle(transparentInputStyle);
        HBox.setHgrow((Node)txtLocationPoll, (Priority)Priority.ALWAYS);
        Button btnMapPoll = new Button("", (Node)this.getWebIcon("https://img.icons8.com/fluency/48/google-maps-new.png", 22.0));
        btnMapPoll.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 0;");
        btnMapPoll.setOnAction(e -> {
            if (!txtLocationPoll.getText().isEmpty()) {
                try {
                    Desktop.getDesktop().browse(new URI("https://www.google.com/maps/search/?api=1&query=" + URLEncoder.encode(txtLocationPoll.getText(), "UTF-8")));
                }
                catch (Exception exception) {
                    // empty catch block
                }
            }
        });
        locPollRow.getChildren().addAll(new Node[]{this.getWebIcon("https://img.icons8.com/fluency/48/map-marker.png", 22.0), txtLocationPoll, btnMapPoll});
        HBox descPollRow = new HBox(15.0);
        descPollRow.setStyle(rowStyle);
        TextArea txtPollDesc = new TextArea();
        txtPollDesc.setPromptText("Nh\u1EADp l\u1EDDi nh\u1EAFn cho học vi\u00EAn v\u00E0o email...");
        txtPollDesc.setStyle("-fx-background-color: transparent; -fx-control-inner-background: transparent; -fx-border-width: 0; -fx-font-size: 14px;");
        txtPollDesc.setPrefRowCount(3);
        txtPollDesc.setWrapText(true);
        HBox.setHgrow((Node)txtPollDesc, (Priority)Priority.ALWAYS);
        descPollRow.getChildren().addAll(new Node[]{this.getWebIcon("https://img.icons8.com/fluency/48/align-left.png", 22.0), txtPollDesc});
        HBox attachPollRow = new HBox(15.0);
        attachPollRow.setStyle(rowStyle);
        VBox attachContentPoll = new VBox(5.0);
        Button btnAttachPoll = new Button("\u0110\u00EDnh k\u00E8m t\u00E0i li\u1EC7u v\u00E0o email");
        btnAttachPoll.setStyle("-fx-background-color: transparent; -fx-text-fill: #6B7280; -fx-cursor: hand; -fx-font-size: 14px; -fx-padding: 0;");
        Label lblFileNamePoll = new Label("");
        lblFileNamePoll.setStyle("-fx-text-fill: #7C3AED; -fx-font-weight: bold; -fx-font-size: 12px;");
        String[] attachedFilePathPoll = new String[]{""};
        btnAttachPoll.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Ch\u1ECDn t\u1EC7p");
            File f = fc.showOpenDialog(btnAttachPoll.getScene().getWindow());
            if (f != null) {
                attachedFilePathPoll[0] = f.getAbsolutePath();
                lblFileNamePoll.setText("\ud83d\udcc4 " + f.getName());
            }
        });
        attachContentPoll.getChildren().addAll(new Node[]{btnAttachPoll, lblFileNamePoll});
        attachPollRow.getChildren().addAll(new Node[]{this.getWebIcon("https://img.icons8.com/color/48/google-drive--v2.png", 22.0), attachContentPoll});
        pollBody.getChildren().addAll(new Node[]{datePollRow, timePollRow, guestPollRow, btnAddMeetPoll, locPollRow, descPollRow, attachPollRow});
        bodyContent.getChildren().setAll(new Node[]{eventBody});
        int[] currentTab = new int[]{0};
        btnEventTab.setOnAction(e -> {
            currentTab[0] = 0;
            btnEventTab.setStyle(activeTabStyle);
            btnTaskTab.setStyle(inactiveTabStyle);
            btnPollTab.setStyle(inactiveTabStyle);
            bodyContent.getChildren().setAll(new Node[]{eventBody});
            txtTitle.setPromptText("Th\u00EAm ti\u00EAu \u0111\u1EC1 s\u1EF1 ki\u1EC7n");
        });
        btnTaskTab.setOnAction(e -> {
            currentTab[0] = 1;
            btnTaskTab.setStyle(activeTabStyle);
            btnEventTab.setStyle(inactiveTabStyle);
            btnPollTab.setStyle(inactiveTabStyle);
            bodyContent.getChildren().setAll(new Node[]{taskBody});
            txtTitle.setPromptText("Th\u00EAm ti\u00EAu \u0111\u1EC1 nhi\u1EC7m v\u1EE5");
        });
        btnPollTab.setOnAction(e -> {
            currentTab[0] = 2;
            btnPollTab.setStyle(activeTabStyle);
            btnEventTab.setStyle(inactiveTabStyle);
            btnTaskTab.setStyle(inactiveTabStyle);
            bodyContent.getChildren().setAll(new Node[]{pollBody});
            txtTitle.setPromptText("Ti\u00EAu \u0111\u1EC1 kh\u1EA3o s\u00E1t (VD: Kh\u1EA3o s\u00E1t lịch học b\u00F9)");
        });
        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setSpacing(15.0);
        footer.setPadding(new Insets(15.0, 0.0, 0.0, 0.0));
        Button btnMoreOpts = new Button("T\u00F9y ch\u1ECDn khác");
        btnMoreOpts.setStyle("-fx-background-color: transparent; -fx-text-fill: #7C3AED; -fx-cursor: hand; -fx-font-weight: bold; -fx-font-size: 14px;");
        Button btnSave = new Button("L\u01B0u");
        btnSave.setStyle("-fx-background-color: #7C3AED; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10px 32px; -fx-background-radius: 8px; -fx-cursor: hand; -fx-font-size: 14px; -fx-effect: dropshadow(three-pass-box, rgba(124,58,237,0.3), 10, 0, 0, 4);");
        HBox spacer = new HBox();
        HBox.setHgrow((Node)spacer, (Priority)Priority.ALWAYS);
        footer.getChildren().addAll(new Node[]{btnMoreOpts, spacer, btnSave});
        popupContent.getChildren().addAll(new Node[]{headerBox, tabContainer, bodyContent, footer});
        popOver.setContentNode((Node)popupContent);
        btnSave.setOnAction(e -> {
            String title = txtTitle.getText().trim();
            if (currentTab[0] == 0) {
                CalendarEventModel newEvent = new CalendarEventModel();
                newEvent.setTitle(title.isEmpty() ? "(Kh\u00F4ng c\u00F3 ti\u00EAu \u0111\u1EC1)" : title);
                LocalDate finalDate = (LocalDate)datePicker.getValue();
                if (isTimeAdded[0]) {
                    newEvent.setAllDay(false);
                    newEvent.setStartTime(finalDate.atTime(this.parseTimeString((String)cmbStartTime.getValue())));
                    newEvent.setEndTime(finalDate.atTime(this.parseTimeString((String)cmbEndTime.getValue())));
                } else {
                    newEvent.setAllDay(true);
                    newEvent.setStartTime(finalDate.atStartOfDay());
                    newEvent.setEndTime(finalDate.atTime(23, 59));
                }
                newEvent.setLocation(txtLocation.getText().trim());
                newEvent.setDescription(txtEventDesc.getText().trim());
                newEvent.setCreatedBy(this.currentTutorId);
                newEvent.setStatus("BUSY");
                newEvent.setColor("#7C3AED");
                newEvent.setOnlineMeetingLink(meetingLink[0]);
                if (!attachedFilePath[0].isEmpty()) {
                    newEvent.setAttachments("[\"" + attachedFilePath[0].replace("\\", "\\\\") + "\"]");
                }
                String rawGuests = txtGuests.getText().trim();
                String jsonGuests = "[]";
                if (!rawGuests.isEmpty()) {
                    String[] emails = rawGuests.split(",");
                    StringBuilder sb = new StringBuilder("[");
                    for (int i = 0; i < emails.length; ++i) {
                        sb.append("\"").append(emails[i].trim()).append("\"");
                        if (i >= emails.length - 1) continue;
                        sb.append(", ");
                    }
                    sb.append("]");
                    jsonGuests = sb.toString();
                }
                newEvent.setGuests(jsonGuests);
                if (this.eventDAO.insertEvent(newEvent) && !rawGuests.isEmpty()) {
                    this.sendEmailNotificationAsync(rawGuests, newEvent);
                }
            } else if (currentTab[0] == 1) {
                CalendarTaskModel var35x = new CalendarTaskModel();
                var35x.setTitle(title.isEmpty() ? "Nhi\u1EC7m v\u1EE5 kh\u00F4ng c\u00F3 ti\u00EAu \u0111\u1EC1" : title);
                LocalDate var37x = (LocalDate)dateDeadline.getValue();
                var35x.setDeadline(var37x != null ? var37x.atTime(LocalTime.of(23, 59)) : null);
                var35x.setTaskList((String)cmbTaskList.getValue());
                var35x.setDescription(txtTaskDesc.getText().trim());
                var35x.setCreatedBy(this.currentTutorId);
                var35x.setStatus("TODO");
                this.taskDAO.insertTask(var35x);
            } else if (currentTab[0] == 2) {
                CalendarPollModel var36x = new CalendarPollModel();
                var36x.setTutorId(this.currentTutorId);
                var36x.setTitle(title.isEmpty() ? "Kh\u1EA3o s\u00E1t th\u1EDDi gian" : title);
                var36x.setDescription(txtPollDesc.getText().trim());
                LocalDate var38x = (LocalDate)dpPollStart.getValue();
                LocalDate var39x = (LocalDate)dpPollEnd.getValue();
                StringBuilder var40x = new StringBuilder("[");
                LocalDate var41x = var38x;
                while (!var41x.isAfter(var39x)) {
                    var40x.append("\"").append(var41x.toString()).append("\"");
                    if (!var41x.isEqual(var39x)) {
                        var40x.append(", ");
                    }
                    var41x = var41x.plusDays(1L);
                }
                var40x.append("]");
                var36x.setDateList(var40x.toString());
                var36x.setStartTime(this.parseTimeString((String)cmbPollStart.getValue()));
                var36x.setEndTime(this.parseTimeString((String)cmbPollEnd.getValue()));
                String var42x = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
                var36x.setUniqueCode(var42x);
                if (this.pollDAO.insertPoll(var36x)) {
                    String var43x = "https://hocbatrolai.github.io/tutorhub-poll/?code=" + var42x;
                    StringSelection var44x = new StringSelection(var43x);
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(var44x, null);
                    String rawGuestsPoll = txtGuestsPoll.getText().trim();
                    if (!rawGuestsPoll.isEmpty()) {
                        this.sendPollEmailNotificationAsync(rawGuestsPoll, var36x, var43x, txtLocationPoll.getText().trim(), meetingLinkPoll[0], attachedFilePathPoll[0]);
                    }
                    Platform.runLater(() -> {
                        Stage webStage = new Stage();
                        webStage.setTitle("TutorHub - Xem trước kh\u1EA3o s\u00E1t [" + var42x + "]");
                        WebView webView = new WebView();
                        WebEngine webEngine = webView.getEngine();
                        webEngine.setOnAlert(ev -> SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, ev.getData(), "Th\u00F4ng b\u00E1o", 1)));
                        webEngine.load(var43x);
                        VBox webLayout = new VBox(12.0);
                        webLayout.setPadding(new Insets(15.0));
                        webLayout.setStyle("-fx-background-color: #F8FAFC;");
                        Label lblInfo = new Label("\ud83d\udd17 Link kh\u1EA3o s\u00E1t (\u0110\u00E3 Copy): " + var43x);
                        lblInfo.setStyle("-fx-font-weight: bold; -fx-text-fill: #7C3AED; -fx-font-size: 13px;");
                        VBox.setVgrow((Node)webView, (Priority)Priority.ALWAYS);
                        webLayout.getChildren().addAll(new Node[]{lblInfo, webView});
                        Scene webScene = new Scene((Parent)webLayout, 950.0, 680.0);
                        webStage.setScene(webScene);
                        webStage.initModality(Modality.APPLICATION_MODAL);
                        webStage.show();
                    });
                }
            }
            popOver.hide();
            this.reloadCalendarData();
        });
        popOver.show(calendarView.getScene().getWindow());
    }

    private void buildSidePanelContent() {
        this.sideListBox.getChildren().clear();
        HBox panelHeader = new HBox(10.0);
        panelHeader.setAlignment(Pos.CENTER_LEFT);
        panelHeader.setPadding(new Insets(4.0, 8.0, 8.0, 8.0));
        Label lblPanelTitle = new Label("\ud83d\udcc6 Qu\u1EA3n l\u00FD lịch");
        lblPanelTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #1E293B;");
        Region hdrSpacer = new Region();
        HBox.setHgrow((Node)hdrSpacer, (Priority)Priority.ALWAYS);
        Button btnClosePanel = new Button("\u2715");
        btnClosePanel.setStyle("-fx-background-color: transparent; -fx-text-fill: #94A3B8; -fx-font-size: 14px; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 2px 6px;");
        btnClosePanel.setOnMouseEntered(e -> btnClosePanel.setStyle("-fx-background-color: #F1F5F9; -fx-text-fill: #1E293B; -fx-font-size: 14px; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 2px 6px; -fx-background-radius: 4px;"));
        btnClosePanel.setOnMouseExited(e -> btnClosePanel.setStyle("-fx-background-color: transparent; -fx-text-fill: #94A3B8; -fx-font-size: 14px; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 2px 6px;"));
        btnClosePanel.setOnAction(e -> {
            if (this.rightPanelWrapper != null) {
                this.rightPanelWrapper.setVisible(false);
                this.rightPanelWrapper.setManaged(false);
            }
        });
        panelHeader.getChildren().addAll(new Node[]{lblPanelTitle, hdrSpacer, btnClosePanel});
        this.sideListBox.getChildren().add(panelHeader);
        HBox tabBox = new HBox(0.0);
        tabBox.setStyle("-fx-border-color: transparent transparent #E2E8F0 transparent; -fx-border-width: 0 0 1px 0; -fx-padding: 0 0 0 8px;");
        ToggleGroup tabGroup = new ToggleGroup();
        ToggleButton btnTabEvent = new ToggleButton("S\u1EF1 ki\u1EC7n");
        ToggleButton btnTabPoll = new ToggleButton("Kh\u1EA3o s\u00E1t");
        btnTabEvent.setToggleGroup(tabGroup);
        btnTabPoll.setToggleGroup(tabGroup);
        String tabUnselected = "-fx-background-color: transparent; -fx-text-fill: #64748B; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 8px 16px; -fx-cursor: hand; -fx-border-color: transparent; -fx-border-width: 0 0 2px 0;";
        String tabSelected = "-fx-background-color: transparent; -fx-text-fill: #1A73E8; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 8px 16px; -fx-cursor: hand; -fx-border-color: transparent transparent #1A73E8 transparent; -fx-border-width: 0 0 2px 0;";
        btnTabEvent.setStyle(tabSelected);
        btnTabPoll.setStyle(tabUnselected);
        tabGroup.selectToggle((Toggle)btnTabEvent);
        tabBox.getChildren().addAll(new Node[]{btnTabEvent, btnTabPoll});
        this.sideListBox.getChildren().add(tabBox);
        VBox contentArea = new VBox(10.0);
        contentArea.setPadding(new Insets(12.0, 4.0, 12.0, 4.0));
        ScrollPane scrollArea = new ScrollPane((Node)contentArea);
        scrollArea.setFitToWidth(true);
        scrollArea.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
        VBox.setVgrow((Node)scrollArea, (Priority)Priority.ALWAYS);
        this.sideListBox.getChildren().add(scrollArea);
        List<CalendarEventModel> events = this.eventDAO.getEventsByTutor(this.currentTutorId);
        List<CalendarPollModel> polls = this.pollDAO.getPollsByTutor(this.currentTutorId);
        Runnable loadEvents = () -> {
            contentArea.getChildren().clear();
            if (events.isEmpty()) {
                VBox emptyBox = new VBox(6.0);
                emptyBox.setAlignment(Pos.CENTER);
                emptyBox.setPadding(new Insets(40.0, 20.0, 40.0, 20.0));
                Label emptyIcon = new Label("\ud83d\udcc5");
                emptyIcon.setStyle("-fx-font-size: 32px;");
                Label emptyText = new Label("Chưa c\u00F3 s\u1EF1 ki\u1EC7n n\u00E0o");
                emptyText.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #475569;");
                Label emptySub = new Label("T\u1EA1o s\u1EF1 ki\u1EC7n m\u1EDBi \u0111\u1EC3 qu\u1EA3n l\u00FD lịch d\u1EC5 d\u00E0ng h\u01A1n.");
                emptySub.setStyle("-fx-font-size: 12px; -fx-text-fill: #94A3B8;");
                emptySub.setWrapText(true);
                emptySub.setTextAlignment(TextAlignment.CENTER);
                emptyBox.getChildren().addAll(new Node[]{emptyIcon, emptyText, emptySub});
                contentArea.getChildren().add(emptyBox);
            } else {
                for (CalendarEventModel var8x : events) {
                    contentArea.getChildren().add(this.buildEventCard(var8x));
                }
            }
        };
        Runnable loadPolls = () -> {
            contentArea.getChildren().clear();
            if (polls.isEmpty()) {
                VBox emptyBox = new VBox(6.0);
                emptyBox.setAlignment(Pos.CENTER);
                emptyBox.setPadding(new Insets(40.0, 20.0, 40.0, 20.0));
                Label emptyIcon = new Label("\ud83d\udcca");
                emptyIcon.setStyle("-fx-font-size: 32px;");
                Label emptyText = new Label("Chưa c\u00F3 kh\u1EA3o s\u00E1t n\u00E0o");
                emptyText.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #475569;");
                Label emptySub = new Label("T\u1EA1o kh\u1EA3o s\u00E1t \u0111\u1EC3 thu th\u1EADp th\u1EDDi gian học ph\u00F9 h\u1EE3p.");
                emptySub.setStyle("-fx-font-size: 12px; -fx-text-fill: #94A3B8;");
                emptySub.setWrapText(true);
                emptySub.setTextAlignment(TextAlignment.CENTER);
                emptyBox.getChildren().addAll(new Node[]{emptyIcon, emptyText, emptySub});
                contentArea.getChildren().add(emptyBox);
            } else {
                for (CalendarPollModel var8x : polls) {
                    contentArea.getChildren().add(this.buildPollCard(var8x));
                }
            }
        };
        tabGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                tabGroup.selectToggle(oldVal);
            } else {
                btnTabEvent.setStyle(newVal == btnTabEvent ? tabSelected : tabUnselected);
                btnTabPoll.setStyle(newVal == btnTabPoll ? tabSelected : tabUnselected);
                if (newVal == btnTabEvent) {
                    loadEvents.run();
                } else {
                    loadPolls.run();
                }
            }
        });
        if (tabGroup.getSelectedToggle() == btnTabEvent) {
            loadEvents.run();
        } else {
            loadPolls.run();
        }
    }

    private String parseGuestEmails(String json) {
        if (json == null || json.isEmpty() || json.equals("[]")) {
            return "";
        }
        if (json.startsWith("[\"")) {
            return json.substring(2, json.length() - 2).replace("\",\"", ", ");
        }
        return json;
    }

    private Node buildPopoverContentNode(CalendarEventModel evModel, CalendarPollModel pollModel, String title, LocalDate startDate, LocalTime startTime, LocalTime endTime, boolean isFullDay, Runnable hideCallback) {
        int reminder;
        boolean isPoll = pollModel != null;
        VBox root = new VBox(0.0);
        root.setStyle("-fx-background-color: #FFFFFF; -fx-min-width: 400px; -fx-max-width: 450px; -fx-background-radius: 16px; -fx-padding: 20px;");
        HBox headerBox = new HBox();
        headerBox.setAlignment(Pos.CENTER_LEFT);
        Label badge = new Label(isPoll ? "\uD83D\uDCC5 KH\u1EA2O S\u00C1T LỊCH" : "\uD83D\uDCC5 S\u1EF0 KI\u1EC6N");
        badge.setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #EF4444; -fx-font-weight: bold; -fx-padding: 4px 10px; -fx-background-radius: 12px; -fx-font-size: 11px;");
        Region spacer = new Region();
        HBox.setHgrow((Node)spacer, (Priority)Priority.ALWAYS);
        HBox actionBox = new HBox(12.0);
        actionBox.setAlignment(Pos.CENTER_RIGHT);
        BiFunction<String, String, VBox> makeActionIcon = (iconText, lblText) -> {
            VBox box = new VBox(2.0);
            box.setAlignment(Pos.CENTER);
            box.setStyle("-fx-cursor: hand;");
            Label icon = new Label(iconText);
            icon.setStyle("-fx-font-size: 16px; -fx-text-fill: #475569;");
            Label textLbl = new Label(lblText);
            textLbl.setStyle("-fx-font-size: 9px; -fx-text-fill: #475569;");
            box.getChildren().addAll(new Node[]{icon, textLbl});
            box.setOnMouseEntered(e -> {
                icon.setStyle("-fx-font-size: 16px; -fx-text-fill: #EF4444;");
                textLbl.setStyle("-fx-font-size: 9px; -fx-text-fill: #EF4444;");
            });
            box.setOnMouseExited(e -> {
                icon.setStyle("-fx-font-size: 16px; -fx-text-fill: #475569;");
                textLbl.setStyle("-fx-font-size: 9px; -fx-text-fill: #475569;");
            });
            return box;
        };
        VBox btnEdit = makeActionIcon.apply("\u270E", "Ch\u1EC9nh sửa");
        VBox btnDelete = makeActionIcon.apply("\uD83D\uDDD1", "X\u00f3a");
        VBox btnEmail = makeActionIcon.apply("\u2709", "G\u1EEDi email");
        Label btnMore = new Label("\u22EE");
        btnMore.setStyle("-fx-font-size: 20px; -fx-text-fill: #475569; -fx-cursor: hand; -fx-padding: 0 4px;");
        Label btnClose = new Label("\u2715");
        btnClose.setStyle("-fx-font-size: 16px; -fx-text-fill: #475569; -fx-cursor: hand; -fx-padding: 0 4px;");
        btnClose.setOnMouseClicked(e -> hideCallback.run());
        btnDelete.setOnMouseClicked(e -> {
            hideCallback.run();
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Xác nh\u1EADn xóa");
            confirm.setHeaderText(null);
            confirm.setContentText("\uD83D\uDDD1 X\u00f3a \"" + title + "\"?");
            confirm.showAndWait().ifPresent(res -> {
                if (res == ButtonType.OK) {
                    if (isPoll) {
                        this.pollDAO.deletePoll(pollModel.getPollId());
                    } else {
                        this.eventDAO.deleteEvent(evModel.getEventId());
                    }
                    this.reloadCalendarData();
                }
            });
        });
        btnEmail.setOnMouseClicked(e -> {
            if (isPoll) {
                String guests = this.parseGuestEmails(pollModel.getGuests());
                if (!guests.isEmpty()) {
                    this.sendPollEmailNotificationAsync(guests, pollModel, "https://hocbatrolai.github.io/tutorhub-poll/?code=" + pollModel.getUniqueCode(), pollModel.getLocation(), "", "");
                }
            } else {
                String guests = this.parseGuestEmails(evModel.getGuests());
                if (!guests.isEmpty()) {
                    this.sendEmailNotificationAsync(guests, evModel);
                }
            }
        });
        actionBox.getChildren().addAll(new Node[]{btnEdit, btnDelete, btnEmail, btnMore, btnClose});
        headerBox.getChildren().addAll(new Node[]{badge, spacer, actionBox});
        Label lblTitle = new Label(title);
        lblTitle.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #1E293B; -fx-wrap-text: true; -fx-padding: 16px 0 8px 0;");
        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy", new Locale("vi", "VN"));
        Label lblDate = new Label("\uD83D\uDCC5  " + startDate.format(dateFmt));
        lblDate.setStyle("-fx-font-size: 14px; -fx-text-fill: #475569; -fx-padding: 0 0 16px 0;");
        VBox rowsBox = new VBox(0.0);
        rowsBox.setStyle("-fx-border-color: #F1F5F9 transparent transparent transparent; -fx-border-width: 1px; -fx-padding: 12px 0 0 0;");
        Function<String[], Node> createRow = args -> {
            HBox botNode;
            String iconUnicode = args[0];
            String topText = args[1];
            String botText = args[2];
            String optBtnText = ((String[])args).length > 3 ? args[3] : null;
            HBox row = new HBox(16.0);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle("-fx-padding: 12px 0; -fx-border-color: transparent transparent #F8FAFC transparent; -fx-border-width: 1px;");
            StackPane iconBg = new StackPane();
            iconBg.setPrefSize(36.0, 36.0);
            iconBg.setMaxSize(36.0, 36.0);
            Circle circle = new Circle(18.0);
            circle.setFill((Paint)javafx.scene.paint.Color.web((String)"#FEF2F2"));
            Label iconLbl = new Label(iconUnicode);
            iconLbl.setStyle("-fx-text-fill: #EF4444; -fx-font-size: 16px;");
            iconBg.getChildren().addAll(new Node[]{circle, iconLbl});
            VBox textBox = new VBox(4.0);
            Label topLbl = new Label(topText);
            topLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B; -fx-font-weight: bold;");
            if (botText.startsWith("LINK:")) {
                HBox linkBox = new HBox(8.0);
                linkBox.setAlignment(Pos.CENTER_LEFT);
                TextField txtLink = new TextField(botText.substring(5));
                txtLink.setEditable(false);
                txtLink.setStyle("-fx-background-color: transparent; -fx-border-color: #CBD5E1; -fx-border-radius: 4px; -fx-padding: 4px 8px; -fx-pref-width: 200px; -fx-text-fill: #1E293B;");
                Button btnCopy = new Button("\uD83D\uDCCB Copy");
                btnCopy.setStyle("-fx-background-color: transparent; -fx-border-color: #FECACA; -fx-text-fill: #EF4444; -fx-font-weight: bold; -fx-border-radius: 4px; -fx-cursor: hand; -fx-padding: 4px 8px;");
                btnCopy.setOnAction(e -> {
                    StringSelection sel = new StringSelection(txtLink.getText());
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(sel, null);
                    btnCopy.setText("\u2705 Copy");
                });
                linkBox.getChildren().addAll(new Node[]{txtLink, btnCopy});
                botNode = linkBox;
            } else {
                HBox botBox = new HBox(8.0);
                botBox.setAlignment(Pos.CENTER_LEFT);
                Label botLbl = new Label(botText);
                botLbl.setStyle("-fx-font-size: 14px; -fx-text-fill: #1E293B;");
                botBox.getChildren().add(botLbl);
                if (optBtnText != null) {
                    Button optBtn = new Button(optBtnText);
                    optBtn.setStyle("-fx-background-color: transparent; -fx-border-color: #FECACA; -fx-text-fill: #EF4444; -fx-font-weight: bold; -fx-border-radius: 4px; -fx-cursor: hand; -fx-padding: 2px 8px; -fx-font-size: 11px;");
                    botBox.getChildren().add(optBtn);
                }
                botNode = botBox;
            }
            textBox.getChildren().addAll(new Node[]{topLbl, botNode});
            row.getChildren().addAll(new Node[]{iconBg, textBox});
            return row;
        };
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");
        String timeStr = isFullDay ? "C\u1EA3 ng\u00E0y" : startTime.format(timeFmt) + " \u2013 " + endTime.format(timeFmt);
        rowsBox.getChildren().add(createRow.apply(new String[]{"\uD83D\uDD52", "Th\u1EDDi gian", timeStr}));
        String location = isPoll ? pollModel.getLocation() : evModel.getLocation();
        String description = isPoll ? pollModel.getDescription() : evModel.getDescription();
        String meetLink = isPoll ? null : evModel.getOnlineMeetingLink();
        int n = reminder = isPoll ? pollModel.getReminderTime() : evModel.getReminderTime();
        if (meetLink != null && !meetLink.isEmpty()) {
            rowsBox.getChildren().add(createRow.apply(new String[]{"\uD83C\uDFA5", "Video Call", "Tham gia cuộc g\u1ECDi", "M\u1EDF Link"}));
        } else {
            rowsBox.getChildren().add(createRow.apply(new String[]{"\uD83C\uDFA5", "Video Call", "Chưa c\u00F3 link cuộc g\u1ECDi"}));
        }
        if (location != null && !location.isEmpty()) {
            rowsBox.getChildren().add(createRow.apply(new String[]{"\uD83D\uDCCD", "Địa \u0111i\u1EC3m", location, "\uD83D\uDDFA Xem Maps"}));
        }
        if (isPoll) {
            rowsBox.getChildren().add(createRow.apply(new String[]{"\uD83D\uDCC5", "Deadline", endTime.format(timeFmt) + " ng\u00E0y " + startDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}));
        }
        if (reminder > 0) {
            String rmText = reminder >= 60 ? reminder / 60 + " gi\u1EDD trước lúc " + startTime.format(timeFmt) : reminder + " ph\u00FAt trước lúc " + startTime.format(timeFmt);
            rowsBox.getChildren().add(createRow.apply(new String[]{"\uD83D\uDD14", "Th\u00F4ng b\u00E1o trước s\u1EF1 ki\u1EC7n", rmText}));
        }
        if (description != null && !description.isEmpty()) {
            rowsBox.getChildren().add(createRow.apply(new String[]{"\uD83D\uDCCB", "M\u00F4 t\u1EA3", description}));
        }
        if (isPoll) {
            rowsBox.getChildren().add(createRow.apply(new String[]{"\uD83D\uDD17", "\u0110\u01B0\u1EDDng link kh\u1EA3o s\u00E1t", "LINK:https://hocbatrolai.github.io/tutorhub-poll/?code=" + pollModel.getUniqueCode()}));
        }
        root.getChildren().addAll(new Node[]{headerBox, lblTitle, lblDate, rowsBox});
        return root;
    }

    private VBox buildEventCard(CalendarEventModel ev) {
        VBox card = new VBox(6.0);
        card.setPadding(new Insets(12.0, 12.0, 12.0, 12.0));
        card.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 10px; -fx-border-color: #EEF0F3; -fx-border-radius: 10px; -fx-border-width: 1px; -fx-cursor: hand;");
        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: #F9F5FF; -fx-background-radius: 10px; -fx-border-color: #C4B5FD; -fx-border-radius: 10px; -fx-border-width: 1px; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, rgba(124,58,237,0.08), 6, 0, 0, 2);"));
        card.setOnMouseExited(e -> card.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 10px; -fx-border-color: #EEF0F3; -fx-border-radius: 10px; -fx-border-width: 1px; -fx-cursor: hand;"));
        HBox titleRow = new HBox(6.0);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Rectangle colorDot = new Rectangle(4.0, 36.0);
        colorDot.setArcWidth(4.0);
        colorDot.setArcHeight(4.0);
        colorDot.setFill((Paint)javafx.scene.paint.Color.web((String)"#7C3AED"));
        Label lblTitle = new Label(ev.getTitle());
        lblTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1E293B; -fx-wrap-text: true;");
        lblTitle.setMaxWidth(160.0);
        Region spacer = new Region();
        HBox.setHgrow((Node)spacer, (Priority)Priority.ALWAYS);
        Button btnEmail = new Button("\u2709");
        btnEmail.setStyle("-fx-background-color: transparent; -fx-text-fill: #94A3B8; -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 2px 4px;");
        btnEmail.setOnMouseEntered(e -> btnEmail.setStyle("-fx-background-color: #F1F5F9; -fx-text-fill: #3B82F6; -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 2px 4px; -fx-background-radius: 6px;"));
        btnEmail.setOnMouseExited(e -> btnEmail.setStyle("-fx-background-color: transparent; -fx-text-fill: #94A3B8; -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 2px 4px;"));
        btnEmail.setOnAction(e -> {
            String guests = this.parseGuestEmails(ev.getGuests());
            if (!guests.isEmpty()) {
                this.sendEmailNotificationAsync(guests, ev);
                btnEmail.setText("\u2713");
            }
        });
        Button btnEdit = new Button("\u270E");
        btnEdit.setStyle("-fx-background-color: transparent; -fx-text-fill: #94A3B8; -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 2px 4px;");
        btnEdit.setOnMouseEntered(e -> btnEdit.setStyle("-fx-background-color: #F1F5F9; -fx-text-fill: #10B981; -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 2px 4px; -fx-background-radius: 6px;"));
        btnEdit.setOnMouseExited(e -> btnEdit.setStyle("-fx-background-color: transparent; -fx-text-fill: #94A3B8; -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 2px 4px;"));
        Button btnDelete = new Button("\uD83D\uDDD1");
        btnDelete.setStyle("-fx-background-color: transparent; -fx-text-fill: #94A3B8; -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 2px 4px;");
        btnDelete.setOnMouseEntered(e -> btnDelete.setStyle("-fx-background-color: #FEF2F2; -fx-text-fill: #EF4444; -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 2px 4px; -fx-background-radius: 6px;"));
        btnDelete.setOnMouseExited(e -> btnDelete.setStyle("-fx-background-color: transparent; -fx-text-fill: #94A3B8; -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 2px 4px;"));
        btnDelete.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Xác nh\u1EADn xóa");
            confirm.setHeaderText(null);
            confirm.setContentText("\uD83D\uDDD1 X\u00f3a s\u1EF1 ki\u1EC7n \"" + ev.getTitle() + "\"?");
            confirm.showAndWait().ifPresent(res -> {
                if (res == ButtonType.OK) {
                    this.eventDAO.deleteEvent(ev.getEventId());
                    this.reloadCalendarData();
                }
            });
        });
        titleRow.getChildren().addAll(new Node[]{colorDot, lblTitle, spacer, btnEmail, btnEdit, btnDelete});
        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy", new Locale("vi"));
        String timeStr = ev.isAllDay() ? ev.getStartTime().toLocalDate().format(dateFmt) + " \u00B7 C\u1EA3 ng\u00E0y" : ev.getStartTime().toLocalDate().format(dateFmt) + " \u00B7 " + ev.getStartTime().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")) + " \u2013 " + ev.getEndTime().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"));
        Label lblTime = new Label("\uD83D\uDD52 " + timeStr);
        lblTime.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B;");
        HBox metaRow = new HBox(10.0);
        if (ev.getLocation() != null && !ev.getLocation().isEmpty()) {
            Label lblLoc = new Label("\uD83D\uDCCD " + ev.getLocation());
            lblLoc.setStyle("-fx-font-size: 11px; -fx-text-fill: #94A3B8; -fx-wrap-text: true;");
            lblLoc.setMaxWidth(220.0);
            metaRow.getChildren().add(lblLoc);
        }
        if (ev.getOnlineMeetingLink() != null && !ev.getOnlineMeetingLink().isEmpty()) {
            Label meetBadge = new Label("\uD83C\uDFA5 Video");
            meetBadge.setStyle("-fx-background-color: #EDE9FE; -fx-text-fill: #7C3AED; -fx-font-size: 10px; -fx-font-weight: bold; -fx-background-radius: 4px; -fx-padding: 2px 6px;");
            metaRow.getChildren().add(meetBadge);
        }
        card.getChildren().addAll(new Node[]{titleRow, lblTime});
        if (!metaRow.getChildren().isEmpty()) {
            card.getChildren().add(metaRow);
        }
        card.setOnMouseClicked(e -> {
            if (e.getClickCount() == 1) {
                PopOver popOver = new PopOver();
                popOver.setArrowLocation(PopOver.ArrowLocation.LEFT_TOP);
                popOver.setContentNode(this.buildPopoverContentNode(ev, null, ev.getTitle(), ev.getStartTime().toLocalDate(), ev.getStartTime().toLocalTime(), ev.getEndTime().toLocalTime(), ev.isAllDay(), () -> popOver.hide()));
                popOver.show((Node)card);
            }
        });
        return card;
    }

    private VBox buildPollCard(CalendarPollModel poll) {
        VBox card = new VBox(6.0);
        card.setPadding(new Insets(12.0, 12.0, 12.0, 12.0));
        card.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 10px; -fx-border-color: #E0F2FE; -fx-border-radius: 10px; -fx-border-width: 1px; -fx-cursor: hand;");
        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: #F0F9FF; -fx-background-radius: 10px; -fx-border-color: #7DD3FC; -fx-border-radius: 10px; -fx-border-width: 1px; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, rgba(8,145,178,0.08), 6, 0, 0, 2);"));
        card.setOnMouseExited(e -> card.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 10px; -fx-border-color: #E0F2FE; -fx-border-radius: 10px; -fx-border-width: 1px; -fx-cursor: hand;"));
        HBox titleRow = new HBox(6.0);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Rectangle colorDot = new Rectangle(4.0, 36.0);
        colorDot.setArcWidth(4.0);
        colorDot.setArcHeight(4.0);
        colorDot.setFill((Paint)javafx.scene.paint.Color.web((String)"#0891B2"));
        Label lblTitle = new Label(poll.getTitle());
        lblTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1E293B; -fx-wrap-text: true;");
        lblTitle.setMaxWidth(160.0);
        Region spacer = new Region();
        HBox.setHgrow((Node)spacer, (Priority)Priority.ALWAYS);
        Button btnEmail = new Button("\u2709");
        btnEmail.setStyle("-fx-background-color: transparent; -fx-text-fill: #94A3B8; -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 2px 4px;");
        btnEmail.setOnMouseEntered(e -> btnEmail.setStyle("-fx-background-color: #F1F5F9; -fx-text-fill: #3B82F6; -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 2px 4px; -fx-background-radius: 6px;"));
        btnEmail.setOnMouseExited(e -> btnEmail.setStyle("-fx-background-color: transparent; -fx-text-fill: #94A3B8; -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 2px 4px;"));
        btnEmail.setOnAction(e -> {
            String guests = this.parseGuestEmails(poll.getGuests());
            if (!guests.isEmpty()) {
                this.sendPollEmailNotificationAsync(guests, poll, "https://hocbatrolai.github.io/tutorhub-poll/?code=" + poll.getUniqueCode(), poll.getLocation(), "", "");
                btnEmail.setText("\u2713");
            }
        });
        Button btnEdit = new Button("\u270E");
        btnEdit.setStyle("-fx-background-color: transparent; -fx-text-fill: #94A3B8; -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 2px 4px;");
        btnEdit.setOnMouseEntered(e -> btnEdit.setStyle("-fx-background-color: #F1F5F9; -fx-text-fill: #10B981; -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 2px 4px; -fx-background-radius: 6px;"));
        btnEdit.setOnMouseExited(e -> btnEdit.setStyle("-fx-background-color: transparent; -fx-text-fill: #94A3B8; -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 2px 4px;"));
        Button btnDelete = new Button("\uD83D\uDDD1");
        btnDelete.setStyle("-fx-background-color: transparent; -fx-text-fill: #94A3B8; -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 2px 4px;");
        btnDelete.setOnMouseEntered(e -> btnDelete.setStyle("-fx-background-color: #FEF2F2; -fx-text-fill: #EF4444; -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 2px 4px; -fx-background-radius: 6px;"));
        btnDelete.setOnMouseExited(e -> btnDelete.setStyle("-fx-background-color: transparent; -fx-text-fill: #94A3B8; -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 2px 4px;"));
        btnDelete.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Xác nh\u1EADp xóa");
            confirm.setHeaderText(null);
            confirm.setContentText("\uD83D\uDDD1 X\u00f3a kh\u1EA3o s\u00E1t \"" + poll.getTitle() + "\"?");
            confirm.showAndWait().ifPresent(res -> {
                if (res == ButtonType.OK) {
                    this.pollDAO.deletePoll(poll.getPollId());
                    this.reloadCalendarData();
                }
            });
        });
        titleRow.getChildren().addAll(new Node[]{colorDot, lblTitle, spacer, btnEmail, btnEdit, btnDelete});
        HBox timeRow = new HBox(4.0);
        timeRow.setAlignment(Pos.CENTER_LEFT);
        Label startLabel = new Label("Khung gi\u1EDD: " + poll.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm")));
        startLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B;");
        Label endLabel = new Label(poll.getEndTime().format(DateTimeFormatter.ofPattern("HH:mm")));
        endLabel.setStyle("-fx-background-color: #E0F2FE; -fx-text-fill: #0891B2; -fx-padding: 2px 6px; -fx-background-radius: 4px; -fx-font-size: 11px; -fx-font-weight: bold;");
        timeRow.getChildren().addAll(new Node[]{startLabel, new Label(" \u2013 "), endLabel});
        HBox metaRow = new HBox(8.0);
        metaRow.setAlignment(Pos.CENTER_LEFT);
        Label codeBadge = new Label("M\u00E3: " + poll.getUniqueCode());
        codeBadge.setStyle("-fx-background-color: #E0F2FE; -fx-text-fill: #0891B2; -fx-font-size: 10px; -fx-font-weight: bold; -fx-background-radius: 4px; -fx-padding: 2px 6px;");
        String pollLink = "https://hocbatrolai.github.io/tutorhub-poll/?code=" + poll.getUniqueCode();
        Button btnCopyLink = new Button("\uD83D\uDCCB Link");
        btnCopyLink.setStyle("-fx-background-color: #F0F9FF; -fx-text-fill: #0891B2; -fx-font-size: 10px; -fx-font-weight: bold; -fx-background-radius: 4px; -fx-cursor: hand; -fx-padding: 2px 8px; -fx-border-color: #BAE6FD; -fx-border-width: 1px; -fx-border-radius: 4px;");
        btnCopyLink.setOnAction(e -> {
            StringSelection sel = new StringSelection(pollLink);
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(sel, null);
            btnCopyLink.setText("\u2705 \u0110\u00E3 copy!");
        });
       metaRow.getChildren().addAll(codeBadge, btnCopyLink);
card.getChildren().addAll(titleRow, timeRow, metaRow);
        card.setOnMouseClicked(e -> {
            if (e.getClickCount() == 1) {
                PopOver popOver = new PopOver();
                popOver.setArrowLocation(PopOver.ArrowLocation.LEFT_TOP);
                popOver.setContentNode(this.buildPopoverContentNode(null, poll, poll.getTitle(), LocalDate.now(), poll.getStartTime(), poll.getEndTime(), false, () -> popOver.hide()));
                popOver.show((Node)card);
            }
        });
        return card;
    }
}
