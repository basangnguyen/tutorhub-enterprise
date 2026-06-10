import os

NEW_ENDPOINT = """
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
        init_json = json.dumps({"chunk": f"Đang đọc tài liệu {filename}...\\n", "audio_url": None})
        yield f"data: {init_json}\\n\\n"
        
        try:
            import fitz
            import docx
            from langchain.text_splitter import RecursiveCharacterTextSplitter
            
            text = ""
            if ext == ".pdf":
                doc = fitz.open(file_path)
                for page in doc:
                    text += page.get_text() + "\\n"
                doc.close()
            elif ext == ".docx":
                doc = docx.Document(file_path)
                for para in doc.paragraphs:
                    text += para.text + "\\n"
            elif ext == ".txt":
                with open(file_path, "r", encoding="utf-8") as f:
                    text = f.read()
            
            if not text.strip():
                err_json = json.dumps({"chunk": "Không thể trích xuất chữ từ tài liệu này.", "audio_url": None})
                yield f"data: {err_json}\\n\\n"
                yield "data: [DONE]\\n\\n"
                return
                
            splitter = RecursiveCharacterTextSplitter(chunk_size=1000, chunk_overlap=200)
            chunks = splitter.split_text(text)
            chunks = [f"[Tài liệu: {filename}] " + c for c in chunks]
            
            vectorstore.add_texts(chunks)
            vectorstore.persist()
            
            success_json = json.dumps({"chunk": f"✅ Đã nạp thành công {len(chunks)} đoạn kiến thức từ tài liệu vào Trí nhớ dài hạn!\\n", "audio_url": None})
            yield f"data: {success_json}\\n\\n"
            
            if message.strip():
                async for chunk in run_agent_stream(message, session_id=session_id, voice=True):
                    yield chunk
            else:
                final_text = "Em đã nạp xong tài liệu vào bộ nhớ rồi ạ!"
                audio_file = f"{uuid.uuid4()}.mp3"
                audio_path = os.path.join("static", "audio", audio_file)
                await text_to_speech(final_text, audio_path)
                audio_url = f"http://127.0.0.1:5000/static/audio/{audio_file}"
                final_json = json.dumps({"chunk": "", "audio_url": audio_url})
                yield f"data: {final_json}\\n\\n"
                
        except Exception as e:
            err_json = json.dumps({"chunk": f"Lỗi đọc tài liệu: {str(e)}", "audio_url": None})
            yield f"data: {err_json}\\n\\n"
        finally:
            yield "data: [DONE]\\n\\n"
            if os.path.exists(file_path):
                os.remove(file_path)

    return StreamingResponse(stream_generator(), media_type="text/event-stream")

"""

with open('app.py', 'r', encoding='utf-8') as f:
    content = f.read()

# Insert before @app.get("/health")
if "@app.get(\"/health\")" in content:
    new_content = content.replace('@app.get("/health")', NEW_ENDPOINT + '\n@app.get("/health")')
    with open('app.py', 'w', encoding='utf-8') as f:
        f.write(new_content)
    print("Injected successfully!")
else:
    print("Could not find health endpoint.")
