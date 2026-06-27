import psycopg2

db_url = "postgresql://neondb_owner:npg_2zR6SambqLdQ@ep-fragrant-bonus-aoym56k3-pooler.c-2.ap-southeast-1.aws.neon.tech/neondb?sslmode=require"

try:
    conn = psycopg2.connect(db_url)
    cur = conn.cursor()
    
    print("--- TABLES ---")
    cur.execute("""
    SELECT table_name
    FROM information_schema.tables
    WHERE table_schema = 'public'
      AND (
        table_name ILIKE '%paper%'
        OR table_name ILIKE '%exam%'
        OR table_name ILIKE '%question%'
        OR table_name ILIKE '%test%'
      )
    ORDER BY table_name;
    """)
    for row in cur.fetchall():
        print(row[0])
        
    print("\n--- SCHEMA ---")
    cur.execute("""
    SELECT table_name, column_name, data_type, is_nullable
    FROM information_schema.columns
    WHERE table_schema = 'public'
      AND (
        table_name ILIKE '%paper%'
        OR table_name ILIKE '%exam%'
        OR table_name ILIKE '%question%'
        OR table_name ILIKE '%test%'
      )
    ORDER BY table_name, ordinal_position;
    """)
    for row in cur.fetchall():
        print(f"{row[0]}: {row[1]} ({row[2]}, nullable={row[3]})")
        
    print("\n--- PAPERS ---")
    cur.execute("SELECT * FROM exam_papers ORDER BY id DESC LIMIT 5;")
    for row in cur.fetchall():
        print(row)

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
    
    cur.close()
    conn.close()
except Exception as e:
    print("Error:", e)
