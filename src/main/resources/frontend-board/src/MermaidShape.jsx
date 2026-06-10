import { HTMLContainer, BaseBoxShapeUtil, Rectangle2d } from 'tldraw'
import { useEffect, useState, useCallback } from 'react'
import mermaid from 'mermaid'

mermaid.initialize({ startOnLoad: false })

export class MermaidShapeUtil extends BaseBoxShapeUtil {
  static type = 'mermaid'

  getDefaultProps() {
    return {
      w: 400,
      h: 300,
      code: 'graph TD\n    A[Bắt đầu] --> B{Điều kiện}\n    B -- Đúng --> C[Kết quả 1]\n    B -- Sai --> D[Kết quả 2]'
    }
  }

  getGeometry(shape) {
    return new Rectangle2d({
      width: shape.props.w,
      height: shape.props.h,
      isFilled: true,
    })
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
      const [svgContent, setSvgContent] = useState('')
      const scale = Math.min(shape.props.w / 400, shape.props.h / 300)

      const onValueChange = useCallback(
        (e) => {
          this.editor.updateShape({
            id: shape.id,
            type: 'mermaid',
            props: { code: e.target.value },
          })
        },
        [shape.id]
      )

      useEffect(() => {
        let isMounted = true

        const renderMermaid = async () => {
          if (isEditing) return
          try {
            mermaid.initialize({ startOnLoad: false, theme: 'default' })
            const id = `mermaid-${shape.id.replace(':', '-')}`
            const { svg } = await mermaid.render(id, shape.props.code || 'graph TD\n    A-->B;')
            if (isMounted) {
              setSvgContent(svg)
            }
          } catch (e) {
            console.error('Lỗi render Mermaid:', e)
            if (isMounted) {
              setSvgContent(`<div style="color: red; padding: 10px;">Lỗi cú pháp Mermaid: ${e.message}</div>`)
            }
          }
        }

        renderMermaid()

        return () => {
          isMounted = false
        }
      }, [shape.props.code, shape.id, isEditing])

      const innerStyle = {
        width: '400px',
        height: '300px',
        transform: `scale(${scale})`,
        transformOrigin: 'center',
        position: 'absolute',
        top: '50%',
        left: '50%',
        marginLeft: '-200px',
        marginTop: '-150px',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center'
      }

      if (isEditing) {
        return (
          <HTMLContainer id={shape.id} style={{ pointerEvents: 'all', position: 'relative' }}>
            <div style={innerStyle}>
              <textarea
                autoFocus
                value={shape.props.code}
                onChange={onValueChange}
                style={{
                  width: '100%',
                  height: '100%',
                  padding: '10px',
                  fontFamily: 'monospace',
                  fontSize: '14px',
                  border: '2px solid #3b82f6',
                  borderRadius: '8px',
                  resize: 'none',
                  outline: 'none',
                  backgroundColor: '#f8fafc',
                  color: '#333'
                }}
                onKeyDown={(e) => {
                  if (e.key === 'Escape') {
                    this.editor.setCurrentTool('select')
                  }
                }}
              />
            </div>
          </HTMLContainer>
        )
      }

      return (
        <HTMLContainer
          id={shape.id}
          style={{
            pointerEvents: 'none',
            backgroundColor: 'white',
            border: '1px solid #ccc',
            borderRadius: '8px',
            overflow: 'hidden',
            position: 'relative'
          }}
        >
          <div style={innerStyle}>
            <div dangerouslySetInnerHTML={{ __html: svgContent }} style={{ width: '100%', height: '100%', display: 'flex', justifyContent: 'center', alignItems: 'center' }} />
          </div>
        </HTMLContainer>
      )
    } catch (err) {
      return <HTMLContainer id={shape.id}>Lỗi MermaidShape: {err.message}</HTMLContainer>
    }
  }
}
