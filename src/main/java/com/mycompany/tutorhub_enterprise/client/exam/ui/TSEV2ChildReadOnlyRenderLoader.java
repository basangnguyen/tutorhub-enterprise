package com.mycompany.tutorhub_enterprise.client.exam.ui;

import com.mycompany.tutorhub_enterprise.models.exam.V2ExamHandoffBundle;
import com.mycompany.tutorhub_enterprise.models.exam.V2ExamHandoffQuestion;
import com.mycompany.tutorhub_enterprise.models.exam.V2ExamHandoffOption;
import com.mycompany.tutorhub_enterprise.models.exam.readonly.TSEV2ReadOnlyExamRenderModel;
import com.mycompany.tutorhub_enterprise.models.exam.readonly.TSEV2ReadOnlyQuestionView;
import com.mycompany.tutorhub_enterprise.models.exam.readonly.TSEV2ReadOnlyOptionView;

public class TSEV2ChildReadOnlyRenderLoader {

    public static TSEV2ReadOnlyExamRenderModel sanitizeForReadOnlyRender(V2ExamHandoffBundle bundle) {
        if (bundle == null) {
            return null;
        }

        TSEV2ReadOnlyExamRenderModel model = new TSEV2ReadOnlyExamRenderModel();
        model.setExamId(bundle.examId);
        model.setPaperId(bundle.paperId);
        model.setAttemptId(bundle.attemptId);
        model.setDeadlineAt(bundle.deadlineAt);
        model.setQuestionCount(bundle.questions != null ? bundle.questions.size() : 0);
        model.setTotalScore(bundle.totalScore);
        model.setPackageHash(bundle.packageHash);

        if (bundle.questions != null) {
            for (V2ExamHandoffQuestion q : bundle.questions) {
                TSEV2ReadOnlyQuestionView qv = new TSEV2ReadOnlyQuestionView();
                qv.setId(q.questionId);
                qv.setContent(q.content);
                qv.setOrderIndex(q.orderIndex);

                if (q.options != null) {
                    for (V2ExamHandoffOption opt : q.options) {
                        TSEV2ReadOnlyOptionView ov = new TSEV2ReadOnlyOptionView();
                        ov.setId(opt.optionId);
                        ov.setContent(opt.content);
                        qv.getOptions().add(ov);
                    }
                }
                model.getQuestions().add(qv);
            }
        }

        return model;
    }
}
