package com.mycompany.tutorhub_enterprise.utils;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.mycompany.tutorhub_enterprise.models.*;
import com.mycompany.tutorhub_enterprise.models.auth.AuthProtocol;
import com.mycompany.tutorhub_enterprise.models.auth.AuthRequest;
import com.mycompany.tutorhub_enterprise.models.auth.AuthResponse;

import java.lang.reflect.Type;
import java.util.List;

public class PacketDeserializer implements JsonDeserializer<Packet> {
    @Override
    public Packet deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        Packet packet = new Packet();

        if (jsonObject.has("action") && !jsonObject.get("action").isJsonNull()) packet.action = jsonObject.get("action").getAsString();
        if (jsonObject.has("payload") && !jsonObject.get("payload").isJsonNull()) packet.payload = jsonObject.get("payload").getAsString();
        if (jsonObject.has("success") && !jsonObject.get("success").isJsonNull()) packet.success = jsonObject.get("success").getAsBoolean();
        if (jsonObject.has("message") && !jsonObject.get("message").isJsonNull()) packet.message = jsonObject.get("message").getAsString();

        if (jsonObject.has("data") && !jsonObject.get("data").isJsonNull()) {
            JsonElement dataElement = jsonObject.get("data");
            String action = packet.action;
            
            if (action == null && jsonObject.has("success")) {
                packet.data = context.deserialize(dataElement, AuthResponse.class);
            } else if (action != null) {
                switch(action) {
                    case AuthProtocol.RESPONSE:
                        packet.data = context.deserialize(dataElement, AuthResponse.class);
                        break;
                    case AuthProtocol.LOGIN:
                    case AuthProtocol.REQUEST_REGISTRATION_OTP:
                    case AuthProtocol.VERIFY_AND_REGISTER:
                    case AuthProtocol.REQUEST_PASSWORD_RESET_OTP:
                    case AuthProtocol.VERIFY_AND_RESET_PASSWORD:
                    case AuthProtocol.REQUEST_SMS_LOGIN_OTP:
                    case AuthProtocol.VERIFY_SMS_LOGIN:
                    case AuthProtocol.REQUEST_PHONE_VERIFICATION_OTP:
                    case AuthProtocol.VERIFY_PHONE_OTP:
                        packet.data = context.deserialize(dataElement, AuthRequest.class);
                        break;
                    case "GET_CONVO_LIST":
                        packet.data = context.deserialize(dataElement, new TypeToken<List<ConversationInfo>>(){}.getType());
                        break;
                    case "GET_MESSAGES":
                        packet.data = context.deserialize(dataElement, new TypeToken<List<Message>>(){}.getType());
                        break;
                    case "SEARCH_USER_RESULT":
                        packet.data = context.deserialize(dataElement, new TypeToken<List<UserInfo>>(){}.getType());
                        break;
                    case "GET_CLASSROOMS_RESPONSE":
                        packet.data = context.deserialize(dataElement, new TypeToken<List<ClassroomGroupModel>>(){}.getType());
                        break;
                    case "GET_CLASSROOM_LESSONS_RESPONSE":
                        packet.data = context.deserialize(dataElement, new TypeToken<List<ClassroomLessonModel>>(){}.getType());
                        break;
                    case "GET_LESSON_MEMBERS_RESPONSE":
                    case "GET_PUBLIC_LESSON_WAITING_ROOM_RESPONSE":
                        packet.data = context.deserialize(dataElement, new TypeToken<List<ClassroomMemberModel>>(){}.getType());
                        break;
                    case "JOIN_PUBLIC_LESSON_SUCCESS":
                    case "PUBLIC_LESSON_APPROVED":
                        try {
                             packet.data = context.deserialize(dataElement, ClassroomLessonModel.class);
                        } catch(Exception e) {
                             packet.data = context.deserialize(dataElement, ClassroomGroupModel.class);
                        }
                        break;
                    case "DEGREES_RESULT":
                    case "CERTIFICATES_RESULT":
                    case "EXPERIENCES_RESULT":
                    case "GET_REELS_RESPONSE":
                    case "GET_REEL_COMMENTS_RESPONSE":
                    case "GET_LOCKET_VIDEOS_RESPONSE":
                    case "GET_BOARDS_FOR_PICKER_RESPONSE":
                        packet.data = context.deserialize(dataElement, new TypeToken<List<String>>(){}.getType());
                        break;
                    case "USER_BOARDS_RESULT":
                        if (dataElement.isJsonArray()) {
                            packet.data = context.deserialize(dataElement, new TypeToken<List<String>>(){}.getType());
                        } else {
                            packet.data = context.deserialize(dataElement, String.class);
                        }
                        break;
                    case "EXAM_LIST":
                    case "GET_EXAMS_RESPONSE":
                        packet.data = context.deserialize(dataElement, new TypeToken<List<com.mycompany.tutorhub_enterprise.models.exam.Exam>>(){}.getType());
                        break;
                    case "EXAM_CREATED":
                        packet.data = context.deserialize(dataElement, com.mycompany.tutorhub_enterprise.models.exam.Exam.class);
                        break;
                    case "EXAM_QUESTIONS_LIST":
                        packet.data = context.deserialize(dataElement, new TypeToken<List<com.mycompany.tutorhub_enterprise.models.exam.Question>>(){}.getType());
                        break;
                    case "QUESTION_ADDED":
                        packet.data = context.deserialize(dataElement, com.mycompany.tutorhub_enterprise.models.exam.Question.class);
                        break;
                    case "SESSION_STARTED":
                        packet.data = context.deserialize(dataElement, com.mycompany.tutorhub_enterprise.models.exam.ExamSession.class);
                        break;
                    default:
                        packet.data = context.deserialize(dataElement, Object.class);
                }
            } else {
                packet.data = context.deserialize(dataElement, Object.class);
            }
        }
        return packet;
    }
}
