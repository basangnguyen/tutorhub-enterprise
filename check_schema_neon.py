import psycopg2
import os

db_url = "postgresql://neondb_owner:npg_2zR6SambqLdQ@ep-fragrant-bonus-aoym56k3-pooler.c-2.ap-southeast-1.aws.neon.tech/neondb?sslmode=require"

try:
    conn = psycopg2.connect(db_url)
    cur = conn.cursor()
    
    print("--- TABLES ---")
    cur.execute("""
        SELECT table_name
        FROM information_schema.tables
        WHERE table_schema = 'public'
        ORDER BY table_name;
    """)
    for row in cur.fetchall():
        print(row[0])
        
    print("\n--- USER COLUMNS ---")
    cur.execute("""
        SELECT table_name, column_name, data_type
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND (
            table_name ILIKE '%user%'
            OR table_name ILIKE '%account%'
            OR table_name ILIKE '%student%'
            OR column_name ILIKE '%password%'
            OR column_name ILIKE '%role%'
          )
        ORDER BY table_name, ordinal_position;
    """)
    for row in cur.fetchall():
        print(f"{row[0]} | {row[1]} | {row[2]}")
        
    print("\n--- EXAM COLUMNS ---")
    cur.execute("""
        SELECT table_name, column_name, data_type
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND (
            table_name ILIKE '%exam%'
            OR table_name ILIKE '%test%'
            OR table_name ILIKE '%attempt%'
            OR table_name ILIKE '%question%'
            OR table_name ILIKE '%paper%'
          )
        ORDER BY table_name, ordinal_position;
    """)
    for row in cur.fetchall():
        print(f"{row[0]} | {row[1]} | {row[2]}")

    cur.close()
    conn.close()
except Exception as e:
    print("Error:", e)
