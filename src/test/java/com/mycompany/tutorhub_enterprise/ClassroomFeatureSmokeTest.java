package com.mycompany.tutorhub_enterprise;

import com.mycompany.tutorhub_enterprise.client.InviteLinkDialog;
import com.mycompany.tutorhub_enterprise.models.ClassroomGroupModel;
import com.mycompany.tutorhub_enterprise.models.ClassroomLessonModel;
import com.mycompany.tutorhub_enterprise.models.ClassroomMemberModel;
import com.mycompany.tutorhub_enterprise.models.Packet;
import com.mycompany.tutorhub_enterprise.server.dao.ClassroomDAO;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassroomFeatureSmokeTest {

    @Test
    void inviteLinkUsesTutorHubSchemeAndTrimsCode() {
        assertEquals(
                "tutorhub://public-lesson?code=CL123456",
                InviteLinkDialog.buildInviteLink(" CL123456 ")
        );
    }

    @Test
    void publicLessonJoinCodeNormalizationAcceptsCodeAndLinks() throws Exception {
        ClassroomDAO dao = new ClassroomDAO();
        Method normalize = ClassroomDAO.class.getDeclaredMethod("normalizeJoinCode", String.class);
        normalize.setAccessible(true);

        assertEquals("CL123456", normalize.invoke(dao, "CL123456"));
        assertEquals("CL123456", normalize.invoke(dao, "tutorhub://public-lesson?code=CL123456"));
        assertEquals("CL123456", normalize.invoke(dao, "https://app.local/public/CL123456?utm=share"));
        assertEquals("CL123456", normalize.invoke(dao, "https://app.local/join?code=CL123456&source=qr"));
        assertEquals("", normalize.invoke(dao, "  "));
    }

    @Test
    void classroomModelsAndPacketsRemainSerializableForSocketTransport() throws Exception {
        ClassroomGroupModel group = new ClassroomGroupModel();
        group.setId(12);
        group.setOwnerId(7);
        group.setName("Public Math");
        group.setJoinCode("CL654321");

        ClassroomLessonModel lesson = new ClassroomLessonModel();
        lesson.setId(33);
        lesson.setClassroomId(group.getId());
        lesson.setTitle("Public Math");
        lesson.setBoardId("LESSON_33");
        lesson.setLessonType("PUBLIC");
        lesson.setLobbyEnabled(true);
        lesson.setMemberStatus("WAITING");

        ClassroomMemberModel member = new ClassroomMemberModel();
        member.setLessonId(lesson.getId());
        member.setClassroomId(group.getId());
        member.setUserId(99);
        member.setFullName("Student One");
        member.setMemberStatus("WAITING");

        Packet packet = new Packet("JOIN_PUBLIC_LESSON_WAITING", lesson);
        Packet restored = roundTrip(packet);
        ClassroomLessonModel restoredLesson = (ClassroomLessonModel) restored.data;

        assertEquals("JOIN_PUBLIC_LESSON_WAITING", restored.action);
        assertEquals("PUBLIC", restoredLesson.getLessonType());
        assertEquals("WAITING", restoredLesson.getMemberStatus());

        ClassroomMemberModel restoredMember = roundTrip(member);
        assertEquals(99, restoredMember.getUserId());
        assertEquals("WAITING", restoredMember.getMemberStatus());
    }

    @Test
    void lobbyDaoApiShapeExists() throws Exception {
        assertNotNull(ClassroomDAO.class.getDeclaredMethod("joinPublicLessonByCode", int.class, String.class));
        assertNotNull(ClassroomDAO.class.getDeclaredMethod("getWaitingMembersForLesson", int.class, int.class));
        assertNotNull(ClassroomDAO.class.getDeclaredMethod("approveLessonStudent", int.class, int.class, int.class));
    }

    @Test
    void clientAndServerPacketActionsStayWired() throws Exception {
        String server = readSource("src/main/java/com/mycompany/tutorhub_enterprise/server/ClientHandler.java");
        String dashboard = readSource("src/main/java/com/mycompany/tutorhub_enterprise/client/MainDashboard.java");
        String manager = readSource("src/main/java/com/mycompany/tutorhub_enterprise/client/ClassManagerTab.java");
        String blackboard = readSource("src/main/java/com/mycompany/tutorhub_enterprise/client/BlackboardManagerTab.java");

        assertContains(server, "CREATE_CLASSROOM_AND_ENTER");
        assertContains(server, "CREATE_PUBLIC_LESSON");
        assertContains(server, "JOIN_PUBLIC_LESSON");
        assertContains(server, "JOIN_PUBLIC_LESSON_WAITING");
        assertContains(server, "GET_PUBLIC_LESSON_WAITING_ROOM");
        assertContains(server, "APPROVE_PUBLIC_LESSON_STUDENT");
        assertContains(server, "PUBLIC_LESSON_APPROVED");

        assertContains(dashboard, "GET_CLASSROOM_LESSONS_RESPONSE");
        assertContains(dashboard, "CREATE_PUBLIC_LESSON_SUCCESS");
        assertContains(dashboard, "JOIN_PUBLIC_LESSON_WAITING");
        assertContains(dashboard, "PUBLIC_LESSON_APPROVED");

        assertContains(manager, "Copy Code");
        assertContains(manager, "Copy Link");
        assertContains(manager, "Lobby");
        assertContains(manager, "Chờ duyệt");
        assertContains(blackboard, "Tạo bảng vẽ mới");
        assertFalse(blackboard.contains("CREATE_PUBLIC_LESSON"));
        assertFalse(blackboard.contains("CREATE_CLASSROOM_AND_ENTER"));
        assertFalse(blackboard.contains("JOIN_PUBLIC_LESSON"));
    }

    private static String readSource(String path) throws Exception {
        return Files.readString(Path.of(path));
    }

    private static void assertContains(String source, String expected) {
        assertTrue(source.contains(expected), "Expected source to contain: " + expected);
    }

    @SuppressWarnings("unchecked")
    private static <T> T roundTrip(T value) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(value);
        }
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            return (T) in.readObject();
        }
    }
}
