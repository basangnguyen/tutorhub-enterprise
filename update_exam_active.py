import psycopg2
import sys

sys.stdout.reconfigure(encoding='utf-8')

db_url = "postgresql://neondb_owner:npg_2zR6SambqLdQ@ep-fragrant-bonus-aoym56k3-pooler.c-2.ap-southeast-1.aws.neon.tech/neondb?sslmode=require"

try:
    conn = psycopg2.connect(db_url)
    cur = conn.cursor()
        
    print("Setting Exam 10 to ACTIVE...")
    cur.execute("""
        UPDATE exams
        SET status = 'ACTIVE',
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
