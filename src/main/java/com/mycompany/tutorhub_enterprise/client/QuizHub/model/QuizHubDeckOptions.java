package com.mycompany.tutorhub_enterprise.client.quizhub.model;

/**
 * Cấu hình mặc định của 1 đề, map trực tiếp từ sheet Thong_tin_de trong file Excel
 * (Tron_cau_hoi_mac_dinh, Hien_giai_thich_ngay) và từ panel "Tuỳ chọn làm bài" trong quiz.html.
 */
public class QuizHubDeckOptions {

    private boolean shuffleQuestions;
    private boolean showExplanationImmediately;

    public QuizHubDeckOptions() {
    }

    public QuizHubDeckOptions(boolean shuffleQuestions, boolean showExplanationImmediately) {
        this.shuffleQuestions = shuffleQuestions;
        this.showExplanationImmediately = showExplanationImmediately;
    }

    public boolean isShuffleQuestions() {
        return shuffleQuestions;
    }

    public void setShuffleQuestions(boolean shuffleQuestions) {
        this.shuffleQuestions = shuffleQuestions;
    }

    public boolean isShowExplanationImmediately() {
        return showExplanationImmediately;
    }

    public void setShowExplanationImmediately(boolean showExplanationImmediately) {
        this.showExplanationImmediately = showExplanationImmediately;
    }
}
