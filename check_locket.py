import psycopg2
db_url = "postgresql://neondb_owner:npg_2zR6SambqLdQ@ep-fragrant-bonus-aoym56k3-pooler.c-2.ap-southeast-1.aws.neon.tech/neondb?sslmode=require"
conn = psycopg2.connect(db_url)
cur = conn.cursor()
cur.execute("SELECT table_name, column_name, data_type FROM information_schema.columns WHERE table_schema = 'public' AND table_name LIKE 'locket_%' ORDER BY table_name, ordinal_position;")
for row in cur.fetchall():
    print(f"{row[0]} | {row[1]} | {row[2]}")
cur.close()
conn.close()
