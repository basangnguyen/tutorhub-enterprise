# Claude-like AI Chat UI - Research and Plan

Ngay hien tai, Chat AI/Lavie trong tab Tin nhan la mot vung JCEF HTML duoc nhung vao Swing. Muc tieu cua tai lieu nay la khoa lai hien trang, pattern hoc tu Claude, pham vi Phase 1 va cach rollback de nang cap UI ma khong lam gay provider/backend hien co.

## 1. Hien trang file/class

Thanh phan lien quan truc tiep:

- `src/main/java/com/mycompany/tutorhub_enterprise/client/ChatTab.java`
  - Quan ly tab Tin nhan bang Swing.
  - Tao conversation dac biet `Lavie AI Agent` voi `LAVIE_AI_CONVERSATION_ID = -999`.
  - Nhung `AiChatPanel` vao khung chat khi chon Lavie.
  - Dang co co che mo rong/thu gon cot danh sach hoi thoai qua `setLavieExpanded(...)`.

- `src/main/java/com/mycompany/tutorhub_enterprise/client/ai/AiChatPanel.java`
  - JPanel boc JCEF browser.
  - Load `/ai/ai_chat.html`.
  - Dang ky `CefMessageRouter` voi `cefQuery`.
  - Nhan request tu HTML qua bridge JSON channel `tutorhub.ai`.
  - Goi provider, stream delta ve HTML bang `appendAssistantDelta(...)`, ket thuc bang `finishAssistantMessage(...)`.

- `src/main/resources/ai/ai_chat.html`
  - UI chat Lavie bang HTML/CSS/JS chay trong JCEF.
  - Quan ly topbar, settings provider, memory, agent mode, thread message, composer, markdown fallback, code block copy.

- Provider/service:
  - `AiAgentService`, `AiAgentStreamHandle`, `AiAgentStreamCallback`.
  - `LavieAiService`: goi Hugging Face endpoint `https://hocba299-3-tutorhub-ai.hf.space/api/chat/stream`.
  - `LangChain4jAiAgentService`: provider Ollama thong qua LangChain4j.
  - `OpenAiCompatibleAiAgentService`: provider OpenAI-compatible SSE/non-SSE.
  - `AiAgentServiceFactory`, `AiAgentProviderConfig`, `AiAgentSettingsStore`: chon va luu cau hinh provider.

## 2. Swing hay HTML/JCEF/WebView

Kien truc hien tai la hybrid:

- Shell Tin nhan, sidebar hoi thoai, bo cuc chinh: Swing.
- Chat AI Lavie: HTML/CSS/JS trong JCEF.
- Bridge giua HTML va Java: `cefQuery` voi JSON `{ channel, type, payload }`.

Vi vay cac nang cap UI Claude-like nen lam trong `ai_chat.html`, khong doi cong nghe, khong migrate sang framework khac.

## 3. Luong gui/nhan tin nhan hien tai

Luong chinh:

1. Nguoi dung nhap tin vao composer trong `ai_chat.html`.
2. JS goi `postBridge('SEND_MESSAGE', { text, agentMode })`.
3. `AiChatPanel.handleBridgeRequest(...)` nhan type `SEND_MESSAGE`.
4. Neu `agentMode` bat thi goi `startReadOnlyAgent(text)`, neu khong thi goi `startAiStream(text)`.
5. Java tao `AiAgentRequest`, chen conversation memory va long-term memory.
6. Provider stream delta qua `AiAgentStreamCallback.onDelta(...)`.
7. Java goi JS `appendAssistantDelta(delta)`.
8. Khi xong, Java goi `finishAssistantMessage()`, cap nhat memory/context va status.

Nhan xet: luong provider da tach khoi UI kha tot. Phase 1 khong can sua provider.

## 4. Streaming that

Co streaming that:

- `LavieAiService` doc response line-by-line tu endpoint Hugging Face, ho tro `data:` SSE va `[DONE]`.
- `OpenAiCompatibleAiAgentService` cung xu ly streaming/non-streaming tuy endpoint.
- `LangChain4jAiAgentService` dung `OllamaStreamingChatModel`.

HTML hien render delta dan vao bubble hien tai bang `appendAssistantDelta(...)`.

## 5. Stop/cancel

Co stop/cancel o tang Java:

- HTML goi `postBridge('STOP_STREAM')`.
- `AiChatPanel.stopStream()` goi:
  - `activeStreamHandle.cancel()`
  - `activeAgentFuture.cancel(true)`
  - `activeMockTimer.stop()`

Gioi han: cancel tuy thuoc provider co doc co `cancelled` va dong ket noi kip hay khong. Phase 1 chi can noi nut dung tren UI vao bridge san co.

## 6. Markdown/code block renderer hien tai

Hien co fallback renderer trong `ai_chat.html`:

- `renderMarkdown(text)` tach fenced code block bang regex.
- Ho tro:
  - Paragraph
  - Xuong dong
  - Inline code
  - Bold
  - Fenced code block
  - Nut copy code trong tung code block

Chua day du:

- Heading, list, quote, table chua render dung kieu Markdown day du.
- Chua co syntax highlight.
- Chua co artifact/canvas panel hoan chinh.

Phase 1 giu fallback renderer, chi bo sung message actions va preview panel nhe neu an toan. Phase 3 moi nen thay renderer bang `markdown-it`/`marked` vendored local neu can.

## 7. Pattern UI hoc tu Claude

Nguon tham khao chinh:

- Claude Artifacts help: https://support.claude.com/en/articles/9487310-what-are-artifacts-and-how-do-i-use-them
- Anthropic visible extended thinking: https://www.anthropic.com/news/visible-extended-thinking

Pattern co the ap dung:

- Chat surface toi gian, nhieu khoang trang, de cau tra loi la trung tam.
- Empty state co headline ro, copy ngan, prompt goi y de bat dau nhanh.
- Composer co bo goc lon, nut gui ro, trang thai khi dang sinh cau tra loi.
- Message action khong lam roi UI: hien nhe duoi assistant message, gom Copy va Open in panel.
- Code block co header ngon ngu va nut copy rieng.
- Artifact/canvas la panel rieng cho noi dung dai/code, tach khoi luong chat chinh.
- Thinking/extended thinking nen la trang thai cap cao co timer/collapse, khong hien chain-of-thought tho.

Khong ap dung:

- Khong copy logo, mau nhan dien, asset hoac source cua Claude.
- Khong hien raw chain-of-thought.
- Khong gan CDN runtime.

## 8. Chuc nang se tich hop

Phase 1 trong luot nay:

- Empty state dep hon voi du 6 suggested chips:
  - Tom tat cuoc tro chuyen
  - Lap ke hoach on tap
  - Giai thich khai niem
  - Tao quiz nhanh
  - Viet lai tin nhan
  - Phan tich tai lieu
- Input bar polish:
  - Giu textarea auto-resize.
  - Khi generating, nut gui doi thanh nut dung va goi `STOP_STREAM`.
  - Khong doi logic provider.
- Message bubble polish:
  - Giữ layout assistant thoang, user bubble gon.
  - Tang tinh tuong tac qua hover/focus.
- Message actions:
  - Copy message AI.
  - Open in panel cho noi dung dai/code.

Phase 2:

- Thinking/activity panel cap cao.
- Timer.
- Step pending/running/done.
- Collapse/expand.
- Chi hien trang thai xu ly, khong chain-of-thought.

Phase 3:

- Markdown renderer tot hon.
- Code block dep hon.
- Syntax highlighting neu vendor local duoc.

Phase 4:

- Artifact/canvas panel dung nghia.
- Preview code/tai lieu dai.
- Copy toan bo, dong/more actions.

## 9. Rủi ro va rollback

Rui ro:

- JS loi cu phap lam JCEF trang trang.
- Message actions neu gan vao stream khong dung co the copy thieu noi dung.
- Nut stop chi cancel tang UI/Java; provider remote co the van giu ket noi trong vai giay.
- Panel noi dung dai neu lam qua nang se anh huong layout chat.

Cach giam rui ro:

- Chi sua `ai_chat.html` trong Phase 1.
- Khong sua provider/backend.
- Kiem tra cu phap JS bang Node truoc Maven.
- Build Maven day du theo lenh yeu cau.

Rollback:

- Revert rieng `src/main/resources/ai/ai_chat.html` ve ban truoc Phase 1.
- `ChatTab.java`, `AiChatPanel.java`, provider khong can rollback neu Phase 1 chi chinh HTML.

## 10. Ke hoach test

1. Kiem tra cu phap JS trong `ai_chat.html`.
2. Build:
   `& "C:\Program Files\Apache NetBeans\java\maven\bin\mvn.cmd" clean compile assembly:single -DskipTests`
3. Copy jar:
   `copy ".\target\TutorHub_Maven-1.0-SNAPSHOT-jar-with-dependencies.jar" ".\HF_UPLOAD\update.jar" -Force`
4. Manual UI check trong app:
   - Mo tab Tin nhan, chon Lavie AI Agent.
   - Thay 6 suggested chips.
   - Gui mot tin ngan.
   - Khi dang sinh cau tra loi, nut gui doi thanh nut dung.
   - Copy message AI.
   - Open in panel voi cau tra loi dai hoac code block.
   - Nut Cau hinh va status provider van hoat dong.

## 11. Cap nhat trien khai Phase 2-4

Da trien khai tiep trong `src/main/resources/ai/ai_chat.html`:

- Phase 2:
  - Them activity panel trong tung assistant message.
  - Co timer, 5 buoc cap cao: Hieu yeu cau, Tim ngu canh, Lap ke hoach, Soan phan hoi, Kiem tra cau tra loi.
  - Co collapse/expand bang header cua activity panel.
  - Khi hoan tat, panel tu thu gon thanh summary "Da xu ly trong X giay".
  - Khi nguoi dung dung phan hoi, panel chuyen sang trang thai da dung.
  - Khong hien chain-of-thought tho, chi hien trang thai xu ly cap cao.

- Phase 3:
  - Nang fallback Markdown renderer noi bo, khong CDN.
  - Ho tro heading h1-h3, paragraph, line break, unordered list, ordered list, blockquote, link HTTPS, inline code, bold, italic.
  - Ho tro Markdown table voi wrapper scroll ngang.
  - Code block co language label, copy button, font monospace, scroll ngang va highlight nhe bang JS noi bo cho json/html/css/js/java-like text.

- Phase 4:
  - Nang Artifact/Canvas panel hien co thanh panel rieng co title, meta, body va action Copy/Dong.
  - Tu phan loai noi dung thanh code/table/document/text.
  - Auto-open panel khi phan hoi dai, co code block, table hoac list dai.
  - Message action "Mo panel" van cho phep mo thu cong tu moi assistant message du dieu kien.

Pham vi van duoc giu:

- Khong sua backend/provider.
- Khong dung CDN runtime.
- Khong hardcode API key.
- Khong anh huong ChatTab thuong, settings provider, Lavie/Hugging Face, Ollama hay OpenAI-compatible.
