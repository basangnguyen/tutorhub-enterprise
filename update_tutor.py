import psycopg2
import sys

sys.stdout.reconfigure(encoding='utf-8')

db_url = "postgresql://neondb_owner:npg_2zR6SambqLdQ@ep-fragrant-bonus-aoym56k3-pooler.c-2.ap-southeast-1.aws.neon.tech/neondb?sslmode=require"

try:
    conn = psycopg2.connect(db_url)
    cur = conn.cursor()

    print("\n--- TEST TUTOR ACCOUNT ---")
    cur.execute("SELECT id, email, full_name, role, status FROM users WHERE email = 'dev.tse@test.local' OR email ILIKE '%dev.tse%';")
    tutor_row = cur.fetchone()
    if tutor_row:
        print(tutor_row)
        if tutor_row[4] != 'ACTIVE':
            print("Updating status to ACTIVE...")
            cur.execute("UPDATE users SET status = 'ACTIVE' WHERE id = %s", (tutor_row[0],))
            conn.commit()
    else:
        print("Tutor account not found.")

    # Create dummy data if needed
    cur.execute("SELECT count(*) FROM exam_papers WHERE title LIKE 'TSE_SMOKE_PAPER%'")
    count = cur.fetchone()[0]
    if count == 0 and tutor_row:
        print("Creating mock paper...")
        cur.execute("""
            INSERT INTO exam_papers (title, description, total_score, creator_id, status)
            VALUES ('TSE_SMOKE_PAPER_01', 'Mock for test', 10, %s, 'DRAFT') RETURNING id
        """, (tutor_row[0],))
        print("Inserted mock paper ID:", cur.fetchone()[0])
        conn.commit()
    
    cur.close()
    conn.close()
except Exception as e:
    print("Error:", e)
