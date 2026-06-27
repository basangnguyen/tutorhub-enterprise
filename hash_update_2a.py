import bcrypt
import psycopg2

password = b"123456"
# Generate salt with 12 rounds
salt = bcrypt.gensalt(rounds=12)
hashed = bcrypt.hashpw(password, salt)
hashed_str = hashed.decode('utf-8')

# Convert $2b$ to $2a$ for jbcrypt 0.4 compatibility
if hashed_str.startswith('$2b$'):
    hashed_str = '$2a$' + hashed_str[4:]

print("GENERATED_HASH:", hashed_str)

db_url = "postgresql://neondb_owner:npg_2zR6SambqLdQ@ep-fragrant-bonus-aoym56k3-pooler.c-2.ap-southeast-1.aws.neon.tech/neondb?sslmode=require"

try:
    conn = psycopg2.connect(db_url)
    cur = conn.cursor()
    
    # Update the hash
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
