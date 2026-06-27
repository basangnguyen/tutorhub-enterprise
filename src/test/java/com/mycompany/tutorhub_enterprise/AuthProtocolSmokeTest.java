package com.mycompany.tutorhub_enterprise;

import com.mycompany.tutorhub_enterprise.models.Packet;
import com.mycompany.tutorhub_enterprise.models.auth.AuthProtocol;
import com.mycompany.tutorhub_enterprise.models.auth.AuthRequest;
import com.mycompany.tutorhub_enterprise.models.auth.AuthResponse;
import com.mycompany.tutorhub_enterprise.utils.SerializationUtils;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthProtocolSmokeTest {

    @Test
    void authRequestAndResponseRemainSerializableForSocketTransport() throws Exception {
        AuthRequest request = AuthRequest.register("teacher@example.com", "123456", "pass,with,comma", "Teacher, One");
        Packet requestPacket = roundTrip(new Packet(AuthProtocol.VERIFY_AND_REGISTER, request));

        AuthRequest restoredRequest = (AuthRequest) requestPacket.data;
        assertEquals(AuthProtocol.VERIFY_AND_REGISTER, requestPacket.action);
        assertEquals(request.getRequestId(), restoredRequest.getRequestId());
        assertEquals("pass,with,comma", restoredRequest.getPassword());
        assertEquals("Teacher, One", restoredRequest.getFullName());

        AuthResponse response = AuthResponse.login(
                request.getRequestId(),
                "Dang nhap thanh cong.",
                "DASHBOARD_GO|42|TUTOR|NO_AVATAR"
        );
        Packet responsePacket = roundTrip(new Packet(AuthProtocol.RESPONSE, response));
        AuthResponse restoredResponse = (AuthResponse) responsePacket.data;

        assertEquals(AuthProtocol.RESPONSE, responsePacket.action);
        assertTrue(restoredResponse.isSuccess());
        assertEquals(request.getRequestId(), restoredResponse.getRequestId());
        assertEquals("DASHBOARD_GO|42|TUTOR|NO_AVATAR", restoredResponse.getDashboardPayload());
    }

    @Test
    void authClientAndServerUseTheNewProtocolActions() throws Exception {
        String authClient = readSource("src/main/java/com/mycompany/tutorhub_enterprise/client/AuthClient.java");
        String loginFrame = readSource("src/main/java/com/mycompany/tutorhub_enterprise/client/LoginFrame.java");
        String signUpFrame = readSource("src/main/java/com/mycompany/tutorhub_enterprise/client/SignUpFrame.java");
        String server = readSource("src/main/java/com/mycompany/tutorhub_enterprise/server/ClientHandler.java");

        assertTrue(authClient.contains("AuthProtocol.LOGIN"));
        assertTrue(authClient.contains("AuthProtocol.REQUEST_REGISTRATION_OTP"));
        assertTrue(authClient.contains("AuthProtocol.VERIFY_AND_REGISTER"));
        assertTrue(authClient.contains("AuthProtocol.REQUEST_SMS_LOGIN_OTP"));
        assertTrue(authClient.contains("AuthProtocol.VERIFY_SMS_LOGIN"));

        assertTrue(loginFrame.contains("new AuthClient().login"));
        assertTrue(loginFrame.contains("requestPasswordResetOtp"));
        assertTrue(loginFrame.contains("resetPassword"));

        assertTrue(signUpFrame.contains("requestRegistrationOtp"));
        assertTrue(signUpFrame.contains("register(email, otp, pass, name)"));

        assertTrue(server.contains("case AuthProtocol.LOGIN"));
        assertTrue(server.contains("case AuthProtocol.REQUEST_REGISTRATION_OTP"));
        assertTrue(server.contains("case AuthProtocol.VERIFY_AND_REGISTER"));
        assertFalse(signUpFrame.contains("VERIFY_AND_REGISTER\", payload"));
    }

    @Test
    void gsonPacketTransportRestoresAuthRequestAndResponseTypes() throws Exception {
        AuthRequest request = AuthRequest.login("teacher@example.com", "secret");
        Packet decodedRequest = (Packet) SerializationUtils.deserialize(
                SerializationUtils.serialize(new Packet(AuthProtocol.LOGIN, request))
        );

        assertEquals(AuthProtocol.LOGIN, decodedRequest.action);
        assertTrue(decodedRequest.data instanceof AuthRequest);
        assertEquals(request.getRequestId(), ((AuthRequest) decodedRequest.data).getRequestId());

        AuthResponse response = AuthResponse.fail(request.getRequestId(), "Sai Email hoac mat khau.");
        Packet decodedResponse = (Packet) SerializationUtils.deserialize(
                SerializationUtils.serialize(new Packet(AuthProtocol.RESPONSE, response))
        );

        assertEquals(AuthProtocol.RESPONSE, decodedResponse.action);
        assertTrue(decodedResponse.data instanceof AuthResponse);
        assertEquals(request.getRequestId(), ((AuthResponse) decodedResponse.data).getRequestId());
        assertEquals("Sai Email hoac mat khau.", ((AuthResponse) decodedResponse.data).getMessage());
    }

    private static String readSource(String path) throws Exception {
        return Files.readString(Path.of(path));
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
