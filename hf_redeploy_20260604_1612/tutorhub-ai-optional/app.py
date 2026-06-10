import os
import sys
import io
import datetime
import json
import psycopg2
import base64
import uuid
import asyncio
from typing import Optional

from fastapi import FastAPI, UploadFile, File, Form
from fastapi.responses import JSONResponse, FileResponse, StreamingResponse
from fastapi.staticfiles import StaticFiles
from pydantic import BaseModel
import uvicorn

from langchain_groq import ChatGroq
from langchain_huggingface import HuggingFaceEmbeddings
from langchain_chroma import Chroma
from langchain_core.messages import SystemMessage, HumanMessage, AIMessage, ToolMessage
from langchain_core.tools import tool
from duckduckgo_search import DDGS
from groq import Groq
import edge_tts
import google.generativeai as genai
import PIL.Image

# ==========================================
# FIX CONSOLE ENCODING (Windows cp1252)
# ==========================================
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')
sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding='utf-8', errors='replace')

# ==========================================
# CAU HINH
# ==========================================
GROQ_KEY = os.getenv("TUTORHUB_GROQ_KEY") or os.getenv("GROQ_KEY", "")
GEMINI_API_KEY = os.getenv("TUTORHUB_GEMINI_API_KEY") or os.getenv("GEMINI_API_KEY", "")
DB_URL = os.getenv("TUTORHUB_AI_DB_URL") or os.getenv("DATABASE_URL", "")

from fastapi.middleware.cors import CORSMiddleware

app = FastAPI(title="Lavie AI API", description="AI Backend for TutorHub Enterprise (Voice & Vision)")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

os.makedirs("static/audio", exist_ok=True)
os.makedirs("temp", exist_ok=True)
app.mount("/static", StaticFiles(directory="static"), name="static")

groq_client = Groq(api_key=GROQ_KEY) if GROQ_KEY else None
if GEMINI_API_KEY:
    genai.configure(api_key=GEMINI_API_KEY)
    gemini_model = genai.GenerativeModel('gemini-flash-latest')
else:
    gemini_model = None
    print("WARNING: Gemini vision is disabled because GEMINI_API_KEY is not configured.")

# ==========================================
# KNOWLEDGE BASE
# ==========================================
tutorhub_knowledge_base = [
    "Ten he thong: TutorHub Enterprise.",
    "TutorHub Enterprise la nen tang quan ly va ket noi gia su thong minh the he moi tich hop AI.",
    "Nguoi sang lap va phat trien nen tang la Nguyen Ba Sang — sinh vien nganh Cong nghe Thong tin tai Hoc vien Ky thuat Mat ma (KMA).",
    "TutorHub duoc xay dung voi muc tieu ho tro gia su quan ly toan dien cong viec giang day tren mot he sinh thai duy nhat.",
    "He thong huong toi viec so hoa hoat dong day hoc, quan ly lop hoc va ho tro giang day truc tuyen bang AI.",
    "Doi tuong su dung chinh gom: Gia su, tro giang, giao vien tu do va trung tam day hoc.",
    "Phong cach thiet ke giao dien: Hien dai, toi gian, truc quan, toi uu trai nghiem nguoi dung.",
    "Mau sac thuong hieu chinh cua TutorHub la xanh duong ket hop trang va tim nhat cong nghe.",
    "Khau hieu he thong: AI-Powered Tutoring.",
    "Ung dung desktop duoc phat trien bang Java Swing ket hop FlatLaf de tao giao dien hien dai.",
    "May chu backend giao tiep thoi gian thuc thong qua Socket TCP tai cong 8888.",
    "He thong AI hoat dong qua Python FastAPI Server tai cong 5000.",
    "Lavie AI su dung mo hinh ngon ngu lon ket hop RAG (Retrieval-Augmented Generation).",
    "He thong ho tro tim kiem Internet tu dong de nang cao chat luong phan hoi.",
    "Du lieu nguoi dung duoc dong bo theo thoi gian thuc giua client va server.",
    "TutorHub duoc toi uu cho moi truong Windows Desktop.",
    "Bang tin lop la khu vuc hien thi danh sach cac lop hoc moi nhat danh cho gia su.",
    "Moi the lop hien thi day du thong tin: mon hoc, dia chi, hoc phi, lich hoc va yeu cau.",
    "Gia su co the nhan lop truc tiep bang nut 'Nhan lop'.",
    "He thong ho tro loc lop theo khoang cach, hoc phi, mon hoc va trang thai.",
    "Cac lop HOT hoac VIP se duoc gan nhan noi bat.",
    "The lop duoc thiet ke dang card hien dai voi hieu ung hover noi.",
    "Gia su co the luu lop yeu thich de xem lai sau.",
    "He thong Chat ho tro nhan tin thoi gian thuc giua gia su va hoc vien.",
    "Chat ho tro hien thi trang thai online/offline.",
    "Nguoi dung co the tim kiem ban be hoac cuoc tro chuyen bang thanh tim kiem.",
    "He thong ho tro gui emoji, hinh anh va tai lieu.",
    "Danh sach cuoc tro chuyen duoc cap nhat theo thoi gian thuc.",
    "Tin nhan moi se hien thi badge thong bao mau do.",
    "Lich day hien thi duoi dang luoi truc quan theo tuan.",
    "Gia su co the quan ly lich hoc ca nhan ngay tren he thong.",
    "He thong ho tro nhac lich hoc tu dong.",
    "Moi buoi hoc hien thi day du mon hoc, thoi gian va dia diem.",
    "Bang ve la khong gian giang day truc tuyen danh cho gia su.",
    "Nguoi dung co the viet, ve va phac thao noi dung bai giang.",
    "He thong ho tro tao nhieu bang ve khac nhau.",
    "Bang ve co the luu len may chu de mo lai sau.",
    "Canvas ho tro thao tac muot ma va toi uu cho day hoc online.",
    "Muc Tai lieu cho phep tai len va quan ly giao trinh hoc tap.",
    "Nguoi dung co the luu tai lieu PDF, Word hoac hinh anh.",
    "Todo List giup gia su quan ly cong viec hang ngay.",
    "Todo ho tro them, sua, xoa va danh dau hoan thanh.",
    "Nguoi dung co the nang cap tai khoan Premium.",
    "Premium mo khoa cac tinh nang AI nang cao.",
    "Tai khoan Premium duoc uu tien hien thi lop hoc HOT.",
    "Premium ho tro luu tru du lieu lon hon.",
    "Giao dien Premium su dung hieu ung tim xanh cong nghe hien dai.",
    "Lavie AI la tro ly ao doc quyen cua TutorHub Enterprise.",
    "Lavie hoat dong 24/7 nham ho tro gia su trong qua trinh su dung he thong.",
    "Lavie duoc thiet ke duoi dang chatbot noi o goc duoi ben phai man hinh.",
    "Giao dien Lavie theo phong cach AI hien dai, mem mai va than thien.",
    "Lavie co the tro chuyen tu nhien voi nguoi dung bang tieng Viet.",
    "Lavie ho tro giai dap cach su dung phan mem TutorHub.",
    "Lavie ho tro tim kiem kien thuc tren Internet.",
    "Lavie ho tro goi y phuong phap giang day hieu qua.",
    "Lavie ho tro tao ke hoach hoc tap cho hoc vien.",
    "Lavie co the ho tro giai bai tap co ban.",
    "Lavie co the ho tro viet noi dung giang day va de cuong.",
    "Lavie co hai y nghia dac biet.",
    "Y nghia thu nhat: Lavie la cach doc gan giong ten Le Vy.",
    "Y nghia thu hai: Lavie mang y nghia 'La vi em'.",
    "Lavie duoc tao ra nham mang lai cam giac than thien va gan gui.",
    "Nguoi tao ra Lavie la Nguyen Ba Sang.",
    "Lavie duoc xem la linh hon AI cua TutorHub Enterprise.",
    "Lavie giao tiep than thien, thong minh va chuyen nghiep.",
    "Lavie luon uu tien ho tro nguoi dung nhanh chong.",
    "Lavie su dung cach noi tu nhien, de hieu.",
    "Lavie khong tra loi qua may moc.",
    "Lavie co the su dung emoji nhe nhang de tang su than thien.",
    "Lavie luon uu tien trai nghiem nguoi dung.",
    "Gia su nen chuan bi giao an truoc moi buoi hoc.",
    "Phuong phap Pomodoro giup hoc tap hieu qua hon.",
    "Viec hoc theo so do tu duy giup ghi nho tot hon.",
    "Gia su nen ket hop hinh anh va vi du truc quan khi giang day.",
    "Luyen tap thuong xuyen la cach tot nhat de cai thien ky nang hoc tap.",
    "Gia su can tao moi truong hoc tap tich cuc va thoai mai.",
    "Hoc sinh tiep thu tot hon khi duoc tuong tac truc tiep.",
    "De nhan lop: Vao muc 'Bang tin lop' va nhan nut 'Nhan lop'.",
    "De mo chat: Chon muc 'Chat' o thanh menu ben trai.",
    "De tao bang ve: Vao muc 'Bang ve' va chon 'Tao bang moi'.",
    "De doi avatar: Truy cap Ho so ca nhan va tai anh moi len.",
    "De nang cap Premium: Nhan nut 'Nang cap ngay' o giao dien chinh.",
    "TutorHub Enterprise la du an cong nghe giao duc tich hop AI.",
    "He thong duoc phat trien theo dinh huong EdTech hien dai.",
    "TutorHub huong toi viec xay dung he sinh thai gia su thong minh tai Viet Nam.",
    "TutorHub tap trung vao trai nghiem nguoi dung va hieu suat lam viec.",
    "Lavie AI la mot trong nhung diem noi bat nhat cua he thong TutorHub."
]

# ==========================================
# INIT CHROMA VECTOR DB
# ==========================================
print("Initializing Vector Database (ChromaDB)...")
embeddings = HuggingFaceEmbeddings(model_name="all-MiniLM-L6-v2")
vectorstore = Chroma.from_texts(
    texts=tutorhub_knowledge_base,
    embedding=embeddings,
    collection_name="tutorhub_kb",
    persist_directory="./chroma_db"
)
retriever = vectorstore.as_retriever(search_kwargs={"k": 5})
print("ChromaDB initialized OK.")

# ==========================================
# TOOLS
# ==========================================
def get_db_connection():
    if not DB_URL:
        raise RuntimeError("Database URL is not configured. Set TUTORHUB_AI_DB_URL or DATABASE_URL.")
    return psycopg2.connect(DB_URL)

@tool
def search_internal_knowledge(query: str) -> str:
    """Search internal knowledge base about TutorHub system, features, usage guide, founder Nguyen Ba Sang, and Lavie AI. Use this tool FIRST for any question about TutorHub."""
    docs = retriever.invoke(query)
    if docs:
        return "\n".join([doc.page_content for doc in docs])
    return "No internal info found."

@tool
def search_internet(query: str) -> str:
    """Search the internet for general knowledge, news, or information not in the internal knowledge base."""
    try:
        with DDGS() as ddgs:
            results = list(ddgs.text(query, max_results=3))
            if results:
                return "\n".join([f"- {r['title']}: {r['body']}" for r in results])
            return "No results found on the internet."
    except Exception as e:
        return f"Web search error: {str(e)}"

@tool
def get_tutor_profile(tutor_id: int) -> str:
    """Get tutor profile information from the database by tutor user ID number."""
    try:
        conn = get_db_connection()
        cursor = conn.cursor()
        cursor.execute(
            "SELECT id, full_name, email, phone, subject, location, bio FROM users WHERE id = %s",
            (tutor_id,)
        )
        row = cursor.fetchone()
        cursor.close()
        conn.close()
        if row:
            return f"ID: {row[0]}, Name: {row[1]}, Email: {row[2]}, Phone: {row[3]}, Subject: {row[4]}, Location: {row[5]}, Bio: {row[6]}"
        return "Tutor not found."
    except Exception as e:
        return f"DB error: {str(e)}"

@tool
def search_classes(keyword: str) -> str:
    """Search for available tutoring classes in the database by keyword (subject, location, etc)."""
    try:
        conn = get_db_connection()
        cursor = conn.cursor()
        cursor.execute(
            "SELECT id, subject, location, fee, status FROM classes WHERE subject ILIKE %s OR location ILIKE %s LIMIT 5",
            (f"%{keyword}%", f"%{keyword}%")
        )
        rows = cursor.fetchall()
        cursor.close()
        conn.close()
        if rows:
            results = []
            for r in rows:
                results.append(f"Class #{r[0]}: {r[1]} | Location: {r[2]} | Fee: {r[3]} | Status: {r[4]}")
            return "\n".join(results)
        return "No classes found matching your search."
    except Exception as e:
        return f"DB error: {str(e)}"

# ==========================================
# LONG-TERM MEMORY (JSON File)
# ==========================================
USER_MEMORY_FILE = "user_memory.json"
def load_user_memory(user_id: str) -> list:
    if not os.path.exists(USER_MEMORY_FILE):
        return []
    try:
        with open(USER_MEMORY_FILE, "r", encoding="utf-8") as f:
            data = json.load(f)
            user_data = data.get(user_id, [])
            if isinstance(user_data, dict):
                # Return the whole dict as a formatted JSON string inside a list
                return [json.dumps(user_data, ensure_ascii=False, indent=2)]
            elif isinstance(user_data, list):
                return user_data
            else:
                return [str(user_data)]
    except Exception:
        return []

def _append_user_memory(user_id: str, fact: str):
    data = {}
    if os.path.exists(USER_MEMORY_FILE):
        try:
            with open(USER_MEMORY_FILE, "r", encoding="utf-8") as f:
                data = json.load(f)
        except Exception:
            data = {}
            
    if user_id not in data:
        data[user_id] = []
        
    user_data = data[user_id]
    if isinstance(user_data, dict):
        if "memory_summary" in user_data and isinstance(user_data["memory_summary"], list):
            if fact not in user_data["memory_summary"]:
                user_data["memory_summary"].append(fact)
        else:
            if "additional_facts" not in user_data:
                user_data["additional_facts"] = []
            if fact not in user_data["additional_facts"]:
                user_data["additional_facts"].append(fact)
    else:
        if fact not in user_data:
            user_data.append(fact)

    try:
        with open(USER_MEMORY_FILE, "w", encoding="utf-8") as f:
            json.dump(data, f, ensure_ascii=False, indent=2)
    except Exception:
        pass

@tool
def save_user_memory(fact: str, user_id: str = "java_user") -> str:
    """Save an important fact, personal preference, weakness, or information about the user to long-term memory."""
    try:
        _append_user_memory(user_id, fact)
        return "Saved successfully."
    except Exception as e:
        return f"Error saving memory: {str(e)}"

@tool
def read_webpage(url: str) -> str:
    """Fetch and read the main text content of a webpage URL. Use this to dive deeper into links found by internet search."""
    try:
        import requests
        from bs4 import BeautifulSoup
        resp = requests.get(url, timeout=10)
        soup = BeautifulSoup(resp.text, 'html.parser')
        
        # Remove script and style elements
        for script in soup(["script", "style"]):
            script.extract()
            
        text = soup.get_text(separator=' ', strip=True)
        return text[:20000] # Return up to 20,000 characters
    except Exception as e:
        return f"Failed to read webpage: {str(e)}"

@tool
def execute_python_code(code: str) -> str:
    """Execute Python code locally and return the console output (stdout).
    Useful for math calculations, data analysis, or generating files (like text or excel) on the local disk.
    For example: code can be 'print(1+1)' or code to create a file."""
    try:
        import sys
        import io
        import contextlib
        
        output = io.StringIO()
        with contextlib.redirect_stdout(output):
            # We use an empty dict for locals and globals to have a fresh scope,
            # but allow imports inside the code string.
            exec(code, {})
        
        result = output.getvalue()
        if not result.strip():
            return "Code executed successfully but returned no output (stdout is empty)."
        return result
    except Exception as e:
        return f"Code execution error: {str(e)}"

ALL_TOOLS = [search_internal_knowledge, search_internet, get_tutor_profile, search_classes, save_user_memory, read_webpage, execute_python_code]
TOOL_MAP = {t.name: t for t in ALL_TOOLS}

# ==========================================
# INIT LLM WITH TOOL BINDING
# ==========================================
if GROQ_KEY:
    llm = ChatGroq(
        temperature=0.5,
        model_name="llama-3.1-8b-instant",
        groq_api_key=GROQ_KEY
    )
    llm_with_tools = llm.bind_tools(ALL_TOOLS)
    print("LLM (llama3-8b) with tools initialized OK.")
else:
    llm = None
    llm_with_tools = None
    print("WARNING: Groq LLM is disabled because GROQ_KEY is not configured.")

# ==========================================
# CONVERSATION MEMORY (in-memory per session)
# ==========================================
conversation_memory: dict[str, list] = {}
MAX_MEMORY_MESSAGES = 4

def get_memory(session_id: str) -> list:
    if session_id not in conversation_memory:
        conversation_memory[session_id] = []
    return conversation_memory[session_id]

def trim_memory(messages: list) -> list:
    if len(messages) > MAX_MEMORY_MESSAGES:
        return messages[-MAX_MEMORY_MESSAGES:]
    return messages

# ==========================================
# AGENT LOOP (manual, robust)
# ==========================================
MAX_TOOL_ITERATIONS = 5

def run_agent(user_message: str, session_id: str = "default") -> str:
    if llm is None or llm_with_tools is None:
        return "Lavie AI chua duoc cau hinh GROQ_KEY tren server."

    current_time = datetime.datetime.now().strftime("%d/%m/%Y %H:%M")

    ltm_facts = load_user_memory(session_id)
    ltm_str = "Không có" if not ltm_facts else "\n".join([f"- {f}" for f in ltm_facts])

    system_msg = SystemMessage(content=f"""Ban la Lavie, tro ly ao thong minh doc quyen cua nen tang quan ly gia su TutorHub Enterprise.
Thoi gian hien tai: {current_time}

[TRÍ NHỚ DÀI HẠN VỀ NGƯỜI DÙNG NÀY]
{ltm_str}

QUY TAC BAT BUOC:
1. Khi nguoi dung hoi ve TutorHub, tinh nang, cach su dung, tac gia, Lavie — hay dung tool search_internal_knowledge TRUOC.
2. Khi nguoi dung hoi kien thuc chung (toan, ly, hoa, tin tuc...) — hay dung tool search_internet.
3. Khi nguoi dung muon tim gia su hoac lop hoc — hay dung tool get_tutor_profile hoac search_classes.
4. Xung 'Lavie' va goi nguoi dung la 'ban'. Luon giu thai do nhiet tinh, than thien, dung emoji.
5. Tra loi bang tieng Viet, ngan gon, suc tich, trinh bay ro rang.
6. Khi nguoi dung tiet lo so thich, thong tin ca nhan, diem yeu, hoac yeu cau ban ghi nho dieu gi do, HAY GOI TOOL save_user_memory de luu lai.
7. NEU ban da co du thong tin de tra loi, KHONG can goi them tool nua.""")

    memory = get_memory(session_id)
    memory.append(HumanMessage(content=user_message))

    messages = [system_msg] + trim_memory(memory)

    for iteration in range(MAX_TOOL_ITERATIONS):
        response = llm_with_tools.invoke(messages)
        messages.append(response)

        # If no tool calls, we have the final answer
        if not response.tool_calls:
            memory.append(AIMessage(content=response.content))
            conversation_memory[session_id] = trim_memory(memory)
            return response.content

        # Execute tool calls
        for tc in response.tool_calls:
            tool_name = tc["name"]
            tool_args = tc["args"]
            tool_fn = TOOL_MAP.get(tool_name)
            if tool_fn:
                try:
                    result = tool_fn.invoke(tool_args)
                except Exception as e:
                    result = f"Tool error: {str(e)}"
            else:
                result = f"Unknown tool: {tool_name}"

            tool_msg = ToolMessage(content=str(result), tool_call_id=tc["id"])
            messages.append(tool_msg)

    # If we exhausted iterations, get final response without tools
    final_response = llm.invoke(messages)
    memory.append(AIMessage(content=final_response.content))
    conversation_memory[session_id] = trim_memory(memory)
    return final_response.content

async def run_agent_stream(user_message: str, session_id: str = "default", voice: bool = False):
    if llm is None or llm_with_tools is None:
        yield f"data: {json.dumps({'content': 'Lavie AI chua duoc cau hinh GROQ_KEY tren server.'}, ensure_ascii=False)}\n\n"
        return

    current_time = datetime.datetime.now().strftime("%d/%m/%Y %H:%M")

    ltm_facts = load_user_memory(session_id)
    ltm_str = "Không có" if not ltm_facts else "\n".join([f"- {f}" for f in ltm_facts])

    system_msg = SystemMessage(content=f"""Ban la Lavie, tro ly ao thong minh doc quyen cua nen tang quan ly gia su TutorHub Enterprise.
Thoi gian hien tai: {current_time}

[TRÍ NHỚ DÀI HẠN VỀ NGƯỜI DÙNG NÀY]
{ltm_str}

QUY TAC BAT BUOC:
1. Khi nguoi dung hoi ve TutorHub, tinh nang, cach su dung, tac gia, Lavie — hay dung tool search_internal_knowledge TRUOC.
2. Khi nguoi dung hoi kien thuc chung (toan, ly, hoa, tin tuc...) — hay dung tool search_internet.
3. Khi nguoi dung muon tim gia su hoac lop hoc — hay dung tool get_tutor_profile hoac search_classes.
4. Xung 'Lavie' va goi nguoi dung la 'ban'. Luon giu thai do nhiet tinh, than thien, dung emoji.
5. Tra loi bang tieng Viet, ngan gon, suc tich, trinh bay ro rang.
6. Khi nguoi dung tiet lo so thich, thong tin ca nhan, diem yeu, hoac yeu cau ban ghi nho dieu gi do, HAY GOI TOOL save_user_memory de luu lai.
7. NEU ban da co du thong tin de tra loi, KHONG can goi them tool nua.""")

    memory = get_memory(session_id)
    memory.append(HumanMessage(content=user_message))

    messages = [system_msg] + trim_memory(memory)

    for iteration in range(MAX_TOOL_ITERATIONS):
        response = llm_with_tools.invoke(messages)
        messages.append(response)

        # If no tool calls, we stream the final answer
        if not response.tool_calls:
            messages.pop() # Remove non-streaming response
            full_content = ""
            # Stream directly from LLM
            for chunk in llm.stream(messages):
                if chunk.content:
                    full_content += chunk.content
                    yield f"data: {json.dumps({'content': chunk.content}, ensure_ascii=False)}\n\n"
            
            memory.append(AIMessage(content=full_content))
            conversation_memory[session_id] = trim_memory(memory)
            
            if voice:
                tts_filename = f"{uuid.uuid4()}.mp3"
                tts_path = f"static/audio/{tts_filename}"
                await text_to_speech(full_content, tts_path)
                yield f"data: {json.dumps({'audio_url': f'/static/audio/{tts_filename}'}, ensure_ascii=False)}\n\n"
                
            yield "data: [DONE]\n\n"
            return

        # Execute tool calls
        for tc in response.tool_calls:
            tool_name = tc["name"]
            tool_args = tc["args"]
            tool_fn = TOOL_MAP.get(tool_name)
            if tool_fn:
                try:
                    result = tool_fn.invoke(tool_args)
                except Exception as e:
                    result = f"Tool error: {str(e)}"
            else:
                result = f"Unknown tool: {tool_name}"

            tool_msg = ToolMessage(content=str(result), tool_call_id=tc["id"])
            messages.append(tool_msg)

    # Fallback if exhausted
    full_content = ""
    for chunk in llm.stream(messages):
        if chunk.content:
            full_content += chunk.content
            yield f"data: {json.dumps({'content': chunk.content}, ensure_ascii=False)}\n\n"
    memory.append(AIMessage(content=full_content))
    conversation_memory[session_id] = trim_memory(memory)
    
    if voice:
        tts_filename = f"{uuid.uuid4()}.mp3"
        tts_path = f"static/audio/{tts_filename}"
        await text_to_speech(full_content, tts_path)
        yield f"data: {json.dumps({'audio_url': f'/static/audio/{tts_filename}'}, ensure_ascii=False)}\n\n"
        
    yield "data: [DONE]\n\n"

# ==========================================
# MULTIMODAL FUNCTIONS (VOICE & VISION)
# ==========================================
import re

def clean_text_for_speech(text: str) -> str:
    # 1. Loại bỏ các emoji (khu vực Unicode của emoji)
    text = re.sub(r'[\U00010000-\U0010ffff]', '', text)
    text = re.sub(r'[\u2600-\u27BF]', '', text)
    # Loại bỏ thêm các ký tự trang trí thường gặp khác
    text = re.sub(r'[✅✨⭐🔥👍👋😊❤️]', '', text)
    
    # 2. Logic từ điển phát âm
    pronunciations = {
        r'\bInformation Technology\b': 'in phờ mê sần téch no lờ ji',
        r'\bTutorHub Enterprise\b': 'Tiu tờ hắp en tơ prai',
        r'\bAmazon Web Services\b': 'a ma zon web sờ vi xìs',
        r'\bGoogle Calendar\b': 'gu gồ ca len đờ',
        r'\bCloud Computing\b': 'clao com piu ting',
        r'\bMicrosoft Azure\b': 'mai crô sóp a zua',
        r'\bQuestion Bank\b': 'quét chần banh',
        r'\bGoogle Drive\b': 'gu gồ đờ rai',
        r'\bPull Request\b': 'pun ri quest',
        r'\bGoogle Cloud\b': 'gu gồ clao',
        r'\bDevelopment\b': 'đi ve lốp mần',
        r'\bSpring Boot\b': 'sờ pring bút',
        r'\bEnterprise\b': 'en tơ prai',
        r'\bVideo Call\b': 'vi đi âu cô',
        r'\bAssignment\b': 'ờ sai mần',
        r'\bVocabulary\b': 'vô cab bu le ri',
        r'\bDeployment\b': 'đi ploi mần',
        r'\bProduction\b': 'prờ đắc sần',
        r'\bJavaScript\b': 'gia va sờ críp',
        r'\bTypeScript\b': 'tai sờ críp',
        r'\bExpress\.js\b': 'ích press chấm chây ét',
        r'\bPostgreSQL\b': 'pốt gre ét kiu eo',
        r'\bKubernetes\b': 'kiu bờ nét tìs',
        r'\bAssistant\b': 'ờ sít tần',
        r'\bDashboard\b': 'đát bo',
        r'\bResources\b': 'ri xo xít',
        r'\bInstagram\b': 'in ta gram',
        r'\bGrid View\b': 'grít viu',
        r'\bList View\b': 'lít viu',
        r'\bFlashcard\b': 'phờ lát card',
        r'\bListening\b': 'lít sờ ning',
        r'\bFullstack\b': 'phun stắc',
        r'\bWebSocket\b': 'queb sóc kịt',
        r'\bFramework\b': 'phờ rêm guốc',
        r'\bExtension\b': 'ích sten sần',
        r'\bBitbucket\b': 'bít bắc kịt',
        r'\bMicrosoft\b': 'mai crô sóp',
        r'\bTutorHub\b': 'Tiu tờ hắp',
        r'\bCalendar\b': 'ke len đờ',
        r'\bFacebook\b': 'phây búc',
        r'\bDownload\b': 'đao lốt',
        r'\bCheckbox\b': 'chéc bóc',
        r'\bDropdown\b': 'đrốp đao',
        r'\bSkeleton\b': 'ske lờ tần',
        r'\bSchedule\b': 'ske ju',
        r'\bDeadline\b': 'đét lai',
        r'\bWorkbook\b': 'quớc búc',
        r'\bSpeaking\b': 'sờ pi king',
        r'\bSoftware\b': 'sóp que',
        r'\bHardware\b': 'hát que',
        r'\bDatabase\b': 'đa ta bây',
        r'\bFrontend\b': 'fron en',
        r'\bREST API\b': 'rét ây pi ai',
        r'\bFirebase\b': 'phai ơ bê',
        r'\bSupabase\b': 'su pa bê',
        r'\bSign Up\b': 'sai ắp',
        r'\bLibrary\b': 'lai brờ ri',
        r'\bClassIn\b': 'cờ lát in',
        r'\bPreview\b': 'pri viu',
        r'\bTooltip\b': 'tun típ',
        r'\bSidebar\b': 'sai ba',
        r'\bLoading\b': 'lâu đing',
        r'\bSuccess\b': 'sắc xét',
        r'\bPremium\b': 'pri mi ơm',
        r'\bStudent\b': 'sờ tiu đần',
        r'\bMeeting\b': 'mít ting',
        r'\bGrammar\b': 'gram mờ',
        r'\bReading\b': 'ri đing',
        r'\bWriting\b': 'rai ting',
        r'\bBackend\b': 'bách en',
        r'\bGraphQL\b': 'gráp kiu eo',
        r'\bStaging\b': 'stây jing',
        r'\bFeature\b': 'phi chờ',
        r'\bLibrary\b': 'lai brờ ri',
        r'\bPackage\b': 'pắc kịch',
        r'\bReactJS\b': 'ri ác chây ét',
        r'\bNext\.js\b': 'néc chấm chây ét',
        r'\bAngular\b': 'ăng gu lờ',
        r'\bNode\.js\b': 'nốt chấm chây ét',
        r'\bLaravel\b': 'la ra ven',
        r'\bFlutter\b': 'phờ lắt tờ',
        r'\bMongoDB\b': 'mon gô đi bi',
        r'\bNetlify\b': 'nét li phai',
        r'\bDiscord\b': 'đít co',
        r'\bTikTok\b': 'tích tóc',
        r'\bUpload\b': 'ắp lốt',
        r'\bFilter\b': 'phiu tơ',
        r'\bHeader\b': 'he đờ',
        r'\bFooter\b': 'phu tờ',
        r'\bButton\b': 'bất tần',
        r'\bSearch\b': 'sớt',
        r'\bParent\b': 'pe rần',
        r'\bMentor\b': 'men tờ',
        r'\bLesson\b': 'lét sần',
        r'\bCourse\b': 'co sờ',
        r'\bPart 1\b': 'phần một',
        r'\bPart 2\b': 'phần hai',
        r'\bPart 3\b': 'phần ba',
        r'\bPart 4\b': 'phần bốn',
        r'\bPart 5\b': 'phần năm',
        r'\bPart 6\b': 'phần sáu',
        r'\bPart 7\b': 'phần bảy',
        r'\bServer\b': 'se vờ',
        r'\bClient\b': 'clai ần',
        r'\bSocket\b': 'sóc kịt',
        r'\bPlugin\b': 'plắc gin',
        r'\bPython\b': 'pai thần',
        r'\bGolang\b': 'gâu lang',
        r'\bKotlin\b': 'cót lin',
        r'\bDjango\b': 'đan gô',
        r'\bSQLite\b': 'ét kiu lai',
        r'\bGitHub\b': 'gít hắp',
        r'\bGitLab\b': 'gít láp',
        r'\bCommit\b': 'cờ mít',
        r'\bBranch\b': 'bran',
        r'\bDocker\b': 'đóc cờ',
        r'\bDevOps\b': 'đép ốp',
        r'\bApache\b': 'a pa chi',
        r'\bVercel\b': 'vơ seo',
        r'\bHeroku\b': 'he rô ku',
        r'\bGoogle\b': 'gu gồ',
        r'\bAmazon\b': 'a ma zon',
        r'\bOpenAI\b': 'âu pần ây ai',
        r'\bNVIDIA\b': 'en vi đi a',
        r'\bOracle\b': 'o ra cồ',
        r'\bNotion\b': 'nâu sần',
        r'\bLavie\b': 'La vi e',
        r'\bLogin\b': 'lóc in',
        r'\bDrive\b': 'đrai',
        r'\bClass\b': 'cờ lát',
        r'\bReels\b': 'riu',
        r'\bIELTS\b': 'ai eo',
        r'\bTOEIC\b': 'tô ích',
        r'\bTOEFL\b': 'tô phồ',
        r'\bShare\b': 'se',
        r'\bModal\b': 'mo đồ',
        r'\bPopup\b': 'póp ặp',
        r'\bToast\b': 'tốt',
        r'\bInput\b': 'in pút',
        r'\bError\b': 'e rờ',
        r'\bAdmin\b': 'át min',
        r'\bTutor\b': 'tiu tờ',
        r'\bCloud\b': 'clao',
        r'\bDebug\b': 'đi bắc',
        r'\bSwift\b': 'suýp',
        r'\bReact\b': 'ri ác',
        r'\bVueJS\b': 'viu chây ét',
        r'\bFlask\b': 'phờ lát',
        r'\bMySQL\b': 'mai ét kiu eo',
        r'\bRedis\b': 're đít',
        r'\bMerge\b': 'mớt',
        r'\bCI/CD\b': 'xi ai xi đi',
        r'\bNginx\b': 'en gin ích',
        r'\bAzure\b': 'a zua',
        r'\bApple\b': 'ép pồ',
        r'\bIntel\b': 'in teo',
        r'\bAdobe\b': 'a đô bi',
        r'\bFigma\b': 'phích ma',
        r'\bCanva\b': 'can va',
        r'\bSlack\b': 'sờ lắc',
        r'\bTeams\b': 'tim',
        r'\bChat\b': 'chát',
        r'\bPPTX\b': 'pi pi ti ích',
        r'\bDOCX\b': 'đóc ích',
        r'\bXLSX\b': 'ích eo ét ích',
        r'\bSort\b': 'soạt',
        r'\bQuiz\b': 'quýt',
        r'\bTest\b': 'tét',
        r'\bJava\b': 'gia va',
        r'\bRuby\b': 'ru bi',
        r'\bDart\b': 'đạt',
        r'\bRust\b': 'rớt',
        r'\bHTML\b': 'hát tê mờ lờ',
        r'\bJSON\b': 'chây sần',
        r'\bMeta\b': 'mê ta',
        r'\bZoom\b': 'zum',
        r'\bSAT\b': 'ét ây ti',
        r'\bPDF\b': 'pê đê ép',
        r'\bPPT\b': 'pi pi ti',
        r'\bDOC\b': 'đóc',
        r'\bXLS\b': 'ích eo ét',
        r'\bMP4\b': 'mờ pê bốn',
        r'\bTab\b': 'táp',
        r'\bVIP\b': 'vi ai pi',
        r'\bAPI\b': 'ây pi ai',
        r'\bBug\b': 'bắc',
        r'\bPHP\b': 'pê hát pê',
        r'\bC\+\+': 'xi cộng cộng',
        r'\bSQL\b': 'ét kiu eo',
        r'\bCSS\b': 'xi ét ét',
        r'\bXML\b': 'ích em eo',
        r'\bVue\b': 'viu',
        r'\bGit\b': 'gít',
        r'\bAWS\b': 'ây đắp liu ét',
        r'\bAMD\b': 'ây em đi',
        r'\bIBM\b': 'ai bi em',
        r'\bAI\b': 'ây ai',
        r'\bIT\b': 'ai ti',
        r'\bC#\b': 'xi sáp',
        r'\bGo\b': 'gâu',
        r'\bC\b': 'xi'
        }
    
    for pattern, replacement in pronunciations.items():
        text = re.sub(pattern, replacement, text, flags=re.IGNORECASE)
        
    # 3. Loai bo the <function=...> va code block ```...```
    text = re.sub(r'<function=.*?>.*?</function>', '', text, flags=re.DOTALL)
    text = re.sub(r'```.*?```', '', text, flags=re.DOTALL)
    text = re.sub(r'<[^>]+>', '', text)
    
    # 4. Chuẩn hóa khoảng trắng
    text = re.sub(r'\s+', ' ', text).strip()
    return text

async def text_to_speech(text: str, output_path: str):
    # Giong nu tieng Viet: vi-VN-HoaiMyNeural
    voice = "vi-VN-HoaiMyNeural" 
    cleaned_text = clean_text_for_speech(text)
    if not cleaned_text.strip():
        cleaned_text = "..."
    communicate = edge_tts.Communicate(cleaned_text, voice)
    await communicate.save(output_path)

def analyze_image_with_vision(image_path: str, prompt: str) -> str:
    try:
        if gemini_model is None:
            return "Gemini vision chua duoc cau hinh GEMINI_API_KEY tren server."

        with open(image_path, "rb") as f:
            image_bytes = f.read()
            
        with PIL.Image.open(io.BytesIO(image_bytes)) as img:
            # Using Gemini 1.5 Flash for vision tasks
            response = gemini_model.generate_content([prompt, img])
            return response.text
    except Exception as e:
        err_str = str(e).lower()
        return f"Xin lỗi bạn, có lỗi xảy ra khi phân tích ảnh qua Gemini: {err_str}"

def transcribe_audio(audio_path: str) -> str:
    if groq_client is None:
        raise RuntimeError("Groq speech-to-text is not configured. Set GROQ_KEY.")

    with open(audio_path, "rb") as file:
        transcription = groq_client.audio.transcriptions.create(
            file=(audio_path, file.read()),
            model="whisper-large-v3",
            prompt="Transcribe the Vietnamese audio.",
            response_format="json",
            language="vi",
            temperature=0.0
        )
    return transcription.text

# ==========================================
# API ROUTES
# ==========================================
class ChatRequest(BaseModel):
    message: str
    user_id: Optional[str] = "default"
    voice: Optional[bool] = False

@app.post("/api/chat")
async def chat_endpoint(req: ChatRequest):
    try:
        answer = run_agent(req.message, session_id=req.user_id or "default")
        
        response_data = {"answer": answer}
        
        if req.voice:
            tts_filename = f"{uuid.uuid4()}.mp3"
            tts_path = f"static/audio/{tts_filename}"
            await text_to_speech(answer, tts_path)
            response_data["audio_url"] = f"/static/audio/{tts_filename}"
            
        return response_data
    except Exception as e:
        return JSONResponse(
            status_code=500,
            content={"answer": f"Xin loi, Lavie dang gap su co: {str(e)}"}
        )

from pydantic import BaseModel

class TTSRequest(BaseModel):
    text: str

@app.post("/api/tts")
async def generate_tts(req: TTSRequest):
    try:
        tts_filename = f"{uuid.uuid4()}.mp3"
        tts_path = f"static/audio/{tts_filename}"
        await text_to_speech(req.text, tts_path)
        return {"audio_url": f"/static/audio/{tts_filename}"}
    except Exception as e:
        return {"error": str(e)}

@app.post("/api/chat/stream")
async def chat_stream_endpoint(req: ChatRequest):
    return StreamingResponse(
        run_agent_stream(req.message, session_id=req.user_id or "default", voice=req.voice),
        media_type="text/event-stream"
    )

@app.post("/api/chat/voice")
async def chat_voice_endpoint(audio: UploadFile = File(...), user_id: str = Form("default")):
    try:
        # 1. Luu file audio tam thoi
        audio_filename = f"{uuid.uuid4()}_{audio.filename}"
        audio_path = f"temp/{audio_filename}"
        with open(audio_path, "wb") as f:
            f.write(await audio.read())
        
        # 2. Chuyen giong noi thanh van ban (STT) bang Whisper
        try:
            user_text = transcribe_audio(audio_path)
        except Exception as e:
            os.remove(audio_path)
            err_str = str(e).lower()
            if "400" in err_str or "process file" in err_str:
                return {"answer": "Đoạn ghi âm quá ngắn hoặc không hợp lệ. Bạn hãy **nhấn giữ** lâu hơn để nói nhé!", "audio_url": ""}
            raise e

        os.remove(audio_path)
        
        if not user_text.strip():
            return {"answer": "Lavie không nghe rõ bạn nói gì, bạn nói lại nhé!", "audio_url": ""}
        
        # 3. Cho Agent suy nghi va tra loi
        answer = run_agent(user_text, session_id=user_id)
        
        # 4. Chuyen van ban thanh giong noi (TTS)
        tts_filename = f"{uuid.uuid4()}.mp3"
        tts_path = f"static/audio/{tts_filename}"
        await text_to_speech(answer, tts_path)
        
        return {
            "user_text": user_text,
            "answer": answer,
            "audio_url": f"/static/audio/{tts_filename}"
        }
    except Exception as e:
        return JSONResponse(status_code=500, content={"answer": f"Lỗi Voice: {str(e)}"})

@app.post("/api/chat/vision")
async def chat_vision_endpoint(image: UploadFile = File(...), message: str = Form(""), user_id: str = Form("default")):
    try:
        # 1. Luu anh tam thoi
        image_filename = f"{uuid.uuid4()}_{image.filename}"
        image_path = f"temp/{image_filename}"
        with open(image_path, "wb") as f:
            f.write(await image.read())
        
        # 2. Goi model Vision de doc anh
        vision_prompt = message if message else "Mô tả chi tiết hình ảnh này."
        vision_result = analyze_image_with_vision(image_path, vision_prompt)
        os.remove(image_path)
        
        # 3. Dua thong tin anh va cau hoi vao Agent
        agent_input = f"[Hình ảnh đính kèm] Người dùng hỏi: {vision_prompt}\nKết quả phân tích ảnh: {vision_result}\nLavie hãy trả lời người dùng dựa trên thông tin này."
        answer = run_agent(agent_input, session_id=user_id)
        
        # 4. Sinh TTS
        tts_filename = f"{uuid.uuid4()}.mp3"
        tts_path = f"static/audio/{tts_filename}"
        await text_to_speech(answer, tts_path)
        
        return {"answer": answer, "vision_analysis": vision_result, "audio_url": f"/static/audio/{tts_filename}"}
    except Exception as e:
        return JSONResponse(status_code=500, content={"answer": f"Lỗi Vision: {str(e)}"})


@app.post("/api/chat/document")
async def chat_document_endpoint(image: UploadFile = File(...), message: str = Form(""), user_id: str = Form("default")):
    session_id = user_id
    filename = image.filename
    ext = os.path.splitext(filename)[1].lower()
    
    file_path = os.path.join("temp", f"{uuid.uuid4()}{ext}")
    with open(file_path, "wb") as f:
        f.write(await image.read())

    async def stream_generator():
        # Yield initial status
        init_json = json.dumps({"chunk": f"Đang đọc tài liệu {filename}...\n", "audio_url": None}, ensure_ascii=False)
        yield f"data: {init_json}\n\n"
        
        try:
            import fitz
            import docx
            from langchain_text_splitters import RecursiveCharacterTextSplitter
            
            text = ""
            if ext == ".pdf":
                doc = fitz.open(file_path)
                for page in doc:
                    text += page.get_text() + "\n"
                doc.close()
            elif ext == ".docx":
                doc = docx.Document(file_path)
                for para in doc.paragraphs:
                    text += para.text + "\n"
            elif ext == ".txt":
                with open(file_path, "r", encoding="utf-8") as f:
                    text = f.read()
            
            if not text.strip():
                err_json = json.dumps({"chunk": "Không thể trích xuất chữ từ tài liệu này.", "audio_url": None}, ensure_ascii=False)
                yield f"data: {err_json}\n\n"
                yield "data: [DONE]\n\n"
                return
                
            splitter = RecursiveCharacterTextSplitter(chunk_size=1000, chunk_overlap=200)
            chunks = splitter.split_text(text)
            chunks = [f"[Tài liệu: {filename}] " + c for c in chunks]
            
            vectorstore.add_texts(chunks)
            
            success_json = json.dumps({"chunk": f"✅ Đã nạp thành công {len(chunks)} đoạn kiến thức từ tài liệu vào Trí nhớ dài hạn!\n", "audio_url": None}, ensure_ascii=False)
            yield f"data: {success_json}\n\n"
            
            if message.strip():
                doc_preview = text[:40000] # Pass up to 40k chars directly (~10k tokens)
                enriched_message = f"[Hệ thống: Người dùng vừa tải lên tài liệu '{filename}'].\n\nNỘI DUNG TÀI LIỆU (Trích xuất):\n{doc_preview}\n\n[Hệ thống] Dựa vào nội dung trên, hãy trả lời yêu cầu sau của người dùng: {message}"
                async for chunk in run_agent_stream(enriched_message, session_id=session_id, voice=True):
                    yield chunk
            else:
                final_text = "Em đã nạp xong tài liệu vào bộ nhớ rồi ạ!"
                audio_file = f"{uuid.uuid4()}.mp3"
                audio_path = os.path.join("static", "audio", audio_file)
                await text_to_speech(final_text, audio_path)
                audio_url = f"http://127.0.0.1:5000/static/audio/{audio_file}"
                final_json = json.dumps({"chunk": "", "audio_url": audio_url}, ensure_ascii=False)
                yield f"data: {final_json}\n\n"
                
        except Exception as e:
            err_json = json.dumps({"chunk": f"Lỗi đọc tài liệu: {str(e)}", "audio_url": None}, ensure_ascii=False)
            yield f"data: {err_json}\n\n"
        finally:
            yield "data: [DONE]\n\n"
            if os.path.exists(file_path):
                os.remove(file_path)

    return StreamingResponse(stream_generator(), media_type="text/event-stream")


@app.get("/health")
async def health():
    return {"status": "ok", "model": "llama-3.3-70b-versatile", "vector_db": "ChromaDB", "tools": list(TOOL_MAP.keys())}

if __name__ == "__main__":
    port = int(os.environ.get("PORT", 7860))
    uvicorn.run("app:app", host="0.0.0.0", port=port, reload=False)
