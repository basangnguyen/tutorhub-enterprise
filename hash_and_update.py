import bcrypt
import psycopg2

password = b"123456"
# Generate salt with 12 rounds (same as Java's BCrypt.gensalt(12))
salt = bcrypt.gensalt(rounds=12)
hashed = bcrypt.hashpw(password, salt)
hashed_str = hashed.decode('utf-8')

print("GENERATED_HASH:", hashed_str)

db_url = "postgresql://neondb_owner:npg_2zR6SambqLdQ@ep-fragrant-bonus-aoym56k3-pooler.c-2.ap-southeast-1.aws.neon.tech/neondb?sslmode=require"

try:
    conn = psycopg2.connect(db_url)
    cur = conn.cursor()
    
    # Check current hash
    cur.execute("SELECT password_hash FROM users WHERE email = 'student.test.safe@tutorhub.local'")
    row = cur.fetchone()
    if row:
        print("OLD_HASH:", row[0])
    
    # Update the hash, role, status
    update_sql = """
    UPDATE users
    SET password_hash = %s,
        role = 'STUDENT',
        status = 'ACTIVE'
    WHERE email = 'student.test.safe@tutorhub.local'
    """
    cur.execute(update_sql, (hashed_str,))
    print("ROWS UPDATED:", cur.rowcount)
    
    conn.commit()
    cur.close()
    conn.close()
    print("SUCCESS")
except Exception as e:
    print("Error:", e)
