import psycopg2
import sys

sys.stdout.reconfigure(encoding='utf-8')

db_url = "postgresql://neondb_owner:npg_2zR6SambqLdQ@ep-fragrant-bonus-aoym56k3-pooler.c-2.ap-southeast-1.aws.neon.tech/neondb?sslmode=require"

try:
    conn = psycopg2.connect(db_url)
    cur = conn.cursor()

    print("Seeding questions...")
    cur.execute("""
        WITH q1 AS (
            INSERT INTO questions (question_type, points, content)
            VALUES ('MCQ', 5, 'TSE_SMOKE_Q1: 2 + 2 bằng bao nhiêu?')
            RETURNING id
        ),
        q1o1 AS (
            INSERT INTO question_options (question_id, content, is_correct)
            SELECT id, '3', false FROM q1
        ),
        q1o2 AS (
            INSERT INTO question_options (question_id, content, is_correct)
            SELECT id, '4', true FROM q1
        ),
        q1o3 AS (
            INSERT INTO question_options (question_id, content, is_correct)
            SELECT id, '5', false FROM q1
        ),
        map1 AS (
            INSERT INTO exam_paper_questions (paper_id, question_id, order_idx, points)
            SELECT 3, id, 1, 5 FROM q1
        ),
        q2 AS (
            INSERT INTO questions (question_type, points, content)
            VALUES ('MCQ', 5, 'TSE_SMOKE_Q2: Java là ngôn ngữ lập trình đúng hay sai?')
            RETURNING id
        ),
        q2o1 AS (
            INSERT INTO question_options (question_id, content, is_correct)
            SELECT id, 'Đúng', true FROM q2
        ),
        q2o2 AS (
            INSERT INTO question_options (question_id, content, is_correct)
            SELECT id, 'Sai', false FROM q2
        ),
        map2 AS (
            INSERT INTO exam_paper_questions (paper_id, question_id, order_idx, points)
            SELECT 3, id, 2, 5 FROM q2
        )
        SELECT 'TSE_SMOKE_QUESTIONS_CREATED_FOR_PAPER_3' AS result;
    """)
    print("Seed result:", cur.fetchall())
    
    print("\nChecking mapped questions...")
    cur.execute("""
        SELECT epq.paper_id, epq.question_id, epq.order_idx, epq.points, q.content
        FROM exam_paper_questions epq
        JOIN questions q ON q.id = epq.question_id
        WHERE epq.paper_id = 3
        ORDER BY epq.order_idx;
    """)
    for row in cur.fetchall():
        print(row)
        
    print("\nAssigning paper 3 to exam 10...")
    cur.execute("""
        UPDATE exams
        SET paper_id = 3,
            updated_at = CURRENT_TIMESTAMP
        WHERE id = 10;
    """)
    
    print("\nChecking exam 10...")
    cur.execute("""
        SELECT id, title, paper_id, status
        FROM exams
        WHERE id = 10;
    """)
    for row in cur.fetchall():
        print(row)
        
    conn.commit()
    cur.close()
    conn.close()
except Exception as e:
    print("Error:", e)
