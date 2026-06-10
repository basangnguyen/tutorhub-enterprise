import React, { useState, useEffect } from 'react'
import ReactDOM from 'react-dom/client'
import { 
  Tldraw, 
  useEditor, 
  createShapeId, 
  DefaultStylePanel,
  StylePanelSection, 
  StylePanelColorPicker, 
  StylePanelOpacityPicker,
  StylePanelFillPicker,
  StylePanelDashPicker,
  StylePanelFontPicker,
  StylePanelTextAlignPicker,
  StylePanelLabelAlignPicker,
  StylePanelGeoShapePicker,
  StylePanelArrowKindPicker,
  StylePanelArrowheadPicker,
  StylePanelSplinePicker,
  TldrawUiSlider,
  TldrawUiButton,
  TldrawUiButtonIcon
} from 'tldraw'
import 'tldraw/tldraw.css'
import { MathShapeUtil } from './MathShape'
import { MermaidShapeUtil } from './MermaidShape'
import { CodeShapeUtil } from './CodeShape'
import { PhetShapeUtil } from './PhetShape'
import { QuizShapeUtil } from './QuizShape'
import { phetSims } from './phetData'
import 'mathlive'

window.phetSims = phetSims;

const customShapeUtils = [MathShapeUtil, MermaidShapeUtil, CodeShapeUtil, PhetShapeUtil, QuizShapeUtil]

// Một component nhỏ để lắng nghe thay đổi của Tldraw và đẩy lên Java
function EditorSync() {
  const editor = useEditor()
  
  React.useEffect(() => {
    // Đăng ký sideEffect để ghi đè strokeWidth cho nét vẽ tự do
    const unlistenSideEffect = editor.sideEffects.registerBeforeCreateHandler('shape', (shape) => {
      if (shape.type === 'draw' && window.__TLDRAW_CUSTOM_STROKE_WIDTH) {
        return {
          ...shape,
          meta: {
            ...shape.meta,
            customStrokeWidth: parseFloat(window.__TLDRAW_CUSTOM_STROKE_WIDTH)
          }
        }
      }
      return shape
    });

    // Lưu tham chiếu editor ra toàn cục để Java có thể gọi vào
    window.tldrawAPI = editor;

    const unlisten = editor.store.listen(
      (update) => {
        if (!window.currentRoom || window.isReceivingSync) return;
        
        // Gửi toàn bộ dữ liệu thay đổi lên server thông qua hàm có sẵn của HTML
        if (window.performDrawSync) {
            window.performDrawSync(update.changes);
        }
      },
      { source: 'user', scope: 'document' }
    )
    
    // Báo cho JCEF biết Editor đã sẵn sàng
    if (window.cefQuery) {
        window.cefQuery({ 
            request: 'EDITOR_READY', 
            persistent: false, 
            onSuccess: function(r){}, 
            onFailure: function(e,m){} 
        });
    }

    // Đăng ký API để HTML gọi vào Tldraw
    window.setPaperMode = (paperMode, isDark) => {
        // Cập nhật Dark Mode trong Tldraw (API v2)
        editor.user.updateUserPreferences({ colorScheme: isDark ? 'dark' : 'light' });
        
        // Cập nhật background
        document.body.className = '';
        if (paperMode !== 'none') {
            document.body.classList.add(`paper-${isDark ? 'dark-' : ''}${paperMode}`);
        }
    };

    window.addMathNode = (latexStr) => {
        const center = editor.getViewportPageBounds().center;
        editor.createShapes([
            {
                id: createShapeId(),
                type: 'math',
                x: center.x - 100,
                y: center.y - 50,
                props: { latex: latexStr, w: 200, h: 100 }
            }
        ]);
    };

    window.addMermaidNode = (codeStr) => {
        const center = editor.getViewportPageBounds().center;
        editor.createShapes([
            {
                id: createShapeId(),
                type: 'mermaid',
                x: center.x - 200,
                y: center.y - 150,
                props: { code: codeStr, w: 400, h: 300 }
            }
        ]);
    };

    window.addCodeNode = (codeStr) => {
        const center = editor.getViewportPageBounds().center;
        editor.createShapes([
            {
                id: createShapeId(),
                type: 'code',
                x: center.x - 200,
                y: center.y - 100,
                props: { 
                    code: codeStr || 'function hello() {\n  console.log("TutorHub!");\n}', 
                    language: 'javascript',
                    w: 400, 
                    h: 200 
                }
            }
        ]);
    };

    window.addPhetNode = (url) => {
        const center = editor.getViewportPageBounds().center;
        editor.createShapes([
            {
                id: createShapeId(),
                type: 'phet',
                x: center.x - 400,
                y: center.y - 300,
                props: { url: url, w: 800, h: 600 }
            }
        ]);
    };

    window.addQuizNode = () => {
        const center = editor.getViewportPageBounds().center;
        editor.createShapes([
            {
                id: createShapeId(),
                type: 'quiz',
                x: center.x - 225,
                y: center.y - 175,
                props: { w: 450, h: 350 }
            }
        ]);
    };

    return () => {
      unlisten();
      unlistenSideEffect();
    }
  }, [editor])

  return null
}

function MyStylePanelContent() {
  // Thay đổi: Mặc định là 1 (chứ không phải 0)
  const [val, setVal] = useState(window.__TLDRAW_CUSTOM_STROKE_WIDTH || 1);
  
  useEffect(() => {
    window.__TLDRAW_CUSTOM_STROKE_WIDTH = val;
  }, [val]);

  const handleInput = (e) => {
    const v = parseFloat(e.target.value);
    setVal(v);
  };

  return (
    <>
      <StylePanelSection>
        <StylePanelColorPicker />
        <StylePanelOpacityPicker />
      </StylePanelSection>
      <StylePanelSection>
        <StylePanelFillPicker />
        <StylePanelDashPicker />
        {/* Custom Size Slider thay thế cho StylePanelSizePicker */}
        <div 
          className="tlui-style-panel__row"
          style={{ padding: '0 4px', pointerEvents: 'all' }}
        >
          <TldrawUiButton
            type="icon"
            title="Giảm nét vẽ"
            onClick={() => setVal(Math.max(0.1, val - 0.1))}
            onPointerDown={(e) => e.stopPropagation()}
          >
            <TldrawUiButtonIcon icon="minus" />
          </TldrawUiButton>

          <div style={{ flex: 1, padding: '0 8px' }} onPointerDown={(e) => e.stopPropagation()}>
            <TldrawUiSlider
              min={1}
              steps={200}
              value={Math.round(val * 10)}
              label={`Nét: ${val.toFixed(1)}`}
              title="Độ dày nét bút"
              onValueChange={(v) => setVal(v / 10)}
            />
          </div>

          <TldrawUiButton
            type="icon"
            title="Tăng nét vẽ"
            onClick={() => setVal(Math.min(20, val + 0.1))}
            onPointerDown={(e) => e.stopPropagation()}
          >
            <TldrawUiButtonIcon icon="plus" />
          </TldrawUiButton>
        </div>
      </StylePanelSection>
      <StylePanelSection>
        <StylePanelFontPicker />
        <StylePanelTextAlignPicker />
        <StylePanelLabelAlignPicker />
      </StylePanelSection>
      <StylePanelSection>
        <StylePanelGeoShapePicker />
        <StylePanelArrowKindPicker />
        <StylePanelArrowheadPicker />
        <StylePanelSplinePicker />
      </StylePanelSection>
    </>
  )
}

function CustomStylePanel(props) {
  return (
    <DefaultStylePanel {...props}>
      <MyStylePanelContent />
    </DefaultStylePanel>
  )
}

const App = () => {
    return (
        <div style={{ position: 'absolute', width: '100%', height: '100%', zIndex: 0 }}>
          <Tldraw 
            persistenceKey="tutorhub-board" 
            shapeUtils={customShapeUtils}
            components={{
              StylePanel: CustomStylePanel
            }}
          >
              <EditorSync />
          </Tldraw>
        </div>
    )
}

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
)
