import psycopg2
import sys

sys.stdout.reconfigure(encoding='utf-8')

db_url = "postgresql://neondb_owner:npg_2zR6SambqLdQ@ep-fragrant-bonus-aoym56k3-pooler.c-2.ap-southeast-1.aws.neon.tech/neondb?sslmode=require"

try:
    conn = psycopg2.connect(db_url)
    cur = conn.cursor()
    
    print("\n--- TABLES ---")
    cur.execute("""
        SELECT table_name
        FROM information_schema.tables
        WHERE table_schema = 'public'
          AND (
            table_name ILIKE '%answer%'
            OR table_name ILIKE '%submission%'
            OR table_name ILIKE '%attempt%'
            OR table_name ILIKE '%exam%'
          )
        ORDER BY table_name;
    """)
    for row in cur.fetchall():
        print(row)

    print("\n--- COLUMNS ---")
    cur.execute("""
        SELECT table_name, column_name, data_type, is_nullable
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND (
            table_name ILIKE '%answer%'
            OR table_name ILIKE '%submission%'
            OR table_name ILIKE '%attempt%'
            OR table_name ILIKE '%exam%'
          )
        ORDER BY table_name, ordinal_position;
    """)
    for row in cur.fetchall():
        print(row)
        
    cur.close()
    conn.close()
except Exception as e:
    print("Error:", e)
