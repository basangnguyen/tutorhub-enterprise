package com.mycompany.tutorhub_enterprise.server.services;

import com.mycompany.tutorhub_enterprise.models.Packet;
import com.mycompany.tutorhub_enterprise.models.exam.QuestionBank;
import com.mycompany.tutorhub_enterprise.server.dao.QuestionBankDAO;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuestionBankService {

    public static Packet handleCreateBank(int creatorId, String name, String description) {
        if (name == null || name.trim().isEmpty()) {
            Packet p = new Packet(false, "Tên ngân hàng câu hỏi không được để trống");
            p.action = "QUESTION_BANK_CREATE_FAILED";
            return p;
        }
        
        int bankId = QuestionBankDAO.createQuestionBank(name, description, creatorId);
        if (bankId > 0) {
            Map<String, Object> data = new HashMap<>();
            data.put("bankId", bankId);
            Packet p = new Packet(true, "Tạo ngân hàng câu hỏi thành công", data);
            p.action = "QUESTION_BANK_CREATE_SUCCESS";
            return p;
        }
        Packet p = new Packet(false, "Lỗi server khi tạo ngân hàng câu hỏi");
        p.action = "QUESTION_BANK_CREATE_FAILED";
        return p;
    }

    public static Packet handleListBanks(int creatorId) {
        List<QuestionBank> banks = QuestionBankDAO.listQuestionBanksByCreator(creatorId);
        Packet p = new Packet(true, "Danh sách ngân hàng câu hỏi", banks);
        p.action = "QUESTION_BANK_LIST_SUCCESS";
        return p;
    }

    public static Packet handleGetBankDetail(int bankId) {
        QuestionBank bank = QuestionBankDAO.getQuestionBankById(bankId);
        if (bank != null) {
            Packet p = new Packet(true, "Lấy thông tin thành công", bank);
            p.action = "GET_BANK_SUCCESS";
            return p;
        }
        Packet p2 = new Packet(false, "Ngân hàng câu hỏi không tồn tại");
        p2.action = "GET_BANK_FAILED";
        return p2;
    }

    public static Packet handleUpdateBank(int creatorId, int bankId, String name, String description) {
        if (name == null || name.trim().isEmpty()) {
            Packet p = new Packet(false, "Tên ngân hàng câu hỏi không được để trống");
            p.action = "UPDATE_BANK_FAILED";
            return p;
        }
        
        QuestionBank existing = QuestionBankDAO.getQuestionBankById(bankId);
        if (existing == null) {
            Packet p = new Packet(false, "Ngân hàng câu hỏi không tồn tại");
            p.action = "UPDATE_BANK_FAILED";
            return p;
        }
        if (existing.creatorId != creatorId) {
            Packet p = new Packet(false, "Bạn không có quyền sửa ngân hàng câu hỏi này");
            p.action = "UPDATE_BANK_FAILED";
            return p;
        }
        
        boolean ok = QuestionBankDAO.updateQuestionBank(bankId, name, description);
        if (ok) {
            Packet p = new Packet(true, "Cập nhật thành công");
            p.action = "UPDATE_BANK_SUCCESS";
            return p;
        }
        Packet p = new Packet(false, "Lỗi server khi cập nhật");
        p.action = "UPDATE_BANK_FAILED";
        return p;
    }
}
