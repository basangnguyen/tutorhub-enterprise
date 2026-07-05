# Claude-like AI Chat Final Audit and Polish Plan

Ngay 2026-07-05, da kiem tra lai giao dien Claude qua anh chup cua nguoi dung va cac tai lieu chinh thuc:

- Claude chat URL: `https://claude.ai/chat` yeu cau session dang nhap nen khong doc duoc DOM noi bo trong moi truong public.
- Claude Artifacts Help Center: artifacts la noi dung lon, tu dung, hien trong cua so rieng ben phai main chat.
- Anthropic visible extended thinking: thinking la che do tang effort/thoi gian xu ly; voi TutorHub chi hien status cap cao, khong hien chain-of-thought tho.

## 1. Tong quan hien trang

Lavie AI Chat hien nam trong:

- `src/main/resources/ai/ai_chat.html`: UI HTML/CSS/JS trong JCEF.
- `src/main/java/com/mycompany/tutorhub_enterprise/client/ai/AiChatPanel.java`: bridge JCEF/Java qua `cefQuery`.
- `src/main/java/com/mycompany/tutorhub_enterprise/client/ChatTab.java`: shell Tin nhan Swing, conversation list, mo rong/thu gon Lavie.

Provider/backend giu nguyen:

- Lavie / Hugging Face.
- LangChain4j Ollama.
- OpenAI-compatible.

## 2. Diem da giong Claude

- Chat surface da toi gian hon, assistant message hien nhu van ban thay vi card lon.
- Composer da noi o duoi, bo goc lon, co send/stop.
- Co activity panel cap cao, co timer, co collapse/expand.
- Co Markdown renderer noi bo.
- Co code block copy.
- Co artifact/canvas panel rieng.
- Khong dung CDN runtime.

## 3. Diem chua giong Claude

Can polish tiep:

- Composer can giong Claude hon: panel noi o giua duoi man hinh, rong vua phai, shadow mem, toolbar nam trong mot mat phang.
- Activity panel dang hoi "dashboard/card"; Claude nhe hon, summary compact hon, chip trang thai mem hon.
- Code block nen nen sang trong chat chinh, header language + nut copy phia tren phai nhu anh chup.
- Artifact panel can giong side canvas hon: chi day body chat khi mo, co header slim, meta ro, khong che composer.
- Message actions can hien it hon, dung hover/focus de tranh lam roi van ban.
- Markdown typography can tang cam giac editorial: body width hop ly, h1/h2 dung serif-like/macOS feel, spacing rong hon.

## 4. Ke hoach polish

Chi sua `src/main/resources/ai/ai_chat.html`.

Nhom 1 - Chat surface va composer:

- Chat column width gan Claude: khoang 900px.
- Nen am hon, bot card/border nang.
- Composer fixed/floating look mem hon, focus ring nhe.
- Nut stop/generate ro hon.

Nhom 2 - Activity panel:

- Doi visual thanh panel compact, border nhe, summary button giong "Da suy nghi trong X giay".
- Step chip xanh nhe giong anh, khong mo to mac dinh sau khi done.
- Them shimmer/typing line rat nhe khi chua co delta.

Nhom 3 - Markdown/code:

- Code block sang hon, header language ben trai, copy ben phai.
- Giu scroll ngang, copy state.
- Table/quote/list can dep hon nhung khong them lib.

Nhom 4 - Artifact panel:

- Khi artifact open, chat/composer co margin-right de khong bi che.
- Panel ben phai co width on dinh, top/bottom hop ly.
- Header co title/meta/action slim.
- Auto-open chi khi noi dung thuc su lon/code lon/table, tranh phien.

## 5. Rủi ro

- Loi JS trong JCEF co the lam panel trang trang.
- Auto-open artifact neu qua nhay se gay kho chiu.
- CSS fixed composer/artifact co the anh huong viewport nho.

## 6. Test

- Kiem tra JS syntax bang Node.
- Kiem tra khong co CDN/runtime ngoai.
- Build Maven:
  `& "C:\Program Files\Apache NetBeans\java\maven\bin\mvn.cmd" clean compile assembly:single -DskipTests`
- Copy jar sang `HF_UPLOAD\update.jar`.
