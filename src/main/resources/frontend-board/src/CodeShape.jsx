import { HTMLContainer, BaseBoxShapeUtil } from 'tldraw'
import { useState, useCallback } from 'react'
import ReactSimpleCodeEditor from 'react-simple-code-editor'
const Editor = ReactSimpleCodeEditor.default || ReactSimpleCodeEditor
import Prism from 'prismjs'
import 'prismjs/components/prism-core'
import 'prismjs/components/prism-clike'
import 'prismjs/components/prism-javascript'
import 'prismjs/components/prism-python'
import 'prismjs/components/prism-java'
import 'prismjs/components/prism-c'
import 'prismjs/components/prism-cpp'
import 'prismjs/themes/prism-tomorrow.css' // Dark theme cho code

export class CodeShapeUtil extends BaseBoxShapeUtil {
  static type = 'code'

  getDefaultProps() {
    return {
      w: 400,
      h: 200,
      code: 'function hello() {\n  console.log("Hello TutorHub!");\n}',
      language: 'javascript'
    }
  }

  canEdit() { return true }
  canResize() { return true }

  indicator(shape) {
    return <rect width={shape.props.w} height={shape.props.h} />
  }

  getIndicatorPath(shape) {
    const path = new Path2D()
    path.rect(0, 0, shape.props.w, shape.props.h)
    return path
  }

  component(shape) {
    try {
      const isEditing = this.editor.getEditingShapeId() === shape.id

      const onValueChange = useCallback(
        (code) => {
          this.editor.updateShape({
            id: shape.id,
            type: 'code',
            props: { code },
          })
        },
        [shape.id]
      )

      const onLanguageChange = (e) => {
        this.editor.updateShape({
          id: shape.id,
          type: 'code',
          props: { language: e.target.value },
        })
      }

      return (
      <HTMLContainer
        id={shape.id}
        style={{
          display: 'flex',
          flexDirection: 'column',
          pointerEvents: isEditing ? 'all' : 'none',
          backgroundColor: '#1d1f21', // Nền của prism-tomorrow
          borderRadius: '8px',
          boxShadow: '0 4px 10px rgba(0,0,0,0.3)',
          overflow: 'hidden',
          width: '100%',
          height: '100%'
        }}
      >
        {/* Header bar cho khối Code */}
        <div style={{
          height: '30px',
          backgroundColor: '#2d2f33',
          borderBottom: '1px solid #111',
          display: 'flex',
          alignItems: 'center',
          padding: '0 10px',
          justifyContent: 'space-between'
        }}>
          <div style={{ display: 'flex', gap: '5px' }}>
            <div style={{ width: 10, height: 10, borderRadius: '50%', backgroundColor: '#ff5f56' }}></div>
            <div style={{ width: 10, height: 10, borderRadius: '50%', backgroundColor: '#ffbd2e' }}></div>
            <div style={{ width: 10, height: 10, borderRadius: '50%', backgroundColor: '#27c93f' }}></div>
          </div>
          {isEditing && (
            <select 
              value={shape.props.language} 
              onChange={onLanguageChange}
              style={{
                backgroundColor: '#1d1f21',
                color: '#ccc',
                border: '1px solid #444',
                borderRadius: '4px',
                fontSize: '11px',
                padding: '2px 5px',
                outline: 'none'
              }}
            >
              <option value="javascript">JavaScript</option>
              <option value="python">Python</option>
              <option value="java">Java</option>
              <option value="cpp">C++</option>
            </select>
          )}
        </div>

        {/* Editor */}
        <div style={{ flex: 1, overflow: 'auto', position: 'relative' }}>
          <Editor
            value={shape.props.code}
            onValueChange={onValueChange}
            highlight={code => Prism.highlight(
              code, 
              Prism.languages[shape.props.language] || Prism.languages.javascript, 
              shape.props.language
            )}
            padding={15}
            style={{
              fontFamily: '"Fira Code", "Consolas", monospace',
              fontSize: 14,
              minHeight: '100%',
              pointerEvents: isEditing ? 'all' : 'none', // Chỉ cho gõ khi Double Click
              color: '#ccc'
            }}
          />
        </div>
        </HTMLContainer>
      )
    } catch (err) {
      return <HTMLContainer id={shape.id}>Lỗi CodeShape: {err.message}</HTMLContainer>
    }
  }
}
