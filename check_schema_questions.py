import psycopg2
import sys

sys.stdout.reconfigure(encoding='utf-8')

db_url = "postgresql://neondb_owner:npg_2zR6SambqLdQ@ep-fragrant-bonus-aoym56k3-pooler.c-2.ap-southeast-1.aws.neon.tech/neondb?sslmode=require"

try:
    conn = psycopg2.connect(db_url)
    cur = conn.cursor()

    cur.execute("""
        SELECT column_name, data_type, is_nullable
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'questions'
        ORDER BY ordinal_position;
    """)
    print("\n--- QUESTIONS TABLE SCHEMA ---")
    for row in cur.fetchall():
        print(row)

    cur.execute("""
        SELECT column_name, data_type, is_nullable
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'question_options'
        ORDER BY ordinal_position;
    """)
    print("\n--- QUESTION_OPTIONS TABLE SCHEMA ---")
    for row in cur.fetchall():
        print(row)

    cur.close()
    conn.close()
except Exception as e:
    print("Error:", e)
