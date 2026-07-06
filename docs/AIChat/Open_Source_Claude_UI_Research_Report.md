# Open-source Claude-like UI research for Lavie AI Chat

Ngay thuc hien: 2026-07-05

## Muc tieu

Nghien cuu cac du an ma nguon mo clone/giong Claude UI de doi chieu voi `src/main/resources/ai/ai_chat.html`, sau do port nhung pattern phu hop vao Lavie AI Chat ma khong doi backend/provider va khong keo React vao JCEF.

## Repo da clone de nghien cuu

Thu muc nghien cuu cuc bo:

`third_party_research/claude-ui`

| Repo | Commit | License | Stack | Gia tri tham khao |
| --- | --- | --- | --- | --- |
| `Damienchakma/Open-claude` | `c19a42c` | MIT | React, Vite, Framer Motion, Monaco, WebContainer | Thinking panel, artifact side panel, Preview/Source toggle, message action |
| `1ps0/claude-ui` | `e69e2e3` | MIT, nhung license file con placeholder `[year] [fullname]` | Vanilla JS modules, CSS, Vite, marked, mermaid, prism | Renderer factory cho Code/Markdown/HTML/SVG/Mermaid/React |
| `13point5/open-artifacts` | `c032aeb` | MIT | Next.js, React, Supabase, AI SDK | Tieu chi khi nao nen tao artifact, HTML artifact co che Preview/Code |
| `assistant-ui/assistant-ui` | `638692a` | MIT | React assistant UI framework | Pattern thread/composer/artifact panel, nhung khong phu hop de nhung truc tiep vao Swing/JCEF hien tai |

## Ket luan ky thuat

Khong nen copy nguyen bat ky repo nao vao TutorHub luc nay.

Ly do:

- `Open-claude`, `open-artifacts`, `assistant-ui` deu dua tren React/framework hien dai, khong khop truc tiep voi file JCEF vanilla `ai_chat.html`.
- `Open-claude` keo them Monaco/WebContainer/Framer Motion, qua nang cho giai doan polish UI.
- `open-artifacts` co doan inject CDN/script vao iframe de capture selection, khong phu hop quy tac khong dung CDN va can can nhac bao mat.
- `claude-ui` gan stack nhat, nhung van dung package runtime nhu `marked`, `mermaid`, `prism`; license file co placeholder nen khong nen copy nguyen khoi.

Huong dung an toan:

- Chi lay y tuong kien truc renderer va interaction.
- Reimplement bang vanilla JS/CSS noi bo trong `ai_chat.html`.
- Khong hien raw chain-of-thought.
- Khong them CDN, khong them dependency moi.

## Phan da port vao Lavie

Da cap nhat `src/main/resources/ai/ai_chat.html`:

- Them segmented control `Preview / Code` tren artifact panel.
- Mo rong `classifyArtifact()` de nhan dien:
  - HTML artifact
  - SVG artifact
  - Mermaid artifact
  - Code artifact
  - Table/document/text nhu cu
- Auto-open panel cho HTML/SVG/Mermaid ke ca khi noi dung khong qua dai, vi day la noi dung tu dung.
- HTML preview chay trong iframe sandbox voi `allow-scripts` nhung khong `allow-same-origin`.
- SVG preview chay trong iframe sandbox khong script.
- Mermaid tam thoi hien fallback source an toan, vi chua vendor local Mermaid renderer.
- Nut Copy trong panel copy source artifact chinh thay vi copy toan bo raw response khi co artifact content.
- Document/table/text van render Markdown nhu cu, khong bi ep sang source code.

## Phan khong port

- Khong port raw thinking/chain-of-thought display tu `Open-claude`.
- Khong port CDN injection/capture selection tu `open-artifacts`.
- Khong port WebContainer, Monaco, React runtime, Supabase, Next.js.
- Khong port Mermaid runtime vi se can vendor local va test rieng tren JCEF.

## Rui ro con lai

- Iframe sandbox HTML cho phep script de co preview tuong tac don gian. Khong co `allow-same-origin`, khong co popup/forms, nhung van can than trong neu sau nay cho artifact doc file/local URL.
- Mermaid hien moi la fallback source, chua render diagram that.
- Chua test bang UI JCEF truc tiep trong app, moi test syntax JS va build.

## Buoc tiep theo de giong Claude hon

1. Vendor local Mermaid neu can diagram preview that, khong dung CDN.
2. Them header artifact co icon type va action "Open full" neu sau nay co right-canvas lon hon.
3. Them artifact history/list neu Lavie sinh nhieu artifact trong mot hoi thoai.
4. Tinh chinh animation panel open/close bang CSS transition nhe hon, khong can Framer Motion.
