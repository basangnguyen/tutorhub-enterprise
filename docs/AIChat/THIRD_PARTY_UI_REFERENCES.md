# Third-party UI references for Lavie AI Chat

Tai lieu nay ghi lai cac nguon ma nguon mo da dung de tham khao khi polish Lavie AI Chat.

## Nguon tham khao

| Ten | URL | Commit cuc bo | License | Cach su dung trong TutorHub |
| --- | --- | --- | --- | --- |
| Open-claude | https://github.com/Damienchakma/Open-claude | `c19a42c` | MIT | Tham khao thinking panel, artifact panel, Preview/Source toggle |
| claude-ui | https://github.com/1ps0/claude-ui | `e69e2e3` | MIT, license file con placeholder | Tham khao renderer factory va cac artifact renderer |
| open-artifacts | https://github.com/13point5/open-artifacts | `c032aeb` | MIT | Tham khao tieu chi artifact va HTML Preview/Code |
| assistant-ui | https://github.com/assistant-ui/assistant-ui | `638692a` | MIT | Tham khao pattern assistant thread/composer/artifact |

## Ghi chu license

- Dot polish hien tai khong copy nguyen file source nao tu cac repo tren vao source chay cua TutorHub.
- Cac pattern duoc chuyen hoa lai bang vanilla JS/CSS trong `src/main/resources/ai/ai_chat.html`.
- Neu tuong lai copy mot doan code dang ke tu bat ky repo nao, can giu license/copyright tuong ung theo dieu kien MIT.

## Nguyen tac ap dung

- Khong dua React/Next/Vite runtime vao JCEF neu chi can polish UI nhe.
- Khong dung CDN trong `ai_chat.html`.
- Khong hien raw chain-of-thought.
- Artifact preview phai uu tien sandbox va khong duoc truy cap parent app.
